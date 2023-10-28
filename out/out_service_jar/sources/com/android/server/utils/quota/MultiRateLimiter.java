package com.android.server.utils.quota;

import android.content.Context;
import com.android.server.utils.quota.QuotaTracker;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
/* loaded from: classes2.dex */
public class MultiRateLimiter {
    private static final CountQuotaTracker[] EMPTY_TRACKER_ARRAY = new CountQuotaTracker[0];
    private static final String TAG = "MultiRateLimiter";
    private final Object mLock;
    private final CountQuotaTracker[] mQuotaTrackers;

    private MultiRateLimiter(List<CountQuotaTracker> quotaTrackers) {
        this.mLock = new Object();
        this.mQuotaTrackers = (CountQuotaTracker[]) quotaTrackers.toArray(EMPTY_TRACKER_ARRAY);
    }

    public void noteEvent(int userId, String packageName, String tag) {
        synchronized (this.mLock) {
            noteEventLocked(userId, packageName, tag);
        }
    }

    public boolean isWithinQuota(int userId, String packageName, String tag) {
        boolean isWithinQuotaLocked;
        synchronized (this.mLock) {
            isWithinQuotaLocked = isWithinQuotaLocked(userId, packageName, tag);
        }
        return isWithinQuotaLocked;
    }

    public void clear(int userId, String packageName) {
        synchronized (this.mLock) {
            clearLocked(userId, packageName);
        }
    }

    private void noteEventLocked(int userId, String packageName, String tag) {
        CountQuotaTracker[] countQuotaTrackerArr;
        for (CountQuotaTracker quotaTracker : this.mQuotaTrackers) {
            quotaTracker.noteEvent(userId, packageName, tag);
        }
    }

    private boolean isWithinQuotaLocked(int userId, String packageName, String tag) {
        CountQuotaTracker[] countQuotaTrackerArr;
        for (CountQuotaTracker quotaTracker : this.mQuotaTrackers) {
            if (!quotaTracker.isWithinQuota(userId, packageName, tag)) {
                return false;
            }
        }
        return true;
    }

    private void clearLocked(int userId, String packageName) {
        CountQuotaTracker[] countQuotaTrackerArr;
        for (CountQuotaTracker quotaTracker : this.mQuotaTrackers) {
            quotaTracker.onAppRemovedLocked(userId, packageName);
        }
    }

    /* loaded from: classes2.dex */
    public static class Builder {
        private final Categorizer mCategorizer;
        private final Category mCategory;
        private final Context mContext;
        private final QuotaTracker.Injector mInjector;
        private final List<CountQuotaTracker> mQuotaTrackers;

        Builder(Context context, QuotaTracker.Injector injector) {
            this.mQuotaTrackers = new ArrayList();
            this.mContext = context;
            this.mInjector = injector;
            this.mCategorizer = Categorizer.SINGLE_CATEGORIZER;
            this.mCategory = Category.SINGLE_CATEGORY;
        }

        public Builder(Context context) {
            this(context, null);
        }

        public Builder addRateLimit(int limit, Duration windowSize) {
            CountQuotaTracker countQuotaTracker;
            if (this.mInjector != null) {
                countQuotaTracker = new CountQuotaTracker(this.mContext, this.mCategorizer, this.mInjector);
            } else {
                countQuotaTracker = new CountQuotaTracker(this.mContext, this.mCategorizer);
            }
            countQuotaTracker.setCountLimit(this.mCategory, limit, windowSize.toMillis());
            this.mQuotaTrackers.add(countQuotaTracker);
            return this;
        }

        public Builder addRateLimit(RateLimit rateLimit) {
            return addRateLimit(rateLimit.mLimit, rateLimit.mWindowSize);
        }

        public Builder addRateLimits(RateLimit[] rateLimits) {
            for (RateLimit rateLimit : rateLimits) {
                addRateLimit(rateLimit);
            }
            return this;
        }

        public MultiRateLimiter build() {
            return new MultiRateLimiter(this.mQuotaTrackers);
        }
    }

    /* loaded from: classes2.dex */
    public static class RateLimit {
        public final int mLimit;
        public final Duration mWindowSize;

        private RateLimit(int limit, Duration windowSize) {
            this.mLimit = limit;
            this.mWindowSize = windowSize;
        }

        public static RateLimit create(int limit, Duration windowSize) {
            return new RateLimit(limit, windowSize);
        }
    }
}
