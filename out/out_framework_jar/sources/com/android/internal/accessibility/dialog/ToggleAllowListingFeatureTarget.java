package com.android.internal.accessibility.dialog;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.provider.Settings;
import com.android.internal.R;
import com.android.internal.accessibility.dialog.TargetAdapter;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes4.dex */
public class ToggleAllowListingFeatureTarget extends AccessibilityTarget {
    /* JADX INFO: Access modifiers changed from: package-private */
    public ToggleAllowListingFeatureTarget(Context context, int shortcutType, boolean isShortcutSwitched, String id, CharSequence label, Drawable icon, String key) {
        super(context, shortcutType, 2, isShortcutSwitched, id, label, icon, key);
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
        if (isFeatureEnabled()) {
            statusResId = R.string.accessibility_shortcut_menu_item_status_on;
        } else {
            statusResId = R.string.accessibility_shortcut_menu_item_status_off;
        }
        return getContext().getString(statusResId);
    }

    private boolean isFeatureEnabled() {
        return Settings.Secure.getInt(getContext().getContentResolver(), getKey(), 0) == 1;
    }
}
