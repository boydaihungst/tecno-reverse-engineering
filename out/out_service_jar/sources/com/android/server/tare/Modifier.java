package com.android.server.tare;

import android.util.IndentingPrintWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
/* loaded from: classes2.dex */
abstract class Modifier {
    static final int COST_MODIFIER_CHARGING = 0;
    static final int COST_MODIFIER_DEVICE_IDLE = 1;
    static final int COST_MODIFIER_POWER_SAVE_MODE = 2;
    static final int COST_MODIFIER_PROCESS_STATE = 3;
    static final int NUM_COST_MODIFIERS = 4;

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes2.dex */
    public @interface CostModifier {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public abstract void dump(IndentingPrintWriter indentingPrintWriter);

    /* JADX INFO: Access modifiers changed from: package-private */
    public long getModifiedCostToProduce(long ctp) {
        return ctp;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public long getModifiedPrice(long price) {
        return price;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setup() {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void tearDown() {
    }
}
