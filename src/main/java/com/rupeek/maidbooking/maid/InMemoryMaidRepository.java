package com.rupeek.maidbooking.maid;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Repository;

@Repository
public class InMemoryMaidRepository implements MaidRepository {

    private final Map<String, Maid> maids = new ConcurrentHashMap<>();

    @Override
    public Maid save(Maid maid) {
        maids.put(maid.id(), maid);
        return maid;
    }

    @Override
    public Optional<Maid> findById(String id) {
        return Optional.ofNullable(maids.get(id));
    }

    @Override
    public Optional<Maid> findByPhone(String phone) {
        return maids.values().stream().filter(maid -> maid.phone().equals(phone)).findFirst();
    }

    @Override
    public List<Maid> findAll() {
        return List.copyOf(maids.values());
    }
}
