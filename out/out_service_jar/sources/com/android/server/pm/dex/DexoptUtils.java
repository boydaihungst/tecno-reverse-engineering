package com.android.server.pm.dex;

import android.content.pm.SharedLibraryInfo;
import android.util.Slog;
import android.util.SparseArray;
import com.android.internal.os.ClassLoaderFactory;
import com.android.internal.util.ArrayUtils;
import com.android.server.pm.parsing.pkg.AndroidPackage;
import java.io.File;
import java.util.List;
/* loaded from: classes2.dex */
public final class DexoptUtils {
    private static final String SHARED_LIBRARY_LOADER_TYPE = ClassLoaderFactory.getPathClassLoaderName();
    private static final String TAG = "DexoptUtils";

    private DexoptUtils() {
    }

    public static String[] getClassLoaderContexts(AndroidPackage pkg, List<SharedLibraryInfo> sharedLibraries, boolean[] pathsWithCode) {
        String str;
        String sharedLibrariesContext = "";
        if (sharedLibraries != null) {
            sharedLibrariesContext = encodeSharedLibraries(sharedLibraries);
        }
        String baseApkContextClassLoader = encodeClassLoader("", pkg.getClassLoaderName(), sharedLibrariesContext);
        if (ArrayUtils.isEmpty(pkg.getSplitCodePaths())) {
            return new String[]{baseApkContextClassLoader};
        }
        String[] splitRelativeCodePaths = getSplitRelativeCodePaths(pkg);
        String baseApkName = new File(pkg.getBaseApkPath()).getName();
        String[] classLoaderContexts = new String[splitRelativeCodePaths.length + 1];
        classLoaderContexts[0] = pathsWithCode[0] ? baseApkContextClassLoader : null;
        SparseArray<int[]> splitDependencies = pkg.getSplitDependencies();
        if (!pkg.isIsolatedSplitLoading() || splitDependencies == null || splitDependencies.size() == 0) {
            String classpath = baseApkName;
            for (int i = 1; i < classLoaderContexts.length; i++) {
                if (pathsWithCode[i]) {
                    classLoaderContexts[i] = encodeClassLoader(classpath, pkg.getClassLoaderName(), sharedLibrariesContext);
                } else {
                    classLoaderContexts[i] = null;
                }
                classpath = encodeClasspath(classpath, splitRelativeCodePaths[i - 1]);
            }
        } else {
            String[] splitClassLoaderEncodingCache = new String[splitRelativeCodePaths.length];
            for (int i2 = 0; i2 < splitRelativeCodePaths.length; i2++) {
                splitClassLoaderEncodingCache[i2] = encodeClassLoader(splitRelativeCodePaths[i2], pkg.getSplitClassLoaderNames()[i2]);
            }
            String splitDependencyOnBase = encodeClassLoader(baseApkName, pkg.getClassLoaderName());
            for (int i3 = 1; i3 < splitDependencies.size(); i3++) {
                int splitIndex = splitDependencies.keyAt(i3);
                if (pathsWithCode[splitIndex]) {
                    getParentDependencies(splitIndex, splitClassLoaderEncodingCache, splitDependencies, classLoaderContexts, splitDependencyOnBase);
                }
            }
            for (int i4 = 1; i4 < classLoaderContexts.length; i4++) {
                String splitClassLoader = encodeClassLoader("", pkg.getSplitClassLoaderNames()[i4 - 1]);
                if (pathsWithCode[i4]) {
                    if (classLoaderContexts[i4] == null) {
                        str = splitClassLoader;
                    } else {
                        str = encodeClassLoaderChain(splitClassLoader, classLoaderContexts[i4]) + sharedLibrariesContext;
                    }
                    classLoaderContexts[i4] = str;
                } else {
                    classLoaderContexts[i4] = null;
                }
            }
        }
        return classLoaderContexts;
    }

    public static String getClassLoaderContext(SharedLibraryInfo info) {
        String sharedLibrariesContext = "";
        if (info.getDependencies() != null) {
            sharedLibrariesContext = encodeSharedLibraries(info.getDependencies());
        }
        return encodeClassLoader("", SHARED_LIBRARY_LOADER_TYPE, sharedLibrariesContext);
    }

    private static String getParentDependencies(int index, String[] splitClassLoaderEncodingCache, SparseArray<int[]> splitDependencies, String[] classLoaderContexts, String splitDependencyOnBase) {
        String splitContext;
        if (index == 0) {
            return splitDependencyOnBase;
        }
        if (classLoaderContexts[index] != null) {
            return classLoaderContexts[index];
        }
        int parent = splitDependencies.get(index)[0];
        String parentDependencies = getParentDependencies(parent, splitClassLoaderEncodingCache, splitDependencies, classLoaderContexts, splitDependencyOnBase);
        if (parent == 0) {
            splitContext = parentDependencies;
        } else {
            splitContext = encodeClassLoaderChain(splitClassLoaderEncodingCache[parent - 1], parentDependencies);
        }
        classLoaderContexts[index] = splitContext;
        return splitContext;
    }

    private static String encodeSharedLibrary(SharedLibraryInfo sharedLibrary) {
        List<String> paths = sharedLibrary.getAllCodePaths();
        String classLoaderSpec = encodeClassLoader(encodeClasspath((String[]) paths.toArray(new String[paths.size()])), SHARED_LIBRARY_LOADER_TYPE);
        if (sharedLibrary.getDependencies() != null) {
            return classLoaderSpec + encodeSharedLibraries(sharedLibrary.getDependencies());
        }
        return classLoaderSpec;
    }

    private static String encodeSharedLibraries(List<SharedLibraryInfo> sharedLibraries) {
        String sharedLibrariesContext = "{";
        boolean first = true;
        for (SharedLibraryInfo info : sharedLibraries) {
            if (!first) {
                sharedLibrariesContext = sharedLibrariesContext + "#";
            }
            first = false;
            sharedLibrariesContext = sharedLibrariesContext + encodeSharedLibrary(info);
        }
        return sharedLibrariesContext + "}";
    }

    private static String encodeClasspath(String[] classpathElements) {
        if (classpathElements == null || classpathElements.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (String element : classpathElements) {
            if (sb.length() != 0) {
                sb.append(":");
            }
            sb.append(element);
        }
        return sb.toString();
    }

    private static String encodeClasspath(String classpath, String newElement) {
        return classpath.isEmpty() ? newElement : classpath + ":" + newElement;
    }

    static String encodeClassLoader(String classpath, String classLoaderName) {
        classpath.getClass();
        String classLoaderDexoptEncoding = classLoaderName;
        if (ClassLoaderFactory.isPathClassLoaderName(classLoaderName)) {
            classLoaderDexoptEncoding = "PCL";
        } else if (ClassLoaderFactory.isDelegateLastClassLoaderName(classLoaderName)) {
            classLoaderDexoptEncoding = "DLC";
        } else {
            Slog.wtf(TAG, "Unsupported classLoaderName: " + classLoaderName);
        }
        return classLoaderDexoptEncoding + "[" + classpath + "]";
    }

    private static String encodeClassLoader(String classpath, String classLoaderName, String sharedLibraries) {
        return encodeClassLoader(classpath, classLoaderName) + sharedLibraries;
    }

    static String encodeClassLoaderChain(String cl1, String cl2) {
        return cl1.isEmpty() ? cl2 : cl2.isEmpty() ? cl1 : cl1 + ";" + cl2;
    }

    static String[] processContextForDexLoad(List<String> classLoadersNames, List<String> classPaths) {
        if (classLoadersNames.size() != classPaths.size()) {
            throw new IllegalArgumentException("The size of the class loader names and the dex paths do not match.");
        }
        if (classLoadersNames.isEmpty()) {
            throw new IllegalArgumentException("Empty classLoadersNames");
        }
        String parentContext = "";
        for (int i = 1; i < classLoadersNames.size(); i++) {
            if (!ClassLoaderFactory.isValidClassLoaderName(classLoadersNames.get(i)) || classPaths.get(i) == null) {
                return null;
            }
            String classpath = encodeClasspath(classPaths.get(i).split(File.pathSeparator));
            parentContext = encodeClassLoaderChain(parentContext, encodeClassLoader(classpath, classLoadersNames.get(i)));
        }
        String loadingClassLoader = classLoadersNames.get(0);
        if (ClassLoaderFactory.isValidClassLoaderName(loadingClassLoader)) {
            String[] loadedDexPaths = classPaths.get(0).split(File.pathSeparator);
            String[] loadedDexPathsContext = new String[loadedDexPaths.length];
            String currentLoadedDexPathClasspath = "";
            for (int i2 = 0; i2 < loadedDexPaths.length; i2++) {
                String dexPath = loadedDexPaths[i2];
                String currentContext = encodeClassLoader(currentLoadedDexPathClasspath, loadingClassLoader);
                loadedDexPathsContext[i2] = encodeClassLoaderChain(currentContext, parentContext);
                currentLoadedDexPathClasspath = encodeClasspath(currentLoadedDexPathClasspath, dexPath);
            }
            return loadedDexPathsContext;
        }
        return null;
    }

    private static String[] getSplitRelativeCodePaths(AndroidPackage pkg) {
        String baseCodePath = new File(pkg.getBaseApkPath()).getParent();
        String[] splitCodePaths = pkg.getSplitCodePaths();
        String[] splitRelativeCodePaths = new String[ArrayUtils.size(splitCodePaths)];
        for (int i = 0; i < splitRelativeCodePaths.length; i++) {
            File pathFile = new File(splitCodePaths[i]);
            splitRelativeCodePaths[i] = pathFile.getName();
            String basePath = pathFile.getParent();
            if (!basePath.equals(baseCodePath)) {
                Slog.wtf(TAG, "Split paths have different base paths: " + basePath + " and " + baseCodePath);
            }
        }
        return splitRelativeCodePaths;
    }
}
