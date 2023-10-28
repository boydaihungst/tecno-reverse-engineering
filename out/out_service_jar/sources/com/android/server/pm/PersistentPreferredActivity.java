package com.android.server.pm;

import android.content.ComponentName;
import android.content.IntentFilter;
import android.util.TypedXmlPullParser;
import android.util.TypedXmlSerializer;
import com.android.internal.util.XmlUtils;
import com.android.server.utils.SnapshotCache;
import java.io.IOException;
import org.xmlpull.v1.XmlPullParserException;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class PersistentPreferredActivity extends WatchedIntentFilter {
    private static final String ATTR_FILTER = "filter";
    private static final String ATTR_NAME = "name";
    private static final String ATTR_SET_BY_DPM = "set-by-dpm";
    private static final boolean DEBUG_FILTERS = false;
    private static final String TAG = "PersistentPreferredActivity";
    final ComponentName mComponent;
    final boolean mIsSetByDpm;
    final SnapshotCache<PersistentPreferredActivity> mSnapshot;

    private SnapshotCache makeCache() {
        return new SnapshotCache<PersistentPreferredActivity>(this, this) { // from class: com.android.server.pm.PersistentPreferredActivity.1
            /* JADX DEBUG: Method merged with bridge method */
            /* JADX WARN: Can't rename method to resolve collision */
            @Override // com.android.server.utils.SnapshotCache
            public PersistentPreferredActivity createSnapshot() {
                PersistentPreferredActivity s = new PersistentPreferredActivity();
                s.seal();
                return s;
            }
        };
    }

    PersistentPreferredActivity(IntentFilter filter, ComponentName activity, boolean isSetByDpm) {
        super(filter);
        this.mComponent = activity;
        this.mIsSetByDpm = isSetByDpm;
        this.mSnapshot = makeCache();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public PersistentPreferredActivity(WatchedIntentFilter filter, ComponentName activity, boolean isSetByDpm) {
        this(filter.mFilter, activity, isSetByDpm);
    }

    private PersistentPreferredActivity(PersistentPreferredActivity f) {
        super(f);
        this.mComponent = f.mComponent;
        this.mIsSetByDpm = f.mIsSetByDpm;
        this.mSnapshot = new SnapshotCache.Sealed();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public PersistentPreferredActivity(TypedXmlPullParser parser) throws XmlPullParserException, IOException {
        String shortComponent = parser.getAttributeValue((String) null, "name");
        ComponentName unflattenFromString = ComponentName.unflattenFromString(shortComponent);
        this.mComponent = unflattenFromString;
        if (unflattenFromString == null) {
            PackageManagerService.reportSettingsProblem(5, "Error in package manager settings: Bad activity name " + shortComponent + " at " + parser.getPositionDescription());
        }
        this.mIsSetByDpm = parser.getAttributeBoolean((String) null, ATTR_SET_BY_DPM, false);
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
                PackageManagerService.reportSettingsProblem(5, "Unknown element: " + tagName + " at " + parser.getPositionDescription());
                XmlUtils.skipCurrentTag(parser);
            }
        }
        if (tagName.equals(ATTR_FILTER)) {
            this.mFilter.readFromXml(parser);
        } else {
            PackageManagerService.reportSettingsProblem(5, "Missing element filter at " + parser.getPositionDescription());
            XmlUtils.skipCurrentTag(parser);
        }
        this.mSnapshot = makeCache();
    }

    public void writeToXml(TypedXmlSerializer serializer) throws IOException {
        serializer.attribute((String) null, "name", this.mComponent.flattenToShortString());
        serializer.attributeBoolean((String) null, ATTR_SET_BY_DPM, this.mIsSetByDpm);
        serializer.startTag((String) null, ATTR_FILTER);
        this.mFilter.writeToXml(serializer);
        serializer.endTag((String) null, ATTR_FILTER);
    }

    @Override // com.android.server.pm.WatchedIntentFilter
    public IntentFilter getIntentFilter() {
        return this.mFilter;
    }

    public String toString() {
        return "PersistentPreferredActivity{0x" + Integer.toHexString(System.identityHashCode(this)) + " " + this.mComponent.flattenToShortString() + ", mIsSetByDpm=" + this.mIsSetByDpm + "}";
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.WatchedIntentFilter, com.android.server.utils.Snappable
    public PersistentPreferredActivity snapshot() {
        return this.mSnapshot.snapshot();
    }
}
