package android.view.contentcapture;

import android.os.ParcelFileDescriptor;
/* loaded from: classes3.dex */
public interface DataShareWriteAdapter {
    void onRejected();

    void onWrite(ParcelFileDescriptor parcelFileDescriptor);

    default void onError(int errorCode) {
    }
}
