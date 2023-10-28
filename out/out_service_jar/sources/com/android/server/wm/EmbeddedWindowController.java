package com.android.server.wm;

import android.os.IBinder;
import android.os.RemoteException;
import android.util.ArrayMap;
import android.util.Slog;
import android.util.proto.ProtoOutputStream;
import android.view.IWindow;
import android.view.InputApplicationHandle;
import android.view.InputChannel;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class EmbeddedWindowController {
    private static final String TAG = "WindowManager";
    private final ActivityTaskManagerService mAtmService;
    private final Object mGlobalLock;
    private ArrayMap<IBinder, EmbeddedWindow> mWindows = new ArrayMap<>();
    private ArrayMap<IBinder, EmbeddedWindow> mWindowsByFocusToken = new ArrayMap<>();
    private ArrayMap<IBinder, EmbeddedWindow> mWindowsByWindowToken = new ArrayMap<>();

    /* JADX INFO: Access modifiers changed from: package-private */
    public EmbeddedWindowController(ActivityTaskManagerService atmService) {
        this.mAtmService = atmService;
        this.mGlobalLock = atmService.getGlobalLock();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void add(final IBinder inputToken, EmbeddedWindow window) {
        try {
            this.mWindows.put(inputToken, window);
            final IBinder focusToken = window.getFocusGrantToken();
            this.mWindowsByFocusToken.put(focusToken, window);
            this.mWindowsByWindowToken.put(window.getWindowToken(), window);
            updateProcessController(window);
            window.mClient.asBinder().linkToDeath(new IBinder.DeathRecipient() { // from class: com.android.server.wm.EmbeddedWindowController$$ExternalSyntheticLambda0
                @Override // android.os.IBinder.DeathRecipient
                public final void binderDied() {
                    EmbeddedWindowController.this.m8011lambda$add$0$comandroidserverwmEmbeddedWindowController(inputToken, focusToken);
                }
            }, 0);
        } catch (RemoteException e) {
            this.mWindows.remove(inputToken);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$add$0$com-android-server-wm-EmbeddedWindowController  reason: not valid java name */
    public /* synthetic */ void m8011lambda$add$0$comandroidserverwmEmbeddedWindowController(IBinder inputToken, IBinder focusToken) {
        synchronized (this.mGlobalLock) {
            EmbeddedWindow ew = this.mWindows.remove(inputToken);
            this.mWindowsByFocusToken.remove(focusToken);
            if (ew != null && ew.mName != null && ew.mName.contains("ThunderBack_")) {
                ew.onRemoved();
                this.mWindowsByWindowToken.remove(ew.getWindowToken());
            }
        }
    }

    private void updateProcessController(EmbeddedWindow window) {
        if (window.mHostActivityRecord == null) {
            return;
        }
        WindowProcessController processController = this.mAtmService.getProcessController(window.mOwnerPid, window.mOwnerUid);
        if (processController == null) {
            Slog.w("WindowManager", "Could not find the embedding process.");
        } else {
            processController.addHostActivity(window.mHostActivityRecord);
        }
    }

    WindowState getHostWindow(IBinder inputToken) {
        EmbeddedWindow embeddedWindow = this.mWindows.get(inputToken);
        if (embeddedWindow != null) {
            return embeddedWindow.mHostWindowState;
        }
        return null;
    }

    boolean isOverlay(IBinder inputToken) {
        EmbeddedWindow embeddedWindow = this.mWindows.get(inputToken);
        if (embeddedWindow != null) {
            return embeddedWindow.getIsOverlay();
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setIsOverlay(IBinder focusGrantToken) {
        EmbeddedWindow embeddedWindow = this.mWindowsByFocusToken.get(focusGrantToken);
        if (embeddedWindow != null) {
            embeddedWindow.setIsOverlay();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void remove(IWindow client) {
        for (int i = this.mWindows.size() - 1; i >= 0; i--) {
            EmbeddedWindow ew = this.mWindows.valueAt(i);
            if (ew.mClient.asBinder() == client.asBinder()) {
                this.mWindows.removeAt(i).onRemoved();
                this.mWindowsByFocusToken.remove(ew.getFocusGrantToken());
                this.mWindowsByWindowToken.remove(ew.getWindowToken());
                return;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onWindowRemoved(WindowState host) {
        for (int i = this.mWindows.size() - 1; i >= 0; i--) {
            EmbeddedWindow ew = this.mWindows.valueAt(i);
            if (ew.mHostWindowState == host) {
                this.mWindows.removeAt(i).onRemoved();
                this.mWindowsByFocusToken.remove(ew.getFocusGrantToken());
                this.mWindowsByWindowToken.remove(ew.getWindowToken());
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public EmbeddedWindow get(IBinder inputToken) {
        return this.mWindows.get(inputToken);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public EmbeddedWindow getByFocusToken(IBinder focusGrantToken) {
        return this.mWindowsByFocusToken.get(focusGrantToken);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public EmbeddedWindow getByWindowToken(IBinder windowToken) {
        return this.mWindowsByWindowToken.get(windowToken);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onActivityRemoved(ActivityRecord activityRecord) {
        WindowProcessController processController;
        for (int i = this.mWindows.size() - 1; i >= 0; i--) {
            EmbeddedWindow window = this.mWindows.valueAt(i);
            if (window.mHostActivityRecord == activityRecord && (processController = this.mAtmService.getProcessController(window.mOwnerPid, window.mOwnerUid)) != null) {
                processController.removeHostActivity(activityRecord);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public static class EmbeddedWindow implements InputTarget {
        final IWindow mClient;
        final int mDisplayId;
        private IBinder mFocusGrantToken;
        final ActivityRecord mHostActivityRecord;
        final WindowState mHostWindowState;
        InputChannel mInputChannel;
        boolean mIsOverlay = false;
        final String mName;
        final int mOwnerPid;
        final int mOwnerUid;
        public Session mSession;
        final int mWindowType;
        final WindowManagerService mWmService;

        /* JADX INFO: Access modifiers changed from: package-private */
        public EmbeddedWindow(Session session, WindowManagerService service, IWindow clientToken, WindowState hostWindowState, int ownerUid, int ownerPid, int windowType, int displayId, IBinder focusGrantToken, String inputHandleName) {
            this.mSession = session;
            this.mWmService = service;
            this.mClient = clientToken;
            this.mHostWindowState = hostWindowState;
            this.mHostActivityRecord = hostWindowState != null ? hostWindowState.mActivityRecord : null;
            this.mOwnerUid = ownerUid;
            this.mOwnerPid = ownerPid;
            this.mWindowType = windowType;
            this.mDisplayId = displayId;
            this.mFocusGrantToken = focusGrantToken;
            String hostWindowName = hostWindowState != null ? "-" + hostWindowState.getWindowTag().toString() : "";
            this.mName = "Embedded{" + inputHandleName + hostWindowName + "}";
        }

        public String toString() {
            return this.mName;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public InputApplicationHandle getApplicationHandle() {
            WindowState windowState = this.mHostWindowState;
            if (windowState == null || windowState.mInputWindowHandle.getInputApplicationHandle() == null) {
                return null;
            }
            return new InputApplicationHandle(this.mHostWindowState.mInputWindowHandle.getInputApplicationHandle());
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public InputChannel openInputChannel() {
            String name = toString();
            InputChannel createInputChannel = this.mWmService.mInputManager.createInputChannel(name);
            this.mInputChannel = createInputChannel;
            return createInputChannel;
        }

        void onRemoved() {
            if (this.mInputChannel != null) {
                this.mWmService.mInputManager.removeInputChannel(this.mInputChannel.getToken());
                this.mInputChannel.dispose();
                this.mInputChannel = null;
            }
        }

        @Override // com.android.server.wm.InputTarget
        public WindowState getWindowState() {
            return this.mHostWindowState;
        }

        @Override // com.android.server.wm.InputTarget
        public int getDisplayId() {
            return this.mDisplayId;
        }

        @Override // com.android.server.wm.InputTarget
        public DisplayContent getDisplayContent() {
            return this.mWmService.mRoot.getDisplayContent(getDisplayId());
        }

        @Override // com.android.server.wm.InputTarget
        public IWindow getIWindow() {
            return this.mClient;
        }

        public IBinder getWindowToken() {
            return this.mClient.asBinder();
        }

        @Override // com.android.server.wm.InputTarget
        public int getPid() {
            return this.mOwnerPid;
        }

        @Override // com.android.server.wm.InputTarget
        public int getUid() {
            return this.mOwnerUid;
        }

        void setIsOverlay() {
            this.mIsOverlay = true;
        }

        boolean getIsOverlay() {
            return this.mIsOverlay;
        }

        IBinder getFocusGrantToken() {
            return this.mFocusGrantToken;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public IBinder getInputChannelToken() {
            InputChannel inputChannel = this.mInputChannel;
            if (inputChannel != null) {
                return inputChannel.getToken();
            }
            return null;
        }

        @Override // com.android.server.wm.InputTarget
        public boolean receiveFocusFromTapOutside() {
            return this.mIsOverlay;
        }

        private void handleTap(boolean grantFocus) {
            if (this.mInputChannel != null) {
                this.mWmService.grantEmbeddedWindowFocus(this.mSession, this.mFocusGrantToken, grantFocus);
            }
        }

        @Override // com.android.server.wm.InputTarget
        public void handleTapOutsideFocusOutsideSelf() {
            handleTap(false);
        }

        @Override // com.android.server.wm.InputTarget
        public void handleTapOutsideFocusInsideSelf() {
            handleTap(true);
        }

        @Override // com.android.server.wm.InputTarget
        public boolean shouldControlIme() {
            return false;
        }

        @Override // com.android.server.wm.InputTarget
        public boolean canScreenshotIme() {
            return true;
        }

        @Override // com.android.server.wm.InputTarget
        public void unfreezeInsetsAfterStartInput() {
        }

        @Override // com.android.server.wm.InputTarget
        public InsetsControlTarget getImeControlTarget() {
            return this.mWmService.getDefaultDisplayContentLocked().mRemoteInsetsControlTarget;
        }

        @Override // com.android.server.wm.InputTarget
        public boolean isInputMethodClientFocus(int uid, int pid) {
            return uid == this.mOwnerUid && pid == this.mOwnerPid;
        }

        @Override // com.android.server.wm.InputTarget
        public ActivityRecord getActivityRecord() {
            return null;
        }

        @Override // com.android.server.wm.InputTarget
        public void dumpProto(ProtoOutputStream proto, long fieldId, int logLevel) {
            long token = proto.start(fieldId);
            long token2 = proto.start(1146756268038L);
            proto.write(CompanionMessage.MESSAGE_ID, System.identityHashCode(this));
            proto.write(1138166333443L, "EmbeddedWindow");
            proto.end(token2);
            proto.end(token);
        }
    }
}
