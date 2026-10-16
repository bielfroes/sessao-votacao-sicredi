package com.froes.assembly.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class ExternalServicesConfiguration {

    @Bean
    RestClient eligibilityRestClient(
            RestClient.Builder builder,
            @Value("${app.eligibility.base-url}") String baseUrl,
            @Value("${app.eligibility.connect-timeout:PT2S}") Duration connectTimeout,
            @Value("${app.eligibility.read-timeout:PT3S}") Duration readTimeout) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeout);
        requestFactory.setReadTimeout(readTimeout);
        return builder.baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }

    @Bean
    RestClient consulRestClient(
            RestClient.Builder builder,
            @Value("${app.mobile.consul.address:http://localhost:8500}") String consulAddress,
            @Value("${app.mobile.consul.connect-timeout:PT2S}") Duration connectTimeout,
            @Value("${app.mobile.consul.read-timeout:PT3S}") Duration readTimeout) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeout);
        requestFactory.setReadTimeout(readTimeout);
        return builder.clone()
                .baseUrl(consulAddress)
                .requestFactory(requestFactory)
                .build();
    }
}
