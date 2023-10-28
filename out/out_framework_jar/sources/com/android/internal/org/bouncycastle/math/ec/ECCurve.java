package com.android.internal.org.bouncycastle.math.ec;

import com.android.internal.org.bouncycastle.math.ec.ECFieldElement;
import com.android.internal.org.bouncycastle.math.ec.ECPoint;
import com.android.internal.org.bouncycastle.math.ec.endo.ECEndomorphism;
import com.android.internal.org.bouncycastle.math.ec.endo.GLVEndomorphism;
import com.android.internal.org.bouncycastle.math.field.FiniteField;
import com.android.internal.org.bouncycastle.math.field.FiniteFields;
import com.android.internal.org.bouncycastle.math.raw.Nat;
import com.android.internal.org.bouncycastle.util.BigIntegers;
import com.android.internal.org.bouncycastle.util.Integers;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Hashtable;
import java.util.Random;
/* loaded from: classes4.dex */
public abstract class ECCurve {
    public static final int COORD_AFFINE = 0;
    public static final int COORD_HOMOGENEOUS = 1;
    public static final int COORD_JACOBIAN = 2;
    public static final int COORD_JACOBIAN_CHUDNOVSKY = 3;
    public static final int COORD_JACOBIAN_MODIFIED = 4;
    public static final int COORD_LAMBDA_AFFINE = 5;
    public static final int COORD_LAMBDA_PROJECTIVE = 6;
    public static final int COORD_SKEWED = 7;
    protected ECFieldElement a;
    protected ECFieldElement b;
    protected BigInteger cofactor;
    protected FiniteField field;
    protected BigInteger order;
    protected int coord = 0;
    protected ECEndomorphism endomorphism = null;
    protected ECMultiplier multiplier = null;

    protected abstract ECCurve cloneCurve();

    /* JADX INFO: Access modifiers changed from: protected */
    public abstract ECPoint createRawPoint(ECFieldElement eCFieldElement, ECFieldElement eCFieldElement2);

    /* JADX INFO: Access modifiers changed from: protected */
    public abstract ECPoint createRawPoint(ECFieldElement eCFieldElement, ECFieldElement eCFieldElement2, ECFieldElement[] eCFieldElementArr);

    protected abstract ECPoint decompressPoint(int i, BigInteger bigInteger);

    public abstract ECFieldElement fromBigInteger(BigInteger bigInteger);

    public abstract int getFieldSize();

    public abstract ECPoint getInfinity();

    public abstract boolean isValidFieldElement(BigInteger bigInteger);

    public abstract ECFieldElement randomFieldElement(SecureRandom secureRandom);

    public abstract ECFieldElement randomFieldElementMult(SecureRandom secureRandom);

    public static int[] getAllCoordinateSystems() {
        return new int[]{0, 1, 2, 3, 4, 5, 6, 7};
    }

    /* loaded from: classes4.dex */
    public class Config {
        protected int coord;
        protected ECEndomorphism endomorphism;
        protected ECMultiplier multiplier;

        Config(int coord, ECEndomorphism endomorphism, ECMultiplier multiplier) {
            this.coord = coord;
            this.endomorphism = endomorphism;
            this.multiplier = multiplier;
        }

        public Config setCoordinateSystem(int coord) {
            this.coord = coord;
            return this;
        }

        public Config setEndomorphism(ECEndomorphism endomorphism) {
            this.endomorphism = endomorphism;
            return this;
        }

        public Config setMultiplier(ECMultiplier multiplier) {
            this.multiplier = multiplier;
            return this;
        }

        public ECCurve create() {
            if (!ECCurve.this.supportsCoordinateSystem(this.coord)) {
                throw new IllegalStateException("unsupported coordinate system");
            }
            ECCurve c = ECCurve.this.cloneCurve();
            if (c == ECCurve.this) {
                throw new IllegalStateException("implementation returned current curve");
            }
            synchronized (c) {
                c.coord = this.coord;
                c.endomorphism = this.endomorphism;
                c.multiplier = this.multiplier;
            }
            return c;
        }
    }

    protected ECCurve(FiniteField field) {
        this.field = field;
    }

    public synchronized Config configure() {
        return new Config(this.coord, this.endomorphism, this.multiplier);
    }

    public ECPoint validatePoint(BigInteger x, BigInteger y) {
        ECPoint p = createPoint(x, y);
        if (!p.isValid()) {
            throw new IllegalArgumentException("Invalid point coordinates");
        }
        return p;
    }

    public ECPoint createPoint(BigInteger x, BigInteger y) {
        return createRawPoint(fromBigInteger(x), fromBigInteger(y));
    }

    protected ECMultiplier createDefaultMultiplier() {
        ECEndomorphism eCEndomorphism = this.endomorphism;
        if (eCEndomorphism instanceof GLVEndomorphism) {
            return new GLVMultiplier(this, (GLVEndomorphism) eCEndomorphism);
        }
        return new WNafL2RMultiplier();
    }

    public boolean supportsCoordinateSystem(int coord) {
        return coord == 0;
    }

    public PreCompInfo getPreCompInfo(ECPoint point, String name) {
        Hashtable table;
        PreCompInfo preCompInfo;
        checkPoint(point);
        synchronized (point) {
            table = point.preCompTable;
        }
        if (table == null) {
            return null;
        }
        synchronized (table) {
            preCompInfo = (PreCompInfo) table.get(name);
        }
        return preCompInfo;
    }

    public PreCompInfo precompute(ECPoint point, String name, PreCompCallback callback) {
        Hashtable table;
        PreCompInfo result;
        checkPoint(point);
        synchronized (point) {
            table = point.preCompTable;
            if (table == null) {
                Hashtable hashtable = new Hashtable(4);
                table = hashtable;
                point.preCompTable = hashtable;
            }
        }
        synchronized (table) {
            PreCompInfo existing = (PreCompInfo) table.get(name);
            result = callback.precompute(existing);
            if (result != existing) {
                table.put(name, result);
            }
        }
        return result;
    }

    public ECPoint importPoint(ECPoint p) {
        if (this == p.getCurve()) {
            return p;
        }
        if (p.isInfinity()) {
            return getInfinity();
        }
        ECPoint p2 = p.normalize();
        return createPoint(p2.getXCoord().toBigInteger(), p2.getYCoord().toBigInteger());
    }

    public void normalizeAll(ECPoint[] points) {
        normalizeAll(points, 0, points.length, null);
    }

    public void normalizeAll(ECPoint[] points, int off, int len, ECFieldElement iso) {
        checkPoints(points, off, len);
        switch (getCoordinateSystem()) {
            case 0:
            case 5:
                if (iso != null) {
                    throw new IllegalArgumentException("'iso' not valid for affine coordinates");
                }
                return;
            default:
                ECFieldElement[] zs = new ECFieldElement[len];
                int[] indices = new int[len];
                int count = 0;
                for (int i = 0; i < len; i++) {
                    ECPoint p = points[off + i];
                    if (p != null && (iso != null || !p.isNormalized())) {
                        zs[count] = p.getZCoord(0);
                        indices[count] = off + i;
                        count++;
                    }
                }
                if (count == 0) {
                    return;
                }
                ECAlgorithms.montgomeryTrick(zs, 0, count, iso);
                for (int j = 0; j < count; j++) {
                    int index = indices[j];
                    points[index] = points[index].normalize(zs[j]);
                }
                return;
        }
    }

    public FiniteField getField() {
        return this.field;
    }

    public ECFieldElement getA() {
        return this.a;
    }

    public ECFieldElement getB() {
        return this.b;
    }

    public BigInteger getOrder() {
        return this.order;
    }

    public BigInteger getCofactor() {
        return this.cofactor;
    }

    public int getCoordinateSystem() {
        return this.coord;
    }

    public ECEndomorphism getEndomorphism() {
        return this.endomorphism;
    }

    public ECMultiplier getMultiplier() {
        if (this.multiplier == null) {
            this.multiplier = createDefaultMultiplier();
        }
        return this.multiplier;
    }

    public ECPoint decodePoint(byte[] encoded) {
        ECPoint p;
        int expectedLength = (getFieldSize() + 7) / 8;
        boolean z = false;
        byte type = encoded[0];
        switch (type) {
            case 0:
                if (encoded.length != 1) {
                    throw new IllegalArgumentException("Incorrect length for infinity encoding");
                }
                p = getInfinity();
                break;
            case 1:
            case 5:
            default:
                throw new IllegalArgumentException("Invalid point encoding 0x" + Integer.toString(type, 16));
            case 2:
            case 3:
                if (encoded.length != expectedLength + 1) {
                    throw new IllegalArgumentException("Incorrect length for compressed encoding");
                }
                int yTilde = type & 1;
                BigInteger X = BigIntegers.fromUnsignedByteArray(encoded, 1, expectedLength);
                p = decompressPoint(yTilde, X);
                if (!p.implIsValid(true, true)) {
                    throw new IllegalArgumentException("Invalid point");
                }
                break;
            case 4:
                if (encoded.length != (expectedLength * 2) + 1) {
                    throw new IllegalArgumentException("Incorrect length for uncompressed encoding");
                }
                BigInteger X2 = BigIntegers.fromUnsignedByteArray(encoded, 1, expectedLength);
                p = validatePoint(X2, BigIntegers.fromUnsignedByteArray(encoded, expectedLength + 1, expectedLength));
                break;
            case 6:
            case 7:
                if (encoded.length != (expectedLength * 2) + 1) {
                    throw new IllegalArgumentException("Incorrect length for hybrid encoding");
                }
                BigInteger X3 = BigIntegers.fromUnsignedByteArray(encoded, 1, expectedLength);
                BigInteger Y = BigIntegers.fromUnsignedByteArray(encoded, expectedLength + 1, expectedLength);
                boolean testBit = Y.testBit(0);
                if (type == 7) {
                    z = true;
                }
                if (testBit != z) {
                    throw new IllegalArgumentException("Inconsistent Y coordinate in hybrid encoding");
                }
                p = validatePoint(X3, Y);
                break;
        }
        if (type != 0 && p.isInfinity()) {
            throw new IllegalArgumentException("Invalid infinity encoding");
        }
        return p;
    }

    public ECLookupTable createCacheSafeLookupTable(ECPoint[] points, int off, final int len) {
        final int FE_BYTES = (getFieldSize() + 7) >>> 3;
        final byte[] table = new byte[len * FE_BYTES * 2];
        int pos = 0;
        for (int i = 0; i < len; i++) {
            ECPoint p = points[off + i];
            byte[] px = p.getRawXCoord().toBigInteger().toByteArray();
            byte[] py = p.getRawYCoord().toBigInteger().toByteArray();
            int pyStart = 0;
            int pxStart = px.length > FE_BYTES ? 1 : 0;
            int pxLen = px.length - pxStart;
            if (py.length > FE_BYTES) {
                pyStart = 1;
            }
            int pyLen = py.length - pyStart;
            System.arraycopy(px, pxStart, table, (pos + FE_BYTES) - pxLen, pxLen);
            int pos2 = pos + FE_BYTES;
            System.arraycopy(py, pyStart, table, (pos2 + FE_BYTES) - pyLen, pyLen);
            pos = pos2 + FE_BYTES;
        }
        return new AbstractECLookupTable() { // from class: com.android.internal.org.bouncycastle.math.ec.ECCurve.1
            @Override // com.android.internal.org.bouncycastle.math.ec.ECLookupTable
            public int getSize() {
                return len;
            }

            @Override // com.android.internal.org.bouncycastle.math.ec.ECLookupTable
            public ECPoint lookup(int index) {
                int i2;
                int i3 = FE_BYTES;
                byte[] x = new byte[i3];
                byte[] y = new byte[i3];
                int pos3 = 0;
                for (int i4 = 0; i4 < len; i4++) {
                    int MASK = ((i4 ^ index) - 1) >> 31;
                    int j = 0;
                    while (true) {
                        i2 = FE_BYTES;
                        if (j < i2) {
                            byte b = x[j];
                            byte[] bArr = table;
                            x[j] = (byte) (b ^ (bArr[pos3 + j] & MASK));
                            y[j] = (byte) ((bArr[(i2 + pos3) + j] & MASK) ^ y[j]);
                            j++;
                        }
                    }
                    pos3 += i2 * 2;
                }
                return createPoint(x, y);
            }

            @Override // com.android.internal.org.bouncycastle.math.ec.AbstractECLookupTable, com.android.internal.org.bouncycastle.math.ec.ECLookupTable
            public ECPoint lookupVar(int index) {
                int i2 = FE_BYTES;
                byte[] x = new byte[i2];
                byte[] y = new byte[i2];
                int pos3 = i2 * index * 2;
                int j = 0;
                while (true) {
                    int i3 = FE_BYTES;
                    if (j < i3) {
                        byte[] bArr = table;
                        x[j] = bArr[pos3 + j];
                        y[j] = bArr[i3 + pos3 + j];
                        j++;
                    } else {
                        return createPoint(x, y);
                    }
                }
            }

            private ECPoint createPoint(byte[] x, byte[] y) {
                ECCurve eCCurve = ECCurve.this;
                return eCCurve.createRawPoint(eCCurve.fromBigInteger(new BigInteger(1, x)), ECCurve.this.fromBigInteger(new BigInteger(1, y)));
            }
        };
    }

    protected void checkPoint(ECPoint point) {
        if (point == null || this != point.getCurve()) {
            throw new IllegalArgumentException("'point' must be non-null and on this curve");
        }
    }

    protected void checkPoints(ECPoint[] points) {
        checkPoints(points, 0, points.length);
    }

    protected void checkPoints(ECPoint[] points, int off, int len) {
        if (points == null) {
            throw new IllegalArgumentException("'points' cannot be null");
        }
        if (off < 0 || len < 0 || off > points.length - len) {
            throw new IllegalArgumentException("invalid range specified for 'points'");
        }
        for (int i = 0; i < len; i++) {
            ECPoint point = points[off + i];
            if (point != null && this != point.getCurve()) {
                throw new IllegalArgumentException("'points' entries must be null or on this curve");
            }
        }
    }

    public boolean equals(ECCurve other) {
        return this == other || (other != null && getField().equals(other.getField()) && getA().toBigInteger().equals(other.getA().toBigInteger()) && getB().toBigInteger().equals(other.getB().toBigInteger()));
    }

    public boolean equals(Object obj) {
        return this == obj || ((obj instanceof ECCurve) && equals((ECCurve) obj));
    }

    public int hashCode() {
        return (getField().hashCode() ^ Integers.rotateLeft(getA().toBigInteger().hashCode(), 8)) ^ Integers.rotateLeft(getB().toBigInteger().hashCode(), 16);
    }

    /* loaded from: classes4.dex */
    public static abstract class AbstractFp extends ECCurve {
        /* JADX INFO: Access modifiers changed from: protected */
        public AbstractFp(BigInteger q) {
            super(FiniteFields.getPrimeField(q));
        }

        @Override // com.android.internal.org.bouncycastle.math.ec.ECCurve
        public boolean isValidFieldElement(BigInteger x) {
            return x != null && x.signum() >= 0 && x.compareTo(getField().getCharacteristic()) < 0;
        }

        @Override // com.android.internal.org.bouncycastle.math.ec.ECCurve
        public ECFieldElement randomFieldElement(SecureRandom r) {
            BigInteger p = getField().getCharacteristic();
            ECFieldElement fe1 = fromBigInteger(implRandomFieldElement(r, p));
            ECFieldElement fe2 = fromBigInteger(implRandomFieldElement(r, p));
            return fe1.multiply(fe2);
        }

        @Override // com.android.internal.org.bouncycastle.math.ec.ECCurve
        public ECFieldElement randomFieldElementMult(SecureRandom r) {
            BigInteger p = getField().getCharacteristic();
            ECFieldElement fe1 = fromBigInteger(implRandomFieldElementMult(r, p));
            ECFieldElement fe2 = fromBigInteger(implRandomFieldElementMult(r, p));
            return fe1.multiply(fe2);
        }

        @Override // com.android.internal.org.bouncycastle.math.ec.ECCurve
        protected ECPoint decompressPoint(int yTilde, BigInteger X1) {
            ECFieldElement x = fromBigInteger(X1);
            ECFieldElement rhs = x.square().add(this.a).multiply(x).add(this.b);
            ECFieldElement y = rhs.sqrt();
            if (y == null) {
                throw new IllegalArgumentException("Invalid point compression");
            }
            if (y.testBitZero() != (yTilde == 1)) {
                y = y.negate();
            }
            return createRawPoint(x, y);
        }

        private static BigInteger implRandomFieldElement(SecureRandom r, BigInteger p) {
            BigInteger x;
            do {
                x = BigIntegers.createRandomBigInteger(p.bitLength(), r);
            } while (x.compareTo(p) >= 0);
            return x;
        }

        private static BigInteger implRandomFieldElementMult(SecureRandom r, BigInteger p) {
            while (true) {
                BigInteger x = BigIntegers.createRandomBigInteger(p.bitLength(), r);
                if (x.signum() > 0 && x.compareTo(p) < 0) {
                    return x;
                }
            }
        }
    }

    /* loaded from: classes4.dex */
    public static class Fp extends AbstractFp {
        private static final int FP_DEFAULT_COORDS = 4;
        ECPoint.Fp infinity;
        BigInteger q;
        BigInteger r;

        public Fp(BigInteger q, BigInteger a, BigInteger b) {
            this(q, a, b, null, null);
        }

        public Fp(BigInteger q, BigInteger a, BigInteger b, BigInteger order, BigInteger cofactor) {
            super(q);
            this.q = q;
            this.r = ECFieldElement.Fp.calculateResidue(q);
            this.infinity = new ECPoint.Fp(this, null, null);
            this.a = fromBigInteger(a);
            this.b = fromBigInteger(b);
            this.order = order;
            this.cofactor = cofactor;
            this.coord = 4;
        }

        protected Fp(BigInteger q, BigInteger r, ECFieldElement a, ECFieldElement b, BigInteger order, BigInteger cofactor) {
            super(q);
            this.q = q;
            this.r = r;
            this.infinity = new ECPoint.Fp(this, null, null);
            this.a = a;
            this.b = b;
            this.order = order;
            this.cofactor = cofactor;
            this.coord = 4;
        }

        @Override // com.android.internal.org.bouncycastle.math.ec.ECCurve
        protected ECCurve cloneCurve() {
            return new Fp(this.q, this.r, this.a, this.b, this.order, this.cofactor);
        }

        @Override // com.android.internal.org.bouncycastle.math.ec.ECCurve
        public boolean supportsCoordinateSystem(int coord) {
            switch (coord) {
                case 0:
                case 1:
                case 2:
                case 4:
                    return true;
                case 3:
                default:
                    return false;
            }
        }

        public BigInteger getQ() {
            return this.q;
        }

        @Override // com.android.internal.org.bouncycastle.math.ec.ECCurve
        public int getFieldSize() {
            return this.q.bitLength();
        }

        @Override // com.android.internal.org.bouncycastle.math.ec.ECCurve
        public ECFieldElement fromBigInteger(BigInteger x) {
            return new ECFieldElement.Fp(this.q, this.r, x);
        }

        @Override // com.android.internal.org.bouncycastle.math.ec.ECCurve
        protected ECPoint createRawPoint(ECFieldElement x, ECFieldElement y) {
            return new ECPoint.Fp(this, x, y);
        }

        @Override // com.android.internal.org.bouncycastle.math.ec.ECCurve
        protected ECPoint createRawPoint(ECFieldElement x, ECFieldElement y, ECFieldElement[] zs) {
            return new ECPoint.Fp(this, x, y, zs);
        }

        @Override // com.android.internal.org.bouncycastle.math.ec.ECCurve
        public ECPoint importPoint(ECPoint p) {
            if (this != p.getCurve() && getCoordinateSystem() == 2 && !p.isInfinity()) {
                switch (p.getCurve().getCoordinateSystem()) {
                    case 2:
                    case 3:
                    case 4:
                        return new ECPoint.Fp(this, fromBigInteger(p.x.toBigInteger()), fromBigInteger(p.y.toBigInteger()), new ECFieldElement[]{fromBigInteger(p.zs[0].toBigInteger())});
                }
            }
            return super.importPoint(p);
        }

        @Override // com.android.internal.org.bouncycastle.math.ec.ECCurve
        public ECPoint getInfinity() {
            return this.infinity;
        }
    }

    /* loaded from: classes4.dex */
    public static abstract class AbstractF2m extends ECCurve {
        private BigInteger[] si;

        public static BigInteger inverse(int m, int[] ks, BigInteger x) {
            return new LongArray(x).modInverse(m, ks).toBigInteger();
        }

        private static FiniteField buildField(int m, int k1, int k2, int k3) {
            if (k1 != 0) {
                if (k2 == 0) {
                    if (k3 == 0) {
                        return FiniteFields.getBinaryExtensionField(new int[]{0, k1, m});
                    }
                    throw new IllegalArgumentException("k3 must be 0 if k2 == 0");
                } else if (k2 <= k1) {
                    throw new IllegalArgumentException("k2 must be > k1");
                } else {
                    if (k3 > k2) {
                        return FiniteFields.getBinaryExtensionField(new int[]{0, k1, k2, k3, m});
                    }
                    throw new IllegalArgumentException("k3 must be > k2");
                }
            }
            throw new IllegalArgumentException("k1 must be > 0");
        }

        protected AbstractF2m(int m, int k1, int k2, int k3) {
            super(buildField(m, k1, k2, k3));
            this.si = null;
        }

        @Override // com.android.internal.org.bouncycastle.math.ec.ECCurve
        public ECPoint createPoint(BigInteger x, BigInteger y) {
            ECFieldElement X = fromBigInteger(x);
            ECFieldElement Y = fromBigInteger(y);
            int coord = getCoordinateSystem();
            switch (coord) {
                case 5:
                case 6:
                    if (X.isZero()) {
                        if (!Y.square().equals(getB())) {
                            throw new IllegalArgumentException();
                        }
                    } else {
                        Y = Y.divide(X).add(X);
                        break;
                    }
                    break;
            }
            return createRawPoint(X, Y);
        }

        @Override // com.android.internal.org.bouncycastle.math.ec.ECCurve
        public boolean isValidFieldElement(BigInteger x) {
            return x != null && x.signum() >= 0 && x.bitLength() <= getFieldSize();
        }

        @Override // com.android.internal.org.bouncycastle.math.ec.ECCurve
        public ECFieldElement randomFieldElement(SecureRandom r) {
            int m = getFieldSize();
            return fromBigInteger(BigIntegers.createRandomBigInteger(m, r));
        }

        @Override // com.android.internal.org.bouncycastle.math.ec.ECCurve
        public ECFieldElement randomFieldElementMult(SecureRandom r) {
            int m = getFieldSize();
            ECFieldElement fe1 = fromBigInteger(implRandomFieldElementMult(r, m));
            ECFieldElement fe2 = fromBigInteger(implRandomFieldElementMult(r, m));
            return fe1.multiply(fe2);
        }

        @Override // com.android.internal.org.bouncycastle.math.ec.ECCurve
        protected ECPoint decompressPoint(int yTilde, BigInteger X1) {
            ECFieldElement x = fromBigInteger(X1);
            ECFieldElement y = null;
            if (x.isZero()) {
                y = getB().sqrt();
            } else {
                ECFieldElement beta = x.square().invert().multiply(getB()).add(getA()).add(x);
                ECFieldElement z = solveQuadraticEquation(beta);
                if (z != null) {
                    if (z.testBitZero() != (yTilde == 1)) {
                        z = z.addOne();
                    }
                    switch (getCoordinateSystem()) {
                        case 5:
                        case 6:
                            y = z.add(x);
                            break;
                        default:
                            y = z.multiply(x);
                            break;
                    }
                }
            }
            if (y == null) {
                throw new IllegalArgumentException("Invalid point compression");
            }
            return createRawPoint(x, y);
        }

        /* JADX INFO: Access modifiers changed from: protected */
        public ECFieldElement solveQuadraticEquation(ECFieldElement beta) {
            ECFieldElement z;
            ECFieldElement t;
            ECFieldElement.AbstractF2m betaF2m = (ECFieldElement.AbstractF2m) beta;
            boolean fastTrace = betaF2m.hasFastTrace();
            if (fastTrace && betaF2m.trace() != 0) {
                return null;
            }
            int m = getFieldSize();
            if ((m & 1) != 0) {
                ECFieldElement r = betaF2m.halfTrace();
                if (!fastTrace && !r.square().add(r).add(beta).isZero()) {
                    return null;
                }
                return r;
            } else if (beta.isZero()) {
                return beta;
            } else {
                ECFieldElement zeroElement = fromBigInteger(ECConstants.ZERO);
                Random rand = new Random();
                do {
                    ECFieldElement t2 = fromBigInteger(new BigInteger(m, rand));
                    z = zeroElement;
                    ECFieldElement w = beta;
                    for (int i = 1; i < m; i++) {
                        ECFieldElement w2 = w.square();
                        z = z.square().add(w2.multiply(t2));
                        w = w2.add(beta);
                    }
                    if (!w.isZero()) {
                        return null;
                    }
                    t = z.square().add(z);
                } while (t.isZero());
                return z;
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public synchronized BigInteger[] getSi() {
            if (this.si == null) {
                this.si = Tnaf.getSi(this);
            }
            return this.si;
        }

        public boolean isKoblitz() {
            return this.order != null && this.cofactor != null && this.b.isOne() && (this.a.isZero() || this.a.isOne());
        }

        private static BigInteger implRandomFieldElementMult(SecureRandom r, int m) {
            BigInteger x;
            do {
                x = BigIntegers.createRandomBigInteger(m, r);
            } while (x.signum() <= 0);
            return x;
        }
    }

    /* loaded from: classes4.dex */
    public static class F2m extends AbstractF2m {
        private static final int F2M_DEFAULT_COORDS = 6;
        private ECPoint.F2m infinity;
        private int k1;
        private int k2;
        private int k3;
        private int m;

        public F2m(int m, int k, BigInteger a, BigInteger b) {
            this(m, k, 0, 0, a, b, (BigInteger) null, (BigInteger) null);
        }

        public F2m(int m, int k, BigInteger a, BigInteger b, BigInteger order, BigInteger cofactor) {
            this(m, k, 0, 0, a, b, order, cofactor);
        }

        public F2m(int m, int k1, int k2, int k3, BigInteger a, BigInteger b) {
            this(m, k1, k2, k3, a, b, (BigInteger) null, (BigInteger) null);
        }

        public F2m(int m, int k1, int k2, int k3, BigInteger a, BigInteger b, BigInteger order, BigInteger cofactor) {
            super(m, k1, k2, k3);
            this.m = m;
            this.k1 = k1;
            this.k2 = k2;
            this.k3 = k3;
            this.order = order;
            this.cofactor = cofactor;
            this.infinity = new ECPoint.F2m(this, null, null);
            this.a = fromBigInteger(a);
            this.b = fromBigInteger(b);
            this.coord = 6;
        }

        protected F2m(int m, int k1, int k2, int k3, ECFieldElement a, ECFieldElement b, BigInteger order, BigInteger cofactor) {
            super(m, k1, k2, k3);
            this.m = m;
            this.k1 = k1;
            this.k2 = k2;
            this.k3 = k3;
            this.order = order;
            this.cofactor = cofactor;
            this.infinity = new ECPoint.F2m(this, null, null);
            this.a = a;
            this.b = b;
            this.coord = 6;
        }

        @Override // com.android.internal.org.bouncycastle.math.ec.ECCurve
        protected ECCurve cloneCurve() {
            return new F2m(this.m, this.k1, this.k2, this.k3, this.a, this.b, this.order, this.cofactor);
        }

        @Override // com.android.internal.org.bouncycastle.math.ec.ECCurve
        public boolean supportsCoordinateSystem(int coord) {
            switch (coord) {
                case 0:
                case 1:
                case 6:
                    return true;
                default:
                    return false;
            }
        }

        @Override // com.android.internal.org.bouncycastle.math.ec.ECCurve
        protected ECMultiplier createDefaultMultiplier() {
            if (isKoblitz()) {
                return new WTauNafMultiplier();
            }
            return super.createDefaultMultiplier();
        }

        @Override // com.android.internal.org.bouncycastle.math.ec.ECCurve
        public int getFieldSize() {
            return this.m;
        }

        @Override // com.android.internal.org.bouncycastle.math.ec.ECCurve
        public ECFieldElement fromBigInteger(BigInteger x) {
            return new ECFieldElement.F2m(this.m, this.k1, this.k2, this.k3, x);
        }

        @Override // com.android.internal.org.bouncycastle.math.ec.ECCurve
        protected ECPoint createRawPoint(ECFieldElement x, ECFieldElement y) {
            return new ECPoint.F2m(this, x, y);
        }

        @Override // com.android.internal.org.bouncycastle.math.ec.ECCurve
        protected ECPoint createRawPoint(ECFieldElement x, ECFieldElement y, ECFieldElement[] zs) {
            return new ECPoint.F2m(this, x, y, zs);
        }

        @Override // com.android.internal.org.bouncycastle.math.ec.ECCurve
        public ECPoint getInfinity() {
            return this.infinity;
        }

        public int getM() {
            return this.m;
        }

        public boolean isTrinomial() {
            return this.k2 == 0 && this.k3 == 0;
        }

        public int getK1() {
            return this.k1;
        }

        public int getK2() {
            return this.k2;
        }

        public int getK3() {
            return this.k3;
        }

        @Override // com.android.internal.org.bouncycastle.math.ec.ECCurve
        public ECLookupTable createCacheSafeLookupTable(ECPoint[] points, int off, final int len) {
            final int FE_LONGS = (this.m + 63) >>> 6;
            final int[] ks = isTrinomial() ? new int[]{this.k1} : new int[]{this.k1, this.k2, this.k3};
            final long[] table = new long[len * FE_LONGS * 2];
            int pos = 0;
            for (int i = 0; i < len; i++) {
                ECPoint p = points[off + i];
                ((ECFieldElement.F2m) p.getRawXCoord()).x.copyTo(table, pos);
                int pos2 = pos + FE_LONGS;
                ((ECFieldElement.F2m) p.getRawYCoord()).x.copyTo(table, pos2);
                pos = pos2 + FE_LONGS;
            }
            return new AbstractECLookupTable() { // from class: com.android.internal.org.bouncycastle.math.ec.ECCurve.F2m.1
                @Override // com.android.internal.org.bouncycastle.math.ec.ECLookupTable
                public int getSize() {
                    return len;
                }

                @Override // com.android.internal.org.bouncycastle.math.ec.ECLookupTable
                public ECPoint lookup(int index) {
                    int i2;
                    long[] x = Nat.create64(FE_LONGS);
                    long[] y = Nat.create64(FE_LONGS);
                    int pos3 = 0;
                    for (int i3 = 0; i3 < len; i3++) {
                        long MASK = ((i3 ^ index) - 1) >> 31;
                        int j = 0;
                        while (true) {
                            i2 = FE_LONGS;
                            if (j < i2) {
                                long j2 = x[j];
                                long[] jArr = table;
                                x[j] = j2 ^ (jArr[pos3 + j] & MASK);
                                y[j] = y[j] ^ (jArr[(i2 + pos3) + j] & MASK);
                                j++;
                            }
                        }
                        pos3 += i2 * 2;
                    }
                    return createPoint(x, y);
                }

                @Override // com.android.internal.org.bouncycastle.math.ec.AbstractECLookupTable, com.android.internal.org.bouncycastle.math.ec.ECLookupTable
                public ECPoint lookupVar(int index) {
                    long[] x = Nat.create64(FE_LONGS);
                    long[] y = Nat.create64(FE_LONGS);
                    int pos3 = FE_LONGS * index * 2;
                    int j = 0;
                    while (true) {
                        int i2 = FE_LONGS;
                        if (j < i2) {
                            long[] jArr = table;
                            x[j] = jArr[pos3 + j];
                            y[j] = jArr[i2 + pos3 + j];
                            j++;
                        } else {
                            return createPoint(x, y);
                        }
                    }
                }

                private ECPoint createPoint(long[] x, long[] y) {
                    ECFieldElement.F2m X = new ECFieldElement.F2m(F2m.this.m, ks, new LongArray(x));
                    ECFieldElement.F2m Y = new ECFieldElement.F2m(F2m.this.m, ks, new LongArray(y));
                    return F2m.this.createRawPoint(X, Y);
                }
            };
        }
    }
}
