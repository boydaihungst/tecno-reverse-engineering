package android.companion;

import android.os.Parcel;
import android.os.Parcelable;
import android.provider.OneTimeUseBuilder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
/* loaded from: classes.dex */
public final class SystemDataTransferRequest implements Parcelable {
    public static final Parcelable.Creator<SystemDataTransferRequest> CREATOR = new Parcelable.Creator<SystemDataTransferRequest>() { // from class: android.companion.SystemDataTransferRequest.1
        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public SystemDataTransferRequest createFromParcel(Parcel in) {
            return new SystemDataTransferRequest(in);
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public SystemDataTransferRequest[] newArray(int size) {
            return new SystemDataTransferRequest[size];
        }
    };
    private final int mAssociationId;
    private final boolean mPermissionSyncAllPackages;
    private final List<String> mPermissionSyncPackages;

    public SystemDataTransferRequest(int associationId, boolean syncAllPackages, List<String> permissionSyncPackages) {
        this.mAssociationId = associationId;
        this.mPermissionSyncAllPackages = syncAllPackages;
        this.mPermissionSyncPackages = permissionSyncPackages;
    }

    public int getAssociationId() {
        return this.mAssociationId;
    }

    public boolean isPermissionSyncAllPackages() {
        return this.mPermissionSyncAllPackages;
    }

    public List<String> getPermissionSyncPackages() {
        return this.mPermissionSyncPackages;
    }

    /* loaded from: classes.dex */
    public static final class Builder extends OneTimeUseBuilder<SystemDataTransferRequest> {
        private final int mAssociationId;
        private boolean mPermissionSyncAllPackages;
        private List<String> mPermissionSyncPackages = new ArrayList();

        public Builder(int associationId) {
            this.mAssociationId = associationId;
        }

        public Builder setPermissionSyncAllPackages() {
            this.mPermissionSyncAllPackages = true;
            return this;
        }

        public Builder setPermissionSyncPackages(List<String> permissionSyncPackages) {
            this.mPermissionSyncPackages = permissionSyncPackages;
            return this;
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.provider.OneTimeUseBuilder
        public SystemDataTransferRequest build() {
            return new SystemDataTransferRequest(this.mAssociationId, this.mPermissionSyncAllPackages, this.mPermissionSyncPackages);
        }
    }

    SystemDataTransferRequest(Parcel in) {
        this.mAssociationId = in.readInt();
        this.mPermissionSyncAllPackages = in.readBoolean();
        this.mPermissionSyncPackages = Arrays.asList(in.createString8Array());
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.mAssociationId);
        dest.writeBoolean(this.mPermissionSyncAllPackages);
        dest.writeString8Array((String[]) this.mPermissionSyncPackages.toArray(new String[0]));
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }
}
