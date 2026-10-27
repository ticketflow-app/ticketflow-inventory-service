package com.inventory.service.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;

import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * Reactive Spring Security configuration for JWT-based authentication.
 *
 * <p>Endpoints:
 * <ul>
 *   <li>GET /actuator/** -> permits all (health checks and monitoring)</li>
 *   <li>GET /api/v1/events/.../availability -> permits all (anonymous read)</li>
 *   <li>POST /api/v1/events/.../reservations -> requires authenticated JWT bearer token</li>
 *   <li>Any other endpoint -> requires authentication</li>
 * </ul>
 *
 * CSRF and CORS are disabled for this stateless API.
 */
@Configuration
public class SecurityConfig {

    @Value("${jwt.secret:change-me-in-production-32chars-min}")
    private String jwtSecret;

    @Bean
    SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.disable())
                .authorizeExchange(exchange -> exchange
                        // 1. Endpoints de infraestructura y salud (públicos)
                        .pathMatchers("/actuator/**").permitAll()

                        // 2. Consulta de disponibilidad (pública con/sin sección)
                        .pathMatchers(HttpMethod.GET, "/api/v1/events/{eventId}/sections/{sectionId}/availability").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/v1/events/{eventId}/availability").permitAll()

                        // 3. Creación de reservas (requiere Token JWT)
                        .pathMatchers(HttpMethod.POST, "/api/v1/events/{eventId}/sections/{sectionId}/reservations").authenticated()
                        .pathMatchers(HttpMethod.POST, "/api/v1/events/{eventId}/reservations").authenticated()

                        // 4. Cualquier otra ruta no especificada requiere autenticación
                        .anyExchange().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .jwtAuthenticationConverter(new ReactiveJwtAuthenticationConverterAdapter(jwtAuthenticationConverter()))
                        )
                )
                .build();
    }

    @Bean
    ReactiveJwtDecoder jwtDecoder() {
        SecretKey key = new SecretKeySpec(
                jwtSecret.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256");
        return NimbusReactiveJwtDecoder.withSecretKey(key).build();
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        grantedAuthoritiesConverter.setAuthoritiesClaimName("roles");
        grantedAuthoritiesConverter.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
        return jwtAuthenticationConverter;
    }
}