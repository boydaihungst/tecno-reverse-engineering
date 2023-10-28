package com.android.server.wm;

import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Debug;
import android.os.IBinder;
import android.util.proto.ProtoOutputStream;
import android.view.DisplayInfo;
import android.view.InsetsState;
import android.view.SurfaceControl;
import com.android.internal.protolog.ProtoLogGroup;
import com.android.internal.protolog.ProtoLogImpl;
import com.android.server.policy.WindowManagerPolicy;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.function.Consumer;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class WindowToken extends WindowContainer<WindowState> {
    private static final String TAG = "WindowManager";
    private boolean mClientVisible;
    private SurfaceControl mFixedRotationTransformLeash;
    private FixedRotationTransformState mFixedRotationTransformState;
    private final boolean mFromClientToken;
    final Bundle mOptions;
    final boolean mOwnerCanManageAppTokens;
    boolean mPersistOnEmpty;
    final boolean mRoundedCornerOverlay;
    private final Comparator<WindowState> mWindowComparator;
    boolean paused;
    String stringName;
    final IBinder token;
    boolean waitingToShow;
    final int windowType;

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static class FixedRotationTransformState {
        final DisplayFrames mDisplayFrames;
        final DisplayInfo mDisplayInfo;
        final Configuration mRotatedOverrideConfiguration;
        final SeamlessRotator mRotator;
        final ArrayList<WindowToken> mAssociatedTokens = new ArrayList<>(3);
        final ArrayList<WindowContainer<?>> mRotatedContainers = new ArrayList<>(3);
        boolean mIsTransforming = true;

        FixedRotationTransformState(DisplayInfo rotatedDisplayInfo, DisplayFrames rotatedDisplayFrames, Configuration rotatedConfig, int currentRotation) {
            this.mDisplayInfo = rotatedDisplayInfo;
            this.mDisplayFrames = rotatedDisplayFrames;
            this.mRotatedOverrideConfiguration = rotatedConfig;
            this.mRotator = new SeamlessRotator(rotatedDisplayInfo.rotation, currentRotation, rotatedDisplayInfo, true);
        }

        void transform(WindowContainer<?> container) {
            this.mRotator.unrotate(container.getPendingTransaction(), container);
            if (!this.mRotatedContainers.contains(container)) {
                this.mRotatedContainers.add(container);
            }
        }

        void resetTransform() {
            for (int i = this.mRotatedContainers.size() - 1; i >= 0; i--) {
                WindowContainer<?> c = this.mRotatedContainers.get(i);
                if (c.getParent() != null) {
                    this.mRotator.finish(c.getPendingTransaction(), c);
                }
            }
        }

        void disassociate(WindowToken token) {
            this.mAssociatedTokens.remove(token);
            this.mRotatedContainers.remove(token);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$new$0$com-android-server-wm-WindowToken  reason: not valid java name */
    public /* synthetic */ int m8540lambda$new$0$comandroidserverwmWindowToken(WindowState newWindow, WindowState existingWindow) {
        if (newWindow.mToken == this) {
            if (existingWindow.mToken == this) {
                return isFirstChildWindowGreaterThanSecond(newWindow, existingWindow) ? 1 : -1;
            }
            throw new IllegalArgumentException("existingWindow=" + existingWindow + " is not a child of token=" + this);
        }
        throw new IllegalArgumentException("newWindow=" + newWindow + " is not a child of token=" + this);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public WindowToken(WindowManagerService service, IBinder _token, int type, boolean persistOnEmpty, DisplayContent dc, boolean ownerCanManageAppTokens) {
        this(service, _token, type, persistOnEmpty, dc, ownerCanManageAppTokens, false, false, null);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public WindowToken(WindowManagerService service, IBinder _token, int type, boolean persistOnEmpty, DisplayContent dc, boolean ownerCanManageAppTokens, boolean roundedCornerOverlay, boolean fromClientToken, Bundle options) {
        super(service);
        this.paused = false;
        this.mWindowComparator = new Comparator() { // from class: com.android.server.wm.WindowToken$$ExternalSyntheticLambda0
            @Override // java.util.Comparator
            public final int compare(Object obj, Object obj2) {
                return WindowToken.this.m8540lambda$new$0$comandroidserverwmWindowToken((WindowState) obj, (WindowState) obj2);
            }
        };
        this.token = _token;
        this.windowType = type;
        this.mOptions = options;
        this.mPersistOnEmpty = persistOnEmpty;
        this.mOwnerCanManageAppTokens = ownerCanManageAppTokens;
        this.mRoundedCornerOverlay = roundedCornerOverlay;
        this.mFromClientToken = fromClientToken;
        if (dc != null) {
            dc.addWindowToken(_token, this);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void removeAllWindowsIfPossible() {
        int i = this.mChildren.size() - 1;
        while (i >= 0) {
            WindowState win = (WindowState) this.mChildren.get(i);
            if (ProtoLogCache.WM_DEBUG_WINDOW_MOVEMENT_enabled) {
                String protoLogParam0 = String.valueOf(win);
                ProtoLogImpl.w(ProtoLogGroup.WM_DEBUG_WINDOW_MOVEMENT, 913494177, 0, (String) null, new Object[]{protoLogParam0});
            }
            win.removeIfPossible();
            if (i > this.mChildren.size()) {
                i = this.mChildren.size();
            }
            i--;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setExiting(boolean animateExit) {
        if (isEmpty()) {
            super.removeImmediately();
            return;
        }
        this.mPersistOnEmpty = false;
        if (!isVisible()) {
            return;
        }
        int count = this.mChildren.size();
        boolean changed = false;
        for (int i = 0; i < count; i++) {
            WindowState win = (WindowState) this.mChildren.get(i);
            changed |= win.onSetAppExiting(animateExit);
        }
        if (changed) {
            this.mWmService.mWindowPlacerLocked.performSurfacePlacement();
            this.mWmService.updateFocusedWindowLocked(0, false);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public float getSizeCompatScale() {
        return this.mDisplayContent.mCompatibleScreenScale;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasSizeCompatBounds() {
        return false;
    }

    protected boolean isFirstChildWindowGreaterThanSecond(WindowState newWindow, WindowState existingWindow) {
        return newWindow.mBaseLayer >= existingWindow.mBaseLayer;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void addWindow(WindowState win) {
        if (ProtoLogCache.WM_DEBUG_FOCUS_enabled) {
            String protoLogParam0 = String.valueOf(win);
            String protoLogParam1 = String.valueOf(Debug.getCallers(5));
            ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_FOCUS, 1219600119, 0, (String) null, new Object[]{protoLogParam0, protoLogParam1});
        }
        if (win.isChildWindow()) {
            return;
        }
        if (this.mSurfaceControl == null) {
            createSurfaceControl(true);
            reassignLayer(getSyncTransaction());
        }
        if (!this.mChildren.contains(win)) {
            if (ProtoLogCache.WM_DEBUG_ADD_REMOVE_enabled) {
                String protoLogParam02 = String.valueOf(win);
                String protoLogParam12 = String.valueOf(this);
                ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_ADD_REMOVE, 241961619, 0, (String) null, new Object[]{protoLogParam02, protoLogParam12});
            }
            addChild((WindowToken) win, (Comparator<WindowToken>) this.mWindowComparator);
            this.mWmService.mWindowsChanged = true;
        }
    }

    @Override // com.android.server.wm.WindowContainer
    void createSurfaceControl(boolean force) {
        if (!this.mFromClientToken || force) {
            super.createSurfaceControl(force);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isEmpty() {
        return this.mChildren.isEmpty();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public WindowState getReplacingWindow() {
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            WindowState win = (WindowState) this.mChildren.get(i);
            WindowState replacing = win.getReplacingWindow();
            if (replacing != null) {
                return replacing;
            }
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean windowsCanBeWallpaperTarget() {
        for (int j = this.mChildren.size() - 1; j >= 0; j--) {
            WindowState w = (WindowState) this.mChildren.get(j);
            if (w.hasWallpaper()) {
                return true;
            }
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.wm.WindowContainer
    public void removeImmediately() {
        if (this.mDisplayContent != null) {
            this.mDisplayContent.removeWindowToken(this.token, true);
        }
        super.removeImmediately();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.wm.WindowContainer
    public void onDisplayChanged(DisplayContent dc) {
        dc.reParentWindowToken(this);
        super.onDisplayChanged(dc);
    }

    @Override // com.android.server.wm.WindowContainer
    void assignLayer(SurfaceControl.Transaction t, int layer) {
        if (this.windowType == 2034) {
            super.assignRelativeLayer(t, this.mDisplayContent.getDefaultTaskDisplayArea().getSplitScreenDividerAnchor(), 1);
        } else if (this.mRoundedCornerOverlay) {
            super.assignLayer(t, WindowManagerPolicy.SCREEN_DECOR_DISPLAY_OVERLAY_LAYER);
        } else {
            super.assignLayer(t, layer);
        }
    }

    @Override // com.android.server.wm.WindowContainer
    SurfaceControl.Builder makeSurface() {
        SurfaceControl.Builder builder = super.makeSurface();
        if (this.mRoundedCornerOverlay) {
            if (this.windowType == 2045) {
                builder.setParent(null);
            } else {
                builder.setParent(getDisplayContent().getOverlayLayer());
            }
        }
        return builder;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isClientVisible() {
        return this.mClientVisible;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setClientVisible(boolean clientVisible) {
        if (this.mClientVisible == clientVisible) {
            return;
        }
        if (ProtoLogCache.WM_DEBUG_APP_TRANSITIONS_enabled) {
            String protoLogParam0 = String.valueOf(this);
            String protoLogParam2 = String.valueOf(Debug.getCallers(5));
            ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_APP_TRANSITIONS, -2093859262, 12, (String) null, new Object[]{protoLogParam0, Boolean.valueOf(clientVisible), protoLogParam2});
        }
        this.mClientVisible = clientVisible;
        sendAppVisibilityToClients();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasFixedRotationTransform() {
        return this.mFixedRotationTransformState != null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasFixedRotationTransform(WindowToken token) {
        FixedRotationTransformState fixedRotationTransformState = this.mFixedRotationTransformState;
        if (fixedRotationTransformState == null || token == null) {
            return false;
        }
        return this == token || fixedRotationTransformState == token.mFixedRotationTransformState;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isFinishingFixedRotationTransform() {
        FixedRotationTransformState fixedRotationTransformState = this.mFixedRotationTransformState;
        return (fixedRotationTransformState == null || fixedRotationTransformState.mIsTransforming) ? false : true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isFixedRotationTransforming() {
        FixedRotationTransformState fixedRotationTransformState = this.mFixedRotationTransformState;
        return fixedRotationTransformState != null && fixedRotationTransformState.mIsTransforming;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public DisplayInfo getFixedRotationTransformDisplayInfo() {
        if (isFixedRotationTransforming()) {
            return this.mFixedRotationTransformState.mDisplayInfo;
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public DisplayFrames getFixedRotationTransformDisplayFrames() {
        if (isFixedRotationTransforming()) {
            return this.mFixedRotationTransformState.mDisplayFrames;
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Rect getFixedRotationTransformMaxBounds() {
        if (isFixedRotationTransforming()) {
            return this.mFixedRotationTransformState.mRotatedOverrideConfiguration.windowConfiguration.getMaxBounds();
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Rect getFixedRotationTransformDisplayBounds() {
        if (isFixedRotationTransforming()) {
            return this.mFixedRotationTransformState.mRotatedOverrideConfiguration.windowConfiguration.getBounds();
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public InsetsState getFixedRotationTransformInsetsState() {
        if (isFixedRotationTransforming()) {
            return this.mFixedRotationTransformState.mDisplayFrames.mInsetsState;
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void applyFixedRotationTransform(DisplayInfo info, DisplayFrames displayFrames, Configuration config) {
        FixedRotationTransformState fixedRotationTransformState = this.mFixedRotationTransformState;
        if (fixedRotationTransformState != null) {
            fixedRotationTransformState.disassociate(this);
        }
        Configuration overrideConfig = new Configuration(config);
        overrideConfig.screenLayout = TaskFragment.computeScreenLayoutOverride(overrideConfig.screenLayout, overrideConfig.screenWidthDp, overrideConfig.screenHeightDp);
        FixedRotationTransformState fixedRotationTransformState2 = new FixedRotationTransformState(info, displayFrames, overrideConfig, this.mDisplayContent.getRotation());
        this.mFixedRotationTransformState = fixedRotationTransformState2;
        fixedRotationTransformState2.mAssociatedTokens.add(this);
        this.mDisplayContent.getDisplayPolicy().simulateLayoutDisplay(displayFrames);
        onFixedRotationStatePrepared();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void linkFixedRotationTransform(WindowToken other) {
        FixedRotationTransformState fixedRotationTransformState;
        FixedRotationTransformState fixedRotationState = other.mFixedRotationTransformState;
        if (fixedRotationState == null || (fixedRotationTransformState = this.mFixedRotationTransformState) == fixedRotationState) {
            return;
        }
        if (fixedRotationTransformState != null) {
            fixedRotationTransformState.disassociate(this);
        }
        this.mFixedRotationTransformState = fixedRotationState;
        fixedRotationState.mAssociatedTokens.add(this);
        onFixedRotationStatePrepared();
    }

    private void onFixedRotationStatePrepared() {
        onConfigurationChanged(getParent().getConfiguration());
        ActivityRecord r = asActivityRecord();
        if (r != null && r.hasProcess()) {
            r.app.registerActivityConfigurationListener(r);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasAnimatingFixedRotationTransition() {
        FixedRotationTransformState fixedRotationTransformState = this.mFixedRotationTransformState;
        if (fixedRotationTransformState == null) {
            return false;
        }
        for (int i = fixedRotationTransformState.mAssociatedTokens.size() - 1; i >= 0; i--) {
            ActivityRecord r = this.mFixedRotationTransformState.mAssociatedTokens.get(i).asActivityRecord();
            if (r != null && r.isAnimating(3)) {
                return true;
            }
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void finishFixedRotationTransform() {
        finishFixedRotationTransform(null);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void finishFixedRotationTransform(Runnable applyDisplayRotation) {
        FixedRotationTransformState state = this.mFixedRotationTransformState;
        if (state == null) {
            return;
        }
        if (this.mTransitionController.isShellTransitionsEnabled()) {
            for (int i = state.mAssociatedTokens.size() - 1; i >= 0; i--) {
                state.mAssociatedTokens.get(i).removeFixedRotationLeash();
            }
        } else {
            state.resetTransform();
        }
        state.mIsTransforming = false;
        if (applyDisplayRotation != null) {
            applyDisplayRotation.run();
        }
        for (int i2 = state.mAssociatedTokens.size() - 1; i2 >= 0; i2--) {
            WindowToken token = state.mAssociatedTokens.get(i2);
            token.mFixedRotationTransformState = null;
            if (applyDisplayRotation == null) {
                token.cancelFixedRotationTransform();
            }
        }
    }

    private void cancelFixedRotationTransform() {
        WindowContainer<?> parent = getParent();
        if (parent == null) {
            return;
        }
        int originalRotation = getWindowConfiguration().getRotation();
        onConfigurationChanged(parent.getConfiguration());
        onCancelFixedRotationTransform(originalRotation);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public SurfaceControl getOrCreateFixedRotationLeash() {
        if (this.mTransitionController.isShellTransitionsEnabled()) {
            int rotation = getRelativeDisplayRotation();
            if (rotation == 0) {
                return this.mFixedRotationTransformLeash;
            }
            SurfaceControl surfaceControl = this.mFixedRotationTransformLeash;
            if (surfaceControl != null) {
                return surfaceControl;
            }
            SurfaceControl.Transaction t = getSyncTransaction();
            SurfaceControl leash = makeSurface().setContainerLayer().setParent(getParentSurfaceControl()).setName(getSurfaceControl() + " - rotation-leash").setHidden(false).setCallsite("WindowToken.getOrCreateFixedRotationLeash").build();
            t.setPosition(leash, this.mLastSurfacePosition.x, this.mLastSurfacePosition.y);
            t.show(leash);
            t.reparent(getSurfaceControl(), leash);
            t.setAlpha(getSurfaceControl(), 1.0f);
            this.mFixedRotationTransformLeash = leash;
            updateSurfaceRotation(t, rotation, leash);
            return this.mFixedRotationTransformLeash;
        }
        return null;
    }

    void removeFixedRotationLeash() {
        if (this.mFixedRotationTransformLeash == null) {
            return;
        }
        SurfaceControl.Transaction t = getSyncTransaction();
        t.reparent(getSurfaceControl(), getParentSurfaceControl());
        t.remove(this.mFixedRotationTransformLeash);
        this.mFixedRotationTransformLeash = null;
    }

    void onCancelFixedRotationTransform(int originalDisplayRotation) {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.wm.ConfigurationContainer
    public void resolveOverrideConfiguration(Configuration newParentConfig) {
        super.resolveOverrideConfiguration(newParentConfig);
        if (isFixedRotationTransforming()) {
            getResolvedOverrideConfiguration().updateFrom(this.mFixedRotationTransformState.mRotatedOverrideConfiguration);
        }
        if (getTaskDisplayArea() == null) {
            getResolvedOverrideConfiguration().windowConfiguration.setWindowingMode(1);
        }
    }

    @Override // com.android.server.wm.WindowContainer
    void updateSurfacePosition(SurfaceControl.Transaction t) {
        super.updateSurfacePosition(t);
        if (!this.mTransitionController.isShellTransitionsEnabled() && isFixedRotationTransforming()) {
            ActivityRecord r = asActivityRecord();
            Task rootTask = r != null ? r.getRootTask() : null;
            if (rootTask == null || !rootTask.inPinnedWindowingMode()) {
                this.mFixedRotationTransformState.transform(this);
            }
        }
    }

    @Override // com.android.server.wm.WindowContainer
    protected void updateSurfaceRotation(SurfaceControl.Transaction t, int deltaRotation, SurfaceControl positionLeash) {
        Task rootTask;
        ActivityRecord r = asActivityRecord();
        if (r != null && (rootTask = r.getRootTask()) != null && this.mTransitionController.getWindowingModeAtStart(rootTask) == 2) {
            return;
        }
        super.updateSurfaceRotation(t, deltaRotation, positionLeash);
    }

    @Override // com.android.server.wm.WindowContainer
    void resetSurfacePositionForAnimationLeash(SurfaceControl.Transaction t) {
        if (!isFixedRotationTransforming()) {
            super.resetSurfacePositionForAnimationLeash(t);
        }
    }

    @Override // com.android.server.wm.WindowContainer, com.android.server.wm.ConfigurationContainer
    public void dumpDebug(ProtoOutputStream proto, long fieldId, int logLevel) {
        if (logLevel == 2 && !isVisible()) {
            return;
        }
        long token = proto.start(fieldId);
        super.dumpDebug(proto, 1146756268033L, logLevel);
        proto.write(1120986464258L, System.identityHashCode(this));
        proto.write(1133871366149L, this.waitingToShow);
        proto.write(1133871366150L, this.paused);
        proto.end(token);
    }

    @Override // com.android.server.wm.WindowContainer
    long getProtoFieldId() {
        return 1146756268039L;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.wm.WindowContainer
    public void dump(PrintWriter pw, String prefix, boolean dumpAll) {
        super.dump(pw, prefix, dumpAll);
        pw.print(prefix);
        pw.print("windows=");
        pw.println(this.mChildren);
        pw.print(prefix);
        pw.print("windowType=");
        pw.print(this.windowType);
        if (this.waitingToShow) {
            pw.print(" waitingToShow=true");
        }
        pw.println();
        if (hasFixedRotationTransform()) {
            pw.print(prefix);
            pw.print("fixedRotationConfig=");
            pw.println(this.mFixedRotationTransformState.mRotatedOverrideConfiguration);
        }
    }

    public String toString() {
        if (this.stringName == null) {
            this.stringName = "WindowToken{" + Integer.toHexString(System.identityHashCode(this)) + " type=" + this.windowType + " " + this.token + "}";
        }
        return this.stringName;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.wm.ConfigurationContainer
    public String getName() {
        return toString();
    }

    @Override // com.android.server.wm.WindowContainer
    WindowToken asWindowToken() {
        return this;
    }

    boolean canLayerAboveSystemBars() {
        int layer = getWindowLayerFromType();
        int navLayer = this.mWmService.mPolicy.getWindowLayerFromTypeLw(2019, this.mOwnerCanManageAppTokens);
        return this.mOwnerCanManageAppTokens && layer > navLayer;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getWindowLayerFromType() {
        return this.mWmService.mPolicy.getWindowLayerFromTypeLw(this.windowType, this.mOwnerCanManageAppTokens, this.mRoundedCornerOverlay);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isFromClient() {
        return this.mFromClientToken;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setInsetsFrozen(final boolean freeze) {
        forAllWindows(new Consumer() { // from class: com.android.server.wm.WindowToken$$ExternalSyntheticLambda1
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                WindowToken.this.m8541lambda$setInsetsFrozen$1$comandroidserverwmWindowToken(freeze, (WindowState) obj);
            }
        }, true);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$setInsetsFrozen$1$com-android-server-wm-WindowToken  reason: not valid java name */
    public /* synthetic */ void m8541lambda$setInsetsFrozen$1$comandroidserverwmWindowToken(boolean freeze, WindowState w) {
        if (w.mToken == this) {
            if (freeze) {
                w.freezeInsetsState();
            } else {
                w.clearFrozenInsetsState();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.wm.WindowContainer
    public int getWindowType() {
        return this.windowType;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public static class Builder {
        private DisplayContent mDisplayContent;
        private boolean mFromClientToken;
        private Bundle mOptions;
        private boolean mOwnerCanManageAppTokens;
        private boolean mPersistOnEmpty;
        private boolean mRoundedCornerOverlay;
        private final WindowManagerService mService;
        private final IBinder mToken;
        private final int mType;

        /* JADX INFO: Access modifiers changed from: package-private */
        public Builder(WindowManagerService service, IBinder token, int type) {
            this.mService = service;
            this.mToken = token;
            this.mType = type;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public Builder setPersistOnEmpty(boolean persistOnEmpty) {
            this.mPersistOnEmpty = persistOnEmpty;
            return this;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public Builder setDisplayContent(DisplayContent dc) {
            this.mDisplayContent = dc;
            return this;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public Builder setOwnerCanManageAppTokens(boolean ownerCanManageAppTokens) {
            this.mOwnerCanManageAppTokens = ownerCanManageAppTokens;
            return this;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public Builder setRoundedCornerOverlay(boolean roundedCornerOverlay) {
            this.mRoundedCornerOverlay = roundedCornerOverlay;
            return this;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public Builder setFromClientToken(boolean fromClientToken) {
            this.mFromClientToken = fromClientToken;
            return this;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public Builder setOptions(Bundle options) {
            this.mOptions = options;
            return this;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public WindowToken build() {
            return new WindowToken(this.mService, this.mToken, this.mType, this.mPersistOnEmpty, this.mDisplayContent, this.mOwnerCanManageAppTokens, this.mRoundedCornerOverlay, this.mFromClientToken, this.mOptions);
        }
    }
}
