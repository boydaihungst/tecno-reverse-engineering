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
public class JobSchedulerEconomicPolicy extends EconomicPolicy {
    public static final int ACTION_JOB_DEFAULT_RUNNING = 1610612741;
    public static final int ACTION_JOB_DEFAULT_START = 1610612740;
    public static final int ACTION_JOB_HIGH_RUNNING = 1610612739;
    public static final int ACTION_JOB_HIGH_START = 1610612738;
    public static final int ACTION_JOB_LOW_RUNNING = 1610612743;
    public static final int ACTION_JOB_LOW_START = 1610612742;
    public static final int ACTION_JOB_MAX_RUNNING = 1610612737;
    public static final int ACTION_JOB_MAX_START = 1610612736;
    public static final int ACTION_JOB_MIN_RUNNING = 1610612745;
    public static final int ACTION_JOB_MIN_START = 1610612744;
    public static final int ACTION_JOB_TIMEOUT = 1610612746;
    private final SparseArray<EconomicPolicy.Action> mActions;
    private long mHardSatiatedConsumptionLimit;
    private long mInitialSatiatedConsumptionLimit;
    private final InternalResourceService mInternalResourceService;
    private long mMaxSatiatedBalance;
    private long mMinSatiatedBalanceExempted;
    private long mMinSatiatedBalanceOther;
    private final KeyValueListParser mParser;
    private final SparseArray<EconomicPolicy.Reward> mRewards;
    private static final String TAG = "TARE- " + JobSchedulerEconomicPolicy.class.getSimpleName();
    private static final int[] COST_MODIFIERS = {0, 1, 2, 3};

    /* JADX INFO: Access modifiers changed from: package-private */
    public JobSchedulerEconomicPolicy(InternalResourceService irs) {
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
        loadConstants(Settings.Global.getString(resolver, "tare_job_scheduler_constants"), properties);
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
                this.mMinSatiatedBalanceExempted = getConstantAsCake(this.mParser, properties, "js_min_satiated_balance_exempted", EconomyManager.DEFAULT_JS_MIN_SATIATED_BALANCE_EXEMPTED_CAKES);
                this.mMinSatiatedBalanceOther = getConstantAsCake(this.mParser, properties, "js_min_satiated_balance_other_app", EconomyManager.DEFAULT_JS_MIN_SATIATED_BALANCE_OTHER_APP_CAKES);
                this.mMaxSatiatedBalance = getConstantAsCake(this.mParser, properties, "js_max_satiated_balance", EconomyManager.DEFAULT_JS_MAX_SATIATED_BALANCE_CAKES);
                long constantAsCake = getConstantAsCake(this.mParser, properties, "js_initial_consumption_limit", EconomyManager.DEFAULT_JS_INITIAL_CONSUMPTION_LIMIT_CAKES);
                this.mInitialSatiatedConsumptionLimit = constantAsCake;
                this.mHardSatiatedConsumptionLimit = Math.max(constantAsCake, getConstantAsCake(this.mParser, properties, "js_hard_consumption_limit", EconomyManager.DEFAULT_JS_HARD_CONSUMPTION_LIMIT_CAKES));
                this.mActions.put(ACTION_JOB_MAX_START, new EconomicPolicy.Action(ACTION_JOB_MAX_START, getConstantAsCake(this.mParser, properties, "js_action_job_max_start_ctp", EconomyManager.DEFAULT_JS_ACTION_JOB_MAX_START_CTP_CAKES), getConstantAsCake(this.mParser, properties, "js_action_job_max_start_base_price", EconomyManager.DEFAULT_JS_ACTION_JOB_MAX_START_BASE_PRICE_CAKES)));
                this.mActions.put(ACTION_JOB_MAX_RUNNING, new EconomicPolicy.Action(ACTION_JOB_MAX_RUNNING, getConstantAsCake(this.mParser, properties, "js_action_job_max_running_ctp", EconomyManager.DEFAULT_JS_ACTION_JOB_MAX_RUNNING_CTP_CAKES), getConstantAsCake(this.mParser, properties, "js_action_job_max_running_base_price", EconomyManager.DEFAULT_JS_ACTION_JOB_MAX_RUNNING_BASE_PRICE_CAKES)));
                this.mActions.put(ACTION_JOB_HIGH_START, new EconomicPolicy.Action(ACTION_JOB_HIGH_START, getConstantAsCake(this.mParser, properties, "js_action_job_high_start_ctp", EconomyManager.DEFAULT_JS_ACTION_JOB_HIGH_START_CTP_CAKES), getConstantAsCake(this.mParser, properties, "js_action_job_high_start_base_price", EconomyManager.DEFAULT_JS_ACTION_JOB_HIGH_START_BASE_PRICE_CAKES)));
                this.mActions.put(ACTION_JOB_HIGH_RUNNING, new EconomicPolicy.Action(ACTION_JOB_HIGH_RUNNING, getConstantAsCake(this.mParser, properties, "js_action_job_high_running_ctp", EconomyManager.DEFAULT_JS_ACTION_JOB_HIGH_RUNNING_CTP_CAKES), getConstantAsCake(this.mParser, properties, "js_action_job_high_running_base_price", EconomyManager.DEFAULT_JS_ACTION_JOB_HIGH_RUNNING_BASE_PRICE_CAKES)));
                this.mActions.put(ACTION_JOB_DEFAULT_START, new EconomicPolicy.Action(ACTION_JOB_DEFAULT_START, getConstantAsCake(this.mParser, properties, "js_action_job_default_start_ctp", EconomyManager.DEFAULT_JS_ACTION_JOB_DEFAULT_START_CTP_CAKES), getConstantAsCake(this.mParser, properties, "js_action_job_default_start_base_price", EconomyManager.DEFAULT_JS_ACTION_JOB_DEFAULT_START_BASE_PRICE_CAKES)));
                this.mActions.put(ACTION_JOB_DEFAULT_RUNNING, new EconomicPolicy.Action(ACTION_JOB_DEFAULT_RUNNING, getConstantAsCake(this.mParser, properties, "js_action_job_default_running_ctp", EconomyManager.DEFAULT_JS_ACTION_JOB_DEFAULT_RUNNING_CTP_CAKES), getConstantAsCake(this.mParser, properties, "js_action_job_default_running_base_price", EconomyManager.DEFAULT_JS_ACTION_JOB_DEFAULT_RUNNING_BASE_PRICE_CAKES)));
                this.mActions.put(ACTION_JOB_LOW_START, new EconomicPolicy.Action(ACTION_JOB_LOW_START, getConstantAsCake(this.mParser, properties, "js_action_job_low_start_ctp", EconomyManager.DEFAULT_JS_ACTION_JOB_LOW_START_CTP_CAKES), getConstantAsCake(this.mParser, properties, "js_action_job_low_start_base_price", EconomyManager.DEFAULT_JS_ACTION_JOB_LOW_START_BASE_PRICE_CAKES)));
                this.mActions.put(ACTION_JOB_LOW_RUNNING, new EconomicPolicy.Action(ACTION_JOB_LOW_RUNNING, getConstantAsCake(this.mParser, properties, "js_action_job_low_running_ctp", EconomyManager.DEFAULT_JS_ACTION_JOB_LOW_RUNNING_CTP_CAKES), getConstantAsCake(this.mParser, properties, "js_action_job_low_running_base_price", EconomyManager.DEFAULT_JS_ACTION_JOB_LOW_RUNNING_BASE_PRICE_CAKES)));
                this.mActions.put(ACTION_JOB_MIN_START, new EconomicPolicy.Action(ACTION_JOB_MIN_START, getConstantAsCake(this.mParser, properties, "js_action_job_min_start_ctp", EconomyManager.DEFAULT_JS_ACTION_JOB_MIN_START_CTP_CAKES), getConstantAsCake(this.mParser, properties, "js_action_job_min_start_base_price", EconomyManager.DEFAULT_JS_ACTION_JOB_MIN_START_BASE_PRICE_CAKES)));
                this.mActions.put(ACTION_JOB_MIN_RUNNING, new EconomicPolicy.Action(ACTION_JOB_MIN_RUNNING, getConstantAsCake(this.mParser, properties, "js_action_job_min_running_ctp", EconomyManager.DEFAULT_JS_ACTION_JOB_MIN_RUNNING_CTP_CAKES), getConstantAsCake(this.mParser, properties, "js_action_job_min_running_base_price", EconomyManager.DEFAULT_JS_ACTION_JOB_MIN_RUNNING_BASE_PRICE_CAKES)));
                this.mActions.put(ACTION_JOB_TIMEOUT, new EconomicPolicy.Action(ACTION_JOB_TIMEOUT, getConstantAsCake(this.mParser, properties, "js_action_job_timeout_penalty_ctp", EconomyManager.DEFAULT_JS_ACTION_JOB_TIMEOUT_PENALTY_CTP_CAKES), getConstantAsCake(this.mParser, properties, "js_action_job_timeout_penalty_base_price", EconomyManager.DEFAULT_JS_ACTION_JOB_TIMEOUT_PENALTY_BASE_PRICE_CAKES)));
                this.mRewards.put(AudioDevice.IN_AMBIENT, new EconomicPolicy.Reward(AudioDevice.IN_AMBIENT, getConstantAsCake(this.mParser, properties, "js_reward_top_activity_instant", EconomyManager.DEFAULT_JS_REWARD_TOP_ACTIVITY_INSTANT_CAKES), getConstantAsCake(this.mParser, properties, "js_reward_top_activity_ongoing", 500000000L), getConstantAsCake(this.mParser, properties, "js_reward_top_activity_max", EconomyManager.DEFAULT_JS_REWARD_TOP_ACTIVITY_MAX_CAKES)));
                this.mRewards.put(Integer.MIN_VALUE, new EconomicPolicy.Reward(Integer.MIN_VALUE, getConstantAsCake(this.mParser, properties, "js_reward_notification_seen_instant", EconomyManager.DEFAULT_JS_REWARD_NOTIFICATION_SEEN_INSTANT_CAKES), getConstantAsCake(this.mParser, properties, "js_reward_notification_seen_ongoing", EconomyManager.DEFAULT_JS_REWARD_NOTIFICATION_SEEN_ONGOING_CAKES), getConstantAsCake(this.mParser, properties, "js_reward_notification_seen_max", EconomyManager.DEFAULT_JS_REWARD_NOTIFICATION_SEEN_MAX_CAKES)));
                this.mRewards.put(-2147483647, new EconomicPolicy.Reward(-2147483647, getConstantAsCake(this.mParser, properties, "js_reward_notification_interaction_instant", EconomyManager.DEFAULT_JS_REWARD_NOTIFICATION_INTERACTION_INSTANT_CAKES), getConstantAsCake(this.mParser, properties, "js_reward_notification_interaction_ongoing", EconomyManager.DEFAULT_JS_REWARD_NOTIFICATION_INTERACTION_ONGOING_CAKES), getConstantAsCake(this.mParser, properties, "js_reward_notification_interaction_max", EconomyManager.DEFAULT_JS_REWARD_NOTIFICATION_INTERACTION_MAX_CAKES)));
                this.mRewards.put(AudioChannelMask.INDEX_MASK_2, new EconomicPolicy.Reward(AudioChannelMask.INDEX_MASK_2, getConstantAsCake(this.mParser, properties, "js_reward_widget_interaction_instant", EconomyManager.DEFAULT_JS_REWARD_WIDGET_INTERACTION_INSTANT_CAKES), getConstantAsCake(this.mParser, properties, "js_reward_widget_interaction_ongoing", EconomyManager.DEFAULT_JS_REWARD_WIDGET_INTERACTION_ONGOING_CAKES), getConstantAsCake(this.mParser, properties, "js_reward_widget_interaction_max", EconomyManager.DEFAULT_JS_REWARD_WIDGET_INTERACTION_MAX_CAKES)));
                this.mRewards.put(AudioDevice.IN_BUILTIN_MIC, new EconomicPolicy.Reward(AudioDevice.IN_BUILTIN_MIC, getConstantAsCake(this.mParser, properties, "js_reward_other_user_interaction_instant", EconomyManager.DEFAULT_JS_REWARD_OTHER_USER_INTERACTION_INSTANT_CAKES), getConstantAsCake(this.mParser, properties, "js_reward_other_user_interaction_ongoing", EconomyManager.DEFAULT_JS_REWARD_OTHER_USER_INTERACTION_ONGOING_CAKES), getConstantAsCake(this.mParser, properties, "js_reward_other_user_interaction_max", EconomyManager.DEFAULT_JS_REWARD_OTHER_USER_INTERACTION_MAX_CAKES)));
            }
        } catch (IllegalArgumentException e2) {
            e = e2;
        }
        this.mMinSatiatedBalanceExempted = getConstantAsCake(this.mParser, properties, "js_min_satiated_balance_exempted", EconomyManager.DEFAULT_JS_MIN_SATIATED_BALANCE_EXEMPTED_CAKES);
        this.mMinSatiatedBalanceOther = getConstantAsCake(this.mParser, properties, "js_min_satiated_balance_other_app", EconomyManager.DEFAULT_JS_MIN_SATIATED_BALANCE_OTHER_APP_CAKES);
        this.mMaxSatiatedBalance = getConstantAsCake(this.mParser, properties, "js_max_satiated_balance", EconomyManager.DEFAULT_JS_MAX_SATIATED_BALANCE_CAKES);
        long constantAsCake2 = getConstantAsCake(this.mParser, properties, "js_initial_consumption_limit", EconomyManager.DEFAULT_JS_INITIAL_CONSUMPTION_LIMIT_CAKES);
        this.mInitialSatiatedConsumptionLimit = constantAsCake2;
        this.mHardSatiatedConsumptionLimit = Math.max(constantAsCake2, getConstantAsCake(this.mParser, properties, "js_hard_consumption_limit", EconomyManager.DEFAULT_JS_HARD_CONSUMPTION_LIMIT_CAKES));
        this.mActions.put(ACTION_JOB_MAX_START, new EconomicPolicy.Action(ACTION_JOB_MAX_START, getConstantAsCake(this.mParser, properties, "js_action_job_max_start_ctp", EconomyManager.DEFAULT_JS_ACTION_JOB_MAX_START_CTP_CAKES), getConstantAsCake(this.mParser, properties, "js_action_job_max_start_base_price", EconomyManager.DEFAULT_JS_ACTION_JOB_MAX_START_BASE_PRICE_CAKES)));
        this.mActions.put(ACTION_JOB_MAX_RUNNING, new EconomicPolicy.Action(ACTION_JOB_MAX_RUNNING, getConstantAsCake(this.mParser, properties, "js_action_job_max_running_ctp", EconomyManager.DEFAULT_JS_ACTION_JOB_MAX_RUNNING_CTP_CAKES), getConstantAsCake(this.mParser, properties, "js_action_job_max_running_base_price", EconomyManager.DEFAULT_JS_ACTION_JOB_MAX_RUNNING_BASE_PRICE_CAKES)));
        this.mActions.put(ACTION_JOB_HIGH_START, new EconomicPolicy.Action(ACTION_JOB_HIGH_START, getConstantAsCake(this.mParser, properties, "js_action_job_high_start_ctp", EconomyManager.DEFAULT_JS_ACTION_JOB_HIGH_START_CTP_CAKES), getConstantAsCake(this.mParser, properties, "js_action_job_high_start_base_price", EconomyManager.DEFAULT_JS_ACTION_JOB_HIGH_START_BASE_PRICE_CAKES)));
        this.mActions.put(ACTION_JOB_HIGH_RUNNING, new EconomicPolicy.Action(ACTION_JOB_HIGH_RUNNING, getConstantAsCake(this.mParser, properties, "js_action_job_high_running_ctp", EconomyManager.DEFAULT_JS_ACTION_JOB_HIGH_RUNNING_CTP_CAKES), getConstantAsCake(this.mParser, properties, "js_action_job_high_running_base_price", EconomyManager.DEFAULT_JS_ACTION_JOB_HIGH_RUNNING_BASE_PRICE_CAKES)));
        this.mActions.put(ACTION_JOB_DEFAULT_START, new EconomicPolicy.Action(ACTION_JOB_DEFAULT_START, getConstantAsCake(this.mParser, properties, "js_action_job_default_start_ctp", EconomyManager.DEFAULT_JS_ACTION_JOB_DEFAULT_START_CTP_CAKES), getConstantAsCake(this.mParser, properties, "js_action_job_default_start_base_price", EconomyManager.DEFAULT_JS_ACTION_JOB_DEFAULT_START_BASE_PRICE_CAKES)));
        this.mActions.put(ACTION_JOB_DEFAULT_RUNNING, new EconomicPolicy.Action(ACTION_JOB_DEFAULT_RUNNING, getConstantAsCake(this.mParser, properties, "js_action_job_default_running_ctp", EconomyManager.DEFAULT_JS_ACTION_JOB_DEFAULT_RUNNING_CTP_CAKES), getConstantAsCake(this.mParser, properties, "js_action_job_default_running_base_price", EconomyManager.DEFAULT_JS_ACTION_JOB_DEFAULT_RUNNING_BASE_PRICE_CAKES)));
        this.mActions.put(ACTION_JOB_LOW_START, new EconomicPolicy.Action(ACTION_JOB_LOW_START, getConstantAsCake(this.mParser, properties, "js_action_job_low_start_ctp", EconomyManager.DEFAULT_JS_ACTION_JOB_LOW_START_CTP_CAKES), getConstantAsCake(this.mParser, properties, "js_action_job_low_start_base_price", EconomyManager.DEFAULT_JS_ACTION_JOB_LOW_START_BASE_PRICE_CAKES)));
        this.mActions.put(ACTION_JOB_LOW_RUNNING, new EconomicPolicy.Action(ACTION_JOB_LOW_RUNNING, getConstantAsCake(this.mParser, properties, "js_action_job_low_running_ctp", EconomyManager.DEFAULT_JS_ACTION_JOB_LOW_RUNNING_CTP_CAKES), getConstantAsCake(this.mParser, properties, "js_action_job_low_running_base_price", EconomyManager.DEFAULT_JS_ACTION_JOB_LOW_RUNNING_BASE_PRICE_CAKES)));
        this.mActions.put(ACTION_JOB_MIN_START, new EconomicPolicy.Action(ACTION_JOB_MIN_START, getConstantAsCake(this.mParser, properties, "js_action_job_min_start_ctp", EconomyManager.DEFAULT_JS_ACTION_JOB_MIN_START_CTP_CAKES), getConstantAsCake(this.mParser, properties, "js_action_job_min_start_base_price", EconomyManager.DEFAULT_JS_ACTION_JOB_MIN_START_BASE_PRICE_CAKES)));
        this.mActions.put(ACTION_JOB_MIN_RUNNING, new EconomicPolicy.Action(ACTION_JOB_MIN_RUNNING, getConstantAsCake(this.mParser, properties, "js_action_job_min_running_ctp", EconomyManager.DEFAULT_JS_ACTION_JOB_MIN_RUNNING_CTP_CAKES), getConstantAsCake(this.mParser, properties, "js_action_job_min_running_base_price", EconomyManager.DEFAULT_JS_ACTION_JOB_MIN_RUNNING_BASE_PRICE_CAKES)));
        this.mActions.put(ACTION_JOB_TIMEOUT, new EconomicPolicy.Action(ACTION_JOB_TIMEOUT, getConstantAsCake(this.mParser, properties, "js_action_job_timeout_penalty_ctp", EconomyManager.DEFAULT_JS_ACTION_JOB_TIMEOUT_PENALTY_CTP_CAKES), getConstantAsCake(this.mParser, properties, "js_action_job_timeout_penalty_base_price", EconomyManager.DEFAULT_JS_ACTION_JOB_TIMEOUT_PENALTY_BASE_PRICE_CAKES)));
        this.mRewards.put(AudioDevice.IN_AMBIENT, new EconomicPolicy.Reward(AudioDevice.IN_AMBIENT, getConstantAsCake(this.mParser, properties, "js_reward_top_activity_instant", EconomyManager.DEFAULT_JS_REWARD_TOP_ACTIVITY_INSTANT_CAKES), getConstantAsCake(this.mParser, properties, "js_reward_top_activity_ongoing", 500000000L), getConstantAsCake(this.mParser, properties, "js_reward_top_activity_max", EconomyManager.DEFAULT_JS_REWARD_TOP_ACTIVITY_MAX_CAKES)));
        this.mRewards.put(Integer.MIN_VALUE, new EconomicPolicy.Reward(Integer.MIN_VALUE, getConstantAsCake(this.mParser, properties, "js_reward_notification_seen_instant", EconomyManager.DEFAULT_JS_REWARD_NOTIFICATION_SEEN_INSTANT_CAKES), getConstantAsCake(this.mParser, properties, "js_reward_notification_seen_ongoing", EconomyManager.DEFAULT_JS_REWARD_NOTIFICATION_SEEN_ONGOING_CAKES), getConstantAsCake(this.mParser, properties, "js_reward_notification_seen_max", EconomyManager.DEFAULT_JS_REWARD_NOTIFICATION_SEEN_MAX_CAKES)));
        this.mRewards.put(-2147483647, new EconomicPolicy.Reward(-2147483647, getConstantAsCake(this.mParser, properties, "js_reward_notification_interaction_instant", EconomyManager.DEFAULT_JS_REWARD_NOTIFICATION_INTERACTION_INSTANT_CAKES), getConstantAsCake(this.mParser, properties, "js_reward_notification_interaction_ongoing", EconomyManager.DEFAULT_JS_REWARD_NOTIFICATION_INTERACTION_ONGOING_CAKES), getConstantAsCake(this.mParser, properties, "js_reward_notification_interaction_max", EconomyManager.DEFAULT_JS_REWARD_NOTIFICATION_INTERACTION_MAX_CAKES)));
        this.mRewards.put(AudioChannelMask.INDEX_MASK_2, new EconomicPolicy.Reward(AudioChannelMask.INDEX_MASK_2, getConstantAsCake(this.mParser, properties, "js_reward_widget_interaction_instant", EconomyManager.DEFAULT_JS_REWARD_WIDGET_INTERACTION_INSTANT_CAKES), getConstantAsCake(this.mParser, properties, "js_reward_widget_interaction_ongoing", EconomyManager.DEFAULT_JS_REWARD_WIDGET_INTERACTION_ONGOING_CAKES), getConstantAsCake(this.mParser, properties, "js_reward_widget_interaction_max", EconomyManager.DEFAULT_JS_REWARD_WIDGET_INTERACTION_MAX_CAKES)));
        this.mRewards.put(AudioDevice.IN_BUILTIN_MIC, new EconomicPolicy.Reward(AudioDevice.IN_BUILTIN_MIC, getConstantAsCake(this.mParser, properties, "js_reward_other_user_interaction_instant", EconomyManager.DEFAULT_JS_REWARD_OTHER_USER_INTERACTION_INSTANT_CAKES), getConstantAsCake(this.mParser, properties, "js_reward_other_user_interaction_ongoing", EconomyManager.DEFAULT_JS_REWARD_OTHER_USER_INTERACTION_ONGOING_CAKES), getConstantAsCake(this.mParser, properties, "js_reward_other_user_interaction_max", EconomyManager.DEFAULT_JS_REWARD_OTHER_USER_INTERACTION_MAX_CAKES)));
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
