package com.android.server.wm;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.HardwareBuffer;
import android.util.Slog;
import android.window.TaskSnapshot;
import com.android.server.wm.nano.WindowManagerProtos;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class TaskSnapshotLoader {
    private static final String TAG = "WindowManager";
    private final TaskSnapshotPersister mPersister;

    /* JADX INFO: Access modifiers changed from: package-private */
    public TaskSnapshotLoader(TaskSnapshotPersister persister) {
        this.mPersister = persister;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public static class PreRLegacySnapshotConfig {
        final boolean mForceLoadReducedJpeg;
        final float mScale;

        PreRLegacySnapshotConfig(float scale, boolean forceLoadReducedJpeg) {
            this.mScale = scale;
            this.mForceLoadReducedJpeg = forceLoadReducedJpeg;
        }
    }

    PreRLegacySnapshotConfig getLegacySnapshotConfig(int taskWidth, float legacyScale, boolean highResFileExists, boolean loadLowResolutionBitmap) {
        float preRLegacyScale = 0.0f;
        boolean forceLoadReducedJpeg = false;
        boolean isPreQLegacyProto = true;
        boolean isPreRLegacySnapshot = taskWidth == 0;
        if (!isPreRLegacySnapshot) {
            return null;
        }
        if (!isPreRLegacySnapshot || Float.compare(legacyScale, 0.0f) != 0) {
            isPreQLegacyProto = false;
        }
        if (isPreQLegacyProto) {
            if (ActivityManager.isLowRamDeviceStatic() && !highResFileExists) {
                preRLegacyScale = 0.6f;
                forceLoadReducedJpeg = true;
            } else {
                preRLegacyScale = loadLowResolutionBitmap ? 0.5f : 1.0f;
            }
        } else if (isPreRLegacySnapshot) {
            if (ActivityManager.isLowRamDeviceStatic()) {
                preRLegacyScale = legacyScale;
                forceLoadReducedJpeg = true;
            } else {
                preRLegacyScale = loadLowResolutionBitmap ? 0.5f * legacyScale : legacyScale;
            }
        }
        return new PreRLegacySnapshotConfig(preRLegacyScale, forceLoadReducedJpeg);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: Removed duplicated region for block: B:19:0x005d A[RETURN] */
    /* JADX WARN: Removed duplicated region for block: B:20:0x005e A[Catch: IOException -> 0x0178, TryCatch #0 {IOException -> 0x0178, blocks: (B:5:0x0019, B:7:0x003e, B:17:0x0055, B:20:0x005e, B:22:0x006c, B:24:0x0070, B:26:0x0075, B:28:0x0082, B:30:0x009d, B:32:0x00aa, B:34:0x00c5, B:36:0x00cd, B:38:0x00e8, B:40:0x00f0, B:42:0x0116, B:41:0x010b, B:25:0x0073, B:16:0x004f), top: B:52:0x0019 }] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public TaskSnapshot loadTask(int taskId, int userId, boolean loadLowResolutionBitmap) {
        String str;
        File lowResolutionBitmapFile;
        File bitmapFile;
        Bitmap.Config config;
        Point taskSize;
        File protoFile = this.mPersister.getProtoFile(taskId, userId);
        if (!protoFile.exists()) {
            return null;
        }
        try {
            byte[] bytes = Files.readAllBytes(protoFile.toPath());
            WindowManagerProtos.TaskSnapshotProto proto = WindowManagerProtos.TaskSnapshotProto.parseFrom(bytes);
            File highResBitmap = this.mPersister.getHighResolutionBitmapFile(taskId, userId);
            PreRLegacySnapshotConfig legacyConfig = getLegacySnapshotConfig(proto.taskWidth, proto.legacyScale, highResBitmap.exists(), loadLowResolutionBitmap);
            boolean forceLoadReducedJpeg = legacyConfig != null && legacyConfig.mForceLoadReducedJpeg;
            if (!loadLowResolutionBitmap && !forceLoadReducedJpeg) {
                lowResolutionBitmapFile = highResBitmap;
                bitmapFile = lowResolutionBitmapFile;
                if (bitmapFile.exists()) {
                    return null;
                }
                BitmapFactory.Options options = new BitmapFactory.Options();
                if (this.mPersister.use16BitFormat() && !proto.isTranslucent) {
                    config = Bitmap.Config.RGB_565;
                } else {
                    config = Bitmap.Config.ARGB_8888;
                }
                options.inPreferredConfig = config;
                Bitmap bitmap = BitmapFactory.decodeFile(bitmapFile.getPath(), options);
                if (bitmap != null) {
                    Bitmap hwBitmap = bitmap.copy(Bitmap.Config.HARDWARE, false);
                    bitmap.recycle();
                    if (hwBitmap == null) {
                        Slog.w("WindowManager", "Failed to create hardware bitmap: " + bitmapFile.getPath());
                        return null;
                    }
                    HardwareBuffer buffer = hwBitmap.getHardwareBuffer();
                    if (buffer == null) {
                        Slog.w("WindowManager", "Failed to retrieve gralloc buffer for bitmap: " + bitmapFile.getPath());
                        return null;
                    }
                    ComponentName topActivityComponent = ComponentName.unflattenFromString(proto.topActivityComponent);
                    if (legacyConfig != null) {
                        int taskWidth = (int) (hwBitmap.getWidth() / legacyConfig.mScale);
                        int taskHeight = (int) (hwBitmap.getHeight() / legacyConfig.mScale);
                        Point taskSize2 = new Point(taskWidth, taskHeight);
                        taskSize = taskSize2;
                    } else {
                        taskSize = new Point(proto.taskWidth, proto.taskHeight);
                    }
                    str = "WindowManager";
                    try {
                        return new TaskSnapshot(proto.id, topActivityComponent, buffer, hwBitmap.getColorSpace(), proto.orientation, proto.rotation, taskSize, new Rect(proto.insetLeft, proto.insetTop, proto.insetRight, proto.insetBottom), new Rect(proto.letterboxInsetLeft, proto.letterboxInsetTop, proto.letterboxInsetRight, proto.letterboxInsetBottom), loadLowResolutionBitmap, proto.isRealSnapshot, proto.windowingMode, proto.appearance, proto.isTranslucent, false);
                    } catch (IOException e) {
                        Slog.w(str, "Unable to load task snapshot data for taskId=" + taskId);
                        return null;
                    }
                }
                Slog.w("WindowManager", "Failed to load bitmap: " + bitmapFile.getPath());
                return null;
            }
            lowResolutionBitmapFile = this.mPersister.getLowResolutionBitmapFile(taskId, userId);
            bitmapFile = lowResolutionBitmapFile;
            if (bitmapFile.exists()) {
            }
        } catch (IOException e2) {
            str = "WindowManager";
        }
    }
}
