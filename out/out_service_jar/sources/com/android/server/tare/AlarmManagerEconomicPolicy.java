package com.android.server.tare;

import android.app.tare.EconomyManager;
import android.content.ContentResolver;
import android.hardware.audio.common.V2_0.AudioChannelMask;
import android.hardware.audio.common.V2_0.AudioDevice;
import android.provider.DeviceConfig;
import android.provider.Settings;
import android.util.IndentingPrintWriter;
import android.util.KeyValueListParser;
import android.util.Slog;
import android.util.SparseArray;
import com.android.server.tare.EconomicPolicy;
/* loaded from: classes2.dex */
public class AlarmManagerEconomicPolicy extends EconomicPolicy {
    public static final int ACTION_ALARM_CLOCK = 1073741832;
    public static final int ACTION_ALARM_NONWAKEUP_EXACT = 1073741829;
    public static final int ACTION_ALARM_NONWAKEUP_EXACT_ALLOW_WHILE_IDLE = 1073741828;
    public static final int ACTION_ALARM_NONWAKEUP_INEXACT = 1073741831;
    public static final int ACTION_ALARM_NONWAKEUP_INEXACT_ALLOW_WHILE_IDLE = 1073741830;
    public static final int ACTION_ALARM_WAKEUP_EXACT = 1073741825;
    public static final int ACTION_ALARM_WAKEUP_EXACT_ALLOW_WHILE_IDLE = 1073741824;
    public static final int ACTION_ALARM_WAKEUP_INEXACT = 1073741827;
    public static final int ACTION_ALARM_WAKEUP_INEXACT_ALLOW_WHILE_IDLE = 1073741826;
    private final SparseArray<EconomicPolicy.Action> mActions;
    private long mHardSatiatedConsumptionLimit;
    private long mInitialSatiatedConsumptionLimit;
    private final InternalResourceService mInternalResourceService;
    private long mMaxSatiatedBalance;
    private long mMinSatiatedBalanceExempted;
    private long mMinSatiatedBalanceOther;
    private final KeyValueListParser mParser;
    private final SparseArray<EconomicPolicy.Reward> mRewards;
    private static final String TAG = "TARE- " + AlarmManagerEconomicPolicy.class.getSimpleName();
    private static final int[] COST_MODIFIERS = {0, 1, 2, 3};

    /* JADX INFO: Access modifiers changed from: package-private */
    public AlarmManagerEconomicPolicy(InternalResourceService irs) {
        super(irs);
        this.mParser = new KeyValueListParser(',');
        this.mActions = new SparseArray<>();
        this.mRewards = new SparseArray<>();
        this.mInternalResourceService = irs;
        loadConstants("", null);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.tare.EconomicPolicy
    public void setup(DeviceConfig.Properties properties) {
        super.setup(properties);
        ContentResolver resolver = this.mInternalResourceService.getContext().getContentResolver();
        loadConstants(Settings.Global.getString(resolver, "tare_alarm_manager_constants"), properties);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.tare.EconomicPolicy
    public long getMinSatiatedBalance(int userId, String pkgName) {
        if (this.mInternalResourceService.isPackageExempted(userId, pkgName)) {
            return this.mMinSatiatedBalanceExempted;
        }
        return this.mMinSatiatedBalanceOther;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.tare.EconomicPolicy
    public long getMaxSatiatedBalance() {
        return this.mMaxSatiatedBalance;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.tare.EconomicPolicy
    public long getInitialSatiatedConsumptionLimit() {
        return this.mInitialSatiatedConsumptionLimit;
    }

    @Override // com.android.server.tare.EconomicPolicy
    long getHardSatiatedConsumptionLimit() {
        return this.mHardSatiatedConsumptionLimit;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.tare.EconomicPolicy
    public int[] getCostModifiers() {
        return COST_MODIFIERS;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.tare.EconomicPolicy
    public EconomicPolicy.Action getAction(int actionId) {
        return this.mActions.get(actionId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.tare.EconomicPolicy
    public EconomicPolicy.Reward getReward(int rewardId) {
        return this.mRewards.get(rewardId);
    }

    private void loadConstants(String policyValuesString, DeviceConfig.Properties properties) {
        this.mActions.clear();
        this.mRewards.clear();
        try {
            try {
                this.mParser.setString(policyValuesString);
            } catch (IllegalArgumentException e) {
                e = e;
                Slog.e(TAG, "Global setting key incorrect: ", e);
                this.mMinSatiatedBalanceExempted = getConstantAsCake(this.mParser, properties, "am_min_satiated_balance_exempted", EconomyManager.DEFAULT_AM_MIN_SATIATED_BALANCE_EXEMPTED_CAKES);
                this.mMinSatiatedBalanceOther = getConstantAsCake(this.mParser, properties, "am_min_satiated_balance_other_app", EconomyManager.DEFAULT_AM_MIN_SATIATED_BALANCE_OTHER_APP_CAKES);
                this.mMaxSatiatedBalance = getConstantAsCake(this.mParser, properties, "am_max_satiated_balance", EconomyManager.DEFAULT_AM_MAX_SATIATED_BALANCE_CAKES);
                long constantAsCake = getConstantAsCake(this.mParser, properties, "am_initial_consumption_limit", EconomyManager.DEFAULT_AM_INITIAL_CONSUMPTION_LIMIT_CAKES);
                this.mInitialSatiatedConsumptionLimit = constantAsCake;
                this.mHardSatiatedConsumptionLimit = Math.max(constantAsCake, getConstantAsCake(this.mParser, properties, "am_hard_consumption_limit", EconomyManager.DEFAULT_AM_HARD_CONSUMPTION_LIMIT_CAKES));
                long exactAllowWhileIdleWakeupBasePrice = getConstantAsCake(this.mParser, properties, "am_action_alarm_allow_while_idle_exact_wakeup_base_price", EconomyManager.DEFAULT_AM_ACTION_ALARM_ALLOW_WHILE_IDLE_EXACT_WAKEUP_BASE_PRICE_CAKES);
                this.mActions.put(1073741824, new EconomicPolicy.Action(1073741824, getConstantAsCake(this.mParser, properties, "am_action_alarm_allow_while_idle_exact_wakeup_ctp", EconomyManager.DEFAULT_AM_ACTION_ALARM_ALLOW_WHILE_IDLE_EXACT_WAKEUP_CTP_CAKES), exactAllowWhileIdleWakeupBasePrice));
                this.mActions.put(1073741825, new EconomicPolicy.Action(1073741825, getConstantAsCake(this.mParser, properties, "am_action_alarm_exact_wakeup_ctp", EconomyManager.DEFAULT_AM_ACTION_ALARM_EXACT_WAKEUP_CTP_CAKES), getConstantAsCake(this.mParser, properties, "am_action_alarm_exact_wakeup_base_price", EconomyManager.DEFAULT_AM_ACTION_ALARM_EXACT_WAKEUP_BASE_PRICE_CAKES)));
                long inexactAllowWhileIdleWakeupBasePrice = getConstantAsCake(this.mParser, properties, "am_action_alarm_allow_while_idle_inexact_wakeup_base_price", EconomyManager.DEFAULT_AM_ACTION_ALARM_ALLOW_WHILE_IDLE_INEXACT_WAKEUP_BASE_PRICE_CAKES);
                this.mActions.put(ACTION_ALARM_WAKEUP_INEXACT_ALLOW_WHILE_IDLE, new EconomicPolicy.Action(ACTION_ALARM_WAKEUP_INEXACT_ALLOW_WHILE_IDLE, getConstantAsCake(this.mParser, properties, "am_action_alarm_allow_while_idle_inexact_wakeup_ctp", EconomyManager.DEFAULT_AM_ACTION_ALARM_ALLOW_WHILE_IDLE_INEXACT_WAKEUP_CTP_CAKES), inexactAllowWhileIdleWakeupBasePrice));
                this.mActions.put(ACTION_ALARM_WAKEUP_INEXACT, new EconomicPolicy.Action(ACTION_ALARM_WAKEUP_INEXACT, getConstantAsCake(this.mParser, properties, "am_action_alarm_inexact_wakeup_ctp", EconomyManager.DEFAULT_AM_ACTION_ALARM_INEXACT_WAKEUP_CTP_CAKES), getConstantAsCake(this.mParser, properties, "am_action_alarm_inexact_wakeup_base_price", EconomyManager.DEFAULT_AM_ACTION_ALARM_INEXACT_WAKEUP_BASE_PRICE_CAKES)));
                long exactAllowWhileIdleNonWakeupBasePrice = getConstantAsCake(this.mParser, properties, "am_action_alarm_allow_while_idle_exact_nonwakeup_base_price", EconomyManager.DEFAULT_AM_ACTION_ALARM_ALLOW_WHILE_IDLE_INEXACT_NONWAKEUP_BASE_PRICE_CAKES);
                this.mActions.put(ACTION_ALARM_NONWAKEUP_EXACT_ALLOW_WHILE_IDLE, new EconomicPolicy.Action(ACTION_ALARM_NONWAKEUP_EXACT_ALLOW_WHILE_IDLE, getConstantAsCake(this.mParser, properties, "am_action_alarm_allow_while_idle_exact_nonwakeup_ctp", EconomyManager.DEFAULT_AM_ACTION_ALARM_ALLOW_WHILE_IDLE_EXACT_NONWAKEUP_CTP_CAKES), exactAllowWhileIdleNonWakeupBasePrice));
                this.mActions.put(ACTION_ALARM_NONWAKEUP_EXACT, new EconomicPolicy.Action(ACTION_ALARM_NONWAKEUP_EXACT, getConstantAsCake(this.mParser, properties, "am_action_alarm_exact_nonwakeup_ctp", EconomyManager.DEFAULT_AM_ACTION_ALARM_EXACT_NONWAKEUP_CTP_CAKES), getConstantAsCake(this.mParser, properties, "am_action_alarm_exact_nonwakeup_base_price", EconomyManager.DEFAULT_AM_ACTION_ALARM_EXACT_NONWAKEUP_BASE_PRICE_CAKES)));
                long inexactAllowWhileIdleNonWakeupBasePrice = getConstantAsCake(this.mParser, properties, "am_action_alarm_allow_while_idle_inexact_nonwakeup_base_price", EconomyManager.DEFAULT_AM_ACTION_ALARM_ALLOW_WHILE_IDLE_INEXACT_NONWAKEUP_BASE_PRICE_CAKES);
                long inexactAllowWhileIdleNonWakeupCtp = getConstantAsCake(this.mParser, properties, "am_action_alarm_allow_while_idle_inexact_nonwakeup_ctp", EconomyManager.DEFAULT_AM_ACTION_ALARM_ALLOW_WHILE_IDLE_INEXACT_NONWAKEUP_CTP_CAKES);
                this.mActions.put(ACTION_ALARM_NONWAKEUP_INEXACT_ALLOW_WHILE_IDLE, new EconomicPolicy.Action(ACTION_ALARM_NONWAKEUP_INEXACT_ALLOW_WHILE_IDLE, inexactAllowWhileIdleNonWakeupCtp, inexactAllowWhileIdleNonWakeupBasePrice));
                this.mActions.put(ACTION_ALARM_NONWAKEUP_INEXACT, new EconomicPolicy.Action(ACTION_ALARM_NONWAKEUP_INEXACT, getConstantAsCake(this.mParser, properties, "am_action_alarm_inexact_nonwakeup_ctp", EconomyManager.DEFAULT_AM_ACTION_ALARM_INEXACT_NONWAKEUP_CTP_CAKES), getConstantAsCake(this.mParser, properties, "am_action_alarm_inexact_nonwakeup_base_price", EconomyManager.DEFAULT_AM_ACTION_ALARM_INEXACT_NONWAKEUP_BASE_PRICE_CAKES)));
                this.mActions.put(ACTION_ALARM_CLOCK, new EconomicPolicy.Action(ACTION_ALARM_CLOCK, getConstantAsCake(this.mParser, properties, "am_action_alarm_alarmclock_ctp", EconomyManager.DEFAULT_AM_ACTION_ALARM_ALARMCLOCK_CTP_CAKES), getConstantAsCake(this.mParser, properties, "am_action_alarm_alarmclock_base_price", EconomyManager.DEFAULT_AM_ACTION_ALARM_ALARMCLOCK_BASE_PRICE_CAKES)));
                this.mRewards.put(AudioDevice.IN_AMBIENT, new EconomicPolicy.Reward(AudioDevice.IN_AMBIENT, getConstantAsCake(this.mParser, properties, "am_reward_top_activity_instant", EconomyManager.DEFAULT_AM_REWARD_TOP_ACTIVITY_INSTANT_CAKES), getConstantAsCake(this.mParser, properties, "am_reward_top_activity_ongoing", 10000000L), getConstantAsCake(this.mParser, properties, "am_reward_top_activity_max", EconomyManager.DEFAULT_AM_REWARD_TOP_ACTIVITY_MAX_CAKES)));
                this.mRewards.put(Integer.MIN_VALUE, new EconomicPolicy.Reward(Integer.MIN_VALUE, getConstantAsCake(this.mParser, properties, "am_reward_notification_seen_instant", EconomyManager.DEFAULT_AM_REWARD_NOTIFICATION_SEEN_INSTANT_CAKES), getConstantAsCake(this.mParser, properties, "am_reward_notification_seen_ongoing", EconomyManager.DEFAULT_AM_REWARD_NOTIFICATION_SEEN_ONGOING_CAKES), getConstantAsCake(this.mParser, properties, "am_reward_notification_seen_max", EconomyManager.DEFAULT_AM_REWARD_NOTIFICATION_SEEN_MAX_CAKES)));
                this.mRewards.put(-2147483647, new EconomicPolicy.Reward(-2147483647, getConstantAsCake(this.mParser, properties, "am_reward_notification_interaction_instant", EconomyManager.DEFAULT_AM_REWARD_NOTIFICATION_INTERACTION_INSTANT_CAKES), getConstantAsCake(this.mParser, properties, "am_reward_notification_interaction_ongoing", EconomyManager.DEFAULT_AM_REWARD_NOTIFICATION_INTERACTION_ONGOING_CAKES), getConstantAsCake(this.mParser, properties, "am_reward_notification_interaction_max", EconomyManager.DEFAULT_AM_REWARD_NOTIFICATION_INTERACTION_MAX_CAKES)));
                this.mRewards.put(AudioChannelMask.INDEX_MASK_2, new EconomicPolicy.Reward(AudioChannelMask.INDEX_MASK_2, getConstantAsCake(this.mParser, properties, "am_reward_widget_interaction_instant", EconomyManager.DEFAULT_AM_REWARD_WIDGET_INTERACTION_INSTANT_CAKES), getConstantAsCake(this.mParser, properties, "am_reward_widget_interaction_ongoing", EconomyManager.DEFAULT_AM_REWARD_WIDGET_INTERACTION_ONGOING_CAKES), getConstantAsCake(this.mParser, properties, "am_reward_widget_interaction_max", EconomyManager.DEFAULT_AM_REWARD_WIDGET_INTERACTION_MAX_CAKES)));
                this.mRewards.put(AudioDevice.IN_BUILTIN_MIC, new EconomicPolicy.Reward(AudioDevice.IN_BUILTIN_MIC, getConstantAsCake(this.mParser, properties, "am_reward_other_user_interaction_instant", EconomyManager.DEFAULT_AM_REWARD_OTHER_USER_INTERACTION_INSTANT_CAKES), getConstantAsCake(this.mParser, properties, "am_reward_other_user_interaction_ongoing", EconomyManager.DEFAULT_AM_REWARD_OTHER_USER_INTERACTION_ONGOING_CAKES), getConstantAsCake(this.mParser, properties, "am_reward_other_user_interaction_max", EconomyManager.DEFAULT_AM_REWARD_OTHER_USER_INTERACTION_MAX_CAKES)));
            }
        } catch (IllegalArgumentException e2) {
            e = e2;
        }
        this.mMinSatiatedBalanceExempted = getConstantAsCake(this.mParser, properties, "am_min_satiated_balance_exempted", EconomyManager.DEFAULT_AM_MIN_SATIATED_BALANCE_EXEMPTED_CAKES);
        this.mMinSatiatedBalanceOther = getConstantAsCake(this.mParser, properties, "am_min_satiated_balance_other_app", EconomyManager.DEFAULT_AM_MIN_SATIATED_BALANCE_OTHER_APP_CAKES);
        this.mMaxSatiatedBalance = getConstantAsCake(this.mParser, properties, "am_max_satiated_balance", EconomyManager.DEFAULT_AM_MAX_SATIATED_BALANCE_CAKES);
        long constantAsCake2 = getConstantAsCake(this.mParser, properties, "am_initial_consumption_limit", EconomyManager.DEFAULT_AM_INITIAL_CONSUMPTION_LIMIT_CAKES);
        this.mInitialSatiatedConsumptionLimit = constantAsCake2;
        this.mHardSatiatedConsumptionLimit = Math.max(constantAsCake2, getConstantAsCake(this.mParser, properties, "am_hard_consumption_limit", EconomyManager.DEFAULT_AM_HARD_CONSUMPTION_LIMIT_CAKES));
        long exactAllowWhileIdleWakeupBasePrice2 = getConstantAsCake(this.mParser, properties, "am_action_alarm_allow_while_idle_exact_wakeup_base_price", EconomyManager.DEFAULT_AM_ACTION_ALARM_ALLOW_WHILE_IDLE_EXACT_WAKEUP_BASE_PRICE_CAKES);
        this.mActions.put(1073741824, new EconomicPolicy.Action(1073741824, getConstantAsCake(this.mParser, properties, "am_action_alarm_allow_while_idle_exact_wakeup_ctp", EconomyManager.DEFAULT_AM_ACTION_ALARM_ALLOW_WHILE_IDLE_EXACT_WAKEUP_CTP_CAKES), exactAllowWhileIdleWakeupBasePrice2));
        this.mActions.put(1073741825, new EconomicPolicy.Action(1073741825, getConstantAsCake(this.mParser, properties, "am_action_alarm_exact_wakeup_ctp", EconomyManager.DEFAULT_AM_ACTION_ALARM_EXACT_WAKEUP_CTP_CAKES), getConstantAsCake(this.mParser, properties, "am_action_alarm_exact_wakeup_base_price", EconomyManager.DEFAULT_AM_ACTION_ALARM_EXACT_WAKEUP_BASE_PRICE_CAKES)));
        long inexactAllowWhileIdleWakeupBasePrice2 = getConstantAsCake(this.mParser, properties, "am_action_alarm_allow_while_idle_inexact_wakeup_base_price", EconomyManager.DEFAULT_AM_ACTION_ALARM_ALLOW_WHILE_IDLE_INEXACT_WAKEUP_BASE_PRICE_CAKES);
        this.mActions.put(ACTION_ALARM_WAKEUP_INEXACT_ALLOW_WHILE_IDLE, new EconomicPolicy.Action(ACTION_ALARM_WAKEUP_INEXACT_ALLOW_WHILE_IDLE, getConstantAsCake(this.mParser, properties, "am_action_alarm_allow_while_idle_inexact_wakeup_ctp", EconomyManager.DEFAULT_AM_ACTION_ALARM_ALLOW_WHILE_IDLE_INEXACT_WAKEUP_CTP_CAKES), inexactAllowWhileIdleWakeupBasePrice2));
        this.mActions.put(ACTION_ALARM_WAKEUP_INEXACT, new EconomicPolicy.Action(ACTION_ALARM_WAKEUP_INEXACT, getConstantAsCake(this.mParser, properties, "am_action_alarm_inexact_wakeup_ctp", EconomyManager.DEFAULT_AM_ACTION_ALARM_INEXACT_WAKEUP_CTP_CAKES), getConstantAsCake(this.mParser, properties, "am_action_alarm_inexact_wakeup_base_price", EconomyManager.DEFAULT_AM_ACTION_ALARM_INEXACT_WAKEUP_BASE_PRICE_CAKES)));
        long exactAllowWhileIdleNonWakeupBasePrice2 = getConstantAsCake(this.mParser, properties, "am_action_alarm_allow_while_idle_exact_nonwakeup_base_price", EconomyManager.DEFAULT_AM_ACTION_ALARM_ALLOW_WHILE_IDLE_INEXACT_NONWAKEUP_BASE_PRICE_CAKES);
        this.mActions.put(ACTION_ALARM_NONWAKEUP_EXACT_ALLOW_WHILE_IDLE, new EconomicPolicy.Action(ACTION_ALARM_NONWAKEUP_EXACT_ALLOW_WHILE_IDLE, getConstantAsCake(this.mParser, properties, "am_action_alarm_allow_while_idle_exact_nonwakeup_ctp", EconomyManager.DEFAULT_AM_ACTION_ALARM_ALLOW_WHILE_IDLE_EXACT_NONWAKEUP_CTP_CAKES), exactAllowWhileIdleNonWakeupBasePrice2));
        this.mActions.put(ACTION_ALARM_NONWAKEUP_EXACT, new EconomicPolicy.Action(ACTION_ALARM_NONWAKEUP_EXACT, getConstantAsCake(this.mParser, properties, "am_action_alarm_exact_nonwakeup_ctp", EconomyManager.DEFAULT_AM_ACTION_ALARM_EXACT_NONWAKEUP_CTP_CAKES), getConstantAsCake(this.mParser, properties, "am_action_alarm_exact_nonwakeup_base_price", EconomyManager.DEFAULT_AM_ACTION_ALARM_EXACT_NONWAKEUP_BASE_PRICE_CAKES)));
        long inexactAllowWhileIdleNonWakeupBasePrice2 = getConstantAsCake(this.mParser, properties, "am_action_alarm_allow_while_idle_inexact_nonwakeup_base_price", EconomyManager.DEFAULT_AM_ACTION_ALARM_ALLOW_WHILE_IDLE_INEXACT_NONWAKEUP_BASE_PRICE_CAKES);
        long inexactAllowWhileIdleNonWakeupCtp2 = getConstantAsCake(this.mParser, properties, "am_action_alarm_allow_while_idle_inexact_nonwakeup_ctp", EconomyManager.DEFAULT_AM_ACTION_ALARM_ALLOW_WHILE_IDLE_INEXACT_NONWAKEUP_CTP_CAKES);
        this.mActions.put(ACTION_ALARM_NONWAKEUP_INEXACT_ALLOW_WHILE_IDLE, new EconomicPolicy.Action(ACTION_ALARM_NONWAKEUP_INEXACT_ALLOW_WHILE_IDLE, inexactAllowWhileIdleNonWakeupCtp2, inexactAllowWhileIdleNonWakeupBasePrice2));
        this.mActions.put(ACTION_ALARM_NONWAKEUP_INEXACT, new EconomicPolicy.Action(ACTION_ALARM_NONWAKEUP_INEXACT, getConstantAsCake(this.mParser, properties, "am_action_alarm_inexact_nonwakeup_ctp", EconomyManager.DEFAULT_AM_ACTION_ALARM_INEXACT_NONWAKEUP_CTP_CAKES), getConstantAsCake(this.mParser, properties, "am_action_alarm_inexact_nonwakeup_base_price", EconomyManager.DEFAULT_AM_ACTION_ALARM_INEXACT_NONWAKEUP_BASE_PRICE_CAKES)));
        this.mActions.put(ACTION_ALARM_CLOCK, new EconomicPolicy.Action(ACTION_ALARM_CLOCK, getConstantAsCake(this.mParser, properties, "am_action_alarm_alarmclock_ctp", EconomyManager.DEFAULT_AM_ACTION_ALARM_ALARMCLOCK_CTP_CAKES), getConstantAsCake(this.mParser, properties, "am_action_alarm_alarmclock_base_price", EconomyManager.DEFAULT_AM_ACTION_ALARM_ALARMCLOCK_BASE_PRICE_CAKES)));
        this.mRewards.put(AudioDevice.IN_AMBIENT, new EconomicPolicy.Reward(AudioDevice.IN_AMBIENT, getConstantAsCake(this.mParser, properties, "am_reward_top_activity_instant", EconomyManager.DEFAULT_AM_REWARD_TOP_ACTIVITY_INSTANT_CAKES), getConstantAsCake(this.mParser, properties, "am_reward_top_activity_ongoing", 10000000L), getConstantAsCake(this.mParser, properties, "am_reward_top_activity_max", EconomyManager.DEFAULT_AM_REWARD_TOP_ACTIVITY_MAX_CAKES)));
        this.mRewards.put(Integer.MIN_VALUE, new EconomicPolicy.Reward(Integer.MIN_VALUE, getConstantAsCake(this.mParser, properties, "am_reward_notification_seen_instant", EconomyManager.DEFAULT_AM_REWARD_NOTIFICATION_SEEN_INSTANT_CAKES), getConstantAsCake(this.mParser, properties, "am_reward_notification_seen_ongoing", EconomyManager.DEFAULT_AM_REWARD_NOTIFICATION_SEEN_ONGOING_CAKES), getConstantAsCake(this.mParser, properties, "am_reward_notification_seen_max", EconomyManager.DEFAULT_AM_REWARD_NOTIFICATION_SEEN_MAX_CAKES)));
        this.mRewards.put(-2147483647, new EconomicPolicy.Reward(-2147483647, getConstantAsCake(this.mParser, properties, "am_reward_notification_interaction_instant", EconomyManager.DEFAULT_AM_REWARD_NOTIFICATION_INTERACTION_INSTANT_CAKES), getConstantAsCake(this.mParser, properties, "am_reward_notification_interaction_ongoing", EconomyManager.DEFAULT_AM_REWARD_NOTIFICATION_INTERACTION_ONGOING_CAKES), getConstantAsCake(this.mParser, properties, "am_reward_notification_interaction_max", EconomyManager.DEFAULT_AM_REWARD_NOTIFICATION_INTERACTION_MAX_CAKES)));
        this.mRewards.put(AudioChannelMask.INDEX_MASK_2, new EconomicPolicy.Reward(AudioChannelMask.INDEX_MASK_2, getConstantAsCake(this.mParser, properties, "am_reward_widget_interaction_instant", EconomyManager.DEFAULT_AM_REWARD_WIDGET_INTERACTION_INSTANT_CAKES), getConstantAsCake(this.mParser, properties, "am_reward_widget_interaction_ongoing", EconomyManager.DEFAULT_AM_REWARD_WIDGET_INTERACTION_ONGOING_CAKES), getConstantAsCake(this.mParser, properties, "am_reward_widget_interaction_max", EconomyManager.DEFAULT_AM_REWARD_WIDGET_INTERACTION_MAX_CAKES)));
        this.mRewards.put(AudioDevice.IN_BUILTIN_MIC, new EconomicPolicy.Reward(AudioDevice.IN_BUILTIN_MIC, getConstantAsCake(this.mParser, properties, "am_reward_other_user_interaction_instant", EconomyManager.DEFAULT_AM_REWARD_OTHER_USER_INTERACTION_INSTANT_CAKES), getConstantAsCake(this.mParser, properties, "am_reward_other_user_interaction_ongoing", EconomyManager.DEFAULT_AM_REWARD_OTHER_USER_INTERACTION_ONGOING_CAKES), getConstantAsCake(this.mParser, properties, "am_reward_other_user_interaction_max", EconomyManager.DEFAULT_AM_REWARD_OTHER_USER_INTERACTION_MAX_CAKES)));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.tare.EconomicPolicy
    public void dump(IndentingPrintWriter pw) {
        pw.println("Min satiated balances:");
        pw.increaseIndent();
        pw.print("Exempted", TareUtils.cakeToString(this.mMinSatiatedBalanceExempted)).println();
        pw.print("Other", TareUtils.cakeToString(this.mMinSatiatedBalanceOther)).println();
        pw.decreaseIndent();
        pw.print("Max satiated balance", TareUtils.cakeToString(this.mMaxSatiatedBalance)).println();
        pw.print("Consumption limits: [");
        pw.print(TareUtils.cakeToString(this.mInitialSatiatedConsumptionLimit));
        pw.print(", ");
        pw.print(TareUtils.cakeToString(this.mHardSatiatedConsumptionLimit));
        pw.println("]");
        pw.println();
        pw.println("Actions:");
        pw.increaseIndent();
        for (int i = 0; i < this.mActions.size(); i++) {
            dumpAction(pw, this.mActions.valueAt(i));
        }
        pw.decreaseIndent();
        pw.println();
        pw.println("Rewards:");
        pw.increaseIndent();
        for (int i2 = 0; i2 < this.mRewards.size(); i2++) {
            dumpReward(pw, this.mRewards.valueAt(i2));
        }
        pw.decreaseIndent();
    }
}
