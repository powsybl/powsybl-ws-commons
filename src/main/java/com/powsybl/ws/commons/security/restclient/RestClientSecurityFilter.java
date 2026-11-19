/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.ws.commons.security.restclient;

import com.powsybl.ws.commons.security.AuthorizationSystemConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * @author Caroline Jeandat <caroline.jeandat at rte-france.com>
 */
public class RestClientSecurityFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String userId = request.getHeader(AuthorizationSystemConstants.HEADER_USER_ID);
        String rolesHeader = request.getHeader(AuthorizationSystemConstants.HEADER_ROLES);

        if (userId != null && !userId.isEmpty()) {
            // The roles are forwarded in a single header using "|" as a separator,
            // so they must be split back into individual authorities when building the SecurityContext
            List<SimpleGrantedAuthority> authorities = AuthorizationSystemConstants.extractAuthorities(rolesHeader);
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(userId, null, authorities));
        }

        filterChain.doFilter(request, response);
    }
}
