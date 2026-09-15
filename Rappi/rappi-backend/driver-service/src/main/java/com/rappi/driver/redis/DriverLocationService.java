package com.rappi.driver.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DriverLocationService {

  private final StringRedisTemplate redis;

  // Redis key names - one place, no magic strings scattered around
  private static final String GEO_KEY = "driver_locations";
  private static final String AVAILABLE_KEY = "available_drivers";

  // Update a driver's GPS position (the 5-second ping)
  public void updateLocation(UUID driverId, double lng, double lat) {
    redis.opsForGeo().add(
            GEO_KEY,
            new Point(lng, lat),      // NOTE: Redis wants (longitude, latitude)
            driverId.toString()
    );
    log.debug("Update location for driver {}: ({}, {})", driverId, lng, lat);
  }

  // Mark a driver as available (on start shift)
  public void markAvailable(UUID driverId) {
    redis.opsForSet().add(AVAILABLE_KEY, driverId.toString());
    log.info("Driver {} added to available_drivers set", driverId);
  }

  // Mark a driver as NOT available (end shift / assigned)
  public void markUnavailable(UUID driverId) {
    redis.opsForSet().remove(AVAILABLE_KEY, driverId.toString());
    log.info("Driver {} removed from available_drivers set", driverId);
  }
}
