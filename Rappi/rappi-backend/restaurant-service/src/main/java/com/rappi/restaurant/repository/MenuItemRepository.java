package com.rappi.restaurant.repository;

import com.rappi.restaurant.model.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MenuItemRepository extends JpaRepository<MenuItem, UUID> {

  // Find all menu items for a restaurant
  List<MenuItem> findByRestaurantId(UUID restaurantId);

  // Find only available items for a restaurant
  List<MenuItem> findByRestaurantIdAndAvailableTrue(UUID restaurantId);
}