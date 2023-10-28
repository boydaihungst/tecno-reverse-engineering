package com.android.server.pm;

import android.content.IntentFilter;
import android.util.TypedXmlPullParser;
import android.util.TypedXmlSerializer;
import com.android.internal.util.XmlUtils;
import com.android.server.utils.SnapshotCache;
import java.io.IOException;
import org.xmlpull.v1.XmlPullParserException;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class CrossProfileIntentFilter extends WatchedIntentFilter {
    private static final String ATTR_FILTER = "filter";
    private static final String ATTR_FLAGS = "flags";
    private static final String ATTR_OWNER_PACKAGE = "ownerPackage";
    private static final String ATTR_TARGET_USER_ID = "targetUserId";
    private static final String TAG = "CrossProfileIntentFilter";
    final int mFlags;
    final String mOwnerPackage;
    final SnapshotCache<CrossProfileIntentFilter> mSnapshot;
    final int mTargetUserId;

    private SnapshotCache makeCache() {
        return new SnapshotCache<CrossProfileIntentFilter>(this, this) { // from class: com.android.server.pm.CrossProfileIntentFilter.1
            /* JADX DEBUG: Method merged with bridge method */
            /* JADX WARN: Can't rename method to resolve collision */
            @Override // com.android.server.utils.SnapshotCache
            public CrossProfileIntentFilter createSnapshot() {
                CrossProfileIntentFilter s = new CrossProfileIntentFilter();
                s.seal();
                return s;
            }
        };
    }

    CrossProfileIntentFilter(IntentFilter filter, String ownerPackage, int targetUserId, int flags) {
        super(filter);
        this.mTargetUserId = targetUserId;
        this.mOwnerPackage = ownerPackage;
        this.mFlags = flags;
        this.mSnapshot = makeCache();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public CrossProfileIntentFilter(WatchedIntentFilter filter, String ownerPackage, int targetUserId, int flags) {
        this(filter.mFilter, ownerPackage, targetUserId, flags);
    }

    private CrossProfileIntentFilter(CrossProfileIntentFilter f) {
        super(f);
        this.mTargetUserId = f.mTargetUserId;
        this.mOwnerPackage = f.mOwnerPackage;
        this.mFlags = f.mFlags;
        this.mSnapshot = new SnapshotCache.Sealed();
    }

    public int getTargetUserId() {
        return this.mTargetUserId;
    }

    public int getFlags() {
        return this.mFlags;
    }

    public String getOwnerPackage() {
        return this.mOwnerPackage;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public CrossProfileIntentFilter(TypedXmlPullParser parser) throws XmlPullParserException, IOException {
        this.mTargetUserId = parser.getAttributeInt((String) null, ATTR_TARGET_USER_ID, -10000);
        this.mOwnerPackage = getStringFromXml(parser, ATTR_OWNER_PACKAGE, "");
        this.mFlags = parser.getAttributeInt((String) null, ATTR_FLAGS, 0);
        this.mSnapshot = makeCache();
        int outerDepth = parser.getDepth();
        String tagName = parser.getName();
        while (true) {
            int type = parser.next();
            if (type == 1 || (type == 3 && parser.getDepth() <= outerDepth)) {
                break;
            }
            tagName = parser.getName();
            if (type != 3 && type != 4 && type == 2) {
                if (tagName.equals(ATTR_FILTER)) {
                    break;
                }
                String msg = "Unknown element under crossProfile-intent-filters: " + tagName + " at " + parser.getPositionDescription();
                PackageManagerService.reportSettingsProblem(5, msg);
                XmlUtils.skipCurrentTag(parser);
            }
        }
        if (tagName.equals(ATTR_FILTER)) {
            this.mFilter.readFromXml(parser);
            return;
        }
        String msg2 = "Missing element under CrossProfileIntentFilter: filter at " + parser.getPositionDescription();
        PackageManagerService.reportSettingsProblem(5, msg2);
        XmlUtils.skipCurrentTag(parser);
    }

    private String getStringFromXml(TypedXmlPullParser parser, String attribute, String defaultValue) {
        String value = parser.getAttributeValue((String) null, attribute);
        if (value == null) {
            String msg = "Missing element under CrossProfileIntentFilter: " + attribute + " at " + parser.getPositionDescription();
            PackageManagerService.reportSettingsProblem(5, msg);
            return defaultValue;
        }
        return value;
    }

    public void writeToXml(TypedXmlSerializer serializer) throws IOException {
        serializer.attributeInt((String) null, ATTR_TARGET_USER_ID, this.mTargetUserId);
        serializer.attributeInt((String) null, ATTR_FLAGS, this.mFlags);
        serializer.attribute((String) null, ATTR_OWNER_PACKAGE, this.mOwnerPackage);
        serializer.startTag((String) null, ATTR_FILTER);
        this.mFilter.writeToXml(serializer);
        serializer.endTag((String) null, ATTR_FILTER);
    }

    public String toString() {
        return "CrossProfileIntentFilter{0x" + Integer.toHexString(System.identityHashCode(this)) + " " + Integer.toString(this.mTargetUserId) + "}";
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean equalsIgnoreFilter(CrossProfileIntentFilter other) {
        return this.mTargetUserId == other.mTargetUserId && this.mOwnerPackage.equals(other.mOwnerPackage) && this.mFlags == other.mFlags;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.WatchedIntentFilter, com.android.server.utils.Snappable
    public CrossProfileIntentFilter snapshot() {
        return this.mSnapshot.snapshot();
    }
}
