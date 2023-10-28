package com.android.server.display;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Slog;
import com.android.server.display.DisplayManagerService;
import com.transsion.hubcore.server.display.ITranDisplayPowerController;
import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArraySet;
/* loaded from: classes.dex */
public class BrightnessSetting {
    private static final int MSG_BRIGHTNESS_CHANGED = 1;
    private static final String TAG = "BrightnessSetting";
    private float mBrightness;
    private final Handler mHandler = new Handler(Looper.getMainLooper()) { // from class: com.android.server.display.BrightnessSetting.1
        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                float brightnessVal = Float.intBitsToFloat(msg.arg1);
                BrightnessSetting.this.notifyListeners(brightnessVal);
            }
        }
    };
    private final CopyOnWriteArraySet<BrightnessSettingListener> mListeners = new CopyOnWriteArraySet<>();
    private final LogicalDisplay mLogicalDisplay;
    private final PersistentDataStore mPersistentDataStore;
    private final DisplayManagerService.SyncRoot mSyncRoot;

    /* loaded from: classes.dex */
    public interface BrightnessSettingListener {
        void onBrightnessChanged(float f);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public BrightnessSetting(PersistentDataStore persistentDataStore, LogicalDisplay logicalDisplay, DisplayManagerService.SyncRoot syncRoot) {
        this.mPersistentDataStore = persistentDataStore;
        this.mLogicalDisplay = logicalDisplay;
        this.mBrightness = persistentDataStore.getBrightness(logicalDisplay.getPrimaryDisplayDeviceLocked());
        this.mSyncRoot = syncRoot;
    }

    public float getBrightness() {
        float f;
        synchronized (this.mSyncRoot) {
            if (ITranDisplayPowerController.Instance().flexibleDisplayBrightnessHandleSupport(this.mLogicalDisplay.getDisplayIdLocked())) {
                this.mBrightness = ITranDisplayPowerController.Instance().getFlexibleDisplayBrightnessSettings();
            }
            f = this.mBrightness;
        }
        return f;
    }

    public void registerListener(BrightnessSettingListener l) {
        if (this.mListeners.contains(l)) {
            Slog.wtf(TAG, "Duplicate Listener added");
        }
        this.mListeners.add(l);
    }

    public void unregisterListener(BrightnessSettingListener l) {
        this.mListeners.remove(l);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setBrightness(float brightness) {
        if (Float.isNaN(brightness)) {
            Slog.w(TAG, "Attempting to set invalid brightness");
            return;
        }
        synchronized (this.mSyncRoot) {
            if (brightness != this.mBrightness) {
                this.mPersistentDataStore.setBrightness(this.mLogicalDisplay.getPrimaryDisplayDeviceLocked(), brightness);
                if (ITranDisplayPowerController.Instance().flexibleDisplayBrightnessHandleSupport(this.mLogicalDisplay.getDisplayIdLocked())) {
                    ITranDisplayPowerController.Instance().putFlexibleDisplayBrightnessSettings(brightness);
                }
            }
            this.mBrightness = brightness;
            int toSend = Float.floatToIntBits(brightness);
            Message msg = this.mHandler.obtainMessage(1, toSend, 0);
            this.mHandler.sendMessage(msg);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifyListeners(float brightness) {
        Iterator<BrightnessSettingListener> it = this.mListeners.iterator();
        while (it.hasNext()) {
            BrightnessSettingListener l = it.next();
            l.onBrightnessChanged(brightness);
        }
    }
}
