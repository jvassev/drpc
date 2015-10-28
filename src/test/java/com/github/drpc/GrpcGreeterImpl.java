package com.github.drpc;

import io.grpc.examples.helloworld.GreeterGrpc.Greeter;
import io.grpc.examples.helloworld.HelloRequest;
import io.grpc.examples.helloworld.HelloResponse;
import io.grpc.stub.StreamObserver;

public class GrpcGreeterImpl implements Greeter {

    private int port;

    public GrpcGreeterImpl(int port) {
        this.port = port;
    }

    @Override
    public void sayHello(HelloRequest req, StreamObserver<HelloResponse> observer) {
        HelloResponse reponse = HelloResponse.newBuilder()
                .setMessage("" + port)
                .build();
        observer.onNext(reponse);
        observer.onCompleted();
    }
}
