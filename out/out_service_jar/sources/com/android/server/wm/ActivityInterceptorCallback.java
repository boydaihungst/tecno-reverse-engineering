package com.android.server.wm;

import android.app.ActivityOptions;
import android.app.TaskInfo;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ResolveInfo;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
/* loaded from: classes2.dex */
public abstract class ActivityInterceptorCallback {
    public static final int DREAM_MANAGER_ORDERED_ID = 4;
    static final int FIRST_ORDERED_ID = 0;
    static final int LAST_ORDERED_ID = 4;
    public static final int PERMISSION_POLICY_ORDERED_ID = 1;
    public static final int VIRTUAL_DEVICE_SERVICE_ORDERED_ID = 3;

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes2.dex */
    public @interface OrderedId {
    }

    public abstract ActivityInterceptResult intercept(ActivityInterceptorInfo activityInterceptorInfo);

    public void onActivityLaunched(TaskInfo taskInfo, ActivityInfo activityInfo, ActivityInterceptorInfo info) {
    }

    /* loaded from: classes2.dex */
    public static final class ActivityInterceptorInfo {
        public final ActivityInfo aInfo;
        public final String callingFeatureId;
        public final String callingPackage;
        public final int callingPid;
        public final int callingUid;
        public final ActivityOptions checkedOptions;
        public final Runnable clearOptionsAnimation;
        public final Intent intent;
        public final ResolveInfo rInfo;
        public final int realCallingPid;
        public final int realCallingUid;
        public final String resolvedType;
        public final int userId;

        public ActivityInterceptorInfo(int realCallingUid, int realCallingPid, int userId, String callingPackage, String callingFeatureId, Intent intent, ResolveInfo rInfo, ActivityInfo aInfo, String resolvedType, int callingPid, int callingUid, ActivityOptions checkedOptions, Runnable clearOptionsAnimation) {
            this.realCallingUid = realCallingUid;
            this.realCallingPid = realCallingPid;
            this.userId = userId;
            this.callingPackage = callingPackage;
            this.callingFeatureId = callingFeatureId;
            this.intent = intent;
            this.rInfo = rInfo;
            this.aInfo = aInfo;
            this.resolvedType = resolvedType;
            this.callingPid = callingPid;
            this.callingUid = callingUid;
            this.checkedOptions = checkedOptions;
            this.clearOptionsAnimation = clearOptionsAnimation;
        }
    }

    /* loaded from: classes2.dex */
    public static final class ActivityInterceptResult {
        public final ActivityOptions activityOptions;
        public final Intent intent;

        public ActivityInterceptResult(Intent intent, ActivityOptions activityOptions) {
            this.intent = intent;
            this.activityOptions = activityOptions;
        }
    }
}
