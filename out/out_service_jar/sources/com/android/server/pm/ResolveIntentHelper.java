package com.android.server.pm;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.IIntentSender;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.AuxiliaryResolveInfo;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.os.Trace;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Slog;
import com.android.internal.app.ResolverActivity;
import com.android.internal.util.ArrayUtils;
import com.android.server.compat.PlatformCompat;
import com.android.server.pm.parsing.pkg.AndroidPackage;
import com.android.server.pm.pkg.PackageStateInternal;
import com.android.server.pm.resolution.ComponentResolverApi;
import com.android.server.pm.verify.domain.DomainVerificationManagerInternal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public final class ResolveIntentHelper {
    private static boolean OS_OPENWITH_WPS_SUPPORT = "1".equals(SystemProperties.get("ro.os_openwith_wps_support", "0"));
    private final Context mContext;
    private final DomainVerificationManagerInternal mDomainVerificationManager;
    private final Supplier<ActivityInfo> mInstantAppInstallerActivitySupplier;
    private final PlatformCompat mPlatformCompat;
    private final PreferredActivityHelper mPreferredActivityHelper;
    private final Supplier<ResolveInfo> mResolveInfoSupplier;
    private final UserManagerService mUserManager;
    private final UserNeedsBadgingCache mUserNeedsBadging;
    private ComponentName fileSetComponent = null;
    private String suggPkg = null;

    /* JADX INFO: Access modifiers changed from: package-private */
    public ResolveIntentHelper(Context context, PreferredActivityHelper preferredActivityHelper, PlatformCompat platformCompat, UserManagerService userManager, DomainVerificationManagerInternal domainVerificationManager, UserNeedsBadgingCache userNeedsBadgingCache, Supplier<ResolveInfo> resolveInfoSupplier, Supplier<ActivityInfo> instantAppInstallerActivitySupplier) {
        this.mContext = context;
        this.mPreferredActivityHelper = preferredActivityHelper;
        this.mPlatformCompat = platformCompat;
        this.mUserManager = userManager;
        this.mDomainVerificationManager = domainVerificationManager;
        this.mUserNeedsBadging = userNeedsBadgingCache;
        this.mResolveInfoSupplier = resolveInfoSupplier;
        this.mInstantAppInstallerActivitySupplier = instantAppInstallerActivitySupplier;
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [165=4] */
    public ResolveInfo resolveIntentInternal(Computer computer, Intent intent, String resolvedType, long flags, long privateResolveFlags, int userId, boolean resolveForStart, int filterCallingUid) {
        long j;
        try {
            Trace.traceBegin(262144L, "resolveIntent");
            if (!this.mUserManager.exists(userId)) {
                Trace.traceEnd(262144L);
                return null;
            }
            int callingUid = Binder.getCallingUid();
            long flags2 = computer.updateFlagsForResolve(flags, userId, filterCallingUid, resolveForStart, computer.isImplicitImageCaptureIntentAndNotSetByDpc(intent, userId, resolvedType, flags));
            try {
                computer.enforceCrossUserPermission(callingUid, userId, false, false, "resolve intent");
                Trace.traceBegin(262144L, "queryIntentActivities");
                if (OS_OPENWITH_WPS_SUPPORT && "from_filemanager".equals(intent.getIdentifier())) {
                    this.suggPkg = intent.getPackage();
                    this.fileSetComponent = intent.getComponent();
                    intent.setPackage(null);
                    intent.setComponent(null);
                }
                List<ResolveInfo> query = computer.queryIntentActivitiesInternal(intent, resolvedType, flags2, privateResolveFlags, filterCallingUid, userId, resolveForStart, true);
                Trace.traceEnd(262144L);
                boolean z = true;
                boolean queryMayBeFiltered = UserHandle.getAppId(filterCallingUid) >= 10000 && !resolveForStart;
                j = 262144;
                try {
                    ResolveInfo bestChoice = chooseBestActivity(computer, intent, resolvedType, flags2, privateResolveFlags, query, userId, queryMayBeFiltered);
                    this.suggPkg = null;
                    this.fileSetComponent = null;
                    if ((privateResolveFlags & 1) == 0) {
                        z = false;
                    }
                    boolean nonBrowserOnly = z;
                    if (nonBrowserOnly && bestChoice != null) {
                        if (bestChoice.handleAllWebDataURI) {
                            Trace.traceEnd(262144L);
                            return null;
                        }
                    }
                    Trace.traceEnd(262144L);
                    return bestChoice;
                } catch (Throwable th) {
                    th = th;
                    Trace.traceEnd(j);
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
                j = 262144;
            }
        } catch (Throwable th3) {
            th = th3;
            j = 262144;
        }
    }

    /* JADX WARN: Removed duplicated region for block: B:39:0x0161  */
    /* JADX WARN: Removed duplicated region for block: B:40:0x0165  */
    /* JADX WARN: Removed duplicated region for block: B:43:0x016c  */
    /* JADX WARN: Removed duplicated region for block: B:44:0x0177  */
    /* JADX WARN: Removed duplicated region for block: B:52:0x01ca  */
    /* JADX WARN: Removed duplicated region for block: B:57:0x01f1  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private ResolveInfo chooseBestActivity(Computer computer, Intent intent, String resolvedType, long flags, long privateResolveFlags, List<ResolveInfo> query, int userId, boolean queryMayBeFiltered) {
        String str;
        ResolveInfo resolveInfo;
        ResolveInfo resolveInfo2;
        boolean openWith;
        String compClass;
        String compClass2;
        int n;
        int i;
        int n2;
        int i2;
        boolean z;
        int browserCount;
        String compClass3;
        int i3;
        boolean isFromResolverActi;
        boolean isFromResolverActi2;
        boolean isFromFileManager;
        boolean openWith2;
        boolean osDebug;
        boolean osDebug2 = false;
        if (query == null) {
            str = "PMS-OPEN";
            resolveInfo = null;
        } else {
            String identifier = intent.getIdentifier();
            String compPkg = null;
            String compClass4 = null;
            if (!OS_OPENWITH_WPS_SUPPORT) {
                resolveInfo2 = null;
                openWith = false;
                compClass = null;
                compClass2 = null;
            } else {
                ComponentName componentName = this.fileSetComponent;
                if (componentName != null) {
                    compPkg = componentName.getPackageName();
                    compClass4 = this.fileSetComponent.getClassName();
                    Log.d("PMS-OPEN", "from fileManagex compPkg = " + compPkg + ", compClass = " + compClass4);
                }
                if ("os_always_openwith".equals(identifier)) {
                    Log.d("PMS-OPEN", "indentifier = " + identifier + ", openWith = true");
                    isFromResolverActi = false;
                    isFromResolverActi2 = false;
                    isFromFileManager = true;
                    openWith2 = true;
                } else if ("from_filemanager".equals(identifier)) {
                    Log.d("PMS-OPEN", "indentifier = " + identifier + ", isFromFileManager = true");
                    isFromResolverActi = false;
                    isFromResolverActi2 = true;
                    isFromFileManager = false;
                    openWith2 = true;
                } else if ("os_once_always".equals(identifier)) {
                    Log.d("PMS-OPEN", "indentifier = " + identifier + ", isFromResolverActi = true");
                    isFromResolverActi = true;
                    isFromResolverActi2 = false;
                    isFromFileManager = false;
                    openWith2 = true;
                } else {
                    isFromResolverActi = false;
                    isFromResolverActi2 = false;
                    isFromFileManager = false;
                    openWith2 = false;
                }
                if (intent.getExtras() == null) {
                    osDebug = openWith2;
                    resolveInfo2 = null;
                } else if (isFromResolverActi2 || isFromResolverActi) {
                    if (isFromResolverActi2) {
                        intent.setPackage(null);
                        intent.setIdentifier(null);
                    }
                    Log.w("PMS-OPEN", "isFromFileManager = " + isFromResolverActi2 + ", isFromResolverActi = " + isFromResolverActi + ", openWith = " + isFromFileManager);
                    boolean suggPkgInstalled = true;
                    try {
                        osDebug = openWith2;
                    } catch (Exception e) {
                        osDebug = openWith2;
                    }
                    try {
                        this.mContext.getPackageManager().getPackageInfo(this.suggPkg, 0);
                    } catch (Exception e2) {
                        suggPkgInstalled = false;
                        Log.w("PMS-OPEN", "suggPkg = " + this.suggPkg + ", intent.getPackage = " + intent.getPackage() + ", suggPkg Installed = " + suggPkgInstalled);
                        if (!suggPkgInstalled) {
                        }
                        if (isFromResolverActi) {
                        }
                        n = query.size();
                        if (OS_OPENWITH_WPS_SUPPORT) {
                            Log.i("PMS-OPEN", "intent = " + intent + ", n = " + n + ", final suggPkg = " + this.suggPkg + ", openWith = " + openWith);
                        }
                        if (n != 1) {
                        }
                    }
                    Log.w("PMS-OPEN", "suggPkg = " + this.suggPkg + ", intent.getPackage = " + intent.getPackage() + ", suggPkg Installed = " + suggPkgInstalled);
                    if (!suggPkgInstalled) {
                        resolveInfo2 = null;
                    } else {
                        resolveInfo2 = null;
                        this.suggPkg = null;
                    }
                } else {
                    osDebug = openWith2;
                    resolveInfo2 = null;
                }
                if (isFromResolverActi) {
                    openWith = isFromFileManager;
                    osDebug2 = osDebug;
                    compClass = compClass4;
                    compClass2 = compPkg;
                } else {
                    openWith = false;
                    osDebug2 = osDebug;
                    compClass = compClass4;
                    compClass2 = compPkg;
                }
            }
            n = query.size();
            if (OS_OPENWITH_WPS_SUPPORT && osDebug2) {
                Log.i("PMS-OPEN", "intent = " + intent + ", n = " + n + ", final suggPkg = " + this.suggPkg + ", openWith = " + openWith);
            }
            if (n != 1) {
                if (osDebug2) {
                    i3 = 0;
                    Log.i("PMS-OPEN", "return N is 1, r = " + query.get(0));
                } else {
                    i3 = 0;
                }
                return query.get(i3);
            } else if (n <= 1) {
                str = "PMS-OPEN";
                resolveInfo = resolveInfo2;
            } else {
                boolean debug = (intent.getFlags() & 8) != 0;
                ResolveInfo r0 = query.get(0);
                ResolveInfo r1 = query.get(1);
                if (osDebug2) {
                    Log.d("PMS-OPEN", r0 + "=" + r0.priority + " , vs " + r1 + "=" + r1.priority);
                }
                if (PackageManagerService.DEBUG_INTENT_MATCHING || debug) {
                    Slog.v("PackageManager", r0.activityInfo.name + "=" + r0.priority + " vs " + r1.activityInfo.name + "=" + r1.priority);
                }
                if ((r0.priority != r1.priority || r0.preferredOrder != r1.preferredOrder || r0.isDefault != r1.isDefault) && !openWith) {
                    if (osDebug2) {
                        i = 0;
                        Log.e("PMS-OPEN", "return higher priority, ri = " + query.get(0));
                    } else {
                        i = 0;
                    }
                    return query.get(i);
                }
                String compPkg2 = compClass2;
                String compClass5 = compClass;
                int n3 = n;
                boolean openWith3 = openWith;
                ResolveInfo ri = this.mPreferredActivityHelper.findPreferredActivityNotLocked(computer, intent, resolvedType, flags, query, true, false, debug, userId, queryMayBeFiltered);
                if (ri != null && !openWith3) {
                    if (osDebug2) {
                        Log.e("PMS-OPEN", "return findPreferredActivity, ri = " + ri);
                    }
                    return ri;
                }
                if (OS_OPENWITH_WPS_SUPPORT) {
                    int i4 = 0;
                    while (true) {
                        n2 = n3;
                        if (i4 < n2) {
                            ResolveInfo info = query.get(i4);
                            if (osDebug2) {
                                Log.d("PMS-OPEN", "for log --- n = " + n2 + ", i = " + i4 + ", pkg = " + info.activityInfo.packageName + ", className = " + info.getComponentInfo().name);
                            }
                            String compPkg3 = compPkg2;
                            if (compPkg3 == null) {
                                compClass3 = compClass5;
                            } else {
                                compClass3 = compClass5;
                                if (compClass3 != null && compPkg3.equals(info.activityInfo.packageName) && compClass3.equals(info.getComponentInfo().name)) {
                                    return info;
                                }
                            }
                            i4++;
                            n3 = n2;
                            compPkg2 = compPkg3;
                            compClass5 = compClass3;
                        } else {
                            if (this.suggPkg != null) {
                                if (osDebug2) {
                                    Log.i("PMS-OPEN", "intent.setPackage suggPkg = " + this.suggPkg);
                                }
                                intent.setPackage(this.suggPkg);
                            }
                            for (int i5 = 0; i5 < n2; i5++) {
                                ResolveInfo ri2 = query.get(i5);
                                if (osDebug2) {
                                    Log.i("PMS-OPEN", "for log --- n = " + n2 + ", i = " + i5 + ", ri = " + ri2 + ", priority = " + ri2.priority);
                                }
                            }
                        }
                    }
                } else {
                    n2 = n3;
                }
                int browserCount2 = 0;
                int i6 = 0;
                while (i6 < n2) {
                    ResolveInfo ri3 = query.get(i6);
                    if (!ri3.handleAllWebDataURI) {
                        browserCount = browserCount2;
                    } else {
                        browserCount = browserCount2 + 1;
                    }
                    if (ri3.activityInfo.applicationInfo.isInstantApp()) {
                        String packageName = ri3.activityInfo.packageName;
                        PackageStateInternal ps = computer.getPackageStateInternal(packageName);
                        if (ps != null && PackageManagerServiceUtils.hasAnyDomainApproval(this.mDomainVerificationManager, ps, intent, flags, userId) && !openWith3) {
                            if (osDebug2) {
                                Log.e("PMS-OPEN", "return ephemeral, ri = " + ri3);
                            }
                            return ri3;
                        }
                    }
                    i6++;
                    browserCount2 = browserCount;
                }
                if ((privateResolveFlags & 2) != 0 && !openWith3) {
                    if (osDebug2) {
                        Log.e("PMS-OPEN", "return null!!! privateResolveFlags");
                    }
                    return null;
                }
                ResolveInfo ri4 = new ResolveInfo(this.mResolveInfoSupplier.get());
                ri4.handleAllWebDataURI = browserCount2 == n2;
                ri4.activityInfo = new ActivityInfo(ri4.activityInfo);
                ri4.activityInfo.labelRes = ResolverActivity.getLabelRes(intent.getAction());
                String intentPackage = intent.getPackage();
                if (TextUtils.isEmpty(intentPackage) || !allHavePackage(query, intentPackage)) {
                    i2 = userId;
                    z = true;
                } else {
                    ApplicationInfo appi = query.get(0).activityInfo.applicationInfo;
                    ri4.resolvePackageName = intentPackage;
                    i2 = userId;
                    if (this.mUserNeedsBadging.get(i2)) {
                        z = true;
                        ri4.noResourceId = true;
                    } else {
                        z = true;
                        ri4.icon = appi.icon;
                    }
                    ri4.iconResourceId = appi.icon;
                    ri4.labelRes = appi.labelRes;
                }
                ri4.activityInfo.applicationInfo = new ApplicationInfo(ri4.activityInfo.applicationInfo);
                if (i2 != 0) {
                    ri4.activityInfo.applicationInfo.uid = UserHandle.getUid(i2, UserHandle.getAppId(ri4.activityInfo.applicationInfo.uid));
                }
                if (ri4.activityInfo.metaData == null) {
                    ri4.activityInfo.metaData = new Bundle();
                }
                ri4.activityInfo.metaData.putBoolean("android.dock_home", z);
                if (osDebug2) {
                    Log.e("PMS-OPEN", "return ResolverActivity, ri = " + ri4);
                }
                return ri4;
            }
        }
        if (osDebug2) {
            Log.e(str, "return null!!! no match");
        }
        return resolveInfo;
    }

    private boolean allHavePackage(List<ResolveInfo> list, String packageName) {
        if (ArrayUtils.isEmpty(list)) {
            return false;
        }
        int n = list.size();
        for (int i = 0; i < n; i++) {
            ResolveInfo ri = list.get(i);
            ActivityInfo ai = ri != null ? ri.activityInfo : null;
            if (ai == null || !packageName.equals(ai.packageName)) {
                return false;
            }
        }
        return true;
    }

    public IntentSender getLaunchIntentSenderForPackage(Computer computer, String packageName, String callingPackage, String featureId, int userId) throws RemoteException {
        Intent intentToResolve;
        List<ResolveInfo> ris;
        Objects.requireNonNull(packageName);
        int callingUid = Binder.getCallingUid();
        computer.enforceCrossUserPermission(callingUid, userId, false, false, "get launch intent sender for package");
        int packageUid = computer.getPackageUid(callingPackage, 0L, userId);
        if (!UserHandle.isSameApp(callingUid, packageUid)) {
            throw new SecurityException("getLaunchIntentSenderForPackage() from calling uid: " + callingUid + " does not own package: " + callingPackage);
        }
        Intent intentToResolve2 = new Intent("android.intent.action.MAIN");
        intentToResolve2.addCategory("android.intent.category.INFO");
        intentToResolve2.setPackage(packageName);
        ContentResolver contentResolver = this.mContext.getContentResolver();
        String resolvedType = intentToResolve2.resolveTypeIfNeeded(contentResolver);
        List<ResolveInfo> ris2 = computer.queryIntentActivitiesInternal(intentToResolve2, resolvedType, 0L, 0L, callingUid, userId, true, false);
        if (ris2 == null || ris2.size() <= 0) {
            intentToResolve2.removeCategory("android.intent.category.INFO");
            intentToResolve2.addCategory("android.intent.category.LAUNCHER");
            intentToResolve2.setPackage(packageName);
            resolvedType = intentToResolve2.resolveTypeIfNeeded(contentResolver);
            intentToResolve = intentToResolve2;
            ris = computer.queryIntentActivitiesInternal(intentToResolve2, resolvedType, 0L, 0L, callingUid, userId, true, false);
        } else {
            ris = ris2;
            intentToResolve = intentToResolve2;
        }
        Intent intent = new Intent(intentToResolve);
        intent.setFlags(268435456);
        if (ris != null && !ris.isEmpty()) {
            intent.setClassName(ris.get(0).activityInfo.packageName, ris.get(0).activityInfo.name);
        }
        IIntentSender target = ActivityManager.getService().getIntentSenderWithFeature(2, callingPackage, featureId, (IBinder) null, (String) null, 1, new Intent[]{intent}, resolvedType != null ? new String[]{resolvedType} : null, 67108864, (Bundle) null, userId);
        return new IntentSender(target);
    }

    public List<ResolveInfo> queryIntentReceiversInternal(Computer computer, Intent intent, String resolvedType, long flags, int userId, int queryingUid) {
        return queryIntentReceiversInternal(computer, intent, resolvedType, flags, userId, queryingUid, false);
    }

    public List<ResolveInfo> queryIntentReceiversInternal(Computer computer, Intent intent, String resolvedType, long flags, int userId, int filterCallingUid, boolean forSend) {
        Intent intent2;
        Intent originalIntent;
        ComponentName comp;
        List<ResolveInfo> result;
        ComponentName comp2;
        if (this.mUserManager.exists(userId)) {
            int queryingUid = forSend ? 1000 : filterCallingUid;
            computer.enforceCrossUserPermission(queryingUid, userId, false, false, "query intent receivers");
            String instantAppPkgName = computer.getInstantAppPackageName(queryingUid);
            long flags2 = computer.updateFlagsForResolve(flags, userId, queryingUid, false, computer.isImplicitImageCaptureIntentAndNotSetByDpc(intent, userId, resolvedType, flags));
            ComponentName comp3 = intent.getComponent();
            if (comp3 == null && intent.getSelector() != null) {
                Intent intent3 = intent.getSelector();
                originalIntent = intent;
                comp = intent3.getComponent();
                intent2 = intent3;
            } else {
                intent2 = intent;
                originalIntent = null;
                comp = comp3;
            }
            ComponentResolverApi componentResolver = computer.getComponentResolver();
            List<ResolveInfo> list = Collections.emptyList();
            if (comp != null) {
                ActivityInfo ai = computer.getReceiverInfo(comp, flags2, userId);
                if (ai != null) {
                    boolean matchInstantApp = (8388608 & flags2) != 0;
                    boolean matchVisibleToInstantAppOnly = (flags2 & 16777216) != 0;
                    boolean matchExplicitlyVisibleOnly = (flags2 & 33554432) != 0;
                    boolean isCallerInstantApp = instantAppPkgName != null;
                    boolean isTargetSameInstantApp = comp.getPackageName().equals(instantAppPkgName);
                    boolean isTargetInstantApp = (ai.applicationInfo.privateFlags & 128) != 0;
                    comp2 = comp;
                    boolean isTargetVisibleToInstantApp = (ai.flags & 1048576) != 0;
                    boolean isTargetExplicitlyVisibleToInstantApp = isTargetVisibleToInstantApp && (ai.flags & 2097152) == 0;
                    boolean isTargetHiddenFromInstantApp = !isTargetVisibleToInstantApp || (matchExplicitlyVisibleOnly && !isTargetExplicitlyVisibleToInstantApp);
                    boolean blockResolution = !isTargetSameInstantApp && (!(matchInstantApp || isCallerInstantApp || !isTargetInstantApp) || (matchVisibleToInstantAppOnly && isCallerInstantApp && isTargetHiddenFromInstantApp));
                    if (!blockResolution) {
                        ResolveInfo ri = new ResolveInfo();
                        ri.activityInfo = ai;
                        List<ResolveInfo> list2 = new ArrayList<>(1);
                        list2.add(ri);
                        PackageManagerServiceUtils.applyEnforceIntentFilterMatching(this.mPlatformCompat, componentResolver, list2, true, intent2, resolvedType, filterCallingUid);
                        list = list2;
                    }
                } else {
                    comp2 = comp;
                }
            } else {
                String pkgName = intent2.getPackage();
                if (pkgName == null && (result = componentResolver.queryReceivers(computer, intent2, resolvedType, flags2, userId)) != null) {
                    list = result;
                }
                AndroidPackage pkg = computer.getPackage(pkgName);
                if (pkg != null) {
                    List<ResolveInfo> result2 = componentResolver.queryReceivers(computer, intent2, resolvedType, flags2, pkg.getReceivers(), userId);
                    if (result2 != null) {
                        list = result2;
                    }
                }
            }
            if (originalIntent != null) {
                PackageManagerServiceUtils.applyEnforceIntentFilterMatching(this.mPlatformCompat, componentResolver, list, true, originalIntent, resolvedType, filterCallingUid);
            }
            return computer.applyPostResolutionFilter(list, instantAppPkgName, false, queryingUid, false, userId, intent2);
        }
        return Collections.emptyList();
    }

    public ResolveInfo resolveServiceInternal(Computer computer, Intent intent, String resolvedType, long flags, int userId, int callingUid) {
        List<ResolveInfo> query;
        if (this.mUserManager.exists(userId) && (query = computer.queryIntentServicesInternal(intent, resolvedType, computer.updateFlagsForResolve(flags, userId, callingUid, false, false), userId, callingUid, false)) != null && query.size() >= 1) {
            return query.get(0);
        }
        return null;
    }

    public List<ResolveInfo> queryIntentContentProvidersInternal(Computer computer, Intent intent, String resolvedType, long flags, int userId) {
        Intent intent2;
        ComponentName comp;
        boolean blockNormalResolution;
        if (this.mUserManager.exists(userId)) {
            int callingUid = Binder.getCallingUid();
            String instantAppPkgName = computer.getInstantAppPackageName(callingUid);
            long flags2 = computer.updateFlagsForResolve(flags, userId, callingUid, false, false);
            ComponentName comp2 = intent.getComponent();
            if (comp2 == null && intent.getSelector() != null) {
                Intent intent3 = intent.getSelector();
                comp = intent3.getComponent();
                intent2 = intent3;
            } else {
                intent2 = intent;
                comp = comp2;
            }
            if (comp != null) {
                List<ResolveInfo> list = new ArrayList<>(1);
                ProviderInfo pi = computer.getProviderInfo(comp, flags2, userId);
                if (pi != null) {
                    boolean matchInstantApp = (8388608 & flags2) != 0;
                    boolean matchVisibleToInstantAppOnly = (flags2 & 16777216) != 0;
                    boolean isCallerInstantApp = instantAppPkgName != null;
                    boolean isTargetSameInstantApp = comp.getPackageName().equals(instantAppPkgName);
                    boolean isTargetInstantApp = (pi.applicationInfo.privateFlags & 128) != 0;
                    boolean isTargetHiddenFromInstantApp = (pi.flags & 1048576) == 0;
                    boolean blockResolution = !isTargetSameInstantApp && (!(matchInstantApp || isCallerInstantApp || !isTargetInstantApp) || (matchVisibleToInstantAppOnly && isCallerInstantApp && isTargetHiddenFromInstantApp));
                    if (!isTargetInstantApp && !isCallerInstantApp) {
                        if (computer.shouldFilterApplication(computer.getPackageStateInternal(pi.applicationInfo.packageName, 1000), callingUid, userId)) {
                            blockNormalResolution = true;
                            if (!blockResolution && !blockNormalResolution) {
                                ResolveInfo ri = new ResolveInfo();
                                ri.providerInfo = pi;
                                list.add(ri);
                            }
                        }
                    }
                    blockNormalResolution = false;
                    if (!blockResolution) {
                        ResolveInfo ri2 = new ResolveInfo();
                        ri2.providerInfo = pi;
                        list.add(ri2);
                    }
                }
                return list;
            }
            ComponentResolverApi componentResolver = computer.getComponentResolver();
            String pkgName = intent2.getPackage();
            if (pkgName == null) {
                List<ResolveInfo> resolveInfos = componentResolver.queryProviders(computer, intent2, resolvedType, flags2, userId);
                if (resolveInfos == null) {
                    return Collections.emptyList();
                }
                return applyPostContentProviderResolutionFilter(computer, resolveInfos, instantAppPkgName, userId, callingUid);
            }
            AndroidPackage pkg = computer.getPackage(pkgName);
            if (pkg != null) {
                List<ResolveInfo> resolveInfos2 = componentResolver.queryProviders(computer, intent2, resolvedType, flags2, pkg.getProviders(), userId);
                if (resolveInfos2 == null) {
                    return Collections.emptyList();
                }
                return applyPostContentProviderResolutionFilter(computer, resolveInfos2, instantAppPkgName, userId, callingUid);
            }
            return Collections.emptyList();
        }
        return Collections.emptyList();
    }

    private List<ResolveInfo> applyPostContentProviderResolutionFilter(Computer computer, List<ResolveInfo> resolveInfos, String instantAppPkgName, int userId, int callingUid) {
        Computer computer2 = computer;
        int i = resolveInfos.size() - 1;
        while (i >= 0) {
            ResolveInfo info = resolveInfos.get(i);
            if (instantAppPkgName == null) {
                PackageStateInternal resolvedSetting = computer2.getPackageStateInternal(info.providerInfo.packageName, 0);
                if (!computer2.shouldFilterApplication(resolvedSetting, callingUid, userId)) {
                    i--;
                    computer2 = computer;
                }
            }
            boolean isEphemeralApp = info.providerInfo.applicationInfo.isInstantApp();
            if (isEphemeralApp && instantAppPkgName.equals(info.providerInfo.packageName)) {
                if (info.providerInfo.splitName != null && !ArrayUtils.contains(info.providerInfo.applicationInfo.splitNames, info.providerInfo.splitName)) {
                    if (this.mInstantAppInstallerActivitySupplier.get() == null) {
                        if (PackageManagerService.DEBUG_INSTANT) {
                            Slog.v("PackageManager", "No installer - not adding it to the ResolveInfo list");
                        }
                        resolveInfos.remove(i);
                    } else {
                        if (PackageManagerService.DEBUG_INSTANT) {
                            Slog.v("PackageManager", "Adding ephemeral installer to the ResolveInfo list");
                        }
                        ResolveInfo installerInfo = new ResolveInfo(computer.getInstantAppInstallerInfo());
                        installerInfo.auxiliaryInfo = new AuxiliaryResolveInfo((ComponentName) null, info.providerInfo.packageName, info.providerInfo.applicationInfo.longVersionCode, info.providerInfo.splitName);
                        installerInfo.filter = new IntentFilter();
                        installerInfo.resolvePackageName = info.getComponentInfo().packageName;
                        resolveInfos.set(i, installerInfo);
                    }
                }
            } else if (isEphemeralApp || (info.providerInfo.flags & 1048576) == 0) {
                resolveInfos.remove(i);
            }
            i--;
            computer2 = computer;
        }
        return resolveInfos;
    }

    /* JADX WARN: Incorrect condition in loop: B:14:0x0081 */
    /* JADX WARN: Removed duplicated region for block: B:41:0x0161  */
    /* JADX WARN: Removed duplicated region for block: B:42:0x0184  */
    /* JADX WARN: Removed duplicated region for block: B:45:0x0192  */
    /* JADX WARN: Removed duplicated region for block: B:67:0x021e  */
    /* JADX WARN: Removed duplicated region for block: B:68:0x0226  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public List<ResolveInfo> queryIntentActivityOptionsInternal(Computer computer, ComponentName caller, Intent[] specifics, String[] specificTypes, Intent intent, String resolvedType, long flags, int userId) {
        long flags2;
        String str;
        List<ResolveInfo> results;
        String str2;
        int specificsPos;
        Iterator<String> it;
        String str3;
        int i;
        int specificsPos2;
        String action;
        String str4;
        List<ResolveInfo> results2;
        int callingUid;
        String str5;
        long flags3;
        ActivityInfo ai;
        ComponentName comp;
        int i2;
        String str6;
        String str7;
        int N;
        int j;
        List<ResolveInfo> results3;
        ResolveInfo ri;
        int specificsPos3;
        String action2;
        int specificsPos4;
        Intent[] intentArr = specifics;
        if (this.mUserManager.exists(userId)) {
            int callingUid2 = Binder.getCallingUid();
            long flags4 = computer.updateFlagsForResolve(flags, userId, callingUid2, false, computer.isImplicitImageCaptureIntentAndNotSetByDpc(intent, userId, resolvedType, flags));
            computer.enforceCrossUserPermission(callingUid2, userId, false, false, "query intent activity options");
            String resultsAction = intent.getAction();
            List<ResolveInfo> results4 = computer.queryIntentActivitiesInternal(intent, resolvedType, flags4 | 64, userId);
            String str8 = ": ";
            String str9 = "PackageManager";
            if (PackageManagerService.DEBUG_INTENT_MATCHING) {
                Log.v("PackageManager", "Query " + intent + ": " + results4);
            }
            int specificsPos5 = 0;
            String str10 = "Removing duplicate item from ";
            if (intentArr == null) {
                flags2 = flags4;
                str = "Removing duplicate item from ";
                results = results4;
                str2 = "PackageManager";
            } else {
                int i3 = 0;
                int specificsPos6 = 0;
                while (i3 < specificsPos) {
                    Intent sintent = intentArr[i3];
                    if (sintent == null) {
                        i = i3;
                        specificsPos2 = specificsPos6;
                        str3 = str8;
                        str4 = str9;
                        results2 = results4;
                        flags3 = flags4;
                        callingUid = callingUid2;
                        str5 = str10;
                    } else {
                        if (PackageManagerService.DEBUG_INTENT_MATCHING) {
                            Log.v(str9, "Specific #" + i3 + str8 + sintent);
                        }
                        String action3 = sintent.getAction();
                        if (resultsAction != 0 && resultsAction.equals(action3)) {
                            action3 = null;
                        }
                        ResolveInfo ri2 = null;
                        String str11 = str8;
                        ComponentName comp2 = sintent.getComponent();
                        if (comp2 == null) {
                            i = i3;
                            specificsPos2 = specificsPos6;
                            action = action3;
                            callingUid = callingUid2;
                            str5 = str10;
                            str3 = str11;
                            str4 = str9;
                            results2 = results4;
                            long flags5 = flags4;
                            ResolveInfo ri3 = resolveIntentInternal(computer, sintent, specificTypes != null ? specificTypes[i3] : null, flags4, 0L, userId, false, Binder.getCallingUid());
                            if (ri3 == null) {
                                flags3 = flags5;
                            } else {
                                this.mResolveInfoSupplier.get();
                                ActivityInfo ai2 = ri3.activityInfo;
                                comp = new ComponentName(ai2.applicationInfo.packageName, ai2.name);
                                ri2 = ri3;
                                ai = ai2;
                                flags3 = flags5;
                                if (PackageManagerService.DEBUG_INTENT_MATCHING) {
                                    i2 = i;
                                    str6 = str3;
                                    str7 = str4;
                                } else {
                                    i2 = i;
                                    str6 = str3;
                                    str7 = str4;
                                    Log.v(str7, "Specific #" + i2 + str6 + ai);
                                }
                                N = results2.size();
                                j = specificsPos2;
                                while (j < N) {
                                    List<ResolveInfo> results5 = results2;
                                    ResolveInfo sri = results5.get(j);
                                    String str12 = str6;
                                    if (sri.activityInfo.name.equals(comp.getClassName()) && sri.activityInfo.applicationInfo.packageName.equals(comp.getPackageName())) {
                                        action2 = action;
                                    } else {
                                        action2 = action;
                                        if (action2 == null) {
                                            action = action2;
                                            specificsPos4 = specificsPos2;
                                        } else if (!sri.filter.matchAction(action2)) {
                                            action = action2;
                                            specificsPos4 = specificsPos2;
                                        }
                                        j++;
                                        specificsPos2 = specificsPos4;
                                        results2 = results5;
                                        str6 = str12;
                                    }
                                    results5.remove(j);
                                    if (PackageManagerService.DEBUG_INTENT_MATCHING) {
                                        action = action2;
                                        specificsPos4 = specificsPos2;
                                        Log.v(str7, str5 + j + " due to specific " + specificsPos4);
                                    } else {
                                        action = action2;
                                        specificsPos4 = specificsPos2;
                                    }
                                    if (ri2 == null) {
                                        ri2 = sri;
                                    }
                                    j--;
                                    N--;
                                    j++;
                                    specificsPos2 = specificsPos4;
                                    results2 = results5;
                                    str6 = str12;
                                }
                                results3 = results2;
                                str3 = str6;
                                int specificsPos7 = specificsPos2;
                                if (ri2 == null) {
                                    ri = ri2;
                                } else {
                                    ri = new ResolveInfo();
                                    ri.activityInfo = ai;
                                }
                                results3.add(specificsPos7, ri);
                                ri.specificIndex = i2;
                                specificsPos3 = specificsPos7 + 1;
                            }
                        } else {
                            str3 = str11;
                            i = i3;
                            specificsPos2 = specificsPos6;
                            action = action3;
                            str4 = str9;
                            results2 = results4;
                            callingUid = callingUid2;
                            str5 = str10;
                            flags3 = flags4;
                            ai = computer.getActivityInfo(comp2, flags3, userId);
                            if (ai != null) {
                                comp = comp2;
                                if (PackageManagerService.DEBUG_INTENT_MATCHING) {
                                }
                                N = results2.size();
                                j = specificsPos2;
                                while (j < N) {
                                }
                                results3 = results2;
                                str3 = str6;
                                int specificsPos72 = specificsPos2;
                                if (ri2 == null) {
                                }
                                results3.add(specificsPos72, ri);
                                ri.specificIndex = i2;
                                specificsPos3 = specificsPos72 + 1;
                            }
                        }
                        i3 = i2 + 1;
                        intentArr = specifics;
                        str10 = str5;
                        callingUid2 = callingUid;
                        str8 = str3;
                        long j2 = flags3;
                        specificsPos6 = specificsPos3;
                        str9 = str7;
                        results4 = results3;
                        flags4 = j2;
                    }
                    results3 = results2;
                    i2 = i;
                    specificsPos3 = specificsPos2;
                    str7 = str4;
                    i3 = i2 + 1;
                    intentArr = specifics;
                    str10 = str5;
                    callingUid2 = callingUid;
                    str8 = str3;
                    long j22 = flags3;
                    specificsPos6 = specificsPos3;
                    str9 = str7;
                    results4 = results3;
                    flags4 = j22;
                }
                str = str10;
                String str13 = str9;
                int specificsPos8 = specificsPos6;
                flags2 = flags4;
                results = results4;
                str2 = str13;
                specificsPos5 = specificsPos8;
            }
            int N2 = results.size();
            int i4 = specificsPos5;
            while (i4 < N2 - 1) {
                ResolveInfo rii = results.get(i4);
                if (rii.filter == null || (it = rii.filter.actionsIterator()) == null) {
                    specificsPos = specificsPos5;
                } else {
                    while (it.hasNext()) {
                        String action4 = it.next();
                        if (resultsAction == null || !resultsAction.equals(action4)) {
                            int j3 = i4 + 1;
                            while (j3 < N2) {
                                ResolveInfo rij = results.get(j3);
                                int specificsPos9 = specificsPos5;
                                if (rij.filter != null && rij.filter.hasAction(action4)) {
                                    results.remove(j3);
                                    if (PackageManagerService.DEBUG_INTENT_MATCHING) {
                                        Log.v(str2, str + j3 + " due to action " + action4 + " at " + i4);
                                    }
                                    j3--;
                                    N2--;
                                }
                                j3++;
                                specificsPos5 = specificsPos9;
                            }
                        }
                    }
                    specificsPos = specificsPos5;
                    if ((flags2 & 64) == 0) {
                        rii.filter = null;
                    }
                }
                i4++;
                specificsPos5 = specificsPos;
            }
            if (caller != null) {
                int N3 = results.size();
                int i5 = 0;
                while (true) {
                    if (i5 >= N3) {
                        break;
                    }
                    ActivityInfo ainfo = results.get(i5).activityInfo;
                    if (!caller.getPackageName().equals(ainfo.applicationInfo.packageName) || !caller.getClassName().equals(ainfo.name)) {
                        i5++;
                    } else {
                        results.remove(i5);
                        break;
                    }
                }
            }
            if ((flags2 & 64) == 0) {
                int N4 = results.size();
                for (int i6 = 0; i6 < N4; i6++) {
                    results.get(i6).filter = null;
                }
            }
            if (PackageManagerService.DEBUG_INTENT_MATCHING) {
                Log.v(str2, "Result: " + results);
            }
            return results;
        }
        return Collections.emptyList();
    }
}
