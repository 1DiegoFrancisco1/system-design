package com.rappi.driver.model;

public enum DriverStatus {
  OFFLINE,      // not on shift
  AVAILABLE,    // on shift, free to take an order
  ON_DELIVERY   // currently assigned to an order
}