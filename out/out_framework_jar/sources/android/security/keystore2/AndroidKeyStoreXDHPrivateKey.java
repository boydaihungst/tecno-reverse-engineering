package android.security.keystore2;

import android.security.KeyStoreSecurityLevel;
import android.system.keystore2.Authorization;
import android.system.keystore2.KeyDescriptor;
import java.security.interfaces.EdECKey;
import java.security.spec.NamedParameterSpec;
/* loaded from: classes3.dex */
public class AndroidKeyStoreXDHPrivateKey extends AndroidKeyStorePrivateKey implements EdECKey {
    public AndroidKeyStoreXDHPrivateKey(KeyDescriptor descriptor, long keyId, Authorization[] authorizations, String algorithm, KeyStoreSecurityLevel securityLevel) {
        super(descriptor, keyId, authorizations, algorithm, securityLevel);
    }

    public NamedParameterSpec getParams() {
        return NamedParameterSpec.X25519;
    }
}
