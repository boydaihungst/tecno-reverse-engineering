package com.android.server.broadcastradio.hal2;

import android.hardware.broadcastradio.V2_0.ProgramIdentifier;
import android.hardware.broadcastradio.V2_0.ProgramInfo;
import android.hardware.broadcastradio.V2_0.ProgramListChunk;
import android.hardware.radio.ProgramList;
import android.hardware.radio.ProgramSelector;
import android.hardware.radio.RadioManager;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class ProgramInfoCache {
    private static final int MAX_NUM_MODIFIED_PER_CHUNK = 100;
    private static final int MAX_NUM_REMOVED_PER_CHUNK = 500;
    private boolean mComplete;
    private final ProgramList.Filter mFilter;
    private final Map<ProgramSelector.Identifier, RadioManager.ProgramInfo> mProgramInfoMap;

    /* JADX INFO: Access modifiers changed from: package-private */
    public ProgramInfoCache(ProgramList.Filter filter) {
        this.mProgramInfoMap = new HashMap();
        this.mComplete = true;
        this.mFilter = filter;
    }

    ProgramInfoCache(ProgramList.Filter filter, boolean complete, RadioManager.ProgramInfo... programInfos) {
        this.mProgramInfoMap = new HashMap();
        this.mComplete = true;
        this.mFilter = filter;
        this.mComplete = complete;
        for (RadioManager.ProgramInfo programInfo : programInfos) {
            this.mProgramInfoMap.put(programInfo.getSelector().getPrimaryId(), programInfo);
        }
    }

    boolean programInfosAreExactly(RadioManager.ProgramInfo... programInfos) {
        Map<ProgramSelector.Identifier, RadioManager.ProgramInfo> expectedMap = new HashMap<>();
        for (RadioManager.ProgramInfo programInfo : programInfos) {
            expectedMap.put(programInfo.getSelector().getPrimaryId(), programInfo);
        }
        return expectedMap.equals(this.mProgramInfoMap);
    }

    public String toString() {
        final StringBuilder sb = new StringBuilder("ProgramInfoCache(mComplete = ");
        sb.append(this.mComplete);
        sb.append(", mFilter = ");
        sb.append(this.mFilter);
        sb.append(", mProgramInfoMap = [");
        this.mProgramInfoMap.forEach(new BiConsumer() { // from class: com.android.server.broadcastradio.hal2.ProgramInfoCache$$ExternalSyntheticLambda0
            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                ProgramInfoCache.lambda$toString$0(sb, (ProgramSelector.Identifier) obj, (RadioManager.ProgramInfo) obj2);
            }
        });
        sb.append("]");
        return sb.toString();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$toString$0(StringBuilder sb, ProgramSelector.Identifier id, RadioManager.ProgramInfo programInfo) {
        sb.append("\n");
        sb.append(programInfo.toString());
    }

    public boolean isComplete() {
        return this.mComplete;
    }

    public ProgramList.Filter getFilter() {
        return this.mFilter;
    }

    void updateFromHalProgramListChunk(ProgramListChunk chunk) {
        if (chunk.purge) {
            this.mProgramInfoMap.clear();
        }
        Iterator<ProgramInfo> it = chunk.modified.iterator();
        while (it.hasNext()) {
            ProgramInfo halProgramInfo = it.next();
            RadioManager.ProgramInfo programInfo = Convert.programInfoFromHal(halProgramInfo);
            this.mProgramInfoMap.put(programInfo.getSelector().getPrimaryId(), programInfo);
        }
        Iterator<ProgramIdentifier> it2 = chunk.removed.iterator();
        while (it2.hasNext()) {
            ProgramIdentifier halProgramId = it2.next();
            this.mProgramInfoMap.remove(Convert.programIdentifierFromHal(halProgramId));
        }
        this.mComplete = chunk.complete;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public List<ProgramList.Chunk> filterAndUpdateFrom(ProgramInfoCache other, boolean purge) {
        return filterAndUpdateFromInternal(other, purge, 100, 500);
    }

    List<ProgramList.Chunk> filterAndUpdateFromInternal(ProgramInfoCache other, boolean purge, int maxNumModifiedPerChunk, int maxNumRemovedPerChunk) {
        if (purge) {
            this.mProgramInfoMap.clear();
        }
        if (this.mProgramInfoMap.isEmpty()) {
            purge = true;
        }
        Set<RadioManager.ProgramInfo> modified = new HashSet<>();
        Set<ProgramSelector.Identifier> removed = new HashSet<>(this.mProgramInfoMap.keySet());
        for (Map.Entry<ProgramSelector.Identifier, RadioManager.ProgramInfo> entry : other.mProgramInfoMap.entrySet()) {
            ProgramSelector.Identifier id = entry.getKey();
            if (passesFilter(id)) {
                removed.remove(id);
                RadioManager.ProgramInfo newInfo = entry.getValue();
                if (shouldIncludeInModified(newInfo)) {
                    this.mProgramInfoMap.put(id, newInfo);
                    modified.add(newInfo);
                }
            }
        }
        for (ProgramSelector.Identifier rem : removed) {
            this.mProgramInfoMap.remove(rem);
        }
        boolean z = other.mComplete;
        this.mComplete = z;
        return buildChunks(purge, z, modified, maxNumModifiedPerChunk, removed, maxNumRemovedPerChunk);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public List<ProgramList.Chunk> filterAndApplyChunk(ProgramList.Chunk chunk) {
        return filterAndApplyChunkInternal(chunk, 100, 500);
    }

    List<ProgramList.Chunk> filterAndApplyChunkInternal(ProgramList.Chunk chunk, int maxNumModifiedPerChunk, int maxNumRemovedPerChunk) {
        if (chunk.isPurge()) {
            this.mProgramInfoMap.clear();
        }
        Set<RadioManager.ProgramInfo> modified = new HashSet<>();
        Set<ProgramSelector.Identifier> removed = new HashSet<>();
        for (RadioManager.ProgramInfo info : chunk.getModified()) {
            ProgramSelector.Identifier id = info.getSelector().getPrimaryId();
            if (passesFilter(id) && shouldIncludeInModified(info)) {
                this.mProgramInfoMap.put(id, info);
                modified.add(info);
            }
        }
        for (ProgramSelector.Identifier id2 : chunk.getRemoved()) {
            if (this.mProgramInfoMap.containsKey(id2)) {
                this.mProgramInfoMap.remove(id2);
                removed.add(id2);
            }
        }
        if (modified.isEmpty() && removed.isEmpty() && this.mComplete == chunk.isComplete() && !chunk.isPurge()) {
            return null;
        }
        this.mComplete = chunk.isComplete();
        return buildChunks(chunk.isPurge(), this.mComplete, modified, maxNumModifiedPerChunk, removed, maxNumRemovedPerChunk);
    }

    private boolean passesFilter(ProgramSelector.Identifier id) {
        ProgramList.Filter filter = this.mFilter;
        if (filter == null) {
            return true;
        }
        if (filter.getIdentifierTypes().isEmpty() || this.mFilter.getIdentifierTypes().contains(Integer.valueOf(id.getType()))) {
            if (this.mFilter.getIdentifiers().isEmpty() || this.mFilter.getIdentifiers().contains(id)) {
                return this.mFilter.areCategoriesIncluded() || !id.isCategoryType();
            }
            return false;
        }
        return false;
    }

    private boolean shouldIncludeInModified(RadioManager.ProgramInfo newInfo) {
        RadioManager.ProgramInfo oldInfo = this.mProgramInfoMap.get(newInfo.getSelector().getPrimaryId());
        if (oldInfo == null) {
            return true;
        }
        ProgramList.Filter filter = this.mFilter;
        if (filter == null || !filter.areModificationsExcluded()) {
            return true ^ oldInfo.equals(newInfo);
        }
        return false;
    }

    private static int roundUpFraction(int numerator, int denominator) {
        return (numerator / denominator) + (numerator % denominator > 0 ? 1 : 0);
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r1v0, types: [int] */
    private static List<ProgramList.Chunk> buildChunks(boolean purge, boolean complete, Collection<RadioManager.ProgramInfo> modified, int maxNumModifiedPerChunk, Collection<ProgramSelector.Identifier> removed, int maxNumRemovedPerChunk) {
        Collection<ProgramSelector.Identifier> removed2;
        if (!purge) {
            removed2 = removed;
        } else {
            removed2 = null;
        }
        ?? r1 = purge;
        int numChunks = modified != null ? Math.max((int) r1, roundUpFraction(modified.size(), maxNumModifiedPerChunk)) : r1;
        int numChunks2 = removed2 != null ? Math.max(numChunks, roundUpFraction(removed2.size(), maxNumRemovedPerChunk)) : numChunks;
        if (numChunks2 == 0) {
            return new ArrayList();
        }
        int modifiedPerChunk = 0;
        int removedPerChunk = 0;
        Iterator<RadioManager.ProgramInfo> modifiedIter = null;
        Iterator<ProgramSelector.Identifier> removedIter = null;
        if (modified != null) {
            modifiedPerChunk = roundUpFraction(modified.size(), numChunks2);
            modifiedIter = modified.iterator();
        }
        if (removed2 != null) {
            removedPerChunk = roundUpFraction(removed2.size(), numChunks2);
            removedIter = removed2.iterator();
        }
        List<ProgramList.Chunk> chunks = new ArrayList<>(numChunks2);
        int i = 0;
        while (i < numChunks2) {
            HashSet<RadioManager.ProgramInfo> modifiedChunk = new HashSet<>();
            HashSet<ProgramSelector.Identifier> removedChunk = new HashSet<>();
            if (modifiedIter != null) {
                for (int j = 0; j < modifiedPerChunk && modifiedIter.hasNext(); j++) {
                    modifiedChunk.add(modifiedIter.next());
                }
            }
            if (removedIter != null) {
                for (int j2 = 0; j2 < removedPerChunk && removedIter.hasNext(); j2++) {
                    removedChunk.add(removedIter.next());
                }
            }
            chunks.add(new ProgramList.Chunk(purge && i == 0, complete && i == numChunks2 + (-1), modifiedChunk, removedChunk));
            i++;
        }
        return chunks;
    }
}
