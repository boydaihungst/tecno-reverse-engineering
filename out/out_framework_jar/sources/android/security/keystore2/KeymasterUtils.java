package android.security.keystore2;

import android.security.keymaster.KeymasterArguments;
import android.security.keystore.KeyProperties;
import com.android.internal.util.ArrayUtils;
import java.security.ProviderException;
/* loaded from: classes3.dex */
public abstract class KeymasterUtils {
    private KeymasterUtils() {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static int getDigestOutputSizeBits(int keymasterDigest) {
        switch (keymasterDigest) {
            case 0:
                return -1;
            case 1:
                return 128;
            case 2:
                return 160;
            case 3:
                return 224;
            case 4:
                return 256;
            case 5:
                return 384;
            case 6:
                return 512;
            default:
                throw new IllegalArgumentException("Unknown digest: " + keymasterDigest);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static boolean isKeymasterBlockModeIndCpaCompatibleWithSymmetricCrypto(int keymasterBlockMode) {
        switch (keymasterBlockMode) {
            case 1:
                return false;
            case 2:
            case 3:
            case 32:
                return true;
            default:
                throw new IllegalArgumentException("Unsupported block mode: " + keymasterBlockMode);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static boolean isKeymasterPaddingSchemeIndCpaCompatibleWithAsymmetricCrypto(int keymasterPadding) {
        switch (keymasterPadding) {
            case 1:
                return false;
            case 2:
            case 4:
                return true;
            case 3:
            default:
                throw new IllegalArgumentException("Unsupported asymmetric encryption padding scheme: " + keymasterPadding);
        }
    }

    public static void addMinMacLengthAuthorizationIfNecessary(KeymasterArguments args, int keymasterAlgorithm, int[] keymasterBlockModes, int[] keymasterDigests) {
        switch (keymasterAlgorithm) {
            case 32:
                if (ArrayUtils.contains(keymasterBlockModes, 32)) {
                    args.addUnsignedInt(805306376, 96L);
                    return;
                }
                return;
            case 128:
                if (keymasterDigests.length != 1) {
                    throw new ProviderException("Unsupported number of authorized digests for HMAC key: " + keymasterDigests.length + ". Exactly one digest must be authorized");
                }
                int keymasterDigest = keymasterDigests[0];
                int digestOutputSizeBits = getDigestOutputSizeBits(keymasterDigest);
                if (digestOutputSizeBits != -1) {
                    args.addUnsignedInt(805306376, digestOutputSizeBits);
                    return;
                }
                throw new ProviderException("HMAC key authorized for unsupported digest: " + KeyProperties.Digest.fromKeymaster(keymasterDigest));
            default:
                return;
        }
    }
}
