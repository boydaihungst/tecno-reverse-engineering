package com.android.server.wm;

import android.app.ThunderbackConfig;
import android.os.Trace;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Slog;
import android.view.RemoteAnimationAdapter;
import android.view.RemoteAnimationDefinition;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.window.ITaskFragmentOrganizer;
import com.android.internal.protolog.ProtoLogGroup;
import com.android.internal.protolog.ProtoLogImpl;
import com.android.server.wm.utils.TranFpUnlockStateController;
import com.transsion.hubcore.multiwindow.ITranMultiWindowManager;
import com.transsion.hubcore.server.wm.ITranAppTransition;
import com.transsion.hubcore.server.wm.ITranAppTransitionController;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.function.Consumer;
import java.util.function.Predicate;
/* loaded from: classes2.dex */
public class AppTransitionController {
    private static final String TAG = "WindowManager";
    private static final int TYPE_ACTIVITY = 1;
    private static final int TYPE_NONE = 0;
    private static final int TYPE_TASK = 3;
    private static final int TYPE_TASK_FRAGMENT = 2;
    private final DisplayContent mDisplayContent;
    private final WindowManagerService mService;
    private final WallpaperController mWallpaperControllerLocked;
    private RemoteAnimationDefinition mRemoteAnimationDefinition = null;
    private final ArrayMap<WindowContainer, Integer> mTempTransitionReasons = new ArrayMap<>();
    private final ArrayList<WindowContainer> mTempTransitionWindows = new ArrayList<>();
    private RemoteAnimationDefinition mRemoteAnimationDefinitionForMultiWin = null;
    private RemoteAnimationAdapter mThunderbackRemoteAnimationAdapter = null;

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes2.dex */
    @interface TransitContainerType {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public AppTransitionController(WindowManagerService service, DisplayContent displayContent) {
        this.mService = service;
        this.mDisplayContent = displayContent;
        this.mWallpaperControllerLocked = displayContent.mWallpaperController;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void registerRemoteAnimations(RemoteAnimationDefinition definition) {
        if (definition.hasTransition(50, new ArraySet())) {
            this.mRemoteAnimationDefinitionForMultiWin = definition;
        } else {
            this.mRemoteAnimationDefinition = definition;
        }
    }

    private WindowState getOldWallpaper() {
        WindowState wallpaperTarget = this.mWallpaperControllerLocked.getWallpaperTarget();
        int firstTransit = this.mDisplayContent.mAppTransition.getFirstAppTransition();
        boolean z = true;
        ArraySet<WindowContainer> openingWcs = getAnimationTargets(this.mDisplayContent.mOpeningApps, this.mDisplayContent.mClosingApps, true);
        if (wallpaperTarget == null || (!wallpaperTarget.hasWallpaper() && ((firstTransit != 1 && firstTransit != 3) || openingWcs.isEmpty() || openingWcs.valueAt(0).asTask() == null || !this.mWallpaperControllerLocked.isWallpaperVisible()))) {
            z = false;
        }
        boolean showWallpaper = z;
        if (this.mWallpaperControllerLocked.isWallpaperTargetAnimating() || !showWallpaper) {
            return null;
        }
        return wallpaperTarget;
    }

    private static ArraySet<ActivityRecord> getAppsForAnimation(ArraySet<ActivityRecord> apps, boolean excludeLauncherFromAnimation) {
        ArraySet<ActivityRecord> appsForAnimation = new ArraySet<>(apps);
        if (excludeLauncherFromAnimation) {
            appsForAnimation.removeIf(new Predicate() { // from class: com.android.server.wm.AppTransitionController$$ExternalSyntheticLambda5
                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    return ((ActivityRecord) obj).isActivityTypeHome();
                }
            });
        }
        return appsForAnimation;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void handleAppTransitionReady() {
        ActivityRecord topChangingApp;
        ActivityRecord animLpActivity;
        ActivityRecord topOpeningApp;
        this.mTempTransitionReasons.clear();
        if (!transitionGoodToGo(this.mDisplayContent.mOpeningApps, this.mTempTransitionReasons) || !transitionGoodToGo(this.mDisplayContent.mChangingContainers, this.mTempTransitionReasons) || !transitionGoodToGoForTaskFragments()) {
            return;
        }
        Trace.traceBegin(32L, "AppTransitionReady");
        if (ProtoLogCache.WM_DEBUG_APP_TRANSITIONS_enabled) {
            ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_APP_TRANSITIONS, 255692476, 0, (String) null, (Object[]) null);
        }
        this.mDisplayContent.forAllWindows(new Consumer() { // from class: com.android.server.wm.AppTransitionController$$ExternalSyntheticLambda7
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ((WindowState) obj).cleanupAnimatingExitWindow();
            }
        }, true);
        AppTransition appTransition = this.mDisplayContent.mAppTransition;
        ITranAppTransition.Instance().hookAppTransitSuccess(this.mDisplayContent.getDisplayPolicy());
        this.mDisplayContent.mNoAnimationNotifyOnTransitionFinished.clear();
        appTransition.removeAppTransitionTimeoutCallbacks();
        this.mDisplayContent.mWallpaperMayChange = false;
        int appCount = this.mDisplayContent.mOpeningApps.size();
        for (int i = 0; i < appCount; i++) {
            ((ActivityRecord) this.mDisplayContent.mOpeningApps.valueAtUnchecked(i)).clearAnimatingFlags();
        }
        int appCount2 = this.mDisplayContent.mChangingContainers.size();
        for (int i2 = 0; i2 < appCount2; i2++) {
            ActivityRecord activity = getAppFromContainer((WindowContainer) this.mDisplayContent.mChangingContainers.valueAtUnchecked(i2));
            if (activity != null) {
                activity.clearAnimatingFlags();
            }
        }
        this.mWallpaperControllerLocked.adjustWallpaperWindowsForAppTransitionIfNeeded(this.mDisplayContent.mOpeningApps);
        boolean excludeLauncherFromAnimation = this.mDisplayContent.mOpeningApps.stream().anyMatch(new Predicate() { // from class: com.android.server.wm.AppTransitionController$$ExternalSyntheticLambda8
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                boolean isAnimating;
                isAnimating = ((ActivityRecord) obj).isAnimating(2, 8);
                return isAnimating;
            }
        }) || this.mDisplayContent.mClosingApps.stream().anyMatch(new Predicate() { // from class: com.android.server.wm.AppTransitionController$$ExternalSyntheticLambda9
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                boolean isAnimating;
                isAnimating = ((ActivityRecord) obj).isAnimating(2, 8);
                return isAnimating;
            }
        });
        ArraySet<ActivityRecord> openingAppsForAnimation = getAppsForAnimation(this.mDisplayContent.mOpeningApps, excludeLauncherFromAnimation);
        ArraySet<ActivityRecord> closingAppsForAnimation = getAppsForAnimation(this.mDisplayContent.mClosingApps, excludeLauncherFromAnimation);
        int transit = getTransitCompatType(this.mDisplayContent.mAppTransition, openingAppsForAnimation, closingAppsForAnimation, this.mDisplayContent.mChangingContainers, this.mWallpaperControllerLocked.getWallpaperTarget(), getOldWallpaper(), this.mDisplayContent.mSkipAppTransitionAnimation);
        this.mDisplayContent.mSkipAppTransitionAnimation = false;
        boolean isFingerprintUnlockTransition = this.mDisplayContent.mAppTransition.isFingerprintUnlockTransition();
        boolean isAuthenticateSucceed = TranFpUnlockStateController.getInstance().isAuthenticateSucceed();
        boolean isKeyguardGoingAwaTransitWithAwayQuicklyFlag = AppTransition.isKeyguardGoingAwayTransitOld(transit) && (appTransition.getTransitFlags() & 512) == 512;
        if ((isKeyguardGoingAwaTransitWithAwayQuicklyFlag || isFingerprintUnlockTransition) && isAuthenticateSucceed) {
            this.mDisplayContent.getDisplayPolicy().hideKeyguardWindow();
        }
        if (ProtoLogCache.WM_DEBUG_APP_TRANSITIONS_enabled) {
            long protoLogParam0 = this.mDisplayContent.mDisplayId;
            String protoLogParam1 = String.valueOf(appTransition.toString());
            String protoLogParam3 = String.valueOf(this.mDisplayContent.mOpeningApps);
            String protoLogParam4 = String.valueOf(this.mDisplayContent.mClosingApps);
            String protoLogParam5 = String.valueOf(AppTransition.appTransitionOldToString(transit));
            ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_APP_TRANSITIONS, -134793542, 49, (String) null, new Object[]{Long.valueOf(protoLogParam0), protoLogParam1, Boolean.valueOf(excludeLauncherFromAnimation), protoLogParam3, protoLogParam4, protoLogParam5});
        }
        if (appTransition.containsTransitRequest(32)) {
            ITranMultiWindowManager.Instance().hookCancelBlurLayer();
        }
        ArraySet<Integer> activityTypes = collectActivityTypes(openingAppsForAnimation, closingAppsForAnimation, this.mDisplayContent.mChangingContainers);
        ActivityRecord animLpActivity2 = findAnimLayoutParamsToken(transit, activityTypes, openingAppsForAnimation, closingAppsForAnimation, this.mDisplayContent.mChangingContainers);
        ActivityRecord topOpeningApp2 = getTopApp(openingAppsForAnimation, false);
        ActivityRecord topClosingApp = getTopApp(closingAppsForAnimation, false);
        ActivityRecord topChangingApp2 = getTopApp(this.mDisplayContent.mChangingContainers, false);
        WindowManager.LayoutParams animLp = getAnimLp(animLpActivity2);
        if (!overrideWithTaskFragmentRemoteAnimation(transit, activityTypes)) {
            unfreezeEmbeddedChangingWindows();
            overrideWithRemoteAnimationIfSet(animLpActivity2, transit, activityTypes);
        }
        boolean voiceInteraction = containsVoiceInteraction(openingAppsForAnimation);
        this.mService.mSurfaceAnimationRunner.deferStartingAnimations();
        try {
            this.mService.mRoot.mStartActivityInMultiTaskDisplayArea = false;
            this.mService.mRoot.mStartActivityInDefaultTaskDisplayArea = false;
            try {
                if (ThunderbackConfig.isVersion4() && transit == 51) {
                    Slog.i("WindowManager", "thunderback close, use app animation");
                    topChangingApp = topChangingApp2;
                    animLpActivity = topClosingApp;
                    topOpeningApp = topOpeningApp2;
                    applyAnimations(new ArraySet<>(), this.mDisplayContent.mClosingApps, transit, animLp, voiceInteraction);
                } else {
                    topChangingApp = topChangingApp2;
                    animLpActivity = topClosingApp;
                    topOpeningApp = topOpeningApp2;
                    ArrayList<ActivityRecord> toRemoveList = new ArrayList<>();
                    if (this.mDisplayContent.mAppTransition.getRemoteAnimationController() == null) {
                        for (int i3 = 0; i3 < openingAppsForAnimation.size(); i3++) {
                            ActivityRecord openingApp = openingAppsForAnimation.valueAt(i3);
                            if (ITranAppTransitionController.Instance().isThunderbackWindow(this.mService.mAtmService, openingApp, openingApp != null ? openingApp.getDisplayArea().mAppTransitionReady : false)) {
                                Slog.i("WindowManager", " First multiWindow open,ignore the animation for openingApp=" + openingApp);
                                toRemoveList.add(openingApp);
                            }
                        }
                    } else if (appTransition.containsTransitRequest(32)) {
                        for (int i4 = 0; i4 < openingAppsForAnimation.size(); i4++) {
                            ActivityRecord openingApp2 = openingAppsForAnimation.valueAt(i4);
                            if (openingApp2 != null && openingApp2.getConfiguration().windowConfiguration.isThunderbackWindow()) {
                                Slog.i("WindowManager", " Thunderback open action,ignore the animation for openingApp=" + openingApp2);
                                toRemoveList.add(openingApp2);
                            }
                        }
                    }
                    int i5 = toRemoveList.size();
                    if (i5 > 0) {
                        Iterator<ActivityRecord> it = toRemoveList.iterator();
                        while (it.hasNext()) {
                            ActivityRecord ar = it.next();
                            openingAppsForAnimation.remove(ar);
                        }
                    }
                    applyAnimations(openingAppsForAnimation, closingAppsForAnimation, transit, animLp, voiceInteraction);
                }
                handleClosingApps();
                handleOpeningApps();
                handleChangingApps(transit);
                appTransition.setLastAppTransition(transit, topOpeningApp, animLpActivity, topChangingApp);
                int flags = appTransition.getTransitFlags();
                int layoutRedo = appTransition.goodToGo(transit, topOpeningApp);
                handleNonAppWindowsInTransition(transit, flags);
                appTransition.postAnimationCallback();
                appTransition.clear();
                this.mService.mSurfaceAnimationRunner.continueStartingAnimations();
                this.mService.mTaskSnapshotController.onTransitionStarting(this.mDisplayContent);
                this.mDisplayContent.mOpeningApps.clear();
                this.mDisplayContent.mClosingApps.clear();
                this.mDisplayContent.mChangingContainers.clear();
                this.mDisplayContent.mUnknownAppVisibilityController.clear();
                this.mDisplayContent.setLayoutNeeded();
                this.mDisplayContent.computeImeTarget(true);
                this.mService.mAtmService.mTaskSupervisor.getActivityMetricsLogger().notifyTransitionStarting(this.mTempTransitionReasons);
                Trace.traceEnd(32L);
                this.mDisplayContent.pendingLayoutChanges |= layoutRedo | 1 | 2;
            } catch (Throwable th) {
                th = th;
                this.mService.mSurfaceAnimationRunner.continueStartingAnimations();
                throw th;
            }
        } catch (Throwable th2) {
            th = th2;
        }
    }

    static int getTransitCompatType(AppTransition appTransition, ArraySet<ActivityRecord> openingApps, ArraySet<ActivityRecord> closingApps, ArraySet<WindowContainer> changingContainers, WindowState wallpaperTarget, WindowState oldWallpaper, boolean skipAppTransitionAnimation) {
        char c;
        int i;
        int i2;
        int firstTransit;
        int openingType;
        int closingType;
        boolean z = true;
        boolean openingAppHasWallpaper = canBeWallpaperTarget(openingApps) && wallpaperTarget != null;
        boolean closingAppHasWallpaper = canBeWallpaperTarget(closingApps) && wallpaperTarget != null;
        switch (appTransition.getKeyguardTransition()) {
            case 7:
                return openingAppHasWallpaper ? 21 : 20;
            case 8:
                return closingApps.isEmpty() ? 22 : 6;
            case 9:
                return 23;
            case 40:
                ActivityRecord topOpeningApp = getTopApp(openingApps, false);
                boolean isLauncherTop = false;
                if (topOpeningApp != null) {
                    if (!"com.transsion.XOSLauncher".equals(topOpeningApp.packageName) && !"com.transsion.hilauncher".equals(topOpeningApp.packageName)) {
                        z = false;
                    }
                    isLauncherTop = z;
                }
                return (openingAppHasWallpaper || isLauncherTop) ? 21 : 8;
            default:
                int transitStatus = ITranAppTransitionController.Instance().getTransitOldThunderStatus(appTransition);
                if (transitStatus != -1) {
                    return transitStatus;
                }
                if (skipAppTransitionAnimation) {
                    return -1;
                }
                int flags = appTransition.getTransitFlags();
                int firstTransit2 = appTransition.getFirstAppTransition();
                if (!appTransition.containsTransitRequest(6) || changingContainers.isEmpty()) {
                    if (ITranAppTransitionController.Instance().isContained(appTransition, 34)) {
                        return 54;
                    }
                    if ((flags & 16) != 0) {
                        return 26;
                    }
                    if (firstTransit2 == 0) {
                        return 0;
                    }
                    if (AppTransition.isNormalTransit(firstTransit2)) {
                        boolean allOpeningVisible = true;
                        boolean allTranslucentOpeningApps = !openingApps.isEmpty();
                        for (int i3 = openingApps.size() - 1; i3 >= 0; i3--) {
                            ActivityRecord activity = openingApps.valueAt(i3);
                            if (!activity.isVisible()) {
                                allOpeningVisible = false;
                                if (activity.fillsParent()) {
                                    allTranslucentOpeningApps = false;
                                }
                            }
                        }
                        boolean allTranslucentClosingApps = !closingApps.isEmpty();
                        int i4 = closingApps.size() - 1;
                        while (true) {
                            if (i4 >= 0) {
                                if (closingApps.valueAt(i4).fillsParent()) {
                                    allTranslucentClosingApps = false;
                                } else {
                                    i4--;
                                }
                            }
                        }
                        if (allTranslucentClosingApps && allOpeningVisible) {
                            return 25;
                        }
                        if (allTranslucentOpeningApps && closingApps.isEmpty()) {
                            return 24;
                        }
                    }
                    ActivityRecord topOpeningApp2 = getTopApp(openingApps, false);
                    ActivityRecord topClosingApp = getTopApp(closingApps, true);
                    if (closingAppHasWallpaper && openingAppHasWallpaper) {
                        if (ProtoLogCache.WM_DEBUG_APP_TRANSITIONS_enabled) {
                            ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_APP_TRANSITIONS, 100936473, 0, (String) null, (Object[]) null);
                        }
                        switch (firstTransit2) {
                            case 1:
                            case 3:
                                return 14;
                            case 2:
                            case 4:
                                return 15;
                            default:
                                i = 2;
                                i2 = 6;
                                firstTransit = firstTransit2;
                                break;
                        }
                    } else if (oldWallpaper != null && !openingApps.isEmpty() && !openingApps.contains(oldWallpaper.mActivityRecord) && closingApps.contains(oldWallpaper.mActivityRecord) && topClosingApp == oldWallpaper.mActivityRecord) {
                        if (ITranAppTransitionController.Instance().isIntegerEqual(firstTransit2, 33)) {
                            return 53;
                        }
                        return 12;
                    } else {
                        if (wallpaperTarget == null || !wallpaperTarget.isVisible()) {
                            c = ' ';
                        } else if (!openingApps.contains(wallpaperTarget.mActivityRecord) || topOpeningApp2 != wallpaperTarget.mActivityRecord) {
                            c = ' ';
                        } else if (ITranAppTransitionController.Instance().isIntegerEqual(firstTransit2, 32)) {
                            return 52;
                        } else {
                            if (firstTransit2 == 56) {
                                return 57;
                            }
                            return 13;
                        }
                        i = 2;
                        i2 = 6;
                        if (ITranAppTransitionController.Instance().isTransitOldNone("WindowManager", topOpeningApp2, wallpaperTarget, openingApps, topClosingApp)) {
                            return 0;
                        }
                        if (wallpaperTarget == null || topOpeningApp2 == null) {
                            firstTransit = firstTransit2;
                        } else if (!wallpaperTarget.isVisible()) {
                            firstTransit = firstTransit2;
                        } else if (topOpeningApp2.getActivityType() != 2) {
                            firstTransit = firstTransit2;
                        } else if (wallpaperTarget.getAttrs().type == 2040 || wallpaperTarget.getAttrs().type == 2044) {
                            firstTransit = firstTransit2;
                            if (firstTransit == 56) {
                                return 57;
                            }
                        } else {
                            firstTransit = firstTransit2;
                        }
                    }
                    ArraySet<WindowContainer> openingWcs = getAnimationTargets(openingApps, closingApps, true);
                    ArraySet<WindowContainer> closingWcs = getAnimationTargets(openingApps, closingApps, false);
                    WindowContainer<?> openingContainer = !openingWcs.isEmpty() ? openingWcs.valueAt(0) : null;
                    WindowContainer<?> closingContainer = !closingWcs.isEmpty() ? closingWcs.valueAt(0) : null;
                    int openingType2 = getTransitContainerType(openingContainer);
                    int closingType2 = getTransitContainerType(closingContainer);
                    int osTransitType = IAppTransitionControllerLice.instance().onGetTransitCompatType(appTransition, openingType2, closingType2, topOpeningApp2, getBottomApp(closingApps, false));
                    if (osTransitType == -1) {
                        if (appTransition.containsTransitRequest(3)) {
                            openingType = openingType2;
                            if (openingType == 3) {
                                return 10;
                            }
                        } else {
                            openingType = openingType2;
                        }
                        if (appTransition.containsTransitRequest(4)) {
                            closingType = closingType2;
                            if (closingType == 3) {
                                return 11;
                            }
                        } else {
                            closingType = closingType2;
                        }
                        if (appTransition.containsTransitRequest(1)) {
                            if (openingType == 3) {
                                return (appTransition.getTransitFlags() & 32) != 0 ? 16 : 8;
                            } else if (openingType == 1) {
                                return i2;
                            } else {
                                if (openingType == i) {
                                    return 28;
                                }
                            }
                        }
                        if (appTransition.containsTransitRequest(i)) {
                            if (closingType == 3) {
                                return 9;
                            }
                            if (closingType == i) {
                                return 29;
                            }
                            if (closingType == 1) {
                                for (int i5 = closingApps.size() - 1; i5 >= 0; i5--) {
                                    if (closingApps.valueAt(i5).visibleIgnoringKeyguard) {
                                        return 7;
                                    }
                                }
                                return -1;
                            }
                        }
                        return (!appTransition.containsTransitRequest(5) || openingWcs.isEmpty() || openingApps.isEmpty()) ? 0 : 18;
                    }
                    return osTransitType;
                }
                int changingType = getTransitContainerType(changingContainers.valueAt(0));
                switch (changingType) {
                    case 2:
                        return 30;
                    case 3:
                        return 27;
                    default:
                        throw new IllegalStateException("TRANSIT_CHANGE with unrecognized changing type=" + changingType);
                }
        }
    }

    private static int getTransitContainerType(WindowContainer<?> container) {
        if (container == null) {
            return 0;
        }
        if (container.asTask() != null) {
            return 3;
        }
        if (container.asTaskFragment() != null) {
            return 2;
        }
        if (container.asActivityRecord() == null) {
            return 0;
        }
        return 1;
    }

    private static WindowManager.LayoutParams getAnimLp(ActivityRecord activity) {
        WindowState mainWindow = activity != null ? activity.findMainWindow() : null;
        if (mainWindow != null) {
            return mainWindow.mAttrs;
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public RemoteAnimationAdapter getRemoteAnimationOverride(WindowContainer container, int transit, ArraySet<Integer> activityTypes) {
        RemoteAnimationAdapter adapter;
        RemoteAnimationDefinition definition;
        RemoteAnimationAdapter adapter2;
        if (container != null && (definition = container.getRemoteAnimationDefinition()) != null && (adapter2 = definition.getAdapter(transit, activityTypes)) != null) {
            return adapter2;
        }
        RemoteAnimationDefinition definition2 = this.mRemoteAnimationDefinitionForMultiWin;
        if (definition2 != null && (adapter = definition2.getAdapter(transit, activityTypes)) != null) {
            this.mThunderbackRemoteAnimationAdapter = adapter;
            return adapter;
        }
        RemoteAnimationDefinition remoteAnimationDefinition = this.mRemoteAnimationDefinition;
        if (remoteAnimationDefinition != null) {
            return remoteAnimationDefinition.getAdapter(transit, activityTypes);
        }
        return null;
    }

    private void unfreezeEmbeddedChangingWindows() {
        ArraySet<WindowContainer> changingContainers = this.mDisplayContent.mChangingContainers;
        for (int i = changingContainers.size() - 1; i >= 0; i--) {
            WindowContainer wc = changingContainers.valueAt(i);
            if (wc.isEmbedded()) {
                wc.mSurfaceFreezer.unfreeze(wc.getSyncTransaction());
            }
        }
    }

    private boolean transitionMayContainNonAppWindows(int transit) {
        return NonAppWindowAnimationAdapter.shouldStartNonAppWindowAnimationsForKeyguardExit(transit) || NonAppWindowAnimationAdapter.shouldAttachNavBarToApp(this.mService, this.mDisplayContent, transit) || WallpaperAnimationAdapter.shouldStartWallpaperAnimation(this.mDisplayContent);
    }

    private Task findParentTaskForAllEmbeddedWindows() {
        this.mTempTransitionWindows.clear();
        this.mTempTransitionWindows.addAll(this.mDisplayContent.mClosingApps);
        this.mTempTransitionWindows.addAll(this.mDisplayContent.mOpeningApps);
        this.mTempTransitionWindows.addAll(this.mDisplayContent.mChangingContainers);
        Task leafTask = null;
        int i = this.mTempTransitionWindows.size() - 1;
        while (true) {
            if (i >= 0) {
                ActivityRecord r = getAppFromContainer(this.mTempTransitionWindows.get(i));
                if (r == null) {
                    leafTask = null;
                    break;
                }
                Task task = r.getTask();
                if (task != null && !task.inPinnedWindowingMode()) {
                    if (leafTask != null && leafTask != task) {
                        leafTask = null;
                        break;
                    }
                    ActivityRecord rootActivity = task.getRootActivity();
                    if (rootActivity == null) {
                        leafTask = null;
                        break;
                    } else if (r.getUid() != task.effectiveUid && !r.isEmbedded()) {
                        leafTask = null;
                        break;
                    } else {
                        leafTask = task;
                        i--;
                    }
                } else {
                    break;
                }
            } else {
                break;
            }
        }
        leafTask = null;
        this.mTempTransitionWindows.clear();
        return leafTask;
    }

    private ITaskFragmentOrganizer findTaskFragmentOrganizer(Task task) {
        if (task == null) {
            return null;
        }
        final ITaskFragmentOrganizer[] organizer = new ITaskFragmentOrganizer[1];
        boolean hasMultipleOrganizers = task.forAllLeafTaskFragments(new Predicate() { // from class: com.android.server.wm.AppTransitionController$$ExternalSyntheticLambda6
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return AppTransitionController.lambda$findTaskFragmentOrganizer$2(organizer, (TaskFragment) obj);
            }
        });
        if (hasMultipleOrganizers) {
            if (ProtoLogCache.WM_DEBUG_APP_TRANSITIONS_enabled) {
                ProtoLogImpl.e(ProtoLogGroup.WM_DEBUG_APP_TRANSITIONS, 1805116444, 0, (String) null, (Object[]) null);
            }
            return null;
        }
        return organizer[0];
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$findTaskFragmentOrganizer$2(ITaskFragmentOrganizer[] organizer, TaskFragment taskFragment) {
        ITaskFragmentOrganizer tfOrganizer = taskFragment.getTaskFragmentOrganizer();
        if (tfOrganizer == null) {
            return false;
        }
        if (organizer[0] != null && !organizer[0].asBinder().equals(tfOrganizer.asBinder())) {
            return true;
        }
        organizer[0] = tfOrganizer;
        return false;
    }

    private boolean overrideWithTaskFragmentRemoteAnimation(int transit, ArraySet<Integer> activityTypes) {
        RemoteAnimationDefinition definition;
        RemoteAnimationAdapter adapter;
        if (transitionMayContainNonAppWindows(transit)) {
            return false;
        }
        final Task task = findParentTaskForAllEmbeddedWindows();
        ITaskFragmentOrganizer organizer = findTaskFragmentOrganizer(task);
        if (organizer != null) {
            definition = this.mDisplayContent.mAtmService.mTaskFragmentOrganizerController.getRemoteAnimationDefinition(organizer, task.mTaskId);
        } else {
            definition = null;
        }
        if (definition == null) {
            adapter = null;
        } else {
            adapter = definition.getAdapter(transit, activityTypes);
        }
        if (adapter == null) {
            return false;
        }
        this.mDisplayContent.mAppTransition.overridePendingAppTransitionRemote(adapter);
        if (ProtoLogCache.WM_DEBUG_APP_TRANSITIONS_enabled) {
            String protoLogParam0 = String.valueOf(AppTransition.appTransitionOldToString(transit));
            ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_APP_TRANSITIONS, -702650156, 0, (String) null, new Object[]{protoLogParam0});
        }
        int organizerUid = this.mDisplayContent.mAtmService.mTaskFragmentOrganizerController.getTaskFragmentOrganizerUid(organizer);
        boolean shouldDisableInputForRemoteAnimation = !task.isFullyTrustedEmbedding(organizerUid);
        RemoteAnimationController remoteAnimationController = this.mDisplayContent.mAppTransition.getRemoteAnimationController();
        if (shouldDisableInputForRemoteAnimation && remoteAnimationController != null) {
            remoteAnimationController.setOnRemoteAnimationReady(new Runnable() { // from class: com.android.server.wm.AppTransitionController$$ExternalSyntheticLambda10
                @Override // java.lang.Runnable
                public final void run() {
                    AppTransitionController.lambda$overrideWithTaskFragmentRemoteAnimation$4(Task.this);
                }
            });
            if (ProtoLogCache.WM_DEBUG_APP_TRANSITIONS_enabled) {
                long protoLogParam02 = task.mTaskId;
                ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_APP_TRANSITIONS, 2100457473, 1, (String) null, new Object[]{Long.valueOf(protoLogParam02)});
            }
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$overrideWithTaskFragmentRemoteAnimation$4(Task task) {
        Consumer<ActivityRecord> updateActivities = new Consumer() { // from class: com.android.server.wm.AppTransitionController$$ExternalSyntheticLambda0
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ((ActivityRecord) obj).setDropInputForAnimation(true);
            }
        };
        task.forAllActivities(updateActivities);
    }

    private void overrideWithRemoteAnimationIfSet(ActivityRecord animLpActivity, int transit, ArraySet<Integer> activityTypes) {
        RemoteAnimationAdapter remoteAnimationAdapter;
        RemoteAnimationAdapter adapter = null;
        if (transit != 26) {
            if (AppTransition.isKeyguardGoingAwayTransitOld(transit)) {
                RemoteAnimationDefinition remoteAnimationDefinition = this.mRemoteAnimationDefinition;
                if (remoteAnimationDefinition != null) {
                    remoteAnimationAdapter = remoteAnimationDefinition.getAdapter(transit, activityTypes);
                } else {
                    remoteAnimationAdapter = null;
                }
                adapter = remoteAnimationAdapter;
            } else if (this.mDisplayContent.mAppTransition.getRemoteAnimationController() == null) {
                adapter = getRemoteAnimationOverride(animLpActivity, transit, activityTypes);
            }
        }
        if (adapter != null) {
            this.mDisplayContent.mAppTransition.overridePendingAppTransitionRemote(adapter);
            this.mDisplayContent.mAppTransition.overridePendingAppTransitionRemoteInner(adapter, false, this.mThunderbackRemoteAnimationAdapter);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isThunderbackTransitionAnimationRunning() {
        if (this.mThunderbackRemoteAnimationAdapter != null) {
            return this.mDisplayContent.mAppTransition.isThunderbackRemoteAnimationRunning(this.mThunderbackRemoteAnimationAdapter);
        }
        return false;
    }

    static Task findRootTaskFromContainer(WindowContainer wc) {
        return wc.asTaskFragment() != null ? wc.asTaskFragment().getRootTask() : wc.asActivityRecord().getRootTask();
    }

    static ActivityRecord getAppFromContainer(WindowContainer wc) {
        return wc.asTaskFragment() != null ? wc.asTaskFragment().getTopNonFinishingActivity() : wc.asActivityRecord();
    }

    private ActivityRecord findAnimLayoutParamsToken(final int transit, final ArraySet<Integer> activityTypes, ArraySet<ActivityRecord> openingApps, ArraySet<ActivityRecord> closingApps, ArraySet<WindowContainer> changingApps) {
        ActivityRecord result = lookForHighestTokenWithFilter(closingApps, openingApps, changingApps, new Predicate() { // from class: com.android.server.wm.AppTransitionController$$ExternalSyntheticLambda2
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return AppTransitionController.lambda$findAnimLayoutParamsToken$5(transit, activityTypes, (ActivityRecord) obj);
            }
        });
        if (result != null) {
            return result;
        }
        ActivityRecord result2 = lookForHighestTokenWithFilter(closingApps, openingApps, changingApps, new Predicate() { // from class: com.android.server.wm.AppTransitionController$$ExternalSyntheticLambda3
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return AppTransitionController.lambda$findAnimLayoutParamsToken$6((ActivityRecord) obj);
            }
        });
        if (result2 != null) {
            return result2;
        }
        return lookForHighestTokenWithFilter(closingApps, openingApps, changingApps, new Predicate() { // from class: com.android.server.wm.AppTransitionController$$ExternalSyntheticLambda4
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return AppTransitionController.lambda$findAnimLayoutParamsToken$7((ActivityRecord) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$findAnimLayoutParamsToken$5(int transit, ArraySet activityTypes, ActivityRecord w) {
        return w.getRemoteAnimationDefinition() != null && w.getRemoteAnimationDefinition().hasTransition(transit, activityTypes);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$findAnimLayoutParamsToken$6(ActivityRecord w) {
        return w.fillsParent() && w.findMainWindow() != null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$findAnimLayoutParamsToken$7(ActivityRecord w) {
        return w.findMainWindow() != null;
    }

    private static ArraySet<Integer> collectActivityTypes(ArraySet<ActivityRecord> array1, ArraySet<ActivityRecord> array2, ArraySet<WindowContainer> array3) {
        ArraySet<Integer> result = new ArraySet<>();
        for (int i = array1.size() - 1; i >= 0; i--) {
            result.add(Integer.valueOf(array1.valueAt(i).getActivityType()));
        }
        int i2 = array2.size();
        for (int i3 = i2 - 1; i3 >= 0; i3--) {
            result.add(Integer.valueOf(array2.valueAt(i3).getActivityType()));
        }
        int i4 = array3.size();
        for (int i5 = i4 - 1; i5 >= 0; i5--) {
            result.add(Integer.valueOf(array3.valueAt(i5).getActivityType()));
        }
        return result;
    }

    private static ActivityRecord lookForHighestTokenWithFilter(ArraySet<ActivityRecord> array1, ArraySet<ActivityRecord> array2, ArraySet<WindowContainer> array3, Predicate<ActivityRecord> filter) {
        WindowContainer wtoken;
        int array2base = array1.size();
        int array3base = array2.size() + array2base;
        int count = array3.size() + array3base;
        int bestPrefixOrderIndex = Integer.MIN_VALUE;
        ActivityRecord bestToken = null;
        for (int i = 0; i < count; i++) {
            if (i < array2base) {
                wtoken = array1.valueAt(i);
            } else if (i < array3base) {
                wtoken = array2.valueAt(i - array2base);
            } else {
                wtoken = array3.valueAt(i - array3base);
            }
            int prefixOrderIndex = wtoken.getPrefixOrderIndex();
            ActivityRecord r = getAppFromContainer(wtoken);
            if (r != null && filter.test(r) && prefixOrderIndex > bestPrefixOrderIndex) {
                bestPrefixOrderIndex = prefixOrderIndex;
                bestToken = r;
            }
        }
        return bestToken;
    }

    private boolean containsVoiceInteraction(ArraySet<ActivityRecord> apps) {
        for (int i = apps.size() - 1; i >= 0; i--) {
            if (apps.valueAt(i).mVoiceInteraction) {
                return true;
            }
        }
        return false;
    }

    private void applyAnimations(ArraySet<WindowContainer> wcs, ArraySet<ActivityRecord> apps, int transit, boolean visible, WindowManager.LayoutParams animLp, boolean voiceInteraction) {
        int wcsCount = wcs.size();
        for (int i = 0; i < wcsCount; i++) {
            WindowContainer wc = wcs.valueAt(i);
            ArrayList<ActivityRecord> transitioningDescendants = new ArrayList<>();
            for (int j = 0; j < apps.size(); j++) {
                ActivityRecord app = apps.valueAt(j);
                if (app.isDescendantOf(wc)) {
                    transitioningDescendants.add(app);
                }
            }
            wc.applyAnimation(animLp, transit, visible, voiceInteraction, transitioningDescendants);
        }
    }

    private static boolean isTaskViewTask(WindowContainer wc) {
        return (wc instanceof Task) && ((Task) wc).mRemoveWithTaskOrganizer;
    }

    static ArraySet<WindowContainer> getAnimationTargets(ArraySet<ActivityRecord> openingApps, ArraySet<ActivityRecord> closingApps, boolean visible) {
        LinkedList<WindowContainer> candidates = new LinkedList<>();
        ArraySet<ActivityRecord> apps = visible ? openingApps : closingApps;
        for (int i = 0; i < apps.size(); i++) {
            ActivityRecord app = apps.valueAt(i);
            if (app.shouldApplyAnimation(visible)) {
                candidates.add(app);
                if (ProtoLogCache.WM_DEBUG_APP_TRANSITIONS_enabled) {
                    String protoLogParam0 = String.valueOf(app);
                    boolean protoLogParam1 = app.isVisible();
                    ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_APP_TRANSITIONS, 1967975839, 60, (String) null, new Object[]{protoLogParam0, Boolean.valueOf(protoLogParam1), false});
                }
            }
        }
        ArraySet<ActivityRecord> otherApps = visible ? closingApps : openingApps;
        ArraySet<WindowContainer> otherAncestors = new ArraySet<>();
        for (int i2 = 0; i2 < otherApps.size(); i2++) {
            for (WindowContainer wc = otherApps.valueAt(i2); wc != null; wc = wc.getParent()) {
                otherAncestors.add(wc);
            }
        }
        ArraySet<WindowContainer> targets = new ArraySet<>();
        ArrayList<WindowContainer> siblings = new ArrayList<>();
        while (!candidates.isEmpty()) {
            WindowContainer current = candidates.removeFirst();
            WindowContainer parent = current.getParent();
            siblings.clear();
            siblings.add(current);
            boolean canPromote = true;
            if (!isTaskViewTask(current)) {
                if (parent == null || !parent.canCreateRemoteAnimationTarget() || !parent.canBeAnimationTarget() || ((current.asTask() != null && current.asTask().mInRemoveTask) || parent.isChangingAppTransition())) {
                    canPromote = false;
                } else {
                    if (otherAncestors.contains(parent)) {
                        canPromote = false;
                    }
                    if (current.asTask() != null && current.asTask().getAdjacentTaskFragment() != null && current.asTask().getAdjacentTaskFragment().asTask() != null) {
                        canPromote = false;
                    }
                    for (int j = 0; j < parent.getChildCount(); j++) {
                        WindowContainer sibling = parent.getChildAt(j);
                        if (candidates.remove(sibling)) {
                            if (!isTaskViewTask(sibling)) {
                                siblings.add(sibling);
                            }
                        } else if (sibling != current && sibling.isVisible()) {
                            canPromote = false;
                        } else if ((parent instanceof TaskDisplayArea) && parent.getConfiguration().windowConfiguration.isThunderbackWindow()) {
                            canPromote = false;
                        }
                    }
                }
                if (canPromote) {
                    candidates.add(parent);
                } else {
                    targets.addAll(siblings);
                }
            }
        }
        if (ProtoLogCache.WM_DEBUG_APP_TRANSITIONS_ANIM_enabled) {
            String protoLogParam02 = String.valueOf(apps);
            String protoLogParam12 = String.valueOf(targets);
            ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_APP_TRANSITIONS_ANIM, 1460759282, 0, (String) null, new Object[]{protoLogParam02, protoLogParam12});
        }
        return targets;
    }

    private void applyAnimations(ArraySet<ActivityRecord> openingApps, ArraySet<ActivityRecord> closingApps, int transit, WindowManager.LayoutParams animLp, boolean voiceInteraction) {
        if (transit != -1) {
            if (openingApps.isEmpty() && closingApps.isEmpty()) {
                return;
            }
            ArraySet<WindowContainer> openingWcs = getAnimationTargets(openingApps, closingApps, true);
            ArraySet<WindowContainer> closingWcs = getAnimationTargets(openingApps, closingApps, false);
            applyAnimations(openingWcs, openingApps, transit, true, animLp, voiceInteraction);
            applyAnimations(closingWcs, closingApps, transit, false, animLp, voiceInteraction);
            RecentsAnimationController rac = this.mService.getRecentsAnimationController();
            if (rac != null) {
                rac.sendTasksAppeared();
            }
            for (int i = 0; i < openingApps.size(); i++) {
                ((ActivityRecord) openingApps.valueAtUnchecked(i)).mOverrideTaskTransition = false;
            }
            for (int i2 = 0; i2 < closingApps.size(); i2++) {
                ((ActivityRecord) closingApps.valueAtUnchecked(i2)).mOverrideTaskTransition = false;
            }
            AccessibilityController accessibilityController = this.mDisplayContent.mWmService.mAccessibilityController;
            if (accessibilityController.hasCallbacks()) {
                accessibilityController.onAppWindowTransition(this.mDisplayContent.getDisplayId(), transit);
            }
        }
    }

    private void handleOpeningApps() {
        ArraySet<ActivityRecord> openingApps = this.mDisplayContent.mOpeningApps;
        int appsCount = openingApps.size();
        for (int i = 0; i < appsCount; i++) {
            ActivityRecord app = openingApps.valueAt(i);
            if (ProtoLogCache.WM_DEBUG_APP_TRANSITIONS_enabled) {
                String protoLogParam0 = String.valueOf(app);
                ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_APP_TRANSITIONS, 1628345525, 0, (String) null, new Object[]{protoLogParam0});
            }
            app.commitVisibility(true, false);
            WindowContainer wc = app.getAnimatingContainer(2, 1);
            if (wc == null || !wc.getAnimationSources().contains(app)) {
                this.mDisplayContent.mNoAnimationNotifyOnTransitionFinished.add(app.token);
            }
            app.updateReportedVisibilityLocked();
            app.waitingToShow = false;
            if (WindowManagerDebugConfig.SHOW_LIGHT_TRANSACTIONS) {
                Slog.i("WindowManager", ">>> OPEN TRANSACTION handleAppTransitionReady()");
            }
            this.mService.openSurfaceTransaction();
            try {
                app.showAllWindowsLocked();
                if (this.mDisplayContent.mAppTransition.isNextAppTransitionThumbnailUp()) {
                    app.attachThumbnailAnimation();
                } else if (this.mDisplayContent.mAppTransition.isNextAppTransitionOpenCrossProfileApps()) {
                    app.attachCrossProfileAppsThumbnailAnimation();
                }
            } finally {
                this.mService.closeSurfaceTransaction("handleAppTransitionReady");
                if (WindowManagerDebugConfig.SHOW_LIGHT_TRANSACTIONS) {
                    Slog.i("WindowManager", "<<< CLOSE TRANSACTION handleAppTransitionReady()");
                }
            }
        }
    }

    private void handleClosingApps() {
        ArraySet<ActivityRecord> closingApps = this.mDisplayContent.mClosingApps;
        int appsCount = closingApps.size();
        for (int i = 0; i < appsCount; i++) {
            ActivityRecord app = closingApps.valueAt(i);
            if (ProtoLogCache.WM_DEBUG_APP_TRANSITIONS_enabled) {
                String protoLogParam0 = String.valueOf(app);
                ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_APP_TRANSITIONS, 794570322, 0, (String) null, new Object[]{protoLogParam0});
            }
            app.commitVisibility(false, false);
            app.updateReportedVisibilityLocked();
            app.allDrawn = true;
            if (app.mStartingWindow != null && !app.mStartingWindow.mAnimatingExit) {
                app.removeStartingWindow();
            }
            if (this.mDisplayContent.mAppTransition.isNextAppTransitionThumbnailDown()) {
                app.attachThumbnailAnimation();
            }
        }
    }

    private void handleChangingApps(int transit) {
        ArraySet<WindowContainer> apps = this.mDisplayContent.mChangingContainers;
        int appsCount = apps.size();
        for (int i = 0; i < appsCount; i++) {
            WindowContainer wc = apps.valueAt(i);
            if (ProtoLogCache.WM_DEBUG_APP_TRANSITIONS_enabled) {
                String protoLogParam0 = String.valueOf(wc);
                ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_APP_TRANSITIONS, 186668272, 0, (String) null, new Object[]{protoLogParam0});
            }
            wc.applyAnimation(null, transit, true, false, null);
        }
    }

    private void handleNonAppWindowsInTransition(int transit, int flags) {
        if (transit == 20 && !WindowManagerService.sEnableRemoteKeyguardGoingAwayAnimation && (flags & 4) != 0 && (flags & 2) == 0 && (flags & 8) == 0) {
            Animation anim = this.mService.mPolicy.createKeyguardWallpaperExit((flags & 1) != 0);
            if (anim != null) {
                anim.scaleCurrentDuration(this.mService.getTransitionAnimationScaleLocked());
                this.mDisplayContent.mWallpaperController.startWallpaperAnimation(anim);
            }
        }
        if ((transit == 20 || transit == 21) && !WindowManagerService.sEnableRemoteKeyguardGoingAwayAnimation) {
            this.mDisplayContent.startKeyguardExitOnNonAppWindows(transit == 21, (flags & 1) != 0, (flags & 8) != 0);
        }
    }

    private boolean transitionGoodToGo(ArraySet<? extends WindowContainer> apps, ArrayMap<WindowContainer, Integer> outReasons) {
        ScreenRotationAnimation screenRotationAnimation;
        char c;
        int i;
        char c2;
        int i2;
        char c3 = 3;
        char c4 = 2;
        char c5 = 1;
        char c6 = 0;
        if (ProtoLogCache.WM_DEBUG_APP_TRANSITIONS_enabled) {
            long protoLogParam0 = apps.size();
            boolean protoLogParam1 = this.mService.mDisplayFrozen;
            boolean protoLogParam2 = this.mDisplayContent.mAppTransition.isTimeout();
            ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_APP_TRANSITIONS, 2045641491, 61, (String) null, new Object[]{Long.valueOf(protoLogParam0), Boolean.valueOf(protoLogParam1), Boolean.valueOf(protoLogParam2)});
        }
        if (this.mDisplayContent.mAppTransition.isTimeout()) {
            return true;
        }
        ScreenRotationAnimation screenRotationAnimation2 = this.mService.mRoot.getDisplayContent(0).getRotationAnimation();
        if (screenRotationAnimation2 != null && screenRotationAnimation2.isAnimating() && this.mDisplayContent.getDisplayRotation().needsUpdate()) {
            if (ProtoLogCache.WM_DEBUG_APP_TRANSITIONS_enabled) {
                ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_APP_TRANSITIONS, 628276090, 0, (String) null, (Object[]) null);
            }
            return false;
        }
        int i3 = 0;
        while (i3 < apps.size()) {
            WindowContainer wc = apps.valueAt(i3);
            ActivityRecord activity = getAppFromContainer(wc);
            if (activity == null) {
                screenRotationAnimation = screenRotationAnimation2;
                c = c3;
                c2 = c4;
            } else {
                if (ProtoLogCache.WM_DEBUG_APP_TRANSITIONS_enabled) {
                    String protoLogParam02 = String.valueOf(activity);
                    boolean protoLogParam12 = activity.allDrawn;
                    boolean protoLogParam22 = activity.isStartingWindowDisplayed();
                    boolean protoLogParam3 = activity.startingMoved;
                    boolean protoLogParam4 = activity.isRelaunching();
                    String protoLogParam5 = String.valueOf(activity.mStartingWindow);
                    ProtoLogGroup protoLogGroup = ProtoLogGroup.WM_DEBUG_APP_TRANSITIONS;
                    Object[] objArr = new Object[6];
                    objArr[c6] = protoLogParam02;
                    objArr[c5] = Boolean.valueOf(protoLogParam12);
                    objArr[2] = Boolean.valueOf(protoLogParam22);
                    c = 3;
                    objArr[3] = Boolean.valueOf(protoLogParam3);
                    i = 4;
                    objArr[4] = Boolean.valueOf(protoLogParam4);
                    objArr[5] = protoLogParam5;
                    screenRotationAnimation = screenRotationAnimation2;
                    ProtoLogImpl.v(protoLogGroup, 289967521, 1020, (String) null, objArr);
                } else {
                    screenRotationAnimation = screenRotationAnimation2;
                    c = c3;
                    i = 4;
                }
                boolean allDrawn = activity.allDrawn && !activity.isRelaunching();
                if (!allDrawn && !activity.isStartingWindowDisplayed() && !activity.startingMoved) {
                    return false;
                }
                if (allDrawn) {
                    c2 = 2;
                    outReasons.put(activity, 2);
                } else {
                    c2 = 2;
                    if (activity.mStartingData instanceof SplashScreenStartingData) {
                        i2 = 1;
                    } else {
                        i2 = i;
                    }
                    outReasons.put(activity, Integer.valueOf(i2));
                }
            }
            i3++;
            c4 = c2;
            c3 = c;
            screenRotationAnimation2 = screenRotationAnimation;
            c5 = 1;
            c6 = 0;
        }
        if (this.mDisplayContent.mAppTransition.isFetchingAppTransitionsSpecs()) {
            if (ProtoLogCache.WM_DEBUG_APP_TRANSITIONS_enabled) {
                ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_APP_TRANSITIONS, -1009117329, 0, (String) null, (Object[]) null);
                return false;
            }
            return false;
        } else if (this.mDisplayContent.mUnknownAppVisibilityController.allResolved()) {
            return !this.mWallpaperControllerLocked.isWallpaperVisible() || this.mWallpaperControllerLocked.wallpaperTransitionReady();
        } else if (ProtoLogCache.WM_DEBUG_APP_TRANSITIONS_enabled) {
            String protoLogParam03 = String.valueOf(this.mDisplayContent.mUnknownAppVisibilityController.getDebugMessage());
            ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_APP_TRANSITIONS, -379068494, 0, (String) null, new Object[]{protoLogParam03});
            return false;
        } else {
            return false;
        }
    }

    private boolean transitionGoodToGoForTaskFragments() {
        if (this.mDisplayContent.mAppTransition.isTimeout()) {
            return true;
        }
        ArraySet<Task> rootTasks = new ArraySet<>();
        for (int i = this.mDisplayContent.mOpeningApps.size() - 1; i >= 0; i--) {
            rootTasks.add(this.mDisplayContent.mOpeningApps.valueAt(i).getRootTask());
        }
        for (int i2 = this.mDisplayContent.mClosingApps.size() - 1; i2 >= 0; i2--) {
            rootTasks.add(this.mDisplayContent.mClosingApps.valueAt(i2).getRootTask());
        }
        for (int i3 = this.mDisplayContent.mChangingContainers.size() - 1; i3 >= 0; i3--) {
            rootTasks.add(findRootTaskFromContainer(this.mDisplayContent.mChangingContainers.valueAt(i3)));
        }
        int i4 = rootTasks.size();
        for (int i5 = i4 - 1; i5 >= 0; i5--) {
            Task rootTask = rootTasks.valueAt(i5);
            if (rootTask != null) {
                boolean notReady = rootTask.forAllLeafTaskFragments(new Predicate() { // from class: com.android.server.wm.AppTransitionController$$ExternalSyntheticLambda1
                    @Override // java.util.function.Predicate
                    public final boolean test(Object obj) {
                        return AppTransitionController.lambda$transitionGoodToGoForTaskFragments$8((TaskFragment) obj);
                    }
                });
                if (notReady) {
                    return false;
                }
            }
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$transitionGoodToGoForTaskFragments$8(TaskFragment taskFragment) {
        if (taskFragment.isReadyToTransit()) {
            return false;
        }
        if (ProtoLogCache.WM_DEBUG_APP_TRANSITIONS_enabled) {
            String protoLogParam0 = String.valueOf(taskFragment);
            ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_APP_TRANSITIONS, -1501564055, 0, (String) null, new Object[]{protoLogParam0});
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isTransitWithinTask(int transit, Task task) {
        if (task == null || !this.mDisplayContent.mChangingContainers.isEmpty()) {
            return false;
        }
        if (transit != 6 && transit != 7 && transit != 18) {
            return false;
        }
        Iterator<ActivityRecord> it = this.mDisplayContent.mOpeningApps.iterator();
        while (it.hasNext()) {
            ActivityRecord activity = it.next();
            Task activityTask = activity.getTask();
            if (activityTask != task) {
                return false;
            }
        }
        Iterator<ActivityRecord> it2 = this.mDisplayContent.mClosingApps.iterator();
        while (it2.hasNext()) {
            ActivityRecord activity2 = it2.next();
            if (activity2.getTask() != task) {
                return false;
            }
        }
        return true;
    }

    private static boolean canBeWallpaperTarget(ArraySet<ActivityRecord> apps) {
        for (int i = apps.size() - 1; i >= 0; i--) {
            if (apps.valueAt(i).windowsCanBeWallpaperTarget()) {
                return true;
            }
        }
        return false;
    }

    private static ActivityRecord getTopApp(ArraySet<? extends WindowContainer> apps, boolean ignoreInvisible) {
        int prefixOrderIndex;
        int topPrefixOrderIndex = Integer.MIN_VALUE;
        ActivityRecord topApp = null;
        for (int i = apps.size() - 1; i >= 0; i--) {
            ActivityRecord app = getAppFromContainer(apps.valueAt(i));
            if (app != null && ((!ignoreInvisible || app.isVisible()) && (prefixOrderIndex = app.getPrefixOrderIndex()) > topPrefixOrderIndex)) {
                topPrefixOrderIndex = prefixOrderIndex;
                topApp = app;
            }
        }
        return topApp;
    }

    private static ActivityRecord getBottomApp(ArraySet<? extends WindowContainer> apps, boolean ignoreInvisible) {
        int prefixOrderIndex;
        int bottomPrefixOrderIndex = Integer.MAX_VALUE;
        ActivityRecord bottomApp = null;
        for (int i = 0; i <= apps.size() - 1; i++) {
            ActivityRecord app = getAppFromContainer(apps.valueAt(i));
            if (app != null && ((!ignoreInvisible || app.isVisible()) && (prefixOrderIndex = app.getPrefixOrderIndex()) < bottomPrefixOrderIndex)) {
                bottomPrefixOrderIndex = prefixOrderIndex;
                bottomApp = app;
            }
        }
        return bottomApp;
    }
}
