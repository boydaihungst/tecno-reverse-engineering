package com.android.server.integrity.serializer;

import android.content.integrity.AtomicFormula;
import android.content.integrity.CompoundFormula;
import android.content.integrity.IntegrityFormula;
import android.content.integrity.Rule;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
/* loaded from: classes.dex */
class RuleIndexingDetailsIdentifier {
    RuleIndexingDetailsIdentifier() {
    }

    public static Map<Integer, Map<String, List<Rule>>> splitRulesIntoIndexBuckets(List<Rule> rules) {
        if (rules == null) {
            throw new IllegalArgumentException("Index buckets cannot be created for null rule list.");
        }
        Map<Integer, Map<String, List<Rule>>> typeOrganizedRuleMap = new HashMap<>();
        typeOrganizedRuleMap.put(0, new HashMap<>());
        typeOrganizedRuleMap.put(1, new HashMap<>());
        typeOrganizedRuleMap.put(2, new HashMap<>());
        for (Rule rule : rules) {
            try {
                RuleIndexingDetails indexingDetails = getIndexingDetails(rule.getFormula());
                int ruleIndexType = indexingDetails.getIndexType();
                String ruleKey = indexingDetails.getRuleKey();
                if (!typeOrganizedRuleMap.get(Integer.valueOf(ruleIndexType)).containsKey(ruleKey)) {
                    typeOrganizedRuleMap.get(Integer.valueOf(ruleIndexType)).put(ruleKey, new ArrayList<>());
                }
                typeOrganizedRuleMap.get(Integer.valueOf(ruleIndexType)).get(ruleKey).add(rule);
            } catch (Exception e) {
                throw new IllegalArgumentException(String.format("Malformed rule identified. [%s]", rule.toString()));
            }
        }
        return typeOrganizedRuleMap;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static RuleIndexingDetails getIndexingDetails(IntegrityFormula formula) {
        switch (formula.getTag()) {
            case 0:
                return getIndexingDetailsForCompoundFormula((CompoundFormula) formula);
            case 1:
                return getIndexingDetailsForStringAtomicFormula((AtomicFormula.StringAtomicFormula) formula);
            case 2:
            case 3:
            case 4:
                return new RuleIndexingDetails(0);
            default:
                throw new IllegalArgumentException(String.format("Invalid formula tag type: %s", Integer.valueOf(formula.getTag())));
        }
    }

    private static RuleIndexingDetails getIndexingDetailsForCompoundFormula(CompoundFormula compoundFormula) {
        int connector = compoundFormula.getConnector();
        List<IntegrityFormula> formulas = compoundFormula.getFormulas();
        switch (connector) {
            case 0:
            case 1:
                Optional<RuleIndexingDetails> packageNameRule = formulas.stream().map(new Function() { // from class: com.android.server.integrity.serializer.RuleIndexingDetailsIdentifier$$ExternalSyntheticLambda0
                    @Override // java.util.function.Function
                    public final Object apply(Object obj) {
                        RuleIndexingDetails indexingDetails;
                        indexingDetails = RuleIndexingDetailsIdentifier.getIndexingDetails((IntegrityFormula) obj);
                        return indexingDetails;
                    }
                }).filter(new Predicate() { // from class: com.android.server.integrity.serializer.RuleIndexingDetailsIdentifier$$ExternalSyntheticLambda1
                    @Override // java.util.function.Predicate
                    public final boolean test(Object obj) {
                        return RuleIndexingDetailsIdentifier.lambda$getIndexingDetailsForCompoundFormula$1((RuleIndexingDetails) obj);
                    }
                }).findAny();
                if (packageNameRule.isPresent()) {
                    return packageNameRule.get();
                }
                Optional<RuleIndexingDetails> appCertificateRule = formulas.stream().map(new Function() { // from class: com.android.server.integrity.serializer.RuleIndexingDetailsIdentifier$$ExternalSyntheticLambda2
                    @Override // java.util.function.Function
                    public final Object apply(Object obj) {
                        RuleIndexingDetails indexingDetails;
                        indexingDetails = RuleIndexingDetailsIdentifier.getIndexingDetails((IntegrityFormula) obj);
                        return indexingDetails;
                    }
                }).filter(new Predicate() { // from class: com.android.server.integrity.serializer.RuleIndexingDetailsIdentifier$$ExternalSyntheticLambda3
                    @Override // java.util.function.Predicate
                    public final boolean test(Object obj) {
                        return RuleIndexingDetailsIdentifier.lambda$getIndexingDetailsForCompoundFormula$3((RuleIndexingDetails) obj);
                    }
                }).findAny();
                if (appCertificateRule.isPresent()) {
                    return appCertificateRule.get();
                }
                return new RuleIndexingDetails(0);
            default:
                return new RuleIndexingDetails(0);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$getIndexingDetailsForCompoundFormula$1(RuleIndexingDetails ruleIndexingDetails) {
        return ruleIndexingDetails.getIndexType() == 1;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$getIndexingDetailsForCompoundFormula$3(RuleIndexingDetails ruleIndexingDetails) {
        return ruleIndexingDetails.getIndexType() == 2;
    }

    private static RuleIndexingDetails getIndexingDetailsForStringAtomicFormula(AtomicFormula.StringAtomicFormula atomicFormula) {
        switch (atomicFormula.getKey()) {
            case 0:
                return new RuleIndexingDetails(1, atomicFormula.getValue());
            case 1:
                return new RuleIndexingDetails(2, atomicFormula.getValue());
            default:
                return new RuleIndexingDetails(0);
        }
    }
}
