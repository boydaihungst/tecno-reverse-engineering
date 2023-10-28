package android.content.pm;

import android.annotation.NonNull;
import android.content.pm.ApplicationInfo;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.ArraySet;
import com.android.internal.util.AnnotationValidations;
import com.android.internal.util.Parcelling;
import java.lang.annotation.Annotation;
/* loaded from: classes.dex */
public class ProcessInfo implements Parcelable {
    public static final Parcelable.Creator<ProcessInfo> CREATOR;
    static Parcelling<ArraySet<String>> sParcellingForDeniedPermissions;
    public ArraySet<String> deniedPermissions;
    public int gwpAsanMode;
    public int memtagMode;
    public String name;
    public int nativeHeapZeroInitialized;

    @Deprecated
    public ProcessInfo(ProcessInfo orig) {
        this.name = orig.name;
        this.deniedPermissions = orig.deniedPermissions;
        this.gwpAsanMode = orig.gwpAsanMode;
        this.memtagMode = orig.memtagMode;
        this.nativeHeapZeroInitialized = orig.nativeHeapZeroInitialized;
    }

    public ProcessInfo(String name, ArraySet<String> deniedPermissions, int gwpAsanMode, int memtagMode, int nativeHeapZeroInitialized) {
        this.name = name;
        AnnotationValidations.validate((Class<NonNull>) NonNull.class, (NonNull) null, (Object) name);
        this.deniedPermissions = deniedPermissions;
        this.gwpAsanMode = gwpAsanMode;
        AnnotationValidations.validate((Class<? extends Annotation>) ApplicationInfo.GwpAsanMode.class, (Annotation) null, gwpAsanMode);
        this.memtagMode = memtagMode;
        AnnotationValidations.validate((Class<? extends Annotation>) ApplicationInfo.MemtagMode.class, (Annotation) null, memtagMode);
        this.nativeHeapZeroInitialized = nativeHeapZeroInitialized;
        AnnotationValidations.validate((Class<? extends Annotation>) ApplicationInfo.NativeHeapZeroInitialized.class, (Annotation) null, nativeHeapZeroInitialized);
    }

    static {
        Parcelling<ArraySet<String>> parcelling = Parcelling.Cache.get(Parcelling.BuiltIn.ForInternedStringArraySet.class);
        sParcellingForDeniedPermissions = parcelling;
        if (parcelling == null) {
            sParcellingForDeniedPermissions = Parcelling.Cache.put(new Parcelling.BuiltIn.ForInternedStringArraySet());
        }
        CREATOR = new Parcelable.Creator<ProcessInfo>() { // from class: android.content.pm.ProcessInfo.1
            /* JADX DEBUG: Method merged with bridge method */
            /* JADX WARN: Can't rename method to resolve collision */
            @Override // android.os.Parcelable.Creator
            public ProcessInfo[] newArray(int size) {
                return new ProcessInfo[size];
            }

            /* JADX DEBUG: Method merged with bridge method */
            /* JADX WARN: Can't rename method to resolve collision */
            @Override // android.os.Parcelable.Creator
            public ProcessInfo createFromParcel(Parcel in) {
                return new ProcessInfo(in);
            }
        };
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        byte flg = this.deniedPermissions != null ? (byte) (0 | 2) : (byte) 0;
        dest.writeByte(flg);
        dest.writeString(this.name);
        sParcellingForDeniedPermissions.parcel(this.deniedPermissions, dest, flags);
        dest.writeInt(this.gwpAsanMode);
        dest.writeInt(this.memtagMode);
        dest.writeInt(this.nativeHeapZeroInitialized);
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    protected ProcessInfo(Parcel in) {
        in.readByte();
        String _name = in.readString();
        ArraySet<String> _deniedPermissions = sParcellingForDeniedPermissions.unparcel(in);
        int _gwpAsanMode = in.readInt();
        int _memtagMode = in.readInt();
        int _nativeHeapZeroInitialized = in.readInt();
        this.name = _name;
        AnnotationValidations.validate((Class<NonNull>) NonNull.class, (NonNull) null, (Object) _name);
        this.deniedPermissions = _deniedPermissions;
        this.gwpAsanMode = _gwpAsanMode;
        AnnotationValidations.validate((Class<? extends Annotation>) ApplicationInfo.GwpAsanMode.class, (Annotation) null, _gwpAsanMode);
        this.memtagMode = _memtagMode;
        AnnotationValidations.validate((Class<? extends Annotation>) ApplicationInfo.MemtagMode.class, (Annotation) null, _memtagMode);
        this.nativeHeapZeroInitialized = _nativeHeapZeroInitialized;
        AnnotationValidations.validate((Class<? extends Annotation>) ApplicationInfo.NativeHeapZeroInitialized.class, (Annotation) null, _nativeHeapZeroInitialized);
    }

    @Deprecated
    private void __metadata() {
    }
}
