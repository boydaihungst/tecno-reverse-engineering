package com.android.server.compat.overrides;

import com.android.server.pm.verify.domain.DomainVerificationLegacySettings;
import com.android.server.timezonedetector.ServiceConfigAccessor;
import java.io.IOException;
import javax.xml.datatype.DatatypeConfigurationException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes.dex */
public class OverrideValue {
    private Boolean enabled;
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
    public static OverrideValue read(XmlPullParser parser) throws XmlPullParserException, IOException, DatatypeConfigurationException {
        OverrideValue instance = new OverrideValue();
        String raw = parser.getAttributeValue(null, DomainVerificationLegacySettings.ATTR_PACKAGE_NAME);
        if (raw != null) {
            instance.setPackageName(raw);
        }
        String raw2 = parser.getAttributeValue(null, ServiceConfigAccessor.PROVIDER_MODE_ENABLED);
        if (raw2 != null) {
            boolean value = Boolean.parseBoolean(raw2);
            instance.setEnabled(value);
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
        if (hasEnabled()) {
            out.print(" enabled=\"");
            out.print(Boolean.toString(getEnabled()));
            out.print("\"");
        }
        out.print(">\n");
        out.print("</" + name + ">\n");
    }
}
