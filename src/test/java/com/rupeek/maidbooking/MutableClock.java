package com.rupeek.maidbooking;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class MutableClock extends Clock {

    private final ZoneId zone;
    private Instant instant;

    public MutableClock(LocalDateTime now, ZoneId zone) {
        this.zone = zone;
        set(now);
    }

    public void set(LocalDateTime now) {
        this.instant = now.atZone(zone).toInstant();
    }

    @Override
    public ZoneId getZone() {
        return zone;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return new MutableClock(LocalDateTime.ofInstant(instant, zone), zone);
    }

    @Override
    public Instant instant() {
        return instant;
    }
}
