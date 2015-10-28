package com.github.drpc;

import com.github.drpc.membership.Membership;

public interface DrpcServer {

    Membership getMembership();

    void start() throws Exception;

    void awaitTermination();

    void stop();

    <T> void addService(Class<T> svcClass, T impl);

    void setRouteKeyExtractor(RouteKeyExtractor routeKeyExtractor);

}