package com.android.server.app;

import android.os.FileUtils;
import android.util.ArrayMap;
import android.util.AtomicFile;
import android.util.Slog;
import android.util.TypedXmlPullParser;
import android.util.TypedXmlSerializer;
import android.util.Xml;
import com.android.internal.util.FrameworkStatsLog;
import com.android.internal.util.XmlUtils;
import com.android.server.am.HostingRecord;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes.dex */
public class GameManagerSettings {
    private static final String ATTR_GAME_MODE = "gameMode";
    private static final String ATTR_NAME = "name";
    private static final String GAME_SERVICE_FILE_NAME = "game-manager-service.xml";
    private static final String TAG_PACKAGE = "package";
    private static final String TAG_PACKAGES = "packages";
    private final ArrayMap<String, Integer> mGameModes = new ArrayMap<>();
    final AtomicFile mSettingsFile;
    private final File mSystemDir;

    /* JADX INFO: Access modifiers changed from: package-private */
    public GameManagerSettings(File dataDir) {
        File file = new File(dataDir, HostingRecord.HOSTING_TYPE_SYSTEM);
        this.mSystemDir = file;
        file.mkdirs();
        FileUtils.setPermissions(file.toString(), 509, -1, -1);
        this.mSettingsFile = new AtomicFile(new File(file, GAME_SERVICE_FILE_NAME));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getGameModeLocked(String packageName) {
        if (this.mGameModes.containsKey(packageName)) {
            return this.mGameModes.get(packageName).intValue();
        }
        return 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setGameModeLocked(String packageName, int gameMode) {
        this.mGameModes.put(packageName, Integer.valueOf(gameMode));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void removeGame(String packageName) {
        this.mGameModes.remove(packageName);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void writePersistentDataLocked() {
        FileOutputStream fstr = null;
        try {
            fstr = this.mSettingsFile.startWrite();
            TypedXmlSerializer serializer = Xml.resolveSerializer(fstr);
            serializer.startDocument((String) null, true);
            serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
            serializer.startTag((String) null, TAG_PACKAGES);
            for (Map.Entry<String, Integer> entry : this.mGameModes.entrySet()) {
                serializer.startTag((String) null, "package");
                serializer.attribute((String) null, "name", entry.getKey());
                serializer.attributeInt((String) null, ATTR_GAME_MODE, entry.getValue().intValue());
                serializer.endTag((String) null, "package");
            }
            serializer.endTag((String) null, TAG_PACKAGES);
            serializer.endDocument();
            this.mSettingsFile.finishWrite(fstr);
            FileUtils.setPermissions(this.mSettingsFile.toString(), FrameworkStatsLog.HOTWORD_DETECTION_SERVICE_RESTARTED, -1, -1);
        } catch (IOException e) {
            this.mSettingsFile.failWrite(fstr);
            Slog.wtf(GameManagerService.TAG, "Unable to write game manager service settings, current changes will be lost at reboot", e);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean readPersistentDataLocked() {
        int type;
        this.mGameModes.clear();
        if (!this.mSettingsFile.exists()) {
            Slog.v(GameManagerService.TAG, "Settings file doesn't exists, skip reading");
            return false;
        }
        try {
            FileInputStream str = this.mSettingsFile.openRead();
            TypedXmlPullParser parser = Xml.resolvePullParser(str);
            while (true) {
                type = parser.next();
                if (type == 2 || type == 1) {
                    break;
                }
            }
            if (type != 2) {
                Slog.wtf(GameManagerService.TAG, "No start tag found in package manager settings");
                return false;
            }
            int outerDepth = parser.getDepth();
            while (true) {
                int type2 = parser.next();
                if (type2 == 1 || (type2 == 3 && parser.getDepth() <= outerDepth)) {
                    break;
                } else if (type2 != 3 && type2 != 4) {
                    String tagName = parser.getName();
                    if (tagName.equals("package")) {
                        readPackage(parser);
                    } else {
                        Slog.w(GameManagerService.TAG, "Unknown element: " + parser.getName());
                        XmlUtils.skipCurrentTag(parser);
                    }
                }
            }
            return true;
        } catch (IOException | XmlPullParserException e) {
            Slog.wtf(GameManagerService.TAG, "Error reading package manager settings", e);
            return false;
        }
    }

    private void readPackage(TypedXmlPullParser parser) throws XmlPullParserException, IOException {
        String name = null;
        int gameMode = 0;
        try {
            name = parser.getAttributeValue((String) null, "name");
            gameMode = parser.getAttributeInt((String) null, ATTR_GAME_MODE);
        } catch (XmlPullParserException e) {
            Slog.wtf(GameManagerService.TAG, "Error reading game mode", e);
        }
        if (name != null) {
            this.mGameModes.put(name, Integer.valueOf(gameMode));
        } else {
            XmlUtils.skipCurrentTag(parser);
        }
    }
}
