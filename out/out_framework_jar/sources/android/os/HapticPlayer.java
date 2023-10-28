package android.os;

import android.app.ActivityThread;
import android.content.Context;
import android.os.CombinedVibration;
import android.os.IVibratorManagerService;
import android.util.Log;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
/* loaded from: classes2.dex */
public class HapticPlayer {
    private static final String TAG = "HapticPlayer";
    private boolean mAvailable;
    private CombinedVibration.Mono mCombinedVibration;
    private DynamicEffect mDynamicEffect;
    private ExecutorService mExcutor;
    private String mPackageName;
    private IVibratorManagerService mService;
    private boolean mStarted;
    private Binder mToken;
    private VibratorManager mVibratorManager;

    public HapticPlayer() {
    }

    public HapticPlayer(DynamicEffect effect) {
        this();
        this.mDynamicEffect = effect;
        this.mAvailable = isAvailable();
        if (this.mDynamicEffect == null) {
            Log.e(TAG, "mDynamicEffect can not be null.");
        } else {
            this.mCombinedVibration = new CombinedVibration.Mono(this.mDynamicEffect);
        }
        Log.d(TAG, "new player");
    }

    private ExecutorService getExcutor() {
        if (this.mExcutor == null) {
            this.mExcutor = Executors.newSingleThreadExecutor();
        }
        return this.mExcutor;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public Binder getToken() {
        if (this.mToken == null) {
            this.mToken = new Binder();
        }
        return this.mToken;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public String getPackageName() {
        String currentPackageName = ActivityThread.currentPackageName();
        this.mPackageName = currentPackageName;
        return currentPackageName;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public IVibratorManagerService getVibratorManagerService() {
        if (this.mService == null) {
            Log.d(TAG, "getVibratorManagerService  ");
            this.mService = IVibratorManagerService.Stub.asInterface(ServiceManager.getService(Context.VIBRATOR_MANAGER_SERVICE));
        }
        return this.mService;
    }

    public static boolean isAvailable() {
        if ("1".equals(SystemProperties.get("ro.tran_vibrate_ontouch.support"))) {
            return true;
        }
        Log.e(TAG, "Not Support vibrate ontouch !!");
        return false;
    }

    public void update_vib_info(final int infocase, final int info) {
        if (!this.mAvailable) {
            Log.e(TAG, "update_vib_info failed !! Not Support vibrate ontouch !!");
        }
        getExcutor().execute(new Runnable() { // from class: android.os.HapticPlayer.1
            @Override // java.lang.Runnable
            public void run() {
                try {
                    HapticPlayer.this.getVibratorManagerService();
                    if (HapticPlayer.this.mService == null) {
                        Log.e(HapticPlayer.TAG, "start failed due to no vibratorService !!");
                    } else {
                        HapticPlayer.this.mService.update_vib_info(infocase, info);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void updateFrequency(int frequency) {
        update_vib_info(1, frequency);
    }

    public void updateInterval(int interval) {
        update_vib_info(2, interval);
    }

    public void updateAmplitude(int amplitude) {
        update_vib_info(3, amplitude);
    }

    public void start(final int loop) {
        if (!this.mAvailable) {
            Log.e(TAG, "start failed !! Not Support vibrate ontouch !!");
        }
        Log.d(TAG, "start, loop = " + loop);
        if (this.mDynamicEffect == null || this.mCombinedVibration == null) {
            Log.e(TAG, "start failed due to invalid dynamic effect !!");
        } else {
            getExcutor().execute(new Runnable() { // from class: android.os.HapticPlayer.2
                @Override // java.lang.Runnable
                public void run() {
                    Log.d(HapticPlayer.TAG, "haptic play start, loop = " + loop);
                    long startRunTime = System.currentTimeMillis();
                    try {
                        String patternJson = HapticPlayer.this.mDynamicEffect.getEffectJson();
                        DynamicEffectParam effectParam = DynamicEffect.ParseJson(patternJson);
                        HapticPlayer.this.mDynamicEffect.setDynamicEffectParam(effectParam);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    HapticPlayer.this.mDynamicEffect.getDynamicEffectParam().setLoop(loop);
                    Log.d(HapticPlayer.TAG, "Loop: " + ((DynamicEffect) HapticPlayer.this.mCombinedVibration.getEffect()).getDynamicEffectParam().getLoop());
                    try {
                        HapticPlayer.this.getVibratorManagerService();
                    } catch (Exception e2) {
                        e2.printStackTrace();
                    }
                    if (HapticPlayer.this.mService == null) {
                        Log.e(HapticPlayer.TAG, "start failed due to no vibratorService !!");
                        return;
                    }
                    HapticPlayer.this.mService.vibrate(Process.myUid(), HapticPlayer.this.getPackageName(), HapticPlayer.this.mCombinedVibration, null, "DynamicEffect", HapticPlayer.this.getToken());
                    long useTime = System.currentTimeMillis() - startRunTime;
                    Log.d(HapticPlayer.TAG, "run vibrate thread use time:" + useTime);
                }
            });
        }
    }

    public void start(final int loop, final int interval, final int amplitude) {
        if (!this.mAvailable) {
            Log.e(TAG, "start failed !! Not Support vibrate ontouch !!");
        }
        Log.d(TAG, "start, loop = " + loop + " interval = " + interval + " amplitude = " + amplitude);
        if (this.mDynamicEffect == null || this.mCombinedVibration == null) {
            Log.e(TAG, "start failed due to invalid dynamic effect !!");
        } else {
            getExcutor().execute(new Runnable() { // from class: android.os.HapticPlayer.3
                @Override // java.lang.Runnable
                public void run() {
                    Log.d(HapticPlayer.TAG, "haptic play start, loop = " + loop + " interval = " + interval + " amplitude = " + amplitude);
                    long startRunTime = System.currentTimeMillis();
                    try {
                        String patternJson = HapticPlayer.this.mDynamicEffect.getEffectJson();
                        DynamicEffectParam effectParam = DynamicEffect.ParseJson(patternJson);
                        HapticPlayer.this.mDynamicEffect.setDynamicEffectParam(effectParam);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    HapticPlayer.this.mDynamicEffect.getDynamicEffectParam().setLoop(loop);
                    HapticPlayer.this.mDynamicEffect.getDynamicEffectParam().setInterval(interval);
                    HapticPlayer.this.mDynamicEffect.getDynamicEffectParam().setAmplitude(amplitude);
                    try {
                        HapticPlayer.this.getVibratorManagerService();
                    } catch (Exception e2) {
                        e2.printStackTrace();
                    }
                    if (HapticPlayer.this.mService == null) {
                        Log.e(HapticPlayer.TAG, "start failed due to no vibratorService !!");
                        return;
                    }
                    HapticPlayer.this.mService.vibrate(Process.myUid(), HapticPlayer.this.getPackageName(), HapticPlayer.this.mCombinedVibration, null, "DynamicEffect", HapticPlayer.this.getToken());
                    long useTime = System.currentTimeMillis() - startRunTime;
                    Log.d(HapticPlayer.TAG, "run vibrate thread use time:" + useTime);
                }
            });
        }
    }

    public void start(final int loop, final int interval, final int amplitude, final int frequency) {
        if (!this.mAvailable) {
            Log.e(TAG, "start failed !! Not Support vibrate ontouch !!");
        }
        Log.d(TAG, "start, loop = " + loop + " onterval = " + interval + " amplitude = " + amplitude + " frequency = " + frequency);
        if (this.mDynamicEffect == null || this.mCombinedVibration == null) {
            Log.e(TAG, "start failed due to invalid dynamic effect !!");
        } else {
            getExcutor().execute(new Runnable() { // from class: android.os.HapticPlayer.4
                @Override // java.lang.Runnable
                public void run() {
                    Log.d(HapticPlayer.TAG, "haptic play start, loop = " + loop + " onterval = " + interval + " amplitude = " + amplitude + " frequency = " + frequency);
                    long startRunTime = System.currentTimeMillis();
                    try {
                        String patternJson = HapticPlayer.this.mDynamicEffect.getEffectJson();
                        DynamicEffectParam effectParam = DynamicEffect.ParseJson(patternJson);
                        HapticPlayer.this.mDynamicEffect.setDynamicEffectParam(effectParam);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    HapticPlayer.this.mDynamicEffect.getDynamicEffectParam().setLoop(loop);
                    HapticPlayer.this.mDynamicEffect.getDynamicEffectParam().setInterval(interval);
                    HapticPlayer.this.mDynamicEffect.getDynamicEffectParam().setAmplitude(amplitude);
                    HapticPlayer.this.mDynamicEffect.getDynamicEffectParam().setFrequency(frequency);
                    try {
                        HapticPlayer.this.getVibratorManagerService();
                    } catch (Exception e2) {
                        e2.printStackTrace();
                    }
                    if (HapticPlayer.this.mService == null) {
                        Log.e(HapticPlayer.TAG, "start failed due to no vibratorService !!");
                        return;
                    }
                    HapticPlayer.this.mService.vibrate(Process.myUid(), HapticPlayer.this.getPackageName(), HapticPlayer.this.mCombinedVibration, null, "DynamicEffect", HapticPlayer.this.getToken());
                    long useTime = System.currentTimeMillis() - startRunTime;
                    Log.d(HapticPlayer.TAG, "run vibrate thread use time:" + useTime);
                }
            });
        }
    }

    public void stop() {
        if (!this.mAvailable) {
            Log.e(TAG, "stop failed !! Not Support vibrate ontouch !!");
        }
        Log.d(TAG, "stop");
        getExcutor().execute(new Runnable() { // from class: android.os.HapticPlayer.5
            @Override // java.lang.Runnable
            public void run() {
                try {
                    HapticPlayer.this.getVibratorManagerService();
                    if (HapticPlayer.this.mService == null) {
                        Log.e(HapticPlayer.TAG, "start failed due to no vibratorService !!");
                    } else {
                        HapticPlayer.this.mService.stopDynamicEffect();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
