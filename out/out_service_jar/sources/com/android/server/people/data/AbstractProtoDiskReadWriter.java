package com.android.server.people.data;

import android.util.ArrayMap;
import android.util.AtomicFile;
import android.util.Slog;
import android.util.proto.ProtoInputStream;
import android.util.proto.ProtoOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public abstract class AbstractProtoDiskReadWriter<T> {
    private static final long DEFAULT_DISK_WRITE_DELAY = 120000;
    private static final long SHUTDOWN_DISK_WRITE_TIMEOUT = 5000;
    private static final String TAG = AbstractProtoDiskReadWriter.class.getSimpleName();
    private final File mRootDir;
    private final ScheduledExecutorService mScheduledExecutorService;
    private Map<String, T> mScheduledFileDataMap = new ArrayMap();
    private ScheduledFuture<?> mScheduledFuture;

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public interface ProtoStreamReader<T> {
        T read(ProtoInputStream protoInputStream);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public interface ProtoStreamWriter<T> {
        void write(ProtoOutputStream protoOutputStream, T t);
    }

    abstract ProtoStreamReader<T> protoStreamReader();

    abstract ProtoStreamWriter<T> protoStreamWriter();

    /* JADX INFO: Access modifiers changed from: package-private */
    public AbstractProtoDiskReadWriter(File rootDir, ScheduledExecutorService scheduledExecutorService) {
        this.mRootDir = rootDir;
        this.mScheduledExecutorService = scheduledExecutorService;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void delete(String fileName) {
        synchronized (this) {
            this.mScheduledFileDataMap.remove(fileName);
        }
        File file = getFile(fileName);
        if (file.exists() && !file.delete()) {
            Slog.e(TAG, "Failed to delete file: " + file.getPath());
        }
    }

    void writeTo(String fileName, T data) {
        File file = getFile(fileName);
        AtomicFile atomicFile = new AtomicFile(file);
        try {
            FileOutputStream fileOutputStream = atomicFile.startWrite();
            try {
                ProtoOutputStream protoOutputStream = new ProtoOutputStream(fileOutputStream);
                protoStreamWriter().write(protoOutputStream, data);
                protoOutputStream.flush();
                atomicFile.finishWrite(fileOutputStream);
                fileOutputStream = null;
            } finally {
                atomicFile.failWrite(fileOutputStream);
            }
        } catch (IOException e) {
            Slog.e(TAG, "Failed to write to protobuf file.", e);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public T read(final String fileName) {
        File[] files = this.mRootDir.listFiles(new FileFilter() { // from class: com.android.server.people.data.AbstractProtoDiskReadWriter$$ExternalSyntheticLambda0
            @Override // java.io.FileFilter
            public final boolean accept(File file) {
                return AbstractProtoDiskReadWriter.lambda$read$0(fileName, file);
            }
        });
        if (files == null || files.length == 0) {
            return null;
        }
        if (files.length > 1) {
            Slog.w(TAG, "Found multiple files with the same name: " + Arrays.toString(files));
        }
        return parseFile(files[0]);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$read$0(String fileName, File pathname) {
        return pathname.isFile() && pathname.getName().equals(fileName);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public synchronized void scheduleSave(String fileName, T data) {
        this.mScheduledFileDataMap.put(fileName, data);
        if (this.mScheduledExecutorService.isShutdown()) {
            Slog.e(TAG, "Worker is shutdown, failed to schedule data saving.");
        } else if (this.mScheduledFuture != null) {
        } else {
            this.mScheduledFuture = this.mScheduledExecutorService.schedule(new AbstractProtoDiskReadWriter$$ExternalSyntheticLambda1(this), 120000L, TimeUnit.MILLISECONDS);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void saveImmediately(String fileName, T data) {
        synchronized (this) {
            this.mScheduledFileDataMap.put(fileName, data);
        }
        triggerScheduledFlushEarly();
    }

    private void triggerScheduledFlushEarly() {
        synchronized (this) {
            if (!this.mScheduledFileDataMap.isEmpty() && !this.mScheduledExecutorService.isShutdown()) {
                ScheduledFuture<?> scheduledFuture = this.mScheduledFuture;
                if (scheduledFuture != null) {
                    scheduledFuture.cancel(true);
                }
                Future<?> future = this.mScheduledExecutorService.submit(new AbstractProtoDiskReadWriter$$ExternalSyntheticLambda1(this));
                try {
                    future.get(SHUTDOWN_DISK_WRITE_TIMEOUT, TimeUnit.MILLISECONDS);
                } catch (InterruptedException | ExecutionException | TimeoutException e) {
                    Slog.e(TAG, "Failed to save data immediately.", e);
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public synchronized void flushScheduledData() {
        if (this.mScheduledFileDataMap.isEmpty()) {
            this.mScheduledFuture = null;
            return;
        }
        for (String fileName : this.mScheduledFileDataMap.keySet()) {
            T data = this.mScheduledFileDataMap.get(fileName);
            writeTo(fileName, data);
        }
        this.mScheduledFileDataMap.clear();
        this.mScheduledFuture = null;
    }

    private T parseFile(File file) {
        AtomicFile atomicFile = new AtomicFile(file);
        try {
            FileInputStream fileInputStream = atomicFile.openRead();
            ProtoInputStream protoInputStream = new ProtoInputStream(fileInputStream);
            T read = protoStreamReader().read(protoInputStream);
            if (fileInputStream != null) {
                fileInputStream.close();
            }
            return read;
        } catch (IOException e) {
            Slog.e(TAG, "Failed to parse protobuf file.", e);
            return null;
        }
    }

    private File getFile(String fileName) {
        return new File(this.mRootDir, fileName);
    }
}
