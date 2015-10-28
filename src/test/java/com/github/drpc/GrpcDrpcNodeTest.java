package com.github.drpc;

import io.grpc.Channel;
import io.grpc.examples.helloworld.GreeterGrpc;
import io.grpc.examples.helloworld.GreeterGrpc.Greeter;
import io.grpc.examples.helloworld.GreeterGrpc.GreeterBlockingStub;
import io.grpc.examples.helloworld.HelloRequest;
import io.grpc.examples.helloworld.HelloResponse;
import io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.NettyServerBuilder;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.github.drpc.grpc.GrpcDrpcServer;
import com.github.drpc.membership.Node;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

public class GrpcDrpcNodeTest {

    @Test
    public void test() throws Exception {
        StaticMembership node1 = new StaticMembership("node1", "localhost", 9001);

        StaticMembership node2 = node1.fork("node2", "localhost", 9002);

        System.out.println(node1);
        System.out.println(node2);

        GrpcDrpcServer rpc1 = new GrpcDrpcServer(node1);
        Channel ch1 = init(rpc1, 9001);
        rpc1.start();

        GrpcDrpcServer rpc2 = new GrpcDrpcServer(node2);
        Channel ch2 = init(rpc2, 9002);
        rpc2.start();

        GreeterBlockingStub client1 = GreeterGrpc.newBlockingStub(ch1);
        GreeterBlockingStub client2 = GreeterGrpc.newBlockingStub(ch2);

        HelloRequest req1 = HelloRequest.newBuilder().setName("name1").build();
        HelloRequest req2 = HelloRequest.newBuilder().setName("name2").build();
        HelloRequest req3 = HelloRequest.newBuilder().setName("name3").build();
        HelloRequest req4 = HelloRequest.newBuilder().setName("name4").build();

        HelloRequest[] all = new HelloRequest[] { req1, req2, req3, req4 };

        long start = System.nanoTime();
        int iterations = 30000;
        for (int i = 0; i < iterations; i++) {
            HelloRequest req;
            HelloResponse resp;

            req = all[(int) (Math.random() * all.length)];
            resp = client1.sayHello(req);
            //   System.out.println(req.getName() + "=" + resp.getMessage());

            req = all[(int) (Math.random() * all.length)];
            resp = client1.sayHello(req);
            //   System.out.println(req.getName() + "=" + resp.getMessage());

            req = all[(int) (Math.random() * all.length)];
            resp = client2.sayHello(req);

            req = all[(int) (Math.random() * all.length)];
            resp = client2.sayHello(req);
            // System.out.println(req.getName() + "=" + resp.getMessage());
        }

        long diff = System.nanoTime() - start;
        System.out.println(String.format("%.2f req/s", iterations * 4.0
                / (diff / 1000 / 1000 / 1000)));
    }

    private Channel init(GrpcDrpcServer node, int port) {
        GrpcRouteKeyExtractor routeKeyExtractor = new GrpcRouteKeyExtractor();
        node.setRouteKeyExtractor(routeKeyExtractor);

        Cache<Node, Channel> channelCache = CacheBuilder.newBuilder().maximumSize(20)
                .expireAfterAccess(3, TimeUnit.MINUTES)
                .expireAfterWrite(1, TimeUnit.MINUTES)
                .build();

        node.setChannelCache(channelCache);

        NettyServerBuilder serverBuilder = NettyServerBuilder.forPort(port).executor(
                Executors.newFixedThreadPool(20));
        node.setServerBuilder(serverBuilder);

        node.addService(Greeter.class, new GrpcGreeterImpl(port));

        Channel ch = NettyChannelBuilder.forAddress("localhost", port)
                .usePlaintext(true)
                .build();
        return ch;
    }
}
