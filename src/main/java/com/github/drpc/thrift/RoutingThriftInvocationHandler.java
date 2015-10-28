package com.github.drpc.thrift;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

import org.apache.thrift.TServiceClient;
import org.apache.thrift.transport.TTransportException;
import org.pacesys.kbop.IKeyedObjectPool;
import org.pacesys.kbop.IPooledObject;

import com.github.drpc.RouteKeyExtractor;
import com.github.drpc.ch.RendezvousHash2;
import com.github.drpc.membership.Membership;
import com.github.drpc.membership.Node;
import com.google.common.reflect.AbstractInvocationHandler;

class RoutingThriftInvocationHandler extends AbstractInvocationHandler {

    private final Object localService;
    private final Membership membership;
    private final RendezvousHash2<String, Node> consistentHash;
    private final RouteKeyExtractor routeKeyExtractor;

    private final Class<?> serviceClass;
    private final IKeyedObjectPool.Multi<NodeClientKey, TServiceClient> clientCache;

    public RoutingThriftInvocationHandler(Object impl, Membership membership,
            RendezvousHash2<String, Node> consistentHash, RouteKeyExtractor routeKeyExtractor,
            Class<?> serviceClass, IKeyedObjectPool.Multi<NodeClientKey, TServiceClient> clientCache) {

        this.localService = impl;
        this.membership = membership;
        this.consistentHash = consistentHash;
        this.routeKeyExtractor = routeKeyExtractor;
        this.serviceClass = serviceClass;
        this.clientCache = clientCache;
    }

    @Override
    protected Object handleInvocation(Object proxy, Method method, Object[] args) throws Throwable {
        String routingKey = routeKeyExtractor.extractRoutingKey(method, args);

        Node ownerNode = consistentHash.getOwnerNode(routingKey, membership.getPeers());

        if (membership.getId().equals(ownerNode.getId())) {
            return method.invoke(localService, args);
        } else {
            IPooledObject<TServiceClient> b = clientCache.borrow(new NodeClientKey(ownerNode,
                    serviceClass), 2, TimeUnit.SECONDS);
            TServiceClient client = b.get();
            boolean ok = true;
            try {
                return method.invoke(client, args);
            } catch (InvocationTargetException e) {
                if (e.getTargetException() instanceof TTransportException) {
                    ok = false;
                }

                throw e.getTargetException();
            } finally {
                if (ok) {
                    b.release();
                } else {
                    b.invalidate();
                }
            }
        }
    }
}
