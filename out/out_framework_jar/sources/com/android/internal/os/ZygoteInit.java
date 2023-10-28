package com.android.internal.os;

import android.app.ApplicationLoaders;
import android.content.pm.SharedLibraryInfo;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.inputmethodservice.navigationbar.NavigationBarInflaterView;
import android.media.MediaMetrics;
import android.os.Build;
import android.os.Environment;
import android.os.IInstalld;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.Trace;
import android.os.UserHandle;
import android.os.ZygoteProcess;
import android.security.keystore2.AndroidKeyStoreProvider;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.system.StructCapUserData;
import android.system.StructCapUserHeader;
import android.text.Hyphenator;
import android.text.TextUtils;
import android.util.Log;
import android.util.Slog;
import android.util.TimingsTraceLog;
import android.webkit.WebViewFactory;
import android.widget.TextView;
import com.android.internal.R;
import com.android.internal.os.RuntimeInit;
import com.android.internal.util.Preconditions;
import dalvik.system.VMRuntime;
import dalvik.system.ZygoteHooks;
import java.io.BufferedReader;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.Provider;
import java.security.Security;
import libcore.io.IoUtils;
/* loaded from: classes4.dex */
public class ZygoteInit {
    private static final String ABI_LIST_ARG = "--abi-list=";
    private static final int LOG_BOOT_PROGRESS_PRELOAD_END = 3030;
    private static final int LOG_BOOT_PROGRESS_PRELOAD_START = 3020;
    private static final String PRELOADED_CLASSES = "/system/etc/preloaded-classes";
    private static final boolean PRELOAD_RESOURCES = true;
    private static final String PROPERTY_DISABLE_GRAPHICS_DRIVER_PRELOADING = "ro.zygote.disable_gl_preload";
    private static final int ROOT_GID = 0;
    private static final int ROOT_UID = 0;
    private static final String SOCKET_NAME_ARG = "--socket-name=";
    private static final int UNPRIVILEGED_GID = 9999;
    private static final int UNPRIVILEGED_UID = 9999;
    private static Resources mResources;
    private static boolean sPreloadComplete;
    private static final String TAG = "Zygote";
    private static final boolean LOGGING_DEBUG = Log.isLoggable(TAG, 3);
    private static boolean sMtprofDisable = false;
    private static ClassLoader sCachedSystemServerClassLoader = null;

    private static native void nativePreloadAppProcessHALs();

    static native void nativePreloadGraphicsDriver();

    private static native void nativeZygoteInit();

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [145=4] */
    private static void addBootEvent(String bootevent) {
        if (sMtprofDisable || Build.HARDWARE.equals("cutf_cvm")) {
            return;
        }
        FileOutputStream fos = null;
        try {
            try {
                try {
                    fos = new FileOutputStream("/proc/bootprof");
                    fos.write(bootevent.getBytes());
                    fos.flush();
                    fos.close();
                } catch (Throwable th) {
                    if (fos != null) {
                        try {
                            fos.close();
                        } catch (IOException e) {
                            Log.e("BOOTPROF", "Failure close /proc/bootprof entry", e);
                        }
                    }
                    throw th;
                }
            } catch (FileNotFoundException e2) {
                Log.e("BOOTPROF", "Failure open /proc/bootprof, not found!", e2);
                if (fos == null) {
                    return;
                }
                fos.close();
            } catch (IOException e3) {
                Log.e("BOOTPROF", "Failure open /proc/bootprof entry", e3);
                if (fos == null) {
                    return;
                }
                fos.close();
            }
        } catch (IOException e4) {
            Log.e("BOOTPROF", "Failure close /proc/bootprof entry", e4);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void preload(TimingsTraceLog bootTimingsTraceLog) {
        Log.d(TAG, "begin preload");
        bootTimingsTraceLog.traceBegin("BeginPreload");
        beginPreload();
        bootTimingsTraceLog.traceEnd();
        bootTimingsTraceLog.traceBegin("PreloadClasses");
        preloadClasses();
        bootTimingsTraceLog.traceEnd();
        bootTimingsTraceLog.traceBegin("CacheNonBootClasspathClassLoaders");
        cacheNonBootClasspathClassLoaders();
        bootTimingsTraceLog.traceEnd();
        bootTimingsTraceLog.traceBegin("PreloadResources");
        preloadResources();
        bootTimingsTraceLog.traceEnd();
        Trace.traceBegin(16384L, "PreloadAppProcessHALs");
        nativePreloadAppProcessHALs();
        Trace.traceEnd(16384L);
        Trace.traceBegin(16384L, "PreloadGraphicsDriver");
        maybePreloadGraphicsDriver();
        Trace.traceEnd(16384L);
        preloadSharedLibraries();
        preloadTextResources();
        WebViewFactory.prepareWebViewInZygote();
        endPreload();
        warmUpJcaProviders();
        Log.d(TAG, "end preload");
        sPreloadComplete = true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void lazyPreload() {
        Preconditions.checkState(!sPreloadComplete);
        Log.i(TAG, "Lazily preloading resources.");
        preload(new TimingsTraceLog("ZygoteInitTiming_lazy", 16384L));
    }

    private static void beginPreload() {
        Log.i(TAG, "Calling ZygoteHooks.beginPreload()");
        ZygoteHooks.onBeginPreload();
    }

    private static void endPreload() {
        ZygoteHooks.onEndPreload();
        Log.i(TAG, "Called ZygoteHooks.endPreload()");
    }

    private static void preloadSharedLibraries() {
        Log.i(TAG, "Preloading shared libraries...");
        System.loadLibrary("android");
        System.loadLibrary("compiler_rt");
        System.loadLibrary("jnigraphics");
    }

    private static void maybePreloadGraphicsDriver() {
        if (!SystemProperties.getBoolean(PROPERTY_DISABLE_GRAPHICS_DRIVER_PRELOADING, false)) {
            nativePreloadGraphicsDriver();
        }
    }

    private static void preloadTextResources() {
        Hyphenator.init();
        TextView.preloadFontCache();
    }

    private static void warmUpJcaProviders() {
        Provider[] providers;
        long startTime = SystemClock.uptimeMillis();
        Trace.traceBegin(16384L, "Starting installation of AndroidKeyStoreProvider");
        AndroidKeyStoreProvider.install();
        Log.i(TAG, "Installed AndroidKeyStoreProvider in " + (SystemClock.uptimeMillis() - startTime) + "ms.");
        Trace.traceEnd(16384L);
        long startTime2 = SystemClock.uptimeMillis();
        Trace.traceBegin(16384L, "Starting warm up of JCA providers");
        for (Provider p : Security.getProviders()) {
            p.warmUpServiceProvision();
        }
        Log.i(TAG, "Warmed up JCA providers in " + (SystemClock.uptimeMillis() - startTime2) + "ms.");
        Trace.traceEnd(16384L);
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [384=5, 403=6, 367=5, 370=7] */
    /* JADX DEBUG: Finally have unexpected throw blocks count: 3, expect 1 */
    /* JADX WARN: Removed duplicated region for block: B:104:0x02b2  */
    /* JADX WARN: Removed duplicated region for block: B:105:0x02b8  */
    /* JADX WARN: Removed duplicated region for block: B:108:0x02bf  */
    /* JADX WARN: Removed duplicated region for block: B:110:0x02cc  */
    /* JADX WARN: Removed duplicated region for block: B:122:0x0320  */
    /* JADX WARN: Removed duplicated region for block: B:125:0x032a  */
    /* JADX WARN: Removed duplicated region for block: B:127:0x0337  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private static void preloadClasses() {
        String str;
        String str2;
        String str3;
        String str4;
        String str5;
        int count;
        long startTime;
        String prop;
        String prop2;
        BufferedReader br;
        int missingLambdaCount;
        String str6 = "ResetJitCounters";
        String str7 = "dalvik.vm.profilebootclasspath";
        VMRuntime runtime = VMRuntime.getRuntime();
        try {
            InputStream is = new FileInputStream(PRELOADED_CLASSES);
            Log.i(TAG, "Preloading classes...");
            long startTime2 = SystemClock.uptimeMillis();
            int reuid = Os.getuid();
            int regid = Os.getgid();
            boolean droppedPriviliges = false;
            String str8 = "ms";
            if (reuid == 0 && regid == 0) {
                try {
                    Os.setregid(0, 9999);
                    Os.setreuid(0, 9999);
                    droppedPriviliges = true;
                } catch (ErrnoException ex) {
                    throw new RuntimeException("Failed to drop root", ex);
                }
            }
            try {
                br = new BufferedReader(new InputStreamReader(is), 256);
                missingLambdaCount = 0;
                count = 0;
            } catch (IOException e) {
                e = e;
                str = "ResetJitCounters";
                str2 = "dalvik.vm.profilebootclasspath";
                str3 = "Zygote:Preload ";
                str4 = "Failed to restore root";
                count = 0;
            } catch (Throwable th) {
                th = th;
                str = "ResetJitCounters";
                str2 = "dalvik.vm.profilebootclasspath";
                str3 = "Zygote:Preload ";
                str4 = "Failed to restore root";
                str5 = str8;
                count = 0;
                startTime = startTime2;
            }
            while (true) {
                try {
                    String line = br.readLine();
                    if (line == null) {
                        break;
                    }
                    try {
                        String line2 = line.trim();
                        BufferedReader br2 = br;
                        if (line2.startsWith("#") || line2.equals("")) {
                            br = br2;
                            str6 = str6;
                            str7 = str7;
                        } else {
                            String str9 = str6;
                            String str10 = str7;
                            try {
                                Trace.traceBegin(16384L, line2);
                                try {
                                    Class.forName(line2, true, null);
                                    count++;
                                } catch (ClassNotFoundException e2) {
                                    if (!line2.contains("$$Lambda$")) {
                                        Log.w(TAG, "Class not found for preloading: " + line2);
                                    } else if (LOGGING_DEBUG) {
                                        missingLambdaCount++;
                                    }
                                } catch (UnsatisfiedLinkError e3) {
                                    Log.w(TAG, "Problem preloading " + line2 + ": " + e3);
                                } catch (Throwable t) {
                                    Log.e(TAG, "Error preloading " + line2 + MediaMetrics.SEPARATOR, t);
                                    if (t instanceof Error) {
                                        throw ((Error) t);
                                    }
                                    if (!(t instanceof RuntimeException)) {
                                        throw new RuntimeException(t);
                                    }
                                    throw ((RuntimeException) t);
                                }
                                Trace.traceEnd(16384L);
                                br = br2;
                                str6 = str9;
                                str7 = str10;
                            } catch (IOException e4) {
                                e = e4;
                                str3 = "Zygote:Preload ";
                                str4 = "Failed to restore root";
                                str = str9;
                                str2 = str10;
                                startTime = startTime2;
                                try {
                                    Log.e(TAG, "Error reading /system/etc/preloaded-classes.", e);
                                    IoUtils.closeQuietly(is);
                                    Trace.traceBegin(16384L, "PreloadDexCaches");
                                    runtime.preloadDexCaches();
                                    Trace.traceEnd(16384L);
                                    prop2 = SystemProperties.get("persist.device_config.runtime_native_boot.profilebootclasspath", "");
                                    if ("true".equals(prop2.length() != 0 ? SystemProperties.get(str2, "") : prop2)) {
                                    }
                                    if (droppedPriviliges) {
                                    }
                                    addBootEvent(str3 + count + " classes in " + (SystemClock.uptimeMillis() - startTime) + str8);
                                } catch (Throwable th2) {
                                    th = th2;
                                    str5 = str8;
                                }
                            } catch (Throwable th3) {
                                th = th3;
                                str5 = str8;
                                str3 = "Zygote:Preload ";
                                str4 = "Failed to restore root";
                                str = str9;
                                str2 = str10;
                                startTime = startTime2;
                            }
                        }
                    } catch (IOException e5) {
                        e = e5;
                        str = str6;
                        str2 = str7;
                        str3 = "Zygote:Preload ";
                        str4 = "Failed to restore root";
                    } catch (Throwable th4) {
                        th = th4;
                        str = str6;
                        str2 = str7;
                        str5 = str8;
                        str3 = "Zygote:Preload ";
                        str4 = "Failed to restore root";
                        startTime = startTime2;
                    }
                } catch (IOException e6) {
                    e = e6;
                    str = str6;
                    str2 = str7;
                    str3 = "Zygote:Preload ";
                    str4 = "Failed to restore root";
                } catch (Throwable th5) {
                    th = th5;
                    str = str6;
                    str2 = str7;
                    str3 = "Zygote:Preload ";
                    str4 = "Failed to restore root";
                }
                IoUtils.closeQuietly(is);
                Trace.traceBegin(16384L, "PreloadDexCaches");
                runtime.preloadDexCaches();
                Trace.traceEnd(16384L);
                prop = SystemProperties.get("persist.device_config.runtime_native_boot.profilebootclasspath", "");
                if (prop.length() == 0) {
                    prop = SystemProperties.get(str2, "");
                }
                if ("true".equals(prop)) {
                    Trace.traceBegin(16384L, str);
                    VMRuntime.resetJitCounters();
                    Trace.traceEnd(16384L);
                }
                if (droppedPriviliges) {
                    try {
                        Os.setreuid(0, 0);
                        Os.setregid(0, 0);
                    } catch (ErrnoException ex2) {
                        throw new RuntimeException(str4, ex2);
                    }
                }
                addBootEvent(str3 + count + " classes in " + (SystemClock.uptimeMillis() - startTime) + str5);
                throw th;
            }
            String str11 = str6;
            String str12 = str7;
            try {
                try {
                    Log.i(TAG, "...preloaded " + count + " classes in " + (SystemClock.uptimeMillis() - startTime2) + "ms.");
                    try {
                        if (LOGGING_DEBUG && missingLambdaCount != 0) {
                            Log.i(TAG, "Unresolved lambda preloads: " + missingLambdaCount);
                        }
                        IoUtils.closeQuietly(is);
                        Trace.traceBegin(16384L, "PreloadDexCaches");
                        runtime.preloadDexCaches();
                        Trace.traceEnd(16384L);
                        String prop3 = SystemProperties.get("persist.device_config.runtime_native_boot.profilebootclasspath", "");
                        if ("true".equals(prop3.length() == 0 ? SystemProperties.get(str12, "") : prop3)) {
                            Trace.traceBegin(16384L, str11);
                            VMRuntime.resetJitCounters();
                            Trace.traceEnd(16384L);
                        }
                        if (droppedPriviliges) {
                            try {
                                Os.setreuid(0, 0);
                                Os.setregid(0, 0);
                            } catch (ErrnoException ex3) {
                                throw new RuntimeException("Failed to restore root", ex3);
                            }
                        }
                        addBootEvent("Zygote:Preload " + count + " classes in " + (SystemClock.uptimeMillis() - startTime2) + str8);
                    } catch (Throwable th6) {
                        th = th6;
                        str3 = "Zygote:Preload ";
                        str4 = "Failed to restore root";
                        str = str11;
                        str2 = str12;
                        startTime = startTime2;
                        str5 = str8;
                    }
                } catch (Throwable th7) {
                    th = th7;
                    str3 = "Zygote:Preload ";
                    str4 = "Failed to restore root";
                    str = str11;
                    str2 = str12;
                    str5 = str8;
                    startTime = startTime2;
                    count = count;
                    IoUtils.closeQuietly(is);
                    Trace.traceBegin(16384L, "PreloadDexCaches");
                    runtime.preloadDexCaches();
                    Trace.traceEnd(16384L);
                    prop = SystemProperties.get("persist.device_config.runtime_native_boot.profilebootclasspath", "");
                    if (prop.length() == 0) {
                    }
                    if ("true".equals(prop)) {
                    }
                    if (droppedPriviliges) {
                    }
                    addBootEvent(str3 + count + " classes in " + (SystemClock.uptimeMillis() - startTime) + str5);
                    throw th;
                }
            } catch (IOException e7) {
                e = e7;
                str3 = "Zygote:Preload ";
                str4 = "Failed to restore root";
                str = str11;
                str2 = str12;
                str8 = str8;
                count = count;
                startTime = startTime2;
                Log.e(TAG, "Error reading /system/etc/preloaded-classes.", e);
                IoUtils.closeQuietly(is);
                Trace.traceBegin(16384L, "PreloadDexCaches");
                runtime.preloadDexCaches();
                Trace.traceEnd(16384L);
                prop2 = SystemProperties.get("persist.device_config.runtime_native_boot.profilebootclasspath", "");
                if ("true".equals(prop2.length() != 0 ? SystemProperties.get(str2, "") : prop2)) {
                    Trace.traceBegin(16384L, str);
                    VMRuntime.resetJitCounters();
                    Trace.traceEnd(16384L);
                }
                if (droppedPriviliges) {
                    try {
                        Os.setreuid(0, 0);
                        Os.setregid(0, 0);
                    } catch (ErrnoException ex4) {
                        throw new RuntimeException(str4, ex4);
                    }
                }
                addBootEvent(str3 + count + " classes in " + (SystemClock.uptimeMillis() - startTime) + str8);
            }
        } catch (FileNotFoundException e8) {
            Log.e(TAG, "Couldn't find /system/etc/preloaded-classes.");
        }
    }

    private static void cacheNonBootClasspathClassLoaders() {
        SharedLibraryInfo hidlBase = new SharedLibraryInfo("/system/framework/android.hidl.base-V1.0-java.jar", null, null, null, 0L, 0, null, null, null, false);
        SharedLibraryInfo hidlManager = new SharedLibraryInfo("/system/framework/android.hidl.manager-V1.0-java.jar", null, null, null, 0L, 0, null, null, null, false);
        SharedLibraryInfo androidTestBase = new SharedLibraryInfo("/system/framework/android.test.base.jar", null, null, null, 0L, 0, null, null, null, false);
        ApplicationLoaders.getDefault().createAndCacheNonBootclasspathSystemClassLoaders(new SharedLibraryInfo[]{hidlBase, hidlManager, androidTestBase});
    }

    private static void preloadResources() {
        try {
            Resources system = Resources.getSystem();
            mResources = system;
            system.startPreloading();
            Log.i(TAG, "Preloading resources...");
            long startTime = SystemClock.uptimeMillis();
            TypedArray ar = mResources.obtainTypedArray(R.array.preloaded_drawables);
            int N = preloadDrawables(ar);
            ar.recycle();
            Log.i(TAG, "...preloaded " + N + " resources in " + (SystemClock.uptimeMillis() - startTime) + "ms.");
            addBootEvent("Zygote:Preload " + N + " obtain resources in " + (SystemClock.uptimeMillis() - startTime) + "ms");
            long startTime2 = SystemClock.uptimeMillis();
            TypedArray ar2 = mResources.obtainTypedArray(R.array.preloaded_color_state_lists);
            int N2 = preloadColorStateLists(ar2);
            ar2.recycle();
            Log.i(TAG, "...preloaded " + N2 + " resources in " + (SystemClock.uptimeMillis() - startTime2) + "ms.");
            if (mResources.getBoolean(R.bool.config_freeformWindowManagement)) {
                startTime2 = SystemClock.uptimeMillis();
                TypedArray ar3 = mResources.obtainTypedArray(R.array.preloaded_freeform_multi_window_drawables);
                N2 = preloadDrawables(ar3);
                ar3.recycle();
                Log.i(TAG, "...preloaded " + N2 + " resource in " + (SystemClock.uptimeMillis() - startTime2) + "ms.");
            }
            addBootEvent("Zygote:Preload " + N2 + " resources in " + (SystemClock.uptimeMillis() - startTime2) + "ms");
            mResources.finishPreloading();
        } catch (RuntimeException e) {
            Log.w(TAG, "Failure preloading resources", e);
        }
    }

    private static int preloadColorStateLists(TypedArray ar) {
        int N = ar.length();
        for (int i = 0; i < N; i++) {
            int id = ar.getResourceId(i, 0);
            if (id != 0 && mResources.getColorStateList(id, null) == null) {
                throw new IllegalArgumentException("Unable to find preloaded color resource #0x" + Integer.toHexString(id) + " (" + ar.getString(i) + NavigationBarInflaterView.KEY_CODE_END);
            }
        }
        return N;
    }

    private static int preloadDrawables(TypedArray ar) {
        int N = ar.length();
        for (int i = 0; i < N; i++) {
            int id = ar.getResourceId(i, 0);
            if (id != 0 && mResources.getDrawable(id, null) == null) {
                throw new IllegalArgumentException("Unable to find preloaded drawable resource #0x" + Integer.toHexString(id) + " (" + ar.getString(i) + NavigationBarInflaterView.KEY_CODE_END);
            }
        }
        return N;
    }

    private static void gcAndFinalize() {
        ZygoteHooks.gcAndFinalize();
    }

    private static boolean shouldProfileSystemServer() {
        boolean defaultValue = SystemProperties.getBoolean("dalvik.vm.profilesystemserver", false);
        return SystemProperties.getBoolean("persist.device_config.runtime_native_boot.profilesystemserver", defaultValue);
    }

    private static Runnable handleSystemServerProcess(ZygoteArguments parsedArgs) {
        Os.umask(OsConstants.S_IRWXG | OsConstants.S_IRWXO);
        if (parsedArgs.mNiceName != null) {
            Process.setArgV0(parsedArgs.mNiceName);
        }
        String systemServerClasspath = Os.getenv("SYSTEMSERVERCLASSPATH");
        if (systemServerClasspath != null && shouldProfileSystemServer() && (Build.IS_USERDEBUG || Build.IS_ENG)) {
            try {
                Log.d(TAG, "Preparing system server profile");
                prepareSystemServerProfile(systemServerClasspath);
            } catch (Exception e) {
                Log.wtf(TAG, "Failed to set up system server profile", e);
            }
        }
        if (parsedArgs.mInvokeWith != null) {
            String[] args = parsedArgs.mRemainingArgs;
            if (systemServerClasspath != null) {
                String[] amendedArgs = new String[args.length + 2];
                amendedArgs[0] = "-cp";
                amendedArgs[1] = systemServerClasspath;
                System.arraycopy(args, 0, amendedArgs, 2, args.length);
                args = amendedArgs;
            }
            WrapperInit.execApplication(parsedArgs.mInvokeWith, parsedArgs.mNiceName, parsedArgs.mTargetSdkVersion, VMRuntime.getCurrentInstructionSet(), null, args);
            throw new IllegalStateException("Unexpected return from WrapperInit.execApplication");
        }
        ClassLoader cl = getOrCreateSystemServerClassLoader();
        if (cl != null) {
            Thread.currentThread().setContextClassLoader(cl);
        }
        return zygoteInit(parsedArgs.mTargetSdkVersion, parsedArgs.mDisabledCompatChanges, parsedArgs.mRemainingArgs, cl);
    }

    private static ClassLoader getOrCreateSystemServerClassLoader() {
        String systemServerClasspath;
        if (sCachedSystemServerClassLoader == null && (systemServerClasspath = Os.getenv("SYSTEMSERVERCLASSPATH")) != null) {
            sCachedSystemServerClassLoader = createPathClassLoader(systemServerClasspath, 10000);
        }
        return sCachedSystemServerClassLoader;
    }

    private static void prefetchStandaloneSystemServerJars() {
        String[] split;
        String envStr = Os.getenv("STANDALONE_SYSTEMSERVER_JARS");
        if (TextUtils.isEmpty(envStr)) {
            return;
        }
        for (String jar : envStr.split(":")) {
            try {
                SystemServerClassLoaderFactory.createClassLoader(jar, getOrCreateSystemServerClassLoader());
            } catch (Error e) {
                Log.e(TAG, String.format("Failed to prefetch standalone system server jar \"%s\": %s", jar, e.toString()));
            }
        }
    }

    private static void prepareSystemServerProfile(String systemServerClasspath) throws RemoteException {
        if (systemServerClasspath.isEmpty()) {
            return;
        }
        String[] codePaths = systemServerClasspath.split(":");
        IInstalld installd = IInstalld.Stub.asInterface(ServiceManager.getService("installd"));
        installd.prepareAppProfile("android", 0, UserHandle.getAppId(1000), "primary.prof", codePaths[0], null);
        File curProfileDir = Environment.getDataProfilesDePackageDirectory(0, "android");
        String curProfilePath = new File(curProfileDir, "primary.prof").getAbsolutePath();
        File refProfileDir = Environment.getDataProfilesDePackageDirectory(0, "android");
        String refProfilePath = new File(refProfileDir, "primary.prof").getAbsolutePath();
        VMRuntime.registerAppInfo("android", curProfilePath, refProfilePath, codePaths, 1);
    }

    public static void setApiDenylistExemptions(String[] exemptions) {
        VMRuntime.getRuntime().setHiddenApiExemptions(exemptions);
    }

    public static void setHiddenApiAccessLogSampleRate(int percent) {
        VMRuntime.getRuntime().setHiddenApiAccessLogSamplingRate(percent);
    }

    public static void setHiddenApiUsageLogger(VMRuntime.HiddenApiUsageLogger logger) {
        VMRuntime.getRuntime();
        VMRuntime.setHiddenApiUsageLogger(logger);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static ClassLoader createPathClassLoader(String classPath, int targetSdkVersion) {
        String libraryPath = System.getProperty("java.library.path");
        ClassLoader parent = ClassLoader.getSystemClassLoader().getParent();
        return ClassLoaderFactory.createClassLoader(classPath, libraryPath, libraryPath, parent, targetSdkVersion, true, null);
    }

    private static Runnable forkSystemServer(String abiList, String socketName, ZygoteServer zygoteServer) {
        long capabilities = posixCapabilitiesAsBits(OsConstants.CAP_IPC_LOCK, OsConstants.CAP_KILL, OsConstants.CAP_NET_ADMIN, OsConstants.CAP_NET_BIND_SERVICE, OsConstants.CAP_NET_BROADCAST, OsConstants.CAP_NET_RAW, OsConstants.CAP_SYS_MODULE, OsConstants.CAP_SYS_NICE, OsConstants.CAP_SYS_PTRACE, OsConstants.CAP_SYS_TIME, OsConstants.CAP_SYS_TTY_CONFIG, OsConstants.CAP_WAKE_ALARM, OsConstants.CAP_BLOCK_SUSPEND);
        StructCapUserHeader header = new StructCapUserHeader(OsConstants._LINUX_CAPABILITY_VERSION_3, 0);
        try {
            StructCapUserData[] data = Os.capget(header);
            long capabilities2 = ((data[1].effective << 32) | data[0].effective) & capabilities;
            String[] args = {"--setuid=1000", "--setgid=1000", "--setgroups=1001,1002,1003,1004,1005,1006,1007,1008,1009,1010,1018,1021,1023,1024,1032,1065,3001,3002,3003,3005,3006,3007,3009,3010,3011,3012", "--capabilities=" + capabilities2 + "," + capabilities2, "--nice-name=system_server", "--runtime-args", "--target-sdk-version=10000", "com.android.server.SystemServer"};
            try {
                ZygoteCommandBuffer commandBuffer = new ZygoteCommandBuffer(args);
                try {
                    ZygoteArguments parsedArgs = ZygoteArguments.getInstance(commandBuffer);
                    commandBuffer.close();
                    Zygote.applyDebuggerSystemProperty(parsedArgs);
                    Zygote.applyInvokeWithSystemProperty(parsedArgs);
                    if (Zygote.nativeSupportsMemoryTagging()) {
                        String mode = SystemProperties.get("arm64.memtag.process.system_server", "");
                        if (mode.isEmpty()) {
                            mode = SystemProperties.get("persist.arm64.memtag.default", "async");
                        }
                        if (mode.equals("async")) {
                            parsedArgs.mRuntimeFlags |= 1048576;
                        } else if (mode.equals("sync")) {
                            parsedArgs.mRuntimeFlags |= 1572864;
                        } else if (!mode.equals("off")) {
                            parsedArgs.mRuntimeFlags |= Zygote.nativeCurrentTaggingLevel();
                            Slog.e(TAG, "Unknown memory tag level for the system server: \"" + mode + "\"");
                        }
                    } else if (Zygote.nativeSupportsTaggedPointers()) {
                        parsedArgs.mRuntimeFlags |= 524288;
                    }
                    parsedArgs.mRuntimeFlags |= 2097152;
                    if (shouldProfileSystemServer()) {
                        parsedArgs.mRuntimeFlags |= 16384;
                    }
                    int pid = Zygote.forkSystemServer(parsedArgs.mUid, parsedArgs.mGid, parsedArgs.mGids, parsedArgs.mRuntimeFlags, null, parsedArgs.mPermittedCapabilities, parsedArgs.mEffectiveCapabilities);
                    if (pid == 0) {
                        if (hasSecondZygote(abiList)) {
                            waitForSecondaryZygote(socketName);
                        }
                        zygoteServer.closeServerSocket();
                        return handleSystemServerProcess(parsedArgs);
                    }
                    return null;
                } catch (EOFException e) {
                    throw new AssertionError("Unexpected argument error for forking system server", e);
                }
            } catch (IllegalArgumentException ex) {
                throw new RuntimeException(ex);
            }
        } catch (ErrnoException ex2) {
            throw new RuntimeException("Failed to capget()", ex2);
        }
    }

    private static long posixCapabilitiesAsBits(int... capabilities) {
        long result = 0;
        for (int capability : capabilities) {
            if (capability < 0 || capability > OsConstants.CAP_LAST_CAP) {
                throw new IllegalArgumentException(String.valueOf(capability));
            }
            result |= 1 << capability;
        }
        return result;
    }

    private static ZygoteServer createTertiaryZygote(String zygoteSocketName) {
        if (!zygoteSocketName.equals(Zygote.TERTIARY_SOCKET_NAME)) {
            return null;
        }
        Zygote.initNativeState();
        ZygoteHooks.stopZygoteNoThreadCreation();
        ZygoteServer zygoteServer = new ZygoteServer(zygoteSocketName, Zygote.USAP_POOL_TERTIARY_SOCKET_NAME);
        return zygoteServer;
    }

    /*  JADX ERROR: JadxRuntimeException in pass: BlockProcessor
        jadx.core.utils.exceptions.JadxRuntimeException: Unreachable block: B:8:0x002b
        	at jadx.core.dex.visitors.blocks.BlockProcessor.checkForUnreachableBlocks(BlockProcessor.java:81)
        	at jadx.core.dex.visitors.blocks.BlockProcessor.processBlocksTree(BlockProcessor.java:47)
        	at jadx.core.dex.visitors.blocks.BlockProcessor.visit(BlockProcessor.java:39)
        */
    public static void main(java.lang.String[] r18) {
        /*
            r1 = r18
            java.lang.String r0 = "--socket-name="
            java.lang.String r2 = "--abi-list="
            java.lang.String r3 = "Zygote"
            r4 = 0
            dalvik.system.ZygoteHooks.startZygoteNoThreadCreation()
            r5 = 0
            android.system.Os.setpgid(r5, r5)     // Catch: android.system.ErrnoException -> L170
            long r5 = android.os.SystemClock.elapsedRealtime()     // Catch: java.lang.Throwable -> L15e
            java.lang.String r7 = "1"
            java.lang.String r8 = "sys.boot_completed"
            java.lang.String r8 = android.os.SystemProperties.get(r8)     // Catch: java.lang.Throwable -> L15e
            boolean r7 = r7.equals(r8)     // Catch: java.lang.Throwable -> L15e
            boolean r8 = android.os.Process.is64Bit()     // Catch: java.lang.Throwable -> L15e
            if (r8 == 0) goto L31
            java.lang.String r8 = "Zygote64Timing"
            goto L33
        L2b:
            r0 = move-exception
            r16 = r4
            r4 = r3
            goto L162
        L31:
            java.lang.String r8 = "Zygote32Timing"
        L33:
            android.util.TimingsTraceLog r9 = new android.util.TimingsTraceLog     // Catch: java.lang.Throwable -> L15e
            r10 = 16384(0x4000, double:8.0948E-320)
            r9.<init>(r8, r10)     // Catch: java.lang.Throwable -> L15e
            java.lang.String r10 = "ZygoteInit"
            r9.traceBegin(r10)     // Catch: java.lang.Throwable -> L15e
            com.android.internal.os.RuntimeInit.preForkInit()     // Catch: java.lang.Throwable -> L15e
            r10 = 0
            java.lang.String r11 = "zygote"
            r12 = r11
            r13 = 0
            r14 = 0
            r15 = 1
        L4a:
            r16 = r4
            int r4 = r1.length     // Catch: java.lang.Throwable -> L15b
            if (r15 >= r4) goto Lb5
            java.lang.String r4 = "start-system-server"
            r17 = r8
            r8 = r1[r15]     // Catch: java.lang.Throwable -> L15b
            boolean r4 = r4.equals(r8)     // Catch: java.lang.Throwable -> L15b
            if (r4 == 0) goto L5f
            r4 = 1
            r10 = r4
            goto L93
        L5f:
            java.lang.String r4 = "--enable-lazy-preload"
            r8 = r1[r15]     // Catch: java.lang.Throwable -> L15b
            boolean r4 = r4.equals(r8)     // Catch: java.lang.Throwable -> L15b
            if (r4 == 0) goto L6c
            r4 = 1
            r14 = r4
            goto L93
        L6c:
            r4 = r1[r15]     // Catch: java.lang.Throwable -> L15b
            boolean r4 = r4.startsWith(r2)     // Catch: java.lang.Throwable -> L15b
            if (r4 == 0) goto L80
            r4 = r1[r15]     // Catch: java.lang.Throwable -> L15b
            int r8 = r2.length()     // Catch: java.lang.Throwable -> L15b
            java.lang.String r4 = r4.substring(r8)     // Catch: java.lang.Throwable -> L15b
            r13 = r4
            goto L93
        L80:
            r4 = r1[r15]     // Catch: java.lang.Throwable -> L15b
            boolean r4 = r4.startsWith(r0)     // Catch: java.lang.Throwable -> L15b
            if (r4 == 0) goto L9a
            r4 = r1[r15]     // Catch: java.lang.Throwable -> L15b
            int r8 = r0.length()     // Catch: java.lang.Throwable -> L15b
            java.lang.String r4 = r4.substring(r8)     // Catch: java.lang.Throwable -> L15b
            r12 = r4
        L93:
            int r15 = r15 + 1
            r4 = r16
            r8 = r17
            goto L4a
        L9a:
            java.lang.RuntimeException r0 = new java.lang.RuntimeException     // Catch: java.lang.Throwable -> L15b
            java.lang.StringBuilder r2 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> L15b
            r2.<init>()     // Catch: java.lang.Throwable -> L15b
            java.lang.String r4 = "Unknown command line argument: "
            java.lang.StringBuilder r2 = r2.append(r4)     // Catch: java.lang.Throwable -> L15b
            r4 = r1[r15]     // Catch: java.lang.Throwable -> L15b
            java.lang.StringBuilder r2 = r2.append(r4)     // Catch: java.lang.Throwable -> L15b
            java.lang.String r2 = r2.toString()     // Catch: java.lang.Throwable -> L15b
            r0.<init>(r2)     // Catch: java.lang.Throwable -> L15b
            throw r0     // Catch: java.lang.Throwable -> L15b
        Lb5:
            r17 = r8
            boolean r0 = r12.equals(r11)     // Catch: java.lang.Throwable -> L15b
            if (r7 != 0) goto Ld5
            r2 = 240(0xf0, float:3.36E-43)
            if (r0 == 0) goto Lc7
            r4 = 17
            com.android.internal.util.FrameworkStatsLog.write(r2, r4, r5)     // Catch: java.lang.Throwable -> L15b
            goto Ld5
        Lc7:
            java.lang.String r4 = "zygote_secondary"
            boolean r4 = r12.equals(r4)     // Catch: java.lang.Throwable -> L15b
            if (r4 == 0) goto Ld5
            r4 = 18
            com.android.internal.util.FrameworkStatsLog.write(r2, r4, r5)     // Catch: java.lang.Throwable -> L15b
        Ld5:
            if (r13 == 0) goto L150
            if (r14 != 0) goto Lff
            java.lang.String r2 = "ZygotePreload"
            r9.traceBegin(r2)     // Catch: java.lang.Throwable -> L15b
            r4 = r3
            long r2 = android.os.SystemClock.uptimeMillis()     // Catch: java.lang.Throwable -> L159
            r8 = 3020(0xbcc, float:4.232E-42)
            android.util.EventLog.writeEvent(r8, r2)     // Catch: java.lang.Throwable -> L159
            java.lang.String r2 = "Zygote:Preload Start"
            addBootEvent(r2)     // Catch: java.lang.Throwable -> L159
            preload(r9)     // Catch: java.lang.Throwable -> L159
            long r2 = android.os.SystemClock.uptimeMillis()     // Catch: java.lang.Throwable -> L159
            r8 = 3030(0xbd6, float:4.246E-42)
            android.util.EventLog.writeEvent(r8, r2)     // Catch: java.lang.Throwable -> L159
            r9.traceEnd()     // Catch: java.lang.Throwable -> L159
            goto L100
        Lff:
            r4 = r3
        L100:
            java.lang.String r2 = "PostZygoteInitGC"
            r9.traceBegin(r2)     // Catch: java.lang.Throwable -> L159
            gcAndFinalize()     // Catch: java.lang.Throwable -> L159
            r9.traceEnd()     // Catch: java.lang.Throwable -> L159
            r9.traceEnd()     // Catch: java.lang.Throwable -> L159
            java.lang.String r2 = "Zygote:Preload End"
            addBootEvent(r2)     // Catch: java.lang.Throwable -> L159
            com.android.internal.os.ZygoteServer r2 = createTertiaryZygote(r12)     // Catch: java.lang.Throwable -> L159
            if (r2 != 0) goto L12a
            com.android.internal.os.Zygote.initNativeState(r0)     // Catch: java.lang.Throwable -> L126
            dalvik.system.ZygoteHooks.stopZygoteNoThreadCreation()     // Catch: java.lang.Throwable -> L126
            com.android.internal.os.ZygoteServer r3 = new com.android.internal.os.ZygoteServer     // Catch: java.lang.Throwable -> L126
            r3.<init>(r0)     // Catch: java.lang.Throwable -> L126
            r2 = r3
            goto L12a
        L126:
            r0 = move-exception
            r16 = r2
            goto L162
        L12a:
            if (r10 == 0) goto L13b
            java.lang.Runnable r3 = forkSystemServer(r13, r12, r2)     // Catch: java.lang.Throwable -> L126
            if (r3 == 0) goto L13b
            r3.run()     // Catch: java.lang.Throwable -> L126
            if (r2 == 0) goto L13a
            r2.closeServerSocket()
        L13a:
            return
        L13b:
            java.lang.String r3 = "Accepting command socket connections"
            android.util.Log.i(r4, r3)     // Catch: java.lang.Throwable -> L126
            java.lang.Runnable r3 = r2.runSelectLoop(r13)     // Catch: java.lang.Throwable -> L126
            r0 = r3
            if (r2 == 0) goto L14a
            r2.closeServerSocket()
        L14a:
            if (r0 == 0) goto L14f
            r0.run()
        L14f:
            return
        L150:
            r4 = r3
            java.lang.RuntimeException r2 = new java.lang.RuntimeException     // Catch: java.lang.Throwable -> L159
            java.lang.String r3 = "No ABI list supplied."
            r2.<init>(r3)     // Catch: java.lang.Throwable -> L159
            throw r2     // Catch: java.lang.Throwable -> L159
        L159:
            r0 = move-exception
            goto L162
        L15b:
            r0 = move-exception
            r4 = r3
            goto L162
        L15e:
            r0 = move-exception
            r16 = r4
            r4 = r3
        L162:
            java.lang.String r2 = "System zygote died with fatal exception"
            android.util.Log.e(r4, r2, r0)     // Catch: java.lang.Throwable -> L169
            throw r0     // Catch: java.lang.Throwable -> L169
        L169:
            r0 = move-exception
            if (r16 == 0) goto L16f
            r16.closeServerSocket()
        L16f:
            throw r0
        L170:
            r0 = move-exception
            r16 = r4
            r2 = r0
            r0 = r2
            java.lang.RuntimeException r2 = new java.lang.RuntimeException
            java.lang.String r3 = "Failed to setpgid(0,0)"
            r2.<init>(r3, r0)
            throw r2
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.ZygoteInit.main(java.lang.String[]):void");
    }

    private static boolean hasSecondZygote(String abiList) {
        return !SystemProperties.get("ro.product.cpu.abilist").equals(abiList);
    }

    private static void waitForSecondaryZygote(String socketName) {
        String otherZygoteName = Zygote.PRIMARY_SOCKET_NAME;
        if (Zygote.PRIMARY_SOCKET_NAME.equals(socketName)) {
            otherZygoteName = Zygote.SECONDARY_SOCKET_NAME;
        }
        ZygoteProcess.waitForConnectionToZygote(otherZygoteName);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static boolean isPreloadComplete() {
        return sPreloadComplete;
    }

    private ZygoteInit() {
    }

    public static Runnable zygoteInit(int targetSdkVersion, long[] disabledCompatChanges, String[] argv, ClassLoader classLoader) {
        Trace.traceBegin(64L, "ZygoteInit");
        RuntimeInit.redirectLogStreams();
        RuntimeInit.commonInit();
        nativeZygoteInit();
        return RuntimeInit.applicationInit(targetSdkVersion, disabledCompatChanges, argv, classLoader);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static Runnable childZygoteInit(String[] argv) {
        RuntimeInit.Arguments args = new RuntimeInit.Arguments(argv);
        return RuntimeInit.findStaticMain(args.startClass, args.startArgs, null);
    }
}
