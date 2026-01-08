package com.blur.apigateway.configuration;

import com.blur.apigateway.repository.IdentityClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.util.Arrays;

@Configuration
public class WebClientConfiguration {

    @Value("${app.services.identity}")
    private String identityServiceUrl;

    @Value("${app.services.chat}")
    private String chatServiceUrl;

    @Value("${CORS_ALLOWED_ORIGIN:http://localhost:3000}")
    private String allowedOrigin;

    @Value("${CORS_ALLOWED_HEADERS:*}")
    private String allowedHeaders;

    @Value("${CORS_ALLOWED_METHODS:GET,POST,PUT,DELETE,OPTIONS}")
    private String allowedMethods;

    @Value("${CORS_ALLOW_CREDENTIALS:true}")
    private Boolean allowCredentials;

    @Value("${CORS_MAX_AGE:3600}")
    private Long maxAge;

    @Bean
    public WebClient webClient() {
        return WebClient.builder()
                .baseUrl(identityServiceUrl)
                .build();
    }

    @Bean
    IdentityClient identityClient(WebClient webClient) {
        HttpServiceProxyFactory httpServiceProxyFactory = HttpServiceProxyFactory
                .builderFor(WebClientAdapter.create(webClient))
                .build();
        return httpServiceProxyFactory.createClient(IdentityClient.class);
    }

    @Bean
    @Order(-2)
    CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfig = new CorsConfiguration();

        corsConfig.setAllowCredentials(allowCredentials);
        corsConfig.setAllowedOrigins(Arrays.asList(allowedOrigin.split(",")));
        corsConfig.setAllowedHeaders(Arrays.asList(allowedHeaders.split(",")));
        corsConfig.setAllowedMethods(Arrays.asList(allowedMethods.split(",")));
        corsConfig.setExposedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type"));
        corsConfig.setMaxAge(maxAge);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);

        System.out.println("✅ CORS Filter configured for: http://localhost:3000");

        return new CorsWebFilter(source);
    }

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("websocket_route", r -> r.path("/ws/**")
                        .uri(chatServiceUrl))
                .build();
    }
}