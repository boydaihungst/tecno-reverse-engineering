package com.android.server.utils;

import com.android.internal.util.ArrayUtils;
import com.android.internal.util.GrowingArrayUtils;
import java.util.Arrays;
/* loaded from: classes2.dex */
public class WatchedSparseBooleanMatrix extends WatchableImpl implements Snappable {
    private static final int PACKING = 32;
    static final int STEP = 64;
    static final int STRING_INUSE_INDEX = 2;
    static final int STRING_KEY_INDEX = 0;
    static final int STRING_MAP_INDEX = 1;
    private boolean[] mInUse;
    private int[] mKeys;
    private int[] mMap;
    private int mOrder;
    private int mSize;
    private int[] mValues;

    private void onChanged() {
        dispatchChange(this);
    }

    public WatchedSparseBooleanMatrix() {
        this(64);
    }

    public WatchedSparseBooleanMatrix(int initialCapacity) {
        this.mOrder = initialCapacity;
        if (initialCapacity < 64) {
            this.mOrder = 64;
        }
        if (this.mOrder % 64 != 0) {
            this.mOrder = ((initialCapacity / 64) + 1) * 64;
        }
        int i = this.mOrder;
        if (i < 64 || i % 64 != 0) {
            throw new RuntimeException("mOrder is " + this.mOrder + " initCap is " + initialCapacity);
        }
        this.mInUse = ArrayUtils.newUnpaddedBooleanArray(i);
        this.mKeys = ArrayUtils.newUnpaddedIntArray(this.mOrder);
        this.mMap = ArrayUtils.newUnpaddedIntArray(this.mOrder);
        int i2 = this.mOrder;
        this.mValues = ArrayUtils.newUnpaddedIntArray((i2 * i2) / 32);
        this.mSize = 0;
    }

    private WatchedSparseBooleanMatrix(WatchedSparseBooleanMatrix r) {
        copyFrom(r);
    }

    public void copyFrom(WatchedSparseBooleanMatrix src) {
        this.mOrder = src.mOrder;
        this.mSize = src.mSize;
        this.mKeys = (int[]) src.mKeys.clone();
        this.mMap = (int[]) src.mMap.clone();
        this.mInUse = (boolean[]) src.mInUse.clone();
        this.mValues = (int[]) src.mValues.clone();
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.utils.Snappable
    public WatchedSparseBooleanMatrix snapshot() {
        return new WatchedSparseBooleanMatrix(this);
    }

    public boolean get(int row, int col) {
        return get(row, col, false);
    }

    public boolean get(int row, int col, boolean valueIfKeyNotFound) {
        int r = indexOfKey(row, false);
        int c = indexOfKey(col, false);
        if (r >= 0 && c >= 0) {
            return valueAt(r, c);
        }
        return valueIfKeyNotFound;
    }

    public void put(int row, int col, boolean value) {
        int r = indexOfKey(row);
        int c = indexOfKey(col);
        if (r < 0 || c < 0) {
            if (r < 0) {
                indexOfKey(row, true);
            }
            if (c < 0) {
                indexOfKey(col, true);
            }
            r = indexOfKey(row);
            c = indexOfKey(col);
        }
        if (r >= 0 && c >= 0) {
            setValueAt(r, c, value);
            return;
        }
        throw new RuntimeException("matrix overflow");
    }

    public void deleteKey(int key) {
        int i = indexOfKey(key, false);
        if (i >= 0) {
            removeAt(i);
        }
    }

    public void removeAt(int index) {
        validateIndex(index);
        this.mInUse[this.mMap[index]] = false;
        int[] iArr = this.mKeys;
        System.arraycopy(iArr, index + 1, iArr, index, this.mSize - (index + 1));
        int[] iArr2 = this.mKeys;
        int i = this.mSize;
        iArr2[i - 1] = 0;
        int[] iArr3 = this.mMap;
        System.arraycopy(iArr3, index + 1, iArr3, index, i - (index + 1));
        int[] iArr4 = this.mMap;
        int i2 = this.mSize;
        iArr4[i2 - 1] = 0;
        this.mSize = i2 - 1;
        onChanged();
    }

    public void removeRange(int fromIndex, int toIndex) {
        if (toIndex < fromIndex) {
            throw new ArrayIndexOutOfBoundsException("toIndex < fromIndex");
        }
        int num = toIndex - fromIndex;
        if (num == 0) {
            return;
        }
        validateIndex(fromIndex);
        validateIndex(toIndex - 1);
        for (int i = fromIndex; i < toIndex; i++) {
            this.mInUse[this.mMap[i]] = false;
        }
        int[] iArr = this.mKeys;
        System.arraycopy(iArr, toIndex, iArr, fromIndex, this.mSize - toIndex);
        int[] iArr2 = this.mMap;
        System.arraycopy(iArr2, toIndex, iArr2, fromIndex, this.mSize - toIndex);
        int i2 = this.mSize - num;
        while (true) {
            int i3 = this.mSize;
            if (i2 < i3) {
                this.mKeys[i2] = 0;
                this.mMap[i2] = 0;
                i2++;
            } else {
                this.mSize = i3 - num;
                onChanged();
                return;
            }
        }
    }

    public int size() {
        return this.mSize;
    }

    public void clear() {
        this.mSize = 0;
        Arrays.fill(this.mInUse, false);
        onChanged();
    }

    public int keyAt(int index) {
        validateIndex(index);
        return this.mKeys[index];
    }

    private boolean valueAtInternal(int row, int col) {
        int element = (this.mOrder * row) + col;
        int offset = element / 32;
        int mask = 1 << (element % 32);
        return (this.mValues[offset] & mask) != 0;
    }

    public boolean valueAt(int rowIndex, int colIndex) {
        validateIndex(rowIndex, colIndex);
        int[] iArr = this.mMap;
        int r = iArr[rowIndex];
        int c = iArr[colIndex];
        return valueAtInternal(r, c);
    }

    private void setValueAtInternal(int row, int col, boolean value) {
        int element = (this.mOrder * row) + col;
        int offset = element / 32;
        int mask = 1 << (element % 32);
        if (value) {
            int[] iArr = this.mValues;
            iArr[offset] = iArr[offset] | mask;
            return;
        }
        int[] iArr2 = this.mValues;
        iArr2[offset] = iArr2[offset] & (~mask);
    }

    public void setValueAt(int rowIndex, int colIndex, boolean value) {
        validateIndex(rowIndex, colIndex);
        int[] iArr = this.mMap;
        int r = iArr[rowIndex];
        int c = iArr[colIndex];
        setValueAtInternal(r, c, value);
        onChanged();
    }

    public int indexOfKey(int key) {
        return binarySearch(this.mKeys, this.mSize, key);
    }

    public boolean contains(int key) {
        return indexOfKey(key) >= 0;
    }

    private int indexOfKey(int key, boolean grow) {
        int i = binarySearch(this.mKeys, this.mSize, key);
        if (i < 0 && grow) {
            i = ~i;
            if (this.mSize >= this.mOrder) {
                growMatrix();
            }
            int newIndex = nextFree(true);
            this.mKeys = GrowingArrayUtils.insert(this.mKeys, this.mSize, i, key);
            this.mMap = GrowingArrayUtils.insert(this.mMap, this.mSize, i, newIndex);
            this.mSize++;
            int valueRow = this.mOrder / 32;
            int offset = newIndex / 32;
            int mask = ~(1 << (newIndex % 32));
            Arrays.fill(this.mValues, newIndex * valueRow, (newIndex + 1) * valueRow, 0);
            for (int n = 0; n < this.mSize; n++) {
                int[] iArr = this.mValues;
                int i2 = (n * valueRow) + offset;
                iArr[i2] = iArr[i2] & mask;
            }
        }
        return i;
    }

    private void validateIndex(int index) {
        if (index >= this.mSize) {
            throw new ArrayIndexOutOfBoundsException(index);
        }
    }

    private void validateIndex(int row, int col) {
        validateIndex(row);
        validateIndex(col);
    }

    private void growMatrix() {
        resizeMatrix(this.mOrder + 64);
    }

    private void resizeMatrix(int newOrder) {
        if (newOrder % 64 != 0) {
            throw new IllegalArgumentException("matrix order " + newOrder + " is not a multiple of 64");
        }
        int minOrder = Math.min(this.mOrder, newOrder);
        boolean[] newInUse = ArrayUtils.newUnpaddedBooleanArray(newOrder);
        System.arraycopy(this.mInUse, 0, newInUse, 0, minOrder);
        int[] newMap = ArrayUtils.newUnpaddedIntArray(newOrder);
        System.arraycopy(this.mMap, 0, newMap, 0, minOrder);
        int[] newKeys = ArrayUtils.newUnpaddedIntArray(newOrder);
        System.arraycopy(this.mKeys, 0, newKeys, 0, minOrder);
        int[] newValues = ArrayUtils.newUnpaddedIntArray((newOrder * newOrder) / 32);
        for (int i = 0; i < minOrder; i++) {
            int row = (this.mOrder * i) / 32;
            int newRow = (newOrder * i) / 32;
            System.arraycopy(this.mValues, row, newValues, newRow, minOrder / 32);
        }
        this.mInUse = newInUse;
        this.mMap = newMap;
        this.mKeys = newKeys;
        this.mValues = newValues;
        this.mOrder = newOrder;
    }

    private int nextFree(boolean acquire) {
        int i = 0;
        while (true) {
            boolean[] zArr = this.mInUse;
            if (i < zArr.length) {
                if (zArr[i]) {
                    i++;
                } else {
                    zArr[i] = acquire;
                    return i;
                }
            } else {
                throw new RuntimeException();
            }
        }
    }

    private int lastInuse() {
        for (int i = this.mOrder - 1; i >= 0; i--) {
            if (this.mInUse[i]) {
                for (int j = 0; j < this.mSize; j++) {
                    if (this.mMap[j] == i) {
                        return j;
                    }
                }
                throw new IndexOutOfBoundsException();
            }
        }
        return -1;
    }

    private void pack() {
        int i = this.mSize;
        if (i == 0 || i == this.mOrder) {
            return;
        }
        int dst = nextFree(false);
        while (dst < this.mSize) {
            this.mInUse[dst] = true;
            int srcIndex = lastInuse();
            int[] iArr = this.mMap;
            int src = iArr[srcIndex];
            this.mInUse[src] = false;
            iArr[srcIndex] = dst;
            int[] iArr2 = this.mValues;
            int i2 = this.mOrder;
            System.arraycopy(iArr2, (src * i2) / 32, iArr2, (dst * i2) / 32, i2 / 32);
            int srcOffset = src / 32;
            int srcMask = 1 << (src % 32);
            int dstOffset = dst / 32;
            int dstMask = 1 << (dst % 32);
            int i3 = 0;
            while (true) {
                int i4 = this.mOrder;
                if (i3 < i4) {
                    int[] iArr3 = this.mValues;
                    if ((iArr3[srcOffset] & srcMask) == 0) {
                        iArr3[dstOffset] = iArr3[dstOffset] & (~dstMask);
                    } else {
                        iArr3[dstOffset] = iArr3[dstOffset] | dstMask;
                    }
                    srcOffset += i4 / 32;
                    dstOffset += i4 / 32;
                    i3++;
                }
            }
            dst = nextFree(false);
        }
    }

    public void compact() {
        pack();
        int i = this.mOrder;
        int unused = (i - this.mSize) / 64;
        if (unused > 0) {
            resizeMatrix(i - (unused * 64));
        }
    }

    public int[] keys() {
        return Arrays.copyOf(this.mKeys, this.mSize);
    }

    public int capacity() {
        return this.mOrder;
    }

    public void setCapacity(int capacity) {
        if (capacity <= this.mOrder) {
            return;
        }
        if (capacity % 64 != 0) {
            capacity = ((capacity / 64) + 1) * 64;
        }
        resizeMatrix(capacity);
    }

    public int hashCode() {
        int hashCode = this.mSize;
        int hashCode2 = (((hashCode * 31) + Arrays.hashCode(this.mKeys)) * 31) + Arrays.hashCode(this.mMap);
        for (int i = 0; i < this.mSize; i++) {
            int row = this.mMap[i];
            for (int j = 0; j < this.mSize; j++) {
                hashCode2 = (hashCode2 * 31) + (valueAtInternal(row, this.mMap[j]) ? 1 : 0);
            }
        }
        return hashCode2;
    }

    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (that instanceof WatchedSparseBooleanMatrix) {
            WatchedSparseBooleanMatrix other = (WatchedSparseBooleanMatrix) that;
            if (this.mSize == other.mSize && Arrays.equals(this.mKeys, other.mKeys)) {
                for (int i = 0; i < this.mSize; i++) {
                    int row = this.mMap[i];
                    for (int j = 0; j < this.mSize; j++) {
                        int col = this.mMap[j];
                        if (valueAtInternal(row, col) != other.valueAtInternal(row, col)) {
                            return false;
                        }
                    }
                }
                return true;
            }
            return false;
        }
        return false;
    }

    String[] matrixToStringMeta() {
        String[] result = new String[3];
        StringBuilder k = new StringBuilder();
        for (int i = 0; i < this.mSize; i++) {
            k.append(this.mKeys[i]);
            if (i < this.mSize - 1) {
                k.append(" ");
            }
        }
        result[0] = k.substring(0);
        StringBuilder m = new StringBuilder();
        for (int i2 = 0; i2 < this.mSize; i2++) {
            m.append(this.mMap[i2]);
            if (i2 < this.mSize - 1) {
                m.append(" ");
            }
        }
        result[1] = m.substring(0);
        StringBuilder u = new StringBuilder();
        for (int i3 = 0; i3 < this.mOrder; i3++) {
            u.append(this.mInUse[i3] ? "1" : "0");
        }
        result[2] = u.substring(0);
        return result;
    }

    String[] matrixToStringRaw() {
        String[] result = new String[this.mOrder];
        int i = 0;
        while (true) {
            int i2 = this.mOrder;
            if (i < i2) {
                StringBuilder line = new StringBuilder(i2);
                for (int j = 0; j < this.mOrder; j++) {
                    line.append(valueAtInternal(i, j) ? "1" : "0");
                }
                result[i] = line.substring(0);
                i++;
            } else {
                return result;
            }
        }
    }

    String[] matrixToStringCooked() {
        String[] result = new String[this.mSize];
        int i = 0;
        while (true) {
            int i2 = this.mSize;
            if (i < i2) {
                int row = this.mMap[i];
                StringBuilder line = new StringBuilder(i2);
                for (int j = 0; j < this.mSize; j++) {
                    line.append(valueAtInternal(row, this.mMap[j]) ? "1" : "0");
                }
                result[i] = line.substring(0);
                i++;
            } else {
                return result;
            }
        }
    }

    public String[] matrixToString(boolean raw) {
        String[] data;
        String[] meta = matrixToStringMeta();
        if (raw) {
            data = matrixToStringRaw();
        } else {
            data = matrixToStringCooked();
        }
        String[] result = new String[meta.length + data.length];
        System.arraycopy(meta, 0, result, 0, meta.length);
        System.arraycopy(data, 0, result, meta.length, data.length);
        return result;
    }

    public String toString() {
        return "{" + this.mSize + "x" + this.mSize + "}";
    }

    private static int binarySearch(int[] array, int size, int value) {
        int lo = 0;
        int hi = size - 1;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            int midVal = array[mid];
            if (midVal < value) {
                lo = mid + 1;
            } else if (midVal > value) {
                hi = mid - 1;
            } else {
                return mid;
            }
        }
        return ~lo;
    }
}
