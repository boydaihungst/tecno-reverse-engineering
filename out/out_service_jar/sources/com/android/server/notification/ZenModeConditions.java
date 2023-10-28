package com.android.server.notification;

import android.content.ComponentName;
import android.net.Uri;
import android.service.notification.Condition;
import android.service.notification.IConditionProvider;
import android.service.notification.ZenModeConfig;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Log;
import com.android.server.notification.ConditionProviders;
import java.io.PrintWriter;
/* loaded from: classes2.dex */
public class ZenModeConditions implements ConditionProviders.Callback {
    private static final boolean DEBUG = ZenModeHelper.DEBUG;
    private static final String TAG = "ZenModeHelper";
    private final ConditionProviders mConditionProviders;
    private final ZenModeHelper mHelper;
    protected final ArrayMap<Uri, ComponentName> mSubscriptions = new ArrayMap<>();

    public ZenModeConditions(ZenModeHelper helper, ConditionProviders conditionProviders) {
        this.mHelper = helper;
        this.mConditionProviders = conditionProviders;
        if (conditionProviders.isSystemProviderEnabled("countdown")) {
            conditionProviders.addSystemProvider(new CountdownConditionProvider());
        }
        if (conditionProviders.isSystemProviderEnabled("schedule")) {
            conditionProviders.addSystemProvider(new ScheduleConditionProvider());
        }
        if (conditionProviders.isSystemProviderEnabled("event")) {
            conditionProviders.addSystemProvider(new EventConditionProvider());
        }
        conditionProviders.setCallback(this);
    }

    public void dump(PrintWriter pw, String prefix) {
        pw.print(prefix);
        pw.print("mSubscriptions=");
        pw.println(this.mSubscriptions);
    }

    public void evaluateConfig(ZenModeConfig config, ComponentName trigger, boolean processSubscriptions) {
        if (config == null) {
            return;
        }
        if (config.manualRule != null && config.manualRule.condition != null && !config.manualRule.isTrueOrUnknown()) {
            if (DEBUG) {
                Log.d(TAG, "evaluateConfig: clearing manual rule");
            }
            config.manualRule = null;
        }
        ArraySet<Uri> current = new ArraySet<>();
        evaluateRule(config.manualRule, current, null, processSubscriptions);
        for (ZenModeConfig.ZenRule automaticRule : config.automaticRules.values()) {
            if (automaticRule.component != null) {
                evaluateRule(automaticRule, current, trigger, processSubscriptions);
                updateSnoozing(automaticRule);
            }
        }
        synchronized (this.mSubscriptions) {
            int N = this.mSubscriptions.size();
            for (int i = N - 1; i >= 0; i--) {
                Uri id = this.mSubscriptions.keyAt(i);
                ComponentName component = this.mSubscriptions.valueAt(i);
                if (processSubscriptions && !current.contains(id)) {
                    this.mConditionProviders.unsubscribeIfNecessary(component, id);
                    this.mSubscriptions.removeAt(i);
                }
            }
        }
    }

    @Override // com.android.server.notification.ConditionProviders.Callback
    public void onBootComplete() {
    }

    @Override // com.android.server.notification.ConditionProviders.Callback
    public void onUserSwitched() {
    }

    @Override // com.android.server.notification.ConditionProviders.Callback
    public void onServiceAdded(ComponentName component) {
        if (DEBUG) {
            Log.d(TAG, "onServiceAdded " + component);
        }
        ZenModeHelper zenModeHelper = this.mHelper;
        zenModeHelper.setConfig(zenModeHelper.getConfig(), component, "zmc.onServiceAdded:" + component);
    }

    @Override // com.android.server.notification.ConditionProviders.Callback
    public void onConditionChanged(Uri id, Condition condition) {
        if (DEBUG) {
            Log.d(TAG, "onConditionChanged " + id + " " + condition);
        }
        ZenModeConfig config = this.mHelper.getConfig();
        if (config == null) {
            return;
        }
        this.mHelper.setAutomaticZenRuleState(id, condition);
    }

    private void evaluateRule(ZenModeConfig.ZenRule rule, ArraySet<Uri> current, ComponentName trigger, boolean processSubscriptions) {
        if (rule == null || rule.conditionId == null || rule.configurationActivity != null) {
            return;
        }
        Uri id = rule.conditionId;
        boolean isSystemCondition = false;
        for (SystemConditionProviderService sp : this.mConditionProviders.getSystemProviders()) {
            if (sp.isValidConditionId(id)) {
                this.mConditionProviders.ensureRecordExists(sp.getComponent(), id, sp.asInterface());
                rule.component = sp.getComponent();
                isSystemCondition = true;
            }
        }
        if (!isSystemCondition) {
            IConditionProvider cp = this.mConditionProviders.findConditionProvider(rule.component);
            if (DEBUG) {
                Log.d(TAG, "Ensure external rule exists: " + (cp != null) + " for " + id);
            }
            if (cp != null) {
                this.mConditionProviders.ensureRecordExists(rule.component, id, cp);
            }
        }
        if (rule.component == null && rule.enabler == null) {
            Log.w(TAG, "No component found for automatic rule: " + rule.conditionId);
            rule.enabled = false;
            return;
        }
        if (current != null) {
            current.add(id);
        }
        if (processSubscriptions && ((trigger != null && trigger.equals(rule.component)) || isSystemCondition)) {
            boolean z = DEBUG;
            if (z) {
                Log.d(TAG, "Subscribing to " + rule.component);
            }
            if (this.mConditionProviders.subscribeIfNecessary(rule.component, rule.conditionId)) {
                synchronized (this.mSubscriptions) {
                    this.mSubscriptions.put(rule.conditionId, rule.component);
                }
            } else {
                rule.condition = null;
                if (z) {
                    Log.d(TAG, "zmc failed to subscribe");
                }
            }
        }
        if (rule.component != null && rule.condition == null) {
            rule.condition = this.mConditionProviders.findCondition(rule.component, rule.conditionId);
            if (rule.condition == null || !DEBUG) {
                return;
            }
            Log.d(TAG, "Found existing condition for: " + rule.conditionId);
        }
    }

    private boolean updateSnoozing(ZenModeConfig.ZenRule rule) {
        if (rule == null || !rule.snoozing || rule.isTrueOrUnknown()) {
            return false;
        }
        rule.snoozing = false;
        if (DEBUG) {
            Log.d(TAG, "Snoozing reset for " + rule.conditionId);
            return true;
        }
        return true;
    }
}
