package android.app;

import android.app.IActivityThreadLice;
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
import com.transsion.annotation.OSBridge;
import com.transsion.lice.LiceInfo;
import java.io.InputStream;
import java.util.function.Supplier;
@OSBridge(client = OSBridge.Client.LICE_INTERFACE_FRAMEWORK)
/* loaded from: classes.dex */
public interface IActivityThreadLice {
    public static final boolean DEBUG = true;
    public static final String TAG = "IActivityThreadLice";
    public static final LiceInfo<IActivityThreadLice> sLiceInfo = new LiceInfo<>("com.transsion.app.ActivityThreadLice", IActivityThreadLice.class, new Supplier() { // from class: android.app.IActivityThreadLice$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new IActivityThreadLice.DefaultImpl();
        }
    });

    /* loaded from: classes.dex */
    public static class DefaultImpl implements IActivityThreadLice {
    }

    static IActivityThreadLice Instance() {
        return sLiceInfo.getImpl();
    }

    default void onattach() {
    }

    default void onbindApplication(String processName, ApplicationInfo applicationInfo, Configuration configuration) {
    }

    default void onhandleConfigurationChanged(int configDiff) {
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

    default void setButtonVisiable(RemoteViews contentView, Notification notification, int actionSize) {
    }

    default void updateButtonVisiable(RemoteViews contentView, Notification notification, int numActions) {
    }

    default boolean enableHeadsUpContentView(Notification notification) {
        return true;
    }

    default boolean shouldInterceptRawActivityEmbedding() {
        return false;
    }

    default void onActivityEmbeddingInit(LoadedApk loadedApk) {
    }

    default void onSplitControllerCallback(Activity top, boolean isInPrimarySide, boolean isInSecondarySide, String source) {
    }

    default InputStream loadOsDefaultWallpaperInputStream(Context context, int which, boolean isSmallScreen) {
        return null;
    }

    default InputStream loadDefaultWallpaperInputStream(Context context, int which, boolean isSmallScreen) {
        return null;
    }

    default void setOsDefaultWallpaper(Context context, int which, String wallpaperName) {
    }

    default void onHandleBindApplication(Bundle coreSettings, int hookFlag) {
    }
}
