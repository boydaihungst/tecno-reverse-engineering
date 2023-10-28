package com.android.server.usb.descriptors;

import com.android.server.usb.descriptors.report.ReportCanvas;
/* loaded from: classes2.dex */
public final class UsbACMidi10Endpoint extends UsbACEndpoint {
    private static final String TAG = "UsbACMidi10Endpoint";
    private byte[] mJackIds;
    private byte mNumJacks;

    @Override // com.android.server.usb.descriptors.UsbACEndpoint
    public /* bridge */ /* synthetic */ int getSubclass() {
        return super.getSubclass();
    }

    @Override // com.android.server.usb.descriptors.UsbACEndpoint
    public /* bridge */ /* synthetic */ byte getSubtype() {
        return super.getSubtype();
    }

    public UsbACMidi10Endpoint(int length, byte type, int subclass, byte subtype) {
        super(length, type, subclass, subtype);
        this.mJackIds = new byte[0];
    }

    public byte getNumJacks() {
        return this.mNumJacks;
    }

    public byte[] getJackIds() {
        return this.mJackIds;
    }

    /* JADX DEBUG: Multi-variable search result rejected for r0v0, resolved type: int */
    /* JADX WARN: Multi-variable type inference failed */
    @Override // com.android.server.usb.descriptors.UsbACEndpoint, com.android.server.usb.descriptors.UsbDescriptor
    public int parseRawDescriptors(ByteStream stream) {
        super.parseRawDescriptors(stream);
        int i = stream.getByte();
        this.mNumJacks = i;
        if (i > 0) {
            this.mJackIds = new byte[i];
            for (int jack = 0; jack < this.mNumJacks; jack++) {
                this.mJackIds[jack] = stream.getByte();
            }
        }
        int jack2 = this.mLength;
        return jack2;
    }

    @Override // com.android.server.usb.descriptors.UsbDescriptor, com.android.server.usb.descriptors.report.Reporting
    public void report(ReportCanvas canvas) {
        super.report(canvas);
        canvas.writeHeader(3, "ACMidi10Endpoint: " + ReportCanvas.getHexString(getType()) + " Length: " + getLength());
        canvas.openList();
        canvas.writeListItem("" + ((int) getNumJacks()) + " Jacks.");
        canvas.closeList();
    }
}
