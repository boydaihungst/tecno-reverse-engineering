package android.service.games;

import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;
/* loaded from: classes3.dex */
public final class GameSessionActivityResult implements Parcelable {
    public static final Parcelable.Creator<GameSessionActivityResult> CREATOR = new Parcelable.Creator<GameSessionActivityResult>() { // from class: android.service.games.GameSessionActivityResult.1
        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public GameSessionActivityResult createFromParcel(Parcel in) {
            int resultCode = in.readInt();
            Intent data = (Intent) in.readParcelable(Intent.class.getClassLoader(), Intent.class);
            return new GameSessionActivityResult(resultCode, data);
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public GameSessionActivityResult[] newArray(int size) {
            return new GameSessionActivityResult[size];
        }
    };
    private final Intent mData;
    private final int mResultCode;

    public GameSessionActivityResult(int resultCode, Intent data) {
        this.mResultCode = resultCode;
        this.mData = data;
    }

    public int getResultCode() {
        return this.mResultCode;
    }

    public Intent getData() {
        return this.mData;
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.mResultCode);
        dest.writeParcelable(this.mData, flags);
    }
}
