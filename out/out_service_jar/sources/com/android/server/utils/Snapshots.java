package com.android.server.utils;

import android.util.SparseArray;
import android.util.SparseIntArray;
import android.util.SparseSetArray;
/* loaded from: classes2.dex */
public class Snapshots {
    public static <T> T maybeSnapshot(T o) {
        if (o instanceof Snappable) {
            return (T) ((Snappable) o).snapshot();
        }
        return o;
    }

    public static <E> void copy(SparseArray<E> dst, SparseArray<E> src) {
        if (dst.size() != 0) {
            throw new IllegalArgumentException("copy destination is not empty");
        }
        int end = src.size();
        for (int i = 0; i < end; i++) {
            dst.put(src.keyAt(i), src.valueAt(i));
        }
    }

    public static <E> void copy(SparseSetArray<E> dst, SparseSetArray<E> src) {
        if (dst.size() != 0) {
            throw new IllegalArgumentException("copy destination is not empty");
        }
        int end = src.size();
        for (int i = 0; i < end; i++) {
            int size = src.sizeAt(i);
            for (int j = 0; j < size; j++) {
                dst.add(src.keyAt(i), src.valueAt(i, j));
            }
        }
    }

    public static void snapshot(SparseIntArray dst, SparseIntArray src) {
        if (dst.size() != 0) {
            throw new IllegalArgumentException("snapshot destination is not empty");
        }
        int end = src.size();
        for (int i = 0; i < end; i++) {
            dst.put(src.keyAt(i), src.valueAt(i));
        }
    }

    /* JADX DEBUG: Multi-variable search result rejected for r4v0, resolved type: android.util.SparseArray<E extends com.android.server.utils.Snappable<E>> */
    /* JADX WARN: Multi-variable type inference failed */
    public static <E extends Snappable<E>> void snapshot(SparseArray<E> dst, SparseArray<E> src) {
        if (dst.size() != 0) {
            throw new IllegalArgumentException("snapshot destination is not empty");
        }
        int end = src.size();
        for (int i = 0; i < end; i++) {
            dst.put(src.keyAt(i), (Snappable) src.valueAt(i).snapshot());
        }
    }

    public static <E extends Snappable<E>> void snapshot(SparseSetArray<E> dst, SparseSetArray<E> src) {
        if (dst.size() != 0) {
            throw new IllegalArgumentException("snapshot destination is not empty");
        }
        int end = src.size();
        for (int i = 0; i < end; i++) {
            int size = src.sizeAt(i);
            for (int j = 0; j < size; j++) {
                dst.add(src.keyAt(i), (Snappable) ((Snappable) src.valueAt(i, j)).snapshot());
            }
        }
    }
}
