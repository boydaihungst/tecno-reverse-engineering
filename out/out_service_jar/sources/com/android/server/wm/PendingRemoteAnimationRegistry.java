package com.android.server.wm;

import android.app.ActivityOptions;
import android.os.Handler;
import android.os.IBinder;
import android.util.ArrayMap;
import android.view.RemoteAnimationAdapter;
import com.android.server.wm.PendingRemoteAnimationRegistry;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class PendingRemoteAnimationRegistry {
    private static final long TIMEOUT_MS = 3000;
    private final ArrayMap<String, Entry> mEntries = new ArrayMap<>();
    private final Handler mHandler;
    private final WindowManagerGlobalLock mLock;

    /* JADX INFO: Access modifiers changed from: package-private */
    public PendingRemoteAnimationRegistry(WindowManagerGlobalLock lock, Handler handler) {
        this.mLock = lock;
        this.mHandler = handler;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void addPendingAnimation(String packageName, RemoteAnimationAdapter adapter, IBinder launchCookie) {
        this.mEntries.put(packageName, new Entry(packageName, adapter, launchCookie));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActivityOptions overrideOptionsIfNeeded(String callingPackage, ActivityOptions options) {
        Entry entry = this.mEntries.get(callingPackage);
        if (entry == null) {
            return options;
        }
        if (options == null) {
            options = ActivityOptions.makeRemoteAnimation(entry.adapter);
        } else {
            options.setRemoteAnimationAdapter(entry.adapter);
        }
        IBinder launchCookie = entry.launchCookie;
        if (launchCookie != null) {
            options.setLaunchCookie(launchCookie);
        }
        this.mEntries.remove(callingPackage);
        return options;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class Entry {
        final RemoteAnimationAdapter adapter;
        final IBinder launchCookie;
        final String packageName;

        Entry(final String packageName, RemoteAnimationAdapter adapter, IBinder launchCookie) {
            this.packageName = packageName;
            this.adapter = adapter;
            this.launchCookie = launchCookie;
            PendingRemoteAnimationRegistry.this.mHandler.postDelayed(new Runnable() { // from class: com.android.server.wm.PendingRemoteAnimationRegistry$Entry$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    PendingRemoteAnimationRegistry.Entry.this.m8130x29b0901e(packageName);
                }
            }, 3000L);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$new$0$com-android-server-wm-PendingRemoteAnimationRegistry$Entry  reason: not valid java name */
        public /* synthetic */ void m8130x29b0901e(String packageName) {
            synchronized (PendingRemoteAnimationRegistry.this.mLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    Entry entry = (Entry) PendingRemoteAnimationRegistry.this.mEntries.get(packageName);
                    if (entry == this) {
                        PendingRemoteAnimationRegistry.this.mEntries.remove(packageName);
                    }
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        }
    }
}
