package com.android.internal.org.bouncycastle.asn1.x9;

import com.android.internal.org.bouncycastle.asn1.ASN1Encodable;
import com.android.internal.org.bouncycastle.asn1.ASN1EncodableVector;
import com.android.internal.org.bouncycastle.asn1.ASN1Integer;
import com.android.internal.org.bouncycastle.asn1.ASN1Object;
import com.android.internal.org.bouncycastle.asn1.ASN1Primitive;
import com.android.internal.org.bouncycastle.asn1.ASN1Sequence;
import com.android.internal.org.bouncycastle.asn1.ASN1TaggedObject;
import com.android.internal.org.bouncycastle.asn1.DERSequence;
import java.math.BigInteger;
import java.util.Enumeration;
/* loaded from: classes4.dex */
public class DHDomainParameters extends ASN1Object {
    private ASN1Integer g;
    private ASN1Integer j;
    private ASN1Integer p;
    private ASN1Integer q;
    private DHValidationParms validationParms;

    public static DHDomainParameters getInstance(ASN1TaggedObject obj, boolean explicit) {
        return getInstance(ASN1Sequence.getInstance(obj, explicit));
    }

    public static DHDomainParameters getInstance(Object obj) {
        if (obj == null || (obj instanceof DHDomainParameters)) {
            return (DHDomainParameters) obj;
        }
        if (obj instanceof ASN1Sequence) {
            return new DHDomainParameters((ASN1Sequence) obj);
        }
        throw new IllegalArgumentException("Invalid DHDomainParameters: " + obj.getClass().getName());
    }

    public DHDomainParameters(BigInteger p, BigInteger g, BigInteger q, BigInteger j, DHValidationParms validationParms) {
        if (p == null) {
            throw new IllegalArgumentException("'p' cannot be null");
        }
        if (g == null) {
            throw new IllegalArgumentException("'g' cannot be null");
        }
        if (q == null) {
            throw new IllegalArgumentException("'q' cannot be null");
        }
        this.p = new ASN1Integer(p);
        this.g = new ASN1Integer(g);
        this.q = new ASN1Integer(q);
        this.j = new ASN1Integer(j);
        this.validationParms = validationParms;
    }

    public DHDomainParameters(ASN1Integer p, ASN1Integer g, ASN1Integer q, ASN1Integer j, DHValidationParms validationParms) {
        if (p == null) {
            throw new IllegalArgumentException("'p' cannot be null");
        }
        if (g == null) {
            throw new IllegalArgumentException("'g' cannot be null");
        }
        if (q == null) {
            throw new IllegalArgumentException("'q' cannot be null");
        }
        this.p = p;
        this.g = g;
        this.q = q;
        this.j = j;
        this.validationParms = validationParms;
    }

    private DHDomainParameters(ASN1Sequence seq) {
        if (seq.size() < 3 || seq.size() > 5) {
            throw new IllegalArgumentException("Bad sequence size: " + seq.size());
        }
        Enumeration e = seq.getObjects();
        this.p = ASN1Integer.getInstance(e.nextElement());
        this.g = ASN1Integer.getInstance(e.nextElement());
        this.q = ASN1Integer.getInstance(e.nextElement());
        ASN1Encodable next = getNext(e);
        if (next != null && (next instanceof ASN1Integer)) {
            this.j = ASN1Integer.getInstance(next);
            next = getNext(e);
        }
        if (next != null) {
            this.validationParms = DHValidationParms.getInstance(next.toASN1Primitive());
        }
    }

    private static ASN1Encodable getNext(Enumeration e) {
        if (e.hasMoreElements()) {
            return (ASN1Encodable) e.nextElement();
        }
        return null;
    }

    public ASN1Integer getP() {
        return this.p;
    }

    public ASN1Integer getG() {
        return this.g;
    }

    public ASN1Integer getQ() {
        return this.q;
    }

    public ASN1Integer getJ() {
        return this.j;
    }

    public DHValidationParms getValidationParms() {
        return this.validationParms;
    }

    @Override // com.android.internal.org.bouncycastle.asn1.ASN1Object, com.android.internal.org.bouncycastle.asn1.ASN1Encodable
    public ASN1Primitive toASN1Primitive() {
        ASN1EncodableVector v = new ASN1EncodableVector(5);
        v.add(this.p);
        v.add(this.g);
        v.add(this.q);
        ASN1Integer aSN1Integer = this.j;
        if (aSN1Integer != null) {
            v.add(aSN1Integer);
        }
        DHValidationParms dHValidationParms = this.validationParms;
        if (dHValidationParms != null) {
            v.add(dHValidationParms);
        }
        return new DERSequence(v);
    }
}
