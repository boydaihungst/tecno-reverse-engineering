package com.android.internal.org.bouncycastle.crypto.digests;

import android.security.keystore.KeyProperties;
import com.android.internal.org.bouncycastle.util.Memoable;
import com.android.internal.org.bouncycastle.util.Pack;
/* loaded from: classes4.dex */
public class SHA512Digest extends LongDigest {
    private static final int DIGEST_LENGTH = 64;

    public SHA512Digest() {
    }

    public SHA512Digest(SHA512Digest t) {
        super(t);
    }

    public SHA512Digest(byte[] encodedState) {
        restoreState(encodedState);
    }

    @Override // com.android.internal.org.bouncycastle.crypto.Digest
    public String getAlgorithmName() {
        return KeyProperties.DIGEST_SHA512;
    }

    @Override // com.android.internal.org.bouncycastle.crypto.Digest
    public int getDigestSize() {
        return 64;
    }

    @Override // com.android.internal.org.bouncycastle.crypto.Digest
    public int doFinal(byte[] out, int outOff) {
        finish();
        Pack.longToBigEndian(this.H1, out, outOff);
        Pack.longToBigEndian(this.H2, out, outOff + 8);
        Pack.longToBigEndian(this.H3, out, outOff + 16);
        Pack.longToBigEndian(this.H4, out, outOff + 24);
        Pack.longToBigEndian(this.H5, out, outOff + 32);
        Pack.longToBigEndian(this.H6, out, outOff + 40);
        Pack.longToBigEndian(this.H7, out, outOff + 48);
        Pack.longToBigEndian(this.H8, out, outOff + 56);
        reset();
        return 64;
    }

    @Override // com.android.internal.org.bouncycastle.crypto.digests.LongDigest, com.android.internal.org.bouncycastle.crypto.Digest
    public void reset() {
        super.reset();
        this.H1 = 7640891576956012808L;
        this.H2 = -4942790177534073029L;
        this.H3 = 4354685564936845355L;
        this.H4 = -6534734903238641935L;
        this.H5 = 5840696475078001361L;
        this.H6 = -7276294671716946913L;
        this.H7 = 2270897969802886507L;
        this.H8 = 6620516959819538809L;
    }

    @Override // com.android.internal.org.bouncycastle.util.Memoable
    public Memoable copy() {
        return new SHA512Digest(this);
    }

    @Override // com.android.internal.org.bouncycastle.util.Memoable
    public void reset(Memoable other) {
        SHA512Digest d = (SHA512Digest) other;
        copyIn(d);
    }

    @Override // com.android.internal.org.bouncycastle.crypto.digests.EncodableDigest
    public byte[] getEncodedState() {
        byte[] encoded = new byte[getEncodedStateSize()];
        super.populateState(encoded);
        return encoded;
    }
}
