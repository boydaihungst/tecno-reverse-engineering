package com.android.apex;

import java.io.IOException;
import javax.xml.datatype.DatatypeConfigurationException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes4.dex */
public class ApexInfo {
    private Boolean isActive;
    private Boolean isFactory;
    private Long lastUpdateMillis;
    private String moduleName;
    private String modulePath;
    private String preinstalledModulePath;
    private Boolean provideSharedApexLibs;
    private Long versionCode;
    private String versionName;

    public String getModuleName() {
        return this.moduleName;
    }

    boolean hasModuleName() {
        if (this.moduleName == null) {
            return false;
        }
        return true;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    public String getModulePath() {
        return this.modulePath;
    }

    boolean hasModulePath() {
        if (this.modulePath == null) {
            return false;
        }
        return true;
    }

    public void setModulePath(String modulePath) {
        this.modulePath = modulePath;
    }

    public String getPreinstalledModulePath() {
        return this.preinstalledModulePath;
    }

    boolean hasPreinstalledModulePath() {
        if (this.preinstalledModulePath == null) {
            return false;
        }
        return true;
    }

    public void setPreinstalledModulePath(String preinstalledModulePath) {
        this.preinstalledModulePath = preinstalledModulePath;
    }

    public long getVersionCode() {
        Long l = this.versionCode;
        if (l == null) {
            return 0L;
        }
        return l.longValue();
    }

    boolean hasVersionCode() {
        if (this.versionCode == null) {
            return false;
        }
        return true;
    }

    public void setVersionCode(long versionCode) {
        this.versionCode = Long.valueOf(versionCode);
    }

    public String getVersionName() {
        return this.versionName;
    }

    boolean hasVersionName() {
        if (this.versionName == null) {
            return false;
        }
        return true;
    }

    public void setVersionName(String versionName) {
        this.versionName = versionName;
    }

    public boolean getIsFactory() {
        Boolean bool = this.isFactory;
        if (bool == null) {
            return false;
        }
        return bool.booleanValue();
    }

    boolean hasIsFactory() {
        if (this.isFactory == null) {
            return false;
        }
        return true;
    }

    public void setIsFactory(boolean isFactory) {
        this.isFactory = Boolean.valueOf(isFactory);
    }

    public boolean getIsActive() {
        Boolean bool = this.isActive;
        if (bool == null) {
            return false;
        }
        return bool.booleanValue();
    }

    boolean hasIsActive() {
        if (this.isActive == null) {
            return false;
        }
        return true;
    }

    public void setIsActive(boolean isActive) {
        this.isActive = Boolean.valueOf(isActive);
    }

    public long getLastUpdateMillis() {
        Long l = this.lastUpdateMillis;
        if (l == null) {
            return 0L;
        }
        return l.longValue();
    }

    boolean hasLastUpdateMillis() {
        if (this.lastUpdateMillis == null) {
            return false;
        }
        return true;
    }

    public void setLastUpdateMillis(long lastUpdateMillis) {
        this.lastUpdateMillis = Long.valueOf(lastUpdateMillis);
    }

    public boolean getProvideSharedApexLibs() {
        Boolean bool = this.provideSharedApexLibs;
        if (bool == null) {
            return false;
        }
        return bool.booleanValue();
    }

    boolean hasProvideSharedApexLibs() {
        if (this.provideSharedApexLibs == null) {
            return false;
        }
        return true;
    }

    public void setProvideSharedApexLibs(boolean provideSharedApexLibs) {
        this.provideSharedApexLibs = Boolean.valueOf(provideSharedApexLibs);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static ApexInfo read(XmlPullParser parser) throws XmlPullParserException, IOException, DatatypeConfigurationException {
        ApexInfo instance = new ApexInfo();
        String raw = parser.getAttributeValue(null, "moduleName");
        if (raw != null) {
            instance.setModuleName(raw);
        }
        String raw2 = parser.getAttributeValue(null, "modulePath");
        if (raw2 != null) {
            instance.setModulePath(raw2);
        }
        String raw3 = parser.getAttributeValue(null, "preinstalledModulePath");
        if (raw3 != null) {
            instance.setPreinstalledModulePath(raw3);
        }
        String raw4 = parser.getAttributeValue(null, "versionCode");
        if (raw4 != null) {
            long value = Long.parseLong(raw4);
            instance.setVersionCode(value);
        }
        String raw5 = parser.getAttributeValue(null, "versionName");
        if (raw5 != null) {
            instance.setVersionName(raw5);
        }
        String raw6 = parser.getAttributeValue(null, "isFactory");
        if (raw6 != null) {
            boolean value2 = Boolean.parseBoolean(raw6);
            instance.setIsFactory(value2);
        }
        String raw7 = parser.getAttributeValue(null, "isActive");
        if (raw7 != null) {
            boolean value3 = Boolean.parseBoolean(raw7);
            instance.setIsActive(value3);
        }
        String raw8 = parser.getAttributeValue(null, "lastUpdateMillis");
        if (raw8 != null) {
            long value4 = Long.parseLong(raw8);
            instance.setLastUpdateMillis(value4);
        }
        String raw9 = parser.getAttributeValue(null, "provideSharedApexLibs");
        if (raw9 != null) {
            boolean value5 = Boolean.parseBoolean(raw9);
            instance.setProvideSharedApexLibs(value5);
        }
        XmlParser.skip(parser);
        return instance;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void write(XmlWriter out, String name) throws IOException {
        out.print("<" + name);
        if (hasModuleName()) {
            out.print(" moduleName=\"");
            out.print(getModuleName());
            out.print("\"");
        }
        if (hasModulePath()) {
            out.print(" modulePath=\"");
            out.print(getModulePath());
            out.print("\"");
        }
        if (hasPreinstalledModulePath()) {
            out.print(" preinstalledModulePath=\"");
            out.print(getPreinstalledModulePath());
            out.print("\"");
        }
        if (hasVersionCode()) {
            out.print(" versionCode=\"");
            out.print(Long.toString(getVersionCode()));
            out.print("\"");
        }
        if (hasVersionName()) {
            out.print(" versionName=\"");
            out.print(getVersionName());
            out.print("\"");
        }
        if (hasIsFactory()) {
            out.print(" isFactory=\"");
            out.print(Boolean.toString(getIsFactory()));
            out.print("\"");
        }
        if (hasIsActive()) {
            out.print(" isActive=\"");
            out.print(Boolean.toString(getIsActive()));
            out.print("\"");
        }
        if (hasLastUpdateMillis()) {
            out.print(" lastUpdateMillis=\"");
            out.print(Long.toString(getLastUpdateMillis()));
            out.print("\"");
        }
        if (hasProvideSharedApexLibs()) {
            out.print(" provideSharedApexLibs=\"");
            out.print(Boolean.toString(getProvideSharedApexLibs()));
            out.print("\"");
        }
        out.print(">\n");
        out.print("</" + name + ">\n");
    }
}
