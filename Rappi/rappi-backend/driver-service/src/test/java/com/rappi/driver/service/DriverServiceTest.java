package com.rappi.driver.service;

import com.rappi.driver.kafka.DriverEventProducer;
import com.rappi.driver.model.Driver;
import com.rappi.driver.model.DriverStatus;
import com.rappi.driver.redis.DriverLocationService;
import com.rappi.driver.repository.DriverRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DriverServiceTest {

  private DriverRepository driverRepository;
  private DriverLocationService locationService;
  private DriverEventProducer driverEventProducer;
  private DriverService driverService;

  @BeforeEach
  void setUp() {
    driverRepository = mock(DriverRepository.class);
    locationService = mock(DriverLocationService.class);
    driverEventProducer = mock(DriverEventProducer.class);
    driverService = new DriverService(driverRepository, locationService, driverEventProducer);
  }

  // Runs ONCE for each status listed — OFFLINE, then AVAILABLE
  @ParameterizedTest
  @EnumSource(value = DriverStatus.class, names = {"OFFLINE", "AVAILABLE"})
  void completeDelivery_throwsIfNotOnDelivery(DriverStatus status) {
    // ── ARRANGE ──────────────────────────────────
    UUID driverId = UUID.randomUUID();
    Driver driver = Driver.builder()
            .id(driverId)
            .name("Test Driver")
            .vehiclePlate("ABC-123")
            .status(status)                 // the parameterized value
            .build();

    when(driverRepository.findById(driverId))
            .thenReturn(Optional.of(driver));

    // ── ACT + ASSERT ─────────────────────────────
    // For any status that isn't ON_DELIVERY, completing a delivery must throw
    assertThrows(IllegalStateException.class,
            () -> driverService.completeDelivery(driverId));

    // And nothing should have been saved or freed
    verify(driverRepository, never()).save(any());
    verify(locationService, never()).markAvailable(any());
  }
}