package com.android.server.am;

import android.os.SystemClock;
import android.util.ArrayMap;
import android.util.Slog;
/* loaded from: classes.dex */
public class DropboxRateLimiter {
    private static final int RATE_LIMIT_ALLOWED_ENTRIES = 6;
    private static final long RATE_LIMIT_BUFFER_DURATION = 600000;
    private static final long RATE_LIMIT_BUFFER_EXPIRY = 1800000;
    private static final String TAG = "DropboxRateLimiter";
    private final Clock mClock;
    private final ArrayMap<String, ErrorRecord> mErrorClusterRecords;
    private long mLastMapCleanUp;

    /* loaded from: classes.dex */
    public interface Clock {
        long uptimeMillis();
    }

    public DropboxRateLimiter() {
        this(new DefaultClock());
    }

    public DropboxRateLimiter(Clock clock) {
        this.mErrorClusterRecords = new ArrayMap<>();
        this.mLastMapCleanUp = 0L;
        this.mClock = clock;
    }

    public RateLimitResult shouldRateLimit(String eventType, String processName) {
        long now = this.mClock.uptimeMillis();
        synchronized (this.mErrorClusterRecords) {
            maybeRemoveExpiredRecords(now);
            ErrorRecord errRecord = this.mErrorClusterRecords.get(errorKey(eventType, processName));
            if (errRecord == null) {
                this.mErrorClusterRecords.put(errorKey(eventType, processName), new ErrorRecord(now, 1));
                return new RateLimitResult(false, 0);
            } else if (now - errRecord.getStartTime() > 600000) {
                int errCount = recentlyDroppedCount(errRecord);
                errRecord.setStartTime(now);
                errRecord.setCount(1);
                return new RateLimitResult(false, errCount);
            } else {
                errRecord.incrementCount();
                if (errRecord.getCount() > 6) {
                    return new RateLimitResult(true, recentlyDroppedCount(errRecord));
                }
                return new RateLimitResult(false, 0);
            }
        }
    }

    private int recentlyDroppedCount(ErrorRecord errRecord) {
        if (errRecord == null || errRecord.getCount() < 6) {
            return 0;
        }
        return errRecord.getCount() - 6;
    }

    private void maybeRemoveExpiredRecords(long now) {
        if (now - this.mLastMapCleanUp <= 1800000) {
            return;
        }
        for (int i = this.mErrorClusterRecords.size() - 1; i >= 0; i--) {
            if (now - this.mErrorClusterRecords.valueAt(i).getStartTime() > 1800000) {
                this.mErrorClusterRecords.removeAt(i);
            }
        }
        this.mLastMapCleanUp = now;
    }

    public void reset() {
        synchronized (this.mErrorClusterRecords) {
            this.mErrorClusterRecords.clear();
        }
        this.mLastMapCleanUp = 0L;
        Slog.i(TAG, "Rate limiter reset.");
    }

    String errorKey(String eventType, String processName) {
        return eventType + processName;
    }

    /* loaded from: classes.dex */
    public class RateLimitResult {
        final int mDroppedCountSinceRateLimitActivated;
        final boolean mShouldRateLimit;

        public RateLimitResult(boolean shouldRateLimit, int droppedCountSinceRateLimitActivated) {
            this.mShouldRateLimit = shouldRateLimit;
            this.mDroppedCountSinceRateLimitActivated = droppedCountSinceRateLimitActivated;
        }

        public boolean shouldRateLimit() {
            return this.mShouldRateLimit;
        }

        public int droppedCountSinceRateLimitActivated() {
            return this.mDroppedCountSinceRateLimitActivated;
        }

        public String createHeader() {
            return "Dropped-Count: " + this.mDroppedCountSinceRateLimitActivated + "\n";
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class ErrorRecord {
        int mCount;
        long mStartTime;

        ErrorRecord(long startTime, int count) {
            this.mStartTime = startTime;
            this.mCount = count;
        }

        public void setStartTime(long startTime) {
            this.mStartTime = startTime;
        }

        public void setCount(int count) {
            this.mCount = count;
        }

        public void incrementCount() {
            this.mCount++;
        }

        public long getStartTime() {
            return this.mStartTime;
        }

        public int getCount() {
            return this.mCount;
        }
    }

    /* loaded from: classes.dex */
    private static class DefaultClock implements Clock {
        private DefaultClock() {
        }

        @Override // com.android.server.am.DropboxRateLimiter.Clock
        public long uptimeMillis() {
            return SystemClock.uptimeMillis();
        }
    }
}
