package com.example.config;

import com.example.util.KeycloakRoleConverter;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Collection;

@Component
public class JwtAuthenticationConverter implements Converter<Jwt, Mono<AbstractAuthenticationToken>> {

    @Override
    public Mono<AbstractAuthenticationToken> convert(Jwt jwt) {
        System.out.println("Token Claims: " + jwt.getClaims());
        Collection<GrantedAuthority> authorities = KeycloakRoleConverter.extractAuthorities(jwt.getClaims());
        return Mono.just(new JwtAuthenticationToken(jwt, authorities));
    }
}