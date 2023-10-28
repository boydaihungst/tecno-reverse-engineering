package com.android.server.powerstats;

import android.content.Context;
import android.util.Slog;
import com.android.internal.util.FileRotator;
import com.android.server.job.controllers.JobStatus;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.locks.ReentrantLock;
/* loaded from: classes2.dex */
public class PowerStatsDataStorage {
    private static final long DELETE_AGE_MILLIS = 172800000;
    private static final long MILLISECONDS_PER_HOUR = 3600000;
    private static final long ROTATE_AGE_MILLIS = 14400000;
    private static final String TAG = PowerStatsDataStorage.class.getSimpleName();
    private final File mDataStorageDir;
    private final String mDataStorageFilename;
    private final FileRotator mFileRotator;
    private final ReentrantLock mLock = new ReentrantLock();

    /* loaded from: classes2.dex */
    public interface DataElementReadCallback {
        void onReadDataElement(byte[] bArr);
    }

    /* loaded from: classes2.dex */
    private static class DataElement {
        private static final int LENGTH_FIELD_WIDTH = 4;
        private static final int MAX_DATA_ELEMENT_SIZE = 1000;
        private byte[] mData;

        /* JADX INFO: Access modifiers changed from: private */
        public byte[] toByteArray() throws IOException {
            ByteArrayOutputStream data = new ByteArrayOutputStream();
            data.write(ByteBuffer.allocate(4).putInt(this.mData.length).array());
            data.write(this.mData);
            return data.toByteArray();
        }

        protected byte[] getData() {
            return this.mData;
        }

        private DataElement(byte[] data) {
            this.mData = data;
        }

        private DataElement(InputStream in) throws IOException {
            byte[] lengthBytes = new byte[4];
            int bytesRead = in.read(lengthBytes);
            this.mData = new byte[0];
            if (bytesRead == 4) {
                int length = ByteBuffer.wrap(lengthBytes).getInt();
                if (length > 0 && length < 1000) {
                    byte[] bArr = new byte[length];
                    this.mData = bArr;
                    int bytesRead2 = in.read(bArr);
                    if (bytesRead2 != length) {
                        throw new IOException("Invalid bytes read, expected: " + length + ", actual: " + bytesRead2);
                    }
                    return;
                }
                throw new IOException("DataElement size is invalid: " + length);
            }
            throw new IOException("Did not read 4 bytes (" + bytesRead + ")");
        }
    }

    /* loaded from: classes2.dex */
    private static class DataReader implements FileRotator.Reader {
        private DataElementReadCallback mCallback;

        DataReader(DataElementReadCallback callback) {
            this.mCallback = callback;
        }

        public void read(InputStream in) throws IOException {
            while (in.available() > 0) {
                DataElement dataElement = new DataElement(in);
                this.mCallback.onReadDataElement(dataElement.getData());
            }
        }
    }

    /* loaded from: classes2.dex */
    private static class DataRewriter implements FileRotator.Rewriter {
        byte[] mActiveFileData = new byte[0];
        byte[] mNewData;

        DataRewriter(byte[] data) {
            this.mNewData = data;
        }

        public void reset() {
        }

        public void read(InputStream in) throws IOException {
            byte[] bArr = new byte[in.available()];
            this.mActiveFileData = bArr;
            in.read(bArr);
        }

        public boolean shouldWrite() {
            return true;
        }

        public void write(OutputStream out) throws IOException {
            out.write(this.mActiveFileData);
            out.write(this.mNewData);
        }
    }

    public PowerStatsDataStorage(Context context, File dataStoragePath, String dataStorageFilename) {
        this.mDataStorageDir = dataStoragePath;
        this.mDataStorageFilename = dataStorageFilename;
        if (!dataStoragePath.exists() && !dataStoragePath.mkdirs()) {
            Slog.wtf(TAG, "mDataStorageDir does not exist: " + dataStoragePath.getPath());
            this.mFileRotator = null;
            return;
        }
        File[] files = dataStoragePath.listFiles();
        for (int i = 0; i < files.length; i++) {
            int versionDot = this.mDataStorageFilename.lastIndexOf(46);
            String beforeVersionDot = this.mDataStorageFilename.substring(0, versionDot);
            if (files[i].getName().startsWith(beforeVersionDot) && !files[i].getName().startsWith(this.mDataStorageFilename)) {
                files[i].delete();
            }
        }
        this.mFileRotator = new FileRotator(this.mDataStorageDir, this.mDataStorageFilename, 14400000L, (long) DELETE_AGE_MILLIS);
    }

    public void write(byte[] data) {
        if (data != null && data.length > 0) {
            this.mLock.lock();
            long currentTimeMillis = System.currentTimeMillis();
            try {
                DataElement dataElement = new DataElement(data);
                this.mFileRotator.rewriteActive(new DataRewriter(dataElement.toByteArray()), currentTimeMillis);
                this.mFileRotator.maybeRotate(currentTimeMillis);
            } catch (IOException e) {
                Slog.e(TAG, "Failed to write to on-device storage: " + e);
            }
            this.mLock.unlock();
        }
    }

    public void read(DataElementReadCallback callback) throws IOException {
        this.mFileRotator.readMatching(new DataReader(callback), Long.MIN_VALUE, (long) JobStatus.NO_LATEST_RUNTIME);
    }

    public void deleteLogs() {
        File[] files = this.mDataStorageDir.listFiles();
        for (int i = 0; i < files.length; i++) {
            int versionDot = this.mDataStorageFilename.lastIndexOf(46);
            String beforeVersionDot = this.mDataStorageFilename.substring(0, versionDot);
            if (files[i].getName().startsWith(beforeVersionDot)) {
                files[i].delete();
            }
        }
    }
}
