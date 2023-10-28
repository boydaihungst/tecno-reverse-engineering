package com.android.server.pm;

import android.content.pm.Signature;
import android.content.pm.SigningDetails;
import android.util.TypedXmlPullParser;
import android.util.TypedXmlSerializer;
import com.android.internal.util.XmlUtils;
import com.android.server.am.AssistDataRequester;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import org.xmlpull.v1.XmlPullParserException;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class PackageSignatures {
    SigningDetails mSigningDetails;

    /* JADX INFO: Access modifiers changed from: package-private */
    public PackageSignatures(PackageSignatures orig) {
        if (orig != null && orig.mSigningDetails != SigningDetails.UNKNOWN) {
            this.mSigningDetails = new SigningDetails(orig.mSigningDetails);
        } else {
            this.mSigningDetails = SigningDetails.UNKNOWN;
        }
    }

    PackageSignatures(SigningDetails signingDetails) {
        this.mSigningDetails = signingDetails;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public PackageSignatures() {
        this.mSigningDetails = SigningDetails.UNKNOWN;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void writeXml(TypedXmlSerializer serializer, String tagName, ArrayList<Signature> writtenSignatures) throws IOException {
        if (this.mSigningDetails.getSignatures() == null) {
            return;
        }
        serializer.startTag((String) null, tagName);
        serializer.attributeInt((String) null, AssistDataRequester.KEY_RECEIVER_EXTRA_COUNT, this.mSigningDetails.getSignatures().length);
        serializer.attributeInt((String) null, "schemeVersion", this.mSigningDetails.getSignatureSchemeVersion());
        writeCertsListXml(serializer, writtenSignatures, this.mSigningDetails.getSignatures(), false);
        if (this.mSigningDetails.getPastSigningCertificates() != null) {
            serializer.startTag((String) null, "pastSigs");
            serializer.attributeInt((String) null, AssistDataRequester.KEY_RECEIVER_EXTRA_COUNT, this.mSigningDetails.getPastSigningCertificates().length);
            writeCertsListXml(serializer, writtenSignatures, this.mSigningDetails.getPastSigningCertificates(), true);
            serializer.endTag((String) null, "pastSigs");
        }
        serializer.endTag((String) null, tagName);
    }

    private void writeCertsListXml(TypedXmlSerializer serializer, ArrayList<Signature> writtenSignatures, Signature[] signatures, boolean isPastSigs) throws IOException {
        for (Signature sig : signatures) {
            serializer.startTag((String) null, "cert");
            int sigHash = sig.hashCode();
            int numWritten = writtenSignatures.size();
            int j = 0;
            while (true) {
                if (j >= numWritten) {
                    break;
                }
                Signature writtenSig = writtenSignatures.get(j);
                if (writtenSig.hashCode() != sigHash || !writtenSig.equals(sig)) {
                    j++;
                } else {
                    serializer.attributeInt((String) null, AssistDataRequester.KEY_RECEIVER_EXTRA_INDEX, j);
                    break;
                }
            }
            if (j >= numWritten) {
                writtenSignatures.add(sig);
                serializer.attributeInt((String) null, AssistDataRequester.KEY_RECEIVER_EXTRA_INDEX, numWritten);
                sig.writeToXmlAttributeBytesHex(serializer, null, "key");
            }
            if (isPastSigs) {
                serializer.attributeInt((String) null, "flags", sig.getFlags());
            }
            serializer.endTag((String) null, "cert");
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void readXml(TypedXmlPullParser parser, ArrayList<Signature> readSignatures) throws IOException, XmlPullParserException {
        SigningDetails.Builder builder = new SigningDetails.Builder();
        int count = parser.getAttributeInt((String) null, AssistDataRequester.KEY_RECEIVER_EXTRA_COUNT, -1);
        if (count != -1) {
            int signatureSchemeVersion = parser.getAttributeInt((String) null, "schemeVersion", 0);
            if (signatureSchemeVersion == 0) {
                PackageManagerService.reportSettingsProblem(5, "Error in package manager settings: <sigs> has no schemeVersion at " + parser.getPositionDescription());
            }
            builder.setSignatureSchemeVersion(signatureSchemeVersion);
            ArrayList<Signature> signatureList = new ArrayList<>();
            int pos = readCertsListXml(parser, readSignatures, signatureList, count, false, builder);
            Signature[] signatures = (Signature[]) signatureList.toArray(new Signature[signatureList.size()]);
            builder.setSignatures(signatures);
            if (pos < count) {
                PackageManagerService.reportSettingsProblem(5, "Error in package manager settings: <sigs> count does not match number of  <cert> entries" + parser.getPositionDescription());
            }
            try {
                this.mSigningDetails = builder.build();
                return;
            } catch (CertificateException e) {
                PackageManagerService.reportSettingsProblem(5, "Error in package manager settings: <sigs> unable to convert certificate(s) to public key(s).");
                this.mSigningDetails = SigningDetails.UNKNOWN;
                return;
            }
        }
        PackageManagerService.reportSettingsProblem(5, "Error in package manager settings: <sigs> has no count at " + parser.getPositionDescription());
        XmlUtils.skipCurrentTag(parser);
    }

    /* JADX WARN: Code restructure failed: missing block: B:34:0x00a1, code lost:
        com.android.server.pm.PackageManagerService.reportSettingsProblem(5, "Error in package manager settings: <cert> index " + r1 + " is out of bounds at " + r19.getPositionDescription());
     */
    /* JADX WARN: Code restructure failed: missing block: B:35:0x00e0, code lost:
        throw new org.xmlpull.v1.XmlPullParserException("Error in package manager settings: <cert> index " + r1 + " is out of bounds!");
     */
    /* JADX WARN: Code restructure failed: missing block: B:96:0x032f, code lost:
        return r15;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private int readCertsListXml(TypedXmlPullParser parser, ArrayList<Signature> readSignatures, ArrayList<Signature> signatures, int count, boolean isPastSigs, SigningDetails.Builder builder) throws IOException, XmlPullParserException {
        String str;
        int i;
        int pastSigsCount;
        TypedXmlPullParser typedXmlPullParser = parser;
        int outerDepth = parser.getDepth();
        SigningDetails.Builder builder2 = builder;
        int pos = 0;
        while (true) {
            int type = parser.next();
            if (type != 1 && (type != 3 || parser.getDepth() > outerDepth)) {
                if (type != 3 && type != 4) {
                    String tagName = parser.getName();
                    if (tagName.equals("cert")) {
                        if (pos < count) {
                            int index = typedXmlPullParser.getAttributeInt((String) null, AssistDataRequester.KEY_RECEIVER_EXTRA_INDEX, -1);
                            if (index != -1) {
                                boolean signatureParsed = false;
                                try {
                                    byte[] key = typedXmlPullParser.getAttributeBytesHex((String) null, "key", (byte[]) null);
                                    if (key == null) {
                                        if (index < 0 || index >= readSignatures.size()) {
                                            break;
                                        }
                                        Signature sig = readSignatures.get(index);
                                        if (sig != null) {
                                            if (isPastSigs) {
                                                signatures.add(new Signature(sig));
                                            } else {
                                                signatures.add(sig);
                                            }
                                            signatureParsed = true;
                                        } else {
                                            PackageManagerService.reportSettingsProblem(5, "Error in package manager settings: <cert> index " + index + " is not defined at " + parser.getPositionDescription());
                                            throw new XmlPullParserException("More than one app use the same userId!");
                                        }
                                    } else {
                                        Signature sig2 = new Signature(key);
                                        while (readSignatures.size() < index) {
                                            readSignatures.add(null);
                                        }
                                        readSignatures.add(sig2);
                                        signatures.add(sig2);
                                        signatureParsed = true;
                                    }
                                } catch (NumberFormatException e) {
                                    PackageManagerService.reportSettingsProblem(5, "Error in package manager settings: <cert> index " + index + " is not a number at " + parser.getPositionDescription());
                                } catch (IllegalArgumentException e2) {
                                    PackageManagerService.reportSettingsProblem(5, "Error in package manager settings: <cert> index " + index + " has an invalid signature at " + parser.getPositionDescription() + ": " + e2.getMessage());
                                }
                                if (isPastSigs) {
                                    int flagsValue = typedXmlPullParser.getAttributeInt((String) null, "flags", -1);
                                    if (flagsValue != -1) {
                                        if (signatureParsed) {
                                            try {
                                                signatures.get(signatures.size() - 1).setFlags(flagsValue);
                                            } catch (NumberFormatException e3) {
                                                PackageManagerService.reportSettingsProblem(5, "Error in package manager settings: <cert> flags " + flagsValue + " is not a number at " + parser.getPositionDescription());
                                            }
                                        } else {
                                            PackageManagerService.reportSettingsProblem(5, "Error in package manager settings: signature not available at index " + pos + " to set flags at " + parser.getPositionDescription());
                                        }
                                    } else {
                                        PackageManagerService.reportSettingsProblem(5, "Error in package manager settings: <cert> has no flags at " + parser.getPositionDescription());
                                    }
                                }
                            } else {
                                PackageManagerService.reportSettingsProblem(5, "Error in package manager settings: <cert> has no index at " + parser.getPositionDescription());
                            }
                        } else {
                            PackageManagerService.reportSettingsProblem(5, "Error in package manager settings: too many <cert> tags, expected " + count + " at " + parser.getPositionDescription());
                        }
                        pos++;
                        XmlUtils.skipCurrentTag(parser);
                    } else if (tagName.equals("pastSigs")) {
                        if (!isPastSigs) {
                            int pastSigsCount2 = typedXmlPullParser.getAttributeInt((String) null, AssistDataRequester.KEY_RECEIVER_EXTRA_COUNT, -1);
                            if (pastSigsCount2 == -1) {
                                PackageManagerService.reportSettingsProblem(5, "Error in package manager settings: <pastSigs> has no count at " + parser.getPositionDescription());
                                XmlUtils.skipCurrentTag(parser);
                            } else {
                                try {
                                    ArrayList<Signature> pastSignatureList = new ArrayList<>();
                                    str = " is not a number at ";
                                    try {
                                        int pastSigsPos = readCertsListXml(parser, readSignatures, pastSignatureList, pastSigsCount2, true, builder2);
                                        Signature[] pastSignatures = (Signature[]) pastSignatureList.toArray(new Signature[pastSignatureList.size()]);
                                        builder2 = builder2.setPastSigningCertificates(pastSignatures);
                                        pastSigsCount = pastSigsCount2;
                                        if (pastSigsPos < pastSigsCount) {
                                            try {
                                                i = 5;
                                                try {
                                                    PackageManagerService.reportSettingsProblem(5, "Error in package manager settings: <pastSigs> count does not match number of <cert> entries " + parser.getPositionDescription());
                                                } catch (NumberFormatException e4) {
                                                    PackageManagerService.reportSettingsProblem(i, "Error in package manager settings: <pastSigs> count " + pastSigsCount + str + parser.getPositionDescription());
                                                    typedXmlPullParser = parser;
                                                }
                                            } catch (NumberFormatException e5) {
                                                i = 5;
                                            }
                                        }
                                    } catch (NumberFormatException e6) {
                                        pastSigsCount = pastSigsCount2;
                                        i = 5;
                                    }
                                } catch (NumberFormatException e7) {
                                    str = " is not a number at ";
                                    i = 5;
                                    pastSigsCount = pastSigsCount2;
                                }
                            }
                        } else {
                            PackageManagerService.reportSettingsProblem(5, "<pastSigs> encountered multiple times under the same <sigs> at " + parser.getPositionDescription());
                            XmlUtils.skipCurrentTag(parser);
                        }
                    } else {
                        PackageManagerService.reportSettingsProblem(5, "Unknown element under <sigs>: " + parser.getName());
                        XmlUtils.skipCurrentTag(parser);
                    }
                    typedXmlPullParser = parser;
                }
                typedXmlPullParser = parser;
            }
        }
    }

    public String toString() {
        StringBuilder buf = new StringBuilder(128);
        buf.append("PackageSignatures{");
        buf.append(Integer.toHexString(System.identityHashCode(this)));
        buf.append(" version:");
        buf.append(this.mSigningDetails.getSignatureSchemeVersion());
        buf.append(", signatures:[");
        if (this.mSigningDetails.getSignatures() != null) {
            for (int i = 0; i < this.mSigningDetails.getSignatures().length; i++) {
                if (i > 0) {
                    buf.append(", ");
                }
                buf.append(Integer.toHexString(this.mSigningDetails.getSignatures()[i].hashCode()));
            }
        }
        buf.append("]");
        buf.append(", past signatures:[");
        if (this.mSigningDetails.getPastSigningCertificates() != null) {
            for (int i2 = 0; i2 < this.mSigningDetails.getPastSigningCertificates().length; i2++) {
                if (i2 > 0) {
                    buf.append(", ");
                }
                buf.append(Integer.toHexString(this.mSigningDetails.getPastSigningCertificates()[i2].hashCode()));
                buf.append(" flags: ");
                buf.append(Integer.toHexString(this.mSigningDetails.getPastSigningCertificates()[i2].getFlags()));
            }
        }
        buf.append("]}");
        return buf.toString();
    }
}
