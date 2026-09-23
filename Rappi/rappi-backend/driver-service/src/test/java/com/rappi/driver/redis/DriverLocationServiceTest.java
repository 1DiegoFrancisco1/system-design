package com.rappi.driver.redis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DriverLocationServiceTest {
  private StringRedisTemplate redis;
  private SetOperations<String, String> setOps;
  private DriverLocationService locationService;

  @BeforeEach
  void setUp() {
    redis = mock(StringRedisTemplate.class);
    setOps = mock(SetOperations.class);

    // redis.opsForSet() returns our fake set operations
    when(redis.opsForSet()).thenReturn(setOps);

    locationService = new DriverLocationService(redis);
  }

  @Test
  void tryClaimDriver_firstClaim_succeeds() {
    // ARRANGE
    String driverId = UUID.randomUUID().toString();

    // SREM removed 1 element → the driver WAS available, we claimed them
    when(setOps.remove("available_drivers", driverId))
            .thenReturn(1L);

    // ACT
    boolean claimed = locationService.tryClaimDriver(driverId);

    // ASSERT
    assertTrue(claimed, "first claim should succeed");
  }

  @Test
  void tryClaimDriver_secondClaim_fails_noDoubleAssignment() {
    // ARRANGE
    String driverId = UUID.randomUUID().toString();

    // SREM removed 0 → driver was already taken by someone else
    when(setOps.remove("available_drivers", driverId))
            .thenReturn(0L);

    // ACT
    boolean claimed = locationService.tryClaimDriver(driverId);

    // ASSERT
    assertFalse(claimed, "claiming an already-taken driver must fail");
  }

}
