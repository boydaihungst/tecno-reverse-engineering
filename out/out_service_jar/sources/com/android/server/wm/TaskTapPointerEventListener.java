package com.android.server.wm;

import android.graphics.Rect;
import android.graphics.Region;
import android.hardware.input.InputManager;
import android.view.MotionEvent;
import android.view.WindowManagerPolicyConstants;
import com.transsion.hubcore.server.wm.ITranWindowManagerService;
/* loaded from: classes2.dex */
public class TaskTapPointerEventListener implements WindowManagerPolicyConstants.PointerEventListener {
    private final DisplayContent mDisplayContent;
    private final WindowManagerService mService;
    private final Region mTouchExcludeRegion = new Region();
    private final Rect mTmpRect = new Rect();
    private int mPointerIconType = 1;

    public TaskTapPointerEventListener(WindowManagerService service, DisplayContent displayContent) {
        this.mService = service;
        this.mDisplayContent = displayContent;
    }

    private void restorePointerIcon(int x, int y) {
        if (this.mPointerIconType != 1) {
            this.mPointerIconType = 1;
            this.mService.mH.removeMessages(55);
            this.mService.mH.obtainMessage(55, x, y, this.mDisplayContent).sendToTarget();
        }
    }

    public void onPointerEvent(MotionEvent motionEvent) {
        int x;
        int y;
        ITranWindowManagerService.Instance().onTaskTapPointerEvent(this, motionEvent);
        switch (motionEvent.getActionMasked()) {
            case 0:
                if (motionEvent.getSource() == 8194) {
                    x = (int) motionEvent.getXCursorPosition();
                    y = (int) motionEvent.getYCursorPosition();
                } else {
                    x = (int) motionEvent.getX();
                    y = (int) motionEvent.getY();
                }
                synchronized (this) {
                    if (!this.mTouchExcludeRegion.contains(x, y)) {
                        this.mService.mTaskPositioningController.handleTapOutsideTask(this.mDisplayContent, x, y);
                    }
                }
                return;
            case 7:
            case 9:
                int x2 = (int) motionEvent.getX();
                int y2 = (int) motionEvent.getY();
                if (this.mTouchExcludeRegion.contains(x2, y2)) {
                    restorePointerIcon(x2, y2);
                    return;
                }
                Task task = this.mDisplayContent.findTaskForResizePoint(x2, y2);
                int iconType = 1;
                if (task != null) {
                    task.getDimBounds(this.mTmpRect);
                    if (!this.mTmpRect.isEmpty() && !this.mTmpRect.contains(x2, y2)) {
                        int i = 1014;
                        if (x2 < this.mTmpRect.left) {
                            if (y2 < this.mTmpRect.top) {
                                i = 1017;
                            } else if (y2 > this.mTmpRect.bottom) {
                                i = 1016;
                            }
                            iconType = i;
                        } else if (x2 > this.mTmpRect.right) {
                            if (y2 < this.mTmpRect.top) {
                                i = 1016;
                            } else if (y2 > this.mTmpRect.bottom) {
                                i = 1017;
                            }
                            iconType = i;
                        } else if (y2 < this.mTmpRect.top || y2 > this.mTmpRect.bottom) {
                            iconType = 1015;
                        }
                    }
                }
                if (this.mPointerIconType != iconType) {
                    this.mPointerIconType = iconType;
                    if (iconType == 1) {
                        this.mService.mH.removeMessages(55);
                        this.mService.mH.obtainMessage(55, x2, y2, this.mDisplayContent).sendToTarget();
                        return;
                    }
                    InputManager.getInstance().setPointerIconType(this.mPointerIconType);
                    return;
                }
                return;
            case 10:
                int x3 = (int) motionEvent.getX();
                int y3 = (int) motionEvent.getY();
                restorePointerIcon(x3, y3);
                return;
            default:
                return;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setTouchExcludeRegion(Region newRegion) {
        synchronized (this) {
            this.mTouchExcludeRegion.set(newRegion);
        }
    }
}
