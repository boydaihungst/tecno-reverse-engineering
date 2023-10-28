package com.android.server.wm;

import android.view.ContentRecordingSession;
import com.android.internal.protolog.ProtoLogGroup;
import com.android.internal.protolog.ProtoLogImpl;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public final class ContentRecordingController {
    private ContentRecordingSession mSession = null;
    private DisplayContent mDisplayContent = null;

    ContentRecordingSession getContentRecordingSessionLocked() {
        return this.mSession;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setContentRecordingSessionLocked(ContentRecordingSession incomingSession, WindowManagerService wmService) {
        if (incomingSession != null && (!ContentRecordingSession.isValid(incomingSession) || ContentRecordingSession.isSameDisplay(this.mSession, incomingSession))) {
            return;
        }
        DisplayContent incomingDisplayContent = null;
        if (incomingSession != null) {
            if (ProtoLogCache.WM_DEBUG_CONTENT_RECORDING_enabled) {
                long protoLogParam0 = incomingSession.getDisplayId();
                ContentRecordingSession contentRecordingSession = this.mSession;
                String protoLogParam1 = String.valueOf(contentRecordingSession == null ? null : Integer.valueOf(contentRecordingSession.getDisplayId()));
                ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_CONTENT_RECORDING, 1401287081, 1, "Handle incoming session on display %d, with a pre-existing session %s", new Object[]{Long.valueOf(protoLogParam0), protoLogParam1});
            }
            incomingDisplayContent = wmService.mRoot.getDisplayContentOrCreate(incomingSession.getDisplayId());
            incomingDisplayContent.setContentRecordingSession(incomingSession);
            incomingDisplayContent.updateRecording();
        }
        if (this.mSession != null) {
            if (ProtoLogCache.WM_DEBUG_CONTENT_RECORDING_enabled) {
                String protoLogParam02 = String.valueOf(this.mDisplayContent.getDisplayId());
                ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_CONTENT_RECORDING, -237664290, 0, "Pause the recording session on display %s", new Object[]{protoLogParam02});
            }
            this.mDisplayContent.pauseRecording();
            this.mDisplayContent.setContentRecordingSession(null);
        }
        this.mDisplayContent = incomingDisplayContent;
        this.mSession = incomingSession;
    }
}
