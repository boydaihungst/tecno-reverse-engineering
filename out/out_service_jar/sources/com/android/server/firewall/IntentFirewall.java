package com.android.server.firewall;

import android.app.AppGlobals;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManagerInternal;
import android.os.Environment;
import android.os.FileObserver;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.util.ArrayMap;
import android.util.Slog;
import android.util.Xml;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.XmlUtils;
import com.android.server.EventLogTags;
import com.android.server.IntentResolver;
import com.android.server.LocalServices;
import com.android.server.pm.Computer;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes.dex */
public class IntentFirewall {
    private static final int LOG_PACKAGES_MAX_LENGTH = 150;
    private static final int LOG_PACKAGES_SUFFICIENT_LENGTH = 125;
    private static final File RULES_DIR = new File(Environment.getDataSystemDirectory(), "ifw");
    static final String TAG = "IntentFirewall";
    private static final String TAG_ACTIVITY = "activity";
    private static final String TAG_BROADCAST = "broadcast";
    private static final String TAG_RULES = "rules";
    private static final String TAG_SERVICE = "service";
    private static final int TYPE_ACTIVITY = 0;
    private static final int TYPE_BROADCAST = 1;
    private static final int TYPE_SERVICE = 2;
    private static final HashMap<String, FilterFactory> factoryMap;
    private final AMSInterface mAms;
    final FirewallHandler mHandler;
    private final RuleObserver mObserver;
    private PackageManagerInternal mPackageManager;
    private FirewallIntentResolver mActivityResolver = new FirewallIntentResolver();
    private FirewallIntentResolver mBroadcastResolver = new FirewallIntentResolver();
    private FirewallIntentResolver mServiceResolver = new FirewallIntentResolver();

    /* loaded from: classes.dex */
    public interface AMSInterface {
        int checkComponentPermission(String str, int i, int i2, int i3, boolean z);

        Object getAMSLock();
    }

    static {
        FilterFactory[] factories = {AndFilter.FACTORY, OrFilter.FACTORY, NotFilter.FACTORY, StringFilter.ACTION, StringFilter.COMPONENT, StringFilter.COMPONENT_NAME, StringFilter.COMPONENT_PACKAGE, StringFilter.DATA, StringFilter.HOST, StringFilter.MIME_TYPE, StringFilter.SCHEME, StringFilter.PATH, StringFilter.SSP, CategoryFilter.FACTORY, SenderFilter.FACTORY, SenderPackageFilter.FACTORY, SenderPermissionFilter.FACTORY, PortFilter.FACTORY};
        factoryMap = new HashMap<>((factories.length * 4) / 3);
        for (FilterFactory factory : factories) {
            factoryMap.put(factory.getTagName(), factory);
        }
    }

    public IntentFirewall(AMSInterface ams, Handler handler) {
        this.mAms = ams;
        this.mHandler = new FirewallHandler(handler.getLooper());
        File rulesDir = getRulesDir();
        rulesDir.mkdirs();
        readRulesDir(rulesDir);
        RuleObserver ruleObserver = new RuleObserver(rulesDir);
        this.mObserver = ruleObserver;
        ruleObserver.startWatching();
    }

    private PackageManagerInternal getPackageManager() {
        if (this.mPackageManager == null) {
            this.mPackageManager = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
        }
        return this.mPackageManager;
    }

    public boolean checkStartActivity(Intent intent, int callerUid, int callerPid, String resolvedType, ApplicationInfo resolvedApp) {
        return checkIntent(this.mActivityResolver, intent.getComponent(), 0, intent, callerUid, callerPid, resolvedType, resolvedApp.uid);
    }

    public boolean checkService(ComponentName resolvedService, Intent intent, int callerUid, int callerPid, String resolvedType, ApplicationInfo resolvedApp) {
        return checkIntent(this.mServiceResolver, resolvedService, 2, intent, callerUid, callerPid, resolvedType, resolvedApp.uid);
    }

    public boolean checkBroadcast(Intent intent, int callerUid, int callerPid, String resolvedType, int receivingUid) {
        return checkIntent(this.mBroadcastResolver, intent.getComponent(), 1, intent, callerUid, callerPid, resolvedType, receivingUid);
    }

    public boolean checkIntent(FirewallIntentResolver resolver, ComponentName resolvedComponent, int intentType, Intent intent, int callerUid, int callerPid, String resolvedType, int receivingUid) {
        boolean log = false;
        boolean block = false;
        List<Rule> candidateRules = resolver.queryIntent(getPackageManager().snapshot(), intent, resolvedType, false, 0);
        if (candidateRules == null) {
            candidateRules = new ArrayList();
        }
        resolver.queryByComponent(resolvedComponent, candidateRules);
        for (int i = 0; i < candidateRules.size(); i++) {
            Rule rule = candidateRules.get(i);
            if (rule.matches(this, resolvedComponent, intent, callerUid, callerPid, resolvedType, receivingUid)) {
                block |= rule.getBlock();
                log |= rule.getLog();
                if (block && log) {
                    break;
                }
            }
        }
        if (log) {
            logIntent(intentType, intent, callerUid, resolvedType);
        }
        return !block;
    }

    private static void logIntent(int intentType, Intent intent, int callerUid, String resolvedType) {
        String shortComponent;
        String callerPackages;
        int callerPackageCount;
        ComponentName cn = intent.getComponent();
        if (cn == null) {
            shortComponent = null;
        } else {
            String shortComponent2 = cn.flattenToShortString();
            shortComponent = shortComponent2;
        }
        String callerPackages2 = null;
        int callerPackageCount2 = 0;
        IPackageManager pm = AppGlobals.getPackageManager();
        if (pm == null) {
            callerPackages = null;
            callerPackageCount = 0;
        } else {
            try {
                String[] callerPackagesArray = pm.getPackagesForUid(callerUid);
                if (callerPackagesArray != null) {
                    callerPackageCount2 = callerPackagesArray.length;
                    callerPackages2 = joinPackages(callerPackagesArray);
                }
                callerPackages = callerPackages2;
                callerPackageCount = callerPackageCount2;
            } catch (RemoteException ex) {
                Slog.e(TAG, "Remote exception while retrieving packages", ex);
                callerPackages = null;
                callerPackageCount = callerPackageCount2;
            }
        }
        EventLogTags.writeIfwIntentMatched(intentType, shortComponent, callerUid, callerPackageCount, callerPackages, intent.getAction(), resolvedType, intent.getDataString(), intent.getFlags());
    }

    private static String joinPackages(String[] packages) {
        boolean first = true;
        StringBuilder sb = new StringBuilder();
        for (String pkg : packages) {
            if (sb.length() + pkg.length() + 1 < 150) {
                if (!first) {
                    sb.append(',');
                } else {
                    first = false;
                }
                sb.append(pkg);
            } else if (sb.length() >= 125) {
                return sb.toString();
            }
        }
        int i = sb.length();
        if (i == 0 && packages.length > 0) {
            String pkg2 = packages[0];
            return pkg2.substring((pkg2.length() - 150) + 1) + '-';
        }
        return null;
    }

    public static File getRulesDir() {
        return RULES_DIR;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void readRulesDir(File rulesDir) {
        FirewallIntentResolver[] resolvers = new FirewallIntentResolver[3];
        for (int i = 0; i < resolvers.length; i++) {
            resolvers[i] = new FirewallIntentResolver();
        }
        File[] files = rulesDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.getName().endsWith(".xml")) {
                    readRules(file, resolvers);
                }
            }
        }
        Slog.i(TAG, "Read new rules (A:" + resolvers[0].filterSet().size() + " B:" + resolvers[1].filterSet().size() + " S:" + resolvers[2].filterSet().size() + ")");
        synchronized (this.mAms.getAMSLock()) {
            this.mActivityResolver = resolvers[0];
            this.mBroadcastResolver = resolvers[1];
            this.mServiceResolver = resolvers[2];
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [379=4, 380=4, 381=4, 382=4] */
    private void readRules(File rulesFile, FirewallIntentResolver[] resolvers) {
        List<List<Rule>> rulesByType = new ArrayList<>(3);
        for (int i = 0; i < 3; i++) {
            rulesByType.add(new ArrayList<>());
        }
        try {
            FileInputStream fis = new FileInputStream(rulesFile);
            try {
                try {
                    try {
                        XmlPullParser parser = Xml.newPullParser();
                        parser.setInput(fis, null);
                        XmlUtils.beginDocument(parser, TAG_RULES);
                        int outerDepth = parser.getDepth();
                        while (XmlUtils.nextElementWithin(parser, outerDepth)) {
                            String tagName = parser.getName();
                            int ruleType = tagName.equals("activity") ? 0 : tagName.equals("broadcast") ? 1 : tagName.equals("service") ? 2 : -1;
                            if (ruleType != -1) {
                                Rule rule = new Rule();
                                List<Rule> rules = rulesByType.get(ruleType);
                                try {
                                    rule.readFromXml(parser);
                                    rules.add(rule);
                                } catch (XmlPullParserException ex) {
                                    Slog.e(TAG, "Error reading an intent firewall rule from " + rulesFile, ex);
                                }
                            }
                        }
                        try {
                            fis.close();
                        } catch (IOException ex2) {
                            Slog.e(TAG, "Error while closing " + rulesFile, ex2);
                        }
                        for (int ruleType2 = 0; ruleType2 < rulesByType.size(); ruleType2++) {
                            List<Rule> rules2 = rulesByType.get(ruleType2);
                            FirewallIntentResolver resolver = resolvers[ruleType2];
                            for (int ruleIndex = 0; ruleIndex < rules2.size(); ruleIndex++) {
                                Rule rule2 = rules2.get(ruleIndex);
                                for (int i2 = 0; i2 < rule2.getIntentFilterCount(); i2++) {
                                    resolver.addFilter(null, rule2.getIntentFilter(i2));
                                }
                                for (int i3 = 0; i3 < rule2.getComponentFilterCount(); i3++) {
                                    resolver.addComponentFilter(rule2.getComponentFilter(i3), rule2);
                                }
                            }
                        }
                    } catch (XmlPullParserException ex3) {
                        Slog.e(TAG, "Error reading intent firewall rules from " + rulesFile, ex3);
                        try {
                            fis.close();
                        } catch (IOException ex4) {
                            Slog.e(TAG, "Error while closing " + rulesFile, ex4);
                        }
                    }
                } catch (IOException ex5) {
                    Slog.e(TAG, "Error reading intent firewall rules from " + rulesFile, ex5);
                    try {
                        fis.close();
                    } catch (IOException ex6) {
                        Slog.e(TAG, "Error while closing " + rulesFile, ex6);
                    }
                }
            } catch (Throwable ex7) {
                try {
                    fis.close();
                } catch (IOException ex8) {
                    Slog.e(TAG, "Error while closing " + rulesFile, ex8);
                }
                throw ex7;
            }
        } catch (FileNotFoundException e) {
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static Filter parseFilter(XmlPullParser parser) throws IOException, XmlPullParserException {
        String elementName = parser.getName();
        FilterFactory factory = factoryMap.get(elementName);
        if (factory == null) {
            throw new XmlPullParserException("Unknown element in filter list: " + elementName);
        }
        return factory.newFilter(parser);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class Rule extends AndFilter {
        private static final String ATTR_BLOCK = "block";
        private static final String ATTR_LOG = "log";
        private static final String ATTR_NAME = "name";
        private static final String TAG_COMPONENT_FILTER = "component-filter";
        private static final String TAG_INTENT_FILTER = "intent-filter";
        private boolean block;
        private boolean log;
        private final ArrayList<ComponentName> mComponentFilters;
        private final ArrayList<FirewallIntentFilter> mIntentFilters;

        private Rule() {
            this.mIntentFilters = new ArrayList<>(1);
            this.mComponentFilters = new ArrayList<>(0);
        }

        /* JADX DEBUG: Method merged with bridge method */
        @Override // com.android.server.firewall.FilterList
        public Rule readFromXml(XmlPullParser parser) throws IOException, XmlPullParserException {
            this.block = Boolean.parseBoolean(parser.getAttributeValue(null, ATTR_BLOCK));
            this.log = Boolean.parseBoolean(parser.getAttributeValue(null, ATTR_LOG));
            super.readFromXml(parser);
            return this;
        }

        @Override // com.android.server.firewall.FilterList
        protected void readChild(XmlPullParser parser) throws IOException, XmlPullParserException {
            String currentTag = parser.getName();
            if (currentTag.equals(TAG_INTENT_FILTER)) {
                FirewallIntentFilter intentFilter = new FirewallIntentFilter(this);
                intentFilter.readFromXml(parser);
                this.mIntentFilters.add(intentFilter);
            } else if (currentTag.equals(TAG_COMPONENT_FILTER)) {
                String componentStr = parser.getAttributeValue(null, "name");
                if (componentStr == null) {
                    throw new XmlPullParserException("Component name must be specified.", parser, null);
                }
                ComponentName componentName = ComponentName.unflattenFromString(componentStr);
                if (componentName == null) {
                    throw new XmlPullParserException("Invalid component name: " + componentStr);
                }
                this.mComponentFilters.add(componentName);
            } else {
                super.readChild(parser);
            }
        }

        public int getIntentFilterCount() {
            return this.mIntentFilters.size();
        }

        public FirewallIntentFilter getIntentFilter(int index) {
            return this.mIntentFilters.get(index);
        }

        public int getComponentFilterCount() {
            return this.mComponentFilters.size();
        }

        public ComponentName getComponentFilter(int index) {
            return this.mComponentFilters.get(index);
        }

        public boolean getBlock() {
            return this.block;
        }

        public boolean getLog() {
            return this.log;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class FirewallIntentFilter extends IntentFilter {
        private final Rule rule;

        public FirewallIntentFilter(Rule rule) {
            this.rule = rule;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class FirewallIntentResolver extends IntentResolver<FirewallIntentFilter, Rule> {
        private final ArrayMap<ComponentName, Rule[]> mRulesByComponent;

        private FirewallIntentResolver() {
            this.mRulesByComponent = new ArrayMap<>(0);
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX INFO: Access modifiers changed from: protected */
        @Override // com.android.server.IntentResolver
        public boolean allowFilterResult(FirewallIntentFilter filter, List<Rule> dest) {
            return !dest.contains(filter.rule);
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX INFO: Access modifiers changed from: protected */
        @Override // com.android.server.IntentResolver
        public boolean isPackageForFilter(String packageName, FirewallIntentFilter filter) {
            return true;
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX INFO: Access modifiers changed from: protected */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // com.android.server.IntentResolver
        public FirewallIntentFilter[] newArray(int size) {
            return new FirewallIntentFilter[size];
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX INFO: Access modifiers changed from: protected */
        @Override // com.android.server.IntentResolver
        public Rule newResult(Computer computer, FirewallIntentFilter filter, int match, int userId, long customFlags) {
            return filter.rule;
        }

        @Override // com.android.server.IntentResolver
        protected void sortResults(List<Rule> results) {
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX INFO: Access modifiers changed from: protected */
        @Override // com.android.server.IntentResolver
        public IntentFilter getIntentFilter(FirewallIntentFilter input) {
            return input;
        }

        public void queryByComponent(ComponentName componentName, List<Rule> candidateRules) {
            Rule[] rules = this.mRulesByComponent.get(componentName);
            if (rules != null) {
                candidateRules.addAll(Arrays.asList(rules));
            }
        }

        public void addComponentFilter(ComponentName componentName, Rule rule) {
            Rule[] rules = this.mRulesByComponent.get(componentName);
            this.mRulesByComponent.put(componentName, (Rule[]) ArrayUtils.appendElement(Rule.class, rules, rule));
        }
    }

    /* loaded from: classes.dex */
    private final class FirewallHandler extends Handler {
        public FirewallHandler(Looper looper) {
            super(looper, null, true);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            IntentFirewall.this.readRulesDir(IntentFirewall.getRulesDir());
        }
    }

    /* loaded from: classes.dex */
    private class RuleObserver extends FileObserver {
        private static final int MONITORED_EVENTS = 968;

        public RuleObserver(File monitoredDir) {
            super(monitoredDir.getAbsolutePath(), (int) MONITORED_EVENTS);
        }

        @Override // android.os.FileObserver
        public void onEvent(int event, String path) {
            if (path != null && path.endsWith(".xml")) {
                IntentFirewall.this.mHandler.removeMessages(0);
                IntentFirewall.this.mHandler.sendEmptyMessageDelayed(0, 250L);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean checkComponentPermission(String permission, int pid, int uid, int owningUid, boolean exported) {
        return this.mAms.checkComponentPermission(permission, pid, uid, owningUid, exported) == 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean signaturesMatch(int uid1, int uid2) {
        try {
            IPackageManager pm = AppGlobals.getPackageManager();
            return pm.checkUidSignatures(uid1, uid2) == 0;
        } catch (RemoteException ex) {
            Slog.e(TAG, "Remote exception while checking signatures", ex);
            return false;
        }
    }
}
