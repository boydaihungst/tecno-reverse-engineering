package com.android.server.usb.descriptors.tree;

import com.android.server.usb.descriptors.UsbDeviceDescriptor;
import com.android.server.usb.descriptors.report.ReportCanvas;
import java.util.ArrayList;
import java.util.Iterator;
/* loaded from: classes2.dex */
public final class UsbDescriptorsDeviceNode extends UsbDescriptorsTreeNode {
    private static final String TAG = "UsbDescriptorsDeviceNode";
    private final ArrayList<UsbDescriptorsConfigNode> mConfigNodes = new ArrayList<>();
    private final UsbDeviceDescriptor mDeviceDescriptor;

    public UsbDescriptorsDeviceNode(UsbDeviceDescriptor deviceDescriptor) {
        this.mDeviceDescriptor = deviceDescriptor;
    }

    public void addConfigDescriptorNode(UsbDescriptorsConfigNode configNode) {
        this.mConfigNodes.add(configNode);
    }

    @Override // com.android.server.usb.descriptors.tree.UsbDescriptorsTreeNode, com.android.server.usb.descriptors.report.Reporting
    public void report(ReportCanvas canvas) {
        this.mDeviceDescriptor.report(canvas);
        Iterator<UsbDescriptorsConfigNode> it = this.mConfigNodes.iterator();
        while (it.hasNext()) {
            UsbDescriptorsConfigNode node = it.next();
            node.report(canvas);
        }
    }
}
