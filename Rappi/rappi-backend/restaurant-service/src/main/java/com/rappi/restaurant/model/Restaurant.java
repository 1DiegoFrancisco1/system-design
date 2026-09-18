package com.rappi.restaurant.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "restaurants")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Restaurant {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false)
  private String address;

  // Coordinates - needed so we can find nearby drivers at assignment time
  @Column(nullable = false)
  private double latitude;

  @Column(nullable = false)
  private double longitude;

  // Is the restaurant currently accepting orders?
  @Column(nullable = false)
  private boolean open;

  // One restaurant has many menu items
  @OneToMany(
          mappedBy = "restaurant",
          cascade = CascadeType.ALL,
          orphanRemoval = true,
          fetch = FetchType.LAZY
  )
  @Builder.Default
  private List<MenuItem> menu = new ArrayList<>();

  @Column(nullable = false)
  private LocalDateTime createdAt;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
  }
}