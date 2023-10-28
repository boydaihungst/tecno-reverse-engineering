package com.android.server.pm.pkg.component;

import android.content.IntentFilter;
import android.content.pm.parsing.result.ParseInput;
import android.content.pm.parsing.result.ParseResult;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.util.TypedValue;
import com.android.internal.R;
import com.android.server.pm.pkg.parsing.ParsingPackage;
import com.android.server.pm.pkg.parsing.ParsingPackageUtils;
import com.android.server.pm.pkg.parsing.ParsingUtils;
import java.io.IOException;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes2.dex */
public class ParsedIntentInfoUtils {
    public static final boolean DEBUG = false;
    private static final String TAG = "PackageParsing";

    public static ParseResult<ParsedIntentInfoImpl> parseIntentInfo(String className, ParsingPackage pkg, Resources res, XmlResourceParser parser, boolean allowGlobs, boolean allowAutoVerify, ParseInput input) throws XmlPullParserException, IOException {
        ParseResult result;
        ParsedIntentInfoImpl intentInfo = new ParsedIntentInfoImpl();
        IntentFilter intentFilter = intentInfo.getIntentFilter();
        TypedArray sa = res.obtainAttributes(parser, R.styleable.AndroidManifestIntentFilter);
        int i = 2;
        int i2 = 0;
        try {
            intentFilter.setPriority(sa.getInt(2, 0));
            int i3 = 3;
            intentFilter.setOrder(sa.getInt(3, 0));
            TypedValue v = sa.peekValue(0);
            if (v != null) {
                intentInfo.setLabelRes(v.resourceId);
                if (v.resourceId == 0) {
                    intentInfo.setNonLocalizedLabel(v.coerceToString());
                }
            }
            if (ParsingPackageUtils.sUseRoundIcon) {
                intentInfo.setIcon(sa.getResourceId(7, 0));
            }
            int i4 = 1;
            if (intentInfo.getIcon() == 0) {
                intentInfo.setIcon(sa.getResourceId(1, 0));
            }
            if (allowAutoVerify) {
                intentFilter.setAutoVerify(sa.getBoolean(6, false));
            }
            sa.recycle();
            int depth = parser.getDepth();
            while (true) {
                int type = parser.next();
                if (type != i4 && (type != i3 || parser.getDepth() > depth)) {
                    if (type == i) {
                        String nodeName = parser.getName();
                        int i5 = -1;
                        switch (nodeName.hashCode()) {
                            case -1422950858:
                                if (nodeName.equals("action")) {
                                    i5 = i2;
                                    break;
                                }
                                break;
                            case 3076010:
                                if (nodeName.equals("data")) {
                                    i5 = i;
                                    break;
                                }
                                break;
                            case 50511102:
                                if (nodeName.equals("category")) {
                                    i5 = i4;
                                    break;
                                }
                                break;
                        }
                        switch (i5) {
                            case 0:
                                String value = parser.getAttributeValue(ParsingUtils.ANDROID_RES_NAMESPACE, "name");
                                if (value == null) {
                                    result = input.error("No value supplied for <android:name>");
                                    break;
                                } else if (value.isEmpty()) {
                                    intentFilter.addAction(value);
                                    result = input.deferError("No value supplied for <android:name>", 151163173L);
                                    break;
                                } else {
                                    intentFilter.addAction(value);
                                    result = input.success((Object) null);
                                    break;
                                }
                            case 1:
                                String value2 = parser.getAttributeValue(ParsingUtils.ANDROID_RES_NAMESPACE, "name");
                                if (value2 == null) {
                                    result = input.error("No value supplied for <android:name>");
                                    break;
                                } else if (value2.isEmpty()) {
                                    intentFilter.addCategory(value2);
                                    result = input.deferError("No value supplied for <android:name>", 151163173L);
                                    break;
                                } else {
                                    intentFilter.addCategory(value2);
                                    result = input.success((Object) null);
                                    break;
                                }
                            case 2:
                                result = parseData(intentInfo, res, parser, allowGlobs, input);
                                break;
                            default:
                                result = ParsingUtils.unknownTag("<intent-filter>", pkg, parser, input);
                                break;
                        }
                        if (result.isError()) {
                            return input.error(result);
                        }
                        i = 2;
                        i2 = 0;
                        i3 = 3;
                        i4 = 1;
                    }
                }
            }
            intentInfo.setHasDefault(intentFilter.hasCategory("android.intent.category.DEFAULT"));
            return input.success(intentInfo);
        } catch (Throwable th) {
            sa.recycle();
            throw th;
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [286=7] */
    private static ParseResult<ParsedIntentInfo> parseData(ParsedIntentInfo intentInfo, Resources resources, XmlResourceParser parser, boolean allowGlobs, ParseInput input) {
        IntentFilter intentFilter = intentInfo.getIntentFilter();
        TypedArray sa = resources.obtainAttributes(parser, R.styleable.AndroidManifestData);
        try {
            String str = sa.getNonConfigurationString(0, 0);
            if (str != null) {
                intentFilter.addDataType(str);
            }
            String str2 = sa.getNonConfigurationString(10, 0);
            if (str2 != null) {
                intentFilter.addMimeGroup(str2);
            }
            String str3 = sa.getNonConfigurationString(1, 0);
            if (str3 != null) {
                intentFilter.addDataScheme(str3);
            }
            String str4 = sa.getNonConfigurationString(7, 0);
            if (str4 != null) {
                intentFilter.addDataSchemeSpecificPart(str4, 0);
            }
            String str5 = sa.getNonConfigurationString(8, 0);
            if (str5 != null) {
                intentFilter.addDataSchemeSpecificPart(str5, 1);
            }
            String str6 = sa.getNonConfigurationString(9, 0);
            if (str6 != null) {
                if (!allowGlobs) {
                    return input.error("sspPattern not allowed here; ssp must be literal");
                }
                intentFilter.addDataSchemeSpecificPart(str6, 2);
            }
            String str7 = sa.getNonConfigurationString(14, 0);
            if (str7 != null) {
                if (!allowGlobs) {
                    return input.error("sspAdvancedPattern not allowed here; ssp must be literal");
                }
                intentFilter.addDataSchemeSpecificPart(str7, 3);
            }
            String str8 = sa.getNonConfigurationString(12, 0);
            if (str8 != null) {
                intentFilter.addDataSchemeSpecificPart(str8, 4);
            }
            String host = sa.getNonConfigurationString(2, 0);
            String port = sa.getNonConfigurationString(3, 0);
            if (host != null) {
                intentFilter.addDataAuthority(host, port);
            }
            String str9 = sa.getNonConfigurationString(4, 0);
            if (str9 != null) {
                intentFilter.addDataPath(str9, 0);
            }
            String str10 = sa.getNonConfigurationString(5, 0);
            if (str10 != null) {
                intentFilter.addDataPath(str10, 1);
            }
            String str11 = sa.getNonConfigurationString(6, 0);
            if (str11 != null) {
                if (!allowGlobs) {
                    return input.error("pathPattern not allowed here; path must be literal");
                }
                intentFilter.addDataPath(str11, 2);
            }
            String str12 = sa.getNonConfigurationString(13, 0);
            if (str12 != null) {
                if (!allowGlobs) {
                    return input.error("pathAdvancedPattern not allowed here; path must be literal");
                }
                intentFilter.addDataPath(str12, 3);
            }
            String str13 = sa.getNonConfigurationString(11, 0);
            if (str13 != null) {
                intentFilter.addDataPath(str13, 4);
            }
            return input.success((Object) null);
        } catch (IntentFilter.MalformedMimeTypeException e) {
            return input.error(e.toString());
        } finally {
            sa.recycle();
        }
    }
}
