package com.github.drpc.ch;

public interface ConsistenHash<K, N> {

    boolean add(N node);

    boolean remove(N node);

    N getOwnerNode(K key);
}