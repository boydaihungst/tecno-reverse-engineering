package com.android.server.net.watchlist;

import android.os.Environment;
import android.util.AtomicFile;
import android.util.Log;
import android.util.Slog;
import android.util.TypedXmlPullParser;
import android.util.TypedXmlSerializer;
import android.util.Xml;
import com.android.internal.util.HexDump;
import com.android.internal.util.XmlUtils;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes2.dex */
class WatchlistSettings {
    private static final String FILE_NAME = "watchlist_settings.xml";
    private static final int SECRET_KEY_LENGTH = 48;
    private static final String TAG = "WatchlistSettings";
    private static final WatchlistSettings sInstance = new WatchlistSettings();
    private byte[] mPrivacySecretKey;
    private final AtomicFile mXmlFile;

    public static WatchlistSettings getInstance() {
        return sInstance;
    }

    private WatchlistSettings() {
        this(getSystemWatchlistFile());
    }

    static File getSystemWatchlistFile() {
        return new File(Environment.getDataSystemDirectory(), FILE_NAME);
    }

    protected WatchlistSettings(File xmlFile) {
        this.mPrivacySecretKey = null;
        this.mXmlFile = new AtomicFile(xmlFile, "net-watchlist");
        reloadSettings();
        if (this.mPrivacySecretKey == null) {
            this.mPrivacySecretKey = generatePrivacySecretKey();
            saveSettings();
        }
    }

    private void reloadSettings() {
        if (!this.mXmlFile.exists()) {
            return;
        }
        try {
            FileInputStream stream = this.mXmlFile.openRead();
            TypedXmlPullParser parser = Xml.resolvePullParser(stream);
            XmlUtils.beginDocument(parser, "network-watchlist-settings");
            int outerDepth = parser.getDepth();
            while (XmlUtils.nextElementWithin(parser, outerDepth)) {
                if (parser.getName().equals("secret-key")) {
                    this.mPrivacySecretKey = parseSecretKey(parser);
                }
            }
            Slog.i(TAG, "Reload watchlist settings done");
            if (stream != null) {
                stream.close();
            }
        } catch (IOException | IllegalStateException | IndexOutOfBoundsException | NullPointerException | NumberFormatException | XmlPullParserException e) {
            Slog.e(TAG, "Failed parsing xml", e);
        }
    }

    private byte[] parseSecretKey(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(2, null, "secret-key");
        byte[] key = HexDump.hexStringToByteArray(parser.nextText());
        parser.require(3, null, "secret-key");
        if (key == null || key.length != 48) {
            Log.e(TAG, "Unable to parse secret key");
            return null;
        }
        return key;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public synchronized byte[] getPrivacySecretKey() {
        byte[] key;
        key = new byte[48];
        System.arraycopy(this.mPrivacySecretKey, 0, key, 0, 48);
        return key;
    }

    private byte[] generatePrivacySecretKey() {
        byte[] key = new byte[48];
        new SecureRandom().nextBytes(key);
        return key;
    }

    private void saveSettings() {
        try {
            FileOutputStream stream = this.mXmlFile.startWrite();
            try {
                TypedXmlSerializer out = Xml.resolveSerializer(stream);
                out.startDocument((String) null, true);
                out.startTag((String) null, "network-watchlist-settings");
                out.startTag((String) null, "secret-key");
                out.text(HexDump.toHexString(this.mPrivacySecretKey));
                out.endTag((String) null, "secret-key");
                out.endTag((String) null, "network-watchlist-settings");
                out.endDocument();
                this.mXmlFile.finishWrite(stream);
            } catch (IOException e) {
                Log.w(TAG, "Failed to write display settings, restoring backup.", e);
                this.mXmlFile.failWrite(stream);
            }
        } catch (IOException e2) {
            Log.w(TAG, "Failed to write display settings: " + e2);
        }
    }
}
