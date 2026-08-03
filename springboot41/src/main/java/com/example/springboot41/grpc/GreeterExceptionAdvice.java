package com.example.springboot41.grpc;

import io.grpc.Status;
import org.springframework.grpc.server.advice.GrpcAdvice;
import org.springframework.grpc.server.advice.GrpcExceptionHandler;

/**
 * Sample: {@code @GrpcAdvice} + {@code @GrpcExceptionHandler} - centralized
 * gRPC exception handling, the gRPC equivalent of {@code @ControllerAdvice}.
 */
@GrpcAdvice
public class GreeterExceptionAdvice {

    @GrpcExceptionHandler(IllegalArgumentException.class)
    public Status handleIllegalArgument(IllegalArgumentException ex) {
        return Status.INVALID_ARGUMENT.withDescription(ex.getMessage());
    }
}
