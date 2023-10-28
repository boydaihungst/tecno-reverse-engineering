package com.android.server.display.config.layout;

import com.android.server.timezonedetector.ServiceConfigAccessor;
import java.io.IOException;
import java.math.BigInteger;
import javax.xml.datatype.DatatypeConfigurationException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes.dex */
public class Display {
    private BigInteger address;
    private Boolean defaultDisplay;
    private Boolean enabled;

    public BigInteger getAddress() {
        return this.address;
    }

    boolean hasAddress() {
        if (this.address == null) {
            return false;
        }
        return true;
    }

    public void setAddress(BigInteger address) {
        this.address = address;
    }

    public boolean isEnabled() {
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

    public boolean isDefaultDisplay() {
        Boolean bool = this.defaultDisplay;
        if (bool == null) {
            return false;
        }
        return bool.booleanValue();
    }

    boolean hasDefaultDisplay() {
        if (this.defaultDisplay == null) {
            return false;
        }
        return true;
    }

    public void setDefaultDisplay(boolean defaultDisplay) {
        this.defaultDisplay = Boolean.valueOf(defaultDisplay);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static Display read(XmlPullParser parser) throws XmlPullParserException, IOException, DatatypeConfigurationException {
        int type;
        Display instance = new Display();
        String raw = parser.getAttributeValue(null, ServiceConfigAccessor.PROVIDER_MODE_ENABLED);
        if (raw != null) {
            boolean value = Boolean.parseBoolean(raw);
            instance.setEnabled(value);
        }
        String raw2 = parser.getAttributeValue(null, "defaultDisplay");
        if (raw2 != null) {
            boolean value2 = Boolean.parseBoolean(raw2);
            instance.setDefaultDisplay(value2);
        }
        parser.getDepth();
        while (true) {
            type = parser.next();
            if (type == 1 || type == 3) {
                break;
            } else if (parser.getEventType() == 2) {
                String tagName = parser.getName();
                if (tagName.equals("address")) {
                    BigInteger value3 = new BigInteger(XmlParser.readText(parser));
                    instance.setAddress(value3);
                } else {
                    XmlParser.skip(parser);
                }
            }
        }
        if (type != 3) {
            throw new DatatypeConfigurationException("Display is not closed");
        }
        return instance;
    }
}
