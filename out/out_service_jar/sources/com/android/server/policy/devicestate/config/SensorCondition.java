package com.android.server.policy.devicestate.config;

import com.android.server.voiceinteraction.DatabaseHelper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.datatype.DatatypeConfigurationException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes2.dex */
public class SensorCondition {
    private String name;
    private String type;
    private List<NumericRange> value;

    public String getType() {
        return this.type;
    }

    boolean hasType() {
        if (this.type == null) {
            return false;
        }
        return true;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return this.name;
    }

    boolean hasName() {
        if (this.name == null) {
            return false;
        }
        return true;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<NumericRange> getValue() {
        if (this.value == null) {
            this.value = new ArrayList();
        }
        return this.value;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static SensorCondition read(XmlPullParser parser) throws XmlPullParserException, IOException, DatatypeConfigurationException {
        int type;
        SensorCondition instance = new SensorCondition();
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
                } else if (tagName.equals("value")) {
                    NumericRange value = NumericRange.read(parser);
                    instance.getValue().add(value);
                } else {
                    XmlParser.skip(parser);
                }
            }
        }
        if (type != 3) {
            throw new DatatypeConfigurationException("SensorCondition is not closed");
        }
        return instance;
    }
}
