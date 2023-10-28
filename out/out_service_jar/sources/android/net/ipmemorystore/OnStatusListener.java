package android.net.ipmemorystore;

import android.net.ipmemorystore.IOnStatusListener;
/* loaded from: classes.dex */
public interface OnStatusListener {
    void onComplete(Status status);

    static IOnStatusListener toAIDL(OnStatusListener listener) {
        return new IOnStatusListener.Stub() { // from class: android.net.ipmemorystore.OnStatusListener.1
            @Override // android.net.ipmemorystore.IOnStatusListener
            public void onComplete(StatusParcelable statusParcelable) {
                OnStatusListener onStatusListener = OnStatusListener.this;
                if (onStatusListener != null) {
                    onStatusListener.onComplete(new Status(statusParcelable));
                }
            }

            @Override // android.net.ipmemorystore.IOnStatusListener
            public int getInterfaceVersion() {
                return 10;
            }

            @Override // android.net.ipmemorystore.IOnStatusListener
            public String getInterfaceHash() {
                return "d5ea5eb3ddbdaa9a986ce6ba70b0804ca3e39b0c";
            }
        };
    }
}
