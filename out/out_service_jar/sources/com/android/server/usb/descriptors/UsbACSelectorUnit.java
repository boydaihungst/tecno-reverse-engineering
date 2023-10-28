package com.android.server.usb.descriptors;
/* loaded from: classes2.dex */
public final class UsbACSelectorUnit extends UsbACInterface {
    private static final String TAG = "UsbACSelectorUnit";
    private byte mNameIndex;
    private byte mNumPins;
    private byte[] mSourceIDs;
    private byte mUnitID;

    public UsbACSelectorUnit(int length, byte type, byte subtype, int subClass) {
        super(length, type, subtype, subClass);
    }

    public byte getUnitID() {
        return this.mUnitID;
    }

    public byte getNumPins() {
        return this.mNumPins;
    }

    public byte[] getSourceIDs() {
        return this.mSourceIDs;
    }

    public byte getNameIndex() {
        return this.mNameIndex;
    }

    /* JADX DEBUG: Multi-variable search result rejected for r0v1, resolved type: int */
    /* JADX WARN: Multi-variable type inference failed */
    @Override // com.android.server.usb.descriptors.UsbDescriptor
    public int parseRawDescriptors(ByteStream stream) {
        this.mUnitID = stream.getByte();
        int i = stream.getByte();
        this.mNumPins = i;
        this.mSourceIDs = new byte[i];
        for (int index = 0; index < this.mNumPins; index++) {
            this.mSourceIDs[index] = stream.getByte();
        }
        this.mNameIndex = stream.getByte();
        return this.mLength;
    }
}
