package com.android.server.storage;

import android.app.usage.CacheQuotaHint;
import android.app.usage.ICacheQuotaService;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManagerInternal;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.pm.UserInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.RemoteCallback;
import android.os.RemoteException;
import android.os.StatFs;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.ArrayMap;
import android.util.AtomicFile;
import android.util.Pair;
import android.util.Slog;
import android.util.SparseLongArray;
import android.util.TypedXmlPullParser;
import android.util.TypedXmlSerializer;
import android.util.Xml;
import com.android.server.am.HostingRecord;
import com.android.server.pm.Installer;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes2.dex */
public class CacheQuotaStrategy implements RemoteCallback.OnResultListener {
    private static final String ATTR_PREVIOUS_BYTES = "previousBytes";
    private static final String ATTR_QUOTA_IN_BYTES = "bytes";
    private static final String ATTR_UID = "uid";
    private static final String ATTR_UUID = "uuid";
    private static final String CACHE_INFO_TAG = "cache-info";
    private static final String TAG = "CacheQuotaStrategy";
    private static final String TAG_QUOTA = "quota";
    private final Context mContext;
    private final Installer mInstaller;
    private final Object mLock = new Object();
    private AtomicFile mPreviousValuesFile = new AtomicFile(new File(new File(Environment.getDataDirectory(), HostingRecord.HOSTING_TYPE_SYSTEM), "cachequota.xml"));
    private final ArrayMap<String, SparseLongArray> mQuotaMap;
    private ICacheQuotaService mRemoteService;
    private ServiceConnection mServiceConnection;
    private final UsageStatsManagerInternal mUsageStats;

    public CacheQuotaStrategy(Context context, UsageStatsManagerInternal usageStatsManager, Installer installer, ArrayMap<String, SparseLongArray> quotaMap) {
        this.mContext = (Context) Objects.requireNonNull(context);
        this.mUsageStats = (UsageStatsManagerInternal) Objects.requireNonNull(usageStatsManager);
        this.mInstaller = (Installer) Objects.requireNonNull(installer);
        this.mQuotaMap = (ArrayMap) Objects.requireNonNull(quotaMap);
    }

    public void recalculateQuotas() {
        createServiceConnection();
        ComponentName component = getServiceComponentName();
        if (component != null) {
            Intent intent = new Intent();
            intent.setComponent(component);
            this.mContext.bindServiceAsUser(intent, this.mServiceConnection, 1, UserHandle.CURRENT);
        }
    }

    private void createServiceConnection() {
        if (this.mServiceConnection != null) {
            return;
        }
        this.mServiceConnection = new ServiceConnection() { // from class: com.android.server.storage.CacheQuotaStrategy.1
            @Override // android.content.ServiceConnection
            public void onServiceConnected(ComponentName name, final IBinder service) {
                Runnable runnable = new Runnable() { // from class: com.android.server.storage.CacheQuotaStrategy.1.1
                    @Override // java.lang.Runnable
                    public void run() {
                        synchronized (CacheQuotaStrategy.this.mLock) {
                            CacheQuotaStrategy.this.mRemoteService = ICacheQuotaService.Stub.asInterface(service);
                            List<CacheQuotaHint> requests = CacheQuotaStrategy.this.getUnfulfilledRequests();
                            RemoteCallback remoteCallback = new RemoteCallback(CacheQuotaStrategy.this);
                            try {
                                CacheQuotaStrategy.this.mRemoteService.computeCacheQuotaHints(remoteCallback, requests);
                            } catch (RemoteException ex) {
                                Slog.w(CacheQuotaStrategy.TAG, "Remote exception occurred while trying to get cache quota", ex);
                            }
                        }
                    }
                };
                AsyncTask.execute(runnable);
            }

            @Override // android.content.ServiceConnection
            public void onServiceDisconnected(ComponentName name) {
                synchronized (CacheQuotaStrategy.this.mLock) {
                    CacheQuotaStrategy.this.mRemoteService = null;
                }
            }
        };
    }

    /* JADX INFO: Access modifiers changed from: private */
    public List<CacheQuotaHint> getUnfulfilledRequests() {
        PackageManager packageManager;
        CacheQuotaStrategy cacheQuotaStrategy = this;
        long timeNow = System.currentTimeMillis();
        long oneYearAgo = timeNow - 31449600000L;
        List<CacheQuotaHint> requests = new ArrayList<>();
        UserManager um = (UserManager) cacheQuotaStrategy.mContext.getSystemService(UserManager.class);
        List<UserInfo> users = um.getUsers();
        int userCount = users.size();
        PackageManager packageManager2 = cacheQuotaStrategy.mContext.getPackageManager();
        int i = 0;
        while (i < userCount) {
            UserInfo info = users.get(i);
            int i2 = i;
            UserInfo info2 = info;
            int userCount2 = userCount;
            PackageManager packageManager3 = packageManager2;
            List<UserInfo> users2 = users;
            List<UsageStats> stats = cacheQuotaStrategy.mUsageStats.queryUsageStatsForUser(info.id, 4, oneYearAgo, timeNow, false);
            if (stats != null) {
                for (UsageStats stat : stats) {
                    String packageName = stat.getPackageName();
                    UserInfo info3 = info2;
                    try {
                        packageManager = packageManager3;
                    } catch (PackageManager.NameNotFoundException e) {
                        packageManager = packageManager3;
                    }
                    try {
                        ApplicationInfo appInfo = packageManager.getApplicationInfoAsUser(packageName, 0, info3.id);
                        requests.add(new CacheQuotaHint.Builder().setVolumeUuid(appInfo.volumeUuid).setUid(appInfo.uid).setUsageStats(stat).setQuota(-1L).build());
                        info2 = info3;
                        packageManager3 = packageManager;
                    } catch (PackageManager.NameNotFoundException e2) {
                        info2 = info3;
                        packageManager3 = packageManager;
                    }
                }
            }
            packageManager2 = packageManager3;
            i = i2 + 1;
            cacheQuotaStrategy = this;
            userCount = userCount2;
            users = users2;
        }
        return requests;
    }

    public void onResult(Bundle data) {
        List<CacheQuotaHint> processedRequests = data.getParcelableArrayList("requests");
        pushProcessedQuotas(processedRequests);
        writeXmlToFile(processedRequests);
    }

    private void pushProcessedQuotas(List<CacheQuotaHint> processedRequests) {
        int requestSize = processedRequests.size();
        for (int i = 0; i < requestSize; i++) {
            CacheQuotaHint request = processedRequests.get(i);
            long proposedQuota = request.getQuota();
            if (proposedQuota != -1) {
                try {
                    int uid = request.getUid();
                    this.mInstaller.setAppQuota(request.getVolumeUuid(), UserHandle.getUserId(uid), UserHandle.getAppId(uid), proposedQuota);
                    insertIntoQuotaMap(request.getVolumeUuid(), UserHandle.getUserId(uid), UserHandle.getAppId(uid), proposedQuota);
                } catch (Installer.InstallerException ex) {
                    Slog.w(TAG, "Failed to set cache quota for " + request.getUid(), ex);
                }
            }
        }
        disconnectService();
    }

    private void insertIntoQuotaMap(String volumeUuid, int userId, int appId, long quota) {
        SparseLongArray volumeMap = this.mQuotaMap.get(volumeUuid);
        if (volumeMap == null) {
            volumeMap = new SparseLongArray();
            this.mQuotaMap.put(volumeUuid, volumeMap);
        }
        volumeMap.put(UserHandle.getUid(userId, appId), quota);
    }

    private void disconnectService() {
        ServiceConnection serviceConnection = this.mServiceConnection;
        if (serviceConnection != null) {
            this.mContext.unbindService(serviceConnection);
            this.mServiceConnection = null;
        }
    }

    private ComponentName getServiceComponentName() {
        String packageName = this.mContext.getPackageManager().getServicesSystemSharedLibraryPackageName();
        if (packageName == null) {
            Slog.w(TAG, "could not access the cache quota service: no package!");
            return null;
        }
        Intent intent = new Intent("android.app.usage.CacheQuotaService");
        intent.setPackage(packageName);
        ResolveInfo resolveInfo = this.mContext.getPackageManager().resolveService(intent, 132);
        if (resolveInfo == null || resolveInfo.serviceInfo == null) {
            Slog.w(TAG, "No valid components found.");
            return null;
        }
        ServiceInfo serviceInfo = resolveInfo.serviceInfo;
        return new ComponentName(serviceInfo.packageName, serviceInfo.name);
    }

    private void writeXmlToFile(List<CacheQuotaHint> processedRequests) {
        FileOutputStream fileStream = null;
        try {
            fileStream = this.mPreviousValuesFile.startWrite();
            TypedXmlSerializer out = Xml.resolveSerializer(fileStream);
            StatFs stats = new StatFs(Environment.getDataDirectory().getAbsolutePath());
            saveToXml(out, processedRequests, stats.getAvailableBytes());
            this.mPreviousValuesFile.finishWrite(fileStream);
        } catch (Exception e) {
            Slog.e(TAG, "An error occurred while writing the cache quota file.", e);
            this.mPreviousValuesFile.failWrite(fileStream);
        }
    }

    public long setupQuotasFromFile() throws IOException {
        try {
            FileInputStream stream = this.mPreviousValuesFile.openRead();
            try {
                Pair<Long, List<CacheQuotaHint>> cachedValues = readFromXml(stream);
                if (stream != null) {
                    stream.close();
                }
                if (cachedValues == null) {
                    Slog.e(TAG, "An error occurred while parsing the cache quota file.");
                    return -1L;
                }
                pushProcessedQuotas((List) cachedValues.second);
                return ((Long) cachedValues.first).longValue();
            } catch (XmlPullParserException e) {
                throw new IllegalStateException(e.getMessage());
            }
        } catch (FileNotFoundException e2) {
            return -1L;
        }
    }

    static void saveToXml(TypedXmlSerializer out, List<CacheQuotaHint> requests, long bytesWhenCalculated) throws IOException {
        out.startDocument((String) null, true);
        out.startTag((String) null, CACHE_INFO_TAG);
        int requestSize = requests.size();
        out.attributeLong((String) null, ATTR_PREVIOUS_BYTES, bytesWhenCalculated);
        for (int i = 0; i < requestSize; i++) {
            CacheQuotaHint request = requests.get(i);
            out.startTag((String) null, TAG_QUOTA);
            String uuid = request.getVolumeUuid();
            if (uuid != null) {
                out.attribute((String) null, ATTR_UUID, request.getVolumeUuid());
            }
            out.attributeInt((String) null, "uid", request.getUid());
            out.attributeLong((String) null, ATTR_QUOTA_IN_BYTES, request.getQuota());
            out.endTag((String) null, TAG_QUOTA);
        }
        out.endTag((String) null, CACHE_INFO_TAG);
        out.endDocument();
    }

    protected static Pair<Long, List<CacheQuotaHint>> readFromXml(InputStream inputStream) throws XmlPullParserException, IOException {
        TypedXmlPullParser parser = Xml.resolvePullParser(inputStream);
        int eventType = parser.getEventType();
        while (eventType != 2 && eventType != 1) {
            eventType = parser.next();
        }
        if (eventType == 1) {
            Slog.d(TAG, "No quotas found in quota file.");
            return null;
        }
        String tagName = parser.getName();
        if (!CACHE_INFO_TAG.equals(tagName)) {
            throw new IllegalStateException("Invalid starting tag.");
        }
        List<CacheQuotaHint> quotas = new ArrayList<>();
        try {
            long previousBytes = parser.getAttributeLong((String) null, ATTR_PREVIOUS_BYTES);
            int eventType2 = parser.next();
            do {
                if (eventType2 == 2) {
                    String tagName2 = parser.getName();
                    if (TAG_QUOTA.equals(tagName2)) {
                        CacheQuotaHint request = getRequestFromXml(parser);
                        if (request == null) {
                            continue;
                        } else {
                            quotas.add(request);
                        }
                    }
                }
                eventType2 = parser.next();
                continue;
            } while (eventType2 != 1);
            return new Pair<>(Long.valueOf(previousBytes), quotas);
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Previous bytes formatted incorrectly; aborting quota read.");
        }
    }

    static CacheQuotaHint getRequestFromXml(TypedXmlPullParser parser) {
        try {
            String uuid = parser.getAttributeValue((String) null, ATTR_UUID);
            int uid = parser.getAttributeInt((String) null, "uid");
            long bytes = parser.getAttributeLong((String) null, ATTR_QUOTA_IN_BYTES);
            return new CacheQuotaHint.Builder().setVolumeUuid(uuid).setUid(uid).setQuota(bytes).build();
        } catch (XmlPullParserException e) {
            Slog.e(TAG, "Invalid cache quota request, skipping.");
            return null;
        }
    }
}
