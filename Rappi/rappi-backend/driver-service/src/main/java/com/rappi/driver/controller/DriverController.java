package com.rappi.driver.controller;

import com.rappi.driver.model.Driver;
import com.rappi.driver.service.DriverService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/drivers")
@RequiredArgsConstructor
public class DriverController {

  private final DriverService driverService;

  // ── Register a new driver ──────────────────────────
  @PostMapping
  public ResponseEntity<Driver> registerDriver(
          @RequestBody RegisterDriverRequest request) {
    Driver driver = driverService.registerDriver(
            request.name(),
            request.vehiclePlate()
    );
    return ResponseEntity.status(HttpStatus.CREATED).body(driver);
  }

  // ── Start shift (OFFLINE → AVAILABLE) ──────────────
  @PostMapping("/{driverId}/start-shift")
  public ResponseEntity<Driver> startShift(@PathVariable UUID driverId) {
    return ResponseEntity.ok(driverService.startShift(driverId));
  }

  // ── End shift (AVAILABLE → OFFLINE) ────────────────
  @PostMapping("/{driverId}/end-shift")
  public ResponseEntity<Driver> endShift(@PathVariable UUID driverId) {
    return ResponseEntity.ok(driverService.endShift(driverId));
  }

  // ── Get one driver ─────────────────────────────────
  @GetMapping("/{driverId}")
  public ResponseEntity<Driver> getDriver(@PathVariable UUID driverId) {
    return ResponseEntity.ok(driverService.getDriver(driverId));
  }

  // ── Get all available drivers (admin/debug view) ───
  @GetMapping("/available")
  public ResponseEntity<List<Driver>> getAvailableDrivers() {
    return ResponseEntity.ok(driverService.getAvailableDrivers());
  }
}