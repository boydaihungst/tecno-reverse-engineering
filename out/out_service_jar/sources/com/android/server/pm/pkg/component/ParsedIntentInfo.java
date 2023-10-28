package com.android.server.pm.pkg.component;

import android.content.IntentFilter;
/* loaded from: classes2.dex */
public interface ParsedIntentInfo {
    int getIcon();

    IntentFilter getIntentFilter();

    int getLabelRes();

    CharSequence getNonLocalizedLabel();

    boolean isHasDefault();
}
