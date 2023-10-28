package com.android.server.clipboard;

import android.content.ClipData;
import android.os.PersistableBundle;
import android.os.SystemProperties;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.system.VmSocketAddress;
import android.util.Slog;
import java.io.EOFException;
import java.io.FileDescriptor;
import java.io.InterruptedIOException;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.function.Consumer;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class EmulatorClipboardMonitor implements Consumer<ClipData> {
    private static final int HOST_PORT = 5000;
    private static final boolean LOG_CLIBOARD_ACCESS = SystemProperties.getBoolean("ro.boot.qemu.log_clipboard_access", false);
    private static final String PIPE_NAME = "pipe:clipboard";
    private static final String TAG = "EmulatorClipboardMonitor";
    private final Thread mHostMonitorThread;
    private FileDescriptor mPipe = null;

    private static byte[] createOpenHandshake() {
        byte[] bits = Arrays.copyOf(PIPE_NAME.getBytes(), PIPE_NAME.length() + 1);
        bits[PIPE_NAME.length()] = 0;
        return bits;
    }

    private synchronized FileDescriptor getPipeFD() {
        return this.mPipe;
    }

    private synchronized void setPipeFD(FileDescriptor fd) {
        this.mPipe = fd;
    }

    private static FileDescriptor openPipeImpl() {
        try {
            FileDescriptor fd = Os.socket(OsConstants.AF_VSOCK, OsConstants.SOCK_STREAM, 0);
            try {
                Os.connect(fd, new VmSocketAddress(5000, OsConstants.VMADDR_CID_HOST));
                byte[] handshake = createOpenHandshake();
                writeFully(fd, handshake, 0, handshake.length);
                return fd;
            } catch (ErrnoException | InterruptedIOException | SocketException e) {
                Os.close(fd);
                return null;
            }
        } catch (ErrnoException e2) {
            return null;
        }
    }

    private static FileDescriptor openPipe() throws InterruptedException {
        FileDescriptor fd = openPipeImpl();
        while (fd == null) {
            Thread.sleep(100L);
            fd = openPipeImpl();
        }
        return fd;
    }

    private static byte[] receiveMessage(FileDescriptor fd) throws ErrnoException, InterruptedIOException, EOFException {
        byte[] lengthBits = new byte[4];
        readFully(fd, lengthBits, 0, lengthBits.length);
        ByteBuffer bb = ByteBuffer.wrap(lengthBits);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        int msgLen = bb.getInt();
        byte[] msg = new byte[msgLen];
        readFully(fd, msg, 0, msg.length);
        return msg;
    }

    private static void sendMessage(FileDescriptor fd, byte[] msg) throws ErrnoException, InterruptedIOException {
        byte[] lengthBits = new byte[4];
        ByteBuffer bb = ByteBuffer.wrap(lengthBits);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        bb.putInt(msg.length);
        writeFully(fd, lengthBits, 0, lengthBits.length);
        writeFully(fd, msg, 0, msg.length);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public EmulatorClipboardMonitor(final Consumer<ClipData> setAndroidClipboard) {
        Thread thread = new Thread(new Runnable() { // from class: com.android.server.clipboard.EmulatorClipboardMonitor$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                EmulatorClipboardMonitor.this.m2667x1c0dcc5b(setAndroidClipboard);
            }
        });
        this.mHostMonitorThread = thread;
        thread.start();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$new$0$com-android-server-clipboard-EmulatorClipboardMonitor  reason: not valid java name */
    public /* synthetic */ void m2667x1c0dcc5b(Consumer setAndroidClipboard) {
        FileDescriptor fd = null;
        while (!Thread.interrupted()) {
            if (fd == null) {
                try {
                    fd = openPipe();
                    setPipeFD(fd);
                } catch (ErrnoException | EOFException | InterruptedIOException | InterruptedException e) {
                    setPipeFD(null);
                    try {
                        Os.close(fd);
                    } catch (ErrnoException e2) {
                    }
                    fd = null;
                }
            }
            byte[] receivedData = receiveMessage(fd);
            String str = new String(receivedData);
            ClipData clip = new ClipData("host clipboard", new String[]{"text/plain"}, new ClipData.Item(str));
            PersistableBundle bundle = new PersistableBundle();
            bundle.putBoolean("com.android.systemui.SUPPRESS_CLIPBOARD_OVERLAY", true);
            clip.getDescription().setExtras(bundle);
            if (LOG_CLIBOARD_ACCESS) {
                Slog.i(TAG, "Setting the guest clipboard to '" + str + "'");
            }
            setAndroidClipboard.accept(clip);
        }
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // java.util.function.Consumer
    public void accept(ClipData clip) {
        FileDescriptor fd = getPipeFD();
        if (fd != null) {
            setHostClipboard(fd, getClipString(clip));
        }
    }

    private String getClipString(ClipData clip) {
        CharSequence text;
        if (clip == null || clip.getItemCount() == 0 || (text = clip.getItemAt(0).getText()) == null) {
            return "";
        }
        return text.toString();
    }

    private static void setHostClipboard(final FileDescriptor fd, final String value) {
        Thread t = new Thread(new Runnable() { // from class: com.android.server.clipboard.EmulatorClipboardMonitor$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                EmulatorClipboardMonitor.lambda$setHostClipboard$1(value, fd);
            }
        });
        t.start();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$setHostClipboard$1(String value, FileDescriptor fd) {
        if (LOG_CLIBOARD_ACCESS) {
            Slog.i(TAG, "Setting the host clipboard to '" + value + "'");
        }
        try {
            sendMessage(fd, value.getBytes());
        } catch (ErrnoException | InterruptedIOException e) {
            Slog.e(TAG, "Failed to set host clipboard " + e.getMessage());
        } catch (IllegalArgumentException e2) {
        }
    }

    private static void readFully(FileDescriptor fd, byte[] buf, int offset, int size) throws ErrnoException, InterruptedIOException, EOFException {
        while (size > 0) {
            int r = Os.read(fd, buf, offset, size);
            if (r > 0) {
                offset += r;
                size -= r;
            } else {
                throw new EOFException();
            }
        }
    }

    private static void writeFully(FileDescriptor fd, byte[] buf, int offset, int size) throws ErrnoException, InterruptedIOException {
        while (size > 0) {
            int r = Os.write(fd, buf, offset, size);
            if (r > 0) {
                offset += r;
                size -= r;
            } else {
                throw new ErrnoException("write", OsConstants.EIO);
            }
        }
    }
}
