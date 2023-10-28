package com.transsion.hubcore.telephony;

import com.transsion.hubcore.utils.TranClassInfo;
import java.util.function.Supplier;
/* loaded from: classes4.dex */
public interface ITranCellSignalStrength {
    public static final TranClassInfo<ITranCellSignalStrength> classInfo = new TranClassInfo<>("com.transsion.hubcore.telephony.TranCellSignalStrengthImpl", ITranCellSignalStrength.class, new Supplier() { // from class: com.transsion.hubcore.telephony.ITranCellSignalStrength$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return ITranCellSignalStrength.lambda$static$0();
        }
    });

    static /* synthetic */ ITranCellSignalStrength lambda$static$0() {
        return new ITranCellSignalStrength() { // from class: com.transsion.hubcore.telephony.ITranCellSignalStrength.1
        };
    }

    static ITranCellSignalStrength Instance() {
        return classInfo.getImpl();
    }

    default int getSignalStrengthLevelGsm(int dbm) {
        return 0;
    }

    default int getSignalStrengthLevelLte(int dbm) {
        return 0;
    }

    default int getSignalStrengthLevelNr(int dbm) {
        return 0;
    }

    default int getSignalStrengthLevelTdscdma(int dbm) {
        return 0;
    }

    default int getSignalStrengthLevelWcdma(int dbm) {
        return 0;
    }
}
