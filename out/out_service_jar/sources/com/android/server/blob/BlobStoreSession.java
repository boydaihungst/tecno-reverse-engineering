package com.android.server.blob;

import android.app.blob.BlobHandle;
import android.app.blob.IBlobCommitCallback;
import android.app.blob.IBlobStoreSession;
import android.content.Context;
import android.os.Binder;
import android.os.FileUtils;
import android.os.LimitExceededException;
import android.os.ParcelFileDescriptor;
import android.os.ParcelableException;
import android.os.RemoteException;
import android.os.RevocableFileDescriptor;
import android.os.Trace;
import android.os.storage.StorageManager;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.text.format.Formatter;
import android.util.ExceptionUtils;
import android.util.IndentingPrintWriter;
import android.util.Slog;
import com.android.internal.util.FrameworkStatsLog;
import com.android.internal.util.Preconditions;
import com.android.internal.util.XmlUtils;
import com.android.server.blob.BlobStoreManagerService;
import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import libcore.io.IoUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class BlobStoreSession extends IBlobStoreSession.Stub {
    static final int STATE_ABANDONED = 2;
    static final int STATE_CLOSED = 0;
    static final int STATE_COMMITTED = 3;
    static final int STATE_OPENED = 1;
    static final int STATE_VERIFIED_INVALID = 5;
    static final int STATE_VERIFIED_VALID = 4;
    private final BlobAccessMode mBlobAccessMode;
    private IBlobCommitCallback mBlobCommitCallback;
    private final BlobHandle mBlobHandle;
    private final Context mContext;
    private final long mCreationTimeMs;
    private byte[] mDataDigest;
    private final BlobStoreManagerService.SessionStateChangeListener mListener;
    private final String mOwnerPackageName;
    private final int mOwnerUid;
    private final ArrayList<RevocableFileDescriptor> mRevocableFds;
    private File mSessionFile;
    private final long mSessionId;
    private final Object mSessionLock;
    private int mState;

    private BlobStoreSession(Context context, long sessionId, BlobHandle blobHandle, int ownerUid, String ownerPackageName, long creationTimeMs, BlobStoreManagerService.SessionStateChangeListener listener) {
        this.mSessionLock = new Object();
        this.mRevocableFds = new ArrayList<>();
        this.mState = 0;
        this.mBlobAccessMode = new BlobAccessMode();
        this.mContext = context;
        this.mBlobHandle = blobHandle;
        this.mSessionId = sessionId;
        this.mOwnerUid = ownerUid;
        this.mOwnerPackageName = ownerPackageName;
        this.mCreationTimeMs = creationTimeMs;
        this.mListener = listener;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public BlobStoreSession(Context context, long sessionId, BlobHandle blobHandle, int ownerUid, String ownerPackageName, BlobStoreManagerService.SessionStateChangeListener listener) {
        this(context, sessionId, blobHandle, ownerUid, ownerPackageName, System.currentTimeMillis(), listener);
    }

    public BlobHandle getBlobHandle() {
        return this.mBlobHandle;
    }

    public long getSessionId() {
        return this.mSessionId;
    }

    public int getOwnerUid() {
        return this.mOwnerUid;
    }

    public String getOwnerPackageName() {
        return this.mOwnerPackageName;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasAccess(int callingUid, String callingPackageName) {
        return this.mOwnerUid == callingUid && this.mOwnerPackageName.equals(callingPackageName);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void open() {
        synchronized (this.mSessionLock) {
            if (isFinalized()) {
                throw new IllegalStateException("Not allowed to open session with state: " + stateToString(this.mState));
            }
            this.mState = 1;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getState() {
        int i;
        synchronized (this.mSessionLock) {
            i = this.mState;
        }
        return i;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void sendCommitCallbackResult(int result) {
        synchronized (this.mSessionLock) {
            try {
                this.mBlobCommitCallback.onResult(result);
            } catch (RemoteException e) {
                Slog.d(BlobStoreConfig.TAG, "Error sending the callback result", e);
            }
            this.mBlobCommitCallback = null;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public BlobAccessMode getBlobAccessMode() {
        BlobAccessMode blobAccessMode;
        synchronized (this.mSessionLock) {
            blobAccessMode = this.mBlobAccessMode;
        }
        return blobAccessMode;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isFinalized() {
        boolean z;
        synchronized (this.mSessionLock) {
            int i = this.mState;
            z = i == 3 || i == 2;
        }
        return z;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isExpired() {
        long lastModifiedTimeMs = getSessionFile().lastModified();
        return BlobStoreConfig.hasSessionExpired(lastModifiedTimeMs == 0 ? this.mCreationTimeMs : lastModifiedTimeMs);
    }

    public ParcelFileDescriptor openWrite(long offsetBytes, long lengthBytes) {
        ParcelFileDescriptor revocableFileDescriptor;
        Preconditions.checkArgumentNonnegative(offsetBytes, "offsetBytes must not be negative");
        assertCallerIsOwner();
        synchronized (this.mSessionLock) {
            if (this.mState != 1) {
                throw new IllegalStateException("Not allowed to write in state: " + stateToString(this.mState));
            }
        }
        try {
            FileDescriptor fd = openWriteInternal(offsetBytes, lengthBytes);
            RevocableFileDescriptor revocableFd = new RevocableFileDescriptor(this.mContext, fd);
            synchronized (this.mSessionLock) {
                if (this.mState != 1) {
                    IoUtils.closeQuietly(fd);
                    throw new IllegalStateException("Not allowed to write in state: " + stateToString(this.mState));
                }
                trackRevocableFdLocked(revocableFd);
                revocableFileDescriptor = revocableFd.getRevocableFileDescriptor();
            }
            return revocableFileDescriptor;
        } catch (IOException e) {
            IoUtils.closeQuietly((FileDescriptor) null);
            throw ExceptionUtils.wrap(e);
        }
    }

    private FileDescriptor openWriteInternal(long offsetBytes, long lengthBytes) throws IOException {
        try {
            File sessionFile = getSessionFile();
            if (sessionFile == null) {
                throw new IllegalStateException("Couldn't get the file for this session");
            }
            FileDescriptor fd = Os.open(sessionFile.getPath(), OsConstants.O_CREAT | OsConstants.O_RDWR, FrameworkStatsLog.NON_A11Y_TOOL_SERVICE_WARNING_REPORT);
            if (offsetBytes > 0) {
                long curOffset = Os.lseek(fd, offsetBytes, OsConstants.SEEK_SET);
                if (curOffset != offsetBytes) {
                    throw new IllegalStateException("Failed to seek " + offsetBytes + "; curOffset=" + offsetBytes);
                }
            }
            if (lengthBytes > 0) {
                ((StorageManager) this.mContext.getSystemService(StorageManager.class)).allocateBytes(fd, lengthBytes);
            }
            return fd;
        } catch (ErrnoException e) {
            throw e.rethrowAsIOException();
        }
    }

    public ParcelFileDescriptor openRead() {
        ParcelFileDescriptor revocableFileDescriptor;
        assertCallerIsOwner();
        synchronized (this.mSessionLock) {
            if (this.mState != 1) {
                throw new IllegalStateException("Not allowed to read in state: " + stateToString(this.mState));
            }
            if (!BlobStoreConfig.shouldUseRevocableFdForReads()) {
                try {
                    return new ParcelFileDescriptor(openReadInternal());
                } catch (IOException e) {
                    throw ExceptionUtils.wrap(e);
                }
            }
            try {
                FileDescriptor fd = openReadInternal();
                RevocableFileDescriptor revocableFd = new RevocableFileDescriptor(this.mContext, fd);
                synchronized (this.mSessionLock) {
                    if (this.mState != 1) {
                        IoUtils.closeQuietly(fd);
                        throw new IllegalStateException("Not allowed to read in state: " + stateToString(this.mState));
                    }
                    trackRevocableFdLocked(revocableFd);
                    revocableFileDescriptor = revocableFd.getRevocableFileDescriptor();
                }
                return revocableFileDescriptor;
            } catch (IOException e2) {
                IoUtils.closeQuietly((FileDescriptor) null);
                throw ExceptionUtils.wrap(e2);
            }
        }
    }

    private FileDescriptor openReadInternal() throws IOException {
        try {
            File sessionFile = getSessionFile();
            if (sessionFile == null) {
                throw new IllegalStateException("Couldn't get the file for this session");
            }
            FileDescriptor fd = Os.open(sessionFile.getPath(), OsConstants.O_RDONLY, 0);
            return fd;
        } catch (ErrnoException e) {
            throw e.rethrowAsIOException();
        }
    }

    public long getSize() {
        return getSessionFile().length();
    }

    public void allowPackageAccess(String packageName, byte[] certificate) {
        assertCallerIsOwner();
        Objects.requireNonNull(packageName, "packageName must not be null");
        synchronized (this.mSessionLock) {
            if (this.mState != 1) {
                throw new IllegalStateException("Not allowed to change access type in state: " + stateToString(this.mState));
            }
            if (this.mBlobAccessMode.getAllowedPackagesCount() >= BlobStoreConfig.getMaxPermittedPackages()) {
                throw new ParcelableException(new LimitExceededException("Too many packages permitted to access the blob: " + this.mBlobAccessMode.getAllowedPackagesCount()));
            }
            this.mBlobAccessMode.allowPackageAccess(packageName, certificate);
        }
    }

    public void allowSameSignatureAccess() {
        assertCallerIsOwner();
        synchronized (this.mSessionLock) {
            if (this.mState != 1) {
                throw new IllegalStateException("Not allowed to change access type in state: " + stateToString(this.mState));
            }
            this.mBlobAccessMode.allowSameSignatureAccess();
        }
    }

    public void allowPublicAccess() {
        assertCallerIsOwner();
        synchronized (this.mSessionLock) {
            if (this.mState != 1) {
                throw new IllegalStateException("Not allowed to change access type in state: " + stateToString(this.mState));
            }
            this.mBlobAccessMode.allowPublicAccess();
        }
    }

    public boolean isPackageAccessAllowed(String packageName, byte[] certificate) {
        boolean isPackageAccessAllowed;
        assertCallerIsOwner();
        Objects.requireNonNull(packageName, "packageName must not be null");
        Preconditions.checkByteArrayNotEmpty(certificate, "certificate");
        synchronized (this.mSessionLock) {
            if (this.mState != 1) {
                throw new IllegalStateException("Not allowed to get access type in state: " + stateToString(this.mState));
            }
            isPackageAccessAllowed = this.mBlobAccessMode.isPackageAccessAllowed(packageName, certificate);
        }
        return isPackageAccessAllowed;
    }

    public boolean isSameSignatureAccessAllowed() {
        boolean isSameSignatureAccessAllowed;
        assertCallerIsOwner();
        synchronized (this.mSessionLock) {
            if (this.mState != 1) {
                throw new IllegalStateException("Not allowed to get access type in state: " + stateToString(this.mState));
            }
            isSameSignatureAccessAllowed = this.mBlobAccessMode.isSameSignatureAccessAllowed();
        }
        return isSameSignatureAccessAllowed;
    }

    public boolean isPublicAccessAllowed() {
        boolean isPublicAccessAllowed;
        assertCallerIsOwner();
        synchronized (this.mSessionLock) {
            if (this.mState != 1) {
                throw new IllegalStateException("Not allowed to get access type in state: " + stateToString(this.mState));
            }
            isPublicAccessAllowed = this.mBlobAccessMode.isPublicAccessAllowed();
        }
        return isPublicAccessAllowed;
    }

    public void close() {
        closeSession(0, false);
    }

    public void abandon() {
        closeSession(2, true);
    }

    public void commit(IBlobCommitCallback callback) {
        synchronized (this.mSessionLock) {
            this.mBlobCommitCallback = callback;
            closeSession(3, true);
        }
    }

    private void closeSession(int state, boolean sendCallback) {
        assertCallerIsOwner();
        synchronized (this.mSessionLock) {
            if (this.mState != 1) {
                if (state != 0) {
                    throw new IllegalStateException("Not allowed to delete or abandon a session with state: " + stateToString(this.mState));
                }
                return;
            }
            this.mState = state;
            revokeAllFds();
            if (sendCallback) {
                this.mListener.onStateChanged(this);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void computeDigest() {
        try {
            try {
                Trace.traceBegin(524288L, "computeBlobDigest-i" + this.mSessionId + "-l" + getSessionFile().length());
                this.mDataDigest = FileUtils.digest(getSessionFile(), this.mBlobHandle.algorithm);
            } catch (IOException | NoSuchAlgorithmException e) {
                Slog.e(BlobStoreConfig.TAG, "Error computing the digest", e);
            }
        } finally {
            Trace.traceEnd(524288L);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void verifyBlobData() {
        synchronized (this.mSessionLock) {
            byte[] bArr = this.mDataDigest;
            if (bArr != null && Arrays.equals(bArr, this.mBlobHandle.digest)) {
                this.mState = 4;
            } else {
                StringBuilder append = new StringBuilder().append("Digest of the data (");
                byte[] bArr2 = this.mDataDigest;
                Slog.d(BlobStoreConfig.TAG, append.append(bArr2 == null ? "null" : BlobHandle.safeDigest(bArr2)).append(") didn't match the given BlobHandle.digest (").append(BlobHandle.safeDigest(this.mBlobHandle.digest)).append(")").toString());
                this.mState = 5;
                FrameworkStatsLog.write((int) FrameworkStatsLog.BLOB_COMMITTED, getOwnerUid(), this.mSessionId, getSize(), 3);
                sendCommitCallbackResult(1);
            }
            this.mListener.onStateChanged(this);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void destroy() {
        revokeAllFds();
        getSessionFile().delete();
    }

    private void revokeAllFds() {
        synchronized (this.mRevocableFds) {
            for (int i = this.mRevocableFds.size() - 1; i >= 0; i--) {
                this.mRevocableFds.get(i).revoke();
            }
            this.mRevocableFds.clear();
        }
    }

    private void trackRevocableFdLocked(final RevocableFileDescriptor revocableFd) {
        synchronized (this.mRevocableFds) {
            this.mRevocableFds.add(revocableFd);
        }
        revocableFd.addOnCloseListener(new ParcelFileDescriptor.OnCloseListener() { // from class: com.android.server.blob.BlobStoreSession$$ExternalSyntheticLambda0
            @Override // android.os.ParcelFileDescriptor.OnCloseListener
            public final void onClose(IOException iOException) {
                BlobStoreSession.this.m2594xbe594c0f(revocableFd, iOException);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$trackRevocableFdLocked$0$com-android-server-blob-BlobStoreSession  reason: not valid java name */
    public /* synthetic */ void m2594xbe594c0f(RevocableFileDescriptor revocableFd, IOException e) {
        synchronized (this.mRevocableFds) {
            this.mRevocableFds.remove(revocableFd);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public File getSessionFile() {
        if (this.mSessionFile == null) {
            this.mSessionFile = BlobStoreConfig.prepareBlobFile(this.mSessionId);
        }
        return this.mSessionFile;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static String stateToString(int state) {
        switch (state) {
            case 0:
                return "<closed>";
            case 1:
                return "<opened>";
            case 2:
                return "<abandoned>";
            case 3:
                return "<committed>";
            case 4:
                return "<verified_valid>";
            case 5:
                return "<verified_invalid>";
            default:
                Slog.wtf(BlobStoreConfig.TAG, "Unknown state: " + state);
                return "<unknown>";
        }
    }

    public String toString() {
        return "BlobStoreSession {id:" + this.mSessionId + ",handle:" + this.mBlobHandle + ",uid:" + this.mOwnerUid + ",pkg:" + this.mOwnerPackageName + "}";
    }

    private void assertCallerIsOwner() {
        int callingUid = Binder.getCallingUid();
        if (callingUid != this.mOwnerUid) {
            throw new SecurityException(this.mOwnerUid + " is not the session owner");
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dump(IndentingPrintWriter fout, BlobStoreManagerService.DumpArgs dumpArgs) {
        synchronized (this.mSessionLock) {
            fout.println("state: " + stateToString(this.mState));
            fout.println("ownerUid: " + this.mOwnerUid);
            fout.println("ownerPkg: " + this.mOwnerPackageName);
            fout.println("creation time: " + BlobStoreUtils.formatTime(this.mCreationTimeMs));
            fout.println("size: " + Formatter.formatFileSize(this.mContext, getSize(), 8));
            fout.println("blobHandle:");
            fout.increaseIndent();
            this.mBlobHandle.dump(fout, dumpArgs.shouldDumpFull());
            fout.decreaseIndent();
            fout.println("accessMode:");
            fout.increaseIndent();
            this.mBlobAccessMode.dump(fout);
            fout.decreaseIndent();
            fout.println("Open fds: #" + this.mRevocableFds.size());
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void writeToXml(XmlSerializer out) throws IOException {
        synchronized (this.mSessionLock) {
            XmlUtils.writeLongAttribute(out, "id", this.mSessionId);
            XmlUtils.writeStringAttribute(out, "p", this.mOwnerPackageName);
            XmlUtils.writeIntAttribute(out, "u", this.mOwnerUid);
            XmlUtils.writeLongAttribute(out, "crt", this.mCreationTimeMs);
            out.startTag(null, "bh");
            this.mBlobHandle.writeToXml(out);
            out.endTag(null, "bh");
            out.startTag(null, "am");
            this.mBlobAccessMode.writeToXml(out);
            out.endTag(null, "am");
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static BlobStoreSession createFromXml(XmlPullParser in, int version, Context context, BlobStoreManagerService.SessionStateChangeListener stateChangeListener) throws IOException, XmlPullParserException {
        long creationTimeMs;
        long sessionId = XmlUtils.readLongAttribute(in, "id");
        String ownerPackageName = XmlUtils.readStringAttribute(in, "p");
        int ownerUid = XmlUtils.readIntAttribute(in, "u");
        if (version >= 5) {
            creationTimeMs = XmlUtils.readLongAttribute(in, "crt");
        } else {
            creationTimeMs = System.currentTimeMillis();
        }
        int depth = in.getDepth();
        BlobHandle blobHandle = null;
        BlobAccessMode blobAccessMode = null;
        while (XmlUtils.nextElementWithin(in, depth)) {
            if ("bh".equals(in.getName())) {
                blobHandle = BlobHandle.createFromXml(in);
            } else if ("am".equals(in.getName())) {
                blobAccessMode = BlobAccessMode.createFromXml(in);
            }
        }
        if (blobHandle == null) {
            Slog.wtf(BlobStoreConfig.TAG, "blobHandle should be available");
            return null;
        } else if (blobAccessMode == null) {
            Slog.wtf(BlobStoreConfig.TAG, "blobAccessMode should be available");
            return null;
        } else {
            BlobStoreSession blobStoreSession = new BlobStoreSession(context, sessionId, blobHandle, ownerUid, ownerPackageName, creationTimeMs, stateChangeListener);
            blobStoreSession.mBlobAccessMode.allow(blobAccessMode);
            return blobStoreSession;
        }
    }
}
