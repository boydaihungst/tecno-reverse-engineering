package com.android.server.location.injector;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.UserHandle;
import com.android.internal.util.Preconditions;
import com.android.server.FgThread;
/* loaded from: classes.dex */
public class SystemScreenInteractiveHelper extends ScreenInteractiveHelper {
    private final Context mContext;
    private volatile boolean mIsInteractive;
    private boolean mReady;

    public SystemScreenInteractiveHelper(Context context) {
        this.mContext = context;
    }

    public void onSystemReady() {
        if (this.mReady) {
            return;
        }
        IntentFilter screenIntentFilter = new IntentFilter();
        screenIntentFilter.addAction("android.intent.action.SCREEN_OFF");
        screenIntentFilter.addAction("android.intent.action.SCREEN_ON");
        this.mContext.registerReceiverAsUser(new BroadcastReceiver() { // from class: com.android.server.location.injector.SystemScreenInteractiveHelper.1
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context, Intent intent) {
                boolean interactive;
                if ("android.intent.action.SCREEN_ON".equals(intent.getAction())) {
                    interactive = true;
                } else if ("android.intent.action.SCREEN_OFF".equals(intent.getAction())) {
                    interactive = false;
                } else {
                    return;
                }
                SystemScreenInteractiveHelper.this.onScreenInteractiveChanged(interactive);
            }
        }, UserHandle.ALL, screenIntentFilter, null, FgThread.getHandler());
        this.mReady = true;
    }

    void onScreenInteractiveChanged(boolean interactive) {
        if (interactive == this.mIsInteractive) {
            return;
        }
        this.mIsInteractive = interactive;
        notifyScreenInteractiveChanged(interactive);
    }

    @Override // com.android.server.location.injector.ScreenInteractiveHelper
    public boolean isInteractive() {
        Preconditions.checkState(this.mReady);
        return this.mIsInteractive;
    }
}
