package com.android.server.pm;

import android.content.pm.ShortcutInfo;
import java.util.function.Predicate;
/* compiled from: D8$$SyntheticClass */
/* loaded from: classes2.dex */
public final /* synthetic */ class ShortcutPackage$$ExternalSyntheticLambda27 implements Predicate {
    @Override // java.util.function.Predicate
    public final boolean test(Object obj) {
        return ((ShortcutInfo) obj).isNonManifestVisible();
    }
}
