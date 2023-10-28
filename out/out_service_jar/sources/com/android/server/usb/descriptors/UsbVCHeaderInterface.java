package com.android.server.usb.descriptors;

import com.android.server.usb.descriptors.report.ReportCanvas;
/* loaded from: classes2.dex */
public abstract class UsbVCHeaderInterface extends UsbVCInterface {
    private static final String TAG = "UsbVCHeaderInterface";
    protected int mTotalLength;
    protected int mVDCRelease;

    public UsbVCHeaderInterface(int length, byte type, byte subtype, int vdcRelease) {
        super(length, type, subtype);
        this.mVDCRelease = vdcRelease;
    }

    public int getVDCRelease() {
        return this.mVDCRelease;
    }

    public int getTotalLength() {
        return this.mTotalLength;
    }

    @Override // com.android.server.usb.descriptors.UsbDescriptor, com.android.server.usb.descriptors.report.Reporting
    public void report(ReportCanvas canvas) {
        super.report(canvas);
        canvas.openList();
        canvas.writeListItem("Release: " + ReportCanvas.getBCDString(getVDCRelease()));
        canvas.writeListItem("Total Length: " + getTotalLength());
        canvas.closeList();
    }
}
