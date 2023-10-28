package com.android.server.display;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.display.WifiDisplay;
import android.hardware.display.WifiDisplaySessionInfo;
import android.hardware.display.WifiDisplayStatus;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Parcelable;
import android.os.UserHandle;
import android.view.Display;
import android.view.DisplayAddress;
import android.view.Surface;
import android.view.SurfaceControl;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.display.DisplayAdapter;
import com.android.server.display.DisplayManagerService;
import com.android.server.display.WifiDisplayController;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public final class WifiDisplayAdapter extends DisplayAdapter {
    private static final String ACTION_DISCONNECT = "android.server.display.wfd.DISCONNECT";
    private static final boolean DEBUG = false;
    private static final String DISPLAY_NAME_PREFIX = "wifi:";
    private static final int MSG_SEND_STATUS_CHANGE_BROADCAST = 1;
    private static final String TAG = "WifiDisplayAdapter";
    private WifiDisplay mActiveDisplay;
    private int mActiveDisplayState;
    private WifiDisplay[] mAvailableDisplays;
    private final BroadcastReceiver mBroadcastReceiver;
    private WifiDisplayStatus mCurrentStatus;
    private WifiDisplayController mDisplayController;
    private WifiDisplayDevice mDisplayDevice;
    private WifiDisplay[] mDisplays;
    private int mFeatureState;
    private final WifiDisplayHandler mHandler;
    private boolean mPendingStatusChangeBroadcast;
    private final PersistentDataStore mPersistentDataStore;
    private WifiDisplay[] mRememberedDisplays;
    private int mScanState;
    private WifiDisplaySessionInfo mSessionInfo;
    private final boolean mSupportsProtectedBuffers;
    private final WifiDisplayController.Listener mWifiDisplayListener;

    public WifiDisplayAdapter(DisplayManagerService.SyncRoot syncRoot, Context context, Handler handler, DisplayAdapter.Listener listener, PersistentDataStore persistentDataStore) {
        super(syncRoot, context, handler, listener, TAG);
        this.mDisplays = WifiDisplay.EMPTY_ARRAY;
        this.mAvailableDisplays = WifiDisplay.EMPTY_ARRAY;
        this.mRememberedDisplays = WifiDisplay.EMPTY_ARRAY;
        this.mBroadcastReceiver = new BroadcastReceiver() { // from class: com.android.server.display.WifiDisplayAdapter.8
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent) {
                if (intent.getAction().equals(WifiDisplayAdapter.ACTION_DISCONNECT)) {
                    synchronized (WifiDisplayAdapter.this.getSyncRoot()) {
                        WifiDisplayAdapter.this.requestDisconnectLocked();
                    }
                }
            }
        };
        this.mWifiDisplayListener = new WifiDisplayController.Listener() { // from class: com.android.server.display.WifiDisplayAdapter.9
            @Override // com.android.server.display.WifiDisplayController.Listener
            public void onFeatureStateChanged(int featureState) {
                synchronized (WifiDisplayAdapter.this.getSyncRoot()) {
                    if (WifiDisplayAdapter.this.mFeatureState != featureState) {
                        WifiDisplayAdapter.this.mFeatureState = featureState;
                        WifiDisplayAdapter.this.scheduleStatusChangedBroadcastLocked();
                    }
                }
            }

            @Override // com.android.server.display.WifiDisplayController.Listener
            public void onScanStarted() {
                synchronized (WifiDisplayAdapter.this.getSyncRoot()) {
                    if (WifiDisplayAdapter.this.mScanState != 1) {
                        WifiDisplayAdapter.this.mScanState = 1;
                        WifiDisplayAdapter.this.scheduleStatusChangedBroadcastLocked();
                    }
                }
            }

            @Override // com.android.server.display.WifiDisplayController.Listener
            public void onScanResults(WifiDisplay[] availableDisplays) {
                synchronized (WifiDisplayAdapter.this.getSyncRoot()) {
                    WifiDisplay[] availableDisplays2 = WifiDisplayAdapter.this.mPersistentDataStore.applyWifiDisplayAliases(availableDisplays);
                    boolean changed = !Arrays.equals(WifiDisplayAdapter.this.mAvailableDisplays, availableDisplays2);
                    for (int i = 0; !changed && i < availableDisplays2.length; i++) {
                        changed = availableDisplays2[i].canConnect() != WifiDisplayAdapter.this.mAvailableDisplays[i].canConnect();
                    }
                    if (changed) {
                        WifiDisplayAdapter.this.mAvailableDisplays = availableDisplays2;
                        WifiDisplayAdapter.this.fixRememberedDisplayNamesFromAvailableDisplaysLocked();
                        WifiDisplayAdapter.this.updateDisplaysLocked();
                        WifiDisplayAdapter.this.scheduleStatusChangedBroadcastLocked();
                    }
                }
            }

            @Override // com.android.server.display.WifiDisplayController.Listener
            public void onScanFinished() {
                synchronized (WifiDisplayAdapter.this.getSyncRoot()) {
                    if (WifiDisplayAdapter.this.mScanState != 0) {
                        WifiDisplayAdapter.this.mScanState = 0;
                        WifiDisplayAdapter.this.scheduleStatusChangedBroadcastLocked();
                    }
                }
            }

            @Override // com.android.server.display.WifiDisplayController.Listener
            public void onDisplayConnecting(WifiDisplay display) {
                synchronized (WifiDisplayAdapter.this.getSyncRoot()) {
                    WifiDisplay display2 = WifiDisplayAdapter.this.mPersistentDataStore.applyWifiDisplayAlias(display);
                    if (WifiDisplayAdapter.this.mActiveDisplayState != 1 || WifiDisplayAdapter.this.mActiveDisplay == null || !WifiDisplayAdapter.this.mActiveDisplay.equals(display2)) {
                        WifiDisplayAdapter.this.mActiveDisplayState = 1;
                        WifiDisplayAdapter.this.mActiveDisplay = display2;
                        WifiDisplayAdapter.this.scheduleStatusChangedBroadcastLocked();
                    }
                }
            }

            @Override // com.android.server.display.WifiDisplayController.Listener
            public void onDisplayConnectionFailed() {
                synchronized (WifiDisplayAdapter.this.getSyncRoot()) {
                    if (WifiDisplayAdapter.this.mActiveDisplayState != 0 || WifiDisplayAdapter.this.mActiveDisplay != null) {
                        WifiDisplayAdapter.this.mActiveDisplayState = 0;
                        WifiDisplayAdapter.this.mActiveDisplay = null;
                        WifiDisplayAdapter.this.scheduleStatusChangedBroadcastLocked();
                    }
                }
            }

            @Override // com.android.server.display.WifiDisplayController.Listener
            public void onDisplayConnected(WifiDisplay display, Surface surface, int width, int height, int flags) {
                synchronized (WifiDisplayAdapter.this.getSyncRoot()) {
                    WifiDisplay display2 = WifiDisplayAdapter.this.mPersistentDataStore.applyWifiDisplayAlias(display);
                    WifiDisplayAdapter.this.addDisplayDeviceLocked(display2, surface, width, height, flags);
                    if (WifiDisplayAdapter.this.mActiveDisplayState != 2 || WifiDisplayAdapter.this.mActiveDisplay == null || !WifiDisplayAdapter.this.mActiveDisplay.equals(display2)) {
                        WifiDisplayAdapter.this.mActiveDisplayState = 2;
                        WifiDisplayAdapter.this.mActiveDisplay = display2;
                        WifiDisplayAdapter.this.scheduleStatusChangedBroadcastLocked();
                    }
                }
            }

            @Override // com.android.server.display.WifiDisplayController.Listener
            public void onDisplaySessionInfo(WifiDisplaySessionInfo sessionInfo) {
                synchronized (WifiDisplayAdapter.this.getSyncRoot()) {
                    WifiDisplayAdapter.this.mSessionInfo = sessionInfo;
                    WifiDisplayAdapter.this.scheduleStatusChangedBroadcastLocked();
                }
            }

            @Override // com.android.server.display.WifiDisplayController.Listener
            public void onDisplayChanged(WifiDisplay display) {
                synchronized (WifiDisplayAdapter.this.getSyncRoot()) {
                    WifiDisplay display2 = WifiDisplayAdapter.this.mPersistentDataStore.applyWifiDisplayAlias(display);
                    if (WifiDisplayAdapter.this.mActiveDisplay != null && WifiDisplayAdapter.this.mActiveDisplay.hasSameAddress(display2) && !WifiDisplayAdapter.this.mActiveDisplay.equals(display2)) {
                        WifiDisplayAdapter.this.mActiveDisplay = display2;
                        WifiDisplayAdapter.this.renameDisplayDeviceLocked(display2.getFriendlyDisplayName());
                        WifiDisplayAdapter.this.scheduleStatusChangedBroadcastLocked();
                    }
                }
            }

            @Override // com.android.server.display.WifiDisplayController.Listener
            public void onDisplayDisconnected() {
                synchronized (WifiDisplayAdapter.this.getSyncRoot()) {
                    WifiDisplayAdapter.this.removeDisplayDeviceLocked();
                    if (WifiDisplayAdapter.this.mActiveDisplayState != 0 || WifiDisplayAdapter.this.mActiveDisplay != null) {
                        WifiDisplayAdapter.this.mActiveDisplayState = 0;
                        WifiDisplayAdapter.this.mActiveDisplay = null;
                        WifiDisplayAdapter.this.scheduleStatusChangedBroadcastLocked();
                    }
                }
            }
        };
        if (!context.getPackageManager().hasSystemFeature("android.hardware.wifi.direct")) {
            throw new RuntimeException("WiFi display was requested, but there is no WiFi Direct feature");
        }
        this.mHandler = new WifiDisplayHandler(handler.getLooper());
        this.mPersistentDataStore = persistentDataStore;
        this.mSupportsProtectedBuffers = context.getResources().getBoolean(17891833);
    }

    @Override // com.android.server.display.DisplayAdapter
    public void dumpLocked(PrintWriter pw) {
        super.dumpLocked(pw);
        pw.println("mCurrentStatus=" + getWifiDisplayStatusLocked());
        pw.println("mFeatureState=" + this.mFeatureState);
        pw.println("mScanState=" + this.mScanState);
        pw.println("mActiveDisplayState=" + this.mActiveDisplayState);
        pw.println("mActiveDisplay=" + this.mActiveDisplay);
        pw.println("mDisplays=" + Arrays.toString(this.mDisplays));
        pw.println("mAvailableDisplays=" + Arrays.toString(this.mAvailableDisplays));
        pw.println("mRememberedDisplays=" + Arrays.toString(this.mRememberedDisplays));
        pw.println("mPendingStatusChangeBroadcast=" + this.mPendingStatusChangeBroadcast);
        pw.println("mSupportsProtectedBuffers=" + this.mSupportsProtectedBuffers);
        if (this.mDisplayController == null) {
            pw.println("mDisplayController=null");
            return;
        }
        pw.println("mDisplayController:");
        IndentingPrintWriter ipw = new IndentingPrintWriter(pw, "  ");
        ipw.increaseIndent();
        DumpUtils.dumpAsync(getHandler(), this.mDisplayController, ipw, "", 200L);
    }

    @Override // com.android.server.display.DisplayAdapter
    public void registerLocked() {
        super.registerLocked();
        updateRememberedDisplaysLocked();
        getHandler().post(new Runnable() { // from class: com.android.server.display.WifiDisplayAdapter.1
            @Override // java.lang.Runnable
            public void run() {
                WifiDisplayAdapter wifiDisplayAdapter = WifiDisplayAdapter.this;
                wifiDisplayAdapter.mDisplayController = new WifiDisplayController(wifiDisplayAdapter.getContext(), WifiDisplayAdapter.this.getHandler(), WifiDisplayAdapter.this.mWifiDisplayListener);
                WifiDisplayAdapter.this.getContext().registerReceiverAsUser(WifiDisplayAdapter.this.mBroadcastReceiver, UserHandle.ALL, new IntentFilter(WifiDisplayAdapter.ACTION_DISCONNECT), null, WifiDisplayAdapter.this.mHandler, 4);
            }
        });
    }

    public void requestStartScanLocked() {
        getHandler().post(new Runnable() { // from class: com.android.server.display.WifiDisplayAdapter.2
            @Override // java.lang.Runnable
            public void run() {
                if (WifiDisplayAdapter.this.mDisplayController != null) {
                    WifiDisplayAdapter.this.mDisplayController.requestStartScan();
                }
            }
        });
    }

    public void requestStopScanLocked() {
        getHandler().post(new Runnable() { // from class: com.android.server.display.WifiDisplayAdapter.3
            @Override // java.lang.Runnable
            public void run() {
                if (WifiDisplayAdapter.this.mDisplayController != null) {
                    WifiDisplayAdapter.this.mDisplayController.requestStopScan();
                }
            }
        });
    }

    public void requestConnectLocked(final String address) {
        getHandler().post(new Runnable() { // from class: com.android.server.display.WifiDisplayAdapter.4
            @Override // java.lang.Runnable
            public void run() {
                if (WifiDisplayAdapter.this.mDisplayController != null) {
                    WifiDisplayAdapter.this.mDisplayController.requestConnect(address);
                }
            }
        });
    }

    public void requestPauseLocked() {
        getHandler().post(new Runnable() { // from class: com.android.server.display.WifiDisplayAdapter.5
            @Override // java.lang.Runnable
            public void run() {
                if (WifiDisplayAdapter.this.mDisplayController != null) {
                    WifiDisplayAdapter.this.mDisplayController.requestPause();
                }
            }
        });
    }

    public void requestResumeLocked() {
        getHandler().post(new Runnable() { // from class: com.android.server.display.WifiDisplayAdapter.6
            @Override // java.lang.Runnable
            public void run() {
                if (WifiDisplayAdapter.this.mDisplayController != null) {
                    WifiDisplayAdapter.this.mDisplayController.requestResume();
                }
            }
        });
    }

    public void requestDisconnectLocked() {
        getHandler().post(new Runnable() { // from class: com.android.server.display.WifiDisplayAdapter.7
            @Override // java.lang.Runnable
            public void run() {
                if (WifiDisplayAdapter.this.mDisplayController != null) {
                    WifiDisplayAdapter.this.mDisplayController.requestDisconnect();
                }
            }
        });
    }

    public void requestRenameLocked(String address, String alias) {
        if (alias != null) {
            alias = alias.trim();
            if (alias.isEmpty() || alias.equals(address)) {
                alias = null;
            }
        }
        WifiDisplay display = this.mPersistentDataStore.getRememberedWifiDisplay(address);
        if (display != null && !Objects.equals(display.getDeviceAlias(), alias)) {
            if (this.mPersistentDataStore.rememberWifiDisplay(new WifiDisplay(address, display.getDeviceName(), alias, false, false, false))) {
                this.mPersistentDataStore.saveIfNeeded();
                updateRememberedDisplaysLocked();
                scheduleStatusChangedBroadcastLocked();
            }
        }
        WifiDisplay wifiDisplay = this.mActiveDisplay;
        if (wifiDisplay != null && wifiDisplay.getDeviceAddress().equals(address)) {
            renameDisplayDeviceLocked(this.mActiveDisplay.getFriendlyDisplayName());
        }
    }

    public void requestForgetLocked(String address) {
        if (this.mPersistentDataStore.forgetWifiDisplay(address)) {
            this.mPersistentDataStore.saveIfNeeded();
            updateRememberedDisplaysLocked();
            scheduleStatusChangedBroadcastLocked();
        }
        WifiDisplay wifiDisplay = this.mActiveDisplay;
        if (wifiDisplay != null && wifiDisplay.getDeviceAddress().equals(address)) {
            requestDisconnectLocked();
        }
    }

    public WifiDisplayStatus getWifiDisplayStatusLocked() {
        if (this.mCurrentStatus == null) {
            this.mCurrentStatus = new WifiDisplayStatus(this.mFeatureState, this.mScanState, this.mActiveDisplayState, this.mActiveDisplay, this.mDisplays, this.mSessionInfo);
        }
        return this.mCurrentStatus;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateDisplaysLocked() {
        WifiDisplay[] wifiDisplayArr;
        List<WifiDisplay> displays = new ArrayList<>(this.mAvailableDisplays.length + this.mRememberedDisplays.length);
        boolean[] remembered = new boolean[this.mAvailableDisplays.length];
        for (WifiDisplay d : this.mRememberedDisplays) {
            boolean available = false;
            int i = 0;
            while (true) {
                WifiDisplay[] wifiDisplayArr2 = this.mAvailableDisplays;
                if (i < wifiDisplayArr2.length) {
                    if (!d.equals(wifiDisplayArr2[i])) {
                        i++;
                    } else {
                        available = true;
                        remembered[i] = true;
                        break;
                    }
                } else {
                    break;
                }
            }
            if (!available) {
                displays.add(new WifiDisplay(d.getDeviceAddress(), d.getDeviceName(), d.getDeviceAlias(), false, false, true));
            }
        }
        int i2 = 0;
        while (true) {
            WifiDisplay[] wifiDisplayArr3 = this.mAvailableDisplays;
            if (i2 < wifiDisplayArr3.length) {
                WifiDisplay d2 = wifiDisplayArr3[i2];
                displays.add(new WifiDisplay(d2.getDeviceAddress(), d2.getDeviceName(), d2.getDeviceAlias(), true, d2.canConnect(), remembered[i2]));
                i2++;
            } else {
                this.mDisplays = (WifiDisplay[]) displays.toArray(WifiDisplay.EMPTY_ARRAY);
                return;
            }
        }
    }

    private void updateRememberedDisplaysLocked() {
        this.mRememberedDisplays = this.mPersistentDataStore.getRememberedWifiDisplays();
        this.mActiveDisplay = this.mPersistentDataStore.applyWifiDisplayAlias(this.mActiveDisplay);
        this.mAvailableDisplays = this.mPersistentDataStore.applyWifiDisplayAliases(this.mAvailableDisplays);
        updateDisplaysLocked();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void fixRememberedDisplayNamesFromAvailableDisplaysLocked() {
        boolean changed = false;
        int i = 0;
        while (true) {
            WifiDisplay[] wifiDisplayArr = this.mRememberedDisplays;
            if (i >= wifiDisplayArr.length) {
                break;
            }
            WifiDisplay rememberedDisplay = wifiDisplayArr[i];
            WifiDisplay availableDisplay = findAvailableDisplayLocked(rememberedDisplay.getDeviceAddress());
            if (availableDisplay != null && !rememberedDisplay.equals(availableDisplay)) {
                this.mRememberedDisplays[i] = availableDisplay;
                changed |= this.mPersistentDataStore.rememberWifiDisplay(availableDisplay);
            }
            i++;
        }
        if (changed) {
            this.mPersistentDataStore.saveIfNeeded();
        }
    }

    private WifiDisplay findAvailableDisplayLocked(String address) {
        WifiDisplay[] wifiDisplayArr;
        for (WifiDisplay display : this.mAvailableDisplays) {
            if (display.getDeviceAddress().equals(address)) {
                return display;
            }
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void addDisplayDeviceLocked(WifiDisplay display, Surface surface, int width, int height, int flags) {
        int deviceFlags;
        removeDisplayDeviceLocked();
        if (this.mPersistentDataStore.rememberWifiDisplay(display)) {
            this.mPersistentDataStore.saveIfNeeded();
            updateRememberedDisplaysLocked();
            scheduleStatusChangedBroadcastLocked();
        }
        boolean secure = (flags & 1) != 0;
        if (!secure) {
            deviceFlags = 64;
        } else {
            int deviceFlags2 = 64 | 4;
            if (!this.mSupportsProtectedBuffers) {
                deviceFlags = deviceFlags2;
            } else {
                deviceFlags = deviceFlags2 | 8;
            }
        }
        String name = display.getFriendlyDisplayName();
        String address = display.getDeviceAddress();
        IBinder displayToken = SurfaceControl.createDisplay(name + "isWifiDpyForHWC", secure);
        WifiDisplayDevice wifiDisplayDevice = new WifiDisplayDevice(displayToken, name, width, height, 60.0f, deviceFlags, address, surface);
        this.mDisplayDevice = wifiDisplayDevice;
        sendDisplayDeviceEventLocked(wifiDisplayDevice, 1);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void removeDisplayDeviceLocked() {
        WifiDisplayDevice wifiDisplayDevice = this.mDisplayDevice;
        if (wifiDisplayDevice != null) {
            wifiDisplayDevice.destroyLocked();
            sendDisplayDeviceEventLocked(this.mDisplayDevice, 3);
            this.mDisplayDevice = null;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void renameDisplayDeviceLocked(String name) {
        WifiDisplayDevice wifiDisplayDevice = this.mDisplayDevice;
        if (wifiDisplayDevice != null && !wifiDisplayDevice.getNameLocked().equals(name)) {
            this.mDisplayDevice.setNameLocked(name);
            sendDisplayDeviceEventLocked(this.mDisplayDevice, 2);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void scheduleStatusChangedBroadcastLocked() {
        this.mCurrentStatus = null;
        if (!this.mPendingStatusChangeBroadcast) {
            this.mPendingStatusChangeBroadcast = true;
            this.mHandler.sendEmptyMessage(1);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleSendStatusChangeBroadcast() {
        synchronized (getSyncRoot()) {
            if (this.mPendingStatusChangeBroadcast) {
                this.mPendingStatusChangeBroadcast = false;
                Intent intent = new Intent("android.hardware.display.action.WIFI_DISPLAY_STATUS_CHANGED");
                intent.addFlags(1073741824);
                intent.putExtra("android.hardware.display.extra.WIFI_DISPLAY_STATUS", (Parcelable) getWifiDisplayStatusLocked());
                getContext().sendBroadcastAsUser(intent, UserHandle.ALL);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class WifiDisplayDevice extends DisplayDevice {
        private final DisplayAddress mAddress;
        private final int mFlags;
        private final int mHeight;
        private DisplayDeviceInfo mInfo;
        private final Display.Mode mMode;
        private String mName;
        private final float mRefreshRate;
        private Surface mSurface;
        private final int mWidth;

        public WifiDisplayDevice(IBinder displayToken, String name, int width, int height, float refreshRate, int flags, String address, Surface surface) {
            super(WifiDisplayAdapter.this, displayToken, WifiDisplayAdapter.DISPLAY_NAME_PREFIX + address, WifiDisplayAdapter.this.getContext());
            this.mName = name;
            this.mWidth = width;
            this.mHeight = height;
            this.mRefreshRate = refreshRate;
            this.mFlags = flags;
            this.mAddress = DisplayAddress.fromMacAddress(address);
            this.mSurface = surface;
            this.mMode = DisplayAdapter.createMode(width, height, refreshRate);
        }

        @Override // com.android.server.display.DisplayDevice
        public boolean hasStableUniqueId() {
            return true;
        }

        public void destroyLocked() {
            Surface surface = this.mSurface;
            if (surface != null) {
                surface.release();
                this.mSurface = null;
            }
            SurfaceControl.destroyDisplay(getDisplayTokenLocked());
        }

        public void setNameLocked(String name) {
            this.mName = name;
            this.mInfo = null;
        }

        @Override // com.android.server.display.DisplayDevice
        public void performTraversalLocked(SurfaceControl.Transaction t) {
            Surface surface = this.mSurface;
            if (surface != null) {
                setSurfaceLocked(t, surface);
            }
        }

        @Override // com.android.server.display.DisplayDevice
        public DisplayDeviceInfo getDisplayDeviceInfoLocked() {
            if (this.mInfo == null) {
                DisplayDeviceInfo displayDeviceInfo = new DisplayDeviceInfo();
                this.mInfo = displayDeviceInfo;
                displayDeviceInfo.name = this.mName;
                this.mInfo.uniqueId = getUniqueId();
                this.mInfo.width = this.mWidth;
                this.mInfo.height = this.mHeight;
                this.mInfo.modeId = this.mMode.getModeId();
                this.mInfo.defaultModeId = this.mMode.getModeId();
                this.mInfo.supportedModes = new Display.Mode[]{this.mMode};
                this.mInfo.presentationDeadlineNanos = 1000000000 / ((int) this.mRefreshRate);
                this.mInfo.flags = this.mFlags;
                this.mInfo.type = 3;
                this.mInfo.address = this.mAddress;
                this.mInfo.touch = 2;
                this.mInfo.setAssumedDensityForExternalDisplay(this.mWidth, this.mHeight);
                this.mInfo.flags |= 8192;
            }
            return this.mInfo;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class WifiDisplayHandler extends Handler {
        public WifiDisplayHandler(Looper looper) {
            super(looper, null, true);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    WifiDisplayAdapter.this.handleSendStatusChangeBroadcast();
                    return;
                default:
                    return;
            }
        }
    }
}
