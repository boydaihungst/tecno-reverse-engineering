package com.android.server.locales;

import android.app.ActivityManager;
import android.app.ILocaleManager;
import android.os.LocaleList;
import android.os.RemoteException;
import android.os.ShellCommand;
import android.os.UserHandle;
import java.io.PrintWriter;
/* loaded from: classes.dex */
public class LocaleManagerShellCommand extends ShellCommand {
    private final ILocaleManager mBinderService;

    /* JADX INFO: Access modifiers changed from: package-private */
    public LocaleManagerShellCommand(ILocaleManager localeManager) {
        this.mBinderService = localeManager;
    }

    public int onCommand(String cmd) {
        if (cmd == null) {
            return handleDefaultCommands(cmd);
        }
        char c = 65535;
        switch (cmd.hashCode()) {
            case 819706294:
                if (cmd.equals("get-app-locales")) {
                    c = 1;
                    break;
                }
                break;
            case 1730458818:
                if (cmd.equals("set-app-locales")) {
                    c = 0;
                    break;
                }
                break;
        }
        switch (c) {
            case 0:
                return runSetAppLocales();
            case 1:
                return runGetAppLocales();
            default:
                return handleDefaultCommands(cmd);
        }
    }

    public void onHelp() {
        PrintWriter pw = getOutPrintWriter();
        pw.println("Locale manager (locale) shell commands:");
        pw.println("  help");
        pw.println("      Print this help text.");
        pw.println("  set-app-locales <PACKAGE_NAME> [--user <USER_ID>] [--locales <LOCALE_INFO>]");
        pw.println("      Set the locales for the specified app.");
        pw.println("      --user <USER_ID>: apply for the given user, the current user is used when unspecified.");
        pw.println("      --locales <LOCALE_INFO>: The language tags of locale to be included as a single String separated by commas");
        pw.println("                 Empty locale list is used when unspecified.");
        pw.println("                 eg. en,en-US,hi ");
        pw.println("  get-app-locales <PACKAGE_NAME> [--user <USER_ID>]");
        pw.println("      Get the locales for the specified app.");
        pw.println("      --user <USER_ID>: get for the given user, the current user is used when unspecified.");
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Code restructure failed: missing block: B:23:0x007d, code lost:
        if (r5.equals("--user") != false) goto L11;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private int runSetAppLocales() {
        PrintWriter err = getErrPrintWriter();
        String packageName = getNextArg();
        if (packageName != null) {
            int userId = ActivityManager.getCurrentUser();
            LocaleList locales = LocaleList.getEmptyLocaleList();
            while (true) {
                String option = getNextOption();
                boolean z = false;
                if (option != null) {
                    switch (option.hashCode()) {
                        case 1333469547:
                            break;
                        case 1724392377:
                            if (option.equals("--locales")) {
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
                            userId = UserHandle.parseUserArg(getNextArgRequired());
                            break;
                        case true:
                            locales = parseLocales();
                            break;
                        default:
                            throw new IllegalArgumentException("Unknown option: " + option);
                    }
                } else {
                    try {
                        this.mBinderService.setApplicationLocales(packageName, userId, locales);
                    } catch (RemoteException e) {
                        getOutPrintWriter().println("Remote Exception: " + e);
                    } catch (IllegalArgumentException e2) {
                        getOutPrintWriter().println("Unknown package " + packageName + " for userId " + userId);
                    }
                    return 0;
                }
            }
        } else {
            err.println("Error: no package specified");
            return -1;
        }
    }

    private int runGetAppLocales() {
        PrintWriter err = getErrPrintWriter();
        String packageName = getNextArg();
        if (packageName != null) {
            int userId = ActivityManager.getCurrentUser();
            String option = getNextOption();
            if (option != null) {
                if ("--user".equals(option)) {
                    userId = UserHandle.parseUserArg(getNextArgRequired());
                } else {
                    throw new IllegalArgumentException("Unknown option: " + option);
                }
            }
            try {
                LocaleList locales = this.mBinderService.getApplicationLocales(packageName, userId);
                getOutPrintWriter().println("Locales for " + packageName + " for user " + userId + " are [" + locales.toLanguageTags() + "]");
                return 0;
            } catch (RemoteException e) {
                getOutPrintWriter().println("Remote Exception: " + e);
                return 0;
            } catch (IllegalArgumentException e2) {
                getOutPrintWriter().println("Unknown package " + packageName + " for userId " + userId);
                return 0;
            }
        }
        err.println("Error: no package specified");
        return -1;
    }

    private LocaleList parseLocales() {
        if (getRemainingArgsCount() <= 0) {
            return LocaleList.getEmptyLocaleList();
        }
        String[] args = peekRemainingArgs();
        String inputLocales = args[0];
        LocaleList locales = LocaleList.forLanguageTags(inputLocales);
        return locales;
    }
}
