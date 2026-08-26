package com.rappi.restaurant.controller;

import com.rappi.restaurant.model.MenuItem;
import com.rappi.restaurant.model.Restaurant;
import com.rappi.restaurant.service.RestaurantService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/restaurants")
@RequiredArgsConstructor
public class RestaurantController {

  private final RestaurantService restaurantService;

  // ── Create a restaurant ────────────────────────────
  @PostMapping
  public ResponseEntity<Restaurant> createRestaurant(
          @RequestBody CreateRestaurantRequest request) {
    Restaurant restaurant = restaurantService.createRestaurant(
            request.name(),
            request.address()
    );
    return ResponseEntity.status(HttpStatus.CREATED).body(restaurant);
  }

  // ── Get all open restaurants ───────────────────────
  @GetMapping
  public ResponseEntity<List<Restaurant>> getOpenRestaurants() {
    return ResponseEntity.ok(restaurantService.getOpenRestaurants());
  }

  // ── Add a menu item ────────────────────────────────
  @PostMapping("/{restaurantId}/menu")
  public ResponseEntity<MenuItem> addMenuItem(
          @PathVariable UUID restaurantId,
          @RequestBody AddMenuItemRequest request) {
    MenuItem item = restaurantService.addMenuItem(
            restaurantId,
            request.name(),
            request.category(),
            request.price()
    );
    return ResponseEntity.status(HttpStatus.CREATED).body(item);
  }

  // ── Get a restaurant's menu ────────────────────────
  @GetMapping("/{restaurantId}/menu")
  public ResponseEntity<List<MenuItem>> getMenu(@PathVariable UUID restaurantId) {
    return ResponseEntity.ok(restaurantService.getMenu(restaurantId));
  }

  // ── Get only available items ───────────────────────
  @GetMapping("/{restaurantId}/menu/available")
  public ResponseEntity<List<MenuItem>> getAvailableMenu(
          @PathVariable UUID restaurantId) {
    return ResponseEntity.ok(restaurantService.getAvailableMenu(restaurantId));
  }

  // ── Mark item available / sold out ─────────────────
  @PatchMapping("/menu/{menuItemId}/availability")
  public ResponseEntity<MenuItem> setAvailability(
          @PathVariable UUID menuItemId,
          @RequestBody SetAvailabilityRequest request) {
    MenuItem item = restaurantService.setAvailability(
            menuItemId,
            request.available()
    );
    return ResponseEntity.ok(item);
  }
}