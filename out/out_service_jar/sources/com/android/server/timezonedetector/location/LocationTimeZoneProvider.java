package com.android.server.timezonedetector.location;

import android.os.SystemClock;
import android.service.timezone.TimeZoneProviderEvent;
import com.android.server.timezonedetector.ConfigurationInternal;
import com.android.server.timezonedetector.Dumpable;
import com.android.server.timezonedetector.ReferenceWithHistory;
import com.android.server.timezonedetector.location.ThreadingDomain;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public abstract class LocationTimeZoneProvider implements Dumpable {
    private final ThreadingDomain.SingleRunnableQueue mInitializationTimeoutQueue;
    ProviderListener mProviderListener;
    private final ProviderMetricsLogger mProviderMetricsLogger;
    final String mProviderName;
    private final boolean mRecordStateChanges;
    final Object mSharedLock;
    final ThreadingDomain mThreadingDomain;
    private final TimeZoneProviderEventPreProcessor mTimeZoneProviderEventPreProcessor;
    private final ArrayList<ProviderState> mRecordedStates = new ArrayList<>(0);
    final ReferenceWithHistory<ProviderState> mCurrentState = new ReferenceWithHistory<>(10);

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public interface ProviderListener {
        void onProviderStateChange(ProviderState providerState);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public interface ProviderMetricsLogger {
        void onProviderStateChanged(int i);
    }

    abstract void onDestroy();

    abstract void onInitialize();

    abstract void onStartUpdates(Duration duration, Duration duration2);

    abstract void onStopUpdates();

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public static class ProviderState {
        static final int PROVIDER_STATE_DESTROYED = 6;
        static final int PROVIDER_STATE_PERM_FAILED = 5;
        static final int PROVIDER_STATE_STARTED_CERTAIN = 2;
        static final int PROVIDER_STATE_STARTED_INITIALIZING = 1;
        static final int PROVIDER_STATE_STARTED_UNCERTAIN = 3;
        static final int PROVIDER_STATE_STOPPED = 4;
        static final int PROVIDER_STATE_UNKNOWN = 0;
        public final ConfigurationInternal currentUserConfiguration;
        public final TimeZoneProviderEvent event;
        private final String mDebugInfo;
        private final long mStateEntryTimeMillis = SystemClock.elapsedRealtime();
        public final LocationTimeZoneProvider provider;
        public final int stateEnum;

        @Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
        @Retention(RetentionPolicy.SOURCE)
        /* loaded from: classes2.dex */
        @interface ProviderStateEnum {
        }

        private ProviderState(LocationTimeZoneProvider provider, int stateEnum, TimeZoneProviderEvent event, ConfigurationInternal currentUserConfiguration, String debugInfo) {
            this.provider = (LocationTimeZoneProvider) Objects.requireNonNull(provider);
            this.stateEnum = stateEnum;
            this.event = event;
            this.currentUserConfiguration = currentUserConfiguration;
            this.mDebugInfo = debugInfo;
        }

        static ProviderState createStartingState(LocationTimeZoneProvider provider) {
            return new ProviderState(provider, 0, null, null, "Initial state");
        }

        /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
        ProviderState newState(int newStateEnum, TimeZoneProviderEvent event, ConfigurationInternal currentUserConfig, String debugInfo) {
            switch (this.stateEnum) {
                case 0:
                    if (newStateEnum != 4) {
                        throw new IllegalArgumentException("Must transition from " + prettyPrintStateEnum(0) + " to " + prettyPrintStateEnum(4));
                    }
                    break;
                case 1:
                case 2:
                case 3:
                case 4:
                    break;
                case 5:
                case 6:
                    throw new IllegalArgumentException("Illegal transition out of " + prettyPrintStateEnum(this.stateEnum));
                default:
                    throw new IllegalArgumentException("Invalid this.stateEnum=" + this.stateEnum);
            }
            switch (newStateEnum) {
                case 0:
                    throw new IllegalArgumentException("Cannot transition to " + prettyPrintStateEnum(0));
                case 1:
                case 2:
                case 3:
                    if (currentUserConfig == null) {
                        throw new IllegalArgumentException("Started state: currentUserConfig must not be null");
                    }
                    break;
                case 4:
                    if (event != null || currentUserConfig != null) {
                        throw new IllegalArgumentException("Stopped state: event and currentUserConfig must be null, event=" + event + ", currentUserConfig=" + currentUserConfig);
                    }
                case 5:
                case 6:
                    if (event != null || currentUserConfig != null) {
                        throw new IllegalArgumentException("Terminal state: event and currentUserConfig must be null, newStateEnum=" + newStateEnum + ", event=" + event + ", currentUserConfig=" + currentUserConfig);
                    }
                default:
                    throw new IllegalArgumentException("Unknown newStateEnum=" + newStateEnum);
            }
            return new ProviderState(this.provider, newStateEnum, event, currentUserConfig, debugInfo);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public boolean isStarted() {
            int i = this.stateEnum;
            return i == 1 || i == 2 || i == 3;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public boolean isTerminated() {
            int i = this.stateEnum;
            return i == 5 || i == 6;
        }

        public String toString() {
            return "ProviderState{stateEnum=" + prettyPrintStateEnum(this.stateEnum) + ", event=" + this.event + ", currentUserConfiguration=" + this.currentUserConfiguration + ", mStateEntryTimeMillis=" + this.mStateEntryTimeMillis + ", mDebugInfo=" + this.mDebugInfo + '}';
        }

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            ProviderState state = (ProviderState) o;
            if (this.stateEnum == state.stateEnum && Objects.equals(this.event, state.event) && Objects.equals(this.currentUserConfiguration, state.currentUserConfiguration)) {
                return true;
            }
            return false;
        }

        public int hashCode() {
            return Objects.hash(Integer.valueOf(this.stateEnum), this.event, this.currentUserConfiguration);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public static String prettyPrintStateEnum(int state) {
            switch (state) {
                case 1:
                    return "Started initializing (1)";
                case 2:
                    return "Started certain (2)";
                case 3:
                    return "Started uncertain (3)";
                case 4:
                    return "Stopped (4)";
                case 5:
                    return "Perm failure (5)";
                case 6:
                    return "Destroyed (6)";
                default:
                    return "Unknown (" + state + ")";
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public LocationTimeZoneProvider(ProviderMetricsLogger providerMetricsLogger, ThreadingDomain threadingDomain, String providerName, TimeZoneProviderEventPreProcessor timeZoneProviderEventPreProcessor, boolean recordStateChanges) {
        this.mThreadingDomain = (ThreadingDomain) Objects.requireNonNull(threadingDomain);
        this.mProviderMetricsLogger = (ProviderMetricsLogger) Objects.requireNonNull(providerMetricsLogger);
        this.mInitializationTimeoutQueue = threadingDomain.createSingleRunnableQueue();
        this.mSharedLock = threadingDomain.getLockObject();
        this.mProviderName = (String) Objects.requireNonNull(providerName);
        this.mTimeZoneProviderEventPreProcessor = (TimeZoneProviderEventPreProcessor) Objects.requireNonNull(timeZoneProviderEventPreProcessor);
        this.mRecordStateChanges = recordStateChanges;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public final void initialize(ProviderListener providerListener) {
        this.mThreadingDomain.assertCurrentThread();
        synchronized (this.mSharedLock) {
            if (this.mProviderListener != null) {
                throw new IllegalStateException("initialize already called");
            }
            this.mProviderListener = (ProviderListener) Objects.requireNonNull(providerListener);
            ProviderState currentState = ProviderState.createStartingState(this).newState(4, null, null, "initialize");
            setCurrentState(currentState, false);
            try {
                onInitialize();
            } catch (RuntimeException e) {
                LocationTimeZoneManagerService.warnLog("Unable to initialize the provider", e);
                setCurrentState(currentState.newState(5, null, null, "Failed to initialize: " + e.getMessage()), true);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public final void destroy() {
        this.mThreadingDomain.assertCurrentThread();
        synchronized (this.mSharedLock) {
            ProviderState currentState = this.mCurrentState.get();
            if (!currentState.isTerminated()) {
                ProviderState destroyedState = currentState.newState(6, null, null, "destroy");
                setCurrentState(destroyedState, false);
                onDestroy();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public final void clearRecordedStates() {
        this.mThreadingDomain.assertCurrentThread();
        synchronized (this.mSharedLock) {
            this.mRecordedStates.clear();
            this.mRecordedStates.trimToSize();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public final List<ProviderState> getRecordedStates() {
        ArrayList arrayList;
        this.mThreadingDomain.assertCurrentThread();
        synchronized (this.mSharedLock) {
            arrayList = new ArrayList(this.mRecordedStates);
        }
        return arrayList;
    }

    private void setCurrentState(ProviderState newState, boolean notifyChanges) {
        this.mThreadingDomain.assertCurrentThread();
        synchronized (this.mSharedLock) {
            ProviderState oldState = this.mCurrentState.get();
            this.mCurrentState.set(newState);
            onSetCurrentState(newState);
            if (!Objects.equals(newState, oldState)) {
                this.mProviderMetricsLogger.onProviderStateChanged(newState.stateEnum);
                if (this.mRecordStateChanges) {
                    this.mRecordedStates.add(newState);
                }
                if (notifyChanges) {
                    this.mProviderListener.onProviderStateChange(newState);
                }
            }
        }
    }

    void onSetCurrentState(ProviderState newState) {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public final ProviderState getCurrentState() {
        ProviderState providerState;
        this.mThreadingDomain.assertCurrentThread();
        synchronized (this.mSharedLock) {
            providerState = this.mCurrentState.get();
        }
        return providerState;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public final String getName() {
        this.mThreadingDomain.assertCurrentThread();
        return this.mProviderName;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public final void startUpdates(ConfigurationInternal currentUserConfiguration, Duration initializationTimeout, Duration initializationTimeoutFuzz, Duration eventFilteringAgeThreshold) {
        this.mThreadingDomain.assertCurrentThread();
        synchronized (this.mSharedLock) {
            assertCurrentState(4);
            ProviderState currentState = this.mCurrentState.get();
            ProviderState newState = currentState.newState(1, null, currentUserConfiguration, "startUpdates");
            setCurrentState(newState, false);
            Duration delay = initializationTimeout.plus(initializationTimeoutFuzz);
            this.mInitializationTimeoutQueue.runDelayed(new Runnable() { // from class: com.android.server.timezonedetector.location.LocationTimeZoneProvider$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    LocationTimeZoneProvider.this.handleInitializationTimeout();
                }
            }, delay.toMillis());
            onStartUpdates(initializationTimeout, eventFilteringAgeThreshold);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleInitializationTimeout() {
        this.mThreadingDomain.assertCurrentThread();
        synchronized (this.mSharedLock) {
            ProviderState currentState = this.mCurrentState.get();
            if (currentState.stateEnum == 1) {
                ProviderState newState = currentState.newState(3, null, currentState.currentUserConfiguration, "handleInitializationTimeout");
                setCurrentState(newState, true);
            } else {
                LocationTimeZoneManagerService.warnLog("handleInitializationTimeout: Initialization timeout triggered when in an unexpected state=" + currentState);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public final void stopUpdates() {
        this.mThreadingDomain.assertCurrentThread();
        synchronized (this.mSharedLock) {
            assertIsStarted();
            ProviderState currentState = this.mCurrentState.get();
            ProviderState newState = currentState.newState(4, null, null, "stopUpdates");
            setCurrentState(newState, false);
            cancelInitializationTimeoutIfSet();
            onStopUpdates();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public final void handleTimeZoneProviderEvent(TimeZoneProviderEvent timeZoneProviderEvent) {
        int providerStateEnum;
        this.mThreadingDomain.assertCurrentThread();
        Objects.requireNonNull(timeZoneProviderEvent);
        TimeZoneProviderEvent timeZoneProviderEvent2 = this.mTimeZoneProviderEventPreProcessor.preProcess(timeZoneProviderEvent);
        synchronized (this.mSharedLock) {
            LocationTimeZoneManagerService.debugLog("handleTimeZoneProviderEvent: mProviderName=" + this.mProviderName + ", timeZoneProviderEvent=" + timeZoneProviderEvent2);
            ProviderState currentState = this.mCurrentState.get();
            int eventType = timeZoneProviderEvent2.getType();
            switch (currentState.stateEnum) {
                case 1:
                case 2:
                case 3:
                    switch (eventType) {
                        case 1:
                            String msg = "handleTimeZoneProviderEvent: Failure event=" + timeZoneProviderEvent2 + " received for provider=" + this.mProviderName + " in state=" + ProviderState.prettyPrintStateEnum(currentState.stateEnum) + ", entering permanently failed state";
                            LocationTimeZoneManagerService.warnLog(msg);
                            ProviderState newState = currentState.newState(5, null, null, msg);
                            setCurrentState(newState, true);
                            cancelInitializationTimeoutIfSet();
                            return;
                        case 2:
                        case 3:
                            if (eventType == 3) {
                                providerStateEnum = 3;
                            } else {
                                providerStateEnum = 2;
                            }
                            ProviderState newState2 = currentState.newState(providerStateEnum, timeZoneProviderEvent2, currentState.currentUserConfiguration, "handleTimeZoneProviderEvent");
                            setCurrentState(newState2, true);
                            cancelInitializationTimeoutIfSet();
                            return;
                        default:
                            throw new IllegalStateException("Unknown eventType=" + timeZoneProviderEvent2);
                    }
                case 4:
                    switch (eventType) {
                        case 1:
                            String msg2 = "handleTimeZoneProviderEvent: Failure event=" + timeZoneProviderEvent2 + " received for stopped provider=" + this.mProviderName + ", entering permanently failed state";
                            LocationTimeZoneManagerService.warnLog(msg2);
                            ProviderState newState3 = currentState.newState(5, null, null, msg2);
                            setCurrentState(newState3, true);
                            cancelInitializationTimeoutIfSet();
                            return;
                        case 2:
                        case 3:
                            LocationTimeZoneManagerService.warnLog("handleTimeZoneProviderEvent: event=" + timeZoneProviderEvent2 + " received for stopped provider=" + this + ", ignoring");
                            return;
                        default:
                            throw new IllegalStateException("Unknown eventType=" + timeZoneProviderEvent2);
                    }
                case 5:
                case 6:
                    LocationTimeZoneManagerService.warnLog("handleTimeZoneProviderEvent: Event=" + timeZoneProviderEvent2 + " received for provider=" + this + " when in terminated state");
                    return;
                default:
                    throw new IllegalStateException("Unknown providerType=" + currentState);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public final void handleTemporaryFailure(String reason) {
        this.mThreadingDomain.assertCurrentThread();
        synchronized (this.mSharedLock) {
            ProviderState currentState = this.mCurrentState.get();
            switch (currentState.stateEnum) {
                case 1:
                case 2:
                case 3:
                    String debugInfo = "handleTemporaryFailure: reason=" + reason + ", currentState=" + ProviderState.prettyPrintStateEnum(currentState.stateEnum);
                    ProviderState newState = currentState.newState(3, null, currentState.currentUserConfiguration, debugInfo);
                    setCurrentState(newState, true);
                    cancelInitializationTimeoutIfSet();
                    break;
                case 4:
                    LocationTimeZoneManagerService.debugLog("handleProviderLost reason=" + reason + ", mProviderName=" + this.mProviderName + ", currentState=" + currentState + ": No state change required, provider is stopped.");
                    break;
                case 5:
                case 6:
                    LocationTimeZoneManagerService.debugLog("handleProviderLost reason=" + reason + ", mProviderName=" + this.mProviderName + ", currentState=" + currentState + ": No state change required, provider is terminated.");
                    break;
                default:
                    throw new IllegalStateException("Unknown currentState=" + currentState);
            }
        }
    }

    private void assertIsStarted() {
        ProviderState currentState = this.mCurrentState.get();
        if (!currentState.isStarted()) {
            throw new IllegalStateException("Required a started state, but was " + currentState);
        }
    }

    private void assertCurrentState(int requiredState) {
        ProviderState currentState = this.mCurrentState.get();
        if (currentState.stateEnum != requiredState) {
            throw new IllegalStateException("Required stateEnum=" + requiredState + ", but was " + currentState);
        }
    }

    boolean isInitializationTimeoutSet() {
        boolean hasQueued;
        synchronized (this.mSharedLock) {
            hasQueued = this.mInitializationTimeoutQueue.hasQueued();
        }
        return hasQueued;
    }

    private void cancelInitializationTimeoutIfSet() {
        if (this.mInitializationTimeoutQueue.hasQueued()) {
            this.mInitializationTimeoutQueue.cancel();
        }
    }

    Duration getInitializationTimeoutDelay() {
        Duration ofMillis;
        synchronized (this.mSharedLock) {
            ofMillis = Duration.ofMillis(this.mInitializationTimeoutQueue.getQueuedDelayMillis());
        }
        return ofMillis;
    }
}
