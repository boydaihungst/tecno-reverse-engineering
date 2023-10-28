package com.android.modules.utils;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.Collections;
import java.util.List;
/* loaded from: classes4.dex */
public class StringParceledListSlice extends BaseParceledListSlice<String> {
    public static final Parcelable.ClassLoaderCreator<StringParceledListSlice> CREATOR = new Parcelable.ClassLoaderCreator<StringParceledListSlice>() { // from class: com.android.modules.utils.StringParceledListSlice.1
        /* JADX DEBUG: Method merged with bridge method */
        @Override // android.os.Parcelable.Creator
        public StringParceledListSlice createFromParcel(Parcel in) {
            return new StringParceledListSlice(in, null);
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.ClassLoaderCreator
        public StringParceledListSlice createFromParcel(Parcel in, ClassLoader loader) {
            return new StringParceledListSlice(in, loader);
        }

        /* JADX DEBUG: Method merged with bridge method */
        @Override // android.os.Parcelable.Creator
        public StringParceledListSlice[] newArray(int size) {
            return new StringParceledListSlice[size];
        }
    };

    /* JADX DEBUG: Return type fixed from 'java.util.List' to match base method */
    @Override // com.android.modules.utils.BaseParceledListSlice
    public /* bridge */ /* synthetic */ List<String> getList() {
        return super.getList();
    }

    @Override // com.android.modules.utils.BaseParceledListSlice
    public /* bridge */ /* synthetic */ void setInlineCountLimit(int i) {
        super.setInlineCountLimit(i);
    }

    @Override // com.android.modules.utils.BaseParceledListSlice, android.os.Parcelable
    public /* bridge */ /* synthetic */ void writeToParcel(Parcel parcel, int i) {
        super.writeToParcel(parcel, i);
    }

    public StringParceledListSlice(List<String> list) {
        super(list);
    }

    private StringParceledListSlice(Parcel in, ClassLoader loader) {
        super(in, loader);
    }

    public static StringParceledListSlice emptyList() {
        return new StringParceledListSlice(Collections.emptyList());
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.modules.utils.BaseParceledListSlice
    public void writeElement(String parcelable, Parcel reply, int callFlags) {
        reply.writeString(parcelable);
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.modules.utils.BaseParceledListSlice
    public void writeParcelableCreator(String parcelable, Parcel dest) {
    }

    @Override // com.android.modules.utils.BaseParceledListSlice
    protected Parcelable.Creator<?> readParcelableCreator(Parcel from, ClassLoader loader) {
        return Parcel.STRING_CREATOR;
    }
}
