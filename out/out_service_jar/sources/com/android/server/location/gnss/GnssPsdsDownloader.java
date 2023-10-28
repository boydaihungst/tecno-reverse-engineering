package com.android.server.location.gnss;

import android.net.TrafficStats;
import android.util.Log;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.TimeUnit;
/* loaded from: classes.dex */
class GnssPsdsDownloader {
    static final int LONG_TERM_PSDS_SERVER_INDEX = 1;
    private static final long MAXIMUM_CONTENT_LENGTH_BYTES = 1000000;
    private static final int MAX_PSDS_TYPE_INDEX = 3;
    private static final int NORMAL_PSDS_SERVER_INDEX = 2;
    static final long PSDS_INTERVAL = 86400000;
    private static final int REALTIME_PSDS_SERVER_INDEX = 3;
    private final String[] mLongTermPsdsServers;
    private int mNextServerIndex;
    private final String[] mPsdsServers;
    private static final String TAG = "GnssPsdsDownloader";
    private static final boolean DEBUG = Log.isLoggable(TAG, 3);
    private static final int CONNECTION_TIMEOUT_MS = (int) TimeUnit.SECONDS.toMillis(30);
    private static final int READ_TIMEOUT_MS = (int) TimeUnit.SECONDS.toMillis(60);

    /* JADX INFO: Access modifiers changed from: package-private */
    public GnssPsdsDownloader(Properties properties) {
        String longTermPsdsServer1 = properties.getProperty("LONGTERM_PSDS_SERVER_1");
        String longTermPsdsServer2 = properties.getProperty("LONGTERM_PSDS_SERVER_2");
        String longTermPsdsServer3 = properties.getProperty("LONGTERM_PSDS_SERVER_3");
        int count = longTermPsdsServer1 != null ? 0 + 1 : 0;
        count = longTermPsdsServer2 != null ? count + 1 : count;
        count = longTermPsdsServer3 != null ? count + 1 : count;
        if (count == 0) {
            Log.e(TAG, "No Long-Term PSDS servers were specified in the GnssConfiguration");
            this.mLongTermPsdsServers = null;
        } else {
            String[] strArr = new String[count];
            this.mLongTermPsdsServers = strArr;
            int count2 = 0;
            if (longTermPsdsServer1 != null) {
                strArr[0] = longTermPsdsServer1;
                count2 = 0 + 1;
            }
            if (longTermPsdsServer2 != null) {
                strArr[count2] = longTermPsdsServer2;
                count2++;
            }
            if (longTermPsdsServer3 != null) {
                strArr[count2] = longTermPsdsServer3;
                count2++;
            }
            Random random = new Random();
            this.mNextServerIndex = random.nextInt(count2);
        }
        String normalPsdsServer = properties.getProperty("NORMAL_PSDS_SERVER");
        String realtimePsdsServer = properties.getProperty("REALTIME_PSDS_SERVER");
        String[] strArr2 = new String[4];
        this.mPsdsServers = strArr2;
        strArr2[2] = normalPsdsServer;
        strArr2[3] = realtimePsdsServer;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public byte[] downloadPsdsData(int psdsType) {
        byte[] result = null;
        int startIndex = this.mNextServerIndex;
        if (psdsType == 1 && this.mLongTermPsdsServers == null) {
            return null;
        }
        if (psdsType > 1 && psdsType <= 3 && this.mPsdsServers[psdsType] == null) {
            return null;
        }
        if (psdsType == 1) {
            while (result == null) {
                result = doDownloadWithTrafficAccounted(this.mLongTermPsdsServers[this.mNextServerIndex]);
                int i = this.mNextServerIndex + 1;
                this.mNextServerIndex = i;
                if (i == this.mLongTermPsdsServers.length) {
                    this.mNextServerIndex = 0;
                }
                if (this.mNextServerIndex == startIndex) {
                    return result;
                }
            }
            return result;
        } else if (psdsType <= 1 || psdsType > 3) {
            return null;
        } else {
            byte[] result2 = doDownloadWithTrafficAccounted(this.mPsdsServers[psdsType]);
            return result2;
        }
    }

    private byte[] doDownloadWithTrafficAccounted(String url) {
        int oldTag = TrafficStats.getAndSetThreadStatsTag(-188);
        try {
            byte[] result = doDownload(url);
            return result;
        } finally {
            TrafficStats.setThreadStatsTag(oldTag);
        }
    }

    /* JADX DEBUG: Another duplicated slice has different insns count: {[IF]}, finally: {[IF, INVOKE] complete} */
    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [182=6, 183=5] */
    private byte[] doDownload(String url) {
        boolean z = DEBUG;
        if (z) {
            Log.d(TAG, "Downloading PSDS data from " + url);
        }
        HttpURLConnection connection = null;
        try {
            try {
                HttpURLConnection connection2 = (HttpURLConnection) new URL(url).openConnection();
                connection2.setRequestProperty("Accept", "*/*, application/vnd.wap.mms-message, application/vnd.wap.sic");
                connection2.setRequestProperty("x-wap-profile", "http://www.openmobilealliance.org/tech/profiles/UAPROF/ccppschema-20021212#");
                connection2.setConnectTimeout(CONNECTION_TIMEOUT_MS);
                connection2.setReadTimeout(READ_TIMEOUT_MS);
                connection2.connect();
                int statusCode = connection2.getResponseCode();
                if (statusCode != 200) {
                    if (z) {
                        Log.d(TAG, "HTTP error downloading gnss PSDS: " + statusCode);
                    }
                    if (connection2 != null) {
                        connection2.disconnect();
                    }
                    return null;
                }
                InputStream in = connection2.getInputStream();
                try {
                    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                    byte[] buffer = new byte[1024];
                    do {
                        int count = in.read(buffer);
                        if (count == -1) {
                            byte[] byteArray = bytes.toByteArray();
                            if (in != null) {
                                in.close();
                            }
                            if (connection2 != null) {
                                connection2.disconnect();
                            }
                            return byteArray;
                        }
                        bytes.write(buffer, 0, count);
                    } while (bytes.size() <= MAXIMUM_CONTENT_LENGTH_BYTES);
                    if (DEBUG) {
                        Log.d(TAG, "PSDS file too large");
                    }
                    if (in != null) {
                        in.close();
                    }
                    if (connection2 != null) {
                        connection2.disconnect();
                    }
                    return null;
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
            } catch (IOException ioe) {
                if (DEBUG) {
                    Log.d(TAG, "Error downloading gnss PSDS: ", ioe);
                }
                if (0 != 0) {
                    connection.disconnect();
                }
                return null;
            }
        } catch (Throwable th3) {
            if (0 != 0) {
                connection.disconnect();
            }
            throw th3;
        }
    }
}
