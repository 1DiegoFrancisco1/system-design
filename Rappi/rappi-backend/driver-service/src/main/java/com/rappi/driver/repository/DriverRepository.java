package com.rappi.driver.repository;

import com.rappi.driver.model.Driver;
import com.rappi.driver.model.DriverStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DriverRepository extends JpaRepository<Driver, UUID> {

  // Find all drivers with a given status (e.g. all AVAILABLE ones)
  List<Driver> findByStatus(DriverStatus status);
}