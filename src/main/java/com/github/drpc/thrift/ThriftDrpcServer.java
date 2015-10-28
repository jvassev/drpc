package com.github.drpc.thrift;

import java.lang.reflect.Constructor;

import org.apache.thrift.TMultiplexedProcessor;
import org.apache.thrift.TProcessor;
import org.apache.thrift.TServiceClient;
import org.apache.thrift.server.AbstractNonblockingServer;
import org.apache.thrift.transport.TTransportException;
import org.pacesys.kbop.IKeyedObjectPool;
import org.pacesys.kbop.IPoolObjectFactory;
import org.pacesys.kbop.PoolKey;
import org.pacesys.kbop.Pools;

import com.github.drpc.AbstractDrpcServer;
import com.github.drpc.membership.Membership;
import com.github.drpc.membership.Node;
import com.google.common.reflect.Reflection;

public class ThriftDrpcServer extends AbstractDrpcServer {

    private TMultiplexedProcessor multiplexer;

    private AbstractNonblockingServer server;

    private ThriftServerFactory serverFactory;

    private IKeyedObjectPool.Multi<NodeClientKey, TServiceClient> clientPool;

    public ThriftDrpcServer(Membership membership) {
        super(membership);
        multiplexer = new TMultiplexedProcessor();
    }

    public void setMaxClientsPerServicePerNode(int n) {
        this.clientPool = Pools.<NodeClientKey, TServiceClient> createMultiPool(
                new IPoolObjectFactory<NodeClientKey, TServiceClient>() {

                    @Override
                    public TServiceClient create(PoolKey<NodeClientKey> key) {
                        Node node = key.get().node;
                        Class<?> type = key.get().type;
                        try {
                            return (TServiceClient) ThriftDrpcClientFactory.makeClient(type,
                                    node.getHost(), node.getPort());
                        } catch (TTransportException e) {
                            throw new RuntimeException(e);
                        }
                    }

                    @Override
                    public void activate(TServiceClient object) {
                    }

                    @Override
                    public void passivate(TServiceClient object) {
                    }

                    @Override
                    public void destroy(TServiceClient object) {
                    }
                }, n);
    }

    @Override
    public void start() throws Exception {
        server = serverFactory.makeServer(multiplexer);
        new Thread(() -> server.serve()).start();
    }

    public ThriftServerFactory getServerFactory() {
        return serverFactory;
    }

    public void setServerFactory(ThriftServerFactory serverFactory) {
        this.serverFactory = serverFactory;
    }

    @Override
    public void awaitTermination() {
        server.stop();
    }

    @Override
    public void stop() {
        server.stop();
    }

    @Override
    public <T> void addService(Class<T> svcClass, T impl) {
        Class<?> hostClass = svcClass.getEnclosingClass();
        ClassLoader classLoader = hostClass.getClassLoader();

        try {
            String processorClassName = hostClass.getName() + "$Processor";
            Class<?> processorClass = classLoader.loadClass(processorClassName);
            Constructor<?> ctor = processorClass.getConstructor(new Class[] { svcClass });

            RoutingThriftInvocationHandler handler = new RoutingThriftInvocationHandler(impl,
                    membership, consistentHash,
                    routeKeyExtractor, svcClass, clientPool);

            Object proxy = Reflection.newProxy(svcClass, handler);

            TProcessor processor = (TProcessor) ctor.newInstance(proxy);

            multiplexer.registerProcessor(hostClass.getSimpleName(), processor);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
