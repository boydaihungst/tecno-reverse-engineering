package com.android.server.timezonedetector;

import android.os.SystemClock;
import android.os.TimestampedValue;
import android.util.IndentingPrintWriter;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Iterator;
/* loaded from: classes2.dex */
public final class ReferenceWithHistory<V> {
    private final int mMaxHistorySize;
    private int mSetCount;
    private ArrayDeque<TimestampedValue<V>> mValues;

    public ReferenceWithHistory(int maxHistorySize) {
        if (maxHistorySize < 1) {
            throw new IllegalArgumentException("maxHistorySize < 1: " + maxHistorySize);
        }
        this.mMaxHistorySize = maxHistorySize;
    }

    public V get() {
        ArrayDeque<TimestampedValue<V>> arrayDeque = this.mValues;
        if (arrayDeque == null || arrayDeque.isEmpty()) {
            return null;
        }
        TimestampedValue<V> valueHolder = this.mValues.getFirst();
        return (V) valueHolder.getValue();
    }

    public V set(V newValue) {
        if (this.mValues == null) {
            this.mValues = new ArrayDeque<>(this.mMaxHistorySize);
        }
        if (this.mValues.size() >= this.mMaxHistorySize) {
            this.mValues.removeLast();
        }
        V previous = get();
        TimestampedValue<V> valueHolder = new TimestampedValue<>(SystemClock.elapsedRealtime(), newValue);
        this.mValues.addFirst(valueHolder);
        this.mSetCount++;
        return previous;
    }

    public void dump(IndentingPrintWriter ipw) {
        ArrayDeque<TimestampedValue<V>> arrayDeque = this.mValues;
        if (arrayDeque == null) {
            ipw.println("{Empty}");
        } else {
            int i = this.mSetCount - arrayDeque.size();
            Iterator<TimestampedValue<V>> reverseIterator = this.mValues.descendingIterator();
            while (reverseIterator.hasNext()) {
                TimestampedValue<V> valueHolder = reverseIterator.next();
                ipw.print(i);
                ipw.print("@");
                ipw.print(Duration.ofMillis(valueHolder.getReferenceTimeMillis()).toString());
                ipw.print(": ");
                ipw.println(valueHolder.getValue());
                i++;
            }
        }
        ipw.flush();
    }

    public int getHistoryCount() {
        ArrayDeque<TimestampedValue<V>> arrayDeque = this.mValues;
        if (arrayDeque == null) {
            return 0;
        }
        return arrayDeque.size();
    }

    public String toString() {
        return String.valueOf(get());
    }
}
