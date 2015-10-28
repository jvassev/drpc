package com.github.drpc.thrift;

import org.apache.thrift.TMultiplexedProcessor;
import org.apache.thrift.server.AbstractNonblockingServer;

public interface ThriftServerFactory {
    AbstractNonblockingServer makeServer(TMultiplexedProcessor multiplexer);
}
