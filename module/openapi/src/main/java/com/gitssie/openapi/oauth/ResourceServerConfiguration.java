package com.gitssie.openapi.oauth;

import com.gitssie.openapi.models.auth.RoleName;
import com.gitssie.openapi.web.EitherReturnValueHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DefaultAuthenticationEventPublisher;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationManager;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationProcessingFilter;
import org.springframework.security.oauth2.provider.error.OAuth2AccessDeniedHandler;
import org.springframework.security.oauth2.provider.error.OAuth2AuthenticationEntryPoint;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@Order(1000)
@EnableResourceServer
public class ResourceServerConfiguration extends ResourceServerConfigurerAdapter {
    private static final String SPARKLR_RESOURCE_ID = "sparklr";
    private String[] PASS_URL = {
            "/api/auth/login",
            "/api/auth/state",
            "/api/auth/generateCode",
            "/api/qywx/revapprove",
            "/api/qywx/login",
            "/api/qywx/oauthCode"};

    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private TokenStore tokenStore;
    @Autowired(required = false)
    private CaptchaStore captchaStore;
    @Autowired
    private EitherReturnValueHandler eitherReturnValueHandler;
    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Override
    public void configure(ResourceServerSecurityConfigurer resources) {
        resources.eventPublisher(new DefaultAuthenticationEventPublisher(eventPublisher));

        StandardOAuth2ExceptionRenderer exceptionRenderer = new StandardOAuth2ExceptionRenderer();
        resources.addObjectPostProcessor(new ObjectPostProcessor() {
            @Override
            public Object postProcess(Object object) {
                if (object instanceof OAuth2AuthenticationEntryPoint) {
                    ((OAuth2AuthenticationEntryPoint) object).setExceptionRenderer(exceptionRenderer);
                } else if (object instanceof OAuth2AuthenticationProcessingFilter) {
                    ((OAuth2AuthenticationProcessingFilter) object).setAuthenticationEntryPoint(exceptionRenderer);
                }
                return object;
            }
        });
        resources.resourceId(SPARKLR_RESOURCE_ID).stateless(false);
        resources.tokenStore(tokenStore);
        resources.authenticationManager(new OAuth2AuthenticationManager());
        //标准的错误处理
        OAuth2AccessDeniedHandler deniedHandler = new OAuth2AccessDeniedHandler();
        deniedHandler.setExceptionRenderer(exceptionRenderer);
        resources.accessDeniedHandler(deniedHandler);
    }

    @Override
    public void configure(HttpSecurity http) throws Exception {
        //使用Spring login filter进行登录
        UsernamePasswordAuthenticationFilter loginFilter = new UsernamePasswordAuthenticationFilter(authenticationManager);
        loginFilter.setRequiresAuthenticationRequestMatcher(new AntPathRequestMatcher("/api/auth/login", "POST"));
        AuthenticationResultHandler authenticationResultHandler = new AuthenticationResultHandler(eitherReturnValueHandler);
        loginFilter.setAuthenticationSuccessHandler(authenticationResultHandler);
        loginFilter.setAuthenticationFailureHandler(authenticationResultHandler);
        loginFilter.setApplicationEventPublisher(eventPublisher);
        loginFilter.setCaptchaStore(captchaStore);

        http.addFilterBefore(loginFilter, AbstractPreAuthenticatedProcessingFilter.class);
        http.logout().logoutUrl("/api/auth/logout").permitAll()
                .logoutSuccessHandler((request, response, authentication) -> {
                    eitherReturnValueHandler.handleReturnValue(true, response);
                })

                //.and().authorizeRequests().accessDecisionManager(accessDecisionManager())
                .and().sessionManagement().sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .and().authorizeRequests().antMatchers(PASS_URL).permitAll()
//                .and().authorizeRequests().antMatchers("/**").permitAll()
                .and().authorizeRequests().anyRequest().hasAnyAuthority(RoleName.ROLE_ADMIN, RoleName.ROLE_AUTHENTICATED);
    }

    /** 废弃使用
     @Deprecated public AccessDecisionManager accessDecisionManager() {
     WebExpressionVoter webExpressionVoter = new WebExpressionVoter();
     webExpressionVoter.setExpressionHandler(new OAuth2WebSecurityExpressionHandler());
     List<AccessDecisionVoter<? extends Object>> decisionVoters = Arrays.asList(
     new AuthenticatedVoter(),
     webExpressionVoter,
     new URLVerbVoter());

     return new AffirmativeBased(decisionVoters);
     }
     **/
}
