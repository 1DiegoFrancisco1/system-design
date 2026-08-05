package com.rappi.order.controller;

import com.rappi.order.model.OrderStatus;

public record UpdateStatusRequest(OrderStatus status) {}