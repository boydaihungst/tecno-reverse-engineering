package android.media;

import android.content.ComponentName;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.ServiceConnection;
import android.media.IMediaScannerListener;
import android.net.Uri;
import android.os.IBinder;
import android.os.UserManager;
import android.provider.DeviceConfig;
import android.provider.MediaStore;
import android.util.Log;
import com.android.internal.os.BackgroundThread;
import java.io.File;
/* loaded from: classes2.dex */
public class MediaScannerConnection implements ServiceConnection {
    private static final String TAG = "MediaScannerConnection";
    private final MediaScannerConnectionClient mClient;
    @Deprecated
    private boolean mConnected;
    private final Context mContext;
    @Deprecated
    private final IMediaScannerListener.Stub mListener = new IMediaScannerListener.Stub() { // from class: android.media.MediaScannerConnection.1
        @Override // android.media.IMediaScannerListener
        public void scanCompleted(String path, Uri uri) {
        }
    };
    private ContentProviderClient mProvider;
    @Deprecated
    private IMediaScannerService mService;

    /* loaded from: classes2.dex */
    public interface MediaScannerConnectionClient extends OnScanCompletedListener {
        void onMediaScannerConnected();
    }

    /* loaded from: classes2.dex */
    public interface OnScanCompletedListener {
        void onScanCompleted(String str, Uri uri);
    }

    public MediaScannerConnection(Context context, MediaScannerConnectionClient client) {
        this.mContext = context;
        this.mClient = client;
    }

    public void connect() {
        synchronized (this) {
            if (this.mProvider == null) {
                this.mProvider = this.mContext.getContentResolver().acquireContentProviderClient(DeviceConfig.NAMESPACE_MEDIA);
                MediaScannerConnectionClient mediaScannerConnectionClient = this.mClient;
                if (mediaScannerConnectionClient != null) {
                    mediaScannerConnectionClient.onMediaScannerConnected();
                }
            }
        }
    }

    public void disconnect() {
        synchronized (this) {
            ContentProviderClient contentProviderClient = this.mProvider;
            if (contentProviderClient != null) {
                contentProviderClient.close();
                this.mProvider = null;
            }
        }
    }

    public synchronized boolean isConnected() {
        return this.mProvider != null;
    }

    public void scanFile(final String path, String mimeType) {
        synchronized (this) {
            if (this.mProvider == null) {
                throw new IllegalStateException("not connected to MediaScannerService");
            }
            BackgroundThread.getExecutor().execute(new Runnable() { // from class: android.media.MediaScannerConnection$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    MediaScannerConnection.this.m2235lambda$scanFile$0$androidmediaMediaScannerConnection(path);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$scanFile$0$android-media-MediaScannerConnection  reason: not valid java name */
    public /* synthetic */ void m2235lambda$scanFile$0$androidmediaMediaScannerConnection(String path) {
        Uri uri = scanFileQuietly(this.mProvider, new File(path));
        runCallBack(this.mContext, this.mClient, path, uri);
    }

    public static void scanFile(final Context context, final String[] paths, String[] mimeTypes, final OnScanCompletedListener callback) {
        BackgroundThread.getExecutor().execute(new Runnable() { // from class: android.media.MediaScannerConnection$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                MediaScannerConnection.lambda$scanFile$1(Context.this, paths, callback);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$scanFile$1(Context context, String[] paths, OnScanCompletedListener callback) {
        ContentProviderClient client = context.getContentResolver().acquireContentProviderClient(DeviceConfig.NAMESPACE_MEDIA);
        try {
            for (String path : paths) {
                Uri uri = scanFileQuietly(client, new File(path));
                runCallBack(context, callback, path, uri);
            }
            if (client != null) {
                client.close();
            }
        } catch (Throwable th) {
            if (client != null) {
                try {
                    client.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
            }
            throw th;
        }
    }

    private static Uri scanFileQuietly(ContentProviderClient client, File file) {
        Uri uri = null;
        try {
            uri = MediaStore.scanFile(ContentResolver.wrap(client), file.getCanonicalFile());
            Log.d(TAG, "Scanned " + file + " to " + uri);
            return uri;
        } catch (Exception e) {
            Log.w(TAG, "Failed to scan " + file + ": " + e);
            return uri;
        }
    }

    private static void runCallBack(Context context, OnScanCompletedListener callback, String path, Uri uri) {
        if (callback != null) {
            try {
                callback.onScanCompleted(path, uri);
            } catch (Throwable e) {
                if (((UserManager) context.getSystemService("user")).isDualProfile()) {
                    Log.w(TAG, "Ignoring exception from callback for dual app", e);
                } else if (context.getApplicationInfo().targetSdkVersion >= 30) {
                    throw e;
                } else {
                    Log.w(TAG, "Ignoring exception from callback for backward compatibility", e);
                }
            }
        }
    }

    @Deprecated
    /* loaded from: classes2.dex */
    static class ClientProxy implements MediaScannerConnectionClient {
        final OnScanCompletedListener mClient;
        MediaScannerConnection mConnection;
        final String[] mMimeTypes;
        int mNextPath;
        final String[] mPaths;

        ClientProxy(String[] paths, String[] mimeTypes, OnScanCompletedListener client) {
            this.mPaths = paths;
            this.mMimeTypes = mimeTypes;
            this.mClient = client;
        }

        @Override // android.media.MediaScannerConnection.MediaScannerConnectionClient
        public void onMediaScannerConnected() {
        }

        @Override // android.media.MediaScannerConnection.OnScanCompletedListener
        public void onScanCompleted(String path, Uri uri) {
        }

        void scanNextPath() {
        }
    }

    @Override // android.content.ServiceConnection
    public void onServiceConnected(ComponentName className, IBinder service) {
    }

    @Override // android.content.ServiceConnection
    public void onServiceDisconnected(ComponentName className) {
    }
}
