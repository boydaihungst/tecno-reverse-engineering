package com.android.server.pm;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.ShortcutInfo;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Xml;
import com.android.internal.R;
import com.android.server.pm.ShareTargetInfo;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes2.dex */
public class ShortcutParser {
    private static final boolean DEBUG = false;
    static final String METADATA_KEY = "android.app.shortcuts";
    private static final String TAG = "ShortcutService";
    private static final String TAG_CATEGORIES = "categories";
    private static final String TAG_CATEGORY = "category";
    private static final String TAG_DATA = "data";
    private static final String TAG_INTENT = "intent";
    private static final String TAG_SHARE_TARGET = "share-target";
    private static final String TAG_SHORTCUT = "shortcut";
    private static final String TAG_SHORTCUTS = "shortcuts";

    public static List<ShortcutInfo> parseShortcuts(ShortcutService service, String packageName, int userId, List<ShareTargetInfo> outShareTargets) throws IOException, XmlPullParserException {
        List<ResolveInfo> activities = service.injectGetMainActivities(packageName, userId);
        if (activities != null && activities.size() != 0) {
            outShareTargets.clear();
            try {
                int size = activities.size();
                List<ShortcutInfo> result = null;
                for (int i = 0; i < size; i++) {
                    try {
                        ActivityInfo activityInfoNoMetadata = activities.get(i).activityInfo;
                        if (activityInfoNoMetadata != null) {
                            try {
                                ActivityInfo activityInfoWithMetadata = service.getActivityInfoWithMetadata(activityInfoNoMetadata.getComponentName(), userId);
                                if (activityInfoWithMetadata != null) {
                                    result = parseShortcutsOneFile(service, activityInfoWithMetadata, packageName, userId, result, outShareTargets);
                                }
                            } catch (RuntimeException e) {
                                e = e;
                                service.wtf("Exception caught while parsing shortcut XML for package=" + packageName, e);
                                return null;
                            }
                        }
                    } catch (RuntimeException e2) {
                        e = e2;
                    }
                }
                return result;
            } catch (RuntimeException e3) {
                e = e3;
            }
        }
        return null;
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [347=11, 348=4, 150=5] */
    /* JADX WARN: Code restructure failed: missing block: B:187:0x0448, code lost:
        if (r0 == null) goto L25;
     */
    /* JADX WARN: Code restructure failed: missing block: B:188:0x044a, code lost:
        r0.close();
     */
    /* JADX WARN: Code restructure failed: missing block: B:189:0x044d, code lost:
        return r6;
     */
    /* JADX WARN: Code restructure failed: missing block: B:37:0x00c6, code lost:
        android.util.Log.e(com.android.server.pm.ShortcutParser.TAG, "More than " + r13 + " shortcuts found for " + r26.getComponentName() + ". Skipping the rest.");
     */
    /* JADX WARN: Code restructure failed: missing block: B:38:0x00f1, code lost:
        if (r0 == null) goto L262;
     */
    /* JADX WARN: Code restructure failed: missing block: B:39:0x00f3, code lost:
        r0.close();
     */
    /* JADX WARN: Code restructure failed: missing block: B:40:0x00f6, code lost:
        return r6;
     */
    /* JADX WARN: Removed duplicated region for block: B:197:0x0464  */
    /* JADX WARN: Removed duplicated region for block: B:250:0x020d A[SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:90:0x0217  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private static List<ShortcutInfo> parseShortcutsOneFile(ShortcutService service, ActivityInfo activityInfo, String packageName, int userId, List<ShortcutInfo> result, List<ShareTargetInfo> outShareTargets) throws IOException, XmlPullParserException {
        XmlResourceParser parser;
        ComponentName activity;
        AttributeSet attrs;
        int maxShortcuts;
        ShareTargetInfo currentShareTarget;
        Set<String> categories;
        ArrayList<Intent> intents;
        ArrayList<ShareTargetInfo.TargetData> dataList;
        List<ShortcutInfo> result2;
        int rank;
        int numShortcuts;
        ShortcutInfo currentShortcut;
        int type;
        int depth;
        String tag;
        ComponentName activity2;
        int numShortcuts2;
        List<ShortcutInfo> result3;
        int maxShortcuts2;
        ShareTargetInfo currentShareTarget2;
        Set<String> categories2;
        ShortcutService shortcutService;
        int numShortcuts3;
        ComponentName activity3;
        ShareTargetInfo currentShareTarget3;
        List<ShortcutInfo> result4;
        int maxShortcuts3;
        ComponentName activity4;
        ShortcutService shortcutService2 = service;
        XmlResourceParser parser2 = null;
        try {
            parser = shortcutService2.injectXmlMetaData(activityInfo, METADATA_KEY);
        } catch (Throwable th) {
            th = th;
        }
        if (parser == null) {
            if (parser != null) {
                parser.close();
            }
            return result;
        }
        try {
            activity = new ComponentName(packageName, activityInfo.name);
            attrs = Xml.asAttributeSet(parser);
            maxShortcuts = service.getMaxActivityShortcuts();
            currentShareTarget = null;
            categories = null;
            intents = new ArrayList<>();
            dataList = new ArrayList<>();
            result2 = result;
            rank = 0;
            numShortcuts = 0;
            currentShortcut = null;
        } catch (Throwable th2) {
            th = th2;
            parser2 = parser;
        }
        while (true) {
            try {
                type = parser.next();
            } catch (Throwable th3) {
                th = th3;
                parser2 = parser;
            }
            if (type == 1) {
                break;
            }
            if (type == 3 && parser.getDepth() <= 0) {
                break;
            }
            int depth2 = parser.getDepth();
            String tag2 = parser.getName();
            if (type == 3) {
                depth = depth2;
                if (depth == 2) {
                    tag = tag2;
                    try {
                    } catch (Throwable th4) {
                        th = th4;
                        parser2 = parser;
                    }
                    if (!TAG_SHORTCUT.equals(tag)) {
                        activity2 = activity;
                    } else if (currentShortcut != null) {
                        ShortcutInfo si = currentShortcut;
                        if (!si.isEnabled()) {
                            activity4 = activity;
                            intents.clear();
                            intents.add(new Intent("android.intent.action.VIEW"));
                        } else if (intents.size() == 0) {
                            activity4 = activity;
                            Log.e(TAG, "Shortcut " + si.getId() + " has no intent. Skipping it.");
                            shortcutService2 = service;
                            currentShortcut = null;
                            activity = activity4;
                        } else {
                            activity4 = activity;
                        }
                        if (numShortcuts >= maxShortcuts) {
                            break;
                        }
                        intents.get(0).addFlags(268484608);
                        try {
                            si.setIntents((Intent[]) intents.toArray(new Intent[intents.size()]));
                            intents.clear();
                            if (categories != null) {
                                si.setCategories(categories);
                                categories = null;
                            }
                            if (result2 == null) {
                                result2 = new ArrayList<>();
                            }
                            result2.add(si);
                            numShortcuts++;
                            rank++;
                            shortcutService2 = service;
                            currentShortcut = null;
                            activity = activity4;
                        } catch (RuntimeException e) {
                            Log.e(TAG, "Shortcut's extras contain un-persistable values. Skipping it.");
                        }
                        th = th4;
                        parser2 = parser;
                        if (parser2 != null) {
                            parser2.close();
                        }
                        throw th;
                    } else {
                        numShortcuts2 = numShortcuts;
                        activity2 = activity;
                        shortcutService2 = service;
                        numShortcuts = numShortcuts2;
                        activity = activity2;
                    }
                } else {
                    tag = tag2;
                    activity2 = activity;
                }
            } else {
                depth = depth2;
                tag = tag2;
                activity2 = activity;
            }
            numShortcuts2 = numShortcuts;
            if (type == 3 && depth == 2) {
                try {
                    if (!TAG_SHARE_TARGET.equals(tag)) {
                        result3 = result2;
                        maxShortcuts2 = maxShortcuts;
                    } else if (currentShareTarget == null) {
                        shortcutService2 = service;
                        numShortcuts = numShortcuts2;
                        activity = activity2;
                    } else {
                        ShareTargetInfo sti = currentShareTarget;
                        if (categories == null || categories.isEmpty()) {
                            currentShareTarget3 = null;
                            result4 = result2;
                            maxShortcuts3 = maxShortcuts;
                        } else if (dataList.isEmpty()) {
                            currentShareTarget3 = null;
                            result4 = result2;
                            maxShortcuts3 = maxShortcuts;
                        } else {
                            currentShareTarget2 = null;
                            result3 = result2;
                            try {
                                maxShortcuts2 = maxShortcuts;
                                ShareTargetInfo newShareTarget = new ShareTargetInfo((ShareTargetInfo.TargetData[]) dataList.toArray(new ShareTargetInfo.TargetData[dataList.size()]), sti.mTargetClass, (String[]) categories.toArray(new String[categories.size()]));
                                try {
                                    outShareTargets.add(newShareTarget);
                                    dataList.clear();
                                    categories2 = null;
                                    if (type == 2) {
                                        shortcutService = service;
                                        numShortcuts3 = numShortcuts2;
                                        activity3 = activity2;
                                        result2 = result3;
                                    } else if (depth == 1 && TAG_SHORTCUTS.equals(tag)) {
                                        shortcutService = service;
                                        numShortcuts3 = numShortcuts2;
                                        activity3 = activity2;
                                        result2 = result3;
                                    } else {
                                        if (depth == 2) {
                                            try {
                                                if (TAG_SHORTCUT.equals(tag)) {
                                                    numShortcuts3 = numShortcuts2;
                                                    List<ShortcutInfo> result5 = result3;
                                                    try {
                                                        ShortcutInfo si2 = parseShortcutAttributes(service, attrs, packageName, activity2, userId, rank);
                                                        if (si2 == null) {
                                                            shortcutService = service;
                                                            result2 = result5;
                                                            activity3 = activity2;
                                                        } else {
                                                            if (result5 != null) {
                                                                for (int i = result5.size() - 1; i >= 0; i--) {
                                                                    if (si2.getId().equals(result5.get(i).getId())) {
                                                                        Log.e(TAG, "Duplicate shortcut ID detected. Skipping it.");
                                                                        shortcutService = service;
                                                                        result2 = result5;
                                                                        activity3 = activity2;
                                                                    }
                                                                }
                                                            }
                                                            currentShortcut = si2;
                                                            categories = null;
                                                            shortcutService2 = service;
                                                            result2 = result5;
                                                            activity = activity2;
                                                            currentShareTarget = currentShareTarget2;
                                                            maxShortcuts = maxShortcuts2;
                                                            numShortcuts = numShortcuts3;
                                                        }
                                                    } catch (Throwable th5) {
                                                        th = th5;
                                                        parser2 = parser;
                                                    }
                                                }
                                            } catch (Throwable th6) {
                                                th = th6;
                                                parser2 = parser;
                                            }
                                        }
                                        numShortcuts3 = numShortcuts2;
                                        int depth3 = depth;
                                        result2 = result3;
                                        if (depth3 == 2) {
                                            try {
                                                if (TAG_SHARE_TARGET.equals(tag)) {
                                                    shortcutService = service;
                                                    try {
                                                        ShareTargetInfo sti2 = parseShareTargetAttributes(shortcutService, attrs);
                                                        if (sti2 == null) {
                                                            activity3 = activity2;
                                                        } else {
                                                            currentShareTarget = sti2;
                                                            categories = null;
                                                            dataList.clear();
                                                            shortcutService2 = shortcutService;
                                                            activity = activity2;
                                                            maxShortcuts = maxShortcuts2;
                                                            numShortcuts = numShortcuts3;
                                                        }
                                                    } catch (Throwable th7) {
                                                        th = th7;
                                                        parser2 = parser;
                                                    }
                                                }
                                            } catch (Throwable th8) {
                                                th = th8;
                                                parser2 = parser;
                                            }
                                        }
                                        shortcutService = service;
                                        if (depth3 == 3 && TAG_INTENT.equals(tag)) {
                                            if (currentShortcut == null) {
                                                activity3 = activity2;
                                            } else if (currentShortcut.isEnabled()) {
                                                Intent intent = Intent.parseIntent(shortcutService.mContext.getResources(), parser, attrs);
                                                if (TextUtils.isEmpty(intent.getAction())) {
                                                    ComponentName activity5 = activity2;
                                                    Log.e(TAG, "Shortcut intent action must be provided. activity=" + activity5);
                                                    currentShortcut = null;
                                                    shortcutService2 = shortcutService;
                                                    activity = activity5;
                                                    currentShareTarget = currentShareTarget2;
                                                    maxShortcuts = maxShortcuts2;
                                                    categories = categories2;
                                                    numShortcuts = numShortcuts3;
                                                } else {
                                                    activity3 = activity2;
                                                    intents.add(intent);
                                                }
                                            } else {
                                                activity3 = activity2;
                                            }
                                            Log.e(TAG, "Ignoring excessive intent tag.");
                                        } else {
                                            activity3 = activity2;
                                            if (depth3 == 3 && TAG_CATEGORIES.equals(tag)) {
                                                if (currentShortcut != null && currentShortcut.getCategories() == null) {
                                                    String name = parseCategories(shortcutService, attrs);
                                                    if (TextUtils.isEmpty(name)) {
                                                        Log.e(TAG, "Empty category found. activity=" + activity3);
                                                    } else {
                                                        categories = categories2 == null ? new ArraySet<>() : categories2;
                                                        categories.add(name);
                                                        shortcutService2 = shortcutService;
                                                        activity = activity3;
                                                        currentShareTarget = currentShareTarget2;
                                                        maxShortcuts = maxShortcuts2;
                                                        numShortcuts = numShortcuts3;
                                                    }
                                                }
                                            } else if (depth3 == 3 && TAG_CATEGORY.equals(tag)) {
                                                if (currentShareTarget2 != null) {
                                                    String name2 = parseCategory(shortcutService, attrs);
                                                    if (TextUtils.isEmpty(name2)) {
                                                        Log.e(TAG, "Empty category found. activity=" + activity3);
                                                    } else {
                                                        categories = categories2 == null ? new ArraySet<>() : categories2;
                                                        categories.add(name2);
                                                        shortcutService2 = shortcutService;
                                                        activity = activity3;
                                                        currentShareTarget = currentShareTarget2;
                                                        maxShortcuts = maxShortcuts2;
                                                        numShortcuts = numShortcuts3;
                                                    }
                                                }
                                            } else if (depth3 != 3 || !"data".equals(tag)) {
                                                Log.w(TAG, String.format("Invalid tag '%s' found at depth %d", tag, Integer.valueOf(depth3)));
                                            } else if (currentShareTarget2 != null) {
                                                ShareTargetInfo.TargetData data = parseShareTargetData(shortcutService, attrs);
                                                if (data == null) {
                                                    Log.e(TAG, "Invalid data tag found. activity=" + activity3);
                                                } else {
                                                    dataList.add(data);
                                                }
                                            }
                                        }
                                    }
                                    shortcutService2 = shortcutService;
                                    activity = activity3;
                                    currentShareTarget = currentShareTarget2;
                                    maxShortcuts = maxShortcuts2;
                                    categories = categories2;
                                    numShortcuts = numShortcuts3;
                                } catch (Throwable th9) {
                                    th = th9;
                                    parser2 = parser;
                                    if (parser2 != null) {
                                    }
                                    throw th;
                                }
                            } catch (Throwable th10) {
                                th = th10;
                            }
                        }
                        shortcutService2 = service;
                        numShortcuts = numShortcuts2;
                        activity = activity2;
                        currentShareTarget = currentShareTarget3;
                        result2 = result4;
                        maxShortcuts = maxShortcuts3;
                    }
                } catch (Throwable th11) {
                    th = th11;
                    parser2 = parser;
                }
            } else {
                result3 = result2;
                maxShortcuts2 = maxShortcuts;
            }
            currentShareTarget2 = currentShareTarget;
            categories2 = categories;
            if (type == 2) {
            }
            shortcutService2 = shortcutService;
            activity = activity3;
            currentShareTarget = currentShareTarget2;
            maxShortcuts = maxShortcuts2;
            categories = categories2;
            numShortcuts = numShortcuts3;
        }
    }

    private static String parseCategories(ShortcutService service, AttributeSet attrs) {
        TypedArray sa = service.mContext.getResources().obtainAttributes(attrs, R.styleable.ShortcutCategories);
        try {
            if (sa.getType(0) == 3) {
                return sa.getNonResourceString(0);
            }
            Log.w(TAG, "android:name for shortcut category must be string literal.");
            return null;
        } finally {
            sa.recycle();
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [415=6] */
    private static ShortcutInfo parseShortcutAttributes(ShortcutService service, AttributeSet attrs, String packageName, ComponentName activity, int userId, int rank) {
        TypedArray sa;
        TypedArray sa2 = service.mContext.getResources().obtainAttributes(attrs, R.styleable.Shortcut);
        try {
            try {
                if (sa2.getType(2) != 3) {
                    Log.w(TAG, "android:shortcutId must be string literal. activity=" + activity);
                    sa2.recycle();
                    return null;
                }
                String id = sa2.getNonResourceString(2);
                boolean enabled = sa2.getBoolean(1, true);
                int iconResId = sa2.getResourceId(0, 0);
                int titleResId = sa2.getResourceId(3, 0);
                int textResId = sa2.getResourceId(4, 0);
                int disabledMessageResId = sa2.getResourceId(5, 0);
                int splashScreenThemeResId = sa2.getResourceId(6, 0);
                String splashScreenThemeResName = splashScreenThemeResId != 0 ? service.mContext.getResources().getResourceName(splashScreenThemeResId) : null;
                if (TextUtils.isEmpty(id)) {
                    Log.w(TAG, "android:shortcutId must be provided. activity=" + activity);
                    sa2.recycle();
                    return null;
                } else if (titleResId == 0) {
                    Log.w(TAG, "android:shortcutShortLabel must be provided. activity=" + activity);
                    sa2.recycle();
                    return null;
                } else {
                    sa = sa2;
                    try {
                        ShortcutInfo createShortcutFromManifest = createShortcutFromManifest(service, userId, id, packageName, activity, titleResId, textResId, disabledMessageResId, rank, iconResId, enabled, splashScreenThemeResName);
                        sa.recycle();
                        return createShortcutFromManifest;
                    } catch (Throwable th) {
                        th = th;
                        sa.recycle();
                        throw th;
                    }
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

    private static ShortcutInfo createShortcutFromManifest(ShortcutService service, int userId, String id, String packageName, ComponentName activityComponent, int titleResId, int textResId, int disabledMessageResId, int rank, int iconResId, boolean enabled, String splashScreenThemeResName) {
        int disabledReason;
        int flags = (enabled ? 32 : 64) | 256 | (iconResId != 0 ? 4 : 0);
        if (enabled) {
            disabledReason = 0;
        } else {
            disabledReason = 1;
        }
        return new ShortcutInfo(userId, id, packageName, activityComponent, null, null, titleResId, null, null, textResId, null, null, disabledMessageResId, null, null, null, rank, null, service.injectCurrentTimeMillis(), flags, iconResId, null, null, null, disabledReason, null, null, splashScreenThemeResName, null);
    }

    private static String parseCategory(ShortcutService service, AttributeSet attrs) {
        TypedArray sa = service.mContext.getResources().obtainAttributes(attrs, R.styleable.IntentCategory);
        try {
            if (sa.getType(0) != 3) {
                Log.w(TAG, "android:name must be string literal.");
                return null;
            }
            return sa.getString(0);
        } finally {
            sa.recycle();
        }
    }

    private static ShareTargetInfo parseShareTargetAttributes(ShortcutService service, AttributeSet attrs) {
        TypedArray sa = service.mContext.getResources().obtainAttributes(attrs, R.styleable.Intent);
        try {
            String targetClass = sa.getString(4);
            if (TextUtils.isEmpty(targetClass)) {
                Log.w(TAG, "android:targetClass must be provided.");
                return null;
            }
            return new ShareTargetInfo(null, targetClass, null);
        } finally {
            sa.recycle();
        }
    }

    private static ShareTargetInfo.TargetData parseShareTargetData(ShortcutService service, AttributeSet attrs) {
        TypedArray sa = service.mContext.getResources().obtainAttributes(attrs, R.styleable.AndroidManifestData);
        try {
            if (sa.getType(0) != 3) {
                Log.w(TAG, "android:mimeType must be string literal.");
                return null;
            }
            String scheme = sa.getString(1);
            String host = sa.getString(2);
            String port = sa.getString(3);
            String path = sa.getString(4);
            String pathPattern = sa.getString(6);
            String pathPrefix = sa.getString(5);
            String mimeType = sa.getString(0);
            return new ShareTargetInfo.TargetData(scheme, host, port, path, pathPattern, pathPrefix, mimeType);
        } finally {
            sa.recycle();
        }
    }
}
