package com.example.rbcs.infrastructure.defer;

public interface Defers {
    void deferOnTransactionComplete(Runnable runnable);
}
