package com.android.server.usb.descriptors;

import com.android.server.usb.descriptors.report.ReportCanvas;
/* loaded from: classes2.dex */
public final class UsbVCProcessingUnit extends UsbVCInterface {
    private static final String TAG = "UsbVCProcessingUnit";

    public UsbVCProcessingUnit(int length, byte type, byte subtype) {
        super(length, type, subtype);
    }

    @Override // com.android.server.usb.descriptors.UsbDescriptor
    public int parseRawDescriptors(ByteStream stream) {
        return super.parseRawDescriptors(stream);
    }

    @Override // com.android.server.usb.descriptors.UsbDescriptor, com.android.server.usb.descriptors.report.Reporting
    public void report(ReportCanvas canvas) {
        super.report(canvas);
    }
}
