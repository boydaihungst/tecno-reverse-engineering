package com.android.server.rotationresolver;

import android.content.ComponentName;
import android.os.ShellCommand;
import android.rotationresolver.RotationResolverInternal;
import android.service.rotationresolver.RotationResolutionRequest;
import java.io.PrintWriter;
/* loaded from: classes2.dex */
final class RotationResolverShellCommand extends ShellCommand {
    private static final int INITIAL_RESULT_CODE = -1;
    static final TestableRotationCallbackInternal sTestableRotationCallbackInternal = new TestableRotationCallbackInternal();
    private final RotationResolverManagerService mService;

    /* JADX INFO: Access modifiers changed from: package-private */
    public RotationResolverShellCommand(RotationResolverManagerService service) {
        this.mService = service;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public static class TestableRotationCallbackInternal implements RotationResolverInternal.RotationResolverCallbackInternal {
        private int mLastCallbackResultCode = -1;

        TestableRotationCallbackInternal() {
        }

        public void onSuccess(int result) {
            this.mLastCallbackResultCode = result;
        }

        public void onFailure(int error) {
            this.mLastCallbackResultCode = error;
        }

        public void reset() {
            this.mLastCallbackResultCode = -1;
        }

        public int getLastCallbackCode() {
            return this.mLastCallbackResultCode;
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
                    c = 2;
                    break;
                }
                break;
            case 384662079:
                if (cmd.equals("resolve-rotation")) {
                    c = 0;
                    break;
                }
                break;
            case 1104883342:
                if (cmd.equals("set-temporary-service")) {
                    c = 3;
                    break;
                }
                break;
            case 1820466124:
                if (cmd.equals("get-last-resolution")) {
                    c = 1;
                    break;
                }
                break;
        }
        switch (c) {
            case 0:
                return runResolveRotation();
            case 1:
                return getLastResolution();
            case 2:
                return getBoundPackageName();
            case 3:
                return setTemporaryService();
            default:
                return handleDefaultCommands(cmd);
        }
    }

    private int getBoundPackageName() {
        PrintWriter out = getOutPrintWriter();
        int userId = Integer.parseInt(getNextArgRequired());
        ComponentName componentName = this.mService.getComponentNameShellCommand(userId);
        out.println(componentName == null ? "" : componentName.getPackageName());
        return 0;
    }

    private int setTemporaryService() {
        PrintWriter out = getOutPrintWriter();
        int userId = Integer.parseInt(getNextArgRequired());
        String serviceName = getNextArg();
        if (serviceName == null) {
            this.mService.resetTemporaryService(userId);
            out.println("RotationResolverService temporary reset. ");
            return 0;
        }
        int duration = Integer.parseInt(getNextArgRequired());
        this.mService.setTemporaryService(userId, serviceName, duration);
        out.println("RotationResolverService temporarily set to " + serviceName + " for " + duration + "ms");
        return 0;
    }

    private int runResolveRotation() {
        int userId = Integer.parseInt(getNextArgRequired());
        RotationResolutionRequest request = new RotationResolutionRequest("", 0, 0, true, 2000L);
        this.mService.resolveRotationShellCommand(userId, sTestableRotationCallbackInternal, request);
        return 0;
    }

    private int getLastResolution() {
        PrintWriter out = getOutPrintWriter();
        out.println(sTestableRotationCallbackInternal.getLastCallbackCode());
        return 0;
    }

    public void onHelp() {
        PrintWriter pw = getOutPrintWriter();
        pw.println("Rotation Resolver commands: ");
        pw.println("  help");
        pw.println("    Print this help text.");
        pw.println();
        pw.println("  resolve-rotation USER_ID: request a rotation resolution.");
        pw.println("  get-last-resolution: show the last rotation resolution result.");
        pw.println("  get-bound-package USER_ID:");
        pw.println("    Print the bound package that implements the service.");
        pw.println("  set-temporary-service USER_ID [COMPONENT_NAME DURATION]");
        pw.println("    Temporarily (for DURATION ms) changes the service implementation.");
        pw.println("    To reset, call with just the USER_ID argument.");
    }
}
