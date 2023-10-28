package com.android.server.am;

import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.os.MessageQueue;
import android.util.Slog;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import libcore.io.IoUtils;
/* loaded from: classes.dex */
public class LmkdConnection {
    private static final int LMKD_REPLY_MAX_SIZE = 222;
    private static final String TAG = "ActivityManager";
    private final ByteBuffer mInputBuf;
    private final DataInputStream mInputData;
    private final LmkdConnectionListener mListener;
    private final MessageQueue mMsgQueue;
    private ByteBuffer mReplyBuf;
    private final Object mReplyBufLock;
    private final Object mLmkdSocketLock = new Object();
    private LocalSocket mLmkdSocket = null;
    private OutputStream mLmkdOutputStream = null;
    private InputStream mLmkdInputStream = null;

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public interface LmkdConnectionListener {
        boolean handleUnsolicitedMessage(DataInputStream dataInputStream, int i);

        boolean isReplyExpected(ByteBuffer byteBuffer, ByteBuffer byteBuffer2, int i);

        boolean onConnect(OutputStream outputStream);

        void onDisconnect();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public LmkdConnection(MessageQueue msgQueue, LmkdConnectionListener listener) {
        ByteBuffer allocate = ByteBuffer.allocate(LMKD_REPLY_MAX_SIZE);
        this.mInputBuf = allocate;
        this.mInputData = new DataInputStream(new ByteArrayInputStream(allocate.array()));
        this.mReplyBufLock = new Object();
        this.mReplyBuf = null;
        this.mMsgQueue = msgQueue;
        this.mListener = listener;
    }

    public boolean connect() {
        synchronized (this.mLmkdSocketLock) {
            if (this.mLmkdSocket != null) {
                return true;
            }
            LocalSocket socket = openSocket();
            if (socket == null) {
                Slog.w(TAG, "Failed to connect to lowmemorykiller, retry later");
                return false;
            }
            try {
                OutputStream ostream = socket.getOutputStream();
                InputStream istream = socket.getInputStream();
                LmkdConnectionListener lmkdConnectionListener = this.mListener;
                if (lmkdConnectionListener != null && !lmkdConnectionListener.onConnect(ostream)) {
                    Slog.w(TAG, "Failed to communicate with lowmemorykiller, retry later");
                    IoUtils.closeQuietly(socket);
                    return false;
                }
                this.mLmkdSocket = socket;
                this.mLmkdOutputStream = ostream;
                this.mLmkdInputStream = istream;
                this.mMsgQueue.addOnFileDescriptorEventListener(socket.getFileDescriptor(), 5, new MessageQueue.OnFileDescriptorEventListener() { // from class: com.android.server.am.LmkdConnection.1
                    @Override // android.os.MessageQueue.OnFileDescriptorEventListener
                    public int onFileDescriptorEvents(FileDescriptor fd, int events) {
                        return LmkdConnection.this.fileDescriptorEventHandler(fd, events);
                    }
                });
                this.mLmkdSocketLock.notifyAll();
                return true;
            } catch (IOException e) {
                IoUtils.closeQuietly(socket);
                return false;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int fileDescriptorEventHandler(FileDescriptor fd, int events) {
        if (this.mListener == null) {
            return 0;
        }
        if ((events & 1) != 0) {
            processIncomingData();
        }
        if ((events & 4) != 0) {
            synchronized (this.mLmkdSocketLock) {
                this.mMsgQueue.removeOnFileDescriptorEventListener(this.mLmkdSocket.getFileDescriptor());
                IoUtils.closeQuietly(this.mLmkdSocket);
                this.mLmkdSocket = null;
            }
            synchronized (this.mReplyBufLock) {
                if (this.mReplyBuf != null) {
                    this.mReplyBuf = null;
                    this.mReplyBufLock.notifyAll();
                }
            }
            this.mListener.onDisconnect();
            return 0;
        }
        return 5;
    }

    private void processIncomingData() {
        int len = read(this.mInputBuf);
        if (len > 0) {
            try {
                this.mInputData.reset();
                synchronized (this.mReplyBufLock) {
                    ByteBuffer byteBuffer = this.mReplyBuf;
                    if (byteBuffer != null) {
                        if (this.mListener.isReplyExpected(byteBuffer, this.mInputBuf, len)) {
                            this.mReplyBuf.put(this.mInputBuf.array(), 0, len);
                            this.mReplyBuf.rewind();
                            this.mReplyBufLock.notifyAll();
                        } else if (!this.mListener.handleUnsolicitedMessage(this.mInputData, len)) {
                            this.mReplyBuf = null;
                            this.mReplyBufLock.notifyAll();
                            Slog.e(TAG, "Received an unexpected packet from lmkd");
                        }
                    } else if (!this.mListener.handleUnsolicitedMessage(this.mInputData, len)) {
                        Slog.w(TAG, "Received an unexpected packet from lmkd");
                    }
                }
            } catch (IOException e) {
                Slog.e(TAG, "Failed to parse lmkd data buffer. Size = " + len);
            }
        }
    }

    public boolean isConnected() {
        boolean z;
        synchronized (this.mLmkdSocketLock) {
            z = this.mLmkdSocket != null;
        }
        return z;
    }

    public boolean waitForConnection(long timeoutMs) {
        synchronized (this.mLmkdSocketLock) {
            if (this.mLmkdSocket != null) {
                return true;
            }
            try {
                this.mLmkdSocketLock.wait(timeoutMs);
                return this.mLmkdSocket != null;
            } catch (InterruptedException e) {
                return false;
            }
        }
    }

    private LocalSocket openSocket() {
        try {
            LocalSocket socket = new LocalSocket(3);
            socket.connect(new LocalSocketAddress("lmkd", LocalSocketAddress.Namespace.RESERVED));
            return socket;
        } catch (IOException ex) {
            Slog.e(TAG, "Connection failed: " + ex.toString());
            return null;
        }
    }

    private boolean write(ByteBuffer buf) {
        synchronized (this.mLmkdSocketLock) {
            try {
                try {
                    this.mLmkdOutputStream.write(buf.array(), 0, buf.position());
                } catch (IOException e) {
                    return false;
                }
            } catch (Throwable th) {
                throw th;
            }
        }
        return true;
    }

    private int read(ByteBuffer buf) {
        int read;
        synchronized (this.mLmkdSocketLock) {
            try {
                try {
                    read = this.mLmkdInputStream.read(buf.array(), 0, buf.array().length);
                } catch (IOException e) {
                    return -1;
                }
            } catch (Throwable th) {
                throw th;
            }
        }
        return read;
    }

    public boolean exchange(ByteBuffer req, ByteBuffer repl) {
        if (repl == null) {
            return write(req);
        }
        boolean result = false;
        synchronized (this.mReplyBufLock) {
            this.mReplyBuf = repl;
            if (write(req)) {
                try {
                    this.mReplyBufLock.wait();
                    result = this.mReplyBuf != null;
                } catch (InterruptedException e) {
                    result = false;
                }
            }
            this.mReplyBuf = null;
        }
        return result;
    }
}
