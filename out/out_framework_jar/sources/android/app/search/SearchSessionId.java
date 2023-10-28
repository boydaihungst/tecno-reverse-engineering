package android.app.search;

import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;
import java.util.Objects;
@SystemApi
/* loaded from: classes.dex */
public final class SearchSessionId implements Parcelable {
    public static final Parcelable.Creator<SearchSessionId> CREATOR = new Parcelable.Creator<SearchSessionId>() { // from class: android.app.search.SearchSessionId.1
        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public SearchSessionId createFromParcel(Parcel parcel) {
            return new SearchSessionId(parcel);
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public SearchSessionId[] newArray(int size) {
            return new SearchSessionId[size];
        }
    };
    private final String mId;
    private final int mUserId;

    public SearchSessionId(String id, int userId) {
        this.mId = id;
        this.mUserId = userId;
    }

    private SearchSessionId(Parcel p) {
        this.mId = p.readString();
        this.mUserId = p.readInt();
    }

    public int getUserId() {
        return this.mUserId;
    }

    public boolean equals(Object o) {
        if (getClass().equals(o != null ? o.getClass() : null)) {
            SearchSessionId other = (SearchSessionId) o;
            return this.mId.equals(other.mId) && this.mUserId == other.mUserId;
        }
        return false;
    }

    public String toString() {
        return this.mId + "," + this.mUserId;
    }

    public int hashCode() {
        return Objects.hash(this.mId, Integer.valueOf(this.mUserId));
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.mId);
        dest.writeInt(this.mUserId);
    }
}
