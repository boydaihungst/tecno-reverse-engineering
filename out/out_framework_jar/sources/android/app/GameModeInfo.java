package android.app;

import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;
@SystemApi
/* loaded from: classes.dex */
public final class GameModeInfo implements Parcelable {
    public static final Parcelable.Creator<GameModeInfo> CREATOR = new Parcelable.Creator<GameModeInfo>() { // from class: android.app.GameModeInfo.1
        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public GameModeInfo createFromParcel(Parcel in) {
            return new GameModeInfo(in);
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public GameModeInfo[] newArray(int size) {
            return new GameModeInfo[size];
        }
    };
    private final int mActiveGameMode;
    private final int[] mAvailableGameModes;

    public GameModeInfo(int activeGameMode, int[] availableGameModes) {
        this.mActiveGameMode = activeGameMode;
        this.mAvailableGameModes = availableGameModes;
    }

    GameModeInfo(Parcel in) {
        this.mActiveGameMode = in.readInt();
        int availableGameModesCount = in.readInt();
        int[] iArr = new int[availableGameModesCount];
        this.mAvailableGameModes = iArr;
        in.readIntArray(iArr);
    }

    public int getActiveGameMode() {
        return this.mActiveGameMode;
    }

    public int[] getAvailableGameModes() {
        return this.mAvailableGameModes;
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.mActiveGameMode);
        dest.writeInt(this.mAvailableGameModes.length);
        dest.writeIntArray(this.mAvailableGameModes);
    }
}
