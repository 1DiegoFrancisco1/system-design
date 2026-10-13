package com.rappi.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

  @Bean
  WebClient restaurantServiceClient() {
    return WebClient.builder()
            .baseUrl("http://localhost:8082")   // your Restaurant Service
            .build();
  }
}