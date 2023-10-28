package com.android.server.companion;

import android.companion.AssociationInfo;
import android.content.pm.UserInfo;
import android.net.MacAddress;
import android.os.Environment;
import android.util.ArrayMap;
import android.util.AtomicFile;
import android.util.Slog;
import android.util.SparseArray;
import android.util.TypedXmlPullParser;
import android.util.TypedXmlSerializer;
import android.util.Xml;
import com.android.internal.util.CollectionUtils;
import com.android.internal.util.FunctionalUtils;
import com.android.internal.util.XmlUtils;
import com.android.server.job.controllers.JobStatus;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public final class PersistentDataStore {
    private static final int CURRENT_PERSISTENCE_VERSION = 1;
    private static final boolean DEBUG = false;
    private static final String FILE_NAME = "companion_device_manager.xml";
    private static final String FILE_NAME_LEGACY = "companion_device_manager_associations.xml";
    private static final String LEGACY_XML_ATTR_DEVICE = "device";
    private static final String TAG = "CompanionDevice_PersistentDataStore";
    private static final String XML_ATTR_DISPLAY_NAME = "display_name";
    private static final String XML_ATTR_ID = "id";
    private static final String XML_ATTR_LAST_TIME_CONNECTED = "last_time_connected";
    private static final String XML_ATTR_MAC_ADDRESS = "mac_address";
    private static final String XML_ATTR_NOTIFY_DEVICE_NEARBY = "notify_device_nearby";
    private static final String XML_ATTR_PACKAGE = "package";
    private static final String XML_ATTR_PACKAGE_NAME = "package_name";
    private static final String XML_ATTR_PERSISTENCE_VERSION = "persistence-version";
    private static final String XML_ATTR_PROFILE = "profile";
    private static final String XML_ATTR_SELF_MANAGED = "self_managed";
    private static final String XML_ATTR_TIME_APPROVED = "time_approved";
    private static final String XML_TAG_ASSOCIATION = "association";
    private static final String XML_TAG_ASSOCIATIONS = "associations";
    private static final String XML_TAG_ID = "id";
    private static final String XML_TAG_PACKAGE = "package";
    private static final String XML_TAG_PREVIOUSLY_USED_IDS = "previously-used-ids";
    private static final String XML_TAG_STATE = "state";
    private final ConcurrentMap<Integer, AtomicFile> mUserIdToStorageFile = new ConcurrentHashMap();

    /* JADX INFO: Access modifiers changed from: package-private */
    public void readStateForUsers(List<UserInfo> users, Set<AssociationInfo> allAssociationsOut, SparseArray<Map<String, Set<Integer>>> previouslyUsedIdsPerUserOut) {
        for (UserInfo user : users) {
            int userId = user.id;
            Map<String, Set<Integer>> previouslyUsedIds = new ArrayMap<>();
            Set<AssociationInfo> associationsForUser = new HashSet<>();
            readStateForUser(userId, associationsForUser, previouslyUsedIds);
            int firstAllowedId = CompanionDeviceManagerService.getFirstAssociationIdForUser(userId);
            int lastAllowedId = CompanionDeviceManagerService.getLastAssociationIdForUser(userId);
            for (AssociationInfo association : associationsForUser) {
                int id = association.getId();
                if (id < firstAllowedId || id > lastAllowedId) {
                    Slog.e(TAG, "Wrong association ID assignment: " + id + ". Association belongs to u" + userId + " and thus its ID should be within [" + firstAllowedId + ", " + lastAllowedId + "] range.");
                }
            }
            allAssociationsOut.addAll(associationsForUser);
            previouslyUsedIdsPerUserOut.append(userId, previouslyUsedIds);
        }
    }

    void readStateForUser(int userId, Collection<AssociationInfo> associationsOut, Map<String, Set<Integer>> previouslyUsedIdsPerPackageOut) {
        AtomicFile readFrom;
        String rootTag;
        Slog.i(TAG, "Reading associations for user " + userId + " from disk");
        AtomicFile file = getStorageFileForUser(userId);
        synchronized (file) {
            File legacyBaseFile = null;
            if (!file.getBaseFile().exists()) {
                legacyBaseFile = getBaseLegacyStorageFileForUser(userId);
                if (!legacyBaseFile.exists()) {
                    return;
                }
                AtomicFile readFrom2 = new AtomicFile(legacyBaseFile);
                readFrom = readFrom2;
                rootTag = XML_TAG_ASSOCIATIONS;
            } else {
                readFrom = file;
                rootTag = "state";
            }
            int version = readStateFromFileLocked(userId, readFrom, rootTag, associationsOut, previouslyUsedIdsPerPackageOut);
            if (legacyBaseFile != null || version < 1) {
                persistStateToFileLocked(file, associationsOut, previouslyUsedIdsPerPackageOut);
                if (legacyBaseFile != null) {
                    legacyBaseFile.delete();
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void persistStateForUser(int userId, Collection<AssociationInfo> associations, Map<String, Set<Integer>> previouslyUsedIdsPerPackage) {
        Slog.i(TAG, "Writing associations for user " + userId + " to disk");
        AtomicFile file = getStorageFileForUser(userId);
        synchronized (file) {
            persistStateToFileLocked(file, associations, previouslyUsedIdsPerPackage);
        }
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    private int readStateFromFileLocked(int userId, AtomicFile file, String rootTag, Collection<AssociationInfo> associationsOut, Map<String, Set<Integer>> previouslyUsedIdsPerPackageOut) {
        try {
            FileInputStream in = file.openRead();
            TypedXmlPullParser parser = Xml.resolvePullParser(in);
            XmlUtils.beginDocument(parser, rootTag);
            int version = XmlUtils.readIntAttribute(parser, XML_ATTR_PERSISTENCE_VERSION, 0);
            switch (version) {
                case 0:
                    readAssociationsV0(parser, userId, associationsOut);
                    break;
                case 1:
                    while (true) {
                        parser.nextTag();
                        if (DataStoreUtils.isStartOfTag(parser, XML_TAG_ASSOCIATIONS)) {
                            readAssociationsV1(parser, userId, associationsOut);
                        } else if (DataStoreUtils.isStartOfTag(parser, XML_TAG_PREVIOUSLY_USED_IDS)) {
                            readPreviouslyUsedIdsV1(parser, previouslyUsedIdsPerPackageOut);
                        } else if (DataStoreUtils.isEndOfTag(parser, rootTag)) {
                            break;
                        }
                    }
            }
            if (in != null) {
                in.close();
            }
            return version;
        } catch (IOException | XmlPullParserException e) {
            Slog.e(TAG, "Error while reading associations file", e);
            return -1;
        }
    }

    private void persistStateToFileLocked(AtomicFile file, final Collection<AssociationInfo> associations, final Map<String, Set<Integer>> previouslyUsedIdsPerPackage) {
        DataStoreUtils.writeToFileSafely(file, new FunctionalUtils.ThrowingConsumer() { // from class: com.android.server.companion.PersistentDataStore$$ExternalSyntheticLambda2
            public final void acceptOrThrow(Object obj) {
                PersistentDataStore.lambda$persistStateToFileLocked$0(associations, previouslyUsedIdsPerPackage, (FileOutputStream) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$persistStateToFileLocked$0(Collection associations, Map previouslyUsedIdsPerPackage, FileOutputStream out) throws Exception {
        TypedXmlSerializer serializer = Xml.resolveSerializer(out);
        serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
        serializer.startDocument((String) null, true);
        serializer.startTag((String) null, "state");
        XmlUtils.writeIntAttribute(serializer, XML_ATTR_PERSISTENCE_VERSION, 1);
        writeAssociations(serializer, associations);
        writePreviouslyUsedIds(serializer, previouslyUsedIdsPerPackage);
        serializer.endTag((String) null, "state");
        serializer.endDocument();
    }

    private AtomicFile getStorageFileForUser(final int userId) {
        return this.mUserIdToStorageFile.computeIfAbsent(Integer.valueOf(userId), new Function() { // from class: com.android.server.companion.PersistentDataStore$$ExternalSyntheticLambda0
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                AtomicFile createStorageFileForUser;
                Integer num = (Integer) obj;
                createStorageFileForUser = DataStoreUtils.createStorageFileForUser(userId, PersistentDataStore.FILE_NAME);
                return createStorageFileForUser;
            }
        });
    }

    private static File getBaseLegacyStorageFileForUser(int userId) {
        return new File(Environment.getUserSystemDirectory(userId), FILE_NAME_LEGACY);
    }

    private static void readAssociationsV0(TypedXmlPullParser parser, int userId, Collection<AssociationInfo> out) throws XmlPullParserException, IOException {
        requireStartOfTag(parser, XML_TAG_ASSOCIATIONS);
        int associationId = CompanionDeviceManagerService.getFirstAssociationIdForUser(userId);
        while (true) {
            parser.nextTag();
            if (!DataStoreUtils.isEndOfTag(parser, XML_TAG_ASSOCIATIONS)) {
                if (DataStoreUtils.isStartOfTag(parser, XML_TAG_ASSOCIATION)) {
                    readAssociationV0(parser, userId, associationId, out);
                    associationId++;
                }
            } else {
                return;
            }
        }
    }

    private static void readAssociationV0(TypedXmlPullParser parser, int userId, int associationId, Collection<AssociationInfo> out) throws XmlPullParserException {
        requireStartOfTag(parser, XML_TAG_ASSOCIATION);
        String appPackage = XmlUtils.readStringAttribute(parser, "package");
        String deviceAddress = XmlUtils.readStringAttribute(parser, LEGACY_XML_ATTR_DEVICE);
        if (appPackage != null && deviceAddress != null) {
            String profile = XmlUtils.readStringAttribute(parser, XML_ATTR_PROFILE);
            boolean notify = XmlUtils.readBooleanAttribute(parser, XML_ATTR_NOTIFY_DEVICE_NEARBY);
            long timeApproved = XmlUtils.readLongAttribute(parser, XML_ATTR_TIME_APPROVED, 0L);
            out.add(new AssociationInfo(associationId, userId, appPackage, MacAddress.fromString(deviceAddress), (CharSequence) null, profile, false, notify, timeApproved, (long) JobStatus.NO_LATEST_RUNTIME));
        }
    }

    private static void readAssociationsV1(TypedXmlPullParser parser, int userId, Collection<AssociationInfo> out) throws XmlPullParserException, IOException {
        requireStartOfTag(parser, XML_TAG_ASSOCIATIONS);
        while (true) {
            parser.nextTag();
            if (!DataStoreUtils.isEndOfTag(parser, XML_TAG_ASSOCIATIONS)) {
                if (DataStoreUtils.isStartOfTag(parser, XML_TAG_ASSOCIATION)) {
                    readAssociationV1(parser, userId, out);
                }
            } else {
                return;
            }
        }
    }

    private static void readAssociationV1(TypedXmlPullParser parser, int userId, Collection<AssociationInfo> out) throws XmlPullParserException, IOException {
        requireStartOfTag(parser, XML_TAG_ASSOCIATION);
        int associationId = XmlUtils.readIntAttribute(parser, "id");
        String profile = XmlUtils.readStringAttribute(parser, XML_ATTR_PROFILE);
        String appPackage = XmlUtils.readStringAttribute(parser, "package");
        MacAddress macAddress = stringToMacAddress(XmlUtils.readStringAttribute(parser, XML_ATTR_MAC_ADDRESS));
        String displayName = XmlUtils.readStringAttribute(parser, XML_ATTR_DISPLAY_NAME);
        boolean selfManaged = XmlUtils.readBooleanAttribute(parser, XML_ATTR_SELF_MANAGED);
        boolean notify = XmlUtils.readBooleanAttribute(parser, XML_ATTR_NOTIFY_DEVICE_NEARBY);
        long timeApproved = XmlUtils.readLongAttribute(parser, XML_ATTR_TIME_APPROVED, 0L);
        long lastTimeConnected = XmlUtils.readLongAttribute(parser, XML_ATTR_LAST_TIME_CONNECTED, (long) JobStatus.NO_LATEST_RUNTIME);
        AssociationInfo associationInfo = createAssociationInfoNoThrow(associationId, userId, appPackage, macAddress, displayName, profile, selfManaged, notify, timeApproved, lastTimeConnected);
        if (associationInfo != null) {
            out.add(associationInfo);
        }
    }

    private static void readPreviouslyUsedIdsV1(TypedXmlPullParser parser, Map<String, Set<Integer>> out) throws XmlPullParserException, IOException {
        requireStartOfTag(parser, XML_TAG_PREVIOUSLY_USED_IDS);
        while (true) {
            parser.nextTag();
            if (!DataStoreUtils.isEndOfTag(parser, XML_TAG_PREVIOUSLY_USED_IDS)) {
                if (DataStoreUtils.isStartOfTag(parser, "package")) {
                    String packageName = XmlUtils.readStringAttribute(parser, XML_ATTR_PACKAGE_NAME);
                    Set<Integer> usedIds = new HashSet<>();
                    while (true) {
                        parser.nextTag();
                        if (DataStoreUtils.isEndOfTag(parser, "package")) {
                            break;
                        } else if (DataStoreUtils.isStartOfTag(parser, "id")) {
                            parser.nextToken();
                            int id = Integer.parseInt(parser.getText());
                            usedIds.add(Integer.valueOf(id));
                        }
                    }
                    out.put(packageName, usedIds);
                }
            } else {
                return;
            }
        }
    }

    private static void writeAssociations(XmlSerializer parent, Collection<AssociationInfo> associations) throws IOException {
        XmlSerializer serializer = parent.startTag(null, XML_TAG_ASSOCIATIONS);
        for (AssociationInfo association : associations) {
            writeAssociation(serializer, association);
        }
        serializer.endTag(null, XML_TAG_ASSOCIATIONS);
    }

    private static void writeAssociation(XmlSerializer parent, AssociationInfo a) throws IOException {
        XmlSerializer serializer = parent.startTag(null, XML_TAG_ASSOCIATION);
        XmlUtils.writeIntAttribute(serializer, "id", a.getId());
        XmlUtils.writeStringAttribute(serializer, XML_ATTR_PROFILE, a.getDeviceProfile());
        XmlUtils.writeStringAttribute(serializer, "package", a.getPackageName());
        XmlUtils.writeStringAttribute(serializer, XML_ATTR_MAC_ADDRESS, a.getDeviceMacAddressAsString());
        XmlUtils.writeStringAttribute(serializer, XML_ATTR_DISPLAY_NAME, a.getDisplayName());
        XmlUtils.writeBooleanAttribute(serializer, XML_ATTR_SELF_MANAGED, a.isSelfManaged());
        XmlUtils.writeBooleanAttribute(serializer, XML_ATTR_NOTIFY_DEVICE_NEARBY, a.isNotifyOnDeviceNearby());
        XmlUtils.writeLongAttribute(serializer, XML_ATTR_TIME_APPROVED, a.getTimeApprovedMs());
        XmlUtils.writeLongAttribute(serializer, XML_ATTR_LAST_TIME_CONNECTED, a.getLastTimeConnectedMs().longValue());
        serializer.endTag(null, XML_TAG_ASSOCIATION);
    }

    private static void writePreviouslyUsedIds(XmlSerializer parent, Map<String, Set<Integer>> previouslyUsedIdsPerPackage) throws IOException {
        XmlSerializer serializer = parent.startTag(null, XML_TAG_PREVIOUSLY_USED_IDS);
        for (Map.Entry<String, Set<Integer>> entry : previouslyUsedIdsPerPackage.entrySet()) {
            writePreviouslyUsedIdsForPackage(serializer, entry.getKey(), entry.getValue());
        }
        serializer.endTag(null, XML_TAG_PREVIOUSLY_USED_IDS);
    }

    private static void writePreviouslyUsedIdsForPackage(XmlSerializer parent, String packageName, Set<Integer> previouslyUsedIds) throws IOException {
        final XmlSerializer serializer = parent.startTag(null, "package");
        XmlUtils.writeStringAttribute(serializer, XML_ATTR_PACKAGE_NAME, packageName);
        CollectionUtils.forEach(previouslyUsedIds, new FunctionalUtils.ThrowingConsumer() { // from class: com.android.server.companion.PersistentDataStore$$ExternalSyntheticLambda1
            public final void acceptOrThrow(Object obj) {
                serializer.startTag(null, "id").text(Integer.toString(((Integer) obj).intValue())).endTag(null, "id");
            }
        });
        serializer.endTag(null, "package");
    }

    private static void requireStartOfTag(XmlPullParser parser, String tag) throws XmlPullParserException {
        if (!DataStoreUtils.isStartOfTag(parser, tag)) {
            throw new XmlPullParserException("Should be at the start of \"associations\" tag");
        }
    }

    private static MacAddress stringToMacAddress(String address) {
        if (address != null) {
            return MacAddress.fromString(address);
        }
        return null;
    }

    private static AssociationInfo createAssociationInfoNoThrow(int associationId, int userId, String appPackage, MacAddress macAddress, CharSequence displayName, String profile, boolean selfManaged, boolean notify, long timeApproved, long lastTimeConnected) {
        try {
            AssociationInfo associationInfo = new AssociationInfo(associationId, userId, appPackage, macAddress, displayName, profile, selfManaged, notify, timeApproved, lastTimeConnected);
            return associationInfo;
        } catch (Exception e) {
            return null;
        }
    }
}
