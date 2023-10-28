package com.android.server.apphibernation;

import android.util.AtomicFile;
import android.util.Slog;
import android.util.proto.ProtoInputStream;
import android.util.proto.ProtoOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class HibernationStateDiskStore<T> {
    private static final long DISK_WRITE_DELAY = 60000;
    private static final String STATES_FILE_NAME = "states";
    private static final String TAG = "HibernationStateDiskStore";
    private final ScheduledExecutorService mExecutorService;
    private ScheduledFuture<?> mFuture;
    private final File mHibernationFile;
    private final ProtoReadWriter<List<T>> mProtoReadWriter;
    private List<T> mScheduledStatesToWrite;

    /* JADX INFO: Access modifiers changed from: package-private */
    public HibernationStateDiskStore(File hibernationDir, ProtoReadWriter<List<T>> readWriter, ScheduledExecutorService executorService) {
        this(hibernationDir, readWriter, executorService, STATES_FILE_NAME);
    }

    HibernationStateDiskStore(File hibernationDir, ProtoReadWriter<List<T>> readWriter, ScheduledExecutorService executorService, String fileName) {
        this.mScheduledStatesToWrite = new ArrayList();
        this.mHibernationFile = new File(hibernationDir, fileName);
        this.mExecutorService = executorService;
        this.mProtoReadWriter = readWriter;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void scheduleWriteHibernationStates(List<T> hibernationStates) {
        synchronized (this) {
            this.mScheduledStatesToWrite = hibernationStates;
            if (this.mExecutorService.isShutdown()) {
                Slog.e(TAG, "Scheduled executor service is shut down.");
            } else if (this.mFuture != null) {
                Slog.i(TAG, "Write already scheduled. Skipping schedule.");
            } else {
                this.mFuture = this.mExecutorService.schedule(new Runnable() { // from class: com.android.server.apphibernation.HibernationStateDiskStore$$ExternalSyntheticLambda0
                    @Override // java.lang.Runnable
                    public final void run() {
                        HibernationStateDiskStore.this.writeHibernationStates();
                    }
                }, 60000L, TimeUnit.MILLISECONDS);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public List<T> readHibernationStates() {
        synchronized (this) {
            if (!this.mHibernationFile.exists()) {
                Slog.i(TAG, "No hibernation file on disk for file " + this.mHibernationFile.getPath());
                return null;
            }
            AtomicFile atomicFile = new AtomicFile(this.mHibernationFile);
            try {
                FileInputStream inputStream = atomicFile.openRead();
                ProtoInputStream protoInputStream = new ProtoInputStream(inputStream);
                return this.mProtoReadWriter.readFromProto(protoInputStream);
            } catch (IOException e) {
                Slog.e(TAG, "Failed to read states protobuf.", e);
                return null;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void writeHibernationStates() {
        synchronized (this) {
            writeStateProto(this.mScheduledStatesToWrite);
            this.mScheduledStatesToWrite.clear();
            this.mFuture = null;
        }
    }

    private void writeStateProto(List<T> states) {
        AtomicFile atomicFile = new AtomicFile(this.mHibernationFile);
        try {
            FileOutputStream fileOutputStream = atomicFile.startWrite();
            try {
                ProtoOutputStream protoOutputStream = new ProtoOutputStream(fileOutputStream);
                this.mProtoReadWriter.writeToProto(protoOutputStream, states);
                protoOutputStream.flush();
                atomicFile.finishWrite(fileOutputStream);
            } catch (Exception e) {
                Slog.e(TAG, "Failed to finish write to states protobuf.", e);
                atomicFile.failWrite(fileOutputStream);
            }
        } catch (IOException e2) {
            Slog.e(TAG, "Failed to start write to states protobuf.", e2);
        }
    }
}
