package com.android.server.companion;

import android.companion.SystemDataTransferRequest;
import android.util.AtomicFile;
import android.util.Slog;
import android.util.TypedXmlPullParser;
import android.util.TypedXmlSerializer;
import android.util.Xml;
import com.android.internal.util.FunctionalUtils;
import com.android.internal.util.XmlUtils;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes.dex */
public class SystemDataTransferRequestDataStore {
    private static final String FILE_NAME = "companion_device_system_data_transfer_requests.xml";
    private static final String LOG_TAG = SystemDataTransferRequestDataStore.class.getSimpleName();
    private static final String XML_ATTR_ASSOCIATION_ID = "association_id";
    private static final String XML_ATTR_IS_PERMISSION_SYNC_ALL_PACKAGES = "is_permission_sync_all_packages";
    private static final String XML_ATTR_PERMISSION_SYNC_PACKAGES = "permission_sync_packages";
    private static final String XML_TAG_LIST = "list";
    private static final String XML_TAG_REQUEST = "request";
    private static final String XML_TAG_REQUESTS = "requests";
    private final ConcurrentMap<Integer, AtomicFile> mUserIdToStorageFile = new ConcurrentHashMap();

    List<SystemDataTransferRequest> readRequestsForUser(int userId) {
        AtomicFile file = getStorageFileForUser(userId);
        String str = LOG_TAG;
        Slog.i(str, "Reading SystemDataTransferRequests for user " + userId + " from file=" + file.getBaseFile().getPath());
        synchronized (file) {
            if (!file.getBaseFile().exists()) {
                Slog.d(str, "File does not exist -> Abort");
                return Collections.emptyList();
            }
            try {
                FileInputStream in = file.openRead();
                try {
                    TypedXmlPullParser parser = Xml.resolvePullParser(in);
                    XmlUtils.beginDocument(parser, XML_TAG_REQUESTS);
                    List<SystemDataTransferRequest> readRequests = readRequests(parser);
                    if (in != null) {
                        in.close();
                    }
                    return readRequests;
                } catch (Throwable th) {
                    if (in != null) {
                        try {
                            in.close();
                        } catch (Throwable th2) {
                            th.addSuppressed(th2);
                        }
                    }
                    throw th;
                }
            } catch (IOException | XmlPullParserException e) {
                Slog.e(LOG_TAG, "Error while reading requests file", e);
                return Collections.emptyList();
            }
        }
    }

    private List<SystemDataTransferRequest> readRequests(TypedXmlPullParser parser) throws XmlPullParserException, IOException {
        if (!DataStoreUtils.isStartOfTag(parser, XML_TAG_REQUESTS)) {
            throw new XmlPullParserException("The XML doesn't have start tag: requests");
        }
        List<SystemDataTransferRequest> requests = new ArrayList<>();
        while (true) {
            parser.nextTag();
            if (!DataStoreUtils.isEndOfTag(parser, XML_TAG_REQUESTS)) {
                if (DataStoreUtils.isStartOfTag(parser, XML_TAG_REQUEST)) {
                    requests.add(readRequest(parser));
                }
            } else {
                return requests;
            }
        }
    }

    private SystemDataTransferRequest readRequest(TypedXmlPullParser parser) throws XmlPullParserException, IOException {
        if (!DataStoreUtils.isStartOfTag(parser, XML_TAG_REQUEST)) {
            throw new XmlPullParserException("XML doesn't have start tag: request");
        }
        int associationId = XmlUtils.readIntAttribute(parser, XML_ATTR_ASSOCIATION_ID);
        boolean isPermissionSyncAllPackages = XmlUtils.readBooleanAttribute(parser, XML_ATTR_IS_PERMISSION_SYNC_ALL_PACKAGES);
        parser.nextTag();
        List<String> permissionSyncPackages = new ArrayList<>();
        if (DataStoreUtils.isStartOfTag(parser, XML_TAG_LIST)) {
            parser.nextTag();
            permissionSyncPackages = XmlUtils.readThisListXml(parser, XML_TAG_LIST, new String[1]);
        }
        return new SystemDataTransferRequest(associationId, isPermissionSyncAllPackages, permissionSyncPackages);
    }

    void writeRequestsForUser(int userId, final List<SystemDataTransferRequest> requests) {
        AtomicFile file = getStorageFileForUser(userId);
        Slog.i(LOG_TAG, "Writing SystemDataTransferRequests for user " + userId + " to file=" + file.getBaseFile().getPath());
        synchronized (file) {
            DataStoreUtils.writeToFileSafely(file, new FunctionalUtils.ThrowingConsumer() { // from class: com.android.server.companion.SystemDataTransferRequestDataStore$$ExternalSyntheticLambda1
                public final void acceptOrThrow(Object obj) {
                    SystemDataTransferRequestDataStore.this.m2702xb902a863(requests, (FileOutputStream) obj);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$writeRequestsForUser$0$com-android-server-companion-SystemDataTransferRequestDataStore  reason: not valid java name */
    public /* synthetic */ void m2702xb902a863(List requests, FileOutputStream out) throws Exception {
        TypedXmlSerializer serializer = Xml.resolveSerializer(out);
        serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
        serializer.startDocument((String) null, true);
        writeRequests(serializer, requests);
        serializer.endDocument();
    }

    private void writeRequests(TypedXmlSerializer serializer, Collection<SystemDataTransferRequest> requests) throws IOException {
        serializer.startTag((String) null, XML_TAG_REQUESTS);
        for (SystemDataTransferRequest request : requests) {
            writeRequest(serializer, request);
        }
        serializer.endTag((String) null, XML_TAG_REQUESTS);
    }

    private void writeRequest(TypedXmlSerializer serializer, SystemDataTransferRequest request) throws IOException {
        serializer.startTag((String) null, XML_TAG_REQUEST);
        XmlUtils.writeIntAttribute(serializer, XML_ATTR_ASSOCIATION_ID, request.getAssociationId());
        XmlUtils.writeBooleanAttribute(serializer, XML_ATTR_IS_PERMISSION_SYNC_ALL_PACKAGES, request.isPermissionSyncAllPackages());
        try {
            XmlUtils.writeListXml(request.getPermissionSyncPackages(), XML_ATTR_PERMISSION_SYNC_PACKAGES, serializer);
        } catch (XmlPullParserException e) {
            Slog.e(LOG_TAG, "Error writing permission sync packages into XML. " + request.getPermissionSyncPackages().toString());
        }
        serializer.endTag((String) null, XML_TAG_REQUEST);
    }

    private AtomicFile getStorageFileForUser(final int userId) {
        return this.mUserIdToStorageFile.computeIfAbsent(Integer.valueOf(userId), new Function() { // from class: com.android.server.companion.SystemDataTransferRequestDataStore$$ExternalSyntheticLambda0
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                AtomicFile createStorageFileForUser;
                Integer num = (Integer) obj;
                createStorageFileForUser = DataStoreUtils.createStorageFileForUser(userId, SystemDataTransferRequestDataStore.FILE_NAME);
                return createStorageFileForUser;
            }
        });
    }
}
