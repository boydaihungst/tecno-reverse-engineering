package com.android.server.pm.verify.domain;

import android.app.ActivityManager;
import android.content.pm.PackageManager;
import android.content.pm.verify.domain.DomainVerificationUserState;
import android.os.Binder;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.IndentingPrintWriter;
import com.android.modules.utils.BasicShellCommandHandler;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
/* loaded from: classes2.dex */
public class DomainVerificationShell {
    private final Callback mCallback;

    /* loaded from: classes2.dex */
    public interface Callback {
        void clearDomainVerificationState(List<String> list);

        void clearUserStates(List<String> list, int i);

        DomainVerificationUserState getDomainVerificationUserState(String str, int i) throws PackageManager.NameNotFoundException;

        void printOwnersForDomains(IndentingPrintWriter indentingPrintWriter, List<String> list, Integer num);

        void printOwnersForPackage(IndentingPrintWriter indentingPrintWriter, String str, Integer num) throws PackageManager.NameNotFoundException;

        void printState(IndentingPrintWriter indentingPrintWriter, String str, Integer num) throws PackageManager.NameNotFoundException;

        void setDomainVerificationLinkHandlingAllowedInternal(String str, boolean z, int i) throws PackageManager.NameNotFoundException;

        void setDomainVerificationStatusInternal(String str, int i, ArraySet<String> arraySet) throws PackageManager.NameNotFoundException;

        void setDomainVerificationUserSelectionInternal(int i, String str, boolean z, ArraySet<String> arraySet) throws PackageManager.NameNotFoundException;

        void verifyPackages(List<String> list, boolean z);
    }

    public DomainVerificationShell(Callback callback) {
        this.mCallback = callback;
    }

    public void printHelp(PrintWriter pw) {
        pw.println("  get-app-links [--user <USER_ID>] [<PACKAGE>]");
        pw.println("    Prints the domain verification state for the given package, or for all");
        pw.println("    packages if none is specified. State codes are defined as follows:");
        pw.println("        - none: nothing has been recorded for this domain");
        pw.println("        - verified: the domain has been successfully verified");
        pw.println("        - approved: force approved, usually through shell");
        pw.println("        - denied: force denied, usually through shell");
        pw.println("        - migrated: preserved verification from a legacy response");
        pw.println("        - restored: preserved verification from a user data restore");
        pw.println("        - legacy_failure: rejected by a legacy verifier, unknown reason");
        pw.println("        - system_configured: automatically approved by the device config");
        pw.println("        - >= 1024: Custom error code which is specific to the device verifier");
        pw.println("      --user <USER_ID>: include user selections (includes all domains, not");
        pw.println("        just autoVerify ones)");
        pw.println("  reset-app-links [--user <USER_ID>] [<PACKAGE>]");
        pw.println("    Resets domain verification state for the given package, or for all");
        pw.println("    packages if none is specified.");
        pw.println("      --user <USER_ID>: clear user selection state instead; note this means");
        pw.println("        domain verification state will NOT be cleared");
        pw.println("      <PACKAGE>: the package to reset, or \"all\" to reset all packages");
        pw.println("  verify-app-links [--re-verify] [<PACKAGE>]");
        pw.println("    Broadcasts a verification request for the given package, or for all");
        pw.println("    packages if none is specified. Only sends if the package has previously");
        pw.println("    not recorded a response.");
        pw.println("      --re-verify: send even if the package has recorded a response");
        pw.println("  set-app-links [--package <PACKAGE>] <STATE> <DOMAINS>...");
        pw.println("    Manually set the state of a domain for a package. The domain must be");
        pw.println("    declared by the package as autoVerify for this to work. This command");
        pw.println("    will not report a failure for domains that could not be applied.");
        pw.println("      --package <PACKAGE>: the package to set, or \"all\" to set all packages");
        pw.println("      <STATE>: the code to set the domains to, valid values are:");
        pw.println("        STATE_NO_RESPONSE (0): reset as if no response was ever recorded.");
        pw.println("        STATE_SUCCESS (1): treat domain as successfully verified by domain.");
        pw.println("          verification agent. Note that the domain verification agent can");
        pw.println("          override this.");
        pw.println("        STATE_APPROVED (2): treat domain as always approved, preventing the");
        pw.println("           domain verification agent from changing it.");
        pw.println("        STATE_DENIED (3): treat domain as always denied, preveting the domain");
        pw.println("          verification agent from changing it.");
        pw.println("      <DOMAINS>: space separated list of domains to change, or \"all\" to");
        pw.println("        change every domain.");
        pw.println("  set-app-links-user-selection --user <USER_ID> [--package <PACKAGE>]");
        pw.println("      <ENABLED> <DOMAINS>...");
        pw.println("    Manually set the state of a host user selection for a package. The domain");
        pw.println("    must be declared by the package for this to work. This command will not");
        pw.println("    report a failure for domains that could not be applied.");
        pw.println("      --user <USER_ID>: the user to change selections for");
        pw.println("      --package <PACKAGE>: the package to set");
        pw.println("      <ENABLED>: whether or not to approve the domain");
        pw.println("      <DOMAINS>: space separated list of domains to change, or \"all\" to");
        pw.println("        change every domain.");
        pw.println("  set-app-links-allowed --user <USER_ID> [--package <PACKAGE>] <ALLOWED>");
        pw.println("      <ENABLED> <DOMAINS>...");
        pw.println("    Toggle the auto verified link handling setting for a package.");
        pw.println("      --user <USER_ID>: the user to change selections for");
        pw.println("      --package <PACKAGE>: the package to set, or \"all\" to set all packages");
        pw.println("        packages will be reset if no one package is specified.");
        pw.println("      <ALLOWED>: true to allow the package to open auto verified links, false");
        pw.println("        to disable");
        pw.println("  get-app-link-owners [--user <USER_ID>] [--package <PACKAGE>] [<DOMAINS>]");
        pw.println("    Print the owners for a specific domain for a given user in low to high");
        pw.println("    priority order.");
        pw.println("      --user <USER_ID>: the user to query for");
        pw.println("      --package <PACKAGE>: optionally also print for all web domains declared");
        pw.println("        by a package, or \"all\" to print all packages");
        pw.println("      --<DOMAINS>: space separated list of domains to query for");
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    public Boolean runCommand(BasicShellCommandHandler commandHandler, String command) {
        char c;
        switch (command.hashCode()) {
            case -2140094634:
                if (command.equals("get-app-links")) {
                    c = 0;
                    break;
                }
                c = 65535;
                break;
            case -2092945963:
                if (command.equals("set-app-links-user-selection")) {
                    c = 4;
                    break;
                }
                c = 65535;
                break;
            case -1850904515:
                if (command.equals("set-app-links-allowed")) {
                    c = 5;
                    break;
                }
                c = 65535;
                break;
            case -1365963422:
                if (command.equals("set-app-links")) {
                    c = 3;
                    break;
                }
                c = 65535;
                break;
            case -825562609:
                if (command.equals("reset-app-links")) {
                    c = 1;
                    break;
                }
                c = 65535;
                break;
            case 1161008944:
                if (command.equals("get-app-link-owners")) {
                    c = 6;
                    break;
                }
                c = 65535;
                break;
            case 1328605369:
                if (command.equals("verify-app-links")) {
                    c = 2;
                    break;
                }
                c = 65535;
                break;
            default:
                c = 65535;
                break;
        }
        switch (c) {
            case 0:
                return Boolean.valueOf(runGetAppLinks(commandHandler));
            case 1:
                return Boolean.valueOf(runResetAppLinks(commandHandler));
            case 2:
                return Boolean.valueOf(runVerifyAppLinks(commandHandler));
            case 3:
                return Boolean.valueOf(runSetAppLinks(commandHandler));
            case 4:
                return Boolean.valueOf(runSetAppLinksUserState(commandHandler));
            case 5:
                return Boolean.valueOf(runSetAppLinksAllowed(commandHandler));
            case 6:
                return Boolean.valueOf(runGetAppLinkOwners(commandHandler));
            default:
                return null;
        }
    }

    private boolean runSetAppLinks(BasicShellCommandHandler commandHandler) {
        int stateInt;
        String packageName = null;
        while (true) {
            String option = commandHandler.getNextOption();
            if (option != null) {
                if (option.equals("--package")) {
                    packageName = commandHandler.getNextArgRequired();
                } else {
                    commandHandler.getErrPrintWriter().println("Error: unknown option: " + option);
                    return false;
                }
            } else if (TextUtils.isEmpty(packageName)) {
                commandHandler.getErrPrintWriter().println("Error: no package specified");
                return false;
            } else {
                if (packageName.equalsIgnoreCase("all")) {
                    packageName = null;
                }
                String state = commandHandler.getNextArgRequired();
                char c = 65535;
                switch (state.hashCode()) {
                    case -1618466107:
                        if (state.equals("STATE_APPROVED")) {
                            c = 4;
                            break;
                        }
                        break;
                    case 48:
                        if (state.equals("0")) {
                            c = 1;
                            break;
                        }
                        break;
                    case 49:
                        if (state.equals("1")) {
                            c = 3;
                            break;
                        }
                        break;
                    case 50:
                        if (state.equals("2")) {
                            c = 5;
                            break;
                        }
                        break;
                    case 51:
                        if (state.equals("3")) {
                            c = 7;
                            break;
                        }
                        break;
                    case 259145237:
                        if (state.equals("STATE_SUCCESS")) {
                            c = 2;
                            break;
                        }
                        break;
                    case 482582065:
                        if (state.equals("STATE_NO_RESPONSE")) {
                            c = 0;
                            break;
                        }
                        break;
                    case 534310697:
                        if (state.equals("STATE_DENIED")) {
                            c = 6;
                            break;
                        }
                        break;
                }
                switch (c) {
                    case 0:
                    case 1:
                        stateInt = 0;
                        break;
                    case 2:
                    case 3:
                        stateInt = 1;
                        break;
                    case 4:
                    case 5:
                        stateInt = 2;
                        break;
                    case 6:
                    case 7:
                        stateInt = 3;
                        break;
                    default:
                        commandHandler.getErrPrintWriter().println("Invalid state option: " + state);
                        return false;
                }
                ArraySet<String> domains = new ArraySet<>(getRemainingArgs(commandHandler));
                if (domains.isEmpty()) {
                    commandHandler.getErrPrintWriter().println("No domains specified");
                    return false;
                }
                if (domains.size() == 1 && domains.contains("all")) {
                    domains = null;
                }
                try {
                    this.mCallback.setDomainVerificationStatusInternal(packageName, stateInt, domains);
                    return true;
                } catch (PackageManager.NameNotFoundException e) {
                    commandHandler.getErrPrintWriter().println("Package not found: " + packageName);
                    return false;
                }
            }
        }
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Code restructure failed: missing block: B:12:0x0024, code lost:
        if (r2.equals("--package") != false) goto L9;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private boolean runSetAppLinksUserState(BasicShellCommandHandler commandHandler) {
        Integer userId = null;
        String packageName = null;
        while (true) {
            String option = commandHandler.getNextOption();
            boolean z = true;
            if (option != null) {
                switch (option.hashCode()) {
                    case 578919078:
                        break;
                    case 1333469547:
                        if (option.equals("--user")) {
                            z = false;
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
                        userId = Integer.valueOf(UserHandle.parseUserArg(commandHandler.getNextArgRequired()));
                        break;
                    case true:
                        packageName = commandHandler.getNextArgRequired();
                        break;
                    default:
                        commandHandler.getErrPrintWriter().println("Error: unknown option: " + option);
                        return false;
                }
            } else if (TextUtils.isEmpty(packageName)) {
                commandHandler.getErrPrintWriter().println("Error: no package specified");
                return false;
            } else if (userId == null) {
                commandHandler.getErrPrintWriter().println("Error: User ID not specified");
                return false;
            } else {
                Integer userId2 = Integer.valueOf(translateUserId(userId.intValue(), "runSetAppLinksUserState"));
                String enabledArg = commandHandler.getNextArg();
                if (TextUtils.isEmpty(enabledArg)) {
                    commandHandler.getErrPrintWriter().println("Error: enabled param not specified");
                    return false;
                }
                try {
                    boolean enabled = parseEnabled(enabledArg);
                    ArraySet<String> domains = new ArraySet<>(getRemainingArgs(commandHandler));
                    if (domains.isEmpty()) {
                        commandHandler.getErrPrintWriter().println("No domains specified");
                        return false;
                    }
                    if (domains.size() == 1 && domains.contains("all")) {
                        domains = null;
                    }
                    try {
                        this.mCallback.setDomainVerificationUserSelectionInternal(userId2.intValue(), packageName, enabled, domains);
                        return true;
                    } catch (PackageManager.NameNotFoundException e) {
                        commandHandler.getErrPrintWriter().println("Package not found: " + packageName);
                        return false;
                    }
                } catch (IllegalArgumentException e2) {
                    commandHandler.getErrPrintWriter().println("Error: invalid enabled param: " + e2.getMessage());
                    return false;
                }
            }
        }
    }

    private boolean runGetAppLinks(BasicShellCommandHandler commandHandler) {
        Integer userId = null;
        while (true) {
            String option = commandHandler.getNextOption();
            if (option != null) {
                if (option.equals("--user")) {
                    userId = Integer.valueOf(UserHandle.parseUserArg(commandHandler.getNextArgRequired()));
                } else {
                    commandHandler.getErrPrintWriter().println("Error: unknown option: " + option);
                    return false;
                }
            } else {
                Integer userId2 = userId == null ? null : Integer.valueOf(translateUserId(userId.intValue(), "runGetAppLinks"));
                String packageName = commandHandler.getNextArg();
                IndentingPrintWriter writer = new IndentingPrintWriter(commandHandler.getOutPrintWriter(), "  ", 120);
                try {
                    writer.increaseIndent();
                    try {
                        this.mCallback.printState(writer, packageName, userId2);
                        writer.decreaseIndent();
                        writer.close();
                        return true;
                    } catch (PackageManager.NameNotFoundException e) {
                        commandHandler.getErrPrintWriter().println("Error: package " + packageName + " unavailable");
                        writer.close();
                        return false;
                    }
                } catch (Throwable th) {
                    try {
                        writer.close();
                    } catch (Throwable th2) {
                        th.addSuppressed(th2);
                    }
                    throw th;
                }
            }
        }
    }

    private boolean runResetAppLinks(BasicShellCommandHandler commandHandler) {
        List<String> packageNames;
        Integer userId = null;
        while (true) {
            String option = commandHandler.getNextOption();
            if (option != null) {
                if (option.equals("--user")) {
                    userId = Integer.valueOf(UserHandle.parseUserArg(commandHandler.getNextArgRequired()));
                } else {
                    commandHandler.getErrPrintWriter().println("Error: unknown option: " + option);
                    return false;
                }
            } else {
                Integer userId2 = userId == null ? null : Integer.valueOf(translateUserId(userId.intValue(), "runResetAppLinks"));
                String pkgNameArg = commandHandler.peekNextArg();
                if (TextUtils.isEmpty(pkgNameArg)) {
                    commandHandler.getErrPrintWriter().println("Error: no package specified");
                    return false;
                }
                if (pkgNameArg.equalsIgnoreCase("all")) {
                    packageNames = null;
                } else {
                    packageNames = Arrays.asList(commandHandler.peekRemainingArgs());
                }
                if (userId2 != null) {
                    this.mCallback.clearUserStates(packageNames, userId2.intValue());
                    return true;
                }
                this.mCallback.clearDomainVerificationState(packageNames);
                return true;
            }
        }
    }

    private boolean runVerifyAppLinks(BasicShellCommandHandler commandHandler) {
        boolean reVerify = false;
        while (true) {
            String option = commandHandler.getNextOption();
            if (option != null) {
                if (option.equals("--re-verify")) {
                    reVerify = true;
                } else {
                    commandHandler.getErrPrintWriter().println("Error: unknown option: " + option);
                    return false;
                }
            } else {
                List<String> packageNames = null;
                String pkgNameArg = commandHandler.getNextArg();
                if (!TextUtils.isEmpty(pkgNameArg)) {
                    packageNames = Collections.singletonList(pkgNameArg);
                }
                this.mCallback.verifyPackages(packageNames, reVerify);
                return true;
            }
        }
    }

    private boolean runSetAppLinksAllowed(BasicShellCommandHandler commandHandler) {
        String packageName = null;
        Integer userId = null;
        while (true) {
            String option = commandHandler.getNextOption();
            if (option != null) {
                if (option.equals("--package")) {
                    packageName = commandHandler.getNextArg();
                } else if (option.equals("--user")) {
                    userId = Integer.valueOf(UserHandle.parseUserArg(commandHandler.getNextArgRequired()));
                } else {
                    commandHandler.getErrPrintWriter().println("Error: unexpected option: " + option);
                    return false;
                }
            } else if (TextUtils.isEmpty(packageName)) {
                commandHandler.getErrPrintWriter().println("Error: no package specified");
                return false;
            } else {
                if (packageName.equalsIgnoreCase("all")) {
                    packageName = null;
                }
                if (userId == null) {
                    commandHandler.getErrPrintWriter().println("Error: user ID not specified");
                    return false;
                }
                String allowedArg = commandHandler.getNextArg();
                if (TextUtils.isEmpty(allowedArg)) {
                    commandHandler.getErrPrintWriter().println("Error: allowed setting not specified");
                    return false;
                }
                try {
                    boolean allowed = parseEnabled(allowedArg);
                    try {
                        this.mCallback.setDomainVerificationLinkHandlingAllowedInternal(packageName, allowed, Integer.valueOf(translateUserId(userId.intValue(), "runSetAppLinksAllowed")).intValue());
                        return true;
                    } catch (PackageManager.NameNotFoundException e) {
                        commandHandler.getErrPrintWriter().println("Package not found: " + packageName);
                        return false;
                    }
                } catch (IllegalArgumentException e2) {
                    commandHandler.getErrPrintWriter().println("Error: invalid allowed setting: " + e2.getMessage());
                    return false;
                }
            }
        }
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Code restructure failed: missing block: B:12:0x0024, code lost:
        if (r2.equals("--package") != false) goto L9;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private boolean runGetAppLinkOwners(BasicShellCommandHandler commandHandler) {
        String packageName = null;
        Integer userId = null;
        while (true) {
            String option = commandHandler.getNextOption();
            boolean z = true;
            if (option != null) {
                switch (option.hashCode()) {
                    case 578919078:
                        break;
                    case 1333469547:
                        if (option.equals("--user")) {
                            z = false;
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
                        userId = Integer.valueOf(UserHandle.parseUserArg(commandHandler.getNextArgRequired()));
                        break;
                    case true:
                        packageName = commandHandler.getNextArgRequired();
                        if (!TextUtils.isEmpty(packageName)) {
                            break;
                        } else {
                            commandHandler.getErrPrintWriter().println("Error: no package specified");
                            return false;
                        }
                    default:
                        commandHandler.getErrPrintWriter().println("Error: unexpected option: " + option);
                        return false;
                }
            } else {
                ArrayList<String> domains = getRemainingArgs(commandHandler);
                if (domains.isEmpty() && TextUtils.isEmpty(packageName)) {
                    commandHandler.getErrPrintWriter().println("Error: no package name or domain specified");
                    return false;
                }
                if (userId != null) {
                    userId = Integer.valueOf(translateUserId(userId.intValue(), "runSetAppLinksAllowed"));
                }
                IndentingPrintWriter writer = new IndentingPrintWriter(commandHandler.getOutPrintWriter(), "  ", 120);
                try {
                    writer.increaseIndent();
                    if (packageName != null) {
                        if (packageName.equals("all")) {
                            packageName = null;
                        }
                        try {
                            this.mCallback.printOwnersForPackage(writer, packageName, userId);
                        } catch (PackageManager.NameNotFoundException e) {
                            commandHandler.getErrPrintWriter().println("Error: package not found: " + packageName);
                            writer.close();
                            return false;
                        }
                    }
                    if (!domains.isEmpty()) {
                        this.mCallback.printOwnersForDomains(writer, domains, userId);
                    }
                    writer.decreaseIndent();
                    writer.close();
                    return true;
                } catch (Throwable th) {
                    try {
                        writer.close();
                    } catch (Throwable th2) {
                        th.addSuppressed(th2);
                    }
                    throw th;
                }
            }
        }
    }

    private ArrayList<String> getRemainingArgs(BasicShellCommandHandler commandHandler) {
        ArrayList<String> args = new ArrayList<>();
        while (true) {
            String arg = commandHandler.getNextArg();
            if (arg != null) {
                args.add(arg);
            } else {
                return args;
            }
        }
    }

    private int translateUserId(int userId, String logContext) {
        return ActivityManager.handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), userId, true, true, logContext, "pm command");
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    private boolean parseEnabled(String arg) throws IllegalArgumentException {
        char c;
        String lowerCase = arg.toLowerCase(Locale.US);
        switch (lowerCase.hashCode()) {
            case 3569038:
                if (lowerCase.equals("true")) {
                    c = 0;
                    break;
                }
                c = 65535;
                break;
            case 97196323:
                if (lowerCase.equals("false")) {
                    c = 1;
                    break;
                }
                c = 65535;
                break;
            default:
                c = 65535;
                break;
        }
        switch (c) {
            case 0:
                return true;
            case 1:
                return false;
            default:
                throw new IllegalArgumentException(arg + " is not a valid boolean");
        }
    }
}
