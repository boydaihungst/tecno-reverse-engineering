package com.android.server.display;

import com.android.server.display.DensityMapping;
import java.util.Arrays;
import java.util.Comparator;
import java.util.function.ToIntFunction;
/* loaded from: classes.dex */
public class DensityMapping {
    private final Entry[] mSortedDensityMappingEntries;

    /* JADX INFO: Access modifiers changed from: package-private */
    public static DensityMapping createByOwning(Entry[] densityMappingEntries) {
        return new DensityMapping(densityMappingEntries);
    }

    private DensityMapping(Entry[] densityMappingEntries) {
        Arrays.sort(densityMappingEntries, Comparator.comparingInt(new ToIntFunction() { // from class: com.android.server.display.DensityMapping$$ExternalSyntheticLambda0
            @Override // java.util.function.ToIntFunction
            public final int applyAsInt(Object obj) {
                int i;
                i = ((DensityMapping.Entry) obj).squaredDiagonal;
                return i;
            }
        }));
        this.mSortedDensityMappingEntries = densityMappingEntries;
        verifyDensityMapping(densityMappingEntries);
    }

    public int getDensityForResolution(int width, int height) {
        int squaredDiagonal = (width * width) + (height * height);
        Entry left = Entry.ZEROES;
        Entry right = null;
        Entry[] entryArr = this.mSortedDensityMappingEntries;
        int length = entryArr.length;
        int i = 0;
        while (true) {
            if (i >= length) {
                break;
            }
            Entry entry = entryArr[i];
            if (entry.squaredDiagonal <= squaredDiagonal) {
                left = entry;
                i++;
            } else {
                right = entry;
                break;
            }
        }
        if (left.squaredDiagonal == squaredDiagonal) {
            return left.density;
        }
        if (right == null) {
            right = left;
            left = Entry.ZEROES;
        }
        double leftDiagonal = Math.sqrt(left.squaredDiagonal);
        double rightDiagonal = Math.sqrt(right.squaredDiagonal);
        double diagonal = Math.sqrt(squaredDiagonal);
        return (int) Math.round((((diagonal - leftDiagonal) * (right.density - left.density)) / (rightDiagonal - leftDiagonal)) + left.density);
    }

    private static void verifyDensityMapping(Entry[] sortedEntries) {
        for (int i = 1; i < sortedEntries.length; i++) {
            Entry prev = sortedEntries[i - 1];
            Entry curr = sortedEntries[i];
            if (prev.squaredDiagonal == curr.squaredDiagonal) {
                throw new IllegalStateException("Found two entries in the density mapping with the same diagonal: " + prev + ", " + curr);
            }
            if (prev.density > curr.density) {
                throw new IllegalStateException("Found two entries in the density mapping with increasing diagonal but decreasing density: " + prev + ", " + curr);
            }
        }
    }

    public String toString() {
        return "DensityMapping{mDensityMappingEntries=" + Arrays.toString(this.mSortedDensityMappingEntries) + '}';
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class Entry {
        public static final Entry ZEROES = new Entry(0, 0, 0);
        public final int density;
        public final int squaredDiagonal;

        /* JADX INFO: Access modifiers changed from: package-private */
        public Entry(int width, int height, int density) {
            this.squaredDiagonal = (width * width) + (height * height);
            this.density = density;
        }

        public String toString() {
            return "DensityMappingEntry{squaredDiagonal=" + this.squaredDiagonal + ", density=" + this.density + '}';
        }
    }
}
