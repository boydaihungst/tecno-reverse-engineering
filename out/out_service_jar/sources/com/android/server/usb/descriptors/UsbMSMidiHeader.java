package com.android.server.usb.descriptors;

import com.android.server.usb.descriptors.report.ReportCanvas;
/* loaded from: classes2.dex */
public final class UsbMSMidiHeader extends UsbACInterface {
    private static final String TAG = "UsbMSMidiHeader";
    private int mMidiStreamingClass;

    public UsbMSMidiHeader(int length, byte type, byte subtype, int subclass) {
        super(length, type, subtype, subclass);
    }

    public int getMidiStreamingClass() {
        return this.mMidiStreamingClass;
    }

    @Override // com.android.server.usb.descriptors.UsbDescriptor
    public int parseRawDescriptors(ByteStream stream) {
        this.mMidiStreamingClass = stream.unpackUsbShort();
        stream.advance(this.mLength - stream.getReadCount());
        return this.mLength;
    }

    @Override // com.android.server.usb.descriptors.UsbACInterface, com.android.server.usb.descriptors.UsbDescriptor, com.android.server.usb.descriptors.report.Reporting
    public void report(ReportCanvas canvas) {
        super.report(canvas);
        canvas.writeHeader(3, "MS Midi Header: " + ReportCanvas.getHexString(getType()) + " SubType: " + ReportCanvas.getHexString(getSubclass()) + " Length: " + getLength() + " MidiStreamingClass :" + getMidiStreamingClass());
    }
}
