package com.android.server.display;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.hardware.display.DisplayManagerInternal;
import android.os.Environment;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.MathUtils;
import android.util.Slog;
import android.util.Spline;
import android.view.DisplayAddress;
import com.android.internal.display.BrightnessSynchronizer;
import com.android.server.display.DensityMapping;
import com.android.server.display.config.BrightnessThresholds;
import com.android.server.display.config.BrightnessThrottlingMap;
import com.android.server.display.config.BrightnessThrottlingPoint;
import com.android.server.display.config.Density;
import com.android.server.display.config.DisplayConfiguration;
import com.android.server.display.config.DisplayQuirks;
import com.android.server.display.config.HbmTiming;
import com.android.server.display.config.HighBrightnessMode;
import com.android.server.display.config.NitsMap;
import com.android.server.display.config.Point;
import com.android.server.display.config.RefreshRateRange;
import com.android.server.display.config.SdrHdrRatioMap;
import com.android.server.display.config.SdrHdrRatioPoint;
import com.android.server.display.config.SensorDetails;
import com.android.server.display.config.ThermalStatus;
import com.android.server.display.config.ThermalThrottling;
import com.android.server.display.config.Thresholds;
import com.android.server.display.config.XmlParser;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import javax.xml.datatype.DatatypeConfigurationException;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes.dex */
public class DisplayDeviceConfig {
    private static final int AMBIENT_LIGHT_LONG_HORIZON_MILLIS = 10000;
    private static final int AMBIENT_LIGHT_SHORT_HORIZON_MILLIS = 2000;
    private static final float BRIGHTNESS_DEFAULT = 0.5f;
    private static final String CONFIG_FILE_FORMAT = "display_%s.xml";
    private static final boolean DEBUG = false;
    private static final String DEFAULT_CONFIG_FILE = "default.xml";
    private static final String DEFAULT_CONFIG_FILE_WITH_UIMODE_FORMAT = "default_%s.xml";
    private static final String DISPLAY_CONFIG_DIR = "displayconfig";
    private static final String ETC_DIR = "etc";
    static final float HDR_PERCENT_OF_SCREEN_REQUIRED_DEFAULT = 0.5f;
    public static final float HIGH_BRIGHTNESS_MODE_UNSUPPORTED = Float.NaN;
    private static final int INTERPOLATION_DEFAULT = 0;
    private static final int INTERPOLATION_LINEAR = 1;
    private static final float INVALID_BRIGHTNESS_IN_CONFIG = -2.0f;
    private static final float NITS_INVALID = -1.0f;
    private static final String NO_SUFFIX_FORMAT = "%d";
    private static final String PORT_SUFFIX_FORMAT = "port_%d";
    public static final String QUIRK_CAN_SET_BRIGHTNESS_VIA_HWC = "canSetBrightnessViaHwc";
    private static final long STABLE_FLAG = 4611686018427387904L;
    private static final String STABLE_ID_SUFFIX_FORMAT = "id_%d";
    private static final String TAG = "DisplayDeviceConfig";
    private float[] mBacklight;
    private Spline mBacklightToBrightnessSpline;
    private Spline mBacklightToNitsSpline;
    private float[] mBrightness;
    private BrightnessThrottlingData mBrightnessThrottlingData;
    private Spline mBrightnessToBacklightSpline;
    private final Context mContext;
    private float[] mCusBrightness;
    private DensityMapping mDensityMapping;
    private HighBrightnessModeData mHbmData;
    private int mInterpolationType;
    private float[] mNits;
    private Spline mNitsToBacklightSpline;
    private List<String> mQuirks;
    private float[] mRawBacklight;
    private float[] mRawNits;
    private Spline mSdrToHdrRatioSpline;
    private final SensorData mAmbientLightSensor = new SensorData();
    private final SensorData mProximitySensor = new SensorData();
    private final List<DisplayManagerInternal.RefreshRateLimitation> mRefreshRateLimitations = new ArrayList(2);
    private float mBacklightMinimum = Float.NaN;
    private float mBacklightMaximum = Float.NaN;
    private float mBrightnessDefault = Float.NaN;
    private float mBrightnessRampFastDecrease = Float.NaN;
    private float mBrightnessRampFastIncrease = Float.NaN;
    private float mBrightnessRampSlowDecrease = Float.NaN;
    private float mBrightnessRampSlowIncrease = Float.NaN;
    private long mBrightnessRampDecreaseMaxMillis = 0;
    private long mBrightnessRampIncreaseMaxMillis = 0;
    private int mAmbientHorizonLong = 10000;
    private int mAmbientHorizonShort = 2000;
    private float mScreenBrighteningMinThreshold = 0.0f;
    private float mScreenDarkeningMinThreshold = 0.0f;
    private float mAmbientLuxBrighteningMinThreshold = 0.0f;
    private float mAmbientLuxDarkeningMinThreshold = 0.0f;
    private boolean mIsHighBrightnessModeEnabled = false;
    private String mLoadedFrom = null;

    private DisplayDeviceConfig(Context context) {
        this.mContext = context;
    }

    public static DisplayDeviceConfig create(Context context, long physicalDisplayId, boolean isFirstDisplay) {
        DisplayDeviceConfig config = createWithoutDefaultValues(context, physicalDisplayId, isFirstDisplay);
        config.copyUninitializedValuesFromSecondaryConfig(loadDefaultConfigurationXml(context));
        return config;
    }

    public static DisplayDeviceConfig create(Context context, boolean useConfigXml) {
        if (useConfigXml) {
            DisplayDeviceConfig config = getConfigFromGlobalXml(context);
            return config;
        }
        DisplayDeviceConfig config2 = getConfigFromPmValues(context);
        return config2;
    }

    private static DisplayDeviceConfig createWithoutDefaultValues(Context context, long physicalDisplayId, boolean isFirstDisplay) {
        DisplayDeviceConfig config = loadConfigFromDirectory(context, Environment.getProductDirectory(), physicalDisplayId);
        if (config != null) {
            return config;
        }
        DisplayDeviceConfig config2 = loadConfigFromDirectory(context, Environment.getVendorDirectory(), physicalDisplayId);
        if (config2 != null) {
            return config2;
        }
        return create(context, isFirstDisplay);
    }

    private static DisplayConfiguration loadDefaultConfigurationXml(Context context) {
        List<File> defaultXmlLocations = new ArrayList<>();
        defaultXmlLocations.add(Environment.buildPath(Environment.getProductDirectory(), new String[]{ETC_DIR, DISPLAY_CONFIG_DIR, DEFAULT_CONFIG_FILE}));
        defaultXmlLocations.add(Environment.buildPath(Environment.getVendorDirectory(), new String[]{ETC_DIR, DISPLAY_CONFIG_DIR, DEFAULT_CONFIG_FILE}));
        int uiModeType = context.getResources().getInteger(17694801);
        String uiModeTypeStr = Configuration.getUiModeTypeString(uiModeType);
        if (uiModeTypeStr != null) {
            defaultXmlLocations.add(Environment.buildPath(Environment.getRootDirectory(), new String[]{ETC_DIR, DISPLAY_CONFIG_DIR, String.format(DEFAULT_CONFIG_FILE_WITH_UIMODE_FORMAT, uiModeTypeStr)}));
        }
        defaultXmlLocations.add(Environment.buildPath(Environment.getRootDirectory(), new String[]{ETC_DIR, DISPLAY_CONFIG_DIR, DEFAULT_CONFIG_FILE}));
        File configFile = getFirstExistingFile(defaultXmlLocations);
        if (configFile == null) {
            return null;
        }
        DisplayConfiguration defaultConfig = null;
        try {
            InputStream in = new BufferedInputStream(new FileInputStream(configFile));
            defaultConfig = XmlParser.read(in);
            if (defaultConfig == null) {
                Slog.i(TAG, "Default DisplayDeviceConfig file is null");
            }
            in.close();
        } catch (IOException | DatatypeConfigurationException | XmlPullParserException e) {
            Slog.e(TAG, "Encountered an error while reading/parsing display config file: " + configFile, e);
        }
        return defaultConfig;
    }

    private static File getFirstExistingFile(Collection<File> files) {
        for (File file : files) {
            if (file.exists() && file.isFile()) {
                return file;
            }
        }
        return null;
    }

    private static DisplayDeviceConfig loadConfigFromDirectory(Context context, File baseDirectory, long physicalDisplayId) {
        DisplayDeviceConfig config = getConfigFromSuffix(context, baseDirectory, STABLE_ID_SUFFIX_FORMAT, physicalDisplayId);
        if (config != null) {
            return config;
        }
        long withoutStableFlag = (-4611686018427387905L) & physicalDisplayId;
        DisplayDeviceConfig config2 = getConfigFromSuffix(context, baseDirectory, NO_SUFFIX_FORMAT, withoutStableFlag);
        if (config2 != null) {
            return config2;
        }
        DisplayAddress.Physical physicalAddress = DisplayAddress.fromPhysicalDisplayId(physicalDisplayId);
        int port = physicalAddress.getPort();
        return getConfigFromSuffix(context, baseDirectory, PORT_SUFFIX_FORMAT, port);
    }

    public float[] getNits() {
        return this.mNits;
    }

    public float[] getBacklight() {
        return this.mBacklight;
    }

    public float getBacklightFromBrightness(float brightness) {
        return this.mBrightnessToBacklightSpline.interpolate(brightness);
    }

    public float getNitsFromBacklight(float backlight) {
        if (this.mBacklightToNitsSpline == null) {
            return -1.0f;
        }
        return this.mBacklightToNitsSpline.interpolate(Math.max(backlight, this.mBacklightMinimum));
    }

    public float getHdrBrightnessFromSdr(float brightness) {
        if (this.mSdrToHdrRatioSpline == null) {
            return -1.0f;
        }
        float backlight = getBacklightFromBrightness(brightness);
        float nits = getNitsFromBacklight(backlight);
        if (nits == -1.0f) {
            return -1.0f;
        }
        float ratio = this.mSdrToHdrRatioSpline.interpolate(nits);
        float hdrNits = nits * ratio;
        Spline spline = this.mNitsToBacklightSpline;
        if (spline == null) {
            return -1.0f;
        }
        float hdrBacklight = spline.interpolate(hdrNits);
        float hdrBrightness = this.mBacklightToBrightnessSpline.interpolate(Math.max(this.mBacklightMinimum, Math.min(this.mBacklightMaximum, hdrBacklight)));
        return hdrBrightness;
    }

    public float[] getBrightness() {
        return this.mBrightness;
    }

    public float getBrightnessDefault() {
        return this.mBrightnessDefault;
    }

    public float getBrightnessRampFastDecrease() {
        return this.mBrightnessRampFastDecrease;
    }

    public float getBrightnessRampFastIncrease() {
        return this.mBrightnessRampFastIncrease;
    }

    public float getBrightnessRampSlowDecrease() {
        return this.mBrightnessRampSlowDecrease;
    }

    public float getBrightnessRampSlowIncrease() {
        return this.mBrightnessRampSlowIncrease;
    }

    public long getBrightnessRampDecreaseMaxMillis() {
        return this.mBrightnessRampDecreaseMaxMillis;
    }

    public long getBrightnessRampIncreaseMaxMillis() {
        return this.mBrightnessRampIncreaseMaxMillis;
    }

    public int getAmbientHorizonLong() {
        return this.mAmbientHorizonLong;
    }

    public int getAmbientHorizonShort() {
        return this.mAmbientHorizonShort;
    }

    public float getScreenBrighteningMinThreshold() {
        return this.mScreenBrighteningMinThreshold;
    }

    public float getScreenDarkeningMinThreshold() {
        return this.mScreenDarkeningMinThreshold;
    }

    public float getAmbientLuxBrighteningMinThreshold() {
        return this.mAmbientLuxBrighteningMinThreshold;
    }

    public float getAmbientLuxDarkeningMinThreshold() {
        return this.mAmbientLuxDarkeningMinThreshold;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public SensorData getAmbientLightSensor() {
        return this.mAmbientLightSensor;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public SensorData getProximitySensor() {
        return this.mProximitySensor;
    }

    public boolean hasQuirk(String quirkValue) {
        List<String> list = this.mQuirks;
        return list != null && list.contains(quirkValue);
    }

    public HighBrightnessModeData getHighBrightnessModeData() {
        if (!this.mIsHighBrightnessModeEnabled || this.mHbmData == null) {
            return null;
        }
        HighBrightnessModeData hbmData = new HighBrightnessModeData();
        this.mHbmData.copyTo(hbmData);
        return hbmData;
    }

    public List<DisplayManagerInternal.RefreshRateLimitation> getRefreshRateLimitations() {
        return this.mRefreshRateLimitations;
    }

    public DensityMapping getDensityMapping() {
        return this.mDensityMapping;
    }

    public BrightnessThrottlingData getBrightnessThrottlingData() {
        return BrightnessThrottlingData.create(this.mBrightnessThrottlingData);
    }

    public String toString() {
        return "DisplayDeviceConfig{mLoadedFrom=" + this.mLoadedFrom + ", mBacklight=" + Arrays.toString(this.mBacklight) + ", mNits=" + Arrays.toString(this.mNits) + ", mRawBacklight=" + Arrays.toString(this.mRawBacklight) + ", mRawNits=" + Arrays.toString(this.mRawNits) + ", mInterpolationType=" + this.mInterpolationType + ", mBrightness=" + Arrays.toString(this.mBrightness) + ", mBrightnessToBacklightSpline=" + this.mBrightnessToBacklightSpline + ", mBacklightToBrightnessSpline=" + this.mBacklightToBrightnessSpline + ", mNitsToBacklightSpline=" + this.mNitsToBacklightSpline + ", mBacklightMinimum=" + this.mBacklightMinimum + ", mBacklightMaximum=" + this.mBacklightMaximum + ", mBrightnessDefault=" + this.mBrightnessDefault + ", mQuirks=" + this.mQuirks + ", isHbmEnabled=" + this.mIsHighBrightnessModeEnabled + ", mHbmData=" + this.mHbmData + ", mSdrToHdrRatioSpline=" + this.mSdrToHdrRatioSpline + ", mBrightnessThrottlingData=" + this.mBrightnessThrottlingData + ", mBrightnessRampFastDecrease=" + this.mBrightnessRampFastDecrease + ", mBrightnessRampFastIncrease=" + this.mBrightnessRampFastIncrease + ", mBrightnessRampSlowDecrease=" + this.mBrightnessRampSlowDecrease + ", mBrightnessRampSlowIncrease=" + this.mBrightnessRampSlowIncrease + ", mBrightnessRampDecreaseMaxMillis=" + this.mBrightnessRampDecreaseMaxMillis + ", mBrightnessRampIncreaseMaxMillis=" + this.mBrightnessRampIncreaseMaxMillis + ", mAmbientHorizonLong=" + this.mAmbientHorizonLong + ", mAmbientHorizonShort=" + this.mAmbientHorizonShort + ", mScreenDarkeningMinThreshold=" + this.mScreenDarkeningMinThreshold + ", mScreenBrighteningMinThreshold=" + this.mScreenBrighteningMinThreshold + ", mAmbientLuxDarkeningMinThreshold=" + this.mAmbientLuxDarkeningMinThreshold + ", mAmbientLuxBrighteningMinThreshold=" + this.mAmbientLuxBrighteningMinThreshold + ", mAmbientLightSensor=" + this.mAmbientLightSensor + ", mProximitySensor=" + this.mProximitySensor + ", mRefreshRateLimitations= " + Arrays.toString(this.mRefreshRateLimitations.toArray()) + ", mDensityMapping= " + this.mDensityMapping + "}";
    }

    private static DisplayDeviceConfig getConfigFromSuffix(Context context, File baseDirectory, String suffixFormat, long idNumber) {
        String suffix = String.format(suffixFormat, Long.valueOf(idNumber));
        String filename = String.format(CONFIG_FILE_FORMAT, suffix);
        File filePath = Environment.buildPath(baseDirectory, new String[]{ETC_DIR, DISPLAY_CONFIG_DIR, filename});
        DisplayDeviceConfig config = new DisplayDeviceConfig(context);
        if (config.initFromFile(filePath)) {
            return config;
        }
        return null;
    }

    private static DisplayDeviceConfig getConfigFromGlobalXml(Context context) {
        DisplayDeviceConfig config = new DisplayDeviceConfig(context);
        config.initFromGlobalXml();
        return config;
    }

    private static DisplayDeviceConfig getConfigFromPmValues(Context context) {
        DisplayDeviceConfig config = new DisplayDeviceConfig(context);
        config.initFromDefaultValues();
        return config;
    }

    private boolean initFromFile(File configFile) {
        if (configFile.exists()) {
            if (!configFile.isFile()) {
                Slog.e(TAG, "Display configuration is not a file: " + configFile + ", skipping");
                return false;
            }
            try {
                InputStream in = new BufferedInputStream(new FileInputStream(configFile));
                DisplayConfiguration config = XmlParser.read(in);
                if (config != null) {
                    loadDensityMapping(config);
                    loadBrightnessDefaultFromDdcXml(config);
                    loadBrightnessConstraintsFromConfigXml();
                    loadBrightnessMap(config);
                    loadBrightnessThrottlingMap(config);
                    loadHighBrightnessModeData(config);
                    loadQuirks(config);
                    loadBrightnessRamps(config);
                    loadAmbientLightSensorFromDdc(config);
                    loadProxSensorFromDdc(config);
                    loadAmbientHorizonFromDdc(config);
                    loadBrightnessChangeThresholds(config);
                } else {
                    Slog.w(TAG, "DisplayDeviceConfig file is null");
                }
                in.close();
            } catch (IOException | DatatypeConfigurationException | XmlPullParserException e) {
                Slog.e(TAG, "Encountered an error while reading/parsing display config file: " + configFile, e);
            }
            this.mLoadedFrom = configFile.toString();
            return true;
        }
        return false;
    }

    private void initFromGlobalXml() {
        loadBrightnessDefaultFromConfigXml();
        loadBrightnessConstraintsFromConfigXml();
        loadBrightnessMapFromConfigXml();
        loadBrightnessRampsFromConfigXml();
        loadAmbientLightSensorFromConfigXml();
        setProxSensorUnspecified();
        this.mLoadedFrom = "<config.xml>";
    }

    private void initFromDefaultValues() {
        this.mLoadedFrom = "Static values";
        this.mBacklightMinimum = 0.0f;
        this.mBacklightMaximum = 1.0f;
        this.mBrightnessDefault = 0.5f;
        this.mBrightnessRampFastDecrease = 1.0f;
        this.mBrightnessRampFastIncrease = 1.0f;
        this.mBrightnessRampSlowDecrease = 1.0f;
        this.mBrightnessRampSlowIncrease = 1.0f;
        this.mBrightnessRampDecreaseMaxMillis = 0L;
        this.mBrightnessRampIncreaseMaxMillis = 0L;
        setSimpleMappingStrategyValues();
        loadAmbientLightSensorFromConfigXml();
        setProxSensorUnspecified();
    }

    private void copyUninitializedValuesFromSecondaryConfig(DisplayConfiguration defaultConfig) {
        if (defaultConfig != null && this.mDensityMapping == null) {
            loadDensityMapping(defaultConfig);
        }
    }

    private void loadDensityMapping(DisplayConfiguration config) {
        if (config.getDensityMapping() == null) {
            return;
        }
        List<Density> entriesFromXml = config.getDensityMapping().getDensity();
        DensityMapping.Entry[] entries = new DensityMapping.Entry[entriesFromXml.size()];
        for (int i = 0; i < entriesFromXml.size(); i++) {
            Density density = entriesFromXml.get(i);
            entries[i] = new DensityMapping.Entry(density.getWidth().intValue(), density.getHeight().intValue(), density.getDensity().intValue());
        }
        this.mDensityMapping = DensityMapping.createByOwning(entries);
    }

    private void loadBrightnessDefaultFromDdcXml(DisplayConfiguration config) {
        if (config != null) {
            BigDecimal configBrightnessDefault = config.getScreenBrightnessDefault();
            if (configBrightnessDefault != null) {
                this.mBrightnessDefault = configBrightnessDefault.floatValue();
            } else {
                loadBrightnessDefaultFromConfigXml();
            }
        }
    }

    private void loadBrightnessDefaultFromConfigXml() {
        float def = this.mContext.getResources().getFloat(17105104);
        if (def == INVALID_BRIGHTNESS_IN_CONFIG) {
            this.mBrightnessDefault = BrightnessSynchronizer.brightnessIntToFloat(this.mContext.getResources().getInteger(17694935));
        } else {
            this.mBrightnessDefault = def;
        }
    }

    private void loadBrightnessConstraintsFromConfigXml() {
        float min = this.mContext.getResources().getFloat(17105109);
        float max = this.mContext.getResources().getFloat(17105108);
        if (min == INVALID_BRIGHTNESS_IN_CONFIG || max == INVALID_BRIGHTNESS_IN_CONFIG) {
            this.mBacklightMinimum = BrightnessSynchronizer.brightnessIntToFloat(this.mContext.getResources().getInteger(17694937));
            this.mBacklightMaximum = BrightnessSynchronizer.brightnessIntToFloat(this.mContext.getResources().getInteger(17694936));
            return;
        }
        this.mBacklightMinimum = min;
        this.mBacklightMaximum = max;
    }

    private void loadBrightnessMap(DisplayConfiguration config) {
        NitsMap map = config.getScreenBrightnessMap();
        if (map == null) {
            loadBrightnessMapFromConfigXml();
            return;
        }
        List<Point> points = map.getPoint();
        int size = points.size();
        float[] nits = new float[size];
        float[] backlight = new float[size];
        this.mInterpolationType = convertInterpolationType(map.getInterpolation());
        int i = 0;
        for (Point point : points) {
            nits[i] = point.getNits().floatValue();
            backlight[i] = point.getValue().floatValue();
            if (i > 0) {
                if (nits[i] < nits[i - 1]) {
                    Slog.e(TAG, "screenBrightnessMap must be non-decreasing, ignoring rest  of configuration. Nits: " + nits[i] + " < " + nits[i - 1]);
                    return;
                } else if (backlight[i] < backlight[i - 1]) {
                    Slog.e(TAG, "screenBrightnessMap must be non-decreasing, ignoring rest  of configuration. Value: " + backlight[i] + " < " + backlight[i - 1]);
                    return;
                }
            }
            i++;
        }
        this.mRawNits = nits;
        this.mRawBacklight = backlight;
        constrainNitsAndBacklightArrays();
    }

    private Spline loadSdrHdrRatioMap(HighBrightnessMode hbmConfig) {
        List<SdrHdrRatioPoint> points;
        int size;
        SdrHdrRatioMap sdrHdrRatioMap = hbmConfig.getSdrHdrRatioMap_all();
        if (sdrHdrRatioMap == null || (size = (points = sdrHdrRatioMap.getPoint()).size()) <= 0) {
            return null;
        }
        float[] nits = new float[size];
        float[] ratios = new float[size];
        int i = 0;
        for (SdrHdrRatioPoint point : points) {
            nits[i] = point.getSdrNits().floatValue();
            if (i > 0 && nits[i] < nits[i - 1]) {
                Slog.e(TAG, "sdrHdrRatioMap must be non-decreasing, ignoring rest  of configuration. nits: " + nits[i] + " < " + nits[i - 1]);
                return null;
            }
            ratios[i] = point.getHdrRatio().floatValue();
            i++;
        }
        return Spline.createSpline(nits, ratios);
    }

    private void loadBrightnessThrottlingMap(DisplayConfiguration config) {
        ThermalThrottling throttlingConfig = config.getThermalThrottling();
        if (throttlingConfig == null) {
            Slog.i(TAG, "no thermal throttling config found");
            return;
        }
        BrightnessThrottlingMap map = throttlingConfig.getBrightnessThrottlingMap();
        if (map == null) {
            Slog.i(TAG, "no brightness throttling map found");
            return;
        }
        List<BrightnessThrottlingPoint> points = map.getBrightnessThrottlingPoint();
        List<BrightnessThrottlingData.ThrottlingLevel> throttlingLevels = new ArrayList<>(points.size());
        boolean badConfig = false;
        Iterator<BrightnessThrottlingPoint> it = points.iterator();
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            BrightnessThrottlingPoint point = it.next();
            ThermalStatus status = point.getThermalStatus();
            if (!thermalStatusIsValid(status)) {
                badConfig = true;
                break;
            }
            throttlingLevels.add(new BrightnessThrottlingData.ThrottlingLevel(convertThermalStatus(status), point.getBrightness().floatValue()));
        }
        if (!badConfig) {
            this.mBrightnessThrottlingData = BrightnessThrottlingData.create(throttlingLevels);
        }
    }

    private void loadBrightnessMapFromConfigXml() {
        Resources res = this.mContext.getResources();
        float[] sysNits = BrightnessMappingStrategy.getFloatArray(res.obtainTypedArray(17236124));
        int[] sysBrightness = res.getIntArray(17236123);
        float[] sysBrightnessFloat = new float[sysBrightness.length];
        for (int i = 0; i < sysBrightness.length; i++) {
            sysBrightnessFloat[i] = BrightnessSynchronizer.brightnessIntToFloat(sysBrightness[i]);
        }
        if (SystemProperties.getBoolean("ro.tinno.project", false)) {
            int[] cusBrightness = res.getIntArray(17236017);
            float[] cusBrightnessFloat = new float[cusBrightness.length];
            for (int i2 = 0; i2 < cusBrightness.length; i2++) {
                cusBrightnessFloat[i2] = BrightnessSynchronizer.brightnessIntToFloat(cusBrightness[i2]);
            }
            this.mCusBrightness = cusBrightnessFloat;
        }
        if (sysBrightnessFloat.length == 0 || sysNits.length == 0) {
            setSimpleMappingStrategyValues();
            return;
        }
        this.mRawNits = sysNits;
        this.mRawBacklight = sysBrightnessFloat;
        constrainNitsAndBacklightArrays();
    }

    private void setSimpleMappingStrategyValues() {
        this.mNits = null;
        this.mBacklight = null;
        float[] simpleMappingStrategyArray = {0.0f, 1.0f};
        this.mBrightnessToBacklightSpline = Spline.createSpline(simpleMappingStrategyArray, simpleMappingStrategyArray);
        this.mBacklightToBrightnessSpline = Spline.createSpline(simpleMappingStrategyArray, simpleMappingStrategyArray);
    }

    private void constrainNitsAndBacklightArrays() {
        float newBacklightVal;
        float newNitsVal;
        float[] fArr = this.mRawBacklight;
        float f = fArr[0];
        float f2 = this.mBacklightMinimum;
        if (f <= f2) {
            float f3 = fArr[fArr.length - 1];
            float f4 = this.mBacklightMaximum;
            if (f3 >= f4 && f2 <= f4) {
                float[] newNits = new float[fArr.length];
                float[] newBacklight = new float[fArr.length];
                int newStart = 0;
                int i = 0;
                while (true) {
                    float[] fArr2 = this.mRawBacklight;
                    if (i >= fArr2.length - 1) {
                        break;
                    } else if (fArr2[i + 1] <= this.mBacklightMinimum) {
                        i++;
                    } else {
                        newStart = i;
                        break;
                    }
                }
                boolean isLastValue = false;
                int newIndex = 0;
                int i2 = newStart;
                while (true) {
                    float[] fArr3 = this.mRawBacklight;
                    if (i2 >= fArr3.length || isLastValue) {
                        break;
                    }
                    newIndex = i2 - newStart;
                    float f5 = fArr3[i2];
                    float f6 = this.mBacklightMaximum;
                    isLastValue = f5 >= f6 || i2 >= fArr3.length - 1;
                    if (newIndex == 0) {
                        newBacklightVal = MathUtils.max(f5, this.mBacklightMinimum);
                        newNitsVal = rawBacklightToNits(i2, newBacklightVal);
                    } else if (isLastValue) {
                        newBacklightVal = MathUtils.min(f5, f6);
                        newNitsVal = rawBacklightToNits(i2 - 1, newBacklightVal);
                    } else {
                        newBacklightVal = fArr3[i2];
                        newNitsVal = this.mRawNits[i2];
                    }
                    newBacklight[newIndex] = newBacklightVal;
                    newNits[newIndex] = newNitsVal;
                    i2++;
                }
                this.mBacklight = Arrays.copyOf(newBacklight, newIndex + 1);
                this.mNits = Arrays.copyOf(newNits, newIndex + 1);
                createBacklightConversionSplines();
                return;
            }
        }
        StringBuilder append = new StringBuilder().append("Min or max values are invalid; raw min=").append(this.mRawBacklight[0]).append("; raw max=");
        float[] fArr4 = this.mRawBacklight;
        throw new IllegalStateException(append.append(fArr4[fArr4.length - 1]).append("; backlight min=").append(this.mBacklightMinimum).append("; backlight max=").append(this.mBacklightMaximum).toString());
    }

    private float rawBacklightToNits(int i, float backlight) {
        float[] fArr = this.mRawBacklight;
        float f = fArr[i];
        float f2 = fArr[i + 1];
        float[] fArr2 = this.mRawNits;
        return MathUtils.map(f, f2, fArr2[i], fArr2[i + 1], backlight);
    }

    private void createBacklightConversionSplines() {
        Spline createSpline;
        Spline createSpline2;
        Spline createSpline3;
        Spline createSpline4;
        this.mBrightness = new float[this.mBacklight.length];
        int i = 0;
        while (true) {
            float[] fArr = this.mBrightness;
            if (i >= fArr.length) {
                break;
            }
            float[] fArr2 = this.mBacklight;
            fArr[i] = MathUtils.map(fArr2[0], fArr2[fArr2.length - 1], 0.0f, 1.0f, fArr2[i]);
            i++;
        }
        if (SystemProperties.getBoolean("ro.tinno.project", false) && this.mCusBrightness.length == this.mBacklight.length) {
            int i2 = 0;
            while (true) {
                float[] fArr3 = this.mCusBrightness;
                if (i2 >= fArr3.length) {
                    break;
                }
                this.mBrightness[i2] = MathUtils.map(fArr3[0], fArr3[fArr3.length - 1], 0.0f, 1.0f, fArr3[i2]);
                i2++;
            }
        }
        int i3 = this.mInterpolationType;
        if (i3 == 1) {
            createSpline = Spline.createLinearSpline(this.mBrightness, this.mBacklight);
        } else {
            createSpline = Spline.createSpline(this.mBrightness, this.mBacklight);
        }
        this.mBrightnessToBacklightSpline = createSpline;
        if (this.mInterpolationType == 1) {
            createSpline2 = Spline.createLinearSpline(this.mBacklight, this.mBrightness);
        } else {
            createSpline2 = Spline.createSpline(this.mBacklight, this.mBrightness);
        }
        this.mBacklightToBrightnessSpline = createSpline2;
        if (this.mInterpolationType == 1) {
            createSpline3 = Spline.createLinearSpline(this.mBacklight, this.mNits);
        } else {
            createSpline3 = Spline.createSpline(this.mBacklight, this.mNits);
        }
        this.mBacklightToNitsSpline = createSpline3;
        if (this.mInterpolationType == 1) {
            createSpline4 = Spline.createLinearSpline(this.mNits, this.mBacklight);
        } else {
            createSpline4 = Spline.createSpline(this.mNits, this.mBacklight);
        }
        this.mNitsToBacklightSpline = createSpline4;
    }

    private void loadQuirks(DisplayConfiguration config) {
        DisplayQuirks quirks = config.getQuirks();
        if (quirks != null) {
            this.mQuirks = new ArrayList(quirks.getQuirk());
        }
    }

    private void loadHighBrightnessModeData(DisplayConfiguration config) {
        HighBrightnessMode hbm = config.getHighBrightnessMode();
        if (hbm != null) {
            this.mIsHighBrightnessModeEnabled = hbm.getEnabled();
            HighBrightnessModeData highBrightnessModeData = new HighBrightnessModeData();
            this.mHbmData = highBrightnessModeData;
            highBrightnessModeData.minimumLux = hbm.getMinimumLux_all().floatValue();
            float transitionPointBacklightScale = hbm.getTransitionPoint_all().floatValue();
            if (transitionPointBacklightScale >= this.mBacklightMaximum) {
                throw new IllegalArgumentException("HBM transition point invalid. " + this.mHbmData.transitionPoint + " is not less than " + this.mBacklightMaximum);
            }
            this.mHbmData.transitionPoint = this.mBacklightToBrightnessSpline.interpolate(transitionPointBacklightScale);
            HbmTiming hbmTiming = hbm.getTiming_all();
            this.mHbmData.timeWindowMillis = hbmTiming.getTimeWindowSecs_all().longValue() * 1000;
            this.mHbmData.timeMaxMillis = hbmTiming.getTimeMaxSecs_all().longValue() * 1000;
            this.mHbmData.timeMinMillis = hbmTiming.getTimeMinSecs_all().longValue() * 1000;
            this.mHbmData.thermalStatusLimit = convertThermalStatus(hbm.getThermalStatusLimit_all());
            this.mHbmData.allowInLowPowerMode = hbm.getAllowInLowPowerMode_all();
            RefreshRateRange rr = hbm.getRefreshRate_all();
            if (rr != null) {
                float min = rr.getMinimum().floatValue();
                float max = rr.getMaximum().floatValue();
                this.mRefreshRateLimitations.add(new DisplayManagerInternal.RefreshRateLimitation(1, min, max));
            }
            BigDecimal minHdrPctOfScreen = hbm.getMinimumHdrPercentOfScreen_all();
            if (minHdrPctOfScreen == null) {
                this.mHbmData.minimumHdrPercentOfScreen = 0.5f;
            } else {
                this.mHbmData.minimumHdrPercentOfScreen = minHdrPctOfScreen.floatValue();
                if (this.mHbmData.minimumHdrPercentOfScreen > 1.0f || this.mHbmData.minimumHdrPercentOfScreen < 0.0f) {
                    Slog.w(TAG, "Invalid minimum HDR percent of screen: " + String.valueOf(this.mHbmData.minimumHdrPercentOfScreen));
                    this.mHbmData.minimumHdrPercentOfScreen = 0.5f;
                }
            }
            this.mSdrToHdrRatioSpline = loadSdrHdrRatioMap(hbm);
        }
    }

    private void loadBrightnessRamps(DisplayConfiguration config) {
        BigDecimal fastDownDecimal = config.getScreenBrightnessRampFastDecrease();
        BigDecimal fastUpDecimal = config.getScreenBrightnessRampFastIncrease();
        BigDecimal slowDownDecimal = config.getScreenBrightnessRampSlowDecrease();
        BigDecimal slowUpDecimal = config.getScreenBrightnessRampSlowIncrease();
        if (fastDownDecimal != null && fastUpDecimal != null && slowDownDecimal != null && slowUpDecimal != null) {
            this.mBrightnessRampFastDecrease = fastDownDecimal.floatValue();
            this.mBrightnessRampFastIncrease = fastUpDecimal.floatValue();
            this.mBrightnessRampSlowDecrease = slowDownDecimal.floatValue();
            this.mBrightnessRampSlowIncrease = slowUpDecimal.floatValue();
        } else {
            if (fastDownDecimal != null || fastUpDecimal != null || slowDownDecimal != null || slowUpDecimal != null) {
                Slog.w(TAG, "Per display brightness ramp values ignored because not all values are present in display device config");
            }
            loadBrightnessRampsFromConfigXml();
        }
        BigInteger increaseMax = config.getScreenBrightnessRampIncreaseMaxMillis();
        if (increaseMax != null) {
            this.mBrightnessRampIncreaseMaxMillis = increaseMax.intValue();
        }
        BigInteger decreaseMax = config.getScreenBrightnessRampDecreaseMaxMillis();
        if (decreaseMax != null) {
            this.mBrightnessRampDecreaseMaxMillis = decreaseMax.intValue();
        }
    }

    private void loadBrightnessRampsFromConfigXml() {
        this.mBrightnessRampFastIncrease = BrightnessSynchronizer.brightnessIntToFloat(this.mContext.getResources().getInteger(17694757));
        float brightnessIntToFloat = BrightnessSynchronizer.brightnessIntToFloat(this.mContext.getResources().getInteger(17694758));
        this.mBrightnessRampSlowIncrease = brightnessIntToFloat;
        this.mBrightnessRampFastDecrease = this.mBrightnessRampFastIncrease;
        this.mBrightnessRampSlowDecrease = brightnessIntToFloat;
    }

    private void loadAmbientLightSensorFromConfigXml() {
        this.mAmbientLightSensor.name = "";
        this.mAmbientLightSensor.type = this.mContext.getResources().getString(17039959);
    }

    private void loadAmbientLightSensorFromDdc(DisplayConfiguration config) {
        SensorDetails sensorDetails = config.getLightSensor();
        if (sensorDetails != null) {
            this.mAmbientLightSensor.type = sensorDetails.getType();
            this.mAmbientLightSensor.name = sensorDetails.getName();
            RefreshRateRange rr = sensorDetails.getRefreshRate();
            if (rr != null) {
                this.mAmbientLightSensor.minRefreshRate = rr.getMinimum().floatValue();
                this.mAmbientLightSensor.maxRefreshRate = rr.getMaximum().floatValue();
                return;
            }
            return;
        }
        loadAmbientLightSensorFromConfigXml();
    }

    private void setProxSensorUnspecified() {
        this.mProximitySensor.name = "";
        this.mProximitySensor.type = "";
    }

    private void loadProxSensorFromDdc(DisplayConfiguration config) {
        SensorDetails sensorDetails = config.getProxSensor();
        if (sensorDetails != null) {
            this.mProximitySensor.name = sensorDetails.getName();
            this.mProximitySensor.type = sensorDetails.getType();
            RefreshRateRange rr = sensorDetails.getRefreshRate();
            if (rr != null) {
                this.mProximitySensor.minRefreshRate = rr.getMinimum().floatValue();
                this.mProximitySensor.maxRefreshRate = rr.getMaximum().floatValue();
                return;
            }
            return;
        }
        setProxSensorUnspecified();
    }

    private void loadBrightnessChangeThresholds(DisplayConfiguration config) {
        Thresholds displayBrightnessThresholds = config.getDisplayBrightnessChangeThresholds();
        Thresholds ambientBrightnessThresholds = config.getAmbientBrightnessChangeThresholds();
        if (displayBrightnessThresholds != null) {
            BrightnessThresholds brighteningScreen = displayBrightnessThresholds.getBrighteningThresholds();
            BrightnessThresholds darkeningScreen = displayBrightnessThresholds.getDarkeningThresholds();
            BigDecimal screenBrighteningThreshold = brighteningScreen.getMinimum();
            BigDecimal screenDarkeningThreshold = darkeningScreen.getMinimum();
            if (screenBrighteningThreshold != null) {
                this.mScreenBrighteningMinThreshold = screenBrighteningThreshold.floatValue();
            }
            if (screenDarkeningThreshold != null) {
                this.mScreenDarkeningMinThreshold = screenDarkeningThreshold.floatValue();
            }
        }
        if (ambientBrightnessThresholds != null) {
            BrightnessThresholds brighteningAmbientLux = ambientBrightnessThresholds.getBrighteningThresholds();
            BrightnessThresholds darkeningAmbientLux = ambientBrightnessThresholds.getDarkeningThresholds();
            BigDecimal ambientBrighteningThreshold = brighteningAmbientLux.getMinimum();
            BigDecimal ambientDarkeningThreshold = darkeningAmbientLux.getMinimum();
            if (ambientBrighteningThreshold != null) {
                this.mAmbientLuxBrighteningMinThreshold = ambientBrighteningThreshold.floatValue();
            }
            if (ambientDarkeningThreshold != null) {
                this.mAmbientLuxDarkeningMinThreshold = ambientDarkeningThreshold.floatValue();
            }
        }
    }

    private boolean thermalStatusIsValid(ThermalStatus value) {
        if (value == null) {
            return false;
        }
        switch (AnonymousClass1.$SwitchMap$com$android$server$display$config$ThermalStatus[value.ordinal()]) {
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
            case 7:
                return true;
            default:
                return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: com.android.server.display.DisplayDeviceConfig$1  reason: invalid class name */
    /* loaded from: classes.dex */
    public static /* synthetic */ class AnonymousClass1 {
        static final /* synthetic */ int[] $SwitchMap$com$android$server$display$config$ThermalStatus;

        static {
            int[] iArr = new int[ThermalStatus.values().length];
            $SwitchMap$com$android$server$display$config$ThermalStatus = iArr;
            try {
                iArr[ThermalStatus.none.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$android$server$display$config$ThermalStatus[ThermalStatus.light.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$com$android$server$display$config$ThermalStatus[ThermalStatus.moderate.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$com$android$server$display$config$ThermalStatus[ThermalStatus.severe.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
            try {
                $SwitchMap$com$android$server$display$config$ThermalStatus[ThermalStatus.critical.ordinal()] = 5;
            } catch (NoSuchFieldError e5) {
            }
            try {
                $SwitchMap$com$android$server$display$config$ThermalStatus[ThermalStatus.emergency.ordinal()] = 6;
            } catch (NoSuchFieldError e6) {
            }
            try {
                $SwitchMap$com$android$server$display$config$ThermalStatus[ThermalStatus.shutdown.ordinal()] = 7;
            } catch (NoSuchFieldError e7) {
            }
        }
    }

    private int convertThermalStatus(ThermalStatus value) {
        if (value == null) {
            return 0;
        }
        switch (AnonymousClass1.$SwitchMap$com$android$server$display$config$ThermalStatus[value.ordinal()]) {
            case 1:
                return 0;
            case 2:
                return 1;
            case 3:
                return 2;
            case 4:
                return 3;
            case 5:
                return 4;
            case 6:
                return 5;
            case 7:
                return 6;
            default:
                Slog.wtf(TAG, "Unexpected Thermal Status: " + value);
                return 0;
        }
    }

    private int convertInterpolationType(String value) {
        if (TextUtils.isEmpty(value)) {
            return 0;
        }
        if ("linear".equals(value)) {
            return 1;
        }
        Slog.wtf(TAG, "Unexpected Interpolation Type: " + value);
        return 0;
    }

    private void loadAmbientHorizonFromDdc(DisplayConfiguration config) {
        BigInteger configLongHorizon = config.getAmbientLightHorizonLong();
        if (configLongHorizon != null) {
            this.mAmbientHorizonLong = configLongHorizon.intValue();
        }
        BigInteger configShortHorizon = config.getAmbientLightHorizonShort();
        if (configShortHorizon != null) {
            this.mAmbientHorizonShort = configShortHorizon.intValue();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class SensorData {
        public String name;
        public String type;
        public float minRefreshRate = 0.0f;
        public float maxRefreshRate = Float.POSITIVE_INFINITY;

        SensorData() {
        }

        public String toString() {
            return "Sensor{type: " + this.type + ", name: " + this.name + ", refreshRateRange: [" + this.minRefreshRate + ", " + this.maxRefreshRate + "]} ";
        }

        public boolean matches(String sensorName, String sensorType) {
            boolean isNameSpecified = !TextUtils.isEmpty(sensorName);
            boolean isTypeSpecified = !TextUtils.isEmpty(sensorType);
            return (isNameSpecified || isTypeSpecified) && (!isNameSpecified || sensorName.equals(this.name)) && (!isTypeSpecified || sensorType.equals(this.type));
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class HighBrightnessModeData {
        public boolean allowInLowPowerMode;
        public float minimumHdrPercentOfScreen;
        public float minimumLux;
        public int thermalStatusLimit;
        public long timeMaxMillis;
        public long timeMinMillis;
        public long timeWindowMillis;
        public float transitionPoint;

        HighBrightnessModeData() {
        }

        HighBrightnessModeData(float minimumLux, float transitionPoint, long timeWindowMillis, long timeMaxMillis, long timeMinMillis, int thermalStatusLimit, boolean allowInLowPowerMode, float minimumHdrPercentOfScreen) {
            this.minimumLux = minimumLux;
            this.transitionPoint = transitionPoint;
            this.timeWindowMillis = timeWindowMillis;
            this.timeMaxMillis = timeMaxMillis;
            this.timeMinMillis = timeMinMillis;
            this.thermalStatusLimit = thermalStatusLimit;
            this.allowInLowPowerMode = allowInLowPowerMode;
            this.minimumHdrPercentOfScreen = minimumHdrPercentOfScreen;
        }

        public void copyTo(HighBrightnessModeData other) {
            other.minimumLux = this.minimumLux;
            other.timeWindowMillis = this.timeWindowMillis;
            other.timeMaxMillis = this.timeMaxMillis;
            other.timeMinMillis = this.timeMinMillis;
            other.transitionPoint = this.transitionPoint;
            other.thermalStatusLimit = this.thermalStatusLimit;
            other.allowInLowPowerMode = this.allowInLowPowerMode;
            other.minimumHdrPercentOfScreen = this.minimumHdrPercentOfScreen;
        }

        public String toString() {
            return "HBM{minLux: " + this.minimumLux + ", transition: " + this.transitionPoint + ", timeWindow: " + this.timeWindowMillis + "ms, timeMax: " + this.timeMaxMillis + "ms, timeMin: " + this.timeMinMillis + "ms, thermalStatusLimit: " + this.thermalStatusLimit + ", allowInLowPowerMode: " + this.allowInLowPowerMode + ", minimumHdrPercentOfScreen: " + this.minimumHdrPercentOfScreen + "} ";
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class BrightnessThrottlingData {
        public List<ThrottlingLevel> throttlingLevels;

        /* JADX INFO: Access modifiers changed from: package-private */
        /* loaded from: classes.dex */
        public static class ThrottlingLevel {
            public float brightness;
            public int thermalStatus;

            ThrottlingLevel(int thermalStatus, float brightness) {
                this.thermalStatus = thermalStatus;
                this.brightness = brightness;
            }

            public String toString() {
                return "[" + this.thermalStatus + "," + this.brightness + "]";
            }
        }

        public static BrightnessThrottlingData create(List<ThrottlingLevel> throttlingLevels) {
            if (throttlingLevels == null || throttlingLevels.size() == 0) {
                Slog.e(DisplayDeviceConfig.TAG, "BrightnessThrottlingData received null or empty throttling levels");
                return null;
            }
            ThrottlingLevel prevLevel = throttlingLevels.get(0);
            int numLevels = throttlingLevels.size();
            for (int i = 1; i < numLevels; i++) {
                ThrottlingLevel thisLevel = throttlingLevels.get(i);
                if (thisLevel.thermalStatus <= prevLevel.thermalStatus) {
                    Slog.e(DisplayDeviceConfig.TAG, "brightnessThrottlingMap must be strictly increasing, ignoring configuration. ThermalStatus " + thisLevel.thermalStatus + " <= " + prevLevel.thermalStatus);
                    return null;
                } else if (thisLevel.brightness >= prevLevel.brightness) {
                    Slog.e(DisplayDeviceConfig.TAG, "brightnessThrottlingMap must be strictly decreasing, ignoring configuration. Brightness " + thisLevel.brightness + " >= " + thisLevel.brightness);
                    return null;
                } else {
                    prevLevel = thisLevel;
                }
            }
            for (ThrottlingLevel level : throttlingLevels) {
                if (level.brightness > 1.0f) {
                    Slog.e(DisplayDeviceConfig.TAG, "brightnessThrottlingMap contains a brightness value exceeding system max. Brightness " + level.brightness + " > 1.0");
                    return null;
                }
            }
            return new BrightnessThrottlingData(throttlingLevels);
        }

        public static BrightnessThrottlingData create(BrightnessThrottlingData other) {
            if (other == null) {
                return null;
            }
            return create(other.throttlingLevels);
        }

        public String toString() {
            return "BrightnessThrottlingData{throttlingLevels:" + this.throttlingLevels + "} ";
        }

        private BrightnessThrottlingData(List<ThrottlingLevel> inLevels) {
            this.throttlingLevels = new ArrayList(inLevels.size());
            for (ThrottlingLevel level : inLevels) {
                this.throttlingLevels.add(new ThrottlingLevel(level.thermalStatus, level.brightness));
            }
        }
    }
}
