package com.transsion.hubcore.app;

import android.app.Activity;
import android.app.ActivityThread;
import android.app.Application;
import android.app.IApplicationThread;
import android.app.Notification;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Parcel;
import android.os.RemoteException;
import android.widget.RemoteViews;
import com.transsion.hubcore.utils.TranClassInfo;
import java.util.HashMap;
import java.util.function.Supplier;
/* loaded from: classes4.dex */
public interface ITranActivityThread {
    public static final TranClassInfo<ITranActivityThread> classInfo = new TranClassInfo<>("com.transsion.hubcore.app.TranActivityThreadImpl", ITranActivityThread.class, new Supplier() { // from class: com.transsion.hubcore.app.ITranActivityThread$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return ITranActivityThread.lambda$static$0();
        }
    });

    static /* synthetic */ ITranActivityThread lambda$static$0() {
        return new ITranActivityThread() { // from class: com.transsion.hubcore.app.ITranActivityThread.1
        };
    }

    static ITranActivityThread Instance() {
        return classInfo.getImpl();
    }

    default void onAttach() {
    }

    default void onBindApplication(String processName, ApplicationInfo applicationInfo, Configuration configuration) {
    }

    default void onHandleConfigurationChanged(int configDiff) {
    }

    default boolean onOSAppMessage(int messageId, Bundle data, int callingUid, int callingPid) {
        return false;
    }

    default Boolean onTransactIApplicationThread(IApplicationThread stub, int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        return null;
    }

    default void onActivityLifecycleEvent(int id, ComponentName cn) {
    }

    default RemoteViews createGameHeadsUpViewLice(Context context, Notification notification, boolean increasedheight, Notification.Builder builder) {
        return null;
    }

    default RemoteViews createAppLockViewLice(Context context, Notification notification, boolean increasedheight, Notification.Builder builder) {
        return null;
    }

    default RemoteViews createAppLockAndGameModeViewLice(Context context, Notification notification, boolean increasedheight, Notification.Builder builder) {
        return null;
    }

    default Intent generateApkInfoForSUN(Intent intent, Context context) {
        return null;
    }

    default void setButtonVisible(RemoteViews contentView, Notification notification, int actionSize) {
    }

    default void updateButtonVisible(RemoteViews contentView, Notification notification, int numActions) {
    }

    default boolean enableHeadsUpContentView(Notification notification) {
        return true;
    }

    default boolean isSameMultiWindowingMode(int currentMultiWindowingMode, int newMultiWindowingMode) {
        return false;
    }

    default boolean includeInBlackList(Activity activity, Configuration newConfiguration, boolean newConfigThunderBack, boolean oldConfigThunderBack) {
        return false;
    }

    default void performResumeActivity(Application app) {
    }

    default void putToActivityInstanceCount(ActivityThread.ActivityClientRecord r, String packageName, String name, HashMap<String, Integer> activityInstanceCount) {
    }

    default void checkTime(long startTime, String where) {
    }
}
