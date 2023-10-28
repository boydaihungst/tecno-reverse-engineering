package com.android.server.timezonedetector.location;

import android.service.timezone.TimeZoneProviderEvent;
import android.service.timezone.TimeZoneProviderSuggestion;
import android.util.IndentingPrintWriter;
import com.android.server.timezonedetector.ConfigurationInternal;
import com.android.server.timezonedetector.Dumpable;
import com.android.server.timezonedetector.GeolocationTimeZoneSuggestion;
import com.android.server.timezonedetector.ReferenceWithHistory;
import com.android.server.timezonedetector.location.LocationTimeZoneManagerServiceState;
import com.android.server.timezonedetector.location.LocationTimeZoneProvider;
import com.android.server.timezonedetector.location.ThreadingDomain;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Objects;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class LocationTimeZoneProviderController implements Dumpable {
    static final String STATE_CERTAIN = "CERTAIN";
    static final String STATE_DESTROYED = "DESTROYED";
    static final String STATE_FAILED = "FAILED";
    static final String STATE_INITIALIZING = "INITIALIZING";
    static final String STATE_PROVIDERS_INITIALIZING = "PROVIDERS_INITIALIZING";
    static final String STATE_STOPPED = "STOPPED";
    static final String STATE_UNCERTAIN = "UNCERTAIN";
    static final String STATE_UNKNOWN = "UNKNOWN";
    private Callback mCallback;
    private ConfigurationInternal mCurrentUserConfiguration;
    private Environment mEnvironment;
    private GeolocationTimeZoneSuggestion mLastSuggestion;
    private final MetricsLogger mMetricsLogger;
    private final LocationTimeZoneProvider mPrimaryProvider;
    private final boolean mRecordStateChanges;
    private final ArrayList<String> mRecordedStates = new ArrayList<>(0);
    private final LocationTimeZoneProvider mSecondaryProvider;
    private final Object mSharedLock;
    private final ReferenceWithHistory<String> mState;
    private final ThreadingDomain mThreadingDomain;
    private final ThreadingDomain.SingleRunnableQueue mUncertaintyTimeoutQueue;

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public interface MetricsLogger {
        void onStateChange(String str);
    }

    @Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes2.dex */
    @interface State {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public LocationTimeZoneProviderController(ThreadingDomain threadingDomain, MetricsLogger metricsLogger, LocationTimeZoneProvider primaryProvider, LocationTimeZoneProvider secondaryProvider, boolean recordStateChanges) {
        ReferenceWithHistory<String> referenceWithHistory = new ReferenceWithHistory<>(10);
        this.mState = referenceWithHistory;
        this.mThreadingDomain = (ThreadingDomain) Objects.requireNonNull(threadingDomain);
        Object lockObject = threadingDomain.getLockObject();
        this.mSharedLock = lockObject;
        this.mUncertaintyTimeoutQueue = threadingDomain.createSingleRunnableQueue();
        this.mMetricsLogger = (MetricsLogger) Objects.requireNonNull(metricsLogger);
        this.mPrimaryProvider = (LocationTimeZoneProvider) Objects.requireNonNull(primaryProvider);
        this.mSecondaryProvider = (LocationTimeZoneProvider) Objects.requireNonNull(secondaryProvider);
        this.mRecordStateChanges = recordStateChanges;
        synchronized (lockObject) {
            referenceWithHistory.set(STATE_UNKNOWN);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void initialize(Environment environment, Callback callback) {
        this.mThreadingDomain.assertCurrentThread();
        synchronized (this.mSharedLock) {
            LocationTimeZoneManagerService.debugLog("initialize()");
            this.mEnvironment = (Environment) Objects.requireNonNull(environment);
            this.mCallback = (Callback) Objects.requireNonNull(callback);
            this.mCurrentUserConfiguration = environment.getCurrentUserConfigurationInternal();
            LocationTimeZoneProvider.ProviderListener providerListener = new LocationTimeZoneProvider.ProviderListener() { // from class: com.android.server.timezonedetector.location.LocationTimeZoneProviderController$$ExternalSyntheticLambda0
                @Override // com.android.server.timezonedetector.location.LocationTimeZoneProvider.ProviderListener
                public final void onProviderStateChange(LocationTimeZoneProvider.ProviderState providerState) {
                    LocationTimeZoneProviderController.this.onProviderStateChange(providerState);
                }
            };
            setState(STATE_PROVIDERS_INITIALIZING);
            this.mPrimaryProvider.initialize(providerListener);
            this.mSecondaryProvider.initialize(providerListener);
            setState(STATE_STOPPED);
            alterProvidersStartedStateIfRequired(null, this.mCurrentUserConfiguration);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onConfigurationInternalChanged() {
        this.mThreadingDomain.assertCurrentThread();
        synchronized (this.mSharedLock) {
            LocationTimeZoneManagerService.debugLog("onConfigChanged()");
            ConfigurationInternal oldConfig = this.mCurrentUserConfiguration;
            ConfigurationInternal newConfig = this.mEnvironment.getCurrentUserConfigurationInternal();
            this.mCurrentUserConfiguration = newConfig;
            if (!newConfig.equals(oldConfig)) {
                if (newConfig.getUserId() != oldConfig.getUserId()) {
                    String reason = "User changed. old=" + oldConfig.getUserId() + ", new=" + newConfig.getUserId();
                    LocationTimeZoneManagerService.debugLog("Stopping providers: " + reason);
                    stopProviders(reason);
                    alterProvidersStartedStateIfRequired(null, newConfig);
                } else {
                    alterProvidersStartedStateIfRequired(oldConfig, newConfig);
                }
            }
        }
    }

    boolean isUncertaintyTimeoutSet() {
        return this.mUncertaintyTimeoutQueue.hasQueued();
    }

    long getUncertaintyTimeoutDelayMillis() {
        return this.mUncertaintyTimeoutQueue.getQueuedDelayMillis();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void destroy() {
        this.mThreadingDomain.assertCurrentThread();
        synchronized (this.mSharedLock) {
            stopProviders("destroy()");
            this.mPrimaryProvider.destroy();
            this.mSecondaryProvider.destroy();
            setState(STATE_DESTROYED);
        }
    }

    private void setState(String state) {
        if (!Objects.equals(this.mState.get(), state)) {
            this.mState.set(state);
            if (this.mRecordStateChanges) {
                this.mRecordedStates.add(state);
            }
            this.mMetricsLogger.onStateChange(state);
        }
    }

    private void stopProviders(String reason) {
        stopProviderIfStarted(this.mPrimaryProvider);
        stopProviderIfStarted(this.mSecondaryProvider);
        cancelUncertaintyTimeout();
        if (Objects.equals(this.mState.get(), STATE_CERTAIN)) {
            GeolocationTimeZoneSuggestion suggestion = createUncertainSuggestion(this.mEnvironment.elapsedRealtimeMillis(), "Withdraw previous suggestion, providers are stopping: " + reason);
            makeSuggestion(suggestion, STATE_UNCERTAIN);
        }
        setState(STATE_STOPPED);
    }

    private void stopProviderIfStarted(LocationTimeZoneProvider provider) {
        if (provider.getCurrentState().isStarted()) {
            stopProvider(provider);
        }
    }

    private void stopProvider(LocationTimeZoneProvider provider) {
        LocationTimeZoneProvider.ProviderState providerState = provider.getCurrentState();
        switch (providerState.stateEnum) {
            case 1:
            case 2:
            case 3:
                LocationTimeZoneManagerService.debugLog("Stopping " + provider);
                provider.stopUpdates();
                return;
            case 4:
                LocationTimeZoneManagerService.debugLog("No need to stop " + provider + ": already stopped");
                return;
            case 5:
            case 6:
                LocationTimeZoneManagerService.debugLog("Unable to stop " + provider + ": it is terminated.");
                return;
            default:
                LocationTimeZoneManagerService.warnLog("Unknown provider state: " + provider);
                return;
        }
    }

    private void alterProvidersStartedStateIfRequired(ConfigurationInternal oldConfiguration, ConfigurationInternal newConfiguration) {
        boolean oldIsGeoDetectionExecutionEnabled = oldConfiguration != null && oldConfiguration.isGeoDetectionExecutionEnabled();
        boolean newIsGeoDetectionExecutionEnabled = newConfiguration.isGeoDetectionExecutionEnabled();
        if (oldIsGeoDetectionExecutionEnabled == newIsGeoDetectionExecutionEnabled) {
            return;
        }
        if (newIsGeoDetectionExecutionEnabled) {
            setState(STATE_INITIALIZING);
            tryStartProvider(this.mPrimaryProvider, newConfiguration);
            LocationTimeZoneProvider.ProviderState newPrimaryState = this.mPrimaryProvider.getCurrentState();
            if (!newPrimaryState.isStarted()) {
                tryStartProvider(this.mSecondaryProvider, newConfiguration);
                LocationTimeZoneProvider.ProviderState newSecondaryState = this.mSecondaryProvider.getCurrentState();
                if (!newSecondaryState.isStarted()) {
                    GeolocationTimeZoneSuggestion suggestion = createUncertainSuggestion(this.mEnvironment.elapsedRealtimeMillis(), "Providers are failed: primary=" + this.mPrimaryProvider.getCurrentState() + " secondary=" + this.mPrimaryProvider.getCurrentState());
                    makeSuggestion(suggestion, STATE_FAILED);
                    return;
                }
                return;
            }
            return;
        }
        stopProviders("Geo detection behavior disabled");
    }

    private void tryStartProvider(LocationTimeZoneProvider provider, ConfigurationInternal configuration) {
        LocationTimeZoneProvider.ProviderState providerState = provider.getCurrentState();
        switch (providerState.stateEnum) {
            case 1:
            case 2:
            case 3:
                LocationTimeZoneManagerService.debugLog("No need to start " + provider + ": already started");
                return;
            case 4:
                LocationTimeZoneManagerService.debugLog("Enabling " + provider);
                provider.startUpdates(configuration, this.mEnvironment.getProviderInitializationTimeout(), this.mEnvironment.getProviderInitializationTimeoutFuzz(), this.mEnvironment.getProviderEventFilteringAgeThreshold());
                return;
            case 5:
            case 6:
                LocationTimeZoneManagerService.debugLog("Unable to start " + provider + ": it is terminated");
                return;
            default:
                throw new IllegalStateException("Unknown provider state: provider=" + provider);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onProviderStateChange(LocationTimeZoneProvider.ProviderState providerState) {
        this.mThreadingDomain.assertCurrentThread();
        LocationTimeZoneProvider provider = providerState.provider;
        assertProviderKnown(provider);
        synchronized (this.mSharedLock) {
            if (Objects.equals(this.mState.get(), STATE_PROVIDERS_INITIALIZING)) {
                LocationTimeZoneManagerService.warnLog("onProviderStateChange: Ignoring provider state change because both providers have not yet completed initialization. providerState=" + providerState);
                return;
            }
            switch (providerState.stateEnum) {
                case 1:
                case 4:
                case 6:
                    LocationTimeZoneManagerService.warnLog("onProviderStateChange: Unexpected state change for provider, provider=" + provider);
                    break;
                case 2:
                case 3:
                    LocationTimeZoneManagerService.debugLog("onProviderStateChange: Received notification of a state change while started, provider=" + provider);
                    handleProviderStartedStateChange(providerState);
                    break;
                case 5:
                    LocationTimeZoneManagerService.debugLog("Received notification of permanent failure for provider=" + provider);
                    handleProviderFailedStateChange(providerState);
                    break;
                default:
                    LocationTimeZoneManagerService.warnLog("onProviderStateChange: Unexpected provider=" + provider);
                    break;
            }
        }
    }

    private void assertProviderKnown(LocationTimeZoneProvider provider) {
        if (provider != this.mPrimaryProvider && provider != this.mSecondaryProvider) {
            throw new IllegalArgumentException("Unknown provider: " + provider);
        }
    }

    private void handleProviderFailedStateChange(LocationTimeZoneProvider.ProviderState providerState) {
        LocationTimeZoneProvider failedProvider = providerState.provider;
        LocationTimeZoneProvider.ProviderState primaryCurrentState = this.mPrimaryProvider.getCurrentState();
        LocationTimeZoneProvider.ProviderState secondaryCurrentState = this.mSecondaryProvider.getCurrentState();
        if (failedProvider == this.mPrimaryProvider) {
            if (!secondaryCurrentState.isTerminated()) {
                tryStartProvider(this.mSecondaryProvider, this.mCurrentUserConfiguration);
            }
        } else if (failedProvider == this.mSecondaryProvider && primaryCurrentState.stateEnum != 3 && !primaryCurrentState.isTerminated()) {
            LocationTimeZoneManagerService.warnLog("Secondary provider unexpected reported a failure: failed provider=" + failedProvider.getName() + ", primary provider=" + this.mPrimaryProvider + ", secondary provider=" + this.mSecondaryProvider);
        }
        if (primaryCurrentState.isTerminated() && secondaryCurrentState.isTerminated()) {
            cancelUncertaintyTimeout();
            GeolocationTimeZoneSuggestion suggestion = createUncertainSuggestion(this.mEnvironment.elapsedRealtimeMillis(), "Both providers are terminated: primary=" + primaryCurrentState.provider + ", secondary=" + secondaryCurrentState.provider);
            makeSuggestion(suggestion, STATE_FAILED);
        }
    }

    private void handleProviderStartedStateChange(LocationTimeZoneProvider.ProviderState providerState) {
        LocationTimeZoneProvider provider = providerState.provider;
        TimeZoneProviderEvent event = providerState.event;
        if (event == null) {
            long uncertaintyStartedElapsedMillis = this.mEnvironment.elapsedRealtimeMillis();
            handleProviderUncertainty(provider, uncertaintyStartedElapsedMillis, "provider=" + provider + ", implicit uncertainty, event=null");
            return;
        }
        if (!this.mCurrentUserConfiguration.isGeoDetectionExecutionEnabled()) {
            LocationTimeZoneManagerService.warnLog("Provider=" + provider + " is started, but currentUserConfiguration=" + this.mCurrentUserConfiguration + " suggests it shouldn't be.");
        }
        switch (event.getType()) {
            case 1:
                LocationTimeZoneManagerService.warnLog("Provider=" + provider + " is started, but event suggests it shouldn't be");
                return;
            case 2:
                handleProviderSuggestion(provider, event);
                return;
            case 3:
                long uncertaintyStartedElapsedMillis2 = event.getCreationElapsedMillis();
                handleProviderUncertainty(provider, uncertaintyStartedElapsedMillis2, "provider=" + provider + ", explicit uncertainty. event=" + event);
                return;
            default:
                LocationTimeZoneManagerService.warnLog("Unknown eventType=" + event.getType());
                return;
        }
    }

    private void handleProviderSuggestion(LocationTimeZoneProvider provider, TimeZoneProviderEvent providerEvent) {
        cancelUncertaintyTimeout();
        if (provider == this.mPrimaryProvider) {
            stopProviderIfStarted(this.mSecondaryProvider);
        }
        TimeZoneProviderSuggestion providerSuggestion = providerEvent.getSuggestion();
        long effectiveFromElapsedMillis = providerSuggestion.getElapsedRealtimeMillis();
        GeolocationTimeZoneSuggestion geoSuggestion = GeolocationTimeZoneSuggestion.createCertainSuggestion(effectiveFromElapsedMillis, providerSuggestion.getTimeZoneIds());
        String debugInfo = "Event received provider=" + provider + ", providerEvent=" + providerEvent + ", suggestionCreationTime=" + this.mEnvironment.elapsedRealtimeMillis();
        geoSuggestion.addDebugInfo(debugInfo);
        makeSuggestion(geoSuggestion, STATE_CERTAIN);
    }

    @Override // com.android.server.timezonedetector.Dumpable
    public void dump(IndentingPrintWriter ipw, String[] args) {
        synchronized (this.mSharedLock) {
            ipw.println("LocationTimeZoneProviderController:");
            ipw.increaseIndent();
            ipw.println("mCurrentUserConfiguration=" + this.mCurrentUserConfiguration);
            ipw.println("providerInitializationTimeout=" + this.mEnvironment.getProviderInitializationTimeout());
            ipw.println("providerInitializationTimeoutFuzz=" + this.mEnvironment.getProviderInitializationTimeoutFuzz());
            ipw.println("uncertaintyDelay=" + this.mEnvironment.getUncertaintyDelay());
            ipw.println("mState=" + this.mState.get());
            ipw.println("mLastSuggestion=" + this.mLastSuggestion);
            ipw.println("State history:");
            ipw.increaseIndent();
            this.mState.dump(ipw);
            ipw.decreaseIndent();
            ipw.println("Primary Provider:");
            ipw.increaseIndent();
            this.mPrimaryProvider.dump(ipw, args);
            ipw.decreaseIndent();
            ipw.println("Secondary Provider:");
            ipw.increaseIndent();
            this.mSecondaryProvider.dump(ipw, args);
            ipw.decreaseIndent();
            ipw.decreaseIndent();
        }
    }

    private void makeSuggestion(GeolocationTimeZoneSuggestion suggestion, String newState) {
        LocationTimeZoneManagerService.debugLog("makeSuggestion: suggestion=" + suggestion);
        this.mCallback.suggest(suggestion);
        this.mLastSuggestion = suggestion;
        setState(newState);
    }

    private void cancelUncertaintyTimeout() {
        this.mUncertaintyTimeoutQueue.cancel();
    }

    void handleProviderUncertainty(final LocationTimeZoneProvider provider, final long uncertaintyStartedElapsedMillis, String reason) {
        Objects.requireNonNull(provider);
        if (!this.mUncertaintyTimeoutQueue.hasQueued()) {
            LocationTimeZoneManagerService.debugLog("Starting uncertainty timeout: reason=" + reason);
            final Duration uncertaintyDelay = this.mEnvironment.getUncertaintyDelay();
            this.mUncertaintyTimeoutQueue.runDelayed(new Runnable() { // from class: com.android.server.timezonedetector.location.LocationTimeZoneProviderController$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    LocationTimeZoneProviderController.this.m6925xd5bd1445(provider, uncertaintyStartedElapsedMillis, uncertaintyDelay);
                }
            }, uncertaintyDelay.toMillis());
        }
        if (provider == this.mPrimaryProvider) {
            tryStartProvider(this.mSecondaryProvider, this.mCurrentUserConfiguration);
        }
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: onProviderUncertaintyTimeout */
    public void m6925xd5bd1445(LocationTimeZoneProvider provider, long uncertaintyStartedElapsedMillis, Duration uncertaintyDelay) {
        this.mThreadingDomain.assertCurrentThread();
        synchronized (this.mSharedLock) {
            long afterUncertaintyTimeoutElapsedMillis = this.mEnvironment.elapsedRealtimeMillis();
            GeolocationTimeZoneSuggestion suggestion = createUncertainSuggestion(uncertaintyStartedElapsedMillis, "Uncertainty timeout triggered for " + provider.getName() + ": primary=" + this.mPrimaryProvider + ", secondary=" + this.mSecondaryProvider + ", uncertaintyStarted=" + Duration.ofMillis(uncertaintyStartedElapsedMillis) + ", afterUncertaintyTimeout=" + Duration.ofMillis(afterUncertaintyTimeoutElapsedMillis) + ", uncertaintyDelay=" + uncertaintyDelay);
            makeSuggestion(suggestion, STATE_UNCERTAIN);
        }
    }

    private static GeolocationTimeZoneSuggestion createUncertainSuggestion(long effectiveFromElapsedMillis, String reason) {
        GeolocationTimeZoneSuggestion suggestion = GeolocationTimeZoneSuggestion.createUncertainSuggestion(effectiveFromElapsedMillis);
        suggestion.addDebugInfo(reason);
        return suggestion;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void clearRecordedStates() {
        this.mThreadingDomain.assertCurrentThread();
        synchronized (this.mSharedLock) {
            this.mRecordedStates.clear();
            this.mPrimaryProvider.clearRecordedStates();
            this.mSecondaryProvider.clearRecordedStates();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public LocationTimeZoneManagerServiceState getStateForTests() {
        LocationTimeZoneManagerServiceState build;
        this.mThreadingDomain.assertCurrentThread();
        synchronized (this.mSharedLock) {
            LocationTimeZoneManagerServiceState.Builder builder = new LocationTimeZoneManagerServiceState.Builder();
            GeolocationTimeZoneSuggestion geolocationTimeZoneSuggestion = this.mLastSuggestion;
            if (geolocationTimeZoneSuggestion != null) {
                builder.setLastSuggestion(geolocationTimeZoneSuggestion);
            }
            builder.setControllerState(this.mState.get()).setStateChanges(this.mRecordedStates).setPrimaryProviderStateChanges(this.mPrimaryProvider.getRecordedStates()).setSecondaryProviderStateChanges(this.mSecondaryProvider.getRecordedStates());
            build = builder.build();
        }
        return build;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public static abstract class Environment {
        protected final Object mSharedLock;
        protected final ThreadingDomain mThreadingDomain;

        abstract void destroy();

        abstract long elapsedRealtimeMillis();

        abstract ConfigurationInternal getCurrentUserConfigurationInternal();

        abstract Duration getProviderEventFilteringAgeThreshold();

        abstract Duration getProviderInitializationTimeout();

        abstract Duration getProviderInitializationTimeoutFuzz();

        abstract Duration getUncertaintyDelay();

        /* JADX INFO: Access modifiers changed from: package-private */
        public Environment(ThreadingDomain threadingDomain) {
            this.mThreadingDomain = (ThreadingDomain) Objects.requireNonNull(threadingDomain);
            this.mSharedLock = threadingDomain.getLockObject();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public static abstract class Callback {
        protected final Object mSharedLock;
        protected final ThreadingDomain mThreadingDomain;

        abstract void suggest(GeolocationTimeZoneSuggestion geolocationTimeZoneSuggestion);

        /* JADX INFO: Access modifiers changed from: package-private */
        public Callback(ThreadingDomain threadingDomain) {
            this.mThreadingDomain = (ThreadingDomain) Objects.requireNonNull(threadingDomain);
            this.mSharedLock = threadingDomain.getLockObject();
        }
    }
}
