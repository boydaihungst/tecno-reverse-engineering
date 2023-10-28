package com.android.server.pm.pkg.component;

import android.annotation.NonNull;
import android.os.Parcel;
import android.os.Parcelable;
import com.android.internal.util.AnnotationValidations;
import com.android.internal.util.Parcelling;
/* loaded from: classes2.dex */
public class ParsedApexSystemServiceImpl implements ParsedApexSystemService, Parcelable {
    public static final Parcelable.Creator<ParsedApexSystemServiceImpl> CREATOR;
    static Parcelling<String> sParcellingForJarPath;
    static Parcelling<String> sParcellingForMaxSdkVersion;
    static Parcelling<String> sParcellingForMinSdkVersion;
    static Parcelling<String> sParcellingForName;
    private int initOrder;
    private String jarPath;
    private String maxSdkVersion;
    private String minSdkVersion;
    private String name;

    public ParsedApexSystemServiceImpl() {
    }

    public ParsedApexSystemServiceImpl(String name, String jarPath, String minSdkVersion, String maxSdkVersion, int initOrder) {
        this.name = name;
        AnnotationValidations.validate(NonNull.class, (NonNull) null, name);
        this.jarPath = jarPath;
        this.minSdkVersion = minSdkVersion;
        this.maxSdkVersion = maxSdkVersion;
        this.initOrder = initOrder;
    }

    @Override // com.android.server.pm.pkg.component.ParsedApexSystemService
    public String getName() {
        return this.name;
    }

    @Override // com.android.server.pm.pkg.component.ParsedApexSystemService
    public String getJarPath() {
        return this.jarPath;
    }

    @Override // com.android.server.pm.pkg.component.ParsedApexSystemService
    public String getMinSdkVersion() {
        return this.minSdkVersion;
    }

    @Override // com.android.server.pm.pkg.component.ParsedApexSystemService
    public String getMaxSdkVersion() {
        return this.maxSdkVersion;
    }

    @Override // com.android.server.pm.pkg.component.ParsedApexSystemService
    public int getInitOrder() {
        return this.initOrder;
    }

    public ParsedApexSystemServiceImpl setName(String value) {
        this.name = value;
        AnnotationValidations.validate(NonNull.class, (NonNull) null, value);
        return this;
    }

    public ParsedApexSystemServiceImpl setJarPath(String value) {
        this.jarPath = value;
        return this;
    }

    public ParsedApexSystemServiceImpl setMinSdkVersion(String value) {
        this.minSdkVersion = value;
        return this;
    }

    public ParsedApexSystemServiceImpl setMaxSdkVersion(String value) {
        this.maxSdkVersion = value;
        return this;
    }

    public ParsedApexSystemServiceImpl setInitOrder(int value) {
        this.initOrder = value;
        return this;
    }

    static {
        Parcelling<String> parcelling = Parcelling.Cache.get(Parcelling.BuiltIn.ForInternedString.class);
        sParcellingForName = parcelling;
        if (parcelling == null) {
            sParcellingForName = Parcelling.Cache.put(new Parcelling.BuiltIn.ForInternedString());
        }
        Parcelling<String> parcelling2 = Parcelling.Cache.get(Parcelling.BuiltIn.ForInternedString.class);
        sParcellingForJarPath = parcelling2;
        if (parcelling2 == null) {
            sParcellingForJarPath = Parcelling.Cache.put(new Parcelling.BuiltIn.ForInternedString());
        }
        Parcelling<String> parcelling3 = Parcelling.Cache.get(Parcelling.BuiltIn.ForInternedString.class);
        sParcellingForMinSdkVersion = parcelling3;
        if (parcelling3 == null) {
            sParcellingForMinSdkVersion = Parcelling.Cache.put(new Parcelling.BuiltIn.ForInternedString());
        }
        Parcelling<String> parcelling4 = Parcelling.Cache.get(Parcelling.BuiltIn.ForInternedString.class);
        sParcellingForMaxSdkVersion = parcelling4;
        if (parcelling4 == null) {
            sParcellingForMaxSdkVersion = Parcelling.Cache.put(new Parcelling.BuiltIn.ForInternedString());
        }
        CREATOR = new Parcelable.Creator<ParsedApexSystemServiceImpl>() { // from class: com.android.server.pm.pkg.component.ParsedApexSystemServiceImpl.1
            /* JADX DEBUG: Method merged with bridge method */
            /* JADX WARN: Can't rename method to resolve collision */
            @Override // android.os.Parcelable.Creator
            public ParsedApexSystemServiceImpl[] newArray(int size) {
                return new ParsedApexSystemServiceImpl[size];
            }

            /* JADX DEBUG: Method merged with bridge method */
            /* JADX WARN: Can't rename method to resolve collision */
            @Override // android.os.Parcelable.Creator
            public ParsedApexSystemServiceImpl createFromParcel(Parcel in) {
                return new ParsedApexSystemServiceImpl(in);
            }
        };
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        byte flg = this.jarPath != null ? (byte) (0 | 2) : (byte) 0;
        if (this.minSdkVersion != null) {
            flg = (byte) (flg | 4);
        }
        if (this.maxSdkVersion != null) {
            flg = (byte) (flg | 8);
        }
        dest.writeByte(flg);
        sParcellingForName.parcel(this.name, dest, flags);
        sParcellingForJarPath.parcel(this.jarPath, dest, flags);
        sParcellingForMinSdkVersion.parcel(this.minSdkVersion, dest, flags);
        sParcellingForMaxSdkVersion.parcel(this.maxSdkVersion, dest, flags);
        dest.writeInt(this.initOrder);
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    protected ParsedApexSystemServiceImpl(Parcel in) {
        in.readByte();
        String _name = (String) sParcellingForName.unparcel(in);
        String _jarPath = (String) sParcellingForJarPath.unparcel(in);
        String _minSdkVersion = (String) sParcellingForMinSdkVersion.unparcel(in);
        String _maxSdkVersion = (String) sParcellingForMaxSdkVersion.unparcel(in);
        int _initOrder = in.readInt();
        this.name = _name;
        AnnotationValidations.validate(NonNull.class, (NonNull) null, _name);
        this.jarPath = _jarPath;
        this.minSdkVersion = _minSdkVersion;
        this.maxSdkVersion = _maxSdkVersion;
        this.initOrder = _initOrder;
    }

    @Deprecated
    private void __metadata() {
    }
}
