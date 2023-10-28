package com.android.server.devicepolicy;

import android.content.ComponentName;
import android.os.UserHandle;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public final class CallerIdentity {
    private final ComponentName mComponentName;
    private final String mPackageName;
    private final int mUid;

    /* JADX INFO: Access modifiers changed from: package-private */
    public CallerIdentity(int uid, String packageName, ComponentName componentName) {
        this.mUid = uid;
        this.mPackageName = packageName;
        this.mComponentName = componentName;
    }

    public int getUid() {
        return this.mUid;
    }

    public int getUserId() {
        return UserHandle.getUserId(this.mUid);
    }

    public UserHandle getUserHandle() {
        return UserHandle.getUserHandleForUid(this.mUid);
    }

    public String getPackageName() {
        return this.mPackageName;
    }

    public ComponentName getComponentName() {
        return this.mComponentName;
    }

    public boolean hasAdminComponent() {
        return this.mComponentName != null;
    }

    public boolean hasPackage() {
        return this.mPackageName != null;
    }

    public String toString() {
        StringBuilder builder = new StringBuilder("CallerIdentity[uid=").append(this.mUid);
        if (this.mPackageName != null) {
            builder.append(", pkg=").append(this.mPackageName);
        }
        if (this.mComponentName != null) {
            builder.append(", cmp=").append(this.mComponentName.flattenToShortString());
        }
        return builder.append("]").toString();
    }
}
