package com.example.algashop.ordering.domain.model.repository;

public interface VersionSynchronizer<T> {

    void synchronizeVersion(T entity, Long version);
}
