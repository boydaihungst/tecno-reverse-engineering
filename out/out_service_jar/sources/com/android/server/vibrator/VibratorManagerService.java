package com.android.server.vibrator;

import android.app.ActivityManager;
import android.app.AppOpsManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Build;
import android.os.CombinedVibration;
import android.os.ExternalVibration;
import android.os.Handler;
import android.os.IBinder;
import android.os.IExternalVibratorService;
import android.os.IVibratorManagerService;
import android.os.IVibratorStateListener;
import android.os.Looper;
import android.os.PowerManager;
import android.os.Process;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ServiceManager;
import android.os.ShellCallback;
import android.os.ShellCommand;
import android.os.SystemClock;
import android.os.Trace;
import android.os.VibrationAttributes;
import android.os.VibrationEffect;
import android.os.VibratorInfo;
import android.os.vibrator.PrebakedSegment;
import android.os.vibrator.VibrationEffectSegment;
import android.text.TextUtils;
import android.util.Slog;
import android.util.SparseArray;
import android.util.proto.ProtoOutputStream;
import com.android.internal.app.IBatteryStats;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.FrameworkStatsLog;
import com.android.server.SystemService;
import com.android.server.job.controllers.JobStatus;
import com.android.server.notification.NotificationShellCmd;
import com.android.server.vibrator.Vibration;
import com.android.server.vibrator.VibrationSettings;
import com.android.server.vibrator.VibrationThread;
import com.android.server.vibrator.VibratorController;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import libcore.util.NativeAllocationRegistry;
/* loaded from: classes2.dex */
public class VibratorManagerService extends IVibratorManagerService.Stub {
    private static final int ATTRIBUTES_ALL_BYPASS_FLAGS = 3;
    private static final long BATTERY_STATS_REPEATING_VIBRATION_DURATION = 5000;
    private static final boolean DEBUG = false;
    private static final VibrationAttributes DEFAULT_ATTRIBUTES = new VibrationAttributes.Builder().build();
    private static final String EXTERNAL_VIBRATOR_SERVICE = "external_vibrator_service";
    private static final String TAG = "VibratorManagerService";
    private static final long VIBRATION_CANCEL_WAIT_MILLIS = 5000;
    private final SparseArray<AlwaysOnVibration> mAlwaysOnEffects;
    private final AppOpsManager mAppOps;
    private final IBatteryStats mBatteryStatsService;
    private final long mCapabilities;
    private final Context mContext;
    private ExternalVibrationHolder mCurrentExternalVibration;
    private VibrationStepConductor mCurrentVibration;
    private final DeviceVibrationEffectAdapter mDeviceVibrationEffectAdapter;
    private final Handler mHandler;
    private final InputDeviceDelegate mInputDeviceDelegate;
    private BroadcastReceiver mIntentReceiver;
    private final NativeWrapper mNativeWrapper;
    private VibrationStepConductor mNextVibration;
    private boolean mServiceReady;
    private final VibrationScaler mVibrationScaler;
    private final VibrationSettings mVibrationSettings;
    private final VibrationThread mVibrationThread;
    private final VibrationThreadCallbacks mVibrationThreadCallbacks;
    private final int[] mVibratorIds;
    private final VibratorManagerRecords mVibratorManagerRecords;
    private final SparseArray<VibratorController> mVibrators;
    private final PowerManager.WakeLock mWakeLock;
    private final AtomicInteger mNextVibrationId = new AtomicInteger(1);
    private final Object mLock = new Object();

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public interface OnSyncedVibrationCompleteListener {
        void onComplete(long j);
    }

    static native void nativeCancelSynced(long j);

    static native long nativeGetCapabilities(long j);

    static native long nativeGetFinalizer();

    static native int[] nativeGetVibratorIds(long j);

    static native long nativeInit(OnSyncedVibrationCompleteListener onSyncedVibrationCompleteListener);

    static native boolean nativePrepareSynced(long j, int[] iArr);

    static native boolean nativeTriggerSynced(long j, long j2);

    /* loaded from: classes2.dex */
    public static class Lifecycle extends SystemService {
        private VibratorManagerService mService;

        public Lifecycle(Context context) {
            super(context);
        }

        /* JADX DEBUG: Multi-variable search result rejected for r3v0, resolved type: com.android.server.vibrator.VibratorManagerService$Lifecycle */
        /* JADX WARN: Multi-variable type inference failed */
        /* JADX WARN: Type inference failed for: r0v0, types: [com.android.server.vibrator.VibratorManagerService, android.os.IBinder] */
        @Override // com.android.server.SystemService
        public void onStart() {
            ?? vibratorManagerService = new VibratorManagerService(getContext(), new Injector());
            this.mService = vibratorManagerService;
            publishBinderService("vibrator_manager", vibratorManagerService);
        }

        @Override // com.android.server.SystemService
        public void onBootPhase(int phase) {
            if (phase == 500) {
                this.mService.systemReady();
            }
        }
    }

    VibratorManagerService(Context context, Injector injector) {
        VibrationThreadCallbacks vibrationThreadCallbacks = new VibrationThreadCallbacks();
        this.mVibrationThreadCallbacks = vibrationThreadCallbacks;
        this.mAlwaysOnEffects = new SparseArray<>();
        this.mIntentReceiver = new BroadcastReceiver() { // from class: com.android.server.vibrator.VibratorManagerService.1
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent) {
                if (intent.getAction().equals("android.intent.action.SCREEN_OFF")) {
                    synchronized (VibratorManagerService.this.mLock) {
                        VibratorManagerService vibratorManagerService = VibratorManagerService.this;
                        if (vibratorManagerService.shouldCancelOnScreenOffLocked(vibratorManagerService.mNextVibration)) {
                            VibratorManagerService.this.clearNextVibrationLocked(Vibration.Status.CANCELLED_BY_SCREEN_OFF);
                        }
                        VibratorManagerService vibratorManagerService2 = VibratorManagerService.this;
                        if (vibratorManagerService2.shouldCancelOnScreenOffLocked(vibratorManagerService2.mCurrentVibration)) {
                            VibratorManagerService.this.mCurrentVibration.notifyCancelled(Vibration.Status.CANCELLED_BY_SCREEN_OFF, false);
                        }
                    }
                }
            }
        };
        this.mContext = context;
        Handler createHandler = injector.createHandler(Looper.myLooper());
        this.mHandler = createHandler;
        VibrationSettings vibrationSettings = new VibrationSettings(context, createHandler);
        this.mVibrationSettings = vibrationSettings;
        this.mVibrationScaler = new VibrationScaler(context, vibrationSettings);
        this.mInputDeviceDelegate = new InputDeviceDelegate(context, createHandler);
        this.mDeviceVibrationEffectAdapter = new DeviceVibrationEffectAdapter(vibrationSettings);
        VibrationCompleteListener listener = new VibrationCompleteListener(this);
        NativeWrapper nativeWrapper = injector.getNativeWrapper();
        this.mNativeWrapper = nativeWrapper;
        nativeWrapper.init(listener);
        int dumpLimit = context.getResources().getInteger(17694919);
        this.mVibratorManagerRecords = new VibratorManagerRecords(dumpLimit);
        this.mBatteryStatsService = injector.getBatteryStatsService();
        this.mAppOps = (AppOpsManager) context.getSystemService(AppOpsManager.class);
        PowerManager pm = (PowerManager) context.getSystemService(PowerManager.class);
        PowerManager.WakeLock newWakeLock = pm.newWakeLock(1, "*vibrator*");
        this.mWakeLock = newWakeLock;
        newWakeLock.setReferenceCounted(true);
        VibrationThread vibrationThread = new VibrationThread(newWakeLock, vibrationThreadCallbacks);
        this.mVibrationThread = vibrationThread;
        vibrationThread.start();
        this.mCapabilities = nativeWrapper.getCapabilities();
        int[] vibratorIds = nativeWrapper.getVibratorIds();
        if (vibratorIds == null) {
            this.mVibratorIds = new int[0];
            this.mVibrators = new SparseArray<>(0);
        } else {
            this.mVibratorIds = vibratorIds;
            this.mVibrators = new SparseArray<>(vibratorIds.length);
            for (int vibratorId : vibratorIds) {
                this.mVibrators.put(vibratorId, injector.createVibratorController(vibratorId, listener));
            }
        }
        this.mNativeWrapper.cancelSynced();
        for (int i = 0; i < this.mVibrators.size(); i++) {
            this.mVibrators.valueAt(i).reset();
        }
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.SCREEN_OFF");
        context.registerReceiver(this.mIntentReceiver, filter, 4);
        injector.addService(EXTERNAL_VIBRATOR_SERVICE, new ExternalVibratorService());
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [271=4] */
    /* JADX DEBUG: Finally have unexpected throw blocks count: 2, expect 1 */
    void systemReady() {
        Slog.v(TAG, "Initializing VibratorManager service...");
        Trace.traceBegin(8388608L, "systemReady");
        for (int i = 0; i < this.mVibrators.size(); i++) {
            try {
                this.mVibrators.valueAt(i).reloadVibratorInfoIfNeeded();
            } catch (Throwable th) {
                synchronized (this.mLock) {
                    this.mServiceReady = true;
                    Slog.v(TAG, "VibratorManager service initialized");
                    Trace.traceEnd(8388608L);
                    throw th;
                }
            }
        }
        this.mVibrationSettings.onSystemReady();
        this.mInputDeviceDelegate.onSystemReady();
        this.mVibrationSettings.addListener(new VibrationSettings.OnVibratorSettingsChanged() { // from class: com.android.server.vibrator.VibratorManagerService$$ExternalSyntheticLambda0
            @Override // com.android.server.vibrator.VibrationSettings.OnVibratorSettingsChanged
            public final void onChange() {
                VibratorManagerService.this.updateServiceState();
            }
        });
        updateServiceState();
        synchronized (this.mLock) {
            this.mServiceReady = true;
        }
        Slog.v(TAG, "VibratorManager service initialized");
        Trace.traceEnd(8388608L);
    }

    public int[] getVibratorIds() {
        int[] iArr = this.mVibratorIds;
        return Arrays.copyOf(iArr, iArr.length);
    }

    public VibratorInfo getVibratorInfo(int vibratorId) {
        VibratorController controller = this.mVibrators.get(vibratorId);
        if (controller == null) {
            return null;
        }
        VibratorInfo info = controller.getVibratorInfo();
        synchronized (this.mLock) {
            if (this.mServiceReady) {
                return info;
            }
            if (controller.isVibratorInfoLoadSuccessful()) {
                return info;
            }
            return null;
        }
    }

    public boolean isVibrating(int vibratorId) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.ACCESS_VIBRATOR_STATE", "isVibrating");
        VibratorController controller = this.mVibrators.get(vibratorId);
        return controller != null && controller.isVibrating();
    }

    public boolean registerVibratorStateListener(int vibratorId, IVibratorStateListener listener) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.ACCESS_VIBRATOR_STATE", "registerVibratorStateListener");
        VibratorController controller = this.mVibrators.get(vibratorId);
        if (controller == null) {
            return false;
        }
        return controller.registerVibratorStateListener(listener);
    }

    public boolean unregisterVibratorStateListener(int vibratorId, IVibratorStateListener listener) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.ACCESS_VIBRATOR_STATE", "unregisterVibratorStateListener");
        VibratorController controller = this.mVibrators.get(vibratorId);
        if (controller == null) {
            return false;
        }
        return controller.unregisterVibratorStateListener(listener);
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [372=5] */
    public boolean setAlwaysOnEffect(int uid, String opPkg, final int alwaysOnId, CombinedVibration effect, VibrationAttributes attrs) {
        Trace.traceBegin(8388608L, "setAlwaysOnEffect");
        try {
            this.mContext.enforceCallingOrSelfPermission("android.permission.VIBRATE_ALWAYS_ON", "setAlwaysOnEffect");
            if (effect == null) {
                synchronized (this.mLock) {
                    this.mAlwaysOnEffects.delete(alwaysOnId);
                    onAllVibratorsLocked(new Consumer() { // from class: com.android.server.vibrator.VibratorManagerService$$ExternalSyntheticLambda1
                        @Override // java.util.function.Consumer
                        public final void accept(Object obj) {
                            VibratorManagerService.lambda$setAlwaysOnEffect$0(alwaysOnId, (VibratorController) obj);
                        }
                    });
                }
                Trace.traceEnd(8388608L);
                return true;
            } else if (!isEffectValid(effect)) {
                Trace.traceEnd(8388608L);
                return false;
            } else {
                try {
                    VibrationAttributes attrs2 = fixupVibrationAttributes(attrs, effect);
                    try {
                        synchronized (this.mLock) {
                            SparseArray<PrebakedSegment> effects = fixupAlwaysOnEffectsLocked(effect);
                            if (effects == null) {
                                Trace.traceEnd(8388608L);
                                return false;
                            }
                            AlwaysOnVibration alwaysOnVibration = new AlwaysOnVibration(alwaysOnId, uid, opPkg, attrs2, effects);
                            this.mAlwaysOnEffects.put(alwaysOnId, alwaysOnVibration);
                            updateAlwaysOnLocked(alwaysOnVibration);
                            Trace.traceEnd(8388608L);
                            return true;
                        }
                    } catch (Throwable th) {
                        th = th;
                        Trace.traceEnd(8388608L);
                        throw th;
                    }
                } catch (Throwable th2) {
                    th = th2;
                }
            }
        } catch (Throwable th3) {
            th = th3;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$setAlwaysOnEffect$0(int alwaysOnId, VibratorController v) {
        if (v.hasCapability(64L)) {
            v.updateAlwaysOn(alwaysOnId, null);
        }
    }

    public void vibrate(int uid, String opPkg, CombinedVibration effect, VibrationAttributes attrs, String reason, IBinder token) {
        vibrateInternal(uid, opPkg, effect, attrs, reason, token);
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [444=6] */
    /* JADX INFO: Access modifiers changed from: private */
    public Vibration vibrateInternal(int uid, String opPkg, CombinedVibration effect, VibrationAttributes attrs, String reason, IBinder token) {
        VibrationAttributes vibrationAttributes;
        Trace.traceBegin(8388608L, "vibrate, reason = " + reason);
        try {
            this.mContext.enforceCallingOrSelfPermission("android.permission.VIBRATE", "vibrate");
            if (token == null) {
                try {
                    Slog.e(TAG, "token must not be null");
                    Trace.traceEnd(8388608L);
                    return null;
                } catch (Throwable th) {
                    th = th;
                }
            } else {
                enforceUpdateAppOpsStatsPermission(uid);
                if (!isEffectValid(effect)) {
                    Trace.traceEnd(8388608L);
                    return null;
                }
                vibrationAttributes = attrs;
                try {
                    VibrationAttributes attrs2 = fixupVibrationAttributes(vibrationAttributes, effect);
                    try {
                        Vibration vib = new Vibration(token, this.mNextVibrationId.getAndIncrement(), effect, attrs2, uid, opPkg, reason);
                        fillVibrationFallbacks(vib, effect);
                        if (attrs2.isFlagSet(3)) {
                            this.mVibrationSettings.mSettingObserver.onChange(false);
                        }
                        synchronized (this.mLock) {
                            Vibration.Status ignoreStatus = shouldIgnoreVibrationLocked(vib.uid, vib.opPkg, vib.attrs);
                            Vibration.Status ignoreStatus2 = ignoreStatus == null ? shouldIgnoreVibrationForOngoingLocked(vib) : ignoreStatus;
                            if (ignoreStatus2 != null) {
                                endVibrationLocked(vib, ignoreStatus2);
                                Trace.traceEnd(8388608L);
                                return vib;
                            }
                            long ident = Binder.clearCallingIdentity();
                            VibrationStepConductor vibrationStepConductor = this.mCurrentVibration;
                            if (vibrationStepConductor != null) {
                                vibrationStepConductor.notifyCancelled(Vibration.Status.CANCELLED_SUPERSEDED, false);
                            }
                            Vibration.Status status = startVibrationLocked(vib);
                            if (status != Vibration.Status.RUNNING) {
                                endVibrationLocked(vib, status);
                            }
                            Binder.restoreCallingIdentity(ident);
                            Trace.traceEnd(8388608L);
                            return vib;
                        }
                    } catch (Throwable th2) {
                        th = th2;
                    }
                } catch (Throwable th3) {
                    th = th3;
                    Trace.traceEnd(8388608L);
                    throw th;
                }
            }
        } catch (Throwable th4) {
            th = th4;
            vibrationAttributes = attrs;
        }
        Trace.traceEnd(8388608L);
        throw th;
    }

    public void cancelVibrate(int usageFilter, IBinder token) {
        Trace.traceBegin(8388608L, "cancelVibrate");
        try {
            this.mContext.enforceCallingOrSelfPermission("android.permission.VIBRATE", "cancelVibrate");
            synchronized (this.mLock) {
                long ident = Binder.clearCallingIdentity();
                try {
                    VibrationStepConductor vibrationStepConductor = this.mNextVibration;
                    if (vibrationStepConductor != null && shouldCancelVibration(vibrationStepConductor.getVibration(), usageFilter, token)) {
                        clearNextVibrationLocked(Vibration.Status.CANCELLED_BY_USER);
                    }
                    VibrationStepConductor vibrationStepConductor2 = this.mCurrentVibration;
                    if (vibrationStepConductor2 != null && shouldCancelVibration(vibrationStepConductor2.getVibration(), usageFilter, token)) {
                        this.mCurrentVibration.notifyCancelled(Vibration.Status.CANCELLED_BY_USER, false);
                    }
                    ExternalVibrationHolder externalVibrationHolder = this.mCurrentExternalVibration;
                    if (externalVibrationHolder != null && shouldCancelVibration(externalVibrationHolder.externalVibration.getVibrationAttributes(), usageFilter)) {
                        this.mCurrentExternalVibration.externalVibration.mute();
                        endExternalVibrateLocked(Vibration.Status.CANCELLED_BY_USER, false);
                    }
                } finally {
                    Binder.restoreCallingIdentity(ident);
                }
            }
        } finally {
            Trace.traceEnd(8388608L);
        }
    }

    public void update_vib_info(int infocase, int info) {
        synchronized (this.mLock) {
            for (int i = 0; i < this.mVibrators.size(); i++) {
                VibratorController controller = this.mVibrators.valueAt(i);
                controller.update_vib_info(infocase, info);
            }
        }
    }

    public void stopDynamicEffect() {
        synchronized (this.mLock) {
            for (int i = 0; i < this.mVibrators.size(); i++) {
                VibratorController controller = this.mVibrators.valueAt(i);
                controller.stopDynamicEffect();
            }
        }
    }

    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (DumpUtils.checkDumpPermission(this.mContext, TAG, pw)) {
            long ident = Binder.clearCallingIdentity();
            boolean isDumpProto = false;
            for (String arg : args) {
                if (arg.equals("--proto")) {
                    isDumpProto = true;
                }
            }
            try {
                if (isDumpProto) {
                    dumpProto(fd);
                } else {
                    dumpText(pw);
                }
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }
    }

    private void dumpText(PrintWriter pw) {
        synchronized (this.mLock) {
            pw.println("Vibrator Manager Service:");
            pw.println("  mVibrationSettings:");
            pw.println("    " + this.mVibrationSettings);
            pw.println();
            pw.println("  mVibratorControllers:");
            for (int i = 0; i < this.mVibrators.size(); i++) {
                pw.println("    " + this.mVibrators.valueAt(i));
            }
            pw.println();
            pw.println("  mCurrentVibration:");
            StringBuilder append = new StringBuilder().append("    ");
            VibrationStepConductor vibrationStepConductor = this.mCurrentVibration;
            pw.println(append.append(vibrationStepConductor == null ? null : vibrationStepConductor.getVibration().getDebugInfo()).toString());
            pw.println();
            pw.println("  mNextVibration:");
            StringBuilder append2 = new StringBuilder().append("    ");
            VibrationStepConductor vibrationStepConductor2 = this.mNextVibration;
            pw.println(append2.append(vibrationStepConductor2 == null ? null : vibrationStepConductor2.getVibration().getDebugInfo()).toString());
            pw.println();
            pw.println("  mCurrentExternalVibration:");
            StringBuilder append3 = new StringBuilder().append("    ");
            ExternalVibrationHolder externalVibrationHolder = this.mCurrentExternalVibration;
            pw.println(append3.append(externalVibrationHolder != null ? externalVibrationHolder.getDebugInfo() : null).toString());
            pw.println();
        }
        this.mVibratorManagerRecords.dumpText(pw);
    }

    synchronized void dumpProto(FileDescriptor fd) {
        ProtoOutputStream proto = new ProtoOutputStream(fd);
        synchronized (this.mLock) {
            try {
                this.mVibrationSettings.dumpProto(proto);
                VibrationStepConductor vibrationStepConductor = this.mCurrentVibration;
                if (vibrationStepConductor != null) {
                    try {
                        vibrationStepConductor.getVibration().getDebugInfo().dumpProto(proto, 1146756268034L);
                    } catch (Throwable th) {
                        th = th;
                        throw th;
                    }
                }
                ExternalVibrationHolder externalVibrationHolder = this.mCurrentExternalVibration;
                if (externalVibrationHolder != null) {
                    externalVibrationHolder.getDebugInfo().dumpProto(proto, 1146756268036L);
                }
                boolean isVibrating = false;
                boolean isUnderExternalControl = false;
                for (int i = 0; i < this.mVibrators.size(); i++) {
                    proto.write(2220498092033L, this.mVibrators.keyAt(i));
                    isVibrating |= this.mVibrators.valueAt(i).isVibrating();
                    isUnderExternalControl |= this.mVibrators.valueAt(i).isUnderExternalControl();
                }
                proto.write(1133871366147L, isVibrating);
                proto.write(1133871366149L, isUnderExternalControl);
                this.mVibratorManagerRecords.dumpProto(proto);
                proto.flush();
            } catch (Throwable th2) {
                th = th2;
            }
        }
    }

    /* JADX DEBUG: Multi-variable search result rejected for r8v0, resolved type: com.android.server.vibrator.VibratorManagerService */
    /* JADX WARN: Multi-variable type inference failed */
    public void onShellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ShellCallback cb, ResultReceiver resultReceiver) {
        new VibratorManagerShellCommand(cb.getShellCallbackBinder()).exec(this, in, out, err, args, cb, resultReceiver);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateServiceState() {
        synchronized (this.mLock) {
            boolean inputDevicesChanged = this.mInputDeviceDelegate.updateInputDeviceVibrators(this.mVibrationSettings.shouldVibrateInputDevices());
            for (int i = 0; i < this.mAlwaysOnEffects.size(); i++) {
                updateAlwaysOnLocked(this.mAlwaysOnEffects.valueAt(i));
            }
            VibrationStepConductor vibrationStepConductor = this.mCurrentVibration;
            if (vibrationStepConductor == null) {
                return;
            }
            Vibration vib = vibrationStepConductor.getVibration();
            Vibration.Status ignoreStatus = shouldIgnoreVibrationLocked(vib.uid, vib.opPkg, vib.attrs);
            if (inputDevicesChanged || ignoreStatus != null) {
                this.mCurrentVibration.notifyCancelled(Vibration.Status.CANCELLED_BY_SETTINGS_UPDATE, false);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setExternalControl(boolean externalControl) {
        for (int i = 0; i < this.mVibrators.size(); i++) {
            this.mVibrators.valueAt(i).setExternalControl(externalControl);
        }
    }

    private void updateAlwaysOnLocked(AlwaysOnVibration vib) {
        PrebakedSegment effect;
        for (int i = 0; i < vib.effects.size(); i++) {
            VibratorController vibrator = this.mVibrators.get(vib.effects.keyAt(i));
            PrebakedSegment effect2 = vib.effects.valueAt(i);
            if (vibrator != null) {
                Vibration.Status ignoreStatus = shouldIgnoreVibrationLocked(vib.uid, vib.opPkg, vib.attrs);
                if (ignoreStatus == null) {
                    effect = this.mVibrationScaler.scale(effect2, vib.attrs.getUsage());
                } else {
                    effect = null;
                }
                vibrator.updateAlwaysOn(vib.alwaysOnId, effect);
            }
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [687=4] */
    private Vibration.Status startVibrationLocked(final Vibration vib) {
        Trace.traceBegin(8388608L, "startVibrationLocked");
        try {
            vib.updateEffects(new Function() { // from class: com.android.server.vibrator.VibratorManagerService$$ExternalSyntheticLambda3
                @Override // java.util.function.Function
                public final Object apply(Object obj) {
                    return VibratorManagerService.this.m7556xc0a07915(vib, (VibrationEffect) obj);
                }
            });
            boolean inputDevicesAvailable = this.mInputDeviceDelegate.vibrateIfAvailable(vib.uid, vib.opPkg, vib.getEffect(), vib.reason, vib.attrs);
            if (inputDevicesAvailable) {
                return Vibration.Status.FORWARDED_TO_INPUT_DEVICES;
            }
            VibrationStepConductor conductor = new VibrationStepConductor(vib, this.mVibrationSettings, this.mDeviceVibrationEffectAdapter, this.mVibrators, this.mVibrationThreadCallbacks);
            if (this.mCurrentVibration == null) {
                return startVibrationOnThreadLocked(conductor);
            }
            clearNextVibrationLocked(Vibration.Status.IGNORED_SUPERSEDED);
            this.mNextVibration = conductor;
            return Vibration.Status.RUNNING;
        } finally {
            Trace.traceEnd(8388608L);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$startVibrationLocked$1$com-android-server-vibrator-VibratorManagerService  reason: not valid java name */
    public /* synthetic */ VibrationEffect m7556xc0a07915(Vibration vib, VibrationEffect effect) {
        return this.mVibrationScaler.scale(effect, vib.attrs.getUsage());
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [714=5] */
    /* JADX INFO: Access modifiers changed from: private */
    public Vibration.Status startVibrationOnThreadLocked(VibrationStepConductor conductor) {
        Trace.traceBegin(8388608L, "startVibrationThreadLocked");
        try {
            Vibration vib = conductor.getVibration();
            int mode = startAppOpModeLocked(vib.uid, vib.opPkg, vib.attrs);
            switch (mode) {
                case 0:
                    Trace.asyncTraceBegin(8388608L, "vibration", 0);
                    this.mCurrentVibration = conductor;
                    if (this.mVibrationThread.runVibrationOnVibrationThread(conductor)) {
                        return Vibration.Status.RUNNING;
                    }
                    this.mCurrentVibration = null;
                    return Vibration.Status.IGNORED_ERROR_SCHEDULING;
                case 1:
                default:
                    return Vibration.Status.IGNORED_APP_OPS;
                case 2:
                    Slog.w(TAG, "Start AppOpsManager operation errored for uid " + vib.uid);
                    return Vibration.Status.IGNORED_ERROR_APP_OPS;
            }
        } finally {
            Trace.traceEnd(8388608L);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void endVibrationLocked(Vibration vib, Vibration.Status status) {
        vib.end(status);
        logVibrationStatus(vib.uid, vib.attrs, status);
        this.mVibratorManagerRecords.record(vib);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void endVibrationLocked(ExternalVibrationHolder vib, Vibration.Status status) {
        vib.end(status);
        logVibrationStatus(vib.externalVibration.getUid(), vib.externalVibration.getVibrationAttributes(), status);
        this.mVibratorManagerRecords.record(vib);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: com.android.server.vibrator.VibratorManagerService$2  reason: invalid class name */
    /* loaded from: classes2.dex */
    public static /* synthetic */ class AnonymousClass2 {
        static final /* synthetic */ int[] $SwitchMap$com$android$server$vibrator$Vibration$Status;

        static {
            int[] iArr = new int[Vibration.Status.values().length];
            $SwitchMap$com$android$server$vibrator$Vibration$Status = iArr;
            try {
                iArr[Vibration.Status.IGNORED_BACKGROUND.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$android$server$vibrator$Vibration$Status[Vibration.Status.IGNORED_ERROR_APP_OPS.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$com$android$server$vibrator$Vibration$Status[Vibration.Status.IGNORED_FOR_ALARM.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$com$android$server$vibrator$Vibration$Status[Vibration.Status.IGNORED_FOR_EXTERNAL.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
            try {
                $SwitchMap$com$android$server$vibrator$Vibration$Status[Vibration.Status.IGNORED_FOR_ONGOING.ordinal()] = 5;
            } catch (NoSuchFieldError e5) {
            }
            try {
                $SwitchMap$com$android$server$vibrator$Vibration$Status[Vibration.Status.IGNORED_FOR_RINGER_MODE.ordinal()] = 6;
            } catch (NoSuchFieldError e6) {
            }
            try {
                $SwitchMap$com$android$server$vibrator$Vibration$Status[Vibration.Status.IGNORED_FOR_RINGTONE.ordinal()] = 7;
            } catch (NoSuchFieldError e7) {
            }
        }
    }

    private void logVibrationStatus(int uid, VibrationAttributes attrs, Vibration.Status status) {
        switch (AnonymousClass2.$SwitchMap$com$android$server$vibrator$Vibration$Status[status.ordinal()]) {
            case 1:
                Slog.e(TAG, "Ignoring incoming vibration as process with uid= " + uid + " is background, attrs= " + attrs);
                return;
            case 2:
                Slog.w(TAG, "Would be an error: vibrate from uid " + uid);
                return;
            case 3:
            case 4:
            case 5:
            case 6:
            case 7:
            default:
                return;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void reportFinishedVibrationLocked(Vibration.Status status) {
        Trace.traceBegin(8388608L, "reportFinishVibrationLocked");
        Trace.asyncTraceEnd(8388608L, "vibration", 0);
        try {
            Vibration vib = this.mCurrentVibration.getVibration();
            endVibrationLocked(vib, status);
            finishAppOpModeLocked(vib.uid, vib.opPkg);
        } finally {
            Trace.traceEnd(8388608L);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onSyncedVibrationComplete(long vibrationId) {
        synchronized (this.mLock) {
            VibrationStepConductor vibrationStepConductor = this.mCurrentVibration;
            if (vibrationStepConductor != null && vibrationStepConductor.getVibration().id == vibrationId) {
                this.mCurrentVibration.notifySyncedVibrationComplete();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onVibrationComplete(int vibratorId, long vibrationId) {
        synchronized (this.mLock) {
            VibrationStepConductor vibrationStepConductor = this.mCurrentVibration;
            if (vibrationStepConductor != null && vibrationStepConductor.getVibration().id == vibrationId) {
                this.mCurrentVibration.notifyVibratorComplete(vibratorId);
            }
        }
    }

    private Vibration.Status shouldIgnoreVibrationForOngoingLocked(Vibration vib) {
        if (this.mCurrentExternalVibration != null) {
            return Vibration.Status.IGNORED_FOR_EXTERNAL;
        }
        if (this.mCurrentVibration == null || vib.isRepeating()) {
            return null;
        }
        Vibration currentVibration = this.mCurrentVibration.getVibration();
        if (currentVibration.hasEnded()) {
            return null;
        }
        if (currentVibration.attrs.getUsage() == 17) {
            return Vibration.Status.IGNORED_FOR_ALARM;
        }
        if (currentVibration.attrs.getUsage() == 33) {
            return Vibration.Status.IGNORED_FOR_RINGTONE;
        }
        if (currentVibration.isRepeating()) {
            return Vibration.Status.IGNORED_FOR_ONGOING;
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public Vibration.Status shouldIgnoreVibrationLocked(int uid, String opPkg, VibrationAttributes attrs) {
        Vibration.Status statusFromSettings = this.mVibrationSettings.shouldIgnoreVibration(uid, attrs);
        if (statusFromSettings != null) {
            return statusFromSettings;
        }
        int mode = checkAppOpModeLocked(uid, opPkg, attrs);
        if (mode != 0) {
            if (mode == 2) {
                return Vibration.Status.IGNORED_ERROR_APP_OPS;
            }
            return Vibration.Status.IGNORED_APP_OPS;
        }
        return null;
    }

    private boolean shouldCancelVibration(Vibration vib, int usageFilter, IBinder token) {
        return vib.token == token && shouldCancelVibration(vib.attrs, usageFilter);
    }

    private boolean shouldCancelVibration(VibrationAttributes attrs, int usageFilter) {
        return attrs.getUsage() == 0 ? usageFilter == 0 || usageFilter == -1 : (attrs.getUsage() & usageFilter) == attrs.getUsage();
    }

    private int checkAppOpModeLocked(int uid, String opPkg, VibrationAttributes attrs) {
        int mode = this.mAppOps.checkAudioOpNoThrow(3, attrs.getAudioUsage(), uid, opPkg);
        int fixedMode = fixupAppOpModeLocked(mode, attrs);
        if (mode != fixedMode && fixedMode == 0) {
            Slog.d(TAG, "Bypassing DND for vibrate from uid " + uid);
        }
        return fixedMode;
    }

    private int startAppOpModeLocked(int uid, String opPkg, VibrationAttributes attrs) {
        return fixupAppOpModeLocked(this.mAppOps.startOpNoThrow(3, uid, opPkg), attrs);
    }

    private void finishAppOpModeLocked(int uid, String opPkg) {
        this.mAppOps.finishOp(3, uid, opPkg);
    }

    private void enforceUpdateAppOpsStatsPermission(int uid) {
        if (uid == Binder.getCallingUid() || Binder.getCallingPid() == Process.myPid()) {
            return;
        }
        this.mContext.enforcePermission("android.permission.UPDATE_APP_OPS_STATS", Binder.getCallingPid(), Binder.getCallingUid(), null);
    }

    private static boolean isEffectValid(CombinedVibration effect) {
        if (effect == null) {
            Slog.wtf(TAG, "effect must not be null");
            return false;
        }
        try {
            effect.validate();
            return true;
        } catch (Exception e) {
            Slog.wtf(TAG, "Encountered issue when verifying CombinedVibrationEffect.", e);
            return false;
        }
    }

    private void fillVibrationFallbacks(Vibration vib, CombinedVibration effect) {
        if (effect instanceof CombinedVibration.Mono) {
            fillVibrationFallbacks(vib, ((CombinedVibration.Mono) effect).getEffect());
        } else if (effect instanceof CombinedVibration.Stereo) {
            SparseArray<VibrationEffect> effects = ((CombinedVibration.Stereo) effect).getEffects();
            for (int i = 0; i < effects.size(); i++) {
                fillVibrationFallbacks(vib, effects.valueAt(i));
            }
        } else if (effect instanceof CombinedVibration.Sequential) {
            List<CombinedVibration> effects2 = ((CombinedVibration.Sequential) effect).getEffects();
            for (int i2 = 0; i2 < effects2.size(); i2++) {
                fillVibrationFallbacks(vib, effects2.get(i2));
            }
        }
    }

    private void fillVibrationFallbacks(Vibration vib, VibrationEffect effect) {
        if (!(effect instanceof VibrationEffect.Composed)) {
            return;
        }
        VibrationEffect.Composed composed = (VibrationEffect.Composed) effect;
        int segmentCount = composed.getSegments().size();
        for (int i = 0; i < segmentCount; i++) {
            PrebakedSegment prebakedSegment = (VibrationEffectSegment) composed.getSegments().get(i);
            if (prebakedSegment instanceof PrebakedSegment) {
                PrebakedSegment prebaked = prebakedSegment;
                VibrationEffect fallback = this.mVibrationSettings.getFallbackEffect(prebaked.getEffectId());
                if (prebaked.shouldFallback() && fallback != null) {
                    vib.addFallback(prebaked.getEffectId(), fallback);
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public VibrationAttributes fixupVibrationAttributes(VibrationAttributes attrs, CombinedVibration effect) {
        if (attrs == null) {
            attrs = DEFAULT_ATTRIBUTES;
        }
        int usage = attrs.getUsage();
        if (usage == 0 && effect != null && effect.isHapticFeedbackCandidate()) {
            usage = 18;
        }
        int flags = attrs.getFlags();
        if ((flags & 3) != 0 && !hasPermission("android.permission.WRITE_SECURE_SETTINGS") && !hasPermission("android.permission.MODIFY_PHONE_STATE") && !hasPermission("android.permission.MODIFY_AUDIO_ROUTING")) {
            flags &= -4;
        }
        if (usage == attrs.getUsage() && flags == attrs.getFlags()) {
            return attrs;
        }
        return new VibrationAttributes.Builder(attrs).setUsage(usage).setFlags(flags, attrs.getFlags()).build();
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [1095=5] */
    private SparseArray<PrebakedSegment> fixupAlwaysOnEffectsLocked(CombinedVibration effect) {
        SparseArray<VibrationEffect> effects;
        Trace.traceBegin(8388608L, "fixupAlwaysOnEffectsLocked");
        try {
            if (effect instanceof CombinedVibration.Mono) {
                final VibrationEffect syncedEffect = ((CombinedVibration.Mono) effect).getEffect();
                effects = transformAllVibratorsLocked(new Function() { // from class: com.android.server.vibrator.VibratorManagerService$$ExternalSyntheticLambda2
                    @Override // java.util.function.Function
                    public final Object apply(Object obj) {
                        return VibratorManagerService.lambda$fixupAlwaysOnEffectsLocked$2(syncedEffect, (VibratorController) obj);
                    }
                });
            } else if (!(effect instanceof CombinedVibration.Stereo)) {
                return null;
            } else {
                effects = ((CombinedVibration.Stereo) effect).getEffects();
            }
            SparseArray<PrebakedSegment> result = new SparseArray<>();
            for (int i = 0; i < effects.size(); i++) {
                PrebakedSegment prebaked = extractPrebakedSegment(effects.valueAt(i));
                if (prebaked == null) {
                    Slog.e(TAG, "Only prebaked effects supported for always-on.");
                    return null;
                }
                int vibratorId = effects.keyAt(i);
                VibratorController vibrator = this.mVibrators.get(vibratorId);
                if (vibrator != null && vibrator.hasCapability(64L)) {
                    result.put(vibratorId, prebaked);
                }
            }
            int i2 = result.size();
            if (i2 == 0) {
                return null;
            }
            return result;
        } finally {
            Trace.traceEnd(8388608L);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ VibrationEffect lambda$fixupAlwaysOnEffectsLocked$2(VibrationEffect syncedEffect, VibratorController unused) {
        return syncedEffect;
    }

    private static PrebakedSegment extractPrebakedSegment(VibrationEffect effect) {
        if (effect instanceof VibrationEffect.Composed) {
            VibrationEffect.Composed composed = (VibrationEffect.Composed) effect;
            if (composed.getSegments().size() == 1) {
                PrebakedSegment prebakedSegment = (VibrationEffectSegment) composed.getSegments().get(0);
                if (prebakedSegment instanceof PrebakedSegment) {
                    return prebakedSegment;
                }
                return null;
            }
            return null;
        }
        return null;
    }

    private int fixupAppOpModeLocked(int mode, VibrationAttributes attrs) {
        if (mode == 1 && attrs.isFlagSet(1)) {
            return 0;
        }
        return mode;
    }

    private boolean hasPermission(String permission) {
        return this.mContext.checkCallingOrSelfPermission(permission) == 0;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean shouldCancelOnScreenOffLocked(VibrationStepConductor conductor) {
        if (conductor == null) {
            return false;
        }
        Vibration vib = conductor.getVibration();
        return this.mVibrationSettings.shouldCancelVibrationOnScreenOff(vib.uid, vib.opPkg, vib.attrs.getUsage(), vib.startUptimeMillis);
    }

    private void onAllVibratorsLocked(Consumer<VibratorController> consumer) {
        for (int i = 0; i < this.mVibrators.size(); i++) {
            consumer.accept(this.mVibrators.valueAt(i));
        }
    }

    private <T> SparseArray<T> transformAllVibratorsLocked(Function<VibratorController, T> fn) {
        SparseArray<T> ret = new SparseArray<>(this.mVibrators.size());
        for (int i = 0; i < this.mVibrators.size(); i++) {
            ret.put(this.mVibrators.keyAt(i), fn.apply(this.mVibrators.valueAt(i)));
        }
        return ret;
    }

    /* loaded from: classes2.dex */
    static class Injector {
        Injector() {
        }

        NativeWrapper getNativeWrapper() {
            return new NativeWrapper();
        }

        Handler createHandler(Looper looper) {
            return new Handler(looper);
        }

        IBatteryStats getBatteryStatsService() {
            return IBatteryStats.Stub.asInterface(ServiceManager.getService("batterystats"));
        }

        VibratorController createVibratorController(int vibratorId, VibratorController.OnVibrationCompleteListener listener) {
            return new VibratorController(vibratorId, listener);
        }

        void addService(String name, IBinder service) {
            ServiceManager.addService(name, service);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public final class VibrationThreadCallbacks implements VibrationThread.VibratorManagerHooks {
        private VibrationThreadCallbacks() {
        }

        @Override // com.android.server.vibrator.VibrationThread.VibratorManagerHooks
        public boolean prepareSyncedVibration(long requiredCapabilities, int[] vibratorIds) {
            if ((VibratorManagerService.this.mCapabilities & requiredCapabilities) != requiredCapabilities) {
                return false;
            }
            return VibratorManagerService.this.mNativeWrapper.prepareSynced(vibratorIds);
        }

        @Override // com.android.server.vibrator.VibrationThread.VibratorManagerHooks
        public boolean triggerSyncedVibration(long vibrationId) {
            return VibratorManagerService.this.mNativeWrapper.triggerSynced(vibrationId);
        }

        @Override // com.android.server.vibrator.VibrationThread.VibratorManagerHooks
        public void cancelSyncedVibration() {
            VibratorManagerService.this.mNativeWrapper.cancelSynced();
        }

        @Override // com.android.server.vibrator.VibrationThread.VibratorManagerHooks
        public void noteVibratorOn(int uid, long duration) {
            if (duration <= 0) {
                return;
            }
            if (duration == JobStatus.NO_LATEST_RUNTIME) {
                duration = 5000;
            }
            try {
                VibratorManagerService.this.mBatteryStatsService.noteVibratorOn(uid, duration);
                FrameworkStatsLog.write_non_chained(84, uid, (String) null, 1, duration);
            } catch (RemoteException e) {
            }
        }

        @Override // com.android.server.vibrator.VibrationThread.VibratorManagerHooks
        public void noteVibratorOff(int uid) {
            try {
                VibratorManagerService.this.mBatteryStatsService.noteVibratorOff(uid);
                FrameworkStatsLog.write_non_chained(84, uid, (String) null, 0, 0);
            } catch (RemoteException e) {
            }
        }

        @Override // com.android.server.vibrator.VibrationThread.VibratorManagerHooks
        public void onVibrationCompleted(long vibrationId, Vibration.Status status) {
            synchronized (VibratorManagerService.this.mLock) {
                if (VibratorManagerService.this.mCurrentVibration != null && VibratorManagerService.this.mCurrentVibration.getVibration().id == vibrationId) {
                    VibratorManagerService.this.reportFinishedVibrationLocked(status);
                }
            }
        }

        @Override // com.android.server.vibrator.VibrationThread.VibratorManagerHooks
        public void onVibrationThreadReleased(long vibrationId) {
            synchronized (VibratorManagerService.this.mLock) {
                if (Build.IS_DEBUGGABLE && VibratorManagerService.this.mCurrentVibration != null && VibratorManagerService.this.mCurrentVibration.getVibration().id != vibrationId) {
                    Slog.wtf(VibratorManagerService.TAG, TextUtils.formatSimple("VibrationId mismatch on release. expected=%d, released=%d", new Object[]{Long.valueOf(VibratorManagerService.this.mCurrentVibration.getVibration().id), Long.valueOf(vibrationId)}));
                }
                VibratorManagerService.this.mCurrentVibration = null;
                if (VibratorManagerService.this.mNextVibration != null) {
                    VibrationStepConductor nextConductor = VibratorManagerService.this.mNextVibration;
                    VibratorManagerService.this.mNextVibration = null;
                    Vibration.Status status = VibratorManagerService.this.startVibrationOnThreadLocked(nextConductor);
                    if (status != Vibration.Status.RUNNING) {
                        VibratorManagerService.this.endVibrationLocked(nextConductor.getVibration(), status);
                    }
                }
            }
        }
    }

    /* loaded from: classes2.dex */
    private static final class VibrationCompleteListener implements VibratorController.OnVibrationCompleteListener, OnSyncedVibrationCompleteListener {
        private WeakReference<VibratorManagerService> mServiceRef;

        VibrationCompleteListener(VibratorManagerService service) {
            this.mServiceRef = new WeakReference<>(service);
        }

        @Override // com.android.server.vibrator.VibratorManagerService.OnSyncedVibrationCompleteListener
        public void onComplete(long vibrationId) {
            VibratorManagerService service = this.mServiceRef.get();
            if (service != null) {
                service.onSyncedVibrationComplete(vibrationId);
            }
        }

        @Override // com.android.server.vibrator.VibratorController.OnVibrationCompleteListener
        public void onComplete(int vibratorId, long vibrationId) {
            VibratorManagerService service = this.mServiceRef.get();
            if (service != null) {
                service.onVibrationComplete(vibratorId, vibrationId);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static final class AlwaysOnVibration {
        public final int alwaysOnId;
        public final VibrationAttributes attrs;
        public final SparseArray<PrebakedSegment> effects;
        public final String opPkg;
        public final int uid;

        AlwaysOnVibration(int alwaysOnId, int uid, String opPkg, VibrationAttributes attrs, SparseArray<PrebakedSegment> effects) {
            this.alwaysOnId = alwaysOnId;
            this.uid = uid;
            this.opPkg = opPkg;
            this.attrs = attrs;
            this.effects = effects;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public final class ExternalVibrationHolder implements IBinder.DeathRecipient {
        public final ExternalVibration externalVibration;
        private long mEndTimeDebug;
        private long mEndUptimeMillis;
        private final long mStartTimeDebug;
        private final long mStartUptimeMillis;
        private Vibration.Status mStatus;
        public int scale;

        private ExternalVibrationHolder(ExternalVibration externalVibration) {
            this.externalVibration = externalVibration;
            this.scale = 0;
            this.mStartUptimeMillis = SystemClock.uptimeMillis();
            this.mStartTimeDebug = System.currentTimeMillis();
            this.mStatus = Vibration.Status.RUNNING;
        }

        public void end(Vibration.Status status) {
            if (this.mStatus != Vibration.Status.RUNNING) {
                return;
            }
            this.mStatus = status;
            this.mEndUptimeMillis = SystemClock.uptimeMillis();
            this.mEndTimeDebug = System.currentTimeMillis();
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            synchronized (VibratorManagerService.this.mLock) {
                if (VibratorManagerService.this.mCurrentExternalVibration != null) {
                    VibratorManagerService.this.endExternalVibrateLocked(Vibration.Status.CANCELLED_BINDER_DIED, false);
                }
            }
        }

        public Vibration.DebugInfo getDebugInfo() {
            long j = this.mEndUptimeMillis;
            long durationMs = j == 0 ? -1L : j - this.mStartUptimeMillis;
            return new Vibration.DebugInfo(this.mStartTimeDebug, this.mEndTimeDebug, durationMs, null, null, this.scale, this.externalVibration.getVibrationAttributes(), this.externalVibration.getUid(), this.externalVibration.getPackage(), null, this.mStatus);
        }
    }

    /* loaded from: classes2.dex */
    public static class NativeWrapper {
        private long mNativeServicePtr = 0;

        public void init(OnSyncedVibrationCompleteListener listener) {
            this.mNativeServicePtr = VibratorManagerService.nativeInit(listener);
            long finalizerPtr = VibratorManagerService.nativeGetFinalizer();
            if (finalizerPtr != 0) {
                NativeAllocationRegistry registry = NativeAllocationRegistry.createMalloced(VibratorManagerService.class.getClassLoader(), finalizerPtr);
                registry.registerNativeAllocation(this, this.mNativeServicePtr);
            }
        }

        public long getCapabilities() {
            return VibratorManagerService.nativeGetCapabilities(this.mNativeServicePtr);
        }

        public int[] getVibratorIds() {
            return VibratorManagerService.nativeGetVibratorIds(this.mNativeServicePtr);
        }

        public boolean prepareSynced(int[] vibratorIds) {
            return VibratorManagerService.nativePrepareSynced(this.mNativeServicePtr, vibratorIds);
        }

        public boolean triggerSynced(long vibrationId) {
            return VibratorManagerService.nativeTriggerSynced(this.mNativeServicePtr, vibrationId);
        }

        public void cancelSynced() {
            VibratorManagerService.nativeCancelSynced(this.mNativeServicePtr);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static final class VibratorManagerRecords {
        private final int mPreviousVibrationsLimit;
        private final SparseArray<LinkedList<Vibration.DebugInfo>> mPreviousVibrations = new SparseArray<>();
        private final LinkedList<Vibration.DebugInfo> mPreviousExternalVibrations = new LinkedList<>();

        VibratorManagerRecords(int limit) {
            this.mPreviousVibrationsLimit = limit;
        }

        synchronized void record(Vibration vib) {
            int usage = vib.attrs.getUsage();
            if (!this.mPreviousVibrations.contains(usage)) {
                this.mPreviousVibrations.put(usage, new LinkedList<>());
            }
            record(this.mPreviousVibrations.get(usage), vib.getDebugInfo());
        }

        synchronized void record(ExternalVibrationHolder vib) {
            record(this.mPreviousExternalVibrations, vib.getDebugInfo());
        }

        synchronized void record(LinkedList<Vibration.DebugInfo> records, Vibration.DebugInfo info) {
            if (records.size() > this.mPreviousVibrationsLimit) {
                records.removeFirst();
            }
            records.addLast(info);
        }

        synchronized void dumpText(PrintWriter pw) {
            for (int i = 0; i < this.mPreviousVibrations.size(); i++) {
                pw.println();
                pw.print("  Previous vibrations for usage ");
                pw.print(VibrationAttributes.usageToString(this.mPreviousVibrations.keyAt(i)));
                pw.println(":");
                Iterator<Vibration.DebugInfo> it = this.mPreviousVibrations.valueAt(i).iterator();
                while (it.hasNext()) {
                    Vibration.DebugInfo info = it.next();
                    pw.println("    " + info);
                }
            }
            pw.println();
            pw.println("  Previous external vibrations:");
            Iterator<Vibration.DebugInfo> it2 = this.mPreviousExternalVibrations.iterator();
            while (it2.hasNext()) {
                Vibration.DebugInfo info2 = it2.next();
                pw.println("    " + info2);
            }
        }

        synchronized void dumpProto(ProtoOutputStream proto) {
            long fieldId;
            for (int i = 0; i < this.mPreviousVibrations.size(); i++) {
                switch (this.mPreviousVibrations.keyAt(i)) {
                    case 17:
                        fieldId = 2246267895823L;
                        break;
                    case 33:
                        fieldId = 2246267895821L;
                        break;
                    case 49:
                        fieldId = 2246267895822L;
                        break;
                    default:
                        fieldId = 2246267895824L;
                        break;
                }
                Iterator<Vibration.DebugInfo> it = this.mPreviousVibrations.valueAt(i).iterator();
                while (it.hasNext()) {
                    Vibration.DebugInfo info = it.next();
                    info.dumpProto(proto, fieldId);
                }
            }
            Iterator<Vibration.DebugInfo> it2 = this.mPreviousExternalVibrations.iterator();
            while (it2.hasNext()) {
                Vibration.DebugInfo info2 = it2.next();
                info2.dumpProto(proto, 2246267895825L);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void clearNextVibrationLocked(Vibration.Status endStatus) {
        VibrationStepConductor vibrationStepConductor = this.mNextVibration;
        if (vibrationStepConductor != null) {
            endVibrationLocked(vibrationStepConductor.getVibration(), endStatus);
            this.mNextVibration = null;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void endExternalVibrateLocked(Vibration.Status status, boolean continueExternalControl) {
        Trace.traceBegin(8388608L, "endExternalVibrateLocked");
        try {
            ExternalVibrationHolder externalVibrationHolder = this.mCurrentExternalVibration;
            if (externalVibrationHolder == null) {
                return;
            }
            endVibrationLocked(externalVibrationHolder, status);
            this.mCurrentExternalVibration.externalVibration.unlinkToDeath(this.mCurrentExternalVibration);
            this.mCurrentExternalVibration = null;
            if (!continueExternalControl) {
                setExternalControl(false);
            }
        } finally {
            Trace.traceEnd(8388608L);
        }
    }

    /* loaded from: classes2.dex */
    final class ExternalVibratorService extends IExternalVibratorService.Stub {
        ExternalVibratorService() {
        }

        public int onExternalVibrationStart(ExternalVibration vib) {
            if (hasExternalControlCapability()) {
                if (ActivityManager.checkComponentPermission("android.permission.VIBRATE", vib.getUid(), -1, true) != 0) {
                    Slog.w(VibratorManagerService.TAG, "pkg=" + vib.getPackage() + ", uid=" + vib.getUid() + " tried to play externally controlled vibration without VIBRATE permission, ignoring.");
                    return -100;
                }
                VibrationAttributes attrs = VibratorManagerService.this.fixupVibrationAttributes(vib.getVibrationAttributes(), null);
                if (attrs.isFlagSet(3)) {
                    VibratorManagerService.this.mVibrationSettings.update();
                }
                boolean alreadyUnderExternalControl = false;
                boolean waitForCompletion = false;
                synchronized (VibratorManagerService.this.mLock) {
                    Vibration.Status ignoreStatus = VibratorManagerService.this.shouldIgnoreVibrationLocked(vib.getUid(), vib.getPackage(), attrs);
                    if (ignoreStatus != null) {
                        ExternalVibrationHolder vibHolder = new ExternalVibrationHolder(vib);
                        vibHolder.scale = -100;
                        VibratorManagerService.this.endVibrationLocked(vibHolder, ignoreStatus);
                        return vibHolder.scale;
                    } else if (VibratorManagerService.this.mCurrentExternalVibration != null && VibratorManagerService.this.mCurrentExternalVibration.externalVibration.equals(vib)) {
                        return VibratorManagerService.this.mCurrentExternalVibration.scale;
                    } else {
                        if (VibratorManagerService.this.mCurrentExternalVibration == null) {
                            if (VibratorManagerService.this.mCurrentVibration != null) {
                                VibratorManagerService.this.clearNextVibrationLocked(Vibration.Status.IGNORED_FOR_EXTERNAL);
                                VibratorManagerService.this.mCurrentVibration.notifyCancelled(Vibration.Status.CANCELLED_SUPERSEDED, true);
                                waitForCompletion = true;
                            }
                        } else {
                            alreadyUnderExternalControl = true;
                            VibratorManagerService.this.mCurrentExternalVibration.externalVibration.mute();
                            VibratorManagerService.this.endExternalVibrateLocked(Vibration.Status.CANCELLED_SUPERSEDED, true);
                        }
                        VibratorManagerService vibratorManagerService = VibratorManagerService.this;
                        vibratorManagerService.mCurrentExternalVibration = new ExternalVibrationHolder(vib);
                        vib.linkToDeath(VibratorManagerService.this.mCurrentExternalVibration);
                        VibratorManagerService.this.mCurrentExternalVibration.scale = VibratorManagerService.this.mVibrationScaler.getExternalVibrationScale(attrs.getUsage());
                        int scale = VibratorManagerService.this.mCurrentExternalVibration.scale;
                        if (waitForCompletion && !VibratorManagerService.this.mVibrationThread.waitForThreadIdle(5000L)) {
                            Slog.e(VibratorManagerService.TAG, "Timed out waiting for vibration to cancel");
                            synchronized (VibratorManagerService.this.mLock) {
                                VibratorManagerService.this.endExternalVibrateLocked(Vibration.Status.IGNORED_ERROR_CANCELLING, false);
                            }
                            return -100;
                        }
                        if (!alreadyUnderExternalControl) {
                            VibratorManagerService.this.setExternalControl(true);
                        }
                        return scale;
                    }
                }
            }
            return -100;
        }

        public void onExternalVibrationStop(ExternalVibration vib) {
            synchronized (VibratorManagerService.this.mLock) {
                if (VibratorManagerService.this.mCurrentExternalVibration != null && VibratorManagerService.this.mCurrentExternalVibration.externalVibration.equals(vib)) {
                    VibratorManagerService.this.endExternalVibrateLocked(Vibration.Status.FINISHED, false);
                }
            }
        }

        private boolean hasExternalControlCapability() {
            for (int i = 0; i < VibratorManagerService.this.mVibrators.size(); i++) {
                if (((VibratorController) VibratorManagerService.this.mVibrators.valueAt(i)).hasCapability(8L)) {
                    return true;
                }
            }
            return false;
        }
    }

    /* loaded from: classes2.dex */
    private final class VibratorManagerShellCommand extends ShellCommand {
        public static final String SHELL_PACKAGE_NAME = "com.android.shell";
        private final IBinder mShellCallbacksToken;

        /* JADX INFO: Access modifiers changed from: private */
        /* loaded from: classes2.dex */
        public final class CommonOptions {
            public boolean background;
            public String description;
            public boolean force;

            CommonOptions() {
                this.force = false;
                this.description = NotificationShellCmd.CHANNEL_NAME;
                this.background = false;
                while (true) {
                    String nextArg = VibratorManagerShellCommand.this.peekNextArg();
                    if (nextArg != null) {
                        char c = 65535;
                        switch (nextArg.hashCode()) {
                            case 1461:
                                if (nextArg.equals("-B")) {
                                    c = 1;
                                    break;
                                }
                                break;
                            case 1495:
                                if (nextArg.equals("-d")) {
                                    c = 2;
                                    break;
                                }
                                break;
                            case 1497:
                                if (nextArg.equals("-f")) {
                                    c = 0;
                                    break;
                                }
                                break;
                        }
                        switch (c) {
                            case 0:
                                VibratorManagerShellCommand.this.getNextArgRequired();
                                this.force = true;
                                break;
                            case 1:
                                VibratorManagerShellCommand.this.getNextArgRequired();
                                this.background = true;
                                break;
                            case 2:
                                VibratorManagerShellCommand.this.getNextArgRequired();
                                this.description = VibratorManagerShellCommand.this.getNextArgRequired();
                                break;
                            default:
                                return;
                        }
                    } else {
                        return;
                    }
                }
            }
        }

        private VibratorManagerShellCommand(IBinder shellCallbacksToken) {
            this.mShellCallbacksToken = shellCallbacksToken;
        }

        /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [1730=7] */
        public int onCommand(String cmd) {
            Trace.traceBegin(8388608L, "onCommand " + cmd);
            try {
                return "list".equals(cmd) ? runListVibrators() : "synced".equals(cmd) ? runMono() : "combined".equals(cmd) ? runStereo() : "sequential".equals(cmd) ? runSequential() : "cancel".equals(cmd) ? runCancel() : handleDefaultCommands(cmd);
            } finally {
                Trace.traceEnd(8388608L);
            }
        }

        private int runListVibrators() {
            int[] iArr;
            PrintWriter pw = getOutPrintWriter();
            try {
                if (VibratorManagerService.this.mVibratorIds.length == 0) {
                    pw.println("No vibrator found");
                } else {
                    for (int id : VibratorManagerService.this.mVibratorIds) {
                        pw.println(id);
                    }
                }
                pw.println("");
                if (pw != null) {
                    pw.close();
                }
                return 0;
            } catch (Throwable th) {
                if (pw != null) {
                    try {
                        pw.close();
                    } catch (Throwable th2) {
                        th.addSuppressed(th2);
                    }
                }
                throw th;
            }
        }

        /* JADX WARN: Multi-variable type inference failed */
        /* JADX WARN: Type inference failed for: r0v4, types: [com.android.server.vibrator.VibratorManagerService] */
        private void runVibrate(CommonOptions commonOptions, CombinedVibration combined) {
            VibrationAttributes attrs = createVibrationAttributes(commonOptions);
            IBinder deathBinder = commonOptions.background ? VibratorManagerService.this : this.mShellCallbacksToken;
            Vibration vib = VibratorManagerService.this.vibrateInternal(Binder.getCallingUid(), SHELL_PACKAGE_NAME, combined, attrs, commonOptions.description, deathBinder);
            if (vib != null && !commonOptions.background) {
                try {
                    vib.waitForEnd();
                } catch (InterruptedException e) {
                }
            }
        }

        private int runMono() {
            runVibrate(new CommonOptions(), CombinedVibration.createParallel(nextEffect()));
            return 0;
        }

        private int runStereo() {
            CommonOptions commonOptions = new CommonOptions();
            CombinedVibration.ParallelCombination combination = CombinedVibration.startParallel();
            while ("-v".equals(getNextOption())) {
                int vibratorId = Integer.parseInt(getNextArgRequired());
                combination.addVibrator(vibratorId, nextEffect());
            }
            runVibrate(commonOptions, combination.combine());
            return 0;
        }

        private int runSequential() {
            CommonOptions commonOptions = new CommonOptions();
            CombinedVibration.SequentialCombination combination = CombinedVibration.startSequential();
            while ("-v".equals(getNextOption())) {
                int vibratorId = Integer.parseInt(getNextArgRequired());
                combination.addNext(vibratorId, nextEffect());
            }
            runVibrate(commonOptions, combination.combine());
            return 0;
        }

        /* JADX WARN: Type inference failed for: r0v0, types: [com.android.server.vibrator.VibratorManagerService, android.os.IBinder] */
        private int runCancel() {
            ?? r0 = VibratorManagerService.this;
            r0.cancelVibrate(-1, r0);
            return 0;
        }

        private VibrationEffect nextEffect() {
            VibrationEffect.Composition composition = VibrationEffect.startComposition();
            while (true) {
                String nextArg = peekNextArg();
                if (nextArg != null) {
                    if ("oneshot".equals(nextArg)) {
                        addOneShotToComposition(composition);
                    } else if ("waveform".equals(nextArg)) {
                        addWaveformToComposition(composition);
                    } else if ("prebaked".equals(nextArg)) {
                        addPrebakedToComposition(composition);
                    } else if (!"primitives".equals(nextArg)) {
                        break;
                    } else {
                        addPrimitivesToComposition(composition);
                    }
                } else {
                    break;
                }
            }
            return composition.compose();
        }

        private void addOneShotToComposition(VibrationEffect.Composition composition) {
            boolean hasAmplitude = false;
            int delay = 0;
            getNextArgRequired();
            while (true) {
                String nextOption = getNextOption();
                if (nextOption == null) {
                    break;
                } else if ("-a".equals(nextOption)) {
                    hasAmplitude = true;
                } else if ("-w".equals(nextOption)) {
                    delay = Integer.parseInt(getNextArgRequired());
                }
            }
            long duration = Long.parseLong(getNextArgRequired());
            int amplitude = hasAmplitude ? Integer.parseInt(getNextArgRequired()) : -1;
            composition.addOffDuration(Duration.ofMillis(delay));
            composition.addEffect(VibrationEffect.createOneShot(duration, amplitude));
        }

        private void addWaveformToComposition(VibrationEffect.Composition composition) {
            String nextOption;
            Duration transitionDuration;
            int delay;
            String nextOption2;
            Duration ofMillis;
            getNextArgRequired();
            int delay2 = 0;
            int repeat = -1;
            boolean isContinuous = false;
            boolean hasFrequencies = false;
            boolean hasFrequencies2 = false;
            while (true) {
                String nextOption3 = getNextOption();
                nextOption = nextOption3;
                if (nextOption3 == null) {
                    break;
                } else if ("-a".equals(nextOption)) {
                    hasFrequencies2 = true;
                } else if ("-r".equals(nextOption)) {
                    repeat = Integer.parseInt(getNextArgRequired());
                } else if ("-w".equals(nextOption)) {
                    delay2 = Integer.parseInt(getNextArgRequired());
                } else if ("-f".equals(nextOption)) {
                    hasFrequencies = true;
                } else if ("-c".equals(nextOption)) {
                    isContinuous = true;
                }
            }
            List<Integer> durations = new ArrayList<>();
            List<Float> amplitudes = new ArrayList<>();
            List<Float> frequencies = new ArrayList<>();
            float nextAmplitude = 0.0f;
            while (true) {
                String nextArg = peekNextArg();
                if (nextArg == null) {
                    break;
                }
                try {
                    durations.add(Integer.valueOf(Integer.parseInt(nextArg)));
                    getNextArgRequired();
                    if (hasFrequencies2) {
                        amplitudes.add(Float.valueOf(Float.parseFloat(getNextArgRequired()) / 255.0f));
                    } else {
                        amplitudes.add(Float.valueOf(nextAmplitude));
                        nextAmplitude = 1.0f - nextAmplitude;
                    }
                    if (hasFrequencies) {
                        frequencies.add(Float.valueOf(Float.parseFloat(getNextArgRequired())));
                    }
                } catch (NumberFormatException e) {
                }
            }
            composition.addOffDuration(Duration.ofMillis(delay2));
            VibrationEffect.WaveformBuilder waveform = VibrationEffect.startWaveform();
            int i = 0;
            while (i < durations.size()) {
                if (isContinuous) {
                    transitionDuration = Duration.ofMillis(durations.get(i).intValue());
                } else {
                    transitionDuration = Duration.ZERO;
                }
                if (isContinuous) {
                    ofMillis = Duration.ZERO;
                    delay = delay2;
                    nextOption2 = nextOption;
                } else {
                    delay = delay2;
                    nextOption2 = nextOption;
                    ofMillis = Duration.ofMillis(durations.get(i).intValue());
                }
                Duration sustainDuration = ofMillis;
                if (hasFrequencies) {
                    waveform.addTransition(transitionDuration, VibrationEffect.VibrationParameter.targetAmplitude(amplitudes.get(i).floatValue()), VibrationEffect.VibrationParameter.targetFrequency(frequencies.get(i).floatValue()));
                } else {
                    waveform.addTransition(transitionDuration, VibrationEffect.VibrationParameter.targetAmplitude(amplitudes.get(i).floatValue()));
                }
                if (!sustainDuration.isZero()) {
                    waveform.addSustain(sustainDuration);
                }
                if (i > 0 && i == repeat) {
                    composition.addEffect(waveform.build());
                    if (hasFrequencies) {
                        waveform = VibrationEffect.startWaveform(VibrationEffect.VibrationParameter.targetAmplitude(amplitudes.get(i).floatValue()), VibrationEffect.VibrationParameter.targetFrequency(frequencies.get(i).floatValue()));
                    } else {
                        waveform = VibrationEffect.startWaveform(VibrationEffect.VibrationParameter.targetAmplitude(amplitudes.get(i).floatValue()));
                    }
                }
                i++;
                nextOption = nextOption2;
                delay2 = delay;
            }
            if (repeat < 0) {
                composition.addEffect(waveform.build());
            } else {
                composition.repeatEffectIndefinitely(waveform.build());
            }
        }

        private void addPrebakedToComposition(VibrationEffect.Composition composition) {
            boolean shouldFallback = false;
            int delay = 0;
            getNextArgRequired();
            while (true) {
                String nextOption = getNextOption();
                if (nextOption != null) {
                    if ("-b".equals(nextOption)) {
                        shouldFallback = true;
                    } else if ("-w".equals(nextOption)) {
                        delay = Integer.parseInt(getNextArgRequired());
                    }
                } else {
                    int effectId = Integer.parseInt(getNextArgRequired());
                    composition.addOffDuration(Duration.ofMillis(delay));
                    composition.addEffect(VibrationEffect.get(effectId, shouldFallback));
                    return;
                }
            }
        }

        private void addPrimitivesToComposition(VibrationEffect.Composition composition) {
            getNextArgRequired();
            while (true) {
                String peekNextArg = peekNextArg();
                String nextArg = peekNextArg;
                if (peekNextArg != null) {
                    int delay = 0;
                    if ("-w".equals(nextArg)) {
                        getNextArgRequired();
                        delay = Integer.parseInt(getNextArgRequired());
                        nextArg = peekNextArg();
                    }
                    try {
                        composition.addPrimitive(Integer.parseInt(nextArg), 1.0f, delay);
                        getNextArgRequired();
                    } catch (NullPointerException | NumberFormatException e) {
                        return;
                    }
                } else {
                    return;
                }
            }
        }

        private VibrationAttributes createVibrationAttributes(CommonOptions commonOptions) {
            boolean z = commonOptions.force;
            VibrationAttributes.Builder builder = new VibrationAttributes.Builder();
            int flags = z ? 1 : 0;
            return builder.setFlags(flags).setUsage(18).build();
        }

        public void onHelp() {
            PrintWriter pw = getOutPrintWriter();
            try {
                pw.println("Vibrator Manager commands:");
                pw.println("  help");
                pw.println("    Prints this help text.");
                pw.println("");
                pw.println("  list");
                pw.println("    Prints the id of device vibrators. This does not include any ");
                pw.println("    connected input device.");
                pw.println("  synced [options] <effect>...");
                pw.println("    Vibrates effect on all vibrators in sync.");
                pw.println("  combined [options] (-v <vibrator-id> <effect>...)...");
                pw.println("    Vibrates different effects on each vibrator in sync.");
                pw.println("  sequential [options] (-v <vibrator-id> <effect>...)...");
                pw.println("    Vibrates different effects on each vibrator in sequence.");
                pw.println("  cancel");
                pw.println("    Cancels any active vibration");
                pw.println("");
                pw.println("Effect commands:");
                pw.println("  oneshot [-w delay] [-a] <duration> [<amplitude>]");
                pw.println("    Vibrates for duration milliseconds; ignored when device is on ");
                pw.println("    DND (Do Not Disturb) mode; touch feedback strength user setting ");
                pw.println("    will be used to scale amplitude.");
                pw.println("    If -w is provided, the effect will be played after the specified");
                pw.println("    wait time in milliseconds.");
                pw.println("    If -a is provided, the command accepts a second argument for ");
                pw.println("    amplitude, in a scale of 1-255.");
                pw.print("  waveform [-w delay] [-r index] [-a] [-f] [-c] ");
                pw.println("(<duration> [<amplitude>] [<frequency>])...");
                pw.println("    Vibrates for durations and amplitudes in list; ignored when ");
                pw.println("    device is on DND (Do Not Disturb) mode; touch feedback strength ");
                pw.println("    user setting will be used to scale amplitude.");
                pw.println("    If -w is provided, the effect will be played after the specified");
                pw.println("    wait time in milliseconds.");
                pw.println("    If -r is provided, the waveform loops back to the specified");
                pw.println("    index (e.g. 0 loops from the beginning)");
                pw.println("    If -a is provided, the command expects amplitude to follow each");
                pw.println("    duration; otherwise, it accepts durations only and alternates");
                pw.println("    off/on");
                pw.println("    If -f is provided, the command expects frequency to follow each");
                pw.println("    amplitude or duration; otherwise, it uses resonant frequency");
                pw.println("    If -c is provided, the waveform is continuous and will ramp");
                pw.println("    between values; otherwise each entry is a fixed step.");
                pw.println("    Duration is in milliseconds; amplitude is a scale of 1-255;");
                pw.println("    frequency is an absolute value in hertz;");
                pw.println("  prebaked [-w delay] [-b] <effect-id>");
                pw.println("    Vibrates with prebaked effect; ignored when device is on DND ");
                pw.println("    (Do Not Disturb) mode; touch feedback strength user setting ");
                pw.println("    will be used to scale amplitude.");
                pw.println("    If -w is provided, the effect will be played after the specified");
                pw.println("    wait time in milliseconds.");
                pw.println("    If -b is provided, the prebaked fallback effect will be played if");
                pw.println("    the device doesn't support the given effect-id.");
                pw.println("  primitives ([-w delay] <primitive-id>)...");
                pw.println("    Vibrates with a composed effect; ignored when device is on DND ");
                pw.println("    (Do Not Disturb) mode; touch feedback strength user setting ");
                pw.println("    will be used to scale primitive intensities.");
                pw.println("    If -w is provided, the next primitive will be played after the ");
                pw.println("    specified wait time in milliseconds.");
                pw.println("");
                pw.println("Common Options:");
                pw.println("  -f");
                pw.println("    Force. Ignore Do Not Disturb setting.");
                pw.println("  -B");
                pw.println("    Run in the background; without this option the shell cmd will");
                pw.println("    block until the vibration has completed.");
                pw.println("  -d <description>");
                pw.println("    Add description to the vibration.");
                pw.println("");
                if (pw != null) {
                    pw.close();
                }
            } catch (Throwable th) {
                if (pw != null) {
                    try {
                        pw.close();
                    } catch (Throwable th2) {
                        th.addSuppressed(th2);
                    }
                }
                throw th;
            }
        }
    }
}
