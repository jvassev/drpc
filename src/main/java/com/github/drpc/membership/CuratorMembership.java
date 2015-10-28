package com.github.drpc.membership;

import java.util.Collections;
import java.util.List;
import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentSkipListSet;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.CuratorWatcher;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.utils.PathUtils;
import org.apache.curator.utils.ZKPaths;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.KeeperException.NodeExistsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CuratorMembership implements Membership, CuratorWatcher {
    private static Logger logger = LoggerFactory.getLogger(CuratorMembership.class);

    private final CuratorFramework curator;
    private final String group;
    private final String path;
    private volatile NavigableSet<Node> peers;
    private final Node self;

    public CuratorMembership(CuratorFramework curator, String group, Node self) {
        this.curator = curator;
        this.group = group;
        this.self = self;
        this.peers = new ConcurrentSkipListSet<Node>();
        path = "/membership/" + group;
        PathUtils.validatePath(path, false);
    }

    public String getGroup() {
        return group;
    }

    @Override
    public Node getSelf() {
        return self;
    }

    @Override
    public void start() throws Exception {
        if (curator.getState() == CuratorFrameworkState.LATENT) {
            curator.start();
        }

        try {
            curator.create().creatingParentsIfNeeded().withMode(CreateMode.PERSISTENT)
                    .forPath(path);
        } catch (NodeExistsException e) {
            logger.info("parent path already exists");
        }

        setupWatch();

        byte[] data = (self.getHost() + ":" + self.getPort()).getBytes();

        curator.create()
                .withMode(CreateMode.EPHEMERAL)
                .forPath(path + "/" + self.getId(), data);
    }

    private void setupWatch() throws Exception {
        curator.getChildren().usingWatcher(this).forPath(path);
    }

    @Override
    public String getId() {
        return self.getId();
    }

    @Override
    public NavigableSet<Node> getPeers() {
        return peers;
    }

    @Override
    public void process(WatchedEvent event) throws Exception {
        try {
            List<String> children = curator.getChildren().forPath(path);

            TreeSet<Node> nodes = new TreeSet<Node>();
            for (String child : children) {
                String fullPath = ZKPaths.makePath(path, "/" + child);

                byte[] data = curator.getData().forPath(fullPath);
                String stringData = new String(data);
                String parts[] = stringData.split(":");
                String host = parts[0];
                int port = Integer.parseInt(parts[1]);

                nodes.add(new Node(child, host, port));
            }

            this.peers = Collections.unmodifiableNavigableSet(nodes);
        } catch (Exception e) {
            System.out.println("missed event, will resubscribe");
        }

        setupWatch();
    }

    @Override
    public String toString() {
        return "CuratorMembership [self=" + self.getId() + ", peers=" + peers + "]";
    }
}
