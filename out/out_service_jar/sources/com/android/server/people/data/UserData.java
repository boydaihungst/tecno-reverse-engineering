package com.android.server.people.data;

import android.os.Environment;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Slog;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;
import java.util.function.Function;
/* loaded from: classes2.dex */
public class UserData {
    private static final int CONVERSATIONS_END_TOKEN = -1;
    private static final String TAG = UserData.class.getSimpleName();
    private String mDefaultDialer;
    private String mDefaultSmsApp;
    private boolean mIsUnlocked;
    private Map<String, PackageData> mPackageDataMap = new ArrayMap();
    private final File mPerUserPeopleDataDir;
    private final ScheduledExecutorService mScheduledExecutorService;
    private final int mUserId;

    public UserData(int userId, ScheduledExecutorService scheduledExecutorService) {
        this.mUserId = userId;
        this.mPerUserPeopleDataDir = new File(Environment.getDataSystemCeDirectory(userId), "people");
        this.mScheduledExecutorService = scheduledExecutorService;
    }

    public int getUserId() {
        return this.mUserId;
    }

    public void forAllPackages(Consumer<PackageData> consumer) {
        for (PackageData packageData : this.mPackageDataMap.values()) {
            consumer.accept(packageData);
        }
    }

    public void setUserUnlocked() {
        this.mIsUnlocked = true;
    }

    public void setUserStopped() {
        this.mIsUnlocked = false;
    }

    public boolean isUnlocked() {
        return this.mIsUnlocked;
    }

    public void loadUserData() {
        this.mPerUserPeopleDataDir.mkdir();
        Map<String, PackageData> packageDataMap = PackageData.packagesDataFromDisk(this.mUserId, new UserData$$ExternalSyntheticLambda1(this), new UserData$$ExternalSyntheticLambda2(this), this.mScheduledExecutorService, this.mPerUserPeopleDataDir);
        this.mPackageDataMap.putAll(packageDataMap);
    }

    public PackageData getOrCreatePackageData(final String packageName) {
        return this.mPackageDataMap.computeIfAbsent(packageName, new Function() { // from class: com.android.server.people.data.UserData$$ExternalSyntheticLambda0
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                return UserData.this.m5361x7cfba4da(packageName, (String) obj);
            }
        });
    }

    /* renamed from: lambda$getOrCreatePackageData$0$com-android-server-people-data-UserData */
    public /* synthetic */ PackageData m5361x7cfba4da(String packageName, String key) {
        return createPackageData(packageName);
    }

    public PackageData getPackageData(String packageName) {
        return this.mPackageDataMap.get(packageName);
    }

    public void deletePackageData(String packageName) {
        PackageData packageData = this.mPackageDataMap.remove(packageName);
        if (packageData != null) {
            packageData.onDestroy();
        }
    }

    public void setDefaultDialer(String packageName) {
        this.mDefaultDialer = packageName;
    }

    public PackageData getDefaultDialer() {
        String str = this.mDefaultDialer;
        if (str != null) {
            return getPackageData(str);
        }
        return null;
    }

    public void setDefaultSmsApp(String packageName) {
        this.mDefaultSmsApp = packageName;
    }

    public PackageData getDefaultSmsApp() {
        String str = this.mDefaultSmsApp;
        if (str != null) {
            return getPackageData(str);
        }
        return null;
    }

    public byte[] getBackupPayload() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(baos);
        for (PackageData packageData : this.mPackageDataMap.values()) {
            try {
                byte[] conversationsBackupPayload = packageData.getConversationStore().getBackupPayload();
                out.writeInt(conversationsBackupPayload.length);
                out.write(conversationsBackupPayload);
                out.writeUTF(packageData.getPackageName());
            } catch (IOException e) {
                Slog.e(TAG, "Failed to write conversations to backup payload.", e);
                return null;
            }
        }
        try {
            out.writeInt(-1);
            return baos.toByteArray();
        } catch (IOException e2) {
            Slog.e(TAG, "Failed to write conversations end token to backup payload.", e2);
            return null;
        }
    }

    public void restore(byte[] payload) {
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(payload));
        try {
            for (int conversationsPayloadSize = in.readInt(); conversationsPayloadSize != -1; conversationsPayloadSize = in.readInt()) {
                byte[] conversationsPayload = new byte[conversationsPayloadSize];
                in.readFully(conversationsPayload, 0, conversationsPayloadSize);
                String packageName = in.readUTF();
                getOrCreatePackageData(packageName).getConversationStore().restore(conversationsPayload);
            }
        } catch (IOException e) {
            Slog.e(TAG, "Failed to restore conversations from backup payload.", e);
        }
    }

    private PackageData createPackageData(String packageName) {
        return new PackageData(packageName, this.mUserId, new UserData$$ExternalSyntheticLambda1(this), new UserData$$ExternalSyntheticLambda2(this), this.mScheduledExecutorService, this.mPerUserPeopleDataDir);
    }

    public boolean isDefaultDialer(String packageName) {
        return TextUtils.equals(this.mDefaultDialer, packageName);
    }

    public boolean isDefaultSmsApp(String packageName) {
        return TextUtils.equals(this.mDefaultSmsApp, packageName);
    }
}
