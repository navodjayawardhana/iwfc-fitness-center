package com.iwfc.support;

import com.iwfc.domain.model.FitnessSession;
import com.iwfc.domain.repository.Repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Behaves like a database: every read returns a fresh copy, so a change only survives if it is saved.
 * Tests use it to prove the schedule saves after each change.
 */
public final class CopyingSessionRepository implements Repository<FitnessSession, String> {

    private final Map<String, FitnessSession> stored = new LinkedHashMap<>();

    private static FitnessSession copy(FitnessSession session) {
        return FitnessSession.restore(session.id(), session.title(), session.instructor(), session.studio(),
                session.slot(), session.capacity(), session.equipment(), session.bookedMembers());
    }

    @Override
    public void save(FitnessSession session) {
        stored.put(session.id(), copy(session));
    }

    @Override
    public Optional<FitnessSession> findById(String id) {
        return Optional.ofNullable(stored.get(id)).map(CopyingSessionRepository::copy);
    }

    @Override
    public List<FitnessSession> findAll() {
        return stored.values().stream().map(CopyingSessionRepository::copy).toList();
    }

    @Override
    public boolean existsById(String id) {
        return stored.containsKey(id);
    }

    @Override
    public void deleteById(String id) {
        stored.remove(id);
    }

    @Override
    public int count() {
        return stored.size();
    }
}
