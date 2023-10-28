package com.android.server.notification;

import android.app.AlarmManager;
import android.app.NotificationHistory;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.audio.common.V2_0.AudioFormat;
import android.net.Uri;
import android.os.Handler;
import android.util.AtomicFile;
import android.util.Slog;
import com.android.server.notification.NotificationHistoryFilter;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
/* loaded from: classes2.dex */
public class NotificationHistoryDatabase {
    private static final int DEFAULT_CURRENT_VERSION = 1;
    private static final String EXTRA_KEY = "key";
    private static final int HISTORY_RETENTION_DAYS = 1;
    private static final int HISTORY_RETENTION_MS = 86400000;
    private static final long INVALID_FILE_TIME_MS = -1;
    private static final int REQUEST_CODE_DELETION = 1;
    private static final String SCHEME_DELETION = "delete";
    private static final String TAG = "NotiHistoryDatabase";
    private static final long WRITE_BUFFER_INTERVAL_MS = 1200000;
    private final AlarmManager mAlarmManager;
    NotificationHistory mBuffer;
    private final Context mContext;
    private int mCurrentVersion;
    private final BroadcastReceiver mFileCleanupReceiver;
    private final Handler mFileWriteHandler;
    private final File mHistoryDir;
    final LinkedList<AtomicFile> mHistoryFiles;
    private final Object mLock = new Object();
    private final File mVersionFile;
    private final WriteBufferRunnable mWriteBufferRunnable;
    private static final boolean DEBUG = NotificationManagerService.DBG;
    private static final String ACTION_HISTORY_DELETION = NotificationHistoryDatabase.class.getSimpleName() + ".CLEANUP";

    public NotificationHistoryDatabase(Context context, Handler fileWriteHandler, File dir) {
        BroadcastReceiver broadcastReceiver = new BroadcastReceiver() { // from class: com.android.server.notification.NotificationHistoryDatabase.1
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent) {
                String action = intent.getAction();
                if (action != null && NotificationHistoryDatabase.ACTION_HISTORY_DELETION.equals(action)) {
                    try {
                        synchronized (NotificationHistoryDatabase.this.mLock) {
                            String filePath = intent.getStringExtra(NotificationHistoryDatabase.EXTRA_KEY);
                            AtomicFile fileToDelete = new AtomicFile(new File(filePath));
                            if (NotificationHistoryDatabase.DEBUG) {
                                Slog.d(NotificationHistoryDatabase.TAG, "Removed " + fileToDelete.getBaseFile().getName());
                            }
                            fileToDelete.delete();
                            NotificationHistoryDatabase.this.removeFilePathFromHistory(filePath);
                        }
                    } catch (Exception e) {
                        Slog.e(NotificationHistoryDatabase.TAG, "Failed to delete notification history file", e);
                    }
                }
            }
        };
        this.mFileCleanupReceiver = broadcastReceiver;
        this.mContext = context;
        this.mAlarmManager = (AlarmManager) context.getSystemService(AlarmManager.class);
        this.mCurrentVersion = 1;
        this.mFileWriteHandler = fileWriteHandler;
        this.mVersionFile = new File(dir, "version");
        this.mHistoryDir = new File(dir, "history");
        this.mHistoryFiles = new LinkedList<>();
        this.mBuffer = new NotificationHistory();
        this.mWriteBufferRunnable = new WriteBufferRunnable();
        IntentFilter deletionFilter = new IntentFilter(ACTION_HISTORY_DELETION);
        deletionFilter.addDataScheme(SCHEME_DELETION);
        context.registerReceiver(broadcastReceiver, deletionFilter, 2);
    }

    public void init() {
        synchronized (this.mLock) {
            try {
                if (!this.mHistoryDir.exists() && !this.mHistoryDir.mkdir()) {
                    throw new IllegalStateException("could not create history directory");
                }
                this.mVersionFile.createNewFile();
            } catch (Exception e) {
                Slog.e(TAG, "could not create needed files", e);
            }
            checkVersionAndBuildLocked();
            indexFilesLocked();
            prune(1, System.currentTimeMillis());
        }
    }

    private void indexFilesLocked() {
        this.mHistoryFiles.clear();
        File[] files = this.mHistoryDir.listFiles();
        if (files == null) {
            return;
        }
        Arrays.sort(files, new Comparator() { // from class: com.android.server.notification.NotificationHistoryDatabase$$ExternalSyntheticLambda0
            @Override // java.util.Comparator
            public final int compare(Object obj, Object obj2) {
                int compare;
                compare = Long.compare(NotificationHistoryDatabase.safeParseLong(((File) obj2).getName()), NotificationHistoryDatabase.safeParseLong(((File) obj).getName()));
                return compare;
            }
        });
        for (File file : files) {
            this.mHistoryFiles.addLast(new AtomicFile(file));
        }
    }

    private void checkVersionAndBuildLocked() {
        int version;
        try {
            BufferedReader reader = new BufferedReader(new FileReader(this.mVersionFile));
            version = Integer.parseInt(reader.readLine());
            reader.close();
        } catch (IOException | NumberFormatException e) {
            version = 0;
        }
        if (version != this.mCurrentVersion && this.mVersionFile.exists()) {
            try {
                BufferedWriter writer = new BufferedWriter(new FileWriter(this.mVersionFile));
                writer.write(Integer.toString(this.mCurrentVersion));
                writer.write("\n");
                writer.flush();
                writer.close();
            } catch (IOException e2) {
                Slog.e(TAG, "Failed to write new version");
                throw new RuntimeException(e2);
            }
        }
    }

    public void forceWriteToDisk() {
        this.mFileWriteHandler.post(this.mWriteBufferRunnable);
    }

    public void onPackageRemoved(String packageName) {
        RemovePackageRunnable rpr = new RemovePackageRunnable(packageName);
        this.mFileWriteHandler.post(rpr);
    }

    public void deleteNotificationHistoryItem(String pkg, long postedTime) {
        RemoveNotificationRunnable rnr = new RemoveNotificationRunnable(pkg, postedTime);
        this.mFileWriteHandler.post(rnr);
    }

    public void deleteConversations(String pkg, Set<String> conversationIds) {
        RemoveConversationRunnable rcr = new RemoveConversationRunnable(pkg, conversationIds);
        this.mFileWriteHandler.post(rcr);
    }

    public void deleteNotificationChannel(String pkg, String channelId) {
        RemoveChannelRunnable rcr = new RemoveChannelRunnable(pkg, channelId);
        this.mFileWriteHandler.post(rcr);
    }

    public void addNotification(NotificationHistory.HistoricalNotification notification) {
        synchronized (this.mLock) {
            this.mBuffer.addNewNotificationToWrite(notification);
            if (this.mBuffer.getHistoryCount() == 1) {
                this.mFileWriteHandler.postDelayed(this.mWriteBufferRunnable, WRITE_BUFFER_INTERVAL_MS);
            }
        }
    }

    public NotificationHistory readNotificationHistory() {
        NotificationHistory notifications;
        synchronized (this.mLock) {
            notifications = new NotificationHistory();
            notifications.addNotificationsToWrite(this.mBuffer);
            Iterator<AtomicFile> it = this.mHistoryFiles.iterator();
            while (it.hasNext()) {
                AtomicFile file = it.next();
                try {
                    readLocked(file, notifications, new NotificationHistoryFilter.Builder().build());
                } catch (Exception e) {
                    Slog.e(TAG, "error reading " + file.getBaseFile().getAbsolutePath(), e);
                }
            }
        }
        return notifications;
    }

    public NotificationHistory readNotificationHistory(String packageName, String channelId, int maxNotifications) {
        NotificationHistory notifications;
        synchronized (this.mLock) {
            notifications = new NotificationHistory();
            Iterator<AtomicFile> it = this.mHistoryFiles.iterator();
            while (it.hasNext()) {
                AtomicFile file = it.next();
                try {
                    readLocked(file, notifications, new NotificationHistoryFilter.Builder().setPackage(packageName).setChannel(packageName, channelId).setMaxNotifications(maxNotifications).build());
                } catch (Exception e) {
                    Slog.e(TAG, "error reading " + file.getBaseFile().getAbsolutePath(), e);
                }
                if (maxNotifications == notifications.getHistoryCount()) {
                    break;
                }
            }
        }
        return notifications;
    }

    public void disableHistory() {
        synchronized (this.mLock) {
            Iterator<AtomicFile> it = this.mHistoryFiles.iterator();
            while (it.hasNext()) {
                AtomicFile file = it.next();
                file.delete();
            }
            this.mHistoryDir.delete();
            this.mHistoryFiles.clear();
        }
    }

    void prune(int retentionDays, long currentTimeMillis) {
        synchronized (this.mLock) {
            GregorianCalendar retentionBoundary = new GregorianCalendar();
            retentionBoundary.setTimeInMillis(currentTimeMillis);
            retentionBoundary.add(5, retentionDays * (-1));
            for (int i = this.mHistoryFiles.size() - 1; i >= 0; i--) {
                AtomicFile currentOldestFile = this.mHistoryFiles.get(i);
                long creationTime = safeParseLong(currentOldestFile.getBaseFile().getName());
                if (DEBUG) {
                    Slog.d(TAG, "File " + currentOldestFile.getBaseFile().getName() + " created on " + creationTime);
                }
                if (creationTime <= retentionBoundary.getTimeInMillis()) {
                    deleteFile(currentOldestFile);
                } else {
                    scheduleDeletion(currentOldestFile.getBaseFile(), creationTime, retentionDays);
                }
            }
        }
    }

    void removeFilePathFromHistory(String filePath) {
        if (filePath == null) {
            return;
        }
        Iterator<AtomicFile> historyFileItr = this.mHistoryFiles.iterator();
        while (historyFileItr.hasNext()) {
            AtomicFile af = historyFileItr.next();
            if (af != null && filePath.equals(af.getBaseFile().getAbsolutePath())) {
                historyFileItr.remove();
                return;
            }
        }
    }

    private void deleteFile(AtomicFile file) {
        if (DEBUG) {
            Slog.d(TAG, "Removed " + file.getBaseFile().getName());
        }
        file.delete();
        removeFilePathFromHistory(file.getBaseFile().getAbsolutePath());
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void scheduleDeletion(File file, long creationTime, int retentionDays) {
        long deletionTime = (HISTORY_RETENTION_MS * retentionDays) + creationTime;
        scheduleDeletion(file, deletionTime);
    }

    private void scheduleDeletion(File file, long deletionTime) {
        if (DEBUG) {
            Slog.d(TAG, "Scheduling deletion for " + file.getName() + " at " + deletionTime);
        }
        PendingIntent pi = PendingIntent.getBroadcast(this.mContext, 1, new Intent(ACTION_HISTORY_DELETION).setData(new Uri.Builder().scheme(SCHEME_DELETION).appendPath(file.getAbsolutePath()).build()).addFlags(268435456).putExtra(EXTRA_KEY, file.getAbsolutePath()), AudioFormat.DTS_HD);
        this.mAlarmManager.setExactAndAllowWhileIdle(0, deletionTime, pi);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void writeLocked(AtomicFile file, NotificationHistory notifications) throws IOException {
        FileOutputStream fos = file.startWrite();
        try {
            NotificationHistoryProtoHelper.write(fos, notifications, this.mCurrentVersion);
            file.finishWrite(fos);
            fos = null;
        } finally {
            file.failWrite(fos);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void readLocked(AtomicFile file, NotificationHistory notificationsOut, NotificationHistoryFilter filter) throws IOException {
        FileInputStream in = null;
        try {
            try {
                in = file.openRead();
                NotificationHistoryProtoHelper.read(in, notificationsOut, filter);
            } catch (FileNotFoundException e) {
                Slog.e(TAG, "Cannot open " + file.getBaseFile().getAbsolutePath(), e);
                throw e;
            }
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }

    public void unregisterFileCleanupReceiver() {
        Context context = this.mContext;
        if (context != null) {
            context.unregisterReceiver(this.mFileCleanupReceiver);
        }
    }

    private static long safeParseLong(String fileName) {
        try {
            return Long.parseLong(fileName);
        } catch (NumberFormatException e) {
            return -1L;
        }
    }

    /* loaded from: classes2.dex */
    final class WriteBufferRunnable implements Runnable {
        WriteBufferRunnable() {
        }

        @Override // java.lang.Runnable
        public void run() {
            long time = System.currentTimeMillis();
            run(time, new AtomicFile(new File(NotificationHistoryDatabase.this.mHistoryDir, String.valueOf(time))));
        }

        void run(long time, AtomicFile file) {
            synchronized (NotificationHistoryDatabase.this.mLock) {
                if (NotificationHistoryDatabase.DEBUG) {
                    Slog.d(NotificationHistoryDatabase.TAG, "WriteBufferRunnable " + file.getBaseFile().getAbsolutePath());
                }
                try {
                    NotificationHistoryDatabase notificationHistoryDatabase = NotificationHistoryDatabase.this;
                    notificationHistoryDatabase.writeLocked(file, notificationHistoryDatabase.mBuffer);
                    NotificationHistoryDatabase.this.mHistoryFiles.addFirst(file);
                    NotificationHistoryDatabase.this.mBuffer = new NotificationHistory();
                    NotificationHistoryDatabase.this.scheduleDeletion(file.getBaseFile(), time, 1);
                } catch (IOException e) {
                    Slog.e(NotificationHistoryDatabase.TAG, "Failed to write buffer to disk. not flushing buffer", e);
                }
            }
        }
    }

    /* loaded from: classes2.dex */
    private final class RemovePackageRunnable implements Runnable {
        private String mPkg;

        public RemovePackageRunnable(String pkg) {
            this.mPkg = pkg;
        }

        @Override // java.lang.Runnable
        public void run() {
            if (NotificationHistoryDatabase.DEBUG) {
                Slog.d(NotificationHistoryDatabase.TAG, "RemovePackageRunnable " + this.mPkg);
            }
            synchronized (NotificationHistoryDatabase.this.mLock) {
                NotificationHistoryDatabase.this.mBuffer.removeNotificationsFromWrite(this.mPkg);
                Iterator<AtomicFile> historyFileItr = NotificationHistoryDatabase.this.mHistoryFiles.iterator();
                while (historyFileItr.hasNext()) {
                    AtomicFile af = historyFileItr.next();
                    try {
                        NotificationHistory notifications = new NotificationHistory();
                        NotificationHistoryDatabase.readLocked(af, notifications, new NotificationHistoryFilter.Builder().build());
                        notifications.removeNotificationsFromWrite(this.mPkg);
                        NotificationHistoryDatabase.this.writeLocked(af, notifications);
                    } catch (Exception e) {
                        Slog.e(NotificationHistoryDatabase.TAG, "Cannot clean up file on pkg removal " + af.getBaseFile().getAbsolutePath(), e);
                    }
                }
            }
        }
    }

    /* loaded from: classes2.dex */
    final class RemoveNotificationRunnable implements Runnable {
        private NotificationHistory mNotificationHistory;
        private String mPkg;
        private long mPostedTime;

        public RemoveNotificationRunnable(String pkg, long postedTime) {
            this.mPkg = pkg;
            this.mPostedTime = postedTime;
        }

        void setNotificationHistory(NotificationHistory nh) {
            this.mNotificationHistory = nh;
        }

        @Override // java.lang.Runnable
        public void run() {
            if (NotificationHistoryDatabase.DEBUG) {
                Slog.d(NotificationHistoryDatabase.TAG, "RemoveNotificationRunnable");
            }
            synchronized (NotificationHistoryDatabase.this.mLock) {
                NotificationHistoryDatabase.this.mBuffer.removeNotificationFromWrite(this.mPkg, this.mPostedTime);
                Iterator<AtomicFile> historyFileItr = NotificationHistoryDatabase.this.mHistoryFiles.iterator();
                while (historyFileItr.hasNext()) {
                    AtomicFile af = historyFileItr.next();
                    try {
                        NotificationHistory notificationHistory = this.mNotificationHistory;
                        if (notificationHistory == null) {
                            notificationHistory = new NotificationHistory();
                        }
                        NotificationHistoryDatabase.readLocked(af, notificationHistory, new NotificationHistoryFilter.Builder().build());
                        if (notificationHistory.removeNotificationFromWrite(this.mPkg, this.mPostedTime)) {
                            NotificationHistoryDatabase.this.writeLocked(af, notificationHistory);
                        }
                    } catch (Exception e) {
                        Slog.e(NotificationHistoryDatabase.TAG, "Cannot clean up file on notification removal " + af.getBaseFile().getName(), e);
                    }
                }
            }
        }
    }

    /* loaded from: classes2.dex */
    final class RemoveConversationRunnable implements Runnable {
        private Set<String> mConversationIds;
        private NotificationHistory mNotificationHistory;
        private String mPkg;

        public RemoveConversationRunnable(String pkg, Set<String> conversationIds) {
            this.mPkg = pkg;
            this.mConversationIds = conversationIds;
        }

        void setNotificationHistory(NotificationHistory nh) {
            this.mNotificationHistory = nh;
        }

        @Override // java.lang.Runnable
        public void run() {
            if (NotificationHistoryDatabase.DEBUG) {
                Slog.d(NotificationHistoryDatabase.TAG, "RemoveConversationRunnable " + this.mPkg + " " + this.mConversationIds);
            }
            synchronized (NotificationHistoryDatabase.this.mLock) {
                NotificationHistoryDatabase.this.mBuffer.removeConversationsFromWrite(this.mPkg, this.mConversationIds);
                Iterator<AtomicFile> historyFileItr = NotificationHistoryDatabase.this.mHistoryFiles.iterator();
                while (historyFileItr.hasNext()) {
                    AtomicFile af = historyFileItr.next();
                    try {
                        NotificationHistory notificationHistory = this.mNotificationHistory;
                        if (notificationHistory == null) {
                            notificationHistory = new NotificationHistory();
                        }
                        NotificationHistoryDatabase.readLocked(af, notificationHistory, new NotificationHistoryFilter.Builder().build());
                        if (notificationHistory.removeConversationsFromWrite(this.mPkg, this.mConversationIds)) {
                            NotificationHistoryDatabase.this.writeLocked(af, notificationHistory);
                        }
                    } catch (Exception e) {
                        Slog.e(NotificationHistoryDatabase.TAG, "Cannot clean up file on conversation removal " + af.getBaseFile().getName(), e);
                    }
                }
            }
        }
    }

    /* loaded from: classes2.dex */
    final class RemoveChannelRunnable implements Runnable {
        private String mChannelId;
        private NotificationHistory mNotificationHistory;
        private String mPkg;

        RemoveChannelRunnable(String pkg, String channelId) {
            this.mPkg = pkg;
            this.mChannelId = channelId;
        }

        void setNotificationHistory(NotificationHistory nh) {
            this.mNotificationHistory = nh;
        }

        @Override // java.lang.Runnable
        public void run() {
            if (NotificationHistoryDatabase.DEBUG) {
                Slog.d(NotificationHistoryDatabase.TAG, "RemoveChannelRunnable");
            }
            synchronized (NotificationHistoryDatabase.this.mLock) {
                NotificationHistoryDatabase.this.mBuffer.removeChannelFromWrite(this.mPkg, this.mChannelId);
                Iterator<AtomicFile> historyFileItr = NotificationHistoryDatabase.this.mHistoryFiles.iterator();
                while (historyFileItr.hasNext()) {
                    AtomicFile af = historyFileItr.next();
                    try {
                        NotificationHistory notificationHistory = this.mNotificationHistory;
                        if (notificationHistory == null) {
                            notificationHistory = new NotificationHistory();
                        }
                        NotificationHistoryDatabase.readLocked(af, notificationHistory, new NotificationHistoryFilter.Builder().build());
                        if (notificationHistory.removeChannelFromWrite(this.mPkg, this.mChannelId)) {
                            NotificationHistoryDatabase.this.writeLocked(af, notificationHistory);
                        }
                    } catch (Exception e) {
                        Slog.e(NotificationHistoryDatabase.TAG, "Cannot clean up file on channel removal " + af.getBaseFile().getName(), e);
                    }
                }
            }
        }
    }
}
