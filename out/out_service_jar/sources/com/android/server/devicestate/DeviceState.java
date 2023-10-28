package com.android.server.devicestate;

import com.android.internal.util.Preconditions;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;
/* loaded from: classes.dex */
public final class DeviceState {
    public static final int FLAG_APP_INACCESSIBLE = 2;
    public static final int FLAG_CANCEL_OVERRIDE_REQUESTS = 1;
    private final int mFlags;
    private final int mIdentifier;
    private final String mName;

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes.dex */
    public @interface DeviceStateFlags {
    }

    public DeviceState(int identifier, String name, int flags) {
        Preconditions.checkArgumentInRange(identifier, 0, 255, "identifier");
        this.mIdentifier = identifier;
        this.mName = name;
        this.mFlags = flags;
    }

    public int getIdentifier() {
        return this.mIdentifier;
    }

    public String getName() {
        return this.mName;
    }

    public int getFlags() {
        return this.mFlags;
    }

    public String toString() {
        return "DeviceState{identifier=" + this.mIdentifier + ", name='" + this.mName + "', app_accessible=" + (!hasFlag(2)) + "}";
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DeviceState that = (DeviceState) o;
        if (this.mIdentifier == that.mIdentifier && Objects.equals(this.mName, that.mName) && this.mFlags == that.mFlags) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        return Objects.hash(Integer.valueOf(this.mIdentifier), this.mName, Integer.valueOf(this.mFlags));
    }

    public boolean hasFlag(int flagToCheckFor) {
        return (this.mFlags & flagToCheckFor) == flagToCheckFor;
    }
}
