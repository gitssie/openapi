package com.gitssie.openapi.oauth;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.oauth2.provider.token.store.InMemoryTokenStore;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.store.JwtTokenStore;
import org.springframework.security.oauth2.provider.token.store.redis.RedisTokenStore;

import java.util.Map;

@Configuration
public class AuthorizationConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public JwtAccessTokenConverter tokenConverter(@Value("${spring.security.oauth2.authorizationserver.jwt.signingKey}") String signKey) {
        JwtAccessTokenConverter converter = new JwtAccessTokenConverter() {
            @Override
            protected Map<String, Object> decode(String token) {
                Map<String, Object> claims = super.decode(token);
                claims.remove(EXP);
                return claims;
            }
        };
        converter.setSigningKey(signKey);
        return converter;
    }

    @Bean
    @ConditionalOnMissingBean
    public TokenStore jwtTokenStore(JwtAccessTokenConverter converter) {
//        return new InMemoryTokenStore();
        return new JwtTokenStore(converter);
    }

    @Bean
    @Profile("prod")
    @ConditionalOnMissingBean
    public TokenStore tokenStore(JwtAccessTokenConverter converter, @Autowired(required = false) RedisConnectionFactory connectionFactory) {
        if (connectionFactory != null) {
            return new RedisTokenStore(connectionFactory);
        }
        return new InMemoryTokenStore();
    }
}
