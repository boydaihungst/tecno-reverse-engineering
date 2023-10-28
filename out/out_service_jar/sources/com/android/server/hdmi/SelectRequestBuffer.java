package com.android.server.hdmi;

import android.hardware.hdmi.IHdmiControlCallback;
import android.os.RemoteException;
import android.util.Slog;
/* loaded from: classes.dex */
public class SelectRequestBuffer {
    public static final SelectRequestBuffer EMPTY_BUFFER = new SelectRequestBuffer() { // from class: com.android.server.hdmi.SelectRequestBuffer.1
        @Override // com.android.server.hdmi.SelectRequestBuffer
        public void process() {
        }
    };
    private static final String TAG = "SelectRequestBuffer";
    private SelectRequest mRequest;

    /* loaded from: classes.dex */
    public static abstract class SelectRequest {
        protected final IHdmiControlCallback mCallback;
        protected final int mId;
        protected final HdmiControlService mService;

        public abstract void process();

        public SelectRequest(HdmiControlService service, int id, IHdmiControlCallback callback) {
            this.mService = service;
            this.mId = id;
            this.mCallback = callback;
        }

        protected HdmiCecLocalDeviceTv tv() {
            return this.mService.tv();
        }

        protected HdmiCecLocalDeviceAudioSystem audioSystem() {
            return this.mService.audioSystem();
        }

        protected boolean isLocalDeviceReady() {
            if (tv() == null) {
                Slog.e(SelectRequestBuffer.TAG, "Local tv device not available");
                invokeCallback(2);
                return false;
            }
            return true;
        }

        private void invokeCallback(int reason) {
            try {
                IHdmiControlCallback iHdmiControlCallback = this.mCallback;
                if (iHdmiControlCallback != null) {
                    iHdmiControlCallback.onComplete(reason);
                }
            } catch (RemoteException e) {
                Slog.e(SelectRequestBuffer.TAG, "Invoking callback failed:" + e);
            }
        }
    }

    /* loaded from: classes.dex */
    public static class DeviceSelectRequest extends SelectRequest {
        private DeviceSelectRequest(HdmiControlService srv, int id, IHdmiControlCallback callback) {
            super(srv, id, callback);
        }

        @Override // com.android.server.hdmi.SelectRequestBuffer.SelectRequest
        public void process() {
            if (isLocalDeviceReady()) {
                Slog.v(SelectRequestBuffer.TAG, "calling delayed deviceSelect id:" + this.mId);
                tv().deviceSelect(this.mId, this.mCallback);
            }
        }
    }

    /* loaded from: classes.dex */
    public static class PortSelectRequest extends SelectRequest {
        private PortSelectRequest(HdmiControlService srv, int id, IHdmiControlCallback callback) {
            super(srv, id, callback);
        }

        @Override // com.android.server.hdmi.SelectRequestBuffer.SelectRequest
        public void process() {
            if (isLocalDeviceReady()) {
                Slog.v(SelectRequestBuffer.TAG, "calling delayed portSelect id:" + this.mId);
                HdmiCecLocalDeviceTv tv = tv();
                if (tv != null) {
                    tv.doManualPortSwitching(this.mId, this.mCallback);
                    return;
                }
                HdmiCecLocalDeviceAudioSystem audioSystem = audioSystem();
                if (audioSystem != null) {
                    audioSystem.doManualPortSwitching(this.mId, this.mCallback);
                }
            }
        }
    }

    public static DeviceSelectRequest newDeviceSelect(HdmiControlService srv, int id, IHdmiControlCallback callback) {
        return new DeviceSelectRequest(srv, id, callback);
    }

    public static PortSelectRequest newPortSelect(HdmiControlService srv, int id, IHdmiControlCallback callback) {
        return new PortSelectRequest(srv, id, callback);
    }

    public void set(SelectRequest request) {
        this.mRequest = request;
    }

    public void process() {
        SelectRequest selectRequest = this.mRequest;
        if (selectRequest != null) {
            selectRequest.process();
            clear();
        }
    }

    public void clear() {
        this.mRequest = null;
    }
}
