package com.android.server.pm.pkg.component;

import android.annotation.NonNull;
import android.annotation.StringRes;
import android.os.Parcel;
import android.os.Parcelable;
import com.android.internal.util.AnnotationValidations;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
/* loaded from: classes2.dex */
public class ParsedAttributionImpl implements ParsedAttribution, Parcelable {
    public static final Parcelable.Creator<ParsedAttributionImpl> CREATOR = new Parcelable.Creator<ParsedAttributionImpl>() { // from class: com.android.server.pm.pkg.component.ParsedAttributionImpl.1
        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public ParsedAttributionImpl[] newArray(int size) {
            return new ParsedAttributionImpl[size];
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public ParsedAttributionImpl createFromParcel(Parcel in) {
            return new ParsedAttributionImpl(in);
        }
    };
    static final int MAX_NUM_ATTRIBUTIONS = 10000;
    private List<String> inheritFrom;
    private int label;
    private String tag;

    public ParsedAttributionImpl() {
    }

    public ParsedAttributionImpl(String tag, int label, List<String> inheritFrom) {
        this.tag = tag;
        AnnotationValidations.validate(NonNull.class, (NonNull) null, tag);
        this.label = label;
        AnnotationValidations.validate(StringRes.class, (Annotation) null, label);
        this.inheritFrom = inheritFrom;
        AnnotationValidations.validate(NonNull.class, (NonNull) null, inheritFrom);
    }

    @Override // com.android.server.pm.pkg.component.ParsedAttribution
    public String getTag() {
        return this.tag;
    }

    @Override // com.android.server.pm.pkg.component.ParsedAttribution
    public int getLabel() {
        return this.label;
    }

    @Override // com.android.server.pm.pkg.component.ParsedAttribution
    public List<String> getInheritFrom() {
        return this.inheritFrom;
    }

    public ParsedAttributionImpl setTag(String value) {
        this.tag = value;
        AnnotationValidations.validate(NonNull.class, (NonNull) null, value);
        return this;
    }

    public ParsedAttributionImpl setLabel(int value) {
        this.label = value;
        AnnotationValidations.validate(StringRes.class, (Annotation) null, value);
        return this;
    }

    public ParsedAttributionImpl setInheritFrom(List<String> value) {
        this.inheritFrom = value;
        AnnotationValidations.validate(NonNull.class, (NonNull) null, value);
        return this;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.tag);
        dest.writeInt(this.label);
        dest.writeStringList(this.inheritFrom);
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    protected ParsedAttributionImpl(Parcel in) {
        String _tag = in.readString();
        int _label = in.readInt();
        List<String> _inheritFrom = new ArrayList<>();
        in.readStringList(_inheritFrom);
        this.tag = _tag;
        AnnotationValidations.validate(NonNull.class, (NonNull) null, _tag);
        this.label = _label;
        AnnotationValidations.validate(StringRes.class, (Annotation) null, _label);
        this.inheritFrom = _inheritFrom;
        AnnotationValidations.validate(NonNull.class, (NonNull) null, _inheritFrom);
    }

    @Deprecated
    private void __metadata() {
    }
}
