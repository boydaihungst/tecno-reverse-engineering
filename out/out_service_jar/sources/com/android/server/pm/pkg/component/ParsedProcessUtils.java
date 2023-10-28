package com.android.server.pm.pkg.component;

import android.content.pm.parsing.result.ParseInput;
import android.content.pm.parsing.result.ParseResult;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.util.ArrayMap;
import android.util.ArraySet;
import com.android.internal.R;
import com.android.internal.util.CollectionUtils;
import com.android.internal.util.XmlUtils;
import com.android.server.pm.pkg.parsing.ParsingPackage;
import com.android.server.pm.pkg.parsing.ParsingUtils;
import java.io.IOException;
import java.util.Set;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes2.dex */
public class ParsedProcessUtils {
    private static ParseResult<Set<String>> parseDenyPermission(Set<String> perms, Resources res, XmlResourceParser parser, ParseInput input) throws IOException, XmlPullParserException {
        TypedArray sa = res.obtainAttributes(parser, R.styleable.AndroidManifestDenyPermission);
        try {
            String perm = sa.getNonConfigurationString(0, 0);
            if (perm != null && perm.equals("android.permission.INTERNET")) {
                perms = CollectionUtils.add(perms, perm);
            }
            sa.recycle();
            XmlUtils.skipCurrentTag(parser);
            return input.success(perms);
        } catch (Throwable th) {
            sa.recycle();
            throw th;
        }
    }

    private static ParseResult<Set<String>> parseAllowPermission(Set<String> perms, Resources res, XmlResourceParser parser, ParseInput input) throws IOException, XmlPullParserException {
        TypedArray sa = res.obtainAttributes(parser, R.styleable.AndroidManifestAllowPermission);
        try {
            String perm = sa.getNonConfigurationString(0, 0);
            if (perm != null && perm.equals("android.permission.INTERNET")) {
                perms = CollectionUtils.remove(perms, perm);
            }
            sa.recycle();
            XmlUtils.skipCurrentTag(parser);
            return input.success(perms);
        } catch (Throwable th) {
            sa.recycle();
            throw th;
        }
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    private static ParseResult<ParsedProcess> parseProcess(Set<String> perms, String[] separateProcesses, ParsingPackage pkg, Resources res, XmlResourceParser parser, int flags, ParseInput input) throws IOException, XmlPullParserException {
        char c;
        ParseResult<?> result;
        ParseResult<?> result2;
        ParsedProcessImpl proc = new ParsedProcessImpl();
        TypedArray sa = res.obtainAttributes(parser, R.styleable.AndroidManifestProcess);
        if (perms != null) {
            try {
                proc.setDeniedPermissions(new ArraySet(perms));
            } finally {
                sa.recycle();
            }
        }
        String processName = sa.getNonConfigurationString(1, 0);
        ParseResult<String> processNameResult = ComponentParseUtils.buildProcessName(pkg.getPackageName(), pkg.getPackageName(), processName, flags, separateProcesses, input);
        if (processNameResult.isError()) {
            return input.error(processNameResult);
        }
        String packageName = pkg.getPackageName();
        String className = ParsingUtils.buildClassName(packageName, sa.getNonConfigurationString(0, 0));
        proc.setName((String) processNameResult.getResult());
        proc.putAppClassNameForPackage(packageName, className);
        proc.setGwpAsanMode(sa.getInt(2, -1));
        proc.setMemtagMode(sa.getInt(3, -1));
        if (sa.hasValue(4)) {
            boolean v = sa.getBoolean(4, false);
            proc.setNativeHeapZeroInitialized(v ? 1 : 0);
        }
        sa.recycle();
        int innerDepth = parser.getDepth();
        while (true) {
            int type = parser.next();
            if (type != 1 && (type != 3 || parser.getDepth() > innerDepth)) {
                if (type != 3 && type != 4) {
                    String tagName = parser.getName();
                    switch (tagName.hashCode()) {
                        case -1239165229:
                            if (tagName.equals("allow-permission")) {
                                c = 1;
                                break;
                            }
                            c = 65535;
                            break;
                        case 1658008624:
                            if (tagName.equals("deny-permission")) {
                                c = 0;
                                break;
                            }
                            c = 65535;
                            break;
                        default:
                            c = 65535;
                            break;
                    }
                    switch (c) {
                        case 0:
                            ParseResult<?> denyResult = parseDenyPermission(proc.getDeniedPermissions(), res, parser, input);
                            result = denyResult;
                            if (denyResult.isSuccess()) {
                                proc.setDeniedPermissions((Set) denyResult.getResult());
                            }
                            result2 = result;
                            break;
                        case 1:
                            ParseResult<?> allowResult = parseAllowPermission(proc.getDeniedPermissions(), res, parser, input);
                            result = allowResult;
                            if (allowResult.isSuccess()) {
                                proc.setDeniedPermissions((Set) allowResult.getResult());
                            }
                            result2 = result;
                            break;
                        default:
                            result2 = ParsingUtils.unknownTag("<process>", pkg, parser, input);
                            break;
                    }
                    if (result2.isError()) {
                        return input.error(result2);
                    }
                }
            }
        }
        return input.success(proc);
    }

    /* JADX DEBUG: Type inference failed for r0v13. Raw type applied. Possible types: android.content.pm.parsing.result.ParseResult<com.android.server.pm.pkg.component.ParsedProcess>, android.content.pm.parsing.result.ParseResult */
    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Code restructure failed: missing block: B:25:0x0058, code lost:
        if (r14.equals("allow-permission") != false) goto L27;
     */
    /* JADX WARN: Code restructure failed: missing block: B:53:0x0103, code lost:
        return r20.success(r1);
     */
    /* JADX WARN: Multi-variable type inference failed */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public static ParseResult<ArrayMap<String, ParsedProcess>> parseProcesses(String[] separateProcesses, ParsingPackage pkg, Resources res, XmlResourceParser parser, int flags, ParseInput input) throws IOException, XmlPullParserException {
        ParseResult<?> result;
        ArrayMap<String, ParsedProcess> processes = new ArrayMap<>();
        int innerDepth = parser.getDepth();
        Set<String> deniedPerms = null;
        while (true) {
            int type = parser.next();
            char c = 1;
            if (type != 1 && (type != 3 || parser.getDepth() > innerDepth)) {
                if (type != 3 && type != 4) {
                    String tagName = parser.getName();
                    switch (tagName.hashCode()) {
                        case -1239165229:
                            break;
                        case -309518737:
                            if (tagName.equals("process")) {
                                c = 2;
                                break;
                            }
                            c = 65535;
                            break;
                        case 1658008624:
                            if (tagName.equals("deny-permission")) {
                                c = 0;
                                break;
                            }
                            c = 65535;
                            break;
                        default:
                            c = 65535;
                            break;
                    }
                    switch (c) {
                        case 0:
                            ParseResult<?> denyResult = parseDenyPermission(deniedPerms, res, parser, input);
                            if (denyResult.isSuccess()) {
                                result = denyResult;
                                deniedPerms = (Set) denyResult.getResult();
                                break;
                            } else {
                                result = denyResult;
                                break;
                            }
                        case 1:
                            ParseResult<?> allowResult = parseAllowPermission(deniedPerms, res, parser, input);
                            if (allowResult.isSuccess()) {
                                result = allowResult;
                                deniedPerms = (Set) allowResult.getResult();
                                break;
                            } else {
                                result = allowResult;
                                break;
                            }
                        case 2:
                            ParseResult<?> processResult = parseProcess(deniedPerms, separateProcesses, pkg, res, parser, flags, input);
                            ParseResult<?> result2 = processResult;
                            if (!processResult.isSuccess()) {
                                result = result2;
                                break;
                            } else {
                                ParsedProcess process = (ParsedProcess) processResult.getResult();
                                if (processes.put(process.getName(), process) != null) {
                                    result2 = input.error("<process> specified existing name '" + process.getName() + "'");
                                }
                                result = result2;
                                break;
                            }
                        default:
                            result = ParsingUtils.unknownTag("<processes>", pkg, parser, input);
                            break;
                    }
                    if (result.isError()) {
                        return input.error(result);
                    }
                }
            }
        }
    }
}
