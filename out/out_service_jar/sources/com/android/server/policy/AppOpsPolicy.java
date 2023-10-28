package com.android.server.policy;

import android.app.AppOpsManager;
import android.app.AppOpsManagerInternal;
import android.app.SyncNotedAppOp;
import android.app.role.OnRoleHoldersChangedListener;
import android.app.role.RoleManager;
import android.content.AttributionSource;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.location.LocationManagerInternal;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PackageTagsList;
import android.os.Process;
import android.os.UserHandle;
import android.service.voice.VoiceInteractionManagerInternal;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import com.android.internal.util.function.HeptFunction;
import com.android.internal.util.function.HexFunction;
import com.android.internal.util.function.QuadFunction;
import com.android.internal.util.function.QuintConsumer;
import com.android.internal.util.function.QuintFunction;
import com.android.internal.util.function.UndecFunction;
import com.android.server.LocalServices;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
/* loaded from: classes2.dex */
public final class AppOpsPolicy implements AppOpsManagerInternal.CheckOpsDelegate {
    private static final String ACTIVITY_RECOGNITION_TAGS = "android:activity_recognition_allow_listed_tags";
    private static final String ACTIVITY_RECOGNITION_TAGS_SEPARATOR = ";";
    private static final String LOG_TAG = AppOpsPolicy.class.getName();
    private final ConcurrentHashMap<Integer, PackageTagsList> mActivityRecognitionTags;
    private final Context mContext;
    private final boolean mIsHotwordDetectionServiceRequired;
    private final ConcurrentHashMap<Integer, PackageTagsList> mLocationTags;
    private final Object mLock = new Object();
    private final SparseArray<PackageTagsList> mPerUidLocationTags;
    private final RoleManager mRoleManager;
    private final IBinder mToken;
    private final VoiceInteractionManagerInternal mVoiceInteractionManagerInternal;

    public AppOpsPolicy(Context context) {
        Binder binder = new Binder();
        this.mToken = binder;
        this.mLocationTags = new ConcurrentHashMap<>();
        this.mPerUidLocationTags = new SparseArray<>();
        this.mActivityRecognitionTags = new ConcurrentHashMap<>();
        this.mContext = context;
        RoleManager roleManager = (RoleManager) context.getSystemService(RoleManager.class);
        this.mRoleManager = roleManager;
        this.mVoiceInteractionManagerInternal = (VoiceInteractionManagerInternal) LocalServices.getService(VoiceInteractionManagerInternal.class);
        this.mIsHotwordDetectionServiceRequired = isHotwordDetectionServiceRequired(context.getPackageManager());
        LocationManagerInternal locationManagerInternal = (LocationManagerInternal) LocalServices.getService(LocationManagerInternal.class);
        locationManagerInternal.setLocationPackageTagsListener(new LocationManagerInternal.LocationPackageTagsListener() { // from class: com.android.server.policy.AppOpsPolicy$$ExternalSyntheticLambda0
            public final void onLocationPackageTagsChanged(int i, PackageTagsList packageTagsList) {
                AppOpsPolicy.this.m5865lambda$new$0$comandroidserverpolicyAppOpsPolicy(i, packageTagsList);
            }
        });
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.PACKAGE_ADDED");
        intentFilter.addAction("android.intent.action.PACKAGE_CHANGED");
        intentFilter.addDataScheme("package");
        context.registerReceiverAsUser(new BroadcastReceiver() { // from class: com.android.server.policy.AppOpsPolicy.1
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent) {
                Uri uri = intent.getData();
                if (uri == null) {
                    return;
                }
                String packageName = uri.getSchemeSpecificPart();
                if (TextUtils.isEmpty(packageName)) {
                    return;
                }
                List<String> activityRecognizers = AppOpsPolicy.this.mRoleManager.getRoleHolders("android.app.role.SYSTEM_ACTIVITY_RECOGNIZER");
                if (activityRecognizers.contains(packageName)) {
                    AppOpsPolicy.this.updateActivityRecognizerTags(packageName);
                }
            }
        }, UserHandle.SYSTEM, intentFilter, null, null);
        roleManager.addOnRoleHoldersChangedListenerAsUser(context.getMainExecutor(), new OnRoleHoldersChangedListener() { // from class: com.android.server.policy.AppOpsPolicy$$ExternalSyntheticLambda1
            public final void onRoleHoldersChanged(String str, UserHandle userHandle) {
                AppOpsPolicy.this.m5866lambda$new$1$comandroidserverpolicyAppOpsPolicy(str, userHandle);
            }
        }, UserHandle.SYSTEM);
        initializeActivityRecognizersTags();
        PackageManager pm = context.getPackageManager();
        if (!pm.hasSystemFeature("android.hardware.telephony") && !pm.hasSystemFeature("android.hardware.microphone") && !pm.hasSystemFeature("android.software.telecom")) {
            AppOpsManager appOps = (AppOpsManager) context.getSystemService(AppOpsManager.class);
            appOps.setUserRestrictionForUser(100, true, binder, null, -1);
            appOps.setUserRestrictionForUser(101, true, binder, null, -1);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$new$0$com-android-server-policy-AppOpsPolicy  reason: not valid java name */
    public /* synthetic */ void m5865lambda$new$0$comandroidserverpolicyAppOpsPolicy(int uid, PackageTagsList packageTagsList) {
        synchronized (this.mLock) {
            if (packageTagsList.isEmpty()) {
                this.mPerUidLocationTags.remove(uid);
            } else {
                this.mPerUidLocationTags.set(uid, packageTagsList);
            }
            int appId = UserHandle.getAppId(uid);
            PackageTagsList.Builder appIdTags = new PackageTagsList.Builder(1);
            int size = this.mPerUidLocationTags.size();
            for (int i = 0; i < size; i++) {
                if (UserHandle.getAppId(this.mPerUidLocationTags.keyAt(i)) == appId) {
                    appIdTags.add(this.mPerUidLocationTags.valueAt(i));
                }
            }
            updateAllowListedTagsForPackageLocked(appId, appIdTags.build(), this.mLocationTags);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$new$1$com-android-server-policy-AppOpsPolicy  reason: not valid java name */
    public /* synthetic */ void m5866lambda$new$1$comandroidserverpolicyAppOpsPolicy(String roleName, UserHandle user) {
        if ("android.app.role.SYSTEM_ACTIVITY_RECOGNIZER".equals(roleName)) {
            initializeActivityRecognizersTags();
        }
    }

    private static boolean isHotwordDetectionServiceRequired(PackageManager pm) {
        return false;
    }

    public int checkOperation(int code, int uid, String packageName, String attributionTag, boolean raw, QuintFunction<Integer, Integer, String, String, Boolean, Integer> superImpl) {
        return ((Integer) superImpl.apply(Integer.valueOf(code), Integer.valueOf(resolveUid(code, uid)), packageName, attributionTag, Boolean.valueOf(raw))).intValue();
    }

    public int checkAudioOperation(int code, int usage, int uid, String packageName, QuadFunction<Integer, Integer, Integer, String, Integer> superImpl) {
        return ((Integer) superImpl.apply(Integer.valueOf(code), Integer.valueOf(usage), Integer.valueOf(uid), packageName)).intValue();
    }

    public SyncNotedAppOp noteOperation(int code, int uid, String packageName, String attributionTag, boolean shouldCollectAsyncNotedOp, String message, boolean shouldCollectMessage, HeptFunction<Integer, Integer, String, String, Boolean, String, Boolean, SyncNotedAppOp> superImpl) {
        return (SyncNotedAppOp) superImpl.apply(Integer.valueOf(resolveDatasourceOp(code, uid, packageName, attributionTag)), Integer.valueOf(resolveUid(code, uid)), packageName, attributionTag, Boolean.valueOf(shouldCollectAsyncNotedOp), message, Boolean.valueOf(shouldCollectMessage));
    }

    public SyncNotedAppOp noteProxyOperation(int code, AttributionSource attributionSource, boolean shouldCollectAsyncNotedOp, String message, boolean shouldCollectMessage, boolean skipProxyOperation, HexFunction<Integer, AttributionSource, Boolean, String, Boolean, Boolean, SyncNotedAppOp> superImpl) {
        return (SyncNotedAppOp) superImpl.apply(Integer.valueOf(resolveDatasourceOp(code, attributionSource.getUid(), attributionSource.getPackageName(), attributionSource.getAttributionTag())), attributionSource, Boolean.valueOf(shouldCollectAsyncNotedOp), message, Boolean.valueOf(shouldCollectMessage), Boolean.valueOf(skipProxyOperation));
    }

    public SyncNotedAppOp startOperation(IBinder token, int code, int uid, String packageName, String attributionTag, boolean startIfModeDefault, boolean shouldCollectAsyncNotedOp, String message, boolean shouldCollectMessage, int attributionFlags, int attributionChainId, UndecFunction<IBinder, Integer, Integer, String, String, Boolean, Boolean, String, Boolean, Integer, Integer, SyncNotedAppOp> superImpl) {
        return (SyncNotedAppOp) superImpl.apply(token, Integer.valueOf(resolveDatasourceOp(code, uid, packageName, attributionTag)), Integer.valueOf(resolveUid(code, uid)), packageName, attributionTag, Boolean.valueOf(startIfModeDefault), Boolean.valueOf(shouldCollectAsyncNotedOp), message, Boolean.valueOf(shouldCollectMessage), Integer.valueOf(attributionFlags), Integer.valueOf(attributionChainId));
    }

    public SyncNotedAppOp startProxyOperation(IBinder clientId, int code, AttributionSource attributionSource, boolean startIfModeDefault, boolean shouldCollectAsyncNotedOp, String message, boolean shouldCollectMessage, boolean skipProxyOperation, int proxyAttributionFlags, int proxiedAttributionFlags, int attributionChainId, UndecFunction<IBinder, Integer, AttributionSource, Boolean, Boolean, String, Boolean, Boolean, Integer, Integer, Integer, SyncNotedAppOp> superImpl) {
        return (SyncNotedAppOp) superImpl.apply(clientId, Integer.valueOf(resolveDatasourceOp(code, attributionSource.getUid(), attributionSource.getPackageName(), attributionSource.getAttributionTag())), attributionSource, Boolean.valueOf(startIfModeDefault), Boolean.valueOf(shouldCollectAsyncNotedOp), message, Boolean.valueOf(shouldCollectMessage), Boolean.valueOf(skipProxyOperation), Integer.valueOf(proxyAttributionFlags), Integer.valueOf(proxiedAttributionFlags), Integer.valueOf(attributionChainId));
    }

    public void finishOperation(IBinder clientId, int code, int uid, String packageName, String attributionTag, QuintConsumer<IBinder, Integer, Integer, String, String> superImpl) {
        superImpl.accept(clientId, Integer.valueOf(resolveDatasourceOp(code, uid, packageName, attributionTag)), Integer.valueOf(resolveUid(code, uid)), packageName, attributionTag);
    }

    public void finishProxyOperation(IBinder clientId, int code, AttributionSource attributionSource, boolean skipProxyOperation, QuadFunction<IBinder, Integer, AttributionSource, Boolean, Void> superImpl) {
        superImpl.apply(clientId, Integer.valueOf(resolveDatasourceOp(code, attributionSource.getUid(), attributionSource.getPackageName(), attributionSource.getAttributionTag())), attributionSource, Boolean.valueOf(skipProxyOperation));
    }

    public void dumpTags(PrintWriter writer) {
        if (!this.mLocationTags.isEmpty()) {
            writer.println("  AppOps policy location tags:");
            writeTags(this.mLocationTags, writer);
            writer.println();
        }
        if (!this.mActivityRecognitionTags.isEmpty()) {
            writer.println("  AppOps policy activity recognition tags:");
            writeTags(this.mActivityRecognitionTags, writer);
            writer.println();
        }
    }

    private void writeTags(Map<Integer, PackageTagsList> tags, PrintWriter writer) {
        int counter = 0;
        for (Map.Entry<Integer, PackageTagsList> tagEntry : tags.entrySet()) {
            writer.print("    #");
            writer.print(counter);
            writer.print(": ");
            writer.print(tagEntry.getKey().toString());
            writer.print("=");
            tagEntry.getValue().dump(writer);
            counter++;
        }
    }

    private int resolveDatasourceOp(int code, int uid, String packageName, String attributionTag) {
        int code2 = resolveRecordAudioOp(code, uid);
        if (attributionTag == null) {
            return code2;
        }
        int resolvedCode = resolveLocationOp(code2);
        if (resolvedCode != code2) {
            if (isDatasourceAttributionTag(uid, packageName, attributionTag, this.mLocationTags)) {
                return resolvedCode;
            }
        } else {
            int resolvedCode2 = resolveArOp(code2);
            if (resolvedCode2 != code2 && isDatasourceAttributionTag(uid, packageName, attributionTag, this.mActivityRecognitionTags)) {
                return resolvedCode2;
            }
        }
        return code2;
    }

    private void initializeActivityRecognizersTags() {
        List<String> activityRecognizers = this.mRoleManager.getRoleHolders("android.app.role.SYSTEM_ACTIVITY_RECOGNIZER");
        int recognizerCount = activityRecognizers.size();
        if (recognizerCount > 0) {
            for (int i = 0; i < recognizerCount; i++) {
                String activityRecognizer = activityRecognizers.get(i);
                updateActivityRecognizerTags(activityRecognizer);
            }
            return;
        }
        clearActivityRecognitionTags();
    }

    private void clearActivityRecognitionTags() {
        synchronized (this.mLock) {
            this.mActivityRecognitionTags.clear();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateActivityRecognizerTags(String activityRecognizer) {
        Intent intent = new Intent("android.intent.action.ACTIVITY_RECOGNIZER");
        intent.setPackage(activityRecognizer);
        ResolveInfo resolvedService = this.mContext.getPackageManager().resolveServiceAsUser(intent, 819332, 0);
        if (resolvedService == null || resolvedService.serviceInfo == null) {
            Log.w(LOG_TAG, "Service recognizer doesn't handle android.intent.action.ACTIVITY_RECOGNIZER, ignoring!");
            return;
        }
        Bundle metaData = resolvedService.serviceInfo.metaData;
        if (metaData == null) {
            return;
        }
        String tagsList = metaData.getString(ACTIVITY_RECOGNITION_TAGS);
        if (!TextUtils.isEmpty(tagsList)) {
            PackageTagsList packageTagsList = new PackageTagsList.Builder(1).add(resolvedService.serviceInfo.packageName, Arrays.asList(tagsList.split(ACTIVITY_RECOGNITION_TAGS_SEPARATOR))).build();
            synchronized (this.mLock) {
                updateAllowListedTagsForPackageLocked(UserHandle.getAppId(resolvedService.serviceInfo.applicationInfo.uid), packageTagsList, this.mActivityRecognitionTags);
            }
        }
    }

    private static void updateAllowListedTagsForPackageLocked(int appId, PackageTagsList packageTagsList, ConcurrentHashMap<Integer, PackageTagsList> datastore) {
        datastore.put(Integer.valueOf(appId), packageTagsList);
    }

    private static boolean isDatasourceAttributionTag(int uid, String packageName, String attributionTag, Map<Integer, PackageTagsList> mappedOps) {
        PackageTagsList appIdTags = mappedOps.get(Integer.valueOf(UserHandle.getAppId(uid)));
        return appIdTags != null && appIdTags.contains(packageName, attributionTag);
    }

    private static int resolveLocationOp(int code) {
        switch (code) {
            case 0:
                return 109;
            case 1:
                return 108;
            default:
                return code;
        }
    }

    private static int resolveArOp(int code) {
        if (code == 79) {
            return 113;
        }
        return code;
    }

    private int resolveRecordAudioOp(int code, int uid) {
        if (code == 102) {
            if (!this.mIsHotwordDetectionServiceRequired) {
                return code;
            }
            VoiceInteractionManagerInternal.HotwordDetectionServiceIdentity hotwordDetectionServiceIdentity = this.mVoiceInteractionManagerInternal.getHotwordDetectionServiceIdentity();
            if (hotwordDetectionServiceIdentity != null && uid == hotwordDetectionServiceIdentity.getIsolatedUid()) {
                return code;
            }
            return 27;
        }
        return code;
    }

    private int resolveUid(int code, int uid) {
        VoiceInteractionManagerInternal.HotwordDetectionServiceIdentity hotwordDetectionServiceIdentity;
        if (Process.isIsolated(uid)) {
            if ((code == 27 || code == 102) && (hotwordDetectionServiceIdentity = this.mVoiceInteractionManagerInternal.getHotwordDetectionServiceIdentity()) != null && uid == hotwordDetectionServiceIdentity.getIsolatedUid()) {
                return hotwordDetectionServiceIdentity.getOwnerUid();
            }
            return uid;
        }
        return uid;
    }
}
