package com.android.server.display.config;

import java.io.IOException;
import java.math.BigInteger;
import javax.xml.datatype.DatatypeConfigurationException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes.dex */
public class Density {
    private BigInteger density;
    private BigInteger height;
    private BigInteger width;

    public final BigInteger getWidth() {
        return this.width;
    }

    boolean hasWidth() {
        if (this.width == null) {
            return false;
        }
        return true;
    }

    public final void setWidth(BigInteger width) {
        this.width = width;
    }

    public final BigInteger getHeight() {
        return this.height;
    }

    boolean hasHeight() {
        if (this.height == null) {
            return false;
        }
        return true;
    }

    public final void setHeight(BigInteger height) {
        this.height = height;
    }

    public final BigInteger getDensity() {
        return this.density;
    }

    boolean hasDensity() {
        if (this.density == null) {
            return false;
        }
        return true;
    }

    public final void setDensity(BigInteger density) {
        this.density = density;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static Density read(XmlPullParser parser) throws XmlPullParserException, IOException, DatatypeConfigurationException {
        int type;
        Density instance = new Density();
        parser.getDepth();
        while (true) {
            type = parser.next();
            if (type == 1 || type == 3) {
                break;
            } else if (parser.getEventType() == 2) {
                String tagName = parser.getName();
                if (tagName.equals("width")) {
                    String raw = XmlParser.readText(parser);
                    BigInteger value = new BigInteger(raw);
                    instance.setWidth(value);
                } else if (tagName.equals("height")) {
                    String raw2 = XmlParser.readText(parser);
                    BigInteger value2 = new BigInteger(raw2);
                    instance.setHeight(value2);
                } else if (tagName.equals("density")) {
                    String raw3 = XmlParser.readText(parser);
                    BigInteger value3 = new BigInteger(raw3);
                    instance.setDensity(value3);
                } else {
                    XmlParser.skip(parser);
                }
            }
        }
        if (type != 3) {
            throw new DatatypeConfigurationException("Density is not closed");
        }
        return instance;
    }
}
