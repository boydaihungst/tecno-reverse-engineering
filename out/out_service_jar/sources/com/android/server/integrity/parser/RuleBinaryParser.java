package com.android.server.integrity.parser;

import android.content.integrity.AtomicFormula;
import android.content.integrity.CompoundFormula;
import android.content.integrity.InstallerAllowedByManifestFormula;
import android.content.integrity.IntegrityFormula;
import android.content.integrity.Rule;
import com.android.server.integrity.model.BitInputStream;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
/* loaded from: classes.dex */
public class RuleBinaryParser implements RuleParser {
    @Override // com.android.server.integrity.parser.RuleParser
    public List<Rule> parse(byte[] ruleBytes) throws RuleParseException {
        return parse(RandomAccessObject.ofBytes(ruleBytes), Collections.emptyList());
    }

    @Override // com.android.server.integrity.parser.RuleParser
    public List<Rule> parse(RandomAccessObject randomAccessObject, List<RuleIndexRange> indexRanges) throws RuleParseException {
        try {
            RandomAccessInputStream randomAccessInputStream = new RandomAccessInputStream(randomAccessObject);
            List<Rule> parseRules = parseRules(randomAccessInputStream, indexRanges);
            randomAccessInputStream.close();
            return parseRules;
        } catch (Exception e) {
            throw new RuleParseException(e.getMessage(), e);
        }
    }

    private List<Rule> parseRules(RandomAccessInputStream randomAccessInputStream, List<RuleIndexRange> indexRanges) throws IOException {
        randomAccessInputStream.skip(1L);
        if (indexRanges.isEmpty()) {
            return parseAllRules(randomAccessInputStream);
        }
        return parseIndexedRules(randomAccessInputStream, indexRanges);
    }

    private List<Rule> parseAllRules(RandomAccessInputStream randomAccessInputStream) throws IOException {
        List<Rule> parsedRules = new ArrayList<>();
        BitInputStream inputStream = new BitInputStream(new BufferedInputStream(randomAccessInputStream));
        while (inputStream.hasNext()) {
            if (inputStream.getNext(1) == 1) {
                parsedRules.add(parseRule(inputStream));
            }
        }
        return parsedRules;
    }

    private List<Rule> parseIndexedRules(RandomAccessInputStream randomAccessInputStream, List<RuleIndexRange> indexRanges) throws IOException {
        List<Rule> parsedRules = new ArrayList<>();
        for (RuleIndexRange range : indexRanges) {
            randomAccessInputStream.seek(range.getStartIndex());
            BitInputStream inputStream = new BitInputStream(new BufferedInputStream(new LimitInputStream(randomAccessInputStream, range.getEndIndex() - range.getStartIndex())));
            while (inputStream.hasNext()) {
                if (inputStream.getNext(1) == 1) {
                    parsedRules.add(parseRule(inputStream));
                }
            }
        }
        return parsedRules;
    }

    private Rule parseRule(BitInputStream bitInputStream) throws IOException {
        IntegrityFormula formula = parseFormula(bitInputStream);
        int effect = bitInputStream.getNext(3);
        if (bitInputStream.getNext(1) != 1) {
            throw new IllegalArgumentException("A rule must end with a '1' bit.");
        }
        return new Rule(formula, effect);
    }

    private IntegrityFormula parseFormula(BitInputStream bitInputStream) throws IOException {
        int separator = bitInputStream.getNext(3);
        switch (separator) {
            case 0:
                return parseAtomicFormula(bitInputStream);
            case 1:
                return parseCompoundFormula(bitInputStream);
            case 2:
                return null;
            case 3:
                return new InstallerAllowedByManifestFormula();
            default:
                throw new IllegalArgumentException(String.format("Unknown formula separator: %s", Integer.valueOf(separator)));
        }
    }

    private CompoundFormula parseCompoundFormula(BitInputStream bitInputStream) throws IOException {
        int connector = bitInputStream.getNext(2);
        List<IntegrityFormula> formulas = new ArrayList<>();
        IntegrityFormula parsedFormula = parseFormula(bitInputStream);
        while (parsedFormula != null) {
            formulas.add(parsedFormula);
            parsedFormula = parseFormula(bitInputStream);
        }
        return new CompoundFormula(connector, formulas);
    }

    private AtomicFormula parseAtomicFormula(BitInputStream bitInputStream) throws IOException {
        int key = bitInputStream.getNext(4);
        int operator = bitInputStream.getNext(3);
        switch (key) {
            case 0:
            case 1:
            case 2:
            case 3:
            case 7:
            case 8:
                boolean isHashedValue = bitInputStream.getNext(1) == 1;
                int valueSize = bitInputStream.getNext(8);
                String stringValue = BinaryFileOperations.getStringValue(bitInputStream, valueSize, isHashedValue);
                return new AtomicFormula.StringAtomicFormula(key, stringValue, isHashedValue);
            case 4:
                long upper = BinaryFileOperations.getIntValue(bitInputStream);
                long lower = BinaryFileOperations.getIntValue(bitInputStream);
                long longValue = (upper << 32) | lower;
                return new AtomicFormula.LongAtomicFormula(key, operator, longValue);
            case 5:
            case 6:
                boolean booleanValue = BinaryFileOperations.getBooleanValue(bitInputStream);
                return new AtomicFormula.BooleanAtomicFormula(key, booleanValue);
            default:
                throw new IllegalArgumentException(String.format("Unknown key: %d", Integer.valueOf(key)));
        }
    }
}
