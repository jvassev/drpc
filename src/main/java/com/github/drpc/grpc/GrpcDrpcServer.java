package com.github.drpc.grpc;

import io.grpc.Channel;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerCall;
import io.grpc.ServerCall.Listener;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.ServerInterceptors;
import io.grpc.ServerServiceDefinition;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import com.github.drpc.AbstractDrpcServer;
import com.github.drpc.DrpcServer;
import com.github.drpc.membership.Membership;
import com.github.drpc.membership.Node;
import com.google.common.cache.Cache;
import com.google.common.reflect.Reflection;

public final class GrpcDrpcServer extends AbstractDrpcServer implements DrpcServer {

    private ServerBuilder<?> serverBuilder;

    private Server server;

    private Cache<Node, Channel> channelCache;

    public GrpcDrpcServer(Membership membership) {
        super(membership);
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
    public <T> void addService(Class<T> svcClass, T impl) {
        InvocationHandler handler = new RoutingInvocationHandler(impl, membership, consistentHash,
                routeKeyExtractor, svcClass.getEnclosingClass(), svcClass, channelCache);

        T bindableService = Reflection.newProxy(svcClass, handler);

        Method bindServiceMethod = findBindMethod(svcClass);

        try {
            Object def = bindServiceMethod.invoke(null, bindableService);

            ServerServiceDefinition intercepted = ServerInterceptors.intercept(
                    (ServerServiceDefinition) def,
                    new ServerInterceptor() {
                        @Override
                        public <ReqT, RespT> Listener<ReqT> interceptCall(
                                MethodDescriptor<ReqT, RespT> method, ServerCall<RespT> call,
                                Metadata headers, ServerCallHandler<ReqT, RespT> next) {
                            try {
                                MethodDescriptorStash.set(method);
                                return next.startCall(method, call, headers);
                            } finally {
                                MethodDescriptorStash.set(null);
                            }
                        }
                    });
            serverBuilder.addService(intercepted);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("gRPC changed incompatibly", e);
        }
    }

    private <T> Method findBindMethod(Class<T> svc) {
        Class<?> grpc = svc.getEnclosingClass();

        try {
            Method bindServiceMethod = grpc.getMethod("bindService", new Class[] { svc });
            return bindServiceMethod;
        } catch (NoSuchMethodException | SecurityException e) {
            throw new RuntimeException("gRPC changed incompatibly", e);
        }
    }

    public void setChannelCache(Cache<Node, Channel> channelCache) {
        this.channelCache = channelCache;
    }
}
