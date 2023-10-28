package com.android.server.am;

import android.content.pm.VersionedPackage;
import android.util.ArrayMap;
import com.android.internal.app.procstats.ProcessStats;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
/* loaded from: classes.dex */
public final class PackageList {
    private final ArrayMap<String, ProcessStats.ProcessStateHolder> mPkgList = new ArrayMap<>();
    private final ProcessRecord mProcess;

    /* JADX INFO: Access modifiers changed from: package-private */
    public PackageList(ProcessRecord app) {
        this.mProcess = app;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ProcessStats.ProcessStateHolder put(String key, ProcessStats.ProcessStateHolder value) {
        ProcessStats.ProcessStateHolder put;
        synchronized (this) {
            this.mProcess.getWindowProcessController().addPackage(key);
            put = this.mPkgList.put(key, value);
        }
        return put;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void clear() {
        synchronized (this) {
            this.mPkgList.clear();
            this.mProcess.getWindowProcessController().clearPackageList();
        }
    }

    public int size() {
        int size;
        synchronized (this) {
            size = this.mPkgList.size();
        }
        return size;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean containsKey(Object key) {
        boolean containsKey;
        synchronized (this) {
            containsKey = this.mPkgList.containsKey(key);
        }
        return containsKey;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ProcessStats.ProcessStateHolder get(String pkgName) {
        ProcessStats.ProcessStateHolder processStateHolder;
        synchronized (this) {
            processStateHolder = this.mPkgList.get(pkgName);
        }
        return processStateHolder;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void forEachPackage(Consumer<String> callback) {
        synchronized (this) {
            int size = this.mPkgList.size();
            for (int i = 0; i < size; i++) {
                callback.accept(this.mPkgList.keyAt(i));
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void forEachPackage(BiConsumer<String, ProcessStats.ProcessStateHolder> callback) {
        synchronized (this) {
            int size = this.mPkgList.size();
            for (int i = 0; i < size; i++) {
                callback.accept(this.mPkgList.keyAt(i), this.mPkgList.valueAt(i));
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public <R> R searchEachPackage(Function<String, R> callback) {
        synchronized (this) {
            int size = this.mPkgList.size();
            for (int i = 0; i < size; i++) {
                R r = callback.apply(this.mPkgList.keyAt(i));
                if (r != null) {
                    return r;
                }
            }
            return null;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void forEachPackageProcessStats(Consumer<ProcessStats.ProcessStateHolder> callback) {
        synchronized (this) {
            int size = this.mPkgList.size();
            for (int i = 0; i < size; i++) {
                callback.accept(this.mPkgList.valueAt(i));
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ArrayMap<String, ProcessStats.ProcessStateHolder> getPackageListLocked() {
        return this.mPkgList;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public String[] getPackageList() {
        synchronized (this) {
            int size = this.mPkgList.size();
            if (size == 0) {
                return null;
            }
            String[] list = new String[size];
            for (int i = 0; i < size; i++) {
                list[i] = this.mPkgList.keyAt(i);
            }
            return list;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public List<VersionedPackage> getPackageListWithVersionCode() {
        synchronized (this) {
            int size = this.mPkgList.size();
            if (size == 0) {
                return null;
            }
            List<VersionedPackage> list = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                list.add(new VersionedPackage(this.mPkgList.keyAt(i), this.mPkgList.valueAt(i).appVersion));
            }
            return list;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dump(PrintWriter pw, String prefix) {
        synchronized (this) {
            pw.print(prefix);
            pw.print("packageList={");
            int size = this.mPkgList.size();
            for (int i = 0; i < size; i++) {
                if (i > 0) {
                    pw.print(", ");
                }
                pw.print(this.mPkgList.keyAt(i));
            }
            pw.println("}");
        }
    }

    public String keyAt(int index) {
        String keyAt;
        synchronized (this) {
            keyAt = this.mPkgList.keyAt(index);
        }
        return keyAt;
    }
}
