package com.mediatek.view.impl;

import com.mediatek.appresolutiontuner.OpenMsyncAppList;
import com.mediatek.view.MsyncExt;
/* loaded from: classes.dex */
public class MsyncExtimpl extends MsyncExt {
    private static final String TAG = "MsyncExt";
    private boolean mIsContainPackageName = false;
    private String mPackageName;

    public MsyncExtimpl() {
        if (!OpenMsyncAppList.getInstance().isRead()) {
            openNewTread();
        }
    }

    /* JADX WARN: Type inference failed for: r0v0, types: [com.mediatek.view.impl.MsyncExtimpl$1] */
    private void openNewTread() {
        new Thread() { // from class: com.mediatek.view.impl.MsyncExtimpl.1
            @Override // java.lang.Thread, java.lang.Runnable
            public void run() {
                OpenMsyncAppList.getInstance().loadOpenMsyncAppList();
            }
        }.start();
    }

    public boolean isOpenMsyncPackage(String mPackageName) {
        boolean isScaledBySurfaceView = OpenMsyncAppList.getInstance().isScaledBySurfaceView(mPackageName);
        this.mIsContainPackageName = isScaledBySurfaceView;
        return isScaledBySurfaceView;
    }
}
