package com.android.server.display;

import android.graphics.Point;
import android.hardware.display.BrightnessConfiguration;
import android.hardware.display.WifiDisplay;
import android.os.Handler;
import android.util.AtomicFile;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseLongArray;
import android.util.TimeUtils;
import android.util.TypedXmlPullParser;
import android.util.TypedXmlSerializer;
import android.util.Xml;
import com.android.internal.os.BackgroundThread;
import com.android.internal.util.XmlUtils;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import libcore.io.IoUtils;
import org.xmlpull.v1.XmlPullParserException;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public final class PersistentDataStore {
    private static final String ATTR_DEVICE_ADDRESS = "deviceAddress";
    private static final String ATTR_DEVICE_ALIAS = "deviceAlias";
    private static final String ATTR_DEVICE_NAME = "deviceName";
    private static final String ATTR_PACKAGE_NAME = "package-name";
    private static final String ATTR_TIME_STAMP = "timestamp";
    private static final String ATTR_UNIQUE_ID = "unique-id";
    private static final String ATTR_USER_SERIAL = "user-serial";
    static final String TAG = "DisplayManager.PersistentDataStore";
    private static final String TAG_BRIGHTNESS_CONFIGURATION = "brightness-configuration";
    private static final String TAG_BRIGHTNESS_CONFIGURATIONS = "brightness-configurations";
    private static final String TAG_BRIGHTNESS_VALUE = "brightness-value";
    private static final String TAG_COLOR_MODE = "color-mode";
    private static final String TAG_DISPLAY = "display";
    private static final String TAG_DISPLAY_MANAGER_STATE = "display-manager-state";
    private static final String TAG_DISPLAY_STATES = "display-states";
    private static final String TAG_REMEMBERED_WIFI_DISPLAYS = "remembered-wifi-displays";
    private static final String TAG_STABLE_DEVICE_VALUES = "stable-device-values";
    private static final String TAG_STABLE_DISPLAY_HEIGHT = "stable-display-height";
    private static final String TAG_STABLE_DISPLAY_WIDTH = "stable-display-width";
    private static final String TAG_WIFI_DISPLAY = "wifi-display";
    private boolean mDirty;
    private final HashMap<String, DisplayState> mDisplayStates;
    private final Object mFileAccessLock;
    private BrightnessConfigurations mGlobalBrightnessConfigurations;
    private final Handler mHandler;
    private Injector mInjector;
    private boolean mLoaded;
    private ArrayList<WifiDisplay> mRememberedWifiDisplays;
    private final StableDeviceValues mStableDeviceValues;

    public PersistentDataStore() {
        this(new Injector());
    }

    PersistentDataStore(Injector injector) {
        this(injector, new Handler(BackgroundThread.getHandler().getLooper()));
    }

    PersistentDataStore(Injector injector, Handler handler) {
        this.mRememberedWifiDisplays = new ArrayList<>();
        this.mDisplayStates = new HashMap<>();
        this.mStableDeviceValues = new StableDeviceValues();
        this.mGlobalBrightnessConfigurations = new BrightnessConfigurations();
        this.mFileAccessLock = new Object();
        this.mInjector = injector;
        this.mHandler = handler;
    }

    public void saveIfNeeded() {
        if (this.mDirty) {
            save();
            this.mDirty = false;
        }
    }

    public WifiDisplay getRememberedWifiDisplay(String deviceAddress) {
        loadIfNeeded();
        int index = findRememberedWifiDisplay(deviceAddress);
        if (index >= 0) {
            return this.mRememberedWifiDisplays.get(index);
        }
        return null;
    }

    public WifiDisplay[] getRememberedWifiDisplays() {
        loadIfNeeded();
        ArrayList<WifiDisplay> arrayList = this.mRememberedWifiDisplays;
        return (WifiDisplay[]) arrayList.toArray(new WifiDisplay[arrayList.size()]);
    }

    public WifiDisplay applyWifiDisplayAlias(WifiDisplay display) {
        if (display != null) {
            loadIfNeeded();
            String alias = null;
            int index = findRememberedWifiDisplay(display.getDeviceAddress());
            if (index >= 0) {
                alias = this.mRememberedWifiDisplays.get(index).getDeviceAlias();
            }
            if (!Objects.equals(display.getDeviceAlias(), alias)) {
                return new WifiDisplay(display.getDeviceAddress(), display.getDeviceName(), alias, display.isAvailable(), display.canConnect(), display.isRemembered());
            }
        }
        return display;
    }

    public WifiDisplay[] applyWifiDisplayAliases(WifiDisplay[] displays) {
        WifiDisplay[] results = displays;
        if (results != null) {
            int count = displays.length;
            for (int i = 0; i < count; i++) {
                WifiDisplay result = applyWifiDisplayAlias(displays[i]);
                if (result != displays[i]) {
                    if (results == displays) {
                        results = new WifiDisplay[count];
                        System.arraycopy(displays, 0, results, 0, count);
                    }
                    results[i] = result;
                }
            }
        }
        return results;
    }

    public boolean rememberWifiDisplay(WifiDisplay display) {
        loadIfNeeded();
        int index = findRememberedWifiDisplay(display.getDeviceAddress());
        if (index >= 0) {
            WifiDisplay other = this.mRememberedWifiDisplays.get(index);
            if (other.equals(display)) {
                return false;
            }
            this.mRememberedWifiDisplays.set(index, display);
        } else {
            this.mRememberedWifiDisplays.add(display);
        }
        setDirty();
        return true;
    }

    public boolean forgetWifiDisplay(String deviceAddress) {
        loadIfNeeded();
        int index = findRememberedWifiDisplay(deviceAddress);
        if (index >= 0) {
            this.mRememberedWifiDisplays.remove(index);
            setDirty();
            return true;
        }
        return false;
    }

    private int findRememberedWifiDisplay(String deviceAddress) {
        int count = this.mRememberedWifiDisplays.size();
        for (int i = 0; i < count; i++) {
            if (this.mRememberedWifiDisplays.get(i).getDeviceAddress().equals(deviceAddress)) {
                return i;
            }
        }
        return -1;
    }

    public int getColorMode(DisplayDevice device) {
        DisplayState state;
        if (device.hasStableUniqueId() && (state = getDisplayState(device.getUniqueId(), false)) != null) {
            return state.getColorMode();
        }
        return -1;
    }

    public boolean setColorMode(DisplayDevice device, int colorMode) {
        if (device.hasStableUniqueId()) {
            DisplayState state = getDisplayState(device.getUniqueId(), true);
            if (state.setColorMode(colorMode)) {
                setDirty();
                return true;
            }
            return false;
        }
        return false;
    }

    public float getBrightness(DisplayDevice device) {
        DisplayState state;
        if (device == null || !device.hasStableUniqueId() || (state = getDisplayState(device.getUniqueId(), false)) == null) {
            return Float.NaN;
        }
        return state.getBrightness();
    }

    public boolean setBrightness(DisplayDevice displayDevice, float brightness) {
        if (displayDevice == null) {
            return false;
        }
        String displayDeviceUniqueId = displayDevice.getUniqueId();
        if (!displayDevice.hasStableUniqueId() || displayDeviceUniqueId == null) {
            return false;
        }
        DisplayState state = getDisplayState(displayDeviceUniqueId, true);
        if (!state.setBrightness(brightness)) {
            return false;
        }
        setDirty();
        return true;
    }

    public boolean setUserPreferredRefreshRate(DisplayDevice displayDevice, float refreshRate) {
        String displayDeviceUniqueId = displayDevice.getUniqueId();
        if (!displayDevice.hasStableUniqueId() || displayDeviceUniqueId == null) {
            return false;
        }
        DisplayState state = getDisplayState(displayDevice.getUniqueId(), true);
        if (state.setRefreshRate(refreshRate)) {
            setDirty();
            return true;
        }
        return false;
    }

    public float getUserPreferredRefreshRate(DisplayDevice device) {
        DisplayState state;
        if (device == null || !device.hasStableUniqueId() || (state = getDisplayState(device.getUniqueId(), false)) == null) {
            return Float.NaN;
        }
        return state.getRefreshRate();
    }

    public boolean setUserPreferredResolution(DisplayDevice displayDevice, int width, int height) {
        String displayDeviceUniqueId = displayDevice.getUniqueId();
        if (!displayDevice.hasStableUniqueId() || displayDeviceUniqueId == null) {
            return false;
        }
        DisplayState state = getDisplayState(displayDevice.getUniqueId(), true);
        if (state.setResolution(width, height)) {
            setDirty();
            return true;
        }
        return false;
    }

    public Point getUserPreferredResolution(DisplayDevice displayDevice) {
        DisplayState state;
        if (displayDevice == null || !displayDevice.hasStableUniqueId() || (state = getDisplayState(displayDevice.getUniqueId(), false)) == null) {
            return null;
        }
        return state.getResolution();
    }

    public Point getStableDisplaySize() {
        loadIfNeeded();
        return this.mStableDeviceValues.getDisplaySize();
    }

    public void setStableDisplaySize(Point size) {
        loadIfNeeded();
        if (this.mStableDeviceValues.setDisplaySize(size)) {
            setDirty();
        }
    }

    public void setBrightnessConfigurationForUser(BrightnessConfiguration c, int userSerial, String packageName) {
        loadIfNeeded();
        if (this.mGlobalBrightnessConfigurations.setBrightnessConfigurationForUser(c, userSerial, packageName)) {
            setDirty();
        }
    }

    public boolean setBrightnessConfigurationForDisplayLocked(BrightnessConfiguration configuration, DisplayDevice device, int userSerial, String packageName) {
        if (device == null || !device.hasStableUniqueId()) {
            return false;
        }
        DisplayState state = getDisplayState(device.getUniqueId(), true);
        if (!state.setBrightnessConfiguration(configuration, userSerial, packageName)) {
            return false;
        }
        setDirty();
        return true;
    }

    public BrightnessConfiguration getBrightnessConfigurationForDisplayLocked(String uniqueDisplayId, int userSerial) {
        loadIfNeeded();
        DisplayState state = this.mDisplayStates.get(uniqueDisplayId);
        if (state != null) {
            return state.getBrightnessConfiguration(userSerial);
        }
        return null;
    }

    public BrightnessConfiguration getBrightnessConfiguration(int userSerial) {
        loadIfNeeded();
        return this.mGlobalBrightnessConfigurations.getBrightnessConfiguration(userSerial);
    }

    private DisplayState getDisplayState(String uniqueId, boolean createIfAbsent) {
        loadIfNeeded();
        DisplayState state = this.mDisplayStates.get(uniqueId);
        if (state == null && createIfAbsent) {
            DisplayState state2 = new DisplayState();
            this.mDisplayStates.put(uniqueId, state2);
            setDirty();
            return state2;
        }
        return state;
    }

    public void loadIfNeeded() {
        if (!this.mLoaded) {
            load();
            this.mLoaded = true;
        }
    }

    private void setDirty() {
        this.mDirty = true;
    }

    private void clearState() {
        this.mRememberedWifiDisplays.clear();
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [459=5] */
    private void load() {
        synchronized (this.mFileAccessLock) {
            clearState();
            try {
                InputStream is = this.mInjector.openRead();
                try {
                    TypedXmlPullParser parser = Xml.resolvePullParser(is);
                    loadFromXml(parser);
                    IoUtils.closeQuietly(is);
                } catch (IOException ex) {
                    Slog.w(TAG, "Failed to load display manager persistent store data.", ex);
                    clearState();
                    IoUtils.closeQuietly(is);
                } catch (XmlPullParserException ex2) {
                    Slog.w(TAG, "Failed to load display manager persistent store data.", ex2);
                    clearState();
                    IoUtils.closeQuietly(is);
                }
            } catch (FileNotFoundException e) {
            }
        }
    }

    private void save() {
        try {
            final ByteArrayOutputStream os = new ByteArrayOutputStream();
            TypedXmlSerializer serializer = Xml.resolveSerializer(os);
            saveToXml(serializer);
            serializer.flush();
            this.mHandler.removeCallbacksAndMessages(null);
            this.mHandler.post(new Runnable() { // from class: com.android.server.display.PersistentDataStore$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    PersistentDataStore.this.m3499lambda$save$0$comandroidserverdisplayPersistentDataStore(os);
                }
            });
        } catch (IOException ex) {
            Slog.w(TAG, "Failed to process the XML serializer.", ex);
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [484=4] */
    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$save$0$com-android-server-display-PersistentDataStore  reason: not valid java name */
    public /* synthetic */ void m3499lambda$save$0$comandroidserverdisplayPersistentDataStore(ByteArrayOutputStream os) {
        Injector injector;
        synchronized (this.mFileAccessLock) {
            OutputStream fileOutput = null;
            try {
                fileOutput = this.mInjector.startWrite();
                os.writeTo(fileOutput);
                fileOutput.flush();
            } catch (IOException ex) {
                Slog.w(TAG, "Failed to save display manager persistent store data.", ex);
                if (fileOutput != null) {
                    injector = this.mInjector;
                }
            }
            if (fileOutput != null) {
                injector = this.mInjector;
                injector.finishWrite(fileOutput, true);
            }
        }
    }

    private void loadFromXml(TypedXmlPullParser parser) throws IOException, XmlPullParserException {
        XmlUtils.beginDocument(parser, TAG_DISPLAY_MANAGER_STATE);
        int outerDepth = parser.getDepth();
        while (XmlUtils.nextElementWithin(parser, outerDepth)) {
            if (parser.getName().equals(TAG_REMEMBERED_WIFI_DISPLAYS)) {
                loadRememberedWifiDisplaysFromXml(parser);
            }
            if (parser.getName().equals(TAG_DISPLAY_STATES)) {
                loadDisplaysFromXml(parser);
            }
            if (parser.getName().equals(TAG_STABLE_DEVICE_VALUES)) {
                this.mStableDeviceValues.loadFromXml(parser);
            }
            if (parser.getName().equals(TAG_BRIGHTNESS_CONFIGURATIONS)) {
                this.mGlobalBrightnessConfigurations.loadFromXml(parser);
            }
        }
    }

    private void loadRememberedWifiDisplaysFromXml(TypedXmlPullParser parser) throws IOException, XmlPullParserException {
        int outerDepth = parser.getDepth();
        while (XmlUtils.nextElementWithin(parser, outerDepth)) {
            if (parser.getName().equals(TAG_WIFI_DISPLAY)) {
                String deviceAddress = parser.getAttributeValue((String) null, ATTR_DEVICE_ADDRESS);
                String deviceName = parser.getAttributeValue((String) null, ATTR_DEVICE_NAME);
                String deviceAlias = parser.getAttributeValue((String) null, ATTR_DEVICE_ALIAS);
                if (deviceAddress == null || deviceName == null) {
                    throw new XmlPullParserException("Missing deviceAddress or deviceName attribute on wifi-display.");
                }
                if (findRememberedWifiDisplay(deviceAddress) >= 0) {
                    throw new XmlPullParserException("Found duplicate wifi display device address.");
                }
                this.mRememberedWifiDisplays.add(new WifiDisplay(deviceAddress, deviceName, deviceAlias, false, false, false));
            }
        }
    }

    private void loadDisplaysFromXml(TypedXmlPullParser parser) throws IOException, XmlPullParserException {
        int outerDepth = parser.getDepth();
        while (XmlUtils.nextElementWithin(parser, outerDepth)) {
            if (parser.getName().equals(TAG_DISPLAY)) {
                String uniqueId = parser.getAttributeValue((String) null, ATTR_UNIQUE_ID);
                if (uniqueId == null) {
                    throw new XmlPullParserException("Missing unique-id attribute on display.");
                }
                if (this.mDisplayStates.containsKey(uniqueId)) {
                    throw new XmlPullParserException("Found duplicate display.");
                }
                DisplayState state = new DisplayState();
                state.loadFromXml(parser);
                this.mDisplayStates.put(uniqueId, state);
            }
        }
    }

    private void saveToXml(TypedXmlSerializer serializer) throws IOException {
        serializer.startDocument((String) null, true);
        serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
        serializer.startTag((String) null, TAG_DISPLAY_MANAGER_STATE);
        serializer.startTag((String) null, TAG_REMEMBERED_WIFI_DISPLAYS);
        Iterator<WifiDisplay> it = this.mRememberedWifiDisplays.iterator();
        while (it.hasNext()) {
            WifiDisplay display = it.next();
            serializer.startTag((String) null, TAG_WIFI_DISPLAY);
            serializer.attribute((String) null, ATTR_DEVICE_ADDRESS, display.getDeviceAddress());
            serializer.attribute((String) null, ATTR_DEVICE_NAME, display.getDeviceName());
            if (display.getDeviceAlias() != null) {
                serializer.attribute((String) null, ATTR_DEVICE_ALIAS, display.getDeviceAlias());
            }
            serializer.endTag((String) null, TAG_WIFI_DISPLAY);
        }
        serializer.endTag((String) null, TAG_REMEMBERED_WIFI_DISPLAYS);
        serializer.startTag((String) null, TAG_DISPLAY_STATES);
        for (Map.Entry<String, DisplayState> entry : this.mDisplayStates.entrySet()) {
            String uniqueId = entry.getKey();
            DisplayState state = entry.getValue();
            serializer.startTag((String) null, TAG_DISPLAY);
            serializer.attribute((String) null, ATTR_UNIQUE_ID, uniqueId);
            state.saveToXml(serializer);
            serializer.endTag((String) null, TAG_DISPLAY);
        }
        serializer.endTag((String) null, TAG_DISPLAY_STATES);
        serializer.startTag((String) null, TAG_STABLE_DEVICE_VALUES);
        this.mStableDeviceValues.saveToXml(serializer);
        serializer.endTag((String) null, TAG_STABLE_DEVICE_VALUES);
        serializer.startTag((String) null, TAG_BRIGHTNESS_CONFIGURATIONS);
        this.mGlobalBrightnessConfigurations.saveToXml(serializer);
        serializer.endTag((String) null, TAG_BRIGHTNESS_CONFIGURATIONS);
        serializer.endTag((String) null, TAG_DISPLAY_MANAGER_STATE);
        serializer.endDocument();
    }

    public void dump(PrintWriter pw) {
        pw.println("PersistentDataStore");
        pw.println("  mLoaded=" + this.mLoaded);
        pw.println("  mDirty=" + this.mDirty);
        pw.println("  RememberedWifiDisplays:");
        int i = 0;
        Iterator<WifiDisplay> it = this.mRememberedWifiDisplays.iterator();
        while (it.hasNext()) {
            WifiDisplay display = it.next();
            pw.println("    " + i + ": " + display);
            i++;
        }
        pw.println("  DisplayStates:");
        int i2 = 0;
        for (Map.Entry<String, DisplayState> entry : this.mDisplayStates.entrySet()) {
            pw.println("    " + i2 + ": " + entry.getKey());
            entry.getValue().dump(pw, "      ");
            i2++;
        }
        pw.println("  StableDeviceValues:");
        this.mStableDeviceValues.dump(pw, "      ");
        pw.println("  GlobalBrightnessConfigurations:");
        this.mGlobalBrightnessConfigurations.dump(pw, "      ");
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static final class DisplayState {
        private float mBrightness;
        private int mColorMode;
        private BrightnessConfigurations mDisplayBrightnessConfigurations;
        private int mHeight;
        private float mRefreshRate;
        private int mWidth;

        private DisplayState() {
            this.mDisplayBrightnessConfigurations = new BrightnessConfigurations();
        }

        public boolean setColorMode(int colorMode) {
            if (colorMode == this.mColorMode) {
                return false;
            }
            this.mColorMode = colorMode;
            return true;
        }

        public int getColorMode() {
            return this.mColorMode;
        }

        public boolean setBrightness(float brightness) {
            if (brightness == this.mBrightness) {
                return false;
            }
            this.mBrightness = brightness;
            return true;
        }

        public float getBrightness() {
            return this.mBrightness;
        }

        public boolean setBrightnessConfiguration(BrightnessConfiguration configuration, int userSerial, String packageName) {
            this.mDisplayBrightnessConfigurations.setBrightnessConfigurationForUser(configuration, userSerial, packageName);
            return true;
        }

        public BrightnessConfiguration getBrightnessConfiguration(int userSerial) {
            return (BrightnessConfiguration) this.mDisplayBrightnessConfigurations.mConfigurations.get(userSerial);
        }

        public boolean setResolution(int width, int height) {
            if (width == this.mWidth && height == this.mHeight) {
                return false;
            }
            this.mWidth = width;
            this.mHeight = height;
            return true;
        }

        public Point getResolution() {
            return new Point(this.mWidth, this.mHeight);
        }

        public boolean setRefreshRate(float refreshRate) {
            if (refreshRate == this.mRefreshRate) {
                return false;
            }
            this.mRefreshRate = refreshRate;
            return true;
        }

        public float getRefreshRate() {
            return this.mRefreshRate;
        }

        public void loadFromXml(TypedXmlPullParser parser) throws IOException, XmlPullParserException {
            int outerDepth = parser.getDepth();
            while (XmlUtils.nextElementWithin(parser, outerDepth)) {
                String name = parser.getName();
                char c = 65535;
                switch (name.hashCode()) {
                    case -1321967815:
                        if (name.equals(PersistentDataStore.TAG_BRIGHTNESS_CONFIGURATIONS)) {
                            c = 2;
                            break;
                        }
                        break;
                    case -945778443:
                        if (name.equals(PersistentDataStore.TAG_BRIGHTNESS_VALUE)) {
                            c = 1;
                            break;
                        }
                        break;
                    case 1243304397:
                        if (name.equals(PersistentDataStore.TAG_COLOR_MODE)) {
                            c = 0;
                            break;
                        }
                        break;
                }
                switch (c) {
                    case 0:
                        String value = parser.nextText();
                        this.mColorMode = Integer.parseInt(value);
                        break;
                    case 1:
                        String brightness = parser.nextText();
                        this.mBrightness = Float.parseFloat(brightness);
                        break;
                    case 2:
                        this.mDisplayBrightnessConfigurations.loadFromXml(parser);
                        break;
                }
            }
        }

        public void saveToXml(TypedXmlSerializer serializer) throws IOException {
            serializer.startTag((String) null, PersistentDataStore.TAG_COLOR_MODE);
            serializer.text(Integer.toString(this.mColorMode));
            serializer.endTag((String) null, PersistentDataStore.TAG_COLOR_MODE);
            serializer.startTag((String) null, PersistentDataStore.TAG_BRIGHTNESS_VALUE);
            serializer.text(Float.toString(this.mBrightness));
            serializer.endTag((String) null, PersistentDataStore.TAG_BRIGHTNESS_VALUE);
            serializer.startTag((String) null, PersistentDataStore.TAG_BRIGHTNESS_CONFIGURATIONS);
            this.mDisplayBrightnessConfigurations.saveToXml(serializer);
            serializer.endTag((String) null, PersistentDataStore.TAG_BRIGHTNESS_CONFIGURATIONS);
        }

        public void dump(PrintWriter pw, String prefix) {
            pw.println(prefix + "ColorMode=" + this.mColorMode);
            pw.println(prefix + "BrightnessValue=" + this.mBrightness);
            pw.println(prefix + "DisplayBrightnessConfigurations: ");
            this.mDisplayBrightnessConfigurations.dump(pw, prefix);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static final class StableDeviceValues {
        private int mHeight;
        private int mWidth;

        private StableDeviceValues() {
        }

        /* JADX INFO: Access modifiers changed from: private */
        public Point getDisplaySize() {
            return new Point(this.mWidth, this.mHeight);
        }

        public boolean setDisplaySize(Point r) {
            if (this.mWidth != r.x || this.mHeight != r.y) {
                this.mWidth = r.x;
                this.mHeight = r.y;
                return true;
            }
            return false;
        }

        public void loadFromXml(TypedXmlPullParser parser) throws IOException, XmlPullParserException {
            int outerDepth = parser.getDepth();
            while (XmlUtils.nextElementWithin(parser, outerDepth)) {
                String name = parser.getName();
                char c = 65535;
                switch (name.hashCode()) {
                    case -1635792540:
                        if (name.equals(PersistentDataStore.TAG_STABLE_DISPLAY_HEIGHT)) {
                            c = 1;
                            break;
                        }
                        break;
                    case 1069578729:
                        if (name.equals(PersistentDataStore.TAG_STABLE_DISPLAY_WIDTH)) {
                            c = 0;
                            break;
                        }
                        break;
                }
                switch (c) {
                    case 0:
                        this.mWidth = loadIntValue(parser);
                        break;
                    case 1:
                        this.mHeight = loadIntValue(parser);
                        break;
                }
            }
        }

        private static int loadIntValue(TypedXmlPullParser parser) throws IOException, XmlPullParserException {
            try {
                String value = parser.nextText();
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                return 0;
            }
        }

        public void saveToXml(TypedXmlSerializer serializer) throws IOException {
            if (this.mWidth > 0 && this.mHeight > 0) {
                serializer.startTag((String) null, PersistentDataStore.TAG_STABLE_DISPLAY_WIDTH);
                serializer.text(Integer.toString(this.mWidth));
                serializer.endTag((String) null, PersistentDataStore.TAG_STABLE_DISPLAY_WIDTH);
                serializer.startTag((String) null, PersistentDataStore.TAG_STABLE_DISPLAY_HEIGHT);
                serializer.text(Integer.toString(this.mHeight));
                serializer.endTag((String) null, PersistentDataStore.TAG_STABLE_DISPLAY_HEIGHT);
            }
        }

        public void dump(PrintWriter pw, String prefix) {
            pw.println(prefix + "StableDisplayWidth=" + this.mWidth);
            pw.println(prefix + "StableDisplayHeight=" + this.mHeight);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static final class BrightnessConfigurations {
        private final SparseArray<BrightnessConfiguration> mConfigurations = new SparseArray<>();
        private final SparseLongArray mTimeStamps = new SparseLongArray();
        private final SparseArray<String> mPackageNames = new SparseArray<>();

        /* JADX INFO: Access modifiers changed from: private */
        public boolean setBrightnessConfigurationForUser(BrightnessConfiguration c, int userSerial, String packageName) {
            BrightnessConfiguration currentConfig = this.mConfigurations.get(userSerial);
            if (currentConfig != c) {
                if (currentConfig == null || !currentConfig.equals(c)) {
                    if (c != null) {
                        if (packageName == null) {
                            this.mPackageNames.remove(userSerial);
                        } else {
                            this.mPackageNames.put(userSerial, packageName);
                        }
                        this.mTimeStamps.put(userSerial, System.currentTimeMillis());
                        this.mConfigurations.put(userSerial, c);
                        return true;
                    }
                    this.mPackageNames.remove(userSerial);
                    this.mTimeStamps.delete(userSerial);
                    this.mConfigurations.remove(userSerial);
                    return true;
                }
                return false;
            }
            return false;
        }

        public BrightnessConfiguration getBrightnessConfiguration(int userSerial) {
            return this.mConfigurations.get(userSerial);
        }

        public void loadFromXml(TypedXmlPullParser parser) throws IOException, XmlPullParserException {
            int userSerial;
            int outerDepth = parser.getDepth();
            while (XmlUtils.nextElementWithin(parser, outerDepth)) {
                if (PersistentDataStore.TAG_BRIGHTNESS_CONFIGURATION.equals(parser.getName())) {
                    try {
                        userSerial = parser.getAttributeInt((String) null, PersistentDataStore.ATTR_USER_SERIAL);
                    } catch (NumberFormatException nfe) {
                        Slog.e(PersistentDataStore.TAG, "Failed to read in brightness configuration", nfe);
                        userSerial = -1;
                    }
                    String packageName = parser.getAttributeValue((String) null, PersistentDataStore.ATTR_PACKAGE_NAME);
                    long timeStamp = parser.getAttributeLong((String) null, "timestamp", -1L);
                    try {
                        BrightnessConfiguration config = BrightnessConfiguration.loadFromXml(parser);
                        if (userSerial >= 0 && config != null) {
                            this.mConfigurations.put(userSerial, config);
                            if (timeStamp != -1) {
                                this.mTimeStamps.put(userSerial, timeStamp);
                            }
                            if (packageName != null) {
                                this.mPackageNames.put(userSerial, packageName);
                            }
                        }
                    } catch (IllegalArgumentException iae) {
                        Slog.e(PersistentDataStore.TAG, "Failed to load brightness configuration!", iae);
                    }
                }
            }
        }

        public void saveToXml(TypedXmlSerializer serializer) throws IOException {
            for (int i = 0; i < this.mConfigurations.size(); i++) {
                int userSerial = this.mConfigurations.keyAt(i);
                BrightnessConfiguration config = this.mConfigurations.valueAt(i);
                serializer.startTag((String) null, PersistentDataStore.TAG_BRIGHTNESS_CONFIGURATION);
                serializer.attributeInt((String) null, PersistentDataStore.ATTR_USER_SERIAL, userSerial);
                String packageName = this.mPackageNames.get(userSerial);
                if (packageName != null) {
                    serializer.attribute((String) null, PersistentDataStore.ATTR_PACKAGE_NAME, packageName);
                }
                long timestamp = this.mTimeStamps.get(userSerial, -1L);
                if (timestamp != -1) {
                    serializer.attributeLong((String) null, "timestamp", timestamp);
                }
                config.saveToXml(serializer);
                serializer.endTag((String) null, PersistentDataStore.TAG_BRIGHTNESS_CONFIGURATION);
            }
        }

        public void dump(PrintWriter pw, String prefix) {
            for (int i = 0; i < this.mConfigurations.size(); i++) {
                int userSerial = this.mConfigurations.keyAt(i);
                long time = this.mTimeStamps.get(userSerial, -1L);
                String packageName = this.mPackageNames.get(userSerial);
                pw.println(prefix + "User " + userSerial + ":");
                if (time != -1) {
                    pw.println(prefix + "  set at: " + TimeUtils.formatForLogging(time));
                }
                if (packageName != null) {
                    pw.println(prefix + "  set by: " + packageName);
                }
                pw.println(prefix + "  " + this.mConfigurations.valueAt(i));
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class Injector {
        private final AtomicFile mAtomicFile = new AtomicFile(new File("/data/system/display-manager-state.xml"), "display-state");

        public InputStream openRead() throws FileNotFoundException {
            return this.mAtomicFile.openRead();
        }

        public OutputStream startWrite() throws IOException {
            return this.mAtomicFile.startWrite();
        }

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
