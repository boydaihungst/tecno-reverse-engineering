package android.service.quickaccesswallet;

import android.app.PendingIntent;
import android.graphics.drawable.Icon;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
/* loaded from: classes3.dex */
public final class WalletCard implements Parcelable {
    public static final Parcelable.Creator<WalletCard> CREATOR = new Parcelable.Creator<WalletCard>() { // from class: android.service.quickaccesswallet.WalletCard.1
        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public WalletCard createFromParcel(Parcel source) {
            return WalletCard.readFromParcel(source);
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public WalletCard[] newArray(int size) {
            return new WalletCard[size];
        }
    };
    private final Icon mCardIcon;
    private final String mCardId;
    private final Icon mCardImage;
    private final CharSequence mCardLabel;
    private final CharSequence mContentDescription;
    private final PendingIntent mPendingIntent;

    private WalletCard(Builder builder) {
        this.mCardId = builder.mCardId;
        this.mCardImage = builder.mCardImage;
        this.mContentDescription = builder.mContentDescription;
        this.mPendingIntent = builder.mPendingIntent;
        this.mCardIcon = builder.mCardIcon;
        this.mCardLabel = builder.mCardLabel;
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.mCardId);
        this.mCardImage.writeToParcel(dest, flags);
        TextUtils.writeToParcel(this.mContentDescription, dest, flags);
        PendingIntent.writePendingIntentOrNullToParcel(this.mPendingIntent, dest);
        if (this.mCardIcon == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            this.mCardIcon.writeToParcel(dest, flags);
        }
        TextUtils.writeToParcel(this.mCardLabel, dest, flags);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static WalletCard readFromParcel(Parcel source) {
        String cardId = source.readString();
        Icon cardImage = Icon.CREATOR.createFromParcel(source);
        CharSequence contentDesc = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(source);
        PendingIntent pendingIntent = PendingIntent.readPendingIntentOrNullFromParcel(source);
        Icon cardIcon = source.readByte() == 0 ? null : Icon.CREATOR.createFromParcel(source);
        CharSequence cardLabel = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(source);
        return new Builder(cardId, cardImage, contentDesc, pendingIntent).setCardIcon(cardIcon).setCardLabel(cardLabel).build();
    }

    public String getCardId() {
        return this.mCardId;
    }

    public Icon getCardImage() {
        return this.mCardImage;
    }

    public CharSequence getContentDescription() {
        return this.mContentDescription;
    }

    public PendingIntent getPendingIntent() {
        return this.mPendingIntent;
    }

    public Icon getCardIcon() {
        return this.mCardIcon;
    }

    public CharSequence getCardLabel() {
        return this.mCardLabel;
    }

    /* loaded from: classes3.dex */
    public static final class Builder {
        private Icon mCardIcon;
        private String mCardId;
        private Icon mCardImage;
        private CharSequence mCardLabel;
        private CharSequence mContentDescription;
        private PendingIntent mPendingIntent;

        public Builder(String cardId, Icon cardImage, CharSequence contentDescription, PendingIntent pendingIntent) {
            this.mCardId = cardId;
            this.mCardImage = cardImage;
            this.mContentDescription = contentDescription;
            this.mPendingIntent = pendingIntent;
        }

        public Builder setCardIcon(Icon cardIcon) {
            this.mCardIcon = cardIcon;
            return this;
        }

        public Builder setCardLabel(CharSequence cardLabel) {
            this.mCardLabel = cardLabel;
            return this;
        }

        public WalletCard build() {
            return new WalletCard(this);
        }
    }
}
