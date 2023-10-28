package com.android.server.ambientcontext;

import android.app.ambientcontext.AmbientContextEventRequest;
import android.content.ComponentName;
import android.os.Binder;
import android.os.Bundle;
import android.os.RemoteCallback;
import android.os.ShellCommand;
import android.service.ambientcontext.AmbientContextDetectionResult;
import android.service.ambientcontext.AmbientContextDetectionServiceStatus;
import com.android.server.ambientcontext.AmbientContextShellCommand;
import java.io.PrintWriter;
/* loaded from: classes.dex */
final class AmbientContextShellCommand extends ShellCommand {
    private static final AmbientContextEventRequest REQUEST = new AmbientContextEventRequest.Builder().addEventType(1).addEventType(2).build();
    static final TestableCallbackInternal sTestableCallbackInternal = new TestableCallbackInternal();
    private final AmbientContextManagerService mService;

    /* JADX INFO: Access modifiers changed from: package-private */
    public AmbientContextShellCommand(AmbientContextManagerService service) {
        this.mService = service;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class TestableCallbackInternal {
        private AmbientContextDetectionResult mLastResult;
        private AmbientContextDetectionServiceStatus mLastStatus;

        TestableCallbackInternal() {
        }

        public AmbientContextDetectionResult getLastResult() {
            return this.mLastResult;
        }

        public AmbientContextDetectionServiceStatus getLastStatus() {
            return this.mLastStatus;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public RemoteCallback createRemoteDetectionResultCallback() {
            return new RemoteCallback(new RemoteCallback.OnResultListener() { // from class: com.android.server.ambientcontext.AmbientContextShellCommand$TestableCallbackInternal$$ExternalSyntheticLambda1
                public final void onResult(Bundle bundle) {
                    AmbientContextShellCommand.TestableCallbackInternal.this.m1535x40f6561a(bundle);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$createRemoteDetectionResultCallback$0$com-android-server-ambientcontext-AmbientContextShellCommand$TestableCallbackInternal  reason: not valid java name */
        public /* synthetic */ void m1535x40f6561a(Bundle result) {
            AmbientContextDetectionResult detectionResult = (AmbientContextDetectionResult) result.get("android.app.ambientcontext.AmbientContextDetectionResultBundleKey");
            long token = Binder.clearCallingIdentity();
            try {
                this.mLastResult = detectionResult;
                System.out.println("Detection result available: " + detectionResult);
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public RemoteCallback createRemoteStatusCallback() {
            return new RemoteCallback(new RemoteCallback.OnResultListener() { // from class: com.android.server.ambientcontext.AmbientContextShellCommand$TestableCallbackInternal$$ExternalSyntheticLambda0
                public final void onResult(Bundle bundle) {
                    AmbientContextShellCommand.TestableCallbackInternal.this.m1536xc2039f69(bundle);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$createRemoteStatusCallback$1$com-android-server-ambientcontext-AmbientContextShellCommand$TestableCallbackInternal  reason: not valid java name */
        public /* synthetic */ void m1536xc2039f69(Bundle result) {
            AmbientContextDetectionServiceStatus status = (AmbientContextDetectionServiceStatus) result.get("android.app.ambientcontext.AmbientContextServiceStatusBundleKey");
            long token = Binder.clearCallingIdentity();
            try {
                this.mLastStatus = status;
                System.out.println("Status available: " + status);
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }
    }

    public int onCommand(String cmd) {
        if (cmd == null) {
            return handleDefaultCommands(cmd);
        }
        char c = 65535;
        switch (cmd.hashCode()) {
            case -2084150080:
                if (cmd.equals("get-bound-package")) {
                    c = 5;
                    break;
                }
                break;
            case -2048517510:
                if (cmd.equals("stop-detection")) {
                    c = 1;
                    break;
                }
                break;
            case -476021038:
                if (cmd.equals("get-last-package-name")) {
                    c = 3;
                    break;
                }
                break;
            case 1104883342:
                if (cmd.equals("set-temporary-service")) {
                    c = 6;
                    break;
                }
                break;
            case 1519475119:
                if (cmd.equals("query-service-status")) {
                    c = 4;
                    break;
                }
                break;
            case 2018305992:
                if (cmd.equals("get-last-status-code")) {
                    c = 2;
                    break;
                }
                break;
            case 2084757210:
                if (cmd.equals("start-detection")) {
                    c = 0;
                    break;
                }
                break;
        }
        switch (c) {
            case 0:
                return runStartDetection();
            case 1:
                return runStopDetection();
            case 2:
                return getLastStatusCode();
            case 3:
                return getLastPackageName();
            case 4:
                return runQueryServiceStatus();
            case 5:
                return getBoundPackageName();
            case 6:
                return setTemporaryService();
            default:
                return handleDefaultCommands(cmd);
        }
    }

    private int runStartDetection() {
        int userId = Integer.parseInt(getNextArgRequired());
        String packageName = getNextArgRequired();
        AmbientContextManagerService ambientContextManagerService = this.mService;
        AmbientContextEventRequest ambientContextEventRequest = REQUEST;
        TestableCallbackInternal testableCallbackInternal = sTestableCallbackInternal;
        ambientContextManagerService.startDetection(userId, ambientContextEventRequest, packageName, testableCallbackInternal.createRemoteDetectionResultCallback(), testableCallbackInternal.createRemoteStatusCallback());
        return 0;
    }

    private int runStopDetection() {
        int userId = Integer.parseInt(getNextArgRequired());
        String packageName = getNextArgRequired();
        this.mService.stopAmbientContextEvent(userId, packageName);
        return 0;
    }

    private int runQueryServiceStatus() {
        int userId = Integer.parseInt(getNextArgRequired());
        String packageName = getNextArgRequired();
        int[] types = {1, 2};
        this.mService.queryServiceStatus(userId, packageName, types, sTestableCallbackInternal.createRemoteStatusCallback());
        return 0;
    }

    private int getLastStatusCode() {
        AmbientContextDetectionServiceStatus lastResponse = sTestableCallbackInternal.getLastStatus();
        if (lastResponse == null) {
            return -1;
        }
        return lastResponse.getStatusCode();
    }

    private int getLastPackageName() {
        AmbientContextDetectionServiceStatus lastResponse = sTestableCallbackInternal.getLastStatus();
        System.out.println(lastResponse == null ? "" : lastResponse.getPackageName());
        return 0;
    }

    public void onHelp() {
        PrintWriter pw = getOutPrintWriter();
        pw.println("AmbientContextEvent commands: ");
        pw.println("  help");
        pw.println("    Print this help text.");
        pw.println();
        pw.println("  start-detection USER_ID PACKAGE_NAME: Starts AmbientContextEvent detection.");
        pw.println("  stop-detection USER_ID: Stops AmbientContextEvent detection.");
        pw.println("  get-last-status-code: Prints the latest request status code.");
        pw.println("  get-last-package-name: Prints the latest request package name.");
        pw.println("  query-event-status USER_ID PACKAGE_NAME: Prints the event status code.");
        pw.println("  get-bound-package USER_ID:     Print the bound package that implements the service.");
        pw.println("  set-temporary-service USER_ID [COMPONENT_NAME DURATION]");
        pw.println("    Temporarily (for DURATION ms) changes the service implementation.");
        pw.println("    To reset, call with just the USER_ID argument.");
    }

    private int getBoundPackageName() {
        PrintWriter out = getOutPrintWriter();
        int userId = Integer.parseInt(getNextArgRequired());
        ComponentName componentName = this.mService.getComponentName(userId);
        out.println(componentName == null ? "" : componentName.getPackageName());
        return 0;
    }

    private int setTemporaryService() {
        PrintWriter out = getOutPrintWriter();
        int userId = Integer.parseInt(getNextArgRequired());
        String serviceName = getNextArg();
        if (serviceName == null) {
            this.mService.resetTemporaryService(userId);
            out.println("AmbientContextDetectionService temporary reset. ");
            return 0;
        }
        int duration = Integer.parseInt(getNextArgRequired());
        this.mService.setTemporaryService(userId, serviceName, duration);
        out.println("AmbientContextDetectionService temporarily set to " + serviceName + " for " + duration + "ms");
        return 0;
    }
}
