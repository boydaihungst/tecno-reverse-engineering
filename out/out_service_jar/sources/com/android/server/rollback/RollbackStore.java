package com.android.server.rollback;

import android.content.pm.VersionedPackage;
import android.content.rollback.PackageRollbackInfo;
import android.content.rollback.RollbackInfo;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.system.ErrnoException;
import android.system.Os;
import android.util.AtomicFile;
import android.util.Slog;
import android.util.SparseIntArray;
import com.android.server.net.watchlist.WatchlistLoggingHandler;
import com.android.server.pm.verify.domain.DomainVerificationLegacySettings;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.text.ParseException;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import libcore.io.IoUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class RollbackStore {
    private static final String TAG = "RollbackManager";
    private final File mRollbackDataDir;
    private final File mRollbackHistoryDir;

    /* JADX INFO: Access modifiers changed from: package-private */
    public RollbackStore(File rollbackDataDir, File rollbackHistoryDir) {
        this.mRollbackDataDir = rollbackDataDir;
        this.mRollbackHistoryDir = rollbackHistoryDir;
    }

    private static List<Rollback> loadRollbacks(File rollbackDataDir) {
        File[] listFiles;
        List<Rollback> rollbacks = new ArrayList<>();
        rollbackDataDir.mkdirs();
        for (File rollbackDir : rollbackDataDir.listFiles()) {
            if (rollbackDir.isDirectory()) {
                try {
                    rollbacks.add(loadRollback(rollbackDir));
                } catch (IOException e) {
                    Slog.e(TAG, "Unable to read rollback at " + rollbackDir, e);
                    removeFile(rollbackDir);
                }
            }
        }
        return rollbacks;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public List<Rollback> loadRollbacks() {
        return loadRollbacks(this.mRollbackDataDir);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public List<Rollback> loadHistorialRollbacks() {
        return loadRollbacks(this.mRollbackHistoryDir);
    }

    private static List<Integer> toIntList(JSONArray jsonArray) throws JSONException {
        List<Integer> ret = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            ret.add(Integer.valueOf(jsonArray.getInt(i)));
        }
        return ret;
    }

    private static JSONArray fromIntList(List<Integer> list) {
        JSONArray jsonArray = new JSONArray();
        for (int i = 0; i < list.size(); i++) {
            jsonArray.put(list.get(i));
        }
        return jsonArray;
    }

    private static JSONArray convertToJsonArray(List<PackageRollbackInfo.RestoreInfo> list) throws JSONException {
        JSONArray jsonArray = new JSONArray();
        for (PackageRollbackInfo.RestoreInfo ri : list) {
            JSONObject jo = new JSONObject();
            jo.put("userId", ri.userId);
            jo.put("appId", ri.appId);
            jo.put("seInfo", ri.seInfo);
            jsonArray.put(jo);
        }
        return jsonArray;
    }

    private static ArrayList<PackageRollbackInfo.RestoreInfo> convertToRestoreInfoArray(JSONArray array) throws JSONException {
        ArrayList<PackageRollbackInfo.RestoreInfo> restoreInfos = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            JSONObject jo = array.getJSONObject(i);
            restoreInfos.add(new PackageRollbackInfo.RestoreInfo(jo.getInt("userId"), jo.getInt("appId"), jo.getString("seInfo")));
        }
        return restoreInfos;
    }

    private static JSONArray extensionVersionsToJson(SparseIntArray extensionVersions) throws JSONException {
        JSONArray array = new JSONArray();
        for (int i = 0; i < extensionVersions.size(); i++) {
            JSONObject entryJson = new JSONObject();
            entryJson.put("sdkVersion", extensionVersions.keyAt(i));
            entryJson.put("extensionVersion", extensionVersions.valueAt(i));
            array.put(entryJson);
        }
        return array;
    }

    private static SparseIntArray extensionVersionsFromJson(JSONArray json) throws JSONException {
        if (json == null) {
            return new SparseIntArray(0);
        }
        SparseIntArray extensionVersions = new SparseIntArray(json.length());
        for (int i = 0; i < json.length(); i++) {
            JSONObject entry = json.getJSONObject(i);
            extensionVersions.append(entry.getInt("sdkVersion"), entry.getInt("extensionVersion"));
        }
        return extensionVersions;
    }

    private static JSONObject rollbackInfoToJson(RollbackInfo rollback) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("rollbackId", rollback.getRollbackId());
        json.put("packages", toJson(rollback.getPackages()));
        json.put("isStaged", rollback.isStaged());
        json.put("causePackages", versionedPackagesToJson(rollback.getCausePackages()));
        json.put("committedSessionId", rollback.getCommittedSessionId());
        return json;
    }

    private static RollbackInfo rollbackInfoFromJson(JSONObject json) throws JSONException {
        return new RollbackInfo(json.getInt("rollbackId"), packageRollbackInfosFromJson(json.getJSONArray("packages")), json.getBoolean("isStaged"), versionedPackagesFromJson(json.getJSONArray("causePackages")), json.getInt("committedSessionId"));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Rollback createNonStagedRollback(int rollbackId, int originalSessionId, int userId, String installerPackageName, int[] packageSessionIds, SparseIntArray extensionVersions) {
        File backupDir = new File(this.mRollbackDataDir, Integer.toString(rollbackId));
        return new Rollback(rollbackId, backupDir, originalSessionId, false, userId, installerPackageName, packageSessionIds, extensionVersions);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Rollback createStagedRollback(int rollbackId, int originalSessionId, int userId, String installerPackageName, int[] packageSessionIds, SparseIntArray extensionVersions) {
        File backupDir = new File(this.mRollbackDataDir, Integer.toString(rollbackId));
        return new Rollback(rollbackId, backupDir, originalSessionId, true, userId, installerPackageName, packageSessionIds, extensionVersions);
    }

    private static boolean isLinkPossible(File oldFile, File newFile) {
        try {
            return Os.stat(oldFile.getAbsolutePath()).st_dev == Os.stat(newFile.getAbsolutePath()).st_dev;
        } catch (ErrnoException e) {
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void backupPackageCodePath(Rollback rollback, String packageName, String codePath) throws IOException {
        File sourceFile = new File(codePath);
        File targetDir = new File(rollback.getBackupDir(), packageName);
        targetDir.mkdirs();
        File targetFile = new File(targetDir, sourceFile.getName());
        boolean fallbackToCopy = !isLinkPossible(sourceFile, targetFile);
        if (!fallbackToCopy) {
            try {
                Os.link(sourceFile.getAbsolutePath(), targetFile.getAbsolutePath());
            } catch (ErrnoException e) {
                boolean isRollbackTest = SystemProperties.getBoolean("persist.rollback.is_test", false);
                if (isRollbackTest) {
                    throw new IOException(e);
                }
                fallbackToCopy = true;
            }
        }
        if (fallbackToCopy) {
            Files.copy(sourceFile.toPath(), targetFile.toPath(), new CopyOption[0]);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static File[] getPackageCodePaths(Rollback rollback, String packageName) {
        File targetDir = new File(rollback.getBackupDir(), packageName);
        File[] files = targetDir.listFiles();
        if (files == null || files.length == 0) {
            return null;
        }
        return files;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void deletePackageCodePaths(Rollback rollback) {
        for (PackageRollbackInfo info : rollback.info.getPackages()) {
            File targetDir = new File(rollback.getBackupDir(), info.getPackageName());
            removeFile(targetDir);
        }
    }

    private static void saveRollback(Rollback rollback, File backDir) {
        FileOutputStream fos = null;
        AtomicFile file = new AtomicFile(new File(backDir, "rollback.json"));
        try {
            backDir.mkdirs();
            JSONObject dataJson = new JSONObject();
            dataJson.put("info", rollbackInfoToJson(rollback.info));
            dataJson.put(WatchlistLoggingHandler.WatchlistEventKeys.TIMESTAMP, rollback.getTimestamp().toString());
            dataJson.put("originalSessionId", rollback.getOriginalSessionId());
            dataJson.put("state", rollback.getStateAsString());
            dataJson.put("stateDescription", rollback.getStateDescription());
            dataJson.put("restoreUserDataInProgress", rollback.isRestoreUserDataInProgress());
            dataJson.put("userId", rollback.getUserId());
            dataJson.putOpt("installerPackageName", rollback.getInstallerPackageName());
            dataJson.putOpt("extensionVersions", extensionVersionsToJson(rollback.getExtensionVersions()));
            fos = file.startWrite();
            fos.write(dataJson.toString().getBytes());
            fos.flush();
            file.finishWrite(fos);
        } catch (IOException | JSONException e) {
            Slog.e(TAG, "Unable to save rollback for: " + rollback.info.getRollbackId(), e);
            if (fos != null) {
                file.failWrite(fos);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void saveRollback(Rollback rollback) {
        saveRollback(rollback, rollback.getBackupDir());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void saveRollbackToHistory(Rollback rollback) {
        String suffix = Long.toHexString(rollback.getTimestamp().getEpochSecond());
        String dirName = Integer.toString(rollback.info.getRollbackId());
        File backupDir = new File(this.mRollbackHistoryDir, dirName + "-" + suffix);
        saveRollback(rollback, backupDir);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void deleteRollback(Rollback rollback) {
        removeFile(rollback.getBackupDir());
    }

    private static Rollback loadRollback(File backupDir) throws IOException {
        try {
            File rollbackJsonFile = new File(backupDir, "rollback.json");
            JSONObject dataJson = new JSONObject(IoUtils.readFileAsString(rollbackJsonFile.getAbsolutePath()));
            return rollbackFromJson(dataJson, backupDir);
        } catch (ParseException | DateTimeParseException | JSONException e) {
            throw new IOException(e);
        }
    }

    static Rollback rollbackFromJson(JSONObject dataJson, File backupDir) throws JSONException, ParseException {
        return new Rollback(rollbackInfoFromJson(dataJson.getJSONObject("info")), backupDir, Instant.parse(dataJson.getString(WatchlistLoggingHandler.WatchlistEventKeys.TIMESTAMP)), dataJson.optInt("originalSessionId", dataJson.optInt("stagedSessionId", -1)), Rollback.rollbackStateFromString(dataJson.getString("state")), dataJson.optString("stateDescription"), dataJson.getBoolean("restoreUserDataInProgress"), dataJson.optInt("userId", UserHandle.SYSTEM.getIdentifier()), dataJson.optString("installerPackageName", ""), extensionVersionsFromJson(dataJson.optJSONArray("extensionVersions")));
    }

    private static JSONObject toJson(VersionedPackage pkg) throws JSONException {
        JSONObject json = new JSONObject();
        json.put(DomainVerificationLegacySettings.ATTR_PACKAGE_NAME, pkg.getPackageName());
        json.put("longVersionCode", pkg.getLongVersionCode());
        return json;
    }

    private static VersionedPackage versionedPackageFromJson(JSONObject json) throws JSONException {
        String packageName = json.getString(DomainVerificationLegacySettings.ATTR_PACKAGE_NAME);
        long longVersionCode = json.getLong("longVersionCode");
        return new VersionedPackage(packageName, longVersionCode);
    }

    private static JSONObject toJson(PackageRollbackInfo info) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("versionRolledBackFrom", toJson(info.getVersionRolledBackFrom()));
        json.put("versionRolledBackTo", toJson(info.getVersionRolledBackTo()));
        List<Integer> pendingBackups = info.getPendingBackups();
        List<PackageRollbackInfo.RestoreInfo> pendingRestores = info.getPendingRestores();
        List<Integer> snapshottedUsers = info.getSnapshottedUsers();
        json.put("pendingBackups", fromIntList(pendingBackups));
        json.put("pendingRestores", convertToJsonArray(pendingRestores));
        json.put("isApex", info.isApex());
        json.put("isApkInApex", info.isApkInApex());
        json.put("installedUsers", fromIntList(snapshottedUsers));
        json.put("rollbackDataPolicy", info.getRollbackDataPolicy());
        return json;
    }

    private static PackageRollbackInfo packageRollbackInfoFromJson(JSONObject json) throws JSONException {
        VersionedPackage versionRolledBackFrom = versionedPackageFromJson(json.getJSONObject("versionRolledBackFrom"));
        VersionedPackage versionRolledBackTo = versionedPackageFromJson(json.getJSONObject("versionRolledBackTo"));
        List<Integer> pendingBackups = toIntList(json.getJSONArray("pendingBackups"));
        ArrayList<PackageRollbackInfo.RestoreInfo> pendingRestores = convertToRestoreInfoArray(json.getJSONArray("pendingRestores"));
        boolean isApex = json.getBoolean("isApex");
        boolean isApkInApex = json.getBoolean("isApkInApex");
        List<Integer> snapshottedUsers = toIntList(json.getJSONArray("installedUsers"));
        int rollbackDataPolicy = json.optInt("rollbackDataPolicy", 0);
        return new PackageRollbackInfo(versionRolledBackFrom, versionRolledBackTo, pendingBackups, pendingRestores, isApex, isApkInApex, snapshottedUsers, rollbackDataPolicy);
    }

    private static JSONArray versionedPackagesToJson(List<VersionedPackage> packages) throws JSONException {
        JSONArray json = new JSONArray();
        for (VersionedPackage pkg : packages) {
            json.put(toJson(pkg));
        }
        return json;
    }

    private static List<VersionedPackage> versionedPackagesFromJson(JSONArray json) throws JSONException {
        List<VersionedPackage> packages = new ArrayList<>();
        for (int i = 0; i < json.length(); i++) {
            packages.add(versionedPackageFromJson(json.getJSONObject(i)));
        }
        return packages;
    }

    private static JSONArray toJson(List<PackageRollbackInfo> infos) throws JSONException {
        JSONArray json = new JSONArray();
        for (PackageRollbackInfo info : infos) {
            json.put(toJson(info));
        }
        return json;
    }

    private static List<PackageRollbackInfo> packageRollbackInfosFromJson(JSONArray json) throws JSONException {
        List<PackageRollbackInfo> infos = new ArrayList<>();
        for (int i = 0; i < json.length(); i++) {
            infos.add(packageRollbackInfoFromJson(json.getJSONObject(i)));
        }
        return infos;
    }

    private static void removeFile(File file) {
        File[] listFiles;
        if (file.isDirectory()) {
            for (File child : file.listFiles()) {
                removeFile(child);
            }
        }
        if (file.exists()) {
            file.delete();
        }
    }
}
