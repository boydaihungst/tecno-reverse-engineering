package com.android.server.wm;

import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Pair;
import android.util.Slog;
import android.util.SparseArray;
import android.view.IWindow;
import android.view.InputWindowHandle;
import android.view.MagnificationSpec;
import android.view.WindowInfo;
import android.window.WindowInfosListener;
import com.android.server.wm.AccessibilityWindowsPopulator;
import com.android.server.wm.utils.RegionUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
/* loaded from: classes2.dex */
public final class AccessibilityWindowsPopulator extends WindowInfosListener {
    private static final int SURFACE_FLINGER_CALLBACK_WINDOWS_STABLE_TIMES_MS = 35;
    private static final int WINDOWS_CHANGED_NOTIFICATION_MAX_DURATION_TIMES_MS = 500;
    private final AccessibilityController mAccessibilityController;
    private final Handler mHandler;
    private final WindowManagerService mService;
    private static final String TAG = AccessibilityWindowsPopulator.class.getSimpleName();
    private static final float[] sTempFloats = new float[9];
    private final SparseArray<List<InputWindowHandle>> mInputWindowHandlesOnDisplays = new SparseArray<>();
    private final SparseArray<Matrix> mMagnificationSpecInverseMatrix = new SparseArray<>();
    private final SparseArray<WindowInfosListener.DisplayInfo> mDisplayInfos = new SparseArray<>();
    private final SparseArray<MagnificationSpec> mCurrentMagnificationSpec = new SparseArray<>();
    private final SparseArray<MagnificationSpec> mPreviousMagnificationSpec = new SparseArray<>();
    private final List<InputWindowHandle> mVisibleWindows = new ArrayList();
    private boolean mWindowsNotificationEnabled = false;
    private final Map<IBinder, Matrix> mWindowsTransformMatrixMap = new HashMap();
    private final Object mLock = new Object();
    private final Matrix mTempMatrix1 = new Matrix();
    private final Matrix mTempMatrix2 = new Matrix();
    private final float[] mTempFloat1 = new float[9];
    private final float[] mTempFloat2 = new float[9];
    private final float[] mTempFloat3 = new float[9];

    /* JADX INFO: Access modifiers changed from: package-private */
    public AccessibilityWindowsPopulator(WindowManagerService service, AccessibilityController accessibilityController) {
        this.mService = service;
        this.mAccessibilityController = accessibilityController;
        this.mHandler = new MyHandler(service.mH.getLooper());
    }

    public void populateVisibleWindowsOnScreenLocked(int displayId, List<AccessibilityWindow> outWindows) {
        Matrix inverseMatrix = new Matrix();
        Matrix displayMatrix = new Matrix();
        synchronized (this.mLock) {
            List<InputWindowHandle> inputWindowHandles = this.mInputWindowHandlesOnDisplays.get(displayId);
            if (inputWindowHandles == null) {
                outWindows.clear();
                return;
            }
            inverseMatrix.set(this.mMagnificationSpecInverseMatrix.get(displayId));
            WindowInfosListener.DisplayInfo displayInfo = this.mDisplayInfos.get(displayId);
            if (displayInfo != null) {
                displayMatrix.set(displayInfo.mTransform);
            } else {
                Slog.w(TAG, "The displayInfo of this displayId (" + displayId + ") called back from the surface fligner is null");
            }
            DisplayContent dc = this.mService.mRoot.getDisplayContent(displayId);
            ShellRoot shellroot = dc.mShellRoots.get(1);
            IBinder pipMenuIBinder = shellroot != null ? shellroot.getAccessibilityWindowToken() : null;
            for (InputWindowHandle windowHandle : inputWindowHandles) {
                AccessibilityWindow accessibilityWindow = AccessibilityWindow.initializeData(this.mService, windowHandle, inverseMatrix, pipMenuIBinder, displayMatrix);
                outWindows.add(accessibilityWindow);
            }
        }
    }

    public void onWindowInfosChanged(final InputWindowHandle[] windowHandles, final WindowInfosListener.DisplayInfo[] displayInfos) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.wm.AccessibilityWindowsPopulator$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                AccessibilityWindowsPopulator.this.m7750xec653b15(windowHandles, displayInfos);
            }
        });
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: onWindowInfosChangedInternal */
    public void m7750xec653b15(InputWindowHandle[] windowHandles, WindowInfosListener.DisplayInfo[] displayInfos) {
        List<InputWindowHandle> tempVisibleWindows = new ArrayList<>();
        for (InputWindowHandle window : windowHandles) {
            boolean visible = (window.inputConfig & 2) == 0;
            if (visible && window.getWindow() != null && !window.isClone) {
                tempVisibleWindows.add(window);
            }
        }
        HashMap<IBinder, Matrix> windowsTransformMatrixMap = getWindowsTransformMatrix(tempVisibleWindows);
        synchronized (this.mLock) {
            this.mWindowsTransformMatrixMap.clear();
            this.mWindowsTransformMatrixMap.putAll(windowsTransformMatrixMap);
            this.mVisibleWindows.clear();
            this.mVisibleWindows.addAll(tempVisibleWindows);
            this.mDisplayInfos.clear();
            for (WindowInfosListener.DisplayInfo displayInfo : displayInfos) {
                this.mDisplayInfos.put(displayInfo.mDisplayId, displayInfo);
            }
            if (this.mWindowsNotificationEnabled) {
                if (!this.mHandler.hasMessages(3)) {
                    this.mHandler.sendEmptyMessageDelayed(3, 500L);
                }
                populateVisibleWindowHandlesAndNotifyWindowsChangeIfNeeded();
            }
        }
    }

    private HashMap<IBinder, Matrix> getWindowsTransformMatrix(List<InputWindowHandle> windows) {
        HashMap<IBinder, Matrix> windowsTransformMatrixMap;
        WindowState windowState;
        synchronized (this.mService.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                windowsTransformMatrixMap = new HashMap<>();
                for (InputWindowHandle inputWindowHandle : windows) {
                    IWindow iWindow = inputWindowHandle.getWindow();
                    if (iWindow != null) {
                        windowState = this.mService.mWindowMap.get(iWindow.asBinder());
                    } else {
                        windowState = null;
                    }
                    if (windowState != null && windowState.shouldMagnify()) {
                        Matrix transformMatrix = new Matrix();
                        windowState.getTransformationMatrix(sTempFloats, transformMatrix);
                        windowsTransformMatrixMap.put(iWindow.asBinder(), transformMatrix);
                    }
                }
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
        return windowsTransformMatrixMap;
    }

    public void setWindowsNotification(boolean register) {
        synchronized (this.mLock) {
            if (this.mWindowsNotificationEnabled == register) {
                return;
            }
            this.mWindowsNotificationEnabled = register;
            if (register) {
                Pair<InputWindowHandle[], WindowInfosListener.DisplayInfo[]> info = register();
                m7750xec653b15((InputWindowHandle[]) info.first, (WindowInfosListener.DisplayInfo[]) info.second);
            } else {
                unregister();
                releaseResources();
            }
        }
    }

    public void setMagnificationSpec(int displayId, MagnificationSpec spec) {
        synchronized (this.mLock) {
            MagnificationSpec currentMagnificationSpec = this.mCurrentMagnificationSpec.get(displayId);
            if (currentMagnificationSpec == null) {
                MagnificationSpec currentMagnificationSpec2 = new MagnificationSpec();
                currentMagnificationSpec2.setTo(spec);
                this.mCurrentMagnificationSpec.put(displayId, currentMagnificationSpec2);
                return;
            }
            MagnificationSpec previousMagnificationSpec = this.mPreviousMagnificationSpec.get(displayId);
            if (previousMagnificationSpec == null) {
                previousMagnificationSpec = new MagnificationSpec();
                this.mPreviousMagnificationSpec.put(displayId, previousMagnificationSpec);
            }
            previousMagnificationSpec.setTo(currentMagnificationSpec);
            currentMagnificationSpec.setTo(spec);
        }
    }

    private void populateVisibleWindowHandlesAndNotifyWindowsChangeIfNeeded() {
        SparseArray<List<InputWindowHandle>> tempWindowHandleList = new SparseArray<>();
        for (InputWindowHandle windowHandle : this.mVisibleWindows) {
            List<InputWindowHandle> inputWindowHandles = tempWindowHandleList.get(windowHandle.displayId);
            if (inputWindowHandles == null) {
                inputWindowHandles = new ArrayList();
                tempWindowHandleList.put(windowHandle.displayId, inputWindowHandles);
            }
            inputWindowHandles.add(windowHandle);
        }
        findMagnificationSpecInverseMatrixIfNeeded(tempWindowHandleList);
        List<Integer> displayIdsForWindowsChanged = new ArrayList<>();
        getDisplaysForWindowsChanged(displayIdsForWindowsChanged, tempWindowHandleList, this.mInputWindowHandlesOnDisplays);
        this.mInputWindowHandlesOnDisplays.clear();
        for (int i = 0; i < tempWindowHandleList.size(); i++) {
            int displayId = tempWindowHandleList.keyAt(i);
            this.mInputWindowHandlesOnDisplays.put(displayId, tempWindowHandleList.get(displayId));
        }
        if (!displayIdsForWindowsChanged.isEmpty()) {
            if (!this.mHandler.hasMessages(1)) {
                this.mHandler.obtainMessage(1, displayIdsForWindowsChanged).sendToTarget();
                return;
            }
            return;
        }
        this.mHandler.removeMessages(2);
        this.mHandler.sendEmptyMessageDelayed(2, 35L);
    }

    private static void getDisplaysForWindowsChanged(List<Integer> outDisplayIdsForWindowsChanged, SparseArray<List<InputWindowHandle>> newWindowsList, SparseArray<List<InputWindowHandle>> oldWindowsList) {
        for (int i = 0; i < newWindowsList.size(); i++) {
            int displayId = newWindowsList.keyAt(i);
            List<InputWindowHandle> newWindows = newWindowsList.get(displayId);
            List<InputWindowHandle> oldWindows = oldWindowsList.get(displayId);
            if (hasWindowsChanged(newWindows, oldWindows)) {
                outDisplayIdsForWindowsChanged.add(Integer.valueOf(displayId));
            }
        }
    }

    private static boolean hasWindowsChanged(List<InputWindowHandle> newWindows, List<InputWindowHandle> oldWindows) {
        if (oldWindows == null || oldWindows.size() != newWindows.size()) {
            return true;
        }
        int windowsCount = newWindows.size();
        for (int i = 0; i < windowsCount; i++) {
            InputWindowHandle newWindow = newWindows.get(i);
            InputWindowHandle oldWindow = oldWindows.get(i);
            if (!newWindow.getWindow().asBinder().equals(oldWindow.getWindow().asBinder())) {
                return true;
            }
        }
        return false;
    }

    private void findMagnificationSpecInverseMatrixIfNeeded(SparseArray<List<InputWindowHandle>> windowHandleList) {
        for (int i = 0; i < windowHandleList.size(); i++) {
            int displayId = windowHandleList.keyAt(i);
            List<InputWindowHandle> inputWindowHandles = windowHandleList.get(displayId);
            MagnificationSpec currentSpec = this.mCurrentMagnificationSpec.get(displayId);
            if (currentSpec != null) {
                MagnificationSpec currentMagnificationSpec = new MagnificationSpec();
                currentMagnificationSpec.setTo(currentSpec);
                MagnificationSpec previousSpec = this.mPreviousMagnificationSpec.get(displayId);
                if (previousSpec == null) {
                    Matrix inverseMatrixForCurrentSpec = new Matrix();
                    generateInverseMatrix(currentMagnificationSpec, inverseMatrixForCurrentSpec);
                    this.mMagnificationSpecInverseMatrix.put(displayId, inverseMatrixForCurrentSpec);
                } else {
                    MagnificationSpec previousMagnificationSpec = new MagnificationSpec();
                    previousMagnificationSpec.setTo(previousSpec);
                    generateInverseMatrixBasedOnProperMagnificationSpecForDisplay(inputWindowHandles, currentMagnificationSpec, previousMagnificationSpec);
                }
            }
        }
    }

    private void generateInverseMatrixBasedOnProperMagnificationSpecForDisplay(List<InputWindowHandle> inputWindowHandles, MagnificationSpec currentMagnificationSpec, MagnificationSpec previousMagnificationSpec) {
        for (int index = inputWindowHandles.size() - 1; index >= 0; index--) {
            Matrix windowTransformMatrix = this.mTempMatrix2;
            InputWindowHandle windowHandle = inputWindowHandles.get(index);
            IBinder iBinder = windowHandle.getWindow().asBinder();
            if (getWindowTransformMatrix(iBinder, windowTransformMatrix)) {
                generateMagnificationSpecInverseMatrix(windowHandle, currentMagnificationSpec, previousMagnificationSpec, windowTransformMatrix);
                return;
            }
        }
    }

    private boolean getWindowTransformMatrix(IBinder iBinder, Matrix outTransform) {
        Matrix windowMatrix = iBinder != null ? this.mWindowsTransformMatrixMap.get(iBinder) : null;
        if (windowMatrix == null) {
            return false;
        }
        outTransform.set(windowMatrix);
        return true;
    }

    private void generateMagnificationSpecInverseMatrix(InputWindowHandle inputWindowHandle, MagnificationSpec currentMagnificationSpec, MagnificationSpec previousMagnificationSpec, Matrix transformMatrix) {
        float[] identityMatrixFloatsForCurrentSpec = this.mTempFloat1;
        computeIdentityMatrix(inputWindowHandle, currentMagnificationSpec, transformMatrix, identityMatrixFloatsForCurrentSpec);
        float[] identityMatrixFloatsForPreviousSpec = this.mTempFloat2;
        computeIdentityMatrix(inputWindowHandle, previousMagnificationSpec, transformMatrix, identityMatrixFloatsForPreviousSpec);
        Matrix inverseMatrixForMagnificationSpec = new Matrix();
        if (selectProperMagnificationSpecByComparingIdentityDegree(identityMatrixFloatsForCurrentSpec, identityMatrixFloatsForPreviousSpec)) {
            generateInverseMatrix(currentMagnificationSpec, inverseMatrixForMagnificationSpec);
            this.mPreviousMagnificationSpec.remove(inputWindowHandle.displayId);
            if (currentMagnificationSpec.isNop()) {
                this.mCurrentMagnificationSpec.remove(inputWindowHandle.displayId);
                this.mMagnificationSpecInverseMatrix.remove(inputWindowHandle.displayId);
                return;
            }
        } else {
            generateInverseMatrix(previousMagnificationSpec, inverseMatrixForMagnificationSpec);
        }
        this.mMagnificationSpecInverseMatrix.put(inputWindowHandle.displayId, inverseMatrixForMagnificationSpec);
    }

    private void computeIdentityMatrix(InputWindowHandle inputWindowHandle, MagnificationSpec magnificationSpec, Matrix transformMatrix, float[] magnifyMatrixFloats) {
        Matrix specMatrix = this.mTempMatrix1;
        transformMagnificationSpecToMatrix(magnificationSpec, specMatrix);
        Matrix resultMatrix = new Matrix(inputWindowHandle.transform);
        resultMatrix.preConcat(specMatrix);
        resultMatrix.preConcat(transformMatrix);
        resultMatrix.getValues(magnifyMatrixFloats);
    }

    private boolean selectProperMagnificationSpecByComparingIdentityDegree(float[] magnifyMatrixFloatsForSpecOne, float[] magnifyMatrixFloatsForSpecTwo) {
        float[] IdentityMatrixValues = this.mTempFloat3;
        Matrix.IDENTITY_MATRIX.getValues(IdentityMatrixValues);
        float scaleDiffForSpecOne = Math.abs(IdentityMatrixValues[0] - magnifyMatrixFloatsForSpecOne[0]);
        float scaleDiffForSpecTwo = Math.abs(IdentityMatrixValues[0] - magnifyMatrixFloatsForSpecTwo[0]);
        float offsetXDiffForSpecOne = Math.abs(IdentityMatrixValues[2] - magnifyMatrixFloatsForSpecOne[2]);
        float offsetXDiffForSpecTwo = Math.abs(IdentityMatrixValues[2] - magnifyMatrixFloatsForSpecTwo[2]);
        float offsetYDiffForSpecOne = Math.abs(IdentityMatrixValues[5] - magnifyMatrixFloatsForSpecOne[5]);
        float offsetYDiffForSpecTwo = Math.abs(IdentityMatrixValues[5] - magnifyMatrixFloatsForSpecTwo[5]);
        float offsetDiffForSpecOne = offsetXDiffForSpecOne + offsetYDiffForSpecOne;
        float offsetDiffForSpecTwo = offsetXDiffForSpecTwo + offsetYDiffForSpecTwo;
        return Float.compare(scaleDiffForSpecTwo, scaleDiffForSpecOne) > 0 || (Float.compare(scaleDiffForSpecTwo, scaleDiffForSpecOne) == 0 && Float.compare(offsetDiffForSpecTwo, offsetDiffForSpecOne) > 0);
    }

    private static void generateInverseMatrix(MagnificationSpec spec, Matrix outMatrix) {
        outMatrix.reset();
        Matrix tempMatrix = new Matrix();
        transformMagnificationSpecToMatrix(spec, tempMatrix);
        boolean result = tempMatrix.invert(outMatrix);
        if (!result) {
            Slog.e(TAG, "Can't inverse the magnification spec matrix with the magnification spec = " + spec);
            outMatrix.reset();
        }
    }

    private static void transformMagnificationSpecToMatrix(MagnificationSpec spec, Matrix outMatrix) {
        outMatrix.reset();
        outMatrix.postScale(spec.scale, spec.scale);
        outMatrix.postTranslate(spec.offsetX, spec.offsetY);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifyWindowsChanged(List<Integer> displayIdsForWindowsChanged) {
        this.mHandler.removeMessages(3);
        for (int i = 0; i < displayIdsForWindowsChanged.size(); i++) {
            this.mAccessibilityController.performComputeChangedWindowsNot(displayIdsForWindowsChanged.get(i).intValue(), false);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void forceUpdateWindows() {
        List<Integer> displayIdsForWindowsChanged = new ArrayList<>();
        synchronized (this.mLock) {
            for (int i = 0; i < this.mInputWindowHandlesOnDisplays.size(); i++) {
                int displayId = this.mInputWindowHandlesOnDisplays.keyAt(i);
                displayIdsForWindowsChanged.add(Integer.valueOf(displayId));
            }
        }
        notifyWindowsChanged(displayIdsForWindowsChanged);
    }

    private void releaseResources() {
        this.mInputWindowHandlesOnDisplays.clear();
        this.mMagnificationSpecInverseMatrix.clear();
        this.mVisibleWindows.clear();
        this.mDisplayInfos.clear();
        this.mCurrentMagnificationSpec.clear();
        this.mPreviousMagnificationSpec.clear();
        this.mWindowsTransformMatrixMap.clear();
        this.mWindowsNotificationEnabled = false;
        this.mHandler.removeCallbacksAndMessages(null);
    }

    /* loaded from: classes2.dex */
    private class MyHandler extends Handler {
        public static final int MESSAGE_NOTIFY_WINDOWS_CHANGED = 1;
        public static final int MESSAGE_NOTIFY_WINDOWS_CHANGED_BY_TIMEOUT = 3;
        public static final int MESSAGE_NOTIFY_WINDOWS_CHANGED_BY_UI_STABLE = 2;

        MyHandler(Looper looper) {
            super(looper, null, false);
        }

        @Override // android.os.Handler
        public void handleMessage(Message message) {
            switch (message.what) {
                case 1:
                    List<Integer> displayIdsForWindowsChanged = (List) message.obj;
                    AccessibilityWindowsPopulator.this.notifyWindowsChanged(displayIdsForWindowsChanged);
                    return;
                case 2:
                    AccessibilityWindowsPopulator.this.forceUpdateWindows();
                    return;
                case 3:
                    Slog.w(AccessibilityWindowsPopulator.TAG, "Windows change within in 2 frames continuously over 500 ms and notify windows changed immediately");
                    AccessibilityWindowsPopulator.this.mHandler.removeMessages(2);
                    AccessibilityWindowsPopulator.this.forceUpdateWindows();
                    return;
                default:
                    return;
            }
        }
    }

    /* loaded from: classes2.dex */
    public static class AccessibilityWindow {
        private int mDisplayId;
        private boolean mIgnoreDuetoRecentsAnimation;
        private int mInputConfig;
        private boolean mIsFocused;
        private boolean mIsPIPMenu;
        private int mPrivateFlags;
        private boolean mShouldMagnify;
        private int mType;
        private IWindow mWindow;
        private WindowInfo mWindowInfo;
        private final Region mTouchableRegionInScreen = new Region();
        private final Region mTouchableRegionInWindow = new Region();
        private final Region mLetterBoxBounds = new Region();

        public static AccessibilityWindow initializeData(WindowManagerService service, InputWindowHandle inputWindowHandle, Matrix magnificationInverseMatrix, IBinder pipIBinder, Matrix displayMatrix) {
            boolean z;
            boolean z2;
            IWindow window = inputWindowHandle.getWindow();
            WindowState windowState = window != null ? service.mWindowMap.get(window.asBinder()) : null;
            AccessibilityWindow instance = new AccessibilityWindow();
            instance.mWindow = inputWindowHandle.getWindow();
            instance.mDisplayId = inputWindowHandle.displayId;
            instance.mInputConfig = inputWindowHandle.inputConfig;
            instance.mType = inputWindowHandle.layoutParamsType;
            instance.mIsPIPMenu = inputWindowHandle.getWindow().asBinder().equals(pipIBinder);
            instance.mPrivateFlags = windowState != null ? windowState.mAttrs.privateFlags : 0;
            boolean z3 = true;
            if (windowState == null || !windowState.isFocused()) {
                z = false;
            } else {
                z = true;
            }
            instance.mIsFocused = z;
            if (windowState != null && !windowState.shouldMagnify()) {
                z2 = false;
            } else {
                z2 = true;
            }
            instance.mShouldMagnify = z2;
            RecentsAnimationController controller = service.getRecentsAnimationController();
            if (windowState == null || controller == null || !controller.shouldIgnoreForAccessibility(windowState)) {
                z3 = false;
            }
            instance.mIgnoreDuetoRecentsAnimation = z3;
            if (windowState != null && windowState.areAppWindowBoundsLetterboxed()) {
                getLetterBoxBounds(windowState, instance.mLetterBoxBounds);
            }
            Rect windowFrame = new Rect(inputWindowHandle.frameLeft, inputWindowHandle.frameTop, inputWindowHandle.frameRight, inputWindowHandle.frameBottom);
            getTouchableRegionInWindow(instance.mShouldMagnify, inputWindowHandle.touchableRegion, instance.mTouchableRegionInWindow, windowFrame, magnificationInverseMatrix, displayMatrix);
            getUnMagnifiedTouchableRegion(instance.mShouldMagnify, inputWindowHandle.touchableRegion, instance.mTouchableRegionInScreen, magnificationInverseMatrix, displayMatrix);
            instance.mWindowInfo = windowState != null ? windowState.getWindowInfo() : getWindowInfoForWindowlessWindows(instance);
            Matrix inverseTransform = new Matrix();
            inputWindowHandle.transform.invert(inverseTransform);
            inverseTransform.postConcat(displayMatrix);
            inverseTransform.getValues(instance.mWindowInfo.mTransformMatrix);
            Matrix magnificationSpecMatrix = new Matrix();
            if (instance.shouldMagnify() && magnificationInverseMatrix != null && !magnificationInverseMatrix.isIdentity()) {
                if (magnificationInverseMatrix.invert(magnificationSpecMatrix)) {
                    magnificationSpecMatrix.getValues(AccessibilityWindowsPopulator.sTempFloats);
                    MagnificationSpec spec = instance.mWindowInfo.mMagnificationSpec;
                    spec.scale = AccessibilityWindowsPopulator.sTempFloats[0];
                    spec.offsetX = AccessibilityWindowsPopulator.sTempFloats[2];
                    spec.offsetY = AccessibilityWindowsPopulator.sTempFloats[5];
                } else {
                    Slog.w(AccessibilityWindowsPopulator.TAG, "can't find spec");
                }
            }
            return instance;
        }

        public void getTouchableRegionInScreen(Region outRegion) {
            outRegion.set(this.mTouchableRegionInScreen);
        }

        public void getTouchableRegionInWindow(Region outRegion) {
            outRegion.set(this.mTouchableRegionInWindow);
        }

        public int getType() {
            return this.mType;
        }

        public int getPrivateFlag() {
            return this.mPrivateFlags;
        }

        public WindowInfo getWindowInfo() {
            return this.mWindowInfo;
        }

        public Boolean setLetterBoxBoundsIfNeeded(Region outBounds) {
            if (this.mLetterBoxBounds.isEmpty()) {
                return false;
            }
            outBounds.set(this.mLetterBoxBounds);
            return true;
        }

        public boolean shouldMagnify() {
            return this.mShouldMagnify;
        }

        public boolean isFocused() {
            return this.mIsFocused;
        }

        public boolean ignoreRecentsAnimationForAccessibility() {
            return this.mIgnoreDuetoRecentsAnimation;
        }

        public boolean isTrustedOverlay() {
            return (this.mInputConfig & 256) != 0;
        }

        public boolean isTouchable() {
            return (this.mInputConfig & 8) == 0;
        }

        public boolean isUntouchableNavigationBar() {
            if (this.mType != 2019) {
                return false;
            }
            return this.mTouchableRegionInScreen.isEmpty();
        }

        public boolean isPIPMenu() {
            return this.mIsPIPMenu;
        }

        private static void getTouchableRegionInWindow(boolean shouldMagnify, Region inRegion, Region outRegion, Rect frame, Matrix inverseMatrix, Matrix displayMatrix) {
            Region touchRegion = new Region();
            touchRegion.set(inRegion);
            touchRegion.op(frame, Region.Op.INTERSECT);
            getUnMagnifiedTouchableRegion(shouldMagnify, touchRegion, outRegion, inverseMatrix, displayMatrix);
        }

        private static void getUnMagnifiedTouchableRegion(boolean shouldMagnify, Region inRegion, final Region outRegion, final Matrix inverseMatrix, final Matrix displayMatrix) {
            if ((!shouldMagnify || inverseMatrix.isIdentity()) && displayMatrix.isIdentity()) {
                outRegion.set(inRegion);
            } else {
                RegionUtils.forEachRect(inRegion, new Consumer() { // from class: com.android.server.wm.AccessibilityWindowsPopulator$AccessibilityWindow$$ExternalSyntheticLambda0
                    @Override // java.util.function.Consumer
                    public final void accept(Object obj) {
                        AccessibilityWindowsPopulator.AccessibilityWindow.lambda$getUnMagnifiedTouchableRegion$0(displayMatrix, inverseMatrix, outRegion, (Rect) obj);
                    }
                });
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public static /* synthetic */ void lambda$getUnMagnifiedTouchableRegion$0(Matrix displayMatrix, Matrix inverseMatrix, Region outRegion, Rect rect) {
            RectF windowFrame = new RectF(rect);
            displayMatrix.mapRect(windowFrame);
            inverseMatrix.mapRect(windowFrame);
            outRegion.union(new Rect((int) windowFrame.left, (int) windowFrame.top, (int) windowFrame.right, (int) windowFrame.bottom));
        }

        private static WindowInfo getWindowInfoForWindowlessWindows(AccessibilityWindow window) {
            WindowInfo windowInfo = WindowInfo.obtain();
            windowInfo.displayId = window.mDisplayId;
            windowInfo.type = window.mType;
            windowInfo.token = window.mWindow.asBinder();
            windowInfo.hasFlagWatchOutsideTouch = (window.mInputConfig & 512) != 0;
            windowInfo.inPictureInPicture = false;
            if (windowInfo.type == 2034) {
                windowInfo.title = "Splitscreen Divider";
            } else if (window.mIsPIPMenu) {
                windowInfo.title = "Picture-in-Picture menu";
                windowInfo.inPictureInPicture = true;
            }
            return windowInfo;
        }

        private static void getLetterBoxBounds(WindowState windowState, Region outRegion) {
            Rect letterboxInsets = windowState.mActivityRecord.getLetterboxInsets();
            Rect nonLetterboxRect = windowState.getBounds();
            nonLetterboxRect.inset(letterboxInsets);
            outRegion.set(windowState.getBounds());
            outRegion.op(nonLetterboxRect, Region.Op.DIFFERENCE);
        }

        public String toString() {
            String builder = "A11yWindow=[" + this.mWindow.asBinder() + ", displayId=" + this.mDisplayId + ", inputConfig=0x" + Integer.toHexString(this.mInputConfig) + ", type=" + this.mType + ", privateFlag=0x" + Integer.toHexString(this.mPrivateFlags) + ", focused=" + this.mIsFocused + ", shouldMagnify=" + this.mShouldMagnify + ", ignoreDuetoRecentsAnimation=" + this.mIgnoreDuetoRecentsAnimation + ", isTrustedOverlay=" + isTrustedOverlay() + ", regionInScreen=" + this.mTouchableRegionInScreen + ", touchableRegion=" + this.mTouchableRegionInWindow + ", letterBoxBounds=" + this.mLetterBoxBounds + ", isPIPMenu=" + this.mIsPIPMenu + ", windowInfo=" + this.mWindowInfo + "]";
            return builder;
        }
    }
}
