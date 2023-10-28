package com.android.server.locksettings.recoverablekeystore.serialization;

import android.security.keystore.recovery.KeyChainProtectionParams;
import android.security.keystore.recovery.KeyChainSnapshot;
import android.security.keystore.recovery.KeyDerivationParams;
import android.security.keystore.recovery.WrappedApplicationKey;
import android.util.Base64;
import android.util.TypedXmlSerializer;
import android.util.Xml;
import java.io.IOException;
import java.io.OutputStream;
import java.security.cert.CertPath;
import java.security.cert.CertificateEncodingException;
import java.util.List;
/* loaded from: classes.dex */
public class KeyChainSnapshotSerializer {
    public static void serialize(KeyChainSnapshot keyChainSnapshot, OutputStream outputStream) throws IOException, CertificateEncodingException {
        TypedXmlSerializer xmlSerializer = Xml.resolveSerializer(outputStream);
        xmlSerializer.startDocument((String) null, (Boolean) null);
        xmlSerializer.startTag(KeyChainSnapshotSchema.NAMESPACE, "keyChainSnapshot");
        writeKeyChainSnapshotProperties(xmlSerializer, keyChainSnapshot);
        writeKeyChainProtectionParams(xmlSerializer, keyChainSnapshot.getKeyChainProtectionParams());
        writeApplicationKeys(xmlSerializer, keyChainSnapshot.getWrappedApplicationKeys());
        xmlSerializer.endTag(KeyChainSnapshotSchema.NAMESPACE, "keyChainSnapshot");
        xmlSerializer.endDocument();
    }

    private static void writeApplicationKeys(TypedXmlSerializer xmlSerializer, List<WrappedApplicationKey> wrappedApplicationKeys) throws IOException {
        xmlSerializer.startTag(KeyChainSnapshotSchema.NAMESPACE, "applicationKeysList");
        for (WrappedApplicationKey key : wrappedApplicationKeys) {
            xmlSerializer.startTag(KeyChainSnapshotSchema.NAMESPACE, "applicationKey");
            writeApplicationKeyProperties(xmlSerializer, key);
            xmlSerializer.endTag(KeyChainSnapshotSchema.NAMESPACE, "applicationKey");
        }
        xmlSerializer.endTag(KeyChainSnapshotSchema.NAMESPACE, "applicationKeysList");
    }

    private static void writeApplicationKeyProperties(TypedXmlSerializer xmlSerializer, WrappedApplicationKey applicationKey) throws IOException {
        writePropertyTag(xmlSerializer, "alias", applicationKey.getAlias());
        writePropertyTag(xmlSerializer, "keyMaterial", applicationKey.getEncryptedKeyMaterial());
        writePropertyTag(xmlSerializer, "keyMetadata", applicationKey.getMetadata());
    }

    private static void writeKeyChainProtectionParams(TypedXmlSerializer xmlSerializer, List<KeyChainProtectionParams> keyChainProtectionParamsList) throws IOException {
        xmlSerializer.startTag(KeyChainSnapshotSchema.NAMESPACE, "keyChainProtectionParamsList");
        for (KeyChainProtectionParams keyChainProtectionParams : keyChainProtectionParamsList) {
            xmlSerializer.startTag(KeyChainSnapshotSchema.NAMESPACE, "keyChainProtectionParams");
            writeKeyChainProtectionParamsProperties(xmlSerializer, keyChainProtectionParams);
            xmlSerializer.endTag(KeyChainSnapshotSchema.NAMESPACE, "keyChainProtectionParams");
        }
        xmlSerializer.endTag(KeyChainSnapshotSchema.NAMESPACE, "keyChainProtectionParamsList");
    }

    private static void writeKeyChainProtectionParamsProperties(TypedXmlSerializer xmlSerializer, KeyChainProtectionParams keyChainProtectionParams) throws IOException {
        writePropertyTag(xmlSerializer, "userSecretType", keyChainProtectionParams.getUserSecretType());
        writePropertyTag(xmlSerializer, "lockScreenUiType", keyChainProtectionParams.getLockScreenUiFormat());
        writeKeyDerivationParams(xmlSerializer, keyChainProtectionParams.getKeyDerivationParams());
    }

    private static void writeKeyDerivationParams(TypedXmlSerializer xmlSerializer, KeyDerivationParams keyDerivationParams) throws IOException {
        xmlSerializer.startTag(KeyChainSnapshotSchema.NAMESPACE, "keyDerivationParams");
        writeKeyDerivationParamsProperties(xmlSerializer, keyDerivationParams);
        xmlSerializer.endTag(KeyChainSnapshotSchema.NAMESPACE, "keyDerivationParams");
    }

    private static void writeKeyDerivationParamsProperties(TypedXmlSerializer xmlSerializer, KeyDerivationParams keyDerivationParams) throws IOException {
        writePropertyTag(xmlSerializer, "algorithm", keyDerivationParams.getAlgorithm());
        writePropertyTag(xmlSerializer, "salt", keyDerivationParams.getSalt());
        writePropertyTag(xmlSerializer, "memoryDifficulty", keyDerivationParams.getMemoryDifficulty());
    }

    private static void writeKeyChainSnapshotProperties(TypedXmlSerializer xmlSerializer, KeyChainSnapshot keyChainSnapshot) throws IOException, CertificateEncodingException {
        writePropertyTag(xmlSerializer, "snapshotVersion", keyChainSnapshot.getSnapshotVersion());
        writePropertyTag(xmlSerializer, "maxAttempts", keyChainSnapshot.getMaxAttempts());
        writePropertyTag(xmlSerializer, "counterId", keyChainSnapshot.getCounterId());
        writePropertyTag(xmlSerializer, "recoveryKeyMaterial", keyChainSnapshot.getEncryptedRecoveryKeyBlob());
        writePropertyTag(xmlSerializer, "serverParams", keyChainSnapshot.getServerParams());
        writePropertyTag(xmlSerializer, "thmCertPath", keyChainSnapshot.getTrustedHardwareCertPath());
    }

    private static void writePropertyTag(TypedXmlSerializer xmlSerializer, String propertyName, long propertyValue) throws IOException {
        xmlSerializer.startTag(KeyChainSnapshotSchema.NAMESPACE, propertyName);
        xmlSerializer.text(Long.toString(propertyValue));
        xmlSerializer.endTag(KeyChainSnapshotSchema.NAMESPACE, propertyName);
    }

    private static void writePropertyTag(TypedXmlSerializer xmlSerializer, String propertyName, String propertyValue) throws IOException {
        xmlSerializer.startTag(KeyChainSnapshotSchema.NAMESPACE, propertyName);
        xmlSerializer.text(propertyValue);
        xmlSerializer.endTag(KeyChainSnapshotSchema.NAMESPACE, propertyName);
    }

    private static void writePropertyTag(TypedXmlSerializer xmlSerializer, String propertyName, byte[] propertyValue) throws IOException {
        if (propertyValue == null) {
            return;
        }
        xmlSerializer.startTag(KeyChainSnapshotSchema.NAMESPACE, propertyName);
        xmlSerializer.text(Base64.encodeToString(propertyValue, 0));
        xmlSerializer.endTag(KeyChainSnapshotSchema.NAMESPACE, propertyName);
    }

    private static void writePropertyTag(TypedXmlSerializer xmlSerializer, String propertyName, CertPath certPath) throws IOException, CertificateEncodingException {
        writePropertyTag(xmlSerializer, propertyName, certPath.getEncoded("PkiPath"));
    }

    private KeyChainSnapshotSerializer() {
    }
}
