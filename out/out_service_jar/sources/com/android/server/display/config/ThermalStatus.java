package com.android.server.display.config;
/* loaded from: classes.dex */
public enum ThermalStatus {
    none("none"),
    light("light"),
    moderate("moderate"),
    severe("severe"),
    critical("critical"),
    emergency("emergency"),
    shutdown("shutdown");
    
    private final String rawName;

    ThermalStatus(String rawName) {
        this.rawName = rawName;
    }

    public String getRawName() {
        return this.rawName;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static ThermalStatus fromString(String rawString) {
        ThermalStatus[] values;
        for (ThermalStatus _f : values()) {
            if (_f.getRawName().equals(rawString)) {
                return _f;
            }
        }
        throw new IllegalArgumentException(rawString);
    }
}
