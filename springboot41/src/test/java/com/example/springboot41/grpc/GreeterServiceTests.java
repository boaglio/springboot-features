package com.example.springboot41.grpc;

import com.google.protobuf.StringValue;
import io.grpc.CallOptions;
import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.ClientCalls;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.grpc.test.autoconfigure.AutoConfigureTestGrpcTransport;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.grpc.client.GrpcChannelFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@code @AutoConfigureTestGrpcTransport} swaps in an in-process channel
 * factory/server, so this exercises the auto-configured
 * {@code GreeterService} and {@code GreeterExceptionAdvice} over a real gRPC
 * call without opening a socket.
 * <p>
 * To call the real running server (port 9090) by hand instead, use
 * {@code grpcurl}. Plain {@code grpcurl -plaintext localhost:9090 list}
 * only shows {@code grpc.health.v1.Health} and the reflection service
 * itself - {@code springboot41.Greeter} is missing, because
 * {@link GreeterService}'s hand-built {@code MethodDescriptor} (see its
 * Javadoc) carries no {@code FileDescriptorProto}, which is what gRPC
 * reflection needs to describe a service. Give grpcurl a matching stub
 * {@code .proto} instead and it works fine - the wire format is identical
 * either way, since reflection is purely a schema-discovery convenience,
 * not something the RPC call itself depends on:
 * <pre>{@code
 * # greeter.proto - just enough to describe the RPC for grpcurl; the app
 * # itself has no .proto file, see GreeterService's Javadoc for why.
 * syntax = "proto3";
 * package springboot41;
 * import "google/protobuf/wrappers.proto";
 *
 * service Greeter {
 *   rpc Greet(google.protobuf.StringValue) returns (google.protobuf.StringValue);
 * }
 *
 * # StringValue is a well-known wrapper type, so its JSON form is a bare
 * # string/number/bool, not {"value": ...} - hence '"Ada"', not '{"value":"Ada"}'.
 * grpcurl -plaintext -import-path . -proto greeter.proto \
 *     -d '"Ada"' localhost:9090 springboot41.Greeter/Greet
 * # => "Hello, Ada!"
 *
 * grpcurl -plaintext -import-path . -proto greeter.proto \
 *     -d '""' localhost:9090 springboot41.Greeter/Greet
 * # => ERROR: Code: InvalidArgument, Message: name must not be blank
 *
 * # No .proto needed for this one - Health is a standard service the
 * # reflection API already knows how to describe.
 * grpcurl -plaintext localhost:9090 grpc.health.v1.Health/Check
 * # => {"status": "SERVING"}
 * }</pre>
 */
@SpringBootTest
@AutoConfigureTestGrpcTransport
class GreeterServiceTests {

    @Autowired
    GrpcChannelFactory channelFactory;

    @Test
    void greetsByName() {
        // grpcurl -plaintext localhost:9090 list
        // grpcurl -plaintext -import-path . -proto greeter.proto -d '"Ada"' localhost:9090 springboot41.Greeter/Greet
        ManagedChannel channel = channelFactory.createChannel("local");

        StringValue reply = ClientCalls.blockingUnaryCall(
                channel.newCall(GreeterService.GREET_METHOD, CallOptions.DEFAULT), StringValue.of("Ada"));

        assertThat(reply.getValue()).isEqualTo("Hello, Ada!");
    }

    @Test
    void grpcAdviceMapsIllegalArgumentToInvalidArgumentStatus() {
        // grpcurl -plaintext -import-path . -proto greeter.proto -d '""' localhost:9090 springboot41.Greeter/Greet
        ManagedChannel channel = channelFactory.createChannel("local");

        assertThatThrownBy(() -> ClientCalls.blockingUnaryCall(
                channel.newCall(GreeterService.GREET_METHOD, CallOptions.DEFAULT), StringValue.of("")))
                .isInstanceOf(StatusRuntimeException.class)
                .hasMessageContaining("INVALID_ARGUMENT");
    }
}
