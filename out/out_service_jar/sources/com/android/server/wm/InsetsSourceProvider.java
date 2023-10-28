package com.android.server.wm;

import android.app.ThunderbackConfig;
import android.graphics.Insets;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.proto.ProtoOutputStream;
import android.view.InsetsSource;
import android.view.InsetsSourceControl;
import android.view.InsetsState;
import android.view.SurfaceControl;
import com.android.internal.protolog.ProtoLogGroup;
import com.android.internal.protolog.ProtoLogImpl;
import com.android.internal.util.function.TriConsumer;
import com.android.server.wm.SurfaceAnimator;
import java.io.PrintWriter;
import java.util.function.Consumer;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public abstract class InsetsSourceProvider {
    private ControlAdapter mAdapter;
    private boolean mClientVisible;
    private InsetsSourceControl mControl;
    private InsetsControlTarget mControlTarget;
    private final boolean mControllable;
    protected final DisplayContent mDisplayContent;
    private final InsetsSourceControl mFakeControl;
    private InsetsControlTarget mFakeControlTarget;
    private TriConsumer<DisplayFrames, WindowContainer, Rect> mFrameProvider;
    private TriConsumer<DisplayFrames, WindowContainer, Rect> mImeFrameProvider;
    private boolean mIsLeashReadyForDispatching;
    private InsetsControlTarget mPendingControlTarget;
    private boolean mSeamlessRotating;
    private boolean mServerVisible;
    protected final InsetsSource mSource;
    private final InsetsStateController mStateController;
    protected WindowContainer mWindowContainer;
    private final Rect mTmpRect = new Rect();
    private final Rect mImeOverrideFrame = new Rect();
    private final Rect mSourceFrame = new Rect();
    private final Rect mLastSourceFrame = new Rect();
    private Insets mInsetsHint = Insets.NONE;
    private final Consumer<SurfaceControl.Transaction> mSetLeashPositionConsumer = new Consumer() { // from class: com.android.server.wm.InsetsSourceProvider$$ExternalSyntheticLambda0
        @Override // java.util.function.Consumer
        public final void accept(Object obj) {
            InsetsSourceProvider.this.m8064lambda$new$0$comandroidserverwmInsetsSourceProvider((SurfaceControl.Transaction) obj);
        }
    };
    private boolean mCropToProvidingInsets = false;

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$new$0$com-android-server-wm-InsetsSourceProvider  reason: not valid java name */
    public /* synthetic */ void m8064lambda$new$0$comandroidserverwmInsetsSourceProvider(SurfaceControl.Transaction t) {
        SurfaceControl leash;
        InsetsSourceControl insetsSourceControl = this.mControl;
        if (insetsSourceControl != null && (leash = insetsSourceControl.getLeash()) != null) {
            Point position = this.mControl.getSurfacePosition();
            t.setPosition(leash, position.x, position.y);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public InsetsSourceProvider(InsetsSource source, InsetsStateController stateController, DisplayContent displayContent) {
        this.mClientVisible = InsetsState.getDefaultVisibility(source.getType());
        this.mSource = source;
        this.mDisplayContent = displayContent;
        this.mStateController = stateController;
        this.mFakeControl = new InsetsSourceControl(source.getType(), (SurfaceControl) null, new Point(), InsetsSourceControl.INVALID_HINTS);
        this.mControllable = InsetsPolicy.isInsetsTypeControllable(source.getType());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public InsetsSource getSource() {
        return this.mSource;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isControllable() {
        return this.mControllable;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setWindowContainer(WindowContainer windowContainer, TriConsumer<DisplayFrames, WindowContainer, Rect> frameProvider, TriConsumer<DisplayFrames, WindowContainer, Rect> imeFrameProvider) {
        WindowContainer windowContainer2 = this.mWindowContainer;
        if (windowContainer2 != null) {
            if (this.mControllable) {
                windowContainer2.setControllableInsetProvider(null);
            }
            this.mWindowContainer.cancelAnimation();
            this.mWindowContainer.getProvidedInsetsSources().remove(this.mSource.getType());
            this.mSeamlessRotating = false;
        }
        if (ProtoLogCache.WM_DEBUG_WINDOW_INSETS_enabled) {
            String protoLogParam0 = String.valueOf(windowContainer);
            String protoLogParam1 = String.valueOf(InsetsState.typeToString(this.mSource.getType()));
            ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_WINDOW_INSETS, -1483435730, 0, (String) null, new Object[]{protoLogParam0, protoLogParam1});
        }
        this.mWindowContainer = windowContainer;
        this.mFrameProvider = frameProvider;
        this.mImeFrameProvider = imeFrameProvider;
        if (windowContainer == null) {
            setServerVisible(false);
            this.mSource.setVisibleFrame((Rect) null);
            this.mSource.setInsetsRoundedCornerFrame(false);
            this.mSourceFrame.setEmpty();
            return;
        }
        windowContainer.getProvidedInsetsSources().put(this.mSource.getType(), this.mSource);
        if (this.mControllable) {
            this.mWindowContainer.setControllableInsetProvider(this);
            InsetsControlTarget insetsControlTarget = this.mPendingControlTarget;
            if (insetsControlTarget != null) {
                updateControlForTarget(insetsControlTarget, true);
                this.mPendingControlTarget = null;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasWindowContainer() {
        return this.mWindowContainer != null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateSourceFrame(Rect frame) {
        WindowContainer windowContainer = this.mWindowContainer;
        if (windowContainer == null) {
            return;
        }
        WindowState win = windowContainer.asWindowState();
        if (win == null) {
            if (this.mServerVisible) {
                this.mTmpRect.set(this.mWindowContainer.getBounds());
                TriConsumer<DisplayFrames, WindowContainer, Rect> triConsumer = this.mFrameProvider;
                if (triConsumer != null) {
                    triConsumer.accept(this.mWindowContainer.getDisplayContent().mDisplayFrames, this.mWindowContainer, this.mTmpRect);
                }
            } else {
                this.mTmpRect.setEmpty();
            }
            this.mSource.setFrame(this.mTmpRect);
            this.mSource.setVisibleFrame((Rect) null);
            return;
        }
        this.mSourceFrame.set(frame);
        TriConsumer<DisplayFrames, WindowContainer, Rect> triConsumer2 = this.mFrameProvider;
        if (triConsumer2 != null) {
            triConsumer2.accept(this.mWindowContainer.getDisplayContent().mDisplayFrames, this.mWindowContainer, this.mSourceFrame);
        } else {
            this.mSourceFrame.inset(win.mGivenContentInsets);
        }
        updateSourceFrameForServerVisibility();
        if (this.mImeFrameProvider != null) {
            this.mImeOverrideFrame.set(frame);
            this.mImeFrameProvider.accept(this.mWindowContainer.getDisplayContent().mDisplayFrames, this.mWindowContainer, this.mImeOverrideFrame);
        }
        if (win.mGivenVisibleInsets.left == 0 && win.mGivenVisibleInsets.top == 0 && win.mGivenVisibleInsets.right == 0 && win.mGivenVisibleInsets.bottom == 0) {
            this.mSource.setVisibleFrame((Rect) null);
            return;
        }
        this.mTmpRect.set(frame);
        this.mTmpRect.inset(win.mGivenVisibleInsets);
        this.mSource.setVisibleFrame(this.mTmpRect);
    }

    private void updateSourceFrameForServerVisibility() {
        if (this.mServerVisible) {
            this.mSource.setFrame(this.mSourceFrame);
        } else {
            this.mSource.setFrame(0, 0, 0, 0);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public InsetsSource createSimulatedSource(DisplayFrames displayFrames, Rect frame) {
        InsetsSource source = new InsetsSource(this.mSource.getType());
        source.setVisible(this.mSource.isVisible());
        this.mTmpRect.set(frame);
        TriConsumer<DisplayFrames, WindowContainer, Rect> triConsumer = this.mFrameProvider;
        if (triConsumer != null) {
            triConsumer.accept(displayFrames, this.mWindowContainer, this.mTmpRect);
        }
        source.setFrame(this.mTmpRect);
        return source;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onPostLayout() {
        boolean isServerVisible;
        WindowContainer windowContainer = this.mWindowContainer;
        if (windowContainer == null) {
            return;
        }
        WindowState windowState = windowContainer.asWindowState();
        if (windowState != null) {
            isServerVisible = windowState.wouldBeVisibleIfPolicyIgnored() && windowState.isVisibleByPolicy();
        } else {
            isServerVisible = this.mWindowContainer.isVisibleRequested();
        }
        setServerVisible(isServerVisible);
        if (this.mControl != null) {
            boolean changed = false;
            Point position = getWindowFrameSurfacePosition();
            if (this.mControl.setSurfacePosition(position.x, position.y) && this.mControlTarget != null) {
                changed = true;
                if (windowState != null && windowState.getWindowFrames().didFrameSizeChange() && windowState.mWinAnimator.getShown() && this.mWindowContainer.okToDisplay()) {
                    windowState.applyWithNextDraw(this.mSetLeashPositionConsumer);
                } else {
                    this.mSetLeashPositionConsumer.accept(this.mWindowContainer.getSyncTransaction());
                }
            }
            if (this.mServerVisible && !this.mLastSourceFrame.equals(this.mSource.getFrame())) {
                Insets insetsHint = this.mSource.calculateInsets(this.mWindowContainer.getBounds(), true);
                if (!insetsHint.equals(this.mControl.getInsetsHint())) {
                    changed = true;
                    this.mControl.setInsetsHint(insetsHint);
                    this.mInsetsHint = insetsHint;
                }
                this.mLastSourceFrame.set(this.mSource.getFrame());
            }
            if (changed) {
                this.mStateController.notifyControlChanged(this.mControlTarget);
            }
        }
    }

    private Point getWindowFrameSurfacePosition() {
        AsyncRotationController controller;
        WindowState win = this.mWindowContainer.asWindowState();
        if (this.mControl != null && (controller = win.mDisplayContent.getAsyncRotationController()) != null && controller.shouldFreezeInsetsPosition(win)) {
            return this.mControl.getSurfacePosition();
        }
        Rect frame = this.mWindowContainer.asWindowState() != null ? this.mWindowContainer.asWindowState().getFrame() : this.mWindowContainer.getBounds();
        Point position = new Point();
        this.mWindowContainer.transformFrameToSurfacePosition(frame.left, frame.top, position);
        return position;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateControlForFakeTarget(InsetsControlTarget fakeTarget) {
        if (fakeTarget == this.mFakeControlTarget) {
            return;
        }
        this.mFakeControlTarget = fakeTarget;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setCropToProvidingInsetsBounds(SurfaceControl.Transaction t) {
        this.mCropToProvidingInsets = true;
        WindowContainer windowContainer = this.mWindowContainer;
        if (windowContainer != null && windowContainer.mSurfaceAnimator.hasLeash()) {
            t.setWindowCrop(this.mWindowContainer.mSurfaceAnimator.mLeash, getProvidingInsetsBoundsCropRect());
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void removeCropToProvidingInsetsBounds(SurfaceControl.Transaction t) {
        this.mCropToProvidingInsets = false;
        WindowContainer windowContainer = this.mWindowContainer;
        if (windowContainer != null && windowContainer.mSurfaceAnimator.hasLeash()) {
            t.setWindowCrop(this.mWindowContainer.mSurfaceAnimator.mLeash, null);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public Rect getProvidingInsetsBoundsCropRect() {
        Rect sourceWindowFrame;
        if (this.mWindowContainer.asWindowState() != null) {
            sourceWindowFrame = this.mWindowContainer.asWindowState().getFrame();
        } else {
            sourceWindowFrame = this.mWindowContainer.getBounds();
        }
        Rect insetFrame = getSource().getFrame();
        return new Rect(insetFrame.left - sourceWindowFrame.left, insetFrame.top - sourceWindowFrame.top, insetFrame.right - sourceWindowFrame.left, insetFrame.bottom - sourceWindowFrame.top);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateControlForTarget(InsetsControlTarget target, boolean force) {
        if (this.mSeamlessRotating) {
            return;
        }
        WindowContainer windowContainer = this.mWindowContainer;
        if (windowContainer != null && windowContainer.getSurfaceControl() == null) {
            setWindowContainer(null, null, null);
        }
        WindowContainer windowContainer2 = this.mWindowContainer;
        if (windowContainer2 == null) {
            this.mPendingControlTarget = target;
            return;
        }
        boolean clientVisible = this.mClientVisible;
        if (target == this.mControlTarget && !force && 0 == 0) {
            return;
        }
        if (target == null) {
            windowContainer2.cancelAnimation();
            setClientVisible(InsetsState.getDefaultVisibility(this.mSource.getType()));
            return;
        }
        Point surfacePosition = getWindowFrameSurfacePosition();
        this.mAdapter = new ControlAdapter(surfacePosition);
        if (getSource().getType() == 19) {
            setClientVisible(target.getRequestedVisibility(this.mSource.getType()));
        }
        SurfaceControl.Transaction t = this.mDisplayContent.getSyncTransaction();
        if (ThunderbackConfig.isVersion4() && this.mSource.getType() == 0) {
            this.mWindowContainer.startAnimation(t, this.mAdapter, !clientVisible, 32);
        } else {
            this.mWindowContainer.startAnimation(t, this.mAdapter, !this.mClientVisible, 32);
        }
        this.mIsLeashReadyForDispatching = false;
        SurfaceControl leash = this.mAdapter.mCapturedLeash;
        this.mControlTarget = target;
        updateVisibility();
        this.mControl = new InsetsSourceControl(this.mSource.getType(), leash, surfacePosition, this.mInsetsHint);
        if (ProtoLogCache.WM_DEBUG_WINDOW_INSETS_enabled) {
            String protoLogParam0 = String.valueOf(this.mControl);
            String protoLogParam1 = String.valueOf(this.mControlTarget);
            ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_WINDOW_INSETS, 416924848, 0, (String) null, new Object[]{protoLogParam0, protoLogParam1});
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void startSeamlessRotation() {
        if (!this.mSeamlessRotating) {
            this.mSeamlessRotating = true;
            this.mWindowContainer.cancelAnimation();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void finishSeamlessRotation() {
        this.mSeamlessRotating = false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean updateClientVisibility(InsetsControlTarget caller) {
        boolean requestedVisible = caller.getRequestedVisibility(this.mSource.getType());
        if (caller != this.mControlTarget || requestedVisible == this.mClientVisible) {
            return false;
        }
        setClientVisible(requestedVisible);
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onSurfaceTransactionApplied() {
        this.mIsLeashReadyForDispatching = true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setClientVisible(boolean clientVisible) {
        if (this.mClientVisible == clientVisible) {
            return;
        }
        this.mClientVisible = clientVisible;
        if (!this.mDisplayContent.mLayoutAndAssignWindowLayersScheduled) {
            this.mDisplayContent.mLayoutAndAssignWindowLayersScheduled = true;
            this.mDisplayContent.mWmService.mH.obtainMessage(63, this.mDisplayContent).sendToTarget();
        }
        updateVisibility();
    }

    void setServerVisible(boolean serverVisible) {
        this.mServerVisible = serverVisible;
        updateSourceFrameForServerVisibility();
        updateVisibility();
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void updateVisibility() {
        this.mSource.setVisible(this.mServerVisible && (isMirroredSource() || this.mClientVisible));
        if (ProtoLogCache.WM_DEBUG_WINDOW_INSETS_enabled) {
            String protoLogParam0 = String.valueOf(InsetsState.typeToString(this.mSource.getType()));
            String protoLogParam1 = String.valueOf(this.mServerVisible);
            String protoLogParam2 = String.valueOf(this.mClientVisible);
            ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_WINDOW_INSETS, 2070726247, 0, (String) null, new Object[]{protoLogParam0, protoLogParam1, protoLogParam2});
        }
    }

    private boolean isMirroredSource() {
        int[] provides;
        WindowContainer windowContainer = this.mWindowContainer;
        if (windowContainer == null || windowContainer.asWindowState() == null || (provides = ((WindowState) this.mWindowContainer).mAttrs.providesInsetsTypes) == null) {
            return false;
        }
        for (int i : provides) {
            if (i == 19) {
                return true;
            }
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public InsetsSourceControl getControl(InsetsControlTarget target) {
        if (target == this.mControlTarget) {
            if (!this.mIsLeashReadyForDispatching && this.mControl != null) {
                return new InsetsSourceControl(this.mControl.getType(), (SurfaceControl) null, this.mControl.getSurfacePosition(), this.mControl.getInsetsHint());
            }
            return this.mControl;
        } else if (target == this.mFakeControlTarget) {
            return this.mFakeControl;
        } else {
            return null;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public InsetsControlTarget getControlTarget() {
        return this.mControlTarget;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isClientVisible() {
        return this.mClientVisible;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean overridesImeFrame() {
        return this.mImeFrameProvider != null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Rect getImeOverrideFrame() {
        return this.mImeOverrideFrame;
    }

    public void dump(PrintWriter pw, String prefix) {
        pw.println(prefix + getClass().getSimpleName());
        String prefix2 = prefix + "  ";
        pw.print(prefix2 + "mSource=");
        this.mSource.dump("", pw);
        if (this.mControl != null) {
            pw.print(prefix2 + "mControl=");
            this.mControl.dump("", pw);
        }
        pw.print(prefix2);
        pw.print("mIsLeashReadyForDispatching=");
        pw.print(this.mIsLeashReadyForDispatching);
        pw.print(" mImeOverrideFrame=");
        pw.print(this.mImeOverrideFrame.toShortString());
        pw.println();
        if (this.mWindowContainer != null) {
            pw.print(prefix2 + "mWindowContainer=");
            pw.println(this.mWindowContainer);
        }
        if (this.mAdapter != null) {
            pw.print(prefix2 + "mAdapter=");
            this.mAdapter.dump(pw, "");
        }
        if (this.mControlTarget != null) {
            pw.print(prefix2 + "mControlTarget=");
            pw.println(this.mControlTarget.getWindow());
        }
        if (this.mPendingControlTarget != null) {
            pw.print(prefix2 + "mPendingControlTarget=");
            pw.println(this.mPendingControlTarget.getWindow());
        }
        if (this.mFakeControlTarget != null) {
            pw.print(prefix2 + "mFakeControlTarget=");
            pw.println(this.mFakeControlTarget.getWindow());
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dumpDebug(ProtoOutputStream proto, long fieldId, int logLevel) {
        long token = proto.start(fieldId);
        this.mSource.dumpDebug(proto, 1146756268033L);
        this.mTmpRect.dumpDebug(proto, 1146756268034L);
        this.mFakeControl.dumpDebug(proto, 1146756268035L);
        InsetsSourceControl insetsSourceControl = this.mControl;
        if (insetsSourceControl != null) {
            insetsSourceControl.dumpDebug(proto, 1146756268036L);
        }
        InsetsControlTarget insetsControlTarget = this.mControlTarget;
        if (insetsControlTarget != null && insetsControlTarget.getWindow() != null) {
            this.mControlTarget.getWindow().dumpDebug(proto, 1146756268037L, logLevel);
        }
        InsetsControlTarget insetsControlTarget2 = this.mPendingControlTarget;
        if (insetsControlTarget2 != null && insetsControlTarget2.getWindow() != null) {
            this.mPendingControlTarget.getWindow().dumpDebug(proto, 1146756268038L, logLevel);
        }
        InsetsControlTarget insetsControlTarget3 = this.mFakeControlTarget;
        if (insetsControlTarget3 != null && insetsControlTarget3.getWindow() != null) {
            this.mFakeControlTarget.getWindow().dumpDebug(proto, 1146756268039L, logLevel);
        }
        ControlAdapter controlAdapter = this.mAdapter;
        if (controlAdapter != null && controlAdapter.mCapturedLeash != null) {
            this.mAdapter.mCapturedLeash.dumpDebug(proto, 1146756268040L);
        }
        this.mImeOverrideFrame.dumpDebug(proto, 1146756268041L);
        proto.write(1133871366154L, this.mIsLeashReadyForDispatching);
        proto.write(1133871366155L, this.mClientVisible);
        proto.write(1133871366156L, this.mServerVisible);
        proto.write(1133871366157L, this.mSeamlessRotating);
        proto.write(1133871366159L, this.mControllable);
        proto.end(token);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class ControlAdapter implements AnimationAdapter {
        private SurfaceControl mCapturedLeash;
        private final Point mSurfacePosition;

        ControlAdapter(Point surfacePosition) {
            this.mSurfacePosition = surfacePosition;
        }

        @Override // com.android.server.wm.AnimationAdapter
        public boolean getShowWallpaper() {
            return false;
        }

        @Override // com.android.server.wm.AnimationAdapter
        public void startAnimation(SurfaceControl animationLeash, SurfaceControl.Transaction t, int type, SurfaceAnimator.OnAnimationFinishedCallback finishCallback) {
            if (InsetsSourceProvider.this.mSource.getType() == 19) {
                t.setAlpha(animationLeash, 1.0f);
                t.hide(animationLeash);
            }
            if (ProtoLogCache.WM_DEBUG_WINDOW_INSETS_enabled) {
                String protoLogParam0 = String.valueOf(InsetsSourceProvider.this.mSource);
                String protoLogParam1 = String.valueOf(InsetsSourceProvider.this.mControlTarget);
                ProtoLogImpl.i(ProtoLogGroup.WM_DEBUG_WINDOW_INSETS, -1185473319, 0, (String) null, new Object[]{protoLogParam0, protoLogParam1});
            }
            this.mCapturedLeash = animationLeash;
            t.setPosition(animationLeash, this.mSurfacePosition.x, this.mSurfacePosition.y);
            if (InsetsSourceProvider.this.mCropToProvidingInsets) {
                t.setWindowCrop(this.mCapturedLeash, InsetsSourceProvider.this.getProvidingInsetsBoundsCropRect());
            }
        }

        @Override // com.android.server.wm.AnimationAdapter
        public void onAnimationCancelled(SurfaceControl animationLeash) {
            if (InsetsSourceProvider.this.mAdapter == this) {
                InsetsSourceProvider.this.mStateController.notifyControlRevoked(InsetsSourceProvider.this.mControlTarget, InsetsSourceProvider.this);
                InsetsSourceProvider.this.mControl = null;
                InsetsSourceProvider.this.mControlTarget = null;
                InsetsSourceProvider.this.mAdapter = null;
                InsetsSourceProvider insetsSourceProvider = InsetsSourceProvider.this;
                insetsSourceProvider.setClientVisible(InsetsState.getDefaultVisibility(insetsSourceProvider.mSource.getType()));
                if (ProtoLogCache.WM_DEBUG_WINDOW_INSETS_enabled) {
                    String protoLogParam0 = String.valueOf(InsetsSourceProvider.this.mSource);
                    String protoLogParam1 = String.valueOf(InsetsSourceProvider.this.mControlTarget);
                    ProtoLogImpl.i(ProtoLogGroup.WM_DEBUG_WINDOW_INSETS, -1394745488, 0, (String) null, new Object[]{protoLogParam0, protoLogParam1});
                }
            }
        }

        @Override // com.android.server.wm.AnimationAdapter
        public long getDurationHint() {
            return 0L;
        }

        @Override // com.android.server.wm.AnimationAdapter
        public long getStatusBarTransitionsStartTime() {
            return 0L;
        }

        @Override // com.android.server.wm.AnimationAdapter
        public void dump(PrintWriter pw, String prefix) {
            pw.print(prefix + "ControlAdapter mCapturedLeash=");
            pw.print(this.mCapturedLeash);
            pw.println();
        }

        @Override // com.android.server.wm.AnimationAdapter
        public void dumpDebug(ProtoOutputStream proto) {
        }
    }
}
