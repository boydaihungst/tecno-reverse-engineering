package com.android.server.display.config;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import javax.xml.datatype.DatatypeConfigurationException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes.dex */
public class DisplayConfiguration {
    private Thresholds ambientBrightnessChangeThresholds;
    private BigInteger ambientLightHorizonLong;
    private BigInteger ambientLightHorizonShort;
    private DensityMapping densityMapping;
    private Thresholds displayBrightnessChangeThresholds;
    private HighBrightnessMode highBrightnessMode;
    private SensorDetails lightSensor;
    private SensorDetails proxSensor;
    private DisplayQuirks quirks;
    private BigDecimal screenBrightnessDefault;
    private NitsMap screenBrightnessMap;
    private BigInteger screenBrightnessRampDecreaseMaxMillis;
    private BigDecimal screenBrightnessRampFastDecrease;
    private BigDecimal screenBrightnessRampFastIncrease;
    private BigInteger screenBrightnessRampIncreaseMaxMillis;
    private BigDecimal screenBrightnessRampSlowDecrease;
    private BigDecimal screenBrightnessRampSlowIncrease;
    private ThermalThrottling thermalThrottling;

    public final DensityMapping getDensityMapping() {
        return this.densityMapping;
    }

    boolean hasDensityMapping() {
        if (this.densityMapping == null) {
            return false;
        }
        return true;
    }

    public final void setDensityMapping(DensityMapping densityMapping) {
        this.densityMapping = densityMapping;
    }

    public final NitsMap getScreenBrightnessMap() {
        return this.screenBrightnessMap;
    }

    boolean hasScreenBrightnessMap() {
        if (this.screenBrightnessMap == null) {
            return false;
        }
        return true;
    }

    public final void setScreenBrightnessMap(NitsMap screenBrightnessMap) {
        this.screenBrightnessMap = screenBrightnessMap;
    }

    public final BigDecimal getScreenBrightnessDefault() {
        return this.screenBrightnessDefault;
    }

    boolean hasScreenBrightnessDefault() {
        if (this.screenBrightnessDefault == null) {
            return false;
        }
        return true;
    }

    public final void setScreenBrightnessDefault(BigDecimal screenBrightnessDefault) {
        this.screenBrightnessDefault = screenBrightnessDefault;
    }

    public final ThermalThrottling getThermalThrottling() {
        return this.thermalThrottling;
    }

    boolean hasThermalThrottling() {
        if (this.thermalThrottling == null) {
            return false;
        }
        return true;
    }

    public final void setThermalThrottling(ThermalThrottling thermalThrottling) {
        this.thermalThrottling = thermalThrottling;
    }

    public HighBrightnessMode getHighBrightnessMode() {
        return this.highBrightnessMode;
    }

    boolean hasHighBrightnessMode() {
        if (this.highBrightnessMode == null) {
            return false;
        }
        return true;
    }

    public void setHighBrightnessMode(HighBrightnessMode highBrightnessMode) {
        this.highBrightnessMode = highBrightnessMode;
    }

    public DisplayQuirks getQuirks() {
        return this.quirks;
    }

    boolean hasQuirks() {
        if (this.quirks == null) {
            return false;
        }
        return true;
    }

    public void setQuirks(DisplayQuirks quirks) {
        this.quirks = quirks;
    }

    public final BigDecimal getScreenBrightnessRampFastDecrease() {
        return this.screenBrightnessRampFastDecrease;
    }

    boolean hasScreenBrightnessRampFastDecrease() {
        if (this.screenBrightnessRampFastDecrease == null) {
            return false;
        }
        return true;
    }

    public final void setScreenBrightnessRampFastDecrease(BigDecimal screenBrightnessRampFastDecrease) {
        this.screenBrightnessRampFastDecrease = screenBrightnessRampFastDecrease;
    }

    public final BigDecimal getScreenBrightnessRampFastIncrease() {
        return this.screenBrightnessRampFastIncrease;
    }

    boolean hasScreenBrightnessRampFastIncrease() {
        if (this.screenBrightnessRampFastIncrease == null) {
            return false;
        }
        return true;
    }

    public final void setScreenBrightnessRampFastIncrease(BigDecimal screenBrightnessRampFastIncrease) {
        this.screenBrightnessRampFastIncrease = screenBrightnessRampFastIncrease;
    }

    public final BigDecimal getScreenBrightnessRampSlowDecrease() {
        return this.screenBrightnessRampSlowDecrease;
    }

    boolean hasScreenBrightnessRampSlowDecrease() {
        if (this.screenBrightnessRampSlowDecrease == null) {
            return false;
        }
        return true;
    }

    public final void setScreenBrightnessRampSlowDecrease(BigDecimal screenBrightnessRampSlowDecrease) {
        this.screenBrightnessRampSlowDecrease = screenBrightnessRampSlowDecrease;
    }

    public final BigDecimal getScreenBrightnessRampSlowIncrease() {
        return this.screenBrightnessRampSlowIncrease;
    }

    boolean hasScreenBrightnessRampSlowIncrease() {
        if (this.screenBrightnessRampSlowIncrease == null) {
            return false;
        }
        return true;
    }

    public final void setScreenBrightnessRampSlowIncrease(BigDecimal screenBrightnessRampSlowIncrease) {
        this.screenBrightnessRampSlowIncrease = screenBrightnessRampSlowIncrease;
    }

    public final BigInteger getScreenBrightnessRampIncreaseMaxMillis() {
        return this.screenBrightnessRampIncreaseMaxMillis;
    }

    boolean hasScreenBrightnessRampIncreaseMaxMillis() {
        if (this.screenBrightnessRampIncreaseMaxMillis == null) {
            return false;
        }
        return true;
    }

    public final void setScreenBrightnessRampIncreaseMaxMillis(BigInteger screenBrightnessRampIncreaseMaxMillis) {
        this.screenBrightnessRampIncreaseMaxMillis = screenBrightnessRampIncreaseMaxMillis;
    }

    public final BigInteger getScreenBrightnessRampDecreaseMaxMillis() {
        return this.screenBrightnessRampDecreaseMaxMillis;
    }

    boolean hasScreenBrightnessRampDecreaseMaxMillis() {
        if (this.screenBrightnessRampDecreaseMaxMillis == null) {
            return false;
        }
        return true;
    }

    public final void setScreenBrightnessRampDecreaseMaxMillis(BigInteger screenBrightnessRampDecreaseMaxMillis) {
        this.screenBrightnessRampDecreaseMaxMillis = screenBrightnessRampDecreaseMaxMillis;
    }

    public final SensorDetails getLightSensor() {
        return this.lightSensor;
    }

    boolean hasLightSensor() {
        if (this.lightSensor == null) {
            return false;
        }
        return true;
    }

    public final void setLightSensor(SensorDetails lightSensor) {
        this.lightSensor = lightSensor;
    }

    public final SensorDetails getProxSensor() {
        return this.proxSensor;
    }

    boolean hasProxSensor() {
        if (this.proxSensor == null) {
            return false;
        }
        return true;
    }

    public final void setProxSensor(SensorDetails proxSensor) {
        this.proxSensor = proxSensor;
    }

    public final BigInteger getAmbientLightHorizonLong() {
        return this.ambientLightHorizonLong;
    }

    boolean hasAmbientLightHorizonLong() {
        if (this.ambientLightHorizonLong == null) {
            return false;
        }
        return true;
    }

    public final void setAmbientLightHorizonLong(BigInteger ambientLightHorizonLong) {
        this.ambientLightHorizonLong = ambientLightHorizonLong;
    }

    public final BigInteger getAmbientLightHorizonShort() {
        return this.ambientLightHorizonShort;
    }

    boolean hasAmbientLightHorizonShort() {
        if (this.ambientLightHorizonShort == null) {
            return false;
        }
        return true;
    }

    public final void setAmbientLightHorizonShort(BigInteger ambientLightHorizonShort) {
        this.ambientLightHorizonShort = ambientLightHorizonShort;
    }

    public final Thresholds getDisplayBrightnessChangeThresholds() {
        return this.displayBrightnessChangeThresholds;
    }

    boolean hasDisplayBrightnessChangeThresholds() {
        if (this.displayBrightnessChangeThresholds == null) {
            return false;
        }
        return true;
    }

    public final void setDisplayBrightnessChangeThresholds(Thresholds displayBrightnessChangeThresholds) {
        this.displayBrightnessChangeThresholds = displayBrightnessChangeThresholds;
    }

    public final Thresholds getAmbientBrightnessChangeThresholds() {
        return this.ambientBrightnessChangeThresholds;
    }

    boolean hasAmbientBrightnessChangeThresholds() {
        if (this.ambientBrightnessChangeThresholds == null) {
            return false;
        }
        return true;
    }

    public final void setAmbientBrightnessChangeThresholds(Thresholds ambientBrightnessChangeThresholds) {
        this.ambientBrightnessChangeThresholds = ambientBrightnessChangeThresholds;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static DisplayConfiguration read(XmlPullParser parser) throws XmlPullParserException, IOException, DatatypeConfigurationException {
        int type;
        DisplayConfiguration instance = new DisplayConfiguration();
        parser.getDepth();
        while (true) {
            type = parser.next();
            if (type == 1 || type == 3) {
                break;
            } else if (parser.getEventType() == 2) {
                String tagName = parser.getName();
                if (tagName.equals("densityMapping")) {
                    DensityMapping value = DensityMapping.read(parser);
                    instance.setDensityMapping(value);
                } else if (tagName.equals("screenBrightnessMap")) {
                    NitsMap value2 = NitsMap.read(parser);
                    instance.setScreenBrightnessMap(value2);
                } else if (tagName.equals("screenBrightnessDefault")) {
                    String raw = XmlParser.readText(parser);
                    BigDecimal value3 = new BigDecimal(raw);
                    instance.setScreenBrightnessDefault(value3);
                } else if (tagName.equals("thermalThrottling")) {
                    ThermalThrottling value4 = ThermalThrottling.read(parser);
                    instance.setThermalThrottling(value4);
                } else if (tagName.equals("highBrightnessMode")) {
                    HighBrightnessMode value5 = HighBrightnessMode.read(parser);
                    instance.setHighBrightnessMode(value5);
                } else if (tagName.equals("quirks")) {
                    DisplayQuirks value6 = DisplayQuirks.read(parser);
                    instance.setQuirks(value6);
                } else if (tagName.equals("screenBrightnessRampFastDecrease")) {
                    String raw2 = XmlParser.readText(parser);
                    BigDecimal value7 = new BigDecimal(raw2);
                    instance.setScreenBrightnessRampFastDecrease(value7);
                } else if (tagName.equals("screenBrightnessRampFastIncrease")) {
                    String raw3 = XmlParser.readText(parser);
                    BigDecimal value8 = new BigDecimal(raw3);
                    instance.setScreenBrightnessRampFastIncrease(value8);
                } else if (tagName.equals("screenBrightnessRampSlowDecrease")) {
                    String raw4 = XmlParser.readText(parser);
                    BigDecimal value9 = new BigDecimal(raw4);
                    instance.setScreenBrightnessRampSlowDecrease(value9);
                } else if (tagName.equals("screenBrightnessRampSlowIncrease")) {
                    String raw5 = XmlParser.readText(parser);
                    BigDecimal value10 = new BigDecimal(raw5);
                    instance.setScreenBrightnessRampSlowIncrease(value10);
                } else if (tagName.equals("screenBrightnessRampIncreaseMaxMillis")) {
                    String raw6 = XmlParser.readText(parser);
                    BigInteger value11 = new BigInteger(raw6);
                    instance.setScreenBrightnessRampIncreaseMaxMillis(value11);
                } else if (tagName.equals("screenBrightnessRampDecreaseMaxMillis")) {
                    String raw7 = XmlParser.readText(parser);
                    BigInteger value12 = new BigInteger(raw7);
                    instance.setScreenBrightnessRampDecreaseMaxMillis(value12);
                } else if (tagName.equals("lightSensor")) {
                    SensorDetails value13 = SensorDetails.read(parser);
                    instance.setLightSensor(value13);
                } else if (tagName.equals("proxSensor")) {
                    SensorDetails value14 = SensorDetails.read(parser);
                    instance.setProxSensor(value14);
                } else if (tagName.equals("ambientLightHorizonLong")) {
                    String raw8 = XmlParser.readText(parser);
                    BigInteger value15 = new BigInteger(raw8);
                    instance.setAmbientLightHorizonLong(value15);
                } else if (tagName.equals("ambientLightHorizonShort")) {
                    String raw9 = XmlParser.readText(parser);
                    BigInteger value16 = new BigInteger(raw9);
                    instance.setAmbientLightHorizonShort(value16);
                } else if (tagName.equals("displayBrightnessChangeThresholds")) {
                    Thresholds value17 = Thresholds.read(parser);
                    instance.setDisplayBrightnessChangeThresholds(value17);
                } else if (tagName.equals("ambientBrightnessChangeThresholds")) {
                    Thresholds value18 = Thresholds.read(parser);
                    instance.setAmbientBrightnessChangeThresholds(value18);
                } else {
                    XmlParser.skip(parser);
                }
            }
        }
        if (type != 3) {
            throw new DatatypeConfigurationException("DisplayConfiguration is not closed");
        }
        return instance;
    }
}
