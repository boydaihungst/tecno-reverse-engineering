package android.app.admin;

import android.os.Parcel;
import android.os.Parcelable;
import com.android.internal.util.Preconditions;
import java.util.Arrays;
import java.util.List;
/* loaded from: classes.dex */
public final class UnsafeStateException extends IllegalStateException implements Parcelable {
    public static final Parcelable.Creator<UnsafeStateException> CREATOR = new Parcelable.Creator<UnsafeStateException>() { // from class: android.app.admin.UnsafeStateException.1
        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public UnsafeStateException createFromParcel(Parcel source) {
            return new UnsafeStateException(source.readInt(), source.readInt());
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public UnsafeStateException[] newArray(int size) {
            return new UnsafeStateException[size];
        }
    };
    private final int mOperation;
    private final int mReason;

    public UnsafeStateException(int operation, int reason) {
        Preconditions.checkArgument(DevicePolicyManager.isValidOperationSafetyReason(reason), "invalid reason %d", Integer.valueOf(reason));
        this.mOperation = operation;
        this.mReason = reason;
    }

    public int getOperation() {
        return this.mOperation;
    }

    public List<Integer> getReasons() {
        return Arrays.asList(Integer.valueOf(this.mReason));
    }

    @Override // java.lang.Throwable
    public String getMessage() {
        return DevicePolicyManager.operationSafetyReasonToString(this.mReason);
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.mOperation);
        dest.writeInt(this.mReason);
    }
}
