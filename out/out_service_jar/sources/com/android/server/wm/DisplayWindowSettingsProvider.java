package com.android.server.wm;

import android.os.Environment;
import android.util.AtomicFile;
import android.util.Slog;
import android.util.TypedXmlPullParser;
import android.util.TypedXmlSerializer;
import android.util.Xml;
import android.view.DisplayAddress;
import android.view.DisplayInfo;
import com.android.internal.util.XmlUtils;
import com.android.server.wm.DisplayWindowSettings;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import org.xmlpull.v1.XmlPullParserException;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class DisplayWindowSettingsProvider implements DisplayWindowSettings.SettingsProvider {
    private static final String DATA_DISPLAY_SETTINGS_FILE_PATH = "system/display_settings.xml";
    private static final int IDENTIFIER_PORT = 1;
    private static final int IDENTIFIER_UNIQUE_ID = 0;
    private static final String TAG = "WindowManager";
    private static final String VENDOR_DISPLAY_SETTINGS_FILE_PATH = "etc/display_settings.xml";
    private static final String WM_DISPLAY_COMMIT_TAG = "wm-displays";
    private ReadableSettings mBaseSettings;
    private final WritableSettings mOverrideSettings;

    /* loaded from: classes2.dex */
    @interface DisplayIdentifierType {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public interface ReadableSettingsStorage {
        InputStream openRead() throws IOException;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public interface WritableSettingsStorage extends ReadableSettingsStorage {
        void finishWrite(OutputStream outputStream, boolean z);

        OutputStream startWrite() throws IOException;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public DisplayWindowSettingsProvider() {
        this(new AtomicFileStorage(getVendorSettingsFile()), new AtomicFileStorage(getOverrideSettingsFile()));
    }

    DisplayWindowSettingsProvider(ReadableSettingsStorage baseSettingsStorage, WritableSettingsStorage overrideSettingsStorage) {
        this.mBaseSettings = new ReadableSettings(baseSettingsStorage);
        this.mOverrideSettings = new WritableSettings(overrideSettingsStorage);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setBaseSettingsFilePath(String path) {
        AtomicFile settingsFile;
        File file = path != null ? new File(path) : null;
        if (file != null && file.exists()) {
            settingsFile = new AtomicFile(file, WM_DISPLAY_COMMIT_TAG);
        } else {
            Slog.w("WindowManager", "display settings " + path + " does not exist, using vendor defaults");
            settingsFile = getVendorSettingsFile();
        }
        setBaseSettingsStorage(new AtomicFileStorage(settingsFile));
    }

    void setBaseSettingsStorage(ReadableSettingsStorage baseSettingsStorage) {
        this.mBaseSettings = new ReadableSettings(baseSettingsStorage);
    }

    @Override // com.android.server.wm.DisplayWindowSettings.SettingsProvider
    public DisplayWindowSettings.SettingsProvider.SettingsEntry getSettings(DisplayInfo info) {
        DisplayWindowSettings.SettingsProvider.SettingsEntry baseSettings = this.mBaseSettings.getSettingsEntry(info);
        DisplayWindowSettings.SettingsProvider.SettingsEntry overrideSettings = this.mOverrideSettings.getOrCreateSettingsEntry(info);
        if (baseSettings == null) {
            return new DisplayWindowSettings.SettingsProvider.SettingsEntry(overrideSettings);
        }
        DisplayWindowSettings.SettingsProvider.SettingsEntry mergedSettings = new DisplayWindowSettings.SettingsProvider.SettingsEntry(baseSettings);
        mergedSettings.updateFrom(overrideSettings);
        return mergedSettings;
    }

    @Override // com.android.server.wm.DisplayWindowSettings.SettingsProvider
    public DisplayWindowSettings.SettingsProvider.SettingsEntry getOverrideSettings(DisplayInfo info) {
        return new DisplayWindowSettings.SettingsProvider.SettingsEntry(this.mOverrideSettings.getOrCreateSettingsEntry(info));
    }

    @Override // com.android.server.wm.DisplayWindowSettings.SettingsProvider
    public void updateOverrideSettings(DisplayInfo info, DisplayWindowSettings.SettingsProvider.SettingsEntry overrides) {
        this.mOverrideSettings.updateSettingsEntry(info, overrides);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static class ReadableSettings {
        protected int mIdentifierType;
        protected final Map<String, DisplayWindowSettings.SettingsProvider.SettingsEntry> mSettings = new HashMap();

        ReadableSettings(ReadableSettingsStorage settingsStorage) {
            loadSettings(settingsStorage);
        }

        final DisplayWindowSettings.SettingsProvider.SettingsEntry getSettingsEntry(DisplayInfo info) {
            String identifier = getIdentifier(info);
            DisplayWindowSettings.SettingsProvider.SettingsEntry settings = this.mSettings.get(identifier);
            if (settings != null) {
                return settings;
            }
            DisplayWindowSettings.SettingsProvider.SettingsEntry settings2 = this.mSettings.get(info.name);
            if (settings2 != null) {
                this.mSettings.remove(info.name);
                this.mSettings.put(identifier, settings2);
                return settings2;
            }
            return null;
        }

        protected final String getIdentifier(DisplayInfo displayInfo) {
            if (this.mIdentifierType == 1 && displayInfo.address != null && (displayInfo.address instanceof DisplayAddress.Physical)) {
                return "port:" + displayInfo.address.getPort();
            }
            return displayInfo.uniqueId;
        }

        private void loadSettings(ReadableSettingsStorage settingsStorage) {
            FileData fileData = DisplayWindowSettingsProvider.readSettings(settingsStorage);
            if (fileData != null) {
                this.mIdentifierType = fileData.mIdentifierType;
                this.mSettings.putAll(fileData.mSettings);
            }
        }
    }

    /* loaded from: classes2.dex */
    private static final class WritableSettings extends ReadableSettings {
        private final WritableSettingsStorage mSettingsStorage;

        WritableSettings(WritableSettingsStorage settingsStorage) {
            super(settingsStorage);
            this.mSettingsStorage = settingsStorage;
        }

        DisplayWindowSettings.SettingsProvider.SettingsEntry getOrCreateSettingsEntry(DisplayInfo info) {
            String identifier = getIdentifier(info);
            DisplayWindowSettings.SettingsProvider.SettingsEntry settings = this.mSettings.get(identifier);
            if (settings != null) {
                return settings;
            }
            DisplayWindowSettings.SettingsProvider.SettingsEntry settings2 = this.mSettings.get(info.name);
            if (settings2 != null) {
                this.mSettings.remove(info.name);
                this.mSettings.put(identifier, settings2);
                writeSettings();
                return settings2;
            }
            DisplayWindowSettings.SettingsProvider.SettingsEntry settings3 = new DisplayWindowSettings.SettingsProvider.SettingsEntry();
            this.mSettings.put(identifier, settings3);
            return settings3;
        }

        void updateSettingsEntry(DisplayInfo info, DisplayWindowSettings.SettingsProvider.SettingsEntry settings) {
            DisplayWindowSettings.SettingsProvider.SettingsEntry overrideSettings = getOrCreateSettingsEntry(info);
            boolean changed = overrideSettings.setTo(settings);
            if (changed) {
                writeSettings();
            }
        }

        private void writeSettings() {
            FileData fileData = new FileData();
            fileData.mIdentifierType = this.mIdentifierType;
            fileData.mSettings.putAll(this.mSettings);
            DisplayWindowSettingsProvider.writeSettings(this.mSettingsStorage, fileData);
        }
    }

    private static AtomicFile getVendorSettingsFile() {
        File vendorFile = new File(Environment.getProductDirectory(), VENDOR_DISPLAY_SETTINGS_FILE_PATH);
        if (!vendorFile.exists()) {
            vendorFile = new File(Environment.getVendorDirectory(), VENDOR_DISPLAY_SETTINGS_FILE_PATH);
        }
        return new AtomicFile(vendorFile, WM_DISPLAY_COMMIT_TAG);
    }

    private static AtomicFile getOverrideSettingsFile() {
        File overrideSettingsFile = new File(Environment.getDataDirectory(), DATA_DISPLAY_SETTINGS_FILE_PATH);
        return new AtomicFile(overrideSettingsFile, WM_DISPLAY_COMMIT_TAG);
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [341=8] */
    /* JADX INFO: Access modifiers changed from: private */
    public static FileData readSettings(ReadableSettingsStorage storage) {
        TypedXmlPullParser parser;
        int type;
        try {
            InputStream stream = storage.openRead();
            FileData fileData = new FileData();
            boolean success = false;
            try {
                try {
                    try {
                        try {
                            try {
                                parser = Xml.resolvePullParser(stream);
                                while (true) {
                                    type = parser.next();
                                    if (type == 2 || type == 1) {
                                        break;
                                    }
                                }
                            } catch (Throwable th) {
                                try {
                                    stream.close();
                                } catch (IOException e) {
                                }
                                throw th;
                            }
                        } catch (IndexOutOfBoundsException e2) {
                            Slog.w("WindowManager", "Failed parsing " + e2);
                            stream.close();
                        } catch (NumberFormatException e3) {
                            Slog.w("WindowManager", "Failed parsing " + e3);
                            stream.close();
                        }
                    } catch (IOException e4) {
                        Slog.w("WindowManager", "Failed parsing " + e4);
                        stream.close();
                    } catch (XmlPullParserException e5) {
                        Slog.w("WindowManager", "Failed parsing " + e5);
                        stream.close();
                    }
                } catch (IllegalStateException e6) {
                    Slog.w("WindowManager", "Failed parsing " + e6);
                    stream.close();
                } catch (NullPointerException e7) {
                    Slog.w("WindowManager", "Failed parsing " + e7);
                    stream.close();
                }
            } catch (IOException e8) {
            }
            if (type == 2) {
                int outerDepth = parser.getDepth();
                while (true) {
                    int type2 = parser.next();
                    if (type2 == 1 || (type2 == 3 && parser.getDepth() <= outerDepth)) {
                        break;
                    } else if (type2 != 3 && type2 != 4) {
                        String tagName = parser.getName();
                        if (tagName.equals("display")) {
                            readDisplay(parser, fileData);
                        } else if (tagName.equals("config")) {
                            readConfig(parser, fileData);
                        } else {
                            Slog.w("WindowManager", "Unknown element under <display-settings>: " + parser.getName());
                            XmlUtils.skipCurrentTag(parser);
                        }
                    }
                }
                success = true;
                stream.close();
                if (!success) {
                    fileData.mSettings.clear();
                }
                return fileData;
            }
            throw new IllegalStateException("no start tag found");
        } catch (IOException e9) {
            Slog.i("WindowManager", "No existing display settings, starting empty");
            return null;
        }
    }

    private static int getIntAttribute(TypedXmlPullParser parser, String name, int defaultValue) {
        return parser.getAttributeInt((String) null, name, defaultValue);
    }

    private static Integer getIntegerAttribute(TypedXmlPullParser parser, String name, Integer defaultValue) {
        try {
            return Integer.valueOf(parser.getAttributeInt((String) null, name));
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private static Boolean getBooleanAttribute(TypedXmlPullParser parser, String name, Boolean defaultValue) {
        try {
            return Boolean.valueOf(parser.getAttributeBoolean((String) null, name));
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private static void readDisplay(TypedXmlPullParser parser, FileData fileData) throws NumberFormatException, XmlPullParserException, IOException {
        String name = parser.getAttributeValue((String) null, "name");
        if (name != null) {
            DisplayWindowSettings.SettingsProvider.SettingsEntry settingsEntry = new DisplayWindowSettings.SettingsProvider.SettingsEntry();
            settingsEntry.mWindowingMode = getIntAttribute(parser, "windowingMode", 0);
            settingsEntry.mUserRotationMode = getIntegerAttribute(parser, "userRotationMode", null);
            settingsEntry.mUserRotation = getIntegerAttribute(parser, "userRotation", null);
            settingsEntry.mForcedWidth = getIntAttribute(parser, "forcedWidth", 0);
            settingsEntry.mForcedHeight = getIntAttribute(parser, "forcedHeight", 0);
            settingsEntry.mForcedDensity = getIntAttribute(parser, "forcedDensity", 0);
            settingsEntry.mForcedScalingMode = getIntegerAttribute(parser, "forcedScalingMode", null);
            settingsEntry.mRemoveContentMode = getIntAttribute(parser, "removeContentMode", 0);
            settingsEntry.mShouldShowWithInsecureKeyguard = getBooleanAttribute(parser, "shouldShowWithInsecureKeyguard", null);
            settingsEntry.mShouldShowSystemDecors = getBooleanAttribute(parser, "shouldShowSystemDecors", null);
            Boolean shouldShowIme = getBooleanAttribute(parser, "shouldShowIme", null);
            if (shouldShowIme == null) {
                settingsEntry.mImePolicy = getIntegerAttribute(parser, "imePolicy", null);
            } else {
                settingsEntry.mImePolicy = Integer.valueOf(shouldShowIme.booleanValue() ? 0 : 1);
            }
            settingsEntry.mFixedToUserRotation = getIntegerAttribute(parser, "fixedToUserRotation", null);
            settingsEntry.mIgnoreOrientationRequest = getBooleanAttribute(parser, "ignoreOrientationRequest", null);
            settingsEntry.mIgnoreDisplayCutout = getBooleanAttribute(parser, "ignoreDisplayCutout", null);
            settingsEntry.mDontMoveToTop = getBooleanAttribute(parser, "dontMoveToTop", null);
            fileData.mSettings.put(name, settingsEntry);
        }
        XmlUtils.skipCurrentTag(parser);
    }

    private static void readConfig(TypedXmlPullParser parser, FileData fileData) throws NumberFormatException, XmlPullParserException, IOException {
        fileData.mIdentifierType = getIntAttribute(parser, "identifier", 0);
        XmlUtils.skipCurrentTag(parser);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void writeSettings(WritableSettingsStorage storage, FileData data) {
        try {
            OutputStream stream = storage.startWrite();
            boolean success = false;
            try {
                try {
                    TypedXmlSerializer out = Xml.resolveSerializer(stream);
                    out.startDocument((String) null, true);
                    out.startTag((String) null, "display-settings");
                    out.startTag((String) null, "config");
                    out.attributeInt((String) null, "identifier", data.mIdentifierType);
                    out.endTag((String) null, "config");
                    for (Map.Entry<String, DisplayWindowSettings.SettingsProvider.SettingsEntry> entry : data.mSettings.entrySet()) {
                        String displayIdentifier = entry.getKey();
                        DisplayWindowSettings.SettingsProvider.SettingsEntry settingsEntry = entry.getValue();
                        if (!settingsEntry.isEmpty()) {
                            out.startTag((String) null, "display");
                            out.attribute((String) null, "name", displayIdentifier);
                            if (settingsEntry.mWindowingMode != 0) {
                                out.attributeInt((String) null, "windowingMode", settingsEntry.mWindowingMode);
                            }
                            if (settingsEntry.mUserRotationMode != null) {
                                out.attributeInt((String) null, "userRotationMode", settingsEntry.mUserRotationMode.intValue());
                            }
                            if (settingsEntry.mUserRotation != null) {
                                out.attributeInt((String) null, "userRotation", settingsEntry.mUserRotation.intValue());
                            }
                            if (settingsEntry.mForcedWidth != 0 && settingsEntry.mForcedHeight != 0) {
                                out.attributeInt((String) null, "forcedWidth", settingsEntry.mForcedWidth);
                                out.attributeInt((String) null, "forcedHeight", settingsEntry.mForcedHeight);
                            }
                            if (settingsEntry.mForcedDensity != 0) {
                                out.attributeInt((String) null, "forcedDensity", settingsEntry.mForcedDensity);
                            }
                            if (settingsEntry.mForcedScalingMode != null) {
                                out.attributeInt((String) null, "forcedScalingMode", settingsEntry.mForcedScalingMode.intValue());
                            }
                            if (settingsEntry.mRemoveContentMode != 0) {
                                out.attributeInt((String) null, "removeContentMode", settingsEntry.mRemoveContentMode);
                            }
                            if (settingsEntry.mShouldShowWithInsecureKeyguard != null) {
                                out.attributeBoolean((String) null, "shouldShowWithInsecureKeyguard", settingsEntry.mShouldShowWithInsecureKeyguard.booleanValue());
                            }
                            if (settingsEntry.mShouldShowSystemDecors != null) {
                                out.attributeBoolean((String) null, "shouldShowSystemDecors", settingsEntry.mShouldShowSystemDecors.booleanValue());
                            }
                            if (settingsEntry.mImePolicy != null) {
                                out.attributeInt((String) null, "imePolicy", settingsEntry.mImePolicy.intValue());
                            }
                            if (settingsEntry.mFixedToUserRotation != null) {
                                out.attributeInt((String) null, "fixedToUserRotation", settingsEntry.mFixedToUserRotation.intValue());
                            }
                            if (settingsEntry.mIgnoreOrientationRequest != null) {
                                out.attributeBoolean((String) null, "ignoreOrientationRequest", settingsEntry.mIgnoreOrientationRequest.booleanValue());
                            }
                            if (settingsEntry.mIgnoreDisplayCutout != null) {
                                out.attributeBoolean((String) null, "ignoreDisplayCutout", settingsEntry.mIgnoreDisplayCutout.booleanValue());
                            }
                            if (settingsEntry.mDontMoveToTop != null) {
                                out.attributeBoolean((String) null, "dontMoveToTop", settingsEntry.mDontMoveToTop.booleanValue());
                            }
                            out.endTag((String) null, "display");
                        }
                    }
                    out.endTag((String) null, "display-settings");
                    out.endDocument();
                    success = true;
                } catch (IOException e) {
                    Slog.w("WindowManager", "Failed to write display window settings.", e);
                }
            } finally {
                storage.finishWrite(stream, false);
            }
        } catch (IOException e2) {
            Slog.w("WindowManager", "Failed to write display settings: " + e2);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static final class FileData {
        int mIdentifierType;
        final Map<String, DisplayWindowSettings.SettingsProvider.SettingsEntry> mSettings;

        private FileData() {
            this.mSettings = new HashMap();
        }

        public String toString() {
            return "FileData{mIdentifierType=" + this.mIdentifierType + ", mSettings=" + this.mSettings + '}';
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static final class AtomicFileStorage implements WritableSettingsStorage {
        private final AtomicFile mAtomicFile;

        AtomicFileStorage(AtomicFile atomicFile) {
            this.mAtomicFile = atomicFile;
        }

        @Override // com.android.server.wm.DisplayWindowSettingsProvider.ReadableSettingsStorage
        public InputStream openRead() throws FileNotFoundException {
            return this.mAtomicFile.openRead();
        }

        @Override // com.android.server.wm.DisplayWindowSettingsProvider.WritableSettingsStorage
        public OutputStream startWrite() throws IOException {
            return this.mAtomicFile.startWrite();
        }

        @Override // com.android.server.wm.DisplayWindowSettingsProvider.WritableSettingsStorage
        public void finishWrite(OutputStream os, boolean success) {
            if (!(os instanceof FileOutputStream)) {
                throw new IllegalArgumentException("Unexpected OutputStream as argument: " + os);
            }
            FileOutputStream fos = (FileOutputStream) os;
            if (success) {
                this.mAtomicFile.finishWrite(fos);
            } else {
                this.mAtomicFile.failWrite(fos);
            }
        }
    }
}
