package com.personalworkspace.gateway.configuration;

import com.personalworkspace.gateway.security.AudienceValidator;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Mono;
import org.springframework.beans.factory.annotation.Value;

@Configuration(proxyBeanMethods = false)
@EnableWebFluxSecurity
public class SecurityConfiguration {

    @Bean
    ReactiveJwtDecoder jwtDecoder(
            @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}") String jwkSetUri,
            @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuer,
            @Value("${security.jwt.audience}") String audience) {
        NimbusReactiveJwtDecoder decoder =
                NimbusReactiveJwtDecoder.withJwkSetUri(jwkSetUri).build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(issuer),
                new AudienceValidator(audience)));
        return decoder;
    }

    @Bean
    SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(authorize -> authorize
                        .pathMatchers("/actuator/health", "/actuator/info").permitAll()
                        .pathMatchers("/api/**").hasAnyRole("USER", "ADMIN")
                        .anyExchange().authenticated())
                .oauth2ResourceServer(resourceServer ->
                        resourceServer.jwt(jwt -> jwt.jwtAuthenticationConverter(
                                keycloakAuthoritiesConverter())))
                .build();
    }

    @Bean
    Converter<Jwt, Mono<AbstractAuthenticationToken>> keycloakAuthoritiesConverter() {
        return jwt -> Mono.just(new JwtAuthenticationToken(jwt, realmAuthorities(jwt)));
    }

    private Collection<GrantedAuthority> realmAuthorities(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess == null || !(realmAccess.get("roles") instanceof Collection<?> roles)) {
            return java.util.List.of();
        }
        return roles.stream()
                .map(Object::toString)
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toUnmodifiableSet());
    }
}
