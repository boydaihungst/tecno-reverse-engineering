package com.android.internal.os;

import com.android.internal.util.ProcFileReader;
import java.io.FileInputStream;
import java.io.IOException;
/* loaded from: classes4.dex */
public class ProcLocksReader {
    private final String mPath;
    private ProcFileReader mReader;

    /* loaded from: classes4.dex */
    public interface ProcLocksReaderCallback {
        void onBlockingFileLock(int i);
    }

    public ProcLocksReader() {
        this.mReader = null;
        this.mPath = "/proc/locks";
    }

    public ProcLocksReader(String path) {
        this.mReader = null;
        this.mPath = path;
    }

    public void handleBlockingFileLocks(ProcLocksReaderCallback callback) throws IOException {
        long last = -1;
        int owner = -1;
        int pid = -1;
        ProcFileReader procFileReader = this.mReader;
        if (procFileReader == null) {
            this.mReader = new ProcFileReader(new FileInputStream(this.mPath));
        } else {
            procFileReader.rewind();
        }
        while (this.mReader.hasMoreData()) {
            long id = this.mReader.nextLong(true);
            if (id == last) {
                this.mReader.finishLine();
                if (pid < 0) {
                    pid = owner;
                    callback.onBlockingFileLock(pid);
                }
            } else {
                pid = -1;
                this.mReader.nextIgnored();
                this.mReader.nextIgnored();
                this.mReader.nextIgnored();
                owner = this.mReader.nextInt();
                this.mReader.finishLine();
                last = id;
            }
        }
    }
}
