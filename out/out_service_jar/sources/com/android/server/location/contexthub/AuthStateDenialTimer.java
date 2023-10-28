package com.android.server.location.contexthub;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import java.util.concurrent.TimeUnit;
/* loaded from: classes.dex */
public class AuthStateDenialTimer {
    private static final int MSG = 1;
    private static final long TIMEOUT_MS = TimeUnit.SECONDS.toMillis(60);
    private boolean mCancelled = false;
    private final ContextHubClientBroker mClient;
    private final Handler mHandler;
    private final long mNanoAppId;
    private long mStopTimeInFuture;

    public AuthStateDenialTimer(ContextHubClientBroker client, long nanoAppId, Looper looper) {
        this.mClient = client;
        this.mNanoAppId = nanoAppId;
        this.mHandler = new CountDownHandler(looper);
    }

    public synchronized void cancel() {
        this.mCancelled = true;
        this.mHandler.removeMessages(1);
    }

    public synchronized void start() {
        this.mCancelled = false;
        this.mStopTimeInFuture = SystemClock.elapsedRealtime() + TIMEOUT_MS;
        Handler handler = this.mHandler;
        handler.sendMessage(handler.obtainMessage(1));
    }

    public void onFinish() {
        this.mClient.handleAuthStateTimerExpiry(this.mNanoAppId);
    }

    /* loaded from: classes.dex */
    private class CountDownHandler extends Handler {
        CountDownHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            synchronized (AuthStateDenialTimer.this) {
                if (AuthStateDenialTimer.this.mCancelled) {
                    return;
                }
                long millisLeft = AuthStateDenialTimer.this.mStopTimeInFuture - SystemClock.elapsedRealtime();
                if (millisLeft <= 0) {
                    AuthStateDenialTimer.this.onFinish();
                } else {
                    sendMessageDelayed(obtainMessage(1), millisLeft);
                }
            }
        }
    }
}
