package com.android.server.usb.descriptors;

import com.android.server.usb.descriptors.report.ReportCanvas;
/* loaded from: classes2.dex */
public final class UsbACMidi20Endpoint extends UsbACEndpoint {
    private static final String TAG = "UsbACMidi20Endpoint";
    private byte[] mBlockIds;
    private byte mNumGroupTerminals;

    @Override // com.android.server.usb.descriptors.UsbACEndpoint
    public /* bridge */ /* synthetic */ int getSubclass() {
        return super.getSubclass();
    }

    @Override // com.android.server.usb.descriptors.UsbACEndpoint
    public /* bridge */ /* synthetic */ byte getSubtype() {
        return super.getSubtype();
    }

    public UsbACMidi20Endpoint(int length, byte type, int subclass, byte subtype) {
        super(length, type, subclass, subtype);
        this.mBlockIds = new byte[0];
    }

    public byte getNumGroupTerminals() {
        return this.mNumGroupTerminals;
    }

    public byte[] getBlockIds() {
        return this.mBlockIds;
    }

    /* JADX DEBUG: Multi-variable search result rejected for r0v0, resolved type: int */
    /* JADX WARN: Multi-variable type inference failed */
    @Override // com.android.server.usb.descriptors.UsbACEndpoint, com.android.server.usb.descriptors.UsbDescriptor
    public int parseRawDescriptors(ByteStream stream) {
        super.parseRawDescriptors(stream);
        int i = stream.getByte();
        this.mNumGroupTerminals = i;
        if (i > 0) {
            this.mBlockIds = new byte[i];
            for (int block = 0; block < this.mNumGroupTerminals; block++) {
                this.mBlockIds[block] = stream.getByte();
            }
        }
        int block2 = this.mLength;
        return block2;
    }

    @Override // com.android.server.usb.descriptors.UsbDescriptor, com.android.server.usb.descriptors.report.Reporting
    public void report(ReportCanvas canvas) {
        super.report(canvas);
        canvas.writeHeader(3, "AC Midi20 Endpoint: " + ReportCanvas.getHexString(getType()) + " Length: " + getLength());
        canvas.openList();
        canvas.writeListItem("" + ((int) getNumGroupTerminals()) + " Group Terminals.");
        canvas.closeList();
    }
}
