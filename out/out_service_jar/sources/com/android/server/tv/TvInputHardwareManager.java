package com.android.server.tv;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.hdmi.HdmiDeviceInfo;
import android.hardware.hdmi.HdmiHotplugEvent;
import android.hardware.hdmi.IHdmiControlService;
import android.hardware.hdmi.IHdmiDeviceEventListener;
import android.hardware.hdmi.IHdmiHotplugEventListener;
import android.hardware.hdmi.IHdmiSystemAudioModeChangeListener;
import android.media.AudioDevicePort;
import android.media.AudioDevicePortConfig;
import android.media.AudioFormat;
import android.media.AudioGain;
import android.media.AudioGainConfig;
import android.media.AudioManager;
import android.media.AudioPatch;
import android.media.AudioPort;
import android.media.AudioPortConfig;
import android.media.tv.ITvInputHardware;
import android.media.tv.ITvInputHardwareCallback;
import android.media.tv.TvInputHardwareInfo;
import android.media.tv.TvInputInfo;
import android.media.tv.TvStreamConfig;
import android.media.tv.tunerresourcemanager.ResourceClientProfile;
import android.media.tv.tunerresourcemanager.TunerResourceManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.ArrayMap;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.view.Surface;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.tv.TvInputHal;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class TvInputHardwareManager implements TvInputHal.Callback {
    private static final String TAG = TvInputHardwareManager.class.getSimpleName();
    private final AudioManager mAudioManager;
    private final SparseArray<Connection> mConnections;
    private final Context mContext;
    private int mCurrentIndex;
    private int mCurrentMaxIndex;
    private final TvInputHal mHal;
    private final Handler mHandler;
    private final SparseArray<String> mHardwareInputIdMap;
    private final List<TvInputHardwareInfo> mHardwareList;
    private final IHdmiDeviceEventListener mHdmiDeviceEventListener;
    private final List<HdmiDeviceInfo> mHdmiDeviceList;
    private final IHdmiHotplugEventListener mHdmiHotplugEventListener;
    private final SparseArray<String> mHdmiInputIdMap;
    private final SparseBooleanArray mHdmiStateMap;
    private final IHdmiSystemAudioModeChangeListener mHdmiSystemAudioModeChangeListener;
    private final Map<String, TvInputInfo> mInputMap;
    private final Listener mListener;
    private final Object mLock;
    private final List<Message> mPendingHdmiDeviceEvents;
    private final List<Message> mPendingTvinputInfoEvents;
    private final BroadcastReceiver mVolumeReceiver;

    /* loaded from: classes2.dex */
    interface Listener {
        void onHardwareDeviceAdded(TvInputHardwareInfo tvInputHardwareInfo);

        void onHardwareDeviceRemoved(TvInputHardwareInfo tvInputHardwareInfo);

        void onHdmiDeviceAdded(HdmiDeviceInfo hdmiDeviceInfo);

        void onHdmiDeviceRemoved(HdmiDeviceInfo hdmiDeviceInfo);

        void onHdmiDeviceUpdated(String str, HdmiDeviceInfo hdmiDeviceInfo);

        void onStateChanged(String str, int i);
    }

    public TvInputHardwareManager(Context context, Listener listener) {
        TvInputHal tvInputHal = new TvInputHal(this);
        this.mHal = tvInputHal;
        this.mConnections = new SparseArray<>();
        this.mHardwareList = new ArrayList();
        this.mHdmiDeviceList = new LinkedList();
        this.mHardwareInputIdMap = new SparseArray<>();
        this.mHdmiInputIdMap = new SparseArray<>();
        this.mInputMap = new ArrayMap();
        this.mHdmiHotplugEventListener = new HdmiHotplugEventListener();
        this.mHdmiDeviceEventListener = new HdmiDeviceEventListener();
        this.mHdmiSystemAudioModeChangeListener = new HdmiSystemAudioModeChangeListener();
        this.mVolumeReceiver = new BroadcastReceiver() { // from class: com.android.server.tv.TvInputHardwareManager.1
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent) {
                TvInputHardwareManager.this.handleVolumeChange(context2, intent);
            }
        };
        this.mCurrentIndex = 0;
        this.mCurrentMaxIndex = 0;
        this.mHdmiStateMap = new SparseBooleanArray();
        this.mPendingHdmiDeviceEvents = new LinkedList();
        this.mPendingTvinputInfoEvents = new LinkedList();
        this.mHandler = new ListenerHandler();
        this.mLock = new Object();
        this.mContext = context;
        this.mListener = listener;
        this.mAudioManager = (AudioManager) context.getSystemService("audio");
        tvInputHal.init();
    }

    public void onBootPhase(int phase) {
        if (phase == 500) {
            IHdmiControlService hdmiControlService = IHdmiControlService.Stub.asInterface(ServiceManager.getService("hdmi_control"));
            if (hdmiControlService != null) {
                try {
                    hdmiControlService.addHotplugEventListener(this.mHdmiHotplugEventListener);
                    hdmiControlService.addDeviceEventListener(this.mHdmiDeviceEventListener);
                    hdmiControlService.addSystemAudioModeChangeListener(this.mHdmiSystemAudioModeChangeListener);
                    this.mHdmiDeviceList.addAll(hdmiControlService.getInputDevices());
                } catch (RemoteException e) {
                    Slog.w(TAG, "Error registering listeners to HdmiControlService:", e);
                }
            } else {
                Slog.w(TAG, "HdmiControlService is not available");
            }
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.media.VOLUME_CHANGED_ACTION");
            filter.addAction("android.media.STREAM_MUTE_CHANGED_ACTION");
            this.mContext.registerReceiver(this.mVolumeReceiver, filter);
            updateVolume();
        }
    }

    @Override // com.android.server.tv.TvInputHal.Callback
    public void onDeviceAvailable(TvInputHardwareInfo info, TvStreamConfig[] configs) {
        synchronized (this.mLock) {
            Connection connection = new Connection(info);
            connection.updateConfigsLocked(configs);
            connection.updateCableConnectionStatusLocked(info.getCableConnectionStatus());
            this.mConnections.put(info.getDeviceId(), connection);
            buildHardwareListLocked();
            this.mHandler.obtainMessage(2, 0, 0, info).sendToTarget();
            if (info.getType() == 9) {
                processPendingHdmiDeviceEventsLocked();
            }
        }
    }

    private void buildHardwareListLocked() {
        this.mHardwareList.clear();
        for (int i = 0; i < this.mConnections.size(); i++) {
            this.mHardwareList.add(this.mConnections.valueAt(i).getHardwareInfoLocked());
        }
    }

    @Override // com.android.server.tv.TvInputHal.Callback
    public void onDeviceUnavailable(int deviceId) {
        synchronized (this.mLock) {
            Connection connection = this.mConnections.get(deviceId);
            if (connection == null) {
                Slog.e(TAG, "onDeviceUnavailable: Cannot find a connection with " + deviceId);
                return;
            }
            connection.resetLocked(null, null, null, null, null, null);
            this.mConnections.remove(deviceId);
            buildHardwareListLocked();
            TvInputHardwareInfo info = connection.getHardwareInfoLocked();
            if (info.getType() == 9) {
                Iterator<HdmiDeviceInfo> it = this.mHdmiDeviceList.iterator();
                while (it.hasNext()) {
                    HdmiDeviceInfo deviceInfo = it.next();
                    if (deviceInfo.getPortId() == info.getHdmiPortId()) {
                        this.mHandler.obtainMessage(5, 0, 0, deviceInfo).sendToTarget();
                        it.remove();
                    }
                }
            }
            this.mHandler.obtainMessage(3, 0, 0, info).sendToTarget();
        }
    }

    @Override // com.android.server.tv.TvInputHal.Callback
    public void onStreamConfigurationChanged(int deviceId, TvStreamConfig[] configs, int cableConnectionStatus) {
        synchronized (this.mLock) {
            Connection connection = this.mConnections.get(deviceId);
            if (connection == null) {
                Slog.e(TAG, "StreamConfigurationChanged: Cannot find a connection with " + deviceId);
                return;
            }
            int previousConfigsLength = connection.getConfigsLengthLocked();
            int previousCableConnectionStatus = connection.getInputStateLocked();
            connection.updateConfigsLocked(configs);
            String inputId = this.mHardwareInputIdMap.get(deviceId);
            if (inputId != null) {
                if (connection.updateCableConnectionStatusLocked(cableConnectionStatus)) {
                    if (previousCableConnectionStatus != connection.getInputStateLocked()) {
                        this.mHandler.obtainMessage(1, connection.getInputStateLocked(), 0, inputId).sendToTarget();
                    }
                } else {
                    if ((previousConfigsLength == 0) != (connection.getConfigsLengthLocked() == 0)) {
                        this.mHandler.obtainMessage(1, connection.getInputStateLocked(), 0, inputId).sendToTarget();
                    }
                }
            } else {
                Message msg = this.mHandler.obtainMessage(7, deviceId, cableConnectionStatus, connection);
                Iterator<Message> it = this.mPendingTvinputInfoEvents.iterator();
                while (it.hasNext()) {
                    if (it.next().arg1 == deviceId) {
                        it.remove();
                    }
                }
                this.mPendingTvinputInfoEvents.add(msg);
            }
            ITvInputHardwareCallback callback = connection.getCallbackLocked();
            if (callback != null) {
                try {
                    callback.onStreamConfigChanged(configs);
                } catch (RemoteException e) {
                    Slog.e(TAG, "error in onStreamConfigurationChanged", e);
                }
            }
        }
    }

    @Override // com.android.server.tv.TvInputHal.Callback
    public void onFirstFrameCaptured(int deviceId, int streamId) {
        synchronized (this.mLock) {
            Connection connection = this.mConnections.get(deviceId);
            if (connection == null) {
                Slog.e(TAG, "FirstFrameCaptured: Cannot find a connection with " + deviceId);
                return;
            }
            Runnable runnable = connection.getOnFirstFrameCapturedLocked();
            if (runnable != null) {
                runnable.run();
                connection.setOnFirstFrameCapturedLocked(null);
            }
        }
    }

    public List<TvInputHardwareInfo> getHardwareList() {
        List<TvInputHardwareInfo> unmodifiableList;
        synchronized (this.mLock) {
            unmodifiableList = Collections.unmodifiableList(this.mHardwareList);
        }
        return unmodifiableList;
    }

    public List<HdmiDeviceInfo> getHdmiDeviceList() {
        List<HdmiDeviceInfo> unmodifiableList;
        synchronized (this.mLock) {
            unmodifiableList = Collections.unmodifiableList(this.mHdmiDeviceList);
        }
        return unmodifiableList;
    }

    private boolean checkUidChangedLocked(Connection connection, int callingUid, int resolvedUserId) {
        Integer connectionCallingUid = connection.getCallingUidLocked();
        Integer connectionResolvedUserId = connection.getResolvedUserIdLocked();
        return connectionCallingUid == null || connectionResolvedUserId == null || connectionCallingUid.intValue() != callingUid || connectionResolvedUserId.intValue() != resolvedUserId;
    }

    public void addHardwareInput(int deviceId, TvInputInfo info) {
        String inputId;
        int state;
        synchronized (this.mLock) {
            String oldInputId = this.mHardwareInputIdMap.get(deviceId);
            if (oldInputId != null) {
                Slog.w(TAG, "Trying to override previous registration: old = " + this.mInputMap.get(oldInputId) + ":" + deviceId + ", new = " + info + ":" + deviceId);
            }
            this.mHardwareInputIdMap.put(deviceId, info.getId());
            this.mInputMap.put(info.getId(), info);
            processPendingTvInputInfoEventsLocked();
            Slog.d(TAG, "deviceId =" + deviceId + ", tvinputinfo = " + info);
            for (int i = 0; i < this.mHdmiStateMap.size(); i++) {
                TvInputHardwareInfo hardwareInfo = findHardwareInfoForHdmiPortLocked(this.mHdmiStateMap.keyAt(i));
                if (hardwareInfo != null && (inputId = this.mHardwareInputIdMap.get(hardwareInfo.getDeviceId())) != null && inputId.equals(info.getId())) {
                    if (this.mHdmiStateMap.valueAt(i)) {
                        state = 0;
                    } else {
                        state = 1;
                    }
                    this.mHandler.obtainMessage(1, state, 0, inputId).sendToTarget();
                    return;
                }
            }
            Connection connection = this.mConnections.get(deviceId);
            if (connection != null) {
                this.mHandler.obtainMessage(1, connection.getInputStateLocked(), 0, info.getId()).sendToTarget();
            }
        }
    }

    private static <T> int indexOfEqualValue(SparseArray<T> map, T value) {
        for (int i = 0; i < map.size(); i++) {
            if (map.valueAt(i).equals(value)) {
                return i;
            }
        }
        return -1;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static boolean intArrayContains(int[] array, int value) {
        for (int element : array) {
            if (element == value) {
                return true;
            }
        }
        return false;
    }

    public void addHdmiInput(int id, TvInputInfo info) {
        if (info.getType() != 1007) {
            throw new IllegalArgumentException("info (" + info + ") has non-HDMI type.");
        }
        synchronized (this.mLock) {
            String parentId = info.getParentId();
            int parentIndex = indexOfEqualValue(this.mHardwareInputIdMap, parentId);
            if (parentIndex < 0) {
                throw new IllegalArgumentException("info (" + info + ") has invalid parentId.");
            }
            String oldInputId = this.mHdmiInputIdMap.get(id);
            if (oldInputId != null) {
                Slog.w(TAG, "Trying to override previous registration: old = " + this.mInputMap.get(oldInputId) + ":" + id + ", new = " + info + ":" + id);
            }
            this.mHdmiInputIdMap.put(id, info.getId());
            this.mInputMap.put(info.getId(), info);
        }
    }

    public void removeHardwareInput(String inputId) {
        synchronized (this.mLock) {
            this.mInputMap.remove(inputId);
            int hardwareIndex = indexOfEqualValue(this.mHardwareInputIdMap, inputId);
            if (hardwareIndex >= 0) {
                this.mHardwareInputIdMap.removeAt(hardwareIndex);
            }
            int deviceIndex = indexOfEqualValue(this.mHdmiInputIdMap, inputId);
            if (deviceIndex >= 0) {
                this.mHdmiInputIdMap.removeAt(deviceIndex);
            }
        }
    }

    public ITvInputHardware acquireHardware(int deviceId, ITvInputHardwareCallback callback, TvInputInfo info, int callingUid, int resolvedUserId, String tvInputSessionId, int priorityHint) {
        if (callback == null) {
            throw new NullPointerException();
        }
        TunerResourceManager trm = (TunerResourceManager) this.mContext.getSystemService("tv_tuner_resource_mgr");
        synchronized (this.mLock) {
            try {
                Connection connection = this.mConnections.get(deviceId);
                if (connection == null) {
                    Slog.e(TAG, "Invalid deviceId : " + deviceId);
                    return null;
                }
                ResourceClientProfile profile = new ResourceClientProfile();
                try {
                    profile.tvInputSessionId = tvInputSessionId;
                } catch (Throwable th) {
                    e = th;
                    throw e;
                }
                try {
                    profile.useCase = priorityHint;
                    ResourceClientProfile holderProfile = connection.getResourceClientProfileLocked();
                    if (holderProfile == null || trm == null || trm.isHigherPriority(profile, holderProfile)) {
                        TvInputHardwareImpl hardware = new TvInputHardwareImpl(connection.getHardwareInfoLocked());
                        try {
                            callback.asBinder().linkToDeath(connection, 0);
                            connection.resetLocked(hardware, callback, info, Integer.valueOf(callingUid), Integer.valueOf(resolvedUserId), profile);
                            return connection.getHardwareLocked();
                        } catch (RemoteException e) {
                            hardware.release();
                            return null;
                        }
                    }
                    Slog.d(TAG, "Acquiring does not show higher priority than the current holder. Device id:" + deviceId);
                    return null;
                } catch (Throwable th2) {
                    e = th2;
                    throw e;
                }
            } catch (Throwable th3) {
                e = th3;
            }
        }
    }

    public void releaseHardware(int deviceId, ITvInputHardware hardware, int callingUid, int resolvedUserId) {
        synchronized (this.mLock) {
            Connection connection = this.mConnections.get(deviceId);
            if (connection == null) {
                Slog.e(TAG, "Invalid deviceId : " + deviceId);
                return;
            }
            if (connection.getHardwareLocked() == hardware && !checkUidChangedLocked(connection, callingUid, resolvedUserId)) {
                ITvInputHardwareCallback callback = connection.getCallbackLocked();
                if (callback != null) {
                    callback.asBinder().unlinkToDeath(connection, 0);
                }
                connection.resetLocked(null, null, null, null, null, null);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public TvInputHardwareInfo findHardwareInfoForHdmiPortLocked(int port) {
        for (TvInputHardwareInfo hardwareInfo : this.mHardwareList) {
            if (hardwareInfo.getType() == 9 && hardwareInfo.getHdmiPortId() == port) {
                return hardwareInfo;
            }
        }
        return null;
    }

    private int findDeviceIdForInputIdLocked(String inputId) {
        for (int i = 0; i < this.mConnections.size(); i++) {
            Connection connection = this.mConnections.get(i);
            if (connection.getInfoLocked().getId().equals(inputId)) {
                return i;
            }
        }
        return -1;
    }

    public List<TvStreamConfig> getAvailableTvStreamConfigList(String inputId, int callingUid, int resolvedUserId) {
        TvStreamConfig[] configsLocked;
        List<TvStreamConfig> configsList = new ArrayList<>();
        synchronized (this.mLock) {
            int deviceId = findDeviceIdForInputIdLocked(inputId);
            if (deviceId < 0) {
                Slog.e(TAG, "Invalid inputId : " + inputId);
                return configsList;
            }
            Connection connection = this.mConnections.get(deviceId);
            for (TvStreamConfig config : connection.getConfigsLocked()) {
                if (config.getType() == 2) {
                    configsList.add(config);
                }
            }
            return configsList;
        }
    }

    public boolean captureFrame(String inputId, Surface surface, final TvStreamConfig config, int callingUid, int resolvedUserId) {
        synchronized (this.mLock) {
            int deviceId = findDeviceIdForInputIdLocked(inputId);
            if (deviceId < 0) {
                Slog.e(TAG, "Invalid inputId : " + inputId);
                return false;
            }
            Connection connection = this.mConnections.get(deviceId);
            final TvInputHardwareImpl hardwareImpl = connection.getHardwareImplLocked();
            if (hardwareImpl == null) {
                return false;
            }
            Runnable runnable = connection.getOnFirstFrameCapturedLocked();
            if (runnable != null) {
                runnable.run();
                connection.setOnFirstFrameCapturedLocked(null);
            }
            boolean result = hardwareImpl.startCapture(surface, config);
            if (result) {
                connection.setOnFirstFrameCapturedLocked(new Runnable() { // from class: com.android.server.tv.TvInputHardwareManager.2
                    @Override // java.lang.Runnable
                    public void run() {
                        hardwareImpl.stopCapture(config);
                    }
                });
            }
            return result;
        }
    }

    private void processPendingHdmiDeviceEventsLocked() {
        Iterator<Message> it = this.mPendingHdmiDeviceEvents.iterator();
        while (it.hasNext()) {
            Message msg = it.next();
            HdmiDeviceInfo deviceInfo = (HdmiDeviceInfo) msg.obj;
            TvInputHardwareInfo hardwareInfo = findHardwareInfoForHdmiPortLocked(deviceInfo.getPortId());
            if (hardwareInfo != null) {
                msg.sendToTarget();
                it.remove();
            }
        }
    }

    private void processPendingTvInputInfoEventsLocked() {
        Iterator<Message> it = this.mPendingTvinputInfoEvents.iterator();
        while (it.hasNext()) {
            Message msg = it.next();
            int deviceId = msg.arg1;
            String inputId = this.mHardwareInputIdMap.get(deviceId);
            if (inputId != null) {
                msg.sendToTarget();
                it.remove();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateVolume() {
        this.mCurrentMaxIndex = this.mAudioManager.getStreamMaxVolume(3);
        this.mCurrentIndex = this.mAudioManager.getStreamVolume(3);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    public void handleVolumeChange(Context context, Intent intent) {
        boolean z;
        int index;
        String action = intent.getAction();
        switch (action.hashCode()) {
            case -1940635523:
                if (action.equals("android.media.VOLUME_CHANGED_ACTION")) {
                    z = false;
                    break;
                }
                z = true;
                break;
            case 1920758225:
                if (action.equals("android.media.STREAM_MUTE_CHANGED_ACTION")) {
                    z = true;
                    break;
                }
                z = true;
                break;
            default:
                z = true;
                break;
        }
        switch (z) {
            case false:
                int streamType = intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_TYPE", -1);
                if (streamType != 3 || (index = intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_VALUE", 0)) == this.mCurrentIndex) {
                    return;
                }
                this.mCurrentIndex = index;
                break;
            case true:
                int streamType2 = intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_TYPE", -1);
                if (streamType2 != 3) {
                    return;
                }
                break;
            default:
                Slog.w(TAG, "Unrecognized intent: " + intent);
                return;
        }
        synchronized (this.mLock) {
            for (int i = 0; i < this.mConnections.size(); i++) {
                TvInputHardwareImpl hardwareImpl = this.mConnections.valueAt(i).getHardwareImplLocked();
                if (hardwareImpl != null) {
                    hardwareImpl.onMediaStreamVolumeChanged();
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public float getMediaStreamVolume() {
        return this.mCurrentIndex / this.mCurrentMaxIndex;
    }

    public void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
        IndentingPrintWriter pw = new IndentingPrintWriter(writer, "  ");
        if (DumpUtils.checkDumpPermission(this.mContext, TAG, pw)) {
            synchronized (this.mLock) {
                pw.println("TvInputHardwareManager Info:");
                pw.increaseIndent();
                pw.println("mConnections: deviceId -> Connection");
                pw.increaseIndent();
                for (int i = 0; i < this.mConnections.size(); i++) {
                    int deviceId = this.mConnections.keyAt(i);
                    Connection mConnection = this.mConnections.valueAt(i);
                    pw.println(deviceId + ": " + mConnection);
                }
                pw.decreaseIndent();
                pw.println("mHardwareList:");
                pw.increaseIndent();
                for (TvInputHardwareInfo tvInputHardwareInfo : this.mHardwareList) {
                    pw.println(tvInputHardwareInfo);
                }
                pw.decreaseIndent();
                pw.println("mHdmiDeviceList:");
                pw.increaseIndent();
                for (HdmiDeviceInfo hdmiDeviceInfo : this.mHdmiDeviceList) {
                    pw.println(hdmiDeviceInfo);
                }
                pw.decreaseIndent();
                pw.println("mHardwareInputIdMap: deviceId -> inputId");
                pw.increaseIndent();
                for (int i2 = 0; i2 < this.mHardwareInputIdMap.size(); i2++) {
                    int deviceId2 = this.mHardwareInputIdMap.keyAt(i2);
                    String inputId = this.mHardwareInputIdMap.valueAt(i2);
                    pw.println(deviceId2 + ": " + inputId);
                }
                pw.decreaseIndent();
                pw.println("mHdmiInputIdMap: id -> inputId");
                pw.increaseIndent();
                for (int i3 = 0; i3 < this.mHdmiInputIdMap.size(); i3++) {
                    int id = this.mHdmiInputIdMap.keyAt(i3);
                    String inputId2 = this.mHdmiInputIdMap.valueAt(i3);
                    pw.println(id + ": " + inputId2);
                }
                pw.decreaseIndent();
                pw.println("mInputMap: inputId -> inputInfo");
                pw.increaseIndent();
                for (Map.Entry<String, TvInputInfo> entry : this.mInputMap.entrySet()) {
                    pw.println(entry.getKey() + ": " + entry.getValue());
                }
                pw.decreaseIndent();
                pw.decreaseIndent();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class Connection implements IBinder.DeathRecipient {
        private ITvInputHardwareCallback mCallback;
        private TvInputHardwareInfo mHardwareInfo;
        private TvInputInfo mInfo;
        private Runnable mOnFirstFrameCaptured;
        private TvInputHardwareImpl mHardware = null;
        private TvStreamConfig[] mConfigs = null;
        private Integer mCallingUid = null;
        private Integer mResolvedUserId = null;
        private ResourceClientProfile mResourceClientProfile = null;
        private boolean mIsCableConnectionStatusUpdated = false;

        public Connection(TvInputHardwareInfo hardwareInfo) {
            this.mHardwareInfo = hardwareInfo;
        }

        public void resetLocked(TvInputHardwareImpl hardware, ITvInputHardwareCallback callback, TvInputInfo info, Integer callingUid, Integer resolvedUserId, ResourceClientProfile profile) {
            if (this.mHardware != null) {
                try {
                    this.mCallback.onReleased();
                } catch (RemoteException e) {
                    Slog.e(TvInputHardwareManager.TAG, "error in Connection::resetLocked", e);
                }
                this.mHardware.release();
            }
            this.mHardware = hardware;
            this.mCallback = callback;
            this.mInfo = info;
            this.mCallingUid = callingUid;
            this.mResolvedUserId = resolvedUserId;
            this.mOnFirstFrameCaptured = null;
            this.mResourceClientProfile = profile;
            if (hardware != null && callback != null) {
                try {
                    callback.onStreamConfigChanged(getConfigsLocked());
                } catch (RemoteException e2) {
                    Slog.e(TvInputHardwareManager.TAG, "error in Connection::resetLocked", e2);
                }
            }
        }

        public void updateConfigsLocked(TvStreamConfig[] configs) {
            this.mConfigs = configs;
        }

        public TvInputHardwareInfo getHardwareInfoLocked() {
            return this.mHardwareInfo;
        }

        public TvInputInfo getInfoLocked() {
            return this.mInfo;
        }

        public ITvInputHardware getHardwareLocked() {
            return this.mHardware;
        }

        public TvInputHardwareImpl getHardwareImplLocked() {
            return this.mHardware;
        }

        public ITvInputHardwareCallback getCallbackLocked() {
            return this.mCallback;
        }

        public TvStreamConfig[] getConfigsLocked() {
            return this.mConfigs;
        }

        public Integer getCallingUidLocked() {
            return this.mCallingUid;
        }

        public Integer getResolvedUserIdLocked() {
            return this.mResolvedUserId;
        }

        public void setOnFirstFrameCapturedLocked(Runnable runnable) {
            this.mOnFirstFrameCaptured = runnable;
        }

        public Runnable getOnFirstFrameCapturedLocked() {
            return this.mOnFirstFrameCaptured;
        }

        public ResourceClientProfile getResourceClientProfileLocked() {
            return this.mResourceClientProfile;
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            synchronized (TvInputHardwareManager.this.mLock) {
                resetLocked(null, null, null, null, null, null);
            }
        }

        public String toString() {
            return "Connection{ mHardwareInfo: " + this.mHardwareInfo + ", mInfo: " + this.mInfo + ", mCallback: " + this.mCallback + ", mConfigs: " + Arrays.toString(this.mConfigs) + ", mCallingUid: " + this.mCallingUid + ", mResolvedUserId: " + this.mResolvedUserId + ", mResourceClientProfile: " + this.mResourceClientProfile + " }";
        }

        public boolean updateCableConnectionStatusLocked(int cableConnectionStatus) {
            if (cableConnectionStatus != 0 || this.mIsCableConnectionStatusUpdated) {
                this.mIsCableConnectionStatusUpdated = true;
                this.mHardwareInfo = this.mHardwareInfo.toBuilder().cableConnectionStatus(cableConnectionStatus).build();
            }
            return this.mIsCableConnectionStatusUpdated;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public int getConfigsLengthLocked() {
            TvStreamConfig[] tvStreamConfigArr = this.mConfigs;
            if (tvStreamConfigArr == null) {
                return 0;
            }
            return tvStreamConfigArr.length;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public int getInputStateLocked() {
            int configsLength = getConfigsLengthLocked();
            if (configsLength <= 0 || this.mIsCableConnectionStatusUpdated) {
                switch (this.mHardwareInfo.getCableConnectionStatus()) {
                    case 1:
                        return 0;
                    case 2:
                        return 2;
                    default:
                        return 1;
                }
            }
            return 0;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class TvInputHardwareImpl extends ITvInputHardware.Stub {
        private TvStreamConfig mActiveConfig;
        private final AudioManager.OnAudioPortUpdateListener mAudioListener;
        private AudioPatch mAudioPatch;
        private List<AudioDevicePort> mAudioSink;
        private AudioDevicePort mAudioSource;
        private float mCommittedVolume;
        private int mDesiredChannelMask;
        private int mDesiredFormat;
        private int mDesiredSamplingRate;
        private final TvInputHardwareInfo mInfo;
        private String mOverrideAudioAddress;
        private int mOverrideAudioType;
        private float mSourceVolume;
        private boolean mReleased = false;
        private final Object mImplLock = new Object();

        public TvInputHardwareImpl(TvInputHardwareInfo info) {
            AudioManager.OnAudioPortUpdateListener onAudioPortUpdateListener = new AudioManager.OnAudioPortUpdateListener() { // from class: com.android.server.tv.TvInputHardwareManager.TvInputHardwareImpl.1
                public void onAudioPortListUpdate(AudioPort[] portList) {
                    synchronized (TvInputHardwareImpl.this.mImplLock) {
                        TvInputHardwareImpl.this.updateAudioConfigLocked();
                    }
                }

                public void onAudioPatchListUpdate(AudioPatch[] patchList) {
                }

                public void onServiceDied() {
                    synchronized (TvInputHardwareImpl.this.mImplLock) {
                        TvInputHardwareImpl.this.mAudioSource = null;
                        TvInputHardwareImpl.this.mAudioSink.clear();
                        if (TvInputHardwareImpl.this.mAudioPatch != null) {
                            AudioManager unused = TvInputHardwareManager.this.mAudioManager;
                            AudioManager.releaseAudioPatch(TvInputHardwareImpl.this.mAudioPatch);
                            TvInputHardwareImpl.this.mAudioPatch = null;
                        }
                    }
                }
            };
            this.mAudioListener = onAudioPortUpdateListener;
            this.mOverrideAudioType = 0;
            this.mOverrideAudioAddress = "";
            this.mAudioSink = new ArrayList();
            this.mAudioPatch = null;
            this.mCommittedVolume = -1.0f;
            this.mSourceVolume = 0.0f;
            this.mActiveConfig = null;
            this.mDesiredSamplingRate = 0;
            this.mDesiredChannelMask = 1;
            this.mDesiredFormat = 1;
            this.mInfo = info;
            TvInputHardwareManager.this.mAudioManager.registerAudioPortUpdateListener(onAudioPortUpdateListener);
            if (info.getAudioType() != 0) {
                this.mAudioSource = findAudioDevicePort(info.getAudioType(), info.getAudioAddress());
                findAudioSinkFromAudioPolicy(this.mAudioSink);
            }
        }

        private void findAudioSinkFromAudioPolicy(List<AudioDevicePort> sinks) {
            sinks.clear();
            ArrayList<AudioDevicePort> devicePorts = new ArrayList<>();
            AudioManager unused = TvInputHardwareManager.this.mAudioManager;
            if (AudioManager.listAudioDevicePorts(devicePorts) != 0) {
                return;
            }
            int sinkDevice = TvInputHardwareManager.this.mAudioManager.getDevicesForStream(3);
            Iterator<AudioDevicePort> it = devicePorts.iterator();
            while (it.hasNext()) {
                AudioDevicePort port = it.next();
                if ((port.type() & sinkDevice) != 0 && (port.type() & Integer.MIN_VALUE) == 0) {
                    sinks.add(port);
                }
            }
        }

        private AudioDevicePort findAudioDevicePort(int type, String address) {
            if (type == 0) {
                return null;
            }
            ArrayList<AudioDevicePort> devicePorts = new ArrayList<>();
            AudioManager unused = TvInputHardwareManager.this.mAudioManager;
            if (AudioManager.listAudioDevicePorts(devicePorts) != 0) {
                return null;
            }
            Iterator<AudioDevicePort> it = devicePorts.iterator();
            while (it.hasNext()) {
                AudioDevicePort port = it.next();
                if (port.type() == type && port.address().equals(address)) {
                    return port;
                }
            }
            return null;
        }

        public void release() {
            synchronized (this.mImplLock) {
                TvInputHardwareManager.this.mAudioManager.unregisterAudioPortUpdateListener(this.mAudioListener);
                if (this.mAudioPatch != null) {
                    AudioManager unused = TvInputHardwareManager.this.mAudioManager;
                    AudioManager.releaseAudioPatch(this.mAudioPatch);
                    this.mAudioPatch = null;
                }
                this.mReleased = true;
            }
        }

        public boolean setSurface(Surface surface, TvStreamConfig config) throws RemoteException {
            synchronized (this.mImplLock) {
                if (this.mReleased) {
                    throw new IllegalStateException("Device already released.");
                }
                int result = 0;
                boolean z = true;
                if (surface == null) {
                    if (this.mActiveConfig == null) {
                        return true;
                    }
                    result = TvInputHardwareManager.this.mHal.removeStream(this.mInfo.getDeviceId(), this.mActiveConfig);
                    this.mActiveConfig = null;
                } else if (config == null) {
                    return false;
                } else {
                    TvStreamConfig tvStreamConfig = this.mActiveConfig;
                    if (tvStreamConfig != null && !config.equals(tvStreamConfig) && (result = TvInputHardwareManager.this.mHal.removeStream(this.mInfo.getDeviceId(), this.mActiveConfig)) != 0) {
                        this.mActiveConfig = null;
                    }
                    if (result == 0 && (result = TvInputHardwareManager.this.mHal.addOrUpdateStream(this.mInfo.getDeviceId(), surface, config)) == 0) {
                        this.mActiveConfig = config;
                    }
                }
                updateAudioConfigLocked();
                if (result != 0) {
                    z = false;
                }
                return z;
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void updateAudioConfigLocked() {
            char c;
            int gainValue;
            boolean sinkUpdated = updateAudioSinkLocked();
            boolean sourceUpdated = updateAudioSourceLocked();
            if (this.mAudioSource == null || this.mAudioSink.isEmpty() || this.mActiveConfig == null) {
                if (this.mAudioPatch != null) {
                    AudioManager unused = TvInputHardwareManager.this.mAudioManager;
                    AudioManager.releaseAudioPatch(this.mAudioPatch);
                    this.mAudioPatch = null;
                    return;
                }
                return;
            }
            TvInputHardwareManager.this.updateVolume();
            float volume = this.mSourceVolume * TvInputHardwareManager.this.getMediaStreamVolume();
            AudioGainConfig sourceGainConfig = null;
            int i = 1;
            if (this.mAudioSource.gains().length > 0 && volume != this.mCommittedVolume) {
                AudioGain sourceGain = null;
                AudioGain[] gains = this.mAudioSource.gains();
                int length = gains.length;
                int i2 = 0;
                while (true) {
                    if (i2 >= length) {
                        break;
                    }
                    AudioGain gain = gains[i2];
                    if ((gain.mode() & 1) == 0) {
                        i2++;
                    } else {
                        sourceGain = gain;
                        break;
                    }
                }
                if (sourceGain != null) {
                    int steps = (sourceGain.maxValue() - sourceGain.minValue()) / sourceGain.stepValue();
                    int gainValue2 = sourceGain.minValue();
                    if (volume < 1.0f) {
                        gainValue = gainValue2 + (sourceGain.stepValue() * ((int) ((steps * volume) + 0.5d)));
                    } else {
                        gainValue = sourceGain.maxValue();
                    }
                    int[] gainValues = {gainValue};
                    sourceGainConfig = sourceGain.buildConfig(1, sourceGain.channelMask(), gainValues, 0);
                } else {
                    Slog.w(TvInputHardwareManager.TAG, "No audio source gain with MODE_JOINT support exists.");
                }
            }
            AudioPortConfig sourceConfig = this.mAudioSource.activeConfig();
            List<AudioPortConfig> sinkConfigs = new ArrayList<>();
            AudioPatch audioPatch = this.mAudioPatch;
            AudioPatch[] audioPatchArray = {audioPatch};
            boolean shouldRecreateAudioPatch = sourceUpdated || sinkUpdated || audioPatch == null;
            for (AudioDevicePort audioSink : this.mAudioSink) {
                AudioDevicePortConfig activeConfig = audioSink.activeConfig();
                int sinkSamplingRate = this.mDesiredSamplingRate;
                int sinkChannelMask = this.mDesiredChannelMask;
                int sinkFormat = this.mDesiredFormat;
                if (activeConfig != null) {
                    if (sinkSamplingRate == 0) {
                        sinkSamplingRate = activeConfig.samplingRate();
                    }
                    if (sinkChannelMask == i) {
                        sinkChannelMask = activeConfig.channelMask();
                    }
                    if (sinkFormat == i) {
                        sinkFormat = activeConfig.format();
                    }
                }
                if (activeConfig == null || activeConfig.samplingRate() != sinkSamplingRate || activeConfig.channelMask() != sinkChannelMask || activeConfig.format() != sinkFormat) {
                    if (!TvInputHardwareManager.intArrayContains(audioSink.samplingRates(), sinkSamplingRate) && audioSink.samplingRates().length > 0) {
                        sinkSamplingRate = audioSink.samplingRates()[0];
                    }
                    if (!TvInputHardwareManager.intArrayContains(audioSink.channelMasks(), sinkChannelMask)) {
                        sinkChannelMask = 1;
                    }
                    if (!TvInputHardwareManager.intArrayContains(audioSink.formats(), sinkFormat)) {
                        sinkFormat = 1;
                    }
                    activeConfig = audioSink.buildConfig(sinkSamplingRate, sinkChannelMask, sinkFormat, (AudioGainConfig) null);
                    shouldRecreateAudioPatch = true;
                }
                sinkConfigs.add(activeConfig);
                i = 1;
            }
            AudioPortConfig sinkConfig = sinkConfigs.get(0);
            if (sourceConfig == null || sourceGainConfig != null) {
                int sourceSamplingRate = 0;
                if (TvInputHardwareManager.intArrayContains(this.mAudioSource.samplingRates(), sinkConfig.samplingRate())) {
                    sourceSamplingRate = sinkConfig.samplingRate();
                } else if (this.mAudioSource.samplingRates().length > 0) {
                    sourceSamplingRate = this.mAudioSource.samplingRates()[0];
                }
                int sourceChannelMask = 1;
                int[] channelMasks = this.mAudioSource.channelMasks();
                int length2 = channelMasks.length;
                int i3 = 0;
                while (true) {
                    if (i3 >= length2) {
                        break;
                    }
                    int inChannelMask = channelMasks[i3];
                    AudioPortConfig sourceConfig2 = sourceConfig;
                    int channelCountFromOutChannelMask = AudioFormat.channelCountFromOutChannelMask(sinkConfig.channelMask());
                    int sourceChannelMask2 = sourceChannelMask;
                    int sourceChannelMask3 = AudioFormat.channelCountFromInChannelMask(inChannelMask);
                    if (channelCountFromOutChannelMask != sourceChannelMask3) {
                        i3++;
                        sourceChannelMask = sourceChannelMask2;
                        sourceConfig = sourceConfig2;
                    } else {
                        sourceChannelMask = inChannelMask;
                        break;
                    }
                }
                int sourceFormat = 1;
                if (TvInputHardwareManager.intArrayContains(this.mAudioSource.formats(), sinkConfig.format())) {
                    sourceFormat = sinkConfig.format();
                }
                shouldRecreateAudioPatch = true;
                sourceConfig = this.mAudioSource.buildConfig(sourceSamplingRate, sourceChannelMask, sourceFormat, sourceGainConfig);
            }
            if (shouldRecreateAudioPatch) {
                this.mCommittedVolume = volume;
                AudioPatch audioPatch2 = this.mAudioPatch;
                if (audioPatch2 == null || sinkUpdated || sourceUpdated) {
                    if (audioPatch2 == null) {
                        c = 0;
                    } else {
                        AudioManager unused2 = TvInputHardwareManager.this.mAudioManager;
                        AudioManager.releaseAudioPatch(this.mAudioPatch);
                        c = 0;
                        audioPatchArray[0] = null;
                    }
                    AudioManager unused3 = TvInputHardwareManager.this.mAudioManager;
                    AudioPortConfig[] audioPortConfigArr = new AudioPortConfig[1];
                    audioPortConfigArr[c] = sourceConfig;
                    AudioManager.createAudioPatch(audioPatchArray, audioPortConfigArr, (AudioPortConfig[]) sinkConfigs.toArray(new AudioPortConfig[sinkConfigs.size()]));
                    this.mAudioPatch = audioPatchArray[c];
                }
            }
            if (sourceGainConfig != null) {
                AudioManager unused4 = TvInputHardwareManager.this.mAudioManager;
                AudioManager.setAudioPortGain(this.mAudioSource, sourceGainConfig);
            }
        }

        public void setStreamVolume(float volume) throws RemoteException {
            synchronized (this.mImplLock) {
                if (this.mReleased) {
                    throw new IllegalStateException("Device already released.");
                }
                this.mSourceVolume = volume;
                updateAudioConfigLocked();
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public boolean startCapture(Surface surface, TvStreamConfig config) {
            synchronized (this.mImplLock) {
                if (this.mReleased) {
                    return false;
                }
                if (surface != null && config != null) {
                    if (config.getType() != 2) {
                        return false;
                    }
                    int result = TvInputHardwareManager.this.mHal.addOrUpdateStream(this.mInfo.getDeviceId(), surface, config);
                    return result == 0;
                }
                return false;
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public boolean stopCapture(TvStreamConfig config) {
            synchronized (this.mImplLock) {
                if (this.mReleased) {
                    return false;
                }
                if (config == null) {
                    return false;
                }
                int result = TvInputHardwareManager.this.mHal.removeStream(this.mInfo.getDeviceId(), config);
                return result == 0;
            }
        }

        private boolean updateAudioSourceLocked() {
            if (this.mInfo.getAudioType() == 0) {
                return false;
            }
            AudioDevicePort previousSource = this.mAudioSource;
            AudioDevicePort findAudioDevicePort = findAudioDevicePort(this.mInfo.getAudioType(), this.mInfo.getAudioAddress());
            this.mAudioSource = findAudioDevicePort;
            return findAudioDevicePort == null ? previousSource != null : !findAudioDevicePort.equals(previousSource);
        }

        private boolean updateAudioSinkLocked() {
            if (this.mInfo.getAudioType() == 0) {
                return false;
            }
            List<AudioDevicePort> previousSink = this.mAudioSink;
            ArrayList arrayList = new ArrayList();
            this.mAudioSink = arrayList;
            int i = this.mOverrideAudioType;
            if (i == 0) {
                findAudioSinkFromAudioPolicy(arrayList);
            } else {
                AudioDevicePort audioSink = findAudioDevicePort(i, this.mOverrideAudioAddress);
                if (audioSink != null) {
                    this.mAudioSink.add(audioSink);
                }
            }
            if (this.mAudioSink.size() != previousSink.size()) {
                return true;
            }
            previousSink.removeAll(this.mAudioSink);
            return !previousSink.isEmpty();
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void handleAudioSinkUpdated() {
            synchronized (this.mImplLock) {
                updateAudioConfigLocked();
            }
        }

        public void overrideAudioSink(int audioType, String audioAddress, int samplingRate, int channelMask, int format) {
            synchronized (this.mImplLock) {
                this.mOverrideAudioType = audioType;
                this.mOverrideAudioAddress = audioAddress;
                this.mDesiredSamplingRate = samplingRate;
                this.mDesiredChannelMask = channelMask;
                this.mDesiredFormat = format;
                updateAudioConfigLocked();
            }
        }

        public void onMediaStreamVolumeChanged() {
            synchronized (this.mImplLock) {
                updateAudioConfigLocked();
            }
        }
    }

    /* loaded from: classes2.dex */
    private class ListenerHandler extends Handler {
        private static final int HARDWARE_DEVICE_ADDED = 2;
        private static final int HARDWARE_DEVICE_REMOVED = 3;
        private static final int HDMI_DEVICE_ADDED = 4;
        private static final int HDMI_DEVICE_REMOVED = 5;
        private static final int HDMI_DEVICE_UPDATED = 6;
        private static final int STATE_CHANGED = 1;
        private static final int TVINPUT_INFO_ADDED = 7;

        private ListenerHandler() {
        }

        @Override // android.os.Handler
        public final void handleMessage(Message msg) {
            String inputId;
            switch (msg.what) {
                case 1:
                    int state = msg.arg1;
                    TvInputHardwareManager.this.mListener.onStateChanged((String) msg.obj, state);
                    return;
                case 2:
                    TvInputHardwareInfo info = (TvInputHardwareInfo) msg.obj;
                    TvInputHardwareManager.this.mListener.onHardwareDeviceAdded(info);
                    return;
                case 3:
                    TvInputHardwareInfo info2 = (TvInputHardwareInfo) msg.obj;
                    TvInputHardwareManager.this.mListener.onHardwareDeviceRemoved(info2);
                    return;
                case 4:
                    HdmiDeviceInfo info3 = (HdmiDeviceInfo) msg.obj;
                    TvInputHardwareManager.this.mListener.onHdmiDeviceAdded(info3);
                    return;
                case 5:
                    HdmiDeviceInfo info4 = (HdmiDeviceInfo) msg.obj;
                    TvInputHardwareManager.this.mListener.onHdmiDeviceRemoved(info4);
                    return;
                case 6:
                    HdmiDeviceInfo info5 = (HdmiDeviceInfo) msg.obj;
                    synchronized (TvInputHardwareManager.this.mLock) {
                        inputId = (String) TvInputHardwareManager.this.mHdmiInputIdMap.get(info5.getId());
                    }
                    if (inputId != null) {
                        TvInputHardwareManager.this.mListener.onHdmiDeviceUpdated(inputId, info5);
                        return;
                    } else {
                        Slog.w(TvInputHardwareManager.TAG, "Could not resolve input ID matching the device info; ignoring.");
                        return;
                    }
                case 7:
                    int deviceId = msg.arg1;
                    int cableConnectionStatus = msg.arg2;
                    Connection connection = (Connection) msg.obj;
                    int previousConfigsLength = connection.getConfigsLengthLocked();
                    int previousCableConnectionStatus = connection.getInputStateLocked();
                    String inputId2 = (String) TvInputHardwareManager.this.mHardwareInputIdMap.get(deviceId);
                    if (inputId2 != null) {
                        if (connection.updateCableConnectionStatusLocked(cableConnectionStatus)) {
                            if (previousCableConnectionStatus != connection.getInputStateLocked()) {
                                TvInputHardwareManager.this.mHandler.obtainMessage(1, connection.getInputStateLocked(), 0, inputId2).sendToTarget();
                                return;
                            }
                            return;
                        }
                        if ((previousConfigsLength == 0) != (connection.getConfigsLengthLocked() == 0)) {
                            TvInputHardwareManager.this.mHandler.obtainMessage(1, connection.getInputStateLocked(), 0, inputId2).sendToTarget();
                            return;
                        }
                        return;
                    }
                    return;
                default:
                    Slog.w(TvInputHardwareManager.TAG, "Unhandled message: " + msg);
                    return;
            }
        }
    }

    /* loaded from: classes2.dex */
    private final class HdmiHotplugEventListener extends IHdmiHotplugEventListener.Stub {
        private HdmiHotplugEventListener() {
        }

        public void onReceived(HdmiHotplugEvent event) {
            int state;
            synchronized (TvInputHardwareManager.this.mLock) {
                TvInputHardwareManager.this.mHdmiStateMap.put(event.getPort(), event.isConnected());
                TvInputHardwareInfo hardwareInfo = TvInputHardwareManager.this.findHardwareInfoForHdmiPortLocked(event.getPort());
                if (hardwareInfo == null) {
                    return;
                }
                String inputId = (String) TvInputHardwareManager.this.mHardwareInputIdMap.get(hardwareInfo.getDeviceId());
                if (inputId == null) {
                    return;
                }
                if (event.isConnected()) {
                    state = 0;
                } else {
                    state = 1;
                }
                TvInputHardwareManager.this.mHandler.obtainMessage(1, state, 0, inputId).sendToTarget();
            }
        }
    }

    /* loaded from: classes2.dex */
    private final class HdmiDeviceEventListener extends IHdmiDeviceEventListener.Stub {
        private HdmiDeviceEventListener() {
        }

        public void onStatusChanged(HdmiDeviceInfo deviceInfo, int status) {
            if (deviceInfo.isSourceType()) {
                synchronized (TvInputHardwareManager.this.mLock) {
                    int messageType = 0;
                    Object obj = null;
                    switch (status) {
                        case 1:
                            if (findHdmiDeviceInfo(deviceInfo.getId()) == null) {
                                TvInputHardwareManager.this.mHdmiDeviceList.add(deviceInfo);
                                messageType = 4;
                                obj = deviceInfo;
                                break;
                            } else {
                                Slog.w(TvInputHardwareManager.TAG, "The list already contains " + deviceInfo + "; ignoring.");
                                return;
                            }
                        case 2:
                            HdmiDeviceInfo originalDeviceInfo = findHdmiDeviceInfo(deviceInfo.getId());
                            if (!TvInputHardwareManager.this.mHdmiDeviceList.remove(originalDeviceInfo)) {
                                Slog.w(TvInputHardwareManager.TAG, "The list doesn't contain " + deviceInfo + "; ignoring.");
                                return;
                            }
                            messageType = 5;
                            obj = deviceInfo;
                            break;
                        case 3:
                            HdmiDeviceInfo originalDeviceInfo2 = findHdmiDeviceInfo(deviceInfo.getId());
                            if (!TvInputHardwareManager.this.mHdmiDeviceList.remove(originalDeviceInfo2)) {
                                Slog.w(TvInputHardwareManager.TAG, "The list doesn't contain " + deviceInfo + "; ignoring.");
                                return;
                            }
                            TvInputHardwareManager.this.mHdmiDeviceList.add(deviceInfo);
                            messageType = 6;
                            obj = deviceInfo;
                            break;
                    }
                    Message msg = TvInputHardwareManager.this.mHandler.obtainMessage(messageType, 0, 0, obj);
                    if (TvInputHardwareManager.this.findHardwareInfoForHdmiPortLocked(deviceInfo.getPortId()) != null) {
                        msg.sendToTarget();
                    } else {
                        TvInputHardwareManager.this.mPendingHdmiDeviceEvents.add(msg);
                    }
                }
            }
        }

        private HdmiDeviceInfo findHdmiDeviceInfo(int id) {
            for (HdmiDeviceInfo info : TvInputHardwareManager.this.mHdmiDeviceList) {
                if (info.getId() == id) {
                    return info;
                }
            }
            return null;
        }
    }

    /* loaded from: classes2.dex */
    private final class HdmiSystemAudioModeChangeListener extends IHdmiSystemAudioModeChangeListener.Stub {
        private HdmiSystemAudioModeChangeListener() {
        }

        public void onStatusChanged(boolean enabled) throws RemoteException {
            synchronized (TvInputHardwareManager.this.mLock) {
                for (int i = 0; i < TvInputHardwareManager.this.mConnections.size(); i++) {
                    TvInputHardwareImpl impl = ((Connection) TvInputHardwareManager.this.mConnections.valueAt(i)).getHardwareImplLocked();
                    if (impl != null) {
                        impl.handleAudioSinkUpdated();
                    }
                }
            }
        }
    }
}
