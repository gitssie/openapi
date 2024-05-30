package com.gitssie.openapi.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gitssie.openapi.converter.NumberToBooleanConverter;
import com.gitssie.openapi.page.ModelConverter;
import com.gitssie.openapi.service.PageService;
import com.gitssie.openapi.service.Provider;
import com.gitssie.openapi.web.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.boot.autoconfigure.jackson.JacksonProperties;
import org.springframework.boot.autoconfigure.web.ErrorProperties;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcRegistrations;
import org.springframework.boot.autoconfigure.web.servlet.error.BasicErrorController;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.format.FormatterRegistry;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    @Autowired
    private ObjectProvider<ObjectMapper> objectMapper;
    @Autowired
    private ObjectProvider<ModelConverter> modelConverter;
    @Autowired
    private ObjectProvider<Provider> provider;
    @Autowired
    private ObjectProvider<PageService> pageService;
    @Autowired
    private Environment env;
    @Autowired
    private MessageSource messageSource;
    @Autowired
    private Tracer tracer;

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jsonCustomizer(JacksonProperties properties) {
        return builder -> builder.modules(new JacksonConfiguration.StdModule(properties), new JacksonConfiguration.EntityBeanModule(properties, provider));
    }

    @Override
    public void addReturnValueHandlers(List<HandlerMethodReturnValueHandler> handlers) {
        handlers.add(eitherReturnValueHandler());
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        ModeArgumentResolver resolver = new ModeArgumentResolver(pageService.getIfAvailable());
        resolvers.add(new QueryFormMethodArgumentResolver(objectMapper.getIfAvailable(), resolver));
        resolvers.add(new FastJsonMethodArgumentResolver(resolver));
        resolvers.add(new ModelMethodArgumentResolver(resolver));
        resolvers.add(new BeanTypeMethodArgumentResolver(provider.getIfAvailable()));
    }

    @Bean
    public EitherReturnValueHandler eitherReturnValueHandler() {
        return new EitherReturnValueHandler(objectMapper.getIfAvailable(), modelConverter, messageSource, tracer);
    }

    @Override
    public void extendHandlerExceptionResolvers(List<HandlerExceptionResolver> exceptionResolvers) {
        CodeExceptionResolver code = new CodeExceptionResolver(objectMapper.getIfAvailable(), messageSource, tracer);
        exceptionResolvers.add(0, code);
    }

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(NumberToBooleanConverter.INSTANCE);
    }

    @Bean
    public ErrorAttributes errorAttributes(EitherReturnValueHandler handler) {
        return new CodeErrorAttributes(handler);
    }

    @Bean
    public WebMvcRegistrations webMvcRegistrations() {
        return new WebApiMvcRegistrations();
    }

    @Bean
    public ErrorController basicErrorController(ErrorAttributes errorAttributes) {
        return new BasicErrorController(errorAttributes, new ErrorProperties()) {
            @RequestMapping(method = {RequestMethod.GET, RequestMethod.HEAD, RequestMethod.POST, RequestMethod.PUT, RequestMethod.PATCH, RequestMethod.DELETE, RequestMethod.OPTIONS, RequestMethod.TRACE}, produces = MediaType.TEXT_HTML_VALUE)
            public ResponseEntity<Map<String, Object>> errorJson(HttpServletRequest request) {
                ResponseEntity<Map<String, Object>> body = super.error(request);
                return ResponseEntity.status(body.getStatusCode()).contentType(MediaType.APPLICATION_JSON).body(body.getBody());
            }
        };
    }
}
