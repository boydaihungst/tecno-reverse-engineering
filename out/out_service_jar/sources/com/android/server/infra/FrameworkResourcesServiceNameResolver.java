package com.android.server.infra;

import android.app.AppGlobals;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ServiceInfo;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.util.TimeUtils;
import com.android.server.infra.ServiceNameResolver;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
/* loaded from: classes.dex */
public final class FrameworkResourcesServiceNameResolver implements ServiceNameResolver {
    private static final int MSG_RESET_TEMPORARY_SERVICE = 0;
    private static final String TAG = FrameworkResourcesServiceNameResolver.class.getSimpleName();
    private final int mArrayResourceId;
    private final Context mContext;
    private final SparseBooleanArray mDefaultServicesDisabled;
    private final boolean mIsMultiple;
    private final Object mLock;
    private ServiceNameResolver.NameResolverListener mOnSetCallback;
    private final int mStringResourceId;
    private Handler mTemporaryHandler;
    private long mTemporaryServiceExpiration;
    private final SparseArray<String[]> mTemporaryServiceNamesList;

    public FrameworkResourcesServiceNameResolver(Context context, int resourceId) {
        this.mLock = new Object();
        this.mTemporaryServiceNamesList = new SparseArray<>();
        this.mDefaultServicesDisabled = new SparseBooleanArray();
        this.mContext = context;
        this.mStringResourceId = resourceId;
        this.mArrayResourceId = -1;
        this.mIsMultiple = false;
    }

    public FrameworkResourcesServiceNameResolver(Context context, int resourceId, boolean isMultiple) {
        this.mLock = new Object();
        this.mTemporaryServiceNamesList = new SparseArray<>();
        this.mDefaultServicesDisabled = new SparseBooleanArray();
        if (!isMultiple) {
            throw new UnsupportedOperationException("Please use FrameworkResourcesServiceNameResolver(context, @StringRes int) constructor if single service mode is requested.");
        }
        this.mContext = context;
        this.mStringResourceId = -1;
        this.mArrayResourceId = resourceId;
        this.mIsMultiple = true;
    }

    @Override // com.android.server.infra.ServiceNameResolver
    public void setOnTemporaryServiceNameChangedCallback(ServiceNameResolver.NameResolverListener callback) {
        synchronized (this.mLock) {
            this.mOnSetCallback = callback;
        }
    }

    @Override // com.android.server.infra.ServiceNameResolver
    public String getServiceName(int userId) {
        String[] serviceNames = getServiceNameList(userId);
        if (serviceNames == null || serviceNames.length == 0) {
            return null;
        }
        return serviceNames[0];
    }

    @Override // com.android.server.infra.ServiceNameResolver
    public String getDefaultServiceName(int userId) {
        String[] serviceNames = getDefaultServiceNameList(userId);
        if (serviceNames == null || serviceNames.length == 0) {
            return null;
        }
        return serviceNames[0];
    }

    @Override // com.android.server.infra.ServiceNameResolver
    public String[] getServiceNameList(int userId) {
        synchronized (this.mLock) {
            String[] temporaryNames = this.mTemporaryServiceNamesList.get(userId);
            if (temporaryNames != null) {
                Slog.w(TAG, "getServiceName(): using temporary name " + Arrays.toString(temporaryNames) + " for user " + userId);
                return temporaryNames;
            }
            boolean disabled = this.mDefaultServicesDisabled.get(userId);
            if (disabled) {
                Slog.w(TAG, "getServiceName(): temporary name not set and default disabled for user " + userId);
                return null;
            }
            return getDefaultServiceNameList(userId);
        }
    }

    @Override // com.android.server.infra.ServiceNameResolver
    public String[] getDefaultServiceNameList(int userId) {
        synchronized (this.mLock) {
            if (this.mIsMultiple) {
                String[] serviceNameList = this.mContext.getResources().getStringArray(this.mArrayResourceId);
                List<String> validatedServiceNameList = new ArrayList<>();
                for (int i = 0; i < serviceNameList.length; i++) {
                    try {
                        if (!TextUtils.isEmpty(serviceNameList[i])) {
                            ComponentName serviceComponent = ComponentName.unflattenFromString(serviceNameList[i]);
                            ServiceInfo serviceInfo = AppGlobals.getPackageManager().getServiceInfo(serviceComponent, 786432L, userId);
                            if (serviceInfo != null) {
                                validatedServiceNameList.add(serviceNameList[i]);
                            }
                        }
                    } catch (Exception e) {
                        Slog.e(TAG, "Could not validate provided services.", e);
                    }
                }
                String[] validatedServiceNameArray = new String[validatedServiceNameList.size()];
                return (String[]) validatedServiceNameList.toArray(validatedServiceNameArray);
            }
            String name = this.mContext.getString(this.mStringResourceId);
            return TextUtils.isEmpty(name) ? new String[0] : new String[]{name};
        }
    }

    @Override // com.android.server.infra.ServiceNameResolver
    public boolean isConfiguredInMultipleMode() {
        return this.mIsMultiple;
    }

    @Override // com.android.server.infra.ServiceNameResolver
    public boolean isTemporary(int userId) {
        boolean z;
        synchronized (this.mLock) {
            z = this.mTemporaryServiceNamesList.get(userId) != null;
        }
        return z;
    }

    @Override // com.android.server.infra.ServiceNameResolver
    public void setTemporaryService(int userId, String componentName, int durationMs) {
        setTemporaryServices(userId, new String[]{componentName}, durationMs);
    }

    @Override // com.android.server.infra.ServiceNameResolver
    public void setTemporaryServices(final int userId, String[] componentNames, int durationMs) {
        synchronized (this.mLock) {
            this.mTemporaryServiceNamesList.put(userId, componentNames);
            Handler handler = this.mTemporaryHandler;
            if (handler == null) {
                this.mTemporaryHandler = new Handler(Looper.getMainLooper(), null, true) { // from class: com.android.server.infra.FrameworkResourcesServiceNameResolver.1
                    @Override // android.os.Handler
                    public void handleMessage(Message msg) {
                        if (msg.what == 0) {
                            synchronized (FrameworkResourcesServiceNameResolver.this.mLock) {
                                FrameworkResourcesServiceNameResolver.this.resetTemporaryService(userId);
                            }
                            return;
                        }
                        Slog.wtf(FrameworkResourcesServiceNameResolver.TAG, "invalid handler msg: " + msg);
                    }
                };
            } else {
                handler.removeMessages(0);
            }
            this.mTemporaryServiceExpiration = SystemClock.elapsedRealtime() + durationMs;
            this.mTemporaryHandler.sendEmptyMessageDelayed(0, durationMs);
            for (String str : componentNames) {
                notifyTemporaryServiceNameChangedLocked(userId, str, true);
            }
        }
    }

    @Override // com.android.server.infra.ServiceNameResolver
    public void resetTemporaryService(int userId) {
        synchronized (this.mLock) {
            Slog.i(TAG, "resetting temporary service for user " + userId + " from " + Arrays.toString(this.mTemporaryServiceNamesList.get(userId)));
            this.mTemporaryServiceNamesList.remove(userId);
            Handler handler = this.mTemporaryHandler;
            if (handler != null) {
                handler.removeMessages(0);
                this.mTemporaryHandler = null;
            }
            notifyTemporaryServiceNameChangedLocked(userId, null, false);
        }
    }

    @Override // com.android.server.infra.ServiceNameResolver
    public boolean setDefaultServiceEnabled(int userId, boolean enabled) {
        synchronized (this.mLock) {
            boolean currentlyEnabled = isDefaultServiceEnabledLocked(userId);
            if (currentlyEnabled == enabled) {
                Slog.i(TAG, "setDefaultServiceEnabled(" + userId + "): already " + enabled);
                return false;
            }
            if (enabled) {
                Slog.i(TAG, "disabling default service for user " + userId);
                this.mDefaultServicesDisabled.removeAt(userId);
            } else {
                Slog.i(TAG, "enabling default service for user " + userId);
                this.mDefaultServicesDisabled.put(userId, true);
            }
            return true;
        }
    }

    @Override // com.android.server.infra.ServiceNameResolver
    public boolean isDefaultServiceEnabled(int userId) {
        boolean isDefaultServiceEnabledLocked;
        synchronized (this.mLock) {
            isDefaultServiceEnabledLocked = isDefaultServiceEnabledLocked(userId);
        }
        return isDefaultServiceEnabledLocked;
    }

    private boolean isDefaultServiceEnabledLocked(int userId) {
        return !this.mDefaultServicesDisabled.get(userId);
    }

    public String toString() {
        String str;
        synchronized (this.mLock) {
            str = "FrameworkResourcesServiceNamer[temps=" + this.mTemporaryServiceNamesList + "]";
        }
        return str;
    }

    @Override // com.android.server.infra.ServiceNameResolver
    public void dumpShort(PrintWriter pw) {
        synchronized (this.mLock) {
            pw.print("FrameworkResourcesServiceNamer: resId=");
            pw.print(this.mStringResourceId);
            pw.print(", numberTemps=");
            pw.print(this.mTemporaryServiceNamesList.size());
            pw.print(", enabledDefaults=");
            pw.print(this.mDefaultServicesDisabled.size());
        }
    }

    @Override // com.android.server.infra.ServiceNameResolver
    public void dumpShort(PrintWriter pw, int userId) {
        synchronized (this.mLock) {
            String[] temporaryNames = this.mTemporaryServiceNamesList.get(userId);
            if (temporaryNames != null) {
                pw.print("tmpName=");
                pw.print(Arrays.toString(temporaryNames));
                long ttl = this.mTemporaryServiceExpiration - SystemClock.elapsedRealtime();
                pw.print(" (expires in ");
                TimeUtils.formatDuration(ttl, pw);
                pw.print("), ");
            }
            pw.print("defaultName=");
            pw.print(getDefaultServiceName(userId));
            boolean disabled = this.mDefaultServicesDisabled.get(userId);
            pw.println(disabled ? " (disabled)" : " (enabled)");
        }
    }

    private void notifyTemporaryServiceNameChangedLocked(int userId, String newTemporaryName, boolean isTemporary) {
        ServiceNameResolver.NameResolverListener nameResolverListener = this.mOnSetCallback;
        if (nameResolverListener != null) {
            nameResolverListener.onNameResolved(userId, newTemporaryName, isTemporary);
        }
    }
}
