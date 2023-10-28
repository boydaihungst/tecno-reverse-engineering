package com.android.server.hdmi;

import android.hardware.hdmi.HdmiDeviceInfo;
import android.util.Slog;
import com.android.server.hdmi.HdmiControlService;
import java.util.BitSet;
import java.util.List;
/* loaded from: classes.dex */
final class HotplugDetectionAction extends HdmiCecFeatureAction {
    private static final int AVR_COUNT_MAX = 3;
    private static final int NUM_OF_ADDRESS = 15;
    public static final int POLLING_INTERVAL_MS_FOR_PLAYBACK = 60000;
    public static final int POLLING_INTERVAL_MS_FOR_TV = 5000;
    private static final int STATE_WAIT_FOR_NEXT_POLLING = 1;
    private static final String TAG = "HotPlugDetectionAction";
    public static final int TIMEOUT_COUNT = 3;
    private int mAvrStatusCount;
    private final boolean mIsTvDevice;
    private int mTimeoutCount;

    /* JADX INFO: Access modifiers changed from: package-private */
    public HotplugDetectionAction(HdmiCecLocalDevice source) {
        super(source);
        this.mTimeoutCount = 0;
        this.mAvrStatusCount = 0;
        this.mIsTvDevice = localDevice().mService.isTvDevice();
    }

    private int getPollingInterval() {
        return this.mIsTvDevice ? 5000 : 60000;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.hdmi.HdmiCecFeatureAction
    public boolean start() {
        Slog.v(TAG, "Hot-plug detection started.");
        this.mState = 1;
        this.mTimeoutCount = 0;
        addTimer(this.mState, getPollingInterval());
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.hdmi.HdmiCecFeatureAction
    public boolean processCommand(HdmiCecMessage cmd) {
        return false;
    }

    @Override // com.android.server.hdmi.HdmiCecFeatureAction
    void handleTimerEvent(int state) {
        if (this.mState == state && this.mState == 1) {
            if (this.mIsTvDevice) {
                int i = (this.mTimeoutCount + 1) % 3;
                this.mTimeoutCount = i;
                if (i == 0) {
                    pollAllDevices();
                } else if (tv().isSystemAudioActivated()) {
                    pollAudioSystem();
                }
                addTimer(this.mState, 5000);
                return;
            }
            pollAllDevices();
            addTimer(this.mState, 60000);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void pollAllDevicesNow() {
        this.mActionTimer.clearTimerMessage();
        this.mTimeoutCount = 0;
        this.mState = 1;
        pollAllDevices();
        addTimer(this.mState, getPollingInterval());
    }

    private void pollAllDevices() {
        Slog.v(TAG, "Poll all devices.");
        pollDevices(new HdmiControlService.DevicePollingCallback() { // from class: com.android.server.hdmi.HotplugDetectionAction.1
            @Override // com.android.server.hdmi.HdmiControlService.DevicePollingCallback
            public void onPollingFinished(List<Integer> ackedAddress) {
                HotplugDetectionAction.this.checkHotplug(ackedAddress, false);
            }
        }, 65537, 1);
    }

    private void pollAudioSystem() {
        Slog.v(TAG, "Poll audio system.");
        pollDevices(new HdmiControlService.DevicePollingCallback() { // from class: com.android.server.hdmi.HotplugDetectionAction.2
            @Override // com.android.server.hdmi.HdmiControlService.DevicePollingCallback
            public void onPollingFinished(List<Integer> ackedAddress) {
                HotplugDetectionAction.this.checkHotplug(ackedAddress, true);
            }
        }, 65538, 1);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void checkHotplug(List<Integer> ackedAddress, boolean audioOnly) {
        HdmiDeviceInfo avr;
        List<HdmiDeviceInfo> deviceInfoList = localDevice().mService.getHdmiCecNetwork().getDeviceInfoList(false);
        BitSet currentInfos = infoListToBitSet(deviceInfoList, audioOnly, false);
        BitSet polledResult = addressListToBitSet(ackedAddress);
        BitSet removed = complement(currentInfos, polledResult);
        int index = -1;
        while (true) {
            int nextSetBit = removed.nextSetBit(index + 1);
            index = nextSetBit;
            if (nextSetBit == -1) {
                break;
            }
            if (this.mIsTvDevice && index == 5 && (avr = tv().getAvrDeviceInfo()) != null && tv().isConnected(avr.getPortId())) {
                this.mAvrStatusCount++;
                Slog.w(TAG, "Ack not returned from AVR. count: " + this.mAvrStatusCount);
                if (this.mAvrStatusCount < 3) {
                }
            }
            Slog.v(TAG, "Remove device by hot-plug detection:" + index);
            removeDevice(index);
        }
        if (!removed.get(5)) {
            this.mAvrStatusCount = 0;
        }
        BitSet currentInfosWithPhysicalAddress = infoListToBitSet(deviceInfoList, audioOnly, true);
        BitSet added = complement(polledResult, currentInfosWithPhysicalAddress);
        int index2 = -1;
        while (true) {
            int nextSetBit2 = added.nextSetBit(index2 + 1);
            index2 = nextSetBit2;
            if (nextSetBit2 != -1) {
                Slog.v(TAG, "Add device by hot-plug detection:" + index2);
                addDevice(index2);
            } else {
                return;
            }
        }
    }

    private static BitSet infoListToBitSet(List<HdmiDeviceInfo> infoList, boolean audioOnly, boolean requirePhysicalAddress) {
        BitSet set = new BitSet(15);
        for (HdmiDeviceInfo info : infoList) {
            boolean requirePhysicalAddressConditionMet = false;
            boolean audioOnlyConditionMet = !audioOnly || info.getDeviceType() == 5;
            requirePhysicalAddressConditionMet = (requirePhysicalAddress && info.getPhysicalAddress() == 65535) ? true : true;
            if (audioOnlyConditionMet && requirePhysicalAddressConditionMet) {
                set.set(info.getLogicalAddress());
            }
        }
        return set;
    }

    private static BitSet addressListToBitSet(List<Integer> list) {
        BitSet set = new BitSet(15);
        for (Integer value : list) {
            set.set(value.intValue());
        }
        return set;
    }

    private static BitSet complement(BitSet first, BitSet second) {
        BitSet clone = (BitSet) first.clone();
        clone.andNot(second);
        return clone;
    }

    private void addDevice(int addedAddress) {
        sendCommand(HdmiCecMessageBuilder.buildGivePhysicalAddress(getSourceAddress(), addedAddress));
    }

    private void removeDevice(int removedAddress) {
        if (this.mIsTvDevice) {
            mayChangeRoutingPath(removedAddress);
            mayCancelOneTouchRecord(removedAddress);
            mayDisableSystemAudioAndARC(removedAddress);
        }
        mayCancelDeviceSelect(removedAddress);
        localDevice().mService.getHdmiCecNetwork().removeCecDevice(localDevice(), removedAddress);
    }

    private void mayChangeRoutingPath(int address) {
        HdmiDeviceInfo info = localDevice().mService.getHdmiCecNetwork().getCecDeviceInfo(address);
        if (info != null) {
            tv().handleRemoveActiveRoutingPath(info.getPhysicalAddress());
        }
    }

    private void mayCancelDeviceSelect(int address) {
        List<DeviceSelectActionFromTv> actionsFromTv = getActions(DeviceSelectActionFromTv.class);
        for (DeviceSelectActionFromTv action : actionsFromTv) {
            if (action.getTargetAddress() == address) {
                removeAction(DeviceSelectActionFromTv.class);
            }
        }
        List<DeviceSelectActionFromPlayback> actionsFromPlayback = getActions(DeviceSelectActionFromPlayback.class);
        for (DeviceSelectActionFromPlayback action2 : actionsFromPlayback) {
            if (action2.getTargetAddress() == address) {
                removeAction(DeviceSelectActionFromTv.class);
            }
        }
    }

    private void mayCancelOneTouchRecord(int address) {
        List<OneTouchRecordAction> actions = getActions(OneTouchRecordAction.class);
        for (OneTouchRecordAction action : actions) {
            if (action.getRecorderAddress() == address) {
                removeAction(action);
            }
        }
    }

    private void mayDisableSystemAudioAndARC(int address) {
        if (!HdmiUtils.isEligibleAddressForDevice(5, address)) {
            return;
        }
        tv().setSystemAudioMode(false);
        if (tv().isArcEstablished()) {
            tv().enableAudioReturnChannel(false);
            addAndStartAction(new RequestArcTerminationAction(localDevice(), address));
        }
    }
}
