package com.android.server.usb.descriptors;

import android.util.Log;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public abstract class UsbVCEndpoint extends UsbDescriptor {
    private static final String TAG = "UsbVCEndpoint";
    public static final byte VCEP_ENDPOINT = 2;
    public static final byte VCEP_GENERAL = 1;
    public static final byte VCEP_INTERRUPT = 3;
    public static final byte VCEP_UNDEFINED = 0;

    UsbVCEndpoint(int length, byte type) {
        super(length, type);
    }

    public static UsbDescriptor allocDescriptor(UsbDescriptorParser parser, int length, byte type, byte subtype) {
        parser.getCurInterface();
        switch (subtype) {
            case 0:
                return null;
            case 1:
                return null;
            case 2:
                return null;
            case 3:
                return null;
            default:
                Log.w(TAG, "Unknown Video Class Endpoint id:0x" + Integer.toHexString(subtype));
                return null;
        }
    }
}
