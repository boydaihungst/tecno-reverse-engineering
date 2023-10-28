package com.android.internal.os;

import android.os.BatteryStats;
import android.os.BatteryUsageStats;
import android.os.BatteryUsageStatsQuery;
import android.os.UidBatteryConsumer;
import android.os.UserHandle;
import android.util.SparseArray;
import com.android.internal.util.ArrayUtils;
/* loaded from: classes4.dex */
public class UserPowerCalculator extends PowerCalculator {
    @Override // com.android.internal.os.PowerCalculator
    public boolean isPowerComponentSupported(int powerComponent) {
        return true;
    }

    @Override // com.android.internal.os.PowerCalculator
    public void calculate(BatteryUsageStats.Builder builder, BatteryStats batteryStats, long rawRealtimeUs, long rawUptimeUs, BatteryUsageStatsQuery query) {
        int[] userIds = query.getUserIds();
        if (ArrayUtils.contains(userIds, -1)) {
            return;
        }
        SparseArray<UidBatteryConsumer.Builder> uidBatteryConsumerBuilders = builder.getUidBatteryConsumerBuilders();
        for (int i = uidBatteryConsumerBuilders.size() - 1; i >= 0; i--) {
            UidBatteryConsumer.Builder uidBuilder = uidBatteryConsumerBuilders.valueAt(i);
            if (!uidBuilder.isVirtualUid()) {
                int uid = uidBuilder.getUid();
                if (UserHandle.getAppId(uid) >= 10000) {
                    int userId = UserHandle.getUserId(uid);
                    if (!ArrayUtils.contains(userIds, userId)) {
                        uidBuilder.excludeFromBatteryUsageStats();
                        builder.getOrCreateUserBatteryConsumerBuilder(userId).addUidBatteryConsumer(uidBuilder);
                    }
                }
            }
        }
    }
}
