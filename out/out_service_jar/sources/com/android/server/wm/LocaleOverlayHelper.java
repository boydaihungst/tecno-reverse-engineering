package com.android.server.wm;

import android.os.LocaleList;
import java.util.Locale;
/* loaded from: classes2.dex */
final class LocaleOverlayHelper {
    LocaleOverlayHelper() {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static LocaleList combineLocalesIfOverlayExists(LocaleList overlayLocales, LocaleList baseLocales) {
        if (overlayLocales == null || overlayLocales.isEmpty()) {
            return overlayLocales;
        }
        return combineLocales(overlayLocales, baseLocales);
    }

    private static LocaleList combineLocales(LocaleList overlayLocales, LocaleList baseLocales) {
        Locale[] combinedLocales = new Locale[overlayLocales.size() + baseLocales.size()];
        for (int i = 0; i < overlayLocales.size(); i++) {
            combinedLocales[i] = overlayLocales.get(i);
        }
        for (int i2 = 0; i2 < baseLocales.size(); i2++) {
            combinedLocales[overlayLocales.size() + i2] = baseLocales.get(i2);
        }
        return new LocaleList(combinedLocales);
    }
}
