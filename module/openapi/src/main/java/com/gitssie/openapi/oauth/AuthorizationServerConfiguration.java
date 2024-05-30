package com.gitssie.openapi.oauth;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.config.annotation.builders.InMemoryClientDetailsServiceBuilder;
import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.client.ClientCredentialsTokenEndpointFilter;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;

@Configuration
@EnableAuthorizationServer
@Order(1000)
public class AuthorizationServerConfiguration extends AuthorizationServerConfigurerAdapter {
    private static final String SPARKLR_RESOURCE_ID = "sparklr";

    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private TokenStore tokenStore;
    @Autowired
    private JwtAccessTokenConverter converter;
    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Override
    public void configure(ClientDetailsServiceConfigurer clients) throws Exception {
        InMemoryClientDetailsServiceBuilder builder = clients.inMemory();
        builder.withClient("8d14bba5fe8d0e6aa1")
                .resourceIds(SPARKLR_RESOURCE_ID)
                .authorizedGrantTypes("password", "authorization_code", "implicit")
                .authorities("ROLE_CLIENT")
                .scopes("*", "any", "read", "write")
                .secret(passwordEncoder.encode("8d14bba5fe8d0e6aa1"))
                .accessTokenValiditySeconds(3600 * 24 * 30)
                .autoApprove(true);
        builder.withClient("19241b414f20e0d9")
                .resourceIds(SPARKLR_RESOURCE_ID)
                .authorizedGrantTypes("client_credentials")
                .authorities("ROLE_ADMIN")
                .scopes("admin")
                .secret(passwordEncoder.encode("19241b414f20e0d9"))
                .accessTokenValiditySeconds(3600 * 24 * 30)
                .autoApprove(true);
    }

    @Override
    public void configure(AuthorizationServerEndpointsConfigurer endpoints) throws Exception {
        endpoints.authenticationManager(new ResourceOwnerPasswordTokenAuthentication(authenticationManager, eventPublisher));
        endpoints.tokenStore(tokenStore);
        endpoints.accessTokenConverter(converter);
    }

    @Override
    public void configure(AuthorizationServerSecurityConfigurer oauthServer) throws Exception {
        StandardOAuth2ExceptionRenderer exceptionRenderer = new StandardOAuth2ExceptionRenderer();
        oauthServer.addObjectPostProcessor(new ObjectPostProcessor() {
            @Override
            public Object postProcess(Object object) {
                if (object instanceof ClientCredentialsTokenEndpointFilter) {
                    ((ClientCredentialsTokenEndpointFilter) object).setAuthenticationEntryPoint(exceptionRenderer);
                }
                return object;
            }
        });
        oauthServer.allowFormAuthenticationForClients();
        oauthServer.passwordEncoder(passwordEncoder);
    }

}