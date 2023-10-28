package com.android.server.pm;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.IIntentSender;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.pm.ActivityInfo;
import android.content.pm.AuxiliaryResolveInfo;
import android.content.pm.InstantAppIntentFilter;
import android.content.pm.InstantAppRequest;
import android.content.pm.InstantAppRequestInfo;
import android.content.pm.InstantAppResolveInfo;
import android.metrics.LogMaker;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Slog;
import com.android.internal.logging.MetricsLogger;
import com.android.server.pm.InstantAppResolverConnection;
import com.android.server.pm.resolution.ComponentResolver;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
/* loaded from: classes2.dex */
public abstract class InstantAppResolver {
    private static final boolean DEBUG_INSTANT = Build.IS_DEBUGGABLE;
    private static final int RESOLUTION_BIND_TIMEOUT = 2;
    private static final int RESOLUTION_CALL_TIMEOUT = 3;
    private static final int RESOLUTION_FAILURE = 1;
    private static final int RESOLUTION_SUCCESS = 0;
    private static final String TAG = "PackageManager";
    private static MetricsLogger sMetricsLogger;

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes2.dex */
    public @interface ResolutionStatus {
    }

    private static MetricsLogger getLogger() {
        if (sMetricsLogger == null) {
            sMetricsLogger = new MetricsLogger();
        }
        return sMetricsLogger;
    }

    public static Intent sanitizeIntent(Intent origIntent) {
        Uri sanitizedUri;
        Intent sanitizedIntent = new Intent(origIntent.getAction());
        Set<String> categories = origIntent.getCategories();
        if (categories != null) {
            for (String category : categories) {
                sanitizedIntent.addCategory(category);
            }
        }
        if (origIntent.getData() == null) {
            sanitizedUri = null;
        } else {
            sanitizedUri = Uri.fromParts(origIntent.getScheme(), "", "");
        }
        sanitizedIntent.setDataAndType(sanitizedUri, origIntent.getType());
        sanitizedIntent.addFlags(origIntent.getFlags());
        sanitizedIntent.setPackage(origIntent.getPackage());
        return sanitizedIntent;
    }

    public static InstantAppResolveInfo.InstantAppDigest parseDigest(Intent origIntent) {
        if (origIntent.getData() != null && !TextUtils.isEmpty(origIntent.getData().getHost())) {
            return new InstantAppResolveInfo.InstantAppDigest(origIntent.getData().getHost(), 5);
        }
        return InstantAppResolveInfo.InstantAppDigest.UNDEFINED;
    }

    /* JADX WARN: Removed duplicated region for block: B:35:0x00a1  */
    /* JADX WARN: Removed duplicated region for block: B:36:0x00be  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public static AuxiliaryResolveInfo doInstantAppResolutionPhaseOne(Computer computer, UserManagerService userManager, InstantAppResolverConnection connection, InstantAppRequest requestObj) {
        Intent origIntent;
        String str;
        int i;
        int resolutionStatus;
        long startTime = System.currentTimeMillis();
        String token = requestObj.token;
        if (DEBUG_INSTANT) {
            Log.d(TAG, "[" + token + "] Phase1; resolving");
        }
        AuxiliaryResolveInfo resolveInfo = null;
        Intent origIntent2 = requestObj.origIntent;
        try {
            List<InstantAppResolveInfo> instantAppResolveInfoList = connection.getInstantAppResolveInfoList(buildRequestInfo(requestObj));
            if (instantAppResolveInfoList == null || instantAppResolveInfoList.size() <= 0) {
                origIntent = origIntent2;
                str = TAG;
                i = 2;
            } else {
                String str2 = requestObj.resolvedType;
                int i2 = requestObj.userId;
                String str3 = origIntent2.getPackage();
                int[] iArr = requestObj.hostDigestPrefixSecure;
                str = TAG;
                i = 2;
                origIntent = origIntent2;
                try {
                    resolveInfo = filterInstantAppIntent(computer, userManager, instantAppResolveInfoList, origIntent2, str2, i2, str3, token, iArr);
                } catch (InstantAppResolverConnection.ConnectionException e) {
                    e = e;
                    if (e.failure == 1) {
                        resolutionStatus = 2;
                    } else if (e.failure == i) {
                        resolutionStatus = 3;
                    } else {
                        resolutionStatus = 1;
                    }
                    if (requestObj.resolveForStart) {
                        logMetrics(899, startTime, token, resolutionStatus);
                    }
                    if (DEBUG_INSTANT) {
                        if (resolutionStatus != i) {
                        }
                    }
                    if (resolveInfo != null) {
                    }
                    return resolveInfo;
                }
            }
            resolutionStatus = 0;
        } catch (InstantAppResolverConnection.ConnectionException e2) {
            e = e2;
            origIntent = origIntent2;
            str = TAG;
            i = 2;
        }
        if (requestObj.resolveForStart && resolutionStatus == 0) {
            logMetrics(899, startTime, token, resolutionStatus);
        }
        if (DEBUG_INSTANT && resolveInfo == null) {
            if (resolutionStatus != i) {
                Log.d(str, "[" + token + "] Phase1; bind timed out");
            } else {
                String str4 = str;
                if (resolutionStatus == 3) {
                    Log.d(str4, "[" + token + "] Phase1; call timed out");
                } else if (resolutionStatus != 0) {
                    Log.d(str4, "[" + token + "] Phase1; service connection error");
                } else {
                    Log.d(str4, "[" + token + "] Phase1; No results matched");
                }
            }
        }
        if (resolveInfo != null && (origIntent.getFlags() & 2048) != 0) {
            return new AuxiliaryResolveInfo(token, false, createFailureIntent(origIntent, token), (List) null, requestObj.hostDigestPrefixSecure);
        }
        return resolveInfo;
    }

    public static void doInstantAppResolutionPhaseTwo(final Context context, final Computer computer, final UserManagerService userManager, InstantAppResolverConnection connection, final InstantAppRequest requestObj, final ActivityInfo instantAppInstaller, Handler callbackHandler) {
        long startTime = System.currentTimeMillis();
        final String token = requestObj.token;
        if (DEBUG_INSTANT) {
            Log.d(TAG, "[" + token + "] Phase2; resolving");
        }
        final Intent origIntent = requestObj.origIntent;
        final Intent sanitizedIntent = sanitizeIntent(origIntent);
        InstantAppResolverConnection.PhaseTwoCallback callback = new InstantAppResolverConnection.PhaseTwoCallback() { // from class: com.android.server.pm.InstantAppResolver.1
            /* JADX INFO: Access modifiers changed from: package-private */
            @Override // com.android.server.pm.InstantAppResolverConnection.PhaseTwoCallback
            public void onPhaseTwoResolved(List<InstantAppResolveInfo> instantAppResolveInfoList, long startTime2) {
                Intent failureIntent;
                if (instantAppResolveInfoList != null && instantAppResolveInfoList.size() > 0) {
                    Computer computer2 = Computer.this;
                    UserManagerService userManagerService = userManager;
                    Intent intent = origIntent;
                    AuxiliaryResolveInfo instantAppIntentInfo = InstantAppResolver.filterInstantAppIntent(computer2, userManagerService, instantAppResolveInfoList, intent, null, 0, intent.getPackage(), token, requestObj.hostDigestPrefixSecure);
                    if (instantAppIntentInfo != null) {
                        failureIntent = instantAppIntentInfo.failureIntent;
                    } else {
                        failureIntent = null;
                    }
                } else {
                    failureIntent = null;
                }
                Intent installerIntent = InstantAppResolver.buildEphemeralInstallerIntent(requestObj.origIntent, sanitizedIntent, failureIntent, requestObj.callingPackage, requestObj.callingFeatureId, requestObj.verificationBundle, requestObj.resolvedType, requestObj.userId, requestObj.responseObj.installFailureActivity, token, false, requestObj.responseObj.filters);
                installerIntent.setComponent(new ComponentName(instantAppInstaller.packageName, instantAppInstaller.name));
                InstantAppResolver.logMetrics(900, startTime2, token, requestObj.responseObj.filters != null ? 0 : 1);
                context.startActivity(installerIntent);
            }
        };
        try {
            connection.getInstantAppIntentFilterList(buildRequestInfo(requestObj), callback, callbackHandler, startTime);
        } catch (InstantAppResolverConnection.ConnectionException e) {
            int resolutionStatus = 1;
            if (e.failure == 1) {
                resolutionStatus = 2;
            }
            logMetrics(900, startTime, token, resolutionStatus);
            if (DEBUG_INSTANT) {
                if (resolutionStatus == 2) {
                    Log.d(TAG, "[" + token + "] Phase2; bind timed out");
                } else {
                    Log.d(TAG, "[" + token + "] Phase2; service connection error");
                }
            }
        }
    }

    public static Intent buildEphemeralInstallerIntent(Intent origIntent, Intent sanitizedIntent, Intent failureIntent, String callingPackage, String callingFeatureId, Bundle verificationBundle, String resolvedType, int userId, ComponentName installFailureActivity, String token, boolean needsPhaseTwo, List<AuxiliaryResolveInfo.AuxiliaryFilter> filters) {
        Intent onFailureIntent;
        int flags = origIntent.getFlags();
        Intent intent = new Intent();
        intent.setFlags(1073741824 | flags | 8388608);
        if (token != null) {
            intent.putExtra("android.intent.extra.INSTANT_APP_TOKEN", token);
        }
        if (origIntent.getData() != null) {
            intent.putExtra("android.intent.extra.INSTANT_APP_HOSTNAME", origIntent.getData().getHost());
        }
        intent.putExtra("android.intent.extra.INSTANT_APP_ACTION", origIntent.getAction());
        intent.putExtra("android.intent.extra.INTENT", sanitizedIntent);
        if (!needsPhaseTwo) {
            if (failureIntent != null || installFailureActivity != null) {
                if (installFailureActivity != null) {
                    try {
                        onFailureIntent = new Intent();
                        onFailureIntent.setComponent(installFailureActivity);
                        if (filters != null && filters.size() == 1) {
                            onFailureIntent.putExtra("android.intent.extra.SPLIT_NAME", filters.get(0).splitName);
                        }
                        onFailureIntent.putExtra("android.intent.extra.INTENT", origIntent);
                    } catch (RemoteException e) {
                    }
                } else {
                    onFailureIntent = failureIntent;
                }
                IIntentSender failureIntentTarget = ActivityManager.getService().getIntentSenderWithFeature(2, callingPackage, callingFeatureId, (IBinder) null, (String) null, 1, new Intent[]{onFailureIntent}, new String[]{resolvedType}, 1409286144, (Bundle) null, userId);
                IntentSender failureSender = new IntentSender(failureIntentTarget);
                intent.putExtra("android.intent.extra.INSTANT_APP_FAILURE", failureSender);
            }
            Intent successIntent = new Intent(origIntent);
            successIntent.setLaunchToken(token);
            try {
                IIntentSender successIntentTarget = ActivityManager.getService().getIntentSenderWithFeature(2, callingPackage, callingFeatureId, (IBinder) null, (String) null, 0, new Intent[]{successIntent}, new String[]{resolvedType}, 1409286144, (Bundle) null, userId);
                IntentSender successSender = new IntentSender(successIntentTarget);
                intent.putExtra("android.intent.extra.INSTANT_APP_SUCCESS", successSender);
            } catch (RemoteException e2) {
            }
            if (verificationBundle != null) {
                intent.putExtra("android.intent.extra.VERIFICATION_BUNDLE", verificationBundle);
            }
            intent.putExtra("android.intent.extra.CALLING_PACKAGE", callingPackage);
            if (filters != null) {
                Bundle[] resolvableFilters = new Bundle[filters.size()];
                int max = filters.size();
                for (int i = 0; i < max; i++) {
                    Bundle resolvableFilter = new Bundle();
                    AuxiliaryResolveInfo.AuxiliaryFilter filter = filters.get(i);
                    resolvableFilter.putBoolean("android.intent.extra.UNKNOWN_INSTANT_APP", filter.resolveInfo != null && filter.resolveInfo.shouldLetInstallerDecide());
                    resolvableFilter.putString("android.intent.extra.PACKAGE_NAME", filter.packageName);
                    resolvableFilter.putString("android.intent.extra.SPLIT_NAME", filter.splitName);
                    resolvableFilter.putLong("android.intent.extra.LONG_VERSION_CODE", filter.versionCode);
                    resolvableFilter.putBundle("android.intent.extra.INSTANT_APP_EXTRAS", filter.extras);
                    resolvableFilters[i] = resolvableFilter;
                    if (i == 0) {
                        intent.putExtras(resolvableFilter);
                        intent.putExtra("android.intent.extra.VERSION_CODE", (int) filter.versionCode);
                    }
                }
                intent.putExtra("android.intent.extra.INSTANT_APP_BUNDLES", resolvableFilters);
            }
            intent.setAction("android.intent.action.INSTALL_INSTANT_APP_PACKAGE");
        } else {
            intent.setAction("android.intent.action.RESOLVE_INSTANT_APP_PACKAGE");
        }
        return intent;
    }

    private static InstantAppRequestInfo buildRequestInfo(InstantAppRequest request) {
        return new InstantAppRequestInfo(sanitizeIntent(request.origIntent), request.hostDigestPrefixSecure, UserHandle.of(request.userId), request.isRequesterInstantApp, request.token);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static AuxiliaryResolveInfo filterInstantAppIntent(Computer computer, UserManagerService userManager, List<InstantAppResolveInfo> instantAppResolveInfoList, Intent origIntent, String resolvedType, int userId, String packageName, String token, int[] hostDigestPrefixSecure) {
        InstantAppResolveInfo.InstantAppDigest digest = parseDigest(origIntent);
        int[] shaPrefix = digest.getDigestPrefix();
        byte[][] digestBytes = digest.getDigestBytes();
        boolean requiresSecondPhase = false;
        ArrayList<AuxiliaryResolveInfo.AuxiliaryFilter> filters = null;
        boolean requiresPrefixMatch = origIntent.isWebIntent() || (shaPrefix.length > 0 && (origIntent.getFlags() & 2048) == 0);
        for (InstantAppResolveInfo instantAppResolveInfo : instantAppResolveInfoList) {
            if (requiresPrefixMatch && instantAppResolveInfo.shouldLetInstallerDecide()) {
                Slog.d(TAG, "InstantAppResolveInfo with mShouldLetInstallerDecide=true when digest required; ignoring");
            } else {
                byte[] filterDigestBytes = instantAppResolveInfo.getDigestBytes();
                if (shaPrefix.length > 0 && (requiresPrefixMatch || filterDigestBytes.length > 0)) {
                    boolean matchFound = false;
                    int i = shaPrefix.length - 1;
                    while (true) {
                        if (i >= 0) {
                            if (Arrays.equals(digestBytes[i], filterDigestBytes)) {
                                matchFound = true;
                                break;
                            } else {
                                i--;
                            }
                        } else {
                            break;
                        }
                    }
                    if (!matchFound) {
                    }
                }
                List<AuxiliaryResolveInfo.AuxiliaryFilter> matchFilters = computeResolveFilters(computer, userManager, origIntent, resolvedType, userId, packageName, token, instantAppResolveInfo);
                if (matchFilters != null) {
                    if (matchFilters.isEmpty()) {
                        requiresSecondPhase = true;
                    }
                    if (filters == null) {
                        filters = new ArrayList<>(matchFilters);
                    } else {
                        filters.addAll(matchFilters);
                    }
                }
            }
        }
        if (filters != null && !filters.isEmpty()) {
            return new AuxiliaryResolveInfo(token, requiresSecondPhase, createFailureIntent(origIntent, token), filters, hostDigestPrefixSecure);
        }
        return null;
    }

    private static Intent createFailureIntent(Intent origIntent, String token) {
        Intent failureIntent = new Intent(origIntent);
        failureIntent.setFlags(failureIntent.getFlags() | 512);
        failureIntent.setFlags(failureIntent.getFlags() & (-2049));
        failureIntent.setLaunchToken(token);
        return failureIntent;
    }

    private static List<AuxiliaryResolveInfo.AuxiliaryFilter> computeResolveFilters(Computer computer, UserManagerService userManager, Intent origIntent, String resolvedType, int userId, String packageName, String token, InstantAppResolveInfo instantAppInfo) {
        List<AuxiliaryResolveInfo.AuxiliaryFilter> list;
        if (instantAppInfo.shouldLetInstallerDecide()) {
            return Collections.singletonList(new AuxiliaryResolveInfo.AuxiliaryFilter(instantAppInfo, (String) null, instantAppInfo.getExtras()));
        }
        if (packageName == null || packageName.equals(instantAppInfo.getPackageName())) {
            List<InstantAppIntentFilter> instantAppFilters = instantAppInfo.getIntentFilters();
            if (instantAppFilters == null) {
                list = null;
            } else if (!instantAppFilters.isEmpty()) {
                ComponentResolver.InstantAppIntentResolver instantAppResolver = new ComponentResolver.InstantAppIntentResolver(userManager);
                for (int j = instantAppFilters.size() - 1; j >= 0; j--) {
                    InstantAppIntentFilter instantAppFilter = instantAppFilters.get(j);
                    List<IntentFilter> splitFilters = instantAppFilter.getFilters();
                    if (splitFilters != null && !splitFilters.isEmpty()) {
                        for (int k = splitFilters.size() - 1; k >= 0; k--) {
                            IntentFilter filter = splitFilters.get(k);
                            Iterator<IntentFilter.AuthorityEntry> authorities = filter.authoritiesIterator();
                            if ((authorities != null && authorities.hasNext()) || ((!filter.hasDataScheme("http") && !filter.hasDataScheme("https")) || !filter.hasAction("android.intent.action.VIEW") || !filter.hasCategory("android.intent.category.BROWSABLE"))) {
                                instantAppResolver.addFilter(computer, new AuxiliaryResolveInfo.AuxiliaryFilter(filter, instantAppInfo, instantAppFilter.getSplitName(), instantAppInfo.getExtras()));
                            }
                        }
                    }
                }
                List<AuxiliaryResolveInfo.AuxiliaryFilter> matchedResolveInfoList = instantAppResolver.queryIntent(computer, origIntent, resolvedType, false, userId);
                if (!matchedResolveInfoList.isEmpty()) {
                    if (DEBUG_INSTANT) {
                        Log.d(TAG, "[" + token + "] Found match(es); " + matchedResolveInfoList);
                    }
                    return matchedResolveInfoList;
                } else if (DEBUG_INSTANT) {
                    Log.d(TAG, "[" + token + "] No matches found package: " + instantAppInfo.getPackageName() + ", versionCode: " + instantAppInfo.getVersionCode());
                    return null;
                } else {
                    return null;
                }
            } else {
                list = null;
            }
            if (origIntent.isWebIntent()) {
                return list;
            }
            if (DEBUG_INSTANT) {
                Log.d(TAG, "No app filters; go to phase 2");
            }
            return Collections.emptyList();
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void logMetrics(int action, long startTime, String token, int status) {
        LogMaker logMaker = new LogMaker(action).setType(4).addTaggedData(901, new Long(System.currentTimeMillis() - startTime)).addTaggedData(903, token).addTaggedData(902, new Integer(status));
        getLogger().write(logMaker);
    }
}
