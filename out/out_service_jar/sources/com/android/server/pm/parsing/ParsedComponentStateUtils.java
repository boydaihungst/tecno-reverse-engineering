package com.android.server.pm.parsing;

import android.util.Pair;
import com.android.server.pm.pkg.PackageStateInternal;
import com.android.server.pm.pkg.component.ParsedComponent;
/* loaded from: classes2.dex */
public class ParsedComponentStateUtils {
    public static Pair<CharSequence, Integer> getNonLocalizedLabelAndIcon(ParsedComponent component, PackageStateInternal pkgSetting, int userId) {
        CharSequence label = component.getNonLocalizedLabel();
        int icon = component.getIcon();
        Pair<String, Integer> overrideLabelIcon = pkgSetting == null ? null : pkgSetting.getUserStateOrDefault(userId).getOverrideLabelIconForComponent(component.getComponentName());
        if (overrideLabelIcon != null) {
            if (overrideLabelIcon.first != null) {
                label = (CharSequence) overrideLabelIcon.first;
            }
            if (overrideLabelIcon.second != null) {
                icon = ((Integer) overrideLabelIcon.second).intValue();
            }
        }
        return Pair.create(label, Integer.valueOf(icon));
    }
}
