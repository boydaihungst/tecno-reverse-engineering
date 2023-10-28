package com.android.server.display;

import java.util.ArrayList;
import java.util.List;
/* loaded from: classes.dex */
public class DisplayGroup {
    private int mChangeCount;
    private final List<LogicalDisplay> mDisplays = new ArrayList();
    private final int mGroupId;

    /* JADX INFO: Access modifiers changed from: package-private */
    public DisplayGroup(int groupId) {
        this.mGroupId = groupId;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getGroupId() {
        return this.mGroupId;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void addDisplayLocked(LogicalDisplay display) {
        if (!containsLocked(display)) {
            this.mChangeCount++;
            this.mDisplays.add(display);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean containsLocked(LogicalDisplay display) {
        return this.mDisplays.contains(display);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean removeDisplayLocked(LogicalDisplay display) {
        this.mChangeCount++;
        return this.mDisplays.remove(display);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isEmptyLocked() {
        return this.mDisplays.isEmpty();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getChangeCountLocked() {
        return this.mChangeCount;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getSizeLocked() {
        return this.mDisplays.size();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getIdLocked(int index) {
        return this.mDisplays.get(index).getDisplayIdLocked();
    }
}
