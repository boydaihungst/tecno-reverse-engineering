package com.android.internal.util;

import android.util.CharsetUtils;
import dalvik.system.VMRuntime;
import java.io.Closeable;
import java.io.DataOutput;
import java.io.Flushable;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
/* loaded from: classes4.dex */
public class FastDataOutput implements DataOutput, Flushable, Closeable {
    private static final int BUFFER_SIZE = 32768;
    private static final int MAX_UNSIGNED_SHORT = 65535;
    private static AtomicReference<FastDataOutput> sOutCache = new AtomicReference<>();
    private final byte[] mBuffer;
    private final int mBufferCap;
    private int mBufferPos;
    private final long mBufferPtr;
    private OutputStream mOut;
    private final VMRuntime mRuntime;
    private final HashMap<String, Short> mStringRefs = new HashMap<>();

    public FastDataOutput(OutputStream out, int bufferSize) {
        VMRuntime runtime = VMRuntime.getRuntime();
        this.mRuntime = runtime;
        if (bufferSize < 8) {
            throw new IllegalArgumentException();
        }
        byte[] bArr = (byte[]) runtime.newNonMovableArray(Byte.TYPE, bufferSize);
        this.mBuffer = bArr;
        this.mBufferPtr = runtime.addressOf(bArr);
        this.mBufferCap = bArr.length;
        setOutput(out);
    }

    public static FastDataOutput obtain(OutputStream out) {
        FastDataOutput instance = sOutCache.getAndSet(null);
        if (instance != null) {
            instance.setOutput(out);
            return instance;
        }
        return new FastDataOutput(out, 32768);
    }

    public void release() {
        if (this.mBufferPos > 0) {
            throw new IllegalStateException("Lingering data, call flush() before releasing.");
        }
        this.mOut = null;
        this.mBufferPos = 0;
        this.mStringRefs.clear();
        if (this.mBufferCap == 32768) {
            sOutCache.compareAndSet(null, this);
        }
    }

    private void setOutput(OutputStream out) {
        this.mOut = (OutputStream) Objects.requireNonNull(out);
        this.mBufferPos = 0;
        this.mStringRefs.clear();
    }

    private void drain() throws IOException {
        int i = this.mBufferPos;
        if (i > 0) {
            this.mOut.write(this.mBuffer, 0, i);
            this.mBufferPos = 0;
        }
    }

    @Override // java.io.Flushable
    public void flush() throws IOException {
        drain();
        this.mOut.flush();
    }

    @Override // java.io.Closeable, java.lang.AutoCloseable
    public void close() throws IOException {
        this.mOut.close();
        release();
    }

    @Override // java.io.DataOutput
    public void write(int b) throws IOException {
        writeByte(b);
    }

    @Override // java.io.DataOutput
    public void write(byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    @Override // java.io.DataOutput
    public void write(byte[] b, int off, int len) throws IOException {
        int i = this.mBufferCap;
        if (i < len) {
            drain();
            this.mOut.write(b, off, len);
            return;
        }
        if (i - this.mBufferPos < len) {
            drain();
        }
        System.arraycopy(b, off, this.mBuffer, this.mBufferPos, len);
        this.mBufferPos += len;
    }

    @Override // java.io.DataOutput
    public void writeUTF(String s) throws IOException {
        if (this.mBufferCap - this.mBufferPos < s.length() + 2) {
            drain();
        }
        int len = CharsetUtils.toModifiedUtf8Bytes(s, this.mBufferPtr, this.mBufferPos + 2, this.mBufferCap);
        if (Math.abs(len) > 65535) {
            throw new IOException("Modified UTF-8 length too large: " + len);
        }
        if (len >= 0) {
            writeShort(len);
            this.mBufferPos += len;
            return;
        }
        int len2 = -len;
        byte[] tmp = (byte[]) this.mRuntime.newNonMovableArray(Byte.TYPE, len2 + 1);
        CharsetUtils.toModifiedUtf8Bytes(s, this.mRuntime.addressOf(tmp), 0, tmp.length);
        writeShort(len2);
        write(tmp, 0, len2);
    }

    public void writeInternedUTF(String s) throws IOException {
        Short ref = this.mStringRefs.get(s);
        if (ref != null) {
            writeShort(ref.shortValue());
            return;
        }
        writeShort(65535);
        writeUTF(s);
        Short ref2 = Short.valueOf((short) this.mStringRefs.size());
        if (ref2.shortValue() < 65535) {
            this.mStringRefs.put(s, ref2);
        }
    }

    @Override // java.io.DataOutput
    public void writeBoolean(boolean v) throws IOException {
        writeByte(v ? 1 : 0);
    }

    @Override // java.io.DataOutput
    public void writeByte(int v) throws IOException {
        if (this.mBufferCap - this.mBufferPos < 1) {
            drain();
        }
        byte[] bArr = this.mBuffer;
        int i = this.mBufferPos;
        this.mBufferPos = i + 1;
        bArr[i] = (byte) ((v >> 0) & 255);
    }

    @Override // java.io.DataOutput
    public void writeShort(int v) throws IOException {
        if (this.mBufferCap - this.mBufferPos < 2) {
            drain();
        }
        byte[] bArr = this.mBuffer;
        int i = this.mBufferPos;
        int i2 = i + 1;
        this.mBufferPos = i2;
        bArr[i] = (byte) ((v >> 8) & 255);
        this.mBufferPos = i2 + 1;
        bArr[i2] = (byte) ((v >> 0) & 255);
    }

    @Override // java.io.DataOutput
    public void writeChar(int v) throws IOException {
        writeShort((short) v);
    }

    @Override // java.io.DataOutput
    public void writeInt(int v) throws IOException {
        if (this.mBufferCap - this.mBufferPos < 4) {
            drain();
        }
        byte[] bArr = this.mBuffer;
        int i = this.mBufferPos;
        int i2 = i + 1;
        this.mBufferPos = i2;
        bArr[i] = (byte) ((v >> 24) & 255);
        int i3 = i2 + 1;
        this.mBufferPos = i3;
        bArr[i2] = (byte) ((v >> 16) & 255);
        int i4 = i3 + 1;
        this.mBufferPos = i4;
        bArr[i3] = (byte) ((v >> 8) & 255);
        this.mBufferPos = i4 + 1;
        bArr[i4] = (byte) ((v >> 0) & 255);
    }

    @Override // java.io.DataOutput
    public void writeLong(long v) throws IOException {
        if (this.mBufferCap - this.mBufferPos < 8) {
            drain();
        }
        int i = (int) (v >> 32);
        byte[] bArr = this.mBuffer;
        int i2 = this.mBufferPos;
        int i3 = i2 + 1;
        this.mBufferPos = i3;
        bArr[i2] = (byte) ((i >> 24) & 255);
        int i4 = i3 + 1;
        this.mBufferPos = i4;
        bArr[i3] = (byte) ((i >> 16) & 255);
        int i5 = i4 + 1;
        this.mBufferPos = i5;
        bArr[i4] = (byte) ((i >> 8) & 255);
        int i6 = i5 + 1;
        this.mBufferPos = i6;
        bArr[i5] = (byte) ((i >> 0) & 255);
        int i7 = (int) v;
        int i8 = i6 + 1;
        this.mBufferPos = i8;
        bArr[i6] = (byte) ((i7 >> 24) & 255);
        int i9 = i8 + 1;
        this.mBufferPos = i9;
        bArr[i8] = (byte) ((i7 >> 16) & 255);
        int i10 = i9 + 1;
        this.mBufferPos = i10;
        bArr[i9] = (byte) ((i7 >> 8) & 255);
        this.mBufferPos = i10 + 1;
        bArr[i10] = (byte) ((i7 >> 0) & 255);
    }

    @Override // java.io.DataOutput
    public void writeFloat(float v) throws IOException {
        writeInt(Float.floatToIntBits(v));
    }

    @Override // java.io.DataOutput
    public void writeDouble(double v) throws IOException {
        writeLong(Double.doubleToLongBits(v));
    }

    @Override // java.io.DataOutput
    public void writeBytes(String s) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override // java.io.DataOutput
    public void writeChars(String s) throws IOException {
        throw new UnsupportedOperationException();
    }
}
