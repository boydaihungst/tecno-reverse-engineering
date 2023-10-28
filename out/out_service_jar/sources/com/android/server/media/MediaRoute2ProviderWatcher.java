package com.android.server.media;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.Handler;
import android.os.UserHandle;
import android.util.Log;
import android.util.Slog;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
/* loaded from: classes2.dex */
public final class MediaRoute2ProviderWatcher {
    private final Callback mCallback;
    private final Context mContext;
    private final Handler mHandler;
    private final PackageManager mPackageManager;
    private boolean mRunning;
    private final int mUserId;
    private static final String TAG = "MR2ProviderWatcher";
    private static final boolean DEBUG = Log.isLoggable(TAG, 3);
    private final ArrayList<MediaRoute2ProviderServiceProxy> mProxies = new ArrayList<>();
    private final BroadcastReceiver mScanPackagesReceiver = new BroadcastReceiver() { // from class: com.android.server.media.MediaRoute2ProviderWatcher.1
        {
            MediaRoute2ProviderWatcher.this = this;
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            if (MediaRoute2ProviderWatcher.DEBUG) {
                Slog.d(MediaRoute2ProviderWatcher.TAG, "Received package manager broadcast: " + intent);
            }
            MediaRoute2ProviderWatcher.this.postScanPackagesIfNeeded();
        }
    };

    /* loaded from: classes2.dex */
    public interface Callback {
        void onAddProviderService(MediaRoute2ProviderServiceProxy mediaRoute2ProviderServiceProxy);

        void onRemoveProviderService(MediaRoute2ProviderServiceProxy mediaRoute2ProviderServiceProxy);
    }

    public MediaRoute2ProviderWatcher(Context context, Callback callback, Handler handler, int userId) {
        this.mContext = context;
        this.mCallback = callback;
        this.mHandler = handler;
        this.mUserId = userId;
        this.mPackageManager = context.getPackageManager();
    }

    public void dump(PrintWriter pw, String prefix) {
        pw.println(prefix + "Watcher");
        pw.println(prefix + "  mUserId=" + this.mUserId);
        pw.println(prefix + "  mRunning=" + this.mRunning);
        pw.println(prefix + "  mProxies.size()=" + this.mProxies.size());
    }

    public void start() {
        if (!this.mRunning) {
            this.mRunning = true;
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.intent.action.PACKAGE_ADDED");
            filter.addAction("android.intent.action.PACKAGE_REMOVED");
            filter.addAction("android.intent.action.PACKAGE_CHANGED");
            filter.addAction("android.intent.action.PACKAGE_REPLACED");
            filter.addAction("android.intent.action.PACKAGE_RESTARTED");
            filter.addDataScheme("package");
            this.mContext.registerReceiverAsUser(this.mScanPackagesReceiver, new UserHandle(this.mUserId), filter, null, this.mHandler);
            postScanPackagesIfNeeded();
        }
    }

    public void stop() {
        if (this.mRunning) {
            this.mRunning = false;
            this.mContext.unregisterReceiver(this.mScanPackagesReceiver);
            this.mHandler.removeCallbacks(new MediaRoute2ProviderWatcher$$ExternalSyntheticLambda0(this));
            for (int i = this.mProxies.size() - 1; i >= 0; i--) {
                this.mProxies.get(i).stop();
            }
        }
    }

    public void scanPackages() {
        if (!this.mRunning) {
            return;
        }
        int targetIndex = 0;
        Intent intent = new Intent("android.media.MediaRoute2ProviderService");
        for (ResolveInfo resolveInfo : this.mPackageManager.queryIntentServicesAsUser(intent, 0, this.mUserId)) {
            ServiceInfo serviceInfo = resolveInfo.serviceInfo;
            if (serviceInfo != null) {
                int sourceIndex = findProvider(serviceInfo.packageName, serviceInfo.name);
                if (sourceIndex < 0) {
                    MediaRoute2ProviderServiceProxy proxy = new MediaRoute2ProviderServiceProxy(this.mContext, new ComponentName(serviceInfo.packageName, serviceInfo.name), this.mUserId);
                    proxy.start();
                    this.mProxies.add(targetIndex, proxy);
                    this.mCallback.onAddProviderService(proxy);
                    targetIndex++;
                } else if (sourceIndex >= targetIndex) {
                    MediaRoute2ProviderServiceProxy proxy2 = this.mProxies.get(sourceIndex);
                    proxy2.start();
                    proxy2.rebindIfDisconnected();
                    Collections.swap(this.mProxies, sourceIndex, targetIndex);
                    targetIndex++;
                }
            }
        }
        if (targetIndex < this.mProxies.size()) {
            for (int i = this.mProxies.size() - 1; i >= targetIndex; i--) {
                MediaRoute2ProviderServiceProxy proxy3 = this.mProxies.get(i);
                this.mCallback.onRemoveProviderService(proxy3);
                this.mProxies.remove(proxy3);
                proxy3.stop();
            }
        }
    }

    private int findProvider(String packageName, String className) {
        int count = this.mProxies.size();
        for (int i = 0; i < count; i++) {
            MediaRoute2ProviderServiceProxy proxy = this.mProxies.get(i);
            if (proxy.hasComponentName(packageName, className)) {
                return i;
            }
        }
        return -1;
    }

    public void postScanPackagesIfNeeded() {
        if (!this.mHandler.hasCallbacks(new MediaRoute2ProviderWatcher$$ExternalSyntheticLambda0(this))) {
            this.mHandler.post(new MediaRoute2ProviderWatcher$$ExternalSyntheticLambda0(this));
        }
    }
}
