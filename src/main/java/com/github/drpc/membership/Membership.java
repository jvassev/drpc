package com.github.drpc.membership;

import java.util.NavigableSet;

public interface Membership {

    void start() throws Exception;

    String getId();

    Node getSelf();

    NavigableSet<Node> getPeers();
}