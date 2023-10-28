package com.android.server.integrity.parser;

import android.content.integrity.AppInstallMetadata;
import com.android.server.integrity.model.BitInputStream;
import com.android.server.integrity.model.IndexingFileConstants;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;
/* loaded from: classes.dex */
public class RuleIndexingController {
    private static LinkedHashMap<String, Integer> sAppCertificateBasedIndexes;
    private static LinkedHashMap<String, Integer> sPackageNameBasedIndexes;
    private static LinkedHashMap<String, Integer> sUnindexedRuleIndexes;

    public RuleIndexingController(InputStream inputStream) throws IOException {
        BitInputStream bitInputStream = new BitInputStream(inputStream);
        sPackageNameBasedIndexes = getNextIndexGroup(bitInputStream);
        sAppCertificateBasedIndexes = getNextIndexGroup(bitInputStream);
        sUnindexedRuleIndexes = getNextIndexGroup(bitInputStream);
    }

    public List<RuleIndexRange> identifyRulesToEvaluate(AppInstallMetadata appInstallMetadata) {
        List<RuleIndexRange> indexRanges = new ArrayList<>();
        indexRanges.add(searchIndexingKeysRangeContainingKey(sPackageNameBasedIndexes, appInstallMetadata.getPackageName()));
        for (String appCertificate : appInstallMetadata.getAppCertificates()) {
            indexRanges.add(searchIndexingKeysRangeContainingKey(sAppCertificateBasedIndexes, appCertificate));
        }
        indexRanges.add(new RuleIndexRange(sUnindexedRuleIndexes.get(IndexingFileConstants.START_INDEXING_KEY).intValue(), sUnindexedRuleIndexes.get(IndexingFileConstants.END_INDEXING_KEY).intValue()));
        return indexRanges;
    }

    private LinkedHashMap<String, Integer> getNextIndexGroup(BitInputStream bitInputStream) throws IOException {
        LinkedHashMap<String, Integer> keyToIndexMap = new LinkedHashMap<>();
        while (bitInputStream.hasNext()) {
            String key = BinaryFileOperations.getStringValue(bitInputStream);
            int value = BinaryFileOperations.getIntValue(bitInputStream);
            keyToIndexMap.put(key, Integer.valueOf(value));
            if (key.matches(IndexingFileConstants.END_INDEXING_KEY)) {
                break;
            }
        }
        if (keyToIndexMap.size() < 2) {
            throw new IllegalStateException("Indexing file is corrupt.");
        }
        return keyToIndexMap;
    }

    private static RuleIndexRange searchIndexingKeysRangeContainingKey(LinkedHashMap<String, Integer> indexMap, String searchedKey) {
        List<String> keys = (List) indexMap.keySet().stream().collect(Collectors.toList());
        List<String> identifiedKeyRange = searchKeysRangeContainingKey(keys, searchedKey, 0, keys.size() - 1);
        return new RuleIndexRange(indexMap.get(identifiedKeyRange.get(0)).intValue(), indexMap.get(identifiedKeyRange.get(1)).intValue());
    }

    private static List<String> searchKeysRangeContainingKey(List<String> sortedKeyList, String key, int startIndex, int endIndex) {
        if (endIndex <= startIndex) {
            throw new IllegalStateException("Indexing file is corrupt.");
        }
        if (endIndex - startIndex == 1) {
            return Arrays.asList(sortedKeyList.get(startIndex), sortedKeyList.get(endIndex));
        }
        int midKeyIndex = ((endIndex - startIndex) / 2) + startIndex;
        String midKey = sortedKeyList.get(midKeyIndex);
        if (key.compareTo(midKey) >= 0) {
            return searchKeysRangeContainingKey(sortedKeyList, key, midKeyIndex, endIndex);
        }
        return searchKeysRangeContainingKey(sortedKeyList, key, startIndex, midKeyIndex);
    }
}
