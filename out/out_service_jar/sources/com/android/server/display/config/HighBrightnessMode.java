package com.android.server.display.config;

import com.android.server.timezonedetector.ServiceConfigAccessor;
import java.io.IOException;
import java.math.BigDecimal;
import javax.xml.datatype.DatatypeConfigurationException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes.dex */
public class HighBrightnessMode {
    private Boolean allowInLowPowerMode_all;
    private Boolean enabled;
    private BigDecimal minimumHdrPercentOfScreen_all;
    private BigDecimal minimumLux_all;
    private RefreshRateRange refreshRate_all;
    private SdrHdrRatioMap sdrHdrRatioMap_all;
    private ThermalStatus thermalStatusLimit_all;
    private HbmTiming timing_all;
    private BigDecimal transitionPoint_all;

    public final BigDecimal getTransitionPoint_all() {
        return this.transitionPoint_all;
    }

    boolean hasTransitionPoint_all() {
        if (this.transitionPoint_all == null) {
            return false;
        }
        return true;
    }

    public final void setTransitionPoint_all(BigDecimal transitionPoint_all) {
        this.transitionPoint_all = transitionPoint_all;
    }

    public final BigDecimal getMinimumLux_all() {
        return this.minimumLux_all;
    }

    boolean hasMinimumLux_all() {
        if (this.minimumLux_all == null) {
            return false;
        }
        return true;
    }

    public final void setMinimumLux_all(BigDecimal minimumLux_all) {
        this.minimumLux_all = minimumLux_all;
    }

    public HbmTiming getTiming_all() {
        return this.timing_all;
    }

    boolean hasTiming_all() {
        if (this.timing_all == null) {
            return false;
        }
        return true;
    }

    public void setTiming_all(HbmTiming timing_all) {
        this.timing_all = timing_all;
    }

    public final RefreshRateRange getRefreshRate_all() {
        return this.refreshRate_all;
    }

    boolean hasRefreshRate_all() {
        if (this.refreshRate_all == null) {
            return false;
        }
        return true;
    }

    public final void setRefreshRate_all(RefreshRateRange refreshRate_all) {
        this.refreshRate_all = refreshRate_all;
    }

    public final ThermalStatus getThermalStatusLimit_all() {
        return this.thermalStatusLimit_all;
    }

    boolean hasThermalStatusLimit_all() {
        if (this.thermalStatusLimit_all == null) {
            return false;
        }
        return true;
    }

    public final void setThermalStatusLimit_all(ThermalStatus thermalStatusLimit_all) {
        this.thermalStatusLimit_all = thermalStatusLimit_all;
    }

    public final boolean getAllowInLowPowerMode_all() {
        Boolean bool = this.allowInLowPowerMode_all;
        if (bool == null) {
            return false;
        }
        return bool.booleanValue();
    }

    boolean hasAllowInLowPowerMode_all() {
        if (this.allowInLowPowerMode_all == null) {
            return false;
        }
        return true;
    }

    public final void setAllowInLowPowerMode_all(boolean allowInLowPowerMode_all) {
        this.allowInLowPowerMode_all = Boolean.valueOf(allowInLowPowerMode_all);
    }

    public final BigDecimal getMinimumHdrPercentOfScreen_all() {
        return this.minimumHdrPercentOfScreen_all;
    }

    boolean hasMinimumHdrPercentOfScreen_all() {
        if (this.minimumHdrPercentOfScreen_all == null) {
            return false;
        }
        return true;
    }

    public final void setMinimumHdrPercentOfScreen_all(BigDecimal minimumHdrPercentOfScreen_all) {
        this.minimumHdrPercentOfScreen_all = minimumHdrPercentOfScreen_all;
    }

    public final SdrHdrRatioMap getSdrHdrRatioMap_all() {
        return this.sdrHdrRatioMap_all;
    }

    boolean hasSdrHdrRatioMap_all() {
        if (this.sdrHdrRatioMap_all == null) {
            return false;
        }
        return true;
    }

    public final void setSdrHdrRatioMap_all(SdrHdrRatioMap sdrHdrRatioMap_all) {
        this.sdrHdrRatioMap_all = sdrHdrRatioMap_all;
    }

    public boolean getEnabled() {
        Boolean bool = this.enabled;
        if (bool == null) {
            return false;
        }
        return bool.booleanValue();
    }

    boolean hasEnabled() {
        if (this.enabled == null) {
            return false;
        }
        return true;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = Boolean.valueOf(enabled);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static HighBrightnessMode read(XmlPullParser parser) throws XmlPullParserException, IOException, DatatypeConfigurationException {
        int type;
        HighBrightnessMode instance = new HighBrightnessMode();
        String raw = parser.getAttributeValue(null, ServiceConfigAccessor.PROVIDER_MODE_ENABLED);
        if (raw != null) {
            boolean value = Boolean.parseBoolean(raw);
            instance.setEnabled(value);
        }
        parser.getDepth();
        while (true) {
            type = parser.next();
            if (type == 1 || type == 3) {
                break;
            } else if (parser.getEventType() == 2) {
                String tagName = parser.getName();
                if (tagName.equals("transitionPoint")) {
                    BigDecimal value2 = new BigDecimal(XmlParser.readText(parser));
                    instance.setTransitionPoint_all(value2);
                } else if (tagName.equals("minimumLux")) {
                    BigDecimal value3 = new BigDecimal(XmlParser.readText(parser));
                    instance.setMinimumLux_all(value3);
                } else if (tagName.equals("timing")) {
                    HbmTiming value4 = HbmTiming.read(parser);
                    instance.setTiming_all(value4);
                } else if (tagName.equals("refreshRate")) {
                    RefreshRateRange value5 = RefreshRateRange.read(parser);
                    instance.setRefreshRate_all(value5);
                } else if (tagName.equals("thermalStatusLimit")) {
                    ThermalStatus value6 = ThermalStatus.fromString(XmlParser.readText(parser));
                    instance.setThermalStatusLimit_all(value6);
                } else if (tagName.equals("allowInLowPowerMode")) {
                    boolean value7 = Boolean.parseBoolean(XmlParser.readText(parser));
                    instance.setAllowInLowPowerMode_all(value7);
                } else if (tagName.equals("minimumHdrPercentOfScreen")) {
                    BigDecimal value8 = new BigDecimal(XmlParser.readText(parser));
                    instance.setMinimumHdrPercentOfScreen_all(value8);
                } else if (tagName.equals("sdrHdrRatioMap")) {
                    SdrHdrRatioMap value9 = SdrHdrRatioMap.read(parser);
                    instance.setSdrHdrRatioMap_all(value9);
                } else {
                    XmlParser.skip(parser);
                }
            }
        }
        if (type != 3) {
            throw new DatatypeConfigurationException("HighBrightnessMode is not closed");
        }
        return instance;
    }
}
