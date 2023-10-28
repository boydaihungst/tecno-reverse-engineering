package com.android.server.usb.descriptors;

import com.android.server.usb.descriptors.report.ReportCanvas;
import com.android.server.usb.descriptors.report.UsbStrings;
/* loaded from: classes2.dex */
public class UsbASFormat extends UsbACInterface {
    public static final byte EXT_FORMAT_TYPE_I = -127;
    public static final byte EXT_FORMAT_TYPE_II = -126;
    public static final byte EXT_FORMAT_TYPE_III = -125;
    public static final byte FORMAT_TYPE_I = 1;
    public static final byte FORMAT_TYPE_II = 2;
    public static final byte FORMAT_TYPE_III = 3;
    public static final byte FORMAT_TYPE_IV = 4;
    private static final String TAG = "UsbASFormat";
    private final byte mFormatType;

    public UsbASFormat(int length, byte type, byte subtype, byte formatType, int mSubclass) {
        super(length, type, subtype, mSubclass);
        this.mFormatType = formatType;
    }

    public byte getFormatType() {
        return this.mFormatType;
    }

    public int[] getSampleRates() {
        return null;
    }

    public int[] getBitDepths() {
        return null;
    }

    public int[] getChannelCounts() {
        return null;
    }

    public static UsbDescriptor allocDescriptor(UsbDescriptorParser parser, ByteStream stream, int length, byte type, byte subtype, int subclass) {
        byte formatType = stream.getByte();
        int acInterfaceSpec = parser.getACInterfaceSpec();
        switch (formatType) {
            case 1:
                if (acInterfaceSpec == 512) {
                    return new Usb20ASFormatI(length, type, subtype, formatType, subclass);
                }
                return new Usb10ASFormatI(length, type, subtype, formatType, subclass);
            case 2:
                if (acInterfaceSpec == 512) {
                    return new Usb20ASFormatII(length, type, subtype, formatType, subclass);
                }
                return new Usb10ASFormatII(length, type, subtype, formatType, subclass);
            case 3:
                return new Usb20ASFormatIII(length, type, subtype, formatType, subclass);
            default:
                return new UsbASFormat(length, type, subtype, formatType, subclass);
        }
    }

    @Override // com.android.server.usb.descriptors.UsbACInterface, com.android.server.usb.descriptors.UsbDescriptor, com.android.server.usb.descriptors.report.Reporting
    public void report(ReportCanvas canvas) {
        super.report(canvas);
        canvas.writeParagraph(UsbStrings.getFormatName(getFormatType()), false);
    }
}
