package com.github.drpc;

import com.github.drpc.thrift.HelloRequest;

public class ThriftRouteKeyExtractor implements RouteKeyExtractor {

    @Override
    public String extractRoutingKey(Object descriptor, Object[] args) {
        HelloRequest req = (HelloRequest) args[0];
        return req.getName();
    }
}
