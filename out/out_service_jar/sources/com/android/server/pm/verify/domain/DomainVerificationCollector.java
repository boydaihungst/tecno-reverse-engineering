package com.android.server.pm.verify.domain;

import android.content.IntentFilter;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Patterns;
import com.android.server.SystemConfig;
import com.android.server.compat.PlatformCompat;
import com.android.server.pm.parsing.pkg.AndroidPackage;
import com.android.server.pm.pkg.component.ParsedActivity;
import com.android.server.pm.pkg.component.ParsedIntentInfo;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
/* loaded from: classes2.dex */
public class DomainVerificationCollector {
    private static final int MAX_DOMAINS_BYTE_SIZE = 1048576;
    public static final long RESTRICT_DOMAINS = 175408749;
    private final Matcher mDomainMatcher = DOMAIN_NAME_WITH_WILDCARD.matcher("");
    private final PlatformCompat mPlatformCompat;
    private final SystemConfig mSystemConfig;
    private static final Pattern DOMAIN_NAME_WITH_WILDCARD = Pattern.compile("(\\*\\.)?" + Patterns.DOMAIN_NAME.pattern());
    private static final BiFunction<ArraySet<String>, String, Boolean> ARRAY_SET_COLLECTOR = new BiFunction() { // from class: com.android.server.pm.verify.domain.DomainVerificationCollector$$ExternalSyntheticLambda1
        @Override // java.util.function.BiFunction
        public final Object apply(Object obj, Object obj2) {
            return DomainVerificationCollector.lambda$static$0((ArraySet) obj, (String) obj2);
        }
    };

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ Boolean lambda$static$0(ArraySet set, String domain) {
        set.add(domain);
        return null;
    }

    public DomainVerificationCollector(PlatformCompat platformCompat, SystemConfig systemConfig) {
        this.mPlatformCompat = platformCompat;
        this.mSystemConfig = systemConfig;
    }

    public ArraySet<String> collectAllWebDomains(AndroidPackage pkg) {
        return collectDomains(pkg, false, true);
    }

    public ArraySet<String> collectValidAutoVerifyDomains(AndroidPackage pkg) {
        return collectDomains(pkg, true, true);
    }

    public ArraySet<String> collectInvalidAutoVerifyDomains(AndroidPackage pkg) {
        return collectDomains(pkg, true, false);
    }

    public boolean containsWebDomain(AndroidPackage pkg, final String targetDomain) {
        return collectDomains(pkg, false, true, null, new BiFunction() { // from class: com.android.server.pm.verify.domain.DomainVerificationCollector$$ExternalSyntheticLambda0
            @Override // java.util.function.BiFunction
            public final Object apply(Object obj, Object obj2) {
                return DomainVerificationCollector.lambda$containsWebDomain$1(targetDomain, (Void) obj, (String) obj2);
            }
        }) != null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ Boolean lambda$containsWebDomain$1(String targetDomain, Void unused, String domain) {
        if (Objects.equals(targetDomain, domain)) {
            return true;
        }
        return null;
    }

    public boolean containsAutoVerifyDomain(AndroidPackage pkg, final String targetDomain) {
        return collectDomains(pkg, true, true, null, new BiFunction() { // from class: com.android.server.pm.verify.domain.DomainVerificationCollector$$ExternalSyntheticLambda2
            @Override // java.util.function.BiFunction
            public final Object apply(Object obj, Object obj2) {
                return DomainVerificationCollector.lambda$containsAutoVerifyDomain$2(targetDomain, (Void) obj, (String) obj2);
            }
        }) != null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ Boolean lambda$containsAutoVerifyDomain$2(String targetDomain, Void unused, String domain) {
        if (Objects.equals(targetDomain, domain)) {
            return true;
        }
        return null;
    }

    private ArraySet<String> collectDomains(AndroidPackage pkg, boolean checkAutoVerify, boolean valid) {
        ArraySet<String> domains = new ArraySet<>();
        collectDomains(pkg, checkAutoVerify, valid, domains, ARRAY_SET_COLLECTOR);
        return domains;
    }

    private <InitialValue, ReturnValue> ReturnValue collectDomains(AndroidPackage pkg, boolean checkAutoVerify, boolean valid, InitialValue initialValue, BiFunction<InitialValue, String, ReturnValue> domainCollector) {
        boolean restrictDomains = DomainVerificationUtils.isChangeEnabled(this.mPlatformCompat, pkg, RESTRICT_DOMAINS);
        if (restrictDomains) {
            return (ReturnValue) collectDomainsInternal(pkg, checkAutoVerify, valid, initialValue, domainCollector);
        }
        return (ReturnValue) collectDomainsLegacy(pkg, checkAutoVerify, valid, initialValue, domainCollector);
    }

    private <InitialValue, ReturnValue> ReturnValue collectDomainsLegacy(AndroidPackage pkg, boolean checkAutoVerify, boolean valid, InitialValue initialValue, BiFunction<InitialValue, String, ReturnValue> domainCollector) {
        if (!checkAutoVerify) {
            return (ReturnValue) collectDomainsInternal(pkg, false, true, initialValue, domainCollector);
        }
        List<ParsedActivity> activities = pkg.getActivities();
        int activitiesSize = activities.size();
        boolean needsAutoVerify = this.mSystemConfig.getLinkedApps().contains(pkg.getPackageName());
        if (!needsAutoVerify) {
            for (int activityIndex = 0; activityIndex < activitiesSize && !needsAutoVerify; activityIndex++) {
                ParsedActivity activity = activities.get(activityIndex);
                List<ParsedIntentInfo> intents = activity.getIntents();
                int intentsSize = intents.size();
                for (int intentIndex = 0; intentIndex < intentsSize && !needsAutoVerify; intentIndex++) {
                    ParsedIntentInfo intent = intents.get(intentIndex);
                    needsAutoVerify = intent.getIntentFilter().needsVerification();
                }
            }
            if (!needsAutoVerify) {
                return null;
            }
        }
        int totalSize = 0;
        boolean underMaxSize = true;
        int activityIndex2 = 0;
        while (activityIndex2 < activitiesSize && underMaxSize) {
            ParsedActivity activity2 = activities.get(activityIndex2);
            List<ParsedIntentInfo> intents2 = activity2.getIntents();
            int intentsSize2 = intents2.size();
            int intentIndex2 = 0;
            while (intentIndex2 < intentsSize2 && underMaxSize) {
                ParsedIntentInfo intent2 = intents2.get(intentIndex2);
                IntentFilter intentFilter = intent2.getIntentFilter();
                if (intentFilter.handlesWebUris(false)) {
                    int authorityCount = intentFilter.countDataAuthorities();
                    int index = 0;
                    while (index < authorityCount) {
                        String host = intentFilter.getDataAuthority(index).getHost();
                        List<ParsedActivity> activities2 = activities;
                        int activitiesSize2 = activitiesSize;
                        if (isValidHost(host) == valid) {
                            totalSize += byteSizeOf(host);
                            underMaxSize = totalSize < 1048576;
                            ReturnValue returnValue = domainCollector.apply(initialValue, host);
                            if (returnValue != null) {
                                return returnValue;
                            }
                        }
                        index++;
                        activities = activities2;
                        activitiesSize = activitiesSize2;
                    }
                    continue;
                }
                intentIndex2++;
                activities = activities;
                activitiesSize = activitiesSize;
            }
            activityIndex2++;
            activities = activities;
            activitiesSize = activitiesSize;
        }
        return null;
    }

    private <InitialValue, ReturnValue> ReturnValue collectDomainsInternal(AndroidPackage pkg, boolean checkAutoVerify, boolean valid, InitialValue initialValue, BiFunction<InitialValue, String, ReturnValue> domainCollector) {
        boolean underMaxSize;
        DomainVerificationCollector domainVerificationCollector = this;
        boolean z = checkAutoVerify;
        int totalSize = 0;
        boolean underMaxSize2 = true;
        List<ParsedActivity> activities = pkg.getActivities();
        int activitiesSize = activities.size();
        int activityIndex = 0;
        while (activityIndex < activitiesSize && underMaxSize2) {
            ParsedActivity activity = activities.get(activityIndex);
            List<ParsedIntentInfo> intents = activity.getIntents();
            int intentsSize = intents.size();
            int intentIndex = 0;
            while (intentIndex < intentsSize && underMaxSize2) {
                ParsedIntentInfo intent = intents.get(intentIndex);
                IntentFilter intentFilter = intent.getIntentFilter();
                if (z && !intentFilter.getAutoVerify()) {
                    underMaxSize = underMaxSize2;
                } else if (!intentFilter.hasCategory("android.intent.category.DEFAULT")) {
                    underMaxSize = underMaxSize2;
                } else if (intentFilter.handlesWebUris(z)) {
                    int authorityCount = intentFilter.countDataAuthorities();
                    int index = 0;
                    while (index < authorityCount && underMaxSize2) {
                        String host = intentFilter.getDataAuthority(index).getHost();
                        boolean underMaxSize3 = underMaxSize2;
                        if (domainVerificationCollector.isValidHost(host) == valid) {
                            totalSize += domainVerificationCollector.byteSizeOf(host);
                            boolean underMaxSize4 = totalSize < 1048576;
                            underMaxSize3 = underMaxSize4;
                            ReturnValue returnValue = domainCollector.apply(initialValue, host);
                            if (returnValue != null) {
                                return returnValue;
                            }
                        }
                        index++;
                        domainVerificationCollector = this;
                        underMaxSize2 = underMaxSize3;
                    }
                    underMaxSize = underMaxSize2;
                } else {
                    underMaxSize = underMaxSize2;
                }
                intentIndex++;
                domainVerificationCollector = this;
                z = checkAutoVerify;
                underMaxSize2 = underMaxSize;
            }
            activityIndex++;
            domainVerificationCollector = this;
            z = checkAutoVerify;
            underMaxSize2 = underMaxSize2;
        }
        return null;
    }

    private int byteSizeOf(String string) {
        return android.content.pm.verify.domain.DomainVerificationUtils.estimatedByteSizeOf(string);
    }

    private boolean isValidHost(String host) {
        if (TextUtils.isEmpty(host)) {
            return false;
        }
        this.mDomainMatcher.reset(host);
        return this.mDomainMatcher.matches();
    }
}
