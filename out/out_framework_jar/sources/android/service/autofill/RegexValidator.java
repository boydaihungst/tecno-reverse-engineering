package android.service.autofill;

import android.inputmethodservice.navigationbar.NavigationBarInflaterView;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import android.view.autofill.AutofillId;
import android.view.autofill.Helper;
import java.util.Objects;
import java.util.regex.Pattern;
/* loaded from: classes3.dex */
public final class RegexValidator extends InternalValidator implements Validator, Parcelable {
    public static final Parcelable.Creator<RegexValidator> CREATOR = new Parcelable.Creator<RegexValidator>() { // from class: android.service.autofill.RegexValidator.1
        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public RegexValidator createFromParcel(Parcel parcel) {
            return new RegexValidator((AutofillId) parcel.readParcelable(null, AutofillId.class), (Pattern) parcel.readSerializable(Pattern.class.getClassLoader(), Pattern.class));
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public RegexValidator[] newArray(int size) {
            return new RegexValidator[size];
        }
    };
    private static final String TAG = "RegexValidator";
    private final AutofillId mId;
    private final Pattern mRegex;

    public RegexValidator(AutofillId id, Pattern regex) {
        this.mId = (AutofillId) Objects.requireNonNull(id);
        this.mRegex = (Pattern) Objects.requireNonNull(regex);
    }

    @Override // android.service.autofill.InternalValidator
    public boolean isValid(ValueFinder finder) {
        String value = finder.findByAutofillId(this.mId);
        if (value == null) {
            Log.w(TAG, "No view for id " + this.mId);
            return false;
        }
        boolean valid = this.mRegex.matcher(value).matches();
        if (Helper.sDebug) {
            Log.d(TAG, "isValid(): " + valid);
        }
        return valid;
    }

    public String toString() {
        return !Helper.sDebug ? super.toString() : "RegexValidator: [id=" + this.mId + ", regex=" + this.mRegex + NavigationBarInflaterView.SIZE_MOD_END;
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeParcelable(this.mId, flags);
        parcel.writeSerializable(this.mRegex);
    }
}
