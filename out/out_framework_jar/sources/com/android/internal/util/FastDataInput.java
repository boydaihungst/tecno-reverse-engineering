package com.android.internal.util;

import android.util.CharsetUtils;
import dalvik.system.VMRuntime;
import java.io.Closeable;
import java.io.DataInput;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Objects;
/* loaded from: classes4.dex */
public class FastDataInput implements DataInput, Closeable {
    private static final int MAX_UNSIGNED_SHORT = 65535;
    private final byte[] mBuffer;
    private final int mBufferCap;
    private int mBufferLim;
    private int mBufferPos;
    private final long mBufferPtr;
    private final InputStream mIn;
    private final VMRuntime mRuntime;
    private int mStringRefCount = 0;
    private String[] mStringRefs = new String[32];

    public FastDataInput(InputStream in, int bufferSize) {
        VMRuntime runtime = VMRuntime.getRuntime();
        this.mRuntime = runtime;
        this.mIn = (InputStream) Objects.requireNonNull(in);
        if (bufferSize < 8) {
            throw new IllegalArgumentException();
        }
        byte[] bArr = (byte[]) runtime.newNonMovableArray(Byte.TYPE, bufferSize);
        this.mBuffer = bArr;
        this.mBufferPtr = runtime.addressOf(bArr);
        this.mBufferCap = bArr.length;
    }

    private void fill(int need) throws IOException {
        int i = this.mBufferLim;
        int i2 = this.mBufferPos;
        int remain = i - i2;
        byte[] bArr = this.mBuffer;
        System.arraycopy(bArr, i2, bArr, 0, remain);
        this.mBufferPos = 0;
        this.mBufferLim = remain;
        int need2 = need - remain;
        while (need2 > 0) {
            InputStream inputStream = this.mIn;
            byte[] bArr2 = this.mBuffer;
            int i3 = this.mBufferLim;
            int c = inputStream.read(bArr2, i3, this.mBufferCap - i3);
            if (c == -1) {
                throw new EOFException();
            }
            this.mBufferLim += c;
            need2 -= c;
        }
    }

    @Override // java.io.Closeable, java.lang.AutoCloseable
    public void close() throws IOException {
        this.mIn.close();
    }

    @Override // java.io.DataInput
    public void readFully(byte[] b) throws IOException {
        readFully(b, 0, b.length);
    }

    @Override // java.io.DataInput
    public void readFully(byte[] b, int off, int len) throws IOException {
        if (this.mBufferCap >= len) {
            if (this.mBufferLim - this.mBufferPos < len) {
                fill(len);
            }
            System.arraycopy(this.mBuffer, this.mBufferPos, b, off, len);
            this.mBufferPos += len;
            return;
        }
        int i = this.mBufferLim;
        int i2 = this.mBufferPos;
        int remain = i - i2;
        System.arraycopy(this.mBuffer, i2, b, off, remain);
        this.mBufferPos += remain;
        int off2 = off + remain;
        int len2 = len - remain;
        while (len2 > 0) {
            int c = this.mIn.read(b, off2, len2);
            if (c == -1) {
                throw new EOFException();
            }
            off2 += c;
            len2 -= c;
        }
    }

    @Override // java.io.DataInput
    public String readUTF() throws IOException {
        int len = readUnsignedShort();
        if (this.mBufferCap > len) {
            if (this.mBufferLim - this.mBufferPos < len) {
                fill(len);
            }
            String res = CharsetUtils.fromModifiedUtf8Bytes(this.mBufferPtr, this.mBufferPos, len);
            this.mBufferPos += len;
            return res;
        }
        byte[] tmp = (byte[]) this.mRuntime.newNonMovableArray(Byte.TYPE, len + 1);
        readFully(tmp, 0, len);
        return CharsetUtils.fromModifiedUtf8Bytes(this.mRuntime.addressOf(tmp), 0, len);
    }

    public String readInternedUTF() throws IOException {
        int ref = readUnsignedShort();
        if (ref == 65535) {
            String s = readUTF();
            int i = this.mStringRefCount;
            if (i < 65535) {
                String[] strArr = this.mStringRefs;
                if (i == strArr.length) {
                    this.mStringRefs = (String[]) Arrays.copyOf(strArr, i + (i >> 1));
                }
                String[] strArr2 = this.mStringRefs;
                int i2 = this.mStringRefCount;
                this.mStringRefCount = i2 + 1;
                strArr2[i2] = s;
            }
            return s;
        }
        return this.mStringRefs[ref];
    }

    @Override // java.io.DataInput
    public boolean readBoolean() throws IOException {
        return readByte() != 0;
    }

    public byte peekByte() throws IOException {
        if (this.mBufferLim - this.mBufferPos < 1) {
            fill(1);
        }
        return this.mBuffer[this.mBufferPos];
    }

    @Override // java.io.DataInput
    public byte readByte() throws IOException {
        if (this.mBufferLim - this.mBufferPos < 1) {
            fill(1);
        }
        byte[] bArr = this.mBuffer;
        int i = this.mBufferPos;
        this.mBufferPos = i + 1;
        return bArr[i];
    }

    @Override // java.io.DataInput
    public int readUnsignedByte() throws IOException {
        return Byte.toUnsignedInt(readByte());
    }

    @Override // java.io.DataInput
    public short readShort() throws IOException {
        if (this.mBufferLim - this.mBufferPos < 2) {
            fill(2);
        }
        byte[] bArr = this.mBuffer;
        int i = this.mBufferPos;
        int i2 = i + 1;
        this.mBufferPos = i2;
        this.mBufferPos = i2 + 1;
        return (short) (((bArr[i2] & 255) << 0) | ((bArr[i] & 255) << 8));
    }

    @Override // java.io.DataInput
    public int readUnsignedShort() throws IOException {
        return Short.toUnsignedInt(readShort());
    }

    @Override // java.io.DataInput
    public char readChar() throws IOException {
        return (char) readShort();
    }

    @Override // java.io.DataInput
    public int readInt() throws IOException {
        if (this.mBufferLim - this.mBufferPos < 4) {
            fill(4);
        }
        byte[] bArr = this.mBuffer;
        int i = this.mBufferPos;
        int i2 = i + 1;
        this.mBufferPos = i2;
        int i3 = i2 + 1;
        this.mBufferPos = i3;
        int i4 = ((bArr[i] & 255) << 24) | ((bArr[i2] & 255) << 16);
        int i5 = i3 + 1;
        this.mBufferPos = i5;
        int i6 = i4 | ((bArr[i3] & 255) << 8);
        this.mBufferPos = i5 + 1;
        return ((bArr[i5] & 255) << 0) | i6;
    }

    @Override // java.io.DataInput
    public long readLong() throws IOException {
        if (this.mBufferLim - this.mBufferPos < 8) {
            fill(8);
        }
        byte[] bArr = this.mBuffer;
        int i = this.mBufferPos;
        int i2 = i + 1;
        this.mBufferPos = i2;
        int i3 = i2 + 1;
        this.mBufferPos = i3;
        int i4 = ((bArr[i] & 255) << 24) | ((bArr[i2] & 255) << 16);
        int i5 = i3 + 1;
        this.mBufferPos = i5;
        int i6 = i4 | ((bArr[i3] & 255) << 8);
        int i7 = i5 + 1;
        this.mBufferPos = i7;
        int h = i6 | ((bArr[i5] & 255) << 0);
        int i8 = i7 + 1;
        this.mBufferPos = i8;
        int i9 = i8 + 1;
        this.mBufferPos = i9;
        int i10 = ((bArr[i8] & 255) << 16) | ((bArr[i7] & 255) << 24);
        int i11 = i9 + 1;
        this.mBufferPos = i11;
        int i12 = ((bArr[i9] & 255) << 8) | i10;
        this.mBufferPos = i11 + 1;
        int l = ((bArr[i11] & 255) << 0) | i12;
        return (h << 32) | (l & 4294967295L);
    }

    @Override // java.io.DataInput
    public float readFloat() throws IOException {
        return Float.intBitsToFloat(readInt());
    }

    @Override // java.io.DataInput
    public double readDouble() throws IOException {
        return Double.longBitsToDouble(readLong());
    }

    @Override // java.io.DataInput
    public int skipBytes(int n) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override // java.io.DataInput
    public String readLine() throws IOException {
        throw new UnsupportedOperationException();
    }
}
