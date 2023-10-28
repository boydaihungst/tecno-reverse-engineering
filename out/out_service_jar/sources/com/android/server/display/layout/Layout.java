package com.android.server.display.layout;

import android.util.Slog;
import android.view.DisplayAddress;
import java.util.ArrayList;
import java.util.List;
/* loaded from: classes.dex */
public class Layout {
    private static final String TAG = "Layout";
    private static int sNextNonDefaultDisplayId = 1;
    private final List<Display> mDisplays = new ArrayList(2);

    public static int assignDisplayIdLocked(boolean isDefault) {
        if (isDefault) {
            return 0;
        }
        int i = sNextNonDefaultDisplayId;
        sNextNonDefaultDisplayId = i + 1;
        return i;
    }

    public String toString() {
        return this.mDisplays.toString();
    }

    public Display createDisplayLocked(DisplayAddress address, boolean isDefault, boolean isEnabled) {
        if (contains(address)) {
            Slog.w(TAG, "Attempting to add second definition for display-device: " + address);
            return null;
        } else if (isDefault && getById(0) != null) {
            Slog.w(TAG, "Ignoring attempt to add a second default display: " + address);
            return null;
        } else {
            int logicalDisplayId = assignDisplayIdLocked(isDefault);
            Display display = new Display(address, logicalDisplayId, isEnabled);
            this.mDisplays.add(display);
            return display;
        }
    }

    public void removeDisplayLocked(int id) {
        Display display = getById(id);
        if (display != null) {
            this.mDisplays.remove(display);
        }
    }

    public boolean contains(DisplayAddress address) {
        int size = this.mDisplays.size();
        for (int i = 0; i < size; i++) {
            if (address.equals(this.mDisplays.get(i).getAddress())) {
                return true;
            }
        }
        return false;
    }

    public Display getById(int id) {
        for (int i = 0; i < this.mDisplays.size(); i++) {
            Display display = this.mDisplays.get(i);
            if (id == display.getLogicalDisplayId()) {
                return display;
            }
        }
        return null;
    }

    public Display getByAddress(DisplayAddress address) {
        for (int i = 0; i < this.mDisplays.size(); i++) {
            Display display = this.mDisplays.get(i);
            if (address.equals(display.getAddress())) {
                return display;
            }
        }
        return null;
    }

    public Display getAt(int index) {
        return this.mDisplays.get(index);
    }

    public int size() {
        return this.mDisplays.size();
    }

    public boolean isDualLayout(Layout layout) {
        if (equals(layout)) {
            return false;
        }
        for (int i = 0; i < this.mDisplays.size(); i++) {
            Display display = this.mDisplays.get(i);
            Display display2 = layout.getByAddress(display.getAddress());
            if (display2 == null) {
                return false;
            }
            if (display.getLogicalDisplayId() == 0) {
                if (display2.getLogicalDisplayId() != 0) {
                    return false;
                }
            } else if (display.isEnabled() || !display2.isEnabled()) {
                return false;
            }
        }
        return true;
    }

    /* loaded from: classes.dex */
    public static class Display {
        private final DisplayAddress mAddress;
        private final boolean mIsEnabled;
        private final int mLogicalDisplayId;

        Display(DisplayAddress address, int logicalDisplayId, boolean isEnabled) {
            this.mAddress = address;
            this.mLogicalDisplayId = logicalDisplayId;
            this.mIsEnabled = isEnabled;
        }

        public String toString() {
            return "{addr: " + this.mAddress + ", dispId: " + this.mLogicalDisplayId + "(" + (this.mIsEnabled ? "ON" : "OFF") + ")}";
        }

        public DisplayAddress getAddress() {
            return this.mAddress;
        }

        public int getLogicalDisplayId() {
            return this.mLogicalDisplayId;
        }

        public boolean isEnabled() {
            return this.mIsEnabled;
        }
    }
}
