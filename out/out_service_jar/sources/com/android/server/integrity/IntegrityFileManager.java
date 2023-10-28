package com.android.server.integrity;

import android.content.integrity.AppInstallMetadata;
import android.content.integrity.Rule;
import android.os.Environment;
import android.util.Slog;
import com.android.server.integrity.model.RuleMetadata;
import com.android.server.integrity.parser.RandomAccessObject;
import com.android.server.integrity.parser.RuleBinaryParser;
import com.android.server.integrity.parser.RuleIndexRange;
import com.android.server.integrity.parser.RuleIndexingController;
import com.android.server.integrity.parser.RuleMetadataParser;
import com.android.server.integrity.parser.RuleParseException;
import com.android.server.integrity.parser.RuleParser;
import com.android.server.integrity.serializer.RuleBinarySerializer;
import com.android.server.integrity.serializer.RuleMetadataSerializer;
import com.android.server.integrity.serializer.RuleSerializeException;
import com.android.server.integrity.serializer.RuleSerializer;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
/* loaded from: classes.dex */
public class IntegrityFileManager {
    private static final String INDEXING_FILE = "indexing";
    private static final String METADATA_FILE = "metadata";
    private static final String RULES_FILE = "rules";
    private static final String TAG = "IntegrityFileManager";
    private final File mDataDir;
    private RuleIndexingController mRuleIndexingController;
    private RuleMetadata mRuleMetadataCache;
    private final RuleParser mRuleParser;
    private final RuleSerializer mRuleSerializer;
    private final File mRulesDir;
    private final File mStagingDir;
    private static final Object RULES_LOCK = new Object();
    private static IntegrityFileManager sInstance = null;

    public static synchronized IntegrityFileManager getInstance() {
        IntegrityFileManager integrityFileManager;
        synchronized (IntegrityFileManager.class) {
            if (sInstance == null) {
                sInstance = new IntegrityFileManager();
            }
            integrityFileManager = sInstance;
        }
        return integrityFileManager;
    }

    private IntegrityFileManager() {
        this(new RuleBinaryParser(), new RuleBinarySerializer(), Environment.getDataSystemDirectory());
    }

    IntegrityFileManager(RuleParser ruleParser, RuleSerializer ruleSerializer, File dataDir) {
        this.mRuleParser = ruleParser;
        this.mRuleSerializer = ruleSerializer;
        this.mDataDir = dataDir;
        File file = new File(dataDir, "integrity_rules");
        this.mRulesDir = file;
        File file2 = new File(dataDir, "integrity_staging");
        this.mStagingDir = file2;
        if (!file2.mkdirs() || !file.mkdirs()) {
            Slog.e(TAG, "Error creating staging and rules directory");
        }
        File metadataFile = new File(file, METADATA_FILE);
        if (metadataFile.exists()) {
            try {
                FileInputStream inputStream = new FileInputStream(metadataFile);
                this.mRuleMetadataCache = RuleMetadataParser.parse(inputStream);
                inputStream.close();
            } catch (Exception e) {
                Slog.e(TAG, "Error reading metadata file.", e);
            }
        }
        updateRuleIndexingController();
    }

    public boolean initialized() {
        return new File(this.mRulesDir, RULES_FILE).exists() && new File(this.mRulesDir, METADATA_FILE).exists() && new File(this.mRulesDir, INDEXING_FILE).exists();
    }

    public void writeRules(String version, String ruleProvider, List<Rule> rules) throws IOException, RuleSerializeException {
        try {
            writeMetadata(this.mStagingDir, ruleProvider, version);
        } catch (IOException e) {
            Slog.e(TAG, "Error writing metadata.", e);
        }
        FileOutputStream ruleFileOutputStream = new FileOutputStream(new File(this.mStagingDir, RULES_FILE));
        try {
            FileOutputStream indexingFileOutputStream = new FileOutputStream(new File(this.mStagingDir, INDEXING_FILE));
            this.mRuleSerializer.serialize(rules, Optional.empty(), ruleFileOutputStream, indexingFileOutputStream);
            indexingFileOutputStream.close();
            ruleFileOutputStream.close();
            switchStagingRulesDir();
            updateRuleIndexingController();
        } catch (Throwable th) {
            try {
                ruleFileOutputStream.close();
            } catch (Throwable th2) {
                th.addSuppressed(th2);
            }
            throw th;
        }
    }

    public List<Rule> readRules(AppInstallMetadata appInstallMetadata) throws IOException, RuleParseException {
        List<Rule> rules;
        synchronized (RULES_LOCK) {
            List<RuleIndexRange> ruleReadingIndexes = Collections.emptyList();
            if (appInstallMetadata != null) {
                try {
                    ruleReadingIndexes = this.mRuleIndexingController.identifyRulesToEvaluate(appInstallMetadata);
                } catch (Exception | StackOverflowError e) {
                    Slog.w(TAG, "Error identifying the rule indexes. Trying unindexed.", e);
                }
            }
            File ruleFile = new File(this.mRulesDir, RULES_FILE);
            rules = this.mRuleParser.parse(RandomAccessObject.ofFile(ruleFile), ruleReadingIndexes);
        }
        return rules;
    }

    public RuleMetadata readMetadata() {
        return this.mRuleMetadataCache;
    }

    private void switchStagingRulesDir() throws IOException {
        File[] listFiles;
        synchronized (RULES_LOCK) {
            File tmpDir = new File(this.mDataDir, "temp");
            if (!this.mRulesDir.renameTo(tmpDir) || !this.mStagingDir.renameTo(this.mRulesDir) || !tmpDir.renameTo(this.mStagingDir)) {
                throw new IOException("Error switching staging/rules directory");
            }
            for (File file : this.mStagingDir.listFiles()) {
                file.delete();
            }
        }
    }

    private void updateRuleIndexingController() {
        File ruleIndexingFile = new File(this.mRulesDir, INDEXING_FILE);
        if (ruleIndexingFile.exists()) {
            try {
                FileInputStream inputStream = new FileInputStream(ruleIndexingFile);
                this.mRuleIndexingController = new RuleIndexingController(inputStream);
                inputStream.close();
            } catch (Exception e) {
                Slog.e(TAG, "Error parsing the rule indexing file.", e);
            }
        }
    }

    private void writeMetadata(File directory, String ruleProvider, String version) throws IOException {
        this.mRuleMetadataCache = new RuleMetadata(ruleProvider, version);
        File metadataFile = new File(directory, METADATA_FILE);
        FileOutputStream outputStream = new FileOutputStream(metadataFile);
        try {
            RuleMetadataSerializer.serialize(this.mRuleMetadataCache, outputStream);
            outputStream.close();
        } catch (Throwable th) {
            try {
                outputStream.close();
            } catch (Throwable th2) {
                th.addSuppressed(th2);
            }
            throw th;
        }
    }
}
