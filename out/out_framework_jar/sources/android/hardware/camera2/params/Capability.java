package android.hardware.camera2.params;

import android.hardware.camera2.utils.HashCodeHelpers;
import android.util.Range;
import android.util.Size;
import com.android.internal.util.Preconditions;
/* loaded from: classes.dex */
public final class Capability {
    public static final int COUNT = 3;
    private final int mMaxStreamingHeight;
    private final int mMaxStreamingWidth;
    private final float mMaxZoomRatio;
    private final float mMinZoomRatio;
    private final int mMode;

    public Capability(int mode, int maxStreamingWidth, int maxStreamingHeight, float minZoomRatio, float maxZoomRatio) {
        this.mMode = mode;
        this.mMaxStreamingWidth = Preconditions.checkArgumentNonnegative(maxStreamingWidth, "maxStreamingWidth must be nonnegative");
        this.mMaxStreamingHeight = Preconditions.checkArgumentNonnegative(maxStreamingHeight, "maxStreamingHeight must be nonnegative");
        if (minZoomRatio > maxZoomRatio) {
            throw new IllegalArgumentException("minZoomRatio " + minZoomRatio + " is greater than maxZoomRatio " + maxZoomRatio);
        }
        this.mMinZoomRatio = Preconditions.checkArgumentPositive(minZoomRatio, "minZoomRatio must be positive");
        this.mMaxZoomRatio = Preconditions.checkArgumentPositive(maxZoomRatio, "maxZoomRatio must be positive");
    }

    public int getMode() {
        return this.mMode;
    }

    public Size getMaxStreamingSize() {
        return new Size(this.mMaxStreamingWidth, this.mMaxStreamingHeight);
    }

    public Range<Float> getZoomRatioRange() {
        return new Range<>(Float.valueOf(this.mMinZoomRatio), Float.valueOf(this.mMaxZoomRatio));
    }

    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Capability)) {
            return false;
        }
        Capability other = (Capability) obj;
        if (this.mMode != other.mMode || this.mMaxStreamingWidth != other.mMaxStreamingWidth || this.mMaxStreamingHeight != other.mMaxStreamingHeight || this.mMinZoomRatio != other.mMinZoomRatio || this.mMaxZoomRatio != other.mMaxZoomRatio) {
            return false;
        }
        return true;
    }

    public int hashCode() {
        return HashCodeHelpers.hashCode(this.mMode, this.mMaxStreamingWidth, this.mMaxStreamingHeight, this.mMinZoomRatio, this.mMaxZoomRatio);
    }

    public String toString() {
        return String.format("(mode:%d, maxStreamingSize:%d x %d, zoomRatio: %f-%f)", Integer.valueOf(this.mMode), Integer.valueOf(this.mMaxStreamingWidth), Integer.valueOf(this.mMaxStreamingHeight), Float.valueOf(this.mMinZoomRatio), Float.valueOf(this.mMaxZoomRatio));
    }
}
