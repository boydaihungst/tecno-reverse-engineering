package com.android.framework.protobuf;

import com.android.framework.protobuf.Internal;
import java.util.Arrays;
import java.util.Collection;
import java.util.RandomAccess;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes4.dex */
public final class BooleanArrayList extends AbstractProtobufList<Boolean> implements Internal.BooleanList, RandomAccess, PrimitiveNonBoxingCollection {
    private static final BooleanArrayList EMPTY_LIST;
    private boolean[] array;
    private int size;

    static {
        BooleanArrayList booleanArrayList = new BooleanArrayList(new boolean[0], 0);
        EMPTY_LIST = booleanArrayList;
        booleanArrayList.makeImmutable();
    }

    public static BooleanArrayList emptyList() {
        return EMPTY_LIST;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public BooleanArrayList() {
        this(new boolean[10], 0);
    }

    private BooleanArrayList(boolean[] other, int size) {
        this.array = other;
        this.size = size;
    }

    @Override // java.util.AbstractList
    protected void removeRange(int fromIndex, int toIndex) {
        ensureIsMutable();
        if (toIndex < fromIndex) {
            throw new IndexOutOfBoundsException("toIndex < fromIndex");
        }
        boolean[] zArr = this.array;
        System.arraycopy(zArr, toIndex, zArr, fromIndex, this.size - toIndex);
        this.size -= toIndex - fromIndex;
        this.modCount++;
    }

    @Override // com.android.framework.protobuf.AbstractProtobufList, java.util.AbstractList, java.util.Collection, java.util.List
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BooleanArrayList)) {
            return super.equals(o);
        }
        BooleanArrayList other = (BooleanArrayList) o;
        if (this.size != other.size) {
            return false;
        }
        boolean[] arr = other.array;
        for (int i = 0; i < this.size; i++) {
            if (this.array[i] != arr[i]) {
                return false;
            }
        }
        return true;
    }

    @Override // com.android.framework.protobuf.AbstractProtobufList, java.util.AbstractList, java.util.Collection, java.util.List
    public int hashCode() {
        int result = 1;
        for (int i = 0; i < this.size; i++) {
            result = (result * 31) + Internal.hashBoolean(this.array[i]);
        }
        return result;
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX DEBUG: Return type fixed from 'com.android.framework.protobuf.Internal$BooleanList' to match base method */
    @Override // com.android.framework.protobuf.Internal.ProtobufList, com.android.framework.protobuf.Internal.BooleanList
    /* renamed from: mutableCopyWithCapacity */
    public Internal.ProtobufList<Boolean> mutableCopyWithCapacity2(int capacity) {
        if (capacity < this.size) {
            throw new IllegalArgumentException();
        }
        return new BooleanArrayList(Arrays.copyOf(this.array, capacity), this.size);
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // java.util.AbstractList, java.util.List
    public Boolean get(int index) {
        return Boolean.valueOf(getBoolean(index));
    }

    @Override // com.android.framework.protobuf.Internal.BooleanList
    public boolean getBoolean(int index) {
        ensureIndexInRange(index);
        return this.array[index];
    }

    @Override // java.util.AbstractCollection, java.util.Collection, java.util.List
    public int size() {
        return this.size;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.framework.protobuf.AbstractProtobufList, java.util.AbstractList, java.util.List
    public Boolean set(int index, Boolean element) {
        return Boolean.valueOf(setBoolean(index, element.booleanValue()));
    }

    @Override // com.android.framework.protobuf.Internal.BooleanList
    public boolean setBoolean(int index, boolean element) {
        ensureIsMutable();
        ensureIndexInRange(index);
        boolean[] zArr = this.array;
        boolean previousValue = zArr[index];
        zArr[index] = element;
        return previousValue;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.framework.protobuf.AbstractProtobufList, java.util.AbstractList, java.util.List
    public void add(int index, Boolean element) {
        addBoolean(index, element.booleanValue());
    }

    @Override // com.android.framework.protobuf.Internal.BooleanList
    public void addBoolean(boolean element) {
        addBoolean(this.size, element);
    }

    private void addBoolean(int index, boolean element) {
        int i;
        ensureIsMutable();
        if (index < 0 || index > (i = this.size)) {
            throw new IndexOutOfBoundsException(makeOutOfBoundsExceptionMessage(index));
        }
        boolean[] zArr = this.array;
        if (i < zArr.length) {
            System.arraycopy(zArr, index, zArr, index + 1, i - index);
        } else {
            int length = ((i * 3) / 2) + 1;
            boolean[] newArray = new boolean[length];
            System.arraycopy(zArr, 0, newArray, 0, index);
            System.arraycopy(this.array, index, newArray, index + 1, this.size - index);
            this.array = newArray;
        }
        this.array[index] = element;
        this.size++;
        this.modCount++;
    }

    @Override // com.android.framework.protobuf.AbstractProtobufList, java.util.AbstractCollection, java.util.Collection, java.util.List
    public boolean addAll(Collection<? extends Boolean> collection) {
        ensureIsMutable();
        Internal.checkNotNull(collection);
        if (!(collection instanceof BooleanArrayList)) {
            return super.addAll(collection);
        }
        BooleanArrayList list = (BooleanArrayList) collection;
        int i = list.size;
        if (i == 0) {
            return false;
        }
        int i2 = this.size;
        int overflow = Integer.MAX_VALUE - i2;
        if (overflow < i) {
            throw new OutOfMemoryError();
        }
        int newSize = i2 + i;
        boolean[] zArr = this.array;
        if (newSize > zArr.length) {
            this.array = Arrays.copyOf(zArr, newSize);
        }
        System.arraycopy(list.array, 0, this.array, this.size, list.size);
        this.size = newSize;
        this.modCount++;
        return true;
    }

    @Override // com.android.framework.protobuf.AbstractProtobufList, java.util.AbstractCollection, java.util.Collection, java.util.List
    public boolean remove(Object o) {
        ensureIsMutable();
        for (int i = 0; i < this.size; i++) {
            if (o.equals(Boolean.valueOf(this.array[i]))) {
                boolean[] zArr = this.array;
                System.arraycopy(zArr, i + 1, zArr, i, (this.size - i) - 1);
                this.size--;
                this.modCount++;
                return true;
            }
        }
        return false;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.framework.protobuf.AbstractProtobufList, java.util.AbstractList, java.util.List
    public Boolean remove(int index) {
        int i;
        ensureIsMutable();
        ensureIndexInRange(index);
        boolean[] zArr = this.array;
        boolean value = zArr[index];
        if (index < this.size - 1) {
            System.arraycopy(zArr, index + 1, zArr, index, (i - index) - 1);
        }
        this.size--;
        this.modCount++;
        return Boolean.valueOf(value);
    }

    private void ensureIndexInRange(int index) {
        if (index < 0 || index >= this.size) {
            throw new IndexOutOfBoundsException(makeOutOfBoundsExceptionMessage(index));
        }
    }

    private String makeOutOfBoundsExceptionMessage(int index) {
        return "Index:" + index + ", Size:" + this.size;
    }
}
