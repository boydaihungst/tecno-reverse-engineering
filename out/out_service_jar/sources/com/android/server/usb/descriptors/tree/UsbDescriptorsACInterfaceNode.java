package com.android.server.usb.descriptors.tree;

import com.android.server.usb.descriptors.UsbACInterface;
import com.android.server.usb.descriptors.report.ReportCanvas;
/* loaded from: classes2.dex */
public final class UsbDescriptorsACInterfaceNode extends UsbDescriptorsTreeNode {
    private static final String TAG = "UsbDescriptorsACInterfaceNode";
    private final UsbACInterface mACInterface;

    public UsbDescriptorsACInterfaceNode(UsbACInterface acInterface) {
        this.mACInterface = acInterface;
    }

    @Override // com.android.server.usb.descriptors.tree.UsbDescriptorsTreeNode, com.android.server.usb.descriptors.report.Reporting
    public void report(ReportCanvas canvas) {
        canvas.writeListItem("AC Interface type: 0x" + Integer.toHexString(this.mACInterface.getSubtype()));
        canvas.openList();
        this.mACInterface.report(canvas);
        canvas.closeList();
    }
}
