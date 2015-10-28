package com.github.drpc.grpc;

import io.grpc.CallOptions;
import io.grpc.ClientCall;
import io.grpc.ManagedChannel;
import io.grpc.MethodDescriptor;

import java.util.concurrent.TimeUnit;

final class FakeChannel extends ManagedChannel {
    @Override
    public <RequestT, ResponseT> ClientCall<RequestT, ResponseT> newCall(
            MethodDescriptor<RequestT, ResponseT> methodDescriptor, CallOptions callOptions) {
        return null;
    }

    @Override
    public String authority() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ManagedChannel shutdownNow() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ManagedChannel shutdown() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isTerminated() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isShutdown() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        // TODO Auto-generated method stub
        return false;
    }
}