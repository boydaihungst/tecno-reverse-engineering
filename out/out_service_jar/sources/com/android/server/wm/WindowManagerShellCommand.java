package com.android.server.wm;

import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.ShellCommand;
import android.provider.Settings;
import android.util.Pair;
import android.view.CrossWindowBlurListeners;
import android.view.IWindowManager;
import com.android.internal.os.ByteTransferPipe;
import com.android.internal.policy.IKeyguardDismissCallback;
import com.android.internal.protolog.ProtoLogImpl;
import com.android.server.LocalServices;
import com.android.server.UiModeManagerService;
import com.android.server.health.HealthServiceWrapperHidl;
import com.android.server.statusbar.StatusBarManagerInternal;
import com.android.server.timezonedetector.ServiceConfigAccessor;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
/* loaded from: classes2.dex */
public class WindowManagerShellCommand extends ShellCommand {
    private final IWindowManager mInterface;
    private final WindowManagerService mInternal;

    public WindowManagerShellCommand(WindowManagerService service) {
        this.mInterface = service;
        this.mInternal = service;
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    public int onCommand(String cmd) {
        char c;
        if (cmd == null) {
            return handleDefaultCommands(cmd);
        }
        PrintWriter pw = getOutPrintWriter();
        try {
            switch (cmd.hashCode()) {
                case -1959253708:
                    if (cmd.equals("get-multi-window-config")) {
                        c = '\r';
                        break;
                    }
                    c = 65535;
                    break;
                case -1829173266:
                    if (cmd.equals("get-ignore-orientation-request")) {
                        c = '\n';
                        break;
                    }
                    c = 65535;
                    break;
                case -1693379742:
                    if (cmd.equals("set-ignore-orientation-request")) {
                        c = '\t';
                        break;
                    }
                    c = 65535;
                    break;
                case -1067396926:
                    if (cmd.equals("tracing")) {
                        c = 5;
                        break;
                    }
                    c = 65535;
                    break;
                case -1032601556:
                    if (cmd.equals("disable-blur")) {
                        c = 16;
                        break;
                    }
                    c = 65535;
                    break;
                case -1014709755:
                    if (cmd.equals("dump-visible-window-views")) {
                        c = 11;
                        break;
                    }
                    c = 65535;
                    break;
                case -336752166:
                    if (cmd.equals("folded-area")) {
                        c = 2;
                        break;
                    }
                    c = 65535;
                    break;
                case -229462135:
                    if (cmd.equals("dismiss-keyguard")) {
                        c = 4;
                        break;
                    }
                    c = 65535;
                    break;
                case 3530753:
                    if (cmd.equals("size")) {
                        c = 0;
                        break;
                    }
                    c = 65535;
                    break;
                case 93018176:
                    if (cmd.equals("set-multi-window-config")) {
                        c = '\f';
                        break;
                    }
                    c = 65535;
                    break;
                case 108404047:
                    if (cmd.equals("reset")) {
                        c = 15;
                        break;
                    }
                    c = 65535;
                    break;
                case 188660544:
                    if (cmd.equals("user-rotation")) {
                        c = 7;
                        break;
                    }
                    c = 65535;
                    break;
                case 342281055:
                    if (cmd.equals("logging")) {
                        c = 6;
                        break;
                    }
                    c = 65535;
                    break;
                case 1336606893:
                    if (cmd.equals("reset-multi-window-config")) {
                        c = 14;
                        break;
                    }
                    c = 65535;
                    break;
                case 1552717032:
                    if (cmd.equals("density")) {
                        c = 1;
                        break;
                    }
                    c = 65535;
                    break;
                case 1583955111:
                    if (cmd.equals("fixed-to-user-rotation")) {
                        c = '\b';
                        break;
                    }
                    c = 65535;
                    break;
                case 1910897543:
                    if (cmd.equals("scaling")) {
                        c = 3;
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
                    return runDisplaySize(pw);
                case 1:
                    return runDisplayDensity(pw);
                case 2:
                    return runDisplayFoldedArea(pw);
                case 3:
                    return runDisplayScaling(pw);
                case 4:
                    return runDismissKeyguard(pw);
                case 5:
                    return this.mInternal.mWindowTracing.onShellCommand(this);
                case 6:
                    String[] args = peekRemainingArgs();
                    int result = ProtoLogImpl.getSingleInstance().onShellCommand(this);
                    if (result != 0) {
                        try {
                            ParcelFileDescriptor pfd = ParcelFileDescriptor.dup(getOutFileDescriptor());
                            try {
                                pw.println("Not handled, calling status bar with args: " + Arrays.toString(args));
                                ((StatusBarManagerInternal) LocalServices.getService(StatusBarManagerInternal.class)).handleWindowManagerLoggingCommand(args, pfd);
                                if (pfd != null) {
                                    pfd.close();
                                }
                            } catch (Throwable th) {
                                if (pfd != null) {
                                    try {
                                        pfd.close();
                                    } catch (Throwable th2) {
                                        th.addSuppressed(th2);
                                    }
                                }
                                throw th;
                            }
                        } catch (IOException e) {
                            pw.println("Failed to handle logging command: " + e.getMessage());
                        }
                    }
                    return result;
                case 7:
                    return runDisplayUserRotation(pw);
                case '\b':
                    return runFixedToUserRotation(pw);
                case '\t':
                    return runSetIgnoreOrientationRequest(pw);
                case '\n':
                    return runGetIgnoreOrientationRequest(pw);
                case 11:
                    return runDumpVisibleWindowViews(pw);
                case '\f':
                    return runSetMultiWindowConfig();
                case '\r':
                    return runGetMultiWindowConfig(pw);
                case 14:
                    return runResetMultiWindowConfig();
                case 15:
                    return runReset(pw);
                case 16:
                    return runSetBlurDisabled(pw);
                default:
                    return handleDefaultCommands(cmd);
            }
        } catch (RemoteException e2) {
            pw.println("Remote exception: " + e2);
            return -1;
        }
    }

    private int getDisplayId(String opt) {
        String option = "-d".equals(opt) ? opt : getNextOption();
        if (option == null || !"-d".equals(option)) {
            return 0;
        }
        try {
            int displayId = Integer.parseInt(getNextArgRequired());
            return displayId;
        } catch (NumberFormatException e) {
            getErrPrintWriter().println("Error: bad number " + e);
            return 0;
        } catch (IllegalArgumentException e2) {
            getErrPrintWriter().println("Error: " + e2);
            return 0;
        }
    }

    private void printInitialDisplaySize(PrintWriter pw, int displayId) {
        Point initialSize = new Point();
        Point baseSize = new Point();
        try {
            this.mInterface.getInitialDisplaySize(displayId, initialSize);
            this.mInterface.getBaseDisplaySize(displayId, baseSize);
            pw.println("Physical size: " + initialSize.x + "x" + initialSize.y);
            if (!initialSize.equals(baseSize)) {
                pw.println("Override size: " + baseSize.x + "x" + baseSize.y);
            }
        } catch (RemoteException e) {
            pw.println("Remote exception: " + e);
        }
    }

    private int runDisplaySize(PrintWriter pw) throws RemoteException {
        int div;
        String size = getNextArg();
        int displayId = getDisplayId(size);
        if (size == null) {
            printInitialDisplaySize(pw, displayId);
            return 0;
        } else if ("-d".equals(size)) {
            printInitialDisplaySize(pw, displayId);
            return 0;
        } else {
            int w = -1;
            if ("reset".equals(size)) {
                div = -1;
            } else {
                int div2 = size.indexOf(120);
                if (div2 <= 0 || div2 >= size.length() - 1) {
                    getErrPrintWriter().println("Error: bad size " + size);
                    return -1;
                }
                String wstr = size.substring(0, div2);
                String hstr = size.substring(div2 + 1);
                try {
                    int w2 = parseDimension(wstr, displayId);
                    int h = parseDimension(hstr, displayId);
                    div = h;
                    w = w2;
                } catch (NumberFormatException e) {
                    getErrPrintWriter().println("Error: bad number " + e);
                    return -1;
                }
            }
            if (w >= 0 && div >= 0) {
                this.mInterface.setForcedDisplaySize(displayId, w, div);
            } else {
                this.mInterface.clearForcedDisplaySize(displayId);
            }
            return 0;
        }
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    private int runSetBlurDisabled(PrintWriter pw) throws RemoteException {
        char c;
        int i;
        String arg = getNextArg();
        if (arg == null) {
            pw.println("Blur supported on device: " + CrossWindowBlurListeners.CROSS_WINDOW_BLUR_SUPPORTED);
            pw.println("Blur enabled: " + this.mInternal.mBlurController.getBlurEnabled());
            return 0;
        }
        switch (arg.hashCode()) {
            case 48:
                if (arg.equals("0")) {
                    c = 3;
                    break;
                }
                c = 65535;
                break;
            case 49:
                if (arg.equals("1")) {
                    c = 1;
                    break;
                }
                c = 65535;
                break;
            case 3569038:
                if (arg.equals("true")) {
                    c = 0;
                    break;
                }
                c = 65535;
                break;
            case 97196323:
                if (arg.equals("false")) {
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
            case 1:
                i = 1;
                break;
            case 2:
            case 3:
                i = 0;
                break;
            default:
                getErrPrintWriter().println("Error: expected true, 1, false, 0, but got " + arg);
                return -1;
        }
        Settings.Global.putInt(this.mInternal.mContext.getContentResolver(), "disable_window_blurs", i);
        return 0;
    }

    private void printInitialDisplayDensity(PrintWriter pw, int displayId) {
        try {
            int initialDensity = this.mInterface.getInitialDisplayDensity(displayId);
            int baseDensity = this.mInterface.getBaseDisplayDensity(displayId);
            pw.println("Physical density: " + initialDensity);
            if (initialDensity != baseDensity) {
                pw.println("Override density: " + baseDensity);
            }
        } catch (RemoteException e) {
            pw.println("Remote exception: " + e);
        }
    }

    private int runDisplayDensity(PrintWriter pw) throws RemoteException {
        int density;
        String densityStr = getNextArg();
        int displayId = getDisplayId(densityStr);
        if (densityStr == null) {
            printInitialDisplayDensity(pw, displayId);
            return 0;
        } else if ("-d".equals(densityStr)) {
            printInitialDisplayDensity(pw, displayId);
            return 0;
        } else {
            if ("reset".equals(densityStr)) {
                density = -1;
            } else {
                try {
                    int density2 = Integer.parseInt(densityStr);
                    if (density2 >= 72) {
                        density = density2;
                    } else {
                        getErrPrintWriter().println("Error: density must be >= 72");
                        return -1;
                    }
                } catch (NumberFormatException e) {
                    getErrPrintWriter().println("Error: bad number " + e);
                    return -1;
                }
            }
            if (density > 0) {
                this.mInterface.setForcedDisplayDensityForUser(displayId, density, -2);
            } else {
                this.mInterface.clearForcedDisplayDensityForUser(displayId, -2);
            }
            return 0;
        }
    }

    private void printFoldedArea(PrintWriter pw) {
        Rect foldedArea = this.mInternal.getFoldedArea();
        if (foldedArea.isEmpty()) {
            pw.println("Folded area: none");
        } else {
            pw.println("Folded area: " + foldedArea.left + "," + foldedArea.top + "," + foldedArea.right + "," + foldedArea.bottom);
        }
    }

    private int runDisplayFoldedArea(PrintWriter pw) {
        String areaStr = getNextArg();
        Rect rect = new Rect();
        if (areaStr == null) {
            printFoldedArea(pw);
            return 0;
        }
        if ("reset".equals(areaStr)) {
            rect.setEmpty();
        } else {
            Pattern flattenedPattern = Pattern.compile("(-?\\d+),(-?\\d+),(-?\\d+),(-?\\d+)");
            Matcher matcher = flattenedPattern.matcher(areaStr);
            if (!matcher.matches()) {
                getErrPrintWriter().println("Error: area should be LEFT,TOP,RIGHT,BOTTOM");
                return -1;
            }
            rect.set(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)), Integer.parseInt(matcher.group(3)), Integer.parseInt(matcher.group(4)));
        }
        this.mInternal.setOverrideFoldedArea(rect);
        return 0;
    }

    private int runDisplayScaling(PrintWriter pw) throws RemoteException {
        String scalingStr = getNextArgRequired();
        if (UiModeManagerService.Shell.NIGHT_MODE_STR_AUTO.equals(scalingStr)) {
            this.mInterface.setForcedDisplayScalingMode(getDisplayId(scalingStr), 0);
        } else if ("off".equals(scalingStr)) {
            this.mInterface.setForcedDisplayScalingMode(getDisplayId(scalingStr), 1);
        } else {
            getErrPrintWriter().println("Error: scaling must be 'auto' or 'off'");
            return -1;
        }
        return 0;
    }

    private int runDismissKeyguard(PrintWriter pw) throws RemoteException {
        this.mInterface.dismissKeyguard((IKeyguardDismissCallback) null, (CharSequence) null);
        return 0;
    }

    private int parseDimension(String s, int displayId) throws NumberFormatException {
        int density;
        if (s.endsWith("px")) {
            return Integer.parseInt(s.substring(0, s.length() - 2));
        }
        if (s.endsWith("dp")) {
            try {
                density = this.mInterface.getBaseDisplayDensity(displayId);
            } catch (RemoteException e) {
                density = 160;
            }
            return (Integer.parseInt(s.substring(0, s.length() - 2)) * density) / 160;
        }
        int density2 = Integer.parseInt(s);
        return density2;
    }

    private int runDisplayUserRotation(PrintWriter pw) {
        int rotation;
        int displayId = 0;
        String arg = getNextArg();
        if (arg == null) {
            return printDisplayUserRotation(pw, 0);
        }
        if ("-d".equals(arg)) {
            displayId = Integer.parseInt(getNextArgRequired());
            arg = getNextArg();
        }
        String lockMode = arg;
        if (lockMode == null) {
            return printDisplayUserRotation(pw, displayId);
        }
        if ("free".equals(lockMode)) {
            this.mInternal.thawDisplayRotation(displayId);
            return 0;
        } else if (!"lock".equals(lockMode)) {
            getErrPrintWriter().println("Error: argument needs to be either -d, free or lock.");
            return -1;
        } else {
            String arg2 = getNextArg();
            if (arg2 == null) {
                rotation = -1;
            } else {
                try {
                    rotation = Integer.parseInt(arg2);
                } catch (IllegalArgumentException e) {
                    getErrPrintWriter().println("Error: " + e.getMessage());
                    return -1;
                }
            }
            this.mInternal.freezeDisplayRotation(displayId, rotation);
            return 0;
        }
    }

    private int printDisplayUserRotation(PrintWriter pw, int displayId) {
        int displayUserRotation = this.mInternal.getDisplayUserRotation(displayId);
        if (displayUserRotation < 0) {
            getErrPrintWriter().println("Error: check logcat for more details.");
            return -1;
        } else if (!this.mInternal.isDisplayRotationFrozen(displayId)) {
            pw.println("free");
            return 0;
        } else {
            pw.print("lock ");
            pw.println(displayUserRotation);
            return 0;
        }
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    private int runFixedToUserRotation(PrintWriter pw) throws RemoteException {
        char c;
        int fixedToUserRotation;
        int displayId = 0;
        String arg = getNextArg();
        if (arg == null) {
            printFixedToUserRotation(pw, 0);
            return 0;
        }
        if ("-d".equals(arg)) {
            displayId = Integer.parseInt(getNextArgRequired());
            arg = getNextArg();
        }
        if (arg == null) {
            return printFixedToUserRotation(pw, displayId);
        }
        switch (arg.hashCode()) {
            case -1609594047:
                if (arg.equals(ServiceConfigAccessor.PROVIDER_MODE_ENABLED)) {
                    c = 0;
                    break;
                }
                c = 65535;
                break;
            case 270940796:
                if (arg.equals(ServiceConfigAccessor.PROVIDER_MODE_DISABLED)) {
                    c = 1;
                    break;
                }
                c = 65535;
                break;
            case 1544803905:
                if (arg.equals(HealthServiceWrapperHidl.INSTANCE_VENDOR)) {
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
                fixedToUserRotation = 2;
                break;
            case 1:
                fixedToUserRotation = 1;
                break;
            case 2:
                fixedToUserRotation = 0;
                break;
            default:
                getErrPrintWriter().println("Error: expecting enabled, disabled or default, but we get " + arg);
                return -1;
        }
        this.mInterface.setFixedToUserRotation(displayId, fixedToUserRotation);
        return 0;
    }

    private int printFixedToUserRotation(PrintWriter pw, int displayId) {
        int fixedToUserRotationMode = this.mInternal.getFixedToUserRotation(displayId);
        switch (fixedToUserRotationMode) {
            case 0:
                pw.println(HealthServiceWrapperHidl.INSTANCE_VENDOR);
                return 0;
            case 1:
                pw.println(ServiceConfigAccessor.PROVIDER_MODE_DISABLED);
                return 0;
            case 2:
                pw.println(ServiceConfigAccessor.PROVIDER_MODE_ENABLED);
                return 0;
            default:
                getErrPrintWriter().println("Error: check logcat for more details.");
                return -1;
        }
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    private int runSetIgnoreOrientationRequest(PrintWriter pw) throws RemoteException {
        char c;
        boolean ignoreOrientationRequest;
        int displayId = 0;
        String arg = getNextArgRequired();
        if ("-d".equals(arg)) {
            displayId = Integer.parseInt(getNextArgRequired());
            arg = getNextArgRequired();
        }
        switch (arg.hashCode()) {
            case 48:
                if (arg.equals("0")) {
                    c = 3;
                    break;
                }
                c = 65535;
                break;
            case 49:
                if (arg.equals("1")) {
                    c = 1;
                    break;
                }
                c = 65535;
                break;
            case 3569038:
                if (arg.equals("true")) {
                    c = 0;
                    break;
                }
                c = 65535;
                break;
            case 97196323:
                if (arg.equals("false")) {
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
            case 1:
                ignoreOrientationRequest = true;
                break;
            case 2:
            case 3:
                ignoreOrientationRequest = false;
                break;
            default:
                getErrPrintWriter().println("Error: expecting true, 1, false, 0, but we get " + arg);
                return -1;
        }
        this.mInterface.setIgnoreOrientationRequest(displayId, ignoreOrientationRequest);
        return 0;
    }

    private int runGetIgnoreOrientationRequest(PrintWriter pw) throws RemoteException {
        int displayId = 0;
        String arg = getNextArg();
        if ("-d".equals(arg)) {
            displayId = Integer.parseInt(getNextArgRequired());
        }
        boolean ignoreOrientationRequest = this.mInternal.getIgnoreOrientationRequest(displayId);
        pw.println("ignoreOrientationRequest " + ignoreOrientationRequest + " for displayId=" + displayId);
        return 0;
    }

    private int runDumpVisibleWindowViews(PrintWriter pw) {
        final int recentsComponentUid;
        if (!this.mInternal.checkCallingPermission("android.permission.DUMP", "runDumpVisibleWindowViews()")) {
            throw new SecurityException("Requires DUMP permission");
        }
        try {
            ZipOutputStream out = new ZipOutputStream(getRawOutputStream());
            final ArrayList<Pair<String, ByteTransferPipe>> requestList = new ArrayList<>();
            synchronized (this.mInternal.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    RecentTasks recentTasks = this.mInternal.mAtmService.getRecentTasks();
                    if (recentTasks != null) {
                        recentsComponentUid = recentTasks.getRecentsComponentUid();
                    } else {
                        recentsComponentUid = -1;
                    }
                    this.mInternal.mRoot.forAllWindows(new Consumer() { // from class: com.android.server.wm.WindowManagerShellCommand$$ExternalSyntheticLambda0
                        @Override // java.util.function.Consumer
                        public final void accept(Object obj) {
                            WindowManagerShellCommand.lambda$runDumpVisibleWindowViews$0(recentsComponentUid, requestList, (WindowState) obj);
                        }
                    }, false);
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
            Iterator<Pair<String, ByteTransferPipe>> it = requestList.iterator();
            while (it.hasNext()) {
                Pair<String, ByteTransferPipe> entry = it.next();
                try {
                    byte[] data = ((ByteTransferPipe) entry.second).get();
                    out.putNextEntry(new ZipEntry((String) entry.first));
                    out.write(data);
                } catch (IOException e) {
                }
            }
            out.close();
        } catch (IOException e2) {
            pw.println("Error fetching dump " + e2.getMessage());
        }
        return 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$runDumpVisibleWindowViews$0(int recentsComponentUid, ArrayList requestList, WindowState w) {
        boolean isRecents = w.getUid() == recentsComponentUid;
        if (w.isVisible() || isRecents) {
            ByteTransferPipe pipe = null;
            try {
                pipe = new ByteTransferPipe();
                w.mClient.executeCommand("DUMP_ENCODED", (String) null, pipe.getWriteFd());
                requestList.add(Pair.create(w.getName(), pipe));
            } catch (RemoteException | IOException e) {
                if (pipe != null) {
                    pipe.kill();
                }
            }
        }
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Code restructure failed: missing block: B:15:0x0034, code lost:
        if (r1.equals("--supportsNonResizable") != false) goto L12;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private int runSetMultiWindowConfig() {
        if (peekNextArg() == null) {
            getErrPrintWriter().println("Error: No arguments provided.");
        }
        int result = 0;
        while (true) {
            boolean z = false;
            if (peekNextArg() == null) {
                return result == 0 ? 0 : -1;
            }
            String arg = getNextArg();
            switch (arg.hashCode()) {
                case 1485032610:
                    break;
                case 1714039607:
                    if (arg.equals("--respectsActivityMinWidthHeight")) {
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
                    result += runSetSupportsNonResizableMultiWindow();
                    break;
                case true:
                    result += runSetRespectsActivityMinWidthHeightMultiWindow();
                    break;
                default:
                    getErrPrintWriter().println("Error: Unrecognized multi window option: " + arg);
                    return -1;
            }
        }
    }

    private int runSetSupportsNonResizableMultiWindow() {
        String arg = getNextArg();
        if (!arg.equals("-1") && !arg.equals("0") && !arg.equals("1")) {
            getErrPrintWriter().println("Error: a config value of [-1, 0, 1] must be provided as an argument for supportsNonResizableMultiWindow");
            return -1;
        }
        int configValue = Integer.parseInt(arg);
        synchronized (this.mInternal.mAtmService.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                this.mInternal.mAtmService.mSupportsNonResizableMultiWindow = configValue;
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
        return 0;
    }

    private int runSetRespectsActivityMinWidthHeightMultiWindow() {
        String arg = getNextArg();
        if (!arg.equals("-1") && !arg.equals("0") && !arg.equals("1")) {
            getErrPrintWriter().println("Error: a config value of [-1, 0, 1] must be provided as an argument for respectsActivityMinWidthHeightMultiWindow");
            return -1;
        }
        int configValue = Integer.parseInt(arg);
        synchronized (this.mInternal.mAtmService.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                this.mInternal.mAtmService.mRespectsActivityMinWidthHeightMultiWindow = configValue;
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
        return 0;
    }

    private int runGetMultiWindowConfig(PrintWriter pw) {
        synchronized (this.mInternal.mAtmService.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                pw.println("Supports non-resizable in multi window: " + this.mInternal.mAtmService.mSupportsNonResizableMultiWindow);
                pw.println("Respects activity min width/height in multi window: " + this.mInternal.mAtmService.mRespectsActivityMinWidthHeightMultiWindow);
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
        return 0;
    }

    private int runResetMultiWindowConfig() {
        int supportsNonResizable = this.mInternal.mContext.getResources().getInteger(17694956);
        int respectsActivityMinWidthHeight = this.mInternal.mContext.getResources().getInteger(17694926);
        synchronized (this.mInternal.mAtmService.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                this.mInternal.mAtmService.mSupportsNonResizableMultiWindow = supportsNonResizable;
                this.mInternal.mAtmService.mRespectsActivityMinWidthHeightMultiWindow = respectsActivityMinWidthHeight;
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
        return 0;
    }

    private int runReset(PrintWriter pw) throws RemoteException {
        int displayId = getDisplayId(getNextArg());
        this.mInterface.clearForcedDisplaySize(displayId);
        this.mInterface.clearForcedDisplayDensityForUser(displayId, -2);
        this.mInternal.setOverrideFoldedArea(new Rect());
        this.mInterface.setForcedDisplayScalingMode(displayId, 0);
        this.mInternal.thawDisplayRotation(displayId);
        this.mInterface.setFixedToUserRotation(displayId, 0);
        this.mInterface.setIgnoreOrientationRequest(displayId, false);
        runResetMultiWindowConfig();
        pw.println("Reset all settings for displayId=" + displayId);
        return 0;
    }

    public void onHelp() {
        PrintWriter pw = getOutPrintWriter();
        pw.println("Window manager (window) commands:");
        pw.println("  help");
        pw.println("      Print this help text.");
        pw.println("  size [reset|WxH|WdpxHdp] [-d DISPLAY_ID]");
        pw.println("    Return or override display size.");
        pw.println("    width and height in pixels unless suffixed with 'dp'.");
        pw.println("  density [reset|DENSITY] [-d DISPLAY_ID]");
        pw.println("    Return or override display density.");
        pw.println("  folded-area [reset|LEFT,TOP,RIGHT,BOTTOM]");
        pw.println("    Return or override folded area.");
        pw.println("  scaling [off|auto] [-d DISPLAY_ID]");
        pw.println("    Set display scaling mode.");
        pw.println("  dismiss-keyguard");
        pw.println("    Dismiss the keyguard, prompting user for auth if necessary.");
        pw.println("  disable-blur [true|1|false|0]");
        pw.println("  user-rotation [-d DISPLAY_ID] [free|lock] [rotation]");
        pw.println("    Print or set user rotation mode and user rotation.");
        pw.println("  dump-visible-window-views");
        pw.println("    Dumps the encoded view hierarchies of visible windows");
        pw.println("  fixed-to-user-rotation [-d DISPLAY_ID] [enabled|disabled|default]");
        pw.println("    Print or set rotating display for app requested orientation.");
        pw.println("  set-ignore-orientation-request [-d DISPLAY_ID] [true|1|false|0]");
        pw.println("  get-ignore-orientation-request [-d DISPLAY_ID] ");
        pw.println("    If app requested orientation should be ignored.");
        printMultiWindowConfigHelp(pw);
        pw.println("  reset [-d DISPLAY_ID]");
        pw.println("    Reset all override settings.");
        if (!Build.IS_USER) {
            pw.println("  tracing (start | stop)");
            pw.println("    Start or stop window tracing.");
            pw.println("  logging (start | stop | enable | disable | enable-text | disable-text)");
            pw.println("    Logging settings.");
        }
    }

    private void printMultiWindowConfigHelp(PrintWriter pw) {
        pw.println("  set-multi-window-config");
        pw.println("    Sets options to determine if activity should be shown in multi window:");
        pw.println("      --supportsNonResizable [configValue]");
        pw.println("        Whether the device supports non-resizable activity in multi window.");
        pw.println("        -1: The device doesn't support non-resizable in multi window.");
        pw.println("         0: The device supports non-resizable in multi window only if");
        pw.println("            this is a large screen device.");
        pw.println("         1: The device always supports non-resizable in multi window.");
        pw.println("      --respectsActivityMinWidthHeight [configValue]");
        pw.println("        Whether the device checks the activity min width/height to determine ");
        pw.println("        if it can be shown in multi window.");
        pw.println("        -1: The device ignores the activity min width/height when determining");
        pw.println("            if it can be shown in multi window.");
        pw.println("         0: If this is a small screen, the device compares the activity min");
        pw.println("            width/height with the min multi window modes dimensions");
        pw.println("            the device supports to determine if the activity can be shown in");
        pw.println("            multi window.");
        pw.println("         1: The device always compare the activity min width/height with the");
        pw.println("            min multi window dimensions the device supports to determine if");
        pw.println("            the activity can be shown in multi window.");
        pw.println("  get-multi-window-config");
        pw.println("    Prints values of the multi window config options.");
        pw.println("  reset-multi-window-config");
        pw.println("    Resets overrides to default values of the multi window config options.");
    }
}
