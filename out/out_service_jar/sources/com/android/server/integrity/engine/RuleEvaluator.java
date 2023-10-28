package com.android.server.integrity.engine;

import android.content.integrity.AppInstallMetadata;
import android.content.integrity.Rule;
import com.android.server.integrity.model.IntegrityCheckResult;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
/* loaded from: classes.dex */
final class RuleEvaluator {
    RuleEvaluator() {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static IntegrityCheckResult evaluateRules(List<Rule> rules, final AppInstallMetadata appInstallMetadata) {
        List<Rule> matchedRules = (List) rules.stream().filter(new Predicate() { // from class: com.android.server.integrity.engine.RuleEvaluator$$ExternalSyntheticLambda0
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                boolean matches;
                matches = ((Rule) obj).getFormula().matches(appInstallMetadata);
                return matches;
            }
        }).collect(Collectors.toList());
        List<Rule> matchedPowerAllowRules = (List) matchedRules.stream().filter(new Predicate() { // from class: com.android.server.integrity.engine.RuleEvaluator$$ExternalSyntheticLambda1
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return RuleEvaluator.lambda$evaluateRules$1((Rule) obj);
            }
        }).collect(Collectors.toList());
        if (!matchedPowerAllowRules.isEmpty()) {
            return IntegrityCheckResult.allow(matchedPowerAllowRules);
        }
        List<Rule> matchedDenyRules = (List) matchedRules.stream().filter(new Predicate() { // from class: com.android.server.integrity.engine.RuleEvaluator$$ExternalSyntheticLambda2
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return RuleEvaluator.lambda$evaluateRules$2((Rule) obj);
            }
        }).collect(Collectors.toList());
        if (!matchedDenyRules.isEmpty()) {
            return IntegrityCheckResult.deny(matchedDenyRules);
        }
        return IntegrityCheckResult.allow();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$evaluateRules$1(Rule rule) {
        return rule.getEffect() == 1;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$evaluateRules$2(Rule rule) {
        return rule.getEffect() == 0;
    }
}
