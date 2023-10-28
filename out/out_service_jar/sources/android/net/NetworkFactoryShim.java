package android.net;

import android.os.Looper;
import android.os.Message;
import java.io.FileDescriptor;
import java.io.PrintWriter;
/* loaded from: classes.dex */
interface NetworkFactoryShim {
    void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr);

    Looper getLooper();

    NetworkProvider getProvider();

    int getRequestCount();

    int getSerialNumber();

    Message obtainMessage(int i, int i2, int i3, Object obj);

    void reevaluateAllRequests();

    void register(String str);

    void releaseRequestAsUnfulfillableByAnyFactory(NetworkRequest networkRequest);

    void setCapabilityFilter(NetworkCapabilities networkCapabilities);

    void setScoreFilter(int i);

    void setScoreFilter(NetworkScore networkScore);

    void terminate();

    default void registerIgnoringScore(String logTag) {
        throw new UnsupportedOperationException();
    }
}
