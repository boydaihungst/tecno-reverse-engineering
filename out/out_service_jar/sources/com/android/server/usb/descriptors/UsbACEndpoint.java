package com.android.server.usb.descriptors;

import android.util.Log;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public abstract class UsbACEndpoint extends UsbDescriptor {
    public static final byte MS_GENERAL = 1;
    public static final byte MS_GENERAL_2_0 = 2;
    private static final String TAG = "UsbACEndpoint";
    protected final int mSubclass;
    protected final byte mSubtype;

    /* JADX INFO: Access modifiers changed from: package-private */
    public UsbACEndpoint(int length, byte type, int subclass, byte subtype) {
        super(length, type);
        this.mSubclass = subclass;
        this.mSubtype = subtype;
    }

    public int getSubclass() {
        return this.mSubclass;
    }

    public byte getSubtype() {
        return this.mSubtype;
    }

    @Override // com.android.server.usb.descriptors.UsbDescriptor
    public int parseRawDescriptors(ByteStream stream) {
        return this.mLength;
    }

    public static UsbDescriptor allocDescriptor(UsbDescriptorParser parser, int length, byte type, byte subType) {
        UsbInterfaceDescriptor interfaceDesc = parser.getCurInterface();
        int subClass = interfaceDesc.getUsbSubclass();
        switch (subClass) {
            case 1:
                return new UsbACAudioControlEndpoint(length, type, subClass, subType);
            case 2:
                return new UsbACAudioStreamEndpoint(length, type, subClass, subType);
            case 3:
                switch (subType) {
                    case 1:
                        return new UsbACMidi10Endpoint(length, type, subClass, subType);
                    case 2:
                        return new UsbACMidi20Endpoint(length, type, subClass, subType);
                    default:
                        Log.w(TAG, "Unknown Midi Endpoint id:0x" + Integer.toHexString(subType));
                        return null;
                }
            default:
                Log.w(TAG, "Unknown Audio Class Endpoint id:0x" + Integer.toHexString(subClass));
                return null;
        }
    }
}
