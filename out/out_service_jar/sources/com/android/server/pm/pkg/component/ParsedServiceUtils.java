package com.android.server.pm.pkg.component;

import android.content.IntentFilter;
import android.content.pm.parsing.result.ParseInput;
import android.content.pm.parsing.result.ParseResult;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import com.android.internal.R;
import com.android.server.pm.pkg.parsing.ParsingPackage;
import com.android.server.pm.pkg.parsing.ParsingUtils;
import java.io.IOException;
import java.util.Objects;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes2.dex */
public class ParsedServiceUtils {
    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [113=6] */
    /* JADX DEBUG: Failed to insert an additional move for type inference into block B:123:0x0130 */
    /* JADX DEBUG: Type inference failed for r1v16. Raw type applied. Possible types: android.content.pm.parsing.result.ParseResult<android.os.Bundle> */
    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r3v7, types: [boolean] */
    public static ParseResult<ParsedService> parseService(String[] separateProcesses, ParsingPackage pkg, Resources res, XmlResourceParser parser, int flags, boolean useRoundIcon, String defaultSplitName, ParseInput input) throws XmlPullParserException, IOException {
        TypedArray sa;
        ParsedServiceImpl service;
        ParsingPackage parsingPackage;
        String packageName;
        int i;
        String packageName2;
        int i2;
        int i3;
        int i4;
        ParsingPackage parsingPackage2;
        ParseResult parseResult;
        String tag;
        String packageName3 = pkg.getPackageName();
        ParsedServiceImpl service2 = new ParsedServiceImpl();
        String tag2 = parser.getName();
        TypedArray sa2 = res.obtainAttributes(parser, R.styleable.AndroidManifestService);
        String tag3 = tag2;
        try {
            ParseResult<ParsedServiceImpl> result = ParsedMainComponentUtils.parseMainComponent(service2, tag2, separateProcesses, pkg, sa2, flags, useRoundIcon, defaultSplitName, input, 12, 7, 13, 4, 1, 0, 8, 2, 6, 15, 17, 20);
            if (result.isError()) {
                try {
                    ParseResult<ParsedService> error = input.error(result);
                    sa2.recycle();
                    return error;
                } catch (Throwable th) {
                    th = th;
                    sa = sa2;
                }
            } else {
                sa = sa2;
                try {
                    boolean setExported = sa.hasValue(5);
                    int i5 = 0;
                    if (setExported) {
                        try {
                            service = service2;
                            try {
                                service.setExported(sa.getBoolean(5, false));
                            } catch (Throwable th2) {
                                th = th2;
                            }
                        } catch (Throwable th3) {
                            th = th3;
                        }
                    } else {
                        service = service2;
                    }
                    int i6 = 3;
                    try {
                        String permission = sa.getNonConfigurationString(3, 0);
                        service.setPermission(permission != null ? permission : pkg.getPermission());
                        int i7 = 1;
                        int i8 = 2;
                        service.setForegroundServiceType(sa.getInt(19, 0)).setFlags(service.getFlags() | ComponentParseUtils.flag(1, 9, sa) | ComponentParseUtils.flag(2, 10, sa) | ComponentParseUtils.flag(4, 14, sa) | ComponentParseUtils.flag(8, 18, sa) | ComponentParseUtils.flag(1073741824, 11, sa));
                        boolean visibleToEphemeral = sa.getBoolean(16, false);
                        if (visibleToEphemeral) {
                            service.setFlags(service.getFlags() | 1048576);
                            parsingPackage = pkg;
                            try {
                                parsingPackage.setVisibleToInstantApps(true);
                            } catch (Throwable th4) {
                                th = th4;
                            }
                        } else {
                            parsingPackage = pkg;
                        }
                        sa.recycle();
                        if (pkg.isCantSaveState()) {
                            packageName = packageName3;
                            if (Objects.equals(service.getProcessName(), packageName)) {
                                return input.error("Heavy-weight applications can not have services in main process");
                            }
                        } else {
                            packageName = packageName3;
                        }
                        int depth = parser.getDepth();
                        while (true) {
                            int type = parser.next();
                            if (type == i7) {
                                i = i7;
                            } else if (type == i6 && parser.getDepth() <= depth) {
                                i = i7;
                            } else if (type == i8) {
                                String name = parser.getName();
                                int i9 = -1;
                                switch (name.hashCode()) {
                                    case -1115949454:
                                        if (name.equals("meta-data")) {
                                            i9 = i7;
                                            break;
                                        }
                                        break;
                                    case -1029793847:
                                        if (name.equals("intent-filter")) {
                                            i9 = i5;
                                            break;
                                        }
                                        break;
                                    case -993141291:
                                        if (name.equals("property")) {
                                            i9 = i8;
                                            break;
                                        }
                                        break;
                                }
                                switch (i9) {
                                    case 0:
                                        packageName2 = packageName;
                                        i2 = i8;
                                        i3 = i7;
                                        i4 = i6;
                                        ParseResult intentResult = ParsedMainComponentUtils.parseIntentFilter(service, pkg, res, parser, visibleToEphemeral, true, false, false, false, input);
                                        if (intentResult.isSuccess()) {
                                            ParsedIntentInfoImpl intent = (ParsedIntentInfoImpl) intentResult.getResult();
                                            IntentFilter intentFilter = intent.getIntentFilter();
                                            service.setOrder(Math.max(intentFilter.getOrder(), service.getOrder()));
                                            service.addIntent(intent);
                                        }
                                        parsingPackage2 = pkg;
                                        parseResult = intentResult;
                                        tag = tag3;
                                        break;
                                    case 1:
                                        ParseResult parseResult2 = ParsedComponentUtils.addMetaData(service, parsingPackage, res, parser, input);
                                        packageName2 = packageName;
                                        i2 = i8;
                                        i3 = i7;
                                        i4 = i6;
                                        tag = tag3;
                                        parseResult = parseResult2;
                                        parsingPackage2 = parsingPackage;
                                        break;
                                    case 2:
                                        ParseResult parseResult3 = ParsedComponentUtils.addProperty(service, parsingPackage, res, parser, input);
                                        packageName2 = packageName;
                                        i2 = i8;
                                        i3 = i7;
                                        i4 = i6;
                                        tag = tag3;
                                        parseResult = parseResult3;
                                        parsingPackage2 = parsingPackage;
                                        break;
                                    default:
                                        packageName2 = packageName;
                                        i2 = i8;
                                        i3 = i7;
                                        i4 = i6;
                                        parsingPackage2 = pkg;
                                        tag = tag3;
                                        parseResult = ParsingUtils.unknownTag(tag, parsingPackage2, parser, input);
                                        break;
                                }
                                if (parseResult.isError()) {
                                    return input.error(parseResult);
                                }
                                parsingPackage = parsingPackage2;
                                tag3 = tag;
                                i8 = i2;
                                i7 = i3;
                                i6 = i4;
                                packageName = packageName2;
                                i5 = 0;
                            }
                        }
                        if (!setExported) {
                            int i10 = i;
                            if (service.getIntents().size() <= 0) {
                                i10 = 0;
                            }
                            ?? r3 = i10;
                            if (r3 != false) {
                                ParseResult exportedCheckResult = input.deferError(service.getName() + ": Targeting S+ (version 31 and above) requires that an explicit value for android:exported be defined when intent filters are present", 150232615L);
                                if (exportedCheckResult.isError()) {
                                    return input.error(exportedCheckResult);
                                }
                            }
                            service.setExported(r3);
                        }
                        return input.success(service);
                    } catch (Throwable th5) {
                        th = th5;
                    }
                } catch (Throwable th6) {
                    th = th6;
                }
            }
        } catch (Throwable th7) {
            th = th7;
            sa = sa2;
        }
        sa.recycle();
        throw th;
    }
}
