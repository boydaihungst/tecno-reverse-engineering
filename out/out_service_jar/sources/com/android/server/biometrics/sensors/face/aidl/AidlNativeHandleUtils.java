package com.android.server.biometrics.sensors.face.aidl;

import android.hardware.common.NativeHandle;
import android.os.ParcelFileDescriptor;
import java.io.FileDescriptor;
import java.io.IOException;
/* loaded from: classes.dex */
public final class AidlNativeHandleUtils {
    public static NativeHandle dup(android.os.NativeHandle handle) throws IOException {
        if (handle == null) {
            return null;
        }
        NativeHandle res = new NativeHandle();
        FileDescriptor[] fds = handle.getFileDescriptors();
        res.ints = (int[]) handle.getInts().clone();
        res.fds = new ParcelFileDescriptor[fds.length];
        for (int i = 0; i < fds.length; i++) {
            res.fds[i] = ParcelFileDescriptor.dup(fds[i]);
        }
        return res;
    }

    public static void close(NativeHandle handle) throws IOException {
        ParcelFileDescriptor[] parcelFileDescriptorArr;
        if (handle != null) {
            for (ParcelFileDescriptor fd : handle.fds) {
                if (fd != null) {
                    fd.close();
                }
            }
        }
    }
}
