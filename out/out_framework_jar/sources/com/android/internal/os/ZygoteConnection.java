package com.android.internal.os;

import android.content.pm.ApplicationInfo;
import android.net.Credentials;
import android.net.LocalSocket;
import android.os.Parcel;
import android.os.Process;
import android.os.Trace;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.system.StructPollfd;
import android.util.Log;
import dalvik.system.VMRuntime;
import dalvik.system.ZygoteHooks;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileDescriptor;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.TimeUnit;
/* loaded from: classes4.dex */
class ZygoteConnection {
    private static final String TAG = "Zygote";
    private final String abiList;
    private boolean isEof;
    private final LocalSocket mSocket;
    private final DataOutputStream mSocketOutStream;
    private final Credentials peer;

    /* JADX INFO: Access modifiers changed from: package-private */
    public ZygoteConnection(LocalSocket socket, String abiList) throws IOException {
        this.mSocket = socket;
        this.abiList = abiList;
        this.mSocketOutStream = new DataOutputStream(socket.getOutputStream());
        socket.setSoTimeout(1000);
        try {
            this.peer = socket.getPeerCredentials();
            this.isEof = false;
        } catch (IOException ex) {
            Log.e(TAG, "Cannot read peer credentials", ex);
            throw ex;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public FileDescriptor getFileDescriptor() {
        return this.mSocket.getFileDescriptor();
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [282=4, 304=11, 120=6] */
    /* JADX DEBUG: Failed to insert an additional move for type inference into block B:123:0x0216 */
    /* JADX DEBUG: Multi-variable search result rejected for r6v4, resolved type: java.io.FileDescriptor */
    /* JADX DEBUG: Multi-variable search result rejected for r7v6, resolved type: java.io.FileDescriptor */
    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: Code restructure failed: missing block: B:100:0x019f, code lost:
        r3 = r0.mAppDataDir;
     */
    /* JADX WARN: Code restructure failed: missing block: B:101:0x01a1, code lost:
        r1 = r0.mIsTopApp;
        r33 = r6;
        r6 = r0.mPkgDataInfoList;
        r34 = r7;
        r7 = r0.mAllowlistedDataInfoList;
        r7 = r0.mBindMountAppDataDirs;
        r7 = r0.mBindMountAppStorageDirs;
        r0 = com.android.internal.os.Zygote.forkAndSpecialize(r11, r12, r13, r14, r8, r0, r15, r5, r11, r29, r10, r2, r3, r1, r6, r7, r7, r7);
     */
    /* JADX WARN: Code restructure failed: missing block: B:103:0x01cf, code lost:
        if (r0 != 0) goto L77;
     */
    /* JADX WARN: Code restructure failed: missing block: B:104:0x01d1, code lost:
        r37.setForkChild();
        r37.closeServerSocket();
        libcore.io.IoUtils.closeQuietly(r34);
     */
    /* JADX WARN: Code restructure failed: missing block: B:105:0x01da, code lost:
        r7 = 0;
     */
    /* JADX WARN: Code restructure failed: missing block: B:108:0x01e1, code lost:
        r0 = handleChildProc(r0, r33, r0.mStartChildZygote);
     */
    /* JADX WARN: Code restructure failed: missing block: B:109:0x01e5, code lost:
        libcore.io.IoUtils.closeQuietly(r33);
        libcore.io.IoUtils.closeQuietly((java.io.FileDescriptor) null);
     */
    /* JADX WARN: Code restructure failed: missing block: B:110:0x01eb, code lost:
        r32.close();
     */
    /* JADX WARN: Code restructure failed: missing block: B:111:0x01ee, code lost:
        return r0;
     */
    /* JADX WARN: Code restructure failed: missing block: B:112:0x01ef, code lost:
        r0 = th;
     */
    /* JADX WARN: Code restructure failed: missing block: B:113:0x01f0, code lost:
        r6 = r33;
     */
    /* JADX WARN: Code restructure failed: missing block: B:114:0x01f5, code lost:
        r0 = th;
     */
    /* JADX WARN: Code restructure failed: missing block: B:115:0x01f6, code lost:
        r6 = r33;
        r7 = r34;
     */
    /* JADX WARN: Code restructure failed: missing block: B:116:0x01fd, code lost:
        r6 = r33;
     */
    /* JADX WARN: Code restructure failed: missing block: B:117:0x0201, code lost:
        libcore.io.IoUtils.closeQuietly((java.io.FileDescriptor) r6);
     */
    /* JADX WARN: Code restructure failed: missing block: B:119:0x0207, code lost:
        handleParentProc(r0, r34);
     */
    /* JADX WARN: Code restructure failed: missing block: B:120:0x020b, code lost:
        libcore.io.IoUtils.closeQuietly((java.io.FileDescriptor) null);
        libcore.io.IoUtils.closeQuietly(r34);
     */
    /* JADX WARN: Code restructure failed: missing block: B:121:0x0211, code lost:
        r32.close();
     */
    /* JADX WARN: Code restructure failed: missing block: B:122:0x0215, code lost:
        return null;
     */
    /* JADX WARN: Code restructure failed: missing block: B:123:0x0216, code lost:
        r0 = th;
     */
    /* JADX WARN: Code restructure failed: missing block: B:125:0x0218, code lost:
        r0 = th;
     */
    /* JADX WARN: Code restructure failed: missing block: B:126:0x0219, code lost:
        r7 = r34;
     */
    /* JADX WARN: Code restructure failed: missing block: B:127:0x021b, code lost:
        libcore.io.IoUtils.closeQuietly(r6);
        libcore.io.IoUtils.closeQuietly(r7);
     */
    /* JADX WARN: Code restructure failed: missing block: B:128:0x0222, code lost:
        throw r0;
     */
    /* JADX WARN: Code restructure failed: missing block: B:129:0x0223, code lost:
        r0 = th;
     */
    /* JADX WARN: Code restructure failed: missing block: B:131:0x0227, code lost:
        r0 = th;
     */
    /* JADX WARN: Code restructure failed: missing block: B:134:0x025b, code lost:
        throw new com.android.internal.os.ZygoteSecurityException("Client may not specify capabilities: permitted=0x" + java.lang.Long.toHexString(r0.mPermittedCapabilities) + ", effective=0x" + java.lang.Long.toHexString(r0.mEffectiveCapabilities));
     */
    /* JADX WARN: Code restructure failed: missing block: B:139:0x0268, code lost:
        r2 = r36;
        r32 = r0;
     */
    /* JADX WARN: Code restructure failed: missing block: B:140:0x026b, code lost:
        r32.close();
     */
    /* JADX WARN: Code restructure failed: missing block: B:141:0x0270, code lost:
        if (r0.mUsapPoolStatusSpecified == false) goto L164;
     */
    /* JADX WARN: Code restructure failed: missing block: B:143:0x027a, code lost:
        return r2.handleUsapPoolStatusChange(r37, r0.mUsapPoolEnabled);
     */
    /* JADX WARN: Code restructure failed: missing block: B:145:0x027f, code lost:
        if (r0.mApiDenylistExemptions == null) goto L168;
     */
    /* JADX WARN: Code restructure failed: missing block: B:147:0x0287, code lost:
        return r2.handleApiDenylistExemptions(r37, r0.mApiDenylistExemptions);
     */
    /* JADX WARN: Code restructure failed: missing block: B:149:0x028a, code lost:
        if (r0.mHiddenApiAccessLogSampleRate != (-1)) goto L174;
     */
    /* JADX WARN: Code restructure failed: missing block: B:151:0x028e, code lost:
        if (r0.mHiddenApiAccessStatslogSampleRate == (-1)) goto L172;
     */
    /* JADX WARN: Code restructure failed: missing block: B:154:0x0298, code lost:
        throw new java.lang.AssertionError("Shouldn't get here");
     */
    /* JADX WARN: Code restructure failed: missing block: B:156:0x02a1, code lost:
        return r2.handleHiddenApiAccessLogSampleRate(r37, r0.mHiddenApiAccessLogSampleRate, r0.mHiddenApiAccessStatslogSampleRate);
     */
    /* JADX WARN: Code restructure failed: missing block: B:99:0x019d, code lost:
        r32 = r0;
     */
    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r6v3, types: [java.lang.String[]] */
    /* JADX WARN: Type inference failed for: r6v5 */
    /* JADX WARN: Type inference failed for: r6v6 */
    /* JADX WARN: Type inference failed for: r6v8, types: [java.io.FileDescriptor] */
    /* JADX WARN: Type inference failed for: r7v5, types: [boolean] */
    /* JADX WARN: Type inference failed for: r7v7 */
    /* JADX WARN: Type inference failed for: r7v8 */
    /* JADX WARN: Type inference failed for: r7v9 */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public Runnable processCommand(ZygoteServer zygoteServer, boolean multipleOK) {
        ZygoteCommandBuffer argBuffer;
        IOException iOException;
        int[] fdsToIgnore;
        ZygoteCommandBuffer argBuffer2 = new ZygoteCommandBuffer(this.mSocket);
        while (true) {
            try {
                try {
                    ZygoteArguments parsedArgs = ZygoteArguments.getInstance(argBuffer2);
                    if (parsedArgs != null) {
                        FileDescriptor childPipeFd = null;
                        FileDescriptor serverPipeFd = null;
                        if (!parsedArgs.mBootCompleted) {
                            if (!parsedArgs.mAbiListQuery) {
                                if (!parsedArgs.mPidQuery) {
                                    if (parsedArgs.mUsapPoolStatusSpecified) {
                                        break;
                                    }
                                    try {
                                        if (parsedArgs.mApiDenylistExemptions != null || parsedArgs.mHiddenApiAccessLogSampleRate != -1) {
                                            break;
                                        } else if (parsedArgs.mHiddenApiAccessStatslogSampleRate != -1) {
                                            ZygoteConnection zygoteConnection = this;
                                            ZygoteCommandBuffer argBuffer3 = argBuffer2;
                                            break;
                                        } else if (parsedArgs.mPreloadDefault) {
                                            handlePreload();
                                            argBuffer2.close();
                                            return null;
                                        } else if (parsedArgs.mPreloadPackage != null) {
                                            handlePreloadPackage(parsedArgs.mPreloadPackage, parsedArgs.mPreloadPackageLibs, parsedArgs.mPreloadPackageLibFileName, parsedArgs.mPreloadPackageCacheKey);
                                            argBuffer2.close();
                                            return null;
                                        } else {
                                            if (canPreloadApp()) {
                                                try {
                                                    if (parsedArgs.mPreloadApp != null) {
                                                        byte[] rawParcelData = Base64.getDecoder().decode(parsedArgs.mPreloadApp);
                                                        Parcel appInfoParcel = Parcel.obtain();
                                                        appInfoParcel.unmarshall(rawParcelData, 0, rawParcelData.length);
                                                        appInfoParcel.setDataPosition(0);
                                                        ApplicationInfo appInfo = ApplicationInfo.CREATOR.createFromParcel(appInfoParcel);
                                                        appInfoParcel.recycle();
                                                        if (appInfo != null) {
                                                            handlePreloadApp(appInfo);
                                                            argBuffer2.close();
                                                            return null;
                                                        }
                                                        throw new IllegalArgumentException("Failed to deserialize --preload-app");
                                                    }
                                                } catch (Throwable th) {
                                                    argBuffer = argBuffer2;
                                                    iOException = th;
                                                    try {
                                                        argBuffer.close();
                                                    } catch (Throwable th2) {
                                                        iOException.addSuppressed(th2);
                                                    }
                                                    throw iOException;
                                                }
                                            }
                                            try {
                                                if (parsedArgs.mPermittedCapabilities != 0 || parsedArgs.mEffectiveCapabilities != 0) {
                                                    break;
                                                }
                                                Zygote.applyUidSecurityPolicy(parsedArgs, this.peer);
                                                Zygote.applyInvokeWithSecurityPolicy(parsedArgs, this.peer);
                                                Zygote.applyDebuggerSystemProperty(parsedArgs);
                                                Zygote.applyInvokeWithSystemProperty(parsedArgs);
                                                int[][] rlimits = parsedArgs.mRLimits != null ? (int[][]) parsedArgs.mRLimits.toArray(Zygote.INT_ARRAY_2D) : null;
                                                if (parsedArgs.mInvokeWith != null) {
                                                    try {
                                                        FileDescriptor[] pipeFds = Os.pipe2(OsConstants.O_CLOEXEC);
                                                        childPipeFd = pipeFds[1];
                                                        serverPipeFd = pipeFds[0];
                                                        Os.fcntlInt(childPipeFd, OsConstants.F_SETFD, 0);
                                                        int[] fdsToIgnore2 = {childPipeFd.getInt$(), serverPipeFd.getInt$()};
                                                        fdsToIgnore = fdsToIgnore2;
                                                    } catch (ErrnoException errnoEx) {
                                                        throw new IllegalStateException("Unable to set up pipe for invoke-with", errnoEx);
                                                    }
                                                } else {
                                                    fdsToIgnore = null;
                                                }
                                                int[] fdsToIgnore3 = {-1, -1};
                                                FileDescriptor fd = this.mSocket.getFileDescriptor();
                                                if (fd != null) {
                                                    fdsToIgnore3[0] = fd.getInt$();
                                                }
                                                FileDescriptor zygoteFd = zygoteServer.getZygoteSocketFileDescriptor();
                                                if (zygoteFd != null) {
                                                    fdsToIgnore3[1] = zygoteFd.getInt$();
                                                }
                                                if (parsedArgs.mInvokeWith != null || parsedArgs.mStartChildZygote || !multipleOK || this.peer.getUid() != 1000) {
                                                    break;
                                                }
                                                ZygoteHooks.preFork();
                                                Runnable result = Zygote.forkSimpleApps(argBuffer2, zygoteServer.getZygoteSocketFileDescriptor(), this.peer.getUid(), Zygote.minChildUid(this.peer), parsedArgs.mNiceName);
                                                if (result != null) {
                                                    zygoteServer.setForkChild();
                                                    Zygote.setAppProcessName(parsedArgs, TAG);
                                                    argBuffer2.close();
                                                    return result;
                                                }
                                                ZygoteHooks.postForkCommon();
                                            } catch (Throwable th3) {
                                                ex = th3;
                                            }
                                        }
                                    } catch (Throwable th4) {
                                        th = th4;
                                        argBuffer = argBuffer2;
                                        iOException = th;
                                        argBuffer.close();
                                        throw iOException;
                                    }
                                } else {
                                    handlePidQuery();
                                    argBuffer2.close();
                                    return null;
                                }
                            } else {
                                handleAbiListQuery();
                                argBuffer2.close();
                                return null;
                            }
                        } else {
                            handleBootCompleted();
                            argBuffer2.close();
                            return null;
                        }
                    } else {
                        this.isEof = true;
                        argBuffer2.close();
                        return null;
                    }
                } catch (Throwable th5) {
                    th = th5;
                    argBuffer = argBuffer2;
                }
            } catch (IOException ex) {
                argBuffer = argBuffer2;
                try {
                    throw new IllegalStateException("IOException on command socket", ex);
                } catch (Throwable th6) {
                    ex = th6;
                    iOException = ex;
                    argBuffer.close();
                    throw iOException;
                }
            }
        }
    }

    private void handleAbiListQuery() {
        try {
            byte[] abiListBytes = this.abiList.getBytes(StandardCharsets.US_ASCII);
            this.mSocketOutStream.writeInt(abiListBytes.length);
            this.mSocketOutStream.write(abiListBytes);
        } catch (IOException ioe) {
            throw new IllegalStateException("Error writing to command socket", ioe);
        }
    }

    private void handlePidQuery() {
        try {
            String pidString = String.valueOf(Process.myPid());
            byte[] pidStringBytes = pidString.getBytes(StandardCharsets.US_ASCII);
            this.mSocketOutStream.writeInt(pidStringBytes.length);
            this.mSocketOutStream.write(pidStringBytes);
        } catch (IOException ioe) {
            throw new IllegalStateException("Error writing to command socket", ioe);
        }
    }

    private void handleBootCompleted() {
        try {
            this.mSocketOutStream.writeInt(0);
            VMRuntime.bootCompleted();
        } catch (IOException ioe) {
            throw new IllegalStateException("Error writing to command socket", ioe);
        }
    }

    private void handlePreload() {
        try {
            if (isPreloadComplete()) {
                this.mSocketOutStream.writeInt(1);
                return;
            }
            preload();
            this.mSocketOutStream.writeInt(0);
        } catch (IOException ioe) {
            throw new IllegalStateException("Error writing to command socket", ioe);
        }
    }

    private Runnable stateChangeWithUsapPoolReset(ZygoteServer zygoteServer, Runnable stateChangeCode) {
        try {
            if (zygoteServer.isUsapPoolEnabled()) {
                Log.i(TAG, "Emptying USAP Pool due to state change.");
                Zygote.emptyUsapPool();
            }
            stateChangeCode.run();
            if (zygoteServer.isUsapPoolEnabled()) {
                Runnable fpResult = zygoteServer.fillUsapPool(new int[]{this.mSocket.getFileDescriptor().getInt$()}, false);
                if (fpResult != null) {
                    zygoteServer.setForkChild();
                    return fpResult;
                }
                Log.i(TAG, "Finished refilling USAP Pool after state change.");
            }
            this.mSocketOutStream.writeInt(0);
            return null;
        } catch (IOException ioe) {
            throw new IllegalStateException("Error writing to command socket", ioe);
        }
    }

    private Runnable handleApiDenylistExemptions(ZygoteServer zygoteServer, final String[] exemptions) {
        return stateChangeWithUsapPoolReset(zygoteServer, new Runnable() { // from class: com.android.internal.os.ZygoteConnection$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                ZygoteInit.setApiDenylistExemptions(exemptions);
            }
        });
    }

    private Runnable handleUsapPoolStatusChange(ZygoteServer zygoteServer, boolean newStatus) {
        try {
            Runnable fpResult = zygoteServer.setUsapPoolStatus(newStatus, this.mSocket);
            if (fpResult == null) {
                this.mSocketOutStream.writeInt(0);
            } else {
                zygoteServer.setForkChild();
            }
            return fpResult;
        } catch (IOException ioe) {
            throw new IllegalStateException("Error writing to command socket", ioe);
        }
    }

    private Runnable handleHiddenApiAccessLogSampleRate(ZygoteServer zygoteServer, final int samplingRate, final int statsdSamplingRate) {
        return stateChangeWithUsapPoolReset(zygoteServer, new Runnable() { // from class: com.android.internal.os.ZygoteConnection$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                ZygoteConnection.lambda$handleHiddenApiAccessLogSampleRate$1(samplingRate, statsdSamplingRate);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$handleHiddenApiAccessLogSampleRate$1(int samplingRate, int statsdSamplingRate) {
        int maxSamplingRate = Math.max(samplingRate, statsdSamplingRate);
        ZygoteInit.setHiddenApiAccessLogSampleRate(maxSamplingRate);
        StatsdHiddenApiUsageLogger.setHiddenApiAccessLogSampleRates(samplingRate, statsdSamplingRate);
        ZygoteInit.setHiddenApiUsageLogger(StatsdHiddenApiUsageLogger.getInstance());
    }

    protected void preload() {
        ZygoteInit.lazyPreload();
    }

    protected boolean isPreloadComplete() {
        return ZygoteInit.isPreloadComplete();
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public DataOutputStream getSocketOutputStream() {
        return this.mSocketOutStream;
    }

    protected void handlePreloadPackage(String packagePath, String libsPath, String libFileName, String cacheKey) {
        throw new RuntimeException("Zygote does not support package preloading");
    }

    protected boolean canPreloadApp() {
        return false;
    }

    protected void handlePreloadApp(ApplicationInfo aInfo) {
        throw new RuntimeException("Zygote does not support app preloading");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void closeSocket() {
        try {
            this.mSocket.close();
        } catch (IOException ex) {
            Log.e(TAG, "Exception while closing command socket in parent", ex);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isClosedByPeer() {
        return this.isEof;
    }

    private Runnable handleChildProc(ZygoteArguments parsedArgs, FileDescriptor pipeFd, boolean isZygote) {
        closeSocket();
        Zygote.setAppProcessName(parsedArgs, TAG);
        Trace.traceEnd(64L);
        if (parsedArgs.mInvokeWith != null) {
            WrapperInit.execApplication(parsedArgs.mInvokeWith, parsedArgs.mNiceName, parsedArgs.mTargetSdkVersion, VMRuntime.getCurrentInstructionSet(), pipeFd, parsedArgs.mRemainingArgs);
            throw new IllegalStateException("WrapperInit.execApplication unexpectedly returned");
        } else if (!isZygote) {
            return ZygoteInit.zygoteInit(parsedArgs.mTargetSdkVersion, parsedArgs.mDisabledCompatChanges, parsedArgs.mRemainingArgs, null);
        } else {
            return ZygoteInit.childZygoteInit(parsedArgs.mRemainingArgs);
        }
    }

    /* JADX WARN: Removed duplicated region for block: B:47:0x00c2  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private void handleParentProc(int pid, FileDescriptor pipeFd) {
        boolean usingWrapper;
        boolean usingWrapper2;
        int innerPid;
        char c;
        boolean z;
        int pid2 = pid;
        if (pid2 > 0) {
            setChildPgid(pid);
        }
        boolean usingWrapper3 = false;
        try {
            if (pipeFd == null || pid2 <= 0) {
                usingWrapper = false;
            } else {
                int innerPid2 = -1;
                int BYTES_REQUIRED = 4;
                try {
                    char c2 = 0;
                    StructPollfd[] fds = {new StructPollfd()};
                    byte[] data = new byte[4];
                    int remainingSleepTime = 30000;
                    int dataIndex = 0;
                    long startTime = System.nanoTime();
                    while (dataIndex < data.length && remainingSleepTime > 0) {
                        fds[c2].fd = pipeFd;
                        fds[c2].events = (short) OsConstants.POLLIN;
                        fds[0].revents = (short) 0;
                        fds[0].userData = null;
                        int res = Os.poll(fds, remainingSleepTime);
                        long endTime = System.nanoTime();
                        usingWrapper = usingWrapper3;
                        innerPid = innerPid2;
                        int BYTES_REQUIRED2 = BYTES_REQUIRED;
                        try {
                            int elapsedTimeMs = (int) TimeUnit.MILLISECONDS.convert(endTime - startTime, TimeUnit.NANOSECONDS);
                            remainingSleepTime = 30000 - elapsedTimeMs;
                            if (res > 0) {
                                c = 0;
                                if ((fds[0].revents & OsConstants.POLLIN) == 0) {
                                    break;
                                }
                                z = true;
                                int readBytes = Os.read(pipeFd, data, dataIndex, 1);
                                if (readBytes < 0) {
                                    throw new RuntimeException("Some error");
                                }
                                dataIndex += readBytes;
                            } else {
                                c = 0;
                                z = true;
                                if (res == 0) {
                                    Log.w(TAG, "Timed out waiting for child.");
                                }
                            }
                            c2 = c;
                            usingWrapper3 = usingWrapper;
                            innerPid2 = innerPid;
                            BYTES_REQUIRED = BYTES_REQUIRED2;
                        } catch (Exception e) {
                            ex = e;
                            innerPid2 = innerPid;
                            Log.w(TAG, "Error reading pid from wrapped process, child may have died", ex);
                            if (innerPid2 > 0) {
                            }
                            usingWrapper2 = usingWrapper;
                            this.mSocketOutStream.writeInt(pid2);
                            this.mSocketOutStream.writeBoolean(usingWrapper2);
                            return;
                        }
                    }
                    usingWrapper = usingWrapper3;
                    innerPid = innerPid2;
                    int BYTES_REQUIRED3 = data.length;
                    if (dataIndex != BYTES_REQUIRED3) {
                        innerPid2 = innerPid;
                    } else {
                        DataInputStream is = new DataInputStream(new ByteArrayInputStream(data));
                        innerPid2 = is.readInt();
                    }
                    if (innerPid2 == -1) {
                        try {
                            Log.w(TAG, "Error reading pid from wrapped process, child may have died");
                        } catch (Exception e2) {
                            ex = e2;
                            Log.w(TAG, "Error reading pid from wrapped process, child may have died", ex);
                            if (innerPid2 > 0) {
                            }
                            usingWrapper2 = usingWrapper;
                            this.mSocketOutStream.writeInt(pid2);
                            this.mSocketOutStream.writeBoolean(usingWrapper2);
                            return;
                        }
                    }
                } catch (Exception e3) {
                    ex = e3;
                    usingWrapper = usingWrapper3;
                }
                if (innerPid2 > 0) {
                    int parentPid = innerPid2;
                    while (parentPid > 0 && parentPid != pid2) {
                        parentPid = Process.getParentPid(parentPid);
                    }
                    if (parentPid > 0) {
                        Log.i(TAG, "Wrapped process has pid " + innerPid2);
                        pid2 = innerPid2;
                        usingWrapper2 = true;
                        this.mSocketOutStream.writeInt(pid2);
                        this.mSocketOutStream.writeBoolean(usingWrapper2);
                        return;
                    }
                    Log.w(TAG, "Wrapped process reported a pid that is not a child of the process that we forked: childPid=" + pid2 + " innerPid=" + innerPid2);
                }
            }
            this.mSocketOutStream.writeInt(pid2);
            this.mSocketOutStream.writeBoolean(usingWrapper2);
            return;
        } catch (IOException ex) {
            throw new IllegalStateException("Error writing to command socket", ex);
        }
        usingWrapper2 = usingWrapper;
    }

    private void setChildPgid(int pid) {
        try {
            Os.setpgid(pid, Os.getpgid(this.peer.getPid()));
        } catch (ErrnoException e) {
            Log.i(TAG, "Zygote: setpgid failed. This is normal if peer is not in our session");
        }
    }
}
