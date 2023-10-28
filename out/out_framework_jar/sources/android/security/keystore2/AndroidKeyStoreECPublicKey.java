package android.security.keystore2;

import android.security.KeyStoreSecurityLevel;
import android.security.keystore.KeyProperties;
import android.system.keystore2.KeyDescriptor;
import android.system.keystore2.KeyMetadata;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
/* loaded from: classes3.dex */
public class AndroidKeyStoreECPublicKey extends AndroidKeyStorePublicKey implements ECPublicKey {
    private final ECParameterSpec mParams;
    private final ECPoint mW;

    public AndroidKeyStoreECPublicKey(KeyDescriptor descriptor, KeyMetadata metadata, byte[] x509EncodedForm, KeyStoreSecurityLevel securityLevel, ECParameterSpec params, ECPoint w) {
        super(descriptor, metadata, x509EncodedForm, KeyProperties.KEY_ALGORITHM_EC, securityLevel);
        this.mParams = params;
        this.mW = w;
    }

    public AndroidKeyStoreECPublicKey(KeyDescriptor descriptor, KeyMetadata metadata, KeyStoreSecurityLevel securityLevel, ECPublicKey info) {
        this(descriptor, metadata, info.getEncoded(), securityLevel, info.getParams(), info.getW());
        if (!"X.509".equalsIgnoreCase(info.getFormat())) {
            throw new IllegalArgumentException("Unsupported key export format: " + info.getFormat());
        }
    }

    @Override // android.security.keystore2.AndroidKeyStorePublicKey
    public AndroidKeyStorePrivateKey getPrivateKey() {
        return new AndroidKeyStoreECPrivateKey(getUserKeyDescriptor(), getKeyIdDescriptor().nspace, getAuthorizations(), getSecurityLevel(), this.mParams);
    }

    @Override // java.security.interfaces.ECKey
    public ECParameterSpec getParams() {
        return this.mParams;
    }

    @Override // java.security.interfaces.ECPublicKey
    public ECPoint getW() {
        return this.mW;
    }
}
