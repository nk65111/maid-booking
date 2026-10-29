package com.rupeek.maidbooking.maid;

import java.util.List;
import java.util.Optional;

public interface MaidRepository {

    Maid save(Maid maid);

    Optional<Maid> findById(String id);

    Optional<Maid> findByPhone(String phone);

    List<Maid> findAll();
}
