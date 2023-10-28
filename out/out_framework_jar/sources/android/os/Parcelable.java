package android.os;

import android.annotation.SystemApi;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
/* loaded from: classes2.dex */
public interface Parcelable {
    public static final int CONTENTS_FILE_DESCRIPTOR = 1;
    public static final int PARCELABLE_ELIDE_DUPLICATES = 2;
    @SystemApi(client = SystemApi.Client.PRIVILEGED_APPS)
    public static final int PARCELABLE_STABILITY_LOCAL = 0;
    @SystemApi(client = SystemApi.Client.PRIVILEGED_APPS)
    public static final int PARCELABLE_STABILITY_VINTF = 1;
    public static final int PARCELABLE_WRITE_RETURN_VALUE = 1;

    /* loaded from: classes2.dex */
    public interface ClassLoaderCreator<T> extends Creator<T> {
        T createFromParcel(Parcel parcel, ClassLoader classLoader);
    }

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes2.dex */
    public @interface ContentsFlags {
    }

    /* loaded from: classes2.dex */
    public interface Creator<T> {
        T createFromParcel(Parcel parcel);

        T[] newArray(int i);
    }

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes2.dex */
    public @interface Stability {
    }

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes2.dex */
    public @interface WriteFlags {
    }

    int describeContents();

    void writeToParcel(Parcel parcel, int i);

    @SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
    default int getStability() {
        return 0;
    }
}
