package com.android.server.devicepolicy;

import android.content.ComponentName;
import android.os.Environment;
import android.text.TextUtils;
import android.util.AtomicFile;
import android.util.Slog;
import android.util.TypedXmlPullParser;
import android.util.TypedXmlSerializer;
import android.util.Xml;
import com.android.internal.util.Preconditions;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Objects;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes.dex */
class TransferOwnershipMetadataManager {
    static final String ADMIN_TYPE_DEVICE_OWNER = "device-owner";
    static final String ADMIN_TYPE_PROFILE_OWNER = "profile-owner";
    public static final String OWNER_TRANSFER_METADATA_XML = "owner-transfer-metadata.xml";
    private static final String TAG = TransferOwnershipMetadataManager.class.getName();
    static final String TAG_ADMIN_TYPE = "admin-type";
    static final String TAG_SOURCE_COMPONENT = "source-component";
    static final String TAG_TARGET_COMPONENT = "target-component";
    static final String TAG_USER_ID = "user-id";
    private final Injector mInjector;

    /* JADX INFO: Access modifiers changed from: package-private */
    public TransferOwnershipMetadataManager() {
        this(new Injector());
    }

    TransferOwnershipMetadataManager(Injector injector) {
        this.mInjector = injector;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean saveMetadataFile(Metadata params) {
        File transferOwnershipMetadataFile = new File(this.mInjector.getOwnerTransferMetadataDir(), OWNER_TRANSFER_METADATA_XML);
        AtomicFile atomicFile = new AtomicFile(transferOwnershipMetadataFile);
        FileOutputStream stream = null;
        try {
            stream = atomicFile.startWrite();
            TypedXmlSerializer serializer = Xml.resolveSerializer(stream);
            serializer.startDocument((String) null, true);
            insertSimpleTag(serializer, TAG_USER_ID, Integer.toString(params.userId));
            insertSimpleTag(serializer, TAG_SOURCE_COMPONENT, params.sourceComponent.flattenToString());
            insertSimpleTag(serializer, TAG_TARGET_COMPONENT, params.targetComponent.flattenToString());
            insertSimpleTag(serializer, TAG_ADMIN_TYPE, params.adminType);
            serializer.endDocument();
            atomicFile.finishWrite(stream);
            return true;
        } catch (IOException e) {
            Slog.e(TAG, "Caught exception while trying to save Owner Transfer Params to file " + transferOwnershipMetadataFile, e);
            transferOwnershipMetadataFile.delete();
            atomicFile.failWrite(stream);
            return false;
        }
    }

    private void insertSimpleTag(TypedXmlSerializer serializer, String tagName, String value) throws IOException {
        serializer.startTag((String) null, tagName);
        serializer.text(value);
        serializer.endTag((String) null, tagName);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Metadata loadMetadataFile() {
        File transferOwnershipMetadataFile = new File(this.mInjector.getOwnerTransferMetadataDir(), OWNER_TRANSFER_METADATA_XML);
        if (transferOwnershipMetadataFile.exists()) {
            Slog.d(TAG, "Loading TransferOwnershipMetadataManager from " + transferOwnershipMetadataFile);
            try {
                FileInputStream stream = new FileInputStream(transferOwnershipMetadataFile);
                TypedXmlPullParser parser = Xml.resolvePullParser(stream);
                Metadata parseMetadataFile = parseMetadataFile(parser);
                stream.close();
                return parseMetadataFile;
            } catch (IOException | IllegalArgumentException | XmlPullParserException e) {
                Slog.e(TAG, "Caught exception while trying to load the owner transfer params from file " + transferOwnershipMetadataFile, e);
                return null;
            }
        }
        return null;
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Code restructure failed: missing block: B:26:0x0053, code lost:
        if (r8.equals(com.android.server.devicepolicy.TransferOwnershipMetadataManager.TAG_TARGET_COMPONENT) != false) goto L22;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private Metadata parseMetadataFile(TypedXmlPullParser parser) throws XmlPullParserException, IOException {
        int outerDepth = parser.getDepth();
        int userId = 0;
        String adminComponent = null;
        String targetComponent = null;
        String adminType = null;
        while (true) {
            int type = parser.next();
            char c = 1;
            if (type != 1 && (type != 3 || parser.getDepth() > outerDepth)) {
                if (type != 3 && type != 4) {
                    String name = parser.getName();
                    switch (name.hashCode()) {
                        case -337219647:
                            break;
                        case -147180963:
                            if (name.equals(TAG_USER_ID)) {
                                c = 0;
                                break;
                            }
                            c = 65535;
                            break;
                        case 281362891:
                            if (name.equals(TAG_SOURCE_COMPONENT)) {
                                c = 2;
                                break;
                            }
                            c = 65535;
                            break;
                        case 641951480:
                            if (name.equals(TAG_ADMIN_TYPE)) {
                                c = 3;
                                break;
                            }
                            c = 65535;
                            break;
                        default:
                            c = 65535;
                            break;
                    }
                    switch (c) {
                        case 0:
                            parser.next();
                            userId = Integer.parseInt(parser.getText());
                            continue;
                        case 1:
                            parser.next();
                            targetComponent = parser.getText();
                            continue;
                        case 2:
                            parser.next();
                            adminComponent = parser.getText();
                            continue;
                        case 3:
                            parser.next();
                            adminType = parser.getText();
                            continue;
                    }
                }
            }
        }
        return new Metadata(adminComponent, targetComponent, userId, adminType);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void deleteMetadataFile() {
        new File(this.mInjector.getOwnerTransferMetadataDir(), OWNER_TRANSFER_METADATA_XML).delete();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean metadataFileExists() {
        return new File(this.mInjector.getOwnerTransferMetadataDir(), OWNER_TRANSFER_METADATA_XML).exists();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class Metadata {
        final String adminType;
        final ComponentName sourceComponent;
        final ComponentName targetComponent;
        final int userId;

        /* JADX INFO: Access modifiers changed from: package-private */
        public Metadata(ComponentName sourceComponent, ComponentName targetComponent, int userId, String adminType) {
            this.sourceComponent = sourceComponent;
            this.targetComponent = targetComponent;
            Objects.requireNonNull(sourceComponent);
            Objects.requireNonNull(targetComponent);
            Preconditions.checkStringNotEmpty(adminType);
            this.userId = userId;
            this.adminType = adminType;
        }

        Metadata(String flatSourceComponent, String flatTargetComponent, int userId, String adminType) {
            this(unflattenComponentUnchecked(flatSourceComponent), unflattenComponentUnchecked(flatTargetComponent), userId, adminType);
        }

        private static ComponentName unflattenComponentUnchecked(String flatComponent) {
            Objects.requireNonNull(flatComponent);
            return ComponentName.unflattenFromString(flatComponent);
        }

        public boolean equals(Object obj) {
            if (obj instanceof Metadata) {
                Metadata params = (Metadata) obj;
                return this.userId == params.userId && this.sourceComponent.equals(params.sourceComponent) && this.targetComponent.equals(params.targetComponent) && TextUtils.equals(this.adminType, params.adminType);
            }
            return false;
        }

        public int hashCode() {
            int hashCode = (1 * 31) + this.userId;
            return (((((hashCode * 31) + this.sourceComponent.hashCode()) * 31) + this.targetComponent.hashCode()) * 31) + this.adminType.hashCode();
        }
    }

    /* loaded from: classes.dex */
    static class Injector {
        Injector() {
        }

        public File getOwnerTransferMetadataDir() {
            return Environment.getDataSystemDirectory();
        }
    }
}
