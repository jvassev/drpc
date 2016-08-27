package com.github.drpc;

import io.grpc.examples.helloworld.HelloRequest;

public class GrpcRouteKeyExtractor implements RouteKeyExtractor {

    @Override
    public String extractRoutingKey(Object descriptor, Object[] args) {
        HelloRequest req = (HelloRequest) args[0];
        return req.getName();
    }
}
