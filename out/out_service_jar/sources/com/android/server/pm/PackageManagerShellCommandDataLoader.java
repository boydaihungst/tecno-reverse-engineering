package com.android.server.pm;

import android.content.ComponentName;
import android.content.pm.DataLoaderParams;
import android.content.pm.InstallationFile;
import android.os.ParcelFileDescriptor;
import android.os.ShellCommand;
import android.service.dataloader.DataLoaderService;
import android.util.Slog;
import android.util.SparseArray;
import com.android.server.wm.ActivityTaskManagerService;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicLong;
import libcore.io.IoUtils;
/* loaded from: classes2.dex */
public class PackageManagerShellCommandDataLoader extends DataLoaderService {
    private static final char ARGS_DELIM = '&';
    private static final int INVALID_SHELL_COMMAND_ID = -1;
    private static final String PACKAGE = "android";
    private static final String SHELL_COMMAND_ID_PREFIX = "shellCommandId=";
    private static final String STDIN_PATH = "-";
    public static final String TAG = "PackageManagerShellCommandDataLoader";
    private static final int TOO_MANY_PENDING_SHELL_COMMANDS = 10;
    private static final String CLASS = PackageManagerShellCommandDataLoader.class.getName();
    static final SecureRandom sRandom = new SecureRandom();
    static final SparseArray<WeakReference<ShellCommand>> sShellCommands = new SparseArray<>();

    private static native void nativeInitialize();

    private static String getDataLoaderParamsArgs(ShellCommand shellCommand) {
        SparseArray<WeakReference<ShellCommand>> sparseArray;
        int commandId;
        SparseArray<WeakReference<ShellCommand>> sparseArray2;
        nativeInitialize();
        SparseArray<WeakReference<ShellCommand>> sparseArray3 = sShellCommands;
        synchronized (sparseArray3) {
            for (int i = sparseArray3.size() - 1; i >= 0; i--) {
                SparseArray<WeakReference<ShellCommand>> sparseArray4 = sShellCommands;
                WeakReference<ShellCommand> oldRef = sparseArray4.valueAt(i);
                if (oldRef.get() == null) {
                    sparseArray4.removeAt(i);
                }
            }
            if (sShellCommands.size() > 10) {
                Slog.e(TAG, "Too many pending shell commands: " + sparseArray.size());
            }
            do {
                commandId = sRandom.nextInt(2147483646) + 1;
                sparseArray2 = sShellCommands;
            } while (sparseArray2.contains(commandId));
            sparseArray2.put(commandId, new WeakReference<>(shellCommand));
        }
        return SHELL_COMMAND_ID_PREFIX + commandId;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static DataLoaderParams getStreamingDataLoaderParams(ShellCommand shellCommand) {
        return DataLoaderParams.forStreaming(new ComponentName("android", CLASS), getDataLoaderParamsArgs(shellCommand));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static DataLoaderParams getIncrementalDataLoaderParams(ShellCommand shellCommand) {
        return DataLoaderParams.forIncremental(new ComponentName("android", CLASS), getDataLoaderParamsArgs(shellCommand));
    }

    private static int extractShellCommandId(String args) {
        int sessionIdIdx = args.indexOf(SHELL_COMMAND_ID_PREFIX);
        if (sessionIdIdx < 0) {
            Slog.e(TAG, "Missing shell command id param.");
            return -1;
        }
        int sessionIdIdx2 = sessionIdIdx + SHELL_COMMAND_ID_PREFIX.length();
        int delimIdx = args.indexOf(38, sessionIdIdx2);
        try {
            if (delimIdx < 0) {
                return Integer.parseInt(args.substring(sessionIdIdx2));
            }
            return Integer.parseInt(args.substring(sessionIdIdx2, delimIdx));
        } catch (NumberFormatException e) {
            Slog.e(TAG, "Incorrect shell command id format.", e);
            return -1;
        }
    }

    /* loaded from: classes2.dex */
    public static class Metadata {
        static final byte DATA_ONLY_STREAMING = 2;
        static final byte LOCAL_FILE = 1;
        static final byte STDIN = 0;
        static final byte STREAMING = 3;
        private static AtomicLong sGlobalSalt = new AtomicLong(new SecureRandom().nextLong());
        private final String mData;
        private final byte mMode;
        private final String mSalt;

        private static Long nextGlobalSalt() {
            return Long.valueOf(sGlobalSalt.incrementAndGet());
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public static Metadata forStdIn(String fileId) {
            return new Metadata((byte) 0, fileId);
        }

        public static Metadata forLocalFile(String filePath) {
            return new Metadata((byte) 1, filePath, nextGlobalSalt().toString());
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public static Metadata forDataOnlyStreaming(String fileId) {
            return new Metadata((byte) 2, fileId);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public static Metadata forStreaming(String fileId) {
            return new Metadata((byte) 3, fileId);
        }

        private Metadata(byte mode, String data) {
            this(mode, data, null);
        }

        private Metadata(byte mode, String data, String salt) {
            this.mMode = mode;
            this.mData = data == null ? "" : data;
            this.mSalt = salt;
        }

        static Metadata fromByteArray(byte[] bytes) throws IOException {
            String data;
            String salt;
            if (bytes == null || bytes.length < 5) {
                return null;
            }
            byte mode = bytes[0];
            int offset = 0 + 1;
            switch (mode) {
                case 1:
                    int dataSize = ByteBuffer.wrap(bytes, offset, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
                    int offset2 = offset + 4;
                    data = new String(bytes, offset2, dataSize, StandardCharsets.UTF_8);
                    int offset3 = offset2 + dataSize;
                    salt = new String(bytes, offset3, bytes.length - offset3, StandardCharsets.UTF_8);
                    break;
                default:
                    data = new String(bytes, offset, bytes.length - offset, StandardCharsets.UTF_8);
                    salt = null;
                    break;
            }
            return new Metadata(mode, data, salt);
        }

        public byte[] toByteArray() {
            byte[] dataBytes = this.mData.getBytes(StandardCharsets.UTF_8);
            byte b = this.mMode;
            switch (b) {
                case 1:
                    int dataSize = dataBytes.length;
                    byte[] saltBytes = this.mSalt.getBytes(StandardCharsets.UTF_8);
                    byte[] result = new byte[dataSize + 5 + saltBytes.length];
                    result[0] = this.mMode;
                    int offset = 0 + 1;
                    ByteBuffer.wrap(result, offset, 4).order(ByteOrder.LITTLE_ENDIAN).putInt(dataSize);
                    int offset2 = offset + 4;
                    System.arraycopy(dataBytes, 0, result, offset2, dataSize);
                    System.arraycopy(saltBytes, 0, result, offset2 + dataSize, saltBytes.length);
                    return result;
                default:
                    byte[] result2 = new byte[dataBytes.length + 1];
                    result2[0] = b;
                    System.arraycopy(dataBytes, 0, result2, 1, dataBytes.length);
                    return result2;
            }
        }

        byte getMode() {
            return this.mMode;
        }

        String getData() {
            return this.mData;
        }
    }

    /* loaded from: classes2.dex */
    private static class DataLoader implements DataLoaderService.DataLoader {
        private DataLoaderService.FileSystemConnector mConnector;
        private DataLoaderParams mParams;

        private DataLoader() {
            this.mParams = null;
            this.mConnector = null;
        }

        public boolean onCreate(DataLoaderParams dataLoaderParams, DataLoaderService.FileSystemConnector connector) {
            this.mParams = dataLoaderParams;
            this.mConnector = connector;
            return true;
        }

        public boolean onPrepareImage(Collection<InstallationFile> addedFiles, Collection<String> removedFiles) {
            ShellCommand shellCommand = PackageManagerShellCommandDataLoader.lookupShellCommand(this.mParams.getArguments());
            if (shellCommand == null) {
                Slog.e(PackageManagerShellCommandDataLoader.TAG, "Missing shell command.");
                return false;
            }
            try {
                for (InstallationFile file : addedFiles) {
                    Metadata metadata = Metadata.fromByteArray(file.getMetadata());
                    if (metadata == null) {
                        Slog.e(PackageManagerShellCommandDataLoader.TAG, "Invalid metadata for file: " + file.getName());
                        return false;
                    }
                    switch (metadata.getMode()) {
                        case 0:
                            ParcelFileDescriptor inFd = PackageManagerShellCommandDataLoader.getStdInPFD(shellCommand);
                            this.mConnector.writeData(file.getName(), 0L, file.getLengthBytes(), inFd);
                            break;
                        case 1:
                            ParcelFileDescriptor incomingFd = null;
                            try {
                                ParcelFileDescriptor incomingFd2 = PackageManagerShellCommandDataLoader.getLocalFilePFD(shellCommand, metadata.getData());
                                try {
                                    this.mConnector.writeData(file.getName(), 0L, incomingFd2.getStatSize(), incomingFd2);
                                    IoUtils.closeQuietly(incomingFd2);
                                    break;
                                } catch (Throwable th) {
                                    th = th;
                                    incomingFd = incomingFd2;
                                    IoUtils.closeQuietly(incomingFd);
                                    throw th;
                                }
                            } catch (Throwable th2) {
                                th = th2;
                            }
                        default:
                            Slog.e(PackageManagerShellCommandDataLoader.TAG, "Unsupported metadata mode: " + ((int) metadata.getMode()));
                            return false;
                    }
                }
                return true;
            } catch (IOException e) {
                Slog.e(PackageManagerShellCommandDataLoader.TAG, "Exception while streaming files", e);
                return false;
            }
        }
    }

    static ShellCommand lookupShellCommand(String args) {
        WeakReference<ShellCommand> shellCommandRef;
        int commandId = extractShellCommandId(args);
        if (commandId == -1) {
            return null;
        }
        SparseArray<WeakReference<ShellCommand>> sparseArray = sShellCommands;
        synchronized (sparseArray) {
            shellCommandRef = sparseArray.get(commandId, null);
        }
        if (shellCommandRef == null) {
            return null;
        }
        ShellCommand shellCommand = shellCommandRef.get();
        return shellCommand;
    }

    static ParcelFileDescriptor getStdInPFD(ShellCommand shellCommand) {
        try {
            return ParcelFileDescriptor.dup(shellCommand.getInFileDescriptor());
        } catch (IOException e) {
            Slog.e(TAG, "Exception while obtaining STDIN fd", e);
            return null;
        }
    }

    static ParcelFileDescriptor getLocalFilePFD(ShellCommand shellCommand, String filePath) {
        return shellCommand.openFileForSystem(filePath, ActivityTaskManagerService.DUMP_RECENTS_SHORT_CMD);
    }

    static int getStdIn(ShellCommand shellCommand) {
        ParcelFileDescriptor pfd = getStdInPFD(shellCommand);
        if (pfd == null) {
            return -1;
        }
        return pfd.detachFd();
    }

    static int getLocalFile(ShellCommand shellCommand, String filePath) {
        ParcelFileDescriptor pfd = getLocalFilePFD(shellCommand, filePath);
        if (pfd == null) {
            return -1;
        }
        return pfd.detachFd();
    }

    public DataLoaderService.DataLoader onCreateDataLoader(DataLoaderParams dataLoaderParams) {
        if (dataLoaderParams.getType() == 1) {
            return new DataLoader();
        }
        return null;
    }
}
