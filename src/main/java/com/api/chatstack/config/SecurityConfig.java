package com.api.chatstack.config;

import com.api.chatstack.entities.auth.UserEntity;
import com.api.chatstack.mappers.UserMapper;
import com.api.chatstack.repositories.UserRepository;
import com.api.chatstack.services.impl.CustomOauth2UserService;
import com.chatstack.dto.AdminUpdateUserRequest;
import com.chatstack.dto.AuthResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final AuthenticationProvider authenticationProvider;
    private final CustomOauth2UserService customOauth2UserService;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final UserMapper userMapper;

    // SWAGGER
    @Bean
    @Order(1)
    public SecurityFilterChain swaggerFilterChain(HttpSecurity http) {
        return http
                .securityMatcher("/swagger/**", "/swagger-ui/**", "/v3/api-docs/**")
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .build();
    }

    // AUTH
    @Bean
    @Order(2)
    public SecurityFilterChain authFilterChain(HttpSecurity http) throws Exception {
        return http
                .securityMatcher("/auth/**")
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/auth/signup",
                                "/auth/login",
                                "/auth/refresh-token",
                                "/auth/verify-email",
                                "/auth/resend-verification",
                                "/auth/forgot-password",
                                "/auth/reset-password"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    // ADMIN
    @Bean
    @Order(3)
    public SecurityFilterChain adminFilterChain(HttpSecurity http) {
        return http
                .securityMatcher("/admin/**")
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().hasAuthority(AdminUpdateUserRequest.RoleEnum.ADMIN.name())
                )
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    // USER
    @Bean
    @Order(4)
    public SecurityFilterChain userFilterChain(HttpSecurity http) {
        return http
                .securityMatcher("/users/**")
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/users/*/avatar/**").permitAll()
                        .requestMatchers("/users/avatar/**").permitAll()
                        .requestMatchers(
                                "/users/me",
                                "/users/{id}"
                        ).hasAnyAuthority(
                                AdminUpdateUserRequest.RoleEnum.USER.name(),
                                AdminUpdateUserRequest.RoleEnum.ADMIN.name()
                        )
                        .anyRequest().hasAnyAuthority(
                                AdminUpdateUserRequest.RoleEnum.USER.name(),
                                AdminUpdateUserRequest.RoleEnum.ADMIN.name()
                        )
                )
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    // OAUTH2
    @Bean
    @Order(5)
    public SecurityFilterChain oAuth2FilterChain(HttpSecurity http) {
        return http
                .securityMatcher("/oauth2/authorization/**", "/login/oauth2/code/**")
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().authenticated()
                )
                .sessionManagement(sm -> sm
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                )
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo ->
                                userInfo.userService(customOauth2UserService)
                        )
                        .successHandler((request, response, authentication) -> {
                            OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
                            assert oAuth2User != null;
                            String email = oAuth2User.getAttribute("email");
                            UserEntity user = userRepository.findByEmail(email).orElseThrow();

                            String accessToken = jwtService.generateAccessToken(user);
                            String refreshToken = jwtService.generateRefreshToken(user);

                            ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", refreshToken)
                                    .httpOnly(true)
                                    .secure(false)
                                    .path("/chat-stack/api/v1/auth/refresh-token")
                                    .maxAge(Duration.ofDays(7))
                                    .sameSite("Lax")
                                    .build();
                            response.addHeader("Set-Cookie", refreshCookie.toString());
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);

                            AuthResponse authResponse = new AuthResponse()
                                    .accessToken(accessToken)
                                    .user(userMapper.toDto(user));
                            response.getWriter().write(new ObjectMapper().writeValueAsString(authResponse));
                        })
                )
                .build();
    }

    // FALLBACK (ANY OTHER REQUEST MUST BE AUTHENTICATED)
    @Bean
    @Order(6)
    public SecurityFilterChain defaultFilterChain(HttpSecurity http) {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().authenticated()
                )
                .sessionManagement(sm -> sm
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

}