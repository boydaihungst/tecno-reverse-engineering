package android.service.notification;

import android.os.Parcel;
import android.os.Parcelable;
import android.service.notification.NotificationListenerService;
/* loaded from: classes3.dex */
public class NotificationRankingUpdate implements Parcelable {
    public static final Parcelable.Creator<NotificationRankingUpdate> CREATOR = new Parcelable.Creator<NotificationRankingUpdate>() { // from class: android.service.notification.NotificationRankingUpdate.1
        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public NotificationRankingUpdate createFromParcel(Parcel parcel) {
            return new NotificationRankingUpdate(parcel);
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public NotificationRankingUpdate[] newArray(int size) {
            return new NotificationRankingUpdate[size];
        }
    };
    private final NotificationListenerService.RankingMap mRankingMap;

    public NotificationRankingUpdate(NotificationListenerService.Ranking[] rankings) {
        this.mRankingMap = new NotificationListenerService.RankingMap(rankings);
    }

    public NotificationRankingUpdate(Parcel in) {
        this.mRankingMap = (NotificationListenerService.RankingMap) in.readParcelable(getClass().getClassLoader(), NotificationListenerService.RankingMap.class);
    }

    public NotificationListenerService.RankingMap getRankingMap() {
        return this.mRankingMap;
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        NotificationRankingUpdate other = (NotificationRankingUpdate) o;
        return this.mRankingMap.equals(other.mRankingMap);
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel out, int flags) {
        out.writeParcelable(this.mRankingMap, flags);
    }
}
