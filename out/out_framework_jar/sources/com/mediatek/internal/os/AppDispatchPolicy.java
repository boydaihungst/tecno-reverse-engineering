package com.mediatek.internal.os;
/* loaded from: classes4.dex */
public interface AppDispatchPolicy {
    boolean checkPackageName(String str);

    default boolean checkZygotePolicy(int zygotePolicyFlags) {
        return ((Integer.MIN_VALUE & zygotePolicyFlags) == 0 || (zygotePolicyFlags & 1) == 0) ? false : true;
    }

    default boolean checkruntimeFlags(int runtimeFlags) {
        return true;
    }
}
