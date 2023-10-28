package com.android.server.slice;

import android.content.ContentProvider;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.AtomicFile;
import android.util.Log;
import android.util.Slog;
import android.util.Xml;
import com.android.internal.util.XmlUtils;
import com.android.server.slice.DirtyTracker;
import com.android.server.slice.SliceProviderPermissions;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Objects;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;
/* loaded from: classes2.dex */
public class SlicePermissionManager implements DirtyTracker {
    static final int DB_VERSION = 2;
    private static final long PERMISSION_CACHE_PERIOD = 300000;
    private static final String SLICE_DIR = "slice";
    private static final String TAG = "SlicePermissionManager";
    private static final String TAG_LIST = "slice-access-list";
    private static final long WRITE_GRACE_PERIOD = 500;
    private final String ATT_VERSION;
    private final ArrayMap<PkgUser, SliceClientPermissions> mCachedClients;
    private final ArrayMap<PkgUser, SliceProviderPermissions> mCachedProviders;
    private final Context mContext;
    private final ArraySet<DirtyTracker.Persistable> mDirty;
    private final Handler mHandler;
    private final File mSliceDir;

    SlicePermissionManager(Context context, Looper looper, File sliceDir) {
        this.ATT_VERSION = "version";
        this.mCachedProviders = new ArrayMap<>();
        this.mCachedClients = new ArrayMap<>();
        this.mDirty = new ArraySet<>();
        this.mContext = context;
        this.mHandler = new H(looper);
        this.mSliceDir = sliceDir;
    }

    public SlicePermissionManager(Context context, Looper looper) {
        this(context, looper, new File(Environment.getDataDirectory(), "system/slice"));
    }

    public void grantFullAccess(String pkg, int userId) {
        PkgUser pkgUser = new PkgUser(pkg, userId);
        SliceClientPermissions client = getClient(pkgUser);
        client.setHasFullAccess(true);
    }

    public void grantSliceAccess(String pkg, int userId, String providerPkg, int providerUser, Uri uri) {
        PkgUser pkgUser = new PkgUser(pkg, userId);
        PkgUser providerPkgUser = new PkgUser(providerPkg, providerUser);
        SliceClientPermissions client = getClient(pkgUser);
        client.grantUri(uri, providerPkgUser);
        SliceProviderPermissions provider = getProvider(providerPkgUser);
        provider.getOrCreateAuthority(ContentProvider.getUriWithoutUserId(uri).getAuthority()).addPkg(pkgUser);
    }

    public void revokeSliceAccess(String pkg, int userId, String providerPkg, int providerUser, Uri uri) {
        PkgUser pkgUser = new PkgUser(pkg, userId);
        PkgUser providerPkgUser = new PkgUser(providerPkg, providerUser);
        SliceClientPermissions client = getClient(pkgUser);
        client.revokeUri(uri, providerPkgUser);
    }

    public void removePkg(String pkg, int userId) {
        PkgUser pkgUser = new PkgUser(pkg, userId);
        SliceProviderPermissions provider = getProvider(pkgUser);
        for (SliceProviderPermissions.SliceAuthority authority : provider.getAuthorities()) {
            for (PkgUser p : authority.getPkgs()) {
                getClient(p).removeAuthority(authority.getAuthority(), userId);
            }
        }
        SliceClientPermissions client = getClient(pkgUser);
        client.clear();
        this.mHandler.obtainMessage(3, pkgUser).sendToTarget();
    }

    public String[] getAllPackagesGranted(String pkg) {
        ArraySet<String> ret = new ArraySet<>();
        for (SliceProviderPermissions.SliceAuthority authority : getProvider(new PkgUser(pkg, 0)).getAuthorities()) {
            for (PkgUser pkgUser : authority.getPkgs()) {
                ret.add(pkgUser.mPkg);
            }
        }
        return (String[]) ret.toArray(new String[ret.size()]);
    }

    public boolean hasFullAccess(String pkg, int userId) {
        PkgUser pkgUser = new PkgUser(pkg, userId);
        return getClient(pkgUser).hasFullAccess();
    }

    public boolean hasPermission(String pkg, int userId, Uri uri) {
        PkgUser pkgUser = new PkgUser(pkg, userId);
        SliceClientPermissions client = getClient(pkgUser);
        int providerUserId = ContentProvider.getUserIdFromUri(uri, userId);
        return client.hasFullAccess() || client.hasPermission(ContentProvider.getUriWithoutUserId(uri), providerUserId);
    }

    @Override // com.android.server.slice.DirtyTracker
    public void onPersistableDirty(DirtyTracker.Persistable obj) {
        this.mHandler.removeMessages(2);
        this.mHandler.obtainMessage(1, obj).sendToTarget();
        this.mHandler.sendEmptyMessageDelayed(2, 500L);
    }

    public void writeBackup(XmlSerializer out) throws IOException, XmlPullParserException {
        String[] list;
        synchronized (this) {
            out.startTag(null, TAG_LIST);
            out.attribute(null, "version", String.valueOf(2));
            DirtyTracker tracker = new DirtyTracker() { // from class: com.android.server.slice.SlicePermissionManager$$ExternalSyntheticLambda0
                @Override // com.android.server.slice.DirtyTracker
                public final void onPersistableDirty(DirtyTracker.Persistable persistable) {
                    SlicePermissionManager.lambda$writeBackup$0(persistable);
                }
            };
            if (this.mHandler.hasMessages(2)) {
                this.mHandler.removeMessages(2);
                handlePersist();
            }
            for (String file : new File(this.mSliceDir.getAbsolutePath()).list()) {
                ParserHolder parser = getParser(file);
                DirtyTracker.Persistable p = null;
                while (true) {
                    if (parser.parser.getEventType() == 1) {
                        break;
                    } else if (parser.parser.getEventType() == 2) {
                        if ("client".equals(parser.parser.getName())) {
                            p = SliceClientPermissions.createFrom(parser.parser, tracker);
                        } else {
                            p = SliceProviderPermissions.createFrom(parser.parser, tracker);
                        }
                    } else {
                        parser.parser.next();
                    }
                }
                if (p != null) {
                    p.writeTo(out);
                } else {
                    Slog.w(TAG, "Invalid or empty slice permissions file: " + file);
                }
                if (parser != null) {
                    parser.close();
                }
            }
            out.endTag(null, TAG_LIST);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$writeBackup$0(DirtyTracker.Persistable obj) {
    }

    public void readRestore(XmlPullParser parser) throws IOException, XmlPullParserException {
        synchronized (this) {
            while (true) {
                if ((parser.getEventType() != 2 || !TAG_LIST.equals(parser.getName())) && parser.getEventType() != 1) {
                    parser.next();
                }
            }
            int xmlVersion = XmlUtils.readIntAttribute(parser, "version", 0);
            if (xmlVersion < 2) {
                return;
            }
            while (parser.getEventType() != 1) {
                if (parser.getEventType() == 2) {
                    if ("client".equals(parser.getName())) {
                        SliceClientPermissions client = SliceClientPermissions.createFrom(parser, this);
                        synchronized (this.mCachedClients) {
                            this.mCachedClients.put(client.getPkg(), client);
                        }
                        onPersistableDirty(client);
                        Handler handler = this.mHandler;
                        handler.sendMessageDelayed(handler.obtainMessage(4, client.getPkg()), 300000L);
                    } else if ("provider".equals(parser.getName())) {
                        SliceProviderPermissions provider = SliceProviderPermissions.createFrom(parser, this);
                        synchronized (this.mCachedProviders) {
                            this.mCachedProviders.put(provider.getPkg(), provider);
                        }
                        onPersistableDirty(provider);
                        Handler handler2 = this.mHandler;
                        handler2.sendMessageDelayed(handler2.obtainMessage(5, provider.getPkg()), 300000L);
                    } else {
                        parser.next();
                    }
                } else {
                    parser.next();
                }
            }
        }
    }

    /* JADX WARN: Removed duplicated region for block: B:48:0x0067 A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private SliceClientPermissions getClient(PkgUser pkgUser) {
        SliceClientPermissions client;
        synchronized (this.mCachedClients) {
            client = this.mCachedClients.get(pkgUser);
        }
        if (client == null) {
            try {
                ParserHolder parser = getParser(SliceClientPermissions.getFileName(pkgUser));
                try {
                    SliceClientPermissions client2 = SliceClientPermissions.createFrom(parser.parser, this);
                    synchronized (this.mCachedClients) {
                        this.mCachedClients.put(pkgUser, client2);
                    }
                    Handler handler = this.mHandler;
                    handler.sendMessageDelayed(handler.obtainMessage(4, pkgUser), 300000L);
                    if (parser != null) {
                        parser.close();
                    }
                    return client2;
                } catch (Throwable th) {
                    if (parser != null) {
                        try {
                            parser.close();
                        } catch (Throwable th2) {
                            th.addSuppressed(th2);
                        }
                    }
                    throw th;
                }
            } catch (FileNotFoundException e) {
                SliceClientPermissions client3 = new SliceClientPermissions(pkgUser, this);
                synchronized (this.mCachedClients) {
                    this.mCachedClients.put(pkgUser, client3);
                }
                return client3;
            } catch (IOException e2) {
                Log.e(TAG, "Can't read client", e2);
                SliceClientPermissions client32 = new SliceClientPermissions(pkgUser, this);
                synchronized (this.mCachedClients) {
                }
            } catch (XmlPullParserException e3) {
                Log.e(TAG, "Can't read client", e3);
                SliceClientPermissions client322 = new SliceClientPermissions(pkgUser, this);
                synchronized (this.mCachedClients) {
                }
            }
        } else {
            return client;
        }
    }

    /* JADX WARN: Removed duplicated region for block: B:48:0x0067 A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private SliceProviderPermissions getProvider(PkgUser pkgUser) {
        SliceProviderPermissions provider;
        synchronized (this.mCachedProviders) {
            provider = this.mCachedProviders.get(pkgUser);
        }
        if (provider == null) {
            try {
                ParserHolder parser = getParser(SliceProviderPermissions.getFileName(pkgUser));
                try {
                    SliceProviderPermissions provider2 = SliceProviderPermissions.createFrom(parser.parser, this);
                    synchronized (this.mCachedProviders) {
                        this.mCachedProviders.put(pkgUser, provider2);
                    }
                    Handler handler = this.mHandler;
                    handler.sendMessageDelayed(handler.obtainMessage(5, pkgUser), 300000L);
                    if (parser != null) {
                        parser.close();
                    }
                    return provider2;
                } catch (Throwable th) {
                    if (parser != null) {
                        try {
                            parser.close();
                        } catch (Throwable th2) {
                            th.addSuppressed(th2);
                        }
                    }
                    throw th;
                }
            } catch (FileNotFoundException e) {
                SliceProviderPermissions provider3 = new SliceProviderPermissions(pkgUser, this);
                synchronized (this.mCachedProviders) {
                    this.mCachedProviders.put(pkgUser, provider3);
                }
                return provider3;
            } catch (IOException e2) {
                Log.e(TAG, "Can't read provider", e2);
                SliceProviderPermissions provider32 = new SliceProviderPermissions(pkgUser, this);
                synchronized (this.mCachedProviders) {
                }
            } catch (XmlPullParserException e3) {
                Log.e(TAG, "Can't read provider", e3);
                SliceProviderPermissions provider322 = new SliceProviderPermissions(pkgUser, this);
                synchronized (this.mCachedProviders) {
                }
            }
        } else {
            return provider;
        }
    }

    private ParserHolder getParser(String fileName) throws FileNotFoundException, XmlPullParserException {
        AtomicFile file = getFile(fileName);
        ParserHolder holder = new ParserHolder();
        holder.input = file.openRead();
        holder.parser = XmlPullParserFactory.newInstance().newPullParser();
        holder.parser.setInput(holder.input, Xml.Encoding.UTF_8.name());
        return holder;
    }

    private AtomicFile getFile(String fileName) {
        if (!this.mSliceDir.exists()) {
            this.mSliceDir.mkdir();
        }
        return new AtomicFile(new File(this.mSliceDir, fileName));
    }

    void handlePersist() {
        synchronized (this) {
            Iterator<DirtyTracker.Persistable> it = this.mDirty.iterator();
            while (it.hasNext()) {
                DirtyTracker.Persistable persistable = it.next();
                AtomicFile file = getFile(persistable.getFileName());
                try {
                    FileOutputStream stream = file.startWrite();
                    try {
                        XmlSerializer out = XmlPullParserFactory.newInstance().newSerializer();
                        out.setOutput(stream, Xml.Encoding.UTF_8.name());
                        persistable.writeTo(out);
                        out.flush();
                        file.finishWrite(stream);
                    } catch (IOException | RuntimeException | XmlPullParserException e) {
                        Slog.w(TAG, "Failed to save access file, restoring backup", e);
                        file.failWrite(stream);
                    }
                } catch (IOException e2) {
                    Slog.w(TAG, "Failed to save access file", e2);
                    return;
                }
            }
            this.mDirty.clear();
        }
    }

    void addDirtyImmediate(DirtyTracker.Persistable obj) {
        this.mDirty.add(obj);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleRemove(PkgUser pkgUser) {
        getFile(SliceClientPermissions.getFileName(pkgUser)).delete();
        getFile(SliceProviderPermissions.getFileName(pkgUser)).delete();
        this.mDirty.remove(this.mCachedClients.remove(pkgUser));
        this.mDirty.remove(this.mCachedProviders.remove(pkgUser));
    }

    /* loaded from: classes2.dex */
    private final class H extends Handler {
        private static final int MSG_ADD_DIRTY = 1;
        private static final int MSG_CLEAR_CLIENT = 4;
        private static final int MSG_CLEAR_PROVIDER = 5;
        private static final int MSG_PERSIST = 2;
        private static final int MSG_REMOVE = 3;

        public H(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    SlicePermissionManager.this.mDirty.add((DirtyTracker.Persistable) msg.obj);
                    return;
                case 2:
                    SlicePermissionManager.this.handlePersist();
                    return;
                case 3:
                    SlicePermissionManager.this.handleRemove((PkgUser) msg.obj);
                    return;
                case 4:
                    synchronized (SlicePermissionManager.this.mCachedClients) {
                        SlicePermissionManager.this.mCachedClients.remove(msg.obj);
                    }
                    return;
                case 5:
                    synchronized (SlicePermissionManager.this.mCachedProviders) {
                        SlicePermissionManager.this.mCachedProviders.remove(msg.obj);
                    }
                    return;
                default:
                    return;
            }
        }
    }

    /* loaded from: classes2.dex */
    public static class PkgUser {
        private static final String FORMAT = "%s@%d";
        private static final String SEPARATOR = "@";
        private final String mPkg;
        private final int mUserId;

        public PkgUser(String pkg, int userId) {
            this.mPkg = pkg;
            this.mUserId = userId;
        }

        public PkgUser(String pkgUserStr) throws IllegalArgumentException {
            try {
                String[] vals = pkgUserStr.split(SEPARATOR, 2);
                this.mPkg = vals[0];
                this.mUserId = Integer.parseInt(vals[1]);
            } catch (Exception e) {
                throw new IllegalArgumentException(e);
            }
        }

        public String getPkg() {
            return this.mPkg;
        }

        public int getUserId() {
            return this.mUserId;
        }

        public int hashCode() {
            return this.mPkg.hashCode() + this.mUserId;
        }

        public boolean equals(Object obj) {
            if (getClass().equals(obj != null ? obj.getClass() : null)) {
                PkgUser other = (PkgUser) obj;
                return Objects.equals(other.mPkg, this.mPkg) && other.mUserId == this.mUserId;
            }
            return false;
        }

        public String toString() {
            return String.format(FORMAT, this.mPkg, Integer.valueOf(this.mUserId));
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class ParserHolder implements AutoCloseable {
        private InputStream input;
        private XmlPullParser parser;

        private ParserHolder() {
        }

        @Override // java.lang.AutoCloseable
        public void close() throws IOException {
            this.input.close();
        }
    }
}
