package com.android.server.display.config;

import com.android.server.voiceinteraction.DatabaseHelper;
import java.io.IOException;
import javax.xml.datatype.DatatypeConfigurationException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes.dex */
public class SensorDetails {
    private String name;
    private RefreshRateRange refreshRate;
    private String type;

    public final String getType() {
        return this.type;
    }

    boolean hasType() {
        if (this.type == null) {
            return false;
        }
        return true;
    }

    public final void setType(String type) {
        this.type = type;
    }

    public final String getName() {
        return this.name;
    }

    boolean hasName() {
        if (this.name == null) {
            return false;
        }
        return true;
    }

    public final void setName(String name) {
        this.name = name;
    }

    public final RefreshRateRange getRefreshRate() {
        return this.refreshRate;
    }

    boolean hasRefreshRate() {
        if (this.refreshRate == null) {
            return false;
        }
        return true;
    }

    public final void setRefreshRate(RefreshRateRange refreshRate) {
        this.refreshRate = refreshRate;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static SensorDetails read(XmlPullParser parser) throws XmlPullParserException, IOException, DatatypeConfigurationException {
        int type;
        SensorDetails instance = new SensorDetails();
        parser.getDepth();
        while (true) {
            type = parser.next();
            if (type == 1 || type == 3) {
                break;
            } else if (parser.getEventType() == 2) {
                String tagName = parser.getName();
                if (tagName.equals(DatabaseHelper.SoundModelContract.KEY_TYPE)) {
                    String raw = XmlParser.readText(parser);
                    instance.setType(raw);
                } else if (tagName.equals("name")) {
                    String raw2 = XmlParser.readText(parser);
                    instance.setName(raw2);
                } else if (tagName.equals("refreshRate")) {
                    RefreshRateRange value = RefreshRateRange.read(parser);
                    instance.setRefreshRate(value);
                } else {
                    XmlParser.skip(parser);
                }
            }
        }
        if (type != 3) {
            throw new DatatypeConfigurationException("SensorDetails is not closed");
        }
        return instance;
    }
}
