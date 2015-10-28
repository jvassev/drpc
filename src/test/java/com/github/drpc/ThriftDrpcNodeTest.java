package com.github.drpc;

import org.apache.thrift.TMultiplexedProcessor;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.server.AbstractNonblockingServer;
import org.apache.thrift.server.TThreadedSelectorServer;
import org.apache.thrift.server.TThreadedSelectorServer.Args;
import org.apache.thrift.transport.TNonblockingServerSocket;
import org.apache.thrift.transport.TNonblockingServerTransport;
import org.apache.thrift.transport.TTransportException;
import org.junit.Test;
import com.github.drpc.thrift.GreeterService;
import com.github.drpc.thrift.HelloRequest;
import com.github.drpc.thrift.HelloResponse;
import com.github.drpc.thrift.ThriftDrpcClientFactory;
import com.github.drpc.thrift.ThriftDrpcServer;
import com.github.drpc.thrift.ThriftServerFactory;

public class ThriftDrpcNodeTest {

    @Test
    public void test() throws Exception {
        StaticMembership node1 = new StaticMembership("node1", "localhost", 9001);

        StaticMembership node2 = node1.fork("node2", "localhost", 9002);

        System.out.println(node1);
        System.out.println(node2);

        ThriftDrpcServer rpc1 = new ThriftDrpcServer(node1);
        init(rpc1, 9001);
        rpc1.start();

        ThriftDrpcServer rpc2 = new ThriftDrpcServer(node2);
        init(rpc2, 9002);
        rpc2.start();

        HelloRequest req1 = new HelloRequest("name1");
        HelloRequest req2 = new HelloRequest("name2");
        HelloRequest req3 = new HelloRequest("name3");
        HelloRequest req4 = new HelloRequest("name4");

        HelloRequest[] all = new HelloRequest[] { req1, req2, req3, req4 };

        GreeterService.Iface client1 = ThriftDrpcClientFactory.makeClient(
                GreeterService.Iface.class, "localhost", 9001);

        GreeterService.Iface client2 = ThriftDrpcClientFactory.makeClient(
                GreeterService.Iface.class, "localhost", 9002);

        long start = System.nanoTime();
        int iterations = 30000;
        for (int i = 0; i < iterations; i++) {
            HelloRequest req;
            HelloResponse resp;

            req = all[(int) (Math.random() * all.length)];
            resp = client1.sayHello(req);
            // System.out.println(req.getName() + "=" + resp.getMessage());

            req = all[(int) (Math.random() * all.length)];
            resp = client1.sayHello(req);
            // System.out.println(req.getName() + "=" + resp.getMessage());

            req = all[(int) (Math.random() * all.length)];
            resp = client2.sayHello(req);

            req = all[(int) (Math.random() * all.length)];
            resp = client2.sayHello(req);
            //  System.out.println(req.getName() + "=" + resp.getMessage());
        }

        long diff = System.nanoTime() - start;
        System.out.println(String.format("%.2f req/s", iterations * 4.0
                / diff * 1000 * 1000 * 1000));
    }

    private void init(ThriftDrpcServer rpc, int port) {
        rpc.setMaxClientsPerServicePerNode(10);

        rpc.setRouteKeyExtractor(new ThriftRouteKeyExtractor());

        rpc.setServerFactory(new ThriftServerFactory() {

            @Override
            public AbstractNonblockingServer makeServer(TMultiplexedProcessor multiplexer) {
                TNonblockingServerTransport trans;
                try {
                    trans = new TNonblockingServerSocket(port, 30000);
                } catch (TTransportException e) {
                    throw new RuntimeException(e);
                }

                Args args = new Args(trans);
                args.processor(multiplexer);
                args.protocolFactory(new TCompactProtocol.Factory());
                TThreadedSelectorServer res = new TThreadedSelectorServer(args);
                return res;
            }
        });

        rpc.addService(GreeterService.Iface.class, new ThriftGreeterImpl(port));
    }
}
