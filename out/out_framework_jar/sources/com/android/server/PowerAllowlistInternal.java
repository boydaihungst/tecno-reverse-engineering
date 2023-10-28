package com.android.server;
/* loaded from: classes4.dex */
public interface PowerAllowlistInternal {

    /* loaded from: classes4.dex */
    public interface TempAllowlistChangeListener {
        void onAppAdded(int i);

        void onAppRemoved(int i);
    }

    void registerTempAllowlistChangeListener(TempAllowlistChangeListener tempAllowlistChangeListener);

    void unregisterTempAllowlistChangeListener(TempAllowlistChangeListener tempAllowlistChangeListener);
}
