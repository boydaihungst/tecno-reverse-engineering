package android.graphics;

import java.io.OutputStream;
/* loaded from: classes.dex */
public class YuvImage {
    private static final int WORKING_COMPRESS_STORAGE = 4096;
    private byte[] mData;
    private int mFormat;
    private int mHeight;
    private int[] mStrides;
    private int mWidth;

    private static native boolean nativeCompressToJpeg(byte[] bArr, int i, int i2, int i3, int[] iArr, int[] iArr2, int i4, OutputStream outputStream, byte[] bArr2);

    public YuvImage(byte[] yuv, int format, int width, int height, int[] strides) {
        if (format != 17 && format != 20) {
            throw new IllegalArgumentException("only support ImageFormat.NV21 and ImageFormat.YUY2 for now");
        }
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("width and height must large than 0");
        }
        if (yuv == null) {
            throw new IllegalArgumentException("yuv cannot be null");
        }
        if (strides == null) {
            this.mStrides = calculateStrides(width, format);
        } else {
            this.mStrides = strides;
        }
        this.mData = yuv;
        this.mFormat = format;
        this.mWidth = width;
        this.mHeight = height;
    }

    public boolean compressToJpeg(Rect rectangle, int quality, OutputStream stream) {
        Rect wholeImage = new Rect(0, 0, this.mWidth, this.mHeight);
        if (!wholeImage.contains(rectangle)) {
            throw new IllegalArgumentException("rectangle is not inside the image");
        }
        if (quality < 0 || quality > 100) {
            throw new IllegalArgumentException("quality must be 0..100");
        }
        if (stream == null) {
            throw new IllegalArgumentException("stream cannot be null");
        }
        adjustRectangle(rectangle);
        int[] offsets = calculateOffsets(rectangle.left, rectangle.top);
        return nativeCompressToJpeg(this.mData, this.mFormat, rectangle.width(), rectangle.height(), offsets, this.mStrides, quality, stream, new byte[4096]);
    }

    public byte[] getYuvData() {
        return this.mData;
    }

    public int getYuvFormat() {
        return this.mFormat;
    }

    public int[] getStrides() {
        return this.mStrides;
    }

    public int getWidth() {
        return this.mWidth;
    }

    public int getHeight() {
        return this.mHeight;
    }

    int[] calculateOffsets(int left, int top) {
        int i = this.mFormat;
        if (i == 17) {
            int[] iArr = this.mStrides;
            int[] offsets = {(iArr[0] * top) + left, (this.mHeight * iArr[0]) + ((top / 2) * iArr[1]) + ((left / 2) * 2)};
            return offsets;
        } else if (i == 20) {
            int[] offsets2 = {(this.mStrides[0] * top) + ((left / 2) * 4)};
            return offsets2;
        } else {
            return null;
        }
    }

    private int[] calculateStrides(int width, int format) {
        if (format == 17) {
            int[] strides = {width, width};
            return strides;
        } else if (format == 20) {
            int[] strides2 = {width * 2};
            return strides2;
        } else {
            return null;
        }
    }

    private void adjustRectangle(Rect rect) {
        int width = rect.width();
        int height = rect.height();
        if (this.mFormat == 17) {
            width &= -2;
            rect.left &= -2;
            rect.top &= -2;
            rect.right = rect.left + width;
            rect.bottom = rect.top + (height & (-2));
        }
        if (this.mFormat == 20) {
            rect.left &= -2;
            rect.right = rect.left + (width & (-2));
        }
    }
}
