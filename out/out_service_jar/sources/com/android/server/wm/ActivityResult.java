package com.android.server.wm;

import android.app.ResultInfo;
import android.content.Intent;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public final class ActivityResult extends ResultInfo {
    final ActivityRecord mFrom;

    public ActivityResult(ActivityRecord from, String resultWho, int requestCode, int resultCode, Intent data) {
        super(resultWho, requestCode, resultCode, data);
        this.mFrom = from;
    }
}
