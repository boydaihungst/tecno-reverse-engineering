package com.mediatek.server.audio;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioDeviceAttributes;
import android.media.AudioDeviceInfo;
import android.os.Build;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import com.android.server.audio.AudioService;
import com.android.server.audio.AudioSystemAdapter;
import com.android.server.audio.SystemServerAdapter;
/* loaded from: classes2.dex */
public class AudioServiceExt {
    protected static final boolean LOGD;
    private static final String TAG = "AS.AudioServiceExt";

    static {
        LOGD = "eng".equals(Build.TYPE) || "userdebug".equals(Build.TYPE);
    }

    private void logd(String msg) {
        if (LOGD) {
            Log.d(TAG, msg);
        }
    }

    public void init(Context context, AudioService audioService, AudioSystemAdapter audioSystem, SystemServerAdapter systemServer, Object deviceBroker) {
        logd("[Default] setBluetoothLeAudioDeviceConnectionState()");
    }

    public void setBluetoothLeAudioDeviceConnectionState(BluetoothDevice device, int state, boolean suppressNoisyIntent, int musicDevice) {
        logd("[Default] setBluetoothLeAudioDeviceConnectionState()");
    }

    public void leVcSupportsAbsoluteVolume(String address, boolean support) {
        logd("[Default] leVcSupportsAbsoluteVolume() false");
    }

    public boolean isBluetoothLeOn() {
        logd("[Default] isBluetoothLeOn() false");
        return false;
    }

    public boolean isBleAudioFeatureSupported() {
        logd("[Default] isBleAudioFeatureSupported() false");
        return false;
    }

    public void handleMessageExt(Message msg) {
        Log.wtf(TAG, "Invalid message " + msg.what);
    }

    public void onReceiveExt(Context context, Intent intent) {
        logd("[Default] onReceiveExt");
    }

    public void postSetLeCgVcIndex(int index) {
        logd("[Default] postSetLeCgVcIndex, index=" + index);
    }

    public void onSystemReadyExt() {
        logd("[Default] onSystemReadyExt");
    }

    public void getBleIntentFilters(IntentFilter intentFilter) {
        logd("[Default] getBleIntentFilters");
    }

    public boolean isBluetoothLeCgOn() {
        logd("[Default] isBluetoothLeCgOn");
        return false;
    }

    public boolean isBluetoothLeTbsDeviceActive() {
        logd("[Default] isBluetoothLeTbsDeviceActive()");
        return false;
    }

    public AudioDeviceAttributes preferredCommunicationDevice() {
        logd("[Default] preferredCommunicationDevice()");
        return null;
    }

    public void startBluetoothLeCg(IBinder cb, int targetSdkVersion) {
        logd("[Default] startBluetoothLeCg()");
    }

    public void startBluetoothLeCg(int pid, int uid, int setMode, IBinder cb) {
        logd("[Default] startBluetoothLeCg()");
    }

    public boolean stopBluetoothLeCg(IBinder cb) {
        logd("[Default] stopBluetoothLeCg()");
        return false;
    }

    public void stopBluetoothLeCgLater(IBinder cb) {
        logd("[Default] stopBluetoothLeCgLater()");
    }

    public void startBluetoothLeCgVirtualCall(IBinder cb) {
        logd("[Default] startBluetoothLeCgVirtualCall()");
    }

    public boolean isBluetoothLeCgActive() {
        logd("[Default] isBluetoothLeCgActive() false");
        return false;
    }

    public void setBluetoothLeCgOn(boolean on) {
        logd("[Default] setBluetoothLeCgOn() false");
    }

    public boolean isSystemReady() {
        logd("[Default] isSystemReady() false");
        return false;
    }

    public int getBleCgVolume() {
        logd("[Default] getBleCgVolume()");
        return 0;
    }

    public boolean setCommunicationDeviceExt(IBinder cb, int pid, AudioDeviceInfo device, String eventSource) {
        logd("[Default] setCommunicationDeviceExt()");
        return false;
    }

    public void onUpdateAudioModeExt(int mode, int pid, IBinder cb) {
        logd("[Default] setCommunicationDevice()");
    }

    public void restartScoInVoipCall() {
        logd("[Default] restartScoInVoipCall()");
    }

    public void setPreferredDeviceForHfpInbandRinging(int pid, int uid, int mode, IBinder cb) {
        logd("[Default] setPreferredDevicesForHfpInbandRinging()");
    }
}
