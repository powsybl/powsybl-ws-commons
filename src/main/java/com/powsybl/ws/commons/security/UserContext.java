/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.ws.commons.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author Caroline Jeandat <caroline.jeandat at rte-france.com>
 */
public final class UserContext {

    public static final String HEADER_USER_ID = "userId";
    public static final String HEADER_ROLES = "roles";
    public static final String ROLES_DELIMITER = "|";

    private UserContext() {
    }

    public static String getUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? null : authentication.getName();
    }

    public static String getRoles() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null
                ? null
                : authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.joining(ROLES_DELIMITER));
    }

    public static List<SimpleGrantedAuthority> extractRoles(String rolesHeader) {
        return rolesHeader == null
                ? Collections.emptyList()
                : Arrays.stream(rolesHeader.split(Pattern.quote(ROLES_DELIMITER)))
                .map(String::trim)
                .filter(role -> !role.isEmpty())
                .map(SimpleGrantedAuthority::new)
                .toList();
    }
}
