package com.android.server.devicepolicy;

import android.app.admin.StartInstallingUpdateCallback;
import android.content.Context;
import android.os.ParcelFileDescriptor;
import android.os.UpdateEngine;
import android.os.UpdateEngineCallback;
import android.util.Log;
import com.android.server.devicepolicy.DevicePolicyManagerService;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntFunction;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class AbUpdateInstaller extends UpdateInstaller {
    private static final int DOWNLOAD_STATE_INITIALIZATION_ERROR = 20;
    private static final int OFFSET_TO_FILE_NAME = 30;
    private static final String PAYLOAD_BIN = "payload.bin";
    private static final String PAYLOAD_PROPERTIES_TXT = "payload_properties.txt";
    public static final String UNKNOWN_ERROR = "Unknown error with error code = ";
    private static final Map<Integer, Integer> errorCodesMap = buildErrorCodesMap();
    private static final Map<Integer, String> errorStringsMap = buildErrorStringsMap();
    private Enumeration<? extends ZipEntry> mEntries;
    private long mOffsetForUpdate;
    private ZipFile mPackedUpdateFile;
    private List<String> mProperties;
    private long mSizeForUpdate;
    private boolean mUpdateInstalled;

    private static Map<Integer, Integer> buildErrorCodesMap() {
        Map<Integer, Integer> map = new HashMap<>();
        map.put(1, 1);
        map.put(20, 2);
        map.put(51, 2);
        map.put(12, 3);
        map.put(11, 3);
        map.put(6, 3);
        map.put(10, 3);
        map.put(26, 3);
        map.put(5, 1);
        map.put(7, 1);
        map.put(9, 1);
        map.put(52, 1);
        return map;
    }

    private static Map<Integer, String> buildErrorStringsMap() {
        Map<Integer, String> map = new HashMap<>();
        map.put(1, UNKNOWN_ERROR);
        map.put(20, "The delta update payload was targeted for another version or the source partitionwas modified after it was installed");
        map.put(5, "Failed to finish the configured postinstall works.");
        map.put(7, "Failed to open one of the partitions it tried to write to or read data from.");
        map.put(6, "Payload mismatch error.");
        map.put(9, "Failed to read the payload data from the given URL.");
        map.put(10, "Payload hash error.");
        map.put(11, "Payload size mismatch error.");
        map.put(12, "Failed to verify the signature of the payload.");
        map.put(52, "The payload has been successfully installed,but the active slot was not flipped.");
        return map;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public AbUpdateInstaller(Context context, ParcelFileDescriptor updateFileDescriptor, StartInstallingUpdateCallback callback, DevicePolicyManagerService.Injector injector, DevicePolicyConstants constants) {
        super(context, updateFileDescriptor, callback, injector, constants);
        this.mUpdateInstalled = false;
    }

    @Override // com.android.server.devicepolicy.UpdateInstaller
    public void installUpdateInThread() {
        if (this.mUpdateInstalled) {
            throw new IllegalStateException("installUpdateInThread can be called only once.");
        }
        try {
            setState();
            applyPayload(Paths.get(this.mCopiedUpdateFile.getAbsolutePath(), new String[0]).toUri().toString());
        } catch (ZipException e) {
            Log.w("UpdateInstaller", e);
            notifyCallbackOnError(3, Log.getStackTraceString(e));
        } catch (IOException e2) {
            Log.w("UpdateInstaller", e2);
            notifyCallbackOnError(1, Log.getStackTraceString(e2));
        }
    }

    private void setState() throws IOException {
        this.mUpdateInstalled = true;
        this.mPackedUpdateFile = new ZipFile(this.mCopiedUpdateFile);
        this.mProperties = new ArrayList();
        this.mSizeForUpdate = -1L;
        this.mOffsetForUpdate = 0L;
        this.mEntries = this.mPackedUpdateFile.entries();
    }

    private UpdateEngine buildBoundUpdateEngine() {
        UpdateEngine updateEngine = new UpdateEngine();
        updateEngine.bind(new DelegatingUpdateEngineCallback(this, updateEngine));
        return updateEngine;
    }

    private void applyPayload(String updatePath) throws IOException {
        if (!updateStateForPayload()) {
            return;
        }
        String[] headerKeyValuePairs = (String[]) this.mProperties.stream().toArray(new IntFunction() { // from class: com.android.server.devicepolicy.AbUpdateInstaller$$ExternalSyntheticLambda0
            @Override // java.util.function.IntFunction
            public final Object apply(int i) {
                return AbUpdateInstaller.lambda$applyPayload$0(i);
            }
        });
        if (this.mSizeForUpdate == -1) {
            Log.w("UpdateInstaller", "Failed to find payload entry in the given package.");
            notifyCallbackOnError(3, "Failed to find payload entry in the given package.");
            return;
        }
        UpdateEngine updateEngine = buildBoundUpdateEngine();
        try {
            updateEngine.applyPayload(updatePath, this.mOffsetForUpdate, this.mSizeForUpdate, headerKeyValuePairs);
        } catch (Exception e) {
            Log.w("UpdateInstaller", "Failed to install update from file.", e);
            notifyCallbackOnError(1, "Failed to install update from file.");
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ String[] lambda$applyPayload$0(int x$0) {
        return new String[x$0];
    }

    private boolean updateStateForPayload() throws IOException {
        long offset = 0;
        while (this.mEntries.hasMoreElements()) {
            ZipEntry entry = this.mEntries.nextElement();
            String name = entry.getName();
            offset += buildOffsetForEntry(entry, name);
            if (entry.isDirectory()) {
                offset -= entry.getCompressedSize();
            } else if (PAYLOAD_BIN.equals(name)) {
                if (entry.getMethod() != 0) {
                    Log.w("UpdateInstaller", "Invalid compression method.");
                    notifyCallbackOnError(3, "Invalid compression method.");
                    return false;
                }
                this.mSizeForUpdate = entry.getCompressedSize();
                this.mOffsetForUpdate = offset - entry.getCompressedSize();
            } else if (PAYLOAD_PROPERTIES_TXT.equals(name)) {
                updatePropertiesForEntry(entry);
            }
        }
        return true;
    }

    private long buildOffsetForEntry(ZipEntry entry, String name) {
        return name.length() + 30 + entry.getCompressedSize() + (entry.getExtra() == null ? 0 : entry.getExtra().length);
    }

    private void updatePropertiesForEntry(ZipEntry entry) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(this.mPackedUpdateFile.getInputStream(entry)));
        while (true) {
            try {
                String line = bufferedReader.readLine();
                if (line != null) {
                    this.mProperties.add(line);
                } else {
                    bufferedReader.close();
                    return;
                }
            } catch (Throwable th) {
                try {
                    bufferedReader.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
                throw th;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class DelegatingUpdateEngineCallback extends UpdateEngineCallback {
        private UpdateEngine mUpdateEngine;
        private UpdateInstaller mUpdateInstaller;

        DelegatingUpdateEngineCallback(UpdateInstaller updateInstaller, UpdateEngine updateEngine) {
            this.mUpdateInstaller = updateInstaller;
            this.mUpdateEngine = updateEngine;
        }

        public void onStatusUpdate(int statusCode, float percentage) {
        }

        public void onPayloadApplicationComplete(int errorCode) {
            this.mUpdateEngine.unbind();
            if (errorCode == 0) {
                this.mUpdateInstaller.notifyCallbackOnSuccess();
            } else {
                this.mUpdateInstaller.notifyCallbackOnError(((Integer) AbUpdateInstaller.errorCodesMap.getOrDefault(Integer.valueOf(errorCode), 1)).intValue(), (String) AbUpdateInstaller.errorStringsMap.getOrDefault(Integer.valueOf(errorCode), AbUpdateInstaller.UNKNOWN_ERROR + errorCode));
            }
        }
    }
}
