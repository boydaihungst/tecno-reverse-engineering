package com.transsion.tne;

import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.util.Slog;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.NoSuchElementException;
import vendor.transsion.hardware.tne.tneengine.V1_0.ITne;
/* loaded from: classes2.dex */
public class TNEService {
    private static final String TAG = "TNEService";
    LocalSocket mSocket;
    InputStream mInputStream = null;
    OutputStream mOutputStream = null;
    String mSocketName = "tnesocket";

    public void startTNE(String tag, long type, int pid, String externinfo) {
        if (!SystemProperties.getBoolean("ro.transsion.tne.support", false)) {
            return;
        }
        try {
            ITne tnev = ITne.getService();
            Slog.i(TAG, "tne=" + tnev);
            if (tnev != null) {
                tnev.startTNE(tag, type, pid, externinfo);
            } else if (tnev == null) {
                Slog.e(TAG, "get tnev is failed");
            }
        } catch (RemoteException e) {
            Slog.i(TAG, "err===" + e);
        } catch (NoSuchElementException e2) {
            Slog.i(TAG, "err===" + e2);
        }
    }

    public void startTNE(String tag, long type) {
        startTNE(tag, type, 0, null);
    }

    public void handlerWe(String data) {
        Slog.d(TAG, "handlerWe()");
        try {
            openSocketLocked(data);
        } catch (Exception e) {
            e.printStackTrace();
        }
        closeSocketLocked();
    }

    public void listenToSocket() throws IOException {
        int count;
        Slog.d(TAG, "listenToSocket()");
        try {
            byte[] buffer = new byte[256];
            do {
                count = this.mInputStream.read(buffer);
            } while (count >= 0);
        } finally {
            closeSocketLocked();
        }
    }

    public void openSocketLocked(String date) {
        Slog.d(TAG, "openSocketLocked() data: " + date);
        try {
            LocalSocketAddress address = new LocalSocketAddress(this.mSocketName, LocalSocketAddress.Namespace.ABSTRACT);
            LocalSocket localSocket = new LocalSocket();
            this.mSocket = localSocket;
            localSocket.connect(address);
            OutputStream outputStream = this.mSocket.getOutputStream();
            this.mOutputStream = outputStream;
            outputStream.write(date.getBytes());
            this.mOutputStream.flush();
        } catch (Exception e) {
            closeSocketLocked();
        }
    }

    public void closeSocketLocked() {
        Slog.d(TAG, "closeSocketLocked()");
        try {
            OutputStream outputStream = this.mOutputStream;
            if (outputStream != null) {
                outputStream.close();
                this.mOutputStream = null;
            }
        } catch (IOException e) {
            Slog.e(TAG, "Failed closing output stream: " + e);
        }
        try {
            LocalSocket localSocket = this.mSocket;
            if (localSocket != null) {
                localSocket.close();
                this.mSocket = null;
            }
        } catch (IOException ex) {
            Slog.e(TAG, "Failed closing socket: " + ex);
        }
    }
}
