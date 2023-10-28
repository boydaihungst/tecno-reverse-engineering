package com.android.internal.app;

import android.content.ComponentName;
import android.content.pm.ResolveInfo;
import java.util.Comparator;
/* loaded from: classes4.dex */
interface ResolverComparatorModel {
    Comparator<ResolveInfo> getComparator();

    float getScore(ComponentName componentName);

    void notifyOnTargetSelected(ComponentName componentName);
}
