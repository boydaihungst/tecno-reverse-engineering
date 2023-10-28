package android.audio.policy.configuration.V7_0;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.datatype.DatatypeConfigurationException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes.dex */
public class Routes {
    private List<Route> route;

    /* loaded from: classes.dex */
    public static class Route {
        private String sink;
        private String sources;
        private MixType type;

        public MixType getType() {
            return this.type;
        }

        boolean hasType() {
            if (this.type == null) {
                return false;
            }
            return true;
        }

        public void setType(MixType type) {
            this.type = type;
        }

        public String getSink() {
            return this.sink;
        }

        boolean hasSink() {
            if (this.sink == null) {
                return false;
            }
            return true;
        }

        public void setSink(String sink) {
            this.sink = sink;
        }

        public String getSources() {
            return this.sources;
        }

        boolean hasSources() {
            if (this.sources == null) {
                return false;
            }
            return true;
        }

        public void setSources(String sources) {
            this.sources = sources;
        }

        static Route read(XmlPullParser parser) throws XmlPullParserException, IOException, DatatypeConfigurationException {
            Route instance = new Route();
            String raw = parser.getAttributeValue(null, "type");
            if (raw != null) {
                MixType value = MixType.fromString(raw);
                instance.setType(value);
            }
            String raw2 = parser.getAttributeValue(null, "sink");
            if (raw2 != null) {
                instance.setSink(raw2);
            }
            String raw3 = parser.getAttributeValue(null, "sources");
            if (raw3 != null) {
                instance.setSources(raw3);
            }
            XmlParser.skip(parser);
            return instance;
        }
    }

    public List<Route> getRoute() {
        if (this.route == null) {
            this.route = new ArrayList();
        }
        return this.route;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static Routes read(XmlPullParser parser) throws XmlPullParserException, IOException, DatatypeConfigurationException {
        int type;
        Routes instance = new Routes();
        parser.getDepth();
        while (true) {
            type = parser.next();
            if (type == 1 || type == 3) {
                break;
            } else if (parser.getEventType() == 2) {
                String tagName = parser.getName();
                if (tagName.equals("route")) {
                    Route value = Route.read(parser);
                    instance.getRoute().add(value);
                } else {
                    XmlParser.skip(parser);
                }
            }
        }
        if (type != 3) {
            throw new DatatypeConfigurationException("Routes is not closed");
        }
        return instance;
    }
}
