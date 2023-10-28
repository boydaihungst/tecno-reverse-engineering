package com.android.server.pm.pkg.component;
/* loaded from: classes2.dex */
public interface ParsedMainComponent extends ParsedComponent {
    String[] getAttributionTags();

    String getClassName();

    int getOrder();

    String getProcessName();

    String getSplitName();

    boolean isDirectBootAware();

    boolean isEnabled();

    boolean isExported();
}
