package com.android.internal.org.bouncycastle.math.ec;
/* loaded from: classes4.dex */
public abstract class AbstractECLookupTable implements ECLookupTable {
    @Override // com.android.internal.org.bouncycastle.math.ec.ECLookupTable
    public ECPoint lookupVar(int index) {
        return lookup(index);
    }
}
