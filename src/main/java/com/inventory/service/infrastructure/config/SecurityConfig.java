package com.inventory.service.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

/**
 * Reactive Spring Security configuration for JWT-based authentication.
 *
 * <p>Reads are public; reservations and ticket operations require a valid bearer token issued
 * by the user service. The token is compared locally with the shared HMAC secret, so inventory
 * never calls the user service per request.
 */
@EnableReactiveMethodSecurity
@Configuration
public class SecurityConfig {

    @Value("${jwt.secret:change-me-in-production-32chars-min}")
    private String jwtSecret;

    @Bean
    SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(ServerHttpSecurity.CorsSpec::disable)
                .authorizeExchange(exchange -> exchange
                        // 1. Infrastructure and health endpoints (public)
                        .pathMatchers("/actuator/**").permitAll()

                        // 2. Availability reads are public (event id and locality id are UUIDs)
                        .pathMatchers(HttpMethod.GET, "/api/v1/events/{eventId}/sections/{sectionId}/availability").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/inventario/eventos/*/disponibilidad").permitAll()
                        .pathMatchers(HttpMethod.GET, "/events/*/availability").permitAll()

                        // 3. Everything else (reservations, tickets) requires authentication
                        .anyExchange().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .jwtAuthenticationConverter(
                                        new ReactiveJwtAuthenticationConverterAdapter(jwtAuthenticationConverter()))
                        )
                )
                .build();
    }

    @Bean
    ReactiveJwtDecoder jwtDecoder() {
        SecretKey key = new SecretKeySpec(
                jwtSecret.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256");
        return NimbusReactiveJwtDecoder.withSecretKey(key)
                .macAlgorithm(macAlgorithmFor(key))
                .build();
    }

    /**
     * The user service signs with jjwt, which picks the strongest HMAC algorithm supported by
     * the shared secret length (HS256/HS384/HS512). The decoder must use the same one.
     */
    private MacAlgorithm macAlgorithmFor(SecretKey key) {
        int keyBytes = key.getEncoded().length;
        if (keyBytes >= 64) {
            return MacAlgorithm.HS512;
        }
        if (keyBytes >= 48) {
            return MacAlgorithm.HS384;
        }
        return MacAlgorithm.HS256;
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        // The user service issues a single lowercase role in the "role" claim; Spring Security
        // expects the ROLE_ prefix in uppercase (hasRole('COMPRADOR') -> ROLE_COMPRADOR).
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            String role = jwt.getClaimAsString("role");
            if (role == null || role.isBlank()) {
                return List.of();
            }
            return List.of(new SimpleGrantedAuthority(
                    "ROLE_" + role.toUpperCase(Locale.ROOT)));
        });
        return converter;
    }
}
