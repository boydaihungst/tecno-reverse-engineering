package com.android.server.hdmi;

import android.hardware.hdmi.HdmiDeviceInfo;
import android.hardware.hdmi.HdmiPortInfo;
import android.os.Handler;
import android.os.Looper;
import android.util.ArraySet;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseIntArray;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.hdmi.HdmiAnnotations;
import com.android.server.hdmi.HdmiCecController;
import com.android.server.location.gnss.hal.GnssNative;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
/* loaded from: classes.dex */
public class HdmiCecNetwork {
    private static final String TAG = "HdmiCecNetwork";
    private final Handler mHandler;
    private final HdmiCecController mHdmiCecController;
    private final HdmiControlService mHdmiControlService;
    private final HdmiMhlControllerStub mHdmiMhlController;
    protected final Object mLock;
    private UnmodifiableSparseArray<HdmiDeviceInfo> mPortDeviceMap;
    private UnmodifiableSparseIntArray mPortIdMap;
    private UnmodifiableSparseArray<HdmiPortInfo> mPortInfoMap;
    private final SparseArray<HdmiCecLocalDevice> mLocalDevices = new SparseArray<>();
    private final SparseArray<HdmiDeviceInfo> mDeviceInfos = new SparseArray<>();
    private final ArraySet<Integer> mCecSwitches = new ArraySet<>();
    private List<HdmiDeviceInfo> mSafeAllDeviceInfos = Collections.emptyList();
    private List<HdmiDeviceInfo> mSafeExternalInputs = Collections.emptyList();
    private List<HdmiPortInfo> mPortInfo = Collections.emptyList();

    /* JADX INFO: Access modifiers changed from: package-private */
    public HdmiCecNetwork(HdmiControlService hdmiControlService, HdmiCecController hdmiCecController, HdmiMhlControllerStub hdmiMhlController) {
        this.mHdmiControlService = hdmiControlService;
        this.mHdmiCecController = hdmiCecController;
        this.mHdmiMhlController = hdmiMhlController;
        this.mHandler = new Handler(hdmiControlService.getServiceLooper());
        this.mLock = hdmiControlService.getServiceLock();
    }

    private static boolean isConnectedToCecSwitch(int path, Collection<Integer> switches) {
        for (Integer num : switches) {
            int switchPath = num.intValue();
            if (isParentPath(switchPath, path)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isParentPath(int parentPath, int childPath) {
        for (int i = 0; i <= 12; i += 4) {
            int nibble = (childPath >> i) & 15;
            if (nibble != 0) {
                int parentNibble = (parentPath >> i) & 15;
                return parentNibble == 0 && (childPath >> (i + 4)) == (parentPath >> (i + 4));
            }
        }
        return false;
    }

    public void addLocalDevice(int deviceType, HdmiCecLocalDevice device) {
        this.mLocalDevices.put(deviceType, device);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public HdmiCecLocalDevice getLocalDevice(int deviceType) {
        return this.mLocalDevices.get(deviceType);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @HdmiAnnotations.ServiceThreadOnly
    public List<HdmiCecLocalDevice> getLocalDeviceList() {
        assertRunOnServiceThread();
        return HdmiUtils.sparseArrayToList(this.mLocalDevices);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @HdmiAnnotations.ServiceThreadOnly
    public boolean isAllocatedLocalDeviceAddress(int address) {
        assertRunOnServiceThread();
        for (int i = 0; i < this.mLocalDevices.size(); i++) {
            if (this.mLocalDevices.valueAt(i).isAddressOf(address)) {
                return true;
            }
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @HdmiAnnotations.ServiceThreadOnly
    public void clearLocalDevices() {
        assertRunOnServiceThread();
        this.mLocalDevices.clear();
    }

    public HdmiDeviceInfo getDeviceInfo(int id) {
        return this.mDeviceInfos.get(id);
    }

    @HdmiAnnotations.ServiceThreadOnly
    private HdmiDeviceInfo addDeviceInfo(HdmiDeviceInfo deviceInfo) {
        assertRunOnServiceThread();
        HdmiDeviceInfo oldDeviceInfo = getCecDeviceInfo(deviceInfo.getLogicalAddress());
        this.mHdmiControlService.checkLogicalAddressConflictAndReallocate(deviceInfo.getLogicalAddress(), deviceInfo.getPhysicalAddress());
        if (oldDeviceInfo != null) {
            removeDeviceInfo(deviceInfo.getId());
        }
        this.mDeviceInfos.append(deviceInfo.getId(), deviceInfo);
        updateSafeDeviceInfoList();
        return oldDeviceInfo;
    }

    @HdmiAnnotations.ServiceThreadOnly
    private HdmiDeviceInfo removeDeviceInfo(int id) {
        assertRunOnServiceThread();
        HdmiDeviceInfo deviceInfo = this.mDeviceInfos.get(id);
        if (deviceInfo != null) {
            this.mDeviceInfos.remove(id);
        }
        updateSafeDeviceInfoList();
        return deviceInfo;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @HdmiAnnotations.ServiceThreadOnly
    public HdmiDeviceInfo getCecDeviceInfo(int logicalAddress) {
        assertRunOnServiceThread();
        return this.mDeviceInfos.get(HdmiDeviceInfo.idForCecDevice(logicalAddress));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @HdmiAnnotations.ServiceThreadOnly
    public final void addCecDevice(HdmiDeviceInfo info) {
        assertRunOnServiceThread();
        HdmiDeviceInfo old = addDeviceInfo(info);
        if (isLocalDeviceAddress(info.getLogicalAddress())) {
            return;
        }
        this.mHdmiControlService.checkAndUpdateAbsoluteVolumeControlState();
        if (info.getPhysicalAddress() == 65535) {
            return;
        }
        if (old == null || old.getPhysicalAddress() == 65535) {
            invokeDeviceEventListener(info, 1);
        } else if (!old.equals(info)) {
            invokeDeviceEventListener(old, 2);
            invokeDeviceEventListener(info, 1);
        }
    }

    private void invokeDeviceEventListener(HdmiDeviceInfo info, int event) {
        if (!hideDevicesBehindLegacySwitch(info)) {
            this.mHdmiControlService.invokeDeviceEventListeners(info, event);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @HdmiAnnotations.ServiceThreadOnly
    public final void updateCecDevice(HdmiDeviceInfo info) {
        assertRunOnServiceThread();
        HdmiDeviceInfo old = addDeviceInfo(info);
        if (info.getPhysicalAddress() == 65535) {
            return;
        }
        if (old == null || old.getPhysicalAddress() == 65535) {
            invokeDeviceEventListener(info, 1);
        } else if (!old.equals(info)) {
            invokeDeviceEventListener(info, 3);
        }
    }

    @HdmiAnnotations.ServiceThreadOnly
    private void updateSafeDeviceInfoList() {
        assertRunOnServiceThread();
        List<HdmiDeviceInfo> copiedDevices = HdmiUtils.sparseArrayToList(this.mDeviceInfos);
        List<HdmiDeviceInfo> externalInputs = getInputDevices();
        this.mSafeAllDeviceInfos = copiedDevices;
        this.mSafeExternalInputs = externalInputs;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @HdmiAnnotations.ServiceThreadOnly
    public List<HdmiDeviceInfo> getDeviceInfoList(boolean includeLocalDevice) {
        assertRunOnServiceThread();
        if (includeLocalDevice) {
            return HdmiUtils.sparseArrayToList(this.mDeviceInfos);
        }
        ArrayList<HdmiDeviceInfo> infoList = new ArrayList<>();
        for (int i = 0; i < this.mDeviceInfos.size(); i++) {
            HdmiDeviceInfo info = this.mDeviceInfos.valueAt(i);
            if (!isLocalDeviceAddress(info.getLogicalAddress())) {
                infoList.add(info);
            }
        }
        return infoList;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public List<HdmiDeviceInfo> getSafeExternalInputsLocked() {
        return this.mSafeExternalInputs;
    }

    private List<HdmiDeviceInfo> getInputDevices() {
        ArrayList<HdmiDeviceInfo> infoList = new ArrayList<>();
        for (int i = 0; i < this.mDeviceInfos.size(); i++) {
            HdmiDeviceInfo info = this.mDeviceInfos.valueAt(i);
            if (!isLocalDeviceAddress(info.getLogicalAddress()) && info.isSourceType() && !hideDevicesBehindLegacySwitch(info)) {
                infoList.add(info);
            }
        }
        return infoList;
    }

    private boolean hideDevicesBehindLegacySwitch(HdmiDeviceInfo info) {
        return (!isLocalDeviceAddress(0) || isConnectedToCecSwitch(info.getPhysicalAddress(), getCecSwitches()) || info.getPhysicalAddress() == 65535) ? false : true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @HdmiAnnotations.ServiceThreadOnly
    public final void removeCecDevice(HdmiCecLocalDevice localDevice, int address) {
        assertRunOnServiceThread();
        HdmiDeviceInfo info = removeDeviceInfo(HdmiDeviceInfo.idForCecDevice(address));
        this.mHdmiControlService.checkAndUpdateAbsoluteVolumeControlState();
        localDevice.mCecMessageCache.flushMessagesFrom(address);
        if (info.getPhysicalAddress() == 65535) {
            return;
        }
        invokeDeviceEventListener(info, 2);
    }

    public void updateDevicePowerStatus(int logicalAddress, int newPowerStatus) {
        HdmiDeviceInfo info = getCecDeviceInfo(logicalAddress);
        if (info == null) {
            Slog.w(TAG, "Can not update power status of non-existing device:" + logicalAddress);
        } else if (info.getDevicePowerStatus() == newPowerStatus) {
        } else {
            updateCecDevice(info.toBuilder().setDevicePowerStatus(newPowerStatus).build());
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isConnectedToArcPort(int physicalAddress) {
        int portId = physicalAddressToPortId(physicalAddress);
        if (portId != -1 && portId != 0) {
            return this.mPortInfoMap.get(portId).isArcSupported();
        }
        return false;
    }

    @HdmiAnnotations.ServiceThreadOnly
    public void initPortInfo() {
        assertRunOnServiceThread();
        HdmiPortInfo[] cecPortInfo = null;
        HdmiCecController hdmiCecController = this.mHdmiCecController;
        if (hdmiCecController != null) {
            cecPortInfo = hdmiCecController.getPortInfos();
        }
        if (cecPortInfo == null) {
            return;
        }
        SparseArray<HdmiPortInfo> portInfoMap = new SparseArray<>();
        SparseIntArray portIdMap = new SparseIntArray();
        SparseArray<HdmiDeviceInfo> portDeviceMap = new SparseArray<>();
        for (HdmiPortInfo info : cecPortInfo) {
            portIdMap.put(info.getAddress(), info.getId());
            portInfoMap.put(info.getId(), info);
            portDeviceMap.put(info.getId(), HdmiDeviceInfo.hardwarePort(info.getAddress(), info.getId()));
        }
        this.mPortIdMap = new UnmodifiableSparseIntArray(portIdMap);
        this.mPortInfoMap = new UnmodifiableSparseArray<>(portInfoMap);
        this.mPortDeviceMap = new UnmodifiableSparseArray<>(portDeviceMap);
        HdmiMhlControllerStub hdmiMhlControllerStub = this.mHdmiMhlController;
        if (hdmiMhlControllerStub == null) {
            return;
        }
        HdmiPortInfo[] mhlPortInfo = hdmiMhlControllerStub.getPortInfos();
        ArraySet<Integer> mhlSupportedPorts = new ArraySet<>(mhlPortInfo.length);
        for (HdmiPortInfo info2 : mhlPortInfo) {
            if (info2.isMhlSupported()) {
                mhlSupportedPorts.add(Integer.valueOf(info2.getId()));
            }
        }
        if (mhlSupportedPorts.isEmpty()) {
            setPortInfo(Collections.unmodifiableList(Arrays.asList(cecPortInfo)));
            return;
        }
        ArrayList<HdmiPortInfo> result = new ArrayList<>(cecPortInfo.length);
        for (HdmiPortInfo info3 : cecPortInfo) {
            if (mhlSupportedPorts.contains(Integer.valueOf(info3.getId()))) {
                result.add(new HdmiPortInfo(info3.getId(), info3.getType(), info3.getAddress(), info3.isCecSupported(), true, info3.isArcSupported()));
            } else {
                result.add(info3);
            }
        }
        setPortInfo(Collections.unmodifiableList(result));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public HdmiDeviceInfo getDeviceForPortId(int portId) {
        return this.mPortDeviceMap.get(portId, HdmiDeviceInfo.INACTIVE_DEVICE);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @HdmiAnnotations.ServiceThreadOnly
    public boolean isInDeviceList(int logicalAddress, int physicalAddress) {
        assertRunOnServiceThread();
        HdmiDeviceInfo device = getCecDeviceInfo(logicalAddress);
        return device != null && device.getPhysicalAddress() == physicalAddress;
    }

    private static int logicalAddressToDeviceType(int logicalAddress) {
        switch (logicalAddress) {
            case 0:
                return 0;
            case 1:
            case 2:
            case 9:
                return 1;
            case 3:
            case 6:
            case 7:
            case 10:
                return 3;
            case 4:
            case 8:
            case 11:
                return 4;
            case 5:
                return 5;
            default:
                return 2;
        }
    }

    @HdmiAnnotations.ServiceThreadOnly
    public void handleCecMessage(HdmiCecMessage message) {
        assertRunOnServiceThread();
        int sourceAddress = message.getSource();
        if (getCecDeviceInfo(sourceAddress) == null) {
            HdmiDeviceInfo newDevice = HdmiDeviceInfo.cecDeviceBuilder().setLogicalAddress(sourceAddress).setDisplayName(HdmiUtils.getDefaultDeviceName(sourceAddress)).setDeviceType(logicalAddressToDeviceType(sourceAddress)).build();
            addCecDevice(newDevice);
        }
        if (message instanceof ReportFeaturesMessage) {
            handleReportFeatures((ReportFeaturesMessage) message);
        }
        switch (message.getOpcode()) {
            case 0:
                handleFeatureAbort(message);
                return;
            case 71:
                handleSetOsdName(message);
                return;
            case 132:
                handleReportPhysicalAddress(message);
                return;
            case 135:
                handleDeviceVendorId(message);
                return;
            case 144:
                handleReportPowerStatus(message);
                return;
            case 158:
                handleCecVersion(message);
                return;
            default:
                return;
        }
    }

    @HdmiAnnotations.ServiceThreadOnly
    private void handleReportFeatures(ReportFeaturesMessage message) {
        assertRunOnServiceThread();
        HdmiDeviceInfo currentDeviceInfo = getCecDeviceInfo(message.getSource());
        HdmiDeviceInfo newDeviceInfo = currentDeviceInfo.toBuilder().setCecVersion(message.getCecVersion()).updateDeviceFeatures(message.getDeviceFeatures()).build();
        updateCecDevice(newDeviceInfo);
        this.mHdmiControlService.checkAndUpdateAbsoluteVolumeControlState();
    }

    @HdmiAnnotations.ServiceThreadOnly
    private void handleFeatureAbort(HdmiCecMessage message) {
        assertRunOnServiceThread();
        if (message.getParams().length < 2) {
            return;
        }
        int originalOpcode = message.getParams()[0] & 255;
        int reason = message.getParams()[1] & 255;
        if (originalOpcode == 115) {
            int featureSupport = reason == 0 ? 0 : 2;
            HdmiDeviceInfo currentDeviceInfo = getCecDeviceInfo(message.getSource());
            HdmiDeviceInfo newDeviceInfo = currentDeviceInfo.toBuilder().updateDeviceFeatures(currentDeviceInfo.getDeviceFeatures().toBuilder().setSetAudioVolumeLevelSupport(featureSupport).build()).build();
            updateCecDevice(newDeviceInfo);
            this.mHdmiControlService.checkAndUpdateAbsoluteVolumeControlState();
        }
    }

    @HdmiAnnotations.ServiceThreadOnly
    private void handleCecVersion(HdmiCecMessage message) {
        assertRunOnServiceThread();
        int version = Byte.toUnsignedInt(message.getParams()[0]);
        updateDeviceCecVersion(message.getSource(), version);
    }

    @HdmiAnnotations.ServiceThreadOnly
    private void handleReportPhysicalAddress(HdmiCecMessage message) {
        assertRunOnServiceThread();
        int logicalAddress = message.getSource();
        int physicalAddress = HdmiUtils.twoBytesToInt(message.getParams());
        int type = message.getParams()[2];
        if (updateCecSwitchInfo(logicalAddress, type, physicalAddress)) {
            return;
        }
        HdmiDeviceInfo deviceInfo = getCecDeviceInfo(logicalAddress);
        if (deviceInfo == null) {
            Slog.i(TAG, "Unknown source device info for <Report Physical Address> " + message);
            return;
        }
        HdmiDeviceInfo updatedDeviceInfo = deviceInfo.toBuilder().setPhysicalAddress(physicalAddress).setPortId(physicalAddressToPortId(physicalAddress)).setDeviceType(type).build();
        updateCecDevice(updatedDeviceInfo);
    }

    @HdmiAnnotations.ServiceThreadOnly
    private void handleReportPowerStatus(HdmiCecMessage message) {
        assertRunOnServiceThread();
        int newStatus = message.getParams()[0] & 255;
        updateDevicePowerStatus(message.getSource(), newStatus);
        if (message.getDestination() == 15) {
            updateDeviceCecVersion(message.getSource(), 6);
        }
    }

    @HdmiAnnotations.ServiceThreadOnly
    private void updateDeviceCecVersion(int logicalAddress, int hdmiCecVersion) {
        assertRunOnServiceThread();
        HdmiDeviceInfo deviceInfo = getCecDeviceInfo(logicalAddress);
        if (deviceInfo == null) {
            Slog.w(TAG, "Can not update CEC version of non-existing device:" + logicalAddress);
        } else if (deviceInfo.getCecVersion() == hdmiCecVersion) {
        } else {
            HdmiDeviceInfo updatedDeviceInfo = deviceInfo.toBuilder().setCecVersion(hdmiCecVersion).build();
            updateCecDevice(updatedDeviceInfo);
        }
    }

    @HdmiAnnotations.ServiceThreadOnly
    private void handleSetOsdName(HdmiCecMessage message) {
        assertRunOnServiceThread();
        int logicalAddress = message.getSource();
        HdmiDeviceInfo deviceInfo = getCecDeviceInfo(logicalAddress);
        if (deviceInfo == null) {
            Slog.i(TAG, "No source device info for <Set Osd Name>." + message);
            return;
        }
        try {
            String osdName = new String(message.getParams(), "US-ASCII");
            if (deviceInfo.getDisplayName() != null && deviceInfo.getDisplayName().equals(osdName)) {
                Slog.d(TAG, "Ignore incoming <Set Osd Name> having same osd name:" + message);
                return;
            }
            Slog.d(TAG, "Updating device OSD name from " + deviceInfo.getDisplayName() + " to " + osdName);
            HdmiDeviceInfo updatedDeviceInfo = deviceInfo.toBuilder().setDisplayName(osdName).build();
            updateCecDevice(updatedDeviceInfo);
        } catch (UnsupportedEncodingException e) {
            Slog.e(TAG, "Invalid <Set Osd Name> request:" + message, e);
        }
    }

    @HdmiAnnotations.ServiceThreadOnly
    private void handleDeviceVendorId(HdmiCecMessage message) {
        assertRunOnServiceThread();
        int logicalAddress = message.getSource();
        int vendorId = HdmiUtils.threeBytesToInt(message.getParams());
        HdmiDeviceInfo deviceInfo = getCecDeviceInfo(logicalAddress);
        if (deviceInfo == null) {
            Slog.i(TAG, "Unknown source device info for <Device Vendor ID> " + message);
            return;
        }
        HdmiDeviceInfo updatedDeviceInfo = deviceInfo.toBuilder().setVendorId(vendorId).build();
        updateCecDevice(updatedDeviceInfo);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void addCecSwitch(int physicalAddress) {
        this.mCecSwitches.add(Integer.valueOf(physicalAddress));
    }

    public ArraySet<Integer> getCecSwitches() {
        return this.mCecSwitches;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void removeCecSwitches(int portId) {
        Iterator<Integer> it = this.mCecSwitches.iterator();
        while (it.hasNext()) {
            int path = it.next().intValue();
            int devicePortId = physicalAddressToPortId(path);
            if (devicePortId == portId || devicePortId == -1) {
                it.remove();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void removeDevicesConnectedToPort(int portId) {
        removeCecSwitches(portId);
        List<Integer> toRemove = new ArrayList<>();
        for (int i = 0; i < this.mDeviceInfos.size(); i++) {
            int key = this.mDeviceInfos.keyAt(i);
            int physicalAddress = this.mDeviceInfos.get(key).getPhysicalAddress();
            int devicePortId = physicalAddressToPortId(physicalAddress);
            if (devicePortId == portId || devicePortId == -1) {
                toRemove.add(Integer.valueOf(key));
            }
        }
        for (Integer key2 : toRemove) {
            removeDeviceInfo(key2.intValue());
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean updateCecSwitchInfo(int address, int type, int path) {
        if (address == 15 && type == 6) {
            this.mCecSwitches.add(Integer.valueOf(path));
            updateSafeDeviceInfoList();
            return true;
        } else if (type == 5) {
            this.mCecSwitches.add(Integer.valueOf(path));
            return false;
        } else {
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public List<HdmiDeviceInfo> getSafeCecDevicesLocked() {
        ArrayList<HdmiDeviceInfo> infoList = new ArrayList<>();
        for (HdmiDeviceInfo info : this.mSafeAllDeviceInfos) {
            if (!isLocalDeviceAddress(info.getLogicalAddress())) {
                infoList.add(info);
            }
        }
        return infoList;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public HdmiDeviceInfo getSafeCecDeviceInfo(int logicalAddress) {
        for (HdmiDeviceInfo info : this.mSafeAllDeviceInfos) {
            if (info.isCecDevice() && info.getLogicalAddress() == logicalAddress) {
                return info;
            }
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @HdmiAnnotations.ServiceThreadOnly
    public final HdmiDeviceInfo getDeviceInfoByPath(int path) {
        assertRunOnServiceThread();
        for (HdmiDeviceInfo info : getDeviceInfoList(false)) {
            if (info.getPhysicalAddress() == path) {
                return info;
            }
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public HdmiDeviceInfo getSafeDeviceInfoByPath(int path) {
        for (HdmiDeviceInfo info : this.mSafeAllDeviceInfos) {
            if (info.getPhysicalAddress() == path) {
                return info;
            }
        }
        return null;
    }

    public int getPhysicalAddress() {
        return this.mHdmiCecController.getPhysicalAddress();
    }

    @HdmiAnnotations.ServiceThreadOnly
    public void clear() {
        assertRunOnServiceThread();
        initPortInfo();
        clearDeviceList();
        clearLocalDevices();
    }

    @HdmiAnnotations.ServiceThreadOnly
    public void clearDeviceList() {
        assertRunOnServiceThread();
        for (HdmiDeviceInfo info : HdmiUtils.sparseArrayToList(this.mDeviceInfos)) {
            if (info.getPhysicalAddress() != getPhysicalAddress() && info.getPhysicalAddress() != 65535) {
                invokeDeviceEventListener(info, 2);
            }
        }
        this.mDeviceInfos.clear();
        updateSafeDeviceInfoList();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public HdmiPortInfo getPortInfo(int portId) {
        return this.mPortInfoMap.get(portId, null);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int portIdToPath(int portId) {
        HdmiPortInfo portInfo = getPortInfo(portId);
        if (portInfo == null) {
            Slog.e(TAG, "Cannot find the port info: " + portId);
            return GnssNative.GNSS_AIDING_TYPE_ALL;
        }
        return portInfo.getAddress();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int physicalAddressToPortId(int path) {
        int physicalAddress = getPhysicalAddress();
        if (path == physicalAddress) {
            return 0;
        }
        int mask = 61440;
        int finalMask = 61440;
        int maskedAddress = physicalAddress;
        while (maskedAddress != 0) {
            maskedAddress = physicalAddress & mask;
            finalMask |= mask;
            mask >>= 4;
        }
        int portAddress = path & finalMask;
        return this.mPortIdMap.get(portAddress, -1);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public List<HdmiPortInfo> getPortInfo() {
        return this.mPortInfo;
    }

    void setPortInfo(List<HdmiPortInfo> portInfo) {
        this.mPortInfo = portInfo;
    }

    private boolean isLocalDeviceAddress(int address) {
        for (int i = 0; i < this.mLocalDevices.size(); i++) {
            int key = this.mLocalDevices.keyAt(i);
            if (this.mLocalDevices.get(key).getDeviceInfo().getLogicalAddress() == address) {
                return true;
            }
        }
        return false;
    }

    private void assertRunOnServiceThread() {
        if (Looper.myLooper() != this.mHandler.getLooper()) {
            throw new IllegalStateException("Should run on service thread.");
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void dump(IndentingPrintWriter pw) {
        pw.println("HDMI CEC Network");
        pw.increaseIndent();
        HdmiUtils.dumpIterable(pw, "mPortInfo:", this.mPortInfo);
        for (int i = 0; i < this.mLocalDevices.size(); i++) {
            pw.println("HdmiCecLocalDevice #" + this.mLocalDevices.keyAt(i) + ":");
            pw.increaseIndent();
            this.mLocalDevices.valueAt(i).dump(pw);
            pw.println("Active Source history:");
            pw.increaseIndent();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            ArrayBlockingQueue<HdmiCecController.Dumpable> activeSourceHistory = this.mLocalDevices.valueAt(i).getActiveSourceHistory();
            Iterator<HdmiCecController.Dumpable> it = activeSourceHistory.iterator();
            while (it.hasNext()) {
                HdmiCecController.Dumpable activeSourceEvent = it.next();
                activeSourceEvent.dump(pw, sdf);
            }
            pw.decreaseIndent();
            pw.decreaseIndent();
        }
        HdmiUtils.dumpIterable(pw, "mDeviceInfos:", this.mSafeAllDeviceInfos);
        pw.decreaseIndent();
    }
}
