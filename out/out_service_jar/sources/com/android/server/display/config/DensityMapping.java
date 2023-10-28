package com.android.server.display.config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.datatype.DatatypeConfigurationException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes.dex */
public class DensityMapping {
    private List<Density> density;

    public List<Density> getDensity() {
        if (this.density == null) {
            this.density = new ArrayList();
        }
        return this.density;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static DensityMapping read(XmlPullParser parser) throws XmlPullParserException, IOException, DatatypeConfigurationException {
        int type;
        DensityMapping instance = new DensityMapping();
        parser.getDepth();
        while (true) {
            type = parser.next();
            if (type == 1 || type == 3) {
                break;
            } else if (parser.getEventType() == 2) {
                String tagName = parser.getName();
                if (tagName.equals("density")) {
                    Density value = Density.read(parser);
                    instance.getDensity().add(value);
                } else {
                    XmlParser.skip(parser);
                }
            }
        }
        if (type != 3) {
            throw new DatatypeConfigurationException("DensityMapping is not closed");
        }
        return instance;
    }
}
