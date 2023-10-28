package com.android.server.usb.descriptors.tree;

import com.android.server.usb.descriptors.UsbInterfaceDescriptor;
import com.android.server.usb.descriptors.report.ReportCanvas;
import java.util.ArrayList;
import java.util.Iterator;
/* loaded from: classes2.dex */
public final class UsbDescriptorsInterfaceNode extends UsbDescriptorsTreeNode {
    private static final String TAG = "UsbDescriptorsInterfaceNode";
    private final UsbInterfaceDescriptor mInterfaceDescriptor;
    private final ArrayList<UsbDescriptorsEndpointNode> mEndpointNodes = new ArrayList<>();
    private final ArrayList<UsbDescriptorsACInterfaceNode> mACInterfaceNodes = new ArrayList<>();

    public UsbDescriptorsInterfaceNode(UsbInterfaceDescriptor interfaceDescriptor) {
        this.mInterfaceDescriptor = interfaceDescriptor;
    }

    public void addEndpointNode(UsbDescriptorsEndpointNode endpointNode) {
        this.mEndpointNodes.add(endpointNode);
    }

    public void addACInterfaceNode(UsbDescriptorsACInterfaceNode acInterfaceNode) {
        this.mACInterfaceNodes.add(acInterfaceNode);
    }

    @Override // com.android.server.usb.descriptors.tree.UsbDescriptorsTreeNode, com.android.server.usb.descriptors.report.Reporting
    public void report(ReportCanvas canvas) {
        this.mInterfaceDescriptor.report(canvas);
        if (this.mACInterfaceNodes.size() > 0) {
            canvas.writeParagraph("Audio Class Interfaces", false);
            canvas.openList();
            Iterator<UsbDescriptorsACInterfaceNode> it = this.mACInterfaceNodes.iterator();
            while (it.hasNext()) {
                UsbDescriptorsACInterfaceNode node = it.next();
                node.report(canvas);
            }
            canvas.closeList();
        }
        if (this.mEndpointNodes.size() > 0) {
            canvas.writeParagraph("Endpoints", false);
            canvas.openList();
            Iterator<UsbDescriptorsEndpointNode> it2 = this.mEndpointNodes.iterator();
            while (it2.hasNext()) {
                UsbDescriptorsEndpointNode node2 = it2.next();
                node2.report(canvas);
            }
            canvas.closeList();
        }
    }
}
