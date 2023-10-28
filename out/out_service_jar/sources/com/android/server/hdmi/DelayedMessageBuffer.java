package com.android.server.hdmi;

import android.hardware.hdmi.HdmiDeviceInfo;
import java.util.ArrayList;
import java.util.Iterator;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public final class DelayedMessageBuffer {
    private final ArrayList<HdmiCecMessage> mBuffer = new ArrayList<>();
    private final HdmiCecLocalDevice mDevice;

    /* JADX INFO: Access modifiers changed from: package-private */
    public DelayedMessageBuffer(HdmiCecLocalDevice device) {
        this.mDevice = device;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void add(HdmiCecMessage message) {
        boolean buffered = true;
        switch (message.getOpcode()) {
            case 114:
            case 192:
                this.mBuffer.add(message);
                break;
            case 130:
                removeActiveSource();
                this.mBuffer.add(message);
                break;
            default:
                buffered = false;
                break;
        }
        if (buffered) {
            HdmiLogger.debug("Buffering message:" + message, new Object[0]);
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void removeActiveSource() {
        Iterator<HdmiCecMessage> iter = this.mBuffer.iterator();
        while (iter.hasNext()) {
            HdmiCecMessage message = iter.next();
            if (message.getOpcode() == 130) {
                iter.remove();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isBuffered(int opcode) {
        Iterator<HdmiCecMessage> it = this.mBuffer.iterator();
        while (it.hasNext()) {
            HdmiCecMessage message = it.next();
            if (message.getOpcode() == opcode) {
                return true;
            }
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void processAllMessages() {
        ArrayList<HdmiCecMessage> copiedBuffer = new ArrayList<>(this.mBuffer);
        this.mBuffer.clear();
        Iterator<HdmiCecMessage> it = copiedBuffer.iterator();
        while (it.hasNext()) {
            HdmiCecMessage message = it.next();
            this.mDevice.onMessage(message);
            HdmiLogger.debug("Processing message:" + message, new Object[0]);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void processMessagesForDevice(int address) {
        ArrayList<HdmiCecMessage> copiedBuffer = new ArrayList<>(this.mBuffer);
        this.mBuffer.clear();
        HdmiLogger.debug("Checking message for address:" + address, new Object[0]);
        Iterator<HdmiCecMessage> it = copiedBuffer.iterator();
        while (it.hasNext()) {
            HdmiCecMessage message = it.next();
            if (message.getSource() != address) {
                this.mBuffer.add(message);
            } else if (message.getOpcode() == 130 && !this.mDevice.isInputReady(HdmiDeviceInfo.idForCecDevice(address))) {
                this.mBuffer.add(message);
            } else {
                this.mDevice.onMessage(message);
                HdmiLogger.debug("Processing message:" + message, new Object[0]);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void processActiveSource(int address) {
        ArrayList<HdmiCecMessage> copiedBuffer = new ArrayList<>(this.mBuffer);
        this.mBuffer.clear();
        Iterator<HdmiCecMessage> it = copiedBuffer.iterator();
        while (it.hasNext()) {
            HdmiCecMessage message = it.next();
            if (message.getOpcode() == 130 && message.getSource() == address) {
                this.mDevice.onMessage(message);
                HdmiLogger.debug("Processing message:" + message, new Object[0]);
            } else {
                this.mBuffer.add(message);
            }
        }
    }
}
