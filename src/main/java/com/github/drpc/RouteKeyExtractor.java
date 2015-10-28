package com.github.drpc;


public interface RouteKeyExtractor {
    String extractRoutingKey(Object descriptor, Object[] args);
}
