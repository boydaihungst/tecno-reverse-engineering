package com.android.server.devicepolicy;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
/* loaded from: classes.dex */
class UserUnlockedBlockingReceiver extends BroadcastReceiver {
    private static final int WAIT_FOR_USER_UNLOCKED_TIMEOUT_SECONDS = 120;
    private final Semaphore mSemaphore = new Semaphore(0);
    private final int mUserId;

    /* JADX INFO: Access modifiers changed from: package-private */
    public UserUnlockedBlockingReceiver(int userId) {
        this.mUserId = userId;
    }

    @Override // android.content.BroadcastReceiver
    public void onReceive(Context context, Intent intent) {
        if ("android.intent.action.USER_UNLOCKED".equals(intent.getAction()) && intent.getIntExtra("android.intent.extra.user_handle", -10000) == this.mUserId) {
            this.mSemaphore.release();
        }
    }

    public boolean waitForUserUnlocked() {
        try {
            return this.mSemaphore.tryAcquire(120L, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            return false;
        }
    }
}
