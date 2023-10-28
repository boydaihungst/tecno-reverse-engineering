package com.android.server.locksettings;

import android.app.ActivityManager;
import android.app.admin.PasswordMetrics;
import android.content.Context;
import android.os.ShellCommand;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.Slog;
import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.LockscreenCredential;
import com.android.internal.widget.PasswordValidationError;
import java.io.PrintWriter;
import java.util.List;
/* loaded from: classes.dex */
class LockSettingsShellCommand extends ShellCommand {
    private static final String COMMAND_CLEAR = "clear";
    private static final String COMMAND_GET_DISABLED = "get-disabled";
    private static final String COMMAND_HELP = "help";
    private static final String COMMAND_REMOVE_CACHE = "remove-cache";
    private static final String COMMAND_REQUIRE_STRONG_AUTH = "require-strong-auth";
    private static final String COMMAND_SET_DISABLED = "set-disabled";
    private static final String COMMAND_SET_PASSWORD = "set-password";
    private static final String COMMAND_SET_PATTERN = "set-pattern";
    private static final String COMMAND_SET_PIN = "set-pin";
    private static final String COMMAND_SET_ROR_PROVIDER_PACKAGE = "set-resume-on-reboot-provider-package";
    private static final String COMMAND_SP = "sp";
    private static final String COMMAND_VERIFY = "verify";
    private final int mCallingPid;
    private final int mCallingUid;
    private final Context mContext;
    private int mCurrentUserId;
    private final LockPatternUtils mLockPatternUtils;
    private String mOld = "";
    private String mNew = "";

    /* JADX INFO: Access modifiers changed from: package-private */
    public LockSettingsShellCommand(LockPatternUtils lockPatternUtils, Context context, int callingPid, int callingUid) {
        this.mLockPatternUtils = lockPatternUtils;
        this.mCallingPid = callingPid;
        this.mCallingUid = callingUid;
        this.mContext = context;
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    public int onCommand(String cmd) {
        boolean z;
        boolean z2;
        if (cmd == null) {
            return handleDefaultCommands(cmd);
        }
        try {
            this.mCurrentUserId = ActivityManager.getService().getCurrentUser().id;
            parseArgs();
            char c = 1;
            if (!this.mLockPatternUtils.hasSecureLockScreen()) {
                switch (cmd.hashCode()) {
                    case -1473704173:
                        if (cmd.equals(COMMAND_GET_DISABLED)) {
                            z2 = true;
                            break;
                        }
                        z2 = true;
                        break;
                    case 3198785:
                        if (cmd.equals(COMMAND_HELP)) {
                            z2 = false;
                            break;
                        }
                        z2 = true;
                        break;
                    case 75288455:
                        if (cmd.equals(COMMAND_SET_DISABLED)) {
                            z2 = true;
                            break;
                        }
                        z2 = true;
                        break;
                    case 1062640281:
                        if (cmd.equals(COMMAND_SET_ROR_PROVIDER_PACKAGE)) {
                            z2 = true;
                            break;
                        }
                        z2 = true;
                        break;
                    default:
                        z2 = true;
                        break;
                }
                switch (z2) {
                    case false:
                    case true:
                    case true:
                    case true:
                        break;
                    default:
                        getErrPrintWriter().println("The device does not support lock screen - ignoring the command.");
                        return -1;
                }
            }
            switch (cmd.hashCode()) {
                case -1957541639:
                    if (cmd.equals(COMMAND_REMOVE_CACHE)) {
                        z = false;
                        break;
                    }
                    z = true;
                    break;
                case -868666698:
                    if (cmd.equals(COMMAND_REQUIRE_STRONG_AUTH)) {
                        z = true;
                        break;
                    }
                    z = true;
                    break;
                case 3198785:
                    if (cmd.equals(COMMAND_HELP)) {
                        z = true;
                        break;
                    }
                    z = true;
                    break;
                case 1062640281:
                    if (cmd.equals(COMMAND_SET_ROR_PROVIDER_PACKAGE)) {
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
                    runRemoveCache();
                    return 0;
                case true:
                    runSetResumeOnRebootProviderPackage();
                    return 0;
                case true:
                    runRequireStrongAuth();
                    return 0;
                case true:
                    onHelp();
                    return 0;
                default:
                    if (!checkCredential()) {
                        return -1;
                    }
                    boolean success = true;
                    switch (cmd.hashCode()) {
                        case -2044327643:
                            if (cmd.equals(COMMAND_SET_PATTERN)) {
                                c = 0;
                                break;
                            }
                            c = 65535;
                            break;
                        case -1473704173:
                            if (cmd.equals(COMMAND_GET_DISABLED)) {
                                c = 7;
                                break;
                            }
                            c = 65535;
                            break;
                        case -819951495:
                            if (cmd.equals(COMMAND_VERIFY)) {
                                c = 6;
                                break;
                            }
                            c = 65535;
                            break;
                        case 3677:
                            if (cmd.equals(COMMAND_SP)) {
                                c = 4;
                                break;
                            }
                            c = 65535;
                            break;
                        case 75288455:
                            if (cmd.equals(COMMAND_SET_DISABLED)) {
                                c = 5;
                                break;
                            }
                            c = 65535;
                            break;
                        case 94746189:
                            if (cmd.equals(COMMAND_CLEAR)) {
                                c = 3;
                                break;
                            }
                            c = 65535;
                            break;
                        case 1021333414:
                            if (cmd.equals(COMMAND_SET_PASSWORD)) {
                                break;
                            }
                            c = 65535;
                            break;
                        case 1983832490:
                            if (cmd.equals(COMMAND_SET_PIN)) {
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
                            success = runSetPattern();
                            break;
                        case 1:
                            success = runSetPassword();
                            break;
                        case 2:
                            success = runSetPin();
                            break;
                        case 3:
                            success = runClear();
                            break;
                        case 4:
                            runChangeSp();
                            break;
                        case 5:
                            runSetDisabled();
                            break;
                        case 6:
                            runVerify();
                            break;
                        case 7:
                            runGetDisabled();
                            break;
                        default:
                            getErrPrintWriter().println("Unknown command: " + cmd);
                            break;
                    }
                    return success ? 0 : -1;
            }
        } catch (Exception e) {
            getErrPrintWriter().println("Error while executing command: " + cmd);
            e.printStackTrace(getErrPrintWriter());
            return -1;
        }
    }

    private void runVerify() {
        getOutPrintWriter().println("Lock credential verified successfully");
    }

    public void onHelp() {
        PrintWriter pw = getOutPrintWriter();
        try {
            pw.println("lockSettings service commands:");
            pw.println("");
            pw.println("NOTE: when lock screen is set, all commands require the --old <CREDENTIAL> argument.");
            pw.println("");
            pw.println("  help");
            pw.println("    Prints this help text.");
            pw.println("");
            pw.println("  get-disabled [--old <CREDENTIAL>] [--user USER_ID]");
            pw.println("    Checks whether lock screen is disabled.");
            pw.println("");
            pw.println("  set-disabled [--old <CREDENTIAL>] [--user USER_ID] <true|false>");
            pw.println("    When true, disables lock screen.");
            pw.println("");
            pw.println("  set-pattern [--old <CREDENTIAL>] [--user USER_ID] <PATTERN>");
            pw.println("    Sets the lock screen as pattern, using the given PATTERN to unlock.");
            pw.println("");
            pw.println("  set-pin [--old <CREDENTIAL>] [--user USER_ID] <PIN>");
            pw.println("    Sets the lock screen as PIN, using the given PIN to unlock.");
            pw.println("");
            pw.println("  set-password [--old <CREDENTIAL>] [--user USER_ID] <PASSWORD>");
            pw.println("    Sets the lock screen as password, using the given PASSOWRD to unlock.");
            pw.println("");
            pw.println("  sp [--old <CREDENTIAL>] [--user USER_ID]");
            pw.println("    Gets whether synthetic password is enabled.");
            pw.println("");
            pw.println("  sp [--old <CREDENTIAL>] [--user USER_ID] <1|0>");
            pw.println("    Enables / disables synthetic password.");
            pw.println("");
            pw.println("  clear [--old <CREDENTIAL>] [--user USER_ID]");
            pw.println("    Clears the lock credentials.");
            pw.println("");
            pw.println("  verify [--old <CREDENTIAL>] [--user USER_ID]");
            pw.println("    Verifies the lock credentials.");
            pw.println("");
            pw.println("  remove-cache [--user USER_ID]");
            pw.println("    Removes cached unified challenge for the managed profile.");
            pw.println("");
            pw.println("  set-resume-on-reboot-provider-package <package_name>");
            pw.println("    Sets the package name for server based resume on reboot service provider.");
            pw.println("");
            pw.println("  require-strong-auth [--user USER_ID] <reason>");
            pw.println("    Requires the strong authentication. The current supported reasons: STRONG_AUTH_REQUIRED_AFTER_USER_LOCKDOWN.");
            pw.println("");
            if (pw != null) {
                pw.close();
            }
        } catch (Throwable th) {
            if (pw != null) {
                try {
                    pw.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
            }
            throw th;
        }
    }

    private void parseArgs() {
        while (true) {
            String opt = getNextOption();
            if (opt != null) {
                if ("--old".equals(opt)) {
                    this.mOld = getNextArgRequired();
                } else if ("--user".equals(opt)) {
                    this.mCurrentUserId = Integer.parseInt(getNextArgRequired());
                } else {
                    getErrPrintWriter().println("Unknown option: " + opt);
                    throw new IllegalArgumentException();
                }
            } else {
                this.mNew = getNextArg();
                return;
            }
        }
    }

    private void runChangeSp() {
        String str = this.mNew;
        if (str != null) {
            if ("1".equals(str)) {
                this.mLockPatternUtils.enableSyntheticPassword();
                getOutPrintWriter().println("Synthetic password enabled");
            } else if ("0".equals(this.mNew)) {
                this.mLockPatternUtils.disableSyntheticPassword();
                getOutPrintWriter().println("Synthetic password disabled");
            }
        }
        getOutPrintWriter().println(String.format("SP Enabled = %b", Boolean.valueOf(this.mLockPatternUtils.isSyntheticPasswordEnabled())));
    }

    private LockscreenCredential getOldCredential() {
        if (TextUtils.isEmpty(this.mOld)) {
            return LockscreenCredential.createNone();
        }
        if (this.mLockPatternUtils.isLockPasswordEnabled(this.mCurrentUserId)) {
            int quality = this.mLockPatternUtils.getKeyguardStoredPasswordQuality(this.mCurrentUserId);
            if (LockPatternUtils.isQualityAlphabeticPassword(quality)) {
                return LockscreenCredential.createPassword(this.mOld);
            }
            return LockscreenCredential.createPin(this.mOld);
        } else if (this.mLockPatternUtils.isLockPatternEnabled(this.mCurrentUserId)) {
            return LockscreenCredential.createPattern(LockPatternUtils.byteArrayToPattern(this.mOld.getBytes()));
        } else {
            return LockscreenCredential.createPassword(this.mOld);
        }
    }

    private boolean runSetPattern() {
        LockscreenCredential pattern = LockscreenCredential.createPattern(LockPatternUtils.byteArrayToPattern(this.mNew.getBytes()));
        if (!isNewCredentialSufficient(pattern)) {
            return false;
        }
        this.mLockPatternUtils.setLockCredential(pattern, getOldCredential(), this.mCurrentUserId);
        getOutPrintWriter().println("Pattern set to '" + this.mNew + "'");
        return true;
    }

    private boolean runSetPassword() {
        LockscreenCredential password = LockscreenCredential.createPassword(this.mNew);
        if (!isNewCredentialSufficient(password)) {
            return false;
        }
        this.mLockPatternUtils.setLockCredential(password, getOldCredential(), this.mCurrentUserId);
        getOutPrintWriter().println("Password set to '" + this.mNew + "'");
        return true;
    }

    private boolean runSetPin() {
        LockscreenCredential pin = LockscreenCredential.createPin(this.mNew);
        if (!isNewCredentialSufficient(pin)) {
            return false;
        }
        this.mLockPatternUtils.setLockCredential(pin, getOldCredential(), this.mCurrentUserId);
        getOutPrintWriter().println("Pin set to '" + this.mNew + "'");
        return true;
    }

    private boolean runSetResumeOnRebootProviderPackage() {
        String packageName = this.mNew;
        Slog.i("ShellCommand", "Setting persist.sys.resume_on_reboot_provider_package to " + packageName);
        this.mContext.enforcePermission("android.permission.BIND_RESUME_ON_REBOOT_SERVICE", this.mCallingPid, this.mCallingUid, "ShellCommand");
        SystemProperties.set("persist.sys.resume_on_reboot_provider_package", packageName);
        return true;
    }

    private boolean runRequireStrongAuth() {
        boolean z;
        String reason = this.mNew;
        switch (reason.hashCode()) {
            case 1785592813:
                if (reason.equals("STRONG_AUTH_REQUIRED_AFTER_USER_LOCKDOWN")) {
                    z = false;
                    break;
                }
            default:
                z = true;
                break;
        }
        switch (z) {
            case false:
                this.mCurrentUserId = -1;
                this.mLockPatternUtils.requireStrongAuth(32, -1);
                getOutPrintWriter().println("Require strong auth for USER_ID " + this.mCurrentUserId + " because of " + this.mNew);
                return true;
            default:
                getErrPrintWriter().println("Unsupported reason: " + reason);
                return false;
        }
    }

    private boolean runClear() {
        LockscreenCredential none = LockscreenCredential.createNone();
        if (!isNewCredentialSufficient(none)) {
            return false;
        }
        this.mLockPatternUtils.setLockCredential(none, getOldCredential(), this.mCurrentUserId);
        getOutPrintWriter().println("Lock credential cleared");
        return true;
    }

    private boolean isNewCredentialSufficient(LockscreenCredential credential) {
        List<PasswordValidationError> errors;
        PasswordMetrics requiredMetrics = this.mLockPatternUtils.getRequestedPasswordMetrics(this.mCurrentUserId);
        int requiredComplexity = this.mLockPatternUtils.getRequestedPasswordComplexity(this.mCurrentUserId);
        if (credential.isPassword() || credential.isPin()) {
            errors = PasswordMetrics.validatePassword(requiredMetrics, requiredComplexity, credential.isPin(), credential.getCredential());
        } else {
            PasswordMetrics metrics = new PasswordMetrics(credential.isPattern() ? 1 : -1);
            errors = PasswordMetrics.validatePasswordMetrics(requiredMetrics, requiredComplexity, metrics);
        }
        if (errors.isEmpty()) {
            return true;
        }
        getOutPrintWriter().println("New credential doesn't satisfy admin policies: " + errors.get(0));
        return false;
    }

    private void runSetDisabled() {
        boolean disabled = Boolean.parseBoolean(this.mNew);
        this.mLockPatternUtils.setLockScreenDisabled(disabled, this.mCurrentUserId);
        getOutPrintWriter().println("Lock screen disabled set to " + disabled);
    }

    private void runGetDisabled() {
        boolean isLockScreenDisabled = this.mLockPatternUtils.isLockScreenDisabled(this.mCurrentUserId);
        getOutPrintWriter().println(isLockScreenDisabled);
    }

    private boolean checkCredential() {
        if (this.mLockPatternUtils.isSecure(this.mCurrentUserId)) {
            if (this.mLockPatternUtils.isManagedProfileWithUnifiedChallenge(this.mCurrentUserId)) {
                getOutPrintWriter().println("Profile uses unified challenge");
                return false;
            }
            try {
                boolean result = this.mLockPatternUtils.checkCredential(getOldCredential(), this.mCurrentUserId, (LockPatternUtils.CheckCredentialProgressCallback) null);
                if (!result) {
                    if (!this.mLockPatternUtils.isManagedProfileWithUnifiedChallenge(this.mCurrentUserId)) {
                        this.mLockPatternUtils.reportFailedPasswordAttempt(this.mCurrentUserId);
                    }
                    getOutPrintWriter().println("Old password '" + this.mOld + "' didn't match");
                } else {
                    this.mLockPatternUtils.reportSuccessfulPasswordAttempt(this.mCurrentUserId);
                }
                return result;
            } catch (LockPatternUtils.RequestThrottledException e) {
                getOutPrintWriter().println("Request throttled");
                return false;
            }
        } else if (!this.mOld.isEmpty()) {
            getOutPrintWriter().println("Old password provided but user has no password");
            return false;
        } else {
            return true;
        }
    }

    private void runRemoveCache() {
        this.mLockPatternUtils.removeCachedUnifiedChallenge(this.mCurrentUserId);
        getOutPrintWriter().println("Password cached removed for user " + this.mCurrentUserId);
    }
}
