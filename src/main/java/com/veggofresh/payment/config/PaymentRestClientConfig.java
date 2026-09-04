package com.veggofresh.payment.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
import org.springframework.web.client.RestTemplate;

/**
 * Razorpay's REST API authenticates every request with HTTP Basic auth --
 * {@code key_id} as username, {@code key_secret} as password (no OAuth,
 * no bearer token). One shared, pre-authenticated {@link RestTemplate}
 * bean for the whole Payment module rather than re-adding the header on
 * every call site.
 */
@Configuration
@RequiredArgsConstructor
public class PaymentRestClientConfig {

    private final RazorpayProperties razorpayProperties;

    @Bean
    public RestTemplate razorpayRestTemplate(RestTemplateBuilder builder) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);  // ms -- Spring Boot 3 way
        factory.setReadTimeout(10_000);

        return builder
                .requestFactory(() -> factory)
                .additionalInterceptors(new BasicAuthenticationInterceptor(
                        razorpayProperties.getKeyId(), razorpayProperties.getKeySecret()))
                .build();
    }
}
