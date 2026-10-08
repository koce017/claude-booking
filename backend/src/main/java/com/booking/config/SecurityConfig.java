package com.booking.config;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.booking.admin.AdminAuthService;
import com.booking.admin.BearerTokenAuthenticationFilter;
import com.booking.common.error.GlobalExceptionHandler;

/**
 * Stateless security: public booking endpoints are open, /api/admin/** requires a
 * valid admin bearer token (except the login endpoints). No cookies are used, so
 * CSRF protection is not needed.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, AdminAuthService authService,
                                                   ObjectMapper objectMapper) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> {
                })
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/admin/auth/request-link", "/api/admin/auth/verify").permitAll()
                        .requestMatchers("/api/admin/**").hasAuthority(BearerTokenAuthenticationFilter.ADMIN_AUTHORITY)
                        .anyRequest().permitAll())
                .addFilterBefore(new BearerTokenAuthenticationFilter(authService), UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((req, res, ex) -> writeProblem(res, objectMapper,
                                HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "Authentication is required"))
                        .accessDeniedHandler((req, res, ex) -> writeProblem(res, objectMapper,
                                HttpStatus.FORBIDDEN, "FORBIDDEN", "Access denied")));
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(BookingProperties properties) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(properties.frontendUrl()));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

    private static void writeProblem(HttpServletResponse response, ObjectMapper objectMapper,
                                     HttpStatus status, String code, String detail) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), GlobalExceptionHandler.problem(status, code, detail));
    }
}
