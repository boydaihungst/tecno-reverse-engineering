package com.android.server.pm.pkg.component;

import android.content.pm.PermissionInfo;
import android.content.pm.parsing.result.ParseInput;
import android.content.pm.parsing.result.ParseResult;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.util.ArrayMap;
import android.util.EventLog;
import android.util.Slog;
import com.android.internal.R;
import com.android.server.pm.PackageManagerService;
import com.android.server.pm.pkg.parsing.ParsingPackage;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes2.dex */
public class ParsedPermissionUtils {
    private static final String TAG = "PackageParsing";

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [141=4] */
    /* JADX WARN: Removed duplicated region for block: B:60:0x019e  */
    /* JADX WARN: Removed duplicated region for block: B:62:0x01a3  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public static ParseResult<ParsedPermission> parsePermission(ParsingPackage pkg, Resources res, XmlResourceParser parser, boolean useRoundIcon, ParseInput input) throws IOException, XmlPullParserException {
        TypedArray sa;
        int otherProtectionFlags;
        String packageName = pkg.getPackageName();
        ParsedPermissionImpl permission = new ParsedPermissionImpl();
        String tag = "<" + parser.getName() + ">";
        TypedArray sa2 = res.obtainAttributes(parser, R.styleable.AndroidManifestPermission);
        try {
            ParseResult<ParsedPermissionImpl> result = ParsedComponentUtils.parseComponent(permission, tag, pkg, sa2, useRoundIcon, input, 8, 5, 1, 0, 6, 2, 9);
            try {
                if (result.isError()) {
                    ParseResult<ParsedPermission> error = input.error(result);
                    sa2.recycle();
                    return error;
                }
                if (sa2.hasValue(11)) {
                    if (PackageManagerService.PLATFORM_PACKAGE_NAME.equals(packageName)) {
                        permission.setBackgroundPermission(sa2.getNonResourceString(11));
                    } else {
                        Slog.w("PackageParsing", packageName + " defines a background permission. Only the 'android' package can do that.");
                    }
                }
                permission.setGroup(sa2.getNonResourceString(4)).setRequestRes(sa2.getResourceId(12, 0)).setProtectionLevel(sa2.getInt(3, 0)).setFlags(sa2.getInt(7, 0));
                int knownCertsResource = sa2.getResourceId(10, 0);
                if (knownCertsResource != 0) {
                    String resourceType = res.getResourceTypeName(knownCertsResource);
                    if (resourceType.equals("array")) {
                        String[] knownCerts = res.getStringArray(knownCertsResource);
                        if (knownCerts != null) {
                            permission.setKnownCerts(knownCerts);
                        }
                    } else {
                        String knownCert = res.getString(knownCertsResource);
                        if (knownCert != null) {
                            permission.setKnownCert(knownCert);
                        }
                    }
                    if (permission.getKnownCerts().isEmpty()) {
                        Slog.w("PackageParsing", packageName + " defines a knownSigner permission but the provided knownCerts resource is null");
                    }
                } else {
                    String knownCert2 = sa2.getString(10);
                    if (knownCert2 != null) {
                        permission.setKnownCert(knownCert2);
                    }
                }
                if (isRuntime(permission) && PackageManagerService.PLATFORM_PACKAGE_NAME.equals(permission.getPackageName())) {
                    if ((permission.getFlags() & 4) != 0 && (permission.getFlags() & 8) != 0) {
                        throw new IllegalStateException("Permission cannot be both soft and hard restricted: " + permission.getName());
                    }
                    sa2.recycle();
                    permission.setProtectionLevel(PermissionInfo.fixProtectionLevel(permission.getProtectionLevel()));
                    otherProtectionFlags = getProtectionFlags(permission) & (-12353);
                    if (otherProtectionFlags != 0 || getProtection(permission) == 2 || getProtection(permission) == 4) {
                        ParseResult<ParsedPermissionImpl> result2 = ComponentParseUtils.parseAllMetaData(pkg, res, parser, tag, permission, input);
                        return !result2.isError() ? input.error(result2) : input.success((ParsedPermission) result2.getResult());
                    }
                    return input.error("<permission> protectionLevel specifies a non-instant, non-appop, non-runtimeOnly flag but is not based on signature or internal type");
                }
                permission.setFlags(permission.getFlags() & (-5));
                permission.setFlags(permission.getFlags() & (-9));
                sa2.recycle();
                permission.setProtectionLevel(PermissionInfo.fixProtectionLevel(permission.getProtectionLevel()));
                otherProtectionFlags = getProtectionFlags(permission) & (-12353);
                if (otherProtectionFlags != 0) {
                }
                ParseResult<ParsedPermissionImpl> result22 = ComponentParseUtils.parseAllMetaData(pkg, res, parser, tag, permission, input);
                if (!result22.isError()) {
                }
            } catch (Throwable th) {
                th = th;
                sa = sa2;
                sa.recycle();
                throw th;
            }
        } catch (Throwable th2) {
            th = th2;
            sa = sa2;
        }
    }

    public static ParseResult<ParsedPermission> parsePermissionTree(ParsingPackage pkg, Resources res, XmlResourceParser parser, boolean useRoundIcon, ParseInput input) throws IOException, XmlPullParserException {
        int index;
        ParsedPermissionImpl permission = new ParsedPermissionImpl();
        String tag = "<" + parser.getName() + ">";
        TypedArray sa = res.obtainAttributes(parser, R.styleable.AndroidManifestPermissionTree);
        try {
            ParseResult<ParsedPermissionImpl> result = ParsedComponentUtils.parseComponent(permission, tag, pkg, sa, useRoundIcon, input, 4, -1, 1, 0, 3, 2, 5);
            if (result.isError()) {
                return input.error(result);
            }
            sa.recycle();
            int index2 = permission.getName().indexOf(46);
            if (index2 <= 0) {
                index = index2;
            } else {
                index = permission.getName().indexOf(46, index2 + 1);
            }
            if (index < 0) {
                return input.error("<permission-tree> name has less than three segments: " + permission.getName());
            }
            permission.setProtectionLevel(0).setTree(true);
            ParseResult<ParsedPermissionImpl> result2 = ComponentParseUtils.parseAllMetaData(pkg, res, parser, tag, permission, input);
            if (!result2.isError()) {
                return input.success((ParsedPermission) result2.getResult());
            }
            return input.error(result2);
        } finally {
            sa.recycle();
        }
    }

    public static ParseResult<ParsedPermissionGroup> parsePermissionGroup(ParsingPackage pkg, Resources res, XmlResourceParser parser, boolean useRoundIcon, ParseInput input) throws IOException, XmlPullParserException {
        TypedArray sa;
        ParsedPermissionGroupImpl permissionGroup = new ParsedPermissionGroupImpl();
        String tag = "<" + parser.getName() + ">";
        TypedArray sa2 = res.obtainAttributes(parser, R.styleable.AndroidManifestPermissionGroup);
        try {
            ParseResult<ParsedPermissionGroupImpl> result = ParsedComponentUtils.parseComponent(permissionGroup, tag, pkg, sa2, useRoundIcon, input, 7, 4, 1, 0, 5, 2, 8);
            if (result.isError()) {
                try {
                    ParseResult<ParsedPermissionGroup> error = input.error(result);
                    sa2.recycle();
                    return error;
                } catch (Throwable th) {
                    th = th;
                    sa = sa2;
                }
            } else {
                sa = sa2;
                try {
                    permissionGroup.setRequestDetailRes(sa.getResourceId(12, 0)).setBackgroundRequestRes(sa.getResourceId(9, 0)).setBackgroundRequestDetailRes(sa.getResourceId(10, 0)).setRequestRes(sa.getResourceId(11, 0)).setPriority(sa.getInt(3, 0)).setFlags(sa.getInt(6, 0));
                    sa.recycle();
                    ParseResult<ParsedPermissionGroupImpl> result2 = ComponentParseUtils.parseAllMetaData(pkg, res, parser, tag, permissionGroup, input);
                    if (!result2.isError()) {
                        return input.success((ParsedPermissionGroup) result2.getResult());
                    }
                    return input.error(result2);
                } catch (Throwable th2) {
                    th = th2;
                }
            }
        } catch (Throwable th3) {
            th = th3;
            sa = sa2;
        }
        sa.recycle();
        throw th;
    }

    public static boolean isRuntime(ParsedPermission permission) {
        return getProtection(permission) == 1;
    }

    public static boolean isAppOp(ParsedPermission permission) {
        return (permission.getProtectionLevel() & 64) != 0;
    }

    public static int getProtection(ParsedPermission permission) {
        return permission.getProtectionLevel() & 15;
    }

    public static int getProtectionFlags(ParsedPermission permission) {
        return permission.getProtectionLevel() & (-16);
    }

    public static int calculateFootprint(ParsedPermission permission) {
        int size = permission.getName().length();
        CharSequence nonLocalizedLabel = permission.getNonLocalizedLabel();
        if (nonLocalizedLabel != null) {
            return size + nonLocalizedLabel.length();
        }
        return size;
    }

    private static boolean isMalformedDuplicate(ParsedPermission p1, ParsedPermission p2) {
        if (p1 == null || p2 == null || p1.isTree() || p2.isTree()) {
            return false;
        }
        return (p1.getProtectionLevel() == p2.getProtectionLevel() && Objects.equals(p1.getGroup(), p2.getGroup())) ? false : true;
    }

    public static boolean declareDuplicatePermission(ParsingPackage pkg) {
        List<ParsedPermission> permissions = pkg.getPermissions();
        int size = permissions.size();
        if (size > 0) {
            ArrayMap<String, ParsedPermission> checkDuplicatePerm = new ArrayMap<>(size);
            for (int i = 0; i < size; i++) {
                ParsedPermission parsedPermission = permissions.get(i);
                String name = parsedPermission.getName();
                ParsedPermission perm = checkDuplicatePerm.get(name);
                if (isMalformedDuplicate(parsedPermission, perm)) {
                    EventLog.writeEvent(1397638484, "213323615", "The package " + pkg.getPackageName() + " seems malicious");
                    return true;
                }
                checkDuplicatePerm.put(name, parsedPermission);
            }
        }
        return false;
    }
}
