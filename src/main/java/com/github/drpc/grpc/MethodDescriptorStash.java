package com.github.drpc.grpc;

import io.grpc.MethodDescriptor;

class MethodDescriptorStash {
    private static final ThreadLocal<MethodDescriptor<?, ?>> theStash = new ThreadLocal<>();

    public static void set(MethodDescriptor<?, ?> m) {
        theStash.set(m);
    }

    public static MethodDescriptor<?, ?> get() {
        return theStash.get();
    }
}
