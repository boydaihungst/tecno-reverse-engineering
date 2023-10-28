package android.app.admin;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.ArrayMap;
import android.util.ArraySet;
import java.util.Map;
import java.util.Set;
/* loaded from: classes.dex */
public class ParcelableGranteeMap implements Parcelable {
    public static final Parcelable.Creator<ParcelableGranteeMap> CREATOR = new Parcelable.Creator<ParcelableGranteeMap>() { // from class: android.app.admin.ParcelableGranteeMap.1
        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public ParcelableGranteeMap createFromParcel(Parcel source) {
            Map<Integer, Set<String>> packagesByUid = new ArrayMap<>();
            int numUids = source.readInt();
            for (int i = 0; i < numUids; i++) {
                int uid = source.readInt();
                String[] pkgs = source.readStringArray();
                packagesByUid.put(Integer.valueOf(uid), new ArraySet<>(pkgs));
            }
            return new ParcelableGranteeMap(packagesByUid);
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public ParcelableGranteeMap[] newArray(int size) {
            return new ParcelableGranteeMap[size];
        }
    };
    private final Map<Integer, Set<String>> mPackagesByUid;

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.mPackagesByUid.size());
        for (Map.Entry<Integer, Set<String>> uidEntry : this.mPackagesByUid.entrySet()) {
            dest.writeInt(uidEntry.getKey().intValue());
            dest.writeStringArray((String[]) uidEntry.getValue().toArray(new String[0]));
        }
    }

    public ParcelableGranteeMap(Map<Integer, Set<String>> packagesByUid) {
        this.mPackagesByUid = packagesByUid;
    }

    public Map<Integer, Set<String>> getPackagesByUid() {
        return this.mPackagesByUid;
    }
}
