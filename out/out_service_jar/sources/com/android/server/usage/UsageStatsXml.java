package com.android.server.usage;

import android.util.Slog;
import android.util.Xml;
import com.android.internal.util.XmlUtils;
import java.io.IOException;
import java.io.InputStream;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes2.dex */
public class UsageStatsXml {
    static final String CHECKED_IN_SUFFIX = "-c";
    private static final String TAG = "UsageStatsXml";
    private static final String USAGESTATS_TAG = "usagestats";
    private static final String VERSION_ATTR = "version";

    public static void read(InputStream in, IntervalStats statsOut) throws IOException {
        XmlPullParser parser = Xml.newPullParser();
        try {
            parser.setInput(in, "utf-8");
            XmlUtils.beginDocument(parser, USAGESTATS_TAG);
            String versionStr = parser.getAttributeValue(null, VERSION_ATTR);
            try {
                switch (Integer.parseInt(versionStr)) {
                    case 1:
                        UsageStatsXmlV1.read(parser, statsOut);
                        return;
                    default:
                        Slog.e(TAG, "Unrecognized version " + versionStr);
                        throw new IOException("Unrecognized version " + versionStr);
                }
            } catch (NumberFormatException e) {
                Slog.e(TAG, "Bad version");
                throw new IOException(e);
            }
        } catch (XmlPullParserException e2) {
            Slog.e(TAG, "Failed to parse Xml", e2);
            throw new IOException(e2);
        }
    }
}
