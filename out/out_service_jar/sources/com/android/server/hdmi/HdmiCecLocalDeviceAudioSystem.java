package com.android.server.hdmi;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.hardware.hdmi.DeviceFeatures;
import android.hardware.hdmi.HdmiDeviceInfo;
import android.hardware.hdmi.IHdmiControlCallback;
import android.media.AudioDeviceInfo;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.tv.TvContract;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.os.SystemProperties;
import android.sysprop.HdmiProperties;
import android.util.Slog;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.hdmi.DeviceDiscoveryAction;
import com.android.server.hdmi.HdmiAnnotations;
import com.android.server.hdmi.HdmiCecLocalDevice;
import com.android.server.hdmi.HdmiUtils;
import com.android.server.location.gnss.hal.GnssNative;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.function.IntFunction;
import java.util.stream.Collectors;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes.dex */
public class HdmiCecLocalDeviceAudioSystem extends HdmiCecLocalDeviceSource {
    private static final String SHORT_AUDIO_DESCRIPTOR_CONFIG_PATH = "/vendor/etc/sadConfig.xml";
    private static final String TAG = "HdmiCecLocalDeviceAudioSystem";
    private static final boolean WAKE_ON_HOTPLUG = false;
    @HdmiAnnotations.ServiceThreadOnly
    private boolean mArcEstablished;
    private boolean mArcIntentUsed;
    private final DelayedMessageBuffer mDelayedMessageBuffer;
    private final HashMap<Integer, String> mPortIdToTvInputs;
    private boolean mSystemAudioControlFeatureEnabled;
    private final TvInputManager.TvInputCallback mTvInputCallback;
    private final HashMap<String, HdmiDeviceInfo> mTvInputsToDeviceInfo;
    private Boolean mTvSystemAudioModeSupport;

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public interface TvSystemAudioModeSupportedCallback {
        void onResult(boolean z);
    }

    @Override // com.android.server.hdmi.HdmiCecLocalDevice
    public /* bridge */ /* synthetic */ ArrayBlockingQueue getActiveSourceHistory() {
        return super.getActiveSourceHistory();
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public HdmiCecLocalDeviceAudioSystem(HdmiControlService service) {
        super(service, 5);
        this.mTvSystemAudioModeSupport = null;
        this.mArcEstablished = false;
        this.mArcIntentUsed = ((String) HdmiProperties.arc_port().orElse("0")).contains("tvinput");
        this.mPortIdToTvInputs = new HashMap<>();
        this.mTvInputsToDeviceInfo = new HashMap<>();
        this.mDelayedMessageBuffer = new DelayedMessageBuffer(this);
        this.mTvInputCallback = new TvInputManager.TvInputCallback() { // from class: com.android.server.hdmi.HdmiCecLocalDeviceAudioSystem.1
            @Override // android.media.tv.TvInputManager.TvInputCallback
            public void onInputAdded(String inputId) {
                HdmiCecLocalDeviceAudioSystem.this.addOrUpdateTvInput(inputId);
            }

            @Override // android.media.tv.TvInputManager.TvInputCallback
            public void onInputRemoved(String inputId) {
                HdmiCecLocalDeviceAudioSystem.this.removeTvInput(inputId);
            }

            @Override // android.media.tv.TvInputManager.TvInputCallback
            public void onInputUpdated(String inputId) {
                HdmiCecLocalDeviceAudioSystem.this.addOrUpdateTvInput(inputId);
            }
        };
        this.mRoutingControlFeatureEnabled = this.mService.getHdmiCecConfig().getIntValue("routing_control") == 1;
        this.mSystemAudioControlFeatureEnabled = this.mService.getHdmiCecConfig().getIntValue("system_audio_control") == 1;
        this.mStandbyHandler = new HdmiCecStandbyModeHandler(service, this);
    }

    /* JADX INFO: Access modifiers changed from: private */
    @HdmiAnnotations.ServiceThreadOnly
    public void addOrUpdateTvInput(String inputId) {
        assertRunOnServiceThread();
        synchronized (this.mLock) {
            TvInputInfo tvInfo = this.mService.getTvInputManager().getTvInputInfo(inputId);
            if (tvInfo == null) {
                return;
            }
            HdmiDeviceInfo info = tvInfo.getHdmiDeviceInfo();
            if (info == null) {
                return;
            }
            this.mPortIdToTvInputs.put(Integer.valueOf(info.getPortId()), inputId);
            this.mTvInputsToDeviceInfo.put(inputId, info);
            if (info.isCecDevice()) {
                processDelayedActiveSource(info.getLogicalAddress());
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    @HdmiAnnotations.ServiceThreadOnly
    public void removeTvInput(String inputId) {
        assertRunOnServiceThread();
        synchronized (this.mLock) {
            if (this.mTvInputsToDeviceInfo.get(inputId) == null) {
                return;
            }
            int portId = this.mTvInputsToDeviceInfo.get(inputId).getPortId();
            this.mPortIdToTvInputs.remove(Integer.valueOf(portId));
            this.mTvInputsToDeviceInfo.remove(inputId);
        }
    }

    @Override // com.android.server.hdmi.HdmiCecLocalDevice
    @HdmiAnnotations.ServiceThreadOnly
    protected boolean isInputReady(int portId) {
        assertRunOnServiceThread();
        String tvInputId = this.mPortIdToTvInputs.get(Integer.valueOf(portId));
        HdmiDeviceInfo info = this.mTvInputsToDeviceInfo.get(tvInputId);
        return info != null;
    }

    @Override // com.android.server.hdmi.HdmiCecLocalDevice
    protected DeviceFeatures computeDeviceFeatures() {
        return DeviceFeatures.NO_FEATURES_SUPPORTED.toBuilder().setArcRxSupport(SystemProperties.getBoolean("persist.sys.hdmi.property_arc_support", true) ? 1 : 0).build();
    }

    @Override // com.android.server.hdmi.HdmiCecLocalDeviceSource, com.android.server.hdmi.HdmiCecLocalDevice
    @HdmiAnnotations.ServiceThreadOnly
    void onHotplug(int portId, boolean connected) {
        assertRunOnServiceThread();
        if (this.mService.getPortInfo(portId).getType() == 1) {
            this.mCecMessageCache.flushAll();
            if (!connected) {
                if (isSystemAudioActivated()) {
                    this.mTvSystemAudioModeSupport = null;
                    checkSupportAndSetSystemAudioMode(false);
                }
                if (isArcEnabled()) {
                    setArcStatus(false);
                }
            }
        } else if (!connected && this.mPortIdToTvInputs.get(Integer.valueOf(portId)) != null) {
            String tvInputId = this.mPortIdToTvInputs.get(Integer.valueOf(portId));
            HdmiDeviceInfo info = this.mTvInputsToDeviceInfo.get(tvInputId);
            if (info == null) {
                return;
            }
            this.mService.getHdmiCecNetwork().removeCecDevice(this, info.getLogicalAddress());
        }
    }

    @Override // com.android.server.hdmi.HdmiCecLocalDeviceSource, com.android.server.hdmi.HdmiCecLocalDevice
    @HdmiAnnotations.ServiceThreadOnly
    protected void disableDevice(boolean initiatedByCec, HdmiCecLocalDevice.PendingActionClearedCallback callback) {
        super.disableDevice(initiatedByCec, callback);
        assertRunOnServiceThread();
        this.mService.unregisterTvInputCallback(this.mTvInputCallback);
    }

    @Override // com.android.server.hdmi.HdmiCecLocalDevice
    @HdmiAnnotations.ServiceThreadOnly
    protected void onStandby(boolean initiatedByCec, int standbyAction) {
        assertRunOnServiceThread();
        this.mService.setActiveSource(-1, GnssNative.GNSS_AIDING_TYPE_ALL, "HdmiCecLocalDeviceAudioSystem#onStandby()");
        this.mTvSystemAudioModeSupport = null;
        synchronized (this.mLock) {
            this.mService.writeStringSystemProperty("persist.sys.hdmi.last_system_audio_control", isSystemAudioActivated() ? "true" : "false");
        }
        terminateSystemAudioMode();
    }

    @Override // com.android.server.hdmi.HdmiCecLocalDevice
    @HdmiAnnotations.ServiceThreadOnly
    protected void onAddressAllocated(int logicalAddress, int reason) {
        assertRunOnServiceThread();
        HdmiControlService hdmiControlService = this.mService;
        if (reason == 0) {
            this.mService.setAndBroadcastActiveSource(this.mService.getPhysicalAddress(), getDeviceInfo().getDeviceType(), 15, "HdmiCecLocalDeviceAudioSystem#onAddressAllocated()");
        }
        this.mService.sendCecCommand(HdmiCecMessageBuilder.buildReportPhysicalAddressCommand(getDeviceInfo().getLogicalAddress(), this.mService.getPhysicalAddress(), this.mDeviceType));
        this.mService.sendCecCommand(HdmiCecMessageBuilder.buildDeviceVendorIdCommand(getDeviceInfo().getLogicalAddress(), this.mService.getVendorId()));
        this.mService.registerTvInputCallback(this.mTvInputCallback);
        initArcOnFromAvr();
        if (!this.mService.isScreenOff()) {
            int systemAudioControlOnPowerOnProp = SystemProperties.getInt("persist.sys.hdmi.system_audio_control_on_power_on", 0);
            boolean lastSystemAudioControlStatus = SystemProperties.getBoolean("persist.sys.hdmi.last_system_audio_control", true);
            systemAudioControlOnPowerOn(systemAudioControlOnPowerOnProp, lastSystemAudioControlStatus);
        }
        this.mService.getHdmiCecNetwork().clearDeviceList();
        launchDeviceDiscovery();
        startQueuedActions();
    }

    @Override // com.android.server.hdmi.HdmiCecLocalDevice
    protected int findKeyReceiverAddress() {
        if (getActiveSource().isValid()) {
            return getActiveSource().logicalAddress;
        }
        return -1;
    }

    protected void systemAudioControlOnPowerOn(int systemAudioOnPowerOnProp, boolean lastSystemAudioControlStatus) {
        if (systemAudioOnPowerOnProp == 0 || (systemAudioOnPowerOnProp == 1 && lastSystemAudioControlStatus && isSystemAudioControlFeatureEnabled())) {
            addAndStartAction(new SystemAudioInitiationActionFromAvr(this));
        }
    }

    @Override // com.android.server.hdmi.HdmiCecLocalDevice
    @HdmiAnnotations.ServiceThreadOnly
    protected int getPreferredAddress() {
        assertRunOnServiceThread();
        return SystemProperties.getInt("persist.sys.hdmi.addr.audiosystem", 15);
    }

    @Override // com.android.server.hdmi.HdmiCecLocalDevice
    @HdmiAnnotations.ServiceThreadOnly
    protected void setPreferredAddress(int addr) {
        assertRunOnServiceThread();
        this.mService.writeStringSystemProperty("persist.sys.hdmi.addr.audiosystem", String.valueOf(addr));
    }

    @HdmiAnnotations.ServiceThreadOnly
    void processDelayedActiveSource(int address) {
        assertRunOnServiceThread();
        this.mDelayedMessageBuffer.processActiveSource(address);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.hdmi.HdmiCecLocalDeviceSource, com.android.server.hdmi.HdmiCecLocalDevice
    @HdmiAnnotations.ServiceThreadOnly
    public int handleActiveSource(HdmiCecMessage message) {
        assertRunOnServiceThread();
        int logicalAddress = message.getSource();
        int physicalAddress = HdmiUtils.twoBytesToInt(message.getParams());
        if (HdmiUtils.getLocalPortFromPhysicalAddress(physicalAddress, this.mService.getPhysicalAddress()) == -1) {
            return super.handleActiveSource(message);
        }
        HdmiDeviceInfo info = this.mService.getHdmiCecNetwork().getCecDeviceInfo(logicalAddress);
        if (info == null) {
            HdmiLogger.debug("Device info %X not found; buffering the command", Integer.valueOf(logicalAddress));
            this.mDelayedMessageBuffer.add(message);
        } else if (!isInputReady(info.getPortId())) {
            HdmiLogger.debug("Input not ready for device: %X; buffering the command", Integer.valueOf(info.getId()));
            this.mDelayedMessageBuffer.add(message);
        } else {
            this.mDelayedMessageBuffer.removeActiveSource();
            return super.handleActiveSource(message);
        }
        return -1;
    }

    @Override // com.android.server.hdmi.HdmiCecLocalDevice
    @HdmiAnnotations.ServiceThreadOnly
    protected int handleInitiateArc(HdmiCecMessage message) {
        assertRunOnServiceThread();
        HdmiLogger.debug("HdmiCecLocalDeviceAudioSystemStub handleInitiateArc", new Object[0]);
        return -1;
    }

    @Override // com.android.server.hdmi.HdmiCecLocalDevice
    @HdmiAnnotations.ServiceThreadOnly
    protected int handleReportArcInitiate(HdmiCecMessage message) {
        assertRunOnServiceThread();
        return -1;
    }

    @Override // com.android.server.hdmi.HdmiCecLocalDevice
    @HdmiAnnotations.ServiceThreadOnly
    protected int handleReportArcTermination(HdmiCecMessage message) {
        assertRunOnServiceThread();
        processArcTermination();
        return -1;
    }

    @Override // com.android.server.hdmi.HdmiCecLocalDevice
    @HdmiAnnotations.ServiceThreadOnly
    protected int handleGiveAudioStatus(HdmiCecMessage message) {
        assertRunOnServiceThread();
        if (isSystemAudioControlFeatureEnabled() && this.mService.getHdmiCecVolumeControl() == 1) {
            reportAudioStatus(message.getSource());
            return -1;
        }
        return 4;
    }

    @Override // com.android.server.hdmi.HdmiCecLocalDevice
    @HdmiAnnotations.ServiceThreadOnly
    protected int handleGiveSystemAudioModeStatus(HdmiCecMessage message) {
        assertRunOnServiceThread();
        boolean isSystemAudioModeOnOrTurningOn = isSystemAudioActivated();
        if (!isSystemAudioModeOnOrTurningOn && message.getSource() == 0 && hasAction(SystemAudioInitiationActionFromAvr.class)) {
            isSystemAudioModeOnOrTurningOn = true;
        }
        this.mService.sendCecCommand(HdmiCecMessageBuilder.buildReportSystemAudioMode(getDeviceInfo().getLogicalAddress(), message.getSource(), isSystemAudioModeOnOrTurningOn));
        return -1;
    }

    @Override // com.android.server.hdmi.HdmiCecLocalDevice
    @HdmiAnnotations.ServiceThreadOnly
    protected int handleRequestArcInitiate(HdmiCecMessage message) {
        assertRunOnServiceThread();
        removeAction(ArcInitiationActionFromAvr.class);
        if (this.mService.readBooleanSystemProperty("persist.sys.hdmi.property_arc_support", true)) {
            if (!isDirectConnectToTv()) {
                HdmiLogger.debug("AVR device is not directly connected with TV", new Object[0]);
                return 1;
            }
            addAndStartAction(new ArcInitiationActionFromAvr(this));
            return -1;
        }
        return 0;
    }

    @Override // com.android.server.hdmi.HdmiCecLocalDevice
    @HdmiAnnotations.ServiceThreadOnly
    protected int handleRequestArcTermination(HdmiCecMessage message) {
        assertRunOnServiceThread();
        if (SystemProperties.getBoolean("persist.sys.hdmi.property_arc_support", true)) {
            if (!isArcEnabled()) {
                HdmiLogger.debug("ARC is not established between TV and AVR device", new Object[0]);
                return 1;
            }
            removeAction(ArcTerminationActionFromAvr.class);
            addAndStartAction(new ArcTerminationActionFromAvr(this));
            return -1;
        }
        return 0;
    }

    @Override // com.android.server.hdmi.HdmiCecLocalDevice
    @HdmiAnnotations.ServiceThreadOnly
    protected int handleRequestShortAudioDescriptor(HdmiCecMessage message) {
        byte[] sadBytes;
        assertRunOnServiceThread();
        HdmiLogger.debug("HdmiCecLocalDeviceAudioSystemStub handleRequestShortAudioDescriptor", new Object[0]);
        if (!isSystemAudioControlFeatureEnabled()) {
            return 4;
        }
        if (!isSystemAudioActivated()) {
            return 1;
        }
        List<HdmiUtils.DeviceConfig> config = null;
        File file = new File(SHORT_AUDIO_DESCRIPTOR_CONFIG_PATH);
        if (file.exists()) {
            try {
                InputStream in = new FileInputStream(file);
                config = HdmiUtils.ShortAudioDescriptorXmlParser.parse(in);
                in.close();
            } catch (IOException e) {
                Slog.e(TAG, "Error reading file: " + file, e);
            } catch (XmlPullParserException e2) {
                Slog.e(TAG, "Unable to parse file: " + file, e2);
            }
        }
        int[] audioFormatCodes = parseAudioFormatCodes(message.getParams());
        if (config != null && config.size() > 0) {
            sadBytes = getSupportedShortAudioDescriptorsFromConfig(config, audioFormatCodes);
        } else {
            AudioDeviceInfo deviceInfo = getSystemAudioDeviceInfo();
            if (deviceInfo == null) {
                return 5;
            }
            sadBytes = getSupportedShortAudioDescriptors(deviceInfo, audioFormatCodes);
        }
        if (sadBytes.length == 0) {
            return 3;
        }
        this.mService.sendCecCommand(HdmiCecMessageBuilder.buildReportShortAudioDescriptor(getDeviceInfo().getLogicalAddress(), message.getSource(), sadBytes));
        return -1;
    }

    private byte[] getSupportedShortAudioDescriptors(AudioDeviceInfo deviceInfo, int[] audioFormatCodes) {
        ArrayList<byte[]> sads = new ArrayList<>(audioFormatCodes.length);
        for (int audioFormatCode : audioFormatCodes) {
            byte[] sad = getSupportedShortAudioDescriptor(deviceInfo, audioFormatCode);
            if (sad != null) {
                if (sad.length == 3) {
                    sads.add(sad);
                } else {
                    HdmiLogger.warning("Dropping Short Audio Descriptor with length %d for requested codec %x", Integer.valueOf(sad.length), Integer.valueOf(audioFormatCode));
                }
            }
        }
        return getShortAudioDescriptorBytes(sads);
    }

    private byte[] getSupportedShortAudioDescriptorsFromConfig(List<HdmiUtils.DeviceConfig> deviceConfig, int[] audioFormatCodes) {
        byte[] sad;
        HdmiUtils.DeviceConfig deviceConfigToUse = null;
        String audioDeviceName = SystemProperties.get("persist.sys.hdmi.property_sytem_audio_mode_audio_port", "VX_AUDIO_DEVICE_IN_HDMI_ARC");
        Iterator<HdmiUtils.DeviceConfig> it = deviceConfig.iterator();
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            HdmiUtils.DeviceConfig device = it.next();
            if (device.name.equals(audioDeviceName)) {
                deviceConfigToUse = device;
                break;
            }
        }
        if (deviceConfigToUse == null) {
            Slog.w(TAG, "sadConfig.xml does not have required device info for " + audioDeviceName);
            return new byte[0];
        }
        HashMap<Integer, byte[]> map = new HashMap<>();
        ArrayList<byte[]> sads = new ArrayList<>(audioFormatCodes.length);
        for (HdmiUtils.CodecSad codecSad : deviceConfigToUse.supportedCodecs) {
            map.put(Integer.valueOf(codecSad.audioCodec), codecSad.sad);
        }
        for (int i = 0; i < audioFormatCodes.length; i++) {
            if (map.containsKey(Integer.valueOf(audioFormatCodes[i])) && (sad = map.get(Integer.valueOf(audioFormatCodes[i]))) != null && sad.length == 3) {
                sads.add(sad);
            }
        }
        return getShortAudioDescriptorBytes(sads);
    }

    private byte[] getShortAudioDescriptorBytes(ArrayList<byte[]> sads) {
        byte[] bytes = new byte[sads.size() * 3];
        int index = 0;
        Iterator<byte[]> it = sads.iterator();
        while (it.hasNext()) {
            byte[] sad = it.next();
            System.arraycopy(sad, 0, bytes, index, 3);
            index += 3;
        }
        return bytes;
    }

    private byte[] getSupportedShortAudioDescriptor(AudioDeviceInfo deviceInfo, int audioFormatCode) {
        switch (audioFormatCode) {
            case 0:
                return null;
            case 1:
                return getLpcmShortAudioDescriptor(deviceInfo);
            default:
                return null;
        }
    }

    private byte[] getLpcmShortAudioDescriptor(AudioDeviceInfo deviceInfo) {
        return null;
    }

    private AudioDeviceInfo getSystemAudioDeviceInfo() {
        AudioManager audioManager = (AudioManager) this.mService.getContext().getSystemService(AudioManager.class);
        if (audioManager == null) {
            HdmiLogger.error("Error getting system audio device because AudioManager not available.", new Object[0]);
            return null;
        }
        AudioDeviceInfo[] devices = audioManager.getDevices(1);
        HdmiLogger.debug("Found %d audio input devices", Integer.valueOf(devices.length));
        for (AudioDeviceInfo device : devices) {
            HdmiLogger.debug("%s at port %s", device.getProductName(), device.getPort());
            HdmiLogger.debug("Supported encodings are %s", Arrays.stream(device.getEncodings()).mapToObj(new IntFunction() { // from class: com.android.server.hdmi.HdmiCecLocalDeviceAudioSystem$$ExternalSyntheticLambda0
                @Override // java.util.function.IntFunction
                public final Object apply(int i) {
                    return AudioFormat.toLogFriendlyEncoding(i);
                }
            }).collect(Collectors.joining(", ")));
            if (device.getType() == 10) {
                return device;
            }
        }
        return null;
    }

    private int[] parseAudioFormatCodes(byte[] params) {
        int[] audioFormatCodes = new int[params.length];
        for (int i = 0; i < params.length; i++) {
            byte val = params[i];
            audioFormatCodes[i] = (val < 1 || val > 15) ? (byte) 0 : val;
        }
        return audioFormatCodes;
    }

    @Override // com.android.server.hdmi.HdmiCecLocalDevice
    @HdmiAnnotations.ServiceThreadOnly
    protected int handleSystemAudioModeRequest(HdmiCecMessage message) {
        assertRunOnServiceThread();
        boolean systemAudioStatusOn = message.getParams().length != 0;
        if (message.getSource() != 0) {
            if (systemAudioStatusOn) {
                return handleSystemAudioModeOnFromNonTvDevice(message);
            }
        } else {
            setTvSystemAudioModeSupport(true);
        }
        if (!checkSupportAndSetSystemAudioMode(systemAudioStatusOn)) {
            return 4;
        }
        this.mService.sendCecCommand(HdmiCecMessageBuilder.buildSetSystemAudioMode(getDeviceInfo().getLogicalAddress(), 15, systemAudioStatusOn));
        if (systemAudioStatusOn) {
            int sourcePhysicalAddress = HdmiUtils.twoBytesToInt(message.getParams());
            if (HdmiUtils.getLocalPortFromPhysicalAddress(sourcePhysicalAddress, getDeviceInfo().getPhysicalAddress()) != -1) {
                return -1;
            }
            HdmiDeviceInfo safeDeviceInfoByPath = this.mService.getHdmiCecNetwork().getSafeDeviceInfoByPath(sourcePhysicalAddress);
            if (safeDeviceInfoByPath == null) {
                switchInputOnReceivingNewActivePath(sourcePhysicalAddress);
            }
        }
        return -1;
    }

    @Override // com.android.server.hdmi.HdmiCecLocalDevice
    @HdmiAnnotations.ServiceThreadOnly
    protected int handleSetSystemAudioMode(HdmiCecMessage message) {
        assertRunOnServiceThread();
        if (!checkSupportAndSetSystemAudioMode(HdmiUtils.parseCommandParamSystemAudioStatus(message))) {
            return 4;
        }
        return -1;
    }

    @Override // com.android.server.hdmi.HdmiCecLocalDevice
    @HdmiAnnotations.ServiceThreadOnly
    protected int handleSystemAudioModeStatus(HdmiCecMessage message) {
        assertRunOnServiceThread();
        if (!checkSupportAndSetSystemAudioMode(HdmiUtils.parseCommandParamSystemAudioStatus(message))) {
            return 4;
        }
        return -1;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @HdmiAnnotations.ServiceThreadOnly
    public void setArcStatus(boolean enabled) {
        assertRunOnServiceThread();
        HdmiLogger.debug("Set Arc Status[old:%b new:%b]", Boolean.valueOf(this.mArcEstablished), Boolean.valueOf(enabled));
        enableAudioReturnChannel(enabled);
        notifyArcStatusToAudioService(enabled);
        this.mArcEstablished = enabled;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void processArcTermination() {
        setArcStatus(false);
        if (getLocalActivePort() == 17) {
            routeToInputFromPortId(getRoutingPort());
        }
    }

    @HdmiAnnotations.ServiceThreadOnly
    private void enableAudioReturnChannel(boolean enabled) {
        assertRunOnServiceThread();
        this.mService.enableAudioReturnChannel(Integer.parseInt((String) HdmiProperties.arc_port().orElse("0")), enabled);
    }

    private void notifyArcStatusToAudioService(boolean enabled) {
        this.mService.getAudioManager().setWiredDeviceConnectionState(-2147483616, enabled ? 1 : 0, "", "");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void reportAudioStatus(int source) {
        assertRunOnServiceThread();
        if (this.mService.getHdmiCecVolumeControl() == 0) {
            return;
        }
        int volume = this.mService.getAudioManager().getStreamVolume(3);
        boolean mute = this.mService.getAudioManager().isStreamMute(3);
        int maxVolume = this.mService.getAudioManager().getStreamMaxVolume(3);
        int minVolume = this.mService.getAudioManager().getStreamMinVolume(3);
        int scaledVolume = VolumeControlAction.scaleToCecVolume(volume, maxVolume);
        HdmiLogger.debug("Reporting volume %d (%d-%d) as CEC volume %d", Integer.valueOf(volume), Integer.valueOf(minVolume), Integer.valueOf(maxVolume), Integer.valueOf(scaledVolume));
        this.mService.sendCecCommand(HdmiCecMessageBuilder.buildReportAudioStatus(getDeviceInfo().getLogicalAddress(), source, scaledVolume, mute));
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public boolean checkSupportAndSetSystemAudioMode(boolean newSystemAudioMode) {
        if (!isSystemAudioControlFeatureEnabled()) {
            HdmiLogger.debug("Cannot turn " + (newSystemAudioMode ? "on" : "off") + "system audio mode because the System Audio Control feature is disabled.", new Object[0]);
            return false;
        }
        HdmiLogger.debug("System Audio Mode change[old:%b new:%b]", Boolean.valueOf(isSystemAudioActivated()), Boolean.valueOf(newSystemAudioMode));
        if (newSystemAudioMode) {
            this.mService.wakeUp();
        }
        setSystemAudioMode(newSystemAudioMode);
        return true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setSystemAudioMode(boolean newSystemAudioMode) {
        int i;
        int targetPhysicalAddress = getActiveSource().physicalAddress;
        int port = this.mService.pathToPortId(targetPhysicalAddress);
        if (newSystemAudioMode && port >= 0) {
            switchToAudioInput();
        }
        boolean systemAudioModeMutingEnabled = this.mService.getHdmiCecConfig().getIntValue("system_audio_mode_muting") == 1;
        boolean currentMuteStatus = this.mService.getAudioManager().isStreamMute(3);
        if (currentMuteStatus == newSystemAudioMode && (systemAudioModeMutingEnabled || newSystemAudioMode)) {
            AudioManager audioManager = this.mService.getAudioManager();
            if (newSystemAudioMode) {
                i = 100;
            } else {
                i = -100;
            }
            audioManager.adjustStreamVolume(3, i, 0);
        }
        updateAudioManagerForSystemAudio(newSystemAudioMode);
        synchronized (this.mLock) {
            if (isSystemAudioActivated() != newSystemAudioMode) {
                this.mService.setSystemAudioActivated(newSystemAudioMode);
                this.mService.announceSystemAudioModeChange(newSystemAudioMode);
            }
        }
        if (this.mArcIntentUsed && !systemAudioModeMutingEnabled && !newSystemAudioMode && getLocalActivePort() == 17) {
            routeToInputFromPortId(getRoutingPort());
        }
        if (SystemProperties.getBoolean("persist.sys.hdmi.property_arc_support", true) && isDirectConnectToTv() && this.mService.isSystemAudioActivated() && !hasAction(ArcInitiationActionFromAvr.class)) {
            addAndStartAction(new ArcInitiationActionFromAvr(this));
        }
    }

    protected void switchToAudioInput() {
    }

    protected boolean isDirectConnectToTv() {
        int myPhysicalAddress = this.mService.getPhysicalAddress();
        return (61440 & myPhysicalAddress) == myPhysicalAddress;
    }

    private void updateAudioManagerForSystemAudio(boolean on) {
        int device = this.mService.getAudioManager().setHdmiSystemAudioSupported(on);
        HdmiLogger.debug("[A]UpdateSystemAudio mode[on=%b] output=[%X]", Boolean.valueOf(on), Integer.valueOf(device));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onSystemAudioControlFeatureSupportChanged(boolean enabled) {
        setSystemAudioControlFeatureEnabled(enabled);
        if (enabled) {
            addAndStartAction(new SystemAudioInitiationActionFromAvr(this));
        }
    }

    @HdmiAnnotations.ServiceThreadOnly
    void setSystemAudioControlFeatureEnabled(boolean enabled) {
        assertRunOnServiceThread();
        synchronized (this.mLock) {
            this.mSystemAudioControlFeatureEnabled = enabled;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @HdmiAnnotations.ServiceThreadOnly
    public void setRoutingControlFeatureEnabled(boolean enabled) {
        assertRunOnServiceThread();
        synchronized (this.mLock) {
            this.mRoutingControlFeatureEnabled = enabled;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @HdmiAnnotations.ServiceThreadOnly
    public void doManualPortSwitching(int portId, IHdmiControlCallback callback) {
        int oldPath;
        assertRunOnServiceThread();
        if (!this.mService.isValidPortId(portId)) {
            invokeCallback(callback, 3);
        } else if (portId == getLocalActivePort()) {
            invokeCallback(callback, 0);
        } else if (!this.mService.isControlEnabled()) {
            setRoutingPort(portId);
            setLocalActivePort(portId);
            invokeCallback(callback, 6);
        } else {
            if (getRoutingPort() != 0) {
                oldPath = this.mService.portIdToPath(getRoutingPort());
            } else {
                oldPath = getDeviceInfo().getPhysicalAddress();
            }
            int newPath = this.mService.portIdToPath(portId);
            if (oldPath == newPath) {
                return;
            }
            setRoutingPort(portId);
            setLocalActivePort(portId);
            HdmiCecMessage routingChange = HdmiCecMessageBuilder.buildRoutingChange(getDeviceInfo().getLogicalAddress(), oldPath, newPath);
            this.mService.sendCecCommand(routingChange);
        }
    }

    boolean isSystemAudioControlFeatureEnabled() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mSystemAudioControlFeatureEnabled;
        }
        return z;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public boolean isSystemAudioActivated() {
        return this.mService.isSystemAudioActivated();
    }

    protected void terminateSystemAudioMode() {
        removeAction(SystemAudioInitiationActionFromAvr.class);
        if (isSystemAudioActivated() && checkSupportAndSetSystemAudioMode(false)) {
            this.mService.sendCecCommand(HdmiCecMessageBuilder.buildSetSystemAudioMode(getDeviceInfo().getLogicalAddress(), 15, false));
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void queryTvSystemAudioModeSupport(TvSystemAudioModeSupportedCallback callback) {
        Boolean bool = this.mTvSystemAudioModeSupport;
        if (bool == null) {
            addAndStartAction(new DetectTvSystemAudioModeSupportAction(this, callback));
        } else {
            callback.onResult(bool.booleanValue());
        }
    }

    int handleSystemAudioModeOnFromNonTvDevice(final HdmiCecMessage message) {
        if (!isSystemAudioControlFeatureEnabled()) {
            HdmiLogger.debug("Cannot turn onsystem audio mode because the System Audio Control feature is disabled.", new Object[0]);
            return 4;
        }
        this.mService.wakeUp();
        if (this.mService.pathToPortId(getActiveSource().physicalAddress) != -1) {
            setSystemAudioMode(true);
            this.mService.sendCecCommand(HdmiCecMessageBuilder.buildSetSystemAudioMode(getDeviceInfo().getLogicalAddress(), 15, true));
            return -1;
        }
        queryTvSystemAudioModeSupport(new TvSystemAudioModeSupportedCallback() { // from class: com.android.server.hdmi.HdmiCecLocalDeviceAudioSystem.2
            @Override // com.android.server.hdmi.HdmiCecLocalDeviceAudioSystem.TvSystemAudioModeSupportedCallback
            public void onResult(boolean supported) {
                if (supported) {
                    HdmiCecLocalDeviceAudioSystem.this.setSystemAudioMode(true);
                    HdmiCecLocalDeviceAudioSystem.this.mService.sendCecCommand(HdmiCecMessageBuilder.buildSetSystemAudioMode(HdmiCecLocalDeviceAudioSystem.this.getDeviceInfo().getLogicalAddress(), 15, true));
                    return;
                }
                HdmiCecLocalDeviceAudioSystem.this.mService.maySendFeatureAbortCommand(message, 4);
            }
        });
        return -1;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setTvSystemAudioModeSupport(boolean supported) {
        this.mTvSystemAudioModeSupport = Boolean.valueOf(supported);
    }

    protected boolean isArcEnabled() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mArcEstablished;
        }
        return z;
    }

    private void initArcOnFromAvr() {
        removeAction(ArcTerminationActionFromAvr.class);
        if (SystemProperties.getBoolean("persist.sys.hdmi.property_arc_support", true) && isDirectConnectToTv() && !isArcEnabled()) {
            removeAction(ArcInitiationActionFromAvr.class);
            addAndStartAction(new ArcInitiationActionFromAvr(this));
        }
    }

    @Override // com.android.server.hdmi.HdmiCecLocalDeviceSource
    protected void switchInputOnReceivingNewActivePath(int physicalAddress) {
        int port = this.mService.pathToPortId(physicalAddress);
        if (isSystemAudioActivated() && port < 0) {
            routeToInputFromPortId(17);
        } else if (this.mIsSwitchDevice && port >= 0) {
            routeToInputFromPortId(port);
        }
    }

    protected void routeToInputFromPortId(int portId) {
        if (!isRoutingControlFeatureEnabled()) {
            HdmiLogger.debug("Routing Control Feature is not enabled.", new Object[0]);
        } else if (this.mArcIntentUsed) {
            routeToTvInputFromPortId(portId);
        }
    }

    protected void routeToTvInputFromPortId(int portId) {
        if (portId < 0 || portId >= 21) {
            HdmiLogger.debug("Invalid port number for Tv Input switching.", new Object[0]);
            return;
        }
        this.mService.wakeUp();
        if (getLocalActivePort() == portId && portId != 17) {
            HdmiLogger.debug("Not switching to the same port " + portId + " except for arc", new Object[0]);
            return;
        }
        if (portId == 0 && this.mService.isPlaybackDevice()) {
            switchToHomeTvInput();
        } else if (portId == 17) {
            switchToTvInput((String) HdmiProperties.arc_port().orElse("0"));
            setLocalActivePort(portId);
            return;
        } else {
            String uri = this.mPortIdToTvInputs.get(Integer.valueOf(portId));
            if (uri != null) {
                switchToTvInput(uri);
            } else {
                HdmiLogger.debug("Port number does not match any Tv Input.", new Object[0]);
                return;
            }
        }
        setLocalActivePort(portId);
        setRoutingPort(portId);
    }

    private void switchToTvInput(String uri) {
        try {
            this.mService.getContext().startActivity(new Intent("android.intent.action.VIEW", TvContract.buildChannelUriForPassthroughInput(uri)).addFlags(268435456));
        } catch (ActivityNotFoundException e) {
            Slog.e(TAG, "Can't find activity to switch to " + uri, e);
        }
    }

    private void switchToHomeTvInput() {
        try {
            Intent activityIntent = new Intent("android.intent.action.MAIN").addCategory("android.intent.category.HOME").setFlags(872480768);
            this.mService.getContext().startActivity(activityIntent);
        } catch (ActivityNotFoundException e) {
            Slog.e(TAG, "Can't find activity to switch to HOME", e);
        }
    }

    @Override // com.android.server.hdmi.HdmiCecLocalDeviceSource
    protected void handleRoutingChangeAndInformation(int physicalAddress, HdmiCecMessage message) {
        int port = this.mService.pathToPortId(physicalAddress);
        if (port > 0) {
            return;
        }
        if (port < 0 && isSystemAudioActivated()) {
            handleRoutingChangeAndInformationForSystemAudio();
        } else if (port == 0) {
            handleRoutingChangeAndInformationForSwitch(message);
        }
    }

    private void handleRoutingChangeAndInformationForSystemAudio() {
        routeToInputFromPortId(17);
    }

    private void handleRoutingChangeAndInformationForSwitch(HdmiCecMessage message) {
        if (getRoutingPort() == 0 && this.mService.isPlaybackDevice()) {
            routeToInputFromPortId(0);
            this.mService.setAndBroadcastActiveSourceFromOneDeviceType(message.getSource(), this.mService.getPhysicalAddress(), "HdmiCecLocalDeviceAudioSystem#handleRoutingChangeAndInformationForSwitch()");
            return;
        }
        int routingInformationPath = this.mService.portIdToPath(getRoutingPort());
        if (routingInformationPath == this.mService.getPhysicalAddress()) {
            HdmiLogger.debug("Current device can't assign valid physical addressto devices under it any more. It's physical address is " + routingInformationPath, new Object[0]);
            return;
        }
        this.mService.sendCecCommand(HdmiCecMessageBuilder.buildRoutingInformation(getDeviceInfo().getLogicalAddress(), routingInformationPath));
        routeToInputFromPortId(getRoutingPort());
    }

    @HdmiAnnotations.ServiceThreadOnly
    private void launchDeviceDiscovery() {
        assertRunOnServiceThread();
        if (hasAction(DeviceDiscoveryAction.class)) {
            Slog.i(TAG, "Device Discovery Action is in progress. Restarting.");
            removeAction(DeviceDiscoveryAction.class);
        }
        DeviceDiscoveryAction action = new DeviceDiscoveryAction(this, new DeviceDiscoveryAction.DeviceDiscoveryCallback() { // from class: com.android.server.hdmi.HdmiCecLocalDeviceAudioSystem.3
            @Override // com.android.server.hdmi.DeviceDiscoveryAction.DeviceDiscoveryCallback
            public void onDeviceDiscoveryDone(List<HdmiDeviceInfo> deviceInfos) {
                for (HdmiDeviceInfo info : deviceInfos) {
                    HdmiCecLocalDeviceAudioSystem.this.mService.getHdmiCecNetwork().addCecDevice(info);
                }
            }
        });
        addAndStartAction(action);
    }

    @Override // com.android.server.hdmi.HdmiCecLocalDevice
    protected void dump(IndentingPrintWriter pw) {
        pw.println("HdmiCecLocalDeviceAudioSystem:");
        pw.increaseIndent();
        pw.println("isRoutingFeatureEnabled " + isRoutingControlFeatureEnabled());
        pw.println("mSystemAudioControlFeatureEnabled: " + this.mSystemAudioControlFeatureEnabled);
        pw.println("mTvSystemAudioModeSupport: " + this.mTvSystemAudioModeSupport);
        pw.println("mArcEstablished: " + this.mArcEstablished);
        pw.println("mArcIntentUsed: " + this.mArcIntentUsed);
        pw.println("mRoutingPort: " + getRoutingPort());
        pw.println("mLocalActivePort: " + getLocalActivePort());
        HdmiUtils.dumpMap(pw, "mPortIdToTvInputs:", this.mPortIdToTvInputs);
        HdmiUtils.dumpMap(pw, "mTvInputsToDeviceInfo:", this.mTvInputsToDeviceInfo);
        pw.decreaseIndent();
        super.dump(pw);
    }
}
