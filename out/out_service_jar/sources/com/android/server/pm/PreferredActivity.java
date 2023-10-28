package com.android.server.pm;

import android.content.ComponentName;
import android.content.IntentFilter;
import android.util.TypedXmlPullParser;
import android.util.TypedXmlSerializer;
import com.android.internal.util.XmlUtils;
import com.android.server.pm.PreferredComponent;
import com.android.server.utils.SnapshotCache;
import java.io.IOException;
import java.io.PrintWriter;
import org.xmlpull.v1.XmlPullParserException;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class PreferredActivity extends WatchedIntentFilter implements PreferredComponent.Callbacks {
    private static final boolean DEBUG_FILTERS = false;
    private static final String TAG = "PreferredActivity";
    final PreferredComponent mPref;
    final SnapshotCache<PreferredActivity> mSnapshot;

    private SnapshotCache makeCache() {
        return new SnapshotCache<PreferredActivity>(this, this) { // from class: com.android.server.pm.PreferredActivity.1
            /* JADX DEBUG: Method merged with bridge method */
            /* JADX WARN: Can't rename method to resolve collision */
            @Override // com.android.server.utils.SnapshotCache
            public PreferredActivity createSnapshot() {
                PreferredActivity s = new PreferredActivity();
                s.seal();
                return s;
            }
        };
    }

    PreferredActivity(IntentFilter filter, int match, ComponentName[] set, ComponentName activity, boolean always) {
        super(filter);
        this.mPref = new PreferredComponent(this, match, set, activity, always);
        this.mSnapshot = makeCache();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public PreferredActivity(WatchedIntentFilter filter, int match, ComponentName[] set, ComponentName activity, boolean always) {
        this(filter.mFilter, match, set, activity, always);
    }

    private PreferredActivity(PreferredActivity f) {
        super(f);
        this.mPref = f.mPref;
        this.mSnapshot = new SnapshotCache.Sealed();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public PreferredActivity(TypedXmlPullParser parser) throws XmlPullParserException, IOException {
        this.mPref = new PreferredComponent(this, parser);
        this.mSnapshot = makeCache();
    }

    public void writeToXml(TypedXmlSerializer serializer, boolean full) throws IOException {
        this.mPref.writeToXml(serializer, full);
        serializer.startTag((String) null, "filter");
        this.mFilter.writeToXml(serializer);
        serializer.endTag((String) null, "filter");
    }

    @Override // com.android.server.pm.PreferredComponent.Callbacks
    public boolean onReadTag(String tagName, TypedXmlPullParser parser) throws XmlPullParserException, IOException {
        if (tagName.equals("filter")) {
            this.mFilter.readFromXml(parser);
            return true;
        }
        PackageManagerService.reportSettingsProblem(5, "Unknown element under <preferred-activities>: " + parser.getName());
        XmlUtils.skipCurrentTag(parser);
        return true;
    }

    public void dumpPref(PrintWriter out, String prefix, PreferredActivity filter) {
        this.mPref.dump(out, prefix, filter);
    }

    public String toString() {
        return "PreferredActivity{0x" + Integer.toHexString(System.identityHashCode(this)) + " " + this.mPref.mComponent.flattenToShortString() + "}";
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.WatchedIntentFilter, com.android.server.utils.Snappable
    public PreferredActivity snapshot() {
        return this.mSnapshot.snapshot();
    }
}
