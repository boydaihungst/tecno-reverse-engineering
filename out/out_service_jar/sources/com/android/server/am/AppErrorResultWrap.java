package com.android.server.am;
/* loaded from: classes.dex */
public final class AppErrorResultWrap {
    AppErrorResult mResult;
    AppErrorResultWrap mResultWrap;

    public AppErrorResultWrap(AppErrorResult result) {
        this.mResult = result;
    }

    public void set(int res) {
        AppErrorResult appErrorResult = this.mResult;
        if (appErrorResult != null && !appErrorResult.mHasResult) {
            this.mResult.set(res);
        }
    }
}
