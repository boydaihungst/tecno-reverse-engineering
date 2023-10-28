package com.android.server.hdmi;

import android.hardware.hdmi.IHdmiControlCallback;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.util.Pair;
import android.util.Slog;
import com.android.server.hdmi.HdmiControlService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public abstract class HdmiCecFeatureAction {
    protected static final int MSG_TIMEOUT = 100;
    protected static final int STATE_NONE = 0;
    private static final String TAG = "HdmiCecFeatureAction";
    protected ActionTimer mActionTimer;
    final List<IHdmiControlCallback> mCallbacks;
    private ArrayList<Pair<HdmiCecFeatureAction, Runnable>> mOnFinishedCallbacks;
    private final HdmiControlService mService;
    private final HdmiCecLocalDevice mSource;
    protected int mState;

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public interface ActionTimer {
        void clearTimerMessage();

        void sendTimerMessage(int i, long j);
    }

    abstract void handleTimerEvent(int i);

    /* JADX INFO: Access modifiers changed from: package-private */
    public abstract boolean processCommand(HdmiCecMessage hdmiCecMessage);

    /* JADX INFO: Access modifiers changed from: package-private */
    public abstract boolean start();

    /* JADX INFO: Access modifiers changed from: package-private */
    public HdmiCecFeatureAction(HdmiCecLocalDevice source) {
        this(source, new ArrayList());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public HdmiCecFeatureAction(HdmiCecLocalDevice source, IHdmiControlCallback callback) {
        this(source, Arrays.asList(callback));
    }

    HdmiCecFeatureAction(HdmiCecLocalDevice source, List<IHdmiControlCallback> callbacks) {
        this.mState = 0;
        this.mCallbacks = new ArrayList();
        for (IHdmiControlCallback callback : callbacks) {
            addCallback(callback);
        }
        this.mSource = source;
        HdmiControlService service = source.getService();
        this.mService = service;
        this.mActionTimer = createActionTimer(service.getServiceLooper());
    }

    void setActionTimer(ActionTimer actionTimer) {
        this.mActionTimer = actionTimer;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class ActionTimerHandler extends Handler implements ActionTimer {
        public ActionTimerHandler(Looper looper) {
            super(looper);
        }

        @Override // com.android.server.hdmi.HdmiCecFeatureAction.ActionTimer
        public void sendTimerMessage(int state, long delayMillis) {
            sendMessageDelayed(obtainMessage(100, state, 0), delayMillis);
        }

        @Override // com.android.server.hdmi.HdmiCecFeatureAction.ActionTimer
        public void clearTimerMessage() {
            removeMessages(100);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 100:
                    HdmiCecFeatureAction.this.handleTimerEvent(msg.arg1);
                    return;
                default:
                    Slog.w(HdmiCecFeatureAction.TAG, "Unsupported message:" + msg.what);
                    return;
            }
        }
    }

    private ActionTimer createActionTimer(Looper looper) {
        return new ActionTimerHandler(looper);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void addTimer(int state, int delayMillis) {
        this.mActionTimer.sendTimerMessage(state, delayMillis);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean started() {
        return this.mState != 0;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public final void sendCommand(HdmiCecMessage cmd) {
        this.mService.sendCecCommand(cmd);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public final void sendCommand(HdmiCecMessage cmd, HdmiControlService.SendMessageCallback callback) {
        this.mService.sendCecCommand(cmd, callback);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public final void addAndStartAction(HdmiCecFeatureAction action) {
        this.mSource.addAndStartAction(action);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public final <T extends HdmiCecFeatureAction> List<T> getActions(Class<T> clazz) {
        return this.mSource.getActions(clazz);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public final HdmiCecMessageCache getCecMessageCache() {
        return this.mSource.getCecMessageCache();
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public final void removeAction(HdmiCecFeatureAction action) {
        this.mSource.removeAction(action);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public final <T extends HdmiCecFeatureAction> void removeAction(Class<T> clazz) {
        this.mSource.removeActionExcept(clazz, null);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public final <T extends HdmiCecFeatureAction> void removeActionExcept(Class<T> clazz, HdmiCecFeatureAction exception) {
        this.mSource.removeActionExcept(clazz, exception);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public final void pollDevices(HdmiControlService.DevicePollingCallback callback, int pickStrategy, int retryCount) {
        this.mService.pollDevices(callback, getSourceAddress(), pickStrategy, retryCount);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void clear() {
        this.mState = 0;
        this.mActionTimer.clearTimerMessage();
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void finish() {
        finish(true);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void finish(boolean removeSelf) {
        clear();
        if (removeSelf) {
            removeAction(this);
        }
        ArrayList<Pair<HdmiCecFeatureAction, Runnable>> arrayList = this.mOnFinishedCallbacks;
        if (arrayList != null) {
            Iterator<Pair<HdmiCecFeatureAction, Runnable>> it = arrayList.iterator();
            while (it.hasNext()) {
                Pair<HdmiCecFeatureAction, Runnable> actionCallbackPair = it.next();
                if (((HdmiCecFeatureAction) actionCallbackPair.first).mState != 0) {
                    ((Runnable) actionCallbackPair.second).run();
                }
            }
            this.mOnFinishedCallbacks = null;
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public final HdmiCecLocalDevice localDevice() {
        return this.mSource;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public final HdmiCecLocalDevicePlayback playback() {
        return (HdmiCecLocalDevicePlayback) this.mSource;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public final HdmiCecLocalDeviceSource source() {
        return (HdmiCecLocalDeviceSource) this.mSource;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public final HdmiCecLocalDeviceTv tv() {
        return (HdmiCecLocalDeviceTv) this.mSource;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public final HdmiCecLocalDeviceAudioSystem audioSystem() {
        return (HdmiCecLocalDeviceAudioSystem) this.mSource;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public final int getSourceAddress() {
        return this.mSource.getDeviceInfo().getLogicalAddress();
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public final int getSourcePath() {
        return this.mSource.getDeviceInfo().getPhysicalAddress();
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public final void sendUserControlPressedAndReleased(int targetAddress, int uiCommand) {
        this.mSource.sendUserControlPressedAndReleased(targetAddress, uiCommand);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public final void addOnFinishedCallback(HdmiCecFeatureAction action, Runnable runnable) {
        if (this.mOnFinishedCallbacks == null) {
            this.mOnFinishedCallbacks = new ArrayList<>();
        }
        this.mOnFinishedCallbacks.add(Pair.create(action, runnable));
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void finishWithCallback(int returnCode) {
        invokeCallback(returnCode);
        finish();
    }

    public void addCallback(IHdmiControlCallback callback) {
        this.mCallbacks.add(callback);
    }

    private void invokeCallback(int result) {
        try {
            for (IHdmiControlCallback callback : this.mCallbacks) {
                if (callback != null) {
                    callback.onComplete(result);
                }
            }
        } catch (RemoteException e) {
            Slog.e(TAG, "Callback failed:" + e);
        }
    }
}
