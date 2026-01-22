package com.api.chatstack.config;

import com.api.chatstack.enums.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final AuthenticationProvider authenticationProvider;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorizeRequests ->
                        authorizeRequests
                                .requestMatchers(
                                    "/chat-stack/api/v1/auth/signup",
                                    "/chat-stack/api/v1/auth/login",
                                    "/chat-stack/api/v1/auth/refresh-token",
                                    "/chat-stack/api/v1/auth/verify-email",
                                    "/chat-stack/api/v1/auth/resend-verification",
                                    "/chat-stack/api/v1/auth/forgot-password",
                                    "/chat-stack/api/v1/auth/reset-password"
                                ).permitAll()
                                .requestMatchers(
                                    "/chat-stack/api/v1/users/me",
                                    "/chat-stack/api/v1/users/{id}",
                                    "/chat-stack/api/v1/auth/logout",
                                    "/chat-stack/api/v1/auth/change-password"
                                ).hasAnyAuthority(Role.USER.name(), Role.ADMIN.name())
                                .requestMatchers("**/admin/**").hasAuthority(Role.ADMIN.name())
                                .anyRequest()
                                .authenticated())
                .sessionManagement(sessionManagement ->
                        sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
