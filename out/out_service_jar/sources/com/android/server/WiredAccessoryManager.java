package com.android.server;

import android.content.Context;
import android.hardware.audio.common.V2_0.AudioDevice;
import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.UEventObserver;
import android.util.Log;
import android.util.Pair;
import android.util.Slog;
import com.android.server.ExtconUEventObserver;
import com.android.server.input.InputManagerService;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public final class WiredAccessoryManager implements InputManagerService.WiredAccessoryCallbacks {
    private static final int BIT_HDMI_AUDIO = 16;
    private static final int BIT_HEADSET = 1;
    private static final int BIT_HEADSET_NO_MIC = 2;
    private static final int BIT_LINEOUT = 32;
    private static final int BIT_USB_HEADSET_ANLG = 4;
    private static final int BIT_USB_HEADSET_DGTL = 8;
    private static final boolean LOG;
    private static final int MSG_NEW_DEVICE_STATE = 1;
    private static final int MSG_SYSTEM_READY = 2;
    private static final String NAME_H2W = "h2w";
    private static final String NAME_HDMI = "hdmi";
    private static final String NAME_HDMI_AUDIO = "hdmi_audio";
    private static final String NAME_USB_AUDIO = "usb_audio";
    private static final int SUPPORTED_HEADSETS = 63;
    private static final String TAG = WiredAccessoryManager.class.getSimpleName();
    private final AudioManager mAudioManager;
    private final WiredAccessoryExtconObserver mExtconObserver;
    private int mHeadsetState;
    private final InputManagerService mInputManager;
    private final WiredAccessoryObserver mObserver;
    private int mSwitchValues;
    private final boolean mUseDevInputEventForAudioJack;
    private final PowerManager.WakeLock mWakeLock;
    private final Object mLock = new Object();
    private final Handler mHandler = new Handler(Looper.myLooper(), null, true) { // from class: com.android.server.WiredAccessoryManager.1
        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    WiredAccessoryManager.this.setDevicesState(msg.arg1, msg.arg2, (String) msg.obj);
                    WiredAccessoryManager.this.mWakeLock.release();
                    return;
                case 2:
                    WiredAccessoryManager.this.onSystemReady();
                    WiredAccessoryManager.this.mWakeLock.release();
                    return;
                default:
                    return;
            }
        }
    };

    static {
        LOG = "eng".equals(Build.TYPE) || "userdebug".equals(Build.TYPE);
    }

    public WiredAccessoryManager(Context context, InputManagerService inputManager) {
        PowerManager pm = (PowerManager) context.getSystemService("power");
        PowerManager.WakeLock newWakeLock = pm.newWakeLock(1, "WiredAccessoryManager");
        this.mWakeLock = newWakeLock;
        newWakeLock.setReferenceCounted(false);
        this.mAudioManager = (AudioManager) context.getSystemService("audio");
        this.mInputManager = inputManager;
        this.mUseDevInputEventForAudioJack = context.getResources().getBoolean(17891811);
        this.mExtconObserver = new WiredAccessoryExtconObserver();
        this.mObserver = new WiredAccessoryObserver();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onSystemReady() {
        if (this.mUseDevInputEventForAudioJack) {
            int switchValues = 0;
            if (this.mInputManager.getSwitchState(-1, -256, 2) == 1) {
                switchValues = 0 | 4;
            }
            if (this.mInputManager.getSwitchState(-1, -256, 4) == 1) {
                switchValues |= 16;
            }
            if (this.mInputManager.getSwitchState(-1, -256, 6) == 1) {
                switchValues |= 64;
            }
            notifyWiredAccessoryChanged(0L, switchValues, 84);
        }
        if (ExtconUEventObserver.extconExists()) {
            if (this.mUseDevInputEventForAudioJack) {
                Log.w(TAG, "Both input event and extcon are used for audio jack, please just choose one.");
            }
            this.mExtconObserver.init();
            return;
        }
        this.mObserver.init();
    }

    @Override // com.android.server.input.InputManagerService.WiredAccessoryCallbacks
    public void notifyWiredAccessoryChanged(long whenNanos, int switchValues, int switchMask) {
        int headset;
        if (LOG) {
            Slog.v(TAG, "notifyWiredAccessoryChanged: when=" + whenNanos + " bits=" + switchCodeToString(switchValues, switchMask) + " mask=" + Integer.toHexString(switchMask));
        }
        synchronized (this.mLock) {
            int i = (this.mSwitchValues & (~switchMask)) | switchValues;
            this.mSwitchValues = i;
            switch (i & 84) {
                case 0:
                    headset = 0;
                    break;
                case 4:
                    headset = 2;
                    break;
                case 16:
                    headset = 1;
                    break;
                case 20:
                    headset = 1;
                    break;
                case 64:
                    headset = 32;
                    break;
                default:
                    headset = 0;
                    break;
            }
            updateLocked(NAME_H2W, (this.mHeadsetState & (-36)) | headset);
        }
    }

    @Override // com.android.server.input.InputManagerService.WiredAccessoryCallbacks
    public void systemReady() {
        synchronized (this.mLock) {
            this.mWakeLock.acquire();
            Message msg = this.mHandler.obtainMessage(2, 0, 0, null);
            this.mHandler.sendMessage(msg);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateLocked(String newName, int newState) {
        int headsetState = newState & 63;
        int usb_headset_anlg = headsetState & 4;
        int usb_headset_dgtl = headsetState & 8;
        int h2w_headset = headsetState & 35;
        boolean h2wStateChange = true;
        boolean usbStateChange = true;
        if (LOG) {
            Slog.v(TAG, "newName=" + newName + " newState=" + newState + " headsetState=" + headsetState + " prev headsetState=" + this.mHeadsetState);
        }
        if (this.mHeadsetState == headsetState) {
            Log.e(TAG, "No state change.");
            return;
        }
        if (h2w_headset == 35) {
            Log.e(TAG, "Invalid combination, unsetting h2w flag");
            h2wStateChange = false;
        }
        if (usb_headset_anlg == 4 && usb_headset_dgtl == 8) {
            Log.e(TAG, "Invalid combination, unsetting usb flag");
            usbStateChange = false;
        }
        if (!h2wStateChange && !usbStateChange) {
            Log.e(TAG, "invalid transition, returning ...");
            return;
        }
        this.mWakeLock.acquire();
        Log.i(TAG, "MSG_NEW_DEVICE_STATE");
        Message msg = this.mHandler.obtainMessage(1, headsetState, this.mHeadsetState, "");
        this.mHandler.sendMessage(msg);
        this.mHeadsetState = headsetState;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setDevicesState(int headsetState, int prevHeadsetState, String headsetName) {
        synchronized (this.mLock) {
            int allHeadsets = 63;
            int curHeadset = 1;
            while (allHeadsets != 0) {
                if ((curHeadset & allHeadsets) != 0) {
                    setDeviceStateLocked(curHeadset, headsetState, prevHeadsetState, headsetName);
                    allHeadsets &= ~curHeadset;
                }
                curHeadset <<= 1;
            }
        }
    }

    private void setDeviceStateLocked(int headset, int headsetState, int prevHeadsetState, String headsetName) {
        int state;
        int outDevice;
        if ((headsetState & headset) != (prevHeadsetState & headset)) {
            int inDevice = 0;
            if ((headsetState & headset) != 0) {
                state = 1;
            } else {
                state = 0;
            }
            if (headset == 1) {
                outDevice = 4;
                inDevice = AudioDevice.IN_WIRED_HEADSET;
            } else if (headset == 2) {
                outDevice = 8;
            } else if (headset == 32) {
                outDevice = 131072;
            } else if (headset == 4) {
                outDevice = 2048;
            } else if (headset == 8) {
                outDevice = 4096;
            } else if (headset == 16) {
                outDevice = 1024;
            } else {
                Slog.e(TAG, "setDeviceState() invalid headset type: " + headset);
                return;
            }
            if (LOG) {
                Slog.v(TAG, "headsetName: " + headsetName + (state == 1 ? " connected" : " disconnected"));
            }
            if (outDevice != 0) {
                this.mAudioManager.setWiredDeviceConnectionState(outDevice, state, "", headsetName);
            }
            if (inDevice != 0) {
                this.mAudioManager.setWiredDeviceConnectionState(inDevice, state, "", headsetName);
            }
        }
    }

    private String switchCodeToString(int switchValues, int switchMask) {
        StringBuilder sb = new StringBuilder();
        if ((switchMask & 4) != 0 && (switchValues & 4) != 0) {
            sb.append("SW_HEADPHONE_INSERT ");
        }
        if ((switchMask & 16) != 0 && (switchValues & 16) != 0) {
            sb.append("SW_MICROPHONE_INSERT");
        }
        return sb.toString();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class WiredAccessoryObserver extends UEventObserver {
        private final List<UEventInfo> mUEventInfo = makeObservedUEventList();

        public WiredAccessoryObserver() {
        }

        void init() {
            synchronized (WiredAccessoryManager.this.mLock) {
                if (WiredAccessoryManager.LOG) {
                    Slog.v(WiredAccessoryManager.TAG, "init()");
                }
                char[] buffer = new char[1024];
                for (int i = 0; i < this.mUEventInfo.size(); i++) {
                    UEventInfo uei = this.mUEventInfo.get(i);
                    try {
                        FileReader file = new FileReader(uei.getSwitchStatePath());
                        int len = file.read(buffer, 0, 1024);
                        file.close();
                        int curState = Integer.parseInt(new String(buffer, 0, len).trim());
                        if (curState > 0) {
                            updateStateLocked(uei.getDevPath(), uei.getDevName(), curState);
                        }
                    } catch (FileNotFoundException e) {
                        Slog.w(WiredAccessoryManager.TAG, uei.getSwitchStatePath() + " not found while attempting to determine initial switch state");
                    } catch (Exception e2) {
                        Slog.e(WiredAccessoryManager.TAG, "Error while attempting to determine initial switch state for " + uei.getDevName(), e2);
                    }
                }
            }
            for (int i2 = 0; i2 < this.mUEventInfo.size(); i2++) {
                startObserving("DEVPATH=" + this.mUEventInfo.get(i2).getDevPath());
            }
        }

        private List<UEventInfo> makeObservedUEventList() {
            List<UEventInfo> retVal = new ArrayList<>();
            if (!WiredAccessoryManager.this.mUseDevInputEventForAudioJack) {
                UEventInfo uei = new UEventInfo(WiredAccessoryManager.NAME_H2W, 1, 2, 32);
                if (uei.checkSwitchExists()) {
                    retVal.add(uei);
                } else {
                    Slog.w(WiredAccessoryManager.TAG, "This kernel does not have wired headset support");
                }
            }
            UEventInfo uei2 = new UEventInfo(WiredAccessoryManager.NAME_USB_AUDIO, 4, 8, 0);
            if (uei2.checkSwitchExists()) {
                retVal.add(uei2);
            } else {
                Slog.w(WiredAccessoryManager.TAG, "This kernel does not have usb audio support");
            }
            UEventInfo uei3 = new UEventInfo(WiredAccessoryManager.NAME_HDMI_AUDIO, 16, 0, 0);
            if (uei3.checkSwitchExists()) {
                retVal.add(uei3);
            } else {
                UEventInfo uei4 = new UEventInfo(WiredAccessoryManager.NAME_HDMI, 16, 0, 0);
                if (uei4.checkSwitchExists()) {
                    retVal.add(uei4);
                } else {
                    Slog.w(WiredAccessoryManager.TAG, "This kernel does not have HDMI audio support");
                }
            }
            return retVal;
        }

        public void onUEvent(UEventObserver.UEvent event) {
            if (WiredAccessoryManager.LOG) {
                Slog.v(WiredAccessoryManager.TAG, "Headset UEVENT: " + event.toString());
            }
            try {
                String devPath = event.get("DEVPATH");
                String name = event.get("SWITCH_NAME");
                int state = Integer.parseInt(event.get("SWITCH_STATE"));
                synchronized (WiredAccessoryManager.this.mLock) {
                    updateStateLocked(devPath, name, state);
                }
            } catch (NumberFormatException e) {
                Slog.e(WiredAccessoryManager.TAG, "Could not parse switch state from event " + event);
            }
        }

        private void updateStateLocked(String devPath, String name, int state) {
            for (int i = 0; i < this.mUEventInfo.size(); i++) {
                UEventInfo uei = this.mUEventInfo.get(i);
                if (devPath.equals(uei.getDevPath())) {
                    WiredAccessoryManager wiredAccessoryManager = WiredAccessoryManager.this;
                    wiredAccessoryManager.updateLocked(name, uei.computeNewHeadsetState(wiredAccessoryManager.mHeadsetState, state));
                    return;
                }
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        /* loaded from: classes.dex */
        public final class UEventInfo {
            private final String mDevName;
            private final int mState1Bits;
            private final int mState2Bits;
            private final int mStateNbits;

            public UEventInfo(String devName, int state1Bits, int state2Bits, int stateNbits) {
                this.mDevName = devName;
                this.mState1Bits = state1Bits;
                this.mState2Bits = state2Bits;
                this.mStateNbits = stateNbits;
            }

            public String getDevName() {
                return this.mDevName;
            }

            public String getDevPath() {
                return String.format(Locale.US, "/devices/virtual/switch/%s", this.mDevName);
            }

            public String getSwitchStatePath() {
                return String.format(Locale.US, "/sys/class/switch/%s/state", this.mDevName);
            }

            public boolean checkSwitchExists() {
                File f = new File(getSwitchStatePath());
                return f.exists();
            }

            public int computeNewHeadsetState(int headsetState, int switchState) {
                int setBits = this.mState1Bits;
                int i = this.mState2Bits;
                int i2 = this.mStateNbits;
                int preserveMask = ~(setBits | i | i2);
                if (switchState != 1) {
                    if (switchState == 2) {
                        setBits = i;
                    } else {
                        setBits = switchState == i2 ? i2 : 0;
                    }
                }
                return (headsetState & preserveMask) | setBits;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class WiredAccessoryExtconObserver extends ExtconStateObserver<Pair<Integer, Integer>> {
        private final List<ExtconUEventObserver.ExtconInfo> mExtconInfos = ExtconUEventObserver.ExtconInfo.getExtconInfoForTypes(new String[]{ExtconUEventObserver.ExtconInfo.EXTCON_HEADPHONE, ExtconUEventObserver.ExtconInfo.EXTCON_MICROPHONE, ExtconUEventObserver.ExtconInfo.EXTCON_HDMI, ExtconUEventObserver.ExtconInfo.EXTCON_LINE_OUT});

        WiredAccessoryExtconObserver() {
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void init() {
            for (ExtconUEventObserver.ExtconInfo extconInfo : this.mExtconInfos) {
                Pair<Integer, Integer> state = null;
                try {
                    state = parseStateFromFile(extconInfo);
                } catch (FileNotFoundException e) {
                    Slog.w(WiredAccessoryManager.TAG, extconInfo.getStatePath() + " not found while attempting to determine initial state", e);
                } catch (IOException e2) {
                    Slog.e(WiredAccessoryManager.TAG, "Error reading " + extconInfo.getStatePath() + " while attempting to determine initial state", e2);
                }
                if (state != null) {
                    updateStateInt(extconInfo, extconInfo.getName(), state);
                }
                if (WiredAccessoryManager.LOG) {
                    Slog.d(WiredAccessoryManager.TAG, "observing " + extconInfo.getName());
                }
                startObserving(extconInfo);
            }
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // com.android.server.ExtconStateObserver
        public Pair<Integer, Integer> parseState(ExtconUEventObserver.ExtconInfo extconInfo, String status) {
            if (WiredAccessoryManager.LOG) {
                Slog.v(WiredAccessoryManager.TAG, "status  " + status);
            }
            int[] maskAndState = {0, 0};
            if (extconInfo.hasCableType(ExtconUEventObserver.ExtconInfo.EXTCON_HEADPHONE)) {
                WiredAccessoryManager.updateBit(maskAndState, 2, status, ExtconUEventObserver.ExtconInfo.EXTCON_HEADPHONE);
            }
            if (extconInfo.hasCableType(ExtconUEventObserver.ExtconInfo.EXTCON_MICROPHONE)) {
                WiredAccessoryManager.updateBit(maskAndState, 1, status, ExtconUEventObserver.ExtconInfo.EXTCON_MICROPHONE);
            }
            if (extconInfo.hasCableType(ExtconUEventObserver.ExtconInfo.EXTCON_HDMI)) {
                WiredAccessoryManager.updateBit(maskAndState, 16, status, ExtconUEventObserver.ExtconInfo.EXTCON_HDMI);
            }
            if (extconInfo.hasCableType(ExtconUEventObserver.ExtconInfo.EXTCON_LINE_OUT)) {
                WiredAccessoryManager.updateBit(maskAndState, 32, status, ExtconUEventObserver.ExtconInfo.EXTCON_LINE_OUT);
            }
            if (WiredAccessoryManager.LOG) {
                Slog.v(WiredAccessoryManager.TAG, "mask " + maskAndState[0] + " state " + maskAndState[1]);
            }
            return Pair.create(Integer.valueOf(maskAndState[0]), Integer.valueOf(maskAndState[1]));
        }

        /* JADX DEBUG: Method merged with bridge method */
        @Override // com.android.server.ExtconStateObserver
        public void updateState(ExtconUEventObserver.ExtconInfo extconInfo, String name, Pair<Integer, Integer> maskAndState) {
            synchronized (WiredAccessoryManager.this.mLock) {
                int mask = ((Integer) maskAndState.first).intValue();
                int state = ((Integer) maskAndState.second).intValue();
                WiredAccessoryManager wiredAccessoryManager = WiredAccessoryManager.this;
                wiredAccessoryManager.updateLocked(name, (wiredAccessoryManager.mHeadsetState & (~((~state) & mask))) | (mask & state));
            }
        }

        private void updateStateInt(ExtconUEventObserver.ExtconInfo extconInfo, String name, Pair<Integer, Integer> maskAndState) {
            synchronized (WiredAccessoryManager.this.mLock) {
                int mask = ((Integer) maskAndState.first).intValue();
                int state = ((Integer) maskAndState.second).intValue();
                if (WiredAccessoryManager.this.mHeadsetState == 0) {
                    WiredAccessoryManager wiredAccessoryManager = WiredAccessoryManager.this;
                    wiredAccessoryManager.updateLocked(name, (wiredAccessoryManager.mHeadsetState & (~((~state) & mask))) | (mask & state));
                } else {
                    WiredAccessoryManager wiredAccessoryManager2 = WiredAccessoryManager.this;
                    wiredAccessoryManager2.updateLocked(name, wiredAccessoryManager2.mHeadsetState | (mask & state & (~((~state) & mask))));
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void updateBit(int[] maskAndState, int position, String state, String name) {
        maskAndState[0] = maskAndState[0] | position;
        if (state.contains(name + "=1")) {
            maskAndState[0] = maskAndState[0] | position;
            maskAndState[1] = maskAndState[1] | position;
        } else if (state.contains(name + "=0")) {
            maskAndState[0] = maskAndState[0] | position;
            maskAndState[1] = maskAndState[1] & (~position);
        }
    }
}
