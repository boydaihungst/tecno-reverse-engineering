package com.android.server.pm;

import android.content.pm.ShortcutInfo;
import android.graphics.Bitmap;
import android.graphics.drawable.Icon;
import android.os.StrictMode;
import android.os.SystemClock;
import android.util.Log;
import android.util.Slog;
import com.android.server.pm.ShortcutService;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Deque;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import libcore.io.IoUtils;
/* loaded from: classes2.dex */
public class ShortcutBitmapSaver {
    private static final boolean ADD_DELAY_BEFORE_SAVE_FOR_TEST = false;
    private static final boolean DEBUG = false;
    private static final long SAVE_DELAY_MS_FOR_TEST = 1000;
    private static final String TAG = "ShortcutService";
    private final long SAVE_WAIT_TIMEOUT_MS = 5000;
    private final Executor mExecutor = new ThreadPoolExecutor(0, 1, 60, TimeUnit.SECONDS, new LinkedBlockingQueue());
    private final Deque<PendingItem> mPendingItems = new LinkedBlockingDeque();
    private final Runnable mRunnable = new Runnable() { // from class: com.android.server.pm.ShortcutBitmapSaver$$ExternalSyntheticLambda1
        @Override // java.lang.Runnable
        public final void run() {
            ShortcutBitmapSaver.this.m5616lambda$new$1$comandroidserverpmShortcutBitmapSaver();
        }
    };
    private final ShortcutService mService;

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static class PendingItem {
        public final byte[] bytes;
        private final long mInstantiatedUptimeMillis;
        public final ShortcutInfo shortcut;

        private PendingItem(ShortcutInfo shortcut, byte[] bytes) {
            this.shortcut = shortcut;
            this.bytes = bytes;
            this.mInstantiatedUptimeMillis = SystemClock.uptimeMillis();
        }

        public String toString() {
            return "PendingItem{size=" + this.bytes.length + " age=" + (SystemClock.uptimeMillis() - this.mInstantiatedUptimeMillis) + "ms shortcut=" + this.shortcut.toInsecureString() + "}";
        }
    }

    public ShortcutBitmapSaver(ShortcutService service) {
        this.mService = service;
    }

    public boolean waitForAllSavesLocked() {
        final CountDownLatch latch = new CountDownLatch(1);
        this.mExecutor.execute(new Runnable() { // from class: com.android.server.pm.ShortcutBitmapSaver$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                latch.countDown();
            }
        });
        try {
            return latch.await(5000L, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Slog.w(TAG, "interrupted");
            return false;
        }
    }

    public String getBitmapPathMayWaitLocked(ShortcutInfo shortcut) {
        boolean success = waitForAllSavesLocked();
        if (!success) {
            this.mService.wtf("Timed out waiting on saving bitmaps.");
        }
        if (success && shortcut.hasIconFile()) {
            return shortcut.getBitmapPath();
        }
        return null;
    }

    public void removeIcon(ShortcutInfo shortcut) {
        shortcut.setIconResourceId(0);
        shortcut.setIconResName(null);
        shortcut.setBitmapPath(null);
        shortcut.setIconUri(null);
        shortcut.clearFlags(35340);
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [200=4] */
    public void saveBitmapLocked(ShortcutInfo shortcut, int maxDimension, Bitmap.CompressFormat format, int quality) {
        Icon icon = shortcut.getIcon();
        Objects.requireNonNull(icon);
        Bitmap original = icon.getBitmap();
        if (original == null) {
            Log.e(TAG, "Missing icon: " + shortcut);
            return;
        }
        StrictMode.ThreadPolicy oldPolicy = StrictMode.getThreadPolicy();
        try {
            try {
                StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder(oldPolicy).permitCustomSlowCalls().build());
                Bitmap shrunk = ShortcutService.shrinkBitmap(original, maxDimension);
                try {
                    ByteArrayOutputStream out = new ByteArrayOutputStream(65536);
                    if (!shrunk.compress(format, quality, out)) {
                        Slog.wtf(TAG, "Unable to compress bitmap");
                    }
                    out.flush();
                    byte[] bytes = out.toByteArray();
                    out.close();
                    out.close();
                    StrictMode.setThreadPolicy(oldPolicy);
                    shortcut.addFlags(2056);
                    if (icon.getType() == 5) {
                        shortcut.addFlags(512);
                    }
                    PendingItem item = new PendingItem(shortcut, bytes);
                    synchronized (this.mPendingItems) {
                        this.mPendingItems.add(item);
                    }
                    this.mExecutor.execute(this.mRunnable);
                } finally {
                    if (shrunk != original) {
                        shrunk.recycle();
                    }
                }
            } catch (IOException | OutOfMemoryError | RuntimeException e) {
                Slog.wtf(TAG, "Unable to write bitmap to file", e);
                StrictMode.setThreadPolicy(oldPolicy);
            }
        } catch (Throwable th) {
            StrictMode.setThreadPolicy(oldPolicy);
            throw th;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$new$1$com-android-server-pm-ShortcutBitmapSaver  reason: not valid java name */
    public /* synthetic */ void m5616lambda$new$1$comandroidserverpmShortcutBitmapSaver() {
        do {
        } while (processPendingItems());
    }

    /* JADX DEBUG: Another duplicated slice has different insns count: {[IF]}, finally: {[IF, INVOKE, INVOKE, INVOKE, IF, INVOKE] complete} */
    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [301=5, 302=5, 303=5, 307=5] */
    private boolean processPendingItems() {
        ShortcutInfo shortcut = null;
        try {
            synchronized (this.mPendingItems) {
                if (this.mPendingItems.size() == 0) {
                    if (0 != 0) {
                        if (shortcut.getBitmapPath() == null) {
                            removeIcon(null);
                        }
                        shortcut.clearFlags(2048);
                    }
                    return false;
                }
                PendingItem item = this.mPendingItems.pop();
                shortcut = item.shortcut;
                if (!shortcut.isIconPendingSave()) {
                    if (shortcut != null) {
                        if (shortcut.getBitmapPath() == null) {
                            removeIcon(shortcut);
                        }
                        shortcut.clearFlags(2048);
                    }
                    return true;
                }
                File file = null;
                try {
                    ShortcutService.FileOutputStreamWithPath out = this.mService.openIconFileForWrite(shortcut.getUserId(), shortcut);
                    File file2 = out.getFile();
                    try {
                        out.write(item.bytes);
                        IoUtils.closeQuietly(out);
                        String path = file2.getAbsolutePath();
                        shortcut.setBitmapPath(path);
                        if (shortcut != null) {
                            if (shortcut.getBitmapPath() == null) {
                                removeIcon(shortcut);
                            }
                            shortcut.clearFlags(2048);
                        }
                        return true;
                    } catch (Throwable th) {
                        IoUtils.closeQuietly(out);
                        throw th;
                    }
                } catch (IOException | RuntimeException e) {
                    Slog.e(TAG, "Unable to write bitmap to file", e);
                    if (0 != 0 && file.exists()) {
                        file.delete();
                    }
                    if (shortcut != null) {
                        if (shortcut.getBitmapPath() == null) {
                            removeIcon(shortcut);
                        }
                        shortcut.clearFlags(2048);
                    }
                    return true;
                }
            }
        } catch (Throwable th2) {
            if (shortcut != null) {
                if (shortcut.getBitmapPath() == null) {
                    removeIcon(shortcut);
                }
                shortcut.clearFlags(2048);
            }
            throw th2;
        }
    }

    public void dumpLocked(PrintWriter pw, String prefix) {
        synchronized (this.mPendingItems) {
            int N = this.mPendingItems.size();
            pw.print(prefix);
            pw.println("Pending saves: Num=" + N + " Executor=" + this.mExecutor);
            for (PendingItem item : this.mPendingItems) {
                pw.print(prefix);
                pw.print("  ");
                pw.println(item);
            }
        }
    }
}
