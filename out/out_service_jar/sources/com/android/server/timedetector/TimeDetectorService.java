package com.android.server.timedetector;

import android.app.time.ExternalTimeSuggestion;
import android.app.time.TimeCapabilitiesAndConfig;
import android.app.time.TimeConfiguration;
import android.app.timedetector.GnssTimeSuggestion;
import android.app.timedetector.ITimeDetectorService;
import android.app.timedetector.ManualTimeSuggestion;
import android.app.timedetector.NetworkTimeSuggestion;
import android.app.timedetector.TelephonyTimeSuggestion;
import android.content.Context;
import android.os.Binder;
import android.os.Handler;
import android.os.ResultReceiver;
import android.os.ShellCallback;
import android.util.IndentingPrintWriter;
import com.android.internal.util.DumpUtils;
import com.android.server.FgThread;
import com.android.server.SystemService;
import com.android.server.timezonedetector.CallerIdentityInjector;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Objects;
/* loaded from: classes2.dex */
public final class TimeDetectorService extends ITimeDetectorService.Stub {
    static final String TAG = "time_detector";
    private final CallerIdentityInjector mCallerIdentityInjector;
    private final Context mContext;
    private final Handler mHandler;
    private final TimeDetectorStrategy mTimeDetectorStrategy;

    /* loaded from: classes2.dex */
    public static class Lifecycle extends SystemService {
        public Lifecycle(Context context) {
            super(context);
        }

        @Override // com.android.server.SystemService
        public void onStart() {
            Context context = getContext();
            Handler handler = FgThread.getHandler();
            ServiceConfigAccessor serviceConfigAccessor = ServiceConfigAccessor.getInstance(context);
            TimeDetectorStrategy timeDetectorStrategy = TimeDetectorStrategyImpl.create(context, handler, serviceConfigAccessor);
            publishBinderService(TimeDetectorService.TAG, new TimeDetectorService(context, handler, timeDetectorStrategy));
        }
    }

    public TimeDetectorService(Context context, Handler handler, TimeDetectorStrategy timeDetectorStrategy) {
        this(context, handler, timeDetectorStrategy, CallerIdentityInjector.REAL);
    }

    public TimeDetectorService(Context context, Handler handler, TimeDetectorStrategy timeDetectorStrategy, CallerIdentityInjector callerIdentityInjector) {
        this.mContext = (Context) Objects.requireNonNull(context);
        this.mHandler = (Handler) Objects.requireNonNull(handler);
        this.mTimeDetectorStrategy = (TimeDetectorStrategy) Objects.requireNonNull(timeDetectorStrategy);
        this.mCallerIdentityInjector = (CallerIdentityInjector) Objects.requireNonNull(callerIdentityInjector);
    }

    public TimeCapabilitiesAndConfig getCapabilitiesAndConfig() {
        int userId = this.mCallerIdentityInjector.getCallingUserId();
        return getTimeCapabilitiesAndConfig(userId);
    }

    private TimeCapabilitiesAndConfig getTimeCapabilitiesAndConfig(int userId) {
        enforceManageTimeDetectorPermission();
        long token = this.mCallerIdentityInjector.clearCallingIdentity();
        try {
            ConfigurationInternal configurationInternal = this.mTimeDetectorStrategy.getConfigurationInternal(userId);
            return configurationInternal.capabilitiesAndConfig();
        } finally {
            this.mCallerIdentityInjector.restoreCallingIdentity(token);
        }
    }

    public boolean updateConfiguration(TimeConfiguration timeConfiguration) {
        enforceManageTimeDetectorPermission();
        return false;
    }

    public void suggestTelephonyTime(final TelephonyTimeSuggestion timeSignal) {
        enforceSuggestTelephonyTimePermission();
        Objects.requireNonNull(timeSignal);
        this.mHandler.post(new Runnable() { // from class: com.android.server.timedetector.TimeDetectorService$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                TimeDetectorService.this.m6870xac02d1ce(timeSignal);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$suggestTelephonyTime$0$com-android-server-timedetector-TimeDetectorService  reason: not valid java name */
    public /* synthetic */ void m6870xac02d1ce(TelephonyTimeSuggestion timeSignal) {
        this.mTimeDetectorStrategy.suggestTelephonyTime(timeSignal);
    }

    public boolean suggestManualTime(ManualTimeSuggestion timeSignal) {
        enforceSuggestManualTimePermission();
        Objects.requireNonNull(timeSignal);
        long token = Binder.clearCallingIdentity();
        try {
            return this.mTimeDetectorStrategy.suggestManualTime(timeSignal);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    public void suggestNetworkTime(final NetworkTimeSuggestion timeSignal) {
        enforceSuggestNetworkTimePermission();
        Objects.requireNonNull(timeSignal);
        this.mHandler.post(new Runnable() { // from class: com.android.server.timedetector.TimeDetectorService$$ExternalSyntheticLambda3
            @Override // java.lang.Runnable
            public final void run() {
                TimeDetectorService.this.m6869x8f042425(timeSignal);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$suggestNetworkTime$1$com-android-server-timedetector-TimeDetectorService  reason: not valid java name */
    public /* synthetic */ void m6869x8f042425(NetworkTimeSuggestion timeSignal) {
        this.mTimeDetectorStrategy.suggestNetworkTime(timeSignal);
    }

    public void suggestGnssTime(final GnssTimeSuggestion timeSignal) {
        enforceSuggestGnssTimePermission();
        Objects.requireNonNull(timeSignal);
        this.mHandler.post(new Runnable() { // from class: com.android.server.timedetector.TimeDetectorService$$ExternalSyntheticLambda2
            @Override // java.lang.Runnable
            public final void run() {
                TimeDetectorService.this.m6868xbd5b042d(timeSignal);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$suggestGnssTime$2$com-android-server-timedetector-TimeDetectorService  reason: not valid java name */
    public /* synthetic */ void m6868xbd5b042d(GnssTimeSuggestion timeSignal) {
        this.mTimeDetectorStrategy.suggestGnssTime(timeSignal);
    }

    public void suggestExternalTime(final ExternalTimeSuggestion timeSignal) {
        enforceSuggestExternalTimePermission();
        Objects.requireNonNull(timeSignal);
        this.mHandler.post(new Runnable() { // from class: com.android.server.timedetector.TimeDetectorService$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                TimeDetectorService.this.m6867xb88d6372(timeSignal);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$suggestExternalTime$3$com-android-server-timedetector-TimeDetectorService  reason: not valid java name */
    public /* synthetic */ void m6867xb88d6372(ExternalTimeSuggestion timeSignal) {
        this.mTimeDetectorStrategy.suggestExternalTime(timeSignal);
    }

    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (DumpUtils.checkDumpPermission(this.mContext, TAG, pw)) {
            IndentingPrintWriter ipw = new IndentingPrintWriter(pw);
            this.mTimeDetectorStrategy.dump(ipw, args);
            ipw.flush();
        }
    }

    /* JADX DEBUG: Multi-variable search result rejected for r8v0, resolved type: com.android.server.timedetector.TimeDetectorService */
    /* JADX WARN: Multi-variable type inference failed */
    public void onShellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ShellCallback callback, ResultReceiver resultReceiver) {
        new TimeDetectorShellCommand(this).exec(this, in, out, err, args, callback, resultReceiver);
    }

    private void enforceSuggestTelephonyTimePermission() {
        this.mContext.enforceCallingPermission("android.permission.SUGGEST_TELEPHONY_TIME_AND_ZONE", "suggest telephony time and time zone");
    }

    private void enforceSuggestManualTimePermission() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.SUGGEST_MANUAL_TIME_AND_ZONE", "suggest manual time and time zone");
    }

    private void enforceSuggestNetworkTimePermission() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.SET_TIME", "set time");
    }

    private void enforceSuggestGnssTimePermission() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.SET_TIME", "suggest gnss time");
    }

    private void enforceSuggestExternalTimePermission() {
        this.mContext.enforceCallingPermission("android.permission.SUGGEST_EXTERNAL_TIME", "suggest time from external source");
    }

    private void enforceManageTimeDetectorPermission() {
        this.mContext.enforceCallingPermission("android.permission.MANAGE_TIME_AND_ZONE_DETECTION", "manage time and time zone detection");
    }
}
