package com.android.net.module.util;

import android.util.SparseArray;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Predicate;
/* loaded from: classes4.dex */
public final class CollectionUtils {
    private CollectionUtils() {
    }

    public static <T> boolean isEmpty(T[] array) {
        return array == null || array.length == 0;
    }

    public static <T> boolean isEmpty(Collection<T> collection) {
        return collection == null || collection.isEmpty();
    }

    public static int[] toIntArray(Collection<Integer> list) {
        int[] array = new int[list.size()];
        int i = 0;
        for (Integer item : list) {
            array[i] = item.intValue();
            i++;
        }
        return array;
    }

    public static long[] toLongArray(Collection<Long> list) {
        long[] array = new long[list.size()];
        int i = 0;
        for (Long item : list) {
            array[i] = item.longValue();
            i++;
        }
        return array;
    }

    public static <T> boolean all(Collection<T> elem, Predicate<T> predicate) {
        for (T e : elem) {
            if (!predicate.test(e)) {
                return false;
            }
        }
        return true;
    }

    public static <T> boolean any(Collection<T> elem, Predicate<T> predicate) {
        return indexOf(elem, predicate) >= 0;
    }

    public static <T> int indexOf(Collection<T> elem, Predicate<T> predicate) {
        int idx = 0;
        for (T e : elem) {
            if (predicate.test(e)) {
                return idx;
            }
            idx++;
        }
        return -1;
    }

    public static <T> boolean any(SparseArray<T> array, Predicate<T> predicate) {
        for (int i = 0; i < array.size(); i++) {
            if (predicate.test(array.valueAt(i))) {
                return true;
            }
        }
        return false;
    }

    public static boolean contains(short[] array, short value) {
        if (array == null) {
            return false;
        }
        for (short s : array) {
            if (s == value) {
                return true;
            }
        }
        return false;
    }

    public static boolean contains(int[] array, int value) {
        if (array == null) {
            return false;
        }
        for (int element : array) {
            if (element == value) {
                return true;
            }
        }
        return false;
    }

    public static <T> boolean contains(T[] array, T value) {
        return indexOf(array, value) != -1;
    }

    public static <T> int indexOf(T[] array, T value) {
        if (array == null) {
            return -1;
        }
        for (int i = 0; i < array.length; i++) {
            if (Objects.equals(array[i], value)) {
                return i;
            }
        }
        return -1;
    }

    public static <T> ArrayList<T> filter(Collection<T> source, Predicate<T> test) {
        ArrayList<T> matches = new ArrayList<>();
        for (T e : source) {
            if (test.test(e)) {
                matches.add(e);
            }
        }
        return matches;
    }

    public static long total(long[] array) {
        long total = 0;
        if (array != null) {
            for (long value : array) {
                total += value;
            }
        }
        return total;
    }

    public static <T> boolean containsAny(Collection<T> haystack, Collection<? extends T> needles) {
        for (T needle : needles) {
            if (haystack.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    public static <T> boolean containsAll(Collection<T> haystack, Collection<? extends T> needles) {
        return haystack.containsAll(needles);
    }
}
