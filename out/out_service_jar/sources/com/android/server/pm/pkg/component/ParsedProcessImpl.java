package com.android.server.pm.pkg.component;

import android.annotation.NonNull;
import android.content.pm.ApplicationInfo;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.ArrayMap;
import android.util.ArraySet;
import com.android.internal.util.AnnotationValidations;
import com.android.internal.util.CollectionUtils;
import com.android.internal.util.Parcelling;
import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.Set;
/* loaded from: classes2.dex */
public class ParsedProcessImpl implements ParsedProcess, Parcelable {
    public static final Parcelable.Creator<ParsedProcessImpl> CREATOR;
    static Parcelling<Set<String>> sParcellingForDeniedPermissions;
    private ArrayMap<String, String> appClassNamesByPackage;
    private Set<String> deniedPermissions;
    private int gwpAsanMode;
    private int memtagMode;
    private String name;
    private int nativeHeapZeroInitialized;

    public ParsedProcessImpl() {
        this.appClassNamesByPackage = ArrayMap.EMPTY;
        this.deniedPermissions = Collections.emptySet();
        this.gwpAsanMode = -1;
        this.memtagMode = -1;
        this.nativeHeapZeroInitialized = -1;
    }

    public ParsedProcessImpl(ParsedProcess other) {
        this.appClassNamesByPackage = ArrayMap.EMPTY;
        this.deniedPermissions = Collections.emptySet();
        this.gwpAsanMode = -1;
        this.memtagMode = -1;
        this.nativeHeapZeroInitialized = -1;
        this.name = other.getName();
        this.appClassNamesByPackage = other.getAppClassNamesByPackage().size() == 0 ? ArrayMap.EMPTY : new ArrayMap<>(other.getAppClassNamesByPackage());
        this.deniedPermissions = new ArraySet(other.getDeniedPermissions());
        this.gwpAsanMode = other.getGwpAsanMode();
        this.memtagMode = other.getMemtagMode();
        this.nativeHeapZeroInitialized = other.getNativeHeapZeroInitialized();
    }

    public void addStateFrom(ParsedProcess other) {
        this.deniedPermissions = CollectionUtils.addAll(this.deniedPermissions, other.getDeniedPermissions());
        this.gwpAsanMode = other.getGwpAsanMode();
        this.memtagMode = other.getMemtagMode();
        this.nativeHeapZeroInitialized = other.getNativeHeapZeroInitialized();
        ArrayMap<String, String> oacn = other.getAppClassNamesByPackage();
        for (int i = 0; i < oacn.size(); i++) {
            this.appClassNamesByPackage.put(oacn.keyAt(i), oacn.valueAt(i));
        }
    }

    public void putAppClassNameForPackage(String packageName, String className) {
        if (this.appClassNamesByPackage.size() == 0) {
            this.appClassNamesByPackage = new ArrayMap<>(4);
        }
        this.appClassNamesByPackage.put(packageName, className);
    }

    public ParsedProcessImpl(String name, ArrayMap<String, String> appClassNamesByPackage, Set<String> deniedPermissions, int gwpAsanMode, int memtagMode, int nativeHeapZeroInitialized) {
        this.appClassNamesByPackage = ArrayMap.EMPTY;
        this.deniedPermissions = Collections.emptySet();
        this.gwpAsanMode = -1;
        this.memtagMode = -1;
        this.nativeHeapZeroInitialized = -1;
        this.name = name;
        AnnotationValidations.validate(NonNull.class, (NonNull) null, name);
        this.appClassNamesByPackage = appClassNamesByPackage;
        AnnotationValidations.validate(NonNull.class, (NonNull) null, appClassNamesByPackage);
        this.deniedPermissions = deniedPermissions;
        AnnotationValidations.validate(NonNull.class, (NonNull) null, deniedPermissions);
        this.gwpAsanMode = gwpAsanMode;
        AnnotationValidations.validate(ApplicationInfo.GwpAsanMode.class, (Annotation) null, gwpAsanMode);
        this.memtagMode = memtagMode;
        AnnotationValidations.validate(ApplicationInfo.MemtagMode.class, (Annotation) null, memtagMode);
        this.nativeHeapZeroInitialized = nativeHeapZeroInitialized;
        AnnotationValidations.validate(ApplicationInfo.NativeHeapZeroInitialized.class, (Annotation) null, nativeHeapZeroInitialized);
    }

    @Override // com.android.server.pm.pkg.component.ParsedProcess
    public String getName() {
        return this.name;
    }

    @Override // com.android.server.pm.pkg.component.ParsedProcess
    public ArrayMap<String, String> getAppClassNamesByPackage() {
        return this.appClassNamesByPackage;
    }

    @Override // com.android.server.pm.pkg.component.ParsedProcess
    public Set<String> getDeniedPermissions() {
        return this.deniedPermissions;
    }

    @Override // com.android.server.pm.pkg.component.ParsedProcess
    public int getGwpAsanMode() {
        return this.gwpAsanMode;
    }

    @Override // com.android.server.pm.pkg.component.ParsedProcess
    public int getMemtagMode() {
        return this.memtagMode;
    }

    @Override // com.android.server.pm.pkg.component.ParsedProcess
    public int getNativeHeapZeroInitialized() {
        return this.nativeHeapZeroInitialized;
    }

    public ParsedProcessImpl setName(String value) {
        this.name = value;
        AnnotationValidations.validate(NonNull.class, (NonNull) null, value);
        return this;
    }

    public ParsedProcessImpl setAppClassNamesByPackage(ArrayMap<String, String> value) {
        this.appClassNamesByPackage = value;
        AnnotationValidations.validate(NonNull.class, (NonNull) null, value);
        return this;
    }

    public ParsedProcessImpl setDeniedPermissions(Set<String> value) {
        this.deniedPermissions = value;
        AnnotationValidations.validate(NonNull.class, (NonNull) null, value);
        return this;
    }

    public ParsedProcessImpl setGwpAsanMode(int value) {
        this.gwpAsanMode = value;
        AnnotationValidations.validate(ApplicationInfo.GwpAsanMode.class, (Annotation) null, value);
        return this;
    }

    public ParsedProcessImpl setMemtagMode(int value) {
        this.memtagMode = value;
        AnnotationValidations.validate(ApplicationInfo.MemtagMode.class, (Annotation) null, value);
        return this;
    }

    public ParsedProcessImpl setNativeHeapZeroInitialized(int value) {
        this.nativeHeapZeroInitialized = value;
        AnnotationValidations.validate(ApplicationInfo.NativeHeapZeroInitialized.class, (Annotation) null, value);
        return this;
    }

    static {
        Parcelling<Set<String>> parcelling = Parcelling.Cache.get(Parcelling.BuiltIn.ForInternedStringSet.class);
        sParcellingForDeniedPermissions = parcelling;
        if (parcelling == null) {
            sParcellingForDeniedPermissions = Parcelling.Cache.put(new Parcelling.BuiltIn.ForInternedStringSet());
        }
        CREATOR = new Parcelable.Creator<ParsedProcessImpl>() { // from class: com.android.server.pm.pkg.component.ParsedProcessImpl.1
            /* JADX DEBUG: Method merged with bridge method */
            /* JADX WARN: Can't rename method to resolve collision */
            @Override // android.os.Parcelable.Creator
            public ParsedProcessImpl[] newArray(int size) {
                return new ParsedProcessImpl[size];
            }

            /* JADX DEBUG: Method merged with bridge method */
            /* JADX WARN: Can't rename method to resolve collision */
            @Override // android.os.Parcelable.Creator
            public ParsedProcessImpl createFromParcel(Parcel in) {
                return new ParsedProcessImpl(in);
            }
        };
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.name);
        dest.writeMap(this.appClassNamesByPackage);
        sParcellingForDeniedPermissions.parcel(this.deniedPermissions, dest, flags);
        dest.writeInt(this.gwpAsanMode);
        dest.writeInt(this.memtagMode);
        dest.writeInt(this.nativeHeapZeroInitialized);
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    protected ParsedProcessImpl(Parcel in) {
        this.appClassNamesByPackage = ArrayMap.EMPTY;
        this.deniedPermissions = Collections.emptySet();
        this.gwpAsanMode = -1;
        this.memtagMode = -1;
        this.nativeHeapZeroInitialized = -1;
        String _name = in.readString();
        ArrayMap<String, String> _appClassNamesByPackage = new ArrayMap<>();
        in.readMap(_appClassNamesByPackage, String.class.getClassLoader());
        Set<String> _deniedPermissions = (Set) sParcellingForDeniedPermissions.unparcel(in);
        int _gwpAsanMode = in.readInt();
        int _memtagMode = in.readInt();
        int _nativeHeapZeroInitialized = in.readInt();
        this.name = _name;
        AnnotationValidations.validate(NonNull.class, (NonNull) null, _name);
        this.appClassNamesByPackage = _appClassNamesByPackage;
        AnnotationValidations.validate(NonNull.class, (NonNull) null, _appClassNamesByPackage);
        this.deniedPermissions = _deniedPermissions;
        AnnotationValidations.validate(NonNull.class, (NonNull) null, _deniedPermissions);
        this.gwpAsanMode = _gwpAsanMode;
        AnnotationValidations.validate(ApplicationInfo.GwpAsanMode.class, (Annotation) null, _gwpAsanMode);
        this.memtagMode = _memtagMode;
        AnnotationValidations.validate(ApplicationInfo.MemtagMode.class, (Annotation) null, _memtagMode);
        this.nativeHeapZeroInitialized = _nativeHeapZeroInitialized;
        AnnotationValidations.validate(ApplicationInfo.NativeHeapZeroInitialized.class, (Annotation) null, _nativeHeapZeroInitialized);
    }

    @Deprecated
    private void __metadata() {
    }
}
