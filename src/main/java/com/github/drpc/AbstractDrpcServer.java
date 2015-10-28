package com.github.drpc;

import java.nio.charset.Charset;

import com.github.drpc.ch.RendezvousHash2;
import com.github.drpc.membership.Membership;
import com.github.drpc.membership.Node;
import com.google.common.hash.Funnel;
import com.google.common.hash.Funnels;
import com.google.common.hash.Hashing;

public abstract class AbstractDrpcServer implements DrpcServer {

    public Membership getMembership() {
        return membership;
    }

    protected final Membership membership;
    protected final RendezvousHash2<String, Node> consistentHash;
    protected RouteKeyExtractor routeKeyExtractor;

    @SuppressWarnings("unchecked")
    protected final Funnel<String> stringFunnel = (Funnel<String>) (Funnel<?>) Funnels
            .stringFunnel(Charset
                    .forName("UTF-8"));
    protected final Funnel<Node> nodeFunnel = (obj, sink) -> {
        stringFunnel.funnel(obj.getId(), sink);
    };

    public AbstractDrpcServer(Membership membership) {
        this.membership = membership;
        consistentHash = new RendezvousHash2<>(Hashing.murmur3_128(), stringFunnel, nodeFunnel);
    }

    @Override
    public void setRouteKeyExtractor(RouteKeyExtractor routeKeyExtractor) {
        this.routeKeyExtractor = routeKeyExtractor;
    }
}