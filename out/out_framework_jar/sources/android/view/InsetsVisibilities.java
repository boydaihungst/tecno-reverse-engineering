package android.view;

import android.os.Parcel;
import android.os.Parcelable;
import android.provider.CalendarContract;
import java.util.Arrays;
import java.util.StringJoiner;
/* loaded from: classes3.dex */
public class InsetsVisibilities implements Parcelable {
    public static final Parcelable.Creator<InsetsVisibilities> CREATOR = new Parcelable.Creator<InsetsVisibilities>() { // from class: android.view.InsetsVisibilities.1
        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public InsetsVisibilities createFromParcel(Parcel in) {
            return new InsetsVisibilities(in);
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public InsetsVisibilities[] newArray(int size) {
            return new InsetsVisibilities[size];
        }
    };
    private static final int INVISIBLE = -1;
    private static final int UNSPECIFIED = 0;
    private static final int VISIBLE = 1;
    private final int[] mVisibilities;

    public InsetsVisibilities() {
        this.mVisibilities = new int[24];
    }

    public InsetsVisibilities(InsetsVisibilities other) {
        this.mVisibilities = new int[24];
        set(other);
    }

    public InsetsVisibilities(Parcel in) {
        int[] iArr = new int[24];
        this.mVisibilities = iArr;
        in.readIntArray(iArr);
    }

    public void set(InsetsVisibilities other) {
        System.arraycopy(other.mVisibilities, 0, this.mVisibilities, 0, 24);
    }

    public void setVisibility(int type, boolean visible) {
        this.mVisibilities[type] = visible ? 1 : -1;
    }

    public boolean getVisibility(int type) {
        int visibility = this.mVisibilities[type];
        if (visibility == 0) {
            return InsetsState.getDefaultVisibility(type);
        }
        return visibility == 1;
    }

    public String toString() {
        StringJoiner joiner = new StringJoiner(", ");
        for (int type = 0; type <= 23; type++) {
            int visibility = this.mVisibilities[type];
            if (visibility != 0) {
                joiner.add(InsetsState.typeToString(type) + ": " + (visibility == 1 ? CalendarContract.CalendarColumns.VISIBLE : "invisible"));
            }
        }
        return joiner.toString();
    }

    public int hashCode() {
        return Arrays.hashCode(this.mVisibilities);
    }

    public boolean equals(Object other) {
        if (!(other instanceof InsetsVisibilities)) {
            return false;
        }
        return Arrays.equals(this.mVisibilities, ((InsetsVisibilities) other).mVisibilities);
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeIntArray(this.mVisibilities);
    }

    public void readFromParcel(Parcel in) {
        in.readIntArray(this.mVisibilities);
    }
}
