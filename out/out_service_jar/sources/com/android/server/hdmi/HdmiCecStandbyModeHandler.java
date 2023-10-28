package com.android.server.hdmi;

import android.util.SparseArray;
/* loaded from: classes.dex */
public final class HdmiCecStandbyModeHandler {
    private final CecMessageHandler mAborterIncorrectMode;
    private final CecMessageHandler mAborterRefused;
    private final CecMessageHandler mAborterUnrecognizedOpcode;
    private final CecMessageHandler mAutoOnHandler;
    private final CecMessageHandler mBypasser;
    private final CecMessageHandler mBystander;
    private final SparseArray<CecMessageHandler> mCecMessageHandlers = new SparseArray<>();
    private final CecMessageHandler mDefaultHandler;
    private final HdmiCecLocalDevice mDevice;
    private final HdmiControlService mService;
    private final UserControlProcessedHandler mUserControlProcessedHandler;

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public interface CecMessageHandler {
        boolean handle(HdmiCecMessage hdmiCecMessage);
    }

    /* loaded from: classes.dex */
    private static final class Bystander implements CecMessageHandler {
        private Bystander() {
        }

        @Override // com.android.server.hdmi.HdmiCecStandbyModeHandler.CecMessageHandler
        public boolean handle(HdmiCecMessage message) {
            return true;
        }
    }

    /* loaded from: classes.dex */
    private static final class Bypasser implements CecMessageHandler {
        private Bypasser() {
        }

        @Override // com.android.server.hdmi.HdmiCecStandbyModeHandler.CecMessageHandler
        public boolean handle(HdmiCecMessage message) {
            return false;
        }
    }

    /* loaded from: classes.dex */
    private final class Aborter implements CecMessageHandler {
        private final int mReason;

        public Aborter(int reason) {
            this.mReason = reason;
        }

        @Override // com.android.server.hdmi.HdmiCecStandbyModeHandler.CecMessageHandler
        public boolean handle(HdmiCecMessage message) {
            HdmiCecStandbyModeHandler.this.mService.maySendFeatureAbortCommand(message, this.mReason);
            return true;
        }
    }

    /* loaded from: classes.dex */
    private final class AutoOnHandler implements CecMessageHandler {
        private AutoOnHandler() {
        }

        @Override // com.android.server.hdmi.HdmiCecStandbyModeHandler.CecMessageHandler
        public boolean handle(HdmiCecMessage message) {
            HdmiCecLocalDeviceTv tv = (HdmiCecLocalDeviceTv) HdmiCecStandbyModeHandler.this.mDevice;
            if (!tv.getAutoWakeup()) {
                HdmiCecStandbyModeHandler.this.mAborterRefused.handle(message);
                return true;
            }
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class UserControlProcessedHandler implements CecMessageHandler {
        private UserControlProcessedHandler() {
        }

        @Override // com.android.server.hdmi.HdmiCecStandbyModeHandler.CecMessageHandler
        public boolean handle(HdmiCecMessage message) {
            if (HdmiCecLocalDevice.isPowerOnOrToggleCommand(message)) {
                return false;
            }
            if (HdmiCecLocalDevice.isPowerOffOrToggleCommand(message)) {
                return true;
            }
            return HdmiCecStandbyModeHandler.this.mAborterIncorrectMode.handle(message);
        }
    }

    private void addCommonHandlers() {
        addHandler(68, this.mUserControlProcessedHandler);
    }

    private void addTvHandlers() {
        addHandler(130, this.mBystander);
        addHandler(133, this.mBystander);
        addHandler(128, this.mBystander);
        addHandler(129, this.mBystander);
        addHandler(134, this.mBystander);
        addHandler(54, this.mBystander);
        addHandler(50, this.mBystander);
        addHandler(69, this.mBystander);
        addHandler(0, this.mBystander);
        addHandler(157, this.mBystander);
        addHandler(126, this.mBystander);
        addHandler(122, this.mBystander);
        addHandler(131, this.mBypasser);
        addHandler(145, this.mBypasser);
        addHandler(132, this.mBypasser);
        addHandler(140, this.mBypasser);
        addHandler(70, this.mBypasser);
        addHandler(71, this.mBypasser);
        addHandler(135, this.mBypasser);
        addHandler(144, this.mBypasser);
        addHandler(165, this.mBypasser);
        addHandler(143, this.mBypasser);
        addHandler(255, this.mBypasser);
        addHandler(159, this.mBypasser);
        addHandler(160, this.mAborterIncorrectMode);
        addHandler(114, this.mAborterIncorrectMode);
        addHandler(4, this.mAutoOnHandler);
        addHandler(13, this.mAutoOnHandler);
        addHandler(10, this.mBystander);
        addHandler(15, this.mAborterIncorrectMode);
        addHandler(192, this.mAborterIncorrectMode);
        addHandler(197, this.mAborterIncorrectMode);
    }

    public HdmiCecStandbyModeHandler(HdmiControlService service, HdmiCecLocalDevice device) {
        Aborter aborter = new Aborter(0);
        this.mAborterUnrecognizedOpcode = aborter;
        this.mAborterIncorrectMode = new Aborter(1);
        this.mAborterRefused = new Aborter(4);
        this.mAutoOnHandler = new AutoOnHandler();
        Bypasser bypasser = new Bypasser();
        this.mBypasser = bypasser;
        this.mBystander = new Bystander();
        this.mUserControlProcessedHandler = new UserControlProcessedHandler();
        this.mService = service;
        this.mDevice = device;
        addCommonHandlers();
        if (device.getType() == 0) {
            addTvHandlers();
            this.mDefaultHandler = aborter;
            return;
        }
        this.mDefaultHandler = bypasser;
    }

    private void addHandler(int opcode, CecMessageHandler handler) {
        this.mCecMessageHandlers.put(opcode, handler);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean handleCommand(HdmiCecMessage message) {
        CecMessageHandler handler = this.mCecMessageHandlers.get(message.getOpcode());
        if (handler != null) {
            return handler.handle(message);
        }
        return this.mDefaultHandler.handle(message);
    }
}
