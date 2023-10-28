package com.android.server.wm;

import android.app.ActivityManager;
import android.app.ActivityManagerInternal;
import android.app.ActivityOptions;
import android.app.AppGlobals;
import android.app.IApplicationThread;
import android.app.KeyguardManager;
import android.app.ProfilerInfo;
import android.app.ThunderbackConfig;
import android.app.WaitResult;
import android.content.ComponentName;
import android.content.Context;
import android.content.IIntentSender;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.AuxiliaryResolveInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManagerInternal;
import android.content.pm.ParceledListSlice;
import android.content.pm.ResolveInfo;
import android.content.pm.UserInfo;
import android.content.res.Configuration;
import android.hardware.fingerprint.Fingerprint;
import android.hardware.fingerprint.FingerprintManager;
import android.os.BaseBundle;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Process;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.os.Trace;
import android.os.UserHandle;
import android.os.UserManager;
import android.service.voice.IVoiceInteractionSession;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.DebugUtils;
import android.util.Pools;
import android.util.Slog;
import android.window.RemoteTransition;
import android.window.WindowContainerToken;
import com.android.internal.app.HeavyWeightSwitcherActivity;
import com.android.internal.app.IVoiceInteractor;
import com.android.internal.protolog.ProtoLogGroup;
import com.android.internal.protolog.ProtoLogImpl;
import com.android.internal.util.function.OctConsumer;
import com.android.internal.util.function.pooled.PooledLambda;
import com.android.server.am.HostingRecord;
import com.android.server.am.PendingIntentRecord;
import com.android.server.pm.InstantAppResolver;
import com.android.server.pm.PackageManagerService;
import com.android.server.power.ShutdownCheckPoints;
import com.android.server.statusbar.StatusBarManagerInternal;
import com.android.server.uri.NeededUriGrants;
import com.android.server.wm.ActivityMetricsLogger;
import com.android.server.wm.ActivityRecord;
import com.android.server.wm.IWindowManagerServiceLice;
import com.android.server.wm.LaunchParamsController;
import com.transsion.hubcore.multiwindow.ITranMultiWindow;
import com.transsion.hubcore.server.am.ITranActivityManagerService;
import com.transsion.hubcore.server.wm.ITranActivityStarter;
import com.transsion.hubcore.server.wm.ITranTaskLaunchParamsModifier;
import com.transsion.hubcore.server.wm.ITranWindowManagerService;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiConsumer;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class ActivityStarter {
    public static boolean AGARES_SUPPORT = false;
    private static final int DEFAULT_SHARE_VALUE = 0;
    static final long ENABLE_PENDING_INTENT_BAL_OPTION = 192341120;
    private static final String EXTRA_AUTH_CALL_DUALPROFILE = "android.intent.extra.auth_to_call_dualprofile";
    private static final String EXTRA_DUALPROFILE_CACHED_USERID = "android.intent.extra.dualprofile_cached_uid";
    private static final String EXTRA_DUALPROFILE_RESOLVE_INTENT_AGAIN = "android.intent.extra.dualprofile_resolve_intent_again";
    private static final String EXTRA_DUALPROFILE_USERID_SELECTED = "android.intent.extra.dualprofile_userid_selected";
    private static final int INVALID_LAUNCH_MODE = -1;
    private static final String PACKAGE_LINKER = "@";
    public static boolean SUPPORT = false;
    private static final int TRANSSION_SHARE_VALUE = 1;
    private static final ArrayList<String> sAddUserPackagesBlackList;
    private static final ArrayList<String> sCrossUserAimPackagesWhiteList;
    private static final ArrayList<String> sCrossUserCallingPackagesWhiteList;
    private static final ArrayList<String> sCrossUserDisableComponentActionWhiteList;
    private static final ArrayList<String> sDualprofilePackagesWhiteList;
    private static final String[] sMultiWindowInvaild;
    private static final ArrayList<String> sPublicActionList;
    boolean mAddingToTask;
    private TaskFragment mAddingToTaskFragment;
    private boolean mAvoidMoveToFront;
    private int mCallingUid;
    private final ActivityStartController mController;
    private boolean mDoResume;
    private boolean mFrozeTaskList;
    private Task mInTask;
    private TaskFragment mInTaskFragment;
    private Intent mIntent;
    private boolean mIntentDelivered;
    private final ActivityStartInterceptor mInterceptor;
    private boolean mIsAppLaunch;
    private boolean mIsTaskCleared;
    private ActivityRecord mLastStartActivityRecord;
    private int mLastStartActivityResult;
    private long mLastStartActivityTimeMs;
    private String mLastStartReason;
    private int mLaunchFlags;
    private int mLaunchMode;
    private boolean mLaunchTaskBehind;
    private boolean mMovedToFront;
    private ActivityInfo mNewTaskInfo;
    private Intent mNewTaskIntent;
    private boolean mNoAnimation;
    private ActivityRecord mNotTop;
    private ActivityOptions mOptions;
    private TaskDisplayArea mPreferredTaskDisplayArea;
    private int mPreferredWindowingMode;
    private Task mPriorAboveTask;
    private int mRecentIndex;
    private boolean mRestrictedBgActivity;
    private final RootWindowContainer mRootWindowContainer;
    private final ActivityTaskManagerService mService;
    private ActivityRecord mSourceRecord;
    private Task mSourceRootTask;
    ActivityRecord mStartActivity;
    private int mStartFlags;
    private final ActivityTaskSupervisor mSupervisor;
    private Task mTargetRootTask;
    private Task mTargetTask;
    private boolean mTransientLaunch;
    private IVoiceInteractor mVoiceInteractor;
    private IVoiceInteractionSession mVoiceSession;
    private static final String TAG = "ActivityTaskManager";
    private static final String TAG_RESULTS = TAG + ActivityTaskManagerDebugConfig.POSTFIX_RESULTS;
    private static final String TAG_FOCUS = TAG + ActivityTaskManagerDebugConfig.POSTFIX_FOCUS;
    private static final String TAG_CONFIGURATION = TAG + ActivityTaskManagerDebugConfig.POSTFIX_CONFIGURATION;
    private static final String TAG_USER_LEAVING = TAG + ActivityTaskManagerDebugConfig.POSTFIX_USER_LEAVING;
    private static final HashMap<String, Integer> sCachedCallingRelationSelfLocked = new HashMap<>();
    private static final ArrayList<String> sDualprofileInstalledPackagesSelfLocked = new ArrayList<>();
    private LaunchParamsController.LaunchParams mLaunchParams = new LaunchParamsController.LaunchParams();
    Request mRequest = new Request();

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public interface Factory {
        ActivityStarter obtain();

        void recycle(ActivityStarter activityStarter);

        void setController(ActivityStartController activityStartController);
    }

    static {
        ArrayList<String> arrayList = new ArrayList<>();
        sDualprofilePackagesWhiteList = arrayList;
        ArrayList<String> arrayList2 = new ArrayList<>();
        sCrossUserCallingPackagesWhiteList = arrayList2;
        ArrayList<String> arrayList3 = new ArrayList<>();
        sPublicActionList = arrayList3;
        ArrayList<String> arrayList4 = new ArrayList<>();
        sCrossUserAimPackagesWhiteList = arrayList4;
        ArrayList<String> arrayList5 = new ArrayList<>();
        sCrossUserDisableComponentActionWhiteList = arrayList5;
        ArrayList<String> arrayList6 = new ArrayList<>();
        sAddUserPackagesBlackList = arrayList6;
        arrayList.add("com.google.android.youtube");
        arrayList.add("com.google.android.gm");
        arrayList.add("com.trassion.infinix.xclub");
        arrayList.add("com.facebook.katana");
        arrayList.add("com.instagram.android");
        arrayList.add("com.facebook.orca");
        arrayList.add("com.snapchat.android");
        arrayList.add("com.whatsapp");
        arrayList.add("org.telegram.messenger");
        arrayList.add("com.viber.voip");
        arrayList.add("com.skype.raider");
        arrayList.add("com.vkontakte.android");
        arrayList.add("jp.naver.line.android");
        arrayList.add("com.imo.android.imoim");
        arrayList.add("com.whatsapp.w4b");
        arrayList.add("com.zebra.letschat");
        arrayList.add("in.mohalla.sharechat");
        arrayList.add("com.hatsune.eagleee");
        arrayList.add("com.facebook.lite");
        arrayList.add("com.tencent.mm");
        arrayList.add("com.tencent.mobileqq");
        arrayList.add("com.tencent.wework");
        arrayList.add("com.sina.weibo");
        arrayList.add("com.happyelements.AndroidAnimal");
        arrayList.add("com.eg.android.AlipayGphone");
        arrayList.add("com.tencent.karaoke");
        arrayList.add("im.thebot.messenger");
        arrayList.add("com.pinterest");
        arrayList.add("com.twitter.android");
        arrayList2.add(PackageManagerService.PLATFORM_PACKAGE_NAME);
        arrayList2.add("com.android.settings");
        arrayList2.add("com.android.systemui");
        arrayList2.add("com.android.launcher3");
        arrayList2.add("com.transsion.hilauncher");
        arrayList2.add("com.transsion.XOSLauncher");
        arrayList2.add("com.transsion.itel.launcher");
        arrayList2.add("com.transsion.hilauncher.upgrade");
        arrayList2.add("com.transsion.XOSLauncher.upgrade");
        arrayList2.add("com.transsion.smartpanel");
        arrayList2.add("com.transsion.resolver");
        arrayList3.add("android.intent.action.SEND");
        arrayList3.add("android.intent.action.SEND_MULTIPLE");
        arrayList3.add("android.intent.action.SENDTO");
        arrayList4.add(PackageManagerService.PLATFORM_PACKAGE_NAME);
        arrayList4.add("com.android.keychain");
        arrayList4.add("com.google.android.webview");
        arrayList4.add("com.google.android.packageinstaller");
        arrayList4.add("com.android.permissioncontroller");
        arrayList4.add("com.google.android.permissioncontroller");
        arrayList4.add("com.android.permissioncontroller");
        arrayList4.add("com.android.packageinstaller");
        arrayList4.add("com.tencent.soter.soterserver");
        arrayList4.add("com.eg.android.AlipayGphone");
        arrayList4.add("com.google.android.gsf");
        arrayList4.add("com.google.android.gsf.login");
        arrayList4.add("com.google.android.gms");
        arrayList4.add("com.google.android.play.games");
        arrayList4.add("com.android.settings");
        arrayList4.add("com.android.systemui");
        arrayList4.add("com.android.soundpicker");
        arrayList4.add("com.transsion.resolver");
        arrayList5.add("android.nfc.action.TECH_DISCOVERED");
        arrayList6.add("com.android.contacts");
        boolean z = false;
        boolean z2 = 1 == SystemProperties.getInt("ro.transsion.appturbo.support", 0);
        SUPPORT = z2;
        if (z2 && 1 == SystemProperties.getInt("ro.transsion.appturbo.agares.support", 0)) {
            z = true;
        }
        AGARES_SUPPORT = z;
        sMultiWindowInvaild = new String[]{"com.transsion.childmode", "com.transsion.iotservice", "com.transsion.mol"};
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public static class DefaultFactory implements Factory {
        private ActivityStartController mController;
        private ActivityStartInterceptor mInterceptor;
        private ActivityTaskManagerService mService;
        private ActivityTaskSupervisor mSupervisor;
        private final int MAX_STARTER_COUNT = 3;
        private Pools.SynchronizedPool<ActivityStarter> mStarterPool = new Pools.SynchronizedPool<>(3);

        /* JADX INFO: Access modifiers changed from: package-private */
        public DefaultFactory(ActivityTaskManagerService service, ActivityTaskSupervisor supervisor, ActivityStartInterceptor interceptor) {
            this.mService = service;
            this.mSupervisor = supervisor;
            this.mInterceptor = interceptor;
        }

        @Override // com.android.server.wm.ActivityStarter.Factory
        public void setController(ActivityStartController controller) {
            this.mController = controller;
        }

        @Override // com.android.server.wm.ActivityStarter.Factory
        public ActivityStarter obtain() {
            ActivityStarter starter = (ActivityStarter) this.mStarterPool.acquire();
            if (starter == null) {
                if (this.mService.mRootWindowContainer == null) {
                    throw new IllegalStateException("Too early to start activity.");
                }
                return new ActivityStarter(this.mController, this.mService, this.mSupervisor, this.mInterceptor);
            }
            return starter;
        }

        @Override // com.android.server.wm.ActivityStarter.Factory
        public void recycle(ActivityStarter starter) {
            starter.reset(true);
            this.mStarterPool.release(starter);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public static class Request {
        private static final int DEFAULT_CALLING_PID = 0;
        private static final int DEFAULT_CALLING_UID = -1;
        static final int DEFAULT_REAL_CALLING_PID = 0;
        static final int DEFAULT_REAL_CALLING_UID = -1;
        ActivityInfo activityInfo;
        SafeActivityOptions activityOptions;
        boolean allowBackgroundActivityStart;
        boolean allowPendingRemoteAnimationRegistryLookup;
        boolean avoidMoveToFront;
        Bundle bOptions;
        IApplicationThread caller;
        String callingFeatureId;
        String callingPackage;
        boolean componentSpecified;
        Intent ephemeralIntent;
        IBinder errorCallbackToken;
        int filterCallingUid;
        Configuration globalConfig;
        boolean ignoreTargetSecurity;
        Task inTask;
        TaskFragment inTaskFragment;
        Intent intent;
        NeededUriGrants intentGrants;
        PendingIntentRecord originatingPendingIntent;
        ActivityRecord[] outActivity;
        ProfilerInfo profilerInfo;
        String reason;
        int requestCode;
        ResolveInfo resolveInfo;
        String resolvedType;
        IBinder resultTo;
        String resultWho;
        int startFlags;
        int userId;
        IVoiceInteractor voiceInteractor;
        IVoiceInteractionSession voiceSession;
        WaitResult waitResult;
        int callingPid = 0;
        int callingUid = -1;
        int realCallingPid = 0;
        int realCallingUid = -1;

        Request() {
            reset();
        }

        void reset() {
            this.caller = null;
            this.intent = null;
            this.intentGrants = null;
            this.ephemeralIntent = null;
            this.resolvedType = null;
            this.activityInfo = null;
            this.resolveInfo = null;
            this.voiceSession = null;
            this.voiceInteractor = null;
            this.resultTo = null;
            this.resultWho = null;
            this.requestCode = 0;
            this.callingPid = 0;
            this.callingUid = -1;
            this.callingPackage = null;
            this.callingFeatureId = null;
            this.realCallingPid = 0;
            this.realCallingUid = -1;
            this.startFlags = 0;
            this.activityOptions = null;
            this.bOptions = null;
            this.ignoreTargetSecurity = false;
            this.componentSpecified = false;
            this.outActivity = null;
            this.inTask = null;
            this.inTaskFragment = null;
            this.reason = null;
            this.profilerInfo = null;
            this.globalConfig = null;
            this.userId = 0;
            this.waitResult = null;
            this.avoidMoveToFront = false;
            this.allowPendingRemoteAnimationRegistryLookup = true;
            this.filterCallingUid = -10000;
            this.originatingPendingIntent = null;
            this.allowBackgroundActivityStart = false;
            this.errorCallbackToken = null;
        }

        void set(Request request) {
            this.caller = request.caller;
            this.intent = request.intent;
            this.intentGrants = request.intentGrants;
            this.ephemeralIntent = request.ephemeralIntent;
            this.resolvedType = request.resolvedType;
            this.activityInfo = request.activityInfo;
            this.resolveInfo = request.resolveInfo;
            this.voiceSession = request.voiceSession;
            this.voiceInteractor = request.voiceInteractor;
            this.resultTo = request.resultTo;
            this.resultWho = request.resultWho;
            this.requestCode = request.requestCode;
            this.callingPid = request.callingPid;
            this.callingUid = request.callingUid;
            this.callingPackage = request.callingPackage;
            this.callingFeatureId = request.callingFeatureId;
            this.realCallingPid = request.realCallingPid;
            this.realCallingUid = request.realCallingUid;
            this.startFlags = request.startFlags;
            this.activityOptions = request.activityOptions;
            this.bOptions = request.bOptions;
            this.ignoreTargetSecurity = request.ignoreTargetSecurity;
            this.componentSpecified = request.componentSpecified;
            this.outActivity = request.outActivity;
            this.inTask = request.inTask;
            this.inTaskFragment = request.inTaskFragment;
            this.reason = request.reason;
            this.profilerInfo = request.profilerInfo;
            this.globalConfig = request.globalConfig;
            this.userId = request.userId;
            this.waitResult = request.waitResult;
            this.avoidMoveToFront = request.avoidMoveToFront;
            this.allowPendingRemoteAnimationRegistryLookup = request.allowPendingRemoteAnimationRegistryLookup;
            this.filterCallingUid = request.filterCallingUid;
            this.originatingPendingIntent = request.originatingPendingIntent;
            this.allowBackgroundActivityStart = request.allowBackgroundActivityStart;
            this.errorCallbackToken = request.errorCallbackToken;
        }

        private boolean isPublicIntent(Intent intent, String callingPackage) {
            if (TextUtils.isEmpty(callingPackage) || TextUtils.equals(getAimPkg(intent), callingPackage)) {
                return false;
            }
            Intent newIntent = new Intent(intent);
            try {
                BaseBundle.setShouldDefuse(false);
                newIntent.hasExtra("");
                return true;
            } catch (Throwable e) {
                try {
                    Slog.w(ActivityStarter.TAG, "Private intent", e);
                    return false;
                } finally {
                    BaseBundle.setShouldDefuse(true);
                }
            }
        }

        private String getAimPkg(Intent intent) {
            ComponentName componentName;
            String aimPkg = intent.getPackage();
            if (aimPkg != null || (componentName = intent.getComponent()) == null) {
                return aimPkg;
            }
            return componentName.getPackageName();
        }

        private boolean checkCallDualprofilePermission(String callingPkg) {
            try {
                ApplicationInfo appInfo = AppGlobals.getPackageManager().getApplicationInfo(callingPkg, 0L, 0);
                if (!ActivityStarter.isSystemApp(appInfo)) {
                    return false;
                }
                return true;
            } catch (RemoteException e) {
                Slog.e(ActivityStarter.TAG, "Failed to read package info of: " + callingPkg, e);
                return false;
            }
        }

        private Intent getResolverActivity(Intent intent, String aimPkg, int requestCode, Request request) {
            Intent resolverActivityIntent = new Intent("tanssion.intent.action.ACTION_DUALPROFILE_RESOLVER_ACTIVITY");
            if (requestCode >= 0) {
                intent.addFlags(33554432);
            }
            intent.putExtra(ActivityStarter.EXTRA_DUALPROFILE_USERID_SELECTED, true);
            resolverActivityIntent.putExtra("android.intent.extra.dualprofile_resolver_activity_original_intent", intent);
            resolverActivityIntent.putExtra("android.intent.extra.dualprofile_resolver_activity_aim_package", aimPkg);
            resolverActivityIntent.putExtra("android.intent.extra.dualprofile_resolver_activity_options", request != null ? request.bOptions : null);
            int shareJump = SystemProperties.getInt("ro.os_resolver_v2_support", 0);
            if (shareJump == 1) {
                resolverActivityIntent.setClassName("com.transsion.resolver", "com.transsion.resolver.ResolverActivity");
            } else {
                resolverActivityIntent.setClassName(PackageManagerService.PLATFORM_PACKAGE_NAME, "com.android.internal.app.ResolverActivity");
            }
            resolverActivityIntent.putExtra(ActivityStarter.EXTRA_DUALPROFILE_RESOLVE_INTENT_AGAIN, true);
            return resolverActivityIntent;
        }

        private boolean shouldResolveAgain(Intent intent, String callingPackage) {
            String aimPkg = getAimPkg(intent);
            Intent newIntent = new Intent(intent);
            try {
                if (!TextUtils.equals(aimPkg, callingPackage) && newIntent.hasExtra(ActivityStarter.EXTRA_DUALPROFILE_RESOLVE_INTENT_AGAIN)) {
                    intent.removeExtra(ActivityStarter.EXTRA_DUALPROFILE_RESOLVE_INTENT_AGAIN);
                    return true;
                }
                return false;
            } catch (Exception e) {
                Slog.w(ActivityStarter.TAG, "Private intent: ", e);
                return false;
            }
        }

        private int getCachedUserId(Intent intent, String callingPackage) {
            if (TextUtils.equals(getAimPkg(intent), callingPackage) || !intent.hasExtra(ActivityStarter.EXTRA_DUALPROFILE_CACHED_USERID)) {
                return -10000;
            }
            int cachedUserId = intent.getIntExtra(ActivityStarter.EXTRA_DUALPROFILE_CACHED_USERID, -10000);
            intent.removeExtra(ActivityStarter.EXTRA_DUALPROFILE_CACHED_USERID);
            return cachedUserId;
        }

        private boolean isScreenLocked(Context context) {
            return ((KeyguardManager) context.getSystemService("keyguard")).inKeyguardRestrictedInputMode();
        }

        private void putCachedCallingRelation(String aimPkg, String callingPackage) {
            synchronized (ActivityStarter.sCachedCallingRelationSelfLocked) {
                String callingRelationKey = aimPkg + ActivityStarter.PACKAGE_LINKER + callingPackage;
                int cachedUserId = UserHandle.getCallingUserId();
                ActivityStarter.sCachedCallingRelationSelfLocked.put(callingRelationKey, Integer.valueOf(cachedUserId));
                Slog.w(ActivityStarter.TAG, "putCachedCallingRelation, callingRelationKey:" + callingRelationKey + ", cachedUserId:" + cachedUserId);
            }
        }

        private int getToUserIdFromCachedCallingRelation(String aimPkg, String callingPackage) {
            int cachedToUserId = -10000;
            synchronized (ActivityStarter.sCachedCallingRelationSelfLocked) {
                String callingRelationKey = callingPackage + ActivityStarter.PACKAGE_LINKER + aimPkg;
                Integer toUserIdObj = (Integer) ActivityStarter.sCachedCallingRelationSelfLocked.get(callingRelationKey);
                if (toUserIdObj != null) {
                    cachedToUserId = toUserIdObj.intValue();
                    ActivityStarter.sCachedCallingRelationSelfLocked.remove(callingRelationKey);
                    Slog.w(ActivityStarter.TAG, "got callingRelationKey :" + callingRelationKey + ", cachedToUserId:" + cachedToUserId);
                }
            }
            return cachedToUserId;
        }

        private boolean isComponentEnabled(ActivityInfo info, int userId) {
            if (info == null) {
                return true;
            }
            boolean enabled = false;
            ComponentName compname = new ComponentName(info.packageName, info.name);
            long token = Binder.clearCallingIdentity();
            try {
                if (AppGlobals.getPackageManager().getActivityInfo(compname, 0L, userId) != null) {
                    enabled = true;
                }
            } catch (RemoteException e) {
                enabled = false;
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(token);
                throw th;
            }
            Binder.restoreCallingIdentity(token);
            if (!enabled) {
                Slog.d(ActivityStarter.TAG, "Component not enabled: " + compname + "  in user " + userId);
            }
            return enabled;
        }

        private Intent checkDualprofileControl(Context context, ActivityInfo aInfo, Intent intent, int requestCode, int userId, String callingPackage, ActivityTaskSupervisor supervisor) {
            if (!isPublicIntent(intent, callingPackage)) {
                return intent;
            }
            String aimPkg = getAimPkg(intent);
            String action = intent.getAction();
            int callingUserId = Binder.getCallingUserHandle().getIdentifier();
            UserInfo userInfo = supervisor.getUserInfo(callingUserId);
            if (ActivityTaskManagerDebugConfig.DEBUG_ACTIVITY_STARTS) {
                Slog.d(ActivityStarter.TAG, "checkDualprofileControl, from:" + callingPackage + ", to:" + aimPkg + ", with act:" + action + ", callingUserId:" + callingUserId + ", toUserId:" + userId + ", aInfo:" + aInfo);
            }
            if (userInfo == null) {
                return intent;
            }
            if (callingUserId != 0 && !userInfo.isDualProfile()) {
                return intent;
            }
            if ((callingUserId == 0 && userId == 0 && (!ActivityStarter.sDualprofileInstalledPackagesSelfLocked.contains(aimPkg) || !isComponentEnabled(aInfo, 999))) || isScreenLocked(context)) {
                return intent;
            }
            if ((ActivityStarter.sCrossUserCallingPackagesWhiteList.contains(callingPackage) && !ActivityStarter.sPublicActionList.contains(action)) || ActivityStarter.sCrossUserAimPackagesWhiteList.contains(aimPkg)) {
                return intent;
            }
            if (ActivityStarter.sCrossUserDisableComponentActionWhiteList.contains(action) && !isComponentEnabled(aInfo, 999)) {
                return intent;
            }
            if (intent.hasExtra(ActivityStarter.EXTRA_DUALPROFILE_USERID_SELECTED)) {
                if (ActivityTaskManagerDebugConfig.DEBUG_ACTIVITY_STARTS) {
                    Slog.d(ActivityStarter.TAG, "from dualprofile ResolverActivity");
                }
                intent.removeExtra(ActivityStarter.EXTRA_DUALPROFILE_RESOLVE_INTENT_AGAIN);
                intent.prepareToLeaveUser(callingUserId);
                putCachedCallingRelation(aimPkg, callingPackage);
                return intent;
            }
            int cachedToUserId = getToUserIdFromCachedCallingRelation(aimPkg, callingPackage);
            if (cachedToUserId != -10000) {
                if (ActivityTaskManagerDebugConfig.DEBUG_ACTIVITY_STARTS) {
                    Slog.d(ActivityStarter.TAG, "using cached calling relation");
                }
                intent.putExtra(ActivityStarter.EXTRA_DUALPROFILE_CACHED_USERID, cachedToUserId);
            } else {
                synchronized (ActivityStarter.sDualprofileInstalledPackagesSelfLocked) {
                    if (ActivityStarter.sDualprofileInstalledPackagesSelfLocked.contains(aimPkg)) {
                        if (intent.hasExtra(ActivityStarter.EXTRA_AUTH_CALL_DUALPROFILE) && checkCallDualprofilePermission(callingPackage)) {
                            if (ActivityTaskManagerDebugConfig.DEBUG_ACTIVITY_STARTS) {
                                Slog.d(ActivityStarter.TAG, "call dualprofile directly");
                            }
                            return intent;
                        }
                        if (ActivityTaskManagerDebugConfig.DEBUG_ACTIVITY_STARTS) {
                            Slog.d(ActivityStarter.TAG, "pop up ResolverActivity");
                        }
                        intent = getResolverActivity(intent, aimPkg, requestCode, this);
                    } else if (ActivityStarter.sDualprofileInstalledPackagesSelfLocked.contains(callingPackage)) {
                        if (ActivityTaskManagerDebugConfig.DEBUG_ACTIVITY_STARTS) {
                            Slog.d(ActivityStarter.TAG, "dualprofile installed App to normal App");
                        }
                        if (!ActivityStarter.sAddUserPackagesBlackList.contains(aimPkg)) {
                            intent.prepareToLeaveUser(callingUserId);
                        }
                        intent.putExtra(ActivityStarter.EXTRA_DUALPROFILE_CACHED_USERID, 0);
                        putCachedCallingRelation(aimPkg, callingPackage);
                    }
                }
            }
            return intent;
        }

        /* JADX WARN: Removed duplicated region for block: B:33:0x0072 A[RETURN] */
        /* JADX WARN: Removed duplicated region for block: B:34:0x0073  */
        /*
            Code decompiled incorrectly, please refer to instructions dump.
        */
        private ActivityInfo resolveDualprofileIntent(ActivityInfo ai, Intent intent, ProfilerInfo profilerInfo, String resolvedType, int startFlags, int userId, String callingPackage, ActivityTaskSupervisor supervisor) {
            ActivityInfo aiRet;
            int cachedUserId;
            UserInfo userInfo = supervisor.getUserInfo(userId);
            if (userInfo == null) {
                return ai;
            }
            if (userId == 0 && !isPublicIntent(intent, callingPackage)) {
                return ai;
            }
            if (userInfo.isDualProfile() && !isPublicIntent(intent, callingPackage)) {
                return ai;
            }
            if (userId != 0 && !userInfo.isDualProfile()) {
                return ai;
            }
            if (shouldResolveAgain(intent, callingPackage)) {
                IPackageManager ipm = AppGlobals.getPackageManager();
                long token = Binder.clearCallingIdentity();
                try {
                    ResolveInfo ri = ipm.resolveIntent(intent, resolvedType, 66560L, userId);
                    ActivityInfo aiRet2 = ri.activityInfo;
                    Binder.restoreCallingIdentity(token);
                    aiRet = aiRet2;
                } catch (Exception e) {
                    Slog.w(ActivityStarter.TAG, "ipm resolveIntent failed", e);
                } finally {
                }
                cachedUserId = getCachedUserId(intent, callingPackage);
                if (cachedUserId != -10000) {
                    return aiRet;
                }
                int filterCallingUid = Binder.getCallingUid();
                token = Binder.clearCallingIdentity();
                try {
                    ActivityInfo aiRet3 = supervisor.resolveActivity(intent, resolvedType, startFlags, profilerInfo, cachedUserId, filterCallingUid);
                    return aiRet3;
                } finally {
                }
            }
            aiRet = ai;
            cachedUserId = getCachedUserId(intent, callingPackage);
            if (cachedUserId != -10000) {
            }
        }

        /* JADX WARN: Removed duplicated region for block: B:59:0x0114  */
        /*
            Code decompiled incorrectly, please refer to instructions dump.
        */
        void resolveActivity(ActivityTaskSupervisor supervisor) {
            int resolvedCallingUid;
            UserInfo userInfo;
            boolean z;
            boolean profileLockedAndParentUnlockingOrUnlocked;
            if (this.realCallingPid == 0) {
                this.realCallingPid = Binder.getCallingPid();
            }
            if (this.realCallingUid == -1) {
                this.realCallingUid = Binder.getCallingUid();
            }
            if (this.callingUid >= 0) {
                this.callingPid = -1;
            } else if (this.caller == null) {
                this.callingPid = this.realCallingPid;
                this.callingUid = this.realCallingUid;
            } else {
                this.callingUid = -1;
                this.callingPid = -1;
            }
            int resolvedCallingUid2 = this.callingUid;
            if (this.caller == null) {
                resolvedCallingUid = resolvedCallingUid2;
            } else {
                synchronized (supervisor.mService.mGlobalLock) {
                    try {
                        WindowManagerService.boostPriorityForLockedSection();
                        WindowProcessController callerApp = supervisor.mService.getProcessController(this.caller);
                        if (callerApp != null) {
                            resolvedCallingUid2 = callerApp.mInfo.uid;
                        }
                    } catch (Throwable th) {
                        WindowManagerService.resetPriorityAfterLockedSection();
                        throw th;
                    }
                }
                WindowManagerService.resetPriorityAfterLockedSection();
                resolvedCallingUid = resolvedCallingUid2;
            }
            this.ephemeralIntent = new Intent(this.intent);
            Intent intent = new Intent(this.intent);
            this.intent = intent;
            if (intent.getComponent() != null && ((!"android.intent.action.VIEW".equals(this.intent.getAction()) || this.intent.getData() != null) && !"android.intent.action.INSTALL_INSTANT_APP_PACKAGE".equals(this.intent.getAction()) && !"android.intent.action.RESOLVE_INSTANT_APP_PACKAGE".equals(this.intent.getAction()) && supervisor.mService.getPackageManagerInternalLocked().isInstantAppInstallerComponent(this.intent.getComponent()))) {
                this.intent.setComponent(null);
            }
            ResolveInfo resolveIntent = supervisor.resolveIntent(this.intent, this.resolvedType, this.userId, 0, ActivityStarter.computeResolveFilterUid(this.callingUid, this.realCallingUid, this.filterCallingUid));
            this.resolveInfo = resolveIntent;
            if (resolveIntent == null && (userInfo = supervisor.getUserInfo(this.userId)) != null && userInfo.isManagedProfile()) {
                UserManager userManager = UserManager.get(supervisor.mService.mContext);
                long token = Binder.clearCallingIdentity();
                try {
                    UserInfo parent = userManager.getProfileParent(this.userId);
                    if (parent != null && userManager.isUserUnlockingOrUnlocked(parent.id)) {
                        if (!userManager.isUserUnlockingOrUnlocked(this.userId)) {
                            z = true;
                            profileLockedAndParentUnlockingOrUnlocked = z;
                            if (profileLockedAndParentUnlockingOrUnlocked) {
                                this.resolveInfo = supervisor.resolveIntent(this.intent, this.resolvedType, this.userId, 786432, ActivityStarter.computeResolveFilterUid(this.callingUid, this.realCallingUid, this.filterCallingUid));
                            }
                        }
                    }
                    z = false;
                    profileLockedAndParentUnlockingOrUnlocked = z;
                    if (profileLockedAndParentUnlockingOrUnlocked) {
                    }
                } finally {
                    Binder.restoreCallingIdentity(token);
                }
            }
            this.activityInfo = supervisor.resolveActivity(this.intent, this.resolveInfo, this.startFlags, this.profilerInfo);
            Intent checkDualprofileControl = checkDualprofileControl(supervisor.mService.mContext, this.activityInfo, this.intent, this.requestCode, this.userId, this.callingPackage, supervisor);
            this.intent = checkDualprofileControl;
            ActivityInfo resolveDualprofileIntent = resolveDualprofileIntent(this.activityInfo, checkDualprofileControl, this.profilerInfo, this.resolvedType, this.startFlags, this.userId, this.callingPackage, supervisor);
            this.activityInfo = resolveDualprofileIntent;
            if (resolveDualprofileIntent != null) {
                this.intentGrants = supervisor.mService.mUgmInternal.checkGrantUriPermissionFromIntent(this.intent, resolvedCallingUid, this.activityInfo.applicationInfo.packageName, UserHandle.getUserId(this.activityInfo.applicationInfo.uid));
            }
        }
    }

    ActivityStarter(ActivityStartController controller, ActivityTaskManagerService service, ActivityTaskSupervisor supervisor, ActivityStartInterceptor interceptor) {
        this.mController = controller;
        this.mService = service;
        this.mRootWindowContainer = service.mRootWindowContainer;
        this.mSupervisor = supervisor;
        this.mInterceptor = interceptor;
        reset(true);
        ITranActivityStarter.Instance().onActivityStarterConstruct(service.mContext, this.mRequest.userId, service, supervisor);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void set(ActivityStarter starter) {
        this.mStartActivity = starter.mStartActivity;
        this.mIntent = starter.mIntent;
        this.mCallingUid = starter.mCallingUid;
        this.mOptions = starter.mOptions;
        this.mRestrictedBgActivity = starter.mRestrictedBgActivity;
        this.mLaunchTaskBehind = starter.mLaunchTaskBehind;
        this.mLaunchFlags = starter.mLaunchFlags;
        this.mLaunchMode = starter.mLaunchMode;
        this.mLaunchParams.set(starter.mLaunchParams);
        this.mNotTop = starter.mNotTop;
        this.mDoResume = starter.mDoResume;
        this.mStartFlags = starter.mStartFlags;
        this.mSourceRecord = starter.mSourceRecord;
        this.mPreferredTaskDisplayArea = starter.mPreferredTaskDisplayArea;
        this.mPreferredWindowingMode = starter.mPreferredWindowingMode;
        this.mInTask = starter.mInTask;
        this.mInTaskFragment = starter.mInTaskFragment;
        this.mAddingToTask = starter.mAddingToTask;
        this.mNewTaskInfo = starter.mNewTaskInfo;
        this.mNewTaskIntent = starter.mNewTaskIntent;
        this.mSourceRootTask = starter.mSourceRootTask;
        this.mTargetTask = starter.mTargetTask;
        this.mTargetRootTask = starter.mTargetRootTask;
        this.mIsTaskCleared = starter.mIsTaskCleared;
        this.mMovedToFront = starter.mMovedToFront;
        this.mNoAnimation = starter.mNoAnimation;
        this.mAvoidMoveToFront = starter.mAvoidMoveToFront;
        this.mFrozeTaskList = starter.mFrozeTaskList;
        this.mVoiceSession = starter.mVoiceSession;
        this.mVoiceInteractor = starter.mVoiceInteractor;
        this.mIntentDelivered = starter.mIntentDelivered;
        this.mLastStartActivityResult = starter.mLastStartActivityResult;
        this.mLastStartActivityTimeMs = starter.mLastStartActivityTimeMs;
        this.mLastStartReason = starter.mLastStartReason;
        this.mRequest.set(starter.mRequest);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean relatedToPackage(String packageName) {
        ActivityRecord activityRecord;
        ActivityRecord activityRecord2 = this.mLastStartActivityRecord;
        return (activityRecord2 != null && packageName.equals(activityRecord2.packageName)) || ((activityRecord = this.mStartActivity) != null && packageName.equals(activityRecord.packageName));
    }

    /* JADX WARN: Unsupported multi-entry loop pattern (BACK_EDGE: B:46:0x00e5 -> B:47:0x00e6). Please submit an issue!!! */
    private void computeMultiWindowId(Request request) {
        ActivityOptions tempActivityOptions;
        boolean inVaild;
        int multiId;
        if (request == null || request.activityOptions == null || this.mSupervisor == null || (tempActivityOptions = this.mRequest.activityOptions.getOptions(this.mSupervisor)) == null) {
            return;
        }
        String packageName = null;
        if (this.mRequest.intent != null && this.mRequest.intent.getComponent() != null) {
            packageName = this.mRequest.intent.getComponent().getPackageName();
        }
        String packageName2 = packageName != null ? packageName : "com.android.settings";
        boolean isThunderBackSupport = tempActivityOptions.getThunderBackSupport();
        Slog.i(TAG, "floatWindow : tempActivityOptions.getThunderBackSupport() = " + isThunderBackSupport);
        String[] strArr = sMultiWindowInvaild;
        int length = strArr.length;
        int i = 0;
        while (true) {
            if (i >= length) {
                inVaild = false;
                break;
            }
            String requestList = strArr[i];
            if (!requestList.equals(packageName2)) {
                i++;
            } else {
                inVaild = true;
                break;
            }
        }
        if (isThunderBackSupport && inVaild) {
            ITranTaskLaunchParamsModifier.Instance().showUnSupportMultiToast();
        } else if (isThunderBackSupport && !inVaild) {
            synchronized (this.mService.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    TaskDisplayArea multiArea = this.mRootWindowContainer.getPreferMultiDisplayArea(packageName2);
                    Slog.i(TAG, "This package " + packageName2 + " activity int multidisplayarea: " + multiArea);
                    if (multiArea == null) {
                        multiId = 0;
                    } else {
                        int multiId2 = multiArea.getMultiWindowingId();
                        multiId = multiId2;
                    }
                    try {
                        WindowManagerService.resetPriorityAfterLockedSection();
                        if (multiId == 0) {
                            multiId = ITranActivityStarter.Instance().hookGetOrCreateMultiWindow(packageName2, 0, 10, -1, true);
                        } else {
                            ITranActivityStarter.Instance().hookGetOrCreateMultiWindow(packageName2, 0, 10, multiId, false);
                        }
                        ITranActivityStarter.Instance().setMultiWindowId(multiId);
                    } catch (Throwable th) {
                        th = th;
                        WindowManagerService.resetPriorityAfterLockedSection();
                        throw th;
                    }
                } catch (Throwable th2) {
                    th = th2;
                }
            }
        }
    }

    private void computeOptions(ActivityOptions checkedOptions) {
        TaskDisplayArea perfefDispalayArea;
        int multiWindowId = ITranActivityStarter.Instance().getMultiWindowId();
        Slog.i(TAG, "floatWindow: multiWindowId = " + multiWindowId);
        if (multiWindowId != 0 && (perfefDispalayArea = this.mRootWindowContainer.getPreferMultiDisplayArea(multiWindowId)) != null) {
            WindowContainerToken token = perfefDispalayArea.mRemoteToken.toWindowContainerToken();
            checkedOptions.setLaunchTaskDisplayArea(token);
        }
        ITranActivityStarter.Instance().setMultiWindowId(0);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static boolean isSystemApp(ApplicationInfo appInfo) {
        return (appInfo.flags & 1) > 0 || appInfo.uid <= 1000;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void onPackageCallback(String packageName, UserHandle user, String action) {
        Slog.w(TAG, "update dualprofiel: packageName:" + packageName + ", user:" + user + ", action:" + action);
        int userId = user.getIdentifier();
        if (userId != 999) {
            return;
        }
        if ("android.intent.action.PACKAGE_ADDED".equals(action)) {
            try {
                ApplicationInfo appInfo = AppGlobals.getPackageManager().getApplicationInfo(packageName, 0L, userId);
                if (appInfo != null) {
                    boolean isSystemApp = !sDualprofilePackagesWhiteList.contains(packageName) && isSystemApp(appInfo);
                    if (!isSystemApp && !"com.google.android.gms".equals(packageName) && !"com.google.android.gsf".equals(packageName)) {
                        ArrayList<String> arrayList = sDualprofileInstalledPackagesSelfLocked;
                        synchronized (arrayList) {
                            arrayList.add(packageName);
                        }
                    }
                    Slog.d(TAG, "dualprofile ignore system or GMS package: " + packageName);
                    return;
                }
            } catch (RemoteException e) {
                Slog.d(TAG, "PMS died", e);
            }
        } else if ("android.intent.action.PACKAGE_REMOVED".equals(action)) {
            ArrayList<String> arrayList2 = sDualprofileInstalledPackagesSelfLocked;
            synchronized (arrayList2) {
                arrayList2.remove(packageName);
            }
        }
        Slog.d(TAG, "onPackageCallback sDualprofileInstalledPackagesSelfLocked =" + sDualprofileInstalledPackagesSelfLocked.toString());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void initDualprofileAppList(Context context) {
        ParceledListSlice<PackageInfo> slice = null;
        try {
            slice = AppGlobals.getPackageManager().getInstalledPackages(0L, 999);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        if (slice != null) {
            List<PackageInfo> appList = slice.getList();
            synchronized (sDualprofileInstalledPackagesSelfLocked) {
                for (PackageInfo pkgInfo : appList) {
                    boolean isSystemApp = !sDualprofilePackagesWhiteList.contains(pkgInfo.packageName) && isSystemApp(pkgInfo.applicationInfo);
                    if (!isSystemApp && !"com.google.android.gms".equals(pkgInfo.packageName) && !"com.google.android.gsf".equals(pkgInfo.packageName)) {
                        sDualprofileInstalledPackagesSelfLocked.add(pkgInfo.packageName);
                    }
                }
            }
        }
        Slog.d(TAG, "initDualprofileAppList " + sDualprofileInstalledPackagesSelfLocked.toString());
        checkFingerprintStart(context);
    }

    private static void checkFingerprintStart(Context context) {
        Slog.i(TAG, "Check fingerprint");
        if (isFpStartSupport()) {
            return;
        }
        UserManager userManager = UserManager.get(context);
        if (userManager.isDualProfile(999)) {
            FingerprintManager fingerprintManager = (FingerprintManager) context.getSystemService("fingerprint");
            List<Fingerprint> items = fingerprintManager.getEnrolledFingerprints(0);
            for (int j = 0; j < items.size(); j++) {
                int fpId = items.get(j).getBiometricId();
                String appPkgname = fingerprintManager.getAppPackagename(fpId);
                int userId = fingerprintManager.getAppUserId(fpId);
                if (userId == 999 && appPkgname != null) {
                    try {
                        try {
                            AppGlobals.getPackageManager().setApplicationEnabledSetting(appPkgname, 1, 1, 999, "com.xui.xhide");
                        } catch (RemoteException e) {
                            e = e;
                            e.printStackTrace();
                            fingerprintManager.setAppAndUserIdForBiometrics(fpId, "", 0);
                        }
                    } catch (RemoteException e2) {
                        e = e2;
                    }
                    fingerprintManager.setAppAndUserIdForBiometrics(fpId, "", 0);
                }
            }
        }
        Slog.i(TAG, "Check fingerprint finish");
    }

    private static boolean isFpStartSupport() {
        return SystemProperties.getInt("ro.os_dualapp_fp_start_support", 0) == 1 && SystemProperties.getInt("ro.os_fingerprint_quickapp_support", 0) == 1;
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [1394=4] */
    /* JADX INFO: Access modifiers changed from: package-private */
    public int execute() {
        ActivityMetricsLogger.LaunchingState launchingState;
        int res;
        int res2;
        try {
            if (ITranActivityStarter.Instance().isAgaresEnable() && agaresPreload(this.mRequest)) {
                return 0;
            }
            ITranWindowManagerService.Instance().restoreInstallIntent(this.mService.mContext, this.mRequest);
            if (this.mRequest.intent != null && this.mRequest.intent.hasFileDescriptors()) {
                throw new IllegalArgumentException("File descriptors passed in Intent");
            }
            if (ThunderbackConfig.isVersion3()) {
                Request request = this.mRequest;
                if (request != null && request.activityOptions != null && this.mSupervisor != null) {
                    ActivityOptions tempActivityOptions = this.mRequest.activityOptions.getOptions(this.mSupervisor);
                    ITranActivityStarter.Instance().needHookStartActivity(TAG, this.mRequest, tempActivityOptions);
                }
            } else if (ThunderbackConfig.isVersion4()) {
                computeMultiWindowId(this.mRequest);
            }
            synchronized (this.mService.mGlobalLock) {
                WindowManagerService.boostPriorityForLockedSection();
                ActivityRecord caller = ActivityRecord.forTokenLocked(this.mRequest.resultTo);
                int callingUid = this.mRequest.realCallingUid == -1 ? Binder.getCallingUid() : this.mRequest.realCallingUid;
                launchingState = this.mSupervisor.getActivityMetricsLogger().notifyActivityLaunching(this.mRequest.intent, caller, callingUid);
                if (ITranActivityManagerService.Instance().isAppLaunchEnable()) {
                    boolean isAppLaunch = ITranActivityStarter.Instance().isAppLaunch(this.mRequest.intent, this.mRequest.callingPackage);
                    this.mIsAppLaunch = isAppLaunch;
                    if (isAppLaunch) {
                        ITranActivityStarter.Instance().appLaunchStart(this.mRequest.intent, this.mRequest.callingPackage, 0L);
                    }
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
            if (this.mRequest.activityInfo == null) {
                this.mRequest.resolveActivity(this.mSupervisor);
            }
            if (this.mRequest.intent != null) {
                String intentAction = this.mRequest.intent.getAction();
                String callingPackage = this.mRequest.callingPackage;
                if (intentAction != null && callingPackage != null && ("com.android.internal.intent.action.REQUEST_SHUTDOWN".equals(intentAction) || "android.intent.action.ACTION_SHUTDOWN".equals(intentAction) || "android.intent.action.REBOOT".equals(intentAction))) {
                    ShutdownCheckPoints.recordCheckPoint(intentAction, callingPackage, null);
                }
            }
            synchronized (this.mService.mGlobalLock) {
                WindowManagerService.boostPriorityForLockedSection();
                boolean globalConfigWillChange = (this.mRequest.globalConfig == null || this.mService.getGlobalConfiguration().diff(this.mRequest.globalConfig) == 0) ? false : true;
                Task rootTask = this.mRootWindowContainer.getTopDisplayFocusedRootTask();
                if (rootTask != null) {
                    rootTask.mConfigWillChange = globalConfigWillChange;
                }
                if (ProtoLogCache.WM_DEBUG_CONFIGURATION_enabled) {
                    boolean protoLogParam0 = globalConfigWillChange;
                    ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_CONFIGURATION, -1492881555, 3, (String) null, new Object[]{Boolean.valueOf(protoLogParam0)});
                }
                long origId = Binder.clearCallingIdentity();
                int res3 = resolveToHeavyWeightSwitcherIfNeeded();
                if (res3 != 0) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return res3;
                }
                int res4 = executeRequest(this.mRequest);
                Binder.restoreCallingIdentity(origId);
                if (globalConfigWillChange) {
                    this.mService.mAmInternal.enforceCallingPermission("android.permission.CHANGE_CONFIGURATION", "updateConfiguration()");
                    if (rootTask != null) {
                        rootTask.mConfigWillChange = false;
                    }
                    if (ProtoLogCache.WM_DEBUG_CONFIGURATION_enabled) {
                        Object[] objArr = null;
                        ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_CONFIGURATION, -1868048288, 0, (String) null, (Object[]) null);
                    }
                    this.mService.updateConfigurationLocked(this.mRequest.globalConfig, null, false);
                }
                ActivityOptions originalOptions = this.mRequest.activityOptions != null ? this.mRequest.activityOptions.getOriginalOptions() : null;
                ActivityRecord activityRecord = this.mStartActivity;
                ActivityRecord activityRecord2 = this.mLastStartActivityRecord;
                boolean newActivityCreated = activityRecord == activityRecord2;
                if (activityRecord2 == null || activityRecord2.mAlreadyLaunchered) {
                    res = res4;
                } else {
                    res = res4;
                    this.mSupervisor.getActivityMetricsLogger().notifyActivityLaunched(launchingState, res4, newActivityCreated, this.mLastStartActivityRecord, originalOptions);
                }
                ActivityRecord activityRecord3 = this.mLastStartActivityRecord;
                if (activityRecord3 != null) {
                    activityRecord3.mAlreadyLaunchered = false;
                }
                if (ITranActivityManagerService.Instance().isAppLaunchEnable() && this.mIsAppLaunch) {
                    ITranActivityStarter.Instance().appLaunchEnd(this.mRequest.intent, launchingState != null ? launchingState.launchType : -1, launchingState != null ? launchingState.procType : -1, this.mRecentIndex, 0L);
                }
                if (this.mRequest.waitResult != null) {
                    this.mRequest.waitResult.result = res;
                    res2 = waitResultIfNeeded(this.mRequest.waitResult, this.mLastStartActivityRecord, launchingState);
                } else {
                    res2 = res;
                }
                int externalResult = getExternalResult(res2);
                WindowManagerService.resetPriorityAfterLockedSection();
                return externalResult;
            }
        } finally {
            onExecutionComplete();
        }
    }

    private int resolveToHeavyWeightSwitcherIfNeeded() {
        WindowProcessController heavy;
        if (this.mRequest.activityInfo == null || !this.mService.mHasHeavyWeightFeature || (this.mRequest.activityInfo.applicationInfo.privateFlags & 2) == 0 || !this.mRequest.activityInfo.processName.equals(this.mRequest.activityInfo.applicationInfo.packageName) || (heavy = this.mService.mHeavyWeightProcess) == null || (heavy.mInfo.uid == this.mRequest.activityInfo.applicationInfo.uid && heavy.mName.equals(this.mRequest.activityInfo.processName))) {
            return 0;
        }
        int appCallingUid = this.mRequest.callingUid;
        if (this.mRequest.caller != null) {
            WindowProcessController callerApp = this.mService.getProcessController(this.mRequest.caller);
            if (callerApp != null) {
                appCallingUid = callerApp.mInfo.uid;
            } else {
                Slog.w(TAG, "Unable to find app for caller " + this.mRequest.caller + " (pid=" + this.mRequest.callingPid + ") when starting: " + this.mRequest.intent.toString());
                SafeActivityOptions.abort(this.mRequest.activityOptions);
                return -94;
            }
        }
        IIntentSender target = this.mService.getIntentSenderLocked(2, PackageManagerService.PLATFORM_PACKAGE_NAME, null, appCallingUid, this.mRequest.userId, null, null, 0, new Intent[]{this.mRequest.intent}, new String[]{this.mRequest.resolvedType}, 1342177280, null);
        Intent newIntent = new Intent();
        if (this.mRequest.requestCode >= 0) {
            newIntent.putExtra("has_result", true);
        }
        newIntent.putExtra("intent", new IntentSender(target));
        heavy.updateIntentForHeavyWeightActivity(newIntent);
        newIntent.putExtra("new_app", this.mRequest.activityInfo.packageName);
        newIntent.setFlags(this.mRequest.intent.getFlags());
        newIntent.setClassName(PackageManagerService.PLATFORM_PACKAGE_NAME, HeavyWeightSwitcherActivity.class.getName());
        this.mRequest.intent = newIntent;
        this.mRequest.resolvedType = null;
        this.mRequest.caller = null;
        this.mRequest.callingUid = Binder.getCallingUid();
        this.mRequest.callingPid = Binder.getCallingPid();
        this.mRequest.componentSpecified = true;
        Request request = this.mRequest;
        request.resolveInfo = this.mSupervisor.resolveIntent(request.intent, null, this.mRequest.userId, 0, computeResolveFilterUid(this.mRequest.callingUid, this.mRequest.realCallingUid, this.mRequest.filterCallingUid));
        Request request2 = this.mRequest;
        request2.activityInfo = request2.resolveInfo != null ? this.mRequest.resolveInfo.activityInfo : null;
        if (this.mRequest.activityInfo != null) {
            this.mRequest.activityInfo = this.mService.mAmInternal.getActivityInfoForUser(this.mRequest.activityInfo, this.mRequest.userId);
        }
        return 0;
    }

    private int waitResultIfNeeded(WaitResult waitResult, ActivityRecord r, ActivityMetricsLogger.LaunchingState launchingState) {
        int res = waitResult.result;
        if (res == 3 || (res == 2 && r.nowVisible && r.isState(ActivityRecord.State.RESUMED))) {
            waitResult.timeout = false;
            waitResult.who = r.mActivityComponent;
            waitResult.totalTime = 0L;
            return res;
        }
        this.mSupervisor.waitActivityVisibleOrLaunched(waitResult, r, launchingState);
        if (res == 0 && waitResult.result == 2) {
            return 2;
        }
        return res;
    }

    /* JADX WARN: Code restructure failed: missing block: B:230:0x0766, code lost:
        if (r15.applicationInfo.uid == r7.mUid) goto L173;
     */
    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Removed duplicated region for block: B:163:0x0460  */
    /* JADX WARN: Removed duplicated region for block: B:164:0x048b  */
    /* JADX WARN: Removed duplicated region for block: B:167:0x049d  */
    /* JADX WARN: Removed duplicated region for block: B:171:0x04b4  */
    /* JADX WARN: Removed duplicated region for block: B:197:0x0601  */
    /* JADX WARN: Removed duplicated region for block: B:198:0x0604  */
    /* JADX WARN: Removed duplicated region for block: B:201:0x062e  */
    /* JADX WARN: Removed duplicated region for block: B:206:0x0641  */
    /* JADX WARN: Removed duplicated region for block: B:221:0x071e  */
    /* JADX WARN: Removed duplicated region for block: B:222:0x0721  */
    /* JADX WARN: Removed duplicated region for block: B:229:0x075c  */
    /* JADX WARN: Removed duplicated region for block: B:232:0x0769  */
    /* JADX WARN: Removed duplicated region for block: B:240:0x07c4  */
    /* JADX WARN: Type inference failed for: r25v3 */
    /* JADX WARN: Type inference failed for: r25v4 */
    /* JADX WARN: Type inference failed for: r25v5 */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private int executeRequest(Request request) {
        String resultWho;
        int callingPid;
        WindowProcessController callerApp;
        String callingPackage;
        String str;
        String str2;
        int userId;
        ActivityRecord sourceRecord;
        String callingFeatureId;
        String callingPackage2;
        String callingPackage3;
        int requestCode;
        ActivityRecord resultRecord;
        IBinder resultTo;
        int userId2;
        int err;
        ActivityOptions checkedOptions;
        IVoiceInteractionSession voiceSession;
        String resolvedType;
        String str3;
        WindowProcessController callerApp2;
        int callingUid;
        NeededUriGrants intentGrants;
        ResolveInfo rInfo;
        int startFlags;
        int realCallingUid;
        TaskFragment inTaskFragment;
        String callingPackage4;
        ActivityInfo aInfo;
        Intent intent;
        boolean restrictedBgActivity;
        ActivityOptions checkedOptions2;
        String str4;
        boolean abort;
        ActivityOptions checkedOptions3;
        ActivityInfo aInfo2;
        int callingUid2;
        ResolveInfo rInfo2;
        String resolvedType2;
        NeededUriGrants intentGrants2;
        Intent intent2;
        Task inTask;
        int callingUid3;
        NeededUriGrants intentGrants3;
        ResolveInfo rInfo3;
        String resolvedType3;
        int userId3;
        int startFlags2;
        int realCallingUid2;
        int i;
        boolean z;
        int callingUid4;
        ResolveInfo rInfo4;
        String resolvedType4;
        NeededUriGrants intentGrants4;
        int callingPid2;
        IWindowManagerServiceLice.ActivityStarterInterceptInfo asii;
        ResolveInfo rInfo5;
        boolean z2;
        String callingPackage5;
        Request request2;
        ActivityStarter activityStarter;
        ActivityRecord sourceRecord2;
        String resultWho2;
        int realCallingPid;
        ActivityRecord resultRecord2;
        WindowProcessController callerApp3;
        ?? r25;
        int callingUid5;
        ActivityInfo aInfo3;
        Intent intent3;
        String resolvedType5;
        int callingPid3;
        ActivityRecord sourceRecord3;
        ActivityRecord r;
        WindowProcessController homeProcess;
        int realCallingPid2;
        String callingPackage6;
        String callingPackage7;
        long j;
        ActivityRecord resultRecord3;
        ActivityRecord resultRecord4;
        if (TextUtils.isEmpty(request.reason)) {
            throw new IllegalArgumentException("Need to specify a reason.");
        }
        this.mLastStartReason = request.reason;
        this.mLastStartActivityTimeMs = System.currentTimeMillis();
        this.mLastStartActivityRecord = null;
        IApplicationThread caller = request.caller;
        Intent intent4 = request.intent;
        NeededUriGrants intentGrants5 = request.intentGrants;
        String resolvedType6 = request.resolvedType;
        ActivityInfo aInfo4 = request.activityInfo;
        ResolveInfo rInfo6 = request.resolveInfo;
        IVoiceInteractionSession voiceSession2 = request.voiceSession;
        IBinder resultTo2 = request.resultTo;
        String resultWho3 = request.resultWho;
        int requestCode2 = request.requestCode;
        int callingPid4 = request.callingPid;
        int callingUid6 = request.callingUid;
        String callingPackage8 = request.callingPackage;
        String callingFeatureId2 = request.callingFeatureId;
        int realCallingPid3 = request.realCallingPid;
        int realCallingUid3 = request.realCallingUid;
        int startFlags3 = request.startFlags;
        SafeActivityOptions options = request.activityOptions;
        Task inTask2 = request.inTask;
        TaskFragment inTaskFragment2 = request.inTaskFragment;
        int err2 = 0;
        Bundle verificationBundle = options != null ? options.popAppVerificationBundle() : null;
        if (caller == null) {
            resultWho = resultWho3;
            callingPid = callingPid4;
            callerApp = null;
        } else {
            resultWho = resultWho3;
            WindowProcessController callerApp4 = this.mService.getProcessController(caller);
            if (callerApp4 != null) {
                int callingPid5 = callerApp4.getPid();
                callingUid6 = callerApp4.mInfo.uid;
                callerApp = callerApp4;
                callingPid = callingPid5;
            } else {
                Slog.w(TAG, "Unable to find app for caller " + caller + " (pid=" + callingPid4 + ") when starting: " + intent4.toString());
                err2 = -94;
                callingPid = callingPid4;
                callerApp = callerApp4;
                callingUid6 = callingUid6;
            }
        }
        int userId4 = (aInfo4 == null || aInfo4.applicationInfo == null) ? 0 : UserHandle.getUserId(aInfo4.applicationInfo.uid);
        if (!ITranActivityStarter.Instance().canStartAppInLowStorage(aInfo4, request.userId)) {
            SafeActivityOptions.abort(options);
            return 1000;
        }
        WindowProcessController callerApp5 = callerApp;
        if (err2 == 0) {
            str = "START u";
            userId = userId4;
            callingPackage = callingPackage8;
            str2 = " {";
            Slog.i(TAG, "START u" + userId + " {" + intent4.toShortString(true, true, true, false) + "} from uid " + callingUid6);
        } else {
            callingPackage = callingPackage8;
            str = "START u";
            str2 = " {";
            userId = userId4;
        }
        ActivityRecord resultRecord5 = null;
        if (resultTo2 == null) {
            sourceRecord = null;
        } else {
            ActivityRecord sourceRecord4 = ActivityRecord.isInAnyTask(resultTo2);
            if (ActivityTaskManagerDebugConfig.DEBUG_RESULTS) {
                resultRecord4 = null;
                Slog.v(TAG_RESULTS, "Will send result to " + resultTo2 + " " + sourceRecord4);
            } else {
                resultRecord4 = null;
            }
            if (sourceRecord4 != null && requestCode2 >= 0 && !sourceRecord4.finishing) {
                resultRecord5 = sourceRecord4;
                sourceRecord = sourceRecord4;
            } else {
                sourceRecord = sourceRecord4;
                resultRecord5 = resultRecord4;
            }
        }
        int launchFlags = intent4.getFlags();
        if ((launchFlags & 33554432) != 0 && sourceRecord != null) {
            if (requestCode2 >= 0) {
                SafeActivityOptions.abort(options);
                return -93;
            }
            ActivityRecord resultRecord6 = sourceRecord.resultTo;
            if (resultRecord6 != null && !resultRecord6.isInRootTaskLocked()) {
                resultRecord3 = null;
            } else {
                resultRecord3 = resultRecord6;
            }
            String resultWho4 = sourceRecord.resultWho;
            int requestCode3 = sourceRecord.requestCode;
            sourceRecord.resultTo = null;
            if (resultRecord3 != null) {
                resultRecord3.removeResultsLocked(sourceRecord, resultWho4, requestCode3);
            }
            if (sourceRecord.launchedFromUid != callingUid6) {
                callingFeatureId = callingFeatureId2;
                callingPackage2 = callingPackage;
                callingPackage3 = resultWho4;
                ActivityRecord activityRecord = resultRecord3;
                requestCode = requestCode3;
                resultRecord = activityRecord;
            } else {
                String callingPackage9 = sourceRecord.launchedFromPackage;
                callingFeatureId = sourceRecord.launchedFromFeatureId;
                callingPackage2 = callingPackage9;
                callingPackage3 = resultWho4;
                ActivityRecord activityRecord2 = resultRecord3;
                requestCode = requestCode3;
                resultRecord = activityRecord2;
            }
        } else {
            callingFeatureId = callingFeatureId2;
            callingPackage2 = callingPackage;
            callingPackage3 = resultWho;
            ActivityRecord activityRecord3 = resultRecord5;
            requestCode = requestCode2;
            resultRecord = activityRecord3;
        }
        if (err2 == 0 && intent4.getComponent() == null) {
            err2 = -91;
        }
        if (err2 != 0 || aInfo4 != null) {
            resultTo = resultTo2;
        } else {
            resultTo = resultTo2;
            if (ITranWindowManagerService.Instance().onStartActivity(intent4.getComponent(), userId)) {
                return 8;
            }
            err2 = -92;
        }
        if (err2 != 0 || sourceRecord == null) {
            userId2 = userId;
        } else if (sourceRecord.getTask().voiceSession == null) {
            userId2 = userId;
        } else if ((launchFlags & 268435456) == 0) {
            userId2 = userId;
            if (sourceRecord.info.applicationInfo.uid != aInfo4.applicationInfo.uid) {
                try {
                    intent4.addCategory("android.intent.category.VOICE");
                    if (!this.mService.getPackageManager().activitySupportsIntent(intent4.getComponent(), intent4, resolvedType6)) {
                        Slog.w(TAG, "Activity being started in current voice task does not support voice: " + intent4);
                        err2 = -97;
                    }
                } catch (RemoteException e) {
                    Slog.w(TAG, "Failure checking voice capabilities", e);
                    err2 = -97;
                }
            }
        } else {
            userId2 = userId;
        }
        if (err2 == 0 && voiceSession2 != null) {
            try {
                if (!this.mService.getPackageManager().activitySupportsIntent(intent4.getComponent(), intent4, resolvedType6)) {
                    Slog.w(TAG, "Activity being started in new voice task does not support: " + intent4);
                    err2 = -97;
                }
                err = err2;
            } catch (RemoteException e2) {
                Slog.w(TAG, "Failure checking voice capabilities", e2);
                err = -97;
            }
        } else {
            err = err2;
        }
        Task resultRootTask = resultRecord == null ? null : resultRecord.getRootTask();
        if (err != 0) {
            if (resultRecord != null) {
                resultRecord.sendResult(-1, callingPackage3, requestCode, 0, null, null);
            }
            SafeActivityOptions.abort(options);
            return err;
        }
        boolean abort2 = !this.mSupervisor.checkStartAnyActivityPermission(intent4, aInfo4, callingPackage3, requestCode, callingPid, callingUid6, callingPackage2, callingFeatureId, request.ignoreTargetSecurity, inTask2 != null, callerApp5, resultRecord, resultRootTask);
        ActivityRecord sourceRecord5 = sourceRecord;
        ActivityRecord sourceRecord6 = resultRecord;
        String str5 = str;
        int userId5 = userId2;
        String resultWho5 = callingPackage3;
        int callingUid7 = callingUid6;
        int requestCode4 = requestCode;
        String str6 = str2;
        String callingPackage10 = callingPackage2;
        boolean abort3 = abort2 | (!this.mService.mIntentFirewall.checkStartActivity(intent4, callingUid6, callingPid, resolvedType6, aInfo4.applicationInfo)) | (!this.mService.getPermissionPolicyInternal().checkStartActivity(intent4, callingUid7, callingPackage10));
        ActivityOptions checkedOptions4 = options != null ? options.getOptions(intent4, aInfo4, callerApp5, this.mSupervisor) : null;
        if (ThunderbackConfig.isVersion4()) {
            computeOptions(checkedOptions4);
        }
        if (new ComponentName("com.transsion.camera", "com.transsion.camera.app.SlaveScreenActivity").equals(intent4.getComponent())) {
            Slog.i(TAG, "setAvoidMoveToFront for com.transsion.camera.app.SlaveScreenActivity");
            checkedOptions4.setAvoidMoveToFront();
        }
        if (abort3) {
            checkedOptions = checkedOptions4;
            voiceSession = voiceSession2;
            resolvedType = resolvedType6;
            str3 = TAG;
            callerApp2 = callerApp5;
            callingUid = callingUid7;
            intentGrants = intentGrants5;
            rInfo = rInfo6;
            startFlags = startFlags3;
            realCallingUid = realCallingUid3;
            inTaskFragment = inTaskFragment2;
            callingPackage4 = callingPackage10;
            aInfo = aInfo4;
            intent = intent4;
            restrictedBgActivity = false;
        } else {
            try {
                Trace.traceBegin(32L, "shouldAbortBackgroundActivityStart");
                PendingIntentRecord pendingIntentRecord = request.originatingPendingIntent;
                boolean z3 = request.allowBackgroundActivityStart;
                j = 32;
                int i2 = callingPid;
                checkedOptions = checkedOptions4;
                callingUid = callingUid7;
                callingPackage4 = callingPackage10;
                voiceSession = voiceSession2;
                rInfo = rInfo6;
                startFlags = startFlags3;
                callerApp2 = callerApp5;
                aInfo = aInfo4;
                resolvedType = resolvedType6;
                str3 = TAG;
                intentGrants = intentGrants5;
                realCallingUid = realCallingUid3;
                inTaskFragment = inTaskFragment2;
                intent = intent4;
                try {
                    boolean restrictedBgActivity2 = shouldAbortBackgroundActivityStart(callingUid7, i2, callingPackage10, realCallingUid3, realCallingPid3, callerApp5, pendingIntentRecord, z3, intent4, checkedOptions);
                    Trace.traceEnd(32L);
                    restrictedBgActivity = restrictedBgActivity2;
                } catch (Throwable th) {
                    th = th;
                    Trace.traceEnd(j);
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
                j = 32;
            }
        }
        if (!request.allowPendingRemoteAnimationRegistryLookup) {
            checkedOptions2 = checkedOptions;
        } else {
            checkedOptions2 = this.mService.getActivityStartController().getPendingRemoteAnimationRegistry().overrideOptionsIfNeeded(callingPackage4, checkedOptions);
        }
        if (this.mService.mController == null) {
            str4 = str3;
        } else {
            try {
                Intent watchIntent = intent.cloneFilter();
                boolean isPreload = false;
                if (watchIntent != null) {
                    isPreload = watchIntent.getPreload();
                }
                if (!isPreload) {
                    abort3 |= !this.mService.mController.activityStarting(watchIntent, aInfo.applicationInfo.packageName);
                    str4 = str3;
                } else {
                    str4 = str3;
                    try {
                        Slog.d(str4, "not call activityStarting, because watchIntent getPreload is true");
                    } catch (RemoteException e3) {
                        this.mService.mController = null;
                        abort = abort3;
                        this.mInterceptor.setStates(userId5, realCallingPid3, realCallingUid, startFlags, callingPackage4, callingFeatureId);
                        if (!this.mInterceptor.intercept(intent, rInfo, aInfo, resolvedType, inTask2, callingPid, callingUid, checkedOptions2)) {
                        }
                        if (!abort) {
                        }
                    }
                }
                abort = abort3;
            } catch (RemoteException e4) {
                str4 = str3;
            }
            this.mInterceptor.setStates(userId5, realCallingPid3, realCallingUid, startFlags, callingPackage4, callingFeatureId);
            if (!this.mInterceptor.intercept(intent, rInfo, aInfo, resolvedType, inTask2, callingPid, callingUid, checkedOptions2)) {
                checkedOptions3 = checkedOptions2;
                aInfo2 = aInfo;
                callingUid2 = callingUid;
                rInfo2 = rInfo;
                resolvedType2 = resolvedType;
                intentGrants2 = intentGrants;
                intent2 = intent;
                inTask = inTask2;
            } else {
                intent2 = this.mInterceptor.mIntent;
                rInfo2 = this.mInterceptor.mRInfo;
                aInfo2 = this.mInterceptor.mAInfo;
                String resolvedType7 = this.mInterceptor.mResolvedType;
                Task inTask3 = this.mInterceptor.mInTask;
                int callingPid6 = this.mInterceptor.mCallingPid;
                callingUid2 = this.mInterceptor.mCallingUid;
                intentGrants2 = null;
                inTask = inTask3;
                callingPid = callingPid6;
                resolvedType2 = resolvedType7;
                checkedOptions3 = this.mInterceptor.mActivityOptions;
            }
            if (!abort) {
                if (ITranWindowManagerService.Instance().interceptUnknowSource(this.mService.mContext, request)) {
                    return 102;
                }
                if (aInfo2 == null) {
                    callingUid3 = callingUid2;
                    intentGrants3 = intentGrants2;
                    rInfo3 = rInfo2;
                    resolvedType3 = resolvedType2;
                    userId3 = userId5;
                    startFlags2 = startFlags;
                    realCallingUid2 = realCallingUid;
                    i = 0;
                    z = true;
                } else {
                    userId3 = userId5;
                    if (!this.mService.getPackageManagerInternalLocked().isPermissionsReviewRequired(aInfo2.packageName, userId3)) {
                        callingUid3 = callingUid2;
                        intentGrants3 = intentGrants2;
                        rInfo3 = rInfo2;
                        resolvedType3 = resolvedType2;
                        startFlags2 = startFlags;
                        realCallingUid2 = realCallingUid;
                        i = 0;
                        z = true;
                    } else {
                        IIntentSender target = this.mService.getIntentSenderLocked(2, callingPackage4, callingFeatureId, callingUid2, userId3, null, null, 0, new Intent[]{intent2}, new String[]{resolvedType2}, 1342177280, null);
                        Intent newIntent = new Intent("android.intent.action.REVIEW_PERMISSIONS");
                        int flags = intent2.getFlags() | 8388608;
                        if ((268959744 & flags) != 0) {
                            flags |= 134217728;
                        }
                        newIntent.setFlags(flags);
                        newIntent.putExtra("android.intent.extra.PACKAGE_NAME", aInfo2.packageName);
                        newIntent.putExtra("android.intent.extra.INTENT", new IntentSender(target));
                        if (sourceRecord6 != null) {
                            newIntent.putExtra("android.intent.extra.RESULT_NEEDED", true);
                        }
                        intent2 = newIntent;
                        callingUid4 = realCallingUid;
                        realCallingUid2 = realCallingUid;
                        ResolveInfo rInfo7 = this.mSupervisor.resolveIntent(intent2, null, userId3, 0, computeResolveFilterUid(callingUid4, realCallingUid2, request.filterCallingUid));
                        resolvedType4 = null;
                        startFlags2 = startFlags;
                        aInfo2 = this.mSupervisor.resolveActivity(intent2, rInfo7, startFlags2, null);
                        if (!ActivityTaskManagerDebugConfig.DEBUG_PERMISSIONS_REVIEW) {
                            rInfo4 = rInfo7;
                            intentGrants4 = null;
                            i = 0;
                            z = true;
                        } else {
                            Task focusedRootTask = this.mRootWindowContainer.getTopDisplayFocusedRootTask();
                            rInfo4 = rInfo7;
                            intentGrants4 = null;
                            i = 0;
                            z = true;
                            Slog.i(str4, str5 + userId3 + str6 + intent2.toShortString(true, true, true, false) + "} from uid " + callingUid4 + " on display " + (focusedRootTask == null ? 0 : focusedRootTask.getDisplayId()));
                        }
                        callingPid2 = realCallingPid3;
                        asii = ITranWindowManagerService.Instance().onActivityStarterIntercept(aInfo2, callingPackage4, callingFeatureId, callingUid4, userId3, intent2, resolvedType4, sourceRecord6, realCallingUid2, this.mSupervisor, computeResolveFilterUid(realCallingUid2, realCallingUid2, this.mRequest.filterCallingUid), startFlags2, checkedOptions3, this.mRootWindowContainer.getTopDisplayFocusedRootTask() != null ? i : this.mRootWindowContainer.getTopDisplayFocusedRootTask().getDisplayId());
                        if (asii != null) {
                            rInfo5 = rInfo4;
                        } else if (asii.intent == null) {
                            return 102;
                        } else {
                            intent2 = asii.intent;
                            resolvedType4 = null;
                            callingUid4 = realCallingUid2;
                            callingPid2 = realCallingPid3;
                            rInfo5 = asii.rInfo;
                            aInfo2 = asii.aInfo;
                        }
                        ITranWindowManagerService.Instance().restoreOptions(this.mRequest, checkedOptions3);
                        if (rInfo5 == null && rInfo5.auxiliaryInfo != null) {
                            callerApp3 = callerApp2;
                            r25 = 0;
                            z2 = z;
                            callingPackage5 = callingPackage4;
                            resultWho2 = resultWho5;
                            realCallingPid = realCallingPid3;
                            request2 = request;
                            sourceRecord2 = sourceRecord5;
                            resultRecord2 = sourceRecord6;
                            activityStarter = this;
                            intent3 = createLaunchIntent(rInfo5.auxiliaryInfo, request.ephemeralIntent, callingPackage5, callingFeatureId, verificationBundle, resolvedType4, userId3);
                            callingPid3 = realCallingPid;
                            intentGrants4 = null;
                            callingUid5 = realCallingUid2;
                            aInfo3 = activityStarter.mSupervisor.resolveActivity(intent3, rInfo5, startFlags2, null);
                            resolvedType5 = null;
                        } else {
                            int callingPid7 = callingPid2;
                            z2 = z;
                            Intent intent5 = intent2;
                            callingPackage5 = callingPackage4;
                            request2 = request;
                            activityStarter = this;
                            sourceRecord2 = sourceRecord5;
                            resultWho2 = resultWho5;
                            realCallingPid = realCallingPid3;
                            resultRecord2 = sourceRecord6;
                            callerApp3 = callerApp2;
                            r25 = 0;
                            callingUid5 = callingUid4;
                            aInfo3 = aInfo2;
                            intent3 = intent5;
                            resolvedType5 = resolvedType4;
                            callingPid3 = callingPid7;
                        }
                        if (callerApp3 == null || realCallingPid <= 0 || (wpc = activityStarter.mService.mProcessMap.getProcess(realCallingPid)) == null) {
                            WindowProcessController wpc = callerApp3;
                        }
                        String callingPackage11 = callingPackage5;
                        sourceRecord3 = sourceRecord2;
                        r = new ActivityRecord.Builder(activityStarter.mService).setCaller(wpc).setLaunchedFromPid(callingPid3).setLaunchedFromUid(callingUid5).setLaunchedFromPackage(callingPackage11).setLaunchedFromFeature(callingFeatureId).setIntent(intent3).setResolvedType(resolvedType5).setActivityInfo(aInfo3).setConfiguration(activityStarter.mService.getGlobalConfiguration()).setResultTo(resultRecord2).setResultWho(resultWho2).setRequestCode(requestCode4).setComponentSpecified(request2.componentSpecified).setRootVoiceInteraction(voiceSession == null ? z2 : r25).setActivityOptions(checkedOptions3).setSourceRecord(sourceRecord3).build();
                        ITranWindowManagerService.Instance().onStartActivity(r, callingPid3, realCallingPid, callingPackage11, activityStarter.mLastStartReason);
                        activityStarter.mLastStartActivityRecord = r;
                        if (r.appTimeTracker == null && sourceRecord3 != null) {
                            r.appTimeTracker = sourceRecord3.appTimeTracker;
                        }
                        homeProcess = activityStarter.mService.mHomeProcess;
                        if (homeProcess == null) {
                            realCallingPid2 = realCallingPid;
                            callingPackage6 = callingPackage11;
                        } else {
                            realCallingPid2 = realCallingPid;
                            callingPackage6 = callingPackage11;
                        }
                        z2 = r25;
                        boolean isHomeProcess = z2;
                        if (!restrictedBgActivity && !isHomeProcess) {
                            activityStarter.mService.resumeAppSwitches();
                        }
                        callingPackage7 = callingPackage6;
                        this.mLastStartActivityResult = startActivityUnchecked(r, sourceRecord3, voiceSession, request2.voiceInteractor, startFlags2, true, checkedOptions3, inTask, inTaskFragment, restrictedBgActivity, intentGrants4);
                        if (request.outActivity != null) {
                            request.outActivity[r25] = this.mLastStartActivityRecord;
                        }
                        if (restrictedBgActivity && this.mLastStartActivityResult != 0) {
                            ITranWindowManagerService.Instance().onBackgroundActivityPrevented(callingUid5, callingPackage7);
                        }
                        return this.mLastStartActivityResult;
                    }
                }
                callingUid4 = callingUid3;
                rInfo4 = rInfo3;
                resolvedType4 = resolvedType3;
                intentGrants4 = intentGrants3;
                callingPid2 = callingPid;
                asii = ITranWindowManagerService.Instance().onActivityStarterIntercept(aInfo2, callingPackage4, callingFeatureId, callingUid4, userId3, intent2, resolvedType4, sourceRecord6, realCallingUid2, this.mSupervisor, computeResolveFilterUid(realCallingUid2, realCallingUid2, this.mRequest.filterCallingUid), startFlags2, checkedOptions3, this.mRootWindowContainer.getTopDisplayFocusedRootTask() != null ? i : this.mRootWindowContainer.getTopDisplayFocusedRootTask().getDisplayId());
                if (asii != null) {
                }
                ITranWindowManagerService.Instance().restoreOptions(this.mRequest, checkedOptions3);
                if (rInfo5 == null) {
                }
                int callingPid72 = callingPid2;
                z2 = z;
                Intent intent52 = intent2;
                callingPackage5 = callingPackage4;
                request2 = request;
                activityStarter = this;
                sourceRecord2 = sourceRecord5;
                resultWho2 = resultWho5;
                realCallingPid = realCallingPid3;
                resultRecord2 = sourceRecord6;
                callerApp3 = callerApp2;
                r25 = 0;
                callingUid5 = callingUid4;
                aInfo3 = aInfo2;
                intent3 = intent52;
                resolvedType5 = resolvedType4;
                callingPid3 = callingPid72;
                if (callerApp3 == null) {
                }
                WindowProcessController wpc2 = callerApp3;
                String callingPackage112 = callingPackage5;
                sourceRecord3 = sourceRecord2;
                r = new ActivityRecord.Builder(activityStarter.mService).setCaller(wpc2).setLaunchedFromPid(callingPid3).setLaunchedFromUid(callingUid5).setLaunchedFromPackage(callingPackage112).setLaunchedFromFeature(callingFeatureId).setIntent(intent3).setResolvedType(resolvedType5).setActivityInfo(aInfo3).setConfiguration(activityStarter.mService.getGlobalConfiguration()).setResultTo(resultRecord2).setResultWho(resultWho2).setRequestCode(requestCode4).setComponentSpecified(request2.componentSpecified).setRootVoiceInteraction(voiceSession == null ? z2 : r25).setActivityOptions(checkedOptions3).setSourceRecord(sourceRecord3).build();
                ITranWindowManagerService.Instance().onStartActivity(r, callingPid3, realCallingPid, callingPackage112, activityStarter.mLastStartReason);
                activityStarter.mLastStartActivityRecord = r;
                if (r.appTimeTracker == null) {
                    r.appTimeTracker = sourceRecord3.appTimeTracker;
                }
                homeProcess = activityStarter.mService.mHomeProcess;
                if (homeProcess == null) {
                }
                z2 = r25;
                boolean isHomeProcess2 = z2;
                if (!restrictedBgActivity) {
                    activityStarter.mService.resumeAppSwitches();
                }
                callingPackage7 = callingPackage6;
                this.mLastStartActivityResult = startActivityUnchecked(r, sourceRecord3, voiceSession, request2.voiceInteractor, startFlags2, true, checkedOptions3, inTask, inTaskFragment, restrictedBgActivity, intentGrants4);
                if (request.outActivity != null) {
                }
                if (restrictedBgActivity) {
                    ITranWindowManagerService.Instance().onBackgroundActivityPrevented(callingUid5, callingPackage7);
                }
                return this.mLastStartActivityResult;
            }
            if (sourceRecord6 != null) {
                sourceRecord6.sendResult(-1, resultWho5, requestCode4, 0, null, null);
            }
            ActivityOptions.abort(checkedOptions3);
            return 102;
        }
        abort = abort3;
        this.mInterceptor.setStates(userId5, realCallingPid3, realCallingUid, startFlags, callingPackage4, callingFeatureId);
        if (!this.mInterceptor.intercept(intent, rInfo, aInfo, resolvedType, inTask2, callingPid, callingUid, checkedOptions2)) {
        }
        if (!abort) {
        }
    }

    private boolean handleBackgroundActivityAbort(ActivityRecord r) {
        boolean abort = !this.mService.isBackgroundActivityStartsEnabled();
        if (!abort) {
            return false;
        }
        ActivityRecord resultRecord = r.resultTo;
        String resultWho = r.resultWho;
        int requestCode = r.requestCode;
        if (resultRecord != null) {
            resultRecord.sendResult(-1, resultWho, requestCode, 0, null, null);
        }
        ActivityOptions.abort(r.getOptions());
        return true;
    }

    static int getExternalResult(int result) {
        if (result != 102) {
            return result;
        }
        return 0;
    }

    private void onExecutionComplete() {
        this.mController.onExecutionComplete(this);
    }

    private boolean isHomeApp(int uid, String packageName) {
        if (this.mService.mHomeProcess != null) {
            return uid == this.mService.mHomeProcess.mUid;
        } else if (packageName == null) {
            return false;
        } else {
            ComponentName activity = this.mService.getPackageManagerInternalLocked().getDefaultHomeActivity(UserHandle.getUserId(uid));
            return activity != null && packageName.equals(activity.getPackageName());
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: Code restructure failed: missing block: B:132:0x022c, code lost:
        if (com.android.server.wm.ActivityTaskManagerDebugConfig.DEBUG_ACTIVITY_STARTS == false) goto L102;
     */
    /* JADX WARN: Code restructure failed: missing block: B:133:0x022e, code lost:
        android.util.Slog.d(com.android.server.wm.ActivityStarter.TAG, "Activity start allowed: realCallingUid (" + r38 + ") has visible (non-toast) window");
     */
    /* JADX WARN: Code restructure failed: missing block: B:134:0x0248, code lost:
        return false;
     */
    /* JADX WARN: Removed duplicated region for block: B:100:0x018a  */
    /* JADX WARN: Removed duplicated region for block: B:109:0x01a0  */
    /* JADX WARN: Removed duplicated region for block: B:117:0x01dd  */
    /* JADX WARN: Removed duplicated region for block: B:149:0x029d  */
    /* JADX WARN: Removed duplicated region for block: B:211:0x045b A[RETURN] */
    /* JADX WARN: Removed duplicated region for block: B:212:0x045c  */
    /* JADX WARN: Removed duplicated region for block: B:81:0x0154  */
    /* JADX WARN: Removed duplicated region for block: B:82:0x0156  */
    /* JADX WARN: Removed duplicated region for block: B:85:0x0161  */
    /* JADX WARN: Removed duplicated region for block: B:86:0x0163  */
    /* JADX WARN: Removed duplicated region for block: B:89:0x016c  */
    /* JADX WARN: Removed duplicated region for block: B:90:0x0171  */
    /* JADX WARN: Removed duplicated region for block: B:99:0x0186  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public boolean shouldAbortBackgroundActivityStart(int callingUid, int callingPid, String callingPackage, int realCallingUid, int realCallingPid, WindowProcessController callerApp, PendingIntentRecord originatingPendingIntent, boolean allowBackgroundActivityStart, Intent intent, ActivityOptions checkedOptions) {
        boolean isCallingUidPersistentSystemProcess;
        boolean z;
        int realCallingUidProcState;
        boolean realCallingUidHasAnyVisibleWindow;
        boolean callingUidHasAnyVisibleWindow;
        boolean callingUidHasAnyVisibleWindow2;
        boolean isRealCallingUidForeground;
        boolean isRealCallingUidForeground2;
        int realCallingUidProcState2;
        int callingUidProcState;
        boolean balAllowedByPiSender;
        boolean isCallingUidPersistentSystemProcess2;
        WindowProcessController callerApp2;
        int callerAppUid;
        boolean realCallingUidHasAnyVisibleWindow2;
        int callingUidProcState2;
        int callingAppId = UserHandle.getAppId(callingUid);
        boolean useCallingUidState = originatingPendingIntent == null || checkedOptions == null || !checkedOptions.getIgnorePendingIntentCreatorForegroundState();
        if (useCallingUidState) {
            if (callingUid == 0 || callingAppId == 1000 || callingAppId == 1027) {
                if (ActivityTaskManagerDebugConfig.DEBUG_ACTIVITY_STARTS) {
                    Slog.d(TAG, "Activity start allowed for important callingUid (" + callingUid + ")");
                }
                return false;
            } else if (isHomeApp(callingUid, callingPackage)) {
                if (ActivityTaskManagerDebugConfig.DEBUG_ACTIVITY_STARTS) {
                    Slog.d(TAG, "Activity start allowed for home app callingUid (" + callingUid + ")");
                }
                return false;
            } else {
                WindowState imeWindow = this.mRootWindowContainer.getCurrentInputMethodWindow();
                if (imeWindow != null && callingAppId == imeWindow.mOwnerUid) {
                    if (ActivityTaskManagerDebugConfig.DEBUG_ACTIVITY_STARTS) {
                        Slog.d(TAG, "Activity start allowed for active ime (" + callingUid + ")");
                    }
                    return false;
                } else if (checkedOptions != null && checkedOptions.getIsCompatibleModeChanged()) {
                    if (ActivityTaskManagerDebugConfig.DEBUG_ACTIVITY_STARTS) {
                        Slog.d(TAG, "Activity start allowed for compatible mode changed callingUid (" + callingUid + ")");
                    }
                    return false;
                }
            }
        }
        int appSwitchState = this.mService.getBalAppSwitchesState();
        int callingUidProcState3 = this.mService.mActiveUids.getUidState(callingUid);
        boolean callingUidHasAnyVisibleWindow3 = this.mService.hasActiveVisibleWindow(callingUid);
        boolean isCallingUidForeground = callingUidHasAnyVisibleWindow3 || callingUidProcState3 == 2 || callingUidProcState3 == 3;
        boolean isCallingUidPersistentSystemProcess3 = callingUidProcState3 <= 1;
        boolean appSwitchAllowedOrFg = appSwitchState == 2 || appSwitchState == 1;
        if ((appSwitchAllowedOrFg || this.mService.mActiveUids.hasNonAppVisibleWindow(callingUid)) && callingUidHasAnyVisibleWindow3) {
            isCallingUidPersistentSystemProcess = isCallingUidPersistentSystemProcess3;
        } else {
            isCallingUidPersistentSystemProcess = isCallingUidPersistentSystemProcess3;
            if (!isCallingUidPersistentSystemProcess) {
                z = false;
                boolean allowCallingUidStartActivity = z;
                if (useCallingUidState || !allowCallingUidStartActivity) {
                    boolean isCallingUidForeground2 = isCallingUidForeground;
                    if (callingUid != realCallingUid) {
                        realCallingUidProcState = callingUidProcState3;
                    } else {
                        realCallingUidProcState = this.mService.mActiveUids.getUidState(realCallingUid);
                    }
                    if (callingUid != realCallingUid) {
                        realCallingUidHasAnyVisibleWindow = callingUidHasAnyVisibleWindow3;
                    } else {
                        realCallingUidHasAnyVisibleWindow = this.mService.hasActiveVisibleWindow(realCallingUid);
                    }
                    if (callingUid != realCallingUid) {
                        callingUidHasAnyVisibleWindow = callingUidHasAnyVisibleWindow3;
                        callingUidHasAnyVisibleWindow2 = isCallingUidForeground2;
                    } else {
                        if (realCallingUidHasAnyVisibleWindow) {
                            callingUidHasAnyVisibleWindow = callingUidHasAnyVisibleWindow3;
                        } else {
                            callingUidHasAnyVisibleWindow = callingUidHasAnyVisibleWindow3;
                            if (realCallingUidProcState != 2) {
                                callingUidHasAnyVisibleWindow2 = false;
                            }
                        }
                        callingUidHasAnyVisibleWindow2 = true;
                    }
                    int realCallingAppId = UserHandle.getAppId(realCallingUid);
                    if (callingUid != realCallingUid) {
                        isRealCallingUidForeground = callingUidHasAnyVisibleWindow2;
                        isRealCallingUidForeground2 = isCallingUidPersistentSystemProcess;
                    } else {
                        isRealCallingUidForeground = callingUidHasAnyVisibleWindow2;
                        isRealCallingUidForeground2 = realCallingAppId == 1000 || realCallingUidProcState <= 1;
                    }
                    if (Process.isSdkSandboxUid(realCallingUid)) {
                        realCallingUidProcState2 = realCallingUidProcState;
                        callingUidProcState = callingUidProcState3;
                    } else {
                        realCallingUidProcState2 = realCallingUidProcState;
                        int realCallingSdkSandboxUidToAppUid = Process.getAppUidForSdkSandboxUid(UserHandle.getAppId(realCallingUid));
                        callingUidProcState = callingUidProcState3;
                        if (this.mService.hasActiveVisibleWindow(realCallingSdkSandboxUidToAppUid)) {
                            if (ActivityTaskManagerDebugConfig.DEBUG_ACTIVITY_STARTS) {
                                Slog.d(TAG, "Activity start allowed: uid in SDK sandbox (" + realCallingUid + ") has visible (non-toast) window.");
                            }
                            return false;
                        }
                    }
                    balAllowedByPiSender = PendingIntentRecord.isPendingIntentBalAllowedByCaller(checkedOptions);
                    if (balAllowedByPiSender || realCallingUid == callingUid) {
                        isCallingUidPersistentSystemProcess2 = isCallingUidPersistentSystemProcess;
                    } else {
                        boolean useCallerPermission = PendingIntentRecord.isPendingIntentBalAllowedByPermission(checkedOptions);
                        isCallingUidPersistentSystemProcess2 = isCallingUidPersistentSystemProcess;
                        if (useCallerPermission && ActivityManager.checkComponentPermission("android.permission.START_ACTIVITIES_FROM_BACKGROUND", realCallingUid, -1, true) == 0) {
                            if (ActivityTaskManagerDebugConfig.DEBUG_ACTIVITY_STARTS) {
                                Slog.d(TAG, "Activity start allowed: realCallingUid (" + realCallingUid + ") has BAL permission.");
                            }
                            return false;
                        }
                        if (isRealCallingUidForeground2 && allowBackgroundActivityStart) {
                            if (ActivityTaskManagerDebugConfig.DEBUG_ACTIVITY_STARTS) {
                                Slog.d(TAG, "Activity start allowed: realCallingUid (" + realCallingUid + ") is persistent system process AND intent sender allowed (allowBackgroundActivityStart = true)");
                            }
                            return false;
                        } else if (this.mService.isAssociatedCompanionApp(UserHandle.getUserId(realCallingUid), realCallingUid)) {
                            if (ActivityTaskManagerDebugConfig.DEBUG_ACTIVITY_STARTS) {
                                Slog.d(TAG, "Activity start allowed: realCallingUid (" + realCallingUid + ") is companion app");
                            }
                            return false;
                        }
                    }
                    if (useCallingUidState) {
                        if (ActivityTaskManagerService.checkPermission("android.permission.START_ACTIVITIES_FROM_BACKGROUND", callingPid, callingUid) == 0) {
                            if (ActivityTaskManagerDebugConfig.DEBUG_ACTIVITY_STARTS) {
                                Slog.d(TAG, "Background activity start allowed: START_ACTIVITIES_FROM_BACKGROUND permission granted for uid " + callingUid);
                            }
                            return false;
                        } else if (this.mSupervisor.mRecentTasks.isCallerRecents(callingUid)) {
                            if (ActivityTaskManagerDebugConfig.DEBUG_ACTIVITY_STARTS) {
                                Slog.d(TAG, "Background activity start allowed: callingUid (" + callingUid + ") is recents");
                            }
                            return false;
                        } else if (this.mService.isDeviceOwner(callingUid)) {
                            if (ActivityTaskManagerDebugConfig.DEBUG_ACTIVITY_STARTS) {
                                Slog.d(TAG, "Background activity start allowed: callingUid (" + callingUid + ") is device owner");
                            }
                            return false;
                        } else {
                            int callingUserId = UserHandle.getUserId(callingUid);
                            if (this.mService.isAssociatedCompanionApp(callingUserId, callingUid)) {
                                if (ActivityTaskManagerDebugConfig.DEBUG_ACTIVITY_STARTS) {
                                    Slog.d(TAG, "Background activity start allowed: callingUid (" + callingUid + ") is companion app");
                                }
                                return false;
                            } else if (this.mService.hasSystemAlertWindowPermission(callingUid, callingPid, callingPackage)) {
                                Slog.w(TAG, "Background activity start for " + callingPackage + " allowed because SYSTEM_ALERT_WINDOW permission is granted.");
                                return false;
                            }
                        }
                    }
                    if (callerApp != null && balAllowedByPiSender) {
                        callerAppUid = realCallingUid;
                        callerApp2 = this.mService.getProcessController(realCallingPid, realCallingUid);
                    } else {
                        callerApp2 = callerApp;
                        callerAppUid = callingUid;
                    }
                    if (callerApp2 != null || !useCallingUidState) {
                        realCallingUidHasAnyVisibleWindow2 = realCallingUidHasAnyVisibleWindow;
                    } else if (callerApp2.areBackgroundActivityStartsAllowed(appSwitchState)) {
                        if (ActivityTaskManagerDebugConfig.DEBUG_ACTIVITY_STARTS) {
                            Slog.d(TAG, "Background activity start allowed: callerApp process (pid = " + callerApp2.getPid() + ", uid = " + callerAppUid + ") is allowed");
                        }
                        return false;
                    } else {
                        ArraySet<WindowProcessController> uidProcesses = this.mService.mProcessMap.getProcesses(callerAppUid);
                        if (uidProcesses != null) {
                            int i = uidProcesses.size() - 1;
                            while (i >= 0) {
                                WindowProcessController proc = uidProcesses.valueAt(i);
                                if (proc == callerApp2 || !proc.areBackgroundActivityStartsAllowed(appSwitchState)) {
                                    i--;
                                    uidProcesses = uidProcesses;
                                    realCallingUidHasAnyVisibleWindow = realCallingUidHasAnyVisibleWindow;
                                } else {
                                    if (ActivityTaskManagerDebugConfig.DEBUG_ACTIVITY_STARTS) {
                                        Slog.d(TAG, "Background activity start allowed: process " + proc.getPid() + " from uid " + callerAppUid + " is allowed");
                                    }
                                    return false;
                                }
                            }
                            realCallingUidHasAnyVisibleWindow2 = realCallingUidHasAnyVisibleWindow;
                        } else {
                            realCallingUidHasAnyVisibleWindow2 = realCallingUidHasAnyVisibleWindow;
                        }
                    }
                    boolean realCallingUidHasAnyVisibleWindow3 = realCallingUidHasAnyVisibleWindow2;
                    boolean isRealCallingUidPersistentSystemProcess = isRealCallingUidForeground2;
                    callingUidProcState2 = callingUidProcState;
                    boolean isCallingUidPersistentSystemProcess4 = isCallingUidPersistentSystemProcess2;
                    WindowProcessController callerApp3 = callerApp2;
                    if (ITranWindowManagerService.Instance().onShouldAbortBackgroundActivityStart(this, callerApp2, callingUid, callingPid, callingUidProcState2, this.mService.getProcessController(realCallingPid, realCallingUid), realCallingUid, realCallingPid, realCallingUidProcState2)) {
                        if (ITranActivityStarter.Instance().isInMultiWindow(callerApp3, callingPackage)) {
                            Slog.w(TAG, "callingPackage = " + callingPackage + ": allowed doResumed");
                            return false;
                        }
                        int realCallingUidProcState3 = realCallingUidProcState2;
                        Slog.w(TAG, "Background activity start [callingPackage: " + callingPackage + "; callingUid: " + callingUid + "; appSwitchState: " + appSwitchState + "; isCallingUidForeground: " + isCallingUidForeground2 + "; callingUidHasAnyVisibleWindow: " + callingUidHasAnyVisibleWindow + "; callingUidProcState: " + DebugUtils.valueToString(ActivityManager.class, "PROCESS_STATE_", callingUidProcState2) + "; isCallingUidPersistentSystemProcess: " + isCallingUidPersistentSystemProcess4 + "; realCallingUid: " + realCallingUid + "; isRealCallingUidForeground: " + isRealCallingUidForeground + "; realCallingUidHasAnyVisibleWindow: " + realCallingUidHasAnyVisibleWindow3 + "; realCallingUidProcState: " + DebugUtils.valueToString(ActivityManager.class, "PROCESS_STATE_", realCallingUidProcState3) + "; isRealCallingUidPersistentSystemProcess: " + isRealCallingUidPersistentSystemProcess + "; originatingPendingIntent: " + originatingPendingIntent + "; allowBackgroundActivityStart: " + allowBackgroundActivityStart + "; intent: " + intent + "; callerApp: " + callerApp3 + "; inVisibleTask: " + (callerApp3 != null && callerApp3.hasActivityInVisibleTask()) + "]");
                        if (this.mService.isActivityStartsLoggingEnabled()) {
                            this.mSupervisor.getActivityMetricsLogger().logAbortedBgActivityStart(intent, callerApp3, callingUid, callingPackage, callingUidProcState2, callingUidHasAnyVisibleWindow, realCallingUid, realCallingUidProcState3, realCallingUidHasAnyVisibleWindow3, originatingPendingIntent != null);
                        }
                        return true;
                    }
                    return false;
                }
                if (ActivityTaskManagerDebugConfig.DEBUG_ACTIVITY_STARTS) {
                    Slog.d(TAG, "Activity start allowed: callingUidHasAnyVisibleWindow = " + callingUid + ", isCallingUidPersistentSystemProcess = " + isCallingUidPersistentSystemProcess);
                }
                return false;
            }
        }
        z = true;
        boolean allowCallingUidStartActivity2 = z;
        if (useCallingUidState) {
        }
        boolean isCallingUidForeground22 = isCallingUidForeground;
        if (callingUid != realCallingUid) {
        }
        if (callingUid != realCallingUid) {
        }
        if (callingUid != realCallingUid) {
        }
        int realCallingAppId2 = UserHandle.getAppId(realCallingUid);
        if (callingUid != realCallingUid) {
        }
        if (Process.isSdkSandboxUid(realCallingUid)) {
        }
        balAllowedByPiSender = PendingIntentRecord.isPendingIntentBalAllowedByCaller(checkedOptions);
        if (balAllowedByPiSender) {
        }
        isCallingUidPersistentSystemProcess2 = isCallingUidPersistentSystemProcess;
        if (useCallingUidState) {
        }
        if (callerApp != null) {
        }
        callerApp2 = callerApp;
        callerAppUid = callingUid;
        if (callerApp2 != null) {
        }
        realCallingUidHasAnyVisibleWindow2 = realCallingUidHasAnyVisibleWindow;
        boolean realCallingUidHasAnyVisibleWindow32 = realCallingUidHasAnyVisibleWindow2;
        boolean isRealCallingUidPersistentSystemProcess2 = isRealCallingUidForeground2;
        callingUidProcState2 = callingUidProcState;
        boolean isCallingUidPersistentSystemProcess42 = isCallingUidPersistentSystemProcess2;
        WindowProcessController callerApp32 = callerApp2;
        if (ITranWindowManagerService.Instance().onShouldAbortBackgroundActivityStart(this, callerApp2, callingUid, callingPid, callingUidProcState2, this.mService.getProcessController(realCallingPid, realCallingUid), realCallingUid, realCallingPid, realCallingUidProcState2)) {
        }
    }

    private Intent createLaunchIntent(AuxiliaryResolveInfo auxiliaryResponse, Intent originalIntent, String callingPackage, String callingFeatureId, Bundle verificationBundle, String resolvedType, int userId) {
        Intent intent;
        ComponentName componentName;
        String str;
        if (auxiliaryResponse != null && auxiliaryResponse.needsPhaseTwo) {
            PackageManagerInternal packageManager = this.mService.getPackageManagerInternalLocked();
            boolean isRequesterInstantApp = packageManager.isInstantApp(callingPackage, userId);
            packageManager.requestInstantAppResolutionPhaseTwo(auxiliaryResponse, originalIntent, resolvedType, callingPackage, callingFeatureId, isRequesterInstantApp, verificationBundle, userId);
        }
        Intent sanitizeIntent = InstantAppResolver.sanitizeIntent(originalIntent);
        List list = null;
        if (auxiliaryResponse != null) {
            intent = auxiliaryResponse.failureIntent;
        } else {
            intent = null;
        }
        if (auxiliaryResponse != null) {
            componentName = auxiliaryResponse.installFailureActivity;
        } else {
            componentName = null;
        }
        if (auxiliaryResponse != null) {
            str = auxiliaryResponse.token;
        } else {
            str = null;
        }
        boolean z = auxiliaryResponse != null && auxiliaryResponse.needsPhaseTwo;
        if (auxiliaryResponse != null) {
            list = auxiliaryResponse.filters;
        }
        return InstantAppResolver.buildEphemeralInstallerIntent(originalIntent, sanitizeIntent, intent, callingPackage, callingFeatureId, verificationBundle, resolvedType, userId, componentName, str, z, list);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void postStartActivityProcessing(ActivityRecord r, int result, Task startedActivityRootTask) {
        Task targetTask;
        if (!ActivityManager.isStartResultSuccessful(result) && this.mFrozeTaskList) {
            this.mSupervisor.mRecentTasks.resetFreezeTaskListReorderingOnTimeout();
        }
        if (ActivityManager.isStartResultFatalError(result)) {
            return;
        }
        this.mSupervisor.reportWaitingActivityLaunchedIfNeeded(r, result);
        if (r.getTask() != null) {
            targetTask = r.getTask();
        } else {
            targetTask = this.mTargetTask;
        }
        if (startedActivityRootTask == null || targetTask == null || !targetTask.isAttached()) {
            return;
        }
        if (result == 2 || result == 3) {
            if (targetTask.getDisplayArea() == null) {
                Slog.w(TAG, "targetTask DisplayArea has been removed, do nothing");
                return;
            }
            Task rootHomeTask = targetTask.getDisplayArea().getRootHomeTask();
            boolean visible = true;
            boolean homeTaskVisible = rootHomeTask != null && rootHomeTask.shouldBeVisible(null);
            ActivityRecord top = targetTask.getTopNonFinishingActivity();
            if (top == null || !top.isVisible()) {
                visible = false;
            }
            this.mService.getTaskChangeNotificationController().notifyActivityRestartAttempt(targetTask.getTaskInfo(), homeTaskVisible, this.mIsTaskCleared, visible);
        }
        if (ActivityManager.isStartResultSuccessful(result)) {
            this.mInterceptor.onActivityLaunched(targetTask.getTaskInfo(), r);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static int computeResolveFilterUid(int customCallingUid, int actualCallingUid, int filterCallingUid) {
        if (filterCallingUid != -10000) {
            return filterCallingUid;
        }
        return customCallingUid >= 0 ? customCallingUid : actualCallingUid;
    }

    private int startActivityUnchecked(ActivityRecord r, ActivityRecord sourceRecord, IVoiceInteractionSession voiceSession, IVoiceInteractor voiceInteractor, int startFlags, boolean doResume, ActivityOptions options, Task inTask, TaskFragment inTaskFragment, boolean restrictedBgActivity, NeededUriGrants intentGrants) {
        TransitionController transitionController = r.mTransitionController;
        Transition newTransition = (transitionController.isCollecting() || transitionController.getTransitionPlayer() == null) ? null : transitionController.createTransition(1);
        RemoteTransition remoteTransition = r.takeRemoteTransition();
        if (newTransition != null && remoteTransition != null) {
            newTransition.setRemoteTransition(remoteTransition);
        }
        transitionController.collect(r);
        try {
            this.mService.deferWindowLayout();
            Trace.traceBegin(32L, "startActivityInner");
            int result = startActivityInner(r, sourceRecord, voiceSession, voiceInteractor, startFlags, doResume, options, inTask, inTaskFragment, restrictedBgActivity, intentGrants);
            Trace.traceEnd(32L);
            Task startedActivityRootTask = handleStartResult(r, options, result, newTransition, remoteTransition);
            this.mService.continueWindowLayout();
            ITranWindowManagerService.Instance().handleStartActivityResult(result, startedActivityRootTask, r, options);
            ITranWindowManagerService.Instance().onStartActivityUnchecked(this.mStartActivity);
            postStartActivityProcessing(r, result, startedActivityRootTask);
            TranFoldWMCustody.instance().onStartActivityUnchecked(result, r);
            return result;
        } catch (Throwable th) {
            Trace.traceEnd(32L);
            handleStartResult(r, options, -96, newTransition, remoteTransition);
            this.mService.continueWindowLayout();
            throw th;
        }
    }

    private Task handleStartResult(ActivityRecord started, ActivityOptions options, int result, Transition newTransition, RemoteTransition remoteTransition) {
        StatusBarManagerInternal statusBar;
        this.mSupervisor.mUserLeaving = false;
        Task currentRootTask = started.getRootTask();
        Task startedActivityRootTask = currentRootTask != null ? currentRootTask : this.mTargetRootTask;
        boolean isStarted = true;
        if (!ActivityManager.isStartResultSuccessful(result) || startedActivityRootTask == null) {
            if (this.mStartActivity.getTask() != null) {
                this.mStartActivity.finishIfPossible("startActivity", true);
            } else if (this.mStartActivity.getParent() != null) {
                this.mStartActivity.getParent().removeChild(this.mStartActivity);
            }
            if (startedActivityRootTask != null && startedActivityRootTask.isAttached() && !startedActivityRootTask.hasActivity() && !startedActivityRootTask.isActivityTypeHome()) {
                startedActivityRootTask.removeIfPossible("handleStartResult");
            }
            if (newTransition != null) {
                newTransition.abort();
            }
            return null;
        }
        if (options != null && options.getTaskAlwaysOnTop()) {
            startedActivityRootTask.setAlwaysOnTop(true);
        }
        ActivityRecord currentTop = startedActivityRootTask.topRunningActivity();
        if (currentTop != null && currentTop.shouldUpdateConfigForDisplayChanged()) {
            this.mRootWindowContainer.ensureVisibilityAndConfig(currentTop, currentTop.getDisplayId(), true, false);
        }
        if (!this.mAvoidMoveToFront && this.mDoResume && this.mRootWindowContainer.hasVisibleWindowAboveButDoesNotOwnNotificationShade(started.launchedFromUid) && (statusBar = this.mService.getStatusBarManagerInternal()) != null) {
            statusBar.collapsePanels();
        }
        TransitionController transitionController = started.mTransitionController;
        if (result != 0 && result != 2) {
            isStarted = false;
        }
        if (isStarted) {
            transitionController.collectExistenceChange(started);
        } else if (result == 3 && newTransition != null) {
            newTransition.abort();
            newTransition = null;
        }
        if (options != null && options.getTransientLaunch()) {
            transitionController.setTransientLaunch(this.mLastStartActivityRecord, this.mPriorAboveTask);
        }
        if (newTransition != null) {
            Task task = this.mTargetTask;
            if (task == null) {
                task = started.getTask();
            }
            transitionController.requestStartTransition(newTransition, task, remoteTransition, null);
        } else if (isStarted) {
            transitionController.setReady(started, false);
        }
        return startedActivityRootTask;
    }

    int startActivityInner(ActivityRecord r, ActivityRecord sourceRecord, IVoiceInteractionSession voiceSession, IVoiceInteractor voiceInteractor, int startFlags, boolean doResume, ActivityOptions options, Task inTask, TaskFragment inTaskFragment, boolean restrictedBgActivity, NeededUriGrants intentGrants) {
        boolean dreamStopping;
        Task reusedTask;
        Task targetTask;
        ActivityRecord activityRecord;
        setInitialState(r, options, inTask, inTaskFragment, doResume, startFlags, sourceRecord, voiceSession, voiceInteractor, restrictedBgActivity);
        computeLaunchingTaskFlags();
        computeSourceRootTask();
        this.mIntent.setFlags(this.mLaunchFlags);
        Iterator<ActivityRecord> it = this.mSupervisor.mStoppingActivities.iterator();
        while (true) {
            if (!it.hasNext()) {
                dreamStopping = false;
                break;
            }
            ActivityRecord stoppingActivity = it.next();
            if (stoppingActivity.getActivityType() == 5) {
                dreamStopping = true;
                break;
            }
        }
        Task prevTopRootTask = this.mPreferredTaskDisplayArea.getFocusedRootTask();
        Task prevTopTask = prevTopRootTask != null ? prevTopRootTask.getTopLeafTask() : null;
        Task reusedTask2 = getReusableTask();
        if (!SplitScreenHelper.removeReuseTaskIfNeed(sourceRecord, reusedTask2)) {
            reusedTask = reusedTask2;
        } else {
            reusedTask = null;
        }
        ActivityOptions activityOptions = this.mOptions;
        if (activityOptions != null && activityOptions.freezeRecentTasksReordering() && this.mSupervisor.mRecentTasks.isCallerRecents(r.launchedFromUid) && !this.mSupervisor.mRecentTasks.isFreezeTaskListReorderingSet()) {
            this.mFrozeTaskList = true;
            this.mSupervisor.mRecentTasks.setFreezeTaskListReordering();
        }
        Task targetTask2 = reusedTask != null ? reusedTask : computeTargetTask();
        boolean newTask = targetTask2 == null;
        this.mTargetTask = targetTask2;
        if (ITranActivityManagerService.Instance().isAppLaunchEnable() && this.mIsAppLaunch) {
            this.mRecentIndex = this.mSupervisor.mRecentTasks.getTaskIndex(this.mTargetTask);
        }
        computeLaunchParams(r, sourceRecord, targetTask2);
        int startResult = isAllowedToStart(r, newTask, targetTask2);
        if (startResult != 0) {
            if (r.resultTo != null) {
                r.resultTo.sendResult(-1, r.resultWho, r.requestCode, 0, null, null);
            }
            return startResult;
        }
        if (targetTask2 != null) {
            this.mPriorAboveTask = TaskDisplayArea.getRootTaskAbove(targetTask2.getRootTask());
        }
        if (ThunderbackConfig.isVersion3()) {
            if (ITranActivityStarter.Instance().needHookMultiWindowToMax(TAG, this.mRootWindowContainer.getMulitWindowTopPackage(), sourceRecord, r)) {
                r.setAlreadyLaunchered(true);
                return 0;
            }
        } else if (ITranActivityStarter.Instance().needHookReparentToDefaultDisplay(TAG, sourceRecord, r, r.mUserId)) {
            r.setAlreadyLaunchered(true);
            return 0;
        } else {
            TaskDisplayArea taskDisplayArea = this.mPreferredTaskDisplayArea;
            if (taskDisplayArea != null && taskDisplayArea.isMultiWindow() && (taskDisplayArea.getMultiWindowingMode() & 64) != 0) {
                ITranTaskLaunchParamsModifier.Instance().hookMultiWindowToLargeV4(taskDisplayArea.getMultiWindowingMode(), taskDisplayArea.getMultiWindowingId());
            }
        }
        ActivityRecord targetTaskTop = newTask ? null : targetTask2.getTopNonFinishingActivity();
        if (targetTaskTop != null) {
            startResult = recycleTask(targetTask2, targetTaskTop, reusedTask, intentGrants);
            if (startResult != 0) {
                return startResult;
            }
        } else {
            this.mAddingToTask = true;
        }
        Task topRootTask = this.mPreferredTaskDisplayArea.getFocusedRootTask();
        if (topRootTask != null && (options == null || !options.getLaunchedFromBubble() || topRootTask.topRunningActivity() == null || !"com.transsion.applock".equals(topRootTask.topRunningActivity().packageName) || !"com.transsion.applock".equals(r.packageName))) {
            int startResult2 = deliverToCurrentTopIfNeeded(topRootTask, intentGrants);
            if (startResult2 != 0) {
                return startResult2;
            }
        }
        if (this.mTargetRootTask == null) {
            this.mTargetRootTask = getOrCreateRootTask(this.mStartActivity, this.mLaunchFlags, targetTask2, this.mOptions);
        }
        if (newTask) {
            Task taskToAffiliate = (!this.mLaunchTaskBehind || (activityRecord = this.mSourceRecord) == null) ? null : activityRecord.getTask();
            setNewTask(taskToAffiliate);
        } else if (this.mAddingToTask) {
            addOrReparentStartingActivity(targetTask2, "adding to task");
        }
        if (!this.mAvoidMoveToFront && this.mDoResume) {
            this.mTargetRootTask.getRootTask().moveToFront("reuseOrNewTask", targetTask2);
            if (!this.mTargetRootTask.isTopRootTaskInDisplayArea() && this.mService.isDreaming() && !dreamStopping) {
                this.mLaunchTaskBehind = true;
                r.mLaunchTaskBehind = true;
            }
        }
        this.mService.mUgmInternal.grantUriPermissionUncheckedFromIntent(intentGrants, this.mStartActivity.getUriPermissionsLocked());
        if (this.mStartActivity.resultTo == null || this.mStartActivity.resultTo.info == null) {
            targetTask = targetTask2;
        } else {
            PackageManagerInternal pmInternal = this.mService.getPackageManagerInternalLocked();
            targetTask = targetTask2;
            int resultToUid = pmInternal.getPackageUid(this.mStartActivity.resultTo.info.packageName, 0L, this.mStartActivity.mUserId);
            pmInternal.grantImplicitAccess(this.mStartActivity.mUserId, this.mIntent, UserHandle.getAppId(this.mStartActivity.info.applicationInfo.uid), resultToUid, true);
        }
        Task startedTask = this.mStartActivity.getTask();
        if (newTask) {
            EventLogTags.writeWmCreateTask(this.mStartActivity.mUserId, startedTask.mTaskId);
        }
        this.mStartActivity.logStartActivity(EventLogTags.WM_CREATE_ACTIVITY, startedTask);
        this.mStartActivity.getTaskFragment().clearLastPausedActivity();
        this.mRootWindowContainer.startPowerModeLaunchIfNeeded(false, this.mStartActivity);
        boolean isTaskSwitch = (startedTask == prevTopTask || startedTask.isEmbedded()) ? false : true;
        this.mTargetRootTask.startActivityLocked(this.mStartActivity, topRootTask, newTask, isTaskSwitch, this.mOptions, sourceRecord);
        if (this.mDoResume) {
            ActivityRecord topTaskActivity = startedTask.topRunningActivityLocked();
            if (!this.mTargetRootTask.isTopActivityFocusable() || (topTaskActivity != null && topTaskActivity.isTaskOverlay() && this.mStartActivity != topTaskActivity)) {
                this.mTargetRootTask.ensureActivitiesVisible(null, 0, false);
                this.mTargetRootTask.mDisplayContent.executeAppTransition();
            } else {
                if (this.mTargetRootTask.isTopActivityFocusable() && !this.mRootWindowContainer.isTopDisplayFocusedRootTask(this.mTargetRootTask)) {
                    this.mTargetRootTask.moveToFront("startActivityInner");
                }
                this.mRootWindowContainer.resumeFocusedTasksTopActivities(this.mTargetRootTask, this.mStartActivity, this.mOptions, this.mTransientLaunch);
            }
        }
        this.mRootWindowContainer.updateUserRootTask(this.mStartActivity.mUserId, this.mTargetRootTask);
        this.mSupervisor.mRecentTasks.add(startedTask);
        this.mSupervisor.handleNonResizableTaskIfNeeded(startedTask, this.mPreferredWindowingMode, this.mPreferredTaskDisplayArea, this.mTargetRootTask);
        ActivityOptions activityOptions2 = this.mOptions;
        if (activityOptions2 != null && activityOptions2.isLaunchIntoPip() && sourceRecord != null && sourceRecord.getTask() == this.mStartActivity.getTask() && !this.mRestrictedBgActivity) {
            this.mRootWindowContainer.moveActivityToPinnedRootTask(this.mStartActivity, sourceRecord, "launch-into-pip");
        }
        return 0;
    }

    private Task computeTargetTask() {
        boolean inMultiWindow = false;
        String pkgName = ITranMultiWindow.Instance().getReadyStartInMultiWindowPackageName();
        int starType = ITranMultiWindow.Instance().getReadyStartInMultiWindowPackageStartType();
        if (pkgName != null && pkgName.equals(this.mStartActivity.packageName) && starType == 5) {
            inMultiWindow = true;
        }
        if (this.mStartActivity.resultTo != null || this.mInTask != null || this.mAddingToTask || (this.mLaunchFlags & 268435456) == 0) {
            if (this.mSourceRecord != null) {
                if (inMultiWindow && !ITranActivityStarter.Instance().inMultiWindow(this.mSourceRecord)) {
                    ITranMultiWindow.Instance().resetReadyStartInMultiWindowPackage();
                    ITranActivityStarter.Instance().showSceneUnSupportMultiToast();
                }
                return this.mSourceRecord.getTask();
            }
            Task task = this.mInTask;
            if (task != null) {
                if (!task.isAttached()) {
                    getOrCreateRootTask(this.mStartActivity, this.mLaunchFlags, this.mInTask, this.mOptions);
                }
                if (inMultiWindow && !this.mInTask.getConfiguration().windowConfiguration.isThunderbackWindow()) {
                    ITranMultiWindow.Instance().resetReadyStartInMultiWindowPackage();
                    ITranActivityStarter.Instance().showSceneUnSupportMultiToast();
                }
                return this.mInTask;
            }
            Task rootTask = getOrCreateRootTask(this.mStartActivity, this.mLaunchFlags, null, this.mOptions);
            ActivityRecord top = rootTask.getTopNonFinishingActivity();
            if (top != null) {
                return top.getTask();
            }
            rootTask.removeIfPossible("computeTargetTask");
            return null;
        }
        return null;
    }

    private void computeLaunchParams(ActivityRecord r, ActivityRecord sourceRecord, Task targetTask) {
        TaskDisplayArea defaultTaskDisplayArea;
        this.mSupervisor.getLaunchParamsController().calculate(targetTask, r.info.windowLayout, r, sourceRecord, this.mOptions, this.mRequest, 3, this.mLaunchParams);
        if (this.mLaunchParams.hasPreferredTaskDisplayArea()) {
            defaultTaskDisplayArea = this.mLaunchParams.mPreferredTaskDisplayArea;
        } else {
            defaultTaskDisplayArea = this.mRootWindowContainer.getDefaultTaskDisplayArea();
        }
        this.mPreferredTaskDisplayArea = defaultTaskDisplayArea;
        this.mPreferredWindowingMode = this.mLaunchParams.mWindowingMode;
    }

    int isAllowedToStart(ActivityRecord r, boolean newTask, Task targetTask) {
        DisplayContent displayContent;
        if (r.packageName == null) {
            ActivityOptions.abort(this.mOptions);
            return -92;
        }
        if (r.isActivityTypeHome() && !this.mRootWindowContainer.canStartHomeOnDisplayArea(r.info, this.mPreferredTaskDisplayArea, true)) {
            Slog.w(TAG, "Cannot launch home on display area " + this.mPreferredTaskDisplayArea);
            return -96;
        }
        boolean blockBalInTask = newTask || !targetTask.isUidPresent(this.mCallingUid) || (3 == this.mLaunchMode && targetTask.inPinnedWindowingMode());
        if (this.mRestrictedBgActivity && blockBalInTask && handleBackgroundActivityAbort(this.mStartActivity) && !this.mRequest.intent.getBooleanExtra("HAS_RUN_INTERCEPT_UNKNOWN_SOURCE_APK", false)) {
            Slog.e(TAG, "Abort background activity starts from " + this.mCallingUid);
            return 102;
        }
        boolean isNewClearTask = (this.mLaunchFlags & 268468224) == 268468224;
        if (!newTask) {
            if (this.mService.getLockTaskController().isLockTaskModeViolation(targetTask, isNewClearTask)) {
                Slog.e(TAG, "Attempted Lock Task Mode violation r=" + r);
                return 101;
            }
        } else if (this.mService.getLockTaskController().isNewTaskLockTaskModeViolation(r)) {
            Slog.e(TAG, "Attempted Lock Task Mode violation r=" + r);
            return 101;
        }
        TaskDisplayArea taskDisplayArea = this.mPreferredTaskDisplayArea;
        if (taskDisplayArea != null && (displayContent = this.mRootWindowContainer.getDisplayContentOrCreate(taskDisplayArea.getDisplayId())) != null && displayContent.mDwpcHelper.hasController()) {
            int targetWindowingMode = targetTask != null ? targetTask.getWindowingMode() : displayContent.getWindowingMode();
            ActivityRecord activityRecord = this.mSourceRecord;
            int launchingFromDisplayId = activityRecord != null ? activityRecord.getDisplayId() : 0;
            if (!displayContent.mDwpcHelper.canActivityBeLaunched(r.info, targetWindowingMode, launchingFromDisplayId, newTask)) {
                Slog.w(TAG, "Abort to launch " + r.info.getComponentName() + " on display area " + this.mPreferredTaskDisplayArea);
                return 102;
            }
        }
        return 0;
    }

    static int canEmbedActivity(TaskFragment taskFragment, ActivityRecord starting, Task targetTask) {
        Task hostTask = taskFragment.getTask();
        if (hostTask == null || targetTask != hostTask) {
            return 3;
        }
        return taskFragment.isAllowedToEmbedActivity(starting);
    }

    int recycleTask(Task targetTask, ActivityRecord targetTaskTop, Task reusedTask, NeededUriGrants intentGrants) {
        ActivityRecord activityRecord;
        if (targetTask.mUserId != this.mStartActivity.mUserId) {
            this.mTargetRootTask = targetTask.getRootTask();
            this.mAddingToTask = true;
            return 0;
        }
        if (reusedTask != null) {
            if (targetTask.intent == null) {
                targetTask.setIntent(this.mStartActivity);
            } else {
                boolean taskOnHome = (this.mStartActivity.intent.getFlags() & 16384) != 0;
                if (taskOnHome) {
                    targetTask.intent.addFlags(16384);
                } else {
                    targetTask.intent.removeFlags(16384);
                }
            }
        }
        this.mRootWindowContainer.startPowerModeLaunchIfNeeded(false, targetTaskTop);
        setTargetRootTaskIfNeeded(targetTaskTop);
        ActivityRecord activityRecord2 = this.mLastStartActivityRecord;
        if (activityRecord2 != null && (activityRecord2.finishing || this.mLastStartActivityRecord.noDisplay)) {
            this.mLastStartActivityRecord = targetTaskTop;
        }
        if ((this.mStartFlags & 1) != 0) {
            if (!this.mMovedToFront && this.mDoResume) {
                if (ProtoLogCache.WM_DEBUG_TASKS_enabled) {
                    String protoLogParam0 = String.valueOf(this.mTargetRootTask);
                    String protoLogParam1 = String.valueOf(targetTaskTop);
                    ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_TASKS, -1585311008, 0, (String) null, new Object[]{protoLogParam0, protoLogParam1});
                }
                this.mTargetRootTask.moveToFront("intentActivityFound");
            }
            resumeTargetRootTaskIfNeeded();
            return 1;
        }
        complyActivityFlags(targetTask, reusedTask != null ? reusedTask.getTopNonFinishingActivity() : null, intentGrants);
        if (this.mAddingToTask) {
            return 0;
        }
        if (targetTaskTop.finishing) {
            activityRecord = targetTask.getTopNonFinishingActivity();
        } else {
            activityRecord = targetTaskTop;
        }
        ActivityRecord targetTaskTop2 = activityRecord;
        if (targetTaskTop2.canTurnScreenOn() && this.mService.isDreaming()) {
            targetTaskTop2.mTaskSupervisor.wakeUp("recycleTask#turnScreenOnFlag");
        }
        if (this.mMovedToFront) {
            if (targetTaskTop2.getTask() != null) {
                targetTaskTop2.showStartingWindow(true);
            } else {
                Slog.w(TAG, "targetTaskTop.getTask() is null, targetTaskTop = " + targetTaskTop2);
            }
        } else if (this.mDoResume) {
            this.mTargetRootTask.moveToFront("intentActivityFound");
        }
        resumeTargetRootTaskIfNeeded();
        this.mLastStartActivityRecord = targetTaskTop2;
        return this.mMovedToFront ? 2 : 3;
    }

    private int deliverToCurrentTopIfNeeded(Task topRootTask, NeededUriGrants intentGrants) {
        ActivityRecord top = topRootTask.topRunningNonDelayedActivityLocked(this.mNotTop);
        boolean dontStart = top != null && top.mActivityComponent.equals(this.mStartActivity.mActivityComponent) && top.mUserId == this.mStartActivity.mUserId && top.attachedToProcess() && ((this.mLaunchFlags & 536870912) != 0 || 1 == this.mLaunchMode) && (!top.isActivityTypeHome() || top.getDisplayArea() == this.mPreferredTaskDisplayArea);
        if (!dontStart) {
            return 0;
        }
        top.getTaskFragment().clearLastPausedActivity();
        if (this.mDoResume) {
            this.mRootWindowContainer.resumeFocusedTasksTopActivities();
        }
        ActivityOptions.abort(this.mOptions);
        if ((this.mStartFlags & 1) != 0) {
            return 1;
        }
        if (this.mStartActivity.resultTo != null) {
            this.mStartActivity.resultTo.sendResult(-1, this.mStartActivity.resultWho, this.mStartActivity.requestCode, 0, null, null);
            this.mStartActivity.resultTo = null;
        }
        deliverNewIntent(top, intentGrants);
        this.mSupervisor.handleNonResizableTaskIfNeeded(top.getTask(), this.mLaunchParams.mWindowingMode, this.mPreferredTaskDisplayArea, topRootTask);
        return 3;
    }

    private void complyActivityFlags(Task targetTask, ActivityRecord reusedActivity, NeededUriGrants intentGrants) {
        ActivityRecord targetTaskTop = targetTask.getTopNonFinishingActivity();
        boolean resetTask = (reusedActivity == null || (this.mLaunchFlags & 2097152) == 0) ? false : true;
        if (resetTask) {
            targetTaskTop = this.mTargetRootTask.resetTaskIfNeeded(targetTaskTop, this.mStartActivity);
        }
        int i = this.mLaunchFlags;
        if ((i & 268468224) == 268468224) {
            targetTask.performClearTaskForReuse(true);
            targetTask.setIntent(this.mStartActivity);
            this.mAddingToTask = true;
            this.mIsTaskCleared = true;
        } else if ((i & 67108864) != 0 || isDocumentLaunchesIntoExisting(i) || isLaunchModeOneOf(3, 2, 4)) {
            ActivityRecord clearTop = targetTask.performClearTop(this.mStartActivity, this.mLaunchFlags);
            if (clearTop != null && !clearTop.finishing) {
                if (clearTop.isRootOfTask()) {
                    clearTop.getTask().setIntent(this.mStartActivity);
                }
                deliverNewIntent(clearTop, intentGrants);
                return;
            }
            this.mAddingToTask = true;
            if (clearTop != null && clearTop.getTaskFragment() != null && clearTop.getTaskFragment().isEmbedded()) {
                this.mAddingToTaskFragment = clearTop.getTaskFragment();
            }
            if (targetTask.getRootTask() == null) {
                Task orCreateRootTask = getOrCreateRootTask(this.mStartActivity, this.mLaunchFlags, null, this.mOptions);
                this.mTargetRootTask = orCreateRootTask;
                orCreateRootTask.addChild(targetTask, !this.mLaunchTaskBehind, (this.mStartActivity.info.flags & 1024) != 0);
            }
        } else {
            int i2 = this.mLaunchFlags;
            if ((i2 & 67108864) == 0 && !this.mAddingToTask && (i2 & 131072) != 0) {
                ActivityRecord act = targetTask.findActivityInHistory(this.mStartActivity.mActivityComponent);
                if (act != null) {
                    Task task = act.getTask();
                    task.moveActivityToFrontLocked(act);
                    act.updateOptionsLocked(this.mOptions);
                    deliverNewIntent(act, intentGrants);
                    act.getTaskFragment().clearLastPausedActivity();
                    return;
                }
                this.mAddingToTask = true;
            } else if (this.mStartActivity.mActivityComponent.equals(targetTask.realActivity)) {
                if (targetTask != this.mInTask) {
                    if (((this.mLaunchFlags & 536870912) != 0 || 1 == this.mLaunchMode) && targetTaskTop.mActivityComponent.equals(this.mStartActivity.mActivityComponent) && this.mStartActivity.resultTo == null) {
                        if (targetTaskTop.isRootOfTask()) {
                            targetTaskTop.getTask().setIntent(this.mStartActivity);
                        }
                        deliverNewIntent(targetTaskTop, intentGrants);
                    } else if (!targetTask.isSameIntentFilter(this.mStartActivity)) {
                        this.mAddingToTask = true;
                    } else if (reusedActivity == null) {
                        this.mAddingToTask = true;
                    }
                }
            } else if (!resetTask) {
                this.mAddingToTask = true;
            } else if (!targetTask.rootWasReset) {
                targetTask.setIntent(this.mStartActivity);
            }
        }
    }

    void reset(boolean clearRequest) {
        this.mStartActivity = null;
        this.mIntent = null;
        this.mCallingUid = -1;
        this.mOptions = null;
        this.mRestrictedBgActivity = false;
        this.mLaunchTaskBehind = false;
        this.mLaunchFlags = 0;
        this.mLaunchMode = -1;
        this.mLaunchParams.reset();
        this.mNotTop = null;
        this.mDoResume = false;
        this.mStartFlags = 0;
        this.mSourceRecord = null;
        this.mPreferredTaskDisplayArea = null;
        this.mPreferredWindowingMode = 0;
        this.mInTask = null;
        this.mInTaskFragment = null;
        this.mAddingToTaskFragment = null;
        this.mAddingToTask = false;
        this.mNewTaskInfo = null;
        this.mNewTaskIntent = null;
        this.mSourceRootTask = null;
        this.mTargetRootTask = null;
        this.mTargetTask = null;
        this.mIsTaskCleared = false;
        this.mMovedToFront = false;
        this.mNoAnimation = false;
        this.mAvoidMoveToFront = false;
        this.mFrozeTaskList = false;
        this.mTransientLaunch = false;
        this.mVoiceSession = null;
        this.mVoiceInteractor = null;
        this.mIntentDelivered = false;
        if (clearRequest) {
            this.mRequest.reset();
        }
    }

    /* JADX WARN: Removed duplicated region for block: B:106:0x021d  */
    /* JADX WARN: Removed duplicated region for block: B:107:0x021f  */
    /* JADX WARN: Removed duplicated region for block: B:86:0x01c5  */
    /* JADX WARN: Removed duplicated region for block: B:87:0x01c7  */
    /* JADX WARN: Removed duplicated region for block: B:95:0x01f2  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private void setInitialState(ActivityRecord r, ActivityOptions options, Task inTask, TaskFragment inTaskFragment, boolean doResume, int startFlags, ActivityRecord sourceRecord, IVoiceInteractionSession voiceSession, IVoiceInteractor voiceInteractor, boolean restrictedBgActivity) {
        TaskDisplayArea defaultTaskDisplayArea;
        TaskFragment inTaskFragment2;
        Task topFocusedRootTask;
        reset(false);
        this.mStartActivity = r;
        this.mIntent = r.intent;
        this.mOptions = options;
        this.mCallingUid = r.launchedFromUid;
        this.mSourceRecord = sourceRecord;
        this.mVoiceSession = voiceSession;
        this.mVoiceInteractor = voiceInteractor;
        this.mRestrictedBgActivity = restrictedBgActivity;
        this.mLaunchParams.reset();
        this.mSupervisor.getLaunchParamsController().calculate(inTask, r.info.windowLayout, r, sourceRecord, options, this.mRequest, 0, this.mLaunchParams);
        if (this.mLaunchParams.hasPreferredTaskDisplayArea()) {
            defaultTaskDisplayArea = this.mLaunchParams.mPreferredTaskDisplayArea;
        } else {
            defaultTaskDisplayArea = this.mRootWindowContainer.getDefaultTaskDisplayArea();
        }
        this.mPreferredTaskDisplayArea = defaultTaskDisplayArea;
        this.mPreferredWindowingMode = this.mLaunchParams.mWindowingMode;
        int i = r.launchMode;
        this.mLaunchMode = i;
        this.mLaunchFlags = adjustLaunchFlagsToDocumentMode(r, 3 == i, 2 == i, this.mIntent.getFlags());
        if (sourceRecord != null && "com.skype.raider".equals(sourceRecord.packageName)) {
            this.mLaunchFlags &= -4097;
        }
        this.mLaunchTaskBehind = (!r.mLaunchTaskBehind || isLaunchModeOneOf(2, 3) || (this.mLaunchFlags & 524288) == 0) ? false : true;
        if (this.mLaunchMode == 4) {
            this.mLaunchFlags |= 268435456;
        }
        sendNewTaskResultRequestIfNeeded();
        if ((this.mLaunchFlags & 524288) != 0 && r.resultTo == null) {
            this.mLaunchFlags |= 268435456;
        }
        if ((this.mLaunchFlags & 268435456) != 0 && (this.mLaunchTaskBehind || r.info.documentLaunchMode == 2)) {
            this.mLaunchFlags |= 134217728;
        }
        this.mSupervisor.mUserLeaving = (this.mLaunchFlags & 262144) == 0;
        if (ActivityTaskManagerDebugConfig.DEBUG_USER_LEAVING) {
            Slog.v(TAG_USER_LEAVING, "startActivity() => mUserLeaving=" + this.mSupervisor.mUserLeaving);
        }
        this.mDoResume = doResume;
        if (!doResume || !r.showToCurrentUser() || this.mLaunchTaskBehind) {
            r.delayedResume = true;
            this.mDoResume = false;
        }
        ActivityOptions activityOptions = this.mOptions;
        if (activityOptions != null) {
            if (activityOptions.getLaunchTaskId() != -1 && this.mOptions.getTaskOverlay()) {
                r.setTaskOverlay(true);
                if (!this.mOptions.canTaskOverlayResume()) {
                    Task task = this.mRootWindowContainer.anyTaskForId(this.mOptions.getLaunchTaskId());
                    ActivityRecord top = task != null ? task.getTopNonFinishingActivity() : null;
                    if (top != null && !top.isState(ActivityRecord.State.RESUMED)) {
                        this.mDoResume = false;
                        this.mAvoidMoveToFront = true;
                    }
                }
            } else if (this.mOptions.getAvoidMoveToFront()) {
                this.mDoResume = false;
                this.mAvoidMoveToFront = true;
            }
            this.mTransientLaunch = this.mOptions.getTransientLaunch();
            this.mTargetRootTask = Task.fromWindowContainerToken(this.mOptions.getLaunchRootTask());
            if (inTaskFragment == null) {
                inTaskFragment2 = TaskFragment.fromTaskFragmentToken(this.mOptions.getLaunchTaskFragmentToken(), this.mService);
                if (inTaskFragment2 != null && inTaskFragment2.isEmbeddedTaskFragmentInPip()) {
                    Slog.w(TAG, "Can not start activity in TaskFragment in PIP: " + inTaskFragment2);
                    inTaskFragment2 = null;
                }
                this.mNotTop = (this.mLaunchFlags & 16777216) == 0 ? sourceRecord : null;
                this.mInTask = inTask;
                if (inTask != null && !inTask.inRecents) {
                    Slog.w(TAG, "Starting activity in task not in recents: " + inTask);
                    this.mInTask = null;
                }
                this.mInTaskFragment = inTaskFragment2;
                this.mStartFlags = startFlags;
                if ((startFlags & 1) != 0) {
                    ActivityRecord checkedCaller = sourceRecord;
                    if (checkedCaller == null && (topFocusedRootTask = this.mRootWindowContainer.getTopDisplayFocusedRootTask()) != null) {
                        checkedCaller = topFocusedRootTask.topRunningNonDelayedActivityLocked(this.mNotTop);
                    }
                    if (checkedCaller == null || !checkedCaller.mActivityComponent.equals(r.mActivityComponent)) {
                        this.mStartFlags &= -2;
                    }
                }
                this.mNoAnimation = (this.mLaunchFlags & 65536) == 0;
                if (!this.mRestrictedBgActivity && !this.mService.isBackgroundActivityStartsEnabled()) {
                    this.mAvoidMoveToFront = true;
                    this.mDoResume = false;
                    return;
                }
            }
        }
        inTaskFragment2 = inTaskFragment;
        this.mNotTop = (this.mLaunchFlags & 16777216) == 0 ? sourceRecord : null;
        this.mInTask = inTask;
        if (inTask != null) {
            Slog.w(TAG, "Starting activity in task not in recents: " + inTask);
            this.mInTask = null;
        }
        this.mInTaskFragment = inTaskFragment2;
        this.mStartFlags = startFlags;
        if ((startFlags & 1) != 0) {
        }
        this.mNoAnimation = (this.mLaunchFlags & 65536) == 0;
        if (!this.mRestrictedBgActivity) {
        }
    }

    private void sendNewTaskResultRequestIfNeeded() {
        if (this.mStartActivity.resultTo != null && (this.mLaunchFlags & 268435456) != 0) {
            Slog.w(TAG, "Activity is launching as a new task, so cancelling activity result.");
            this.mStartActivity.resultTo.sendResult(-1, this.mStartActivity.resultWho, this.mStartActivity.requestCode, 0, null, null);
            this.mStartActivity.resultTo = null;
        }
    }

    private void computeLaunchingTaskFlags() {
        ActivityRecord activityRecord;
        Task task;
        if (this.mSourceRecord == null && (task = this.mInTask) != null && task.getRootTask() != null) {
            Intent baseIntent = this.mInTask.getBaseIntent();
            ActivityRecord root = this.mInTask.getRootActivity();
            if (baseIntent == null) {
                ActivityOptions.abort(this.mOptions);
                throw new IllegalArgumentException("Launching into task without base intent: " + this.mInTask);
            }
            if (isLaunchModeOneOf(3, 2)) {
                if (!baseIntent.getComponent().equals(this.mStartActivity.intent.getComponent()) && ITranWindowManagerService.Instance().onComponentNameJudge(this.mStartActivity.intent.getComponent())) {
                    ActivityOptions.abort(this.mOptions);
                    throw new IllegalArgumentException("Trying to launch singleInstance/Task " + this.mStartActivity + " into different task " + this.mInTask);
                } else if (root != null) {
                    ActivityOptions.abort(this.mOptions);
                    throw new IllegalArgumentException("Caller with mInTask " + this.mInTask + " has root " + root + " but target is singleInstance/Task");
                }
            }
            if (root == null) {
                int flags = (this.mLaunchFlags & (-403185665)) | (baseIntent.getFlags() & 403185664);
                this.mLaunchFlags = flags;
                this.mIntent.setFlags(flags);
                this.mInTask.setIntent(this.mStartActivity);
                this.mAddingToTask = true;
            } else if ((this.mLaunchFlags & 268435456) != 0) {
                this.mAddingToTask = false;
            } else {
                this.mAddingToTask = true;
            }
        } else {
            this.mInTask = null;
            if ((this.mStartActivity.isResolverOrDelegateActivity() || this.mStartActivity.noDisplay) && (activityRecord = this.mSourceRecord) != null && activityRecord.inFreeformWindowingMode()) {
                this.mAddingToTask = true;
            }
        }
        Task task2 = this.mInTask;
        if (task2 == null) {
            ActivityRecord activityRecord2 = this.mSourceRecord;
            if (activityRecord2 == null) {
                if ((this.mLaunchFlags & 268435456) == 0 && task2 == null) {
                    Slog.w(TAG, "startActivity called from non-Activity context; forcing Intent.FLAG_ACTIVITY_NEW_TASK for: " + this.mIntent);
                    this.mLaunchFlags |= 268435456;
                }
            } else if (activityRecord2.launchMode == 3) {
                this.mLaunchFlags |= 268435456;
            } else if (isLaunchModeOneOf(3, 2)) {
                this.mLaunchFlags |= 268435456;
            }
        }
    }

    private void computeSourceRootTask() {
        ActivityRecord activityRecord = this.mSourceRecord;
        if (activityRecord == null) {
            this.mSourceRootTask = null;
        } else if (!activityRecord.finishing) {
            this.mSourceRootTask = this.mSourceRecord.getRootTask();
        } else {
            if ((this.mLaunchFlags & 268435456) == 0) {
                Slog.w(TAG, "startActivity called from finishing " + this.mSourceRecord + "; forcing Intent.FLAG_ACTIVITY_NEW_TASK for: " + this.mIntent);
                this.mLaunchFlags |= 268435456;
                Task sourceTask = this.mSourceRecord.getTask();
                if (sourceTask == null || sourceTask.getTopNonFinishingActivity() == null) {
                    this.mNewTaskInfo = this.mSourceRecord.info;
                    this.mNewTaskIntent = sourceTask != null ? sourceTask.intent : null;
                }
            }
            this.mSourceRecord = null;
            this.mSourceRootTask = null;
        }
    }

    private Task getReusableTask() {
        ActivityOptions activityOptions = this.mOptions;
        if (activityOptions != null && activityOptions.getLaunchTaskId() != -1) {
            Task launchTask = this.mRootWindowContainer.anyTaskForId(this.mOptions.getLaunchTaskId());
            if (launchTask != null) {
                return launchTask;
            }
            return null;
        }
        int i = this.mLaunchFlags;
        boolean putIntoExistingTask = ((268435456 & i) != 0 && (i & 134217728) == 0) || isLaunchModeOneOf(3, 2);
        ActivityRecord intentActivity = null;
        if (putIntoExistingTask & (this.mInTask == null && this.mStartActivity.resultTo == null)) {
            if (3 == this.mLaunchMode) {
                intentActivity = this.mRootWindowContainer.findActivity(this.mIntent, this.mStartActivity.info, this.mStartActivity.isActivityTypeHome());
            } else if ((this.mLaunchFlags & 4096) != 0) {
                intentActivity = this.mRootWindowContainer.findActivity(this.mIntent, this.mStartActivity.info, 2 != this.mLaunchMode);
            } else {
                intentActivity = this.mRootWindowContainer.findTask(this.mStartActivity, this.mPreferredTaskDisplayArea);
            }
        }
        if (intentActivity != null && this.mLaunchMode == 4 && !intentActivity.getTask().getRootActivity().mActivityComponent.equals(this.mStartActivity.mActivityComponent)) {
            intentActivity = null;
        }
        if (intentActivity != null && ((this.mStartActivity.isActivityTypeHome() || intentActivity.isActivityTypeHome()) && intentActivity.getDisplayArea() != this.mPreferredTaskDisplayArea)) {
            intentActivity = null;
        }
        if (intentActivity != null) {
            return intentActivity.getTask();
        }
        return null;
    }

    private void setTargetRootTaskIfNeeded(ActivityRecord intentActivity) {
        boolean differentTopTask;
        ActivityRecord activityRecord;
        intentActivity.getTaskFragment().clearLastPausedActivity();
        Task intentTask = intentActivity.getTask();
        Task task = this.mTargetRootTask;
        if (task == null) {
            ActivityRecord activityRecord2 = this.mSourceRecord;
            if (activityRecord2 != null && activityRecord2.mLaunchRootTask != null) {
                this.mTargetRootTask = Task.fromWindowContainerToken(this.mSourceRecord.mLaunchRootTask);
            } else {
                this.mTargetRootTask = getOrCreateRootTask(this.mStartActivity, this.mLaunchFlags, intentTask, this.mOptions);
            }
        } else {
            Task adjacentTargetTask = task.getAdjacentTaskFragment() != null ? this.mTargetRootTask.getAdjacentTaskFragment().asTask() : null;
            if (adjacentTargetTask != null && intentActivity.isDescendantOf(adjacentTargetTask)) {
                this.mTargetRootTask = adjacentTargetTask;
            }
        }
        if (this.mTargetRootTask.getDisplayArea() == this.mPreferredTaskDisplayArea) {
            Task focusRootTask = this.mTargetRootTask.mDisplayContent.getFocusedRootTask();
            ActivityRecord curTop = focusRootTask == null ? null : focusRootTask.topRunningNonDelayedActivityLocked(this.mNotTop);
            Task topTask = curTop != null ? curTop.getTask() : null;
            differentTopTask = (topTask == intentTask && (focusRootTask == null || topTask == focusRootTask.getTopMostTask())) ? false : true;
        } else {
            differentTopTask = true;
        }
        if (differentTopTask && !this.mAvoidMoveToFront) {
            this.mStartActivity.intent.addFlags(4194304);
            if (this.mSourceRecord == null || (this.mSourceRootTask.getTopNonFinishingActivity() != null && this.mSourceRootTask.getTopNonFinishingActivity().getTask() == this.mSourceRecord.getTask())) {
                if (this.mLaunchTaskBehind && (activityRecord = this.mSourceRecord) != null) {
                    intentActivity.setTaskToAffiliateWith(activityRecord.getTask());
                }
                if (intentActivity.isDescendantOf(this.mTargetRootTask)) {
                    Task task2 = this.mTargetRootTask;
                    if (task2 != intentTask && task2 != intentTask.getParent().asTask()) {
                        intentTask.getParent().positionChildAt(Integer.MAX_VALUE, intentTask, false);
                        intentTask = intentTask.getParent().asTaskFragment().getTask();
                    }
                    boolean wasTopOfVisibleRootTask = intentActivity.mVisibleRequested && intentActivity.inMultiWindowMode() && intentActivity == this.mTargetRootTask.topRunningActivity();
                    this.mTargetRootTask.moveTaskToFront(intentTask, this.mNoAnimation, this.mOptions, this.mStartActivity.appTimeTracker, true, "bringingFoundTaskToFront");
                    this.mMovedToFront = wasTopOfVisibleRootTask ? false : true;
                } else if (intentActivity.getWindowingMode() != 2) {
                    intentTask.reparent(this.mTargetRootTask, true, 0, true, true, "reparentToTargetRootTask");
                    this.mMovedToFront = true;
                }
                this.mOptions = null;
            }
        }
        if (this.mStartActivity.mLaunchCookie != null) {
            intentActivity.mLaunchCookie = this.mStartActivity.mLaunchCookie;
        }
        this.mTargetRootTask = intentActivity.getRootTask();
        this.mSupervisor.handleNonResizableTaskIfNeeded(intentTask, 0, this.mRootWindowContainer.getDefaultTaskDisplayArea(), this.mTargetRootTask);
    }

    private void resumeTargetRootTaskIfNeeded() {
        if (this.mDoResume) {
            ActivityRecord next = this.mTargetRootTask.topRunningActivity(true);
            if (next != null) {
                next.setCurrentLaunchCanTurnScreenOn(true);
            }
            if (this.mTargetRootTask.isFocusable()) {
                this.mRootWindowContainer.resumeFocusedTasksTopActivities(this.mTargetRootTask, null, this.mOptions, this.mTransientLaunch);
            } else {
                this.mRootWindowContainer.ensureActivitiesVisible(null, 0, false);
            }
        } else {
            ActivityOptions.abort(this.mOptions);
        }
        this.mRootWindowContainer.updateUserRootTask(this.mStartActivity.mUserId, this.mTargetRootTask);
    }

    private void setNewTask(Task taskToAffiliate) {
        boolean toTop = (this.mLaunchTaskBehind || this.mAvoidMoveToFront) ? false : true;
        Task task = this.mTargetRootTask;
        ActivityInfo activityInfo = this.mNewTaskInfo;
        if (activityInfo == null) {
            activityInfo = this.mStartActivity.info;
        }
        ActivityInfo activityInfo2 = activityInfo;
        Intent intent = this.mNewTaskIntent;
        if (intent == null) {
            intent = this.mIntent;
        }
        Task task2 = task.reuseOrCreateTask(activityInfo2, intent, this.mVoiceSession, this.mVoiceInteractor, toTop, this.mStartActivity, this.mSourceRecord, this.mOptions);
        task2.mTransitionController.collectExistenceChange(task2);
        addOrReparentStartingActivity(task2, "setTaskFromReuseOrCreateNewTask");
        if (ProtoLogCache.WM_DEBUG_TASKS_enabled) {
            String protoLogParam0 = String.valueOf(this.mStartActivity);
            String protoLogParam1 = String.valueOf(this.mStartActivity.getTask());
            ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_TASKS, -1304806505, 0, (String) null, new Object[]{protoLogParam0, protoLogParam1});
        }
        if (taskToAffiliate != null) {
            this.mStartActivity.setTaskToAffiliateWith(taskToAffiliate);
        }
    }

    private void deliverNewIntent(ActivityRecord activity, NeededUriGrants intentGrants) {
        if (this.mIntentDelivered) {
            return;
        }
        activity.logStartActivity(EventLogTags.WM_NEW_INTENT, activity.getTask());
        activity.deliverNewIntentLocked(this.mCallingUid, this.mStartActivity.intent, intentGrants, this.mStartActivity.launchedFromPackage);
        this.mIntentDelivered = true;
    }

    private void addOrReparentStartingActivity(Task task, String reason) {
        ActivityRecord top;
        TaskFragment newParent = task;
        TaskFragment taskFragment = this.mInTaskFragment;
        if (taskFragment != null) {
            int embeddingCheckResult = canEmbedActivity(taskFragment, this.mStartActivity, task);
            if (embeddingCheckResult == 0) {
                newParent = this.mInTaskFragment;
            } else {
                sendCanNotEmbedActivityError(this.mInTaskFragment, embeddingCheckResult);
            }
        } else {
            TaskFragment candidateTf = this.mAddingToTaskFragment;
            if (candidateTf == null) {
                candidateTf = null;
            }
            if (candidateTf == null && (top = task.topRunningActivity(false, false)) != null) {
                candidateTf = top.getTaskFragment();
            }
            if (candidateTf != null && candidateTf.isEmbedded() && canEmbedActivity(candidateTf, this.mStartActivity, task) == 0) {
                newParent = candidateTf;
            }
        }
        if (this.mStartActivity.getTaskFragment() == null || this.mStartActivity.getTaskFragment() == newParent) {
            newParent.addChild(this.mStartActivity, Integer.MAX_VALUE);
        } else {
            this.mStartActivity.reparent(newParent, newParent.getChildCount(), reason);
        }
    }

    private void sendCanNotEmbedActivityError(TaskFragment taskFragment, int result) {
        String errMsg;
        switch (result) {
            case 1:
                errMsg = "The app:" + this.mCallingUid + "is not trusted to " + this.mStartActivity;
                break;
            case 2:
                errMsg = "Cannot embed " + this.mStartActivity + ". TaskFragment's bounds:" + taskFragment.getBounds() + ", minimum dimensions:" + this.mStartActivity.getMinDimensions();
                break;
            case 3:
                errMsg = "Cannot embed " + this.mStartActivity + " that launched on another task,mLaunchMode=" + this.mLaunchMode + ",mLaunchFlag=" + Integer.toHexString(this.mLaunchFlags);
                break;
            default:
                errMsg = "Unhandled embed result:" + result;
                break;
        }
        if (taskFragment.isOrganized()) {
            this.mService.mWindowOrganizerController.sendTaskFragmentOperationFailure(taskFragment.getTaskFragmentOrganizer(), this.mRequest.errorCallbackToken, new SecurityException(errMsg));
        } else {
            Slog.w(TAG, errMsg);
        }
    }

    private int adjustLaunchFlagsToDocumentMode(ActivityRecord r, boolean launchSingleInstance, boolean launchSingleTask, int launchFlags) {
        if ((launchFlags & 524288) != 0 && (launchSingleInstance || launchSingleTask)) {
            Slog.i(TAG, "Ignoring FLAG_ACTIVITY_NEW_DOCUMENT, launchMode is \"singleInstance\" or \"singleTask\"");
            return launchFlags & (-134742017);
        }
        switch (r.info.documentLaunchMode) {
            case 0:
            default:
                return launchFlags;
            case 1:
                return launchFlags | 524288;
            case 2:
                return launchFlags | 524288;
            case 3:
                if (this.mLaunchMode == 4) {
                    if ((524288 & launchFlags) != 0) {
                        return launchFlags & (-134742017);
                    }
                    return launchFlags;
                }
                return launchFlags & (-134742017);
        }
    }

    private Task getOrCreateRootTask(ActivityRecord r, int launchFlags, Task task, ActivityOptions aOptions) {
        boolean onTop = (aOptions == null || !aOptions.getAvoidMoveToFront()) && !this.mLaunchTaskBehind;
        ActivityRecord activityRecord = this.mSourceRecord;
        Task sourceTask = activityRecord != null ? activityRecord.getTask() : null;
        return this.mRootWindowContainer.getOrCreateRootTask(r, aOptions, task, sourceTask, onTop, this.mLaunchParams, launchFlags);
    }

    private boolean isLaunchModeOneOf(int mode1, int mode2) {
        int i = this.mLaunchMode;
        return mode1 == i || mode2 == i;
    }

    private boolean isLaunchModeOneOf(int mode1, int mode2, int mode3) {
        int i = this.mLaunchMode;
        return mode1 == i || mode2 == i || mode3 == i;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static boolean isDocumentLaunchesIntoExisting(int flags) {
        return (524288 & flags) != 0 && (134217728 & flags) == 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActivityStarter setIntent(Intent intent) {
        this.mRequest.intent = intent;
        return this;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Intent getIntent() {
        return this.mRequest.intent;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActivityStarter setIntentGrants(NeededUriGrants intentGrants) {
        this.mRequest.intentGrants = intentGrants;
        return this;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActivityStarter setReason(String reason) {
        this.mRequest.reason = reason;
        return this;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActivityStarter setCaller(IApplicationThread caller) {
        this.mRequest.caller = caller;
        return this;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActivityStarter setResolvedType(String type) {
        this.mRequest.resolvedType = type;
        return this;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActivityStarter setActivityInfo(ActivityInfo info) {
        this.mRequest.activityInfo = info;
        return this;
    }

    ActivityStarter setResolveInfo(ResolveInfo info) {
        this.mRequest.resolveInfo = info;
        return this;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActivityStarter setVoiceSession(IVoiceInteractionSession voiceSession) {
        this.mRequest.voiceSession = voiceSession;
        return this;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActivityStarter setVoiceInteractor(IVoiceInteractor voiceInteractor) {
        this.mRequest.voiceInteractor = voiceInteractor;
        return this;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActivityStarter setResultTo(IBinder resultTo) {
        this.mRequest.resultTo = resultTo;
        return this;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActivityStarter setResultWho(String resultWho) {
        this.mRequest.resultWho = resultWho;
        return this;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActivityStarter setRequestCode(int requestCode) {
        this.mRequest.requestCode = requestCode;
        return this;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActivityStarter setCallingPid(int pid) {
        this.mRequest.callingPid = pid;
        return this;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActivityStarter setCallingUid(int uid) {
        this.mRequest.callingUid = uid;
        return this;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActivityStarter setCallingPackage(String callingPackage) {
        this.mRequest.callingPackage = callingPackage;
        return this;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActivityStarter setCallingFeatureId(String callingFeatureId) {
        this.mRequest.callingFeatureId = callingFeatureId;
        return this;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActivityStarter setRealCallingPid(int pid) {
        this.mRequest.realCallingPid = pid;
        return this;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActivityStarter setRealCallingUid(int uid) {
        this.mRequest.realCallingUid = uid;
        return this;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActivityStarter setStartFlags(int startFlags) {
        this.mRequest.startFlags = startFlags;
        return this;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActivityStarter setActivityOptions(SafeActivityOptions options) {
        this.mRequest.activityOptions = options;
        return this;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActivityStarter setActivityOptions(Bundle bOptions) {
        this.mRequest.bOptions = bOptions;
        return setActivityOptions(SafeActivityOptions.fromBundle(bOptions));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActivityStarter setIgnoreTargetSecurity(boolean ignoreTargetSecurity) {
        this.mRequest.ignoreTargetSecurity = ignoreTargetSecurity;
        return this;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActivityStarter setFilterCallingUid(int filterCallingUid) {
        this.mRequest.filterCallingUid = filterCallingUid;
        return this;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActivityStarter setComponentSpecified(boolean componentSpecified) {
        this.mRequest.componentSpecified = componentSpecified;
        return this;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActivityStarter setOutActivity(ActivityRecord[] outActivity) {
        this.mRequest.outActivity = outActivity;
        return this;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActivityStarter setInTask(Task inTask) {
        this.mRequest.inTask = inTask;
        return this;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActivityStarter setInTaskFragment(TaskFragment taskFragment) {
        this.mRequest.inTaskFragment = taskFragment;
        return this;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActivityStarter setWaitResult(WaitResult result) {
        this.mRequest.waitResult = result;
        return this;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActivityStarter setProfilerInfo(ProfilerInfo info) {
        this.mRequest.profilerInfo = info;
        return this;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActivityStarter setGlobalConfiguration(Configuration config) {
        this.mRequest.globalConfig = config;
        return this;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActivityStarter setUserId(int userId) {
        this.mRequest.userId = userId;
        return this;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActivityStarter setAllowPendingRemoteAnimationRegistryLookup(boolean allowLookup) {
        this.mRequest.allowPendingRemoteAnimationRegistryLookup = allowLookup;
        return this;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActivityStarter setOriginatingPendingIntent(PendingIntentRecord originatingPendingIntent) {
        this.mRequest.originatingPendingIntent = originatingPendingIntent;
        return this;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActivityStarter setAllowBackgroundActivityStart(boolean allowBackgroundActivityStart) {
        this.mRequest.allowBackgroundActivityStart = allowBackgroundActivityStart;
        return this;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActivityStarter setErrorCallbackToken(IBinder errorCallbackToken) {
        this.mRequest.errorCallbackToken = errorCallbackToken;
        return this;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dump(PrintWriter pw, String prefix) {
        pw.print(prefix);
        pw.print("mCurrentUser=");
        pw.println(this.mRootWindowContainer.mCurrentUser);
        pw.print(prefix);
        pw.print("mLastStartReason=");
        pw.println(this.mLastStartReason);
        pw.print(prefix);
        pw.print("mLastStartActivityTimeMs=");
        pw.println(DateFormat.getDateTimeInstance().format(new Date(this.mLastStartActivityTimeMs)));
        pw.print(prefix);
        pw.print("mLastStartActivityResult=");
        pw.println(this.mLastStartActivityResult);
        if (this.mLastStartActivityRecord != null) {
            pw.print(prefix);
            pw.println("mLastStartActivityRecord:");
            this.mLastStartActivityRecord.dump(pw, prefix + "  ", true);
        }
        if (this.mStartActivity != null) {
            pw.print(prefix);
            pw.println("mStartActivity:");
            this.mStartActivity.dump(pw, prefix + "  ", true);
        }
        if (this.mIntent != null) {
            pw.print(prefix);
            pw.print("mIntent=");
            pw.println(this.mIntent);
        }
        if (this.mOptions != null) {
            pw.print(prefix);
            pw.print("mOptions=");
            pw.println(this.mOptions);
        }
        pw.print(prefix);
        pw.print("mLaunchSingleTop=");
        pw.print(1 == this.mLaunchMode);
        pw.print(" mLaunchSingleInstance=");
        pw.print(3 == this.mLaunchMode);
        pw.print(" mLaunchSingleTask=");
        pw.println(2 == this.mLaunchMode);
        pw.print(prefix);
        pw.print("mLaunchFlags=0x");
        pw.print(Integer.toHexString(this.mLaunchFlags));
        pw.print(" mDoResume=");
        pw.print(this.mDoResume);
        pw.print(" mAddingToTask=");
        pw.print(this.mAddingToTask);
        pw.print(" mInTaskFragment=");
        pw.println(this.mInTaskFragment);
    }

    private boolean agaresPreload(Request request) {
        boolean isAgares = false;
        if (request == null || request.intent == null) {
            return false;
        }
        Intent intent = request.intent;
        if (intent.getPreload() || intent.getBooleanExtra("agaresPreload", false)) {
            isAgares = true;
        }
        if (request.activityInfo == null) {
            request.resolveActivity(this.mSupervisor);
        }
        if (request.activityInfo == null) {
            Slog.e(TAG, "agaresPreload error: activityInfo is null");
            return isAgares;
        }
        String processName = request.activityInfo.processName;
        if (isAgares) {
            Message msg = PooledLambda.obtainMessage(new OctConsumer() { // from class: com.android.server.wm.ActivityStarter$$ExternalSyntheticLambda0
                public final void accept(Object obj, Object obj2, Object obj3, Object obj4, Object obj5, Object obj6, Object obj7, Object obj8) {
                    ((ActivityManagerInternal) obj).startProcess((String) obj2, (ApplicationInfo) obj3, ((Boolean) obj4).booleanValue(), ((Boolean) obj5).booleanValue(), (String) obj6, (ComponentName) obj7, ((Boolean) obj8).booleanValue());
                }
            }, this.mService.mAmInternal, processName, request.activityInfo.applicationInfo, false, false, HostingRecord.HOSTING_TYPE_ACTIVITY, intent.getComponent(), true);
            this.mService.mH.sendMessage(msg);
            Slog.d(TAG, "agaresFW preload processName=" + processName + " for " + intent.getComponent());
        } else if (processName != null && processName.equals(request.callingPackage)) {
            Message msg2 = PooledLambda.obtainMessage(new BiConsumer() { // from class: com.android.server.wm.ActivityStarter$$ExternalSyntheticLambda1
                @Override // java.util.function.BiConsumer
                public final void accept(Object obj, Object obj2) {
                    ((ActivityManagerInternal) obj).adjustProcessesPreloadState((String) obj2);
                }
            }, this.mService.mAmInternal, processName);
            this.mService.mH.sendMessage(msg2);
        }
        return isAgares;
    }
}
