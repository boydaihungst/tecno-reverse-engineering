package com.android.internal.accessibility.dialog;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Context;
import com.android.internal.R;
import com.android.internal.accessibility.dialog.TargetAdapter;
import com.android.internal.accessibility.util.AccessibilityUtils;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes4.dex */
public class ToggleAccessibilityServiceTarget extends AccessibilityServiceTarget {
    /* JADX INFO: Access modifiers changed from: package-private */
    public ToggleAccessibilityServiceTarget(Context context, int shortcutType, AccessibilityServiceInfo serviceInfo) {
        super(context, shortcutType, 2, serviceInfo);
    }

    @Override // com.android.internal.accessibility.dialog.AccessibilityTarget, com.android.internal.accessibility.dialog.TargetOperations
    public void updateActionItem(TargetAdapter.ViewHolder holder, int shortcutMenuMode) {
        super.updateActionItem(holder, shortcutMenuMode);
        boolean isEditMenuMode = shortcutMenuMode == 1;
        holder.mStatusView.setVisibility(isEditMenuMode ? 8 : 0);
        holder.mStatusView.setText(getStateDescription());
    }

    @Override // com.android.internal.accessibility.dialog.AccessibilityTarget
    public CharSequence getStateDescription() {
        int statusResId;
        if (AccessibilityUtils.isAccessibilityServiceEnabled(getContext(), getId())) {
            statusResId = R.string.accessibility_shortcut_menu_item_status_on;
        } else {
            statusResId = R.string.accessibility_shortcut_menu_item_status_off;
        }
        return getContext().getString(statusResId);
    }
}
