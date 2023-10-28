package com.android.server.pm.pkg;

import android.content.ComponentName;
import android.content.pm.pkg.FrameworkPackageUserState;
import android.util.Pair;
import com.android.server.utils.WatchedArrayMap;
import com.android.server.utils.WatchedArraySet;
/* loaded from: classes2.dex */
public interface PackageUserStateInternal extends PackageUserState, FrameworkPackageUserState {
    public static final PackageUserStateInternal DEFAULT = new PackageUserStateDefault();

    WatchedArraySet<String> getDisabledComponentsNoCopy();

    WatchedArraySet<String> getEnabledComponentsNoCopy();

    Pair<String, Integer> getOverrideLabelIconForComponent(ComponentName componentName);

    WatchedArrayMap<String, SuspendParams> getSuspendParams();
}
