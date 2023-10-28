package com.android.server.policy.devicestate.config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.datatype.DatatypeConfigurationException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes2.dex */
public class Conditions {
    private LidSwitchCondition lidSwitch;
    private List<SensorCondition> sensor;

    public LidSwitchCondition getLidSwitch() {
        return this.lidSwitch;
    }

    boolean hasLidSwitch() {
        if (this.lidSwitch == null) {
            return false;
        }
        return true;
    }

    public void setLidSwitch(LidSwitchCondition lidSwitch) {
        this.lidSwitch = lidSwitch;
    }

    public List<SensorCondition> getSensor() {
        if (this.sensor == null) {
            this.sensor = new ArrayList();
        }
        return this.sensor;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static Conditions read(XmlPullParser parser) throws XmlPullParserException, IOException, DatatypeConfigurationException {
        int type;
        Conditions instance = new Conditions();
        parser.getDepth();
        while (true) {
            type = parser.next();
            if (type == 1 || type == 3) {
                break;
            } else if (parser.getEventType() == 2) {
                String tagName = parser.getName();
                if (tagName.equals("lid-switch")) {
                    LidSwitchCondition value = LidSwitchCondition.read(parser);
                    instance.setLidSwitch(value);
                } else if (tagName.equals("sensor")) {
                    SensorCondition value2 = SensorCondition.read(parser);
                    instance.getSensor().add(value2);
                } else {
                    XmlParser.skip(parser);
                }
            }
        }
        if (type != 3) {
            throw new DatatypeConfigurationException("Conditions is not closed");
        }
        return instance;
    }
}
