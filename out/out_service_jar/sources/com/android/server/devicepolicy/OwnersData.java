package com.android.server.devicepolicy;

import android.app.admin.SystemUpdateInfo;
import android.app.admin.SystemUpdatePolicy;
import android.content.ComponentName;
import android.util.ArrayMap;
import android.util.AtomicFile;
import android.util.IndentingPrintWriter;
import android.util.Slog;
import android.util.TypedXmlPullParser;
import android.util.TypedXmlSerializer;
import android.util.Xml;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import libcore.io.IoUtils;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes.dex */
class OwnersData {
    private static final String ATTR_CAN_ACCESS_DEVICE_IDS = "canAccessDeviceIds";
    private static final String ATTR_COMPONENT_NAME = "component";
    private static final String ATTR_DEVICE_OWNER_TYPE_VALUE = "value";
    private static final String ATTR_FREEZE_RECORD_END = "end";
    private static final String ATTR_FREEZE_RECORD_START = "start";
    private static final String ATTR_NAME = "name";
    private static final String ATTR_PACKAGE = "package";
    private static final String ATTR_PROFILE_OWNER_OF_ORG_OWNED_DEVICE = "isPoOrganizationOwnedDevice";
    private static final String ATTR_REMOTE_BUGREPORT_HASH = "remoteBugreportHash";
    private static final String ATTR_REMOTE_BUGREPORT_URI = "remoteBugreportUri";
    private static final String ATTR_SIZE = "size";
    private static final String ATTR_USERID = "userId";
    private static final boolean DEBUG = false;
    private static final String DEVICE_OWNER_XML = "device_owner_2.xml";
    private static final String PROFILE_OWNER_XML = "profile_owner.xml";
    private static final String TAG = "DevicePolicyManagerService";
    private static final String TAG_DEVICE_OWNER = "device-owner";
    private static final String TAG_DEVICE_OWNER_CONTEXT = "device-owner-context";
    private static final String TAG_DEVICE_OWNER_PROTECTED_PACKAGES = "device-owner-protected-packages";
    private static final String TAG_DEVICE_OWNER_TYPE = "device-owner-type";
    private static final String TAG_FREEZE_PERIOD_RECORD = "freeze-record";
    private static final String TAG_PENDING_OTA_INFO = "pending-ota-info";
    private static final String TAG_PROFILE_OWNER = "profile-owner";
    private static final String TAG_ROOT = "root";
    private static final String TAG_SYSTEM_UPDATE_POLICY = "system-update-policy";
    OwnerInfo mDeviceOwner;
    @Deprecated
    ArrayMap<String, List<String>> mDeviceOwnerProtectedPackages;
    private final PolicyPathProvider mPathProvider;
    LocalDate mSystemUpdateFreezeEnd;
    LocalDate mSystemUpdateFreezeStart;
    SystemUpdateInfo mSystemUpdateInfo;
    SystemUpdatePolicy mSystemUpdatePolicy;
    int mDeviceOwnerUserId = -10000;
    final ArrayMap<String, Integer> mDeviceOwnerTypes = new ArrayMap<>();
    final ArrayMap<Integer, OwnerInfo> mProfileOwners = new ArrayMap<>();

    /* JADX INFO: Access modifiers changed from: package-private */
    public OwnersData(PolicyPathProvider pathProvider) {
        this.mPathProvider = pathProvider;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void load(int[] allUsers) {
        new DeviceOwnerReadWriter().readFromFileLocked();
        for (int userId : allUsers) {
            new ProfileOwnerReadWriter(userId).readFromFileLocked();
        }
        OwnerInfo profileOwner = this.mProfileOwners.get(Integer.valueOf(this.mDeviceOwnerUserId));
        ComponentName admin = profileOwner != null ? profileOwner.admin : null;
        if (this.mDeviceOwner != null && admin != null) {
            Slog.w(TAG, String.format("User %d has both DO and PO, which is not supported", Integer.valueOf(this.mDeviceOwnerUserId)));
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean writeDeviceOwner() {
        return new DeviceOwnerReadWriter().writeToFileLocked();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean writeProfileOwner(int userId) {
        return new ProfileOwnerReadWriter(userId).writeToFileLocked();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dump(IndentingPrintWriter pw) {
        boolean needBlank = false;
        if (this.mDeviceOwner != null) {
            pw.println("Device Owner: ");
            pw.increaseIndent();
            this.mDeviceOwner.dump(pw);
            pw.println("User ID: " + this.mDeviceOwnerUserId);
            pw.decreaseIndent();
            needBlank = true;
        }
        if (this.mSystemUpdatePolicy != null) {
            if (needBlank) {
                pw.println();
            }
            pw.println("System Update Policy: " + this.mSystemUpdatePolicy);
            needBlank = true;
        }
        ArrayMap<Integer, OwnerInfo> arrayMap = this.mProfileOwners;
        if (arrayMap != null) {
            for (Map.Entry<Integer, OwnerInfo> entry : arrayMap.entrySet()) {
                if (needBlank) {
                    pw.println();
                }
                pw.println("Profile Owner (User " + entry.getKey() + "): ");
                pw.increaseIndent();
                entry.getValue().dump(pw);
                pw.decreaseIndent();
                needBlank = true;
            }
        }
        if (this.mSystemUpdateInfo != null) {
            if (needBlank) {
                pw.println();
            }
            pw.println("Pending System Update: " + this.mSystemUpdateInfo);
            needBlank = true;
        }
        if (this.mSystemUpdateFreezeStart != null || this.mSystemUpdateFreezeEnd != null) {
            if (needBlank) {
                pw.println();
            }
            pw.println("System update freeze record: " + getSystemUpdateFreezePeriodRecordAsString());
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public String getSystemUpdateFreezePeriodRecordAsString() {
        StringBuilder freezePeriodRecord = new StringBuilder();
        freezePeriodRecord.append("start: ");
        LocalDate localDate = this.mSystemUpdateFreezeStart;
        if (localDate != null) {
            freezePeriodRecord.append(localDate.toString());
        } else {
            freezePeriodRecord.append("null");
        }
        freezePeriodRecord.append("; end: ");
        LocalDate localDate2 = this.mSystemUpdateFreezeEnd;
        if (localDate2 != null) {
            freezePeriodRecord.append(localDate2.toString());
        } else {
            freezePeriodRecord.append("null");
        }
        return freezePeriodRecord.toString();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public File getDeviceOwnerFile() {
        return new File(this.mPathProvider.getDataSystemDirectory(), DEVICE_OWNER_XML);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public File getProfileOwnerFile(int userId) {
        return new File(this.mPathProvider.getUserSystemDirectory(userId), PROFILE_OWNER_XML);
    }

    /* loaded from: classes.dex */
    private static abstract class FileReadWriter {
        private final File mFile;

        abstract boolean readInner(TypedXmlPullParser typedXmlPullParser, int i, String str);

        abstract boolean shouldWrite();

        abstract void writeInner(TypedXmlSerializer typedXmlSerializer) throws IOException;

        protected FileReadWriter(File file) {
            this.mFile = file;
        }

        boolean writeToFileLocked() {
            if (!shouldWrite()) {
                if (this.mFile.exists() && !this.mFile.delete()) {
                    Slog.e(OwnersData.TAG, "Failed to remove " + this.mFile.getPath());
                }
                return true;
            }
            AtomicFile f = new AtomicFile(this.mFile);
            FileOutputStream outputStream = null;
            try {
                outputStream = f.startWrite();
                TypedXmlSerializer out = Xml.resolveSerializer(outputStream);
                out.startDocument((String) null, true);
                out.startTag((String) null, OwnersData.TAG_ROOT);
                writeInner(out);
                out.endTag((String) null, OwnersData.TAG_ROOT);
                out.endDocument();
                out.flush();
                f.finishWrite(outputStream);
                return true;
            } catch (IOException e) {
                Slog.e(OwnersData.TAG, "Exception when writing", e);
                if (outputStream != null) {
                    f.failWrite(outputStream);
                    return false;
                }
                return false;
            }
        }

        /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [331=4] */
        void readFromFileLocked() {
            if (!this.mFile.exists()) {
                return;
            }
            AtomicFile f = new AtomicFile(this.mFile);
            InputStream input = null;
            try {
                try {
                    input = f.openRead();
                    TypedXmlPullParser parser = Xml.resolvePullParser(input);
                    int depth = 0;
                    while (true) {
                        int type = parser.next();
                        if (type != 1) {
                            switch (type) {
                                case 2:
                                    depth++;
                                    String tag = parser.getName();
                                    if (depth != 1) {
                                        if (readInner(parser, depth, tag)) {
                                            break;
                                        } else {
                                            return;
                                        }
                                    } else if (!OwnersData.TAG_ROOT.equals(tag)) {
                                        Slog.e(OwnersData.TAG, "Invalid root tag: " + tag);
                                        return;
                                    } else {
                                        break;
                                    }
                                case 3:
                                    depth--;
                                    break;
                            }
                        }
                    }
                } catch (IOException | XmlPullParserException e) {
                    Slog.e(OwnersData.TAG, "Error parsing owners information file", e);
                }
            } finally {
                IoUtils.closeQuietly(input);
            }
        }
    }

    /* loaded from: classes.dex */
    private class DeviceOwnerReadWriter extends FileReadWriter {
        protected DeviceOwnerReadWriter() {
            super(OwnersData.this.getDeviceOwnerFile());
        }

        @Override // com.android.server.devicepolicy.OwnersData.FileReadWriter
        boolean shouldWrite() {
            return (OwnersData.this.mDeviceOwner == null && OwnersData.this.mSystemUpdatePolicy == null && OwnersData.this.mSystemUpdateInfo == null) ? false : true;
        }

        @Override // com.android.server.devicepolicy.OwnersData.FileReadWriter
        void writeInner(TypedXmlSerializer out) throws IOException {
            if (OwnersData.this.mDeviceOwner != null) {
                OwnersData.this.mDeviceOwner.writeToXml(out, OwnersData.TAG_DEVICE_OWNER);
                out.startTag((String) null, OwnersData.TAG_DEVICE_OWNER_CONTEXT);
                out.attributeInt((String) null, "userId", OwnersData.this.mDeviceOwnerUserId);
                out.endTag((String) null, OwnersData.TAG_DEVICE_OWNER_CONTEXT);
            }
            if (!OwnersData.this.mDeviceOwnerTypes.isEmpty()) {
                for (Map.Entry<String, Integer> entry : OwnersData.this.mDeviceOwnerTypes.entrySet()) {
                    out.startTag((String) null, OwnersData.TAG_DEVICE_OWNER_TYPE);
                    out.attribute((String) null, "package", entry.getKey());
                    out.attributeInt((String) null, OwnersData.ATTR_DEVICE_OWNER_TYPE_VALUE, entry.getValue().intValue());
                    out.endTag((String) null, OwnersData.TAG_DEVICE_OWNER_TYPE);
                }
            }
            if (OwnersData.this.mSystemUpdatePolicy != null) {
                out.startTag((String) null, OwnersData.TAG_SYSTEM_UPDATE_POLICY);
                OwnersData.this.mSystemUpdatePolicy.saveToXml(out);
                out.endTag((String) null, OwnersData.TAG_SYSTEM_UPDATE_POLICY);
            }
            if (OwnersData.this.mSystemUpdateInfo != null) {
                OwnersData.this.mSystemUpdateInfo.writeToXml(out, OwnersData.TAG_PENDING_OTA_INFO);
            }
            if (OwnersData.this.mSystemUpdateFreezeStart != null || OwnersData.this.mSystemUpdateFreezeEnd != null) {
                out.startTag((String) null, OwnersData.TAG_FREEZE_PERIOD_RECORD);
                if (OwnersData.this.mSystemUpdateFreezeStart != null) {
                    out.attribute((String) null, OwnersData.ATTR_FREEZE_RECORD_START, OwnersData.this.mSystemUpdateFreezeStart.toString());
                }
                if (OwnersData.this.mSystemUpdateFreezeEnd != null) {
                    out.attribute((String) null, OwnersData.ATTR_FREEZE_RECORD_END, OwnersData.this.mSystemUpdateFreezeEnd.toString());
                }
                out.endTag((String) null, OwnersData.TAG_FREEZE_PERIOD_RECORD);
            }
        }

        /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
        /* JADX WARN: Code restructure failed: missing block: B:9:0x0016, code lost:
            if (r11.equals(com.android.server.devicepolicy.OwnersData.TAG_SYSTEM_UPDATE_POLICY) != false) goto L9;
         */
        @Override // com.android.server.devicepolicy.OwnersData.FileReadWriter
        /*
            Code decompiled incorrectly, please refer to instructions dump.
        */
        boolean readInner(TypedXmlPullParser parser, int depth, String tag) {
            char c = 2;
            if (depth > 2) {
                return true;
            }
            switch (tag.hashCode()) {
                case -2101756875:
                    if (tag.equals(OwnersData.TAG_PENDING_OTA_INFO)) {
                        c = 3;
                        break;
                    }
                    c = 65535;
                    break;
                case -2020438916:
                    if (tag.equals(OwnersData.TAG_DEVICE_OWNER)) {
                        c = 0;
                        break;
                    }
                    c = 65535;
                    break;
                case -1900517026:
                    if (tag.equals(OwnersData.TAG_DEVICE_OWNER_CONTEXT)) {
                        c = 1;
                        break;
                    }
                    c = 65535;
                    break;
                case -465393379:
                    if (tag.equals(OwnersData.TAG_DEVICE_OWNER_PROTECTED_PACKAGES)) {
                        c = 6;
                        break;
                    }
                    c = 65535;
                    break;
                case 544117227:
                    if (tag.equals(OwnersData.TAG_DEVICE_OWNER_TYPE)) {
                        c = 5;
                        break;
                    }
                    c = 65535;
                    break;
                case 1303827527:
                    if (tag.equals(OwnersData.TAG_FREEZE_PERIOD_RECORD)) {
                        c = 4;
                        break;
                    }
                    c = 65535;
                    break;
                case 1748301720:
                    break;
                default:
                    c = 65535;
                    break;
            }
            switch (c) {
                case 0:
                    OwnersData.this.mDeviceOwner = OwnerInfo.readFromXml(parser);
                    OwnersData.this.mDeviceOwnerUserId = 0;
                    break;
                case 1:
                    OwnersData ownersData = OwnersData.this;
                    ownersData.mDeviceOwnerUserId = parser.getAttributeInt((String) null, "userId", ownersData.mDeviceOwnerUserId);
                    break;
                case 2:
                    OwnersData.this.mSystemUpdatePolicy = SystemUpdatePolicy.restoreFromXml(parser);
                    break;
                case 3:
                    OwnersData.this.mSystemUpdateInfo = SystemUpdateInfo.readFromXml(parser);
                    break;
                case 4:
                    String startDate = parser.getAttributeValue((String) null, OwnersData.ATTR_FREEZE_RECORD_START);
                    String endDate = parser.getAttributeValue((String) null, OwnersData.ATTR_FREEZE_RECORD_END);
                    if (startDate != null && endDate != null) {
                        OwnersData.this.mSystemUpdateFreezeStart = LocalDate.parse(startDate);
                        OwnersData.this.mSystemUpdateFreezeEnd = LocalDate.parse(endDate);
                        if (OwnersData.this.mSystemUpdateFreezeStart.isAfter(OwnersData.this.mSystemUpdateFreezeEnd)) {
                            Slog.e(OwnersData.TAG, "Invalid system update freeze record loaded");
                            OwnersData.this.mSystemUpdateFreezeStart = null;
                            OwnersData.this.mSystemUpdateFreezeEnd = null;
                            break;
                        }
                    }
                    break;
                case 5:
                    String packageName = parser.getAttributeValue((String) null, "package");
                    int deviceOwnerType = parser.getAttributeInt((String) null, OwnersData.ATTR_DEVICE_OWNER_TYPE_VALUE, 0);
                    OwnersData.this.mDeviceOwnerTypes.put(packageName, Integer.valueOf(deviceOwnerType));
                    break;
                case 6:
                    String packageName2 = parser.getAttributeValue((String) null, "package");
                    int protectedPackagesSize = parser.getAttributeInt((String) null, OwnersData.ATTR_SIZE, 0);
                    List<String> protectedPackages = new ArrayList<>();
                    for (int i = 0; i < protectedPackagesSize; i++) {
                        protectedPackages.add(parser.getAttributeValue((String) null, "name" + i));
                    }
                    if (OwnersData.this.mDeviceOwnerProtectedPackages == null) {
                        OwnersData.this.mDeviceOwnerProtectedPackages = new ArrayMap<>();
                    }
                    OwnersData.this.mDeviceOwnerProtectedPackages.put(packageName2, protectedPackages);
                    break;
                default:
                    Slog.e(OwnersData.TAG, "Unexpected tag: " + tag);
                    return false;
            }
            return true;
        }
    }

    /* loaded from: classes.dex */
    private class ProfileOwnerReadWriter extends FileReadWriter {
        private final int mUserId;

        ProfileOwnerReadWriter(int userId) {
            super(OwnersData.this.getProfileOwnerFile(userId));
            this.mUserId = userId;
        }

        @Override // com.android.server.devicepolicy.OwnersData.FileReadWriter
        boolean shouldWrite() {
            return OwnersData.this.mProfileOwners.get(Integer.valueOf(this.mUserId)) != null;
        }

        @Override // com.android.server.devicepolicy.OwnersData.FileReadWriter
        void writeInner(TypedXmlSerializer out) throws IOException {
            OwnerInfo profileOwner = OwnersData.this.mProfileOwners.get(Integer.valueOf(this.mUserId));
            if (profileOwner != null) {
                profileOwner.writeToXml(out, OwnersData.TAG_PROFILE_OWNER);
            }
        }

        @Override // com.android.server.devicepolicy.OwnersData.FileReadWriter
        boolean readInner(TypedXmlPullParser parser, int depth, String tag) {
            if (depth > 2) {
                return true;
            }
            char c = 65535;
            switch (tag.hashCode()) {
                case 2145316239:
                    if (tag.equals(OwnersData.TAG_PROFILE_OWNER)) {
                        c = 0;
                        break;
                    }
                    break;
            }
            switch (c) {
                case 0:
                    OwnersData.this.mProfileOwners.put(Integer.valueOf(this.mUserId), OwnerInfo.readFromXml(parser));
                    return true;
                default:
                    Slog.e(OwnersData.TAG, "Unexpected tag: " + tag);
                    return false;
            }
        }
    }

    /* loaded from: classes.dex */
    static class OwnerInfo {
        public final ComponentName admin;
        public boolean isOrganizationOwnedDevice;
        public final String name;
        public final String packageName;
        public String remoteBugreportHash;
        public String remoteBugreportUri;

        /* JADX INFO: Access modifiers changed from: package-private */
        public OwnerInfo(String name, ComponentName admin, String remoteBugreportUri, String remoteBugreportHash, boolean isOrganizationOwnedDevice) {
            this.name = name;
            this.admin = admin;
            this.packageName = admin.getPackageName();
            this.remoteBugreportUri = remoteBugreportUri;
            this.remoteBugreportHash = remoteBugreportHash;
            this.isOrganizationOwnedDevice = isOrganizationOwnedDevice;
        }

        public void writeToXml(TypedXmlSerializer out, String tag) throws IOException {
            out.startTag((String) null, tag);
            String str = this.name;
            if (str != null) {
                out.attribute((String) null, "name", str);
            }
            ComponentName componentName = this.admin;
            if (componentName != null) {
                out.attribute((String) null, OwnersData.ATTR_COMPONENT_NAME, componentName.flattenToString());
            }
            String str2 = this.remoteBugreportUri;
            if (str2 != null) {
                out.attribute((String) null, OwnersData.ATTR_REMOTE_BUGREPORT_URI, str2);
            }
            String str3 = this.remoteBugreportHash;
            if (str3 != null) {
                out.attribute((String) null, OwnersData.ATTR_REMOTE_BUGREPORT_HASH, str3);
            }
            boolean z = this.isOrganizationOwnedDevice;
            if (z) {
                out.attributeBoolean((String) null, OwnersData.ATTR_PROFILE_OWNER_OF_ORG_OWNED_DEVICE, z);
            }
            out.endTag((String) null, tag);
        }

        public static OwnerInfo readFromXml(TypedXmlPullParser parser) {
            String name = parser.getAttributeValue((String) null, "name");
            String componentName = parser.getAttributeValue((String) null, OwnersData.ATTR_COMPONENT_NAME);
            String remoteBugreportUri = parser.getAttributeValue((String) null, OwnersData.ATTR_REMOTE_BUGREPORT_URI);
            String remoteBugreportHash = parser.getAttributeValue((String) null, OwnersData.ATTR_REMOTE_BUGREPORT_HASH);
            String canAccessDeviceIdsStr = parser.getAttributeValue((String) null, OwnersData.ATTR_CAN_ACCESS_DEVICE_IDS);
            boolean canAccessDeviceIds = "true".equals(canAccessDeviceIdsStr);
            String isOrgOwnedDeviceStr = parser.getAttributeValue((String) null, OwnersData.ATTR_PROFILE_OWNER_OF_ORG_OWNED_DEVICE);
            boolean isOrgOwnedDevice = "true".equals(isOrgOwnedDeviceStr) | canAccessDeviceIds;
            if (componentName == null) {
                Slog.e(OwnersData.TAG, "Owner component not found");
                return null;
            }
            ComponentName admin = ComponentName.unflattenFromString(componentName);
            if (admin == null) {
                Slog.e(OwnersData.TAG, "Owner component not parsable: " + componentName);
                return null;
            }
            return new OwnerInfo(name, admin, remoteBugreportUri, remoteBugreportHash, isOrgOwnedDevice);
        }

        public void dump(IndentingPrintWriter pw) {
            pw.println("admin=" + this.admin);
            pw.println("name=" + this.name);
            pw.println("package=" + this.packageName);
            pw.println("isOrganizationOwnedDevice=" + this.isOrganizationOwnedDevice);
        }
    }
}
