package com.android.server.people.data;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.people.ConversationStatus;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.audio.common.V2_0.AudioFormat;
import android.net.Uri;
import android.os.Binder;
import android.os.CancellationSignal;
import com.android.server.LocalServices;
import com.android.server.people.PeopleServiceInternal;
import com.android.server.pm.PackageManagerService;
/* loaded from: classes2.dex */
public class ConversationStatusExpirationBroadcastReceiver extends BroadcastReceiver {
    static final String ACTION = "ConversationStatusExpiration";
    static final String EXTRA_USER_ID = "userId";
    static final int REQUEST_CODE = 10;
    static final String SCHEME = "expStatus";

    /* JADX INFO: Access modifiers changed from: package-private */
    public void scheduleExpiration(Context context, int userId, String pkg, String conversationId, ConversationStatus status) {
        long identity = Binder.clearCallingIdentity();
        try {
            PendingIntent pi = PendingIntent.getBroadcast(context, 10, new Intent(ACTION).setPackage(PackageManagerService.PLATFORM_PACKAGE_NAME).setData(new Uri.Builder().scheme(SCHEME).appendPath(getKey(userId, pkg, conversationId, status)).build()).addFlags(268435456).putExtra("userId", userId), AudioFormat.DTS_HD);
            ((AlarmManager) context.getSystemService(AlarmManager.class)).setExactAndAllowWhileIdle(0, status.getEndTimeMillis(), pi);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    private static String getKey(int userId, String pkg, String conversationId, ConversationStatus status) {
        return userId + pkg + conversationId + status.getId();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static IntentFilter getFilter() {
        IntentFilter conversationStatusFilter = new IntentFilter(ACTION);
        conversationStatusFilter.addDataScheme(SCHEME);
        return conversationStatusFilter;
    }

    @Override // android.content.BroadcastReceiver
    public void onReceive(Context context, final Intent intent) {
        String action = intent.getAction();
        if (action != null && ACTION.equals(action)) {
            new Thread(new Runnable() { // from class: com.android.server.people.data.ConversationStatusExpirationBroadcastReceiver$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    ConversationStatusExpirationBroadcastReceiver.lambda$onReceive$0(intent);
                }
            }).start();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$onReceive$0(Intent intent) {
        PeopleServiceInternal peopleServiceInternal = (PeopleServiceInternal) LocalServices.getService(PeopleServiceInternal.class);
        peopleServiceInternal.pruneDataForUser(intent.getIntExtra("userId", ActivityManager.getCurrentUser()), new CancellationSignal());
    }
}
