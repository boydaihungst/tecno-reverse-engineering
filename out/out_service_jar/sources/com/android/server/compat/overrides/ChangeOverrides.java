package com.android.server.compat.overrides;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.datatype.DatatypeConfigurationException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes.dex */
public class ChangeOverrides {
    private Long changeId;
    private Deferred deferred;
    private Raw raw;
    private Validated validated;

    /* loaded from: classes.dex */
    public static class Validated {
        private List<OverrideValue> overrideValue;

        public List<OverrideValue> getOverrideValue() {
            if (this.overrideValue == null) {
                this.overrideValue = new ArrayList();
            }
            return this.overrideValue;
        }

        static Validated read(XmlPullParser parser) throws XmlPullParserException, IOException, DatatypeConfigurationException {
            int type;
            Validated instance = new Validated();
            parser.getDepth();
            while (true) {
                type = parser.next();
                if (type == 1 || type == 3) {
                    break;
                } else if (parser.getEventType() == 2) {
                    String tagName = parser.getName();
                    if (tagName.equals("override-value")) {
                        OverrideValue value = OverrideValue.read(parser);
                        instance.getOverrideValue().add(value);
                    } else {
                        XmlParser.skip(parser);
                    }
                }
            }
            if (type != 3) {
                throw new DatatypeConfigurationException("ChangeOverrides.Validated is not closed");
            }
            return instance;
        }

        void write(XmlWriter out, String name) throws IOException {
            out.print("<" + name);
            out.print(">\n");
            out.increaseIndent();
            for (OverrideValue value : getOverrideValue()) {
                value.write(out, "override-value");
            }
            out.decreaseIndent();
            out.print("</" + name + ">\n");
        }
    }

    /* loaded from: classes.dex */
    public static class Deferred {
        private List<OverrideValue> overrideValue;

        public List<OverrideValue> getOverrideValue() {
            if (this.overrideValue == null) {
                this.overrideValue = new ArrayList();
            }
            return this.overrideValue;
        }

        static Deferred read(XmlPullParser parser) throws XmlPullParserException, IOException, DatatypeConfigurationException {
            int type;
            Deferred instance = new Deferred();
            parser.getDepth();
            while (true) {
                type = parser.next();
                if (type == 1 || type == 3) {
                    break;
                } else if (parser.getEventType() == 2) {
                    String tagName = parser.getName();
                    if (tagName.equals("override-value")) {
                        OverrideValue value = OverrideValue.read(parser);
                        instance.getOverrideValue().add(value);
                    } else {
                        XmlParser.skip(parser);
                    }
                }
            }
            if (type != 3) {
                throw new DatatypeConfigurationException("ChangeOverrides.Deferred is not closed");
            }
            return instance;
        }

        void write(XmlWriter out, String name) throws IOException {
            out.print("<" + name);
            out.print(">\n");
            out.increaseIndent();
            for (OverrideValue value : getOverrideValue()) {
                value.write(out, "override-value");
            }
            out.decreaseIndent();
            out.print("</" + name + ">\n");
        }
    }

    /* loaded from: classes.dex */
    public static class Raw {
        private List<RawOverrideValue> rawOverrideValue;

        public List<RawOverrideValue> getRawOverrideValue() {
            if (this.rawOverrideValue == null) {
                this.rawOverrideValue = new ArrayList();
            }
            return this.rawOverrideValue;
        }

        static Raw read(XmlPullParser parser) throws XmlPullParserException, IOException, DatatypeConfigurationException {
            int type;
            Raw instance = new Raw();
            parser.getDepth();
            while (true) {
                type = parser.next();
                if (type == 1 || type == 3) {
                    break;
                } else if (parser.getEventType() == 2) {
                    String tagName = parser.getName();
                    if (tagName.equals("raw-override-value")) {
                        RawOverrideValue value = RawOverrideValue.read(parser);
                        instance.getRawOverrideValue().add(value);
                    } else {
                        XmlParser.skip(parser);
                    }
                }
            }
            if (type != 3) {
                throw new DatatypeConfigurationException("ChangeOverrides.Raw is not closed");
            }
            return instance;
        }

        void write(XmlWriter out, String name) throws IOException {
            out.print("<" + name);
            out.print(">\n");
            out.increaseIndent();
            for (RawOverrideValue value : getRawOverrideValue()) {
                value.write(out, "raw-override-value");
            }
            out.decreaseIndent();
            out.print("</" + name + ">\n");
        }
    }

    public Validated getValidated() {
        return this.validated;
    }

    boolean hasValidated() {
        if (this.validated == null) {
            return false;
        }
        return true;
    }

    public void setValidated(Validated validated) {
        this.validated = validated;
    }

    public Deferred getDeferred() {
        return this.deferred;
    }

    boolean hasDeferred() {
        if (this.deferred == null) {
            return false;
        }
        return true;
    }

    public void setDeferred(Deferred deferred) {
        this.deferred = deferred;
    }

    public Raw getRaw() {
        return this.raw;
    }

    boolean hasRaw() {
        if (this.raw == null) {
            return false;
        }
        return true;
    }

    public void setRaw(Raw raw) {
        this.raw = raw;
    }

    public long getChangeId() {
        Long l = this.changeId;
        if (l == null) {
            return 0L;
        }
        return l.longValue();
    }

    boolean hasChangeId() {
        if (this.changeId == null) {
            return false;
        }
        return true;
    }

    public void setChangeId(long changeId) {
        this.changeId = Long.valueOf(changeId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static ChangeOverrides read(XmlPullParser parser) throws XmlPullParserException, IOException, DatatypeConfigurationException {
        int type;
        ChangeOverrides instance = new ChangeOverrides();
        String raw = parser.getAttributeValue(null, "changeId");
        if (raw != null) {
            long value = Long.parseLong(raw);
            instance.setChangeId(value);
        }
        parser.getDepth();
        while (true) {
            type = parser.next();
            if (type == 1 || type == 3) {
                break;
            } else if (parser.getEventType() == 2) {
                String tagName = parser.getName();
                if (tagName.equals("validated")) {
                    Validated value2 = Validated.read(parser);
                    instance.setValidated(value2);
                } else if (tagName.equals("deferred")) {
                    Deferred value3 = Deferred.read(parser);
                    instance.setDeferred(value3);
                } else if (tagName.equals("raw")) {
                    Raw value4 = Raw.read(parser);
                    instance.setRaw(value4);
                } else {
                    XmlParser.skip(parser);
                }
            }
        }
        if (type != 3) {
            throw new DatatypeConfigurationException("ChangeOverrides is not closed");
        }
        return instance;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void write(XmlWriter out, String name) throws IOException {
        out.print("<" + name);
        if (hasChangeId()) {
            out.print(" changeId=\"");
            out.print(Long.toString(getChangeId()));
            out.print("\"");
        }
        out.print(">\n");
        out.increaseIndent();
        if (hasValidated()) {
            getValidated().write(out, "validated");
        }
        if (hasDeferred()) {
            getDeferred().write(out, "deferred");
        }
        if (hasRaw()) {
            getRaw().write(out, "raw");
        }
        out.decreaseIndent();
        out.print("</" + name + ">\n");
    }
}
