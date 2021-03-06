/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.backend.verifier.ws.config;

import ch.admin.bag.covidcertificate.backend.verifier.ws.config.model.ApiKeyConfig;
import ch.admin.bag.covidcertificate.backend.verifier.ws.security.ApiKeyAuthFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE + 8)
@Profile("api-key")
public class ApiKeySecurity extends WebSecurityConfigurerAdapter {

    @Autowired private ApiKeyConfig apiKeyConfig;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        ApiKeyAuthFilter filter = new ApiKeyAuthFilter();
        filter.setAuthenticationManager(
                new AuthenticationManager() {

                    @Override
                    public Authentication authenticate(Authentication authentication)
                            throws AuthenticationException {
                        String principal = (String) authentication.getPrincipal();
                        if (principal == null) {
                            throw new BadCredentialsException("no authentication provided");
                        }
                        principal = principal.replace("Bearer ", "");
                        if (!apiKeyConfig.getApiKeys().values().contains(principal)) {
                            throw new BadCredentialsException("invalid API key");
                        }
                        authentication.setAuthenticated(true);
                        return authentication;
                    }
                });

        http.antMatcher("/trust/**")
                .csrf()
                .disable()
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .addFilter(filter)
                .authorizeRequests()
                .antMatchers("/trust/v?/keys") // hello endpoint
                .permitAll()
                .antMatchers("/trust/v?/**")
                .authenticated();
    }
}
