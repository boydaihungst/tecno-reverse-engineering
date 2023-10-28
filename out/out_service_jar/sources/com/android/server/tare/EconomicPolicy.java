package com.android.server.tare;

import android.app.tare.EconomyManager;
import android.provider.DeviceConfig;
import android.util.IndentingPrintWriter;
import android.util.KeyValueListParser;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
/* loaded from: classes2.dex */
public abstract class EconomicPolicy {
    static final int MASK_EVENT = 536870911;
    static final int MASK_POLICY = 536870912;
    static final int MASK_TYPE = -1073741824;
    static final int POLICY_AM = 0;
    static final int POLICY_JS = 536870912;
    static final int REGULATION_BASIC_INCOME = 0;
    static final int REGULATION_BIRTHRIGHT = 1;
    static final int REGULATION_DEMOTION = 4;
    static final int REGULATION_PROMOTION = 3;
    static final int REGULATION_WEALTH_RECLAMATION = 2;
    static final int REWARD_NOTIFICATION_INTERACTION = -2147483647;
    static final int REWARD_NOTIFICATION_SEEN = Integer.MIN_VALUE;
    static final int REWARD_OTHER_USER_INTERACTION = -2147483644;
    static final int REWARD_TOP_ACTIVITY = -2147483646;
    static final int REWARD_WIDGET_INTERACTION = -2147483645;
    private static final int SHIFT_POLICY = 29;
    private static final int SHIFT_TYPE = 30;
    static final int TYPE_ACTION = 1073741824;
    static final int TYPE_REGULATION = 0;
    static final int TYPE_REWARD = Integer.MIN_VALUE;
    private static final String TAG = "TARE-" + EconomicPolicy.class.getSimpleName();
    private static final Modifier[] COST_MODIFIER_BY_INDEX = new Modifier[4];

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes2.dex */
    public @interface AppAction {
    }

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes2.dex */
    public @interface EventType {
    }

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes2.dex */
    public @interface UtilityReward {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public abstract Action getAction(int i);

    /* JADX INFO: Access modifiers changed from: package-private */
    public abstract int[] getCostModifiers();

    abstract long getHardSatiatedConsumptionLimit();

    /* JADX INFO: Access modifiers changed from: package-private */
    public abstract long getInitialSatiatedConsumptionLimit();

    /* JADX INFO: Access modifiers changed from: package-private */
    public abstract long getMaxSatiatedBalance();

    /* JADX INFO: Access modifiers changed from: package-private */
    public abstract long getMinSatiatedBalance(int i, String str);

    /* JADX INFO: Access modifiers changed from: package-private */
    public abstract Reward getReward(int i);

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public static class Action {
        public final long basePrice;
        public final long costToProduce;
        public final int id;

        /* JADX INFO: Access modifiers changed from: package-private */
        public Action(int id, long costToProduce, long basePrice) {
            this.id = id;
            this.costToProduce = costToProduce;
            this.basePrice = basePrice;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public static class Reward {
        public final int id;
        public final long instantReward;
        public final long maxDailyReward;
        public final long ongoingRewardPerSecond;

        /* JADX INFO: Access modifiers changed from: package-private */
        public Reward(int id, long instantReward, long ongoingReward, long maxDailyReward) {
            this.id = id;
            this.instantReward = instantReward;
            this.ongoingRewardPerSecond = ongoingReward;
            this.maxDailyReward = maxDailyReward;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public static class Cost {
        public final long costToProduce;
        public final long price;

        /* JADX INFO: Access modifiers changed from: package-private */
        public Cost(long costToProduce, long price) {
            this.costToProduce = costToProduce;
            this.price = price;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public EconomicPolicy(InternalResourceService irs) {
        int[] costModifiers;
        for (int mId : getCostModifiers()) {
            initModifier(mId, irs);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setup(DeviceConfig.Properties properties) {
        for (int i = 0; i < 4; i++) {
            Modifier modifier = COST_MODIFIER_BY_INDEX[i];
            if (modifier != null) {
                modifier.setup();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void tearDown() {
        for (int i = 0; i < 4; i++) {
            Modifier modifier = COST_MODIFIER_BY_INDEX[i];
            if (modifier != null) {
                modifier.tearDown();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dump(IndentingPrintWriter pw) {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public final Cost getCostOfAction(int actionId, int userId, String pkgName) {
        Action action = getAction(actionId);
        if (action == null) {
            return new Cost(0L, 0L);
        }
        long ctp = action.costToProduce;
        long price = action.basePrice;
        int[] costModifiers = getCostModifiers();
        boolean useProcessStatePriceDeterminant = false;
        for (int costModifier : costModifiers) {
            if (costModifier == 3) {
                useProcessStatePriceDeterminant = true;
            } else {
                Modifier modifier = getModifier(costModifier);
                ctp = modifier.getModifiedCostToProduce(ctp);
                price = modifier.getModifiedPrice(price);
            }
        }
        if (useProcessStatePriceDeterminant) {
            ProcessStateModifier processStateModifier = (ProcessStateModifier) getModifier(3);
            price = processStateModifier.getModifiedPrice(userId, pkgName, ctp, price);
        }
        return new Cost(ctp, price);
    }

    private static void initModifier(int modifierId, InternalResourceService irs) {
        Modifier modifier;
        if (modifierId >= 0) {
            Modifier[] modifierArr = COST_MODIFIER_BY_INDEX;
            if (modifierId < modifierArr.length) {
                Modifier modifier2 = modifierArr[modifierId];
                if (modifier2 == null) {
                    switch (modifierId) {
                        case 0:
                            modifier = new ChargingModifier(irs);
                            break;
                        case 1:
                            modifier = new DeviceIdleModifier(irs);
                            break;
                        case 2:
                            modifier = new PowerSaveModeModifier(irs);
                            break;
                        case 3:
                            modifier = new ProcessStateModifier(irs);
                            break;
                        default:
                            throw new IllegalArgumentException("Invalid modifier id " + modifierId);
                    }
                    modifierArr[modifierId] = modifier;
                    return;
                }
                return;
            }
        }
        throw new IllegalArgumentException("Invalid modifier id " + modifierId);
    }

    private static Modifier getModifier(int modifierId) {
        if (modifierId >= 0) {
            Modifier[] modifierArr = COST_MODIFIER_BY_INDEX;
            if (modifierId < modifierArr.length) {
                Modifier modifier = modifierArr[modifierId];
                if (modifier == null) {
                    throw new IllegalStateException("Modifier #" + modifierId + " was never initialized");
                }
                return modifier;
            }
        }
        throw new IllegalArgumentException("Invalid modifier id " + modifierId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static int getEventType(int eventId) {
        return (-1073741824) & eventId;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static String eventToString(int eventId) {
        switch ((-1073741824) & eventId) {
            case Integer.MIN_VALUE:
                return rewardToString(eventId);
            case 0:
                return regulationToString(eventId);
            case 1073741824:
                return actionToString(eventId);
            default:
                return "UNKNOWN_EVENT:" + Integer.toHexString(eventId);
        }
    }

    static String actionToString(int eventId) {
        switch (536870912 & eventId) {
            case 0:
                switch (eventId) {
                    case 1073741824:
                        return "ALARM_WAKEUP_EXACT_ALLOW_WHILE_IDLE";
                    case 1073741825:
                        return "ALARM_WAKEUP_EXACT";
                    case AlarmManagerEconomicPolicy.ACTION_ALARM_WAKEUP_INEXACT_ALLOW_WHILE_IDLE /* 1073741826 */:
                        return "ALARM_WAKEUP_INEXACT_ALLOW_WHILE_IDLE";
                    case AlarmManagerEconomicPolicy.ACTION_ALARM_WAKEUP_INEXACT /* 1073741827 */:
                        return "ALARM_WAKEUP_INEXACT";
                    case AlarmManagerEconomicPolicy.ACTION_ALARM_NONWAKEUP_EXACT_ALLOW_WHILE_IDLE /* 1073741828 */:
                        return "ALARM_NONWAKEUP_EXACT_ALLOW_WHILE_IDLE";
                    case AlarmManagerEconomicPolicy.ACTION_ALARM_NONWAKEUP_EXACT /* 1073741829 */:
                        return "ALARM_NONWAKEUP_EXACT";
                    case AlarmManagerEconomicPolicy.ACTION_ALARM_NONWAKEUP_INEXACT_ALLOW_WHILE_IDLE /* 1073741830 */:
                        return "ALARM_NONWAKEUP_INEXACT_ALLOW_WHILE_IDLE";
                    case AlarmManagerEconomicPolicy.ACTION_ALARM_NONWAKEUP_INEXACT /* 1073741831 */:
                        return "ALARM_NONWAKEUP_INEXACT";
                    case AlarmManagerEconomicPolicy.ACTION_ALARM_CLOCK /* 1073741832 */:
                        return "ALARM_CLOCK";
                }
            case 536870912:
                switch (eventId) {
                    case JobSchedulerEconomicPolicy.ACTION_JOB_MAX_START /* 1610612736 */:
                        return "JOB_MAX_START";
                    case JobSchedulerEconomicPolicy.ACTION_JOB_MAX_RUNNING /* 1610612737 */:
                        return "JOB_MAX_RUNNING";
                    case JobSchedulerEconomicPolicy.ACTION_JOB_HIGH_START /* 1610612738 */:
                        return "JOB_HIGH_START";
                    case JobSchedulerEconomicPolicy.ACTION_JOB_HIGH_RUNNING /* 1610612739 */:
                        return "JOB_HIGH_RUNNING";
                    case JobSchedulerEconomicPolicy.ACTION_JOB_DEFAULT_START /* 1610612740 */:
                        return "JOB_DEFAULT_START";
                    case JobSchedulerEconomicPolicy.ACTION_JOB_DEFAULT_RUNNING /* 1610612741 */:
                        return "JOB_DEFAULT_RUNNING";
                    case JobSchedulerEconomicPolicy.ACTION_JOB_LOW_START /* 1610612742 */:
                        return "JOB_LOW_START";
                    case JobSchedulerEconomicPolicy.ACTION_JOB_LOW_RUNNING /* 1610612743 */:
                        return "JOB_LOW_RUNNING";
                    case JobSchedulerEconomicPolicy.ACTION_JOB_MIN_START /* 1610612744 */:
                        return "JOB_MIN_START";
                    case JobSchedulerEconomicPolicy.ACTION_JOB_MIN_RUNNING /* 1610612745 */:
                        return "JOB_MIN_RUNNING";
                    case JobSchedulerEconomicPolicy.ACTION_JOB_TIMEOUT /* 1610612746 */:
                        return "JOB_TIMEOUT";
                }
        }
        return "UNKNOWN_ACTION:" + Integer.toHexString(eventId);
    }

    static String regulationToString(int eventId) {
        switch (eventId) {
            case 0:
                return "BASIC_INCOME";
            case 1:
                return "BIRTHRIGHT";
            case 2:
                return "WEALTH_RECLAMATION";
            case 3:
                return "PROMOTION";
            case 4:
                return "DEMOTION";
            default:
                return "UNKNOWN_REGULATION:" + Integer.toHexString(eventId);
        }
    }

    static String rewardToString(int eventId) {
        switch (eventId) {
            case Integer.MIN_VALUE:
                return "REWARD_NOTIFICATION_SEEN";
            case -2147483647:
                return "REWARD_NOTIFICATION_INTERACTION";
            case -2147483646:
                return "REWARD_TOP_ACTIVITY";
            case -2147483645:
                return "REWARD_WIDGET_INTERACTION";
            case -2147483644:
                return "REWARD_OTHER_USER_INTERACTION";
            default:
                return "UNKNOWN_REWARD:" + Integer.toHexString(eventId);
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public long getConstantAsCake(KeyValueListParser parser, DeviceConfig.Properties properties, String key, long defaultValCake) {
        if (parser.size() > 0) {
            return EconomyManager.parseCreditValue(parser.getString(key, (String) null), defaultValCake);
        }
        if (properties != null) {
            return EconomyManager.parseCreditValue(properties.getString(key, (String) null), defaultValCake);
        }
        return defaultValCake;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public static void dumpActiveModifiers(IndentingPrintWriter pw) {
        for (int i = 0; i < 4; i++) {
            pw.print("Modifier ");
            pw.println(i);
            pw.increaseIndent();
            Modifier modifier = COST_MODIFIER_BY_INDEX[i];
            if (modifier != null) {
                modifier.dump(pw);
            } else {
                pw.println("NOT ACTIVE");
            }
            pw.decreaseIndent();
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public static void dumpAction(IndentingPrintWriter pw, Action action) {
        pw.print(actionToString(action.id));
        pw.print(": ");
        pw.print("ctp=");
        pw.print(TareUtils.cakeToString(action.costToProduce));
        pw.print(", basePrice=");
        pw.print(TareUtils.cakeToString(action.basePrice));
        pw.println();
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public static void dumpReward(IndentingPrintWriter pw, Reward reward) {
        pw.print(rewardToString(reward.id));
        pw.print(": ");
        pw.print("instant=");
        pw.print(TareUtils.cakeToString(reward.instantReward));
        pw.print(", ongoing/sec=");
        pw.print(TareUtils.cakeToString(reward.ongoingRewardPerSecond));
        pw.print(", maxDaily=");
        pw.print(TareUtils.cakeToString(reward.maxDailyReward));
        pw.println();
    }
}
