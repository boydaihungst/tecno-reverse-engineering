package com.android.server.wm;

import android.app.compat.CompatChanges;
import android.os.Process;
import android.view.InputApplicationHandle;
import android.view.InputWindowHandle;
import android.view.SurfaceControl;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class ActivityRecordInputSink {
    static final long ENABLE_TOUCH_OPAQUE_ACTIVITIES = 194480991;
    private final ActivityRecord mActivityRecord;
    private InputWindowHandleWrapper mInputWindowHandleWrapper;
    private final boolean mIsCompatEnabled;
    private final String mName;
    private SurfaceControl mSurfaceControl;

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActivityRecordInputSink(ActivityRecord activityRecord, ActivityRecord sourceRecord) {
        this.mActivityRecord = activityRecord;
        this.mIsCompatEnabled = CompatChanges.isChangeEnabled((long) ENABLE_TOUCH_OPAQUE_ACTIVITIES, activityRecord.getUid());
        this.mName = Integer.toHexString(System.identityHashCode(this)) + " ActivityRecordInputSink " + activityRecord.mActivityComponent.flattenToShortString();
        if (sourceRecord != null) {
            sourceRecord.mAllowedTouchUid = activityRecord.getUid();
        }
    }

    public void applyChangesToSurfaceIfChanged(SurfaceControl.Transaction transaction) {
        if (!this.mActivityRecord.isVisible()) {
            SurfaceControl surfaceControl = this.mSurfaceControl;
            if (surfaceControl != null) {
                transaction.remove(surfaceControl);
                this.mSurfaceControl = null;
                return;
            }
            return;
        }
        InputWindowHandleWrapper inputWindowHandleWrapper = getInputWindowHandleWrapper();
        if (this.mSurfaceControl == null) {
            this.mSurfaceControl = createSurface(transaction);
        }
        if (inputWindowHandleWrapper.isChanged()) {
            inputWindowHandleWrapper.applyChangesToSurface(transaction, this.mSurfaceControl);
        }
    }

    private SurfaceControl createSurface(SurfaceControl.Transaction t) {
        SurfaceControl surfaceControl = this.mActivityRecord.makeChildSurface(null).setName(this.mName).setHidden(false).setCallsite("ActivityRecordInputSink.createSurface").build();
        t.setLayer(surfaceControl, Integer.MIN_VALUE);
        return surfaceControl;
    }

    private InputWindowHandleWrapper getInputWindowHandleWrapper() {
        if (this.mInputWindowHandleWrapper == null) {
            this.mInputWindowHandleWrapper = new InputWindowHandleWrapper(createInputWindowHandle());
        }
        ActivityRecord activityBelowInTask = this.mActivityRecord.getTask().getActivityBelow(this.mActivityRecord);
        boolean allowPassthrough = activityBelowInTask != null && (activityBelowInTask.mAllowedTouchUid == this.mActivityRecord.getUid() || activityBelowInTask.isUid(this.mActivityRecord.getUid()));
        if (!allowPassthrough && this.mIsCompatEnabled && !this.mActivityRecord.isInTransition()) {
            this.mInputWindowHandleWrapper.setInputConfigMasked(0, 8);
        } else {
            this.mInputWindowHandleWrapper.setInputConfigMasked(8, 8);
        }
        return this.mInputWindowHandleWrapper;
    }

    private InputWindowHandle createInputWindowHandle() {
        InputWindowHandle inputWindowHandle = new InputWindowHandle((InputApplicationHandle) null, this.mActivityRecord.getDisplayId());
        inputWindowHandle.replaceTouchableRegionWithCrop = true;
        inputWindowHandle.name = this.mName;
        inputWindowHandle.layoutParamsType = 2022;
        inputWindowHandle.ownerUid = Process.myUid();
        inputWindowHandle.ownerPid = Process.myPid();
        inputWindowHandle.inputConfig = 5;
        return inputWindowHandle;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void releaseSurfaceControl() {
        SurfaceControl surfaceControl = this.mSurfaceControl;
        if (surfaceControl != null) {
            surfaceControl.release();
            this.mSurfaceControl = null;
        }
    }
}
