package com.android.server.pm.verify.domain;

import android.content.Intent;
import android.content.pm.IntentFilterVerificationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.verify.domain.DomainVerificationInfo;
import android.util.IndentingPrintWriter;
import android.util.Pair;
import android.util.TypedXmlPullParser;
import android.util.TypedXmlSerializer;
import com.android.server.pm.Computer;
import com.android.server.pm.pkg.PackageStateInternal;
import com.android.server.pm.verify.domain.DomainVerificationEnforcer;
import com.android.server.pm.verify.domain.proxy.DomainVerificationProxy;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes2.dex */
public interface DomainVerificationManagerInternal {
    public static final int APPROVAL_LEVEL_DISABLED = -3;
    public static final int APPROVAL_LEVEL_INSTANT_APP = 5;
    public static final int APPROVAL_LEVEL_LEGACY_ALWAYS = 2;
    public static final int APPROVAL_LEVEL_LEGACY_ASK = 1;
    public static final int APPROVAL_LEVEL_NONE = 0;
    public static final int APPROVAL_LEVEL_NOT_INSTALLED = -4;
    public static final int APPROVAL_LEVEL_SELECTION = 3;
    public static final int APPROVAL_LEVEL_UNDECLARED = -2;
    public static final int APPROVAL_LEVEL_UNVERIFIED = -1;
    public static final int APPROVAL_LEVEL_VERIFIED = 4;
    public static final UUID DISABLED_ID = new UUID(0, 0);

    /* loaded from: classes2.dex */
    public @interface ApprovalLevel {
    }

    /* loaded from: classes2.dex */
    public interface Connection extends DomainVerificationEnforcer.Callback {
        int[] getAllUserIds();

        int getCallingUid();

        int getCallingUserId();

        void schedule(int i, Object obj);

        void scheduleWriteSettings();

        Computer snapshot();
    }

    void addLegacySetting(String str, IntentFilterVerificationInfo intentFilterVerificationInfo);

    void addPackage(PackageStateInternal packageStateInternal);

    int approvalLevelForDomain(PackageStateInternal packageStateInternal, Intent intent, long j, int i);

    void clearPackage(String str);

    void clearPackageForUser(String str, int i);

    void clearUser(int i);

    Pair<List<ResolveInfo>, Integer> filterToApprovedApp(Intent intent, List<ResolveInfo> list, int i, Function<String, PackageStateInternal> function);

    UUID generateNewId();

    DomainVerificationCollector getCollector();

    DomainVerificationInfo getDomainVerificationInfo(String str) throws PackageManager.NameNotFoundException;

    UUID getDomainVerificationInfoId(String str);

    int getLegacyState(String str, int i);

    DomainVerificationProxy getProxy();

    DomainVerificationShell getShell();

    void migrateState(PackageStateInternal packageStateInternal, PackageStateInternal packageStateInternal2);

    void printState(Computer computer, IndentingPrintWriter indentingPrintWriter, String str, Integer num) throws PackageManager.NameNotFoundException;

    void readLegacySettings(TypedXmlPullParser typedXmlPullParser) throws IOException, XmlPullParserException;

    void readSettings(Computer computer, TypedXmlPullParser typedXmlPullParser) throws IOException, XmlPullParserException;

    void restoreSettings(Computer computer, TypedXmlPullParser typedXmlPullParser) throws IOException, XmlPullParserException;

    boolean runMessage(int i, Object obj);

    void setConnection(Connection connection);

    int setDomainVerificationStatusInternal(int i, UUID uuid, Set<String> set, int i2) throws PackageManager.NameNotFoundException;

    boolean setLegacyUserState(String str, int i, int i2);

    void setProxy(DomainVerificationProxy domainVerificationProxy);

    void writeSettings(Computer computer, TypedXmlSerializer typedXmlSerializer, boolean z, int i) throws IOException;

    static String approvalLevelToDebugString(int level) {
        switch (level) {
            case -4:
                return "NOT_INSTALLED";
            case -3:
                return "DISABLED";
            case -2:
                return "UNDECLARED";
            case -1:
                return "UNVERIFIED";
            case 0:
                return "NONE";
            case 1:
                return "LEGACY_ASK";
            case 2:
                return "LEGACY_ALWAYS";
            case 3:
                return "USER_SELECTION";
            case 4:
                return "VERIFIED";
            case 5:
                return "INSTANT_APP";
            default:
                return "UNKNOWN";
        }
    }
}
