package com.android.server.inputmethod;

import android.app.ActivityManager;
import android.app.ActivityManagerInternal;
import android.app.AppGlobals;
import android.app.AppOpsManager;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.content.pm.PackageManagerInternal;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.Matrix;
import android.hardware.display.DisplayManagerInternal;
import android.hardware.input.InputManagerInternal;
import android.media.AudioManagerInternal;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.DeadObjectException;
import android.os.Debug;
import android.os.Handler;
import android.os.IBinder;
import android.os.LocaleList;
import android.os.Message;
import android.os.Parcel;
import android.os.Process;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ServiceManager;
import android.os.ShellCallback;
import android.os.ShellCommand;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.Trace;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.EventLog;
import android.util.IndentingPrintWriter;
import android.util.Pair;
import android.util.PrintWriterPrinter;
import android.util.Printer;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.util.proto.ProtoOutputStream;
import android.view.DisplayInfo;
import android.view.IWindowManager;
import android.view.InputChannel;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityManager;
import android.view.autofill.AutofillId;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InlineSuggestionsRequest;
import android.view.inputmethod.InputBinding;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodSubtype;
import android.window.ImeOnBackInvokedDispatcher;
import com.android.internal.content.PackageMonitor;
import com.android.internal.infra.AndroidFuture;
import com.android.internal.inputmethod.IAccessibilityInputMethodSession;
import com.android.internal.inputmethod.IInputContentUriToken;
import com.android.internal.inputmethod.IInputMethodPrivilegedOperations;
import com.android.internal.inputmethod.IRemoteAccessibilityInputConnection;
import com.android.internal.inputmethod.ImeTracing;
import com.android.internal.inputmethod.InputBindResult;
import com.android.internal.inputmethod.InputConnectionCommandHeader;
import com.android.internal.inputmethod.InputMethodDebug;
import com.android.internal.notification.SystemNotificationChannels;
import com.android.internal.os.SomeArgs;
import com.android.internal.os.TransferPipe;
import com.android.internal.util.ConcurrentUtils;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.FrameworkStatsLog;
import com.android.internal.util.FunctionalUtils;
import com.android.internal.view.IInlineSuggestionsRequestCallback;
import com.android.internal.view.IInlineSuggestionsResponseCallback;
import com.android.internal.view.IInputContext;
import com.android.internal.view.IInputMethodClient;
import com.android.internal.view.IInputMethodManager;
import com.android.internal.view.IInputMethodSession;
import com.android.internal.view.IInputSessionCallback;
import com.android.internal.view.InlineSuggestionsRequestInfo;
import com.android.server.AccessibilityManagerInternal;
import com.android.server.EventLogTags;
import com.android.server.LocalServices;
import com.android.server.ServiceThread;
import com.android.server.SystemServerInitThreadPool;
import com.android.server.SystemService;
import com.android.server.backup.BackupAgentTimeoutParameters;
import com.android.server.inputmethod.HandwritingModeController;
import com.android.server.inputmethod.InputMethodManagerInternal;
import com.android.server.inputmethod.InputMethodManagerService;
import com.android.server.inputmethod.InputMethodSubtypeSwitchingController;
import com.android.server.inputmethod.InputMethodUtils;
import com.android.server.pm.UserManagerInternal;
import com.android.server.pm.verify.domain.DomainVerificationLegacySettings;
import com.android.server.statusbar.StatusBarManagerService;
import com.android.server.timezonedetector.ServiceConfigAccessor;
import com.android.server.utils.PriorityDump;
import com.android.server.wm.ActivityTaskManagerInternal;
import com.android.server.wm.WindowManagerInternal;
import com.google.android.collect.Sets;
import com.transsion.griffin.Griffin;
import com.transsion.hubcore.server.inputmethod.ITranInputMethodManagerService;
import com.transsion.hubcore.server.wm.ITranWindowManagerService;
import defpackage.CompanionAppsPermissions;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.security.InvalidParameterException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Predicate;
/* loaded from: classes.dex */
public final class InputMethodManagerService extends IInputMethodManager.Stub implements Handler.Callback {
    private static final String ACTION_SHOW_INPUT_METHOD_PICKER = "com.android.server.inputmethod.InputMethodManagerService.SHOW_INPUT_METHOD_PICKER";
    static final boolean DEBUG;
    private static final int FALLBACK_DISPLAY_ID = 0;
    private static final String HANDLER_THREAD_NAME = "android.imms";
    private static final int MSG_BIND_ACCESSIBILITY_SERVICE = 3011;
    private static final int MSG_BIND_CLIENT = 3010;
    private static final int MSG_DISPATCH_ON_INPUT_METHOD_LIST_UPDATED = 5010;
    private static final int MSG_FINISH_HANDWRITING = 1110;
    private static final int MSG_HARD_KEYBOARD_SWITCH_CHANGED = 4000;
    private static final int MSG_HIDE_CURRENT_INPUT_METHOD = 1035;
    private static final int MSG_NOTIFY_IME_UID_TO_AUDIO_SERVICE = 7000;
    private static final int MSG_REMOVE_IME_SURFACE = 1060;
    private static final int MSG_REMOVE_IME_SURFACE_FROM_WINDOW = 1061;
    private static final int MSG_REPORT_FULLSCREEN_MODE = 3045;
    private static final int MSG_RESET_HANDWRITING = 1090;
    private static final int MSG_SET_ACTIVE = 3020;
    private static final int MSG_SET_INTERACTIVE = 3030;
    private static final int MSG_SHOW_IM_SUBTYPE_PICKER = 1;
    private static final int MSG_START_HANDWRITING = 1100;
    private static final int MSG_SYSTEM_UNLOCK_USER = 5000;
    private static final int MSG_UNBIND_ACCESSIBILITY_SERVICE = 3001;
    private static final int MSG_UNBIND_CLIENT = 3000;
    private static final int MSG_UPDATE_IME_WINDOW_STATUS = 1070;
    private static final int NOT_A_SUBTYPE_ID = -1;
    public static final String PROTO_ARG = "--proto";
    static final String TAG = "InputMethodManagerService";
    private static final String TAG_TRY_SUPPRESSING_IME_SWITCHER = "TrySuppressingImeSwitcher";
    private static final boolean TRAN_SECURITY_INPUT_SUPPORT;
    private static final String TRAN_SK_SWITCH = "sk_switch";
    private final AccessibilityManager mAccessibilityManager;
    private boolean mAccessibilityRequestingNoSoftKeyboard;
    final ActivityTaskManagerInternal mActivityTaskManagerInternal;
    private final AppOpsManager mAppOpsManager;
    private final InputMethodBindingController mBindingController;
    boolean mBoundToAccessibility;
    boolean mBoundToMethod;
    final Context mContext;
    EditorInfo mCurAttribute;
    private ClientState mCurClient;
    IBinder mCurFocusedWindow;
    ClientState mCurFocusedWindowClient;
    int mCurFocusedWindowSoftInputMode;
    private IBinder mCurHostInputToken;
    ImeOnBackInvokedDispatcher mCurImeDispatcher;
    IInputContext mCurInputContext;
    private boolean mCurPerceptible;
    IRemoteAccessibilityInputConnection mCurRemoteAccessibilityInputConnection;
    private InputMethodSubtype mCurrentSubtype;
    private final DisplayManagerInternal mDisplayManagerInternal;
    SessionState mEnabledSession;
    private final Handler mHandler;
    final boolean mHasFeature;
    private final HandwritingModeController mHwController;
    final IWindowManager mIWindowManager;
    final ImeDisplayValidator mImeDisplayValidator;
    private OverlayableSystemBooleanResourceWrapper mImeDrawsImeNavBarRes;
    Future<?> mImeDrawsImeNavBarResLazyInitFuture;
    boolean mImeHiddenByDisplayPolicy;
    final ImePlatformCompatUtils mImePlatformCompatUtils;
    private final PendingIntent mImeSwitchPendingIntent;
    private final Notification.Builder mImeSwitcherNotification;
    int mImeWindowVis;
    boolean mInFullscreenMode;
    private IInlineSuggestionsRequestCallback mInlineSuggestionsRequestCallback;
    final InputManagerInternal mInputManagerInternal;
    private boolean mInputShown;
    KeyguardManager mKeyguardManager;
    IBinder mLastImeTargetWindow;
    private int mLastSwitchUserId;
    private LocaleList mLastSystemLocales;
    private final InputMethodMenuController mMenuController;
    private final Set<String> mNonPreemptibleInputMethods;
    private NotificationManager mNotificationManager;
    private boolean mNotificationShown;
    final PackageManagerInternal mPackageManagerInternal;
    private CreateInlineSuggestionsRequest mPendingInlineSuggestionsRequest;
    private final boolean mPreventImeStartupUnlessTextEditor;
    final Resources mRes;
    final InputMethodUtils.InputMethodSettings mSettings;
    final SettingsObserver mSettingsObserver;
    boolean mShowExplicitlyRequested;
    boolean mShowForced;
    private boolean mShowOngoingImeSwitcherForPhones;
    private boolean mShowRequested;
    private final String mSlotIme;
    private StatusBarManagerService mStatusBar;
    final InputMethodSubtypeSwitchingController mSwitchingController;
    boolean mSystemReady;
    private final UserManager mUserManager;
    private final UserManagerInternal mUserManagerInternal;
    private UserSwitchHandlerTask mUserSwitchHandlerTask;
    final WindowManagerInternal mWindowManagerInternal;
    private final SparseBooleanArray mLoggedDeniedGetInputMethodWindowVisibleHeightForUid = new SparseBooleanArray(0);
    private final ArrayMap<String, List<InputMethodSubtype>> mAdditionalSubtypeMap = new ArrayMap<>();
    private AudioManagerInternal mAudioManagerInternal = null;
    final ArrayList<InputMethodInfo> mMethodList = new ArrayList<>();
    final ArrayMap<String, InputMethodInfo> mMethodMap = new ArrayMap<>();
    private int mMethodMapUpdateCount = 0;
    private int mDisplayIdToShowIme = -1;
    private List<String> mSecurityInputBlackList = new ArrayList();
    private int mSecurityInputEnable = 0;
    private Object mSecurityLock = new Object();
    final ArrayMap<IBinder, ClientState> mClients = new ArrayMap<>();
    private final SparseArray<VirtualDisplayInfo> mVirtualDisplayIdToParentMap = new SparseArray<>();
    private Matrix mCurVirtualDisplayToScreenMatrix = null;
    private int mCurTokenDisplayId = -1;
    SparseArray<AccessibilitySessionState> mEnabledAccessibilitySessions = new SparseArray<>();
    boolean mIsInteractive = true;
    int mBackDisposition = 0;
    private final MyPackageMonitor mMyPackageMonitor = new MyPackageMonitor();
    private final CopyOnWriteArrayList<InputMethodManagerInternal.InputMethodListListener> mInputMethodListListeners = new CopyOnWriteArrayList<>();
    private final WeakHashMap<IBinder, IBinder> mImeTargetWindowMap = new WeakHashMap<>();
    private final WeakHashMap<IBinder, IBinder> mShowRequestWindowMap = new WeakHashMap<>();
    private final WeakHashMap<IBinder, IBinder> mHideRequestWindowMap = new WeakHashMap<>();
    private final StartInputHistory mStartInputHistory = new StartInputHistory();
    private final SoftInputShowHideHistory mSoftInputShowHideHistory = new SoftInputShowHideHistory();
    private final PriorityDump.PriorityDumper mPriorityDumper = new PriorityDump.PriorityDumper() { // from class: com.android.server.inputmethod.InputMethodManagerService.2
        @Override // com.android.server.utils.PriorityDump.PriorityDumper
        public void dumpCritical(FileDescriptor fd, PrintWriter pw, String[] args, boolean asProto) {
            if (asProto) {
                dumpAsProtoNoCheck(fd);
            } else {
                InputMethodManagerService.this.dumpAsStringNoCheck(fd, pw, args, true);
            }
        }

        @Override // com.android.server.utils.PriorityDump.PriorityDumper
        public void dumpHigh(FileDescriptor fd, PrintWriter pw, String[] args, boolean asProto) {
            dumpNormal(fd, pw, args, asProto);
        }

        @Override // com.android.server.utils.PriorityDump.PriorityDumper
        public void dumpNormal(FileDescriptor fd, PrintWriter pw, String[] args, boolean asProto) {
            if (asProto) {
                dumpAsProtoNoCheck(fd);
            } else {
                InputMethodManagerService.this.dumpAsStringNoCheck(fd, pw, args, false);
            }
        }

        @Override // com.android.server.utils.PriorityDump.PriorityDumper
        public void dump(FileDescriptor fd, PrintWriter pw, String[] args, boolean asProto) {
            dumpNormal(fd, pw, args, asProto);
        }

        private void dumpAsProtoNoCheck(FileDescriptor fd) {
            ProtoOutputStream proto = new ProtoOutputStream(fd);
            InputMethodManagerService.this.dumpDebug(proto, 1146756268035L);
            proto.flush();
        }
    };
    private final IPackageManager mIPackageManager = AppGlobals.getPackageManager();

    /* JADX INFO: Access modifiers changed from: package-private */
    @FunctionalInterface
    /* loaded from: classes.dex */
    public interface ImeDisplayValidator {
        int getDisplayImePolicy(int i);
    }

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes.dex */
    private @interface ShellCommandResult {
        public static final int FAILURE = -1;
        public static final int SUCCESS = 0;
    }

    static {
        DEBUG = "1".equals(SystemProperties.get("persist.sys.adb.support", "0")) || "1".equals(SystemProperties.get("persist.sys.fans.support", "0"));
        TRAN_SECURITY_INPUT_SUPPORT = 1 == SystemProperties.getInt("ro.tran_security_input_support", 0);
    }

    private static void hookIMEVisibleChanged(boolean show) {
        ITranWindowManagerService.Instance().hookIMEVisibleChanged(show);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static final class CreateInlineSuggestionsRequest {
        final IInlineSuggestionsRequestCallback mCallback;
        final String mPackageName;
        final InlineSuggestionsRequestInfo mRequestInfo;

        CreateInlineSuggestionsRequest(InlineSuggestionsRequestInfo requestInfo, IInlineSuggestionsRequestCallback callback, String packageName) {
            this.mRequestInfo = requestInfo;
            this.mCallback = callback;
            this.mPackageName = packageName;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getDisplayIdToShowImeLocked() {
        return this.mDisplayIdToShowIme;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class SessionState {
        InputChannel channel;
        final ClientState client;
        final IInputMethodInvoker method;
        IInputMethodSession session;

        public String toString() {
            return "SessionState{uid " + this.client.uid + " pid " + this.client.pid + " method " + Integer.toHexString(IInputMethodInvoker.getBinderIdentityHashCode(this.method)) + " session " + Integer.toHexString(System.identityHashCode(this.session)) + " channel " + this.channel + "}";
        }

        SessionState(ClientState _client, IInputMethodInvoker _method, IInputMethodSession _session, InputChannel _channel) {
            this.client = _client;
            this.method = _method;
            this.session = _session;
            this.channel = _channel;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class AccessibilitySessionState {
        final ClientState mClient;
        final int mId;
        public IAccessibilityInputMethodSession mSession;

        public String toString() {
            return "AccessibilitySessionState{uid " + this.mClient.uid + " pid " + this.mClient.pid + " id " + Integer.toHexString(this.mId) + " session " + Integer.toHexString(System.identityHashCode(this.mSession)) + "}";
        }

        AccessibilitySessionState(ClientState client, int id, IAccessibilityInputMethodSession session) {
            this.mClient = client;
            this.mId = id;
            this.mSession = session;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static final class ClientDeathRecipient implements IBinder.DeathRecipient {
        private final IInputMethodClient mClient;
        private final InputMethodManagerService mImms;

        ClientDeathRecipient(InputMethodManagerService imms, IInputMethodClient client) {
            this.mImms = imms;
            this.mClient = client;
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            this.mImms.removeClient(this.mClient);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static final class ClientState {
        final InputBinding binding;
        final IInputMethodClient client;
        final ClientDeathRecipient clientDeathRecipient;
        SessionState curSession;
        final IInputContext inputContext;
        SparseArray<AccessibilitySessionState> mAccessibilitySessions = new SparseArray<>();
        boolean mSessionRequestedForAccessibility;
        final int pid;
        final int selfReportedDisplayId;
        boolean sessionRequested;
        final int uid;

        public String toString() {
            return "ClientState{" + Integer.toHexString(System.identityHashCode(this)) + " uid=" + this.uid + " pid=" + this.pid + " displayId=" + this.selfReportedDisplayId + "}";
        }

        ClientState(IInputMethodClient _client, IInputContext _inputContext, int _uid, int _pid, int _selfReportedDisplayId, ClientDeathRecipient _clientDeathRecipient) {
            this.client = _client;
            this.inputContext = _inputContext;
            this.uid = _uid;
            this.pid = _pid;
            this.selfReportedDisplayId = _selfReportedDisplayId;
            this.binding = new InputBinding(null, _inputContext.asBinder(), _uid, _pid);
            this.clientDeathRecipient = _clientDeathRecipient;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static final class VirtualDisplayInfo {
        private final Matrix mMatrix;
        private final ClientState mParentClient;

        VirtualDisplayInfo(ClientState parentClient, Matrix matrix) {
            this.mParentClient = parentClient;
            this.mMatrix = matrix;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public String getSelectedMethodIdLocked() {
        return this.mBindingController.getSelectedMethodId();
    }

    private void setSelectedMethodIdLocked(String selectedMethodId) {
        this.mBindingController.setSelectedMethodId(selectedMethodId);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int getSequenceNumberLocked() {
        return this.mBindingController.getSequenceNumber();
    }

    private void advanceSequenceNumberLocked() {
        this.mBindingController.advanceSequenceNumber();
    }

    private String getCurIdLocked() {
        return this.mBindingController.getCurId();
    }

    private boolean hasConnectionLocked() {
        return this.mBindingController.hasConnection();
    }

    private Intent getCurIntentLocked() {
        return this.mBindingController.getCurIntent();
    }

    private IBinder getCurTokenLocked() {
        return this.mBindingController.getCurToken();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getCurTokenDisplayIdLocked() {
        return this.mCurTokenDisplayId;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setCurTokenDisplayIdLocked(int curTokenDisplayId) {
        this.mCurTokenDisplayId = curTokenDisplayId;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public IInputMethodInvoker getCurMethodLocked() {
        return this.mBindingController.getCurMethod();
    }

    private int getCurMethodUidLocked() {
        return this.mBindingController.getCurMethodUid();
    }

    private long getLastBindTimeLocked() {
        return this.mBindingController.getLastBindTime();
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class StartInputInfo {
        private static final AtomicInteger sSequenceNumber = new AtomicInteger(0);
        final int mClientBindSequenceNumber;
        final EditorInfo mEditorInfo;
        final int mImeDisplayId;
        final String mImeId;
        final IBinder mImeToken;
        final int mImeUserId;
        final boolean mRestarting;
        final int mStartInputReason;
        final int mTargetDisplayId;
        final int mTargetUserId;
        final IBinder mTargetWindow;
        final int mTargetWindowSoftInputMode;
        final int mSequenceNumber = sSequenceNumber.getAndIncrement();
        final long mTimestamp = SystemClock.uptimeMillis();
        final long mWallTime = System.currentTimeMillis();

        StartInputInfo(int imeUserId, IBinder imeToken, int imeDisplayId, String imeId, int startInputReason, boolean restarting, int targetUserId, int targetDisplayId, IBinder targetWindow, EditorInfo editorInfo, int targetWindowSoftInputMode, int clientBindSequenceNumber) {
            this.mImeUserId = imeUserId;
            this.mImeToken = imeToken;
            this.mImeDisplayId = imeDisplayId;
            this.mImeId = imeId;
            this.mStartInputReason = startInputReason;
            this.mRestarting = restarting;
            this.mTargetUserId = targetUserId;
            this.mTargetDisplayId = targetDisplayId;
            this.mTargetWindow = targetWindow;
            this.mEditorInfo = editorInfo;
            this.mTargetWindowSoftInputMode = targetWindowSoftInputMode;
            this.mClientBindSequenceNumber = clientBindSequenceNumber;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static final class SoftInputShowHideHistory {
        private static final AtomicInteger sSequenceNumber = new AtomicInteger(0);
        private final Entry[] mEntries;
        private int mNextIndex;

        private SoftInputShowHideHistory() {
            this.mEntries = new Entry[16];
            this.mNextIndex = 0;
        }

        /* JADX INFO: Access modifiers changed from: private */
        /* loaded from: classes.dex */
        public static final class Entry {
            final ClientState mClientState;
            final EditorInfo mEditorInfo;
            final String mFocusedWindowName;
            final int mFocusedWindowSoftInputMode;
            final String mImeControlTargetName;
            final String mImeTargetNameFromWm;
            final boolean mInFullscreenMode;
            final int mReason;
            final String mRequestWindowName;
            final int mSequenceNumber = SoftInputShowHideHistory.sSequenceNumber.getAndIncrement();
            final long mTimestamp = SystemClock.uptimeMillis();
            final long mWallTime = System.currentTimeMillis();

            Entry(ClientState client, EditorInfo editorInfo, String focusedWindowName, int softInputMode, int reason, boolean inFullscreenMode, String requestWindowName, String imeControlTargetName, String imeTargetName) {
                this.mClientState = client;
                this.mEditorInfo = editorInfo;
                this.mFocusedWindowName = focusedWindowName;
                this.mFocusedWindowSoftInputMode = softInputMode;
                this.mReason = reason;
                this.mInFullscreenMode = inFullscreenMode;
                this.mRequestWindowName = requestWindowName;
                this.mImeControlTargetName = imeControlTargetName;
                this.mImeTargetNameFromWm = imeTargetName;
            }
        }

        void addEntry(Entry entry) {
            int index = this.mNextIndex;
            Entry[] entryArr = this.mEntries;
            entryArr[index] = entry;
            this.mNextIndex = (this.mNextIndex + 1) % entryArr.length;
        }

        void dump(PrintWriter pw, String prefix) {
            SimpleDateFormat dataFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);
            int i = 0;
            while (true) {
                Entry[] entryArr = this.mEntries;
                if (i < entryArr.length) {
                    Entry entry = entryArr[(this.mNextIndex + i) % entryArr.length];
                    if (entry != null) {
                        pw.print(prefix);
                        pw.println("SoftInputShowHideHistory #" + entry.mSequenceNumber + ":");
                        pw.print(prefix);
                        pw.println(" time=" + dataFormat.format(new Date(entry.mWallTime)) + " (timestamp=" + entry.mTimestamp + ")");
                        pw.print(prefix);
                        pw.print(" reason=" + InputMethodDebug.softInputDisplayReasonToString(entry.mReason));
                        pw.println(" inFullscreenMode=" + entry.mInFullscreenMode);
                        pw.print(prefix);
                        pw.println(" requestClient=" + entry.mClientState);
                        pw.print(prefix);
                        pw.println(" focusedWindowName=" + entry.mFocusedWindowName);
                        pw.print(prefix);
                        pw.println(" requestWindowName=" + entry.mRequestWindowName);
                        pw.print(prefix);
                        pw.println(" imeControlTargetName=" + entry.mImeControlTargetName);
                        pw.print(prefix);
                        pw.println(" imeTargetNameFromWm=" + entry.mImeTargetNameFromWm);
                        pw.print(prefix);
                        pw.print(" editorInfo: ");
                        pw.print(" inputType=" + entry.mEditorInfo.inputType);
                        pw.print(" privateImeOptions=" + entry.mEditorInfo.privateImeOptions);
                        pw.println(" fieldId (viewId)=" + entry.mEditorInfo.fieldId);
                        pw.print(prefix);
                        pw.println(" focusedWindowSoftInputMode=" + InputMethodDebug.softInputModeToString(entry.mFocusedWindowSoftInputMode));
                    }
                    i++;
                } else {
                    return;
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static final class StartInputHistory {
        private static final int ENTRY_SIZE_FOR_HIGH_RAM_DEVICE = 32;
        private static final int ENTRY_SIZE_FOR_LOW_RAM_DEVICE = 5;
        private final Entry[] mEntries;
        private int mNextIndex;

        private StartInputHistory() {
            this.mEntries = new Entry[getEntrySize()];
            this.mNextIndex = 0;
        }

        private static int getEntrySize() {
            if (ActivityManager.isLowRamDeviceStatic()) {
                return 5;
            }
            return 32;
        }

        /* JADX INFO: Access modifiers changed from: private */
        /* loaded from: classes.dex */
        public static final class Entry {
            int mClientBindSequenceNumber;
            EditorInfo mEditorInfo;
            int mImeDisplayId;
            String mImeId;
            String mImeTokenString;
            int mImeUserId;
            boolean mRestarting;
            int mSequenceNumber;
            int mStartInputReason;
            int mTargetDisplayId;
            int mTargetUserId;
            int mTargetWindowSoftInputMode;
            String mTargetWindowString;
            long mTimestamp;
            long mWallTime;

            Entry(StartInputInfo original) {
                set(original);
            }

            void set(StartInputInfo original) {
                this.mSequenceNumber = original.mSequenceNumber;
                this.mTimestamp = original.mTimestamp;
                this.mWallTime = original.mWallTime;
                this.mImeUserId = original.mImeUserId;
                this.mImeTokenString = String.valueOf(original.mImeToken);
                this.mImeDisplayId = original.mImeDisplayId;
                this.mImeId = original.mImeId;
                this.mStartInputReason = original.mStartInputReason;
                this.mRestarting = original.mRestarting;
                this.mTargetUserId = original.mTargetUserId;
                this.mTargetDisplayId = original.mTargetDisplayId;
                this.mTargetWindowString = String.valueOf(original.mTargetWindow);
                this.mEditorInfo = original.mEditorInfo;
                this.mTargetWindowSoftInputMode = original.mTargetWindowSoftInputMode;
                this.mClientBindSequenceNumber = original.mClientBindSequenceNumber;
            }
        }

        void addEntry(StartInputInfo info) {
            int index = this.mNextIndex;
            Entry[] entryArr = this.mEntries;
            Entry entry = entryArr[index];
            if (entry == null) {
                entryArr[index] = new Entry(info);
            } else {
                entry.set(info);
            }
            this.mNextIndex = (this.mNextIndex + 1) % this.mEntries.length;
        }

        void dump(PrintWriter pw, String prefix) {
            SimpleDateFormat dataFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);
            int i = 0;
            while (true) {
                Entry[] entryArr = this.mEntries;
                if (i < entryArr.length) {
                    Entry entry = entryArr[(this.mNextIndex + i) % entryArr.length];
                    if (entry != null) {
                        pw.print(prefix);
                        pw.println("StartInput #" + entry.mSequenceNumber + ":");
                        pw.print(prefix);
                        pw.println(" time=" + dataFormat.format(new Date(entry.mWallTime)) + " (timestamp=" + entry.mTimestamp + ") reason=" + InputMethodDebug.startInputReasonToString(entry.mStartInputReason) + " restarting=" + entry.mRestarting);
                        pw.print(prefix);
                        pw.print(" imeToken=" + entry.mImeTokenString + " [" + entry.mImeId + "]");
                        pw.print(" imeUserId=" + entry.mImeUserId);
                        pw.println(" imeDisplayId=" + entry.mImeDisplayId);
                        pw.print(prefix);
                        pw.println(" targetWin=" + entry.mTargetWindowString + " [" + entry.mEditorInfo.packageName + "] targetUserId=" + entry.mTargetUserId + " targetDisplayId=" + entry.mTargetDisplayId + " clientBindSeq=" + entry.mClientBindSequenceNumber);
                        pw.print(prefix);
                        pw.println(" softInputMode=" + InputMethodDebug.softInputModeToString(entry.mTargetWindowSoftInputMode));
                        pw.print(prefix);
                        pw.println(" inputType=0x" + Integer.toHexString(entry.mEditorInfo.inputType) + " imeOptions=0x" + Integer.toHexString(entry.mEditorInfo.imeOptions) + " fieldId=0x" + Integer.toHexString(entry.mEditorInfo.fieldId) + " fieldName=" + entry.mEditorInfo.fieldName + " actionId=" + entry.mEditorInfo.actionId + " actionLabel=" + ((Object) entry.mEditorInfo.actionLabel));
                    }
                    i++;
                } else {
                    return;
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class SettingsObserver extends ContentObserver {
        String mLastEnabled;
        boolean mRegistered;
        int mUserId;
        final Uri tranSkSwitchUri;

        SettingsObserver(Handler handler) {
            super(handler);
            this.mRegistered = false;
            this.mLastEnabled = "";
            this.tranSkSwitchUri = Settings.Secure.getUriFor(InputMethodManagerService.TRAN_SK_SWITCH);
        }

        public void registerContentObserverLocked(int userId) {
            if (this.mRegistered && this.mUserId == userId) {
                return;
            }
            ContentResolver resolver = InputMethodManagerService.this.mContext.getContentResolver();
            if (this.mRegistered) {
                InputMethodManagerService.this.mContext.getContentResolver().unregisterContentObserver(this);
                this.mRegistered = false;
            }
            if (this.mUserId != userId) {
                this.mLastEnabled = "";
                this.mUserId = userId;
            }
            resolver.registerContentObserver(Settings.Secure.getUriFor("default_input_method"), false, this, userId);
            resolver.registerContentObserver(Settings.Secure.getUriFor("enabled_input_methods"), false, this, userId);
            resolver.registerContentObserver(Settings.Secure.getUriFor("selected_input_method_subtype"), false, this, userId);
            resolver.registerContentObserver(Settings.Secure.getUriFor("show_ime_with_hard_keyboard"), false, this, userId);
            resolver.registerContentObserver(Settings.Secure.getUriFor("accessibility_soft_keyboard_mode"), false, this, userId);
            if (InputMethodManagerService.TRAN_SECURITY_INPUT_SUPPORT) {
                resolver.registerContentObserver(this.tranSkSwitchUri, false, this, userId);
                InputMethodManagerService inputMethodManagerService = InputMethodManagerService.this;
                inputMethodManagerService.mSecurityInputEnable = Settings.Secure.getIntForUser(inputMethodManagerService.mContext.getContentResolver(), InputMethodManagerService.TRAN_SK_SWITCH, 0, userId);
            }
            this.mRegistered = true;
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            Uri showImeUri = Settings.Secure.getUriFor("show_ime_with_hard_keyboard");
            Uri accessibilityRequestingNoImeUri = Settings.Secure.getUriFor("accessibility_soft_keyboard_mode");
            synchronized (ImfLock.class) {
                if (showImeUri.equals(uri)) {
                    InputMethodManagerService.this.mMenuController.updateKeyboardFromSettingsLocked();
                } else if (accessibilityRequestingNoImeUri.equals(uri)) {
                    int accessibilitySoftKeyboardSetting = Settings.Secure.getIntForUser(InputMethodManagerService.this.mContext.getContentResolver(), "accessibility_soft_keyboard_mode", 0, this.mUserId);
                    InputMethodManagerService.this.mAccessibilityRequestingNoSoftKeyboard = (accessibilitySoftKeyboardSetting & 3) == 1;
                    if (InputMethodManagerService.this.mAccessibilityRequestingNoSoftKeyboard) {
                        boolean showRequested = InputMethodManagerService.this.mShowRequested;
                        InputMethodManagerService inputMethodManagerService = InputMethodManagerService.this;
                        inputMethodManagerService.hideCurrentInputLocked(inputMethodManagerService.mCurFocusedWindow, 0, null, 15);
                        InputMethodManagerService.this.mShowRequested = showRequested;
                    } else if (InputMethodManagerService.this.mShowRequested) {
                        InputMethodManagerService inputMethodManagerService2 = InputMethodManagerService.this;
                        inputMethodManagerService2.showCurrentInputLocked(inputMethodManagerService2.mCurFocusedWindow, 1, null, 8);
                    }
                } else if (InputMethodManagerService.TRAN_SECURITY_INPUT_SUPPORT && this.tranSkSwitchUri.equals(uri)) {
                    InputMethodManagerService inputMethodManagerService3 = InputMethodManagerService.this;
                    inputMethodManagerService3.mSecurityInputEnable = Settings.Secure.getIntForUser(inputMethodManagerService3.mContext.getContentResolver(), InputMethodManagerService.TRAN_SK_SWITCH, 0, this.mUserId);
                } else {
                    boolean enabledChanged = false;
                    String newEnabled = InputMethodManagerService.this.mSettings.getEnabledInputMethodsStr();
                    if (!this.mLastEnabled.equals(newEnabled)) {
                        this.mLastEnabled = newEnabled;
                        enabledChanged = true;
                    }
                    InputMethodManagerService.this.updateInputMethodsFromSettingsLocked(enabledChanged);
                }
            }
        }

        public String toString() {
            return "SettingsObserver{mUserId=" + this.mUserId + " mRegistered=" + this.mRegistered + " mLastEnabled=" + this.mLastEnabled + "}";
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class ImmsBroadcastReceiverForSystemUser extends BroadcastReceiver {
        private ImmsBroadcastReceiverForSystemUser() {
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("android.intent.action.USER_ADDED".equals(action) || "android.intent.action.USER_REMOVED".equals(action)) {
                InputMethodManagerService.this.updateCurrentProfileIds();
            } else if ("android.intent.action.LOCALE_CHANGED".equals(action)) {
                InputMethodManagerService.this.onActionLocaleChanged();
            } else if (InputMethodManagerService.ACTION_SHOW_INPUT_METHOD_PICKER.equals(action)) {
                InputMethodManagerService.this.mHandler.obtainMessage(1, 1, 0).sendToTarget();
            } else {
                Slog.w(InputMethodManagerService.TAG, "Unexpected intent " + intent);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class ImmsBroadcastReceiverForAllUsers extends BroadcastReceiver {
        private ImmsBroadcastReceiverForAllUsers() {
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("android.intent.action.CLOSE_SYSTEM_DIALOGS".equals(action)) {
                BroadcastReceiver.PendingResult pendingResult = getPendingResult();
                if (pendingResult == null) {
                    return;
                }
                int senderUserId = pendingResult.getSendingUserId();
                if (senderUserId != -1 && senderUserId != InputMethodManagerService.this.mSettings.getCurrentUserId()) {
                    return;
                }
                InputMethodManagerService.this.mMenuController.hideInputMethodMenu();
                return;
            }
            Slog.w(InputMethodManagerService.TAG, "Unexpected intent " + intent);
        }
    }

    void onActionLocaleChanged() {
        synchronized (ImfLock.class) {
            LocaleList possibleNewLocale = this.mRes.getConfiguration().getLocales();
            if (possibleNewLocale == null || !possibleNewLocale.equals(this.mLastSystemLocales)) {
                buildInputMethodListLocked(true);
                resetDefaultImeLocked(this.mContext);
                updateFromSettingsLocked(true);
                this.mLastSystemLocales = possibleNewLocale;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public final class MyPackageMonitor extends PackageMonitor {
        private final ArraySet<String> mKnownImePackageNames = new ArraySet<>();
        private final ArrayList<String> mChangedPackages = new ArrayList<>();
        private boolean mImePackageAppeared = false;

        MyPackageMonitor() {
        }

        void clearKnownImePackageNamesLocked() {
            this.mKnownImePackageNames.clear();
        }

        final void addKnownImePackageNameLocked(String packageName) {
            this.mKnownImePackageNames.add(packageName);
        }

        private boolean isChangingPackagesOfCurrentUserLocked() {
            int userId = getChangingUserId();
            boolean retval = userId == InputMethodManagerService.this.mSettings.getCurrentUserId();
            if (InputMethodManagerService.DEBUG && !retval) {
                Slog.d(InputMethodManagerService.TAG, "--- ignore this call back from a background user: " + userId);
            }
            return retval;
        }

        public boolean onHandleForceStop(Intent intent, String[] packages, int uid, boolean doit) {
            synchronized (ImfLock.class) {
                if (isChangingPackagesOfCurrentUserLocked()) {
                    String curInputMethodId = InputMethodManagerService.this.mSettings.getSelectedInputMethod();
                    int N = InputMethodManagerService.this.mMethodList.size();
                    if (curInputMethodId != null) {
                        for (int i = 0; i < N; i++) {
                            InputMethodInfo imi = InputMethodManagerService.this.mMethodList.get(i);
                            if (imi.getId().equals(curInputMethodId)) {
                                for (String pkg : packages) {
                                    if (imi.getPackageName().equals(pkg)) {
                                        if (!doit) {
                                            return true;
                                        }
                                        InputMethodManagerService.this.resetSelectedInputMethodAndSubtypeLocked("");
                                        InputMethodManagerService.this.chooseNewDefaultIMELocked();
                                        return true;
                                    }
                                }
                                continue;
                            }
                        }
                    }
                    return false;
                }
                return false;
            }
        }

        public void onBeginPackageChanges() {
            clearPackageChangeState();
        }

        public void onPackageAppeared(String packageName, int reason) {
            if (!this.mImePackageAppeared) {
                PackageManager pm = InputMethodManagerService.this.mContext.getPackageManager();
                List<ResolveInfo> services = pm.queryIntentServicesAsUser(new Intent("android.view.InputMethod").setPackage(packageName), 512, getChangingUserId());
                if (!services.isEmpty()) {
                    this.mImePackageAppeared = true;
                }
            }
            this.mChangedPackages.add(packageName);
        }

        public void onPackageDisappeared(String packageName, int reason) {
            this.mChangedPackages.add(packageName);
        }

        public void onPackageModified(String packageName) {
            this.mChangedPackages.add(packageName);
        }

        public void onPackagesSuspended(String[] packages) {
            for (String packageName : packages) {
                this.mChangedPackages.add(packageName);
            }
        }

        public void onPackagesUnsuspended(String[] packages) {
            for (String packageName : packages) {
                this.mChangedPackages.add(packageName);
            }
        }

        public void onFinishPackageChanges() {
            onFinishPackageChangesInternal();
            clearPackageChangeState();
        }

        public void onUidRemoved(int uid) {
            synchronized (ImfLock.class) {
                InputMethodManagerService.this.mLoggedDeniedGetInputMethodWindowVisibleHeightForUid.delete(uid);
            }
        }

        private void clearPackageChangeState() {
            this.mChangedPackages.clear();
            this.mImePackageAppeared = false;
        }

        private boolean shouldRebuildInputMethodListLocked() {
            if (this.mImePackageAppeared) {
                return true;
            }
            int N = this.mChangedPackages.size();
            for (int i = 0; i < N; i++) {
                String packageName = this.mChangedPackages.get(i);
                if (this.mKnownImePackageNames.contains(packageName)) {
                    return true;
                }
            }
            return false;
        }

        private void onFinishPackageChangesInternal() {
            int change;
            synchronized (ImfLock.class) {
                if (isChangingPackagesOfCurrentUserLocked()) {
                    if (shouldRebuildInputMethodListLocked()) {
                        InputMethodInfo curIm = null;
                        String curInputMethodId = InputMethodManagerService.this.mSettings.getSelectedInputMethod();
                        int N = InputMethodManagerService.this.mMethodList.size();
                        if (curInputMethodId != null) {
                            for (int i = 0; i < N; i++) {
                                InputMethodInfo imi = InputMethodManagerService.this.mMethodList.get(i);
                                String imiId = imi.getId();
                                if (imiId.equals(curInputMethodId)) {
                                    curIm = imi;
                                }
                                int change2 = isPackageDisappearing(imi.getPackageName());
                                if (isPackageModified(imi.getPackageName())) {
                                    InputMethodManagerService.this.mAdditionalSubtypeMap.remove(imi.getId());
                                    AdditionalSubtypeUtils.save(InputMethodManagerService.this.mAdditionalSubtypeMap, InputMethodManagerService.this.mMethodMap, InputMethodManagerService.this.mSettings.getCurrentUserId());
                                }
                                if (change2 == 2 || change2 == 3) {
                                    Slog.i(InputMethodManagerService.TAG, "Input method uninstalled, disabling: " + imi.getComponent());
                                    InputMethodManagerService.this.setInputMethodEnabledLocked(imi.getId(), false);
                                }
                            }
                        }
                        InputMethodManagerService.this.buildInputMethodListLocked(false);
                        boolean changed = false;
                        if (curIm != null && ((change = isPackageDisappearing(curIm.getPackageName())) == 2 || change == 3)) {
                            ServiceInfo si = null;
                            try {
                                si = InputMethodManagerService.this.mIPackageManager.getServiceInfo(curIm.getComponent(), 0L, InputMethodManagerService.this.mSettings.getCurrentUserId());
                            } catch (RemoteException e) {
                            }
                            if (si == null) {
                                Slog.i(InputMethodManagerService.TAG, "Current input method removed: " + curInputMethodId);
                                InputMethodManagerService inputMethodManagerService = InputMethodManagerService.this;
                                inputMethodManagerService.updateSystemUiLocked(0, inputMethodManagerService.mBackDisposition);
                                if (!InputMethodManagerService.this.chooseNewDefaultIMELocked()) {
                                    changed = true;
                                    curIm = null;
                                    Slog.i(InputMethodManagerService.TAG, "Unsetting current input method");
                                    InputMethodManagerService.this.resetSelectedInputMethodAndSubtypeLocked("");
                                }
                            }
                        }
                        if (curIm == null) {
                            changed = InputMethodManagerService.this.chooseNewDefaultIMELocked();
                        } else if (!changed && isPackageModified(curIm.getPackageName())) {
                            changed = true;
                        }
                        if (changed) {
                            InputMethodManagerService.this.updateFromSettingsLocked(false);
                        }
                    }
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static final class UserSwitchHandlerTask implements Runnable {
        IInputMethodClient mClientToBeReset;
        final InputMethodManagerService mService;
        final int mToUserId;

        UserSwitchHandlerTask(InputMethodManagerService service, int toUserId, IInputMethodClient clientToBeReset) {
            this.mService = service;
            this.mToUserId = toUserId;
            this.mClientToBeReset = clientToBeReset;
        }

        @Override // java.lang.Runnable
        public void run() {
            synchronized (ImfLock.class) {
                if (this.mService.mUserSwitchHandlerTask != this) {
                    return;
                }
                InputMethodManagerService inputMethodManagerService = this.mService;
                inputMethodManagerService.switchUserOnHandlerLocked(inputMethodManagerService.mUserSwitchHandlerTask.mToUserId, this.mClientToBeReset);
                this.mService.mUserSwitchHandlerTask = null;
            }
        }
    }

    /* loaded from: classes.dex */
    public static final class Lifecycle extends SystemService {
        private final InputMethodManagerService mService;

        public Lifecycle(Context context) {
            super(context);
            this.mService = new InputMethodManagerService(context);
        }

        @Override // com.android.server.SystemService
        public void onStart() {
            this.mService.publishLocalService();
            publishBinderService("input_method", this.mService, false, 21);
        }

        @Override // com.android.server.SystemService
        public void onUserSwitching(SystemService.TargetUser from, SystemService.TargetUser to) {
            synchronized (ImfLock.class) {
                this.mService.scheduleSwitchUserTaskLocked(to.getUserIdentifier(), null);
            }
        }

        @Override // com.android.server.SystemService
        public void onBootPhase(int phase) {
            if (phase == 550) {
                StatusBarManagerService statusBarService = (StatusBarManagerService) ServiceManager.getService("statusbar");
                this.mService.systemRunning(statusBarService);
            }
        }

        @Override // com.android.server.SystemService
        public void onUserUnlocking(SystemService.TargetUser user) {
            this.mService.mHandler.obtainMessage(5000, user.getUserIdentifier(), 0).sendToTarget();
        }
    }

    void onUnlockUser(int userId) {
        synchronized (ImfLock.class) {
            int currentUserId = this.mSettings.getCurrentUserId();
            if (DEBUG) {
                Slog.d(TAG, "onUnlockUser: userId=" + userId + " curUserId=" + currentUserId);
            }
            if (userId != currentUserId) {
                return;
            }
            this.mSettings.switchCurrentUser(currentUserId, !this.mSystemReady);
            if (this.mSystemReady) {
                buildInputMethodListLocked(false);
                updateInputMethodsFromSettingsLocked(true);
            }
        }
    }

    void scheduleSwitchUserTaskLocked(int userId, IInputMethodClient clientToBeReset) {
        UserSwitchHandlerTask userSwitchHandlerTask = this.mUserSwitchHandlerTask;
        if (userSwitchHandlerTask != null) {
            if (userSwitchHandlerTask.mToUserId == userId) {
                this.mUserSwitchHandlerTask.mClientToBeReset = clientToBeReset;
                return;
            }
            this.mHandler.removeCallbacks(this.mUserSwitchHandlerTask);
        }
        hideCurrentInputLocked(this.mCurFocusedWindow, 0, null, 9);
        UserSwitchHandlerTask task = new UserSwitchHandlerTask(this, userId, clientToBeReset);
        this.mUserSwitchHandlerTask = task;
        this.mHandler.post(task);
    }

    public InputMethodManagerService(Context context) {
        this.mContext = context;
        this.mRes = context.getResources();
        ServiceThread thread = new ServiceThread(HANDLER_THREAD_NAME, -2, true);
        thread.start();
        Handler createAsync = Handler.createAsync(thread.getLooper(), this);
        this.mHandler = createAsync;
        this.mSettingsObserver = new SettingsObserver(createAsync);
        this.mIWindowManager = IWindowManager.Stub.asInterface(ServiceManager.getService("window"));
        final WindowManagerInternal windowManagerInternal = (WindowManagerInternal) LocalServices.getService(WindowManagerInternal.class);
        this.mWindowManagerInternal = windowManagerInternal;
        this.mActivityTaskManagerInternal = (ActivityTaskManagerInternal) LocalServices.getService(ActivityTaskManagerInternal.class);
        this.mPackageManagerInternal = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
        this.mInputManagerInternal = (InputManagerInternal) LocalServices.getService(InputManagerInternal.class);
        this.mImePlatformCompatUtils = new ImePlatformCompatUtils();
        Objects.requireNonNull(windowManagerInternal);
        this.mImeDisplayValidator = new ImeDisplayValidator() { // from class: com.android.server.inputmethod.InputMethodManagerService$$ExternalSyntheticLambda5
            @Override // com.android.server.inputmethod.InputMethodManagerService.ImeDisplayValidator
            public final int getDisplayImePolicy(int i) {
                return WindowManagerInternal.this.getDisplayImePolicy(i);
            }
        };
        this.mDisplayManagerInternal = (DisplayManagerInternal) LocalServices.getService(DisplayManagerInternal.class);
        this.mAppOpsManager = (AppOpsManager) context.getSystemService(AppOpsManager.class);
        this.mUserManager = (UserManager) context.getSystemService(UserManager.class);
        this.mUserManagerInternal = (UserManagerInternal) LocalServices.getService(UserManagerInternal.class);
        this.mAccessibilityManager = AccessibilityManager.getInstance(context);
        this.mHasFeature = context.getPackageManager().hasSystemFeature("android.software.input_methods");
        this.mSlotIme = context.getString(17041571);
        Bundle extras = new Bundle();
        extras.putBoolean("android.allowDuringSetup", true);
        int accentColor = context.getColor(17170460);
        this.mImeSwitcherNotification = new Notification.Builder(context, SystemNotificationChannels.VIRTUAL_KEYBOARD).setSmallIcon(17302810).setWhen(0L).setOngoing(true).addExtras(extras).setCategory(Griffin.SYS).setColor(accentColor);
        Intent intent = new Intent(ACTION_SHOW_INPUT_METHOD_PICKER).setPackage(context.getPackageName());
        this.mImeSwitchPendingIntent = PendingIntent.getBroadcast(context, 0, intent, 67108864);
        this.mShowOngoingImeSwitcherForPhones = false;
        this.mNotificationShown = false;
        int userId = 0;
        try {
            userId = ActivityManager.getService().getCurrentUser().id;
        } catch (RemoteException e) {
            Slog.w(TAG, "Couldn't get current user ID; guessing it's 0", e);
        }
        this.mLastSwitchUserId = userId;
        InputMethodUtils.InputMethodSettings inputMethodSettings = new InputMethodUtils.InputMethodSettings(this.mRes, context.getContentResolver(), this.mMethodMap, userId, !this.mSystemReady);
        this.mSettings = inputMethodSettings;
        updateCurrentProfileIds();
        AdditionalSubtypeUtils.load(this.mAdditionalSubtypeMap, userId);
        this.mSwitchingController = InputMethodSubtypeSwitchingController.createInstanceLocked(inputMethodSettings, context);
        this.mMenuController = new InputMethodMenuController(this);
        this.mBindingController = new InputMethodBindingController(this);
        this.mPreventImeStartupUnlessTextEditor = this.mRes.getBoolean(17891335);
        this.mNonPreemptibleInputMethods = Sets.newHashSet(this.mRes.getStringArray(17236101));
        this.mHwController = new HandwritingModeController(thread.getLooper(), new InkWindowInitializer());
    }

    /* loaded from: classes.dex */
    private final class InkWindowInitializer implements Runnable {
        private InkWindowInitializer() {
        }

        @Override // java.lang.Runnable
        public void run() {
            synchronized (ImfLock.class) {
                IInputMethodInvoker curMethod = InputMethodManagerService.this.getCurMethodLocked();
                if (curMethod != null) {
                    curMethod.initInkWindow();
                }
            }
        }
    }

    private void resetDefaultImeLocked(Context context) {
        String selectedMethodId = getSelectedMethodIdLocked();
        if (selectedMethodId != null && !this.mMethodMap.get(selectedMethodId).isSystem()) {
            return;
        }
        List<InputMethodInfo> suitableImes = InputMethodUtils.getDefaultEnabledImes(context, this.mSettings.getEnabledInputMethodListLocked());
        if (suitableImes.isEmpty()) {
            Slog.i(TAG, "No default found");
            return;
        }
        InputMethodInfo defIm = suitableImes.get(0);
        if (DEBUG) {
            Slog.i(TAG, "Default found, using " + defIm.getId());
        }
        setSelectedInputMethodAndSubtypeLocked(defIm, -1, false);
    }

    private void maybeInitImeNavbarConfigLocked(int targetUserId) {
        Context userContext;
        int profileParentUserId = this.mUserManagerInternal.getProfileParentId(targetUserId);
        OverlayableSystemBooleanResourceWrapper overlayableSystemBooleanResourceWrapper = this.mImeDrawsImeNavBarRes;
        if (overlayableSystemBooleanResourceWrapper != null && overlayableSystemBooleanResourceWrapper.getUserId() != profileParentUserId) {
            this.mImeDrawsImeNavBarRes.close();
            this.mImeDrawsImeNavBarRes = null;
        }
        if (this.mImeDrawsImeNavBarRes == null) {
            if (this.mContext.getUserId() == profileParentUserId) {
                userContext = this.mContext;
            } else {
                Context userContext2 = this.mContext;
                userContext = userContext2.createContextAsUser(UserHandle.of(profileParentUserId), 0);
            }
            this.mImeDrawsImeNavBarRes = OverlayableSystemBooleanResourceWrapper.create(userContext, 17891683, this.mHandler, new Consumer() { // from class: com.android.server.inputmethod.InputMethodManagerService$$ExternalSyntheticLambda9
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    InputMethodManagerService.this.m4022x956a85e7((OverlayableSystemBooleanResourceWrapper) obj);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$maybeInitImeNavbarConfigLocked$0$com-android-server-inputmethod-InputMethodManagerService  reason: not valid java name */
    public /* synthetic */ void m4022x956a85e7(OverlayableSystemBooleanResourceWrapper resource) {
        synchronized (ImfLock.class) {
            if (resource == this.mImeDrawsImeNavBarRes) {
                sendOnNavButtonFlagsChangedLocked();
            }
        }
    }

    private static PackageManager getPackageManagerForUser(Context context, int userId) {
        if (context.getUserId() == userId) {
            return context.getPackageManager();
        }
        return context.createContextAsUser(UserHandle.of(userId), 0).getPackageManager();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void switchUserOnHandlerLocked(int newUserId, IInputMethodClient clientToBeReset) {
        ClientState cs;
        boolean z = DEBUG;
        if (z) {
            Slog.d(TAG, "Switching user stage 1/3. newUserId=" + newUserId + " currentUserId=" + this.mSettings.getCurrentUserId());
        }
        maybeInitImeNavbarConfigLocked(newUserId);
        this.mSettingsObserver.registerContentObserverLocked(newUserId);
        boolean useCopyOnWriteSettings = (this.mSystemReady && this.mUserManagerInternal.isUserUnlockingOrUnlocked(newUserId)) ? false : true;
        this.mSettings.switchCurrentUser(newUserId, useCopyOnWriteSettings);
        updateCurrentProfileIds();
        AdditionalSubtypeUtils.load(this.mAdditionalSubtypeMap, newUserId);
        String defaultImiId = this.mSettings.getSelectedInputMethod();
        if (z) {
            Slog.d(TAG, "Switching user stage 2/3. newUserId=" + newUserId + " defaultImiId=" + defaultImiId);
        }
        boolean initialUserSwitch = TextUtils.isEmpty(defaultImiId);
        this.mLastSystemLocales = this.mRes.getConfiguration().getLocales();
        resetCurrentMethodAndClientLocked(6);
        buildInputMethodListLocked(initialUserSwitch);
        if (TextUtils.isEmpty(this.mSettings.getSelectedInputMethod())) {
            resetDefaultImeLocked(this.mContext);
        }
        updateFromSettingsLocked(true);
        if (initialUserSwitch) {
            InputMethodUtils.setNonSelectedSystemImesDisabledUntilUsed(getPackageManagerForUser(this.mContext, newUserId), this.mSettings.getEnabledInputMethodListLocked());
        }
        if (z) {
            Slog.d(TAG, "Switching user stage 3/3. newUserId=" + newUserId + " selectedIme=" + this.mSettings.getSelectedInputMethod());
        }
        this.mLastSwitchUserId = newUserId;
        if (!this.mIsInteractive || clientToBeReset == null || (cs = this.mClients.get(clientToBeReset.asBinder())) == null) {
            return;
        }
        try {
            cs.client.scheduleStartInputIfNecessary(this.mInFullscreenMode);
        } catch (RemoteException e) {
        }
    }

    void updateCurrentProfileIds() {
        InputMethodUtils.InputMethodSettings inputMethodSettings = this.mSettings;
        inputMethodSettings.setCurrentProfileIds(this.mUserManager.getProfileIdsWithDisabled(inputMethodSettings.getCurrentUserId()));
    }

    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        try {
            return super.onTransact(code, data, reply, flags);
        } catch (RuntimeException e) {
            if (!(e instanceof SecurityException)) {
                Slog.wtf(TAG, "Input Method Manager Crash", e);
            }
            throw e;
        }
    }

    public void systemRunning(StatusBarManagerService statusBar) {
        synchronized (ImfLock.class) {
            try {
                try {
                    if (DEBUG) {
                        Slog.d(TAG, "--- systemReady");
                    }
                    if (!this.mSystemReady) {
                        this.mSystemReady = true;
                        this.mLastSystemLocales = this.mRes.getConfiguration().getLocales();
                        final int currentUserId = this.mSettings.getCurrentUserId();
                        this.mSettings.switchCurrentUser(currentUserId, !this.mUserManagerInternal.isUserUnlockingOrUnlocked(currentUserId));
                        this.mKeyguardManager = (KeyguardManager) this.mContext.getSystemService(KeyguardManager.class);
                        this.mNotificationManager = (NotificationManager) this.mContext.getSystemService(NotificationManager.class);
                        this.mStatusBar = statusBar;
                        hideStatusBarIconLocked();
                        updateSystemUiLocked(this.mImeWindowVis, this.mBackDisposition);
                        boolean z = this.mRes.getBoolean(17891856);
                        this.mShowOngoingImeSwitcherForPhones = z;
                        if (z) {
                            this.mWindowManagerInternal.setOnHardKeyboardStatusChangeListener(new WindowManagerInternal.OnHardKeyboardStatusChangeListener() { // from class: com.android.server.inputmethod.InputMethodManagerService$$ExternalSyntheticLambda3
                                @Override // com.android.server.wm.WindowManagerInternal.OnHardKeyboardStatusChangeListener
                                public final void onHardKeyboardStatusChange(boolean z2) {
                                    InputMethodManagerService.this.m4023x4ec80475(z2);
                                }
                            });
                        }
                        this.mImeDrawsImeNavBarResLazyInitFuture = SystemServerInitThreadPool.submit(new Runnable() { // from class: com.android.server.inputmethod.InputMethodManagerService$$ExternalSyntheticLambda4
                            @Override // java.lang.Runnable
                            public final void run() {
                                InputMethodManagerService.this.m4024xdbb51b94(currentUserId);
                            }
                        }, "Lazily initialize IMMS#mImeDrawsImeNavBarRes");
                        this.mMyPackageMonitor.register(this.mContext, null, UserHandle.ALL, true);
                        this.mSettingsObserver.registerContentObserverLocked(currentUserId);
                        IntentFilter broadcastFilterForSystemUser = new IntentFilter();
                        broadcastFilterForSystemUser.addAction("android.intent.action.USER_ADDED");
                        broadcastFilterForSystemUser.addAction("android.intent.action.USER_REMOVED");
                        broadcastFilterForSystemUser.addAction("android.intent.action.LOCALE_CHANGED");
                        broadcastFilterForSystemUser.addAction(ACTION_SHOW_INPUT_METHOD_PICKER);
                        this.mContext.registerReceiver(new ImmsBroadcastReceiverForSystemUser(), broadcastFilterForSystemUser);
                        IntentFilter broadcastFilterForAllUsers = new IntentFilter();
                        broadcastFilterForAllUsers.addAction("android.intent.action.CLOSE_SYSTEM_DIALOGS");
                        this.mContext.registerReceiverAsUser(new ImmsBroadcastReceiverForAllUsers(), UserHandle.ALL, broadcastFilterForAllUsers, null, null, 2);
                        String defaultImiId = this.mSettings.getSelectedInputMethod();
                        boolean imeSelectedOnBoot = !TextUtils.isEmpty(defaultImiId);
                        buildInputMethodListLocked(imeSelectedOnBoot ? false : true);
                        updateFromSettingsLocked(true);
                        InputMethodUtils.setNonSelectedSystemImesDisabledUntilUsed(getPackageManagerForUser(this.mContext, currentUserId), this.mSettings.getEnabledInputMethodListLocked());
                    }
                } catch (Throwable th) {
                    th = th;
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$systemRunning$1$com-android-server-inputmethod-InputMethodManagerService  reason: not valid java name */
    public /* synthetic */ void m4023x4ec80475(boolean available) {
        this.mHandler.obtainMessage(MSG_HARD_KEYBOARD_SWITCH_CHANGED, available ? 1 : 0, 0).sendToTarget();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$systemRunning$2$com-android-server-inputmethod-InputMethodManagerService  reason: not valid java name */
    public /* synthetic */ void m4024xdbb51b94(int currentUserId) {
        synchronized (ImfLock.class) {
            this.mImeDrawsImeNavBarResLazyInitFuture = null;
            if (currentUserId != this.mSettings.getCurrentUserId()) {
                return;
            }
            maybeInitImeNavbarConfigLocked(currentUserId);
        }
    }

    private int checkDualID(int userid) {
        if (this.mUserManagerInternal.isDualProfile(userid)) {
            return 0;
        }
        return userid;
    }

    private boolean calledFromValidUserLocked() {
        int uid = Binder.getCallingUid();
        int userId = checkDualID(UserHandle.getUserId(uid));
        boolean z = DEBUG;
        if (z) {
            Slog.d(TAG, "--- calledFromForegroundUserOrSystemProcess ? calling uid = " + uid + " system uid = 1000 calling userId = " + userId + ", foreground user id = " + this.mSettings.getCurrentUserId() + ", calling pid = " + Binder.getCallingPid() + InputMethodUtils.getApiCallStack());
        }
        if (uid == 1000 || userId == this.mSettings.getCurrentUserId()) {
            return true;
        }
        if (this.mContext.checkCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS_FULL") == 0) {
            if (z) {
                Slog.d(TAG, "--- Access granted because the calling process has the INTERACT_ACROSS_USERS_FULL permission");
            }
            return true;
        }
        Slog.w(TAG, "--- IPC called from background users. Ignore. callers=" + Debug.getCallers(10));
        return false;
    }

    private boolean calledWithValidTokenLocked(IBinder token) {
        if (token == null) {
            throw new InvalidParameterException("token must not be null.");
        }
        if (token != getCurTokenLocked()) {
            Slog.e(TAG, "Ignoring " + Debug.getCaller() + " due to an invalid token. uid:" + Binder.getCallingUid() + " token:" + token);
            return false;
        }
        return true;
    }

    private List<InputMethodInfo> getInputMethodListInternal(int userId, int directBootAwareness) {
        if (UserHandle.getCallingUserId() != userId) {
            this.mContext.enforceCallingPermission("android.permission.INTERACT_ACROSS_USERS_FULL", null);
        }
        synchronized (ImfLock.class) {
            int[] resolvedUserIds = InputMethodUtils.resolveUserId(userId, this.mSettings.getCurrentUserId(), null);
            if (resolvedUserIds.length != 1) {
                return Collections.emptyList();
            }
            long ident = Binder.clearCallingIdentity();
            List<InputMethodInfo> inputMethodListLocked = getInputMethodListLocked(resolvedUserIds[0], directBootAwareness);
            Binder.restoreCallingIdentity(ident);
            return inputMethodListLocked;
        }
    }

    public List<InputMethodInfo> getInputMethodList(int userId) {
        return getInputMethodListInternal(userId, 0);
    }

    public List<InputMethodInfo> getAwareLockedInputMethodList(int userId, int directBootAwareness) {
        return getInputMethodListInternal(userId, directBootAwareness);
    }

    public List<InputMethodInfo> getEnabledInputMethodList(int userId) {
        if (UserHandle.getCallingUserId() != userId) {
            this.mContext.enforceCallingPermission("android.permission.INTERACT_ACROSS_USERS_FULL", null);
        }
        synchronized (ImfLock.class) {
            int[] resolvedUserIds = InputMethodUtils.resolveUserId(userId, this.mSettings.getCurrentUserId(), null);
            if (resolvedUserIds.length != 1) {
                return Collections.emptyList();
            }
            long ident = Binder.clearCallingIdentity();
            List<InputMethodInfo> imList = getEnabledInputMethodListLocked(resolvedUserIds[0]);
            if (TRAN_SECURITY_INPUT_SUPPORT && imList != null && !imList.isEmpty()) {
                int i = 0;
                while (true) {
                    if (i >= imList.size()) {
                        break;
                    }
                    InputMethodInfo tmpInfo = imList.get(i);
                    if (!"com.transsion.sk/.inputservice.TInputMethodService".equals(tmpInfo.getId())) {
                        i++;
                    } else {
                        imList.remove(tmpInfo);
                        break;
                    }
                }
            }
            Binder.restoreCallingIdentity(ident);
            return imList;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public List<InputMethodInfo> getInputMethodListLocked(int userId, int directBootAwareness) {
        if (userId == this.mSettings.getCurrentUserId() && directBootAwareness == 0) {
            return new ArrayList<>(this.mMethodList);
        }
        ArrayMap<String, InputMethodInfo> methodMap = new ArrayMap<>();
        ArrayList<InputMethodInfo> methodList = new ArrayList<>();
        ArrayMap<String, List<InputMethodSubtype>> additionalSubtypeMap = new ArrayMap<>();
        AdditionalSubtypeUtils.load(additionalSubtypeMap, userId);
        queryInputMethodServicesInternal(this.mContext, userId, additionalSubtypeMap, methodMap, methodList, directBootAwareness);
        return methodList;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public List<InputMethodInfo> getEnabledInputMethodListLocked(int userId) {
        if (userId == this.mSettings.getCurrentUserId()) {
            return this.mSettings.getEnabledInputMethodListLocked();
        }
        ArrayMap<String, InputMethodInfo> methodMap = queryMethodMapForUser(userId);
        InputMethodUtils.InputMethodSettings settings = new InputMethodUtils.InputMethodSettings(this.mContext.getResources(), this.mContext.getContentResolver(), methodMap, userId, true);
        return settings.getEnabledInputMethodListLocked();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onCreateInlineSuggestionsRequestLocked(int userId, InlineSuggestionsRequestInfo requestInfo, IInlineSuggestionsRequestCallback callback, boolean touchExplorationEnabled) {
        clearPendingInlineSuggestionsRequestLocked();
        this.mInlineSuggestionsRequestCallback = callback;
        InputMethodInfo imi = this.mMethodMap.get(getSelectedMethodIdLocked());
        try {
            if (userId == this.mSettings.getCurrentUserId() && imi != null && isInlineSuggestionsEnabled(imi, touchExplorationEnabled)) {
                this.mPendingInlineSuggestionsRequest = new CreateInlineSuggestionsRequest(requestInfo, callback, imi.getPackageName());
                if (getCurMethodLocked() != null) {
                    performOnCreateInlineSuggestionsRequestLocked();
                } else if (DEBUG) {
                    Slog.d(TAG, "IME not connected. Delaying inline suggestions request.");
                }
            } else {
                callback.onInlineSuggestionsUnsupported();
            }
        } catch (RemoteException e) {
            Slog.w(TAG, "RemoteException calling onCreateInlineSuggestionsRequest(): " + e);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void performOnCreateInlineSuggestionsRequestLocked() {
        if (this.mPendingInlineSuggestionsRequest == null) {
            return;
        }
        IInputMethodInvoker curMethod = getCurMethodLocked();
        if (DEBUG) {
            Slog.d(TAG, "Performing onCreateInlineSuggestionsRequest. mCurMethod = " + curMethod);
        }
        if (curMethod != null) {
            curMethod.onCreateInlineSuggestionsRequest(this.mPendingInlineSuggestionsRequest.mRequestInfo, new InlineSuggestionsRequestCallbackDecorator(this.mPendingInlineSuggestionsRequest.mCallback, this.mPendingInlineSuggestionsRequest.mPackageName, this.mCurTokenDisplayId, getCurTokenLocked(), this));
        } else {
            Slog.w(TAG, "No IME connected! Abandoning inline suggestions creation request.");
        }
        clearPendingInlineSuggestionsRequestLocked();
    }

    private void clearPendingInlineSuggestionsRequestLocked() {
        this.mPendingInlineSuggestionsRequest = null;
    }

    private static boolean isInlineSuggestionsEnabled(InputMethodInfo imi, boolean touchExplorationEnabled) {
        return imi.isInlineSuggestionsEnabled() && (!touchExplorationEnabled || imi.supportsInlineSuggestionsWithTouchExploration());
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static final class InlineSuggestionsRequestCallbackDecorator extends IInlineSuggestionsRequestCallback.Stub {
        private final IInlineSuggestionsRequestCallback mCallback;
        private final int mImeDisplayId;
        private final String mImePackageName;
        private final IBinder mImeToken;
        private final InputMethodManagerService mImms;

        InlineSuggestionsRequestCallbackDecorator(IInlineSuggestionsRequestCallback callback, String imePackageName, int displayId, IBinder imeToken, InputMethodManagerService imms) {
            this.mCallback = callback;
            this.mImePackageName = imePackageName;
            this.mImeDisplayId = displayId;
            this.mImeToken = imeToken;
            this.mImms = imms;
        }

        public void onInlineSuggestionsUnsupported() throws RemoteException {
            this.mCallback.onInlineSuggestionsUnsupported();
        }

        public void onInlineSuggestionsRequest(InlineSuggestionsRequest request, IInlineSuggestionsResponseCallback callback) throws RemoteException {
            if (!this.mImePackageName.equals(request.getHostPackageName())) {
                throw new SecurityException("Host package name in the provide request=[" + request.getHostPackageName() + "] doesn't match the IME package name=[" + this.mImePackageName + "].");
            }
            request.setHostDisplayId(this.mImeDisplayId);
            this.mImms.setCurHostInputToken(this.mImeToken, request.getHostInputToken());
            this.mCallback.onInlineSuggestionsRequest(request, callback);
        }

        public void onInputMethodStartInput(AutofillId imeFieldId) throws RemoteException {
            this.mCallback.onInputMethodStartInput(imeFieldId);
        }

        public void onInputMethodShowInputRequested(boolean requestResult) throws RemoteException {
            this.mCallback.onInputMethodShowInputRequested(requestResult);
        }

        public void onInputMethodStartInputView() throws RemoteException {
            this.mCallback.onInputMethodStartInputView();
        }

        public void onInputMethodFinishInputView() throws RemoteException {
            this.mCallback.onInputMethodFinishInputView();
        }

        public void onInputMethodFinishInput() throws RemoteException {
            this.mCallback.onInputMethodFinishInput();
        }

        public void onInlineSuggestionsSessionInvalidated() throws RemoteException {
            this.mCallback.onInlineSuggestionsSessionInvalidated();
        }
    }

    void setCurHostInputToken(IBinder callerImeToken, IBinder hostInputToken) {
        synchronized (ImfLock.class) {
            if (calledWithValidTokenLocked(callerImeToken)) {
                this.mCurHostInputToken = hostInputToken;
            }
        }
    }

    public List<InputMethodSubtype> getEnabledInputMethodSubtypeList(String imiId, boolean allowsImplicitlySelectedSubtypes) {
        int callingUserId = UserHandle.getCallingUserId();
        synchronized (ImfLock.class) {
            int[] resolvedUserIds = InputMethodUtils.resolveUserId(callingUserId, this.mSettings.getCurrentUserId(), null);
            if (resolvedUserIds.length != 1) {
                return Collections.emptyList();
            }
            long ident = Binder.clearCallingIdentity();
            List<InputMethodSubtype> enabledInputMethodSubtypeListLocked = getEnabledInputMethodSubtypeListLocked(imiId, allowsImplicitlySelectedSubtypes, resolvedUserIds[0]);
            Binder.restoreCallingIdentity(ident);
            return enabledInputMethodSubtypeListLocked;
        }
    }

    private List<InputMethodSubtype> getEnabledInputMethodSubtypeListLocked(String imiId, boolean allowsImplicitlySelectedSubtypes, int userId) {
        InputMethodInfo imi;
        if (userId == this.mSettings.getCurrentUserId()) {
            String selectedMethodId = getSelectedMethodIdLocked();
            if (imiId == null && selectedMethodId != null) {
                imi = this.mMethodMap.get(selectedMethodId);
            } else {
                imi = this.mMethodMap.get(imiId);
            }
            if (imi == null) {
                return Collections.emptyList();
            }
            return this.mSettings.getEnabledInputMethodSubtypeListLocked(this.mContext, imi, allowsImplicitlySelectedSubtypes);
        }
        ArrayMap<String, InputMethodInfo> methodMap = queryMethodMapForUser(userId);
        InputMethodInfo imi2 = methodMap.get(imiId);
        if (imi2 == null) {
            return Collections.emptyList();
        }
        InputMethodUtils.InputMethodSettings settings = new InputMethodUtils.InputMethodSettings(this.mContext.getResources(), this.mContext.getContentResolver(), methodMap, userId, true);
        return settings.getEnabledInputMethodSubtypeListLocked(this.mContext, imi2, allowsImplicitlySelectedSubtypes);
    }

    public void clearClient(IInputMethodClient client) {
        removeClient(client);
    }

    public void addClient(IInputMethodClient client, IInputContext inputContext, int selfReportedDisplayId) {
        int callerUid = Binder.getCallingUid();
        int callerPid = Binder.getCallingPid();
        synchronized (ImfLock.class) {
            try {
                int numClients = this.mClients.size();
                for (int i = 0; i < numClients; i++) {
                    ClientState state = this.mClients.valueAt(i);
                    if (state.uid == callerUid && state.pid == callerPid && state.selfReportedDisplayId == selfReportedDisplayId) {
                        throw new SecurityException("uid=" + callerUid + "/pid=" + callerPid + "/displayId=" + selfReportedDisplayId + " is already registered.");
                    }
                }
            } catch (Throwable th) {
                e = th;
            }
            try {
                ClientDeathRecipient deathRecipient = new ClientDeathRecipient(this, client);
                try {
                    client.asBinder().linkToDeath(deathRecipient, 0);
                    this.mClients.put(client.asBinder(), new ClientState(client, inputContext, callerUid, callerPid, selfReportedDisplayId, deathRecipient));
                } catch (RemoteException e) {
                    throw new IllegalStateException(e);
                }
            } catch (Throwable th2) {
                e = th2;
                throw e;
            }
        }
    }

    void removeClient(IInputMethodClient client) {
        synchronized (ImfLock.class) {
            ClientState cs = this.mClients.remove(client.asBinder());
            if (cs != null) {
                client.asBinder().unlinkToDeath(cs.clientDeathRecipient, 0);
                clearClientSessionLocked(cs);
                clearClientSessionForAccessibilityLocked(cs);
                int numItems = this.mVirtualDisplayIdToParentMap.size();
                for (int i = numItems - 1; i >= 0; i--) {
                    VirtualDisplayInfo info = this.mVirtualDisplayIdToParentMap.valueAt(i);
                    if (info.mParentClient == cs) {
                        this.mVirtualDisplayIdToParentMap.removeAt(i);
                    }
                }
                if (this.mCurClient == cs) {
                    hideCurrentInputLocked(this.mCurFocusedWindow, 0, null, 21);
                    if (this.mBoundToMethod) {
                        this.mBoundToMethod = false;
                        IInputMethodInvoker curMethod = getCurMethodLocked();
                        if (curMethod != null) {
                            curMethod.unbindInput();
                            AccessibilityManagerInternal.get().unbindInput();
                        }
                    }
                    this.mBoundToAccessibility = false;
                    this.mCurClient = null;
                    this.mCurVirtualDisplayToScreenMatrix = null;
                }
                if (this.mCurFocusedWindowClient == cs) {
                    this.mCurFocusedWindowClient = null;
                }
            }
        }
    }

    private Message obtainMessageOO(int what, Object arg1, Object arg2) {
        SomeArgs args = SomeArgs.obtain();
        args.arg1 = arg1;
        args.arg2 = arg2;
        return this.mHandler.obtainMessage(what, 0, 0, args);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public Message obtainMessageOOO(int what, Object arg1, Object arg2, Object arg3) {
        SomeArgs args = SomeArgs.obtain();
        args.arg1 = arg1;
        args.arg2 = arg2;
        args.arg3 = arg3;
        return this.mHandler.obtainMessage(what, 0, 0, args);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public Message obtainMessageIIOO(int what, int arg1, int arg2, Object arg3, Object arg4) {
        SomeArgs args = SomeArgs.obtain();
        args.arg1 = arg3;
        args.arg2 = arg4;
        return this.mHandler.obtainMessage(what, arg1, arg2, args);
    }

    private Message obtainMessageIIIO(int what, int argi1, int argi2, int argi3, Object arg1) {
        SomeArgs args = SomeArgs.obtain();
        args.arg1 = arg1;
        args.argi1 = argi1;
        args.argi2 = argi2;
        args.argi3 = argi3;
        return this.mHandler.obtainMessage(what, 0, 0, args);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void executeOrSendMessage(IInputMethodClient target, Message msg) {
        if (target.asBinder() instanceof Binder) {
            msg.sendToTarget();
            return;
        }
        handleMessage(msg);
        msg.recycle();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void unbindCurrentClientLocked(int unbindClientReason) {
        if (this.mCurClient != null) {
            if (DEBUG) {
                Slog.v(TAG, "unbindCurrentInputLocked: client=" + this.mCurClient.client.asBinder());
            }
            if (this.mBoundToMethod) {
                this.mBoundToMethod = false;
                IInputMethodInvoker curMethod = getCurMethodLocked();
                if (curMethod != null) {
                    curMethod.unbindInput();
                }
            }
            this.mBoundToAccessibility = false;
            scheduleSetActiveToClient(this.mCurClient, false, false, false);
            executeOrSendMessage(this.mCurClient.client, this.mHandler.obtainMessage(MSG_UNBIND_CLIENT, getSequenceNumberLocked(), unbindClientReason, this.mCurClient.client));
            this.mCurClient.sessionRequested = false;
            this.mCurClient.mSessionRequestedForAccessibility = false;
            this.mCurClient = null;
            this.mCurVirtualDisplayToScreenMatrix = null;
            this.mMenuController.hideInputMethodMenuLocked();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void clearInputShowRequestLocked() {
        this.mShowRequested = this.mInputShown;
        this.mInputShown = false;
        ITranInputMethodManagerService.Instance().hookInputMethodShown(false);
        hookIMEVisibleChanged(this.mInputShown);
        ITranInputMethodManagerService.Instance().hookInputMethodShown(this.mInputShown);
        ITranInputMethodManagerService.Instance().setConnectInputShow(this.mInputShown);
    }

    private int getImeShowFlagsLocked() {
        if (this.mShowForced) {
            int flags = 0 | 3;
            return flags;
        } else if (!this.mShowExplicitlyRequested) {
            return 0;
        } else {
            int flags2 = 0 | 1;
            return flags2;
        }
    }

    private int getAppShowFlagsLocked() {
        if (this.mShowForced) {
            int flags = 0 | 2;
            return flags;
        } else if (this.mShowExplicitlyRequested) {
            return 0;
        } else {
            int flags2 = 0 | 1;
            return flags2;
        }
    }

    InputBindResult attachNewInputLocked(int startInputReason, boolean initial) {
        boolean z = true;
        if (!this.mBoundToMethod) {
            getCurMethodLocked().bindInput(this.mCurClient.binding);
            this.mBoundToMethod = true;
        }
        boolean restarting = !initial;
        Binder startInputToken = new Binder();
        StartInputInfo info = new StartInputInfo(this.mSettings.getCurrentUserId(), getCurTokenLocked(), this.mCurTokenDisplayId, getCurIdLocked(), startInputReason, restarting, UserHandle.getUserId(this.mCurClient.uid), this.mCurClient.selfReportedDisplayId, this.mCurFocusedWindow, this.mCurAttribute, this.mCurFocusedWindowSoftInputMode, getSequenceNumberLocked());
        this.mImeTargetWindowMap.put(startInputToken, this.mCurFocusedWindow);
        this.mStartInputHistory.addEntry(info);
        if (this.mSettings.getCurrentUserId() == UserHandle.getUserId(this.mCurClient.uid)) {
            this.mPackageManagerInternal.grantImplicitAccess(this.mSettings.getCurrentUserId(), null, UserHandle.getAppId(getCurMethodUidLocked()), this.mCurClient.uid, true);
        }
        int navButtonFlags = getInputMethodNavButtonFlagsLocked();
        SessionState session = this.mCurClient.curSession;
        setEnabledSessionLocked(session);
        session.method.startInput(startInputToken, this.mCurInputContext, this.mCurAttribute, restarting, navButtonFlags, this.mCurImeDispatcher);
        if (this.mShowRequested) {
            if (DEBUG) {
                Slog.v(TAG, "Attach new input asks to show input");
            }
            showCurrentInputLocked(this.mCurFocusedWindow, getAppShowFlagsLocked(), null, 1);
        }
        String curId = getCurIdLocked();
        InputMethodInfo curInputMethodInfo = this.mMethodMap.get(curId);
        boolean suppressesSpellChecker = (curInputMethodInfo == null || !curInputMethodInfo.suppressesSpellChecker()) ? false : false;
        SparseArray<IAccessibilityInputMethodSession> accessibilityInputMethodSessions = createAccessibilityInputMethodSessions(this.mCurClient.mAccessibilitySessions);
        return new InputBindResult(0, session.session, accessibilityInputMethodSessions, session.channel != null ? session.channel.dup() : null, curId, getSequenceNumberLocked(), this.mCurVirtualDisplayToScreenMatrix, suppressesSpellChecker);
    }

    private Matrix getVirtualDisplayToScreenMatrixLocked(int clientDisplayId, int imeDisplayId) {
        if (clientDisplayId == imeDisplayId) {
            return null;
        }
        int displayId = clientDisplayId;
        Matrix matrix = null;
        while (true) {
            VirtualDisplayInfo info = this.mVirtualDisplayIdToParentMap.get(displayId);
            if (info == null) {
                return null;
            }
            if (matrix == null) {
                matrix = new Matrix(info.mMatrix);
            } else {
                matrix.postConcat(info.mMatrix);
            }
            if (info.mParentClient.selfReportedDisplayId == imeDisplayId) {
                return matrix;
            }
            displayId = info.mParentClient.selfReportedDisplayId;
        }
    }

    InputBindResult attachNewAccessibilityLocked(int startInputReason, boolean initial, int id) {
        if (!this.mBoundToAccessibility) {
            AccessibilityManagerInternal.get().bindInput();
            this.mBoundToAccessibility = true;
        }
        AccessibilitySessionState accessibilitySession = this.mCurClient.mAccessibilitySessions.get(id);
        if (startInputReason != 11) {
            new Binder();
            setEnabledSessionForAccessibilityLocked(this.mCurClient.mAccessibilitySessions);
            AccessibilityManagerInternal.get().startInput(this.mCurRemoteAccessibilityInputConnection, this.mCurAttribute, !initial);
        }
        if (accessibilitySession == null) {
            return null;
        }
        SessionState session = this.mCurClient.curSession;
        IInputMethodSession imeSession = session != null ? session.session : null;
        SparseArray<IAccessibilityInputMethodSession> accessibilityInputMethodSessions = createAccessibilityInputMethodSessions(this.mCurClient.mAccessibilitySessions);
        return new InputBindResult(16, imeSession, accessibilityInputMethodSessions, (InputChannel) null, getCurIdLocked(), getSequenceNumberLocked(), this.mCurVirtualDisplayToScreenMatrix, false);
    }

    private SparseArray<IAccessibilityInputMethodSession> createAccessibilityInputMethodSessions(SparseArray<AccessibilitySessionState> accessibilitySessions) {
        SparseArray<IAccessibilityInputMethodSession> accessibilityInputMethodSessions = new SparseArray<>();
        if (accessibilitySessions != null) {
            for (int i = 0; i < accessibilitySessions.size(); i++) {
                accessibilityInputMethodSessions.append(accessibilitySessions.keyAt(i), accessibilitySessions.valueAt(i).mSession);
            }
        }
        return accessibilityInputMethodSessions;
    }

    private InputBindResult startInputUncheckedLocked(ClientState cs, IInputContext inputContext, IRemoteAccessibilityInputConnection remoteAccessibilityInputConnection, EditorInfo attribute, int startInputFlags, int startInputReason, int unverifiedTargetSdkVersion, ImeOnBackInvokedDispatcher imeDispatcher) {
        String selectedMethodId = getSelectedMethodIdLocked();
        if (selectedMethodId == null) {
            return InputBindResult.NO_IME;
        }
        if (!this.mSystemReady) {
            return new InputBindResult(8, (IInputMethodSession) null, (SparseArray) null, (InputChannel) null, selectedMethodId, getSequenceNumberLocked(), (Matrix) null, false);
        }
        if (!InputMethodUtils.checkIfPackageBelongsToUid(this.mAppOpsManager, cs.uid, attribute.packageName)) {
            Slog.e(TAG, "Rejecting this client as it reported an invalid package name. uid=" + cs.uid + " package=" + attribute.packageName);
            return InputBindResult.INVALID_PACKAGE_NAME;
        }
        boolean isKeyguardLock = isKeyguardLocked();
        boolean isSplitScreen = this.mActivityTaskManagerInternal.isSplitScreen();
        if (attribute != null) {
            Slog.i("input_debug", "package: " + attribute.packageName + " isPassWordInput: " + isPassWordInput(attribute.inputType) + " inputType: " + attribute.inputType + " mMethodMap: " + (this.mMethodMap.get("com.transsion.sk/.inputservice.TInputMethodService") != null) + " isKeyguardShowing: " + isKeyguardLock + " enable: " + this.mSecurityInputEnable + " isSplitScreen: " + isSplitScreen);
        }
        if (TRAN_SECURITY_INPUT_SUPPORT) {
            synchronized (this.mSecurityLock) {
                if (this.mSecurityInputEnable != 0 && attribute != null && !this.mSecurityInputBlackList.contains(attribute.packageName) && isPassWordInput(attribute.inputType) && this.mMethodMap.get("com.transsion.sk/.inputservice.TInputMethodService") != null && !isKeyguardLock && !isSplitScreen) {
                    setSelectedMethodIdLocked("com.transsion.sk/.inputservice.TInputMethodService");
                } else if ("com.transsion.sk/.inputservice.TInputMethodService".equals(selectedMethodId)) {
                    updateSystemUiLocked(0, this.mBackDisposition);
                    String defaultImiId = this.mSettings.getSelectedInputMethod();
                    setSelectedMethodIdLocked(defaultImiId);
                }
            }
        }
        int computeImeDisplayIdForTarget = computeImeDisplayIdForTarget(cs.selfReportedDisplayId, this.mImeDisplayValidator);
        this.mDisplayIdToShowIme = computeImeDisplayIdForTarget;
        if (computeImeDisplayIdForTarget == -1) {
            this.mImeHiddenByDisplayPolicy = true;
            hideCurrentInputLocked(this.mCurFocusedWindow, 0, null, 26);
            return InputBindResult.NO_IME;
        }
        this.mImeHiddenByDisplayPolicy = false;
        if (this.mCurClient != cs) {
            prepareClientSwitchLocked(cs);
        }
        advanceSequenceNumberLocked();
        this.mCurClient = cs;
        this.mCurInputContext = inputContext;
        this.mCurRemoteAccessibilityInputConnection = remoteAccessibilityInputConnection;
        this.mCurImeDispatcher = imeDispatcher;
        this.mCurVirtualDisplayToScreenMatrix = getVirtualDisplayToScreenMatrixLocked(cs.selfReportedDisplayId, this.mDisplayIdToShowIme);
        this.mCurAttribute = attribute;
        if (shouldPreventImeStartupLocked(selectedMethodId, startInputFlags, unverifiedTargetSdkVersion)) {
            if (DEBUG) {
                Slog.d(TAG, "Avoiding IME startup and unbinding current input method.");
            }
            invalidateAutofillSessionLocked();
            this.mBindingController.unbindCurrentMethod();
            return InputBindResult.NO_EDITOR;
        }
        if (isSelectedMethodBoundLocked()) {
            if (cs.curSession != null) {
                cs.mSessionRequestedForAccessibility = false;
                requestClientSessionForAccessibilityLocked(cs);
                attachNewAccessibilityLocked(startInputReason, (startInputFlags & 4) != 0, -1);
                return attachNewInputLocked(startInputReason, (startInputFlags & 4) != 0);
            }
            InputBindResult bindResult = tryReuseConnectionLocked(cs);
            if (bindResult != null) {
                return bindResult;
            }
        }
        this.mBindingController.unbindCurrentMethod();
        Intent currentIntent = getCurIntentLocked();
        ITranInputMethodManagerService.Instance().hookStartInputUncheckedLocked(currentIntent);
        return this.mBindingController.bindCurrentMethod();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void invalidateAutofillSessionLocked() {
        IInlineSuggestionsRequestCallback iInlineSuggestionsRequestCallback = this.mInlineSuggestionsRequestCallback;
        if (iInlineSuggestionsRequestCallback != null) {
            try {
                iInlineSuggestionsRequestCallback.onInlineSuggestionsSessionInvalidated();
            } catch (RemoteException e) {
                Slog.e(TAG, "Cannot invalidate autofill session.", e);
            }
        }
    }

    private boolean shouldPreventImeStartupLocked(String selectedMethodId, int startInputFlags, int unverifiedTargetSdkVersion) {
        if (this.mPreventImeStartupUnlessTextEditor) {
            boolean imeVisibleAllowed = InputMethodUtils.isSoftInputModeStateVisibleAllowed(unverifiedTargetSdkVersion, startInputFlags);
            return (imeVisibleAllowed || this.mShowRequested || isNonPreemptibleImeLocked(selectedMethodId)) ? false : true;
        }
        return false;
    }

    private boolean isNonPreemptibleImeLocked(String selectedMethodId) {
        InputMethodInfo imi = this.mMethodMap.get(selectedMethodId);
        if (imi != null) {
            return this.mNonPreemptibleInputMethods.contains(imi.getPackageName());
        }
        return false;
    }

    private boolean isSelectedMethodBoundLocked() {
        String curId = getCurIdLocked();
        return curId != null && curId.equals(getSelectedMethodIdLocked()) && this.mDisplayIdToShowIme == this.mCurTokenDisplayId;
    }

    private void prepareClientSwitchLocked(ClientState cs) {
        unbindCurrentClientLocked(1);
        if (this.mIsInteractive) {
            scheduleSetActiveToClient(cs, true, false, false);
        }
    }

    private InputBindResult tryReuseConnectionLocked(ClientState cs) {
        if (hasConnectionLocked()) {
            if (getCurMethodLocked() != null) {
                requestClientSessionLocked(cs);
                requestClientSessionForAccessibilityLocked(cs);
                return new InputBindResult(1, (IInputMethodSession) null, (SparseArray) null, (InputChannel) null, getCurIdLocked(), getSequenceNumberLocked(), (Matrix) null, false);
            }
            long bindingDuration = SystemClock.uptimeMillis() - getLastBindTimeLocked();
            if (bindingDuration < BackupAgentTimeoutParameters.DEFAULT_QUOTA_EXCEEDED_TIMEOUT_MILLIS) {
                return new InputBindResult(2, (IInputMethodSession) null, (SparseArray) null, (InputChannel) null, getCurIdLocked(), getSequenceNumberLocked(), (Matrix) null, false);
            }
            EventLog.writeEvent((int) EventLogTags.IMF_FORCE_RECONNECT_IME, getSelectedMethodIdLocked(), Long.valueOf(bindingDuration), 0);
            return null;
        }
        return null;
    }

    static int computeImeDisplayIdForTarget(int displayId, ImeDisplayValidator checker) {
        if (displayId == 0 || displayId == -1) {
            return 0;
        }
        int result = checker.getDisplayImePolicy(displayId);
        if (result == 0) {
            return displayId;
        }
        return result == 2 ? -1 : 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void initializeImeLocked(IInputMethodInvoker inputMethod, IBinder token, int configChanges, boolean supportStylusHw) {
        if (DEBUG) {
            Slog.v(TAG, "Sending attach of token: " + token + " for display: " + this.mCurTokenDisplayId);
        }
        inputMethod.initializeInternal(token, new InputMethodPrivilegedOperationsImpl(this, token), configChanges, supportStylusHw, getInputMethodNavButtonFlagsLocked());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void scheduleResetStylusHandwriting() {
        this.mHandler.obtainMessage(MSG_RESET_HANDWRITING).sendToTarget();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void scheduleNotifyImeUidToAudioService(int uid) {
        this.mHandler.removeMessages(MSG_NOTIFY_IME_UID_TO_AUDIO_SERVICE);
        this.mHandler.obtainMessage(MSG_NOTIFY_IME_UID_TO_AUDIO_SERVICE, uid, 0).sendToTarget();
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [3159=4] */
    void onSessionCreated(IInputMethodInvoker method, IInputMethodSession session, InputChannel channel) {
        ClientState clientState;
        Trace.traceBegin(32L, "IMMS.onSessionCreated");
        try {
            synchronized (ImfLock.class) {
                if (this.mUserSwitchHandlerTask != null) {
                    channel.dispose();
                    return;
                }
                IInputMethodInvoker curMethod = getCurMethodLocked();
                if (curMethod == null || method == null || curMethod.asBinder() != method.asBinder() || (clientState = this.mCurClient) == null) {
                    channel.dispose();
                    return;
                }
                clearClientSessionLocked(clientState);
                ClientState clientState2 = this.mCurClient;
                clientState2.curSession = new SessionState(clientState2, method, session, channel);
                InputBindResult res = attachNewInputLocked(10, true);
                attachNewAccessibilityLocked(10, true, -1);
                if (res.method != null) {
                    executeOrSendMessage(this.mCurClient.client, obtainMessageOO(3010, this.mCurClient.client, res));
                }
            }
        } finally {
            Trace.traceEnd(32L);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void resetSystemUiLocked() {
        this.mImeWindowVis = 0;
        this.mBackDisposition = 0;
        updateSystemUiLocked(0, 0);
        this.mCurTokenDisplayId = -1;
        this.mCurHostInputToken = null;
    }

    void resetCurrentMethodAndClientLocked(int unbindClientReason) {
        setSelectedMethodIdLocked(null);
        this.mBindingController.unbindCurrentMethod();
        unbindCurrentClientLocked(unbindClientReason);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void reRequestCurrentClientSessionLocked() {
        ClientState clientState = this.mCurClient;
        if (clientState != null) {
            clearClientSessionLocked(clientState);
            clearClientSessionForAccessibilityLocked(this.mCurClient);
            requestClientSessionLocked(this.mCurClient);
            requestClientSessionForAccessibilityLocked(this.mCurClient);
        }
    }

    void requestClientSessionLocked(ClientState cs) {
        if (!cs.sessionRequested) {
            if (DEBUG) {
                Slog.v(TAG, "Creating new session for client " + cs);
            }
            InputChannel[] channels = InputChannel.openInputChannelPair(cs.toString());
            final InputChannel serverChannel = channels[0];
            InputChannel clientChannel = channels[1];
            cs.sessionRequested = true;
            final IInputMethodInvoker curMethod = getCurMethodLocked();
            IInputSessionCallback.Stub callback = new IInputSessionCallback.Stub() { // from class: com.android.server.inputmethod.InputMethodManagerService.1
                public void sessionCreated(IInputMethodSession session) {
                    long ident = Binder.clearCallingIdentity();
                    try {
                        InputMethodManagerService.this.onSessionCreated(curMethod, session, serverChannel);
                    } finally {
                        Binder.restoreCallingIdentity(ident);
                    }
                }
            };
            try {
                curMethod.createSession(clientChannel, callback);
            } finally {
                if (clientChannel != null) {
                    clientChannel.dispose();
                }
            }
        }
    }

    void requestClientSessionForAccessibilityLocked(ClientState cs) {
        if (!cs.mSessionRequestedForAccessibility) {
            if (DEBUG) {
                Slog.v(TAG, "Creating new accessibility sessions for client " + cs);
            }
            cs.mSessionRequestedForAccessibility = true;
            ArraySet<Integer> ignoreSet = new ArraySet<>();
            for (int i = 0; i < cs.mAccessibilitySessions.size(); i++) {
                ignoreSet.add(Integer.valueOf(cs.mAccessibilitySessions.keyAt(i)));
            }
            AccessibilityManagerInternal.get().createImeSession(ignoreSet);
        }
    }

    void clearClientSessionLocked(ClientState cs) {
        finishSessionLocked(cs.curSession);
        cs.curSession = null;
        cs.sessionRequested = false;
    }

    void clearClientSessionForAccessibilityLocked(ClientState cs) {
        for (int i = 0; i < cs.mAccessibilitySessions.size(); i++) {
            finishSessionForAccessibilityLocked(cs.mAccessibilitySessions.valueAt(i));
        }
        cs.mAccessibilitySessions.clear();
        cs.mSessionRequestedForAccessibility = false;
    }

    void clearClientSessionForAccessibilityLocked(ClientState cs, int id) {
        AccessibilitySessionState session = cs.mAccessibilitySessions.get(id);
        if (session != null) {
            finishSessionForAccessibilityLocked(session);
            cs.mAccessibilitySessions.remove(id);
        }
    }

    private void finishSessionLocked(SessionState sessionState) {
        if (sessionState != null) {
            if (sessionState.session != null) {
                try {
                    sessionState.session.finishSession();
                } catch (RemoteException e) {
                    Slog.w(TAG, "Session failed to close due to remote exception", e);
                    updateSystemUiLocked(0, this.mBackDisposition);
                }
                sessionState.session = null;
            }
            if (sessionState.channel != null) {
                sessionState.channel.dispose();
                sessionState.channel = null;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void finishSessionForAccessibilityLocked(AccessibilitySessionState sessionState) {
        if (sessionState != null && sessionState.mSession != null) {
            try {
                sessionState.mSession.finishSession();
            } catch (RemoteException e) {
                Slog.w(TAG, "Session failed to close due to remote exception", e);
            }
            sessionState.mSession = null;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void clearClientSessionsLocked() {
        if (getCurMethodLocked() != null) {
            int numClients = this.mClients.size();
            for (int i = 0; i < numClients; i++) {
                clearClientSessionLocked(this.mClients.valueAt(i));
                clearClientSessionForAccessibilityLocked(this.mClients.valueAt(i));
            }
            finishSessionLocked(this.mEnabledSession);
            for (int i2 = 0; i2 < this.mEnabledAccessibilitySessions.size(); i2++) {
                finishSessionForAccessibilityLocked(this.mEnabledAccessibilitySessions.valueAt(i2));
            }
            this.mEnabledSession = null;
            this.mEnabledAccessibilitySessions.clear();
            scheduleNotifyImeUidToAudioService(-1);
        }
        hideStatusBarIconLocked();
        this.mInFullscreenMode = false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateStatusIcon(IBinder token, String packageName, int iconId) {
        synchronized (ImfLock.class) {
            if (calledWithValidTokenLocked(token)) {
                long ident = Binder.clearCallingIdentity();
                if (iconId == 0) {
                    if (DEBUG) {
                        Slog.d(TAG, "hide the small icon for the input method");
                    }
                    hideStatusBarIconLocked();
                } else if (packageName != null) {
                    if (DEBUG) {
                        Slog.d(TAG, "show a small icon for the input method");
                    }
                    CharSequence contentDescription = null;
                    try {
                        PackageManager packageManager = this.mContext.getPackageManager();
                        contentDescription = packageManager.getApplicationLabel(this.mIPackageManager.getApplicationInfo(packageName, 0L, this.mSettings.getCurrentUserId()));
                    } catch (RemoteException e) {
                    }
                    StatusBarManagerService statusBarManagerService = this.mStatusBar;
                    if (statusBarManagerService != null) {
                        statusBarManagerService.setIcon(this.mSlotIme, packageName, iconId, 0, contentDescription != null ? contentDescription.toString() : null);
                        this.mStatusBar.setIconVisibility(this.mSlotIme, true);
                    }
                }
                Binder.restoreCallingIdentity(ident);
            }
        }
    }

    private void hideStatusBarIconLocked() {
        StatusBarManagerService statusBarManagerService = this.mStatusBar;
        if (statusBarManagerService != null) {
            statusBarManagerService.setIconVisibility(this.mSlotIme, false);
        }
    }

    private int getInputMethodNavButtonFlagsLocked() {
        Future<?> future = this.mImeDrawsImeNavBarResLazyInitFuture;
        if (future != null) {
            ConcurrentUtils.waitForFutureNoInterrupt(future, "Waiting for the lazy init of mImeDrawsImeNavBarRes");
        }
        OverlayableSystemBooleanResourceWrapper overlayableSystemBooleanResourceWrapper = this.mImeDrawsImeNavBarRes;
        boolean canImeDrawsImeNavBar = overlayableSystemBooleanResourceWrapper != null && overlayableSystemBooleanResourceWrapper.get();
        boolean shouldShowImeSwitcherWhenImeIsShown = shouldShowImeSwitcherLocked(3);
        return (canImeDrawsImeNavBar ? 1 : 0) | (shouldShowImeSwitcherWhenImeIsShown ? 2 : 0);
    }

    private boolean shouldShowImeSwitcherLocked(int visibility) {
        KeyguardManager keyguardManager;
        if (this.mShowOngoingImeSwitcherForPhones && this.mMenuController.getSwitchingDialogLocked() == null) {
            if ((this.mWindowManagerInternal.isKeyguardShowingAndNotOccluded() && (keyguardManager = this.mKeyguardManager) != null && keyguardManager.isKeyguardSecure()) || (visibility & 1) == 0 || (visibility & 4) != 0) {
                return false;
            }
            boolean z = TRAN_SECURITY_INPUT_SUPPORT;
            if (z && "com.transsion.sk/.inputservice.TInputMethodService".equals(getSelectedMethodIdLocked())) {
                return false;
            }
            if (this.mWindowManagerInternal.isHardKeyboardAvailable()) {
                return true;
            }
            if ((visibility & 2) == 0) {
                return false;
            }
            List<InputMethodInfo> imis = this.mSettings.getEnabledInputMethodListWithFilterLocked(new Predicate() { // from class: com.android.server.inputmethod.InputMethodManagerService$$ExternalSyntheticLambda1
                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    return ((InputMethodInfo) obj).shouldShowInInputMethodPicker();
                }
            });
            if (z && !imis.isEmpty()) {
                int i = 0;
                while (true) {
                    if (i >= imis.size()) {
                        break;
                    }
                    InputMethodInfo tmpInfo = imis.get(i);
                    if (!"com.transsion.sk/.inputservice.TInputMethodService".equals(tmpInfo.getId())) {
                        i++;
                    } else {
                        imis.remove(tmpInfo);
                        break;
                    }
                }
            }
            int N = imis.size();
            if (N > 2) {
                return true;
            }
            if (N < 1) {
                return false;
            }
            int nonAuxCount = 0;
            int auxCount = 0;
            InputMethodSubtype nonAuxSubtype = null;
            InputMethodSubtype auxSubtype = null;
            for (int i2 = 0; i2 < N; i2++) {
                InputMethodInfo imi = imis.get(i2);
                List<InputMethodSubtype> subtypes = this.mSettings.getEnabledInputMethodSubtypeListLocked(this.mContext, imi, true);
                int subtypeCount = subtypes.size();
                if (subtypeCount == 0) {
                    nonAuxCount++;
                } else {
                    for (int j = 0; j < subtypeCount; j++) {
                        InputMethodSubtype subtype = subtypes.get(j);
                        if (!subtype.isAuxiliary()) {
                            nonAuxCount++;
                            nonAuxSubtype = subtype;
                        } else {
                            auxCount++;
                            auxSubtype = subtype;
                        }
                    }
                }
            }
            if (nonAuxCount > 1 || auxCount > 1) {
                return true;
            }
            if (nonAuxCount == 1 && auxCount == 1) {
                return nonAuxSubtype == null || auxSubtype == null || !((nonAuxSubtype.getLocale().equals(auxSubtype.getLocale()) || auxSubtype.overridesImplicitlyEnabledSubtype() || nonAuxSubtype.overridesImplicitlyEnabledSubtype()) && nonAuxSubtype.containsExtraValueKey(TAG_TRY_SUPPRESSING_IME_SWITCHER));
            }
            return false;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setImeWindowStatus(IBinder token, int vis, int backDisposition) {
        boolean dismissImeOnBackKeyPressed;
        int topFocusedDisplayId = this.mWindowManagerInternal.getTopFocusedDisplayId();
        synchronized (ImfLock.class) {
            if (calledWithValidTokenLocked(token)) {
                int i = this.mCurTokenDisplayId;
                if (i == topFocusedDisplayId || i == 0) {
                    this.mImeWindowVis = vis;
                    this.mBackDisposition = backDisposition;
                    updateSystemUiLocked(vis, backDisposition);
                    switch (backDisposition) {
                        case 1:
                            dismissImeOnBackKeyPressed = false;
                            break;
                        case 2:
                            dismissImeOnBackKeyPressed = true;
                            break;
                        default:
                            if ((vis & 2) == 0) {
                                dismissImeOnBackKeyPressed = false;
                                break;
                            } else {
                                dismissImeOnBackKeyPressed = true;
                                break;
                            }
                    }
                    this.mWindowManagerInternal.setDismissImeOnBackKeyPressed(dismissImeOnBackKeyPressed);
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void reportStartInput(IBinder token, IBinder startInputToken) {
        synchronized (ImfLock.class) {
            if (calledWithValidTokenLocked(token)) {
                IBinder targetWindow = this.mImeTargetWindowMap.get(startInputToken);
                if (targetWindow != null) {
                    this.mWindowManagerInternal.updateInputMethodTargetWindow(token, targetWindow);
                }
                this.mLastImeTargetWindow = targetWindow;
            }
        }
    }

    private void updateImeWindowStatus(boolean disableImeIcon) {
        synchronized (ImfLock.class) {
            if (disableImeIcon) {
                updateSystemUiLocked(0, this.mBackDisposition);
            } else {
                updateSystemUiLocked();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateSystemUiLocked() {
        updateSystemUiLocked(this.mImeWindowVis, this.mBackDisposition);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateSystemUiLocked(int vis, int backDisposition) {
        if (getCurTokenLocked() == null) {
            return;
        }
        boolean z = DEBUG;
        if (z) {
            Slog.d(TAG, "IME window vis: " + vis + " active: " + (vis & 1) + " inv: " + (vis & 4) + " displayId: " + this.mCurTokenDisplayId);
        }
        long ident = Binder.clearCallingIdentity();
        try {
            if (!this.mCurPerceptible) {
                if ((vis & 2) != 0) {
                    vis = (vis & (-3)) | 8;
                }
            } else {
                vis &= -9;
            }
            boolean needsToShowImeSwitcher = shouldShowImeSwitcherLocked(vis);
            StatusBarManagerService statusBarManagerService = this.mStatusBar;
            if (statusBarManagerService != null) {
                statusBarManagerService.setImeWindowStatus(this.mCurTokenDisplayId, getCurTokenLocked(), vis, backDisposition, needsToShowImeSwitcher);
            }
            InputMethodInfo imi = this.mMethodMap.get(getSelectedMethodIdLocked());
            if (imi != null && needsToShowImeSwitcher) {
                CharSequence title = this.mRes.getText(17041468);
                CharSequence summary = InputMethodUtils.getImeAndSubtypeDisplayName(this.mContext, imi, this.mCurrentSubtype);
                this.mImeSwitcherNotification.setContentTitle(title).setContentText(summary).setContentIntent(this.mImeSwitchPendingIntent);
                try {
                    if (this.mNotificationManager != null && !this.mIWindowManager.hasNavigationBar(0)) {
                        if (z) {
                            Slog.d(TAG, "--- show notification: label =  " + ((Object) summary));
                        }
                        this.mNotificationManager.notifyAsUser(null, 8, this.mImeSwitcherNotification.build(), UserHandle.ALL);
                        this.mNotificationShown = true;
                    }
                } catch (RemoteException e) {
                }
            } else if (this.mNotificationShown && this.mNotificationManager != null) {
                if (z) {
                    Slog.d(TAG, "--- hide notification");
                }
                this.mNotificationManager.cancelAsUser(null, 8, UserHandle.ALL);
                this.mNotificationShown = false;
            }
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    void updateFromSettingsLocked(boolean enabledMayChange) {
        updateInputMethodsFromSettingsLocked(enabledMayChange);
        this.mMenuController.updateKeyboardFromSettingsLocked();
    }

    void updateInputMethodsFromSettingsLocked(boolean enabledMayChange) {
        if (enabledMayChange) {
            List<InputMethodInfo> enabled = this.mSettings.getEnabledInputMethodListLocked();
            for (int i = 0; i < enabled.size(); i++) {
                InputMethodInfo imm = enabled.get(i);
                try {
                    ApplicationInfo ai = this.mIPackageManager.getApplicationInfo(imm.getPackageName(), 32768L, this.mSettings.getCurrentUserId());
                    if (ai != null && ai.enabledSetting == 4) {
                        if (DEBUG) {
                            Slog.d(TAG, "Update state(" + imm.getId() + "): DISABLED_UNTIL_USED -> DEFAULT");
                        }
                        this.mIPackageManager.setApplicationEnabledSetting(imm.getPackageName(), 0, 1, this.mSettings.getCurrentUserId(), this.mContext.getBasePackageName());
                    }
                } catch (RemoteException e) {
                }
            }
        }
        String id = this.mSettings.getSelectedInputMethod();
        if (TextUtils.isEmpty(id) && chooseNewDefaultIMELocked()) {
            id = this.mSettings.getSelectedInputMethod();
        }
        if (!TextUtils.isEmpty(id)) {
            try {
                setInputMethodLocked(id, this.mSettings.getSelectedInputMethodSubtypeId(id));
            } catch (IllegalArgumentException e2) {
                Slog.w(TAG, "Unknown input method from prefs: " + id, e2);
                resetCurrentMethodAndClientLocked(5);
            }
        } else {
            resetCurrentMethodAndClientLocked(4);
        }
        this.mSwitchingController.resetCircularListLocked(this.mContext);
        sendOnNavButtonFlagsChangedLocked();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setInputMethodLocked(String id, int subtypeId) {
        InputMethodSubtype newSubtype;
        InputMethodInfo info = this.mMethodMap.get(id);
        if (info == null) {
            throw new IllegalArgumentException("Unknown id: " + id);
        }
        if (id.equals(getSelectedMethodIdLocked())) {
            int subtypeCount = info.getSubtypeCount();
            if (subtypeCount <= 0) {
                return;
            }
            InputMethodSubtype oldSubtype = this.mCurrentSubtype;
            if (subtypeId >= 0 && subtypeId < subtypeCount) {
                newSubtype = info.getSubtypeAt(subtypeId);
            } else {
                newSubtype = getCurrentInputMethodSubtypeLocked();
            }
            if (newSubtype == null || oldSubtype == null) {
                Slog.w(TAG, "Illegal subtype state: old subtype = " + oldSubtype + ", new subtype = " + newSubtype);
                return;
            } else if (newSubtype != oldSubtype) {
                setSelectedInputMethodAndSubtypeLocked(info, subtypeId, true);
                IInputMethodInvoker curMethod = getCurMethodLocked();
                if (curMethod != null) {
                    updateSystemUiLocked(this.mImeWindowVis, this.mBackDisposition);
                    curMethod.changeInputMethodSubtype(newSubtype);
                    return;
                }
                return;
            } else {
                return;
            }
        }
        ITranInputMethodManagerService.Instance().hookSetInputMethodLocked(info);
        long ident = Binder.clearCallingIdentity();
        try {
            setSelectedInputMethodAndSubtypeLocked(info, subtypeId, false);
            setSelectedMethodIdLocked(id);
            if (((ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class)).isSystemReady()) {
                Intent intent = new Intent("android.intent.action.INPUT_METHOD_CHANGED");
                intent.addFlags(536870912);
                intent.putExtra("input_method_id", id);
                ITranInputMethodManagerService.Instance().hookChangeIme(intent.getAction(), id);
                this.mContext.sendBroadcastAsUser(intent, UserHandle.CURRENT);
            }
            unbindCurrentClientLocked(2);
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public boolean showSoftInput(IInputMethodClient client, IBinder windowToken, int flags, ResultReceiver resultReceiver, int reason) {
        Trace.traceBegin(32L, "IMMS.showSoftInput");
        int uid = Binder.getCallingUid();
        ImeTracing.getInstance().triggerManagerServiceDump("InputMethodManagerService#showSoftInput");
        synchronized (ImfLock.class) {
            if (calledFromValidUserLocked()) {
                long ident = Binder.clearCallingIdentity();
                if (canInteractWithImeLocked(uid, client, "showSoftInput")) {
                    if (DEBUG) {
                        Slog.v(TAG, "Client requesting input be shown");
                    }
                    boolean showCurrentInputLocked = showCurrentInputLocked(windowToken, flags, resultReceiver, reason);
                    Binder.restoreCallingIdentity(ident);
                    Trace.traceEnd(32L);
                    return showCurrentInputLocked;
                }
                Binder.restoreCallingIdentity(ident);
                Trace.traceEnd(32L);
                return false;
            }
            return false;
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [3791=6, 3792=6] */
    public void startStylusHandwriting(IInputMethodClient client) {
        Trace.traceBegin(32L, "IMMS.startStylusHandwriting");
        ImeTracing.getInstance().triggerManagerServiceDump("InputMethodManagerService#startStylusHandwriting");
        int uid = Binder.getCallingUid();
        synchronized (ImfLock.class) {
            if (!calledFromValidUserLocked()) {
                Trace.traceEnd(32L);
                return;
            }
            long ident = Binder.clearCallingIdentity();
            if (!canInteractWithImeLocked(uid, client, "startStylusHandwriting")) {
                Binder.restoreCallingIdentity(ident);
                Trace.traceEnd(32L);
            } else if (!this.mBindingController.supportsStylusHandwriting()) {
                Slog.w(TAG, "Stylus HW unsupported by IME. Ignoring startStylusHandwriting()");
                Binder.restoreCallingIdentity(ident);
                Trace.traceEnd(32L);
            } else {
                OptionalInt requestId = this.mHwController.getCurrentRequestId();
                if (!requestId.isPresent()) {
                    Slog.e(TAG, "Stylus handwriting was not initialized.");
                    Binder.restoreCallingIdentity(ident);
                    Trace.traceEnd(32L);
                } else if (!this.mHwController.isStylusGestureOngoing()) {
                    Slog.e(TAG, "There is no ongoing stylus gesture to start stylus handwriting.");
                    Binder.restoreCallingIdentity(ident);
                    Trace.traceEnd(32L);
                } else {
                    if (DEBUG) {
                        Slog.v(TAG, "Client requesting Stylus Handwriting to be started");
                    }
                    IInputMethodInvoker curMethod = getCurMethodLocked();
                    if (curMethod != null) {
                        curMethod.canStartStylusHandwriting(requestId.getAsInt());
                    }
                    Binder.restoreCallingIdentity(ident);
                    Trace.traceEnd(32L);
                }
            }
        }
    }

    public void reportPerceptibleAsync(IBinder windowToken, boolean perceptible) {
        Objects.requireNonNull(windowToken, "windowToken must not be null");
        Binder.getCallingUid();
        synchronized (ImfLock.class) {
            if (calledFromValidUserLocked()) {
                long ident = Binder.clearCallingIdentity();
                if (this.mCurFocusedWindow == windowToken && this.mCurPerceptible != perceptible) {
                    this.mCurPerceptible = perceptible;
                    updateSystemUiLocked(this.mImeWindowVis, this.mBackDisposition);
                }
                Binder.restoreCallingIdentity(ident);
            }
        }
    }

    boolean showCurrentInputLocked(IBinder windowToken, int flags, ResultReceiver resultReceiver, int reason) {
        this.mShowRequested = true;
        if (this.mAccessibilityRequestingNoSoftKeyboard || this.mImeHiddenByDisplayPolicy) {
            return false;
        }
        if ((flags & 2) != 0) {
            this.mShowExplicitlyRequested = true;
            this.mShowForced = true;
        } else if ((flags & 1) == 0) {
            this.mShowExplicitlyRequested = true;
        }
        if (this.mSystemReady) {
            this.mBindingController.setCurrentMethodVisible();
            IInputMethodInvoker curMethod = getCurMethodLocked();
            if (curMethod != null) {
                ITranInputMethodManagerService.Instance().hookInputMethodShown(true);
                Slog.d(TAG, "showCurrentInputLocked getTouchFromScreen: " + ITranInputMethodManagerService.Instance().getTouchFromScreen());
                if (ITranInputMethodManagerService.Instance().getTouchFromScreen()) {
                    Binder showInputToken = new Binder();
                    this.mShowRequestWindowMap.put(showInputToken, windowToken);
                    int showFlags = getImeShowFlagsLocked();
                    if (DEBUG) {
                        Slog.v(TAG, "Calling " + curMethod + ".showSoftInput(" + showInputToken + ", " + showFlags + ", " + resultReceiver + ") for reason: " + InputMethodDebug.softInputDisplayReasonToString(reason));
                    }
                    if (curMethod.showSoftInput(showInputToken, showFlags, resultReceiver)) {
                        onShowHideSoftInputRequested(true, windowToken, reason);
                    }
                    ITranInputMethodManagerService.Instance().setConnectInputShow(true);
                } else {
                    hideCurrentInputClient(windowToken);
                }
                this.mInputShown = true;
                hookIMEVisibleChanged(true);
                ITranInputMethodManagerService.Instance().hookInputMethodShown(this.mInputShown);
                return true;
            }
            return false;
        }
        return false;
    }

    public boolean hideSoftInput(IInputMethodClient client, IBinder windowToken, int flags, ResultReceiver resultReceiver, int reason) {
        int uid = Binder.getCallingUid();
        ImeTracing.getInstance().triggerManagerServiceDump("InputMethodManagerService#hideSoftInput");
        synchronized (ImfLock.class) {
            try {
                try {
                    if (!calledFromValidUserLocked()) {
                        return false;
                    }
                    long ident = Binder.clearCallingIdentity();
                    try {
                        Trace.traceBegin(32L, "IMMS.hideSoftInput");
                        ClientState clientState = this.mCurClient;
                        try {
                            if (clientState == null || client == null || clientState.client.asBinder() != client.asBinder()) {
                                ClientState cs = this.mClients.get(client.asBinder());
                                if (cs == null) {
                                    throw new IllegalArgumentException("unknown client " + client.asBinder());
                                }
                                if (!isImeClientFocused(windowToken, cs)) {
                                    if (DEBUG) {
                                        Slog.w(TAG, "Ignoring hideSoftInput of uid " + uid + ": " + client);
                                    }
                                    Binder.restoreCallingIdentity(ident);
                                    Trace.traceEnd(32L);
                                    return false;
                                }
                            }
                            if (DEBUG) {
                                Slog.v(TAG, "Client requesting input be hidden");
                            }
                            boolean hideCurrentInputLocked = hideCurrentInputLocked(windowToken, flags, resultReceiver, reason);
                            Binder.restoreCallingIdentity(ident);
                            Trace.traceEnd(32L);
                            return hideCurrentInputLocked;
                        } catch (Throwable th) {
                            th = th;
                            Binder.restoreCallingIdentity(ident);
                            Trace.traceEnd(32L);
                            throw th;
                        }
                    } catch (Throwable th2) {
                        th = th2;
                    }
                } catch (Throwable th3) {
                    th = th3;
                    throw th;
                }
            } catch (Throwable th4) {
                th = th4;
            }
        }
    }

    boolean hideCurrentInputLocked(IBinder windowToken, int flags, ResultReceiver resultReceiver, int reason) {
        boolean res;
        if ((flags & 1) != 0 && (this.mShowExplicitlyRequested || this.mShowForced)) {
            if (DEBUG) {
                Slog.v(TAG, "Not hiding: explicit show not cancelled by non-explicit hide");
            }
            return false;
        } else if (this.mShowForced && (flags & 2) != 0) {
            if (DEBUG) {
                Slog.v(TAG, "Not hiding: forced show not cancelled by not-always hide");
            }
            return false;
        } else {
            ITranInputMethodManagerService.Instance().hookInputMethodShown(false);
            IInputMethodInvoker curMethod = getCurMethodLocked();
            boolean shouldHideSoftInput = true;
            if (curMethod == null || (!this.mInputShown && (this.mImeWindowVis & 1) == 0)) {
                shouldHideSoftInput = false;
            }
            if (shouldHideSoftInput) {
                Binder hideInputToken = new Binder();
                this.mHideRequestWindowMap.put(hideInputToken, windowToken);
                if (DEBUG) {
                    Slog.v(TAG, "Calling " + curMethod + ".hideSoftInput(0, " + hideInputToken + ", " + resultReceiver + ") for reason: " + InputMethodDebug.softInputDisplayReasonToString(reason));
                }
                if (curMethod.hideSoftInput(hideInputToken, 0, resultReceiver)) {
                    onShowHideSoftInputRequested(false, windowToken, reason);
                }
                res = true;
            } else {
                res = false;
            }
            this.mBindingController.setCurrentMethodNotVisible();
            this.mInputShown = false;
            hookIMEVisibleChanged(false);
            ITranInputMethodManagerService.Instance().hookInputMethodShown(this.mInputShown);
            ITranInputMethodManagerService.Instance().setConnectInputShow(this.mInputShown);
            this.mShowRequested = false;
            this.mShowExplicitlyRequested = false;
            this.mShowForced = false;
            return res;
        }
    }

    private boolean isImeClientFocused(IBinder windowToken, ClientState cs) {
        int imeClientFocus = this.mWindowManagerInternal.hasInputMethodClientFocus(windowToken, cs.uid, cs.pid, cs.selfReportedDisplayId);
        return imeClientFocus == 0;
    }

    public InputBindResult startInputOrWindowGainedFocus(int startInputReason, IInputMethodClient client, IBinder windowToken, int startInputFlags, int softInputMode, int windowFlags, EditorInfo attribute, IInputContext inputContext, IRemoteAccessibilityInputConnection remoteAccessibilityInputConnection, int unverifiedTargetSdkVersion, ImeOnBackInvokedDispatcher imeDispatcher) {
        return startInputOrWindowGainedFocusInternal(startInputReason, client, windowToken, startInputFlags, softInputMode, windowFlags, attribute, inputContext, remoteAccessibilityInputConnection, unverifiedTargetSdkVersion, imeDispatcher);
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [4070=5] */
    private InputBindResult startInputOrWindowGainedFocusInternal(int startInputReason, IInputMethodClient client, IBinder windowToken, int startInputFlags, int softInputMode, int windowFlags, EditorInfo attribute, IInputContext inputContext, IRemoteAccessibilityInputConnection remoteAccessibilityInputConnection, int unverifiedTargetSdkVersion, ImeOnBackInvokedDispatcher imeDispatcher) {
        int userId;
        if (windowToken == null) {
            Slog.e(TAG, "windowToken cannot be null.");
            return InputBindResult.NULL;
        }
        try {
            Trace.traceBegin(32L, "IMMS.startInputOrWindowGainedFocus");
            ImeTracing.getInstance().triggerManagerServiceDump("InputMethodManagerService#startInputOrWindowGainedFocus");
            int callingUserId = UserHandle.getCallingUserId();
            if (attribute == null || attribute.targetInputMethodUser == null || attribute.targetInputMethodUser.getIdentifier() == callingUserId) {
                userId = callingUserId;
            } else {
                this.mContext.enforceCallingPermission("android.permission.INTERACT_ACROSS_USERS_FULL", "Using EditorInfo.targetInputMethodUser requires INTERACT_ACROSS_USERS_FULL.");
                int userId2 = attribute.targetInputMethodUser.getIdentifier();
                if (!this.mUserManagerInternal.isUserRunning(userId2)) {
                    Slog.e(TAG, "User #" + userId2 + " is not running.");
                    InputBindResult inputBindResult = InputBindResult.INVALID_USER;
                    Trace.traceEnd(32L);
                    return inputBindResult;
                }
                userId = userId2;
            }
            ITranInputMethodManagerService.Instance().hookstartInputOrWindowGainedFocus(attribute != null ? attribute.packageName : null);
        } catch (Throwable th) {
            th = th;
        }
        try {
            try {
                synchronized (ImfLock.class) {
                    try {
                        long ident = Binder.clearCallingIdentity();
                        InputBindResult result = startInputOrWindowGainedFocusInternalLocked(startInputReason, client, windowToken, startInputFlags, softInputMode, windowFlags, attribute, inputContext, remoteAccessibilityInputConnection, unverifiedTargetSdkVersion, userId, imeDispatcher);
                        Binder.restoreCallingIdentity(ident);
                        if (result != null) {
                            Trace.traceEnd(32L);
                            return result;
                        }
                        Slog.wtf(TAG, "InputBindResult is @NonNull. startInputReason=" + InputMethodDebug.startInputReasonToString(startInputReason) + " windowFlags=#" + Integer.toHexString(windowFlags) + " editorInfo=" + attribute);
                        InputBindResult inputBindResult2 = InputBindResult.NULL;
                        Trace.traceEnd(32L);
                        return inputBindResult2;
                    } catch (Throwable th2) {
                        th = th2;
                        throw th;
                    }
                }
            } catch (Throwable th3) {
                th = th3;
            }
        } catch (Throwable th4) {
            th = th4;
            Trace.traceEnd(32L);
            throw th;
        }
    }

    private InputBindResult startInputOrWindowGainedFocusInternalLocked(int startInputReason, IInputMethodClient client, IBinder windowToken, int startInputFlags, int softInputMode, int windowFlags, EditorInfo attribute, IInputContext inputContext, IRemoteAccessibilityInputConnection remoteAccessibilityInputConnection, int unverifiedTargetSdkVersion, int userId, ImeOnBackInvokedDispatcher imeDispatcher) {
        int i;
        ClientState cs;
        ResultReceiver resultReceiver;
        ClientState cs2;
        int[] profileIdsWithDisabled;
        boolean z = DEBUG;
        if (z) {
            Slog.v(TAG, "startInputOrWindowGainedFocusInternalLocked: reason=" + InputMethodDebug.startInputReasonToString(startInputReason) + " client=" + client.asBinder() + " inputContext=" + inputContext + " attribute=" + attribute + " startInputFlags=" + InputMethodDebug.startInputFlagsToString(startInputFlags) + " softInputMode=" + InputMethodDebug.softInputModeToString(softInputMode) + " windowFlags=#" + Integer.toHexString(windowFlags) + " unverifiedTargetSdkVersion=" + unverifiedTargetSdkVersion + " imeDispatcher=" + imeDispatcher);
        }
        int userId2 = checkDualID(userId);
        ClientState cs3 = this.mClients.get(client.asBinder());
        if (cs3 == null) {
            throw new IllegalArgumentException("unknown client " + client.asBinder());
        }
        int imeClientFocus = this.mWindowManagerInternal.hasInputMethodClientFocus(windowToken, cs3.uid, cs3.pid, cs3.selfReportedDisplayId);
        switch (imeClientFocus) {
            case -3:
                return InputBindResult.INVALID_DISPLAY_ID;
            case -2:
                Slog.e(TAG, "startInputOrWindowGainedFocusInternal: display ID mismatch.");
                return InputBindResult.DISPLAY_ID_MISMATCH;
            case -1:
                if (z) {
                    Slog.w(TAG, "Focus gain on non-focused client " + cs3.client + " (uid=" + cs3.uid + " pid=" + cs3.pid + ")");
                }
                return InputBindResult.NOT_IME_TARGET_WINDOW;
            default:
                UserSwitchHandlerTask userSwitchHandlerTask = this.mUserSwitchHandlerTask;
                if (userSwitchHandlerTask != null) {
                    int nextUserId = userSwitchHandlerTask.mToUserId;
                    if (userId2 == nextUserId) {
                        scheduleSwitchUserTaskLocked(userId2, cs3.client);
                        return InputBindResult.USER_SWITCHING;
                    }
                    for (int profileId : this.mUserManager.getProfileIdsWithDisabled(nextUserId)) {
                        if (profileId == userId2) {
                            scheduleSwitchUserTaskLocked(userId2, cs3.client);
                            return InputBindResult.USER_SWITCHING;
                        }
                    }
                    return InputBindResult.INVALID_USER;
                }
                boolean shouldClearFlag = this.mImePlatformCompatUtils.shouldClearShowForcedFlag(cs3.uid);
                if (this.mCurFocusedWindow != windowToken && this.mShowForced && shouldClearFlag) {
                    this.mShowForced = false;
                }
                boolean isSystemUiApp = ITranInputMethodManagerService.Instance().hookStartInputVisible(this.mIPackageManager, cs3.uid);
                if (!this.mSettings.isCurrentProfile(userId2) && !isSystemUiApp) {
                    Slog.w(TAG, "A background user is requesting window. Hiding IME.");
                    Slog.w(TAG, "If you need to impersonate a foreground user/profile from a background user, use EditorInfo.targetInputMethodUser with INTERACT_ACROSS_USERS_FULL permission.");
                    hideCurrentInputLocked(this.mCurFocusedWindow, 0, null, 10);
                    return InputBindResult.INVALID_USER;
                } else if (userId2 != this.mSettings.getCurrentUserId() && !isSystemUiApp) {
                    scheduleSwitchUserTaskLocked(userId2, cs3.client);
                    return InputBindResult.USER_SWITCHING;
                } else {
                    boolean sameWindowFocused = this.mCurFocusedWindow == windowToken;
                    boolean isTextEditor = (startInputFlags & 2) != 0;
                    boolean startInputByWinGainedFocus = (startInputFlags & 8) != 0;
                    if (sameWindowFocused && isTextEditor) {
                        if (z) {
                            Slog.w(TAG, "Window already focused, ignoring focus gain of: " + client + " attribute=" + attribute + ", token = " + windowToken + ", startInputReason=" + InputMethodDebug.startInputReasonToString(startInputReason));
                        }
                        if (attribute != null) {
                            return startInputUncheckedLocked(cs3, inputContext, remoteAccessibilityInputConnection, attribute, startInputFlags, startInputReason, unverifiedTargetSdkVersion, imeDispatcher);
                        }
                        return new InputBindResult(4, (IInputMethodSession) null, (SparseArray) null, (InputChannel) null, (String) null, -1, (Matrix) null, false);
                    }
                    this.mCurFocusedWindow = windowToken;
                    this.mCurFocusedWindowSoftInputMode = softInputMode;
                    this.mCurFocusedWindowClient = cs3;
                    this.mCurPerceptible = true;
                    boolean doAutoShow = (softInputMode & FrameworkStatsLog.BOOT_TIME_EVENT_ELAPSED_TIME_REPORTED) == 16 || this.mRes.getConfiguration().isLayoutSizeAtLeast(3);
                    boolean didStart = false;
                    InputBindResult res = null;
                    if (!isTextEditor || attribute == null) {
                        i = 1;
                        cs = cs3;
                        resultReceiver = null;
                    } else if (!shouldRestoreImeVisibility(windowToken, softInputMode)) {
                        i = 1;
                        cs = cs3;
                        resultReceiver = null;
                    } else {
                        InputBindResult res2 = startInputUncheckedLocked(cs3, inputContext, remoteAccessibilityInputConnection, attribute, startInputFlags, startInputReason, unverifiedTargetSdkVersion, imeDispatcher);
                        showCurrentInputLocked(windowToken, 1, null, 22);
                        return res2;
                    }
                    switch (softInputMode & 15) {
                        case 0:
                            if (sameWindowFocused) {
                                cs2 = cs;
                            } else if (!isTextEditor || !doAutoShow) {
                                if (WindowManager.LayoutParams.mayUseInputMethod(windowFlags)) {
                                    if (z) {
                                        Slog.v(TAG, "Unspecified window will hide input");
                                    }
                                    hideCurrentInputLocked(this.mCurFocusedWindow, 2, resultReceiver, 11);
                                    ClientState cs4 = cs;
                                    if (cs4.selfReportedDisplayId == this.mCurTokenDisplayId) {
                                        cs = cs4;
                                        break;
                                    } else {
                                        this.mBindingController.unbindCurrentMethod();
                                        cs = cs4;
                                        break;
                                    }
                                } else {
                                    break;
                                }
                            } else {
                                cs2 = cs;
                            }
                            if (!isTextEditor || !doAutoShow || (softInputMode & 256) == 0) {
                                cs = cs2;
                                break;
                            } else {
                                if (z) {
                                    Slog.v(TAG, "Unspecified window will show input");
                                }
                                if (attribute == null) {
                                    cs = cs2;
                                } else {
                                    cs = cs2;
                                    res = startInputUncheckedLocked(cs2, inputContext, remoteAccessibilityInputConnection, attribute, startInputFlags, startInputReason, unverifiedTargetSdkVersion, imeDispatcher);
                                    didStart = true;
                                }
                                showCurrentInputLocked(windowToken, i, resultReceiver, 5);
                                break;
                            }
                        case 2:
                            if ((softInputMode & 256) != 0) {
                                if (z) {
                                    Slog.v(TAG, "Window asks to hide input going forward");
                                }
                                hideCurrentInputLocked(this.mCurFocusedWindow, 0, resultReceiver, 12);
                                break;
                            } else if ("com.transsion.sk/.inputservice.TInputMethodService".equals(getSelectedMethodIdLocked())) {
                                Slog.i(TAG, "current is secureinput, so first hide");
                                hideCurrentInputLocked(this.mCurFocusedWindow, 0, resultReceiver, 3);
                                break;
                            }
                            break;
                        case 3:
                            if (sameWindowFocused) {
                                break;
                            } else {
                                if (z) {
                                    Slog.v(TAG, "Window asks to hide input");
                                }
                                hideCurrentInputLocked(this.mCurFocusedWindow, 0, resultReceiver, 13);
                                break;
                            }
                        case 4:
                            if ((softInputMode & 256) != 0) {
                                if (z) {
                                    Slog.v(TAG, "Window asks to show input going forward");
                                }
                                if (InputMethodUtils.isSoftInputModeStateVisibleAllowed(unverifiedTargetSdkVersion, startInputFlags)) {
                                    if (attribute != null) {
                                        res = startInputUncheckedLocked(cs, inputContext, remoteAccessibilityInputConnection, attribute, startInputFlags, startInputReason, unverifiedTargetSdkVersion, imeDispatcher);
                                        didStart = true;
                                    }
                                    showCurrentInputLocked(windowToken, i, resultReceiver, 6);
                                    break;
                                } else {
                                    Slog.e(TAG, "SOFT_INPUT_STATE_VISIBLE is ignored because there is no focused view that also returns true from View#onCheckIsTextEditor()");
                                    break;
                                }
                            }
                            break;
                        case 5:
                            if (z) {
                                Slog.v(TAG, "Window asks to always show input");
                            }
                            if (InputMethodUtils.isSoftInputModeStateVisibleAllowed(unverifiedTargetSdkVersion, startInputFlags)) {
                                if (!sameWindowFocused) {
                                    if (attribute != null) {
                                        res = startInputUncheckedLocked(cs, inputContext, remoteAccessibilityInputConnection, attribute, startInputFlags, startInputReason, unverifiedTargetSdkVersion, imeDispatcher);
                                        didStart = true;
                                    }
                                    showCurrentInputLocked(windowToken, i, resultReceiver, 7);
                                    break;
                                }
                            } else {
                                Slog.e(TAG, "SOFT_INPUT_STATE_ALWAYS_VISIBLE is ignored because there is no focused view that also returns true from View#onCheckIsTextEditor()");
                                break;
                            }
                            break;
                    }
                    if (!didStart) {
                        if (attribute != null) {
                            if (sameWindowFocused && startInputByWinGainedFocus) {
                                hideCurrentInputLocked(this.mCurFocusedWindow, 0, resultReceiver, 20);
                            }
                            return startInputUncheckedLocked(cs, inputContext, remoteAccessibilityInputConnection, attribute, startInputFlags, startInputReason, unverifiedTargetSdkVersion, imeDispatcher);
                        }
                        return InputBindResult.NULL_EDITOR_INFO;
                    }
                    return res;
                }
        }
    }

    private boolean canInteractWithImeLocked(int uid, IInputMethodClient client, String methodName) {
        ClientState clientState = this.mCurClient;
        if (clientState == null || client == null || clientState.client.asBinder() != client.asBinder()) {
            ClientState cs = this.mClients.get(client.asBinder());
            if (cs == null) {
                throw new IllegalArgumentException("unknown client " + client.asBinder());
            }
            if (!isImeClientFocused(this.mCurFocusedWindow, cs)) {
                Slog.w(TAG, String.format("Ignoring %s of uid %d : %s", methodName, Integer.valueOf(uid), client));
                return false;
            }
        }
        return true;
    }

    private boolean shouldRestoreImeVisibility(IBinder windowToken, int softInputMode) {
        switch (softInputMode & 15) {
            case 3:
                return false;
            case 2:
                if ((softInputMode & 256) != 0) {
                    return false;
                }
                break;
        }
        return this.mWindowManagerInternal.shouldRestoreImeVisibility(windowToken);
    }

    private boolean canShowInputMethodPickerLocked(IInputMethodClient client) {
        int uid = Binder.getCallingUid();
        ClientState clientState = this.mCurFocusedWindowClient;
        if (clientState == null || client == null || clientState.client.asBinder() != client.asBinder()) {
            return getCurIntentLocked() != null && InputMethodUtils.checkIfPackageBelongsToUid(this.mAppOpsManager, uid, getCurIntentLocked().getComponent().getPackageName());
        }
        return true;
    }

    public void showInputMethodPickerFromClient(IInputMethodClient client, int auxiliarySubtypeMode) {
        synchronized (ImfLock.class) {
            if (calledFromValidUserLocked()) {
                if (!canShowInputMethodPickerLocked(client)) {
                    Slog.w(TAG, "Ignoring showInputMethodPickerFromClient of uid " + Binder.getCallingUid() + ": " + client);
                    return;
                }
                ClientState clientState = this.mCurClient;
                int displayId = clientState != null ? clientState.selfReportedDisplayId : 0;
                this.mHandler.obtainMessage(1, auxiliarySubtypeMode, displayId).sendToTarget();
            }
        }
    }

    public void showInputMethodPickerFromSystem(IInputMethodClient client, int auxiliarySubtypeMode, int displayId) {
        if (this.mContext.checkCallingPermission("android.permission.WRITE_SECURE_SETTINGS") != 0) {
            throw new SecurityException("showInputMethodPickerFromSystem requires WRITE_SECURE_SETTINGS permission");
        }
        this.mHandler.obtainMessage(1, auxiliarySubtypeMode, displayId).sendToTarget();
    }

    public boolean isInputMethodPickerShownForTest() {
        boolean isisInputMethodPickerShownForTestLocked;
        synchronized (ImfLock.class) {
            isisInputMethodPickerShownForTestLocked = this.mMenuController.isisInputMethodPickerShownForTestLocked();
        }
        return isisInputMethodPickerShownForTestLocked;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setInputMethod(IBinder token, String id) {
        synchronized (ImfLock.class) {
            if (calledWithValidTokenLocked(token)) {
                setInputMethodWithSubtypeIdLocked(token, id, -1);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setInputMethodAndSubtype(IBinder token, String id, InputMethodSubtype subtype) {
        synchronized (ImfLock.class) {
            if (calledWithValidTokenLocked(token)) {
                if (subtype != null) {
                    setInputMethodWithSubtypeIdLocked(token, id, InputMethodUtils.getSubtypeIdFromHashCode(this.mMethodMap.get(id), subtype.hashCode()));
                } else {
                    setInputMethod(token, id);
                }
            }
        }
    }

    public void showInputMethodAndSubtypeEnablerFromClient(IInputMethodClient client, String inputMethodId) {
        synchronized (ImfLock.class) {
            if (calledFromValidUserLocked()) {
                showInputMethodAndSubtypeEnabler(inputMethodId);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean switchToPreviousInputMethod(IBinder token) {
        InputMethodInfo lastImi;
        List<InputMethodInfo> enabled;
        String locale;
        InputMethodSubtype keyboardSubtype;
        synchronized (ImfLock.class) {
            try {
                try {
                    if (calledWithValidTokenLocked(token)) {
                        Pair<String, String> lastIme = this.mSettings.getLastInputMethodAndSubtypeLocked();
                        if (lastIme != null) {
                            lastImi = this.mMethodMap.get(lastIme.first);
                        } else {
                            lastImi = null;
                        }
                        String targetLastImiId = null;
                        int subtypeId = -1;
                        if (lastIme != null && lastImi != null) {
                            boolean imiIdIsSame = lastImi.getId().equals(getSelectedMethodIdLocked());
                            int lastSubtypeHash = Integer.parseInt((String) lastIme.second);
                            InputMethodSubtype inputMethodSubtype = this.mCurrentSubtype;
                            int currentSubtypeHash = inputMethodSubtype == null ? -1 : inputMethodSubtype.hashCode();
                            if (!imiIdIsSame || lastSubtypeHash != currentSubtypeHash) {
                                targetLastImiId = (String) lastIme.first;
                                subtypeId = InputMethodUtils.getSubtypeIdFromHashCode(lastImi, lastSubtypeHash);
                            }
                        }
                        boolean imiIdIsSame2 = TextUtils.isEmpty(targetLastImiId);
                        if (imiIdIsSame2 && !InputMethodUtils.canAddToLastInputMethod(this.mCurrentSubtype) && (enabled = this.mSettings.getEnabledInputMethodListLocked()) != null) {
                            int N = enabled.size();
                            InputMethodSubtype inputMethodSubtype2 = this.mCurrentSubtype;
                            if (inputMethodSubtype2 == null) {
                                locale = this.mRes.getConfiguration().locale.toString();
                            } else {
                                locale = inputMethodSubtype2.getLocale();
                            }
                            for (int i = 0; i < N; i++) {
                                InputMethodInfo imi = enabled.get(i);
                                if (imi.getSubtypeCount() > 0 && imi.isSystem() && (keyboardSubtype = InputMethodUtils.findLastResortApplicableSubtypeLocked(this.mRes, InputMethodUtils.getSubtypes(imi), "keyboard", locale, true)) != null) {
                                    targetLastImiId = imi.getId();
                                    subtypeId = InputMethodUtils.getSubtypeIdFromHashCode(imi, keyboardSubtype.hashCode());
                                    if (keyboardSubtype.getLocale().equals(locale)) {
                                        break;
                                    }
                                }
                            }
                        }
                        if (TextUtils.isEmpty(targetLastImiId)) {
                            return false;
                        }
                        if (DEBUG) {
                            Slog.d(TAG, "Switch to: " + lastImi.getId() + ", " + ((String) lastIme.second) + ", from: " + getSelectedMethodIdLocked() + ", " + subtypeId);
                        }
                        setInputMethodWithSubtypeIdLocked(token, targetLastImiId, subtypeId);
                        return true;
                    }
                    return false;
                } catch (Throwable th) {
                    th = th;
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
                throw th;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean switchToNextInputMethod(IBinder token, boolean onlyCurrentIme) {
        synchronized (ImfLock.class) {
            if (calledWithValidTokenLocked(token)) {
                InputMethodSubtypeSwitchingController.ImeSubtypeListItem nextSubtype = this.mSwitchingController.getNextInputMethodLocked(onlyCurrentIme, this.mMethodMap.get(getSelectedMethodIdLocked()), this.mCurrentSubtype);
                if (nextSubtype == null) {
                    return false;
                }
                setInputMethodWithSubtypeIdLocked(token, nextSubtype.mImi.getId(), nextSubtype.mSubtypeId);
                return true;
            }
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean shouldOfferSwitchingToNextInputMethod(IBinder token) {
        synchronized (ImfLock.class) {
            if (calledWithValidTokenLocked(token)) {
                InputMethodSubtypeSwitchingController.ImeSubtypeListItem nextSubtype = this.mSwitchingController.getNextInputMethodLocked(false, this.mMethodMap.get(getSelectedMethodIdLocked()), this.mCurrentSubtype);
                return nextSubtype != null;
            }
            return false;
        }
    }

    public InputMethodSubtype getLastInputMethodSubtype() {
        synchronized (ImfLock.class) {
            if (calledFromValidUserLocked()) {
                Pair<String, String> lastIme = this.mSettings.getLastInputMethodAndSubtypeLocked();
                if (lastIme != null && !TextUtils.isEmpty((CharSequence) lastIme.first) && !TextUtils.isEmpty((CharSequence) lastIme.second)) {
                    InputMethodInfo lastImi = this.mMethodMap.get(lastIme.first);
                    if (lastImi == null) {
                        return null;
                    }
                    try {
                        int lastSubtypeHash = Integer.parseInt((String) lastIme.second);
                        int lastSubtypeId = InputMethodUtils.getSubtypeIdFromHashCode(lastImi, lastSubtypeHash);
                        if (lastSubtypeId >= 0 && lastSubtypeId < lastImi.getSubtypeCount()) {
                            return lastImi.getSubtypeAt(lastSubtypeId);
                        }
                        return null;
                    } catch (NumberFormatException e) {
                        return null;
                    }
                }
                return null;
            }
            return null;
        }
    }

    public void setAdditionalInputMethodSubtypes(String imiId, InputMethodSubtype[] subtypes) {
        if (TextUtils.isEmpty(imiId) || subtypes == null) {
            return;
        }
        ArrayList<InputMethodSubtype> toBeAdded = new ArrayList<>();
        for (InputMethodSubtype subtype : subtypes) {
            if (!toBeAdded.contains(subtype)) {
                toBeAdded.add(subtype);
            } else {
                Slog.w(TAG, "Duplicated subtype definition found: " + subtype.getLocale() + ", " + subtype.getMode());
            }
        }
        synchronized (ImfLock.class) {
            if (calledFromValidUserLocked()) {
                if (this.mSystemReady) {
                    InputMethodInfo imi = this.mMethodMap.get(imiId);
                    if (imi == null) {
                        return;
                    }
                    try {
                        String[] packageInfos = this.mIPackageManager.getPackagesForUid(Binder.getCallingUid());
                        if (packageInfos != null) {
                            for (String str : packageInfos) {
                                if (str.equals(imi.getPackageName())) {
                                    if (subtypes.length > 0) {
                                        this.mAdditionalSubtypeMap.put(imi.getId(), toBeAdded);
                                    } else {
                                        this.mAdditionalSubtypeMap.remove(imi.getId());
                                    }
                                    AdditionalSubtypeUtils.save(this.mAdditionalSubtypeMap, this.mMethodMap, this.mSettings.getCurrentUserId());
                                    long ident = Binder.clearCallingIdentity();
                                    buildInputMethodListLocked(false);
                                    Binder.restoreCallingIdentity(ident);
                                    return;
                                }
                            }
                        }
                    } catch (RemoteException e) {
                        Slog.e(TAG, "Failed to get package infos");
                    }
                }
            }
        }
    }

    @Deprecated
    public int getInputMethodWindowVisibleHeight(final IInputMethodClient client) {
        final int callingUid = Binder.getCallingUid();
        return ((Integer) Binder.withCleanCallingIdentity(new FunctionalUtils.ThrowingSupplier() { // from class: com.android.server.inputmethod.InputMethodManagerService$$ExternalSyntheticLambda8
            public final Object getOrThrow() {
                return InputMethodManagerService.this.m4021x3b34046f(callingUid, client);
            }
        })).intValue();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getInputMethodWindowVisibleHeight$3$com-android-server-inputmethod-InputMethodManagerService  reason: not valid java name */
    public /* synthetic */ Integer m4021x3b34046f(int callingUid, IInputMethodClient client) throws Exception {
        synchronized (ImfLock.class) {
            if (!canInteractWithImeLocked(callingUid, client, "getInputMethodWindowVisibleHeight")) {
                if (!this.mLoggedDeniedGetInputMethodWindowVisibleHeightForUid.get(callingUid)) {
                    EventLog.writeEvent(1397638484, "204906124", Integer.valueOf(callingUid), "");
                    this.mLoggedDeniedGetInputMethodWindowVisibleHeightForUid.put(callingUid, true);
                }
                return 0;
            }
            int curTokenDisplayId = this.mCurTokenDisplayId;
            return Integer.valueOf(this.mWindowManagerInternal.getInputMethodWindowVisibleHeight(curTokenDisplayId));
        }
    }

    public void removeImeSurface() {
        this.mContext.enforceCallingPermission("android.permission.INTERNAL_SYSTEM_WINDOW", null);
        this.mHandler.obtainMessage(MSG_REMOVE_IME_SURFACE).sendToTarget();
    }

    /* JADX WARN: Code restructure failed: missing block: B:34:0x00a4, code lost:
        if (r1.mWindowManagerInternal.isUidAllowedOnDisplay(r18, r0.uid) == false) goto L69;
     */
    /* JADX WARN: Code restructure failed: missing block: B:35:0x00a6, code lost:
        r9 = new com.android.server.inputmethod.InputMethodManagerService.VirtualDisplayInfo(r0, new android.graphics.Matrix());
        r1.mVirtualDisplayIdToParentMap.put(r18, r9);
        r9 = r9;
     */
    /* JADX WARN: Code restructure failed: missing block: B:37:0x00d4, code lost:
        throw new java.lang.SecurityException(r0 + " cannot access to display #" + r18);
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public void reportVirtualDisplayGeometryAsync(IInputMethodClient parentClient, int childDisplayId, float[] matrixValues) {
        Matrix matrix;
        InputMethodManagerService inputMethodManagerService = this;
        try {
            DisplayInfo displayInfo = inputMethodManagerService.mDisplayManagerInternal.getDisplayInfo(childDisplayId);
            if (displayInfo == null) {
                throw new IllegalArgumentException("Cannot find display for non-existent displayId: " + childDisplayId);
            }
            int callingUid = Binder.getCallingUid();
            if (callingUid != displayInfo.ownerUid) {
                throw new SecurityException("The caller doesn't own the display.");
            }
            synchronized (ImfLock.class) {
                ClientState cs = inputMethodManagerService.mClients.get(parentClient.asBinder());
                if (cs == null) {
                    return;
                }
                if (matrixValues == null) {
                    VirtualDisplayInfo info = inputMethodManagerService.mVirtualDisplayIdToParentMap.get(childDisplayId);
                    if (info == null) {
                        return;
                    }
                    if (info.mParentClient != cs) {
                        throw new SecurityException("Only the owner client can clear VirtualDisplayGeometry for display #" + childDisplayId);
                    }
                    inputMethodManagerService.mVirtualDisplayIdToParentMap.remove(childDisplayId);
                    return;
                }
                VirtualDisplayInfo info2 = inputMethodManagerService.mVirtualDisplayIdToParentMap.get(childDisplayId);
                if (info2 != null && info2.mParentClient != cs) {
                    throw new InvalidParameterException("Display #" + childDisplayId + " is already registered by " + info2.mParentClient);
                }
                VirtualDisplayInfo info3 = info2;
                info3.mMatrix.setValues(matrixValues);
                ClientState clientState = inputMethodManagerService.mCurClient;
                if (clientState != null && clientState.curSession != null) {
                    Matrix matrix2 = null;
                    int displayId = inputMethodManagerService.mCurClient.selfReportedDisplayId;
                    boolean needToNotify = false;
                    while (true) {
                        needToNotify |= displayId == childDisplayId;
                        VirtualDisplayInfo next = inputMethodManagerService.mVirtualDisplayIdToParentMap.get(displayId);
                        if (next == null) {
                            break;
                        }
                        if (matrix2 == null) {
                            matrix = new Matrix(next.mMatrix);
                        } else {
                            matrix2.postConcat(next.mMatrix);
                            matrix = matrix2;
                        }
                        if (next.mParentClient.selfReportedDisplayId == inputMethodManagerService.mCurTokenDisplayId) {
                            if (needToNotify) {
                                float[] values = new float[9];
                                matrix.getValues(values);
                                try {
                                    inputMethodManagerService.mCurClient.client.updateVirtualDisplayToScreenMatrix(getSequenceNumberLocked(), values);
                                } catch (RemoteException e) {
                                    Slog.e(TAG, "Exception calling updateVirtualDisplayToScreenMatrix()", e);
                                }
                            }
                        } else {
                            displayId = info3.mParentClient.selfReportedDisplayId;
                            inputMethodManagerService = this;
                            matrix2 = matrix;
                        }
                    }
                }
            }
        } catch (Throwable t) {
            if (parentClient != null) {
                try {
                    parentClient.throwExceptionFromSystem(t.toString());
                } catch (RemoteException e2) {
                    Slog.e(TAG, "Exception calling throwExceptionFromSystem()", e2);
                }
            }
        }
    }

    public void removeImeSurfaceFromWindowAsync(IBinder windowToken) {
        this.mHandler.obtainMessage(MSG_REMOVE_IME_SURFACE_FROM_WINDOW, windowToken).sendToTarget();
    }

    public void startProtoDump(byte[] protoDump, int source, String where) {
        if (protoDump == null && source != 2) {
            return;
        }
        ImeTracing tracingInstance = ImeTracing.getInstance();
        if (!tracingInstance.isAvailable() || !tracingInstance.isEnabled()) {
            return;
        }
        ProtoOutputStream proto = new ProtoOutputStream();
        switch (source) {
            case 0:
                long client_token = proto.start(2246267895810L);
                proto.write(1125281431553L, SystemClock.elapsedRealtimeNanos());
                proto.write(1138166333442L, where);
                proto.write(1146756268035L, protoDump);
                proto.end(client_token);
                break;
            case 1:
                long service_token = proto.start(2246267895810L);
                proto.write(1125281431553L, SystemClock.elapsedRealtimeNanos());
                proto.write(1138166333442L, where);
                proto.write(1146756268035L, protoDump);
                proto.end(service_token);
                break;
            case 2:
                long managerservice_token = proto.start(2246267895810L);
                proto.write(1125281431553L, SystemClock.elapsedRealtimeNanos());
                proto.write(1138166333442L, where);
                dumpDebug(proto, 1146756268035L);
                proto.end(managerservice_token);
                break;
            default:
                return;
        }
        tracingInstance.addToBuffer(proto, source);
    }

    public boolean isImeTraceEnabled() {
        return ImeTracing.getInstance().isEnabled();
    }

    public void startImeTrace() {
        ArrayMap<IBinder, ClientState> clients;
        ImeTracing.getInstance().startTrace((PrintWriter) null);
        synchronized (ImfLock.class) {
            clients = new ArrayMap<>(this.mClients);
        }
        for (ClientState state : clients.values()) {
            if (state != null) {
                try {
                    state.client.setImeTraceEnabled(true);
                } catch (RemoteException e) {
                    Slog.e(TAG, "Error while trying to enable ime trace on client window", e);
                }
            }
        }
    }

    public void stopImeTrace() {
        ArrayMap<IBinder, ClientState> clients;
        ImeTracing.getInstance().stopTrace((PrintWriter) null);
        synchronized (ImfLock.class) {
            clients = new ArrayMap<>(this.mClients);
        }
        for (ClientState state : clients.values()) {
            if (state != null) {
                try {
                    state.client.setImeTraceEnabled(false);
                } catch (RemoteException e) {
                    Slog.e(TAG, "Error while trying to disable ime trace on client window", e);
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void dumpDebug(ProtoOutputStream proto, long fieldId) {
        synchronized (ImfLock.class) {
            long token = proto.start(fieldId);
            proto.write(CompanionAppsPermissions.AppPermissions.PACKAGE_NAME, getSelectedMethodIdLocked());
            proto.write(1120986464258L, getSequenceNumberLocked());
            proto.write(1138166333443L, Objects.toString(this.mCurClient));
            proto.write(1138166333444L, this.mWindowManagerInternal.getWindowName(this.mCurFocusedWindow));
            proto.write(1138166333445L, this.mWindowManagerInternal.getWindowName(this.mLastImeTargetWindow));
            proto.write(1138166333446L, InputMethodDebug.softInputModeToString(this.mCurFocusedWindowSoftInputMode));
            EditorInfo editorInfo = this.mCurAttribute;
            if (editorInfo != null) {
                editorInfo.dumpDebug(proto, 1146756268039L);
            }
            proto.write(1138166333448L, getCurIdLocked());
            proto.write(1133871366153L, this.mShowRequested);
            proto.write(1133871366154L, this.mShowExplicitlyRequested);
            proto.write(1133871366155L, this.mShowForced);
            proto.write(1133871366156L, this.mInputShown);
            proto.write(1133871366157L, this.mInFullscreenMode);
            proto.write(1138166333454L, Objects.toString(getCurTokenLocked()));
            proto.write(1120986464271L, this.mCurTokenDisplayId);
            proto.write(1133871366160L, this.mSystemReady);
            proto.write(1120986464273L, this.mLastSwitchUserId);
            proto.write(1133871366162L, hasConnectionLocked());
            proto.write(1133871366163L, this.mBoundToMethod);
            proto.write(1133871366164L, this.mIsInteractive);
            proto.write(1120986464277L, this.mBackDisposition);
            proto.write(1120986464278L, this.mImeWindowVis);
            proto.write(1133871366167L, this.mMenuController.getShowImeWithHardKeyboard());
            proto.write(1133871366168L, this.mAccessibilityRequestingNoSoftKeyboard);
            proto.end(token);
        }
    }

    public void updateSecurityInputBlackList(List<String> blacklist) {
        synchronized (this.mSecurityLock) {
            if (blacklist == null) {
                this.mSecurityInputBlackList.clear();
            } else {
                this.mSecurityInputBlackList = blacklist;
            }
        }
    }

    private boolean isKeyguardLocked() {
        KeyguardManager keyguardManager = this.mKeyguardManager;
        return keyguardManager != null && keyguardManager.isKeyguardLocked();
    }

    private boolean isPassWordInput(int inputType) {
        int variation = inputType & 4095;
        return variation == 129 || variation == 225 || variation == 18 || variation == 145 || variation == 128 || variation == 224 || variation == 16 || variation == 144;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifyUserAction(IBinder token) {
        boolean z = DEBUG;
        if (z) {
            Slog.d(TAG, "Got the notification of a user action.");
        }
        synchronized (ImfLock.class) {
            if (getCurTokenLocked() != token) {
                if (z) {
                    Slog.d(TAG, "Ignoring the user action notification from IMEs that are no longer active.");
                }
                return;
            }
            InputMethodInfo imi = this.mMethodMap.get(getSelectedMethodIdLocked());
            if (imi != null) {
                this.mSwitchingController.onUserActionLocked(imi, this.mCurrentSubtype);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void applyImeVisibility(IBinder token, IBinder windowToken, boolean setVisible) {
        Trace.traceBegin(32L, "IMMS.applyImeVisibility");
        synchronized (ImfLock.class) {
            if (calledWithValidTokenLocked(token)) {
                if (!setVisible) {
                    if (this.mCurClient != null) {
                        this.mWindowManagerInternal.hideIme(this.mHideRequestWindowMap.get(windowToken), this.mCurClient.selfReportedDisplayId);
                    }
                } else {
                    this.mWindowManagerInternal.showImePostLayout(this.mShowRequestWindowMap.get(windowToken));
                }
                Trace.traceEnd(32L);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void resetStylusHandwriting(int requestId) {
        synchronized (ImfLock.class) {
            OptionalInt curRequest = this.mHwController.getCurrentRequestId();
            if (!curRequest.isPresent() || curRequest.getAsInt() != requestId) {
                Slog.w(TAG, "IME requested to finish handwriting with a mismatched requestId: " + requestId);
            }
            scheduleResetStylusHandwriting();
        }
    }

    private void setInputMethodWithSubtypeIdLocked(IBinder token, String id, int subtypeId) {
        if (token == null) {
            if (this.mContext.checkCallingOrSelfPermission("android.permission.WRITE_SECURE_SETTINGS") != 0) {
                throw new SecurityException("Using null token requires permission android.permission.WRITE_SECURE_SETTINGS");
            }
        } else if (getCurTokenLocked() != token) {
            Slog.w(TAG, "Ignoring setInputMethod of uid " + Binder.getCallingUid() + " token: " + token);
            return;
        }
        long ident = Binder.clearCallingIdentity();
        try {
            setInputMethodLocked(id, subtypeId);
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    private void onShowHideSoftInputRequested(boolean show, IBinder requestToken, int reason) {
        WindowManagerInternal.ImeTargetInfo info = this.mWindowManagerInternal.onToggleImeRequested(show, this.mCurFocusedWindow, requestToken, this.mCurTokenDisplayId);
        this.mSoftInputShowHideHistory.addEntry(new SoftInputShowHideHistory.Entry(this.mCurFocusedWindowClient, this.mCurAttribute, info.focusedWindowName, this.mCurFocusedWindowSoftInputMode, reason, this.mInFullscreenMode, info.requestWindowName, info.imeControlTargetName, info.imeLayerTargetName));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void hideMySoftInput(IBinder token, int flags) {
        Trace.traceBegin(32L, "IMMS.hideMySoftInput");
        synchronized (ImfLock.class) {
            if (calledWithValidTokenLocked(token)) {
                long ident = Binder.clearCallingIdentity();
                hideCurrentInputLocked(this.mLastImeTargetWindow, flags, null, 4);
                Binder.restoreCallingIdentity(ident);
                Trace.traceEnd(32L);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void showMySoftInput(IBinder token, int flags) {
        Trace.traceBegin(32L, "IMMS.showMySoftInput");
        synchronized (ImfLock.class) {
            if (calledWithValidTokenLocked(token)) {
                long ident = Binder.clearCallingIdentity();
                showCurrentInputLocked(this.mLastImeTargetWindow, flags, null, 2);
                Binder.restoreCallingIdentity(ident);
                Trace.traceEnd(32L);
            }
        }
    }

    void setEnabledSessionLocked(SessionState session) {
        SessionState sessionState = this.mEnabledSession;
        if (sessionState != session) {
            if (sessionState != null && sessionState.session != null) {
                if (DEBUG) {
                    Slog.v(TAG, "Disabling: " + this.mEnabledSession);
                }
                this.mEnabledSession.method.setSessionEnabled(this.mEnabledSession.session, false);
            }
            this.mEnabledSession = session;
            if (session != null && session.session != null) {
                if (DEBUG) {
                    Slog.v(TAG, "Enabling: " + this.mEnabledSession);
                }
                this.mEnabledSession.method.setSessionEnabled(this.mEnabledSession.session, true);
            }
        }
    }

    void setEnabledSessionForAccessibilityLocked(SparseArray<AccessibilitySessionState> accessibilitySessions) {
        AccessibilitySessionState sessionState;
        AccessibilitySessionState sessionState2;
        SparseArray<IAccessibilityInputMethodSession> disabledSessions = new SparseArray<>();
        for (int i = 0; i < this.mEnabledAccessibilitySessions.size(); i++) {
            if (!accessibilitySessions.contains(this.mEnabledAccessibilitySessions.keyAt(i)) && (sessionState2 = this.mEnabledAccessibilitySessions.valueAt(i)) != null) {
                disabledSessions.append(this.mEnabledAccessibilitySessions.keyAt(i), sessionState2.mSession);
            }
        }
        int i2 = disabledSessions.size();
        if (i2 > 0) {
            AccessibilityManagerInternal.get().setImeSessionEnabled(disabledSessions, false);
        }
        SparseArray<IAccessibilityInputMethodSession> enabledSessions = new SparseArray<>();
        for (int i3 = 0; i3 < accessibilitySessions.size(); i3++) {
            if (!this.mEnabledAccessibilitySessions.contains(accessibilitySessions.keyAt(i3)) && (sessionState = accessibilitySessions.valueAt(i3)) != null) {
                enabledSessions.append(accessibilitySessions.keyAt(i3), sessionState.mSession);
            }
        }
        int i4 = enabledSessions.size();
        if (i4 > 0) {
            AccessibilityManagerInternal.get().setImeSessionEnabled(enabledSessions, true);
        }
        this.mEnabledAccessibilitySessions = accessibilitySessions;
    }

    /* JADX DEBUG: Another duplicated slice has different insns count: {[IGET]}, finally: {[IGET, IGET, INVOKE, IF, INVOKE, IF] complete} */
    /* JADX WARN: Code restructure failed: missing block: B:73:0x0130, code lost:
        if (android.os.Binder.isProxy(r1) != false) goto L70;
     */
    /* JADX WARN: Code restructure failed: missing block: B:88:0x015e, code lost:
        if (android.os.Binder.isProxy(r1) != false) goto L94;
     */
    /* JADX WARN: Code restructure failed: missing block: B:89:0x0160, code lost:
        r3.channel.dispose();
     */
    /* JADX WARN: Code restructure failed: missing block: B:97:0x018c, code lost:
        if (android.os.Binder.isProxy(r1) != false) goto L94;
     */
    @Override // android.os.Handler.Callback
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public boolean handleMessage(Message msg) {
        boolean showAuxSubtypes;
        SessionState sessionState;
        IInputMethodClient client;
        InputBindResult res;
        InputChannel inputChannel;
        switch (msg.what) {
            case 1:
                int displayId = msg.arg2;
                switch (msg.arg1) {
                    case 0:
                        showAuxSubtypes = this.mInputShown;
                        break;
                    case 1:
                        showAuxSubtypes = true;
                        break;
                    case 2:
                        showAuxSubtypes = false;
                        break;
                    default:
                        Slog.e(TAG, "Unknown subtype picker mode = " + msg.arg1);
                        return false;
                }
                this.mMenuController.showInputMethodMenu(showAuxSubtypes, displayId);
                return true;
            case MSG_HIDE_CURRENT_INPUT_METHOD /* 1035 */:
                synchronized (ImfLock.class) {
                    int reason = ((Integer) msg.obj).intValue();
                    hideCurrentInputLocked(this.mCurFocusedWindow, 0, null, reason);
                }
                return true;
            case MSG_REMOVE_IME_SURFACE /* 1060 */:
                synchronized (ImfLock.class) {
                    try {
                        SessionState sessionState2 = this.mEnabledSession;
                        if (sessionState2 != null && sessionState2.session != null && !this.mShowRequested) {
                            this.mEnabledSession.session.removeImeSurface();
                        }
                    } catch (RemoteException e) {
                    }
                }
                return true;
            case MSG_REMOVE_IME_SURFACE_FROM_WINDOW /* 1061 */:
                IBinder windowToken = (IBinder) msg.obj;
                synchronized (ImfLock.class) {
                    try {
                        if (windowToken == this.mCurFocusedWindow && (sessionState = this.mEnabledSession) != null && sessionState.session != null) {
                            this.mEnabledSession.session.removeImeSurface();
                        }
                    } catch (RemoteException e2) {
                    }
                }
                return true;
            case MSG_UPDATE_IME_WINDOW_STATUS /* 1070 */:
                updateImeWindowStatus(msg.arg1 == 1);
                return true;
            case MSG_RESET_HANDWRITING /* 1090 */:
                synchronized (ImfLock.class) {
                    if (this.mBindingController.supportsStylusHandwriting() && getCurMethodLocked() != null) {
                        this.mHwController.initializeHandwritingSpy(this.mCurTokenDisplayId);
                    } else {
                        this.mHwController.reset();
                    }
                }
                return true;
            case MSG_START_HANDWRITING /* 1100 */:
                synchronized (ImfLock.class) {
                    IInputMethodInvoker curMethod = getCurMethodLocked();
                    if (curMethod != null && this.mCurFocusedWindow != null) {
                        HandwritingModeController.HandwritingSession session = this.mHwController.startHandwritingSession(msg.arg1, msg.arg2, this.mBindingController.getCurMethodUid(), this.mCurFocusedWindow);
                        if (session == null) {
                            Slog.e(TAG, "Failed to start handwriting session for requestId: " + msg.arg1);
                            return true;
                        }
                        if (!curMethod.startStylusHandwriting(session.getRequestId(), session.getHandwritingChannel(), session.getRecordedEvents())) {
                            Slog.w(TAG, "Resetting handwriting mode.");
                            scheduleResetStylusHandwriting();
                        }
                        return true;
                    }
                    return true;
                }
            case MSG_FINISH_HANDWRITING /* 1110 */:
                synchronized (ImfLock.class) {
                    IInputMethodInvoker curMethod2 = getCurMethodLocked();
                    if (curMethod2 != null && this.mHwController.getCurrentRequestId().isPresent()) {
                        curMethod2.finishStylusHandwriting();
                    }
                }
                return true;
            case MSG_UNBIND_CLIENT /* 3000 */:
                try {
                    ((IInputMethodClient) msg.obj).onUnbindMethod(msg.arg1, msg.arg2);
                } catch (RemoteException e3) {
                    if (!(e3 instanceof DeadObjectException)) {
                        Slog.w(TAG, "RemoteException when unbinding input method service oraccessibility services");
                    }
                }
                return true;
            case MSG_UNBIND_ACCESSIBILITY_SERVICE /* 3001 */:
                SomeArgs args = (SomeArgs) msg.obj;
                IInputMethodClient client2 = (IInputMethodClient) args.arg1;
                int id = ((Integer) args.arg2).intValue();
                try {
                    client2.onUnbindAccessibilityService(msg.arg1, id);
                } catch (RemoteException e4) {
                    if (!(e4 instanceof DeadObjectException)) {
                        Slog.w(TAG, "RemoteException when unbinding accessibility services");
                    }
                }
                args.recycle();
                return true;
            case 3010:
                SomeArgs args2 = (SomeArgs) msg.obj;
                client = (IInputMethodClient) args2.arg1;
                res = (InputBindResult) args2.arg2;
                try {
                    try {
                        client.onBindMethod(res);
                        if (res.channel != null) {
                            break;
                        }
                    } finally {
                        if (res.channel != null && Binder.isProxy(client)) {
                            res.channel.dispose();
                        }
                    }
                } catch (RemoteException e5) {
                    Slog.w(TAG, "Client died receiving input method " + args2.arg2);
                    if (res.channel != null) {
                        break;
                    }
                }
                args2.recycle();
                return true;
            case 3011:
                SomeArgs args3 = (SomeArgs) msg.obj;
                client = (IInputMethodClient) args3.arg1;
                res = (InputBindResult) args3.arg2;
                int id2 = ((Integer) args3.arg3).intValue();
                try {
                    try {
                        client.onBindAccessibilityService(res, id2);
                        if (inputChannel != null) {
                            break;
                        }
                    } finally {
                        if (res.channel != null && Binder.isProxy(client)) {
                            res.channel.dispose();
                        }
                    }
                } catch (RemoteException e6) {
                    Slog.w(TAG, "Client died receiving input method " + args3.arg2);
                    if (res.channel != null) {
                        break;
                    }
                }
                args3.recycle();
                return true;
            case MSG_SET_ACTIVE /* 3020 */:
                SomeArgs args4 = (SomeArgs) msg.obj;
                ClientState clientState = (ClientState) args4.arg1;
                try {
                    clientState.client.setActive(args4.argi1 != 0, args4.argi2 != 0, args4.argi3 != 0);
                } catch (RemoteException e7) {
                    Slog.w(TAG, "Got RemoteException sending setActive(false) notification to pid " + clientState.pid + " uid " + clientState.uid);
                }
                args4.recycle();
                return true;
            case MSG_SET_INTERACTIVE /* 3030 */:
                handleSetInteractive(msg.arg1 != 0);
                return true;
            case MSG_REPORT_FULLSCREEN_MODE /* 3045 */:
                boolean fullscreen = msg.arg1 != 0;
                ClientState clientState2 = (ClientState) msg.obj;
                try {
                    clientState2.client.reportFullscreenMode(fullscreen);
                } catch (RemoteException e8) {
                    Slog.w(TAG, "Got RemoteException sending reportFullscreen(" + fullscreen + ") notification to pid=" + clientState2.pid + " uid=" + clientState2.uid);
                }
                return true;
            case MSG_HARD_KEYBOARD_SWITCH_CHANGED /* 4000 */:
                this.mMenuController.handleHardKeyboardStatusChange(msg.arg1 == 1);
                synchronized (ImfLock.class) {
                    sendOnNavButtonFlagsChangedLocked();
                }
                return true;
            case 5000:
                int userId = msg.arg1;
                onUnlockUser(userId);
                return true;
            case MSG_DISPATCH_ON_INPUT_METHOD_LIST_UPDATED /* 5010 */:
                final int userId2 = msg.arg1;
                final List<InputMethodInfo> imes = (List) msg.obj;
                this.mInputMethodListListeners.forEach(new Consumer() { // from class: com.android.server.inputmethod.InputMethodManagerService$$ExternalSyntheticLambda0
                    @Override // java.util.function.Consumer
                    public final void accept(Object obj) {
                        ((InputMethodManagerInternal.InputMethodListListener) obj).onInputMethodListUpdated(imes, userId2);
                    }
                });
                return true;
            case MSG_NOTIFY_IME_UID_TO_AUDIO_SERVICE /* 7000 */:
                if (this.mAudioManagerInternal == null) {
                    this.mAudioManagerInternal = (AudioManagerInternal) LocalServices.getService(AudioManagerInternal.class);
                }
                AudioManagerInternal audioManagerInternal = this.mAudioManagerInternal;
                if (audioManagerInternal != null) {
                    audioManagerInternal.setInputMethodServiceUid(msg.arg1);
                }
                return true;
            default:
                return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onStylusHandwritingReady(int requestId, int pid) {
        this.mHandler.obtainMessage(MSG_START_HANDWRITING, requestId, pid).sendToTarget();
    }

    private void handleSetInteractive(boolean interactive) {
        synchronized (ImfLock.class) {
            this.mIsInteractive = interactive;
            updateSystemUiLocked(interactive ? this.mImeWindowVis : 0, this.mBackDisposition);
            ClientState clientState = this.mCurClient;
            if (clientState != null && clientState.client != null) {
                scheduleSetActiveToClient(this.mCurClient, this.mIsInteractive, this.mInFullscreenMode, this.mImePlatformCompatUtils.shouldFinishInputWithReportToIme(getCurMethodUidLocked()));
            }
        }
    }

    private void scheduleSetActiveToClient(ClientState state, boolean active, boolean fullscreen, boolean reportToImeController) {
        executeOrSendMessage(state.client, obtainMessageIIIO(MSG_SET_ACTIVE, active ? 1 : 0, fullscreen ? 1 : 0, reportToImeController ? 1 : 0, state));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean chooseNewDefaultIMELocked() {
        InputMethodInfo imi = InputMethodUtils.getMostApplicableDefaultIME(this.mSettings.getEnabledInputMethodListLocked());
        if (imi != null) {
            if (DEBUG) {
                Slog.d(TAG, "New default IME was selected: " + imi.getId());
            }
            resetSelectedInputMethodAndSubtypeLocked(imi.getId());
            return true;
        }
        return false;
    }

    static void queryInputMethodServicesInternal(Context context, int userId, ArrayMap<String, List<InputMethodSubtype>> additionalSubtypeMap, ArrayMap<String, InputMethodInfo> methodMap, ArrayList<InputMethodInfo> methodList, int directBootAwareness) {
        int directBootAwarenessFlags;
        int flags;
        methodList.clear();
        methodMap.clear();
        switch (directBootAwareness) {
            case 0:
                directBootAwarenessFlags = 268435456;
                break;
            case 1:
                directBootAwarenessFlags = 786432;
                break;
            default:
                Slog.e(TAG, "Unknown directBootAwareness=" + directBootAwareness + ". Falling back to DirectBootAwareness.AUTO");
                directBootAwarenessFlags = 268435456;
                break;
        }
        int flags2 = 32896 | directBootAwarenessFlags;
        if (((UserManagerInternal) LocalServices.getService(UserManagerInternal.class)).isUserUnlockingOrUnlocked(userId)) {
            flags = flags2;
        } else {
            if (DEBUG) {
                Slog.d(TAG, "match system only in locking state ");
            }
            flags = flags2 | 1048576;
        }
        List<ResolveInfo> services = context.getPackageManager().queryIntentServicesAsUser(new Intent("android.view.InputMethod"), flags, userId);
        methodList.ensureCapacity(services.size());
        methodMap.ensureCapacity(services.size());
        for (int i = 0; i < services.size(); i++) {
            ResolveInfo ri = services.get(i);
            ServiceInfo si = ri.serviceInfo;
            String imeId = InputMethodInfo.computeId(ri);
            if (!"android.permission.BIND_INPUT_METHOD".equals(si.permission)) {
                Slog.w(TAG, "Skipping input method " + imeId + ": it does not require the permission android.permission.BIND_INPUT_METHOD");
            } else {
                boolean z = DEBUG;
                if (z) {
                    Slog.d(TAG, "Checking " + imeId);
                }
                try {
                    try {
                    } catch (Exception e) {
                        e = e;
                    }
                } catch (Exception e2) {
                    e = e2;
                }
                try {
                    InputMethodInfo imi = new InputMethodInfo(context, ri, additionalSubtypeMap.get(imeId));
                    if (!imi.isVrOnly()) {
                        methodList.add(imi);
                        methodMap.put(imi.getId(), imi);
                        if (z) {
                            Slog.d(TAG, "Found an input method " + imi);
                        }
                    }
                } catch (Exception e3) {
                    e = e3;
                    Slog.wtf(TAG, "Unable to load input method " + imeId, e);
                }
            }
        }
    }

    void buildInputMethodListLocked(boolean resetDefaultEnabledIme) {
        if (DEBUG) {
            Slog.d(TAG, "--- re-buildInputMethodList reset = " + resetDefaultEnabledIme + " \n ------ caller=" + Debug.getCallers(10));
        }
        if (!this.mSystemReady) {
            Slog.e(TAG, "buildInputMethodListLocked is not allowed until system is ready");
            return;
        }
        this.mMethodMapUpdateCount++;
        this.mMyPackageMonitor.clearKnownImePackageNamesLocked();
        queryInputMethodServicesInternal(this.mContext, this.mSettings.getCurrentUserId(), this.mAdditionalSubtypeMap, this.mMethodMap, this.mMethodList, 0);
        List<ResolveInfo> allInputMethodServices = this.mContext.getPackageManager().queryIntentServicesAsUser(new Intent("android.view.InputMethod"), 512, this.mSettings.getCurrentUserId());
        int N = allInputMethodServices.size();
        for (int i = 0; i < N; i++) {
            ServiceInfo si = allInputMethodServices.get(i).serviceInfo;
            if ("android.permission.BIND_INPUT_METHOD".equals(si.permission)) {
                this.mMyPackageMonitor.addKnownImePackageNameLocked(si.packageName);
            }
        }
        boolean reenableMinimumNonAuxSystemImes = false;
        if (!resetDefaultEnabledIme) {
            boolean enabledImeFound = false;
            boolean enabledNonAuxImeFound = false;
            List<InputMethodInfo> enabledImes = this.mSettings.getEnabledInputMethodListLocked();
            int N2 = enabledImes.size();
            int i2 = 0;
            while (true) {
                if (i2 >= N2) {
                    break;
                }
                InputMethodInfo imi = enabledImes.get(i2);
                if (this.mMethodList.contains(imi)) {
                    enabledImeFound = true;
                    if (!imi.isAuxiliaryIme()) {
                        enabledNonAuxImeFound = true;
                        break;
                    }
                }
                i2++;
            }
            if (!enabledImeFound) {
                if (DEBUG) {
                    Slog.i(TAG, "All the enabled IMEs are gone. Reset default enabled IMEs.");
                }
                resetDefaultEnabledIme = true;
                resetSelectedInputMethodAndSubtypeLocked("");
            } else if (!enabledNonAuxImeFound) {
                if (DEBUG) {
                    Slog.i(TAG, "All the enabled non-Aux IMEs are gone. Do partial reset.");
                }
                reenableMinimumNonAuxSystemImes = true;
            }
        }
        if (resetDefaultEnabledIme || reenableMinimumNonAuxSystemImes) {
            ArrayList<InputMethodInfo> defaultEnabledIme = InputMethodUtils.getDefaultEnabledImes(this.mContext, this.mMethodList, reenableMinimumNonAuxSystemImes);
            int N3 = defaultEnabledIme.size();
            for (int i3 = 0; i3 < N3; i3++) {
                InputMethodInfo imi2 = defaultEnabledIme.get(i3);
                if (DEBUG) {
                    Slog.d(TAG, "--- enable ime = " + imi2);
                }
                setInputMethodEnabledLocked(imi2.getId(), true);
            }
        }
        String defaultImiId = this.mSettings.getSelectedInputMethod();
        if (!TextUtils.isEmpty(defaultImiId)) {
            if (!this.mMethodMap.containsKey(defaultImiId)) {
                Slog.w(TAG, "Default IME is uninstalled. Choose new default IME.");
                if (chooseNewDefaultIMELocked()) {
                    updateInputMethodsFromSettingsLocked(true);
                }
            } else {
                setInputMethodEnabledLocked(defaultImiId, true);
            }
        }
        updateDefaultVoiceImeIfNeededLocked();
        this.mSwitchingController.resetCircularListLocked(this.mContext);
        sendOnNavButtonFlagsChangedLocked();
        List<InputMethodInfo> inputMethodList = new ArrayList<>(this.mMethodList);
        this.mHandler.obtainMessage(MSG_DISPATCH_ON_INPUT_METHOD_LIST_UPDATED, this.mSettings.getCurrentUserId(), 0, inputMethodList).sendToTarget();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void sendOnNavButtonFlagsChangedLocked() {
        IInputMethodInvoker curMethod = this.mBindingController.getCurMethod();
        if (curMethod == null) {
            return;
        }
        curMethod.onNavButtonFlagsChanged(getInputMethodNavButtonFlagsLocked());
    }

    private void updateDefaultVoiceImeIfNeededLocked() {
        String systemSpeechRecognizer = this.mContext.getString(17039406);
        String currentDefaultVoiceImeId = this.mSettings.getDefaultVoiceInputMethod();
        InputMethodInfo newSystemVoiceIme = InputMethodUtils.chooseSystemVoiceIme(this.mMethodMap, systemSpeechRecognizer, currentDefaultVoiceImeId);
        if (newSystemVoiceIme == null) {
            if (DEBUG) {
                Slog.i(TAG, "Found no valid default Voice IME. If the user is still locked, this may be expected.");
            }
            if (!TextUtils.isEmpty(currentDefaultVoiceImeId)) {
                this.mSettings.putDefaultVoiceInputMethod("");
            }
        } else if (TextUtils.equals(currentDefaultVoiceImeId, newSystemVoiceIme.getId())) {
        } else {
            if (DEBUG) {
                Slog.i(TAG, "Enabling the default Voice IME:" + newSystemVoiceIme);
            }
            setInputMethodEnabledLocked(newSystemVoiceIme.getId(), true);
            this.mSettings.putDefaultVoiceInputMethod(newSystemVoiceIme.getId());
        }
    }

    private void showInputMethodAndSubtypeEnabler(String inputMethodId) {
        int userId;
        Intent intent = new Intent("android.settings.INPUT_METHOD_SUBTYPE_SETTINGS");
        intent.setFlags(337641472);
        if (!TextUtils.isEmpty(inputMethodId)) {
            intent.putExtra("input_method_id", inputMethodId);
        }
        synchronized (ImfLock.class) {
            userId = this.mSettings.getCurrentUserId();
        }
        this.mContext.startActivityAsUser(intent, null, UserHandle.of(userId));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean setInputMethodEnabledLocked(String id, boolean enabled) {
        List<Pair<String, ArrayList<String>>> enabledInputMethodsList = this.mSettings.getEnabledInputMethodsAndSubtypeListLocked();
        if (enabled) {
            for (Pair<String, ArrayList<String>> pair : enabledInputMethodsList) {
                if (((String) pair.first).equals(id)) {
                    return true;
                }
            }
            this.mSettings.appendAndPutEnabledInputMethodLocked(id, false);
            return false;
        }
        StringBuilder builder = new StringBuilder();
        if (this.mSettings.buildAndPutEnabledInputMethodsStrRemovingIdLocked(builder, enabledInputMethodsList, id)) {
            String selId = this.mSettings.getSelectedInputMethod();
            if (id.equals(selId) && !chooseNewDefaultIMELocked()) {
                Slog.i(TAG, "Can't find new IME, unsetting the current input method.");
                resetSelectedInputMethodAndSubtypeLocked("");
            }
            return true;
        }
        return false;
    }

    private void setSelectedInputMethodAndSubtypeLocked(InputMethodInfo imi, int subtypeId, boolean setSubtypeOnly) {
        this.mSettings.saveCurrentInputMethodAndSubtypeToHistory(getSelectedMethodIdLocked(), this.mCurrentSubtype);
        if (imi == null || subtypeId < 0) {
            this.mSettings.putSelectedSubtype(-1);
            this.mCurrentSubtype = null;
        } else if (subtypeId >= imi.getSubtypeCount()) {
            this.mSettings.putSelectedSubtype(-1);
            this.mCurrentSubtype = getCurrentInputMethodSubtypeLocked();
        } else {
            InputMethodSubtype subtype = imi.getSubtypeAt(subtypeId);
            this.mSettings.putSelectedSubtype(subtype.hashCode());
            this.mCurrentSubtype = subtype;
        }
        if (!setSubtypeOnly) {
            this.mSettings.putSelectedInputMethod(imi != null ? imi.getId() : "");
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void resetSelectedInputMethodAndSubtypeLocked(String newDefaultIme) {
        String subtypeHashCode;
        InputMethodInfo imi = this.mMethodMap.get(newDefaultIme);
        int lastSubtypeId = -1;
        if (imi != null && !TextUtils.isEmpty(newDefaultIme) && (subtypeHashCode = this.mSettings.getLastSubtypeForInputMethodLocked(newDefaultIme)) != null) {
            try {
                lastSubtypeId = InputMethodUtils.getSubtypeIdFromHashCode(imi, Integer.parseInt(subtypeHashCode));
            } catch (NumberFormatException e) {
                Slog.w(TAG, "HashCode for subtype looks broken: " + subtypeHashCode, e);
            }
        }
        setSelectedInputMethodAndSubtypeLocked(imi, lastSubtypeId, false);
    }

    public InputMethodSubtype getCurrentInputMethodSubtype() {
        synchronized (ImfLock.class) {
            if (!calledFromValidUserLocked()) {
                return null;
            }
            return getCurrentInputMethodSubtypeLocked();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public InputMethodSubtype getCurrentInputMethodSubtypeLocked() {
        InputMethodSubtype inputMethodSubtype;
        String selectedMethodId = getSelectedMethodIdLocked();
        if (selectedMethodId == null) {
            return null;
        }
        boolean subtypeIsSelected = this.mSettings.isSubtypeSelected();
        InputMethodInfo imi = this.mMethodMap.get(selectedMethodId);
        if (imi == null || imi.getSubtypeCount() == 0) {
            return null;
        }
        if (!subtypeIsSelected || (inputMethodSubtype = this.mCurrentSubtype) == null || !InputMethodUtils.isValidSubtypeId(imi, inputMethodSubtype.hashCode())) {
            int subtypeId = this.mSettings.getSelectedInputMethodSubtypeId(selectedMethodId);
            if (subtypeId == -1) {
                List<InputMethodSubtype> explicitlyOrImplicitlyEnabledSubtypes = this.mSettings.getEnabledInputMethodSubtypeListLocked(this.mContext, imi, true);
                if (explicitlyOrImplicitlyEnabledSubtypes.size() != 1) {
                    if (explicitlyOrImplicitlyEnabledSubtypes.size() > 1) {
                        InputMethodSubtype findLastResortApplicableSubtypeLocked = InputMethodUtils.findLastResortApplicableSubtypeLocked(this.mRes, explicitlyOrImplicitlyEnabledSubtypes, "keyboard", null, true);
                        this.mCurrentSubtype = findLastResortApplicableSubtypeLocked;
                        if (findLastResortApplicableSubtypeLocked == null) {
                            this.mCurrentSubtype = InputMethodUtils.findLastResortApplicableSubtypeLocked(this.mRes, explicitlyOrImplicitlyEnabledSubtypes, null, null, true);
                        }
                    }
                } else {
                    this.mCurrentSubtype = explicitlyOrImplicitlyEnabledSubtypes.get(0);
                }
            } else {
                this.mCurrentSubtype = InputMethodUtils.getSubtypes(imi).get(subtypeId);
            }
        }
        return this.mCurrentSubtype;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public ArrayMap<String, InputMethodInfo> queryMethodMapForUser(int userId) {
        ArrayMap<String, InputMethodInfo> methodMap = new ArrayMap<>();
        ArrayList<InputMethodInfo> methodList = new ArrayList<>();
        ArrayMap<String, List<InputMethodSubtype>> additionalSubtypeMap = new ArrayMap<>();
        AdditionalSubtypeUtils.load(additionalSubtypeMap, userId);
        queryInputMethodServicesInternal(this.mContext, userId, additionalSubtypeMap, methodMap, methodList, 0);
        return methodMap;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean switchToInputMethodLocked(String imeId, int userId) {
        if (userId == this.mSettings.getCurrentUserId()) {
            if (this.mMethodMap.containsKey(imeId) && this.mSettings.getEnabledInputMethodListLocked().contains(this.mMethodMap.get(imeId))) {
                setInputMethodLocked(imeId, -1);
                return true;
            }
            return false;
        }
        ArrayMap<String, InputMethodInfo> methodMap = queryMethodMapForUser(userId);
        InputMethodUtils.InputMethodSettings settings = new InputMethodUtils.InputMethodSettings(this.mContext.getResources(), this.mContext.getContentResolver(), methodMap, userId, false);
        if (methodMap.containsKey(imeId) && settings.getEnabledInputMethodListLocked().contains(methodMap.get(imeId))) {
            settings.putSelectedInputMethod(imeId);
            settings.putSelectedSubtype(-1);
            return true;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void publishLocalService() {
        LocalServices.addService(InputMethodManagerInternal.class, new LocalServiceImpl());
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class LocalServiceImpl extends InputMethodManagerInternal {
        private LocalServiceImpl() {
        }

        @Override // com.android.server.inputmethod.InputMethodManagerInternal
        public void setInteractive(boolean interactive) {
            InputMethodManagerService.this.mHandler.obtainMessage(InputMethodManagerService.MSG_SET_INTERACTIVE, interactive ? 1 : 0, 0).sendToTarget();
        }

        @Override // com.android.server.inputmethod.InputMethodManagerInternal
        public void hideCurrentInputMethod(int reason) {
            InputMethodManagerService.this.mHandler.removeMessages(InputMethodManagerService.MSG_HIDE_CURRENT_INPUT_METHOD);
            InputMethodManagerService.this.mHandler.obtainMessage(InputMethodManagerService.MSG_HIDE_CURRENT_INPUT_METHOD, Integer.valueOf(reason)).sendToTarget();
        }

        @Override // com.android.server.inputmethod.InputMethodManagerInternal
        public List<InputMethodInfo> getInputMethodListAsUser(int userId) {
            List<InputMethodInfo> inputMethodListLocked;
            synchronized (ImfLock.class) {
                inputMethodListLocked = InputMethodManagerService.this.getInputMethodListLocked(userId, 0);
            }
            return inputMethodListLocked;
        }

        @Override // com.android.server.inputmethod.InputMethodManagerInternal
        public List<InputMethodInfo> getEnabledInputMethodListAsUser(int userId) {
            List<InputMethodInfo> enabledInputMethodListLocked;
            synchronized (ImfLock.class) {
                enabledInputMethodListLocked = InputMethodManagerService.this.getEnabledInputMethodListLocked(userId);
            }
            return enabledInputMethodListLocked;
        }

        @Override // com.android.server.inputmethod.InputMethodManagerInternal
        public void onCreateInlineSuggestionsRequest(int userId, InlineSuggestionsRequestInfo requestInfo, IInlineSuggestionsRequestCallback cb) {
            boolean touchExplorationEnabled = InputMethodManagerService.this.mAccessibilityManager.isTouchExplorationEnabled();
            synchronized (ImfLock.class) {
                InputMethodManagerService.this.onCreateInlineSuggestionsRequestLocked(userId, requestInfo, cb, touchExplorationEnabled);
            }
        }

        @Override // com.android.server.inputmethod.InputMethodManagerInternal
        public boolean switchToInputMethod(String imeId, int userId) {
            boolean switchToInputMethodLocked;
            synchronized (ImfLock.class) {
                switchToInputMethodLocked = InputMethodManagerService.this.switchToInputMethodLocked(imeId, userId);
            }
            return switchToInputMethodLocked;
        }

        @Override // com.android.server.inputmethod.InputMethodManagerInternal
        public boolean setInputMethodEnabled(String imeId, boolean enabled, int userId) {
            synchronized (ImfLock.class) {
                if (userId == InputMethodManagerService.this.mSettings.getCurrentUserId()) {
                    if (InputMethodManagerService.this.mMethodMap.containsKey(imeId)) {
                        InputMethodManagerService.this.setInputMethodEnabledLocked(imeId, enabled);
                        return true;
                    }
                    return false;
                }
                ArrayMap<String, InputMethodInfo> methodMap = InputMethodManagerService.this.queryMethodMapForUser(userId);
                InputMethodUtils.InputMethodSettings settings = new InputMethodUtils.InputMethodSettings(InputMethodManagerService.this.mContext.getResources(), InputMethodManagerService.this.mContext.getContentResolver(), methodMap, userId, false);
                if (methodMap.containsKey(imeId)) {
                    if (enabled) {
                        if (!settings.getEnabledInputMethodListLocked().contains(methodMap.get(imeId))) {
                            settings.appendAndPutEnabledInputMethodLocked(imeId, false);
                        }
                    } else {
                        settings.buildAndPutEnabledInputMethodsStrRemovingIdLocked(new StringBuilder(), settings.getEnabledInputMethodsAndSubtypeListLocked(), imeId);
                    }
                    return true;
                }
                return false;
            }
        }

        @Override // com.android.server.inputmethod.InputMethodManagerInternal
        public void registerInputMethodListListener(InputMethodManagerInternal.InputMethodListListener listener) {
            InputMethodManagerService.this.mInputMethodListListeners.addIfAbsent(listener);
        }

        @Override // com.android.server.inputmethod.InputMethodManagerInternal
        public boolean transferTouchFocusToImeWindow(IBinder sourceInputToken, int displayId) {
            synchronized (ImfLock.class) {
                if (displayId == InputMethodManagerService.this.mCurTokenDisplayId && InputMethodManagerService.this.mCurHostInputToken != null) {
                    IBinder curHostInputToken = InputMethodManagerService.this.mCurHostInputToken;
                    return InputMethodManagerService.this.mInputManagerInternal.transferTouchFocus(sourceInputToken, curHostInputToken);
                }
                return false;
            }
        }

        @Override // com.android.server.inputmethod.InputMethodManagerInternal
        public void reportImeControl(IBinder windowToken) {
            synchronized (ImfLock.class) {
                if (InputMethodManagerService.this.mCurFocusedWindow != windowToken) {
                    InputMethodManagerService.this.mCurPerceptible = true;
                }
            }
        }

        @Override // com.android.server.inputmethod.InputMethodManagerInternal
        public void onImeParentChanged() {
            synchronized (ImfLock.class) {
                InputMethodManagerService.this.mMenuController.hideInputMethodMenu();
            }
        }

        @Override // com.android.server.inputmethod.InputMethodManagerInternal
        public void removeImeSurface() {
            InputMethodManagerService.this.mHandler.obtainMessage(InputMethodManagerService.MSG_REMOVE_IME_SURFACE).sendToTarget();
        }

        @Override // com.android.server.inputmethod.InputMethodManagerInternal
        public void updateImeWindowStatus(boolean disableImeIcon) {
            InputMethodManagerService.this.mHandler.obtainMessage(InputMethodManagerService.MSG_UPDATE_IME_WINDOW_STATUS, disableImeIcon ? 1 : 0, 0).sendToTarget();
        }

        @Override // com.android.server.inputmethod.InputMethodManagerInternal
        public void onSessionForAccessibilityCreated(int accessibilityConnectionId, IAccessibilityInputMethodSession session) {
            synchronized (ImfLock.class) {
                if (InputMethodManagerService.this.mCurClient != null) {
                    InputMethodManagerService inputMethodManagerService = InputMethodManagerService.this;
                    inputMethodManagerService.clearClientSessionForAccessibilityLocked(inputMethodManagerService.mCurClient, accessibilityConnectionId);
                    InputMethodManagerService.this.mCurClient.mAccessibilitySessions.put(accessibilityConnectionId, new AccessibilitySessionState(InputMethodManagerService.this.mCurClient, accessibilityConnectionId, session));
                    InputBindResult res = InputMethodManagerService.this.attachNewAccessibilityLocked(11, true, accessibilityConnectionId);
                    InputMethodManagerService inputMethodManagerService2 = InputMethodManagerService.this;
                    IInputMethodClient iInputMethodClient = inputMethodManagerService2.mCurClient.client;
                    InputMethodManagerService inputMethodManagerService3 = InputMethodManagerService.this;
                    inputMethodManagerService2.executeOrSendMessage(iInputMethodClient, inputMethodManagerService3.obtainMessageOOO(3011, inputMethodManagerService3.mCurClient.client, res, Integer.valueOf(accessibilityConnectionId)));
                }
            }
        }

        @Override // com.android.server.inputmethod.InputMethodManagerInternal
        public void unbindAccessibilityFromCurrentClient(int accessibilityConnectionId) {
            synchronized (ImfLock.class) {
                if (InputMethodManagerService.this.mCurClient != null) {
                    if (InputMethodManagerService.DEBUG) {
                        Slog.v(InputMethodManagerService.TAG, "unbindAccessibilityFromCurrentClientLocked: client=" + InputMethodManagerService.this.mCurClient.client.asBinder());
                    }
                    InputMethodManagerService inputMethodManagerService = InputMethodManagerService.this;
                    IInputMethodClient iInputMethodClient = inputMethodManagerService.mCurClient.client;
                    InputMethodManagerService inputMethodManagerService2 = InputMethodManagerService.this;
                    inputMethodManagerService.executeOrSendMessage(iInputMethodClient, inputMethodManagerService2.obtainMessageIIOO(InputMethodManagerService.MSG_UNBIND_ACCESSIBILITY_SERVICE, inputMethodManagerService2.getSequenceNumberLocked(), 7, InputMethodManagerService.this.mCurClient.client, Integer.valueOf(accessibilityConnectionId)));
                }
                if (InputMethodManagerService.this.getCurMethodLocked() != null) {
                    int numClients = InputMethodManagerService.this.mClients.size();
                    for (int i = 0; i < numClients; i++) {
                        InputMethodManagerService inputMethodManagerService3 = InputMethodManagerService.this;
                        inputMethodManagerService3.clearClientSessionForAccessibilityLocked(inputMethodManagerService3.mClients.valueAt(i), accessibilityConnectionId);
                    }
                    AccessibilitySessionState session = InputMethodManagerService.this.mEnabledAccessibilitySessions.get(accessibilityConnectionId);
                    if (session != null) {
                        InputMethodManagerService.this.finishSessionForAccessibilityLocked(session);
                        InputMethodManagerService.this.mEnabledAccessibilitySessions.remove(accessibilityConnectionId);
                    }
                }
            }
        }

        @Override // com.android.server.inputmethod.InputMethodManagerInternal
        public void maybeFinishStylusHandwriting() {
            InputMethodManagerService.this.mHandler.removeMessages(InputMethodManagerService.MSG_FINISH_HANDWRITING);
            InputMethodManagerService.this.mHandler.obtainMessage(InputMethodManagerService.MSG_FINISH_HANDWRITING).sendToTarget();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public IInputContentUriToken createInputContentUriToken(IBinder token, Uri contentUri, String packageName) {
        if (token == null) {
            throw new NullPointerException("token");
        }
        if (packageName == null) {
            throw new NullPointerException(DomainVerificationLegacySettings.ATTR_PACKAGE_NAME);
        }
        if (contentUri == null) {
            throw new NullPointerException("contentUri");
        }
        String contentUriScheme = contentUri.getScheme();
        if (!ActivityTaskManagerInternal.ASSIST_KEY_CONTENT.equals(contentUriScheme)) {
            throw new InvalidParameterException("contentUri must have content scheme");
        }
        synchronized (ImfLock.class) {
            int uid = Binder.getCallingUid();
            if (getSelectedMethodIdLocked() == null) {
                return null;
            }
            if (getCurTokenLocked() != token) {
                Slog.e(TAG, "Ignoring createInputContentUriToken mCurToken=" + getCurTokenLocked() + " token=" + token);
                return null;
            } else if (!TextUtils.equals(this.mCurAttribute.packageName, packageName)) {
                Slog.e(TAG, "Ignoring createInputContentUriToken mCurAttribute.packageName=" + this.mCurAttribute.packageName + " packageName=" + packageName);
                return null;
            } else {
                int imeUserId = UserHandle.getUserId(uid);
                int appUserId = UserHandle.getUserId(this.mCurClient.uid);
                int contentUriOwnerUserId = ContentProvider.getUserIdFromUri(contentUri, imeUserId);
                Uri contentUriWithoutUserId = ContentProvider.getUriWithoutUserId(contentUri);
                return new InputContentUriTokenHandler(contentUriWithoutUserId, uid, packageName, contentUriOwnerUserId, appUserId);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void reportFullscreenMode(IBinder token, boolean fullscreen) {
        synchronized (ImfLock.class) {
            if (calledWithValidTokenLocked(token)) {
                ClientState clientState = this.mCurClient;
                if (clientState != null && clientState.client != null) {
                    this.mInFullscreenMode = fullscreen;
                    executeOrSendMessage(this.mCurClient.client, this.mHandler.obtainMessage(MSG_REPORT_FULLSCREEN_MODE, fullscreen ? 1 : 0, 0, this.mCurClient));
                }
            }
        }
    }

    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (DumpUtils.checkDumpPermission(this.mContext, TAG, pw)) {
            PriorityDump.dump(this.mPriorityDumper, fd, pw, args);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void dumpAsStringNoCheck(FileDescriptor fd, PrintWriter pw, String[] args, boolean isCritical) {
        ClientState client;
        ClientState focusedWindowClient;
        IInputMethodInvoker method;
        Printer p = new PrintWriterPrinter(pw);
        synchronized (ImfLock.class) {
            p.println("Current Input Method Manager state:");
            int N = this.mMethodList.size();
            p.println("  Input Methods: mMethodMapUpdateCount=" + this.mMethodMapUpdateCount);
            for (int i = 0; i < N; i++) {
                InputMethodInfo info = this.mMethodList.get(i);
                p.println("  InputMethod #" + i + ":");
                info.dump(p, "    ");
            }
            p.println("  Clients:");
            int numClients = this.mClients.size();
            for (int i2 = 0; i2 < numClients; i2++) {
                ClientState ci = this.mClients.valueAt(i2);
                p.println("  Client " + ci + ":");
                p.println("    client=" + ci.client);
                p.println("    inputContext=" + ci.inputContext);
                p.println("    sessionRequested=" + ci.sessionRequested);
                p.println("    sessionRequestedForAccessibility=" + ci.mSessionRequestedForAccessibility);
                p.println("    curSession=" + ci.curSession);
            }
            p.println("  mCurMethodId=" + getSelectedMethodIdLocked());
            client = this.mCurClient;
            p.println("  mCurClient=" + client + " mCurSeq=" + getSequenceNumberLocked());
            p.println("  mCurPerceptible=" + this.mCurPerceptible);
            p.println("  mCurFocusedWindow=" + this.mCurFocusedWindow + " softInputMode=" + InputMethodDebug.softInputModeToString(this.mCurFocusedWindowSoftInputMode) + " client=" + this.mCurFocusedWindowClient);
            focusedWindowClient = this.mCurFocusedWindowClient;
            p.println("  mCurId=" + getCurIdLocked() + " mHaveConnection=" + hasConnectionLocked() + " mBoundToMethod=" + this.mBoundToMethod + " mVisibleBound=" + this.mBindingController.isVisibleBound());
            p.println("  mCurToken=" + getCurTokenLocked());
            p.println("  mCurTokenDisplayId=" + this.mCurTokenDisplayId);
            p.println("  mCurHostInputToken=" + this.mCurHostInputToken);
            p.println("  mCurIntent=" + getCurIntentLocked());
            method = getCurMethodLocked();
            p.println("  mCurMethod=" + getCurMethodLocked());
            p.println("  mEnabledSession=" + this.mEnabledSession);
            p.println("  mShowRequested=" + this.mShowRequested + " mShowExplicitlyRequested=" + this.mShowExplicitlyRequested + " mShowForced=" + this.mShowForced + " mInputShown=" + this.mInputShown);
            p.println("  mInFullscreenMode=" + this.mInFullscreenMode);
            p.println("  mSystemReady=" + this.mSystemReady + " mInteractive=" + this.mIsInteractive);
            p.println("  mSettingsObserver=" + this.mSettingsObserver);
            p.println("  mImeHiddenByDisplayPolicy=" + this.mImeHiddenByDisplayPolicy);
            p.println("  mSwitchingController:");
            this.mSwitchingController.dump(p);
            p.println("  mSettings:");
            this.mSettings.dumpLocked(p, "    ");
            p.println("  mStartInputHistory:");
            this.mStartInputHistory.dump(pw, "   ");
            p.println("  mSoftInputShowHideHistory:");
            this.mSoftInputShowHideHistory.dump(pw, "   ");
        }
        if (isCritical) {
            return;
        }
        p.println(" ");
        if (client != null) {
            pw.flush();
            try {
                TransferPipe.dumpAsync(client.client.asBinder(), fd, args);
            } catch (RemoteException | IOException e) {
                p.println("Failed to dump input method client: " + e);
            }
        } else {
            p.println("No input method client.");
        }
        if (focusedWindowClient != null && client != focusedWindowClient) {
            p.println(" ");
            p.println("Warning: Current input method client doesn't match the last focused. window.");
            p.println("Dumping input method client in the last focused window just in case.");
            p.println(" ");
            pw.flush();
            try {
                TransferPipe.dumpAsync(focusedWindowClient.client.asBinder(), fd, args);
            } catch (RemoteException | IOException e2) {
                p.println("Failed to dump input method client in focused window: " + e2);
            }
        }
        p.println(" ");
        if (method != null) {
            pw.flush();
            try {
                TransferPipe.dumpAsync(method.asBinder(), fd, args);
                return;
            } catch (RemoteException | IOException e3) {
                p.println("Failed to dump input method service: " + e3);
                return;
            }
        }
        p.println("No input method service.");
    }

    /* JADX DEBUG: Multi-variable search result rejected for r11v0, resolved type: com.android.server.inputmethod.InputMethodManagerService */
    /* JADX WARN: Multi-variable type inference failed */
    public void onShellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ShellCallback callback, ResultReceiver resultReceiver) throws RemoteException {
        int callingUid = Binder.getCallingUid();
        if (callingUid != 0 && callingUid != 2000) {
            if (resultReceiver != null) {
                resultReceiver.send(-1, null);
            }
            String errorMsg = "InputMethodManagerService does not support shell commands from non-shell users. callingUid=" + callingUid + " args=" + Arrays.toString(args);
            if (Process.isCoreUid(callingUid)) {
                Slog.e(TAG, errorMsg);
                return;
            }
            throw new SecurityException(errorMsg);
        }
        new ShellCommandImpl(this).exec(this, in, out, err, args, callback, resultReceiver);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static final class ShellCommandImpl extends ShellCommand {
        final InputMethodManagerService mService;

        ShellCommandImpl(InputMethodManagerService service) {
            this.mService = service;
        }

        public int onCommand(String cmd) {
            Arrays.asList("android.permission.DUMP", "android.permission.INTERACT_ACROSS_USERS_FULL", "android.permission.WRITE_SECURE_SETTINGS").forEach(new Consumer() { // from class: com.android.server.inputmethod.InputMethodManagerService$ShellCommandImpl$$ExternalSyntheticLambda0
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    InputMethodManagerService.ShellCommandImpl.this.m4025x1bfad0c5((String) obj);
                }
            });
            long identity = Binder.clearCallingIdentity();
            try {
                return onCommandWithSystemIdentity(cmd);
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onCommand$0$com-android-server-inputmethod-InputMethodManagerService$ShellCommandImpl  reason: not valid java name */
        public /* synthetic */ void m4025x1bfad0c5(String permission) {
            this.mService.mContext.enforceCallingPermission(permission, null);
        }

        /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
        /* JADX WARN: Code restructure failed: missing block: B:31:0x0070, code lost:
            if (r0.equals("help") != false) goto L15;
         */
        /*
            Code decompiled incorrectly, please refer to instructions dump.
        */
        private int onCommandWithSystemIdentity(String cmd) {
            boolean z;
            String emptyIfNull = TextUtils.emptyIfNull(cmd);
            char c = 2;
            switch (emptyIfNull.hashCode()) {
                case -1180406812:
                    if (emptyIfNull.equals("get-last-switch-user-id")) {
                        z = false;
                        break;
                    }
                    z = true;
                    break;
                case -1067396926:
                    if (emptyIfNull.equals("tracing")) {
                        z = true;
                        break;
                    }
                    z = true;
                    break;
                case 104385:
                    if (emptyIfNull.equals("ime")) {
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
                    return this.mService.getLastSwitchUserId(this);
                case true:
                    return this.mService.handleShellCommandTraceInputMethod(this);
                case true:
                    String imeCommand = TextUtils.emptyIfNull(getNextArg());
                    switch (imeCommand.hashCode()) {
                        case -1298848381:
                            if (imeCommand.equals("enable")) {
                                c = 4;
                                break;
                            }
                            c = 65535;
                            break;
                        case -1067396926:
                            if (imeCommand.equals("tracing")) {
                                c = '\b';
                                break;
                            }
                            c = 65535;
                            break;
                        case 0:
                            if (imeCommand.equals("")) {
                                c = 0;
                                break;
                            }
                            c = 65535;
                            break;
                        case 1499:
                            if (imeCommand.equals("-h")) {
                                c = 1;
                                break;
                            }
                            c = 65535;
                            break;
                        case 113762:
                            if (imeCommand.equals("set")) {
                                c = 6;
                                break;
                            }
                            c = 65535;
                            break;
                        case 3198785:
                            break;
                        case 3322014:
                            if (imeCommand.equals("list")) {
                                c = 3;
                                break;
                            }
                            c = 65535;
                            break;
                        case 108404047:
                            if (imeCommand.equals("reset")) {
                                c = 7;
                                break;
                            }
                            c = 65535;
                            break;
                        case 1671308008:
                            if (imeCommand.equals("disable")) {
                                c = 5;
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
                        case 2:
                            return onImeCommandHelp();
                        case 3:
                            return this.mService.handleShellCommandListInputMethods(this);
                        case 4:
                            return this.mService.handleShellCommandEnableDisableInputMethod(this, true);
                        case 5:
                            return this.mService.handleShellCommandEnableDisableInputMethod(this, false);
                        case 6:
                            return this.mService.handleShellCommandSetInputMethod(this);
                        case 7:
                            return this.mService.handleShellCommandResetInputMethod(this);
                        case '\b':
                            return this.mService.handleShellCommandTraceInputMethod(this);
                        default:
                            getOutPrintWriter().println("Unknown command: " + imeCommand);
                            return -1;
                    }
                default:
                    return handleDefaultCommands(cmd);
            }
        }

        public void onHelp() {
            PrintWriter pw = getOutPrintWriter();
            try {
                pw.println("InputMethodManagerService commands:");
                pw.println("  help");
                pw.println("    Prints this help text.");
                pw.println("  dump [options]");
                pw.println("    Synonym of dumpsys.");
                pw.println("  ime <command> [options]");
                pw.println("    Manipulate IMEs.  Run \"ime help\" for details.");
                pw.println("  tracing <command>");
                pw.println("    start: Start tracing.");
                pw.println("    stop : Stop tracing.");
                pw.println("    help : Show help.");
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

        private int onImeCommandHelp() {
            IndentingPrintWriter pw = new IndentingPrintWriter(getOutPrintWriter(), "  ", 100);
            try {
                pw.println("ime <command>:");
                pw.increaseIndent();
                pw.println("list [-a] [-s]");
                pw.increaseIndent();
                pw.println("prints all enabled input methods.");
                pw.increaseIndent();
                pw.println("-a: see all input methods");
                pw.println("-s: only a single summary line of each");
                pw.decreaseIndent();
                pw.decreaseIndent();
                pw.println("enable [--user <USER_ID>] <ID>");
                pw.increaseIndent();
                pw.println("allows the given input method ID to be used.");
                pw.increaseIndent();
                pw.print("--user <USER_ID>: Specify which user to enable.");
                pw.println(" Assumes the current user if not specified.");
                pw.decreaseIndent();
                pw.decreaseIndent();
                pw.println("disable [--user <USER_ID>] <ID>");
                pw.increaseIndent();
                pw.println("disallows the given input method ID to be used.");
                pw.increaseIndent();
                pw.print("--user <USER_ID>: Specify which user to disable.");
                pw.println(" Assumes the current user if not specified.");
                pw.decreaseIndent();
                pw.decreaseIndent();
                pw.println("set [--user <USER_ID>] <ID>");
                pw.increaseIndent();
                pw.println("switches to the given input method ID.");
                pw.increaseIndent();
                pw.print("--user <USER_ID>: Specify which user to enable.");
                pw.println(" Assumes the current user if not specified.");
                pw.decreaseIndent();
                pw.decreaseIndent();
                pw.println("reset [--user <USER_ID>]");
                pw.increaseIndent();
                pw.println("reset currently selected/enabled IMEs to the default ones as if the device is initially booted with the current locale.");
                pw.increaseIndent();
                pw.print("--user <USER_ID>: Specify which user to reset.");
                pw.println(" Assumes the current user if not specified.");
                pw.decreaseIndent();
                pw.decreaseIndent();
                pw.decreaseIndent();
                pw.close();
                return 0;
            } catch (Throwable th) {
                try {
                    pw.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
                throw th;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int getLastSwitchUserId(ShellCommand shellCommand) {
        synchronized (ImfLock.class) {
            shellCommand.getOutPrintWriter().println(this.mLastSwitchUserId);
        }
        return 0;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int handleShellCommandListInputMethods(ShellCommand shellCommand) {
        List<InputMethodInfo> methods;
        int userIdToBeResolved = -2;
        boolean brief = false;
        boolean brief2 = false;
        while (true) {
            String nextOption = shellCommand.getNextOption();
            int i = 1;
            if (nextOption != null) {
                char c = 65535;
                switch (nextOption.hashCode()) {
                    case 1492:
                        if (nextOption.equals("-a")) {
                            c = 0;
                            break;
                        }
                        break;
                    case 1510:
                        if (nextOption.equals("-s")) {
                            c = 1;
                            break;
                        }
                        break;
                    case 1512:
                        if (nextOption.equals("-u")) {
                            c = 2;
                            break;
                        }
                        break;
                    case 1333469547:
                        if (nextOption.equals("--user")) {
                            c = 3;
                            break;
                        }
                        break;
                }
                switch (c) {
                    case 0:
                        brief2 = true;
                        break;
                    case 1:
                        brief = true;
                        break;
                    case 2:
                    case 3:
                        userIdToBeResolved = UserHandle.parseUserArg(shellCommand.getNextArgRequired());
                        break;
                }
            } else {
                synchronized (ImfLock.class) {
                    final PrintWriter pr = shellCommand.getOutPrintWriter();
                    int[] userIds = InputMethodUtils.resolveUserId(userIdToBeResolved, this.mSettings.getCurrentUserId(), shellCommand.getErrPrintWriter());
                    int length = userIds.length;
                    int i2 = 0;
                    while (i2 < length) {
                        int userId = userIds[i2];
                        if (brief2) {
                            methods = getInputMethodListLocked(userId, 0);
                        } else {
                            methods = getEnabledInputMethodListLocked(userId);
                        }
                        if (userIds.length > i) {
                            pr.print("User #");
                            pr.print(userId);
                            pr.println(":");
                        }
                        for (InputMethodInfo info : methods) {
                            if (brief) {
                                pr.println(info.getId());
                            } else {
                                pr.print(info.getId());
                                pr.println(":");
                                Objects.requireNonNull(pr);
                                info.dump(new Printer() { // from class: com.android.server.inputmethod.InputMethodManagerService$$ExternalSyntheticLambda2
                                    @Override // android.util.Printer
                                    public final void println(String str) {
                                        pr.println(str);
                                    }
                                }, "  ");
                            }
                        }
                        i2++;
                        i = 1;
                    }
                }
                return 0;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX WARN: Unsupported multi-entry loop pattern (BACK_EDGE: B:18:0x0056 -> B:19:0x0057). Please submit an issue!!! */
    public int handleShellCommandEnableDisableInputMethod(ShellCommand shellCommand, boolean enabled) {
        int i;
        int userIdToBeResolved = handleOptionsForCommandsThatOnlyHaveUserOption(shellCommand);
        String imeId = shellCommand.getNextArgRequired();
        PrintWriter out = shellCommand.getOutPrintWriter();
        PrintWriter error = shellCommand.getErrPrintWriter();
        synchronized (ImfLock.class) {
            try {
                int[] userIds = InputMethodUtils.resolveUserId(userIdToBeResolved, this.mSettings.getCurrentUserId(), shellCommand.getErrPrintWriter());
                int length = userIds.length;
                boolean hasFailed = false;
                int i2 = 0;
                while (i2 < length) {
                    try {
                        int userId = userIds[i2];
                        if (!userHasDebugPriv(userId, shellCommand)) {
                            i = i2;
                        } else {
                            i = i2;
                            hasFailed |= !handleShellCommandEnableDisableInputMethodInternalLocked(userId, imeId, enabled, out, error);
                        }
                        i2 = i + 1;
                    } catch (Throwable th) {
                        th = th;
                        throw th;
                    }
                }
                return hasFailed ? -1 : 0;
            } catch (Throwable th2) {
                th = th2;
            }
        }
    }

    private static int handleOptionsForCommandsThatOnlyHaveUserOption(ShellCommand shellCommand) {
        while (true) {
            String nextOption = shellCommand.getNextOption();
            if (nextOption != null) {
                char c = 65535;
                switch (nextOption.hashCode()) {
                    case 1512:
                        if (nextOption.equals("-u")) {
                            c = 0;
                            continue;
                        } else {
                            continue;
                        }
                    case 1333469547:
                        if (nextOption.equals("--user")) {
                            c = 1;
                            continue;
                        } else {
                            continue;
                        }
                }
                switch (c) {
                    case 0:
                    case 1:
                        return UserHandle.parseUserArg(shellCommand.getNextArgRequired());
                }
            }
            return -2;
        }
    }

    private boolean handleShellCommandEnableDisableInputMethodInternalLocked(int userId, String imeId, boolean enabled, PrintWriter out, PrintWriter error) {
        boolean failedToEnableUnknownIme = false;
        boolean previouslyEnabled = false;
        if (userId == this.mSettings.getCurrentUserId()) {
            if (enabled && !this.mMethodMap.containsKey(imeId)) {
                failedToEnableUnknownIme = true;
            } else {
                previouslyEnabled = setInputMethodEnabledLocked(imeId, enabled);
            }
        } else {
            ArrayMap<String, InputMethodInfo> methodMap = queryMethodMapForUser(userId);
            InputMethodUtils.InputMethodSettings settings = new InputMethodUtils.InputMethodSettings(this.mContext.getResources(), this.mContext.getContentResolver(), methodMap, userId, false);
            if (enabled) {
                if (!methodMap.containsKey(imeId)) {
                    failedToEnableUnknownIme = true;
                } else {
                    Iterator<InputMethodInfo> it = settings.getEnabledInputMethodListLocked().iterator();
                    while (true) {
                        if (!it.hasNext()) {
                            break;
                        }
                        InputMethodInfo imi = it.next();
                        if (TextUtils.equals(imi.getId(), imeId)) {
                            previouslyEnabled = true;
                            break;
                        }
                    }
                    if (!previouslyEnabled) {
                        settings.appendAndPutEnabledInputMethodLocked(imeId, false);
                    }
                }
            } else {
                previouslyEnabled = settings.buildAndPutEnabledInputMethodsStrRemovingIdLocked(new StringBuilder(), settings.getEnabledInputMethodsAndSubtypeListLocked(), imeId);
            }
        }
        if (failedToEnableUnknownIme) {
            error.print("Unknown input method ");
            error.print(imeId);
            error.println(" cannot be enabled for user #" + userId);
            Slog.e(TAG, "\"ime enable " + imeId + "\" for user #" + userId + " failed due to its unrecognized IME ID.");
            return false;
        }
        out.print("Input method ");
        out.print(imeId);
        out.print(": ");
        out.print(enabled == previouslyEnabled ? "already " : "now ");
        out.print(enabled ? ServiceConfigAccessor.PROVIDER_MODE_ENABLED : ServiceConfigAccessor.PROVIDER_MODE_DISABLED);
        out.print(" for user #");
        out.println(userId);
        return true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int handleShellCommandSetInputMethod(ShellCommand shellCommand) {
        int userIdToBeResolved = handleOptionsForCommandsThatOnlyHaveUserOption(shellCommand);
        String imeId = shellCommand.getNextArgRequired();
        PrintWriter out = shellCommand.getOutPrintWriter();
        PrintWriter error = shellCommand.getErrPrintWriter();
        boolean hasFailed = false;
        synchronized (ImfLock.class) {
            try {
                int[] userIds = InputMethodUtils.resolveUserId(userIdToBeResolved, this.mSettings.getCurrentUserId(), shellCommand.getErrPrintWriter());
                for (int userId : userIds) {
                    try {
                        if (userHasDebugPriv(userId, shellCommand)) {
                            boolean failedToSelectUnknownIme = !switchToInputMethodLocked(imeId, userId);
                            if (failedToSelectUnknownIme) {
                                error.print("Unknown input method ");
                                error.print(imeId);
                                error.print(" cannot be selected for user #");
                                error.println(userId);
                                Slog.e(TAG, "\"ime set " + imeId + "\" for user #" + userId + " failed due to its unrecognized IME ID.");
                            } else {
                                out.print("Input method ");
                                out.print(imeId);
                                out.print(" selected for user #");
                                out.println(userId);
                            }
                            hasFailed |= failedToSelectUnknownIme;
                        }
                    } catch (Throwable th) {
                        th = th;
                        throw th;
                    }
                }
                return hasFailed ? -1 : 0;
            } catch (Throwable th2) {
                th = th2;
            }
        }
    }

    /* JADX DEBUG: Multi-variable search result rejected for r20v0, resolved type: com.android.server.inputmethod.InputMethodManagerService */
    /* JADX INFO: Access modifiers changed from: private */
    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r6v0 */
    /* JADX WARN: Type inference failed for: r6v1, types: [int, boolean] */
    /* JADX WARN: Type inference failed for: r6v12 */
    public int handleShellCommandResetInputMethod(ShellCommand shellCommand) {
        List<InputMethodInfo> nextEnabledImes;
        String nextIme;
        final PrintWriter out = shellCommand.getOutPrintWriter();
        int userIdToBeResolved = handleOptionsForCommandsThatOnlyHaveUserOption(shellCommand);
        synchronized (ImfLock.class) {
            int[] userIds = InputMethodUtils.resolveUserId(userIdToBeResolved, this.mSettings.getCurrentUserId(), shellCommand.getErrPrintWriter());
            int length = userIds.length;
            ?? r6 = 0;
            int i = 0;
            while (i < length) {
                int userId = userIds[i];
                if (userHasDebugPriv(userId, shellCommand)) {
                    if (userId == this.mSettings.getCurrentUserId()) {
                        hideCurrentInputLocked(this.mCurFocusedWindow, r6, null, 14);
                        this.mBindingController.unbindCurrentMethod();
                        resetSelectedInputMethodAndSubtypeLocked(null);
                        this.mSettings.putSelectedInputMethod(null);
                        Iterator<InputMethodInfo> it = this.mSettings.getEnabledInputMethodListLocked().iterator();
                        while (it.hasNext()) {
                            InputMethodInfo inputMethodInfo = it.next();
                            setInputMethodEnabledLocked(inputMethodInfo.getId(), r6);
                        }
                        Iterator<InputMethodInfo> it2 = InputMethodUtils.getDefaultEnabledImes(this.mContext, this.mMethodList).iterator();
                        while (it2.hasNext()) {
                            InputMethodInfo imi = it2.next();
                            setInputMethodEnabledLocked(imi.getId(), true);
                        }
                        updateInputMethodsFromSettingsLocked(true);
                        InputMethodUtils.setNonSelectedSystemImesDisabledUntilUsed(getPackageManagerForUser(this.mContext, this.mSettings.getCurrentUserId()), this.mSettings.getEnabledInputMethodListLocked());
                        nextIme = this.mSettings.getSelectedInputMethod();
                        nextEnabledImes = this.mSettings.getEnabledInputMethodListLocked();
                    } else {
                        ArrayMap<String, InputMethodInfo> methodMap = new ArrayMap<>();
                        ArrayList<InputMethodInfo> methodList = new ArrayList<>();
                        ArrayMap<String, List<InputMethodSubtype>> additionalSubtypeMap = new ArrayMap<>();
                        AdditionalSubtypeUtils.load(additionalSubtypeMap, userId);
                        queryInputMethodServicesInternal(this.mContext, userId, additionalSubtypeMap, methodMap, methodList, 0);
                        final InputMethodUtils.InputMethodSettings settings = new InputMethodUtils.InputMethodSettings(this.mContext.getResources(), this.mContext.getContentResolver(), methodMap, userId, false);
                        nextEnabledImes = InputMethodUtils.getDefaultEnabledImes(this.mContext, methodList);
                        String nextIme2 = InputMethodUtils.getMostApplicableDefaultIME(nextEnabledImes).getId();
                        settings.putEnabledInputMethodsStr("");
                        nextEnabledImes.forEach(new Consumer() { // from class: com.android.server.inputmethod.InputMethodManagerService$$ExternalSyntheticLambda6
                            @Override // java.util.function.Consumer
                            public final void accept(Object obj) {
                                InputMethodUtils.InputMethodSettings.this.appendAndPutEnabledInputMethodLocked(((InputMethodInfo) obj).getId(), false);
                            }
                        });
                        settings.putSelectedInputMethod(nextIme2);
                        settings.putSelectedSubtype(-1);
                        nextIme = nextIme2;
                    }
                    out.println("Reset current and enabled IMEs for user #" + userId);
                    out.println("  Selected: " + nextIme);
                    nextEnabledImes.forEach(new Consumer() { // from class: com.android.server.inputmethod.InputMethodManagerService$$ExternalSyntheticLambda7
                        @Override // java.util.function.Consumer
                        public final void accept(Object obj) {
                            InputMethodInfo inputMethodInfo2 = (InputMethodInfo) obj;
                            out.println("   Enabled: " + inputMethodInfo2.getId());
                        }
                    });
                }
                i++;
                r6 = 0;
            }
        }
        return 0;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    public int handleShellCommandTraceInputMethod(ShellCommand shellCommand) {
        char c;
        ArrayMap<IBinder, ClientState> clients;
        String cmd = shellCommand.getNextArgRequired();
        PrintWriter pw = shellCommand.getOutPrintWriter();
        switch (cmd.hashCode()) {
            case -390772652:
                if (cmd.equals("save-for-bugreport")) {
                    c = 2;
                    break;
                }
                c = 65535;
                break;
            case 3540994:
                if (cmd.equals("stop")) {
                    c = 1;
                    break;
                }
                c = 65535;
                break;
            case 109757538:
                if (cmd.equals("start")) {
                    c = 0;
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
                ImeTracing.getInstance().startTrace(pw);
                break;
            case 1:
                ImeTracing.getInstance().stopTrace(pw);
                break;
            case 2:
                ImeTracing.getInstance().saveForBugreport(pw);
                return 0;
            default:
                pw.println("Unknown command: " + cmd);
                pw.println("Input method trace options:");
                pw.println("  start: Start tracing");
                pw.println("  stop: Stop tracing");
                return -1;
        }
        boolean isImeTraceEnabled = ImeTracing.getInstance().isEnabled();
        synchronized (ImfLock.class) {
            clients = new ArrayMap<>(this.mClients);
        }
        for (ClientState state : clients.values()) {
            if (state != null) {
                try {
                    state.client.setImeTraceEnabled(isImeTraceEnabled);
                } catch (RemoteException e) {
                    Slog.e(TAG, "Error while trying to enable/disable ime trace on client window", e);
                }
            }
        }
        return 0;
    }

    private boolean userHasDebugPriv(int userId, ShellCommand shellCommand) {
        if (this.mUserManager.hasUserRestriction("no_debugging_features", UserHandle.of(userId))) {
            shellCommand.getErrPrintWriter().println("User #" + userId + " is restricted with DISALLOW_DEBUGGING_FEATURES.");
            return false;
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static final class InputMethodPrivilegedOperationsImpl extends IInputMethodPrivilegedOperations.Stub {
        private final InputMethodManagerService mImms;
        private final IBinder mToken;

        InputMethodPrivilegedOperationsImpl(InputMethodManagerService imms, IBinder token) {
            this.mImms = imms;
            this.mToken = token;
        }

        public void setImeWindowStatusAsync(int vis, int backDisposition) {
            this.mImms.setImeWindowStatus(this.mToken, vis, backDisposition);
        }

        public void reportStartInputAsync(IBinder startInputToken) {
            this.mImms.reportStartInput(this.mToken, startInputToken);
        }

        public void createInputContentUriToken(Uri contentUri, String packageName, AndroidFuture future) {
            try {
                future.complete(this.mImms.createInputContentUriToken(this.mToken, contentUri, packageName).asBinder());
            } catch (Throwable e) {
                future.completeExceptionally(e);
            }
        }

        public void reportFullscreenModeAsync(boolean fullscreen) {
            this.mImms.reportFullscreenMode(this.mToken, fullscreen);
        }

        public void setInputMethod(String id, AndroidFuture future) {
            try {
                this.mImms.setInputMethod(this.mToken, id);
                future.complete((Object) null);
            } catch (Throwable e) {
                future.completeExceptionally(e);
            }
        }

        public void setInputMethodAndSubtype(String id, InputMethodSubtype subtype, AndroidFuture future) {
            try {
                this.mImms.setInputMethodAndSubtype(this.mToken, id, subtype);
                future.complete((Object) null);
            } catch (Throwable e) {
                future.completeExceptionally(e);
            }
        }

        public void hideMySoftInput(int flags, AndroidFuture future) {
            try {
                this.mImms.hideMySoftInput(this.mToken, flags);
                future.complete((Object) null);
            } catch (Throwable e) {
                future.completeExceptionally(e);
            }
        }

        public void showMySoftInput(int flags, AndroidFuture future) {
            try {
                this.mImms.showMySoftInput(this.mToken, flags);
                future.complete((Object) null);
            } catch (Throwable e) {
                future.completeExceptionally(e);
            }
        }

        public void updateStatusIconAsync(String packageName, int iconId) {
            this.mImms.updateStatusIcon(this.mToken, packageName, iconId);
        }

        public void switchToPreviousInputMethod(AndroidFuture future) {
            try {
                future.complete(Boolean.valueOf(this.mImms.switchToPreviousInputMethod(this.mToken)));
            } catch (Throwable e) {
                future.completeExceptionally(e);
            }
        }

        public void switchToNextInputMethod(boolean onlyCurrentIme, AndroidFuture future) {
            try {
                future.complete(Boolean.valueOf(this.mImms.switchToNextInputMethod(this.mToken, onlyCurrentIme)));
            } catch (Throwable e) {
                future.completeExceptionally(e);
            }
        }

        public void shouldOfferSwitchingToNextInputMethod(AndroidFuture future) {
            try {
                future.complete(Boolean.valueOf(this.mImms.shouldOfferSwitchingToNextInputMethod(this.mToken)));
            } catch (Throwable e) {
                future.completeExceptionally(e);
            }
        }

        public void notifyUserActionAsync() {
            this.mImms.notifyUserAction(this.mToken);
        }

        public void applyImeVisibilityAsync(IBinder windowToken, boolean setVisible) {
            this.mImms.applyImeVisibility(this.mToken, windowToken, setVisible);
        }

        public void onStylusHandwritingReady(int requestId, int pid) {
            this.mImms.onStylusHandwritingReady(requestId, pid);
        }

        public void resetStylusHandwriting(int requestId) {
            this.mImms.resetStylusHandwriting(requestId);
        }
    }

    public void commitConnectKeyAndText(KeyEvent keyEvent, String text) {
        synchronized (this.mMethodMap) {
            if (this.mCurInputContext != null) {
                hideCurrentInputClient(this.mCurFocusedWindow);
                int sessionId = ITranInputMethodManagerService.Instance().getConnectSessionId();
                try {
                    Slog.d(TAG, "commitConnectKeyAndText keyEvent: " + keyEvent + " text: " + text + " sessionId: " + sessionId);
                    if (keyEvent != null && text == null) {
                        this.mCurInputContext.sendKeyEvent(new InputConnectionCommandHeader(sessionId), keyEvent);
                    } else if (keyEvent == null && text != null) {
                        this.mCurInputContext.commitText(new InputConnectionCommandHeader(sessionId), text, 1);
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void setConnectSessionId(int sessionId) {
        ITranInputMethodManagerService.Instance().setConnectSessionId(sessionId);
    }

    private void hideCurrentInputClient(IBinder windowToken) {
        IInputMethodInvoker curMethod = getCurMethodLocked();
        if (ITranInputMethodManagerService.Instance().getConnectInputShow() && curMethod != null && this.mInputShown) {
            Binder hideInputToken = new Binder();
            this.mHideRequestWindowMap.put(hideInputToken, windowToken);
            if (curMethod.hideSoftInput(hideInputToken, 0, null)) {
                onShowHideSoftInputRequested(false, windowToken, 4);
            }
            ITranInputMethodManagerService.Instance().setConnectInputShow(false);
        }
    }
}
