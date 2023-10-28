package android.view.translation;

import android.annotation.NonNull;
import android.os.Parcel;
import android.os.Parcelable;
import com.android.internal.util.AnnotationValidations;
import com.android.internal.util.BitUtils;
import com.android.internal.util.Preconditions;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.IntFunction;
/* loaded from: classes3.dex */
public final class TranslationRequest implements Parcelable {
    public static final Parcelable.Creator<TranslationRequest> CREATOR = new Parcelable.Creator<TranslationRequest>() { // from class: android.view.translation.TranslationRequest.1
        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public TranslationRequest[] newArray(int size) {
            return new TranslationRequest[size];
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public TranslationRequest createFromParcel(Parcel in) {
            return new TranslationRequest(in);
        }
    };
    public static final int FLAG_DICTIONARY_RESULT = 2;
    public static final int FLAG_PARTIAL_RESPONSES = 8;
    public static final int FLAG_TRANSLATION_RESULT = 1;
    public static final int FLAG_TRANSLITERATION_RESULT = 4;
    private final int mFlags;
    private final List<TranslationRequestValue> mTranslationRequestValues;
    private final List<ViewTranslationRequest> mViewTranslationRequests;

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes3.dex */
    public @interface RequestFlags {
    }

    /* renamed from: -$$Nest$smdefaultFlags  reason: not valid java name */
    static /* bridge */ /* synthetic */ int m5446$$Nest$smdefaultFlags() {
        return defaultFlags();
    }

    /* renamed from: -$$Nest$smdefaultTranslationRequestValues  reason: not valid java name */
    static /* bridge */ /* synthetic */ List m5447$$Nest$smdefaultTranslationRequestValues() {
        return defaultTranslationRequestValues();
    }

    /* renamed from: -$$Nest$smdefaultViewTranslationRequests  reason: not valid java name */
    static /* bridge */ /* synthetic */ List m5448$$Nest$smdefaultViewTranslationRequests() {
        return defaultViewTranslationRequests();
    }

    private static int defaultFlags() {
        return 1;
    }

    private static List<TranslationRequestValue> defaultTranslationRequestValues() {
        return Collections.emptyList();
    }

    private static List<ViewTranslationRequest> defaultViewTranslationRequests() {
        return Collections.emptyList();
    }

    /* loaded from: classes3.dex */
    static abstract class BaseBuilder {
        @Deprecated
        public abstract Builder addTranslationRequestValue(TranslationRequestValue translationRequestValue);

        @Deprecated
        public abstract Builder addViewTranslationRequest(ViewTranslationRequest viewTranslationRequest);

        BaseBuilder() {
        }
    }

    public static String requestFlagsToString(int value) {
        return BitUtils.flagsToString(value, new IntFunction() { // from class: android.view.translation.TranslationRequest$$ExternalSyntheticLambda0
            @Override // java.util.function.IntFunction
            public final Object apply(int i) {
                return TranslationRequest.singleRequestFlagsToString(i);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static String singleRequestFlagsToString(int value) {
        switch (value) {
            case 1:
                return "FLAG_TRANSLATION_RESULT";
            case 2:
                return "FLAG_DICTIONARY_RESULT";
            case 4:
                return "FLAG_TRANSLITERATION_RESULT";
            case 8:
                return "FLAG_PARTIAL_RESPONSES";
            default:
                return Integer.toHexString(value);
        }
    }

    TranslationRequest(int flags, List<TranslationRequestValue> translationRequestValues, List<ViewTranslationRequest> viewTranslationRequests) {
        this.mFlags = flags;
        Preconditions.checkFlagsArgument(flags, 15);
        this.mTranslationRequestValues = translationRequestValues;
        AnnotationValidations.validate((Class<NonNull>) NonNull.class, (NonNull) null, (Object) translationRequestValues);
        this.mViewTranslationRequests = viewTranslationRequests;
        AnnotationValidations.validate((Class<NonNull>) NonNull.class, (NonNull) null, (Object) viewTranslationRequests);
    }

    public int getFlags() {
        return this.mFlags;
    }

    public List<TranslationRequestValue> getTranslationRequestValues() {
        return this.mTranslationRequestValues;
    }

    public List<ViewTranslationRequest> getViewTranslationRequests() {
        return this.mViewTranslationRequests;
    }

    public String toString() {
        return "TranslationRequest { flags = " + requestFlagsToString(this.mFlags) + ", translationRequestValues = " + this.mTranslationRequestValues + ", viewTranslationRequests = " + this.mViewTranslationRequests + " }";
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.mFlags);
        dest.writeParcelableList(this.mTranslationRequestValues, flags);
        dest.writeParcelableList(this.mViewTranslationRequests, flags);
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    TranslationRequest(Parcel in) {
        int flags = in.readInt();
        ArrayList arrayList = new ArrayList();
        in.readParcelableList(arrayList, TranslationRequestValue.class.getClassLoader(), TranslationRequestValue.class);
        ArrayList arrayList2 = new ArrayList();
        in.readParcelableList(arrayList2, ViewTranslationRequest.class.getClassLoader(), ViewTranslationRequest.class);
        this.mFlags = flags;
        Preconditions.checkFlagsArgument(flags, 15);
        this.mTranslationRequestValues = arrayList;
        AnnotationValidations.validate((Class<NonNull>) NonNull.class, (NonNull) null, (Object) arrayList);
        this.mViewTranslationRequests = arrayList2;
        AnnotationValidations.validate((Class<NonNull>) NonNull.class, (NonNull) null, (Object) arrayList2);
    }

    /* loaded from: classes3.dex */
    public static final class Builder extends BaseBuilder {
        private long mBuilderFieldsSet = 0;
        private int mFlags;
        private List<TranslationRequestValue> mTranslationRequestValues;
        private List<ViewTranslationRequest> mViewTranslationRequests;

        public Builder setFlags(int value) {
            checkNotUsed();
            this.mBuilderFieldsSet |= 1;
            this.mFlags = value;
            return this;
        }

        public Builder setTranslationRequestValues(List<TranslationRequestValue> value) {
            checkNotUsed();
            this.mBuilderFieldsSet |= 2;
            this.mTranslationRequestValues = value;
            return this;
        }

        @Override // android.view.translation.TranslationRequest.BaseBuilder
        @Deprecated
        public Builder addTranslationRequestValue(TranslationRequestValue value) {
            if (this.mTranslationRequestValues == null) {
                setTranslationRequestValues(new ArrayList());
            }
            this.mTranslationRequestValues.add(value);
            return this;
        }

        public Builder setViewTranslationRequests(List<ViewTranslationRequest> value) {
            checkNotUsed();
            this.mBuilderFieldsSet |= 4;
            this.mViewTranslationRequests = value;
            return this;
        }

        @Override // android.view.translation.TranslationRequest.BaseBuilder
        @Deprecated
        public Builder addViewTranslationRequest(ViewTranslationRequest value) {
            if (this.mViewTranslationRequests == null) {
                setViewTranslationRequests(new ArrayList());
            }
            this.mViewTranslationRequests.add(value);
            return this;
        }

        public TranslationRequest build() {
            checkNotUsed();
            long j = this.mBuilderFieldsSet | 8;
            this.mBuilderFieldsSet = j;
            if ((j & 1) == 0) {
                this.mFlags = TranslationRequest.m5446$$Nest$smdefaultFlags();
            }
            if ((this.mBuilderFieldsSet & 2) == 0) {
                this.mTranslationRequestValues = TranslationRequest.m5447$$Nest$smdefaultTranslationRequestValues();
            }
            if ((this.mBuilderFieldsSet & 4) == 0) {
                this.mViewTranslationRequests = TranslationRequest.m5448$$Nest$smdefaultViewTranslationRequests();
            }
            TranslationRequest o = new TranslationRequest(this.mFlags, this.mTranslationRequestValues, this.mViewTranslationRequests);
            return o;
        }

        private void checkNotUsed() {
            if ((this.mBuilderFieldsSet & 8) != 0) {
                throw new IllegalStateException("This Builder should not be reused. Use a new Builder instance instead");
            }
        }
    }

    @Deprecated
    private void __metadata() {
    }
}
