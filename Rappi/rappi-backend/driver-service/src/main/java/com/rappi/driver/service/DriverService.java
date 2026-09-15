package com.rappi.driver.service;

import com.rappi.driver.model.Driver;
import com.rappi.driver.model.DriverStatus;
import com.rappi.driver.redis.DriverLocationService;
import com.rappi.driver.repository.DriverRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DriverService {

  private final DriverRepository driverRepository;
  private final DriverLocationService locationService;

  // ── Register a new driver (starts OFFLINE) ─────────
  @Transactional
  public Driver registerDriver(String name, String vehiclePlate) {
    var driver = Driver.builder()
            .name(name)
            .vehiclePlate(vehiclePlate)
            .status(DriverStatus.OFFLINE)
            .build();
    var saved = driverRepository.save(driver);
    log.info("Driver registered: {} ({})", saved.getName(), saved.getId());
    return saved;
  }

  // ── Start shift: OFFLINE → AVAILABLE ───────────────
  @Transactional
  public Driver startShift(UUID driverId) {
    var driver = getDriverOrThrow(driverId);

    if (driver.getStatus() == DriverStatus.ON_DELIVERY) {
      throw new IllegalStateException("Driver is on a delivery, cannot change shift");
    }

    driver.setStatus(DriverStatus.AVAILABLE);
    var saved = driverRepository.save(driver);
    log.info("Driver {} started shift → AVAILABLE", driverId);

    locationService.markAvailable(driverId); // Redis: SADD available_drivers
    return saved;
  }

  // ── End shift: AVAILABLE → OFFLINE ─────────────────
  @Transactional
  public Driver endShift(UUID driverId) {
    var driver = getDriverOrThrow(driverId);

    if (driver.getStatus() == DriverStatus.ON_DELIVERY) {
      throw new IllegalStateException("Driver is on a delivery, cannot end shift");
    }

    driver.setStatus(DriverStatus.OFFLINE);
    var saved = driverRepository.save(driver);
    log.info("Driver {} ended shift → OFFLINE", driverId);

    locationService.markUnavailable(driverId); // Redis: SREM available_drivers
    return saved;
  }

  // ── Read helpers ───────────────────────────────────
  public Driver getDriver(UUID driverId) {
    return getDriverOrThrow(driverId);
  }

  public List<Driver> getAvailableDrivers() {
    return driverRepository.findByStatus(DriverStatus.AVAILABLE);
  }

  private Driver getDriverOrThrow(UUID driverId) {
    return driverRepository.findById(driverId)
            .orElseThrow(() -> new RuntimeException("Driver not found: " + driverId));
  }

  // Driver location ping -> straight to Redis GEO
  public void updateLocation(UUID driverId, double lng, double lat) {
    // The durable driver record in Postgres doesn't change on every GPS ping.
    locationService.updateLocation(driverId, lng, lat);
  }
}