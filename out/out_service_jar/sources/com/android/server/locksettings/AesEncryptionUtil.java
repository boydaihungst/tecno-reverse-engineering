package com.android.server.locksettings;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
/* loaded from: classes.dex */
class AesEncryptionUtil {
    private static final String CIPHER_ALGO = "AES/GCM/NoPadding";

    private AesEncryptionUtil() {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static byte[] decrypt(SecretKey key, DataInputStream cipherStream) throws IOException {
        Objects.requireNonNull(key);
        Objects.requireNonNull(cipherStream);
        int ivSize = cipherStream.readInt();
        if (ivSize < 0 || ivSize > 32) {
            throw new IOException("IV out of range: " + ivSize);
        }
        byte[] iv = new byte[ivSize];
        cipherStream.readFully(iv);
        int rawCipherTextSize = cipherStream.readInt();
        if (rawCipherTextSize < 0) {
            throw new IOException("Invalid cipher text size: " + rawCipherTextSize);
        }
        byte[] rawCipherText = new byte[rawCipherTextSize];
        cipherStream.readFully(rawCipherText);
        try {
            Cipher c = Cipher.getInstance(CIPHER_ALGO);
            c.init(2, key, new GCMParameterSpec(128, iv));
            byte[] plainText = c.doFinal(rawCipherText);
            return plainText;
        } catch (InvalidAlgorithmParameterException | InvalidKeyException | NoSuchAlgorithmException | BadPaddingException | IllegalBlockSizeException | NoSuchPaddingException e) {
            throw new IOException("Could not decrypt cipher text", e);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static byte[] decrypt(SecretKey key, byte[] cipherText) throws IOException {
        Objects.requireNonNull(key);
        Objects.requireNonNull(cipherText);
        DataInputStream cipherStream = new DataInputStream(new ByteArrayInputStream(cipherText));
        return decrypt(key, cipherStream);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static byte[] encrypt(SecretKey key, byte[] plainText) throws IOException {
        Objects.requireNonNull(key);
        Objects.requireNonNull(plainText);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(bos);
        try {
            Cipher cipher = Cipher.getInstance(CIPHER_ALGO);
            cipher.init(1, key);
            byte[] cipherText = cipher.doFinal(plainText);
            byte[] iv = cipher.getIV();
            dos.writeInt(iv.length);
            dos.write(iv);
            dos.writeInt(cipherText.length);
            dos.write(cipherText);
            return bos.toByteArray();
        } catch (InvalidKeyException | NoSuchAlgorithmException | BadPaddingException | IllegalBlockSizeException | NoSuchPaddingException e) {
            throw new IOException("Could not encrypt input data", e);
        }
    }
}
