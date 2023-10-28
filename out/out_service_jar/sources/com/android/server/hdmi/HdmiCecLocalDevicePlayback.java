package com.android.server.hdmi;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.hardware.hdmi.HdmiDeviceInfo;
import android.hardware.hdmi.IHdmiControlCallback;
import android.os.Binder;
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemProperties;
import android.sysprop.HdmiProperties;
import android.util.Slog;
import com.android.internal.app.LocalePicker;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.hdmi.DeviceDiscoveryAction;
import com.android.server.hdmi.HdmiAnnotations;
import com.android.server.hdmi.HdmiCecLocalDevice;
import com.android.server.hdmi.HdmiControlService;
import com.android.server.location.gnss.hal.GnssNative;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ArrayBlockingQueue;
/* loaded from: classes.dex */
public class HdmiCecLocalDevicePlayback extends HdmiCecLocalDeviceSource {
    static final long STANDBY_AFTER_HOTPLUG_OUT_DELAY_MS = 30000;
    private static final String TAG = "HdmiCecLocalDevicePlayback";
    private Handler mDelayedStandbyHandler;
    protected HdmiProperties.playback_device_action_on_routing_control_values mPlaybackDeviceActionOnRoutingControl;
    private ActiveWakeLock mWakeLock;

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public interface ActiveWakeLock {
        void acquire();

        boolean isHeld();

        void release();
    }

    @Override // com.android.server.hdmi.HdmiCecLocalDevice
    public /* bridge */ /* synthetic */ ArrayBlockingQueue getActiveSourceHistory() {
        return super.getActiveSourceHistory();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public HdmiCecLocalDevicePlayback(HdmiControlService service) {
        super(service, 4);
        this.mPlaybackDeviceActionOnRoutingControl = (HdmiProperties.playback_device_action_on_routing_control_values) HdmiProperties.playback_device_action_on_routing_control().orElse(HdmiProperties.playback_device_action_on_routing_control_values.NONE);
        this.mDelayedStandbyHandler = new Handler(service.getServiceLooper());
        this.mStandbyHandler = new HdmiCecStandbyModeHandler(service, this);
    }

    @Override // com.android.server.hdmi.HdmiCecLocalDevice
    @HdmiAnnotations.ServiceThreadOnly
    protected void onAddressAllocated(int logicalAddress, int reason) {
        assertRunOnServiceThread();
        HdmiControlService hdmiControlService = this.mService;
        if (reason == 0) {
            this.mService.setAndBroadcastActiveSource(this.mService.getPhysicalAddress(), getDeviceInfo().getDeviceType(), 15, "HdmiCecLocalDevicePlayback#onAddressAllocated()");
        }
        this.mService.sendCecCommand(HdmiCecMessageBuilder.buildReportPhysicalAddressCommand(getDeviceInfo().getLogicalAddress(), this.mService.getPhysicalAddress(), this.mDeviceType));
        this.mService.sendCecCommand(HdmiCecMessageBuilder.buildDeviceVendorIdCommand(getDeviceInfo().getLogicalAddress(), this.mService.getVendorId()));
        buildAndSendSetOsdName(0);
        if (this.mService.audioSystem() == null) {
            this.mService.sendCecCommand(HdmiCecMessageBuilder.buildGiveSystemAudioModeStatus(getDeviceInfo().getLogicalAddress(), 5), new HdmiControlService.SendMessageCallback() { // from class: com.android.server.hdmi.HdmiCecLocalDevicePlayback.1
                @Override // com.android.server.hdmi.HdmiControlService.SendMessageCallback
                public void onSendCompleted(int error) {
                    if (error != 0) {
                        HdmiLogger.debug("AVR did not respond to <Give System Audio Mode Status>", new Object[0]);
                        HdmiCecLocalDevicePlayback.this.mService.setSystemAudioActivated(false);
                    }
                }
            });
        }
        launchDeviceDiscovery();
        startQueuedActions();
    }

    @HdmiAnnotations.ServiceThreadOnly
    private void launchDeviceDiscovery() {
        assertRunOnServiceThread();
        clearDeviceInfoList();
        DeviceDiscoveryAction action = new DeviceDiscoveryAction(this, new DeviceDiscoveryAction.DeviceDiscoveryCallback() { // from class: com.android.server.hdmi.HdmiCecLocalDevicePlayback.2
            @Override // com.android.server.hdmi.DeviceDiscoveryAction.DeviceDiscoveryCallback
            public void onDeviceDiscoveryDone(List<HdmiDeviceInfo> deviceInfos) {
                for (HdmiDeviceInfo info : deviceInfos) {
                    HdmiCecLocalDevicePlayback.this.mService.getHdmiCecNetwork().addCecDevice(info);
                }
                for (HdmiCecLocalDevice device : HdmiCecLocalDevicePlayback.this.mService.getAllLocalDevices()) {
                    synchronized (device.mLock) {
                        HdmiCecLocalDevicePlayback.this.mService.getHdmiCecNetwork().addCecDevice(device.getDeviceInfo());
                    }
                }
                List<HotplugDetectionAction> hotplugActions = HdmiCecLocalDevicePlayback.this.getActions(HotplugDetectionAction.class);
                if (hotplugActions.isEmpty()) {
                    HdmiCecLocalDevicePlayback hdmiCecLocalDevicePlayback = HdmiCecLocalDevicePlayback.this;
                    hdmiCecLocalDevicePlayback.addAndStartAction(new HotplugDetectionAction(hdmiCecLocalDevicePlayback));
                }
            }
        });
        addAndStartAction(action);
    }

    @Override // com.android.server.hdmi.HdmiCecLocalDevice
    @HdmiAnnotations.ServiceThreadOnly
    protected int getPreferredAddress() {
        assertRunOnServiceThread();
        return SystemProperties.getInt("persist.sys.hdmi.addr.playback", 15);
    }

    @Override // com.android.server.hdmi.HdmiCecLocalDevice
    @HdmiAnnotations.ServiceThreadOnly
    protected void setPreferredAddress(int addr) {
        assertRunOnServiceThread();
        this.mService.writeStringSystemProperty("persist.sys.hdmi.addr.playback", String.valueOf(addr));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @HdmiAnnotations.ServiceThreadOnly
    public void deviceSelect(int id, IHdmiControlCallback callback) {
        assertRunOnServiceThread();
        synchronized (this.mLock) {
            if (id == getDeviceInfo().getId()) {
                this.mService.oneTouchPlay(callback);
                return;
            }
            HdmiDeviceInfo targetDevice = this.mService.getHdmiCecNetwork().getDeviceInfo(id);
            if (targetDevice == null) {
                invokeCallback(callback, 3);
                return;
            }
            int targetAddress = targetDevice.getLogicalAddress();
            if (isAlreadyActiveSource(targetDevice, targetAddress, callback)) {
                return;
            }
            if (!this.mService.isControlEnabled()) {
                setActiveSource(targetDevice, "HdmiCecLocalDevicePlayback#deviceSelect()");
                invokeCallback(callback, 6);
                return;
            }
            removeAction(DeviceSelectActionFromPlayback.class);
            addAndStartAction(new DeviceSelectActionFromPlayback(this, targetDevice, callback));
        }
    }

    @Override // com.android.server.hdmi.HdmiCecLocalDeviceSource, com.android.server.hdmi.HdmiCecLocalDevice
    @HdmiAnnotations.ServiceThreadOnly
    void onHotplug(int portId, boolean connected) {
        assertRunOnServiceThread();
        this.mCecMessageCache.flushAll();
        if (connected) {
            this.mDelayedStandbyHandler.removeCallbacksAndMessages(null);
            return;
        }
        getWakeLock().release();
        this.mService.getHdmiCecNetwork().removeDevicesConnectedToPort(portId);
        this.mDelayedStandbyHandler.removeCallbacksAndMessages(null);
        this.mDelayedStandbyHandler.postDelayed(new DelayedStandbyRunnable(), 30000L);
    }

    /* loaded from: classes.dex */
    private class DelayedStandbyRunnable implements Runnable {
        private DelayedStandbyRunnable() {
        }

        @Override // java.lang.Runnable
        public void run() {
            if (HdmiCecLocalDevicePlayback.this.mService.getPowerManagerInternal().wasDeviceIdleFor(30000L)) {
                HdmiCecLocalDevicePlayback.this.mService.standby();
            } else {
                HdmiCecLocalDevicePlayback.this.mDelayedStandbyHandler.postDelayed(new DelayedStandbyRunnable(), 30000L);
            }
        }
    }

    @Override // com.android.server.hdmi.HdmiCecLocalDevice
    @HdmiAnnotations.ServiceThreadOnly
    protected void onStandby(boolean initiatedByCec, int standbyAction) {
        assertRunOnServiceThread();
        if (!this.mService.isControlEnabled()) {
            return;
        }
        boolean wasActiveSource = isActiveSource();
        char c = 65535;
        this.mService.setActiveSource(-1, GnssNative.GNSS_AIDING_TYPE_ALL, "HdmiCecLocalDevicePlayback#onStandby()");
        if (!wasActiveSource) {
            return;
        }
        if (initiatedByCec) {
            this.mService.sendCecCommand(HdmiCecMessageBuilder.buildInactiveSource(getDeviceInfo().getLogicalAddress(), this.mService.getPhysicalAddress()));
            return;
        }
        switch (standbyAction) {
            case 0:
                String powerControlMode = this.mService.getHdmiCecConfig().getStringValue("power_control_mode");
                switch (powerControlMode.hashCode()) {
                    case -1744153479:
                        if (powerControlMode.equals("to_tv_and_audio_system")) {
                            c = 1;
                            break;
                        }
                        break;
                    case -1618876223:
                        if (powerControlMode.equals("broadcast")) {
                            c = 2;
                            break;
                        }
                        break;
                    case 3387192:
                        if (powerControlMode.equals("none")) {
                            c = 3;
                            break;
                        }
                        break;
                    case 110530246:
                        if (powerControlMode.equals("to_tv")) {
                            c = 0;
                            break;
                        }
                        break;
                }
                switch (c) {
                    case 0:
                        this.mService.sendCecCommand(HdmiCecMessageBuilder.buildStandby(getDeviceInfo().getLogicalAddress(), 0));
                        return;
                    case 1:
                        this.mService.sendCecCommand(HdmiCecMessageBuilder.buildStandby(getDeviceInfo().getLogicalAddress(), 0));
                        this.mService.sendCecCommand(HdmiCecMessageBuilder.buildStandby(getDeviceInfo().getLogicalAddress(), 5));
                        return;
                    case 2:
                        this.mService.sendCecCommand(HdmiCecMessageBuilder.buildStandby(getDeviceInfo().getLogicalAddress(), 15));
                        return;
                    case 3:
                        this.mService.sendCecCommand(HdmiCecMessageBuilder.buildInactiveSource(getDeviceInfo().getLogicalAddress(), this.mService.getPhysicalAddress()));
                        return;
                    default:
                        return;
                }
            case 1:
                this.mService.sendCecCommand(HdmiCecMessageBuilder.buildStandby(getDeviceInfo().getLogicalAddress(), 15));
                return;
            default:
                return;
        }
    }

    @Override // com.android.server.hdmi.HdmiCecLocalDevice
    @HdmiAnnotations.ServiceThreadOnly
    protected void onInitializeCecComplete(int initiatedBy) {
        if (initiatedBy == 2) {
            oneTouchPlay(new IHdmiControlCallback.Stub() { // from class: com.android.server.hdmi.HdmiCecLocalDevicePlayback.3
                public void onComplete(int result) {
                    if (result != 0) {
                        Slog.w(HdmiCecLocalDevicePlayback.TAG, "Failed to complete One Touch Play. result=" + result);
                    }
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.hdmi.HdmiCecLocalDeviceSource, com.android.server.hdmi.HdmiCecLocalDevice
    @HdmiAnnotations.ServiceThreadOnly
    public void setActiveSource(int logicalAddress, int physicalAddress, String caller) {
        assertRunOnServiceThread();
        super.setActiveSource(logicalAddress, physicalAddress, caller);
        if (isActiveSource()) {
            getWakeLock().acquire();
        } else {
            getWakeLock().release();
        }
    }

    @HdmiAnnotations.ServiceThreadOnly
    private ActiveWakeLock getWakeLock() {
        assertRunOnServiceThread();
        if (this.mWakeLock == null) {
            if (SystemProperties.getBoolean("persist.sys.hdmi.keep_awake", true)) {
                this.mWakeLock = new SystemWakeLock();
            } else {
                this.mWakeLock = new ActiveWakeLock() { // from class: com.android.server.hdmi.HdmiCecLocalDevicePlayback.4
                    @Override // com.android.server.hdmi.HdmiCecLocalDevicePlayback.ActiveWakeLock
                    public void acquire() {
                    }

                    @Override // com.android.server.hdmi.HdmiCecLocalDevicePlayback.ActiveWakeLock
                    public void release() {
                    }

                    @Override // com.android.server.hdmi.HdmiCecLocalDevicePlayback.ActiveWakeLock
                    public boolean isHeld() {
                        return false;
                    }
                };
                HdmiLogger.debug("No wakelock is used to keep the display on.", new Object[0]);
            }
        }
        return this.mWakeLock;
    }

    @Override // com.android.server.hdmi.HdmiCecLocalDevice
    protected boolean canGoToStandby() {
        return !getWakeLock().isHeld();
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    @Override // com.android.server.hdmi.HdmiCecLocalDeviceSource
    @HdmiAnnotations.ServiceThreadOnly
    protected void onActiveSourceLost() {
        char c;
        assertRunOnServiceThread();
        this.mService.pauseActiveMediaSessions();
        String stringValue = this.mService.getHdmiCecConfig().getStringValue("power_state_change_on_active_source_lost");
        switch (stringValue.hashCode()) {
            case -1129124284:
                if (stringValue.equals("standby_now")) {
                    c = 0;
                    break;
                }
                c = 65535;
                break;
            case 3387192:
                if (stringValue.equals("none")) {
                    c = 1;
                    break;
                }
                c = 65535;
                break;
            default:
                c = 65535;
                break;
        }
        switch (c) {
            case 0:
                this.mService.standby();
                return;
            case 1:
                return;
            default:
                return;
        }
    }

    @Override // com.android.server.hdmi.HdmiCecLocalDevice
    @HdmiAnnotations.ServiceThreadOnly
    protected int handleUserControlPressed(HdmiCecMessage message) {
        assertRunOnServiceThread();
        wakeUpIfActiveSource();
        return super.handleUserControlPressed(message);
    }

    @Override // com.android.server.hdmi.HdmiCecLocalDevice
    @HdmiAnnotations.ServiceThreadOnly
    protected int handleSetMenuLanguage(HdmiCecMessage message) {
        assertRunOnServiceThread();
        if (this.mService.getHdmiCecConfig().getIntValue("set_menu_language") == 0) {
            return 0;
        }
        try {
            String iso3Language = new String(message.getParams(), 0, 3, "US-ASCII");
            Locale currentLocale = this.mService.getContext().getResources().getConfiguration().locale;
            HdmiControlService hdmiControlService = this.mService;
            String curIso3Language = HdmiControlService.localeToMenuLanguage(currentLocale);
            HdmiLogger.debug("handleSetMenuLanguage " + iso3Language + " cur:" + curIso3Language, new Object[0]);
            if (curIso3Language.equals(iso3Language)) {
                return -1;
            }
            List<LocalePicker.LocaleInfo> localeInfos = LocalePicker.getAllAssetLocales(this.mService.getContext(), false);
            for (LocalePicker.LocaleInfo localeInfo : localeInfos) {
                HdmiControlService hdmiControlService2 = this.mService;
                if (HdmiControlService.localeToMenuLanguage(localeInfo.getLocale()).equals(iso3Language)) {
                    startSetMenuLanguageActivity(localeInfo.getLocale());
                    return -1;
                }
            }
            Slog.w(TAG, "Can't handle <Set Menu Language> of " + iso3Language);
            return 3;
        } catch (UnsupportedEncodingException e) {
            Slog.w(TAG, "Can't handle <Set Menu Language>", e);
            return 3;
        }
    }

    private void startSetMenuLanguageActivity(Locale locale) {
        long identity = Binder.clearCallingIdentity();
        try {
            try {
                Context context = this.mService.getContext();
                Intent intent = new Intent();
                intent.putExtra("android.hardware.hdmi.extra.LOCALE", locale.toLanguageTag());
                intent.setComponent(ComponentName.unflattenFromString(context.getResources().getString(17039981)));
                intent.addFlags(268435456);
                context.startActivityAsUser(intent, context.getUser());
            } catch (ActivityNotFoundException e) {
                Slog.e(TAG, "unable to start HdmiCecSetMenuLanguageActivity");
            }
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    @Override // com.android.server.hdmi.HdmiCecLocalDevice
    protected int handleSetSystemAudioMode(HdmiCecMessage message) {
        boolean setSystemAudioModeOn;
        if (message.getDestination() == 15 && message.getSource() == 5 && this.mService.audioSystem() == null && this.mService.isSystemAudioActivated() != (setSystemAudioModeOn = HdmiUtils.parseCommandParamSystemAudioStatus(message))) {
            this.mService.setSystemAudioActivated(setSystemAudioModeOn);
        }
        return -1;
    }

    @Override // com.android.server.hdmi.HdmiCecLocalDevice
    protected int handleSystemAudioModeStatus(HdmiCecMessage message) {
        boolean setSystemAudioModeOn;
        if (message.getDestination() == getDeviceInfo().getLogicalAddress() && message.getSource() == 5 && this.mService.isSystemAudioActivated() != (setSystemAudioModeOn = HdmiUtils.parseCommandParamSystemAudioStatus(message))) {
            this.mService.setSystemAudioActivated(setSystemAudioModeOn);
            return -1;
        }
        return -1;
    }

    @Override // com.android.server.hdmi.HdmiCecLocalDeviceSource, com.android.server.hdmi.HdmiCecLocalDevice
    @HdmiAnnotations.ServiceThreadOnly
    protected int handleRoutingChange(HdmiCecMessage message) {
        assertRunOnServiceThread();
        int physicalAddress = HdmiUtils.twoBytesToInt(message.getParams(), 2);
        handleRoutingChangeAndInformation(physicalAddress, message);
        return -1;
    }

    @Override // com.android.server.hdmi.HdmiCecLocalDeviceSource, com.android.server.hdmi.HdmiCecLocalDevice
    @HdmiAnnotations.ServiceThreadOnly
    protected int handleRoutingInformation(HdmiCecMessage message) {
        assertRunOnServiceThread();
        int physicalAddress = HdmiUtils.twoBytesToInt(message.getParams());
        handleRoutingChangeAndInformation(physicalAddress, message);
        return -1;
    }

    @Override // com.android.server.hdmi.HdmiCecLocalDeviceSource
    @HdmiAnnotations.ServiceThreadOnly
    protected void handleRoutingChangeAndInformation(int physicalAddress, HdmiCecMessage message) {
        assertRunOnServiceThread();
        if (physicalAddress != this.mService.getPhysicalAddress()) {
            setActiveSource(physicalAddress, "HdmiCecLocalDevicePlayback#handleRoutingChangeAndInformation()");
            return;
        }
        if (!isActiveSource()) {
            setActiveSource(physicalAddress, "HdmiCecLocalDevicePlayback#handleRoutingChangeAndInformation()");
        }
        switch (AnonymousClass5.$SwitchMap$android$sysprop$HdmiProperties$playback_device_action_on_routing_control_values[this.mPlaybackDeviceActionOnRoutingControl.ordinal()]) {
            case 1:
                setAndBroadcastActiveSource(message, physicalAddress, "HdmiCecLocalDevicePlayback#handleRoutingChangeAndInformation()");
                return;
            case 2:
                this.mService.wakeUp();
                return;
            default:
                return;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: com.android.server.hdmi.HdmiCecLocalDevicePlayback$5  reason: invalid class name */
    /* loaded from: classes.dex */
    public static /* synthetic */ class AnonymousClass5 {
        static final /* synthetic */ int[] $SwitchMap$android$sysprop$HdmiProperties$playback_device_action_on_routing_control_values;

        static {
            int[] iArr = new int[HdmiProperties.playback_device_action_on_routing_control_values.values().length];
            $SwitchMap$android$sysprop$HdmiProperties$playback_device_action_on_routing_control_values = iArr;
            try {
                iArr[HdmiProperties.playback_device_action_on_routing_control_values.WAKE_UP_AND_SEND_ACTIVE_SOURCE.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$android$sysprop$HdmiProperties$playback_device_action_on_routing_control_values[HdmiProperties.playback_device_action_on_routing_control_values.WAKE_UP_ONLY.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$android$sysprop$HdmiProperties$playback_device_action_on_routing_control_values[HdmiProperties.playback_device_action_on_routing_control_values.NONE.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
        }
    }

    @Override // com.android.server.hdmi.HdmiCecLocalDevice
    protected int findKeyReceiverAddress() {
        return 0;
    }

    @Override // com.android.server.hdmi.HdmiCecLocalDevice
    protected int findAudioReceiverAddress() {
        if (this.mService.isSystemAudioActivated()) {
            return 5;
        }
        return 0;
    }

    @Override // com.android.server.hdmi.HdmiCecLocalDeviceSource, com.android.server.hdmi.HdmiCecLocalDevice
    @HdmiAnnotations.ServiceThreadOnly
    protected void disableDevice(boolean initiatedByCec, HdmiCecLocalDevice.PendingActionClearedCallback callback) {
        assertRunOnServiceThread();
        removeAction(DeviceDiscoveryAction.class);
        removeAction(HotplugDetectionAction.class);
        removeAction(NewDeviceAction.class);
        super.disableDevice(initiatedByCec, callback);
        clearDeviceInfoList();
        checkIfPendingActionsCleared();
    }

    @Override // com.android.server.hdmi.HdmiCecLocalDevice
    protected void dump(IndentingPrintWriter pw) {
        super.dump(pw);
        pw.println("isActiveSource(): " + isActiveSource());
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class SystemWakeLock implements ActiveWakeLock {
        private final PowerManager.WakeLock mWakeLock;

        public SystemWakeLock() {
            PowerManager.WakeLock newWakeLock = HdmiCecLocalDevicePlayback.this.mService.getPowerManager().newWakeLock(1, HdmiCecLocalDevicePlayback.TAG);
            this.mWakeLock = newWakeLock;
            newWakeLock.setReferenceCounted(false);
        }

        @Override // com.android.server.hdmi.HdmiCecLocalDevicePlayback.ActiveWakeLock
        public void acquire() {
            this.mWakeLock.acquire();
            HdmiLogger.debug("active source: %b. Wake lock acquired", Boolean.valueOf(HdmiCecLocalDevicePlayback.this.isActiveSource()));
        }

        @Override // com.android.server.hdmi.HdmiCecLocalDevicePlayback.ActiveWakeLock
        public void release() {
            this.mWakeLock.release();
            HdmiLogger.debug("Wake lock released", new Object[0]);
        }

        @Override // com.android.server.hdmi.HdmiCecLocalDevicePlayback.ActiveWakeLock
        public boolean isHeld() {
            return this.mWakeLock.isHeld();
        }
    }
}
