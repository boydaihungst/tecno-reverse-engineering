package com.android.server.usb.descriptors;

import com.android.server.usb.descriptors.report.ReportCanvas;
/* loaded from: classes2.dex */
public final class UsbVCHeader extends UsbVCHeaderInterface {
    private static final String TAG = "UsbVCHeader";

    public UsbVCHeader(int length, byte type, byte subtype, int spec) {
        super(length, type, subtype, spec);
    }

    @Override // com.android.server.usb.descriptors.UsbDescriptor
    public int parseRawDescriptors(ByteStream stream) {
        return super.parseRawDescriptors(stream);
    }

    @Override // com.android.server.usb.descriptors.UsbVCHeaderInterface, com.android.server.usb.descriptors.UsbDescriptor, com.android.server.usb.descriptors.report.Reporting
    public void report(ReportCanvas canvas) {
        super.report(canvas);
    }
}
