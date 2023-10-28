package com.android.server.usb.descriptors.tree;

import com.android.server.usb.descriptors.UsbACInterface;
import com.android.server.usb.descriptors.UsbConfigDescriptor;
import com.android.server.usb.descriptors.UsbDescriptor;
import com.android.server.usb.descriptors.UsbDescriptorParser;
import com.android.server.usb.descriptors.UsbDeviceDescriptor;
import com.android.server.usb.descriptors.UsbEndpointDescriptor;
import com.android.server.usb.descriptors.UsbInterfaceDescriptor;
import com.android.server.usb.descriptors.report.ReportCanvas;
import java.util.ArrayList;
/* loaded from: classes2.dex */
public final class UsbDescriptorsTree {
    private static final String TAG = "UsbDescriptorsTree";
    private UsbDescriptorsConfigNode mConfigNode;
    private UsbDescriptorsDeviceNode mDeviceNode;
    private UsbDescriptorsInterfaceNode mInterfaceNode;

    private void addDeviceDescriptor(UsbDeviceDescriptor deviceDescriptor) {
        this.mDeviceNode = new UsbDescriptorsDeviceNode(deviceDescriptor);
    }

    private void addConfigDescriptor(UsbConfigDescriptor configDescriptor) {
        UsbDescriptorsConfigNode usbDescriptorsConfigNode = new UsbDescriptorsConfigNode(configDescriptor);
        this.mConfigNode = usbDescriptorsConfigNode;
        this.mDeviceNode.addConfigDescriptorNode(usbDescriptorsConfigNode);
    }

    private void addInterfaceDescriptor(UsbInterfaceDescriptor interfaceDescriptor) {
        UsbDescriptorsInterfaceNode usbDescriptorsInterfaceNode = new UsbDescriptorsInterfaceNode(interfaceDescriptor);
        this.mInterfaceNode = usbDescriptorsInterfaceNode;
        this.mConfigNode.addInterfaceNode(usbDescriptorsInterfaceNode);
    }

    private void addEndpointDescriptor(UsbEndpointDescriptor endpointDescriptor) {
        this.mInterfaceNode.addEndpointNode(new UsbDescriptorsEndpointNode(endpointDescriptor));
    }

    private void addACInterface(UsbACInterface acInterface) {
        this.mInterfaceNode.addACInterfaceNode(new UsbDescriptorsACInterfaceNode(acInterface));
    }

    public void parse(UsbDescriptorParser parser) {
        ArrayList<UsbDescriptor> descriptors = parser.getDescriptors();
        for (int descrIndex = 0; descrIndex < descriptors.size(); descrIndex++) {
            UsbDescriptor descriptor = descriptors.get(descrIndex);
            switch (descriptor.getType()) {
                case 1:
                    addDeviceDescriptor((UsbDeviceDescriptor) descriptor);
                    break;
                case 2:
                    addConfigDescriptor((UsbConfigDescriptor) descriptor);
                    break;
                case 4:
                    addInterfaceDescriptor((UsbInterfaceDescriptor) descriptor);
                    break;
                case 5:
                    addEndpointDescriptor((UsbEndpointDescriptor) descriptor);
                    break;
            }
        }
    }

    public void report(ReportCanvas canvas) {
        this.mDeviceNode.report(canvas);
    }
}
