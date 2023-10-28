package android.os;

import android.content.Context;
import android.os.SystemVibrator;
import android.os.Vibrator;
import android.os.VibratorInfo;
import android.util.ArrayMap;
import android.util.Log;
import android.util.Range;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.util.SparseIntArray;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.function.Function;
/* loaded from: classes2.dex */
public class SystemVibrator extends Vibrator {
    private static final String TAG = "Vibrator";
    private final ArrayList<AllVibratorsStateListener> mBrokenListeners;
    private final Context mContext;
    private final Object mLock;
    private final ArrayMap<Vibrator.OnVibratorStateChangedListener, AllVibratorsStateListener> mRegisteredListeners;
    private VibratorInfo mVibratorInfo;
    private final VibratorManager mVibratorManager;

    public SystemVibrator(Context context) {
        super(context);
        this.mBrokenListeners = new ArrayList<>();
        this.mRegisteredListeners = new ArrayMap<>();
        this.mLock = new Object();
        this.mContext = context;
        this.mVibratorManager = (VibratorManager) context.getSystemService(VibratorManager.class);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // android.os.Vibrator
    public VibratorInfo getInfo() {
        synchronized (this.mLock) {
            VibratorInfo vibratorInfo = this.mVibratorInfo;
            if (vibratorInfo != null) {
                return vibratorInfo;
            }
            VibratorManager vibratorManager = this.mVibratorManager;
            if (vibratorManager == null) {
                Log.w(TAG, "Failed to retrieve vibrator info; no vibrator manager.");
                return VibratorInfo.EMPTY_VIBRATOR_INFO;
            }
            int[] vibratorIds = vibratorManager.getVibratorIds();
            if (vibratorIds.length == 0) {
                NoVibratorInfo noVibratorInfo = new NoVibratorInfo();
                this.mVibratorInfo = noVibratorInfo;
                return noVibratorInfo;
            }
            VibratorInfo[] vibratorInfos = new VibratorInfo[vibratorIds.length];
            for (int i = 0; i < vibratorIds.length; i++) {
                Vibrator vibrator = this.mVibratorManager.getVibrator(vibratorIds[i]);
                if (vibrator instanceof NullVibrator) {
                    Log.w(TAG, "Vibrator manager service not ready; Info not yet available for vibrator: " + vibratorIds[i]);
                    return VibratorInfo.EMPTY_VIBRATOR_INFO;
                }
                vibratorInfos[i] = vibrator.getInfo();
            }
            int i2 = vibratorInfos.length;
            if (i2 == 1) {
                VibratorInfo vibratorInfo2 = new VibratorInfo(-1, vibratorInfos[0]);
                this.mVibratorInfo = vibratorInfo2;
                return vibratorInfo2;
            }
            MultiVibratorInfo multiVibratorInfo = new MultiVibratorInfo(vibratorInfos);
            this.mVibratorInfo = multiVibratorInfo;
            return multiVibratorInfo;
        }
    }

    @Override // android.os.Vibrator
    public boolean hasVibrator() {
        VibratorManager vibratorManager = this.mVibratorManager;
        if (vibratorManager != null) {
            return vibratorManager.getVibratorIds().length > 0;
        }
        Log.w(TAG, "Failed to check if vibrator exists; no vibrator manager.");
        return false;
    }

    @Override // android.os.Vibrator
    public boolean isVibrating() {
        int[] vibratorIds;
        VibratorManager vibratorManager = this.mVibratorManager;
        if (vibratorManager == null) {
            Log.w(TAG, "Failed to vibrate; no vibrator manager.");
            return false;
        }
        for (int vibratorId : vibratorManager.getVibratorIds()) {
            if (this.mVibratorManager.getVibrator(vibratorId).isVibrating()) {
                return true;
            }
        }
        return false;
    }

    @Override // android.os.Vibrator
    public void addVibratorStateListener(Vibrator.OnVibratorStateChangedListener listener) {
        Objects.requireNonNull(listener);
        Context context = this.mContext;
        if (context == null) {
            Log.w(TAG, "Failed to add vibrate state listener; no vibrator context.");
        } else {
            addVibratorStateListener(context.getMainExecutor(), listener);
        }
    }

    /* JADX DEBUG: Finally have unexpected throw blocks count: 2, expect 1 */
    @Override // android.os.Vibrator
    public void addVibratorStateListener(Executor executor, Vibrator.OnVibratorStateChangedListener listener) {
        Objects.requireNonNull(listener);
        Objects.requireNonNull(executor);
        if (this.mVibratorManager == null) {
            Log.w(TAG, "Failed to add vibrate state listener; no vibrator manager.");
            return;
        }
        AllVibratorsStateListener delegate = null;
        try {
            synchronized (this.mRegisteredListeners) {
                if (this.mRegisteredListeners.containsKey(listener)) {
                    Log.w(TAG, "Listener already registered.");
                    if (0 != 0 && delegate.hasRegisteredListeners()) {
                        synchronized (this.mBrokenListeners) {
                            this.mBrokenListeners.add(null);
                        }
                    }
                    tryUnregisterBrokenListeners();
                    return;
                }
                AllVibratorsStateListener delegate2 = new AllVibratorsStateListener(executor, listener);
                delegate2.register(this.mVibratorManager);
                this.mRegisteredListeners.put(listener, delegate2);
                AllVibratorsStateListener delegate3 = null;
                if (0 != 0 && delegate3.hasRegisteredListeners()) {
                    synchronized (this.mBrokenListeners) {
                        this.mBrokenListeners.add(null);
                    }
                }
                tryUnregisterBrokenListeners();
            }
        } catch (Throwable th) {
            if (0 != 0 && delegate.hasRegisteredListeners()) {
                synchronized (this.mBrokenListeners) {
                    this.mBrokenListeners.add(null);
                }
            }
            tryUnregisterBrokenListeners();
            throw th;
        }
    }

    @Override // android.os.Vibrator
    public void removeVibratorStateListener(Vibrator.OnVibratorStateChangedListener listener) {
        Objects.requireNonNull(listener);
        if (this.mVibratorManager == null) {
            Log.w(TAG, "Failed to remove vibrate state listener; no vibrator manager.");
            return;
        }
        synchronized (this.mRegisteredListeners) {
            if (this.mRegisteredListeners.containsKey(listener)) {
                AllVibratorsStateListener delegate = this.mRegisteredListeners.get(listener);
                delegate.unregister(this.mVibratorManager);
                this.mRegisteredListeners.remove(listener);
            }
        }
        tryUnregisterBrokenListeners();
    }

    @Override // android.os.Vibrator
    public boolean hasAmplitudeControl() {
        return getInfo().hasAmplitudeControl();
    }

    @Override // android.os.Vibrator
    public boolean setAlwaysOnEffect(int uid, String opPkg, int alwaysOnId, VibrationEffect effect, VibrationAttributes attrs) {
        if (this.mVibratorManager == null) {
            Log.w(TAG, "Failed to set always-on effect; no vibrator manager.");
            return false;
        }
        CombinedVibration combinedEffect = CombinedVibration.createParallel(effect);
        return this.mVibratorManager.setAlwaysOnEffect(uid, opPkg, alwaysOnId, combinedEffect, attrs);
    }

    @Override // android.os.Vibrator
    public void vibrate(int uid, String opPkg, VibrationEffect effect, String reason, VibrationAttributes attributes) {
        if (this.mVibratorManager == null) {
            Log.w(TAG, "Failed to vibrate; no vibrator manager.");
            return;
        }
        CombinedVibration combinedEffect = CombinedVibration.createParallel(effect);
        this.mVibratorManager.vibrate(uid, opPkg, combinedEffect, reason, attributes);
    }

    @Override // android.os.Vibrator
    public void cancel() {
        VibratorManager vibratorManager = this.mVibratorManager;
        if (vibratorManager == null) {
            Log.w(TAG, "Failed to cancel vibrate; no vibrator manager.");
        } else {
            vibratorManager.cancel();
        }
    }

    @Override // android.os.Vibrator
    public void cancel(int usageFilter) {
        VibratorManager vibratorManager = this.mVibratorManager;
        if (vibratorManager == null) {
            Log.w(TAG, "Failed to cancel vibrate; no vibrator manager.");
        } else {
            vibratorManager.cancel(usageFilter);
        }
    }

    private void tryUnregisterBrokenListeners() {
        synchronized (this.mBrokenListeners) {
            try {
                int i = this.mBrokenListeners.size();
                while (true) {
                    i--;
                    if (i < 0) {
                        break;
                    }
                    this.mBrokenListeners.get(i).unregister(this.mVibratorManager);
                    this.mBrokenListeners.remove(i);
                }
            } catch (RuntimeException e) {
                Log.w(TAG, "Failed to unregister broken listener", e);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static class SingleVibratorStateListener implements Vibrator.OnVibratorStateChangedListener {
        private final AllVibratorsStateListener mAllVibratorsListener;
        private final int mVibratorIdx;

        SingleVibratorStateListener(AllVibratorsStateListener listener, int vibratorIdx) {
            this.mAllVibratorsListener = listener;
            this.mVibratorIdx = vibratorIdx;
        }

        @Override // android.os.Vibrator.OnVibratorStateChangedListener
        public void onVibratorStateChanged(boolean isVibrating) {
            this.mAllVibratorsListener.onVibrating(this.mVibratorIdx, isVibrating);
        }
    }

    /* loaded from: classes2.dex */
    public static class NoVibratorInfo extends VibratorInfo {
        public NoVibratorInfo() {
            super(-1, 0L, new SparseBooleanArray(), new SparseBooleanArray(), new SparseIntArray(), 0, 0, 0, 0, Float.NaN, new VibratorInfo.FrequencyProfile(Float.NaN, Float.NaN, Float.NaN, null));
        }
    }

    /* loaded from: classes2.dex */
    public static class MultiVibratorInfo extends VibratorInfo {
        private static final float EPSILON = 1.0E-5f;

        public MultiVibratorInfo(VibratorInfo[] vibrators) {
            super(-1, capabilitiesIntersection(vibrators), supportedEffectsIntersection(vibrators), supportedBrakingIntersection(vibrators), supportedPrimitivesAndDurationsIntersection(vibrators), integerLimitIntersection(vibrators, new Function() { // from class: android.os.SystemVibrator$MultiVibratorInfo$$ExternalSyntheticLambda0
                @Override // java.util.function.Function
                public final Object apply(Object obj) {
                    return Integer.valueOf(((VibratorInfo) obj).getPrimitiveDelayMax());
                }
            }), integerLimitIntersection(vibrators, new Function() { // from class: android.os.SystemVibrator$MultiVibratorInfo$$ExternalSyntheticLambda1
                @Override // java.util.function.Function
                public final Object apply(Object obj) {
                    return Integer.valueOf(((VibratorInfo) obj).getCompositionSizeMax());
                }
            }), integerLimitIntersection(vibrators, new Function() { // from class: android.os.SystemVibrator$MultiVibratorInfo$$ExternalSyntheticLambda2
                @Override // java.util.function.Function
                public final Object apply(Object obj) {
                    return Integer.valueOf(((VibratorInfo) obj).getPwlePrimitiveDurationMax());
                }
            }), integerLimitIntersection(vibrators, new Function() { // from class: android.os.SystemVibrator$MultiVibratorInfo$$ExternalSyntheticLambda3
                @Override // java.util.function.Function
                public final Object apply(Object obj) {
                    return Integer.valueOf(((VibratorInfo) obj).getPwleSizeMax());
                }
            }), floatPropertyIntersection(vibrators, new Function() { // from class: android.os.SystemVibrator$MultiVibratorInfo$$ExternalSyntheticLambda4
                @Override // java.util.function.Function
                public final Object apply(Object obj) {
                    return Float.valueOf(((VibratorInfo) obj).getQFactor());
                }
            }), frequencyProfileIntersection(vibrators));
        }

        private static int capabilitiesIntersection(VibratorInfo[] infos) {
            int intersection = -1;
            for (VibratorInfo info : infos) {
                intersection = (int) (intersection & info.getCapabilities());
            }
            return intersection;
        }

        private static SparseBooleanArray supportedBrakingIntersection(VibratorInfo[] infos) {
            for (VibratorInfo info : infos) {
                if (!info.isBrakingSupportKnown()) {
                    return null;
                }
            }
            SparseBooleanArray intersection = new SparseBooleanArray();
            SparseBooleanArray firstVibratorBraking = infos[0].getSupportedBraking();
            for (int i = 0; i < firstVibratorBraking.size(); i++) {
                int brakingId = firstVibratorBraking.keyAt(i);
                if (firstVibratorBraking.valueAt(i)) {
                    int j = 1;
                    while (true) {
                        if (j < infos.length) {
                            if (!infos[j].hasBrakingSupport(brakingId)) {
                                break;
                            }
                            j++;
                        } else {
                            intersection.put(brakingId, true);
                            break;
                        }
                    }
                }
            }
            return intersection;
        }

        private static SparseBooleanArray supportedEffectsIntersection(VibratorInfo[] infos) {
            for (VibratorInfo info : infos) {
                if (!info.isEffectSupportKnown()) {
                    return null;
                }
            }
            SparseBooleanArray intersection = new SparseBooleanArray();
            SparseBooleanArray firstVibratorEffects = infos[0].getSupportedEffects();
            for (int i = 0; i < firstVibratorEffects.size(); i++) {
                int effectId = firstVibratorEffects.keyAt(i);
                if (firstVibratorEffects.valueAt(i)) {
                    int j = 1;
                    while (true) {
                        if (j < infos.length) {
                            if (infos[j].isEffectSupported(effectId) != 1) {
                                break;
                            }
                            j++;
                        } else {
                            intersection.put(effectId, true);
                            break;
                        }
                    }
                }
            }
            return intersection;
        }

        private static SparseIntArray supportedPrimitivesAndDurationsIntersection(VibratorInfo[] infos) {
            SparseIntArray intersection = new SparseIntArray();
            SparseIntArray firstVibratorPrimitives = infos[0].getSupportedPrimitives();
            for (int i = 0; i < firstVibratorPrimitives.size(); i++) {
                int primitiveId = firstVibratorPrimitives.keyAt(i);
                int primitiveDuration = firstVibratorPrimitives.valueAt(i);
                if (primitiveDuration != 0) {
                    int j = 1;
                    while (true) {
                        if (j < infos.length) {
                            int vibratorPrimitiveDuration = infos[j].getPrimitiveDuration(primitiveId);
                            if (vibratorPrimitiveDuration == 0) {
                                break;
                            }
                            primitiveDuration = Math.max(primitiveDuration, vibratorPrimitiveDuration);
                            j++;
                        } else {
                            intersection.put(primitiveId, primitiveDuration);
                            break;
                        }
                    }
                }
            }
            return intersection;
        }

        private static int integerLimitIntersection(VibratorInfo[] infos, Function<VibratorInfo, Integer> propertyGetter) {
            int limit = 0;
            for (VibratorInfo info : infos) {
                int vibratorLimit = propertyGetter.apply(info).intValue();
                if (limit == 0 || (vibratorLimit > 0 && vibratorLimit < limit)) {
                    limit = vibratorLimit;
                }
            }
            return limit;
        }

        private static float floatPropertyIntersection(VibratorInfo[] infos, Function<VibratorInfo, Float> propertyGetter) {
            float property = propertyGetter.apply(infos[0]).floatValue();
            if (Float.isNaN(property)) {
                return Float.NaN;
            }
            for (int i = 1; i < infos.length; i++) {
                if (Float.compare(property, propertyGetter.apply(infos[i]).floatValue()) != 0) {
                    return Float.NaN;
                }
            }
            return property;
        }

        private static VibratorInfo.FrequencyProfile frequencyProfileIntersection(VibratorInfo[] infos) {
            float freqResolution = floatPropertyIntersection(infos, new Function() { // from class: android.os.SystemVibrator$MultiVibratorInfo$$ExternalSyntheticLambda5
                @Override // java.util.function.Function
                public final Object apply(Object obj) {
                    Float valueOf;
                    valueOf = Float.valueOf(((VibratorInfo) obj).getFrequencyProfile().getFrequencyResolutionHz());
                    return valueOf;
                }
            });
            float resonantFreq = floatPropertyIntersection(infos, new Function() { // from class: android.os.SystemVibrator$MultiVibratorInfo$$ExternalSyntheticLambda6
                @Override // java.util.function.Function
                public final Object apply(Object obj) {
                    return Float.valueOf(((VibratorInfo) obj).getResonantFrequencyHz());
                }
            });
            Range<Float> freqRange = frequencyRangeIntersection(infos, freqResolution);
            if (freqRange == null || Float.isNaN(freqResolution)) {
                return new VibratorInfo.FrequencyProfile(resonantFreq, Float.NaN, freqResolution, null);
            }
            int amplitudeCount = Math.round(((freqRange.getUpper().floatValue() - freqRange.getLower().floatValue()) / freqResolution) + 1.0f);
            float[] maxAmplitudes = new float[amplitudeCount];
            Arrays.fill(maxAmplitudes, Float.MAX_VALUE);
            for (VibratorInfo info : infos) {
                Range<Float> vibratorFreqRange = info.getFrequencyProfile().getFrequencyRangeHz();
                float[] vibratorMaxAmplitudes = info.getFrequencyProfile().getMaxAmplitudes();
                int vibratorStartIdx = Math.round((freqRange.getLower().floatValue() - vibratorFreqRange.getLower().floatValue()) / freqResolution);
                int vibratorEndIdx = (maxAmplitudes.length + vibratorStartIdx) - 1;
                if (vibratorStartIdx < 0 || vibratorEndIdx >= vibratorMaxAmplitudes.length) {
                    Slog.w(SystemVibrator.TAG, "Error calculating the intersection of vibrator frequency profiles: attempted to fetch from vibrator " + info.getId() + " max amplitude with bad index " + vibratorStartIdx);
                    return new VibratorInfo.FrequencyProfile(resonantFreq, Float.NaN, Float.NaN, null);
                }
                for (int i = 0; i < maxAmplitudes.length; i++) {
                    maxAmplitudes[i] = Math.min(maxAmplitudes[i], vibratorMaxAmplitudes[vibratorStartIdx + i]);
                }
            }
            return new VibratorInfo.FrequencyProfile(resonantFreq, freqRange.getLower().floatValue(), freqResolution, maxAmplitudes);
        }

        private static Range<Float> frequencyRangeIntersection(VibratorInfo[] infos, float frequencyResolution) {
            Range<Float> firstRange = infos[0].getFrequencyProfile().getFrequencyRangeHz();
            if (firstRange == null) {
                return null;
            }
            float intersectionLower = firstRange.getLower().floatValue();
            float intersectionUpper = firstRange.getUpper().floatValue();
            for (int i = 1; i < infos.length; i++) {
                Range<Float> vibratorRange = infos[i].getFrequencyProfile().getFrequencyRangeHz();
                if (vibratorRange == null || vibratorRange.getLower().floatValue() >= intersectionUpper || vibratorRange.getUpper().floatValue() <= intersectionLower) {
                    return null;
                }
                float frequencyDelta = Math.abs(intersectionLower - vibratorRange.getLower().floatValue());
                if (frequencyDelta % frequencyResolution > EPSILON) {
                    return null;
                }
                intersectionLower = Math.max(intersectionLower, vibratorRange.getLower().floatValue());
                intersectionUpper = Math.min(intersectionUpper, vibratorRange.getUpper().floatValue());
            }
            if (intersectionUpper - intersectionLower < frequencyResolution) {
                return null;
            }
            return Range.create(Float.valueOf(intersectionLower), Float.valueOf(intersectionUpper));
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static class AllVibratorsStateListener {
        private final Vibrator.OnVibratorStateChangedListener mDelegate;
        private final Executor mExecutor;
        private int mInitializedMask;
        private int mVibratingMask;
        private final Object mLock = new Object();
        private final SparseArray<SingleVibratorStateListener> mVibratorListeners = new SparseArray<>();

        AllVibratorsStateListener(Executor executor, Vibrator.OnVibratorStateChangedListener listener) {
            this.mExecutor = executor;
            this.mDelegate = listener;
        }

        boolean hasRegisteredListeners() {
            boolean z;
            synchronized (this.mLock) {
                z = this.mVibratorListeners.size() > 0;
            }
            return z;
        }

        void register(VibratorManager vibratorManager) {
            int[] vibratorIds = vibratorManager.getVibratorIds();
            synchronized (this.mLock) {
                for (int i = 0; i < vibratorIds.length; i++) {
                    int vibratorId = vibratorIds[i];
                    SingleVibratorStateListener listener = new SingleVibratorStateListener(this, i);
                    try {
                        vibratorManager.getVibrator(vibratorId).addVibratorStateListener(this.mExecutor, listener);
                        this.mVibratorListeners.put(vibratorId, listener);
                    } catch (RuntimeException e) {
                        try {
                            unregister(vibratorManager);
                        } catch (RuntimeException e1) {
                            Log.w(SystemVibrator.TAG, "Failed to unregister listener while recovering from a failed register call", e1);
                        }
                        throw e;
                    }
                }
            }
        }

        void unregister(VibratorManager vibratorManager) {
            synchronized (this.mLock) {
                int i = this.mVibratorListeners.size();
                while (true) {
                    i--;
                    if (i >= 0) {
                        int vibratorId = this.mVibratorListeners.keyAt(i);
                        SingleVibratorStateListener listener = this.mVibratorListeners.valueAt(i);
                        vibratorManager.getVibrator(vibratorId).removeVibratorStateListener(listener);
                        this.mVibratorListeners.removeAt(i);
                    }
                }
            }
        }

        void onVibrating(final int vibratorIdx, final boolean vibrating) {
            this.mExecutor.execute(new Runnable() { // from class: android.os.SystemVibrator$AllVibratorsStateListener$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    SystemVibrator.AllVibratorsStateListener.this.m3062x418a3113(vibratorIdx, vibrating);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onVibrating$0$android-os-SystemVibrator$AllVibratorsStateListener  reason: not valid java name */
        public /* synthetic */ void m3062x418a3113(int vibratorIdx, boolean vibrating) {
            synchronized (this.mLock) {
                boolean z = true;
                int allInitializedMask = (1 << this.mVibratorListeners.size()) - 1;
                int vibratorMask = 1 << vibratorIdx;
                int i = this.mInitializedMask;
                if ((i & vibratorMask) == 0) {
                    this.mInitializedMask = i | vibratorMask;
                    this.mVibratingMask |= vibrating ? vibratorMask : 0;
                } else {
                    int i2 = this.mVibratingMask;
                    boolean prevVibrating = (i2 & vibratorMask) != 0;
                    if (prevVibrating != vibrating) {
                        this.mVibratingMask = i2 ^ vibratorMask;
                    }
                }
                if (this.mInitializedMask != allInitializedMask) {
                    return;
                }
                if (this.mVibratingMask == 0) {
                    z = false;
                }
                boolean anyVibrating = z;
                this.mDelegate.onVibratorStateChanged(anyVibrating);
            }
        }
    }
}
