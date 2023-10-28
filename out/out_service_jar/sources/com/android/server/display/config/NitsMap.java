package com.android.server.display.config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.datatype.DatatypeConfigurationException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes.dex */
public class NitsMap {
    private String interpolation;
    private List<Point> point;

    public final List<Point> getPoint() {
        if (this.point == null) {
            this.point = new ArrayList();
        }
        return this.point;
    }

    public String getInterpolation() {
        return this.interpolation;
    }

    boolean hasInterpolation() {
        if (this.interpolation == null) {
            return false;
        }
        return true;
    }

    public void setInterpolation(String interpolation) {
        this.interpolation = interpolation;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static NitsMap read(XmlPullParser parser) throws XmlPullParserException, IOException, DatatypeConfigurationException {
        int type;
        NitsMap instance = new NitsMap();
        String raw = parser.getAttributeValue(null, "interpolation");
        if (raw != null) {
            instance.setInterpolation(raw);
        }
        parser.getDepth();
        while (true) {
            type = parser.next();
            if (type == 1 || type == 3) {
                break;
            } else if (parser.getEventType() == 2) {
                String tagName = parser.getName();
                if (tagName.equals("point")) {
                    Point value = Point.read(parser);
                    instance.getPoint().add(value);
                } else {
                    XmlParser.skip(parser);
                }
            }
        }
        if (type != 3) {
            throw new DatatypeConfigurationException("NitsMap is not closed");
        }
        return instance;
    }
}
