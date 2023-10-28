package com.android.server.camera;

import android.app.ActivityManager;
import android.app.ActivityTaskManager;
import android.app.admin.DevicePolicyManager;
import android.app.compat.CompatChanges;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ParceledListSlice;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.hardware.CameraSessionStats;
import android.hardware.CameraStreamStats;
import android.hardware.ICameraService;
import android.hardware.ICameraServiceProxy;
import android.hardware.devicestate.DeviceStateManager;
import android.hardware.display.DisplayManager;
import android.hardware.usb.UsbDevice;
import android.media.AudioManager;
import android.nfc.IAppCallback;
import android.nfc.INfcAdapter;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerExecutor;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.stats.camera.nano.CameraProtos;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Log;
import android.util.Slog;
import android.view.Display;
import android.view.IDisplayWindowListener;
import android.view.WindowManagerGlobal;
import com.android.framework.protobuf.nano.MessageNano;
import com.android.internal.util.FrameworkStatsLog;
import com.android.server.LocalServices;
import com.android.server.ServiceThread;
import com.android.server.SystemService;
import com.android.server.wm.WindowManagerInternal;
import com.transsion.hubcore.server.camera.ITranCameraServiceProxy;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
/* loaded from: classes.dex */
public class CameraServiceProxy extends SystemService implements Handler.Callback, IBinder.DeathRecipient {
    private static final String CAMERA_SERVICE_BINDER_NAME = "media.camera";
    public static final String CAMERA_SERVICE_PROXY_BINDER_NAME = "media.camera.proxy";
    private static final boolean DEBUG = false;
    public static final int DISABLE_POLLING_FLAGS = 4096;
    public static final int ENABLE_POLLING_FLAGS = 0;
    private static final float MAX_PREVIEW_FPS = 60.0f;
    private static final int MAX_STREAM_STATISTICS = 5;
    private static final int MAX_USAGE_HISTORY = 20;
    private static final float MIN_PREVIEW_FPS = 30.0f;
    private static final int MSG_NOTIFY_DEVICE_STATE = 2;
    private static final int MSG_REMOVE_PACKAGE = 4;
    private static final int MSG_SWITCH_USER = 1;
    private static final String NFC_NOTIFICATION_PROP = "ro.camera.notify_nfc";
    private static final String NFC_SERVICE_BINDER_NAME = "nfc";
    public static final long OVERRIDE_CAMERA_RESIZABLE_AND_SDK_CHECK = 191513214;
    public static final long OVERRIDE_CAMERA_ROTATE_AND_CROP_DEFAULTS = 189229956;
    private static final int RETRY_DELAY_TIME = 20;
    private static final int RETRY_TIMES = 60;
    private static final String TAG = "CameraService_proxy";
    private static final IBinder nfcInterfaceToken = new Binder();
    private final ArrayMap<String, CameraUsageEvent> mActiveCameraUsage;
    private final ICameraServiceProxy.Stub mCameraServiceProxy;
    private ICameraService mCameraServiceRaw;
    private final List<CameraUsageEvent> mCameraUsageHistory;
    private final Context mContext;
    private int mDeviceState;
    private final DisplayWindowListener mDisplayWindowListener;
    private Set<Integer> mEnabledCameraUsers;
    private final DeviceStateManager.FoldStateListener mFoldStateListener;
    private final Handler mHandler;
    private final ServiceThread mHandlerThread;
    private final BroadcastReceiver mIntentReceiver;
    private int mLastReportedDeviceState;
    private int mLastUser;
    private final Object mLock;
    private ScheduledThreadPoolExecutor mLogWriterService;
    private final boolean mNotifyNfc;
    private UserManager mUserManager;

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes.dex */
    @interface DeviceStateFlags {
    }

    /* loaded from: classes.dex */
    public static final class TaskInfo {
        public int displayId;
        public int frontTaskId;
        public boolean isFixedOrientationLandscape;
        public boolean isFixedOrientationPortrait;
        public boolean isResizeable;
        public int userId;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class CameraUsageEvent {
        public final int mAPILevel;
        public final int mAction;
        public final int mCameraFacing;
        public final String mCameraId;
        public final String mClientName;
        public boolean mDeviceError;
        public int mInternalReconfigure;
        public final boolean mIsNdk;
        public final int mLatencyMs;
        public final int mOperatingMode;
        public long mRequestCount;
        public long mResultErrorCount;
        public List<CameraStreamStats> mStreamStats;
        public String mUserTag;
        public int mVideoStabilizationMode;
        private long mDurationOrStartTimeMs = SystemClock.elapsedRealtime();
        private boolean mCompleted = false;

        CameraUsageEvent(String cameraId, int facing, String clientName, int apiLevel, boolean isNdk, int action, int latencyMs, int operatingMode) {
            this.mCameraId = cameraId;
            this.mCameraFacing = facing;
            this.mClientName = clientName;
            this.mAPILevel = apiLevel;
            this.mIsNdk = isNdk;
            this.mAction = action;
            this.mLatencyMs = latencyMs;
            this.mOperatingMode = operatingMode;
        }

        public void markCompleted(int internalReconfigure, long requestCount, long resultErrorCount, boolean deviceError, List<CameraStreamStats> streamStats, String userTag, int videoStabilizationMode) {
            if (this.mCompleted) {
                return;
            }
            this.mCompleted = true;
            this.mDurationOrStartTimeMs = SystemClock.elapsedRealtime() - this.mDurationOrStartTimeMs;
            this.mInternalReconfigure = internalReconfigure;
            this.mRequestCount = requestCount;
            this.mResultErrorCount = resultErrorCount;
            this.mDeviceError = deviceError;
            this.mStreamStats = streamStats;
            this.mUserTag = userTag;
            this.mVideoStabilizationMode = videoStabilizationMode;
        }

        public long getDuration() {
            if (this.mCompleted) {
                return this.mDurationOrStartTimeMs;
            }
            return 0L;
        }
    }

    /* loaded from: classes.dex */
    private final class DisplayWindowListener extends IDisplayWindowListener.Stub {
        private DisplayWindowListener() {
        }

        public void onDisplayConfigurationChanged(int displayId, Configuration newConfig) {
            ICameraService cs = CameraServiceProxy.this.getCameraServiceRawLocked();
            if (cs == null) {
                return;
            }
            try {
                cs.notifyDisplayConfigurationChange();
            } catch (RemoteException e) {
                Slog.w(CameraServiceProxy.TAG, "Could not notify cameraserver, remote exception: " + e);
            }
        }

        public void onDisplayAdded(int displayId) {
        }

        public void onDisplayRemoved(int displayId) {
        }

        public void onFixedRotationStarted(int displayId, int newRotation) {
        }

        public void onFixedRotationFinished(int displayId) {
        }

        public void onKeepClearAreasChanged(int displayId, List<Rect> restricted, List<Rect> unrestricted) {
        }
    }

    private static boolean isMOrBelow(Context ctx, String packageName) {
        try {
            return ctx.getPackageManager().getPackageInfo(packageName, 0).applicationInfo.targetSdkVersion <= 23;
        } catch (PackageManager.NameNotFoundException e) {
            Slog.e(TAG, "Package name not found!");
            return false;
        }
    }

    public static int getCropRotateScale(Context ctx, String packageName, TaskInfo taskInfo, int displayRotation, int lensFacing, boolean ignoreResizableAndSdkCheck) {
        int rotationDegree;
        if (taskInfo == null) {
            return 0;
        }
        if (lensFacing != 0 && lensFacing != 1) {
            Log.v(TAG, "lensFacing=" + lensFacing + ". Crop-rotate-scale is disabled.");
            return 0;
        } else if (!ignoreResizableAndSdkCheck && !isMOrBelow(ctx, packageName) && taskInfo.isResizeable) {
            Slog.v(TAG, "The activity is N or above and claims to support resizeable-activity. Crop-rotate-scale is disabled.");
            return 0;
        } else if (!taskInfo.isFixedOrientationPortrait && !taskInfo.isFixedOrientationLandscape) {
            Log.v(TAG, "Non-fixed orientation activity. Crop-rotate-scale is disabled.");
            return 0;
        } else {
            switch (displayRotation) {
                case 0:
                    rotationDegree = 0;
                    break;
                case 1:
                    rotationDegree = 90;
                    break;
                case 2:
                    rotationDegree = FrameworkStatsLog.DEVICE_POLICY_EVENT__EVENT_ID__CREDENTIAL_MANAGEMENT_APP_REQUEST_ACCEPTED;
                    break;
                case 3:
                    rotationDegree = 270;
                    break;
                default:
                    Log.e(TAG, "Unsupported display rotation: " + displayRotation);
                    return 0;
            }
            Slog.v(TAG, "Display.getRotation()=" + rotationDegree + " isFixedOrientationPortrait=" + taskInfo.isFixedOrientationPortrait + " isFixedOrientationLandscape=" + taskInfo.isFixedOrientationLandscape);
            if (rotationDegree == 0) {
                return 0;
            }
            if (lensFacing == 0) {
                rotationDegree = 360 - rotationDegree;
            }
            switch (rotationDegree) {
                case 90:
                    return 1;
                case FrameworkStatsLog.DEVICE_POLICY_EVENT__EVENT_ID__CREDENTIAL_MANAGEMENT_APP_REQUEST_ACCEPTED /* 180 */:
                    return 2;
                case 270:
                    return 3;
                default:
                    return 0;
            }
        }
    }

    public CameraServiceProxy(Context context) {
        super(context);
        this.mLock = new Object();
        this.mActiveCameraUsage = new ArrayMap<>();
        this.mCameraUsageHistory = new ArrayList();
        this.mLogWriterService = new ScheduledThreadPoolExecutor(1);
        this.mDisplayWindowListener = new DisplayWindowListener();
        this.mIntentReceiver = new BroadcastReceiver() { // from class: com.android.server.camera.CameraServiceProxy.1
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent) {
                String action = intent.getAction();
                if (action == null) {
                    return;
                }
                char c = 65535;
                switch (action.hashCode()) {
                    case -2114103349:
                        if (action.equals("android.hardware.usb.action.USB_DEVICE_ATTACHED")) {
                            c = 5;
                            break;
                        }
                        break;
                    case -2061058799:
                        if (action.equals("android.intent.action.USER_REMOVED")) {
                            c = 1;
                            break;
                        }
                        break;
                    case -1608292967:
                        if (action.equals("android.hardware.usb.action.USB_DEVICE_DETACHED")) {
                            c = 6;
                            break;
                        }
                        break;
                    case -385593787:
                        if (action.equals("android.intent.action.MANAGED_PROFILE_ADDED")) {
                            c = 3;
                            break;
                        }
                        break;
                    case -201513518:
                        if (action.equals("android.intent.action.USER_INFO_CHANGED")) {
                            c = 2;
                            break;
                        }
                        break;
                    case 1051477093:
                        if (action.equals("android.intent.action.MANAGED_PROFILE_REMOVED")) {
                            c = 4;
                            break;
                        }
                        break;
                    case 1121780209:
                        if (action.equals("android.intent.action.USER_ADDED")) {
                            c = 0;
                            break;
                        }
                        break;
                }
                switch (c) {
                    case 0:
                    case 1:
                    case 2:
                    case 3:
                    case 4:
                        synchronized (CameraServiceProxy.this.mLock) {
                            if (CameraServiceProxy.this.mEnabledCameraUsers == null) {
                                return;
                            }
                            CameraServiceProxy cameraServiceProxy = CameraServiceProxy.this;
                            cameraServiceProxy.switchUserLocked(cameraServiceProxy.mLastUser);
                            return;
                        }
                    case 5:
                    case 6:
                        synchronized (CameraServiceProxy.this.mLock) {
                            UsbDevice device = (UsbDevice) intent.getParcelableExtra("device");
                            if (device != null) {
                                CameraServiceProxy.this.notifyUsbDeviceHotplugLocked(device, action.equals("android.hardware.usb.action.USB_DEVICE_ATTACHED"));
                            }
                        }
                        return;
                    default:
                        return;
                }
            }
        };
        this.mCameraServiceProxy = new ICameraServiceProxy.Stub() { // from class: com.android.server.camera.CameraServiceProxy.2
            public int getRotateAndCropOverride(String packageName, int lensFacing, int userId) {
                if (Binder.getCallingUid() != 1047) {
                    Slog.e(CameraServiceProxy.TAG, "Calling UID: " + Binder.getCallingUid() + " doesn't match expected  camera service UID!");
                    return 0;
                }
                TaskInfo taskInfo = null;
                try {
                    ParceledListSlice<ActivityManager.RecentTaskInfo> recentTasks = ActivityTaskManager.getService().getRecentTasks(2, 0, userId);
                    if (recentTasks != null && !recentTasks.getList().isEmpty()) {
                        Iterator it = recentTasks.getList().iterator();
                        while (true) {
                            if (!it.hasNext()) {
                                break;
                            }
                            ActivityManager.RecentTaskInfo task = (ActivityManager.RecentTaskInfo) it.next();
                            if (packageName.equals(task.topActivityInfo.packageName)) {
                                taskInfo = new TaskInfo();
                                taskInfo.frontTaskId = task.taskId;
                                taskInfo.isResizeable = (task.topActivityInfo.resizeMode == 0 || task.topActivityInfo.resizeMode == -1) ? false : true;
                                taskInfo.displayId = task.displayId;
                                taskInfo.userId = task.userId;
                                taskInfo.isFixedOrientationLandscape = ActivityInfo.isFixedOrientationLandscape(task.topActivityInfo.screenOrientation);
                                taskInfo.isFixedOrientationPortrait = ActivityInfo.isFixedOrientationPortrait(task.topActivityInfo.screenOrientation);
                            }
                        }
                        if (taskInfo == null) {
                            Log.e(CameraServiceProxy.TAG, "Recent tasks don't include camera client package name: " + packageName);
                            return 0;
                        } else if (taskInfo != null && CompatChanges.isChangeEnabled((long) CameraServiceProxy.OVERRIDE_CAMERA_ROTATE_AND_CROP_DEFAULTS, packageName, UserHandle.getUserHandleForUid(taskInfo.userId))) {
                            Slog.v(CameraServiceProxy.TAG, "OVERRIDE_CAMERA_ROTATE_AND_CROP_DEFAULTS enabled!");
                            return 0;
                        } else {
                            boolean ignoreResizableAndSdkCheck = false;
                            if (taskInfo != null && CompatChanges.isChangeEnabled((long) CameraServiceProxy.OVERRIDE_CAMERA_RESIZABLE_AND_SDK_CHECK, packageName, UserHandle.getUserHandleForUid(taskInfo.userId))) {
                                Slog.v(CameraServiceProxy.TAG, "OVERRIDE_CAMERA_RESIZABLE_AND_SDK_CHECK enabled!");
                                ignoreResizableAndSdkCheck = true;
                            }
                            DisplayManager displayManager = (DisplayManager) CameraServiceProxy.this.mContext.getSystemService(DisplayManager.class);
                            if (displayManager != null) {
                                Display display = displayManager.getDisplay(taskInfo.displayId);
                                if (display == null) {
                                    Slog.e(CameraServiceProxy.TAG, "Invalid display id: " + taskInfo.displayId);
                                    return 0;
                                }
                                int displayRotation = display.getRotation();
                                return CameraServiceProxy.getCropRotateScale(CameraServiceProxy.this.mContext, packageName, taskInfo, displayRotation, lensFacing, ignoreResizableAndSdkCheck);
                            }
                            Slog.e(CameraServiceProxy.TAG, "Failed to query display manager!");
                            return 0;
                        }
                    }
                    Log.e(CameraServiceProxy.TAG, "Recent task list is empty!");
                    return 0;
                } catch (RemoteException e) {
                    Log.e(CameraServiceProxy.TAG, "Failed to query recent tasks!");
                    return 0;
                }
            }

            public void pingForUserUpdate() {
                if (Binder.getCallingUid() != 1047) {
                    Slog.e(CameraServiceProxy.TAG, "Calling UID: " + Binder.getCallingUid() + " doesn't match expected  camera service UID!");
                    return;
                }
                CameraServiceProxy.this.notifySwitchWithRetries(60);
                CameraServiceProxy.this.notifyDeviceStateWithRetries(60);
            }

            public void notifyCameraState(CameraSessionStats cameraState) {
                if (Binder.getCallingUid() != 1047) {
                    Slog.e(CameraServiceProxy.TAG, "Calling UID: " + Binder.getCallingUid() + " doesn't match expected  camera service UID!");
                    return;
                }
                CameraServiceProxy.cameraStateToString(cameraState.getNewCameraState());
                CameraServiceProxy.cameraFacingToString(cameraState.getFacing());
                CameraServiceProxy.this.updateActivityCount(cameraState);
            }

            public boolean isCameraDisabled() {
                DevicePolicyManager dpm = (DevicePolicyManager) CameraServiceProxy.this.mContext.getSystemService(DevicePolicyManager.class);
                if (dpm == null) {
                    Slog.e(CameraServiceProxy.TAG, "Failed to get the device policy manager service");
                    return false;
                }
                return dpm.getCameraDisabled(null);
            }
        };
        this.mContext = context;
        ServiceThread serviceThread = new ServiceThread(TAG, -4, false);
        this.mHandlerThread = serviceThread;
        serviceThread.start();
        this.mHandler = new Handler(serviceThread.getLooper(), this);
        this.mNotifyNfc = SystemProperties.getInt(NFC_NOTIFICATION_PROP, 0) > 0;
        this.mLogWriterService.setKeepAliveTime(1L, TimeUnit.SECONDS);
        this.mLogWriterService.allowCoreThreadTimeOut(true);
        this.mFoldStateListener = new DeviceStateManager.FoldStateListener(context, new Consumer() { // from class: com.android.server.camera.CameraServiceProxy$$ExternalSyntheticLambda0
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                CameraServiceProxy.this.m2649lambda$new$0$comandroidservercameraCameraServiceProxy((Boolean) obj);
            }
        });
        ITranCameraServiceProxy.Instance();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$new$0$com-android-server-camera-CameraServiceProxy  reason: not valid java name */
    public /* synthetic */ void m2649lambda$new$0$comandroidservercameraCameraServiceProxy(Boolean folded) {
        if (folded.booleanValue()) {
            setDeviceStateFlags(4);
        } else {
            clearDeviceStateFlags(4);
        }
    }

    public void onForceModeChanged(int forcedMode) {
        Slog.d(TAG, "onForceModeChanged:" + forcedMode);
    }

    private void setDeviceStateFlags(int deviceStateFlags) {
        synchronized (this.mLock) {
            this.mHandler.removeMessages(2);
            int i = this.mDeviceState | deviceStateFlags;
            this.mDeviceState = i;
            if (i != this.mLastReportedDeviceState) {
                notifyDeviceStateWithRetriesLocked(60);
            }
        }
    }

    private void clearDeviceStateFlags(int deviceStateFlags) {
        synchronized (this.mLock) {
            this.mHandler.removeMessages(2);
            int i = this.mDeviceState & (~deviceStateFlags);
            this.mDeviceState = i;
            if (i != this.mLastReportedDeviceState) {
                notifyDeviceStateWithRetriesLocked(60);
            }
        }
    }

    @Override // android.os.Handler.Callback
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case 1:
                notifySwitchWithRetries(msg.arg1);
                return true;
            case 2:
                notifyDeviceStateWithRetries(msg.arg1);
                return true;
            case 3:
            default:
                if (!ITranCameraServiceProxy.Instance().handleMessage(msg.what)) {
                    Slog.e(TAG, "CameraServiceProxy error, invalid message: " + msg.what);
                    return true;
                }
                return true;
            case 4:
                WindowManagerInternal wmi = (WindowManagerInternal) LocalServices.getService(WindowManagerInternal.class);
                wmi.removeRefreshRateRangeForPackage((String) msg.obj);
                ITranCameraServiceProxy.Instance().onCameraInactive((String) msg.obj);
                return true;
        }
    }

    @Override // com.android.server.SystemService
    public void onStart() {
        UserManager userManager = UserManager.get(this.mContext);
        this.mUserManager = userManager;
        if (userManager == null) {
            throw new IllegalStateException("UserManagerService must start before CameraServiceProxy!");
        }
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.USER_ADDED");
        filter.addAction("android.intent.action.USER_REMOVED");
        filter.addAction("android.intent.action.USER_INFO_CHANGED");
        filter.addAction("android.intent.action.MANAGED_PROFILE_ADDED");
        filter.addAction("android.intent.action.MANAGED_PROFILE_REMOVED");
        filter.addAction("android.hardware.usb.action.USB_DEVICE_ATTACHED");
        filter.addAction("android.hardware.usb.action.USB_DEVICE_DETACHED");
        this.mContext.registerReceiver(this.mIntentReceiver, filter);
        publishBinderService(CAMERA_SERVICE_PROXY_BINDER_NAME, this.mCameraServiceProxy);
        publishLocalService(CameraServiceProxy.class, this);
    }

    @Override // com.android.server.SystemService
    public void onBootPhase(int phase) {
        if (phase == 1000) {
            CameraStatsJobService.schedule(this.mContext);
            try {
                int[] displayIds = WindowManagerGlobal.getWindowManagerService().registerDisplayWindowListener(this.mDisplayWindowListener);
                for (int i : displayIds) {
                    this.mDisplayWindowListener.onDisplayAdded(i);
                }
            } catch (RemoteException e) {
                Log.e(TAG, "Failed to register display window listener!");
            }
            ((DeviceStateManager) this.mContext.getSystemService(DeviceStateManager.class)).registerCallback(new HandlerExecutor(this.mHandler), this.mFoldStateListener);
        }
    }

    @Override // com.android.server.SystemService
    public void onUserStarting(SystemService.TargetUser user) {
        synchronized (this.mLock) {
            if (this.mEnabledCameraUsers == null) {
                switchUserLocked(user.getUserIdentifier());
            }
        }
    }

    @Override // com.android.server.SystemService
    public void onUserSwitching(SystemService.TargetUser from, SystemService.TargetUser to) {
        synchronized (this.mLock) {
            switchUserLocked(to.getUserIdentifier());
        }
    }

    @Override // android.os.IBinder.DeathRecipient
    public void binderDied() {
        synchronized (this.mLock) {
            this.mCameraServiceRaw = null;
            boolean wasEmpty = this.mActiveCameraUsage.isEmpty();
            this.mActiveCameraUsage.clear();
            if (this.mNotifyNfc && !wasEmpty) {
                notifyNfcService(true);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class EventWriterTask implements Runnable {
        private static final long WRITER_SLEEP_MS = 100;
        private ArrayList<CameraUsageEvent> mEventList;

        public EventWriterTask(ArrayList<CameraUsageEvent> eventList) {
            this.mEventList = eventList;
        }

        @Override // java.lang.Runnable
        public void run() {
            ArrayList<CameraUsageEvent> arrayList = this.mEventList;
            if (arrayList != null) {
                Iterator<CameraUsageEvent> it = arrayList.iterator();
                while (it.hasNext()) {
                    CameraUsageEvent event = it.next();
                    logCameraUsageEvent(event);
                    try {
                        Thread.sleep(WRITER_SLEEP_MS);
                    } catch (InterruptedException e) {
                    }
                }
                this.mEventList.clear();
            }
        }

        private void logCameraUsageEvent(CameraUsageEvent e) {
            int facing = 0;
            switch (e.mCameraFacing) {
                case 0:
                    facing = 1;
                    break;
                case 1:
                    facing = 2;
                    break;
                case 2:
                    facing = 3;
                    break;
                default:
                    Slog.w(CameraServiceProxy.TAG, "Unknown camera facing: " + e.mCameraFacing);
                    break;
            }
            int streamCount = 0;
            if (e.mStreamStats != null) {
                streamCount = e.mStreamStats.size();
            }
            CameraProtos.CameraStreamProto[] streamProtos = new CameraProtos.CameraStreamProto[5];
            for (int i = 0; i < 5; i++) {
                streamProtos[i] = new CameraProtos.CameraStreamProto();
                if (i < streamCount) {
                    CameraStreamStats streamStats = e.mStreamStats.get(i);
                    streamProtos[i].width = streamStats.getWidth();
                    streamProtos[i].height = streamStats.getHeight();
                    streamProtos[i].format = streamStats.getFormat();
                    streamProtos[i].dataSpace = streamStats.getDataSpace();
                    streamProtos[i].usage = streamStats.getUsage();
                    streamProtos[i].requestCount = streamStats.getRequestCount();
                    streamProtos[i].errorCount = streamStats.getErrorCount();
                    streamProtos[i].firstCaptureLatencyMillis = streamStats.getStartLatencyMs();
                    streamProtos[i].maxHalBuffers = streamStats.getMaxHalBuffers();
                    streamProtos[i].maxAppBuffers = streamStats.getMaxAppBuffers();
                    streamProtos[i].histogramType = streamStats.getHistogramType();
                    streamProtos[i].histogramBins = streamStats.getHistogramBins();
                    streamProtos[i].histogramCounts = streamStats.getHistogramCounts();
                    streamProtos[i].dynamicRangeProfile = streamStats.getDynamicRangeProfile();
                    streamProtos[i].streamUseCase = streamStats.getStreamUseCase();
                }
            }
            FrameworkStatsLog.write((int) FrameworkStatsLog.CAMERA_ACTION_EVENT, e.getDuration(), e.mAPILevel, e.mClientName, facing, e.mCameraId, e.mAction, e.mIsNdk, e.mLatencyMs, e.mOperatingMode, e.mInternalReconfigure, e.mRequestCount, e.mResultErrorCount, e.mDeviceError, streamCount, MessageNano.toByteArray(streamProtos[0]), MessageNano.toByteArray(streamProtos[1]), MessageNano.toByteArray(streamProtos[2]), MessageNano.toByteArray(streamProtos[3]), MessageNano.toByteArray(streamProtos[4]), e.mUserTag, e.mVideoStabilizationMode);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dumpUsageEvents() {
        synchronized (this.mLock) {
            Collections.shuffle(this.mCameraUsageHistory);
            this.mLogWriterService.execute(new EventWriterTask(new ArrayList(this.mCameraUsageHistory)));
            this.mCameraUsageHistory.clear();
        }
        long ident = Binder.clearCallingIdentity();
        try {
            CameraStatsJobService.schedule(this.mContext);
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public ICameraService getCameraServiceRawLocked() {
        if (this.mCameraServiceRaw == null) {
            IBinder cameraServiceBinder = getBinderService(CAMERA_SERVICE_BINDER_NAME);
            if (cameraServiceBinder == null) {
                return null;
            }
            try {
                cameraServiceBinder.linkToDeath(this, 0);
                this.mCameraServiceRaw = ICameraService.Stub.asInterface(cameraServiceBinder);
            } catch (RemoteException e) {
                Slog.w(TAG, "Could not link to death of native camera service");
                return null;
            }
        }
        return this.mCameraServiceRaw;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void switchUserLocked(int userHandle) {
        Set<Integer> currentUserHandles = getEnabledUserHandles(userHandle);
        this.mLastUser = userHandle;
        Set<Integer> set = this.mEnabledCameraUsers;
        if (set == null || !set.equals(currentUserHandles)) {
            this.mEnabledCameraUsers = currentUserHandles;
            notifySwitchWithRetriesLocked(60);
        }
    }

    private Set<Integer> getEnabledUserHandles(int currentUserHandle) {
        int[] userProfiles = this.mUserManager.getEnabledProfileIds(currentUserHandle);
        Set<Integer> handles = new ArraySet<>(userProfiles.length);
        for (int id : userProfiles) {
            handles.add(Integer.valueOf(id));
        }
        return handles;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifySwitchWithRetries(int retries) {
        synchronized (this.mLock) {
            notifySwitchWithRetriesLocked(retries);
        }
    }

    private void notifySwitchWithRetriesLocked(int retries) {
        Set<Integer> set = this.mEnabledCameraUsers;
        if (set == null) {
            return;
        }
        if (notifyCameraserverLocked(1, set)) {
            retries = 0;
        }
        if (retries <= 0) {
            return;
        }
        Slog.i(TAG, "Could not notify camera service of user switch, retrying...");
        Handler handler = this.mHandler;
        handler.sendMessageDelayed(handler.obtainMessage(1, retries - 1, 0, null), 20L);
    }

    private boolean notifyCameraserverLocked(int eventType, Set<Integer> updatedUserHandles) {
        ICameraService cameraService = getCameraServiceRawLocked();
        if (cameraService == null) {
            Slog.w(TAG, "Could not notify cameraserver, camera service not available.");
            return false;
        }
        try {
            this.mCameraServiceRaw.notifySystemEvent(eventType, toArray(updatedUserHandles));
            return true;
        } catch (RemoteException e) {
            Slog.w(TAG, "Could not notify cameraserver, remote exception: " + e);
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifyDeviceStateWithRetries(int retries) {
        synchronized (this.mLock) {
            notifyDeviceStateWithRetriesLocked(retries);
        }
    }

    private void notifyDeviceStateWithRetriesLocked(int retries) {
        if (notifyDeviceStateChangeLocked(this.mDeviceState) || retries <= 0) {
            return;
        }
        Slog.i(TAG, "Could not notify camera service of device state change, retrying...");
        Handler handler = this.mHandler;
        handler.sendMessageDelayed(handler.obtainMessage(2, retries - 1, 0, null), 20L);
    }

    private boolean notifyDeviceStateChangeLocked(int deviceState) {
        ICameraService cameraService = getCameraServiceRawLocked();
        if (cameraService == null) {
            Slog.w(TAG, "Could not notify cameraserver, camera service not available.");
            return false;
        }
        try {
            this.mCameraServiceRaw.notifyDeviceStateChange(deviceState);
            this.mLastReportedDeviceState = deviceState;
            return true;
        } catch (RemoteException e) {
            Slog.w(TAG, "Could not notify cameraserver, remote exception: " + e);
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean notifyUsbDeviceHotplugLocked(UsbDevice device, boolean attached) {
        if (device.getHasVideoCapture()) {
            ICameraService cameraService = getCameraServiceRawLocked();
            if (cameraService == null) {
                Slog.w(TAG, "Could not notify cameraserver, camera service not available.");
                return false;
            }
            int eventType = attached ? 2 : 3;
            try {
                this.mCameraServiceRaw.notifySystemEvent(eventType, new int[]{device.getDeviceId()});
                return true;
            } catch (RemoteException e) {
                Slog.w(TAG, "Could not notify cameraserver, remote exception: " + e);
                return false;
            }
        }
        return false;
    }

    private float getMinFps(CameraSessionStats cameraState) {
        float maxFps = cameraState.getMaxPreviewFps();
        return Math.max(Math.min(maxFps, (float) MAX_PREVIEW_FPS), (float) MIN_PREVIEW_FPS);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateActivityCount(CameraSessionStats cameraState) {
        Object obj;
        boolean alreadyActivePackage;
        String cameraId = cameraState.getCameraId();
        int newCameraState = cameraState.getNewCameraState();
        int facing = cameraState.getFacing();
        String clientName = cameraState.getClientName();
        int apiLevel = cameraState.getApiLevel();
        boolean isNdk = cameraState.isNdk();
        int sessionType = cameraState.getSessionType();
        int internalReconfigureCount = cameraState.getInternalReconfigureCount();
        int latencyMs = cameraState.getLatencyMs();
        long requestCount = cameraState.getRequestCount();
        long resultErrorCount = cameraState.getResultErrorCount();
        boolean deviceError = cameraState.getDeviceErrorFlag();
        List<CameraStreamStats> streamStats = cameraState.getStreamStats();
        String userTag = cameraState.getUserTag();
        int videoStabilizationMode = cameraState.getVideoStabilizationMode();
        Object obj2 = this.mLock;
        synchronized (obj2) {
            try {
                try {
                    boolean wasEmpty = this.mActiveCameraUsage.isEmpty();
                    switch (newCameraState) {
                        case 0:
                            obj = obj2;
                            ITranCameraServiceProxy.Instance().onCameraOpen(this.mHandler, cameraId);
                            AudioManager audioManager = (AudioManager) getContext().getSystemService(AudioManager.class);
                            if (audioManager != null) {
                                String facingStr = facing == 0 ? "back" : "front";
                                String facingParameter = "cameraFacing=" + facingStr;
                                audioManager.setParameters(facingParameter);
                            }
                            int i = 0;
                            while (true) {
                                if (i >= this.mActiveCameraUsage.size()) {
                                    alreadyActivePackage = false;
                                } else if (!this.mActiveCameraUsage.valueAt(i).mClientName.equals(clientName)) {
                                    i++;
                                } else {
                                    alreadyActivePackage = true;
                                }
                            }
                            if (!alreadyActivePackage) {
                                WindowManagerInternal wmi = (WindowManagerInternal) LocalServices.getService(WindowManagerInternal.class);
                                float minFps = getMinFps(cameraState);
                                wmi.addRefreshRateRangeForPackage(clientName, minFps, MAX_PREVIEW_FPS);
                                ITranCameraServiceProxy.Instance().onCameraActive(clientName);
                            }
                            CameraUsageEvent openEvent = new CameraUsageEvent(cameraId, facing, clientName, apiLevel, isNdk, 1, latencyMs, sessionType);
                            this.mCameraUsageHistory.add(openEvent);
                            break;
                        case 1:
                            obj = obj2;
                            CameraUsageEvent newEvent = new CameraUsageEvent(cameraId, facing, clientName, apiLevel, isNdk, 3, latencyMs, sessionType);
                            CameraUsageEvent oldEvent = this.mActiveCameraUsage.put(cameraId, newEvent);
                            if (oldEvent != null) {
                                Slog.w(TAG, "Camera " + cameraId + " was already marked as active");
                                oldEvent.markCompleted(0, 0L, 0L, false, streamStats, "", -1);
                                this.mCameraUsageHistory.add(oldEvent);
                                break;
                            }
                            break;
                        case 2:
                        case 3:
                            ITranCameraServiceProxy.Instance().onCameraClose(this.mHandler, newCameraState, cameraId);
                            CameraUsageEvent doneEvent = this.mActiveCameraUsage.remove(cameraId);
                            if (doneEvent != null) {
                                doneEvent.markCompleted(internalReconfigureCount, requestCount, resultErrorCount, deviceError, streamStats, userTag, videoStabilizationMode);
                                this.mCameraUsageHistory.add(doneEvent);
                                boolean stillActivePackage = false;
                                int i2 = 0;
                                while (true) {
                                    if (i2 < this.mActiveCameraUsage.size()) {
                                        if (!this.mActiveCameraUsage.valueAt(i2).mClientName.equals(clientName)) {
                                            i2++;
                                        } else {
                                            stillActivePackage = true;
                                        }
                                    }
                                }
                                if (!stillActivePackage) {
                                    Handler handler = this.mHandler;
                                    handler.sendMessageDelayed(handler.obtainMessage(4, clientName), 1000L);
                                }
                            }
                            if (newCameraState == 3) {
                                obj = obj2;
                                CameraUsageEvent closeEvent = new CameraUsageEvent(cameraId, facing, clientName, apiLevel, isNdk, 2, latencyMs, sessionType);
                                this.mCameraUsageHistory.add(closeEvent);
                            } else {
                                obj = obj2;
                            }
                            if (this.mCameraUsageHistory.size() > 20) {
                                dumpUsageEvents();
                                break;
                            }
                            break;
                        default:
                            obj = obj2;
                            break;
                    }
                    boolean isEmpty = this.mActiveCameraUsage.isEmpty();
                    if (this.mNotifyNfc && wasEmpty != isEmpty) {
                        notifyNfcService(isEmpty);
                    }
                    return;
                } catch (Throwable th) {
                    th = th;
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
            }
        }
        throw th;
    }

    private void notifyNfcService(boolean enablePolling) {
        IBinder nfcServiceBinder = getBinderService(NFC_SERVICE_BINDER_NAME);
        if (nfcServiceBinder == null) {
            Slog.w(TAG, "Could not connect to NFC service to notify it of camera state");
            return;
        }
        INfcAdapter nfcAdapterRaw = INfcAdapter.Stub.asInterface(nfcServiceBinder);
        int flags = enablePolling ? 0 : 4096;
        try {
            nfcAdapterRaw.setReaderMode(nfcInterfaceToken, (IAppCallback) null, flags, (Bundle) null);
        } catch (RemoteException e) {
            Slog.w(TAG, "Could not notify NFC service, remote exception: " + e);
        }
    }

    private static int[] toArray(Collection<Integer> c) {
        int len = c.size();
        int[] ret = new int[len];
        int idx = 0;
        for (Integer i : c) {
            ret[idx] = i.intValue();
            idx++;
        }
        return ret;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static String cameraStateToString(int newCameraState) {
        switch (newCameraState) {
            case 0:
                return "CAMERA_STATE_OPEN";
            case 1:
                return "CAMERA_STATE_ACTIVE";
            case 2:
                return "CAMERA_STATE_IDLE";
            case 3:
                return "CAMERA_STATE_CLOSED";
            default:
                return "CAMERA_STATE_UNKNOWN";
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static String cameraFacingToString(int cameraFacing) {
        switch (cameraFacing) {
            case 0:
                return "CAMERA_FACING_BACK";
            case 1:
                return "CAMERA_FACING_FRONT";
            case 2:
                return "CAMERA_FACING_EXTERNAL";
            default:
                return "CAMERA_FACING_UNKNOWN";
        }
    }

    private static String cameraHistogramTypeToString(int cameraHistogramType) {
        switch (cameraHistogramType) {
            case 1:
                return "HISTOGRAM_TYPE_CAPTURE_LATENCY";
            default:
                return "HISTOGRAM_TYPE_UNKNOWN";
        }
    }
}
