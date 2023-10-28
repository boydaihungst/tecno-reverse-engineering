package com.android.server;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.UEventObserver;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Pair;
import android.util.Slog;
import com.android.internal.util.DumpUtils;
import com.android.server.ExtconUEventObserver;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public final class DockObserver extends SystemService {
    private static final int MSG_DOCK_STATE_CHANGED = 0;
    private static final String TAG = "DockObserver";
    private int mActualDockState;
    private final boolean mAllowTheaterModeWakeFromDock;
    private final List<ExtconStateConfig> mExtconStateConfigs;
    private final ExtconUEventObserver mExtconUEventObserver;
    private final Handler mHandler;
    private final Object mLock;
    private final PowerManager mPowerManager;
    private int mPreviousDockState;
    private int mReportedDockState;
    private boolean mSystemReady;
    private boolean mUpdatesStopped;
    private final PowerManager.WakeLock mWakeLock;

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static final class ExtconStateProvider {
        private final Map<String, String> mState;

        ExtconStateProvider(Map<String, String> state) {
            this.mState = state;
        }

        String getValue(String key) {
            return this.mState.get(key);
        }

        static ExtconStateProvider fromString(String stateString) {
            Map<String, String> states = new HashMap<>();
            String[] lines = stateString.split("\n");
            for (String line : lines) {
                String[] fields = line.split("=");
                if (fields.length == 2) {
                    states.put(fields[0], fields[1]);
                } else {
                    Slog.e(DockObserver.TAG, "Invalid line: " + line);
                }
            }
            return new ExtconStateProvider(states);
        }

        static ExtconStateProvider fromFile(String stateFilePath) {
            char[] buffer = new char[1024];
            try {
                FileReader file = new FileReader(stateFilePath);
                int len = file.read(buffer, 0, 1024);
                String stateString = new String(buffer, 0, len).trim();
                ExtconStateProvider fromString = fromString(stateString);
                file.close();
                return fromString;
            } catch (FileNotFoundException e) {
                Slog.w(DockObserver.TAG, "No state file found at: " + stateFilePath);
                return new ExtconStateProvider(new HashMap());
            } catch (Exception e2) {
                Slog.e(DockObserver.TAG, "", e2);
                return new ExtconStateProvider(new HashMap());
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static final class ExtconStateConfig {
        public final int extraStateValue;
        public final List<Pair<String, String>> keyValuePairs = new ArrayList();

        ExtconStateConfig(int extraStateValue) {
            this.extraStateValue = extraStateValue;
        }
    }

    private static List<ExtconStateConfig> loadExtconStateConfigs(Context context) {
        String[] rows = context.getResources().getStringArray(17236054);
        try {
            ArrayList<ExtconStateConfig> configs = new ArrayList<>();
            for (String row : rows) {
                String[] rowFields = row.split(",");
                ExtconStateConfig config = new ExtconStateConfig(Integer.parseInt(rowFields[0]));
                for (int i = 1; i < rowFields.length; i++) {
                    String[] keyValueFields = rowFields[i].split("=");
                    if (keyValueFields.length != 2) {
                        throw new IllegalArgumentException("Invalid key-value: " + rowFields[i]);
                    }
                    config.keyValuePairs.add(Pair.create(keyValueFields[0], keyValueFields[1]));
                }
                configs.add(config);
            }
            return configs;
        } catch (ArrayIndexOutOfBoundsException | IllegalArgumentException e) {
            Slog.e(TAG, "Could not parse extcon state config", e);
            return new ArrayList();
        }
    }

    public DockObserver(Context context) {
        super(context);
        this.mLock = new Object();
        this.mActualDockState = 0;
        this.mReportedDockState = 0;
        this.mPreviousDockState = 0;
        this.mHandler = new Handler(true) { // from class: com.android.server.DockObserver.1
            @Override // android.os.Handler
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 0:
                        DockObserver.this.handleDockStateChange();
                        DockObserver.this.mWakeLock.release();
                        return;
                    default:
                        return;
                }
            }
        };
        ExtconUEventObserver extconUEventObserver = new ExtconUEventObserver() { // from class: com.android.server.DockObserver.2
            @Override // com.android.server.ExtconUEventObserver
            public void onUEvent(ExtconUEventObserver.ExtconInfo extconInfo, UEventObserver.UEvent event) {
                synchronized (DockObserver.this.mLock) {
                    String stateString = event.get("STATE");
                    if (stateString != null) {
                        DockObserver.this.setDockStateFromProviderLocked(ExtconStateProvider.fromString(stateString));
                    } else {
                        Slog.e(DockObserver.TAG, "Extcon event missing STATE: " + event);
                    }
                }
            }
        };
        this.mExtconUEventObserver = extconUEventObserver;
        PowerManager powerManager = (PowerManager) context.getSystemService("power");
        this.mPowerManager = powerManager;
        this.mWakeLock = powerManager.newWakeLock(1, TAG);
        this.mAllowTheaterModeWakeFromDock = context.getResources().getBoolean(17891354);
        this.mExtconStateConfigs = loadExtconStateConfigs(context);
        List<ExtconUEventObserver.ExtconInfo> infos = ExtconUEventObserver.ExtconInfo.getExtconInfoForTypes(new String[]{ExtconUEventObserver.ExtconInfo.EXTCON_DOCK});
        if (infos.isEmpty()) {
            Slog.i(TAG, "No extcon dock device found in this kernel.");
            return;
        }
        ExtconUEventObserver.ExtconInfo info = infos.get(0);
        Slog.i(TAG, "Found extcon info devPath: " + info.getDevicePath() + ", statePath: " + info.getStatePath());
        setDockStateFromProviderLocked(ExtconStateProvider.fromFile(info.getStatePath()));
        this.mPreviousDockState = this.mActualDockState;
        extconUEventObserver.startObserving(info);
    }

    @Override // com.android.server.SystemService
    public void onStart() {
        publishBinderService(TAG, new BinderService());
    }

    @Override // com.android.server.SystemService
    public void onBootPhase(int phase) {
        if (phase == 550) {
            synchronized (this.mLock) {
                this.mSystemReady = true;
                if (this.mReportedDockState != 0) {
                    updateLocked();
                }
            }
        }
    }

    private void setActualDockStateLocked(int newState) {
        this.mActualDockState = newState;
        if (!this.mUpdatesStopped) {
            setDockStateLocked(newState);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setDockStateLocked(int newState) {
        if (newState != this.mReportedDockState) {
            this.mReportedDockState = newState;
            if (this.mSystemReady) {
                if (this.mAllowTheaterModeWakeFromDock || Settings.Global.getInt(getContext().getContentResolver(), "theater_mode_on", 0) == 0) {
                    this.mPowerManager.wakeUp(SystemClock.uptimeMillis(), "android.server:DOCK");
                }
                updateLocked();
            }
        }
    }

    private void updateLocked() {
        this.mWakeLock.acquire();
        this.mHandler.sendEmptyMessage(0);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleDockStateChange() {
        String soundPath;
        Uri soundUri;
        Ringtone sfx;
        synchronized (this.mLock) {
            Slog.i(TAG, "Dock state changed from " + this.mPreviousDockState + " to " + this.mReportedDockState);
            int previousDockState = this.mPreviousDockState;
            this.mPreviousDockState = this.mReportedDockState;
            ContentResolver cr = getContext().getContentResolver();
            if (Settings.Global.getInt(cr, "device_provisioned", 0) == 0) {
                Slog.i(TAG, "Device not provisioned, skipping dock broadcast");
                return;
            }
            Intent intent = new Intent("android.intent.action.DOCK_EVENT");
            intent.addFlags(536870912);
            intent.putExtra("android.intent.extra.DOCK_STATE", this.mReportedDockState);
            boolean dockSoundsEnabled = Settings.Global.getInt(cr, "dock_sounds_enabled", 1) == 1;
            boolean dockSoundsEnabledWhenAccessibility = Settings.Global.getInt(cr, "dock_sounds_enabled_when_accessbility", 1) == 1;
            boolean accessibilityEnabled = Settings.Secure.getInt(cr, "accessibility_enabled", 0) == 1;
            if (dockSoundsEnabled || (accessibilityEnabled && dockSoundsEnabledWhenAccessibility)) {
                String whichSound = null;
                int i = this.mReportedDockState;
                if (i == 0) {
                    if (previousDockState != 1 && previousDockState != 3 && previousDockState != 4) {
                        if (previousDockState == 2) {
                            whichSound = "car_undock_sound";
                        }
                    }
                    whichSound = "desk_undock_sound";
                } else {
                    if (i != 1 && i != 3 && i != 4) {
                        if (i == 2) {
                            whichSound = "car_dock_sound";
                        }
                    }
                    whichSound = "desk_dock_sound";
                }
                if (whichSound != null && (soundPath = Settings.Global.getString(cr, whichSound)) != null && (soundUri = Uri.parse("file://" + soundPath)) != null && (sfx = RingtoneManager.getRingtone(getContext(), soundUri)) != null) {
                    sfx.setStreamType(1);
                    sfx.play();
                }
            }
            getContext().sendStickyBroadcastAsUser(intent, UserHandle.ALL);
        }
    }

    /* JADX WARN: Code restructure failed: missing block: B:26:0x0042, code lost:
        continue;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private int getDockedStateExtraValue(ExtconStateProvider state) {
        for (ExtconStateConfig config : this.mExtconStateConfigs) {
            boolean match = true;
            for (Pair<String, String> keyValue : config.keyValuePairs) {
                String stateValue = state.getValue((String) keyValue.first);
                match = match && ((String) keyValue.second).equals(stateValue);
                if (!match) {
                    break;
                }
            }
            if (match) {
                return config.extraStateValue;
            }
        }
        return 1;
    }

    void setDockStateFromProviderForTesting(ExtconStateProvider provider) {
        synchronized (this.mLock) {
            setDockStateFromProviderLocked(provider);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setDockStateFromProviderLocked(ExtconStateProvider provider) {
        int state = 0;
        if ("1".equals(provider.getValue(ExtconUEventObserver.ExtconInfo.EXTCON_DOCK))) {
            state = getDockedStateExtraValue(provider);
        }
        setActualDockStateLocked(state);
    }

    /* loaded from: classes.dex */
    private final class BinderService extends Binder {
        private BinderService() {
        }

        @Override // android.os.Binder
        protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            if (DumpUtils.checkDumpPermission(DockObserver.this.getContext(), DockObserver.TAG, pw)) {
                long ident = Binder.clearCallingIdentity();
                try {
                    synchronized (DockObserver.this.mLock) {
                        if (args != null && args.length != 0 && !"-a".equals(args[0])) {
                            if (args.length == 3 && "set".equals(args[0])) {
                                String key = args[1];
                                String value = args[2];
                                try {
                                    if ("state".equals(key)) {
                                        DockObserver.this.mUpdatesStopped = true;
                                        DockObserver.this.setDockStateLocked(Integer.parseInt(value));
                                    } else {
                                        pw.println("Unknown set option: " + key);
                                    }
                                } catch (NumberFormatException e) {
                                    pw.println("Bad value: " + value);
                                }
                            } else if (args.length == 1 && "reset".equals(args[0])) {
                                DockObserver.this.mUpdatesStopped = false;
                                DockObserver dockObserver = DockObserver.this;
                                dockObserver.setDockStateLocked(dockObserver.mActualDockState);
                            } else {
                                pw.println("Dump current dock state, or:");
                                pw.println("  set state <value>");
                                pw.println("  reset");
                            }
                        }
                        pw.println("Current Dock Observer Service state:");
                        if (DockObserver.this.mUpdatesStopped) {
                            pw.println("  (UPDATES STOPPED -- use 'reset' to restart)");
                        }
                        pw.println("  reported state: " + DockObserver.this.mReportedDockState);
                        pw.println("  previous state: " + DockObserver.this.mPreviousDockState);
                        pw.println("  actual state: " + DockObserver.this.mActualDockState);
                    }
                } finally {
                    Binder.restoreCallingIdentity(ident);
                }
            }
        }
    }
}
