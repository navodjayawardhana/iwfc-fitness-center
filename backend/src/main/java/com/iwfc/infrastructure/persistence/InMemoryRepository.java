package com.iwfc.infrastructure.persistence;

import com.iwfc.domain.repository.Repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * Adapter: keeps entities in a {@link LinkedHashMap} (no database needed for the prototype).
 * One generic class serves every entity; it only needs to know how to read an id from an item.
 */
public class InMemoryRepository<T, ID> implements Repository<T, ID> {

    private final Map<ID, T> items = new LinkedHashMap<>();
    private final Function<T, ID> idOf;

    public InMemoryRepository(Function<T, ID> idOf) {
        this.idOf = Objects.requireNonNull(idOf);
    }

    @Override
    public void save(T item) {
        Objects.requireNonNull(item, "Cannot save null");
        items.put(idOf.apply(item), item);
    }

    @Override
    public Optional<T> findById(ID id) {
        return Optional.ofNullable(items.get(id));
    }

    @Override
    public List<T> findAll() {
        return List.copyOf(items.values());
    }

    @Override
    public boolean existsById(ID id) {
        return items.containsKey(id);
    }

    @Override
    public void deleteById(ID id) {
        items.remove(id);
    }

    @Override
    public int count() {
        return items.size();
    }
}
