package com.android.server.power;

import android.content.Context;
import android.hardware.thermal.V1_0.IThermal;
import android.hardware.thermal.V1_0.ThermalStatus;
import android.hardware.thermal.V1_1.IThermalCallback;
import android.hardware.thermal.V2_0.IThermal;
import android.hardware.thermal.V2_0.IThermalChangedCallback;
import android.hardware.thermal.V2_0.TemperatureThreshold;
import android.os.Binder;
import android.os.CoolingDevice;
import android.os.Handler;
import android.os.IHwBinder;
import android.os.IThermalEventListener;
import android.os.IThermalService;
import android.os.IThermalStatusListener;
import android.os.PowerManager;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ShellCallback;
import android.os.ShellCommand;
import android.os.SystemClock;
import android.os.Temperature;
import android.util.ArrayMap;
import android.util.EventLog;
import android.util.Slog;
import com.android.internal.os.BackgroundThread;
import com.android.internal.util.DumpUtils;
import com.android.server.EventLogTags;
import com.android.server.FgThread;
import com.android.server.SystemService;
import com.android.server.UiModeManagerService;
import com.android.server.job.controllers.JobStatus;
import com.android.server.power.ThermalManagerService;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
/* loaded from: classes2.dex */
public class ThermalManagerService extends SystemService {
    private static final String TAG = ThermalManagerService.class.getSimpleName();
    private final AtomicBoolean mHalReady;
    private ThermalHalWrapper mHalWrapper;
    private boolean mIsStatusOverride;
    private final Object mLock;
    final IThermalService.Stub mService;
    private int mStatus;
    private ArrayMap<String, Temperature> mTemperatureMap;
    final TemperatureWatcher mTemperatureWatcher;
    private final RemoteCallbackList<IThermalEventListener> mThermalEventListeners;
    private final RemoteCallbackList<IThermalStatusListener> mThermalStatusListeners;

    public ThermalManagerService(Context context) {
        this(context, null);
    }

    ThermalManagerService(Context context, ThermalHalWrapper halWrapper) {
        super(context);
        this.mLock = new Object();
        this.mThermalEventListeners = new RemoteCallbackList<>();
        this.mThermalStatusListeners = new RemoteCallbackList<>();
        this.mTemperatureMap = new ArrayMap<>();
        this.mHalReady = new AtomicBoolean();
        this.mTemperatureWatcher = new TemperatureWatcher();
        this.mService = new IThermalService.Stub() { // from class: com.android.server.power.ThermalManagerService.1
            public boolean registerThermalEventListener(IThermalEventListener listener) {
                ThermalManagerService.this.getContext().enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
                synchronized (ThermalManagerService.this.mLock) {
                    long token = Binder.clearCallingIdentity();
                    if (ThermalManagerService.this.mThermalEventListeners.register(listener, null)) {
                        ThermalManagerService.this.postEventListenerCurrentTemperatures(listener, null);
                        Binder.restoreCallingIdentity(token);
                        return true;
                    }
                    Binder.restoreCallingIdentity(token);
                    return false;
                }
            }

            public boolean registerThermalEventListenerWithType(IThermalEventListener listener, int type) {
                ThermalManagerService.this.getContext().enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
                synchronized (ThermalManagerService.this.mLock) {
                    long token = Binder.clearCallingIdentity();
                    if (ThermalManagerService.this.mThermalEventListeners.register(listener, new Integer(type))) {
                        ThermalManagerService.this.postEventListenerCurrentTemperatures(listener, new Integer(type));
                        Binder.restoreCallingIdentity(token);
                        return true;
                    }
                    Binder.restoreCallingIdentity(token);
                    return false;
                }
            }

            public boolean unregisterThermalEventListener(IThermalEventListener listener) {
                boolean unregister;
                ThermalManagerService.this.getContext().enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
                synchronized (ThermalManagerService.this.mLock) {
                    long token = Binder.clearCallingIdentity();
                    unregister = ThermalManagerService.this.mThermalEventListeners.unregister(listener);
                    Binder.restoreCallingIdentity(token);
                }
                return unregister;
            }

            public Temperature[] getCurrentTemperatures() {
                ThermalManagerService.this.getContext().enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
                long token = Binder.clearCallingIdentity();
                try {
                    if (!ThermalManagerService.this.mHalReady.get()) {
                        return new Temperature[0];
                    }
                    List<Temperature> curr = ThermalManagerService.this.mHalWrapper.getCurrentTemperatures(false, 0);
                    return (Temperature[]) curr.toArray(new Temperature[curr.size()]);
                } finally {
                    Binder.restoreCallingIdentity(token);
                }
            }

            public Temperature[] getCurrentTemperaturesWithType(int type) {
                ThermalManagerService.this.getContext().enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
                long token = Binder.clearCallingIdentity();
                try {
                    if (!ThermalManagerService.this.mHalReady.get()) {
                        return new Temperature[0];
                    }
                    List<Temperature> curr = ThermalManagerService.this.mHalWrapper.getCurrentTemperatures(true, type);
                    return (Temperature[]) curr.toArray(new Temperature[curr.size()]);
                } finally {
                    Binder.restoreCallingIdentity(token);
                }
            }

            public boolean registerThermalStatusListener(IThermalStatusListener listener) {
                synchronized (ThermalManagerService.this.mLock) {
                    long token = Binder.clearCallingIdentity();
                    if (ThermalManagerService.this.mThermalStatusListeners.register(listener)) {
                        ThermalManagerService.this.postStatusListener(listener);
                        Binder.restoreCallingIdentity(token);
                        return true;
                    }
                    Binder.restoreCallingIdentity(token);
                    return false;
                }
            }

            public boolean unregisterThermalStatusListener(IThermalStatusListener listener) {
                boolean unregister;
                synchronized (ThermalManagerService.this.mLock) {
                    long token = Binder.clearCallingIdentity();
                    unregister = ThermalManagerService.this.mThermalStatusListeners.unregister(listener);
                    Binder.restoreCallingIdentity(token);
                }
                return unregister;
            }

            public int getCurrentThermalStatus() {
                int i;
                synchronized (ThermalManagerService.this.mLock) {
                    long token = Binder.clearCallingIdentity();
                    i = ThermalManagerService.this.mStatus;
                    Binder.restoreCallingIdentity(token);
                }
                return i;
            }

            public CoolingDevice[] getCurrentCoolingDevices() {
                ThermalManagerService.this.getContext().enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
                long token = Binder.clearCallingIdentity();
                try {
                    if (!ThermalManagerService.this.mHalReady.get()) {
                        return new CoolingDevice[0];
                    }
                    List<CoolingDevice> devList = ThermalManagerService.this.mHalWrapper.getCurrentCoolingDevices(false, 0);
                    return (CoolingDevice[]) devList.toArray(new CoolingDevice[devList.size()]);
                } finally {
                    Binder.restoreCallingIdentity(token);
                }
            }

            public CoolingDevice[] getCurrentCoolingDevicesWithType(int type) {
                ThermalManagerService.this.getContext().enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
                long token = Binder.clearCallingIdentity();
                try {
                    if (!ThermalManagerService.this.mHalReady.get()) {
                        return new CoolingDevice[0];
                    }
                    List<CoolingDevice> devList = ThermalManagerService.this.mHalWrapper.getCurrentCoolingDevices(true, type);
                    return (CoolingDevice[]) devList.toArray(new CoolingDevice[devList.size()]);
                } finally {
                    Binder.restoreCallingIdentity(token);
                }
            }

            public float getThermalHeadroom(int forecastSeconds) {
                if (!ThermalManagerService.this.mHalReady.get()) {
                    return Float.NaN;
                }
                return ThermalManagerService.this.mTemperatureWatcher.getForecast(forecastSeconds);
            }

            private void dumpItemsLocked(PrintWriter pw, String prefix, Collection<?> items) {
                Iterator iterator = items.iterator();
                while (iterator.hasNext()) {
                    pw.println(prefix + iterator.next().toString());
                }
            }

            public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
                if (!DumpUtils.checkDumpPermission(ThermalManagerService.this.getContext(), ThermalManagerService.TAG, pw)) {
                    return;
                }
                long token = Binder.clearCallingIdentity();
                try {
                    synchronized (ThermalManagerService.this.mLock) {
                        pw.println("IsStatusOverride: " + ThermalManagerService.this.mIsStatusOverride);
                        pw.println("ThermalEventListeners:");
                        ThermalManagerService.this.mThermalEventListeners.dump(pw, "\t");
                        pw.println("ThermalStatusListeners:");
                        ThermalManagerService.this.mThermalStatusListeners.dump(pw, "\t");
                        pw.println("Thermal Status: " + ThermalManagerService.this.mStatus);
                        pw.println("Cached temperatures:");
                        dumpItemsLocked(pw, "\t", ThermalManagerService.this.mTemperatureMap.values());
                        pw.println("HAL Ready: " + ThermalManagerService.this.mHalReady.get());
                        if (ThermalManagerService.this.mHalReady.get()) {
                            pw.println("HAL connection:");
                            ThermalManagerService.this.mHalWrapper.dump(pw, "\t");
                            pw.println("Current temperatures from HAL:");
                            dumpItemsLocked(pw, "\t", ThermalManagerService.this.mHalWrapper.getCurrentTemperatures(false, 0));
                            pw.println("Current cooling devices from HAL:");
                            dumpItemsLocked(pw, "\t", ThermalManagerService.this.mHalWrapper.getCurrentCoolingDevices(false, 0));
                            pw.println("Temperature static thresholds from HAL:");
                            dumpItemsLocked(pw, "\t", ThermalManagerService.this.mHalWrapper.getTemperatureThresholds(false, 0));
                        }
                    }
                } finally {
                    Binder.restoreCallingIdentity(token);
                }
            }

            private boolean isCallerShell() {
                int callingUid = Binder.getCallingUid();
                return callingUid == 2000 || callingUid == 0;
            }

            /* JADX DEBUG: Multi-variable search result rejected for r10v0, resolved type: com.android.server.power.ThermalManagerService$1 */
            /* JADX WARN: Multi-variable type inference failed */
            public void onShellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ShellCallback callback, ResultReceiver resultReceiver) {
                if (!isCallerShell()) {
                    Slog.w(ThermalManagerService.TAG, "Only shell is allowed to call thermalservice shell commands");
                } else {
                    new ThermalShellCommand().exec(this, in, out, err, args, callback, resultReceiver);
                }
            }
        };
        this.mHalWrapper = halWrapper;
        this.mStatus = 0;
    }

    @Override // com.android.server.SystemService
    public void onStart() {
        publishBinderService("thermalservice", this.mService);
    }

    @Override // com.android.server.SystemService
    public void onBootPhase(int phase) {
        if (phase == 550) {
            onActivityManagerReady();
        }
    }

    private void onActivityManagerReady() {
        synchronized (this.mLock) {
            boolean halConnected = this.mHalWrapper != null;
            if (!halConnected) {
                ThermalHal20Wrapper thermalHal20Wrapper = new ThermalHal20Wrapper();
                this.mHalWrapper = thermalHal20Wrapper;
                halConnected = thermalHal20Wrapper.connectToHal();
            }
            if (!halConnected) {
                ThermalHal11Wrapper thermalHal11Wrapper = new ThermalHal11Wrapper();
                this.mHalWrapper = thermalHal11Wrapper;
                halConnected = thermalHal11Wrapper.connectToHal();
            }
            if (!halConnected) {
                ThermalHal10Wrapper thermalHal10Wrapper = new ThermalHal10Wrapper();
                this.mHalWrapper = thermalHal10Wrapper;
                halConnected = thermalHal10Wrapper.connectToHal();
            }
            this.mHalWrapper.setCallback(new ThermalHalWrapper.TemperatureChangedCallback() { // from class: com.android.server.power.ThermalManagerService$$ExternalSyntheticLambda2
                @Override // com.android.server.power.ThermalManagerService.ThermalHalWrapper.TemperatureChangedCallback
                public final void onValues(Temperature temperature) {
                    ThermalManagerService.this.onTemperatureChangedCallback(temperature);
                }
            });
            if (!halConnected) {
                Slog.w(TAG, "No Thermal HAL service on this device");
                return;
            }
            List<Temperature> temperatures = this.mHalWrapper.getCurrentTemperatures(false, 0);
            int count = temperatures.size();
            if (count == 0) {
                Slog.w(TAG, "Thermal HAL reported invalid data, abort connection");
            }
            for (int i = 0; i < count; i++) {
                onTemperatureChanged(temperatures.get(i), false);
            }
            onTemperatureMapChangedLocked();
            this.mTemperatureWatcher.updateSevereThresholds();
            this.mHalReady.set(true);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void postStatusListener(final IThermalStatusListener listener) {
        boolean thermalCallbackQueued = FgThread.getHandler().post(new Runnable() { // from class: com.android.server.power.ThermalManagerService$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                ThermalManagerService.this.m6199x7b29c12c(listener);
            }
        });
        if (!thermalCallbackQueued) {
            Slog.e(TAG, "Thermal callback failed to queue");
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$postStatusListener$0$com-android-server-power-ThermalManagerService  reason: not valid java name */
    public /* synthetic */ void m6199x7b29c12c(IThermalStatusListener listener) {
        try {
            listener.onStatusChange(this.mStatus);
        } catch (RemoteException | RuntimeException e) {
            Slog.e(TAG, "Thermal callback failed to call", e);
        }
    }

    private void notifyStatusListenersLocked() {
        int length = this.mThermalStatusListeners.beginBroadcast();
        for (int i = 0; i < length; i++) {
            try {
                IThermalStatusListener listener = this.mThermalStatusListeners.getBroadcastItem(i);
                postStatusListener(listener);
            } finally {
                this.mThermalStatusListeners.finishBroadcast();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onTemperatureMapChangedLocked() {
        int newStatus = 0;
        int count = this.mTemperatureMap.size();
        for (int i = 0; i < count; i++) {
            Temperature t = this.mTemperatureMap.valueAt(i);
            if (t.getType() == 3 && t.getStatus() >= newStatus) {
                newStatus = t.getStatus();
            }
        }
        if (!this.mIsStatusOverride) {
            setStatusLocked(newStatus);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setStatusLocked(int newStatus) {
        if (newStatus != this.mStatus) {
            this.mStatus = newStatus;
            notifyStatusListenersLocked();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void postEventListenerCurrentTemperatures(IThermalEventListener listener, Integer type) {
        synchronized (this.mLock) {
            int count = this.mTemperatureMap.size();
            for (int i = 0; i < count; i++) {
                postEventListener(this.mTemperatureMap.valueAt(i), listener, type);
            }
        }
    }

    private void postEventListener(final Temperature temperature, final IThermalEventListener listener, Integer type) {
        if (type != null && type.intValue() != temperature.getType()) {
            return;
        }
        boolean thermalCallbackQueued = FgThread.getHandler().post(new Runnable() { // from class: com.android.server.power.ThermalManagerService$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                ThermalManagerService.lambda$postEventListener$1(listener, temperature);
            }
        });
        if (!thermalCallbackQueued) {
            Slog.e(TAG, "Thermal callback failed to queue");
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$postEventListener$1(IThermalEventListener listener, Temperature temperature) {
        try {
            listener.notifyThrottling(temperature);
        } catch (RemoteException | RuntimeException e) {
            Slog.e(TAG, "Thermal callback failed to call", e);
        }
    }

    private void notifyEventListenersLocked(Temperature temperature) {
        int length = this.mThermalEventListeners.beginBroadcast();
        for (int i = 0; i < length; i++) {
            try {
                IThermalEventListener listener = this.mThermalEventListeners.getBroadcastItem(i);
                Integer type = (Integer) this.mThermalEventListeners.getBroadcastCookie(i);
                postEventListener(temperature, listener, type);
            } catch (Throwable th) {
                this.mThermalEventListeners.finishBroadcast();
                throw th;
            }
        }
        this.mThermalEventListeners.finishBroadcast();
        EventLog.writeEvent((int) EventLogTags.THERMAL_CHANGED, temperature.getName(), Integer.valueOf(temperature.getType()), Float.valueOf(temperature.getValue()), Integer.valueOf(temperature.getStatus()), Integer.valueOf(this.mStatus));
    }

    private void shutdownIfNeeded(Temperature temperature) {
        if (temperature.getStatus() != 6) {
            return;
        }
        PowerManager powerManager = (PowerManager) getContext().getSystemService(PowerManager.class);
        switch (temperature.getType()) {
            case 0:
            case 1:
            case 3:
            case 9:
                powerManager.shutdown(false, "thermal", false);
                return;
            case 2:
                powerManager.shutdown(false, "thermal,battery", false);
                return;
            default:
                return;
        }
    }

    private void onTemperatureChanged(Temperature temperature, boolean sendStatus) {
        shutdownIfNeeded(temperature);
        synchronized (this.mLock) {
            Temperature old = this.mTemperatureMap.put(temperature.getName(), temperature);
            if (old == null || old.getStatus() != temperature.getStatus()) {
                notifyEventListenersLocked(temperature);
            }
            if (sendStatus) {
                onTemperatureMapChangedLocked();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onTemperatureChangedCallback(Temperature temperature) {
        long token = Binder.clearCallingIdentity();
        try {
            onTemperatureChanged(temperature, true);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    /* loaded from: classes2.dex */
    class ThermalShellCommand extends ShellCommand {
        ThermalShellCommand() {
        }

        public int onCommand(String cmd) {
            String str = cmd != null ? cmd : "";
            char c = 65535;
            switch (str.hashCode()) {
                case 108404047:
                    if (str.equals("reset")) {
                        c = 1;
                        break;
                    }
                    break;
                case 385515795:
                    if (str.equals("override-status")) {
                        c = 0;
                        break;
                    }
                    break;
            }
            switch (c) {
                case 0:
                    return runOverrideStatus();
                case 1:
                    return runReset();
                default:
                    return handleDefaultCommands(cmd);
            }
        }

        private int runReset() {
            long token = Binder.clearCallingIdentity();
            try {
                synchronized (ThermalManagerService.this.mLock) {
                    ThermalManagerService.this.mIsStatusOverride = false;
                    ThermalManagerService.this.onTemperatureMapChangedLocked();
                }
                return 0;
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [593=4] */
        private int runOverrideStatus() {
            PrintWriter pw;
            long token = Binder.clearCallingIdentity();
            try {
                pw = getOutPrintWriter();
                int status = Integer.parseInt(getNextArgRequired());
                if (!Temperature.isValidStatus(status)) {
                    pw.println("Invalid status: " + status);
                    return -1;
                }
                synchronized (ThermalManagerService.this.mLock) {
                    ThermalManagerService.this.mIsStatusOverride = true;
                    ThermalManagerService.this.setStatusLocked(status);
                }
                return 0;
            } catch (RuntimeException ex) {
                pw.println("Error: " + ex.toString());
                return -1;
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public void onHelp() {
            PrintWriter pw = getOutPrintWriter();
            pw.println("Thermal service (thermalservice) commands:");
            pw.println("  help");
            pw.println("    Print this help text.");
            pw.println("");
            pw.println("  override-status STATUS");
            pw.println("    sets and locks the thermal status of the device to STATUS.");
            pw.println("    status code is defined in android.os.Temperature.");
            pw.println("  reset");
            pw.println("    unlocks the thermal status of the device.");
            pw.println();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public static abstract class ThermalHalWrapper {
        protected static final String TAG = ThermalHalWrapper.class.getSimpleName();
        protected static final int THERMAL_HAL_DEATH_COOKIE = 5612;
        protected TemperatureChangedCallback mCallback;
        protected final Object mHalLock = new Object();

        /* JADX INFO: Access modifiers changed from: package-private */
        @FunctionalInterface
        /* loaded from: classes2.dex */
        public interface TemperatureChangedCallback {
            void onValues(Temperature temperature);
        }

        protected abstract boolean connectToHal();

        protected abstract void dump(PrintWriter printWriter, String str);

        protected abstract List<CoolingDevice> getCurrentCoolingDevices(boolean z, int i);

        protected abstract List<Temperature> getCurrentTemperatures(boolean z, int i);

        protected abstract List<TemperatureThreshold> getTemperatureThresholds(boolean z, int i);

        ThermalHalWrapper() {
        }

        protected void setCallback(TemperatureChangedCallback cb) {
            this.mCallback = cb;
        }

        protected void resendCurrentTemperatures() {
            synchronized (this.mHalLock) {
                List<Temperature> temperatures = getCurrentTemperatures(false, 0);
                int count = temperatures.size();
                for (int i = 0; i < count; i++) {
                    this.mCallback.onValues(temperatures.get(i));
                }
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* loaded from: classes2.dex */
        public final class DeathRecipient implements IHwBinder.DeathRecipient {
            DeathRecipient() {
            }

            public void serviceDied(long cookie) {
                if (cookie == 5612) {
                    Slog.e(ThermalHalWrapper.TAG, "Thermal HAL service died cookie: " + cookie);
                    synchronized (ThermalHalWrapper.this.mHalLock) {
                        ThermalHalWrapper.this.connectToHal();
                        ThermalHalWrapper.this.resendCurrentTemperatures();
                    }
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public static class ThermalHal10Wrapper extends ThermalHalWrapper {
        private IThermal mThermalHal10 = null;

        ThermalHal10Wrapper() {
        }

        @Override // com.android.server.power.ThermalManagerService.ThermalHalWrapper
        protected List<Temperature> getCurrentTemperatures(final boolean shouldFilter, final int type) {
            synchronized (this.mHalLock) {
                final List<Temperature> ret = new ArrayList<>();
                IThermal iThermal = this.mThermalHal10;
                if (iThermal == null) {
                    return ret;
                }
                try {
                    iThermal.getTemperatures(new IThermal.getTemperaturesCallback() { // from class: com.android.server.power.ThermalManagerService$ThermalHal10Wrapper$$ExternalSyntheticLambda0
                        public final void onValues(ThermalStatus thermalStatus, ArrayList arrayList) {
                            ThermalManagerService.ThermalHal10Wrapper.lambda$getCurrentTemperatures$0(shouldFilter, type, ret, thermalStatus, arrayList);
                        }
                    });
                } catch (RemoteException e) {
                    Slog.e(TAG, "Couldn't getCurrentTemperatures, reconnecting...", e);
                    connectToHal();
                }
                return ret;
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public static /* synthetic */ void lambda$getCurrentTemperatures$0(boolean shouldFilter, int type, List ret, ThermalStatus status, ArrayList temperatures) {
            if (status.code == 0) {
                Iterator it = temperatures.iterator();
                while (it.hasNext()) {
                    android.hardware.thermal.V1_0.Temperature temperature = (android.hardware.thermal.V1_0.Temperature) it.next();
                    if (!shouldFilter || type == temperature.type) {
                        ret.add(new Temperature(temperature.currentValue, temperature.type, temperature.name, 0));
                    }
                }
                return;
            }
            Slog.e(TAG, "Couldn't get temperatures because of HAL error: " + status.debugMessage);
        }

        @Override // com.android.server.power.ThermalManagerService.ThermalHalWrapper
        protected List<CoolingDevice> getCurrentCoolingDevices(final boolean shouldFilter, final int type) {
            synchronized (this.mHalLock) {
                final List<CoolingDevice> ret = new ArrayList<>();
                IThermal iThermal = this.mThermalHal10;
                if (iThermal == null) {
                    return ret;
                }
                try {
                    iThermal.getCoolingDevices(new IThermal.getCoolingDevicesCallback() { // from class: com.android.server.power.ThermalManagerService$ThermalHal10Wrapper$$ExternalSyntheticLambda1
                        public final void onValues(ThermalStatus thermalStatus, ArrayList arrayList) {
                            ThermalManagerService.ThermalHal10Wrapper.lambda$getCurrentCoolingDevices$1(shouldFilter, type, ret, thermalStatus, arrayList);
                        }
                    });
                } catch (RemoteException e) {
                    Slog.e(TAG, "Couldn't getCurrentCoolingDevices, reconnecting...", e);
                    connectToHal();
                }
                return ret;
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public static /* synthetic */ void lambda$getCurrentCoolingDevices$1(boolean shouldFilter, int type, List ret, ThermalStatus status, ArrayList coolingDevices) {
            if (status.code == 0) {
                Iterator it = coolingDevices.iterator();
                while (it.hasNext()) {
                    android.hardware.thermal.V1_0.CoolingDevice coolingDevice = (android.hardware.thermal.V1_0.CoolingDevice) it.next();
                    if (!shouldFilter || type == coolingDevice.type) {
                        ret.add(new CoolingDevice(coolingDevice.currentValue, coolingDevice.type, coolingDevice.name));
                    }
                }
                return;
            }
            Slog.e(TAG, "Couldn't get cooling device because of HAL error: " + status.debugMessage);
        }

        @Override // com.android.server.power.ThermalManagerService.ThermalHalWrapper
        protected List<TemperatureThreshold> getTemperatureThresholds(boolean shouldFilter, int type) {
            return new ArrayList();
        }

        @Override // com.android.server.power.ThermalManagerService.ThermalHalWrapper
        protected boolean connectToHal() {
            boolean z;
            synchronized (this.mHalLock) {
                z = true;
                try {
                    IThermal service = IThermal.getService(true);
                    this.mThermalHal10 = service;
                    service.linkToDeath(new ThermalHalWrapper.DeathRecipient(), 5612L);
                    Slog.i(TAG, "Thermal HAL 1.0 service connected, no thermal call back will be called due to legacy API.");
                } catch (RemoteException | NoSuchElementException e) {
                    Slog.e(TAG, "Thermal HAL 1.0 service not connected.");
                    this.mThermalHal10 = null;
                }
                if (this.mThermalHal10 == null) {
                    z = false;
                }
            }
            return z;
        }

        @Override // com.android.server.power.ThermalManagerService.ThermalHalWrapper
        protected void dump(PrintWriter pw, String prefix) {
            synchronized (this.mHalLock) {
                pw.print(prefix);
                pw.println("ThermalHAL 1.0 connected: " + (this.mThermalHal10 != null ? UiModeManagerService.Shell.NIGHT_MODE_STR_YES : UiModeManagerService.Shell.NIGHT_MODE_STR_NO));
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public static class ThermalHal11Wrapper extends ThermalHalWrapper {
        private android.hardware.thermal.V1_1.IThermal mThermalHal11 = null;
        private final IThermalCallback.Stub mThermalCallback11 = new IThermalCallback.Stub() { // from class: com.android.server.power.ThermalManagerService.ThermalHal11Wrapper.1
            public void notifyThrottling(boolean isThrottling, android.hardware.thermal.V1_0.Temperature temperature) {
                Temperature thermalSvcTemp = new Temperature(temperature.currentValue, temperature.type, temperature.name, isThrottling ? 3 : 0);
                long token = Binder.clearCallingIdentity();
                try {
                    ThermalHal11Wrapper.this.mCallback.onValues(thermalSvcTemp);
                } finally {
                    Binder.restoreCallingIdentity(token);
                }
            }
        };

        ThermalHal11Wrapper() {
        }

        @Override // com.android.server.power.ThermalManagerService.ThermalHalWrapper
        protected List<Temperature> getCurrentTemperatures(final boolean shouldFilter, final int type) {
            synchronized (this.mHalLock) {
                final List<Temperature> ret = new ArrayList<>();
                android.hardware.thermal.V1_1.IThermal iThermal = this.mThermalHal11;
                if (iThermal == null) {
                    return ret;
                }
                try {
                    iThermal.getTemperatures(new IThermal.getTemperaturesCallback() { // from class: com.android.server.power.ThermalManagerService$ThermalHal11Wrapper$$ExternalSyntheticLambda0
                        public final void onValues(ThermalStatus thermalStatus, ArrayList arrayList) {
                            ThermalManagerService.ThermalHal11Wrapper.lambda$getCurrentTemperatures$0(shouldFilter, type, ret, thermalStatus, arrayList);
                        }
                    });
                } catch (RemoteException e) {
                    Slog.e(TAG, "Couldn't getCurrentTemperatures, reconnecting...", e);
                    connectToHal();
                }
                return ret;
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public static /* synthetic */ void lambda$getCurrentTemperatures$0(boolean shouldFilter, int type, List ret, ThermalStatus status, ArrayList temperatures) {
            if (status.code == 0) {
                Iterator it = temperatures.iterator();
                while (it.hasNext()) {
                    android.hardware.thermal.V1_0.Temperature temperature = (android.hardware.thermal.V1_0.Temperature) it.next();
                    if (!shouldFilter || type == temperature.type) {
                        ret.add(new Temperature(temperature.currentValue, temperature.type, temperature.name, 0));
                    }
                }
                return;
            }
            Slog.e(TAG, "Couldn't get temperatures because of HAL error: " + status.debugMessage);
        }

        @Override // com.android.server.power.ThermalManagerService.ThermalHalWrapper
        protected List<CoolingDevice> getCurrentCoolingDevices(final boolean shouldFilter, final int type) {
            synchronized (this.mHalLock) {
                final List<CoolingDevice> ret = new ArrayList<>();
                android.hardware.thermal.V1_1.IThermal iThermal = this.mThermalHal11;
                if (iThermal == null) {
                    return ret;
                }
                try {
                    iThermal.getCoolingDevices(new IThermal.getCoolingDevicesCallback() { // from class: com.android.server.power.ThermalManagerService$ThermalHal11Wrapper$$ExternalSyntheticLambda1
                        public final void onValues(ThermalStatus thermalStatus, ArrayList arrayList) {
                            ThermalManagerService.ThermalHal11Wrapper.lambda$getCurrentCoolingDevices$1(shouldFilter, type, ret, thermalStatus, arrayList);
                        }
                    });
                } catch (RemoteException e) {
                    Slog.e(TAG, "Couldn't getCurrentCoolingDevices, reconnecting...", e);
                    connectToHal();
                }
                return ret;
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public static /* synthetic */ void lambda$getCurrentCoolingDevices$1(boolean shouldFilter, int type, List ret, ThermalStatus status, ArrayList coolingDevices) {
            if (status.code == 0) {
                Iterator it = coolingDevices.iterator();
                while (it.hasNext()) {
                    android.hardware.thermal.V1_0.CoolingDevice coolingDevice = (android.hardware.thermal.V1_0.CoolingDevice) it.next();
                    if (!shouldFilter || type == coolingDevice.type) {
                        ret.add(new CoolingDevice(coolingDevice.currentValue, coolingDevice.type, coolingDevice.name));
                    }
                }
                return;
            }
            Slog.e(TAG, "Couldn't get cooling device because of HAL error: " + status.debugMessage);
        }

        @Override // com.android.server.power.ThermalManagerService.ThermalHalWrapper
        protected List<TemperatureThreshold> getTemperatureThresholds(boolean shouldFilter, int type) {
            return new ArrayList();
        }

        @Override // com.android.server.power.ThermalManagerService.ThermalHalWrapper
        protected boolean connectToHal() {
            boolean z;
            synchronized (this.mHalLock) {
                z = true;
                try {
                    android.hardware.thermal.V1_1.IThermal service = android.hardware.thermal.V1_1.IThermal.getService(true);
                    this.mThermalHal11 = service;
                    service.linkToDeath(new ThermalHalWrapper.DeathRecipient(), 5612L);
                    this.mThermalHal11.registerThermalCallback(this.mThermalCallback11);
                    Slog.i(TAG, "Thermal HAL 1.1 service connected, limited thermal functions due to legacy API.");
                } catch (RemoteException | NoSuchElementException e) {
                    Slog.e(TAG, "Thermal HAL 1.1 service not connected.");
                    this.mThermalHal11 = null;
                }
                if (this.mThermalHal11 == null) {
                    z = false;
                }
            }
            return z;
        }

        @Override // com.android.server.power.ThermalManagerService.ThermalHalWrapper
        protected void dump(PrintWriter pw, String prefix) {
            synchronized (this.mHalLock) {
                pw.print(prefix);
                pw.println("ThermalHAL 1.1 connected: " + (this.mThermalHal11 != null ? UiModeManagerService.Shell.NIGHT_MODE_STR_YES : UiModeManagerService.Shell.NIGHT_MODE_STR_NO));
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public static class ThermalHal20Wrapper extends ThermalHalWrapper {
        private android.hardware.thermal.V2_0.IThermal mThermalHal20 = null;
        private final IThermalChangedCallback.Stub mThermalCallback20 = new IThermalChangedCallback.Stub() { // from class: com.android.server.power.ThermalManagerService.ThermalHal20Wrapper.1
            public void notifyThrottling(android.hardware.thermal.V2_0.Temperature temperature) {
                Temperature thermalSvcTemp = new Temperature(temperature.value, temperature.type, temperature.name, temperature.throttlingStatus);
                long token = Binder.clearCallingIdentity();
                try {
                    ThermalHal20Wrapper.this.mCallback.onValues(thermalSvcTemp);
                } finally {
                    Binder.restoreCallingIdentity(token);
                }
            }
        };

        ThermalHal20Wrapper() {
        }

        @Override // com.android.server.power.ThermalManagerService.ThermalHalWrapper
        protected List<Temperature> getCurrentTemperatures(boolean shouldFilter, int type) {
            synchronized (this.mHalLock) {
                final List<Temperature> ret = new ArrayList<>();
                android.hardware.thermal.V2_0.IThermal iThermal = this.mThermalHal20;
                if (iThermal == null) {
                    return ret;
                }
                try {
                    iThermal.getCurrentTemperatures(shouldFilter, type, new IThermal.getCurrentTemperaturesCallback() { // from class: com.android.server.power.ThermalManagerService$ThermalHal20Wrapper$$ExternalSyntheticLambda1
                        public final void onValues(ThermalStatus thermalStatus, ArrayList arrayList) {
                            ThermalManagerService.ThermalHal20Wrapper.lambda$getCurrentTemperatures$0(ret, thermalStatus, arrayList);
                        }
                    });
                } catch (RemoteException e) {
                    Slog.e(TAG, "Couldn't getCurrentTemperatures, reconnecting...", e);
                    connectToHal();
                }
                return ret;
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public static /* synthetic */ void lambda$getCurrentTemperatures$0(List ret, ThermalStatus status, ArrayList temperatures) {
            if (status.code == 0) {
                Iterator it = temperatures.iterator();
                while (it.hasNext()) {
                    android.hardware.thermal.V2_0.Temperature temperature = (android.hardware.thermal.V2_0.Temperature) it.next();
                    if (!Temperature.isValidStatus(temperature.throttlingStatus)) {
                        Slog.e(TAG, "Invalid status data from HAL");
                        temperature.throttlingStatus = 0;
                    }
                    ret.add(new Temperature(temperature.value, temperature.type, temperature.name, temperature.throttlingStatus));
                }
                return;
            }
            Slog.e(TAG, "Couldn't get temperatures because of HAL error: " + status.debugMessage);
        }

        @Override // com.android.server.power.ThermalManagerService.ThermalHalWrapper
        protected List<CoolingDevice> getCurrentCoolingDevices(boolean shouldFilter, int type) {
            synchronized (this.mHalLock) {
                final List<CoolingDevice> ret = new ArrayList<>();
                android.hardware.thermal.V2_0.IThermal iThermal = this.mThermalHal20;
                if (iThermal == null) {
                    return ret;
                }
                try {
                    iThermal.getCurrentCoolingDevices(shouldFilter, type, new IThermal.getCurrentCoolingDevicesCallback() { // from class: com.android.server.power.ThermalManagerService$ThermalHal20Wrapper$$ExternalSyntheticLambda0
                        public final void onValues(ThermalStatus thermalStatus, ArrayList arrayList) {
                            ThermalManagerService.ThermalHal20Wrapper.lambda$getCurrentCoolingDevices$1(ret, thermalStatus, arrayList);
                        }
                    });
                } catch (RemoteException e) {
                    Slog.e(TAG, "Couldn't getCurrentCoolingDevices, reconnecting...", e);
                    connectToHal();
                }
                return ret;
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public static /* synthetic */ void lambda$getCurrentCoolingDevices$1(List ret, ThermalStatus status, ArrayList coolingDevices) {
            if (status.code == 0) {
                Iterator it = coolingDevices.iterator();
                while (it.hasNext()) {
                    android.hardware.thermal.V2_0.CoolingDevice coolingDevice = (android.hardware.thermal.V2_0.CoolingDevice) it.next();
                    ret.add(new CoolingDevice(coolingDevice.value, coolingDevice.type, coolingDevice.name));
                }
                return;
            }
            Slog.e(TAG, "Couldn't get cooling device because of HAL error: " + status.debugMessage);
        }

        @Override // com.android.server.power.ThermalManagerService.ThermalHalWrapper
        protected List<TemperatureThreshold> getTemperatureThresholds(boolean shouldFilter, int type) {
            synchronized (this.mHalLock) {
                final List<TemperatureThreshold> ret = new ArrayList<>();
                android.hardware.thermal.V2_0.IThermal iThermal = this.mThermalHal20;
                if (iThermal == null) {
                    return ret;
                }
                try {
                    iThermal.getTemperatureThresholds(shouldFilter, type, new IThermal.getTemperatureThresholdsCallback() { // from class: com.android.server.power.ThermalManagerService$ThermalHal20Wrapper$$ExternalSyntheticLambda2
                        public final void onValues(ThermalStatus thermalStatus, ArrayList arrayList) {
                            ThermalManagerService.ThermalHal20Wrapper.lambda$getTemperatureThresholds$2(ret, thermalStatus, arrayList);
                        }
                    });
                } catch (RemoteException e) {
                    Slog.e(TAG, "Couldn't getTemperatureThresholds, reconnecting...", e);
                }
                return ret;
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public static /* synthetic */ void lambda$getTemperatureThresholds$2(List ret, ThermalStatus status, ArrayList thresholds) {
            if (status.code == 0) {
                ret.addAll(thresholds);
            } else {
                Slog.e(TAG, "Couldn't get temperature thresholds because of HAL error: " + status.debugMessage);
            }
        }

        @Override // com.android.server.power.ThermalManagerService.ThermalHalWrapper
        protected boolean connectToHal() {
            boolean z;
            synchronized (this.mHalLock) {
                z = true;
                try {
                    android.hardware.thermal.V2_0.IThermal service = android.hardware.thermal.V2_0.IThermal.getService(true);
                    this.mThermalHal20 = service;
                    service.linkToDeath(new ThermalHalWrapper.DeathRecipient(), 5612L);
                    this.mThermalHal20.registerThermalChangedCallback(this.mThermalCallback20, false, 0);
                    Slog.i(TAG, "Thermal HAL 2.0 service connected.");
                } catch (RemoteException | NoSuchElementException e) {
                    Slog.e(TAG, "Thermal HAL 2.0 service not connected.");
                    this.mThermalHal20 = null;
                }
                if (this.mThermalHal20 == null) {
                    z = false;
                }
            }
            return z;
        }

        @Override // com.android.server.power.ThermalManagerService.ThermalHalWrapper
        protected void dump(PrintWriter pw, String prefix) {
            synchronized (this.mHalLock) {
                pw.print(prefix);
                pw.println("ThermalHAL 2.0 connected: " + (this.mThermalHal20 != null ? UiModeManagerService.Shell.NIGHT_MODE_STR_YES : UiModeManagerService.Shell.NIGHT_MODE_STR_NO));
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public class TemperatureWatcher {
        private static final float DEGREES_BETWEEN_ZERO_AND_ONE = 30.0f;
        private static final int INACTIVITY_THRESHOLD_MILLIS = 10000;
        private static final int MINIMUM_SAMPLE_COUNT = 3;
        private static final int RING_BUFFER_SIZE = 30;
        private final Handler mHandler = BackgroundThread.getHandler();
        final ArrayMap<String, ArrayList<Sample>> mSamples = new ArrayMap<>();
        ArrayMap<String, Float> mSevereThresholds = new ArrayMap<>();
        private long mLastForecastCallTimeMillis = 0;
        long mInactivityThresholdMillis = JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY;

        TemperatureWatcher() {
        }

        void updateSevereThresholds() {
            synchronized (this.mSamples) {
                List<TemperatureThreshold> thresholds = ThermalManagerService.this.mHalWrapper.getTemperatureThresholds(true, 3);
                for (int t = 0; t < thresholds.size(); t++) {
                    TemperatureThreshold threshold = thresholds.get(t);
                    if (threshold.hotThrottlingThresholds.length > 3) {
                        float temperature = threshold.hotThrottlingThresholds[3];
                        if (!Float.isNaN(temperature)) {
                            this.mSevereThresholds.put(threshold.name, Float.valueOf(threshold.hotThrottlingThresholds[3]));
                        }
                    }
                }
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void updateTemperature() {
            synchronized (this.mSamples) {
                if (SystemClock.elapsedRealtime() - this.mLastForecastCallTimeMillis < this.mInactivityThresholdMillis) {
                    this.mHandler.postDelayed(new Runnable() { // from class: com.android.server.power.ThermalManagerService$TemperatureWatcher$$ExternalSyntheticLambda0
                        @Override // java.lang.Runnable
                        public final void run() {
                            ThermalManagerService.TemperatureWatcher.this.updateTemperature();
                        }
                    }, 1000L);
                    long now = SystemClock.elapsedRealtime();
                    List<Temperature> temperatures = ThermalManagerService.this.mHalWrapper.getCurrentTemperatures(true, 3);
                    for (int t = 0; t < temperatures.size(); t++) {
                        Temperature temperature = temperatures.get(t);
                        if (!Float.isNaN(temperature.getValue())) {
                            ArrayList<Sample> samples = this.mSamples.computeIfAbsent(temperature.getName(), new Function() { // from class: com.android.server.power.ThermalManagerService$TemperatureWatcher$$ExternalSyntheticLambda1
                                @Override // java.util.function.Function
                                public final Object apply(Object obj) {
                                    return ThermalManagerService.TemperatureWatcher.lambda$updateTemperature$0((String) obj);
                                }
                            });
                            if (samples.size() == 30) {
                                samples.remove(0);
                            }
                            samples.add(new Sample(now, temperature.getValue()));
                        }
                    }
                    return;
                }
                this.mSamples.clear();
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public static /* synthetic */ ArrayList lambda$updateTemperature$0(String k) {
            return new ArrayList(30);
        }

        float getSlopeOf(List<Sample> samples) {
            long sumTimes = 0;
            float sumTemperatures = 0.0f;
            for (int s = 0; s < samples.size(); s++) {
                Sample sample = samples.get(s);
                sumTimes += sample.time;
                sumTemperatures += sample.temperature;
            }
            int s2 = samples.size();
            long meanTime = sumTimes / s2;
            float meanTemperature = sumTemperatures / samples.size();
            long sampleVariance = 0;
            float sampleCovariance = 0.0f;
            for (int s3 = 0; s3 < samples.size(); s3++) {
                Sample sample2 = samples.get(s3);
                long timeDelta = sample2.time - meanTime;
                float temperatureDelta = sample2.temperature - meanTemperature;
                sampleVariance += timeDelta * timeDelta;
                sampleCovariance += ((float) timeDelta) * temperatureDelta;
            }
            return sampleCovariance / ((float) sampleVariance);
        }

        float normalizeTemperature(float temperature, float severeThreshold) {
            synchronized (this.mSamples) {
                float zeroNormalized = severeThreshold - DEGREES_BETWEEN_ZERO_AND_ONE;
                if (temperature <= zeroNormalized) {
                    return 0.0f;
                }
                float delta = temperature - zeroNormalized;
                return delta / DEGREES_BETWEEN_ZERO_AND_ONE;
            }
        }

        float getForecast(int forecastSeconds) {
            synchronized (this.mSamples) {
                this.mLastForecastCallTimeMillis = SystemClock.elapsedRealtime();
                if (this.mSamples.isEmpty()) {
                    updateTemperature();
                }
                if (this.mSamples.isEmpty()) {
                    Slog.e(ThermalManagerService.TAG, "No temperature samples found");
                    return Float.NaN;
                } else if (this.mSevereThresholds.isEmpty()) {
                    Slog.e(ThermalManagerService.TAG, "No temperature thresholds found");
                    return Float.NaN;
                } else {
                    float maxNormalized = Float.NaN;
                    for (Map.Entry<String, ArrayList<Sample>> entry : this.mSamples.entrySet()) {
                        String name = entry.getKey();
                        ArrayList<Sample> samples = entry.getValue();
                        Float threshold = this.mSevereThresholds.get(name);
                        if (threshold == null) {
                            Slog.e(ThermalManagerService.TAG, "No threshold found for " + name);
                        } else {
                            float currentTemperature = samples.get(0).temperature;
                            if (samples.size() < 3) {
                                float normalized = normalizeTemperature(currentTemperature, threshold.floatValue());
                                if (Float.isNaN(maxNormalized) || normalized > maxNormalized) {
                                    maxNormalized = normalized;
                                }
                            } else {
                                float slope = getSlopeOf(samples);
                                float normalized2 = normalizeTemperature((forecastSeconds * slope * 1000.0f) + currentTemperature, threshold.floatValue());
                                if (Float.isNaN(maxNormalized) || normalized2 > maxNormalized) {
                                    maxNormalized = normalized2;
                                }
                            }
                        }
                    }
                    return maxNormalized;
                }
            }
        }

        Sample createSampleForTesting(long time, float temperature) {
            return new Sample(time, temperature);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* loaded from: classes2.dex */
        public class Sample {
            public float temperature;
            public long time;

            Sample(long time, float temperature) {
                this.time = time;
                this.temperature = temperature;
            }
        }
    }
}
