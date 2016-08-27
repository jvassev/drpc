package com.github.drpc.membership;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class CuratorMembershipTest {

    @Test
    public void test() throws Exception {

        CuratorMembership m = new CuratorMembership(makeCurator(), "test2", new Node("id-1",
                "localhost",
                8001));

        CuratorMembership m2 = new CuratorMembership(makeCurator(), "test2", new Node("id-2",
                "localhost",
                8002));
        m.start();
        m2.start();

        Thread.sleep(1000);
        System.out.println(m);
        System.out.println(m2);
    }

    private CuratorFramework makeCurator() {
        RetryPolicy rp = new ExponentialBackoffRetry(500, 20, 1000);
        return CuratorFrameworkFactory.newClient("172.17.0.1", 5 * 1000,
                30 * 1000, rp);
    }
}
