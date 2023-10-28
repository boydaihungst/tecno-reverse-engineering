package com.android.server.hdmi;

import android.hardware.hdmi.DeviceFeatures;
import android.hardware.hdmi.HdmiDeviceInfo;
import android.hardware.hdmi.IHdmiControlCallback;
import android.hardware.input.InputManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.Slog;
import android.view.KeyEvent;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.hdmi.HdmiAnnotations;
import com.android.server.hdmi.HdmiCecController;
import com.android.server.hdmi.HdmiControlService;
import com.android.server.location.gnss.hal.GnssNative;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.function.Predicate;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public abstract class HdmiCecLocalDevice {
    private static final int DEVICE_CLEANUP_TIMEOUT = 5000;
    private static final int FOLLOWER_SAFETY_TIMEOUT = 550;
    private static final int MAX_HDMI_ACTIVE_SOURCE_HISTORY = 10;
    private static final int MSG_DISABLE_DEVICE_TIMEOUT = 1;
    private static final int MSG_USER_CONTROL_RELEASE_TIMEOUT = 2;
    private static final String TAG = "HdmiCecLocalDevice";
    private int mActiveRoutingPath;
    protected HdmiDeviceInfo mDeviceInfo;
    protected final int mDeviceType;
    protected final Object mLock;
    protected PendingActionClearedCallback mPendingActionClearedCallback;
    protected int mPreferredAddress;
    protected final HdmiControlService mService;
    HdmiCecStandbyModeHandler mStandbyHandler;
    protected int mLastKeycode = -1;
    protected int mLastKeyRepeatCount = 0;
    private final ArrayBlockingQueue<HdmiCecController.Dumpable> mActiveSourceHistory = new ArrayBlockingQueue<>(10);
    protected final HdmiCecMessageCache mCecMessageCache = new HdmiCecMessageCache();
    final ArrayList<HdmiCecFeatureAction> mActions = new ArrayList<>();
    private final Handler mHandler = new Handler() { // from class: com.android.server.hdmi.HdmiCecLocalDevice.1
        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    HdmiCecLocalDevice.this.handleDisableDeviceTimeout();
                    return;
                case 2:
                    HdmiCecLocalDevice.this.handleUserControlReleased();
                    return;
                default:
                    return;
            }
        }
    };

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public interface PendingActionClearedCallback {
        void onCleared(HdmiCecLocalDevice hdmiCecLocalDevice);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public abstract int getPreferredAddress();

    protected abstract List<Integer> getRcFeatures();

    protected abstract int getRcProfile();

    protected abstract void onAddressAllocated(int i, int i2);

    protected abstract void setPreferredAddress(int i);

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class ActiveSource {
        int logicalAddress;
        int physicalAddress;

        public ActiveSource() {
            invalidate();
        }

        public ActiveSource(int logical, int physical) {
            this.logicalAddress = logical;
            this.physicalAddress = physical;
        }

        public static ActiveSource of(ActiveSource source) {
            return new ActiveSource(source.logicalAddress, source.physicalAddress);
        }

        public static ActiveSource of(int logical, int physical) {
            return new ActiveSource(logical, physical);
        }

        public boolean isValid() {
            return HdmiUtils.isValidAddress(this.logicalAddress);
        }

        public void invalidate() {
            this.logicalAddress = -1;
            this.physicalAddress = GnssNative.GNSS_AIDING_TYPE_ALL;
        }

        public boolean equals(int logical, int physical) {
            return this.logicalAddress == logical && this.physicalAddress == physical;
        }

        public boolean equals(Object obj) {
            if (obj instanceof ActiveSource) {
                ActiveSource that = (ActiveSource) obj;
                return that.logicalAddress == this.logicalAddress && that.physicalAddress == this.physicalAddress;
            }
            return false;
        }

        public int hashCode() {
            return (this.logicalAddress * 29) + this.physicalAddress;
        }

        public String toString() {
            StringBuilder s = new StringBuilder();
            int i = this.logicalAddress;
            String logicalAddressString = i == -1 ? "invalid" : String.format("0x%02x", Integer.valueOf(i));
            s.append("(").append(logicalAddressString);
            int i2 = this.physicalAddress;
            String physicalAddressString = i2 != 65535 ? String.format("0x%04x", Integer.valueOf(i2)) : "invalid";
            s.append(", ").append(physicalAddressString).append(")");
            return s.toString();
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public HdmiCecLocalDevice(HdmiControlService service, int deviceType) {
        this.mService = service;
        this.mDeviceType = deviceType;
        this.mLock = service.getServiceLock();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static HdmiCecLocalDevice create(HdmiControlService service, int deviceType) {
        switch (deviceType) {
            case 0:
                return new HdmiCecLocalDeviceTv(service);
            case 4:
                return new HdmiCecLocalDevicePlayback(service);
            case 5:
                return new HdmiCecLocalDeviceAudioSystem(service);
            default:
                return null;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @HdmiAnnotations.ServiceThreadOnly
    public void init() {
        assertRunOnServiceThread();
        this.mPreferredAddress = getPreferredAddress();
        if (this.mHandler.hasMessages(1)) {
            this.mHandler.removeMessages(1);
            handleDisableDeviceTimeout();
        }
        this.mPendingActionClearedCallback = null;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public boolean isInputReady(int deviceId) {
        return true;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public boolean canGoToStandby() {
        return true;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @HdmiAnnotations.ServiceThreadOnly
    public int dispatchMessage(HdmiCecMessage message) {
        assertRunOnServiceThread();
        int dest = message.getDestination();
        if (dest != this.mDeviceInfo.getLogicalAddress() && dest != 15) {
            return -2;
        }
        if (this.mService.isPowerStandby() && !this.mService.isWakeUpMessageReceived() && this.mStandbyHandler.handleCommand(message)) {
            return -1;
        }
        this.mCecMessageCache.cacheMessage(message);
        return onMessage(message);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @HdmiAnnotations.ServiceThreadOnly
    public boolean isAlreadyActiveSource(HdmiDeviceInfo targetDevice, int targetAddress, IHdmiControlCallback callback) {
        ActiveSource active = getActiveSource();
        if (targetDevice.getDevicePowerStatus() == 0 && active.isValid() && targetAddress == active.logicalAddress) {
            invokeCallback(callback, 0);
            return true;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @HdmiAnnotations.ServiceThreadOnly
    public void clearDeviceInfoList() {
        assertRunOnServiceThread();
        this.mService.getHdmiCecNetwork().clearDeviceList();
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @HdmiAnnotations.ServiceThreadOnly
    public final int onMessage(HdmiCecMessage message) {
        assertRunOnServiceThread();
        if (dispatchMessageToAction(message)) {
            return -1;
        }
        if (message instanceof SetAudioVolumeLevelMessage) {
            return handleSetAudioVolumeLevel((SetAudioVolumeLevelMessage) message);
        }
        switch (message.getOpcode()) {
            case 4:
                return handleImageViewOn(message);
            case 10:
                return handleRecordStatus(message);
            case 13:
                return handleTextViewOn(message);
            case 15:
                return handleRecordTvScreen(message);
            case 50:
                return handleSetMenuLanguage(message);
            case 53:
                return handleTimerStatus(message);
            case 54:
                return handleStandby(message);
            case 67:
                return handleTimerClearedStatus(message);
            case 68:
                return handleUserControlPressed(message);
            case 69:
                return handleUserControlReleased();
            case 70:
                return handleGiveOsdName(message);
            case 71:
                return handleSetOsdName(message);
            case 112:
                return handleSystemAudioModeRequest(message);
            case 113:
                return handleGiveAudioStatus(message);
            case 114:
                return handleSetSystemAudioMode(message);
            case 122:
                return handleReportAudioStatus(message);
            case 125:
                return handleGiveSystemAudioModeStatus(message);
            case 126:
                return handleSystemAudioModeStatus(message);
            case 128:
                return handleRoutingChange(message);
            case 129:
                return handleRoutingInformation(message);
            case 130:
                return handleActiveSource(message);
            case 131:
                return handleGivePhysicalAddress(message);
            case 132:
                return handleReportPhysicalAddress(message);
            case 133:
                return handleRequestActiveSource(message);
            case 134:
                return handleSetStreamPath(message);
            case 137:
                return handleVendorCommand(message);
            case 140:
                return handleGiveDeviceVendorId(message);
            case 141:
                return handleMenuRequest(message);
            case 142:
                return handleMenuStatus(message);
            case 143:
                return handleGiveDevicePowerStatus(message);
            case 144:
                return handleReportPowerStatus(message);
            case 145:
                return handleGetMenuLanguage(message);
            case 157:
                return handleInactiveSource(message);
            case 158:
                return handleCecVersion();
            case 159:
                return handleGetCecVersion(message);
            case 160:
                return handleVendorCommandWithId(message);
            case 163:
                return handleReportShortAudioDescriptor(message);
            case 164:
                return handleRequestShortAudioDescriptor(message);
            case 165:
                return handleGiveFeatures(message);
            case 192:
                return handleInitiateArc(message);
            case 193:
                return handleReportArcInitiate(message);
            case 194:
                return handleReportArcTermination(message);
            case 195:
                return handleRequestArcInitiate(message);
            case 196:
                return handleRequestArcTermination(message);
            case 197:
                return handleTerminateArc(message);
            default:
                return -2;
        }
    }

    @HdmiAnnotations.ServiceThreadOnly
    private boolean dispatchMessageToAction(HdmiCecMessage message) {
        assertRunOnServiceThread();
        boolean processed = false;
        Iterator it = new ArrayList(this.mActions).iterator();
        while (it.hasNext()) {
            HdmiCecFeatureAction action = (HdmiCecFeatureAction) it.next();
            boolean result = action.processCommand(message);
            processed = processed || result;
        }
        return processed;
    }

    @HdmiAnnotations.ServiceThreadOnly
    protected int handleGivePhysicalAddress(HdmiCecMessage message) {
        assertRunOnServiceThread();
        int physicalAddress = this.mService.getPhysicalAddress();
        if (physicalAddress == 65535) {
            this.mService.maySendFeatureAbortCommand(message, 5);
            return -1;
        }
        HdmiCecMessage cecMessage = HdmiCecMessageBuilder.buildReportPhysicalAddressCommand(this.mDeviceInfo.getLogicalAddress(), physicalAddress, this.mDeviceType);
        this.mService.sendCecCommand(cecMessage);
        return -1;
    }

    @HdmiAnnotations.ServiceThreadOnly
    protected int handleGiveDeviceVendorId(HdmiCecMessage message) {
        assertRunOnServiceThread();
        int vendorId = this.mService.getVendorId();
        if (vendorId == 1) {
            this.mService.maySendFeatureAbortCommand(message, 5);
            return -1;
        }
        HdmiCecMessage cecMessage = HdmiCecMessageBuilder.buildDeviceVendorIdCommand(this.mDeviceInfo.getLogicalAddress(), vendorId);
        this.mService.sendCecCommand(cecMessage);
        return -1;
    }

    @HdmiAnnotations.ServiceThreadOnly
    protected int handleGetCecVersion(HdmiCecMessage message) {
        assertRunOnServiceThread();
        int version = this.mService.getCecVersion();
        HdmiCecMessage cecMessage = HdmiCecMessageBuilder.buildCecVersion(message.getDestination(), message.getSource(), version);
        this.mService.sendCecCommand(cecMessage);
        return -1;
    }

    @HdmiAnnotations.ServiceThreadOnly
    protected int handleCecVersion() {
        assertRunOnServiceThread();
        return -1;
    }

    @HdmiAnnotations.ServiceThreadOnly
    protected int handleActiveSource(HdmiCecMessage message) {
        return -2;
    }

    @HdmiAnnotations.ServiceThreadOnly
    protected int handleInactiveSource(HdmiCecMessage message) {
        return -2;
    }

    @HdmiAnnotations.ServiceThreadOnly
    protected int handleRequestActiveSource(HdmiCecMessage message) {
        return -2;
    }

    @HdmiAnnotations.ServiceThreadOnly
    protected int handleGetMenuLanguage(HdmiCecMessage message) {
        assertRunOnServiceThread();
        Slog.w(TAG, "Only TV can handle <Get Menu Language>:" + message.toString());
        return -2;
    }

    @HdmiAnnotations.ServiceThreadOnly
    protected int handleSetMenuLanguage(HdmiCecMessage message) {
        assertRunOnServiceThread();
        Slog.w(TAG, "Only Playback device can handle <Set Menu Language>:" + message.toString());
        return -2;
    }

    @HdmiAnnotations.ServiceThreadOnly
    protected int handleGiveOsdName(HdmiCecMessage message) {
        assertRunOnServiceThread();
        buildAndSendSetOsdName(message.getSource());
        return -1;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void buildAndSendSetOsdName(int dest) {
        final HdmiCecMessage cecMessage = HdmiCecMessageBuilder.buildSetOsdNameCommand(this.mDeviceInfo.getLogicalAddress(), dest, this.mDeviceInfo.getDisplayName());
        if (cecMessage != null) {
            this.mService.sendCecCommand(cecMessage, new HdmiControlService.SendMessageCallback() { // from class: com.android.server.hdmi.HdmiCecLocalDevice.2
                @Override // com.android.server.hdmi.HdmiControlService.SendMessageCallback
                public void onSendCompleted(int error) {
                    if (error != 0) {
                        HdmiLogger.debug("Failed to send cec command " + cecMessage, new Object[0]);
                    }
                }
            });
        } else {
            Slog.w(TAG, "Failed to build <Get Osd Name>:" + this.mDeviceInfo.getDisplayName());
        }
    }

    protected int handleRoutingChange(HdmiCecMessage message) {
        return -2;
    }

    protected int handleRoutingInformation(HdmiCecMessage message) {
        return -2;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public int handleReportPhysicalAddress(HdmiCecMessage message) {
        int address = message.getSource();
        if (hasAction(DeviceDiscoveryAction.class)) {
            Slog.i(TAG, "Ignored while Device Discovery Action is in progress: " + message);
            return -1;
        }
        HdmiDeviceInfo cecDeviceInfo = this.mService.getHdmiCecNetwork().getCecDeviceInfo(address);
        if (cecDeviceInfo != null && cecDeviceInfo.getDisplayName().equals(HdmiUtils.getDefaultDeviceName(address))) {
            this.mService.sendCecCommand(HdmiCecMessageBuilder.buildGiveOsdNameCommand(this.mDeviceInfo.getLogicalAddress(), address));
        }
        return -1;
    }

    protected int handleSystemAudioModeStatus(HdmiCecMessage message) {
        return -2;
    }

    protected int handleGiveSystemAudioModeStatus(HdmiCecMessage message) {
        return -2;
    }

    protected int handleSetSystemAudioMode(HdmiCecMessage message) {
        return -2;
    }

    protected int handleSystemAudioModeRequest(HdmiCecMessage message) {
        return -2;
    }

    protected int handleTerminateArc(HdmiCecMessage message) {
        return -2;
    }

    protected int handleInitiateArc(HdmiCecMessage message) {
        return -2;
    }

    protected int handleRequestArcInitiate(HdmiCecMessage message) {
        return -2;
    }

    protected int handleRequestArcTermination(HdmiCecMessage message) {
        return -2;
    }

    protected int handleReportArcInitiate(HdmiCecMessage message) {
        return -2;
    }

    protected int handleReportArcTermination(HdmiCecMessage message) {
        return -2;
    }

    protected int handleReportAudioStatus(HdmiCecMessage message) {
        return -2;
    }

    protected int handleGiveAudioStatus(HdmiCecMessage message) {
        return -2;
    }

    protected int handleRequestShortAudioDescriptor(HdmiCecMessage message) {
        return -2;
    }

    protected int handleReportShortAudioDescriptor(HdmiCecMessage message) {
        return -2;
    }

    protected int handleSetAudioVolumeLevel(SetAudioVolumeLevelMessage message) {
        return -2;
    }

    protected DeviceFeatures computeDeviceFeatures() {
        return DeviceFeatures.NO_FEATURES_SUPPORTED;
    }

    private void updateDeviceFeatures() {
        synchronized (this.mLock) {
            setDeviceInfo(getDeviceInfo().toBuilder().setDeviceFeatures(computeDeviceFeatures()).build());
        }
    }

    protected final DeviceFeatures getDeviceFeatures() {
        DeviceFeatures deviceFeatures;
        updateDeviceFeatures();
        synchronized (this.mLock) {
            deviceFeatures = getDeviceInfo().getDeviceFeatures();
        }
        return deviceFeatures;
    }

    protected int handleGiveFeatures(HdmiCecMessage message) {
        if (this.mService.getCecVersion() < 6) {
            return 0;
        }
        reportFeatures();
        return -1;
    }

    protected void reportFeatures() {
        int logicalAddress;
        List<Integer> localDeviceTypes = new ArrayList<>();
        for (HdmiCecLocalDevice localDevice : this.mService.getAllLocalDevices()) {
            localDeviceTypes.add(Integer.valueOf(localDevice.mDeviceType));
        }
        int rcProfile = getRcProfile();
        List<Integer> rcFeatures = getRcFeatures();
        DeviceFeatures deviceFeatures = getDeviceFeatures();
        synchronized (this.mLock) {
            logicalAddress = this.mDeviceInfo.getLogicalAddress();
        }
        HdmiControlService hdmiControlService = this.mService;
        hdmiControlService.sendCecCommand(ReportFeaturesMessage.build(logicalAddress, hdmiControlService.getCecVersion(), localDeviceTypes, rcProfile, rcFeatures, deviceFeatures));
    }

    @HdmiAnnotations.ServiceThreadOnly
    protected int handleStandby(HdmiCecMessage message) {
        assertRunOnServiceThread();
        if (this.mService.isControlEnabled() && !this.mService.isProhibitMode() && this.mService.isPowerOnOrTransient()) {
            this.mService.standby();
            return -1;
        }
        return 1;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @HdmiAnnotations.ServiceThreadOnly
    public int handleUserControlPressed(HdmiCecMessage message) {
        assertRunOnServiceThread();
        this.mHandler.removeMessages(2);
        if (this.mService.isPowerOnOrTransient() && isPowerOffOrToggleCommand(message)) {
            this.mService.standby();
            return -1;
        } else if (this.mService.isPowerStandbyOrTransient() && isPowerOnOrToggleCommand(message)) {
            this.mService.wakeUp();
            return -1;
        } else if (this.mService.getHdmiCecVolumeControl() == 0 && isVolumeOrMuteCommand(message)) {
            return 4;
        } else {
            if (isPowerOffOrToggleCommand(message) || isPowerOnOrToggleCommand(message)) {
                return -1;
            }
            long downTime = SystemClock.uptimeMillis();
            byte[] params = message.getParams();
            int keycode = HdmiCecKeycode.cecKeycodeAndParamsToAndroidKey(params);
            int keyRepeatCount = 0;
            int i = this.mLastKeycode;
            if (i != -1) {
                if (keycode == i) {
                    keyRepeatCount = this.mLastKeyRepeatCount + 1;
                } else {
                    injectKeyEvent(downTime, 1, i, 0);
                }
            }
            this.mLastKeycode = keycode;
            this.mLastKeyRepeatCount = keyRepeatCount;
            if (keycode != -1) {
                injectKeyEvent(downTime, 0, keycode, keyRepeatCount);
                Handler handler = this.mHandler;
                handler.sendMessageDelayed(Message.obtain(handler, 2), 550L);
                return -1;
            } else if (params.length > 0) {
                return handleUnmappedCecKeycode(params[0]);
            } else {
                return 3;
            }
        }
    }

    @HdmiAnnotations.ServiceThreadOnly
    protected int handleUnmappedCecKeycode(int cecKeycode) {
        if (cecKeycode == 101) {
            this.mService.getAudioManager().adjustStreamVolume(3, -100, 1);
            return -1;
        } else if (cecKeycode == 102) {
            this.mService.getAudioManager().adjustStreamVolume(3, 100, 1);
            return -1;
        } else {
            return 3;
        }
    }

    @HdmiAnnotations.ServiceThreadOnly
    protected int handleUserControlReleased() {
        assertRunOnServiceThread();
        this.mHandler.removeMessages(2);
        this.mLastKeyRepeatCount = 0;
        if (this.mLastKeycode != -1) {
            long upTime = SystemClock.uptimeMillis();
            injectKeyEvent(upTime, 1, this.mLastKeycode, 0);
            this.mLastKeycode = -1;
        }
        return -1;
    }

    static void injectKeyEvent(long time, int action, int keycode, int repeat) {
        KeyEvent keyEvent = KeyEvent.obtain(time, time, action, keycode, repeat, 0, -1, 0, 8, 33554433, null);
        InputManager.getInstance().injectInputEvent(keyEvent, 0);
        keyEvent.recycle();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static boolean isPowerOnOrToggleCommand(HdmiCecMessage message) {
        byte[] params = message.getParams();
        if (message.getOpcode() == 68) {
            return params[0] == 64 || params[0] == 109 || params[0] == 107;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static boolean isPowerOffOrToggleCommand(HdmiCecMessage message) {
        byte[] params = message.getParams();
        if (message.getOpcode() == 68) {
            return params[0] == 108 || params[0] == 107;
        }
        return false;
    }

    static boolean isVolumeOrMuteCommand(HdmiCecMessage message) {
        byte[] params = message.getParams();
        if (message.getOpcode() == 68) {
            return params[0] == 66 || params[0] == 65 || params[0] == 67 || params[0] == 101 || params[0] == 102;
        }
        return false;
    }

    protected int handleTextViewOn(HdmiCecMessage message) {
        return -2;
    }

    protected int handleImageViewOn(HdmiCecMessage message) {
        return -2;
    }

    protected int handleSetStreamPath(HdmiCecMessage message) {
        return -2;
    }

    protected int handleGiveDevicePowerStatus(HdmiCecMessage message) {
        this.mService.sendCecCommand(HdmiCecMessageBuilder.buildReportPowerStatus(this.mDeviceInfo.getLogicalAddress(), message.getSource(), this.mService.getPowerStatus()));
        return -1;
    }

    protected int handleMenuRequest(HdmiCecMessage message) {
        this.mService.sendCecCommand(HdmiCecMessageBuilder.buildReportMenuStatus(this.mDeviceInfo.getLogicalAddress(), message.getSource(), 0));
        return -1;
    }

    protected int handleMenuStatus(HdmiCecMessage message) {
        return -2;
    }

    protected int handleVendorCommand(HdmiCecMessage message) {
        if (!this.mService.invokeVendorCommandListenersOnReceived(this.mDeviceType, message.getSource(), message.getDestination(), message.getParams(), false)) {
            return 4;
        }
        return -1;
    }

    protected int handleVendorCommandWithId(HdmiCecMessage message) {
        byte[] params = message.getParams();
        HdmiUtils.threeBytesToInt(params);
        if (message.getDestination() == 15 || message.getSource() == 15) {
            Slog.v(TAG, "Wrong broadcast vendor command. Ignoring");
            return -1;
        } else if (!this.mService.invokeVendorCommandListenersOnReceived(this.mDeviceType, message.getSource(), message.getDestination(), params, true)) {
            return 4;
        } else {
            return -1;
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void sendStandby(int deviceId) {
    }

    protected int handleSetOsdName(HdmiCecMessage message) {
        return -1;
    }

    protected int handleRecordTvScreen(HdmiCecMessage message) {
        return -2;
    }

    protected int handleTimerClearedStatus(HdmiCecMessage message) {
        return -2;
    }

    protected int handleReportPowerStatus(HdmiCecMessage message) {
        return -1;
    }

    protected int handleTimerStatus(HdmiCecMessage message) {
        return -2;
    }

    protected int handleRecordStatus(HdmiCecMessage message) {
        return -2;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @HdmiAnnotations.ServiceThreadOnly
    public final void handleAddressAllocated(int logicalAddress, int reason) {
        assertRunOnServiceThread();
        this.mPreferredAddress = logicalAddress;
        updateDeviceFeatures();
        if (this.mService.getCecVersion() >= 6) {
            reportFeatures();
        }
        onAddressAllocated(logicalAddress, reason);
        setPreferredAddress(logicalAddress);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getType() {
        return this.mDeviceType;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public HdmiDeviceInfo getDeviceInfo() {
        HdmiDeviceInfo hdmiDeviceInfo;
        synchronized (this.mLock) {
            hdmiDeviceInfo = this.mDeviceInfo;
        }
        return hdmiDeviceInfo;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setDeviceInfo(HdmiDeviceInfo info) {
        synchronized (this.mLock) {
            this.mDeviceInfo = info;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @HdmiAnnotations.ServiceThreadOnly
    public boolean isAddressOf(int addr) {
        assertRunOnServiceThread();
        return addr == this.mDeviceInfo.getLogicalAddress();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @HdmiAnnotations.ServiceThreadOnly
    public void addAndStartAction(HdmiCecFeatureAction action) {
        assertRunOnServiceThread();
        this.mActions.add(action);
        if (this.mService.isPowerStandby() || !this.mService.isAddressAllocated()) {
            Slog.i(TAG, "Not ready to start action. Queued for deferred start:" + action);
        } else {
            action.start();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void addAvcAudioStatusAction(int targetAddress) {
        if (!hasAction(AbsoluteVolumeAudioStatusAction.class)) {
            addAndStartAction(new AbsoluteVolumeAudioStatusAction(this, targetAddress));
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void removeAvcAudioStatusAction() {
        removeAction(AbsoluteVolumeAudioStatusAction.class);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateAvcVolume(int volumeIndex) {
        for (AbsoluteVolumeAudioStatusAction action : getActions(AbsoluteVolumeAudioStatusAction.class)) {
            action.updateVolume(volumeIndex);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @HdmiAnnotations.ServiceThreadOnly
    public void queryAvcSupport(final int targetAddress) {
        assertRunOnServiceThread();
        if (this.mService.getCecVersion() >= 6) {
            synchronized (this.mLock) {
                this.mService.sendCecCommand(HdmiCecMessageBuilder.buildGiveFeatures(getDeviceInfo().getLogicalAddress(), targetAddress));
            }
        }
        List<SetAudioVolumeLevelDiscoveryAction> savlDiscoveryActions = getActions(SetAudioVolumeLevelDiscoveryAction.class);
        if (savlDiscoveryActions.stream().noneMatch(new Predicate() { // from class: com.android.server.hdmi.HdmiCecLocalDevice$$ExternalSyntheticLambda0
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return HdmiCecLocalDevice.lambda$queryAvcSupport$0(targetAddress, (SetAudioVolumeLevelDiscoveryAction) obj);
            }
        })) {
            addAndStartAction(new SetAudioVolumeLevelDiscoveryAction(this, targetAddress, new IHdmiControlCallback.Stub() { // from class: com.android.server.hdmi.HdmiCecLocalDevice.3
                public void onComplete(int result) {
                    if (result == 0) {
                        HdmiCecLocalDevice.this.getService().checkAndUpdateAbsoluteVolumeControlState();
                    }
                }
            }));
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$queryAvcSupport$0(int targetAddress, SetAudioVolumeLevelDiscoveryAction a) {
        return a.getTargetAddress() == targetAddress;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @HdmiAnnotations.ServiceThreadOnly
    public void startQueuedActions() {
        assertRunOnServiceThread();
        Iterator it = new ArrayList(this.mActions).iterator();
        while (it.hasNext()) {
            HdmiCecFeatureAction action = (HdmiCecFeatureAction) it.next();
            if (!action.started()) {
                Slog.i(TAG, "Starting queued action:" + action);
                action.start();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @HdmiAnnotations.ServiceThreadOnly
    public <T extends HdmiCecFeatureAction> boolean hasAction(Class<T> clazz) {
        assertRunOnServiceThread();
        Iterator<HdmiCecFeatureAction> it = this.mActions.iterator();
        while (it.hasNext()) {
            HdmiCecFeatureAction action = it.next();
            if (action.getClass().equals(clazz)) {
                return true;
            }
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @HdmiAnnotations.ServiceThreadOnly
    public <T extends HdmiCecFeatureAction> List<T> getActions(Class<T> clazz) {
        assertRunOnServiceThread();
        List<T> actions = Collections.emptyList();
        Iterator<HdmiCecFeatureAction> it = this.mActions.iterator();
        while (it.hasNext()) {
            HdmiCecFeatureAction action = it.next();
            if (action.getClass().equals(clazz)) {
                if (actions.isEmpty()) {
                    List<T> actions2 = new ArrayList<>();
                    actions = actions2;
                }
                actions.add(action);
            }
        }
        return actions;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @HdmiAnnotations.ServiceThreadOnly
    public void removeAction(HdmiCecFeatureAction action) {
        assertRunOnServiceThread();
        action.finish(false);
        this.mActions.remove(action);
        checkIfPendingActionsCleared();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @HdmiAnnotations.ServiceThreadOnly
    public <T extends HdmiCecFeatureAction> void removeAction(Class<T> clazz) {
        assertRunOnServiceThread();
        removeActionExcept(clazz, null);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @HdmiAnnotations.ServiceThreadOnly
    public <T extends HdmiCecFeatureAction> void removeActionExcept(Class<T> clazz, HdmiCecFeatureAction exception) {
        assertRunOnServiceThread();
        Iterator<HdmiCecFeatureAction> iter = this.mActions.iterator();
        while (iter.hasNext()) {
            HdmiCecFeatureAction action = iter.next();
            if (action != exception && action.getClass().equals(clazz)) {
                action.finish(false);
                iter.remove();
            }
        }
        checkIfPendingActionsCleared();
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void checkIfPendingActionsCleared() {
        if (this.mActions.isEmpty() && this.mPendingActionClearedCallback != null) {
            PendingActionClearedCallback callback = this.mPendingActionClearedCallback;
            this.mPendingActionClearedCallback = null;
            callback.onCleared(this);
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void assertRunOnServiceThread() {
        if (Looper.myLooper() != this.mService.getServiceLooper()) {
            throw new IllegalStateException("Should run on service thread.");
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onHotplug(int portId, boolean connected) {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public final HdmiControlService getService() {
        return this.mService;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @HdmiAnnotations.ServiceThreadOnly
    public final boolean isConnectedToArcPort(int path) {
        assertRunOnServiceThread();
        return this.mService.isConnectedToArcPort(path);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActiveSource getActiveSource() {
        return this.mService.getLocalActiveSource();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setActiveSource(ActiveSource newActive, String caller) {
        setActiveSource(newActive.logicalAddress, newActive.physicalAddress, caller);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setActiveSource(HdmiDeviceInfo info, String caller) {
        setActiveSource(info.getLogicalAddress(), info.getPhysicalAddress(), caller);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setActiveSource(int logicalAddress, int physicalAddress, String caller) {
        this.mService.setActiveSource(logicalAddress, physicalAddress, caller);
        this.mService.setLastInputForMhl(-1);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getActivePath() {
        int i;
        synchronized (this.mLock) {
            i = this.mActiveRoutingPath;
        }
        return i;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setActivePath(int path) {
        synchronized (this.mLock) {
            this.mActiveRoutingPath = path;
        }
        this.mService.setActivePortId(pathToPortId(path));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getActivePortId() {
        int pathToPortId;
        synchronized (this.mLock) {
            pathToPortId = this.mService.pathToPortId(this.mActiveRoutingPath);
        }
        return pathToPortId;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setActivePortId(int portId) {
        setActivePath(this.mService.portIdToPath(portId));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getPortId(int physicalAddress) {
        return this.mService.pathToPortId(physicalAddress);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @HdmiAnnotations.ServiceThreadOnly
    public HdmiCecMessageCache getCecMessageCache() {
        assertRunOnServiceThread();
        return this.mCecMessageCache;
    }

    @HdmiAnnotations.ServiceThreadOnly
    int pathToPortId(int newPath) {
        assertRunOnServiceThread();
        return this.mService.pathToPortId(newPath);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void onStandby(boolean initiatedByCec, int standbyAction) {
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void onInitializeCecComplete(int initiatedBy) {
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void disableDevice(boolean initiatedByCec, final PendingActionClearedCallback originalCallback) {
        removeAction(AbsoluteVolumeAudioStatusAction.class);
        removeAction(SetAudioVolumeLevelDiscoveryAction.class);
        this.mPendingActionClearedCallback = new PendingActionClearedCallback() { // from class: com.android.server.hdmi.HdmiCecLocalDevice.4
            @Override // com.android.server.hdmi.HdmiCecLocalDevice.PendingActionClearedCallback
            public void onCleared(HdmiCecLocalDevice device) {
                HdmiCecLocalDevice.this.mHandler.removeMessages(1);
                originalCallback.onCleared(device);
            }
        };
        Handler handler = this.mHandler;
        handler.sendMessageDelayed(Message.obtain(handler, 1), 5000L);
    }

    /* JADX INFO: Access modifiers changed from: private */
    @HdmiAnnotations.ServiceThreadOnly
    public void handleDisableDeviceTimeout() {
        assertRunOnServiceThread();
        Iterator<HdmiCecFeatureAction> iter = this.mActions.iterator();
        while (iter.hasNext()) {
            HdmiCecFeatureAction action = iter.next();
            action.finish(false);
            iter.remove();
        }
        PendingActionClearedCallback pendingActionClearedCallback = this.mPendingActionClearedCallback;
        if (pendingActionClearedCallback != null) {
            pendingActionClearedCallback.onCleared(this);
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @HdmiAnnotations.ServiceThreadOnly
    public void sendKeyEvent(int keyCode, boolean isPressed) {
        assertRunOnServiceThread();
        if (!HdmiCecKeycode.isSupportedKeycode(keyCode)) {
            Slog.w(TAG, "Unsupported key: " + keyCode);
            return;
        }
        List<SendKeyAction> action = getActions(SendKeyAction.class);
        int logicalAddress = findKeyReceiverAddress();
        if (logicalAddress == -1 || logicalAddress == this.mDeviceInfo.getLogicalAddress()) {
            Slog.w(TAG, "Discard key event: " + keyCode + ", pressed:" + isPressed + ", receiverAddr=" + logicalAddress);
        } else if (!action.isEmpty()) {
            action.get(0).processKeyEvent(keyCode, isPressed);
        } else if (isPressed) {
            addAndStartAction(new SendKeyAction(this, logicalAddress, keyCode));
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @HdmiAnnotations.ServiceThreadOnly
    public void sendVolumeKeyEvent(int keyCode, boolean isPressed) {
        assertRunOnServiceThread();
        if (this.mService.getHdmiCecVolumeControl() == 0) {
            return;
        }
        if (!HdmiCecKeycode.isVolumeKeycode(keyCode)) {
            Slog.w(TAG, "Not a volume key: " + keyCode);
            return;
        }
        List<SendKeyAction> action = getActions(SendKeyAction.class);
        int logicalAddress = findAudioReceiverAddress();
        if (logicalAddress == -1 || logicalAddress == this.mDeviceInfo.getLogicalAddress()) {
            Slog.w(TAG, "Discard volume key event: " + keyCode + ", pressed:" + isPressed + ", receiverAddr=" + logicalAddress);
        } else if (!action.isEmpty()) {
            action.get(0).processKeyEvent(keyCode, isPressed);
        } else if (isPressed) {
            addAndStartAction(new SendKeyAction(this, logicalAddress, keyCode));
        }
    }

    protected int findKeyReceiverAddress() {
        Slog.w(TAG, "findKeyReceiverAddress is not implemented");
        return -1;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public int findAudioReceiverAddress() {
        Slog.w(TAG, "findAudioReceiverAddress is not implemented");
        return -1;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @HdmiAnnotations.ServiceThreadOnly
    public void invokeCallback(IHdmiControlCallback callback, int result) {
        assertRunOnServiceThread();
        if (callback == null) {
            return;
        }
        try {
            callback.onComplete(result);
        } catch (RemoteException e) {
            Slog.e(TAG, "Invoking callback failed:" + e);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void sendUserControlPressedAndReleased(int targetAddress, int cecKeycode) {
        this.mService.sendCecCommand(HdmiCecMessageBuilder.buildUserControlPressed(this.mDeviceInfo.getLogicalAddress(), targetAddress, cecKeycode));
        this.mService.sendCecCommand(HdmiCecMessageBuilder.buildUserControlReleased(this.mDeviceInfo.getLogicalAddress(), targetAddress));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void addActiveSourceHistoryItem(ActiveSource activeSource, boolean isActiveSource, String caller) {
        ActiveSourceHistoryRecord record = new ActiveSourceHistoryRecord(activeSource, isActiveSource, caller);
        if (!this.mActiveSourceHistory.offer(record)) {
            this.mActiveSourceHistory.poll();
            this.mActiveSourceHistory.offer(record);
        }
    }

    public ArrayBlockingQueue<HdmiCecController.Dumpable> getActiveSourceHistory() {
        return this.mActiveSourceHistory;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void dump(IndentingPrintWriter pw) {
        pw.println("mDeviceType: " + this.mDeviceType);
        pw.println("mPreferredAddress: " + this.mPreferredAddress);
        pw.println("mDeviceInfo: " + this.mDeviceInfo);
        pw.println("mActiveSource: " + getActiveSource());
        pw.println(String.format("mActiveRoutingPath: 0x%04x", Integer.valueOf(this.mActiveRoutingPath)));
    }

    protected int getActivePathOnSwitchFromActivePortId(int activePortId) {
        int myPhysicalAddress = this.mService.getPhysicalAddress();
        int finalMask = activePortId << 8;
        for (int mask = 3840; mask > 15 && (myPhysicalAddress & mask) != 0; mask >>= 4) {
            finalMask >>= 4;
        }
        return finalMask | myPhysicalAddress;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static final class ActiveSourceHistoryRecord extends HdmiCecController.Dumpable {
        private final ActiveSource mActiveSource;
        private final String mCaller;
        private final boolean mIsActiveSource;

        private ActiveSourceHistoryRecord(ActiveSource mActiveSource, boolean mIsActiveSource, String caller) {
            this.mActiveSource = mActiveSource;
            this.mIsActiveSource = mIsActiveSource;
            this.mCaller = caller;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        @Override // com.android.server.hdmi.HdmiCecController.Dumpable
        public void dump(IndentingPrintWriter pw, SimpleDateFormat sdf) {
            pw.print("time=");
            pw.print(sdf.format(new Date(this.mTime)));
            pw.print(" active source=");
            pw.print(this.mActiveSource);
            pw.print(" isActiveSource=");
            pw.print(this.mIsActiveSource);
            pw.print(" from=");
            pw.println(this.mCaller);
        }
    }
}
