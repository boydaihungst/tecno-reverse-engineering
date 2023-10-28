package com.android.server.location.settings;

import android.content.res.Resources;
import com.android.server.location.settings.SettingsStore;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Objects;
/* loaded from: classes.dex */
public final class LocationUserSettings implements SettingsStore.VersionedSettings {
    private static final int VERSION = 1;
    private final boolean mAdasGnssLocationEnabled;

    private LocationUserSettings(boolean adasGnssLocationEnabled) {
        this.mAdasGnssLocationEnabled = adasGnssLocationEnabled;
    }

    @Override // com.android.server.location.settings.SettingsStore.VersionedSettings
    public int getVersion() {
        return 1;
    }

    public boolean isAdasGnssLocationEnabled() {
        return this.mAdasGnssLocationEnabled;
    }

    public LocationUserSettings withAdasGnssLocationEnabled(boolean adasEnabled) {
        if (adasEnabled == this.mAdasGnssLocationEnabled) {
            return this;
        }
        return new LocationUserSettings(adasEnabled);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void write(DataOutput out) throws IOException {
        out.writeBoolean(this.mAdasGnssLocationEnabled);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static LocationUserSettings read(Resources resources, int version, DataInput in) throws IOException {
        boolean adasGnssLocationEnabled;
        switch (version) {
            case 1:
                adasGnssLocationEnabled = in.readBoolean();
                break;
            default:
                adasGnssLocationEnabled = resources.getBoolean(17891583);
                break;
        }
        return new LocationUserSettings(adasGnssLocationEnabled);
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof LocationUserSettings) {
            LocationUserSettings that = (LocationUserSettings) o;
            return this.mAdasGnssLocationEnabled == that.mAdasGnssLocationEnabled;
        }
        return false;
    }

    public int hashCode() {
        return Objects.hash(Boolean.valueOf(this.mAdasGnssLocationEnabled));
    }
}
