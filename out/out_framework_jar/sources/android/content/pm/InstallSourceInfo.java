package android.content.pm;

import android.os.Parcel;
import android.os.Parcelable;
/* loaded from: classes.dex */
public final class InstallSourceInfo implements Parcelable {
    public static final Parcelable.Creator<InstallSourceInfo> CREATOR = new Parcelable.Creator<InstallSourceInfo>() { // from class: android.content.pm.InstallSourceInfo.1
        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public InstallSourceInfo createFromParcel(Parcel source) {
            return new InstallSourceInfo(source);
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public InstallSourceInfo[] newArray(int size) {
            return new InstallSourceInfo[size];
        }
    };
    private final String mInitiatingPackageName;
    private final SigningInfo mInitiatingPackageSigningInfo;
    private final String mInstallingPackageName;
    private final String mOriginatingPackageName;
    private final int mPackageSource;

    public InstallSourceInfo(String initiatingPackageName, SigningInfo initiatingPackageSigningInfo, String originatingPackageName, String installingPackageName) {
        this(initiatingPackageName, initiatingPackageSigningInfo, originatingPackageName, installingPackageName, 0);
    }

    public InstallSourceInfo(String initiatingPackageName, SigningInfo initiatingPackageSigningInfo, String originatingPackageName, String installingPackageName, int packageSource) {
        this.mInitiatingPackageName = initiatingPackageName;
        this.mInitiatingPackageSigningInfo = initiatingPackageSigningInfo;
        this.mOriginatingPackageName = originatingPackageName;
        this.mInstallingPackageName = installingPackageName;
        this.mPackageSource = packageSource;
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        SigningInfo signingInfo = this.mInitiatingPackageSigningInfo;
        if (signingInfo == null) {
            return 0;
        }
        return signingInfo.describeContents();
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.mInitiatingPackageName);
        dest.writeParcelable(this.mInitiatingPackageSigningInfo, flags);
        dest.writeString(this.mOriginatingPackageName);
        dest.writeString(this.mInstallingPackageName);
        dest.writeInt(this.mPackageSource);
    }

    private InstallSourceInfo(Parcel source) {
        this.mInitiatingPackageName = source.readString();
        this.mInitiatingPackageSigningInfo = (SigningInfo) source.readParcelable(SigningInfo.class.getClassLoader(), SigningInfo.class);
        this.mOriginatingPackageName = source.readString();
        this.mInstallingPackageName = source.readString();
        this.mPackageSource = source.readInt();
    }

    public String getInitiatingPackageName() {
        return this.mInitiatingPackageName;
    }

    public SigningInfo getInitiatingPackageSigningInfo() {
        return this.mInitiatingPackageSigningInfo;
    }

    public String getOriginatingPackageName() {
        return this.mOriginatingPackageName;
    }

    public String getInstallingPackageName() {
        return this.mInstallingPackageName;
    }

    public int getPackageSource() {
        return this.mPackageSource;
    }
}
