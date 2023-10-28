package com.android.server.hdmi;

import android.hardware.hdmi.DeviceFeatures;
import android.hardware.hdmi.HdmiDeviceInfo;
import android.hardware.hdmi.HdmiPortInfo;
import android.hardware.hdmi.HdmiRecordSources;
import android.hardware.hdmi.HdmiTimerRecordSources;
import android.hardware.hdmi.IHdmiControlCallback;
import android.media.AudioDescriptor;
import android.media.AudioDeviceAttributes;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.util.Slog;
import android.util.SparseBooleanArray;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.hdmi.DeviceDiscoveryAction;
import com.android.server.hdmi.HdmiAnnotations;
import com.android.server.hdmi.HdmiCecLocalDevice;
import com.android.server.hdmi.HdmiControlService;
import com.android.server.location.gnss.hal.GnssNative;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public final class HdmiCecLocalDeviceTv extends HdmiCecLocalDevice {
    private static final String TAG = "HdmiCecLocalDeviceTv";
    @HdmiAnnotations.ServiceThreadOnly
    private boolean mArcEstablished;
    private final SparseBooleanArray mArcFeatureEnabled;
    private final DelayedMessageBuffer mDelayedMessageBuffer;
    private int mPrevPortId;
    private SelectRequestBuffer mSelectRequestBuffer;
    private boolean mSkipRoutingControl;
    private boolean mSystemAudioControlFeatureEnabled;
    private boolean mSystemAudioMute;
    private int mSystemAudioVolume;
    private final TvInputManager.TvInputCallback mTvInputCallback;
    private final HashMap<String, Integer> mTvInputs;

    /* JADX INFO: Access modifiers changed from: private */
    @HdmiAnnotations.ServiceThreadOnly
    public void addTvInput(String inputId, int deviceId) {
        assertRunOnServiceThread();
        this.mTvInputs.put(inputId, Integer.valueOf(deviceId));
    }

    /* JADX INFO: Access modifiers changed from: private */
    @HdmiAnnotations.ServiceThreadOnly
    public void removeTvInput(String inputId) {
        assertRunOnServiceThread();
        this.mTvInputs.remove(inputId);
    }

    @Override // com.android.server.hdmi.HdmiCecLocalDevice
    @HdmiAnnotations.ServiceThreadOnly
    protected boolean isInputReady(int deviceId) {
        assertRunOnServiceThread();
        return this.mTvInputs.containsValue(Integer.valueOf(deviceId));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public HdmiCecLocalDeviceTv(HdmiControlService service) {
        super(service, 0);
        this.mArcEstablished = false;
        this.mArcFeatureEnabled = new SparseBooleanArray();
        this.mSystemAudioVolume = -1;
        this.mSystemAudioMute = false;
        this.mDelayedMessageBuffer = new DelayedMessageBuffer(this);
        this.mTvInputCallback = new TvInputManager.TvInputCallback() { // from class: com.android.server.hdmi.HdmiCecLocalDeviceTv.1
            @Override // android.media.tv.TvInputManager.TvInputCallback
            public void onInputAdded(String inputId) {
                HdmiDeviceInfo info;
                TvInputInfo tvInfo = HdmiCecLocalDeviceTv.this.mService.getTvInputManager().getTvInputInfo(inputId);
                if (tvInfo == null || (info = tvInfo.getHdmiDeviceInfo()) == null) {
                    return;
                }
                HdmiCecLocalDeviceTv.this.addTvInput(inputId, info.getId());
                if (info.isCecDevice()) {
                    HdmiCecLocalDeviceTv.this.processDelayedActiveSource(info.getLogicalAddress());
                }
            }

            @Override // android.media.tv.TvInputManager.TvInputCallback
            public void onInputRemoved(String inputId) {
                HdmiCecLocalDeviceTv.this.removeTvInput(inputId);
            }
        };
        this.mTvInputs = new HashMap<>();
        this.mPrevPortId = -1;
        this.mSystemAudioControlFeatureEnabled = service.getHdmiCecConfig().getIntValue("system_audio_control") == 1;
        this.mStandbyHandler = new HdmiCecStandbyModeHandler(service, this);
    }

    @Override // com.android.server.hdmi.HdmiCecLocalDevice
    @HdmiAnnotations.ServiceThreadOnly
    protected void onAddressAllocated(int logicalAddress, int reason) {
        assertRunOnServiceThread();
        List<HdmiPortInfo> ports = this.mService.getPortInfo();
        for (HdmiPortInfo port : ports) {
            this.mArcFeatureEnabled.put(port.getId(), port.isArcSupported());
        }
        this.mService.registerTvInputCallback(this.mTvInputCallback);
        this.mService.sendCecCommand(HdmiCecMessageBuilder.buildReportPhysicalAddressCommand(getDeviceInfo().getLogicalAddress(), this.mService.getPhysicalAddress(), this.mDeviceType));
        this.mService.sendCecCommand(HdmiCecMessageBuilder.buildDeviceVendorIdCommand(getDeviceInfo().getLogicalAddress(), this.mService.getVendorId()));
        this.mService.getHdmiCecNetwork().addCecSwitch(this.mService.getHdmiCecNetwork().getPhysicalAddress());
        this.mTvInputs.clear();
        boolean z = false;
        this.mSkipRoutingControl = reason == 3;
        if (reason != 0 && reason != 1) {
            z = true;
        }
        launchRoutingControl(z);
        resetSelectRequestBuffer();
        launchDeviceDiscovery();
        startQueuedActions();
        if (!this.mDelayedMessageBuffer.isBuffered(130)) {
            this.mService.sendCecCommand(HdmiCecMessageBuilder.buildRequestActiveSource(getDeviceInfo().getLogicalAddress()));
        }
    }

    @HdmiAnnotations.ServiceThreadOnly
    public void setSelectRequestBuffer(SelectRequestBuffer requestBuffer) {
        assertRunOnServiceThread();
        this.mSelectRequestBuffer = requestBuffer;
    }

    /* JADX INFO: Access modifiers changed from: private */
    @HdmiAnnotations.ServiceThreadOnly
    public void resetSelectRequestBuffer() {
        assertRunOnServiceThread();
        setSelectRequestBuffer(SelectRequestBuffer.EMPTY_BUFFER);
    }

    @Override // com.android.server.hdmi.HdmiCecLocalDevice
    protected int getPreferredAddress() {
        return 0;
    }

    @Override // com.android.server.hdmi.HdmiCecLocalDevice
    protected void setPreferredAddress(int addr) {
        Slog.w(TAG, "Preferred addres will not be stored for TV");
    }

    @Override // com.android.server.hdmi.HdmiCecLocalDevice
    @HdmiAnnotations.ServiceThreadOnly
    protected int dispatchMessage(HdmiCecMessage message) {
        assertRunOnServiceThread();
        if (this.mService.isPowerStandby() && !this.mService.isWakeUpMessageReceived() && this.mStandbyHandler.handleCommand(message)) {
            return -1;
        }
        return super.onMessage(message);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @HdmiAnnotations.ServiceThreadOnly
    public void deviceSelect(int id, IHdmiControlCallback callback) {
        assertRunOnServiceThread();
        HdmiDeviceInfo targetDevice = this.mService.getHdmiCecNetwork().getDeviceInfo(id);
        if (targetDevice == null) {
            invokeCallback(callback, 3);
            return;
        }
        int targetAddress = targetDevice.getLogicalAddress();
        if (isAlreadyActiveSource(targetDevice, targetAddress, callback)) {
            return;
        }
        if (targetAddress == 0) {
            handleSelectInternalSource();
            setActiveSource(targetAddress, this.mService.getPhysicalAddress(), "HdmiCecLocalDeviceTv#deviceSelect()");
            setActivePath(this.mService.getPhysicalAddress());
            invokeCallback(callback, 0);
        } else if (!this.mService.isControlEnabled()) {
            setActiveSource(targetDevice, "HdmiCecLocalDeviceTv#deviceSelect()");
            invokeCallback(callback, 6);
        } else {
            removeAction(DeviceSelectActionFromTv.class);
            addAndStartAction(new DeviceSelectActionFromTv(this, targetDevice, callback));
        }
    }

    @HdmiAnnotations.ServiceThreadOnly
    private void handleSelectInternalSource() {
        assertRunOnServiceThread();
        if (this.mService.isControlEnabled() && getActiveSource().logicalAddress != getDeviceInfo().getLogicalAddress()) {
            updateActiveSource(getDeviceInfo().getLogicalAddress(), this.mService.getPhysicalAddress(), "HdmiCecLocalDeviceTv#handleSelectInternalSource()");
            if (this.mSkipRoutingControl) {
                this.mSkipRoutingControl = false;
                return;
            }
            HdmiCecMessage activeSource = HdmiCecMessageBuilder.buildActiveSource(getDeviceInfo().getLogicalAddress(), this.mService.getPhysicalAddress());
            this.mService.sendCecCommand(activeSource);
        }
    }

    @HdmiAnnotations.ServiceThreadOnly
    void updateActiveSource(int logicalAddress, int physicalAddress, String caller) {
        assertRunOnServiceThread();
        updateActiveSource(HdmiCecLocalDevice.ActiveSource.of(logicalAddress, physicalAddress), caller);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @HdmiAnnotations.ServiceThreadOnly
    public void updateActiveSource(HdmiCecLocalDevice.ActiveSource newActive, String caller) {
        assertRunOnServiceThread();
        if (getActiveSource().equals(newActive)) {
            return;
        }
        setActiveSource(newActive, caller);
        int logicalAddress = newActive.logicalAddress;
        if (this.mService.getHdmiCecNetwork().getCecDeviceInfo(logicalAddress) != null && logicalAddress != getDeviceInfo().getLogicalAddress() && this.mService.pathToPortId(newActive.physicalAddress) == getActivePortId()) {
            setPrevPortId(getActivePortId());
        }
    }

    int getPrevPortId() {
        int i;
        synchronized (this.mLock) {
            i = this.mPrevPortId;
        }
        return i;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setPrevPortId(int portId) {
        synchronized (this.mLock) {
            this.mPrevPortId = portId;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @HdmiAnnotations.ServiceThreadOnly
    public void updateActiveInput(int path, boolean notifyInputChange) {
        assertRunOnServiceThread();
        setActivePath(path);
        if (notifyInputChange) {
            HdmiCecLocalDevice.ActiveSource activeSource = getActiveSource();
            HdmiDeviceInfo info = this.mService.getHdmiCecNetwork().getCecDeviceInfo(activeSource.logicalAddress);
            if (info == null && (info = this.mService.getDeviceInfoByPort(getActivePortId())) == null) {
                info = HdmiDeviceInfo.hardwarePort(path, getActivePortId());
            }
            this.mService.invokeInputChangeListener(info);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @HdmiAnnotations.ServiceThreadOnly
    public void doManualPortSwitching(int portId, IHdmiControlCallback callback) {
        assertRunOnServiceThread();
        if (!this.mService.isValidPortId(portId)) {
            invokeCallback(callback, 6);
        } else if (portId == getActivePortId()) {
            invokeCallback(callback, 0);
        } else {
            getActiveSource().invalidate();
            if (!this.mService.isControlEnabled()) {
                setActivePortId(portId);
                invokeCallback(callback, 6);
                return;
            }
            int oldPath = getActivePortId() != -1 ? this.mService.portIdToPath(getActivePortId()) : getDeviceInfo().getPhysicalAddress();
            setActivePath(oldPath);
            if (this.mSkipRoutingControl) {
                this.mSkipRoutingControl = false;
                return;
            }
            int newPath = this.mService.portIdToPath(portId);
            startRoutingControl(oldPath, newPath, callback);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @HdmiAnnotations.ServiceThreadOnly
    public void startRoutingControl(int oldPath, int newPath, IHdmiControlCallback callback) {
        assertRunOnServiceThread();
        if (oldPath == newPath) {
            return;
        }
        HdmiCecMessage routingChange = HdmiCecMessageBuilder.buildRoutingChange(getDeviceInfo().getLogicalAddress(), oldPath, newPath);
        this.mService.sendCecCommand(routingChange);
        removeAction(RoutingControlAction.class);
        addAndStartAction(new RoutingControlAction(this, newPath, callback));
    }

    @HdmiAnnotations.ServiceThreadOnly
    int getPowerStatus() {
        assertRunOnServiceThread();
        return this.mService.getPowerStatus();
    }

    @Override // com.android.server.hdmi.HdmiCecLocalDevice
    protected int findKeyReceiverAddress() {
        if (getActiveSource().isValid()) {
            return getActiveSource().logicalAddress;
        }
        HdmiDeviceInfo info = this.mService.getHdmiCecNetwork().getDeviceInfoByPath(getActivePath());
        if (info != null) {
            return info.getLogicalAddress();
        }
        return -1;
    }

    @Override // com.android.server.hdmi.HdmiCecLocalDevice
    protected int findAudioReceiverAddress() {
        return 5;
    }

    @Override // com.android.server.hdmi.HdmiCecLocalDevice
    @HdmiAnnotations.ServiceThreadOnly
    protected int handleActiveSource(HdmiCecMessage message) {
        assertRunOnServiceThread();
        int logicalAddress = message.getSource();
        int physicalAddress = HdmiUtils.twoBytesToInt(message.getParams());
        HdmiDeviceInfo info = this.mService.getHdmiCecNetwork().getCecDeviceInfo(logicalAddress);
        if (info == null) {
            if (!handleNewDeviceAtTheTailOfActivePath(physicalAddress)) {
                HdmiLogger.debug("Device info %X not found; buffering the command", Integer.valueOf(logicalAddress));
                this.mDelayedMessageBuffer.add(message);
                return -1;
            }
            return -1;
        } else if (isInputReady(info.getId()) || info.getDeviceType() == 5) {
            this.mService.getHdmiCecNetwork().updateDevicePowerStatus(logicalAddress, 0);
            HdmiCecLocalDevice.ActiveSource activeSource = HdmiCecLocalDevice.ActiveSource.of(logicalAddress, physicalAddress);
            ActiveSourceHandler.create(this, null).process(activeSource, info.getDeviceType());
            return -1;
        } else {
            HdmiLogger.debug("Input not ready for device: %X; buffering the command", Integer.valueOf(info.getId()));
            this.mDelayedMessageBuffer.add(message);
            return -1;
        }
    }

    @Override // com.android.server.hdmi.HdmiCecLocalDevice
    @HdmiAnnotations.ServiceThreadOnly
    protected int handleInactiveSource(HdmiCecMessage message) {
        assertRunOnServiceThread();
        if (getActiveSource().logicalAddress == message.getSource() && !isProhibitMode()) {
            int portId = getPrevPortId();
            if (portId != -1) {
                HdmiDeviceInfo inactiveSource = this.mService.getHdmiCecNetwork().getCecDeviceInfo(message.getSource());
                if (inactiveSource == null || this.mService.pathToPortId(inactiveSource.getPhysicalAddress()) == portId) {
                    return -1;
                }
                doManualPortSwitching(portId, null);
                setPrevPortId(-1);
            } else {
                getActiveSource().invalidate();
                setActivePath(GnssNative.GNSS_AIDING_TYPE_ALL);
                this.mService.invokeInputChangeListener(HdmiDeviceInfo.INACTIVE_DEVICE);
            }
            return -1;
        }
        return -1;
    }

    @Override // com.android.server.hdmi.HdmiCecLocalDevice
    @HdmiAnnotations.ServiceThreadOnly
    protected int handleRequestActiveSource(HdmiCecMessage message) {
        assertRunOnServiceThread();
        if (getDeviceInfo().getLogicalAddress() == getActiveSource().logicalAddress) {
            this.mService.sendCecCommand(HdmiCecMessageBuilder.buildActiveSource(getDeviceInfo().getLogicalAddress(), getActivePath()));
            return -1;
        }
        return -1;
    }

    @Override // com.android.server.hdmi.HdmiCecLocalDevice
    @HdmiAnnotations.ServiceThreadOnly
    protected int handleGetMenuLanguage(HdmiCecMessage message) {
        assertRunOnServiceThread();
        if (!broadcastMenuLanguage(this.mService.getLanguage())) {
            Slog.w(TAG, "Failed to respond to <Get Menu Language>: " + message.toString());
            return -1;
        }
        return -1;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @HdmiAnnotations.ServiceThreadOnly
    public boolean broadcastMenuLanguage(String language) {
        assertRunOnServiceThread();
        HdmiCecMessage command = HdmiCecMessageBuilder.buildSetMenuLanguageCommand(getDeviceInfo().getLogicalAddress(), language);
        if (command != null) {
            this.mService.sendCecCommand(command);
            return true;
        }
        return false;
    }

    @Override // com.android.server.hdmi.HdmiCecLocalDevice
    protected int handleReportPhysicalAddress(HdmiCecMessage message) {
        super.handleReportPhysicalAddress(message);
        int path = HdmiUtils.twoBytesToInt(message.getParams());
        int address = message.getSource();
        int type = message.getParams()[2];
        if (!this.mService.getHdmiCecNetwork().isInDeviceList(address, path)) {
            handleNewDeviceAtTheTailOfActivePath(path);
        }
        startNewDeviceAction(HdmiCecLocalDevice.ActiveSource.of(address, path), type);
        return -1;
    }

    @Override // com.android.server.hdmi.HdmiCecLocalDevice
    protected int handleTimerStatus(HdmiCecMessage message) {
        return -1;
    }

    @Override // com.android.server.hdmi.HdmiCecLocalDevice
    protected int handleRecordStatus(HdmiCecMessage message) {
        return -1;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void startNewDeviceAction(HdmiCecLocalDevice.ActiveSource activeSource, int deviceType) {
        for (NewDeviceAction action : getActions(NewDeviceAction.class)) {
            if (action.isActionOf(activeSource)) {
                return;
            }
        }
        addAndStartAction(new NewDeviceAction(this, activeSource.logicalAddress, activeSource.physicalAddress, deviceType));
    }

    private boolean handleNewDeviceAtTheTailOfActivePath(int path) {
        if (isTailOfActivePath(path, getActivePath())) {
            int newPath = this.mService.portIdToPath(getActivePortId());
            setActivePath(newPath);
            startRoutingControl(getActivePath(), newPath, null);
            return true;
        }
        return false;
    }

    static boolean isTailOfActivePath(int path, int activePath) {
        if (activePath == 0) {
            return false;
        }
        for (int i = 12; i >= 0; i -= 4) {
            int curActivePath = (activePath >> i) & 15;
            if (curActivePath == 0) {
                return true;
            }
            int curPath = (path >> i) & 15;
            if (curPath != curActivePath) {
                return false;
            }
        }
        return false;
    }

    @Override // com.android.server.hdmi.HdmiCecLocalDevice
    @HdmiAnnotations.ServiceThreadOnly
    protected int handleRoutingChange(HdmiCecMessage message) {
        assertRunOnServiceThread();
        byte[] params = message.getParams();
        int currentPath = HdmiUtils.twoBytesToInt(params);
        if (HdmiUtils.isAffectingActiveRoutingPath(getActivePath(), currentPath)) {
            getActiveSource().invalidate();
            removeAction(RoutingControlAction.class);
            int newPath = HdmiUtils.twoBytesToInt(params, 2);
            addAndStartAction(new RoutingControlAction(this, newPath, null));
            return -1;
        }
        return -1;
    }

    @Override // com.android.server.hdmi.HdmiCecLocalDevice
    @HdmiAnnotations.ServiceThreadOnly
    protected int handleReportAudioStatus(HdmiCecMessage message) {
        assertRunOnServiceThread();
        if (this.mService.getHdmiCecVolumeControl() == 0) {
            return 4;
        }
        boolean mute = HdmiUtils.isAudioStatusMute(message);
        int volume = HdmiUtils.getAudioStatusVolume(message);
        setAudioStatus(mute, volume);
        return -1;
    }

    @Override // com.android.server.hdmi.HdmiCecLocalDevice
    @HdmiAnnotations.ServiceThreadOnly
    protected int handleTextViewOn(HdmiCecMessage message) {
        assertRunOnServiceThread();
        if (getAutoWakeup()) {
            this.mService.wakeUp();
            return -1;
        }
        return -1;
    }

    @Override // com.android.server.hdmi.HdmiCecLocalDevice
    @HdmiAnnotations.ServiceThreadOnly
    protected int handleImageViewOn(HdmiCecMessage message) {
        assertRunOnServiceThread();
        return handleTextViewOn(message);
    }

    @HdmiAnnotations.ServiceThreadOnly
    private void launchDeviceDiscovery() {
        assertRunOnServiceThread();
        clearDeviceInfoList();
        DeviceDiscoveryAction action = new DeviceDiscoveryAction(this, new DeviceDiscoveryAction.DeviceDiscoveryCallback() { // from class: com.android.server.hdmi.HdmiCecLocalDeviceTv.2
            @Override // com.android.server.hdmi.DeviceDiscoveryAction.DeviceDiscoveryCallback
            public void onDeviceDiscoveryDone(List<HdmiDeviceInfo> deviceInfos) {
                for (HdmiDeviceInfo info : deviceInfos) {
                    HdmiCecLocalDeviceTv.this.mService.getHdmiCecNetwork().addCecDevice(info);
                }
                for (HdmiCecLocalDevice device : HdmiCecLocalDeviceTv.this.mService.getAllLocalDevices()) {
                    HdmiCecLocalDeviceTv.this.mService.getHdmiCecNetwork().addCecDevice(device.getDeviceInfo());
                }
                HdmiCecLocalDeviceTv.this.mSelectRequestBuffer.process();
                HdmiCecLocalDeviceTv.this.resetSelectRequestBuffer();
                List<HotplugDetectionAction> hotplugActions = HdmiCecLocalDeviceTv.this.getActions(HotplugDetectionAction.class);
                if (hotplugActions.isEmpty()) {
                    HdmiCecLocalDeviceTv hdmiCecLocalDeviceTv = HdmiCecLocalDeviceTv.this;
                    hdmiCecLocalDeviceTv.addAndStartAction(new HotplugDetectionAction(hdmiCecLocalDeviceTv));
                }
                List<PowerStatusMonitorAction> powerStatusActions = HdmiCecLocalDeviceTv.this.getActions(PowerStatusMonitorAction.class);
                if (powerStatusActions.isEmpty()) {
                    HdmiCecLocalDeviceTv hdmiCecLocalDeviceTv2 = HdmiCecLocalDeviceTv.this;
                    hdmiCecLocalDeviceTv2.addAndStartAction(new PowerStatusMonitorAction(hdmiCecLocalDeviceTv2));
                }
                HdmiDeviceInfo avr = HdmiCecLocalDeviceTv.this.getAvrDeviceInfo();
                if (avr != null) {
                    HdmiCecLocalDeviceTv.this.onNewAvrAdded(avr);
                } else {
                    HdmiCecLocalDeviceTv.this.setSystemAudioMode(false);
                }
            }
        });
        addAndStartAction(action);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @HdmiAnnotations.ServiceThreadOnly
    public void onNewAvrAdded(HdmiDeviceInfo avr) {
        assertRunOnServiceThread();
        addAndStartAction(new SystemAudioAutoInitiationAction(this, avr.getLogicalAddress()));
        if (!isDirectConnectAddress(avr.getPhysicalAddress())) {
            startArcAction(false);
        } else if (isConnected(avr.getPortId()) && isArcFeatureEnabled(avr.getPortId()) && !hasAction(SetArcTransmissionStateAction.class)) {
            startArcAction(true);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @HdmiAnnotations.ServiceThreadOnly
    public void changeSystemAudioMode(boolean enabled, IHdmiControlCallback callback) {
        assertRunOnServiceThread();
        if (!this.mService.isControlEnabled() || hasAction(DeviceDiscoveryAction.class)) {
            setSystemAudioMode(false);
            invokeCallback(callback, 6);
            return;
        }
        HdmiDeviceInfo avr = getAvrDeviceInfo();
        if (avr == null) {
            setSystemAudioMode(false);
            invokeCallback(callback, 3);
            return;
        }
        addAndStartAction(new SystemAudioActionFromTv(this, avr.getLogicalAddress(), enabled, callback));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setSystemAudioMode(boolean on) {
        if (!isSystemAudioControlFeatureEnabled() && on) {
            HdmiLogger.debug("Cannot turn on system audio mode because the System Audio Control feature is disabled.", new Object[0]);
            return;
        }
        HdmiLogger.debug("System Audio Mode change[old:%b new:%b]", Boolean.valueOf(this.mService.isSystemAudioActivated()), Boolean.valueOf(on));
        updateAudioManagerForSystemAudio(on);
        synchronized (this.mLock) {
            if (this.mService.isSystemAudioActivated() != on) {
                this.mService.setSystemAudioActivated(on);
                this.mService.announceSystemAudioModeChange(on);
            }
            if (on && !this.mArcEstablished) {
                startArcAction(true);
            } else if (!on) {
                startArcAction(false);
            }
        }
    }

    private void updateAudioManagerForSystemAudio(boolean on) {
        int device = this.mService.getAudioManager().setHdmiSystemAudioSupported(on);
        HdmiLogger.debug("[A]UpdateSystemAudio mode[on=%b] output=[%X]", Boolean.valueOf(on), Integer.valueOf(device));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isSystemAudioActivated() {
        if (!hasSystemAudioDevice()) {
            return false;
        }
        return this.mService.isSystemAudioActivated();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @HdmiAnnotations.ServiceThreadOnly
    public void setSystemAudioControlFeatureEnabled(boolean enabled) {
        assertRunOnServiceThread();
        synchronized (this.mLock) {
            this.mSystemAudioControlFeatureEnabled = enabled;
        }
        if (hasSystemAudioDevice()) {
            changeSystemAudioMode(enabled, null);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isSystemAudioControlFeatureEnabled() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mSystemAudioControlFeatureEnabled;
        }
        return z;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @HdmiAnnotations.ServiceThreadOnly
    public void enableArc(List<byte[]> supportedSads) {
        assertRunOnServiceThread();
        HdmiLogger.debug("Set Arc Status[old:%b new:true]", Boolean.valueOf(this.mArcEstablished));
        enableAudioReturnChannel(true);
        notifyArcStatusToAudioService(true, supportedSads);
        this.mArcEstablished = true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @HdmiAnnotations.ServiceThreadOnly
    public void disableArc() {
        assertRunOnServiceThread();
        HdmiLogger.debug("Set Arc Status[old:%b new:false]", Boolean.valueOf(this.mArcEstablished));
        enableAudioReturnChannel(false);
        notifyArcStatusToAudioService(false, new ArrayList());
        this.mArcEstablished = false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @HdmiAnnotations.ServiceThreadOnly
    public void enableAudioReturnChannel(boolean enabled) {
        assertRunOnServiceThread();
        HdmiDeviceInfo avr = getAvrDeviceInfo();
        if (avr != null) {
            this.mService.enableAudioReturnChannel(avr.getPortId(), enabled);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @HdmiAnnotations.ServiceThreadOnly
    public boolean isConnected(int portId) {
        assertRunOnServiceThread();
        return this.mService.isConnected(portId);
    }

    private void notifyArcStatusToAudioService(boolean enabled, List<byte[]> supportedSads) {
        AudioDeviceAttributes attributes = new AudioDeviceAttributes(2, 10, "", "", new ArrayList(), (List) supportedSads.stream().map(new Function() { // from class: com.android.server.hdmi.HdmiCecLocalDeviceTv$$ExternalSyntheticLambda0
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                return HdmiCecLocalDeviceTv.lambda$notifyArcStatusToAudioService$0((byte[]) obj);
            }
        }).collect(Collectors.toList()));
        this.mService.getAudioManager().setWiredDeviceConnectionState(attributes, enabled ? 1 : 0);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ AudioDescriptor lambda$notifyArcStatusToAudioService$0(byte[] sad) {
        return new AudioDescriptor(1, 0, sad);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @HdmiAnnotations.ServiceThreadOnly
    public boolean isArcEstablished() {
        assertRunOnServiceThread();
        if (this.mArcEstablished) {
            for (int i = 0; i < this.mArcFeatureEnabled.size(); i++) {
                if (this.mArcFeatureEnabled.valueAt(i)) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    @HdmiAnnotations.ServiceThreadOnly
    void changeArcFeatureEnabled(int portId, boolean enabled) {
        assertRunOnServiceThread();
        if (this.mArcFeatureEnabled.get(portId) == enabled) {
            return;
        }
        this.mArcFeatureEnabled.put(portId, enabled);
        HdmiDeviceInfo avr = getAvrDeviceInfo();
        if (avr == null || avr.getPortId() != portId) {
            return;
        }
        if (enabled && !this.mArcEstablished) {
            startArcAction(true);
        } else if (!enabled && this.mArcEstablished) {
            startArcAction(false);
        }
    }

    @HdmiAnnotations.ServiceThreadOnly
    boolean isArcFeatureEnabled(int portId) {
        assertRunOnServiceThread();
        return this.mArcFeatureEnabled.get(portId);
    }

    @HdmiAnnotations.ServiceThreadOnly
    void startArcAction(boolean enabled) {
        assertRunOnServiceThread();
        HdmiDeviceInfo info = getAvrDeviceInfo();
        if (info == null) {
            Slog.w(TAG, "Failed to start arc action; No AVR device.");
        } else if (!canStartArcUpdateAction(info.getLogicalAddress(), enabled)) {
            Slog.w(TAG, "Failed to start arc action; ARC configuration check failed.");
            if (enabled && !isConnectedToArcPort(info.getPhysicalAddress())) {
                displayOsd(1);
            }
        } else if (enabled) {
            removeAction(RequestArcTerminationAction.class);
            if (!hasAction(RequestArcInitiationAction.class)) {
                addAndStartAction(new RequestArcInitiationAction(this, info.getLogicalAddress()));
            }
        } else {
            removeAction(RequestArcInitiationAction.class);
            if (!hasAction(RequestArcTerminationAction.class)) {
                addAndStartAction(new RequestArcTerminationAction(this, info.getLogicalAddress()));
            }
        }
    }

    private boolean isDirectConnectAddress(int physicalAddress) {
        return (61440 & physicalAddress) == physicalAddress;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setAudioStatus(boolean mute, int volume) {
        if (!isSystemAudioActivated() || this.mService.getHdmiCecVolumeControl() == 0) {
            return;
        }
        synchronized (this.mLock) {
            this.mSystemAudioMute = mute;
            this.mSystemAudioVolume = volume;
            displayOsd(2, mute ? 101 : volume);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @HdmiAnnotations.ServiceThreadOnly
    public void changeVolume(int curVolume, int delta, int maxVolume) {
        assertRunOnServiceThread();
        if (getAvrDeviceInfo() == null || delta == 0 || !isSystemAudioActivated() || this.mService.getHdmiCecVolumeControl() == 0) {
            return;
        }
        int targetVolume = curVolume + delta;
        int cecVolume = VolumeControlAction.scaleToCecVolume(targetVolume, maxVolume);
        synchronized (this.mLock) {
            if (cecVolume == this.mSystemAudioVolume) {
                this.mService.setAudioStatus(false, VolumeControlAction.scaleToCustomVolume(this.mSystemAudioVolume, maxVolume));
                return;
            }
            List<VolumeControlAction> actions = getActions(VolumeControlAction.class);
            if (actions.isEmpty()) {
                addAndStartAction(new VolumeControlAction(this, getAvrDeviceInfo().getLogicalAddress(), delta > 0));
            } else {
                actions.get(0).handleVolumeChange(delta > 0);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @HdmiAnnotations.ServiceThreadOnly
    public void changeMute(boolean mute) {
        assertRunOnServiceThread();
        if (getAvrDeviceInfo() == null || this.mService.getHdmiCecVolumeControl() == 0) {
            return;
        }
        HdmiLogger.debug("[A]:Change mute:%b", Boolean.valueOf(mute));
        synchronized (this.mLock) {
            if (this.mSystemAudioMute == mute) {
                HdmiLogger.debug("No need to change mute.", new Object[0]);
            } else if (!isSystemAudioActivated()) {
                HdmiLogger.debug("[A]:System audio is not activated.", new Object[0]);
            } else {
                removeAction(VolumeControlAction.class);
                sendUserControlPressedAndReleased(getAvrDeviceInfo().getLogicalAddress(), HdmiCecKeycode.getMuteKey(mute));
            }
        }
    }

    @Override // com.android.server.hdmi.HdmiCecLocalDevice
    @HdmiAnnotations.ServiceThreadOnly
    protected int handleInitiateArc(HdmiCecMessage message) {
        assertRunOnServiceThread();
        if (!canStartArcUpdateAction(message.getSource(), true)) {
            HdmiDeviceInfo avrDeviceInfo = getAvrDeviceInfo();
            if (avrDeviceInfo == null) {
                this.mDelayedMessageBuffer.add(message);
                return -1;
            } else if (!isConnectedToArcPort(avrDeviceInfo.getPhysicalAddress())) {
                displayOsd(1);
                return 4;
            } else {
                return 4;
            }
        }
        removeAction(RequestArcInitiationAction.class);
        SetArcTransmissionStateAction action = new SetArcTransmissionStateAction(this, message.getSource(), true);
        addAndStartAction(action);
        return -1;
    }

    private boolean canStartArcUpdateAction(int avrAddress, boolean enabled) {
        HdmiDeviceInfo avr = getAvrDeviceInfo();
        if (avr != null && avrAddress == avr.getLogicalAddress() && isConnectedToArcPort(avr.getPhysicalAddress())) {
            if (!enabled) {
                return true;
            }
            if (!isConnected(avr.getPortId()) || !isArcFeatureEnabled(avr.getPortId()) || !isDirectConnectAddress(avr.getPhysicalAddress())) {
                return false;
            }
            return true;
        }
        return false;
    }

    @Override // com.android.server.hdmi.HdmiCecLocalDevice
    @HdmiAnnotations.ServiceThreadOnly
    protected int handleTerminateArc(HdmiCecMessage message) {
        assertRunOnServiceThread();
        if (this.mService.isPowerStandbyOrTransient()) {
            disableArc();
            return -1;
        }
        removeAction(RequestArcTerminationAction.class);
        SetArcTransmissionStateAction action = new SetArcTransmissionStateAction(this, message.getSource(), false);
        addAndStartAction(action);
        return -1;
    }

    @Override // com.android.server.hdmi.HdmiCecLocalDevice
    @HdmiAnnotations.ServiceThreadOnly
    protected int handleSetSystemAudioMode(HdmiCecMessage message) {
        assertRunOnServiceThread();
        boolean systemAudioStatus = HdmiUtils.parseCommandParamSystemAudioStatus(message);
        if (!isMessageForSystemAudio(message)) {
            if (getAvrDeviceInfo() == null) {
                this.mDelayedMessageBuffer.add(message);
            } else {
                HdmiLogger.warning("Invalid <Set System Audio Mode> message:" + message, new Object[0]);
                return 4;
            }
        } else if (systemAudioStatus && !isSystemAudioControlFeatureEnabled()) {
            HdmiLogger.debug("Ignoring <Set System Audio Mode> message because the System Audio Control feature is disabled: %s", message);
            return 4;
        }
        removeAction(SystemAudioAutoInitiationAction.class);
        SystemAudioActionFromAvr action = new SystemAudioActionFromAvr(this, message.getSource(), systemAudioStatus, null);
        addAndStartAction(action);
        return -1;
    }

    @Override // com.android.server.hdmi.HdmiCecLocalDevice
    @HdmiAnnotations.ServiceThreadOnly
    protected int handleSystemAudioModeStatus(HdmiCecMessage message) {
        assertRunOnServiceThread();
        if (!isMessageForSystemAudio(message)) {
            HdmiLogger.warning("Invalid <System Audio Mode Status> message:" + message, new Object[0]);
            return -1;
        }
        boolean tvSystemAudioMode = isSystemAudioControlFeatureEnabled();
        boolean avrSystemAudioMode = HdmiUtils.parseCommandParamSystemAudioStatus(message);
        HdmiDeviceInfo avr = getAvrDeviceInfo();
        if (avr == null) {
            setSystemAudioMode(false);
        } else if (avrSystemAudioMode != tvSystemAudioMode) {
            addAndStartAction(new SystemAudioActionFromTv(this, avr.getLogicalAddress(), tvSystemAudioMode, null));
        } else {
            setSystemAudioMode(tvSystemAudioMode);
        }
        return -1;
    }

    @Override // com.android.server.hdmi.HdmiCecLocalDevice
    @HdmiAnnotations.ServiceThreadOnly
    protected int handleRecordTvScreen(HdmiCecMessage message) {
        List<OneTouchRecordAction> actions = getActions(OneTouchRecordAction.class);
        if (!actions.isEmpty()) {
            OneTouchRecordAction action = actions.get(0);
            if (action.getRecorderAddress() != message.getSource()) {
                announceOneTouchRecordResult(message.getSource(), 48);
                return 2;
            }
            return 2;
        }
        int recorderAddress = message.getSource();
        byte[] recordSource = this.mService.invokeRecordRequestListener(recorderAddress);
        return startOneTouchRecord(recorderAddress, recordSource);
    }

    @Override // com.android.server.hdmi.HdmiCecLocalDevice
    protected int handleTimerClearedStatus(HdmiCecMessage message) {
        byte[] params = message.getParams();
        int timerClearedStatusData = params[0] & 255;
        announceTimerRecordingResult(message.getSource(), timerClearedStatusData);
        return -1;
    }

    @Override // com.android.server.hdmi.HdmiCecLocalDevice
    protected int handleSetAudioVolumeLevel(SetAudioVolumeLevelMessage message) {
        if (this.mService.isSystemAudioActivated()) {
            return 1;
        }
        this.mService.setStreamMusicVolume(message.getAudioVolumeLevel(), 0);
        return -1;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void announceOneTouchRecordResult(int recorderAddress, int result) {
        this.mService.invokeOneTouchRecordResult(recorderAddress, result);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void announceTimerRecordingResult(int recorderAddress, int result) {
        this.mService.invokeTimerRecordingResult(recorderAddress, result);
    }

    void announceClearTimerRecordingResult(int recorderAddress, int result) {
        this.mService.invokeClearTimerRecordingResult(recorderAddress, result);
    }

    private boolean isMessageForSystemAudio(HdmiCecMessage message) {
        return this.mService.isControlEnabled() && message.getSource() == 5 && (message.getDestination() == 0 || message.getDestination() == 15) && getAvrDeviceInfo() != null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @HdmiAnnotations.ServiceThreadOnly
    public HdmiDeviceInfo getAvrDeviceInfo() {
        assertRunOnServiceThread();
        return this.mService.getHdmiCecNetwork().getCecDeviceInfo(5);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasSystemAudioDevice() {
        return getSafeAvrDeviceInfo() != null;
    }

    HdmiDeviceInfo getSafeAvrDeviceInfo() {
        return this.mService.getHdmiCecNetwork().getSafeCecDeviceInfo(5);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public AudioDeviceAttributes getSystemAudioOutputDevice() {
        return HdmiControlService.AUDIO_OUTPUT_DEVICE_HDMI_ARC;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @HdmiAnnotations.ServiceThreadOnly
    public void handleRemoveActiveRoutingPath(int path) {
        assertRunOnServiceThread();
        if (isTailOfActivePath(path, getActivePath())) {
            int newPath = this.mService.portIdToPath(getActivePortId());
            startRoutingControl(getActivePath(), newPath, null);
        }
    }

    @HdmiAnnotations.ServiceThreadOnly
    void launchRoutingControl(boolean routingForBootup) {
        assertRunOnServiceThread();
        if (getActivePortId() != -1) {
            if (!routingForBootup && !isProhibitMode()) {
                int newPath = this.mService.portIdToPath(getActivePortId());
                setActivePath(newPath);
                startRoutingControl(getActivePath(), newPath, null);
                return;
            }
            return;
        }
        int activePath = this.mService.getPhysicalAddress();
        setActivePath(activePath);
        if (!routingForBootup && !this.mDelayedMessageBuffer.isBuffered(130)) {
            this.mService.sendCecCommand(HdmiCecMessageBuilder.buildActiveSource(getDeviceInfo().getLogicalAddress(), activePath));
        }
    }

    @Override // com.android.server.hdmi.HdmiCecLocalDevice
    @HdmiAnnotations.ServiceThreadOnly
    void onHotplug(int portId, boolean connected) {
        assertRunOnServiceThread();
        if (!connected) {
            this.mService.getHdmiCecNetwork().removeCecSwitches(portId);
        }
        if (getAvrDeviceInfo() != null && portId == getAvrDeviceInfo().getPortId()) {
            HdmiLogger.debug("Port ID:%d, 5v=%b", Integer.valueOf(portId), Boolean.valueOf(connected));
            if (!connected) {
                setSystemAudioMode(false);
            } else {
                onNewAvrAdded(getAvrDeviceInfo());
            }
        }
        List<HotplugDetectionAction> hotplugActions = getActions(HotplugDetectionAction.class);
        if (!hotplugActions.isEmpty()) {
            hotplugActions.get(0).pollAllDevicesNow();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @HdmiAnnotations.ServiceThreadOnly
    public boolean getAutoWakeup() {
        assertRunOnServiceThread();
        return this.mService.getHdmiCecConfig().getIntValue("tv_wake_on_one_touch_play") == 1;
    }

    @Override // com.android.server.hdmi.HdmiCecLocalDevice
    @HdmiAnnotations.ServiceThreadOnly
    protected void disableDevice(boolean initiatedByCec, HdmiCecLocalDevice.PendingActionClearedCallback callback) {
        assertRunOnServiceThread();
        this.mService.unregisterTvInputCallback(this.mTvInputCallback);
        removeAction(DeviceDiscoveryAction.class);
        removeAction(HotplugDetectionAction.class);
        removeAction(PowerStatusMonitorAction.class);
        removeAction(OneTouchRecordAction.class);
        removeAction(TimerRecordingAction.class);
        removeAction(NewDeviceAction.class);
        removeAction(AbsoluteVolumeAudioStatusAction.class);
        disableSystemAudioIfExist();
        disableArcIfExist();
        super.disableDevice(initiatedByCec, callback);
        clearDeviceInfoList();
        getActiveSource().invalidate();
        setActivePath(GnssNative.GNSS_AIDING_TYPE_ALL);
        checkIfPendingActionsCleared();
    }

    @HdmiAnnotations.ServiceThreadOnly
    private void disableSystemAudioIfExist() {
        assertRunOnServiceThread();
        if (getAvrDeviceInfo() == null) {
            return;
        }
        removeAction(SystemAudioActionFromAvr.class);
        removeAction(SystemAudioActionFromTv.class);
        removeAction(SystemAudioAutoInitiationAction.class);
        removeAction(VolumeControlAction.class);
        if (!this.mService.isControlEnabled()) {
            setSystemAudioMode(false);
        }
    }

    @HdmiAnnotations.ServiceThreadOnly
    private void disableArcIfExist() {
        assertRunOnServiceThread();
        HdmiDeviceInfo avr = getAvrDeviceInfo();
        if (avr == null) {
            return;
        }
        disableArc();
        removeAllRunningArcAction();
        if (!hasAction(RequestArcTerminationAction.class) && isArcEstablished()) {
            addAndStartAction(new RequestArcTerminationAction(this, avr.getLogicalAddress()));
        }
    }

    @HdmiAnnotations.ServiceThreadOnly
    private void removeAllRunningArcAction() {
        removeAction(RequestArcTerminationAction.class);
        removeAction(RequestArcInitiationAction.class);
        removeAction(SetArcTransmissionStateAction.class);
    }

    @Override // com.android.server.hdmi.HdmiCecLocalDevice
    @HdmiAnnotations.ServiceThreadOnly
    protected void onStandby(boolean initiatedByCec, int standbyAction) {
        assertRunOnServiceThread();
        if (!this.mService.isControlEnabled()) {
            return;
        }
        boolean sendStandbyOnSleep = this.mService.getHdmiCecConfig().getIntValue("tv_send_standby_on_sleep") == 1;
        if (!initiatedByCec && sendStandbyOnSleep) {
            this.mService.sendCecCommand(HdmiCecMessageBuilder.buildStandby(getDeviceInfo().getLogicalAddress(), 15));
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isProhibitMode() {
        return this.mService.isProhibitMode();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isPowerStandbyOrTransient() {
        return this.mService.isPowerStandbyOrTransient();
    }

    @HdmiAnnotations.ServiceThreadOnly
    void displayOsd(int messageId) {
        assertRunOnServiceThread();
        this.mService.displayOsd(messageId);
    }

    @HdmiAnnotations.ServiceThreadOnly
    void displayOsd(int messageId, int extra) {
        assertRunOnServiceThread();
        this.mService.displayOsd(messageId, extra);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @HdmiAnnotations.ServiceThreadOnly
    public int startOneTouchRecord(int recorderAddress, byte[] recordSource) {
        assertRunOnServiceThread();
        if (!this.mService.isControlEnabled()) {
            Slog.w(TAG, "Can not start one touch record. CEC control is disabled.");
            announceOneTouchRecordResult(recorderAddress, 51);
            return 1;
        } else if (!checkRecorder(recorderAddress)) {
            Slog.w(TAG, "Invalid recorder address:" + recorderAddress);
            announceOneTouchRecordResult(recorderAddress, 49);
            return 1;
        } else if (!checkRecordSource(recordSource)) {
            Slog.w(TAG, "Invalid record source." + Arrays.toString(recordSource));
            announceOneTouchRecordResult(recorderAddress, 50);
            return 2;
        } else {
            addAndStartAction(new OneTouchRecordAction(this, recorderAddress, recordSource));
            Slog.i(TAG, "Start new [One Touch Record]-Target:" + recorderAddress + ", recordSource:" + Arrays.toString(recordSource));
            return -1;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @HdmiAnnotations.ServiceThreadOnly
    public void stopOneTouchRecord(int recorderAddress) {
        assertRunOnServiceThread();
        if (!this.mService.isControlEnabled()) {
            Slog.w(TAG, "Can not stop one touch record. CEC control is disabled.");
            announceOneTouchRecordResult(recorderAddress, 51);
        } else if (!checkRecorder(recorderAddress)) {
            Slog.w(TAG, "Invalid recorder address:" + recorderAddress);
            announceOneTouchRecordResult(recorderAddress, 49);
        } else {
            removeAction(OneTouchRecordAction.class);
            this.mService.sendCecCommand(HdmiCecMessageBuilder.buildRecordOff(getDeviceInfo().getLogicalAddress(), recorderAddress));
            Slog.i(TAG, "Stop [One Touch Record]-Target:" + recorderAddress);
        }
    }

    private boolean checkRecorder(int recorderAddress) {
        HdmiDeviceInfo device = this.mService.getHdmiCecNetwork().getCecDeviceInfo(recorderAddress);
        return device != null && HdmiUtils.isEligibleAddressForDevice(1, recorderAddress);
    }

    private boolean checkRecordSource(byte[] recordSource) {
        return recordSource != null && HdmiRecordSources.checkRecordSource(recordSource);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @HdmiAnnotations.ServiceThreadOnly
    public void startTimerRecording(int recorderAddress, int sourceType, byte[] recordSource) {
        assertRunOnServiceThread();
        if (!this.mService.isControlEnabled()) {
            Slog.w(TAG, "Can not start one touch record. CEC control is disabled.");
            announceTimerRecordingResult(recorderAddress, 3);
        } else if (!checkRecorder(recorderAddress)) {
            Slog.w(TAG, "Invalid recorder address:" + recorderAddress);
            announceTimerRecordingResult(recorderAddress, 1);
        } else if (!checkTimerRecordingSource(sourceType, recordSource)) {
            Slog.w(TAG, "Invalid record source." + Arrays.toString(recordSource));
            announceTimerRecordingResult(recorderAddress, 2);
        } else {
            addAndStartAction(new TimerRecordingAction(this, recorderAddress, sourceType, recordSource));
            Slog.i(TAG, "Start [Timer Recording]-Target:" + recorderAddress + ", SourceType:" + sourceType + ", RecordSource:" + Arrays.toString(recordSource));
        }
    }

    private boolean checkTimerRecordingSource(int sourceType, byte[] recordSource) {
        return recordSource != null && HdmiTimerRecordSources.checkTimerRecordSource(sourceType, recordSource);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @HdmiAnnotations.ServiceThreadOnly
    public void clearTimerRecording(int recorderAddress, int sourceType, byte[] recordSource) {
        assertRunOnServiceThread();
        if (!this.mService.isControlEnabled()) {
            Slog.w(TAG, "Can not start one touch record. CEC control is disabled.");
            announceClearTimerRecordingResult(recorderAddress, 162);
        } else if (!checkRecorder(recorderAddress)) {
            Slog.w(TAG, "Invalid recorder address:" + recorderAddress);
            announceClearTimerRecordingResult(recorderAddress, 160);
        } else if (!checkTimerRecordingSource(sourceType, recordSource)) {
            Slog.w(TAG, "Invalid record source." + Arrays.toString(recordSource));
            announceClearTimerRecordingResult(recorderAddress, 161);
        } else {
            sendClearTimerMessage(recorderAddress, sourceType, recordSource);
        }
    }

    private void sendClearTimerMessage(final int recorderAddress, int sourceType, byte[] recordSource) {
        HdmiCecMessage message;
        switch (sourceType) {
            case 1:
                message = HdmiCecMessageBuilder.buildClearDigitalTimer(getDeviceInfo().getLogicalAddress(), recorderAddress, recordSource);
                break;
            case 2:
                message = HdmiCecMessageBuilder.buildClearAnalogueTimer(getDeviceInfo().getLogicalAddress(), recorderAddress, recordSource);
                break;
            case 3:
                message = HdmiCecMessageBuilder.buildClearExternalTimer(getDeviceInfo().getLogicalAddress(), recorderAddress, recordSource);
                break;
            default:
                Slog.w(TAG, "Invalid source type:" + recorderAddress);
                announceClearTimerRecordingResult(recorderAddress, 161);
                return;
        }
        this.mService.sendCecCommand(message, new HdmiControlService.SendMessageCallback() { // from class: com.android.server.hdmi.HdmiCecLocalDeviceTv.3
            @Override // com.android.server.hdmi.HdmiControlService.SendMessageCallback
            public void onSendCompleted(int error) {
                if (error != 0) {
                    HdmiCecLocalDeviceTv.this.announceClearTimerRecordingResult(recorderAddress, 161);
                }
            }
        });
    }

    @Override // com.android.server.hdmi.HdmiCecLocalDevice
    protected int handleMenuStatus(HdmiCecMessage message) {
        return -1;
    }

    @Override // com.android.server.hdmi.HdmiCecLocalDevice
    protected int getRcProfile() {
        return 0;
    }

    @Override // com.android.server.hdmi.HdmiCecLocalDevice
    protected List<Integer> getRcFeatures() {
        List<Integer> features = new ArrayList<>();
        int profile = this.mService.getHdmiCecConfig().getIntValue("rc_profile_tv");
        features.add(Integer.valueOf(profile));
        return features;
    }

    @Override // com.android.server.hdmi.HdmiCecLocalDevice
    protected DeviceFeatures computeDeviceFeatures() {
        int i = 0;
        List<HdmiPortInfo> ports = this.mService.getPortInfo();
        Iterator<HdmiPortInfo> it = ports.iterator();
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            HdmiPortInfo port = it.next();
            if (isArcFeatureEnabled(port.getId())) {
                i = 1;
                break;
            }
        }
        return DeviceFeatures.NO_FEATURES_SUPPORTED.toBuilder().setRecordTvScreenSupport(1).setArcTxSupport(i).setSetAudioVolumeLevelSupport(1).build();
    }

    @Override // com.android.server.hdmi.HdmiCecLocalDevice
    protected void sendStandby(int deviceId) {
        HdmiDeviceInfo targetDevice = this.mService.getHdmiCecNetwork().getDeviceInfo(deviceId);
        if (targetDevice == null) {
            return;
        }
        int targetAddress = targetDevice.getLogicalAddress();
        this.mService.sendCecCommand(HdmiCecMessageBuilder.buildStandby(getDeviceInfo().getLogicalAddress(), targetAddress));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @HdmiAnnotations.ServiceThreadOnly
    public void processAllDelayedMessages() {
        assertRunOnServiceThread();
        this.mDelayedMessageBuffer.processAllMessages();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @HdmiAnnotations.ServiceThreadOnly
    public void processDelayedMessages(int address) {
        assertRunOnServiceThread();
        this.mDelayedMessageBuffer.processMessagesForDevice(address);
    }

    @HdmiAnnotations.ServiceThreadOnly
    void processDelayedActiveSource(int address) {
        assertRunOnServiceThread();
        this.mDelayedMessageBuffer.processActiveSource(address);
    }

    @Override // com.android.server.hdmi.HdmiCecLocalDevice
    protected void dump(IndentingPrintWriter pw) {
        super.dump(pw);
        pw.println("mArcEstablished: " + this.mArcEstablished);
        pw.println("mArcFeatureEnabled: " + this.mArcFeatureEnabled);
        pw.println("mSystemAudioMute: " + this.mSystemAudioMute);
        pw.println("mSystemAudioControlFeatureEnabled: " + this.mSystemAudioControlFeatureEnabled);
        pw.println("mSkipRoutingControl: " + this.mSkipRoutingControl);
        pw.println("mPrevPortId: " + this.mPrevPortId);
    }
}
