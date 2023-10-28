package android.content.pm;

import android.annotation.SystemApi;
import android.content.IntentFilter;
import android.os.Parcel;
import android.os.Parcelable;
import java.util.ArrayList;
import java.util.List;
@SystemApi
/* loaded from: classes.dex */
public final class InstantAppIntentFilter implements Parcelable {
    public static final Parcelable.Creator<InstantAppIntentFilter> CREATOR = new Parcelable.Creator<InstantAppIntentFilter>() { // from class: android.content.pm.InstantAppIntentFilter.1
        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public InstantAppIntentFilter createFromParcel(Parcel in) {
            return new InstantAppIntentFilter(in);
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public InstantAppIntentFilter[] newArray(int size) {
            return new InstantAppIntentFilter[size];
        }
    };
    private final List<IntentFilter> mFilters;
    private final String mSplitName;

    public InstantAppIntentFilter(String splitName, List<IntentFilter> filters) {
        ArrayList arrayList = new ArrayList();
        this.mFilters = arrayList;
        if (filters == null || filters.size() == 0) {
            throw new IllegalArgumentException();
        }
        this.mSplitName = splitName;
        arrayList.addAll(filters);
    }

    InstantAppIntentFilter(Parcel in) {
        ArrayList arrayList = new ArrayList();
        this.mFilters = arrayList;
        this.mSplitName = in.readString();
        in.readList(arrayList, getClass().getClassLoader(), IntentFilter.class);
    }

    public String getSplitName() {
        return this.mSplitName;
    }

    public List<IntentFilter> getFilters() {
        return this.mFilters;
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(this.mSplitName);
        out.writeList(this.mFilters);
    }
}
