package com.example.algashop.ordering.domain.model;

public interface VersionSynchronizer<T> {

    void synchronizeVersion(T entity, Long version);
}
