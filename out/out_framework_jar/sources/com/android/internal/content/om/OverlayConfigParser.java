package com.android.internal.content.om;

import android.content.pm.PackagePartitions;
import android.os.FileUtils;
import android.util.ArraySet;
import android.util.Log;
import android.util.Xml;
import com.android.internal.content.om.OverlayScanner;
import com.android.internal.util.Preconditions;
import com.android.internal.util.XmlUtils;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import libcore.io.IoUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes4.dex */
public final class OverlayConfigParser {
    private static final String CONFIG_DEFAULT_FILENAME = "config/config.xml";
    private static final String CONFIG_DIRECTORY = "config";
    static final boolean DEFAULT_ENABLED_STATE = false;
    static final boolean DEFAULT_MUTABILITY = true;
    private static final int MAXIMUM_MERGE_DEPTH = 5;

    OverlayConfigParser() {
    }

    /* loaded from: classes4.dex */
    public static class ParsedConfiguration {
        public final boolean enabled;
        public final boolean mutable;
        public final String packageName;
        public final OverlayScanner.ParsedOverlayInfo parsedInfo;
        public final String policy;

        /* JADX INFO: Access modifiers changed from: package-private */
        public ParsedConfiguration(String packageName, boolean enabled, boolean mutable, String policy, OverlayScanner.ParsedOverlayInfo parsedInfo) {
            this.packageName = packageName;
            this.enabled = enabled;
            this.mutable = mutable;
            this.policy = policy;
            this.parsedInfo = parsedInfo;
        }

        public String toString() {
            return getClass().getSimpleName() + String.format("{packageName=%s, enabled=%s, mutable=%s, policy=%s, parsedInfo=%s}", this.packageName, Boolean.valueOf(this.enabled), Boolean.valueOf(this.mutable), this.policy, this.parsedInfo);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes4.dex */
    public static class OverlayPartition extends PackagePartitions.SystemPartition {
        static final String POLICY_ODM = "odm";
        static final String POLICY_OEM = "oem";
        static final String POLICY_PRODUCT = "product";
        static final String POLICY_PUBLIC = "public";
        static final String POLICY_SYSTEM = "system";
        static final String POLICY_VENDOR = "vendor";
        public final String policy;

        /* JADX INFO: Access modifiers changed from: package-private */
        public OverlayPartition(PackagePartitions.SystemPartition partition) {
            super(partition);
            this.policy = policyForPartition(partition);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public OverlayPartition(File folder, PackagePartitions.SystemPartition original) {
            super(folder, original);
            this.policy = policyForPartition(original);
        }

        private static String policyForPartition(PackagePartitions.SystemPartition partition) {
            switch (partition.type) {
                case 0:
                case 5:
                case 6:
                case 7:
                case 8:
                case 9:
                case 10:
                case 11:
                case 12:
                    return "system";
                case 1:
                    return "vendor";
                case 2:
                    return "odm";
                case 3:
                    return "oem";
                case 4:
                    return "product";
                default:
                    throw new IllegalStateException("Unable to determine policy for " + partition.getFolder());
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes4.dex */
    public static class ParsingContext {
        private final ArraySet<String> mConfiguredOverlays;
        private boolean mFoundMutableOverlay;
        private int mMergeDepth;
        private final ArrayList<ParsedConfiguration> mOrderedConfigurations;
        private final OverlayPartition mPartition;

        private ParsingContext(OverlayPartition partition) {
            this.mOrderedConfigurations = new ArrayList<>();
            this.mConfiguredOverlays = new ArraySet<>();
            this.mPartition = partition;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static ArrayList<ParsedConfiguration> getConfigurations(OverlayPartition partition, OverlayScanner scanner, Map<String, OverlayScanner.ParsedOverlayInfo> packageManagerOverlayInfos, List<String> activeApexes) {
        if (scanner != null) {
            if (partition.getOverlayFolder() != null) {
                scanner.scanDir(partition.getOverlayFolder());
            }
            for (String apex : activeApexes) {
                scanner.scanDir(new File("/apex/" + apex + "/overlay/"));
            }
        }
        if (partition.getOverlayFolder() == null) {
            return null;
        }
        File configFile = new File(partition.getOverlayFolder(), CONFIG_DEFAULT_FILENAME);
        if (configFile.exists()) {
            ParsingContext parsingContext = new ParsingContext(partition);
            readConfigFile(configFile, scanner, packageManagerOverlayInfos, parsingContext);
            return parsingContext.mOrderedConfigurations;
        }
        return null;
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    private static void readConfigFile(File configFile, OverlayScanner scanner, Map<String, OverlayScanner.ParsedOverlayInfo> packageManagerOverlayInfos, ParsingContext parsingContext) {
        try {
            FileReader configReader = new FileReader(configFile);
            try {
                try {
                    XmlPullParser parser = Xml.newPullParser();
                    parser.setInput(configReader);
                    XmlUtils.beginDocument(parser, CONFIG_DIRECTORY);
                    int depth = parser.getDepth();
                    while (XmlUtils.nextElementWithin(parser, depth)) {
                        String name = parser.getName();
                        char c = 65535;
                        switch (name.hashCode()) {
                            case -1091287984:
                                if (name.equals("overlay")) {
                                    c = 1;
                                    break;
                                }
                                break;
                            case 103785528:
                                if (name.equals("merge")) {
                                    c = 0;
                                    break;
                                }
                                break;
                        }
                        switch (c) {
                            case 0:
                                parseMerge(configFile, parser, scanner, packageManagerOverlayInfos, parsingContext);
                                break;
                            case 1:
                                parseOverlay(configFile, parser, scanner, packageManagerOverlayInfos, parsingContext);
                                break;
                            default:
                                Log.w("OverlayConfig", String.format("Tag %s is unknown in %s at %s", name, configFile, parser.getPositionDescription()));
                                break;
                        }
                    }
                } catch (IOException | XmlPullParserException e) {
                    Log.w("OverlayConfig", "Got exception parsing overlay configuration.", e);
                }
            } finally {
                IoUtils.closeQuietly(configReader);
            }
        } catch (FileNotFoundException e2) {
            Log.w("OverlayConfig", "Couldn't find or open overlay configuration file " + configFile);
        }
    }

    private static void parseMerge(File configFile, XmlPullParser parser, OverlayScanner scanner, Map<String, OverlayScanner.ParsedOverlayInfo> packageManagerOverlayInfos, ParsingContext parsingContext) {
        String path = parser.getAttributeValue(null, "path");
        if (path == null) {
            throw new IllegalStateException(String.format("<merge> without path in %s at %s" + configFile, parser.getPositionDescription()));
        }
        if (path.startsWith("/")) {
            throw new IllegalStateException(String.format("Path %s must be relative to the directory containing overlay configurations  files in %s at %s ", path, configFile, parser.getPositionDescription()));
        }
        int i = parsingContext.mMergeDepth;
        parsingContext.mMergeDepth = i + 1;
        if (i == 5) {
            throw new IllegalStateException(String.format("Maximum <merge> depth exceeded in %s at %s", configFile, parser.getPositionDescription()));
        }
        try {
            File configDirectory = new File(parsingContext.mPartition.getOverlayFolder(), CONFIG_DIRECTORY).getCanonicalFile();
            File includedConfigFile = new File(configDirectory, path).getCanonicalFile();
            if (!includedConfigFile.exists()) {
                throw new IllegalStateException(String.format("Merged configuration file %s does not exist in %s at %s", path, configFile, parser.getPositionDescription()));
            }
            if (!FileUtils.contains(configDirectory, includedConfigFile)) {
                throw new IllegalStateException(String.format("Merged file %s outside of configuration directory in %s at %s", includedConfigFile.getAbsolutePath(), includedConfigFile, parser.getPositionDescription()));
            }
            readConfigFile(includedConfigFile, scanner, packageManagerOverlayInfos, parsingContext);
            parsingContext.mMergeDepth--;
        } catch (IOException e) {
            throw new IllegalStateException(String.format("Couldn't find or open merged configuration file %s in %s at %s", path, configFile, parser.getPositionDescription()), e);
        }
    }

    private static void parseOverlay(File configFile, XmlPullParser parser, OverlayScanner scanner, Map<String, OverlayScanner.ParsedOverlayInfo> packageManagerOverlayInfos, ParsingContext parsingContext) {
        OverlayScanner.ParsedOverlayInfo info;
        boolean isEnabled;
        boolean isMutable;
        Preconditions.checkArgument((scanner == null) != (packageManagerOverlayInfos == null), "scanner and packageManagerOverlayInfos cannot be both null or both non-null");
        String packageName = parser.getAttributeValue(null, "package");
        if (packageName == null) {
            throw new IllegalStateException(String.format("\"<overlay> without package in %s at %s", configFile, parser.getPositionDescription()));
        }
        if (scanner != null) {
            OverlayScanner.ParsedOverlayInfo info2 = scanner.getParsedInfo(packageName);
            if (info2 == null && scanner.isExcludedOverlayPackage(packageName, parsingContext.mPartition)) {
                Log.d("OverlayConfig", "overlay " + packageName + " in partition " + parsingContext.mPartition.getOverlayFolder() + " is ignored.");
                return;
            } else if (info2 == null || !parsingContext.mPartition.containsOverlay(info2.path)) {
                throw new IllegalStateException(String.format("overlay %s not present in partition %s in %s at %s", packageName, parsingContext.mPartition.getOverlayFolder(), configFile, parser.getPositionDescription()));
            } else {
                info = info2;
            }
        } else if (packageManagerOverlayInfos.get(packageName) != null) {
            info = null;
        } else {
            Log.d("OverlayConfig", "overlay " + packageName + " in partition " + parsingContext.mPartition.getOverlayFolder() + " is ignored.");
            return;
        }
        if (!parsingContext.mConfiguredOverlays.contains(packageName)) {
            String enabled = parser.getAttributeValue(null, "enabled");
            if (enabled == null) {
                isEnabled = false;
            } else {
                boolean isEnabled2 = !"false".equals(enabled);
                isEnabled = isEnabled2;
            }
            String mutable = parser.getAttributeValue(null, "mutable");
            if (mutable == null) {
                isMutable = true;
            } else {
                boolean isMutable2 = !"false".equals(mutable);
                if (!isMutable2 && parsingContext.mFoundMutableOverlay) {
                    throw new IllegalStateException(String.format("immutable overlays must precede mutable overlays: found in %s at %s", configFile, parser.getPositionDescription()));
                }
                isMutable = isMutable2;
            }
            if (isMutable) {
                parsingContext.mFoundMutableOverlay = true;
            } else if (!isEnabled) {
                Log.w("OverlayConfig", "found default-disabled immutable overlay " + packageName);
            }
            ParsedConfiguration Config = new ParsedConfiguration(packageName, isEnabled, isMutable, parsingContext.mPartition.policy, info);
            parsingContext.mConfiguredOverlays.add(packageName);
            parsingContext.mOrderedConfigurations.add(Config);
            return;
        }
        throw new IllegalStateException(String.format("overlay %s configured multiple times in a single partition in %s at %s", packageName, configFile, parser.getPositionDescription()));
    }
}
