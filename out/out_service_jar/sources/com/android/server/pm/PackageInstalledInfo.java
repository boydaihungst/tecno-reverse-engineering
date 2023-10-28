package com.android.server.pm;

import android.util.ExceptionUtils;
import android.util.Slog;
import com.android.server.pm.parsing.pkg.AndroidPackage;
import java.util.ArrayList;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public final class PackageInstalledInfo {
    PackageFreezer mFreezer;
    String mInstallerPackageName;
    ArrayList<AndroidPackage> mLibraryConsumers;
    String mName;
    int[] mNewUsers;
    String mOrigPackage;
    String mOrigPermission;
    int[] mOrigUsers;
    int mReturnCode;
    String mReturnMsg;
    int mUid = -1;
    AndroidPackage mPkg = null;
    PackageRemovedInfo mRemovedInfo = null;

    /* JADX INFO: Access modifiers changed from: package-private */
    public PackageInstalledInfo(int currentStatus) {
        this.mReturnCode = currentStatus;
    }

    public void setError(int code, String msg) {
        setReturnCode(code);
        setReturnMessage(msg);
        Slog.w("PackageManager", msg);
    }

    public void setError(String msg, PackageManagerException e) {
        this.mReturnCode = e.error;
        setReturnMessage(ExceptionUtils.getCompleteMessage(msg, e));
        Slog.w("PackageManager", msg, e);
    }

    public void setReturnCode(int returnCode) {
        this.mReturnCode = returnCode;
    }

    private void setReturnMessage(String returnMsg) {
        this.mReturnMsg = returnMsg;
    }
}
