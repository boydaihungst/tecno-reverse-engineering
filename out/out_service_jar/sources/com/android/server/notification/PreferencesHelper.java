package com.android.server.notification;

import android.app.ActivityManager;
import android.app.AppOpsManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ParceledListSlice;
import android.content.pm.UserInfo;
import android.metrics.LogMaker;
import android.os.Binder;
import android.os.UserHandle;
import android.provider.Settings;
import android.service.notification.ConversationChannelWrapper;
import android.service.notification.NotificationListenerService;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.IntArray;
import android.util.Pair;
import android.util.Slog;
import android.util.SparseBooleanArray;
import android.util.StatsEvent;
import android.util.TypedXmlPullParser;
import android.util.TypedXmlSerializer;
import android.util.proto.ProtoOutputStream;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.util.FrameworkStatsLog;
import com.android.internal.util.Preconditions;
import com.android.internal.util.XmlUtils;
import com.android.server.notification.NotificationManagerService;
import com.android.server.notification.PermissionHelper;
import com.android.server.notification.SysUiStatsEvent;
import com.android.server.pm.verify.domain.DomainVerificationLegacySettings;
import com.android.server.usage.UnixCalendar;
import defpackage.CompanionAppsPermissions;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes2.dex */
public class PreferencesHelper implements RankingConfig {
    private static final String ATT_ALLOW_BUBBLE = "allow_bubble";
    private static final String ATT_APP_USER_LOCKED_FIELDS = "app_user_locked_fields";
    private static final String ATT_ENABLED = "enabled";
    private static final String ATT_HIDE_SILENT = "hide_gentle";
    private static final String ATT_ID = "id";
    private static final String ATT_IMPORTANCE = "importance";
    private static final String ATT_NAME = "name";
    private static final String ATT_PLAY_SOUND = "play_sound";
    private static final String ATT_PLAY_VIBRATION = "play_vibration";
    private static final String ATT_PRIORITY = "priority";
    private static final String ATT_SENT_INVALID_MESSAGE = "sent_invalid_msg";
    private static final String ATT_SENT_VALID_MESSAGE = "sent_valid_msg";
    private static final String ATT_SHOW_BADGE = "show_badge";
    private static final String ATT_UID = "uid";
    private static final String ATT_USER_ALLOWED = "allowed";
    private static final String ATT_USER_DEMOTED_INVALID_MSG_APP = "user_demote_msg_app";
    private static final String ATT_VERSION = "version";
    private static final String ATT_VISIBILITY = "visibility";
    private static final boolean DEFAULT_APP_LOCKED_IMPORTANCE = false;
    static final boolean DEFAULT_BUBBLES_ENABLED = true;
    static final int DEFAULT_BUBBLE_PREFERENCE = 0;
    static final boolean DEFAULT_HIDE_SILENT_STATUS_BAR_ICONS = false;
    private static final int DEFAULT_IMPORTANCE = -1000;
    private static final int DEFAULT_LOCKED_APP_FIELDS = 0;
    static final boolean DEFAULT_MEDIA_NOTIFICATION_FILTERING = true;
    private static final boolean DEFAULT_PLAY_SOUND = true;
    private static final boolean DEFAULT_PLAY_VIBRATION = true;
    private static final int DEFAULT_PRIORITY = 0;
    private static final boolean DEFAULT_SHOW_BADGE = true;
    private static final int DEFAULT_VISIBILITY = -1000;
    private static final String NON_BLOCKABLE_CHANNEL_DELIM = ":";
    static final int NOTIFICATION_CHANNEL_COUNT_LIMIT = 5000;
    private static final int NOTIFICATION_CHANNEL_DELETION_RETENTION_DAYS = 30;
    static final int NOTIFICATION_CHANNEL_GROUP_COUNT_LIMIT = 6000;
    private static final int NOTIFICATION_CHANNEL_GROUP_PULL_LIMIT = 1000;
    private static final int NOTIFICATION_CHANNEL_PULL_LIMIT = 2000;
    private static final int NOTIFICATION_PREFERENCES_PULL_LIMIT = 1000;
    private static final String TAG = "NotificationPrefHelper";
    private static final String TAG_CHANNEL = "channel";
    private static final String TAG_DELEGATE = "delegate";
    private static final String TAG_GROUP = "channelGroup";
    private static final String TAG_PACKAGE = "package";
    static final String TAG_RANKING = "ranking";
    private static final String TAG_STATUS_ICONS = "silent_status_icons";
    static final int UNKNOWN_UID = -10000;
    private static final int XML_VERSION_BUBBLES_UPGRADE = 1;
    private static final int XML_VERSION_NOTIF_PERMISSION = 3;
    private static final int XML_VERSION_REVIEW_PERMISSIONS_NOTIFICATION = 4;
    private final AppOpsManager mAppOps;
    private boolean mAreChannelsBypassingDnd;
    private SparseBooleanArray mBadgingEnabled;
    private SparseBooleanArray mBubblesEnabled;
    private final Context mContext;
    private SparseBooleanArray mLockScreenPrivateNotifications;
    private SparseBooleanArray mLockScreenShowNotifications;
    private final NotificationChannelLogger mNotificationChannelLogger;
    private final PermissionHelper mPermissionHelper;
    private final PackageManager mPm;
    private final RankingHandler mRankingHandler;
    private boolean mShowReviewPermissionsNotification;
    private final SysUiStatsEvent.BuilderFactory mStatsEventBuilderFactory;
    private final ZenModeHelper mZenModeHelper;
    private final ArrayMap<String, PackagePreferences> mPackagePreferences = new ArrayMap<>();
    private final ArrayMap<String, PackagePreferences> mRestoredWithoutUids = new ArrayMap<>();
    private boolean mIsMediaNotificationFilteringEnabled = true;
    private boolean mHideSilentStatusBarIcons = false;
    private boolean mAllowInvalidShortcuts = false;
    private final int XML_VERSION = 4;

    /* loaded from: classes2.dex */
    public @interface LockableAppFields {
        public static final int USER_LOCKED_BUBBLE = 2;
        public static final int USER_LOCKED_IMPORTANCE = 1;
    }

    public PreferencesHelper(Context context, PackageManager pm, RankingHandler rankingHandler, ZenModeHelper zenHelper, PermissionHelper permHelper, NotificationChannelLogger notificationChannelLogger, AppOpsManager appOpsManager, SysUiStatsEvent.BuilderFactory statsEventBuilderFactory, boolean showReviewPermissionsNotification) {
        this.mContext = context;
        this.mZenModeHelper = zenHelper;
        this.mRankingHandler = rankingHandler;
        this.mPermissionHelper = permHelper;
        this.mPm = pm;
        this.mNotificationChannelLogger = notificationChannelLogger;
        this.mAppOps = appOpsManager;
        this.mStatsEventBuilderFactory = statsEventBuilderFactory;
        this.mShowReviewPermissionsNotification = showReviewPermissionsNotification;
        updateBadgingEnabled();
        updateBubblesEnabled();
        updateMediaNotificationFilteringEnabled();
        syncChannelsBypassingDnd();
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [270=6] */
    public void readXml(TypedXmlPullParser parser, boolean forRestore, int userId) throws XmlPullParserException, IOException {
        ArrayMap<String, PackagePreferences> arrayMap;
        int type;
        String tag;
        int type2;
        int i;
        if (parser.getEventType() == 2 && TAG_RANKING.equals(parser.getName())) {
            int xmlVersion = parser.getAttributeInt((String) null, ATT_VERSION, -1);
            boolean upgradeForBubbles = xmlVersion == 1;
            int i2 = 3;
            boolean migrateToPermission = xmlVersion < 3;
            if (this.mShowReviewPermissionsNotification && xmlVersion < 4) {
                Settings.Global.putInt(this.mContext.getContentResolver(), "review_permissions_notification_state", 0);
            }
            ArrayList<PermissionHelper.PackagePermission> pkgPerms = new ArrayList<>();
            ArrayMap<String, PackagePreferences> arrayMap2 = this.mPackagePreferences;
            synchronized (arrayMap2) {
                while (true) {
                    try {
                        int type3 = parser.next();
                        if (type3 == 1) {
                            type = type3;
                            arrayMap = arrayMap2;
                            break;
                        }
                        try {
                            String tag2 = parser.getName();
                            if (type3 == i2) {
                                try {
                                    if (TAG_RANKING.equals(tag2)) {
                                        type = type3;
                                        arrayMap = arrayMap2;
                                        break;
                                    }
                                } catch (Throwable th) {
                                    th = th;
                                    arrayMap = arrayMap2;
                                    while (true) {
                                        try {
                                            break;
                                        } catch (Throwable th2) {
                                            th = th2;
                                        }
                                    }
                                    throw th;
                                }
                            }
                            if (type3 == 2) {
                                try {
                                    if (TAG_STATUS_ICONS.equals(tag2)) {
                                        if (!forRestore || userId == 0) {
                                            this.mHideSilentStatusBarIcons = parser.getAttributeBoolean((String) null, ATT_HIDE_SILENT, false);
                                            tag = tag2;
                                            type2 = type3;
                                            arrayMap = arrayMap2;
                                            i = i2;
                                        } else {
                                            tag = tag2;
                                            type2 = type3;
                                            arrayMap = arrayMap2;
                                            i = i2;
                                        }
                                    } else if ("package".equals(tag2)) {
                                        String name = parser.getAttributeValue((String) null, "name");
                                        if (TextUtils.isEmpty(name)) {
                                            tag = tag2;
                                            type2 = type3;
                                            arrayMap = arrayMap2;
                                            i = i2;
                                        } else {
                                            tag = tag2;
                                            type2 = type3;
                                            arrayMap = arrayMap2;
                                            i = i2;
                                            try {
                                                restorePackage(parser, forRestore, userId, name, upgradeForBubbles, migrateToPermission, pkgPerms);
                                            } catch (Throwable th3) {
                                                th = th3;
                                                while (true) {
                                                    break;
                                                    break;
                                                }
                                                throw th;
                                            }
                                        }
                                    } else {
                                        tag = tag2;
                                        type2 = type3;
                                        arrayMap = arrayMap2;
                                        i = i2;
                                    }
                                } catch (Throwable th4) {
                                    th = th4;
                                    arrayMap = arrayMap2;
                                }
                            } else {
                                tag = tag2;
                                type2 = type3;
                                arrayMap = arrayMap2;
                                i = i2;
                            }
                            i2 = i;
                            arrayMap2 = arrayMap;
                        } catch (Throwable th5) {
                            th = th5;
                            arrayMap = arrayMap2;
                        }
                    } catch (Throwable th6) {
                        th = th6;
                        arrayMap = arrayMap2;
                    }
                }
                try {
                    if (migrateToPermission) {
                        Iterator<PermissionHelper.PackagePermission> it = pkgPerms.iterator();
                        while (it.hasNext()) {
                            PermissionHelper.PackagePermission p = it.next();
                            try {
                                this.mPermissionHelper.setNotificationPermission(p);
                            } catch (Exception e) {
                                Slog.e(TAG, "could not migrate setting for " + p.packageName, e);
                            }
                        }
                    }
                } catch (Throwable th7) {
                    th = th7;
                    while (true) {
                        break;
                        break;
                    }
                    throw th;
                }
            }
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [407=6] */
    /* JADX WARN: Can't wrap try/catch for region: R(7:(17:137|138|139|140|141|142|8|(1:136)(3:11|(1:13)(1:135)|14)|(1:16)(1:134)|17|18|19|20|(2:21|(2:23|(4:(2:60|(2:124|125)(8:62|63|64|(3:66|67|(2:69|(7:71|72|73|74|75|77|78)(2:81|82))(3:85|86|(1:88)))(2:119|120)|89|(2:91|(1:(5:94|95|96|97|78)(2:98|99))(4:100|101|102|103))(2:114|115)|104|(3:106|(1:112)(1:110)|111)(1:113)))(2:126|127)|83|84|78)(2:27|28))(2:128|129))|29|30|(2:32|(7:34|(1:36)(1:48)|37|38|39|40|42)(2:49|50))(2:51|52))(1:6)|19|20|(3:21|(0)(0)|78)|29|30|(0)(0)) */
    /* JADX WARN: Code restructure failed: missing block: B:95:0x0245, code lost:
        r0 = move-exception;
     */
    /* JADX WARN: Code restructure failed: missing block: B:97:0x0248, code lost:
        android.util.Slog.e(r8, "deleteDefaultChannelIfNeededLocked - Exception: " + r0);
     */
    /* JADX WARN: Removed duplicated region for block: B:112:0x028f  */
    /* JADX WARN: Removed duplicated region for block: B:141:0x0238 A[SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:28:0x0067  */
    /* JADX WARN: Removed duplicated region for block: B:29:0x006a A[Catch: Exception -> 0x029e, TryCatch #1 {Exception -> 0x029e, blocks: (B:3:0x0017, B:8:0x0024, B:21:0x0048, B:30:0x0071, B:29:0x006a), top: B:123:0x0017 }] */
    /* JADX WARN: Removed duplicated region for block: B:35:0x0102  */
    /* JADX WARN: Removed duplicated region for block: B:99:0x0260 A[Catch: Exception -> 0x0242, TryCatch #9 {Exception -> 0x0242, blocks: (B:91:0x023e, B:97:0x0248, B:99:0x0260, B:101:0x026d, B:105:0x027d, B:70:0x01cd, B:72:0x01d7, B:74:0x01df, B:76:0x0201, B:78:0x0207, B:80:0x0211), top: B:135:0x01cd, inners: #3 }] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private void restorePackage(TypedXmlPullParser parser, boolean forRestore, int userId, String name, boolean upgradeForBubbles, boolean migrateToPermission, ArrayList<PermissionHelper.PackagePermission> pkgPerms) {
        String str;
        int uid;
        boolean hasSAWPermission;
        String str2;
        int attributeInt;
        String str3;
        boolean z;
        int type;
        PreferencesHelper preferencesHelper;
        String str4;
        String str5;
        try {
            int uid2 = parser.getAttributeInt((String) null, "uid", -10000);
            try {
                if (forRestore) {
                    try {
                        try {
                            int uid3 = this.mPm.getPackageUidAsUser(name, userId);
                            uid = uid3;
                        } catch (PackageManager.NameNotFoundException e) {
                        }
                    } catch (PackageManager.NameNotFoundException e2) {
                    } catch (Exception e3) {
                        e = e3;
                        str = TAG;
                        Slog.w(str, "Failed to restore pkg", e);
                        return;
                    }
                    boolean skipGroupWarningLogged = false;
                    if (upgradeForBubbles || uid == -10000) {
                        hasSAWPermission = false;
                    } else {
                        boolean hasSAWPermission2 = this.mAppOps.noteOpNoThrow(24, uid, name, (String) null, "check-notif-bubble") == 0;
                        hasSAWPermission = hasSAWPermission2;
                    }
                    if (hasSAWPermission) {
                        str2 = null;
                        attributeInt = parser.getAttributeInt((String) null, ATT_ALLOW_BUBBLE, 0);
                    } else {
                        attributeInt = 1;
                        str2 = null;
                    }
                    int bubblePref = attributeInt;
                    int appImportance = parser.getAttributeInt(str2, ATT_IMPORTANCE, -1000);
                    int attributeInt2 = parser.getAttributeInt(str2, ATT_PRIORITY, 0);
                    int attributeInt3 = parser.getAttributeInt(str2, ATT_VISIBILITY, -1000);
                    boolean attributeBoolean = parser.getAttributeBoolean(str2, ATT_SHOW_BADGE, true);
                    int uid4 = uid;
                    String str6 = str2;
                    boolean z2 = false;
                    str3 = TAG;
                    String str7 = "uid";
                    PackagePreferences r = getOrCreatePackagePreferencesLocked(name, userId, uid4, appImportance, attributeInt2, attributeInt3, attributeBoolean, bubblePref);
                    r.priority = parser.getAttributeInt(str6, ATT_PRIORITY, 0);
                    r.visibility = parser.getAttributeInt(str6, ATT_VISIBILITY, -1000);
                    z = true;
                    r.showBadge = parser.getAttributeBoolean(str6, ATT_SHOW_BADGE, true);
                    r.lockedAppFields = parser.getAttributeInt(str6, ATT_APP_USER_LOCKED_FIELDS, 0);
                    r.hasSentInvalidMessage = parser.getAttributeBoolean(str6, ATT_SENT_INVALID_MESSAGE, false);
                    r.hasSentValidMessage = parser.getAttributeBoolean(str6, ATT_SENT_VALID_MESSAGE, false);
                    r.userDemotedMsgApp = parser.getAttributeBoolean(str6, ATT_USER_DEMOTED_INVALID_MSG_APP, false);
                    r.playSound = XmlUtils.readBooleanAttribute(parser, ATT_PLAY_SOUND, true);
                    r.playVibration = XmlUtils.readBooleanAttribute(parser, ATT_PLAY_VIBRATION, true);
                    int innerDepth = parser.getDepth();
                    boolean skipWarningLogged = false;
                    while (true) {
                        type = parser.next();
                        if (type != z) {
                            preferencesHelper = this;
                            str = str3;
                            break;
                        } else if (type == 3 && parser.getDepth() <= innerDepth) {
                            preferencesHelper = this;
                            str = str3;
                            break;
                        } else {
                            if (type == 3) {
                                str = str3;
                                str4 = str7;
                            } else if (type == 4) {
                                str = str3;
                                str4 = str7;
                            } else {
                                try {
                                    String tagName = parser.getName();
                                    if (TAG_GROUP.equals(tagName)) {
                                        try {
                                            if (r.groups.size() < NOTIFICATION_CHANNEL_GROUP_COUNT_LIMIT) {
                                                str = str3;
                                                String id = parser.getAttributeValue(str6, ATT_ID);
                                                CharSequence groupName = parser.getAttributeValue(str6, "name");
                                                if (!TextUtils.isEmpty(id)) {
                                                    NotificationChannelGroup group = new NotificationChannelGroup(id, groupName);
                                                    group.populateFromXml(parser);
                                                    r.groups.put(id, group);
                                                }
                                            } else if (skipGroupWarningLogged) {
                                                str = str3;
                                                str4 = str7;
                                            } else {
                                                str = str3;
                                                try {
                                                    Slog.w(str, "Skipping further groups for " + r.pkg);
                                                    skipGroupWarningLogged = true;
                                                    str3 = str;
                                                } catch (Exception e4) {
                                                    e = e4;
                                                    Slog.w(str, "Failed to restore pkg", e);
                                                    return;
                                                }
                                            }
                                        } catch (Exception e5) {
                                            e = e5;
                                            str = str3;
                                        }
                                    } else {
                                        str = str3;
                                    }
                                    if (!TAG_CHANNEL.equals(tagName)) {
                                        str5 = str6;
                                    } else if (r.channels.size() < 5000) {
                                        str5 = str6;
                                        try {
                                            restoreChannel(parser, forRestore, r);
                                        } catch (Exception e6) {
                                            e = e6;
                                            Slog.w(str, "Failed to restore pkg", e);
                                            return;
                                        }
                                    } else if (skipWarningLogged) {
                                        str4 = str7;
                                    } else {
                                        Slog.w(str, "Skipping further channels for " + r.pkg);
                                        skipWarningLogged = true;
                                        str3 = str;
                                    }
                                    if (TAG_DELEGATE.equals(tagName)) {
                                        str4 = str7;
                                        int delegateId = parser.getAttributeInt(str5, str4, -10000);
                                        String delegateName = XmlUtils.readStringAttribute(parser, "name");
                                        boolean delegateEnabled = parser.getAttributeBoolean(str5, "enabled", z);
                                        boolean userAllowed = parser.getAttributeBoolean(str5, ATT_USER_ALLOWED, z);
                                        Delegate d = (delegateId == -10000 || TextUtils.isEmpty(delegateName)) ? null : new Delegate(delegateName, delegateId, delegateEnabled, userAllowed);
                                        r.delegate = d;
                                    } else {
                                        str4 = str7;
                                    }
                                } catch (Exception e7) {
                                    e = e7;
                                    str = str3;
                                }
                            }
                            str3 = str;
                            str7 = str4;
                            z = true;
                            z2 = false;
                            str6 = null;
                        }
                    }
                    preferencesHelper.deleteDefaultChannelIfNeededLocked(r);
                    if (migrateToPermission) {
                        return;
                    }
                    r.importance = appImportance;
                    boolean z3 = true;
                    r.migrateToPm = true;
                    if (r.uid != -10000) {
                        String str8 = r.pkg;
                        int userId2 = UserHandle.getUserId(r.uid);
                        if (r.importance == 0) {
                            z3 = false;
                        }
                        PermissionHelper.PackagePermission pkgPerm = new PermissionHelper.PackagePermission(str8, userId2, z3, preferencesHelper.hasUserConfiguredSettings(r));
                        try {
                            pkgPerms.add(pkgPerm);
                            return;
                        } catch (Exception e8) {
                            e = e8;
                            Slog.w(str, "Failed to restore pkg", e);
                            return;
                        }
                    }
                    return;
                }
                PackagePreferences r2 = getOrCreatePackagePreferencesLocked(name, userId, uid4, appImportance, attributeInt2, attributeInt3, attributeBoolean, bubblePref);
                r2.priority = parser.getAttributeInt(str6, ATT_PRIORITY, 0);
                r2.visibility = parser.getAttributeInt(str6, ATT_VISIBILITY, -1000);
                z = true;
                r2.showBadge = parser.getAttributeBoolean(str6, ATT_SHOW_BADGE, true);
                r2.lockedAppFields = parser.getAttributeInt(str6, ATT_APP_USER_LOCKED_FIELDS, 0);
                r2.hasSentInvalidMessage = parser.getAttributeBoolean(str6, ATT_SENT_INVALID_MESSAGE, false);
                r2.hasSentValidMessage = parser.getAttributeBoolean(str6, ATT_SENT_VALID_MESSAGE, false);
                r2.userDemotedMsgApp = parser.getAttributeBoolean(str6, ATT_USER_DEMOTED_INVALID_MSG_APP, false);
                r2.playSound = XmlUtils.readBooleanAttribute(parser, ATT_PLAY_SOUND, true);
                r2.playVibration = XmlUtils.readBooleanAttribute(parser, ATT_PLAY_VIBRATION, true);
                int innerDepth2 = parser.getDepth();
                boolean skipWarningLogged2 = false;
                while (true) {
                    type = parser.next();
                    if (type != z) {
                    }
                }
                preferencesHelper.deleteDefaultChannelIfNeededLocked(r2);
                if (migrateToPermission) {
                }
            } catch (Exception e9) {
                e = e9;
                str = str3;
            }
            uid = uid2;
            boolean skipGroupWarningLogged2 = false;
            if (upgradeForBubbles) {
            }
            hasSAWPermission = false;
            if (hasSAWPermission) {
            }
            int bubblePref2 = attributeInt;
            int appImportance2 = parser.getAttributeInt(str2, ATT_IMPORTANCE, -1000);
            int attributeInt22 = parser.getAttributeInt(str2, ATT_PRIORITY, 0);
            int attributeInt32 = parser.getAttributeInt(str2, ATT_VISIBILITY, -1000);
            boolean attributeBoolean2 = parser.getAttributeBoolean(str2, ATT_SHOW_BADGE, true);
            int uid42 = uid;
            String str62 = str2;
            boolean z22 = false;
            str3 = TAG;
            String str72 = "uid";
        } catch (Exception e10) {
            e = e10;
        }
    }

    private void restoreChannel(TypedXmlPullParser parser, boolean forRestore, PackagePreferences r) {
        boolean z;
        try {
            String id = parser.getAttributeValue((String) null, ATT_ID);
            String channelName = parser.getAttributeValue((String) null, "name");
            int channelImportance = parser.getAttributeInt((String) null, ATT_IMPORTANCE, -1000);
            if (!TextUtils.isEmpty(id) && !TextUtils.isEmpty(channelName)) {
                NotificationChannel channel = new NotificationChannel(id, channelName, channelImportance);
                if (forRestore) {
                    channel.populateFromXmlForRestore(parser, this.mContext);
                } else {
                    channel.populateFromXml(parser);
                }
                if (!r.defaultAppLockedImportance && !r.fixedImportance) {
                    z = false;
                    channel.setImportanceLockedByCriticalDeviceFunction(z);
                    if (!isShortcutOk(channel) && isDeletionOk(channel)) {
                        r.channels.put(id, channel);
                        return;
                    }
                }
                z = true;
                channel.setImportanceLockedByCriticalDeviceFunction(z);
                if (!isShortcutOk(channel)) {
                }
            }
        } catch (Exception e) {
            Slog.w(TAG, "could not restore channel for " + r.pkg, e);
        }
    }

    private boolean hasUserConfiguredSettings(PackagePreferences p) {
        boolean hasChangedChannel = false;
        Iterator<NotificationChannel> it = p.channels.values().iterator();
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            NotificationChannel channel = it.next();
            if (channel.getUserLockedFields() != 0) {
                hasChangedChannel = true;
                break;
            }
        }
        return hasChangedChannel || p.importance == 0;
    }

    private boolean isShortcutOk(NotificationChannel channel) {
        boolean isInvalidShortcutChannel = channel.getConversationId() != null && channel.getConversationId().contains(":placeholder_id");
        boolean z = this.mAllowInvalidShortcuts;
        if (z) {
            return true;
        }
        return (z || isInvalidShortcutChannel) ? false : true;
    }

    private boolean isDeletionOk(NotificationChannel nc) {
        if (nc.isDeleted()) {
            long boundary = System.currentTimeMillis() - UnixCalendar.MONTH_IN_MILLIS;
            return nc.getDeletedTimeMs() > boundary;
        }
        return true;
    }

    private PackagePreferences getPackagePreferencesLocked(String pkg, int uid) {
        String key = packagePreferencesKey(pkg, uid);
        return this.mPackagePreferences.get(key);
    }

    private PackagePreferences getOrCreatePackagePreferencesLocked(String pkg, int uid) {
        return getOrCreatePackagePreferencesLocked(pkg, UserHandle.getUserId(uid), uid, -1000, 0, -1000, true, 0);
    }

    private PackagePreferences getOrCreatePackagePreferencesLocked(String pkg, int userId, int uid, int importance, int priority, int visibility, boolean showBadge, int bubblePreference) {
        PackagePreferences r;
        String key = packagePreferencesKey(pkg, uid);
        if (uid == -10000) {
            r = this.mRestoredWithoutUids.get(unrestoredPackageKey(pkg, userId));
        } else {
            r = this.mPackagePreferences.get(key);
        }
        if (r == null) {
            r = new PackagePreferences();
            r.pkg = pkg;
            r.uid = uid;
            r.importance = importance;
            r.priority = priority;
            r.visibility = visibility;
            r.showBadge = showBadge;
            r.bubblePreference = bubblePreference;
            try {
                createDefaultChannelIfNeededLocked(r);
            } catch (PackageManager.NameNotFoundException e) {
                Slog.e(TAG, "createDefaultChannelIfNeededLocked - Exception: " + e);
            }
            if (r.uid == -10000) {
                this.mRestoredWithoutUids.put(unrestoredPackageKey(pkg, userId), r);
            } else {
                this.mPackagePreferences.put(key, r);
            }
        }
        return r;
    }

    private boolean shouldHaveDefaultChannel(PackagePreferences r) throws PackageManager.NameNotFoundException {
        int userId = UserHandle.getUserId(r.uid);
        ApplicationInfo applicationInfo = this.mPm.getApplicationInfoAsUser(r.pkg, 0, userId);
        if (applicationInfo.targetSdkVersion >= 26) {
            return false;
        }
        return true;
    }

    private boolean deleteDefaultChannelIfNeededLocked(PackagePreferences r) throws PackageManager.NameNotFoundException {
        if (r.channels.containsKey("miscellaneous") && !shouldHaveDefaultChannel(r)) {
            r.channels.remove("miscellaneous");
            return true;
        }
        return false;
    }

    private boolean createDefaultChannelIfNeededLocked(PackagePreferences r) throws PackageManager.NameNotFoundException {
        if (r.uid == -10000) {
            return false;
        }
        if (r.channels.containsKey("miscellaneous")) {
            r.channels.get("miscellaneous").setName(this.mContext.getString(17040144));
            return false;
        } else if (shouldHaveDefaultChannel(r)) {
            NotificationChannel channel = new NotificationChannel("miscellaneous", this.mContext.getString(17040144), r.importance);
            channel.setBypassDnd(r.priority == 2);
            channel.setLockscreenVisibility(r.visibility);
            if (r.importance != -1000) {
                channel.lockFields(4);
            }
            if (r.priority != 0) {
                channel.lockFields(1);
            }
            if (r.visibility != -1000) {
                channel.lockFields(2);
            }
            r.channels.put(channel.getId(), channel);
            return true;
        } else {
            return false;
        }
    }

    public void writeXml(TypedXmlSerializer out, boolean forBackup, int userId) throws IOException {
        ArrayMap<Pair<Integer, String>, Pair<Boolean, Boolean>> notifPermissions;
        out.startTag((String) null, TAG_RANKING);
        out.attributeInt((String) null, ATT_VERSION, this.XML_VERSION);
        if (this.mHideSilentStatusBarIcons && (!forBackup || userId == 0)) {
            out.startTag((String) null, TAG_STATUS_ICONS);
            out.attributeBoolean((String) null, ATT_HIDE_SILENT, this.mHideSilentStatusBarIcons);
            out.endTag((String) null, TAG_STATUS_ICONS);
        }
        ArrayMap<Pair<Integer, String>, Pair<Boolean, Boolean>> notifPermissions2 = new ArrayMap<>();
        if (!forBackup) {
            notifPermissions = notifPermissions2;
        } else {
            ArrayMap<Pair<Integer, String>, Pair<Boolean, Boolean>> notifPermissions3 = this.mPermissionHelper.getNotificationPermissionValues(userId);
            notifPermissions = notifPermissions3;
        }
        synchronized (this.mPackagePreferences) {
            int N = this.mPackagePreferences.size();
            int i = 0;
            while (true) {
                int i2 = 3;
                if (i >= N) {
                    break;
                }
                PackagePreferences r = this.mPackagePreferences.valueAt(i);
                if (!forBackup || UserHandle.getUserId(r.uid) == userId) {
                    out.startTag((String) null, "package");
                    out.attribute((String) null, "name", r.pkg);
                    if (!notifPermissions.isEmpty()) {
                        Pair<Integer, String> app = new Pair<>(Integer.valueOf(r.uid), r.pkg);
                        Pair<Boolean, Boolean> permission = notifPermissions.get(app);
                        if (permission == null || !((Boolean) permission.first).booleanValue()) {
                            i2 = 0;
                        }
                        out.attributeInt((String) null, ATT_IMPORTANCE, i2);
                        notifPermissions.remove(app);
                    } else if (r.importance != -1000) {
                        out.attributeInt((String) null, ATT_IMPORTANCE, r.importance);
                    }
                    if (r.priority != 0) {
                        out.attributeInt((String) null, ATT_PRIORITY, r.priority);
                    }
                    if (r.visibility != -1000) {
                        out.attributeInt((String) null, ATT_VISIBILITY, r.visibility);
                    }
                    if (r.bubblePreference != 0) {
                        out.attributeInt((String) null, ATT_ALLOW_BUBBLE, r.bubblePreference);
                    }
                    out.attributeBoolean((String) null, ATT_SHOW_BADGE, r.showBadge);
                    out.attributeInt((String) null, ATT_APP_USER_LOCKED_FIELDS, r.lockedAppFields);
                    out.attributeBoolean((String) null, ATT_SENT_INVALID_MESSAGE, r.hasSentInvalidMessage);
                    out.attributeBoolean((String) null, ATT_SENT_VALID_MESSAGE, r.hasSentValidMessage);
                    out.attributeBoolean((String) null, ATT_USER_DEMOTED_INVALID_MSG_APP, r.userDemotedMsgApp);
                    out.attribute((String) null, ATT_PLAY_SOUND, Boolean.toString(r.playSound));
                    out.attribute((String) null, ATT_PLAY_VIBRATION, Boolean.toString(r.playVibration));
                    if (!forBackup) {
                        out.attributeInt((String) null, "uid", r.uid);
                    }
                    if (r.delegate != null) {
                        out.startTag((String) null, TAG_DELEGATE);
                        out.attribute((String) null, "name", r.delegate.mPkg);
                        out.attributeInt((String) null, "uid", r.delegate.mUid);
                        if (!r.delegate.mEnabled) {
                            out.attributeBoolean((String) null, "enabled", r.delegate.mEnabled);
                        }
                        if (!r.delegate.mUserAllowed) {
                            out.attributeBoolean((String) null, ATT_USER_ALLOWED, r.delegate.mUserAllowed);
                        }
                        out.endTag((String) null, TAG_DELEGATE);
                    }
                    for (NotificationChannelGroup group : r.groups.values()) {
                        group.writeXml(out);
                    }
                    for (NotificationChannel channel : r.channels.values()) {
                        if (forBackup) {
                            if (!channel.isDeleted()) {
                                channel.writeXmlForBackup(out, this.mContext);
                            }
                        } else {
                            channel.writeXml(out);
                        }
                    }
                    out.endTag((String) null, "package");
                }
                i++;
            }
        }
        if (!notifPermissions.isEmpty()) {
            for (Pair<Integer, String> app2 : notifPermissions.keySet()) {
                out.startTag((String) null, "package");
                out.attribute((String) null, "name", (String) app2.second);
                out.attributeInt((String) null, ATT_IMPORTANCE, ((Boolean) notifPermissions.get(app2).first).booleanValue() ? 3 : 0);
                out.endTag((String) null, "package");
            }
        }
        out.endTag((String) null, TAG_RANKING);
    }

    public void setBubblesAllowed(String pkg, int uid, int bubblePreference) {
        boolean changed;
        synchronized (this.mPackagePreferences) {
            PackagePreferences p = getOrCreatePackagePreferencesLocked(pkg, uid);
            changed = p.bubblePreference != bubblePreference;
            p.bubblePreference = bubblePreference;
            p.lockedAppFields |= 2;
        }
        if (changed) {
            updateConfig();
        }
    }

    @Override // com.android.server.notification.RankingConfig
    public int getBubblePreference(String pkg, int uid) {
        int i;
        synchronized (this.mPackagePreferences) {
            i = getOrCreatePackagePreferencesLocked(pkg, uid).bubblePreference;
        }
        return i;
    }

    public int getAppLockedFields(String pkg, int uid) {
        int i;
        synchronized (this.mPackagePreferences) {
            i = getOrCreatePackagePreferencesLocked(pkg, uid).lockedAppFields;
        }
        return i;
    }

    @Override // com.android.server.notification.RankingConfig
    public boolean canShowBadge(String packageName, int uid) {
        boolean z;
        synchronized (this.mPackagePreferences) {
            z = getOrCreatePackagePreferencesLocked(packageName, uid).showBadge;
        }
        return z;
    }

    @Override // com.android.server.notification.RankingConfig
    public void setShowBadge(String packageName, int uid, boolean showBadge) {
        synchronized (this.mPackagePreferences) {
            getOrCreatePackagePreferencesLocked(packageName, uid).showBadge = showBadge;
        }
        updateConfig();
    }

    @Override // com.android.server.notification.RankingConfig
    public boolean canPlaySound(String packageName, int uid) {
        boolean z;
        synchronized (this.mPackagePreferences) {
            z = getOrCreatePackagePreferencesLocked(packageName, uid).playSound;
        }
        return z;
    }

    @Override // com.android.server.notification.RankingConfig
    public void setPlaySound(String packageName, int uid, boolean playSound) {
        synchronized (this.mPackagePreferences) {
            getOrCreatePackagePreferencesLocked(packageName, uid).playSound = playSound;
        }
        updateConfig();
    }

    @Override // com.android.server.notification.RankingConfig
    public boolean canPlayVibration(String packageName, int uid) {
        boolean z;
        synchronized (this.mPackagePreferences) {
            z = getOrCreatePackagePreferencesLocked(packageName, uid).playVibration;
        }
        return z;
    }

    @Override // com.android.server.notification.RankingConfig
    public void setPlayVibration(String packageName, int uid, boolean playVibration) {
        synchronized (this.mPackagePreferences) {
            getOrCreatePackagePreferencesLocked(packageName, uid).playVibration = playVibration;
        }
        updateConfig();
    }

    public boolean isInInvalidMsgState(String packageName, int uid) {
        boolean z;
        synchronized (this.mPackagePreferences) {
            PackagePreferences r = getOrCreatePackagePreferencesLocked(packageName, uid);
            z = r.hasSentInvalidMessage && !r.hasSentValidMessage;
        }
        return z;
    }

    public boolean hasUserDemotedInvalidMsgApp(String packageName, int uid) {
        boolean z;
        synchronized (this.mPackagePreferences) {
            PackagePreferences r = getOrCreatePackagePreferencesLocked(packageName, uid);
            z = isInInvalidMsgState(packageName, uid) ? r.userDemotedMsgApp : false;
        }
        return z;
    }

    public void setInvalidMsgAppDemoted(String packageName, int uid, boolean isDemoted) {
        synchronized (this.mPackagePreferences) {
            PackagePreferences r = getOrCreatePackagePreferencesLocked(packageName, uid);
            r.userDemotedMsgApp = isDemoted;
        }
    }

    public boolean setInvalidMessageSent(String packageName, int uid) {
        boolean valueChanged;
        synchronized (this.mPackagePreferences) {
            PackagePreferences r = getOrCreatePackagePreferencesLocked(packageName, uid);
            valueChanged = !r.hasSentInvalidMessage;
            r.hasSentInvalidMessage = true;
        }
        return valueChanged;
    }

    public boolean setValidMessageSent(String packageName, int uid) {
        boolean valueChanged;
        synchronized (this.mPackagePreferences) {
            PackagePreferences r = getOrCreatePackagePreferencesLocked(packageName, uid);
            valueChanged = !r.hasSentValidMessage;
            r.hasSentValidMessage = true;
        }
        return valueChanged;
    }

    boolean hasSentInvalidMsg(String packageName, int uid) {
        boolean z;
        synchronized (this.mPackagePreferences) {
            PackagePreferences r = getOrCreatePackagePreferencesLocked(packageName, uid);
            z = r.hasSentInvalidMessage;
        }
        return z;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasSentValidMsg(String packageName, int uid) {
        boolean z;
        synchronized (this.mPackagePreferences) {
            PackagePreferences r = getOrCreatePackagePreferencesLocked(packageName, uid);
            z = r.hasSentValidMessage;
        }
        return z;
    }

    boolean didUserEverDemoteInvalidMsgApp(String packageName, int uid) {
        boolean z;
        synchronized (this.mPackagePreferences) {
            PackagePreferences r = getOrCreatePackagePreferencesLocked(packageName, uid);
            z = r.userDemotedMsgApp;
        }
        return z;
    }

    public boolean setValidBubbleSent(String packageName, int uid) {
        boolean valueChanged;
        synchronized (this.mPackagePreferences) {
            PackagePreferences r = getOrCreatePackagePreferencesLocked(packageName, uid);
            valueChanged = !r.hasSentValidBubble;
            r.hasSentValidBubble = true;
        }
        return valueChanged;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasSentValidBubble(String packageName, int uid) {
        boolean z;
        synchronized (this.mPackagePreferences) {
            PackagePreferences r = getOrCreatePackagePreferencesLocked(packageName, uid);
            z = r.hasSentValidBubble;
        }
        return z;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isImportanceLocked(String pkg, int uid) {
        boolean z;
        synchronized (this.mPackagePreferences) {
            PackagePreferences r = getOrCreatePackagePreferencesLocked(pkg, uid);
            z = r.fixedImportance || r.defaultAppLockedImportance;
        }
        return z;
    }

    @Override // com.android.server.notification.RankingConfig
    public boolean isGroupBlocked(String packageName, int uid, String groupId) {
        if (groupId == null) {
            return false;
        }
        synchronized (this.mPackagePreferences) {
            PackagePreferences r = getOrCreatePackagePreferencesLocked(packageName, uid);
            NotificationChannelGroup group = r.groups.get(groupId);
            if (group == null) {
                return false;
            }
            return group.isBlocked();
        }
    }

    int getPackagePriority(String pkg, int uid) {
        int i;
        synchronized (this.mPackagePreferences) {
            i = getOrCreatePackagePreferencesLocked(pkg, uid).priority;
        }
        return i;
    }

    int getPackageVisibility(String pkg, int uid) {
        int i;
        synchronized (this.mPackagePreferences) {
            i = getOrCreatePackagePreferencesLocked(pkg, uid).visibility;
        }
        return i;
    }

    /* JADX WARN: Removed duplicated region for block: B:25:0x0089 A[Catch: all -> 0x00c4, TRY_LEAVE, TryCatch #2 {all -> 0x00c4, blocks: (B:23:0x0083, B:25:0x0089), top: B:60:0x0083 }] */
    /* JADX WARN: Removed duplicated region for block: B:37:0x00b0  */
    /* JADX WARN: Removed duplicated region for block: B:41:0x00be  */
    /* JADX WARN: Removed duplicated region for block: B:63:? A[RETURN, SYNTHETIC] */
    @Override // com.android.server.notification.RankingConfig
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public void createNotificationChannelGroup(String pkg, int uid, NotificationChannelGroup group, boolean fromTargetApp) {
        boolean needsDndChange;
        boolean needsDndChange2;
        Objects.requireNonNull(pkg);
        Objects.requireNonNull(group);
        Objects.requireNonNull(group.getId());
        Objects.requireNonNull(Boolean.valueOf(!TextUtils.isEmpty(group.getName())));
        synchronized (this.mPackagePreferences) {
            try {
                try {
                    PackagePreferences r = getOrCreatePackagePreferencesLocked(pkg, uid);
                    if (r == null) {
                        throw new IllegalArgumentException("Invalid package");
                    }
                    if (fromTargetApp) {
                        group.setBlocked(false);
                        if (r.groups.size() >= NOTIFICATION_CHANNEL_GROUP_COUNT_LIMIT) {
                            throw new IllegalStateException("Limit exceed; cannot create more groups");
                        }
                    }
                    NotificationChannelGroup oldGroup = r.groups.get(group.getId());
                    try {
                        if (oldGroup != null) {
                            group.setChannels(oldGroup.getChannels());
                            if (fromTargetApp) {
                                group.setBlocked(oldGroup.isBlocked());
                                group.unlockFields(group.getUserLockedFields());
                                group.lockFields(oldGroup.getUserLockedFields());
                            } else if (group.isBlocked() != oldGroup.isBlocked()) {
                                group.lockFields(1);
                                needsDndChange = true;
                                needsDndChange2 = group.equals(oldGroup);
                                if (needsDndChange2) {
                                    try {
                                        MetricsLogger.action(getChannelGroupLog(group.getId(), pkg));
                                        this.mNotificationChannelLogger.logNotificationChannelGroup(group, uid, pkg, oldGroup == null, oldGroup != null && oldGroup.isBlocked());
                                    } catch (Throwable th) {
                                        th = th;
                                        throw th;
                                    }
                                }
                                r.groups.put(group.getId(), group);
                                if (!needsDndChange) {
                                    updateChannelsBypassingDnd();
                                    return;
                                }
                                return;
                            }
                        }
                        needsDndChange2 = group.equals(oldGroup);
                        if (needsDndChange2) {
                        }
                        r.groups.put(group.getId(), group);
                        if (!needsDndChange) {
                        }
                    } catch (Throwable th2) {
                        th = th2;
                    }
                    needsDndChange = false;
                } catch (Throwable th3) {
                    th = th3;
                }
            } catch (Throwable th4) {
                th = th4;
            }
        }
    }

    /* JADX WARN: Removed duplicated region for block: B:100:0x01c7 A[Catch: all -> 0x0236, TryCatch #2 {all -> 0x0236, blocks: (B:4:0x0028, B:6:0x002e, B:8:0x0034, B:11:0x0041, B:12:0x0048, B:13:0x0049, B:15:0x0056, B:18:0x006a, B:20:0x0070, B:111:0x020f, B:124:0x0237, B:72:0x0176, B:74:0x0180, B:76:0x0187, B:80:0x0192, B:84:0x019a, B:86:0x019f, B:88:0x01a6, B:90:0x01ac, B:91:0x01af, B:93:0x01b6, B:98:0x01be, B:100:0x01c7, B:101:0x01ca, B:103:0x01ce, B:104:0x01d1, B:106:0x01de, B:107:0x01ed, B:110:0x01ff, B:115:0x0216, B:116:0x021d, B:117:0x021e, B:118:0x0225, B:119:0x0226, B:120:0x022d, B:121:0x022e, B:122:0x0235), top: B:130:0x0028 }] */
    /* JADX WARN: Removed duplicated region for block: B:103:0x01ce A[Catch: all -> 0x0236, TryCatch #2 {all -> 0x0236, blocks: (B:4:0x0028, B:6:0x002e, B:8:0x0034, B:11:0x0041, B:12:0x0048, B:13:0x0049, B:15:0x0056, B:18:0x006a, B:20:0x0070, B:111:0x020f, B:124:0x0237, B:72:0x0176, B:74:0x0180, B:76:0x0187, B:80:0x0192, B:84:0x019a, B:86:0x019f, B:88:0x01a6, B:90:0x01ac, B:91:0x01af, B:93:0x01b6, B:98:0x01be, B:100:0x01c7, B:101:0x01ca, B:103:0x01ce, B:104:0x01d1, B:106:0x01de, B:107:0x01ed, B:110:0x01ff, B:115:0x0216, B:116:0x021d, B:117:0x021e, B:118:0x0225, B:119:0x0226, B:120:0x022d, B:121:0x022e, B:122:0x0235), top: B:130:0x0028 }] */
    /* JADX WARN: Removed duplicated region for block: B:106:0x01de A[Catch: all -> 0x0236, TryCatch #2 {all -> 0x0236, blocks: (B:4:0x0028, B:6:0x002e, B:8:0x0034, B:11:0x0041, B:12:0x0048, B:13:0x0049, B:15:0x0056, B:18:0x006a, B:20:0x0070, B:111:0x020f, B:124:0x0237, B:72:0x0176, B:74:0x0180, B:76:0x0187, B:80:0x0192, B:84:0x019a, B:86:0x019f, B:88:0x01a6, B:90:0x01ac, B:91:0x01af, B:93:0x01b6, B:98:0x01be, B:100:0x01c7, B:101:0x01ca, B:103:0x01ce, B:104:0x01d1, B:106:0x01de, B:107:0x01ed, B:110:0x01ff, B:115:0x0216, B:116:0x021d, B:117:0x021e, B:118:0x0225, B:119:0x0226, B:120:0x022d, B:121:0x022e, B:122:0x0235), top: B:130:0x0028 }] */
    /* JADX WARN: Removed duplicated region for block: B:109:0x01fe  */
    @Override // com.android.server.notification.RankingConfig
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public boolean createNotificationChannel(String pkg, int uid, NotificationChannel channel, boolean fromTargetApp, boolean hasDndAccess) {
        boolean needsPolicyFileChange;
        boolean z;
        boolean needsDndChange;
        int i;
        boolean wasUndeleted;
        boolean needsDndChange2;
        boolean needsPolicyFileChange2;
        boolean bypassDnd;
        Objects.requireNonNull(pkg);
        Objects.requireNonNull(channel);
        Objects.requireNonNull(channel.getId());
        Preconditions.checkArgument(!TextUtils.isEmpty(channel.getName()));
        boolean needsPolicyFileChange3 = false;
        synchronized (this.mPackagePreferences) {
            try {
                PackagePreferences r = getOrCreatePackagePreferencesLocked(pkg, uid);
                if (r == null) {
                    throw new IllegalArgumentException("Invalid package");
                }
                if (channel.getGroup() != null && !r.groups.containsKey(channel.getGroup())) {
                    throw new IllegalArgumentException("NotificationChannelGroup doesn't exist");
                }
                if ("miscellaneous".equals(channel.getId())) {
                    throw new IllegalArgumentException("Reserved id");
                }
                NotificationChannel existing = r.channels.get(channel.getId());
                if (existing != null && fromTargetApp) {
                    if (!existing.isDeleted()) {
                        wasUndeleted = false;
                    } else {
                        existing.setDeleted(false);
                        existing.setDeletedTimeMs(-1L);
                        needsPolicyFileChange3 = true;
                        MetricsLogger.action(getChannelLog(channel, pkg).setType(1));
                        this.mNotificationChannelLogger.logNotificationChannelCreated(channel, uid, pkg);
                        wasUndeleted = true;
                    }
                    try {
                        if (!Objects.equals(channel.getName().toString(), existing.getName().toString())) {
                            existing.setName(channel.getName().toString());
                            needsPolicyFileChange3 = true;
                        }
                        if (!Objects.equals(channel.getDescription(), existing.getDescription())) {
                            existing.setDescription(channel.getDescription());
                            needsPolicyFileChange3 = true;
                        }
                        if (channel.isBlockable() != existing.isBlockable()) {
                            existing.setBlockable(channel.isBlockable());
                            needsPolicyFileChange3 = true;
                        }
                        if (channel.getGroup() != null && existing.getGroup() == null) {
                            existing.setGroup(channel.getGroup());
                            needsPolicyFileChange3 = true;
                        }
                        int previousExistingImportance = existing.getImportance();
                        int previousLoggingImportance = NotificationChannelLogger.getLoggingImportance(existing);
                        if (existing.getUserLockedFields() == 0 && channel.getImportance() < existing.getImportance()) {
                            existing.setImportance(channel.getImportance());
                            needsPolicyFileChange3 = true;
                        }
                        if (existing.getUserLockedFields() == 0 && hasDndAccess && ((bypassDnd = channel.canBypassDnd()) != existing.canBypassDnd() || wasUndeleted)) {
                            existing.setBypassDnd(bypassDnd);
                            needsPolicyFileChange3 = true;
                            if (bypassDnd == this.mAreChannelsBypassingDnd) {
                                if (previousExistingImportance == existing.getImportance()) {
                                    needsDndChange2 = false;
                                }
                            }
                            needsDndChange2 = true;
                        } else {
                            needsDndChange2 = false;
                        }
                        try {
                            if (existing.getOriginalImportance() != -1000) {
                                needsPolicyFileChange2 = needsPolicyFileChange3;
                            } else {
                                existing.setOriginalImportance(channel.getImportance());
                                needsPolicyFileChange2 = true;
                            }
                            try {
                                updateConfig();
                                if (needsPolicyFileChange2 && !wasUndeleted) {
                                    this.mNotificationChannelLogger.logNotificationChannelModified(existing, uid, pkg, previousLoggingImportance, false);
                                }
                                needsDndChange = needsDndChange2;
                                needsPolicyFileChange = needsPolicyFileChange2;
                            } catch (Throwable th) {
                                th = th;
                                throw th;
                            }
                        } catch (Throwable th2) {
                            th = th2;
                        }
                    } catch (Throwable th3) {
                        th = th3;
                    }
                } else if (r.channels.size() >= 5000) {
                    throw new IllegalStateException("Limit exceed; cannot create more channels");
                } else {
                    needsPolicyFileChange = true;
                    if (channel.getImportance() < 0 || channel.getImportance() > 5) {
                        throw new IllegalArgumentException("Invalid importance level");
                    }
                    if (fromTargetApp && !hasDndAccess) {
                        channel.setBypassDnd(r.priority == 2);
                    }
                    if (fromTargetApp) {
                        channel.setLockscreenVisibility(r.visibility);
                        if (existing != null) {
                            i = existing.getAllowBubbles();
                        } else {
                            i = -1;
                        }
                        channel.setAllowBubbles(i);
                    }
                    clearLockedFieldsLocked(channel);
                    if (!r.defaultAppLockedImportance && !r.fixedImportance) {
                        z = false;
                        channel.setImportanceLockedByCriticalDeviceFunction(z);
                        if (channel.getLockscreenVisibility() == 1) {
                            channel.setLockscreenVisibility(-1000);
                        }
                        if (!r.showBadge) {
                            channel.setShowBadge(false);
                        }
                        channel.setOriginalImportance(channel.getImportance());
                        if (channel.getParentChannelId() != null) {
                            Preconditions.checkArgument(r.channels.containsKey(channel.getParentChannelId()), "Tried to create a conversation channel without a preexisting parent");
                        }
                        r.channels.put(channel.getId(), channel);
                        needsDndChange = channel.canBypassDnd() != this.mAreChannelsBypassingDnd;
                        MetricsLogger.action(getChannelLog(channel, pkg).setType(1));
                        this.mNotificationChannelLogger.logNotificationChannelCreated(channel, uid, pkg);
                    }
                    z = true;
                    channel.setImportanceLockedByCriticalDeviceFunction(z);
                    if (channel.getLockscreenVisibility() == 1) {
                    }
                    if (!r.showBadge) {
                    }
                    channel.setOriginalImportance(channel.getImportance());
                    if (channel.getParentChannelId() != null) {
                    }
                    r.channels.put(channel.getId(), channel);
                    if (channel.canBypassDnd() != this.mAreChannelsBypassingDnd) {
                    }
                    MetricsLogger.action(getChannelLog(channel, pkg).setType(1));
                    this.mNotificationChannelLogger.logNotificationChannelCreated(channel, uid, pkg);
                }
                if (needsDndChange) {
                    updateChannelsBypassingDnd();
                }
                return needsPolicyFileChange;
            } catch (Throwable th4) {
                th = th4;
            }
        }
    }

    void clearLockedFieldsLocked(NotificationChannel channel) {
        channel.unlockFields(channel.getUserLockedFields());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void unlockNotificationChannelImportance(String pkg, int uid, String updatedChannelId) {
        Objects.requireNonNull(updatedChannelId);
        synchronized (this.mPackagePreferences) {
            PackagePreferences r = getOrCreatePackagePreferencesLocked(pkg, uid);
            if (r == null) {
                throw new IllegalArgumentException("Invalid package");
            }
            NotificationChannel channel = r.channels.get(updatedChannelId);
            if (channel == null || channel.isDeleted()) {
                throw new IllegalArgumentException("Channel does not exist");
            }
            channel.unlockFields(4);
        }
    }

    @Override // com.android.server.notification.RankingConfig
    public void updateNotificationChannel(String pkg, int uid, NotificationChannel updatedChannel, boolean fromUser) {
        Objects.requireNonNull(updatedChannel);
        Objects.requireNonNull(updatedChannel.getId());
        boolean needsDndChange = false;
        synchronized (this.mPackagePreferences) {
            PackagePreferences r = getOrCreatePackagePreferencesLocked(pkg, uid);
            if (r == null) {
                throw new IllegalArgumentException("Invalid package");
            }
            NotificationChannel channel = r.channels.get(updatedChannel.getId());
            if (channel == null || channel.isDeleted()) {
                throw new IllegalArgumentException("Channel does not exist");
            }
            int i = 1;
            if (updatedChannel.getLockscreenVisibility() == 1) {
                updatedChannel.setLockscreenVisibility(-1000);
            }
            if (fromUser) {
                updatedChannel.lockFields(channel.getUserLockedFields());
                lockFieldsForUpdateLocked(channel, updatedChannel);
            } else {
                updatedChannel.unlockFields(updatedChannel.getUserLockedFields());
            }
            if (channel.isImportanceLockedByCriticalDeviceFunction() && !channel.isBlockable() && channel.getImportance() != 0) {
                updatedChannel.setImportance(channel.getImportance());
            }
            r.channels.put(updatedChannel.getId(), updatedChannel);
            if (onlyHasDefaultChannel(pkg, uid)) {
                r.priority = updatedChannel.canBypassDnd() ? 2 : 0;
                r.visibility = updatedChannel.getLockscreenVisibility();
                r.showBadge = updatedChannel.canShowBadge();
            }
            if (!channel.equals(updatedChannel)) {
                LogMaker channelLog = getChannelLog(updatedChannel, pkg);
                if (!fromUser) {
                    i = 0;
                }
                MetricsLogger.action(channelLog.setSubtype(i));
                this.mNotificationChannelLogger.logNotificationChannelModified(updatedChannel, uid, pkg, NotificationChannelLogger.getLoggingImportance(channel), fromUser);
            }
            needsDndChange = (updatedChannel.canBypassDnd() == this.mAreChannelsBypassingDnd && channel.getImportance() == updatedChannel.getImportance()) ? true : true;
        }
        if (needsDndChange) {
            updateChannelsBypassingDnd();
        }
        updateConfig();
    }

    @Override // com.android.server.notification.RankingConfig
    public NotificationChannel getNotificationChannel(String pkg, int uid, String channelId, boolean includeDeleted) {
        Objects.requireNonNull(pkg);
        return getConversationNotificationChannel(pkg, uid, channelId, null, true, includeDeleted);
    }

    @Override // com.android.server.notification.RankingConfig
    public NotificationChannel getConversationNotificationChannel(String pkg, int uid, String channelId, String conversationId, boolean returnParentIfNoConversationChannel, boolean includeDeleted) {
        NotificationChannel nc;
        Preconditions.checkNotNull(pkg);
        synchronized (this.mPackagePreferences) {
            PackagePreferences r = getOrCreatePackagePreferencesLocked(pkg, uid);
            if (r == null) {
                return null;
            }
            if (channelId == null) {
                channelId = "miscellaneous";
            }
            NotificationChannel channel = null;
            if (conversationId != null) {
                channel = findConversationChannel(r, channelId, conversationId, includeDeleted);
            }
            return (channel != null || !returnParentIfNoConversationChannel || (nc = r.channels.get(channelId)) == null || (!includeDeleted && nc.isDeleted())) ? channel : nc;
        }
    }

    private NotificationChannel findConversationChannel(PackagePreferences p, String parentId, String conversationId, boolean includeDeleted) {
        for (NotificationChannel nc : p.channels.values()) {
            if (conversationId.equals(nc.getConversationId()) && parentId.equals(nc.getParentChannelId()) && (includeDeleted || !nc.isDeleted())) {
                return nc;
            }
        }
        return null;
    }

    public List<NotificationChannel> getNotificationChannelsByConversationId(String pkg, int uid, String conversationId) {
        Preconditions.checkNotNull(pkg);
        Preconditions.checkNotNull(conversationId);
        List<NotificationChannel> channels = new ArrayList<>();
        synchronized (this.mPackagePreferences) {
            PackagePreferences r = getOrCreatePackagePreferencesLocked(pkg, uid);
            if (r == null) {
                return channels;
            }
            for (NotificationChannel nc : r.channels.values()) {
                if (conversationId.equals(nc.getConversationId()) && !nc.isDeleted()) {
                    channels.add(nc);
                }
            }
            return channels;
        }
    }

    @Override // com.android.server.notification.RankingConfig
    public boolean deleteNotificationChannel(String pkg, int uid, String channelId) {
        boolean deletedChannel = false;
        boolean channelBypassedDnd = false;
        synchronized (this.mPackagePreferences) {
            PackagePreferences r = getPackagePreferencesLocked(pkg, uid);
            if (r == null) {
                return false;
            }
            NotificationChannel channel = r.channels.get(channelId);
            if (channel != null) {
                channelBypassedDnd = channel.canBypassDnd();
                deletedChannel = deleteNotificationChannelLocked(channel, pkg, uid);
            }
            if (channelBypassedDnd) {
                updateChannelsBypassingDnd();
            }
            return deletedChannel;
        }
    }

    private boolean deleteNotificationChannelLocked(NotificationChannel channel, String pkg, int uid) {
        if (!channel.isDeleted()) {
            channel.setDeleted(true);
            channel.setDeletedTimeMs(System.currentTimeMillis());
            LogMaker lm = getChannelLog(channel, pkg);
            lm.setType(2);
            MetricsLogger.action(lm);
            this.mNotificationChannelLogger.logNotificationChannelDeleted(channel, uid, pkg);
            return true;
        }
        return false;
    }

    @Override // com.android.server.notification.RankingConfig
    public void permanentlyDeleteNotificationChannel(String pkg, int uid, String channelId) {
        Objects.requireNonNull(pkg);
        Objects.requireNonNull(channelId);
        synchronized (this.mPackagePreferences) {
            PackagePreferences r = getPackagePreferencesLocked(pkg, uid);
            if (r == null) {
                return;
            }
            r.channels.remove(channelId);
        }
    }

    @Override // com.android.server.notification.RankingConfig
    public void permanentlyDeleteNotificationChannels(String pkg, int uid) {
        Objects.requireNonNull(pkg);
        synchronized (this.mPackagePreferences) {
            PackagePreferences r = getPackagePreferencesLocked(pkg, uid);
            if (r == null) {
                return;
            }
            int N = r.channels.size() - 1;
            for (int i = N; i >= 0; i--) {
                String key = r.channels.keyAt(i);
                if (!"miscellaneous".equals(key)) {
                    r.channels.remove(key);
                }
            }
        }
    }

    public boolean shouldHideSilentStatusIcons() {
        return this.mHideSilentStatusBarIcons;
    }

    public void setHideSilentStatusIcons(boolean hide) {
        this.mHideSilentStatusBarIcons = hide;
    }

    public void updateFixedImportance(List<UserInfo> users) {
        for (UserInfo user : users) {
            List<PackageInfo> packages = this.mPm.getInstalledPackagesAsUser(PackageManager.PackageInfoFlags.of(1048576L), user.getUserHandle().getIdentifier());
            for (PackageInfo pi : packages) {
                boolean fixed = this.mPermissionHelper.isPermissionFixed(pi.packageName, user.getUserHandle().getIdentifier());
                if (fixed) {
                    synchronized (this.mPackagePreferences) {
                        PackagePreferences p = getOrCreatePackagePreferencesLocked(pi.packageName, pi.applicationInfo.uid);
                        p.fixedImportance = true;
                        for (NotificationChannel channel : p.channels.values()) {
                            channel.setImportanceLockedByCriticalDeviceFunction(true);
                        }
                    }
                }
            }
        }
    }

    public void updateDefaultApps(int userId, ArraySet<String> toRemove, ArraySet<Pair<String, Integer>> toAdd) {
        synchronized (this.mPackagePreferences) {
            for (PackagePreferences p : this.mPackagePreferences.values()) {
                if (userId == UserHandle.getUserId(p.uid) && toRemove != null && toRemove.contains(p.pkg)) {
                    p.defaultAppLockedImportance = false;
                    if (!p.fixedImportance) {
                        for (NotificationChannel channel : p.channels.values()) {
                            channel.setImportanceLockedByCriticalDeviceFunction(false);
                        }
                    }
                }
            }
            if (toAdd != null) {
                Iterator<Pair<String, Integer>> it = toAdd.iterator();
                while (it.hasNext()) {
                    Pair<String, Integer> approvedApp = it.next();
                    PackagePreferences p2 = getOrCreatePackagePreferencesLocked((String) approvedApp.first, ((Integer) approvedApp.second).intValue());
                    p2.defaultAppLockedImportance = true;
                    for (NotificationChannel channel2 : p2.channels.values()) {
                        channel2.setImportanceLockedByCriticalDeviceFunction(true);
                    }
                }
            }
        }
    }

    public NotificationChannelGroup getNotificationChannelGroupWithChannels(String pkg, int uid, String groupId, boolean includeDeleted) {
        Objects.requireNonNull(pkg);
        synchronized (this.mPackagePreferences) {
            PackagePreferences r = getPackagePreferencesLocked(pkg, uid);
            if (r != null && groupId != null && r.groups.containsKey(groupId)) {
                NotificationChannelGroup group = r.groups.get(groupId).clone();
                group.setChannels(new ArrayList());
                int N = r.channels.size();
                for (int i = 0; i < N; i++) {
                    NotificationChannel nc = r.channels.valueAt(i);
                    if ((includeDeleted || !nc.isDeleted()) && groupId.equals(nc.getGroup())) {
                        group.addChannel(nc);
                    }
                }
                return group;
            }
            return null;
        }
    }

    public NotificationChannelGroup getNotificationChannelGroup(String groupId, String pkg, int uid) {
        Objects.requireNonNull(pkg);
        synchronized (this.mPackagePreferences) {
            PackagePreferences r = getPackagePreferencesLocked(pkg, uid);
            if (r == null) {
                return null;
            }
            return r.groups.get(groupId);
        }
    }

    @Override // com.android.server.notification.RankingConfig
    public ParceledListSlice<NotificationChannelGroup> getNotificationChannelGroups(String pkg, int uid, boolean includeDeleted, boolean includeNonGrouped, boolean includeEmpty) {
        Objects.requireNonNull(pkg);
        Map<String, NotificationChannelGroup> groups = new ArrayMap<>();
        synchronized (this.mPackagePreferences) {
            PackagePreferences r = getPackagePreferencesLocked(pkg, uid);
            if (r == null) {
                return ParceledListSlice.emptyList();
            }
            NotificationChannelGroup nonGrouped = new NotificationChannelGroup(null, null);
            int N = r.channels.size();
            for (int i = 0; i < N; i++) {
                NotificationChannel nc = r.channels.valueAt(i);
                if (includeDeleted || !nc.isDeleted()) {
                    if (nc.getGroup() != null) {
                        if (r.groups.get(nc.getGroup()) != null) {
                            NotificationChannelGroup ncg = groups.get(nc.getGroup());
                            if (ncg == null) {
                                ncg = r.groups.get(nc.getGroup()).clone();
                                ncg.setChannels(new ArrayList());
                                groups.put(nc.getGroup(), ncg);
                            }
                            ncg.addChannel(nc);
                        }
                    } else {
                        nonGrouped.addChannel(nc);
                    }
                }
            }
            if (includeNonGrouped && nonGrouped.getChannels().size() > 0) {
                groups.put(null, nonGrouped);
            }
            if (includeEmpty) {
                for (NotificationChannelGroup group : r.groups.values()) {
                    if (!groups.containsKey(group.getId())) {
                        groups.put(group.getId(), group);
                    }
                }
            }
            return new ParceledListSlice<>(new ArrayList(groups.values()));
        }
    }

    public List<NotificationChannel> deleteNotificationChannelGroup(String pkg, int uid, String groupId) {
        List<NotificationChannel> deletedChannels = new ArrayList<>();
        boolean groupBypassedDnd = false;
        synchronized (this.mPackagePreferences) {
            PackagePreferences r = getPackagePreferencesLocked(pkg, uid);
            if (r != null && !TextUtils.isEmpty(groupId)) {
                NotificationChannelGroup channelGroup = r.groups.remove(groupId);
                if (channelGroup != null) {
                    this.mNotificationChannelLogger.logNotificationChannelGroupDeleted(channelGroup, uid, pkg);
                }
                int N = r.channels.size();
                for (int i = 0; i < N; i++) {
                    NotificationChannel nc = r.channels.valueAt(i);
                    if (groupId.equals(nc.getGroup())) {
                        groupBypassedDnd |= nc.canBypassDnd();
                        deleteNotificationChannelLocked(nc, pkg, uid);
                        deletedChannels.add(nc);
                    }
                }
                if (groupBypassedDnd) {
                    updateChannelsBypassingDnd();
                }
                return deletedChannels;
            }
            return deletedChannels;
        }
    }

    @Override // com.android.server.notification.RankingConfig
    public Collection<NotificationChannelGroup> getNotificationChannelGroups(String pkg, int uid) {
        List<NotificationChannelGroup> groups = new ArrayList<>();
        synchronized (this.mPackagePreferences) {
            PackagePreferences r = getPackagePreferencesLocked(pkg, uid);
            if (r == null) {
                return groups;
            }
            groups.addAll(r.groups.values());
            return groups;
        }
    }

    public NotificationChannelGroup getGroupForChannel(String pkg, int uid, String channelId) {
        NotificationChannel nc;
        NotificationChannelGroup group;
        synchronized (this.mPackagePreferences) {
            PackagePreferences p = getPackagePreferencesLocked(pkg, uid);
            if (p == null || (nc = p.channels.get(channelId)) == null || nc.isDeleted() || nc.getGroup() == null || (group = p.groups.get(nc.getGroup())) == null) {
                return null;
            }
            return group;
        }
    }

    public ArrayList<ConversationChannelWrapper> getConversations(IntArray userIds, boolean onlyImportant) {
        ArrayList<ConversationChannelWrapper> conversations;
        CharSequence name;
        NotificationChannelGroup group;
        synchronized (this.mPackagePreferences) {
            conversations = new ArrayList<>();
            for (PackagePreferences p : this.mPackagePreferences.values()) {
                if (userIds.binarySearch(UserHandle.getUserId(p.uid)) >= 0) {
                    int N = p.channels.size();
                    for (int i = 0; i < N; i++) {
                        NotificationChannel nc = p.channels.valueAt(i);
                        if (!TextUtils.isEmpty(nc.getConversationId()) && !nc.isDeleted() && !nc.isDemoted() && (nc.isImportantConversation() || !onlyImportant)) {
                            ConversationChannelWrapper conversation = new ConversationChannelWrapper();
                            conversation.setPkg(p.pkg);
                            conversation.setUid(p.uid);
                            conversation.setNotificationChannel(nc);
                            NotificationChannel parent = p.channels.get(nc.getParentChannelId());
                            if (parent == null) {
                                name = null;
                            } else {
                                name = parent.getName();
                            }
                            conversation.setParentChannelLabel(name);
                            boolean blockedByGroup = false;
                            if (nc.getGroup() != null && (group = p.groups.get(nc.getGroup())) != null) {
                                if (group.isBlocked()) {
                                    blockedByGroup = true;
                                } else {
                                    conversation.setGroupLabel(group.getName());
                                }
                            }
                            if (!blockedByGroup) {
                                conversations.add(conversation);
                            }
                        }
                    }
                }
            }
        }
        return conversations;
    }

    public ArrayList<ConversationChannelWrapper> getConversations(String pkg, int uid) {
        NotificationChannelGroup group;
        Objects.requireNonNull(pkg);
        synchronized (this.mPackagePreferences) {
            PackagePreferences r = getPackagePreferencesLocked(pkg, uid);
            if (r == null) {
                return new ArrayList<>();
            }
            ArrayList<ConversationChannelWrapper> conversations = new ArrayList<>();
            int N = r.channels.size();
            for (int i = 0; i < N; i++) {
                NotificationChannel nc = r.channels.valueAt(i);
                if (!TextUtils.isEmpty(nc.getConversationId()) && !nc.isDeleted() && !nc.isDemoted()) {
                    ConversationChannelWrapper conversation = new ConversationChannelWrapper();
                    conversation.setPkg(r.pkg);
                    conversation.setUid(r.uid);
                    conversation.setNotificationChannel(nc);
                    conversation.setParentChannelLabel(r.channels.get(nc.getParentChannelId()).getName());
                    boolean blockedByGroup = false;
                    if (nc.getGroup() != null && (group = r.groups.get(nc.getGroup())) != null) {
                        if (group.isBlocked()) {
                            blockedByGroup = true;
                        } else {
                            conversation.setGroupLabel(group.getName());
                        }
                    }
                    if (!blockedByGroup) {
                        conversations.add(conversation);
                    }
                }
            }
            return conversations;
        }
    }

    public List<String> deleteConversations(String pkg, int uid, Set<String> conversationIds) {
        List<String> deletedChannelIds = new ArrayList<>();
        synchronized (this.mPackagePreferences) {
            PackagePreferences r = getPackagePreferencesLocked(pkg, uid);
            if (r == null) {
                return deletedChannelIds;
            }
            int N = r.channels.size();
            for (int i = 0; i < N; i++) {
                NotificationChannel nc = r.channels.valueAt(i);
                if (nc.getConversationId() != null && conversationIds.contains(nc.getConversationId())) {
                    nc.setDeleted(true);
                    nc.setDeletedTimeMs(System.currentTimeMillis());
                    LogMaker lm = getChannelLog(nc, pkg);
                    lm.setType(2);
                    MetricsLogger.action(lm);
                    this.mNotificationChannelLogger.logNotificationChannelDeleted(nc, uid, pkg);
                    deletedChannelIds.add(nc.getId());
                }
            }
            if (!deletedChannelIds.isEmpty() && this.mAreChannelsBypassingDnd) {
                updateChannelsBypassingDnd();
            }
            return deletedChannelIds;
        }
    }

    @Override // com.android.server.notification.RankingConfig
    public ParceledListSlice<NotificationChannel> getNotificationChannels(String pkg, int uid, boolean includeDeleted) {
        Objects.requireNonNull(pkg);
        List<NotificationChannel> channels = new ArrayList<>();
        synchronized (this.mPackagePreferences) {
            PackagePreferences r = getPackagePreferencesLocked(pkg, uid);
            if (r == null) {
                return ParceledListSlice.emptyList();
            }
            int N = r.channels.size();
            for (int i = 0; i < N; i++) {
                NotificationChannel nc = r.channels.valueAt(i);
                if (includeDeleted || !nc.isDeleted()) {
                    channels.add(nc);
                }
            }
            return new ParceledListSlice<>(channels);
        }
    }

    public ParceledListSlice<NotificationChannel> getNotificationChannelsBypassingDnd(String pkg, int uid) {
        List<NotificationChannel> channels = new ArrayList<>();
        synchronized (this.mPackagePreferences) {
            PackagePreferences r = this.mPackagePreferences.get(packagePreferencesKey(pkg, uid));
            if (r != null) {
                for (NotificationChannel channel : r.channels.values()) {
                    if (channelIsLiveLocked(r, channel) && channel.canBypassDnd()) {
                        channels.add(channel);
                    }
                }
            }
        }
        return new ParceledListSlice<>(channels);
    }

    public boolean onlyHasDefaultChannel(String pkg, int uid) {
        synchronized (this.mPackagePreferences) {
            PackagePreferences r = getOrCreatePackagePreferencesLocked(pkg, uid);
            return r.channels.size() == 1 && r.channels.containsKey("miscellaneous");
        }
    }

    public int getDeletedChannelCount(String pkg, int uid) {
        Objects.requireNonNull(pkg);
        int deletedCount = 0;
        synchronized (this.mPackagePreferences) {
            PackagePreferences r = getPackagePreferencesLocked(pkg, uid);
            if (r == null) {
                return 0;
            }
            int N = r.channels.size();
            for (int i = 0; i < N; i++) {
                NotificationChannel nc = r.channels.valueAt(i);
                if (nc.isDeleted()) {
                    deletedCount++;
                }
            }
            return deletedCount;
        }
    }

    public int getBlockedChannelCount(String pkg, int uid) {
        Objects.requireNonNull(pkg);
        int blockedCount = 0;
        synchronized (this.mPackagePreferences) {
            PackagePreferences r = getPackagePreferencesLocked(pkg, uid);
            if (r == null) {
                return 0;
            }
            int N = r.channels.size();
            for (int i = 0; i < N; i++) {
                NotificationChannel nc = r.channels.valueAt(i);
                if (!nc.isDeleted() && nc.getImportance() == 0) {
                    blockedCount++;
                }
            }
            return blockedCount;
        }
    }

    private void syncChannelsBypassingDnd() {
        this.mAreChannelsBypassingDnd = (this.mZenModeHelper.getNotificationPolicy().state & 1) == 1;
        updateChannelsBypassingDnd();
    }

    private void updateChannelsBypassingDnd() {
        ArraySet<Pair<String, Integer>> candidatePkgs = new ArraySet<>();
        int currentUserId = getCurrentUser();
        synchronized (this.mPackagePreferences) {
            int numPackagePreferences = this.mPackagePreferences.size();
            for (int i = 0; i < numPackagePreferences; i++) {
                PackagePreferences r = this.mPackagePreferences.valueAt(i);
                if (currentUserId == UserHandle.getUserId(r.uid)) {
                    Iterator<NotificationChannel> it = r.channels.values().iterator();
                    while (true) {
                        if (it.hasNext()) {
                            NotificationChannel channel = it.next();
                            if (channelIsLiveLocked(r, channel) && channel.canBypassDnd()) {
                                candidatePkgs.add(new Pair<>(r.pkg, Integer.valueOf(r.uid)));
                                break;
                            }
                        }
                    }
                }
            }
        }
        for (int i2 = candidatePkgs.size() - 1; i2 >= 0; i2--) {
            Pair<String, Integer> app = candidatePkgs.valueAt(i2);
            if (!this.mPermissionHelper.hasPermission(((Integer) app.second).intValue())) {
                candidatePkgs.removeAt(i2);
            }
        }
        int i3 = candidatePkgs.size();
        boolean haveBypassingApps = i3 > 0;
        if (this.mAreChannelsBypassingDnd != haveBypassingApps) {
            this.mAreChannelsBypassingDnd = haveBypassingApps;
            updateZenPolicy(haveBypassingApps);
        }
    }

    private int getCurrentUser() {
        long identity = Binder.clearCallingIdentity();
        int currentUserId = ActivityManager.getCurrentUser();
        Binder.restoreCallingIdentity(identity);
        return currentUserId;
    }

    private boolean channelIsLiveLocked(PackagePreferences pkgPref, NotificationChannel channel) {
        return (isGroupBlocked(pkgPref.pkg, pkgPref.uid, channel.getGroup()) || channel.isDeleted() || channel.getImportance() == 0) ? false : true;
    }

    public void updateZenPolicy(boolean areChannelsBypassingDnd) {
        NotificationManager.Policy policy = this.mZenModeHelper.getNotificationPolicy();
        this.mZenModeHelper.setNotificationPolicy(new NotificationManager.Policy(policy.priorityCategories, policy.priorityCallSenders, policy.priorityMessageSenders, policy.suppressedVisualEffects, areChannelsBypassingDnd ? 1 : 0, policy.priorityConversationSenders));
    }

    public boolean areChannelsBypassingDnd() {
        return this.mAreChannelsBypassingDnd;
    }

    public void setAppImportanceLocked(String packageName, int uid) {
        synchronized (this.mPackagePreferences) {
            PackagePreferences prefs = getOrCreatePackagePreferencesLocked(packageName, uid);
            if ((prefs.lockedAppFields & 1) != 0) {
                return;
            }
            prefs.lockedAppFields |= 1;
            updateConfig();
        }
    }

    public String getNotificationDelegate(String sourcePkg, int sourceUid) {
        synchronized (this.mPackagePreferences) {
            PackagePreferences prefs = getPackagePreferencesLocked(sourcePkg, sourceUid);
            if (prefs != null && prefs.delegate != null) {
                if (prefs.delegate.mUserAllowed && prefs.delegate.mEnabled) {
                    return prefs.delegate.mPkg;
                }
                return null;
            }
            return null;
        }
    }

    public void setNotificationDelegate(String sourcePkg, int sourceUid, String delegatePkg, int delegateUid) {
        boolean userAllowed;
        synchronized (this.mPackagePreferences) {
            PackagePreferences prefs = getOrCreatePackagePreferencesLocked(sourcePkg, sourceUid);
            if (prefs.delegate != null && !prefs.delegate.mUserAllowed) {
                userAllowed = false;
                Delegate delegate = new Delegate(delegatePkg, delegateUid, true, userAllowed);
                prefs.delegate = delegate;
            }
            userAllowed = true;
            Delegate delegate2 = new Delegate(delegatePkg, delegateUid, true, userAllowed);
            prefs.delegate = delegate2;
        }
        updateConfig();
    }

    public void revokeNotificationDelegate(String sourcePkg, int sourceUid) {
        boolean changed = false;
        synchronized (this.mPackagePreferences) {
            PackagePreferences prefs = getPackagePreferencesLocked(sourcePkg, sourceUid);
            if (prefs != null && prefs.delegate != null) {
                prefs.delegate.mEnabled = false;
                changed = true;
            }
        }
        if (changed) {
            updateConfig();
        }
    }

    public void toggleNotificationDelegate(String sourcePkg, int sourceUid, boolean userAllowed) {
        boolean changed = false;
        synchronized (this.mPackagePreferences) {
            PackagePreferences prefs = getPackagePreferencesLocked(sourcePkg, sourceUid);
            if (prefs != null && prefs.delegate != null) {
                prefs.delegate.mUserAllowed = userAllowed;
                changed = true;
            }
        }
        if (changed) {
            updateConfig();
        }
    }

    public boolean isDelegateAllowed(String sourcePkg, int sourceUid, String potentialDelegatePkg, int potentialDelegateUid) {
        boolean z;
        synchronized (this.mPackagePreferences) {
            PackagePreferences prefs = getPackagePreferencesLocked(sourcePkg, sourceUid);
            z = prefs != null && prefs.isValidDelegate(potentialDelegatePkg, potentialDelegateUid);
        }
        return z;
    }

    void lockFieldsForUpdateLocked(NotificationChannel original, NotificationChannel update) {
        if (original.canBypassDnd() != update.canBypassDnd()) {
            update.lockFields(1);
        }
        if (original.getLockscreenVisibility() != update.getLockscreenVisibility()) {
            update.lockFields(2);
        }
        if (original.getImportance() != update.getImportance()) {
            update.lockFields(4);
        }
        if (original.shouldShowLights() != update.shouldShowLights() || original.getLightColor() != update.getLightColor()) {
            update.lockFields(8);
        }
        if (!Objects.equals(original.getSound(), update.getSound())) {
            update.lockFields(32);
        }
        if (!Arrays.equals(original.getVibrationPattern(), update.getVibrationPattern()) || original.shouldVibrate() != update.shouldVibrate()) {
            update.lockFields(16);
        }
        if (original.canShowBadge() != update.canShowBadge()) {
            update.lockFields(128);
        }
        if (original.getAllowBubbles() != update.getAllowBubbles()) {
            update.lockFields(256);
        }
    }

    public void dump(PrintWriter pw, String prefix, NotificationManagerService.DumpFilter filter, ArrayMap<Pair<Integer, String>, Pair<Boolean, Boolean>> pkgPermissions) {
        pw.print(prefix);
        pw.println("per-package config version: " + this.XML_VERSION);
        pw.println("PackagePreferences:");
        synchronized (this.mPackagePreferences) {
            dumpPackagePreferencesLocked(pw, prefix, filter, this.mPackagePreferences, pkgPermissions);
        }
        pw.println("Restored without uid:");
        dumpPackagePreferencesLocked(pw, prefix, filter, this.mRestoredWithoutUids, (ArrayMap<Pair<Integer, String>, Pair<Boolean, Boolean>>) null);
    }

    public void dump(ProtoOutputStream proto, NotificationManagerService.DumpFilter filter, ArrayMap<Pair<Integer, String>, Pair<Boolean, Boolean>> pkgPermissions) {
        synchronized (this.mPackagePreferences) {
            dumpPackagePreferencesLocked(proto, 2246267895810L, filter, this.mPackagePreferences, pkgPermissions);
        }
        dumpPackagePreferencesLocked(proto, 2246267895811L, filter, this.mRestoredWithoutUids, (ArrayMap<Pair<Integer, String>, Pair<Boolean, Boolean>>) null);
    }

    private void dumpPackagePreferencesLocked(PrintWriter pw, String prefix, NotificationManagerService.DumpFilter filter, ArrayMap<String, PackagePreferences> packagePreferences, ArrayMap<Pair<Integer, String>, Pair<Boolean, Boolean>> packagePermissions) {
        Set<Pair<Integer, String>> pkgsWithPermissionsToHandle = packagePermissions != null ? packagePermissions.keySet() : null;
        int N = packagePreferences.size();
        int i = 0;
        while (true) {
            if (i >= N) {
                break;
            }
            PackagePreferences r = packagePreferences.valueAt(i);
            if (filter.matches(r.pkg)) {
                pw.print(prefix);
                pw.print("  AppSettings: ");
                pw.print(r.pkg);
                pw.print(" (");
                pw.print(r.uid != -10000 ? Integer.toString(r.uid) : "UNKNOWN_UID");
                pw.print(')');
                Pair<Integer, String> key = new Pair<>(Integer.valueOf(r.uid), r.pkg);
                if (packagePermissions != null && pkgsWithPermissionsToHandle.contains(key)) {
                    pw.print(" importance=");
                    pw.print(NotificationListenerService.Ranking.importanceToString(((Boolean) packagePermissions.get(key).first).booleanValue() ? 3 : 0));
                    pw.print(" userSet=");
                    pw.print(packagePermissions.get(key).second);
                    pkgsWithPermissionsToHandle.remove(key);
                }
                if (r.priority != 0) {
                    pw.print(" priority=");
                    pw.print(Notification.priorityToString(r.priority));
                }
                if (r.visibility != -1000) {
                    pw.print(" visibility=");
                    pw.print(Notification.visibilityToString(r.visibility));
                }
                if (!r.showBadge) {
                    pw.print(" showBadge=");
                    pw.print(r.showBadge);
                }
                if (r.defaultAppLockedImportance) {
                    pw.print(" defaultAppLocked=");
                    pw.print(r.defaultAppLockedImportance);
                }
                if (r.fixedImportance) {
                    pw.print(" fixedImportance=");
                    pw.print(r.fixedImportance);
                }
                pw.println();
                for (NotificationChannel channel : r.channels.values()) {
                    pw.print(prefix);
                    channel.dump(pw, "    ", filter.redact);
                }
                for (NotificationChannelGroup group : r.groups.values()) {
                    pw.print(prefix);
                    pw.print("  ");
                    pw.print("  ");
                    pw.println(group);
                }
            }
            i++;
        }
        if (pkgsWithPermissionsToHandle != null) {
            for (Pair<Integer, String> p : pkgsWithPermissionsToHandle) {
                if (filter.matches((String) p.second)) {
                    pw.print(prefix);
                    pw.print("  AppSettings: ");
                    pw.print((String) p.second);
                    pw.print(" (");
                    pw.print(((Integer) p.first).intValue() == -10000 ? "UNKNOWN_UID" : Integer.toString(((Integer) p.first).intValue()));
                    pw.print(')');
                    pw.print(" importance=");
                    pw.print(NotificationListenerService.Ranking.importanceToString(((Boolean) packagePermissions.get(p).first).booleanValue() ? 3 : 0));
                    pw.print(" userSet=");
                    pw.print(packagePermissions.get(p).second);
                    pw.println();
                }
            }
        }
    }

    private void dumpPackagePreferencesLocked(ProtoOutputStream proto, long fieldId, NotificationManagerService.DumpFilter filter, ArrayMap<String, PackagePreferences> packagePreferences, ArrayMap<Pair<Integer, String>, Pair<Boolean, Boolean>> packagePermissions) {
        Set<Pair<Integer, String>> pkgsWithPermissionsToHandle = packagePermissions != null ? packagePermissions.keySet() : null;
        int N = packagePreferences.size();
        for (int i = 0; i < N; i++) {
            PackagePreferences r = packagePreferences.valueAt(i);
            if (filter.matches(r.pkg)) {
                long fToken = proto.start(fieldId);
                proto.write(CompanionAppsPermissions.AppPermissions.PACKAGE_NAME, r.pkg);
                proto.write(1120986464258L, r.uid);
                Pair<Integer, String> key = new Pair<>(Integer.valueOf(r.uid), r.pkg);
                if (packagePermissions != null && pkgsWithPermissionsToHandle.contains(key)) {
                    proto.write(1172526071811L, ((Boolean) packagePermissions.get(key).first).booleanValue() ? 3 : 0);
                    pkgsWithPermissionsToHandle.remove(key);
                }
                proto.write(1120986464260L, r.priority);
                proto.write(1172526071813L, r.visibility);
                proto.write(1133871366150L, r.showBadge);
                for (NotificationChannel channel : r.channels.values()) {
                    channel.dumpDebug(proto, 2246267895815L);
                }
                for (NotificationChannelGroup group : r.groups.values()) {
                    group.dumpDebug(proto, 2246267895816L);
                }
                proto.end(fToken);
            }
        }
        if (pkgsWithPermissionsToHandle != null) {
            for (Pair<Integer, String> p : pkgsWithPermissionsToHandle) {
                if (filter.matches((String) p.second)) {
                    long fToken2 = proto.start(fieldId);
                    proto.write(CompanionAppsPermissions.AppPermissions.PACKAGE_NAME, (String) p.second);
                    proto.write(1120986464258L, ((Integer) p.first).intValue());
                    proto.write(1172526071811L, ((Boolean) packagePermissions.get(p).first).booleanValue() ? 3 : 0);
                    proto.end(fToken2);
                }
            }
        }
    }

    public void pullPackagePreferencesStats(List<StatsEvent> events, ArrayMap<Pair<Integer, String>, Pair<Boolean, Boolean>> pkgPermissions) {
        Set<Pair<Integer, String>> pkgsWithPermissionsToHandle = null;
        if (pkgPermissions != null) {
            pkgsWithPermissionsToHandle = pkgPermissions.keySet();
        }
        int pulledEvents = 0;
        synchronized (this.mPackagePreferences) {
            int i = 0;
            while (true) {
                int i2 = 3;
                if (i >= this.mPackagePreferences.size() || pulledEvents > 1000) {
                    break;
                }
                pulledEvents++;
                SysUiStatsEvent.Builder event = this.mStatsEventBuilderFactory.newBuilder().setAtomId(FrameworkStatsLog.PACKAGE_NOTIFICATION_PREFERENCES);
                PackagePreferences r = this.mPackagePreferences.valueAt(i);
                event.writeInt(r.uid);
                event.addBooleanAnnotation((byte) 1, true);
                boolean importanceIsUserSet = false;
                int importance = 0;
                Pair<Integer, String> key = new Pair<>(Integer.valueOf(r.uid), r.pkg);
                if (pkgPermissions != null && pkgsWithPermissionsToHandle.contains(key)) {
                    Pair<Boolean, Boolean> permissionPair = pkgPermissions.get(key);
                    if (!((Boolean) permissionPair.first).booleanValue()) {
                        i2 = 0;
                    }
                    importance = i2;
                    importanceIsUserSet = ((Boolean) permissionPair.second).booleanValue();
                    pkgsWithPermissionsToHandle.remove(key);
                }
                event.writeInt(importance);
                event.writeInt(r.visibility);
                event.writeInt(r.lockedAppFields);
                event.writeBoolean(importanceIsUserSet);
                events.add(event.build());
                i++;
            }
        }
        if (pkgPermissions != null) {
            for (Pair<Integer, String> p : pkgsWithPermissionsToHandle) {
                if (pulledEvents <= 1000) {
                    pulledEvents++;
                    SysUiStatsEvent.Builder event2 = this.mStatsEventBuilderFactory.newBuilder().setAtomId(FrameworkStatsLog.PACKAGE_NOTIFICATION_PREFERENCES);
                    event2.writeInt(((Integer) p.first).intValue());
                    event2.addBooleanAnnotation((byte) 1, true);
                    event2.writeInt(((Boolean) pkgPermissions.get(p).first).booleanValue() ? 3 : 0);
                    event2.writeInt(-1000);
                    event2.writeInt(0);
                    event2.writeBoolean(((Boolean) pkgPermissions.get(p).second).booleanValue());
                    events.add(event2.build());
                } else {
                    return;
                }
            }
        }
    }

    public void pullPackageChannelPreferencesStats(List<StatsEvent> events) {
        synchronized (this.mPackagePreferences) {
            int totalChannelsPulled = 0;
            for (int i = 0; i < this.mPackagePreferences.size() && totalChannelsPulled <= 2000; i++) {
                PackagePreferences r = this.mPackagePreferences.valueAt(i);
                for (NotificationChannel channel : r.channels.values()) {
                    totalChannelsPulled++;
                    if (totalChannelsPulled > 2000) {
                        break;
                    }
                    SysUiStatsEvent.Builder event = this.mStatsEventBuilderFactory.newBuilder().setAtomId(FrameworkStatsLog.PACKAGE_NOTIFICATION_CHANNEL_PREFERENCES);
                    event.writeInt(r.uid);
                    boolean z = true;
                    event.addBooleanAnnotation((byte) 1, true);
                    event.writeString(channel.getId());
                    event.writeString(channel.getName().toString());
                    event.writeString(channel.getDescription());
                    event.writeInt(channel.getImportance());
                    event.writeInt(channel.getUserLockedFields());
                    event.writeBoolean(channel.isDeleted());
                    if (channel.getConversationId() == null) {
                        z = false;
                    }
                    event.writeBoolean(z);
                    event.writeBoolean(channel.isDemoted());
                    event.writeBoolean(channel.isImportantConversation());
                    events.add(event.build());
                }
            }
        }
    }

    public void pullPackageChannelGroupPreferencesStats(List<StatsEvent> events) {
        synchronized (this.mPackagePreferences) {
            int totalGroupsPulled = 0;
            for (int i = 0; i < this.mPackagePreferences.size() && totalGroupsPulled <= 1000; i++) {
                PackagePreferences r = this.mPackagePreferences.valueAt(i);
                for (NotificationChannelGroup groupChannel : r.groups.values()) {
                    totalGroupsPulled++;
                    if (totalGroupsPulled > 1000) {
                        break;
                    }
                    SysUiStatsEvent.Builder event = this.mStatsEventBuilderFactory.newBuilder().setAtomId(FrameworkStatsLog.PACKAGE_NOTIFICATION_CHANNEL_GROUP_PREFERENCES);
                    event.writeInt(r.uid);
                    event.addBooleanAnnotation((byte) 1, true);
                    event.writeString(groupChannel.getId());
                    event.writeString(groupChannel.getName().toString());
                    event.writeString(groupChannel.getDescription());
                    event.writeBoolean(groupChannel.isBlocked());
                    event.writeInt(groupChannel.getUserLockedFields());
                    events.add(event.build());
                }
            }
        }
    }

    public JSONObject dumpJson(NotificationManagerService.DumpFilter filter, ArrayMap<Pair<Integer, String>, Pair<Boolean, Boolean>> pkgPermissions) {
        Set<Pair<Integer, String>> pkgsWithPermissionsToHandle;
        JSONObject ranking = new JSONObject();
        JSONArray PackagePreferencess = new JSONArray();
        try {
            ranking.put("noUid", this.mRestoredWithoutUids.size());
        } catch (JSONException e) {
        }
        if (pkgPermissions == null) {
            pkgsWithPermissionsToHandle = null;
        } else {
            Set<Pair<Integer, String>> pkgsWithPermissionsToHandle2 = pkgPermissions.keySet();
            pkgsWithPermissionsToHandle = pkgsWithPermissionsToHandle2;
        }
        synchronized (this.mPackagePreferences) {
            int N = this.mPackagePreferences.size();
            int i = 0;
            while (true) {
                int i2 = 3;
                if (i >= N) {
                    break;
                }
                PackagePreferences r = this.mPackagePreferences.valueAt(i);
                if (filter == null || filter.matches(r.pkg)) {
                    JSONObject PackagePreferences2 = new JSONObject();
                    try {
                        PackagePreferences2.put("userId", UserHandle.getUserId(r.uid));
                        PackagePreferences2.put(DomainVerificationLegacySettings.ATTR_PACKAGE_NAME, r.pkg);
                        Pair<Integer, String> key = new Pair<>(Integer.valueOf(r.uid), r.pkg);
                        if (pkgPermissions != null && pkgsWithPermissionsToHandle.contains(key)) {
                            if (!((Boolean) pkgPermissions.get(key).first).booleanValue()) {
                                i2 = 0;
                            }
                            PackagePreferences2.put(ATT_IMPORTANCE, NotificationListenerService.Ranking.importanceToString(i2));
                            pkgsWithPermissionsToHandle.remove(key);
                        }
                        if (r.priority != 0) {
                            PackagePreferences2.put(ATT_PRIORITY, Notification.priorityToString(r.priority));
                        }
                        if (r.visibility != -1000) {
                            PackagePreferences2.put(ATT_VISIBILITY, Notification.visibilityToString(r.visibility));
                        }
                        if (!r.showBadge) {
                            PackagePreferences2.put("showBadge", Boolean.valueOf(r.showBadge));
                        }
                        JSONArray channels = new JSONArray();
                        for (NotificationChannel channel : r.channels.values()) {
                            channels.put(channel.toJson());
                        }
                        PackagePreferences2.put("channels", channels);
                        JSONArray groups = new JSONArray();
                        for (NotificationChannelGroup group : r.groups.values()) {
                            groups.put(group.toJson());
                            key = key;
                        }
                        PackagePreferences2.put("groups", groups);
                    } catch (JSONException e2) {
                    }
                    PackagePreferencess.put(PackagePreferences2);
                }
                i++;
            }
        }
        if (pkgsWithPermissionsToHandle != null) {
            for (Pair<Integer, String> p : pkgsWithPermissionsToHandle) {
                if (filter == null || filter.matches((String) p.second)) {
                    JSONObject PackagePreferences3 = new JSONObject();
                    try {
                        PackagePreferences3.put("userId", UserHandle.getUserId(((Integer) p.first).intValue()));
                        PackagePreferences3.put(DomainVerificationLegacySettings.ATTR_PACKAGE_NAME, p.second);
                        PackagePreferences3.put(ATT_IMPORTANCE, NotificationListenerService.Ranking.importanceToString(((Boolean) pkgPermissions.get(p).first).booleanValue() ? 3 : 0));
                    } catch (JSONException e3) {
                    }
                    PackagePreferencess.put(PackagePreferences3);
                }
            }
        }
        try {
            ranking.put("PackagePreferencess", PackagePreferencess);
        } catch (JSONException e4) {
        }
        return ranking;
    }

    public JSONArray dumpBansJson(NotificationManagerService.DumpFilter filter, ArrayMap<Pair<Integer, String>, Pair<Boolean, Boolean>> pkgPermissions) {
        JSONArray bans = new JSONArray();
        Map<Integer, String> packageBans = getPermissionBasedPackageBans(pkgPermissions);
        for (Map.Entry<Integer, String> ban : packageBans.entrySet()) {
            int userId = UserHandle.getUserId(ban.getKey().intValue());
            String packageName = ban.getValue();
            if (filter == null || filter.matches(packageName)) {
                JSONObject banJson = new JSONObject();
                try {
                    banJson.put("userId", userId);
                    banJson.put(DomainVerificationLegacySettings.ATTR_PACKAGE_NAME, packageName);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                bans.put(banJson);
            }
        }
        return bans;
    }

    public Map<Integer, String> getPackageBans() {
        ArrayMap<Integer, String> packageBans;
        synchronized (this.mPackagePreferences) {
            int N = this.mPackagePreferences.size();
            packageBans = new ArrayMap<>(N);
            for (int i = 0; i < N; i++) {
                PackagePreferences r = this.mPackagePreferences.valueAt(i);
                if (r.importance == 0) {
                    packageBans.put(Integer.valueOf(r.uid), r.pkg);
                }
            }
        }
        return packageBans;
    }

    protected Map<Integer, String> getPermissionBasedPackageBans(ArrayMap<Pair<Integer, String>, Pair<Boolean, Boolean>> pkgPermissions) {
        ArrayMap<Integer, String> packageBans = new ArrayMap<>();
        if (pkgPermissions != null) {
            for (Pair<Integer, String> p : pkgPermissions.keySet()) {
                if (!((Boolean) pkgPermissions.get(p).first).booleanValue()) {
                    packageBans.put((Integer) p.first, (String) p.second);
                }
            }
        }
        return packageBans;
    }

    public JSONArray dumpChannelsJson(NotificationManagerService.DumpFilter filter) {
        JSONArray channels = new JSONArray();
        Map<String, Integer> packageChannels = getPackageChannels();
        for (Map.Entry<String, Integer> channelCount : packageChannels.entrySet()) {
            String packageName = channelCount.getKey();
            if (filter == null || filter.matches(packageName)) {
                JSONObject channelCountJson = new JSONObject();
                try {
                    channelCountJson.put(DomainVerificationLegacySettings.ATTR_PACKAGE_NAME, packageName);
                    channelCountJson.put("channelCount", channelCount.getValue());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                channels.put(channelCountJson);
            }
        }
        return channels;
    }

    private Map<String, Integer> getPackageChannels() {
        ArrayMap<String, Integer> packageChannels = new ArrayMap<>();
        synchronized (this.mPackagePreferences) {
            for (int i = 0; i < this.mPackagePreferences.size(); i++) {
                PackagePreferences r = this.mPackagePreferences.valueAt(i);
                int channelCount = 0;
                for (int j = 0; j < r.channels.size(); j++) {
                    if (!r.channels.valueAt(j).isDeleted()) {
                        channelCount++;
                    }
                }
                packageChannels.put(r.pkg, Integer.valueOf(channelCount));
            }
        }
        return packageChannels;
    }

    public void onUserRemoved(int userId) {
        synchronized (this.mPackagePreferences) {
            int N = this.mPackagePreferences.size();
            for (int i = N - 1; i >= 0; i--) {
                PackagePreferences PackagePreferences2 = this.mPackagePreferences.valueAt(i);
                if (UserHandle.getUserId(PackagePreferences2.uid) == userId) {
                    this.mPackagePreferences.removeAt(i);
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void onLocaleChanged(Context context, int userId) {
        synchronized (this.mPackagePreferences) {
            int N = this.mPackagePreferences.size();
            for (int i = 0; i < N; i++) {
                PackagePreferences PackagePreferences2 = this.mPackagePreferences.valueAt(i);
                if (UserHandle.getUserId(PackagePreferences2.uid) == userId && PackagePreferences2.channels.containsKey("miscellaneous")) {
                    PackagePreferences2.channels.get("miscellaneous").setName(context.getResources().getString(17040144));
                }
            }
        }
    }

    public boolean onPackagesChanged(boolean removingPackage, int changeUserId, String[] pkgList, int[] uidList) {
        if (pkgList == null || pkgList.length == 0) {
            return false;
        }
        boolean updated = false;
        if (!removingPackage) {
            for (String pkg : pkgList) {
                PackagePreferences r = this.mRestoredWithoutUids.get(unrestoredPackageKey(pkg, changeUserId));
                if (r != null) {
                    try {
                        r.uid = this.mPm.getPackageUidAsUser(r.pkg, changeUserId);
                        this.mRestoredWithoutUids.remove(unrestoredPackageKey(pkg, changeUserId));
                        synchronized (this.mPackagePreferences) {
                            this.mPackagePreferences.put(packagePreferencesKey(r.pkg, r.uid), r);
                        }
                        if (r.migrateToPm) {
                            try {
                                PermissionHelper.PackagePermission p = new PermissionHelper.PackagePermission(r.pkg, UserHandle.getUserId(r.uid), r.importance != 0, hasUserConfiguredSettings(r));
                                this.mPermissionHelper.setNotificationPermission(p);
                            } catch (Exception e) {
                                Slog.e(TAG, "could not migrate setting for " + r.pkg, e);
                            }
                        }
                        updated = true;
                    } catch (Exception e2) {
                        Slog.e(TAG, "could not restore " + r.pkg, e2);
                    }
                }
                try {
                    synchronized (this.mPackagePreferences) {
                        PackagePreferences fullPackagePreferences = getPackagePreferencesLocked(pkg, this.mPm.getPackageUidAsUser(pkg, changeUserId));
                        if (fullPackagePreferences != null) {
                            updated = updated | createDefaultChannelIfNeededLocked(fullPackagePreferences) | deleteDefaultChannelIfNeededLocked(fullPackagePreferences);
                        }
                    }
                } catch (PackageManager.NameNotFoundException e3) {
                }
            }
        } else {
            int size = Math.min(pkgList.length, uidList.length);
            for (int i = 0; i < size; i++) {
                String pkg2 = pkgList[i];
                int uid = uidList[i];
                synchronized (this.mPackagePreferences) {
                    this.mPackagePreferences.remove(packagePreferencesKey(pkg2, uid));
                }
                this.mRestoredWithoutUids.remove(unrestoredPackageKey(pkg2, changeUserId));
                updated = true;
            }
        }
        if (updated) {
            updateConfig();
        }
        return updated;
    }

    public void clearData(String pkg, int uid) {
        synchronized (this.mPackagePreferences) {
            PackagePreferences p = getPackagePreferencesLocked(pkg, uid);
            if (p != null) {
                p.channels = new ArrayMap<>();
                p.groups = new ArrayMap();
                p.delegate = null;
                p.lockedAppFields = 0;
                p.bubblePreference = 0;
                p.importance = -1000;
                p.priority = 0;
                p.visibility = -1000;
                p.showBadge = true;
            }
        }
    }

    private LogMaker getChannelLog(NotificationChannel channel, String pkg) {
        return new LogMaker(856).setType(6).setPackageName(pkg).addTaggedData(857, channel.getId()).addTaggedData(858, Integer.valueOf(channel.getImportance()));
    }

    private LogMaker getChannelGroupLog(String groupId, String pkg) {
        return new LogMaker(859).setType(6).addTaggedData(860, groupId).setPackageName(pkg);
    }

    public void updateMediaNotificationFilteringEnabled() {
        boolean newValue = Settings.Global.getInt(this.mContext.getContentResolver(), "qs_media_controls", 1) > 0;
        if (newValue != this.mIsMediaNotificationFilteringEnabled) {
            this.mIsMediaNotificationFilteringEnabled = newValue;
            updateConfig();
        }
    }

    @Override // com.android.server.notification.RankingConfig
    public boolean isMediaNotificationFilteringEnabled() {
        return this.mIsMediaNotificationFilteringEnabled;
    }

    public void updateBadgingEnabled() {
        if (this.mBadgingEnabled == null) {
            this.mBadgingEnabled = new SparseBooleanArray();
        }
        boolean changed = false;
        for (int index = 0; index < this.mBadgingEnabled.size(); index++) {
            int userId = this.mBadgingEnabled.keyAt(index);
            boolean oldValue = this.mBadgingEnabled.get(userId);
            boolean z = true;
            boolean newValue = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "notification_badging", 1, userId) != 0;
            this.mBadgingEnabled.put(userId, newValue);
            if (oldValue == newValue) {
                z = false;
            }
            changed |= z;
        }
        if (changed) {
            updateConfig();
        }
    }

    @Override // com.android.server.notification.RankingConfig
    public boolean badgingEnabled(UserHandle userHandle) {
        int userId = userHandle.getIdentifier();
        if (userId == -1) {
            return false;
        }
        if (this.mBadgingEnabled.indexOfKey(userId) < 0) {
            this.mBadgingEnabled.put(userId, Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "notification_badging", 1, userId) != 0);
        }
        return this.mBadgingEnabled.get(userId, true);
    }

    public void updateBubblesEnabled() {
        if (this.mBubblesEnabled == null) {
            this.mBubblesEnabled = new SparseBooleanArray();
        }
        boolean changed = false;
        for (int index = 0; index < this.mBubblesEnabled.size(); index++) {
            int userId = this.mBubblesEnabled.keyAt(index);
            boolean oldValue = this.mBubblesEnabled.get(userId);
            boolean z = true;
            boolean newValue = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "notification_bubbles", 1, userId) != 0;
            this.mBubblesEnabled.put(userId, newValue);
            if (oldValue == newValue) {
                z = false;
            }
            changed |= z;
        }
        if (changed) {
            updateConfig();
        }
    }

    @Override // com.android.server.notification.RankingConfig
    public boolean bubblesEnabled(UserHandle userHandle) {
        int userId = userHandle.getIdentifier();
        if (userId == -1) {
            return false;
        }
        if (this.mBubblesEnabled.indexOfKey(userId) < 0) {
            this.mBubblesEnabled.put(userId, Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "notification_bubbles", 1, userId) != 0);
        }
        return this.mBubblesEnabled.get(userId, true);
    }

    public void updateLockScreenPrivateNotifications() {
        if (this.mLockScreenPrivateNotifications == null) {
            this.mLockScreenPrivateNotifications = new SparseBooleanArray();
        }
        boolean changed = false;
        for (int index = 0; index < this.mLockScreenPrivateNotifications.size(); index++) {
            int userId = this.mLockScreenPrivateNotifications.keyAt(index);
            boolean oldValue = this.mLockScreenPrivateNotifications.get(userId);
            boolean z = true;
            boolean newValue = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "lock_screen_allow_private_notifications", 1, userId) != 0;
            this.mLockScreenPrivateNotifications.put(userId, newValue);
            if (oldValue == newValue) {
                z = false;
            }
            changed |= z;
        }
        if (changed) {
            updateConfig();
        }
    }

    public void updateLockScreenShowNotifications() {
        if (this.mLockScreenShowNotifications == null) {
            this.mLockScreenShowNotifications = new SparseBooleanArray();
        }
        boolean changed = false;
        for (int index = 0; index < this.mLockScreenShowNotifications.size(); index++) {
            int userId = this.mLockScreenShowNotifications.keyAt(index);
            boolean oldValue = this.mLockScreenShowNotifications.get(userId);
            boolean z = true;
            boolean newValue = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "lock_screen_show_notifications", 1, userId) != 0;
            this.mLockScreenShowNotifications.put(userId, newValue);
            if (oldValue == newValue) {
                z = false;
            }
            changed |= z;
        }
        if (changed) {
            updateConfig();
        }
    }

    @Override // com.android.server.notification.RankingConfig
    public boolean canShowNotificationsOnLockscreen(int userId) {
        if (this.mLockScreenShowNotifications == null) {
            this.mLockScreenShowNotifications = new SparseBooleanArray();
        }
        return this.mLockScreenShowNotifications.get(userId, true);
    }

    @Override // com.android.server.notification.RankingConfig
    public boolean canShowPrivateNotificationsOnLockScreen(int userId) {
        if (this.mLockScreenPrivateNotifications == null) {
            this.mLockScreenPrivateNotifications = new SparseBooleanArray();
        }
        return this.mLockScreenPrivateNotifications.get(userId, true);
    }

    public void unlockAllNotificationChannels() {
        synchronized (this.mPackagePreferences) {
            int numPackagePreferences = this.mPackagePreferences.size();
            for (int i = 0; i < numPackagePreferences; i++) {
                PackagePreferences r = this.mPackagePreferences.valueAt(i);
                for (NotificationChannel channel : r.channels.values()) {
                    channel.unlockFields(4);
                }
            }
        }
    }

    private void updateConfig() {
        this.mRankingHandler.requestSort();
    }

    private static String packagePreferencesKey(String pkg, int uid) {
        return pkg + "|" + uid;
    }

    private static String unrestoredPackageKey(String pkg, int userId) {
        return pkg + "|" + userId;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static class PackagePreferences {
        int bubblePreference;
        ArrayMap<String, NotificationChannel> channels;
        boolean defaultAppLockedImportance;
        Delegate delegate;
        boolean fixedImportance;
        Map<String, NotificationChannelGroup> groups;
        boolean hasSentInvalidMessage;
        boolean hasSentValidBubble;
        boolean hasSentValidMessage;
        int importance;
        int lockedAppFields;
        boolean migrateToPm;
        String pkg;
        boolean playSound;
        boolean playVibration;
        int priority;
        boolean showBadge;
        int uid;
        boolean userDemotedMsgApp;
        int visibility;

        private PackagePreferences() {
            this.uid = -10000;
            this.importance = -1000;
            this.priority = 0;
            this.visibility = -1000;
            this.showBadge = true;
            this.bubblePreference = 0;
            this.lockedAppFields = 0;
            this.playSound = true;
            this.playVibration = true;
            this.defaultAppLockedImportance = false;
            this.fixedImportance = false;
            this.hasSentInvalidMessage = false;
            this.hasSentValidMessage = false;
            this.userDemotedMsgApp = false;
            this.hasSentValidBubble = false;
            this.migrateToPm = false;
            this.delegate = null;
            this.channels = new ArrayMap<>();
            this.groups = new ConcurrentHashMap();
        }

        public boolean isValidDelegate(String pkg, int uid) {
            Delegate delegate = this.delegate;
            return delegate != null && delegate.isAllowed(pkg, uid);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static class Delegate {
        static final boolean DEFAULT_ENABLED = true;
        static final boolean DEFAULT_USER_ALLOWED = true;
        boolean mEnabled;
        String mPkg;
        int mUid;
        boolean mUserAllowed;

        Delegate(String pkg, int uid, boolean enabled, boolean userAllowed) {
            this.mUid = -10000;
            this.mEnabled = true;
            this.mUserAllowed = true;
            this.mPkg = pkg;
            this.mUid = uid;
            this.mEnabled = enabled;
            this.mUserAllowed = userAllowed;
        }

        public boolean isAllowed(String pkg, int uid) {
            return pkg != null && uid != -10000 && pkg.equals(this.mPkg) && uid == this.mUid && this.mUserAllowed && this.mEnabled;
        }
    }
}
