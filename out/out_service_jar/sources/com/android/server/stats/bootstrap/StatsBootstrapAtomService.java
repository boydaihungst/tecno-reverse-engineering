package com.android.server.stats.bootstrap;

import android.content.Context;
import android.os.IStatsBootstrapAtomService;
import android.os.StatsBootstrapAtom;
import android.os.StatsBootstrapAtomValue;
import android.util.Slog;
import android.util.StatsEvent;
import android.util.StatsLog;
import com.android.server.SystemService;
/* loaded from: classes2.dex */
public class StatsBootstrapAtomService extends IStatsBootstrapAtomService.Stub {
    private static final boolean DEBUG = false;
    private static final String TAG = "StatsBootstrapAtomService";

    public void reportBootstrapAtom(StatsBootstrapAtom atom) {
        StatsBootstrapAtomValue[] statsBootstrapAtomValueArr;
        if (atom.atomId < 1 || atom.atomId >= 10000) {
            Slog.e(TAG, "Atom ID " + atom.atomId + " is not a valid atom ID");
            return;
        }
        StatsEvent.Builder builder = StatsEvent.newBuilder().setAtomId(atom.atomId);
        for (StatsBootstrapAtomValue value : atom.values) {
            switch (value.getTag()) {
                case 0:
                    builder.writeBoolean(value.getBoolValue());
                    break;
                case 1:
                    builder.writeInt(value.getIntValue());
                    break;
                case 2:
                    builder.writeLong(value.getLongValue());
                    break;
                case 3:
                    builder.writeFloat(value.getFloatValue());
                    break;
                case 4:
                    builder.writeString(value.getStringValue());
                    break;
                case 5:
                    builder.writeByteArray(value.getBytesValue());
                    break;
                default:
                    Slog.e(TAG, "Unexpected value type " + value.getTag() + " when logging atom " + atom.atomId);
                    return;
            }
        }
        StatsLog.write(builder.usePooledBuffer().build());
    }

    /* loaded from: classes2.dex */
    public static final class Lifecycle extends SystemService {
        private StatsBootstrapAtomService mStatsBootstrapAtomService;

        public Lifecycle(Context context) {
            super(context);
        }

        /* JADX DEBUG: Multi-variable search result rejected for r3v0, resolved type: com.android.server.stats.bootstrap.StatsBootstrapAtomService$Lifecycle */
        /* JADX WARN: Multi-variable type inference failed */
        /* JADX WARN: Type inference failed for: r0v0, types: [com.android.server.stats.bootstrap.StatsBootstrapAtomService, android.os.IBinder] */
        @Override // com.android.server.SystemService
        public void onStart() {
            ?? statsBootstrapAtomService = new StatsBootstrapAtomService();
            this.mStatsBootstrapAtomService = statsBootstrapAtomService;
            try {
                publishBinderService("statsbootstrap", statsBootstrapAtomService);
            } catch (Exception e) {
                Slog.e(StatsBootstrapAtomService.TAG, "Failed to publishBinderService", e);
            }
        }
    }
}
