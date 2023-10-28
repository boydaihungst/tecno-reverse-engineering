package com.mediatek.internal.content;

import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.drm.DrmManagerClient;
import android.media.MediaFile;
import android.net.Uri;
import android.os.SystemProperties;
import android.provider.MediaStore;
import android.util.Log;
import android.webkit.MimeTypeMap;
import com.mediatek.media.MtkMediaStore;
import java.io.File;
import java.io.FileNotFoundException;
/* loaded from: classes.dex */
public class MtkFileSystemProviderHelper {
    private static final boolean DEBUG = false;
    private static final boolean LOG_INOTIFY = false;
    private static final String MIMETYPE_JPEG = "image/jpeg";
    private static final String MIMETYPE_JPG = "image/jpg";
    private static final String MIMETYPE_OCTET_STREAM = "application/octet-stream";
    private static final String TAG = "FileSystemProvider";
    private Context mContext;
    private String[] mDefaultProjection;
    private static final Uri BASE_URI = new Uri.Builder().scheme("content").authority("com.android.externalstorage.documents").build();
    private static final String[] DEFAULT_DOCUMENT_PROJECTION = {"document_id", "mime_type", "_display_name", "last_modified", "flags", "_size", "_data", "is_drm", MtkMediaStore.MediaColumns.DRM_METHOD};

    public MtkFileSystemProviderHelper(Context context) {
        this.mContext = null;
        this.mContext = context;
    }

    public static boolean isMtkDrmApp() {
        return SystemProperties.getBoolean("ro.vendor.mtk_oma_drm_support", false);
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [133=5] */
    /* JADX WARN: Code restructure failed: missing block: B:30:0x00a9, code lost:
        if (r15 != null) goto L23;
     */
    /* JADX WARN: Code restructure failed: missing block: B:31:0x00ab, code lost:
        r15.close();
     */
    /* JADX WARN: Code restructure failed: missing block: B:39:0x00bb, code lost:
        if (r15 == null) goto L24;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public void supportDRM(File file, MatrixCursor.RowBuilder row, String docId, String mimeType, File visibleFile) throws FileNotFoundException {
        File file2;
        String mimeType2;
        String extension;
        String displayName = file.getName();
        if (isMtkDrmApp() && !file.isDirectory()) {
            int lastDot = displayName.lastIndexOf(46);
            if (lastDot >= 0) {
                String extension2 = displayName.substring(lastDot + 1).toLowerCase();
                extension = extension2;
            } else {
                extension = null;
            }
            if (extension != null && extension.equalsIgnoreCase("dcf")) {
                Uri fileUri = MediaStore.Files.getContentUri("external");
                String[] projection = {"is_drm", MtkMediaStore.MediaColumns.DRM_METHOD, "mime_type"};
                Cursor drmCursor = null;
                file2 = visibleFile;
                try {
                    if (file2 != null) {
                        drmCursor = this.mContext.getContentResolver().query(fileUri, projection, "_data = ?", new String[]{file2.getAbsolutePath()}, null);
                        if (drmCursor == null || !drmCursor.moveToFirst()) {
                            mimeType2 = mimeType;
                        } else {
                            int isDrm = drmCursor.getInt(drmCursor.getColumnIndex("is_drm"));
                            int drmMethod = drmCursor.getInt(drmCursor.getColumnIndex(MtkMediaStore.MediaColumns.DRM_METHOD));
                            mimeType2 = drmCursor.getString(drmCursor.getColumnIndex("mime_type"));
                            try {
                                row.add("is_drm", Integer.valueOf(isDrm));
                                row.add(MtkMediaStore.MediaColumns.DRM_METHOD, Integer.valueOf(drmMethod));
                            } catch (IllegalStateException e) {
                            } catch (Throwable th) {
                                th = th;
                                if (drmCursor != null) {
                                    drmCursor.close();
                                }
                                throw th;
                            }
                        }
                    } else {
                        Log.d(TAG, "VisibleFile is null");
                        mimeType2 = mimeType;
                    }
                } catch (IllegalStateException e2) {
                    mimeType2 = mimeType;
                } catch (Throwable th2) {
                    th = th2;
                }
            }
        }
        file2 = file;
        mimeType2 = mimeType;
        row.add("mime_type", mimeType2);
        row.add("_data", file2.getAbsolutePath());
    }

    public String getTypeForNameMtk(File file, String name) {
        int lastDot = name.lastIndexOf(46);
        if (lastDot >= 0) {
            String extension = name.substring(lastDot + 1).toLowerCase();
            if (extension.equalsIgnoreCase("dcf")) {
                return getTypeForDrmFile(file);
            }
            String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
            if (mime != null) {
                return mime;
            }
        }
        String mime2 = MediaFile.getMimeTypeForFile(name);
        if (mime2 != null) {
            return mime2;
        }
        return MIMETYPE_OCTET_STREAM;
    }

    private String getTypeForDrmFile(File file) {
        DrmManagerClient client = new DrmManagerClient(this.mContext);
        String rawFile = file.toString();
        if (client.canHandle(rawFile, (String) null)) {
            return client.getOriginalMimeType(rawFile);
        }
        return MIMETYPE_OCTET_STREAM;
    }

    public String[] getDefaultProjection() {
        return DEFAULT_DOCUMENT_PROJECTION;
    }
}
