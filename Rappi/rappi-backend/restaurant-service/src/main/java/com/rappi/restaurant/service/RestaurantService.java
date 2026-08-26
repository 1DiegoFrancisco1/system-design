package com.rappi.restaurant.service;

import com.rappi.restaurant.model.MenuItem;
import com.rappi.restaurant.model.Restaurant;
import com.rappi.restaurant.repository.MenuItemRepository;
import com.rappi.restaurant.repository.RestaurantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RestaurantService {

  private final RestaurantRepository restaurantRepository;
  private final MenuItemRepository menuItemRepository;

  // ── Create a restaurant ────────────────────────────
  @Transactional
  public Restaurant createRestaurant(String name, String address) {
    var restaurant = Restaurant.builder()
            .name(name)
            .address(address)
            .open(true)
            .build();
    var saved = restaurantRepository.save(restaurant);
    log.info("Restaurant created: {} ({})", saved.getName(), saved.getId());
    return saved;
  }

  // ── Add a menu item to a restaurant ────────────────
  @Transactional
  public MenuItem addMenuItem(UUID restaurantId, String name,
                              String category, java.math.BigDecimal price) {
    var restaurant = restaurantRepository.findById(restaurantId)
            .orElseThrow(() -> new RuntimeException("Restaurant not found: " + restaurantId));

    var item = MenuItem.builder()
            .name(name)
            .category(category)
            .price(price)
            .available(true)
            .restaurant(restaurant)
            .build();

    var saved = menuItemRepository.save(item);
    log.info("Menu item added: {} to restaurant {}", saved.getName(), restaurantId);
    return saved;
  }

  // ── Get a restaurant's full menu ───────────────────
  public List<MenuItem> getMenu(UUID restaurantId) {
    return menuItemRepository.findByRestaurantId(restaurantId);
  }

  // ── Get only available items ───────────────────────
  public List<MenuItem> getAvailableMenu(UUID restaurantId) {
    return menuItemRepository.findByRestaurantIdAndAvailableTrue(restaurantId);
  }

  // ── Mark an item as sold out (the cache invalidation trigger) ──
  @Transactional
  public MenuItem setAvailability(UUID menuItemId, boolean available) {
    var item = menuItemRepository.findById(menuItemId)
            .orElseThrow(() -> new RuntimeException("Menu item not found: " + menuItemId));

    item.setAvailable(available);
    var saved = menuItemRepository.save(item);
    log.info("Menu item {} availability set to: {}", menuItemId, available);

    // TODO Stage 2: invalidate Redis cache here
    // redisTemplate.delete("menu:restaurant:" + item.getRestaurant().getId());

    return saved;
  }

  // ── Get all open restaurants ───────────────────────
  public List<Restaurant> getOpenRestaurants() {
    return restaurantRepository.findByOpenTrue();
  }

  // ── Check if restaurant can accept orders ──────────
  public boolean isRestaurantOpen(UUID restaurantId) {
    return restaurantRepository.findById(restaurantId)
            .map(Restaurant::isOpen)
            .orElse(false);
  }
}