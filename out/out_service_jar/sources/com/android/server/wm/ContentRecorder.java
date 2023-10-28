package com.android.server.wm;

import android.graphics.Point;
import android.graphics.Rect;
import android.os.IBinder;
import android.provider.DeviceConfig;
import android.view.ContentRecordingSession;
import android.view.SurfaceControl;
import com.android.internal.protolog.ProtoLogGroup;
import com.android.internal.protolog.ProtoLogImpl;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public final class ContentRecorder {
    static final String KEY_RECORD_TASK_FEATURE = "record_task_content";
    private final DisplayContent mDisplayContent;
    private ContentRecordingSession mContentRecordingSession = null;
    private WindowContainer mRecordedWindowContainer = null;
    private SurfaceControl mRecordedSurface = null;
    private Rect mLastRecordedBounds = null;

    /* JADX INFO: Access modifiers changed from: package-private */
    public ContentRecorder(DisplayContent displayContent) {
        this.mDisplayContent = displayContent;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setContentRecordingSession(ContentRecordingSession session) {
        this.mContentRecordingSession = session;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isCurrentlyRecording() {
        return (this.mContentRecordingSession == null || this.mRecordedSurface == null) ? false : true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateRecording() {
        if (isCurrentlyRecording() && (this.mDisplayContent.getLastHasContent() || this.mDisplayContent.getDisplay().getState() == 1)) {
            pauseRecording();
        } else {
            startRecordingIfNeeded();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onConfigurationChanged(int lastOrientation) {
        if (isCurrentlyRecording() && this.mLastRecordedBounds != null) {
            if (this.mRecordedWindowContainer == null) {
                if (ProtoLogCache.WM_DEBUG_CONTENT_RECORDING_enabled) {
                    long protoLogParam0 = this.mDisplayContent.getDisplayId();
                    ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_CONTENT_RECORDING, 1444064727, 1, "Unexpectedly null window container; unable to update recording for display %d", new Object[]{Long.valueOf(protoLogParam0)});
                    return;
                }
                return;
            }
            if (ProtoLogCache.WM_DEBUG_CONTENT_RECORDING_enabled) {
                long protoLogParam02 = this.mDisplayContent.getDisplayId();
                ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_CONTENT_RECORDING, -302468137, 1, "Display %d was already recording, so apply transformations if necessary", new Object[]{Long.valueOf(protoLogParam02)});
            }
            Rect recordedContentBounds = this.mRecordedWindowContainer.getBounds();
            int recordedContentOrientation = this.mRecordedWindowContainer.getOrientation();
            if (this.mLastRecordedBounds.equals(recordedContentBounds) && lastOrientation == recordedContentOrientation) {
                return;
            }
            Point surfaceSize = fetchSurfaceSizeIfPresent();
            if (surfaceSize != null) {
                if (ProtoLogCache.WM_DEBUG_CONTENT_RECORDING_enabled) {
                    long protoLogParam03 = this.mDisplayContent.getDisplayId();
                    String protoLogParam1 = String.valueOf(recordedContentBounds);
                    long protoLogParam2 = recordedContentOrientation;
                    ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_CONTENT_RECORDING, -1373875178, 17, "Going ahead with updating recording for display %d to new bounds %s and/or orientation %d.", new Object[]{Long.valueOf(protoLogParam03), protoLogParam1, Long.valueOf(protoLogParam2)});
                }
                updateMirroredSurface(this.mDisplayContent.mWmService.mTransactionFactory.get(), recordedContentBounds, surfaceSize);
            } else if (ProtoLogCache.WM_DEBUG_CONTENT_RECORDING_enabled) {
                long protoLogParam04 = this.mDisplayContent.getDisplayId();
                String protoLogParam12 = String.valueOf(recordedContentBounds);
                long protoLogParam22 = recordedContentOrientation;
                ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_CONTENT_RECORDING, -751255162, 17, "Unable to update recording for display %d to new bounds %s and/or orientation %d, since the surface is not available.", new Object[]{Long.valueOf(protoLogParam04), protoLogParam12, Long.valueOf(protoLogParam22)});
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void pauseRecording() {
        if (this.mRecordedSurface == null) {
            return;
        }
        if (ProtoLogCache.WM_DEBUG_CONTENT_RECORDING_enabled) {
            long protoLogParam0 = this.mDisplayContent.getDisplayId();
            boolean protoLogParam1 = this.mDisplayContent.getLastHasContent();
            ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_CONTENT_RECORDING, -1781861035, 13, "Display %d has content (%b) so pause recording", new Object[]{Long.valueOf(protoLogParam0), Boolean.valueOf(protoLogParam1)});
        }
        this.mDisplayContent.mWmService.mTransactionFactory.get().remove(this.mRecordedSurface).reparent(this.mDisplayContent.getWindowingLayer(), this.mDisplayContent.getSurfaceControl()).reparent(this.mDisplayContent.getOverlayLayer(), this.mDisplayContent.getSurfaceControl()).apply();
        this.mRecordedSurface = null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void remove() {
        if (this.mRecordedSurface != null) {
            this.mDisplayContent.mWmService.mTransactionFactory.get().remove(this.mRecordedSurface).apply();
            this.mRecordedSurface = null;
            clearContentRecordingSession();
        }
    }

    private void clearContentRecordingSession() {
        this.mContentRecordingSession = null;
        this.mDisplayContent.mWmService.mContentRecordingController.setContentRecordingSessionLocked(null, this.mDisplayContent.mWmService);
    }

    private void startRecordingIfNeeded() {
        if (this.mDisplayContent.getLastHasContent() || isCurrentlyRecording() || this.mDisplayContent.getDisplay().getState() == 1 || this.mContentRecordingSession == null) {
            return;
        }
        WindowContainer retrieveRecordedWindowContainer = retrieveRecordedWindowContainer();
        this.mRecordedWindowContainer = retrieveRecordedWindowContainer;
        if (retrieveRecordedWindowContainer == null) {
            return;
        }
        Point surfaceSize = fetchSurfaceSizeIfPresent();
        if (surfaceSize == null) {
            if (ProtoLogCache.WM_DEBUG_CONTENT_RECORDING_enabled) {
                long protoLogParam0 = this.mDisplayContent.getDisplayId();
                ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_CONTENT_RECORDING, -142844021, 1, "Unable to start recording for display %d since the surface is not available.", new Object[]{Long.valueOf(protoLogParam0)});
                return;
            }
            return;
        }
        if (ProtoLogCache.WM_DEBUG_CONTENT_RECORDING_enabled) {
            long protoLogParam02 = this.mDisplayContent.getDisplayId();
            long protoLogParam1 = this.mDisplayContent.getDisplay().getState();
            ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_CONTENT_RECORDING, 609880497, 5, "Display %d has no content and is on, so start recording for state %d", new Object[]{Long.valueOf(protoLogParam02), Long.valueOf(protoLogParam1)});
        }
        this.mRecordedSurface = SurfaceControl.mirrorSurface(this.mRecordedWindowContainer.getSurfaceControl());
        SurfaceControl.Transaction transaction = this.mDisplayContent.mWmService.mTransactionFactory.get().reparent(this.mRecordedSurface, this.mDisplayContent.getSurfaceControl()).reparent(this.mDisplayContent.getWindowingLayer(), null).reparent(this.mDisplayContent.getOverlayLayer(), null);
        updateMirroredSurface(transaction, this.mRecordedWindowContainer.getBounds(), surfaceSize);
    }

    private WindowContainer retrieveRecordedWindowContainer() {
        int contentToRecord = this.mContentRecordingSession.getContentToRecord();
        IBinder tokenToRecord = this.mContentRecordingSession.getTokenToRecord();
        if (tokenToRecord == null) {
            handleStartRecordingFailed();
            if (ProtoLogCache.WM_DEBUG_CONTENT_RECORDING_enabled) {
                long protoLogParam0 = this.mDisplayContent.getDisplayId();
                ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_CONTENT_RECORDING, -1605829532, 1, "Unable to start recording due to null token for display %d", new Object[]{Long.valueOf(protoLogParam0)});
            }
            return null;
        }
        switch (contentToRecord) {
            case 0:
                WindowContainer wc = this.mDisplayContent.mWmService.mWindowContextListenerController.getContainer(tokenToRecord);
                if (wc == null) {
                    this.mDisplayContent.mWmService.mDisplayManagerInternal.setWindowManagerMirroring(this.mDisplayContent.getDisplayId(), false);
                    handleStartRecordingFailed();
                    if (ProtoLogCache.WM_DEBUG_CONTENT_RECORDING_enabled) {
                        long protoLogParam02 = this.mDisplayContent.getDisplayId();
                        ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_CONTENT_RECORDING, -732715767, 1, "Unable to retrieve window container to start recording for display %d", new Object[]{Long.valueOf(protoLogParam02)});
                    }
                    return null;
                }
                return wc.getDisplayContent();
            case 1:
                if (!DeviceConfig.getBoolean("window_manager", KEY_RECORD_TASK_FEATURE, false)) {
                    handleStartRecordingFailed();
                    if (ProtoLogCache.WM_DEBUG_CONTENT_RECORDING_enabled) {
                        long protoLogParam03 = this.mDisplayContent.getDisplayId();
                        ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_CONTENT_RECORDING, 778774915, 1, "Unable to record task since feature is disabled %d", new Object[]{Long.valueOf(protoLogParam03)});
                    }
                    return null;
                }
                Task taskToRecord = WindowContainer.fromBinder(tokenToRecord).asTask();
                if (taskToRecord == null) {
                    handleStartRecordingFailed();
                    if (ProtoLogCache.WM_DEBUG_CONTENT_RECORDING_enabled) {
                        long protoLogParam04 = this.mDisplayContent.getDisplayId();
                        ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_CONTENT_RECORDING, 264036181, 1, "Unable to retrieve task to start recording for display %d", new Object[]{Long.valueOf(protoLogParam04)});
                    }
                }
                return taskToRecord;
            default:
                handleStartRecordingFailed();
                if (ProtoLogCache.WM_DEBUG_CONTENT_RECORDING_enabled) {
                    long protoLogParam05 = this.mDisplayContent.getDisplayId();
                    ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_CONTENT_RECORDING, 1608402305, 1, "Unable to start recording due to invalid region for display %d", new Object[]{Long.valueOf(protoLogParam05)});
                }
                return null;
        }
    }

    private void handleStartRecordingFailed() {
        ContentRecordingSession contentRecordingSession = this.mContentRecordingSession;
        boolean z = true;
        boolean shouldExitTaskRecording = (contentRecordingSession == null || contentRecordingSession.getContentToRecord() != 1) ? false : false;
        if (shouldExitTaskRecording) {
            clearContentRecordingSession();
            tearDownVirtualDisplay();
            return;
        }
        clearContentRecordingSession();
    }

    private void tearDownVirtualDisplay() {
    }

    void updateMirroredSurface(SurfaceControl.Transaction transaction, Rect recordedContentBounds, Point surfaceSize) {
        int shiftedX;
        int shiftedY;
        float scaleX = surfaceSize.x / recordedContentBounds.width();
        float scaleY = surfaceSize.y / recordedContentBounds.height();
        float scale = Math.min(scaleX, scaleY);
        int scaledWidth = Math.round(recordedContentBounds.width() * scale);
        int scaledHeight = Math.round(recordedContentBounds.height() * scale);
        if (scaledWidth == surfaceSize.x) {
            shiftedX = 0;
        } else {
            int shiftedX2 = (surfaceSize.x - scaledWidth) / 2;
            shiftedX = shiftedX2;
        }
        if (scaledHeight == surfaceSize.y) {
            shiftedY = 0;
        } else {
            int shiftedY2 = (surfaceSize.y - scaledHeight) / 2;
            shiftedY = shiftedY2;
        }
        transaction.setWindowCrop(this.mRecordedSurface, recordedContentBounds.width(), recordedContentBounds.height()).setMatrix(this.mRecordedSurface, scale, 0.0f, 0.0f, scale).setPosition(this.mRecordedSurface, shiftedX, shiftedY).apply();
        this.mLastRecordedBounds = new Rect(recordedContentBounds);
    }

    private Point fetchSurfaceSizeIfPresent() {
        Point surfaceSize = this.mDisplayContent.mWmService.mDisplayManagerInternal.getDisplaySurfaceDefaultSize(this.mDisplayContent.getDisplayId());
        if (surfaceSize == null) {
            if (ProtoLogCache.WM_DEBUG_CONTENT_RECORDING_enabled) {
                long protoLogParam0 = this.mDisplayContent.getDisplayId();
                ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_CONTENT_RECORDING, -1326876381, 1, "Provided surface for recording on display %d is not present, so do not update the surface", new Object[]{Long.valueOf(protoLogParam0)});
                return null;
            }
            return null;
        }
        return surfaceSize;
    }
}
