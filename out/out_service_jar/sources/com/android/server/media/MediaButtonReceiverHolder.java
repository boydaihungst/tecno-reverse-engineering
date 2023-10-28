package com.android.server.media;

import android.app.BroadcastOptions;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ComponentInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import com.android.internal.util.FrameworkStatsLog;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Collections;
import java.util.List;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public final class MediaButtonReceiverHolder {
    private static final String COMPONENT_NAME_USER_ID_DELIM = ",";
    public static final int COMPONENT_TYPE_ACTIVITY = 2;
    public static final int COMPONENT_TYPE_BROADCAST = 1;
    public static final int COMPONENT_TYPE_INVALID = 0;
    public static final int COMPONENT_TYPE_SERVICE = 3;
    private static final boolean DEBUG_KEY_EVENT = true;
    private static final int PACKAGE_MANAGER_COMMON_FLAGS = 786432;
    private static final String TAG = "PendingIntentHolder";
    private final ComponentName mComponentName;
    private final int mComponentType;
    private final String mPackageName;
    private final PendingIntent mPendingIntent;
    private final int mUserId;

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes2.dex */
    public @interface ComponentType {
    }

    public static MediaButtonReceiverHolder unflattenFromString(Context context, String mediaButtonReceiverInfo) {
        String[] tokens;
        ComponentName componentName;
        int componentType;
        if (TextUtils.isEmpty(mediaButtonReceiverInfo) || (tokens = mediaButtonReceiverInfo.split(COMPONENT_NAME_USER_ID_DELIM)) == null || ((tokens.length != 2 && tokens.length != 3) || (componentName = ComponentName.unflattenFromString(tokens[0])) == null)) {
            return null;
        }
        int userId = Integer.parseInt(tokens[1]);
        if (tokens.length == 3) {
            componentType = Integer.parseInt(tokens[2]);
        } else {
            componentType = getComponentType(context, componentName);
        }
        return new MediaButtonReceiverHolder(userId, null, componentName, componentType);
    }

    public static MediaButtonReceiverHolder create(int userId, PendingIntent pendingIntent, String sessionPackageName) {
        if (pendingIntent == null) {
            return null;
        }
        int componentType = getComponentType(pendingIntent);
        ComponentName componentName = getComponentName(pendingIntent, componentType);
        if (componentName != null) {
            return new MediaButtonReceiverHolder(userId, pendingIntent, componentName, componentType);
        }
        Log.w(TAG, "Unresolvable implicit intent is set, pi=" + pendingIntent);
        return new MediaButtonReceiverHolder(userId, pendingIntent, sessionPackageName);
    }

    public static MediaButtonReceiverHolder create(int userId, ComponentName broadcastReceiver) {
        return new MediaButtonReceiverHolder(userId, null, broadcastReceiver, 1);
    }

    private MediaButtonReceiverHolder(int userId, PendingIntent pendingIntent, ComponentName componentName, int componentType) {
        this.mUserId = userId;
        this.mPendingIntent = pendingIntent;
        this.mComponentName = componentName;
        this.mPackageName = componentName.getPackageName();
        this.mComponentType = componentType;
    }

    private MediaButtonReceiverHolder(int userId, PendingIntent pendingIntent, String packageName) {
        this.mUserId = userId;
        this.mPendingIntent = pendingIntent;
        this.mComponentName = null;
        this.mPackageName = packageName;
        this.mComponentType = 0;
    }

    public int getUserId() {
        return this.mUserId;
    }

    public String getPackageName() {
        return this.mPackageName;
    }

    public boolean send(Context context, KeyEvent keyEvent, String callingPackageName, int resultCode, PendingIntent.OnFinished onFinishedListener, Handler handler, long fgsAllowlistDurationMs) {
        String str;
        PendingIntent pendingIntent;
        Bundle bundle;
        Intent mediaButtonIntent = new Intent("android.intent.action.MEDIA_BUTTON");
        mediaButtonIntent.addFlags(268435456);
        mediaButtonIntent.putExtra("android.intent.extra.KEY_EVENT", keyEvent);
        mediaButtonIntent.putExtra("android.intent.extra.PACKAGE_NAME", callingPackageName);
        BroadcastOptions options = BroadcastOptions.makeBasic();
        options.setTemporaryAppAllowlist(fgsAllowlistDurationMs, 0, (int) FrameworkStatsLog.APP_BACKGROUND_RESTRICTIONS_INFO__EXEMPTION_REASON__REASON_MEDIA_BUTTON, "");
        options.setBackgroundActivityStartsAllowed(true);
        if (this.mPendingIntent != null) {
            Log.d(TAG, "Sending " + keyEvent + " to the last known PendingIntent " + this.mPendingIntent);
            try {
                pendingIntent = this.mPendingIntent;
                bundle = options.toBundle();
                str = TAG;
            } catch (PendingIntent.CanceledException e) {
                e = e;
                str = TAG;
            }
            try {
                pendingIntent.send(context, resultCode, mediaButtonIntent, onFinishedListener, handler, null, bundle);
            } catch (PendingIntent.CanceledException e2) {
                e = e2;
                Log.w(str, "Error sending key event to media button receiver " + this.mPendingIntent, e);
                return false;
            }
        } else if (this.mComponentName != null) {
            Log.d(TAG, "Sending " + keyEvent + " to the restored intent " + this.mComponentName + ", type=" + this.mComponentType);
            mediaButtonIntent.setComponent(this.mComponentName);
            UserHandle userHandle = UserHandle.of(this.mUserId);
            try {
                switch (this.mComponentType) {
                    case 2:
                        context.startActivityAsUser(mediaButtonIntent, userHandle);
                        break;
                    case 3:
                        context.createContextAsUser(userHandle, 0).startForegroundService(mediaButtonIntent);
                        break;
                    default:
                        context.sendBroadcastAsUser(mediaButtonIntent, userHandle, null, options.toBundle());
                        break;
                }
            } catch (Exception e3) {
                Log.w(TAG, "Error sending media button to the restored intent " + this.mComponentName + ", type=" + this.mComponentType, e3);
                return false;
            }
        } else {
            Log.e(TAG, "Shouldn't be happen -- pending intent or component name must be set");
            return false;
        }
        return true;
    }

    public String toString() {
        if (this.mPendingIntent != null) {
            return "MBR {pi=" + this.mPendingIntent + ", type=" + this.mComponentType + "}";
        }
        return "Restored MBR {component=" + this.mComponentName + ", type=" + this.mComponentType + "}";
    }

    public String flattenToString() {
        ComponentName componentName = this.mComponentName;
        if (componentName == null) {
            return "";
        }
        return String.join(COMPONENT_NAME_USER_ID_DELIM, componentName.flattenToString(), String.valueOf(this.mUserId), String.valueOf(this.mComponentType));
    }

    private static int getComponentType(PendingIntent pendingIntent) {
        if (pendingIntent.isBroadcast()) {
            return 1;
        }
        if (pendingIntent.isActivity()) {
            return 2;
        }
        if (pendingIntent.isForegroundService() || pendingIntent.isService()) {
            return 3;
        }
        return 0;
    }

    private static int getComponentType(Context context, ComponentName componentName) {
        if (componentName == null) {
            return 0;
        }
        PackageManager pm = context.getPackageManager();
        try {
            ActivityInfo activityInfo = pm.getActivityInfo(componentName, 786433);
            if (activityInfo != null) {
                return 2;
            }
        } catch (PackageManager.NameNotFoundException e) {
        }
        try {
            ServiceInfo serviceInfo = pm.getServiceInfo(componentName, 786436);
            if (serviceInfo != null) {
                return 3;
            }
            return 1;
        } catch (PackageManager.NameNotFoundException e2) {
            return 1;
        }
    }

    private static ComponentName getComponentName(PendingIntent pendingIntent, int componentType) {
        List<ResolveInfo> resolveInfos = Collections.emptyList();
        switch (componentType) {
            case 1:
                resolveInfos = pendingIntent.queryIntentComponents(786434);
                break;
            case 2:
                resolveInfos = pendingIntent.queryIntentComponents(851969);
                break;
            case 3:
                resolveInfos = pendingIntent.queryIntentComponents(786436);
                break;
        }
        for (ResolveInfo resolveInfo : resolveInfos) {
            ComponentInfo componentInfo = getComponentInfo(resolveInfo);
            if (componentInfo != null && TextUtils.equals(componentInfo.packageName, pendingIntent.getCreatorPackage()) && componentInfo.packageName != null && componentInfo.name != null) {
                return new ComponentName(componentInfo.packageName, componentInfo.name);
            }
        }
        return null;
    }

    private static ComponentInfo getComponentInfo(ResolveInfo resolveInfo) {
        if (resolveInfo.activityInfo != null) {
            return resolveInfo.activityInfo;
        }
        if (resolveInfo.serviceInfo != null) {
            return resolveInfo.serviceInfo;
        }
        return null;
    }
}
