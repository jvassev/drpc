package com.github.drpc.grpc;

import java.util.UUID;

import com.github.drpc.AbstractDrpcServer;
import com.github.drpc.DrpcServer;
import com.github.drpc.membership.Membership;
import com.github.drpc.membership.Node;
import com.google.common.cache.Cache;
import io.grpc.BindableService;
import io.grpc.Channel;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import net.sf.cglib.proxy.Enhancer;

public final class GrpcDrpcServer extends AbstractDrpcServer implements DrpcServer {

    private ServerBuilder<?> serverBuilder;
    private Server server;

    private Cache<Node, Channel> channelCache;
    private final String name;

    public GrpcDrpcServer(Membership membership) {
        super(membership);
        name = UUID.randomUUID().toString();
    }

    public void setServerBuilder(ServerBuilder<?> sb) {
        this.serverBuilder = sb;
    }

    @Override
    public void start() throws Exception {
        membership.start();

        server = serverBuilder.build();
        server.start();
    }

    @Override
    public void awaitTermination() {
        try {
            server.awaitTermination();
        } catch (InterruptedException e) {
        }
    }

    @Override
    public void stop() {
        server.shutdown();
    }

    @Override
    public <T> void addService(Class<T> serviceType, T service) {
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(serviceType);
        RoutingInvocationHandler callback = new RoutingInvocationHandler(
                (BindableService) service, membership,
                consistentHash, routeKeyExtractor,
                serviceType, channelCache);

        enhancer.setCallback(callback);
        BindableService proxy = (BindableService) enhancer.create();

        serverBuilder.addService(proxy);
    }

    public void setChannelCache(Cache<Node, Channel> channelCache) {
        this.channelCache = channelCache;
    }
}
