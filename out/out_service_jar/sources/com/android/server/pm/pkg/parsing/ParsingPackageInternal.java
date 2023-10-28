package com.android.server.pm.pkg.parsing;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public interface ParsingPackageInternal {
    String getOverlayCategory();

    int getOverlayPriority();

    String getOverlayTarget();

    String getOverlayTargetOverlayableName();

    boolean isOverlayIsStatic();
}
