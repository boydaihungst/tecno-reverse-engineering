package android.net.ipmemorystore;

import android.net.ipmemorystore.IOnL2KeyResponseListener;
/* loaded from: classes.dex */
public interface OnL2KeyResponseListener {
    void onL2KeyResponse(Status status, String str);

    static IOnL2KeyResponseListener toAIDL(OnL2KeyResponseListener listener) {
        return new IOnL2KeyResponseListener.Stub() { // from class: android.net.ipmemorystore.OnL2KeyResponseListener.1
            @Override // android.net.ipmemorystore.IOnL2KeyResponseListener
            public void onL2KeyResponse(StatusParcelable statusParcelable, String l2Key) {
                OnL2KeyResponseListener onL2KeyResponseListener = OnL2KeyResponseListener.this;
                if (onL2KeyResponseListener != null) {
                    onL2KeyResponseListener.onL2KeyResponse(new Status(statusParcelable), l2Key);
                }
            }

            @Override // android.net.ipmemorystore.IOnL2KeyResponseListener
            public int getInterfaceVersion() {
                return 10;
            }

            @Override // android.net.ipmemorystore.IOnL2KeyResponseListener
            public String getInterfaceHash() {
                return "d5ea5eb3ddbdaa9a986ce6ba70b0804ca3e39b0c";
            }
        };
    }
}
