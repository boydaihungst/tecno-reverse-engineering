package com.android.server.pm.pkg.component;
/* loaded from: classes2.dex */
public interface ParsedInstrumentation extends ParsedComponent {
    String getTargetPackage();

    String getTargetProcesses();

    boolean isFunctionalTest();

    boolean isHandleProfiling();
}
