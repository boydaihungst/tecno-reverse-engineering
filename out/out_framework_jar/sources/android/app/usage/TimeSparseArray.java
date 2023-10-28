package android.app.usage;

import android.util.LongSparseArray;
import android.util.Slog;
/* loaded from: classes.dex */
public class TimeSparseArray<E> extends LongSparseArray<E> {
    private static final String TAG = TimeSparseArray.class.getSimpleName();
    private boolean mWtfReported;

    public int closestIndexOnOrAfter(long time) {
        int size = size();
        int lo = 0;
        int hi = size - 1;
        int mid = -1;
        long key = -1;
        while (lo <= hi) {
            mid = lo + ((hi - lo) / 2);
            key = keyAt(mid);
            if (time > key) {
                lo = mid + 1;
            } else if (time < key) {
                hi = mid - 1;
            } else {
                return mid;
            }
        }
        if (time < key) {
            return mid;
        }
        if (time > key && lo < size) {
            return lo;
        }
        return -1;
    }

    @Override // android.util.LongSparseArray
    public void put(long key, E value) {
        if (indexOfKey(key) >= 0 && !this.mWtfReported) {
            Slog.wtf(TAG, "Overwriting value " + get(key) + " by " + value);
            this.mWtfReported = true;
        }
        super.put(key, value);
    }

    public int closestIndexOnOrBefore(long time) {
        int index = closestIndexOnOrAfter(time);
        if (index < 0) {
            return size() - 1;
        }
        if (keyAt(index) == time) {
            return index;
        }
        return index - 1;
    }
}
