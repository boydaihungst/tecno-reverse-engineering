package com.android.server.audio;

import android.app.ActivityManager;
import android.app.ActivityManagerInternal;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.UserInfo;
import android.os.Binder;
import android.os.UserHandle;
import android.os.UserManager;
import com.android.server.LocalServices;
import java.util.Objects;
/* loaded from: classes.dex */
public class SystemServerAdapter {
    protected final Context mContext;

    protected SystemServerAdapter(Context context) {
        this.mContext = context;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static final SystemServerAdapter getDefaultAdapter(Context context) {
        Objects.requireNonNull(context);
        return new SystemServerAdapter(context);
    }

    public boolean isPrivileged() {
        return true;
    }

    public void sendMicrophoneMuteChangedIntent() {
        this.mContext.sendBroadcastAsUser(new Intent("android.media.action.MICROPHONE_MUTE_CHANGED").setFlags(1073741824), UserHandle.ALL);
    }

    public void sendDeviceBecomingNoisyIntent() {
        if (this.mContext == null) {
            return;
        }
        Intent intent = new Intent("android.media.AUDIO_BECOMING_NOISY");
        intent.addFlags(67108864);
        intent.addFlags(268435456);
        long ident = Binder.clearCallingIdentity();
        try {
            this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public void broadcastStickyIntentToCurrentProfileGroup(Intent intent) {
        int[] profileIds = ((ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class)).getCurrentProfileIds();
        for (int userId : profileIds) {
            ActivityManager.broadcastStickyIntent(intent, userId);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void registerUserStartedReceiver(Context context) {
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.USER_STARTED");
        context.registerReceiverAsUser(new BroadcastReceiver() { // from class: com.android.server.audio.SystemServerAdapter.1
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent) {
                int userId;
                if (!"android.intent.action.USER_STARTED".equals(intent.getAction()) || (userId = intent.getIntExtra("android.intent.extra.user_handle", -10000)) == -10000) {
                    return;
                }
                UserManager userManager = (UserManager) context2.getSystemService(UserManager.class);
                UserInfo profileParent = userManager.getProfileParent(userId);
                if (profileParent == null) {
                    return;
                }
                SystemServerAdapter.this.broadcastProfileParentStickyIntent(context2, "android.media.action.HDMI_AUDIO_PLUG", userId, profileParent.id);
                SystemServerAdapter.this.broadcastProfileParentStickyIntent(context2, "android.intent.action.HEADSET_PLUG", userId, profileParent.id);
            }
        }, UserHandle.ALL, filter, null, null);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void broadcastProfileParentStickyIntent(Context context, String intentAction, int profileId, int parentId) {
        Intent intent = context.registerReceiverAsUser(null, UserHandle.of(parentId), new IntentFilter(intentAction), null, null);
        if (intent != null) {
            ActivityManager.broadcastStickyIntent(intent, profileId);
        }
    }
}
