package com.android.server.tare;

import android.provider.DeviceConfig;
import android.util.ArraySet;
import android.util.IndentingPrintWriter;
import android.util.SparseArray;
import com.android.server.tare.EconomicPolicy;
import libcore.util.EmptyArray;
/* loaded from: classes2.dex */
public class CompleteEconomicPolicy extends EconomicPolicy {
    private final SparseArray<EconomicPolicy.Action> mActions;
    private long mConsumptionLimit;
    private final int[] mCostModifiers;
    private final ArraySet<EconomicPolicy> mEnabledEconomicPolicies;
    private long mMaxSatiatedBalance;
    private final SparseArray<EconomicPolicy.Reward> mRewards;

    /* JADX INFO: Access modifiers changed from: package-private */
    public CompleteEconomicPolicy(InternalResourceService irs) {
        super(irs);
        ArraySet<EconomicPolicy> arraySet = new ArraySet<>();
        this.mEnabledEconomicPolicies = arraySet;
        this.mActions = new SparseArray<>();
        this.mRewards = new SparseArray<>();
        arraySet.add(new AlarmManagerEconomicPolicy(irs));
        arraySet.add(new JobSchedulerEconomicPolicy(irs));
        ArraySet<Integer> costModifiers = new ArraySet<>();
        for (int i = 0; i < this.mEnabledEconomicPolicies.size(); i++) {
            int[] sm = this.mEnabledEconomicPolicies.valueAt(i).getCostModifiers();
            for (int s : sm) {
                costModifiers.add(Integer.valueOf(s));
            }
        }
        int i2 = costModifiers.size();
        this.mCostModifiers = new int[i2];
        for (int i3 = 0; i3 < costModifiers.size(); i3++) {
            this.mCostModifiers[i3] = costModifiers.valueAt(i3).intValue();
        }
        updateMaxBalances();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.tare.EconomicPolicy
    public void setup(DeviceConfig.Properties properties) {
        super.setup(properties);
        for (int i = 0; i < this.mEnabledEconomicPolicies.size(); i++) {
            this.mEnabledEconomicPolicies.valueAt(i).setup(properties);
        }
        updateMaxBalances();
    }

    private void updateMaxBalances() {
        long max = 0;
        for (int i = 0; i < this.mEnabledEconomicPolicies.size(); i++) {
            max += this.mEnabledEconomicPolicies.valueAt(i).getMaxSatiatedBalance();
        }
        this.mMaxSatiatedBalance = max;
        long max2 = 0;
        for (int i2 = 0; i2 < this.mEnabledEconomicPolicies.size(); i2++) {
            max2 += this.mEnabledEconomicPolicies.valueAt(i2).getInitialSatiatedConsumptionLimit();
        }
        this.mConsumptionLimit = max2;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.tare.EconomicPolicy
    public long getMinSatiatedBalance(int userId, String pkgName) {
        long min = 0;
        for (int i = 0; i < this.mEnabledEconomicPolicies.size(); i++) {
            min += this.mEnabledEconomicPolicies.valueAt(i).getMinSatiatedBalance(userId, pkgName);
        }
        return min;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.tare.EconomicPolicy
    public long getMaxSatiatedBalance() {
        return this.mMaxSatiatedBalance;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.tare.EconomicPolicy
    public long getInitialSatiatedConsumptionLimit() {
        return this.mConsumptionLimit;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.tare.EconomicPolicy
    public long getHardSatiatedConsumptionLimit() {
        return this.mConsumptionLimit;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.tare.EconomicPolicy
    public int[] getCostModifiers() {
        int[] iArr = this.mCostModifiers;
        return iArr == null ? EmptyArray.INT : iArr;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.tare.EconomicPolicy
    public EconomicPolicy.Action getAction(int actionId) {
        if (this.mActions.contains(actionId)) {
            return this.mActions.get(actionId);
        }
        long ctp = 0;
        long price = 0;
        boolean exists = false;
        for (int i = 0; i < this.mEnabledEconomicPolicies.size(); i++) {
            EconomicPolicy.Action a = this.mEnabledEconomicPolicies.valueAt(i).getAction(actionId);
            if (a != null) {
                exists = true;
                ctp += a.costToProduce;
                price += a.basePrice;
            }
        }
        EconomicPolicy.Action action = exists ? new EconomicPolicy.Action(actionId, ctp, price) : null;
        this.mActions.put(actionId, action);
        return action;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.tare.EconomicPolicy
    public EconomicPolicy.Reward getReward(int rewardId) {
        if (this.mRewards.contains(rewardId)) {
            return this.mRewards.get(rewardId);
        }
        long instantReward = 0;
        long ongoingReward = 0;
        long maxReward = 0;
        boolean exists = false;
        for (int i = 0; i < this.mEnabledEconomicPolicies.size(); i++) {
            EconomicPolicy.Reward r = this.mEnabledEconomicPolicies.valueAt(i).getReward(rewardId);
            if (r != null) {
                instantReward += r.instantReward;
                ongoingReward += r.ongoingRewardPerSecond;
                maxReward += r.maxDailyReward;
                exists = true;
            }
        }
        EconomicPolicy.Reward reward = exists ? new EconomicPolicy.Reward(rewardId, instantReward, ongoingReward, maxReward) : null;
        this.mRewards.put(rewardId, reward);
        return reward;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.tare.EconomicPolicy
    public void dump(IndentingPrintWriter pw) {
        dumpActiveModifiers(pw);
        pw.println();
        pw.println(getClass().getSimpleName() + ":");
        pw.increaseIndent();
        pw.println("Cached actions:");
        pw.increaseIndent();
        for (int i = 0; i < this.mActions.size(); i++) {
            dumpAction(pw, this.mActions.valueAt(i));
        }
        pw.decreaseIndent();
        pw.println();
        pw.println("Cached rewards:");
        pw.increaseIndent();
        for (int i2 = 0; i2 < this.mRewards.size(); i2++) {
            dumpReward(pw, this.mRewards.valueAt(i2));
        }
        pw.decreaseIndent();
        for (int i3 = 0; i3 < this.mEnabledEconomicPolicies.size(); i3++) {
            EconomicPolicy economicPolicy = this.mEnabledEconomicPolicies.valueAt(i3);
            pw.println();
            pw.print("(Includes) ");
            pw.println(economicPolicy.getClass().getSimpleName() + ":");
            pw.increaseIndent();
            economicPolicy.dump(pw);
            pw.decreaseIndent();
        }
        pw.decreaseIndent();
    }
}
