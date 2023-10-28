package com.android.server.usb;

import java.io.ByteArrayOutputStream;
/* loaded from: classes2.dex */
public class UsbMidiPacketConverter {
    private static final byte CODE_INDEX_NUMBER_SINGLE_BYTE = 15;
    private static final byte CODE_INDEX_NUMBER_SYSEX_END_SINGLE_BYTE = 5;
    private static final byte CODE_INDEX_NUMBER_SYSEX_STARTS_OR_CONTINUES = 4;
    private static final byte FIRST_SYSTEM_MESSAGE_VALUE = -16;
    private static final byte SYSEX_END_EXCLUSIVE = -9;
    private static final byte SYSEX_START_EXCLUSIVE = -16;
    private UsbMidiDecoder mUsbMidiDecoder = new UsbMidiDecoder();
    private UsbMidiEncoder[] mUsbMidiEncoders;
    private static final int[] PAYLOAD_SIZE = {-1, -1, 2, 3, 3, 1, 2, 3, 3, 3, 3, 3, 2, 2, 3, 1};
    private static final int[] CODE_INDEX_NUMBER_FROM_SYSTEM_TYPE = {-1, 2, 3, 2, -1, -1, 5, -1, 5, -1, 5, 5, 5, -1, 5, 5};

    public UsbMidiPacketConverter(int numEncoders) {
        this.mUsbMidiEncoders = new UsbMidiEncoder[numEncoders];
        for (int i = 0; i < numEncoders; i++) {
            this.mUsbMidiEncoders[i] = new UsbMidiEncoder();
        }
    }

    public byte[] usbMidiToRawMidi(byte[] usbMidiBytes, int size) {
        return this.mUsbMidiDecoder.decode(usbMidiBytes, size);
    }

    public byte[] rawMidiToUsbMidi(byte[] midiBytes, int size, int encoderId) {
        return this.mUsbMidiEncoders[encoderId].encode(midiBytes, size);
    }

    /* loaded from: classes2.dex */
    private class UsbMidiDecoder {
        private UsbMidiDecoder() {
        }

        public byte[] decode(byte[] usbMidiBytes, int size) {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            for (int i = 0; i + 3 < size; i += 4) {
                int codeIndex = usbMidiBytes[i] & 15;
                int numPayloadBytes = UsbMidiPacketConverter.PAYLOAD_SIZE[codeIndex];
                if (numPayloadBytes >= 0) {
                    outputStream.write(usbMidiBytes, i + 1, numPayloadBytes);
                }
            }
            return outputStream.toByteArray();
        }
    }

    /* loaded from: classes2.dex */
    private class UsbMidiEncoder {
        private byte[] mEmptyBytes;
        private boolean mHasSystemExclusiveStarted;
        private int mNumStoredSystemExclusiveBytes;
        private byte[] mStoredSystemExclusiveBytes;

        private UsbMidiEncoder() {
            this.mStoredSystemExclusiveBytes = new byte[3];
            this.mNumStoredSystemExclusiveBytes = 0;
            this.mHasSystemExclusiveStarted = false;
            this.mEmptyBytes = new byte[3];
        }

        public byte[] encode(byte[] midiBytes, int size) {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            int curLocation = 0;
            while (curLocation < size) {
                if (midiBytes[curLocation] >= 0) {
                    if (this.mHasSystemExclusiveStarted) {
                        byte[] bArr = this.mStoredSystemExclusiveBytes;
                        int i = this.mNumStoredSystemExclusiveBytes;
                        bArr[i] = midiBytes[curLocation];
                        int i2 = i + 1;
                        this.mNumStoredSystemExclusiveBytes = i2;
                        if (i2 == 3) {
                            outputStream.write(4);
                            outputStream.write(this.mStoredSystemExclusiveBytes, 0, 3);
                            this.mNumStoredSystemExclusiveBytes = 0;
                        }
                    } else {
                        writeSingleByte(outputStream, midiBytes[curLocation]);
                    }
                    curLocation++;
                } else {
                    if (midiBytes[curLocation] != -9 && this.mHasSystemExclusiveStarted) {
                        for (int index = 0; index < this.mNumStoredSystemExclusiveBytes; index++) {
                            writeSingleByte(outputStream, this.mStoredSystemExclusiveBytes[index]);
                        }
                        this.mNumStoredSystemExclusiveBytes = 0;
                        this.mHasSystemExclusiveStarted = false;
                    }
                    int index2 = midiBytes[curLocation];
                    if (index2 < -16) {
                        byte codeIndexNumber = (byte) ((midiBytes[curLocation] >> 4) & 15);
                        int channelMessageSize = UsbMidiPacketConverter.PAYLOAD_SIZE[codeIndexNumber];
                        if (curLocation + channelMessageSize <= size) {
                            outputStream.write(codeIndexNumber);
                            outputStream.write(midiBytes, curLocation, channelMessageSize);
                            outputStream.write(this.mEmptyBytes, 0, 3 - channelMessageSize);
                            curLocation += channelMessageSize;
                        } else {
                            while (curLocation < size) {
                                writeSingleByte(outputStream, midiBytes[curLocation]);
                                curLocation++;
                            }
                        }
                    } else if (midiBytes[curLocation] == -16) {
                        this.mHasSystemExclusiveStarted = true;
                        this.mStoredSystemExclusiveBytes[0] = midiBytes[curLocation];
                        this.mNumStoredSystemExclusiveBytes = 1;
                        curLocation++;
                    } else if (midiBytes[curLocation] == -9) {
                        outputStream.write(this.mNumStoredSystemExclusiveBytes + 5);
                        byte[] bArr2 = this.mStoredSystemExclusiveBytes;
                        int i3 = this.mNumStoredSystemExclusiveBytes;
                        bArr2[i3] = midiBytes[curLocation];
                        int i4 = i3 + 1;
                        this.mNumStoredSystemExclusiveBytes = i4;
                        outputStream.write(bArr2, 0, i4);
                        outputStream.write(this.mEmptyBytes, 0, 3 - this.mNumStoredSystemExclusiveBytes);
                        this.mHasSystemExclusiveStarted = false;
                        this.mNumStoredSystemExclusiveBytes = 0;
                        curLocation++;
                    } else {
                        int systemType = midiBytes[curLocation] & 15;
                        int codeIndexNumber2 = UsbMidiPacketConverter.CODE_INDEX_NUMBER_FROM_SYSTEM_TYPE[systemType];
                        if (codeIndexNumber2 < 0) {
                            writeSingleByte(outputStream, midiBytes[curLocation]);
                            curLocation++;
                        } else {
                            int systemMessageSize = UsbMidiPacketConverter.PAYLOAD_SIZE[codeIndexNumber2];
                            if (curLocation + systemMessageSize <= size) {
                                outputStream.write(codeIndexNumber2);
                                outputStream.write(midiBytes, curLocation, systemMessageSize);
                                outputStream.write(this.mEmptyBytes, 0, 3 - systemMessageSize);
                                curLocation += systemMessageSize;
                            } else {
                                while (curLocation < size) {
                                    writeSingleByte(outputStream, midiBytes[curLocation]);
                                    curLocation++;
                                }
                            }
                        }
                    }
                }
            }
            return outputStream.toByteArray();
        }

        private void writeSingleByte(ByteArrayOutputStream outputStream, byte byteToWrite) {
            outputStream.write(15);
            outputStream.write(byteToWrite);
            outputStream.write(0);
            outputStream.write(0);
        }
    }
}
