package com.android.server.am;

import android.app.ActivityManager;
import android.app.ActivityManagerInternal;
import android.app.AppOpsManager;
import android.app.IActivityManager;
import android.app.IUidObserver;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.role.OnRoleHoldersChangedListener;
import android.app.role.RoleManager;
import android.app.usage.AppStandbyInfo;
import android.app.usage.UsageStatsManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.ModuleInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManagerInternal;
import android.database.ContentObserver;
import android.graphics.drawable.Icon;
import android.hardware.audio.common.V2_0.AudioFormat;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerExecutor;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.PowerExemptionManager;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.DeviceConfig;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.AtomicFile;
import android.util.Pair;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseArrayMap;
import android.util.TimeUtils;
import android.util.TypedXmlPullParser;
import android.util.TypedXmlSerializer;
import android.util.Xml;
import android.util.proto.ProtoOutputStream;
import com.android.internal.notification.SystemNotificationChannels;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.FrameworkStatsLog;
import com.android.internal.util.function.TriConsumer;
import com.android.server.AppStateTracker;
import com.android.server.LocalServices;
import com.android.server.SystemConfig;
import com.android.server.am.AppBatteryTracker;
import com.android.server.am.AppRestrictionController;
import com.android.server.apphibernation.AppHibernationManagerInternal;
import com.android.server.job.JobPackageTracker;
import com.android.server.pm.PackageManagerService;
import com.android.server.pm.UserManagerInternal;
import com.android.server.usage.AppStandbyInternal;
import com.android.server.usage.UnixCalendar;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.ToIntFunction;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes.dex */
public final class AppRestrictionController {
    private static final String APP_RESTRICTION_SETTINGS_DIRNAME = "apprestriction";
    private static final String APP_RESTRICTION_SETTINGS_FILENAME = "settings.xml";
    private static final String ATTR_CUR_LEVEL = "curlevel";
    private static final String ATTR_LEVEL_TS = "levelts";
    private static final String ATTR_PACKAGE = "package";
    private static final String ATTR_REASON = "reason";
    private static final String ATTR_UID = "uid";
    static final boolean DEBUG_BG_RESTRICTION_CONTROLLER = false;
    static final String DEVICE_CONFIG_SUBNAMESPACE_PREFIX = "bg_";
    private static final boolean ENABLE_SHOW_FGS_MANAGER_ACTION_ON_BG_RESTRICTION = false;
    private static final boolean ENABLE_SHOW_FOREGROUND_SERVICE_MANAGER = true;
    private static final String[] ROLES_IN_INTEREST = {"android.app.role.DIALER", "android.app.role.EMERGENCY"};
    static final int STOCK_PM_FLAGS = 819200;
    static final String TAG = "ActivityManager";
    private static final String TAG_SETTINGS = "settings";
    static final int TRACKER_TYPE_BATTERY = 1;
    static final int TRACKER_TYPE_BATTERY_EXEMPTION = 2;
    static final int TRACKER_TYPE_BIND_SERVICE_EVENTS = 7;
    static final int TRACKER_TYPE_BROADCAST_EVENTS = 6;
    static final int TRACKER_TYPE_FGS = 3;
    static final int TRACKER_TYPE_MEDIA_SESSION = 4;
    static final int TRACKER_TYPE_PERMISSION = 5;
    static final int TRACKER_TYPE_UNKNOWN = 0;
    private final SparseArrayMap<String, Runnable> mActiveUids;
    final ActivityManagerService mActivityManagerService;
    private final AppStandbyInternal.AppIdleStateChangeListener mAppIdleStateChangeListener;
    private final ArrayList<BaseAppStateTracker> mAppStateTrackers;
    private final AppStateTracker.BackgroundRestrictedAppListener mBackgroundRestrictionListener;
    private final HandlerExecutor mBgExecutor;
    private final BgHandler mBgHandler;
    private final HandlerThread mBgHandlerThread;
    ArraySet<String> mBgRestrictionExemptioFromSysConfig;
    private final BroadcastReceiver mBootReceiver;
    private final BroadcastReceiver mBroadcastReceiver;
    private List<String> mCarrierPrivilegedApps;
    private final Object mCarrierPrivilegedLock;
    private final ConstantsObserver mConstantsObserver;
    private final Context mContext;
    private int[] mDeviceIdleAllowlist;
    private int[] mDeviceIdleExceptIdleAllowlist;
    private final TrackerInfo mEmptyTrackerInfo;
    private final Injector mInjector;
    private final Object mLock;
    private final NotificationHelper mNotificationHelper;
    private final CopyOnWriteArraySet<ActivityManagerInternal.AppBackgroundRestrictionListener> mRestrictionListeners;
    final RestrictionSettings mRestrictionSettings;
    private final AtomicBoolean mRestrictionSettingsXmlLoaded;
    private final OnRoleHoldersChangedListener mRoleHolderChangedListener;
    private final Object mSettingsLock;
    private final ArraySet<Integer> mSystemDeviceIdleAllowlist;
    private final ArraySet<Integer> mSystemDeviceIdleExceptIdleAllowlist;
    private final HashMap<String, Boolean> mSystemModulesCache;
    private final ArrayList<Runnable> mTmpRunnables;
    private final IUidObserver mUidObserver;
    private final SparseArray<ArrayList<String>> mUidRolesMapping;

    /* loaded from: classes.dex */
    @interface TrackerType {
    }

    /* loaded from: classes.dex */
    interface UidBatteryUsageProvider {
        AppBatteryTracker.ImmutableBatteryUsage getUidBatteryUsage(int i);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public final class RestrictionSettings {
        final SparseArrayMap<String, PkgSettings> mRestrictionLevels = new SparseArrayMap<>();

        RestrictionSettings() {
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* loaded from: classes.dex */
        public final class PkgSettings {
            private long[] mLastNotificationShownTime;
            private long mLevelChangeTime;
            private int[] mNotificationId;
            private final String mPackageName;
            private int mReason;
            private final int mUid;
            private int mLastRestrictionLevel = 0;
            private int mCurrentRestrictionLevel = 0;

            PkgSettings(String packageName, int uid) {
                this.mPackageName = packageName;
                this.mUid = uid;
            }

            int update(int level, int reason, int subReason) {
                int i = this.mCurrentRestrictionLevel;
                if (level != i) {
                    this.mLastRestrictionLevel = i;
                    this.mCurrentRestrictionLevel = level;
                    this.mLevelChangeTime = AppRestrictionController.this.mInjector.currentTimeMillis();
                    this.mReason = (65280 & reason) | (subReason & 255);
                    AppRestrictionController.this.mBgHandler.obtainMessage(1, this.mUid, level, this.mPackageName).sendToTarget();
                }
                return this.mLastRestrictionLevel;
            }

            public String toString() {
                StringBuilder sb = new StringBuilder(128);
                sb.append("RestrictionLevel{");
                sb.append(Integer.toHexString(System.identityHashCode(this)));
                sb.append(':');
                sb.append(this.mPackageName);
                sb.append('/');
                sb.append(UserHandle.formatUid(this.mUid));
                sb.append('}');
                sb.append(' ');
                sb.append(ActivityManager.restrictionLevelToName(this.mCurrentRestrictionLevel));
                sb.append('(');
                sb.append(UsageStatsManager.reasonToString(this.mReason));
                sb.append(')');
                return sb.toString();
            }

            void dump(PrintWriter pw, long now) {
                synchronized (AppRestrictionController.this.mSettingsLock) {
                    pw.print(toString());
                    if (this.mLastRestrictionLevel != 0) {
                        pw.print('/');
                        pw.print(ActivityManager.restrictionLevelToName(this.mLastRestrictionLevel));
                    }
                    pw.print(" levelChange=");
                    TimeUtils.formatDuration(this.mLevelChangeTime - now, pw);
                    if (this.mLastNotificationShownTime != null) {
                        int i = 0;
                        while (true) {
                            long[] jArr = this.mLastNotificationShownTime;
                            if (i >= jArr.length) {
                                break;
                            }
                            if (jArr[i] > 0) {
                                pw.print(" lastNoti(");
                                NotificationHelper unused = AppRestrictionController.this.mNotificationHelper;
                                pw.print(NotificationHelper.notificationTypeToString(i));
                                pw.print(")=");
                                TimeUtils.formatDuration(this.mLastNotificationShownTime[i] - now, pw);
                            }
                            i++;
                        }
                    }
                }
                pw.print(" effectiveExemption=");
                pw.print(PowerExemptionManager.reasonCodeToString(AppRestrictionController.this.getBackgroundRestrictionExemptionReason(this.mUid)));
            }

            String getPackageName() {
                return this.mPackageName;
            }

            /* JADX INFO: Access modifiers changed from: package-private */
            public int getUid() {
                return this.mUid;
            }

            int getCurrentRestrictionLevel() {
                return this.mCurrentRestrictionLevel;
            }

            int getLastRestrictionLevel() {
                return this.mLastRestrictionLevel;
            }

            int getReason() {
                return this.mReason;
            }

            long getLastNotificationTime(int notificationType) {
                long[] jArr = this.mLastNotificationShownTime;
                if (jArr == null) {
                    return 0L;
                }
                return jArr[notificationType];
            }

            void setLastNotificationTime(int notificationType, long timestamp) {
                setLastNotificationTime(notificationType, timestamp, true);
            }

            void setLastNotificationTime(int notificationType, long timestamp, boolean persist) {
                if (this.mLastNotificationShownTime == null) {
                    this.mLastNotificationShownTime = new long[2];
                }
                this.mLastNotificationShownTime[notificationType] = timestamp;
                if (persist && AppRestrictionController.this.mRestrictionSettingsXmlLoaded.get()) {
                    RestrictionSettings.this.schedulePersistToXml(UserHandle.getUserId(this.mUid));
                }
            }

            int getNotificationId(int notificationType) {
                int[] iArr = this.mNotificationId;
                if (iArr == null) {
                    return 0;
                }
                return iArr[notificationType];
            }

            void setNotificationId(int notificationType, int notificationId) {
                if (this.mNotificationId == null) {
                    this.mNotificationId = new int[2];
                }
                this.mNotificationId[notificationType] = notificationId;
            }

            void setLevelChangeTime(long timestamp) {
                this.mLevelChangeTime = timestamp;
            }

            public Object clone() {
                PkgSettings newObj = new PkgSettings(this.mPackageName, this.mUid);
                newObj.mCurrentRestrictionLevel = this.mCurrentRestrictionLevel;
                newObj.mLastRestrictionLevel = this.mLastRestrictionLevel;
                newObj.mLevelChangeTime = this.mLevelChangeTime;
                newObj.mReason = this.mReason;
                long[] jArr = this.mLastNotificationShownTime;
                if (jArr != null) {
                    newObj.mLastNotificationShownTime = Arrays.copyOf(jArr, jArr.length);
                }
                int[] iArr = this.mNotificationId;
                if (iArr != null) {
                    newObj.mNotificationId = Arrays.copyOf(iArr, iArr.length);
                }
                return newObj;
            }

            public boolean equals(Object other) {
                if (other == this) {
                    return true;
                }
                if (other == null || !(other instanceof PkgSettings)) {
                    return false;
                }
                PkgSettings otherSettings = (PkgSettings) other;
                if (otherSettings.mUid == this.mUid && otherSettings.mCurrentRestrictionLevel == this.mCurrentRestrictionLevel && otherSettings.mLastRestrictionLevel == this.mLastRestrictionLevel && otherSettings.mLevelChangeTime == this.mLevelChangeTime && otherSettings.mReason == this.mReason && TextUtils.equals(otherSettings.mPackageName, this.mPackageName) && Arrays.equals(otherSettings.mLastNotificationShownTime, this.mLastNotificationShownTime) && Arrays.equals(otherSettings.mNotificationId, this.mNotificationId)) {
                    return true;
                }
                return false;
            }
        }

        int update(String packageName, int uid, int level, int reason, int subReason) {
            int update;
            synchronized (AppRestrictionController.this.mSettingsLock) {
                PkgSettings settings = getRestrictionSettingsLocked(uid, packageName);
                if (settings == null) {
                    settings = new PkgSettings(packageName, uid);
                    this.mRestrictionLevels.add(uid, packageName, settings);
                }
                update = settings.update(level, reason, subReason);
            }
            return update;
        }

        int getReason(String packageName, int uid) {
            int reason;
            synchronized (AppRestrictionController.this.mSettingsLock) {
                PkgSettings settings = (PkgSettings) this.mRestrictionLevels.get(uid, packageName);
                reason = settings != null ? settings.getReason() : 256;
            }
            return reason;
        }

        int getRestrictionLevel(int uid) {
            synchronized (AppRestrictionController.this.mSettingsLock) {
                int uidKeyIndex = this.mRestrictionLevels.indexOfKey(uid);
                if (uidKeyIndex < 0) {
                    return 0;
                }
                int numPackages = this.mRestrictionLevels.numElementsForKeyAt(uidKeyIndex);
                if (numPackages == 0) {
                    return 0;
                }
                int level = 0;
                for (int i = 0; i < numPackages; i++) {
                    PkgSettings setting = (PkgSettings) this.mRestrictionLevels.valueAt(uidKeyIndex, i);
                    if (setting != null) {
                        int l = setting.getCurrentRestrictionLevel();
                        level = level == 0 ? l : Math.min(level, l);
                    }
                }
                return level;
            }
        }

        int getRestrictionLevel(int uid, String packageName) {
            int restrictionLevel;
            synchronized (AppRestrictionController.this.mSettingsLock) {
                PkgSettings settings = getRestrictionSettingsLocked(uid, packageName);
                restrictionLevel = settings == null ? getRestrictionLevel(uid) : settings.getCurrentRestrictionLevel();
            }
            return restrictionLevel;
        }

        int getRestrictionLevel(String packageName, int userId) {
            PackageManagerInternal pm = AppRestrictionController.this.mInjector.getPackageManagerInternal();
            int uid = pm.getPackageUid(packageName, 819200L, userId);
            return getRestrictionLevel(uid, packageName);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public int getLastRestrictionLevel(int uid, String packageName) {
            int lastRestrictionLevel;
            synchronized (AppRestrictionController.this.mSettingsLock) {
                PkgSettings settings = (PkgSettings) this.mRestrictionLevels.get(uid, packageName);
                lastRestrictionLevel = settings == null ? 0 : settings.getLastRestrictionLevel();
            }
            return lastRestrictionLevel;
        }

        void forEachPackageInUidLocked(int uid, TriConsumer<String, Integer, Integer> consumer) {
            int uidKeyIndex = this.mRestrictionLevels.indexOfKey(uid);
            if (uidKeyIndex < 0) {
                return;
            }
            int numPackages = this.mRestrictionLevels.numElementsForKeyAt(uidKeyIndex);
            for (int i = 0; i < numPackages; i++) {
                PkgSettings settings = (PkgSettings) this.mRestrictionLevels.valueAt(uidKeyIndex, i);
                consumer.accept((String) this.mRestrictionLevels.keyAt(uidKeyIndex, i), Integer.valueOf(settings.getCurrentRestrictionLevel()), Integer.valueOf(settings.getReason()));
            }
        }

        void forEachUidLocked(Consumer<Integer> consumer) {
            for (int i = this.mRestrictionLevels.numMaps() - 1; i >= 0; i--) {
                consumer.accept(Integer.valueOf(this.mRestrictionLevels.keyAt(i)));
            }
        }

        PkgSettings getRestrictionSettingsLocked(int uid, String packageName) {
            return (PkgSettings) this.mRestrictionLevels.get(uid, packageName);
        }

        void removeUser(int userId) {
            synchronized (AppRestrictionController.this.mSettingsLock) {
                for (int i = this.mRestrictionLevels.numMaps() - 1; i >= 0; i--) {
                    int uid = this.mRestrictionLevels.keyAt(i);
                    if (UserHandle.getUserId(uid) == userId) {
                        this.mRestrictionLevels.deleteAt(i);
                    }
                }
            }
        }

        void removePackage(String pkgName, int uid) {
            removePackage(pkgName, uid, true);
        }

        void removePackage(String pkgName, int uid, boolean persist) {
            synchronized (AppRestrictionController.this.mSettingsLock) {
                int keyIndex = this.mRestrictionLevels.indexOfKey(uid);
                this.mRestrictionLevels.delete(uid, pkgName);
                if (keyIndex >= 0 && this.mRestrictionLevels.numElementsForKeyAt(keyIndex) == 0) {
                    this.mRestrictionLevels.deleteAt(keyIndex);
                }
            }
            if (persist && AppRestrictionController.this.mRestrictionSettingsXmlLoaded.get()) {
                schedulePersistToXml(UserHandle.getUserId(uid));
            }
        }

        void removeUid(int uid) {
            removeUid(uid, true);
        }

        void removeUid(int uid, boolean persist) {
            synchronized (AppRestrictionController.this.mSettingsLock) {
                this.mRestrictionLevels.delete(uid);
            }
            if (persist && AppRestrictionController.this.mRestrictionSettingsXmlLoaded.get()) {
                schedulePersistToXml(UserHandle.getUserId(uid));
            }
        }

        void reset() {
            synchronized (AppRestrictionController.this.mSettingsLock) {
                for (int i = this.mRestrictionLevels.numMaps() - 1; i >= 0; i--) {
                    this.mRestrictionLevels.deleteAt(i);
                }
            }
        }

        void resetToDefault() {
            synchronized (AppRestrictionController.this.mSettingsLock) {
                this.mRestrictionLevels.forEach(new Consumer() { // from class: com.android.server.am.AppRestrictionController$RestrictionSettings$$ExternalSyntheticLambda0
                    @Override // java.util.function.Consumer
                    public final void accept(Object obj) {
                        AppRestrictionController.RestrictionSettings.lambda$resetToDefault$0((AppRestrictionController.RestrictionSettings.PkgSettings) obj);
                    }
                });
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public static /* synthetic */ void lambda$resetToDefault$0(PkgSettings settings) {
            settings.mCurrentRestrictionLevel = 0;
            settings.mLastRestrictionLevel = 0;
            settings.mLevelChangeTime = 0L;
            settings.mReason = 256;
            if (settings.mLastNotificationShownTime != null) {
                for (int i = 0; i < settings.mLastNotificationShownTime.length; i++) {
                    settings.mLastNotificationShownTime[i] = 0;
                }
            }
        }

        void dump(PrintWriter pw, String prefix) {
            final ArrayList<PkgSettings> settings = new ArrayList<>();
            synchronized (AppRestrictionController.this.mSettingsLock) {
                this.mRestrictionLevels.forEach(new Consumer() { // from class: com.android.server.am.AppRestrictionController$RestrictionSettings$$ExternalSyntheticLambda1
                    @Override // java.util.function.Consumer
                    public final void accept(Object obj) {
                        settings.add((AppRestrictionController.RestrictionSettings.PkgSettings) obj);
                    }
                });
            }
            Collections.sort(settings, Comparator.comparingInt(new ToIntFunction() { // from class: com.android.server.am.AppRestrictionController$RestrictionSettings$$ExternalSyntheticLambda2
                @Override // java.util.function.ToIntFunction
                public final int applyAsInt(Object obj) {
                    return ((AppRestrictionController.RestrictionSettings.PkgSettings) obj).getUid();
                }
            }));
            long now = AppRestrictionController.this.mInjector.currentTimeMillis();
            int size = settings.size();
            for (int i = 0; i < size; i++) {
                pw.print(prefix);
                pw.print('#');
                pw.print(i);
                pw.print(' ');
                settings.get(i).dump(pw, now);
                pw.println();
            }
        }

        void schedulePersistToXml(int userId) {
            AppRestrictionController.this.mBgHandler.obtainMessage(11, userId, 0).sendToTarget();
        }

        void scheduleLoadFromXml() {
            AppRestrictionController.this.mBgHandler.sendEmptyMessage(10);
        }

        File getXmlFileNameForUser(int userId) {
            File dir = new File(AppRestrictionController.this.mInjector.getDataSystemDeDirectory(userId), AppRestrictionController.APP_RESTRICTION_SETTINGS_DIRNAME);
            return new File(dir, AppRestrictionController.APP_RESTRICTION_SETTINGS_FILENAME);
        }

        void loadFromXml(boolean applyLevel) {
            int[] allUsers = AppRestrictionController.this.mInjector.getUserManagerInternal().getUserIds();
            for (int userId : allUsers) {
                loadFromXml(userId, applyLevel);
            }
            AppRestrictionController.this.mRestrictionSettingsXmlLoaded.set(true);
        }

        void loadFromXml(int userId, boolean applyLevel) {
            File file = getXmlFileNameForUser(userId);
            if (!file.exists()) {
                return;
            }
            long[] ts = new long[2];
            try {
                InputStream in = new FileInputStream(file);
                TypedXmlPullParser parser = Xml.resolvePullParser(in);
                long now = SystemClock.elapsedRealtime();
                while (true) {
                    int type = parser.next();
                    if (type != 1) {
                        if (type == 2) {
                            String tagName = parser.getName();
                            if (!AppRestrictionController.TAG_SETTINGS.equals(tagName)) {
                                Slog.w(AppRestrictionController.TAG, "Unexpected tag name: " + tagName);
                            } else {
                                loadOneFromXml(parser, now, ts, applyLevel);
                            }
                        }
                    } else {
                        in.close();
                        return;
                    }
                }
            } catch (IOException | XmlPullParserException e) {
            }
        }

        /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
        private void loadOneFromXml(TypedXmlPullParser parser, long now, long[] ts, boolean applyLevel) {
            long j;
            PkgSettings pkgSettings;
            String attrName;
            String attrValue;
            int i = 0;
            while (true) {
                j = 0;
                if (i >= ts.length) {
                    break;
                }
                ts[i] = 0;
                i++;
            }
            String packageName = null;
            int curLevel = 0;
            int reason = 256;
            int curLevel2 = 0;
            int uid = 0;
            long levelTs = 0;
            while (true) {
                int uid2 = parser.getAttributeCount();
                char c = 0;
                if (curLevel2 < uid2) {
                    try {
                        attrName = parser.getAttributeName(curLevel2);
                        attrValue = parser.getAttributeValue(curLevel2);
                        switch (attrName.hashCode()) {
                            case -934964668:
                                if (attrName.equals("reason")) {
                                    c = 4;
                                    break;
                                }
                                c = 65535;
                                break;
                            case -807062458:
                                if (attrName.equals("package")) {
                                    c = 1;
                                    break;
                                }
                                c = 65535;
                                break;
                            case 115792:
                                if (attrName.equals("uid")) {
                                    break;
                                }
                                c = 65535;
                                break;
                            case 69785859:
                                if (attrName.equals(AppRestrictionController.ATTR_LEVEL_TS)) {
                                    c = 3;
                                    break;
                                }
                                c = 65535;
                                break;
                            case 569868612:
                                if (attrName.equals(AppRestrictionController.ATTR_CUR_LEVEL)) {
                                    c = 2;
                                    break;
                                }
                                c = 65535;
                                break;
                            default:
                                c = 65535;
                                break;
                        }
                    } catch (IllegalArgumentException e) {
                    }
                    switch (c) {
                        case 0:
                            uid = Integer.parseInt(attrValue);
                            continue;
                            curLevel2++;
                        case 1:
                            packageName = attrValue;
                            continue;
                            curLevel2++;
                        case 2:
                            curLevel = Integer.parseInt(attrValue);
                            continue;
                            curLevel2++;
                        case 3:
                            levelTs = Long.parseLong(attrValue);
                            continue;
                            curLevel2++;
                        case 4:
                            reason = Integer.parseInt(attrValue);
                            continue;
                            curLevel2++;
                        default:
                            int type = NotificationHelper.notificationTimeAttrToType(attrName);
                            ts[type] = Long.parseLong(attrValue);
                            continue;
                            curLevel2++;
                    }
                    curLevel2++;
                } else if (uid != 0) {
                    synchronized (AppRestrictionController.this.mSettingsLock) {
                        try {
                            pkgSettings = getRestrictionSettingsLocked(uid, packageName);
                        } catch (Throwable th) {
                            th = th;
                        }
                        try {
                            if (pkgSettings == null) {
                                return;
                            }
                            int i2 = 0;
                            while (i2 < ts.length) {
                                if (pkgSettings.getLastNotificationTime(i2) == j && ts[i2] != j) {
                                    pkgSettings.setLastNotificationTime(i2, ts[i2], false);
                                }
                                i2++;
                                j = 0;
                            }
                            int i3 = pkgSettings.mCurrentRestrictionLevel;
                            if (i3 >= curLevel) {
                                return;
                            }
                            long levelTs2 = levelTs;
                            int curBucket = AppRestrictionController.this.mInjector.getAppStandbyInternal().getAppStandbyBucket(packageName, UserHandle.getUserId(uid), now, false);
                            if (applyLevel) {
                                AppRestrictionController appRestrictionController = AppRestrictionController.this;
                                int curLevel3 = curLevel;
                                int curLevel4 = uid;
                                appRestrictionController.applyRestrictionLevel(packageName, curLevel4, curLevel3, appRestrictionController.mEmptyTrackerInfo, curBucket, true, reason & JobPackageTracker.EVENT_STOP_REASON_MASK, reason & 255);
                            } else {
                                int reason2 = reason;
                                pkgSettings.update(curLevel, 65280 & reason2, reason2 & 255);
                            }
                            synchronized (AppRestrictionController.this.mSettingsLock) {
                                pkgSettings.setLevelChangeTime(levelTs2);
                            }
                            return;
                        } catch (Throwable th2) {
                            th = th2;
                            while (true) {
                                try {
                                    throw th;
                                } catch (Throwable th3) {
                                    th = th3;
                                }
                            }
                        }
                    }
                } else {
                    return;
                }
            }
        }

        void persistToXml(int userId) {
            File file = getXmlFileNameForUser(userId);
            File dir = file.getParentFile();
            if (!dir.isDirectory() && !dir.mkdirs()) {
                Slog.w(AppRestrictionController.TAG, "Failed to create folder for " + userId);
                return;
            }
            AtomicFile atomicFile = new AtomicFile(file);
            FileOutputStream stream = null;
            try {
                stream = atomicFile.startWrite();
                stream.write(toXmlByteArray(userId));
                atomicFile.finishWrite(stream);
            } catch (Exception e) {
                Slog.e(AppRestrictionController.TAG, "Failed to write file " + file, e);
                if (stream != null) {
                    atomicFile.failWrite(stream);
                }
            }
        }

        private byte[] toXmlByteArray(int userId) {
            try {
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                TypedXmlSerializer serializer = Xml.resolveSerializer(os);
                serializer.startDocument((String) null, true);
                synchronized (AppRestrictionController.this.mSettingsLock) {
                    for (int i = this.mRestrictionLevels.numMaps() - 1; i >= 0; i--) {
                        for (int j = this.mRestrictionLevels.numElementsForKeyAt(i) - 1; j >= 0; j--) {
                            PkgSettings settings = (PkgSettings) this.mRestrictionLevels.valueAt(i, j);
                            int uid = settings.getUid();
                            if (UserHandle.getUserId(uid) == userId) {
                                serializer.startTag((String) null, AppRestrictionController.TAG_SETTINGS);
                                serializer.attributeInt((String) null, "uid", uid);
                                serializer.attribute((String) null, "package", settings.getPackageName());
                                serializer.attributeInt((String) null, AppRestrictionController.ATTR_CUR_LEVEL, settings.mCurrentRestrictionLevel);
                                serializer.attributeLong((String) null, AppRestrictionController.ATTR_LEVEL_TS, settings.mLevelChangeTime);
                                serializer.attributeInt((String) null, "reason", settings.mReason);
                                for (int k = 0; k < 2; k++) {
                                    serializer.attributeLong((String) null, NotificationHelper.notificationTypeToTimeAttr(k), settings.getLastNotificationTime(k));
                                }
                                serializer.endTag((String) null, AppRestrictionController.TAG_SETTINGS);
                            }
                        }
                    }
                }
                serializer.endDocument();
                serializer.flush();
                byte[] byteArray = os.toByteArray();
                os.close();
                return byteArray;
            } catch (IOException e) {
                return null;
            }
        }

        void removeXml() {
            int[] allUsers = AppRestrictionController.this.mInjector.getUserManagerInternal().getUserIds();
            for (int userId : allUsers) {
                getXmlFileNameForUser(userId).delete();
            }
        }

        public Object clone() {
            RestrictionSettings newObj = new RestrictionSettings();
            synchronized (AppRestrictionController.this.mSettingsLock) {
                for (int i = this.mRestrictionLevels.numMaps() - 1; i >= 0; i--) {
                    for (int j = this.mRestrictionLevels.numElementsForKeyAt(i) - 1; j >= 0; j--) {
                        PkgSettings settings = (PkgSettings) this.mRestrictionLevels.valueAt(i, j);
                        newObj.mRestrictionLevels.add(this.mRestrictionLevels.keyAt(i), (String) this.mRestrictionLevels.keyAt(i, j), (PkgSettings) settings.clone());
                    }
                }
            }
            return newObj;
        }

        public boolean equals(Object other) {
            if (other == this) {
                return true;
            }
            if (other == null || !(other instanceof RestrictionSettings)) {
                return false;
            }
            SparseArrayMap<String, PkgSettings> otherSettings = ((RestrictionSettings) other).mRestrictionLevels;
            synchronized (AppRestrictionController.this.mSettingsLock) {
                if (otherSettings.numMaps() == this.mRestrictionLevels.numMaps()) {
                    for (int i = this.mRestrictionLevels.numMaps() - 1; i >= 0; i--) {
                        int uid = this.mRestrictionLevels.keyAt(i);
                        if (otherSettings.numElementsForKey(uid) == this.mRestrictionLevels.numElementsForKeyAt(i)) {
                            for (int j = this.mRestrictionLevels.numElementsForKeyAt(i) - 1; j >= 0; j--) {
                                PkgSettings settings = (PkgSettings) this.mRestrictionLevels.valueAt(i, j);
                                if (!settings.equals(otherSettings.get(uid, settings.getPackageName()))) {
                                    return false;
                                }
                            }
                        } else {
                            return false;
                        }
                    }
                    return true;
                }
                return false;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public final class ConstantsObserver extends ContentObserver implements DeviceConfig.OnPropertiesChangedListener {
        static final long DEFAULT_BG_ABUSIVE_NOTIFICATION_MINIMAL_INTERVAL_MS = 2592000000L;
        static final boolean DEFAULT_BG_AUTO_RESTRICTED_BUCKET_ON_BG_RESTRICTION = false;
        static final boolean DEFAULT_BG_AUTO_RESTRICT_ABUSIVE_APPS = true;
        static final long DEFAULT_BG_LONG_FGS_NOTIFICATION_MINIMAL_INTERVAL_MS = 2592000000L;
        static final boolean DEFAULT_BG_PROMPT_FGS_ON_LONG_RUNNING = true;
        static final boolean DEFAULT_BG_PROMPT_FGS_WITH_NOTIFICATION_ON_LONG_RUNNING = false;
        static final String KEY_BG_ABUSIVE_NOTIFICATION_MINIMAL_INTERVAL = "bg_abusive_notification_minimal_interval";
        static final String KEY_BG_AUTO_RESTRICTED_BUCKET_ON_BG_RESTRICTION = "bg_auto_restricted_bucket_on_bg_restricted";
        static final String KEY_BG_AUTO_RESTRICT_ABUSIVE_APPS = "bg_auto_restrict_abusive_apps";
        static final String KEY_BG_LONG_FGS_NOTIFICATION_MINIMAL_INTERVAL = "bg_long_fgs_notification_minimal_interval";
        static final String KEY_BG_PROMPT_ABUSIVE_APPS_TO_BG_RESTRICTED = "bg_prompt_abusive_apps_to_bg_restricted";
        static final String KEY_BG_PROMPT_FGS_ON_LONG_RUNNING = "bg_prompt_fgs_on_long_running";
        static final String KEY_BG_PROMPT_FGS_WITH_NOTIFICATION_ON_LONG_RUNNING = "bg_prompt_fgs_with_noti_on_long_running";
        static final String KEY_BG_PROMPT_FGS_WITH_NOTIFICATION_TO_BG_RESTRICTED = "bg_prompt_fgs_with_noti_to_bg_restricted";
        static final String KEY_BG_RESTRICTION_EXEMPTED_PACKAGES = "bg_restriction_exempted_packages";
        volatile long mBgAbusiveNotificationMinIntervalMs;
        volatile boolean mBgAutoRestrictAbusiveApps;
        volatile boolean mBgAutoRestrictedBucket;
        volatile long mBgLongFgsNotificationMinIntervalMs;
        volatile boolean mBgPromptAbusiveAppsToBgRestricted;
        volatile boolean mBgPromptFgsOnLongRunning;
        volatile boolean mBgPromptFgsWithNotiOnLongRunning;
        volatile boolean mBgPromptFgsWithNotiToBgRestricted;
        volatile Set<String> mBgRestrictionExemptedPackages;
        final boolean mDefaultBgPromptAbusiveAppToBgRestricted;
        final boolean mDefaultBgPromptFgsWithNotiToBgRestricted;
        volatile boolean mRestrictedBucketEnabled;

        ConstantsObserver(Handler handler, Context context) {
            super(handler);
            this.mBgRestrictionExemptedPackages = Collections.emptySet();
            this.mDefaultBgPromptFgsWithNotiToBgRestricted = context.getResources().getBoolean(17891391);
            this.mDefaultBgPromptAbusiveAppToBgRestricted = context.getResources().getBoolean(17891390);
        }

        public void onPropertiesChanged(DeviceConfig.Properties properties) {
            String name;
            Iterator it = properties.getKeyset().iterator();
            while (it.hasNext() && (name = (String) it.next()) != null && name.startsWith(AppRestrictionController.DEVICE_CONFIG_SUBNAMESPACE_PREFIX)) {
                char c = 65535;
                switch (name.hashCode()) {
                    case -1918659497:
                        if (name.equals(KEY_BG_PROMPT_ABUSIVE_APPS_TO_BG_RESTRICTED)) {
                            c = 7;
                            break;
                        }
                        break;
                    case -1199889595:
                        if (name.equals(KEY_BG_AUTO_RESTRICT_ABUSIVE_APPS)) {
                            c = 1;
                            break;
                        }
                        break;
                    case -582264882:
                        if (name.equals(KEY_BG_PROMPT_FGS_ON_LONG_RUNNING)) {
                            c = 6;
                            break;
                        }
                        break;
                    case -395763044:
                        if (name.equals(KEY_BG_AUTO_RESTRICTED_BUCKET_ON_BG_RESTRICTION)) {
                            c = 0;
                            break;
                        }
                        break;
                    case -157665503:
                        if (name.equals(KEY_BG_RESTRICTION_EXEMPTED_PACKAGES)) {
                            c = '\b';
                            break;
                        }
                        break;
                    case 854605367:
                        if (name.equals(KEY_BG_ABUSIVE_NOTIFICATION_MINIMAL_INTERVAL)) {
                            c = 2;
                            break;
                        }
                        break;
                    case 892275457:
                        if (name.equals(KEY_BG_LONG_FGS_NOTIFICATION_MINIMAL_INTERVAL)) {
                            c = 3;
                            break;
                        }
                        break;
                    case 1771474142:
                        if (name.equals(KEY_BG_PROMPT_FGS_WITH_NOTIFICATION_ON_LONG_RUNNING)) {
                            c = 5;
                            break;
                        }
                        break;
                    case 1965398671:
                        if (name.equals(KEY_BG_PROMPT_FGS_WITH_NOTIFICATION_TO_BG_RESTRICTED)) {
                            c = 4;
                            break;
                        }
                        break;
                }
                switch (c) {
                    case 0:
                        updateBgAutoRestrictedBucketChanged();
                        break;
                    case 1:
                        updateBgAutoRestrictAbusiveApps();
                        break;
                    case 2:
                        updateBgAbusiveNotificationMinimalInterval();
                        break;
                    case 3:
                        updateBgLongFgsNotificationMinimalInterval();
                        break;
                    case 4:
                        updateBgPromptFgsWithNotiToBgRestricted();
                        break;
                    case 5:
                        updateBgPromptFgsWithNotiOnLongRunning();
                        break;
                    case 6:
                        updateBgPromptFgsOnLongRunning();
                        break;
                    case 7:
                        updateBgPromptAbusiveAppToBgRestricted();
                        break;
                    case '\b':
                        updateBgRestrictionExemptedPackages();
                        break;
                }
                AppRestrictionController.this.onPropertiesChanged(name);
            }
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange) {
            updateSettings();
        }

        public void start() {
            ContentResolver cr = AppRestrictionController.this.mContext.getContentResolver();
            cr.registerContentObserver(Settings.Global.getUriFor("enable_restricted_bucket"), false, this);
            updateSettings();
            updateDeviceConfig();
        }

        void updateSettings() {
            this.mRestrictedBucketEnabled = isRestrictedBucketEnabled();
        }

        private boolean isRestrictedBucketEnabled() {
            return Settings.Global.getInt(AppRestrictionController.this.mContext.getContentResolver(), "enable_restricted_bucket", 1) == 1;
        }

        void updateDeviceConfig() {
            updateBgAutoRestrictedBucketChanged();
            updateBgAutoRestrictAbusiveApps();
            updateBgAbusiveNotificationMinimalInterval();
            updateBgLongFgsNotificationMinimalInterval();
            updateBgPromptFgsWithNotiToBgRestricted();
            updateBgPromptFgsWithNotiOnLongRunning();
            updateBgPromptFgsOnLongRunning();
            updateBgPromptAbusiveAppToBgRestricted();
            updateBgRestrictionExemptedPackages();
        }

        private void updateBgAutoRestrictedBucketChanged() {
            boolean oldValue = this.mBgAutoRestrictedBucket;
            this.mBgAutoRestrictedBucket = DeviceConfig.getBoolean("activity_manager", KEY_BG_AUTO_RESTRICTED_BUCKET_ON_BG_RESTRICTION, false);
            if (oldValue != this.mBgAutoRestrictedBucket) {
                AppRestrictionController.this.dispatchAutoRestrictedBucketFeatureFlagChanged(this.mBgAutoRestrictedBucket);
            }
        }

        private void updateBgAutoRestrictAbusiveApps() {
            this.mBgAutoRestrictAbusiveApps = DeviceConfig.getBoolean("activity_manager", KEY_BG_AUTO_RESTRICT_ABUSIVE_APPS, true);
        }

        private void updateBgAbusiveNotificationMinimalInterval() {
            this.mBgAbusiveNotificationMinIntervalMs = DeviceConfig.getLong("activity_manager", KEY_BG_ABUSIVE_NOTIFICATION_MINIMAL_INTERVAL, (long) UnixCalendar.MONTH_IN_MILLIS);
        }

        private void updateBgLongFgsNotificationMinimalInterval() {
            this.mBgLongFgsNotificationMinIntervalMs = DeviceConfig.getLong("activity_manager", KEY_BG_LONG_FGS_NOTIFICATION_MINIMAL_INTERVAL, (long) UnixCalendar.MONTH_IN_MILLIS);
        }

        private void updateBgPromptFgsWithNotiToBgRestricted() {
            this.mBgPromptFgsWithNotiToBgRestricted = DeviceConfig.getBoolean("activity_manager", KEY_BG_PROMPT_FGS_WITH_NOTIFICATION_TO_BG_RESTRICTED, this.mDefaultBgPromptFgsWithNotiToBgRestricted);
        }

        private void updateBgPromptFgsWithNotiOnLongRunning() {
            this.mBgPromptFgsWithNotiOnLongRunning = DeviceConfig.getBoolean("activity_manager", KEY_BG_PROMPT_FGS_WITH_NOTIFICATION_ON_LONG_RUNNING, false);
        }

        private void updateBgPromptFgsOnLongRunning() {
            this.mBgPromptFgsOnLongRunning = DeviceConfig.getBoolean("activity_manager", KEY_BG_PROMPT_FGS_ON_LONG_RUNNING, true);
        }

        private void updateBgPromptAbusiveAppToBgRestricted() {
            this.mBgPromptAbusiveAppsToBgRestricted = DeviceConfig.getBoolean("activity_manager", KEY_BG_PROMPT_ABUSIVE_APPS_TO_BG_RESTRICTED, this.mDefaultBgPromptAbusiveAppToBgRestricted);
        }

        private void updateBgRestrictionExemptedPackages() {
            String settings = DeviceConfig.getString("activity_manager", KEY_BG_RESTRICTION_EXEMPTED_PACKAGES, (String) null);
            if (settings == null) {
                this.mBgRestrictionExemptedPackages = Collections.emptySet();
                return;
            }
            String[] settingsList = settings.split(",");
            ArraySet<String> packages = new ArraySet<>();
            for (String pkg : settingsList) {
                packages.add(pkg);
            }
            this.mBgRestrictionExemptedPackages = Collections.unmodifiableSet(packages);
        }

        void dump(PrintWriter pw, String prefix) {
            pw.print(prefix);
            pw.println("BACKGROUND RESTRICTION POLICY SETTINGS:");
            String prefix2 = "  " + prefix;
            pw.print(prefix2);
            pw.print(KEY_BG_AUTO_RESTRICTED_BUCKET_ON_BG_RESTRICTION);
            pw.print('=');
            pw.println(this.mBgAutoRestrictedBucket);
            pw.print(prefix2);
            pw.print(KEY_BG_AUTO_RESTRICT_ABUSIVE_APPS);
            pw.print('=');
            pw.println(this.mBgAutoRestrictAbusiveApps);
            pw.print(prefix2);
            pw.print(KEY_BG_ABUSIVE_NOTIFICATION_MINIMAL_INTERVAL);
            pw.print('=');
            pw.println(this.mBgAbusiveNotificationMinIntervalMs);
            pw.print(prefix2);
            pw.print(KEY_BG_LONG_FGS_NOTIFICATION_MINIMAL_INTERVAL);
            pw.print('=');
            pw.println(this.mBgLongFgsNotificationMinIntervalMs);
            pw.print(prefix2);
            pw.print(KEY_BG_PROMPT_FGS_ON_LONG_RUNNING);
            pw.print('=');
            pw.println(this.mBgPromptFgsOnLongRunning);
            pw.print(prefix2);
            pw.print(KEY_BG_PROMPT_FGS_WITH_NOTIFICATION_ON_LONG_RUNNING);
            pw.print('=');
            pw.println(this.mBgPromptFgsWithNotiOnLongRunning);
            pw.print(prefix2);
            pw.print(KEY_BG_PROMPT_FGS_WITH_NOTIFICATION_TO_BG_RESTRICTED);
            pw.print('=');
            pw.println(this.mBgPromptFgsWithNotiToBgRestricted);
            pw.print(prefix2);
            pw.print(KEY_BG_PROMPT_ABUSIVE_APPS_TO_BG_RESTRICTED);
            pw.print('=');
            pw.println(this.mBgPromptAbusiveAppsToBgRestricted);
            pw.print(prefix2);
            pw.print(KEY_BG_RESTRICTION_EXEMPTED_PACKAGES);
            pw.print('=');
            pw.println(this.mBgRestrictionExemptedPackages.toString());
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class TrackerInfo {
        final byte[] mInfo;
        final int mType;

        TrackerInfo() {
            this.mType = 0;
            this.mInfo = null;
        }

        TrackerInfo(int type, byte[] info) {
            this.mType = type;
            this.mInfo = info;
        }
    }

    public void addAppBackgroundRestrictionListener(ActivityManagerInternal.AppBackgroundRestrictionListener listener) {
        this.mRestrictionListeners.add(listener);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public AppRestrictionController(Context context, ActivityManagerService service) {
        this(new Injector(context), service);
    }

    AppRestrictionController(Injector injector, ActivityManagerService service) {
        this.mAppStateTrackers = new ArrayList<>();
        this.mRestrictionSettings = new RestrictionSettings();
        this.mRestrictionListeners = new CopyOnWriteArraySet<>();
        this.mActiveUids = new SparseArrayMap<>();
        this.mTmpRunnables = new ArrayList<>();
        this.mDeviceIdleAllowlist = new int[0];
        this.mDeviceIdleExceptIdleAllowlist = new int[0];
        this.mSystemDeviceIdleAllowlist = new ArraySet<>();
        this.mSystemDeviceIdleExceptIdleAllowlist = new ArraySet<>();
        this.mLock = new Object();
        this.mSettingsLock = new Object();
        this.mRoleHolderChangedListener = new OnRoleHoldersChangedListener() { // from class: com.android.server.am.AppRestrictionController$$ExternalSyntheticLambda5
            public final void onRoleHoldersChanged(String str, UserHandle userHandle) {
                AppRestrictionController.this.onRoleHoldersChanged(str, userHandle);
            }
        };
        this.mUidRolesMapping = new SparseArray<>();
        this.mSystemModulesCache = new HashMap<>();
        this.mCarrierPrivilegedLock = new Object();
        this.mRestrictionSettingsXmlLoaded = new AtomicBoolean();
        this.mEmptyTrackerInfo = new TrackerInfo();
        this.mBroadcastReceiver = new BroadcastReceiver() { // from class: com.android.server.am.AppRestrictionController.1
            /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context, Intent intent) {
                char c;
                int uid;
                String ssp;
                int uid2;
                intent.getAction();
                String action = intent.getAction();
                switch (action.hashCode()) {
                    case -2061058799:
                        if (action.equals("android.intent.action.USER_REMOVED")) {
                            c = 7;
                            break;
                        }
                        c = 65535;
                        break;
                    case -1749672628:
                        if (action.equals("android.intent.action.UID_REMOVED")) {
                            c = 3;
                            break;
                        }
                        c = 65535;
                        break;
                    case -755112654:
                        if (action.equals("android.intent.action.USER_STARTED")) {
                            c = 5;
                            break;
                        }
                        c = 65535;
                        break;
                    case -742246786:
                        if (action.equals("android.intent.action.USER_STOPPED")) {
                            c = 6;
                            break;
                        }
                        c = 65535;
                        break;
                    case 172491798:
                        if (action.equals("android.intent.action.PACKAGE_CHANGED")) {
                            c = 1;
                            break;
                        }
                        c = 65535;
                        break;
                    case 1121780209:
                        if (action.equals("android.intent.action.USER_ADDED")) {
                            c = 4;
                            break;
                        }
                        c = 65535;
                        break;
                    case 1544582882:
                        if (action.equals("android.intent.action.PACKAGE_ADDED")) {
                            c = 0;
                            break;
                        }
                        c = 65535;
                        break;
                    case 1580442797:
                        if (action.equals("android.intent.action.PACKAGE_FULLY_REMOVED")) {
                            c = 2;
                            break;
                        }
                        c = 65535;
                        break;
                    default:
                        c = 65535;
                        break;
                }
                switch (c) {
                    case 0:
                        if (!intent.getBooleanExtra("android.intent.extra.REPLACING", false) && (uid = intent.getIntExtra("android.intent.extra.UID", -1)) >= 0) {
                            AppRestrictionController.this.onUidAdded(uid);
                            break;
                        }
                        break;
                    case 1:
                        break;
                    case 2:
                        int uid3 = intent.getIntExtra("android.intent.extra.UID", -1);
                        Uri data = intent.getData();
                        if (uid3 >= 0 && data != null && (ssp = data.getSchemeSpecificPart()) != null) {
                            AppRestrictionController.this.onPackageRemoved(ssp, uid3);
                            return;
                        }
                        return;
                    case 3:
                        if (!intent.getBooleanExtra("android.intent.extra.REPLACING", false) && (uid2 = intent.getIntExtra("android.intent.extra.UID", -1)) >= 0) {
                            AppRestrictionController.this.onUidRemoved(uid2);
                            return;
                        }
                        return;
                    case 4:
                        int userId = intent.getIntExtra("android.intent.extra.user_handle", -1);
                        if (userId >= 0) {
                            AppRestrictionController.this.onUserAdded(userId);
                            return;
                        }
                        return;
                    case 5:
                        int userId2 = intent.getIntExtra("android.intent.extra.user_handle", -1);
                        if (userId2 >= 0) {
                            AppRestrictionController.this.onUserStarted(userId2);
                            return;
                        }
                        return;
                    case 6:
                        int userId3 = intent.getIntExtra("android.intent.extra.user_handle", -1);
                        if (userId3 >= 0) {
                            AppRestrictionController.this.onUserStopped(userId3);
                            return;
                        }
                        return;
                    case 7:
                        int userId4 = intent.getIntExtra("android.intent.extra.user_handle", -1);
                        if (userId4 >= 0) {
                            AppRestrictionController.this.onUserRemoved(userId4);
                            return;
                        }
                        return;
                    default:
                        return;
                }
                String pkgName = intent.getData().getSchemeSpecificPart();
                String[] cmpList = intent.getStringArrayExtra("android.intent.extra.changed_component_name_list");
                if (cmpList == null || (cmpList.length == 1 && pkgName.equals(cmpList[0]))) {
                    AppRestrictionController.this.clearCarrierPrivilegedApps();
                }
            }
        };
        this.mBootReceiver = new BroadcastReceiver() { // from class: com.android.server.am.AppRestrictionController.2
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context, Intent intent) {
                char c;
                intent.getAction();
                String action = intent.getAction();
                switch (action.hashCode()) {
                    case -905063602:
                        if (action.equals("android.intent.action.LOCKED_BOOT_COMPLETED")) {
                            c = 0;
                            break;
                        }
                    default:
                        c = 65535;
                        break;
                }
                switch (c) {
                    case 0:
                        AppRestrictionController.this.onLockedBootCompleted();
                        return;
                    default:
                        return;
                }
            }
        };
        this.mBackgroundRestrictionListener = new AppStateTracker.BackgroundRestrictedAppListener() { // from class: com.android.server.am.AppRestrictionController.3
            public void updateBackgroundRestrictedForUidPackage(int uid, String packageName, boolean restricted) {
                AppRestrictionController.this.mBgHandler.obtainMessage(0, uid, restricted ? 1 : 0, packageName).sendToTarget();
            }
        };
        this.mAppIdleStateChangeListener = new AppStandbyInternal.AppIdleStateChangeListener() { // from class: com.android.server.am.AppRestrictionController.4
            public void onAppIdleStateChanged(String packageName, int userId, boolean idle, int bucket, int reason) {
                AppRestrictionController.this.mBgHandler.obtainMessage(2, userId, bucket, packageName).sendToTarget();
            }

            public void onUserInteractionStarted(String packageName, int userId) {
                AppRestrictionController.this.mBgHandler.obtainMessage(3, userId, 0, packageName).sendToTarget();
            }
        };
        this.mUidObserver = new IUidObserver.Stub() { // from class: com.android.server.am.AppRestrictionController.5
            public void onUidStateChanged(int uid, int procState, long procStateSeq, int capability) {
                AppRestrictionController.this.mBgHandler.obtainMessage(8, uid, procState).sendToTarget();
            }

            public void onUidIdle(int uid, boolean disabled) {
                AppRestrictionController.this.mBgHandler.obtainMessage(5, uid, disabled ? 1 : 0).sendToTarget();
            }

            public void onUidGone(int uid, boolean disabled) {
                AppRestrictionController.this.mBgHandler.obtainMessage(7, uid, disabled ? 1 : 0).sendToTarget();
            }

            public void onUidActive(int uid) {
                AppRestrictionController.this.mBgHandler.obtainMessage(6, uid, 0).sendToTarget();
            }

            public void onUidCachedChanged(int uid, boolean cached) {
            }

            public void onUidProcAdjChanged(int uid) {
            }
        };
        this.mInjector = injector;
        Context context = injector.getContext();
        this.mContext = context;
        this.mActivityManagerService = service;
        HandlerThread handlerThread = new HandlerThread("bgres-controller", 10);
        this.mBgHandlerThread = handlerThread;
        handlerThread.start();
        BgHandler bgHandler = new BgHandler(handlerThread.getLooper(), injector);
        this.mBgHandler = bgHandler;
        this.mBgExecutor = new HandlerExecutor(bgHandler);
        this.mConstantsObserver = new ConstantsObserver(bgHandler, context);
        this.mNotificationHelper = new NotificationHelper(this);
        injector.initAppStateTrackers(this);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onSystemReady() {
        DeviceConfig.addOnPropertiesChangedListener("activity_manager", this.mBgExecutor, this.mConstantsObserver);
        this.mConstantsObserver.start();
        initBgRestrictionExemptioFromSysConfig();
        initRestrictionStates();
        initSystemModuleNames();
        initRolesInInterest();
        registerForUidObservers();
        registerForSystemBroadcasts();
        this.mNotificationHelper.onSystemReady();
        this.mInjector.getAppStateTracker().addBackgroundRestrictedAppListener(this.mBackgroundRestrictionListener);
        this.mInjector.getAppStandbyInternal().addListener(this.mAppIdleStateChangeListener);
        this.mInjector.getRoleManager().addOnRoleHoldersChangedListenerAsUser(this.mBgExecutor, this.mRoleHolderChangedListener, UserHandle.ALL);
        this.mInjector.scheduleInitTrackers(this.mBgHandler, new Runnable() { // from class: com.android.server.am.AppRestrictionController$$ExternalSyntheticLambda3
            @Override // java.lang.Runnable
            public final void run() {
                AppRestrictionController.this.m1188xfb220ac8();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onSystemReady$0$com-android-server-am-AppRestrictionController  reason: not valid java name */
    public /* synthetic */ void m1188xfb220ac8() {
        int size = this.mAppStateTrackers.size();
        for (int i = 0; i < size; i++) {
            this.mAppStateTrackers.get(i).onSystemReady();
        }
    }

    void resetRestrictionSettings() {
        synchronized (this.mSettingsLock) {
            this.mRestrictionSettings.reset();
        }
        initRestrictionStates();
    }

    void tearDown() {
        DeviceConfig.removeOnPropertiesChangedListener(this.mConstantsObserver);
        unregisterForUidObservers();
        unregisterForSystemBroadcasts();
        this.mRestrictionSettings.removeXml();
    }

    private void initBgRestrictionExemptioFromSysConfig() {
        SystemConfig sysConfig = SystemConfig.getInstance();
        this.mBgRestrictionExemptioFromSysConfig = sysConfig.getBgRestrictionExemption();
        loadAppIdsFromPackageList(sysConfig.getAllowInPowerSaveExceptIdle(), this.mSystemDeviceIdleExceptIdleAllowlist);
        loadAppIdsFromPackageList(sysConfig.getAllowInPowerSave(), this.mSystemDeviceIdleAllowlist);
    }

    private void loadAppIdsFromPackageList(ArraySet<String> packages, ArraySet<Integer> apps) {
        PackageManager pm = this.mInjector.getPackageManager();
        for (int i = packages.size() - 1; i >= 0; i--) {
            String pkg = packages.valueAt(i);
            try {
                ApplicationInfo ai = pm.getApplicationInfo(pkg, 1048576);
                if (ai != null) {
                    apps.add(Integer.valueOf(UserHandle.getAppId(ai.uid)));
                }
            } catch (PackageManager.NameNotFoundException e) {
            }
        }
    }

    private boolean isExemptedFromSysConfig(String packageName) {
        ArraySet<String> arraySet = this.mBgRestrictionExemptioFromSysConfig;
        return arraySet != null && arraySet.contains(packageName);
    }

    private void initRestrictionStates() {
        int[] allUsers = this.mInjector.getUserManagerInternal().getUserIds();
        for (int userId : allUsers) {
            refreshAppRestrictionLevelForUser(userId, 1024, 2);
        }
        if (!this.mInjector.isTest()) {
            this.mRestrictionSettings.scheduleLoadFromXml();
            for (int userId2 : allUsers) {
                this.mRestrictionSettings.schedulePersistToXml(userId2);
            }
        }
    }

    private void initSystemModuleNames() {
        PackageManager pm = this.mInjector.getPackageManager();
        List<ModuleInfo> moduleInfos = pm.getInstalledModules(0);
        if (moduleInfos == null) {
            return;
        }
        synchronized (this.mLock) {
            for (ModuleInfo info : moduleInfos) {
                this.mSystemModulesCache.put(info.getPackageName(), Boolean.TRUE);
            }
        }
    }

    /* JADX WARN: Code restructure failed: missing block: B:21:0x0041, code lost:
        if (r4.applicationInfo.sourceDir.startsWith(android.os.Environment.getApexDirectory().getAbsolutePath()) != false) goto L34;
     */
    /* JADX WARN: Removed duplicated region for block: B:42:0x004c A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private boolean isSystemModule(String packageName) {
        synchronized (this.mLock) {
            Boolean val = this.mSystemModulesCache.get(packageName);
            if (val != null) {
                return val.booleanValue();
            }
            PackageManager pm = this.mInjector.getPackageManager();
            boolean isSystemModule = false;
            boolean isSystemModule2 = true;
            try {
                isSystemModule = pm.getModuleInfo(packageName, 0) != null;
            } catch (PackageManager.NameNotFoundException e) {
            }
            if (!isSystemModule) {
                try {
                    PackageInfo pkg = pm.getPackageInfo(packageName, 0);
                    if (pkg != null) {
                    }
                    isSystemModule2 = false;
                } catch (PackageManager.NameNotFoundException e2) {
                }
                synchronized (this.mLock) {
                    this.mSystemModulesCache.put(packageName, Boolean.valueOf(isSystemModule2));
                }
                return isSystemModule2;
            }
            isSystemModule2 = isSystemModule;
            synchronized (this.mLock) {
            }
        }
    }

    private void registerForUidObservers() {
        try {
            this.mInjector.getIActivityManager().registerUidObserver(this.mUidObserver, 15, 4, PackageManagerService.PLATFORM_PACKAGE_NAME);
        } catch (RemoteException e) {
        }
    }

    private void unregisterForUidObservers() {
        try {
            this.mInjector.getIActivityManager().unregisterUidObserver(this.mUidObserver);
        } catch (RemoteException e) {
        }
    }

    private void refreshAppRestrictionLevelForUser(int userId, int reason, int subReason) {
        List<AppStandbyInfo> appStandbyInfos = this.mInjector.getAppStandbyInternal().getAppStandbyBuckets(userId);
        if (ArrayUtils.isEmpty(appStandbyInfos)) {
            return;
        }
        PackageManagerInternal pm = this.mInjector.getPackageManagerInternal();
        for (AppStandbyInfo info : appStandbyInfos) {
            int uid = pm.getPackageUid(info.mPackageName, 819200L, userId);
            if (uid < 0) {
                Slog.e(TAG, "Unable to find " + info.mPackageName + "/u" + userId);
            } else {
                Pair<Integer, TrackerInfo> levelTypePair = calcAppRestrictionLevel(userId, uid, info.mPackageName, info.mStandbyBucket, false, false);
                applyRestrictionLevel(info.mPackageName, uid, ((Integer) levelTypePair.first).intValue(), (TrackerInfo) levelTypePair.second, info.mStandbyBucket, true, reason, subReason);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void refreshAppRestrictionLevelForUid(int uid, int reason, int subReason, boolean allowRequestBgRestricted) {
        String[] packages = this.mInjector.getPackageManager().getPackagesForUid(uid);
        if (ArrayUtils.isEmpty(packages)) {
            return;
        }
        AppStandbyInternal appStandbyInternal = this.mInjector.getAppStandbyInternal();
        int userId = UserHandle.getUserId(uid);
        long now = SystemClock.elapsedRealtime();
        int i = 0;
        for (int length = packages.length; i < length; length = length) {
            String pkg = packages[i];
            int curBucket = appStandbyInternal.getAppStandbyBucket(pkg, userId, now, false);
            Pair<Integer, TrackerInfo> levelTypePair = calcAppRestrictionLevel(userId, uid, pkg, curBucket, allowRequestBgRestricted, true);
            applyRestrictionLevel(pkg, uid, ((Integer) levelTypePair.first).intValue(), (TrackerInfo) levelTypePair.second, curBucket, true, reason, subReason);
            i++;
        }
    }

    private Pair<Integer, TrackerInfo> calcAppRestrictionLevel(int userId, int uid, String packageName, int standbyBucket, boolean allowRequestBgRestricted, boolean calcTrackers) {
        int level;
        if (this.mInjector.getAppHibernationInternal().isHibernatingForUser(packageName, userId)) {
            return new Pair<>(60, this.mEmptyTrackerInfo);
        }
        TrackerInfo trackerInfo = null;
        switch (standbyBucket) {
            case 5:
                level = 20;
                break;
            case 50:
                level = 50;
                break;
            default:
                if (this.mInjector.getAppStateTracker().isAppBackgroundRestricted(uid, packageName)) {
                    return new Pair<>(50, this.mEmptyTrackerInfo);
                }
                if (this.mConstantsObserver.mRestrictedBucketEnabled && standbyBucket == 45) {
                    level = 40;
                } else {
                    level = 30;
                }
                if (calcTrackers) {
                    Pair<Integer, TrackerInfo> levelTypePair = calcAppRestrictionLevelFromTackers(uid, packageName, 100);
                    int l = ((Integer) levelTypePair.first).intValue();
                    if (l == 20) {
                        return new Pair<>(20, (TrackerInfo) levelTypePair.second);
                    }
                    if (l > level) {
                        level = l;
                        trackerInfo = (TrackerInfo) levelTypePair.second;
                    }
                    if (level == 50) {
                        if (allowRequestBgRestricted) {
                            this.mBgHandler.obtainMessage(4, uid, 0, packageName).sendToTarget();
                        }
                        Pair<Integer, TrackerInfo> levelTypePair2 = calcAppRestrictionLevelFromTackers(uid, packageName, 50);
                        level = ((Integer) levelTypePair2.first).intValue();
                        trackerInfo = (TrackerInfo) levelTypePair2.second;
                        break;
                    }
                }
                break;
        }
        return new Pair<>(Integer.valueOf(level), trackerInfo);
    }

    private Pair<Integer, TrackerInfo> calcAppRestrictionLevelFromTackers(int uid, String packageName, int maxLevel) {
        TrackerInfo trackerInfo;
        int level = 0;
        int prevLevel = 0;
        BaseAppStateTracker resultTracker = null;
        boolean isRestrictedBucketEnabled = this.mConstantsObserver.mRestrictedBucketEnabled;
        for (int i = this.mAppStateTrackers.size() - 1; i >= 0; i--) {
            int l = this.mAppStateTrackers.get(i).getPolicy().getProposedRestrictionLevel(packageName, uid, maxLevel);
            if (!isRestrictedBucketEnabled && l == 40) {
                l = 30;
            }
            level = Math.max(level, l);
            if (level != prevLevel) {
                BaseAppStateTracker resultTracker2 = this.mAppStateTrackers.get(i);
                resultTracker = resultTracker2;
                prevLevel = level;
            }
        }
        if (resultTracker == null) {
            trackerInfo = this.mEmptyTrackerInfo;
        } else {
            trackerInfo = new TrackerInfo(resultTracker.getType(), resultTracker.getTrackerInfoForStatsd(uid));
        }
        return new Pair<>(Integer.valueOf(level), trackerInfo);
    }

    private static int standbyBucketToRestrictionLevel(int standbyBucket) {
        switch (standbyBucket) {
            case 5:
                return 20;
            case 10:
            case 20:
            case 30:
            case 40:
                return 30;
            case 45:
                return 40;
            case 50:
                return 50;
            default:
                return 0;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getRestrictionLevel(int uid) {
        return this.mRestrictionSettings.getRestrictionLevel(uid);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getRestrictionLevel(int uid, String packageName) {
        return this.mRestrictionSettings.getRestrictionLevel(uid, packageName);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getRestrictionLevel(String packageName, int userId) {
        return this.mRestrictionSettings.getRestrictionLevel(packageName, userId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isAutoRestrictAbusiveAppEnabled() {
        return this.mConstantsObserver.mBgAutoRestrictAbusiveApps;
    }

    long getForegroundServiceTotalDurations(String packageName, int uid, long now, int serviceType) {
        return this.mInjector.getAppFGSTracker().getTotalDurations(packageName, uid, now, AppFGSTracker.foregroundServiceTypeToIndex(serviceType));
    }

    long getForegroundServiceTotalDurations(int uid, long now, int serviceType) {
        return this.mInjector.getAppFGSTracker().getTotalDurations(uid, now, AppFGSTracker.foregroundServiceTypeToIndex(serviceType));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public long getForegroundServiceTotalDurationsSince(String packageName, int uid, long since, long now, int serviceType) {
        return this.mInjector.getAppFGSTracker().getTotalDurationsSince(packageName, uid, since, now, AppFGSTracker.foregroundServiceTypeToIndex(serviceType));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public long getForegroundServiceTotalDurationsSince(int uid, long since, long now, int serviceType) {
        return this.mInjector.getAppFGSTracker().getTotalDurationsSince(uid, since, now, AppFGSTracker.foregroundServiceTypeToIndex(serviceType));
    }

    long getMediaSessionTotalDurations(String packageName, int uid, long now) {
        return this.mInjector.getAppMediaSessionTracker().getTotalDurations(packageName, uid, now);
    }

    long getMediaSessionTotalDurations(int uid, long now) {
        return this.mInjector.getAppMediaSessionTracker().getTotalDurations(uid, now);
    }

    long getMediaSessionTotalDurationsSince(String packageName, int uid, long since, long now) {
        return this.mInjector.getAppMediaSessionTracker().getTotalDurationsSince(packageName, uid, since, now);
    }

    long getMediaSessionTotalDurationsSince(int uid, long since, long now) {
        return this.mInjector.getAppMediaSessionTracker().getTotalDurationsSince(uid, since, now);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public long getCompositeMediaPlaybackDurations(String packageName, int uid, long now, long window) {
        long since = Math.max(0L, now - window);
        long mediaPlaybackDuration = Math.max(getMediaSessionTotalDurationsSince(packageName, uid, since, now), getForegroundServiceTotalDurationsSince(packageName, uid, since, now, 2));
        return mediaPlaybackDuration;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public long getCompositeMediaPlaybackDurations(int uid, long now, long window) {
        long since = Math.max(0L, now - window);
        long mediaPlaybackDuration = Math.max(getMediaSessionTotalDurationsSince(uid, since, now), getForegroundServiceTotalDurationsSince(uid, since, now, 2));
        return mediaPlaybackDuration;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasForegroundServices(String packageName, int uid) {
        return this.mInjector.getAppFGSTracker().hasForegroundServices(packageName, uid);
    }

    boolean hasForegroundServices(int uid) {
        return this.mInjector.getAppFGSTracker().hasForegroundServices(uid);
    }

    boolean hasForegroundServiceNotifications(String packageName, int uid) {
        return this.mInjector.getAppFGSTracker().hasForegroundServiceNotifications(packageName, uid);
    }

    boolean hasForegroundServiceNotifications(int uid) {
        return this.mInjector.getAppFGSTracker().hasForegroundServiceNotifications(uid);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public AppBatteryTracker.ImmutableBatteryUsage getUidBatteryExemptedUsageSince(int uid, long since, long now, int types) {
        return this.mInjector.getAppBatteryExemptionTracker().getUidBatteryExemptedUsageSince(uid, since, now, types);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public AppBatteryTracker.ImmutableBatteryUsage getUidBatteryUsage(int uid) {
        return this.mInjector.getUidBatteryUsageProvider().getUidBatteryUsage(uid);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dump(PrintWriter pw, String prefix) {
        pw.print(prefix);
        pw.println("APP BACKGROUND RESTRICTIONS");
        String prefix2 = "  " + prefix;
        pw.print(prefix2);
        pw.println("BACKGROUND RESTRICTION LEVEL SETTINGS");
        this.mRestrictionSettings.dump(pw, "  " + prefix2);
        this.mConstantsObserver.dump(pw, "  " + prefix2);
        int size = this.mAppStateTrackers.size();
        for (int i = 0; i < size; i++) {
            pw.println();
            this.mAppStateTrackers.get(i).dump(pw, prefix2);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dumpAsProto(ProtoOutputStream proto, int uid) {
        int size = this.mAppStateTrackers.size();
        for (int i = 0; i < size; i++) {
            this.mAppStateTrackers.get(i).dumpAsProto(proto, uid);
        }
    }

    private int getRestrictionLevelStatsd(int level) {
        switch (level) {
            case 0:
                return 0;
            case 10:
                return 1;
            case 20:
                return 2;
            case 30:
                return 3;
            case 40:
                return 4;
            case 50:
                return 5;
            case 60:
                return 6;
            default:
                return 0;
        }
    }

    private int getThresholdStatsd(int reason) {
        switch (reason) {
            case 1024:
                return 2;
            case 1536:
                return 1;
            default:
                return 0;
        }
    }

    private int getTrackerTypeStatsd(int type) {
        switch (type) {
            case 1:
                return 1;
            case 2:
                return 2;
            case 3:
                return 3;
            case 4:
                return 4;
            case 5:
                return 5;
            case 6:
                return 6;
            case 7:
                return 7;
            default:
                return 0;
        }
    }

    private int getExemptionReasonStatsd(int uid, int level) {
        if (level != 20) {
            return 1;
        }
        int reasonCode = getBackgroundRestrictionExemptionReason(uid);
        return PowerExemptionManager.getExemptionReasonForStatsd(reasonCode);
    }

    private int getOptimizationLevelStatsd(int level) {
        switch (level) {
            case 0:
                return 0;
            case 10:
                return 3;
            case 30:
                return 1;
            case 50:
                return 2;
            default:
                return 0;
        }
    }

    private int getTargetSdkStatsd(String packageName) {
        PackageManager pm = this.mInjector.getPackageManager();
        if (pm == null) {
            return 0;
        }
        try {
            PackageInfo pkg = pm.getPackageInfo(packageName, 0);
            if (pkg != null && pkg.applicationInfo != null) {
                int targetSdk = pkg.applicationInfo.targetSdkVersion;
                if (targetSdk < 31) {
                    return 1;
                }
                if (targetSdk < 33) {
                    return 2;
                }
                if (targetSdk != 33) {
                    return 0;
                }
                return 3;
            }
            return 0;
        } catch (PackageManager.NameNotFoundException e) {
            return 0;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void applyRestrictionLevel(final String pkgName, final int uid, final int level, TrackerInfo trackerInfo, int curBucket, boolean allowUpdateBucket, int reason, int subReason) {
        TrackerInfo trackerInfo2;
        int reason2;
        int subReason2;
        Object obj;
        int curLevel;
        AppStandbyInternal appStandbyInternal;
        int bucketReason;
        final AppStandbyInternal appStandbyInternal2 = this.mInjector.getAppStandbyInternal();
        if (trackerInfo != null) {
            trackerInfo2 = trackerInfo;
        } else {
            trackerInfo2 = this.mEmptyTrackerInfo;
        }
        synchronized (this.mSettingsLock) {
            try {
                final int curLevel2 = getRestrictionLevel(uid, pkgName);
                try {
                    if (curLevel2 == level) {
                        return;
                    }
                    int levelOfBucket = standbyBucketToRestrictionLevel(curBucket);
                    if (levelOfBucket == level && (bucketReason = appStandbyInternal2.getAppStandbyBucketReason(pkgName, UserHandle.getUserId(uid), SystemClock.elapsedRealtime())) != 0) {
                        int reason3 = bucketReason & JobPackageTracker.EVENT_STOP_REASON_MASK;
                        reason2 = reason3;
                        subReason2 = bucketReason & 255;
                    } else {
                        reason2 = reason;
                        subReason2 = subReason;
                    }
                    try {
                        int prevReason = this.mRestrictionSettings.getReason(pkgName, uid);
                        final int subReason3 = subReason2;
                        int subReason4 = reason2;
                        final int reason4 = reason2;
                        try {
                            this.mRestrictionSettings.update(pkgName, uid, level, subReason4, subReason3);
                            if (allowUpdateBucket && curBucket != 5) {
                                if (level >= 40 && curLevel2 < 40) {
                                    if (this.mConstantsObserver.mRestrictedBucketEnabled && curBucket != 45) {
                                        if (this.mConstantsObserver.mBgAutoRestrictedBucket || level == 40) {
                                            boolean doIt = true;
                                            Object obj2 = this.mSettingsLock;
                                            synchronized (obj2) {
                                                try {
                                                    int index = this.mActiveUids.indexOfKey(uid, pkgName);
                                                    if (index >= 0) {
                                                        final TrackerInfo localTrackerInfo = trackerInfo2;
                                                        try {
                                                            obj = obj2;
                                                            curLevel = curLevel2;
                                                            appStandbyInternal = appStandbyInternal2;
                                                            try {
                                                                this.mActiveUids.add(uid, pkgName, new Runnable() { // from class: com.android.server.am.AppRestrictionController$$ExternalSyntheticLambda10
                                                                    @Override // java.lang.Runnable
                                                                    public final void run() {
                                                                        AppRestrictionController.this.m1185xcbbfd276(appStandbyInternal2, pkgName, uid, reason4, subReason3, curLevel2, level, localTrackerInfo);
                                                                    }
                                                                });
                                                                doIt = false;
                                                            } catch (Throwable th) {
                                                                th = th;
                                                                while (true) {
                                                                    try {
                                                                        break;
                                                                    } catch (Throwable th2) {
                                                                        th = th2;
                                                                    }
                                                                }
                                                                throw th;
                                                            }
                                                        } catch (Throwable th3) {
                                                            th = th3;
                                                            obj = obj2;
                                                        }
                                                    } else {
                                                        obj = obj2;
                                                        curLevel = curLevel2;
                                                        appStandbyInternal = appStandbyInternal2;
                                                    }
                                                } catch (Throwable th4) {
                                                    th = th4;
                                                    obj = obj2;
                                                }
                                                try {
                                                    if (doIt) {
                                                        appStandbyInternal.restrictApp(pkgName, UserHandle.getUserId(uid), reason4, subReason3);
                                                        logAppBackgroundRestrictionInfo(pkgName, uid, curLevel, level, trackerInfo2, reason4);
                                                    }
                                                } catch (Throwable th5) {
                                                    th = th5;
                                                    while (true) {
                                                        break;
                                                        break;
                                                    }
                                                    throw th;
                                                }
                                            }
                                        }
                                    }
                                } else if (curLevel2 >= 40 && level < 40) {
                                    synchronized (this.mSettingsLock) {
                                        try {
                                            int index2 = this.mActiveUids.indexOfKey(uid, pkgName);
                                            if (index2 >= 0) {
                                                try {
                                                    this.mActiveUids.add(uid, pkgName, (Object) null);
                                                } catch (Throwable th6) {
                                                    th = th6;
                                                    while (true) {
                                                        try {
                                                            break;
                                                        } catch (Throwable th7) {
                                                            th = th7;
                                                        }
                                                    }
                                                    throw th;
                                                }
                                            }
                                            appStandbyInternal2.maybeUnrestrictApp(pkgName, UserHandle.getUserId(uid), prevReason & JobPackageTracker.EVENT_STOP_REASON_MASK, prevReason & 255, reason4, subReason3);
                                            logAppBackgroundRestrictionInfo(pkgName, uid, curLevel2, level, trackerInfo2, reason4);
                                        } catch (Throwable th8) {
                                            th = th8;
                                        }
                                    }
                                }
                            }
                        } catch (Throwable th9) {
                            th = th9;
                            while (true) {
                                try {
                                    break;
                                } catch (Throwable th10) {
                                    th = th10;
                                }
                            }
                            throw th;
                        }
                    } catch (Throwable th11) {
                        th = th11;
                    }
                } catch (Throwable th12) {
                    th = th12;
                }
            } catch (Throwable th13) {
                th = th13;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$applyRestrictionLevel$1$com-android-server-am-AppRestrictionController  reason: not valid java name */
    public /* synthetic */ void m1185xcbbfd276(AppStandbyInternal appStandbyInternal, String pkgName, int uid, int localReason, int localSubReason, int curLevel, int level, TrackerInfo localTrackerInfo) {
        appStandbyInternal.restrictApp(pkgName, UserHandle.getUserId(uid), localReason, localSubReason);
        logAppBackgroundRestrictionInfo(pkgName, uid, curLevel, level, localTrackerInfo, localReason);
    }

    private void logAppBackgroundRestrictionInfo(String pkgName, int uid, int prevLevel, int level, TrackerInfo trackerInfo, int reason) {
        FrameworkStatsLog.write((int) FrameworkStatsLog.APP_BACKGROUND_RESTRICTIONS_INFO, uid, getRestrictionLevelStatsd(level), getThresholdStatsd(reason), getTrackerTypeStatsd(trackerInfo.mType), trackerInfo.mType == 3 ? trackerInfo.mInfo : null, trackerInfo.mType == 1 ? trackerInfo.mInfo : null, trackerInfo.mType == 6 ? trackerInfo.mInfo : null, trackerInfo.mType == 7 ? trackerInfo.mInfo : null, getExemptionReasonStatsd(uid, level), getOptimizationLevelStatsd(level), getTargetSdkStatsd(pkgName), ActivityManager.isLowRamDeviceStatic(), getRestrictionLevelStatsd(prevLevel));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleBackgroundRestrictionChanged(int uid, String pkgName, boolean restricted) {
        int tentativeBucket;
        int size = this.mAppStateTrackers.size();
        for (int i = 0; i < size; i++) {
            this.mAppStateTrackers.get(i).onBackgroundRestrictionChanged(uid, pkgName, restricted);
        }
        AppStandbyInternal appStandbyInternal = this.mInjector.getAppStandbyInternal();
        int userId = UserHandle.getUserId(uid);
        long now = SystemClock.elapsedRealtime();
        int curBucket = appStandbyInternal.getAppStandbyBucket(pkgName, userId, now, false);
        if (restricted) {
            applyRestrictionLevel(pkgName, uid, 50, this.mEmptyTrackerInfo, curBucket, true, 1024, 2);
            this.mBgHandler.obtainMessage(9, uid, 0, pkgName).sendToTarget();
            return;
        }
        int lastLevel = this.mRestrictionSettings.getLastRestrictionLevel(uid, pkgName);
        int i2 = 40;
        if (curBucket == 5) {
            tentativeBucket = 5;
        } else {
            if (lastLevel == 40) {
                i2 = 45;
            }
            tentativeBucket = i2;
        }
        Pair<Integer, TrackerInfo> levelTypePair = calcAppRestrictionLevel(UserHandle.getUserId(uid), uid, pkgName, tentativeBucket, false, true);
        applyRestrictionLevel(pkgName, uid, ((Integer) levelTypePair.first).intValue(), (TrackerInfo) levelTypePair.second, curBucket, true, 768, 3);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void dispatchAppRestrictionLevelChanges(final int uid, final String pkgName, final int newLevel) {
        this.mRestrictionListeners.forEach(new Consumer() { // from class: com.android.server.am.AppRestrictionController$$ExternalSyntheticLambda0
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ((ActivityManagerInternal.AppBackgroundRestrictionListener) obj).onRestrictionLevelChanged(uid, pkgName, newLevel);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void dispatchAutoRestrictedBucketFeatureFlagChanged(final boolean newValue) {
        final AppStandbyInternal appStandbyInternal = this.mInjector.getAppStandbyInternal();
        final ArrayList<Runnable> pendingTasks = new ArrayList<>();
        synchronized (this.mSettingsLock) {
            this.mRestrictionSettings.forEachUidLocked(new Consumer() { // from class: com.android.server.am.AppRestrictionController$$ExternalSyntheticLambda8
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    AppRestrictionController.this.m1186xca9a65d(pendingTasks, newValue, appStandbyInternal, (Integer) obj);
                }
            });
        }
        for (int i = 0; i < pendingTasks.size(); i++) {
            pendingTasks.get(i).run();
        }
        this.mRestrictionListeners.forEach(new Consumer() { // from class: com.android.server.am.AppRestrictionController$$ExternalSyntheticLambda9
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ((ActivityManagerInternal.AppBackgroundRestrictionListener) obj).onAutoRestrictedBucketFeatureFlagChanged(newValue);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$dispatchAutoRestrictedBucketFeatureFlagChanged$6$com-android-server-am-AppRestrictionController  reason: not valid java name */
    public /* synthetic */ void m1186xca9a65d(final ArrayList pendingTasks, final boolean newValue, final AppStandbyInternal appStandbyInternal, final Integer uid) {
        this.mRestrictionSettings.forEachPackageInUidLocked(uid.intValue(), new TriConsumer() { // from class: com.android.server.am.AppRestrictionController$$ExternalSyntheticLambda7
            public final void accept(Object obj, Object obj2, Object obj3) {
                AppRestrictionController.lambda$dispatchAutoRestrictedBucketFeatureFlagChanged$5(pendingTasks, newValue, appStandbyInternal, uid, (String) obj, (Integer) obj2, (Integer) obj3);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$dispatchAutoRestrictedBucketFeatureFlagChanged$5(ArrayList pendingTasks, boolean newValue, final AppStandbyInternal appStandbyInternal, final Integer uid, final String pkgName, Integer level, final Integer reason) {
        Runnable runnable;
        if (level.intValue() == 50) {
            if (newValue) {
                runnable = new Runnable() { // from class: com.android.server.am.AppRestrictionController$$ExternalSyntheticLambda1
                    @Override // java.lang.Runnable
                    public final void run() {
                        appStandbyInternal.restrictApp(pkgName, UserHandle.getUserId(uid.intValue()), r3.intValue() & JobPackageTracker.EVENT_STOP_REASON_MASK, reason.intValue() & 255);
                    }
                };
            } else {
                runnable = new Runnable() { // from class: com.android.server.am.AppRestrictionController$$ExternalSyntheticLambda2
                    @Override // java.lang.Runnable
                    public final void run() {
                        appStandbyInternal.maybeUnrestrictApp(pkgName, UserHandle.getUserId(uid.intValue()), r3.intValue() & JobPackageTracker.EVENT_STOP_REASON_MASK, reason.intValue() & 255, 768, 6);
                    }
                };
            }
            pendingTasks.add(runnable);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleAppStandbyBucketChanged(int bucket, String packageName, int userId) {
        int uid = this.mInjector.getPackageManagerInternal().getPackageUid(packageName, 819200L, userId);
        Pair<Integer, TrackerInfo> levelTypePair = calcAppRestrictionLevel(userId, uid, packageName, bucket, false, false);
        applyRestrictionLevel(packageName, uid, ((Integer) levelTypePair.first).intValue(), (TrackerInfo) levelTypePair.second, bucket, false, 256, 0);
    }

    void handleRequestBgRestricted(String packageName, int uid) {
        this.mNotificationHelper.postRequestBgRestrictedIfNecessary(packageName, uid);
    }

    void handleCancelRequestBgRestricted(String packageName, int uid) {
        this.mNotificationHelper.cancelRequestBgRestrictedIfNecessary(packageName, uid);
    }

    void handleUidProcStateChanged(int uid, int procState) {
        int size = this.mAppStateTrackers.size();
        for (int i = 0; i < size; i++) {
            this.mAppStateTrackers.get(i).onUidProcStateChanged(uid, procState);
        }
    }

    void handleUidGone(int uid) {
        int size = this.mAppStateTrackers.size();
        for (int i = 0; i < size; i++) {
            this.mAppStateTrackers.get(i).onUidGone(uid);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class NotificationHelper {
        static final String ACTION_FGS_MANAGER_TRAMPOLINE = "com.android.server.am.ACTION_FGS_MANAGER_TRAMPOLINE";
        static final String GROUP_KEY = "com.android.app.abusive_bg_apps";
        static final int NOTIFICATION_TYPE_ABUSIVE_CURRENT_DRAIN = 0;
        static final int NOTIFICATION_TYPE_LAST = 2;
        static final int NOTIFICATION_TYPE_LONG_RUNNING_FGS = 1;
        static final String PACKAGE_SCHEME = "package";
        static final int SUMMARY_NOTIFICATION_ID = 203105544;
        private final AppRestrictionController mBgController;
        private final Context mContext;
        private final Injector mInjector;
        private final Object mLock;
        private final NotificationManager mNotificationManager;
        private final Object mSettingsLock;
        static final String[] NOTIFICATION_TYPE_STRINGS = {"Abusive current drain", "Long-running FGS"};
        static final String ATTR_LAST_BATTERY_NOTIFICATION_TIME = "last_batt_noti_ts";
        static final String ATTR_LAST_LONG_FGS_NOTIFICATION_TIME = "last_long_fgs_noti_ts";
        static final String[] NOTIFICATION_TIME_ATTRS = {ATTR_LAST_BATTERY_NOTIFICATION_TIME, ATTR_LAST_LONG_FGS_NOTIFICATION_TIME};
        private final BroadcastReceiver mActionButtonReceiver = new BroadcastReceiver() { // from class: com.android.server.am.AppRestrictionController.NotificationHelper.1
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context, Intent intent) {
                char c;
                intent.getAction();
                String action = intent.getAction();
                switch (action.hashCode()) {
                    case -2048453630:
                        if (action.equals(NotificationHelper.ACTION_FGS_MANAGER_TRAMPOLINE)) {
                            c = 0;
                            break;
                        }
                    default:
                        c = 65535;
                        break;
                }
                switch (c) {
                    case 0:
                        String packageName = intent.getStringExtra("android.intent.extra.PACKAGE_NAME");
                        int uid = intent.getIntExtra("android.intent.extra.UID", 0);
                        NotificationHelper.this.cancelRequestBgRestrictedIfNecessary(packageName, uid);
                        Intent newIntent = new Intent("android.intent.action.SHOW_FOREGROUND_SERVICE_MANAGER");
                        newIntent.addFlags(16777216);
                        NotificationHelper.this.mContext.sendBroadcastAsUser(newIntent, UserHandle.SYSTEM);
                        return;
                    default:
                        return;
                }
            }
        };
        private int mNotificationIDStepper = 203105545;

        @Retention(RetentionPolicy.SOURCE)
        /* loaded from: classes.dex */
        @interface NotificationType {
        }

        /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
        static int notificationTimeAttrToType(String attr) {
            char c;
            switch (attr.hashCode()) {
                case -1157017279:
                    if (attr.equals(ATTR_LAST_LONG_FGS_NOTIFICATION_TIME)) {
                        c = 1;
                        break;
                    }
                    c = 65535;
                    break;
                case 17543473:
                    if (attr.equals(ATTR_LAST_BATTERY_NOTIFICATION_TIME)) {
                        c = 0;
                        break;
                    }
                    c = 65535;
                    break;
                default:
                    c = 65535;
                    break;
            }
            switch (c) {
                case 0:
                    return 0;
                case 1:
                    return 1;
                default:
                    throw new IllegalArgumentException();
            }
        }

        static String notificationTypeToTimeAttr(int type) {
            return NOTIFICATION_TIME_ATTRS[type];
        }

        static String notificationTypeToString(int notificationType) {
            return NOTIFICATION_TYPE_STRINGS[notificationType];
        }

        NotificationHelper(AppRestrictionController controller) {
            this.mBgController = controller;
            Injector injector = controller.mInjector;
            this.mInjector = injector;
            this.mNotificationManager = injector.getNotificationManager();
            this.mLock = controller.mLock;
            this.mSettingsLock = controller.mSettingsLock;
            this.mContext = injector.getContext();
        }

        void onSystemReady() {
            this.mContext.registerReceiverForAllUsers(this.mActionButtonReceiver, new IntentFilter(ACTION_FGS_MANAGER_TRAMPOLINE), "android.permission.MANAGE_ACTIVITY_TASKS", this.mBgController.mBgHandler, 4);
        }

        void postRequestBgRestrictedIfNecessary(String packageName, int uid) {
            if (!this.mBgController.mConstantsObserver.mBgPromptAbusiveAppsToBgRestricted) {
                return;
            }
            Intent intent = new Intent("android.settings.VIEW_ADVANCED_POWER_USAGE_DETAIL");
            intent.setData(Uri.fromParts("package", packageName, null));
            intent.addFlags(AudioFormat.AAC_ADIF);
            PendingIntent pendingIntent = PendingIntent.getActivityAsUser(this.mContext, 0, intent, AudioFormat.DTS_HD, null, UserHandle.of(UserHandle.getUserId(uid)));
            boolean hasForegroundServices = this.mBgController.hasForegroundServices(packageName, uid);
            boolean hasForegroundServiceNotifications = this.mBgController.hasForegroundServiceNotifications(packageName, uid);
            if (this.mBgController.mConstantsObserver.mBgPromptFgsWithNotiToBgRestricted || !hasForegroundServices || !hasForegroundServiceNotifications) {
                postNotificationIfNecessary(0, 17040928, 17040910, pendingIntent, packageName, uid, null);
            }
        }

        void postLongRunningFgsIfNecessary(String packageName, int uid) {
            FrameworkStatsLog.write((int) FrameworkStatsLog.APP_BACKGROUND_RESTRICTIONS_INFO, uid, this.mBgController.getRestrictionLevel(uid), 0, 3, this.mInjector.getAppFGSTracker().getTrackerInfoForStatsd(uid), (byte[]) null, (byte[]) null, (byte[]) null, PowerExemptionManager.getExemptionReasonForStatsd(this.mBgController.getBackgroundRestrictionExemptionReason(uid)), 0, 0, ActivityManager.isLowRamDeviceStatic(), this.mBgController.getRestrictionLevel(uid));
            if (!this.mBgController.mConstantsObserver.mBgPromptFgsOnLongRunning) {
                return;
            }
            if (!this.mBgController.mConstantsObserver.mBgPromptFgsWithNotiOnLongRunning && this.mBgController.hasForegroundServiceNotifications(packageName, uid)) {
                return;
            }
            Intent intent = new Intent("android.intent.action.SHOW_FOREGROUND_SERVICE_MANAGER");
            intent.addFlags(16777216);
            PendingIntent pendingIntent = PendingIntent.getBroadcastAsUser(this.mContext, 0, intent, AudioFormat.DTS_HD, UserHandle.SYSTEM);
            postNotificationIfNecessary(1, 17040929, 17040911, pendingIntent, packageName, uid, null);
        }

        long getNotificationMinInterval(int notificationType) {
            switch (notificationType) {
                case 0:
                    return this.mBgController.mConstantsObserver.mBgAbusiveNotificationMinIntervalMs;
                case 1:
                    return this.mBgController.mConstantsObserver.mBgLongFgsNotificationMinIntervalMs;
                default:
                    return 0L;
            }
        }

        int getNotificationIdIfNecessary(int notificationType, String packageName, int uid) {
            synchronized (this.mSettingsLock) {
                RestrictionSettings.PkgSettings settings = this.mBgController.mRestrictionSettings.getRestrictionSettingsLocked(uid, packageName);
                if (settings == null) {
                    return 0;
                }
                long now = this.mInjector.currentTimeMillis();
                long lastNotificationShownTime = settings.getLastNotificationTime(notificationType);
                if (lastNotificationShownTime != 0 && getNotificationMinInterval(notificationType) + lastNotificationShownTime > now) {
                    return 0;
                }
                settings.setLastNotificationTime(notificationType, now);
                int notificationId = settings.getNotificationId(notificationType);
                if (notificationId <= 0) {
                    int i = this.mNotificationIDStepper;
                    this.mNotificationIDStepper = i + 1;
                    notificationId = i;
                    settings.setNotificationId(notificationType, notificationId);
                }
                return notificationId;
            }
        }

        void postNotificationIfNecessary(int notificationType, int titleRes, int messageRes, PendingIntent pendingIntent, String packageName, int uid, Notification.Action[] actions) {
            int notificationId = getNotificationIdIfNecessary(notificationType, packageName, uid);
            if (notificationId <= 0) {
                return;
            }
            PackageManagerInternal pmi = this.mInjector.getPackageManagerInternal();
            PackageManager pm = this.mInjector.getPackageManager();
            ApplicationInfo ai = pmi.getApplicationInfo(packageName, 819200L, 1000, UserHandle.getUserId(uid));
            String title = this.mContext.getString(titleRes);
            Context context = this.mContext;
            Object[] objArr = new Object[1];
            objArr[0] = ai != null ? ai.loadLabel(pm) : packageName;
            String message = context.getString(messageRes, objArr);
            Icon icon = ai != null ? Icon.createWithResource(packageName, ai.icon) : null;
            postNotification(notificationId, packageName, uid, title, message, icon, pendingIntent, actions);
        }

        void postNotification(int notificationId, String packageName, int uid, String title, String message, Icon icon, PendingIntent pendingIntent, Notification.Action[] actions) {
            UserHandle targetUser = UserHandle.of(UserHandle.getUserId(uid));
            postSummaryNotification(targetUser);
            Notification.Builder notificationBuilder = new Notification.Builder(this.mContext, SystemNotificationChannels.ABUSIVE_BACKGROUND_APPS).setAutoCancel(true).setGroup(GROUP_KEY).setWhen(this.mInjector.currentTimeMillis()).setSmallIcon(17301642).setColor(this.mContext.getColor(17170460)).setContentTitle(title).setContentText(message).setContentIntent(pendingIntent);
            if (icon != null) {
                notificationBuilder.setLargeIcon(icon);
            }
            if (actions != null) {
                for (Notification.Action action : actions) {
                    notificationBuilder.addAction(action);
                }
            }
            Notification notification = notificationBuilder.build();
            notification.extras.putString("android.intent.extra.PACKAGE_NAME", packageName);
            this.mNotificationManager.notifyAsUser(null, notificationId, notification, targetUser);
        }

        private void postSummaryNotification(UserHandle targetUser) {
            Notification summary = new Notification.Builder(this.mContext, SystemNotificationChannels.ABUSIVE_BACKGROUND_APPS).setGroup(GROUP_KEY).setGroupSummary(true).setStyle(new Notification.BigTextStyle()).setSmallIcon(17301642).setColor(this.mContext.getColor(17170460)).build();
            this.mNotificationManager.notifyAsUser(null, SUMMARY_NOTIFICATION_ID, summary, targetUser);
        }

        void cancelRequestBgRestrictedIfNecessary(String packageName, int uid) {
            int notificationId;
            synchronized (this.mSettingsLock) {
                RestrictionSettings.PkgSettings settings = this.mBgController.mRestrictionSettings.getRestrictionSettingsLocked(uid, packageName);
                if (settings != null && (notificationId = settings.getNotificationId(0)) > 0) {
                    this.mNotificationManager.cancel(notificationId);
                }
            }
        }

        void cancelLongRunningFGSNotificationIfNecessary(String packageName, int uid) {
            int notificationId;
            synchronized (this.mSettingsLock) {
                RestrictionSettings.PkgSettings settings = this.mBgController.mRestrictionSettings.getRestrictionSettingsLocked(uid, packageName);
                if (settings != null && (notificationId = settings.getNotificationId(1)) > 0) {
                    this.mNotificationManager.cancel(notificationId);
                }
            }
        }
    }

    void handleUidInactive(int uid, boolean disabled) {
        ArrayList<Runnable> pendingTasks = this.mTmpRunnables;
        synchronized (this.mSettingsLock) {
            int index = this.mActiveUids.indexOfKey(uid);
            if (index < 0) {
                return;
            }
            int numPackages = this.mActiveUids.numElementsForKeyAt(index);
            for (int i = 0; i < numPackages; i++) {
                Runnable pendingTask = (Runnable) this.mActiveUids.valueAt(index, i);
                if (pendingTask != null) {
                    pendingTasks.add(pendingTask);
                }
            }
            this.mActiveUids.deleteAt(index);
            int size = pendingTasks.size();
            for (int i2 = 0; i2 < size; i2++) {
                pendingTasks.get(i2).run();
            }
            pendingTasks.clear();
        }
    }

    void handleUidActive(final int uid) {
        synchronized (this.mSettingsLock) {
            final AppStandbyInternal appStandbyInternal = this.mInjector.getAppStandbyInternal();
            final int userId = UserHandle.getUserId(uid);
            this.mRestrictionSettings.forEachPackageInUidLocked(uid, new TriConsumer() { // from class: com.android.server.am.AppRestrictionController$$ExternalSyntheticLambda6
                public final void accept(Object obj, Object obj2, Object obj3) {
                    AppRestrictionController.this.m1187x5c7e14a6(uid, appStandbyInternal, userId, (String) obj, (Integer) obj2, (Integer) obj3);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$handleUidActive$9$com-android-server-am-AppRestrictionController  reason: not valid java name */
    public /* synthetic */ void m1187x5c7e14a6(int uid, final AppStandbyInternal appStandbyInternal, final int userId, final String pkgName, Integer level, final Integer reason) {
        if (this.mConstantsObserver.mBgAutoRestrictedBucket && level.intValue() == 50) {
            this.mActiveUids.add(uid, pkgName, new Runnable() { // from class: com.android.server.am.AppRestrictionController$$ExternalSyntheticLambda4
                @Override // java.lang.Runnable
                public final void run() {
                    appStandbyInternal.restrictApp(pkgName, userId, r3.intValue() & JobPackageTracker.EVENT_STOP_REASON_MASK, reason.intValue() & 255);
                }
            });
        } else {
            this.mActiveUids.add(uid, pkgName, (Object) null);
        }
    }

    boolean isOnDeviceIdleAllowlist(int uid) {
        int appId = UserHandle.getAppId(uid);
        return Arrays.binarySearch(this.mDeviceIdleAllowlist, appId) >= 0 || Arrays.binarySearch(this.mDeviceIdleExceptIdleAllowlist, appId) >= 0;
    }

    boolean isOnSystemDeviceIdleAllowlist(int uid) {
        int appId = UserHandle.getAppId(uid);
        return this.mSystemDeviceIdleAllowlist.contains(Integer.valueOf(appId)) || this.mSystemDeviceIdleExceptIdleAllowlist.contains(Integer.valueOf(appId));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setDeviceIdleAllowlist(int[] allAppids, int[] exceptIdleAppids) {
        this.mDeviceIdleAllowlist = allAppids;
        this.mDeviceIdleExceptIdleAllowlist = exceptIdleAppids;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getBackgroundRestrictionExemptionReason(int uid) {
        if (UserHandle.isCore(uid)) {
            return 51;
        }
        if (isOnSystemDeviceIdleAllowlist(uid)) {
            return 300;
        }
        if (UserManager.isDeviceInDemoMode(this.mContext)) {
            return 63;
        }
        int userId = UserHandle.getUserId(uid);
        if (this.mInjector.getUserManagerInternal().hasUserRestriction("no_control_apps", userId)) {
            return 323;
        }
        ActivityManagerInternal am = this.mInjector.getActivityManagerInternal();
        if (am.isDeviceOwner(uid)) {
            return 55;
        }
        if (am.isProfileOwner(uid)) {
            return 56;
        }
        int uidProcState = am.getUidProcessState(uid);
        if (uidProcState <= 0) {
            return 10;
        }
        if (uidProcState <= 1) {
            return 11;
        }
        String[] packages = this.mInjector.getPackageManager().getPackagesForUid(uid);
        if (packages != null) {
            AppOpsManager appOpsManager = this.mInjector.getAppOpsManager();
            PackageManagerInternal pm = this.mInjector.getPackageManagerInternal();
            AppStandbyInternal appStandbyInternal = this.mInjector.getAppStandbyInternal();
            for (String pkg : packages) {
                if (isSystemModule(pkg)) {
                    return 320;
                }
                if (isCarrierApp(pkg)) {
                    return 321;
                }
                if (isExemptedFromSysConfig(pkg) || this.mConstantsObserver.mBgRestrictionExemptedPackages.contains(pkg)) {
                    return 300;
                }
                if (pm.isPackageStateProtected(pkg, userId)) {
                    return 322;
                }
                if (appStandbyInternal.isActiveDeviceAdmin(pkg, userId)) {
                    return FrameworkStatsLog.APP_BACKGROUND_RESTRICTIONS_INFO__EXEMPTION_REASON__REASON_ACTIVE_DEVICE_ADMIN;
                }
            }
            for (String pkg2 : packages) {
                if (appOpsManager.checkOpNoThrow(47, uid, pkg2) == 0) {
                    return 68;
                }
                if (appOpsManager.checkOpNoThrow(94, uid, pkg2) == 0) {
                    return 69;
                }
            }
        }
        if (isRoleHeldByUid("android.app.role.DIALER", uid)) {
            return FrameworkStatsLog.APP_BACKGROUND_RESTRICTIONS_INFO__EXEMPTION_REASON__REASON_ROLE_DIALER;
        }
        if (isRoleHeldByUid("android.app.role.EMERGENCY", uid)) {
            return 319;
        }
        if (isOnDeviceIdleAllowlist(uid)) {
            return 65;
        }
        if (am.isAssociatedCompanionApp(UserHandle.getUserId(uid), uid)) {
            return 57;
        }
        return -1;
    }

    private boolean isCarrierApp(String packageName) {
        synchronized (this.mCarrierPrivilegedLock) {
            if (this.mCarrierPrivilegedApps == null) {
                fetchCarrierPrivilegedAppsCPL();
            }
            List<String> list = this.mCarrierPrivilegedApps;
            if (list != null) {
                return list.contains(packageName);
            }
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void clearCarrierPrivilegedApps() {
        synchronized (this.mCarrierPrivilegedLock) {
            this.mCarrierPrivilegedApps = null;
        }
    }

    private void fetchCarrierPrivilegedAppsCPL() {
        TelephonyManager telephonyManager = this.mInjector.getTelephonyManager();
        this.mCarrierPrivilegedApps = telephonyManager.getCarrierPrivilegedPackagesForAllActiveSubscriptions();
    }

    private boolean isRoleHeldByUid(String roleName, int uid) {
        boolean z;
        synchronized (this.mLock) {
            ArrayList<String> roles = this.mUidRolesMapping.get(uid);
            z = roles != null && roles.indexOf(roleName) >= 0;
        }
        return z;
    }

    private void initRolesInInterest() {
        String[] strArr;
        int[] allUsers = this.mInjector.getUserManagerInternal().getUserIds();
        for (String role : ROLES_IN_INTEREST) {
            if (this.mInjector.getRoleManager().isRoleAvailable(role)) {
                for (int userId : allUsers) {
                    UserHandle user = UserHandle.of(userId);
                    onRoleHoldersChanged(role, user);
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onRoleHoldersChanged(String roleName, UserHandle user) {
        List<String> rolePkgs = this.mInjector.getRoleManager().getRoleHoldersAsUser(roleName, user);
        ArraySet<Integer> roleUids = new ArraySet<>();
        int userId = user.getIdentifier();
        if (rolePkgs != null) {
            PackageManagerInternal pm = this.mInjector.getPackageManagerInternal();
            for (String pkg : rolePkgs) {
                roleUids.add(Integer.valueOf(pm.getPackageUid(pkg, 819200L, userId)));
            }
        }
        synchronized (this.mLock) {
            for (int i = this.mUidRolesMapping.size() - 1; i >= 0; i--) {
                int uid = this.mUidRolesMapping.keyAt(i);
                if (UserHandle.getUserId(uid) == userId) {
                    ArrayList<String> roles = this.mUidRolesMapping.valueAt(i);
                    int index = roles.indexOf(roleName);
                    boolean isRole = roleUids.contains(Integer.valueOf(uid));
                    if (index >= 0) {
                        if (!isRole) {
                            roles.remove(index);
                            if (roles.isEmpty()) {
                                this.mUidRolesMapping.removeAt(i);
                            }
                        }
                    } else if (isRole) {
                        roles.add(roleName);
                        roleUids.remove(Integer.valueOf(uid));
                    }
                }
            }
            int i2 = roleUids.size();
            for (int i3 = i2 - 1; i3 >= 0; i3--) {
                ArrayList<String> roles2 = new ArrayList<>();
                roles2.add(roleName);
                this.mUidRolesMapping.put(roleUids.valueAt(i3).intValue(), roles2);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Handler getBackgroundHandler() {
        return this.mBgHandler;
    }

    HandlerThread getBackgroundHandlerThread() {
        return this.mBgHandlerThread;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Object getLock() {
        return this.mLock;
    }

    void addAppStateTracker(BaseAppStateTracker tracker) {
        this.mAppStateTrackers.add(tracker);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public <T extends BaseAppStateTracker> T getAppStateTracker(Class<T> trackerClass) {
        Iterator<BaseAppStateTracker> it = this.mAppStateTrackers.iterator();
        while (it.hasNext()) {
            T t = (T) it.next();
            if (trackerClass.isAssignableFrom(t.getClass())) {
                return t;
            }
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void postLongRunningFgsIfNecessary(String packageName, int uid) {
        this.mNotificationHelper.postLongRunningFgsIfNecessary(packageName, uid);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void cancelLongRunningFGSNotificationIfNecessary(String packageName, int uid) {
        this.mNotificationHelper.cancelLongRunningFGSNotificationIfNecessary(packageName, uid);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public String getPackageName(int pid) {
        return this.mInjector.getPackageName(pid);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class BgHandler extends Handler {
        static final int MSG_APP_RESTRICTION_LEVEL_CHANGED = 1;
        static final int MSG_APP_STANDBY_BUCKET_CHANGED = 2;
        static final int MSG_BACKGROUND_RESTRICTION_CHANGED = 0;
        static final int MSG_CANCEL_REQUEST_BG_RESTRICTED = 9;
        static final int MSG_LOAD_RESTRICTION_SETTINGS = 10;
        static final int MSG_PERSIST_RESTRICTION_SETTINGS = 11;
        static final int MSG_REQUEST_BG_RESTRICTED = 4;
        static final int MSG_UID_ACTIVE = 6;
        static final int MSG_UID_GONE = 7;
        static final int MSG_UID_IDLE = 5;
        static final int MSG_UID_PROC_STATE_CHANGED = 8;
        static final int MSG_USER_INTERACTION_STARTED = 3;
        private final Injector mInjector;

        BgHandler(Looper looper, Injector injector) {
            super(looper);
            this.mInjector = injector;
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            AppRestrictionController c = this.mInjector.getAppRestrictionController();
            switch (msg.what) {
                case 0:
                    c.handleBackgroundRestrictionChanged(msg.arg1, (String) msg.obj, msg.arg2 == 1);
                    return;
                case 1:
                    c.dispatchAppRestrictionLevelChanges(msg.arg1, (String) msg.obj, msg.arg2);
                    return;
                case 2:
                    c.handleAppStandbyBucketChanged(msg.arg2, (String) msg.obj, msg.arg1);
                    return;
                case 3:
                    c.onUserInteractionStarted((String) msg.obj, msg.arg1);
                    return;
                case 4:
                    c.handleRequestBgRestricted((String) msg.obj, msg.arg1);
                    return;
                case 5:
                    c.handleUidInactive(msg.arg1, msg.arg2 == 1);
                    return;
                case 6:
                    c.handleUidActive(msg.arg1);
                    return;
                case 7:
                    c.handleUidInactive(msg.arg1, msg.arg2 == 1);
                    c.handleUidGone(msg.arg1);
                    return;
                case 8:
                    c.handleUidProcStateChanged(msg.arg1, msg.arg2);
                    return;
                case 9:
                    c.handleCancelRequestBgRestricted((String) msg.obj, msg.arg1);
                    return;
                case 10:
                    c.mRestrictionSettings.loadFromXml(true);
                    return;
                case 11:
                    c.mRestrictionSettings.persistToXml(msg.arg1);
                    return;
                default:
                    return;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class Injector {
        private ActivityManagerInternal mActivityManagerInternal;
        private AppBatteryExemptionTracker mAppBatteryExemptionTracker;
        private AppBatteryTracker mAppBatteryTracker;
        private AppFGSTracker mAppFGSTracker;
        private AppHibernationManagerInternal mAppHibernationInternal;
        private AppMediaSessionTracker mAppMediaSessionTracker;
        private AppOpsManager mAppOpsManager;
        private AppPermissionTracker mAppPermissionTracker;
        private AppRestrictionController mAppRestrictionController;
        private AppStandbyInternal mAppStandbyInternal;
        private AppStateTracker mAppStateTracker;
        private final Context mContext;
        private IActivityManager mIActivityManager;
        private NotificationManager mNotificationManager;
        private PackageManagerInternal mPackageManagerInternal;
        private RoleManager mRoleManager;
        private TelephonyManager mTelephonyManager;
        private UserManagerInternal mUserManagerInternal;

        Injector(Context context) {
            this.mContext = context;
        }

        Context getContext() {
            return this.mContext;
        }

        void initAppStateTrackers(AppRestrictionController controller) {
            this.mAppRestrictionController = controller;
            this.mAppBatteryTracker = new AppBatteryTracker(this.mContext, controller);
            this.mAppBatteryExemptionTracker = new AppBatteryExemptionTracker(this.mContext, controller);
            this.mAppFGSTracker = new AppFGSTracker(this.mContext, controller);
            this.mAppMediaSessionTracker = new AppMediaSessionTracker(this.mContext, controller);
            this.mAppPermissionTracker = new AppPermissionTracker(this.mContext, controller);
            controller.mAppStateTrackers.add(this.mAppBatteryTracker);
            controller.mAppStateTrackers.add(this.mAppBatteryExemptionTracker);
            controller.mAppStateTrackers.add(this.mAppFGSTracker);
            controller.mAppStateTrackers.add(this.mAppMediaSessionTracker);
            controller.mAppStateTrackers.add(this.mAppPermissionTracker);
            controller.mAppStateTrackers.add(new AppBroadcastEventsTracker(this.mContext, controller));
            controller.mAppStateTrackers.add(new AppBindServiceEventsTracker(this.mContext, controller));
        }

        ActivityManagerInternal getActivityManagerInternal() {
            if (this.mActivityManagerInternal == null) {
                this.mActivityManagerInternal = (ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class);
            }
            return this.mActivityManagerInternal;
        }

        AppRestrictionController getAppRestrictionController() {
            return this.mAppRestrictionController;
        }

        AppOpsManager getAppOpsManager() {
            if (this.mAppOpsManager == null) {
                this.mAppOpsManager = (AppOpsManager) getContext().getSystemService(AppOpsManager.class);
            }
            return this.mAppOpsManager;
        }

        AppStandbyInternal getAppStandbyInternal() {
            if (this.mAppStandbyInternal == null) {
                this.mAppStandbyInternal = (AppStandbyInternal) LocalServices.getService(AppStandbyInternal.class);
            }
            return this.mAppStandbyInternal;
        }

        AppHibernationManagerInternal getAppHibernationInternal() {
            if (this.mAppHibernationInternal == null) {
                this.mAppHibernationInternal = (AppHibernationManagerInternal) LocalServices.getService(AppHibernationManagerInternal.class);
            }
            return this.mAppHibernationInternal;
        }

        AppStateTracker getAppStateTracker() {
            if (this.mAppStateTracker == null) {
                this.mAppStateTracker = (AppStateTracker) LocalServices.getService(AppStateTracker.class);
            }
            return this.mAppStateTracker;
        }

        IActivityManager getIActivityManager() {
            return ActivityManager.getService();
        }

        UserManagerInternal getUserManagerInternal() {
            if (this.mUserManagerInternal == null) {
                this.mUserManagerInternal = (UserManagerInternal) LocalServices.getService(UserManagerInternal.class);
            }
            return this.mUserManagerInternal;
        }

        PackageManagerInternal getPackageManagerInternal() {
            if (this.mPackageManagerInternal == null) {
                this.mPackageManagerInternal = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
            }
            return this.mPackageManagerInternal;
        }

        PackageManager getPackageManager() {
            return getContext().getPackageManager();
        }

        NotificationManager getNotificationManager() {
            if (this.mNotificationManager == null) {
                this.mNotificationManager = (NotificationManager) getContext().getSystemService(NotificationManager.class);
            }
            return this.mNotificationManager;
        }

        RoleManager getRoleManager() {
            if (this.mRoleManager == null) {
                this.mRoleManager = (RoleManager) getContext().getSystemService(RoleManager.class);
            }
            return this.mRoleManager;
        }

        TelephonyManager getTelephonyManager() {
            if (this.mTelephonyManager == null) {
                this.mTelephonyManager = (TelephonyManager) getContext().getSystemService(TelephonyManager.class);
            }
            return this.mTelephonyManager;
        }

        AppFGSTracker getAppFGSTracker() {
            return this.mAppFGSTracker;
        }

        AppMediaSessionTracker getAppMediaSessionTracker() {
            return this.mAppMediaSessionTracker;
        }

        ActivityManagerService getActivityManagerService() {
            return this.mAppRestrictionController.mActivityManagerService;
        }

        UidBatteryUsageProvider getUidBatteryUsageProvider() {
            return this.mAppBatteryTracker;
        }

        AppBatteryExemptionTracker getAppBatteryExemptionTracker() {
            return this.mAppBatteryExemptionTracker;
        }

        AppPermissionTracker getAppPermissionTracker() {
            return this.mAppPermissionTracker;
        }

        String getPackageName(int pid) {
            ApplicationInfo ai;
            ActivityManagerService am = getActivityManagerService();
            synchronized (am.mPidsSelfLocked) {
                ProcessRecord app = am.mPidsSelfLocked.get(pid);
                if (app != null && (ai = app.info) != null) {
                    return ai.packageName;
                }
                return null;
            }
        }

        void scheduleInitTrackers(Handler handler, Runnable initializers) {
            handler.post(initializers);
        }

        File getDataSystemDeDirectory(int userId) {
            return Environment.getDataSystemDeDirectory(userId);
        }

        long currentTimeMillis() {
            return System.currentTimeMillis();
        }

        boolean isTest() {
            return false;
        }
    }

    private void registerForSystemBroadcasts() {
        IntentFilter packageFilter = new IntentFilter();
        packageFilter.addAction("android.intent.action.PACKAGE_ADDED");
        packageFilter.addAction("android.intent.action.PACKAGE_CHANGED");
        packageFilter.addAction("android.intent.action.PACKAGE_FULLY_REMOVED");
        packageFilter.addDataScheme("package");
        this.mContext.registerReceiverForAllUsers(this.mBroadcastReceiver, packageFilter, null, this.mBgHandler);
        IntentFilter userFilter = new IntentFilter();
        userFilter.addAction("android.intent.action.USER_ADDED");
        userFilter.addAction("android.intent.action.USER_REMOVED");
        userFilter.addAction("android.intent.action.UID_REMOVED");
        this.mContext.registerReceiverForAllUsers(this.mBroadcastReceiver, userFilter, null, this.mBgHandler);
        IntentFilter bootFilter = new IntentFilter();
        bootFilter.addAction("android.intent.action.LOCKED_BOOT_COMPLETED");
        this.mContext.registerReceiverAsUser(this.mBootReceiver, UserHandle.SYSTEM, bootFilter, null, this.mBgHandler);
    }

    private void unregisterForSystemBroadcasts() {
        this.mContext.unregisterReceiver(this.mBroadcastReceiver);
        this.mContext.unregisterReceiver(this.mBootReceiver);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void forEachTracker(Consumer<BaseAppStateTracker> sink) {
        int size = this.mAppStateTrackers.size();
        for (int i = 0; i < size; i++) {
            sink.accept(this.mAppStateTrackers.get(i));
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onUserAdded(int userId) {
        int size = this.mAppStateTrackers.size();
        for (int i = 0; i < size; i++) {
            this.mAppStateTrackers.get(i).onUserAdded(userId);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onUserStarted(int userId) {
        refreshAppRestrictionLevelForUser(userId, 1024, 2);
        int size = this.mAppStateTrackers.size();
        for (int i = 0; i < size; i++) {
            this.mAppStateTrackers.get(i).onUserStarted(userId);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onUserStopped(int userId) {
        int size = this.mAppStateTrackers.size();
        for (int i = 0; i < size; i++) {
            this.mAppStateTrackers.get(i).onUserStopped(userId);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onUserRemoved(int userId) {
        int size = this.mAppStateTrackers.size();
        for (int i = 0; i < size; i++) {
            this.mAppStateTrackers.get(i).onUserRemoved(userId);
        }
        this.mRestrictionSettings.removeUser(userId);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onUidAdded(int uid) {
        refreshAppRestrictionLevelForUid(uid, 1536, 0, false);
        int size = this.mAppStateTrackers.size();
        for (int i = 0; i < size; i++) {
            this.mAppStateTrackers.get(i).onUidAdded(uid);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onPackageRemoved(String pkgName, int uid) {
        this.mRestrictionSettings.removePackage(pkgName, uid);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onUidRemoved(int uid) {
        int size = this.mAppStateTrackers.size();
        for (int i = 0; i < size; i++) {
            this.mAppStateTrackers.get(i).onUidRemoved(uid);
        }
        this.mRestrictionSettings.removeUid(uid);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onLockedBootCompleted() {
        int size = this.mAppStateTrackers.size();
        for (int i = 0; i < size; i++) {
            this.mAppStateTrackers.get(i).onLockedBootCompleted();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isBgAutoRestrictedBucketFeatureFlagEnabled() {
        return this.mConstantsObserver.mBgAutoRestrictedBucket;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onPropertiesChanged(String name) {
        int size = this.mAppStateTrackers.size();
        for (int i = 0; i < size; i++) {
            this.mAppStateTrackers.get(i).onPropertiesChanged(name);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onUserInteractionStarted(String packageName, int userId) {
        int uid = this.mInjector.getPackageManagerInternal().getPackageUid(packageName, 819200L, userId);
        int size = this.mAppStateTrackers.size();
        for (int i = 0; i < size; i++) {
            this.mAppStateTrackers.get(i).onUserInteractionStarted(packageName, uid);
        }
    }
}
