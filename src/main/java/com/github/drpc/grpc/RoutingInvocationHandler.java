package com.github.drpc.grpc;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import com.github.drpc.RouteKeyExtractor;
import com.github.drpc.ch.RendezvousHash2;
import com.github.drpc.membership.Membership;
import com.github.drpc.membership.Node;
import com.google.common.cache.Cache;
import io.grpc.BindableService;
import io.grpc.Channel;
import io.grpc.netty.NettyChannelBuilder;
import io.grpc.stub.AbstractStub;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

class RoutingInvocationHandler implements MethodInterceptor {

    private final BindableService localService;
    private final Membership membership;
    private final RendezvousHash2<String, Node> consistentHash;
    private final RouteKeyExtractor routeKeyExtractor;
    private final Cache<Node, Channel> channelCache;

    private final Map<Method, Method> clientToServer;
    private final Class<?> serviceClass;
    private Method newStubMethod;

    public RoutingInvocationHandler(BindableService localService, Membership membership,
            RendezvousHash2<String, Node> consistentHash, RouteKeyExtractor routeKeyExtractor,
            Class<?> serviceClass, Cache<Node, Channel> channelCache) {

        this.localService = localService;
        this.membership = membership;
        this.consistentHash = consistentHash;
        this.routeKeyExtractor = routeKeyExtractor;
        this.serviceClass = serviceClass;
        this.channelCache = channelCache;

        try {
            newStubMethod = serviceClass.getEnclosingClass().getMethod("newStub", Channel.class);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("gRPC changed incompatibly", e);
        }

        this.clientToServer = new HashMap<>(10);
        initMapping();
    }

    private void initMapping() {
        Class<?> stubClass = newStubMethod.getReturnType();

        for (Method m : serviceClass.getMethods()) {
            try {
                Method found = stubClass.getMethod(m.getName(), m.getParameterTypes());
                clientToServer.put(m, found);
            } catch (ReflectiveOperationException e) {

            }
        }
    }

    @Override
    public Object intercept(Object target, Method method, Object[] args,
            MethodProxy methodProxy) throws Throwable {

        if ("bindService".equals(method.getName())) {
            return methodProxy.invokeSuper(target, args);
        }

        if (method.getDeclaringClass() == Object.class) {
            return methodProxy.invokeSuper(target, args);
        }
        String routingKey = routeKeyExtractor.extractRoutingKey(null, args);

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
        return channelCache.get(node, () -> NettyChannelBuilder
                .forAddress(node.getHost(), node.getPort())
                .usePlaintext(true)
                .userAgent("fwd:" + membership.getId()).build());
    }

    public AbstractStub<?> newStub(Channel ch) {
        try {
            return (AbstractStub<?>) newStubMethod.invoke(null, ch);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("gRPC changed incompatibly", e);
        }
    }
}
