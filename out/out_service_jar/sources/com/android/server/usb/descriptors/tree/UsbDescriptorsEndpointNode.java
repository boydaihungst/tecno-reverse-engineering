package com.android.server.usb.descriptors.tree;

import com.android.server.usb.descriptors.UsbEndpointDescriptor;
import com.android.server.usb.descriptors.report.ReportCanvas;
/* loaded from: classes2.dex */
public final class UsbDescriptorsEndpointNode extends UsbDescriptorsTreeNode {
    private static final String TAG = "UsbDescriptorsEndpointNode";
    private final UsbEndpointDescriptor mEndpointDescriptor;

    public UsbDescriptorsEndpointNode(UsbEndpointDescriptor endpointDescriptor) {
        this.mEndpointDescriptor = endpointDescriptor;
    }

    @Override // com.android.server.usb.descriptors.tree.UsbDescriptorsTreeNode, com.android.server.usb.descriptors.report.Reporting
    public void report(ReportCanvas canvas) {
        this.mEndpointDescriptor.report(canvas);
    }
}
