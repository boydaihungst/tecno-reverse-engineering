package android.net.vcn;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.PersistableBundle;
import android.util.ArraySet;
import com.android.internal.util.Preconditions;
import com.android.server.vcn.repackaged.util.PersistableBundleUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
/* loaded from: classes2.dex */
public final class VcnConfig implements Parcelable {
    private static final String GATEWAY_CONNECTION_CONFIGS_KEY = "mGatewayConnectionConfigs";
    private static final String IS_TEST_MODE_PROFILE_KEY = "mIsTestModeProfile";
    private static final String PACKAGE_NAME_KEY = "mPackageName";
    private final Set<VcnGatewayConnectionConfig> mGatewayConnectionConfigs;
    private final boolean mIsTestModeProfile;
    private final String mPackageName;
    private static final String TAG = VcnConfig.class.getSimpleName();
    public static final Parcelable.Creator<VcnConfig> CREATOR = new Parcelable.Creator<VcnConfig>() { // from class: android.net.vcn.VcnConfig.1
        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public VcnConfig createFromParcel(Parcel in) {
            return new VcnConfig((PersistableBundle) in.readParcelable(null, PersistableBundle.class));
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public VcnConfig[] newArray(int size) {
            return new VcnConfig[size];
        }
    };

    private VcnConfig(String packageName, Set<VcnGatewayConnectionConfig> gatewayConnectionConfigs, boolean isTestModeProfile) {
        this.mPackageName = packageName;
        this.mGatewayConnectionConfigs = Collections.unmodifiableSet(new ArraySet(gatewayConnectionConfigs));
        this.mIsTestModeProfile = isTestModeProfile;
        validate();
    }

    public VcnConfig(PersistableBundle in) {
        this.mPackageName = in.getString(PACKAGE_NAME_KEY);
        PersistableBundle gatewayConnectionConfigsBundle = in.getPersistableBundle(GATEWAY_CONNECTION_CONFIGS_KEY);
        this.mGatewayConnectionConfigs = new ArraySet(PersistableBundleUtils.toList(gatewayConnectionConfigsBundle, new PersistableBundleUtils.Deserializer() { // from class: android.net.vcn.VcnConfig$$ExternalSyntheticLambda1
            @Override // com.android.server.vcn.repackaged.util.PersistableBundleUtils.Deserializer
            public final Object fromPersistableBundle(PersistableBundle persistableBundle) {
                return new VcnGatewayConnectionConfig(persistableBundle);
            }
        }));
        this.mIsTestModeProfile = in.getBoolean(IS_TEST_MODE_PROFILE_KEY);
        validate();
    }

    private void validate() {
        Objects.requireNonNull(this.mPackageName, "packageName was null");
        Preconditions.checkCollectionNotEmpty(this.mGatewayConnectionConfigs, "gatewayConnectionConfigs was empty");
    }

    public String getProvisioningPackageName() {
        return this.mPackageName;
    }

    public Set<VcnGatewayConnectionConfig> getGatewayConnectionConfigs() {
        return Collections.unmodifiableSet(this.mGatewayConnectionConfigs);
    }

    public boolean isTestModeProfile() {
        return this.mIsTestModeProfile;
    }

    public PersistableBundle toPersistableBundle() {
        PersistableBundle result = new PersistableBundle();
        result.putString(PACKAGE_NAME_KEY, this.mPackageName);
        PersistableBundle gatewayConnectionConfigsBundle = PersistableBundleUtils.fromList(new ArrayList(this.mGatewayConnectionConfigs), new PersistableBundleUtils.Serializer() { // from class: android.net.vcn.VcnConfig$$ExternalSyntheticLambda0
            @Override // com.android.server.vcn.repackaged.util.PersistableBundleUtils.Serializer
            public final PersistableBundle toPersistableBundle(Object obj) {
                return ((VcnGatewayConnectionConfig) obj).toPersistableBundle();
            }
        });
        result.putPersistableBundle(GATEWAY_CONNECTION_CONFIGS_KEY, gatewayConnectionConfigsBundle);
        result.putBoolean(IS_TEST_MODE_PROFILE_KEY, this.mIsTestModeProfile);
        return result;
    }

    public int hashCode() {
        return Objects.hash(this.mPackageName, this.mGatewayConnectionConfigs, Boolean.valueOf(this.mIsTestModeProfile));
    }

    public boolean equals(Object other) {
        if (other instanceof VcnConfig) {
            VcnConfig rhs = (VcnConfig) other;
            return this.mPackageName.equals(rhs.mPackageName) && this.mGatewayConnectionConfigs.equals(rhs.mGatewayConnectionConfigs) && this.mIsTestModeProfile == rhs.mIsTestModeProfile;
        }
        return false;
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel out, int flags) {
        out.writeParcelable(toPersistableBundle(), flags);
    }

    /* loaded from: classes2.dex */
    public static final class Builder {
        private final Set<VcnGatewayConnectionConfig> mGatewayConnectionConfigs = new ArraySet();
        private boolean mIsTestModeProfile = false;
        private final String mPackageName;

        public Builder(Context context) {
            Objects.requireNonNull(context, "context was null");
            this.mPackageName = context.getOpPackageName();
        }

        public Builder addGatewayConnectionConfig(VcnGatewayConnectionConfig gatewayConnectionConfig) {
            Objects.requireNonNull(gatewayConnectionConfig, "gatewayConnectionConfig was null");
            for (VcnGatewayConnectionConfig vcnGatewayConnectionConfig : this.mGatewayConnectionConfigs) {
                if (vcnGatewayConnectionConfig.getGatewayConnectionName().equals(gatewayConnectionConfig.getGatewayConnectionName())) {
                    throw new IllegalArgumentException("GatewayConnection for specified name already exists");
                }
            }
            this.mGatewayConnectionConfigs.add(gatewayConnectionConfig);
            return this;
        }

        public Builder setIsTestModeProfile() {
            this.mIsTestModeProfile = true;
            return this;
        }

        public VcnConfig build() {
            return new VcnConfig(this.mPackageName, this.mGatewayConnectionConfigs, this.mIsTestModeProfile);
        }
    }
}
