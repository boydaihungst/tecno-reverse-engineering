package com.android.server.slice;

import android.net.Uri;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Slog;
import com.android.server.slice.DirtyTracker;
import com.android.server.slice.SliceClientPermissions;
import com.android.server.slice.SlicePermissionManager;
import com.android.server.wm.ActivityTaskManagerInternal;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;
/* loaded from: classes2.dex */
public class SliceClientPermissions implements DirtyTracker, DirtyTracker.Persistable {
    private static final String ATTR_AUTHORITY = "authority";
    private static final String ATTR_FULL_ACCESS = "fullAccess";
    private static final String ATTR_PKG = "pkg";
    private static final String NAMESPACE = null;
    private static final String TAG = "SliceClientPermissions";
    private static final String TAG_AUTHORITY = "authority";
    static final String TAG_CLIENT = "client";
    private static final String TAG_PATH = "path";
    private final ArrayMap<SlicePermissionManager.PkgUser, SliceAuthority> mAuths = new ArrayMap<>();
    private boolean mHasFullAccess;
    private final SlicePermissionManager.PkgUser mPkg;
    private final DirtyTracker mTracker;

    public SliceClientPermissions(SlicePermissionManager.PkgUser pkg, DirtyTracker tracker) {
        this.mPkg = pkg;
        this.mTracker = tracker;
    }

    public SlicePermissionManager.PkgUser getPkg() {
        return this.mPkg;
    }

    public synchronized Collection<SliceAuthority> getAuthorities() {
        return new ArrayList(this.mAuths.values());
    }

    public synchronized SliceAuthority getOrCreateAuthority(SlicePermissionManager.PkgUser authority, SlicePermissionManager.PkgUser provider) {
        SliceAuthority ret;
        ret = this.mAuths.get(authority);
        if (ret == null) {
            ret = new SliceAuthority(authority.getPkg(), provider, this);
            this.mAuths.put(authority, ret);
            onPersistableDirty(ret);
        }
        return ret;
    }

    public synchronized SliceAuthority getAuthority(SlicePermissionManager.PkgUser authority) {
        return this.mAuths.get(authority);
    }

    public boolean hasFullAccess() {
        return this.mHasFullAccess;
    }

    public void setHasFullAccess(boolean hasFullAccess) {
        if (this.mHasFullAccess == hasFullAccess) {
            return;
        }
        this.mHasFullAccess = hasFullAccess;
        this.mTracker.onPersistableDirty(this);
    }

    public void removeAuthority(String authority, int userId) {
        if (this.mAuths.remove(new SlicePermissionManager.PkgUser(authority, userId)) != null) {
            this.mTracker.onPersistableDirty(this);
        }
    }

    public synchronized boolean hasPermission(Uri uri, int userId) {
        boolean z = false;
        if (Objects.equals(ActivityTaskManagerInternal.ASSIST_KEY_CONTENT, uri.getScheme())) {
            SliceAuthority authority = getAuthority(new SlicePermissionManager.PkgUser(uri.getAuthority(), userId));
            if (authority != null) {
                if (authority.hasPermission(uri.getPathSegments())) {
                    z = true;
                }
            }
            return z;
        }
        return false;
    }

    public void grantUri(Uri uri, SlicePermissionManager.PkgUser providerPkg) {
        SliceAuthority authority = getOrCreateAuthority(new SlicePermissionManager.PkgUser(uri.getAuthority(), providerPkg.getUserId()), providerPkg);
        authority.addPath(uri.getPathSegments());
    }

    public void revokeUri(Uri uri, SlicePermissionManager.PkgUser providerPkg) {
        SliceAuthority authority = getOrCreateAuthority(new SlicePermissionManager.PkgUser(uri.getAuthority(), providerPkg.getUserId()), providerPkg);
        authority.removePath(uri.getPathSegments());
    }

    public void clear() {
        if (this.mHasFullAccess || !this.mAuths.isEmpty()) {
            this.mHasFullAccess = false;
            this.mAuths.clear();
            onPersistableDirty(this);
        }
    }

    @Override // com.android.server.slice.DirtyTracker
    public void onPersistableDirty(DirtyTracker.Persistable obj) {
        this.mTracker.onPersistableDirty(this);
    }

    @Override // com.android.server.slice.DirtyTracker.Persistable
    public String getFileName() {
        return getFileName(this.mPkg);
    }

    @Override // com.android.server.slice.DirtyTracker.Persistable
    public synchronized void writeTo(XmlSerializer out) throws IOException {
        String str = NAMESPACE;
        out.startTag(str, TAG_CLIENT);
        out.attribute(str, ATTR_PKG, this.mPkg.toString());
        out.attribute(str, ATTR_FULL_ACCESS, this.mHasFullAccess ? "1" : "0");
        int N = this.mAuths.size();
        for (int i = 0; i < N; i++) {
            String str2 = NAMESPACE;
            out.startTag(str2, "authority");
            out.attribute(str2, "authority", this.mAuths.valueAt(i).mAuthority);
            out.attribute(str2, ATTR_PKG, this.mAuths.valueAt(i).mPkg.toString());
            this.mAuths.valueAt(i).writeTo(out);
            out.endTag(str2, "authority");
        }
        out.endTag(NAMESPACE, TAG_CLIENT);
    }

    public static SliceClientPermissions createFrom(XmlPullParser parser, DirtyTracker tracker) throws XmlPullParserException, IOException {
        while (true) {
            if (parser.getEventType() != 2 || !TAG_CLIENT.equals(parser.getName())) {
                int depth = parser.getEventType();
                if (depth == 1) {
                    throw new XmlPullParserException("Can't find client tag in xml");
                }
                parser.next();
            } else {
                int depth2 = parser.getDepth();
                String str = NAMESPACE;
                SlicePermissionManager.PkgUser pkgUser = new SlicePermissionManager.PkgUser(parser.getAttributeValue(str, ATTR_PKG));
                SliceClientPermissions provider = new SliceClientPermissions(pkgUser, tracker);
                String fullAccess = parser.getAttributeValue(str, ATTR_FULL_ACCESS);
                if (fullAccess == null) {
                    fullAccess = "0";
                }
                provider.mHasFullAccess = Integer.parseInt(fullAccess) != 0;
                parser.next();
                while (parser.getDepth() > depth2) {
                    if (parser.getEventType() == 1) {
                        return provider;
                    }
                    if (parser.getEventType() == 2 && "authority".equals(parser.getName())) {
                        try {
                            String str2 = NAMESPACE;
                            SlicePermissionManager.PkgUser pkg = new SlicePermissionManager.PkgUser(parser.getAttributeValue(str2, ATTR_PKG));
                            SliceAuthority authority = new SliceAuthority(parser.getAttributeValue(str2, "authority"), pkg, provider);
                            authority.readFrom(parser);
                            provider.mAuths.put(new SlicePermissionManager.PkgUser(authority.getAuthority(), pkg.getUserId()), authority);
                        } catch (IllegalArgumentException e) {
                            Slog.e(TAG, "Couldn't read PkgUser", e);
                        }
                    }
                    parser.next();
                }
                return provider;
            }
        }
    }

    public static String getFileName(SlicePermissionManager.PkgUser pkg) {
        return String.format("client_%s", pkg.toString());
    }

    /* loaded from: classes2.dex */
    public static class SliceAuthority implements DirtyTracker.Persistable {
        public static final String DELIMITER = "/";
        private final String mAuthority;
        private final ArraySet<String[]> mPaths = new ArraySet<>();
        private final SlicePermissionManager.PkgUser mPkg;
        private final DirtyTracker mTracker;

        public SliceAuthority(String authority, SlicePermissionManager.PkgUser pkg, DirtyTracker tracker) {
            this.mAuthority = authority;
            this.mPkg = pkg;
            this.mTracker = tracker;
        }

        public String getAuthority() {
            return this.mAuthority;
        }

        public SlicePermissionManager.PkgUser getPkg() {
            return this.mPkg;
        }

        void addPath(List<String> path) {
            String[] pathSegs = (String[]) path.toArray(new String[path.size()]);
            for (int i = this.mPaths.size() - 1; i >= 0; i--) {
                String[] existing = this.mPaths.valueAt(i);
                if (isPathPrefixMatch(existing, pathSegs)) {
                    return;
                }
                if (isPathPrefixMatch(pathSegs, existing)) {
                    this.mPaths.removeAt(i);
                }
            }
            this.mPaths.add(pathSegs);
            this.mTracker.onPersistableDirty(this);
        }

        void removePath(List<String> path) {
            boolean changed = false;
            String[] pathSegs = (String[]) path.toArray(new String[path.size()]);
            for (int i = this.mPaths.size() - 1; i >= 0; i--) {
                String[] existing = this.mPaths.valueAt(i);
                if (isPathPrefixMatch(pathSegs, existing)) {
                    changed = true;
                    this.mPaths.removeAt(i);
                }
            }
            if (changed) {
                this.mTracker.onPersistableDirty(this);
            }
        }

        public synchronized Collection<String[]> getPaths() {
            return new ArraySet((ArraySet) this.mPaths);
        }

        public boolean hasPermission(List<String> path) {
            Iterator<String[]> it = this.mPaths.iterator();
            while (it.hasNext()) {
                String[] p = it.next();
                if (isPathPrefixMatch(p, (String[]) path.toArray(new String[path.size()]))) {
                    return true;
                }
            }
            return false;
        }

        private boolean isPathPrefixMatch(String[] prefix, String[] path) {
            int prefixSize = prefix.length;
            if (path.length < prefixSize) {
                return false;
            }
            for (int i = 0; i < prefixSize; i++) {
                if (!Objects.equals(path[i], prefix[i])) {
                    return false;
                }
            }
            return true;
        }

        @Override // com.android.server.slice.DirtyTracker.Persistable
        public String getFileName() {
            return null;
        }

        @Override // com.android.server.slice.DirtyTracker.Persistable
        public synchronized void writeTo(XmlSerializer out) throws IOException {
            int N = this.mPaths.size();
            for (int i = 0; i < N; i++) {
                String[] segments = this.mPaths.valueAt(i);
                if (segments != null) {
                    out.startTag(SliceClientPermissions.NAMESPACE, SliceClientPermissions.TAG_PATH);
                    out.text(encodeSegments(segments));
                    out.endTag(SliceClientPermissions.NAMESPACE, SliceClientPermissions.TAG_PATH);
                }
            }
        }

        public synchronized void readFrom(XmlPullParser parser) throws IOException, XmlPullParserException {
            parser.next();
            int depth = parser.getDepth();
            while (parser.getDepth() >= depth) {
                if (parser.getEventType() == 2 && SliceClientPermissions.TAG_PATH.equals(parser.getName())) {
                    this.mPaths.add(decodeSegments(parser.nextText()));
                }
                parser.next();
            }
        }

        private String encodeSegments(String[] s) {
            String[] out = new String[s.length];
            for (int i = 0; i < s.length; i++) {
                out[i] = Uri.encode(s[i]);
            }
            return TextUtils.join(DELIMITER, out);
        }

        private String[] decodeSegments(String s) {
            String[] sets = s.split(DELIMITER, -1);
            for (int i = 0; i < sets.length; i++) {
                sets[i] = Uri.decode(sets[i]);
            }
            return sets;
        }

        public boolean equals(Object obj) {
            if (getClass().equals(obj != null ? obj.getClass() : null)) {
                SliceAuthority other = (SliceAuthority) obj;
                if (this.mPaths.size() != other.mPaths.size()) {
                    return false;
                }
                ArrayList<String[]> p1 = new ArrayList<>(this.mPaths);
                ArrayList<String[]> p2 = new ArrayList<>(other.mPaths);
                p1.sort(Comparator.comparing(new Function() { // from class: com.android.server.slice.SliceClientPermissions$SliceAuthority$$ExternalSyntheticLambda1
                    @Override // java.util.function.Function
                    public final Object apply(Object obj2) {
                        String join;
                        join = TextUtils.join(",", (String[]) obj2);
                        return join;
                    }
                }));
                p2.sort(Comparator.comparing(new Function() { // from class: com.android.server.slice.SliceClientPermissions$SliceAuthority$$ExternalSyntheticLambda2
                    @Override // java.util.function.Function
                    public final Object apply(Object obj2) {
                        String join;
                        join = TextUtils.join(",", (String[]) obj2);
                        return join;
                    }
                }));
                for (int i = 0; i < p1.size(); i++) {
                    String[] a1 = p1.get(i);
                    String[] a2 = p2.get(i);
                    if (a1.length != a2.length) {
                        return false;
                    }
                    for (int j = 0; j < a1.length; j++) {
                        if (!Objects.equals(a1[j], a2[j])) {
                            return false;
                        }
                    }
                }
                return Objects.equals(this.mAuthority, other.mAuthority) && Objects.equals(this.mPkg, other.mPkg);
            }
            return false;
        }

        public String toString() {
            return String.format("(%s, %s: %s)", this.mAuthority, this.mPkg.toString(), pathToString(this.mPaths));
        }

        private String pathToString(ArraySet<String[]> paths) {
            return TextUtils.join(", ", (Iterable) paths.stream().map(new Function() { // from class: com.android.server.slice.SliceClientPermissions$SliceAuthority$$ExternalSyntheticLambda0
                @Override // java.util.function.Function
                public final Object apply(Object obj) {
                    String join;
                    join = TextUtils.join(SliceClientPermissions.SliceAuthority.DELIMITER, (String[]) obj);
                    return join;
                }
            }).collect(Collectors.toList()));
        }
    }
}
