package com.android.timezone.distro.installer;

import android.util.Slog;
import com.android.i18n.timezone.TelephonyLookup;
import com.android.i18n.timezone.TimeZoneFinder;
import com.android.i18n.timezone.TzDataSetVersion;
import com.android.i18n.timezone.ZoneInfoDb;
import com.android.timezone.distro.DistroException;
import com.android.timezone.distro.DistroVersion;
import com.android.timezone.distro.FileUtils;
import com.android.timezone.distro.StagedDistroOperation;
import com.android.timezone.distro.TimeZoneDistro;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
/* loaded from: classes2.dex */
public class TimeZoneDistroInstaller {
    private static final String CURRENT_TZ_DATA_DIR_NAME = "current";
    public static final int INSTALL_FAIL_BAD_DISTRO_FORMAT_VERSION = 2;
    public static final int INSTALL_FAIL_BAD_DISTRO_STRUCTURE = 1;
    public static final int INSTALL_FAIL_RULES_TOO_OLD = 3;
    public static final int INSTALL_FAIL_VALIDATION_ERROR = 4;
    public static final int INSTALL_SUCCESS = 0;
    private static final String OLD_TZ_DATA_DIR_NAME = "old";
    private static final String STAGED_TZ_DATA_DIR_NAME = "staged";
    public static final int UNINSTALL_FAIL = 2;
    public static final int UNINSTALL_NOTHING_INSTALLED = 1;
    public static final int UNINSTALL_SUCCESS = 0;
    public static final String UNINSTALL_TOMBSTONE_FILE_NAME = "STAGED_UNINSTALL_TOMBSTONE";
    private static final String WORKING_DIR_NAME = "working";
    private final File baseVersionFile;
    private final File currentTzDataDir;
    private final String logTag;
    private final File oldStagedDataDir;
    private final File stagedTzDataDir;
    private final File workingDir;

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes2.dex */
    private @interface InstallResultType {
    }

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes2.dex */
    private @interface UninstallResultType {
    }

    public TimeZoneDistroInstaller(String logTag, File baseVersionFile, File installDir) {
        this.logTag = logTag;
        this.baseVersionFile = baseVersionFile;
        this.oldStagedDataDir = new File(installDir, OLD_TZ_DATA_DIR_NAME);
        this.stagedTzDataDir = new File(installDir, STAGED_TZ_DATA_DIR_NAME);
        this.currentTzDataDir = new File(installDir, CURRENT_TZ_DATA_DIR_NAME);
        this.workingDir = new File(installDir, WORKING_DIR_NAME);
    }

    File getOldStagedDataDir() {
        return this.oldStagedDataDir;
    }

    File getStagedTzDataDir() {
        return this.stagedTzDataDir;
    }

    File getCurrentTzDataDir() {
        return this.currentTzDataDir;
    }

    File getWorkingDir() {
        return this.workingDir;
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [273=13, 274=13] */
    public int stageInstallWithErrorCode(TimeZoneDistro distro) throws IOException {
        File file;
        File file2;
        File zoneInfoFile;
        File tzLookupFile;
        File telephonyLookupFile;
        if (this.oldStagedDataDir.exists()) {
            FileUtils.deleteRecursive(this.oldStagedDataDir);
        }
        if (this.workingDir.exists()) {
            FileUtils.deleteRecursive(this.workingDir);
        }
        Slog.i(this.logTag, "Unpacking / verifying time zone update");
        try {
            unpackDistro(distro, this.workingDir);
            DistroVersion distroVersion = readDistroVersion(this.workingDir);
            if (distroVersion == null) {
                Slog.i(this.logTag, "Update not applied: Distro version could not be loaded");
                return 1;
            }
            TzDataSetVersion distroTzDataSetVersion = new TzDataSetVersion(distroVersion.formatMajorVersion, distroVersion.formatMinorVersion, distroVersion.rulesVersion, distroVersion.revision);
            if (!TzDataSetVersion.isCompatibleWithThisDevice(distroTzDataSetVersion)) {
                Slog.i(this.logTag, "Update not applied: Distro format version check failed: " + distroVersion);
                return 2;
            } else if (!checkDistroDataFilesExist(this.workingDir)) {
                Slog.i(this.logTag, "Update not applied: Distro is missing required data file(s)");
                return 1;
            } else if (!checkDistroRulesNewerThanBase(this.baseVersionFile, distroVersion)) {
                Slog.i(this.logTag, "Update not applied: Distro rules version check failed");
                return 3;
            } else {
                zoneInfoFile = new File(this.workingDir, TimeZoneDistro.TZDATA_FILE_NAME);
                ZoneInfoDb.validateTzData(zoneInfoFile.getPath());
                tzLookupFile = new File(this.workingDir, TimeZoneDistro.TZLOOKUP_FILE_NAME);
                if (!tzLookupFile.exists()) {
                    Slog.i(this.logTag, "Update not applied: " + tzLookupFile + " does not exist");
                    return 1;
                }
                TimeZoneFinder timeZoneFinder = TimeZoneFinder.createInstance(tzLookupFile.getPath());
                timeZoneFinder.validate();
                telephonyLookupFile = new File(this.workingDir, TimeZoneDistro.TELEPHONYLOOKUP_FILE_NAME);
                if (!telephonyLookupFile.exists()) {
                    Slog.i(this.logTag, "Update not applied: " + telephonyLookupFile + " does not exist");
                    return 1;
                }
                TelephonyLookup telephonyLookup = TelephonyLookup.createInstance(telephonyLookupFile.getPath());
                telephonyLookup.validate();
                Slog.i(this.logTag, "Applying time zone update");
                FileUtils.makeDirectoryWorldAccessible(this.workingDir);
                if (this.stagedTzDataDir.exists()) {
                    Slog.i(this.logTag, "Moving " + this.stagedTzDataDir + " to " + this.oldStagedDataDir);
                    FileUtils.rename(this.stagedTzDataDir, this.oldStagedDataDir);
                } else {
                    Slog.i(this.logTag, "Nothing to unstage at " + this.stagedTzDataDir);
                }
                Slog.i(this.logTag, "Moving " + this.workingDir + " to " + this.stagedTzDataDir);
                FileUtils.rename(this.workingDir, this.stagedTzDataDir);
                Slog.i(this.logTag, "Install staged: " + this.stagedTzDataDir + " successfully created");
                return 0;
            }
        } catch (DistroException e) {
            Slog.i(this.logTag, "Invalid distro version: " + e.getMessage());
            return 1;
        } catch (IOException e2) {
            Slog.i(this.logTag, "Update not applied: " + tzLookupFile + " failed validation", e2);
            return 4;
        } catch (IOException e3) {
            Slog.i(this.logTag, "Update not applied: " + telephonyLookupFile + " failed validation", e3);
            return 4;
        } catch (IOException e4) {
            Slog.i(this.logTag, "Update not applied: " + zoneInfoFile + " failed validation", e4);
            return 4;
        } catch (TzDataSetVersion.TzDataSetException e5) {
            Slog.i(this.logTag, "Update not applied: Distro version could not be converted", e5);
            return 1;
        } finally {
            deleteBestEffort(this.oldStagedDataDir);
            deleteBestEffort(this.workingDir);
        }
    }

    public int stageUninstall() throws IOException {
        Slog.i(this.logTag, "Uninstalling time zone update");
        if (this.oldStagedDataDir.exists()) {
            FileUtils.deleteRecursive(this.oldStagedDataDir);
        }
        if (this.workingDir.exists()) {
            FileUtils.deleteRecursive(this.workingDir);
        }
        try {
            if (!this.stagedTzDataDir.exists()) {
                Slog.i(this.logTag, "Nothing to unstage at " + this.stagedTzDataDir);
            } else {
                Slog.i(this.logTag, "Moving " + this.stagedTzDataDir + " to " + this.oldStagedDataDir);
                FileUtils.rename(this.stagedTzDataDir, this.oldStagedDataDir);
            }
            if (!this.currentTzDataDir.exists()) {
                Slog.i(this.logTag, "Nothing to uninstall at " + this.currentTzDataDir);
                return 1;
            }
            FileUtils.ensureDirectoriesExist(this.workingDir, true);
            FileUtils.createEmptyFile(new File(this.workingDir, UNINSTALL_TOMBSTONE_FILE_NAME));
            Slog.i(this.logTag, "Moving " + this.workingDir + " to " + this.stagedTzDataDir);
            FileUtils.rename(this.workingDir, this.stagedTzDataDir);
            Slog.i(this.logTag, "Uninstall staged: " + this.stagedTzDataDir + " successfully created");
            return 0;
        } finally {
            deleteBestEffort(this.oldStagedDataDir);
            deleteBestEffort(this.workingDir);
        }
    }

    public DistroVersion getInstalledDistroVersion() throws DistroException, IOException {
        if (!this.currentTzDataDir.exists()) {
            return null;
        }
        return readDistroVersion(this.currentTzDataDir);
    }

    public StagedDistroOperation getStagedDistroOperation() throws DistroException, IOException {
        if (!this.stagedTzDataDir.exists()) {
            return null;
        }
        if (new File(this.stagedTzDataDir, UNINSTALL_TOMBSTONE_FILE_NAME).exists()) {
            return StagedDistroOperation.uninstall();
        }
        return StagedDistroOperation.install(readDistroVersion(this.stagedTzDataDir));
    }

    public TzDataSetVersion readBaseVersion() throws IOException {
        return readBaseVersion(this.baseVersionFile);
    }

    private TzDataSetVersion readBaseVersion(File baseVersionFile) throws IOException {
        if (!baseVersionFile.exists()) {
            Slog.i(this.logTag, "version file cannot be found in " + baseVersionFile);
            throw new FileNotFoundException("base version file does not exist: " + baseVersionFile);
        }
        try {
            return TzDataSetVersion.readFromFile(baseVersionFile);
        } catch (TzDataSetVersion.TzDataSetException e) {
            throw new IOException("Unable to read: " + baseVersionFile, e);
        }
    }

    private void deleteBestEffort(File dir) {
        if (dir.exists()) {
            Slog.i(this.logTag, "Deleting " + dir);
            try {
                FileUtils.deleteRecursive(dir);
            } catch (IOException e) {
                Slog.w(this.logTag, "Unable to delete " + dir, e);
            }
        }
    }

    private void unpackDistro(TimeZoneDistro distro, File targetDir) throws IOException {
        Slog.i(this.logTag, "Unpacking update content to: " + targetDir);
        distro.extractTo(targetDir);
    }

    private boolean checkDistroDataFilesExist(File unpackedContentDir) throws IOException {
        Slog.i(this.logTag, "Verifying distro contents");
        return FileUtils.filesExist(unpackedContentDir, TimeZoneDistro.TZDATA_FILE_NAME, TimeZoneDistro.ICU_DATA_FILE_NAME);
    }

    private DistroVersion readDistroVersion(File distroDir) throws DistroException, IOException {
        Slog.d(this.logTag, "Reading distro format version: " + distroDir);
        File distroVersionFile = new File(distroDir, TimeZoneDistro.DISTRO_VERSION_FILE_NAME);
        if (!distroVersionFile.exists()) {
            throw new DistroException("No distro version file found: " + distroVersionFile);
        }
        byte[] versionBytes = FileUtils.readBytes(distroVersionFile, DistroVersion.DISTRO_VERSION_FILE_LENGTH);
        return DistroVersion.fromBytes(versionBytes);
    }

    private boolean checkDistroRulesNewerThanBase(File baseVersionFile, DistroVersion distroVersion) throws IOException {
        Slog.i(this.logTag, "Reading base time zone rules version");
        TzDataSetVersion baseVersion = readBaseVersion(baseVersionFile);
        String baseRulesVersion = baseVersion.getRulesVersion();
        String distroRulesVersion = distroVersion.rulesVersion;
        boolean canApply = distroRulesVersion.compareTo(baseRulesVersion) >= 0;
        if (!canApply) {
            Slog.i(this.logTag, "Failed rules version check: distroRulesVersion=" + distroRulesVersion + ", baseRulesVersion=" + baseRulesVersion);
        } else {
            Slog.i(this.logTag, "Passed rules version check: distroRulesVersion=" + distroRulesVersion + ", baseRulesVersion=" + baseRulesVersion);
        }
        return canApply;
    }
}
