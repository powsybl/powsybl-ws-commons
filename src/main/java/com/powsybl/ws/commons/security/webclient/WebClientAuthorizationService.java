/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.ws.commons.security.webclient;

import com.powsybl.ws.commons.security.PermissionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.UUID;

import static com.powsybl.ws.commons.security.AuthorizationSystemConstants.*;

/**
 * @author Radouane KHOUADRI {@literal <redouane.khouadri_externe at rte-france.com>}
 */
public class WebClientAuthorizationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(WebClientAuthorizationService.class);

    private static final String PARAM_IDS = "ids";
    private static final String PARAM_ACCESS_TYPE = "accessType";
    private static final String QUERY_STUDY_UUID = "studyUuid";
    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private final WebClient webClient;

    public WebClientAuthorizationService(WebClient.Builder webClientBuilder, String directoryServerBaseUri) {
        this.webClient = webClientBuilder
                .baseUrl(directoryServerBaseUri)
                .build();
    }

    public Mono<Boolean> canReadStudy(WebSocketSession session) {
        UUID studyUuid = extractStudyUuid(session);
        if (studyUuid == null) {
            return Mono.just(false);
        }

        return session.getHandshakeInfo().getPrincipal()
                .filter(Authentication.class::isInstance)
                .map(Authentication.class::cast)
                .flatMap(authentication -> checkReadAccess(authentication.getName(), extractRoles(authentication), studyUuid))
                .defaultIfEmpty(false);
    }

    private Mono<Boolean> checkReadAccess(String userId, String roles, UUID studyUuid) {
        return webClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.path("/v1/elements/authorized")
                        .queryParam(PARAM_ACCESS_TYPE, PermissionType.READ)
                        .queryParam(PARAM_IDS, studyUuid);
                    return uriBuilder.build();
                })
                .header(HEADER_USER_ID, userId)
                .header(HEADER_ROLES, roles)
                .exchangeToMono(response -> {
                    if (response.statusCode().is2xxSuccessful()) {
                        return Mono.just(true);
                    }
                    if (response.statusCode().value() == 403) {
                        return Mono.just(false);
                    }
                    return response.createException().flatMap(Mono::error);
                }).timeout(TIMEOUT)
                .doOnNext(granted -> LOGGER.debug("Read access for user {} on study {}: {}", userId, studyUuid, granted))
                .onErrorResume(e -> {
                    LOGGER.warn("Authorization check failed for user {} on study {}. Access denied.", userId, studyUuid, e);
                    return Mono.just(false);
                })
                .defaultIfEmpty(false);
    }

    private static UUID extractStudyUuid(WebSocketSession session) {
        String raw = UriComponentsBuilder.fromUri(session.getHandshakeInfo().getUri())
                .build()
                .getQueryParams()
                .getFirst(QUERY_STUDY_UUID);
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(raw.trim());
        } catch (IllegalArgumentException _) {
            return null;
        }
    }
}
