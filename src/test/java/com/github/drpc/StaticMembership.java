package com.github.drpc;

import java.util.concurrent.ConcurrentSkipListSet;

import com.github.drpc.membership.Membership;
import com.github.drpc.membership.Node;

public class StaticMembership implements Membership {

    private ConcurrentSkipListSet<Node> peers;
    private String id;
    private String host;
    private int port;

    public StaticMembership(String id, String host, int port) {
        this.id = id;
        this.peers = new ConcurrentSkipListSet<Node>();
        peers.add(new Node(id, host, port));
    }

    public void add(String id, String host, int port) {
        peers.add(new Node(id, host, port));
    }

    @Override
    public void start() {

    }

    public StaticMembership fork(String newId, String newHost, int newPort) {
        StaticMembership res = new StaticMembership(newId, newHost, newPort);
        res.peers.addAll(this.peers);

        this.add(newId, newHost, newPort);
        return res;
    }

    @Override
    public Node getSelf() {
        return new Node(id, host, port);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public ConcurrentSkipListSet<Node> getPeers() {
        return peers;
    }

    @Override
    public String toString() {
        return "StaticMembership [self=" + id + ", peers=" + peers + "]";
    }

}
