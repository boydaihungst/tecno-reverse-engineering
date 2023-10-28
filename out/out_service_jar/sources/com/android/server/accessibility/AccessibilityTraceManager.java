package com.android.server.accessibility;

import android.accessibilityservice.AccessibilityTrace;
import android.os.Binder;
import android.os.ShellCommand;
import com.android.server.wm.WindowManagerInternal;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
/* loaded from: classes.dex */
public class AccessibilityTraceManager implements AccessibilityTrace {
    private static AccessibilityTraceManager sInstance = null;
    private final WindowManagerInternal.AccessibilityControllerInternal mA11yController;
    private final Object mA11yMSLock;
    private volatile long mEnabledLoggingFlags = 0;
    private final AccessibilityManagerService mService;

    /* JADX INFO: Access modifiers changed from: package-private */
    public static AccessibilityTraceManager getInstance(WindowManagerInternal.AccessibilityControllerInternal a11yController, AccessibilityManagerService service, Object lock) {
        if (sInstance == null) {
            sInstance = new AccessibilityTraceManager(a11yController, service, lock);
        }
        return sInstance;
    }

    private AccessibilityTraceManager(WindowManagerInternal.AccessibilityControllerInternal a11yController, AccessibilityManagerService service, Object lock) {
        this.mA11yController = a11yController;
        this.mService = service;
        this.mA11yMSLock = lock;
    }

    public boolean isA11yTracingEnabled() {
        return this.mEnabledLoggingFlags != 0;
    }

    public boolean isA11yTracingEnabledForTypes(long typeIdFlags) {
        return (this.mEnabledLoggingFlags & typeIdFlags) != 0;
    }

    public int getTraceStateForAccessibilityManagerClientState() {
        int state = 0;
        if (isA11yTracingEnabledForTypes(16L)) {
            state = 0 | 256;
        }
        if (isA11yTracingEnabledForTypes(32L)) {
            state |= 512;
        }
        if (isA11yTracingEnabledForTypes(262144L)) {
            state |= 1024;
        }
        if (isA11yTracingEnabledForTypes(16384L)) {
            return state | 2048;
        }
        return state;
    }

    public void startTrace(long loggingTypes) {
        if (loggingTypes == 0) {
            return;
        }
        long oldEnabled = this.mEnabledLoggingFlags;
        this.mEnabledLoggingFlags = loggingTypes;
        if (needToNotifyClients(oldEnabled)) {
            synchronized (this.mA11yMSLock) {
                AccessibilityManagerService accessibilityManagerService = this.mService;
                accessibilityManagerService.scheduleUpdateClientsIfNeededLocked(accessibilityManagerService.getCurrentUserState());
            }
        }
        this.mA11yController.startTrace(loggingTypes);
    }

    public void stopTrace() {
        boolean stop = isA11yTracingEnabled();
        long oldEnabled = this.mEnabledLoggingFlags;
        this.mEnabledLoggingFlags = 0L;
        if (needToNotifyClients(oldEnabled)) {
            synchronized (this.mA11yMSLock) {
                AccessibilityManagerService accessibilityManagerService = this.mService;
                accessibilityManagerService.scheduleUpdateClientsIfNeededLocked(accessibilityManagerService.getCurrentUserState());
            }
        }
        if (stop) {
            this.mA11yController.stopTrace();
        }
    }

    public void logTrace(String where, long loggingTypes) {
        logTrace(where, loggingTypes, "");
    }

    public void logTrace(String where, long loggingTypes, String callingParams) {
        if (isA11yTracingEnabledForTypes(loggingTypes)) {
            this.mA11yController.logTrace(where, loggingTypes, callingParams, "".getBytes(), Binder.getCallingUid(), Thread.currentThread().getStackTrace(), new HashSet(Arrays.asList("logTrace")));
        }
    }

    public void logTrace(long timestamp, String where, long loggingTypes, String callingParams, int processId, long threadId, int callingUid, StackTraceElement[] callStack, Set<String> ignoreElementList) {
        if (isA11yTracingEnabledForTypes(loggingTypes)) {
            this.mA11yController.logTrace(where, loggingTypes, callingParams, "".getBytes(), callingUid, callStack, timestamp, processId, threadId, ignoreElementList == null ? new HashSet() : ignoreElementList);
        }
    }

    private boolean needToNotifyClients(long otherTypesEnabled) {
        return (this.mEnabledLoggingFlags & 278576) != (278576 & otherTypesEnabled);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    public int onShellCommand(String cmd, ShellCommand shell) {
        boolean z;
        boolean z2;
        switch (cmd.hashCode()) {
            case 1340897306:
                if (cmd.equals("start-trace")) {
                    z = false;
                    break;
                }
                z = true;
                break;
            case 1857979322:
                if (cmd.equals("stop-trace")) {
                    z = true;
                    break;
                }
                z = true;
                break;
            default:
                z = true;
                break;
        }
        switch (z) {
            case false:
                String opt = shell.getNextOption();
                if (opt == null) {
                    startTrace(-1L);
                    return 0;
                }
                List<String> types = new ArrayList<>();
                while (opt != null) {
                    switch (opt.hashCode()) {
                        case 1511:
                            if (opt.equals("-t")) {
                                z2 = false;
                                break;
                            }
                        default:
                            z2 = true;
                            break;
                    }
                    switch (z2) {
                        case false:
                            String type = shell.getNextArg();
                            while (type != null) {
                                types.add(type);
                                type = shell.getNextArg();
                            }
                            opt = shell.getNextOption();
                        default:
                            shell.getErrPrintWriter().println("Error: option not recognized " + opt);
                            stopTrace();
                            return -1;
                    }
                }
                long enabledTypes = AccessibilityTrace.getLoggingFlagsFromNames(types);
                startTrace(enabledTypes);
                return 0;
            case true:
                stopTrace();
                return 0;
            default:
                return -1;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onHelp(PrintWriter pw) {
        pw.println("  start-trace [-t LOGGING_TYPE [LOGGING_TYPE...]]");
        pw.println("    Start the debug tracing. If no option is present, full trace will be");
        pw.println("    generated. Options are:");
        pw.println("      -t: Only generate tracing for the logging type(s) specified here.");
        pw.println("          LOGGING_TYPE can be any one of below:");
        pw.println("            IAccessibilityServiceConnection");
        pw.println("            IAccessibilityServiceClient");
        pw.println("            IAccessibilityManager");
        pw.println("            IAccessibilityManagerClient");
        pw.println("            IAccessibilityInteractionConnection");
        pw.println("            IAccessibilityInteractionConnectionCallback");
        pw.println("            IRemoteMagnificationAnimationCallback");
        pw.println("            IWindowMagnificationConnection");
        pw.println("            IWindowMagnificationConnectionCallback");
        pw.println("            WindowManagerInternal");
        pw.println("            WindowsForAccessibilityCallback");
        pw.println("            MagnificationCallbacks");
        pw.println("            InputFilter");
        pw.println("            Gesture");
        pw.println("            AccessibilityService");
        pw.println("            PMBroadcastReceiver");
        pw.println("            UserBroadcastReceiver");
        pw.println("            FingerprintGesture");
        pw.println("  stop-trace");
        pw.println("    Stop the debug tracing.");
    }
}
