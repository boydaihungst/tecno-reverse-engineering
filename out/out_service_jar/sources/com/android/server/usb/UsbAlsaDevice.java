package com.android.server.usb;

import android.hardware.audio.common.V2_0.AudioDevice;
import android.media.AudioDeviceAttributes;
import android.media.IAudioService;
import android.os.RemoteException;
import android.util.Slog;
import com.android.internal.util.dump.DualDumpOutputStream;
import com.android.server.audio.AudioService;
/* loaded from: classes2.dex */
public final class UsbAlsaDevice {
    protected static final boolean DEBUG = false;
    private static final String TAG = "UsbAlsaDevice";
    private IAudioService mAudioService;
    private final int mCardNum;
    private final String mDeviceAddress;
    private final int mDeviceNum;
    private final boolean mHasInput;
    private final boolean mHasOutput;
    private int mInputState;
    private final boolean mIsDock;
    private final boolean mIsInputHeadset;
    private final boolean mIsOutputHeadset;
    private UsbAlsaJackDetector mJackDetector;
    private int mOutputState;
    private boolean mSelected = false;
    private String mDeviceName = "";
    private String mDeviceDescription = "";

    public UsbAlsaDevice(IAudioService audioService, int card, int device, String deviceAddress, boolean hasOutput, boolean hasInput, boolean isInputHeadset, boolean isOutputHeadset, boolean isDock) {
        this.mAudioService = audioService;
        this.mCardNum = card;
        this.mDeviceNum = device;
        this.mDeviceAddress = deviceAddress;
        this.mHasOutput = hasOutput;
        this.mHasInput = hasInput;
        this.mIsInputHeadset = isInputHeadset;
        this.mIsOutputHeadset = isOutputHeadset;
        this.mIsDock = isDock;
    }

    public int getCardNum() {
        return this.mCardNum;
    }

    public int getDeviceNum() {
        return this.mDeviceNum;
    }

    public String getDeviceAddress() {
        return this.mDeviceAddress;
    }

    public String getAlsaCardDeviceString() {
        int i;
        int i2 = this.mCardNum;
        if (i2 < 0 || (i = this.mDeviceNum) < 0) {
            Slog.e(TAG, "Invalid alsa card or device alsaCard: " + this.mCardNum + " alsaDevice: " + this.mDeviceNum);
            return null;
        }
        return AudioService.makeAlsaAddressString(i2, i);
    }

    public boolean hasOutput() {
        return this.mHasOutput;
    }

    public boolean hasInput() {
        return this.mHasInput;
    }

    public boolean isInputHeadset() {
        return this.mIsInputHeadset;
    }

    public boolean isOutputHeadset() {
        return this.mIsOutputHeadset;
    }

    public boolean isDock() {
        return this.mIsDock;
    }

    private synchronized boolean isInputJackConnected() {
        UsbAlsaJackDetector usbAlsaJackDetector = this.mJackDetector;
        if (usbAlsaJackDetector == null) {
            return true;
        }
        return usbAlsaJackDetector.isInputJackConnected();
    }

    private synchronized boolean isOutputJackConnected() {
        UsbAlsaJackDetector usbAlsaJackDetector = this.mJackDetector;
        if (usbAlsaJackDetector == null) {
            return true;
        }
        return usbAlsaJackDetector.isOutputJackConnected();
    }

    private synchronized void startJackDetect() {
        this.mJackDetector = UsbAlsaJackDetector.startJackDetect(this);
    }

    private synchronized void stopJackDetect() {
        UsbAlsaJackDetector usbAlsaJackDetector = this.mJackDetector;
        if (usbAlsaJackDetector != null) {
            usbAlsaJackDetector.pleaseStop();
        }
        this.mJackDetector = null;
    }

    public synchronized void start() {
        this.mSelected = true;
        this.mInputState = 0;
        this.mOutputState = 0;
        startJackDetect();
        updateWiredDeviceConnectionState(true);
    }

    public synchronized void stop() {
        stopJackDetect();
        updateWiredDeviceConnectionState(false);
        this.mSelected = false;
    }

    public synchronized void updateWiredDeviceConnectionState(boolean enable) {
        int device;
        int device2;
        if (!this.mSelected) {
            Slog.e(TAG, "updateWiredDeviceConnectionState on unselected AlsaDevice!");
            return;
        }
        String alsaCardDeviceString = getAlsaCardDeviceString();
        if (alsaCardDeviceString == null) {
            return;
        }
        try {
            int inputState = 1;
            if (this.mHasOutput) {
                if (this.mIsDock) {
                    device2 = 4096;
                } else if (this.mIsOutputHeadset) {
                    device2 = 67108864;
                } else {
                    device2 = 16384;
                }
                boolean connected = isOutputJackConnected();
                Slog.i(TAG, "OUTPUT JACK connected: " + connected);
                int outputState = (enable && connected) ? 1 : 0;
                if (outputState != this.mOutputState) {
                    this.mOutputState = outputState;
                    AudioDeviceAttributes attributes = new AudioDeviceAttributes(device2, alsaCardDeviceString, this.mDeviceName);
                    this.mAudioService.setWiredDeviceConnectionState(attributes, outputState, TAG);
                }
            }
            if (this.mHasInput) {
                if (this.mIsInputHeadset) {
                    device = AudioDevice.IN_USB_HEADSET;
                } else {
                    device = AudioDevice.IN_USB_DEVICE;
                }
                boolean connected2 = isInputJackConnected();
                Slog.i(TAG, "INPUT JACK connected: " + connected2);
                if (!enable || !connected2) {
                    inputState = 0;
                }
                if (inputState != this.mInputState) {
                    this.mInputState = inputState;
                    AudioDeviceAttributes attributes2 = new AudioDeviceAttributes(device, alsaCardDeviceString, this.mDeviceName);
                    this.mAudioService.setWiredDeviceConnectionState(attributes2, inputState, TAG);
                }
            }
        } catch (RemoteException e) {
            Slog.e(TAG, "RemoteException in setWiredDeviceConnectionState");
        }
    }

    public synchronized String toString() {
        return "UsbAlsaDevice: [card: " + this.mCardNum + ", device: " + this.mDeviceNum + ", name: " + this.mDeviceName + ", hasOutput: " + this.mHasOutput + ", hasInput: " + this.mHasInput + "]";
    }

    public synchronized void dump(DualDumpOutputStream dump, String idName, long id) {
        long token = dump.start(idName, id);
        dump.write("card", (long) CompanionMessage.MESSAGE_ID, this.mCardNum);
        dump.write("device", 1120986464258L, this.mDeviceNum);
        dump.write("name", 1138166333443L, this.mDeviceName);
        dump.write("has_output", 1133871366148L, this.mHasOutput);
        dump.write("has_input", 1133871366149L, this.mHasInput);
        dump.write("address", 1138166333446L, this.mDeviceAddress);
        dump.end(token);
    }

    synchronized String toShortString() {
        return "[card:" + this.mCardNum + " device:" + this.mDeviceNum + " " + this.mDeviceName + "]";
    }

    synchronized String getDeviceName() {
        return this.mDeviceName;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public synchronized void setDeviceNameAndDescription(String deviceName, String deviceDescription) {
        this.mDeviceName = deviceName;
        this.mDeviceDescription = deviceDescription;
    }

    public boolean equals(Object obj) {
        if (obj instanceof UsbAlsaDevice) {
            UsbAlsaDevice other = (UsbAlsaDevice) obj;
            return this.mCardNum == other.mCardNum && this.mDeviceNum == other.mDeviceNum && this.mHasOutput == other.mHasOutput && this.mHasInput == other.mHasInput && this.mIsInputHeadset == other.mIsInputHeadset && this.mIsOutputHeadset == other.mIsOutputHeadset && this.mIsDock == other.mIsDock;
        }
        return false;
    }

    public int hashCode() {
        int result = (1 * 31) + this.mCardNum;
        return (((((((((((result * 31) + this.mDeviceNum) * 31) + (!this.mHasOutput ? 1 : 0)) * 31) + (!this.mHasInput ? 1 : 0)) * 31) + (!this.mIsInputHeadset ? 1 : 0)) * 31) + (!this.mIsOutputHeadset ? 1 : 0)) * 31) + (!this.mIsDock ? 1 : 0);
    }
}
