package android.net.lowpan;

import android.inputmethodservice.navigationbar.NavigationBarInflaterView;
/* loaded from: classes2.dex */
public class LowpanEnergyScanResult {
    public static final int UNKNOWN = Integer.MAX_VALUE;
    private int mChannel = Integer.MAX_VALUE;
    private int mMaxRssi = Integer.MAX_VALUE;

    public int getChannel() {
        return this.mChannel;
    }

    public int getMaxRssi() {
        return this.mMaxRssi;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setChannel(int x) {
        this.mChannel = x;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setMaxRssi(int x) {
        this.mMaxRssi = x;
    }

    public String toString() {
        return "LowpanEnergyScanResult(channel: " + this.mChannel + ", maxRssi:" + this.mMaxRssi + NavigationBarInflaterView.KEY_CODE_END;
    }
}
