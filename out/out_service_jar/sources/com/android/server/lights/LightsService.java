package com.android.server.lights;

import android.app.ActivityManager;
import android.content.Context;
import android.hardware.audio.common.V2_0.AudioFormat;
import android.hardware.light.HwLight;
import android.hardware.light.HwLightState;
import android.hardware.light.ILights;
import android.hardware.lights.ILightsManager;
import android.hardware.lights.Light;
import android.hardware.lights.LightState;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.Trace;
import android.provider.Settings;
import android.util.Slog;
import android.util.SparseArray;
import com.android.internal.display.BrightnessSynchronizer;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.Preconditions;
import com.android.server.SystemService;
import com.android.server.lights.LightsService;
import com.transsion.hubcore.server.tranhbm.ITranHBMManager;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
/* loaded from: classes.dex */
public class LightsService extends SystemService {
    static final boolean DEBUG = false;
    static final String TAG = "LightsService";
    private Handler mH;
    private final SparseArray<LightImpl> mLightsById;
    private final LightImpl[] mLightsByType;
    final LightsManagerBinderService mManagerService;
    private final LightsManager mService;
    private final Supplier<ILights> mVintfLights;
    private static final boolean TRAN_BACKLIGHT_OPTIMIZATION_SUPPORT = "1".equals(SystemProperties.get("ro.transsion.backlight.optimization", "0"));
    private static final boolean TRAN_BACKLIGHT_4095_LEVEL = "4095".equals(SystemProperties.get("ro.transsion.backlight.level", "4095"));
    private static final boolean TRAN_BATTERY_LIGHT_ONLY = "1".equals(SystemProperties.get("ro.vendor.tran.tran_battery_light_only"));

    static native void setLight_native(int i, int i2, int i3, int i4, int i5, int i6);

    static native int setLucidLight(int i, int i2, int i3, int i4, int i5, int i6);

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class LightsManagerBinderService extends ILightsManager.Stub {
        private final List<Session> mSessions;

        private LightsManagerBinderService() {
            this.mSessions = new ArrayList();
        }

        /* JADX INFO: Access modifiers changed from: private */
        /* loaded from: classes.dex */
        public final class Session implements Comparable<Session> {
            final int mPriority;
            final SparseArray<LightState> mRequests = new SparseArray<>();
            final IBinder mToken;

            Session(IBinder token, int priority) {
                this.mToken = token;
                this.mPriority = priority;
            }

            void setRequest(int lightId, LightState state) {
                if (state != null) {
                    this.mRequests.put(lightId, state);
                } else {
                    this.mRequests.remove(lightId);
                }
            }

            /* JADX DEBUG: Method merged with bridge method */
            @Override // java.lang.Comparable
            public int compareTo(Session otherSession) {
                return Integer.compare(otherSession.mPriority, this.mPriority);
            }
        }

        public List<Light> getLights() {
            List<Light> lights;
            LightsService.this.getContext().enforceCallingOrSelfPermission("android.permission.CONTROL_DEVICE_LIGHTS", "getLights requires CONTROL_DEVICE_LIGHTS_PERMISSION");
            synchronized (LightsService.this) {
                lights = new ArrayList<>();
                for (int i = 0; i < LightsService.this.mLightsById.size(); i++) {
                    if (!((LightImpl) LightsService.this.mLightsById.valueAt(i)).isSystemLight()) {
                        HwLight hwLight = ((LightImpl) LightsService.this.mLightsById.valueAt(i)).mHwLight;
                        lights.add(new Light(hwLight.id, hwLight.ordinal, hwLight.type));
                    }
                }
            }
            return lights;
        }

        public void setLightStates(IBinder token, int[] lightIds, LightState[] lightStates) {
            LightsService.this.getContext().enforceCallingOrSelfPermission("android.permission.CONTROL_DEVICE_LIGHTS", "setLightStates requires CONTROL_DEVICE_LIGHTS permission");
            Preconditions.checkState(lightIds.length == lightStates.length);
            synchronized (LightsService.this) {
                Session session = getSessionLocked((IBinder) Preconditions.checkNotNull(token));
                Preconditions.checkState(session != null, "not registered");
                checkRequestIsValid(lightIds);
                for (int i = 0; i < lightIds.length; i++) {
                    session.setRequest(lightIds[i], lightStates[i]);
                }
                invalidateLightStatesLocked();
            }
        }

        public LightState getLightState(int lightId) {
            LightState lightState;
            LightsService.this.getContext().enforceCallingOrSelfPermission("android.permission.CONTROL_DEVICE_LIGHTS", "getLightState(@TestApi) requires CONTROL_DEVICE_LIGHTS permission");
            synchronized (LightsService.this) {
                LightImpl light = (LightImpl) LightsService.this.mLightsById.get(lightId);
                if (light == null || light.isSystemLight()) {
                    throw new IllegalArgumentException("Invalid light: " + lightId);
                }
                lightState = new LightState(light.getColor());
            }
            return lightState;
        }

        public void openSession(final IBinder token, int priority) {
            LightsService.this.getContext().enforceCallingOrSelfPermission("android.permission.CONTROL_DEVICE_LIGHTS", "openSession requires CONTROL_DEVICE_LIGHTS permission");
            Preconditions.checkNotNull(token);
            synchronized (LightsService.this) {
                Preconditions.checkState(getSessionLocked(token) == null, "already registered");
                try {
                    token.linkToDeath(new IBinder.DeathRecipient() { // from class: com.android.server.lights.LightsService$LightsManagerBinderService$$ExternalSyntheticLambda0
                        @Override // android.os.IBinder.DeathRecipient
                        public final void binderDied() {
                            LightsService.LightsManagerBinderService.this.m4269x2ba8a640(token);
                        }
                    }, 0);
                    this.mSessions.add(new Session(token, priority));
                    Collections.sort(this.mSessions);
                } catch (RemoteException e) {
                    Slog.e(LightsService.TAG, "Couldn't open session, client already died", e);
                    throw new IllegalArgumentException("Client is already dead.");
                }
            }
        }

        public void closeSession(IBinder token) {
            LightsService.this.getContext().enforceCallingOrSelfPermission("android.permission.CONTROL_DEVICE_LIGHTS", "closeSession requires CONTROL_DEVICE_LIGHTS permission");
            Preconditions.checkNotNull(token);
            m4269x2ba8a640(token);
        }

        protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            if (DumpUtils.checkDumpPermission(LightsService.this.getContext(), LightsService.TAG, pw)) {
                synchronized (LightsService.this) {
                    if (LightsService.this.mVintfLights != null) {
                        pw.println("Service: aidl (" + LightsService.this.mVintfLights.get() + ")");
                    } else {
                        pw.println("Service: hidl");
                    }
                    pw.println("Lights:");
                    for (int i = 0; i < LightsService.this.mLightsById.size(); i++) {
                        LightImpl light = (LightImpl) LightsService.this.mLightsById.valueAt(i);
                        pw.println(String.format("  Light id=%d ordinal=%d color=%08x", Integer.valueOf(light.mHwLight.id), Integer.valueOf(light.mHwLight.ordinal), Integer.valueOf(light.getColor())));
                    }
                    pw.println("Session clients:");
                    for (Session session : this.mSessions) {
                        pw.println("  Session token=" + session.mToken);
                        for (int i2 = 0; i2 < session.mRequests.size(); i2++) {
                            pw.println(String.format("    Request id=%d color=%08x", Integer.valueOf(session.mRequests.keyAt(i2)), Integer.valueOf(session.mRequests.valueAt(i2).getColor())));
                        }
                    }
                }
            }
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX INFO: Access modifiers changed from: private */
        /* renamed from: closeSessionInternal */
        public void m4269x2ba8a640(IBinder token) {
            synchronized (LightsService.this) {
                Session session = getSessionLocked(token);
                if (session != null) {
                    this.mSessions.remove(session);
                    invalidateLightStatesLocked();
                }
            }
        }

        private void checkRequestIsValid(int[] lightIds) {
            for (int lightId : lightIds) {
                LightImpl light = (LightImpl) LightsService.this.mLightsById.get(lightId);
                Preconditions.checkState((light == null || light.isSystemLight()) ? false : true, "Invalid lightId " + lightId);
            }
        }

        private void invalidateLightStatesLocked() {
            Map<Integer, LightState> states = new HashMap<>();
            for (int i = this.mSessions.size() - 1; i >= 0; i--) {
                SparseArray<LightState> requests = this.mSessions.get(i).mRequests;
                for (int j = 0; j < requests.size(); j++) {
                    states.put(Integer.valueOf(requests.keyAt(j)), requests.valueAt(j));
                }
            }
            for (int i2 = 0; i2 < LightsService.this.mLightsById.size(); i2++) {
                LightImpl light = (LightImpl) LightsService.this.mLightsById.valueAt(i2);
                if (!light.isSystemLight()) {
                    LightState state = states.get(Integer.valueOf(light.mHwLight.id));
                    if (state != null) {
                        light.setColor(state.getColor());
                    } else {
                        light.turnOff();
                    }
                }
            }
        }

        private Session getSessionLocked(IBinder token) {
            for (int i = 0; i < this.mSessions.size(); i++) {
                if (token.equals(this.mSessions.get(i).mToken)) {
                    return this.mSessions.get(i);
                }
            }
            return null;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class LightImpl extends LogicalLight {
        private int mBrightnessMode;
        private int mColor;
        private boolean mFlashing;
        private HwLight mHwLight;
        private boolean mInitialized;
        private int mLastBrightnessMode;
        private int mLastColor;
        private int mMode;
        private int mOffMS;
        private int mOnMS;
        private boolean mUseLowPersistenceForVR;
        private boolean mVrModeEnabled;

        private LightImpl(Context context, HwLight hwLight) {
            this.mHwLight = hwLight;
        }

        @Override // com.android.server.lights.LogicalLight
        public void setBrightness(float brightness) {
            setBrightness(brightness, 0);
        }

        @Override // com.android.server.lights.LogicalLight
        public void setBrightness(float brightness, int brightnessMode) {
            if (Float.isNaN(brightness)) {
                Slog.w(LightsService.TAG, "Brightness is not valid: " + brightness);
                return;
            }
            synchronized (this) {
                if (brightnessMode == 2) {
                    Slog.w(LightsService.TAG, "setBrightness with LOW_PERSISTENCE unexpected #" + this.mHwLight.id + ": brightness=" + brightness);
                    return;
                }
                if (ITranHBMManager.TRAN_HIGH_BRIGHTNESS_MODE_SUPPORT && this.mHwLight.id == 0) {
                    boolean allowHbmMode = ITranHBMManager.Instance().allowHbmMode();
                    boolean hbmBrightnessInspire = !LightsService.TRAN_BACKLIGHT_4095_LEVEL && allowHbmMode && brightness > 1.0f;
                    Slog.i(LightsService.TAG, "HBM allowHbmMode=" + allowHbmMode + ",4095=" + LightsService.TRAN_BACKLIGHT_4095_LEVEL + "brightness=" + brightness);
                    if (hbmBrightnessInspire) {
                        int hbmBrightness = ITranHBMManager.Instance().brightnessFloatTo512Int(brightness);
                        Slog.i(LightsService.TAG, "setBrightness HBM hbmBrightness=" + hbmBrightness);
                        setLightLocked(hbmBrightness, 2, 100, 100, 0);
                        return;
                    }
                }
                int brightnessInt = BrightnessSynchronizer.brightnessFloatToInt(brightness);
                int color = brightnessInt & 255;
                int color2 = color | (-16777216) | (color << 16) | (color << 8);
                if (LightsService.TRAN_BACKLIGHT_OPTIMIZATION_SUPPORT) {
                    if (this.mHwLight.id == 0 && LightsService.TRAN_BACKLIGHT_4095_LEVEL) {
                        int brightness4095Int = BrightnessSynchronizer.brightnessFloatTo4095Int(LightsService.this.getContext(), brightness);
                        setLightLocked(brightness4095Int, 2, 100, 100, 0);
                    } else {
                        setLightLocked(color2, 0, 0, 0, brightnessMode);
                    }
                } else {
                    setLightLocked(color2, 0, 0, 0, brightnessMode);
                }
            }
        }

        @Override // com.android.server.lights.LogicalLight
        public void setColor(int color) {
            synchronized (this) {
                setLightLocked(color, 0, 0, 0, 0);
            }
        }

        @Override // com.android.server.lights.LogicalLight
        public void setFlashing(int color, int mode, int onMS, int offMS) {
            synchronized (this) {
                setLightLocked(color, mode, onMS, offMS, 0);
            }
        }

        @Override // com.android.server.lights.LogicalLight
        public void pulse() {
            pulse(AudioFormat.SUB_MASK, 7);
        }

        @Override // com.android.server.lights.LogicalLight
        public void pulse(int color, int onMS) {
            synchronized (this) {
                if (this.mColor == 0 && !this.mFlashing) {
                    setLightLocked(color, 2, onMS, 1000, 0);
                    this.mColor = 0;
                    LightsService.this.mH.postDelayed(new Runnable() { // from class: com.android.server.lights.LightsService$LightImpl$$ExternalSyntheticLambda0
                        @Override // java.lang.Runnable
                        public final void run() {
                            LightsService.LightImpl.this.stopFlashing();
                        }
                    }, onMS);
                }
            }
        }

        @Override // com.android.server.lights.LogicalLight
        public void turnOff() {
            synchronized (this) {
                setLightLocked(0, 0, 0, 0, 0);
            }
        }

        @Override // com.android.server.lights.LogicalLight
        public void setVrMode(boolean enabled) {
            synchronized (this) {
                if (this.mVrModeEnabled != enabled) {
                    this.mVrModeEnabled = enabled;
                    this.mUseLowPersistenceForVR = LightsService.this.getVrDisplayMode() == 0;
                    if (shouldBeInLowPersistenceMode()) {
                        this.mLastBrightnessMode = this.mBrightnessMode;
                    }
                }
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void stopFlashing() {
            synchronized (this) {
                setLightLocked(this.mColor, 0, 0, 0, 0);
            }
        }

        private void setLightLocked(int color, int mode, int onMS, int offMS, int brightnessMode) {
            if (shouldBeInLowPersistenceMode()) {
                brightnessMode = 2;
            } else if (brightnessMode == 2) {
                brightnessMode = this.mLastBrightnessMode;
            }
            if (LightsService.TRAN_BATTERY_LIGHT_ONLY && this.mHwLight.id == 4) {
                return;
            }
            if (!this.mInitialized || color != this.mColor || mode != this.mMode || onMS != this.mOnMS || offMS != this.mOffMS || this.mBrightnessMode != brightnessMode) {
                this.mInitialized = true;
                this.mLastColor = this.mColor;
                this.mColor = color;
                this.mMode = mode;
                this.mOnMS = onMS;
                this.mOffMS = offMS;
                this.mBrightnessMode = brightnessMode;
                setLightUnchecked(color, mode, onMS, offMS, brightnessMode);
            }
        }

        private void setLightUnchecked(int color, int mode, int onMS, int offMS, int brightnessMode) {
            Trace.traceBegin(131072L, "setLightState(" + this.mHwLight.id + ", 0x" + Integer.toHexString(color) + ")");
            try {
                try {
                    if (LightsService.this.mVintfLights != null) {
                        int color2 = LightsService.setLucidLight(this.mHwLight.id, color, mode, onMS, offMS, brightnessMode);
                        HwLightState lightState = new HwLightState();
                        lightState.color = color2;
                        lightState.flashMode = (byte) mode;
                        lightState.flashOnMs = onMS;
                        lightState.flashOffMs = offMS;
                        lightState.brightnessMode = (byte) brightnessMode;
                        ((ILights) LightsService.this.mVintfLights.get()).setLightState(this.mHwLight.id, lightState);
                    } else {
                        LightsService.setLight_native(this.mHwLight.id, color, mode, onMS, offMS, brightnessMode);
                    }
                } catch (RemoteException | UnsupportedOperationException ex) {
                    Slog.e(LightsService.TAG, "Failed issuing setLightState", ex);
                }
            } finally {
                Trace.traceEnd(131072L);
            }
        }

        private boolean shouldBeInLowPersistenceMode() {
            return this.mVrModeEnabled && this.mUseLowPersistenceForVR;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public boolean isSystemLight() {
            return this.mHwLight.type >= 0 && this.mHwLight.type < 8;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public int getColor() {
            return this.mColor;
        }
    }

    public LightsService(Context context) {
        this(context, new VintfHalCache(), Looper.myLooper());
    }

    LightsService(Context context, Supplier<ILights> service, Looper looper) {
        super(context);
        this.mLightsByType = new LightImpl[8];
        this.mLightsById = new SparseArray<>();
        this.mService = new LightsManager() { // from class: com.android.server.lights.LightsService.1
            @Override // com.android.server.lights.LightsManager
            public LogicalLight getLight(int lightType) {
                if (LightsService.this.mLightsByType != null && lightType >= 0 && lightType < LightsService.this.mLightsByType.length) {
                    return LightsService.this.mLightsByType[lightType];
                }
                return null;
            }
        };
        this.mH = new Handler(looper);
        this.mVintfLights = service.get() != null ? service : null;
        populateAvailableLights(context);
        this.mManagerService = new LightsManagerBinderService();
    }

    private void populateAvailableLights(Context context) {
        if (this.mVintfLights != null) {
            populateAvailableLightsFromAidl(context);
        } else {
            populateAvailableLightsFromHidl(context);
        }
        for (int i = this.mLightsById.size() - 1; i >= 0; i--) {
            int type = this.mLightsById.keyAt(i);
            if (type >= 0) {
                LightImpl[] lightImplArr = this.mLightsByType;
                if (type < lightImplArr.length) {
                    lightImplArr[type] = this.mLightsById.valueAt(i);
                }
            }
        }
    }

    private void populateAvailableLightsFromAidl(Context context) {
        HwLight[] lights;
        try {
            for (HwLight hwLight : this.mVintfLights.get().getLights()) {
                this.mLightsById.put(hwLight.id, new LightImpl(context, hwLight));
            }
        } catch (RemoteException ex) {
            Slog.e(TAG, "Unable to get lights from HAL", ex);
        }
    }

    private void populateAvailableLightsFromHidl(Context context) {
        for (int i = 0; i < this.mLightsByType.length; i++) {
            HwLight hwLight = new HwLight();
            hwLight.id = (byte) i;
            hwLight.ordinal = 1;
            hwLight.type = (byte) i;
            this.mLightsById.put(hwLight.id, new LightImpl(context, hwLight));
        }
    }

    @Override // com.android.server.SystemService
    public void onStart() {
        publishLocalService(LightsManager.class, this.mService);
        publishBinderService("lights", this.mManagerService);
    }

    @Override // com.android.server.SystemService
    public void onBootPhase(int phase) {
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int getVrDisplayMode() {
        int currentUser = ActivityManager.getCurrentUser();
        return Settings.Secure.getIntForUser(getContext().getContentResolver(), "vr_display_mode", 0, currentUser);
    }

    /* loaded from: classes.dex */
    private static class VintfHalCache implements Supplier<ILights>, IBinder.DeathRecipient {
        private ILights mInstance;

        private VintfHalCache() {
            this.mInstance = null;
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // java.util.function.Supplier
        public synchronized ILights get() {
            IBinder binder;
            if (this.mInstance == null && (binder = Binder.allowBlocking(ServiceManager.waitForDeclaredService(ILights.DESCRIPTOR + "/default"))) != null) {
                this.mInstance = ILights.Stub.asInterface(binder);
                try {
                    binder.linkToDeath(this, 0);
                } catch (RemoteException e) {
                    Slog.e(LightsService.TAG, "Unable to register DeathRecipient for " + this.mInstance);
                }
            }
            return this.mInstance;
        }

        @Override // android.os.IBinder.DeathRecipient
        public synchronized void binderDied() {
            this.mInstance = null;
        }
    }
}
