package com.android.server.hdmi;

import android.util.Slog;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.ToIntFunction;
/* loaded from: classes.dex */
final class RequestSadAction extends HdmiCecFeatureAction {
    private static final int MAX_SAD_PER_REQUEST = 4;
    private static final int RETRY_COUNTER_MAX = 1;
    private static final int STATE_WAITING_FOR_REPORT_SAD = 1;
    private static final String TAG = "RequestSadAction";
    private final RequestSadCallback mCallback;
    private final List<Integer> mCecCodecsToQuery;
    private int mQueriedSadCount;
    private final List<byte[]> mSupportedSads;
    private final int mTargetAddress;
    private int mTimeoutRetry;

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public interface RequestSadCallback {
        void onRequestSadDone(List<byte[]> list);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public RequestSadAction(HdmiCecLocalDevice source, int targetAddress, RequestSadCallback callback) {
        super(source);
        ArrayList arrayList = new ArrayList();
        this.mCecCodecsToQuery = arrayList;
        this.mSupportedSads = new ArrayList();
        this.mQueriedSadCount = 0;
        this.mTimeoutRetry = 0;
        this.mTargetAddress = targetAddress;
        this.mCallback = (RequestSadCallback) Objects.requireNonNull(callback);
        HdmiCecConfig hdmiCecConfig = localDevice().mService.getHdmiCecConfig();
        if (hdmiCecConfig.getIntValue("query_sad_lpcm") == 1) {
            arrayList.add(1);
        }
        if (hdmiCecConfig.getIntValue("query_sad_dd") == 1) {
            arrayList.add(2);
        }
        if (hdmiCecConfig.getIntValue("query_sad_mpeg1") == 1) {
            arrayList.add(3);
        }
        if (hdmiCecConfig.getIntValue("query_sad_mp3") == 1) {
            arrayList.add(4);
        }
        if (hdmiCecConfig.getIntValue("query_sad_mpeg2") == 1) {
            arrayList.add(5);
        }
        if (hdmiCecConfig.getIntValue("query_sad_aac") == 1) {
            arrayList.add(6);
        }
        if (hdmiCecConfig.getIntValue("query_sad_dts") == 1) {
            arrayList.add(7);
        }
        if (hdmiCecConfig.getIntValue("query_sad_atrac") == 1) {
            arrayList.add(8);
        }
        if (hdmiCecConfig.getIntValue("query_sad_onebitaudio") == 1) {
            arrayList.add(9);
        }
        if (hdmiCecConfig.getIntValue("query_sad_ddp") == 1) {
            arrayList.add(10);
        }
        if (hdmiCecConfig.getIntValue("query_sad_dtshd") == 1) {
            arrayList.add(11);
        }
        if (hdmiCecConfig.getIntValue("query_sad_truehd") == 1) {
            arrayList.add(12);
        }
        if (hdmiCecConfig.getIntValue("query_sad_dst") == 1) {
            arrayList.add(13);
        }
        if (hdmiCecConfig.getIntValue("query_sad_wmapro") == 1) {
            arrayList.add(14);
        }
        if (hdmiCecConfig.getIntValue("query_sad_max") == 1) {
            arrayList.add(15);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.hdmi.HdmiCecFeatureAction
    public boolean start() {
        querySad();
        return true;
    }

    private void querySad() {
        if (this.mQueriedSadCount >= this.mCecCodecsToQuery.size()) {
            wrapUpAndFinish();
            return;
        }
        List<Integer> list = this.mCecCodecsToQuery;
        int[] codecsToQuery = list.subList(this.mQueriedSadCount, Math.min(list.size(), this.mQueriedSadCount + 4)).stream().mapToInt(new ToIntFunction() { // from class: com.android.server.hdmi.RequestSadAction$$ExternalSyntheticLambda0
            @Override // java.util.function.ToIntFunction
            public final int applyAsInt(Object obj) {
                int intValue;
                intValue = ((Integer) obj).intValue();
                return intValue;
            }
        }).toArray();
        sendCommand(HdmiCecMessageBuilder.buildRequestShortAudioDescriptor(getSourceAddress(), this.mTargetAddress, codecsToQuery));
        this.mState = 1;
        addTimer(this.mState, 2000);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.hdmi.HdmiCecFeatureAction
    public boolean processCommand(HdmiCecMessage cmd) {
        if (this.mState == 1 && this.mTargetAddress == cmd.getSource()) {
            if (cmd.getOpcode() == 163) {
                if (cmd.getParams() == null || cmd.getParams().length == 0 || cmd.getParams().length % 3 != 0) {
                    return true;
                }
                for (int i = 0; i < cmd.getParams().length - 2; i += 3) {
                    if (isValidCodec(cmd.getParams()[i])) {
                        byte[] sad = {cmd.getParams()[i], cmd.getParams()[i + 1], cmd.getParams()[i + 2]};
                        updateResult(sad);
                    } else {
                        Slog.w(TAG, "Dropped invalid codec " + ((int) cmd.getParams()[i]) + ".");
                    }
                }
                int i2 = this.mQueriedSadCount;
                this.mQueriedSadCount = i2 + 4;
                this.mTimeoutRetry = 0;
                querySad();
                return true;
            }
            if (cmd.getOpcode() == 0 && (cmd.getParams()[0] & 255) == 164) {
                if ((cmd.getParams()[1] & 255) == 0) {
                    wrapUpAndFinish();
                    return true;
                } else if ((cmd.getParams()[1] & 255) == 3) {
                    this.mQueriedSadCount += 4;
                    this.mTimeoutRetry = 0;
                    querySad();
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    private boolean isValidCodec(byte codec) {
        return (codec & 255) > 0 && (codec & 255) <= 15;
    }

    private void updateResult(byte[] sad) {
        this.mSupportedSads.add(sad);
    }

    @Override // com.android.server.hdmi.HdmiCecFeatureAction
    void handleTimerEvent(int state) {
        if (this.mState == state && state == 1) {
            int i = this.mTimeoutRetry + 1;
            this.mTimeoutRetry = i;
            if (i <= 1) {
                querySad();
            } else {
                wrapUpAndFinish();
            }
        }
    }

    private void wrapUpAndFinish() {
        this.mCallback.onRequestSadDone(this.mSupportedSads);
        finish();
    }
}
