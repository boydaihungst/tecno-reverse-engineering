package com.android.server.wm;

import android.os.Trace;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.SparseArray;
import android.view.InsetsSourceControl;
import android.view.InsetsState;
import android.view.WindowInsets;
import com.android.internal.protolog.ProtoLogGroup;
import com.android.internal.protolog.ProtoLogImpl;
import com.android.server.inputmethod.InputMethodManagerInternal;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.function.Function;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class InsetsStateController {
    private final DisplayContent mDisplayContent;
    private final InsetsState mLastState = new InsetsState();
    private final InsetsState mState = new InsetsState();
    private final ArrayMap<Integer, WindowContainerInsetsSourceProvider> mProviders = new ArrayMap<>();
    private final ArrayMap<InsetsControlTarget, ArrayList<Integer>> mControlTargetTypeMap = new ArrayMap<>();
    private final SparseArray<InsetsControlTarget> mTypeControlTargetMap = new SparseArray<>();
    private final SparseArray<InsetsControlTarget> mTypeFakeControlTargetMap = new SparseArray<>();
    private final ArraySet<InsetsControlTarget> mPendingControlChanged = new ArraySet<>();
    private final Consumer<WindowState> mDispatchInsetsChanged = new Consumer() { // from class: com.android.server.wm.InsetsStateController$$ExternalSyntheticLambda5
        @Override // java.util.function.Consumer
        public final void accept(Object obj) {
            InsetsStateController.lambda$new$0((WindowState) obj);
        }
    };
    private final InsetsControlTarget mEmptyImeControlTarget = new AnonymousClass1();

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$new$0(WindowState w) {
        if (w.isReadyToDispatchInsetsState()) {
            w.notifyInsetsChanged();
        }
    }

    /* renamed from: com.android.server.wm.InsetsStateController$1  reason: invalid class name */
    /* loaded from: classes2.dex */
    class AnonymousClass1 implements InsetsControlTarget {
        AnonymousClass1() {
        }

        @Override // com.android.server.wm.InsetsControlTarget
        public void notifyInsetsControlChanged() {
            InsetsSourceControl[] controls = InsetsStateController.this.getControlsForDispatch(this);
            if (controls == null) {
                return;
            }
            for (InsetsSourceControl control : controls) {
                if (control.getType() == 19) {
                    InsetsStateController.this.mDisplayContent.mWmService.mH.post(new Runnable() { // from class: com.android.server.wm.InsetsStateController$1$$ExternalSyntheticLambda0
                        @Override // java.lang.Runnable
                        public final void run() {
                            InputMethodManagerInternal.get().removeImeSurface();
                        }
                    });
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public InsetsStateController(DisplayContent displayContent) {
        this.mDisplayContent = displayContent;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public InsetsState getRawInsetsState() {
        return this.mState;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public InsetsSourceControl[] getControlsForDispatch(InsetsControlTarget target) {
        ArrayList<Integer> controlled = this.mControlTargetTypeMap.get(target);
        if (controlled == null) {
            return null;
        }
        int size = controlled.size();
        InsetsSourceControl[] result = new InsetsSourceControl[size];
        for (int i = 0; i < size; i++) {
            result[i] = this.mProviders.get(controlled.get(i)).getControl(target);
        }
        return result;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ArrayMap<Integer, WindowContainerInsetsSourceProvider> getSourceProviders() {
        return this.mProviders;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public WindowContainerInsetsSourceProvider getSourceProvider(int type) {
        if (type == 19) {
            return this.mProviders.computeIfAbsent(Integer.valueOf(type), new Function() { // from class: com.android.server.wm.InsetsStateController$$ExternalSyntheticLambda0
                @Override // java.util.function.Function
                public final Object apply(Object obj) {
                    return InsetsStateController.this.m8067x258def2e((Integer) obj);
                }
            });
        }
        return this.mProviders.computeIfAbsent(Integer.valueOf(type), new Function() { // from class: com.android.server.wm.InsetsStateController$$ExternalSyntheticLambda1
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                return InsetsStateController.this.m8068xc02eb1af((Integer) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getSourceProvider$1$com-android-server-wm-InsetsStateController  reason: not valid java name */
    public /* synthetic */ WindowContainerInsetsSourceProvider m8067x258def2e(Integer key) {
        return new ImeInsetsSourceProvider(this.mState.getSource(key.intValue()), this, this.mDisplayContent);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getSourceProvider$2$com-android-server-wm-InsetsStateController  reason: not valid java name */
    public /* synthetic */ WindowContainerInsetsSourceProvider m8068xc02eb1af(Integer key) {
        return new WindowContainerInsetsSourceProvider(this.mState.getSource(key.intValue()), this, this.mDisplayContent);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ImeInsetsSourceProvider getImeSourceProvider() {
        return (ImeInsetsSourceProvider) getSourceProvider(19);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public WindowContainerInsetsSourceProvider peekSourceProvider(int type) {
        return this.mProviders.get(Integer.valueOf(type));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onPostLayout() {
        Trace.traceBegin(32L, "ISC.onPostLayout");
        for (int i = this.mProviders.size() - 1; i >= 0; i--) {
            this.mProviders.valueAt(i).onPostLayout();
        }
        if (!this.mLastState.equals(this.mState)) {
            this.mLastState.set(this.mState, true);
            notifyInsetsChanged();
        }
        Trace.traceEnd(32L);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateAboveInsetsState(boolean notifyInsetsChange) {
        InsetsState aboveInsetsState = new InsetsState();
        aboveInsetsState.set(this.mState, WindowInsets.Type.displayCutout() | WindowInsets.Type.systemGestures() | WindowInsets.Type.mandatorySystemGestures());
        ArraySet<WindowState> insetsChangedWindows = new ArraySet<>();
        SparseArray<InsetsSourceProvider> localInsetsSourceProvidersFromParent = new SparseArray<>();
        this.mDisplayContent.updateAboveInsetsState(aboveInsetsState, localInsetsSourceProvidersFromParent, insetsChangedWindows);
        if (notifyInsetsChange) {
            for (int i = insetsChangedWindows.size() - 1; i >= 0; i--) {
                this.mDispatchInsetsChanged.accept(insetsChangedWindows.valueAt(i));
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onDisplayFramesUpdated(boolean notifyInsetsChange) {
        final ArrayList<WindowState> insetsChangedWindows = new ArrayList<>();
        this.mDisplayContent.forAllWindows(new Consumer() { // from class: com.android.server.wm.InsetsStateController$$ExternalSyntheticLambda2
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                InsetsStateController.this.m8070x2a7077ba(insetsChangedWindows, (WindowState) obj);
            }
        }, true);
        if (notifyInsetsChange) {
            for (int i = insetsChangedWindows.size() - 1; i >= 0; i--) {
                this.mDispatchInsetsChanged.accept(insetsChangedWindows.get(i));
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onDisplayFramesUpdated$3$com-android-server-wm-InsetsStateController  reason: not valid java name */
    public /* synthetic */ void m8070x2a7077ba(ArrayList insetsChangedWindows, WindowState w) {
        w.mAboveInsetsState.set(this.mState, WindowInsets.Type.displayCutout());
        insetsChangedWindows.add(w);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onInsetsModified(InsetsControlTarget caller) {
        boolean changed = false;
        for (int i = this.mProviders.size() - 1; i >= 0; i--) {
            changed |= this.mProviders.valueAt(i).updateClientVisibility(caller);
        }
        if (changed) {
            notifyInsetsChanged();
            this.mDisplayContent.updateSystemGestureExclusion();
            this.mDisplayContent.updateKeepClearAreas();
            this.mDisplayContent.getDisplayPolicy().updateSystemBarAttributes();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isFakeTarget(int type, InsetsControlTarget target) {
        return this.mTypeFakeControlTargetMap.get(type) == target;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onImeControlTargetChanged(InsetsControlTarget imeTarget) {
        InsetsControlTarget target = imeTarget != null ? imeTarget : this.mEmptyImeControlTarget;
        onControlChanged(19, target);
        if (ProtoLogCache.WM_DEBUG_IME_enabled) {
            String protoLogParam0 = String.valueOf(target != null ? target.getWindow() : "null");
            ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_IME, 1658605381, 0, (String) null, new Object[]{protoLogParam0});
        }
        notifyPendingInsetsControlChanged();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onBarControlTargetChanged(InsetsControlTarget statusControlling, InsetsControlTarget fakeStatusControlling, InsetsControlTarget navControlling, InsetsControlTarget fakeNavControlling) {
        onControlChanged(0, statusControlling);
        onControlChanged(1, navControlling);
        onControlChanged(20, statusControlling);
        onControlChanged(21, navControlling);
        onControlFakeTargetChanged(0, fakeStatusControlling);
        onControlFakeTargetChanged(1, fakeNavControlling);
        onControlFakeTargetChanged(20, fakeStatusControlling);
        onControlFakeTargetChanged(21, fakeNavControlling);
        notifyPendingInsetsControlChanged();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void notifyControlRevoked(InsetsControlTarget previousControlTarget, InsetsSourceProvider provider) {
        removeFromControlMaps(previousControlTarget, provider.getSource().getType(), false);
    }

    private void onControlChanged(int type, InsetsControlTarget target) {
        WindowContainerInsetsSourceProvider provider;
        InsetsControlTarget previous = this.mTypeControlTargetMap.get(type);
        if (target == previous || (provider = this.mProviders.get(Integer.valueOf(type))) == null || !provider.isControllable()) {
            return;
        }
        provider.updateControlForTarget(target, false);
        InsetsControlTarget target2 = provider.getControlTarget();
        if (previous != null) {
            removeFromControlMaps(previous, type, false);
            this.mPendingControlChanged.add(previous);
        }
        if (target2 != null) {
            addToControlMaps(target2, type, false);
            this.mPendingControlChanged.add(target2);
        }
    }

    void onControlFakeTargetChanged(int type, InsetsControlTarget fakeTarget) {
        WindowContainerInsetsSourceProvider provider;
        InsetsControlTarget previous = this.mTypeFakeControlTargetMap.get(type);
        if (fakeTarget == previous || (provider = this.mProviders.get(Integer.valueOf(type))) == null) {
            return;
        }
        provider.updateControlForFakeTarget(fakeTarget);
        if (previous != null) {
            removeFromControlMaps(previous, type, true);
            this.mPendingControlChanged.add(previous);
        }
        if (fakeTarget != null) {
            addToControlMaps(fakeTarget, type, true);
            this.mPendingControlChanged.add(fakeTarget);
        }
    }

    private void removeFromControlMaps(InsetsControlTarget target, int type, boolean fake) {
        ArrayList<Integer> array = this.mControlTargetTypeMap.get(target);
        if (array == null) {
            return;
        }
        array.remove(Integer.valueOf(type));
        if (array.isEmpty()) {
            this.mControlTargetTypeMap.remove(target);
        }
        if (fake) {
            this.mTypeFakeControlTargetMap.remove(type);
        } else {
            this.mTypeControlTargetMap.remove(type);
        }
    }

    private void addToControlMaps(InsetsControlTarget target, int type, boolean fake) {
        ArrayList<Integer> array = this.mControlTargetTypeMap.computeIfAbsent(target, new Function() { // from class: com.android.server.wm.InsetsStateController$$ExternalSyntheticLambda3
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                return InsetsStateController.lambda$addToControlMaps$4((InsetsControlTarget) obj);
            }
        });
        array.add(Integer.valueOf(type));
        if (fake) {
            this.mTypeFakeControlTargetMap.put(type, target);
        } else {
            this.mTypeControlTargetMap.put(type, target);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ ArrayList lambda$addToControlMaps$4(InsetsControlTarget key) {
        return new ArrayList();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void notifyControlChanged(InsetsControlTarget target) {
        this.mPendingControlChanged.add(target);
        notifyPendingInsetsControlChanged();
    }

    private void notifyPendingInsetsControlChanged() {
        if (this.mPendingControlChanged.isEmpty()) {
            return;
        }
        this.mDisplayContent.mWmService.mAnimator.addAfterPrepareSurfacesRunnable(new Runnable() { // from class: com.android.server.wm.InsetsStateController$$ExternalSyntheticLambda4
            @Override // java.lang.Runnable
            public final void run() {
                InsetsStateController.this.m8069x3ed585ab();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$notifyPendingInsetsControlChanged$5$com-android-server-wm-InsetsStateController  reason: not valid java name */
    public /* synthetic */ void m8069x3ed585ab() {
        synchronized (this.mPendingControlChanged) {
            for (int i = this.mProviders.size() - 1; i >= 0; i--) {
                WindowContainerInsetsSourceProvider provider = this.mProviders.valueAt(i);
                provider.onSurfaceTransactionApplied();
            }
            ArraySet<InsetsControlTarget> newControlTargets = new ArraySet<>();
            for (int i2 = this.mPendingControlChanged.size() - 1; i2 >= 0; i2--) {
                InsetsControlTarget controlTarget = this.mPendingControlChanged.valueAt(i2);
                controlTarget.notifyInsetsControlChanged();
                if (this.mControlTargetTypeMap.containsKey(controlTarget)) {
                    newControlTargets.add(controlTarget);
                }
            }
            this.mPendingControlChanged.clear();
            for (int i3 = newControlTargets.size() - 1; i3 >= 0; i3--) {
                onInsetsModified(newControlTargets.valueAt(i3));
            }
            newControlTargets.clear();
        }
    }

    void notifyInsetsChanged() {
        this.mDisplayContent.notifyInsetsChanged(this.mDispatchInsetsChanged);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dump(String prefix, PrintWriter pw) {
        pw.println(prefix + "WindowInsetsStateController");
        String prefix2 = prefix + "  ";
        this.mState.dump(prefix2, pw);
        pw.println(prefix2 + "Control map:");
        for (int i = this.mTypeControlTargetMap.size() - 1; i >= 0; i--) {
            pw.print(prefix2 + "  ");
            pw.println(InsetsState.typeToString(this.mTypeControlTargetMap.keyAt(i)) + " -> " + this.mTypeControlTargetMap.valueAt(i));
        }
        pw.println(prefix2 + "InsetsSourceProviders:");
        for (int i2 = this.mProviders.size() - 1; i2 >= 0; i2--) {
            this.mProviders.valueAt(i2).dump(pw, prefix2 + "  ");
        }
    }
}
