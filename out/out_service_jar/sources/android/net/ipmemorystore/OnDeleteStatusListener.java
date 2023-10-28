package android.net.ipmemorystore;

import android.net.ipmemorystore.IOnStatusAndCountListener;
/* loaded from: classes.dex */
public interface OnDeleteStatusListener {
    void onComplete(Status status, int i);

    static IOnStatusAndCountListener toAIDL(OnDeleteStatusListener listener) {
        return new IOnStatusAndCountListener.Stub() { // from class: android.net.ipmemorystore.OnDeleteStatusListener.1
            @Override // android.net.ipmemorystore.IOnStatusAndCountListener
            public void onComplete(StatusParcelable statusParcelable, int deletedRecords) {
                OnDeleteStatusListener onDeleteStatusListener = OnDeleteStatusListener.this;
                if (onDeleteStatusListener != null) {
                    onDeleteStatusListener.onComplete(new Status(statusParcelable), deletedRecords);
                }
            }

            @Override // android.net.ipmemorystore.IOnStatusAndCountListener
            public int getInterfaceVersion() {
                return 10;
            }

            @Override // android.net.ipmemorystore.IOnStatusAndCountListener
            public String getInterfaceHash() {
                return "d5ea5eb3ddbdaa9a986ce6ba70b0804ca3e39b0c";
            }
        };
    }
}
