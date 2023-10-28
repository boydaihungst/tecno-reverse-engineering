package com.android.server.firewall;

import com.android.internal.util.XmlUtils;
import java.io.IOException;
import java.util.ArrayList;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public abstract class FilterList implements Filter {
    protected final ArrayList<Filter> children = new ArrayList<>();

    public FilterList readFromXml(XmlPullParser parser) throws IOException, XmlPullParserException {
        int outerDepth = parser.getDepth();
        while (XmlUtils.nextElementWithin(parser, outerDepth)) {
            readChild(parser);
        }
        return this;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void readChild(XmlPullParser parser) throws IOException, XmlPullParserException {
        Filter filter = IntentFirewall.parseFilter(parser);
        this.children.add(filter);
    }
}
