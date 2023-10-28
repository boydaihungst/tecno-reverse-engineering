package com.android.server.wm;

import android.util.ArrayMap;
import android.util.Slog;
import java.io.PrintWriter;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class UnknownAppVisibilityController {
    private static final String TAG = "WindowManager";
    private static final int UNKNOWN_STATE_WAITING_RELAYOUT = 2;
    private static final int UNKNOWN_STATE_WAITING_RESUME = 1;
    private static final int UNKNOWN_STATE_WAITING_RESUME_FINISH = 4;
    private static final int UNKNOWN_STATE_WAITING_VISIBILITY_UPDATE = 3;
    private final DisplayContent mDisplayContent;
    private final WindowManagerService mService;
    private final ArrayMap<ActivityRecord, Integer> mUnknownApps = new ArrayMap<>();

    /* JADX INFO: Access modifiers changed from: package-private */
    public UnknownAppVisibilityController(WindowManagerService service, DisplayContent displayContent) {
        this.mService = service;
        this.mDisplayContent = displayContent;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean allResolved() {
        return this.mUnknownApps.isEmpty();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void clear() {
        this.mUnknownApps.clear();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public String getDebugMessage() {
        StringBuilder builder = new StringBuilder();
        for (int i = this.mUnknownApps.size() - 1; i >= 0; i--) {
            builder.append("app=").append(this.mUnknownApps.keyAt(i)).append(" state=").append(this.mUnknownApps.valueAt(i));
            if (i != 0) {
                builder.append(' ');
            }
        }
        return builder.toString();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void appRemovedOrHidden(ActivityRecord activity) {
        if (WindowManagerDebugConfig.DEBUG_UNKNOWN_APP_VISIBILITY) {
            Slog.d("WindowManager", "App removed or hidden activity=" + activity);
        }
        this.mUnknownApps.remove(activity);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void notifyLaunched(ActivityRecord activity) {
        if (WindowManagerDebugConfig.DEBUG_UNKNOWN_APP_VISIBILITY) {
            Slog.d("WindowManager", "App launched activity=" + activity);
        }
        if (!activity.mLaunchTaskBehind) {
            this.mUnknownApps.put(activity, 1);
        } else {
            this.mUnknownApps.put(activity, 2);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void notifyAppResumedFinished(ActivityRecord activity) {
        if (this.mUnknownApps.containsKey(activity) && this.mUnknownApps.get(activity).intValue() == 1) {
            if (WindowManagerDebugConfig.DEBUG_UNKNOWN_APP_VISIBILITY) {
                Slog.d("WindowManager", "App resume finished activity=" + activity);
            }
            this.mUnknownApps.put(activity, 2);
        }
        if (this.mUnknownApps.containsKey(activity) && this.mUnknownApps.get(activity).intValue() == 4) {
            Slog.d("WindowManager", "resume finish slower than relayout window, keep going....");
            this.mUnknownApps.put(activity, 3);
            this.mDisplayContent.notifyKeyguardFlagsChanged();
            notifyVisibilitiesUpdated();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void notifyRelayouted(ActivityRecord activity) {
        if (!this.mUnknownApps.containsKey(activity)) {
            return;
        }
        if (WindowManagerDebugConfig.DEBUG_UNKNOWN_APP_VISIBILITY) {
            Slog.d("WindowManager", "App relayouted appWindow=" + activity);
        }
        int state = this.mUnknownApps.get(activity).intValue();
        if (state == 1) {
            Slog.d("WindowManager", "notifyRelayouted set to UNKNOWN_STATE_WAITING_RESUME_FINISH ");
            this.mUnknownApps.put(activity, 4);
        }
        if (state == 2 || activity.mStartingWindow != null) {
            this.mUnknownApps.put(activity, 3);
            this.mDisplayContent.notifyKeyguardFlagsChanged();
            notifyVisibilitiesUpdated();
        }
    }

    private void notifyVisibilitiesUpdated() {
        if (WindowManagerDebugConfig.DEBUG_UNKNOWN_APP_VISIBILITY) {
            Slog.d("WindowManager", "Visibility updated DONE");
        }
        boolean changed = false;
        for (int i = this.mUnknownApps.size() - 1; i >= 0; i--) {
            if (this.mUnknownApps.valueAt(i).intValue() == 3) {
                this.mUnknownApps.removeAt(i);
                changed = true;
            }
        }
        if (changed) {
            this.mService.mWindowPlacerLocked.performSurfacePlacement();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dump(PrintWriter pw, String prefix) {
        if (this.mUnknownApps.isEmpty()) {
            return;
        }
        pw.println(prefix + "Unknown visibilities:");
        for (int i = this.mUnknownApps.size() - 1; i >= 0; i--) {
            pw.println(prefix + "  app=" + this.mUnknownApps.keyAt(i) + " state=" + this.mUnknownApps.valueAt(i));
        }
    }
}
