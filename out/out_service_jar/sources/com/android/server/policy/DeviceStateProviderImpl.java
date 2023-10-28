package com.android.server.policy;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.input.InputManagerInternal;
import android.os.Environment;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Slog;
import android.util.SparseArray;
import com.android.internal.util.Preconditions;
import com.android.server.LocalServices;
import com.android.server.devicestate.DeviceState;
import com.android.server.devicestate.DeviceStateProvider;
import com.android.server.policy.devicestate.config.Conditions;
import com.android.server.policy.devicestate.config.DeviceStateConfig;
import com.android.server.policy.devicestate.config.Flags;
import com.android.server.policy.devicestate.config.LidSwitchCondition;
import com.android.server.policy.devicestate.config.NumericRange;
import com.android.server.policy.devicestate.config.SensorCondition;
import com.android.server.policy.devicestate.config.XmlParser;
import com.transsion.hubcore.server.policy.ITranDeviceStateProviderImpl;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.ToIntFunction;
import javax.xml.datatype.DatatypeConfigurationException;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes2.dex */
public final class DeviceStateProviderImpl implements DeviceStateProvider, InputManagerInternal.LidSwitchCallback, SensorEventListener {
    private static final String CONFIG_FILE_NAME = "device_state_configuration.xml";
    private static final String DATA_CONFIG_FILE_PATH = "system/devicestate/";
    private static final boolean DEBUG = false;
    private static final String FLAG_APP_INACCESSIBLE = "FLAG_APP_INACCESSIBLE";
    private static final String FLAG_CANCEL_OVERRIDE_REQUESTS = "FLAG_CANCEL_OVERRIDE_REQUESTS";
    private static final String TAG = "DeviceStateProviderImpl";
    private static final String VENDOR_CONFIG_FILE_PATH = "etc/devicestate/";
    private final Context mContext;
    private Boolean mIsLidOpen;
    private final DeviceState[] mOrderedStates;
    private static final BooleanSupplier TRUE_BOOLEAN_SUPPLIER = new BooleanSupplier() { // from class: com.android.server.policy.DeviceStateProviderImpl$$ExternalSyntheticLambda1
        @Override // java.util.function.BooleanSupplier
        public final boolean getAsBoolean() {
            return DeviceStateProviderImpl.lambda$static$0();
        }
    };
    private static final BooleanSupplier FALSE_BOOLEAN_SUPPLIER = new BooleanSupplier() { // from class: com.android.server.policy.DeviceStateProviderImpl$$ExternalSyntheticLambda2
        @Override // java.util.function.BooleanSupplier
        public final boolean getAsBoolean() {
            return DeviceStateProviderImpl.lambda$static$1();
        }
    };
    static final DeviceState DEFAULT_DEVICE_STATE = new DeviceState(0, "DEFAULT", 0);
    private final Object mLock = new Object();
    private final SparseArray<BooleanSupplier> mStateConditions = new SparseArray<>();
    private DeviceStateProvider.Listener mListener = null;
    private int mLastReportedState = -1;
    private final Map<Sensor, SensorEvent> mLatestSensorEvent = new ArrayMap();

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public interface ReadableConfig {
        InputStream openRead() throws IOException;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$static$0() {
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$static$1() {
        return false;
    }

    public static DeviceStateProviderImpl create(Context context) {
        File configFile = getConfigurationFile();
        if (configFile == null) {
            return createFromConfig(context, null);
        }
        return createFromConfig(context, new ReadableFileConfig(configFile));
    }

    static DeviceStateProviderImpl createFromConfig(Context context, ReadableConfig readableConfig) {
        DeviceStateConfig config;
        List<DeviceState> deviceStateList = new ArrayList<>();
        List<Conditions> conditionsList = new ArrayList<>();
        if (readableConfig != null && (config = parseConfig(readableConfig)) != null) {
            for (com.android.server.policy.devicestate.config.DeviceState stateConfig : config.getDeviceState()) {
                int state = stateConfig.getIdentifier().intValue();
                String name = stateConfig.getName() == null ? "" : stateConfig.getName();
                int flags = 0;
                Flags configFlags = stateConfig.getFlags();
                if (configFlags != null) {
                    List<String> configFlagStrings = configFlags.getFlag();
                    for (int i = 0; i < configFlagStrings.size(); i++) {
                        String configFlagString = configFlagStrings.get(i);
                        char c = 65535;
                        switch (configFlagString.hashCode()) {
                            case -1134441332:
                                if (configFlagString.equals(FLAG_APP_INACCESSIBLE)) {
                                    c = 1;
                                    break;
                                }
                                break;
                            case -1054037563:
                                if (configFlagString.equals(FLAG_CANCEL_OVERRIDE_REQUESTS)) {
                                    c = 0;
                                    break;
                                }
                                break;
                        }
                        switch (c) {
                            case 0:
                                flags |= 1;
                                break;
                            case 1:
                                flags |= 2;
                                break;
                            default:
                                Slog.w(TAG, "Parsed unknown flag with name: " + configFlagString);
                                break;
                        }
                    }
                }
                deviceStateList.add(new DeviceState(state, name, flags));
                Conditions condition = stateConfig.getConditions();
                conditionsList.add(condition);
            }
        }
        if (deviceStateList.size() == 0) {
            deviceStateList.add(DEFAULT_DEVICE_STATE);
            conditionsList.add(null);
        }
        return new DeviceStateProviderImpl(context, deviceStateList, conditionsList);
    }

    private DeviceStateProviderImpl(Context context, List<DeviceState> deviceStates, List<Conditions> stateConditions) {
        Preconditions.checkArgument(deviceStates.size() == stateConditions.size(), "Number of device states must be equal to the number of device state conditions.");
        this.mContext = context;
        DeviceState[] orderedStates = (DeviceState[]) deviceStates.toArray(new DeviceState[deviceStates.size()]);
        Arrays.sort(orderedStates, Comparator.comparingInt(new ToIntFunction() { // from class: com.android.server.policy.DeviceStateProviderImpl$$ExternalSyntheticLambda0
            @Override // java.util.function.ToIntFunction
            public final int applyAsInt(Object obj) {
                return ((DeviceState) obj).getIdentifier();
            }
        }));
        this.mOrderedStates = orderedStates;
        ITranDeviceStateProviderImpl.Instance().onConstruct(this, new ITranDeviceStateProviderImpl.ConstructParameters() { // from class: com.android.server.policy.DeviceStateProviderImpl.1
            @Override // com.transsion.hubcore.server.policy.ITranDeviceStateProviderImpl.ConstructParameters
            public Map<Sensor, SensorEvent> getLatestSensorEventMap() {
                return DeviceStateProviderImpl.this.mLatestSensorEvent;
            }
        });
        setStateConditions(deviceStates, stateConditions);
    }

    private void setStateConditions(List<DeviceState> deviceStates, List<Conditions> stateConditions) {
        boolean shouldListenToLidSwitch = false;
        ArraySet<Sensor> sensorsToListenTo = new ArraySet<>();
        for (int i = 0; i < stateConditions.size(); i++) {
            int state = deviceStates.get(i).getIdentifier();
            Conditions conditions = stateConditions.get(i);
            if (conditions == null) {
                this.mStateConditions.put(state, TRUE_BOOLEAN_SUPPLIER);
            } else {
                boolean allRequiredComponentsFound = true;
                boolean lidSwitchRequired = false;
                ArraySet<Sensor> sensorsRequired = new ArraySet<>();
                List<BooleanSupplier> suppliers = new ArrayList<>();
                LidSwitchCondition lidSwitchCondition = conditions.getLidSwitch();
                if (lidSwitchCondition != null) {
                    suppliers.add(new LidSwitchBooleanSupplier(lidSwitchCondition.getOpen()));
                    lidSwitchRequired = true;
                }
                List<SensorCondition> sensorConditions = conditions.getSensor();
                int j = 0;
                while (true) {
                    if (j >= sensorConditions.size()) {
                        break;
                    }
                    SensorCondition sensorCondition = sensorConditions.get(j);
                    String expectedSensorType = sensorCondition.getType();
                    String expectedSensorName = sensorCondition.getName();
                    Conditions conditions2 = conditions;
                    Sensor foundSensor = findSensor(expectedSensorType, expectedSensorName);
                    if (foundSensor == null) {
                        Slog.e(TAG, "Failed to find Sensor with type: " + expectedSensorType + " and name: " + expectedSensorName);
                        allRequiredComponentsFound = false;
                        break;
                    }
                    suppliers.add(new SensorBooleanSupplier(foundSensor, sensorCondition.getValue()));
                    sensorsRequired.add(foundSensor);
                    j++;
                    conditions = conditions2;
                    allRequiredComponentsFound = allRequiredComponentsFound;
                    lidSwitchCondition = lidSwitchCondition;
                }
                if (allRequiredComponentsFound) {
                    shouldListenToLidSwitch |= lidSwitchRequired;
                    sensorsToListenTo.addAll((ArraySet<? extends Sensor>) sensorsRequired);
                    if (suppliers.size() > 1) {
                        this.mStateConditions.put(state, new AndBooleanSupplier(suppliers));
                    } else if (suppliers.size() > 0) {
                        this.mStateConditions.put(state, suppliers.get(0));
                    } else {
                        this.mStateConditions.put(state, TRUE_BOOLEAN_SUPPLIER);
                    }
                } else {
                    this.mStateConditions.put(state, FALSE_BOOLEAN_SUPPLIER);
                }
            }
        }
        if (shouldListenToLidSwitch) {
            InputManagerInternal inputManager = (InputManagerInternal) LocalServices.getService(InputManagerInternal.class);
            inputManager.registerLidSwitchCallback(this);
        }
        SensorManager sensorManager = (SensorManager) this.mContext.getSystemService(SensorManager.class);
        for (int i2 = 0; i2 < sensorsToListenTo.size(); i2++) {
            Sensor sensor = sensorsToListenTo.valueAt(i2);
            ITranDeviceStateProviderImpl.Instance().hookSensorBeforeRegister(sensor, sensorManager);
            sensorManager.registerListener(this, sensor, 0);
        }
    }

    private Sensor findSensor(String type, String name) {
        SensorManager sensorManager = (SensorManager) this.mContext.getSystemService(SensorManager.class);
        List<Sensor> sensors = sensorManager.getSensorList(-1);
        for (int sensorIndex = 0; sensorIndex < sensors.size(); sensorIndex++) {
            Sensor sensor = sensors.get(sensorIndex);
            String sensorType = sensor.getStringType();
            String sensorName = sensor.getName();
            if (sensorType != null && sensorName != null && sensorType.equals(type) && sensorName.equals(name)) {
                return sensor;
            }
        }
        return null;
    }

    @Override // com.android.server.devicestate.DeviceStateProvider
    public void setListener(DeviceStateProvider.Listener listener) {
        synchronized (this.mLock) {
            if (this.mListener != null) {
                throw new RuntimeException("Provider already has a listener set.");
            }
            this.mListener = listener;
        }
        notifySupportedStatesChanged();
        notifyDeviceStateChangedIfNeeded();
    }

    private void notifySupportedStatesChanged() {
        synchronized (this.mLock) {
            if (this.mListener == null) {
                return;
            }
            DeviceState[] deviceStateArr = this.mOrderedStates;
            DeviceState[] supportedStates = (DeviceState[]) Arrays.copyOf(deviceStateArr, deviceStateArr.length);
            this.mListener.onSupportedDeviceStatesChanged(supportedStates);
        }
    }

    void notifyDeviceStateChangedIfNeeded() {
        int stateToReport = -1;
        synchronized (this.mLock) {
            if (this.mListener == null) {
                return;
            }
            int newState = this.mOrderedStates[0].getIdentifier();
            int i = 0;
            while (true) {
                DeviceState[] deviceStateArr = this.mOrderedStates;
                if (i >= deviceStateArr.length) {
                    break;
                }
                int state = deviceStateArr[i].getIdentifier();
                try {
                    boolean conditionSatisfied = this.mStateConditions.get(state).getAsBoolean();
                    if (!conditionSatisfied) {
                        i++;
                    } else {
                        newState = state;
                        break;
                    }
                } catch (IllegalStateException e) {
                    return;
                }
            }
            int i2 = this.mLastReportedState;
            if (newState != i2) {
                this.mLastReportedState = newState;
                stateToReport = newState;
            }
            if (stateToReport != -1) {
                this.mListener.onStateChanged(stateToReport);
            }
        }
    }

    public void notifyLidSwitchChanged(long whenNanos, boolean lidOpen) {
        synchronized (this.mLock) {
            this.mIsLidOpen = Boolean.valueOf(lidOpen);
        }
        notifyDeviceStateChangedIfNeeded();
    }

    @Override // com.android.server.devicestate.DeviceStateProvider
    public void onSystemBootedEnd() {
        ITranDeviceStateProviderImpl.Instance().onSystemBootedEnd();
    }

    @Override // com.android.server.devicestate.DeviceStateProvider
    public void setHallKeyStateUp(boolean isUp) {
        ITranDeviceStateProviderImpl.Instance().setHallKeyStateUp(isUp);
    }

    @Override // android.hardware.SensorEventListener
    public void onSensorChanged(SensorEvent event) {
        synchronized (this.mLock) {
            if (ITranDeviceStateProviderImpl.Instance().isSensorValueInValid(event)) {
                return;
            }
            this.mLatestSensorEvent.put(event.sensor, event);
            notifyDeviceStateChangedIfNeeded();
        }
    }

    @Override // android.hardware.SensorEventListener
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public final class LidSwitchBooleanSupplier implements BooleanSupplier {
        private final boolean mExpectedOpen;

        LidSwitchBooleanSupplier(boolean expectedOpen) {
            this.mExpectedOpen = expectedOpen;
        }

        @Override // java.util.function.BooleanSupplier
        public boolean getAsBoolean() {
            boolean z;
            synchronized (DeviceStateProviderImpl.this.mLock) {
                if (DeviceStateProviderImpl.this.mIsLidOpen == null) {
                    throw new IllegalStateException("Have not received lid switch value.");
                }
                z = DeviceStateProviderImpl.this.mIsLidOpen.booleanValue() == this.mExpectedOpen;
            }
            return z;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public final class SensorBooleanSupplier implements BooleanSupplier {
        private final List<NumericRange> mExpectedValues;
        private final Sensor mSensor;

        SensorBooleanSupplier(Sensor sensor, List<NumericRange> expectedValues) {
            this.mSensor = sensor;
            this.mExpectedValues = expectedValues;
        }

        @Override // java.util.function.BooleanSupplier
        public boolean getAsBoolean() {
            synchronized (DeviceStateProviderImpl.this.mLock) {
                SensorEvent latestEvent = (SensorEvent) DeviceStateProviderImpl.this.mLatestSensorEvent.get(this.mSensor);
                if (latestEvent == null) {
                    throw new IllegalStateException("Have not received sensor event.");
                }
                if (latestEvent.values.length < this.mExpectedValues.size()) {
                    throw new RuntimeException("Number of supplied numeric range(s) does not match the number of values in the latest sensor event for sensor: " + this.mSensor);
                }
                for (int i = 0; i < this.mExpectedValues.size(); i++) {
                    if (!adheresToRange(latestEvent.values[i], this.mExpectedValues.get(i))) {
                        return false;
                    }
                }
                return true;
            }
        }

        private boolean adheresToRange(float value, NumericRange range) {
            BigDecimal min = range.getMin_optional();
            if (min != null && value <= min.floatValue()) {
                return false;
            }
            BigDecimal minInclusive = range.getMinInclusive_optional();
            if (minInclusive != null && value < minInclusive.floatValue()) {
                return false;
            }
            BigDecimal max = range.getMax_optional();
            if (max != null && value >= max.floatValue()) {
                return false;
            }
            BigDecimal maxInclusive = range.getMaxInclusive_optional();
            if (maxInclusive != null && value > maxInclusive.floatValue()) {
                return false;
            }
            return true;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static final class AndBooleanSupplier implements BooleanSupplier {
        List<BooleanSupplier> mBooleanSuppliers;

        AndBooleanSupplier(List<BooleanSupplier> booleanSuppliers) {
            this.mBooleanSuppliers = booleanSuppliers;
        }

        @Override // java.util.function.BooleanSupplier
        public boolean getAsBoolean() {
            for (int i = 0; i < this.mBooleanSuppliers.size(); i++) {
                if (!this.mBooleanSuppliers.get(i).getAsBoolean()) {
                    return false;
                }
            }
            return true;
        }
    }

    private static File getConfigurationFile() {
        File configFileFromDataDir = Environment.buildPath(Environment.getDataDirectory(), new String[]{DATA_CONFIG_FILE_PATH, CONFIG_FILE_NAME});
        if (configFileFromDataDir.exists()) {
            return configFileFromDataDir;
        }
        File configFileFromVendorDir = Environment.buildPath(Environment.getVendorDirectory(), new String[]{VENDOR_CONFIG_FILE_PATH, CONFIG_FILE_NAME});
        if (configFileFromVendorDir.exists()) {
            return configFileFromVendorDir;
        }
        return null;
    }

    private static DeviceStateConfig parseConfig(ReadableConfig readableConfig) {
        try {
            InputStream in = readableConfig.openRead();
            InputStream bin = new BufferedInputStream(in);
            DeviceStateConfig read = XmlParser.read(bin);
            bin.close();
            if (in != null) {
                in.close();
            }
            return read;
        } catch (IOException | DatatypeConfigurationException | XmlPullParserException e) {
            Slog.e(TAG, "Encountered an error while reading device state config", e);
            return null;
        }
    }

    /* loaded from: classes2.dex */
    private static final class ReadableFileConfig implements ReadableConfig {
        private final File mFile;

        private ReadableFileConfig(File file) {
            this.mFile = file;
        }

        @Override // com.android.server.policy.DeviceStateProviderImpl.ReadableConfig
        public InputStream openRead() throws IOException {
            return new FileInputStream(this.mFile);
        }
    }
}
