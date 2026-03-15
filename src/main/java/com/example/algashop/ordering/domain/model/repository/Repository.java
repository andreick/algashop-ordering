package com.example.algashop.ordering.domain.model.repository;

import com.example.algashop.ordering.domain.model.entity.AggregateRoot;

import java.util.Optional;

public interface Repository<T extends AggregateRoot<ID>, ID> {

    Optional<T> ofId(ID id);

    boolean exists(ID id);

    void add(T aggregateRoot);

    long count();
}
