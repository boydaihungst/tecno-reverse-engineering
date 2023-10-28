package com.android.server.timezonedetector;

import android.app.ActivityManager;
import android.app.time.ITimeZoneDetectorListener;
import android.app.time.TimeZoneCapabilitiesAndConfig;
import android.app.time.TimeZoneConfiguration;
import android.app.timezonedetector.ITimeZoneDetectorService;
import android.app.timezonedetector.ManualTimeZoneSuggestion;
import android.app.timezonedetector.TelephonyTimeZoneSuggestion;
import android.content.Context;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ShellCallback;
import android.util.ArrayMap;
import android.util.IndentingPrintWriter;
import android.util.Slog;
import com.android.internal.util.DumpUtils;
import com.android.server.FgThread;
import com.android.server.SystemService;
import com.android.server.timezonedetector.DeviceActivityMonitor;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
/* loaded from: classes2.dex */
public final class TimeZoneDetectorService extends ITimeZoneDetectorService.Stub implements IBinder.DeathRecipient {
    static final boolean DBG = false;
    static final String TAG = "time_zone_detector";
    private final CallerIdentityInjector mCallerIdentityInjector;
    private final Context mContext;
    private final Handler mHandler;
    private final ServiceConfigAccessor mServiceConfigAccessor;
    private final TimeZoneDetectorStrategy mTimeZoneDetectorStrategy;
    private final ArrayMap<IBinder, ITimeZoneDetectorListener> mListeners = new ArrayMap<>();
    private final List<Dumpable> mDumpables = new ArrayList();

    /* loaded from: classes2.dex */
    public static final class Lifecycle extends SystemService {
        public Lifecycle(Context context) {
            super(context);
        }

        /* JADX DEBUG: Multi-variable search result rejected for r8v0, resolved type: com.android.server.timezonedetector.TimeZoneDetectorService$Lifecycle */
        /* JADX WARN: Multi-variable type inference failed */
        /* JADX WARN: Type inference failed for: r6v1, types: [com.android.server.timezonedetector.TimeZoneDetectorService, android.os.IBinder] */
        @Override // com.android.server.SystemService
        public void onStart() {
            Context context = getContext();
            Handler handler = FgThread.getHandler();
            ServiceConfigAccessor serviceConfigAccessor = ServiceConfigAccessorImpl.getInstance(context);
            final TimeZoneDetectorStrategy timeZoneDetectorStrategy = TimeZoneDetectorStrategyImpl.create(context, handler, serviceConfigAccessor);
            DeviceActivityMonitor deviceActivityMonitor = DeviceActivityMonitorImpl.create(context, handler);
            deviceActivityMonitor.addListener(new DeviceActivityMonitor.Listener() { // from class: com.android.server.timezonedetector.TimeZoneDetectorService.Lifecycle.1
                @Override // com.android.server.timezonedetector.DeviceActivityMonitor.Listener
                public void onFlightComplete() {
                    timeZoneDetectorStrategy.enableTelephonyTimeZoneFallback();
                }
            });
            publishLocalService(TimeZoneDetectorInternal.class, new TimeZoneDetectorInternalImpl(context, handler, timeZoneDetectorStrategy));
            ?? create = TimeZoneDetectorService.create(context, handler, serviceConfigAccessor, timeZoneDetectorStrategy);
            create.addDumpable(deviceActivityMonitor);
            publishBinderService(TimeZoneDetectorService.TAG, create);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static TimeZoneDetectorService create(Context context, Handler handler, ServiceConfigAccessor serviceConfigAccessor, TimeZoneDetectorStrategy timeZoneDetectorStrategy) {
        CallerIdentityInjector callerIdentityInjector = CallerIdentityInjector.REAL;
        return new TimeZoneDetectorService(context, handler, callerIdentityInjector, serviceConfigAccessor, timeZoneDetectorStrategy);
    }

    public TimeZoneDetectorService(Context context, Handler handler, CallerIdentityInjector callerIdentityInjector, ServiceConfigAccessor serviceConfigAccessor, TimeZoneDetectorStrategy timeZoneDetectorStrategy) {
        this.mContext = (Context) Objects.requireNonNull(context);
        this.mHandler = (Handler) Objects.requireNonNull(handler);
        this.mCallerIdentityInjector = (CallerIdentityInjector) Objects.requireNonNull(callerIdentityInjector);
        ServiceConfigAccessor serviceConfigAccessor2 = (ServiceConfigAccessor) Objects.requireNonNull(serviceConfigAccessor);
        this.mServiceConfigAccessor = serviceConfigAccessor2;
        this.mTimeZoneDetectorStrategy = (TimeZoneDetectorStrategy) Objects.requireNonNull(timeZoneDetectorStrategy);
        serviceConfigAccessor2.addConfigurationInternalChangeListener(new ConfigurationChangeListener() { // from class: com.android.server.timezonedetector.TimeZoneDetectorService$$ExternalSyntheticLambda0
            @Override // com.android.server.timezonedetector.ConfigurationChangeListener
            public final void onChange() {
                TimeZoneDetectorService.this.m6904x2c9c1513();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$new$0$com-android-server-timezonedetector-TimeZoneDetectorService  reason: not valid java name */
    public /* synthetic */ void m6904x2c9c1513() {
        this.mHandler.post(new Runnable() { // from class: com.android.server.timezonedetector.TimeZoneDetectorService$$ExternalSyntheticLambda2
            @Override // java.lang.Runnable
            public final void run() {
                TimeZoneDetectorService.this.handleConfigurationInternalChangedOnHandlerThread();
            }
        });
    }

    public TimeZoneCapabilitiesAndConfig getCapabilitiesAndConfig() {
        int userId = this.mCallerIdentityInjector.getCallingUserId();
        return getCapabilitiesAndConfig(userId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public TimeZoneCapabilitiesAndConfig getCapabilitiesAndConfig(int userId) {
        enforceManageTimeZoneDetectorPermission();
        long token = this.mCallerIdentityInjector.clearCallingIdentity();
        try {
            ConfigurationInternal configurationInternal = this.mServiceConfigAccessor.getConfigurationInternal(userId);
            return configurationInternal.createCapabilitiesAndConfig();
        } finally {
            this.mCallerIdentityInjector.restoreCallingIdentity(token);
        }
    }

    public boolean updateConfiguration(TimeZoneConfiguration configuration) {
        int callingUserId = this.mCallerIdentityInjector.getCallingUserId();
        return updateConfiguration(callingUserId, configuration);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean updateConfiguration(int userId, TimeZoneConfiguration configuration) {
        int userId2 = ActivityManager.handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), userId, false, false, "updateConfiguration", null);
        enforceManageTimeZoneDetectorPermission();
        Objects.requireNonNull(configuration);
        long token = this.mCallerIdentityInjector.clearCallingIdentity();
        try {
            return this.mServiceConfigAccessor.updateConfiguration(userId2, configuration);
        } finally {
            this.mCallerIdentityInjector.restoreCallingIdentity(token);
        }
    }

    public void addListener(ITimeZoneDetectorListener listener) {
        enforceManageTimeZoneDetectorPermission();
        Objects.requireNonNull(listener);
        synchronized (this.mListeners) {
            IBinder listenerBinder = listener.asBinder();
            if (this.mListeners.containsKey(listenerBinder)) {
                return;
            }
            try {
                listenerBinder.linkToDeath(this, 0);
                this.mListeners.put(listenerBinder, listener);
            } catch (RemoteException e) {
                Slog.e(TAG, "Unable to linkToDeath() for listener=" + listener, e);
            }
        }
    }

    public void removeListener(ITimeZoneDetectorListener listener) {
        enforceManageTimeZoneDetectorPermission();
        Objects.requireNonNull(listener);
        synchronized (this.mListeners) {
            IBinder listenerBinder = listener.asBinder();
            boolean removedListener = false;
            if (this.mListeners.remove(listenerBinder) != null) {
                listenerBinder.unlinkToDeath(this, 0);
                removedListener = true;
            }
            if (!removedListener) {
                Slog.w(TAG, "Client asked to remove listener=" + listener + ", but no listeners were removed. mListeners=" + this.mListeners);
            }
        }
    }

    @Override // android.os.IBinder.DeathRecipient
    public void binderDied() {
        Slog.wtf(TAG, "binderDied() called unexpectedly.");
    }

    public void binderDied(IBinder who) {
        synchronized (this.mListeners) {
            boolean removedListener = false;
            int listenerCount = this.mListeners.size();
            int listenerIndex = listenerCount - 1;
            while (true) {
                if (listenerIndex < 0) {
                    break;
                }
                IBinder listenerBinder = this.mListeners.keyAt(listenerIndex);
                if (!listenerBinder.equals(who)) {
                    listenerIndex--;
                } else {
                    this.mListeners.removeAt(listenerIndex);
                    removedListener = true;
                    break;
                }
            }
            if (!removedListener) {
                Slog.w(TAG, "Notified of binder death for who=" + who + ", but did not remove any listeners. mListeners=" + this.mListeners);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void handleConfigurationInternalChangedOnHandlerThread() {
        synchronized (this.mListeners) {
            int listenerCount = this.mListeners.size();
            for (int listenerIndex = 0; listenerIndex < listenerCount; listenerIndex++) {
                ITimeZoneDetectorListener listener = this.mListeners.valueAt(listenerIndex);
                try {
                    listener.onChange();
                } catch (RemoteException e) {
                    Slog.w(TAG, "Unable to notify listener=" + listener, e);
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void suggestGeolocationTimeZone(final GeolocationTimeZoneSuggestion timeZoneSuggestion) {
        enforceSuggestGeolocationTimeZonePermission();
        Objects.requireNonNull(timeZoneSuggestion);
        this.mHandler.post(new Runnable() { // from class: com.android.server.timezonedetector.TimeZoneDetectorService$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                TimeZoneDetectorService.this.m6905x9c954ca9(timeZoneSuggestion);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$suggestGeolocationTimeZone$1$com-android-server-timezonedetector-TimeZoneDetectorService  reason: not valid java name */
    public /* synthetic */ void m6905x9c954ca9(GeolocationTimeZoneSuggestion timeZoneSuggestion) {
        this.mTimeZoneDetectorStrategy.suggestGeolocationTimeZone(timeZoneSuggestion);
    }

    public boolean suggestManualTimeZone(ManualTimeZoneSuggestion timeZoneSuggestion) {
        enforceSuggestManualTimeZonePermission();
        Objects.requireNonNull(timeZoneSuggestion);
        int userId = this.mCallerIdentityInjector.getCallingUserId();
        long token = this.mCallerIdentityInjector.clearCallingIdentity();
        try {
            return this.mTimeZoneDetectorStrategy.suggestManualTimeZone(userId, timeZoneSuggestion);
        } finally {
            this.mCallerIdentityInjector.restoreCallingIdentity(token);
        }
    }

    public void suggestTelephonyTimeZone(final TelephonyTimeZoneSuggestion timeZoneSuggestion) {
        enforceSuggestTelephonyTimeZonePermission();
        Objects.requireNonNull(timeZoneSuggestion);
        this.mHandler.post(new Runnable() { // from class: com.android.server.timezonedetector.TimeZoneDetectorService$$ExternalSyntheticLambda3
            @Override // java.lang.Runnable
            public final void run() {
                TimeZoneDetectorService.this.m6906xf3955ffc(timeZoneSuggestion);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$suggestTelephonyTimeZone$2$com-android-server-timezonedetector-TimeZoneDetectorService  reason: not valid java name */
    public /* synthetic */ void m6906xf3955ffc(TelephonyTimeZoneSuggestion timeZoneSuggestion) {
        this.mTimeZoneDetectorStrategy.suggestTelephonyTimeZone(timeZoneSuggestion);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isTelephonyTimeZoneDetectionSupported() {
        enforceManageTimeZoneDetectorPermission();
        return this.mTimeZoneDetectorStrategy.isTelephonyTimeZoneDetectionSupported();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isGeoTimeZoneDetectionSupported() {
        enforceManageTimeZoneDetectorPermission();
        return this.mTimeZoneDetectorStrategy.isGeoTimeZoneDetectionSupported();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void enableTelephonyFallback() {
        enforceManageTimeZoneDetectorPermission();
        this.mTimeZoneDetectorStrategy.enableTelephonyTimeZoneFallback();
    }

    void addDumpable(Dumpable dumpable) {
        synchronized (this.mDumpables) {
            this.mDumpables.add(dumpable);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public MetricsTimeZoneDetectorState generateMetricsState() {
        enforceManageTimeZoneDetectorPermission();
        return this.mTimeZoneDetectorStrategy.generateMetricsState();
    }

    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (DumpUtils.checkDumpPermission(this.mContext, TAG, pw)) {
            IndentingPrintWriter ipw = new IndentingPrintWriter(pw);
            this.mTimeZoneDetectorStrategy.dump(ipw, args);
            synchronized (this.mDumpables) {
                for (Dumpable dumpable : this.mDumpables) {
                    dumpable.dump(ipw, args);
                }
            }
            ipw.flush();
        }
    }

    /* JADX DEBUG: Multi-variable search result rejected for r8v0, resolved type: com.android.server.timezonedetector.TimeZoneDetectorService */
    /* JADX WARN: Multi-variable type inference failed */
    public void onShellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ShellCallback callback, ResultReceiver resultReceiver) {
        new TimeZoneDetectorShellCommand(this).exec(this, in, out, err, args, callback, resultReceiver);
    }

    private void enforceManageTimeZoneDetectorPermission() {
        this.mContext.enforceCallingPermission("android.permission.MANAGE_TIME_AND_ZONE_DETECTION", "manage time and time zone detection");
    }

    private void enforceSuggestGeolocationTimeZonePermission() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.SET_TIME_ZONE", "suggest geolocation time zone");
    }

    private void enforceSuggestTelephonyTimeZonePermission() {
        this.mContext.enforceCallingPermission("android.permission.SUGGEST_TELEPHONY_TIME_AND_ZONE", "suggest telephony time and time zone");
    }

    private void enforceSuggestManualTimeZonePermission() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.SUGGEST_MANUAL_TIME_AND_ZONE", "suggest manual time and time zone");
    }
}
