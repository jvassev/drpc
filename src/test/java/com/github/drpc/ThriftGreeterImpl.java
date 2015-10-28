package com.github.drpc;

import org.apache.thrift.TException;

import com.github.drpc.thrift.GreeterService;
import com.github.drpc.thrift.HelloRequest;
import com.github.drpc.thrift.HelloResponse;

public class ThriftGreeterImpl implements GreeterService.Iface {

    private int port;

    public ThriftGreeterImpl(int port) {
        this.port = port;
    }

    @Override
    public HelloResponse sayHello(HelloRequest req) throws TException {
        HelloResponse res = new HelloResponse("" + port);
        return res;
    }
}
