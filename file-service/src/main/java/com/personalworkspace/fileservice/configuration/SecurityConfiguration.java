package com.personalworkspace.fileservice.configuration;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;
@Configuration(proxyBeanMethods=false)
public class SecurityConfiguration {
 @Bean JwtDecoder jwtDecoder(@Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")String jwks,@Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")String issuer,@Value("${security.jwt.audience}")String audience){
  NimbusJwtDecoder d=NimbusJwtDecoder.withJwkSetUri(jwks).build();
  OAuth2TokenValidator<Jwt> aud=j->j.getAudience().contains(audience)?OAuth2TokenValidatorResult.success():OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token","Required audience is missing",null));
  d.setJwtValidator(new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefaultWithIssuer(issuer),aud));return d;
 }
 @Bean SecurityFilterChain chain(HttpSecurity h)throws Exception{return h.csrf(c->c.disable()).sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
  .authorizeHttpRequests(a->a.requestMatchers("/actuator/health","/actuator/info").permitAll().requestMatchers("/api/**").hasAnyRole("USER","ADMIN").anyRequest().authenticated())
  .oauth2ResourceServer(o->o.jwt(j->j.jwtAuthenticationConverter(converter()))).build();}
 private Converter<Jwt,JwtAuthenticationToken> converter(){return jwt->{Map<String,Object> ra=jwt.getClaimAsMap("realm_access");Collection<?> roles=ra==null?List.of():(Collection<?>)ra.getOrDefault("roles",List.of());Collection<GrantedAuthority> as=roles.stream().map(Object::toString).map(r->new SimpleGrantedAuthority("ROLE_"+r)).collect(Collectors.toSet());return new JwtAuthenticationToken(jwt,as);};}
}
