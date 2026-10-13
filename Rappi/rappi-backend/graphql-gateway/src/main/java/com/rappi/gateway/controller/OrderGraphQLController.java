package com.rappi.gateway.controller;

import com.rappi.gateway.model.Order;
import com.rappi.gateway.model.PlaceOrderInput;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.stereotype.Controller;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Controller
public class OrderGraphQLController {

  private final WebClient orderServiceClient;

  // Two WebClient beans exist, so we name which one we want
  public OrderGraphQLController(@Qualifier("orderServiceClient") WebClient orderServiceClient) {
    this.orderServiceClient = orderServiceClient;
  }

  @MutationMapping
  public Mono<Order> placeOrder(@Argument PlaceOrderInput input) {
    return orderServiceClient.post()
            .uri("/api/orders")
            .bodyValue(input)
            .retrieve()
            .bodyToMono(Order.class);
  }
}
