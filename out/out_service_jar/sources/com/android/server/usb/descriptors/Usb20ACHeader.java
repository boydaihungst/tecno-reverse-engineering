package com.android.server.usb.descriptors;

import com.android.server.usb.descriptors.report.ReportCanvas;
/* loaded from: classes2.dex */
public final class Usb20ACHeader extends UsbACHeaderInterface {
    private static final String TAG = "Usb20ACHeader";
    private byte mCategory;
    private byte mControls;

    public Usb20ACHeader(int length, byte type, byte subtype, int subclass, int spec) {
        super(length, type, subtype, subclass, spec);
    }

    public byte getCategory() {
        return this.mCategory;
    }

    public byte getControls() {
        return this.mControls;
    }

    @Override // com.android.server.usb.descriptors.UsbDescriptor
    public int parseRawDescriptors(ByteStream stream) {
        this.mCategory = stream.getByte();
        this.mTotalLength = stream.unpackUsbShort();
        this.mControls = stream.getByte();
        return this.mLength;
    }

    @Override // com.android.server.usb.descriptors.UsbACHeaderInterface, com.android.server.usb.descriptors.UsbACInterface, com.android.server.usb.descriptors.UsbDescriptor, com.android.server.usb.descriptors.report.Reporting
    public void report(ReportCanvas canvas) {
        super.report(canvas);
        canvas.openList();
        canvas.writeListItem("Category: " + ReportCanvas.getHexString(getCategory()));
        canvas.writeListItem("Controls: " + ReportCanvas.getHexString(getControls()));
        canvas.closeList();
    }
}
