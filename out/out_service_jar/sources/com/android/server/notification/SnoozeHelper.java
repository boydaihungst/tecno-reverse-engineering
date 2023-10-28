package com.android.server.notification;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.audio.common.V2_0.AudioFormat;
import android.net.Uri;
import android.os.Binder;
import android.service.notification.StatusBarNotification;
import android.util.ArrayMap;
import android.util.IntArray;
import android.util.Log;
import android.util.Slog;
import android.util.TypedXmlPullParser;
import android.util.TypedXmlSerializer;
import com.android.internal.logging.MetricsLogger;
import com.android.server.notification.ManagedServices;
import com.android.server.notification.NotificationManagerService;
import com.android.server.pm.PackageManagerService;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes2.dex */
public class SnoozeHelper {
    static final int CONCURRENT_SNOOZE_LIMIT = 500;
    static final String EXTRA_KEY = "key";
    private static final String EXTRA_USER_ID = "userId";
    private static final String INDENT = "    ";
    static final int MAX_STRING_LENGTH = 1000;
    private static final String REPOST_SCHEME = "repost";
    private static final int REQUEST_CODE_REPOST = 1;
    private static final String XML_SNOOZED_NOTIFICATION = "notification";
    private static final String XML_SNOOZED_NOTIFICATION_CONTEXT = "context";
    private static final String XML_SNOOZED_NOTIFICATION_CONTEXT_ID = "id";
    private static final String XML_SNOOZED_NOTIFICATION_KEY = "key";
    private static final String XML_SNOOZED_NOTIFICATION_PKG = "pkg";
    private static final String XML_SNOOZED_NOTIFICATION_TIME = "time";
    private static final String XML_SNOOZED_NOTIFICATION_USER_ID = "user-id";
    public static final int XML_SNOOZED_NOTIFICATION_VERSION = 1;
    private static final String XML_SNOOZED_NOTIFICATION_VERSION_LABEL = "version";
    protected static final String XML_TAG_NAME = "snoozed-notifications";
    private AlarmManager mAm;
    private final BroadcastReceiver mBroadcastReceiver;
    private Callback mCallback;
    private final Context mContext;
    private final ManagedServices.UserProfiles mUserProfiles;
    private static final String TAG = "SnoozeHelper";
    private static final boolean DEBUG = Log.isLoggable(TAG, 3);
    private static final String REPOST_ACTION = SnoozeHelper.class.getSimpleName() + ".EVALUATE";
    private ArrayMap<String, ArrayMap<String, NotificationRecord>> mSnoozedNotifications = new ArrayMap<>();
    private final ArrayMap<String, ArrayMap<String, Long>> mPersistedSnoozedNotifications = new ArrayMap<>();
    private final ArrayMap<String, ArrayMap<String, String>> mPersistedSnoozedNotificationsWithContext = new ArrayMap<>();
    private ArrayMap<String, String> mPackages = new ArrayMap<>();
    private ArrayMap<String, Integer> mUsers = new ArrayMap<>();
    private final Object mLock = new Object();

    /* JADX INFO: Access modifiers changed from: protected */
    /* loaded from: classes2.dex */
    public interface Callback {
        void repost(int i, NotificationRecord notificationRecord, boolean z);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public interface Inserter<T> {
        void insert(T t) throws IOException;
    }

    public SnoozeHelper(Context context, Callback callback, ManagedServices.UserProfiles userProfiles) {
        BroadcastReceiver broadcastReceiver = new BroadcastReceiver() { // from class: com.android.server.notification.SnoozeHelper.1
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent) {
                if (SnoozeHelper.DEBUG) {
                    Slog.d(SnoozeHelper.TAG, "Reposting notification");
                }
                if (SnoozeHelper.REPOST_ACTION.equals(intent.getAction())) {
                    SnoozeHelper.this.repost(intent.getStringExtra("key"), intent.getIntExtra("userId", 0), false);
                }
            }
        };
        this.mBroadcastReceiver = broadcastReceiver;
        this.mContext = context;
        IntentFilter filter = new IntentFilter(REPOST_ACTION);
        filter.addDataScheme(REPOST_SCHEME);
        context.registerReceiver(broadcastReceiver, filter, 2);
        this.mAm = (AlarmManager) context.getSystemService("alarm");
        this.mCallback = callback;
        this.mUserProfiles = userProfiles;
    }

    private String getPkgKey(int userId, String pkg) {
        return userId + "|" + pkg;
    }

    void cleanupPersistedContext(String key) {
        synchronized (this.mLock) {
            int userId = this.mUsers.get(key).intValue();
            String pkg = this.mPackages.get(key);
            removeRecordLocked(pkg, key, Integer.valueOf(userId), this.mPersistedSnoozedNotificationsWithContext);
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public boolean canSnooze(int numberToSnooze) {
        synchronized (this.mLock) {
            if (this.mPackages.size() + numberToSnooze > 500) {
                return false;
            }
            return true;
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public Long getSnoozeTimeForUnpostedNotification(int userId, String pkg, String key) {
        Long time = null;
        synchronized (this.mLock) {
            ArrayMap<String, Long> snoozed = this.mPersistedSnoozedNotifications.get(getPkgKey(userId, pkg));
            if (snoozed != null) {
                time = snoozed.get(getTrimmedString(key));
            }
        }
        if (time == null) {
            return 0L;
        }
        return time;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public String getSnoozeContextForUnpostedNotification(int userId, String pkg, String key) {
        synchronized (this.mLock) {
            ArrayMap<String, String> snoozed = this.mPersistedSnoozedNotificationsWithContext.get(getPkgKey(userId, pkg));
            if (snoozed != null) {
                return snoozed.get(getTrimmedString(key));
            }
            return null;
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public boolean isSnoozed(int userId, String pkg, String key) {
        boolean z;
        synchronized (this.mLock) {
            z = this.mSnoozedNotifications.containsKey(getPkgKey(userId, pkg)) && this.mSnoozedNotifications.get(getPkgKey(userId, pkg)).containsKey(key);
        }
        return z;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public Collection<NotificationRecord> getSnoozed(int userId, String pkg) {
        synchronized (this.mLock) {
            if (this.mSnoozedNotifications.containsKey(getPkgKey(userId, pkg))) {
                return this.mSnoozedNotifications.get(getPkgKey(userId, pkg)).values();
            }
            return Collections.EMPTY_LIST;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ArrayList<NotificationRecord> getNotifications(String pkg, String groupKey, Integer userId) {
        ArrayList<NotificationRecord> records = new ArrayList<>();
        synchronized (this.mLock) {
            ArrayMap<String, NotificationRecord> allRecords = this.mSnoozedNotifications.get(getPkgKey(userId.intValue(), pkg));
            if (allRecords != null) {
                for (int i = 0; i < allRecords.size(); i++) {
                    NotificationRecord r = allRecords.valueAt(i);
                    String currentGroupKey = r.getSbn().getGroup();
                    if (Objects.equals(currentGroupKey, groupKey)) {
                        records.add(r);
                    }
                }
            }
        }
        return records;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public NotificationRecord getNotification(String key) {
        synchronized (this.mLock) {
            if (this.mUsers.containsKey(key) && this.mPackages.containsKey(key)) {
                int userId = this.mUsers.get(key).intValue();
                String pkg = this.mPackages.get(key);
                ArrayMap<String, NotificationRecord> snoozed = this.mSnoozedNotifications.get(getPkgKey(userId, pkg));
                if (snoozed == null) {
                    return null;
                }
                return snoozed.get(key);
            }
            Slog.w(TAG, "Snoozed data sets no longer agree for " + key);
            return null;
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public List<NotificationRecord> getSnoozed() {
        List<NotificationRecord> snoozed;
        synchronized (this.mLock) {
            snoozed = new ArrayList<>();
            for (String userPkgKey : this.mSnoozedNotifications.keySet()) {
                ArrayMap<String, NotificationRecord> snoozedRecords = this.mSnoozedNotifications.get(userPkgKey);
                snoozed.addAll(snoozedRecords.values());
            }
        }
        return snoozed;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void snooze(NotificationRecord record, long duration) {
        String pkg = record.getSbn().getPackageName();
        String key = record.getKey();
        int userId = record.getUser().getIdentifier();
        snooze(record);
        scheduleRepost(pkg, key, userId, duration);
        Long activateAt = Long.valueOf(System.currentTimeMillis() + duration);
        synchronized (this.mLock) {
            storeRecordLocked(pkg, getTrimmedString(key), Integer.valueOf(userId), this.mPersistedSnoozedNotifications, activateAt);
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void snooze(NotificationRecord record, String contextId) {
        int userId = record.getUser().getIdentifier();
        if (contextId != null) {
            synchronized (this.mLock) {
                storeRecordLocked(record.getSbn().getPackageName(), getTrimmedString(record.getKey()), Integer.valueOf(userId), this.mPersistedSnoozedNotificationsWithContext, getTrimmedString(contextId));
            }
        }
        snooze(record);
    }

    private void snooze(NotificationRecord record) {
        int userId = record.getUser().getIdentifier();
        if (DEBUG) {
            Slog.d(TAG, "Snoozing " + record.getKey());
        }
        synchronized (this.mLock) {
            storeRecordLocked(record.getSbn().getPackageName(), record.getKey(), Integer.valueOf(userId), this.mSnoozedNotifications, record);
        }
    }

    private String getTrimmedString(String key) {
        if (key != null && key.length() > 1000) {
            return key.substring(0, 1000);
        }
        return key;
    }

    private <T> void storeRecordLocked(String pkg, String key, Integer userId, ArrayMap<String, ArrayMap<String, T>> targets, T object) {
        this.mPackages.put(key, pkg);
        this.mUsers.put(key, userId);
        ArrayMap<String, T> keyToValue = targets.get(getPkgKey(userId.intValue(), pkg));
        if (keyToValue == null) {
            keyToValue = new ArrayMap<>();
        }
        keyToValue.put(key, object);
        targets.put(getPkgKey(userId.intValue(), pkg), keyToValue);
    }

    private <T> T removeRecordLocked(String pkg, String key, Integer userId, ArrayMap<String, ArrayMap<String, T>> targets) {
        ArrayMap<String, T> keyToValue = targets.get(getPkgKey(userId.intValue(), pkg));
        if (keyToValue == null) {
            return null;
        }
        T object = keyToValue.remove(key);
        if (keyToValue.size() == 0) {
            targets.remove(getPkgKey(userId.intValue(), pkg));
        }
        return object;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public boolean cancel(int userId, String pkg, String tag, int id) {
        synchronized (this.mLock) {
            ArrayMap<String, NotificationRecord> recordsForPkg = this.mSnoozedNotifications.get(getPkgKey(userId, pkg));
            if (recordsForPkg != null) {
                Set<Map.Entry<String, NotificationRecord>> records = recordsForPkg.entrySet();
                for (Map.Entry<String, NotificationRecord> record : records) {
                    StatusBarNotification sbn = record.getValue().getSbn();
                    if (Objects.equals(sbn.getTag(), tag) && sbn.getId() == id) {
                        record.getValue().isCanceled = true;
                        return true;
                    }
                }
            }
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void cancel(int userId, boolean includeCurrentProfiles) {
        synchronized (this.mLock) {
            if (this.mSnoozedNotifications.size() == 0) {
                return;
            }
            IntArray userIds = new IntArray();
            userIds.add(userId);
            if (includeCurrentProfiles) {
                userIds = this.mUserProfiles.getCurrentProfileIds();
            }
            for (ArrayMap<String, NotificationRecord> snoozedRecords : this.mSnoozedNotifications.values()) {
                for (NotificationRecord r : snoozedRecords.values()) {
                    if (userIds.binarySearch(r.getUserId()) >= 0) {
                        r.isCanceled = true;
                    }
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public boolean cancel(int userId, String pkg) {
        synchronized (this.mLock) {
            ArrayMap<String, NotificationRecord> records = this.mSnoozedNotifications.get(getPkgKey(userId, pkg));
            if (records == null) {
                return false;
            }
            int N = records.size();
            for (int i = 0; i < N; i++) {
                records.valueAt(i).isCanceled = true;
            }
            return true;
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void update(int userId, NotificationRecord record) {
        synchronized (this.mLock) {
            ArrayMap<String, NotificationRecord> records = this.mSnoozedNotifications.get(getPkgKey(userId, record.getSbn().getPackageName()));
            if (records == null) {
                return;
            }
            records.put(record.getKey(), record);
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void repost(String key, boolean muteOnReturn) {
        synchronized (this.mLock) {
            Integer userId = this.mUsers.get(key);
            if (userId != null) {
                repost(key, userId.intValue(), muteOnReturn);
            }
        }
    }

    protected void repost(String key, int userId, boolean muteOnReturn) {
        String trimmedKey = getTrimmedString(key);
        synchronized (this.mLock) {
            String pkg = this.mPackages.remove(key);
            this.mUsers.remove(key);
            removeRecordLocked(pkg, trimmedKey, Integer.valueOf(userId), this.mPersistedSnoozedNotifications);
            removeRecordLocked(pkg, trimmedKey, Integer.valueOf(userId), this.mPersistedSnoozedNotificationsWithContext);
            ArrayMap<String, NotificationRecord> records = this.mSnoozedNotifications.get(getPkgKey(userId, pkg));
            if (records == null) {
                return;
            }
            NotificationRecord record = records.remove(key);
            if (record != null && !record.isCanceled) {
                PendingIntent pi = createPendingIntent(record.getSbn().getPackageName(), record.getKey(), userId);
                this.mAm.cancel(pi);
                MetricsLogger.action(record.getLogMaker().setCategory(831).setType(1));
                this.mCallback.repost(userId, record, muteOnReturn);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void repostGroupSummary(String pkg, final int userId, String groupKey) {
        synchronized (this.mLock) {
            ArrayMap<String, NotificationRecord> recordsByKey = this.mSnoozedNotifications.get(getPkgKey(userId, pkg));
            if (recordsByKey == null) {
                return;
            }
            String groupSummaryKey = null;
            int N = recordsByKey.size();
            int i = 0;
            while (true) {
                if (i >= N) {
                    break;
                }
                NotificationRecord potentialGroupSummary = recordsByKey.valueAt(i);
                if (!potentialGroupSummary.getSbn().isGroup() || !potentialGroupSummary.getNotification().isGroupSummary() || !groupKey.equals(potentialGroupSummary.getGroupKey())) {
                    i++;
                } else {
                    groupSummaryKey = potentialGroupSummary.getKey();
                    break;
                }
            }
            if (groupSummaryKey != null) {
                final NotificationRecord record = recordsByKey.remove(groupSummaryKey);
                this.mPackages.remove(groupSummaryKey);
                this.mUsers.remove(groupSummaryKey);
                if (record != null && !record.isCanceled) {
                    Runnable runnable = new Runnable() { // from class: com.android.server.notification.SnoozeHelper$$ExternalSyntheticLambda2
                        @Override // java.lang.Runnable
                        public final void run() {
                            SnoozeHelper.this.m5187x8f807e77(record, userId);
                        }
                    };
                    runnable.run();
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$repostGroupSummary$0$com-android-server-notification-SnoozeHelper  reason: not valid java name */
    public /* synthetic */ void m5187x8f807e77(NotificationRecord record, int userId) {
        MetricsLogger.action(record.getLogMaker().setCategory(831).setType(1));
        this.mCallback.repost(userId, record, false);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void clearData(final int userId, final String pkg) {
        synchronized (this.mLock) {
            ArrayMap<String, NotificationRecord> records = this.mSnoozedNotifications.get(getPkgKey(userId, pkg));
            if (records == null) {
                return;
            }
            for (int i = records.size() - 1; i >= 0; i--) {
                final NotificationRecord r = records.removeAt(i);
                if (r != null) {
                    this.mPackages.remove(r.getKey());
                    this.mUsers.remove(r.getKey());
                    Runnable runnable = new Runnable() { // from class: com.android.server.notification.SnoozeHelper$$ExternalSyntheticLambda3
                        @Override // java.lang.Runnable
                        public final void run() {
                            SnoozeHelper.this.m5186lambda$clearData$1$comandroidservernotificationSnoozeHelper(pkg, r, userId);
                        }
                    };
                    runnable.run();
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$clearData$1$com-android-server-notification-SnoozeHelper  reason: not valid java name */
    public /* synthetic */ void m5186lambda$clearData$1$comandroidservernotificationSnoozeHelper(String pkg, NotificationRecord r, int userId) {
        PendingIntent pi = createPendingIntent(pkg, r.getKey(), userId);
        this.mAm.cancel(pi);
        MetricsLogger.action(r.getLogMaker().setCategory(831).setType(5));
    }

    private PendingIntent createPendingIntent(String pkg, String key, int userId) {
        return PendingIntent.getBroadcast(this.mContext, 1, new Intent(REPOST_ACTION).setPackage(PackageManagerService.PLATFORM_PACKAGE_NAME).setData(new Uri.Builder().scheme(REPOST_SCHEME).appendPath(key).build()).addFlags(268435456).putExtra("key", key).putExtra("userId", userId), AudioFormat.DTS_HD);
    }

    public void scheduleRepostsForPersistedNotifications(long currentTime) {
        synchronized (this.mLock) {
            for (ArrayMap<String, Long> snoozed : this.mPersistedSnoozedNotifications.values()) {
                for (int i = 0; i < snoozed.size(); i++) {
                    String key = snoozed.keyAt(i);
                    Long time = snoozed.valueAt(i);
                    String pkg = this.mPackages.get(key);
                    Integer userId = this.mUsers.get(key);
                    if (time != null && pkg != null && userId != null) {
                        if (time != null && time.longValue() > currentTime) {
                            scheduleRepostAtTime(pkg, key, userId.intValue(), time.longValue());
                        }
                    }
                    Slog.w(TAG, "data out of sync: " + time + "|" + pkg + "|" + userId);
                }
            }
        }
    }

    private void scheduleRepost(String pkg, String key, int userId, long duration) {
        scheduleRepostAtTime(pkg, key, userId, System.currentTimeMillis() + duration);
    }

    private void scheduleRepostAtTime(final String pkg, final String key, final int userId, final long time) {
        Runnable runnable = new Runnable() { // from class: com.android.server.notification.SnoozeHelper$$ExternalSyntheticLambda4
            @Override // java.lang.Runnable
            public final void run() {
                SnoozeHelper.this.m5188xa14b0b05(pkg, key, userId, time);
            }
        };
        runnable.run();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$scheduleRepostAtTime$2$com-android-server-notification-SnoozeHelper  reason: not valid java name */
    public /* synthetic */ void m5188xa14b0b05(String pkg, String key, int userId, long time) {
        long identity = Binder.clearCallingIdentity();
        try {
            PendingIntent pi = createPendingIntent(pkg, key, userId);
            this.mAm.cancel(pi);
            if (DEBUG) {
                Slog.d(TAG, "Scheduling evaluate for " + new Date(time));
            }
            this.mAm.setExactAndAllowWhileIdle(0, time, pi);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public void dump(PrintWriter pw, NotificationManagerService.DumpFilter filter) {
        synchronized (this.mLock) {
            pw.println("\n  Snoozed notifications:");
            for (String userPkgKey : this.mSnoozedNotifications.keySet()) {
                pw.print(INDENT);
                pw.println("key: " + userPkgKey);
                Set<String> snoozedKeys = this.mSnoozedNotifications.get(userPkgKey).keySet();
                for (String key : snoozedKeys) {
                    pw.print(INDENT);
                    pw.print(INDENT);
                    pw.print(INDENT);
                    pw.println(key);
                }
            }
            pw.println("\n Pending snoozed notifications");
            for (String userPkgKey2 : this.mPersistedSnoozedNotifications.keySet()) {
                pw.print(INDENT);
                pw.println("key: " + userPkgKey2);
                ArrayMap<String, Long> snoozedRecords = this.mPersistedSnoozedNotifications.get(userPkgKey2);
                if (snoozedRecords != null) {
                    Set<String> snoozedKeys2 = snoozedRecords.keySet();
                    for (String key2 : snoozedKeys2) {
                        pw.print(INDENT);
                        pw.print(INDENT);
                        pw.print(INDENT);
                        pw.print(key2);
                        pw.print(INDENT);
                        pw.println(snoozedRecords.get(key2));
                    }
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void writeXml(final TypedXmlSerializer out) throws IOException {
        synchronized (this.mLock) {
            final long currentTime = System.currentTimeMillis();
            out.startTag((String) null, XML_TAG_NAME);
            writeXml(out, this.mPersistedSnoozedNotifications, XML_SNOOZED_NOTIFICATION, new Inserter() { // from class: com.android.server.notification.SnoozeHelper$$ExternalSyntheticLambda0
                @Override // com.android.server.notification.SnoozeHelper.Inserter
                public final void insert(Object obj) {
                    SnoozeHelper.lambda$writeXml$3(currentTime, out, (Long) obj);
                }
            });
            writeXml(out, this.mPersistedSnoozedNotificationsWithContext, XML_SNOOZED_NOTIFICATION_CONTEXT, new Inserter() { // from class: com.android.server.notification.SnoozeHelper$$ExternalSyntheticLambda1
                @Override // com.android.server.notification.SnoozeHelper.Inserter
                public final void insert(Object obj) {
                    out.attribute((String) null, SnoozeHelper.XML_SNOOZED_NOTIFICATION_CONTEXT_ID, (String) obj);
                }
            });
            out.endTag((String) null, XML_TAG_NAME);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$writeXml$3(long currentTime, TypedXmlSerializer out, Long value) throws IOException {
        if (value.longValue() < currentTime) {
            return;
        }
        out.attributeLong((String) null, XML_SNOOZED_NOTIFICATION_TIME, value.longValue());
    }

    private <T> void writeXml(TypedXmlSerializer out, ArrayMap<String, ArrayMap<String, T>> targets, String tag, Inserter<T> attributeInserter) throws IOException {
        int M = targets.size();
        for (int i = 0; i < M; i++) {
            ArrayMap<String, T> keyToValue = targets.valueAt(i);
            for (int j = 0; j < keyToValue.size(); j++) {
                String key = keyToValue.keyAt(j);
                T value = keyToValue.valueAt(j);
                String pkg = this.mPackages.get(key);
                Integer userId = this.mUsers.get(key);
                if (pkg == null || userId == null) {
                    Slog.w(TAG, "pkg " + pkg + " or user " + userId + " missing for " + key);
                } else {
                    out.startTag((String) null, tag);
                    attributeInserter.insert(value);
                    out.attributeInt((String) null, XML_SNOOZED_NOTIFICATION_VERSION_LABEL, 1);
                    out.attribute((String) null, "key", key);
                    out.attribute((String) null, XML_SNOOZED_NOTIFICATION_PKG, pkg);
                    out.attributeInt((String) null, XML_SNOOZED_NOTIFICATION_USER_ID, userId.intValue());
                    out.endTag((String) null, tag);
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void readXml(TypedXmlPullParser parser, long currentTime) throws XmlPullParserException, IOException {
        while (true) {
            int type = parser.next();
            if (type != 1) {
                String tag = parser.getName();
                if (type != 3 || !XML_TAG_NAME.equals(tag)) {
                    if (type == 2 && (XML_SNOOZED_NOTIFICATION.equals(tag) || tag.equals(XML_SNOOZED_NOTIFICATION_CONTEXT))) {
                        if (parser.getAttributeInt((String) null, XML_SNOOZED_NOTIFICATION_VERSION_LABEL, -1) == 1) {
                            try {
                                String key = parser.getAttributeValue((String) null, "key");
                                String pkg = parser.getAttributeValue((String) null, XML_SNOOZED_NOTIFICATION_PKG);
                                int userId = parser.getAttributeInt((String) null, XML_SNOOZED_NOTIFICATION_USER_ID, -1);
                                if (tag.equals(XML_SNOOZED_NOTIFICATION)) {
                                    Long time = Long.valueOf(parser.getAttributeLong((String) null, XML_SNOOZED_NOTIFICATION_TIME, 0L));
                                    if (time.longValue() > currentTime) {
                                        synchronized (this.mLock) {
                                            storeRecordLocked(pkg, key, Integer.valueOf(userId), this.mPersistedSnoozedNotifications, time);
                                        }
                                    }
                                }
                                if (tag.equals(XML_SNOOZED_NOTIFICATION_CONTEXT)) {
                                    String creationId = parser.getAttributeValue((String) null, XML_SNOOZED_NOTIFICATION_CONTEXT_ID);
                                    synchronized (this.mLock) {
                                        storeRecordLocked(pkg, key, Integer.valueOf(userId), this.mPersistedSnoozedNotificationsWithContext, creationId);
                                    }
                                }
                            } catch (Exception e) {
                                Slog.e(TAG, "Exception in reading snooze data from policy xml", e);
                            }
                        }
                    }
                } else {
                    return;
                }
            } else {
                return;
            }
        }
    }

    void setAlarmManager(AlarmManager am) {
        this.mAm = am;
    }
}
