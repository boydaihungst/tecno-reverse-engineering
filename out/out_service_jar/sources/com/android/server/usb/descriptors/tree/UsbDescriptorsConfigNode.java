package com.android.server.usb.descriptors.tree;

import com.android.server.usb.descriptors.UsbConfigDescriptor;
import com.android.server.usb.descriptors.report.ReportCanvas;
import java.util.ArrayList;
import java.util.Iterator;
/* loaded from: classes2.dex */
public final class UsbDescriptorsConfigNode extends UsbDescriptorsTreeNode {
    private static final String TAG = "UsbDescriptorsConfigNode";
    private final UsbConfigDescriptor mConfigDescriptor;
    private final ArrayList<UsbDescriptorsInterfaceNode> mInterfaceNodes = new ArrayList<>();

    public UsbDescriptorsConfigNode(UsbConfigDescriptor configDescriptor) {
        this.mConfigDescriptor = configDescriptor;
    }

    public void addInterfaceNode(UsbDescriptorsInterfaceNode interfaceNode) {
        this.mInterfaceNodes.add(interfaceNode);
    }

    @Override // com.android.server.usb.descriptors.tree.UsbDescriptorsTreeNode, com.android.server.usb.descriptors.report.Reporting
    public void report(ReportCanvas canvas) {
        this.mConfigDescriptor.report(canvas);
        canvas.openList();
        Iterator<UsbDescriptorsInterfaceNode> it = this.mInterfaceNodes.iterator();
        while (it.hasNext()) {
            UsbDescriptorsInterfaceNode node = it.next();
            node.report(canvas);
        }
        canvas.closeList();
    }
}
