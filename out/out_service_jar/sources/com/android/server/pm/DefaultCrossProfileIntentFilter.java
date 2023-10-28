package com.android.server.pm;

import android.content.IntentFilter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;
/* loaded from: classes2.dex */
final class DefaultCrossProfileIntentFilter {
    public final int direction;
    public final WatchedIntentFilter filter;
    public final int flags;
    public final boolean letsPersonalDataIntoProfile;

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes2.dex */
    @interface Direction {
        public static final int TO_PARENT = 0;
        public static final int TO_PROFILE = 1;
    }

    private DefaultCrossProfileIntentFilter(WatchedIntentFilter filter, int flags, int direction, boolean letsPersonalDataIntoProfile) {
        this.filter = (WatchedIntentFilter) Objects.requireNonNull(filter);
        this.flags = flags;
        this.direction = direction;
        this.letsPersonalDataIntoProfile = letsPersonalDataIntoProfile;
    }

    /* loaded from: classes2.dex */
    static final class Builder {
        private int mDirection;
        private WatchedIntentFilter mFilter = new WatchedIntentFilter();
        private int mFlags;
        private boolean mLetsPersonalDataIntoProfile;

        /* JADX INFO: Access modifiers changed from: package-private */
        public Builder(int direction, int flags, boolean letsPersonalDataIntoProfile) {
            this.mDirection = direction;
            this.mFlags = flags;
            this.mLetsPersonalDataIntoProfile = letsPersonalDataIntoProfile;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public Builder addAction(String action) {
            this.mFilter.addAction(action);
            return this;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public Builder addCategory(String category) {
            this.mFilter.addCategory(category);
            return this;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public Builder addDataType(String type) {
            try {
                this.mFilter.addDataType(type);
            } catch (IntentFilter.MalformedMimeTypeException e) {
            }
            return this;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public Builder addDataScheme(String scheme) {
            this.mFilter.addDataScheme(scheme);
            return this;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public DefaultCrossProfileIntentFilter build() {
            return new DefaultCrossProfileIntentFilter(this.mFilter, this.mFlags, this.mDirection, this.mLetsPersonalDataIntoProfile);
        }
    }
}
