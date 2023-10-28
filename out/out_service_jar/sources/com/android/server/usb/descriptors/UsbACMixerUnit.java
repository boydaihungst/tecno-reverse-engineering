package com.android.server.usb.descriptors;
/* loaded from: classes2.dex */
public class UsbACMixerUnit extends UsbACInterface {
    private static final String TAG = "UsbACMixerUnit";
    protected byte[] mInputIDs;
    protected byte mNumInputs;
    protected byte mNumOutputs;
    protected byte mUnitID;

    public UsbACMixerUnit(int length, byte type, byte subtype, int subClass) {
        super(length, type, subtype, subClass);
    }

    public byte getUnitID() {
        return this.mUnitID;
    }

    public byte getNumInputs() {
        return this.mNumInputs;
    }

    public byte[] getInputIDs() {
        return this.mInputIDs;
    }

    public byte getNumOutputs() {
        return this.mNumOutputs;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public static int calcControlArraySize(int numInputs, int numOutputs) {
        int totalChannels = numInputs * numOutputs;
        return (totalChannels + 7) / 8;
    }

    /* JADX DEBUG: Multi-variable search result rejected for r0v1, resolved type: int */
    /* JADX WARN: Multi-variable type inference failed */
    @Override // com.android.server.usb.descriptors.UsbDescriptor
    public int parseRawDescriptors(ByteStream stream) {
        this.mUnitID = stream.getByte();
        int i = stream.getByte();
        this.mNumInputs = i;
        this.mInputIDs = new byte[i];
        for (int input = 0; input < this.mNumInputs; input++) {
            this.mInputIDs[input] = stream.getByte();
        }
        this.mNumOutputs = stream.getByte();
        return this.mLength;
    }
}
