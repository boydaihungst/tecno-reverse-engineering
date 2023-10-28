package com.android.server.backup.restore;

import android.os.ParcelFileDescriptor;
import java.io.InputStream;
import libcore.io.IoUtils;
/* loaded from: classes.dex */
class FullRestoreEngineThread implements Runnable {
    FullRestoreEngine mEngine;
    InputStream mEngineStream;
    private final boolean mMustKillAgent;

    /* JADX INFO: Access modifiers changed from: package-private */
    public FullRestoreEngineThread(FullRestoreEngine engine, ParcelFileDescriptor engineSocket) {
        this.mEngine = engine;
        engine.setRunning(true);
        this.mEngineStream = new ParcelFileDescriptor.AutoCloseInputStream(engineSocket);
        this.mMustKillAgent = false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public FullRestoreEngineThread(FullRestoreEngine engine, InputStream inputStream) {
        this.mEngine = engine;
        engine.setRunning(true);
        this.mEngineStream = inputStream;
        this.mMustKillAgent = true;
    }

    public boolean isRunning() {
        return this.mEngine.isRunning();
    }

    public int waitForResult() {
        return this.mEngine.waitForResult();
    }

    @Override // java.lang.Runnable
    public void run() {
        while (this.mEngine.isRunning()) {
            try {
                FullRestoreEngine fullRestoreEngine = this.mEngine;
                fullRestoreEngine.restoreOneFile(this.mEngineStream, this.mMustKillAgent, fullRestoreEngine.mBuffer, this.mEngine.mOnlyPackage, this.mEngine.mAllowApks, this.mEngine.mEphemeralOpToken, this.mEngine.mMonitor);
            } finally {
                IoUtils.closeQuietly(this.mEngineStream);
            }
        }
    }

    public void handleTimeout() {
        IoUtils.closeQuietly(this.mEngineStream);
        this.mEngine.handleTimeout();
    }
}
