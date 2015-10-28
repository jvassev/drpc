package com.github.drpc.ch;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

import com.github.drpc.ch.ConsistenHash;
import com.github.drpc.ch.NaiveConsistentHash;
import com.github.drpc.ch.RendezvousHash;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.hash.Funnel;
import com.google.common.hash.Funnels;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

/**
 * For comparing the load differences between consistent hash and HRW
 */
public class Compare {
    private static final HashFunction hfunc = Hashing.murmur3_128();
    private static final Funnel<CharSequence> strFunnel = Funnels.stringFunnel(Charset
            .defaultCharset());

    public static void main(String[] args) {
        Map<String, AtomicInteger> distribution = Maps.newHashMap();

        System.out.println("======: ConsistentHash :========");
        ConsistenHash<String, String> c = new NaiveConsistentHash(hfunc, strFunnel, strFunnel,
                getNodes(distribution));
        for (int i = 0; i < 100000; i++) {
            distribution.get(c.getOwnerNode("" + i)).incrementAndGet();
        }

        for (Entry<String, AtomicInteger> e : distribution.entrySet()) {
            System.out.println(e.getKey() + "," + e.getValue().get());
        }
        printStats(distribution);
        for (Entry<String, AtomicInteger> e : distribution.entrySet()) {
            e.getValue().set(0);
        }

        System.out.println("====== remove 2 ========");
        for (int i = 0; i < 2; i++) {
            c.remove("Node" + i);
            distribution.remove("Node" + i);
        }
        for (int i = 0; i < 100000; i++) {
            distribution.get(c.getOwnerNode("" + i)).incrementAndGet();
        }
        for (Entry<String, AtomicInteger> e : distribution.entrySet()) {
            System.out.println(e.getKey() + "," + e.getValue().get());
        }
        printStats(distribution);

        System.out.println("======: RendezvousHash :========");
        distribution = Maps.newHashMap();
        RendezvousHash<String, String> r = new RendezvousHash(hfunc, strFunnel, strFunnel,
                getNodes(distribution));

        for (int i = 0; i < 100000; i++) {
            distribution.get(r.getOwnerNode("" + i)).incrementAndGet();
        }
        for (Entry<String, AtomicInteger> e : distribution.entrySet()) {
            System.out.println(e.getKey() + "," + e.getValue().get());
        }
        printStats(distribution);
        for (Entry<String, AtomicInteger> e : distribution.entrySet()) {
            e.getValue().set(0);
        }

        System.out.println("====== remove 2 ========");
        for (int i = 0; i < 2; i++) {
            r.remove("Node" + i);
            distribution.remove("Node" + i);
        }
        for (int i = 0; i < 100000; i++) {
            distribution.get(r.getOwnerNode("" + i)).incrementAndGet();
        }
        for (Entry<String, AtomicInteger> e : distribution.entrySet()) {
            System.out.println(e.getKey() + "," + e.getValue().get());
        }
        printStats(distribution);
    }

    private static void printStats(Map<String, AtomicInteger> distribution) {
        double avg = 0;
        for (AtomicInteger i : distribution.values()) {
            avg += i.longValue();
        }

        avg = avg / distribution.size();

        double ssq = 0;
        for (AtomicInteger i : distribution.values()) {
            double d = avg - i.doubleValue();
            ssq += d * d;
        }

        ssq = ssq / distribution.size();
        ssq = Math.sqrt(ssq);

        System.out.println(String.format("avg = %.2f, std = %.2f", avg, ssq));
    }

    private static List<String> getNodes(Map<String, AtomicInteger> distribution) {
        List<String> nodes = Lists.newArrayList();
        for (int i = 0; i < 5; i++) {
            nodes.add("Node" + i);
            distribution.put("Node" + i, new AtomicInteger());
        }
        return nodes;
    }
}
