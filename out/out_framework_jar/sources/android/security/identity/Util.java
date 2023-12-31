package android.security.identity;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECPoint;
import java.util.Collection;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
/* loaded from: classes3.dex */
public class Util {
    private static final String TAG = "Util";

    static int[] integerCollectionToArray(Collection<Integer> collection) {
        int[] result = new int[collection.size()];
        int n = 0;
        for (Integer num : collection) {
            int item = num.intValue();
            result[n] = item;
            n++;
        }
        return result;
    }

    static byte[] stripLeadingZeroes(byte[] value) {
        int n = 0;
        while (n < value.length && value[n] == 0) {
            n++;
        }
        int newLen = value.length - n;
        byte[] ret = new byte[newLen];
        int m = 0;
        while (n < value.length) {
            ret[m] = value[n];
            m++;
            n++;
        }
        return ret;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static byte[] publicKeyEncodeUncompressedForm(PublicKey publicKey) {
        ECPoint w = ((ECPublicKey) publicKey).getW();
        byte[] x = stripLeadingZeroes(w.getAffineX().toByteArray());
        byte[] y = stripLeadingZeroes(w.getAffineY().toByteArray());
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            baos.write(4);
            baos.write(x);
            baos.write(y);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Unexpected IOException", e);
        }
    }

    /* JADX WARN: Removed duplicated region for block: B:15:0x0052 A[Catch: InvalidKeyException -> 0x0062, LOOP:0: B:13:0x003f->B:15:0x0052, LOOP_END, TryCatch #1 {InvalidKeyException -> 0x0062, blocks: (B:7:0x0011, B:10:0x0015, B:12:0x002c, B:13:0x003f, B:15:0x0052, B:16:0x005b, B:11:0x001e), top: B:28:0x0011 }] */
    /* JADX WARN: Removed duplicated region for block: B:30:0x005b A[SYNTHETIC] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public static byte[] computeHkdf(String macAlgorithm, byte[] ikm, byte[] salt, byte[] info, int size) {
        byte[] result;
        int ctr;
        int pos;
        byte[] digest;
        try {
            Mac mac = Mac.getInstance(macAlgorithm);
            if (size > mac.getMacLength() * 255) {
                throw new RuntimeException("size too large");
            }
            if (salt != null) {
                try {
                    if (salt.length != 0) {
                        mac.init(new SecretKeySpec(salt, macAlgorithm));
                        byte[] prk = mac.doFinal(ikm);
                        result = new byte[size];
                        ctr = 1;
                        pos = 0;
                        mac.init(new SecretKeySpec(prk, macAlgorithm));
                        digest = new byte[0];
                        while (true) {
                            mac.update(digest);
                            mac.update(info);
                            mac.update((byte) ctr);
                            digest = mac.doFinal();
                            if (digest.length + pos >= size) {
                                System.arraycopy(digest, 0, result, pos, digest.length);
                                pos += digest.length;
                                ctr++;
                            } else {
                                System.arraycopy(digest, 0, result, pos, size - pos);
                                return result;
                            }
                        }
                    }
                } catch (InvalidKeyException e) {
                    throw new RuntimeException("Error MACing", e);
                }
            }
            mac.init(new SecretKeySpec(new byte[mac.getMacLength()], macAlgorithm));
            byte[] prk2 = mac.doFinal(ikm);
            result = new byte[size];
            ctr = 1;
            pos = 0;
            mac.init(new SecretKeySpec(prk2, macAlgorithm));
            digest = new byte[0];
            while (true) {
                mac.update(digest);
                mac.update(info);
                mac.update((byte) ctr);
                digest = mac.doFinal();
                if (digest.length + pos >= size) {
                }
                System.arraycopy(digest, 0, result, pos, digest.length);
                pos += digest.length;
                ctr++;
            }
        } catch (NoSuchAlgorithmException e2) {
            throw new RuntimeException("No such algorithm: " + macAlgorithm, e2);
        }
    }

    private Util() {
    }
}
