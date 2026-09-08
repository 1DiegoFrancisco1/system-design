package com.rappi.order.grpc;

import com.rappi.grpc.cart.CartValidationServiceGrpc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.client.GrpcChannelFactory;

@Configuration
public class GrpcClientConfig {

  @Bean
  CartValidationServiceGrpc.CartValidationServiceBlockingStub cartValidationStub(
          GrpcChannelFactory channels) {
    // Pass the full address directly
    return CartValidationServiceGrpc.newBlockingStub(
            channels.createChannel("static://localhost:9090")
    );
  }
}