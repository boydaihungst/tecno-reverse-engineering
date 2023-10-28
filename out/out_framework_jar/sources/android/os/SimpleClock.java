package android.os;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
/* loaded from: classes2.dex */
public abstract class SimpleClock extends Clock {
    private final ZoneId zone;

    @Override // java.time.Clock
    public abstract long millis();

    public SimpleClock(ZoneId zone) {
        this.zone = zone;
    }

    @Override // java.time.Clock
    public ZoneId getZone() {
        return this.zone;
    }

    @Override // java.time.Clock
    public Clock withZone(ZoneId zone) {
        return new SimpleClock(zone) { // from class: android.os.SimpleClock.1
            @Override // android.os.SimpleClock, java.time.Clock
            public long millis() {
                return SimpleClock.this.millis();
            }
        };
    }

    @Override // java.time.Clock
    public Instant instant() {
        return Instant.ofEpochMilli(millis());
    }
}
