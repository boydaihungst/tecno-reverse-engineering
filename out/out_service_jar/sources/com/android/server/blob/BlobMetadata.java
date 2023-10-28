package com.android.server.blob;

import android.app.blob.BlobHandle;
import android.app.blob.LeaseInfo;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.ResourceId;
import android.content.res.Resources;
import android.os.Binder;
import android.os.ParcelFileDescriptor;
import android.os.RevocableFileDescriptor;
import android.os.UserHandle;
import android.permission.PermissionManager;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.text.format.Formatter;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.IndentingPrintWriter;
import android.util.Slog;
import android.util.SparseArray;
import android.util.StatsEvent;
import android.util.proto.ProtoOutputStream;
import com.android.internal.util.FrameworkStatsLog;
import com.android.internal.util.XmlUtils;
import com.android.server.blob.BlobMetadata;
import com.android.server.blob.BlobStoreManagerService;
import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;
import libcore.io.IoUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class BlobMetadata {
    private File mBlobFile;
    private final BlobHandle mBlobHandle;
    private final long mBlobId;
    private final Context mContext;
    private final Object mMetadataLock = new Object();
    private final ArraySet<Committer> mCommitters = new ArraySet<>();
    private final ArraySet<Leasee> mLeasees = new ArraySet<>();
    private final ArrayMap<Accessor, ArraySet<RevocableFileDescriptor>> mRevocableFds = new ArrayMap<>();

    /* JADX INFO: Access modifiers changed from: package-private */
    public BlobMetadata(Context context, long blobId, BlobHandle blobHandle) {
        this.mContext = context;
        this.mBlobId = blobId;
        this.mBlobHandle = blobHandle;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public long getBlobId() {
        return this.mBlobId;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public BlobHandle getBlobHandle() {
        return this.mBlobHandle;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void addOrReplaceCommitter(Committer committer) {
        synchronized (this.mMetadataLock) {
            this.mCommitters.remove(committer);
            this.mCommitters.add(committer);
        }
    }

    void setCommitters(ArraySet<Committer> committers) {
        synchronized (this.mMetadataLock) {
            this.mCommitters.clear();
            this.mCommitters.addAll((ArraySet<? extends Committer>) committers);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void removeCommitter(final String packageName, final int uid) {
        synchronized (this.mMetadataLock) {
            this.mCommitters.removeIf(new Predicate() { // from class: com.android.server.blob.BlobMetadata$$ExternalSyntheticLambda3
                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    return BlobMetadata.lambda$removeCommitter$0(uid, packageName, (BlobMetadata.Committer) obj);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$removeCommitter$0(int uid, String packageName, Committer committer) {
        return committer.uid == uid && committer.packageName.equals(packageName);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void removeCommitter(Committer committer) {
        synchronized (this.mMetadataLock) {
            this.mCommitters.remove(committer);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void removeCommittersFromUnknownPkgs(final SparseArray<SparseArray<String>> knownPackages) {
        synchronized (this.mMetadataLock) {
            this.mCommitters.removeIf(new Predicate() { // from class: com.android.server.blob.BlobMetadata$$ExternalSyntheticLambda5
                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    return BlobMetadata.lambda$removeCommittersFromUnknownPkgs$1(knownPackages, (BlobMetadata.Committer) obj);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$removeCommittersFromUnknownPkgs$1(SparseArray knownPackages, Committer committer) {
        int userId = UserHandle.getUserId(committer.uid);
        SparseArray<String> userPackages = (SparseArray) knownPackages.get(userId);
        if (userPackages == null) {
            return true;
        }
        return true ^ committer.packageName.equals(userPackages.get(committer.uid));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void addCommittersAndLeasees(BlobMetadata blobMetadata) {
        this.mCommitters.addAll((ArraySet<? extends Committer>) blobMetadata.mCommitters);
        this.mLeasees.addAll((ArraySet<? extends Leasee>) blobMetadata.mLeasees);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Committer getExistingCommitter(String packageName, int uid) {
        synchronized (this.mCommitters) {
            int size = this.mCommitters.size();
            for (int i = 0; i < size; i++) {
                Committer committer = this.mCommitters.valueAt(i);
                if (committer.uid == uid && committer.packageName.equals(packageName)) {
                    return committer;
                }
            }
            return null;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void addOrReplaceLeasee(String callingPackage, int callingUid, int descriptionResId, CharSequence description, long leaseExpiryTimeMillis) {
        synchronized (this.mMetadataLock) {
            Leasee leasee = new Leasee(this.mContext, callingPackage, callingUid, descriptionResId, description, leaseExpiryTimeMillis);
            this.mLeasees.remove(leasee);
            this.mLeasees.add(leasee);
        }
    }

    void setLeasees(ArraySet<Leasee> leasees) {
        synchronized (this.mMetadataLock) {
            this.mLeasees.clear();
            this.mLeasees.addAll((ArraySet<? extends Leasee>) leasees);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void removeLeasee(final String packageName, final int uid) {
        synchronized (this.mMetadataLock) {
            this.mLeasees.removeIf(new Predicate() { // from class: com.android.server.blob.BlobMetadata$$ExternalSyntheticLambda8
                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    return BlobMetadata.lambda$removeLeasee$2(uid, packageName, (BlobMetadata.Leasee) obj);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$removeLeasee$2(int uid, String packageName, Leasee leasee) {
        return leasee.uid == uid && leasee.packageName.equals(packageName);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void removeLeaseesFromUnknownPkgs(final SparseArray<SparseArray<String>> knownPackages) {
        synchronized (this.mMetadataLock) {
            this.mLeasees.removeIf(new Predicate() { // from class: com.android.server.blob.BlobMetadata$$ExternalSyntheticLambda7
                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    return BlobMetadata.lambda$removeLeaseesFromUnknownPkgs$3(knownPackages, (BlobMetadata.Leasee) obj);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$removeLeaseesFromUnknownPkgs$3(SparseArray knownPackages, Leasee leasee) {
        int userId = UserHandle.getUserId(leasee.uid);
        SparseArray<String> userPackages = (SparseArray) knownPackages.get(userId);
        if (userPackages == null) {
            return true;
        }
        return true ^ leasee.packageName.equals(userPackages.get(leasee.uid));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void removeExpiredLeases() {
        synchronized (this.mMetadataLock) {
            this.mLeasees.removeIf(new Predicate() { // from class: com.android.server.blob.BlobMetadata$$ExternalSyntheticLambda6
                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    return BlobMetadata.lambda$removeExpiredLeases$4((BlobMetadata.Leasee) obj);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$removeExpiredLeases$4(Leasee leasee) {
        return !leasee.isStillValid();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void removeDataForUser(final int userId) {
        synchronized (this.mMetadataLock) {
            this.mCommitters.removeIf(new Predicate() { // from class: com.android.server.blob.BlobMetadata$$ExternalSyntheticLambda0
                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    return BlobMetadata.lambda$removeDataForUser$5(userId, (BlobMetadata.Committer) obj);
                }
            });
            this.mLeasees.removeIf(new Predicate() { // from class: com.android.server.blob.BlobMetadata$$ExternalSyntheticLambda1
                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    return BlobMetadata.lambda$removeDataForUser$6(userId, (BlobMetadata.Leasee) obj);
                }
            });
            this.mRevocableFds.entrySet().removeIf(new Predicate() { // from class: com.android.server.blob.BlobMetadata$$ExternalSyntheticLambda2
                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    return BlobMetadata.lambda$removeDataForUser$7(userId, (Map.Entry) obj);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$removeDataForUser$5(int userId, Committer committer) {
        return userId == UserHandle.getUserId(committer.uid);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$removeDataForUser$6(int userId, Leasee leasee) {
        return userId == UserHandle.getUserId(leasee.uid);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$removeDataForUser$7(int userId, Map.Entry entry) {
        Accessor accessor = (Accessor) entry.getKey();
        ArraySet<RevocableFileDescriptor> rFds = (ArraySet) entry.getValue();
        if (userId != UserHandle.getUserId(accessor.uid)) {
            return false;
        }
        int fdCount = rFds.size();
        for (int i = 0; i < fdCount; i++) {
            rFds.valueAt(i).revoke();
        }
        rFds.clear();
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasValidLeases() {
        synchronized (this.mMetadataLock) {
            int size = this.mLeasees.size();
            for (int i = 0; i < size; i++) {
                if (this.mLeasees.valueAt(i).isStillValid()) {
                    return true;
                }
            }
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public long getSize() {
        return getBlobFile().length();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isAccessAllowedForCaller(String callingPackage, int callingUid) {
        if (getBlobHandle().isExpired()) {
            return false;
        }
        synchronized (this.mMetadataLock) {
            int size = this.mLeasees.size();
            for (int i = 0; i < size; i++) {
                Leasee leasee = this.mLeasees.valueAt(i);
                if (leasee.isStillValid() && leasee.equals(callingPackage, callingUid)) {
                    return true;
                }
            }
            int callingUserId = UserHandle.getUserId(callingUid);
            int size2 = this.mCommitters.size();
            for (int i2 = 0; i2 < size2; i2++) {
                Committer committer = this.mCommitters.valueAt(i2);
                if (callingUserId == UserHandle.getUserId(committer.uid)) {
                    if (committer.equals(callingPackage, callingUid)) {
                        return true;
                    }
                    if (committer.blobAccessMode.isAccessAllowedForCaller(this.mContext, callingPackage, committer.packageName)) {
                        return true;
                    }
                }
            }
            boolean canCallerAccessBlobsAcrossUsers = checkCallerCanAccessBlobsAcrossUsers(callingPackage, callingUserId);
            if (canCallerAccessBlobsAcrossUsers) {
                int size3 = this.mCommitters.size();
                for (int i3 = 0; i3 < size3; i3++) {
                    Committer committer2 = this.mCommitters.valueAt(i3);
                    int committerUserId = UserHandle.getUserId(committer2.uid);
                    if (callingUserId != committerUserId && isPackageInstalledOnUser(callingPackage, committerUserId) && committer2.blobAccessMode.isAccessAllowedForCaller(this.mContext, callingPackage, committer2.packageName)) {
                        return true;
                    }
                }
                return false;
            }
            return false;
        }
    }

    private static boolean checkCallerCanAccessBlobsAcrossUsers(String callingPackage, int callingUserId) {
        long token = Binder.clearCallingIdentity();
        try {
            return PermissionManager.checkPackageNamePermission("android.permission.ACCESS_BLOBS_ACROSS_USERS", callingPackage, callingUserId) == 0;
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    private boolean isPackageInstalledOnUser(String packageName, int userId) {
        long token = Binder.clearCallingIdentity();
        try {
            this.mContext.getPackageManager().getPackageInfoAsUser(packageName, 0, userId);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasACommitterOrLeaseeInUser(int userId) {
        return hasACommitterInUser(userId) || hasALeaseeInUser(userId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasACommitterInUser(int userId) {
        synchronized (this.mMetadataLock) {
            int size = this.mCommitters.size();
            for (int i = 0; i < size; i++) {
                Committer committer = this.mCommitters.valueAt(i);
                if (userId == UserHandle.getUserId(committer.uid)) {
                    return true;
                }
            }
            return false;
        }
    }

    private boolean hasALeaseeInUser(int userId) {
        synchronized (this.mMetadataLock) {
            int size = this.mLeasees.size();
            for (int i = 0; i < size; i++) {
                Leasee leasee = this.mLeasees.valueAt(i);
                if (userId == UserHandle.getUserId(leasee.uid)) {
                    return true;
                }
            }
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isACommitter(String packageName, int uid) {
        boolean isAnAccessor;
        synchronized (this.mMetadataLock) {
            isAnAccessor = isAnAccessor(this.mCommitters, packageName, uid, UserHandle.getUserId(uid));
        }
        return isAnAccessor;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isALeasee(String packageName, int uid) {
        boolean z;
        synchronized (this.mMetadataLock) {
            Leasee leasee = (Leasee) getAccessor(this.mLeasees, packageName, uid, UserHandle.getUserId(uid));
            z = leasee != null && leasee.isStillValid();
        }
        return z;
    }

    private boolean isALeaseeInUser(String packageName, int uid, int userId) {
        boolean z;
        synchronized (this.mMetadataLock) {
            Leasee leasee = (Leasee) getAccessor(this.mLeasees, packageName, uid, userId);
            z = leasee != null && leasee.isStillValid();
        }
        return z;
    }

    private static <T extends Accessor> boolean isAnAccessor(ArraySet<T> accessors, String packageName, int uid, int userId) {
        return getAccessor(accessors, packageName, uid, userId) != null;
    }

    private static <T extends Accessor> T getAccessor(ArraySet<T> accessors, String packageName, int uid, int userId) {
        int size = accessors.size();
        for (int i = 0; i < size; i++) {
            T valueAt = accessors.valueAt(i);
            if (packageName != null && uid != -1 && valueAt.equals(packageName, uid)) {
                return valueAt;
            }
            if (packageName != null && valueAt.packageName.equals(packageName) && userId == UserHandle.getUserId(valueAt.uid)) {
                return valueAt;
            }
            if (uid != -1 && valueAt.uid == uid) {
                return valueAt;
            }
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean shouldAttributeToUser(int userId) {
        synchronized (this.mMetadataLock) {
            int size = this.mLeasees.size();
            for (int i = 0; i < size; i++) {
                Leasee leasee = this.mLeasees.valueAt(i);
                if (userId != UserHandle.getUserId(leasee.uid)) {
                    return false;
                }
            }
            return true;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean shouldAttributeToLeasee(String packageName, int userId, boolean callerHasStatsPermission) {
        if (isALeaseeInUser(packageName, -1, userId)) {
            return (callerHasStatsPermission && hasOtherLeasees(packageName, -1, userId)) ? false : true;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean shouldAttributeToLeasee(int uid, boolean callerHasStatsPermission) {
        int userId = UserHandle.getUserId(uid);
        if (isALeaseeInUser(null, uid, userId)) {
            return (callerHasStatsPermission && hasOtherLeasees(null, uid, userId)) ? false : true;
        }
        return false;
    }

    private boolean hasOtherLeasees(String packageName, int uid, int userId) {
        synchronized (this.mMetadataLock) {
            int size = this.mLeasees.size();
            for (int i = 0; i < size; i++) {
                Leasee leasee = this.mLeasees.valueAt(i);
                if (leasee.isStillValid()) {
                    if (packageName != null && uid != -1 && !leasee.equals(packageName, uid)) {
                        return true;
                    }
                    if (packageName != null && (!leasee.packageName.equals(packageName) || userId != UserHandle.getUserId(leasee.uid))) {
                        return true;
                    }
                    if (uid != -1 && leasee.uid != uid) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public LeaseInfo getLeaseInfo(String packageName, int uid) {
        int descriptionResId;
        synchronized (this.mMetadataLock) {
            int size = this.mLeasees.size();
            for (int i = 0; i < size; i++) {
                Leasee leasee = this.mLeasees.valueAt(i);
                if (leasee.isStillValid() && leasee.uid == uid && leasee.packageName.equals(packageName)) {
                    if (leasee.descriptionResEntryName == null) {
                        descriptionResId = 0;
                    } else {
                        descriptionResId = BlobStoreUtils.getDescriptionResourceId(this.mContext, leasee.descriptionResEntryName, leasee.packageName, UserHandle.getUserId(leasee.uid));
                    }
                    return new LeaseInfo(packageName, leasee.expiryTimeMillis, descriptionResId, leasee.description);
                }
            }
            return null;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void forEachLeasee(Consumer<Leasee> consumer) {
        synchronized (this.mMetadataLock) {
            this.mLeasees.forEach(consumer);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public File getBlobFile() {
        if (this.mBlobFile == null) {
            this.mBlobFile = BlobStoreConfig.getBlobFile(this.mBlobId);
        }
        return this.mBlobFile;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ParcelFileDescriptor openForRead(String callingPackage, int callingUid) throws IOException {
        try {
            FileDescriptor fd = Os.open(getBlobFile().getPath(), OsConstants.O_RDONLY, 0);
            try {
                if (BlobStoreConfig.shouldUseRevocableFdForReads()) {
                    return createRevocableFd(fd, callingPackage, callingUid);
                }
                return new ParcelFileDescriptor(fd);
            } catch (IOException e) {
                IoUtils.closeQuietly(fd);
                throw e;
            }
        } catch (ErrnoException e2) {
            throw e2.rethrowAsIOException();
        }
    }

    private ParcelFileDescriptor createRevocableFd(FileDescriptor fd, String callingPackage, int callingUid) throws IOException {
        final Accessor accessor;
        final RevocableFileDescriptor revocableFd = new RevocableFileDescriptor(this.mContext, fd);
        synchronized (this.mRevocableFds) {
            accessor = new Accessor(callingPackage, callingUid);
            ArraySet<RevocableFileDescriptor> revocableFdsForAccessor = this.mRevocableFds.get(accessor);
            if (revocableFdsForAccessor == null) {
                revocableFdsForAccessor = new ArraySet<>();
                this.mRevocableFds.put(accessor, revocableFdsForAccessor);
            }
            revocableFdsForAccessor.add(revocableFd);
        }
        revocableFd.addOnCloseListener(new ParcelFileDescriptor.OnCloseListener() { // from class: com.android.server.blob.BlobMetadata$$ExternalSyntheticLambda4
            @Override // android.os.ParcelFileDescriptor.OnCloseListener
            public final void onClose(IOException iOException) {
                BlobMetadata.this.m2551lambda$createRevocableFd$8$comandroidserverblobBlobMetadata(accessor, revocableFd, iOException);
            }
        });
        return revocableFd.getRevocableFileDescriptor();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$createRevocableFd$8$com-android-server-blob-BlobMetadata  reason: not valid java name */
    public /* synthetic */ void m2551lambda$createRevocableFd$8$comandroidserverblobBlobMetadata(Accessor accessor, RevocableFileDescriptor revocableFd, IOException e) {
        synchronized (this.mRevocableFds) {
            ArraySet<RevocableFileDescriptor> revocableFdsForAccessor = this.mRevocableFds.get(accessor);
            if (revocableFdsForAccessor != null) {
                revocableFdsForAccessor.remove(revocableFd);
                if (revocableFdsForAccessor.isEmpty()) {
                    this.mRevocableFds.remove(accessor);
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void destroy() {
        revokeAndClearAllFds();
        getBlobFile().delete();
    }

    private void revokeAndClearAllFds() {
        synchronized (this.mRevocableFds) {
            int accessorCount = this.mRevocableFds.size();
            for (int i = 0; i < accessorCount; i++) {
                ArraySet<RevocableFileDescriptor> rFds = this.mRevocableFds.valueAt(i);
                if (rFds != null) {
                    int fdCount = rFds.size();
                    for (int j = 0; j < fdCount; j++) {
                        rFds.valueAt(j).revoke();
                    }
                }
            }
            this.mRevocableFds.clear();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean shouldBeDeleted(boolean respectLeaseWaitTime) {
        if (getBlobHandle().isExpired()) {
            return true;
        }
        return (!respectLeaseWaitTime || hasLeaseWaitTimeElapsedForAll()) && !hasValidLeases();
    }

    boolean hasLeaseWaitTimeElapsedForAll() {
        int size = this.mCommitters.size();
        for (int i = 0; i < size; i++) {
            Committer committer = this.mCommitters.valueAt(i);
            if (!BlobStoreConfig.hasLeaseWaitTimeElapsed(committer.getCommitTimeMs())) {
                return false;
            }
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public StatsEvent dumpAsStatsEvent(int atomTag) {
        StatsEvent buildStatsEvent;
        synchronized (this.mMetadataLock) {
            ProtoOutputStream proto = new ProtoOutputStream();
            int size = this.mCommitters.size();
            for (int i = 0; i < size; i++) {
                Committer committer = this.mCommitters.valueAt(i);
                long token = proto.start(CompanionAppsPermissions.APP_PERMISSIONS);
                proto.write(CompanionMessage.MESSAGE_ID, committer.uid);
                proto.write(1112396529666L, committer.commitTimeMs);
                proto.write(1120986464259L, committer.blobAccessMode.getAccessType());
                proto.write(1120986464260L, committer.blobAccessMode.getAllowedPackagesCount());
                proto.end(token);
            }
            byte[] committersBytes = proto.getBytes();
            ProtoOutputStream proto2 = new ProtoOutputStream();
            int size2 = this.mLeasees.size();
            for (int i2 = 0; i2 < size2; i2++) {
                Leasee leasee = this.mLeasees.valueAt(i2);
                long token2 = proto2.start(CompanionAppsPermissions.APP_PERMISSIONS);
                proto2.write(CompanionMessage.MESSAGE_ID, leasee.uid);
                proto2.write(1112396529666L, leasee.expiryTimeMillis);
                proto2.end(token2);
            }
            byte[] leaseesBytes = proto2.getBytes();
            buildStatsEvent = FrameworkStatsLog.buildStatsEvent(atomTag, this.mBlobId, getSize(), this.mBlobHandle.getExpiryTimeMillis(), committersBytes, leaseesBytes);
        }
        return buildStatsEvent;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dump(IndentingPrintWriter fout, BlobStoreManagerService.DumpArgs dumpArgs) {
        synchronized (this.mMetadataLock) {
            fout.println("blobHandle:");
            fout.increaseIndent();
            this.mBlobHandle.dump(fout, dumpArgs.shouldDumpFull());
            fout.decreaseIndent();
            fout.println("size: " + Formatter.formatFileSize(this.mContext, getSize(), 8));
            fout.println("Committers:");
            fout.increaseIndent();
            if (this.mCommitters.isEmpty()) {
                fout.println("<empty>");
            } else {
                int count = this.mCommitters.size();
                for (int i = 0; i < count; i++) {
                    Committer committer = this.mCommitters.valueAt(i);
                    fout.println("committer " + committer.toString());
                    fout.increaseIndent();
                    committer.dump(fout);
                    fout.decreaseIndent();
                }
            }
            fout.decreaseIndent();
            fout.println("Leasees:");
            fout.increaseIndent();
            if (this.mLeasees.isEmpty()) {
                fout.println("<empty>");
            } else {
                int count2 = this.mLeasees.size();
                for (int i2 = 0; i2 < count2; i2++) {
                    Leasee leasee = this.mLeasees.valueAt(i2);
                    fout.println("leasee " + leasee.toString());
                    fout.increaseIndent();
                    leasee.dump(this.mContext, fout);
                    fout.decreaseIndent();
                }
            }
            fout.decreaseIndent();
            fout.println("Open fds:");
            fout.increaseIndent();
            if (this.mRevocableFds.isEmpty()) {
                fout.println("<empty>");
            } else {
                int count3 = this.mRevocableFds.size();
                for (int i3 = 0; i3 < count3; i3++) {
                    Accessor accessor = this.mRevocableFds.keyAt(i3);
                    ArraySet<RevocableFileDescriptor> rFds = this.mRevocableFds.valueAt(i3);
                    fout.println(accessor + ": #" + rFds.size());
                }
            }
            fout.decreaseIndent();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void writeToXml(XmlSerializer out) throws IOException {
        synchronized (this.mMetadataLock) {
            XmlUtils.writeLongAttribute(out, "id", this.mBlobId);
            out.startTag(null, "bh");
            this.mBlobHandle.writeToXml(out);
            out.endTag(null, "bh");
            int count = this.mCommitters.size();
            for (int i = 0; i < count; i++) {
                out.startTag(null, "c");
                this.mCommitters.valueAt(i).writeToXml(out);
                out.endTag(null, "c");
            }
            int count2 = this.mLeasees.size();
            for (int i2 = 0; i2 < count2; i2++) {
                out.startTag(null, "l");
                this.mLeasees.valueAt(i2).writeToXml(out);
                out.endTag(null, "l");
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static BlobMetadata createFromXml(XmlPullParser in, int version, Context context) throws XmlPullParserException, IOException {
        long blobId = XmlUtils.readLongAttribute(in, "id");
        if (version < 6) {
            XmlUtils.readIntAttribute(in, "us");
        }
        BlobHandle blobHandle = null;
        ArraySet<Committer> committers = new ArraySet<>();
        ArraySet<Leasee> leasees = new ArraySet<>();
        int depth = in.getDepth();
        while (XmlUtils.nextElementWithin(in, depth)) {
            if ("bh".equals(in.getName())) {
                blobHandle = BlobHandle.createFromXml(in);
            } else if ("c".equals(in.getName())) {
                Committer committer = Committer.createFromXml(in, version);
                if (committer != null) {
                    committers.add(committer);
                }
            } else if ("l".equals(in.getName())) {
                leasees.add(Leasee.createFromXml(in, version));
            }
        }
        if (blobHandle == null) {
            Slog.wtf(BlobStoreConfig.TAG, "blobHandle should be available");
            return null;
        }
        BlobMetadata blobMetadata = new BlobMetadata(context, blobId, blobHandle);
        blobMetadata.setCommitters(committers);
        blobMetadata.setLeasees(leasees);
        return blobMetadata;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static final class Committer extends Accessor {
        public final BlobAccessMode blobAccessMode;
        public final long commitTimeMs;

        /* JADX INFO: Access modifiers changed from: package-private */
        public Committer(String packageName, int uid, BlobAccessMode blobAccessMode, long commitTimeMs) {
            super(packageName, uid);
            this.blobAccessMode = blobAccessMode;
            this.commitTimeMs = commitTimeMs;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public long getCommitTimeMs() {
            return this.commitTimeMs;
        }

        void dump(IndentingPrintWriter fout) {
            StringBuilder append = new StringBuilder().append("commit time: ");
            long j = this.commitTimeMs;
            fout.println(append.append(j == 0 ? "<null>" : BlobStoreUtils.formatTime(j)).toString());
            fout.println("accessMode:");
            fout.increaseIndent();
            this.blobAccessMode.dump(fout);
            fout.decreaseIndent();
        }

        void writeToXml(XmlSerializer out) throws IOException {
            XmlUtils.writeStringAttribute(out, "p", this.packageName);
            XmlUtils.writeIntAttribute(out, "u", this.uid);
            XmlUtils.writeLongAttribute(out, "cmt", this.commitTimeMs);
            out.startTag(null, "am");
            this.blobAccessMode.writeToXml(out);
            out.endTag(null, "am");
        }

        static Committer createFromXml(XmlPullParser in, int version) throws XmlPullParserException, IOException {
            long commitTimeMs;
            String packageName = XmlUtils.readStringAttribute(in, "p");
            int uid = XmlUtils.readIntAttribute(in, "u");
            if (version >= 4) {
                commitTimeMs = XmlUtils.readLongAttribute(in, "cmt");
            } else {
                commitTimeMs = 0;
            }
            int depth = in.getDepth();
            BlobAccessMode blobAccessMode = null;
            while (XmlUtils.nextElementWithin(in, depth)) {
                if ("am".equals(in.getName())) {
                    blobAccessMode = BlobAccessMode.createFromXml(in);
                }
            }
            if (blobAccessMode == null) {
                Slog.wtf(BlobStoreConfig.TAG, "blobAccessMode should be available");
                return null;
            }
            return new Committer(packageName, uid, blobAccessMode, commitTimeMs);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static final class Leasee extends Accessor {
        public final CharSequence description;
        public final String descriptionResEntryName;
        public final long expiryTimeMillis;

        Leasee(Context context, String packageName, int uid, int descriptionResId, CharSequence description, long expiryTimeMillis) {
            super(packageName, uid);
            CharSequence charSequence;
            Resources packageResources = BlobStoreUtils.getPackageResources(context, packageName, UserHandle.getUserId(uid));
            this.descriptionResEntryName = getResourceEntryName(packageResources, descriptionResId);
            this.expiryTimeMillis = expiryTimeMillis;
            if (description == null) {
                charSequence = getDescription(packageResources, descriptionResId);
            } else {
                charSequence = description;
            }
            this.description = charSequence;
        }

        Leasee(String packageName, int uid, String descriptionResEntryName, CharSequence description, long expiryTimeMillis) {
            super(packageName, uid);
            this.descriptionResEntryName = descriptionResEntryName;
            this.expiryTimeMillis = expiryTimeMillis;
            this.description = description;
        }

        private static String getResourceEntryName(Resources packageResources, int resId) {
            if (!ResourceId.isValid(resId) || packageResources == null) {
                return null;
            }
            return packageResources.getResourceEntryName(resId);
        }

        private static String getDescription(Context context, String descriptionResEntryName, String packageName, int userId) {
            Resources resources;
            int resId;
            if (descriptionResEntryName == null || descriptionResEntryName.isEmpty() || (resources = BlobStoreUtils.getPackageResources(context, packageName, userId)) == null || (resId = BlobStoreUtils.getDescriptionResourceId(resources, descriptionResEntryName, packageName)) == 0) {
                return null;
            }
            return resources.getString(resId);
        }

        private static String getDescription(Resources packageResources, int descriptionResId) {
            if (!ResourceId.isValid(descriptionResId) || packageResources == null) {
                return null;
            }
            return packageResources.getString(descriptionResId);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public boolean isStillValid() {
            long j = this.expiryTimeMillis;
            return j == 0 || j >= System.currentTimeMillis();
        }

        void dump(Context context, IndentingPrintWriter fout) {
            fout.println("desc: " + getDescriptionToDump(context));
            fout.println("expiryMs: " + this.expiryTimeMillis);
        }

        private String getDescriptionToDump(Context context) {
            String desc = getDescription(context, this.descriptionResEntryName, this.packageName, UserHandle.getUserId(this.uid));
            if (desc == null) {
                desc = this.description.toString();
            }
            return desc == null ? "<none>" : desc;
        }

        void writeToXml(XmlSerializer out) throws IOException {
            XmlUtils.writeStringAttribute(out, "p", this.packageName);
            XmlUtils.writeIntAttribute(out, "u", this.uid);
            XmlUtils.writeStringAttribute(out, "rn", this.descriptionResEntryName);
            XmlUtils.writeLongAttribute(out, "ex", this.expiryTimeMillis);
            XmlUtils.writeStringAttribute(out, "d", this.description);
        }

        static Leasee createFromXml(XmlPullParser in, int version) throws IOException {
            String descriptionResEntryName;
            CharSequence description;
            String packageName = XmlUtils.readStringAttribute(in, "p");
            int uid = XmlUtils.readIntAttribute(in, "u");
            if (version >= 3) {
                descriptionResEntryName = XmlUtils.readStringAttribute(in, "rn");
            } else {
                descriptionResEntryName = null;
            }
            long expiryTimeMillis = XmlUtils.readLongAttribute(in, "ex");
            if (version >= 2) {
                description = XmlUtils.readStringAttribute(in, "d");
            } else {
                description = null;
            }
            return new Leasee(packageName, uid, descriptionResEntryName, description, expiryTimeMillis);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class Accessor {
        public final String packageName;
        public final int uid;

        Accessor(String packageName, int uid) {
            this.packageName = packageName;
            this.uid = uid;
        }

        public boolean equals(String packageName, int uid) {
            return this.uid == uid && this.packageName.equals(packageName);
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || !(obj instanceof Accessor)) {
                return false;
            }
            Accessor other = (Accessor) obj;
            if (this.uid == other.uid && this.packageName.equals(other.packageName)) {
                return true;
            }
            return false;
        }

        public int hashCode() {
            return Objects.hash(this.packageName, Integer.valueOf(this.uid));
        }

        public String toString() {
            return "[" + this.packageName + ", " + this.uid + "]";
        }
    }
}
