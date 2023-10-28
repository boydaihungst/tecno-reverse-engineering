package com.android.server.companion;

import android.os.Environment;
import android.util.AtomicFile;
import android.util.Slog;
import com.android.internal.util.FunctionalUtils;
import java.io.File;
import java.io.FileOutputStream;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public final class DataStoreUtils {
    private static final String TAG = "CompanionDevice_DataStoreUtils";

    /* JADX INFO: Access modifiers changed from: package-private */
    public static boolean isStartOfTag(XmlPullParser parser, String tag) throws XmlPullParserException {
        return parser.getEventType() == 2 && tag.equals(parser.getName());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static boolean isEndOfTag(XmlPullParser parser, String tag) throws XmlPullParserException {
        return parser.getEventType() == 3 && tag.equals(parser.getName());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static AtomicFile createStorageFileForUser(int userId, String fileName) {
        return new AtomicFile(getBaseStorageFileForUser(userId, fileName));
    }

    private static File getBaseStorageFileForUser(int userId, String fileName) {
        return new File(Environment.getDataSystemDeDirectory(userId), fileName);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void writeToFileSafely(AtomicFile file, FunctionalUtils.ThrowingConsumer<FileOutputStream> consumer) {
        try {
            file.write(consumer);
        } catch (Exception e) {
            Slog.e(TAG, "Error while writing to file " + file, e);
        }
    }

    private DataStoreUtils() {
    }
}
