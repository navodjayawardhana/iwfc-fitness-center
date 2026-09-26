package com.iwfc.domain.repository;

import java.util.List;
import java.util.Optional;

/**
 * Port owned by the domain: how entities are stored, without saying where (Dependency Inversion).
 * Generic over the entity type {@code T} and its id type {@code ID}.
 */
public interface Repository<T, ID> {

    /** Adds the item, or replaces the stored item with the same id. */
    void save(T item);

    Optional<T> findById(ID id);

    /** A read-only snapshot in insertion order. */
    List<T> findAll();

    boolean existsById(ID id);

    void deleteById(ID id);

    int count();
}
