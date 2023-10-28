package android.media;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
/* loaded from: classes2.dex */
public final class AudioTimestamp {
    public static final int TIMEBASE_BOOTTIME = 1;
    public static final int TIMEBASE_MONOTONIC = 0;
    public long framePosition;
    public long nanoTime;

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes2.dex */
    public @interface Timebase {
    }
}
