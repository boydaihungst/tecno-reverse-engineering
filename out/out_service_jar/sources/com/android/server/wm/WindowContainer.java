package com.android.server.wm;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Debug;
import android.os.IBinder;
import android.os.SystemProperties;
import android.os.Trace;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Log;
import android.util.Pair;
import android.util.Pools;
import android.util.RotationUtils;
import android.util.Slog;
import android.util.SparseArray;
import android.util.proto.ProtoOutputStream;
import android.view.DisplayInfo;
import android.view.InsetsSource;
import android.view.InsetsState;
import android.view.MagnificationSpec;
import android.view.RemoteAnimationDefinition;
import android.view.RemoteAnimationTarget;
import android.view.SurfaceControl;
import android.view.SurfaceControlViewHost;
import android.view.SurfaceSession;
import android.view.TaskTransitionSpec;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.window.IWindowContainerToken;
import android.window.WindowContainerToken;
import com.android.internal.graphics.ColorUtils;
import com.android.internal.protolog.ProtoLogGroup;
import com.android.internal.protolog.ProtoLogImpl;
import com.android.internal.util.ToBooleanFunction;
import com.android.server.backup.BackupAgentTimeoutParameters;
import com.android.server.wm.BLASTSyncEngine;
import com.android.server.wm.RemoteAnimationController;
import com.android.server.wm.SurfaceAnimator;
import com.android.server.wm.SurfaceFreezer;
import com.android.server.wm.WindowContainer;
import com.android.server.wm.utils.TranFpUnlockStateController;
import com.transsion.hubcore.griffin.ITranGriffinFeature;
import com.transsion.hubcore.multiwindow.ITranMultiWindow;
import com.transsion.hubcore.server.policy.ITranPhoneWindowManager;
import com.transsion.hubcore.server.wm.ITranActivityTaskManagerService;
import com.transsion.hubcore.server.wm.ITranWindowManagerService;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class WindowContainer<E extends WindowContainer> extends ConfigurationContainer<E> implements Comparable<WindowContainer>, SurfaceAnimator.Animatable, SurfaceFreezer.Freezable, InsetsControlTarget {
    private static final boolean ENBLE_LITE_LAUNCHER_UNLOCK = "1".equals(SystemProperties.get("ro.transsion_enable_lite_launcher_unlock_support"));
    static final int POSITION_BOTTOM = Integer.MIN_VALUE;
    static final int POSITION_TOP = Integer.MAX_VALUE;
    public static final int SYNC_STATE_NONE = 0;
    public static final int SYNC_STATE_READY = 2;
    public static final int SYNC_STATE_WAITING_FOR_DRAW = 1;
    private static final String TAG = "WindowManager";
    SurfaceControl mAnimationBoundsLayer;
    private SurfaceControl mAnimationLeash;
    private boolean mCommittedReparentToAnimationLeash;
    protected InsetsSourceProvider mControllableInsetProvider;
    protected DisplayContent mDisplayContent;
    private MagnificationSpec mLastMagnificationSpec;
    protected WindowContainer mLastOrientationSource;
    boolean mLaunchTaskBehind;
    boolean mNeedsAnimationBoundsLayer;
    boolean mNeedsZBoost;
    protected TrustedOverlayHost mOverlayHost;
    private final SurfaceControl.Transaction mPendingTransaction;
    boolean mReparenting;
    protected final SurfaceAnimator mSurfaceAnimator;
    protected SurfaceControl mSurfaceControl;
    final SurfaceFreezer mSurfaceFreezer;
    final SurfaceControl.Transaction mSyncTransaction;
    WindowContainerThumbnail mThumbnail;
    int mTransit;
    int mTransitFlags;
    final TransitionController mTransitionController;
    protected final WindowManagerService mWmService;
    private WindowContainer<WindowContainer> mParent = null;
    SparseArray<InsetsSourceProvider> mLocalInsetsSourceProviders = null;
    protected SparseArray<InsetsSource> mProvidedInsetsSources = null;
    protected final WindowList<E> mChildren = new WindowList<>();
    protected int mOrientation = -1;
    private final Pools.SynchronizedPool<WindowContainer<E>.ForAllWindowsConsumerWrapper> mConsumerWrapperPool = new Pools.SynchronizedPool<>(3);
    private int mLastLayer = 0;
    private SurfaceControl mLastRelativeToLayer = null;
    final ArrayList<WindowState> mWaitingForDrawn = new ArrayList<>();
    private final ArraySet<WindowContainer> mSurfaceAnimationSources = new ArraySet<>();
    private final Point mTmpPos = new Point();
    protected final Point mLastSurfacePosition = new Point();
    protected int mLastDeltaRotation = 0;
    private int mTreeWeight = 1;
    private int mSyncTransactionCommitCallbackDepth = 0;
    final Point mTmpPoint = new Point();
    protected final Rect mTmpRect = new Rect();
    final Rect mTmpPrevBounds = new Rect();
    private boolean mIsFocusable = true;
    RemoteToken mRemoteToken = null;
    BLASTSyncEngine.SyncGroup mSyncGroup = null;
    int mSyncState = 0;
    private final List<WindowContainerListener> mListeners = new ArrayList();
    private final LinkedList<WindowContainer> mTmpChain1 = new LinkedList<>();
    private final LinkedList<WindowContainer> mTmpChain2 = new LinkedList<>();

    /* loaded from: classes2.dex */
    public interface AnimationFlags {
        public static final int CHILDREN = 4;
        public static final int PARENTS = 2;
        public static final int TRANSITION = 1;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public interface IAnimationStarter {
        void startAnimation(SurfaceControl.Transaction transaction, AnimationAdapter animationAdapter, boolean z, int i, AnimationAdapter animationAdapter2);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public interface PreAssignChildLayersCallback {
        void onPreAssignChildLayers();
    }

    /* loaded from: classes2.dex */
    @interface SyncState {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public WindowContainer(WindowManagerService wms) {
        this.mWmService = wms;
        this.mTransitionController = wms.mAtmService.getTransitionController();
        this.mPendingTransaction = wms.mTransactionFactory.get();
        this.mSyncTransaction = wms.mTransactionFactory.get();
        this.mSurfaceAnimator = new SurfaceAnimator(this, new SurfaceAnimator.OnAnimationFinishedCallback() { // from class: com.android.server.wm.WindowContainer$$ExternalSyntheticLambda4
            @Override // com.android.server.wm.SurfaceAnimator.OnAnimationFinishedCallback
            public final void onAnimationFinished(int i, AnimationAdapter animationAdapter) {
                WindowContainer.this.onAnimationFinished(i, animationAdapter);
            }
        }, wms);
        this.mSurfaceFreezer = new SurfaceFreezer(this, wms);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateAboveInsetsState(InsetsState aboveInsetsState, SparseArray<InsetsSourceProvider> localInsetsSourceProvidersFromParent, ArraySet<WindowState> insetsChangedWindows) {
        SparseArray<InsetsSourceProvider> mergedLocalInsetsSourceProviders = localInsetsSourceProvidersFromParent;
        SparseArray<InsetsSourceProvider> sparseArray = this.mLocalInsetsSourceProviders;
        if (sparseArray != null && sparseArray.size() != 0) {
            mergedLocalInsetsSourceProviders = createShallowCopy(mergedLocalInsetsSourceProviders);
            for (int i = 0; i < this.mLocalInsetsSourceProviders.size(); i++) {
                mergedLocalInsetsSourceProviders.put(this.mLocalInsetsSourceProviders.keyAt(i), this.mLocalInsetsSourceProviders.valueAt(i));
            }
        }
        for (int i2 = this.mChildren.size() - 1; i2 >= 0; i2--) {
            this.mChildren.get(i2).updateAboveInsetsState(aboveInsetsState, mergedLocalInsetsSourceProviders, insetsChangedWindows);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static <T> SparseArray<T> createShallowCopy(SparseArray<T> inputArray) {
        SparseArray<T> copyOfInput = new SparseArray<>(inputArray.size());
        for (int i = 0; i < inputArray.size(); i++) {
            copyOfInput.append(inputArray.keyAt(i), inputArray.valueAt(i));
        }
        return copyOfInput;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void addLocalRectInsetsSourceProvider(Rect providerFrame, int[] insetsTypes) {
        if (insetsTypes == null || insetsTypes.length == 0) {
            throw new IllegalArgumentException("Insets type not specified.");
        }
        if (this.mLocalInsetsSourceProviders == null) {
            this.mLocalInsetsSourceProviders = new SparseArray<>();
        }
        for (int i = 0; i < insetsTypes.length; i++) {
            if (this.mLocalInsetsSourceProviders.get(insetsTypes[i]) != null && WindowManagerDebugConfig.DEBUG) {
                Slog.d("WindowManager", "The local insets provider for this type " + insetsTypes[i] + " already exists. Overwriting");
            }
            InsetsSourceProvider insetsSourceProvider = new RectInsetsSourceProvider(new InsetsSource(insetsTypes[i]), this.mDisplayContent.getInsetsStateController(), this.mDisplayContent);
            this.mLocalInsetsSourceProviders.put(insetsTypes[i], insetsSourceProvider);
            ((RectInsetsSourceProvider) insetsSourceProvider).setRect(providerFrame);
        }
        this.mDisplayContent.getInsetsStateController().updateAboveInsetsState(true);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void removeLocalInsetsSourceProvider(int[] insetsTypes) {
        if (insetsTypes == null || insetsTypes.length == 0) {
            throw new IllegalArgumentException("Insets type not specified.");
        }
        if (this.mLocalInsetsSourceProviders == null) {
            return;
        }
        for (int i = 0; i < insetsTypes.length; i++) {
            InsetsSourceProvider insetsSourceProvider = this.mLocalInsetsSourceProviders.get(insetsTypes[i]);
            if (insetsSourceProvider == null) {
                if (WindowManagerDebugConfig.DEBUG) {
                    Slog.d("WindowManager", "Given insets type " + insetsTypes[i] + " doesn't have a local insetsSourceProvider.");
                }
            } else {
                this.mLocalInsetsSourceProviders.remove(insetsTypes[i]);
            }
        }
        this.mDisplayContent.getInsetsStateController().updateAboveInsetsState(true);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setControllableInsetProvider(InsetsSourceProvider insetProvider) {
        this.mControllableInsetProvider = insetProvider;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public InsetsSourceProvider getControllableInsetProvider() {
        return this.mControllableInsetProvider;
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.wm.ConfigurationContainer
    public final WindowContainer getParent() {
        return this.mParent;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.wm.ConfigurationContainer
    public int getChildCount() {
        return this.mChildren.size();
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.wm.ConfigurationContainer
    public E getChildAt(int index) {
        return this.mChildren.get(index);
    }

    @Override // com.android.server.wm.ConfigurationContainer
    public void onConfigurationChanged(Configuration newParentConfig) {
        super.onConfigurationChanged(newParentConfig);
        updateSurfacePositionNonOrganized();
        SurfaceControl surfaceControl = this.mSurfaceControl;
        if (surfaceControl != null && surfaceControl.isValid()) {
            getSyncTransaction().setSkipParentIntersect(this.mSurfaceControl, isOSFullDialog());
        }
        scheduleAnimation();
        TrustedOverlayHost trustedOverlayHost = this.mOverlayHost;
        if (trustedOverlayHost != null) {
            trustedOverlayHost.dispatchConfigurationChanged(getConfiguration());
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void reparent(WindowContainer newParent, int position) {
        if (newParent == null) {
            throw new IllegalArgumentException("reparent: can't reparent to null " + this);
        }
        if (newParent == this) {
            throw new IllegalArgumentException("Can not reparent to itself " + this);
        }
        WindowContainer oldParent = this.mParent;
        if (this.mParent == newParent) {
            throw new IllegalArgumentException("WC=" + this + " already child of " + this.mParent);
        }
        if (oldParent == null) {
            return;
        }
        hookScreenSplitPackageChanged(newParent, oldParent, position);
        DisplayContent prevDc = oldParent.getDisplayContent();
        DisplayContent dc = newParent.getDisplayContent();
        this.mReparenting = true;
        oldParent.removeChild(this);
        newParent.addChild(this, position);
        this.mReparenting = false;
        dc.setLayoutNeeded();
        if (prevDc != dc) {
            onDisplayChanged(dc);
            prevDc.setLayoutNeeded();
        }
        getDisplayContent().layoutAndAssignWindowLayersIfNeeded();
        onParentChanged(newParent, oldParent);
        onSyncReparent(oldParent, newParent);
    }

    protected final void setParent(WindowContainer<WindowContainer> parent) {
        DisplayContent displayContent;
        if (parent == null) {
            Slog.d("WindowManager", "setParent old=" + this.mParent + ",new=" + parent + ",this window=" + this + ",callers=" + Debug.getCallers(6));
        }
        WindowContainer oldParent = this.mParent;
        this.mParent = parent;
        if (parent != null) {
            parent.onChildAdded(this);
        } else if (this.mSurfaceAnimator.hasLeash()) {
            if (asTask() != null && TextUtils.equals(asTask().mCallingPackage, "com.xui.xhide")) {
                Slog.d("WindowManager", "setParent don't cancel animation mParent=" + this.mParent + ",this window=" + this);
            } else {
                this.mSurfaceAnimator.cancelAnimation();
            }
        }
        if (!this.mReparenting) {
            onSyncReparent(oldParent, this.mParent);
            WindowContainer<WindowContainer> windowContainer = this.mParent;
            if (windowContainer != null && (displayContent = windowContainer.mDisplayContent) != null && this.mDisplayContent != displayContent) {
                onDisplayChanged(displayContent);
            }
            onParentChanged(this.mParent, oldParent);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.wm.ConfigurationContainer
    public void onParentChanged(ConfigurationContainer newParent, ConfigurationContainer oldParent) {
        onParentChanged(newParent, oldParent, null);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onParentChanged(ConfigurationContainer newParent, ConfigurationContainer oldParent, PreAssignChildLayersCallback callback) {
        super.onParentChanged(newParent, oldParent);
        if (this.mParent == null) {
            return;
        }
        if (this.mSurfaceControl == null) {
            createSurfaceControl(false);
        } else {
            reparentSurfaceControl(getSyncTransaction(), this.mParent.mSurfaceControl);
        }
        if (isOSFullDialog()) {
            getSyncTransaction().setSkipParentIntersect(this.mSurfaceControl, true);
        }
        if (callback != null) {
            callback.onPreAssignChildLayers();
        }
        this.mParent.assignChildLayers();
        scheduleAnimation();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void createSurfaceControl(boolean force) {
        setInitialSurfaceControlProperties(makeSurface());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setInitialSurfaceControlProperties(SurfaceControl.Builder b) {
        setSurfaceControl(b.setCallsite("WindowContainer.setInitialSurfaceControlProperties").build());
        if (showSurfaceOnCreation()) {
            getSyncTransaction().show(this.mSurfaceControl);
        }
        onSurfaceShown(getSyncTransaction());
        updateSurfacePositionNonOrganized();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void migrateToNewSurfaceControl(SurfaceControl.Transaction t) {
        t.remove(this.mSurfaceControl);
        this.mLastSurfacePosition.set(0, 0);
        this.mLastDeltaRotation = 0;
        SurfaceControl.Builder b = this.mWmService.makeSurfaceBuilder(null).setContainerLayer().setName(getName());
        setInitialSurfaceControlProperties(b);
        SurfaceControl surfaceControl = this.mSurfaceControl;
        WindowContainer<WindowContainer> windowContainer = this.mParent;
        t.reparent(surfaceControl, windowContainer != null ? windowContainer.getSurfaceControl() : null);
        SurfaceControl surfaceControl2 = this.mLastRelativeToLayer;
        if (surfaceControl2 != null) {
            t.setRelativeLayer(this.mSurfaceControl, surfaceControl2, this.mLastLayer);
        } else {
            t.setLayer(this.mSurfaceControl, this.mLastLayer);
        }
        for (int i = 0; i < this.mChildren.size(); i++) {
            SurfaceControl sc = this.mChildren.get(i).getSurfaceControl();
            if (sc != null) {
                t.reparent(sc, this.mSurfaceControl);
            }
        }
        TrustedOverlayHost trustedOverlayHost = this.mOverlayHost;
        if (trustedOverlayHost != null) {
            trustedOverlayHost.setParent(t, this.mSurfaceControl);
        }
        scheduleAnimation();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onSurfaceShown(SurfaceControl.Transaction t) {
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void addChild(E child, Comparator<E> comparator) {
        if (!child.mReparenting && child.getParent() != null) {
            throw new IllegalArgumentException("addChild: container=" + child.getName() + " is already a child of container=" + child.getParent().getName() + " can't add to container=" + getName());
        }
        int positionToAdd = -1;
        if (comparator != null) {
            int count = this.mChildren.size();
            int i = 0;
            while (true) {
                if (i >= count) {
                    break;
                } else if (comparator.compare(child, this.mChildren.get(i)) >= 0) {
                    i++;
                } else {
                    positionToAdd = i;
                    break;
                }
            }
        }
        if (positionToAdd == -1) {
            this.mChildren.add(child);
        } else {
            this.mChildren.add(positionToAdd, child);
        }
        child.setParent(this);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void addChild(E child, int index) {
        if (!child.mReparenting && child.getParent() != null) {
            throw new IllegalArgumentException("addChild: container=" + child.getName() + " is already a child of container=" + child.getParent().getName() + " can't add to container=" + getName() + "\n callers=" + Debug.getCallers(15, "\n"));
        }
        if ((index < 0 && index != Integer.MIN_VALUE) || (index > this.mChildren.size() && index != Integer.MAX_VALUE)) {
            throw new IllegalArgumentException("addChild: invalid position=" + index + ", children number=" + this.mChildren.size());
        }
        if (index == Integer.MAX_VALUE) {
            index = this.mChildren.size();
        } else if (index == Integer.MIN_VALUE) {
            index = 0;
        }
        this.mChildren.add(index, child);
        child.setParent(this);
    }

    private void onChildAdded(WindowContainer child) {
        this.mTreeWeight += child.mTreeWeight;
        for (WindowContainer parent = getParent(); parent != null; parent = parent.getParent()) {
            parent.mTreeWeight += child.mTreeWeight;
        }
        onChildPositionChanged(child);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void removeChild(E child) {
        if (this.mChildren.remove(child)) {
            onChildRemoved(child);
            if (!child.mReparenting) {
                child.setParent(null);
                return;
            }
            return;
        }
        throw new IllegalArgumentException("removeChild: container=" + child.getName() + " is not a child of container=" + getName());
    }

    private void onChildRemoved(WindowContainer child) {
        this.mTreeWeight -= child.mTreeWeight;
        for (WindowContainer parent = getParent(); parent != null; parent = parent.getParent()) {
            parent.mTreeWeight -= child.mTreeWeight;
        }
        onChildPositionChanged(child);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void removeImmediately() {
        DisplayContent dc = getDisplayContent();
        if (dc != null) {
            this.mSurfaceFreezer.unfreeze(getSyncTransaction());
        }
        while (!this.mChildren.isEmpty()) {
            E child = this.mChildren.peekLast();
            child.removeImmediately();
            if (this.mChildren.remove(child)) {
                onChildRemoved(child);
            }
        }
        if (this.mSurfaceControl != null) {
            getSyncTransaction().remove(this.mSurfaceControl);
            setSurfaceControl(null);
            this.mLastSurfacePosition.set(0, 0);
            this.mLastDeltaRotation = 0;
            scheduleAnimation();
        }
        TrustedOverlayHost trustedOverlayHost = this.mOverlayHost;
        if (trustedOverlayHost != null) {
            trustedOverlayHost.release();
            this.mOverlayHost = null;
        }
        WindowContainer<WindowContainer> windowContainer = this.mParent;
        if (windowContainer != null) {
            windowContainer.removeChild(this);
        }
        for (int i = this.mListeners.size() - 1; i >= 0; i--) {
            this.mListeners.get(i).onRemoved();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getTreeWeight() {
        return this.mTreeWeight;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getPrefixOrderIndex() {
        WindowContainer<WindowContainer> windowContainer = this.mParent;
        if (windowContainer == null) {
            return 0;
        }
        return windowContainer.getPrefixOrderIndex(this);
    }

    private int getPrefixOrderIndex(WindowContainer child) {
        WindowContainer childI;
        int order = 0;
        for (int i = 0; i < this.mChildren.size() && child != (childI = this.mChildren.get(i)); i++) {
            order += childI.mTreeWeight;
        }
        WindowContainer<WindowContainer> windowContainer = this.mParent;
        if (windowContainer != null) {
            order += windowContainer.getPrefixOrderIndex(this);
        }
        return order + 1;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void removeIfPossible() {
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            WindowContainer wc = this.mChildren.get(i);
            wc.removeIfPossible();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasChild(E child) {
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            E current = this.mChildren.get(i);
            if (current == child || current.hasChild(child)) {
                return true;
            }
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isDescendantOf(WindowContainer ancestor) {
        WindowContainer parent = getParent();
        if (parent == ancestor) {
            return true;
        }
        return parent != null && parent.isDescendantOf(ancestor);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void positionChildAt(int position, E child, boolean includingParents) {
        if (child.getParent() != this) {
            throw new IllegalArgumentException("positionChildAt: container=" + child.getName() + " is not a child of container=" + getName() + " current parent=" + child.getParent());
        }
        if (position >= this.mChildren.size() - 1) {
            position = Integer.MAX_VALUE;
        } else if (position <= 0) {
            position = Integer.MIN_VALUE;
        }
        switch (position) {
            case Integer.MIN_VALUE:
                if (this.mChildren.peekFirst() != child) {
                    this.mChildren.remove(child);
                    this.mChildren.addFirst(child);
                    onChildPositionChanged(child);
                }
                if (includingParents && getParent() != null) {
                    getParent().positionChildAt(Integer.MIN_VALUE, this, true);
                    return;
                }
                return;
            case Integer.MAX_VALUE:
                if (this.mChildren.peekLast() != child) {
                    this.mChildren.remove(child);
                    this.mChildren.add(child);
                    onChildPositionChanged(child);
                }
                if (includingParents && getParent() != null) {
                    getParent().positionChildAt(Integer.MAX_VALUE, this, true);
                    return;
                }
                return;
            default:
                if (this.mChildren.indexOf(child) != position) {
                    this.mChildren.remove(child);
                    this.mChildren.add(position, child);
                    onChildPositionChanged(child);
                    return;
                }
                return;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onChildPositionChanged(WindowContainer child) {
    }

    @Override // com.android.server.wm.ConfigurationContainer
    public void onRequestedOverrideConfigurationChanged(Configuration overrideConfiguration) {
        int diff = diffRequestedOverrideBounds(overrideConfiguration.windowConfiguration.getBounds());
        super.onRequestedOverrideConfigurationChanged(overrideConfiguration);
        WindowContainer<WindowContainer> windowContainer = this.mParent;
        if (windowContainer != null) {
            windowContainer.onDescendantOverrideConfigurationChanged();
        }
        if (diff == 0) {
            return;
        }
        if ((diff & 2) == 2) {
            onResize();
        } else {
            onMovedByResize();
        }
    }

    void onDescendantOverrideConfigurationChanged() {
        WindowContainer<WindowContainer> windowContainer = this.mParent;
        if (windowContainer != null) {
            windowContainer.onDescendantOverrideConfigurationChanged();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onDisplayChanged(DisplayContent dc) {
        DisplayContent displayContent = this.mDisplayContent;
        if (displayContent != null && displayContent.mChangingContainers.remove(this)) {
            this.mSurfaceFreezer.unfreeze(getSyncTransaction());
        }
        this.mDisplayContent = dc;
        if (dc != null && dc != this) {
            dc.getPendingTransaction().merge(this.mPendingTransaction);
        }
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            WindowContainer child = this.mChildren.get(i);
            child.onDisplayChanged(dc);
        }
        for (int i2 = this.mListeners.size() - 1; i2 >= 0; i2--) {
            this.mListeners.get(i2).onDisplayChanged(dc);
        }
    }

    public SparseArray<InsetsSource> getProvidedInsetsSources() {
        if (this.mProvidedInsetsSources == null) {
            this.mProvidedInsetsSources = new SparseArray<>();
        }
        return this.mProvidedInsetsSources;
    }

    public DisplayContent getDisplayContent() {
        return this.mDisplayContent;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public DisplayArea getDisplayArea() {
        WindowContainer parent = getParent();
        if (parent != null) {
            return parent.getDisplayArea();
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public RootDisplayArea getRootDisplayArea() {
        WindowContainer parent = getParent();
        if (parent != null) {
            return parent.getRootDisplayArea();
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public TaskDisplayArea getTaskDisplayArea() {
        WindowContainer parent = getParent();
        if (parent != null) {
            return parent.getTaskDisplayArea();
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isAttached() {
        WindowContainer parent = getParent();
        return parent != null && parent.isAttached();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setWaitingForDrawnIfResizingChanged() {
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            WindowContainer wc = this.mChildren.get(i);
            wc.setWaitingForDrawnIfResizingChanged();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onResize() {
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            WindowContainer wc = this.mChildren.get(i);
            wc.onParentResize();
        }
    }

    void onParentResize() {
        if (hasOverrideBounds()) {
            return;
        }
        onResize();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onMovedByResize() {
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            WindowContainer wc = this.mChildren.get(i);
            wc.onMovedByResize();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void resetDragResizingChangeReported() {
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            WindowContainer wc = this.mChildren.get(i);
            wc.resetDragResizingChangeReported();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean canCustomizeAppTransition() {
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public final boolean isAnimating(int flags, int typesToCheck) {
        return getAnimatingContainer(flags, typesToCheck) != null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Deprecated
    public final boolean isAnimating(int flags) {
        return isAnimating(flags, -1);
    }

    boolean isWaitingForTransitionStart() {
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isAppTransitioning() {
        return getActivity(new Predicate() { // from class: com.android.server.wm.WindowContainer$$ExternalSyntheticLambda14
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                boolean isAnimating;
                isAnimating = ((ActivityRecord) obj).isAnimating(3);
                return isAnimating;
            }
        }) != null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public final boolean isAnimating() {
        return isAnimating(0);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isChangingAppTransition() {
        DisplayContent displayContent = this.mDisplayContent;
        return displayContent != null && displayContent.mChangingContainers.contains(this);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean inTransition() {
        return this.mTransitionController.inTransition(this);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isExitAnimationRunningSelfOrChild() {
        if (!this.mTransitionController.isShellTransitionsEnabled()) {
            return isAnimating(5, 25);
        }
        if (this.mTransitionController.isCollecting(this)) {
            return true;
        }
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            WindowContainer child = this.mChildren.get(i);
            if (child.isExitAnimationRunningSelfOrChild()) {
                return true;
            }
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void sendAppVisibilityToClients() {
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            WindowContainer wc = this.mChildren.get(i);
            wc.sendAppVisibilityToClients();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasContentToDisplay() {
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            WindowContainer wc = this.mChildren.get(i);
            if (wc.hasContentToDisplay()) {
                return true;
            }
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isVisible() {
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            WindowContainer wc = this.mChildren.get(i);
            if (wc.isVisible()) {
                return true;
            }
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isVisibleRequested() {
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            WindowContainer child = this.mChildren.get(i);
            if (child.isVisibleRequested()) {
                return true;
            }
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onChildVisibilityRequested(boolean visible) {
        if (!visible) {
            this.mSurfaceFreezer.unfreeze(getSyncTransaction());
        }
        WindowContainer parent = getParent();
        if (parent != null) {
            parent.onChildVisibilityRequested(visible);
        }
    }

    void writeIdentifierToProto(ProtoOutputStream proto, long fieldId) {
        long token = proto.start(fieldId);
        proto.write(CompanionMessage.MESSAGE_ID, System.identityHashCode(this));
        proto.write(1120986464258L, -10000);
        proto.write(1138166333443L, "WindowContainer");
        proto.end(token);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isFocusable() {
        WindowContainer parent = getParent();
        return (parent == null || parent.isFocusable()) && this.mIsFocusable;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean setFocusable(boolean focusable) {
        if (this.mIsFocusable == focusable) {
            return false;
        }
        this.mIsFocusable = focusable;
        return true;
    }

    /* JADX DEBUG: Multi-variable search result rejected for r0v0, resolved type: com.android.server.wm.WindowContainer */
    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: Multi-variable type inference failed */
    public boolean isOnTop() {
        WindowContainer parent = getParent();
        return parent != 0 && parent.getTopChild() == this && parent.isOnTop();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public E getTopChild() {
        return this.mChildren.peekLast();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean handleCompleteDeferredRemoval() {
        boolean stillDeferringRemoval = false;
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            WindowContainer wc = this.mChildren.get(i);
            stillDeferringRemoval |= wc.handleCompleteDeferredRemoval();
            if (!hasChild()) {
                return false;
            }
        }
        return stillDeferringRemoval;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void checkAppWindowsReadyToShow() {
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            WindowContainer wc = this.mChildren.get(i);
            wc.checkAppWindowsReadyToShow();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onAppTransitionDone() {
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            WindowContainer wc = this.mChildren.get(i);
            wc.onAppTransitionDone();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean onDescendantOrientationChanged(WindowContainer requestingContainer) {
        WindowContainer parent = getParent();
        if (parent == null) {
            return false;
        }
        return parent.onDescendantOrientationChanged(requestingContainer);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean handlesOrientationChangeFromDescendant() {
        WindowContainer parent = getParent();
        return parent != null && parent.handlesOrientationChangeFromDescendant();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getRequestedConfigurationOrientation() {
        return getRequestedConfigurationOrientation(false);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getRequestedConfigurationOrientation(boolean forDisplay) {
        int requestedOrientation = this.mOrientation;
        RootDisplayArea root = getRootDisplayArea();
        if (forDisplay && root != null && root.isOrientationDifferentFromDisplay()) {
            requestedOrientation = ActivityInfo.reverseOrientation(this.mOrientation);
        }
        if (requestedOrientation == 5) {
            DisplayContent displayContent = this.mDisplayContent;
            if (displayContent != null) {
                return displayContent.getNaturalOrientation();
            }
            return 0;
        } else if (requestedOrientation == 14) {
            return getConfiguration().orientation;
        } else {
            if (ActivityInfo.isFixedOrientationLandscape(requestedOrientation)) {
                return 2;
            }
            if (ActivityInfo.isFixedOrientationPortrait(requestedOrientation)) {
                return 1;
            }
            return 0;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setOrientation(int orientation) {
        setOrientation(orientation, null);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setOrientation(int orientation, WindowContainer requestingContainer) {
        if (this.mOrientation == orientation) {
            return;
        }
        this.mOrientation = orientation;
        WindowContainer parent = getParent();
        if (parent != null) {
            if (getConfiguration().orientation != getRequestedConfigurationOrientation() && (inMultiWindowMode() || !handlesOrientationChangeFromDescendant())) {
                onConfigurationChanged(parent.getConfiguration());
            }
            onDescendantOrientationChanged(requestingContainer);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getOrientation() {
        return getOrientation(this.mOrientation);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getOrientation(int candidate) {
        this.mLastOrientationSource = null;
        if (providesOrientation()) {
            int i = this.mOrientation;
            if (i != -2 && i != -1) {
                this.mLastOrientationSource = this;
                return i;
            }
            for (int i2 = this.mChildren.size() - 1; i2 >= 0; i2--) {
                WindowContainer wc = this.mChildren.get(i2);
                int orientation = wc.getOrientation(candidate == 3 ? 3 : -2);
                if (orientation == 3) {
                    candidate = orientation;
                    this.mLastOrientationSource = wc;
                } else if (orientation != -2 && (wc.providesOrientation() || orientation != -1)) {
                    if (ProtoLogCache.WM_DEBUG_ORIENTATION_enabled) {
                        String protoLogParam0 = String.valueOf(wc.toString());
                        long protoLogParam1 = orientation;
                        String protoLogParam2 = String.valueOf(ActivityInfo.screenOrientationToString(orientation));
                        ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_ORIENTATION, -1108775960, 4, (String) null, new Object[]{protoLogParam0, Long.valueOf(protoLogParam1), protoLogParam2});
                    }
                    this.mLastOrientationSource = wc;
                    return orientation;
                }
            }
            return candidate;
        }
        return -2;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public WindowContainer getLastOrientationSource() {
        WindowContainer nextSource;
        WindowContainer source = this.mLastOrientationSource;
        if (source != null && source != this && (nextSource = source.getLastOrientationSource()) != null) {
            return nextSource;
        }
        return source;
    }

    boolean providesOrientation() {
        return fillsParent();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean fillsParent() {
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void switchUser(int userId) {
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            this.mChildren.get(i).switchUser(userId);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean showToCurrentUser() {
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void forAllWindowContainers(Consumer<WindowContainer> callback) {
        callback.accept(this);
        int count = this.mChildren.size();
        for (int i = 0; i < count; i++) {
            this.mChildren.get(i).forAllWindowContainers(callback);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean forAllWindows(ToBooleanFunction<WindowState> callback, boolean traverseTopToBottom) {
        WindowList<E> windowList = this.mChildren;
        if (windowList != null && windowList.size() > 100) {
            Slog.e("WindowManager", "forAllWindows,Many Children in this container: " + this + ", mChildren.size():" + this.mChildren.size());
        }
        if (traverseTopToBottom) {
            for (int i = this.mChildren.size() - 1; i >= 0; i--) {
                if (this.mChildren.get(i).forAllWindows(callback, traverseTopToBottom)) {
                    return true;
                }
            }
            return false;
        }
        int count = this.mChildren.size();
        for (int i2 = 0; i2 < count; i2++) {
            if (this.mChildren.get(i2).forAllWindows(callback, traverseTopToBottom)) {
                return true;
            }
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void forAllWindows(Consumer<WindowState> callback, boolean traverseTopToBottom) {
        WindowContainer<E>.ForAllWindowsConsumerWrapper wrapper = obtainConsumerWrapper(callback);
        forAllWindows(wrapper, traverseTopToBottom);
        wrapper.release();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean forAllActivities(Predicate<ActivityRecord> callback) {
        return forAllActivities(callback, true);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean forAllActivities(Predicate<ActivityRecord> callback, boolean traverseTopToBottom) {
        WindowList<E> windowList = this.mChildren;
        if (windowList != null && windowList.size() > 100) {
            Slog.e("WindowManager", "forAllActivities1,Many Children in this container: " + this + ", mChildren.size():" + this.mChildren.size());
        }
        if (traverseTopToBottom) {
            for (int i = this.mChildren.size() - 1; i >= 0; i--) {
                if (this.mChildren.get(i).forAllActivities(callback, traverseTopToBottom)) {
                    return true;
                }
            }
            return false;
        }
        int count = this.mChildren.size();
        for (int i2 = 0; i2 < count; i2++) {
            if (this.mChildren.get(i2).forAllActivities(callback, traverseTopToBottom)) {
                return true;
            }
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void forAllActivities(Consumer<ActivityRecord> callback) {
        forAllActivities(callback, true);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void forAllActivities(Consumer<ActivityRecord> callback, boolean traverseTopToBottom) {
        WindowList<E> windowList = this.mChildren;
        if (windowList != null && windowList.size() > 100) {
            Slog.e("WindowManager", "forAllActivities2,Many Children in this container: " + this + ", mChildren.size():" + this.mChildren.size());
        }
        if (traverseTopToBottom) {
            for (int i = this.mChildren.size() - 1; i >= 0; i--) {
                this.mChildren.get(i).forAllActivities(callback, traverseTopToBottom);
            }
            return;
        }
        int count = this.mChildren.size();
        for (int i2 = 0; i2 < count; i2++) {
            this.mChildren.get(i2).forAllActivities(callback, traverseTopToBottom);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public final boolean forAllActivities(Predicate<ActivityRecord> callback, WindowContainer boundary, boolean includeBoundary, boolean traverseTopToBottom) {
        return forAllActivities(callback, boundary, includeBoundary, traverseTopToBottom, new boolean[1]);
    }

    private boolean forAllActivities(Predicate<ActivityRecord> callback, WindowContainer boundary, boolean includeBoundary, boolean traverseTopToBottom, boolean[] boundaryFound) {
        WindowList<E> windowList = this.mChildren;
        if (windowList != null && windowList.size() > 100) {
            Slog.e("WindowManager", "forAllActivities3,Many Children in this container: " + this + ", mChildren.size():" + this.mChildren.size());
        }
        if (traverseTopToBottom) {
            for (int i = this.mChildren.size() - 1; i >= 0; i--) {
                if (processForAllActivitiesWithBoundary(callback, boundary, includeBoundary, traverseTopToBottom, boundaryFound, this.mChildren.get(i))) {
                    return true;
                }
            }
            return false;
        }
        int count = this.mChildren.size();
        for (int i2 = 0; i2 < count; i2++) {
            if (processForAllActivitiesWithBoundary(callback, boundary, includeBoundary, traverseTopToBottom, boundaryFound, this.mChildren.get(i2))) {
                return true;
            }
        }
        return false;
    }

    private boolean processForAllActivitiesWithBoundary(Predicate<ActivityRecord> callback, WindowContainer boundary, boolean includeBoundary, boolean traverseTopToBottom, boolean[] boundaryFound, WindowContainer wc) {
        if (wc == boundary) {
            boundaryFound[0] = true;
            if (!includeBoundary) {
                return false;
            }
        }
        if (boundaryFound[0]) {
            return wc.forAllActivities(callback, traverseTopToBottom);
        }
        return wc.forAllActivities(callback, boundary, includeBoundary, traverseTopToBottom, boundaryFound);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasActivity() {
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            if (this.mChildren.get(i).hasActivity()) {
                return true;
            }
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActivityRecord getActivity(Predicate<ActivityRecord> callback) {
        return getActivity(callback, true);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActivityRecord getActivity(Predicate<ActivityRecord> callback, boolean traverseTopToBottom) {
        return getActivity(callback, traverseTopToBottom, null);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActivityRecord getActivity(Predicate<ActivityRecord> callback, boolean traverseTopToBottom, ActivityRecord boundary) {
        if (traverseTopToBottom) {
            for (int i = this.mChildren.size() - 1; i >= 0; i--) {
                WindowContainer wc = this.mChildren.get(i);
                if (wc == boundary) {
                    return boundary;
                }
                ActivityRecord r = wc.getActivity(callback, traverseTopToBottom, boundary);
                if (r != null) {
                    return r;
                }
            }
            return null;
        }
        int count = this.mChildren.size();
        for (int i2 = 0; i2 < count; i2++) {
            WindowContainer wc2 = this.mChildren.get(i2);
            if (wc2 == boundary) {
                return boundary;
            }
            ActivityRecord r2 = wc2.getActivity(callback, traverseTopToBottom, boundary);
            if (r2 != null) {
                return r2;
            }
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public final ActivityRecord getActivity(Predicate<ActivityRecord> callback, WindowContainer boundary, boolean includeBoundary, boolean traverseTopToBottom) {
        return getActivity(callback, boundary, includeBoundary, traverseTopToBottom, new boolean[1]);
    }

    private ActivityRecord getActivity(Predicate<ActivityRecord> callback, WindowContainer boundary, boolean includeBoundary, boolean traverseTopToBottom, boolean[] boundaryFound) {
        if (traverseTopToBottom) {
            for (int i = this.mChildren.size() - 1; i >= 0; i--) {
                ActivityRecord r = processGetActivityWithBoundary(callback, boundary, includeBoundary, traverseTopToBottom, boundaryFound, this.mChildren.get(i));
                if (r != null) {
                    return r;
                }
            }
            return null;
        }
        int count = this.mChildren.size();
        for (int i2 = 0; i2 < count; i2++) {
            ActivityRecord r2 = processGetActivityWithBoundary(callback, boundary, includeBoundary, traverseTopToBottom, boundaryFound, this.mChildren.get(i2));
            if (r2 != null) {
                return r2;
            }
        }
        return null;
    }

    private ActivityRecord processGetActivityWithBoundary(Predicate<ActivityRecord> callback, WindowContainer boundary, boolean includeBoundary, boolean traverseTopToBottom, boolean[] boundaryFound, WindowContainer wc) {
        if (wc == boundary || boundary == null) {
            boundaryFound[0] = true;
            if (!includeBoundary) {
                return null;
            }
        }
        if (boundaryFound[0]) {
            return wc.getActivity(callback, traverseTopToBottom);
        }
        return wc.getActivity(callback, boundary, includeBoundary, traverseTopToBottom, boundaryFound);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$getActivityAbove$1(ActivityRecord above) {
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActivityRecord getActivityAbove(ActivityRecord r) {
        return getActivity(new Predicate() { // from class: com.android.server.wm.WindowContainer$$ExternalSyntheticLambda10
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return WindowContainer.lambda$getActivityAbove$1((ActivityRecord) obj);
            }
        }, r, false, false);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$getActivityBelow$2(ActivityRecord below) {
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActivityRecord getActivityBelow(ActivityRecord r) {
        return getActivity(new Predicate() { // from class: com.android.server.wm.WindowContainer$$ExternalSyntheticLambda6
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return WindowContainer.lambda$getActivityBelow$2((ActivityRecord) obj);
            }
        }, r, false, true);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$getBottomMostActivity$3(ActivityRecord r) {
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActivityRecord getBottomMostActivity() {
        return getActivity(new Predicate() { // from class: com.android.server.wm.WindowContainer$$ExternalSyntheticLambda13
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return WindowContainer.lambda$getBottomMostActivity$3((ActivityRecord) obj);
            }
        }, false);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$getTopMostActivity$4(ActivityRecord r) {
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActivityRecord getTopMostActivity() {
        return getActivity(new Predicate() { // from class: com.android.server.wm.WindowContainer$$ExternalSyntheticLambda8
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return WindowContainer.lambda$getTopMostActivity$4((ActivityRecord) obj);
            }
        }, true);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActivityRecord getTopActivity(boolean includeFinishing, boolean includeOverlays) {
        if (includeFinishing) {
            if (includeOverlays) {
                return getActivity(new Predicate() { // from class: com.android.server.wm.WindowContainer$$ExternalSyntheticLambda0
                    @Override // java.util.function.Predicate
                    public final boolean test(Object obj) {
                        return WindowContainer.lambda$getTopActivity$5((ActivityRecord) obj);
                    }
                });
            }
            return getActivity(new Predicate() { // from class: com.android.server.wm.WindowContainer$$ExternalSyntheticLambda1
                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    return WindowContainer.lambda$getTopActivity$6((ActivityRecord) obj);
                }
            });
        } else if (includeOverlays) {
            return getActivity(new Predicate() { // from class: com.android.server.wm.WindowContainer$$ExternalSyntheticLambda2
                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    return WindowContainer.lambda$getTopActivity$7((ActivityRecord) obj);
                }
            });
        } else {
            return getActivity(new Predicate() { // from class: com.android.server.wm.WindowContainer$$ExternalSyntheticLambda3
                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    return WindowContainer.lambda$getTopActivity$8((ActivityRecord) obj);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$getTopActivity$5(ActivityRecord r) {
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$getTopActivity$6(ActivityRecord r) {
        return !r.isTaskOverlay();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$getTopActivity$7(ActivityRecord r) {
        return !r.finishing;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$getTopActivity$8(ActivityRecord r) {
        return (r.finishing || r.isTaskOverlay()) ? false : true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void forAllWallpaperWindows(Consumer<WallpaperWindowToken> callback) {
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            this.mChildren.get(i).forAllWallpaperWindows(callback);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean forAllTasks(Predicate<Task> callback) {
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            if (this.mChildren.get(i).forAllTasks(callback)) {
                return true;
            }
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean forAllLeafTasks(Predicate<Task> callback) {
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            if (this.mChildren.get(i).forAllLeafTasks(callback)) {
                return true;
            }
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean forAllLeafTaskFragments(Predicate<TaskFragment> callback) {
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            if (this.mChildren.get(i).forAllLeafTaskFragments(callback)) {
                return true;
            }
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean forAllRootTasks(Predicate<Task> callback) {
        return forAllRootTasks(callback, true);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean forAllRootTasks(Predicate<Task> callback, boolean traverseTopToBottom) {
        int count = this.mChildren.size();
        if (traverseTopToBottom) {
            for (int i = count - 1; i >= 0; i--) {
                if (this.mChildren.get(i).forAllRootTasks(callback, traverseTopToBottom)) {
                    return true;
                }
            }
            return false;
        }
        int i2 = 0;
        while (i2 < count) {
            if (this.mChildren.get(i2).forAllRootTasks(callback, traverseTopToBottom)) {
                return true;
            }
            int newCount = this.mChildren.size();
            count = newCount;
            i2 = (i2 - (count - newCount)) + 1;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void forAllTasks(Consumer<Task> callback) {
        forAllTasks(callback, true);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void forAllTasks(Consumer<Task> callback, boolean traverseTopToBottom) {
        int count = this.mChildren.size();
        if (traverseTopToBottom) {
            for (int i = count - 1; i >= 0; i--) {
                this.mChildren.get(i).forAllTasks(callback, traverseTopToBottom);
            }
            return;
        }
        for (int i2 = 0; i2 < count; i2++) {
            this.mChildren.get(i2).forAllTasks(callback, traverseTopToBottom);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void forAllTaskFragments(Consumer<TaskFragment> callback) {
        forAllTaskFragments(callback, true);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void forAllTaskFragments(Consumer<TaskFragment> callback, boolean traverseTopToBottom) {
        int count = this.mChildren.size();
        if (traverseTopToBottom) {
            for (int i = count - 1; i >= 0; i--) {
                this.mChildren.get(i).forAllTaskFragments(callback, traverseTopToBottom);
            }
            return;
        }
        for (int i2 = 0; i2 < count; i2++) {
            this.mChildren.get(i2).forAllTaskFragments(callback, traverseTopToBottom);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void forAllLeafTasks(Consumer<Task> callback, boolean traverseTopToBottom) {
        int count = this.mChildren.size();
        if (traverseTopToBottom) {
            for (int i = count - 1; i >= 0; i--) {
                this.mChildren.get(i).forAllLeafTasks(callback, traverseTopToBottom);
            }
            return;
        }
        for (int i2 = 0; i2 < count; i2++) {
            this.mChildren.get(i2).forAllLeafTasks(callback, traverseTopToBottom);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void forAllLeafTaskFragments(Consumer<TaskFragment> callback, boolean traverseTopToBottom) {
        int count = this.mChildren.size();
        if (traverseTopToBottom) {
            for (int i = count - 1; i >= 0; i--) {
                this.mChildren.get(i).forAllLeafTaskFragments(callback, traverseTopToBottom);
            }
            return;
        }
        for (int i2 = 0; i2 < count; i2++) {
            this.mChildren.get(i2).forAllLeafTaskFragments(callback, traverseTopToBottom);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void forAllRootTasks(Consumer<Task> callback) {
        forAllRootTasks(callback, true);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void forAllRootTasks(Consumer<Task> callback, boolean traverseTopToBottom) {
        int count = this.mChildren.size();
        if (traverseTopToBottom) {
            for (int i = count - 1; i >= 0; i--) {
                this.mChildren.get(i).forAllRootTasks(callback, traverseTopToBottom);
            }
            return;
        }
        int i2 = 0;
        while (i2 < count) {
            this.mChildren.get(i2).forAllRootTasks(callback, traverseTopToBottom);
            int newCount = this.mChildren.size();
            count = newCount;
            i2 = (i2 - (count - newCount)) + 1;
        }
    }

    Task getTaskAbove(Task t) {
        return getTask(new Predicate() { // from class: com.android.server.wm.WindowContainer$$ExternalSyntheticLambda12
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return WindowContainer.lambda$getTaskAbove$9((Task) obj);
            }
        }, t, false, false);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$getTaskAbove$9(Task above) {
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$getTaskBelow$10(Task below) {
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Task getTaskBelow(Task t) {
        return getTask(new Predicate() { // from class: com.android.server.wm.WindowContainer$$ExternalSyntheticLambda9
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return WindowContainer.lambda$getTaskBelow$10((Task) obj);
            }
        }, t, false, true);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$getBottomMostTask$11(Task t) {
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Task getBottomMostTask() {
        return getTask(new Predicate() { // from class: com.android.server.wm.WindowContainer$$ExternalSyntheticLambda11
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return WindowContainer.lambda$getBottomMostTask$11((Task) obj);
            }
        }, false);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$getTopMostTask$12(Task t) {
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Task getTopMostTask() {
        return getTask(new Predicate() { // from class: com.android.server.wm.WindowContainer$$ExternalSyntheticLambda7
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return WindowContainer.lambda$getTopMostTask$12((Task) obj);
            }
        }, true);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Task getTask(Predicate<Task> callback) {
        return getTask(callback, true);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Task getTask(Predicate<Task> callback, boolean traverseTopToBottom) {
        if (traverseTopToBottom) {
            for (int i = this.mChildren.size() - 1; i >= 0; i--) {
                Task t = this.mChildren.get(i).getTask(callback, traverseTopToBottom);
                if (t != null) {
                    return t;
                }
            }
            return null;
        }
        int count = this.mChildren.size();
        for (int i2 = 0; i2 < count; i2++) {
            Task t2 = this.mChildren.get(i2).getTask(callback, traverseTopToBottom);
            if (t2 != null) {
                return t2;
            }
        }
        return null;
    }

    final Task getTask(Predicate<Task> callback, WindowContainer boundary, boolean includeBoundary, boolean traverseTopToBottom) {
        return getTask(callback, boundary, includeBoundary, traverseTopToBottom, new boolean[1]);
    }

    private Task getTask(Predicate<Task> callback, WindowContainer boundary, boolean includeBoundary, boolean traverseTopToBottom, boolean[] boundaryFound) {
        if (traverseTopToBottom) {
            for (int i = this.mChildren.size() - 1; i >= 0; i--) {
                Task t = processGetTaskWithBoundary(callback, boundary, includeBoundary, traverseTopToBottom, boundaryFound, this.mChildren.get(i));
                if (t != null) {
                    return t;
                }
            }
            return null;
        }
        int count = this.mChildren.size();
        for (int i2 = 0; i2 < count; i2++) {
            Task t2 = processGetTaskWithBoundary(callback, boundary, includeBoundary, traverseTopToBottom, boundaryFound, this.mChildren.get(i2));
            if (t2 != null) {
                return t2;
            }
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Task getRootTask(Predicate<Task> callback) {
        return getRootTask(callback, true);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Task getRootTask(Predicate<Task> callback, boolean traverseTopToBottom) {
        int count = this.mChildren.size();
        if (traverseTopToBottom) {
            for (int i = count - 1; i >= 0; i--) {
                Task t = this.mChildren.get(i).getRootTask(callback, traverseTopToBottom);
                if (t != null) {
                    return t;
                }
            }
            return null;
        }
        int i2 = 0;
        while (i2 < count) {
            Task t2 = this.mChildren.get(i2).getRootTask(callback, traverseTopToBottom);
            if (t2 != null) {
                return t2;
            }
            int newCount = this.mChildren.size();
            count = newCount;
            i2 = (i2 - (count - newCount)) + 1;
        }
        return null;
    }

    private Task processGetTaskWithBoundary(Predicate<Task> callback, WindowContainer boundary, boolean includeBoundary, boolean traverseTopToBottom, boolean[] boundaryFound, WindowContainer wc) {
        if (wc == boundary || boundary == null) {
            boundaryFound[0] = true;
            if (!includeBoundary) {
                return null;
            }
        }
        if (boundaryFound[0]) {
            return wc.getTask(callback, traverseTopToBottom);
        }
        return wc.getTask(callback, boundary, includeBoundary, traverseTopToBottom, boundaryFound);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public WindowState getWindow(Predicate<WindowState> callback) {
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            WindowState w = this.mChildren.get(i).getWindow(callback);
            if (w != null) {
                return w;
            }
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void forAllDisplayAreas(Consumer<DisplayArea> callback) {
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            this.mChildren.get(i).forAllDisplayAreas(callback);
        }
    }

    boolean forAllTaskDisplayAreas(Predicate<TaskDisplayArea> callback, boolean traverseTopToBottom) {
        int childCount = this.mChildren.size();
        int i = traverseTopToBottom ? childCount - 1 : 0;
        while (i >= 0 && i < childCount) {
            int i2 = 1;
            if (this.mChildren.get(i).forAllTaskDisplayAreas(callback, traverseTopToBottom)) {
                return true;
            }
            if (traverseTopToBottom) {
                i2 = -1;
            }
            i += i2;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean forAllTaskDisplayAreas(Predicate<TaskDisplayArea> callback) {
        return forAllTaskDisplayAreas(callback, true);
    }

    void forAllTaskDisplayAreas(Consumer<TaskDisplayArea> callback, boolean traverseTopToBottom) {
        int childCount = this.mChildren.size();
        int i = traverseTopToBottom ? childCount - 1 : 0;
        while (i >= 0 && i < childCount) {
            this.mChildren.get(i).forAllTaskDisplayAreas(callback, traverseTopToBottom);
            i += traverseTopToBottom ? -1 : 1;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void forAllTaskDisplayAreas(Consumer<TaskDisplayArea> callback) {
        forAllTaskDisplayAreas(callback, true);
    }

    <R> R reduceOnAllTaskDisplayAreas(BiFunction<TaskDisplayArea, R, R> accumulator, R initValue, boolean traverseTopToBottom) {
        int childCount = this.mChildren.size();
        int i = traverseTopToBottom ? childCount - 1 : 0;
        R result = initValue;
        while (i >= 0 && i < childCount) {
            result = (R) this.mChildren.get(i).reduceOnAllTaskDisplayAreas(accumulator, result, traverseTopToBottom);
            i += traverseTopToBottom ? -1 : 1;
        }
        return result;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public <R> R reduceOnAllTaskDisplayAreas(BiFunction<TaskDisplayArea, R, R> accumulator, R initValue) {
        return (R) reduceOnAllTaskDisplayAreas(accumulator, initValue, true);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public <R> R getItemFromDisplayAreas(Function<DisplayArea, R> callback) {
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            R result = (R) this.mChildren.get(i).getItemFromDisplayAreas(callback);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    <R> R getItemFromTaskDisplayAreas(Function<TaskDisplayArea, R> callback, boolean traverseTopToBottom) {
        int childCount = this.mChildren.size();
        int i = traverseTopToBottom ? childCount - 1 : 0;
        while (i >= 0 && i < childCount) {
            R result = (R) this.mChildren.get(i).getItemFromTaskDisplayAreas(callback, traverseTopToBottom);
            if (result != null) {
                return result;
            }
            i += traverseTopToBottom ? -1 : 1;
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public <R> R getItemFromTaskDisplayAreas(Function<TaskDisplayArea, R> callback) {
        return (R) getItemFromTaskDisplayAreas(callback, true);
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [2483=4, 2484=4] */
    /* JADX DEBUG: Method merged with bridge method */
    /* JADX WARN: Can't rename method to resolve collision */
    /* JADX WARN: Code restructure failed: missing block: B:22:0x0055, code lost:
        if (r4 != r10) goto L26;
     */
    /* JADX WARN: Code restructure failed: missing block: B:24:0x0062, code lost:
        return -1;
     */
    /* JADX WARN: Code restructure failed: missing block: B:25:0x0063, code lost:
        if (r4 != r11) goto L29;
     */
    /* JADX WARN: Code restructure failed: missing block: B:27:0x0070, code lost:
        return 1;
     */
    /* JADX WARN: Code restructure failed: missing block: B:28:0x0071, code lost:
        r7 = r4.mChildren;
     */
    /* JADX WARN: Code restructure failed: missing block: B:29:0x0083, code lost:
        if (r7.indexOf(r0.peekLast()) <= r7.indexOf(r3.peekLast())) goto L31;
     */
    /* JADX WARN: Code restructure failed: missing block: B:31:0x0086, code lost:
        r1 = -1;
     */
    /* JADX WARN: Code restructure failed: missing block: B:33:0x0091, code lost:
        return r1;
     */
    @Override // java.lang.Comparable
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public int compareTo(WindowContainer other) {
        if (this == other) {
            return 0;
        }
        WindowContainer<WindowContainer> windowContainer = this.mParent;
        int i = 1;
        if (windowContainer != null && windowContainer == other.mParent) {
            WindowList<WindowContainer> list = windowContainer.mChildren;
            return list.indexOf(this) > list.indexOf(other) ? 1 : -1;
        }
        LinkedList<WindowContainer> thisParentChain = this.mTmpChain1;
        LinkedList<WindowContainer> otherParentChain = this.mTmpChain2;
        try {
            getParents(thisParentChain);
            other.getParents(otherParentChain);
            WindowContainer commonAncestor = null;
            WindowContainer thisTop = thisParentChain.peekLast();
            for (WindowContainer otherTop = otherParentChain.peekLast(); thisTop != null && otherTop != null && thisTop == otherTop; otherTop = otherParentChain.peekLast()) {
                commonAncestor = thisParentChain.removeLast();
                otherParentChain.removeLast();
                thisTop = thisParentChain.peekLast();
            }
            throw new IllegalArgumentException("No in the same hierarchy this=" + thisParentChain + " other=" + otherParentChain);
        } finally {
            this.mTmpChain1.clear();
            this.mTmpChain2.clear();
        }
    }

    private void getParents(LinkedList<WindowContainer> parents) {
        parents.clear();
        WindowContainer current = this;
        do {
            parents.addLast(current);
            current = current.mParent;
        } while (current != null);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public SurfaceControl.Builder makeSurface() {
        WindowContainer p = getParent();
        return p.makeChildSurface(this);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public SurfaceControl.Builder makeChildSurface(WindowContainer child) {
        WindowContainer p = getParent();
        return p.makeChildSurface(child).setParent(this.mSurfaceControl);
    }

    public SurfaceControl getParentSurfaceControl() {
        WindowContainer parent = getParent();
        if (parent == null) {
            return null;
        }
        return parent.getSurfaceControl();
    }

    boolean shouldMagnify() {
        if (this.mSurfaceControl == null) {
            return false;
        }
        for (int i = 0; i < this.mChildren.size(); i++) {
            if (!this.mChildren.get(i).shouldMagnify()) {
                return false;
            }
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public SurfaceSession getSession() {
        if (getParent() != null) {
            return getParent().getSession();
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void assignLayer(SurfaceControl.Transaction t, int layer) {
        if (this.mTransitionController.isPlaying()) {
            return;
        }
        boolean changed = (layer == this.mLastLayer && this.mLastRelativeToLayer == null) ? false : true;
        if (this.mSurfaceControl != null && changed) {
            setLayer(t, layer);
            this.mLastLayer = layer;
            this.mLastRelativeToLayer = null;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void assignRelativeLayer(SurfaceControl.Transaction t, SurfaceControl relativeTo, int layer, boolean forceUpdate) {
        boolean changed = (layer == this.mLastLayer && this.mLastRelativeToLayer == relativeTo) ? false : true;
        if (this.mSurfaceControl != null) {
            if (changed || forceUpdate) {
                setRelativeLayer(t, relativeTo, layer);
                this.mLastLayer = layer;
                this.mLastRelativeToLayer = relativeTo;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void assignRelativeLayer(SurfaceControl.Transaction t, SurfaceControl relativeTo, int layer) {
        assignRelativeLayer(t, relativeTo, layer, false);
    }

    protected void setLayer(SurfaceControl.Transaction t, int layer) {
        if (this.mSurfaceFreezer.hasLeash()) {
            this.mSurfaceFreezer.setLayer(t, layer);
        } else {
            this.mSurfaceAnimator.setLayer(t, layer);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getLastLayer() {
        return this.mLastLayer;
    }

    protected void setRelativeLayer(SurfaceControl.Transaction t, SurfaceControl relativeTo, int layer) {
        if (this.mSurfaceFreezer.hasLeash()) {
            this.mSurfaceFreezer.setRelativeLayer(t, relativeTo, layer);
        } else {
            this.mSurfaceAnimator.setRelativeLayer(t, relativeTo, layer);
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void reparentSurfaceControl(SurfaceControl.Transaction t, SurfaceControl newParent) {
        if (this.mSurfaceFreezer.hasLeash() || this.mSurfaceAnimator.hasLeash()) {
            return;
        }
        t.reparent(getSurfaceControl(), newParent);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void assignChildLayers(SurfaceControl.Transaction t) {
        int layer = 0;
        List<WindowContainer> thunderbackTaskDisplayAreas = new ArrayList<>();
        boolean defaultTaskDisplayAreaAssigned = false;
        for (int j = 0; j < this.mChildren.size(); j++) {
            WindowContainer wc = this.mChildren.get(j);
            if (wc.asTaskDisplayArea() != null && !defaultTaskDisplayAreaAssigned && wc.getConfiguration().windowConfiguration.isThunderbackWindow()) {
                thunderbackTaskDisplayAreas.add(wc);
            } else {
                wc.assignChildLayers(t);
                if (!wc.needsZBoost()) {
                    wc.assignLayer(t, layer);
                    layer++;
                }
                TaskDisplayArea taskDisplayArea = wc.asTaskDisplayArea();
                DisplayContent displayContent = getDisplayContent();
                if (taskDisplayArea != null && displayContent != null && taskDisplayArea == displayContent.getDefaultTaskDisplayArea()) {
                    defaultTaskDisplayAreaAssigned = true;
                    for (int i = 0; i < thunderbackTaskDisplayAreas.size(); i++) {
                        WindowContainer wc_ = thunderbackTaskDisplayAreas.get(i);
                        wc_.assignChildLayers(t);
                        if (!wc_.needsZBoost()) {
                            wc_.assignLayer(t, layer);
                            layer++;
                        }
                    }
                    thunderbackTaskDisplayAreas.clear();
                }
            }
        }
        for (int i2 = 0; i2 < thunderbackTaskDisplayAreas.size(); i2++) {
            WindowContainer wc_2 = thunderbackTaskDisplayAreas.get(i2);
            wc_2.assignChildLayers(t);
            if (!wc_2.needsZBoost()) {
                wc_2.assignLayer(t, layer);
                layer++;
            }
        }
        thunderbackTaskDisplayAreas.clear();
        for (int j2 = 0; j2 < this.mChildren.size(); j2++) {
            WindowContainer wc2 = this.mChildren.get(j2);
            if (wc2.needsZBoost()) {
                wc2.assignLayer(t, layer);
                layer++;
            }
        }
        TrustedOverlayHost trustedOverlayHost = this.mOverlayHost;
        if (trustedOverlayHost != null) {
            int i3 = layer + 1;
            trustedOverlayHost.setLayer(t, layer);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void assignChildLayers() {
        assignChildLayers(getSyncTransaction());
        scheduleAnimation();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean needsZBoost() {
        if (this.mNeedsZBoost) {
            return true;
        }
        if (asTask() != null) {
            int zboostTaskIdToSplit = ITranMultiWindow.Instance().getZBoostTaskIdWhenToSplit();
            Task self = asTask();
            if (zboostTaskIdToSplit == self.mTaskId) {
                Log.d("zbtag", "needsZBoost, return true, zboostTaskIdToSplit:" + zboostTaskIdToSplit);
                return true;
            }
        }
        for (int i = 0; i < this.mChildren.size(); i++) {
            if (this.mChildren.get(i).needsZBoost()) {
                return true;
            }
        }
        return false;
    }

    @Override // com.android.server.wm.ConfigurationContainer
    public void dumpDebug(ProtoOutputStream proto, long fieldId, int logLevel) {
        boolean isVisible = isVisible();
        if (logLevel == 2 && !isVisible) {
            return;
        }
        long token = proto.start(fieldId);
        super.dumpDebug(proto, 1146756268033L, logLevel);
        proto.write(1120986464258L, this.mOrientation);
        proto.write(1133871366147L, isVisible);
        writeIdentifierToProto(proto, 1146756268038L);
        if (this.mSurfaceAnimator.isAnimating()) {
            this.mSurfaceAnimator.dumpDebug(proto, 1146756268036L);
        }
        SurfaceControl surfaceControl = this.mSurfaceControl;
        if (surfaceControl != null) {
            surfaceControl.dumpDebug(proto, 1146756268039L);
        }
        for (int i = 0; i < getChildCount(); i++) {
            long childToken = proto.start(2246267895813L);
            E child = getChildAt(i);
            child.dumpDebug(proto, child.getProtoFieldId(), logLevel);
            proto.end(childToken);
        }
        proto.end(token);
    }

    long getProtoFieldId() {
        return 1146756268034L;
    }

    private WindowContainer<E>.ForAllWindowsConsumerWrapper obtainConsumerWrapper(Consumer<WindowState> consumer) {
        WindowContainer<E>.ForAllWindowsConsumerWrapper wrapper = (ForAllWindowsConsumerWrapper) this.mConsumerWrapperPool.acquire();
        if (wrapper == null) {
            wrapper = new ForAllWindowsConsumerWrapper();
        }
        wrapper.setConsumer(consumer);
        return wrapper;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public final class ForAllWindowsConsumerWrapper implements ToBooleanFunction<WindowState> {
        private Consumer<WindowState> mConsumer;

        private ForAllWindowsConsumerWrapper() {
        }

        void setConsumer(Consumer<WindowState> consumer) {
            this.mConsumer = consumer;
        }

        /* JADX DEBUG: Method merged with bridge method */
        public boolean apply(WindowState w) {
            this.mConsumer.accept(w);
            return false;
        }

        void release() {
            this.mConsumer = null;
            WindowContainer.this.mConsumerWrapperPool.release(this);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void applyMagnificationSpec(SurfaceControl.Transaction t, MagnificationSpec spec) {
        if (shouldMagnify()) {
            t.setMatrix(this.mSurfaceControl, spec.scale, 0.0f, 0.0f, spec.scale).setPosition(this.mSurfaceControl, spec.offsetX, spec.offsetY);
            this.mLastMagnificationSpec = spec;
            return;
        }
        clearMagnificationSpec(t);
        for (int i = 0; i < this.mChildren.size(); i++) {
            this.mChildren.get(i).applyMagnificationSpec(t, spec);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void clearMagnificationSpec(SurfaceControl.Transaction t) {
        if (this.mLastMagnificationSpec != null) {
            t.setMatrix(this.mSurfaceControl, 1.0f, 0.0f, 0.0f, 1.0f).setPosition(this.mSurfaceControl, 0.0f, 0.0f);
        }
        this.mLastMagnificationSpec = null;
        for (int i = 0; i < this.mChildren.size(); i++) {
            this.mChildren.get(i).clearMagnificationSpec(t);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void prepareSurfaces() {
        this.mCommittedReparentToAnimationLeash = this.mSurfaceAnimator.hasLeash();
        for (int i = 0; i < this.mChildren.size(); i++) {
            this.mChildren.get(i).prepareSurfaces();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasCommittedReparentToAnimationLeash() {
        return this.mCommittedReparentToAnimationLeash;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void scheduleAnimation() {
        WindowContainer<WindowContainer> windowContainer = this.mParent;
        if (windowContainer != null) {
            windowContainer.scheduleAnimation();
        }
    }

    public SurfaceControl getSurfaceControl() {
        return this.mSurfaceControl;
    }

    public SurfaceControl.Transaction getSyncTransaction() {
        if (this.mSyncTransactionCommitCallbackDepth > 0) {
            return this.mSyncTransaction;
        }
        if (this.mSyncState != 0) {
            return this.mSyncTransaction;
        }
        return getPendingTransaction();
    }

    public SurfaceControl.Transaction getPendingTransaction() {
        DisplayContent displayContent = getDisplayContent();
        if (displayContent != null && displayContent != this) {
            return displayContent.getPendingTransaction();
        }
        return this.mPendingTransaction;
    }

    void startAnimation(SurfaceControl.Transaction t, AnimationAdapter anim, boolean hidden, int type, SurfaceAnimator.OnAnimationFinishedCallback animationFinishedCallback, Runnable animationCancelledCallback, AnimationAdapter snapshotAnim) {
        if (ProtoLogCache.WM_DEBUG_ANIM_enabled) {
            String protoLogParam0 = String.valueOf(this);
            long protoLogParam1 = type;
            String protoLogParam2 = String.valueOf(anim);
            ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_ANIM, 385595355, 4, (String) null, new Object[]{protoLogParam0, Long.valueOf(protoLogParam1), protoLogParam2});
        }
        this.mSurfaceAnimator.startAnimation(t, anim, hidden, type, animationFinishedCallback, animationCancelledCallback, snapshotAnim, this.mSurfaceFreezer);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void startAnimation(SurfaceControl.Transaction t, AnimationAdapter anim, boolean hidden, int type, SurfaceAnimator.OnAnimationFinishedCallback animationFinishedCallback) {
        startAnimation(t, anim, hidden, type, animationFinishedCallback, null, null);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void startAnimation(SurfaceControl.Transaction t, AnimationAdapter anim, boolean hidden, int type) {
        startAnimation(t, anim, hidden, type, null);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void transferAnimation(WindowContainer from) {
        this.mSurfaceAnimator.transferAnimation(from.mSurfaceAnimator);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void cancelAnimation() {
        doAnimationFinished(this.mSurfaceAnimator.getAnimationType(), this.mSurfaceAnimator.getAnimation());
        this.mSurfaceAnimator.cancelAnimation();
        this.mSurfaceFreezer.unfreeze(getSyncTransaction());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean canStartChangeTransition() {
        return (this.mWmService.mDisableTransitionAnimation || this.mDisplayContent == null || getSurfaceControl() == null || this.mDisplayContent.inTransition() || !isVisible() || !isVisibleRequested() || !okToAnimate() || inPinnedWindowingMode() || getParent() == null || getParent().inPinnedWindowingMode()) ? false : true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void initializeChangeTransition(Rect startBounds, SurfaceControl freezeTarget) {
        this.mDisplayContent.prepareAppTransition(6);
        this.mDisplayContent.mChangingContainers.add(this);
        Rect parentBounds = getParent().getBounds();
        this.mTmpPoint.set(startBounds.left - parentBounds.left, startBounds.top - parentBounds.top);
        this.mSurfaceFreezer.freeze(getSyncTransaction(), startBounds, this.mTmpPoint, freezeTarget);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void initializeChangeTransition(Rect startBounds) {
        initializeChangeTransition(startBounds, null);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ArraySet<WindowContainer> getAnimationSources() {
        return this.mSurfaceAnimationSources;
    }

    public SurfaceControl getFreezeSnapshotTarget() {
        if (!this.mDisplayContent.mAppTransition.containsTransitRequest(6) || !this.mDisplayContent.mChangingContainers.contains(this)) {
            return null;
        }
        return getSurfaceControl();
    }

    public void onUnfrozen() {
        DisplayContent displayContent = this.mDisplayContent;
        if (displayContent != null) {
            displayContent.mChangingContainers.remove(this);
        }
    }

    public SurfaceControl.Builder makeAnimationLeash() {
        return makeSurface().setContainerLayer();
    }

    public SurfaceControl getAnimationLeashParent() {
        return getParentSurfaceControl();
    }

    Rect getAnimationBounds(int appRootTaskClipMode) {
        return getBounds();
    }

    void getAnimationPosition(Point outPosition) {
        getRelativePosition(outPosition);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean applyAnimation(WindowManager.LayoutParams lp, int transit, boolean enter, boolean isVoiceInteraction, ArrayList<WindowContainer> sources) {
        if (this.mWmService.mDisableTransitionAnimation) {
            if (ProtoLogCache.WM_DEBUG_APP_TRANSITIONS_ANIM_enabled) {
                String protoLogParam0 = String.valueOf(this);
                ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_APP_TRANSITIONS_ANIM, -33096143, 0, (String) null, new Object[]{protoLogParam0});
            }
            cancelAnimation();
            return false;
        }
        try {
            Trace.traceBegin(32L, "WC#applyAnimation");
            if (!okToAnimate() && (!ITranPhoneWindowManager.Instance().isDisableLauncherUnlockAnimation() || !AppTransition.isKeyguardGoingAwayTransitOld(transit) || !TranFpUnlockStateController.getInstance().canHideByFingerprint())) {
                cancelAnimation();
                Trace.traceEnd(32L);
                return isAnimating();
            }
            if (ProtoLogCache.WM_DEBUG_APP_TRANSITIONS_ANIM_enabled) {
                String protoLogParam02 = String.valueOf(AppTransition.appTransitionOldToString(transit));
                String protoLogParam2 = String.valueOf(this);
                ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_APP_TRANSITIONS_ANIM, -701167286, 12, (String) null, new Object[]{protoLogParam02, Boolean.valueOf(enter), protoLogParam2});
            }
            applyAnimationUnchecked(lp, enter, transit, isVoiceInteraction, sources);
            Trace.traceEnd(32L);
            return isAnimating();
        } catch (Throwable th) {
            Trace.traceEnd(32L);
            throw th;
        }
    }

    Pair<AnimationAdapter, AnimationAdapter> getAnimationAdapter(WindowManager.LayoutParams lp, int transit, boolean enter, boolean isVoiceInteraction) {
        RemoteAnimationController controller;
        float windowCornerRadius;
        int appRootTaskClipMode = getDisplayContent().mAppTransition.getAppRootTaskClipMode();
        Rect screenBounds = getAnimationBounds(appRootTaskClipMode);
        this.mTmpRect.set(screenBounds);
        if (asTask() != null && AppTransition.isTaskTransitOld(transit)) {
            asTask().adjustAnimationBoundsForTransition(this.mTmpRect);
        }
        getAnimationPosition(this.mTmpPoint);
        this.mTmpRect.offsetTo(0, 0);
        RemoteAnimationController controller2 = getDisplayContent().mAppTransition.getRemoteAnimationController();
        boolean isChanging = AppTransition.isChangeTransitOld(transit) && enter && isChangingAppTransition();
        if (AppTransition.isKeyguardGoingAwayTransitOld(transit) && (ITranWindowManagerService.Instance().isAodWallpaperFeatureEnabled() || ITranPhoneWindowManager.Instance().isDisableLauncherUnlockAnimation())) {
            controller = null;
        } else {
            controller = controller2;
        }
        if (controller != null && !this.mSurfaceAnimator.isAnimationStartDelayed()) {
            Rect localBounds = new Rect(this.mTmpRect);
            localBounds.offsetTo(this.mTmpPoint.x, this.mTmpPoint.y);
            RemoteAnimationController.RemoteAnimationRecord adapters = controller.createRemoteAnimationRecord(this, this.mTmpPoint, localBounds, screenBounds, isChanging ? this.mSurfaceFreezer.mFreezeBounds : null);
            if (!isChanging) {
                adapters.setMode(enter ? 0 : 1);
            }
            Pair<AnimationAdapter, AnimationAdapter> resultAdapters = new Pair<>(adapters.mAdapter, adapters.mThumbnailAdapter);
            return resultAdapters;
        } else if (!isChanging) {
            this.mNeedsAnimationBoundsLayer = appRootTaskClipMode == 0;
            Animation a = loadAnimation(lp, transit, enter, isVoiceInteraction);
            if (a == null) {
                Pair<AnimationAdapter, AnimationAdapter> resultAdapters2 = new Pair<>(null, null);
                return resultAdapters2;
            }
            boolean oneHandMode = SystemProperties.getInt("sys.onehand.state", 0) != 0 && "1".equals(SystemProperties.get("ro.tran.privacy.dot.dynamic.support", "1"));
            if (!inMultiWindowMode() && !oneHandMode) {
                windowCornerRadius = getDisplayContent().getWindowCornerRadius();
            } else {
                windowCornerRadius = 0.0f;
            }
            AnimationAdapter adapter = new LocalAnimationAdapter(new WindowAnimationSpec(a, this.mTmpPoint, this.mTmpRect, getDisplayContent().mAppTransition.canSkipFirstFrame(), appRootTaskClipMode, true, windowCornerRadius), getSurfaceAnimationRunner());
            Pair<AnimationAdapter, AnimationAdapter> resultAdapters3 = new Pair<>(adapter, null);
            this.mNeedsZBoost = a.getZAdjustment() == 1 || AppTransition.isClosingTransitOld(transit);
            if (transit == 9) {
                this.mNeedsZBoost = a.getZAdjustment() == 1;
            }
            this.mTransit = transit;
            this.mTransitFlags = getDisplayContent().mAppTransition.getTransitFlags();
            return resultAdapters3;
        } else {
            float durationScale = this.mWmService.getTransitionAnimationScaleLocked();
            DisplayInfo displayInfo = getDisplayContent().getDisplayInfo();
            this.mTmpRect.offsetTo(this.mTmpPoint.x, this.mTmpPoint.y);
            AnimationAdapter adapter2 = new LocalAnimationAdapter(new WindowChangeAnimationSpec(this.mSurfaceFreezer.mFreezeBounds, this.mTmpRect, displayInfo, durationScale, true, false), getSurfaceAnimationRunner());
            AnimationAdapter thumbnailAdapter = this.mSurfaceFreezer.mSnapshot != null ? new LocalAnimationAdapter(new WindowChangeAnimationSpec(this.mSurfaceFreezer.mFreezeBounds, this.mTmpRect, displayInfo, durationScale, true, true), getSurfaceAnimationRunner()) : null;
            Pair<AnimationAdapter, AnimationAdapter> resultAdapters4 = new Pair<>(adapter2, thumbnailAdapter);
            this.mTransit = transit;
            this.mTransitFlags = getDisplayContent().mAppTransition.getTransitFlags();
            return resultAdapters4;
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void applyAnimationUnchecked(WindowManager.LayoutParams lp, boolean enter, int transit, boolean isVoiceInteraction, ArrayList<WindowContainer> sources) {
        int backgroundColorForTransition;
        Task task = asTask();
        if (task != null && !enter && !task.isActivityTypeHomeOrRecents()) {
            boolean isImeLayeringTarget = false;
            InsetsControlTarget imeTarget = this.mDisplayContent.getImeTarget(0);
            if (imeTarget != null && imeTarget.getWindow() != null && imeTarget.getWindow().getTask() == task) {
                isImeLayeringTarget = true;
            }
            if (isImeLayeringTarget && AppTransition.isTaskCloseTransitOld(transit)) {
                this.mDisplayContent.showImeScreenshot();
            }
        }
        Pair<AnimationAdapter, AnimationAdapter> adapters = getAnimationAdapter(lp, transit, enter, isVoiceInteraction);
        AnimationAdapter adapter = (AnimationAdapter) adapters.first;
        AnimationAdapter thumbnailAdapter = (AnimationAdapter) adapters.second;
        if (adapter != null) {
            if (sources != null) {
                this.mSurfaceAnimationSources.addAll(sources);
            }
            ITranActivityTaskManagerService.Instance().hookBoostTransitAnimation(adapter.getDurationHint(), enter, (task == null || task.getTopNonFinishingActivity() == null) ? null : task.getTopNonFinishingActivity().launchedFromPackage, AppTransition.isActivityTransitOld(transit), AppTransition.isKeyguardGoingAwayTransitOld(transit), AppTransition.isClosingTransitOld(transit));
            WindowContainer<E>.AnimationRunnerBuilder animationRunnerBuilder = new AnimationRunnerBuilder();
            if (AppTransition.isTaskTransitOld(transit) && this.mWmService.mTaskTransitionSpec != null) {
                animationRunnerBuilder.hideInsetSourceViewOverflows(this.mWmService.mTaskTransitionSpec.animationBoundInsets);
            }
            ActivityRecord activityRecord = asActivityRecord();
            if (activityRecord != null && AppTransition.isActivityTransitOld(transit) && adapter.getShowBackground()) {
                if (adapter.getBackgroundColor() != 0) {
                    backgroundColorForTransition = adapter.getBackgroundColor();
                } else {
                    Task arTask = activityRecord.getTask();
                    backgroundColorForTransition = ColorUtils.setAlphaComponent(arTask.getTaskDescription().getBackgroundColor(), 255);
                }
                animationRunnerBuilder.setTaskBackgroundColor(backgroundColorForTransition);
            }
            animationRunnerBuilder.build().startAnimation(getPendingTransaction(), adapter, true ^ isVisible(), 1, thumbnailAdapter);
            if (adapter.getShowWallpaper()) {
                getDisplayContent().pendingLayoutChanges |= 4;
            }
        }
    }

    private int getTaskAnimationBackgroundColor() {
        Context uiContext = this.mDisplayContent.getDisplayPolicy().getSystemUiContext();
        TaskTransitionSpec customSpec = this.mWmService.mTaskTransitionSpec;
        int defaultFallbackColor = uiContext.getColor(17171008);
        if (customSpec != null && customSpec.backgroundColor != 0) {
            return customSpec.backgroundColor;
        }
        return defaultFallbackColor;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public final SurfaceAnimationRunner getSurfaceAnimationRunner() {
        return this.mWmService.mSurfaceAnimationRunner;
    }

    /* JADX WARN: Removed duplicated region for block: B:20:0x007a  */
    /* JADX WARN: Removed duplicated region for block: B:21:0x0083  */
    /* JADX WARN: Removed duplicated region for block: B:24:0x0088  */
    /* JADX WARN: Removed duplicated region for block: B:25:0x0091  */
    /* JADX WARN: Removed duplicated region for block: B:28:0x0099  */
    /* JADX WARN: Removed duplicated region for block: B:31:0x00cc  */
    /* JADX WARN: Removed duplicated region for block: B:34:0x00d3  */
    /* JADX WARN: Removed duplicated region for block: B:37:0x00dd  */
    /* JADX WARN: Removed duplicated region for block: B:38:0x0115  */
    /* JADX WARN: Removed duplicated region for block: B:41:0x0159  */
    /* JADX WARN: Removed duplicated region for block: B:55:0x01c2  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private Animation loadAnimation(WindowManager.LayoutParams lp, int transit, boolean enter, boolean isVoiceInteraction) {
        boolean enter2;
        int transit2;
        boolean replaceTranitForSplitScreen;
        boolean z;
        Animation a;
        if (!isOrganized() || getWindowingMode() == 1 || getWindowingMode() == 5 || getWindowingMode() == 6) {
            boolean isMultiWindow = isMultiWindow();
            if (!isMultiWindow()) {
                enter2 = enter;
            } else {
                enter2 = enter;
                int newTransit = SplitScreenHelper.getTransitForSplitScreen(getDisplayContent(), transit, enter2);
                if (newTransit != -1) {
                    Slog.d("WindowManager", "loadAnimation modify transit for splitscreen " + transit + "  => " + newTransit + " " + this);
                    transit2 = newTransit;
                    replaceTranitForSplitScreen = true;
                    DisplayContent displayContent = getDisplayContent();
                    DisplayInfo displayInfo = displayContent.getDisplayInfo();
                    int width = !isMultiWindow ? getBounds().width() : displayInfo.appWidth;
                    int height = !isMultiWindow ? getBounds().height() : displayInfo.appHeight;
                    if (ProtoLogCache.WM_DEBUG_APP_TRANSITIONS_ANIM_enabled) {
                        String protoLogParam0 = String.valueOf(this);
                        ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_APP_TRANSITIONS_ANIM, 1584270979, 0, (String) null, new Object[]{protoLogParam0});
                    }
                    Rect frame = new Rect(0, 0, width, height);
                    Rect displayFrame = new Rect(0, 0, displayInfo.logicalWidth, displayInfo.logicalHeight);
                    Rect insets = new Rect();
                    Rect stableInsets = new Rect();
                    Rect surfaceInsets = new Rect();
                    if (!isMultiWindow) {
                        getAnimationFrames(frame, insets, stableInsets, surfaceInsets);
                    }
                    if (this.mLaunchTaskBehind) {
                        enter2 = false;
                    }
                    if (ProtoLogCache.WM_DEBUG_APP_TRANSITIONS_enabled) {
                        z = true;
                    } else {
                        String protoLogParam02 = String.valueOf(AppTransition.appTransitionOldToString(transit2));
                        boolean protoLogParam1 = enter2;
                        String protoLogParam2 = String.valueOf(frame);
                        String protoLogParam3 = String.valueOf(insets);
                        String protoLogParam4 = String.valueOf(surfaceInsets);
                        z = true;
                        ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_APP_TRANSITIONS, 1831008694, 12, (String) null, new Object[]{protoLogParam02, Boolean.valueOf(protoLogParam1), protoLogParam2, protoLogParam3, protoLogParam4});
                    }
                    Configuration displayConfig = displayContent.getConfiguration();
                    boolean enter3 = enter2;
                    int width2 = transit2;
                    boolean z2 = z;
                    a = getDisplayContent().mAppTransition.loadAnimation(lp, transit2, enter2, displayConfig.uiMode, displayConfig.orientation, frame, displayFrame, insets, surfaceInsets, stableInsets, isVoiceInteraction, inFreeformWindowingMode(), this);
                    if (a != null) {
                        if (a != null) {
                            a.restrictDuration(BackupAgentTimeoutParameters.DEFAULT_QUOTA_EXCEEDED_TIMEOUT_MILLIS);
                        }
                        if (ProtoLogImpl.isEnabled(ProtoLogGroup.WM_DEBUG_ANIM) && ProtoLogCache.WM_DEBUG_ANIM_enabled) {
                            String protoLogParam03 = String.valueOf(a);
                            String protoLogParam12 = String.valueOf(this);
                            long protoLogParam22 = a != null ? a.getDuration() : 0L;
                            String protoLogParam32 = String.valueOf(Debug.getCallers(20));
                            ProtoLogGroup protoLogGroup = ProtoLogGroup.WM_DEBUG_ANIM;
                            Object[] objArr = new Object[4];
                            objArr[0] = protoLogParam03;
                            objArr[z2 ? 1 : 0] = protoLogParam12;
                            objArr[2] = Long.valueOf(protoLogParam22);
                            objArr[3] = protoLogParam32;
                            ProtoLogImpl.i(protoLogGroup, 769218938, 16, (String) null, objArr);
                        }
                        if (replaceTranitForSplitScreen) {
                            SplitScreenHelper.modifyAnimationForSplitScreen(a, width2, enter3);
                        }
                        int containingWidth = frame.width();
                        int containingHeight = frame.height();
                        a.initialize(containingWidth, containingHeight, width, height);
                        a.scaleCurrentDuration(this.mWmService.getTransitionAnimationScaleLocked());
                    }
                    return a;
                }
            }
            transit2 = transit;
            replaceTranitForSplitScreen = false;
            DisplayContent displayContent2 = getDisplayContent();
            DisplayInfo displayInfo2 = displayContent2.getDisplayInfo();
            int width3 = !isMultiWindow ? getBounds().width() : displayInfo2.appWidth;
            int height2 = !isMultiWindow ? getBounds().height() : displayInfo2.appHeight;
            if (ProtoLogCache.WM_DEBUG_APP_TRANSITIONS_ANIM_enabled) {
            }
            Rect frame2 = new Rect(0, 0, width3, height2);
            Rect displayFrame2 = new Rect(0, 0, displayInfo2.logicalWidth, displayInfo2.logicalHeight);
            Rect insets2 = new Rect();
            Rect stableInsets2 = new Rect();
            Rect surfaceInsets2 = new Rect();
            if (!isMultiWindow) {
            }
            if (this.mLaunchTaskBehind) {
            }
            if (ProtoLogCache.WM_DEBUG_APP_TRANSITIONS_enabled) {
            }
            Configuration displayConfig2 = displayContent2.getConfiguration();
            boolean enter32 = enter2;
            int width22 = transit2;
            boolean z22 = z;
            a = getDisplayContent().mAppTransition.loadAnimation(lp, transit2, enter2, displayConfig2.uiMode, displayConfig2.orientation, frame2, displayFrame2, insets2, surfaceInsets2, stableInsets2, isVoiceInteraction, inFreeformWindowingMode(), this);
            if (a != null) {
            }
            return a;
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public RemoteAnimationTarget createRemoteAnimationTarget(RemoteAnimationController.RemoteAnimationRecord record) {
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean canCreateRemoteAnimationTarget() {
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean canBeAnimationTarget() {
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean okToDisplay() {
        DisplayContent dc = getDisplayContent();
        return dc != null && dc.okToDisplay();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean okToAnimate() {
        return okToAnimate(false, false);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean okToAnimate(boolean ignoreFrozen, boolean ignoreScreenOn) {
        DisplayContent dc = getDisplayContent();
        return dc != null && dc.okToAnimate(ignoreFrozen, ignoreScreenOn);
    }

    public void commitPendingTransaction() {
        scheduleAnimation();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void transformFrameToSurfacePosition(int left, int top, Point outPoint) {
        outPoint.set(left, top);
        WindowContainer parentWindowContainer = getParent();
        if (parentWindowContainer == null) {
            return;
        }
        Rect parentBounds = parentWindowContainer.getBounds();
        outPoint.offset(-parentBounds.left, -parentBounds.top);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void reassignLayer(SurfaceControl.Transaction t) {
        WindowContainer parent = getParent();
        if (parent != null) {
            parent.assignChildLayers(t);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void resetSurfacePositionForAnimationLeash(SurfaceControl.Transaction t) {
        t.setPosition(this.mSurfaceControl, 0.0f, 0.0f);
        this.mLastSurfacePosition.set(0, 0);
    }

    public void onAnimationLeashCreated(SurfaceControl.Transaction t, SurfaceControl leash) {
        this.mLastLayer = -1;
        this.mAnimationLeash = leash;
        reassignLayer(t);
        resetSurfacePositionForAnimationLeash(t);
    }

    public void onAnimationLeashLost(SurfaceControl.Transaction t) {
        this.mLastLayer = -1;
        this.mWmService.mSurfaceAnimationRunner.onAnimationLeashLost(this.mAnimationLeash, t);
        this.mAnimationLeash = null;
        this.mNeedsZBoost = false;
        reassignLayer(t);
        updateSurfacePosition(t);
    }

    public SurfaceControl getAnimationLeash() {
        return this.mAnimationLeash;
    }

    private void doAnimationFinished(int type, AnimationAdapter anim) {
        for (int i = 0; i < this.mSurfaceAnimationSources.size(); i++) {
            this.mSurfaceAnimationSources.valueAt(i).onAnimationFinished(type, anim);
        }
        this.mSurfaceAnimationSources.clear();
        DisplayContent displayContent = this.mDisplayContent;
        if (displayContent != null) {
            displayContent.onWindowAnimationFinished(this, type);
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void onAnimationFinished(int type, AnimationAdapter anim) {
        doAnimationFinished(type, anim);
        this.mWmService.onAnimationFinished();
        this.mNeedsZBoost = false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public AnimationAdapter getAnimation() {
        return this.mSurfaceAnimator.getAnimation();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public WindowContainer getAnimatingContainer(int flags, int typesToCheck) {
        if (isSelfAnimating(flags, typesToCheck)) {
            return this;
        }
        if ((flags & 2) != 0) {
            for (WindowContainer parent = getParent(); parent != null; parent = parent.getParent()) {
                if (parent.isSelfAnimating(flags, typesToCheck)) {
                    return parent;
                }
            }
        }
        if ((flags & 4) != 0) {
            for (int i = 0; i < this.mChildren.size(); i++) {
                WindowContainer wc = this.mChildren.get(i).getAnimatingContainer(flags & (-3), typesToCheck);
                if (wc != null) {
                    return wc;
                }
            }
            return null;
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public boolean isSelfAnimating(int flags, int typesToCheck) {
        if (!this.mSurfaceAnimator.isAnimating() || (this.mSurfaceAnimator.getAnimationType() & typesToCheck) <= 0) {
            return (flags & 1) != 0 && isWaitingForTransitionStart();
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Deprecated
    public final WindowContainer getAnimatingContainer() {
        return getAnimatingContainer(2, -1);
    }

    void startDelayingAnimationStart() {
        this.mSurfaceAnimator.startDelayingAnimationStart();
    }

    void endDelayingAnimationStart() {
        this.mSurfaceAnimator.endDelayingAnimationStart();
    }

    public int getSurfaceWidth() {
        return this.mSurfaceControl.getWidth();
    }

    public int getSurfaceHeight() {
        return this.mSurfaceControl.getHeight();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dump(PrintWriter pw, String prefix, boolean dumpAll) {
        if (this.mSurfaceAnimator.isAnimating()) {
            pw.print(prefix);
            pw.println("ContainerAnimator:");
            this.mSurfaceAnimator.dump(pw, prefix + "  ");
        }
        if (this.mLastOrientationSource != null && this == this.mDisplayContent) {
            pw.println(prefix + "mLastOrientationSource=" + this.mLastOrientationSource);
            pw.println(prefix + "deepestLastOrientationSource=" + getLastOrientationSource());
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public final void updateSurfacePositionNonOrganized() {
        if (isOrganized()) {
            return;
        }
        updateSurfacePosition(getSyncTransaction());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateSurfacePosition(SurfaceControl.Transaction t) {
        if (this.mSurfaceControl == null || this.mSurfaceAnimator.hasLeash() || this.mSurfaceFreezer.hasLeash()) {
            return;
        }
        getRelativePosition(this.mTmpPos);
        int deltaRotation = getRelativeDisplayRotation();
        if (this.mTmpPos.equals(this.mLastSurfacePosition) && deltaRotation == this.mLastDeltaRotation) {
            return;
        }
        t.setPosition(this.mSurfaceControl, this.mTmpPos.x, this.mTmpPos.y);
        this.mLastSurfacePosition.set(this.mTmpPos.x, this.mTmpPos.y);
        if (this.mTransitionController.isShellTransitionsEnabled() && !this.mTransitionController.useShellTransitionsRotation()) {
            if (deltaRotation != 0) {
                updateSurfaceRotation(t, deltaRotation, null);
            } else if (deltaRotation != this.mLastDeltaRotation) {
                t.setMatrix(this.mSurfaceControl, 1.0f, 0.0f, 0.0f, 1.0f);
            }
        }
        this.mLastDeltaRotation = deltaRotation;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void updateSurfaceRotation(SurfaceControl.Transaction t, int deltaRotation, SurfaceControl positionLeash) {
        RotationUtils.rotateSurface(t, this.mSurfaceControl, deltaRotation);
        this.mTmpPos.set(this.mLastSurfacePosition.x, this.mLastSurfacePosition.y);
        Rect parentBounds = getParent().getBounds();
        boolean flipped = deltaRotation % 2 != 0;
        RotationUtils.rotatePoint(this.mTmpPos, deltaRotation, flipped ? parentBounds.height() : parentBounds.width(), flipped ? parentBounds.width() : parentBounds.height());
        t.setPosition(positionLeash != null ? positionLeash : this.mSurfaceControl, this.mTmpPos.x, this.mTmpPos.y);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Point getLastSurfacePosition() {
        return this.mLastSurfacePosition;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void getAnimationFrames(Rect outFrame, Rect outInsets, Rect outStableInsets, Rect outSurfaceInsets) {
        DisplayInfo displayInfo = getDisplayContent().getDisplayInfo();
        outFrame.set(0, 0, displayInfo.appWidth, displayInfo.appHeight);
        outInsets.setEmpty();
        outStableInsets.setEmpty();
        outSurfaceInsets.setEmpty();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void getRelativePosition(Point outPos) {
        Rect dispBounds = getBounds();
        outPos.set(dispBounds.left, dispBounds.top);
        WindowContainer parent = getParent();
        if (parent != null) {
            Rect parentBounds = parent.getBounds();
            outPos.offset(-parentBounds.left, -parentBounds.top);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getRelativeDisplayRotation() {
        WindowContainer parent = getParent();
        if (parent == null) {
            return 0;
        }
        int rotation = getWindowConfiguration().getDisplayRotation();
        int parentRotation = parent.getWindowConfiguration().getDisplayRotation();
        return RotationUtils.deltaRotation(rotation, parentRotation);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void waitForAllWindowsDrawn() {
        forAllWindows(new Consumer() { // from class: com.android.server.wm.WindowContainer$$ExternalSyntheticLambda5
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                WindowContainer.this.m8463x72cccc4b((WindowState) obj);
            }
        }, true);
        TranFoldWMCustody.instance().startWaitForWindowsDrawn(this);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$waitForAllWindowsDrawn$13$com-android-server-wm-WindowContainer  reason: not valid java name */
    public /* synthetic */ void m8463x72cccc4b(WindowState w) {
        w.requestDrawIfNeeded(this.mWaitingForDrawn);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Dimmer getDimmer() {
        WindowContainer<WindowContainer> windowContainer = this.mParent;
        if (windowContainer == null) {
            return null;
        }
        return windowContainer.getDimmer();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setSurfaceControl(SurfaceControl sc) {
        this.mSurfaceControl = sc;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public RemoteAnimationDefinition getRemoteAnimationDefinition() {
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Task asTask() {
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public TaskFragment asTaskFragment() {
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public WindowToken asWindowToken() {
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public WindowState asWindowState() {
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActivityRecord asActivityRecord() {
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public WallpaperWindowToken asWallpaperToken() {
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public DisplayArea asDisplayArea() {
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public RootDisplayArea asRootDisplayArea() {
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public TaskDisplayArea asTaskDisplayArea() {
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public DisplayContent asDisplayContent() {
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isOrganized() {
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isEmbedded() {
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean showSurfaceOnCreation() {
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean showWallpaper() {
        if (!isVisibleRequested() || inMultiWindowMode()) {
            return false;
        }
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            WindowContainer child = this.mChildren.get(i);
            if (child.showWallpaper()) {
                return true;
            }
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static WindowContainer fromBinder(IBinder binder) {
        return RemoteToken.fromBinder(binder).getContainer();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public static class RemoteToken extends IWindowContainerToken.Stub {
        final WeakReference<WindowContainer> mWeakRef;
        private WindowContainerToken mWindowContainerToken;

        /* JADX INFO: Access modifiers changed from: package-private */
        public RemoteToken(WindowContainer container) {
            this.mWeakRef = new WeakReference<>(container);
        }

        WindowContainer getContainer() {
            return this.mWeakRef.get();
        }

        static RemoteToken fromBinder(IBinder binder) {
            return (RemoteToken) binder;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public WindowContainerToken toWindowContainerToken() {
            if (this.mWindowContainerToken == null) {
                this.mWindowContainerToken = new WindowContainerToken(this);
            }
            return this.mWindowContainerToken;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder(128);
            sb.append("RemoteToken{");
            sb.append(Integer.toHexString(System.identityHashCode(this)));
            sb.append(' ');
            sb.append(this.mWeakRef.get());
            sb.append('}');
            return sb.toString();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean onSyncFinishedDrawing() {
        if (this.mSyncState == 0) {
            return false;
        }
        this.mSyncState = 2;
        this.mWmService.mWindowPlacerLocked.requestTraversal();
        if (ProtoLogCache.WM_DEBUG_SYNC_ENGINE_enabled) {
            String protoLogParam0 = String.valueOf(this);
            ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_SYNC_ENGINE, -1918702467, 0, (String) null, new Object[]{protoLogParam0});
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setSyncGroup(BLASTSyncEngine.SyncGroup group) {
        BLASTSyncEngine.SyncGroup syncGroup;
        if (ProtoLogCache.WM_DEBUG_SYNC_ENGINE_enabled) {
            long protoLogParam0 = group.mSyncId;
            String protoLogParam1 = String.valueOf(this);
            ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_SYNC_ENGINE, 959486822, 1, (String) null, new Object[]{Long.valueOf(protoLogParam0), protoLogParam1});
        }
        if (group != null && (syncGroup = this.mSyncGroup) != null && syncGroup != group) {
            throw new IllegalStateException("Can't sync on 2 engines simultaneously currentSyncId=" + this.mSyncGroup.mSyncId + " newSyncId=" + group.mSyncId);
        }
        this.mSyncGroup = group;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean prepareSync() {
        if (this.mSyncState != 0) {
            return false;
        }
        for (int i = getChildCount() - 1; i >= 0; i--) {
            WindowContainer child = getChildAt(i);
            child.prepareSync();
        }
        this.mSyncState = 2;
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean useBLASTSync() {
        return this.mSyncState != 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void finishSync(SurfaceControl.Transaction outMergedTransaction, boolean cancel) {
        BLASTSyncEngine.SyncGroup syncGroup;
        if (this.mSyncState == 0) {
            return;
        }
        if (ProtoLogCache.WM_DEBUG_SYNC_ENGINE_enabled) {
            String protoLogParam1 = String.valueOf(this);
            ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_SYNC_ENGINE, 371173718, 3, (String) null, new Object[]{Boolean.valueOf(cancel), protoLogParam1});
        }
        outMergedTransaction.merge(this.mSyncTransaction);
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            this.mChildren.get(i).finishSync(outMergedTransaction, cancel);
        }
        if (cancel && (syncGroup = this.mSyncGroup) != null) {
            syncGroup.onCancelSync(this);
        }
        clearSyncState();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void clearSyncState() {
        this.mSyncState = 0;
        this.mSyncGroup = null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isSyncFinished() {
        if (isVisibleRequested()) {
            if (this.mSyncState == 0) {
                prepareSync();
            }
            if (this.mSyncState == 1) {
                return false;
            }
            for (int i = this.mChildren.size() - 1; i >= 0; i--) {
                WindowContainer child = this.mChildren.get(i);
                boolean childFinished = child.isSyncFinished();
                if (childFinished && child.isVisibleRequested() && child.fillsParent()) {
                    return true;
                }
                if (!childFinished) {
                    return false;
                }
            }
            return true;
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean allSyncFinished() {
        if (isVisibleRequested()) {
            if (this.mSyncState != 2) {
                return false;
            }
            for (int i = this.mChildren.size() - 1; i >= 0; i--) {
                WindowContainer child = this.mChildren.get(i);
                if (!child.allSyncFinished()) {
                    return false;
                }
            }
            return true;
        }
        return true;
    }

    private void onSyncReparent(WindowContainer oldParent, WindowContainer newParent) {
        if (newParent == null || newParent.mSyncState == 0) {
            if (this.mSyncState == 0) {
                return;
            }
            if (newParent == null) {
                if (oldParent.mSyncState != 0) {
                    finishSync(oldParent.mSyncTransaction, true);
                    return;
                }
                BLASTSyncEngine.SyncGroup syncGroup = this.mSyncGroup;
                if (syncGroup != null) {
                    finishSync(syncGroup.getOrphanTransaction(), true);
                    return;
                }
                throw new IllegalStateException("This container is in sync mode without a sync group: " + this);
            } else if (this.mSyncGroup == null) {
                finishSync(getPendingTransaction(), true);
                return;
            }
        }
        if (this.mTransitionController.isShellTransitionsEnabled()) {
            this.mSyncState = 0;
        }
        prepareSync();
    }

    void registerWindowContainerListener(WindowContainerListener listener) {
        registerWindowContainerListener(listener, true);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void registerWindowContainerListener(WindowContainerListener listener, boolean shouldDispatchConfig) {
        if (this.mListeners.contains(listener)) {
            return;
        }
        this.mListeners.add(listener);
        registerConfigurationChangeListener(listener, shouldDispatchConfig);
        if (shouldDispatchConfig) {
            listener.onDisplayChanged(getDisplayContent());
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void unregisterWindowContainerListener(WindowContainerListener listener) {
        this.mListeners.remove(listener);
        unregisterConfigurationChangeListener(listener);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void overrideConfigurationPropagation(WindowContainer<?> receiver, final WindowContainer<?> supplier) {
        final ConfigurationContainerListener listener = new ConfigurationContainerListener() { // from class: com.android.server.wm.WindowContainer.1
            @Override // com.android.server.wm.ConfigurationContainerListener
            public void onMergedOverrideConfigurationChanged(Configuration mergedOverrideConfig) {
                WindowContainer.this.onRequestedOverrideConfigurationChanged(supplier.getConfiguration());
            }
        };
        supplier.registerConfigurationChangeListener(listener);
        receiver.registerWindowContainerListener(new WindowContainerListener() { // from class: com.android.server.wm.WindowContainer.2
            @Override // com.android.server.wm.WindowContainerListener
            public void onRemoved() {
                WindowContainer.this.unregisterWindowContainerListener(this);
                supplier.unregisterConfigurationChangeListener(listener);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getWindowType() {
        return -1;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean setCanScreenshot(SurfaceControl.Transaction t, boolean canScreenshot) {
        SurfaceControl surfaceControl = this.mSurfaceControl;
        if (surfaceControl == null) {
            return false;
        }
        t.setSecure(surfaceControl, !canScreenshot);
        return true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class AnimationRunnerBuilder {
        private final List<Runnable> mOnAnimationCancelled;
        private final List<Runnable> mOnAnimationFinished;

        private AnimationRunnerBuilder() {
            this.mOnAnimationFinished = new LinkedList();
            this.mOnAnimationCancelled = new LinkedList();
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void setTaskBackgroundColor(int backgroundColor) {
            final TaskDisplayArea taskDisplayArea = WindowContainer.this.getTaskDisplayArea();
            if (taskDisplayArea != null && backgroundColor != 0) {
                taskDisplayArea.setBackgroundColor(backgroundColor);
                final AtomicInteger callbackCounter = new AtomicInteger(0);
                Runnable clearBackgroundColorHandler = new Runnable() { // from class: com.android.server.wm.WindowContainer$AnimationRunnerBuilder$$ExternalSyntheticLambda3
                    @Override // java.lang.Runnable
                    public final void run() {
                        WindowContainer.AnimationRunnerBuilder.lambda$setTaskBackgroundColor$0(callbackCounter, taskDisplayArea);
                    }
                };
                this.mOnAnimationFinished.add(clearBackgroundColorHandler);
                this.mOnAnimationCancelled.add(clearBackgroundColorHandler);
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public static /* synthetic */ void lambda$setTaskBackgroundColor$0(AtomicInteger callbackCounter, TaskDisplayArea taskDisplayArea) {
            if (callbackCounter.getAndIncrement() == 0) {
                taskDisplayArea.clearBackgroundColor();
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void hideInsetSourceViewOverflows(Set<Integer> insetTypes) {
            new ArrayList(insetTypes.size());
            for (Integer num : insetTypes) {
                int insetType = num.intValue();
                final WindowContainerInsetsSourceProvider insetProvider = WindowContainer.this.getDisplayContent().getInsetsStateController().getSourceProvider(insetType);
                insetProvider.setCropToProvidingInsetsBounds(WindowContainer.this.getPendingTransaction());
                this.mOnAnimationFinished.add(new Runnable() { // from class: com.android.server.wm.WindowContainer$AnimationRunnerBuilder$$ExternalSyntheticLambda5
                    @Override // java.lang.Runnable
                    public final void run() {
                        WindowContainer.AnimationRunnerBuilder.this.m8470xa9508495(insetProvider);
                    }
                });
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$hideInsetSourceViewOverflows$1$com-android-server-wm-WindowContainer$AnimationRunnerBuilder  reason: not valid java name */
        public /* synthetic */ void m8470xa9508495(WindowContainerInsetsSourceProvider insetProvider) {
            insetProvider.removeCropToProvidingInsetsBounds(WindowContainer.this.getPendingTransaction());
        }

        /* JADX INFO: Access modifiers changed from: private */
        public IAnimationStarter build() {
            return new IAnimationStarter() { // from class: com.android.server.wm.WindowContainer$AnimationRunnerBuilder$$ExternalSyntheticLambda4
                @Override // com.android.server.wm.WindowContainer.IAnimationStarter
                public final void startAnimation(SurfaceControl.Transaction transaction, AnimationAdapter animationAdapter, boolean z, int i, AnimationAdapter animationAdapter2) {
                    WindowContainer.AnimationRunnerBuilder.this.m8469xbcf2a54(transaction, animationAdapter, z, i, animationAdapter2);
                }
            };
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$build$4$com-android-server-wm-WindowContainer$AnimationRunnerBuilder  reason: not valid java name */
        public /* synthetic */ void m8469xbcf2a54(SurfaceControl.Transaction t, AnimationAdapter adapter, boolean hidden, int type, AnimationAdapter snapshotAnim) {
            WindowContainer windowContainer = WindowContainer.this;
            windowContainer.startAnimation(windowContainer.getPendingTransaction(), adapter, !WindowContainer.this.isVisible(), type, new SurfaceAnimator.OnAnimationFinishedCallback() { // from class: com.android.server.wm.WindowContainer$AnimationRunnerBuilder$$ExternalSyntheticLambda1
                @Override // com.android.server.wm.SurfaceAnimator.OnAnimationFinishedCallback
                public final void onAnimationFinished(int i, AnimationAdapter animationAdapter) {
                    WindowContainer.AnimationRunnerBuilder.this.m8467xe3524b16(i, animationAdapter);
                }
            }, new Runnable() { // from class: com.android.server.wm.WindowContainer$AnimationRunnerBuilder$$ExternalSyntheticLambda2
                @Override // java.lang.Runnable
                public final void run() {
                    WindowContainer.AnimationRunnerBuilder.this.m8468x7790bab5();
                }
            }, snapshotAnim);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$build$2$com-android-server-wm-WindowContainer$AnimationRunnerBuilder  reason: not valid java name */
        public /* synthetic */ void m8467xe3524b16(int animType, AnimationAdapter anim) {
            this.mOnAnimationFinished.forEach(new WindowContainer$AnimationRunnerBuilder$$ExternalSyntheticLambda0());
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$build$3$com-android-server-wm-WindowContainer$AnimationRunnerBuilder  reason: not valid java name */
        public /* synthetic */ void m8468x7790bab5() {
            this.mOnAnimationCancelled.forEach(new WindowContainer$AnimationRunnerBuilder$$ExternalSyntheticLambda0());
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void addTrustedOverlay(SurfaceControlViewHost.SurfacePackage overlay, WindowState initialWindowState) {
        if (this.mOverlayHost == null) {
            this.mOverlayHost = new TrustedOverlayHost(this.mWmService);
        }
        this.mOverlayHost.addOverlay(overlay, this.mSurfaceControl);
        try {
            overlay.getRemoteInterface().onConfigurationChanged(getConfiguration());
        } catch (Exception e) {
            if (ProtoLogCache.WM_DEBUG_ANIM_enabled) {
                ProtoLogImpl.e(ProtoLogGroup.WM_DEBUG_ANIM, -32102932, 0, (String) null, (Object[]) null);
            }
            removeTrustedOverlay(overlay);
        }
        if (initialWindowState != null) {
            InsetsState insetsState = initialWindowState.getInsetsState();
            Rect dispBounds = getBounds();
            try {
                overlay.getRemoteInterface().onInsetsChanged(insetsState, dispBounds);
            } catch (Exception e2) {
                if (ProtoLogCache.WM_DEBUG_ANIM_enabled) {
                    ProtoLogImpl.e(ProtoLogGroup.WM_DEBUG_ANIM, 1288920916, 0, (String) null, (Object[]) null);
                }
                removeTrustedOverlay(overlay);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void removeTrustedOverlay(SurfaceControlViewHost.SurfacePackage overlay) {
        TrustedOverlayHost trustedOverlayHost = this.mOverlayHost;
        if (trustedOverlayHost != null && !trustedOverlayHost.removeOverlay(overlay)) {
            this.mOverlayHost.release();
            this.mOverlayHost = null;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateOverlayInsetsState(WindowState originalChange) {
        WindowContainer p = getParent();
        if (p != null) {
            p.updateOverlayInsetsState(originalChange);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void waitForSyncTransactionCommit(ArraySet<WindowContainer> wcAwaitingCommit) {
        if (wcAwaitingCommit.contains(this)) {
            return;
        }
        this.mSyncTransactionCommitCallbackDepth++;
        wcAwaitingCommit.add(this);
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            this.mChildren.get(i).waitForSyncTransactionCommit(wcAwaitingCommit);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onSyncTransactionCommitted(SurfaceControl.Transaction t) {
        int i = this.mSyncTransactionCommitCallbackDepth - 1;
        this.mSyncTransactionCommitCallbackDepth = i;
        if (i > 0 || this.mSyncState != 0) {
            return;
        }
        t.merge(this.mSyncTransaction);
    }

    void hookScreenSplitPackageChanged(WindowContainer newParent, WindowContainer oldParent, int position) {
        Task curTask;
        if (ITranGriffinFeature.Instance().isGriffinSupport() && (curTask = asTask()) != null && curTask.getParent() != null) {
            int displayId = curTask.getDisplayId();
            String pkg = curTask.realActivity == null ? "null" : curTask.realActivity.getPackageName();
            if (curTask.realActivity != null) {
                if (oldParent.getWindowingMode() == 3 && newParent.getWindowingMode() != 3) {
                    Slog.d("Griffin/Split", "exit split screen , pkg:" + pkg);
                    ITranGriffinFeature.Instance().hookScreenSplitPackageChanged(displayId, null);
                } else if (oldParent.getWindowingMode() != 3 && newParent.getWindowingMode() == 3) {
                    Slog.d("Griffin/Split", "enter split screen , pkg:" + pkg);
                    ITranGriffinFeature.Instance().hookScreenSplitPackageChanged(displayId, pkg);
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasChildOSFullDialog() {
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            if (this.mChildren.get(i).isOSFullDialog()) {
                return true;
            }
        }
        return false;
    }

    @Override // com.android.server.wm.ConfigurationContainer
    public boolean isOSFullDialog() {
        ActivityRecord activityRecord;
        TaskFragment taskFragment;
        Task parentTask;
        WindowContainer parent = getParent();
        if (parent == null || (activityRecord = parent.asActivityRecord()) == null || (taskFragment = activityRecord.getOrganizedTaskFragment()) == null || (parentTask = taskFragment.getTask()) == null) {
            return false;
        }
        Rect taskBounds = parentTask.getBounds();
        Rect taskFragBounds = taskFragment.getBounds();
        return super.isOSFullDialog() && !taskBounds.equals(taskFragBounds) && taskBounds.contains(taskFragBounds) && WallpaperController.OS_FOLEABLE_SCREEN_SUPPORT;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void moveChildAdjacentWithChild(E child, Predicate<E> target, boolean moveDown) {
        int childIndex = getChildIndex(child);
        if (moveDown) {
            for (int i = childIndex - 1; i >= 0; i--) {
                E wc = getChildAt(i);
                if (target.test(wc)) {
                    positionChildAt(i + 1, child, false);
                    return;
                }
            }
            return;
        }
        for (int i2 = childIndex + 1; i2 <= getChildCount() - 1; i2++) {
            E wc2 = getChildAt(i2);
            if (target.test(wc2)) {
                positionChildAt(i2 - 1, child, false);
                return;
            }
        }
    }

    int getChildIndex(E child) {
        return this.mChildren.indexOf(child);
    }

    private boolean isMultiWindow() {
        if (getWindowingMode() == 6) {
            return true;
        }
        if (this instanceof Task) {
            Task task = (Task) this;
            if (SplitScreenHelper.isSplitScreenTask(task)) {
                Slog.w("WindowManager", "Warning: " + task + " is split screen task, but windowing mode is not multi-window");
                return true;
            }
            return false;
        }
        return false;
    }
}
