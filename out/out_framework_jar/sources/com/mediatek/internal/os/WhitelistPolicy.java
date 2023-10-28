package com.mediatek.internal.os;

import com.mediatek.internal.os.DispatchPolicyImpl;
/* loaded from: classes4.dex */
public class WhitelistPolicy extends DispatchPolicyImpl implements AppDispatchPolicy {
    @Override // com.mediatek.internal.os.AppDispatchPolicy
    public boolean checkPackageName(String packageName) {
        return DispatchPolicyImpl.PackageList.find(packageName);
    }
}
