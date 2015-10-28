package com.github.drpc.ch;

import java.util.NavigableSet;

import com.google.common.hash.Funnel;
import com.google.common.hash.HashFunction;

/**
 * <p>A high performance thread safe implementation of Rendezvous (Highest Random Weight, HRW) hashing is an algorithm that allows clients to achieve distributed agreement on which node (or proxy) a given
 * key is to be placed in. This implementation has the following properties.
 * <ul>
 * <li>Non-blocking reads : Determining which node a key belongs to is always non-blocking.  Adding and removing nodes however blocks each other</li>
 * <li>Low overhead: providing using a hash function of low overhead</li>
 * <li>Load balancing: Since the hash function is randomizing, each of the n nodes is equally likely to receive the key K. Loads are uniform across the sites.</li>
 * <li>High hit rate: Since all clients agree on placing an key K into the same node N , each fetch or placement of K into N yields the maximum utility in terms of hit rate. The key K will always be found unless it is evicted by some replacement algorithm at N.</li>
 * <li>Minimal disruption: When a node is removed, only the keys mapped to that node need to be remapped and they will be distributed evenly</li>
 * </ul>
 * </p>
 * source: https://en.wikipedia.org/wiki/Rendezvous_hashing
 *
 * @author Chris Lohfink
 *
 * @param <K>
 *            type of key
 * @param <N>
 *            type node/site or whatever want to be returned (ie IP address or String)
 */
public class RendezvousHash2<K, N extends Comparable<? super N>> {

    /**
     * A hashing function from guava, ie Hashing.murmur3_128()
     */
    private final HashFunction hasher;

    /**
     * A funnel to describe how to take the key and add it to a hash.
     *
     * @see com.google.common.hash.Funnel
     */
    private final Funnel<K> keyFunnel;

    /**
     * Funnel describing how to take the type of the node and add it to a hash
     */
    private final Funnel<N> nodeFunnel;

    /**
     * Creates a new RendezvousHash with a starting set of nodes provided by init. The funnels will be used when generating the hash that combines the nodes and
     * keys. The hasher specifies the hashing algorithm to use.
     */
    public RendezvousHash2(HashFunction hasher, Funnel<K> keyFunnel, Funnel<N> nodeFunnel) {
        if (hasher == null)
            throw new NullPointerException("hasher");
        if (keyFunnel == null)
            throw new NullPointerException("keyFunnel");
        if (nodeFunnel == null)
            throw new NullPointerException("nodeFunnel");
        this.hasher = hasher;
        this.keyFunnel = keyFunnel;
        this.nodeFunnel = nodeFunnel;
    }

    /**
     * return a node for a given key
     */
    public N getOwnerNode(K key, NavigableSet<N> nodes) {
        long maxValue = Long.MIN_VALUE;
        N max = null;
        for (N node : nodes) {
            long nodesHash = hasher.newHasher()
                    .putObject(key, keyFunnel)
                    .putObject(node, nodeFunnel)
                    .hash().asLong();
            if (nodesHash > maxValue) {
                max = node;
                maxValue = nodesHash;
            }
        }
        return max;
    }
}
