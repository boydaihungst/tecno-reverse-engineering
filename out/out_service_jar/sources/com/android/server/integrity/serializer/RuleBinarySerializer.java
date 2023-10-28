package com.android.server.integrity.serializer;

import android.content.integrity.AtomicFormula;
import android.content.integrity.CompoundFormula;
import android.content.integrity.InstallerAllowedByManifestFormula;
import android.content.integrity.IntegrityFormula;
import android.content.integrity.IntegrityUtils;
import android.content.integrity.Rule;
import com.android.internal.util.Preconditions;
import com.android.server.audio.AudioService$$ExternalSyntheticLambda1;
import com.android.server.integrity.model.BitOutputStream;
import com.android.server.integrity.model.ByteTrackedOutputStream;
import com.android.server.integrity.model.IndexingFileConstants;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
/* loaded from: classes.dex */
public class RuleBinarySerializer implements RuleSerializer {
    static final int INDEXED_RULE_SIZE_LIMIT = 100000;
    static final int NONINDEXED_RULE_SIZE_LIMIT = 1000;
    static final int TOTAL_RULE_SIZE_LIMIT = 200000;

    @Override // com.android.server.integrity.serializer.RuleSerializer
    public byte[] serialize(List<Rule> rules, Optional<Integer> formatVersion) throws RuleSerializeException {
        try {
            ByteArrayOutputStream rulesOutputStream = new ByteArrayOutputStream();
            serialize(rules, formatVersion, rulesOutputStream, new ByteArrayOutputStream());
            return rulesOutputStream.toByteArray();
        } catch (Exception e) {
            throw new RuleSerializeException(e.getMessage(), e);
        }
    }

    @Override // com.android.server.integrity.serializer.RuleSerializer
    public void serialize(List<Rule> rules, Optional<Integer> formatVersion, OutputStream rulesFileOutputStream, OutputStream indexingFileOutputStream) throws RuleSerializeException {
        try {
            if (rules == null) {
                throw new IllegalArgumentException("Null rules cannot be serialized.");
            }
            if (rules.size() > TOTAL_RULE_SIZE_LIMIT) {
                throw new IllegalArgumentException("Too many rules provided: " + rules.size());
            }
            Map<Integer, Map<String, List<Rule>>> indexedRules = RuleIndexingDetailsIdentifier.splitRulesIntoIndexBuckets(rules);
            verifySize(indexedRules.get(1), INDEXED_RULE_SIZE_LIMIT);
            verifySize(indexedRules.get(2), INDEXED_RULE_SIZE_LIMIT);
            verifySize(indexedRules.get(0), 1000);
            ByteTrackedOutputStream ruleFileByteTrackedOutputStream = new ByteTrackedOutputStream(rulesFileOutputStream);
            serializeRuleFileMetadata(formatVersion, ruleFileByteTrackedOutputStream);
            LinkedHashMap<String, Integer> packageNameIndexes = serializeRuleList(indexedRules.get(1), ruleFileByteTrackedOutputStream);
            LinkedHashMap<String, Integer> appCertificateIndexes = serializeRuleList(indexedRules.get(2), ruleFileByteTrackedOutputStream);
            LinkedHashMap<String, Integer> unindexedRulesIndexes = serializeRuleList(indexedRules.get(0), ruleFileByteTrackedOutputStream);
            BitOutputStream indexingBitOutputStream = new BitOutputStream(indexingFileOutputStream);
            serializeIndexGroup(packageNameIndexes, indexingBitOutputStream, true);
            serializeIndexGroup(appCertificateIndexes, indexingBitOutputStream, true);
            serializeIndexGroup(unindexedRulesIndexes, indexingBitOutputStream, false);
            indexingBitOutputStream.flush();
        } catch (Exception e) {
            throw new RuleSerializeException(e.getMessage(), e);
        }
    }

    private void verifySize(Map<String, List<Rule>> ruleListMap, int ruleSizeLimit) {
        int totalRuleCount = ((Integer) ruleListMap.values().stream().map(new Function() { // from class: com.android.server.integrity.serializer.RuleBinarySerializer$$ExternalSyntheticLambda0
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                Integer valueOf;
                valueOf = Integer.valueOf(((List) obj).size());
                return valueOf;
            }
        }).collect(Collectors.summingInt(new AudioService$$ExternalSyntheticLambda1()))).intValue();
        if (totalRuleCount > ruleSizeLimit) {
            throw new IllegalArgumentException("Too many rules provided in the indexing group. Provided " + totalRuleCount + " limit " + ruleSizeLimit);
        }
    }

    private void serializeRuleFileMetadata(Optional<Integer> formatVersion, ByteTrackedOutputStream outputStream) throws IOException {
        int formatVersionValue = formatVersion.orElse(1).intValue();
        BitOutputStream bitOutputStream = new BitOutputStream(outputStream);
        bitOutputStream.setNext(8, formatVersionValue);
        bitOutputStream.flush();
    }

    private LinkedHashMap<String, Integer> serializeRuleList(Map<String, List<Rule>> rulesMap, ByteTrackedOutputStream outputStream) throws IOException {
        Preconditions.checkArgument(rulesMap != null, "serializeRuleList should never be called with null rule list.");
        BitOutputStream bitOutputStream = new BitOutputStream(outputStream);
        LinkedHashMap<String, Integer> indexMapping = new LinkedHashMap<>();
        indexMapping.put(IndexingFileConstants.START_INDEXING_KEY, Integer.valueOf(outputStream.getWrittenBytesCount()));
        List<String> sortedKeys = (List) rulesMap.keySet().stream().sorted().collect(Collectors.toList());
        int indexTracker = 0;
        for (String key : sortedKeys) {
            if (indexTracker >= 50) {
                indexMapping.put(key, Integer.valueOf(outputStream.getWrittenBytesCount()));
                indexTracker = 0;
            }
            for (Rule rule : rulesMap.get(key)) {
                serializeRule(rule, bitOutputStream);
                bitOutputStream.flush();
                indexTracker++;
            }
        }
        indexMapping.put(IndexingFileConstants.END_INDEXING_KEY, Integer.valueOf(outputStream.getWrittenBytesCount()));
        return indexMapping;
    }

    private void serializeRule(Rule rule, BitOutputStream bitOutputStream) throws IOException {
        if (rule == null) {
            throw new IllegalArgumentException("Null rule can not be serialized");
        }
        bitOutputStream.setNext();
        serializeFormula(rule.getFormula(), bitOutputStream);
        bitOutputStream.setNext(3, rule.getEffect());
        bitOutputStream.setNext();
    }

    private void serializeFormula(IntegrityFormula formula, BitOutputStream bitOutputStream) throws IOException {
        if (formula instanceof AtomicFormula) {
            serializeAtomicFormula((AtomicFormula) formula, bitOutputStream);
        } else if (formula instanceof CompoundFormula) {
            serializeCompoundFormula((CompoundFormula) formula, bitOutputStream);
        } else if (formula instanceof InstallerAllowedByManifestFormula) {
            bitOutputStream.setNext(3, 3);
        } else {
            throw new IllegalArgumentException(String.format("Invalid formula type: %s", formula.getClass()));
        }
    }

    private void serializeCompoundFormula(CompoundFormula compoundFormula, BitOutputStream bitOutputStream) throws IOException {
        if (compoundFormula == null) {
            throw new IllegalArgumentException("Null compound formula can not be serialized");
        }
        bitOutputStream.setNext(3, 1);
        bitOutputStream.setNext(2, compoundFormula.getConnector());
        for (IntegrityFormula formula : compoundFormula.getFormulas()) {
            serializeFormula(formula, bitOutputStream);
        }
        bitOutputStream.setNext(3, 2);
    }

    private void serializeAtomicFormula(AtomicFormula atomicFormula, BitOutputStream bitOutputStream) throws IOException {
        if (atomicFormula == null) {
            throw new IllegalArgumentException("Null atomic formula can not be serialized");
        }
        bitOutputStream.setNext(3, 0);
        bitOutputStream.setNext(4, atomicFormula.getKey());
        if (atomicFormula.getTag() == 1) {
            AtomicFormula.StringAtomicFormula stringAtomicFormula = (AtomicFormula.StringAtomicFormula) atomicFormula;
            bitOutputStream.setNext(3, 0);
            serializeStringValue(stringAtomicFormula.getValue(), stringAtomicFormula.getIsHashedValue().booleanValue(), bitOutputStream);
        } else if (atomicFormula.getTag() != 2) {
            if (atomicFormula.getTag() == 3) {
                AtomicFormula.BooleanAtomicFormula booleanAtomicFormula = (AtomicFormula.BooleanAtomicFormula) atomicFormula;
                bitOutputStream.setNext(3, 0);
                serializeBooleanValue(booleanAtomicFormula.getValue().booleanValue(), bitOutputStream);
                return;
            }
            throw new IllegalArgumentException(String.format("Invalid atomic formula type: %s", atomicFormula.getClass()));
        } else {
            AtomicFormula.LongAtomicFormula longAtomicFormula = (AtomicFormula.LongAtomicFormula) atomicFormula;
            bitOutputStream.setNext(3, longAtomicFormula.getOperator().intValue());
            long value = longAtomicFormula.getValue().longValue();
            serializeIntValue((int) (value >>> 32), bitOutputStream);
            serializeIntValue((int) value, bitOutputStream);
        }
    }

    private void serializeIndexGroup(LinkedHashMap<String, Integer> indexes, BitOutputStream bitOutputStream, boolean isIndexed) throws IOException {
        serializeStringValue(IndexingFileConstants.START_INDEXING_KEY, false, bitOutputStream);
        serializeIntValue(indexes.get(IndexingFileConstants.START_INDEXING_KEY).intValue(), bitOutputStream);
        if (isIndexed) {
            for (Map.Entry<String, Integer> entry : indexes.entrySet()) {
                if (!entry.getKey().equals(IndexingFileConstants.START_INDEXING_KEY) && !entry.getKey().equals(IndexingFileConstants.END_INDEXING_KEY)) {
                    serializeStringValue(entry.getKey(), false, bitOutputStream);
                    serializeIntValue(entry.getValue().intValue(), bitOutputStream);
                }
            }
        }
        serializeStringValue(IndexingFileConstants.END_INDEXING_KEY, false, bitOutputStream);
        serializeIntValue(indexes.get(IndexingFileConstants.END_INDEXING_KEY).intValue(), bitOutputStream);
    }

    private void serializeStringValue(String value, boolean isHashedValue, BitOutputStream bitOutputStream) throws IOException {
        if (value == null) {
            throw new IllegalArgumentException("String value can not be null.");
        }
        byte[] valueBytes = getBytesForString(value, isHashedValue);
        bitOutputStream.setNext(isHashedValue);
        bitOutputStream.setNext(8, valueBytes.length);
        for (byte valueByte : valueBytes) {
            bitOutputStream.setNext(8, valueByte);
        }
    }

    private void serializeIntValue(int value, BitOutputStream bitOutputStream) throws IOException {
        bitOutputStream.setNext(32, value);
    }

    private void serializeBooleanValue(boolean value, BitOutputStream bitOutputStream) throws IOException {
        bitOutputStream.setNext(value);
    }

    private static byte[] getBytesForString(String value, boolean isHashedValue) {
        if (!isHashedValue) {
            return value.getBytes(StandardCharsets.UTF_8);
        }
        return IntegrityUtils.getBytesFromHexDigest(value);
    }
}
