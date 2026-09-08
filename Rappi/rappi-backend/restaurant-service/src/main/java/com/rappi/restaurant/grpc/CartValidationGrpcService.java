package com.rappi.restaurant.grpc;

import com.rappi.grpc.cart.*;
import com.rappi.restaurant.model.MenuItem;
import com.rappi.restaurant.repository.MenuItemRepository;
import com.rappi.restaurant.repository.RestaurantRepository;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.grpc.server.service.GrpcService;

import java.math.BigDecimal;
import java.util.UUID;

@GrpcService
@RequiredArgsConstructor
@Slf4j
public class CartValidationGrpcService
        extends CartValidationServiceGrpc.CartValidationServiceImplBase {

  private final RestaurantRepository restaurantRepository;
  private final MenuItemRepository menuItemRepository;

  @Override
  public void validateCart(ValidateCartRequest request,
                           StreamObserver<ValidateCartResponse> responseObserver) {

    log.info("gRPC ValidateCart called for restaurant: {}", request.getRestaurantId());

    var responseBuilder = ValidateCartResponse.newBuilder();
    boolean valid = true;
    BigDecimal authoritativeTotal = BigDecimal.ZERO;

    UUID restaurantId = UUID.fromString(request.getRestaurantId());

    // Check 1: is the restaurant open?
    boolean restaurantOpen = restaurantRepository.findById(restaurantId)
            .map(r -> r.isOpen())
            .orElse(false);

    if (!restaurantOpen) {
      responseBuilder.addErrors(ValidationError.newBuilder()
              .setMenuItemId("N/A")
              .setReason("RESTAURANT_CLOSED")
              .build());
      valid = false;
    }

    // Check 2: validate each item
    for (CartItem item : request.getItemsList()) {
      UUID menuItemId = UUID.fromString(item.getMenuItemId());
      var menuItemOpt = menuItemRepository.findById(menuItemId);

      if (menuItemOpt.isEmpty()) {
        responseBuilder.addErrors(ValidationError.newBuilder()
                .setMenuItemId(item.getMenuItemId())
                .setReason("NOT_FOUND")
                .build());
        valid = false;
        continue;
      }

      MenuItem menuItem = menuItemOpt.get();

      // Check availability
      if (!menuItem.isAvailable()) {
        responseBuilder.addErrors(ValidationError.newBuilder()
                .setMenuItemId(item.getMenuItemId())
                .setReason("UNAVAILABLE")
                .build());
        valid = false;
        continue;
      }

      // Check price matches (never trust the client's price)
      BigDecimal clientPrice = new BigDecimal(item.getClientPrice());
      if (menuItem.getPrice().compareTo(clientPrice) != 0) {
        responseBuilder.addErrors(ValidationError.newBuilder()
                .setMenuItemId(item.getMenuItemId())
                .setReason("PRICE_MISMATCH")
                .build());
        valid = false;
        continue;
      }

      // Item is valid — add to authoritative total
      authoritativeTotal = authoritativeTotal.add(
              menuItem.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()))
      );
    }

    // Build and send the response
    var response = responseBuilder
            .setValid(valid)
            .setAuthoritativeTotal(authoritativeTotal.toString())
            .build();

    responseObserver.onNext(response);      // send the response
    responseObserver.onCompleted();          // signal we're done

    log.info("gRPC ValidateCart result: valid={}, total={}", valid, authoritativeTotal);
  }
}