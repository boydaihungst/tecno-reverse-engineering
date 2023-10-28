package com.android.server.display;

import android.content.Context;
import android.os.Handler;
import android.view.Display;
import com.android.server.display.DisplayManagerService;
import java.io.PrintWriter;
import java.util.concurrent.atomic.AtomicInteger;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public abstract class DisplayAdapter {
    public static final int DISPLAY_DEVICE_EVENT_ADDED = 1;
    public static final int DISPLAY_DEVICE_EVENT_CHANGED = 2;
    public static final int DISPLAY_DEVICE_EVENT_REMOVED = 3;
    private static final AtomicInteger NEXT_DISPLAY_MODE_ID = new AtomicInteger(1);
    private final Context mContext;
    private final Handler mHandler;
    private final Listener mListener;
    private final String mName;
    private final DisplayManagerService.SyncRoot mSyncRoot;

    /* loaded from: classes.dex */
    public interface Listener {
        void onDisplayDeviceEvent(DisplayDevice displayDevice, int i);

        void onTraversalRequested();
    }

    public DisplayAdapter(DisplayManagerService.SyncRoot syncRoot, Context context, Handler handler, Listener listener, String name) {
        this.mSyncRoot = syncRoot;
        this.mContext = context;
        this.mHandler = handler;
        this.mListener = listener;
        this.mName = name;
    }

    public final DisplayManagerService.SyncRoot getSyncRoot() {
        return this.mSyncRoot;
    }

    public final Context getContext() {
        return this.mContext;
    }

    public final Handler getHandler() {
        return this.mHandler;
    }

    public final String getName() {
        return this.mName;
    }

    public void registerLocked() {
    }

    public void dumpLocked(PrintWriter pw) {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$sendDisplayDeviceEventLocked$0$com-android-server-display-DisplayAdapter  reason: not valid java name */
    public /* synthetic */ void m3226x1f33e191(DisplayDevice device, int event) {
        this.mListener.onDisplayDeviceEvent(device, event);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public final void sendDisplayDeviceEventLocked(final DisplayDevice device, final int event) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.display.DisplayAdapter$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                DisplayAdapter.this.m3226x1f33e191(device, event);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$sendTraversalRequestLocked$1$com-android-server-display-DisplayAdapter  reason: not valid java name */
    public /* synthetic */ void m3227xc26f2001() {
        this.mListener.onTraversalRequested();
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public final void sendTraversalRequestLocked() {
        this.mHandler.post(new Runnable() { // from class: com.android.server.display.DisplayAdapter$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                DisplayAdapter.this.m3227xc26f2001();
            }
        });
    }

    public static Display.Mode createMode(int width, int height, float refreshRate) {
        return createMode(width, height, refreshRate, new float[0]);
    }

    public static Display.Mode createMode(int width, int height, float refreshRate, float[] alternativeRefreshRates) {
        return new Display.Mode(NEXT_DISPLAY_MODE_ID.getAndIncrement(), width, height, refreshRate, alternativeRefreshRates);
    }
}
