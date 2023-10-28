package android.view;

import android.graphics.Insets;
import android.graphics.Rect;
import android.util.ArraySet;
import android.util.proto.ProtoOutputStream;
import android.view.SurfaceControl;
import android.view.WindowInsets;
import com.android.internal.inputmethod.ImeTracing;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;
import java.util.function.Supplier;
/* loaded from: classes3.dex */
public class InsetsSourceConsumer {
    private static final String TAG = "InsetsSourceConsumer";
    protected final InsetsController mController;
    private boolean mHasViewFocusWhenWindowFocusGain;
    private boolean mHasWindowFocus;
    private boolean mIsAnimationPending;
    private Rect mPendingFrame;
    private Rect mPendingVisibleFrame;
    protected boolean mRequestedVisible;
    private InsetsSourceControl mSourceControl;
    protected final InsetsState mState;
    private final Supplier<SurfaceControl.Transaction> mTransactionSupplier;
    protected final int mType;

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes3.dex */
    @interface ShowResult {
        public static final int IME_SHOW_DELAYED = 1;
        public static final int IME_SHOW_FAILED = 2;
        public static final int SHOW_IMMEDIATELY = 0;
    }

    public InsetsSourceConsumer(int type, InsetsState state, Supplier<SurfaceControl.Transaction> transactionSupplier, InsetsController controller) {
        this.mType = type;
        this.mState = state;
        this.mTransactionSupplier = transactionSupplier;
        this.mController = controller;
        this.mRequestedVisible = InsetsState.getDefaultVisibility(type);
    }

    public boolean setControl(InsetsSourceControl control, int[] showTypes, int[] hideTypes) {
        if (this.mType == 19) {
            ImeTracing.getInstance().triggerClientDump("InsetsSourceConsumer#setControl", this.mController.getHost().getInputMethodManager(), null);
        }
        if (Objects.equals(this.mSourceControl, control)) {
            InsetsSourceControl insetsSourceControl = this.mSourceControl;
            if (insetsSourceControl != null && insetsSourceControl != control) {
                insetsSourceControl.release(new InsetsAnimationThreadControlRunner$$ExternalSyntheticLambda0());
                this.mSourceControl = control;
            }
            return false;
        }
        InsetsSourceControl insetsSourceControl2 = this.mSourceControl;
        SurfaceControl leash = insetsSourceControl2 != null ? insetsSourceControl2.getLeash() : null;
        InsetsSourceControl lastControl = this.mSourceControl;
        this.mSourceControl = control;
        if (control == null) {
            this.mController.notifyControlRevoked(this);
            InsetsSource source = this.mState.getSource(this.mType);
            boolean serverVisibility = this.mController.getLastDispatchedState().getSourceOrDefaultVisibility(this.mType);
            if (source.isVisible() != serverVisibility) {
                source.setVisible(serverVisibility);
                this.mController.notifyVisibilityChanged();
            }
            applyLocalVisibilityOverride();
        } else {
            boolean requestedVisible = isRequestedVisibleAwaitingControl();
            boolean fakeControl = InsetsSourceControl.INVALID_HINTS.equals(control.getInsetsHint());
            boolean needsAnimation = (requestedVisible == this.mState.getSource(this.mType).isVisible() || fakeControl) ? false : true;
            if (control.getLeash() != null && (needsAnimation || this.mIsAnimationPending)) {
                if (requestedVisible) {
                    showTypes[0] = showTypes[0] | InsetsState.toPublicType(getType());
                } else {
                    hideTypes[0] = hideTypes[0] | InsetsState.toPublicType(getType());
                }
                this.mIsAnimationPending = false;
            } else {
                if (needsAnimation) {
                    this.mIsAnimationPending = true;
                }
                if (applyLocalVisibilityOverride()) {
                    this.mController.notifyVisibilityChanged();
                }
                applyRequestedVisibilityToControl();
                if (!requestedVisible && !this.mIsAnimationPending && lastControl == null) {
                    removeSurface();
                }
            }
        }
        if (lastControl != null) {
            lastControl.release(new InsetsAnimationThreadControlRunner$$ExternalSyntheticLambda0());
        }
        return true;
    }

    public InsetsSourceControl getControl() {
        return this.mSourceControl;
    }

    protected boolean isRequestedVisibleAwaitingControl() {
        return isRequestedVisible();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getType() {
        return this.mType;
    }

    public void show(boolean fromIme) {
        setRequestedVisible(true);
    }

    public void hide() {
        setRequestedVisible(false);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void hide(boolean animationFinished, int animationType) {
        hide();
    }

    public void onWindowFocusGained(boolean hasViewFocus) {
        this.mHasWindowFocus = true;
        this.mHasViewFocusWhenWindowFocusGain = hasViewFocus;
    }

    public void onWindowFocusLost() {
        this.mHasWindowFocus = false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasViewFocusWhenWindowFocusGain() {
        return this.mHasViewFocusWhenWindowFocusGain;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean applyLocalVisibilityOverride() {
        InsetsSource source = this.mState.peekSource(this.mType);
        boolean isVisible = source != null ? source.isVisible() : InsetsState.getDefaultVisibility(this.mType);
        boolean hasControl = this.mSourceControl != null;
        if (this.mType == 19) {
            ImeTracing.getInstance().triggerClientDump("InsetsSourceConsumer#applyLocalVisibilityOverride", this.mController.getHost().getInputMethodManager(), null);
        }
        updateCompatSysUiVisibility(hasControl, source, isVisible);
        if (hasControl && isVisible != this.mRequestedVisible) {
            this.mState.getSource(this.mType).setVisible(this.mRequestedVisible);
            return true;
        }
        return false;
    }

    private void updateCompatSysUiVisibility(boolean hasControl, InsetsSource source, boolean visible) {
        boolean compatVisible;
        int publicType = InsetsState.toPublicType(this.mType);
        if (publicType != WindowInsets.Type.statusBars() && publicType != WindowInsets.Type.navigationBars()) {
            return;
        }
        if (hasControl) {
            compatVisible = this.mRequestedVisible;
        } else if (source != null && (this.mType == 1 || !source.getFrame().isEmpty())) {
            compatVisible = visible;
        } else {
            ArraySet<Integer> types = InsetsState.toInternalType(publicType);
            for (int i = types.size() - 1; i >= 0; i--) {
                InsetsSource s = this.mState.peekSource(types.valueAt(i).intValue());
                if (s != null && !s.getFrame().isEmpty()) {
                    return;
                }
            }
            compatVisible = this.mRequestedVisible;
        }
        this.mController.updateCompatSysUiVisibility(this.mType, compatVisible, hasControl);
    }

    public boolean isRequestedVisible() {
        return this.mRequestedVisible;
    }

    public int requestShow(boolean fromController) {
        return 0;
    }

    public void onPerceptible(boolean perceptible) {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void notifyHidden() {
    }

    public void removeSurface() {
    }

    public void updateSource(InsetsSource newSource, int animationType) {
        InsetsSource source = this.mState.peekSource(this.mType);
        Rect rect = null;
        if (source == null || animationType == -1 || source.getFrame().equals(newSource.getFrame())) {
            this.mPendingFrame = null;
            this.mPendingVisibleFrame = null;
            this.mState.addSource(newSource);
            return;
        }
        InsetsSource newSource2 = new InsetsSource(newSource);
        this.mPendingFrame = new Rect(newSource2.getFrame());
        if (newSource2.getVisibleFrame() != null) {
            rect = new Rect(newSource2.getVisibleFrame());
        }
        this.mPendingVisibleFrame = rect;
        newSource2.setFrame(source.getFrame());
        newSource2.setVisibleFrame(source.getVisibleFrame());
        this.mState.addSource(newSource2);
    }

    public boolean notifyAnimationFinished() {
        if (this.mPendingFrame != null) {
            InsetsSource source = this.mState.getSource(this.mType);
            source.setFrame(this.mPendingFrame);
            source.setVisibleFrame(this.mPendingVisibleFrame);
            this.mPendingFrame = null;
            this.mPendingVisibleFrame = null;
            return true;
        }
        return false;
    }

    protected void setRequestedVisible(boolean requestedVisible) {
        InsetsSourceControl insetsSourceControl;
        if (this.mRequestedVisible != requestedVisible) {
            this.mRequestedVisible = requestedVisible;
            this.mIsAnimationPending = (this.mIsAnimationPending || (insetsSourceControl = this.mSourceControl) == null || insetsSourceControl.getLeash() != null || Insets.NONE.equals(this.mSourceControl.getInsetsHint())) ? false : true;
            this.mController.onRequestedVisibilityChanged(this);
        }
        if (applyLocalVisibilityOverride()) {
            this.mController.notifyVisibilityChanged();
        }
    }

    private void applyRequestedVisibilityToControl() {
        InsetsSourceControl insetsSourceControl = this.mSourceControl;
        if (insetsSourceControl == null || insetsSourceControl.getLeash() == null || !this.mSourceControl.getLeash().isValid()) {
            return;
        }
        SurfaceControl.Transaction t = this.mTransactionSupplier.get();
        try {
            if (this.mRequestedVisible) {
                t.show(this.mSourceControl.getLeash());
            } else {
                t.hide(this.mSourceControl.getLeash());
            }
            t.setAlpha(this.mSourceControl.getLeash(), this.mRequestedVisible ? 1.0f : 0.0f);
            t.apply();
            if (t != null) {
                t.close();
            }
            onPerceptible(this.mRequestedVisible);
        } catch (Throwable th) {
            if (t != null) {
                try {
                    t.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
            }
            throw th;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dumpDebug(ProtoOutputStream proto, long fieldId) {
        long token = proto.start(fieldId);
        proto.write(1138166333441L, InsetsState.typeToString(this.mType));
        proto.write(1133871366146L, this.mHasWindowFocus);
        proto.write(1133871366147L, this.mRequestedVisible);
        InsetsSourceControl insetsSourceControl = this.mSourceControl;
        if (insetsSourceControl != null) {
            insetsSourceControl.dumpDebug(proto, 1146756268036L);
        }
        Rect rect = this.mPendingFrame;
        if (rect != null) {
            rect.dumpDebug(proto, 1146756268037L);
        }
        Rect rect2 = this.mPendingVisibleFrame;
        if (rect2 != null) {
            rect2.dumpDebug(proto, 1146756268038L);
        }
        proto.end(token);
    }
}
