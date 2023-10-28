package com.android.server.compat.config;

import com.android.server.timezonedetector.ServiceConfigAccessor;
import java.io.IOException;
import javax.xml.datatype.DatatypeConfigurationException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes.dex */
public class Change {
    private String description;
    private Boolean disabled;
    private Integer enableAfterTargetSdk;
    private Integer enableSinceTargetSdk;
    private Long id;
    private Boolean loggingOnly;
    private String name;
    private Boolean overridable;
    private String value;

    public long getId() {
        Long l = this.id;
        if (l == null) {
            return 0L;
        }
        return l.longValue();
    }

    boolean hasId() {
        if (this.id == null) {
            return false;
        }
        return true;
    }

    public void setId(long id) {
        this.id = Long.valueOf(id);
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

    public boolean getDisabled() {
        Boolean bool = this.disabled;
        if (bool == null) {
            return false;
        }
        return bool.booleanValue();
    }

    boolean hasDisabled() {
        if (this.disabled == null) {
            return false;
        }
        return true;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = Boolean.valueOf(disabled);
    }

    public boolean getLoggingOnly() {
        Boolean bool = this.loggingOnly;
        if (bool == null) {
            return false;
        }
        return bool.booleanValue();
    }

    boolean hasLoggingOnly() {
        if (this.loggingOnly == null) {
            return false;
        }
        return true;
    }

    public void setLoggingOnly(boolean loggingOnly) {
        this.loggingOnly = Boolean.valueOf(loggingOnly);
    }

    public int getEnableAfterTargetSdk() {
        Integer num = this.enableAfterTargetSdk;
        if (num == null) {
            return 0;
        }
        return num.intValue();
    }

    boolean hasEnableAfterTargetSdk() {
        if (this.enableAfterTargetSdk == null) {
            return false;
        }
        return true;
    }

    public void setEnableAfterTargetSdk(int enableAfterTargetSdk) {
        this.enableAfterTargetSdk = Integer.valueOf(enableAfterTargetSdk);
    }

    public int getEnableSinceTargetSdk() {
        Integer num = this.enableSinceTargetSdk;
        if (num == null) {
            return 0;
        }
        return num.intValue();
    }

    boolean hasEnableSinceTargetSdk() {
        if (this.enableSinceTargetSdk == null) {
            return false;
        }
        return true;
    }

    public void setEnableSinceTargetSdk(int enableSinceTargetSdk) {
        this.enableSinceTargetSdk = Integer.valueOf(enableSinceTargetSdk);
    }

    public String getDescription() {
        return this.description;
    }

    boolean hasDescription() {
        if (this.description == null) {
            return false;
        }
        return true;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean getOverridable() {
        Boolean bool = this.overridable;
        if (bool == null) {
            return false;
        }
        return bool.booleanValue();
    }

    boolean hasOverridable() {
        if (this.overridable == null) {
            return false;
        }
        return true;
    }

    public void setOverridable(boolean overridable) {
        this.overridable = Boolean.valueOf(overridable);
    }

    public String getValue() {
        return this.value;
    }

    boolean hasValue() {
        if (this.value == null) {
            return false;
        }
        return true;
    }

    public void setValue(String value) {
        this.value = value;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static Change read(XmlPullParser parser) throws XmlPullParserException, IOException, DatatypeConfigurationException {
        Change instance = new Change();
        String raw = parser.getAttributeValue(null, "id");
        if (raw != null) {
            long value = Long.parseLong(raw);
            instance.setId(value);
        }
        String raw2 = parser.getAttributeValue(null, "name");
        if (raw2 != null) {
            instance.setName(raw2);
        }
        String raw3 = parser.getAttributeValue(null, ServiceConfigAccessor.PROVIDER_MODE_DISABLED);
        if (raw3 != null) {
            boolean value2 = Boolean.parseBoolean(raw3);
            instance.setDisabled(value2);
        }
        String raw4 = parser.getAttributeValue(null, "loggingOnly");
        if (raw4 != null) {
            boolean value3 = Boolean.parseBoolean(raw4);
            instance.setLoggingOnly(value3);
        }
        String raw5 = parser.getAttributeValue(null, "enableAfterTargetSdk");
        if (raw5 != null) {
            int value4 = Integer.parseInt(raw5);
            instance.setEnableAfterTargetSdk(value4);
        }
        String raw6 = parser.getAttributeValue(null, "enableSinceTargetSdk");
        if (raw6 != null) {
            int value5 = Integer.parseInt(raw6);
            instance.setEnableSinceTargetSdk(value5);
        }
        String raw7 = parser.getAttributeValue(null, "description");
        if (raw7 != null) {
            instance.setDescription(raw7);
        }
        String raw8 = parser.getAttributeValue(null, "overridable");
        if (raw8 != null) {
            boolean value6 = Boolean.parseBoolean(raw8);
            instance.setOverridable(value6);
        }
        String raw9 = XmlParser.readText(parser);
        if (raw9 != null) {
            instance.setValue(raw9);
        }
        return instance;
    }
}
