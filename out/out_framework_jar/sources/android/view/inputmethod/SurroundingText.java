package android.view.inputmethod;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
/* loaded from: classes3.dex */
public final class SurroundingText implements Parcelable {
    public static final Parcelable.Creator<SurroundingText> CREATOR = new Parcelable.Creator<SurroundingText>() { // from class: android.view.inputmethod.SurroundingText.1
        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public SurroundingText createFromParcel(Parcel in) {
            CharSequence text = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in);
            int selectionHead = in.readInt();
            int selectionEnd = in.readInt();
            int offset = in.readInt();
            return new SurroundingText(text == null ? "" : text, selectionHead, selectionEnd, offset);
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public SurroundingText[] newArray(int size) {
            return new SurroundingText[size];
        }
    };
    private final int mOffset;
    private final int mSelectionEnd;
    private final int mSelectionStart;
    private final CharSequence mText;

    public SurroundingText(CharSequence text, int selectionStart, int selectionEnd, int offset) {
        this.mText = copyWithParcelableSpans(text);
        this.mSelectionStart = selectionStart;
        this.mSelectionEnd = selectionEnd;
        this.mOffset = offset;
    }

    public CharSequence getText() {
        return this.mText;
    }

    public int getSelectionStart() {
        return this.mSelectionStart;
    }

    public int getSelectionEnd() {
        return this.mSelectionEnd;
    }

    public int getOffset() {
        return this.mOffset;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel out, int flags) {
        TextUtils.writeToParcel(this.mText, out, flags);
        out.writeInt(this.mSelectionStart);
        out.writeInt(this.mSelectionEnd);
        out.writeInt(this.mOffset);
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    private static CharSequence copyWithParcelableSpans(CharSequence source) {
        if (source == null) {
            return null;
        }
        Parcel parcel = null;
        try {
            parcel = Parcel.obtain();
            TextUtils.writeToParcel(source, parcel, 0);
            parcel.setDataPosition(0);
            return TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(parcel);
        } finally {
            if (parcel != null) {
                parcel.recycle();
            }
        }
    }
}
