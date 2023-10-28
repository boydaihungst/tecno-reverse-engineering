package com.android.server.wm;

import android.graphics.Bitmap;
import android.os.Process;
import android.os.SystemClock;
import android.util.ArraySet;
import android.util.AtomicFile;
import android.util.Slog;
import android.window.TaskSnapshot;
import com.android.server.LocalServices;
import com.android.server.pm.UserManagerInternal;
import com.android.server.wm.nano.WindowManagerProtos;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Arrays;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class TaskSnapshotPersister {
    private static final String BITMAP_EXTENSION = ".jpg";
    private static final long DELAY_MS = 100;
    private static final String LOW_RES_FILE_POSTFIX = "_reduced";
    private static final int MAX_STORE_QUEUE_DEPTH = 2;
    private static final String PROTO_EXTENSION = ".proto";
    private static final int QUALITY = 95;
    private static final String SNAPSHOTS_DIRNAME = "snapshots";
    private static final String TAG = "WindowManager";
    private final DirectoryResolver mDirectoryResolver;
    private boolean mEnableLowResSnapshots;
    private final float mLowResScaleFactor;
    private boolean mPaused;
    private boolean mQueueIdling;
    private boolean mStarted;
    private final boolean mUse16BitFormat;
    private final ArrayDeque<WriteQueueItem> mWriteQueue = new ArrayDeque<>();
    private final ArrayDeque<StoreWriteQueueItem> mStoreQueueItems = new ArrayDeque<>();
    private final Object mLock = new Object();
    private final ArraySet<Integer> mPersistedTaskIdsSinceLastRemoveObsolete = new ArraySet<>();
    private Thread mPersister = new Thread("TaskSnapshotPersister") { // from class: com.android.server.wm.TaskSnapshotPersister.1
        @Override // java.lang.Thread, java.lang.Runnable
        public void run() {
            WriteQueueItem next;
            Process.setThreadPriority(10);
            while (true) {
                boolean isReadyToWrite = false;
                synchronized (TaskSnapshotPersister.this.mLock) {
                    if (TaskSnapshotPersister.this.mPaused) {
                        next = null;
                    } else {
                        next = (WriteQueueItem) TaskSnapshotPersister.this.mWriteQueue.poll();
                        if (next != null) {
                            if (next.isReady()) {
                                isReadyToWrite = true;
                                next.onDequeuedLocked();
                            } else {
                                TaskSnapshotPersister.this.mWriteQueue.addLast(next);
                            }
                        }
                    }
                }
                if (next != null) {
                    if (isReadyToWrite) {
                        next.write();
                    }
                    SystemClock.sleep(TaskSnapshotPersister.DELAY_MS);
                }
                synchronized (TaskSnapshotPersister.this.mLock) {
                    boolean writeQueueEmpty = TaskSnapshotPersister.this.mWriteQueue.isEmpty();
                    if (writeQueueEmpty || TaskSnapshotPersister.this.mPaused) {
                        try {
                            TaskSnapshotPersister.this.mQueueIdling = writeQueueEmpty;
                            TaskSnapshotPersister.this.mLock.wait();
                            TaskSnapshotPersister.this.mQueueIdling = false;
                        } catch (InterruptedException e) {
                        }
                    }
                }
            }
        }
    };
    private final UserManagerInternal mUserManagerInternal = (UserManagerInternal) LocalServices.getService(UserManagerInternal.class);

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public interface DirectoryResolver {
        File getSystemDirectoryForUser(int i);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public TaskSnapshotPersister(WindowManagerService service, DirectoryResolver resolver) {
        this.mDirectoryResolver = resolver;
        float highResTaskSnapshotScale = service.mContext.getResources().getFloat(17105079);
        float lowResTaskSnapshotScale = service.mContext.getResources().getFloat(17105085);
        if (lowResTaskSnapshotScale < 0.0f || 1.0f <= lowResTaskSnapshotScale) {
            throw new RuntimeException("Low-res scale must be between 0 and 1");
        }
        if (highResTaskSnapshotScale <= 0.0f || 1.0f < highResTaskSnapshotScale) {
            throw new RuntimeException("High-res scale must be between 0 and 1");
        }
        if (highResTaskSnapshotScale <= lowResTaskSnapshotScale) {
            throw new RuntimeException("High-res scale must be greater than low-res scale");
        }
        if (lowResTaskSnapshotScale > 0.0f) {
            this.mLowResScaleFactor = lowResTaskSnapshotScale / highResTaskSnapshotScale;
            this.mEnableLowResSnapshots = true;
        } else {
            this.mLowResScaleFactor = 0.0f;
            this.mEnableLowResSnapshots = false;
        }
        this.mUse16BitFormat = service.mContext.getResources().getBoolean(17891807);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void start() {
        if (!this.mStarted) {
            this.mStarted = true;
            this.mPersister.start();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void persistSnapshot(int taskId, int userId, TaskSnapshot snapshot) {
        synchronized (this.mLock) {
            this.mPersistedTaskIdsSinceLastRemoveObsolete.add(Integer.valueOf(taskId));
            sendToQueueLocked(new StoreWriteQueueItem(taskId, userId, snapshot));
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onTaskRemovedFromRecents(int taskId, int userId) {
        synchronized (this.mLock) {
            this.mPersistedTaskIdsSinceLastRemoveObsolete.remove(Integer.valueOf(taskId));
            sendToQueueLocked(new DeleteWriteQueueItem(taskId, userId));
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void removeObsoleteFiles(ArraySet<Integer> persistentTaskIds, int[] runningUserIds) {
        synchronized (this.mLock) {
            this.mPersistedTaskIdsSinceLastRemoveObsolete.clear();
            sendToQueueLocked(new RemoveObsoleteFilesQueueItem(persistentTaskIds, runningUserIds));
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setPaused(boolean paused) {
        synchronized (this.mLock) {
            this.mPaused = paused;
            if (!paused) {
                this.mLock.notifyAll();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean enableLowResSnapshots() {
        return this.mEnableLowResSnapshots;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean use16BitFormat() {
        return this.mUse16BitFormat;
    }

    void waitForQueueEmpty() {
        while (true) {
            synchronized (this.mLock) {
                if (this.mWriteQueue.isEmpty() && this.mQueueIdling) {
                    return;
                }
            }
            SystemClock.sleep(DELAY_MS);
        }
    }

    private void sendToQueueLocked(WriteQueueItem item) {
        this.mWriteQueue.offer(item);
        item.onQueuedLocked();
        ensureStoreQueueDepthLocked();
        if (!this.mPaused) {
            this.mLock.notifyAll();
        }
    }

    private void ensureStoreQueueDepthLocked() {
        while (this.mStoreQueueItems.size() > 2) {
            StoreWriteQueueItem item = this.mStoreQueueItems.poll();
            this.mWriteQueue.remove(item);
            Slog.i("WindowManager", "Queue is too deep! Purged item with taskid=" + item.mTaskId);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public File getDirectory(int userId) {
        return new File(this.mDirectoryResolver.getSystemDirectoryForUser(userId), SNAPSHOTS_DIRNAME);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public File getProtoFile(int taskId, int userId) {
        return new File(getDirectory(userId), taskId + PROTO_EXTENSION);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public File getHighResolutionBitmapFile(int taskId, int userId) {
        return new File(getDirectory(userId), taskId + BITMAP_EXTENSION);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public File getLowResolutionBitmapFile(int taskId, int userId) {
        return new File(getDirectory(userId), taskId + LOW_RES_FILE_POSTFIX + BITMAP_EXTENSION);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean createDirectory(int userId) {
        File dir = getDirectory(userId);
        return dir.exists() || dir.mkdir();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void deleteSnapshot(int taskId, int userId) {
        File protoFile = getProtoFile(taskId, userId);
        File bitmapLowResFile = getLowResolutionBitmapFile(taskId, userId);
        protoFile.delete();
        if (bitmapLowResFile.exists()) {
            bitmapLowResFile.delete();
        }
        File bitmapFile = getHighResolutionBitmapFile(taskId, userId);
        if (bitmapFile.exists()) {
            bitmapFile.delete();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public abstract class WriteQueueItem {
        abstract void write();

        private WriteQueueItem() {
        }

        boolean isReady() {
            return true;
        }

        void onQueuedLocked() {
        }

        void onDequeuedLocked() {
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class StoreWriteQueueItem extends WriteQueueItem {
        private final TaskSnapshot mSnapshot;
        private final int mTaskId;
        private final int mUserId;

        StoreWriteQueueItem(int taskId, int userId, TaskSnapshot snapshot) {
            super();
            this.mTaskId = taskId;
            this.mUserId = userId;
            this.mSnapshot = snapshot;
        }

        @Override // com.android.server.wm.TaskSnapshotPersister.WriteQueueItem
        void onQueuedLocked() {
            TaskSnapshotPersister.this.mStoreQueueItems.offer(this);
        }

        @Override // com.android.server.wm.TaskSnapshotPersister.WriteQueueItem
        void onDequeuedLocked() {
            TaskSnapshotPersister.this.mStoreQueueItems.remove(this);
        }

        @Override // com.android.server.wm.TaskSnapshotPersister.WriteQueueItem
        boolean isReady() {
            return TaskSnapshotPersister.this.mUserManagerInternal.isUserUnlocked(this.mUserId);
        }

        @Override // com.android.server.wm.TaskSnapshotPersister.WriteQueueItem
        void write() {
            if (!TaskSnapshotPersister.this.createDirectory(this.mUserId)) {
                Slog.e("WindowManager", "Unable to create snapshot directory for user dir=" + TaskSnapshotPersister.this.getDirectory(this.mUserId));
            }
            boolean failed = false;
            if (!writeProto()) {
                failed = true;
            }
            if (!writeBuffer()) {
                failed = true;
            }
            if (failed) {
                TaskSnapshotPersister.this.deleteSnapshot(this.mTaskId, this.mUserId);
            }
        }

        boolean writeProto() {
            WindowManagerProtos.TaskSnapshotProto proto = new WindowManagerProtos.TaskSnapshotProto();
            proto.orientation = this.mSnapshot.getOrientation();
            proto.rotation = this.mSnapshot.getRotation();
            proto.taskWidth = this.mSnapshot.getTaskSize().x;
            proto.taskHeight = this.mSnapshot.getTaskSize().y;
            proto.insetLeft = this.mSnapshot.getContentInsets().left;
            proto.insetTop = this.mSnapshot.getContentInsets().top;
            proto.insetRight = this.mSnapshot.getContentInsets().right;
            proto.insetBottom = this.mSnapshot.getContentInsets().bottom;
            proto.letterboxInsetLeft = this.mSnapshot.getLetterboxInsets().left;
            proto.letterboxInsetTop = this.mSnapshot.getLetterboxInsets().top;
            proto.letterboxInsetRight = this.mSnapshot.getLetterboxInsets().right;
            proto.letterboxInsetBottom = this.mSnapshot.getLetterboxInsets().bottom;
            proto.isRealSnapshot = this.mSnapshot.isRealSnapshot();
            proto.windowingMode = this.mSnapshot.getWindowingMode();
            proto.appearance = this.mSnapshot.getAppearance();
            proto.isTranslucent = this.mSnapshot.isTranslucent();
            proto.topActivityComponent = this.mSnapshot.getTopActivityComponent().flattenToString();
            proto.id = this.mSnapshot.getId();
            byte[] bytes = WindowManagerProtos.TaskSnapshotProto.toByteArray(proto);
            File file = TaskSnapshotPersister.this.getProtoFile(this.mTaskId, this.mUserId);
            AtomicFile atomicFile = new AtomicFile(file);
            FileOutputStream fos = null;
            try {
                fos = atomicFile.startWrite();
                fos.write(bytes);
                atomicFile.finishWrite(fos);
                return true;
            } catch (IOException e) {
                atomicFile.failWrite(fos);
                Slog.e("WindowManager", "Unable to open " + file + " for persisting. " + e);
                return false;
            }
        }

        boolean writeBuffer() {
            if (TaskSnapshotController.isInvalidHardwareBuffer(this.mSnapshot.getHardwareBuffer())) {
                Slog.e("WindowManager", "Invalid task snapshot hw buffer, taskId=" + this.mTaskId);
                return false;
            }
            Bitmap bitmap = Bitmap.wrapHardwareBuffer(this.mSnapshot.getHardwareBuffer(), this.mSnapshot.getColorSpace());
            if (bitmap == null) {
                Slog.e("WindowManager", "Invalid task snapshot hw bitmap");
                return false;
            }
            Bitmap swBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, false);
            File file = TaskSnapshotPersister.this.getHighResolutionBitmapFile(this.mTaskId, this.mUserId);
            try {
                FileOutputStream fos = new FileOutputStream(file);
                swBitmap.compress(Bitmap.CompressFormat.JPEG, 95, fos);
                fos.close();
                if (!TaskSnapshotPersister.this.mEnableLowResSnapshots) {
                    swBitmap.recycle();
                    return true;
                }
                Bitmap lowResBitmap = Bitmap.createScaledBitmap(swBitmap, (int) (bitmap.getWidth() * TaskSnapshotPersister.this.mLowResScaleFactor), (int) (bitmap.getHeight() * TaskSnapshotPersister.this.mLowResScaleFactor), true);
                swBitmap.recycle();
                File lowResFile = TaskSnapshotPersister.this.getLowResolutionBitmapFile(this.mTaskId, this.mUserId);
                try {
                    FileOutputStream lowResFos = new FileOutputStream(lowResFile);
                    lowResBitmap.compress(Bitmap.CompressFormat.JPEG, 95, lowResFos);
                    lowResFos.close();
                    lowResBitmap.recycle();
                    return true;
                } catch (IOException e) {
                    Slog.e("WindowManager", "Unable to open " + lowResFile + " for persisting.", e);
                    return false;
                }
            } catch (IOException e2) {
                Slog.e("WindowManager", "Unable to open " + file + " for persisting.", e2);
                return false;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class DeleteWriteQueueItem extends WriteQueueItem {
        private final int mTaskId;
        private final int mUserId;

        DeleteWriteQueueItem(int taskId, int userId) {
            super();
            this.mTaskId = taskId;
            this.mUserId = userId;
        }

        @Override // com.android.server.wm.TaskSnapshotPersister.WriteQueueItem
        void write() {
            TaskSnapshotPersister.this.deleteSnapshot(this.mTaskId, this.mUserId);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public class RemoveObsoleteFilesQueueItem extends WriteQueueItem {
        private final ArraySet<Integer> mPersistentTaskIds;
        private final int[] mRunningUserIds;

        RemoveObsoleteFilesQueueItem(ArraySet<Integer> persistentTaskIds, int[] runningUserIds) {
            super();
            this.mPersistentTaskIds = new ArraySet<>(persistentTaskIds);
            this.mRunningUserIds = Arrays.copyOf(runningUserIds, runningUserIds.length);
        }

        @Override // com.android.server.wm.TaskSnapshotPersister.WriteQueueItem
        void write() {
            ArraySet<Integer> newPersistedTaskIds;
            int[] iArr;
            synchronized (TaskSnapshotPersister.this.mLock) {
                newPersistedTaskIds = new ArraySet<>((ArraySet<Integer>) TaskSnapshotPersister.this.mPersistedTaskIdsSinceLastRemoveObsolete);
            }
            for (int userId : this.mRunningUserIds) {
                File dir = TaskSnapshotPersister.this.getDirectory(userId);
                String[] files = dir.list();
                if (files != null) {
                    for (String file : files) {
                        int taskId = getTaskId(file);
                        if (!this.mPersistentTaskIds.contains(Integer.valueOf(taskId)) && !newPersistedTaskIds.contains(Integer.valueOf(taskId))) {
                            new File(dir, file).delete();
                        }
                    }
                }
            }
        }

        int getTaskId(String fileName) {
            int end;
            if ((fileName.endsWith(TaskSnapshotPersister.PROTO_EXTENSION) || fileName.endsWith(TaskSnapshotPersister.BITMAP_EXTENSION)) && (end = fileName.lastIndexOf(46)) != -1) {
                String name = fileName.substring(0, end);
                if (name.endsWith(TaskSnapshotPersister.LOW_RES_FILE_POSTFIX)) {
                    name = name.substring(0, name.length() - TaskSnapshotPersister.LOW_RES_FILE_POSTFIX.length());
                }
                try {
                    return Integer.parseInt(name);
                } catch (NumberFormatException e) {
                    return -1;
                }
            }
            return -1;
        }
    }
}
