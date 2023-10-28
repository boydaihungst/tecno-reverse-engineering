package android.view.textclassifier;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.Locale;
import java.util.Objects;
/* loaded from: classes3.dex */
public final class SystemTextClassifierMetadata implements Parcelable {
    public static final Parcelable.Creator<SystemTextClassifierMetadata> CREATOR = new Parcelable.Creator<SystemTextClassifierMetadata>() { // from class: android.view.textclassifier.SystemTextClassifierMetadata.1
        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public SystemTextClassifierMetadata createFromParcel(Parcel in) {
            return SystemTextClassifierMetadata.readFromParcel(in);
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public SystemTextClassifierMetadata[] newArray(int size) {
            return new SystemTextClassifierMetadata[size];
        }
    };
    private final String mCallingPackageName;
    private final boolean mUseDefaultTextClassifier;
    private final int mUserId;

    public SystemTextClassifierMetadata(String packageName, int userId, boolean useDefaultTextClassifier) {
        Objects.requireNonNull(packageName);
        this.mCallingPackageName = packageName;
        this.mUserId = userId;
        this.mUseDefaultTextClassifier = useDefaultTextClassifier;
    }

    public int getUserId() {
        return this.mUserId;
    }

    public String getCallingPackageName() {
        return this.mCallingPackageName;
    }

    public boolean useDefaultTextClassifier() {
        return this.mUseDefaultTextClassifier;
    }

    public String toString() {
        return String.format(Locale.US, "SystemTextClassifierMetadata {callingPackageName=%s, userId=%d, useDefaultTextClassifier=%b}", this.mCallingPackageName, Integer.valueOf(this.mUserId), Boolean.valueOf(this.mUseDefaultTextClassifier));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static SystemTextClassifierMetadata readFromParcel(Parcel in) {
        String packageName = in.readString();
        int userId = in.readInt();
        boolean useDefaultTextClassifier = in.readBoolean();
        return new SystemTextClassifierMetadata(packageName, userId, useDefaultTextClassifier);
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.mCallingPackageName);
        dest.writeInt(this.mUserId);
        dest.writeBoolean(this.mUseDefaultTextClassifier);
    }
}
