package com.android.net.module.util;

import com.android.net.module.util.DnsPacketUtils;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
/* loaded from: classes4.dex */
public abstract class DnsPacket {
    public static final int ANSECTION = 1;
    public static final int ARSECTION = 3;
    public static final int NSSECTION = 2;
    private static final int NUM_SECTIONS = 4;
    public static final int QDSECTION = 0;
    private static final String TAG = DnsPacket.class.getSimpleName();
    protected final DnsHeader mHeader;
    protected final List<DnsRecord>[] mRecords;

    /* loaded from: classes4.dex */
    public static class ParseException extends RuntimeException {
        public String reason;

        public ParseException(String reason) {
            super(reason);
            this.reason = reason;
        }

        public ParseException(String reason, Throwable cause) {
            super(reason, cause);
            this.reason = reason;
        }
    }

    /* loaded from: classes4.dex */
    public class DnsHeader {
        private static final int FLAGS_SECTION_QR_BIT = 15;
        private static final String TAG = "DnsHeader";
        public final int flags;
        public final int id;
        private final int[] mRecordCount;
        public final int rcode;

        DnsHeader(ByteBuffer buf) throws BufferUnderflowException {
            this.id = Short.toUnsignedInt(buf.getShort());
            int unsignedInt = Short.toUnsignedInt(buf.getShort());
            this.flags = unsignedInt;
            this.rcode = unsignedInt & 15;
            this.mRecordCount = new int[4];
            for (int i = 0; i < 4; i++) {
                this.mRecordCount[i] = Short.toUnsignedInt(buf.getShort());
            }
        }

        public boolean isResponse() {
            return (this.flags & 32768) != 0;
        }

        public int getRecordCount(int type) {
            return this.mRecordCount[type];
        }
    }

    /* loaded from: classes4.dex */
    public class DnsRecord {
        private static final int MAXNAMESIZE = 255;
        public static final int NAME_COMPRESSION = 192;
        public static final int NAME_NORMAL = 0;
        private static final String TAG = "DnsRecord";
        public final String dName;
        private final byte[] mRdata;
        public final int nsClass;
        public final int nsType;
        public final long ttl;

        DnsRecord(int recordType, ByteBuffer buf) throws BufferUnderflowException, ParseException {
            String parseName = DnsPacketUtils.DnsRecordParser.parseName(buf, 0, true);
            this.dName = parseName;
            if (parseName.length() > 255) {
                throw new ParseException("Parse name fail, name size is too long: " + parseName.length());
            }
            this.nsType = Short.toUnsignedInt(buf.getShort());
            this.nsClass = Short.toUnsignedInt(buf.getShort());
            if (recordType != 0) {
                this.ttl = Integer.toUnsignedLong(buf.getInt());
                int length = Short.toUnsignedInt(buf.getShort());
                byte[] bArr = new byte[length];
                this.mRdata = bArr;
                buf.get(bArr);
                return;
            }
            this.ttl = 0L;
            this.mRdata = null;
        }

        public byte[] getRR() {
            byte[] bArr = this.mRdata;
            if (bArr == null) {
                return null;
            }
            return (byte[]) bArr.clone();
        }
    }

    protected DnsPacket(byte[] data) throws ParseException {
        if (data == null) {
            throw new ParseException("Parse header failed, null input data");
        }
        try {
            ByteBuffer buffer = ByteBuffer.wrap(data);
            this.mHeader = new DnsHeader(buffer);
            this.mRecords = new ArrayList[4];
            for (int i = 0; i < 4; i++) {
                int count = this.mHeader.getRecordCount(i);
                if (count > 0) {
                    this.mRecords[i] = new ArrayList(count);
                }
                for (int j = 0; j < count; j++) {
                    try {
                        this.mRecords[i].add(new DnsRecord(i, buffer));
                    } catch (BufferUnderflowException e) {
                        throw new ParseException("Parse record fail", e);
                    }
                }
            }
        } catch (BufferUnderflowException e2) {
            throw new ParseException("Parse Header fail, bad input data", e2);
        }
    }
}
