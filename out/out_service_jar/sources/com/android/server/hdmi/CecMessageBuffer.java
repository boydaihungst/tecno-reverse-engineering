package com.android.server.hdmi;

import java.util.ArrayList;
import java.util.List;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public final class CecMessageBuffer {
    private List<HdmiCecMessage> mBuffer = new ArrayList();
    private HdmiControlService mHdmiControlService;

    /* JADX INFO: Access modifiers changed from: package-private */
    public CecMessageBuffer(HdmiControlService hdmiControlService) {
        this.mHdmiControlService = hdmiControlService;
    }

    public boolean bufferMessage(HdmiCecMessage message) {
        switch (message.getOpcode()) {
            case 4:
            case 13:
                bufferImageOrTextViewOn(message);
                return true;
            case 112:
                bufferSystemAudioModeRequest(message);
                return true;
            case 130:
                bufferActiveSource(message);
                return true;
            default:
                return false;
        }
    }

    public void processMessages() {
        for (final HdmiCecMessage message : this.mBuffer) {
            this.mHdmiControlService.runOnServiceThread(new Runnable() { // from class: com.android.server.hdmi.CecMessageBuffer.1
                @Override // java.lang.Runnable
                public void run() {
                    CecMessageBuffer.this.mHdmiControlService.handleCecCommand(message);
                }
            });
        }
        this.mBuffer.clear();
    }

    private void bufferActiveSource(HdmiCecMessage message) {
        if (!replaceMessageIfBuffered(message, 130)) {
            this.mBuffer.add(message);
        }
    }

    private void bufferImageOrTextViewOn(HdmiCecMessage message) {
        if (!replaceMessageIfBuffered(message, 4) && !replaceMessageIfBuffered(message, 13)) {
            this.mBuffer.add(message);
        }
    }

    private void bufferSystemAudioModeRequest(HdmiCecMessage message) {
        if (!replaceMessageIfBuffered(message, 112)) {
            this.mBuffer.add(message);
        }
    }

    private boolean replaceMessageIfBuffered(HdmiCecMessage message, int opcode) {
        for (int i = 0; i < this.mBuffer.size(); i++) {
            HdmiCecMessage bufferedMessage = this.mBuffer.get(i);
            if (bufferedMessage.getOpcode() == opcode) {
                this.mBuffer.set(i, message);
                return true;
            }
        }
        return false;
    }
}
