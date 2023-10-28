package android.media.tv.tuner.dvr;

import android.annotation.SystemApi;
import android.media.tv.tuner.TunerUtils;
import android.media.tv.tuner.filter.Filter;
import android.os.ParcelFileDescriptor;
import android.os.Process;
import android.util.Log;
import com.android.internal.util.FrameworkStatsLog;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.concurrent.Executor;
@SystemApi
/* loaded from: classes2.dex */
public class DvrPlayback implements AutoCloseable {
    public static final int PLAYBACK_STATUS_ALMOST_EMPTY = 2;
    public static final int PLAYBACK_STATUS_ALMOST_FULL = 4;
    public static final int PLAYBACK_STATUS_EMPTY = 1;
    public static final int PLAYBACK_STATUS_FULL = 8;
    private static final String TAG = "TvTunerPlayback";
    private static int sInstantId = 0;
    private Executor mExecutor;
    private OnPlaybackStatusChangedListener mListener;
    private long mNativeContext;
    private int mSegmentId;
    private int mUnderflow;
    private final Object mListenerLock = new Object();
    private int mUserId = Process.myUid();

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes2.dex */
    @interface PlaybackStatus {
    }

    private native int nativeAttachFilter(Filter filter);

    private native int nativeClose();

    private native int nativeConfigureDvr(DvrSettings dvrSettings);

    private native int nativeDetachFilter(Filter filter);

    private native int nativeFlushDvr();

    private native long nativeRead(long j);

    private native long nativeRead(byte[] bArr, long j, long j2);

    private native long nativeSeek(long j);

    private native void nativeSetFileDescriptor(int i);

    private native int nativeStartDvr();

    private native int nativeStopDvr();

    private DvrPlayback() {
        this.mSegmentId = 0;
        int i = sInstantId;
        this.mSegmentId = (65535 & i) << 16;
        sInstantId = i + 1;
    }

    public void setListener(Executor executor, OnPlaybackStatusChangedListener listener) {
        synchronized (this.mListenerLock) {
            this.mExecutor = executor;
            this.mListener = listener;
        }
    }

    private void onPlaybackStatusChanged(final int status) {
        if (status == 1) {
            this.mUnderflow++;
        }
        synchronized (this.mListenerLock) {
            Executor executor = this.mExecutor;
            if (executor != null && this.mListener != null) {
                executor.execute(new Runnable() { // from class: android.media.tv.tuner.dvr.DvrPlayback$$ExternalSyntheticLambda0
                    @Override // java.lang.Runnable
                    public final void run() {
                        DvrPlayback.this.m2673x172a4184(status);
                    }
                });
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onPlaybackStatusChanged$0$android-media-tv-tuner-dvr-DvrPlayback  reason: not valid java name */
    public /* synthetic */ void m2673x172a4184(int status) {
        synchronized (this.mListenerLock) {
            OnPlaybackStatusChangedListener onPlaybackStatusChangedListener = this.mListener;
            if (onPlaybackStatusChangedListener != null) {
                onPlaybackStatusChangedListener.onPlaybackStatusChanged(status);
            }
        }
    }

    @Deprecated
    public int attachFilter(Filter filter) {
        return 1;
    }

    @Deprecated
    public int detachFilter(Filter filter) {
        return 1;
    }

    public int configure(DvrSettings settings) {
        return nativeConfigureDvr(settings);
    }

    public int start() {
        int i = this.mSegmentId;
        this.mSegmentId = (((i & 65535) + 1) & 65535) | ((-65536) & i);
        this.mUnderflow = 0;
        Log.d(TAG, "Write Stats Log for Playback.");
        FrameworkStatsLog.write(279, this.mUserId, 1, 1, this.mSegmentId, 0);
        return nativeStartDvr();
    }

    public int stop() {
        Log.d(TAG, "Write Stats Log for Playback.");
        FrameworkStatsLog.write(279, this.mUserId, 1, 2, this.mSegmentId, this.mUnderflow);
        return nativeStopDvr();
    }

    public int flush() {
        return nativeFlushDvr();
    }

    @Override // java.lang.AutoCloseable
    public void close() {
        int res = nativeClose();
        if (res != 0) {
            TunerUtils.throwExceptionForResult(res, "failed to close DVR playback");
        }
    }

    public void setFileDescriptor(ParcelFileDescriptor fd) {
        nativeSetFileDescriptor(fd.getFd());
    }

    public long read(long size) {
        return nativeRead(size);
    }

    public long read(byte[] buffer, long offset, long size) {
        if (size + offset > buffer.length) {
            throw new ArrayIndexOutOfBoundsException("Array length=" + buffer.length + ", offset=" + offset + ", size=" + size);
        }
        return nativeRead(buffer, offset, size);
    }

    public long seek(long position) {
        return nativeSeek(position);
    }
}
