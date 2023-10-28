package com.android.server.usb.descriptors;

import android.hardware.usb.UsbConfiguration;
import android.hardware.usb.UsbInterface;
import com.android.server.usb.descriptors.report.ReportCanvas;
import java.util.ArrayList;
import java.util.Iterator;
/* loaded from: classes2.dex */
public final class UsbConfigDescriptor extends UsbDescriptor {
    private static final String TAG = "UsbConfigDescriptor";
    private int mAttribs;
    private boolean mBlockAudio;
    private byte mConfigIndex;
    private int mConfigValue;
    private ArrayList<UsbInterfaceDescriptor> mInterfaceDescriptors;
    private int mMaxPower;
    private byte mNumInterfaces;
    private int mTotalLength;

    /* JADX INFO: Access modifiers changed from: package-private */
    public UsbConfigDescriptor(int length, byte type) {
        super(length, type);
        this.mInterfaceDescriptors = new ArrayList<>();
        this.mHierarchyLevel = 2;
    }

    public int getTotalLength() {
        return this.mTotalLength;
    }

    public byte getNumInterfaces() {
        return this.mNumInterfaces;
    }

    public int getConfigValue() {
        return this.mConfigValue;
    }

    public byte getConfigIndex() {
        return this.mConfigIndex;
    }

    public int getAttribs() {
        return this.mAttribs;
    }

    public int getMaxPower() {
        return this.mMaxPower;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void addInterfaceDescriptor(UsbInterfaceDescriptor interfaceDesc) {
        this.mInterfaceDescriptors.add(interfaceDesc);
    }

    private boolean isAudioInterface(UsbInterfaceDescriptor descriptor) {
        return descriptor.getUsbClass() == 1 && descriptor.getUsbSubclass() == 2;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public UsbConfiguration toAndroid(UsbDescriptorParser parser) {
        String name = parser.getDescriptorString(this.mConfigIndex);
        UsbConfiguration config = new UsbConfiguration(this.mConfigValue, name, this.mAttribs, this.mMaxPower);
        ArrayList<UsbInterface> filteredInterfaces = new ArrayList<>();
        Iterator<UsbInterfaceDescriptor> it = this.mInterfaceDescriptors.iterator();
        while (it.hasNext()) {
            UsbInterfaceDescriptor descriptor = it.next();
            if (!this.mBlockAudio || !isAudioInterface(descriptor)) {
                filteredInterfaces.add(descriptor.toAndroid(parser));
            }
        }
        UsbInterface[] interfaceArray = new UsbInterface[0];
        config.setInterfaces((UsbInterface[]) filteredInterfaces.toArray(interfaceArray));
        return config;
    }

    @Override // com.android.server.usb.descriptors.UsbDescriptor
    public int parseRawDescriptors(ByteStream stream) {
        this.mTotalLength = stream.unpackUsbShort();
        this.mNumInterfaces = stream.getByte();
        this.mConfigValue = stream.getUnsignedByte();
        this.mConfigIndex = stream.getByte();
        this.mAttribs = stream.getUnsignedByte();
        this.mMaxPower = stream.getUnsignedByte();
        return this.mLength;
    }

    @Override // com.android.server.usb.descriptors.UsbDescriptor, com.android.server.usb.descriptors.report.Reporting
    public void report(ReportCanvas canvas) {
        super.report(canvas);
        canvas.openList();
        canvas.writeListItem("Config # " + getConfigValue());
        canvas.writeListItem(((int) getNumInterfaces()) + " Interfaces.");
        canvas.writeListItem("Attributes: " + ReportCanvas.getHexString(getAttribs()));
        canvas.closeList();
    }
}
