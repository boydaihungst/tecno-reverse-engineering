package com.android.modules.utils;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.Collections;
import java.util.List;
/* loaded from: classes4.dex */
public class ParceledListSlice<T extends Parcelable> extends BaseParceledListSlice<T> {
    public static final Parcelable.ClassLoaderCreator<ParceledListSlice> CREATOR = new Parcelable.ClassLoaderCreator<ParceledListSlice>() { // from class: com.android.modules.utils.ParceledListSlice.1
        /* JADX DEBUG: Method merged with bridge method */
        @Override // android.os.Parcelable.Creator
        public ParceledListSlice createFromParcel(Parcel in) {
            return new ParceledListSlice(in, null);
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.ClassLoaderCreator
        public ParceledListSlice createFromParcel(Parcel in, ClassLoader loader) {
            return new ParceledListSlice(in, loader);
        }

        /* JADX DEBUG: Method merged with bridge method */
        @Override // android.os.Parcelable.Creator
        public ParceledListSlice[] newArray(int size) {
            return new ParceledListSlice[size];
        }
    };

    @Override // com.android.modules.utils.BaseParceledListSlice
    public /* bridge */ /* synthetic */ List getList() {
        return super.getList();
    }

    @Override // com.android.modules.utils.BaseParceledListSlice
    public /* bridge */ /* synthetic */ void setInlineCountLimit(int i) {
        super.setInlineCountLimit(i);
    }

    /* JADX DEBUG: Multi-variable search result rejected for r0v0, resolved type: com.android.modules.utils.ParceledListSlice<T extends android.os.Parcelable> */
    /* JADX WARN: Multi-variable type inference failed */
    @Override // com.android.modules.utils.BaseParceledListSlice
    protected /* bridge */ /* synthetic */ void writeElement(Object obj, Parcel parcel, int i) {
        writeElement((ParceledListSlice<T>) obj, parcel, i);
    }

    /* JADX DEBUG: Multi-variable search result rejected for r0v0, resolved type: com.android.modules.utils.ParceledListSlice<T extends android.os.Parcelable> */
    /* JADX WARN: Multi-variable type inference failed */
    @Override // com.android.modules.utils.BaseParceledListSlice
    protected /* bridge */ /* synthetic */ void writeParcelableCreator(Object obj, Parcel parcel) {
        writeParcelableCreator((ParceledListSlice<T>) obj, parcel);
    }

    @Override // com.android.modules.utils.BaseParceledListSlice, android.os.Parcelable
    public /* bridge */ /* synthetic */ void writeToParcel(Parcel parcel, int i) {
        super.writeToParcel(parcel, i);
    }

    public ParceledListSlice(List<T> list) {
        super(list);
    }

    private ParceledListSlice(Parcel in, ClassLoader loader) {
        super(in, loader);
    }

    public static <T extends Parcelable> ParceledListSlice<T> emptyList() {
        return new ParceledListSlice<>(Collections.emptyList());
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        int contents = 0;
        List<T> list = getList();
        for (int i = 0; i < list.size(); i++) {
            contents |= list.get(i).describeContents();
        }
        return contents;
    }

    protected void writeElement(T parcelable, Parcel dest, int callFlags) {
        parcelable.writeToParcel(dest, callFlags);
    }

    protected void writeParcelableCreator(T parcelable, Parcel dest) {
        dest.writeParcelableCreator(parcelable);
    }

    @Override // com.android.modules.utils.BaseParceledListSlice
    protected Parcelable.Creator<?> readParcelableCreator(Parcel from, ClassLoader loader) {
        return from.readParcelableCreator(loader);
    }
}
