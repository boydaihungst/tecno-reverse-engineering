package android.media;

import android.content.Context;
import android.hardware.cas.V1_0.HidlCasPluginDescriptor;
import android.hardware.cas.V1_0.ICas;
import android.hardware.cas.V1_0.IMediaCasService;
import android.hardware.cas.V1_2.ICas;
import android.hardware.cas.V1_2.ICasListener;
import android.media.MediaCasException;
import android.media.tv.tunerresourcemanager.CasSessionRequest;
import android.media.tv.tunerresourcemanager.ResourceClientProfile;
import android.media.tv.tunerresourcemanager.TunerResourceManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IHwBinder;
import android.os.IHwInterface;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.RemoteException;
import android.util.Log;
import android.util.Singleton;
import com.android.internal.util.FrameworkStatsLog;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
/* loaded from: classes2.dex */
public final class MediaCas implements AutoCloseable {
    public static final int PLUGIN_STATUS_PHYSICAL_MODULE_CHANGED = 0;
    public static final int PLUGIN_STATUS_SESSION_NUMBER_CHANGED = 1;
    public static final int SCRAMBLING_MODE_AES128 = 9;
    public static final int SCRAMBLING_MODE_AES_ECB = 10;
    public static final int SCRAMBLING_MODE_AES_SCTE52 = 11;
    public static final int SCRAMBLING_MODE_DVB_CISSA_V1 = 6;
    public static final int SCRAMBLING_MODE_DVB_CSA1 = 1;
    public static final int SCRAMBLING_MODE_DVB_CSA2 = 2;
    public static final int SCRAMBLING_MODE_DVB_CSA3_ENHANCE = 5;
    public static final int SCRAMBLING_MODE_DVB_CSA3_MINIMAL = 4;
    public static final int SCRAMBLING_MODE_DVB_CSA3_STANDARD = 3;
    public static final int SCRAMBLING_MODE_DVB_IDSA = 7;
    public static final int SCRAMBLING_MODE_MULTI2 = 8;
    public static final int SCRAMBLING_MODE_RESERVED = 0;
    public static final int SCRAMBLING_MODE_TDES_ECB = 12;
    public static final int SCRAMBLING_MODE_TDES_SCTE52 = 13;
    public static final int SESSION_USAGE_LIVE = 0;
    public static final int SESSION_USAGE_PLAYBACK = 1;
    public static final int SESSION_USAGE_RECORD = 2;
    public static final int SESSION_USAGE_TIMESHIFT = 3;
    private static final String TAG = "MediaCas";
    private static final Singleton<IMediaCasService> sService = new Singleton<IMediaCasService>() { // from class: android.media.MediaCas.1
        /* JADX DEBUG: Method merged with bridge method */
        /* JADX INFO: Access modifiers changed from: protected */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.util.Singleton
        public IMediaCasService create() {
            try {
                Log.d(MediaCas.TAG, "Trying to get cas@1.2 service");
                android.hardware.cas.V1_2.IMediaCasService serviceV12 = android.hardware.cas.V1_2.IMediaCasService.getService(true);
                if (serviceV12 != null) {
                    return serviceV12;
                }
            } catch (Exception e) {
                Log.d(MediaCas.TAG, "Failed to get cas@1.2 service");
            }
            try {
                Log.d(MediaCas.TAG, "Trying to get cas@1.1 service");
                android.hardware.cas.V1_1.IMediaCasService serviceV11 = android.hardware.cas.V1_1.IMediaCasService.getService(true);
                if (serviceV11 != null) {
                    return serviceV11;
                }
            } catch (Exception e2) {
                Log.d(MediaCas.TAG, "Failed to get cas@1.1 service");
            }
            try {
                Log.d(MediaCas.TAG, "Trying to get cas@1.0 service");
                return IMediaCasService.getService(true);
            } catch (Exception e3) {
                Log.d(MediaCas.TAG, "Failed to get cas@1.0 service");
                return null;
            }
        }
    };
    private int mCasSystemId;
    private int mClientId;
    private EventHandler mEventHandler;
    private HandlerThread mHandlerThread;
    private ICas mICas;
    private android.hardware.cas.V1_1.ICas mICasV11;
    private android.hardware.cas.V1_2.ICas mICasV12;
    private EventListener mListener;
    private int mPriorityHint;
    private String mTvInputServiceSessionId;
    private int mUserId;
    private TunerResourceManager mTunerResourceManager = null;
    private final Map<Session, Integer> mSessionMap = new HashMap();
    private final ICasListener.Stub mBinder = new ICasListener.Stub() { // from class: android.media.MediaCas.2
        @Override // android.hardware.cas.V1_0.ICasListener
        public void onEvent(int event, int arg, ArrayList<Byte> data) throws RemoteException {
            if (MediaCas.this.mEventHandler != null) {
                MediaCas.this.mEventHandler.sendMessage(MediaCas.this.mEventHandler.obtainMessage(0, event, arg, data));
            }
        }

        @Override // android.hardware.cas.V1_1.ICasListener
        public void onSessionEvent(ArrayList<Byte> sessionId, int event, int arg, ArrayList<Byte> data) throws RemoteException {
            if (MediaCas.this.mEventHandler != null) {
                Message msg = MediaCas.this.mEventHandler.obtainMessage();
                msg.what = 1;
                msg.arg1 = event;
                msg.arg2 = arg;
                Bundle bundle = new Bundle();
                bundle.putByteArray("sessionId", MediaCas.this.toBytes(sessionId));
                bundle.putByteArray("data", MediaCas.this.toBytes(data));
                msg.setData(bundle);
                MediaCas.this.mEventHandler.sendMessage(msg);
            }
        }

        @Override // android.hardware.cas.V1_2.ICasListener
        public void onStatusUpdate(byte status, int arg) throws RemoteException {
            if (MediaCas.this.mEventHandler != null) {
                MediaCas.this.mEventHandler.sendMessage(MediaCas.this.mEventHandler.obtainMessage(2, status, arg));
            }
        }
    };
    private final TunerResourceManager.ResourcesReclaimListener mResourceListener = new TunerResourceManager.ResourcesReclaimListener() { // from class: android.media.MediaCas.3
        @Override // android.media.tv.tunerresourcemanager.TunerResourceManager.ResourcesReclaimListener
        public void onReclaimResources() {
            synchronized (MediaCas.this.mSessionMap) {
                List<Session> sessionList = new ArrayList<>(MediaCas.this.mSessionMap.keySet());
                for (Session casSession : sessionList) {
                    casSession.close();
                }
            }
            MediaCas.this.mEventHandler.sendMessage(MediaCas.this.mEventHandler.obtainMessage(3));
        }
    };

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes2.dex */
    public @interface PluginStatus {
    }

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes2.dex */
    public @interface ScramblingMode {
    }

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes2.dex */
    public @interface SessionUsage {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static IMediaCasService getService() {
        return sService.get();
    }

    private void validateInternalStates() {
        if (this.mICas == null) {
            throw new IllegalStateException();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void cleanupAndRethrowIllegalState() {
        this.mICas = null;
        this.mICasV11 = null;
        this.mICasV12 = null;
        throw new IllegalStateException();
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class EventHandler extends Handler {
        private static final String DATA_KEY = "data";
        private static final int MSG_CAS_EVENT = 0;
        private static final int MSG_CAS_RESOURCE_LOST = 3;
        private static final int MSG_CAS_SESSION_EVENT = 1;
        private static final int MSG_CAS_STATUS_EVENT = 2;
        private static final String SESSION_KEY = "sessionId";

        public EventHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            if (msg.what == 0) {
                MediaCas.this.mListener.onEvent(MediaCas.this, msg.arg1, msg.arg2, MediaCas.this.toBytes((ArrayList) msg.obj));
            } else if (msg.what == 1) {
                Bundle bundle = msg.getData();
                ArrayList<Byte> sessionId = MediaCas.this.toByteArray(bundle.getByteArray("sessionId"));
                EventListener eventListener = MediaCas.this.mListener;
                MediaCas mediaCas = MediaCas.this;
                eventListener.onSessionEvent(mediaCas, mediaCas.createFromSessionId(sessionId), msg.arg1, msg.arg2, bundle.getByteArray("data"));
            } else if (msg.what == 2) {
                if (msg.arg1 == 1 && MediaCas.this.mTunerResourceManager != null) {
                    MediaCas.this.mTunerResourceManager.updateCasInfo(MediaCas.this.mCasSystemId, msg.arg2);
                }
                MediaCas.this.mListener.onPluginStatusUpdate(MediaCas.this, msg.arg1, msg.arg2);
            } else if (msg.what == 3) {
                MediaCas.this.mListener.onResourceLost(MediaCas.this);
            }
        }
    }

    /* loaded from: classes2.dex */
    public static class PluginDescriptor {
        private final int mCASystemId;
        private final String mName;

        private PluginDescriptor() {
            this.mCASystemId = 65535;
            this.mName = null;
        }

        PluginDescriptor(HidlCasPluginDescriptor descriptor) {
            this.mCASystemId = descriptor.caSystemId;
            this.mName = descriptor.name;
        }

        public int getSystemId() {
            return this.mCASystemId;
        }

        public String getName() {
            return this.mName;
        }

        public String toString() {
            return "PluginDescriptor {" + this.mCASystemId + ", " + this.mName + "}";
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public ArrayList<Byte> toByteArray(byte[] data, int offset, int length) {
        ArrayList<Byte> byteArray = new ArrayList<>(length);
        for (int i = 0; i < length; i++) {
            byteArray.add(Byte.valueOf(data[offset + i]));
        }
        return byteArray;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public ArrayList<Byte> toByteArray(byte[] data) {
        if (data == null) {
            return new ArrayList<>();
        }
        return toByteArray(data, 0, data.length);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public byte[] toBytes(ArrayList<Byte> byteArray) {
        byte[] data = null;
        if (byteArray != null) {
            data = new byte[byteArray.size()];
            for (int i = 0; i < data.length; i++) {
                data[i] = byteArray.get(i).byteValue();
            }
        }
        return data;
    }

    /* loaded from: classes2.dex */
    public final class Session implements AutoCloseable {
        boolean mIsClosed = false;
        final ArrayList<Byte> mSessionId;

        Session(ArrayList<Byte> sessionId) {
            this.mSessionId = new ArrayList<>(sessionId);
        }

        private void validateSessionInternalStates() {
            if (MediaCas.this.mICas == null) {
                throw new IllegalStateException();
            }
            if (this.mIsClosed) {
                MediaCasStateException.throwExceptionIfNeeded(3);
            }
        }

        public boolean equals(Object obj) {
            if (obj instanceof Session) {
                return this.mSessionId.equals(((Session) obj).mSessionId);
            }
            return false;
        }

        public void setPrivateData(byte[] data) throws MediaCasException {
            validateSessionInternalStates();
            try {
                MediaCasException.throwExceptionIfNeeded(MediaCas.this.mICas.setSessionPrivateData(this.mSessionId, MediaCas.this.toByteArray(data, 0, data.length)));
            } catch (RemoteException e) {
                MediaCas.this.cleanupAndRethrowIllegalState();
            }
        }

        public void processEcm(byte[] data, int offset, int length) throws MediaCasException {
            validateSessionInternalStates();
            try {
                MediaCasException.throwExceptionIfNeeded(MediaCas.this.mICas.processEcm(this.mSessionId, MediaCas.this.toByteArray(data, offset, length)));
            } catch (RemoteException e) {
                MediaCas.this.cleanupAndRethrowIllegalState();
            }
        }

        public void processEcm(byte[] data) throws MediaCasException {
            processEcm(data, 0, data.length);
        }

        public void sendSessionEvent(int event, int arg, byte[] data) throws MediaCasException {
            validateSessionInternalStates();
            if (MediaCas.this.mICasV11 == null) {
                Log.d(MediaCas.TAG, "Send Session Event isn't supported by cas@1.0 interface");
                throw new MediaCasException.UnsupportedCasException("Send Session Event is not supported");
            }
            try {
                MediaCasException.throwExceptionIfNeeded(MediaCas.this.mICasV11.sendSessionEvent(this.mSessionId, event, arg, MediaCas.this.toByteArray(data)));
            } catch (RemoteException e) {
                MediaCas.this.cleanupAndRethrowIllegalState();
            }
        }

        public byte[] getSessionId() {
            validateSessionInternalStates();
            return MediaCas.this.toBytes(this.mSessionId);
        }

        @Override // java.lang.AutoCloseable
        public void close() {
            validateSessionInternalStates();
            try {
                MediaCasStateException.throwExceptionIfNeeded(MediaCas.this.mICas.closeSession(this.mSessionId));
                this.mIsClosed = true;
                MediaCas.this.removeSessionFromResourceMap(this);
            } catch (RemoteException e) {
                MediaCas.this.cleanupAndRethrowIllegalState();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Session createFromSessionId(ArrayList<Byte> sessionId) {
        if (sessionId == null || sessionId.size() == 0) {
            return null;
        }
        return new Session(sessionId);
    }

    public static boolean isSystemIdSupported(int CA_system_id) {
        IMediaCasService service = getService();
        if (service != null) {
            try {
                return service.isSystemIdSupported(CA_system_id);
            } catch (RemoteException e) {
                return false;
            }
        }
        return false;
    }

    public static PluginDescriptor[] enumeratePlugins() {
        IMediaCasService service = getService();
        if (service != null) {
            try {
                ArrayList<HidlCasPluginDescriptor> descriptors = service.enumeratePlugins();
                if (descriptors.size() == 0) {
                    return null;
                }
                PluginDescriptor[] results = new PluginDescriptor[descriptors.size()];
                for (int i = 0; i < results.length; i++) {
                    results[i] = new PluginDescriptor(descriptors.get(i));
                }
                return results;
            } catch (RemoteException e) {
            }
        }
        return null;
    }

    /* JADX DEBUG: Finally have unexpected throw blocks count: 2, expect 1 */
    private void createPlugin(int casSystemId) throws MediaCasException.UnsupportedCasException {
        try {
            try {
                this.mCasSystemId = casSystemId;
                this.mUserId = Process.myUid();
                IMediaCasService service = getService();
                android.hardware.cas.V1_2.IMediaCasService serviceV12 = android.hardware.cas.V1_2.IMediaCasService.castFrom((IHwInterface) service);
                if (serviceV12 == null) {
                    android.hardware.cas.V1_1.IMediaCasService serviceV11 = android.hardware.cas.V1_1.IMediaCasService.castFrom((IHwInterface) service);
                    if (serviceV11 == null) {
                        Log.d(TAG, "Used cas@1_0 interface to create plugin");
                        this.mICas = service.createPlugin(casSystemId, this.mBinder);
                    } else {
                        Log.d(TAG, "Used cas@1.1 interface to create plugin");
                        android.hardware.cas.V1_1.ICas createPluginExt = serviceV11.createPluginExt(casSystemId, this.mBinder);
                        this.mICasV11 = createPluginExt;
                        this.mICas = createPluginExt;
                    }
                } else {
                    Log.d(TAG, "Used cas@1.2 interface to create plugin");
                    android.hardware.cas.V1_2.ICas castFrom = android.hardware.cas.V1_2.ICas.castFrom((IHwInterface) serviceV12.createPluginExt(casSystemId, this.mBinder));
                    this.mICasV12 = castFrom;
                    this.mICasV11 = castFrom;
                    this.mICas = castFrom;
                }
                if (this.mICas == null) {
                    throw new MediaCasException.UnsupportedCasException("Unsupported casSystemId " + casSystemId);
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to create plugin: " + e);
                this.mICas = null;
                throw new MediaCasException.UnsupportedCasException("Unsupported casSystemId " + casSystemId);
            }
        } catch (Throwable th) {
            if (this.mICas == null) {
                throw new MediaCasException.UnsupportedCasException("Unsupported casSystemId " + casSystemId);
            }
            throw th;
        }
    }

    private void registerClient(Context context, String tvInputServiceSessionId, int priorityHint) {
        TunerResourceManager tunerResourceManager = (TunerResourceManager) context.getSystemService(Context.TV_TUNER_RESOURCE_MGR_SERVICE);
        this.mTunerResourceManager = tunerResourceManager;
        if (tunerResourceManager != null) {
            int[] clientId = new int[1];
            ResourceClientProfile profile = new ResourceClientProfile();
            profile.tvInputSessionId = tvInputServiceSessionId;
            profile.useCase = priorityHint;
            this.mTunerResourceManager.registerClientProfile(profile, context.getMainExecutor(), this.mResourceListener, clientId);
            this.mClientId = clientId[0];
        }
    }

    public MediaCas(int casSystemId) throws MediaCasException.UnsupportedCasException {
        createPlugin(casSystemId);
    }

    public MediaCas(Context context, int casSystemId, String tvInputServiceSessionId, int priorityHint) throws MediaCasException.UnsupportedCasException {
        Objects.requireNonNull(context, "context must not be null");
        createPlugin(casSystemId);
        registerClient(context, tvInputServiceSessionId, priorityHint);
    }

    public MediaCas(Context context, int casSystemId, String tvInputServiceSessionId, int priorityHint, Handler handler, EventListener listener) throws MediaCasException.UnsupportedCasException {
        Objects.requireNonNull(context, "context must not be null");
        setEventListener(listener, handler);
        createPlugin(casSystemId);
        registerClient(context, tvInputServiceSessionId, priorityHint);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public IHwBinder getBinder() {
        validateInternalStates();
        return this.mICas.asBinder();
    }

    /* loaded from: classes2.dex */
    public interface EventListener {
        void onEvent(MediaCas mediaCas, int i, int i2, byte[] bArr);

        default void onSessionEvent(MediaCas mediaCas, Session session, int event, int arg, byte[] data) {
            Log.d(MediaCas.TAG, "Received MediaCas Session event");
        }

        default void onPluginStatusUpdate(MediaCas mediaCas, int status, int arg) {
            Log.d(MediaCas.TAG, "Received MediaCas Plugin Status event");
        }

        default void onResourceLost(MediaCas mediaCas) {
            Log.d(MediaCas.TAG, "Received MediaCas Resource Reclaim event");
        }
    }

    public void setEventListener(EventListener listener, Handler handler) {
        this.mListener = listener;
        if (listener == null) {
            this.mEventHandler = null;
            return;
        }
        Looper looper = handler != null ? handler.getLooper() : null;
        if (looper == null) {
            Looper myLooper = Looper.myLooper();
            looper = myLooper;
            if (myLooper == null) {
                Looper mainLooper = Looper.getMainLooper();
                looper = mainLooper;
                if (mainLooper == null) {
                    HandlerThread handlerThread = this.mHandlerThread;
                    if (handlerThread == null || !handlerThread.isAlive()) {
                        HandlerThread handlerThread2 = new HandlerThread("MediaCasEventThread", -2);
                        this.mHandlerThread = handlerThread2;
                        handlerThread2.start();
                    }
                    looper = this.mHandlerThread.getLooper();
                }
            }
        }
        this.mEventHandler = new EventHandler(looper);
    }

    public void setPrivateData(byte[] data) throws MediaCasException {
        validateInternalStates();
        try {
            MediaCasException.throwExceptionIfNeeded(this.mICas.setPrivateData(toByteArray(data, 0, data.length)));
        } catch (RemoteException e) {
            cleanupAndRethrowIllegalState();
        }
    }

    /* loaded from: classes2.dex */
    private class OpenSessionCallback implements ICas.openSessionCallback {
        public Session mSession;
        public int mStatus;

        private OpenSessionCallback() {
        }

        @Override // android.hardware.cas.V1_0.ICas.openSessionCallback
        public void onValues(int status, ArrayList<Byte> sessionId) {
            this.mStatus = status;
            this.mSession = MediaCas.this.createFromSessionId(sessionId);
        }
    }

    /* loaded from: classes2.dex */
    private class OpenSession_1_2_Callback implements ICas.openSession_1_2Callback {
        public Session mSession;
        public int mStatus;

        private OpenSession_1_2_Callback() {
        }

        @Override // android.hardware.cas.V1_2.ICas.openSession_1_2Callback
        public void onValues(int status, ArrayList<Byte> sessionId) {
            this.mStatus = status;
            this.mSession = MediaCas.this.createFromSessionId(sessionId);
        }
    }

    private int getSessionResourceHandle() throws MediaCasException {
        validateInternalStates();
        int[] sessionResourceHandle = {-1};
        if (this.mTunerResourceManager != null) {
            CasSessionRequest casSessionRequest = new CasSessionRequest();
            casSessionRequest.clientId = this.mClientId;
            casSessionRequest.casSystemId = this.mCasSystemId;
            if (!this.mTunerResourceManager.requestCasSession(casSessionRequest, sessionResourceHandle)) {
                throw new MediaCasException.InsufficientResourceException("insufficient resource to Open Session");
            }
        }
        return sessionResourceHandle[0];
    }

    private void addSessionToResourceMap(Session session, int sessionResourceHandle) {
        if (sessionResourceHandle != -1) {
            synchronized (this.mSessionMap) {
                this.mSessionMap.put(session, Integer.valueOf(sessionResourceHandle));
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void removeSessionFromResourceMap(Session session) {
        synchronized (this.mSessionMap) {
            if (this.mSessionMap.get(session) != null) {
                this.mTunerResourceManager.releaseCasSession(this.mSessionMap.get(session).intValue(), this.mClientId);
                this.mSessionMap.remove(session);
            }
        }
    }

    public Session openSession() throws MediaCasException {
        int sessionResourceHandle = getSessionResourceHandle();
        try {
            OpenSessionCallback cb = new OpenSessionCallback();
            this.mICas.openSession(cb);
            MediaCasException.throwExceptionIfNeeded(cb.mStatus);
            addSessionToResourceMap(cb.mSession, sessionResourceHandle);
            Log.d(TAG, "Write Stats Log for succeed to Open Session.");
            FrameworkStatsLog.write(280, this.mUserId, this.mCasSystemId, 1);
            return cb.mSession;
        } catch (RemoteException e) {
            cleanupAndRethrowIllegalState();
            Log.d(TAG, "Write Stats Log for fail to Open Session.");
            FrameworkStatsLog.write(280, this.mUserId, this.mCasSystemId, 2);
            return null;
        }
    }

    public Session openSession(int sessionUsage, int scramblingMode) throws MediaCasException {
        int sessionResourceHandle = getSessionResourceHandle();
        if (this.mICasV12 == null) {
            Log.d(TAG, "Open Session with scrambling mode is only supported by cas@1.2+ interface");
            throw new MediaCasException.UnsupportedCasException("Open Session with scrambling mode is not supported");
        }
        try {
            OpenSession_1_2_Callback cb = new OpenSession_1_2_Callback();
            this.mICasV12.openSession_1_2(sessionUsage, scramblingMode, cb);
            MediaCasException.throwExceptionIfNeeded(cb.mStatus);
            addSessionToResourceMap(cb.mSession, sessionResourceHandle);
            Log.d(TAG, "Write Stats Log for succeed to Open Session.");
            FrameworkStatsLog.write(280, this.mUserId, this.mCasSystemId, 1);
            return cb.mSession;
        } catch (RemoteException e) {
            cleanupAndRethrowIllegalState();
            Log.d(TAG, "Write Stats Log for fail to Open Session.");
            FrameworkStatsLog.write(280, this.mUserId, this.mCasSystemId, 2);
            return null;
        }
    }

    public void processEmm(byte[] data, int offset, int length) throws MediaCasException {
        validateInternalStates();
        try {
            MediaCasException.throwExceptionIfNeeded(this.mICas.processEmm(toByteArray(data, offset, length)));
        } catch (RemoteException e) {
            cleanupAndRethrowIllegalState();
        }
    }

    public void processEmm(byte[] data) throws MediaCasException {
        processEmm(data, 0, data.length);
    }

    public void sendEvent(int event, int arg, byte[] data) throws MediaCasException {
        validateInternalStates();
        try {
            MediaCasException.throwExceptionIfNeeded(this.mICas.sendEvent(event, arg, toByteArray(data)));
        } catch (RemoteException e) {
            cleanupAndRethrowIllegalState();
        }
    }

    public void provision(String provisionString) throws MediaCasException {
        validateInternalStates();
        try {
            MediaCasException.throwExceptionIfNeeded(this.mICas.provision(provisionString));
        } catch (RemoteException e) {
            cleanupAndRethrowIllegalState();
        }
    }

    public void refreshEntitlements(int refreshType, byte[] refreshData) throws MediaCasException {
        validateInternalStates();
        try {
            MediaCasException.throwExceptionIfNeeded(this.mICas.refreshEntitlements(refreshType, toByteArray(refreshData)));
        } catch (RemoteException e) {
            cleanupAndRethrowIllegalState();
        }
    }

    public void forceResourceLost() {
        TunerResourceManager.ResourcesReclaimListener resourcesReclaimListener = this.mResourceListener;
        if (resourcesReclaimListener != null) {
            resourcesReclaimListener.onReclaimResources();
        }
    }

    @Override // java.lang.AutoCloseable
    public void close() {
        android.hardware.cas.V1_0.ICas iCas = this.mICas;
        if (iCas != null) {
            try {
                iCas.release();
            } catch (RemoteException e) {
            } catch (Throwable th) {
                this.mICas = null;
                throw th;
            }
            this.mICas = null;
        }
        TunerResourceManager tunerResourceManager = this.mTunerResourceManager;
        if (tunerResourceManager != null) {
            tunerResourceManager.unregisterClientProfile(this.mClientId);
            this.mTunerResourceManager = null;
        }
        HandlerThread handlerThread = this.mHandlerThread;
        if (handlerThread != null) {
            handlerThread.quit();
            this.mHandlerThread = null;
        }
    }

    protected void finalize() {
        close();
    }
}
