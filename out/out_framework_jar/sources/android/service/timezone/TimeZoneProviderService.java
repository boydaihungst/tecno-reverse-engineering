package android.service.timezone;

import android.annotation.SystemApi;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.SystemClock;
import android.service.timezone.ITimeZoneProvider;
import android.service.timezone.TimeZoneProviderService;
import android.util.Log;
import com.android.internal.os.BackgroundThread;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Objects;
@SystemApi
/* loaded from: classes3.dex */
public abstract class TimeZoneProviderService extends Service {
    public static final String PRIMARY_LOCATION_TIME_ZONE_PROVIDER_SERVICE_INTERFACE = "android.service.timezone.PrimaryLocationTimeZoneProviderService";
    public static final String SECONDARY_LOCATION_TIME_ZONE_PROVIDER_SERVICE_INTERFACE = "android.service.timezone.SecondaryLocationTimeZoneProviderService";
    private static final String TAG = "TimeZoneProviderService";
    public static final String TEST_COMMAND_RESULT_ERROR_KEY = "ERROR";
    public static final String TEST_COMMAND_RESULT_SUCCESS_KEY = "SUCCESS";
    private long mEventFilteringAgeThresholdMillis;
    private TimeZoneProviderEvent mLastEventSent;
    private ITimeZoneProviderManager mManager;
    private final TimeZoneProviderServiceWrapper mWrapper = new TimeZoneProviderServiceWrapper();
    private final Object mLock = new Object();
    private final Handler mHandler = BackgroundThread.getHandler();

    public abstract void onStartUpdates(long j);

    public abstract void onStopUpdates();

    @Override // android.app.Service
    public final IBinder onBind(Intent intent) {
        return this.mWrapper;
    }

    public final void reportSuggestion(final TimeZoneProviderSuggestion suggestion) {
        Objects.requireNonNull(suggestion);
        this.mHandler.post(new Runnable() { // from class: android.service.timezone.TimeZoneProviderService$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                TimeZoneProviderService.this.m3674x4251b350(suggestion);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$reportSuggestion$0$android-service-timezone-TimeZoneProviderService  reason: not valid java name */
    public /* synthetic */ void m3674x4251b350(TimeZoneProviderSuggestion suggestion) {
        synchronized (this.mLock) {
            ITimeZoneProviderManager manager = this.mManager;
            if (manager != null) {
                try {
                    TimeZoneProviderEvent thisEvent = TimeZoneProviderEvent.createSuggestionEvent(SystemClock.elapsedRealtime(), suggestion);
                    if (shouldSendEvent(thisEvent)) {
                        manager.onTimeZoneProviderEvent(thisEvent);
                        this.mLastEventSent = thisEvent;
                    }
                } catch (RemoteException | RuntimeException e) {
                    Log.w(TAG, e);
                }
            }
        }
    }

    public final void reportUncertain() {
        this.mHandler.post(new Runnable() { // from class: android.service.timezone.TimeZoneProviderService$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                TimeZoneProviderService.this.m3675xe5922bec();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$reportUncertain$1$android-service-timezone-TimeZoneProviderService  reason: not valid java name */
    public /* synthetic */ void m3675xe5922bec() {
        synchronized (this.mLock) {
            ITimeZoneProviderManager manager = this.mManager;
            if (manager != null) {
                try {
                    TimeZoneProviderEvent thisEvent = TimeZoneProviderEvent.createUncertainEvent(SystemClock.elapsedRealtime());
                    if (shouldSendEvent(thisEvent)) {
                        manager.onTimeZoneProviderEvent(thisEvent);
                        this.mLastEventSent = thisEvent;
                    }
                } catch (RemoteException | RuntimeException e) {
                    Log.w(TAG, e);
                }
            }
        }
    }

    public final void reportPermanentFailure(final Throwable cause) {
        Objects.requireNonNull(cause);
        this.mHandler.post(new Runnable() { // from class: android.service.timezone.TimeZoneProviderService$$ExternalSyntheticLambda2
            @Override // java.lang.Runnable
            public final void run() {
                TimeZoneProviderService.this.m3673x17268a56(cause);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$reportPermanentFailure$2$android-service-timezone-TimeZoneProviderService  reason: not valid java name */
    public /* synthetic */ void m3673x17268a56(Throwable cause) {
        synchronized (this.mLock) {
            ITimeZoneProviderManager manager = this.mManager;
            if (manager != null) {
                try {
                    String causeString = cause.getMessage();
                    TimeZoneProviderEvent thisEvent = TimeZoneProviderEvent.createPermanentFailureEvent(SystemClock.elapsedRealtime(), causeString);
                    if (shouldSendEvent(thisEvent)) {
                        manager.onTimeZoneProviderEvent(thisEvent);
                        this.mLastEventSent = thisEvent;
                    }
                } catch (RemoteException | RuntimeException e) {
                    Log.w(TAG, e);
                }
            }
        }
    }

    private boolean shouldSendEvent(TimeZoneProviderEvent newEvent) {
        if (newEvent.isEquivalentTo(this.mLastEventSent)) {
            long timeSinceLastEventMillis = newEvent.getCreationElapsedMillis() - this.mLastEventSent.getCreationElapsedMillis();
            return timeSinceLastEventMillis > this.mEventFilteringAgeThresholdMillis;
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onStartUpdatesInternal(ITimeZoneProviderManager manager, long initializationTimeoutMillis, long eventFilteringAgeThresholdMillis) {
        synchronized (this.mLock) {
            this.mManager = manager;
            this.mEventFilteringAgeThresholdMillis = eventFilteringAgeThresholdMillis;
            this.mLastEventSent = null;
            onStartUpdates(initializationTimeoutMillis);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onStopUpdatesInternal() {
        synchronized (this.mLock) {
            onStopUpdates();
            this.mManager = null;
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // android.app.Service
    public void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
        synchronized (this.mLock) {
            writer.append((CharSequence) ("mLastEventSent=" + this.mLastEventSent));
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes3.dex */
    public class TimeZoneProviderServiceWrapper extends ITimeZoneProvider.Stub {
        private TimeZoneProviderServiceWrapper() {
        }

        @Override // android.service.timezone.ITimeZoneProvider
        public void startUpdates(final ITimeZoneProviderManager manager, final long initializationTimeoutMillis, final long eventFilteringAgeThresholdMillis) {
            Objects.requireNonNull(manager);
            TimeZoneProviderService.this.mHandler.post(new Runnable() { // from class: android.service.timezone.TimeZoneProviderService$TimeZoneProviderServiceWrapper$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    TimeZoneProviderService.TimeZoneProviderServiceWrapper.this.m3676xae26320c(manager, initializationTimeoutMillis, eventFilteringAgeThresholdMillis);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$startUpdates$0$android-service-timezone-TimeZoneProviderService$TimeZoneProviderServiceWrapper  reason: not valid java name */
        public /* synthetic */ void m3676xae26320c(ITimeZoneProviderManager manager, long initializationTimeoutMillis, long eventFilteringAgeThresholdMillis) {
            TimeZoneProviderService.this.onStartUpdatesInternal(manager, initializationTimeoutMillis, eventFilteringAgeThresholdMillis);
        }

        @Override // android.service.timezone.ITimeZoneProvider
        public void stopUpdates() {
            Handler handler = TimeZoneProviderService.this.mHandler;
            final TimeZoneProviderService timeZoneProviderService = TimeZoneProviderService.this;
            handler.post(new Runnable() { // from class: android.service.timezone.TimeZoneProviderService$TimeZoneProviderServiceWrapper$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    TimeZoneProviderService.this.onStopUpdatesInternal();
                }
            });
        }
    }
}
