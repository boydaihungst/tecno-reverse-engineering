package com.android.server.integrity.model;

import android.content.integrity.Rule;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
/* loaded from: classes.dex */
public final class IntegrityCheckResult {
    private final Effect mEffect;
    private final List<Rule> mRuleList;

    /* loaded from: classes.dex */
    public enum Effect {
        ALLOW,
        DENY
    }

    private IntegrityCheckResult(Effect effect, List<Rule> ruleList) {
        this.mEffect = effect;
        this.mRuleList = ruleList;
    }

    public Effect getEffect() {
        return this.mEffect;
    }

    public List<Rule> getMatchedRules() {
        return this.mRuleList;
    }

    public static IntegrityCheckResult allow() {
        return new IntegrityCheckResult(Effect.ALLOW, Collections.emptyList());
    }

    public static IntegrityCheckResult allow(List<Rule> ruleList) {
        return new IntegrityCheckResult(Effect.ALLOW, ruleList);
    }

    public static IntegrityCheckResult deny(List<Rule> ruleList) {
        return new IntegrityCheckResult(Effect.DENY, ruleList);
    }

    public int getLoggingResponse() {
        if (getEffect() == Effect.DENY) {
            return 2;
        }
        if (getEffect() == Effect.ALLOW && getMatchedRules().isEmpty()) {
            return 1;
        }
        if (getEffect() == Effect.ALLOW && !getMatchedRules().isEmpty()) {
            return 3;
        }
        throw new IllegalStateException("IntegrityCheckResult is not valid.");
    }

    public boolean isCausedByAppCertRule() {
        return this.mRuleList.stream().anyMatch(new Predicate() { // from class: com.android.server.integrity.model.IntegrityCheckResult$$ExternalSyntheticLambda1
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                boolean isAppCertificateFormula;
                isAppCertificateFormula = ((Rule) obj).getFormula().isAppCertificateFormula();
                return isAppCertificateFormula;
            }
        });
    }

    public boolean isCausedByInstallerRule() {
        return this.mRuleList.stream().anyMatch(new Predicate() { // from class: com.android.server.integrity.model.IntegrityCheckResult$$ExternalSyntheticLambda0
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                boolean isInstallerFormula;
                isInstallerFormula = ((Rule) obj).getFormula().isInstallerFormula();
                return isInstallerFormula;
            }
        });
    }
}
