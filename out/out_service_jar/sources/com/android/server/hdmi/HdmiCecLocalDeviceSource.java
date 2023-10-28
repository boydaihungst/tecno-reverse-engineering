package com.android.server.hdmi;

import android.hardware.hdmi.IHdmiControlCallback;
import android.sysprop.HdmiProperties;
import android.util.Slog;
import com.android.server.hdmi.HdmiAnnotations;
import com.android.server.hdmi.HdmiCecLocalDevice;
import java.util.ArrayList;
import java.util.List;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public abstract class HdmiCecLocalDeviceSource extends HdmiCecLocalDevice {
    private static final String TAG = "HdmiCecLocalDeviceSource";
    protected boolean mIsSwitchDevice;
    protected int mLocalActivePort;
    protected boolean mRoutingControlFeatureEnabled;
    private int mRoutingPort;

    /* JADX INFO: Access modifiers changed from: protected */
    public HdmiCecLocalDeviceSource(HdmiControlService service, int deviceType) {
        super(service, deviceType);
        this.mIsSwitchDevice = ((Boolean) HdmiProperties.is_switch().orElse(false)).booleanValue();
        this.mRoutingPort = 0;
        this.mLocalActivePort = 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @HdmiAnnotations.ServiceThreadOnly
    public void queryDisplayStatus(IHdmiControlCallback callback) {
        assertRunOnServiceThread();
        List<DevicePowerStatusAction> actions = getActions(DevicePowerStatusAction.class);
        if (!actions.isEmpty()) {
            Slog.i(TAG, "queryDisplayStatus already in progress");
            actions.get(0).addCallback(callback);
            return;
        }
        DevicePowerStatusAction action = DevicePowerStatusAction.create(this, 0, callback);
        if (action == null) {
            Slog.w(TAG, "Cannot initiate queryDisplayStatus");
            invokeCallback(callback, 5);
            return;
        }
        addAndStartAction(action);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.hdmi.HdmiCecLocalDevice
    @HdmiAnnotations.ServiceThreadOnly
    public void onHotplug(int portId, boolean connected) {
        assertRunOnServiceThread();
        if (this.mService.getPortInfo(portId).getType() == 1) {
            this.mCecMessageCache.flushAll();
        }
        if (connected) {
            this.mService.wakeUp();
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.hdmi.HdmiCecLocalDevice
    @HdmiAnnotations.ServiceThreadOnly
    public void sendStandby(int deviceId) {
        assertRunOnServiceThread();
        String powerControlMode = this.mService.getHdmiCecConfig().getStringValue("power_control_mode");
        if (powerControlMode.equals("broadcast")) {
            this.mService.sendCecCommand(HdmiCecMessageBuilder.buildStandby(getDeviceInfo().getLogicalAddress(), 15));
            return;
        }
        this.mService.sendCecCommand(HdmiCecMessageBuilder.buildStandby(getDeviceInfo().getLogicalAddress(), 0));
        if (powerControlMode.equals("to_tv_and_audio_system")) {
            this.mService.sendCecCommand(HdmiCecMessageBuilder.buildStandby(getDeviceInfo().getLogicalAddress(), 5));
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @HdmiAnnotations.ServiceThreadOnly
    public void oneTouchPlay(IHdmiControlCallback callback) {
        assertRunOnServiceThread();
        List<OneTouchPlayAction> actions = getActions(OneTouchPlayAction.class);
        if (!actions.isEmpty()) {
            Slog.i(TAG, "oneTouchPlay already in progress");
            actions.get(0).addCallback(callback);
            return;
        }
        OneTouchPlayAction action = OneTouchPlayAction.create(this, 0, callback);
        if (action == null) {
            Slog.w(TAG, "Cannot initiate oneTouchPlay");
            invokeCallback(callback, 5);
            return;
        }
        addAndStartAction(action);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @HdmiAnnotations.ServiceThreadOnly
    public void toggleAndFollowTvPower() {
        assertRunOnServiceThread();
        if (this.mService.getPowerManager().isInteractive()) {
            this.mService.pauseActiveMediaSessions();
        } else {
            this.mService.wakeUp();
        }
        this.mService.queryDisplayStatus(new IHdmiControlCallback.Stub() { // from class: com.android.server.hdmi.HdmiCecLocalDeviceSource.1
            public void onComplete(int status) {
                if (status == -1) {
                    Slog.i(HdmiCecLocalDeviceSource.TAG, "TV power toggle: TV power status unknown");
                    HdmiCecLocalDeviceSource.this.sendUserControlPressedAndReleased(0, 107);
                } else if (status == 0 || status == 2) {
                    Slog.i(HdmiCecLocalDeviceSource.TAG, "TV power toggle: turning off TV");
                    HdmiCecLocalDeviceSource.this.sendStandby(0);
                    HdmiCecLocalDeviceSource.this.mService.standby();
                } else if (status == 1 || status == 3) {
                    Slog.i(HdmiCecLocalDeviceSource.TAG, "TV power toggle: turning on TV");
                    HdmiCecLocalDeviceSource.this.oneTouchPlay(new IHdmiControlCallback.Stub() { // from class: com.android.server.hdmi.HdmiCecLocalDeviceSource.1.1
                        public void onComplete(int result) {
                            if (result != 0) {
                                Slog.w(HdmiCecLocalDeviceSource.TAG, "Failed to complete One Touch Play. result=" + result);
                                HdmiCecLocalDeviceSource.this.sendUserControlPressedAndReleased(0, 107);
                            }
                        }
                    });
                }
            }
        });
    }

    @HdmiAnnotations.ServiceThreadOnly
    protected void onActiveSourceLost() {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.hdmi.HdmiCecLocalDevice
    @HdmiAnnotations.ServiceThreadOnly
    public void setActiveSource(int logicalAddress, int physicalAddress, String caller) {
        boolean wasActiveSource = isActiveSource();
        super.setActiveSource(logicalAddress, physicalAddress, caller);
        if (wasActiveSource && !isActiveSource()) {
            onActiveSourceLost();
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @HdmiAnnotations.ServiceThreadOnly
    public void setActiveSource(int physicalAddress, String caller) {
        assertRunOnServiceThread();
        HdmiCecLocalDevice.ActiveSource activeSource = HdmiCecLocalDevice.ActiveSource.of(-1, physicalAddress);
        setActiveSource(activeSource, caller);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.hdmi.HdmiCecLocalDevice
    @HdmiAnnotations.ServiceThreadOnly
    public int handleActiveSource(HdmiCecMessage message) {
        assertRunOnServiceThread();
        int logicalAddress = message.getSource();
        int physicalAddress = HdmiUtils.twoBytesToInt(message.getParams());
        HdmiCecLocalDevice.ActiveSource activeSource = HdmiCecLocalDevice.ActiveSource.of(logicalAddress, physicalAddress);
        if (!getActiveSource().equals(activeSource)) {
            setActiveSource(activeSource, "HdmiCecLocalDeviceSource#handleActiveSource()");
        }
        updateDevicePowerStatus(logicalAddress, 0);
        if (isRoutingControlFeatureEnabled()) {
            switchInputOnReceivingNewActivePath(physicalAddress);
            return -1;
        }
        return -1;
    }

    @Override // com.android.server.hdmi.HdmiCecLocalDevice
    @HdmiAnnotations.ServiceThreadOnly
    protected int handleRequestActiveSource(HdmiCecMessage message) {
        assertRunOnServiceThread();
        maySendActiveSource(message.getSource());
        return -1;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.hdmi.HdmiCecLocalDevice
    @HdmiAnnotations.ServiceThreadOnly
    public int handleSetStreamPath(HdmiCecMessage message) {
        assertRunOnServiceThread();
        int physicalAddress = HdmiUtils.twoBytesToInt(message.getParams());
        if (physicalAddress == this.mService.getPhysicalAddress() && this.mService.isPlaybackDevice()) {
            setAndBroadcastActiveSource(message, physicalAddress, "HdmiCecLocalDeviceSource#handleSetStreamPath()");
        } else if (physicalAddress != this.mService.getPhysicalAddress() || !isActiveSource()) {
            setActiveSource(physicalAddress, "HdmiCecLocalDeviceSource#handleSetStreamPath()");
        }
        switchInputOnReceivingNewActivePath(physicalAddress);
        return -1;
    }

    @Override // com.android.server.hdmi.HdmiCecLocalDevice
    @HdmiAnnotations.ServiceThreadOnly
    protected int handleRoutingChange(HdmiCecMessage message) {
        assertRunOnServiceThread();
        int physicalAddress = HdmiUtils.twoBytesToInt(message.getParams(), 2);
        if (physicalAddress != this.mService.getPhysicalAddress() || !isActiveSource()) {
            setActiveSource(physicalAddress, "HdmiCecLocalDeviceSource#handleRoutingChange()");
        }
        if (!isRoutingControlFeatureEnabled()) {
            return 4;
        }
        handleRoutingChangeAndInformation(physicalAddress, message);
        return -1;
    }

    @Override // com.android.server.hdmi.HdmiCecLocalDevice
    @HdmiAnnotations.ServiceThreadOnly
    protected int handleRoutingInformation(HdmiCecMessage message) {
        assertRunOnServiceThread();
        int physicalAddress = HdmiUtils.twoBytesToInt(message.getParams());
        if (physicalAddress != this.mService.getPhysicalAddress() || !isActiveSource()) {
            setActiveSource(physicalAddress, "HdmiCecLocalDeviceSource#handleRoutingInformation()");
        }
        if (!isRoutingControlFeatureEnabled()) {
            return 4;
        }
        handleRoutingChangeAndInformation(physicalAddress, message);
        return -1;
    }

    protected void switchInputOnReceivingNewActivePath(int physicalAddress) {
    }

    protected void handleRoutingChangeAndInformation(int physicalAddress, HdmiCecMessage message) {
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.hdmi.HdmiCecLocalDevice
    @HdmiAnnotations.ServiceThreadOnly
    public void disableDevice(boolean initiatedByCec, HdmiCecLocalDevice.PendingActionClearedCallback callback) {
        removeAction(OneTouchPlayAction.class);
        removeAction(DevicePowerStatusAction.class);
        removeAction(AbsoluteVolumeAudioStatusAction.class);
        super.disableDevice(initiatedByCec, callback);
    }

    protected void updateDevicePowerStatus(int logicalAddress, int newPowerStatus) {
    }

    @Override // com.android.server.hdmi.HdmiCecLocalDevice
    protected int getRcProfile() {
        return 1;
    }

    @Override // com.android.server.hdmi.HdmiCecLocalDevice
    protected List<Integer> getRcFeatures() {
        List<Integer> features = new ArrayList<>();
        HdmiCecConfig hdmiCecConfig = this.mService.getHdmiCecConfig();
        if (hdmiCecConfig.getIntValue("rc_profile_source_handles_root_menu") == 1) {
            features.add(4);
        }
        if (hdmiCecConfig.getIntValue("rc_profile_source_handles_setup_menu") == 1) {
            features.add(3);
        }
        if (hdmiCecConfig.getIntValue("rc_profile_source_handles_contents_menu") == 1) {
            features.add(2);
        }
        if (hdmiCecConfig.getIntValue("rc_profile_source_handles_top_menu") == 1) {
            features.add(1);
        }
        if (hdmiCecConfig.getIntValue("rc_profile_source_handles_media_context_sensitive_menu") == 1) {
            features.add(0);
        }
        return features;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void setAndBroadcastActiveSource(HdmiCecMessage message, int physicalAddress, String caller) {
        this.mService.setAndBroadcastActiveSource(physicalAddress, getDeviceInfo().getDeviceType(), message.getSource(), caller);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @HdmiAnnotations.ServiceThreadOnly
    public boolean isActiveSource() {
        if (getDeviceInfo() == null) {
            return false;
        }
        return getActiveSource().equals(getDeviceInfo().getLogicalAddress(), getDeviceInfo().getPhysicalAddress());
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void wakeUpIfActiveSource() {
        if (!isActiveSource()) {
            return;
        }
        this.mService.wakeUp();
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void maySendActiveSource(int dest) {
        if (!isActiveSource()) {
            return;
        }
        addAndStartAction(new ActiveSourceAction(this, dest));
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void setRoutingPort(int portId) {
        synchronized (this.mLock) {
            this.mRoutingPort = portId;
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public int getRoutingPort() {
        int i;
        synchronized (this.mLock) {
            i = this.mRoutingPort;
        }
        return i;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public int getLocalActivePort() {
        int i;
        synchronized (this.mLock) {
            i = this.mLocalActivePort;
        }
        return i;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void setLocalActivePort(int activePort) {
        synchronized (this.mLock) {
            this.mLocalActivePort = activePort;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isRoutingControlFeatureEnabled() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mRoutingControlFeatureEnabled;
        }
        return z;
    }

    protected boolean isSwitchingToTheSameInput(int activePort) {
        return activePort == getLocalActivePort();
    }
}
