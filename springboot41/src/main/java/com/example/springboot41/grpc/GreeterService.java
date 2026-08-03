package com.example.springboot41.grpc;

import com.google.protobuf.StringValue;
import io.grpc.BindableService;
import io.grpc.MethodDescriptor;
import io.grpc.ServerServiceDefinition;
import io.grpc.protobuf.ProtoUtils;
import io.grpc.stub.ServerCalls;
import io.grpc.stub.StreamObserver;
import org.springframework.grpc.server.service.GrpcService;

/**
 * Sample: Spring Boot 4.1 gRPC auto-configuration.
 * <p>
 * {@code @GrpcService} is auto-detected and bound to the auto-configured
 * Netty gRPC server - no manual {@code ServerBuilder} wiring required. This
 * uses hand-built {@link MethodDescriptor}s with the well-known
 * {@link StringValue} protobuf type instead of a generated stub, so the
 * sample needs no protoc/protobuf-maven-plugin toolchain to build.
 * <p>
 * Trade-off: no {@code FileDescriptorProto}, so this service doesn't show
 * up in {@code grpcurl localhost:9090 list} even though the server's gRPC
 * reflection service is auto-registered - see {@code GreeterServiceTests}
 * for the {@code grpcurl} incantation that calls it anyway with a small
 * stand-in {@code .proto}.
 */
// Auto-detected by Spring Boot 4.1's gRPC auto-configuration and registered
// on the auto-configured server - equivalent to adding a service to a
// ServerBuilder by hand, but with no wiring code of our own.
@GrpcService
public class GreeterService implements BindableService {

    // A plain gRPC-Java MethodDescriptor describing one RPC (name, request/
    // response types and how to (de)serialize them). Generated stubs build
    // this same object from a .proto file at compile time; here it's
    // hand-built so the sample needs no protoc toolchain.
    static final MethodDescriptor<StringValue, StringValue> GREET_METHOD = MethodDescriptor.<StringValue, StringValue>newBuilder()
            .setType(MethodDescriptor.MethodType.UNARY) // one request, one response (vs. streaming)
            .setFullMethodName(MethodDescriptor.generateFullMethodName("springboot41.Greeter", "Greet"))
            .setRequestMarshaller(ProtoUtils.marshaller(StringValue.getDefaultInstance()))
            .setResponseMarshaller(ProtoUtils.marshaller(StringValue.getDefaultInstance()))
            .build();

    @Override
    public ServerServiceDefinition bindService() {
        // What a generated *ImplBase normally assembles for you: bind the
        // method descriptor above to the handler method that serves it.
        return ServerServiceDefinition.builder("springboot41.Greeter")
                .addMethod(GREET_METHOD, ServerCalls.asyncUnaryCall(this::greet))
                .build();
    }

    private void greet(StringValue request, StreamObserver<StringValue> responseObserver) {
        if (request.getValue().isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        responseObserver.onNext(StringValue.of("Hello, " + request.getValue() + "!"));
        responseObserver.onCompleted();
    }
}
