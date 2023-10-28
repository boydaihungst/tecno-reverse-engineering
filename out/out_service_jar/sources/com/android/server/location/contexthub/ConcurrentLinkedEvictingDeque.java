package com.android.server.location.contexthub;

import java.util.concurrent.ConcurrentLinkedDeque;
/* loaded from: classes.dex */
public class ConcurrentLinkedEvictingDeque<E> extends ConcurrentLinkedDeque<E> {
    private int mSize;

    /* JADX INFO: Access modifiers changed from: package-private */
    public ConcurrentLinkedEvictingDeque(int size) {
        this.mSize = size;
    }

    @Override // java.util.concurrent.ConcurrentLinkedDeque, java.util.AbstractCollection, java.util.Collection, java.util.Deque, java.util.Queue
    public boolean add(E elem) {
        boolean add;
        synchronized (this) {
            if (size() == this.mSize) {
                poll();
            }
            add = super.add(elem);
        }
        return add;
    }
}
