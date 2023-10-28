package com.android.server.wm;

import android.content.res.Configuration;
import android.graphics.Rect;
import android.util.SparseArray;
import android.util.proto.ProtoOutputStream;
import android.view.SurfaceControl;
import android.window.DisplayAreaAppearedInfo;
import android.window.DisplayAreaInfo;
import android.window.IDisplayAreaOrganizer;
import com.android.internal.protolog.ProtoLogGroup;
import com.android.internal.protolog.ProtoLogImpl;
import com.android.internal.util.Preconditions;
import com.android.server.policy.WindowManagerPolicy;
import com.android.server.wm.DisplayArea;
import com.android.server.wm.WindowContainer;
import com.transsion.hubcore.server.wm.ITranDisplayArea;
import java.io.PrintWriter;
import java.util.Comparator;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;
/* loaded from: classes2.dex */
public class DisplayArea<T extends WindowContainer> extends WindowContainer<T> {
    private static final String TAG_MULTIWIN = "TranMultiwin";
    public DisplayAreaAppearedInfo mDisplayAreaAppearedInfo;
    final int mFeatureId;
    private boolean mFillsParent;
    private int mMultiDisplayAreaId;
    private boolean mMultiWindow;
    private final String mName;
    private boolean mNeedsZNoost;
    IDisplayAreaOrganizer mOrganizer;
    private final DisplayAreaOrganizerController mOrganizerController;
    protected boolean mSetIgnoreOrientationRequest;
    private final Configuration mTmpConfiguration;
    protected final Type mType;

    @Override // com.android.server.wm.WindowContainer, com.android.server.wm.SurfaceAnimator.Animatable
    public /* bridge */ /* synthetic */ void commitPendingTransaction() {
        super.commitPendingTransaction();
    }

    @Override // com.android.server.wm.WindowContainer
    public /* bridge */ /* synthetic */ int compareTo(WindowContainer windowContainer) {
        return super.compareTo(windowContainer);
    }

    @Override // com.android.server.wm.WindowContainer, com.android.server.wm.SurfaceAnimator.Animatable
    public /* bridge */ /* synthetic */ SurfaceControl getAnimationLeash() {
        return super.getAnimationLeash();
    }

    @Override // com.android.server.wm.WindowContainer, com.android.server.wm.SurfaceAnimator.Animatable
    public /* bridge */ /* synthetic */ SurfaceControl getAnimationLeashParent() {
        return super.getAnimationLeashParent();
    }

    @Override // com.android.server.wm.WindowContainer
    public /* bridge */ /* synthetic */ DisplayContent getDisplayContent() {
        return super.getDisplayContent();
    }

    @Override // com.android.server.wm.WindowContainer, com.android.server.wm.SurfaceFreezer.Freezable
    public /* bridge */ /* synthetic */ SurfaceControl getFreezeSnapshotTarget() {
        return super.getFreezeSnapshotTarget();
    }

    @Override // com.android.server.wm.WindowContainer, com.android.server.wm.SurfaceAnimator.Animatable
    public /* bridge */ /* synthetic */ SurfaceControl getParentSurfaceControl() {
        return super.getParentSurfaceControl();
    }

    @Override // com.android.server.wm.WindowContainer, com.android.server.wm.SurfaceAnimator.Animatable
    public /* bridge */ /* synthetic */ SurfaceControl.Transaction getPendingTransaction() {
        return super.getPendingTransaction();
    }

    @Override // com.android.server.wm.WindowContainer
    public /* bridge */ /* synthetic */ SparseArray getProvidedInsetsSources() {
        return super.getProvidedInsetsSources();
    }

    @Override // com.android.server.wm.WindowContainer, com.android.server.wm.SurfaceAnimator.Animatable
    public /* bridge */ /* synthetic */ SurfaceControl getSurfaceControl() {
        return super.getSurfaceControl();
    }

    @Override // com.android.server.wm.WindowContainer, com.android.server.wm.SurfaceAnimator.Animatable
    public /* bridge */ /* synthetic */ int getSurfaceHeight() {
        return super.getSurfaceHeight();
    }

    @Override // com.android.server.wm.WindowContainer, com.android.server.wm.SurfaceAnimator.Animatable
    public /* bridge */ /* synthetic */ int getSurfaceWidth() {
        return super.getSurfaceWidth();
    }

    @Override // com.android.server.wm.WindowContainer, com.android.server.wm.SurfaceAnimator.Animatable
    public /* bridge */ /* synthetic */ SurfaceControl.Transaction getSyncTransaction() {
        return super.getSyncTransaction();
    }

    @Override // com.android.server.wm.WindowContainer, com.android.server.wm.ConfigurationContainer
    public /* bridge */ /* synthetic */ boolean isOSFullDialog() {
        return super.isOSFullDialog();
    }

    @Override // com.android.server.wm.WindowContainer, com.android.server.wm.SurfaceAnimator.Animatable
    public /* bridge */ /* synthetic */ SurfaceControl.Builder makeAnimationLeash() {
        return super.makeAnimationLeash();
    }

    @Override // com.android.server.wm.WindowContainer, com.android.server.wm.SurfaceAnimator.Animatable
    public /* bridge */ /* synthetic */ void onAnimationLeashCreated(SurfaceControl.Transaction transaction, SurfaceControl surfaceControl) {
        super.onAnimationLeashCreated(transaction, surfaceControl);
    }

    @Override // com.android.server.wm.WindowContainer, com.android.server.wm.SurfaceAnimator.Animatable
    public /* bridge */ /* synthetic */ void onAnimationLeashLost(SurfaceControl.Transaction transaction) {
        super.onAnimationLeashLost(transaction);
    }

    @Override // com.android.server.wm.WindowContainer, com.android.server.wm.ConfigurationContainer
    public /* bridge */ /* synthetic */ void onRequestedOverrideConfigurationChanged(Configuration configuration) {
        super.onRequestedOverrideConfigurationChanged(configuration);
    }

    @Override // com.android.server.wm.WindowContainer, com.android.server.wm.SurfaceFreezer.Freezable
    public /* bridge */ /* synthetic */ void onUnfrozen() {
        super.onUnfrozen();
    }

    DisplayArea(WindowManagerService wms, Type type, String name) {
        this(wms, type, name, -1);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public DisplayArea(WindowManagerService wms, Type type, String name, int featureId) {
        super(wms);
        this.mTmpConfiguration = new Configuration();
        this.mNeedsZNoost = false;
        this.mFillsParent = true;
        this.mOrientation = -2;
        this.mType = type;
        this.mName = name;
        this.mFeatureId = featureId;
        this.mRemoteToken = new WindowContainer.RemoteToken(this);
        this.mOrganizerController = wms.mAtmService.mWindowOrganizerController.mDisplayAreaOrganizerController;
        ITranDisplayArea.Instance().onConstruct(wms);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.wm.WindowContainer
    public void onChildPositionChanged(WindowContainer child) {
        super.onChildPositionChanged(child);
        Type.checkChild(this.mType, Type.typeOf(child));
        if (child instanceof Task) {
            return;
        }
        for (int i = 1; i < getChildCount(); i++) {
            WindowContainer top = getChildAt(i - 1);
            WindowContainer bottom = getChildAt(i);
            if (child == top || child == bottom) {
                Type.checkSiblings(Type.typeOf(top), Type.typeOf(bottom));
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.wm.WindowContainer
    public void positionChildAt(int position, T child, boolean includingParents) {
        if (child.asDisplayArea() == null) {
            super.positionChildAt(position, child, includingParents);
            return;
        }
        int targetPosition = findPositionForChildDisplayArea(position, child.asDisplayArea());
        super.positionChildAt(targetPosition, child, false);
        WindowContainer parent = getParent();
        if (!includingParents || parent == null) {
            return;
        }
        if (position == Integer.MAX_VALUE || position == Integer.MIN_VALUE) {
            parent.positionChildAt(position, this, true);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.wm.WindowContainer
    public int getOrientation(int candidate) {
        this.mLastOrientationSource = null;
        if (getIgnoreOrientationRequest()) {
            return -2;
        }
        return super.getOrientation(candidate);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.wm.WindowContainer
    public boolean handlesOrientationChangeFromDescendant() {
        return !getIgnoreOrientationRequest() && super.handlesOrientationChangeFromDescendant();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.wm.WindowContainer
    public boolean onDescendantOrientationChanged(WindowContainer requestingContainer) {
        return !getIgnoreOrientationRequest() && super.onDescendantOrientationChanged(requestingContainer);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean setIgnoreOrientationRequest(boolean ignoreOrientationRequest) {
        if (this.mSetIgnoreOrientationRequest == ignoreOrientationRequest) {
            return false;
        }
        this.mSetIgnoreOrientationRequest = ignoreOrientationRequest;
        if (this.mDisplayContent == null) {
            return false;
        }
        if (this.mDisplayContent.mFocusedApp != null) {
            this.mDisplayContent.onLastFocusedTaskDisplayAreaChanged(this.mDisplayContent.mFocusedApp.getDisplayArea());
        }
        if (!ignoreOrientationRequest) {
            return this.mDisplayContent.updateOrientation();
        }
        int lastOrientation = this.mDisplayContent.getLastOrientation();
        WindowContainer lastOrientationSource = this.mDisplayContent.getLastOrientationSource();
        if (lastOrientation == -2 || lastOrientation == -1) {
            return false;
        }
        if (lastOrientationSource == null || lastOrientationSource.isDescendantOf(this)) {
            return this.mDisplayContent.updateOrientation();
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean getIgnoreOrientationRequest() {
        return this.mSetIgnoreOrientationRequest && !this.mWmService.isIgnoreOrientationRequestDisabled();
    }

    private int findPositionForChildDisplayArea(int requestPosition, DisplayArea child) {
        if (child.getParent() != this) {
            throw new IllegalArgumentException("positionChildAt: container=" + child.getName() + " is not a child of container=" + getName() + " current parent=" + child.getParent());
        }
        int maxPosition = findMaxPositionForChildDisplayArea(child);
        int minPosition = findMinPositionForChildDisplayArea(child);
        return Math.max(Math.min(requestPosition, maxPosition), minPosition);
    }

    private int findMaxPositionForChildDisplayArea(DisplayArea child) {
        Type childType = Type.typeOf(child);
        for (int i = this.mChildren.size() - 1; i > 0; i--) {
            if (Type.typeOf(getChildAt(i)) == childType) {
                return i;
            }
        }
        return 0;
    }

    private int findMinPositionForChildDisplayArea(DisplayArea child) {
        Type childType = Type.typeOf(child);
        for (int i = 0; i < this.mChildren.size(); i++) {
            if (Type.typeOf(getChildAt(i)) == childType) {
                return i;
            }
        }
        return this.mChildren.size() - 1;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.wm.ConfigurationContainer
    public String getName() {
        return this.mName;
    }

    public String toString() {
        return this.mName + "@" + System.identityHashCode(this);
    }

    @Override // com.android.server.wm.WindowContainer, com.android.server.wm.ConfigurationContainer
    public void dumpDebug(ProtoOutputStream proto, long fieldId, int logLevel) {
        if (logLevel == 2 && !isVisible()) {
            return;
        }
        long token = proto.start(fieldId);
        super.dumpDebug(proto, 1146756268033L, logLevel);
        proto.write(1138166333442L, this.mName);
        proto.write(1133871366148L, isTaskDisplayArea());
        proto.write(1133871366149L, asRootDisplayArea() != null);
        proto.write(1120986464262L, this.mFeatureId);
        proto.write(1133871366151L, isOrganized());
        proto.end(token);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.wm.WindowContainer
    public void dump(PrintWriter pw, String prefix, boolean dumpAll) {
        super.dump(pw, prefix, dumpAll);
        if (this.mSetIgnoreOrientationRequest) {
            pw.println(prefix + "mSetIgnoreOrientationRequest=true");
        }
        if (hasRequestedOverrideConfiguration()) {
            pw.println(prefix + "overrideConfig=" + getRequestedOverrideConfiguration());
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dumpChildDisplayArea(PrintWriter pw, String prefix, boolean dumpAll) {
        String doublePrefix = prefix + "  ";
        for (int i = getChildCount() - 1; i >= 0; i--) {
            DisplayArea<?> childArea = getChildAt(i).asDisplayArea();
            if (childArea != null) {
                pw.println(prefix + "* " + childArea.getName());
                if (!childArea.isTaskDisplayArea()) {
                    childArea.dump(pw, doublePrefix, dumpAll);
                    childArea.dumpChildDisplayArea(pw, doublePrefix, dumpAll);
                }
            }
        }
    }

    @Override // com.android.server.wm.WindowContainer
    long getProtoFieldId() {
        return 1146756268036L;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.wm.WindowContainer
    public final DisplayArea asDisplayArea() {
        return this;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Tokens asTokens() {
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.wm.WindowContainer
    public void forAllDisplayAreas(Consumer<DisplayArea> callback) {
        super.forAllDisplayAreas(callback);
        callback.accept(this);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.wm.WindowContainer
    public boolean forAllTaskDisplayAreas(Predicate<TaskDisplayArea> callback, boolean traverseTopToBottom) {
        if (this.mType != Type.ANY) {
            return false;
        }
        int childCount = this.mChildren.size();
        int i = traverseTopToBottom ? childCount - 1 : 0;
        while (i >= 0 && i < childCount) {
            WindowContainer windowContainer = (WindowContainer) this.mChildren.get(i);
            int i2 = 1;
            if (windowContainer.asDisplayArea() != null && windowContainer.asDisplayArea().forAllTaskDisplayAreas(callback, traverseTopToBottom)) {
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
    @Override // com.android.server.wm.WindowContainer
    public void forAllTaskDisplayAreas(Consumer<TaskDisplayArea> callback, boolean traverseTopToBottom) {
        if (this.mType != Type.ANY) {
            return;
        }
        int childCount = this.mChildren.size();
        int i = traverseTopToBottom ? childCount - 1 : 0;
        while (i >= 0 && i < childCount) {
            WindowContainer windowContainer = (WindowContainer) this.mChildren.get(i);
            if (windowContainer.asDisplayArea() != null) {
                windowContainer.asDisplayArea().forAllTaskDisplayAreas(callback, traverseTopToBottom);
            }
            i += traverseTopToBottom ? -1 : 1;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.wm.WindowContainer
    public <R> R reduceOnAllTaskDisplayAreas(BiFunction<TaskDisplayArea, R, R> accumulator, R initValue, boolean traverseTopToBottom) {
        if (this.mType != Type.ANY) {
            return initValue;
        }
        int childCount = this.mChildren.size();
        int i = traverseTopToBottom ? childCount - 1 : 0;
        R result = initValue;
        while (i >= 0 && i < childCount) {
            WindowContainer windowContainer = (WindowContainer) this.mChildren.get(i);
            if (windowContainer.asDisplayArea() != null) {
                result = (R) windowContainer.asDisplayArea().reduceOnAllTaskDisplayAreas(accumulator, result, traverseTopToBottom);
            }
            i += traverseTopToBottom ? -1 : 1;
        }
        return result;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.wm.WindowContainer
    public <R> R getItemFromDisplayAreas(Function<DisplayArea, R> callback) {
        R item = (R) super.getItemFromDisplayAreas(callback);
        return item != null ? item : callback.apply(this);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.wm.WindowContainer
    public <R> R getItemFromTaskDisplayAreas(Function<TaskDisplayArea, R> callback, boolean traverseTopToBottom) {
        R result;
        if (this.mType != Type.ANY) {
            return null;
        }
        int childCount = this.mChildren.size();
        int i = traverseTopToBottom ? childCount - 1 : 0;
        while (i >= 0 && i < childCount) {
            WindowContainer windowContainer = (WindowContainer) this.mChildren.get(i);
            if (windowContainer.asDisplayArea() != null && (result = (R) windowContainer.asDisplayArea().getItemFromTaskDisplayAreas(callback, traverseTopToBottom)) != null) {
                return result;
            }
            i += traverseTopToBottom ? -1 : 1;
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setOrganizer(IDisplayAreaOrganizer organizer) {
        setOrganizer(organizer, false);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setOrganizer(IDisplayAreaOrganizer organizer, boolean skipDisplayAreaAppeared) {
        if (this.mOrganizer == organizer) {
            return;
        }
        if (this.mDisplayContent == null || !this.mDisplayContent.isTrusted()) {
            throw new IllegalStateException("Don't organize or trigger events for unavailable or untrusted display.");
        }
        IDisplayAreaOrganizer lastOrganizer = this.mOrganizer;
        this.mOrganizer = organizer;
        sendDisplayAreaVanished(lastOrganizer);
        if (!skipDisplayAreaAppeared) {
            sendDisplayAreaAppeared();
        }
    }

    void sendDisplayAreaAppeared() {
        IDisplayAreaOrganizer iDisplayAreaOrganizer = this.mOrganizer;
        if (iDisplayAreaOrganizer == null) {
            return;
        }
        this.mOrganizerController.onDisplayAreaAppeared(iDisplayAreaOrganizer, this);
    }

    void sendDisplayAreaVanished(IDisplayAreaOrganizer organizer) {
        if (organizer == null) {
            return;
        }
        migrateToNewSurfaceControl(getSyncTransaction());
        this.mOrganizerController.onDisplayAreaVanished(organizer, this);
    }

    @Override // com.android.server.wm.WindowContainer, com.android.server.wm.ConfigurationContainer
    public void onConfigurationChanged(Configuration newParentConfig) {
        this.mTmpConfiguration.setTo(getConfiguration());
        super.onConfigurationChanged(newParentConfig);
        if (this.mOrganizer != null && getConfiguration().diff(this.mTmpConfiguration) != 0) {
            this.mOrganizerController.onDisplayAreaInfoChanged(this.mOrganizer, this);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.wm.ConfigurationContainer
    public void resolveOverrideConfiguration(Configuration newParentConfiguration) {
        super.resolveOverrideConfiguration(newParentConfiguration);
        Configuration resolvedConfig = getResolvedOverrideConfiguration();
        Rect overrideBounds = resolvedConfig.windowConfiguration.getBounds();
        Rect overrideAppBounds = resolvedConfig.windowConfiguration.getAppBounds();
        Rect parentAppBounds = newParentConfiguration.windowConfiguration.getAppBounds();
        if (!overrideBounds.isEmpty() && ((overrideAppBounds == null || overrideAppBounds.isEmpty()) && parentAppBounds != null && !parentAppBounds.isEmpty())) {
            Rect appBounds = new Rect(overrideBounds);
            appBounds.intersect(parentAppBounds);
            resolvedConfig.windowConfiguration.setAppBounds(appBounds);
        }
        if (isMultiWindow()) {
            this.mFillsParent = false;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.wm.WindowContainer
    public boolean isOrganized() {
        return this.mOrganizer != null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public DisplayAreaInfo getDisplayAreaInfo() {
        DisplayAreaInfo info = new DisplayAreaInfo(this.mRemoteToken.toWindowContainerToken(), getDisplayContent().getDisplayId(), this.mFeatureId);
        RootDisplayArea root = getRootDisplayArea();
        info.rootDisplayAreaId = root == null ? getDisplayContent().mFeatureId : root.mFeatureId;
        info.configuration.setTo(getConfiguration());
        return info;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void getStableRect(Rect out) {
        if (this.mDisplayContent == null) {
            getBounds(out);
            return;
        }
        this.mDisplayContent.getStableRect(out);
        out.intersect(getBounds());
    }

    @Override // com.android.server.wm.ConfigurationContainer
    public boolean providesMaxBounds() {
        if (isMultiWindow()) {
            return false;
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isTaskDisplayArea() {
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.wm.WindowContainer
    public void removeImmediately() {
        setOrganizer(null);
        super.removeImmediately();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.wm.WindowContainer
    public DisplayArea getDisplayArea() {
        return this;
    }

    /* loaded from: classes2.dex */
    public static class Tokens extends DisplayArea<WindowToken> {
        private final Predicate<WindowState> mGetOrientingWindow;
        int mLastKeyguardForcedOrientation;
        private final Comparator<WindowToken> mWindowComparator;

        @Override // com.android.server.wm.DisplayArea, com.android.server.wm.WindowContainer, com.android.server.wm.SurfaceAnimator.Animatable
        public /* bridge */ /* synthetic */ void commitPendingTransaction() {
            super.commitPendingTransaction();
        }

        @Override // com.android.server.wm.DisplayArea, com.android.server.wm.WindowContainer
        public /* bridge */ /* synthetic */ int compareTo(WindowContainer windowContainer) {
            return super.compareTo(windowContainer);
        }

        @Override // com.android.server.wm.DisplayArea, com.android.server.wm.WindowContainer, com.android.server.wm.SurfaceAnimator.Animatable
        public /* bridge */ /* synthetic */ SurfaceControl getAnimationLeash() {
            return super.getAnimationLeash();
        }

        @Override // com.android.server.wm.DisplayArea, com.android.server.wm.WindowContainer, com.android.server.wm.SurfaceAnimator.Animatable
        public /* bridge */ /* synthetic */ SurfaceControl getAnimationLeashParent() {
            return super.getAnimationLeashParent();
        }

        @Override // com.android.server.wm.DisplayArea, com.android.server.wm.WindowContainer
        public /* bridge */ /* synthetic */ DisplayContent getDisplayContent() {
            return super.getDisplayContent();
        }

        @Override // com.android.server.wm.DisplayArea, com.android.server.wm.WindowContainer, com.android.server.wm.SurfaceFreezer.Freezable
        public /* bridge */ /* synthetic */ SurfaceControl getFreezeSnapshotTarget() {
            return super.getFreezeSnapshotTarget();
        }

        @Override // com.android.server.wm.DisplayArea, com.android.server.wm.WindowContainer, com.android.server.wm.SurfaceAnimator.Animatable
        public /* bridge */ /* synthetic */ SurfaceControl getParentSurfaceControl() {
            return super.getParentSurfaceControl();
        }

        @Override // com.android.server.wm.DisplayArea, com.android.server.wm.WindowContainer, com.android.server.wm.SurfaceAnimator.Animatable
        public /* bridge */ /* synthetic */ SurfaceControl.Transaction getPendingTransaction() {
            return super.getPendingTransaction();
        }

        @Override // com.android.server.wm.DisplayArea, com.android.server.wm.WindowContainer
        public /* bridge */ /* synthetic */ SparseArray getProvidedInsetsSources() {
            return super.getProvidedInsetsSources();
        }

        @Override // com.android.server.wm.DisplayArea, com.android.server.wm.WindowContainer, com.android.server.wm.SurfaceAnimator.Animatable
        public /* bridge */ /* synthetic */ SurfaceControl getSurfaceControl() {
            return super.getSurfaceControl();
        }

        @Override // com.android.server.wm.DisplayArea, com.android.server.wm.WindowContainer, com.android.server.wm.SurfaceAnimator.Animatable
        public /* bridge */ /* synthetic */ int getSurfaceHeight() {
            return super.getSurfaceHeight();
        }

        @Override // com.android.server.wm.DisplayArea, com.android.server.wm.WindowContainer, com.android.server.wm.SurfaceAnimator.Animatable
        public /* bridge */ /* synthetic */ int getSurfaceWidth() {
            return super.getSurfaceWidth();
        }

        @Override // com.android.server.wm.DisplayArea, com.android.server.wm.WindowContainer, com.android.server.wm.SurfaceAnimator.Animatable
        public /* bridge */ /* synthetic */ SurfaceControl.Transaction getSyncTransaction() {
            return super.getSyncTransaction();
        }

        @Override // com.android.server.wm.DisplayArea, com.android.server.wm.WindowContainer, com.android.server.wm.ConfigurationContainer
        public /* bridge */ /* synthetic */ boolean isOSFullDialog() {
            return super.isOSFullDialog();
        }

        @Override // com.android.server.wm.DisplayArea, com.android.server.wm.WindowContainer, com.android.server.wm.SurfaceAnimator.Animatable
        public /* bridge */ /* synthetic */ SurfaceControl.Builder makeAnimationLeash() {
            return super.makeAnimationLeash();
        }

        @Override // com.android.server.wm.DisplayArea, com.android.server.wm.WindowContainer, com.android.server.wm.SurfaceAnimator.Animatable
        public /* bridge */ /* synthetic */ void onAnimationLeashCreated(SurfaceControl.Transaction transaction, SurfaceControl surfaceControl) {
            super.onAnimationLeashCreated(transaction, surfaceControl);
        }

        @Override // com.android.server.wm.DisplayArea, com.android.server.wm.WindowContainer, com.android.server.wm.SurfaceAnimator.Animatable
        public /* bridge */ /* synthetic */ void onAnimationLeashLost(SurfaceControl.Transaction transaction) {
            super.onAnimationLeashLost(transaction);
        }

        @Override // com.android.server.wm.DisplayArea, com.android.server.wm.WindowContainer, com.android.server.wm.ConfigurationContainer
        public /* bridge */ /* synthetic */ void onRequestedOverrideConfigurationChanged(Configuration configuration) {
            super.onRequestedOverrideConfigurationChanged(configuration);
        }

        @Override // com.android.server.wm.DisplayArea, com.android.server.wm.WindowContainer, com.android.server.wm.SurfaceFreezer.Freezable
        public /* bridge */ /* synthetic */ void onUnfrozen() {
            super.onUnfrozen();
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$new$0$com-android-server-wm-DisplayArea$Tokens  reason: not valid java name */
        public /* synthetic */ boolean m7889lambda$new$0$comandroidserverwmDisplayArea$Tokens(WindowState w) {
            if (w.isVisible() && w.mLegacyPolicyVisibilityAfterAnim) {
                WindowManagerPolicy policy = this.mWmService.mPolicy;
                if (policy.isKeyguardHostWindow(w.mAttrs)) {
                    if (!this.mDisplayContent.isKeyguardLocked() && this.mDisplayContent.getDisplayPolicy().isAwake() && policy.okToAnimate(true)) {
                        return false;
                    }
                    boolean isUnoccluding = this.mDisplayContent.mAppTransition.isUnoccluding() && this.mDisplayContent.mUnknownAppVisibilityController.allResolved();
                    if (policy.isKeyguardShowingAndNotOccluded() || isUnoccluding) {
                        return true;
                    }
                }
                int req = w.mAttrs.screenOrientation;
                return (req == -1 || req == 3 || req == -2) ? false : true;
            }
            return false;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public Tokens(WindowManagerService wms, Type type, String name) {
            this(wms, type, name, 2);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public Tokens(WindowManagerService wms, Type type, String name, int featureId) {
            super(wms, type, name, featureId);
            this.mLastKeyguardForcedOrientation = -1;
            this.mWindowComparator = Comparator.comparingInt(new ToIntFunction() { // from class: com.android.server.wm.DisplayArea$Tokens$$ExternalSyntheticLambda0
                @Override // java.util.function.ToIntFunction
                public final int applyAsInt(Object obj) {
                    return ((WindowToken) obj).getWindowLayerFromType();
                }
            });
            this.mGetOrientingWindow = new Predicate() { // from class: com.android.server.wm.DisplayArea$Tokens$$ExternalSyntheticLambda1
                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    return DisplayArea.Tokens.this.m7889lambda$new$0$comandroidserverwmDisplayArea$Tokens((WindowState) obj);
                }
            };
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public void addChild(WindowToken token) {
            addChild((Tokens) token, (Comparator<Tokens>) this.mWindowComparator);
        }

        @Override // com.android.server.wm.DisplayArea, com.android.server.wm.WindowContainer
        int getOrientation(int candidate) {
            this.mLastOrientationSource = null;
            if (getIgnoreOrientationRequest()) {
                return -2;
            }
            WindowState win = getWindow(this.mGetOrientingWindow);
            if (win == null) {
                return candidate;
            }
            int req = win.mAttrs.screenOrientation;
            if (ProtoLogCache.WM_DEBUG_ORIENTATION_enabled) {
                String protoLogParam0 = String.valueOf(win);
                long protoLogParam1 = req;
                long protoLogParam2 = this.mDisplayContent.getDisplayId();
                ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_ORIENTATION, 1674747211, 20, (String) null, new Object[]{protoLogParam0, Long.valueOf(protoLogParam1), Long.valueOf(protoLogParam2)});
            }
            if (this.mWmService.mPolicy.isKeyguardHostWindow(win.mAttrs)) {
                if (req != -2 && req != -1) {
                    this.mLastKeyguardForcedOrientation = req;
                } else {
                    req = this.mLastKeyguardForcedOrientation;
                }
            }
            this.mLastOrientationSource = win;
            return req;
        }

        @Override // com.android.server.wm.DisplayArea
        final Tokens asTokens() {
            return this;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public static class Dimmable extends DisplayArea<DisplayArea> {
        private final Dimmer mDimmer;
        private final Rect mTmpDimBoundsRect;

        /* JADX INFO: Access modifiers changed from: package-private */
        public Dimmable(WindowManagerService wms, Type type, String name, int featureId) {
            super(wms, type, name, featureId);
            this.mDimmer = new Dimmer(this);
            this.mTmpDimBoundsRect = new Rect();
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        @Override // com.android.server.wm.WindowContainer
        public Dimmer getDimmer() {
            return this.mDimmer;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        @Override // com.android.server.wm.WindowContainer
        public void prepareSurfaces() {
            this.mDimmer.resetDimStates();
            super.prepareSurfaces();
            getBounds(this.mTmpDimBoundsRect);
            this.mTmpDimBoundsRect.offsetTo(0, 0);
            if (forAllTasks(new Predicate() { // from class: com.android.server.wm.DisplayArea$Dimmable$$ExternalSyntheticLambda0
                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    return DisplayArea.Dimmable.lambda$prepareSurfaces$0((Task) obj);
                }
            })) {
                this.mDimmer.resetDimStates();
            }
            if (this.mDimmer.updateDims(getSyncTransaction(), this.mTmpDimBoundsRect)) {
                scheduleAnimation();
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public static /* synthetic */ boolean lambda$prepareSurfaces$0(Task task) {
            return !task.canAffectSystemUiFlags();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public enum Type {
        ABOVE_TASKS,
        BELOW_TASKS,
        ANY;

        static void checkSiblings(Type bottom, Type top) {
            Type type = BELOW_TASKS;
            boolean z = false;
            Preconditions.checkState(bottom == type || top != type, bottom + " must be above BELOW_TASKS");
            Type type2 = ABOVE_TASKS;
            if (bottom != type2 || top == type2) {
                z = true;
            }
            Preconditions.checkState(z, top + " must be below ABOVE_TASKS");
        }

        static void checkChild(Type parent, Type child) {
            switch (AnonymousClass1.$SwitchMap$com$android$server$wm$DisplayArea$Type[parent.ordinal()]) {
                case 1:
                    Preconditions.checkState(child == ABOVE_TASKS, "ABOVE_TASKS can only contain ABOVE_TASKS");
                    return;
                case 2:
                    Preconditions.checkState(child == BELOW_TASKS, "BELOW_TASKS can only contain BELOW_TASKS");
                    return;
                default:
                    return;
            }
        }

        static Type typeOf(WindowContainer c) {
            if (c.asDisplayArea() != null) {
                return ((DisplayArea) c).mType;
            }
            if ((c instanceof WindowToken) && !(c instanceof ActivityRecord)) {
                return typeOf((WindowToken) c);
            }
            if (c instanceof Task) {
                return ANY;
            }
            throw new IllegalArgumentException("Unknown container: " + c);
        }

        private static Type typeOf(WindowToken c) {
            return c.getWindowLayerFromType() < 2 ? BELOW_TASKS : ABOVE_TASKS;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: com.android.server.wm.DisplayArea$1  reason: invalid class name */
    /* loaded from: classes2.dex */
    public static /* synthetic */ class AnonymousClass1 {
        static final /* synthetic */ int[] $SwitchMap$com$android$server$wm$DisplayArea$Type;

        static {
            int[] iArr = new int[Type.values().length];
            $SwitchMap$com$android$server$wm$DisplayArea$Type = iArr;
            try {
                iArr[Type.ABOVE_TASKS.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$android$server$wm$DisplayArea$Type[Type.BELOW_TASKS.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.wm.WindowContainer
    public boolean needsZBoost() {
        return this.mNeedsZNoost;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.wm.WindowContainer
    public boolean fillsParent() {
        return this.mFillsParent;
    }

    public void setDisplayAreaAppearedInfo(DisplayAreaAppearedInfo displayAreaAppearedInfo) {
        this.mDisplayAreaAppearedInfo = displayAreaAppearedInfo;
    }

    public DisplayAreaAppearedInfo getDisplayAreaAppearedInfo() {
        return this.mDisplayAreaAppearedInfo;
    }

    public boolean isMultiWindow() {
        return getConfiguration().windowConfiguration.isThunderbackWindow();
    }

    public int getMultiWindowingMode() {
        return getConfiguration().windowConfiguration.getMultiWindowingMode();
    }

    public int getMultiWindowingId() {
        return getConfiguration().windowConfiguration.getMultiWindowingId();
    }

    public void setTranMultiWindowMode(int mode) {
        setMultiWindowMode(mode);
    }
}
