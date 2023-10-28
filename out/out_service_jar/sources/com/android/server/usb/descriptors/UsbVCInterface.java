package com.android.server.usb.descriptors;

import android.util.Log;
/* loaded from: classes2.dex */
public abstract class UsbVCInterface extends UsbDescriptor {
    private static final String TAG = "UsbVCInterface";
    public static final byte VCI_EXTENSION_UNIT = 6;
    public static final byte VCI_INPUT_TERMINAL = 2;
    public static final byte VCI_OUTPUT_TERMINAL = 3;
    public static final byte VCI_PROCESSING_UNIT = 5;
    public static final byte VCI_SELECTOR_UNIT = 4;
    public static final byte VCI_UNDEFINED = 0;
    public static final byte VCI_VEADER = 1;
    protected final byte mSubtype;

    public UsbVCInterface(int length, byte type, byte subtype) {
        super(length, type);
        this.mSubtype = subtype;
    }

    public static UsbDescriptor allocDescriptor(UsbDescriptorParser parser, ByteStream stream, int length, byte type) {
        byte subtype = stream.getByte();
        parser.getCurInterface();
        switch (subtype) {
            case 0:
            case 6:
                return null;
            case 1:
                int vcInterfaceSpec = stream.unpackUsbShort();
                parser.setVCInterfaceSpec(vcInterfaceSpec);
                return new UsbVCHeader(length, type, subtype, vcInterfaceSpec);
            case 2:
                return new UsbVCInputTerminal(length, type, subtype);
            case 3:
                return new UsbVCOutputTerminal(length, type, subtype);
            case 4:
                return new UsbVCSelectorUnit(length, type, subtype);
            case 5:
                return new UsbVCProcessingUnit(length, type, subtype);
            default:
                Log.w(TAG, "Unknown Video Class Interface subtype: 0x" + Integer.toHexString(subtype));
                return null;
        }
    }
}
