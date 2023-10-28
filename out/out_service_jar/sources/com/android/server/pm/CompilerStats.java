package com.android.server.pm;

import android.util.ArrayMap;
import android.util.AtomicFile;
import android.util.Log;
import com.android.internal.util.FastPrintWriter;
import com.android.internal.util.IndentingPrintWriter;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import libcore.io.IoUtils;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class CompilerStats extends AbstractStatsBase<Void> {
    private static final int COMPILER_STATS_VERSION = 1;
    private static final String COMPILER_STATS_VERSION_HEADER = "PACKAGE_MANAGER__COMPILER_STATS__";
    private final Map<String, PackageStats> packageStats;

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public static class PackageStats {
        private final Map<String, Long> compileTimePerCodePath = new ArrayMap(2);
        private final String packageName;

        public PackageStats(String packageName) {
            this.packageName = packageName;
        }

        public String getPackageName() {
            return this.packageName;
        }

        public long getCompileTime(String codePath) {
            String storagePath = getStoredPathFromCodePath(codePath);
            synchronized (this.compileTimePerCodePath) {
                Long l = this.compileTimePerCodePath.get(storagePath);
                if (l == null) {
                    return 0L;
                }
                return l.longValue();
            }
        }

        public void setCompileTime(String codePath, long compileTimeInMs) {
            String storagePath = getStoredPathFromCodePath(codePath);
            synchronized (this.compileTimePerCodePath) {
                if (compileTimeInMs <= 0) {
                    this.compileTimePerCodePath.remove(storagePath);
                } else {
                    this.compileTimePerCodePath.put(storagePath, Long.valueOf(compileTimeInMs));
                }
            }
        }

        private static String getStoredPathFromCodePath(String codePath) {
            int lastSlash = codePath.lastIndexOf(File.separatorChar);
            return codePath.substring(lastSlash + 1);
        }

        public void dump(IndentingPrintWriter ipw) {
            synchronized (this.compileTimePerCodePath) {
                if (this.compileTimePerCodePath.size() == 0) {
                    ipw.println("(No recorded stats)");
                } else {
                    for (Map.Entry<String, Long> e : this.compileTimePerCodePath.entrySet()) {
                        ipw.println(" " + e.getKey() + " - " + e.getValue());
                    }
                }
            }
        }
    }

    public CompilerStats() {
        super("package-cstats.list", "CompilerStats_DiskWriter", false);
        this.packageStats = new HashMap();
    }

    public PackageStats getPackageStats(String packageName) {
        PackageStats packageStats;
        synchronized (this.packageStats) {
            packageStats = this.packageStats.get(packageName);
        }
        return packageStats;
    }

    public void setPackageStats(String packageName, PackageStats stats) {
        synchronized (this.packageStats) {
            this.packageStats.put(packageName, stats);
        }
    }

    public PackageStats createPackageStats(String packageName) {
        PackageStats newStats;
        synchronized (this.packageStats) {
            newStats = new PackageStats(packageName);
            this.packageStats.put(packageName, newStats);
        }
        return newStats;
    }

    public PackageStats getOrCreatePackageStats(String packageName) {
        synchronized (this.packageStats) {
            PackageStats existingStats = this.packageStats.get(packageName);
            if (existingStats != null) {
                return existingStats;
            }
            return createPackageStats(packageName);
        }
    }

    public void deletePackageStats(String packageName) {
        synchronized (this.packageStats) {
            this.packageStats.remove(packageName);
        }
    }

    public void write(Writer out) {
        FastPrintWriter fpw = new FastPrintWriter(out);
        fpw.print(COMPILER_STATS_VERSION_HEADER);
        fpw.println(1);
        synchronized (this.packageStats) {
            for (PackageStats pkg : this.packageStats.values()) {
                synchronized (pkg.compileTimePerCodePath) {
                    if (!pkg.compileTimePerCodePath.isEmpty()) {
                        fpw.println(pkg.getPackageName());
                        for (Map.Entry<String, Long> e : pkg.compileTimePerCodePath.entrySet()) {
                            fpw.println("-" + e.getKey() + ":" + e.getValue());
                        }
                    }
                }
            }
        }
        fpw.flush();
    }

    /* JADX WARN: Code restructure failed: missing block: B:21:0x0078, code lost:
        throw new java.lang.IllegalArgumentException("Could not parse data " + r6);
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public boolean read(Reader r) {
        synchronized (this.packageStats) {
            this.packageStats.clear();
            try {
                BufferedReader in = new BufferedReader(r);
                String versionLine = in.readLine();
                if (versionLine == null) {
                    throw new IllegalArgumentException("No version line found.");
                }
                if (!versionLine.startsWith(COMPILER_STATS_VERSION_HEADER)) {
                    throw new IllegalArgumentException("Invalid version line: " + versionLine);
                }
                int version = Integer.parseInt(versionLine.substring(COMPILER_STATS_VERSION_HEADER.length()));
                if (version != 1) {
                    throw new IllegalArgumentException("Unexpected version: " + version);
                }
                PackageStats currentPackage = new PackageStats("fake package");
                while (true) {
                    String s = in.readLine();
                    if (s != null) {
                        if (s.startsWith("-")) {
                            int colonIndex = s.indexOf(58);
                            if (colonIndex == -1 || colonIndex == 1) {
                                break;
                            }
                            String codePath = s.substring(1, colonIndex);
                            long time = Long.parseLong(s.substring(colonIndex + 1));
                            currentPackage.setCompileTime(codePath, time);
                        } else {
                            currentPackage = getOrCreatePackageStats(s);
                        }
                    }
                }
            } catch (Exception e) {
                Log.e("PackageManager", "Error parsing compiler stats", e);
                return false;
            }
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void writeNow() {
        writeNow(null);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean maybeWriteAsync() {
        return maybeWriteAsync(null);
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
            Log.e("PackageManager", "Failed to write compiler stats", e);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void read() {
        read((CompilerStats) null);
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.pm.AbstractStatsBase
    public void readInternal(Void data) {
        AtomicFile file = getFile();
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(file.openRead()));
            read((Reader) in);
        } catch (FileNotFoundException e) {
        } catch (Throwable th) {
            IoUtils.closeQuietly(in);
            throw th;
        }
        IoUtils.closeQuietly(in);
    }
}
