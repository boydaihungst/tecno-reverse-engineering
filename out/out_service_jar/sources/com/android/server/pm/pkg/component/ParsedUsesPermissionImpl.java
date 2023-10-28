package com.android.server.pm.pkg.component;

import android.annotation.NonNull;
import android.os.Parcel;
import android.os.Parcelable;
import com.android.internal.util.AnnotationValidations;
import com.android.internal.util.Parcelling;
import com.android.server.pm.pkg.component.ParsedUsesPermission;
import java.lang.annotation.Annotation;
/* loaded from: classes2.dex */
public class ParsedUsesPermissionImpl implements ParsedUsesPermission, Parcelable {
    public static final Parcelable.Creator<ParsedUsesPermissionImpl> CREATOR;
    static Parcelling<String> sParcellingForName;
    private String name;
    private int usesPermissionFlags;

    public ParsedUsesPermissionImpl(String name, int usesPermissionFlags) {
        this.name = name;
        AnnotationValidations.validate(NonNull.class, (NonNull) null, name);
        this.usesPermissionFlags = usesPermissionFlags;
        AnnotationValidations.validate(ParsedUsesPermission.UsesPermissionFlags.class, (Annotation) null, usesPermissionFlags);
    }

    @Override // com.android.server.pm.pkg.component.ParsedUsesPermission
    public String getName() {
        return this.name;
    }

    @Override // com.android.server.pm.pkg.component.ParsedUsesPermission
    public int getUsesPermissionFlags() {
        return this.usesPermissionFlags;
    }

    public ParsedUsesPermissionImpl setName(String value) {
        this.name = value;
        AnnotationValidations.validate(NonNull.class, (NonNull) null, value);
        return this;
    }

    public ParsedUsesPermissionImpl setUsesPermissionFlags(int value) {
        this.usesPermissionFlags = value;
        AnnotationValidations.validate(ParsedUsesPermission.UsesPermissionFlags.class, (Annotation) null, value);
        return this;
    }

    static {
        Parcelling<String> parcelling = Parcelling.Cache.get(Parcelling.BuiltIn.ForInternedString.class);
        sParcellingForName = parcelling;
        if (parcelling == null) {
            sParcellingForName = Parcelling.Cache.put(new Parcelling.BuiltIn.ForInternedString());
        }
        CREATOR = new Parcelable.Creator<ParsedUsesPermissionImpl>() { // from class: com.android.server.pm.pkg.component.ParsedUsesPermissionImpl.1
            /* JADX DEBUG: Method merged with bridge method */
            /* JADX WARN: Can't rename method to resolve collision */
            @Override // android.os.Parcelable.Creator
            public ParsedUsesPermissionImpl[] newArray(int size) {
                return new ParsedUsesPermissionImpl[size];
            }

            /* JADX DEBUG: Method merged with bridge method */
            /* JADX WARN: Can't rename method to resolve collision */
            @Override // android.os.Parcelable.Creator
            public ParsedUsesPermissionImpl createFromParcel(Parcel in) {
                return new ParsedUsesPermissionImpl(in);
            }
        };
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        sParcellingForName.parcel(this.name, dest, flags);
        dest.writeInt(this.usesPermissionFlags);
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    protected ParsedUsesPermissionImpl(Parcel in) {
        String _name = (String) sParcellingForName.unparcel(in);
        int _usesPermissionFlags = in.readInt();
        this.name = _name;
        AnnotationValidations.validate(NonNull.class, (NonNull) null, _name);
        this.usesPermissionFlags = _usesPermissionFlags;
        AnnotationValidations.validate(ParsedUsesPermission.UsesPermissionFlags.class, (Annotation) null, _usesPermissionFlags);
    }

    @Deprecated
    private void __metadata() {
    }
}
