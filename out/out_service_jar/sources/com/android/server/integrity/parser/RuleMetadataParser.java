package com.android.server.integrity.parser;

import android.util.TypedXmlPullParser;
import android.util.Xml;
import com.android.server.integrity.model.RuleMetadata;
import java.io.IOException;
import java.io.InputStream;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes.dex */
public class RuleMetadataParser {
    public static final String RULE_PROVIDER_TAG = "P";
    public static final String VERSION_TAG = "V";

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Code restructure failed: missing block: B:11:0x0026, code lost:
        if (r3.equals(com.android.server.integrity.parser.RuleMetadataParser.VERSION_TAG) != false) goto L11;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public static RuleMetadata parse(InputStream inputStream) throws XmlPullParserException, IOException {
        String ruleProvider = "";
        String version = "";
        TypedXmlPullParser xmlPullParser = Xml.resolvePullParser(inputStream);
        while (true) {
            int eventType = xmlPullParser.next();
            boolean z = true;
            if (eventType != 1) {
                if (eventType == 2) {
                    String tag = xmlPullParser.getName();
                    switch (tag.hashCode()) {
                        case 80:
                            if (tag.equals(RULE_PROVIDER_TAG)) {
                                z = false;
                                break;
                            }
                            z = true;
                            break;
                        case 86:
                            break;
                        default:
                            z = true;
                            break;
                    }
                    switch (z) {
                        case false:
                            ruleProvider = xmlPullParser.nextText();
                            continue;
                        case true:
                            version = xmlPullParser.nextText();
                            continue;
                        default:
                            throw new IllegalStateException("Unknown tag in metadata: " + tag);
                    }
                }
            } else {
                return new RuleMetadata(ruleProvider, version);
            }
        }
    }
}
