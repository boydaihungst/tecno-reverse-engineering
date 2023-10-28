package android.media.tv.tuner.frontend;

import android.annotation.SystemApi;
@SystemApi
/* loaded from: classes2.dex */
public class Atsc3PlpInfo {
    private final boolean mLlsFlag;
    private final int mPlpId;

    private Atsc3PlpInfo(int plpId, boolean llsFlag) {
        this.mPlpId = plpId;
        this.mLlsFlag = llsFlag;
    }

    public int getPlpId() {
        return this.mPlpId;
    }

    public boolean getLlsFlag() {
        return this.mLlsFlag;
    }
}
