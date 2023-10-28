package com.android.server.audio;

import android.content.Context;
import android.hardware.devicestate.DeviceStateManager;
import android.hardware.display.DisplayManager;
import android.hardware.display.DisplayManagerGlobal;
import android.media.AudioSystem;
import android.os.Handler;
import android.os.HandlerExecutor;
import android.util.Log;
import java.util.function.Consumer;
/* loaded from: classes.dex */
class RotationHelper {
    private static final boolean DEBUG_ROTATION = false;
    private static final String TAG = "AudioService.RotationHelper";
    private static Context sContext;
    private static AudioDisplayListener sDisplayListener;
    private static DeviceStateManager.FoldStateListener sFoldStateListener;
    private static Handler sHandler;
    private static final Object sRotationLock = new Object();
    private static final Object sFoldStateLock = new Object();
    private static int sDeviceRotation = 0;
    private static boolean sDeviceFold = true;

    RotationHelper() {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void init(Context context, Handler handler) {
        if (context == null) {
            throw new IllegalArgumentException("Invalid null context");
        }
        sContext = context;
        sHandler = handler;
        sDisplayListener = new AudioDisplayListener();
        enable();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void enable() {
        ((DisplayManager) sContext.getSystemService("display")).registerDisplayListener(sDisplayListener, sHandler);
        updateOrientation();
        sFoldStateListener = new DeviceStateManager.FoldStateListener(sContext, new Consumer() { // from class: com.android.server.audio.RotationHelper$$ExternalSyntheticLambda0
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                RotationHelper.updateFoldState(((Boolean) obj).booleanValue());
            }
        });
        ((DeviceStateManager) sContext.getSystemService(DeviceStateManager.class)).registerCallback(new HandlerExecutor(sHandler), sFoldStateListener);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void disable() {
        ((DisplayManager) sContext.getSystemService("display")).unregisterDisplayListener(sDisplayListener);
        ((DeviceStateManager) sContext.getSystemService(DeviceStateManager.class)).unregisterCallback(sFoldStateListener);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void updateOrientation() {
        int newRotation = DisplayManagerGlobal.getInstance().getDisplayInfo(0).rotation;
        synchronized (sRotationLock) {
            if (newRotation != sDeviceRotation) {
                sDeviceRotation = newRotation;
                publishRotation(newRotation);
            }
        }
    }

    private static void publishRotation(int rotation) {
        switch (rotation) {
            case 0:
                AudioSystem.setParameters("rotation=0");
                return;
            case 1:
                AudioSystem.setParameters("rotation=90");
                return;
            case 2:
                AudioSystem.setParameters("rotation=180");
                return;
            case 3:
                AudioSystem.setParameters("rotation=270");
                return;
            default:
                Log.e(TAG, "Unknown device rotation");
                return;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void updateFoldState(boolean newFolded) {
        synchronized (sFoldStateLock) {
            if (sDeviceFold != newFolded) {
                sDeviceFold = newFolded;
                if (newFolded) {
                    AudioSystem.setParameters("device_folded=on");
                } else {
                    AudioSystem.setParameters("device_folded=off");
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static final class AudioDisplayListener implements DisplayManager.DisplayListener {
        AudioDisplayListener() {
        }

        @Override // android.hardware.display.DisplayManager.DisplayListener
        public void onDisplayAdded(int displayId) {
        }

        @Override // android.hardware.display.DisplayManager.DisplayListener
        public void onDisplayRemoved(int displayId) {
        }

        @Override // android.hardware.display.DisplayManager.DisplayListener
        public void onDisplayChanged(int displayId) {
            RotationHelper.updateOrientation();
        }
    }
}
