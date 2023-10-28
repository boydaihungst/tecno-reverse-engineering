package com.android.server.wm.utils;

import android.graphics.Color;
import android.graphics.ColorSpace;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.HardwareBuffer;
import android.media.Image;
import android.media.ImageReader;
import android.view.Display;
import android.view.SurfaceControl;
import java.nio.ByteBuffer;
import java.util.Arrays;
/* loaded from: classes2.dex */
public class RotationAnimationUtils {
    public static boolean hasProtectedContent(HardwareBuffer hardwareBuffer) {
        return (hardwareBuffer.getUsage() & 16384) == 16384;
    }

    public static float getMedianBorderLuma(HardwareBuffer hardwareBuffer, ColorSpace colorSpace) {
        if (hardwareBuffer == null || hardwareBuffer.getFormat() != 1 || hasProtectedContent(hardwareBuffer)) {
            return 0.0f;
        }
        ImageReader ir = ImageReader.newInstance(hardwareBuffer.getWidth(), hardwareBuffer.getHeight(), hardwareBuffer.getFormat(), 1);
        ir.getSurface().attachAndQueueBufferWithColorSpace(hardwareBuffer, colorSpace);
        Image image = ir.acquireLatestImage();
        if (image == null || image.getPlanes().length == 0) {
            return 0.0f;
        }
        Image.Plane plane = image.getPlanes()[0];
        ByteBuffer buffer = plane.getBuffer();
        int width = image.getWidth();
        int height = image.getHeight();
        int pixelStride = plane.getPixelStride();
        int rowStride = plane.getRowStride();
        float[] borderLumas = new float[(width * 2) + (height * 2)];
        int l = 0;
        for (int x = 0; x < width; x++) {
            int l2 = l + 1;
            borderLumas[l] = getPixelLuminance(buffer, x, 0, pixelStride, rowStride);
            l = l2 + 1;
            borderLumas[l2] = getPixelLuminance(buffer, x, height - 1, pixelStride, rowStride);
        }
        for (int y = 0; y < height; y++) {
            int l3 = l + 1;
            borderLumas[l] = getPixelLuminance(buffer, 0, y, pixelStride, rowStride);
            l = l3 + 1;
            borderLumas[l3] = getPixelLuminance(buffer, width - 1, y, pixelStride, rowStride);
        }
        ir.close();
        Arrays.sort(borderLumas);
        return borderLumas[borderLumas.length / 2];
    }

    private static float getPixelLuminance(ByteBuffer buffer, int x, int y, int pixelStride, int rowStride) {
        int offset = (y * rowStride) + (x * pixelStride);
        int pixel = 0 | ((buffer.get(offset) & 255) << 16);
        return Color.valueOf(pixel | ((buffer.get(offset + 1) & 255) << 8) | (buffer.get(offset + 2) & 255) | ((buffer.get(offset + 3) & 255) << 24)).luminance();
    }

    public static float getLumaOfSurfaceControl(Display display, SurfaceControl surfaceControl) {
        if (surfaceControl == null) {
            return 0.0f;
        }
        Point size = new Point();
        display.getSize(size);
        Rect crop = new Rect(0, 0, size.x, size.y);
        SurfaceControl.ScreenshotHardwareBuffer buffer = SurfaceControl.captureLayers(surfaceControl, crop, 1.0f);
        if (buffer == null) {
            return 0.0f;
        }
        return getMedianBorderLuma(buffer.getHardwareBuffer(), buffer.getColorSpace());
    }

    public static void createRotationMatrix(int rotation, int width, int height, Matrix outMatrix) {
        switch (rotation) {
            case 0:
                outMatrix.reset();
                return;
            case 1:
                outMatrix.setRotate(90.0f, 0.0f, 0.0f);
                outMatrix.postTranslate(height, 0.0f);
                return;
            case 2:
                outMatrix.setRotate(180.0f, 0.0f, 0.0f);
                outMatrix.postTranslate(width, height);
                return;
            case 3:
                outMatrix.setRotate(270.0f, 0.0f, 0.0f);
                outMatrix.postTranslate(0.0f, width);
                return;
            default:
                return;
        }
    }
}
