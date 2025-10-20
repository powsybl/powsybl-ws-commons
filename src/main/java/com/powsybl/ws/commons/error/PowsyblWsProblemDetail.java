/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.ws.commons.error;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;

import java.time.Instant;
import java.util.*;

/**
 * @author Mohamed Ben-rejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 * Shared {@link ProblemDetail} subclass that carries typed metadata used across Powsybl services.
 */
@Getter
@JsonIgnoreProperties({"instance", "type"})
public final class PowsyblWsProblemDetail extends ProblemDetail {

    private String server;
    private String businessErrorCode;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant timestamp;
    private String path;
    private String traceId;
    private List<ChainEntry> chain;

    @JsonCreator
    public PowsyblWsProblemDetail(
        @JsonProperty("title") String title,
        @JsonProperty("status") Integer status,
        @JsonProperty("detail") String detail,
        @JsonProperty("server") String server,
        @JsonProperty("businessErrorCode") String businessErrorCode,
        @JsonProperty("timestamp") Instant timestamp,
        @JsonProperty("path") String path,
        @JsonProperty("traceId") String traceId,
        @JsonProperty("chain") List<ChainEntry> chain
    ) {
        super(status != null ? status : HttpStatus.INTERNAL_SERVER_ERROR.value());
        HttpStatusCode resolvedStatus = status != null
            ? HttpStatusCode.valueOf(status)
            : HttpStatus.INTERNAL_SERVER_ERROR;
        applyStatus(resolvedStatus);
        setTitle(title);
        setDetail(detail);
        this.server = server;
        this.businessErrorCode = businessErrorCode;
        this.timestamp = timestamp;
        this.path = path;
        this.traceId = traceId;
        this.chain = chain != null ? new ArrayList<>(chain) : new ArrayList<>();
        ensureDetail();
    }

    private PowsyblWsProblemDetail(HttpStatusCode status) {
        super(status != null ? status.value() : HttpStatus.INTERNAL_SERVER_ERROR.value());
        applyStatus(status != null ? status : HttpStatus.INTERNAL_SERVER_ERROR);
        HttpStatus resolved = status instanceof HttpStatus httpStatus
            ? httpStatus
            : HttpStatus.resolve(getStatus());
        if (resolved != null) {
            setTitle(resolved.getReasonPhrase());
        }
        this.chain = new ArrayList<>();
    }

    private void applyStatus(HttpStatusCode status) {
        if (status instanceof HttpStatus httpStatus) {
            setStatus(httpStatus);
        } else if (status != null) {
            setStatus(status.value());
        }
    }

    public static Builder builder(HttpStatus status) {
        return builder((HttpStatusCode) status);
    }

    public static Builder builder(HttpStatusCode status) {
        return new Builder(status);
    }

    public static Builder builderFrom(PowsyblWsProblemDetail template) {
        return new Builder(Objects.requireNonNull(template, "template"));
    }

    private void ensureDetail() {
        if (hasText(getDetail())) {
            return;
        }
        if (hasText(getTitle())) {
            setDetail(getTitle());
            return;
        }
        Integer status = getStatus();
        HttpStatus resolved = HttpStatus.resolve(status);
        setDetail(resolved != null ? resolved.getReasonPhrase() : String.valueOf(status));
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    @Getter
    public static final class Builder {
        private final PowsyblWsProblemDetail target;
        private final List<ChainEntry> chainEntries;

        private String fromServer;
        private String method;
        private String path;
        Integer status;
        private Instant timestamp;

        private Builder(HttpStatusCode status) {
            this.target = new PowsyblWsProblemDetail(Objects.requireNonNull(status, "status"));
            this.chainEntries = new ArrayList<>();
        }

        private Builder(PowsyblWsProblemDetail template) {
            HttpStatusCode statusCode = HttpStatusCode.valueOf(template.getStatus());
            this.target = new PowsyblWsProblemDetail(statusCode);
            this.target.setTitle(template.getTitle());
            this.target.setDetail(template.getDetail());
            this.target.server = template.server;
            this.target.businessErrorCode = template.businessErrorCode;
            this.target.timestamp = template.timestamp;
            this.target.path = template.path;
            this.target.traceId = template.traceId;
            this.chainEntries = template.chain != null
                ? new ArrayList<>(template.chain)
                : new ArrayList<>();
        }

        public Builder title(String title) {
            target.setTitle(title);
            return this;
        }

        public Builder detail(String detail) {
            target.setDetail(detail);
            return this;
        }

        public Builder server(String server) {
            target.server = server;
            return this;
        }

        public Builder businessErrorCode(String businessErrorCode) {
            target.businessErrorCode = businessErrorCode;
            return this;
        }

        public Builder timestamp(Instant timestamp) {
            target.timestamp = timestamp;
            return this;
        }

        public Builder path(String path) {
            target.path = path;
            return this;
        }

        public Builder traceId(String traceId) {
            target.traceId = traceId;
            return this;
        }

        public Builder appendChain(String fromServer, String method, String path, Integer status, Instant timestamp) {
            this.fromServer = fromServer;
            this.method = method;
            this.path = path;
            this.status = status;
            this.timestamp = timestamp;
            return this;
        }

        public PowsyblWsProblemDetail build() {
            if (target.timestamp == null) {
                target.timestamp = Instant.now();
            }
            target.ensureDetail();
            List<ChainEntry> updatedChain = new ArrayList<>(chainEntries);
            ChainEntry chainEntry = createNewChainEntry(updatedChain);
            if (chainEntry != null) {
                updatedChain.addFirst(chainEntry);
            }
            target.chain = List.copyOf(updatedChain);
            return target;
        }

        private ChainEntry createNewChainEntry(List<ChainEntry> existing) {
            if (fromServer == null) {
                return null;
            }
            String toServer = determineTarget(existing);
            if (toServer == null) {
                return null;
            }
            Instant instant = timestamp != null ? timestamp : target.timestamp;
            Integer statusValue = status != null ? status : target.getStatus();
            return new ChainEntry(fromServer, toServer, method, path, statusValue, instant);
        }

        private String determineTarget(List<ChainEntry> existing) {
            return existing.isEmpty()
                ? target.server
                : Optional.ofNullable(existing.getFirst().getFromServer()).orElse(target.server);
        }
    }

    @Getter
    public static final class ChainEntry {
        private final String fromServer;

        private final String toServer;

        private final String method;

        private final String path;

        private final Integer status;

        private final Instant timestamp;

        @JsonCreator
        public ChainEntry(
            @JsonProperty("from-server") String fromServer,
            @JsonProperty("to-server") String toServer,
            @JsonProperty("method") String method,
            @JsonProperty("path") String path,
            @JsonProperty("status") Integer status,
            @JsonProperty("timestamp") @JsonFormat(shape = JsonFormat.Shape.STRING) Instant timestamp
        ) {
            Objects.requireNonNull(fromServer, "from-server");
            Objects.requireNonNull(toServer, "to-server");
            Objects.requireNonNull(timestamp, "timestamp");
            this.fromServer = fromServer;
            this.toServer = toServer;
            this.method = method;
            this.path = path;
            this.status = status;
            this.timestamp = timestamp;
        }
    }
}
