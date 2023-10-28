package com.android.server.compat.overrides;

import com.android.server.pm.verify.domain.DomainVerificationLegacySettings;
import com.android.server.timezonedetector.ServiceConfigAccessor;
import java.io.IOException;
import javax.xml.datatype.DatatypeConfigurationException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes.dex */
public class RawOverrideValue {
    private Boolean enabled;
    private Long maxVersionCode;
    private Long minVersionCode;
    private String packageName;

    public String getPackageName() {
        return this.packageName;
    }

    boolean hasPackageName() {
        if (this.packageName == null) {
            return false;
        }
        return true;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public long getMinVersionCode() {
        Long l = this.minVersionCode;
        if (l == null) {
            return 0L;
        }
        return l.longValue();
    }

    boolean hasMinVersionCode() {
        if (this.minVersionCode == null) {
            return false;
        }
        return true;
    }

    public void setMinVersionCode(long minVersionCode) {
        this.minVersionCode = Long.valueOf(minVersionCode);
    }

    public long getMaxVersionCode() {
        Long l = this.maxVersionCode;
        if (l == null) {
            return 0L;
        }
        return l.longValue();
    }

    boolean hasMaxVersionCode() {
        if (this.maxVersionCode == null) {
            return false;
        }
        return true;
    }

    public void setMaxVersionCode(long maxVersionCode) {
        this.maxVersionCode = Long.valueOf(maxVersionCode);
    }

    public boolean getEnabled() {
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

    /* JADX INFO: Access modifiers changed from: package-private */
    public static RawOverrideValue read(XmlPullParser parser) throws XmlPullParserException, IOException, DatatypeConfigurationException {
        RawOverrideValue instance = new RawOverrideValue();
        String raw = parser.getAttributeValue(null, DomainVerificationLegacySettings.ATTR_PACKAGE_NAME);
        if (raw != null) {
            instance.setPackageName(raw);
        }
        String raw2 = parser.getAttributeValue(null, "minVersionCode");
        if (raw2 != null) {
            long value = Long.parseLong(raw2);
            instance.setMinVersionCode(value);
        }
        String raw3 = parser.getAttributeValue(null, "maxVersionCode");
        if (raw3 != null) {
            long value2 = Long.parseLong(raw3);
            instance.setMaxVersionCode(value2);
        }
        String raw4 = parser.getAttributeValue(null, ServiceConfigAccessor.PROVIDER_MODE_ENABLED);
        if (raw4 != null) {
            boolean value3 = Boolean.parseBoolean(raw4);
            instance.setEnabled(value3);
        }
        XmlParser.skip(parser);
        return instance;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void write(XmlWriter out, String name) throws IOException {
        out.print("<" + name);
        if (hasPackageName()) {
            out.print(" packageName=\"");
            out.print(getPackageName());
            out.print("\"");
        }
        if (hasMinVersionCode()) {
            out.print(" minVersionCode=\"");
            out.print(Long.toString(getMinVersionCode()));
            out.print("\"");
        }
        if (hasMaxVersionCode()) {
            out.print(" maxVersionCode=\"");
            out.print(Long.toString(getMaxVersionCode()));
            out.print("\"");
        }
        if (hasEnabled()) {
            out.print(" enabled=\"");
            out.print(Boolean.toString(getEnabled()));
            out.print("\"");
        }
        out.print(">\n");
        out.print("</" + name + ">\n");
    }
}
