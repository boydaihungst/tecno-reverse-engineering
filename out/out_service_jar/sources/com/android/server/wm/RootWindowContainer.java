package com.android.server.wm;

import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.app.ActivityTaskManager;
import android.app.AppGlobals;
import android.app.ThunderbackConfig;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.hardware.display.DisplayManager;
import android.hardware.display.DisplayManagerInternal;
import android.hardware.usb.gadget.V1_2.GadgetFunction;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.Trace;
import android.os.UserHandle;
import android.os.storage.StorageManager;
import android.provider.Settings;
import android.service.voice.IVoiceInteractionSession;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.IntArray;
import android.util.Pair;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.util.TimeUtils;
import android.util.proto.ProtoOutputStream;
import android.view.Display;
import android.view.DisplayInfo;
import android.view.MotionEvent;
import android.view.SurfaceControl;
import android.view.WindowManager;
import android.window.WindowContainerToken;
import com.android.internal.app.ResolverActivity;
import com.android.internal.os.BackgroundThread;
import com.android.internal.protolog.ProtoLogGroup;
import com.android.internal.protolog.ProtoLogImpl;
import com.android.internal.util.ToBooleanFunction;
import com.android.internal.util.function.QuadConsumer;
import com.android.internal.util.function.QuintPredicate;
import com.android.internal.util.function.TriConsumer;
import com.android.internal.util.function.pooled.PooledConsumer;
import com.android.internal.util.function.pooled.PooledLambda;
import com.android.internal.util.function.pooled.PooledPredicate;
import com.android.server.LocalServices;
import com.android.server.UiModeManagerService;
import com.android.server.am.AppTimeTracker;
import com.android.server.am.UserState;
import com.android.server.policy.PermissionPolicyInternal;
import com.android.server.policy.PhoneWindowManager;
import com.android.server.slice.SliceClientPermissions;
import com.android.server.wm.ActivityRecord;
import com.android.server.wm.ActivityTaskManagerInternal;
import com.android.server.wm.ActivityTaskManagerService;
import com.android.server.wm.LaunchParamsController;
import com.android.server.wm.RootWindowContainer;
import com.android.server.wm.Task;
import com.transsion.hubcore.multiwindow.ITranMultiWindow;
import com.transsion.hubcore.server.wm.ITranRootWindowContainer;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class RootWindowContainer extends WindowContainer<DisplayContent> implements DisplayManager.DisplayListener {
    private static final String DISPLAY_OFF_SLEEP_TOKEN_TAG = "Display-off";
    static final int MATCH_ATTACHED_TASK_ONLY = 0;
    static final int MATCH_ATTACHED_TASK_OR_RECENT_TASKS = 1;
    static final int MATCH_ATTACHED_TASK_OR_RECENT_TASKS_AND_RESTORE = 2;
    private static final int SET_SCREEN_BRIGHTNESS_OVERRIDE = 1;
    private static final int SET_USER_ACTIVITY_TIMEOUT = 2;
    private static final String TAG = "WindowManager";
    public static final String TAG_MULTI = "TranMultiWindow";
    private ActivityRecord hookToOSRecord;
    private final AttachApplicationHelper mAttachApplicationHelper;
    private final Consumer<WindowState> mCloseSystemDialogsConsumer;
    private String mCloseSystemDialogsReason;
    int mCurrentUser;
    private DisplayContent mDefaultDisplay;
    int mDefaultMinSizeOfResizeableTaskDp;
    private String mDestroyAllActivitiesReason;
    private final Runnable mDestroyAllActivitiesRunnable;
    private final SparseArray<IntArray> mDisplayAccessUIDs;
    DisplayManager mDisplayManager;
    private DisplayManagerInternal mDisplayManagerInternal;
    final ActivityTaskManagerInternal.SleepTokenAcquirer mDisplayOffTokenAcquirer;
    private final SurfaceControl.Transaction mDisplayTransaction;
    FinishDisabledPackageActivitiesHelper mFinishDisabledPackageActivitiesHelper;
    private int mFixRotation;
    private SurfaceControl.Transaction mFixRotationTransaction;
    private final Handler mHandler;
    private Session mHoldScreen;
    WindowState mHoldScreenWindow;
    private boolean mIsValidActivityHere;
    private Object mLastWindowFreezeSource;
    private final String mMultiDragAndZoomBackground;
    private final String mMultiWeltWindowTag;
    private boolean mMultiWindowAnimationNeeded;
    private List<String> mMultiWindowBlackList;
    private List<String> mMultiWindowWhiteList;
    private Handler mMutliWindowHandler;
    private boolean mObscureApplicationContentOnSecondaryDisplays;
    WindowState mObscuringWindow;
    boolean mOrientationChangeComplete;
    private TaskDisplayArea mPreferMultiTaskDisplayArea;
    private final RankTaskLayersRunnable mRankTaskLayersRunnable;
    private float mScreenBrightnessOverride;
    ActivityTaskManagerService mService;
    final SparseArray<SleepToken> mSleepTokens;
    boolean mStartActivityInDefaultTaskDisplayArea;
    boolean mStartActivityInMultiTaskDisplayArea;
    private boolean mSustainedPerformanceModeCurrent;
    private boolean mSustainedPerformanceModeEnabled;
    private boolean mTaskLayersChanged;
    ActivityTaskSupervisor mTaskSupervisor;
    private TaskDisplayArea mTmpDisplayArea;
    private final FindTaskResult mTmpFindTaskResult;
    private int mTmpTaskLayerRank;
    final ArrayMap<Integer, ActivityRecord> mTopFocusedAppByProcess;
    private int mTopFocusedDisplayId;
    private boolean mUpdateRotation;
    private long mUserActivityTimeout;
    SparseIntArray mUserRootTaskInFront;
    boolean mWallpaperActionPending;
    WindowManagerService mWindowManager;
    private Runnable runable;
    static final String TAG_TASKS = "WindowManager" + ActivityTaskManagerDebugConfig.POSTFIX_TASKS;
    static final String TAG_STATES = "WindowManager" + ActivityTaskManagerDebugConfig.POSTFIX_STATES;
    private static final String TAG_RECENTS = "WindowManager" + ActivityTaskManagerDebugConfig.POSTFIX_RECENTS;
    private static final Consumer<WindowState> sRemoveReplacedWindowsConsumer = new Consumer() { // from class: com.android.server.wm.RootWindowContainer$$ExternalSyntheticLambda4
        @Override // java.util.function.Consumer
        public final void accept(Object obj) {
            RootWindowContainer.lambda$static$1((WindowState) obj);
        }
    };

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes2.dex */
    public @interface AnyTaskForIdMatchTaskMode {
    }

    /* renamed from: com.android.server.wm.RootWindowContainer$1  reason: invalid class name */
    /* loaded from: classes2.dex */
    class AnonymousClass1 implements Runnable {
        AnonymousClass1() {
        }

        @Override // java.lang.Runnable
        public void run() {
            synchronized (RootWindowContainer.this.mService.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    RootWindowContainer.this.mTaskSupervisor.beginDeferResume();
                    Consumer<ActivityRecord> obtainConsumer = PooledLambda.obtainConsumer(new BiConsumer() { // from class: com.android.server.wm.RootWindowContainer$1$$ExternalSyntheticLambda0
                        @Override // java.util.function.BiConsumer
                        public final void accept(Object obj, Object obj2) {
                            ((RootWindowContainer) obj).destroyActivity((ActivityRecord) obj2);
                        }
                    }, RootWindowContainer.this, PooledLambda.__(ActivityRecord.class));
                    RootWindowContainer.this.forAllActivities(obtainConsumer);
                    obtainConsumer.recycle();
                    RootWindowContainer.this.mTaskSupervisor.endDeferResume();
                    RootWindowContainer.this.resumeFocusedTasksTopActivities();
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public static class FindTaskResult implements Predicate<Task> {
        private ComponentName cls;
        private Uri documentData;
        private boolean isDocument;
        private int mActivityType;
        ActivityRecord mCandidateRecord;
        ActivityRecord mIdealRecord;
        private ActivityInfo mInfo;
        private Intent mIntent;
        private String mTaskAffinity;
        private int userId;

        FindTaskResult() {
        }

        void init(int activityType, String taskAffinity, Intent intent, ActivityInfo info) {
            this.mActivityType = activityType;
            this.mTaskAffinity = taskAffinity;
            this.mIntent = intent;
            this.mInfo = info;
            this.mIdealRecord = null;
            this.mCandidateRecord = null;
        }

        void process(WindowContainer parent) {
            this.cls = this.mIntent.getComponent();
            if (this.mInfo.targetActivity != null) {
                this.cls = new ComponentName(this.mInfo.packageName, this.mInfo.targetActivity);
            }
            this.userId = UserHandle.getUserId(this.mInfo.applicationInfo.uid);
            Intent intent = this.mIntent;
            boolean isDocument = intent.isDocument() & (intent != null);
            this.isDocument = isDocument;
            this.documentData = isDocument ? this.mIntent.getData() : null;
            if (ProtoLogCache.WM_DEBUG_TASKS_enabled) {
                String protoLogParam0 = String.valueOf(this.mInfo);
                String protoLogParam1 = String.valueOf(parent);
                ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_TASKS, -814760297, 0, (String) null, new Object[]{protoLogParam0, protoLogParam1});
            }
            parent.forAllLeafTasks(this);
        }

        /* JADX DEBUG: Method merged with bridge method */
        @Override // java.util.function.Predicate
        public boolean test(Task task) {
            if (!ConfigurationContainer.isCompatibleActivityType(this.mActivityType, task.getActivityType())) {
                if (ProtoLogCache.WM_DEBUG_TASKS_enabled) {
                    String protoLogParam0 = String.valueOf(task);
                    ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_TASKS, -373110070, 0, (String) null, new Object[]{protoLogParam0});
                }
                return false;
            } else if (task.voiceSession != null) {
                if (ProtoLogCache.WM_DEBUG_TASKS_enabled) {
                    String protoLogParam02 = String.valueOf(task);
                    ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_TASKS, 51927339, 0, (String) null, new Object[]{protoLogParam02});
                }
                return false;
            } else if (task.mUserId != this.userId) {
                if (ProtoLogCache.WM_DEBUG_TASKS_enabled) {
                    String protoLogParam03 = String.valueOf(task);
                    ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_TASKS, -399343789, 0, (String) null, new Object[]{protoLogParam03});
                }
                return false;
            } else if (matchingCandidate(task)) {
                return true;
            } else {
                return !task.isLeafTaskFragment() && task.forAllLeafTaskFragments(new Predicate() { // from class: com.android.server.wm.RootWindowContainer$FindTaskResult$$ExternalSyntheticLambda0
                    @Override // java.util.function.Predicate
                    public final boolean test(Object obj) {
                        return RootWindowContainer.FindTaskResult.this.matchingCandidate((TaskFragment) obj);
                    }
                });
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public boolean matchingCandidate(TaskFragment taskFragment) {
            boolean taskIsDocument;
            Uri taskDocumentData;
            boolean z;
            boolean z2;
            Task task = taskFragment.asTask();
            if (task == null) {
                return false;
            }
            ActivityRecord r = task.getTopNonFinishingActivity(false, false);
            if (r != null && !r.finishing && r.mUserId == this.userId && r.launchMode != 3) {
                if (!ConfigurationContainer.isCompatibleActivityType(r.getActivityType(), this.mActivityType)) {
                    if (ProtoLogCache.WM_DEBUG_TASKS_enabled) {
                        String protoLogParam0 = String.valueOf(task);
                        ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_TASKS, -1022146708, 0, (String) null, new Object[]{protoLogParam0});
                    }
                    return false;
                }
                Intent taskIntent = task.intent;
                Intent affinityIntent = task.affinityIntent;
                if (taskIntent != null && taskIntent.isDocument()) {
                    taskIsDocument = true;
                    taskDocumentData = taskIntent.getData();
                } else if (affinityIntent != null && affinityIntent.isDocument()) {
                    taskIsDocument = true;
                    taskDocumentData = affinityIntent.getData();
                } else {
                    taskIsDocument = false;
                    taskDocumentData = null;
                }
                if (ProtoLogCache.WM_DEBUG_TASKS_enabled) {
                    String protoLogParam02 = String.valueOf(r.getTask().rootAffinity);
                    String protoLogParam1 = String.valueOf(this.mIntent.getComponent().flattenToShortString());
                    String protoLogParam2 = String.valueOf(this.mInfo.taskAffinity);
                    String protoLogParam3 = String.valueOf(task.realActivity != null ? task.realActivity.flattenToShortString() : "");
                    ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_TASKS, 1192413464, 0, (String) null, new Object[]{protoLogParam02, protoLogParam1, protoLogParam2, protoLogParam3});
                }
                if (task.realActivity != null && task.realActivity.compareTo(this.cls) == 0 && Objects.equals(this.documentData, taskDocumentData)) {
                    if (ProtoLogCache.WM_DEBUG_TASKS_enabled) {
                        ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_TASKS, 1947936538, 0, (String) null, (Object[]) null);
                    }
                    if (ProtoLogCache.WM_DEBUG_TASKS_enabled) {
                        String protoLogParam03 = String.valueOf(this.mIntent);
                        String protoLogParam12 = String.valueOf(r.intent);
                        z2 = true;
                        ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_TASKS, 1557732761, 0, (String) null, new Object[]{protoLogParam03, protoLogParam12});
                    } else {
                        z2 = true;
                    }
                    this.mIdealRecord = r;
                    return z2;
                } else if (affinityIntent != null && affinityIntent.getComponent() != null && affinityIntent.getComponent().compareTo(this.cls) == 0 && Objects.equals(this.documentData, taskDocumentData)) {
                    if (ProtoLogCache.WM_DEBUG_TASKS_enabled) {
                        ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_TASKS, 1947936538, 0, (String) null, (Object[]) null);
                    }
                    if (ProtoLogCache.WM_DEBUG_TASKS_enabled) {
                        String protoLogParam04 = String.valueOf(this.mIntent);
                        String protoLogParam13 = String.valueOf(r.intent);
                        z = true;
                        ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_TASKS, 1557732761, 0, (String) null, new Object[]{protoLogParam04, protoLogParam13});
                    } else {
                        z = true;
                    }
                    this.mIdealRecord = r;
                    return z;
                } else {
                    if (!this.isDocument && !taskIsDocument && this.mIdealRecord == null && this.mCandidateRecord == null && task.rootAffinity != null) {
                        if (task.rootAffinity.equals(this.mTaskAffinity)) {
                            if (ProtoLogCache.WM_DEBUG_TASKS_enabled) {
                                ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_TASKS, 2039056415, 0, (String) null, (Object[]) null);
                            }
                            this.mCandidateRecord = r;
                        }
                    } else if (ProtoLogCache.WM_DEBUG_TASKS_enabled) {
                        String protoLogParam05 = String.valueOf(task);
                        ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_TASKS, -775004869, 0, (String) null, new Object[]{protoLogParam05});
                    }
                    return false;
                }
            }
            if (ProtoLogCache.WM_DEBUG_TASKS_enabled) {
                String protoLogParam06 = String.valueOf(task);
                String protoLogParam14 = String.valueOf(r);
                ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_TASKS, -1575977269, 0, (String) null, new Object[]{protoLogParam06, protoLogParam14});
            }
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$new$0$com-android-server-wm-RootWindowContainer  reason: not valid java name */
    public /* synthetic */ void m8203lambda$new$0$comandroidserverwmRootWindowContainer(WindowState w) {
        if (w.mHasSurface) {
            try {
                w.mClient.closeSystemDialogs(this.mCloseSystemDialogsReason);
            } catch (RemoteException e) {
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$static$1(WindowState w) {
        ActivityRecord activity = w.mActivityRecord;
        if (activity != null) {
            activity.removeReplacedWindowIfNeeded(w);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public RootWindowContainer(WindowManagerService service) {
        super(service);
        this.mLastWindowFreezeSource = null;
        this.mHoldScreen = null;
        this.mScreenBrightnessOverride = Float.NaN;
        this.mUserActivityTimeout = -1L;
        this.mUpdateRotation = false;
        this.mHoldScreenWindow = null;
        this.mObscuringWindow = null;
        this.mObscureApplicationContentOnSecondaryDisplays = false;
        this.mSustainedPerformanceModeEnabled = false;
        this.mSustainedPerformanceModeCurrent = false;
        this.mOrientationChangeComplete = true;
        this.mWallpaperActionPending = false;
        this.mTopFocusedDisplayId = -1;
        this.mTopFocusedAppByProcess = new ArrayMap<>();
        this.mDisplayAccessUIDs = new SparseArray<>();
        this.mUserRootTaskInFront = new SparseIntArray(2);
        this.mSleepTokens = new SparseArray<>();
        this.mDefaultMinSizeOfResizeableTaskDp = -1;
        this.mTaskLayersChanged = true;
        this.mRankTaskLayersRunnable = new RankTaskLayersRunnable();
        this.mAttachApplicationHelper = new AttachApplicationHelper();
        this.mDestroyAllActivitiesRunnable = new AnonymousClass1();
        this.mTmpFindTaskResult = new FindTaskResult();
        this.mCloseSystemDialogsConsumer = new Consumer() { // from class: com.android.server.wm.RootWindowContainer$$ExternalSyntheticLambda52
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                RootWindowContainer.this.m8203lambda$new$0$comandroidserverwmRootWindowContainer((WindowState) obj);
            }
        };
        this.mFinishDisabledPackageActivitiesHelper = new FinishDisabledPackageActivitiesHelper();
        this.mMultiWindowBlackList = new ArrayList();
        this.mMultiWindowWhiteList = new ArrayList();
        this.mMultiWindowAnimationNeeded = false;
        this.mMultiWeltWindowTag = "Tran_Welt_Window_@6a859d9";
        this.mMultiDragAndZoomBackground = "DragAndZoomBackground";
        this.mMutliWindowHandler = BackgroundThread.getHandler();
        this.runable = new Runnable() { // from class: com.android.server.wm.RootWindowContainer.2
            @Override // java.lang.Runnable
            public void run() {
            }
        };
        this.mDisplayTransaction = service.mTransactionFactory.get();
        this.mHandler = new MyHandler(service.mH.getLooper());
        ActivityTaskManagerService activityTaskManagerService = service.mAtmService;
        this.mService = activityTaskManagerService;
        ActivityTaskSupervisor activityTaskSupervisor = activityTaskManagerService.mTaskSupervisor;
        this.mTaskSupervisor = activityTaskSupervisor;
        activityTaskSupervisor.mRootWindowContainer = this;
        ActivityTaskManagerService activityTaskManagerService2 = this.mService;
        Objects.requireNonNull(activityTaskManagerService2);
        this.mDisplayOffTokenAcquirer = new ActivityTaskManagerService.SleepTokenAcquirerImpl(DISPLAY_OFF_SLEEP_TOKEN_TAG);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean updateFocusedWindowLocked(int mode, boolean updateInputWindows) {
        this.mTopFocusedAppByProcess.clear();
        boolean changed = false;
        int topFocusedDisplayId = -1;
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            DisplayContent dc = (DisplayContent) this.mChildren.get(i);
            changed |= dc.updateFocusedWindowLocked(mode, updateInputWindows, topFocusedDisplayId);
            WindowState newFocus = dc.mCurrentFocus;
            if (newFocus != null) {
                int pidOfNewFocus = newFocus.mSession.mPid;
                if (this.mTopFocusedAppByProcess.get(Integer.valueOf(pidOfNewFocus)) == null) {
                    this.mTopFocusedAppByProcess.put(Integer.valueOf(pidOfNewFocus), newFocus.mActivityRecord);
                }
                if (topFocusedDisplayId == -1) {
                    topFocusedDisplayId = dc.getDisplayId();
                }
            } else if (topFocusedDisplayId == -1 && dc.mFocusedApp != null) {
                topFocusedDisplayId = dc.getDisplayId();
            }
        }
        if (topFocusedDisplayId == -1) {
            topFocusedDisplayId = 0;
        }
        if (this.mTopFocusedDisplayId != topFocusedDisplayId) {
            this.mTopFocusedDisplayId = topFocusedDisplayId;
            this.mWmService.mInputManager.setFocusedDisplay(topFocusedDisplayId);
            this.mWmService.mPolicy.setTopFocusedDisplay(topFocusedDisplayId);
            this.mWmService.mAccessibilityController.setFocusedDisplay(topFocusedDisplayId);
            if (ProtoLogCache.WM_DEBUG_FOCUS_LIGHT_enabled) {
                long protoLogParam0 = topFocusedDisplayId;
                ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_FOCUS_LIGHT, 312030608, 1, (String) null, new Object[]{Long.valueOf(protoLogParam0)});
            }
        }
        return changed;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public DisplayContent getTopFocusedDisplayContent() {
        DisplayContent dc = getDisplayContent(this.mTopFocusedDisplayId);
        return dc != null ? dc : getDisplayContent(0);
    }

    @Override // com.android.server.wm.WindowContainer
    boolean isOnTop() {
        return true;
    }

    @Override // com.android.server.wm.WindowContainer
    void onChildPositionChanged(WindowContainer child) {
        this.mWmService.updateFocusedWindowLocked(0, !this.mWmService.mPerDisplayFocusEnabled);
        this.mTaskSupervisor.updateTopResumedActivityIfNeeded();
    }

    @Override // com.android.server.wm.WindowContainer
    boolean isAttached() {
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onSettingsRetrieved() {
        int numDisplays = this.mChildren.size();
        for (int displayNdx = 0; displayNdx < numDisplays; displayNdx++) {
            DisplayContent displayContent = (DisplayContent) this.mChildren.get(displayNdx);
            boolean changed = this.mWmService.mDisplayWindowSettings.updateSettingsForDisplay(displayContent);
            if (changed) {
                displayContent.reconfigureDisplayLocked();
                if (displayContent.isDefaultDisplay) {
                    Configuration newConfig = this.mWmService.computeNewConfiguration(displayContent.getDisplayId());
                    this.mWmService.mAtmService.updateConfigurationLocked(newConfig, null, false);
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isLayoutNeeded() {
        int numDisplays = this.mChildren.size();
        for (int displayNdx = 0; displayNdx < numDisplays; displayNdx++) {
            DisplayContent displayContent = (DisplayContent) this.mChildren.get(displayNdx);
            if (displayContent.isLayoutNeeded()) {
                return true;
            }
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void getWindowsByName(ArrayList<WindowState> output, String name) {
        int objectId = 0;
        try {
            objectId = Integer.parseInt(name, 16);
            name = null;
        } catch (RuntimeException e) {
        }
        getWindowsByName(output, name, objectId);
    }

    private void getWindowsByName(final ArrayList<WindowState> output, final String name, final int objectId) {
        forAllWindows(new Consumer() { // from class: com.android.server.wm.RootWindowContainer$$ExternalSyntheticLambda53
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                RootWindowContainer.lambda$getWindowsByName$2(name, output, objectId, (WindowState) obj);
            }
        }, true);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$getWindowsByName$2(String name, ArrayList output, int objectId, WindowState w) {
        if (name != null) {
            if (w.mAttrs.getTitle().toString().contains(name)) {
                output.add(w);
            }
        } else if (System.identityHashCode(w) == objectId) {
            output.add(w);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActivityRecord getActivityRecord(IBinder binder) {
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            DisplayContent dc = (DisplayContent) this.mChildren.get(i);
            ActivityRecord activity = dc.getActivityRecord(binder);
            if (activity != null) {
                return activity;
            }
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public WindowToken getWindowToken(IBinder binder) {
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            DisplayContent dc = (DisplayContent) this.mChildren.get(i);
            WindowToken wtoken = dc.getWindowToken(binder);
            if (wtoken != null) {
                return wtoken;
            }
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public DisplayContent getWindowTokenDisplay(WindowToken token) {
        if (token == null) {
            return null;
        }
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            DisplayContent dc = (DisplayContent) this.mChildren.get(i);
            WindowToken current = dc.getWindowToken(token.token);
            if (current == token) {
                return dc;
            }
        }
        return null;
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.wm.ConfigurationContainer
    public void dispatchConfigurationToChild(DisplayContent child, Configuration config) {
        if (child.isDefaultDisplay) {
            child.performDisplayOverrideConfigUpdate(config);
        } else {
            child.onConfigurationChanged(config);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void refreshSecureSurfaceState() {
        forAllWindows(new Consumer() { // from class: com.android.server.wm.RootWindowContainer$$ExternalSyntheticLambda41
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                RootWindowContainer.lambda$refreshSecureSurfaceState$3((WindowState) obj);
            }
        }, true);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$refreshSecureSurfaceState$3(WindowState w) {
        if (w.mHasSurface) {
            w.mWinAnimator.setSecureLocked(w.isSecureLocked());
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateHiddenWhileSuspendedState(final ArraySet<String> packages, final boolean suspended) {
        forAllWindows(new Consumer() { // from class: com.android.server.wm.RootWindowContainer$$ExternalSyntheticLambda47
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                RootWindowContainer.lambda$updateHiddenWhileSuspendedState$4(packages, suspended, (WindowState) obj);
            }
        }, false);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$updateHiddenWhileSuspendedState$4(ArraySet packages, boolean suspended, WindowState w) {
        if (packages.contains(w.getOwningPackage())) {
            w.setHiddenWhileSuspended(suspended);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateAppOpsState() {
        forAllWindows(new Consumer() { // from class: com.android.server.wm.RootWindowContainer$$ExternalSyntheticLambda60
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ((WindowState) obj).updateAppOpsState();
            }
        }, false);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$canShowStrictModeViolation$6(int pid, WindowState w) {
        return w.mSession.mPid == pid && w.isVisible();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean canShowStrictModeViolation(final int pid) {
        WindowState win = getWindow(new Predicate() { // from class: com.android.server.wm.RootWindowContainer$$ExternalSyntheticLambda48
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return RootWindowContainer.lambda$canShowStrictModeViolation$6(pid, (WindowState) obj);
            }
        });
        return win != null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void closeSystemDialogs(String reason) {
        this.mCloseSystemDialogsReason = reason;
        forAllWindows(this.mCloseSystemDialogsConsumer, false);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: Type inference failed for: r3v0, types: [java.lang.Object[], java.lang.String] */
    public void removeReplacedWindows() {
        if (ProtoLogCache.WM_SHOW_TRANSACTIONS_enabled) {
            ProtoLogImpl.i(ProtoLogGroup.WM_SHOW_TRANSACTIONS, -1515151503, 0, (String) null, (Object[]) null);
        }
        this.mWmService.openSurfaceTransaction();
        try {
            forAllWindows(sRemoveReplacedWindowsConsumer, true);
        } finally {
            this.mWmService.closeSurfaceTransaction("removeReplacedWindows");
            if (ProtoLogCache.WM_SHOW_TRANSACTIONS_enabled) {
                Object[] objArr = null;
                ProtoLogImpl.i(ProtoLogGroup.WM_SHOW_TRANSACTIONS, 1423592961, 0, (String) null, (Object[]) null);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasPendingLayoutChanges(WindowAnimator animator) {
        boolean hasChanges = false;
        int count = this.mChildren.size();
        for (int i = 0; i < count; i++) {
            int pendingChanges = ((DisplayContent) this.mChildren.get(i)).pendingLayoutChanges;
            if ((pendingChanges & 4) != 0) {
                animator.mBulkUpdateParams |= 2;
            }
            if (pendingChanges != 0) {
                hasChanges = true;
            }
        }
        return hasChanges;
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [816=4] */
    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean reclaimSomeSurfaceMemory(WindowStateAnimator winAnimator, String operation, boolean secure) {
        boolean z;
        WindowSurfaceController surfaceController = winAnimator.mSurfaceController;
        boolean leakedSurface = false;
        boolean killedApps = false;
        EventLogTags.writeWmNoSurfaceMemory(winAnimator.mWin.toString(), winAnimator.mSession.mPid, operation);
        long callingIdentity = Binder.clearCallingIdentity();
        try {
            Slog.i("WindowManager", "Out of memory for surface!  Looking for leaks...");
            int numDisplays = this.mChildren.size();
            for (int displayNdx = 0; displayNdx < numDisplays; displayNdx++) {
                leakedSurface |= ((DisplayContent) this.mChildren.get(displayNdx)).destroyLeakedSurfaces();
            }
            boolean z2 = false;
            if (!leakedSurface) {
                Slog.w("WindowManager", "No leaked surfaces; killing applications!");
                final SparseIntArray pidCandidates = new SparseIntArray();
                boolean killedApps2 = false;
                int displayNdx2 = 0;
                while (displayNdx2 < numDisplays) {
                    try {
                        ((DisplayContent) this.mChildren.get(displayNdx2)).forAllWindows(new Consumer() { // from class: com.android.server.wm.RootWindowContainer$$ExternalSyntheticLambda31
                            @Override // java.util.function.Consumer
                            public final void accept(Object obj) {
                                RootWindowContainer.this.m8206x3661fa35(pidCandidates, (WindowState) obj);
                            }
                        }, z2);
                        if (pidCandidates.size() > 0) {
                            int[] pids = new int[pidCandidates.size()];
                            for (int i = 0; i < pids.length; i++) {
                                pids[i] = pidCandidates.keyAt(i);
                            }
                            try {
                                try {
                                    if (this.mWmService.mActivityManager.killPids(pids, "Free memory", secure)) {
                                        killedApps2 = true;
                                    }
                                } catch (RemoteException e) {
                                } catch (Throwable th) {
                                    th = th;
                                    Binder.restoreCallingIdentity(callingIdentity);
                                    throw th;
                                }
                            } catch (RemoteException e2) {
                            }
                        }
                        displayNdx2++;
                        z2 = false;
                    } catch (Throwable th2) {
                        th = th2;
                    }
                }
                killedApps = killedApps2;
            }
            if (leakedSurface || killedApps) {
                try {
                    Slog.w("WindowManager", "Looks like we have reclaimed some memory, clearing surface for retry.");
                    if (surfaceController != null) {
                        if (ProtoLogCache.WM_SHOW_SURFACE_ALLOC_enabled) {
                            String protoLogParam0 = String.valueOf(winAnimator.mWin);
                            z = false;
                            ProtoLogImpl.i(ProtoLogGroup.WM_SHOW_SURFACE_ALLOC, 399841913, 0, (String) null, new Object[]{protoLogParam0});
                        } else {
                            z = false;
                        }
                        SurfaceControl.Transaction t = this.mWmService.mTransactionFactory.get();
                        winAnimator.destroySurface(t);
                        t.apply();
                        if (winAnimator.mWin.mActivityRecord != null) {
                            winAnimator.mWin.mActivityRecord.removeStartingWindow();
                        }
                    } else {
                        z = false;
                    }
                    try {
                        winAnimator.mWin.mClient.dispatchGetNewSurface();
                    } catch (RemoteException e3) {
                    }
                } catch (Throwable th3) {
                    th = th3;
                    Binder.restoreCallingIdentity(callingIdentity);
                    throw th;
                }
            } else {
                z = false;
            }
            Binder.restoreCallingIdentity(callingIdentity);
            if (leakedSurface || killedApps) {
                return true;
            }
            return z;
        } catch (Throwable th4) {
            th = th4;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$reclaimSomeSurfaceMemory$7$com-android-server-wm-RootWindowContainer  reason: not valid java name */
    public /* synthetic */ void m8206x3661fa35(SparseIntArray pidCandidates, WindowState w) {
        if (this.mWmService.mForceRemoves.contains(w)) {
            return;
        }
        WindowStateAnimator wsa = w.mWinAnimator;
        if (wsa.mSurfaceController != null) {
            pidCandidates.append(wsa.mSession.mPid, wsa.mSession.mPid);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void performSurfacePlacement() {
        Trace.traceBegin(32L, "performSurfacePlacement");
        try {
            performSurfacePlacementNoTrace();
        } finally {
            Trace.traceEnd(32L);
        }
    }

    private void checkTime(long startTime, String where) {
        long now = SystemClock.uptimeMillis();
        if (now - startTime > 60) {
            Slog.d("WindowManager", "System monitor slow operation: " + (now - startTime) + "ms so far, now at " + where);
        }
    }

    /* JADX WARN: Code restructure failed: missing block: B:100:0x023a, code lost:
        if (com.android.server.wm.ProtoLogCache.WM_DEBUG_ORIENTATION_enabled == false) goto L101;
     */
    /* JADX WARN: Code restructure failed: missing block: B:101:0x023c, code lost:
        com.android.internal.protolog.ProtoLogImpl.d(com.android.internal.protolog.ProtoLogGroup.WM_DEBUG_ORIENTATION, -1103115659, 0, (java.lang.String) null, (java.lang.Object[]) null);
     */
    /* JADX WARN: Code restructure failed: missing block: B:102:0x0247, code lost:
        r18.mUpdateRotation = updateRotationUnchecked();
     */
    /* JADX WARN: Code restructure failed: missing block: B:104:0x0255, code lost:
        if (r18.mWmService.mWaitingForDrawnCallbacks.isEmpty() == false) goto L116;
     */
    /* JADX WARN: Code restructure failed: missing block: B:106:0x0259, code lost:
        if (r18.mOrientationChangeComplete == false) goto L110;
     */
    /* JADX WARN: Code restructure failed: missing block: B:108:0x025f, code lost:
        if (isLayoutNeeded() != false) goto L110;
     */
    /* JADX WARN: Code restructure failed: missing block: B:110:0x0263, code lost:
        if (r18.mUpdateRotation != false) goto L110;
     */
    /* JADX WARN: Code restructure failed: missing block: B:111:0x0265, code lost:
        r18.mWmService.checkDrawnWindowsLocked();
     */
    /* JADX WARN: Code restructure failed: missing block: B:112:0x026a, code lost:
        forAllDisplays(new com.android.server.wm.RootWindowContainer$$ExternalSyntheticLambda59());
        r18.mWmService.enableScreenIfNeededLocked();
        r18.mWmService.scheduleAnimationLocked();
     */
    /* JADX WARN: Code restructure failed: missing block: B:113:0x027e, code lost:
        if (com.android.server.wm.WindowManagerDebugConfig.DEBUG_WINDOW_TRACE == false) goto L115;
     */
    /* JADX WARN: Code restructure failed: missing block: B:114:0x0280, code lost:
        android.util.Slog.e("WindowManager", "performSurfacePlacementInner exit");
     */
    /* JADX WARN: Code restructure failed: missing block: B:115:0x0286, code lost:
        return;
     */
    /* JADX WARN: Code restructure failed: missing block: B:131:?, code lost:
        return;
     */
    /* JADX WARN: Code restructure failed: missing block: B:25:0x00a4, code lost:
        if (com.android.server.wm.WindowManagerDebugConfig.SHOW_LIGHT_TRANSACTIONS == false) goto L19;
     */
    /* JADX WARN: Code restructure failed: missing block: B:28:0x00a9, code lost:
        if (android.os.Build.IS_DEBUG_ENABLE == false) goto L22;
     */
    /* JADX WARN: Code restructure failed: missing block: B:29:0x00ab, code lost:
        checkTime(r12, "CLOSE TRANSACTION performLayoutAndPlaceSurfaces");
     */
    /* JADX WARN: Code restructure failed: missing block: B:30:0x00b0, code lost:
        com.android.server.wm.IWindowManagerServiceLice.Instance().onApplySurfaceTransaction();
        r18.mWmService.mAtmService.mTaskOrganizerController.dispatchPendingEvents();
        r18.mWmService.mAtmService.mTaskFragmentOrganizerController.dispatchPendingEvents();
        r18.mWmService.mSyncEngine.onSurfacePlacement();
        r18.mWmService.mAnimator.executeAfterPrepareSurfacesRunnables();
        checkAppTransitionReady(r11);
        r0 = r18.mWmService.getRecentsAnimationController();
     */
    /* JADX WARN: Code restructure failed: missing block: B:31:0x00e0, code lost:
        if (r0 == null) goto L25;
     */
    /* JADX WARN: Code restructure failed: missing block: B:32:0x00e2, code lost:
        r0.checkAnimationReady(r9.mWallpaperController);
     */
    /* JADX WARN: Code restructure failed: missing block: B:33:0x00e7, code lost:
        r2 = 0;
     */
    /* JADX WARN: Code restructure failed: missing block: B:35:0x00ee, code lost:
        if (r2 >= r18.mChildren.size()) goto L40;
     */
    /* JADX WARN: Code restructure failed: missing block: B:36:0x00f0, code lost:
        r3 = (com.android.server.wm.DisplayContent) r18.mChildren.get(r2);
     */
    /* JADX WARN: Code restructure failed: missing block: B:37:0x00fa, code lost:
        if (r3.mWallpaperMayChange == false) goto L39;
     */
    /* JADX WARN: Code restructure failed: missing block: B:39:0x00fe, code lost:
        if (com.android.server.wm.ProtoLogCache.WM_DEBUG_WALLPAPER_enabled == false) goto L33;
     */
    /* JADX WARN: Code restructure failed: missing block: B:40:0x0100, code lost:
        com.android.internal.protolog.ProtoLogImpl.v(com.android.internal.protolog.ProtoLogGroup.WM_DEBUG_WALLPAPER, 535103992, 0, (java.lang.String) null, (java.lang.Object[]) null);
     */
    /* JADX WARN: Code restructure failed: missing block: B:41:0x010b, code lost:
        r3.pendingLayoutChanges |= 4;
     */
    /* JADX WARN: Code restructure failed: missing block: B:42:0x0113, code lost:
        if (com.android.server.wm.WindowManagerDebugConfig.DEBUG_LAYOUT_REPEATS == false) goto L38;
     */
    /* JADX WARN: Code restructure failed: missing block: B:43:0x0115, code lost:
        r11.debugLayoutRepeats("WallpaperMayChange", r3.pendingLayoutChanges);
     */
    /* JADX WARN: Code restructure failed: missing block: B:44:0x011c, code lost:
        r2 = r2 + 1;
     */
    /* JADX WARN: Code restructure failed: missing block: B:46:0x0124, code lost:
        if (r18.mWmService.mFocusMayChange == false) goto L44;
     */
    /* JADX WARN: Code restructure failed: missing block: B:47:0x0126, code lost:
        r18.mWmService.mFocusMayChange = false;
        r18.mWmService.updateFocusedWindowLocked(2, false);
     */
    /* JADX WARN: Code restructure failed: missing block: B:49:0x0133, code lost:
        if (isLayoutNeeded() == false) goto L49;
     */
    /* JADX WARN: Code restructure failed: missing block: B:50:0x0135, code lost:
        r9.pendingLayoutChanges |= 1;
     */
    /* JADX WARN: Code restructure failed: missing block: B:51:0x013c, code lost:
        if (com.android.server.wm.WindowManagerDebugConfig.DEBUG_LAYOUT_REPEATS == false) goto L49;
     */
    /* JADX WARN: Code restructure failed: missing block: B:52:0x013e, code lost:
        r11.debugLayoutRepeats("mLayoutNeeded", r9.pendingLayoutChanges);
     */
    /* JADX WARN: Code restructure failed: missing block: B:53:0x0146, code lost:
        handleResizingWindows();
     */
    /* JADX WARN: Code restructure failed: missing block: B:54:0x014d, code lost:
        if (r18.mWmService.mDisplayFrozen == false) goto L54;
     */
    /* JADX WARN: Code restructure failed: missing block: B:56:0x0151, code lost:
        if (com.android.server.wm.ProtoLogCache.WM_DEBUG_ORIENTATION_enabled == false) goto L54;
     */
    /* JADX WARN: Code restructure failed: missing block: B:57:0x0153, code lost:
        r2 = r18.mOrientationChangeComplete;
        com.android.internal.protolog.ProtoLogImpl.v(com.android.internal.protolog.ProtoLogGroup.WM_DEBUG_ORIENTATION, -666510420, 3, (java.lang.String) null, new java.lang.Object[]{java.lang.Boolean.valueOf(r2)});
     */
    /* JADX WARN: Code restructure failed: missing block: B:58:0x0165, code lost:
        r2 = r18.mOrientationChangeComplete;
     */
    /* JADX WARN: Code restructure failed: missing block: B:59:0x0167, code lost:
        if (r2 == false) goto L62;
     */
    /* JADX WARN: Code restructure failed: missing block: B:61:0x016f, code lost:
        if (com.android.server.wm.SplitScreenHelper.shouldBlockStopFreezingForSplitScreen(r18, r18.mWmService) != false) goto L62;
     */
    /* JADX WARN: Code restructure failed: missing block: B:63:0x0175, code lost:
        if (r18.mWmService.mWindowsFreezingScreen == 0) goto L61;
     */
    /* JADX WARN: Code restructure failed: missing block: B:64:0x0177, code lost:
        r18.mWmService.mWindowsFreezingScreen = 0;
        r18.mWmService.mLastFinishedFreezeSource = r18.mLastWindowFreezeSource;
        r18.mWmService.mH.removeMessages(11);
     */
    /* JADX WARN: Code restructure failed: missing block: B:65:0x018a, code lost:
        r18.mWmService.stopFreezingDisplayLocked();
     */
    /* JADX WARN: Code restructure failed: missing block: B:66:0x018f, code lost:
        r2 = r18.mWmService.mDestroySurface.size();
     */
    /* JADX WARN: Code restructure failed: missing block: B:67:0x0197, code lost:
        if (r2 <= 0) goto L74;
     */
    /* JADX WARN: Code restructure failed: missing block: B:68:0x0199, code lost:
        r2 = r2 - 1;
        r4 = r18.mWmService.mDestroySurface.get(r2);
        r4.mDestroying = false;
        r8 = r4.getDisplayContent();
     */
    /* JADX WARN: Code restructure failed: missing block: B:69:0x01ad, code lost:
        if (r8.mInputMethodWindow != r4) goto L67;
     */
    /* JADX WARN: Code restructure failed: missing block: B:70:0x01af, code lost:
        r8.setInputMethodWindowLocked(null);
     */
    /* JADX WARN: Code restructure failed: missing block: B:72:0x01b8, code lost:
        if (r8.mWallpaperController.isWallpaperTarget(r4) == false) goto L70;
     */
    /* JADX WARN: Code restructure failed: missing block: B:73:0x01ba, code lost:
        r8.pendingLayoutChanges |= 4;
     */
    /* JADX WARN: Code restructure failed: missing block: B:74:0x01c0, code lost:
        r4.destroySurfaceUnchecked();
     */
    /* JADX WARN: Code restructure failed: missing block: B:75:0x01c3, code lost:
        if (r2 > 0) goto L64;
     */
    /* JADX WARN: Code restructure failed: missing block: B:76:0x01c5, code lost:
        r18.mWmService.mDestroySurface.clear();
     */
    /* JADX WARN: Code restructure failed: missing block: B:77:0x01cc, code lost:
        r4 = 0;
     */
    /* JADX WARN: Code restructure failed: missing block: B:79:0x01d3, code lost:
        if (r4 >= r18.mChildren.size()) goto L83;
     */
    /* JADX WARN: Code restructure failed: missing block: B:80:0x01d5, code lost:
        r8 = (com.android.server.wm.DisplayContent) r18.mChildren.get(r4);
     */
    /* JADX WARN: Code restructure failed: missing block: B:81:0x01df, code lost:
        if (r8.pendingLayoutChanges == 0) goto L82;
     */
    /* JADX WARN: Code restructure failed: missing block: B:82:0x01e1, code lost:
        r8.setLayoutNeeded();
     */
    /* JADX WARN: Code restructure failed: missing block: B:83:0x01e4, code lost:
        r4 = r4 + 1;
     */
    /* JADX WARN: Code restructure failed: missing block: B:84:0x01e7, code lost:
        r18.mWmService.setHoldScreenLocked(r18.mHoldScreen);
     */
    /* JADX WARN: Code restructure failed: missing block: B:85:0x01f2, code lost:
        if (r18.mWmService.mDisplayFrozen != false) goto L93;
     */
    /* JADX WARN: Code restructure failed: missing block: B:86:0x01f4, code lost:
        r4 = r18.mScreenBrightnessOverride;
     */
    /* JADX WARN: Code restructure failed: missing block: B:87:0x01f9, code lost:
        if (r4 < 0.0f) goto L92;
     */
    /* JADX WARN: Code restructure failed: missing block: B:89:0x01ff, code lost:
        if (r4 <= 1.0f) goto L90;
     */
    /* JADX WARN: Code restructure failed: missing block: B:91:0x0202, code lost:
        r8 = r4;
     */
    /* JADX WARN: Code restructure failed: missing block: B:92:0x0204, code lost:
        r8 = Float.NaN;
     */
    /* JADX WARN: Code restructure failed: missing block: B:93:0x0206, code lost:
        r4 = r8;
        r8 = java.lang.Float.floatToIntBits(r4);
        r18.mHandler.obtainMessage(1, r8, 0).sendToTarget();
        r18.mHandler.obtainMessage(2, java.lang.Long.valueOf(r18.mUserActivityTimeout)).sendToTarget();
     */
    /* JADX WARN: Code restructure failed: missing block: B:94:0x0223, code lost:
        r4 = r18.mSustainedPerformanceModeCurrent;
     */
    /* JADX WARN: Code restructure failed: missing block: B:95:0x0227, code lost:
        if (r4 == r18.mSustainedPerformanceModeEnabled) goto L96;
     */
    /* JADX WARN: Code restructure failed: missing block: B:96:0x0229, code lost:
        r18.mSustainedPerformanceModeEnabled = r4;
        r18.mWmService.mPowerManagerInternal.setPowerMode(2, r18.mSustainedPerformanceModeEnabled);
     */
    /* JADX WARN: Code restructure failed: missing block: B:98:0x0236, code lost:
        if (r18.mUpdateRotation == false) goto L102;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    void performSurfacePlacementNoTrace() {
        if (WindowManagerDebugConfig.DEBUG_WINDOW_TRACE) {
            Slog.v("WindowManager", "performSurfacePlacementInner: entry. Called by " + Debug.getCallers(3));
        }
        if (this.mWmService.mFocusMayChange) {
            this.mWmService.mFocusMayChange = false;
            this.mWmService.updateFocusedWindowLocked(3, false);
        }
        this.mHoldScreen = null;
        this.mScreenBrightnessOverride = Float.NaN;
        this.mUserActivityTimeout = -1L;
        this.mObscureApplicationContentOnSecondaryDisplays = false;
        this.mSustainedPerformanceModeCurrent = false;
        this.mWmService.mTransactionSequence++;
        DisplayContent defaultDisplay = this.mWmService.getDefaultDisplayContentLocked();
        WindowSurfacePlacer surfacePlacer = this.mWmService.mWindowPlacerLocked;
        if (WindowManagerDebugConfig.SHOW_LIGHT_TRANSACTIONS) {
            Slog.i("WindowManager", ">>> OPEN TRANSACTION performLayoutAndPlaceSurfaces");
        }
        long startTime = 0;
        if (Build.IS_DEBUG_ENABLE) {
            startTime = SystemClock.uptimeMillis();
        }
        Trace.traceBegin(32L, "applySurfaceChanges");
        this.mWmService.openSurfaceTransaction();
        try {
            try {
                applySurfaceChangesTransaction();
            } catch (RuntimeException e) {
                Slog.wtf("WindowManager", "Unhandled exception in Window Manager", e);
                this.mWmService.closeSurfaceTransaction("performLayoutAndPlaceSurfaces");
                Trace.traceEnd(32L);
            }
        } finally {
            this.mWmService.closeSurfaceTransaction("performLayoutAndPlaceSurfaces");
            Trace.traceEnd(32L);
            if (WindowManagerDebugConfig.SHOW_LIGHT_TRANSACTIONS) {
                Slog.i("WindowManager", "<<< CLOSE TRANSACTION performLayoutAndPlaceSurfaces");
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$performSurfacePlacementNoTrace$8(DisplayContent dc) {
        dc.getInputMonitor().updateInputWindowsLw(true);
        dc.updateSystemGestureExclusion();
        dc.updateKeepClearAreas();
        dc.updateTouchExcludeRegion();
    }

    private void checkAppTransitionReady(WindowSurfacePlacer surfacePlacer) {
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            DisplayContent curDisplay = (DisplayContent) this.mChildren.get(i);
            if (curDisplay.mAppTransition.isReady()) {
                curDisplay.mAppTransitionController.handleAppTransitionReady();
                if (WindowManagerDebugConfig.DEBUG_LAYOUT_REPEATS) {
                    surfacePlacer.debugLayoutRepeats("after handleAppTransitionReady", curDisplay.pendingLayoutChanges);
                }
            }
            if (curDisplay.mAppTransition.isRunning() && !curDisplay.isAppTransitioning()) {
                curDisplay.handleAnimatingStoppedAndTransition();
                if (WindowManagerDebugConfig.DEBUG_LAYOUT_REPEATS) {
                    surfacePlacer.debugLayoutRepeats("after handleAnimStopAndXitionLock", curDisplay.pendingLayoutChanges);
                }
            }
        }
    }

    private void applySurfaceChangesTransaction() {
        this.mHoldScreenWindow = null;
        this.mObscuringWindow = null;
        DisplayContent defaultDc = this.mWmService.getDefaultDisplayContentLocked();
        DisplayInfo defaultInfo = defaultDc.getDisplayInfo();
        int defaultDw = defaultInfo.logicalWidth;
        int defaultDh = defaultInfo.logicalHeight;
        if (this.mWmService.mWatermark != null) {
            this.mWmService.mWatermark.positionSurface(defaultDw, defaultDh, this.mDisplayTransaction);
        }
        if (this.mWmService.tranWaterMark != null) {
            this.mWmService.tranWaterMark.positionSurface(defaultDw, defaultDh);
        }
        if (this.mWmService.mStrictModeFlash != null) {
            this.mWmService.mStrictModeFlash.positionSurface(defaultDw, defaultDh, this.mDisplayTransaction);
        }
        if (this.mWmService.mEmulatorDisplayOverlay != null) {
            this.mWmService.mEmulatorDisplayOverlay.positionSurface(defaultDw, defaultDh, this.mWmService.getDefaultDisplayRotation(), this.mDisplayTransaction);
        }
        int count = this.mChildren.size();
        for (int j = 0; j < count; j++) {
            DisplayContent dc = (DisplayContent) this.mChildren.get(j);
            dc.applySurfaceChangesTransaction();
        }
        this.mWmService.mDisplayManagerInternal.performTraversal(this.mDisplayTransaction);
        if (ThunderbackConfig.isVersion4()) {
            hookAddFixRotationTransaction(defaultInfo.rotation);
        }
        SurfaceControl.mergeToGlobalTransaction(this.mDisplayTransaction);
    }

    private void handleResizingWindows() {
        for (int i = this.mWmService.mResizingWindows.size() - 1; i >= 0; i--) {
            WindowState win = this.mWmService.mResizingWindows.get(i);
            if (!win.mAppFreezing && !win.getDisplayContent().mWaitingForConfig) {
                win.reportResized();
                this.mWmService.mResizingWindows.remove(i);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean handleNotObscuredLocked(WindowState w, boolean obscured, boolean syswin) {
        boolean displayHasContent;
        boolean displayHasContent2;
        WindowManager.LayoutParams attrs = w.mAttrs;
        int attrFlags = attrs.flags;
        boolean onScreen = w.isOnScreen();
        boolean canBeSeen = w.isDisplayed();
        int privateflags = attrs.privateFlags;
        if (ProtoLogCache.WM_DEBUG_KEEP_SCREEN_ON_enabled) {
            String protoLogParam0 = String.valueOf(w);
            boolean protoLogParam1 = w.mHasSurface;
            boolean protoLogParam3 = w.isDisplayed();
            long protoLogParam4 = w.mAttrs.userActivityTimeout;
            displayHasContent = false;
            ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_KEEP_SCREEN_ON, -481924678, 508, (String) null, new Object[]{protoLogParam0, Boolean.valueOf(protoLogParam1), Boolean.valueOf(onScreen), Boolean.valueOf(protoLogParam3), Long.valueOf(protoLogParam4)});
        } else {
            displayHasContent = false;
        }
        boolean displayHasContent3 = w.mHasSurface;
        if (displayHasContent3 && onScreen && !syswin && w.mAttrs.userActivityTimeout >= 0 && this.mUserActivityTimeout < 0) {
            this.mUserActivityTimeout = w.mAttrs.userActivityTimeout;
            if (ProtoLogCache.WM_DEBUG_KEEP_SCREEN_ON_enabled) {
                long protoLogParam02 = this.mUserActivityTimeout;
                ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_KEEP_SCREEN_ON, 221540118, 1, (String) null, new Object[]{Long.valueOf(protoLogParam02)});
            }
        }
        if (w.mHasSurface && canBeSeen) {
            if ((attrFlags & 128) != 0) {
                this.mHoldScreen = w.mSession;
                this.mHoldScreenWindow = w;
            } else if (w == this.mWmService.mLastWakeLockHoldingWindow && ProtoLogCache.WM_DEBUG_KEEP_SCREEN_ON_enabled) {
                String protoLogParam03 = String.valueOf(w);
                String protoLogParam12 = String.valueOf(Debug.getCallers(10));
                ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_KEEP_SCREEN_ON, 2088592090, 0, (String) null, new Object[]{protoLogParam03, protoLogParam12});
            }
            if (!syswin && w.mAttrs.screenBrightness >= 0.0f && Float.isNaN(this.mScreenBrightnessOverride)) {
                this.mScreenBrightnessOverride = w.mAttrs.screenBrightness;
            }
            int type = attrs.type;
            DisplayContent displayContent = w.getDisplayContent();
            if (displayContent != null && displayContent.isDefaultDisplay) {
                if (w.isDreamWindow() || this.mWmService.mPolicy.isKeyguardShowing()) {
                    this.mObscureApplicationContentOnSecondaryDisplays = true;
                }
                displayHasContent2 = true;
            } else if (displayContent != null && (!this.mObscureApplicationContentOnSecondaryDisplays || (obscured && type == 2009))) {
                displayHasContent2 = true;
            } else {
                displayHasContent2 = displayHasContent;
            }
            if ((262144 & privateflags) != 0) {
                this.mSustainedPerformanceModeCurrent = true;
            }
            return displayHasContent2;
        }
        return displayHasContent;
    }

    boolean updateRotationUnchecked() {
        boolean changed = false;
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            if (((DisplayContent) this.mChildren.get(i)).getDisplayRotation().updateRotationAndSendNewConfigIfChanged()) {
                changed = true;
            }
        }
        return changed;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean copyAnimToLayoutParams() {
        boolean doRequest = false;
        int bulkUpdateParams = this.mWmService.mAnimator.mBulkUpdateParams;
        if ((bulkUpdateParams & 1) != 0) {
            this.mUpdateRotation = true;
            doRequest = true;
        }
        if (this.mOrientationChangeComplete) {
            this.mLastWindowFreezeSource = this.mWmService.mAnimator.mLastWindowFreezeSource;
            if (this.mWmService.mWindowsFreezingScreen != 0) {
                doRequest = true;
            }
        }
        if ((bulkUpdateParams & 2) != 0) {
            this.mWallpaperActionPending = true;
        }
        return doRequest;
    }

    /* loaded from: classes2.dex */
    private final class MyHandler extends Handler {
        public MyHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    RootWindowContainer.this.mWmService.mPowerManagerInternal.setScreenBrightnessOverrideFromWindowManager(Float.intBitsToFloat(msg.arg1));
                    return;
                case 2:
                    RootWindowContainer.this.mWmService.mPowerManagerInternal.setUserActivityTimeoutOverrideFromWindowManager(((Long) msg.obj).longValue());
                    return;
                default:
                    return;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dumpDisplayContents(PrintWriter pw) {
        pw.println("WINDOW MANAGER DISPLAY CONTENTS (dumpsys window displays)");
        if (this.mWmService.mDisplayReady) {
            int count = this.mChildren.size();
            for (int i = 0; i < count; i++) {
                DisplayContent displayContent = (DisplayContent) this.mChildren.get(i);
                displayContent.dump(pw, "  ", true);
            }
            return;
        }
        pw.println("  NO DISPLAY");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dumpTopFocusedDisplayId(PrintWriter pw) {
        pw.print("  mTopFocusedDisplayId=");
        pw.println(this.mTopFocusedDisplayId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dumpLayoutNeededDisplayIds(PrintWriter pw) {
        if (!isLayoutNeeded()) {
            return;
        }
        pw.print("  mLayoutNeeded on displays=");
        int count = this.mChildren.size();
        for (int displayNdx = 0; displayNdx < count; displayNdx++) {
            DisplayContent displayContent = (DisplayContent) this.mChildren.get(displayNdx);
            if (displayContent.isLayoutNeeded()) {
                pw.print(displayContent.getDisplayId());
            }
        }
        pw.println();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dumpWindowsNoHeader(final PrintWriter pw, final boolean dumpAll, final ArrayList<WindowState> windows) {
        final int[] index = new int[1];
        forAllWindows(new Consumer() { // from class: com.android.server.wm.RootWindowContainer$$ExternalSyntheticLambda57
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                RootWindowContainer.lambda$dumpWindowsNoHeader$9(windows, pw, index, dumpAll, (WindowState) obj);
            }
        }, true);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$dumpWindowsNoHeader$9(ArrayList windows, PrintWriter pw, int[] index, boolean dumpAll, WindowState w) {
        if (windows == null || windows.contains(w)) {
            pw.println("  Window #" + index[0] + " " + w + ":");
            w.dump(pw, "    ", dumpAll || windows != null);
            index[0] = index[0] + 1;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dumpTokens(PrintWriter pw, boolean dumpAll) {
        pw.println("  All tokens:");
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            ((DisplayContent) this.mChildren.get(i)).dumpTokens(pw, dumpAll);
        }
    }

    @Override // com.android.server.wm.WindowContainer, com.android.server.wm.ConfigurationContainer
    public void dumpDebug(ProtoOutputStream proto, long fieldId, int logLevel) {
        if (logLevel == 2 && !isVisible()) {
            return;
        }
        long token = proto.start(fieldId);
        super.dumpDebug(proto, 1146756268033L, logLevel);
        this.mTaskSupervisor.getKeyguardController().dumpDebug(proto, 1146756268037L);
        proto.write(1133871366150L, this.mTaskSupervisor.mRecentTasks.isRecentsComponentHomeActivity(this.mCurrentUser));
        proto.end(token);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.wm.ConfigurationContainer
    public String getName() {
        return "ROOT";
    }

    @Override // com.android.server.wm.WindowContainer
    void scheduleAnimation() {
        this.mWmService.scheduleAnimationLocked();
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.wm.WindowContainer
    public void removeChild(DisplayContent dc) {
        super.removeChild((RootWindowContainer) dc);
        if (this.mTopFocusedDisplayId == dc.getDisplayId()) {
            this.mWmService.updateFocusedWindowLocked(0, true);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void forAllDisplays(Consumer<DisplayContent> callback) {
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            callback.accept((DisplayContent) this.mChildren.get(i));
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void forAllDisplayPolicies(Consumer<DisplayPolicy> callback) {
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            callback.accept(((DisplayContent) this.mChildren.get(i)).getDisplayPolicy());
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public WindowState getCurrentInputMethodWindow() {
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            DisplayContent displayContent = (DisplayContent) this.mChildren.get(i);
            if (displayContent.mInputMethodWindow != null) {
                return displayContent.mInputMethodWindow;
            }
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void getDisplayContextsWithNonToastVisibleWindows(final int pid, List<Context> outContexts) {
        if (outContexts == null) {
            return;
        }
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            DisplayContent dc = (DisplayContent) this.mChildren.get(i);
            if (dc.getWindow(new Predicate() { // from class: com.android.server.wm.RootWindowContainer$$ExternalSyntheticLambda56
                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    return RootWindowContainer.lambda$getDisplayContextsWithNonToastVisibleWindows$10(pid, (WindowState) obj);
                }
            }) != null) {
                outContexts.add(dc.getDisplayUiContext());
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$getDisplayContextsWithNonToastVisibleWindows$10(int pid, WindowState w) {
        return pid == w.mSession.mPid && w.isVisibleNow() && w.mAttrs.type != 2005;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Context getDisplayUiContext(int displayId) {
        if (getDisplayContent(displayId) != null) {
            return getDisplayContent(displayId).getDisplayUiContext();
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setWindowManager(WindowManagerService wm) {
        this.mWindowManager = wm;
        DisplayManager displayManager = (DisplayManager) this.mService.mContext.getSystemService(DisplayManager.class);
        this.mDisplayManager = displayManager;
        displayManager.registerDisplayListener(this, this.mService.mUiHandler);
        this.mDisplayManagerInternal = (DisplayManagerInternal) LocalServices.getService(DisplayManagerInternal.class);
        Display[] displays = this.mDisplayManager.getDisplays();
        for (Display display : displays) {
            DisplayContent displayContent = new DisplayContent(display, this);
            addChild((RootWindowContainer) displayContent, Integer.MIN_VALUE);
            if (displayContent.mDisplayId == 0) {
                this.mDefaultDisplay = displayContent;
            }
        }
        TaskDisplayArea defaultTaskDisplayArea = getDefaultTaskDisplayArea();
        defaultTaskDisplayArea.getOrCreateRootHomeTask(true);
        positionChildAt(Integer.MAX_VALUE, defaultTaskDisplayArea.mDisplayContent, false);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public DisplayContent getDefaultDisplay() {
        return this.mDefaultDisplay;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public TaskDisplayArea getDefaultTaskDisplayArea() {
        return this.mDefaultDisplay.getDefaultTaskDisplayArea();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public DisplayContent getDisplayContent(String uniqueId) {
        for (int i = getChildCount() - 1; i >= 0; i--) {
            DisplayContent display = (DisplayContent) getChildAt(i);
            boolean isValid = display.mDisplay.isValid();
            if (isValid && display.mDisplay.getUniqueId().equals(uniqueId)) {
                return display;
            }
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public DisplayContent getDisplayContent(int displayId) {
        for (int i = getChildCount() - 1; i >= 0; i--) {
            DisplayContent displayContent = (DisplayContent) getChildAt(i);
            if (displayContent.mDisplayId == displayId) {
                return displayContent;
            }
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public DisplayContent getDisplayContentOrCreate(int displayId) {
        Display display;
        DisplayContent displayContent = getDisplayContent(displayId);
        if (displayContent != null) {
            return displayContent;
        }
        DisplayManager displayManager = this.mDisplayManager;
        if (displayManager == null || (display = displayManager.getDisplay(displayId)) == null) {
            return null;
        }
        DisplayContent displayContent2 = new DisplayContent(display, this);
        if ((displayContent2.getDisplay().getFlags() & 1048576) != 0) {
            addChild((RootWindowContainer) displayContent2, Integer.MAX_VALUE);
            displayContent2.setSourceConnectDisplay();
        } else {
            addChild((RootWindowContainer) displayContent2, Integer.MIN_VALUE);
        }
        return displayContent2;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActivityRecord getDefaultDisplayHomeActivityForUser(int userId) {
        return getDefaultTaskDisplayArea().getHomeActivityForUser(userId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean startHomeOnAllDisplays(int userId, String reason) {
        boolean homeStarted = false;
        for (int i = getChildCount() - 1; i >= 0; i--) {
            int displayId = ((DisplayContent) getChildAt(i)).mDisplayId;
            homeStarted |= startHomeOnDisplay(userId, reason, displayId);
        }
        return homeStarted;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void startHomeOnEmptyDisplays(final String reason) {
        forAllTaskDisplayAreas(new Consumer() { // from class: com.android.server.wm.RootWindowContainer$$ExternalSyntheticLambda46
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                RootWindowContainer.this.m8210x1619eb5d(reason, (TaskDisplayArea) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$startHomeOnEmptyDisplays$11$com-android-server-wm-RootWindowContainer  reason: not valid java name */
    public /* synthetic */ void m8210x1619eb5d(String reason, TaskDisplayArea taskDisplayArea) {
        if (taskDisplayArea.topRunningActivity() == null) {
            startHomeOnTaskDisplayArea(this.mCurrentUser, reason, taskDisplayArea, false, false);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean startHomeOnDisplay(int userId, String reason, int displayId) {
        return startHomeOnDisplay(userId, reason, displayId, false, false);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean startHomeOnDisplay(final int userId, final String reason, int displayId, final boolean allowInstrumenting, final boolean fromHomeKey) {
        if (displayId == -1) {
            Task rootTask = getTopDisplayFocusedRootTask();
            displayId = rootTask != null ? rootTask.getDisplayId() : 0;
        }
        DisplayContent display = getDisplayContent(displayId);
        return ((Boolean) display.reduceOnAllTaskDisplayAreas(new BiFunction() { // from class: com.android.server.wm.RootWindowContainer$$ExternalSyntheticLambda9
            @Override // java.util.function.BiFunction
            public final Object apply(Object obj, Object obj2) {
                return RootWindowContainer.this.m8209x48ad367a(userId, reason, allowInstrumenting, fromHomeKey, (TaskDisplayArea) obj, (Boolean) obj2);
            }
        }, false)).booleanValue();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$startHomeOnDisplay$12$com-android-server-wm-RootWindowContainer  reason: not valid java name */
    public /* synthetic */ Boolean m8209x48ad367a(int userId, String reason, boolean allowInstrumenting, boolean fromHomeKey, TaskDisplayArea taskDisplayArea, Boolean result) {
        return Boolean.valueOf(result.booleanValue() | startHomeOnTaskDisplayArea(userId, reason, taskDisplayArea, allowInstrumenting, fromHomeKey));
    }

    boolean startHomeOnTaskDisplayArea(int userId, String reason, TaskDisplayArea taskDisplayArea, boolean allowInstrumenting, boolean fromHomeKey) {
        if (taskDisplayArea == null) {
            Task rootTask = getTopDisplayFocusedRootTask();
            taskDisplayArea = rootTask != null ? rootTask.getDisplayArea() : getDefaultTaskDisplayArea();
        }
        Intent homeIntent = null;
        ActivityInfo aInfo = null;
        if (taskDisplayArea == getDefaultTaskDisplayArea()) {
            homeIntent = this.mService.getHomeIntent();
            aInfo = resolveHomeActivity(userId, homeIntent);
        } else if (shouldPlaceSecondaryHomeOnDisplayArea(taskDisplayArea)) {
            Pair<ActivityInfo, Intent> info = resolveSecondaryHomeActivity(userId, taskDisplayArea);
            aInfo = (ActivityInfo) info.first;
            homeIntent = (Intent) info.second;
        }
        if (aInfo == null || homeIntent == null || !canStartHomeOnDisplayArea(aInfo, taskDisplayArea, allowInstrumenting)) {
            return false;
        }
        homeIntent.setComponent(new ComponentName(aInfo.applicationInfo.packageName, aInfo.name));
        homeIntent.setFlags(homeIntent.getFlags() | 268435456);
        if (fromHomeKey) {
            homeIntent.putExtra("android.intent.extra.FROM_HOME_KEY", true);
            if (this.mWindowManager.getRecentsAnimationController() != null) {
                this.mWindowManager.getRecentsAnimationController().cancelAnimationForHomeStart();
            }
        }
        homeIntent.putExtra("android.intent.extra.EXTRA_START_REASON", reason);
        String myReason = reason + ":" + userId + ":" + UserHandle.getUserId(aInfo.applicationInfo.uid) + ":" + taskDisplayArea.getDisplayId();
        this.mService.getActivityStartController().startHomeActivity(homeIntent, aInfo, myReason, taskDisplayArea);
        return true;
    }

    ActivityInfo resolveHomeActivity(int userId, Intent homeIntent) {
        ComponentName comp = homeIntent.getComponent();
        ActivityInfo aInfo = null;
        try {
            if (comp != null) {
                aInfo = AppGlobals.getPackageManager().getActivityInfo(comp, (long) GadgetFunction.NCM, userId);
            } else {
                String resolvedType = homeIntent.resolveTypeIfNeeded(this.mService.mContext.getContentResolver());
                ResolveInfo info = AppGlobals.getPackageManager().resolveIntent(homeIntent, resolvedType, (long) GadgetFunction.NCM, userId);
                if (info != null) {
                    aInfo = info.activityInfo;
                }
            }
        } catch (RemoteException e) {
        }
        if (aInfo == null) {
            Slog.wtf("WindowManager", "No home screen found for " + homeIntent, new Throwable());
            return null;
        }
        ActivityInfo aInfo2 = new ActivityInfo(aInfo);
        aInfo2.applicationInfo = this.mService.getAppInfoForUser(aInfo2.applicationInfo, userId);
        return aInfo2;
    }

    Pair<ActivityInfo, Intent> resolveSecondaryHomeActivity(int userId, TaskDisplayArea taskDisplayArea) {
        if (taskDisplayArea == getDefaultTaskDisplayArea()) {
            throw new IllegalArgumentException("resolveSecondaryHomeActivity: Should not be default task container");
        }
        Intent homeIntent = this.mService.getHomeIntent();
        ActivityInfo aInfo = resolveHomeActivity(userId, homeIntent);
        if (aInfo != null) {
            if (ResolverActivity.class.getName().equals(aInfo.name)) {
                aInfo = null;
            } else {
                homeIntent = this.mService.getSecondaryHomeIntent(aInfo.applicationInfo.packageName);
                List<ResolveInfo> resolutions = resolveActivities(userId, homeIntent);
                int size = resolutions.size();
                String targetName = aInfo.name;
                aInfo = null;
                int i = 0;
                while (true) {
                    if (i >= size) {
                        break;
                    }
                    ResolveInfo resolveInfo = resolutions.get(i);
                    if (!resolveInfo.activityInfo.name.equals(targetName)) {
                        i++;
                    } else {
                        aInfo = resolveInfo.activityInfo;
                        break;
                    }
                }
                if (aInfo == null && size > 0) {
                    aInfo = resolutions.get(0).activityInfo;
                }
            }
        }
        if (aInfo != null && !canStartHomeOnDisplayArea(aInfo, taskDisplayArea, false)) {
            aInfo = null;
        }
        if (aInfo == null) {
            homeIntent = this.mService.getSecondaryHomeIntent(null);
            aInfo = resolveHomeActivity(userId, homeIntent);
        }
        return Pair.create(aInfo, homeIntent);
    }

    List<ResolveInfo> resolveActivities(int userId, Intent homeIntent) {
        try {
            String resolvedType = homeIntent.resolveTypeIfNeeded(this.mService.mContext.getContentResolver());
            List<ResolveInfo> resolutions = AppGlobals.getPackageManager().queryIntentActivities(homeIntent, resolvedType, (long) GadgetFunction.NCM, userId).getList();
            return resolutions;
        } catch (RemoteException e) {
            List<ResolveInfo> resolutions2 = new ArrayList<>();
            return resolutions2;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean resumeHomeActivity(ActivityRecord prev, String reason, TaskDisplayArea taskDisplayArea) {
        if (!this.mService.isBooting() && !this.mService.isBooted()) {
            return false;
        }
        if (taskDisplayArea == null) {
            taskDisplayArea = getDefaultTaskDisplayArea();
        }
        ActivityRecord r = taskDisplayArea.getHomeActivity();
        String myReason = reason + " resumeHomeActivity";
        if (r != null && !r.finishing) {
            r.moveFocusableActivityToTop(myReason);
            return resumeFocusedTasksTopActivities(r.getRootTask(), prev, null);
        }
        return startHomeOnTaskDisplayArea(this.mCurrentUser, myReason, taskDisplayArea, false, false);
    }

    boolean shouldPlaceSecondaryHomeOnDisplayArea(TaskDisplayArea taskDisplayArea) {
        DisplayContent display;
        if (getDefaultTaskDisplayArea() == taskDisplayArea) {
            throw new IllegalArgumentException("shouldPlaceSecondaryHomeOnDisplay: Should not be on default task container");
        }
        if (taskDisplayArea == null || !taskDisplayArea.canHostHomeTask()) {
            return false;
        }
        if (taskDisplayArea.getDisplayId() == 0 || this.mService.mSupportsMultiDisplay) {
            boolean deviceProvisioned = Settings.Global.getInt(this.mService.mContext.getContentResolver(), "device_provisioned", 0) != 0;
            return deviceProvisioned && StorageManager.isUserKeyUnlocked(this.mCurrentUser) && (display = taskDisplayArea.getDisplayContent()) != null && !display.isRemoved() && display.supportsSystemDecorations() && !ITranRootWindowContainer.Instance().isThunderbackWindow(taskDisplayArea.getConfiguration().windowConfiguration.isThunderbackWindow());
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean canStartHomeOnDisplayArea(ActivityInfo homeInfo, TaskDisplayArea taskDisplayArea, boolean allowInstrumenting) {
        if (this.mService.mFactoryTest == 1 && this.mService.mTopAction == null) {
            return false;
        }
        WindowProcessController app = this.mService.getProcessController(homeInfo.processName, homeInfo.applicationInfo.uid);
        if (allowInstrumenting || app == null || !app.isInstrumenting()) {
            int displayId = taskDisplayArea != null ? taskDisplayArea.getDisplayId() : -1;
            if (displayId == 0 || (displayId != -1 && displayId == this.mService.mVr2dDisplayId)) {
                return true;
            }
            if (shouldPlaceSecondaryHomeOnDisplayArea(taskDisplayArea)) {
                boolean supportMultipleInstance = (homeInfo.launchMode == 2 || homeInfo.launchMode == 3) ? false : true;
                return supportMultipleInstance;
            }
            return false;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean ensureVisibilityAndConfig(ActivityRecord starting, int displayId, boolean markFrozenIfConfigChanged, boolean deferResume) {
        ensureActivitiesVisible(null, 0, false, false);
        if (displayId == -1) {
            return true;
        }
        DisplayContent displayContent = getDisplayContent(displayId);
        Configuration config = null;
        if (displayContent != null) {
            config = displayContent.updateOrientation(starting, true);
        }
        if (starting != null) {
            starting.reportDescendantOrientationChangeIfNeeded();
        }
        if (starting != null && markFrozenIfConfigChanged && config != null) {
            starting.frozenBeforeDestroy = true;
        }
        if (displayContent == null) {
            return true;
        }
        return displayContent.updateDisplayOverrideConfigurationLocked(config, starting, deferResume, null);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public List<ActivityAssistInfo> getTopVisibleActivities() {
        final ArrayList<ActivityAssistInfo> topVisibleActivities = new ArrayList<>();
        final Task topFocusedRootTask = getTopDisplayFocusedRootTask();
        forAllRootTasks(new Consumer() { // from class: com.android.server.wm.RootWindowContainer$$ExternalSyntheticLambda8
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                RootWindowContainer.lambda$getTopVisibleActivities$13(Task.this, topVisibleActivities, (Task) obj);
            }
        });
        return topVisibleActivities;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$getTopVisibleActivities$13(Task topFocusedRootTask, ArrayList topVisibleActivities, Task rootTask) {
        ActivityRecord top;
        if (rootTask.shouldBeVisible(null) && (top = rootTask.getTopNonFinishingActivity()) != null) {
            ActivityAssistInfo visibleActivity = new ActivityAssistInfo(top);
            if (rootTask == topFocusedRootTask) {
                topVisibleActivities.add(0, visibleActivity);
            } else {
                topVisibleActivities.add(visibleActivity);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Task getTopDisplayFocusedRootTask() {
        for (int i = getChildCount() - 1; i >= 0; i--) {
            Task focusedRootTask = ((DisplayContent) getChildAt(i)).getFocusedRootTask();
            if (focusedRootTask != null) {
                return focusedRootTask;
            }
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean inTopVisiblePackages(String packageName) {
        ActivityRecord top;
        ActivityRecord record;
        if (packageName == null || (top = getTopResumedActivity()) == null || top.app == null || top.getRootTask() == null) {
            return false;
        }
        if (packageName.equals(top.packageName)) {
            return true;
        }
        if (top.getWindowingMode() == 6) {
            Task pTask = top.getRootTask();
            if (pTask.isLeafTask()) {
                for (int i = pTask.mChildren.size() - 1; i >= 0; i--) {
                    WindowContainer fragment = (WindowContainer) pTask.mChildren.get(i);
                    if (fragment.asTaskFragment() != null && (record = fragment.asTaskFragment().getResumedActivity()) != null && record.packageName != null && packageName.equals(record.packageName)) {
                        return true;
                    }
                }
            }
            if (packageName.equals(top.launchedFromPackage)) {
                return true;
            }
        }
        return false;
    }

    public String getTopActivity() {
        ActivityRecord top = getTopResumedActivity();
        if (top == null || top.app == null || top.getRootTask() == null || top.mActivityComponent == null) {
            return "";
        }
        String topActivity = top.packageName + SliceClientPermissions.SliceAuthority.DELIMITER + top.mActivityComponent.getClassName();
        return topActivity;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActivityRecord getTopResumedActivity() {
        Task focusedRootTask = getTopDisplayFocusedRootTask();
        if (focusedRootTask == null) {
            return null;
        }
        ActivityRecord resumedActivity = focusedRootTask.getTopResumedActivity();
        if (resumedActivity != null && resumedActivity.app != null) {
            return resumedActivity;
        }
        return (ActivityRecord) getItemFromTaskDisplayAreas(new Function() { // from class: com.android.server.wm.RootWindowContainer$$ExternalSyntheticLambda12
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                return ((TaskDisplayArea) obj).getFocusedActivity();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isTopDisplayFocusedRootTask(Task task) {
        return task != null && task == getTopDisplayFocusedRootTask();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean attachApplication(WindowProcessController app) throws RemoteException {
        try {
            return this.mAttachApplicationHelper.process(app);
        } finally {
            this.mAttachApplicationHelper.reset();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void ensureActivitiesVisible(ActivityRecord starting, int configChanges, boolean preserveWindows) {
        ensureActivitiesVisible(starting, configChanges, preserveWindows, true);
    }

    void ensureActivitiesVisible(ActivityRecord starting, int configChanges, boolean preserveWindows, boolean notifyClients) {
        if (this.mTaskSupervisor.inActivityVisibilityUpdate() || this.mTaskSupervisor.isRootVisibilityUpdateDeferred()) {
            return;
        }
        try {
            this.mTaskSupervisor.beginActivityVisibilityUpdate();
            for (int displayNdx = getChildCount() - 1; displayNdx >= 0; displayNdx--) {
                DisplayContent display = (DisplayContent) getChildAt(displayNdx);
                display.ensureActivitiesVisible(starting, configChanges, preserveWindows, notifyClients);
            }
        } finally {
            this.mTaskSupervisor.endActivityVisibilityUpdate();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean switchUser(final int userId, UserState uss) {
        Task topFocusedRootTask = getTopDisplayFocusedRootTask();
        int focusRootTaskId = topFocusedRootTask != null ? topFocusedRootTask.getRootTaskId() : -1;
        removeRootTasksInWindowingModes(2);
        this.mUserRootTaskInFront.put(this.mCurrentUser, focusRootTaskId);
        this.mCurrentUser = userId;
        this.mTaskSupervisor.mStartingUsers.add(uss);
        forAllRootTasks(new Consumer() { // from class: com.android.server.wm.RootWindowContainer$$ExternalSyntheticLambda15
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ((Task) obj).switchUser(userId);
            }
        });
        int restoreRootTaskId = this.mUserRootTaskInFront.get(userId);
        Task rootTask = getRootTask(restoreRootTaskId);
        if (rootTask == null) {
            rootTask = getDefaultTaskDisplayArea().getOrCreateRootHomeTask();
        }
        boolean homeInFront = rootTask.isActivityTypeHome();
        if (rootTask.isOnHomeDisplay()) {
            rootTask.moveToFront("switchUserOnHomeDisplay");
        } else {
            resumeHomeActivity(null, "switchUserOnOtherDisplay", getDefaultTaskDisplayArea());
        }
        return homeInFront;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void removeUser(int userId) {
        this.mUserRootTaskInFront.delete(userId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateUserRootTask(int userId, Task rootTask) {
        if (userId != this.mCurrentUser) {
            if (rootTask == null) {
                rootTask = getDefaultTaskDisplayArea().getOrCreateRootHomeTask();
            }
            this.mUserRootTaskInFront.put(userId, rootTask.getRootTaskId());
        }
    }

    void moveRootTaskToTaskDisplayArea(int rootTaskId, TaskDisplayArea taskDisplayArea, boolean onTop) {
        Task rootTask = getRootTask(rootTaskId);
        if (rootTask == null) {
            throw new IllegalArgumentException("moveRootTaskToTaskDisplayArea: Unknown rootTaskId=" + rootTaskId);
        }
        TaskDisplayArea currentTaskDisplayArea = rootTask.getDisplayArea();
        if (currentTaskDisplayArea == null) {
            throw new IllegalStateException("moveRootTaskToTaskDisplayArea: rootTask=" + rootTask + " is not attached to any task display area.");
        }
        if (taskDisplayArea == null) {
            throw new IllegalArgumentException("moveRootTaskToTaskDisplayArea: Unknown taskDisplayArea=" + taskDisplayArea);
        }
        if (currentTaskDisplayArea == taskDisplayArea) {
            throw new IllegalArgumentException("Trying to move rootTask=" + rootTask + " to its current taskDisplayArea=" + taskDisplayArea);
        }
        rootTask.reparent(taskDisplayArea, onTop);
        rootTask.resumeNextFocusAfterReparent();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void moveRootTaskToDisplay(int rootTaskId, int displayId, boolean onTop) {
        DisplayContent displayContent = getDisplayContentOrCreate(displayId);
        if (displayContent == null) {
            throw new IllegalArgumentException("moveRootTaskToDisplay: Unknown displayId=" + displayId);
        }
        moveRootTaskToTaskDisplayArea(rootTaskId, displayContent.getDefaultTaskDisplayArea(), onTop);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void moveActivityToPinnedRootTask(ActivityRecord r, ActivityRecord launchIntoPipHostActivity, String reason) {
        Task rootTask;
        this.mService.deferWindowLayout();
        TaskDisplayArea taskDisplayArea = r.getDisplayArea();
        try {
            Task task = r.getTask();
            TransitionController transitionController = task.mTransitionController;
            Transition newTransition = null;
            if (transitionController.isCollecting()) {
                transitionController.setReady(task, false);
            } else if (transitionController.getTransitionPlayer() != null) {
                newTransition = transitionController.createTransition(10);
            }
            Task rootPinnedTask = taskDisplayArea.getRootPinnedTask();
            if (rootPinnedTask != null) {
                transitionController.collect(rootPinnedTask);
                removeRootTasksInWindowingModes(2);
            }
            r.getDisplayContent().prepareAppTransition(0);
            TaskFragment organizedTf = r.getOrganizedTaskFragment();
            boolean singleActivity = task.getNonFinishingActivityCount() == 1;
            if (singleActivity) {
                rootTask = task;
                rootTask.maybeApplyLastRecentsAnimationTransaction();
            } else {
                rootTask = new Task.Builder(this.mService).setActivityType(r.getActivityType()).setOnTop(true).setActivityInfo(r.info).setParent(taskDisplayArea).setIntent(r.intent).setDeferTaskAppear(true).setHasBeenVisible(true).build();
                r.setLastParentBeforePip(launchIntoPipHostActivity);
                rootTask.setLastNonFullscreenBounds(task.mLastNonFullscreenBounds);
                rootTask.setBounds(task.getBounds());
                if (task.mLastRecentsAnimationTransaction != null) {
                    rootTask.setLastRecentsAnimationTransaction(task.mLastRecentsAnimationTransaction, task.mLastRecentsAnimationOverlay);
                    task.clearLastRecentsAnimationTransaction(false);
                }
                if (organizedTf != null && organizedTf.getNonFinishingActivityCount() == 1 && organizedTf.getTopNonFinishingActivity() == r) {
                    organizedTf.mClearedTaskFragmentForPip = true;
                }
                try {
                    r.reparent(rootTask, Integer.MAX_VALUE, reason);
                    rootTask.maybeApplyLastRecentsAnimationTransaction();
                    ActivityRecord oldTopActivity = task.getTopMostActivity();
                    if (oldTopActivity != null && oldTopActivity.isState(ActivityRecord.State.STOPPED) && task.getDisplayContent().mAppTransition.containsTransitRequest(4)) {
                        task.getDisplayContent().mClosingApps.add(oldTopActivity);
                        oldTopActivity.mRequestForceTransition = true;
                    }
                } catch (Throwable th) {
                    th = th;
                    this.mService.continueWindowLayout();
                    throw th;
                }
            }
            int intermediateWindowingMode = rootTask.getWindowingMode();
            if (rootTask.getParent() != taskDisplayArea) {
                rootTask.reparent(taskDisplayArea, true);
            }
            if (newTransition != null) {
                transitionController.requestStartTransition(newTransition, rootTask, null, null);
            }
            transitionController.collect(rootTask);
            r.setWindowingMode(intermediateWindowingMode);
            r.mWaitForEnteringPinnedMode = true;
            r.onBeforeEnterPinnedMode();
            rootTask.forAllTaskFragments(new Consumer() { // from class: com.android.server.wm.RootWindowContainer$$ExternalSyntheticLambda20
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    RootWindowContainer.lambda$moveActivityToPinnedRootTask$15((TaskFragment) obj);
                }
            });
            rootTask.setWindowingMode(2);
            if (r.getOptions() != null && r.getOptions().isLaunchIntoPip()) {
                this.mWindowManager.mTaskSnapshotController.recordTaskSnapshot(task, false);
                rootTask.setBounds(r.getOptions().getLaunchBounds());
            }
            rootTask.setDeferTaskAppear(false);
            r.supportsEnterPipOnTaskSwitch = false;
            if (organizedTf != null && organizedTf.mClearedTaskFragmentForPip && organizedTf.isTaskVisibleRequested()) {
                this.mService.mTaskFragmentOrganizerController.dispatchPendingInfoChangedEvent(organizedTf);
            }
            this.mService.continueWindowLayout();
            ensureActivitiesVisible(null, 0, false);
            resumeFocusedTasksTopActivities();
            notifyActivityPipModeChanged(r.getTask(), r);
        } catch (Throwable th2) {
            th = th2;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$moveActivityToPinnedRootTask$15(TaskFragment tf) {
        if (!tf.isOrganizedTaskFragment()) {
            return;
        }
        tf.resetAdjacentTaskFragment();
        if (tf.getTopNonFinishingActivity() != null) {
            tf.updateRequestedOverrideConfiguration(Configuration.EMPTY);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void notifyActivityPipModeChanged(Task task, ActivityRecord r) {
        boolean inPip = r != null;
        if (inPip) {
            this.mService.getTaskChangeNotificationController().notifyActivityPinned(r);
        } else {
            this.mService.getTaskChangeNotificationController().notifyActivityUnpinned();
        }
        this.mWindowManager.mPolicy.setPipVisibilityLw(inPip);
        this.mWmService.mTransactionFactory.get().setTrustedOverlay(task.getSurfaceControl(), inPip).apply();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void executeAppTransitionForAllDisplay() {
        for (int displayNdx = getChildCount() - 1; displayNdx >= 0; displayNdx--) {
            DisplayContent display = (DisplayContent) getChildAt(displayNdx);
            display.mDisplayContent.executeAppTransition();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActivityRecord findTask(ActivityRecord r, TaskDisplayArea preferredTaskDisplayArea) {
        return findTask(r.getActivityType(), r.taskAffinity, r.intent, r.info, preferredTaskDisplayArea);
    }

    ActivityRecord findTask(int activityType, String taskAffinity, Intent intent, ActivityInfo info, final TaskDisplayArea preferredTaskDisplayArea) {
        if (ProtoLogCache.WM_DEBUG_TASKS_enabled) {
            String protoLogParam0 = String.valueOf(activityType);
            String protoLogParam1 = String.valueOf(taskAffinity);
            String protoLogParam2 = String.valueOf(intent);
            String protoLogParam3 = String.valueOf(info);
            String protoLogParam4 = String.valueOf(preferredTaskDisplayArea);
            ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_TASKS, -1559645910, 0, (String) null, new Object[]{protoLogParam0, protoLogParam1, protoLogParam2, protoLogParam3, protoLogParam4});
        }
        this.mTmpFindTaskResult.init(activityType, taskAffinity, intent, info);
        ActivityRecord candidateActivity = null;
        if (preferredTaskDisplayArea != null) {
            this.mTmpFindTaskResult.process(preferredTaskDisplayArea);
            if (this.mTmpFindTaskResult.mIdealRecord != null) {
                return this.mTmpFindTaskResult.mIdealRecord;
            }
            if (this.mTmpFindTaskResult.mCandidateRecord != null) {
                candidateActivity = this.mTmpFindTaskResult.mCandidateRecord;
            }
        }
        ActivityRecord idealMatchActivity = (ActivityRecord) getItemFromTaskDisplayAreas(new Function() { // from class: com.android.server.wm.RootWindowContainer$$ExternalSyntheticLambda7
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                return RootWindowContainer.this.m8190lambda$findTask$16$comandroidserverwmRootWindowContainer(preferredTaskDisplayArea, (TaskDisplayArea) obj);
            }
        });
        if (idealMatchActivity != null) {
            return idealMatchActivity;
        }
        if (ProtoLogGroup.WM_DEBUG_TASKS.isEnabled() && candidateActivity == null && ProtoLogCache.WM_DEBUG_TASKS_enabled) {
            ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_TASKS, -1376035390, 0, (String) null, (Object[]) null);
        }
        return candidateActivity;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$findTask$16$com-android-server-wm-RootWindowContainer  reason: not valid java name */
    public /* synthetic */ ActivityRecord m8190lambda$findTask$16$comandroidserverwmRootWindowContainer(TaskDisplayArea preferredTaskDisplayArea, TaskDisplayArea taskDisplayArea) {
        if (taskDisplayArea == preferredTaskDisplayArea) {
            return null;
        }
        this.mTmpFindTaskResult.process(taskDisplayArea);
        if (this.mTmpFindTaskResult.mIdealRecord == null) {
            return null;
        }
        return this.mTmpFindTaskResult.mIdealRecord;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int finishTopCrashedActivities(final WindowProcessController app, final String reason) {
        final Task focusedRootTask = getTopDisplayFocusedRootTask();
        final Task[] finishedTask = new Task[1];
        forAllTasks(new Consumer() { // from class: com.android.server.wm.RootWindowContainer$$ExternalSyntheticLambda22
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                RootWindowContainer.lambda$finishTopCrashedActivities$17(WindowProcessController.this, reason, focusedRootTask, finishedTask, (Task) obj);
            }
        });
        if (finishedTask[0] != null) {
            return finishedTask[0].mTaskId;
        }
        return -1;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$finishTopCrashedActivities$17(WindowProcessController app, String reason, Task focusedRootTask, Task[] finishedTask, Task rootTask) {
        Task t = rootTask.finishTopCrashedActivityLocked(app, reason);
        if (rootTask == focusedRootTask || finishedTask[0] == null) {
            finishedTask[0] = t;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean resumeFocusedTasksTopActivities() {
        return resumeFocusedTasksTopActivities(null, null, null);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean resumeFocusedTasksTopActivities(Task targetRootTask, ActivityRecord target, ActivityOptions targetOptions) {
        return resumeFocusedTasksTopActivities(targetRootTask, target, targetOptions, false);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean resumeFocusedTasksTopActivities(final Task targetRootTask, final ActivityRecord target, final ActivityOptions targetOptions, boolean deferPause) {
        if (this.mTaskSupervisor.readyToResume()) {
            boolean result = false;
            if (targetRootTask != null && (targetRootTask.isTopRootTaskInDisplayArea() || getTopDisplayFocusedRootTask() == targetRootTask)) {
                result = targetRootTask.resumeTopActivityUncheckedLocked(target, targetOptions, deferPause);
            }
            int i = 1;
            boolean result2 = result;
            int displayNdx = getChildCount() - 1;
            while (displayNdx >= 0) {
                final DisplayContent display = (DisplayContent) getChildAt(displayNdx);
                final boolean curResult = result2;
                final boolean[] resumedOnDisplay = new boolean[i];
                display.forAllRootTasks(new Consumer() { // from class: com.android.server.wm.RootWindowContainer$$ExternalSyntheticLambda23
                    @Override // java.util.function.Consumer
                    public final void accept(Object obj) {
                        RootWindowContainer.this.m8208xa709673e(targetRootTask, resumedOnDisplay, curResult, display, targetOptions, target, (Task) obj);
                    }
                });
                boolean result3 = resumedOnDisplay[0] | result2;
                if (!resumedOnDisplay[0]) {
                    Task focusedRoot = display.getFocusedRootTask();
                    if (focusedRoot != null) {
                        result3 |= focusedRoot.resumeTopActivityUncheckedLocked(target, targetOptions);
                    } else if (targetRootTask == null) {
                        result3 |= resumeHomeActivity(null, "no-focusable-task", display.getDefaultTaskDisplayArea());
                    }
                    hookDisplayAreaChildCount();
                }
                result2 = result3;
                displayNdx--;
                i = 1;
            }
            return result2;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$resumeFocusedTasksTopActivities$18$com-android-server-wm-RootWindowContainer  reason: not valid java name */
    public /* synthetic */ void m8208xa709673e(Task targetRootTask, boolean[] resumedOnDisplay, boolean curResult, DisplayContent display, ActivityOptions targetOptions, ActivityRecord target, Task rootTask) {
        ActivityRecord topRunningActivity = rootTask.topRunningActivity();
        if (!rootTask.isFocusableAndVisible() || topRunningActivity == null) {
            return;
        }
        if (rootTask == targetRootTask) {
            resumedOnDisplay[0] = resumedOnDisplay[0] | curResult;
        } else if (!rootTask.getDisplayArea().isTopRootTask(rootTask) || !topRunningActivity.isState(ActivityRecord.State.RESUMED)) {
            resumedOnDisplay[0] = resumedOnDisplay[0] | topRunningActivity.makeActiveIfNeeded(target);
        } else if (rootTask.getConfiguration().windowConfiguration.isThunderbackWindow() && this.mStartActivityInDefaultTaskDisplayArea && (display.mAppTransition.containsTransitRequest(3) || display.mAppTransition.containsTransitRequest(1))) {
            Slog.i("WindowManager", "  ignore to setReady here.");
        } else if (this.mStartActivityInMultiTaskDisplayArea && !rootTask.getConfiguration().windowConfiguration.isThunderbackWindow() && display.mAppTransition.containsTransitRequest(1)) {
            Slog.i("WindowManager", "  ignore to setReady here.");
        } else if (targetRootTask != null && !targetRootTask.getConfiguration().windowConfiguration.isThunderbackWindow() && rootTask.getConfiguration().windowConfiguration.isThunderbackWindow() && display.mAppTransition.getFirstAppTransition() == 2) {
            Slog.i("WindowManager", "  ignore to setReady here that multiWindow rootTask should not impact the default DisplayArea rootTask.");
        } else {
            rootTask.executeAppTransition(targetOptions);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void applySleepTokens(boolean applyToRootTasks) {
        for (int displayNdx = getChildCount() - 1; displayNdx >= 0; displayNdx--) {
            final DisplayContent display = (DisplayContent) getChildAt(displayNdx);
            final boolean displayShouldSleep = display.shouldSleep();
            if (displayShouldSleep != display.isSleeping()) {
                display.setIsSleeping(displayShouldSleep);
                if (applyToRootTasks) {
                    display.forAllRootTasks(new Consumer() { // from class: com.android.server.wm.RootWindowContainer$$ExternalSyntheticLambda21
                        @Override // java.util.function.Consumer
                        public final void accept(Object obj) {
                            RootWindowContainer.this.m8188x68174036(displayShouldSleep, display, (Task) obj);
                        }
                    });
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$applySleepTokens$20$com-android-server-wm-RootWindowContainer  reason: not valid java name */
    public /* synthetic */ void m8188x68174036(boolean displayShouldSleep, DisplayContent display, Task rootTask) {
        if (displayShouldSleep) {
            rootTask.goToSleepIfPossible(false);
            return;
        }
        rootTask.forAllLeafTasksAndLeafTaskFragments(new Consumer() { // from class: com.android.server.wm.RootWindowContainer$$ExternalSyntheticLambda34
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ((TaskFragment) obj).awakeFromSleeping();
            }
        }, true);
        if (rootTask.isFocusedRootTaskOnDisplay() && !this.mTaskSupervisor.getKeyguardController().isKeyguardOrAodShowing(display.mDisplayId)) {
            rootTask.resumeTopActivityUncheckedLocked(null, null);
        }
        rootTask.ensureActivitiesVisible(null, 0, false);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public Task getRootTask(int rooTaskId) {
        for (int i = getChildCount() - 1; i >= 0; i--) {
            Task rootTask = ((DisplayContent) getChildAt(i)).getRootTask(rooTaskId);
            if (rootTask != null) {
                return rootTask;
            }
        }
        return null;
    }

    Task getRootTask(int windowingMode, int activityType) {
        for (int i = getChildCount() - 1; i >= 0; i--) {
            Task rootTask = ((DisplayContent) getChildAt(i)).getRootTask(windowingMode, activityType);
            if (rootTask != null) {
                return rootTask;
            }
        }
        return null;
    }

    private Task getRootTask(int windowingMode, int activityType, int displayId) {
        DisplayContent display = getDisplayContent(displayId);
        if (display == null) {
            return null;
        }
        return display.getRootTask(windowingMode, activityType);
    }

    private ActivityTaskManager.RootTaskInfo getRootTaskInfo(final Task task) {
        ActivityTaskManager.RootTaskInfo info = new ActivityTaskManager.RootTaskInfo();
        task.fillTaskInfo(info);
        DisplayContent displayContent = task.getDisplayContent();
        if (displayContent == null) {
            info.position = -1;
        } else {
            final int[] taskIndex = new int[1];
            final boolean[] hasFound = new boolean[1];
            displayContent.forAllRootTasks(new Predicate() { // from class: com.android.server.wm.RootWindowContainer$$ExternalSyntheticLambda35
                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    return RootWindowContainer.lambda$getRootTaskInfo$21(Task.this, hasFound, taskIndex, (Task) obj);
                }
            }, false);
            info.position = hasFound[0] ? taskIndex[0] : -1;
        }
        info.visible = task.shouldBeVisible(null);
        task.getBounds(info.bounds);
        int numTasks = task.getDescendantTaskCount();
        info.childTaskIds = new int[numTasks];
        info.childTaskNames = new String[numTasks];
        info.childTaskBounds = new Rect[numTasks];
        info.childTaskUserIds = new int[numTasks];
        int[] currentIndex = {0};
        PooledConsumer c = PooledLambda.obtainConsumer(new TriConsumer() { // from class: com.android.server.wm.RootWindowContainer$$ExternalSyntheticLambda36
            public final void accept(Object obj, Object obj2, Object obj3) {
                RootWindowContainer.processTaskForTaskInfo((Task) obj, (ActivityTaskManager.RootTaskInfo) obj2, (int[]) obj3);
            }
        }, PooledLambda.__(Task.class), info, currentIndex);
        task.forAllLeafTasks(c, false);
        c.recycle();
        ActivityRecord top = task.topRunningActivity();
        info.topActivity = top != null ? top.intent.getComponent() : null;
        return info;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$getRootTaskInfo$21(Task task, boolean[] hasFound, int[] taskIndex, Task rootTask) {
        if (task != rootTask) {
            taskIndex[0] = taskIndex[0] + 1;
            return false;
        }
        hasFound[0] = true;
        return true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void processTaskForTaskInfo(Task task, ActivityTaskManager.RootTaskInfo info, int[] currentIndex) {
        String str;
        int i = currentIndex[0];
        info.childTaskIds[i] = task.mTaskId;
        String[] strArr = info.childTaskNames;
        if (task.origActivity != null) {
            str = task.origActivity.flattenToString();
        } else if (task.realActivity != null) {
            str = task.realActivity.flattenToString();
        } else {
            str = task.getTopNonFinishingActivity() != null ? task.getTopNonFinishingActivity().packageName : UiModeManagerService.Shell.NIGHT_MODE_STR_UNKNOWN;
        }
        strArr[i] = str;
        info.childTaskBounds[i] = task.mAtmService.getTaskBounds(task.mTaskId);
        info.childTaskUserIds[i] = task.mUserId;
        currentIndex[0] = i + 1;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActivityTaskManager.RootTaskInfo getRootTaskInfo(int taskId) {
        Task task = getRootTask(taskId);
        if (task != null) {
            return getRootTaskInfo(task);
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActivityTaskManager.RootTaskInfo getRootTaskInfo(int windowingMode, int activityType) {
        Task rootTask = getRootTask(windowingMode, activityType);
        if (rootTask != null) {
            return getRootTaskInfo(rootTask);
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActivityTaskManager.RootTaskInfo getRootTaskInfo(int windowingMode, int activityType, int displayId) {
        Task rootTask = getRootTask(windowingMode, activityType, displayId);
        if (rootTask != null) {
            return getRootTaskInfo(rootTask);
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ArrayList<ActivityTaskManager.RootTaskInfo> getAllRootTaskInfos(int displayId) {
        final ArrayList<ActivityTaskManager.RootTaskInfo> list = new ArrayList<>();
        if (displayId == -1) {
            forAllRootTasks(new Consumer() { // from class: com.android.server.wm.RootWindowContainer$$ExternalSyntheticLambda44
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    RootWindowContainer.this.m8191xf266882(list, (Task) obj);
                }
            });
            return list;
        }
        DisplayContent display = getDisplayContent(displayId);
        if (display == null) {
            return list;
        }
        display.forAllRootTasks(new Consumer() { // from class: com.android.server.wm.RootWindowContainer$$ExternalSyntheticLambda45
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                RootWindowContainer.this.m8192xf467d743(list, (Task) obj);
            }
        });
        return list;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getAllRootTaskInfos$22$com-android-server-wm-RootWindowContainer  reason: not valid java name */
    public /* synthetic */ void m8191xf266882(ArrayList list, Task rootTask) {
        list.add(getRootTaskInfo(rootTask));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getAllRootTaskInfos$23$com-android-server-wm-RootWindowContainer  reason: not valid java name */
    public /* synthetic */ void m8192xf467d743(ArrayList list, Task rootTask) {
        list.add(getRootTaskInfo(rootTask));
    }

    @Override // android.hardware.display.DisplayManager.DisplayListener
    public void onDisplayAdded(int displayId) {
        if (ActivityTaskManagerDebugConfig.DEBUG_ROOT_TASK) {
            Slog.v("WindowManager", "Display added displayId=" + displayId);
        }
        synchronized (this.mService.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                DisplayContent display = getDisplayContentOrCreate(displayId);
                if (display == null) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return;
                }
                if (this.mService.isBooted() || this.mService.isBooting()) {
                    startSystemDecorations(display);
                }
                this.mWmService.mPossibleDisplayInfoMapper.removePossibleDisplayInfos(displayId);
                WindowManagerService.resetPriorityAfterLockedSection();
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    private void startSystemDecorations(DisplayContent displayContent) {
        startHomeOnDisplay(this.mCurrentUser, "displayAdded", displayContent.getDisplayId());
        displayContent.getDisplayPolicy().notifyDisplayReady();
    }

    @Override // android.hardware.display.DisplayManager.DisplayListener
    public void onDisplayRemoved(int displayId) {
        if (ActivityTaskManagerDebugConfig.DEBUG_ROOT_TASK) {
            Slog.v("WindowManager", "Display removed displayId=" + displayId);
        }
        if (displayId == 0) {
            throw new IllegalArgumentException("Can't remove the primary display.");
        }
        synchronized (this.mService.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                DisplayContent displayContent = getDisplayContent(displayId);
                if (displayContent == null) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return;
                }
                displayContent.remove();
                this.mWmService.mPossibleDisplayInfoMapper.removePossibleDisplayInfos(displayId);
                WindowManagerService.resetPriorityAfterLockedSection();
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    @Override // android.hardware.display.DisplayManager.DisplayListener
    public void onDisplayChanged(int displayId) {
        if (ActivityTaskManagerDebugConfig.DEBUG_ROOT_TASK) {
            Slog.v("WindowManager", "Display changed displayId=" + displayId);
        }
        synchronized (this.mService.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                DisplayContent displayContent = getDisplayContent(displayId);
                if (displayContent != null) {
                    displayContent.onDisplayChanged();
                }
                this.mWmService.mPossibleDisplayInfoMapper.removePossibleDisplayInfos(displayId);
                updateDisplayImePolicyCache();
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateDisplayImePolicyCache() {
        final ArrayMap<Integer, Integer> displayImePolicyMap = new ArrayMap<>();
        forAllDisplays(new Consumer() { // from class: com.android.server.wm.RootWindowContainer$$ExternalSyntheticLambda3
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                displayImePolicyMap.put(Integer.valueOf(r2.getDisplayId()), Integer.valueOf(((DisplayContent) obj).getImePolicy()));
            }
        });
        this.mWmService.mDisplayImePolicyCache = Collections.unmodifiableMap(displayImePolicyMap);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateUIDsPresentOnDisplay() {
        this.mDisplayAccessUIDs.clear();
        for (int displayNdx = getChildCount() - 1; displayNdx >= 0; displayNdx--) {
            DisplayContent displayContent = (DisplayContent) getChildAt(displayNdx);
            if (displayContent.isPrivate()) {
                this.mDisplayAccessUIDs.append(displayContent.mDisplayId, displayContent.getPresentUIDs());
            }
        }
        this.mDisplayManagerInternal.setDisplayAccessUIDs(this.mDisplayAccessUIDs);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void prepareForShutdown() {
        for (int i = 0; i < getChildCount(); i++) {
            createSleepToken("shutdown", ((DisplayContent) getChildAt(i)).mDisplayId);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public SleepToken createSleepToken(String tag, int displayId) {
        DisplayContent display = getDisplayContent(displayId);
        if (display == null) {
            throw new IllegalArgumentException("Invalid display: " + displayId);
        }
        int tokenKey = makeSleepTokenKey(tag, displayId);
        SleepToken token = this.mSleepTokens.get(tokenKey);
        if (token == null) {
            SleepToken token2 = new SleepToken(tag, displayId);
            this.mSleepTokens.put(tokenKey, token2);
            display.mAllSleepTokens.add(token2);
            if (ProtoLogCache.WM_DEBUG_STATES_enabled) {
                String protoLogParam0 = String.valueOf(tag);
                long protoLogParam1 = displayId;
                ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_STATES, -317761482, 4, (String) null, new Object[]{protoLogParam0, Long.valueOf(protoLogParam1)});
            }
            return token2;
        }
        throw new RuntimeException("Create the same sleep token twice: " + token);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void removeSleepToken(SleepToken token) {
        if (!this.mSleepTokens.contains(token.mHashKey)) {
            Slog.d("WindowManager", "Remove non-exist sleep token: " + token + " from " + Debug.getCallers(6));
        }
        this.mSleepTokens.remove(token.mHashKey);
        DisplayContent display = getDisplayContent(token.mDisplayId);
        if (display == null) {
            Slog.d("WindowManager", "Remove sleep token for non-existing display: " + token + " from " + Debug.getCallers(6));
            return;
        }
        if (ProtoLogCache.WM_DEBUG_STATES_enabled) {
            String protoLogParam0 = String.valueOf(token.mTag);
            long protoLogParam1 = token.mDisplayId;
            ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_STATES, -436553282, 4, (String) null, new Object[]{protoLogParam0, Long.valueOf(protoLogParam1)});
        }
        display.mAllSleepTokens.remove(token);
        if (display.mAllSleepTokens.isEmpty()) {
            this.mService.updateSleepIfNeededLocked();
            if ((!this.mTaskSupervisor.getKeyguardController().isDisplayOccluded(display.mDisplayId) && token.mTag.equals("keyguard")) || token.mTag.equals(DISPLAY_OFF_SLEEP_TOKEN_TAG)) {
                display.mSkipAppTransitionAnimation = true;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void addStartingWindowsForVisibleActivities() {
        final ArrayList<Task> addedTasks = new ArrayList<>();
        forAllActivities(new Consumer() { // from class: com.android.server.wm.RootWindowContainer$$ExternalSyntheticLambda43
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                RootWindowContainer.lambda$addStartingWindowsForVisibleActivities$25(addedTasks, (ActivityRecord) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$addStartingWindowsForVisibleActivities$25(ArrayList addedTasks, ActivityRecord r) {
        Task task = r.getTask();
        if (r.mVisibleRequested && r.mStartingData == null && !addedTasks.contains(task)) {
            r.showStartingWindow(true);
            addedTasks.add(task);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void invalidateTaskLayers() {
        if (!this.mTaskLayersChanged) {
            this.mTaskLayersChanged = true;
            this.mService.mH.post(this.mRankTaskLayersRunnable);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void rankTaskLayers() {
        if (this.mTaskLayersChanged) {
            this.mTaskLayersChanged = false;
            this.mService.mH.removeCallbacks(this.mRankTaskLayersRunnable);
        }
        this.mTmpTaskLayerRank = 0;
        forAllLeafTasks(new Consumer() { // from class: com.android.server.wm.RootWindowContainer$$ExternalSyntheticLambda30
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                RootWindowContainer.this.m8205xf81ad0cd((Task) obj);
            }
        }, true);
        if (!this.mTaskSupervisor.inActivityVisibilityUpdate()) {
            this.mTaskSupervisor.computeProcessActivityStateBatch();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$rankTaskLayers$27$com-android-server-wm-RootWindowContainer  reason: not valid java name */
    public /* synthetic */ void m8205xf81ad0cd(Task task) {
        int oldRank = task.mLayerRank;
        ActivityRecord r = task.topRunningActivityLocked();
        if (r != null && r.mVisibleRequested) {
            int i = this.mTmpTaskLayerRank + 1;
            this.mTmpTaskLayerRank = i;
            task.mLayerRank = i;
        } else {
            task.mLayerRank = -1;
        }
        if (task.mLayerRank != oldRank) {
            task.forAllActivities(new Consumer() { // from class: com.android.server.wm.RootWindowContainer$$ExternalSyntheticLambda14
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    RootWindowContainer.this.m8204x12d9620c((ActivityRecord) obj);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$rankTaskLayers$26$com-android-server-wm-RootWindowContainer  reason: not valid java name */
    public /* synthetic */ void m8204x12d9620c(ActivityRecord activity) {
        if (activity.hasProcess()) {
            this.mTaskSupervisor.onProcessActivityStateChanged(activity.app, true);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void clearOtherAppTimeTrackers(AppTimeTracker except) {
        PooledConsumer c = PooledLambda.obtainConsumer(new BiConsumer() { // from class: com.android.server.wm.RootWindowContainer$$ExternalSyntheticLambda18
            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                RootWindowContainer.clearOtherAppTimeTrackers((ActivityRecord) obj, (AppTimeTracker) obj2);
            }
        }, PooledLambda.__(ActivityRecord.class), except);
        forAllActivities((Consumer<ActivityRecord>) c);
        c.recycle();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void clearOtherAppTimeTrackers(ActivityRecord r, AppTimeTracker except) {
        if (r.appTimeTracker != except) {
            r.appTimeTracker = null;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void scheduleDestroyAllActivities(String reason) {
        this.mDestroyAllActivitiesReason = reason;
        this.mService.mH.post(this.mDestroyAllActivitiesRunnable);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void destroyActivity(ActivityRecord r) {
        if (r.finishing || !r.isDestroyable()) {
            return;
        }
        if (ActivityTaskManagerDebugConfig.DEBUG_SWITCH) {
            Slog.v(ActivityTaskManagerService.TAG_SWITCH, "Destroying " + r + " in state " + r.getState() + " resumed=" + r.getTask().getTopResumedActivity() + " pausing=" + r.getTask().getTopPausingActivity() + " for reason " + this.mDestroyAllActivitiesReason);
        }
        r.destroyImmediately(this.mDestroyAllActivitiesReason);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean putTasksToSleep(final boolean allowDelay, final boolean shuttingDown) {
        final boolean[] result = {true};
        forAllRootTasks(new Consumer() { // from class: com.android.server.wm.RootWindowContainer$$ExternalSyntheticLambda50
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                RootWindowContainer.lambda$putTasksToSleep$28(allowDelay, result, shuttingDown, (Task) obj);
            }
        });
        return result[0];
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$putTasksToSleep$28(boolean allowDelay, boolean[] result, boolean shuttingDown, Task task) {
        if (allowDelay) {
            result[0] = result[0] & task.goToSleepIfPossible(shuttingDown);
        } else {
            task.ensureActivitiesVisible(null, 0, false);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void handleAppCrash(WindowProcessController app) {
        PooledConsumer c = PooledLambda.obtainConsumer(new BiConsumer() { // from class: com.android.server.wm.RootWindowContainer$$ExternalSyntheticLambda17
            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                RootWindowContainer.handleAppCrash((ActivityRecord) obj, (WindowProcessController) obj2);
            }
        }, PooledLambda.__(ActivityRecord.class), app);
        forAllActivities((Consumer<ActivityRecord>) c);
        c.recycle();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void handleAppCrash(ActivityRecord r, WindowProcessController app) {
        if (r.app != app) {
            return;
        }
        Slog.w("WindowManager", "  Force finishing activity " + r.intent.getComponent().flattenToShortString());
        r.detachFromProcess();
        r.mDisplayContent.requestTransitionAndLegacyPrepare(2, 16);
        r.destroyIfPossible("handleAppCrashed");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActivityRecord findActivity(Intent intent, ActivityInfo info, boolean compareIntentFilters) {
        ComponentName cls = intent.getComponent();
        if (info.targetActivity != null) {
            cls = new ComponentName(info.packageName, info.targetActivity);
        }
        int userId = UserHandle.getUserId(info.applicationInfo.uid);
        PooledPredicate p = PooledLambda.obtainPredicate(new QuintPredicate() { // from class: com.android.server.wm.RootWindowContainer$$ExternalSyntheticLambda58
            public final boolean test(Object obj, Object obj2, Object obj3, Object obj4, Object obj5) {
                boolean matchesActivity;
                matchesActivity = RootWindowContainer.matchesActivity((ActivityRecord) obj, ((Integer) obj2).intValue(), ((Boolean) obj3).booleanValue(), (Intent) obj4, (ComponentName) obj5);
                return matchesActivity;
            }
        }, PooledLambda.__(ActivityRecord.class), Integer.valueOf(userId), Boolean.valueOf(compareIntentFilters), intent, cls);
        ActivityRecord r = getActivity(p);
        p.recycle();
        return r;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static boolean matchesActivity(ActivityRecord r, int userId, boolean compareIntentFilters, Intent intent, ComponentName cls) {
        if (r.canBeTopRunning() && r.mUserId == userId) {
            if (compareIntentFilters) {
                if (r.intent.filterEquals(intent)) {
                    return true;
                }
            } else if (r.mActivityComponent.equals(cls)) {
                return true;
            }
            return false;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasAwakeDisplay() {
        for (int displayNdx = getChildCount() - 1; displayNdx >= 0; displayNdx--) {
            DisplayContent display = (DisplayContent) getChildAt(displayNdx);
            if (!display.shouldSleep()) {
                return true;
            }
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Task getOrCreateRootTask(ActivityRecord r, ActivityOptions options, Task candidateTask, boolean onTop) {
        return getOrCreateRootTask(r, options, candidateTask, null, onTop, null, 0);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Task getOrCreateRootTask(ActivityRecord r, ActivityOptions options, Task candidateTask, Task sourceTask, boolean onTop, LaunchParamsController.LaunchParams launchParams, int launchFlags) {
        TaskDisplayArea taskDisplayArea;
        int launchDisplayId;
        DisplayContent displayContent;
        int activityType;
        Task rootTask;
        TaskDisplayArea taskDisplayArea2;
        int candidateTaskId;
        Task candidateRoot;
        if (options != null && (candidateRoot = Task.fromWindowContainerToken(options.getLaunchRootTask())) != null && canLaunchOnDisplay(r, candidateRoot)) {
            return candidateRoot;
        }
        if (options != null && (candidateTaskId = options.getLaunchTaskId()) != -1) {
            options.setLaunchTaskId(-1);
            Task task = anyTaskForId(candidateTaskId, 2, options, onTop);
            options.setLaunchTaskId(candidateTaskId);
            if (canLaunchOnDisplay(r, task)) {
                return task.getRootTask();
            }
        }
        if (launchParams != null && launchParams.mPreferredTaskDisplayArea != null) {
            TaskDisplayArea taskDisplayArea3 = launchParams.mPreferredTaskDisplayArea;
            taskDisplayArea = taskDisplayArea3;
        } else if (options == null) {
            taskDisplayArea = null;
        } else {
            WindowContainerToken daToken = options.getLaunchTaskDisplayArea();
            TaskDisplayArea taskDisplayArea4 = daToken != null ? (TaskDisplayArea) WindowContainer.fromBinder(daToken.asBinder()) : null;
            if (taskDisplayArea4 == null && (launchDisplayId = options.getLaunchDisplayId()) != -1 && (displayContent = getDisplayContent(launchDisplayId)) != null) {
                TaskDisplayArea taskDisplayArea5 = getPreferMultiDisplayArea(candidateTask);
                if (taskDisplayArea5 != null) {
                    taskDisplayArea = taskDisplayArea5;
                } else {
                    TaskDisplayArea taskDisplayArea6 = displayContent.getDefaultTaskDisplayArea();
                    taskDisplayArea = taskDisplayArea6;
                }
            } else {
                taskDisplayArea = taskDisplayArea4;
            }
        }
        int activityType2 = resolveActivityType(r, options, candidateTask);
        if (taskDisplayArea != null) {
            if (canLaunchOnDisplay(r, taskDisplayArea.getDisplayId())) {
                return taskDisplayArea.getOrCreateRootTask(r, options, candidateTask, sourceTask, launchParams, launchFlags, activityType2, onTop);
            }
            activityType = activityType2;
            taskDisplayArea = null;
        } else {
            activityType = activityType2;
        }
        Task rootTask2 = null;
        if (candidateTask != null) {
            rootTask2 = candidateTask.getRootTask();
        }
        if (rootTask2 == null && r != null) {
            Task rootTask3 = r.getRootTask();
            rootTask = rootTask3;
        } else {
            rootTask = rootTask2;
        }
        int windowingMode = launchParams != null ? launchParams.mWindowingMode : 0;
        if (rootTask != null) {
            taskDisplayArea = rootTask.getDisplayArea();
            if (taskDisplayArea != null && canLaunchOnDisplay(r, taskDisplayArea.mDisplayContent.mDisplayId)) {
                if (windowingMode == 0) {
                    windowingMode = taskDisplayArea.resolveWindowingMode(r, options, candidateTask);
                }
                if (rootTask.isCompatible(windowingMode, activityType) || rootTask.mCreatedByOrganizer) {
                    return rootTask;
                }
            } else {
                taskDisplayArea = null;
            }
        }
        if (taskDisplayArea != null) {
            taskDisplayArea2 = taskDisplayArea;
        } else {
            TaskDisplayArea taskDisplayArea7 = getDefaultTaskDisplayArea();
            taskDisplayArea2 = taskDisplayArea7;
        }
        return hookGetOrCreateRootTask(taskDisplayArea2, r, options, candidateTask, sourceTask, launchParams, launchFlags, activityType, onTop);
    }

    private boolean canLaunchOnDisplay(ActivityRecord r, Task task) {
        if (task == null) {
            Slog.w("WindowManager", "canLaunchOnDisplay(), invalid task: " + task);
            return false;
        } else if (!task.isAttached()) {
            Slog.w("WindowManager", "canLaunchOnDisplay(), Task is not attached: " + task);
            return false;
        } else {
            return canLaunchOnDisplay(r, task.getTaskDisplayArea().getDisplayId());
        }
    }

    private boolean canLaunchOnDisplay(ActivityRecord r, int displayId) {
        if (r == null || r.canBeLaunchedOnDisplay(displayId)) {
            return true;
        }
        Slog.w("WindowManager", "Not allow to launch " + r + " on display " + displayId);
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int resolveActivityType(ActivityRecord r, ActivityOptions options, Task task) {
        int activityType = r != null ? r.getActivityType() : 0;
        if (activityType == 0 && task != null) {
            activityType = task.getActivityType();
        }
        if (activityType != 0) {
            return activityType;
        }
        if (options != null) {
            activityType = options.getLaunchActivityType();
        }
        if (activityType != 0) {
            return activityType;
        }
        return 1;
    }

    /* JADX DEBUG: Multi-variable search result rejected for r7v0, resolved type: com.android.server.wm.Task */
    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: Multi-variable type inference failed */
    public Task getNextFocusableRootTask(Task currentFocus, boolean ignoreCurrent) {
        Task nextFocusableRootTask;
        TaskDisplayArea preferredDisplayArea = currentFocus.getDisplayArea();
        if (preferredDisplayArea == null) {
            preferredDisplayArea = getDisplayContent(currentFocus.mPrevDisplayId).getDefaultTaskDisplayArea();
        }
        Task preferredFocusableRootTask = preferredDisplayArea.getNextFocusableRootTask(currentFocus, ignoreCurrent);
        if (preferredFocusableRootTask != null) {
            return preferredFocusableRootTask;
        }
        if (preferredDisplayArea.mDisplayContent.supportsSystemDecorations()) {
            return null;
        }
        for (int i = getChildCount() - 1; i >= 0; i--) {
            DisplayContent display = (DisplayContent) getChildAt(i);
            if (display != preferredDisplayArea.mDisplayContent && (nextFocusableRootTask = display.getDefaultTaskDisplayArea().getNextFocusableRootTask(currentFocus, ignoreCurrent)) != null) {
                return nextFocusableRootTask;
            }
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void closeSystemDialogActivities(final String reason) {
        forAllActivities(new Consumer() { // from class: com.android.server.wm.RootWindowContainer$$ExternalSyntheticLambda39
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                RootWindowContainer.this.m8189x36e3fd40(reason, (ActivityRecord) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$closeSystemDialogActivities$29$com-android-server-wm-RootWindowContainer  reason: not valid java name */
    public /* synthetic */ void m8189x36e3fd40(String reason, ActivityRecord r) {
        if ((r.info.flags & 256) != 0 || shouldCloseAssistant(r, reason)) {
            r.finishIfPossible(reason, true);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasVisibleWindowAboveButDoesNotOwnNotificationShade(final int uid) {
        final boolean[] visibleWindowFound = {false};
        return forAllWindows(new ToBooleanFunction() { // from class: com.android.server.wm.RootWindowContainer$$ExternalSyntheticLambda0
            public final boolean apply(Object obj) {
                return RootWindowContainer.lambda$hasVisibleWindowAboveButDoesNotOwnNotificationShade$30(uid, visibleWindowFound, (WindowState) obj);
            }
        }, true);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$hasVisibleWindowAboveButDoesNotOwnNotificationShade$30(int uid, boolean[] visibleWindowFound, WindowState w) {
        if (w.mOwnerUid == uid && w.isVisible()) {
            visibleWindowFound[0] = true;
        }
        if (w.mAttrs.type == 2040) {
            return visibleWindowFound[0] && w.mOwnerUid != uid;
        }
        return false;
    }

    private boolean shouldCloseAssistant(ActivityRecord r, String reason) {
        if (r.isActivityTypeAssistant() && reason != PhoneWindowManager.SYSTEM_DIALOG_REASON_ASSIST) {
            return this.mWmService.mAssistantOnTopOfDream;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public class FinishDisabledPackageActivitiesHelper implements Predicate<ActivityRecord> {
        private final ArrayList<ActivityRecord> mCollectedActivities = new ArrayList<>();
        private boolean mDoit;
        private boolean mEvenPersistent;
        private Set<String> mFilterByClasses;
        private Task mLastTask;
        private boolean mOnlyRemoveNoProcess;
        private String mPackageName;
        private int mUserId;

        FinishDisabledPackageActivitiesHelper() {
        }

        private void reset(String packageName, Set<String> filterByClasses, boolean doit, boolean evenPersistent, int userId, boolean onlyRemoveNoProcess) {
            this.mPackageName = packageName;
            this.mFilterByClasses = filterByClasses;
            this.mDoit = doit;
            this.mEvenPersistent = evenPersistent;
            this.mUserId = userId;
            this.mOnlyRemoveNoProcess = onlyRemoveNoProcess;
            this.mLastTask = null;
        }

        boolean process(String packageName, Set<String> filterByClasses, boolean doit, boolean evenPersistent, int userId, boolean onlyRemoveNoProcess) {
            reset(packageName, filterByClasses, doit, evenPersistent, userId, onlyRemoveNoProcess);
            RootWindowContainer.this.forAllActivities(this);
            boolean didSomething = false;
            int size = this.mCollectedActivities.size();
            for (int i = 0; i < size; i++) {
                ActivityRecord r = this.mCollectedActivities.get(i);
                if (this.mOnlyRemoveNoProcess) {
                    if (!r.hasProcess()) {
                        didSomething = true;
                        Slog.i("WindowManager", "  Force removing " + r);
                        r.cleanUp(false, false);
                        r.removeFromHistory("force-stop");
                    }
                } else {
                    didSomething = true;
                    Slog.i("WindowManager", "  Force finishing " + r);
                    r.finishIfPossible("force-stop", true);
                }
            }
            this.mCollectedActivities.clear();
            return didSomething;
        }

        /* JADX DEBUG: Method merged with bridge method */
        @Override // java.util.function.Predicate
        public boolean test(ActivityRecord r) {
            Set<String> set;
            boolean sameComponent = (r.packageName.equals(this.mPackageName) && ((set = this.mFilterByClasses) == null || set.contains(r.mActivityComponent.getClassName()))) || (this.mPackageName == null && r.mUserId == this.mUserId);
            boolean noProcess = !r.hasProcess();
            if ((this.mUserId == -1 || r.mUserId == this.mUserId) && ((sameComponent || r.getTask() == this.mLastTask) && (noProcess || this.mEvenPersistent || !r.app.isPersistent()))) {
                if (!this.mDoit) {
                    return !r.finishing;
                }
                this.mCollectedActivities.add(r);
                this.mLastTask = r.getTask();
            }
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean finishDisabledPackageActivities(String packageName, Set<String> filterByClasses, boolean doit, boolean evenPersistent, int userId, boolean onlyRemoveNoProcess) {
        return this.mFinishDisabledPackageActivitiesHelper.process(packageName, filterByClasses, doit, evenPersistent, userId, onlyRemoveNoProcess);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateActivityApplicationInfo(ApplicationInfo aInfo) {
        String packageName = aInfo.packageName;
        int userId = UserHandle.getUserId(aInfo.uid);
        PooledConsumer c = PooledLambda.obtainConsumer(new QuadConsumer() { // from class: com.android.server.wm.RootWindowContainer$$ExternalSyntheticLambda5
            public final void accept(Object obj, Object obj2, Object obj3, Object obj4) {
                RootWindowContainer.updateActivityApplicationInfo((ActivityRecord) obj, (ApplicationInfo) obj2, ((Integer) obj3).intValue(), (String) obj4);
            }
        }, PooledLambda.__(ActivityRecord.class), aInfo, Integer.valueOf(userId), packageName);
        forAllActivities((Consumer<ActivityRecord>) c);
        c.recycle();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void updateActivityApplicationInfo(ActivityRecord r, ApplicationInfo aInfo, int userId, String packageName) {
        if (r.mUserId == userId && packageName.equals(r.packageName)) {
            r.updateApplicationInfo(aInfo);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void finishVoiceTask(final IVoiceInteractionSession session) {
        forAllRootTasks(new Consumer() { // from class: com.android.server.wm.RootWindowContainer$$ExternalSyntheticLambda19
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ((Task) obj).finishVoiceTask(session);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void removeRootTasksInWindowingModes(int... windowingModes) {
        for (int i = getChildCount() - 1; i >= 0; i--) {
            ((DisplayContent) getChildAt(i)).removeRootTasksInWindowingModes(windowingModes);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void removeRootTasksWithActivityTypes(int... activityTypes) {
        for (int i = getChildCount() - 1; i >= 0; i--) {
            ((DisplayContent) getChildAt(i)).removeRootTasksWithActivityTypes(activityTypes);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActivityRecord topRunningActivity() {
        for (int i = getChildCount() - 1; i >= 0; i--) {
            ActivityRecord topActivity = ((DisplayContent) getChildAt(i)).topRunningActivity();
            if (topActivity != null) {
                return topActivity;
            }
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean allResumedActivitiesIdle() {
        Task rootTask;
        ActivityRecord resumedActivity;
        for (int displayNdx = getChildCount() - 1; displayNdx >= 0; displayNdx--) {
            DisplayContent display = (DisplayContent) getChildAt(displayNdx);
            if (!display.isSleeping() && (rootTask = display.getFocusedRootTask()) != null && rootTask.hasActivity() && ((resumedActivity = rootTask.getTopResumedActivity()) == null || !resumedActivity.idle)) {
                if (ProtoLogCache.WM_DEBUG_STATES_enabled) {
                    long protoLogParam0 = rootTask.getRootTaskId();
                    String protoLogParam1 = String.valueOf(resumedActivity);
                    ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_STATES, -938271693, 1, (String) null, new Object[]{Long.valueOf(protoLogParam0), protoLogParam1});
                }
                return false;
            }
        }
        this.mService.endLaunchPowerMode(1);
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean allResumedActivitiesVisible() {
        final boolean[] foundResumed = {false};
        boolean foundInvisibleResumedActivity = forAllRootTasks(new Predicate() { // from class: com.android.server.wm.RootWindowContainer$$ExternalSyntheticLambda38
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return RootWindowContainer.lambda$allResumedActivitiesVisible$32(foundResumed, (Task) obj);
            }
        });
        if (foundInvisibleResumedActivity) {
            return false;
        }
        return foundResumed[0];
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$allResumedActivitiesVisible$32(boolean[] foundResumed, Task rootTask) {
        ActivityRecord r = rootTask.getTopResumedActivity();
        if (r != null) {
            if (!r.nowVisible) {
                return true;
            }
            foundResumed[0] = true;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean allPausedActivitiesComplete() {
        final boolean[] pausing = {true};
        boolean hasActivityNotCompleted = forAllLeafTasks(new Predicate() { // from class: com.android.server.wm.RootWindowContainer$$ExternalSyntheticLambda42
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return RootWindowContainer.lambda$allPausedActivitiesComplete$33(pausing, (Task) obj);
            }
        });
        if (hasActivityNotCompleted) {
            return false;
        }
        return pausing[0];
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$allPausedActivitiesComplete$33(boolean[] pausing, Task task) {
        ActivityRecord r = task.getTopPausingActivity();
        if (r != null && !r.isState(ActivityRecord.State.PAUSED, ActivityRecord.State.STOPPED, ActivityRecord.State.STOPPING, ActivityRecord.State.FINISHING)) {
            if (ProtoLogCache.WM_DEBUG_STATES_enabled) {
                String protoLogParam0 = String.valueOf(r);
                String protoLogParam1 = String.valueOf(r.getState());
                ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_STATES, 895158150, 0, (String) null, new Object[]{protoLogParam0, protoLogParam1});
            }
            if (!ProtoLogGroup.WM_DEBUG_STATES.isEnabled()) {
                return true;
            }
            pausing[0] = false;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void lockAllProfileTasks(final int userId) {
        forAllLeafTasks(new Consumer() { // from class: com.android.server.wm.RootWindowContainer$$ExternalSyntheticLambda6
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                RootWindowContainer.this.m8202x1d4427bc(userId, (Task) obj);
            }
        }, true);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$lockAllProfileTasks$35$com-android-server-wm-RootWindowContainer  reason: not valid java name */
    public /* synthetic */ void m8202x1d4427bc(final int userId, Task task) {
        ActivityRecord top = task.topRunningActivity();
        if ((top == null || top.finishing || !"android.app.action.CONFIRM_DEVICE_CREDENTIAL_WITH_USER".equals(top.intent.getAction()) || !top.packageName.equals(this.mService.getSysUiServiceComponentLocked().getPackageName())) && task.getActivity(new Predicate() { // from class: com.android.server.wm.RootWindowContainer$$ExternalSyntheticLambda33
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return RootWindowContainer.lambda$lockAllProfileTasks$34(userId, (ActivityRecord) obj);
            }
        }) != null) {
            this.mService.getTaskChangeNotificationController().notifyTaskProfileLocked(task.getTaskInfo());
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$lockAllProfileTasks$34(int userId, ActivityRecord activity) {
        return !activity.finishing && activity.mUserId == userId;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Task anyTaskForId(int id) {
        return anyTaskForId(id, 2);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Task anyTaskForId(int id, int matchMode) {
        return anyTaskForId(id, matchMode, null, false);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Task anyTaskForId(int id, int matchMode, ActivityOptions aOptions, boolean onTop) {
        Task targetRootTask;
        int i = 2;
        if (matchMode != 2 && aOptions != null) {
            throw new IllegalArgumentException("Should not specify activity options for non-restore lookup");
        }
        PooledPredicate p = PooledLambda.obtainPredicate(new AppTransition$$ExternalSyntheticLambda2(), PooledLambda.__(Task.class), Integer.valueOf(id));
        Task task = getTask(p);
        p.recycle();
        if (task != null) {
            if (aOptions != null && (targetRootTask = getOrCreateRootTask(null, aOptions, task, onTop)) != null && task.getRootTask() != targetRootTask) {
                if (onTop) {
                    i = 0;
                }
                int reparentMode = i;
                task.reparent(targetRootTask, onTop, reparentMode, true, true, "anyTaskForId");
            }
            return task;
        } else if (matchMode == 0) {
            return null;
        } else {
            if (ActivityTaskManagerDebugConfig.DEBUG_RECENTS) {
                Slog.v(TAG_RECENTS, "Looking for task id=" + id + " in recents");
            }
            Task task2 = this.mTaskSupervisor.mRecentTasks.getTask(id);
            if (task2 == null) {
                if (ActivityTaskManagerDebugConfig.DEBUG_RECENTS) {
                    Slog.d(TAG_RECENTS, "\tDidn't find task id=" + id + " in recents");
                }
                return null;
            } else if (matchMode == 1) {
                return task2;
            } else {
                if (!this.mTaskSupervisor.restoreRecentTaskLocked(task2, aOptions, onTop)) {
                    if (ActivityTaskManagerDebugConfig.DEBUG_RECENTS) {
                        Slog.w(TAG_RECENTS, "Couldn't restore task id=" + id + " found in recents");
                    }
                    return null;
                }
                if (ActivityTaskManagerDebugConfig.DEBUG_RECENTS) {
                    Slog.w(TAG_RECENTS, "Restored task id=" + id + " from in recents");
                }
                return task2;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void getRunningTasks(int maxNum, List<ActivityManager.RunningTaskInfo> list, int flags, int callingUid, ArraySet<Integer> profileIds) {
        this.mTaskSupervisor.getRunningTasks().getTasks(maxNum, list, flags, this, callingUid, profileIds);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void startPowerModeLaunchIfNeeded(boolean forceSend, final ActivityRecord targetActivity) {
        ActivityOptions opts;
        if (!forceSend && targetActivity != null && targetActivity.app != null) {
            final boolean[] noResumedActivities = {true};
            final boolean[] allFocusedProcessesDiffer = {true};
            forAllTaskDisplayAreas(new Consumer() { // from class: com.android.server.wm.RootWindowContainer$$ExternalSyntheticLambda25
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    RootWindowContainer.lambda$startPowerModeLaunchIfNeeded$36(noResumedActivities, allFocusedProcessesDiffer, targetActivity, (TaskDisplayArea) obj);
                }
            });
            if (!noResumedActivities[0] && !allFocusedProcessesDiffer[0]) {
                return;
            }
        }
        int reason = 1;
        boolean isKeyguardLocked = targetActivity != null ? targetActivity.isKeyguardLocked() : this.mDefaultDisplay.isKeyguardLocked();
        if (isKeyguardLocked && targetActivity != null && !targetActivity.isLaunchSourceType(3) && ((opts = targetActivity.getOptions()) == null || opts.getSourceInfo() == null || opts.getSourceInfo().type != 3)) {
            reason = 1 | 4;
        }
        this.mService.startLaunchPowerMode(reason);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$startPowerModeLaunchIfNeeded$36(boolean[] noResumedActivities, boolean[] allFocusedProcessesDiffer, ActivityRecord targetActivity, TaskDisplayArea taskDisplayArea) {
        ActivityRecord resumedActivity = taskDisplayArea.getFocusedActivity();
        WindowProcessController resumedActivityProcess = resumedActivity == null ? null : resumedActivity.app;
        noResumedActivities[0] = noResumedActivities[0] & (resumedActivityProcess == null);
        if (resumedActivityProcess != null) {
            allFocusedProcessesDiffer[0] = allFocusedProcessesDiffer[0] & (true ^ resumedActivityProcess.equals(targetActivity.app));
        }
    }

    public int getTaskToShowPermissionDialogOn(final String pkgName, final int uid) {
        final PermissionPolicyInternal pPi = this.mService.getPermissionPolicyInternal();
        if (pPi == null) {
            return -1;
        }
        final int[] validTaskId = {-1};
        forAllLeafTaskFragments(new Predicate() { // from class: com.android.server.wm.RootWindowContainer$$ExternalSyntheticLambda10
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return RootWindowContainer.lambda$getTaskToShowPermissionDialogOn$38(PermissionPolicyInternal.this, uid, pkgName, validTaskId, (TaskFragment) obj);
            }
        });
        return validTaskId[0];
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$getTaskToShowPermissionDialogOn$38(final PermissionPolicyInternal pPi, int uid, String pkgName, int[] validTaskId, TaskFragment fragment) {
        ActivityRecord record = fragment.getActivity(new Predicate() { // from class: com.android.server.wm.RootWindowContainer$$ExternalSyntheticLambda24
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return RootWindowContainer.lambda$getTaskToShowPermissionDialogOn$37(PermissionPolicyInternal.this, (ActivityRecord) obj);
            }
        });
        if (record == null || !record.isUid(uid) || !Objects.equals(pkgName, record.packageName) || !pPi.shouldShowNotificationDialogForTask(record.getTask().getTaskInfo(), pkgName, record.launchedFromPackage, record.intent, record.getName())) {
            return false;
        }
        validTaskId[0] = record.getTask().mTaskId;
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$getTaskToShowPermissionDialogOn$37(PermissionPolicyInternal pPi, ActivityRecord r) {
        return r.canBeTopRunning() && r.isVisibleRequested() && !pPi.isIntentToPermissionDialog(r.intent);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ArrayList<ActivityRecord> getDumpActivities(final String name, final boolean dumpVisibleRootTasksOnly, boolean dumpFocusedRootTaskOnly, final int userId) {
        final int recentsComponentUid;
        if (dumpFocusedRootTaskOnly) {
            Task topFocusedRootTask = getTopDisplayFocusedRootTask();
            if (topFocusedRootTask != null) {
                return topFocusedRootTask.getDumpActivitiesLocked(name, userId);
            }
            return new ArrayList<>();
        }
        RecentTasks recentTasks = this.mWindowManager.mAtmService.getRecentTasks();
        if (recentTasks != null) {
            recentsComponentUid = recentTasks.getRecentsComponentUid();
        } else {
            recentsComponentUid = -1;
        }
        final ArrayList<ActivityRecord> activities = new ArrayList<>();
        forAllLeafTasks(new Predicate() { // from class: com.android.server.wm.RootWindowContainer$$ExternalSyntheticLambda32
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return RootWindowContainer.lambda$getDumpActivities$39(recentsComponentUid, dumpVisibleRootTasksOnly, activities, name, userId, (Task) obj);
            }
        });
        return activities;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$getDumpActivities$39(int recentsComponentUid, boolean dumpVisibleRootTasksOnly, ArrayList activities, String name, int userId, Task task) {
        boolean isRecents = task.effectiveUid == recentsComponentUid;
        if (!dumpVisibleRootTasksOnly || task.shouldBeVisible(null) || isRecents) {
            activities.addAll(task.getDumpActivitiesLocked(name, userId));
        }
        return false;
    }

    @Override // com.android.server.wm.WindowContainer
    public void dump(PrintWriter pw, String prefix, boolean dumpAll) {
        super.dump(pw, prefix, dumpAll);
        pw.print(prefix);
        pw.println("topDisplayFocusedRootTask=" + getTopDisplayFocusedRootTask());
        for (int i = getChildCount() - 1; i >= 0; i--) {
            DisplayContent display = (DisplayContent) getChildAt(i);
            display.dump(pw, prefix, dumpAll);
        }
        pw.println();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dumpDisplayConfigs(PrintWriter pw, String prefix) {
        pw.print(prefix);
        pw.println("Display override configurations:");
        int displayCount = getChildCount();
        for (int i = 0; i < displayCount; i++) {
            DisplayContent displayContent = (DisplayContent) getChildAt(i);
            pw.print(prefix);
            pw.print("  ");
            pw.print(displayContent.mDisplayId);
            pw.print(": ");
            pw.println(displayContent.getRequestedOverrideConfiguration());
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean dumpActivities(final FileDescriptor fd, final PrintWriter pw, final boolean dumpAll, final boolean dumpClient, final String dumpPackage) {
        final boolean[] printed = {false};
        final boolean[] needSep = {false};
        for (int displayNdx = getChildCount() - 1; displayNdx >= 0; displayNdx--) {
            DisplayContent displayContent = (DisplayContent) getChildAt(displayNdx);
            if (printed[0]) {
                pw.println();
            }
            pw.print("Display #");
            pw.print(displayContent.mDisplayId);
            pw.println(" (activities from top to bottom):");
            displayContent.forAllRootTasks(new Consumer() { // from class: com.android.server.wm.RootWindowContainer$$ExternalSyntheticLambda26
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    RootWindowContainer.lambda$dumpActivities$40(needSep, pw, fd, dumpAll, dumpClient, dumpPackage, printed, (Task) obj);
                }
            });
            displayContent.forAllTaskDisplayAreas(new Consumer() { // from class: com.android.server.wm.RootWindowContainer$$ExternalSyntheticLambda27
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    RootWindowContainer.lambda$dumpActivities$42(printed, pw, dumpPackage, needSep, (TaskDisplayArea) obj);
                }
            });
        }
        printed[0] = printed[0] | ActivityTaskSupervisor.dumpHistoryList(fd, pw, this.mTaskSupervisor.mFinishingActivities, "  ", "Fin", false, !dumpAll, false, dumpPackage, true, new Runnable() { // from class: com.android.server.wm.RootWindowContainer$$ExternalSyntheticLambda28
            @Override // java.lang.Runnable
            public final void run() {
                pw.println("  Activities waiting to finish:");
            }
        }, null);
        printed[0] = printed[0] | ActivityTaskSupervisor.dumpHistoryList(fd, pw, this.mTaskSupervisor.mStoppingActivities, "  ", "Stop", false, !dumpAll, false, dumpPackage, true, new Runnable() { // from class: com.android.server.wm.RootWindowContainer$$ExternalSyntheticLambda29
            @Override // java.lang.Runnable
            public final void run() {
                pw.println("  Activities waiting to stop:");
            }
        }, null);
        return printed[0];
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$dumpActivities$40(boolean[] needSep, PrintWriter pw, FileDescriptor fd, boolean dumpAll, boolean dumpClient, String dumpPackage, boolean[] printed, Task rootTask) {
        if (needSep[0]) {
            pw.println();
        }
        needSep[0] = rootTask.dump(fd, pw, dumpAll, dumpClient, dumpPackage, false);
        printed[0] = printed[0] | needSep[0];
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$dumpActivities$42(boolean[] printed, final PrintWriter pw, String dumpPackage, boolean[] needSep, TaskDisplayArea taskDisplayArea) {
        printed[0] = printed[0] | ActivityTaskSupervisor.printThisActivity(pw, taskDisplayArea.getFocusedActivity(), dumpPackage, needSep[0], "    Resumed: ", new Runnable() { // from class: com.android.server.wm.RootWindowContainer$$ExternalSyntheticLambda51
            @Override // java.lang.Runnable
            public final void run() {
                pw.println("  Resumed activities in task display areas (from top to bottom):");
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static int makeSleepTokenKey(String tag, int displayId) {
        String tokenKey = tag + displayId;
        return tokenKey.hashCode();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public static final class SleepToken {
        private final long mAcquireTime = SystemClock.uptimeMillis();
        private final int mDisplayId;
        final int mHashKey;
        private final String mTag;

        SleepToken(String tag, int displayId) {
            this.mTag = tag;
            this.mDisplayId = displayId;
            this.mHashKey = RootWindowContainer.makeSleepTokenKey(tag, displayId);
        }

        public String toString() {
            return "{\"" + this.mTag + "\", display " + this.mDisplayId + ", acquire at " + TimeUtils.formatUptime(this.mAcquireTime) + "}";
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public void writeTagToProto(ProtoOutputStream proto, long fieldId) {
            proto.write(fieldId, this.mTag);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class RankTaskLayersRunnable implements Runnable {
        private RankTaskLayersRunnable() {
        }

        @Override // java.lang.Runnable
        public void run() {
            synchronized (RootWindowContainer.this.mService.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    if (RootWindowContainer.this.mTaskLayersChanged) {
                        RootWindowContainer.this.mTaskLayersChanged = false;
                        RootWindowContainer.this.rankTaskLayers();
                    }
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class AttachApplicationHelper implements Consumer<Task>, Predicate<ActivityRecord> {
        private WindowProcessController mApp;
        private boolean mHasActivityStarted;
        private RemoteException mRemoteException;
        private ActivityRecord mTop;

        private AttachApplicationHelper() {
        }

        void reset() {
            this.mHasActivityStarted = false;
            this.mRemoteException = null;
            this.mApp = null;
            this.mTop = null;
        }

        boolean process(WindowProcessController app) throws RemoteException {
            this.mApp = app;
            for (int displayNdx = RootWindowContainer.this.getChildCount() - 1; displayNdx >= 0; displayNdx--) {
                ((DisplayContent) RootWindowContainer.this.getChildAt(displayNdx)).forAllRootTasks((Consumer<Task>) this);
                RemoteException remoteException = this.mRemoteException;
                if (remoteException != null) {
                    throw remoteException;
                }
            }
            if (!this.mHasActivityStarted) {
                RootWindowContainer.this.ensureActivitiesVisible(null, 0, false);
            }
            return this.mHasActivityStarted;
        }

        /* JADX DEBUG: Method merged with bridge method */
        @Override // java.util.function.Consumer
        public void accept(Task rootTask) {
            if (this.mRemoteException != null || rootTask.getVisibility(null) == 2) {
                return;
            }
            this.mTop = rootTask.topRunningActivity();
            rootTask.forAllActivities((Predicate<ActivityRecord>) this);
        }

        /* JADX DEBUG: Method merged with bridge method */
        @Override // java.util.function.Predicate
        public boolean test(ActivityRecord r) {
            if (!r.finishing && r.showToCurrentUser() && r.visibleIgnoringKeyguard && r.app == null && this.mApp.mUid == r.info.applicationInfo.uid && this.mApp.mName.equals(r.processName)) {
                try {
                    if (RootWindowContainer.this.mTaskSupervisor.realStartActivityLocked(r, this.mApp, this.mTop == r && r.getTask().canBeResumed(r), true)) {
                        this.mHasActivityStarted = true;
                    }
                    return false;
                } catch (RemoteException e) {
                    Slog.w("WindowManager", "Exception in new application when starting activity " + this.mTop, e);
                    this.mRemoteException = e;
                    return true;
                }
            }
            return false;
        }
    }

    public boolean isInPreferMultiDisplayArea(String pkgName) {
        synchronized (this.mService.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                TaskDisplayArea tda = getDefaultDisplay().getPreferDisplayArea(pkgName);
                if (tda != null && tda.isMultiWindow()) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return true;
                }
                WindowManagerService.resetPriorityAfterLockedSection();
                return false;
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public TaskDisplayArea getPreferMultiTaskDisplayArea() {
        TaskDisplayArea taskDisplayArea;
        synchronized (this.mService.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                taskDisplayArea = this.mPreferMultiTaskDisplayArea;
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
        return taskDisplayArea;
    }

    public void resetPreferMultiTaskDisplayArea() {
        synchronized (this.mService.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                this.mPreferMultiTaskDisplayArea = null;
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    public void setPreferMultiTaskDisplayArea(String pkgName) {
        synchronized (this.mService.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                TaskDisplayArea tda = getPreferMultiDisplayArea(pkgName);
                if (tda != null && tda.isMultiWindow()) {
                    this.mPreferMultiTaskDisplayArea = tda;
                } else {
                    this.mPreferMultiTaskDisplayArea = null;
                }
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    public void setPreferMultiTaskDisplayArea(int multiId) {
        synchronized (this.mService.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                TaskDisplayArea tda = getPreferMultiDisplayArea(multiId);
                if (tda != null && tda.isMultiWindow()) {
                    this.mPreferMultiTaskDisplayArea = tda;
                } else {
                    this.mPreferMultiTaskDisplayArea = null;
                }
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    public void setMultiWindowBlackListToSystem(List<String> list) {
        this.mMultiWindowBlackList = list;
    }

    public void setMultiWindowWhiteListToSystem(List<String> list) {
        this.mMultiWindowWhiteList = list;
    }

    public void setMultiWindowConfigToSystem(String key, List<String> list) {
        ITranMultiWindow.Instance().setMultiWindowConfigToSystem(key, list);
    }

    public TaskDisplayArea getPreferMultiDisplayAreaByAr(ActivityRecord r) {
        return getDefaultDisplay().getPreferMultiDisplayArea(r);
    }

    public TaskDisplayArea getPreferMultiDisplayArea(String pkgName) {
        return getDefaultDisplay().getPreferMultiDisplayArea(pkgName);
    }

    public TaskDisplayArea getPreferMultiDisplayArea(int id) {
        if (id == -1) {
            Slog.w("WindowManager", "getPreferMultiDisplayArea id is illegal!");
            return null;
        }
        return getDefaultDisplay().getPreferMultiDisplayArea(id);
    }

    public TaskDisplayArea getPreferMultiDisplayArea(Task task) {
        if (task == null) {
            Slog.w("WindowManager", "getPreferMultiDisplayArea task is null!");
            return null;
        } else if (task.getConfiguration() == null || task.getConfiguration().windowConfiguration == null || !task.getConfiguration().windowConfiguration.isThunderbackWindow()) {
            return null;
        } else {
            return task.getDisplayArea();
        }
    }

    public TaskDisplayArea getMultiDisplayArea() {
        if (ThunderbackConfig.isVersion4()) {
            return getDefaultDisplay().getTopMultiDisplayArea();
        }
        return getDefaultDisplay().getMultiDisplayArea();
    }

    public TaskDisplayArea getMultiDisplayArea(int multiWindowMode, int multiWindowId) {
        return getDefaultDisplay().getMultiDisplayArea(multiWindowMode, multiWindowId);
    }

    public TaskDisplayArea getPerferTaskDisplayAreaFromPackageReady() {
        return getPreferMultiTaskDisplayArea();
    }

    public List<String> getTopPackages() {
        return getDefaultDisplay().getTopPackages();
    }

    public void hookShowMultiDisplayWindow(final Task task) {
        ActivityManager.RunningTaskInfo taskInfo = task.getTaskInfo();
        String packageName = null;
        if (ThunderbackConfig.isVersion4()) {
            packageName = task.getTopNonFinishingActivity() != null ? task.getTopNonFinishingActivity().packageName : null;
            if (packageName == null) {
                Slog.i(TAG_MULTI, "top task packagename is null , so not support");
                ITranRootWindowContainer.Instance().showUnSupportMultiToast();
                return;
            }
        }
        if (taskInfo != null && taskInfo.token != null) {
            if (ThunderbackConfig.isVersion4()) {
                ITranRootWindowContainer.Instance().hookShowMultiDisplayWindowV4(taskInfo.token, taskInfo.taskId, task.getSurfaceControl(), true, 1001, packageName);
                ITranMultiWindow.Instance().postExecute(new Runnable() { // from class: com.android.server.wm.RootWindowContainer$$ExternalSyntheticLambda61
                    @Override // java.lang.Runnable
                    public final void run() {
                        RootWindowContainer.this.m8198xcadb8e43(task);
                    }
                }, 500L);
            } else {
                ITranRootWindowContainer.Instance().hookShowMultiDisplayWindowV3(taskInfo.token, task.getSurfaceControl(), true, true);
                this.mMutliWindowHandler.postDelayed(new Runnable() { // from class: com.android.server.wm.RootWindowContainer$$ExternalSyntheticLambda62
                    @Override // java.lang.Runnable
                    public final void run() {
                        RootWindowContainer.this.m8199xb01cfd04();
                    }
                }, 500L);
            }
            this.mMultiWindowAnimationNeeded = false;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$hookShowMultiDisplayWindow$45$com-android-server-wm-RootWindowContainer  reason: not valid java name */
    public /* synthetic */ void m8198xcadb8e43(Task task) {
        synchronized (this.mService.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                boolean inMultiWindow = task.getConfiguration().windowConfiguration.isThunderbackWindow();
                int mulitWindowId = task.getConfiguration().windowConfiguration.getMultiWindowingId();
                if (inMultiWindow && getPreferMultiDisplayArea(mulitWindowId) != null) {
                    getPreferMultiDisplayArea(mulitWindowId).mAppTransitionReady = true;
                }
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$hookShowMultiDisplayWindow$46$com-android-server-wm-RootWindowContainer  reason: not valid java name */
    public /* synthetic */ void m8199xb01cfd04() {
        synchronized (this.mService.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (getMultiDisplayArea() != null) {
                    getMultiDisplayArea().mAppTransitionReady = true;
                }
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    public void hookShowMultiDisplayWindow() {
        hookShowMultiDisplayWindow(-1);
    }

    public void hookShowMultiDisplayWindow(int startType) {
        getDefaultDisplay();
        TaskDisplayArea defaultDisplayArea = getDefaultTaskDisplayArea();
        final Task topTask = defaultDisplayArea.getTopRootTask();
        if (topTask != null) {
            if (topTask.getActivityType() != 1 || topTask.getWindowingMode() != 1) {
                ITranRootWindowContainer.Instance().showSceneUnSupportMultiToast();
                return;
            }
            ActivityManager.RunningTaskInfo taskInfo = topTask.getTaskInfo();
            String packageName = null;
            if (ThunderbackConfig.isVersion4()) {
                packageName = topTask.realActivity != null ? topTask.realActivity.getPackageName() : null;
                if (packageName == null) {
                    Slog.i(TAG_MULTI, "top task packagename is null , so not support");
                    ITranRootWindowContainer.Instance().showUnSupportMultiToast();
                    return;
                }
            }
            if (taskInfo != null && taskInfo.token != null) {
                if (ThunderbackConfig.isVersion4()) {
                    ITranRootWindowContainer.Instance().hookShowMultiDisplayWindowV4(taskInfo.token, taskInfo.taskId, topTask.getSurfaceControl(), topTask.getDisplayCutoutInsets() == null, startType, packageName);
                    this.mMultiWindowAnimationNeeded = true;
                    ITranMultiWindow.Instance().postExecute(new Runnable() { // from class: com.android.server.wm.RootWindowContainer$$ExternalSyntheticLambda1
                        @Override // java.lang.Runnable
                        public final void run() {
                            RootWindowContainer.this.m8200x955e6bc5(topTask);
                        }
                    }, 500L);
                    return;
                }
                ITranRootWindowContainer.Instance().hookShowMultiDisplayWindowV3(taskInfo.token, topTask.getSurfaceControl(), topTask.getDisplayCutoutInsets() == null, true);
                this.mMultiWindowAnimationNeeded = true;
                this.mMutliWindowHandler.postDelayed(new Runnable() { // from class: com.android.server.wm.RootWindowContainer$$ExternalSyntheticLambda2
                    @Override // java.lang.Runnable
                    public final void run() {
                        RootWindowContainer.this.m8201x7a9fda86();
                    }
                }, 500L);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$hookShowMultiDisplayWindow$47$com-android-server-wm-RootWindowContainer  reason: not valid java name */
    public /* synthetic */ void m8200x955e6bc5(Task topTask) {
        synchronized (this.mService.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                boolean inMultiWindow = topTask.getConfiguration().windowConfiguration.isThunderbackWindow();
                int mulitWindowId = topTask.getConfiguration().windowConfiguration.getMultiWindowingId();
                if (inMultiWindow && getPreferMultiDisplayArea(mulitWindowId) != null) {
                    getPreferMultiDisplayArea(mulitWindowId).mAppTransitionReady = true;
                }
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$hookShowMultiDisplayWindow$48$com-android-server-wm-RootWindowContainer  reason: not valid java name */
    public /* synthetic */ void m8201x7a9fda86() {
        synchronized (this.mService.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (getMultiDisplayArea() != null) {
                    getMultiDisplayArea().mAppTransitionReady = true;
                }
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    public void hookReparentToDefaultDisplay(int multiWindowMode, final int multiWindowId, boolean anim) {
        Task topTask;
        ActivityManager.RunningTaskInfo taskInfo;
        TaskDisplayArea multiDisplayArea = getMultiDisplayArea(multiWindowMode, multiWindowId);
        TaskDisplayArea defaultDisplayArea = getDefaultTaskDisplayArea();
        if (multiDisplayArea != null && (topTask = multiDisplayArea.getTopRootTask()) != null && (taskInfo = topTask.getTaskInfo()) != null && taskInfo.token != null) {
            ITranRootWindowContainer.Instance().hookReparentToDefaultDisplay(multiDisplayArea.getMultiWindowingMode(), multiDisplayArea.getMultiWindowingId(), defaultDisplayArea.mRemoteToken.toWindowContainerToken(), taskInfo.token, anim);
            this.hookToOSRecord = topTask.getResumedActivity();
            ITranMultiWindow.Instance().postExecute(this.runable, 100L);
            Slog.d(TAG_MULTI, "recovery no mute");
            this.mService.setMuteStateV4(false, multiWindowId);
            ITranMultiWindow.Instance().postExecute(new Runnable() { // from class: com.android.server.wm.RootWindowContainer$$ExternalSyntheticLambda54
                @Override // java.lang.Runnable
                public final void run() {
                    RootWindowContainer.this.m8197xe55e8144(multiWindowId);
                }
            }, 0L);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$hookReparentToDefaultDisplay$49$com-android-server-wm-RootWindowContainer  reason: not valid java name */
    public /* synthetic */ void m8197xe55e8144(int multiWindowId) {
        this.mService.mAmInternal.clearMuteProcessList(multiWindowId);
    }

    public void hookMultiWindowToMax(final int multiWindowId) {
        TaskDisplayArea multiDisplayArea;
        Task topTask;
        ActivityManager.RunningTaskInfo taskInfo;
        getPreferMultiDisplayArea(multiWindowId);
        if (ThunderbackConfig.isVersion4()) {
            multiDisplayArea = getPreferMultiDisplayArea(multiWindowId);
        } else {
            multiDisplayArea = getMultiDisplayArea();
        }
        TaskDisplayArea defaultDisplayArea = getDefaultTaskDisplayArea();
        if (multiDisplayArea != null && (topTask = multiDisplayArea.getTopRootTask()) != null && (taskInfo = topTask.getTaskInfo()) != null && taskInfo.token != null) {
            if (ThunderbackConfig.isVersion4()) {
                ITranRootWindowContainer.Instance().hookMultiWindowToMax(multiDisplayArea.getMultiWindowingId(), defaultDisplayArea.mRemoteToken.toWindowContainerToken(), taskInfo.token, topTask.getDisplayCutoutInsets() == null);
            } else {
                ITranRootWindowContainer.Instance().hookMultiWindowToMax(multiDisplayArea.mDisplayAreaAppearedInfo.getTranMultiDisplayAreaId(), defaultDisplayArea.mRemoteToken.toWindowContainerToken(), taskInfo.token, topTask.getDisplayCutoutInsets() == null);
            }
            this.hookToOSRecord = topTask.getResumedActivity();
            if (ThunderbackConfig.isVersion4()) {
                ITranMultiWindow.Instance().postExecute(this.runable, 100L);
            } else {
                this.mMutliWindowHandler.postDelayed(this.runable, 100L);
            }
            Slog.d(TAG_MULTI, "recovery no mute");
            this.mService.setMuteStateV4(false, multiWindowId);
            ITranMultiWindow.Instance().postExecute(new Runnable() { // from class: com.android.server.wm.RootWindowContainer$$ExternalSyntheticLambda11
                @Override // java.lang.Runnable
                public final void run() {
                    RootWindowContainer.this.m8196xe4e3b2b3(multiWindowId);
                }
            }, -1L);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$hookMultiWindowToMax$50$com-android-server-wm-RootWindowContainer  reason: not valid java name */
    public /* synthetic */ void m8196xe4e3b2b3(int multiWindowId) {
        this.mService.mAmInternal.clearMuteProcessList(multiWindowId);
    }

    public void hookMultiWindowToMinV3(int displayAreaId) {
        ITranRootWindowContainer.Instance().hookMultiWindowToMin(displayAreaId);
    }

    public void hookMultiWindowToCloseV3(int displayAreaId) {
        ITranRootWindowContainer.Instance().hookMultiWindowToCloseV3(displayAreaId);
    }

    public void hookMultiWindowToCloseV4(int multiWindowMode, int multiWindowId) {
        ITranRootWindowContainer.Instance().hookMultiWindowToCloseV4(multiWindowMode, multiWindowId);
    }

    public void hookMultiWindowToSmallV3(int displayAreaId) {
        ITranRootWindowContainer.Instance().hookMultiWindowToSmallV3(displayAreaId);
    }

    public void hookMultiWindowToSmallV4(int multiWindowMode, int multiWindowId) {
        ITranRootWindowContainer.Instance().hookMultiWindowToSmallV4(multiWindowMode, multiWindowId);
    }

    public void hookMultiWindowToLargeV3(int displayAreaId) {
        ITranRootWindowContainer.Instance().hookMultiWindowToLarge(displayAreaId);
    }

    public void hookFinishMovingLocationV3(int displayAreaId) {
        ITranRootWindowContainer.Instance().hookFinishMovingLocation(displayAreaId);
    }

    public void hookMultiWindowFlingV3(int displayAreaId, MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        ITranRootWindowContainer.Instance().hookMultiWindowFling(displayAreaId, e1, e2, velocityX, velocityY);
    }

    public void hookMultiWindowToSplit(int multiWindowMode, int multiWindowId) {
        ITranRootWindowContainer.Instance().hookMultiWindowToSplit(multiWindowMode, multiWindowId);
    }

    public void setTranMultiWindowModeV3(int mode) {
        TaskDisplayArea multiDisplayArea = getMultiDisplayArea();
        if (multiDisplayArea != null) {
            multiDisplayArea.setTranMultiWindowMode(mode);
        }
    }

    public boolean activityInMultiWindow(final String pkgName) {
        Task task;
        if (pkgName == null) {
            return false;
        }
        if (ThunderbackConfig.isVersion4()) {
            DisplayContent display = getDefaultDisplay().mDisplayContent;
            display.forAllTaskDisplayAreas(new Predicate() { // from class: com.android.server.wm.RootWindowContainer$$ExternalSyntheticLambda37
                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    return RootWindowContainer.this.m8187xf72cb01c(pkgName, (TaskDisplayArea) obj);
                }
            });
            if (this.mTmpDisplayArea != null) {
                this.mTmpDisplayArea = null;
                Slog.i("WindowManager", "In multiwindow : pkgName = " + pkgName);
                return true;
            }
        } else {
            TaskDisplayArea multiDisplayArea = getMultiDisplayArea();
            if (multiDisplayArea == null || (task = multiDisplayArea.getTopRootTask()) == null) {
                return false;
            }
            ActivityRecord resumedActivity = task.getResumedActivity();
            if (resumedActivity == null) {
                ComponentName realName = task.realActivity;
                return (realName == null || realName.getPackageName() == null || !realName.getPackageName().equals(pkgName)) ? false : true;
            }
            String taskName = resumedActivity.packageName;
            if (pkgName.equals(taskName)) {
                return true;
            }
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$activityInMultiWindow$51$com-android-server-wm-RootWindowContainer  reason: not valid java name */
    public /* synthetic */ boolean m8187xf72cb01c(String pkgName, TaskDisplayArea multiDisplayArea) {
        Task task;
        if ((multiDisplayArea.getMultiWindowingMode() & 2) == 0 || (task = multiDisplayArea.getTopRootTask()) == null) {
            return false;
        }
        ActivityRecord topResumedActivity = task.getTopResumedActivity();
        if (topResumedActivity == null) {
            ActivityRecord topNonFinishingActivity = task.getTopNonFinishingActivity();
            if (topNonFinishingActivity == null || topNonFinishingActivity.packageName == null || !topNonFinishingActivity.packageName.equals(pkgName)) {
                return false;
            }
            this.mTmpDisplayArea = multiDisplayArea;
            return true;
        }
        String taskName = topResumedActivity.packageName;
        if (pkgName.equals(taskName)) {
            this.mTmpDisplayArea = multiDisplayArea;
            return true;
        }
        return false;
    }

    public String getMulitWindowTopPackage() {
        Task task;
        TaskDisplayArea multiDisplayArea = getMultiDisplayArea();
        if (multiDisplayArea == null || (task = multiDisplayArea.getTopRootTask()) == null) {
            return null;
        }
        if (task.realActivity != null) {
            String packageName = task.realActivity.getPackageName();
            return packageName;
        }
        return "null";
    }

    public List<String> getMultiWindowBlackList() {
        return this.mMultiWindowBlackList;
    }

    public List<String> getMultiWindowWhiteList() {
        return this.mMultiWindowWhiteList;
    }

    public String getMultiDisplayAreaTopPackageV3() {
        return getMultiDisplayAreaTopPackage(-1, -1);
    }

    public String getMultiDisplayAreaTopPackage(int multiWindowMode, int multiWindowId) {
        TaskDisplayArea multiDisplayArea;
        Task task;
        if (ThunderbackConfig.isVersion4()) {
            multiDisplayArea = getMultiDisplayArea(multiWindowMode, multiWindowId);
        } else {
            multiDisplayArea = getMultiDisplayArea();
        }
        if (multiDisplayArea == null || (task = multiDisplayArea.getTopRootTask()) == null || task.realActivity == null) {
            return null;
        }
        String taskName = task.realActivity.getPackageName();
        return taskName;
    }

    private Task findTaskById(final int taskId) {
        final Task[] resultTask = new Task[1];
        forAllTasks(new Consumer() { // from class: com.android.server.wm.RootWindowContainer$$ExternalSyntheticLambda16
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                RootWindowContainer.lambda$findTaskById$52(taskId, resultTask, (Task) obj);
            }
        });
        return resultTask[0];
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$findTaskById$52(int taskId, Task[] resultTask, Task task) {
        if (task.isTaskId(taskId)) {
            resultTask[0] = task;
        }
    }

    public ActivityManager.RunningTaskInfo getTopTask(int displayId) {
        TaskDisplayArea displayArea;
        Task task;
        DisplayContent display = getDisplayContent(displayId);
        if (display == null || (displayArea = display.getDefaultTaskDisplayArea()) == null || (task = displayArea.getTopVisibleRootTask()) == null) {
            return null;
        }
        ActivityManager.RunningTaskInfo taskInfo = new ActivityManager.RunningTaskInfo();
        task.fillTaskInfo(taskInfo);
        taskInfo.id = taskInfo.taskId;
        return taskInfo;
    }

    public ActivityManager.RunningTaskInfo getMultiWinTopTask(int winMode, int winId) {
        Task task;
        TaskDisplayArea displayArea = getMultiDisplayArea(winMode, winId);
        if (displayArea == null || (task = displayArea.getTopRootTask()) == null) {
            return null;
        }
        ActivityManager.RunningTaskInfo taskInfo = new ActivityManager.RunningTaskInfo();
        task.fillTaskInfo(taskInfo);
        taskInfo.id = taskInfo.taskId;
        return taskInfo;
    }

    public int getTaskOrientation(int taskId) {
        Task task = findTaskById(taskId);
        if (task == null) {
            return -1;
        }
        ActivityRecord r = task.topRunningActivity();
        if (r != null) {
            int orientation = r.getOrientation();
            return orientation;
        }
        int orientation2 = task.getOrientation();
        return orientation2;
    }

    public void minimizeMultiWinToEdge(int taskId, boolean toEdge) {
        Task rootTask = getRootTask(taskId);
        if (rootTask != null && rootTask.getConfiguration().windowConfiguration.isThunderbackWindow()) {
            int multiWinMode = rootTask.getConfiguration().windowConfiguration.getMultiWindowingMode();
            int multiWinId = rootTask.getConfiguration().windowConfiguration.getMultiWindowingId();
            ITranRootWindowContainer.Instance().minimizeMultiWinToEdge(multiWinMode, multiWinId, toEdge);
        }
    }

    public Rect getMultiWindowContentRegion(int taskId) {
        Task rootTask = getRootTask(taskId);
        if (rootTask != null && rootTask.getConfiguration().windowConfiguration.isThunderbackWindow()) {
            int multiWinMode = rootTask.getConfiguration().windowConfiguration.getMultiWindowingMode();
            int multiWinId = rootTask.getConfiguration().windowConfiguration.getMultiWindowingId();
            return ITranRootWindowContainer.Instance().getMultiWindowContentRegion(multiWinMode, multiWinId);
        }
        return null;
    }

    public Bundle getMultiWindowParams(String pkgName) {
        Bundle bundle;
        synchronized (this.mService.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                bundle = null;
                TaskDisplayArea tda = getPreferMultiDisplayArea(pkgName);
                if (tda != null && tda.isMultiWindow()) {
                    int multiWinId = tda.getMultiWindowingId();
                    bundle = ITranRootWindowContainer.Instance().getMultiWindowParams(multiWinId);
                }
                if (bundle == null) {
                    bundle = new Bundle();
                }
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
        return bundle;
    }

    public boolean needMultiWindowAnimation() {
        return this.mMultiWindowAnimationNeeded;
    }

    public void setFinishFixedRotationEnterMultiWindowTransaction(SurfaceControl leash, int x, int y, int rotation, float scale) {
        float degrees;
        Matrix mtx = new Matrix();
        switch (rotation) {
            case 0:
            case 2:
                degrees = 0.0f;
                break;
            case 1:
                degrees = 90.0f;
                break;
            case 3:
                degrees = -90.0f;
                break;
            default:
                degrees = 0.0f;
                break;
        }
        mtx.setRotate(degrees);
        mtx.preScale(scale, scale);
        this.mDisplayTransaction.setMatrix(leash, mtx, new float[9]);
        this.mDisplayTransaction.setPosition(leash, x, y);
    }

    public void setFinishFixedRotationWithTransaction(SurfaceControl leash, float[] transFloat9, float[] cropFloat4, int rotation) {
        this.mFixRotation = rotation;
        if (this.mFixRotationTransaction == null) {
            this.mFixRotationTransaction = new SurfaceControl.Transaction();
        }
        this.mFixRotationTransaction.setWindowCrop(leash, new Rect((int) cropFloat4[0], (int) cropFloat4[1], (int) cropFloat4[2], (int) cropFloat4[3]));
        this.mFixRotationTransaction.setMatrix(leash, transFloat9[0], transFloat9[3], transFloat9[1], transFloat9[4]);
        this.mFixRotationTransaction.setPosition(leash, transFloat9[2], transFloat9[5]);
    }

    public void clearFinishFixedRotationWithTransaction() {
        this.mFixRotationTransaction = null;
    }

    public SurfaceControl getWeltWindowLeash(int width, int height, int x, int y, boolean hidden) {
        WindowState win = getWindow(new Predicate() { // from class: com.android.server.wm.RootWindowContainer$$ExternalSyntheticLambda55
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return RootWindowContainer.this.m8194x25030438((WindowState) obj);
            }
        });
        if (win != null) {
            SurfaceControl.Transaction t = this.mWmService.mTransactionFactory.get();
            return win.createWeltAnimationLeash(t, width, height, x, y, hidden);
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getWeltWindowLeash$53$com-android-server-wm-RootWindowContainer  reason: not valid java name */
    public /* synthetic */ boolean m8194x25030438(WindowState w) {
        return "Tran_Welt_Window_@6a859d9".equals(w.getWindowTag().toString());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$removeWeltWindowLeash$54$com-android-server-wm-RootWindowContainer  reason: not valid java name */
    public /* synthetic */ boolean m8207xbf5f6e01(WindowState w) {
        return "Tran_Welt_Window_@6a859d9".equals(w.getWindowTag().toString());
    }

    public void removeWeltWindowLeash(SurfaceControl leash) {
        WindowState win = getWindow(new Predicate() { // from class: com.android.server.wm.RootWindowContainer$$ExternalSyntheticLambda40
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return RootWindowContainer.this.m8207xbf5f6e01((WindowState) obj);
            }
        });
        Slog.i("WindowManager", "removeWeltWindowLeash, win:" + win);
        if (leash != null && leash.isValid()) {
            leash.release();
        }
        SurfaceControl.Transaction t = this.mWmService.mTransactionFactory.get();
        ITranRootWindowContainer.Instance().removeWeltLeash(win, t);
    }

    public SurfaceControl getDragAndZoomBgLeash(int width, int height, int x, int y, boolean hidden) {
        WindowState win = getWindow(new Predicate() { // from class: com.android.server.wm.RootWindowContainer$$ExternalSyntheticLambda13
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return RootWindowContainer.this.m8193xf19ae59d((WindowState) obj);
            }
        });
        if (win != null) {
            SurfaceControl.Transaction t = this.mWmService.mTransactionFactory.get();
            return win.createDragAndZoomBgLeash(t, width, height, x, y, hidden);
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getDragAndZoomBgLeash$55$com-android-server-wm-RootWindowContainer  reason: not valid java name */
    public /* synthetic */ boolean m8193xf19ae59d(WindowState w) {
        return "DragAndZoomBackground".equals(w.getWindowTag().toString());
    }

    private void hookDisplayAreaChildCount() {
        if (getMultiDisplayArea() != null) {
            TaskDisplayArea multiDisplayArea = getMultiDisplayArea();
            this.mIsValidActivityHere = false;
            multiDisplayArea.forAllRootTasks(new Consumer() { // from class: com.android.server.wm.RootWindowContainer$$ExternalSyntheticLambda49
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    RootWindowContainer.this.m8195xe52cf37((Task) obj);
                }
            });
            if (!this.mIsValidActivityHere) {
                if (ThunderbackConfig.isVersion4()) {
                    Slog.i("WindowManager", " there is no activity should resume on multi-window : getMultiDisplayArea().getMultiWindowingId() = " + multiDisplayArea.getMultiWindowingId());
                    ITranRootWindowContainer.Instance().hookDisplayAreaChildCountV4(multiDisplayArea.getMultiWindowingMode(), multiDisplayArea.getMultiWindowingId(), 0);
                } else {
                    Slog.i("WindowManager", " there is no activity should resume on multi-window");
                    ITranRootWindowContainer.Instance().hookDisplayAreaChildCountV3(multiDisplayArea.mDisplayAreaAppearedInfo.getTranMultiDisplayAreaId(), 0);
                }
                multiDisplayArea.mPendingToShow = true;
            } else if (multiDisplayArea.mPendingToShow) {
                if (ThunderbackConfig.isVersion4()) {
                    Slog.i("WindowManager", " there is  activity should resume on multi-window : getMultiDisplayArea().getMultiWindowingId() = " + multiDisplayArea.getMultiWindowingId());
                    ITranRootWindowContainer.Instance().hookDisplayAreaChildCountV4(multiDisplayArea.getMultiWindowingMode(), multiDisplayArea.getMultiWindowingId(), 1);
                } else {
                    Slog.i("WindowManager", " there is activity should resume on multi-window");
                    ITranRootWindowContainer.Instance().hookDisplayAreaChildCountV3(multiDisplayArea.mDisplayAreaAppearedInfo.getTranMultiDisplayAreaId(), 1);
                }
                multiDisplayArea.mPendingToShow = false;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$hookDisplayAreaChildCount$56$com-android-server-wm-RootWindowContainer  reason: not valid java name */
    public /* synthetic */ void m8195xe52cf37(Task rootTask) {
        if (this.mIsValidActivityHere) {
            return;
        }
        ActivityRecord topRunningActivity = rootTask.topRunningActivity();
        if (rootTask.mShouldSkipHookToMax) {
            this.mIsValidActivityHere = true;
        }
        if (!rootTask.isFocusableAndVisible() || topRunningActivity == null) {
            return;
        }
        this.mIsValidActivityHere = true;
    }

    private Task hookGetOrCreateRootTask(TaskDisplayArea taskDisplayArea, ActivityRecord r, ActivityOptions options, Task candidateTask, Task sourceTask, LaunchParamsController.LaunchParams launchParams, int launchFlags, int activityType, boolean onTop) {
        boolean isMultiWinDisplayArea;
        TaskDisplayArea tda;
        TaskDisplayArea taskDisplayArea2;
        String pkgNameInMultiWindow = ITranMultiWindow.Instance().getReadyStartInMultiWindowPackageName();
        if (ThunderbackConfig.isVersion4()) {
            TaskDisplayArea tda2 = getPerferTaskDisplayAreaFromPackageReady();
            boolean isMultiWinDisplayArea2 = tda2 != null && tda2.isMultiWindow();
            isMultiWinDisplayArea = isMultiWinDisplayArea2;
            tda = tda2;
        } else if (!ThunderbackConfig.isVersion3()) {
            isMultiWinDisplayArea = false;
            tda = null;
        } else {
            TaskDisplayArea tda3 = getMultiDisplayArea();
            boolean isMultiWinDisplayArea3 = tda3 != null;
            isMultiWinDisplayArea = isMultiWinDisplayArea3;
            tda = tda3;
        }
        if (isMultiWinDisplayArea && pkgNameInMultiWindow != null && candidateTask != null && candidateTask.mChildren.size() == 0 && candidateTask.affinity != null && candidateTask.affinity.contains(pkgNameInMultiWindow)) {
            TaskDisplayArea taskDisplayArea3 = tda;
            candidateTask.mShouldSkipHookToMax = true;
            taskDisplayArea2 = taskDisplayArea3;
        } else {
            taskDisplayArea2 = taskDisplayArea;
        }
        Task targetTask = taskDisplayArea2.getOrCreateRootTask(r, options, candidateTask, sourceTask, launchParams, launchFlags, activityType, onTop);
        if (candidateTask != null) {
            candidateTask.mShouldSkipHookToMax = false;
        }
        return targetTask;
    }

    public boolean taskInMultiWindowById(int taskId) {
        Task task;
        Task rootTask = getRootTask(taskId);
        if (rootTask == null) {
            Slog.i("WindowManager", "taskInMultiWindowById is invalid");
            return false;
        }
        if (ThunderbackConfig.isVersion4()) {
            if (rootTask.getConfiguration().windowConfiguration.isThunderbackWindow()) {
                return true;
            }
        } else {
            TaskDisplayArea multiDisplayArea = getMultiDisplayArea();
            if (multiDisplayArea != null && (task = multiDisplayArea.getTopRootTask()) != null && task.getRootTaskId() == taskId) {
                return true;
            }
        }
        return false;
    }

    private void hookAddFixRotationTransaction(int rotation) {
        SurfaceControl.Transaction transaction = this.mFixRotationTransaction;
        if (transaction != null && this.mFixRotation == rotation) {
            this.mDisplayTransaction.merge(transaction);
            this.mFixRotationTransaction = null;
        }
    }

    public SurfaceControl getDefaultRootLeash() {
        DisplayArea da = getDefaultDisplay().findAreaForWindowType(2038, null, false, false);
        if (da == null) {
            da = getDefaultTaskDisplayArea();
        }
        return new SurfaceControl(da.getSurfaceControl(), "TranMultiWindow#Welt.Parent");
    }

    public void reparentActivity(int fromDisplayId, int destDisplayId, boolean onTop) {
        Task rootTask;
        DisplayContent formDisplayContent = getDisplayContentOrCreate(fromDisplayId);
        DisplayContent destDisplayContent = getDisplayContentOrCreate(destDisplayId);
        if (formDisplayContent == null || destDisplayContent == null) {
            Slog.w("WindowManager", "move task to unknown display");
            return;
        }
        if (fromDisplayId == 0) {
            rootTask = formDisplayContent.getDefaultTaskDisplayArea().getTopRootTask();
        } else {
            rootTask = formDisplayContent.getTopRootTask();
        }
        if (rootTask == null) {
            Slog.w("WindowManager", "cant find root task from display: " + formDisplayContent);
        } else {
            moveRootTaskToTaskDisplayArea(rootTask.mTaskId, destDisplayContent.getDefaultTaskDisplayArea(), onTop);
        }
    }
}
