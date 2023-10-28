package com.transsion.hubcore.griffin.lib.app;

import java.util.HashSet;
import java.util.Set;
/* loaded from: classes2.dex */
public class TranIntentInfo {
    private final Set<String> mActions = new HashSet();
    private final Set<String> mCategories = new HashSet();

    public final void addAction(String action) {
        if (!this.mActions.contains(action)) {
            this.mActions.add(action.intern());
        }
    }

    public final void addCatetory(String category) {
        if (!this.mCategories.contains(category)) {
            this.mCategories.add(category.intern());
        }
    }

    public final boolean hasAction(String action) {
        return action != null && this.mActions.contains(action);
    }

    public final boolean hasCategory(String category) {
        Set<String> set = this.mCategories;
        return set != null && set.contains(category);
    }

    public final Set<String> getActions() {
        return this.mActions;
    }

    public final Set<String> getCategories() {
        return this.mCategories;
    }

    public final boolean match(String action, String category) {
        return hasAction(action) && hasCategory(category);
    }
}
