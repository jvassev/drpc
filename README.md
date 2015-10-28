# What is this for?
The project was insipred by this blob post
https://speakerdeck.com/caitiem20/building-scalable-stateful-services. The idea
is simple: instead of disitrbuting state, route computation to where state
already is. While the original blog post also talks about the actor model drpc
focuses on the "routing" part and shows a practical implentation based on gRPC
and Thrift.
If your cluster has N nodes, 1/N request will directly hit the owner node while
the other (N-1)/N will have to be forwarded to the owner node.

# Building blocks
So let's say the "state" is only present in several thrift/grpc servers lying
around. Every server has only a subset of all the data. A request can be
handled by any of the servers. However if a request arrives on a node/server
that doesn't own the data is should operate on it is transparently routed to
the data owning node. The task is handled by several services:

## Membership
Before a request is routed, the set of all nodes should be known. Nodes can
come and go but every node needs a view of this p2p network
```java
public interface Membership {
    void start() throws Exception;
    String getId();
    Node getSelf();
    NavigableSet<Node> getPeers();
}
```
A naive StaticMembership and CuratorMembership (based on zookeeper membership)
are included.
## RouteKeyExtractor
```java
public interface RouteKeyExtractor {
    String extractRoutingKey(Object descriptor, Object[] args);
}
```
A routeKeyExtractor assignes a routing key to a RPC request. The descriptor
parameter describes the remote method being invoked. In the case of gRPC it's
an instance of
https://github.com/grpc/grpc-java/blob/master/core/src/main/java/io/grpc/MethodDescriptor.java.
The Thrift implementation passes a java.lang.reflect.Method of the
ServiceClass.Iface interface.
Most probably you would be building a routing key based on the descriptor and
one of the args, for example accountId, or anything that you initally used to
distribute state.
## ConsistentHash
A Renedezvous hash is used to uniquely map a routing key to a server node. I am
using https://github.com/clohfink/RendezvousHash. The original code is changed
a bit to make it work with the Memebership abstraction. This part of the
framework is not customizeable.

## DrpcServer
A DrpcServer is modelled after a io.grpc.Server and has methods for handling
its lifecycle and and configuring Membership and RouteKeyExtractor. The code is
very opinionated to make it easy to use.
```java
public interface DrpcServer {
    Membership getMembership();
    void start() throws Exception;
    void awaitTermination();
    void stop();
    <T> void addService(Class<T> svcClass, T impl);
    void setRouteKeyExtractor(RouteKeyExtractor routeKeyExtractor);
}
```
A gRPC and Thrift implentations are provided.

# Examples
Have a look at the unit tests:
* Thrift:
  https://github.com/jvassev/drpc/tree/master/src/test/java/com/github/drpc/ThriftDrpcNodeTest.java
* gRPC:
  https://github.com/jvassev/drpc/tree/master/src/test/java/com/github/drpc/GrpcDrpcNodeTest.java

The example GreeterService from the gRPC test project is used. It is translated
to Thrift to make comparisons easier.

# gRPC notes
Drpc uses reflection to generate a runtime proxy of the remote stub. It
delegates local calls to the instance provided when building the DrpcServer.
Remote calls are routed on the network. A guava cache is stores the channels to
the members. The implementation make a lot of assumptions about the code
generated by gRPC: if its layout changes Drpc has to change as well.

# Thrift notes
The Thrift implementation is very similar to gRPC's. However, because Thrift
clients are not thread-safe so extra care is taken to cache AND pool them. The ThriftDrcpServer uses TNonblockingServerSocket and a TMultiplexProcessor and a TCompocatProtocol. To make creating compatible clients easier use the class com.github.drpc.thrift.ThriftDrpcClientFactory.

#What can be improved
The most visible flaw of Drpc is that it has to parse the request before routing it. Ideally only the headers could be parsed and the routing key extracted. The body/stream should just be piped to the remote service.
