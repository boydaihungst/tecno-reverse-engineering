package com.android.server.audio;

import android.media.AudioAttributes;
import android.media.AudioDeviceAttributes;
import android.media.AudioSystem;
import android.media.audiopolicy.AudioMix;
import android.util.Log;
import android.util.Pair;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
/* loaded from: classes.dex */
public class AudioSystemAdapter implements AudioSystem.RoutingUpdateCallback, AudioSystem.VolumeRangeInitRequestCallback {
    private static final boolean DEBUG_CACHE = false;
    private static final boolean ENABLE_GETDEVICES_STATS = false;
    private static final int METHOD_GETDEVICESFORATTRIBUTES = 0;
    private static final int NB_MEASUREMENTS = 1;
    private static final String TAG = "AudioSystemAdapter";
    private static final boolean USE_CACHE_FOR_GETDEVICES = true;
    private static OnRoutingUpdatedListener sRoutingListener;
    private static AudioSystemAdapter sSingletonDefaultAdapter;
    private static OnVolRangeInitRequestListener sVolRangeInitReqListener;
    private ConcurrentHashMap<Pair<AudioAttributes, Boolean>, ArrayList<AudioDeviceAttributes>> mDevicesForAttrCache;
    private int[] mMethodCacheHit;
    private int[] mMethodCallCounter;
    private String[] mMethodNames = {"getDevicesForAttributes"};
    private long[] mMethodTimeNs;
    private static final Object sRoutingListenerLock = new Object();
    private static final Object sVolRangeInitReqListenerLock = new Object();

    /* loaded from: classes.dex */
    interface OnRoutingUpdatedListener {
        void onRoutingUpdatedFromNative();
    }

    /* loaded from: classes.dex */
    interface OnVolRangeInitRequestListener {
        void onVolumeRangeInitRequestFromNative();
    }

    public void onRoutingUpdated() {
        OnRoutingUpdatedListener listener;
        invalidateRoutingCache();
        synchronized (sRoutingListenerLock) {
            listener = sRoutingListener;
        }
        if (listener != null) {
            listener.onRoutingUpdatedFromNative();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void setRoutingListener(OnRoutingUpdatedListener listener) {
        synchronized (sRoutingListenerLock) {
            sRoutingListener = listener;
        }
    }

    public void onVolumeRangeInitializationRequested() {
        OnVolRangeInitRequestListener listener;
        synchronized (sVolRangeInitReqListenerLock) {
            listener = sVolRangeInitReqListener;
        }
        if (listener != null) {
            listener.onVolumeRangeInitRequestFromNative();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void setVolRangeInitReqListener(OnVolRangeInitRequestListener listener) {
        synchronized (sVolRangeInitReqListenerLock) {
            sVolRangeInitReqListener = listener;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static final synchronized AudioSystemAdapter getDefaultAdapter() {
        AudioSystemAdapter audioSystemAdapter;
        synchronized (AudioSystemAdapter.class) {
            if (sSingletonDefaultAdapter == null) {
                AudioSystemAdapter audioSystemAdapter2 = new AudioSystemAdapter();
                sSingletonDefaultAdapter = audioSystemAdapter2;
                AudioSystem.setRoutingCallback(audioSystemAdapter2);
                AudioSystem.setVolumeRangeInitRequestCallback(sSingletonDefaultAdapter);
                sSingletonDefaultAdapter.mDevicesForAttrCache = new ConcurrentHashMap<>(AudioSystem.getNumStreamTypes());
                sSingletonDefaultAdapter.mMethodCacheHit = new int[1];
            }
            audioSystemAdapter = sSingletonDefaultAdapter;
        }
        return audioSystemAdapter;
    }

    private void invalidateRoutingCache() {
        ConcurrentHashMap<Pair<AudioAttributes, Boolean>, ArrayList<AudioDeviceAttributes>> concurrentHashMap = this.mDevicesForAttrCache;
        if (concurrentHashMap != null) {
            synchronized (concurrentHashMap) {
                this.mDevicesForAttrCache.clear();
            }
        }
    }

    public ArrayList<AudioDeviceAttributes> getDevicesForAttributes(AudioAttributes attributes, boolean forVolume) {
        return getDevicesForAttributesImpl(attributes, forVolume);
    }

    private ArrayList<AudioDeviceAttributes> getDevicesForAttributesImpl(AudioAttributes attributes, boolean forVolume) {
        Pair<AudioAttributes, Boolean> key = new Pair<>(attributes, Boolean.valueOf(forVolume));
        synchronized (this.mDevicesForAttrCache) {
            ArrayList<AudioDeviceAttributes> res = this.mDevicesForAttrCache.get(key);
            if (res == null) {
                ArrayList<AudioDeviceAttributes> res2 = AudioSystem.getDevicesForAttributes(attributes, forVolume);
                if (res2.size() > 1 && res2.get(0) != null && res2.get(0).getInternalType() == 0) {
                    Log.e(TAG, "unable to get devices for " + attributes);
                    return res2;
                }
                this.mDevicesForAttrCache.put(key, res2);
                return res2;
            }
            int[] iArr = this.mMethodCacheHit;
            iArr[0] = iArr[0] + 1;
            return res;
        }
    }

    private static String attrDeviceToDebugString(AudioAttributes attr, List<AudioDeviceAttributes> devices) {
        return " attrUsage=" + attr.getSystemUsage() + " " + AudioSystem.deviceSetToString(AudioSystem.generateAudioDeviceTypesSet(devices));
    }

    public int setDeviceConnectionState(AudioDeviceAttributes attributes, int state, int codecFormat) {
        invalidateRoutingCache();
        return AudioSystem.setDeviceConnectionState(attributes, state, codecFormat);
    }

    public int getDeviceConnectionState(int device, String deviceAddress) {
        return AudioSystem.getDeviceConnectionState(device, deviceAddress);
    }

    public int handleDeviceConfigChange(int device, String deviceAddress, String deviceName, int codecFormat) {
        invalidateRoutingCache();
        return AudioSystem.handleDeviceConfigChange(device, deviceAddress, deviceName, codecFormat);
    }

    public int setDevicesRoleForStrategy(int strategy, int role, List<AudioDeviceAttributes> devices) {
        invalidateRoutingCache();
        return AudioSystem.setDevicesRoleForStrategy(strategy, role, devices);
    }

    public int removeDevicesRoleForStrategy(int strategy, int role) {
        invalidateRoutingCache();
        return AudioSystem.removeDevicesRoleForStrategy(strategy, role);
    }

    public int setDevicesRoleForCapturePreset(int capturePreset, int role, List<AudioDeviceAttributes> devices) {
        invalidateRoutingCache();
        return AudioSystem.setDevicesRoleForCapturePreset(capturePreset, role, devices);
    }

    public int removeDevicesRoleForCapturePreset(int capturePreset, int role, List<AudioDeviceAttributes> devicesToRemove) {
        invalidateRoutingCache();
        return AudioSystem.removeDevicesRoleForCapturePreset(capturePreset, role, devicesToRemove);
    }

    public int clearDevicesRoleForCapturePreset(int capturePreset, int role) {
        invalidateRoutingCache();
        return AudioSystem.clearDevicesRoleForCapturePreset(capturePreset, role);
    }

    public int setParameters(String keyValuePairs) {
        return AudioSystem.setParameters(keyValuePairs);
    }

    public boolean isMicrophoneMuted() {
        return AudioSystem.isMicrophoneMuted();
    }

    public int muteMicrophone(boolean on) {
        return AudioSystem.muteMicrophone(on);
    }

    public int setCurrentImeUid(int uid) {
        return AudioSystem.setCurrentImeUid(uid);
    }

    public boolean isStreamActive(int stream, int inPastMs) {
        return AudioSystem.isStreamActive(stream, inPastMs);
    }

    public boolean isStreamActiveRemotely(int stream, int inPastMs) {
        return AudioSystem.isStreamActiveRemotely(stream, inPastMs);
    }

    public int setPhoneState(int state, int uid) {
        invalidateRoutingCache();
        return AudioSystem.setPhoneState(state, uid);
    }

    public int setAllowedCapturePolicy(int uid, int flags) {
        return AudioSystem.setAllowedCapturePolicy(uid, flags);
    }

    public int setForceUse(int usage, int config) {
        invalidateRoutingCache();
        return AudioSystem.setForceUse(usage, config);
    }

    public int getForceUse(int usage) {
        return AudioSystem.getForceUse(usage);
    }

    public int registerPolicyMixes(ArrayList<AudioMix> mixes, boolean register) {
        invalidateRoutingCache();
        return AudioSystem.registerPolicyMixes(mixes, register);
    }

    public int setUidDeviceAffinities(int uid, int[] types, String[] addresses) {
        invalidateRoutingCache();
        return AudioSystem.setUidDeviceAffinities(uid, types, addresses);
    }

    public int removeUidDeviceAffinities(int uid) {
        invalidateRoutingCache();
        return AudioSystem.removeUidDeviceAffinities(uid);
    }

    public int setUserIdDeviceAffinities(int userId, int[] types, String[] addresses) {
        invalidateRoutingCache();
        return AudioSystem.setUserIdDeviceAffinities(userId, types, addresses);
    }

    public int removeUserIdDeviceAffinities(int userId) {
        invalidateRoutingCache();
        return AudioSystem.removeUserIdDeviceAffinities(userId);
    }

    public void dump(PrintWriter pw) {
        pw.println("\nAudioSystemAdapter:");
        pw.println(" mDevicesForAttrCache:");
        ConcurrentHashMap<Pair<AudioAttributes, Boolean>, ArrayList<AudioDeviceAttributes>> concurrentHashMap = this.mDevicesForAttrCache;
        if (concurrentHashMap != null) {
            for (Map.Entry<Pair<AudioAttributes, Boolean>, ArrayList<AudioDeviceAttributes>> entry : concurrentHashMap.entrySet()) {
                AudioAttributes attributes = (AudioAttributes) entry.getKey().first;
                try {
                    int stream = attributes.getVolumeControlStream();
                    pw.println("\t" + attributes + " forVolume: " + entry.getKey().second + " stream: " + AudioSystem.STREAM_NAMES[stream] + "(" + stream + ")");
                    Iterator<AudioDeviceAttributes> it = entry.getValue().iterator();
                    while (it.hasNext()) {
                        AudioDeviceAttributes devAttr = it.next();
                        pw.println("\t\t" + devAttr);
                    }
                } catch (IllegalArgumentException e) {
                    pw.println("\t dump failed for attributes: " + attributes);
                    Log.e(TAG, "dump failed", e);
                }
            }
        }
    }
}
