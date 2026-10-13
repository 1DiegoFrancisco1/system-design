package com.rappi.gateway.controller;

import com.rappi.gateway.model.MenuItem;
import com.rappi.gateway.model.Restaurant;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Controller
@RequiredArgsConstructor
public class RestaurantGraphQLController {

  private final WebClient restaurantServiceClient;

  @QueryMapping
  public Mono<Restaurant> restaurant(@Argument String id) {
    return restaurantServiceClient.get()
            .uri("/api/restaurants/{id}", id)
            .retrieve()
            .bodyToMono(Restaurant.class);   // no .block() — return the Mono
  }

  @QueryMapping
  public Flux<Restaurant> openRestaurants() {
    return restaurantServiceClient.get()
            .uri("/api/restaurants")
            .retrieve()
            .bodyToFlux(Restaurant.class);   // return the Flux
  }

  @SchemaMapping(typeName = "Restaurant", field = "menu")
  public Flux<MenuItem> menu(Restaurant restaurant) {
    return restaurantServiceClient.get()
            .uri("/api/restaurants/{id}/menu", restaurant.id())
            .retrieve()
            .bodyToFlux(MenuItem.class);
  }
}