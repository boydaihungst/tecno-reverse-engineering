package com.mediatek.internal.os;
/* loaded from: classes4.dex */
public class AlwaysOnPolicy implements AppDispatchPolicy {
    private static final int EXCLUDED_RUNTIME_FLAGS = 33161;

    @Override // com.mediatek.internal.os.AppDispatchPolicy
    public boolean checkruntimeFlags(int runtimeFlags) {
        return (EXCLUDED_RUNTIME_FLAGS & runtimeFlags) == 0;
    }

    @Override // com.mediatek.internal.os.AppDispatchPolicy
    public boolean checkPackageName(String packageName) {
        return true;
    }
}
