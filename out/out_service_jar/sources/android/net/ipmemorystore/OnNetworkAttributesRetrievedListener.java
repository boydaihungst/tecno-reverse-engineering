package android.net.ipmemorystore;

import android.net.ipmemorystore.IOnNetworkAttributesRetrievedListener;
/* loaded from: classes.dex */
public interface OnNetworkAttributesRetrievedListener {
    void onNetworkAttributesRetrieved(Status status, String str, NetworkAttributes networkAttributes);

    static IOnNetworkAttributesRetrievedListener toAIDL(OnNetworkAttributesRetrievedListener listener) {
        return new IOnNetworkAttributesRetrievedListener.Stub() { // from class: android.net.ipmemorystore.OnNetworkAttributesRetrievedListener.1
            @Override // android.net.ipmemorystore.IOnNetworkAttributesRetrievedListener
            public void onNetworkAttributesRetrieved(StatusParcelable statusParcelable, String l2Key, NetworkAttributesParcelable networkAttributesParcelable) {
                OnNetworkAttributesRetrievedListener onNetworkAttributesRetrievedListener = OnNetworkAttributesRetrievedListener.this;
                if (onNetworkAttributesRetrievedListener != null) {
                    onNetworkAttributesRetrievedListener.onNetworkAttributesRetrieved(new Status(statusParcelable), l2Key, networkAttributesParcelable == null ? null : new NetworkAttributes(networkAttributesParcelable));
                }
            }

            @Override // android.net.ipmemorystore.IOnNetworkAttributesRetrievedListener
            public int getInterfaceVersion() {
                return 10;
            }

            @Override // android.net.ipmemorystore.IOnNetworkAttributesRetrievedListener
            public String getInterfaceHash() {
                return "d5ea5eb3ddbdaa9a986ce6ba70b0804ca3e39b0c";
            }
        };
    }
}
