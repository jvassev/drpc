package com.github.drpc.grpc;

import io.grpc.Channel;
import io.grpc.MethodDescriptor;
import io.grpc.netty.NettyChannelBuilder;
import io.grpc.stub.AbstractStub;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import com.github.drpc.RouteKeyExtractor;
import com.github.drpc.ch.RendezvousHash2;
import com.github.drpc.membership.Membership;
import com.github.drpc.membership.Node;
import com.google.common.cache.Cache;
import com.google.common.reflect.AbstractInvocationHandler;

class RoutingInvocationHandler extends AbstractInvocationHandler {

    private final Object localService;
    private final Membership membership;
    private final RendezvousHash2<String, Node> consistentHash;
    private final RouteKeyExtractor routeKeyExtractor;
    private final Class<?> grpcFacadeClass;
    private final Cache<Node, Channel> channelCache;

    private final Map<Method, Method> clientToServer;
    private final Class<?> serviceClass;

    public RoutingInvocationHandler(Object impl, Membership membership,
            RendezvousHash2<String, Node> consistentHash, RouteKeyExtractor routeKeyExtractor,
            Class<?> grpcFacadeClass, Class<?> serviceClass, Cache<Node, Channel> channelCache) {

        this.localService = impl;
        this.membership = membership;
        this.consistentHash = consistentHash;
        this.routeKeyExtractor = routeKeyExtractor;
        this.grpcFacadeClass = grpcFacadeClass;
        this.serviceClass = serviceClass;
        this.channelCache = channelCache;

        this.clientToServer = new HashMap<Method, Method>(10);
        initMapping();
    }

    private void initMapping() {
        AbstractStub<?> stub = newStub(new FakeChannel());
        for (Method m : serviceClass.getMethods()) {
            try {
                Method found = stub.getClass().getMethod(m.getName(), m.getParameterTypes());
                clientToServer.put(m, found);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException("gRPC changed incompatibly", e);
            }
        }
    }

    @Override
    protected Object handleInvocation(Object proxy, Method method, Object[] args) throws Throwable {
        MethodDescriptor<?, ?> descriptor = MethodDescriptorStash.get();

        String routingKey = routeKeyExtractor.extractRoutingKey(descriptor, args);

        Node ownerNode = consistentHash.getOwnerNode(routingKey, membership.getPeers());

        if (membership.getId().equals(ownerNode.getId())) {
            return method.invoke(localService, args);
        } else {
            Channel ch = getChannel(ownerNode);

            AbstractStub<?> stub = newStub(ch);

            return getCorrespondingStubMethod(method).invoke(stub, args);
        }
    }

    private Method getCorrespondingStubMethod(Method method)
            throws NoSuchMethodException {
        return clientToServer.get(method);
    }

    private Channel getChannel(Node node) throws ExecutionException {
        return channelCache.get(node, () -> {
            return NettyChannelBuilder.forAddress(node.getHost(), node.getPort())
                    .usePlaintext(true).
                    userAgent("fwd:" + membership.getId()).build();
        });
    }

    public AbstractStub<?> newStub(Channel ch) {
        try {
            Method newStubMethod = grpcFacadeClass.getMethod("newStub",
                    new Class[] { Channel.class });
            return (AbstractStub<?>) newStubMethod.invoke(null, ch);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException
                | NoSuchMethodException | SecurityException e) {
            throw new RuntimeException("gRPC changed incompatibly", e);
        }
    }
}
