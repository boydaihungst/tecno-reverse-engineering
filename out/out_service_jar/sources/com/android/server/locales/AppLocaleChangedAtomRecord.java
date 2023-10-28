package com.android.server.locales;
/* loaded from: classes.dex */
public final class AppLocaleChangedAtomRecord {
    final int mCallingUid;
    int mTargetUid = -1;
    String mNewLocales = "";
    String mPrevLocales = "";
    int mStatus = 0;

    /* JADX INFO: Access modifiers changed from: package-private */
    public AppLocaleChangedAtomRecord(int callingUid) {
        this.mCallingUid = callingUid;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setNewLocales(String newLocales) {
        this.mNewLocales = newLocales;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setTargetUid(int targetUid) {
        this.mTargetUid = targetUid;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setPrevLocales(String prevLocales) {
        this.mPrevLocales = prevLocales;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setStatus(int status) {
        this.mStatus = status;
    }
}
