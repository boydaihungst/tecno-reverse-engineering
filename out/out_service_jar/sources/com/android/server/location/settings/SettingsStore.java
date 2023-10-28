package com.android.server.location.settings;

import android.util.AtomicFile;
import android.util.Log;
import com.android.internal.os.BackgroundThread;
import com.android.internal.util.Preconditions;
import com.android.server.location.LocationManagerService;
import com.android.server.location.settings.SettingsStore.VersionedSettings;
import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.function.Function;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public abstract class SettingsStore<T extends VersionedSettings> {
    private T mCache;
    private final AtomicFile mFile;
    private boolean mInitialized;

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public interface VersionedSettings {
        public static final int VERSION_DOES_NOT_EXIST = Integer.MAX_VALUE;

        int getVersion();
    }

    protected abstract void onChange(T t, T t2);

    protected abstract T read(int i, DataInput dataInput) throws IOException;

    protected abstract void write(DataOutput dataOutput, T t) throws IOException;

    /* JADX INFO: Access modifiers changed from: protected */
    public SettingsStore(File file) {
        this.mFile = new AtomicFile(file);
    }

    public final synchronized void initializeCache() {
        if (!this.mInitialized) {
            if (this.mFile.exists()) {
                try {
                    DataInputStream is = new DataInputStream(this.mFile.openRead());
                    try {
                        T read = read(is.readInt(), is);
                        this.mCache = read;
                        Preconditions.checkState(read.getVersion() < Integer.MAX_VALUE);
                        is.close();
                    } catch (Throwable th) {
                        try {
                            is.close();
                        } catch (Throwable th2) {
                            th.addSuppressed(th2);
                        }
                        throw th;
                    }
                } catch (IOException e) {
                    Log.e(LocationManagerService.TAG, "error reading location settings (" + this.mFile + "), falling back to defaults", e);
                }
            }
            if (this.mCache == null) {
                try {
                    T read2 = read(Integer.MAX_VALUE, new DataInputStream(new ByteArrayInputStream(new byte[0])));
                    this.mCache = read2;
                    Preconditions.checkState(read2.getVersion() < Integer.MAX_VALUE);
                } catch (IOException e2) {
                    throw new AssertionError(e2);
                }
            }
            this.mInitialized = true;
        }
    }

    public final synchronized T get() {
        initializeCache();
        return this.mCache;
    }

    public synchronized void update(Function<T, T> updater) {
        initializeCache();
        T oldSettings = this.mCache;
        T newSettings = (T) Objects.requireNonNull(updater.apply(oldSettings));
        if (oldSettings.equals(newSettings)) {
            return;
        }
        this.mCache = newSettings;
        Preconditions.checkState(newSettings.getVersion() < Integer.MAX_VALUE);
        writeLazily(newSettings);
        onChange(oldSettings, newSettings);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public synchronized void flushFile() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        Executor executor = BackgroundThread.getExecutor();
        Objects.requireNonNull(latch);
        executor.execute(new Runnable() { // from class: com.android.server.location.settings.SettingsStore$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                latch.countDown();
            }
        });
        latch.await();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public synchronized void deleteFile() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        BackgroundThread.getExecutor().execute(new Runnable() { // from class: com.android.server.location.settings.SettingsStore$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                SettingsStore.this.m4538x7b42680(latch);
            }
        });
        latch.await();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$deleteFile$0$com-android-server-location-settings-SettingsStore  reason: not valid java name */
    public /* synthetic */ void m4538x7b42680(CountDownLatch latch) {
        this.mFile.delete();
        latch.countDown();
    }

    private void writeLazily(final T settings) {
        BackgroundThread.getExecutor().execute(new Runnable() { // from class: com.android.server.location.settings.SettingsStore$$ExternalSyntheticLambda2
            @Override // java.lang.Runnable
            public final void run() {
                SettingsStore.this.m4539x6fd57fb0(settings);
            }
        });
    }

    /* JADX DEBUG: Multi-variable search result rejected for r5v0, resolved type: com.android.server.location.settings.SettingsStore$VersionedSettings */
    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: Multi-variable type inference failed */
    /* renamed from: lambda$writeLazily$1$com-android-server-location-settings-SettingsStore  reason: not valid java name */
    public /* synthetic */ void m4539x6fd57fb0(VersionedSettings settings) {
        FileOutputStream os = null;
        try {
            os = this.mFile.startWrite();
            DataOutputStream out = new DataOutputStream(os);
            out.writeInt(settings.getVersion());
            write(out, settings);
            this.mFile.finishWrite(os);
        } catch (IOException e) {
            this.mFile.failWrite(os);
            Log.e(LocationManagerService.TAG, "failure serializing location settings", e);
        } catch (Throwable e2) {
            this.mFile.failWrite(os);
            throw e2;
        }
    }
}
