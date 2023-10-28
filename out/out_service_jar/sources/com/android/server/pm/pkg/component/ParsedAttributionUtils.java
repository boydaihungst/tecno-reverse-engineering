package com.android.server.pm.pkg.component;

import android.content.pm.parsing.result.ParseInput;
import android.content.pm.parsing.result.ParseResult;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.util.ArraySet;
import com.android.internal.R;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes2.dex */
public class ParsedAttributionUtils {
    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [70=5] */
    /* JADX WARN: Code restructure failed: missing block: B:49:0x00af, code lost:
        if (r0 != null) goto L35;
     */
    /* JADX WARN: Code restructure failed: missing block: B:50:0x00b1, code lost:
        r0 = java.util.Collections.emptyList();
     */
    /* JADX WARN: Code restructure failed: missing block: B:51:0x00b6, code lost:
        ((java.util.ArrayList) r0).trimToSize();
     */
    /* JADX WARN: Code restructure failed: missing block: B:53:0x00c5, code lost:
        return r13.success(new com.android.server.pm.pkg.component.ParsedAttributionImpl(r4, r5, r0));
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public static ParseResult<ParsedAttribution> parseAttribution(Resources res, XmlResourceParser parser, ParseInput input) throws IOException, XmlPullParserException {
        List<String> inheritFrom = null;
        TypedArray sa = res.obtainAttributes(parser, R.styleable.AndroidManifestAttribution);
        if (sa == null) {
            return input.error("<attribution> could not be parsed");
        }
        try {
            String attributionTag = sa.getNonConfigurationString(1, 0);
            if (attributionTag == null) {
                return input.error("<attribution> does not specify android:tag");
            }
            if (attributionTag.length() > 50) {
                return input.error("android:tag is too long. Max length is 50");
            }
            int label = sa.getResourceId(0, 0);
            if (label == 0) {
                return input.error("<attribution> does not specify android:label");
            }
            sa.recycle();
            int innerDepth = parser.getDepth();
            while (true) {
                int type = parser.next();
                if (type == 1 || (type == 3 && parser.getDepth() <= innerDepth)) {
                    break;
                } else if (type != 3 && type != 4) {
                    String tagName = parser.getName();
                    if (!tagName.equals("inherit-from")) {
                        return input.error("Bad element under <attribution>: " + tagName);
                    }
                    sa = res.obtainAttributes(parser, R.styleable.AndroidManifestAttributionInheritFrom);
                    if (sa == null) {
                        return input.error("<inherit-from> could not be parsed");
                    }
                    try {
                        String inheritFromId = sa.getNonConfigurationString(0, 0);
                        if (inheritFrom == null) {
                            inheritFrom = new ArrayList<>();
                        }
                        inheritFrom.add(inheritFromId);
                    } finally {
                    }
                }
            }
        } finally {
        }
    }

    public static boolean isCombinationValid(List<ParsedAttribution> attributions) {
        if (attributions == null) {
            return true;
        }
        ArraySet<String> attributionTags = new ArraySet<>(attributions.size());
        ArraySet<String> inheritFromAttributionTags = new ArraySet<>();
        int numAttributions = attributions.size();
        if (numAttributions > 10000) {
            return false;
        }
        for (int attributionNum = 0; attributionNum < numAttributions; attributionNum++) {
            boolean wasAdded = attributionTags.add(attributions.get(attributionNum).getTag());
            if (!wasAdded) {
                return false;
            }
        }
        for (int attributionNum2 = 0; attributionNum2 < numAttributions; attributionNum2++) {
            ParsedAttribution feature = attributions.get(attributionNum2);
            List<String> inheritFromList = feature.getInheritFrom();
            int numInheritFrom = inheritFromList.size();
            for (int inheritFromNum = 0; inheritFromNum < numInheritFrom; inheritFromNum++) {
                String inheritFrom = inheritFromList.get(inheritFromNum);
                if (attributionTags.contains(inheritFrom)) {
                    return false;
                }
                boolean wasAdded2 = inheritFromAttributionTags.add(inheritFrom);
                if (!wasAdded2) {
                    return false;
                }
            }
        }
        return true;
    }
}
