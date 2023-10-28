package android.view.inputmethod;

import android.os.Bundle;
import android.os.LocaleList;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.UserHandle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Printer;
import android.util.proto.ProtoOutputStream;
import android.view.autofill.AutofillId;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.Preconditions;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.Objects;
/* loaded from: classes3.dex */
public class EditorInfo implements InputType, Parcelable {
    public static final Parcelable.Creator<EditorInfo> CREATOR = new Parcelable.Creator<EditorInfo>() { // from class: android.view.inputmethod.EditorInfo.1
        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public EditorInfo createFromParcel(Parcel source) {
            EditorInfo res = new EditorInfo();
            res.inputType = source.readInt();
            res.imeOptions = source.readInt();
            res.privateImeOptions = source.readString();
            res.internalImeOptions = source.readInt();
            res.actionLabel = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(source);
            res.actionId = source.readInt();
            res.initialSelStart = source.readInt();
            res.initialSelEnd = source.readInt();
            res.initialCapsMode = source.readInt();
            res.hintText = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(source);
            res.label = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(source);
            res.packageName = source.readString();
            res.autofillId = (AutofillId) source.readParcelable(AutofillId.class.getClassLoader(), AutofillId.class);
            res.fieldId = source.readInt();
            res.fieldName = source.readString();
            res.extras = source.readBundle();
            boolean hasInitialSurroundingText = source.readBoolean();
            if (hasInitialSurroundingText) {
                res.mInitialSurroundingText = SurroundingText.CREATOR.createFromParcel(source);
            }
            LocaleList hintLocales = LocaleList.CREATOR.createFromParcel(source);
            res.hintLocales = hintLocales.isEmpty() ? null : hintLocales;
            res.contentMimeTypes = source.readStringArray();
            res.targetInputMethodUser = UserHandle.readFromParcel(source);
            return res;
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public EditorInfo[] newArray(int size) {
            return new EditorInfo[size];
        }
    };
    public static final int IME_ACTION_DONE = 6;
    public static final int IME_ACTION_GO = 2;
    public static final int IME_ACTION_NEXT = 5;
    public static final int IME_ACTION_NONE = 1;
    public static final int IME_ACTION_PREVIOUS = 7;
    public static final int IME_ACTION_SEARCH = 3;
    public static final int IME_ACTION_SEND = 4;
    public static final int IME_ACTION_UNSPECIFIED = 0;
    public static final int IME_FLAG_FORCE_ASCII = Integer.MIN_VALUE;
    public static final int IME_FLAG_NAVIGATE_NEXT = 134217728;
    public static final int IME_FLAG_NAVIGATE_PREVIOUS = 67108864;
    public static final int IME_FLAG_NO_ACCESSORY_ACTION = 536870912;
    public static final int IME_FLAG_NO_ENTER_ACTION = 1073741824;
    public static final int IME_FLAG_NO_EXTRACT_UI = 268435456;
    public static final int IME_FLAG_NO_FULLSCREEN = 33554432;
    public static final int IME_FLAG_NO_PERSONALIZED_LEARNING = 16777216;
    public static final int IME_INTERNAL_FLAG_APP_WINDOW_PORTRAIT = 1;
    public static final int IME_MASK_ACTION = 255;
    public static final int IME_NULL = 0;
    static final int MAX_INITIAL_SELECTION_LENGTH = 1024;
    static final int MEMORY_EFFICIENT_TEXT_LENGTH = 2048;
    public AutofillId autofillId;
    public Bundle extras;
    public int fieldId;
    public String fieldName;
    public CharSequence hintText;
    public CharSequence label;
    public String packageName;
    public int inputType = 0;
    public int imeOptions = 0;
    public String privateImeOptions = null;
    public int internalImeOptions = 0;
    public CharSequence actionLabel = null;
    public int actionId = 0;
    public int initialSelStart = -1;
    public int initialSelEnd = -1;
    public int initialCapsMode = 0;
    public LocaleList hintLocales = null;
    public String[] contentMimeTypes = null;
    public UserHandle targetInputMethodUser = null;
    private SurroundingText mInitialSurroundingText = null;

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes3.dex */
    @interface TrimPolicy {
        public static final int HEAD = 0;
        public static final int TAIL = 1;
    }

    public void setInitialSurroundingText(CharSequence sourceText) {
        setInitialSurroundingSubText(sourceText, 0);
    }

    public final void setInitialSurroundingTextInternal(SurroundingText surroundingText) {
        this.mInitialSurroundingText = surroundingText;
    }

    public void setInitialSurroundingSubText(CharSequence subText, int subTextStart) {
        Objects.requireNonNull(subText);
        if (isPasswordInputType(this.inputType)) {
            this.mInitialSurroundingText = null;
            return;
        }
        int i = this.initialSelStart;
        int i2 = this.initialSelEnd;
        int subTextSelStart = i > i2 ? i2 - subTextStart : i - subTextStart;
        int subTextSelEnd = i > i2 ? i - subTextStart : i2 - subTextStart;
        int subTextLength = subText.length();
        if (subTextStart < 0 || subTextSelStart < 0 || subTextSelEnd > subTextLength) {
            this.mInitialSurroundingText = null;
        } else if (subTextLength <= 2048) {
            this.mInitialSurroundingText = new SurroundingText(subText, subTextSelStart, subTextSelEnd, subTextStart);
        } else {
            trimLongSurroundingText(subText, subTextSelStart, subTextSelEnd, subTextStart);
        }
    }

    private void trimLongSurroundingText(CharSequence subText, int selStart, int selEnd, int subTextStart) {
        CharSequence newInitialSurroundingText;
        int sourceSelLength = selEnd - selStart;
        int newSelLength = sourceSelLength > 1024 ? 0 : sourceSelLength;
        int subTextAfterCursorLength = subText.length() - selEnd;
        int maxLengthMinusSelection = 2048 - newSelLength;
        int possibleMaxBeforeCursorLength = Math.min(selStart, (int) (maxLengthMinusSelection * 0.8d));
        int newAfterCursorLength = Math.min(subTextAfterCursorLength, maxLengthMinusSelection - possibleMaxBeforeCursorLength);
        int newBeforeCursorLength = Math.min(selStart, maxLengthMinusSelection - newAfterCursorLength);
        int newBeforeCursorHead = selStart - newBeforeCursorLength;
        if (isCutOnSurrogate(subText, selStart - newBeforeCursorLength, 0)) {
            newBeforeCursorHead++;
            newBeforeCursorLength--;
        }
        if (isCutOnSurrogate(subText, (selEnd + newAfterCursorLength) - 1, 1)) {
            newAfterCursorLength--;
        }
        int newTextLength = newBeforeCursorLength + newSelLength + newAfterCursorLength;
        if (newSelLength != sourceSelLength) {
            CharSequence beforeCursor = subText.subSequence(newBeforeCursorHead, newBeforeCursorHead + newBeforeCursorLength);
            CharSequence afterCursor = subText.subSequence(selEnd, selEnd + newAfterCursorLength);
            newInitialSurroundingText = TextUtils.concat(beforeCursor, afterCursor);
        } else {
            newInitialSurroundingText = subText.subSequence(newBeforeCursorHead, newBeforeCursorHead + newTextLength);
        }
        int newSelHead = 0 + newBeforeCursorLength;
        int newOffset = (subTextStart + selStart) - newSelHead;
        this.mInitialSurroundingText = new SurroundingText(newInitialSurroundingText, newSelHead, newSelHead + newSelLength, newOffset);
    }

    public CharSequence getInitialTextBeforeCursor(int length, int flags) {
        SurroundingText surroundingText = this.mInitialSurroundingText;
        if (surroundingText == null) {
            return null;
        }
        int selStart = Math.min(surroundingText.getSelectionStart(), this.mInitialSurroundingText.getSelectionEnd());
        int n = Math.min(length, selStart);
        if ((flags & 1) != 0) {
            return this.mInitialSurroundingText.getText().subSequence(selStart - n, selStart);
        }
        return TextUtils.substring(this.mInitialSurroundingText.getText(), selStart - n, selStart);
    }

    public CharSequence getInitialSelectedText(int flags) {
        SurroundingText surroundingText = this.mInitialSurroundingText;
        if (surroundingText == null) {
            return null;
        }
        int correctedTextSelEnd = this.initialSelStart;
        int i = this.initialSelEnd;
        int correctedTextSelStart = correctedTextSelEnd > i ? i : correctedTextSelEnd;
        if (correctedTextSelEnd <= i) {
            correctedTextSelEnd = i;
        }
        int sourceSelLength = correctedTextSelEnd - correctedTextSelStart;
        int selStart = surroundingText.getSelectionStart();
        int selEnd = this.mInitialSurroundingText.getSelectionEnd();
        if (selStart > selEnd) {
            selStart = selEnd;
            selEnd = selStart;
        }
        int tmp = selEnd - selStart;
        if (this.initialSelStart < 0 || this.initialSelEnd < 0 || tmp != sourceSelLength) {
            return null;
        }
        if ((flags & 1) != 0) {
            return this.mInitialSurroundingText.getText().subSequence(selStart, selEnd);
        }
        return TextUtils.substring(this.mInitialSurroundingText.getText(), selStart, selEnd);
    }

    public CharSequence getInitialTextAfterCursor(int length, int flags) {
        SurroundingText surroundingText = this.mInitialSurroundingText;
        if (surroundingText == null) {
            return null;
        }
        int surroundingTextLength = surroundingText.getText().length();
        int selEnd = Math.max(this.mInitialSurroundingText.getSelectionStart(), this.mInitialSurroundingText.getSelectionEnd());
        int n = Math.min(length, surroundingTextLength - selEnd);
        if ((flags & 1) != 0) {
            return this.mInitialSurroundingText.getText().subSequence(selEnd, selEnd + n);
        }
        return TextUtils.substring(this.mInitialSurroundingText.getText(), selEnd, selEnd + n);
    }

    public SurroundingText getInitialSurroundingText(int beforeLength, int afterLength, int flags) {
        CharSequence newText;
        Preconditions.checkArgumentNonnegative(beforeLength);
        Preconditions.checkArgumentNonnegative(afterLength);
        SurroundingText surroundingText = this.mInitialSurroundingText;
        if (surroundingText == null) {
            return null;
        }
        int length = surroundingText.getText().length();
        int selStart = this.mInitialSurroundingText.getSelectionStart();
        int selEnd = this.mInitialSurroundingText.getSelectionEnd();
        if (selStart > selEnd) {
            selStart = selEnd;
            selEnd = selStart;
        }
        int before = Math.min(beforeLength, selStart);
        int after = Math.min(selEnd + afterLength, length);
        int offset = selStart - before;
        if ((flags & 1) != 0) {
            newText = this.mInitialSurroundingText.getText().subSequence(offset, after);
        } else {
            newText = TextUtils.substring(this.mInitialSurroundingText.getText(), offset, after);
        }
        int newSelEnd = Math.min(selEnd - offset, length);
        return new SurroundingText(newText, before, newSelEnd, this.mInitialSurroundingText.getOffset() + offset);
    }

    private static boolean isCutOnSurrogate(CharSequence sourceText, int cutPosition, int policy) {
        switch (policy) {
            case 0:
                return Character.isLowSurrogate(sourceText.charAt(cutPosition));
            case 1:
                return Character.isHighSurrogate(sourceText.charAt(cutPosition));
            default:
                return false;
        }
    }

    private static boolean isPasswordInputType(int inputType) {
        int variation = inputType & 4095;
        return variation == 129 || variation == 225 || variation == 18;
    }

    public final void makeCompatible(int targetSdkVersion) {
        if (targetSdkVersion < 11) {
            int i = this.inputType;
            switch (i & 4095) {
                case 2:
                case 18:
                    this.inputType = (i & InputType.TYPE_MASK_FLAGS) | 2;
                    return;
                case 209:
                    this.inputType = (i & InputType.TYPE_MASK_FLAGS) | 33;
                    return;
                case 225:
                    this.inputType = (i & InputType.TYPE_MASK_FLAGS) | 129;
                    return;
                default:
                    return;
            }
        }
    }

    public void dumpDebug(ProtoOutputStream proto, long fieldId) {
        long token = proto.start(fieldId);
        proto.write(1120986464257L, this.inputType);
        proto.write(1120986464258L, this.imeOptions);
        proto.write(1138166333443L, this.privateImeOptions);
        proto.write(1138166333444L, this.packageName);
        proto.write(1120986464261L, this.fieldId);
        UserHandle userHandle = this.targetInputMethodUser;
        if (userHandle != null) {
            proto.write(1120986464262L, userHandle.getIdentifier());
        }
        proto.end(token);
    }

    public void dump(Printer pw, String prefix) {
        pw.println(prefix + "inputType=0x" + Integer.toHexString(this.inputType) + " imeOptions=0x" + Integer.toHexString(this.imeOptions) + " privateImeOptions=" + this.privateImeOptions);
        pw.println(prefix + "actionLabel=" + ((Object) this.actionLabel) + " actionId=" + this.actionId);
        pw.println(prefix + "initialSelStart=" + this.initialSelStart + " initialSelEnd=" + this.initialSelEnd + " initialCapsMode=0x" + Integer.toHexString(this.initialCapsMode));
        pw.println(prefix + "hintText=" + ((Object) this.hintText) + " label=" + ((Object) this.label));
        pw.println(prefix + "packageName=" + this.packageName + " autofillId=" + this.autofillId + " fieldId=" + this.fieldId + " fieldName=" + this.fieldName);
        pw.println(prefix + "extras=" + this.extras);
        pw.println(prefix + "hintLocales=" + this.hintLocales);
        pw.println(prefix + "contentMimeTypes=" + Arrays.toString(this.contentMimeTypes));
        if (this.targetInputMethodUser != null) {
            pw.println(prefix + "targetInputMethodUserId=" + this.targetInputMethodUser.getIdentifier());
        }
    }

    public final EditorInfo createCopyInternal() {
        EditorInfo newEditorInfo = new EditorInfo();
        newEditorInfo.inputType = this.inputType;
        newEditorInfo.imeOptions = this.imeOptions;
        newEditorInfo.privateImeOptions = this.privateImeOptions;
        newEditorInfo.internalImeOptions = this.internalImeOptions;
        newEditorInfo.actionLabel = TextUtils.stringOrSpannedString(this.actionLabel);
        newEditorInfo.actionId = this.actionId;
        newEditorInfo.initialSelStart = this.initialSelStart;
        newEditorInfo.initialSelEnd = this.initialSelEnd;
        newEditorInfo.initialCapsMode = this.initialCapsMode;
        newEditorInfo.hintText = TextUtils.stringOrSpannedString(this.hintText);
        newEditorInfo.label = TextUtils.stringOrSpannedString(this.label);
        newEditorInfo.packageName = this.packageName;
        newEditorInfo.autofillId = this.autofillId;
        newEditorInfo.fieldId = this.fieldId;
        newEditorInfo.fieldName = this.fieldName;
        Bundle bundle = this.extras;
        newEditorInfo.extras = bundle != null ? bundle.deepCopy() : null;
        newEditorInfo.mInitialSurroundingText = this.mInitialSurroundingText;
        newEditorInfo.hintLocales = this.hintLocales;
        newEditorInfo.contentMimeTypes = (String[]) ArrayUtils.cloneOrNull(this.contentMimeTypes);
        return newEditorInfo;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.inputType);
        dest.writeInt(this.imeOptions);
        dest.writeString(this.privateImeOptions);
        dest.writeInt(this.internalImeOptions);
        TextUtils.writeToParcel(this.actionLabel, dest, flags);
        dest.writeInt(this.actionId);
        dest.writeInt(this.initialSelStart);
        dest.writeInt(this.initialSelEnd);
        dest.writeInt(this.initialCapsMode);
        TextUtils.writeToParcel(this.hintText, dest, flags);
        TextUtils.writeToParcel(this.label, dest, flags);
        dest.writeString(this.packageName);
        dest.writeParcelable(this.autofillId, flags);
        dest.writeInt(this.fieldId);
        dest.writeString(this.fieldName);
        dest.writeBundle(this.extras);
        dest.writeBoolean(this.mInitialSurroundingText != null);
        SurroundingText surroundingText = this.mInitialSurroundingText;
        if (surroundingText != null) {
            surroundingText.writeToParcel(dest, flags);
        }
        LocaleList localeList = this.hintLocales;
        if (localeList != null) {
            localeList.writeToParcel(dest, flags);
        } else {
            LocaleList.getEmptyLocaleList().writeToParcel(dest, flags);
        }
        dest.writeStringArray(this.contentMimeTypes);
        UserHandle.writeToParcel(this.targetInputMethodUser, dest);
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }
}
