package com.android.server.pm.pkg.component;

import android.content.ComponentName;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import com.android.server.pm.pkg.parsing.ParsingPackageImpl;
/* loaded from: classes2.dex */
public class ParsedServiceImpl extends ParsedMainComponentImpl implements ParsedService, Parcelable {
    public static final Parcelable.Creator<ParsedServiceImpl> CREATOR = new Parcelable.Creator<ParsedServiceImpl>() { // from class: com.android.server.pm.pkg.component.ParsedServiceImpl.1
        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public ParsedServiceImpl createFromParcel(Parcel source) {
            return new ParsedServiceImpl(source);
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public ParsedServiceImpl[] newArray(int size) {
            return new ParsedServiceImpl[size];
        }
    };
    private int foregroundServiceType;
    private String permission;

    public ParsedServiceImpl(ParsedServiceImpl other) {
        super(other);
        this.foregroundServiceType = other.foregroundServiceType;
        this.permission = other.permission;
    }

    public ParsedMainComponent setPermission(String permission) {
        this.permission = TextUtils.isEmpty(permission) ? null : permission.intern();
        return this;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(128);
        sb.append("Service{");
        sb.append(Integer.toHexString(System.identityHashCode(this)));
        sb.append(' ');
        ComponentName.appendShortString(sb, getPackageName(), getName());
        sb.append('}');
        return sb.toString();
    }

    @Override // com.android.server.pm.pkg.component.ParsedMainComponentImpl, com.android.server.pm.pkg.component.ParsedComponentImpl, android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    @Override // com.android.server.pm.pkg.component.ParsedMainComponentImpl, com.android.server.pm.pkg.component.ParsedComponentImpl, android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeInt(this.foregroundServiceType);
        ParsingPackageImpl.sForInternedString.parcel(this.permission, dest, flags);
    }

    public ParsedServiceImpl() {
    }

    protected ParsedServiceImpl(Parcel in) {
        super(in);
        this.foregroundServiceType = in.readInt();
        this.permission = ParsingPackageImpl.sForInternedString.unparcel(in);
    }

    public ParsedServiceImpl(int foregroundServiceType, String permission) {
        this.foregroundServiceType = foregroundServiceType;
        this.permission = permission;
    }

    @Override // com.android.server.pm.pkg.component.ParsedService
    public int getForegroundServiceType() {
        return this.foregroundServiceType;
    }

    @Override // com.android.server.pm.pkg.component.ParsedService
    public String getPermission() {
        return this.permission;
    }

    public ParsedServiceImpl setForegroundServiceType(int value) {
        this.foregroundServiceType = value;
        return this;
    }

    @Deprecated
    private void __metadata() {
    }
}
