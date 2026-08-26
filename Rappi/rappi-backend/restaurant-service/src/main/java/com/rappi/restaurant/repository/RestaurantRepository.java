package com.rappi.restaurant.repository;

import com.rappi.restaurant.model.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RestaurantRepository extends JpaRepository<Restaurant, UUID> {

  // Find all restaurants that are currently open
  List<Restaurant> findByOpenTrue();

  // Find restaurants by name (for search)
  List<Restaurant> findByNameContainingIgnoreCase(String name);
}