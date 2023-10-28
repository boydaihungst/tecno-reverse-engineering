package com.android.server.timezone;

import android.util.AtomicFile;
import android.util.Slog;
import android.util.TypedXmlPullParser;
import android.util.TypedXmlSerializer;
import android.util.Xml;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes2.dex */
final class PackageStatusStorage {
    private static final String ATTRIBUTE_CHECK_STATUS = "checkStatus";
    private static final String ATTRIBUTE_DATA_APP_VERSION = "dataAppPackageVersion";
    private static final String ATTRIBUTE_OPTIMISTIC_LOCK_ID = "optimisticLockId";
    private static final String ATTRIBUTE_UPDATE_APP_VERSION = "updateAppPackageVersion";
    private static final String LOG_TAG = "timezone.PackageStatusStorage";
    private static final String TAG_PACKAGE_STATUS = "PackageStatus";
    private static final long UNKNOWN_PACKAGE_VERSION = -1;
    private final AtomicFile mPackageStatusFile;

    /* JADX INFO: Access modifiers changed from: package-private */
    public PackageStatusStorage(File storageDir) {
        this.mPackageStatusFile = new AtomicFile(new File(storageDir, "package-status.xml"), "timezone-status");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void initialize() throws IOException {
        if (!this.mPackageStatusFile.getBaseFile().exists()) {
            insertInitialPackageStatus();
        }
    }

    void deleteFileForTests() {
        synchronized (this) {
            this.mPackageStatusFile.delete();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public PackageStatus getPackageStatus() {
        PackageStatus packageStatusLocked;
        synchronized (this) {
            try {
                try {
                    packageStatusLocked = getPackageStatusLocked();
                } catch (ParseException e) {
                    Slog.e(LOG_TAG, "Package status invalid, resetting and retrying", e);
                    recoverFromBadData(e);
                    try {
                        return getPackageStatusLocked();
                    } catch (ParseException e2) {
                        throw new IllegalStateException("Recovery from bad file failed", e2);
                    }
                }
            } catch (Throwable th) {
                throw th;
            }
        }
        return packageStatusLocked;
    }

    private PackageStatus getPackageStatusLocked() throws ParseException {
        try {
            FileInputStream fis = this.mPackageStatusFile.openRead();
            TypedXmlPullParser parser = parseToPackageStatusTag(fis);
            Integer checkStatus = getNullableIntAttribute(parser, ATTRIBUTE_CHECK_STATUS);
            if (checkStatus != null) {
                int updateAppVersion = getIntAttribute(parser, ATTRIBUTE_UPDATE_APP_VERSION);
                int dataAppVersion = getIntAttribute(parser, ATTRIBUTE_DATA_APP_VERSION);
                PackageStatus packageStatus = new PackageStatus(checkStatus.intValue(), new PackageVersions(updateAppVersion, dataAppVersion));
                if (fis != null) {
                    fis.close();
                }
                return packageStatus;
            }
            if (fis != null) {
                fis.close();
            }
            return null;
        } catch (IOException e) {
            ParseException e2 = new ParseException("Error reading package status", 0);
            e2.initCause(e);
            throw e2;
        }
    }

    private int recoverFromBadData(Exception cause) {
        this.mPackageStatusFile.delete();
        try {
            return insertInitialPackageStatus();
        } catch (IOException e) {
            IllegalStateException fatal = new IllegalStateException(e);
            fatal.addSuppressed(cause);
            throw fatal;
        }
    }

    private int insertInitialPackageStatus() throws IOException {
        int initialOptimisticLockId = (int) System.currentTimeMillis();
        writePackageStatusLocked(null, initialOptimisticLockId, null);
        return initialOptimisticLockId;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public CheckToken generateCheckToken(PackageVersions currentInstalledVersions) {
        int optimisticLockId;
        CheckToken checkToken;
        if (currentInstalledVersions == null) {
            throw new NullPointerException("currentInstalledVersions == null");
        }
        synchronized (this) {
            try {
                optimisticLockId = getCurrentOptimisticLockId();
            } catch (ParseException e) {
                Slog.w(LOG_TAG, "Unable to find optimistic lock ID from package status");
                optimisticLockId = recoverFromBadData(e);
            }
            int newOptimisticLockId = optimisticLockId + 1;
            try {
                boolean statusUpdated = writePackageStatusWithOptimisticLockCheck(optimisticLockId, newOptimisticLockId, 1, currentInstalledVersions);
                if (!statusUpdated) {
                    throw new IllegalStateException("Unable to update status to CHECK_STARTED. synchronization failure?");
                }
                checkToken = new CheckToken(newOptimisticLockId, currentInstalledVersions);
            } catch (IOException e2) {
                throw new IllegalStateException(e2);
            }
        }
        return checkToken;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void resetCheckState() {
        int optimisticLockId;
        synchronized (this) {
            try {
                optimisticLockId = getCurrentOptimisticLockId();
            } catch (ParseException e) {
                Slog.w(LOG_TAG, "resetCheckState: Unable to find optimistic lock ID from package status");
                optimisticLockId = recoverFromBadData(e);
            }
            int newOptimisticLockId = optimisticLockId + 1;
            try {
                if (!writePackageStatusWithOptimisticLockCheck(optimisticLockId, newOptimisticLockId, null, null)) {
                    throw new IllegalStateException("resetCheckState: Unable to reset package status, newOptimisticLockId=" + newOptimisticLockId);
                }
            } catch (IOException e2) {
                throw new IllegalStateException(e2);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean markChecked(CheckToken checkToken, boolean succeeded) {
        boolean writePackageStatusWithOptimisticLockCheck;
        synchronized (this) {
            int optimisticLockId = checkToken.mOptimisticLockId;
            int newOptimisticLockId = optimisticLockId + 1;
            int status = succeeded ? 2 : 3;
            try {
                writePackageStatusWithOptimisticLockCheck = writePackageStatusWithOptimisticLockCheck(optimisticLockId, newOptimisticLockId, Integer.valueOf(status), checkToken.mPackageVersions);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
        return writePackageStatusWithOptimisticLockCheck;
    }

    private int getCurrentOptimisticLockId() throws ParseException {
        try {
            FileInputStream fis = this.mPackageStatusFile.openRead();
            TypedXmlPullParser parser = parseToPackageStatusTag(fis);
            int intAttribute = getIntAttribute(parser, ATTRIBUTE_OPTIMISTIC_LOCK_ID);
            if (fis != null) {
                fis.close();
            }
            return intAttribute;
        } catch (IOException e) {
            ParseException e2 = new ParseException("Unable to read file", 0);
            e2.initCause(e);
            throw e2;
        }
    }

    /* JADX WARN: Code restructure failed: missing block: B:13:0x0025, code lost:
        throw new java.text.ParseException("Unable to find PackageStatus tag", 0);
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private static TypedXmlPullParser parseToPackageStatusTag(FileInputStream fis) throws ParseException {
        try {
            TypedXmlPullParser parser = Xml.resolvePullParser(fis);
            while (true) {
                int type = parser.next();
                if (type == 1) {
                    break;
                }
                String tag = parser.getName();
                if (type == 2 && TAG_PACKAGE_STATUS.equals(tag)) {
                    return parser;
                }
            }
        } catch (IOException e) {
            ParseException e2 = new ParseException("Error reading XML", 0);
            e.initCause(e);
            throw e2;
        } catch (XmlPullParserException e3) {
            throw new IllegalStateException("Unable to configure parser", e3);
        }
    }

    private boolean writePackageStatusWithOptimisticLockCheck(int optimisticLockId, int newOptimisticLockId, Integer status, PackageVersions packageVersions) throws IOException {
        try {
            int currentOptimisticLockId = getCurrentOptimisticLockId();
            if (currentOptimisticLockId != optimisticLockId) {
                return false;
            }
            writePackageStatusLocked(status, newOptimisticLockId, packageVersions);
            return true;
        } catch (ParseException e) {
            recoverFromBadData(e);
            return false;
        }
    }

    private void writePackageStatusLocked(Integer status, int optimisticLockId, PackageVersions packageVersions) throws IOException {
        if ((status == null) != (packageVersions == null)) {
            throw new IllegalArgumentException("Provide both status and packageVersions, or neither.");
        }
        FileOutputStream fos = null;
        try {
            fos = this.mPackageStatusFile.startWrite();
            TypedXmlSerializer serializer = Xml.resolveSerializer(fos);
            serializer.startDocument((String) null, true);
            serializer.startTag((String) null, TAG_PACKAGE_STATUS);
            String statusAttributeValue = status == null ? "" : Integer.toString(status.intValue());
            serializer.attribute((String) null, ATTRIBUTE_CHECK_STATUS, statusAttributeValue);
            serializer.attribute((String) null, ATTRIBUTE_OPTIMISTIC_LOCK_ID, Integer.toString(optimisticLockId));
            long dataAppVersion = -1;
            long updateAppVersion = status == null ? -1L : packageVersions.mUpdateAppVersion;
            serializer.attribute((String) null, ATTRIBUTE_UPDATE_APP_VERSION, Long.toString(updateAppVersion));
            if (status != null) {
                dataAppVersion = packageVersions.mDataAppVersion;
            }
            serializer.attribute((String) null, ATTRIBUTE_DATA_APP_VERSION, Long.toString(dataAppVersion));
            serializer.endTag((String) null, TAG_PACKAGE_STATUS);
            serializer.endDocument();
            serializer.flush();
            this.mPackageStatusFile.finishWrite(fos);
        } catch (IOException e) {
            if (fos != null) {
                this.mPackageStatusFile.failWrite(fos);
            }
            throw e;
        }
    }

    public void forceCheckStateForTests(int checkStatus, PackageVersions packageVersions) throws IOException {
        synchronized (this) {
            try {
                try {
                    int initialOptimisticLockId = (int) System.currentTimeMillis();
                    writePackageStatusLocked(Integer.valueOf(checkStatus), initialOptimisticLockId, packageVersions);
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    private static Integer getNullableIntAttribute(TypedXmlPullParser parser, String attributeName) throws ParseException {
        String attributeValue = parser.getAttributeValue((String) null, attributeName);
        try {
            if (attributeValue == null) {
                throw new ParseException("Attribute " + attributeName + " missing", 0);
            }
            if (attributeValue.isEmpty()) {
                return null;
            }
            return Integer.valueOf(Integer.parseInt(attributeValue));
        } catch (NumberFormatException e) {
            throw new ParseException("Bad integer for attributeName=" + attributeName + ": " + attributeValue, 0);
        }
    }

    private static int getIntAttribute(TypedXmlPullParser parser, String attributeName) throws ParseException {
        Integer value = getNullableIntAttribute(parser, attributeName);
        if (value == null) {
            throw new ParseException("Missing attribute " + attributeName, 0);
        }
        return value.intValue();
    }

    public void dump(PrintWriter printWriter) {
        printWriter.println("Package status: " + getPackageStatus());
    }
}
