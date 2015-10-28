package com.github.drpc.thrift;

import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.protocol.TMultiplexedProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransportException;

public class ThriftDrpcClientFactory {
    @SuppressWarnings("unchecked")
    public static <T> T makeClient(Class<T> clientType, String host, int port)
            throws TTransportException {

        Class<?> facadeClass = clientType.getEnclosingClass();
        ClassLoader cl = facadeClass.getClassLoader();
        Class<?> clientClass;
        try {
            clientClass = cl.loadClass(facadeClass.getName() + "$Client");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        String serviceName = facadeClass.getSimpleName();

        TSocket socket1 = new TSocket(host, port);

        TMultiplexedProtocol protocol = new TMultiplexedProtocol(
                new TCompactProtocol(new TFramedTransport(socket1)), serviceName);

        Object res;

        try {
            res = clientClass.getConstructor(new Class[] { TProtocol.class })
                    .newInstance(protocol);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }

        socket1.open();
        return (T) res;
    }
}
