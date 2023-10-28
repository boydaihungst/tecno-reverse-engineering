package com.android.server.pm.dex;

import android.os.Build;
import android.util.AtomicFile;
import android.util.Slog;
import com.android.internal.util.FastPrintWriter;
import com.android.server.pm.AbstractStatsBase;
import com.android.server.pm.PackageManagerServiceUtils;
import dalvik.system.VMRuntime;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import libcore.io.IoUtils;
/* loaded from: classes2.dex */
public class PackageDexUsage extends AbstractStatsBase<Void> {
    private static final String CODE_PATH_LINE_CHAR = "+";
    private static final String DEX_LINE_CHAR = "#";
    private static final String LOADING_PACKAGE_CHAR = "@";
    static final int MAX_SECONDARY_FILES_PER_OWNER = 100;
    private static final int PACKAGE_DEX_USAGE_VERSION = 2;
    private static final String PACKAGE_DEX_USAGE_VERSION_HEADER = "PACKAGE_MANAGER__PACKAGE_DEX_USAGE__";
    private static final String SPLIT_CHAR = ",";
    private static final String TAG = "PackageDexUsage";
    static final String UNSUPPORTED_CLASS_LOADER_CONTEXT = "=UnsupportedClassLoaderContext=";
    static final String VARIABLE_CLASS_LOADER_CONTEXT = "=VariableClassLoaderContext=";
    private final Map<String, PackageUseInfo> mPackageUseInfoMap;

    /* JADX INFO: Access modifiers changed from: package-private */
    public PackageDexUsage() {
        super("package-dex-usage.list", "PackageDexUsage_DiskWriter", false);
        this.mPackageUseInfoMap = new HashMap();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean record(String owningPackageName, String dexPath, int ownerUserId, String loaderIsa, boolean primaryOrSplit, String loadingPackageName, String classLoaderContext, boolean overwriteCLC) {
        if (!PackageManagerServiceUtils.checkISA(loaderIsa)) {
            throw new IllegalArgumentException("loaderIsa " + loaderIsa + " is unsupported");
        }
        if (classLoaderContext == null) {
            throw new IllegalArgumentException("Null classLoaderContext");
        }
        boolean z = false;
        if (classLoaderContext.equals(UNSUPPORTED_CLASS_LOADER_CONTEXT)) {
            Slog.e(TAG, "Unsupported context?");
            return false;
        }
        boolean isUsedByOtherApps = !owningPackageName.equals(loadingPackageName);
        synchronized (this.mPackageUseInfoMap) {
            try {
                try {
                    PackageUseInfo packageUseInfo = this.mPackageUseInfoMap.get(owningPackageName);
                    if (packageUseInfo == null) {
                        PackageUseInfo packageUseInfo2 = new PackageUseInfo(owningPackageName);
                        if (primaryOrSplit) {
                            packageUseInfo2.mergePrimaryCodePaths(dexPath, loadingPackageName);
                        } else {
                            DexUseInfo newData = new DexUseInfo(isUsedByOtherApps, ownerUserId, classLoaderContext, loaderIsa);
                            packageUseInfo2.mDexUseInfoMap.put(dexPath, newData);
                            maybeAddLoadingPackage(owningPackageName, loadingPackageName, newData.mLoadingPackages);
                        }
                        this.mPackageUseInfoMap.put(owningPackageName, packageUseInfo2);
                        return true;
                    } else if (primaryOrSplit) {
                        return packageUseInfo.mergePrimaryCodePaths(dexPath, loadingPackageName);
                    } else {
                        DexUseInfo newData2 = new DexUseInfo(isUsedByOtherApps, ownerUserId, classLoaderContext, loaderIsa);
                        boolean updateLoadingPackages = maybeAddLoadingPackage(owningPackageName, loadingPackageName, newData2.mLoadingPackages);
                        DexUseInfo existingData = (DexUseInfo) packageUseInfo.mDexUseInfoMap.get(dexPath);
                        if (existingData == null) {
                            if (packageUseInfo.mDexUseInfoMap.size() < 100) {
                                packageUseInfo.mDexUseInfoMap.put(dexPath, newData2);
                                return true;
                            }
                            return updateLoadingPackages;
                        } else if (ownerUserId == existingData.mOwnerUserId) {
                            if (existingData.merge(newData2, overwriteCLC) || updateLoadingPackages) {
                                z = true;
                            }
                            return z;
                        } else {
                            throw new IllegalArgumentException("Trying to change ownerUserId for  dex path " + dexPath + " from " + existingData.mOwnerUserId + " to " + ownerUserId);
                        }
                    }
                } catch (Throwable th) {
                    th = th;
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void read() {
        read((PackageDexUsage) null);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void maybeWriteAsync() {
        maybeWriteAsync(null);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void writeNow() {
        writeInternal((Void) null);
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.pm.AbstractStatsBase
    public void writeInternal(Void data) {
        AtomicFile file = getFile();
        FileOutputStream f = null;
        try {
            f = file.startWrite();
            OutputStreamWriter osw = new OutputStreamWriter(f);
            write(osw);
            osw.flush();
            file.finishWrite(f);
        } catch (IOException e) {
            if (f != null) {
                file.failWrite(f);
            }
            Slog.e(TAG, "Failed to write usage for dex files", e);
        }
    }

    void write(Writer out) {
        Map<String, PackageUseInfo> packageUseInfoMapClone = clonePackageUseInfoMap();
        FastPrintWriter fpw = new FastPrintWriter(out);
        fpw.print(PACKAGE_DEX_USAGE_VERSION_HEADER);
        int i = 2;
        fpw.println(2);
        for (Map.Entry<String, PackageUseInfo> pEntry : packageUseInfoMapClone.entrySet()) {
            String packageName = pEntry.getKey();
            PackageUseInfo packageUseInfo = pEntry.getValue();
            fpw.println(packageName);
            for (Map.Entry<String, Set<String>> codeEntry : packageUseInfo.mPrimaryCodePaths.entrySet()) {
                String codePath = codeEntry.getKey();
                Set<String> loadingPackages = codeEntry.getValue();
                fpw.println(CODE_PATH_LINE_CHAR + codePath);
                fpw.println(LOADING_PACKAGE_CHAR + String.join(SPLIT_CHAR, loadingPackages));
            }
            for (Map.Entry<String, DexUseInfo> dEntry : packageUseInfo.mDexUseInfoMap.entrySet()) {
                String dexPath = dEntry.getKey();
                DexUseInfo dexUseInfo = dEntry.getValue();
                fpw.println(DEX_LINE_CHAR + dexPath);
                CharSequence[] charSequenceArr = new CharSequence[i];
                charSequenceArr[0] = Integer.toString(dexUseInfo.mOwnerUserId);
                Map<String, PackageUseInfo> packageUseInfoMapClone2 = packageUseInfoMapClone;
                charSequenceArr[1] = writeBoolean(dexUseInfo.mIsUsedByOtherApps);
                fpw.print(String.join(SPLIT_CHAR, charSequenceArr));
                for (String isa : dexUseInfo.mLoaderIsas) {
                    fpw.print(SPLIT_CHAR + isa);
                }
                fpw.println();
                fpw.println(LOADING_PACKAGE_CHAR + String.join(SPLIT_CHAR, dexUseInfo.mLoadingPackages));
                fpw.println(dexUseInfo.getClassLoaderContext());
                packageUseInfoMapClone = packageUseInfoMapClone2;
                i = 2;
            }
            packageUseInfoMapClone = packageUseInfoMapClone;
            i = 2;
        }
        fpw.flush();
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.pm.AbstractStatsBase
    public void readInternal(Void data) {
        AtomicFile file = getFile();
        BufferedReader in = null;
        try {
            try {
                in = new BufferedReader(new InputStreamReader(file.openRead()));
                read((Reader) in);
            } catch (FileNotFoundException e) {
            } catch (IOException e2) {
                Slog.w(TAG, "Failed to parse package dex usage.", e2);
            }
        } finally {
            IoUtils.closeQuietly(in);
        }
    }

    void read(Reader reader) throws IOException {
        String[] strArr;
        String currentPackage;
        Set<String> loadingPackages;
        String classLoaderContext;
        String[] elems;
        Map<String, PackageUseInfo> data = new HashMap<>();
        BufferedReader in = new BufferedReader(reader);
        String versionLine = in.readLine();
        if (versionLine == null) {
            throw new IllegalStateException("No version line found.");
        }
        if (!versionLine.startsWith(PACKAGE_DEX_USAGE_VERSION_HEADER)) {
            throw new IllegalStateException("Invalid version line: " + versionLine);
        }
        int version = Integer.parseInt(versionLine.substring(PACKAGE_DEX_USAGE_VERSION_HEADER.length()));
        if (!isSupportedVersion(version)) {
            Slog.w(TAG, "Unexpected package-dex-use version: " + version + ". Not reading from it");
            return;
        }
        Set<String> supportedIsas = new HashSet<>();
        char c = 0;
        for (String abi : Build.SUPPORTED_ABIS) {
            supportedIsas.add(VMRuntime.getInstructionSet(abi));
        }
        PackageUseInfo currentPackageData = null;
        String currentPackage2 = null;
        while (true) {
            String line = in.readLine();
            if (line == null) {
                synchronized (this.mPackageUseInfoMap) {
                    this.mPackageUseInfoMap.clear();
                    this.mPackageUseInfoMap.putAll(data);
                }
                return;
            }
            if (line.startsWith(DEX_LINE_CHAR)) {
                if (currentPackage2 == null) {
                    throw new IllegalStateException("Malformed PackageDexUsage file. Expected package line before dex line.");
                }
                String dexPath = line.substring(DEX_LINE_CHAR.length());
                String line2 = in.readLine();
                if (line2 == null) {
                    throw new IllegalStateException("Could not find dexUseInfo line");
                }
                String[] elems2 = line2.split(SPLIT_CHAR);
                if (elems2.length < 3) {
                    throw new IllegalStateException("Invalid PackageDexUsage line: " + line2);
                }
                Set<String> loadingPackages2 = readLoadingPackages(in, version);
                String classLoaderContext2 = readClassLoaderContext(in, version);
                if (UNSUPPORTED_CLASS_LOADER_CONTEXT.equals(classLoaderContext2)) {
                    currentPackage = currentPackage2;
                } else {
                    int ownerUserId = Integer.parseInt(elems2[c]);
                    boolean isUsedByOtherApps = readBoolean(elems2[1]);
                    currentPackage = currentPackage2;
                    DexUseInfo dexUseInfo = new DexUseInfo(isUsedByOtherApps, ownerUserId, classLoaderContext2, null);
                    dexUseInfo.mLoadingPackages.addAll(loadingPackages2);
                    int i = 2;
                    while (true) {
                        boolean isUsedByOtherApps2 = isUsedByOtherApps;
                        if (i >= elems2.length) {
                            break;
                        }
                        String isa = elems2[i];
                        if (supportedIsas.contains(isa)) {
                            loadingPackages = loadingPackages2;
                            classLoaderContext = classLoaderContext2;
                            dexUseInfo.mLoaderIsas.add(elems2[i]);
                            elems = elems2;
                        } else {
                            loadingPackages = loadingPackages2;
                            classLoaderContext = classLoaderContext2;
                            elems = elems2;
                            Slog.wtf(TAG, "Unsupported ISA when parsing PackageDexUsage: " + isa);
                        }
                        i++;
                        isUsedByOtherApps = isUsedByOtherApps2;
                        loadingPackages2 = loadingPackages;
                        classLoaderContext2 = classLoaderContext;
                        elems2 = elems;
                    }
                    if (supportedIsas.isEmpty()) {
                        Slog.wtf(TAG, "Ignore dexPath when parsing PackageDexUsage because of unsupported isas. dexPath=" + dexPath);
                    } else {
                        currentPackageData.mDexUseInfoMap.put(dexPath, dexUseInfo);
                    }
                }
            } else {
                currentPackage = currentPackage2;
                if (line.startsWith(CODE_PATH_LINE_CHAR)) {
                    String codePath = line.substring(CODE_PATH_LINE_CHAR.length());
                    currentPackageData.mPrimaryCodePaths.put(codePath, readLoadingPackages(in, version));
                } else {
                    currentPackage2 = line;
                    currentPackageData = new PackageUseInfo(currentPackage2);
                    data.put(currentPackage2, currentPackageData);
                    c = 0;
                }
            }
            currentPackage2 = currentPackage;
            c = 0;
        }
    }

    private String readClassLoaderContext(BufferedReader in, int version) throws IOException {
        String context = in.readLine();
        if (context == null) {
            throw new IllegalStateException("Could not find the classLoaderContext line.");
        }
        return context;
    }

    private Set<String> readLoadingPackages(BufferedReader in, int version) throws IOException {
        String line = in.readLine();
        if (line == null) {
            throw new IllegalStateException("Could not find the loadingPackages line.");
        }
        Set<String> result = new HashSet<>();
        if (line.length() != LOADING_PACKAGE_CHAR.length()) {
            Collections.addAll(result, line.substring(LOADING_PACKAGE_CHAR.length()).split(SPLIT_CHAR));
        }
        return result;
    }

    private boolean maybeAddLoadingPackage(String owningPackage, String loadingPackage, Set<String> loadingPackages) {
        return !owningPackage.equals(loadingPackage) && loadingPackages.add(loadingPackage);
    }

    private boolean isSupportedVersion(int version) {
        return version == 2;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void syncData(Map<String, Set<Integer>> packageToUsersMap, Map<String, Set<String>> packageToCodePaths, List<String> packagesToKeepDataAbout) {
        synchronized (this.mPackageUseInfoMap) {
            try {
                try {
                    Iterator<Map.Entry<String, PackageUseInfo>> pIt = this.mPackageUseInfoMap.entrySet().iterator();
                    while (pIt.hasNext()) {
                        Map.Entry<String, PackageUseInfo> pEntry = pIt.next();
                        String packageName = pEntry.getKey();
                        if (!packagesToKeepDataAbout.contains(packageName)) {
                            PackageUseInfo packageUseInfo = pEntry.getValue();
                            Set<Integer> users = packageToUsersMap.get(packageName);
                            if (users == null) {
                                pIt.remove();
                            } else {
                                Iterator<Map.Entry<String, DexUseInfo>> dIt = packageUseInfo.mDexUseInfoMap.entrySet().iterator();
                                while (dIt.hasNext()) {
                                    DexUseInfo dexUseInfo = dIt.next().getValue();
                                    if (!users.contains(Integer.valueOf(dexUseInfo.mOwnerUserId))) {
                                        dIt.remove();
                                    }
                                }
                                Set<String> codePaths = packageToCodePaths.get(packageName);
                                Iterator<Map.Entry<String, Set<String>>> recordedIt = packageUseInfo.mPrimaryCodePaths.entrySet().iterator();
                                while (recordedIt.hasNext()) {
                                    Map.Entry<String, Set<String>> entry = recordedIt.next();
                                    String recordedCodePath = entry.getKey();
                                    if (!codePaths.contains(recordedCodePath)) {
                                        recordedIt.remove();
                                    } else {
                                        Set<String> recordedLoadingPackages = entry.getValue();
                                        Iterator<String> recordedLoadingPackagesIt = recordedLoadingPackages.iterator();
                                        while (recordedLoadingPackagesIt.hasNext()) {
                                            String recordedLoadingPackage = recordedLoadingPackagesIt.next();
                                            if (!packagesToKeepDataAbout.contains(recordedLoadingPackage) && !packageToUsersMap.containsKey(recordedLoadingPackage)) {
                                                recordedLoadingPackagesIt.remove();
                                            }
                                        }
                                    }
                                }
                                if (!packageUseInfo.isAnyCodePathUsedByOtherApps() && packageUseInfo.mDexUseInfoMap.isEmpty()) {
                                    pIt.remove();
                                }
                            }
                        }
                    }
                } catch (Throwable th) {
                    th = th;
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
                throw th;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean clearUsedByOtherApps(String packageName) {
        synchronized (this.mPackageUseInfoMap) {
            PackageUseInfo packageUseInfo = this.mPackageUseInfoMap.get(packageName);
            if (packageUseInfo == null) {
                return false;
            }
            return packageUseInfo.clearCodePathUsedByOtherApps();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean removePackage(String packageName) {
        boolean z;
        synchronized (this.mPackageUseInfoMap) {
            z = this.mPackageUseInfoMap.remove(packageName) != null;
        }
        return z;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean removeUserPackage(String packageName, int userId) {
        synchronized (this.mPackageUseInfoMap) {
            PackageUseInfo packageUseInfo = this.mPackageUseInfoMap.get(packageName);
            if (packageUseInfo == null) {
                return false;
            }
            boolean updated = false;
            Iterator<Map.Entry<String, DexUseInfo>> dIt = packageUseInfo.mDexUseInfoMap.entrySet().iterator();
            while (dIt.hasNext()) {
                DexUseInfo dexUseInfo = dIt.next().getValue();
                if (dexUseInfo.mOwnerUserId == userId) {
                    dIt.remove();
                    updated = true;
                }
            }
            if (packageUseInfo.mDexUseInfoMap.isEmpty() && !packageUseInfo.isAnyCodePathUsedByOtherApps()) {
                this.mPackageUseInfoMap.remove(packageName);
                updated = true;
            }
            return updated;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean removeDexFile(String packageName, String dexFile, int userId) {
        synchronized (this.mPackageUseInfoMap) {
            PackageUseInfo packageUseInfo = this.mPackageUseInfoMap.get(packageName);
            if (packageUseInfo == null) {
                return false;
            }
            return removeDexFile(packageUseInfo, dexFile, userId);
        }
    }

    private boolean removeDexFile(PackageUseInfo packageUseInfo, String dexFile, int userId) {
        DexUseInfo dexUseInfo = (DexUseInfo) packageUseInfo.mDexUseInfoMap.get(dexFile);
        if (dexUseInfo == null || dexUseInfo.mOwnerUserId != userId) {
            return false;
        }
        packageUseInfo.mDexUseInfoMap.remove(dexFile);
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public PackageUseInfo getPackageUseInfo(String packageName) {
        PackageUseInfo packageUseInfo;
        synchronized (this.mPackageUseInfoMap) {
            PackageUseInfo useInfo = this.mPackageUseInfoMap.get(packageName);
            packageUseInfo = null;
            if (useInfo != null) {
                packageUseInfo = new PackageUseInfo(useInfo);
            }
        }
        return packageUseInfo;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Set<String> getAllPackagesWithSecondaryDexFiles() {
        Set<String> packages = new HashSet<>();
        synchronized (this.mPackageUseInfoMap) {
            for (Map.Entry<String, PackageUseInfo> entry : this.mPackageUseInfoMap.entrySet()) {
                if (!entry.getValue().mDexUseInfoMap.isEmpty()) {
                    packages.add(entry.getKey());
                }
            }
        }
        return packages;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void clear() {
        synchronized (this.mPackageUseInfoMap) {
            this.mPackageUseInfoMap.clear();
        }
    }

    private Map<String, PackageUseInfo> clonePackageUseInfoMap() {
        Map<String, PackageUseInfo> clone = new HashMap<>();
        synchronized (this.mPackageUseInfoMap) {
            for (Map.Entry<String, PackageUseInfo> e : this.mPackageUseInfoMap.entrySet()) {
                clone.put(e.getKey(), new PackageUseInfo(e.getValue()));
            }
        }
        return clone;
    }

    private String writeBoolean(boolean bool) {
        return bool ? "1" : "0";
    }

    private boolean readBoolean(String bool) {
        if ("0".equals(bool)) {
            return false;
        }
        if ("1".equals(bool)) {
            return true;
        }
        throw new IllegalArgumentException("Unknown bool encoding: " + bool);
    }

    String dump() {
        StringWriter sw = new StringWriter();
        write(sw);
        return sw.toString();
    }

    /* loaded from: classes2.dex */
    public static class PackageUseInfo {
        private final Map<String, DexUseInfo> mDexUseInfoMap;
        private final String mPackageName;
        private final Map<String, Set<String>> mPrimaryCodePaths;

        /* JADX INFO: Access modifiers changed from: package-private */
        public PackageUseInfo(String packageName) {
            this.mPrimaryCodePaths = new HashMap();
            this.mDexUseInfoMap = new HashMap();
            this.mPackageName = packageName;
        }

        private PackageUseInfo(PackageUseInfo other) {
            this.mPackageName = other.mPackageName;
            this.mPrimaryCodePaths = new HashMap();
            for (Map.Entry<String, Set<String>> e : other.mPrimaryCodePaths.entrySet()) {
                this.mPrimaryCodePaths.put(e.getKey(), new HashSet(e.getValue()));
            }
            this.mDexUseInfoMap = new HashMap();
            for (Map.Entry<String, DexUseInfo> e2 : other.mDexUseInfoMap.entrySet()) {
                this.mDexUseInfoMap.put(e2.getKey(), new DexUseInfo(e2.getValue()));
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public boolean mergePrimaryCodePaths(String codePath, String loadingPackage) {
            Set<String> loadingPackages = this.mPrimaryCodePaths.get(codePath);
            if (loadingPackages == null) {
                loadingPackages = new HashSet();
                this.mPrimaryCodePaths.put(codePath, loadingPackages);
            }
            return loadingPackages.add(loadingPackage);
        }

        public boolean isUsedByOtherApps(String codePath) {
            if (this.mPrimaryCodePaths.containsKey(codePath)) {
                Set<String> loadingPackages = this.mPrimaryCodePaths.get(codePath);
                if (loadingPackages.contains(this.mPackageName)) {
                    return loadingPackages.size() > 1;
                }
                return !loadingPackages.isEmpty();
            }
            return false;
        }

        public Map<String, DexUseInfo> getDexUseInfoMap() {
            return this.mDexUseInfoMap;
        }

        public Set<String> getLoadingPackages(String codePath) {
            return this.mPrimaryCodePaths.getOrDefault(codePath, null);
        }

        public boolean isAnyCodePathUsedByOtherApps() {
            return !this.mPrimaryCodePaths.isEmpty();
        }

        boolean clearCodePathUsedByOtherApps() {
            boolean updated = false;
            List<String> retainOnlyOwningPackage = new ArrayList<>(1);
            retainOnlyOwningPackage.add(this.mPackageName);
            for (Map.Entry<String, Set<String>> entry : this.mPrimaryCodePaths.entrySet()) {
                if (entry.getValue().retainAll(retainOnlyOwningPackage)) {
                    updated = true;
                }
            }
            return updated;
        }
    }

    /* loaded from: classes2.dex */
    public static class DexUseInfo {
        private String mClassLoaderContext;
        private boolean mIsUsedByOtherApps;
        private final Set<String> mLoaderIsas;
        private final Set<String> mLoadingPackages;
        private final int mOwnerUserId;

        DexUseInfo(boolean isUsedByOtherApps, int ownerUserId, String classLoaderContext, String loaderIsa) {
            this.mIsUsedByOtherApps = isUsedByOtherApps;
            this.mOwnerUserId = ownerUserId;
            this.mClassLoaderContext = classLoaderContext;
            HashSet hashSet = new HashSet();
            this.mLoaderIsas = hashSet;
            if (loaderIsa != null) {
                hashSet.add(loaderIsa);
            }
            this.mLoadingPackages = new HashSet();
        }

        private DexUseInfo(DexUseInfo other) {
            this.mIsUsedByOtherApps = other.mIsUsedByOtherApps;
            this.mOwnerUserId = other.mOwnerUserId;
            this.mClassLoaderContext = other.mClassLoaderContext;
            this.mLoaderIsas = new HashSet(other.mLoaderIsas);
            this.mLoadingPackages = new HashSet(other.mLoadingPackages);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public boolean merge(DexUseInfo dexUseInfo, boolean overwriteCLC) {
            boolean oldIsUsedByOtherApps = this.mIsUsedByOtherApps;
            this.mIsUsedByOtherApps = this.mIsUsedByOtherApps || dexUseInfo.mIsUsedByOtherApps;
            boolean updateIsas = this.mLoaderIsas.addAll(dexUseInfo.mLoaderIsas);
            boolean updateLoadingPackages = this.mLoadingPackages.addAll(dexUseInfo.mLoadingPackages);
            String oldClassLoaderContext = this.mClassLoaderContext;
            if (overwriteCLC) {
                this.mClassLoaderContext = dexUseInfo.mClassLoaderContext;
            } else if (isUnsupportedContext(this.mClassLoaderContext)) {
                this.mClassLoaderContext = dexUseInfo.mClassLoaderContext;
            } else if (!Objects.equals(this.mClassLoaderContext, dexUseInfo.mClassLoaderContext)) {
                this.mClassLoaderContext = PackageDexUsage.VARIABLE_CLASS_LOADER_CONTEXT;
            }
            return updateIsas || oldIsUsedByOtherApps != this.mIsUsedByOtherApps || updateLoadingPackages || !Objects.equals(oldClassLoaderContext, this.mClassLoaderContext);
        }

        private static boolean isUnsupportedContext(String context) {
            return PackageDexUsage.UNSUPPORTED_CLASS_LOADER_CONTEXT.equals(context);
        }

        public boolean isUsedByOtherApps() {
            return this.mIsUsedByOtherApps;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public int getOwnerUserId() {
            return this.mOwnerUserId;
        }

        public Set<String> getLoaderIsas() {
            return this.mLoaderIsas;
        }

        public Set<String> getLoadingPackages() {
            return this.mLoadingPackages;
        }

        public String getClassLoaderContext() {
            return this.mClassLoaderContext;
        }

        public boolean isUnsupportedClassLoaderContext() {
            return isUnsupportedContext(this.mClassLoaderContext);
        }

        public boolean isVariableClassLoaderContext() {
            return PackageDexUsage.VARIABLE_CLASS_LOADER_CONTEXT.equals(this.mClassLoaderContext);
        }
    }
}
