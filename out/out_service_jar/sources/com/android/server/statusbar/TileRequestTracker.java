package com.android.server.statusbar;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.UserHandle;
import android.util.ArraySet;
import android.util.IndentingPrintWriter;
import android.util.SparseArrayMap;
import java.io.FileDescriptor;
/* loaded from: classes2.dex */
public class TileRequestTracker {
    static final int MAX_NUM_DENIALS = 3;
    private final Context mContext;
    private final BroadcastReceiver mUninstallReceiver;
    private final Object mLock = new Object();
    private final SparseArrayMap<ComponentName, Integer> mTrackingMap = new SparseArrayMap<>();
    private final ArraySet<ComponentName> mComponentsToRemove = new ArraySet<>();

    /* JADX INFO: Access modifiers changed from: package-private */
    public TileRequestTracker(Context context) {
        BroadcastReceiver broadcastReceiver = new BroadcastReceiver() { // from class: com.android.server.statusbar.TileRequestTracker.1
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent) {
                if (intent.getBooleanExtra("android.intent.extra.REPLACING", false)) {
                    return;
                }
                Uri data = intent.getData();
                String packageName = data.getEncodedSchemeSpecificPart();
                if (!intent.hasExtra("android.intent.extra.UID")) {
                    return;
                }
                int userId = UserHandle.getUserId(intent.getIntExtra("android.intent.extra.UID", -1));
                synchronized (TileRequestTracker.this.mLock) {
                    TileRequestTracker.this.mComponentsToRemove.clear();
                    int elementsForUser = TileRequestTracker.this.mTrackingMap.numElementsForKey(userId);
                    int userKeyIndex = TileRequestTracker.this.mTrackingMap.indexOfKey(userId);
                    for (int compKeyIndex = 0; compKeyIndex < elementsForUser; compKeyIndex++) {
                        ComponentName c = (ComponentName) TileRequestTracker.this.mTrackingMap.keyAt(userKeyIndex, compKeyIndex);
                        if (c.getPackageName().equals(packageName)) {
                            TileRequestTracker.this.mComponentsToRemove.add(c);
                        }
                    }
                    int compsToRemoveNum = TileRequestTracker.this.mComponentsToRemove.size();
                    for (int i = 0; i < compsToRemoveNum; i++) {
                        TileRequestTracker.this.mTrackingMap.delete(userId, (ComponentName) TileRequestTracker.this.mComponentsToRemove.valueAt(i));
                    }
                }
            }
        };
        this.mUninstallReceiver = broadcastReceiver;
        this.mContext = context;
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.PACKAGE_REMOVED");
        intentFilter.addAction("android.intent.action.PACKAGE_DATA_CLEARED");
        intentFilter.addDataScheme("package");
        context.registerReceiverAsUser(broadcastReceiver, UserHandle.ALL, intentFilter, null, null);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean shouldBeDenied(int userId, ComponentName componentName) {
        boolean z;
        synchronized (this.mLock) {
            z = ((Integer) this.mTrackingMap.getOrDefault(userId, componentName, 0)).intValue() >= 3;
        }
        return z;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void addDenial(int userId, ComponentName componentName) {
        synchronized (this.mLock) {
            int current = ((Integer) this.mTrackingMap.getOrDefault(userId, componentName, 0)).intValue();
            this.mTrackingMap.add(userId, componentName, Integer.valueOf(current + 1));
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void resetRequests(int userId, ComponentName componentName) {
        synchronized (this.mLock) {
            this.mTrackingMap.delete(userId, componentName);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dump(FileDescriptor fd, final IndentingPrintWriter pw, String[] args) {
        pw.println("TileRequestTracker:");
        pw.increaseIndent();
        synchronized (this.mLock) {
            this.mTrackingMap.forEach(new SparseArrayMap.TriConsumer() { // from class: com.android.server.statusbar.TileRequestTracker$$ExternalSyntheticLambda0
                public final void accept(int i, Object obj, Object obj2) {
                    ComponentName componentName = (ComponentName) obj;
                    pw.println("user=" + i + ", " + componentName.toShortString() + ": " + ((Integer) obj2));
                }
            });
        }
        pw.decreaseIndent();
    }
}
