package com.android.server.graphics.fonts;

import android.graphics.fonts.FontUpdateRequest;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Slog;
import android.util.TypedXmlPullParser;
import android.util.TypedXmlSerializer;
import android.util.Xml;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes.dex */
class PersistentSystemFontConfig {
    private static final String ATTR_VALUE = "value";
    private static final String TAG = "PersistentSystemFontConfig";
    private static final String TAG_FAMILY = "family";
    private static final String TAG_LAST_MODIFIED_DATE = "lastModifiedDate";
    private static final String TAG_ROOT = "fontConfig";
    private static final String TAG_UPDATED_FONT_DIR = "updatedFontDir";

    /* loaded from: classes.dex */
    static class Config {
        public long lastModifiedMillis;
        public final Set<String> updatedFontDirs = new ArraySet();
        public final List<FontUpdateRequest.Family> fontFamilies = new ArrayList();
    }

    PersistentSystemFontConfig() {
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Code restructure failed: missing block: B:19:0x004d, code lost:
        if (r5.equals(com.android.server.graphics.fonts.PersistentSystemFontConfig.TAG_UPDATED_FONT_DIR) != false) goto L16;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public static void loadFromXml(InputStream is, Config out) throws XmlPullParserException, IOException {
        TypedXmlPullParser parser = Xml.resolvePullParser(is);
        while (true) {
            int type = parser.next();
            boolean z = true;
            if (type != 1) {
                if (type == 2) {
                    int depth = parser.getDepth();
                    String tag = parser.getName();
                    if (depth == 1) {
                        if (!TAG_ROOT.equals(tag)) {
                            Slog.e(TAG, "Invalid root tag: " + tag);
                            return;
                        }
                    } else if (depth == 2) {
                        switch (tag.hashCode()) {
                            case -1540845619:
                                if (tag.equals(TAG_LAST_MODIFIED_DATE)) {
                                    z = false;
                                    break;
                                }
                                z = true;
                                break;
                            case -1281860764:
                                if (tag.equals(TAG_FAMILY)) {
                                    z = true;
                                    break;
                                }
                                z = true;
                                break;
                            case -23402365:
                                break;
                            default:
                                z = true;
                                break;
                        }
                        switch (z) {
                            case false:
                                out.lastModifiedMillis = parseLongAttribute(parser, ATTR_VALUE, 0L);
                                continue;
                            case true:
                                out.updatedFontDirs.add(getAttribute(parser, ATTR_VALUE));
                                continue;
                            case true:
                                out.fontFamilies.add(FontUpdateRequest.Family.readFromXml(parser));
                                continue;
                            default:
                                Slog.w(TAG, "Skipping unknown tag: " + tag);
                                continue;
                        }
                    }
                }
            } else {
                return;
            }
        }
    }

    public static void writeToXml(OutputStream os, Config config) throws IOException {
        TypedXmlSerializer out = Xml.resolveSerializer(os);
        out.startDocument((String) null, true);
        out.startTag((String) null, TAG_ROOT);
        out.startTag((String) null, TAG_LAST_MODIFIED_DATE);
        out.attribute((String) null, ATTR_VALUE, Long.toString(config.lastModifiedMillis));
        out.endTag((String) null, TAG_LAST_MODIFIED_DATE);
        for (String dir : config.updatedFontDirs) {
            out.startTag((String) null, TAG_UPDATED_FONT_DIR);
            out.attribute((String) null, ATTR_VALUE, dir);
            out.endTag((String) null, TAG_UPDATED_FONT_DIR);
        }
        List<FontUpdateRequest.Family> fontFamilies = config.fontFamilies;
        for (int i = 0; i < fontFamilies.size(); i++) {
            FontUpdateRequest.Family fontFamily = fontFamilies.get(i);
            out.startTag((String) null, TAG_FAMILY);
            FontUpdateRequest.Family.writeFamilyToXml(out, fontFamily);
            out.endTag((String) null, TAG_FAMILY);
        }
        out.endTag((String) null, TAG_ROOT);
        out.endDocument();
    }

    private static long parseLongAttribute(TypedXmlPullParser parser, String attr, long defValue) {
        String value = parser.getAttributeValue((String) null, attr);
        if (TextUtils.isEmpty(value)) {
            return defValue;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return defValue;
        }
    }

    private static String getAttribute(TypedXmlPullParser parser, String attr) {
        String value = parser.getAttributeValue((String) null, attr);
        return value == null ? "" : value;
    }
}
