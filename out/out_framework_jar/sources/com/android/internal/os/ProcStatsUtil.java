package com.android.internal.os;

import android.os.StrictMode;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
/* loaded from: classes4.dex */
public final class ProcStatsUtil {
    private static final boolean DEBUG = false;
    private static final int READ_SIZE = 1024;
    private static final String TAG = "ProcStatsUtil";

    private ProcStatsUtil() {
    }

    public static String readNullSeparatedFile(String path) {
        String contents = readSingleLineProcFile(path);
        if (contents == null) {
            return null;
        }
        int endIndex = contents.indexOf("\u0000\u0000");
        if (endIndex != -1) {
            contents = contents.substring(0, endIndex);
        }
        return contents.replace("\u0000", " ");
    }

    public static String readSingleLineProcFile(String path) {
        return readTerminatedProcFile(path, (byte) 10);
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [145=4, 151=5] */
    public static String readTerminatedProcFile(String path, byte terminator) {
        boolean foundTerminator;
        StrictMode.ThreadPolicy savedPolicy = StrictMode.allowThreadDiskReads();
        try {
            FileInputStream is = new FileInputStream(path);
            ByteArrayOutputStream byteStream = null;
            try {
                byte[] buffer = new byte[1024];
                do {
                    int len = is.read(buffer);
                    if (len <= 0) {
                        break;
                    }
                    int terminatingIndex = -1;
                    int i = 0;
                    while (true) {
                        if (i >= len) {
                            break;
                        } else if (buffer[i] == terminator) {
                            terminatingIndex = i;
                            break;
                        } else {
                            i++;
                        }
                    }
                    foundTerminator = terminatingIndex != -1;
                    if (foundTerminator && byteStream == null) {
                        String str = new String(buffer, 0, terminatingIndex);
                        is.close();
                        return str;
                    }
                    if (byteStream == null) {
                        byteStream = new ByteArrayOutputStream(1024);
                    }
                    byteStream.write(buffer, 0, foundTerminator ? terminatingIndex : len);
                } while (!foundTerminator);
                if (byteStream == null) {
                    is.close();
                    return "";
                }
                String byteArrayOutputStream = byteStream.toString();
                is.close();
                return byteArrayOutputStream;
            } catch (Throwable th) {
                try {
                    is.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
                throw th;
            }
        } catch (IOException e) {
            return null;
        } finally {
            StrictMode.setThreadPolicy(savedPolicy);
        }
    }
}
