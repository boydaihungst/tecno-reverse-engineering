package com.android.server.policy.devicestate.config;

import java.io.IOException;
import java.math.BigInteger;
import javax.xml.datatype.DatatypeConfigurationException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes2.dex */
public class DeviceState {
    private Conditions conditions;
    private Flags flags;
    private BigInteger identifier;
    private String name;

    public BigInteger getIdentifier() {
        return this.identifier;
    }

    boolean hasIdentifier() {
        if (this.identifier == null) {
            return false;
        }
        return true;
    }

    public void setIdentifier(BigInteger identifier) {
        this.identifier = identifier;
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

    public Flags getFlags() {
        return this.flags;
    }

    boolean hasFlags() {
        if (this.flags == null) {
            return false;
        }
        return true;
    }

    public void setFlags(Flags flags) {
        this.flags = flags;
    }

    public Conditions getConditions() {
        return this.conditions;
    }

    boolean hasConditions() {
        if (this.conditions == null) {
            return false;
        }
        return true;
    }

    public void setConditions(Conditions conditions) {
        this.conditions = conditions;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static DeviceState read(XmlPullParser parser) throws XmlPullParserException, IOException, DatatypeConfigurationException {
        int type;
        DeviceState instance = new DeviceState();
        parser.getDepth();
        while (true) {
            type = parser.next();
            if (type == 1 || type == 3) {
                break;
            } else if (parser.getEventType() == 2) {
                String tagName = parser.getName();
                if (tagName.equals("identifier")) {
                    String raw = XmlParser.readText(parser);
                    BigInteger value = new BigInteger(raw);
                    instance.setIdentifier(value);
                } else if (tagName.equals("name")) {
                    String raw2 = XmlParser.readText(parser);
                    instance.setName(raw2);
                } else if (tagName.equals("flags")) {
                    Flags value2 = Flags.read(parser);
                    instance.setFlags(value2);
                } else if (tagName.equals("conditions")) {
                    Conditions value3 = Conditions.read(parser);
                    instance.setConditions(value3);
                } else {
                    XmlParser.skip(parser);
                }
            }
        }
        if (type != 3) {
            throw new DatatypeConfigurationException("DeviceState is not closed");
        }
        return instance;
    }
}
