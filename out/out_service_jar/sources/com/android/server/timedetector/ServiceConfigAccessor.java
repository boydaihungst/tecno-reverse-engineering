package com.android.server.timedetector;

import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.os.SystemProperties;
import com.android.internal.util.Preconditions;
import com.android.server.timezonedetector.ConfigurationChangeListener;
import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
/* loaded from: classes2.dex */
final class ServiceConfigAccessor {
    private static final int SYSTEM_CLOCK_UPDATE_THRESHOLD_MILLIS_DEFAULT = 2000;
    private static ServiceConfigAccessor sInstance;
    private final ConfigOriginPrioritiesSupplier mConfigOriginPrioritiesSupplier;
    private final Context mContext;
    private final ServerFlags mServerFlags;
    private final ServerFlagsOriginPrioritiesSupplier mServerFlagsOriginPrioritiesSupplier;
    private final int mSystemClockUpdateThresholdMillis;
    private static final int[] DEFAULT_AUTOMATIC_TIME_ORIGIN_PRIORITIES = {1, 3};
    private static final Instant TIME_LOWER_BOUND_DEFAULT = Instant.ofEpochMilli(Long.max(Environment.getRootDirectory().lastModified(), Build.TIME));
    private static final Set<String> SERVER_FLAGS_KEYS_TO_WATCH = Set.of(ServerFlags.KEY_TIME_DETECTOR_LOWER_BOUND_MILLIS_OVERRIDE, ServerFlags.KEY_TIME_DETECTOR_ORIGIN_PRIORITIES_OVERRIDE);
    private static final Object SLOCK = new Object();

    private ServiceConfigAccessor(Context context) {
        Context context2 = (Context) Objects.requireNonNull(context);
        this.mContext = context2;
        ServerFlags serverFlags = ServerFlags.getInstance(context2);
        this.mServerFlags = serverFlags;
        this.mConfigOriginPrioritiesSupplier = new ConfigOriginPrioritiesSupplier(context);
        this.mServerFlagsOriginPrioritiesSupplier = new ServerFlagsOriginPrioritiesSupplier(serverFlags);
        this.mSystemClockUpdateThresholdMillis = SystemProperties.getInt("ro.sys.time_detector_update_diff", 2000);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static ServiceConfigAccessor getInstance(Context context) {
        ServiceConfigAccessor serviceConfigAccessor;
        synchronized (SLOCK) {
            if (sInstance == null) {
                sInstance = new ServiceConfigAccessor(context);
            }
            serviceConfigAccessor = sInstance;
        }
        return serviceConfigAccessor;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void addListener(ConfigurationChangeListener listener) {
        this.mServerFlags.addListener(listener, SERVER_FLAGS_KEYS_TO_WATCH);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int[] getOriginPriorities() {
        int[] serverFlagsValue = this.mServerFlagsOriginPrioritiesSupplier.get();
        if (serverFlagsValue != null) {
            return serverFlagsValue;
        }
        int[] configValue = this.mConfigOriginPrioritiesSupplier.get();
        if (configValue != null) {
            return configValue;
        }
        return DEFAULT_AUTOMATIC_TIME_ORIGIN_PRIORITIES;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int systemClockUpdateThresholdMillis() {
        return this.mSystemClockUpdateThresholdMillis;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Instant autoTimeLowerBound() {
        return this.mServerFlags.getOptionalInstant(ServerFlags.KEY_TIME_DETECTOR_LOWER_BOUND_MILLIS_OVERRIDE).orElse(TIME_LOWER_BOUND_DEFAULT);
    }

    /* loaded from: classes2.dex */
    private static abstract class BaseOriginPrioritiesSupplier implements Supplier<int[]> {
        private int[] mLastPriorityInts;
        private String[] mLastPriorityStrings;

        protected abstract String[] lookupPriorityStrings();

        private BaseOriginPrioritiesSupplier() {
        }

        /* JADX DEBUG: Method merged with bridge method */
        @Override // java.util.function.Supplier
        public int[] get() {
            String[] priorityStrings = lookupPriorityStrings();
            synchronized (this) {
                if (Arrays.equals(this.mLastPriorityStrings, priorityStrings)) {
                    return this.mLastPriorityInts;
                }
                int[] priorityInts = null;
                if (priorityStrings != null && priorityStrings.length > 0) {
                    priorityInts = new int[priorityStrings.length];
                    for (int i = 0; i < priorityInts.length; i++) {
                        try {
                            String priorityString = priorityStrings[i];
                            Preconditions.checkArgument(priorityString != null);
                            priorityInts[i] = TimeDetectorStrategy.stringToOrigin(priorityString.trim());
                        } catch (IllegalArgumentException e) {
                            priorityInts = null;
                        }
                    }
                }
                this.mLastPriorityStrings = priorityStrings;
                this.mLastPriorityInts = priorityInts;
                return priorityInts;
            }
        }
    }

    /* loaded from: classes2.dex */
    private static class ConfigOriginPrioritiesSupplier extends BaseOriginPrioritiesSupplier {
        private final Context mContext;

        private ConfigOriginPrioritiesSupplier(Context context) {
            super();
            this.mContext = (Context) Objects.requireNonNull(context);
        }

        @Override // com.android.server.timedetector.ServiceConfigAccessor.BaseOriginPrioritiesSupplier
        protected String[] lookupPriorityStrings() {
            return this.mContext.getResources().getStringArray(17235995);
        }
    }

    /* loaded from: classes2.dex */
    private static class ServerFlagsOriginPrioritiesSupplier extends BaseOriginPrioritiesSupplier {
        private final ServerFlags mServerFlags;

        private ServerFlagsOriginPrioritiesSupplier(ServerFlags serverFlags) {
            super();
            this.mServerFlags = (ServerFlags) Objects.requireNonNull(serverFlags);
        }

        @Override // com.android.server.timedetector.ServiceConfigAccessor.BaseOriginPrioritiesSupplier
        protected String[] lookupPriorityStrings() {
            Optional<String[]> priorityStrings = this.mServerFlags.getOptionalStringArray(ServerFlags.KEY_TIME_DETECTOR_ORIGIN_PRIORITIES_OVERRIDE);
            return priorityStrings.orElse(null);
        }
    }
}
