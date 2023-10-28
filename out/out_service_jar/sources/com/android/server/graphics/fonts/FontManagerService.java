package com.android.server.graphics.fonts;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.fonts.FontFamily;
import android.graphics.fonts.FontUpdateRequest;
import android.graphics.fonts.SystemFonts;
import android.os.ParcelFileDescriptor;
import android.os.ResultReceiver;
import android.os.SharedMemory;
import android.os.ShellCallback;
import android.system.ErrnoException;
import android.text.FontConfig;
import android.util.AndroidException;
import android.util.ArrayMap;
import android.util.IndentingPrintWriter;
import android.util.Slog;
import com.android.internal.graphics.fonts.IFontManager;
import com.android.internal.security.VerityUtils;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.Preconditions;
import com.android.server.LocalServices;
import com.android.server.SystemService;
import com.android.server.graphics.fonts.UpdatableFontDir;
import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.DirectByteBuffer;
import java.nio.NioUtils;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
/* loaded from: classes.dex */
public final class FontManagerService extends IFontManager.Stub {
    private static final String CONFIG_XML_FILE = "/data/fonts/config/config.xml";
    private static final String FONT_FILES_DIR = "/data/fonts/files";
    private static final String TAG = "FontManagerService";
    private final Context mContext;
    private SharedMemory mSerializedFontMap;
    private final Object mSerializedFontMapLock;
    private final UpdatableFontDir mUpdatableFontDir;
    private final Object mUpdatableFontDirLock;

    public FontConfig getFontConfig() {
        getContext().enforceCallingPermission("android.permission.UPDATE_FONTS", "UPDATE_FONTS permission required.");
        return getSystemFontConfig();
    }

    public int updateFontFamily(List<FontUpdateRequest> requests, int baseVersion) {
        try {
            Preconditions.checkArgumentNonnegative(baseVersion);
            Objects.requireNonNull(requests);
            getContext().enforceCallingPermission("android.permission.UPDATE_FONTS", "UPDATE_FONTS permission required.");
            update(baseVersion, requests);
            return 0;
        } catch (SystemFontException e) {
            Slog.e(TAG, "Failed to update font family", e);
            return e.getErrorCode();
        } finally {
            closeFileDescriptors(requests);
        }
    }

    private static void closeFileDescriptors(List<FontUpdateRequest> requests) {
        ParcelFileDescriptor fd;
        if (requests == null) {
            return;
        }
        for (FontUpdateRequest request : requests) {
            if (request != null && (fd = request.getFd()) != null) {
                try {
                    fd.close();
                } catch (IOException e) {
                    Slog.w(TAG, "Failed to close fd", e);
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class SystemFontException extends AndroidException {
        private final int mErrorCode;

        /* JADX INFO: Access modifiers changed from: package-private */
        public SystemFontException(int errorCode, String msg, Throwable cause) {
            super(msg, cause);
            this.mErrorCode = errorCode;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public SystemFontException(int errorCode, String msg) {
            super(msg);
            this.mErrorCode = errorCode;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public int getErrorCode() {
            return this.mErrorCode;
        }
    }

    /* loaded from: classes.dex */
    public static final class Lifecycle extends SystemService {
        private final FontManagerService mService;

        public Lifecycle(Context context, boolean safeMode) {
            super(context);
            this.mService = new FontManagerService(context, safeMode);
        }

        @Override // com.android.server.SystemService
        public void onStart() {
            LocalServices.addService(FontManagerInternal.class, new FontManagerInternal() { // from class: com.android.server.graphics.fonts.FontManagerService.Lifecycle.1
                @Override // com.android.server.graphics.fonts.FontManagerInternal
                public SharedMemory getSerializedSystemFontMap() {
                    return Lifecycle.this.mService.getCurrentFontMap();
                }
            });
            publishBinderService("font", this.mService);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class FsverityUtilImpl implements UpdatableFontDir.FsverityUtil {
        private FsverityUtilImpl() {
        }

        @Override // com.android.server.graphics.fonts.UpdatableFontDir.FsverityUtil
        public boolean hasFsverity(String filePath) {
            return VerityUtils.hasFsverity(filePath);
        }

        @Override // com.android.server.graphics.fonts.UpdatableFontDir.FsverityUtil
        public void setUpFsverity(String filePath, byte[] pkcs7Signature) throws IOException {
            VerityUtils.setUpFsverity(filePath, pkcs7Signature);
        }

        @Override // com.android.server.graphics.fonts.UpdatableFontDir.FsverityUtil
        public boolean rename(File src, File dest) {
            return src.renameTo(dest);
        }
    }

    private FontManagerService(Context context, boolean safeMode) {
        this.mUpdatableFontDirLock = new Object();
        this.mSerializedFontMapLock = new Object();
        this.mSerializedFontMap = null;
        if (safeMode) {
            Slog.i(TAG, "Entering safe mode. Deleting all font updates.");
            UpdatableFontDir.deleteAllFiles(new File(FONT_FILES_DIR), new File(CONFIG_XML_FILE));
        }
        this.mContext = context;
        this.mUpdatableFontDir = createUpdatableFontDir(safeMode);
        initialize();
    }

    private static UpdatableFontDir createUpdatableFontDir(boolean safeMode) {
        if (safeMode || !VerityUtils.isFsVeritySupported()) {
            return null;
        }
        return new UpdatableFontDir(new File(FONT_FILES_DIR), new OtfFontFileParser(), new FsverityUtilImpl(), new File(CONFIG_XML_FILE));
    }

    private void initialize() {
        synchronized (this.mUpdatableFontDirLock) {
            UpdatableFontDir updatableFontDir = this.mUpdatableFontDir;
            if (updatableFontDir == null) {
                setSerializedFontMap(serializeSystemServerFontMap());
                return;
            }
            updatableFontDir.loadFontFileMap();
            updateSerializedFontMap();
        }
    }

    public Context getContext() {
        return this.mContext;
    }

    SharedMemory getCurrentFontMap() {
        SharedMemory sharedMemory;
        synchronized (this.mSerializedFontMapLock) {
            sharedMemory = this.mSerializedFontMap;
        }
        return sharedMemory;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void update(int baseVersion, List<FontUpdateRequest> requests) throws SystemFontException {
        if (this.mUpdatableFontDir == null) {
            throw new SystemFontException(-7, "The font updater is disabled.");
        }
        synchronized (this.mUpdatableFontDirLock) {
            if (baseVersion != -1) {
                if (this.mUpdatableFontDir.getConfigVersion() != baseVersion) {
                    throw new SystemFontException(-8, "The base config version is older than current.");
                }
            }
            this.mUpdatableFontDir.update(requests);
            updateSerializedFontMap();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void clearUpdates() {
        UpdatableFontDir.deleteAllFiles(new File(FONT_FILES_DIR), new File(CONFIG_XML_FILE));
        initialize();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void restart() {
        initialize();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Map<String, File> getFontFileMap() {
        Map<String, File> postScriptMap;
        if (this.mUpdatableFontDir == null) {
            return Collections.emptyMap();
        }
        synchronized (this.mUpdatableFontDirLock) {
            postScriptMap = this.mUpdatableFontDir.getPostScriptMap();
        }
        return postScriptMap;
    }

    public void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
        if (DumpUtils.checkDumpPermission(this.mContext, TAG, writer)) {
            new FontManagerShellCommand(this).dumpAll(new IndentingPrintWriter(writer, "  "));
        }
    }

    /* JADX DEBUG: Multi-variable search result rejected for r8v0, resolved type: com.android.server.graphics.fonts.FontManagerService */
    /* JADX WARN: Multi-variable type inference failed */
    public void onShellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ShellCallback callback, ResultReceiver result) {
        new FontManagerShellCommand(this).exec(this, in, out, err, args, callback, result);
    }

    public FontConfig getSystemFontConfig() {
        FontConfig systemFontConfig;
        if (this.mUpdatableFontDir == null) {
            return SystemFonts.getSystemPreinstalledFontConfig();
        }
        synchronized (this.mUpdatableFontDirLock) {
            systemFontConfig = this.mUpdatableFontDir.getSystemFontConfig();
        }
        return systemFontConfig;
    }

    private void updateSerializedFontMap() {
        SharedMemory serializedFontMap = serializeFontMap(getSystemFontConfig());
        if (serializedFontMap == null) {
            serializedFontMap = serializeSystemServerFontMap();
        }
        setSerializedFontMap(serializedFontMap);
    }

    /* JADX DEBUG: Another duplicated slice has different insns count: {[INVOKE, INVOKE]}, finally: {[INVOKE, INVOKE, INVOKE, IF, INVOKE, CHECK_CAST, INSTANCE_OF, IF, INVOKE] complete} */
    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [340=4] */
    private static SharedMemory serializeFontMap(FontConfig fontConfig) {
        ArrayMap<String, ByteBuffer> bufferCache = new ArrayMap<>();
        try {
            try {
                Map<String, FontFamily[]> fallback = SystemFonts.buildSystemFallback(fontConfig, bufferCache);
                Map<String, Typeface> typefaceMap = SystemFonts.buildSystemTypefaces(fontConfig, fallback);
                SharedMemory serializeFontMap = Typeface.serializeFontMap(typefaceMap);
                for (ByteBuffer buffer : bufferCache.values()) {
                    if (buffer instanceof DirectByteBuffer) {
                        NioUtils.freeDirectBuffer(buffer);
                    }
                }
                return serializeFontMap;
            } catch (ErrnoException | IOException e) {
                Slog.w(TAG, "Failed to serialize updatable font map. Retrying with system image fonts.", e);
                for (ByteBuffer buffer2 : bufferCache.values()) {
                    if (buffer2 instanceof DirectByteBuffer) {
                        NioUtils.freeDirectBuffer(buffer2);
                    }
                }
                return null;
            }
        } catch (Throwable th) {
            for (ByteBuffer buffer3 : bufferCache.values()) {
                if (buffer3 instanceof DirectByteBuffer) {
                    NioUtils.freeDirectBuffer(buffer3);
                }
            }
            throw th;
        }
    }

    private static SharedMemory serializeSystemServerFontMap() {
        try {
            return Typeface.serializeFontMap(Typeface.getSystemFontMap());
        } catch (ErrnoException | IOException e) {
            Slog.e(TAG, "Failed to serialize SystemServer system font map", e);
            return null;
        }
    }

    private void setSerializedFontMap(SharedMemory serializedFontMap) {
        SharedMemory oldFontMap;
        synchronized (this.mSerializedFontMapLock) {
            oldFontMap = this.mSerializedFontMap;
            this.mSerializedFontMap = serializedFontMap;
        }
        if (oldFontMap != null) {
            oldFontMap.close();
        }
    }
}
