package com.example.rbcs.domain.event;

import lombok.Getter;

@Getter
public class DomainEvent<T> {
    private final T aggregateRoot;
    private final long timestamp;

    public DomainEvent(T aggregateRoot) {
        this.aggregateRoot = aggregateRoot;
        timestamp = System.currentTimeMillis();
    }
}
