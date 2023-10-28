package android.hardware.display;

import android.annotation.IntRange;
import android.annotation.NonNull;
import android.hardware.display.DisplayManager;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.Surface;
import com.android.internal.util.AnnotationValidations;
import java.lang.annotation.Annotation;
/* loaded from: classes.dex */
public final class VirtualDisplayConfig implements Parcelable {
    public static final Parcelable.Creator<VirtualDisplayConfig> CREATOR = new Parcelable.Creator<VirtualDisplayConfig>() { // from class: android.hardware.display.VirtualDisplayConfig.1
        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public VirtualDisplayConfig[] newArray(int size) {
            return new VirtualDisplayConfig[size];
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public VirtualDisplayConfig createFromParcel(Parcel in) {
            return new VirtualDisplayConfig(in);
        }
    };
    private int mDensityDpi;
    private int mDisplayIdToMirror;
    private int mFlags;
    private int mHeight;
    private String mName;
    private Surface mSurface;
    private String mUniqueId;
    private int mWidth;
    private boolean mWindowManagerMirroring;

    VirtualDisplayConfig(String name, int width, int height, int densityDpi, int flags, Surface surface, String uniqueId, int displayIdToMirror, boolean windowManagerMirroring) {
        this.mFlags = 0;
        this.mSurface = null;
        this.mUniqueId = null;
        this.mDisplayIdToMirror = 0;
        this.mWindowManagerMirroring = false;
        this.mName = name;
        AnnotationValidations.validate((Class<NonNull>) NonNull.class, (NonNull) null, (Object) name);
        this.mWidth = width;
        AnnotationValidations.validate((Class<IntRange>) IntRange.class, (IntRange) null, width, "from", 1L);
        this.mHeight = height;
        AnnotationValidations.validate((Class<IntRange>) IntRange.class, (IntRange) null, height, "from", 1L);
        this.mDensityDpi = densityDpi;
        AnnotationValidations.validate((Class<IntRange>) IntRange.class, (IntRange) null, densityDpi, "from", 1L);
        this.mFlags = flags;
        AnnotationValidations.validate((Class<? extends Annotation>) DisplayManager.VirtualDisplayFlag.class, (Annotation) null, flags);
        this.mSurface = surface;
        this.mUniqueId = uniqueId;
        this.mDisplayIdToMirror = displayIdToMirror;
        this.mWindowManagerMirroring = windowManagerMirroring;
    }

    public String getName() {
        return this.mName;
    }

    public int getWidth() {
        return this.mWidth;
    }

    public int getHeight() {
        return this.mHeight;
    }

    public int getDensityDpi() {
        return this.mDensityDpi;
    }

    public int getFlags() {
        return this.mFlags;
    }

    public Surface getSurface() {
        return this.mSurface;
    }

    public String getUniqueId() {
        return this.mUniqueId;
    }

    public int getDisplayIdToMirror() {
        return this.mDisplayIdToMirror;
    }

    public boolean isWindowManagerMirroring() {
        return this.mWindowManagerMirroring;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        int flg = this.mWindowManagerMirroring ? 0 | 256 : 0;
        if (this.mSurface != null) {
            flg |= 32;
        }
        if (this.mUniqueId != null) {
            flg |= 64;
        }
        dest.writeInt(flg);
        dest.writeString(this.mName);
        dest.writeInt(this.mWidth);
        dest.writeInt(this.mHeight);
        dest.writeInt(this.mDensityDpi);
        dest.writeInt(this.mFlags);
        Surface surface = this.mSurface;
        if (surface != null) {
            dest.writeTypedObject(surface, flags);
        }
        String str = this.mUniqueId;
        if (str != null) {
            dest.writeString(str);
        }
        dest.writeInt(this.mDisplayIdToMirror);
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    VirtualDisplayConfig(Parcel in) {
        this.mFlags = 0;
        this.mSurface = null;
        this.mUniqueId = null;
        this.mDisplayIdToMirror = 0;
        this.mWindowManagerMirroring = false;
        int flg = in.readInt();
        boolean windowManagerMirroring = (flg & 256) != 0;
        String name = in.readString();
        int width = in.readInt();
        int height = in.readInt();
        int densityDpi = in.readInt();
        int flags = in.readInt();
        Surface surface = (flg & 32) == 0 ? null : (Surface) in.readTypedObject(Surface.CREATOR);
        String uniqueId = (flg & 64) == 0 ? null : in.readString();
        int displayIdToMirror = in.readInt();
        this.mName = name;
        AnnotationValidations.validate((Class<NonNull>) NonNull.class, (NonNull) null, (Object) name);
        this.mWidth = width;
        AnnotationValidations.validate((Class<IntRange>) IntRange.class, (IntRange) null, width, "from", 1L);
        this.mHeight = height;
        AnnotationValidations.validate((Class<IntRange>) IntRange.class, (IntRange) null, height, "from", 1L);
        this.mDensityDpi = densityDpi;
        AnnotationValidations.validate((Class<IntRange>) IntRange.class, (IntRange) null, densityDpi, "from", 1L);
        this.mFlags = flags;
        AnnotationValidations.validate((Class<? extends Annotation>) DisplayManager.VirtualDisplayFlag.class, (Annotation) null, flags);
        this.mSurface = surface;
        this.mUniqueId = uniqueId;
        this.mDisplayIdToMirror = displayIdToMirror;
        this.mWindowManagerMirroring = windowManagerMirroring;
    }

    /* loaded from: classes.dex */
    public static final class Builder {
        private long mBuilderFieldsSet = 0;
        private int mDensityDpi;
        private int mDisplayIdToMirror;
        private int mFlags;
        private int mHeight;
        private String mName;
        private Surface mSurface;
        private String mUniqueId;
        private int mWidth;
        private boolean mWindowManagerMirroring;

        public Builder(String name, int width, int height, int densityDpi) {
            this.mName = name;
            AnnotationValidations.validate((Class<NonNull>) NonNull.class, (NonNull) null, (Object) name);
            this.mWidth = width;
            AnnotationValidations.validate((Class<IntRange>) IntRange.class, (IntRange) null, width, "from", 1L);
            this.mHeight = height;
            AnnotationValidations.validate((Class<IntRange>) IntRange.class, (IntRange) null, height, "from", 1L);
            this.mDensityDpi = densityDpi;
            AnnotationValidations.validate((Class<IntRange>) IntRange.class, (IntRange) null, densityDpi, "from", 1L);
        }

        public Builder setName(String value) {
            checkNotUsed();
            this.mBuilderFieldsSet |= 1;
            this.mName = value;
            return this;
        }

        public Builder setWidth(int value) {
            checkNotUsed();
            this.mBuilderFieldsSet |= 2;
            this.mWidth = value;
            return this;
        }

        public Builder setHeight(int value) {
            checkNotUsed();
            this.mBuilderFieldsSet |= 4;
            this.mHeight = value;
            return this;
        }

        public Builder setDensityDpi(int value) {
            checkNotUsed();
            this.mBuilderFieldsSet |= 8;
            this.mDensityDpi = value;
            return this;
        }

        public Builder setFlags(int value) {
            checkNotUsed();
            this.mBuilderFieldsSet |= 16;
            this.mFlags = value;
            return this;
        }

        public Builder setSurface(Surface value) {
            checkNotUsed();
            this.mBuilderFieldsSet |= 32;
            this.mSurface = value;
            return this;
        }

        public Builder setUniqueId(String value) {
            checkNotUsed();
            this.mBuilderFieldsSet |= 64;
            this.mUniqueId = value;
            return this;
        }

        public Builder setDisplayIdToMirror(int value) {
            checkNotUsed();
            this.mBuilderFieldsSet |= 128;
            this.mDisplayIdToMirror = value;
            return this;
        }

        public Builder setWindowManagerMirroring(boolean value) {
            checkNotUsed();
            this.mBuilderFieldsSet |= 256;
            this.mWindowManagerMirroring = value;
            return this;
        }

        public VirtualDisplayConfig build() {
            checkNotUsed();
            long j = this.mBuilderFieldsSet | 512;
            this.mBuilderFieldsSet = j;
            if ((16 & j) == 0) {
                this.mFlags = 0;
            }
            if ((32 & j) == 0) {
                this.mSurface = null;
            }
            if ((64 & j) == 0) {
                this.mUniqueId = null;
            }
            if ((128 & j) == 0) {
                this.mDisplayIdToMirror = 0;
            }
            if ((j & 256) == 0) {
                this.mWindowManagerMirroring = false;
            }
            VirtualDisplayConfig o = new VirtualDisplayConfig(this.mName, this.mWidth, this.mHeight, this.mDensityDpi, this.mFlags, this.mSurface, this.mUniqueId, this.mDisplayIdToMirror, this.mWindowManagerMirroring);
            return o;
        }

        private void checkNotUsed() {
            if ((this.mBuilderFieldsSet & 512) != 0) {
                throw new IllegalStateException("This Builder should not be reused. Use a new Builder instance instead");
            }
        }
    }

    @Deprecated
    private void __metadata() {
    }
}
