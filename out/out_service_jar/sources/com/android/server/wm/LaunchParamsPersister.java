package com.android.server.wm;

import android.content.ComponentName;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManagerInternal;
import android.graphics.Rect;
import android.os.Environment;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.AtomicFile;
import android.util.Slog;
import android.util.SparseArray;
import android.util.TypedXmlPullParser;
import android.util.TypedXmlSerializer;
import android.util.Xml;
import android.view.DisplayInfo;
import com.android.server.LocalServices;
import com.android.server.pm.PackageList;
import com.android.server.wm.LaunchParamsController;
import com.android.server.wm.LaunchParamsPersister;
import com.android.server.wm.PersisterQueue;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class LaunchParamsPersister {
    private static final char ESCAPED_COMPONENT_SEPARATOR = '-';
    private static final String LAUNCH_PARAMS_DIRNAME = "launch_params";
    private static final String LAUNCH_PARAMS_FILE_SUFFIX = ".xml";
    private static final char OLD_ESCAPED_COMPONENT_SEPARATOR = '_';
    private static final char ORIGINAL_COMPONENT_SEPARATOR = '/';
    private static final String TAG = "LaunchParamsPersister";
    private static final String TAG_LAUNCH_PARAMS = "launch_params";
    private final SparseArray<ArrayMap<ComponentName, PersistableLaunchParams>> mLaunchParamsMap;
    private PackageList mPackageList;
    private final PersisterQueue mPersisterQueue;
    private final ActivityTaskSupervisor mSupervisor;
    private final IntFunction<File> mUserFolderGetter;
    private final ArrayMap<String, ArraySet<ComponentName>> mWindowLayoutAffinityMap;

    /* JADX INFO: Access modifiers changed from: package-private */
    public LaunchParamsPersister(PersisterQueue persisterQueue, ActivityTaskSupervisor supervisor) {
        this(persisterQueue, supervisor, new IntFunction() { // from class: com.android.server.wm.LaunchParamsPersister$$ExternalSyntheticLambda2
            @Override // java.util.function.IntFunction
            public final Object apply(int i) {
                return Environment.getDataSystemCeDirectory(i);
            }
        });
    }

    LaunchParamsPersister(PersisterQueue persisterQueue, ActivityTaskSupervisor supervisor, IntFunction<File> userFolderGetter) {
        this.mLaunchParamsMap = new SparseArray<>();
        this.mWindowLayoutAffinityMap = new ArrayMap<>();
        this.mPersisterQueue = persisterQueue;
        this.mSupervisor = supervisor;
        this.mUserFolderGetter = userFolderGetter;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onSystemReady() {
        PackageManagerInternal pmi = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
        this.mPackageList = pmi.getPackageList(new PackageListObserver());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onUnlockUser(int userId) {
        loadLaunchParams(userId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onCleanupUser(int userId) {
        this.mLaunchParamsMap.remove(userId);
    }

    private void loadLaunchParams(int userId) {
        File launchParamsFolder;
        Set<String> packages;
        File[] paramsFiles;
        int i;
        Throwable th;
        List<File> filesToDelete = new ArrayList<>();
        File launchParamsFolder2 = getLaunchParamFolder(userId);
        if (!launchParamsFolder2.isDirectory()) {
            Slog.i(TAG, "Didn't find launch param folder for user " + userId);
            return;
        }
        Set<String> packages2 = new ArraySet<>(this.mPackageList.getPackageNames());
        File[] paramsFiles2 = launchParamsFolder2.listFiles();
        ArrayMap<ComponentName, PersistableLaunchParams> map = new ArrayMap<>(paramsFiles2.length);
        this.mLaunchParamsMap.put(userId, map);
        int length = paramsFiles2.length;
        int i2 = 0;
        while (i2 < length) {
            File paramsFile = paramsFiles2[i2];
            if (!paramsFile.isFile()) {
                Slog.w(TAG, paramsFile.getAbsolutePath() + " is not a file.");
                launchParamsFolder = launchParamsFolder2;
                packages = packages2;
                paramsFiles = paramsFiles2;
                i = length;
            } else if (!paramsFile.getName().endsWith(LAUNCH_PARAMS_FILE_SUFFIX)) {
                Slog.w(TAG, "Unexpected params file name: " + paramsFile.getName());
                filesToDelete.add(paramsFile);
                launchParamsFolder = launchParamsFolder2;
                packages = packages2;
                paramsFiles = paramsFiles2;
                i = length;
            } else {
                String paramsFileName = paramsFile.getName();
                int oldSeparatorIndex = paramsFileName.indexOf(95);
                if (oldSeparatorIndex != -1) {
                    if (paramsFileName.indexOf(95, oldSeparatorIndex + 1) == -1) {
                        paramsFileName = paramsFileName.replace(OLD_ESCAPED_COMPONENT_SEPARATOR, ESCAPED_COMPONENT_SEPARATOR);
                        File newFile = new File(launchParamsFolder2, paramsFileName);
                        if (paramsFile.renameTo(newFile)) {
                            paramsFile = newFile;
                        } else {
                            filesToDelete.add(paramsFile);
                            launchParamsFolder = launchParamsFolder2;
                            packages = packages2;
                            paramsFiles = paramsFiles2;
                            i = length;
                        }
                    } else {
                        filesToDelete.add(paramsFile);
                        launchParamsFolder = launchParamsFolder2;
                        packages = packages2;
                        paramsFiles = paramsFiles2;
                        i = length;
                    }
                }
                String componentNameString = paramsFileName.substring(0, paramsFileName.length() - LAUNCH_PARAMS_FILE_SUFFIX.length()).replace(ESCAPED_COMPONENT_SEPARATOR, ORIGINAL_COMPONENT_SEPARATOR);
                ComponentName name = ComponentName.unflattenFromString(componentNameString);
                if (name == null) {
                    Slog.w(TAG, "Unexpected file name: " + paramsFileName);
                    filesToDelete.add(paramsFile);
                    launchParamsFolder = launchParamsFolder2;
                    packages = packages2;
                    paramsFiles = paramsFiles2;
                    i = length;
                } else if (!packages2.contains(name.getPackageName())) {
                    filesToDelete.add(paramsFile);
                    launchParamsFolder = launchParamsFolder2;
                    packages = packages2;
                    paramsFiles = paramsFiles2;
                    i = length;
                } else {
                    try {
                        InputStream in = new FileInputStream(paramsFile);
                        try {
                            launchParamsFolder = launchParamsFolder2;
                            try {
                                PersistableLaunchParams params = new PersistableLaunchParams();
                                TypedXmlPullParser parser = Xml.resolvePullParser(in);
                                while (true) {
                                    packages = packages2;
                                    try {
                                        int event = parser.next();
                                        paramsFiles = paramsFiles2;
                                        if (event == 1) {
                                            i = length;
                                            break;
                                        } else if (event == 3) {
                                            i = length;
                                            break;
                                        } else if (event != 2) {
                                            packages2 = packages;
                                            paramsFiles2 = paramsFiles;
                                        } else {
                                            try {
                                                String tagName = parser.getName();
                                                if (!"launch_params".equals(tagName)) {
                                                    i = length;
                                                    try {
                                                        Slog.w(TAG, "Unexpected tag name: " + tagName);
                                                        packages2 = packages;
                                                        paramsFiles2 = paramsFiles;
                                                        length = i;
                                                    } catch (Throwable th2) {
                                                        th = th2;
                                                        th = th;
                                                        in.close();
                                                        throw th;
                                                        break;
                                                    }
                                                } else {
                                                    params.restore(paramsFile, parser);
                                                    packages2 = packages;
                                                    paramsFiles2 = paramsFiles;
                                                    length = length;
                                                }
                                            } catch (Throwable th3) {
                                                th = th3;
                                                i = length;
                                            }
                                        }
                                    } catch (Throwable th4) {
                                        paramsFiles = paramsFiles2;
                                        i = length;
                                        th = th4;
                                    }
                                }
                                map.put(name, params);
                                addComponentNameToLaunchParamAffinityMapIfNotNull(name, params.mWindowLayoutAffinity);
                                try {
                                    in.close();
                                } catch (Exception e) {
                                    e = e;
                                    Slog.w(TAG, "Failed to restore launch params for " + name, e);
                                    filesToDelete.add(paramsFile);
                                    i2++;
                                    launchParamsFolder2 = launchParamsFolder;
                                    packages2 = packages;
                                    paramsFiles2 = paramsFiles;
                                    length = i;
                                }
                            } catch (Throwable th5) {
                                packages = packages2;
                                paramsFiles = paramsFiles2;
                                i = length;
                                th = th5;
                            }
                        } catch (Throwable th6) {
                            launchParamsFolder = launchParamsFolder2;
                            packages = packages2;
                            paramsFiles = paramsFiles2;
                            i = length;
                            th = th6;
                        }
                    } catch (Exception e2) {
                        e = e2;
                        launchParamsFolder = launchParamsFolder2;
                        packages = packages2;
                        paramsFiles = paramsFiles2;
                        i = length;
                    }
                }
            }
            i2++;
            launchParamsFolder2 = launchParamsFolder;
            packages2 = packages;
            paramsFiles2 = paramsFiles;
            length = i;
        }
        if (!filesToDelete.isEmpty()) {
            this.mPersisterQueue.addItem(new CleanUpComponentQueueItem(filesToDelete), true);
        }
    }

    void saveTask(Task task) {
        saveTask(task, task.getDisplayContent());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void saveTask(Task task, DisplayContent display) {
        ArrayMap<ComponentName, PersistableLaunchParams> map;
        ComponentName name = task.realActivity;
        if (name == null) {
            return;
        }
        int userId = task.mUserId;
        ArrayMap<ComponentName, PersistableLaunchParams> map2 = this.mLaunchParamsMap.get(userId);
        if (map2 != null) {
            map = map2;
        } else {
            ArrayMap<ComponentName, PersistableLaunchParams> map3 = new ArrayMap<>();
            this.mLaunchParamsMap.put(userId, map3);
            map = map3;
        }
        PersistableLaunchParams params = map.computeIfAbsent(name, new Function() { // from class: com.android.server.wm.LaunchParamsPersister$$ExternalSyntheticLambda0
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                return LaunchParamsPersister.this.m8095lambda$saveTask$0$comandroidserverwmLaunchParamsPersister((ComponentName) obj);
            }
        });
        boolean changed = saveTaskToLaunchParam(task, display, params);
        addComponentNameToLaunchParamAffinityMapIfNotNull(name, params.mWindowLayoutAffinity);
        if (changed) {
            this.mPersisterQueue.updateLastOrAddItem(new LaunchParamsWriteQueueItem(userId, name, params), false);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$saveTask$0$com-android-server-wm-LaunchParamsPersister  reason: not valid java name */
    public /* synthetic */ PersistableLaunchParams m8095lambda$saveTask$0$comandroidserverwmLaunchParamsPersister(ComponentName componentName) {
        return new PersistableLaunchParams();
    }

    private boolean saveTaskToLaunchParam(Task task, DisplayContent display, PersistableLaunchParams params) {
        boolean changed;
        DisplayInfo info = new DisplayInfo();
        display.mDisplay.getDisplayInfo(info);
        boolean changed2 = !Objects.equals(params.mDisplayUniqueId, info.uniqueId);
        params.mDisplayUniqueId = info.uniqueId;
        boolean changed3 = changed2 | (params.mWindowingMode != task.getWindowingMode());
        params.mWindowingMode = task.getWindowingMode();
        if (task.mLastNonFullscreenBounds != null) {
            changed = changed3 | (true ^ Objects.equals(params.mBounds, task.mLastNonFullscreenBounds));
            params.mBounds.set(task.mLastNonFullscreenBounds);
        } else {
            changed = changed3 | (true ^ params.mBounds.isEmpty());
            params.mBounds.setEmpty();
        }
        String launchParamAffinity = task.mWindowLayoutAffinity;
        boolean changed4 = changed | Objects.equals(launchParamAffinity, params.mWindowLayoutAffinity);
        params.mWindowLayoutAffinity = launchParamAffinity;
        if (changed4) {
            params.mTimestamp = System.currentTimeMillis();
        }
        return changed4;
    }

    private void addComponentNameToLaunchParamAffinityMapIfNotNull(ComponentName name, String launchParamAffinity) {
        if (launchParamAffinity == null) {
            return;
        }
        this.mWindowLayoutAffinityMap.computeIfAbsent(launchParamAffinity, new Function() { // from class: com.android.server.wm.LaunchParamsPersister$$ExternalSyntheticLambda1
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                return LaunchParamsPersister.lambda$addComponentNameToLaunchParamAffinityMapIfNotNull$1((String) obj);
            }
        }).add(name);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ ArraySet lambda$addComponentNameToLaunchParamAffinityMapIfNotNull$1(String affinity) {
        return new ArraySet();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void getLaunchParams(Task task, ActivityRecord activity, LaunchParamsController.LaunchParams outParams) {
        String windowLayoutAffinity;
        ComponentName name = task != null ? task.realActivity : activity.mActivityComponent;
        int userId = task != null ? task.mUserId : activity.mUserId;
        if (task != null) {
            windowLayoutAffinity = task.mWindowLayoutAffinity;
        } else {
            ActivityInfo.WindowLayout layout = activity.info.windowLayout;
            windowLayoutAffinity = layout == null ? null : layout.windowLayoutAffinity;
        }
        outParams.reset();
        Map<ComponentName, PersistableLaunchParams> map = this.mLaunchParamsMap.get(userId);
        if (map == null) {
            return;
        }
        PersistableLaunchParams persistableParams = map.get(name);
        if (windowLayoutAffinity != null && this.mWindowLayoutAffinityMap.get(windowLayoutAffinity) != null) {
            ArraySet<ComponentName> candidates = this.mWindowLayoutAffinityMap.get(windowLayoutAffinity);
            for (int i = 0; i < candidates.size(); i++) {
                ComponentName candidate = candidates.valueAt(i);
                PersistableLaunchParams candidateParams = map.get(candidate);
                if (candidateParams != null && (persistableParams == null || candidateParams.mTimestamp > persistableParams.mTimestamp)) {
                    persistableParams = candidateParams;
                }
            }
        }
        if (persistableParams == null) {
            return;
        }
        DisplayContent display = this.mSupervisor.mRootWindowContainer.getDisplayContent(persistableParams.mDisplayUniqueId);
        if (display != null) {
            outParams.mPreferredTaskDisplayArea = display.getDefaultTaskDisplayArea();
        }
        outParams.mWindowingMode = persistableParams.mWindowingMode;
        outParams.mBounds.set(persistableParams.mBounds);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void removeRecordForPackage(final String packageName) {
        List<File> fileToDelete = new ArrayList<>();
        for (int i = 0; i < this.mLaunchParamsMap.size(); i++) {
            int userId = this.mLaunchParamsMap.keyAt(i);
            File launchParamsFolder = getLaunchParamFolder(userId);
            ArrayMap<ComponentName, PersistableLaunchParams> map = this.mLaunchParamsMap.valueAt(i);
            for (int j = map.size() - 1; j >= 0; j--) {
                ComponentName name = map.keyAt(j);
                if (name.getPackageName().equals(packageName)) {
                    map.removeAt(j);
                    fileToDelete.add(getParamFile(launchParamsFolder, name));
                }
            }
        }
        synchronized (this.mPersisterQueue) {
            this.mPersisterQueue.removeItems(new Predicate() { // from class: com.android.server.wm.LaunchParamsPersister$$ExternalSyntheticLambda3
                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    boolean equals;
                    equals = ((LaunchParamsPersister.LaunchParamsWriteQueueItem) obj).mComponentName.getPackageName().equals(packageName);
                    return equals;
                }
            }, LaunchParamsWriteQueueItem.class);
            this.mPersisterQueue.addItem(new CleanUpComponentQueueItem(fileToDelete), true);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public File getParamFile(File launchParamFolder, ComponentName name) {
        String componentNameString = name.flattenToShortString().replace(ORIGINAL_COMPONENT_SEPARATOR, ESCAPED_COMPONENT_SEPARATOR);
        return new File(launchParamFolder, componentNameString + LAUNCH_PARAMS_FILE_SUFFIX);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public File getLaunchParamFolder(int userId) {
        File userFolder = this.mUserFolderGetter.apply(userId);
        return new File(userFolder, "launch_params");
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class PackageListObserver implements PackageManagerInternal.PackageListObserver {
        private PackageListObserver() {
        }

        @Override // android.content.pm.PackageManagerInternal.PackageListObserver
        public void onPackageAdded(String packageName, int uid) {
        }

        @Override // android.content.pm.PackageManagerInternal.PackageListObserver
        public void onPackageRemoved(String packageName, int uid) {
            synchronized (LaunchParamsPersister.this.mSupervisor.mService.getGlobalLock()) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    LaunchParamsPersister.this.removeRecordForPackage(packageName);
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class LaunchParamsWriteQueueItem implements PersisterQueue.WriteQueueItem<LaunchParamsWriteQueueItem> {
        private final ComponentName mComponentName;
        private PersistableLaunchParams mLaunchParams;
        private final int mUserId;

        private LaunchParamsWriteQueueItem(int userId, ComponentName componentName, PersistableLaunchParams launchParams) {
            this.mUserId = userId;
            this.mComponentName = componentName;
            this.mLaunchParams = launchParams;
        }

        private byte[] saveParamsToXml() {
            try {
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                TypedXmlSerializer serializer = Xml.resolveSerializer(os);
                serializer.startDocument((String) null, true);
                serializer.startTag((String) null, "launch_params");
                this.mLaunchParams.saveToXml(serializer);
                serializer.endTag((String) null, "launch_params");
                serializer.endDocument();
                serializer.flush();
                return os.toByteArray();
            } catch (IOException e) {
                return null;
            }
        }

        @Override // com.android.server.wm.PersisterQueue.WriteQueueItem
        public void process() {
            byte[] data = saveParamsToXml();
            File launchParamFolder = LaunchParamsPersister.this.getLaunchParamFolder(this.mUserId);
            if (!launchParamFolder.isDirectory() && !launchParamFolder.mkdirs()) {
                Slog.w(LaunchParamsPersister.TAG, "Failed to create folder for " + this.mUserId);
                return;
            }
            File launchParamFile = LaunchParamsPersister.this.getParamFile(launchParamFolder, this.mComponentName);
            AtomicFile atomicFile = new AtomicFile(launchParamFile);
            FileOutputStream stream = null;
            try {
                stream = atomicFile.startWrite();
                stream.write(data);
                atomicFile.finishWrite(stream);
            } catch (Exception e) {
                Slog.e(LaunchParamsPersister.TAG, "Failed to write param file for " + this.mComponentName, e);
                if (stream != null) {
                    atomicFile.failWrite(stream);
                }
            }
        }

        /* JADX DEBUG: Method merged with bridge method */
        @Override // com.android.server.wm.PersisterQueue.WriteQueueItem
        public boolean matches(LaunchParamsWriteQueueItem item) {
            return this.mUserId == item.mUserId && this.mComponentName.equals(item.mComponentName);
        }

        /* JADX DEBUG: Method merged with bridge method */
        @Override // com.android.server.wm.PersisterQueue.WriteQueueItem
        public void updateFrom(LaunchParamsWriteQueueItem item) {
            this.mLaunchParams = item.mLaunchParams;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class CleanUpComponentQueueItem implements PersisterQueue.WriteQueueItem {
        private final List<File> mComponentFiles;

        private CleanUpComponentQueueItem(List<File> componentFiles) {
            this.mComponentFiles = componentFiles;
        }

        @Override // com.android.server.wm.PersisterQueue.WriteQueueItem
        public void process() {
            for (File file : this.mComponentFiles) {
                if (!file.delete()) {
                    Slog.w(LaunchParamsPersister.TAG, "Failed to delete " + file.getAbsolutePath());
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class PersistableLaunchParams {
        private static final String ATTR_BOUNDS = "bounds";
        private static final String ATTR_DISPLAY_UNIQUE_ID = "display_unique_id";
        private static final String ATTR_WINDOWING_MODE = "windowing_mode";
        private static final String ATTR_WINDOW_LAYOUT_AFFINITY = "window_layout_affinity";
        final Rect mBounds;
        String mDisplayUniqueId;
        long mTimestamp;
        String mWindowLayoutAffinity;
        int mWindowingMode;

        private PersistableLaunchParams() {
            this.mBounds = new Rect();
        }

        void saveToXml(TypedXmlSerializer serializer) throws IOException {
            serializer.attribute((String) null, ATTR_DISPLAY_UNIQUE_ID, this.mDisplayUniqueId);
            serializer.attributeInt((String) null, ATTR_WINDOWING_MODE, this.mWindowingMode);
            serializer.attribute((String) null, ATTR_BOUNDS, this.mBounds.flattenToString());
            String str = this.mWindowLayoutAffinity;
            if (str != null) {
                serializer.attribute((String) null, ATTR_WINDOW_LAYOUT_AFFINITY, str);
            }
        }

        void restore(File xmlFile, TypedXmlPullParser parser) {
            for (int i = 0; i < parser.getAttributeCount(); i++) {
                String attrValue = parser.getAttributeValue(i);
                String attributeName = parser.getAttributeName(i);
                char c = 65535;
                switch (attributeName.hashCode()) {
                    case -1499361012:
                        if (attributeName.equals(ATTR_DISPLAY_UNIQUE_ID)) {
                            c = 0;
                            break;
                        }
                        break;
                    case -1383205195:
                        if (attributeName.equals(ATTR_BOUNDS)) {
                            c = 2;
                            break;
                        }
                        break;
                    case 748872656:
                        if (attributeName.equals(ATTR_WINDOWING_MODE)) {
                            c = 1;
                            break;
                        }
                        break;
                    case 1999609934:
                        if (attributeName.equals(ATTR_WINDOW_LAYOUT_AFFINITY)) {
                            c = 3;
                            break;
                        }
                        break;
                }
                switch (c) {
                    case 0:
                        this.mDisplayUniqueId = attrValue;
                        break;
                    case 1:
                        this.mWindowingMode = Integer.parseInt(attrValue);
                        break;
                    case 2:
                        Rect bounds = Rect.unflattenFromString(attrValue);
                        if (bounds != null) {
                            this.mBounds.set(bounds);
                            break;
                        } else {
                            break;
                        }
                    case 3:
                        this.mWindowLayoutAffinity = attrValue;
                        break;
                }
            }
            this.mTimestamp = xmlFile.lastModified();
        }

        public String toString() {
            StringBuilder builder = new StringBuilder("PersistableLaunchParams{");
            builder.append(" windowingMode=" + this.mWindowingMode);
            builder.append(" displayUniqueId=" + this.mDisplayUniqueId);
            builder.append(" bounds=" + this.mBounds);
            if (this.mWindowLayoutAffinity != null) {
                builder.append(" launchParamsAffinity=" + this.mWindowLayoutAffinity);
            }
            builder.append(" timestamp=" + this.mTimestamp);
            builder.append(" }");
            return builder.toString();
        }
    }
}
