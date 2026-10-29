package com.rupeek.maidbooking.maid;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.rupeek.maidbooking.common.ConflictException;
import com.rupeek.maidbooking.common.KeyedLocks;
import com.rupeek.maidbooking.common.NotFoundException;

@Service
public class MaidService {

    private final MaidRepository maids;
    private final KeyedLocks locks;

    public MaidService(MaidRepository maids, KeyedLocks locks) {
        this.maids = maids;
        this.locks = locks;
    }

    public Maid onboard(OnboardMaidRequest request) {
        Maid maid = new Maid(UUID.randomUUID().toString(), request.name(), request.phone(), request.locality(),
                request.gender(), request.hourlyRates(), request.availability(), request.rating());

        // phone is the natural identity of a maid; lock on it so two parallel onboardings can't both pass the check
        return locks.withLock("maid-phone:" + maid.phone(), () -> {
            if (maids.findByPhone(maid.phone()).isPresent()) {
                throw new ConflictException("A maid with phone " + maid.phone() + " is already onboarded");
            }
            return maids.save(maid);
        });
    }

    public Maid get(String id) {
        return maids.findById(id).orElseThrow(() -> new NotFoundException("Maid " + id + " not found"));
    }
}
