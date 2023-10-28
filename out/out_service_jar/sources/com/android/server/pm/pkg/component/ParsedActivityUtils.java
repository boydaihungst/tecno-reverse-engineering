package com.android.server.pm.pkg.component;

import android.app.ActivityTaskManager;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.parsing.result.ParseInput;
import android.content.pm.parsing.result.ParseResult;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.util.ArraySet;
import android.util.AttributeSet;
import android.util.Log;
import com.android.internal.R;
import com.android.internal.util.ArrayUtils;
import com.android.server.pm.pkg.parsing.ParsingPackage;
import com.android.server.pm.pkg.parsing.ParsingPackageUtils;
import com.android.server.pm.pkg.parsing.ParsingUtils;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes2.dex */
public class ParsedActivityUtils {
    public static final boolean LOG_UNSAFE_BROADCASTS = false;
    private static final int RECREATE_ON_CONFIG_CHANGES_MASK = 3;
    public static final Set<String> SAFE_BROADCASTS;
    private static final String TAG = "PackageParsing";

    static {
        ArraySet arraySet = new ArraySet();
        SAFE_BROADCASTS = arraySet;
        arraySet.add("android.intent.action.BOOT_COMPLETED");
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [236=9] */
    /* JADX WARN: Removed duplicated region for block: B:52:0x02cb  */
    /* JADX WARN: Removed duplicated region for block: B:56:0x02fe A[Catch: all -> 0x0306, TRY_ENTER, TRY_LEAVE, TryCatch #5 {all -> 0x0306, blocks: (B:34:0x024f, B:38:0x0289, B:40:0x029d, B:42:0x02a4, B:43:0x02ab, B:45:0x02b3, B:47:0x02ba, B:56:0x02fe, B:64:0x031b), top: B:89:0x024f }] */
    /* JADX WARN: Removed duplicated region for block: B:61:0x0308 A[Catch: all -> 0x0358, TRY_ENTER, TRY_LEAVE, TryCatch #6 {all -> 0x0358, blocks: (B:30:0x00c4, B:54:0x02e5, B:61:0x0308, B:53:0x02cd), top: B:90:0x00c4 }] */
    /* JADX WARN: Removed duplicated region for block: B:83:0x0152 A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public static ParseResult<ParsedActivity> parseActivityOrReceiver(String[] separateProcesses, ParsingPackage pkg, Resources res, XmlResourceParser parser, int flags, boolean useRoundIcon, String defaultSplitName, ParseInput input) throws XmlPullParserException, IOException {
        TypedArray sa;
        String packageName;
        ParsingPackage parsingPackage;
        ParseResult<String> affinityNameResult;
        String packageName2 = pkg.getPackageName();
        ParsedActivityImpl activity = new ParsedActivityImpl();
        boolean receiver = ParsingPackageUtils.TAG_RECEIVER.equals(parser.getName());
        String tag = "<" + parser.getName() + ">";
        TypedArray sa2 = res.obtainAttributes(parser, R.styleable.AndroidManifestActivity);
        try {
            ParseResult<ParsedActivityImpl> result = ParsedMainComponentUtils.parseMainComponent(activity, tag, separateProcesses, pkg, sa2, flags, useRoundIcon, defaultSplitName, input, 30, 17, 42, 5, 2, 1, 23, 3, 7, 44, 48, 57);
            if (result.isError()) {
                try {
                    ParseResult<ParsedActivity> error = input.error(result);
                    sa2.recycle();
                    return error;
                } catch (Throwable th) {
                    th = th;
                    sa = sa2;
                }
            } else {
                try {
                    try {
                        if (receiver) {
                            try {
                                if (pkg.isCantSaveState()) {
                                    packageName = packageName2;
                                    try {
                                        if (Objects.equals(activity.getProcessName(), packageName)) {
                                            ParseResult<ParsedActivity> error2 = input.error("Heavy-weight applications can not have receivers in main process");
                                            sa2.recycle();
                                            return error2;
                                        }
                                        activity.setTheme(sa2.getResourceId(0, 0)).setUiOptions(sa2.getInt(26, pkg.getUiOptions()));
                                        activity.setFlags(activity.getFlags() | ComponentParseUtils.flag(64, 19, pkg.isAllowTaskReparenting(), sa2) | ComponentParseUtils.flag(8, 18, sa2) | ComponentParseUtils.flag(4, 11, sa2) | ComponentParseUtils.flag(32, 13, sa2) | ComponentParseUtils.flag(256, 22, sa2) | ComponentParseUtils.flag(2, 10, sa2) | ComponentParseUtils.flag(2048, 24, sa2) | ComponentParseUtils.flag(1, 9, sa2) | ComponentParseUtils.flag(128, 21, sa2) | ComponentParseUtils.flag(1024, 39, sa2) | ComponentParseUtils.flag(1024, 29, sa2) | ComponentParseUtils.flag(16, 12, sa2) | ComponentParseUtils.flag(536870912, 63, sa2));
                                        if (receiver) {
                                            try {
                                                activity.setFlags(activity.getFlags() | ComponentParseUtils.flag(512, 25, pkg.isBaseHardwareAccelerated(), sa2) | ComponentParseUtils.flag(Integer.MIN_VALUE, 31, sa2) | ComponentParseUtils.flag(262144, 62, sa2) | ComponentParseUtils.flag(8192, 35, sa2) | ComponentParseUtils.flag(4096, 36, sa2) | ComponentParseUtils.flag(16384, 37, sa2) | ComponentParseUtils.flag(8388608, 51, sa2) | ComponentParseUtils.flag(4194304, 41, sa2) | ComponentParseUtils.flag(16777216, 52, sa2) | ComponentParseUtils.flag(33554432, 56, sa2) | ComponentParseUtils.flag(268435456, 60, sa2));
                                                activity.setPrivateFlags(activity.getPrivateFlags() | ComponentParseUtils.flag(1, 54, sa2) | ComponentParseUtils.flag(2, 58, true, sa2));
                                                activity.setColorMode(sa2.getInt(49, 0)).setDocumentLaunchMode(sa2.getInt(33, 0)).setLaunchMode(sa2.getInt(14, 0)).setLockTaskLaunchMode(sa2.getInt(38, 0)).setMaxRecents(sa2.getInt(34, ActivityTaskManager.getDefaultAppRecentsLimitStatic())).setPersistableMode(sa2.getInteger(32, 0)).setRequestedVrComponent(sa2.getString(43)).setRotationAnimation(sa2.getInt(46, -1)).setSoftInputMode(sa2.getInt(20, 0)).setConfigChanges(getActivityConfigChanges(sa2.getInt(16, 0), sa2.getInt(47, 0)));
                                                if ("com.android.quickstep.recents_ui_overrides.src.com.android.launcher3.uioverrides.QuickstepLauncher".equals(activity.getName())) {
                                                    try {
                                                        activity.setConfigChanges(activity.getConfigChanges() | Integer.MIN_VALUE);
                                                        Log.i("PackageParsing", "configChanges-ParsedActivityUtils-config-after:" + activity.getConfigChanges() + "  /name:" + activity.getName());
                                                    } catch (Throwable th2) {
                                                        th = th2;
                                                        sa = sa2;
                                                        sa.recycle();
                                                        throw th;
                                                    }
                                                }
                                                int screenOrientation = sa2.getInt(15, -1);
                                                parsingPackage = pkg;
                                                int resizeMode = getActivityResizeMode(parsingPackage, sa2, screenOrientation);
                                                activity.setScreenOrientation(screenOrientation).setResizeMode(resizeMode);
                                                if (sa2.hasValue(50) && sa2.getType(50) == 4) {
                                                    activity.setMaxAspectRatio(resizeMode, sa2.getFloat(50, 0.0f));
                                                }
                                                if (sa2.hasValue(53) && sa2.getType(53) == 4) {
                                                    activity.setMinAspectRatio(resizeMode, sa2.getFloat(53, 0.0f));
                                                }
                                            } catch (Throwable th3) {
                                                th = th3;
                                            }
                                        } else {
                                            parsingPackage = pkg;
                                            activity.setLaunchMode(0).setConfigChanges(0).setFlags(activity.getFlags() | ComponentParseUtils.flag(1073741824, 28, sa2));
                                        }
                                        String taskAffinity = sa2.getNonConfigurationString(8, 1024);
                                        affinityNameResult = ComponentParseUtils.buildTaskAffinityName(packageName, pkg.getTaskAffinity(), taskAffinity, input);
                                        if (!affinityNameResult.isError()) {
                                            ParseResult<ParsedActivity> error3 = input.error(affinityNameResult);
                                            sa2.recycle();
                                            return error3;
                                        }
                                        activity.setTaskAffinity((String) affinityNameResult.getResult());
                                        boolean visibleToEphemeral = sa2.getBoolean(45, false);
                                        if (visibleToEphemeral) {
                                            activity.setFlags(activity.getFlags() | 1048576);
                                            parsingPackage.setVisibleToInstantApps(true);
                                        }
                                        sa = sa2;
                                        try {
                                            ParseResult<ParsedActivity> parseActivityOrAlias = parseActivityOrAlias(activity, pkg, tag, parser, res, sa2, receiver, false, visibleToEphemeral, input, 27, 4, 6);
                                            sa.recycle();
                                            return parseActivityOrAlias;
                                        } catch (Throwable th4) {
                                            th = th4;
                                        }
                                    } catch (Throwable th5) {
                                        th = th5;
                                        sa = sa2;
                                    }
                                }
                            } catch (Throwable th6) {
                                th = th6;
                                sa = sa2;
                            }
                        }
                        activity.setTheme(sa2.getResourceId(0, 0)).setUiOptions(sa2.getInt(26, pkg.getUiOptions()));
                        activity.setFlags(activity.getFlags() | ComponentParseUtils.flag(64, 19, pkg.isAllowTaskReparenting(), sa2) | ComponentParseUtils.flag(8, 18, sa2) | ComponentParseUtils.flag(4, 11, sa2) | ComponentParseUtils.flag(32, 13, sa2) | ComponentParseUtils.flag(256, 22, sa2) | ComponentParseUtils.flag(2, 10, sa2) | ComponentParseUtils.flag(2048, 24, sa2) | ComponentParseUtils.flag(1, 9, sa2) | ComponentParseUtils.flag(128, 21, sa2) | ComponentParseUtils.flag(1024, 39, sa2) | ComponentParseUtils.flag(1024, 29, sa2) | ComponentParseUtils.flag(16, 12, sa2) | ComponentParseUtils.flag(536870912, 63, sa2));
                        if (receiver) {
                        }
                        String taskAffinity2 = sa2.getNonConfigurationString(8, 1024);
                        affinityNameResult = ComponentParseUtils.buildTaskAffinityName(packageName, pkg.getTaskAffinity(), taskAffinity2, input);
                        if (!affinityNameResult.isError()) {
                        }
                    } catch (Throwable th7) {
                        th = th7;
                        sa = sa2;
                    }
                } catch (Throwable th8) {
                    th = th8;
                    sa = sa2;
                }
                packageName = packageName2;
            }
        } catch (Throwable th9) {
            th = th9;
            sa = sa2;
        }
        sa.recycle();
        throw th;
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [309=7] */
    public static ParseResult<ParsedActivity> parseActivityAlias(ParsingPackage pkg, Resources res, XmlResourceParser parser, boolean useRoundIcon, String defaultSplitName, ParseInput input) throws XmlPullParserException, IOException {
        TypedArray sa;
        ParsedActivity target;
        TypedArray sa2 = res.obtainAttributes(parser, R.styleable.AndroidManifestActivityAlias);
        try {
            String targetActivity = sa2.getNonConfigurationString(7, 1024);
            try {
                if (targetActivity == null) {
                    ParseResult<ParsedActivity> error = input.error("<activity-alias> does not specify android:targetActivity");
                    sa2.recycle();
                    return error;
                }
                String packageName = pkg.getPackageName();
                String targetActivity2 = ParsingUtils.buildClassName(packageName, targetActivity);
                if (targetActivity2 == null) {
                    ParseResult<ParsedActivity> error2 = input.error("Empty class name in package " + packageName);
                    sa2.recycle();
                    return error2;
                }
                List<ParsedActivity> activities = pkg.getActivities();
                int activitiesSize = ArrayUtils.size(activities);
                int i = 0;
                while (true) {
                    if (i >= activitiesSize) {
                        target = null;
                        break;
                    }
                    ParsedActivity t = activities.get(i);
                    if (targetActivity2.equals(t.getName())) {
                        target = t;
                        break;
                    }
                    i++;
                }
                if (target == null) {
                    ParseResult<ParsedActivity> error3 = input.error("<activity-alias> target activity " + targetActivity2 + " not found in manifest with activities = " + pkg.getActivities() + ", parsedActivities = " + activities);
                    sa2.recycle();
                    return error3;
                }
                ParsedActivityImpl activity = ParsedActivityImpl.makeAlias(targetActivity2, target);
                String tag = "<" + parser.getName() + ">";
                sa = sa2;
                try {
                    ParseResult<ParsedActivityImpl> result = ParsedMainComponentUtils.parseMainComponent(activity, tag, null, pkg, sa2, 0, useRoundIcon, defaultSplitName, input, 10, 6, -1, 4, 1, 0, 8, 2, -1, 11, -1, 12);
                    if (result.isError()) {
                        ParseResult<ParsedActivity> error4 = input.error(result);
                        sa.recycle();
                        return error4;
                    }
                    boolean visibleToEphemeral = (activity.getFlags() & 1048576) != 0;
                    ParseResult<ParsedActivity> parseActivityOrAlias = parseActivityOrAlias(activity, pkg, tag, parser, res, sa, false, true, visibleToEphemeral, input, 9, 3, 5);
                    sa.recycle();
                    return parseActivityOrAlias;
                } catch (Throwable th) {
                    th = th;
                    sa.recycle();
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
                sa = sa2;
            }
        } catch (Throwable th3) {
            th = th3;
            sa = sa2;
        }
    }

    /* JADX WARN: Code restructure failed: missing block: B:100:0x0230, code lost:
        r22.setWindowLayout((android.content.pm.ActivityInfo.WindowLayout) r1.getResult());
     */
    /* JADX WARN: Code restructure failed: missing block: B:101:0x0239, code lost:
        if (r16 != false) goto L64;
     */
    /* JADX WARN: Code restructure failed: missing block: B:103:0x0243, code lost:
        if (r22.getIntents().size() <= 0) goto L56;
     */
    /* JADX WARN: Code restructure failed: missing block: B:105:0x0246, code lost:
        r6 = r17;
     */
    /* JADX WARN: Code restructure failed: missing block: B:106:0x0248, code lost:
        r2 = r6;
     */
    /* JADX WARN: Code restructure failed: missing block: B:107:0x0249, code lost:
        if (r2 == false) goto L63;
     */
    /* JADX WARN: Code restructure failed: missing block: B:108:0x024b, code lost:
        r3 = r31.deferError(r22.getName() + ": Targeting S+ (version 31 and above) requires that an explicit value for android:exported be defined when intent filters are present", 150232615);
     */
    /* JADX WARN: Code restructure failed: missing block: B:109:0x0279, code lost:
        if (r3.isError() == false) goto L63;
     */
    /* JADX WARN: Code restructure failed: missing block: B:111:0x027f, code lost:
        return r31.error(r3);
     */
    /* JADX WARN: Code restructure failed: missing block: B:112:0x0280, code lost:
        r22.setExported(r2);
     */
    /* JADX WARN: Code restructure failed: missing block: B:114:0x0287, code lost:
        return r31.success(r22);
     */
    /* JADX WARN: Code restructure failed: missing block: B:80:0x01d4, code lost:
        if (r29 != false) goto L42;
     */
    /* JADX WARN: Code restructure failed: missing block: B:82:0x01db, code lost:
        if (r22.getLaunchMode() == 4) goto L42;
     */
    /* JADX WARN: Code restructure failed: missing block: B:84:0x01e7, code lost:
        if (r22.getMetaData().containsKey(com.android.server.pm.pkg.parsing.ParsingPackageUtils.METADATA_ACTIVITY_LAUNCH_MODE) == false) goto L42;
     */
    /* JADX WARN: Code restructure failed: missing block: B:85:0x01e9, code lost:
        r1 = r22.getMetaData().getString(com.android.server.pm.pkg.parsing.ParsingPackageUtils.METADATA_ACTIVITY_LAUNCH_MODE);
     */
    /* JADX WARN: Code restructure failed: missing block: B:86:0x01f1, code lost:
        if (r1 == null) goto L42;
     */
    /* JADX WARN: Code restructure failed: missing block: B:88:0x01fa, code lost:
        if (r1.equals("singleInstancePerTask") == false) goto L42;
     */
    /* JADX WARN: Code restructure failed: missing block: B:89:0x01fc, code lost:
        r22.setLaunchMode(4);
     */
    /* JADX WARN: Code restructure failed: missing block: B:90:0x01ff, code lost:
        if (r29 != false) goto L48;
     */
    /* JADX WARN: Code restructure failed: missing block: B:91:0x0201, code lost:
        r1 = r27.getBoolean(59, true);
     */
    /* JADX WARN: Code restructure failed: missing block: B:92:0x0211, code lost:
        if (r22.getMetaData().getBoolean(com.android.server.pm.pkg.parsing.ParsingPackageUtils.METADATA_CAN_DISPLAY_ON_REMOTE_DEVICES, true) != false) goto L46;
     */
    /* JADX WARN: Code restructure failed: missing block: B:93:0x0213, code lost:
        r1 = false;
     */
    /* JADX WARN: Code restructure failed: missing block: B:94:0x0214, code lost:
        if (r1 == false) goto L48;
     */
    /* JADX WARN: Code restructure failed: missing block: B:95:0x0216, code lost:
        r22.setFlags(r22.getFlags() | 65536);
     */
    /* JADX WARN: Code restructure failed: missing block: B:96:0x0220, code lost:
        r1 = resolveActivityWindowLayout(r22, r31);
     */
    /* JADX WARN: Code restructure failed: missing block: B:97:0x0229, code lost:
        if (r1.isError() == false) goto L52;
     */
    /* JADX WARN: Code restructure failed: missing block: B:99:0x022f, code lost:
        return r31.error(r1);
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private static ParseResult<ParsedActivity> parseActivityOrAlias(ParsedActivityImpl activity, ParsingPackage pkg, String tag, XmlResourceParser parser, Resources resources, TypedArray array, boolean isReceiver, boolean isAlias, boolean visibleToEphemeral, ParseInput input, int parentActivityNameAttr, int permissionAttr, int exportedAttr) throws IOException, XmlPullParserException {
        int i;
        boolean z;
        int depth;
        ParseResult<Set<String>> knownActivityEmbeddingCertsResult;
        String permission;
        boolean z2;
        ParseResult result;
        ParsedIntentInfoImpl intent;
        ParsedIntentInfoImpl intentInfo;
        String parentActivityName = array.getNonConfigurationString(parentActivityNameAttr, 1024);
        if (parentActivityName != null) {
            String packageName = pkg.getPackageName();
            String parentClassName = ParsingUtils.buildClassName(packageName, parentActivityName);
            if (parentClassName == null) {
                Log.e("PackageParsing", "Activity " + activity.getName() + " specified invalid parentActivityName " + parentActivityName);
            } else {
                activity.setParentActivityName(parentClassName);
            }
        }
        String permission2 = array.getNonConfigurationString(permissionAttr, 0);
        if (!isAlias) {
            activity.setPermission(permission2 != null ? permission2 : pkg.getPermission());
        } else {
            activity.setPermission(permission2);
        }
        if (isAlias) {
            i = 14;
        } else {
            i = 61;
        }
        ParseResult<Set<String>> knownActivityEmbeddingCertsResult2 = ParsingUtils.parseKnownActivityEmbeddingCerts(array, resources, i, input);
        if (knownActivityEmbeddingCertsResult2.isError()) {
            return input.error(knownActivityEmbeddingCertsResult2);
        }
        Set<String> knownActivityEmbeddingCerts = (Set) knownActivityEmbeddingCertsResult2.getResult();
        if (knownActivityEmbeddingCerts != null) {
            activity.setKnownActivityEmbeddingCerts(knownActivityEmbeddingCerts);
        }
        boolean setExported = array.hasValue(exportedAttr);
        if (setExported) {
            activity.setExported(array.getBoolean(exportedAttr, false));
        }
        int depth2 = parser.getDepth();
        while (true) {
            int type = parser.next();
            boolean z3 = true;
            if (type == 1) {
                z = false;
                break;
            } else if (type == 3 && parser.getDepth() <= depth2) {
                z = false;
                break;
            } else if (type == 2) {
                if (parser.getName().equals("intent-filter")) {
                    depth = depth2;
                    knownActivityEmbeddingCertsResult = knownActivityEmbeddingCertsResult2;
                    permission = permission2;
                    z2 = false;
                    ParseResult intentResult = parseIntentFilter(pkg, activity, !isReceiver, visibleToEphemeral, resources, parser, input);
                    if (intentResult.isSuccess() && (intentInfo = (ParsedIntentInfoImpl) intentResult.getResult()) != null) {
                        IntentFilter intentFilter = intentInfo.getIntentFilter();
                        activity.setOrder(Math.max(intentFilter.getOrder(), activity.getOrder()));
                        activity.addIntent(intentInfo);
                    }
                    result = intentResult;
                } else {
                    depth = depth2;
                    knownActivityEmbeddingCertsResult = knownActivityEmbeddingCertsResult2;
                    permission = permission2;
                    z2 = false;
                    if (parser.getName().equals("meta-data")) {
                        result = ParsedComponentUtils.addMetaData(activity, pkg, resources, parser, input);
                    } else if (parser.getName().equals("property")) {
                        result = ParsedComponentUtils.addProperty(activity, pkg, resources, parser, input);
                    } else if (!isReceiver && !isAlias && parser.getName().equals("preferred")) {
                        ParseResult intentResult2 = parseIntentFilter(pkg, activity, true, visibleToEphemeral, resources, parser, input);
                        if (intentResult2.isSuccess() && (intent = (ParsedIntentInfoImpl) intentResult2.getResult()) != null) {
                            pkg.addPreferredActivityFilter(activity.getClassName(), intent);
                        }
                        result = intentResult2;
                    } else if (!isReceiver && !isAlias && parser.getName().equals("layout")) {
                        ParseResult layoutResult = parseActivityWindowLayout(resources, parser, input);
                        if (layoutResult.isSuccess()) {
                            activity.setWindowLayout((ActivityInfo.WindowLayout) layoutResult.getResult());
                        }
                        result = layoutResult;
                    } else {
                        result = ParsingUtils.unknownTag(tag, pkg, parser, input);
                    }
                }
                if (result.isError()) {
                    return input.error(result);
                }
                depth2 = depth;
                knownActivityEmbeddingCertsResult2 = knownActivityEmbeddingCertsResult;
                permission2 = permission;
            }
        }
    }

    private static ParseResult<ParsedIntentInfoImpl> parseIntentFilter(ParsingPackage pkg, ParsedActivityImpl activity, boolean allowImplicitEphemeralVisibility, boolean visibleToEphemeral, Resources resources, XmlResourceParser parser, ParseInput input) throws IOException, XmlPullParserException {
        ParseResult<ParsedIntentInfoImpl> result = ParsedMainComponentUtils.parseIntentFilter(activity, pkg, resources, parser, visibleToEphemeral, true, true, allowImplicitEphemeralVisibility, true, input);
        if (result.isError()) {
            return input.error(result);
        }
        ParsedIntentInfoImpl intent = (ParsedIntentInfoImpl) result.getResult();
        if (intent != null) {
            IntentFilter intentFilter = intent.getIntentFilter();
            if (intentFilter.isVisibleToInstantApp()) {
                activity.setFlags(activity.getFlags() | 1048576);
            }
            if (intentFilter.isImplicitlyVisibleToInstantApp()) {
                activity.setFlags(activity.getFlags() | 2097152);
            }
        }
        return input.success(intent);
    }

    private static int getActivityResizeMode(ParsingPackage pkg, TypedArray sa, int screenOrientation) {
        Boolean resizeableActivity = pkg.getResizeableActivity();
        boolean z = true;
        if (sa.hasValue(40) || resizeableActivity != null) {
            if (resizeableActivity == null || !resizeableActivity.booleanValue()) {
                z = false;
            }
            if (!sa.getBoolean(40, z)) {
                return 0;
            }
            return 2;
        } else if (pkg.isResizeableActivityViaSdkVersion()) {
            return 1;
        } else {
            if (ActivityInfo.isFixedOrientationPortrait(screenOrientation)) {
                return 6;
            }
            if (ActivityInfo.isFixedOrientationLandscape(screenOrientation)) {
                return 5;
            }
            if (screenOrientation == 14) {
                return 7;
            }
            return 4;
        }
    }

    private static ParseResult<ActivityInfo.WindowLayout> parseActivityWindowLayout(Resources res, AttributeSet attrs, ParseInput input) {
        TypedArray sw = res.obtainAttributes(attrs, R.styleable.AndroidManifestLayout);
        int width = -1;
        float widthFraction = -1.0f;
        int height = -1;
        float heightFraction = -1.0f;
        try {
            int widthType = sw.getType(3);
            if (widthType == 6) {
                widthFraction = sw.getFraction(3, 1, 1, -1.0f);
            } else if (widthType == 5) {
                width = sw.getDimensionPixelSize(3, -1);
            }
            int heightType = sw.getType(4);
            if (heightType == 6) {
                heightFraction = sw.getFraction(4, 1, 1, -1.0f);
            } else if (heightType == 5) {
                height = sw.getDimensionPixelSize(4, -1);
            }
            int gravity = sw.getInt(0, 17);
            int minWidth = sw.getDimensionPixelSize(1, -1);
            int minHeight = sw.getDimensionPixelSize(2, -1);
            String windowLayoutAffinity = sw.getNonConfigurationString(5, 0);
            ActivityInfo.WindowLayout windowLayout = new ActivityInfo.WindowLayout(width, widthFraction, height, heightFraction, gravity, minWidth, minHeight, windowLayoutAffinity);
            try {
                ParseResult<ActivityInfo.WindowLayout> success = input.success(windowLayout);
                sw.recycle();
                return success;
            } catch (Throwable th) {
                th = th;
                sw.recycle();
                throw th;
            }
        } catch (Throwable th2) {
            th = th2;
        }
    }

    private static ParseResult<ActivityInfo.WindowLayout> resolveActivityWindowLayout(ParsedActivity activity, ParseInput input) {
        if (!activity.getMetaData().containsKey(ParsingPackageUtils.METADATA_ACTIVITY_WINDOW_LAYOUT_AFFINITY)) {
            return input.success(activity.getWindowLayout());
        }
        if (activity.getWindowLayout() != null && activity.getWindowLayout().windowLayoutAffinity != null) {
            return input.success(activity.getWindowLayout());
        }
        String windowLayoutAffinity = activity.getMetaData().getString(ParsingPackageUtils.METADATA_ACTIVITY_WINDOW_LAYOUT_AFFINITY);
        ActivityInfo.WindowLayout layout = activity.getWindowLayout();
        if (layout == null) {
            layout = new ActivityInfo.WindowLayout(-1, -1.0f, -1, -1.0f, 0, -1, -1, windowLayoutAffinity);
        } else {
            layout.windowLayoutAffinity = windowLayoutAffinity;
        }
        return input.success(layout);
    }

    public static int getActivityConfigChanges(int configChanges, int recreateOnConfigChanges) {
        return ((~recreateOnConfigChanges) & 3) | configChanges;
    }
}
