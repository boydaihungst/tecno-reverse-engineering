package com.android.server.graphics.fonts;

import android.graphics.fonts.FontUpdateRequest;
import android.graphics.fonts.SystemFonts;
import android.os.FileUtils;
import android.os.LocaleList;
import android.system.ErrnoException;
import android.system.Os;
import android.text.FontConfig;
import android.util.ArrayMap;
import android.util.AtomicFile;
import android.util.Base64;
import android.util.Slog;
import com.android.internal.art.ArtStatsLog;
import com.android.internal.util.FrameworkStatsLog;
import com.android.server.graphics.fonts.FontManagerService;
import com.android.server.graphics.fonts.PersistentSystemFontConfig;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import org.xmlpull.v1.XmlPullParserException;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public final class UpdatableFontDir {
    private static final String RANDOM_DIR_PREFIX = "~~";
    private static final String TAG = "UpdatableFontDir";
    private final AtomicFile mConfigFile;
    private final Function<Map<String, File>, FontConfig> mConfigSupplier;
    private int mConfigVersion;
    private final Supplier<Long> mCurrentTimeSupplier;
    private final File mFilesDir;
    private final ArrayMap<String, FontFileInfo> mFontFileInfoMap;
    private final FsverityUtil mFsverityUtil;
    private long mLastModifiedMillis;
    private final FontFileParser mParser;

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public interface FontFileParser {
        String buildFontFileName(File file) throws IOException;

        String getPostScriptName(File file) throws IOException;

        long getRevision(File file) throws IOException;

        void tryToCreateTypeface(File file) throws Throwable;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public interface FsverityUtil {
        boolean hasFsverity(String str);

        boolean rename(File file, File file2);

        void setUpFsverity(String str, byte[] bArr) throws IOException;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static final class FontFileInfo {
        private final File mFile;
        private final String mPsName;
        private final long mRevision;

        FontFileInfo(File file, String psName, long revision) {
            this.mFile = file;
            this.mPsName = psName;
            this.mRevision = revision;
        }

        public File getFile() {
            return this.mFile;
        }

        public String getPostScriptName() {
            return this.mPsName;
        }

        public File getRandomizedFontDir() {
            return this.mFile.getParentFile();
        }

        public long getRevision() {
            return this.mRevision;
        }

        public String toString() {
            return "FontFileInfo{mFile=" + this.mFile + ", psName=" + this.mPsName + ", mRevision=" + this.mRevision + '}';
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public UpdatableFontDir(File filesDir, FontFileParser parser, FsverityUtil fsverityUtil, File configFile) {
        this(filesDir, parser, fsverityUtil, configFile, new Supplier() { // from class: com.android.server.graphics.fonts.UpdatableFontDir$$ExternalSyntheticLambda0
            @Override // java.util.function.Supplier
            public final Object get() {
                return Long.valueOf(System.currentTimeMillis());
            }
        }, new Function() { // from class: com.android.server.graphics.fonts.UpdatableFontDir$$ExternalSyntheticLambda1
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                FontConfig systemFontConfig;
                systemFontConfig = SystemFonts.getSystemFontConfig((Map) obj, 0L, 0);
                return systemFontConfig;
            }
        });
    }

    UpdatableFontDir(File filesDir, FontFileParser parser, FsverityUtil fsverityUtil, File configFile, Supplier<Long> currentTimeSupplier, Function<Map<String, File>, FontConfig> configSupplier) {
        this.mFontFileInfoMap = new ArrayMap<>();
        this.mFilesDir = filesDir;
        this.mParser = parser;
        this.mFsverityUtil = fsverityUtil;
        this.mConfigFile = new AtomicFile(configFile);
        this.mCurrentTimeSupplier = currentTimeSupplier;
        this.mConfigSupplier = configSupplier;
    }

    /* JADX DEBUG: Another duplicated slice has different insns count: {[IF]}, finally: {[IF, IGET, INVOKE, IPUT, IGET, INVOKE] complete} */
    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [209=5, 210=6, 211=5, 212=5] */
    /* JADX INFO: Access modifiers changed from: package-private */
    public void loadFontFileMap() {
        PersistentSystemFontConfig.Config config;
        File[] dirs;
        this.mFontFileInfoMap.clear();
        this.mLastModifiedMillis = 0L;
        this.mConfigVersion = 1;
        try {
            config = readPersistentConfig();
            this.mLastModifiedMillis = config.lastModifiedMillis;
            dirs = this.mFilesDir.listFiles();
        } catch (Throwable t) {
            try {
                Slog.e(TAG, "Failed to load font mappings.", t);
                if (0 != 0) {
                    return;
                }
            } finally {
                if (0 == 0) {
                    this.mFontFileInfoMap.clear();
                    this.mLastModifiedMillis = 0L;
                    FileUtils.deleteContents(this.mFilesDir);
                }
            }
        }
        if (dirs == null) {
            Slog.e(TAG, "Could not read: " + this.mFilesDir);
            if (success) {
                return;
            }
            return;
        }
        FontConfig fontConfig = null;
        for (File dir : dirs) {
            if (!dir.getName().startsWith(RANDOM_DIR_PREFIX)) {
                Slog.e(TAG, "Unexpected dir found: " + dir);
                if (0 == 0) {
                    this.mFontFileInfoMap.clear();
                    this.mLastModifiedMillis = 0L;
                    FileUtils.deleteContents(this.mFilesDir);
                    return;
                }
                return;
            }
            if (config.updatedFontDirs.contains(dir.getName())) {
                File[] files = dir.listFiles();
                if (files != null && files.length == 1) {
                    FontFileInfo fontFileInfo = validateFontFile(files[0]);
                    if (fontConfig == null) {
                        fontConfig = getSystemFontConfig();
                    }
                    addFileToMapIfSameOrNewer(fontFileInfo, fontConfig, true);
                }
                Slog.e(TAG, "Unexpected files in dir: " + dir);
                if (0 == 0) {
                    this.mFontFileInfoMap.clear();
                    this.mLastModifiedMillis = 0L;
                    FileUtils.deleteContents(this.mFilesDir);
                    return;
                }
                return;
            }
            Slog.i(TAG, "Deleting obsolete dir: " + dir);
            FileUtils.deleteContentsAndDir(dir);
        }
        if (1 != 0) {
            return;
        }
        this.mFontFileInfoMap.clear();
        this.mLastModifiedMillis = 0L;
        FileUtils.deleteContents(this.mFilesDir);
    }

    public void update(List<FontUpdateRequest> requests) throws FontManagerService.SystemFontException {
        for (FontUpdateRequest request : requests) {
            switch (request.getType()) {
                case 0:
                    Objects.requireNonNull(request.getFd());
                    Objects.requireNonNull(request.getSignature());
                    break;
                case 1:
                    Objects.requireNonNull(request.getFontFamily());
                    Objects.requireNonNull(request.getFontFamily().getName());
                    break;
            }
        }
        ArrayMap<String, FontFileInfo> backupMap = new ArrayMap<>(this.mFontFileInfoMap);
        PersistentSystemFontConfig.Config curConfig = readPersistentConfig();
        Map<String, FontUpdateRequest.Family> familyMap = new HashMap<>();
        for (int i = 0; i < curConfig.fontFamilies.size(); i++) {
            FontUpdateRequest.Family family = curConfig.fontFamilies.get(i);
            familyMap.put(family.getName(), family);
        }
        long backupLastModifiedDate = this.mLastModifiedMillis;
        boolean success = false;
        try {
            for (FontUpdateRequest request2 : requests) {
                switch (request2.getType()) {
                    case 0:
                        installFontFile(request2.getFd().getFileDescriptor(), request2.getSignature());
                        break;
                    case 1:
                        FontUpdateRequest.Family family2 = request2.getFontFamily();
                        familyMap.put(family2.getName(), family2);
                        break;
                }
            }
            for (FontUpdateRequest.Family family3 : familyMap.values()) {
                if (resolveFontFiles(family3) == null) {
                    throw new FontManagerService.SystemFontException(-9, "Required fonts are not available");
                }
            }
            this.mLastModifiedMillis = this.mCurrentTimeSupplier.get().longValue();
            PersistentSystemFontConfig.Config newConfig = new PersistentSystemFontConfig.Config();
            newConfig.lastModifiedMillis = this.mLastModifiedMillis;
            for (FontFileInfo info : this.mFontFileInfoMap.values()) {
                newConfig.updatedFontDirs.add(info.getRandomizedFontDir().getName());
            }
            newConfig.fontFamilies.addAll(familyMap.values());
            writePersistentConfig(newConfig);
            this.mConfigVersion++;
            success = true;
        } finally {
            if (!success) {
                this.mFontFileInfoMap.clear();
                this.mFontFileInfoMap.putAll((ArrayMap<? extends String, ? extends FontFileInfo>) backupMap);
                this.mLastModifiedMillis = backupLastModifiedDate;
            }
        }
    }

    private void installFontFile(FileDescriptor fd, byte[] pkcs7Signature) throws FontManagerService.SystemFontException {
        File newDir = getRandomDir(this.mFilesDir);
        if (!newDir.mkdir()) {
            throw new FontManagerService.SystemFontException(-1, "Failed to create font directory.");
        }
        try {
            Os.chmod(newDir.getAbsolutePath(), ArtStatsLog.ISOLATED_COMPILATION_SCHEDULED);
            boolean success = false;
            try {
                File tempNewFontFile = new File(newDir, "font.ttf");
                try {
                    FileOutputStream out = new FileOutputStream(tempNewFontFile);
                    try {
                        FileUtils.copy(fd, out.getFD());
                        out.close();
                        try {
                            this.mFsverityUtil.setUpFsverity(tempNewFontFile.getAbsolutePath(), pkcs7Signature);
                            try {
                                String fontFileName = this.mParser.buildFontFileName(tempNewFontFile);
                                if (fontFileName == null) {
                                    throw new FontManagerService.SystemFontException(-4, "Failed to read PostScript name from font file");
                                }
                                File newFontFile = new File(newDir, fontFileName);
                                if (!this.mFsverityUtil.rename(tempNewFontFile, newFontFile)) {
                                    throw new FontManagerService.SystemFontException(-1, "Failed to move verified font file.");
                                }
                                try {
                                    Os.chmod(newFontFile.getAbsolutePath(), FrameworkStatsLog.VBMETA_DIGEST_REPORTED);
                                    FontFileInfo fontFileInfo = validateFontFile(newFontFile);
                                    this.mParser.tryToCreateTypeface(fontFileInfo.getFile());
                                    FontConfig fontConfig = getSystemFontConfig();
                                    if (!addFileToMapIfSameOrNewer(fontFileInfo, fontConfig, false)) {
                                        throw new FontManagerService.SystemFontException(-5, "Downgrading font file is forbidden.");
                                    }
                                    success = true;
                                } catch (ErrnoException e) {
                                    throw new FontManagerService.SystemFontException(-1, "Failed to change mode to 711", e);
                                }
                            } catch (IOException e2) {
                                throw new FontManagerService.SystemFontException(-3, "Failed to read PostScript name from font file", e2);
                            }
                        } catch (IOException e3) {
                            throw new FontManagerService.SystemFontException(-2, "Failed to setup fs-verity.", e3);
                        }
                    } catch (Throwable e4) {
                        try {
                            out.close();
                        } catch (Throwable th) {
                            e4.addSuppressed(th);
                        }
                        throw e4;
                    }
                } catch (IOException e5) {
                    throw new FontManagerService.SystemFontException(-1, "Failed to write font file to storage.", e5);
                }
            } finally {
                if (!success) {
                    FileUtils.deleteContentsAndDir(newDir);
                }
            }
        } catch (ErrnoException e6) {
            throw new FontManagerService.SystemFontException(-1, "Failed to change mode to 711", e6);
        }
    }

    private static File getRandomDir(File parent) {
        File dir;
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[16];
        do {
            random.nextBytes(bytes);
            String dirName = RANDOM_DIR_PREFIX + Base64.encodeToString(bytes, 10);
            dir = new File(parent, dirName);
        } while (dir.exists());
        return dir;
    }

    private FontFileInfo lookupFontFileInfo(String psName) {
        return this.mFontFileInfoMap.get(psName);
    }

    private void putFontFileInfo(FontFileInfo info) {
        this.mFontFileInfoMap.put(info.getPostScriptName(), info);
    }

    private boolean addFileToMapIfSameOrNewer(FontFileInfo fontFileInfo, FontConfig fontConfig, boolean deleteOldFile) {
        FontFileInfo existingInfo = lookupFontFileInfo(fontFileInfo.getPostScriptName());
        boolean shouldAddToMap = true;
        if (existingInfo == null) {
            long preInstalledRev = getPreinstalledFontRevision(fontFileInfo, fontConfig);
            if (preInstalledRev > fontFileInfo.getRevision()) {
                shouldAddToMap = false;
            }
        } else if (existingInfo.getRevision() > fontFileInfo.getRevision()) {
            shouldAddToMap = false;
        }
        if (shouldAddToMap) {
            if (deleteOldFile && existingInfo != null) {
                FileUtils.deleteContentsAndDir(existingInfo.getRandomizedFontDir());
            }
            putFontFileInfo(fontFileInfo);
        } else if (deleteOldFile) {
            FileUtils.deleteContentsAndDir(fontFileInfo.getRandomizedFontDir());
        }
        return shouldAddToMap;
    }

    private long getPreinstalledFontRevision(FontFileInfo info, FontConfig fontConfig) {
        String psName = info.getPostScriptName();
        FontConfig.Font targetFont = null;
        for (int i = 0; i < fontConfig.getFontFamilies().size(); i++) {
            FontConfig.FontFamily family = (FontConfig.FontFamily) fontConfig.getFontFamilies().get(i);
            int j = 0;
            while (true) {
                if (j < family.getFontList().size()) {
                    FontConfig.Font font = (FontConfig.Font) family.getFontList().get(j);
                    if (!font.getPostScriptName().equals(psName)) {
                        j++;
                    } else {
                        targetFont = font;
                        break;
                    }
                }
            }
        }
        if (targetFont == null) {
            return -1L;
        }
        File preinstalledFontFile = targetFont.getOriginalFile() != null ? targetFont.getOriginalFile() : targetFont.getFile();
        if (!preinstalledFontFile.exists()) {
            return -1L;
        }
        long revision = getFontRevision(preinstalledFontFile);
        if (revision == -1) {
            Slog.w(TAG, "Invalid preinstalled font file");
        }
        return revision;
    }

    private FontFileInfo validateFontFile(File file) throws FontManagerService.SystemFontException {
        if (!this.mFsverityUtil.hasFsverity(file.getAbsolutePath())) {
            throw new FontManagerService.SystemFontException(-2, "Font validation failed. Fs-verity is not enabled: " + file);
        }
        try {
            String psName = this.mParser.getPostScriptName(file);
            long revision = getFontRevision(file);
            if (revision == -1) {
                throw new FontManagerService.SystemFontException(-3, "Font validation failed. Could not read font revision: " + file);
            }
            return new FontFileInfo(file, psName, revision);
        } catch (IOException e) {
            throw new FontManagerService.SystemFontException(-4, "Font validation failed. Could not read PostScript name name: " + file);
        }
    }

    private long getFontRevision(File file) {
        try {
            return this.mParser.getRevision(file);
        } catch (IOException e) {
            Slog.e(TAG, "Failed to read font file", e);
            return -1L;
        }
    }

    private FontConfig.FontFamily resolveFontFiles(FontUpdateRequest.Family fontFamily) {
        List<FontUpdateRequest.Font> fontList = fontFamily.getFonts();
        List<FontConfig.Font> resolvedFonts = new ArrayList<>(fontList.size());
        for (int i = 0; i < fontList.size(); i++) {
            FontUpdateRequest.Font font = fontList.get(i);
            FontFileInfo info = this.mFontFileInfoMap.get(font.getPostScriptName());
            if (info == null) {
                Slog.e(TAG, "Failed to lookup font file that has " + font.getPostScriptName());
                return null;
            }
            resolvedFonts.add(new FontConfig.Font(info.mFile, (File) null, info.getPostScriptName(), font.getFontStyle(), font.getIndex(), font.getFontVariationSettings(), (String) null));
        }
        return new FontConfig.FontFamily(resolvedFonts, fontFamily.getName(), LocaleList.getEmptyLocaleList(), 0);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Map<String, File> getPostScriptMap() {
        Map<String, File> map = new ArrayMap<>();
        for (int i = 0; i < this.mFontFileInfoMap.size(); i++) {
            FontFileInfo info = this.mFontFileInfoMap.valueAt(i);
            map.put(info.getPostScriptName(), info.getFile());
        }
        return map;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public FontConfig getSystemFontConfig() {
        FontConfig config = this.mConfigSupplier.apply(getPostScriptMap());
        PersistentSystemFontConfig.Config persistentConfig = readPersistentConfig();
        List<FontUpdateRequest.Family> families = persistentConfig.fontFamilies;
        List<FontConfig.FontFamily> mergedFamilies = new ArrayList<>(config.getFontFamilies().size() + families.size());
        mergedFamilies.addAll(config.getFontFamilies());
        for (int i = 0; i < families.size(); i++) {
            FontConfig.FontFamily family = resolveFontFiles(families.get(i));
            if (family != null) {
                mergedFamilies.add(family);
            }
        }
        return new FontConfig(mergedFamilies, config.getAliases(), this.mLastModifiedMillis, this.mConfigVersion);
    }

    private PersistentSystemFontConfig.Config readPersistentConfig() {
        PersistentSystemFontConfig.Config config = new PersistentSystemFontConfig.Config();
        try {
            FileInputStream fis = this.mConfigFile.openRead();
            PersistentSystemFontConfig.loadFromXml(fis, config);
            if (fis != null) {
                fis.close();
            }
        } catch (IOException | XmlPullParserException e) {
        }
        return config;
    }

    private void writePersistentConfig(PersistentSystemFontConfig.Config config) throws FontManagerService.SystemFontException {
        FileOutputStream fos = null;
        try {
            fos = this.mConfigFile.startWrite();
            PersistentSystemFontConfig.writeToXml(fos, config);
            this.mConfigFile.finishWrite(fos);
        } catch (IOException e) {
            if (fos != null) {
                this.mConfigFile.failWrite(fos);
            }
            throw new FontManagerService.SystemFontException(-6, "Failed to write config XML.", e);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getConfigVersion() {
        return this.mConfigVersion;
    }

    public Map<String, FontConfig.FontFamily> getFontFamilyMap() {
        PersistentSystemFontConfig.Config curConfig = readPersistentConfig();
        Map<String, FontConfig.FontFamily> familyMap = new HashMap<>();
        for (int i = 0; i < curConfig.fontFamilies.size(); i++) {
            FontUpdateRequest.Family family = curConfig.fontFamilies.get(i);
            FontConfig.FontFamily resolvedFamily = resolveFontFiles(family);
            if (resolvedFamily != null) {
                familyMap.put(family.getName(), resolvedFamily);
            }
        }
        return familyMap;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void deleteAllFiles(File filesDir, File configFile) {
        try {
            new AtomicFile(configFile).delete();
        } catch (Throwable th) {
            Slog.w(TAG, "Failed to delete " + configFile);
        }
        try {
            FileUtils.deleteContents(filesDir);
        } catch (Throwable th2) {
            Slog.w(TAG, "Failed to delete " + filesDir);
        }
    }
}
