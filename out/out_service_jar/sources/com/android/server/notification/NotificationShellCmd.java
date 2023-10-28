package com.android.server.notification;

import android.app.ActivityManager;
import android.app.INotificationManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.PendingIntent;
import android.app.Person;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ParceledListSlice;
import android.content.res.Resources;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.hardware.audio.common.V2_0.AudioFormat;
import android.net.Uri;
import android.net.util.NetworkConstants;
import android.os.Binder;
import android.os.RemoteException;
import android.os.ShellCommand;
import android.os.UserHandle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Slog;
import com.android.server.am.HostingRecord;
import com.android.server.pm.PackageManagerService;
import com.android.server.slice.SliceClientPermissions;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Date;
/* loaded from: classes2.dex */
public class NotificationShellCmd extends ShellCommand {
    public static final String CHANNEL_ID = "shell_cmd";
    public static final int CHANNEL_IMP = 3;
    public static final String CHANNEL_NAME = "Shell command";
    public static final int NOTIFICATION_ID = 2020;
    private static final String NOTIFY_USAGE = "usage: cmd notification post [flags] <tag> <text>\n\nflags:\n  -h|--help\n  -v|--verbose\n  -t|--title <text>\n  -i|--icon <iconspec>\n  -I|--large-icon <iconspec>\n  -S|--style <style> [styleargs]\n  -c|--content-intent <intentspec>\n\nstyles: (default none)\n  bigtext\n  bigpicture --picture <iconspec>\n  inbox --line <text> --line <text> ...\n  messaging --conversation <title> --message <who>:<text> ...\n  media\n\nan <iconspec> is one of\n  file:///data/local/tmp/<img.png>\n  content://<provider>/<path>\n  @[<package>:]drawable/<img>\n  data:base64,<B64DATA==>\n\nan <intentspec> is (broadcast|service|activity) <args>\n  <args> are as described in `am start`";
    private static final String TAG = "NotifShellCmd";
    private static final String USAGE = "usage: cmd notification SUBCMD [args]\n\nSUBCMDs:\n  allow_listener COMPONENT [user_id (current user if not specified)]\n  disallow_listener COMPONENT [user_id (current user if not specified)]\n  allow_assistant COMPONENT [user_id (current user if not specified)]\n  remove_assistant COMPONENT [user_id (current user if not specified)]\n  set_dnd [on|none (same as on)|priority|alarms|all|off (same as all)]  allow_dnd PACKAGE [user_id (current user if not specified)]\n  disallow_dnd PACKAGE [user_id (current user if not specified)]\n  reset_assistant_user_set [user_id (current user if not specified)]\n  get_approved_assistant [user_id (current user if not specified)]\n  post [--help | flags] TAG TEXT\n  set_bubbles PACKAGE PREFERENCE (0=none 1=all 2=selected) [user_id (current user if not specified)]\n  set_bubbles_channel PACKAGE CHANNEL_ID ALLOW [user_id (current user if not specified)]\n  list\n  get <notification-key>\n  snooze --for <msec> <notification-key>\n  unsnooze <notification-key>\n";
    private final INotificationManager mBinderService;
    private NotificationChannel mChannel;
    private final NotificationManagerService mDirectService;
    private final PackageManager mPm;

    public NotificationShellCmd(NotificationManagerService service) {
        this.mDirectService = service;
        this.mBinderService = service.getBinderService();
        this.mPm = service.getContext().getPackageManager();
    }

    protected boolean checkShellCommandPermission(int callingUid) {
        return callingUid == 0 || callingUid == 2000;
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    public int onCommand(String cmd) {
        long identity;
        char c;
        char c2;
        char c3;
        String key;
        long duration;
        String key2;
        if (cmd == null) {
            return handleDefaultCommands(cmd);
        }
        String callingPackage = null;
        int callingUid = Binder.getCallingUid();
        long identity2 = Binder.clearCallingIdentity();
        if (callingUid == 0) {
            callingPackage = "root";
        } else {
            try {
                String[] packages = this.mPm.getPackagesForUid(callingUid);
                if (packages != null && packages.length > 0) {
                    callingPackage = packages[0];
                }
            } catch (Exception e) {
                try {
                    Slog.e(TAG, "failed to get caller pkg", e);
                } catch (Throwable th) {
                    th = th;
                    identity = identity2;
                    Binder.restoreCallingIdentity(identity);
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
                identity = identity2;
                Binder.restoreCallingIdentity(identity);
                throw th;
            }
        }
        Binder.restoreCallingIdentity(identity2);
        PrintWriter pw = getOutPrintWriter();
        if (!checkShellCommandPermission(callingUid)) {
            Slog.e(TAG, "error: permission denied: callingUid=" + callingUid + " callingPackage=" + callingPackage);
            pw.println("error: permission denied: callingUid=" + callingUid + " callingPackage=" + callingPackage);
            return 255;
        }
        try {
            String replace = cmd.replace('-', '_');
            try {
                switch (replace.hashCode()) {
                    case -2056114370:
                        if (replace.equals("snoozed")) {
                            c = 15;
                            break;
                        }
                        c = 65535;
                        break;
                    case -1736066994:
                        if (replace.equals("set_bubbles_channel")) {
                            c = '\n';
                            break;
                        }
                        c = 65535;
                        break;
                    case -1325770982:
                        if (replace.equals("disallow_assistant")) {
                            c = 6;
                            break;
                        }
                        c = 65535;
                        break;
                    case -1039689911:
                        if (replace.equals("notify")) {
                            c = '\f';
                            break;
                        }
                        c = 65535;
                        break;
                    case -897610266:
                        if (replace.equals("snooze")) {
                            c = 17;
                            break;
                        }
                        c = 65535;
                        break;
                    case -432999190:
                        if (replace.equals("allow_listener")) {
                            c = 3;
                            break;
                        }
                        c = 65535;
                        break;
                    case -429832618:
                        if (replace.equals("disallow_dnd")) {
                            c = 2;
                            break;
                        }
                        c = 65535;
                        break;
                    case -414550305:
                        if (replace.equals("get_approved_assistant")) {
                            c = '\b';
                            break;
                        }
                        c = 65535;
                        break;
                    case -11106881:
                        if (replace.equals("unsnooze")) {
                            c = 16;
                            break;
                        }
                        c = 65535;
                        break;
                    case 102230:
                        if (replace.equals("get")) {
                            c = 14;
                            break;
                        }
                        c = 65535;
                        break;
                    case 3322014:
                        if (replace.equals("list")) {
                            c = '\r';
                            break;
                        }
                        c = 65535;
                        break;
                    case 3446944:
                        if (replace.equals("post")) {
                            c = 11;
                            break;
                        }
                        c = 65535;
                        break;
                    case 212123274:
                        if (replace.equals("set_bubbles")) {
                            c = '\t';
                            break;
                        }
                        c = 65535;
                        break;
                    case 372345636:
                        if (replace.equals("allow_dnd")) {
                            c = 1;
                            break;
                        }
                        c = 65535;
                        break;
                    case 683492127:
                        if (replace.equals("reset_assistant_user_set")) {
                            c = 7;
                            break;
                        }
                        c = 65535;
                        break;
                    case 1257269496:
                        if (replace.equals("disallow_listener")) {
                            c = 4;
                            break;
                        }
                        c = 65535;
                        break;
                    case 1985310653:
                        if (replace.equals("set_dnd")) {
                            c = 0;
                            break;
                        }
                        c = 65535;
                        break;
                    case 2110474600:
                        if (replace.equals("allow_assistant")) {
                            c = 5;
                            break;
                        }
                        c = 65535;
                        break;
                    default:
                        c = 65535;
                        break;
                }
            } catch (Exception e2) {
                e = e2;
            }
        } catch (Exception e3) {
            e = e3;
        }
        try {
            switch (c) {
                case 0:
                    String mode = getNextArgRequired();
                    int interruptionFilter = 0;
                    switch (mode.hashCode()) {
                        case -1415196606:
                            if (mode.equals("alarms")) {
                                c2 = 3;
                                break;
                            }
                            c2 = 65535;
                            break;
                        case -1165461084:
                            if (mode.equals("priority")) {
                                c2 = 2;
                                break;
                            }
                            c2 = 65535;
                            break;
                        case 3551:
                            if (mode.equals("on")) {
                                c2 = 1;
                                break;
                            }
                            c2 = 65535;
                            break;
                        case 96673:
                            if (mode.equals("all")) {
                                c2 = 4;
                                break;
                            }
                            c2 = 65535;
                            break;
                        case 109935:
                            if (mode.equals("off")) {
                                c2 = 5;
                                break;
                            }
                            c2 = 65535;
                            break;
                        case 3387192:
                            if (mode.equals("none")) {
                                c2 = 0;
                                break;
                            }
                            c2 = 65535;
                            break;
                        default:
                            c2 = 65535;
                            break;
                    }
                    switch (c2) {
                        case 0:
                        case 1:
                            interruptionFilter = 3;
                            break;
                        case 2:
                            interruptionFilter = 2;
                            break;
                        case 3:
                            interruptionFilter = 4;
                            break;
                        case 4:
                        case 5:
                            interruptionFilter = 1;
                            break;
                    }
                    int filter = interruptionFilter;
                    this.mBinderService.setInterruptionFilter(callingPackage, filter);
                    return 0;
                case 1:
                    String packageName = getNextArgRequired();
                    int userId = ActivityManager.getCurrentUser();
                    if (peekNextArg() != null) {
                        userId = Integer.parseInt(getNextArgRequired());
                    }
                    this.mBinderService.setNotificationPolicyAccessGrantedForUser(packageName, userId, true);
                    return 0;
                case 2:
                    String packageName2 = getNextArgRequired();
                    int userId2 = ActivityManager.getCurrentUser();
                    if (peekNextArg() != null) {
                        userId2 = Integer.parseInt(getNextArgRequired());
                    }
                    this.mBinderService.setNotificationPolicyAccessGrantedForUser(packageName2, userId2, false);
                    return 0;
                case 3:
                    ComponentName cn = ComponentName.unflattenFromString(getNextArgRequired());
                    if (cn == null) {
                        pw.println("Invalid listener - must be a ComponentName");
                        return -1;
                    }
                    int userId3 = ActivityManager.getCurrentUser();
                    if (peekNextArg() != null) {
                        userId3 = Integer.parseInt(getNextArgRequired());
                    }
                    this.mBinderService.setNotificationListenerAccessGrantedForUser(cn, userId3, true, true);
                    return 0;
                case 4:
                    ComponentName cn2 = ComponentName.unflattenFromString(getNextArgRequired());
                    if (cn2 == null) {
                        pw.println("Invalid listener - must be a ComponentName");
                        return -1;
                    }
                    int userId4 = ActivityManager.getCurrentUser();
                    if (peekNextArg() != null) {
                        userId4 = Integer.parseInt(getNextArgRequired());
                    }
                    this.mBinderService.setNotificationListenerAccessGrantedForUser(cn2, userId4, false, true);
                    return 0;
                case 5:
                    ComponentName cn3 = ComponentName.unflattenFromString(getNextArgRequired());
                    if (cn3 == null) {
                        pw.println("Invalid assistant - must be a ComponentName");
                        return -1;
                    }
                    int userId5 = ActivityManager.getCurrentUser();
                    if (peekNextArg() != null) {
                        userId5 = Integer.parseInt(getNextArgRequired());
                    }
                    this.mBinderService.setNotificationAssistantAccessGrantedForUser(cn3, userId5, true);
                    return 0;
                case 6:
                    ComponentName cn4 = ComponentName.unflattenFromString(getNextArgRequired());
                    if (cn4 == null) {
                        pw.println("Invalid assistant - must be a ComponentName");
                        return -1;
                    }
                    int userId6 = ActivityManager.getCurrentUser();
                    if (peekNextArg() != null) {
                        userId6 = Integer.parseInt(getNextArgRequired());
                    }
                    this.mBinderService.setNotificationAssistantAccessGrantedForUser(cn4, userId6, false);
                    return 0;
                case 7:
                    int userId7 = ActivityManager.getCurrentUser();
                    if (peekNextArg() != null) {
                        userId7 = Integer.parseInt(getNextArgRequired());
                    }
                    this.mDirectService.resetAssistantUserSet(userId7);
                    return 0;
                case '\b':
                    int userId8 = ActivityManager.getCurrentUser();
                    if (peekNextArg() != null) {
                        userId8 = Integer.parseInt(getNextArgRequired());
                    }
                    ComponentName approvedAssistant = this.mDirectService.getApprovedAssistant(userId8);
                    if (approvedAssistant == null) {
                        pw.println("null");
                        return 0;
                    }
                    pw.println(approvedAssistant.flattenToString());
                    return 0;
                case '\t':
                    String packageName3 = getNextArgRequired();
                    int preference = Integer.parseInt(getNextArgRequired());
                    if (preference <= 3 && preference >= 0) {
                        int userId9 = ActivityManager.getCurrentUser();
                        if (peekNextArg() != null) {
                            userId9 = Integer.parseInt(getNextArgRequired());
                        }
                        int appUid = UserHandle.getUid(userId9, this.mPm.getPackageUid(packageName3, 0));
                        this.mBinderService.setBubblesAllowed(packageName3, appUid, preference);
                        return 0;
                    }
                    pw.println("Invalid preference - must be between 0-3 (0=none 1=all 2=selected)");
                    return -1;
                case '\n':
                    String packageName4 = getNextArgRequired();
                    String channelId = getNextArgRequired();
                    boolean allow = Boolean.parseBoolean(getNextArgRequired());
                    int userId10 = ActivityManager.getCurrentUser();
                    if (peekNextArg() != null) {
                        userId10 = Integer.parseInt(getNextArgRequired());
                    }
                    NotificationChannel channel = this.mBinderService.getNotificationChannel(callingPackage, userId10, packageName4, channelId);
                    channel.setAllowBubbles(allow);
                    int appUid2 = UserHandle.getUid(userId10, this.mPm.getPackageUid(packageName4, 0));
                    this.mBinderService.updateNotificationChannelForPackage(packageName4, appUid2, channel);
                    return 0;
                case 11:
                case '\f':
                    doNotify(pw, callingPackage, callingUid);
                    return 0;
                case '\r':
                    for (String key3 : this.mDirectService.mNotificationsByKey.keySet()) {
                        pw.println(key3);
                    }
                    return 0;
                case 14:
                    String key4 = getNextArgRequired();
                    NotificationRecord nr = this.mDirectService.getNotificationRecord(key4);
                    if (nr != null) {
                        nr.dump(pw, "", this.mDirectService.getContext(), false);
                        return 0;
                    }
                    pw.println("error: no active notification matching key: " + key4);
                    return 1;
                case 15:
                    new StringBuilder();
                    SnoozeHelper sh = this.mDirectService.mSnoozeHelper;
                    for (NotificationRecord nr2 : sh.getSnoozed()) {
                        String pkg = nr2.getSbn().getPackageName();
                        String key5 = nr2.getKey();
                        pw.println(key5 + " snoozed, time=" + sh.getSnoozeTimeForUnpostedNotification(nr2.getUserId(), pkg, key5) + " context=" + sh.getSnoozeContextForUnpostedNotification(nr2.getUserId(), pkg, key5));
                    }
                    return 0;
                case 16:
                    boolean mute = false;
                    String key6 = getNextArgRequired();
                    if ("--mute".equals(key6)) {
                        mute = true;
                        key6 = getNextArgRequired();
                    }
                    if (this.mDirectService.mSnoozeHelper.getNotification(key6) != null) {
                        pw.println("unsnoozing: " + key6);
                        this.mDirectService.unsnoozeNotificationInt(key6, null, mute);
                        return 0;
                    }
                    pw.println("error: no snoozed otification matching key: " + key6);
                    return 1;
                case 17:
                    String subflag = getNextArg();
                    if (subflag == null) {
                        subflag = "help";
                    } else if (subflag.startsWith("--")) {
                        subflag = subflag.substring(2);
                    }
                    String flagarg = getNextArg();
                    String key7 = getNextArg();
                    if (key7 == null) {
                        subflag = "help";
                    }
                    String criterion = null;
                    switch (subflag.hashCode()) {
                        case -1992012396:
                            if (subflag.equals("duration")) {
                                c3 = 5;
                                break;
                            }
                            c3 = 65535;
                            break;
                        case -861311717:
                            if (subflag.equals("condition")) {
                                c3 = 1;
                                break;
                            }
                            c3 = 65535;
                            break;
                        case 101577:
                            if (subflag.equals("for")) {
                                c3 = 4;
                                break;
                            }
                            c3 = 65535;
                            break;
                        case 111443806:
                            if (subflag.equals("until")) {
                                c3 = 3;
                                break;
                            }
                            c3 = 65535;
                            break;
                        case 383913633:
                            if (subflag.equals("criterion")) {
                                c3 = 2;
                                break;
                            }
                            c3 = 65535;
                            break;
                        case 951530927:
                            if (subflag.equals("context")) {
                                c3 = 0;
                                break;
                            }
                            c3 = 65535;
                            break;
                        default:
                            c3 = 65535;
                            break;
                    }
                    switch (c3) {
                        case 0:
                        case 1:
                        case 2:
                            criterion = flagarg;
                            key = key7;
                            duration = 0;
                            break;
                        case 3:
                        case 4:
                        case 5:
                            long duration2 = Long.parseLong(flagarg);
                            key = key7;
                            duration = duration2;
                            break;
                        default:
                            pw.println("usage: cmd notification snooze (--for <msec> | --context <snooze-criterion-id>) <key>");
                            return 1;
                    }
                    if (duration <= 0 && criterion == null) {
                        pw.println("error: invalid value for --" + subflag + ": " + flagarg);
                        return 1;
                    }
                    ShellNls nls = new ShellNls();
                    nls.registerAsSystemService(this.mDirectService.getContext(), new ComponentName(nls.getClass().getPackageName(), nls.getClass().getName()), ActivityManager.getCurrentUser());
                    if (!waitForBind(nls)) {
                        pw.println("error: could not bind a listener in time");
                        return 1;
                    }
                    if (duration > 0) {
                        Object[] objArr = new Object[2];
                        objArr[0] = key;
                        objArr[1] = new Date(System.currentTimeMillis() + duration);
                        pw.println(String.format("snoozing <%s> until time: %s", objArr));
                        key2 = key;
                        nls.snoozeNotification(key2, duration);
                    } else {
                        key2 = key;
                        pw.println(String.format("snoozing <%s> until criterion: %s", key2, criterion));
                        nls.snoozeNotification(key2, criterion);
                    }
                    waitForSnooze(nls, key2);
                    nls.unregisterAsSystemService();
                    waitForUnbind(nls);
                    return 0;
                default:
                    return handleDefaultCommands(cmd);
            }
        } catch (Exception e4) {
            e = e4;
            pw.println("Error occurred. Check logcat for details. " + e.getMessage());
            Slog.e(NotificationManagerService.TAG, "Error running shell command", e);
            return 0;
        }
    }

    void ensureChannel(String callingPackage, int callingUid) throws RemoteException {
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, 3);
        this.mBinderService.createNotificationChannels(callingPackage, new ParceledListSlice(Collections.singletonList(channel)));
        Slog.v(NotificationManagerService.TAG, "created channel: " + this.mBinderService.getNotificationChannel(callingPackage, UserHandle.getUserId(callingUid), callingPackage, CHANNEL_ID));
    }

    Icon parseIcon(Resources res, String encoded) throws IllegalArgumentException {
        if (TextUtils.isEmpty(encoded)) {
            return null;
        }
        if (encoded.startsWith(SliceClientPermissions.SliceAuthority.DELIMITER)) {
            encoded = "file://" + encoded;
        }
        if (encoded.startsWith("http:") || encoded.startsWith("https:") || encoded.startsWith("content:") || encoded.startsWith("file:") || encoded.startsWith("android.resource:")) {
            Uri asUri = Uri.parse(encoded);
            return Icon.createWithContentUri(asUri);
        }
        if (encoded.startsWith("@")) {
            int resid = res.getIdentifier(encoded.substring(1), "drawable", PackageManagerService.PLATFORM_PACKAGE_NAME);
            if (resid != 0) {
                return Icon.createWithResource(res, resid);
            }
        } else if (encoded.startsWith("data:")) {
            byte[] bits = Base64.decode(encoded.substring(encoded.indexOf(44) + 1), 0);
            return Icon.createWithData(bits, 0, bits.length);
        }
        return null;
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Removed duplicated region for block: B:201:0x0490  */
    /* JADX WARN: Removed duplicated region for block: B:242:0x0478 A[SYNTHETIC] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private int doNotify(PrintWriter pw, String callingPackage, int callingUid) throws RemoteException, URISyntaxException {
        char c;
        Icon smallIcon;
        Notification.MessagingStyle messagingStyle;
        Notification.InboxStyle inboxStyle;
        Icon icon;
        char c2;
        PendingIntent pi;
        char c3;
        Context context = this.mDirectService.getContext();
        Resources res = context.getResources();
        Notification.Builder builder = new Notification.Builder(context, CHANNEL_ID);
        Notification.MessagingStyle messagingStyle2 = null;
        boolean verbose = false;
        Notification.BigPictureStyle bigPictureStyle = null;
        Notification.BigTextStyle bigTextStyle = null;
        Icon smallIcon2 = null;
        Notification.InboxStyle inboxStyle2 = null;
        while (true) {
            String opt = getNextOption();
            if (opt != null) {
                boolean large = false;
                switch (opt.hashCode()) {
                    case -1954060697:
                        if (opt.equals("--message")) {
                            c = 25;
                            break;
                        }
                        c = 65535;
                        break;
                    case -1613915119:
                        if (opt.equals("--style")) {
                            c = 19;
                            break;
                        }
                        c = 65535;
                        break;
                    case -1613324104:
                        if (opt.equals("--title")) {
                            c = 3;
                            break;
                        }
                        c = 65535;
                        break;
                    case -1210178960:
                        if (opt.equals("content-intent")) {
                            c = 15;
                            break;
                        }
                        c = 65535;
                        break;
                    case -1183762788:
                        if (opt.equals("intent")) {
                            c = 17;
                            break;
                        }
                        c = 65535;
                        break;
                    case -853380573:
                        if (opt.equals("--conversation")) {
                            c = 26;
                            break;
                        }
                        c = 65535;
                        break;
                    case -45879957:
                        if (opt.equals("--large-icon")) {
                            c = 6;
                            break;
                        }
                        c = 65535;
                        break;
                    case 1468:
                        if (opt.equals("-I")) {
                            c = 5;
                            break;
                        }
                        c = 65535;
                        break;
                    case 1478:
                        if (opt.equals("-S")) {
                            c = 18;
                            break;
                        }
                        c = 65535;
                        break;
                    case 1494:
                        if (opt.equals("-c")) {
                            c = '\r';
                            break;
                        }
                        c = 65535;
                        break;
                    case 1499:
                        if (opt.equals("-h")) {
                            c = 27;
                            break;
                        }
                        c = 65535;
                        break;
                    case NetworkConstants.ETHER_MTU /* 1500 */:
                        if (opt.equals("-i")) {
                            c = '\n';
                            break;
                        }
                        c = 65535;
                        break;
                    case 1511:
                        if (opt.equals("-t")) {
                            c = 2;
                            break;
                        }
                        c = 65535;
                        break;
                    case 1513:
                        if (opt.equals("-v")) {
                            c = 0;
                            break;
                        }
                        c = 65535;
                        break;
                    case 3226745:
                        if (opt.equals("icon")) {
                            c = '\f';
                            break;
                        }
                        c = 65535;
                        break;
                    case 43017097:
                        if (opt.equals("--wtf")) {
                            c = 29;
                            break;
                        }
                        c = 65535;
                        break;
                    case 110371416:
                        if (opt.equals("title")) {
                            c = 4;
                            break;
                        }
                        c = 65535;
                        break;
                    case 704999290:
                        if (opt.equals("--big-text")) {
                            c = 22;
                            break;
                        }
                        c = 65535;
                        break;
                    case 705941520:
                        if (opt.equals("--content-intent")) {
                            c = 14;
                            break;
                        }
                        c = 65535;
                        break;
                    case 758833716:
                        if (opt.equals("largeicon")) {
                            c = '\b';
                            break;
                        }
                        c = 65535;
                        break;
                    case 808239966:
                        if (opt.equals("--picture")) {
                            c = 23;
                            break;
                        }
                        c = 65535;
                        break;
                    case 1216250940:
                        if (opt.equals("--intent")) {
                            c = 16;
                            break;
                        }
                        c = 65535;
                        break;
                    case 1247228052:
                        if (opt.equals("--largeicon")) {
                            c = 7;
                            break;
                        }
                        c = 65535;
                        break;
                    case 1270815917:
                        if (opt.equals("--bigText")) {
                            c = 20;
                            break;
                        }
                        c = 65535;
                        break;
                    case 1271769229:
                        if (opt.equals("--bigtext")) {
                            c = 21;
                            break;
                        }
                        c = 65535;
                        break;
                    case 1333069025:
                        if (opt.equals("--help")) {
                            c = 28;
                            break;
                        }
                        c = 65535;
                        break;
                    case 1333096985:
                        if (opt.equals("--icon")) {
                            c = 11;
                            break;
                        }
                        c = 65535;
                        break;
                    case 1333192084:
                        if (opt.equals("--line")) {
                            c = 24;
                            break;
                        }
                        c = 65535;
                        break;
                    case 1737088994:
                        if (opt.equals("--verbose")) {
                            c = 1;
                            break;
                        }
                        c = 65535;
                        break;
                    case 1993764811:
                        if (opt.equals("large-icon")) {
                            c = '\t';
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
                        verbose = true;
                        break;
                    case 2:
                    case 3:
                    case 4:
                        smallIcon = smallIcon2;
                        messagingStyle = messagingStyle2;
                        inboxStyle = inboxStyle2;
                        builder.setContentTitle(getNextArgRequired());
                        messagingStyle2 = messagingStyle;
                        inboxStyle2 = inboxStyle;
                        smallIcon2 = smallIcon;
                        break;
                    case 5:
                    case 6:
                    case 7:
                    case '\b':
                    case '\t':
                        smallIcon = smallIcon2;
                        messagingStyle = messagingStyle2;
                        inboxStyle = inboxStyle2;
                        large = true;
                        String iconSpec = getNextArgRequired();
                        icon = parseIcon(res, iconSpec);
                        if (icon != null) {
                            pw.println("error: invalid icon: " + iconSpec);
                            return -1;
                        } else if (large) {
                            builder.setLargeIcon(icon);
                            messagingStyle2 = messagingStyle;
                            inboxStyle2 = inboxStyle;
                            smallIcon2 = smallIcon;
                            break;
                        } else {
                            smallIcon2 = icon;
                            messagingStyle2 = messagingStyle;
                            inboxStyle2 = inboxStyle;
                            break;
                        }
                    case '\n':
                    case 11:
                    case '\f':
                        smallIcon = smallIcon2;
                        messagingStyle = messagingStyle2;
                        inboxStyle = inboxStyle2;
                        String iconSpec2 = getNextArgRequired();
                        icon = parseIcon(res, iconSpec2);
                        if (icon != null) {
                        }
                        break;
                    case '\r':
                    case 14:
                    case 15:
                    case 16:
                    case 17:
                        Icon smallIcon3 = smallIcon2;
                        String intentKind = null;
                        String peekNextArg = peekNextArg();
                        switch (peekNextArg.hashCode()) {
                            case -1655966961:
                                if (peekNextArg.equals(HostingRecord.HOSTING_TYPE_ACTIVITY)) {
                                    c2 = 2;
                                    break;
                                }
                                c2 = 65535;
                                break;
                            case -1618876223:
                                if (peekNextArg.equals("broadcast")) {
                                    c2 = 0;
                                    break;
                                }
                                c2 = 65535;
                                break;
                            case 1984153269:
                                if (peekNextArg.equals(HostingRecord.HOSTING_TYPE_SERVICE)) {
                                    c2 = 1;
                                    break;
                                }
                                c2 = 65535;
                                break;
                            default:
                                c2 = 65535;
                                break;
                        }
                        switch (c2) {
                            case 0:
                            case 1:
                            case 2:
                                intentKind = getNextArg();
                                break;
                        }
                        Intent intent = Intent.parseCommandArgs(this, null);
                        if (intent.getData() != null) {
                            messagingStyle = messagingStyle2;
                            inboxStyle = inboxStyle2;
                        } else {
                            messagingStyle = messagingStyle2;
                            inboxStyle = inboxStyle2;
                            intent.setData(Uri.parse("xyz:" + System.currentTimeMillis()));
                        }
                        if ("broadcast".equals(intentKind)) {
                            pi = PendingIntent.getBroadcastAsUser(context, 0, intent, AudioFormat.E_AC3, UserHandle.CURRENT);
                            smallIcon = smallIcon3;
                        } else if (HostingRecord.HOSTING_TYPE_SERVICE.equals(intentKind)) {
                            pi = PendingIntent.getService(context, 0, intent, AudioFormat.E_AC3);
                            smallIcon = smallIcon3;
                        } else {
                            smallIcon = smallIcon3;
                            pi = PendingIntent.getActivityAsUser(context, 0, intent, AudioFormat.E_AC3, null, UserHandle.CURRENT);
                        }
                        builder.setContentIntent(pi);
                        messagingStyle2 = messagingStyle;
                        inboxStyle2 = inboxStyle;
                        smallIcon2 = smallIcon;
                        break;
                    case 18:
                    case 19:
                        Icon smallIcon4 = smallIcon2;
                        String styleSpec = getNextArgRequired().toLowerCase();
                        switch (styleSpec.hashCode()) {
                            case -1440008444:
                                if (styleSpec.equals("messaging")) {
                                    c3 = 3;
                                    break;
                                }
                                c3 = 65535;
                                break;
                            case -114212307:
                                if (styleSpec.equals("bigtext")) {
                                    c3 = 0;
                                    break;
                                }
                                c3 = 65535;
                                break;
                            case -44548098:
                                if (styleSpec.equals("bigpicture")) {
                                    c3 = 1;
                                    break;
                                }
                                c3 = 65535;
                                break;
                            case 100344454:
                                if (styleSpec.equals("inbox")) {
                                    c3 = 2;
                                    break;
                                }
                                c3 = 65535;
                                break;
                            case 103772132:
                                if (styleSpec.equals("media")) {
                                    c3 = 4;
                                    break;
                                }
                                c3 = 65535;
                                break;
                            default:
                                c3 = 65535;
                                break;
                        }
                        switch (c3) {
                            case 0:
                                bigTextStyle = new Notification.BigTextStyle();
                                builder.setStyle(bigTextStyle);
                                break;
                            case 1:
                                bigPictureStyle = new Notification.BigPictureStyle();
                                builder.setStyle(bigPictureStyle);
                                break;
                            case 2:
                                inboxStyle2 = new Notification.InboxStyle();
                                builder.setStyle(inboxStyle2);
                                break;
                            case 3:
                                String name = "You";
                                if ("--user".equals(peekNextArg())) {
                                    getNextArg();
                                    name = getNextArgRequired();
                                }
                                messagingStyle2 = new Notification.MessagingStyle(new Person.Builder().setName(name).build());
                                builder.setStyle(messagingStyle2);
                                break;
                            case 4:
                                Notification.MediaStyle mediaStyle = new Notification.MediaStyle();
                                builder.setStyle(mediaStyle);
                                break;
                            default:
                                throw new IllegalArgumentException("unrecognized notification style: " + styleSpec);
                        }
                        smallIcon2 = smallIcon4;
                        break;
                    case 20:
                    case 21:
                    case 22:
                        Icon smallIcon5 = smallIcon2;
                        if (bigTextStyle == null) {
                            throw new IllegalArgumentException("--bigtext requires --style bigtext");
                        }
                        bigTextStyle.bigText(getNextArgRequired());
                        messagingStyle = messagingStyle2;
                        inboxStyle = inboxStyle2;
                        smallIcon = smallIcon5;
                        messagingStyle2 = messagingStyle;
                        inboxStyle2 = inboxStyle;
                        smallIcon2 = smallIcon;
                        break;
                    case 23:
                        Icon smallIcon6 = smallIcon2;
                        if (bigPictureStyle == null) {
                            throw new IllegalArgumentException("--picture requires --style bigpicture");
                        }
                        String pictureSpec = getNextArgRequired();
                        Icon pictureAsIcon = parseIcon(res, pictureSpec);
                        if (pictureAsIcon != null) {
                            Drawable d = pictureAsIcon.loadDrawable(context);
                            if (d instanceof BitmapDrawable) {
                                bigPictureStyle.bigPicture(((BitmapDrawable) d).getBitmap());
                                messagingStyle = messagingStyle2;
                                inboxStyle = inboxStyle2;
                                smallIcon = smallIcon6;
                                messagingStyle2 = messagingStyle;
                                inboxStyle2 = inboxStyle;
                                smallIcon2 = smallIcon;
                                break;
                            } else {
                                throw new IllegalArgumentException("not a bitmap: " + pictureSpec);
                            }
                        } else {
                            throw new IllegalArgumentException("bad picture spec: " + pictureSpec);
                        }
                    case 24:
                        Icon smallIcon7 = smallIcon2;
                        if (inboxStyle2 == null) {
                            throw new IllegalArgumentException("--line requires --style inbox");
                        }
                        inboxStyle2.addLine(getNextArgRequired());
                        messagingStyle = messagingStyle2;
                        inboxStyle = inboxStyle2;
                        smallIcon = smallIcon7;
                        messagingStyle2 = messagingStyle;
                        inboxStyle2 = inboxStyle;
                        smallIcon2 = smallIcon;
                        break;
                    case 25:
                        if (messagingStyle2 != null) {
                            String arg = getNextArgRequired();
                            String[] parts = arg.split(":", 2);
                            if (parts.length > 1) {
                                messagingStyle2.addMessage(parts[1], System.currentTimeMillis(), parts[0]);
                                messagingStyle = messagingStyle2;
                                inboxStyle = inboxStyle2;
                                smallIcon = smallIcon2;
                            } else {
                                messagingStyle2.addMessage(parts[0], System.currentTimeMillis(), new String[]{messagingStyle2.getUserDisplayName().toString(), "Them"}[messagingStyle2.getMessages().size() % 2]);
                                messagingStyle = messagingStyle2;
                                inboxStyle = inboxStyle2;
                                smallIcon = smallIcon2;
                            }
                            messagingStyle2 = messagingStyle;
                            inboxStyle2 = inboxStyle;
                            smallIcon2 = smallIcon;
                            break;
                        } else {
                            throw new IllegalArgumentException("--message requires --style messaging");
                        }
                    case 26:
                        if (messagingStyle2 == null) {
                            throw new IllegalArgumentException("--conversation requires --style messaging");
                        }
                        messagingStyle2.setConversationTitle(getNextArgRequired());
                        smallIcon = smallIcon2;
                        messagingStyle = messagingStyle2;
                        inboxStyle = inboxStyle2;
                        messagingStyle2 = messagingStyle;
                        inboxStyle2 = inboxStyle;
                        smallIcon2 = smallIcon;
                        break;
                    default:
                        pw.println(NOTIFY_USAGE);
                        return 0;
                }
            } else {
                Icon smallIcon8 = smallIcon2;
                String tag = getNextArg();
                String text = getNextArg();
                if (tag != null && text != null) {
                    builder.setContentText(text);
                    if (smallIcon8 == null) {
                        builder.setSmallIcon(17301623);
                    } else {
                        builder.setSmallIcon(smallIcon8);
                    }
                    ensureChannel(callingPackage, callingUid);
                    Notification n = builder.build();
                    pw.println("posting:\n  " + n);
                    Slog.v("NotificationManager", "posting: " + n);
                    this.mBinderService.enqueueNotificationWithTag(callingPackage, callingPackage, tag, (int) NOTIFICATION_ID, n, UserHandle.getUserId(callingUid));
                    if (verbose) {
                        NotificationRecord nr = this.mDirectService.findNotificationLocked(callingPackage, tag, NOTIFICATION_ID, UserHandle.getUserId(callingUid));
                        NotificationRecord nr2 = nr;
                        int tries = 3;
                        while (true) {
                            int tries2 = tries - 1;
                            if (tries > 0 && nr2 == null) {
                                try {
                                    pw.println("waiting for notification to post...");
                                    Thread.sleep(500L);
                                } catch (InterruptedException e) {
                                }
                                nr2 = this.mDirectService.findNotificationLocked(callingPackage, tag, NOTIFICATION_ID, UserHandle.getUserId(callingUid));
                                tries = tries2;
                            }
                        }
                        if (nr2 == null) {
                            pw.println("warning: couldn't find notification after enqueueing");
                            return 0;
                        }
                        pw.println("posted: ");
                        nr2.dump(pw, "  ", context, false);
                        return 0;
                    }
                    return 0;
                }
                pw.println(NOTIFY_USAGE);
                return -1;
            }
        }
    }

    private void waitForSnooze(ShellNls nls, String key) {
        for (int i = 0; i < 20; i++) {
            StatusBarNotification[] sbns = nls.getSnoozedNotifications();
            for (StatusBarNotification sbn : sbns) {
                if (sbn.getKey().equals(key)) {
                    return;
                }
            }
            try {
                Thread.sleep(100L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private boolean waitForBind(ShellNls nls) {
        for (int i = 0; i < 20; i++) {
            if (nls.isConnected) {
                Slog.i(TAG, "Bound Shell NLS");
                return true;
            }
            try {
                Thread.sleep(100L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    private void waitForUnbind(ShellNls nls) {
        for (int i = 0; i < 10; i++) {
            if (!nls.isConnected) {
                Slog.i(TAG, "Unbound Shell NLS");
                return;
            }
            try {
                Thread.sleep(100L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void onHelp() {
        getOutPrintWriter().println(USAGE);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static class ShellNls extends NotificationListenerService {
        private static ShellNls sNotificationListenerInstance = null;
        boolean isConnected;

        private ShellNls() {
        }

        @Override // android.service.notification.NotificationListenerService
        public void onListenerConnected() {
            super.onListenerConnected();
            sNotificationListenerInstance = this;
            this.isConnected = true;
        }

        @Override // android.service.notification.NotificationListenerService
        public void onListenerDisconnected() {
            this.isConnected = false;
        }

        public static ShellNls getInstance() {
            return sNotificationListenerInstance;
        }
    }
}
