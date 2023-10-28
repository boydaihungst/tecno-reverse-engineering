package com.android.server;

import android.app.ActivityManager;
import android.app.AppOpsManager;
import android.app.compat.CompatChanges;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Process;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.DeviceConfig;
import android.telephony.BarringInfo;
import android.telephony.CallAttributes;
import android.telephony.CallQuality;
import android.telephony.CellIdentity;
import android.telephony.CellInfo;
import android.telephony.CellSignalStrength;
import android.telephony.CellSignalStrengthCdma;
import android.telephony.CellSignalStrengthGsm;
import android.telephony.CellSignalStrengthLte;
import android.telephony.CellSignalStrengthNr;
import android.telephony.CellSignalStrengthTdscdma;
import android.telephony.CellSignalStrengthWcdma;
import android.telephony.LinkCapacityEstimate;
import android.telephony.LocationAccessPolicy;
import android.telephony.PhoneCapability;
import android.telephony.PhysicalChannelConfig;
import android.telephony.PreciseCallState;
import android.telephony.PreciseDataConnectionState;
import android.telephony.Rlog;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyDisplayInfo;
import android.telephony.TelephonyManager;
import android.telephony.data.ApnSetting;
import android.telephony.emergency.EmergencyNumber;
import android.telephony.ims.ImsReasonInfo;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.LocalLog;
import android.util.Pair;
import com.android.internal.app.IBatteryStats;
import com.android.internal.telephony.ICarrierPrivilegesCallback;
import com.android.internal.telephony.IOnSubscriptionsChangedListener;
import com.android.internal.telephony.IPhoneStateListener;
import com.android.internal.telephony.ITelephonyRegistry;
import com.android.internal.telephony.TelephonyPermissions;
import com.android.internal.telephony.util.TelephonyUtils;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.FrameworkStatsLog;
import com.android.internal.util.FunctionalUtils;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.am.BatteryStatsService;
import com.android.server.health.HealthServiceWrapperHidl;
import dalvik.annotation.optimization.NeverCompile;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.function.IntPredicate;
import java.util.stream.Collectors;
/* loaded from: classes.dex */
public class TelephonyRegistry extends ITelephonyRegistry.Stub {
    private static final String ACTION_ANY_DATA_CONNECTION_STATE_CHANGED = "android.intent.action.ANY_DATA_STATE";
    public static final String ACTION_SIGNAL_STRENGTH_CHANGED = "android.intent.action.SIG_STR";
    private static final String ACTION_SUBSCRIPTION_PHONE_STATE_CHANGED = "android.intent.action.SUBSCRIPTION_PHONE_STATE";
    private static final boolean DBG = false;
    private static final boolean DBG_LOC = false;
    private static final long DISPLAY_INFO_NR_ADVANCED_SUPPORTED = 181658987;
    private static final List<LinkCapacityEstimate> INVALID_LCE_LIST;
    private static final int MSG_UPDATE_DEFAULT_SUB = 2;
    private static final int MSG_USER_SWITCHED = 1;
    private static final String PHONE_CONSTANTS_DATA_APN_KEY = "apn";
    private static final String PHONE_CONSTANTS_DATA_APN_TYPE_KEY = "apnType";
    private static final String PHONE_CONSTANTS_SLOT_KEY = "slot";
    private static final String PHONE_CONSTANTS_STATE_KEY = "state";
    private static final String PHONE_CONSTANTS_SUBSCRIPTION_KEY = "subscription";
    private static final Set<Integer> REQUIRE_PRECISE_PHONE_STATE_PERMISSION;
    private static final long REQUIRE_READ_PHONE_STATE_PERMISSION_FOR_ACTIVE_DATA_SUB_ID = 182478738;
    private static final long REQUIRE_READ_PHONE_STATE_PERMISSION_FOR_CELL_INFO = 184323934;
    private static final long REQUIRE_READ_PHONE_STATE_PERMISSION_FOR_DISPLAY_INFO = 183164979;
    private static final String TAG = "TelephonyRegistry";
    private static final boolean VDBG = false;
    private static Constructor sMtkSignalStrength;
    private static Constructor sMtkSignalStrength2;
    private int[] mAllowedNetworkTypeReason;
    private long[] mAllowedNetworkTypeValue;
    private final AppOpsManager mAppOps;
    private int[] mBackgroundCallState;
    private List<BarringInfo> mBarringInfo;
    private CallAttributes[] mCallAttributes;
    private int[] mCallDisconnectCause;
    private boolean[] mCallForwarding;
    private String[] mCallIncomingNumber;
    private int[] mCallNetworkType;
    private int[] mCallPreciseDisconnectCause;
    private CallQuality[] mCallQuality;
    private int[] mCallState;
    private boolean[] mCarrierNetworkChangeState;
    private List<Pair<List<String>, int[]>> mCarrierPrivilegeStates;
    private List<Pair<String, Integer>> mCarrierServiceStates;
    private CellIdentity[] mCellIdentity;
    private ArrayList<List<CellInfo>> mCellInfo;
    private ConfigurationProvider mConfigurationProvider;
    private final Context mContext;
    private int[] mDataActivationState;
    private int[] mDataActivity;
    private int[] mDataConnectionNetworkType;
    private int[] mDataConnectionState;
    private int[] mDataEnabledReason;
    private Map<Integer, List<EmergencyNumber>> mEmergencyNumberList;
    private int[] mForegroundCallState;
    private List<ImsReasonInfo> mImsReasonInfo;
    private boolean[] mIsDataEnabled;
    private List<List<LinkCapacityEstimate>> mLinkCapacityEstimateLists;
    private boolean[] mMessageWaiting;
    private int mNumPhones;
    private EmergencyNumber[] mOutgoingCallEmergencyNumber;
    private EmergencyNumber[] mOutgoingSmsEmergencyNumber;
    private List<List<PhysicalChannelConfig>> mPhysicalChannelConfigs;
    private PreciseCallState[] mPreciseCallState;
    private List<Map<Pair<Integer, ApnSetting>, PreciseDataConnectionState>> mPreciseDataConnectionStates;
    private int[] mRadioPowerState;
    private int[] mRingingCallState;
    private ServiceState[] mServiceState;
    private SignalStrength[] mSignalStrength;
    private int[] mSrvccState;
    private TelephonyDisplayInfo[] mTelephonyDisplayInfos;
    private boolean[] mUserMobileDataState;
    private int[] mVoiceActivationState;
    private final ArrayList<IBinder> mRemoveList = new ArrayList<>();
    private final ArrayList<Record> mRecords = new ArrayList<>();
    private boolean mHasNotifySubscriptionInfoChangedOccurred = false;
    private boolean mHasNotifyOpportunisticSubscriptionInfoChangedOccurred = false;
    private int mDefaultSubId = -1;
    private int mDefaultPhoneId = -1;
    private PhoneCapability mPhoneCapability = null;
    private int mActiveDataSubId = -1;
    private final LocalLog mLocalLog = new LocalLog(200);
    private final LocalLog mListenLog = new LocalLog(200);
    private final Handler mHandler = new Handler() { // from class: com.android.server.TelephonyRegistry.1
        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            int subId;
            switch (msg.what) {
                case 1:
                    int numPhones = TelephonyRegistry.this.getTelephonyManager().getActiveModemCount();
                    for (int phoneId = 0; phoneId < numPhones; phoneId++) {
                        int[] subIds = SubscriptionManager.getSubId(phoneId);
                        if (subIds != null && subIds.length > 0) {
                            subId = subIds[0];
                        } else {
                            subId = Integer.MAX_VALUE;
                        }
                        TelephonyRegistry telephonyRegistry = TelephonyRegistry.this;
                        telephonyRegistry.notifyCellLocationForSubscriber(subId, telephonyRegistry.mCellIdentity[phoneId], true);
                    }
                    return;
                case 2:
                    int newDefaultPhoneId = msg.arg1;
                    int newDefaultSubId = msg.arg2;
                    synchronized (TelephonyRegistry.this.mRecords) {
                        Iterator it = TelephonyRegistry.this.mRecords.iterator();
                        while (it.hasNext()) {
                            Record r = (Record) it.next();
                            if (r.subId == Integer.MAX_VALUE) {
                                TelephonyRegistry.this.checkPossibleMissNotify(r, newDefaultPhoneId);
                            }
                        }
                        TelephonyRegistry.this.handleRemoveListLocked();
                    }
                    TelephonyRegistry.this.mDefaultSubId = newDefaultSubId;
                    TelephonyRegistry.this.mDefaultPhoneId = newDefaultPhoneId;
                    TelephonyRegistry.this.mLocalLog.log("Default subscription updated: mDefaultPhoneId=" + TelephonyRegistry.this.mDefaultPhoneId + ", mDefaultSubId=" + TelephonyRegistry.this.mDefaultSubId);
                    return;
                default:
                    return;
            }
        }
    };
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() { // from class: com.android.server.TelephonyRegistry.2
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("android.intent.action.USER_SWITCHED".equals(action)) {
                int userHandle = intent.getIntExtra("android.intent.extra.user_handle", 0);
                TelephonyRegistry.this.mHandler.sendMessage(TelephonyRegistry.this.mHandler.obtainMessage(1, userHandle, 0));
            } else if (action.equals("android.telephony.action.DEFAULT_SUBSCRIPTION_CHANGED")) {
                int newDefaultSubId = intent.getIntExtra("android.telephony.extra.SUBSCRIPTION_INDEX", SubscriptionManager.getDefaultSubscriptionId());
                int newDefaultPhoneId = intent.getIntExtra("android.telephony.extra.SLOT_INDEX", TelephonyRegistry.this.getPhoneIdFromSubId(newDefaultSubId));
                if (TelephonyRegistry.this.validatePhoneId(newDefaultPhoneId)) {
                    if (newDefaultSubId != TelephonyRegistry.this.mDefaultSubId || newDefaultPhoneId != TelephonyRegistry.this.mDefaultPhoneId) {
                        TelephonyRegistry.this.mHandler.sendMessage(TelephonyRegistry.this.mHandler.obtainMessage(2, newDefaultPhoneId, newDefaultSubId));
                    }
                }
            } else if (action.equals("android.telephony.action.MULTI_SIM_CONFIG_CHANGED")) {
                TelephonyRegistry.this.onMultiSimConfigChanged();
            }
        }
    };
    private final IBatteryStats mBatteryStats = BatteryStatsService.getService();

    static {
        sMtkSignalStrength = null;
        sMtkSignalStrength2 = null;
        if ("0".equals(SystemProperties.get("ro.vendor.mtk_telephony_add_on_policy", "0"))) {
            try {
                Class<?> clazz = Class.forName("mediatek.telephony.MtkSignalStrength");
                Constructor<?> constructor = clazz.getConstructor(Integer.TYPE);
                sMtkSignalStrength = constructor;
                constructor.setAccessible(true);
                Constructor<?> constructor2 = clazz.getConstructor(Integer.TYPE, SignalStrength.class);
                sMtkSignalStrength2 = constructor2;
                constructor2.setAccessible(true);
            } catch (Exception e) {
                loge("MtkSignalStrength Exception! Used AOSP instead!");
            }
        }
        INVALID_LCE_LIST = new ArrayList(Arrays.asList(new LinkCapacityEstimate(2, -1, -1)));
        HashSet hashSet = new HashSet();
        REQUIRE_PRECISE_PHONE_STATE_PERMISSION = hashSet;
        hashSet.add(13);
        hashSet.add(14);
        hashSet.add(12);
        hashSet.add(26);
        hashSet.add(27);
        hashSet.add(28);
        hashSet.add(31);
        hashSet.add(32);
        hashSet.add(33);
        hashSet.add(34);
        hashSet.add(37);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class Record {
        IBinder binder;
        IPhoneStateListener callback;
        int callerPid;
        int callerUid;
        String callingFeatureId;
        String callingPackage;
        ICarrierPrivilegesCallback carrierPrivilegesCallback;
        Context context;
        TelephonyRegistryDeathRecipient deathRecipient;
        Set<Integer> eventList;
        IOnSubscriptionsChangedListener onOpportunisticSubscriptionsChangedListenerCallback;
        IOnSubscriptionsChangedListener onSubscriptionsChangedListenerCallback;
        int phoneId;
        boolean renounceCoarseLocationAccess;
        boolean renounceFineLocationAccess;
        int subId;

        private Record() {
            this.subId = -1;
            this.phoneId = -1;
        }

        boolean matchTelephonyCallbackEvent(int event) {
            return this.callback != null && this.eventList.contains(Integer.valueOf(event));
        }

        boolean matchOnSubscriptionsChangedListener() {
            return this.onSubscriptionsChangedListenerCallback != null;
        }

        boolean matchOnOpportunisticSubscriptionsChangedListener() {
            return this.onOpportunisticSubscriptionsChangedListenerCallback != null;
        }

        boolean matchCarrierPrivilegesCallback() {
            return this.carrierPrivilegesCallback != null;
        }

        boolean canReadCallLog() {
            try {
                return TelephonyPermissions.checkReadCallLog(this.context, this.subId, this.callerPid, this.callerUid, this.callingPackage, this.callingFeatureId);
            } catch (SecurityException e) {
                return false;
            }
        }

        public String toString() {
            return "{callingPackage=" + TelephonyRegistry.pii(this.callingPackage) + " callerUid=" + this.callerUid + " binder=" + this.binder + " callback=" + this.callback + " onSubscriptionsChangedListenererCallback=" + this.onSubscriptionsChangedListenerCallback + " onOpportunisticSubscriptionsChangedListenererCallback=" + this.onOpportunisticSubscriptionsChangedListenerCallback + " carrierPrivilegesCallback=" + this.carrierPrivilegesCallback + " subId=" + this.subId + " phoneId=" + this.phoneId + " events=" + this.eventList + "}";
        }
    }

    /* loaded from: classes.dex */
    public static class ConfigurationProvider {
        public int getRegistrationLimit() {
            return ((Integer) Binder.withCleanCallingIdentity(new FunctionalUtils.ThrowingSupplier() { // from class: com.android.server.TelephonyRegistry$ConfigurationProvider$$ExternalSyntheticLambda2
                public final Object getOrThrow() {
                    Integer valueOf;
                    valueOf = Integer.valueOf(DeviceConfig.getInt("telephony", "phone_state_listener_per_pid_registration_limit", 50));
                    return valueOf;
                }
            })).intValue();
        }

        public boolean isRegistrationLimitEnabledInPlatformCompat(final int uid) {
            return ((Boolean) Binder.withCleanCallingIdentity(new FunctionalUtils.ThrowingSupplier() { // from class: com.android.server.TelephonyRegistry$ConfigurationProvider$$ExternalSyntheticLambda3
                public final Object getOrThrow() {
                    Boolean valueOf;
                    valueOf = Boolean.valueOf(CompatChanges.isChangeEnabled(150880553L, uid));
                    return valueOf;
                }
            })).booleanValue();
        }

        public boolean isCallStateReadPhoneStateEnforcedInPlatformCompat(final String packageName, final UserHandle userHandle) {
            return ((Boolean) Binder.withCleanCallingIdentity(new FunctionalUtils.ThrowingSupplier() { // from class: com.android.server.TelephonyRegistry$ConfigurationProvider$$ExternalSyntheticLambda5
                public final Object getOrThrow() {
                    Boolean valueOf;
                    valueOf = Boolean.valueOf(CompatChanges.isChangeEnabled(157233955L, packageName, userHandle));
                    return valueOf;
                }
            })).booleanValue();
        }

        public boolean isActiveDataSubIdReadPhoneStateEnforcedInPlatformCompat(final String packageName, final UserHandle userHandle) {
            return ((Boolean) Binder.withCleanCallingIdentity(new FunctionalUtils.ThrowingSupplier() { // from class: com.android.server.TelephonyRegistry$ConfigurationProvider$$ExternalSyntheticLambda0
                public final Object getOrThrow() {
                    Boolean valueOf;
                    valueOf = Boolean.valueOf(CompatChanges.isChangeEnabled((long) TelephonyRegistry.REQUIRE_READ_PHONE_STATE_PERMISSION_FOR_ACTIVE_DATA_SUB_ID, packageName, userHandle));
                    return valueOf;
                }
            })).booleanValue();
        }

        public boolean isCellInfoReadPhoneStateEnforcedInPlatformCompat(final String packageName, final UserHandle userHandle) {
            return ((Boolean) Binder.withCleanCallingIdentity(new FunctionalUtils.ThrowingSupplier() { // from class: com.android.server.TelephonyRegistry$ConfigurationProvider$$ExternalSyntheticLambda4
                public final Object getOrThrow() {
                    Boolean valueOf;
                    valueOf = Boolean.valueOf(CompatChanges.isChangeEnabled((long) TelephonyRegistry.REQUIRE_READ_PHONE_STATE_PERMISSION_FOR_CELL_INFO, packageName, userHandle));
                    return valueOf;
                }
            })).booleanValue();
        }

        public boolean isDisplayInfoReadPhoneStateEnforcedInPlatformCompat(final String packageName, final UserHandle userHandle) {
            return ((Boolean) Binder.withCleanCallingIdentity(new FunctionalUtils.ThrowingSupplier() { // from class: com.android.server.TelephonyRegistry$ConfigurationProvider$$ExternalSyntheticLambda6
                public final Object getOrThrow() {
                    Boolean valueOf;
                    valueOf = Boolean.valueOf(CompatChanges.isChangeEnabled((long) TelephonyRegistry.REQUIRE_READ_PHONE_STATE_PERMISSION_FOR_DISPLAY_INFO, packageName, userHandle));
                    return valueOf;
                }
            })).booleanValue();
        }

        public boolean isDisplayInfoNrAdvancedSupported(final String packageName, final UserHandle userHandle) {
            return ((Boolean) Binder.withCleanCallingIdentity(new FunctionalUtils.ThrowingSupplier() { // from class: com.android.server.TelephonyRegistry$ConfigurationProvider$$ExternalSyntheticLambda1
                public final Object getOrThrow() {
                    Boolean valueOf;
                    valueOf = Boolean.valueOf(CompatChanges.isChangeEnabled((long) TelephonyRegistry.DISPLAY_INFO_NR_ADVANCED_SUPPORTED, packageName, userHandle));
                    return valueOf;
                }
            })).booleanValue();
        }
    }

    private boolean isLocationPermissionRequired(Set<Integer> events) {
        return events.contains(5) || events.contains(11) || events.contains(31) || events.contains(32);
    }

    private boolean isPhoneStatePermissionRequired(Set<Integer> events, String callingPackage, UserHandle userHandle) {
        if (events.contains(4) || events.contains(3) || events.contains(25)) {
            return true;
        }
        if ((events.contains(36) || events.contains(6)) && this.mConfigurationProvider.isCallStateReadPhoneStateEnforcedInPlatformCompat(callingPackage, userHandle)) {
            return true;
        }
        if (events.contains(23) && this.mConfigurationProvider.isActiveDataSubIdReadPhoneStateEnforcedInPlatformCompat(callingPackage, userHandle)) {
            return true;
        }
        if (events.contains(11) && this.mConfigurationProvider.isCellInfoReadPhoneStateEnforcedInPlatformCompat(callingPackage, userHandle)) {
            return true;
        }
        return events.contains(21) && !this.mConfigurationProvider.isDisplayInfoReadPhoneStateEnforcedInPlatformCompat(callingPackage, userHandle);
    }

    private boolean isPrecisePhoneStatePermissionRequired(Set<Integer> events) {
        for (Integer requireEvent : REQUIRE_PRECISE_PHONE_STATE_PERMISSION) {
            if (events.contains(requireEvent)) {
                return true;
            }
        }
        return false;
    }

    private boolean isActiveEmergencySessionPermissionRequired(Set<Integer> events) {
        return events.contains(29) || events.contains(30);
    }

    private boolean isPrivilegedPhoneStatePermissionRequired(Set<Integer> events) {
        return events.contains(16) || events.contains(18) || events.contains(24) || events.contains(35);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class TelephonyRegistryDeathRecipient implements IBinder.DeathRecipient {
        private final IBinder binder;

        TelephonyRegistryDeathRecipient(IBinder binder) {
            this.binder = binder;
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            TelephonyRegistry.this.remove(this.binder);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public TelephonyManager getTelephonyManager() {
        return (TelephonyManager) this.mContext.getSystemService("phone");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onMultiSimConfigChanged() {
        synchronized (this.mRecords) {
            int oldNumPhones = this.mNumPhones;
            int activeModemCount = getTelephonyManager().getActiveModemCount();
            this.mNumPhones = activeModemCount;
            if (oldNumPhones == activeModemCount) {
                return;
            }
            int[] copyOf = Arrays.copyOf(this.mCallState, activeModemCount);
            this.mCallState = copyOf;
            this.mDataActivity = Arrays.copyOf(copyOf, this.mNumPhones);
            this.mDataConnectionState = Arrays.copyOf(this.mCallState, this.mNumPhones);
            this.mDataConnectionNetworkType = Arrays.copyOf(this.mCallState, this.mNumPhones);
            this.mCallIncomingNumber = (String[]) Arrays.copyOf(this.mCallIncomingNumber, this.mNumPhones);
            this.mServiceState = (ServiceState[]) Arrays.copyOf(this.mServiceState, this.mNumPhones);
            this.mVoiceActivationState = Arrays.copyOf(this.mVoiceActivationState, this.mNumPhones);
            this.mDataActivationState = Arrays.copyOf(this.mDataActivationState, this.mNumPhones);
            this.mUserMobileDataState = Arrays.copyOf(this.mUserMobileDataState, this.mNumPhones);
            SignalStrength[] signalStrengthArr = this.mSignalStrength;
            if (signalStrengthArr != null) {
                this.mSignalStrength = (SignalStrength[]) Arrays.copyOf(signalStrengthArr, this.mNumPhones);
            } else {
                this.mSignalStrength = new SignalStrength[this.mNumPhones];
            }
            this.mMessageWaiting = Arrays.copyOf(this.mMessageWaiting, this.mNumPhones);
            this.mCallForwarding = Arrays.copyOf(this.mCallForwarding, this.mNumPhones);
            this.mCellIdentity = (CellIdentity[]) Arrays.copyOf(this.mCellIdentity, this.mNumPhones);
            this.mSrvccState = Arrays.copyOf(this.mSrvccState, this.mNumPhones);
            this.mPreciseCallState = (PreciseCallState[]) Arrays.copyOf(this.mPreciseCallState, this.mNumPhones);
            this.mForegroundCallState = Arrays.copyOf(this.mForegroundCallState, this.mNumPhones);
            this.mBackgroundCallState = Arrays.copyOf(this.mBackgroundCallState, this.mNumPhones);
            this.mRingingCallState = Arrays.copyOf(this.mRingingCallState, this.mNumPhones);
            this.mCallDisconnectCause = Arrays.copyOf(this.mCallDisconnectCause, this.mNumPhones);
            this.mCallPreciseDisconnectCause = Arrays.copyOf(this.mCallPreciseDisconnectCause, this.mNumPhones);
            this.mCallQuality = (CallQuality[]) Arrays.copyOf(this.mCallQuality, this.mNumPhones);
            this.mCallNetworkType = Arrays.copyOf(this.mCallNetworkType, this.mNumPhones);
            this.mCallAttributes = (CallAttributes[]) Arrays.copyOf(this.mCallAttributes, this.mNumPhones);
            this.mOutgoingCallEmergencyNumber = (EmergencyNumber[]) Arrays.copyOf(this.mOutgoingCallEmergencyNumber, this.mNumPhones);
            this.mOutgoingSmsEmergencyNumber = (EmergencyNumber[]) Arrays.copyOf(this.mOutgoingSmsEmergencyNumber, this.mNumPhones);
            this.mTelephonyDisplayInfos = (TelephonyDisplayInfo[]) Arrays.copyOf(this.mTelephonyDisplayInfos, this.mNumPhones);
            this.mCarrierNetworkChangeState = Arrays.copyOf(this.mCarrierNetworkChangeState, this.mNumPhones);
            this.mIsDataEnabled = Arrays.copyOf(this.mIsDataEnabled, this.mNumPhones);
            this.mDataEnabledReason = Arrays.copyOf(this.mDataEnabledReason, this.mNumPhones);
            this.mAllowedNetworkTypeReason = Arrays.copyOf(this.mAllowedNetworkTypeReason, this.mNumPhones);
            this.mAllowedNetworkTypeValue = Arrays.copyOf(this.mAllowedNetworkTypeValue, this.mNumPhones);
            this.mRadioPowerState = Arrays.copyOf(this.mRadioPowerState, this.mNumPhones);
            int i = this.mNumPhones;
            if (i < oldNumPhones) {
                cutListToSize(this.mCellInfo, i);
                cutListToSize(this.mImsReasonInfo, this.mNumPhones);
                cutListToSize(this.mPreciseDataConnectionStates, this.mNumPhones);
                cutListToSize(this.mBarringInfo, this.mNumPhones);
                cutListToSize(this.mPhysicalChannelConfigs, this.mNumPhones);
                cutListToSize(this.mLinkCapacityEstimateLists, this.mNumPhones);
                cutListToSize(this.mCarrierPrivilegeStates, this.mNumPhones);
                cutListToSize(this.mCarrierServiceStates, this.mNumPhones);
                return;
            }
            for (int i2 = oldNumPhones; i2 < this.mNumPhones; i2++) {
                this.mCallState[i2] = 0;
                this.mDataActivity[i2] = 0;
                this.mDataConnectionState[i2] = -1;
                this.mVoiceActivationState[i2] = 0;
                this.mDataActivationState[i2] = 0;
                this.mCallIncomingNumber[i2] = "";
                this.mServiceState[i2] = new ServiceState();
                this.mSignalStrength[i2] = null;
                this.mUserMobileDataState[i2] = false;
                this.mMessageWaiting[i2] = false;
                this.mCallForwarding[i2] = false;
                this.mCellIdentity[i2] = null;
                this.mCellInfo.add(i2, null);
                this.mImsReasonInfo.add(i2, null);
                this.mSrvccState[i2] = -1;
                this.mCallDisconnectCause[i2] = -1;
                this.mCallPreciseDisconnectCause[i2] = -1;
                this.mCallQuality[i2] = createCallQuality();
                this.mCallAttributes[i2] = new CallAttributes(createPreciseCallState(), 0, createCallQuality());
                this.mCallNetworkType[i2] = 0;
                this.mPreciseCallState[i2] = createPreciseCallState();
                this.mRingingCallState[i2] = 0;
                this.mForegroundCallState[i2] = 0;
                this.mBackgroundCallState[i2] = 0;
                this.mPreciseDataConnectionStates.add(new ArrayMap());
                this.mBarringInfo.add(i2, new BarringInfo());
                this.mCarrierNetworkChangeState[i2] = false;
                this.mTelephonyDisplayInfos[i2] = null;
                this.mIsDataEnabled[i2] = false;
                this.mDataEnabledReason[i2] = 0;
                this.mPhysicalChannelConfigs.add(i2, new ArrayList());
                this.mAllowedNetworkTypeReason[i2] = -1;
                this.mAllowedNetworkTypeValue[i2] = -1;
                this.mLinkCapacityEstimateLists.add(i2, INVALID_LCE_LIST);
                this.mCarrierPrivilegeStates.add(i2, new Pair<>(Collections.emptyList(), new int[0]));
                this.mRadioPowerState[i2] = 2;
                this.mCarrierServiceStates.add(i2, new Pair<>(null, -1));
            }
        }
    }

    private void cutListToSize(List list, int size) {
        if (list == null) {
            return;
        }
        while (list.size() > size) {
            list.remove(list.size() - 1);
        }
    }

    public TelephonyRegistry(Context context, ConfigurationProvider configurationProvider) {
        this.mCellInfo = null;
        this.mImsReasonInfo = null;
        this.mBarringInfo = null;
        this.mCarrierNetworkChangeState = null;
        this.mContext = context;
        this.mConfigurationProvider = configurationProvider;
        int numPhones = getTelephonyManager().getActiveModemCount();
        this.mNumPhones = numPhones;
        this.mCallState = new int[numPhones];
        this.mDataActivity = new int[numPhones];
        this.mDataConnectionState = new int[numPhones];
        this.mDataConnectionNetworkType = new int[numPhones];
        this.mCallIncomingNumber = new String[numPhones];
        this.mServiceState = new ServiceState[numPhones];
        this.mVoiceActivationState = new int[numPhones];
        this.mDataActivationState = new int[numPhones];
        this.mUserMobileDataState = new boolean[numPhones];
        this.mSignalStrength = new SignalStrength[numPhones];
        this.mMessageWaiting = new boolean[numPhones];
        this.mCallForwarding = new boolean[numPhones];
        this.mCellIdentity = new CellIdentity[numPhones];
        this.mSrvccState = new int[numPhones];
        this.mPreciseCallState = new PreciseCallState[numPhones];
        this.mForegroundCallState = new int[numPhones];
        this.mBackgroundCallState = new int[numPhones];
        this.mRingingCallState = new int[numPhones];
        this.mCallDisconnectCause = new int[numPhones];
        this.mCallPreciseDisconnectCause = new int[numPhones];
        this.mCallQuality = new CallQuality[numPhones];
        this.mCallNetworkType = new int[numPhones];
        this.mCallAttributes = new CallAttributes[numPhones];
        this.mPreciseDataConnectionStates = new ArrayList();
        this.mCellInfo = new ArrayList<>();
        this.mImsReasonInfo = new ArrayList();
        this.mEmergencyNumberList = new HashMap();
        this.mOutgoingCallEmergencyNumber = new EmergencyNumber[numPhones];
        this.mOutgoingSmsEmergencyNumber = new EmergencyNumber[numPhones];
        this.mBarringInfo = new ArrayList();
        this.mCarrierNetworkChangeState = new boolean[numPhones];
        this.mTelephonyDisplayInfos = new TelephonyDisplayInfo[numPhones];
        this.mPhysicalChannelConfigs = new ArrayList();
        this.mAllowedNetworkTypeReason = new int[numPhones];
        this.mAllowedNetworkTypeValue = new long[numPhones];
        this.mIsDataEnabled = new boolean[numPhones];
        this.mDataEnabledReason = new int[numPhones];
        this.mLinkCapacityEstimateLists = new ArrayList();
        this.mRadioPowerState = new int[numPhones];
        this.mCarrierPrivilegeStates = new ArrayList();
        this.mCarrierServiceStates = new ArrayList();
        for (int i = 0; i < numPhones; i++) {
            this.mCallState[i] = 0;
            this.mDataActivity[i] = 0;
            this.mDataConnectionState[i] = -1;
            this.mVoiceActivationState[i] = 0;
            this.mDataActivationState[i] = 0;
            this.mCallIncomingNumber[i] = "";
            this.mServiceState[i] = new ServiceState();
            this.mSignalStrength[i] = null;
            this.mUserMobileDataState[i] = false;
            this.mMessageWaiting[i] = false;
            this.mCallForwarding[i] = false;
            this.mCellIdentity[i] = null;
            this.mCellInfo.add(i, null);
            this.mImsReasonInfo.add(i, null);
            this.mSrvccState[i] = -1;
            this.mCallDisconnectCause[i] = -1;
            this.mCallPreciseDisconnectCause[i] = -1;
            this.mCallQuality[i] = createCallQuality();
            this.mCallAttributes[i] = new CallAttributes(createPreciseCallState(), 0, createCallQuality());
            this.mCallNetworkType[i] = 0;
            this.mPreciseCallState[i] = createPreciseCallState();
            this.mRingingCallState[i] = 0;
            this.mForegroundCallState[i] = 0;
            this.mBackgroundCallState[i] = 0;
            this.mPreciseDataConnectionStates.add(new ArrayMap());
            this.mBarringInfo.add(i, new BarringInfo());
            this.mCarrierNetworkChangeState[i] = false;
            this.mTelephonyDisplayInfos[i] = null;
            this.mIsDataEnabled[i] = false;
            this.mDataEnabledReason[i] = 0;
            this.mPhysicalChannelConfigs.add(i, new ArrayList());
            this.mAllowedNetworkTypeReason[i] = -1;
            this.mAllowedNetworkTypeValue[i] = -1;
            this.mLinkCapacityEstimateLists.add(i, INVALID_LCE_LIST);
            this.mRadioPowerState[i] = 2;
            this.mCarrierPrivilegeStates.add(i, new Pair<>(Collections.emptyList(), new int[0]));
            this.mCarrierServiceStates.add(i, new Pair<>(null, -1));
        }
        this.mAppOps = (AppOpsManager) this.mContext.getSystemService(AppOpsManager.class);
    }

    public void systemRunning() {
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.USER_SWITCHED");
        filter.addAction("android.intent.action.USER_REMOVED");
        filter.addAction("android.telephony.action.DEFAULT_SUBSCRIPTION_CHANGED");
        filter.addAction("android.telephony.action.MULTI_SIM_CONFIG_CHANGED");
        log("systemRunning register for intents");
        this.mContext.registerReceiver(this.mBroadcastReceiver, filter);
    }

    public void addOnSubscriptionsChangedListener(String callingPackage, String callingFeatureId, IOnSubscriptionsChangedListener callback) {
        UserHandle.getCallingUserId();
        this.mAppOps.checkPackage(Binder.getCallingUid(), callingPackage);
        synchronized (this.mRecords) {
            IBinder b = callback.asBinder();
            Record r = add(b, Binder.getCallingUid(), Binder.getCallingPid(), false);
            if (r == null) {
                return;
            }
            r.context = this.mContext;
            r.onSubscriptionsChangedListenerCallback = callback;
            r.callingPackage = callingPackage;
            r.callingFeatureId = callingFeatureId;
            r.callerUid = Binder.getCallingUid();
            r.callerPid = Binder.getCallingPid();
            r.eventList = new ArraySet();
            if (this.mHasNotifySubscriptionInfoChangedOccurred) {
                try {
                    r.onSubscriptionsChangedListenerCallback.onSubscriptionsChanged();
                } catch (RemoteException e) {
                    remove(r.binder);
                }
            } else {
                log("listen oscl: mHasNotifySubscriptionInfoChangedOccurred==false no callback");
            }
        }
    }

    public void removeOnSubscriptionsChangedListener(String pkgForDebug, IOnSubscriptionsChangedListener callback) {
        remove(callback.asBinder());
    }

    public void addOnOpportunisticSubscriptionsChangedListener(String callingPackage, String callingFeatureId, IOnSubscriptionsChangedListener callback) {
        UserHandle.getCallingUserId();
        this.mAppOps.checkPackage(Binder.getCallingUid(), callingPackage);
        synchronized (this.mRecords) {
            IBinder b = callback.asBinder();
            Record r = add(b, Binder.getCallingUid(), Binder.getCallingPid(), false);
            if (r == null) {
                return;
            }
            r.context = this.mContext;
            r.onOpportunisticSubscriptionsChangedListenerCallback = callback;
            r.callingPackage = callingPackage;
            r.callingFeatureId = callingFeatureId;
            r.callerUid = Binder.getCallingUid();
            r.callerPid = Binder.getCallingPid();
            r.eventList = new ArraySet();
            if (this.mHasNotifyOpportunisticSubscriptionInfoChangedOccurred) {
                try {
                    r.onOpportunisticSubscriptionsChangedListenerCallback.onSubscriptionsChanged();
                } catch (RemoteException e) {
                    remove(r.binder);
                }
            } else {
                log("listen ooscl: hasNotifyOpptSubInfoChangedOccurred==false no callback");
            }
        }
    }

    public void notifySubscriptionInfoChanged() {
        synchronized (this.mRecords) {
            if (!this.mHasNotifySubscriptionInfoChangedOccurred) {
                log("notifySubscriptionInfoChanged: first invocation mRecords.size=" + this.mRecords.size());
            }
            this.mHasNotifySubscriptionInfoChangedOccurred = true;
            this.mRemoveList.clear();
            Iterator<Record> it = this.mRecords.iterator();
            while (it.hasNext()) {
                Record r = it.next();
                if (r.matchOnSubscriptionsChangedListener()) {
                    try {
                        r.onSubscriptionsChangedListenerCallback.onSubscriptionsChanged();
                    } catch (RemoteException e) {
                        this.mRemoveList.add(r.binder);
                    }
                }
            }
            handleRemoveListLocked();
        }
    }

    public void notifyOpportunisticSubscriptionInfoChanged() {
        synchronized (this.mRecords) {
            if (!this.mHasNotifyOpportunisticSubscriptionInfoChangedOccurred) {
                log("notifyOpptSubscriptionInfoChanged: first invocation mRecords.size=" + this.mRecords.size());
            }
            this.mHasNotifyOpportunisticSubscriptionInfoChangedOccurred = true;
            this.mRemoveList.clear();
            Iterator<Record> it = this.mRecords.iterator();
            while (it.hasNext()) {
                Record r = it.next();
                if (r.matchOnOpportunisticSubscriptionsChangedListener()) {
                    try {
                        r.onOpportunisticSubscriptionsChangedListenerCallback.onSubscriptionsChanged();
                    } catch (RemoteException e) {
                        this.mRemoveList.add(r.binder);
                    }
                }
            }
            handleRemoveListLocked();
        }
    }

    public void listenWithEventList(boolean renounceFineLocationAccess, boolean renounceCoarseLocationAccess, int subId, String callingPackage, String callingFeatureId, IPhoneStateListener callback, int[] events, boolean notifyNow) {
        Set<Integer> eventList = (Set) Arrays.stream(events).boxed().collect(Collectors.toSet());
        listen(renounceFineLocationAccess, renounceFineLocationAccess, callingPackage, callingFeatureId, callback, eventList, notifyNow, subId);
    }

    private void listen(boolean renounceFineLocationAccess, boolean renounceCoarseLocationAccess, String callingPackage, String callingFeatureId, IPhoneStateListener callback, Set<Integer> events, boolean notifyNow, int subId) {
        List<PhysicalChannelConfig> list;
        int callerUserId = UserHandle.getCallingUserId();
        this.mAppOps.checkPackage(Binder.getCallingUid(), callingPackage);
        String str = "listen: E pkg=" + pii(callingPackage) + " uid=" + Binder.getCallingUid() + " events=" + events + " notifyNow=" + notifyNow + " subId=" + subId + " myUserId=" + UserHandle.myUserId() + " callerUserId=" + callerUserId;
        this.mListenLog.log(str);
        if (events.isEmpty()) {
            events.clear();
            remove(callback.asBinder());
        } else if (checkListenerPermission(events, subId, callingPackage, callingFeatureId, "listen")) {
            synchronized (this.mRecords) {
                try {
                    IBinder b = callback.asBinder();
                    boolean doesLimitApply = (Binder.getCallingUid() == 1000 || Binder.getCallingUid() == 1001 || Binder.getCallingUid() == Process.myUid()) ? false : true;
                    Record r = add(b, Binder.getCallingUid(), Binder.getCallingPid(), doesLimitApply);
                    if (r == null) {
                        return;
                    }
                    r.context = this.mContext;
                    try {
                        r.callback = callback;
                        r.callingPackage = callingPackage;
                    } catch (Throwable th) {
                        th = th;
                    }
                    try {
                        r.callingFeatureId = callingFeatureId;
                        r.renounceCoarseLocationAccess = renounceCoarseLocationAccess;
                        r.renounceFineLocationAccess = renounceFineLocationAccess;
                        r.callerUid = Binder.getCallingUid();
                        r.callerPid = Binder.getCallingPid();
                        if (!SubscriptionManager.isValidSubscriptionId(subId)) {
                            r.subId = Integer.MAX_VALUE;
                        } else {
                            r.subId = subId;
                        }
                        r.phoneId = getPhoneIdFromSubId(r.subId);
                        r.eventList = events;
                        if (notifyNow && validatePhoneId(r.phoneId)) {
                            if (events.contains(1)) {
                                try {
                                    ServiceState rawSs = new ServiceState(this.mServiceState[r.phoneId]);
                                    if (!checkFineLocationAccess(r, 29)) {
                                        if (checkCoarseLocationAccess(r, 29)) {
                                            r.callback.onServiceStateChanged(rawSs.createLocationInfoSanitizedCopy(false));
                                        } else {
                                            r.callback.onServiceStateChanged(rawSs.createLocationInfoSanitizedCopy(true));
                                        }
                                    } else {
                                        r.callback.onServiceStateChanged(rawSs);
                                    }
                                } catch (RemoteException e) {
                                    remove(r.binder);
                                }
                            }
                            if (events.contains(2)) {
                                try {
                                    if (this.mSignalStrength[r.phoneId] != null) {
                                        int gsmSignalStrength = this.mSignalStrength[r.phoneId].getGsmSignalStrength();
                                        r.callback.onSignalStrengthChanged(gsmSignalStrength == 99 ? -1 : gsmSignalStrength);
                                    }
                                } catch (RemoteException e2) {
                                    remove(r.binder);
                                }
                            }
                            if (events.contains(3)) {
                                try {
                                    r.callback.onMessageWaitingIndicatorChanged(this.mMessageWaiting[r.phoneId]);
                                } catch (RemoteException e3) {
                                    remove(r.binder);
                                }
                            }
                            if (events.contains(4)) {
                                try {
                                    r.callback.onCallForwardingIndicatorChanged(this.mCallForwarding[r.phoneId]);
                                } catch (RemoteException e4) {
                                    remove(r.binder);
                                }
                            }
                            if (validateEventAndUserLocked(r, 5)) {
                                try {
                                    if (checkCoarseLocationAccess(r, 1) && checkFineLocationAccess(r, 29)) {
                                        r.callback.onCellLocationChanged(this.mCellIdentity[r.phoneId]);
                                    }
                                } catch (RemoteException e5) {
                                    remove(r.binder);
                                }
                            }
                            if (events.contains(36)) {
                                try {
                                    r.callback.onLegacyCallStateChanged(this.mCallState[r.phoneId], getCallIncomingNumber(r, r.phoneId));
                                } catch (RemoteException e6) {
                                    remove(r.binder);
                                }
                            }
                            if (events.contains(6)) {
                                try {
                                    r.callback.onCallStateChanged(this.mCallState[r.phoneId]);
                                } catch (RemoteException e7) {
                                    remove(r.binder);
                                }
                            }
                            if (events.contains(7)) {
                                try {
                                    r.callback.onDataConnectionStateChanged(this.mDataConnectionState[r.phoneId], this.mDataConnectionNetworkType[r.phoneId]);
                                } catch (RemoteException e8) {
                                    remove(r.binder);
                                }
                            }
                            if (events.contains(8)) {
                                try {
                                    r.callback.onDataActivity(this.mDataActivity[r.phoneId]);
                                } catch (RemoteException e9) {
                                    remove(r.binder);
                                }
                            }
                            if (events.contains(9)) {
                                try {
                                    if (this.mSignalStrength[r.phoneId] != null) {
                                        r.callback.onSignalStrengthsChanged(this.mSignalStrength[r.phoneId]);
                                    }
                                } catch (RemoteException e10) {
                                    remove(r.binder);
                                }
                            }
                            if (validateEventAndUserLocked(r, 11)) {
                                try {
                                    if (checkCoarseLocationAccess(r, 1) && checkFineLocationAccess(r, 29)) {
                                        r.callback.onCellInfoChanged(this.mCellInfo.get(r.phoneId));
                                    }
                                } catch (RemoteException e11) {
                                    remove(r.binder);
                                }
                            }
                            if (events.contains(12)) {
                                try {
                                    r.callback.onPreciseCallStateChanged(this.mPreciseCallState[r.phoneId]);
                                } catch (RemoteException e12) {
                                    remove(r.binder);
                                }
                            }
                            if (events.contains(26)) {
                                try {
                                    r.callback.onCallDisconnectCauseChanged(this.mCallDisconnectCause[r.phoneId], this.mCallPreciseDisconnectCause[r.phoneId]);
                                } catch (RemoteException e13) {
                                    remove(r.binder);
                                }
                            }
                            if (events.contains(28)) {
                                try {
                                    r.callback.onImsCallDisconnectCauseChanged(this.mImsReasonInfo.get(r.phoneId));
                                } catch (RemoteException e14) {
                                    remove(r.binder);
                                }
                            }
                            if (events.contains(13)) {
                                try {
                                    for (PreciseDataConnectionState pdcs : this.mPreciseDataConnectionStates.get(r.phoneId).values()) {
                                        r.callback.onPreciseDataConnectionStateChanged(pdcs);
                                    }
                                } catch (RemoteException e15) {
                                    remove(r.binder);
                                }
                            }
                            if (events.contains(17)) {
                                try {
                                    r.callback.onCarrierNetworkChange(this.mCarrierNetworkChangeState[r.phoneId]);
                                } catch (RemoteException e16) {
                                    remove(r.binder);
                                }
                            }
                            if (events.contains(18)) {
                                try {
                                    r.callback.onVoiceActivationStateChanged(this.mVoiceActivationState[r.phoneId]);
                                } catch (RemoteException e17) {
                                    remove(r.binder);
                                }
                            }
                            if (events.contains(19)) {
                                try {
                                    r.callback.onDataActivationStateChanged(this.mDataActivationState[r.phoneId]);
                                } catch (RemoteException e18) {
                                    remove(r.binder);
                                }
                            }
                            if (events.contains(20)) {
                                try {
                                    r.callback.onUserMobileDataStateChanged(this.mUserMobileDataState[r.phoneId]);
                                } catch (RemoteException e19) {
                                    remove(r.binder);
                                }
                            }
                            if (events.contains(21)) {
                                try {
                                    if (this.mTelephonyDisplayInfos[r.phoneId] != null) {
                                        r.callback.onDisplayInfoChanged(this.mTelephonyDisplayInfos[r.phoneId]);
                                    }
                                } catch (RemoteException e20) {
                                    remove(r.binder);
                                }
                            }
                            if (events.contains(25)) {
                                try {
                                    r.callback.onEmergencyNumberListChanged(this.mEmergencyNumberList);
                                } catch (RemoteException e21) {
                                    remove(r.binder);
                                }
                            }
                            if (events.contains(22)) {
                                try {
                                    r.callback.onPhoneCapabilityChanged(this.mPhoneCapability);
                                } catch (RemoteException e22) {
                                    remove(r.binder);
                                }
                            }
                            if (events.contains(23)) {
                                try {
                                    r.callback.onActiveDataSubIdChanged(this.mActiveDataSubId);
                                } catch (RemoteException e23) {
                                    remove(r.binder);
                                }
                            }
                            if (events.contains(24)) {
                                try {
                                    r.callback.onRadioPowerStateChanged(this.mRadioPowerState[r.phoneId]);
                                } catch (RemoteException e24) {
                                    remove(r.binder);
                                }
                            }
                            if (events.contains(16)) {
                                try {
                                    r.callback.onSrvccStateChanged(this.mSrvccState[r.phoneId]);
                                } catch (RemoteException e25) {
                                    remove(r.binder);
                                }
                            }
                            if (events.contains(27)) {
                                try {
                                    r.callback.onCallAttributesChanged(this.mCallAttributes[r.phoneId]);
                                } catch (RemoteException e26) {
                                    remove(r.binder);
                                }
                            }
                            if (events.contains(32)) {
                                BarringInfo barringInfo = this.mBarringInfo.get(r.phoneId);
                                BarringInfo biNoLocation = barringInfo != null ? barringInfo.createLocationInfoSanitizedCopy() : null;
                                try {
                                    r.callback.onBarringInfoChanged(checkFineLocationAccess(r, 1) ? barringInfo : biNoLocation);
                                } catch (RemoteException e27) {
                                    remove(r.binder);
                                }
                            }
                            if (events.contains(33)) {
                                try {
                                    IPhoneStateListener iPhoneStateListener = r.callback;
                                    if (shouldSanitizeLocationForPhysicalChannelConfig(r)) {
                                        list = getLocationSanitizedConfigs(this.mPhysicalChannelConfigs.get(r.phoneId));
                                    } else {
                                        list = this.mPhysicalChannelConfigs.get(r.phoneId);
                                    }
                                    iPhoneStateListener.onPhysicalChannelConfigChanged(list);
                                } catch (RemoteException e28) {
                                    remove(r.binder);
                                }
                            }
                            if (events.contains(34)) {
                                try {
                                    r.callback.onDataEnabledChanged(this.mIsDataEnabled[r.phoneId], this.mDataEnabledReason[r.phoneId]);
                                } catch (RemoteException e29) {
                                    remove(r.binder);
                                }
                            }
                            if (events.contains(37)) {
                                try {
                                    if (this.mLinkCapacityEstimateLists.get(r.phoneId) != null) {
                                        r.callback.onLinkCapacityEstimateChanged(this.mLinkCapacityEstimateLists.get(r.phoneId));
                                    }
                                } catch (RemoteException e30) {
                                    remove(r.binder);
                                }
                            }
                        }
                    } catch (Throwable th2) {
                        th = th2;
                        throw th;
                    }
                } catch (Throwable th3) {
                    th = th3;
                }
            }
        }
    }

    private String getCallIncomingNumber(Record record, int phoneId) {
        return record.canReadCallLog() ? this.mCallIncomingNumber[phoneId] : "";
    }

    private Record add(IBinder binder, int callingUid, int callingPid, boolean doesLimitApply) {
        synchronized (this.mRecords) {
            int N = this.mRecords.size();
            int numRecordsForPid = 0;
            for (int i = 0; i < N; i++) {
                Record r = this.mRecords.get(i);
                if (binder == r.binder) {
                    return r;
                }
                if (r.callerPid == callingPid) {
                    numRecordsForPid++;
                }
            }
            int registrationLimit = this.mConfigurationProvider.getRegistrationLimit();
            if (doesLimitApply && registrationLimit >= 1 && numRecordsForPid >= registrationLimit) {
                String errorMsg = "Pid " + callingPid + " has exceeded the number of permissible registered listeners. Ignoring request to add.";
                loge(errorMsg);
                if (this.mConfigurationProvider.isRegistrationLimitEnabledInPlatformCompat(callingUid)) {
                    throw new IllegalStateException(errorMsg);
                }
            } else if (doesLimitApply && numRecordsForPid >= 25) {
                Rlog.w(TAG, "Pid " + callingPid + " has exceeded half the number of permissible registered listeners. Now at " + numRecordsForPid);
            }
            Record r2 = new Record();
            r2.binder = binder;
            r2.deathRecipient = new TelephonyRegistryDeathRecipient(binder);
            try {
                binder.linkToDeath(r2.deathRecipient, 0);
                this.mRecords.add(r2);
                return r2;
            } catch (RemoteException e) {
                return null;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void remove(IBinder binder) {
        synchronized (this.mRecords) {
            int recordCount = this.mRecords.size();
            for (int i = 0; i < recordCount; i++) {
                Record r = this.mRecords.get(i);
                if (r.binder == binder) {
                    if (r.deathRecipient != null) {
                        try {
                            binder.unlinkToDeath(r.deathRecipient, 0);
                        } catch (NoSuchElementException e) {
                        }
                    }
                    this.mRecords.remove(i);
                    return;
                }
            }
        }
    }

    public void notifyCallStateForAllSubs(int state, String phoneNumber) {
        if (!checkNotifyPermission("notifyCallState()")) {
            return;
        }
        synchronized (this.mRecords) {
            Iterator<Record> it = this.mRecords.iterator();
            while (it.hasNext()) {
                Record r = it.next();
                if (r.matchTelephonyCallbackEvent(36) && r.subId == Integer.MAX_VALUE) {
                    try {
                        String phoneNumberOrEmpty = r.canReadCallLog() ? phoneNumber : "";
                        r.callback.onLegacyCallStateChanged(state, phoneNumberOrEmpty);
                    } catch (RemoteException e) {
                        this.mRemoveList.add(r.binder);
                    }
                }
                if (r.matchTelephonyCallbackEvent(6) && r.subId == Integer.MAX_VALUE) {
                    try {
                        r.callback.onCallStateChanged(state);
                    } catch (RemoteException e2) {
                        this.mRemoveList.add(r.binder);
                    }
                }
            }
            handleRemoveListLocked();
        }
        broadcastCallStateChanged(state, phoneNumber, -1, -1);
    }

    public void notifyCallState(int phoneId, int subId, int state, String incomingNumber) {
        if (!checkNotifyPermission("notifyCallState()")) {
            return;
        }
        synchronized (this.mRecords) {
            if (validatePhoneId(phoneId)) {
                this.mCallState[phoneId] = state;
                this.mCallIncomingNumber[phoneId] = incomingNumber;
                Iterator<Record> it = this.mRecords.iterator();
                while (it.hasNext()) {
                    Record r = it.next();
                    if (r.matchTelephonyCallbackEvent(36) && r.subId == subId && r.subId != Integer.MAX_VALUE) {
                        try {
                            String incomingNumberOrEmpty = getCallIncomingNumber(r, phoneId);
                            r.callback.onLegacyCallStateChanged(state, incomingNumberOrEmpty);
                        } catch (RemoteException e) {
                            this.mRemoveList.add(r.binder);
                        }
                    }
                    if (r.matchTelephonyCallbackEvent(6) && r.subId == subId && r.subId != Integer.MAX_VALUE) {
                        try {
                            r.callback.onCallStateChanged(state);
                        } catch (RemoteException e2) {
                            this.mRemoveList.add(r.binder);
                        }
                    }
                }
            }
            handleRemoveListLocked();
        }
        broadcastCallStateChanged(state, incomingNumber, phoneId, subId);
    }

    public void notifyServiceStateForPhoneId(int phoneId, int subId, ServiceState state) {
        ServiceState stateToSend;
        if (!checkNotifyPermission("notifyServiceState()")) {
            return;
        }
        synchronized (this.mRecords) {
            String str = "notifyServiceStateForSubscriber: subId=" + subId + " phoneId=" + phoneId + " state=" + state;
            this.mLocalLog.log(str);
            if (validatePhoneId(phoneId) && SubscriptionManager.isValidSubscriptionId(subId)) {
                this.mServiceState[phoneId] = state;
                Iterator<Record> it = this.mRecords.iterator();
                while (it.hasNext()) {
                    Record r = it.next();
                    if (r.matchTelephonyCallbackEvent(1) && idMatch(r, subId, phoneId)) {
                        try {
                            if (checkFineLocationAccess(r, 29)) {
                                stateToSend = new ServiceState(state);
                            } else if (checkCoarseLocationAccess(r, 29)) {
                                stateToSend = state.createLocationInfoSanitizedCopy(false);
                            } else {
                                stateToSend = state.createLocationInfoSanitizedCopy(true);
                            }
                            r.callback.onServiceStateChanged(stateToSend);
                        } catch (RemoteException e) {
                            this.mRemoveList.add(r.binder);
                        }
                    }
                }
            } else {
                log("notifyServiceStateForSubscriber: INVALID phoneId=" + phoneId + " or subId=" + subId);
            }
            handleRemoveListLocked();
        }
        broadcastServiceStateChanged(state, phoneId, subId);
    }

    public void notifySimActivationStateChangedForPhoneId(int phoneId, int subId, int activationType, int activationState) {
        if (!checkNotifyPermission("notifySimActivationState()")) {
            return;
        }
        synchronized (this.mRecords) {
            if (validatePhoneId(phoneId)) {
                switch (activationType) {
                    case 0:
                        this.mVoiceActivationState[phoneId] = activationState;
                        break;
                    case 1:
                        this.mDataActivationState[phoneId] = activationState;
                        break;
                    default:
                        return;
                }
                Iterator<Record> it = this.mRecords.iterator();
                while (it.hasNext()) {
                    Record r = it.next();
                    if (activationType == 0) {
                        try {
                            if (r.matchTelephonyCallbackEvent(18) && idMatch(r, subId, phoneId)) {
                                r.callback.onVoiceActivationStateChanged(activationState);
                            }
                        } catch (RemoteException e) {
                            this.mRemoveList.add(r.binder);
                        }
                    }
                    if (activationType == 1 && r.matchTelephonyCallbackEvent(19) && idMatch(r, subId, phoneId)) {
                        r.callback.onDataActivationStateChanged(activationState);
                    }
                }
            } else {
                log("notifySimActivationStateForPhoneId: INVALID phoneId=" + phoneId);
            }
            handleRemoveListLocked();
        }
    }

    public void notifySignalStrengthForPhoneId(int phoneId, int subId, SignalStrength signalStrength) {
        if (!checkNotifyPermission("notifySignalStrength()")) {
            return;
        }
        synchronized (this.mRecords) {
            if (validatePhoneId(phoneId)) {
                this.mSignalStrength[phoneId] = signalStrength;
                Iterator<Record> it = this.mRecords.iterator();
                while (it.hasNext()) {
                    Record r = it.next();
                    if (r.matchTelephonyCallbackEvent(9) && idMatch(r, subId, phoneId)) {
                        try {
                            r.callback.onSignalStrengthsChanged(makeSignalStrength(phoneId, signalStrength));
                        } catch (RemoteException e) {
                            this.mRemoveList.add(r.binder);
                        }
                    }
                    if (r.matchTelephonyCallbackEvent(2) && idMatch(r, subId, phoneId)) {
                        try {
                            int gsmSignalStrength = signalStrength.getGsmSignalStrength();
                            int ss = gsmSignalStrength == 99 ? -1 : gsmSignalStrength;
                            r.callback.onSignalStrengthChanged(ss);
                        } catch (RemoteException e2) {
                            this.mRemoveList.add(r.binder);
                        }
                    }
                }
            } else {
                log("notifySignalStrengthForPhoneId: invalid phoneId=" + phoneId);
            }
            handleRemoveListLocked();
        }
        broadcastSignalStrengthChanged(signalStrength, phoneId, subId);
    }

    public void notifyCarrierNetworkChange(boolean active) {
        int[] subIds = Arrays.stream(SubscriptionManager.from(this.mContext).getCompleteActiveSubscriptionIdList()).filter(new IntPredicate() { // from class: com.android.server.TelephonyRegistry$$ExternalSyntheticLambda4
            @Override // java.util.function.IntPredicate
            public final boolean test(int i) {
                return TelephonyRegistry.this.m453x52244101(i);
            }
        }).toArray();
        if (ArrayUtils.isEmpty(subIds)) {
            loge("notifyCarrierNetworkChange without carrier privilege");
            throw new SecurityException("notifyCarrierNetworkChange without carrier privilege");
        }
        for (int subId : subIds) {
            notifyCarrierNetworkChangeWithPermission(subId, active);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$notifyCarrierNetworkChange$0$com-android-server-TelephonyRegistry  reason: not valid java name */
    public /* synthetic */ boolean m453x52244101(int i) {
        return TelephonyPermissions.checkCarrierPrivilegeForSubId(this.mContext, i);
    }

    public void notifyCarrierNetworkChangeWithSubId(int subId, boolean active) {
        if (!TelephonyPermissions.checkCarrierPrivilegeForSubId(this.mContext, subId)) {
            throw new SecurityException("notifyCarrierNetworkChange without carrier privilege on subId " + subId);
        }
        notifyCarrierNetworkChangeWithPermission(subId, active);
    }

    private void notifyCarrierNetworkChangeWithPermission(int subId, boolean active) {
        synchronized (this.mRecords) {
            int phoneId = getPhoneIdFromSubId(subId);
            this.mCarrierNetworkChangeState[phoneId] = active;
            Iterator<Record> it = this.mRecords.iterator();
            while (it.hasNext()) {
                Record r = it.next();
                if (r.matchTelephonyCallbackEvent(17) && idMatch(r, subId, phoneId)) {
                    try {
                        r.callback.onCarrierNetworkChange(active);
                    } catch (RemoteException e) {
                        this.mRemoveList.add(r.binder);
                    }
                }
            }
            handleRemoveListLocked();
        }
    }

    public void notifyCellInfo(List<CellInfo> cellInfo) {
        notifyCellInfoForSubscriber(Integer.MAX_VALUE, cellInfo);
    }

    public void notifyCellInfoForSubscriber(int subId, List<CellInfo> cellInfo) {
        if (!checkNotifyPermission("notifyCellInfoForSubscriber()")) {
            return;
        }
        int phoneId = getPhoneIdFromSubId(subId);
        synchronized (this.mRecords) {
            if (validatePhoneId(phoneId)) {
                this.mCellInfo.set(phoneId, cellInfo);
                Iterator<Record> it = this.mRecords.iterator();
                while (it.hasNext()) {
                    Record r = it.next();
                    if (validateEventAndUserLocked(r, 11) && idMatch(r, subId, phoneId) && checkCoarseLocationAccess(r, 1) && checkFineLocationAccess(r, 29)) {
                        try {
                            r.callback.onCellInfoChanged(cellInfo);
                        } catch (RemoteException e) {
                            this.mRemoveList.add(r.binder);
                        }
                    }
                }
            }
            handleRemoveListLocked();
        }
    }

    public void notifyMessageWaitingChangedForPhoneId(int phoneId, int subId, boolean mwi) {
        if (!checkNotifyPermission("notifyMessageWaitingChanged()")) {
            return;
        }
        synchronized (this.mRecords) {
            if (validatePhoneId(phoneId)) {
                this.mMessageWaiting[phoneId] = mwi;
                Iterator<Record> it = this.mRecords.iterator();
                while (it.hasNext()) {
                    Record r = it.next();
                    if (r.matchTelephonyCallbackEvent(3) && idMatch(r, subId, phoneId)) {
                        try {
                            r.callback.onMessageWaitingIndicatorChanged(mwi);
                        } catch (RemoteException e) {
                            this.mRemoveList.add(r.binder);
                        }
                    }
                }
            }
            handleRemoveListLocked();
        }
    }

    public void notifyUserMobileDataStateChangedForPhoneId(int phoneId, int subId, boolean state) {
        if (!checkNotifyPermission("notifyUserMobileDataStateChanged()")) {
            return;
        }
        synchronized (this.mRecords) {
            if (validatePhoneId(phoneId)) {
                this.mUserMobileDataState[phoneId] = state;
                Iterator<Record> it = this.mRecords.iterator();
                while (it.hasNext()) {
                    Record r = it.next();
                    if (r.matchTelephonyCallbackEvent(20) && idMatch(r, subId, phoneId)) {
                        try {
                            r.callback.onUserMobileDataStateChanged(state);
                        } catch (RemoteException e) {
                            this.mRemoveList.add(r.binder);
                        }
                    }
                }
            }
            handleRemoveListLocked();
        }
    }

    public void notifyDisplayInfoChanged(int phoneId, int subId, TelephonyDisplayInfo telephonyDisplayInfo) {
        if (!checkNotifyPermission("notifyDisplayInfoChanged()")) {
            return;
        }
        String str = "notifyDisplayInfoChanged: PhoneId=" + phoneId + " subId=" + subId + " telephonyDisplayInfo=" + telephonyDisplayInfo;
        this.mLocalLog.log(str);
        synchronized (this.mRecords) {
            if (validatePhoneId(phoneId)) {
                this.mTelephonyDisplayInfos[phoneId] = telephonyDisplayInfo;
                Iterator<Record> it = this.mRecords.iterator();
                while (it.hasNext()) {
                    Record r = it.next();
                    if (r.matchTelephonyCallbackEvent(21) && idMatch(r, subId, phoneId)) {
                        try {
                            if (!this.mConfigurationProvider.isDisplayInfoNrAdvancedSupported(r.callingPackage, Binder.getCallingUserHandle())) {
                                r.callback.onDisplayInfoChanged(getBackwardCompatibleTelephonyDisplayInfo(telephonyDisplayInfo));
                            } else {
                                r.callback.onDisplayInfoChanged(telephonyDisplayInfo);
                            }
                        } catch (RemoteException e) {
                            this.mRemoveList.add(r.binder);
                        }
                    }
                }
            }
            handleRemoveListLocked();
        }
    }

    private TelephonyDisplayInfo getBackwardCompatibleTelephonyDisplayInfo(TelephonyDisplayInfo telephonyDisplayInfo) {
        int networkType = telephonyDisplayInfo.getNetworkType();
        int overrideNetworkType = telephonyDisplayInfo.getOverrideNetworkType();
        if (networkType == 20) {
            overrideNetworkType = 0;
        } else if (networkType == 13 && overrideNetworkType == 5) {
            overrideNetworkType = 4;
        }
        return new TelephonyDisplayInfo(networkType, overrideNetworkType);
    }

    public void notifyCallForwardingChanged(boolean cfi) {
        notifyCallForwardingChangedForSubscriber(Integer.MAX_VALUE, cfi);
    }

    public void notifyCallForwardingChangedForSubscriber(int subId, boolean cfi) {
        if (!checkNotifyPermission("notifyCallForwardingChanged()")) {
            return;
        }
        int phoneId = getPhoneIdFromSubId(subId);
        synchronized (this.mRecords) {
            if (validatePhoneId(phoneId)) {
                this.mCallForwarding[phoneId] = cfi;
                Iterator<Record> it = this.mRecords.iterator();
                while (it.hasNext()) {
                    Record r = it.next();
                    if (r.matchTelephonyCallbackEvent(4) && idMatch(r, subId, phoneId)) {
                        try {
                            r.callback.onCallForwardingIndicatorChanged(cfi);
                        } catch (RemoteException e) {
                            this.mRemoveList.add(r.binder);
                        }
                    }
                }
            }
            handleRemoveListLocked();
        }
    }

    public void notifyDataActivity(int state) {
        notifyDataActivityForSubscriber(Integer.MAX_VALUE, state);
    }

    public void notifyDataActivityForSubscriber(int subId, int state) {
        if (!checkNotifyPermission("notifyDataActivity()")) {
            return;
        }
        int phoneId = getPhoneIdFromSubId(subId);
        synchronized (this.mRecords) {
            if (validatePhoneId(phoneId)) {
                this.mDataActivity[phoneId] = state;
                Iterator<Record> it = this.mRecords.iterator();
                while (it.hasNext()) {
                    Record r = it.next();
                    if (r.matchTelephonyCallbackEvent(8) && idMatch(r, subId, phoneId)) {
                        try {
                            r.callback.onDataActivity(state);
                        } catch (RemoteException e) {
                            this.mRemoveList.add(r.binder);
                        }
                    }
                }
            }
            handleRemoveListLocked();
        }
    }

    public void notifyDataConnectionForSubscriber(int phoneId, int subId, PreciseDataConnectionState preciseState) {
        int state;
        if (!checkNotifyPermission("notifyDataConnection()")) {
            return;
        }
        synchronized (this.mRecords) {
            if (validatePhoneId(phoneId) && preciseState.getApnSetting() != null) {
                Pair<Integer, ApnSetting> key = Pair.create(Integer.valueOf(preciseState.getTransportType()), preciseState.getApnSetting());
                PreciseDataConnectionState oldState = this.mPreciseDataConnectionStates.get(phoneId).remove(key);
                if (!Objects.equals(oldState, preciseState)) {
                    Iterator<Record> it = this.mRecords.iterator();
                    while (it.hasNext()) {
                        Record r = it.next();
                        if (r.matchTelephonyCallbackEvent(13) && idMatch(r, subId, phoneId)) {
                            try {
                                r.callback.onPreciseDataConnectionStateChanged(preciseState);
                            } catch (RemoteException e) {
                                this.mRemoveList.add(r.binder);
                            }
                        }
                    }
                    handleRemoveListLocked();
                    broadcastDataConnectionStateChanged(phoneId, subId, preciseState);
                    String str = "notifyDataConnectionForSubscriber: phoneId=" + phoneId + " subId=" + subId + " " + preciseState;
                    log(str);
                    this.mLocalLog.log(str);
                }
                if (preciseState.getState() != 0) {
                    this.mPreciseDataConnectionStates.get(phoneId).put(key, preciseState);
                }
                Map<Integer, PreciseDataConnectionState> internetConnections = new ArrayMap<>();
                int i = 0;
                if (preciseState.getState() == 0 && preciseState.getApnSetting().getApnTypes().contains(17)) {
                    internetConnections.put(0, preciseState);
                }
                for (Map.Entry<Pair<Integer, ApnSetting>, PreciseDataConnectionState> entry : this.mPreciseDataConnectionStates.get(phoneId).entrySet()) {
                    if (((Integer) entry.getKey().first).intValue() == 1 && ((ApnSetting) entry.getKey().second).getApnTypes().contains(17)) {
                        internetConnections.put(Integer.valueOf(entry.getValue().getState()), entry.getValue());
                    }
                }
                int[] statesInPriority = {2, 3, 1, 4, 0};
                int networkType = 0;
                int length = statesInPriority.length;
                while (true) {
                    if (i >= length) {
                        state = 0;
                        break;
                    }
                    int s = statesInPriority[i];
                    if (!internetConnections.containsKey(Integer.valueOf(s))) {
                        i++;
                    } else {
                        networkType = internetConnections.get(Integer.valueOf(s)).getNetworkType();
                        state = s;
                        break;
                    }
                }
                if (this.mDataConnectionState[phoneId] != state || this.mDataConnectionNetworkType[phoneId] != networkType) {
                    String str2 = "onDataConnectionStateChanged(" + TelephonyUtils.dataStateToString(state) + ", " + TelephonyManager.getNetworkTypeName(networkType) + ") subId=" + subId + ", phoneId=" + phoneId;
                    log(str2);
                    this.mLocalLog.log(str2);
                    Iterator<Record> it2 = this.mRecords.iterator();
                    while (it2.hasNext()) {
                        Record r2 = it2.next();
                        if (r2.matchTelephonyCallbackEvent(7) && idMatch(r2, subId, phoneId)) {
                            try {
                                r2.callback.onDataConnectionStateChanged(state, networkType);
                            } catch (RemoteException e2) {
                                this.mRemoveList.add(r2.binder);
                            }
                        }
                    }
                    this.mDataConnectionState[phoneId] = state;
                    this.mDataConnectionNetworkType[phoneId] = networkType;
                    handleRemoveListLocked();
                }
            }
        }
    }

    public void notifyCellLocationForSubscriber(int subId, CellIdentity cellIdentity) {
        notifyCellLocationForSubscriber(subId, cellIdentity, false);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifyCellLocationForSubscriber(int subId, CellIdentity cellIdentity, boolean hasUserSwitched) {
        log("notifyCellLocationForSubscriber: subId=" + subId + " cellIdentity=" + Rlog.pii(false, cellIdentity));
        if (!checkNotifyPermission("notifyCellLocation()")) {
            return;
        }
        int phoneId = getPhoneIdFromSubId(subId);
        synchronized (this.mRecords) {
            if (validatePhoneId(phoneId) && (hasUserSwitched || !Objects.equals(cellIdentity, this.mCellIdentity[phoneId]))) {
                this.mCellIdentity[phoneId] = cellIdentity;
                Iterator<Record> it = this.mRecords.iterator();
                while (it.hasNext()) {
                    Record r = it.next();
                    if (validateEventAndUserLocked(r, 5) && idMatch(r, subId, phoneId) && checkCoarseLocationAccess(r, 1) && checkFineLocationAccess(r, 29)) {
                        try {
                            r.callback.onCellLocationChanged(cellIdentity);
                        } catch (RemoteException e) {
                            this.mRemoveList.add(r.binder);
                        }
                    }
                }
            }
            handleRemoveListLocked();
        }
    }

    public void notifyPreciseCallState(int phoneId, int subId, int ringingCallState, int foregroundCallState, int backgroundCallState) {
        if (!checkNotifyPermission("notifyPreciseCallState()")) {
            return;
        }
        synchronized (this.mRecords) {
            if (validatePhoneId(phoneId)) {
                this.mRingingCallState[phoneId] = ringingCallState;
                this.mForegroundCallState[phoneId] = foregroundCallState;
                this.mBackgroundCallState[phoneId] = backgroundCallState;
                this.mPreciseCallState[phoneId] = new PreciseCallState(ringingCallState, foregroundCallState, backgroundCallState, -1, -1);
                boolean notifyCallAttributes = true;
                if (this.mCallQuality == null) {
                    log("notifyPreciseCallState: mCallQuality is null, skipping call attributes");
                    notifyCallAttributes = false;
                } else {
                    if (this.mPreciseCallState[phoneId].getForegroundCallState() != 1) {
                        this.mCallNetworkType[phoneId] = 0;
                        this.mCallQuality[phoneId] = createCallQuality();
                    }
                    this.mCallAttributes[phoneId] = new CallAttributes(this.mPreciseCallState[phoneId], this.mCallNetworkType[phoneId], this.mCallQuality[phoneId]);
                }
                Iterator<Record> it = this.mRecords.iterator();
                while (it.hasNext()) {
                    Record r = it.next();
                    if (r.matchTelephonyCallbackEvent(12) && idMatch(r, subId, phoneId)) {
                        try {
                            r.callback.onPreciseCallStateChanged(this.mPreciseCallState[phoneId]);
                        } catch (RemoteException e) {
                            this.mRemoveList.add(r.binder);
                        }
                    }
                    if (notifyCallAttributes && r.matchTelephonyCallbackEvent(27) && idMatch(r, subId, phoneId)) {
                        try {
                            r.callback.onCallAttributesChanged(this.mCallAttributes[phoneId]);
                        } catch (RemoteException e2) {
                            this.mRemoveList.add(r.binder);
                        }
                    }
                }
            }
            handleRemoveListLocked();
        }
    }

    public void notifyDisconnectCause(int phoneId, int subId, int disconnectCause, int preciseDisconnectCause) {
        if (!checkNotifyPermission("notifyDisconnectCause()")) {
            return;
        }
        synchronized (this.mRecords) {
            if (validatePhoneId(phoneId)) {
                this.mCallDisconnectCause[phoneId] = disconnectCause;
                this.mCallPreciseDisconnectCause[phoneId] = preciseDisconnectCause;
                Iterator<Record> it = this.mRecords.iterator();
                while (it.hasNext()) {
                    Record r = it.next();
                    if (r.matchTelephonyCallbackEvent(26) && idMatch(r, subId, phoneId)) {
                        try {
                            r.callback.onCallDisconnectCauseChanged(this.mCallDisconnectCause[phoneId], this.mCallPreciseDisconnectCause[phoneId]);
                        } catch (RemoteException e) {
                            this.mRemoveList.add(r.binder);
                        }
                    }
                }
            }
            handleRemoveListLocked();
        }
    }

    public void notifyImsDisconnectCause(int subId, ImsReasonInfo imsReasonInfo) {
        if (!checkNotifyPermission("notifyImsCallDisconnectCause()")) {
            return;
        }
        int phoneId = getPhoneIdFromSubId(subId);
        synchronized (this.mRecords) {
            if (validatePhoneId(phoneId)) {
                this.mImsReasonInfo.set(phoneId, imsReasonInfo);
                Iterator<Record> it = this.mRecords.iterator();
                while (it.hasNext()) {
                    Record r = it.next();
                    if (r.matchTelephonyCallbackEvent(28) && idMatch(r, subId, phoneId)) {
                        try {
                            r.callback.onImsCallDisconnectCauseChanged(this.mImsReasonInfo.get(phoneId));
                        } catch (RemoteException e) {
                            this.mRemoveList.add(r.binder);
                        }
                    }
                }
            }
            handleRemoveListLocked();
        }
    }

    public void notifySrvccStateChanged(int subId, int state) {
        if (!checkNotifyPermission("notifySrvccStateChanged()")) {
            return;
        }
        int phoneId = getPhoneIdFromSubId(subId);
        synchronized (this.mRecords) {
            if (validatePhoneId(phoneId)) {
                this.mSrvccState[phoneId] = state;
                Iterator<Record> it = this.mRecords.iterator();
                while (it.hasNext()) {
                    Record r = it.next();
                    if (r.matchTelephonyCallbackEvent(16) && idMatch(r, subId, phoneId)) {
                        try {
                            r.callback.onSrvccStateChanged(state);
                        } catch (RemoteException e) {
                            this.mRemoveList.add(r.binder);
                        }
                    }
                }
            }
            handleRemoveListLocked();
        }
    }

    public void notifyOemHookRawEventForSubscriber(int phoneId, int subId, byte[] rawData) {
        if (!checkNotifyPermission("notifyOemHookRawEventForSubscriber")) {
            return;
        }
        synchronized (this.mRecords) {
            if (validatePhoneId(phoneId)) {
                Iterator<Record> it = this.mRecords.iterator();
                while (it.hasNext()) {
                    Record r = it.next();
                    if (r.matchTelephonyCallbackEvent(15) && idMatch(r, subId, phoneId)) {
                        try {
                            r.callback.onOemHookRawEvent(rawData);
                        } catch (RemoteException e) {
                            this.mRemoveList.add(r.binder);
                        }
                    }
                }
            }
            handleRemoveListLocked();
        }
    }

    public void notifyPhoneCapabilityChanged(PhoneCapability capability) {
        if (!checkNotifyPermission("notifyPhoneCapabilityChanged()")) {
            return;
        }
        synchronized (this.mRecords) {
            this.mPhoneCapability = capability;
            Iterator<Record> it = this.mRecords.iterator();
            while (it.hasNext()) {
                Record r = it.next();
                if (r.matchTelephonyCallbackEvent(22)) {
                    try {
                        r.callback.onPhoneCapabilityChanged(capability);
                    } catch (RemoteException e) {
                        this.mRemoveList.add(r.binder);
                    }
                }
            }
            handleRemoveListLocked();
        }
    }

    public void notifyActiveDataSubIdChanged(int activeDataSubId) {
        if (!checkNotifyPermission("notifyActiveDataSubIdChanged()")) {
            return;
        }
        log("notifyActiveDataSubIdChanged: activeDataSubId=" + activeDataSubId);
        this.mLocalLog.log("notifyActiveDataSubIdChanged: activeDataSubId=" + activeDataSubId);
        this.mActiveDataSubId = activeDataSubId;
        synchronized (this.mRecords) {
            Iterator<Record> it = this.mRecords.iterator();
            while (it.hasNext()) {
                Record r = it.next();
                if (r.matchTelephonyCallbackEvent(23)) {
                    try {
                        r.callback.onActiveDataSubIdChanged(activeDataSubId);
                    } catch (RemoteException e) {
                        this.mRemoveList.add(r.binder);
                    }
                }
            }
            handleRemoveListLocked();
        }
    }

    public void notifyRadioPowerStateChanged(int phoneId, int subId, int state) {
        if (!checkNotifyPermission("notifyRadioPowerStateChanged()")) {
            return;
        }
        synchronized (this.mRecords) {
            if (validatePhoneId(phoneId)) {
                this.mRadioPowerState[phoneId] = state;
                Iterator<Record> it = this.mRecords.iterator();
                while (it.hasNext()) {
                    Record r = it.next();
                    if (r.matchTelephonyCallbackEvent(24) && idMatch(r, subId, phoneId)) {
                        try {
                            r.callback.onRadioPowerStateChanged(state);
                        } catch (RemoteException e) {
                            this.mRemoveList.add(r.binder);
                        }
                    }
                }
            }
            handleRemoveListLocked();
        }
    }

    public void notifyEmergencyNumberList(int phoneId, int subId) {
        if (!checkNotifyPermission("notifyEmergencyNumberList()")) {
            return;
        }
        synchronized (this.mRecords) {
            if (validatePhoneId(phoneId)) {
                TelephonyManager tm = (TelephonyManager) this.mContext.getSystemService("phone");
                this.mEmergencyNumberList = tm.getEmergencyNumberList();
                Iterator<Record> it = this.mRecords.iterator();
                while (it.hasNext()) {
                    Record r = it.next();
                    if (r.matchTelephonyCallbackEvent(25) && idMatch(r, subId, phoneId)) {
                        try {
                            r.callback.onEmergencyNumberListChanged(this.mEmergencyNumberList);
                        } catch (RemoteException e) {
                            this.mRemoveList.add(r.binder);
                        }
                    }
                }
            }
            handleRemoveListLocked();
        }
    }

    public void notifyOutgoingEmergencyCall(int phoneId, int subId, EmergencyNumber emergencyNumber) {
        if (!checkNotifyPermission("notifyOutgoingEmergencyCall()")) {
            return;
        }
        synchronized (this.mRecords) {
            if (validatePhoneId(phoneId)) {
                this.mOutgoingCallEmergencyNumber[phoneId] = emergencyNumber;
            }
            Iterator<Record> it = this.mRecords.iterator();
            while (it.hasNext()) {
                Record r = it.next();
                if (r.matchTelephonyCallbackEvent(29)) {
                    try {
                        r.callback.onOutgoingEmergencyCall(emergencyNumber, subId);
                    } catch (RemoteException e) {
                        this.mRemoveList.add(r.binder);
                    }
                }
            }
        }
        handleRemoveListLocked();
    }

    public void notifyOutgoingEmergencySms(int phoneId, int subId, EmergencyNumber emergencyNumber) {
        if (!checkNotifyPermission("notifyOutgoingEmergencySms()")) {
            return;
        }
        synchronized (this.mRecords) {
            if (validatePhoneId(phoneId)) {
                this.mOutgoingSmsEmergencyNumber[phoneId] = emergencyNumber;
                Iterator<Record> it = this.mRecords.iterator();
                while (it.hasNext()) {
                    Record r = it.next();
                    if (r.matchTelephonyCallbackEvent(30)) {
                        try {
                            r.callback.onOutgoingEmergencySms(emergencyNumber, subId);
                        } catch (RemoteException e) {
                            this.mRemoveList.add(r.binder);
                        }
                    }
                }
            }
            handleRemoveListLocked();
        }
    }

    public void notifyCallQualityChanged(CallQuality callQuality, int phoneId, int subId, int callNetworkType) {
        if (!checkNotifyPermission("notifyCallQualityChanged()")) {
            return;
        }
        synchronized (this.mRecords) {
            if (validatePhoneId(phoneId)) {
                this.mCallQuality[phoneId] = callQuality;
                this.mCallNetworkType[phoneId] = callNetworkType;
                this.mCallAttributes[phoneId] = new CallAttributes(this.mPreciseCallState[phoneId], callNetworkType, callQuality);
                Iterator<Record> it = this.mRecords.iterator();
                while (it.hasNext()) {
                    Record r = it.next();
                    if (r.matchTelephonyCallbackEvent(27) && idMatch(r, subId, phoneId)) {
                        try {
                            r.callback.onCallAttributesChanged(this.mCallAttributes[phoneId]);
                        } catch (RemoteException e) {
                            this.mRemoveList.add(r.binder);
                        }
                    }
                }
            }
            handleRemoveListLocked();
        }
    }

    public void notifyRegistrationFailed(int phoneId, int subId, CellIdentity cellIdentity, String chosenPlmn, int domain, int causeCode, int additionalCauseCode) {
        if (!checkNotifyPermission("notifyRegistrationFailed()")) {
            return;
        }
        CellIdentity noLocationCi = cellIdentity.sanitizeLocationInfo();
        synchronized (this.mRecords) {
            try {
                try {
                    if (validatePhoneId(phoneId)) {
                        Iterator<Record> it = this.mRecords.iterator();
                        while (it.hasNext()) {
                            Record r = it.next();
                            if (r.matchTelephonyCallbackEvent(31) && idMatch(r, subId, phoneId)) {
                                try {
                                    r.callback.onRegistrationFailed(checkFineLocationAccess(r, 1) ? cellIdentity : noLocationCi, chosenPlmn, domain, causeCode, additionalCauseCode);
                                } catch (RemoteException e) {
                                    this.mRemoveList.add(r.binder);
                                }
                            }
                        }
                    }
                    handleRemoveListLocked();
                } catch (Throwable th) {
                    th = th;
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
                throw th;
            }
        }
    }

    public void notifyBarringInfoChanged(int phoneId, int subId, BarringInfo barringInfo) {
        if (!checkNotifyPermission("notifyBarringInfo()")) {
            return;
        }
        if (barringInfo == null) {
            log("Received null BarringInfo for subId=" + subId + ", phoneId=" + phoneId);
            this.mBarringInfo.set(phoneId, new BarringInfo());
            return;
        }
        synchronized (this.mRecords) {
            if (validatePhoneId(phoneId)) {
                this.mBarringInfo.set(phoneId, barringInfo);
                BarringInfo biNoLocation = barringInfo.createLocationInfoSanitizedCopy();
                Iterator<Record> it = this.mRecords.iterator();
                while (it.hasNext()) {
                    Record r = it.next();
                    if (r.matchTelephonyCallbackEvent(32) && idMatch(r, subId, phoneId)) {
                        try {
                            r.callback.onBarringInfoChanged(checkFineLocationAccess(r, 1) ? barringInfo : biNoLocation);
                        } catch (RemoteException e) {
                            this.mRemoveList.add(r.binder);
                        }
                    }
                }
            }
            handleRemoveListLocked();
        }
    }

    public void notifyPhysicalChannelConfigForSubscriber(int phoneId, int subId, List<PhysicalChannelConfig> configs) {
        if (!checkNotifyPermission("notifyPhysicalChannelConfig()")) {
            return;
        }
        List<PhysicalChannelConfig> sanitizedConfigs = getLocationSanitizedConfigs(configs);
        synchronized (this.mRecords) {
            if (validatePhoneId(phoneId)) {
                this.mPhysicalChannelConfigs.set(phoneId, configs);
                Iterator<Record> it = this.mRecords.iterator();
                while (it.hasNext()) {
                    Record r = it.next();
                    if (r.matchTelephonyCallbackEvent(33) && idMatch(r, subId, phoneId)) {
                        try {
                            r.callback.onPhysicalChannelConfigChanged(shouldSanitizeLocationForPhysicalChannelConfig(r) ? sanitizedConfigs : configs);
                        } catch (RemoteException e) {
                            this.mRemoveList.add(r.binder);
                        }
                    }
                }
            }
            handleRemoveListLocked();
        }
    }

    private static boolean shouldSanitizeLocationForPhysicalChannelConfig(Record record) {
        return (record.callerUid == 1001 || record.callerUid == 1000) ? false : true;
    }

    private static List<PhysicalChannelConfig> getLocationSanitizedConfigs(List<PhysicalChannelConfig> configs) {
        List<PhysicalChannelConfig> sanitizedConfigs = new ArrayList<>(configs.size());
        for (PhysicalChannelConfig config : configs) {
            sanitizedConfigs.add(config.createLocationInfoSanitizedCopy());
        }
        return sanitizedConfigs;
    }

    public void notifyDataEnabled(int phoneId, int subId, boolean enabled, int reason) {
        if (!checkNotifyPermission("notifyDataEnabled()")) {
            return;
        }
        synchronized (this.mRecords) {
            if (validatePhoneId(phoneId)) {
                this.mIsDataEnabled[phoneId] = enabled;
                this.mDataEnabledReason[phoneId] = reason;
                Iterator<Record> it = this.mRecords.iterator();
                while (it.hasNext()) {
                    Record r = it.next();
                    if (r.matchTelephonyCallbackEvent(34) && idMatch(r, subId, phoneId)) {
                        try {
                            r.callback.onDataEnabledChanged(enabled, reason);
                        } catch (RemoteException e) {
                            this.mRemoveList.add(r.binder);
                        }
                    }
                }
            }
            handleRemoveListLocked();
        }
    }

    public void notifyAllowedNetworkTypesChanged(int phoneId, int subId, int reason, long allowedNetworkType) {
        if (!checkNotifyPermission("notifyAllowedNetworkTypesChanged()")) {
            return;
        }
        synchronized (this.mRecords) {
            if (validatePhoneId(phoneId)) {
                this.mAllowedNetworkTypeReason[phoneId] = reason;
                this.mAllowedNetworkTypeValue[phoneId] = allowedNetworkType;
                Iterator<Record> it = this.mRecords.iterator();
                while (it.hasNext()) {
                    Record r = it.next();
                    if (r.matchTelephonyCallbackEvent(35) && idMatch(r, subId, phoneId)) {
                        try {
                            r.callback.onAllowedNetworkTypesChanged(reason, allowedNetworkType);
                        } catch (RemoteException e) {
                            this.mRemoveList.add(r.binder);
                        }
                    }
                }
            }
            handleRemoveListLocked();
        }
    }

    public void notifyLinkCapacityEstimateChanged(int phoneId, int subId, List<LinkCapacityEstimate> linkCapacityEstimateList) {
        if (!checkNotifyPermission("notifyLinkCapacityEstimateChanged()")) {
            return;
        }
        synchronized (this.mRecords) {
            if (validatePhoneId(phoneId)) {
                this.mLinkCapacityEstimateLists.set(phoneId, linkCapacityEstimateList);
                Iterator<Record> it = this.mRecords.iterator();
                while (it.hasNext()) {
                    Record r = it.next();
                    if (r.matchTelephonyCallbackEvent(37) && idMatch(r, subId, phoneId)) {
                        try {
                            r.callback.onLinkCapacityEstimateChanged(linkCapacityEstimateList);
                        } catch (RemoteException e) {
                            this.mRemoveList.add(r.binder);
                        }
                    }
                }
            }
            handleRemoveListLocked();
        }
    }

    public void addCarrierPrivilegesCallback(int phoneId, ICarrierPrivilegesCallback callback, String callingPackage, String callingFeatureId) {
        UserHandle.getCallingUserId();
        this.mAppOps.checkPackage(Binder.getCallingUid(), callingPackage);
        this.mContext.enforceCallingOrSelfPermission("android.permission.READ_PRIVILEGED_PHONE_STATE", "addCarrierPrivilegesCallback");
        onMultiSimConfigChanged();
        synchronized (this.mRecords) {
            if (!validatePhoneId(phoneId)) {
                throw new IllegalArgumentException("Invalid slot index: " + phoneId);
            }
            Record r = add(callback.asBinder(), Binder.getCallingUid(), Binder.getCallingPid(), false);
            if (r == null) {
                return;
            }
            r.context = this.mContext;
            r.carrierPrivilegesCallback = callback;
            r.callingPackage = callingPackage;
            r.callingFeatureId = callingFeatureId;
            r.callerUid = Binder.getCallingUid();
            r.callerPid = Binder.getCallingPid();
            r.phoneId = phoneId;
            r.eventList = new ArraySet();
            Pair<List<String>, int[]> state = this.mCarrierPrivilegeStates.get(phoneId);
            Pair<String, Integer> carrierServiceState = this.mCarrierServiceStates.get(phoneId);
            try {
                if (r.matchCarrierPrivilegesCallback()) {
                    r.carrierPrivilegesCallback.onCarrierPrivilegesChanged(Collections.unmodifiableList((List) state.first), Arrays.copyOf((int[]) state.second, ((int[]) state.second).length));
                    r.carrierPrivilegesCallback.onCarrierServiceChanged((String) carrierServiceState.first, ((Integer) carrierServiceState.second).intValue());
                }
            } catch (RemoteException e) {
                remove(r.binder);
            }
        }
    }

    public void removeCarrierPrivilegesCallback(ICarrierPrivilegesCallback callback, String callingPackage) {
        this.mAppOps.checkPackage(Binder.getCallingUid(), callingPackage);
        this.mContext.enforceCallingOrSelfPermission("android.permission.READ_PRIVILEGED_PHONE_STATE", "removeCarrierPrivilegesCallback");
        remove(callback.asBinder());
    }

    public void notifyCarrierPrivilegesChanged(int phoneId, List<String> privilegedPackageNames, int[] privilegedUids) {
        if (!checkNotifyPermission("notifyCarrierPrivilegesChanged")) {
            return;
        }
        onMultiSimConfigChanged();
        synchronized (this.mRecords) {
            if (!validatePhoneId(phoneId)) {
                throw new IllegalArgumentException("Invalid slot index: " + phoneId);
            }
            this.mCarrierPrivilegeStates.set(phoneId, new Pair<>(privilegedPackageNames, privilegedUids));
            Iterator<Record> it = this.mRecords.iterator();
            while (it.hasNext()) {
                Record r = it.next();
                if (r.matchCarrierPrivilegesCallback() && idMatch(r, -1, phoneId)) {
                    try {
                        r.carrierPrivilegesCallback.onCarrierPrivilegesChanged(Collections.unmodifiableList(privilegedPackageNames), Arrays.copyOf(privilegedUids, privilegedUids.length));
                    } catch (RemoteException e) {
                        this.mRemoveList.add(r.binder);
                    }
                }
            }
            handleRemoveListLocked();
        }
    }

    public void notifyCarrierServiceChanged(int phoneId, String packageName, int uid) {
        if (checkNotifyPermission("notifyCarrierServiceChanged") && validatePhoneId(phoneId)) {
            onMultiSimConfigChanged();
            synchronized (this.mRecords) {
                this.mCarrierServiceStates.set(phoneId, new Pair<>(packageName, Integer.valueOf(uid)));
                Iterator<Record> it = this.mRecords.iterator();
                while (it.hasNext()) {
                    Record r = it.next();
                    if (r.matchCarrierPrivilegesCallback() && idMatch(r, -1, phoneId)) {
                        try {
                            r.carrierPrivilegesCallback.onCarrierServiceChanged(packageName, uid);
                        } catch (RemoteException e) {
                            this.mRemoveList.add(r.binder);
                        }
                    }
                }
                handleRemoveListLocked();
            }
        }
    }

    @NeverCompile
    public void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
        IndentingPrintWriter pw = new IndentingPrintWriter(writer, "  ");
        if (DumpUtils.checkDumpPermission(this.mContext, TAG, pw)) {
            synchronized (this.mRecords) {
                int recordCount = this.mRecords.size();
                pw.println("last known state:");
                pw.increaseIndent();
                for (int i = 0; i < getTelephonyManager().getActiveModemCount(); i++) {
                    pw.println("Phone Id=" + i);
                    pw.increaseIndent();
                    pw.println("mCallState=" + this.mCallState[i]);
                    pw.println("mRingingCallState=" + this.mRingingCallState[i]);
                    pw.println("mForegroundCallState=" + this.mForegroundCallState[i]);
                    pw.println("mBackgroundCallState=" + this.mBackgroundCallState[i]);
                    pw.println("mPreciseCallState=" + this.mPreciseCallState[i]);
                    pw.println("mCallDisconnectCause=" + this.mCallDisconnectCause[i]);
                    pw.println("mCallIncomingNumber=" + this.mCallIncomingNumber[i]);
                    pw.println("mServiceState=" + this.mServiceState[i]);
                    pw.println("mVoiceActivationState= " + this.mVoiceActivationState[i]);
                    pw.println("mDataActivationState= " + this.mDataActivationState[i]);
                    pw.println("mUserMobileDataState= " + this.mUserMobileDataState[i]);
                    pw.println("mSignalStrength=" + this.mSignalStrength[i]);
                    pw.println("mMessageWaiting=" + this.mMessageWaiting[i]);
                    pw.println("mCallForwarding=" + this.mCallForwarding[i]);
                    pw.println("mDataActivity=" + this.mDataActivity[i]);
                    pw.println("mDataConnectionState=" + this.mDataConnectionState[i]);
                    pw.println("mCellIdentity=" + this.mCellIdentity[i]);
                    pw.println("mCellInfo=" + this.mCellInfo.get(i));
                    pw.println("mImsCallDisconnectCause=" + this.mImsReasonInfo.get(i));
                    pw.println("mSrvccState=" + this.mSrvccState[i]);
                    pw.println("mCallPreciseDisconnectCause=" + this.mCallPreciseDisconnectCause[i]);
                    pw.println("mCallQuality=" + this.mCallQuality[i]);
                    pw.println("mCallAttributes=" + this.mCallAttributes[i]);
                    pw.println("mCallNetworkType=" + this.mCallNetworkType[i]);
                    pw.println("mPreciseDataConnectionStates=" + this.mPreciseDataConnectionStates.get(i));
                    pw.println("mOutgoingCallEmergencyNumber=" + this.mOutgoingCallEmergencyNumber[i]);
                    pw.println("mOutgoingSmsEmergencyNumber=" + this.mOutgoingSmsEmergencyNumber[i]);
                    pw.println("mBarringInfo=" + this.mBarringInfo.get(i));
                    pw.println("mCarrierNetworkChangeState=" + this.mCarrierNetworkChangeState[i]);
                    pw.println("mTelephonyDisplayInfo=" + this.mTelephonyDisplayInfos[i]);
                    pw.println("mIsDataEnabled=" + this.mIsDataEnabled);
                    pw.println("mDataEnabledReason=" + this.mDataEnabledReason);
                    pw.println("mAllowedNetworkTypeReason=" + this.mAllowedNetworkTypeReason[i]);
                    pw.println("mAllowedNetworkTypeValue=" + this.mAllowedNetworkTypeValue[i]);
                    pw.println("mPhysicalChannelConfigs=" + this.mPhysicalChannelConfigs.get(i));
                    pw.println("mLinkCapacityEstimateList=" + this.mLinkCapacityEstimateLists.get(i));
                    pw.println("mRadioPowerState=" + this.mRadioPowerState[i]);
                    Pair<List<String>, int[]> carrierPrivilegeState = this.mCarrierPrivilegeStates.get(i);
                    pw.println("mCarrierPrivilegeState=<packages=" + pii((List) carrierPrivilegeState.first) + ", uids=" + Arrays.toString((int[]) carrierPrivilegeState.second) + ">");
                    Pair<String, Integer> carrierServiceState = this.mCarrierServiceStates.get(i);
                    pw.println("mCarrierServiceState=<package=" + pii((String) carrierServiceState.first) + ", uid=" + carrierServiceState.second + ">");
                    pw.decreaseIndent();
                }
                pw.println("mPhoneCapability=" + this.mPhoneCapability);
                pw.println("mActiveDataSubId=" + this.mActiveDataSubId);
                pw.println("mEmergencyNumberList=" + this.mEmergencyNumberList);
                pw.println("mDefaultPhoneId=" + this.mDefaultPhoneId);
                pw.println("mDefaultSubId=" + this.mDefaultSubId);
                pw.decreaseIndent();
                pw.println("local logs:");
                pw.increaseIndent();
                this.mLocalLog.dump(fd, pw, args);
                pw.decreaseIndent();
                pw.println("listen logs:");
                pw.increaseIndent();
                this.mListenLog.dump(fd, pw, args);
                pw.decreaseIndent();
                pw.println("registrations: count=" + recordCount);
                pw.increaseIndent();
                Iterator<Record> it = this.mRecords.iterator();
                while (it.hasNext()) {
                    Record r = it.next();
                    pw.println(r);
                }
                pw.decreaseIndent();
            }
        }
    }

    /* JADX DEBUG: Multi-variable search result rejected for r15v0, resolved type: android.content.Context */
    /* JADX DEBUG: Multi-variable search result rejected for r16v0, resolved type: com.android.server.TelephonyRegistry */
    /* JADX DEBUG: Multi-variable search result rejected for r7v5, resolved type: android.content.Context */
    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r10v0 */
    /* JADX WARN: Type inference failed for: r10v1, types: [int, boolean] */
    /* JADX WARN: Type inference failed for: r10v7 */
    private void broadcastServiceStateChanged(ServiceState state, int phoneId, int subId) {
        long ident = Binder.clearCallingIdentity();
        try {
            this.mBatteryStats.notePhoneState(state.getState());
        } catch (RemoteException e) {
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(ident);
            throw th;
        }
        Binder.restoreCallingIdentity(ident);
        ?? r10 = 0;
        if (!((Boolean) Binder.withCleanCallingIdentity(new FunctionalUtils.ThrowingSupplier() { // from class: com.android.server.TelephonyRegistry$$ExternalSyntheticLambda1
            public final Object getOrThrow() {
                return TelephonyRegistry.this.m449x2cba6a78();
            }
        })).booleanValue()) {
            String[] locationBypassPackages = (String[]) Binder.withCleanCallingIdentity(new FunctionalUtils.ThrowingSupplier() { // from class: com.android.server.TelephonyRegistry$$ExternalSyntheticLambda2
                public final Object getOrThrow() {
                    return TelephonyRegistry.this.m450x8e0d0717();
                }
            });
            int length = locationBypassPackages.length;
            int i = 0;
            while (i < length) {
                String locationBypassPackage = locationBypassPackages[i];
                Intent fullIntent = createServiceStateIntent(state, subId, phoneId, r10);
                fullIntent.setPackage(locationBypassPackage);
                this.mContext.createContextAsUser(UserHandle.ALL, r10).sendBroadcastMultiplePermissions(fullIntent, new String[]{"android.permission.READ_PHONE_STATE"});
                this.mContext.createContextAsUser(UserHandle.ALL, r10).sendBroadcastMultiplePermissions(fullIntent, new String[]{"android.permission.READ_PRIVILEGED_PHONE_STATE"}, new String[]{"android.permission.READ_PHONE_STATE"});
                i++;
                r10 = 0;
            }
            Intent sanitizedIntent = createServiceStateIntent(state, subId, phoneId, true);
            this.mContext.createContextAsUser(UserHandle.ALL, 0).sendBroadcastMultiplePermissions(sanitizedIntent, new String[]{"android.permission.READ_PHONE_STATE"}, new String[0], locationBypassPackages);
            this.mContext.createContextAsUser(UserHandle.ALL, 0).sendBroadcastMultiplePermissions(sanitizedIntent, new String[]{"android.permission.READ_PRIVILEGED_PHONE_STATE"}, new String[]{"android.permission.READ_PHONE_STATE"}, locationBypassPackages);
            return;
        }
        Intent fullIntent2 = createServiceStateIntent(state, subId, phoneId, false);
        this.mContext.createContextAsUser(UserHandle.ALL, 0).sendBroadcastMultiplePermissions(fullIntent2, new String[]{"android.permission.READ_PHONE_STATE", "android.permission.ACCESS_FINE_LOCATION"});
        this.mContext.createContextAsUser(UserHandle.ALL, 0).sendBroadcastMultiplePermissions(fullIntent2, new String[]{"android.permission.READ_PRIVILEGED_PHONE_STATE", "android.permission.ACCESS_FINE_LOCATION"}, new String[]{"android.permission.READ_PHONE_STATE"});
        Intent sanitizedIntent2 = createServiceStateIntent(state, subId, phoneId, true);
        this.mContext.createContextAsUser(UserHandle.ALL, 0).sendBroadcastMultiplePermissions(sanitizedIntent2, new String[]{"android.permission.READ_PHONE_STATE"}, new String[]{"android.permission.ACCESS_FINE_LOCATION"});
        this.mContext.createContextAsUser(UserHandle.ALL, 0).sendBroadcastMultiplePermissions(sanitizedIntent2, new String[]{"android.permission.READ_PRIVILEGED_PHONE_STATE"}, new String[]{"android.permission.READ_PHONE_STATE", "android.permission.ACCESS_FINE_LOCATION"});
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$broadcastServiceStateChanged$1$com-android-server-TelephonyRegistry  reason: not valid java name */
    public /* synthetic */ Boolean m449x2cba6a78() throws Exception {
        Context context = this.mContext;
        return Boolean.valueOf(LocationAccessPolicy.isLocationModeEnabled(context, context.getUserId()));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$broadcastServiceStateChanged$2$com-android-server-TelephonyRegistry  reason: not valid java name */
    public /* synthetic */ String[] m450x8e0d0717() throws Exception {
        return LocationAccessPolicy.getLocationBypassPackages(this.mContext);
    }

    private Intent createServiceStateIntent(ServiceState state, int subId, int phoneId, boolean sanitizeLocation) {
        Intent intent = new Intent("android.intent.action.SERVICE_STATE");
        intent.addFlags(16777216);
        Bundle data = new Bundle();
        if (sanitizeLocation) {
            state.createLocationInfoSanitizedCopy(true).fillInNotifierBundle(data);
        } else {
            state.fillInNotifierBundle(data);
        }
        intent.putExtras(data);
        intent.putExtra(PHONE_CONSTANTS_SUBSCRIPTION_KEY, subId);
        intent.putExtra("android.telephony.extra.SUBSCRIPTION_INDEX", subId);
        intent.putExtra(PHONE_CONSTANTS_SLOT_KEY, phoneId);
        intent.putExtra("android.telephony.extra.SLOT_INDEX", phoneId);
        return intent;
    }

    private void broadcastSignalStrengthChanged(SignalStrength signalStrength, int phoneId, int subId) {
        long ident = Binder.clearCallingIdentity();
        try {
            this.mBatteryStats.notePhoneSignalStrength(signalStrength);
        } catch (RemoteException e) {
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(ident);
            throw th;
        }
        Binder.restoreCallingIdentity(ident);
        Intent intent = new Intent(ACTION_SIGNAL_STRENGTH_CHANGED);
        Bundle data = new Bundle();
        fillInSignalStrengthNotifierBundle(signalStrength, data);
        intent.putExtras(data);
        intent.putExtra(PHONE_CONSTANTS_SUBSCRIPTION_KEY, subId);
        intent.putExtra(PHONE_CONSTANTS_SLOT_KEY, phoneId);
        this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
    }

    private void fillInSignalStrengthNotifierBundle(SignalStrength signalStrength, Bundle bundle) {
        List<CellSignalStrength> cellSignalStrengths = signalStrength.getCellSignalStrengths();
        for (CellSignalStrength cellSignalStrength : cellSignalStrengths) {
            if (cellSignalStrength instanceof CellSignalStrengthLte) {
                bundle.putParcelable("Lte", (CellSignalStrengthLte) cellSignalStrength);
            } else if (cellSignalStrength instanceof CellSignalStrengthCdma) {
                bundle.putParcelable("Cdma", (CellSignalStrengthCdma) cellSignalStrength);
            } else if (cellSignalStrength instanceof CellSignalStrengthGsm) {
                bundle.putParcelable("Gsm", (CellSignalStrengthGsm) cellSignalStrength);
            } else if (cellSignalStrength instanceof CellSignalStrengthWcdma) {
                bundle.putParcelable("Wcdma", (CellSignalStrengthWcdma) cellSignalStrength);
            } else if (cellSignalStrength instanceof CellSignalStrengthTdscdma) {
                bundle.putParcelable("Tdscdma", (CellSignalStrengthTdscdma) cellSignalStrength);
            } else if (cellSignalStrength instanceof CellSignalStrengthNr) {
                bundle.putParcelable("Nr", (CellSignalStrengthNr) cellSignalStrength);
            }
        }
    }

    private void broadcastCallStateChanged(int state, String incomingNumber, int phoneId, int subId) {
        long ident = Binder.clearCallingIdentity();
        try {
            if (state == 0) {
                this.mBatteryStats.notePhoneOff();
                FrameworkStatsLog.write(95, 0);
            } else {
                this.mBatteryStats.notePhoneOn();
                FrameworkStatsLog.write(95, 1);
            }
        } catch (RemoteException e) {
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(ident);
            throw th;
        }
        Binder.restoreCallingIdentity(ident);
        Intent intent = new Intent("android.intent.action.PHONE_STATE");
        intent.putExtra("state", callStateToString(state));
        if (subId != -1) {
            intent.setAction(ACTION_SUBSCRIPTION_PHONE_STATE_CHANGED);
            intent.putExtra(PHONE_CONSTANTS_SUBSCRIPTION_KEY, subId);
            intent.putExtra("android.telephony.extra.SUBSCRIPTION_INDEX", subId);
        }
        if (phoneId != -1) {
            intent.putExtra(PHONE_CONSTANTS_SLOT_KEY, phoneId);
            intent.putExtra("android.telephony.extra.SLOT_INDEX", phoneId);
        }
        intent.addFlags(16777216);
        Intent intentWithPhoneNumber = new Intent(intent);
        intentWithPhoneNumber.putExtra("incoming_number", incomingNumber);
        this.mContext.sendBroadcastAsUser(intentWithPhoneNumber, UserHandle.ALL, "android.permission.READ_PRIVILEGED_PHONE_STATE");
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL, "android.permission.READ_PHONE_STATE", 51);
        this.mContext.sendBroadcastAsUserMultiplePermissions(intentWithPhoneNumber, UserHandle.ALL, new String[]{"android.permission.READ_PHONE_STATE", "android.permission.READ_CALL_LOG"});
    }

    private static String callStateToString(int callState) {
        switch (callState) {
            case 1:
                return TelephonyManager.EXTRA_STATE_RINGING;
            case 2:
                return TelephonyManager.EXTRA_STATE_OFFHOOK;
            default:
                return TelephonyManager.EXTRA_STATE_IDLE;
        }
    }

    private void broadcastDataConnectionStateChanged(int slotIndex, int subId, PreciseDataConnectionState pdcs) {
        Intent intent = new Intent(ACTION_ANY_DATA_CONNECTION_STATE_CHANGED);
        intent.putExtra("state", TelephonyUtils.dataStateToString(pdcs.getState()));
        intent.putExtra(PHONE_CONSTANTS_DATA_APN_KEY, pdcs.getApnSetting().getApnName());
        intent.putExtra(PHONE_CONSTANTS_DATA_APN_TYPE_KEY, getApnTypesStringFromBitmask(pdcs.getApnSetting().getApnTypeBitmask()));
        intent.putExtra(PHONE_CONSTANTS_SLOT_KEY, slotIndex);
        intent.putExtra(PHONE_CONSTANTS_SUBSCRIPTION_KEY, subId);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL, "android.permission.READ_PHONE_STATE");
        this.mContext.createContextAsUser(UserHandle.ALL, 0).sendBroadcastMultiplePermissions(intent, new String[]{"android.permission.READ_PRIVILEGED_PHONE_STATE"}, new String[]{"android.permission.READ_PHONE_STATE"});
    }

    public static String getApnTypesStringFromBitmask(int apnTypeBitmask) {
        List<String> types = new ArrayList<>();
        int remainingApnTypes = apnTypeBitmask;
        if ((remainingApnTypes & 17) == 17) {
            types.add(HealthServiceWrapperHidl.INSTANCE_VENDOR);
            remainingApnTypes &= -18;
        }
        while (remainingApnTypes != 0) {
            int highestApnTypeBit = Integer.highestOneBit(remainingApnTypes);
            String apnString = ApnSetting.getApnTypeString(highestApnTypeBit);
            if (!TextUtils.isEmpty(apnString)) {
                types.add(apnString);
            }
            remainingApnTypes &= ~highestApnTypeBit;
        }
        return TextUtils.join(",", types);
    }

    private void enforceNotifyPermissionOrCarrierPrivilege(String method) {
        if (checkNotifyPermission()) {
            return;
        }
        TelephonyPermissions.enforceCallingOrSelfCarrierPrivilege(this.mContext, SubscriptionManager.getDefaultSubscriptionId(), method);
    }

    private boolean checkNotifyPermission(String method) {
        if (checkNotifyPermission()) {
            return true;
        }
        String str = "Modify Phone State Permission Denial: " + method + " from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid();
        return false;
    }

    private boolean checkNotifyPermission() {
        return this.mContext.checkCallingOrSelfPermission("android.permission.MODIFY_PHONE_STATE") == 0;
    }

    private boolean checkListenerPermission(Set<Integer> events, int subId, String callingPackage, String callingFeatureId, String message) {
        LocationAccessPolicy.LocationPermissionQuery.Builder locationQueryBuilder = new LocationAccessPolicy.LocationPermissionQuery.Builder().setCallingPackage(callingPackage).setCallingFeatureId(callingFeatureId).setMethod(message + " events: " + events).setCallingPid(Binder.getCallingPid()).setCallingUid(Binder.getCallingUid());
        boolean shouldCheckLocationPermissions = false;
        if (isLocationPermissionRequired(events)) {
            locationQueryBuilder.setMinSdkVersionForFine(29);
            locationQueryBuilder.setMinSdkVersionForCoarse(0);
            locationQueryBuilder.setMinSdkVersionForEnforcement(0);
            shouldCheckLocationPermissions = true;
        }
        boolean isPermissionCheckSuccessful = true;
        if (shouldCheckLocationPermissions) {
            LocationAccessPolicy.LocationPermissionResult result = LocationAccessPolicy.checkLocationPermission(this.mContext, locationQueryBuilder.build());
            switch (AnonymousClass3.$SwitchMap$android$telephony$LocationAccessPolicy$LocationPermissionResult[result.ordinal()]) {
                case 1:
                    throw new SecurityException("Unable to listen for events " + events + " due to insufficient location permissions.");
                case 2:
                    isPermissionCheckSuccessful = false;
                    break;
            }
        }
        if (isPhoneStatePermissionRequired(events, callingPackage, Binder.getCallingUserHandle()) && !TelephonyPermissions.checkCallingOrSelfReadPhoneState(this.mContext, subId, callingPackage, callingFeatureId, message)) {
            isPermissionCheckSuccessful = false;
        }
        if (isPrecisePhoneStatePermissionRequired(events)) {
            try {
                this.mContext.enforceCallingOrSelfPermission("android.permission.READ_PRECISE_PHONE_STATE", null);
            } catch (SecurityException e) {
                TelephonyPermissions.enforceCallingOrSelfCarrierPrivilege(this.mContext, subId, message);
            }
        }
        if (isActiveEmergencySessionPermissionRequired(events)) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.READ_ACTIVE_EMERGENCY_SESSION", null);
        }
        if (isPrivilegedPhoneStatePermissionRequired(events)) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.READ_PRIVILEGED_PHONE_STATE", null);
        }
        return isPermissionCheckSuccessful;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: com.android.server.TelephonyRegistry$3  reason: invalid class name */
    /* loaded from: classes.dex */
    public static /* synthetic */ class AnonymousClass3 {
        static final /* synthetic */ int[] $SwitchMap$android$telephony$LocationAccessPolicy$LocationPermissionResult;

        static {
            int[] iArr = new int[LocationAccessPolicy.LocationPermissionResult.values().length];
            $SwitchMap$android$telephony$LocationAccessPolicy$LocationPermissionResult = iArr;
            try {
                iArr[LocationAccessPolicy.LocationPermissionResult.DENIED_HARD.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$android$telephony$LocationAccessPolicy$LocationPermissionResult[LocationAccessPolicy.LocationPermissionResult.DENIED_SOFT.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleRemoveListLocked() {
        int size = this.mRemoveList.size();
        if (size > 0) {
            Iterator<IBinder> it = this.mRemoveList.iterator();
            while (it.hasNext()) {
                IBinder b = it.next();
                remove(b);
            }
            this.mRemoveList.clear();
        }
    }

    private boolean validateEventAndUserLocked(Record r, int event) {
        boolean z;
        long callingIdentity = Binder.clearCallingIdentity();
        try {
            int foregroundUser = ActivityManager.getCurrentUser();
            if (UserHandle.getUserId(r.callerUid) == foregroundUser) {
                if (r.matchTelephonyCallbackEvent(event)) {
                    z = true;
                    boolean valid = z;
                    return valid;
                }
            }
            z = false;
            boolean valid2 = z;
            return valid2;
        } finally {
            Binder.restoreCallingIdentity(callingIdentity);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean validatePhoneId(int phoneId) {
        return phoneId >= 0 && phoneId < getTelephonyManager().getActiveModemCount();
    }

    private static void log(String s) {
        Rlog.d(TAG, s);
    }

    private static void loge(String s) {
        Rlog.e(TAG, s);
    }

    boolean idMatch(Record r, int subId, int phoneId) {
        return subId < 0 ? r.phoneId == phoneId : r.subId == Integer.MAX_VALUE ? subId == this.mDefaultSubId : r.subId == subId;
    }

    private boolean checkFineLocationAccess(Record r) {
        return checkFineLocationAccess(r, 1);
    }

    private boolean checkCoarseLocationAccess(Record r) {
        return checkCoarseLocationAccess(r, 1);
    }

    private boolean checkFineLocationAccess(Record r, int minSdk) {
        if (r.renounceFineLocationAccess) {
            return false;
        }
        final LocationAccessPolicy.LocationPermissionQuery query = new LocationAccessPolicy.LocationPermissionQuery.Builder().setCallingPackage(r.callingPackage).setCallingFeatureId(r.callingFeatureId).setCallingPid(r.callerPid).setCallingUid(r.callerUid).setMethod("TelephonyRegistry push").setLogAsInfo(true).setMinSdkVersionForFine(minSdk).setMinSdkVersionForCoarse(minSdk).setMinSdkVersionForEnforcement(minSdk).build();
        return ((Boolean) Binder.withCleanCallingIdentity(new FunctionalUtils.ThrowingSupplier() { // from class: com.android.server.TelephonyRegistry$$ExternalSyntheticLambda3
            public final Object getOrThrow() {
                return TelephonyRegistry.this.m452xf72592cc(query);
            }
        })).booleanValue();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$checkFineLocationAccess$3$com-android-server-TelephonyRegistry  reason: not valid java name */
    public /* synthetic */ Boolean m452xf72592cc(LocationAccessPolicy.LocationPermissionQuery query) throws Exception {
        LocationAccessPolicy.LocationPermissionResult locationResult = LocationAccessPolicy.checkLocationPermission(this.mContext, query);
        return Boolean.valueOf(locationResult == LocationAccessPolicy.LocationPermissionResult.ALLOWED);
    }

    private boolean checkCoarseLocationAccess(Record r, int minSdk) {
        if (r.renounceCoarseLocationAccess) {
            return false;
        }
        final LocationAccessPolicy.LocationPermissionQuery query = new LocationAccessPolicy.LocationPermissionQuery.Builder().setCallingPackage(r.callingPackage).setCallingFeatureId(r.callingFeatureId).setCallingPid(r.callerPid).setCallingUid(r.callerUid).setMethod("TelephonyRegistry push").setLogAsInfo(true).setMinSdkVersionForCoarse(minSdk).setMinSdkVersionForFine(Integer.MAX_VALUE).setMinSdkVersionForEnforcement(minSdk).build();
        return ((Boolean) Binder.withCleanCallingIdentity(new FunctionalUtils.ThrowingSupplier() { // from class: com.android.server.TelephonyRegistry$$ExternalSyntheticLambda0
            public final Object getOrThrow() {
                return TelephonyRegistry.this.m451xfa200a56(query);
            }
        })).booleanValue();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$checkCoarseLocationAccess$4$com-android-server-TelephonyRegistry  reason: not valid java name */
    public /* synthetic */ Boolean m451xfa200a56(LocationAccessPolicy.LocationPermissionQuery query) throws Exception {
        LocationAccessPolicy.LocationPermissionResult locationResult = LocationAccessPolicy.checkLocationPermission(this.mContext, query);
        return Boolean.valueOf(locationResult == LocationAccessPolicy.LocationPermissionResult.ALLOWED);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void checkPossibleMissNotify(Record r, int phoneId) {
        Set<Integer> events = r.eventList;
        if (events == null || events.isEmpty()) {
            log("checkPossibleMissNotify: events = null.");
            return;
        }
        if (events.contains(1)) {
            try {
                ServiceState ss = new ServiceState(this.mServiceState[phoneId]);
                if (checkFineLocationAccess(r, 29)) {
                    r.callback.onServiceStateChanged(ss);
                } else if (checkCoarseLocationAccess(r, 29)) {
                    r.callback.onServiceStateChanged(ss.createLocationInfoSanitizedCopy(false));
                } else {
                    r.callback.onServiceStateChanged(ss.createLocationInfoSanitizedCopy(true));
                }
            } catch (RemoteException e) {
                this.mRemoveList.add(r.binder);
            }
        }
        if (events.contains(9)) {
            try {
                SignalStrength signalStrength = this.mSignalStrength[phoneId];
                if (signalStrength != null) {
                    r.callback.onSignalStrengthsChanged(makeSignalStrength(phoneId, signalStrength));
                }
            } catch (RemoteException e2) {
                this.mRemoveList.add(r.binder);
            }
        }
        if (events.contains(2)) {
            try {
                SignalStrength signalStrength2 = this.mSignalStrength[phoneId];
                if (signalStrength2 != null) {
                    int gsmSignalStrength = signalStrength2.getGsmSignalStrength();
                    r.callback.onSignalStrengthChanged(gsmSignalStrength == 99 ? -1 : gsmSignalStrength);
                }
            } catch (RemoteException e3) {
                this.mRemoveList.add(r.binder);
            }
        }
        if (validateEventAndUserLocked(r, 11)) {
            try {
                if (checkCoarseLocationAccess(r, 1) && checkFineLocationAccess(r, 29)) {
                    r.callback.onCellInfoChanged(this.mCellInfo.get(phoneId));
                }
            } catch (RemoteException e4) {
                this.mRemoveList.add(r.binder);
            }
        }
        if (events.contains(20)) {
            try {
                r.callback.onUserMobileDataStateChanged(this.mUserMobileDataState[phoneId]);
            } catch (RemoteException e5) {
                this.mRemoveList.add(r.binder);
            }
        }
        if (events.contains(21)) {
            try {
                if (this.mTelephonyDisplayInfos[phoneId] != null) {
                    r.callback.onDisplayInfoChanged(this.mTelephonyDisplayInfos[phoneId]);
                }
            } catch (RemoteException e6) {
                this.mRemoveList.add(r.binder);
            }
        }
        if (events.contains(3)) {
            try {
                r.callback.onMessageWaitingIndicatorChanged(this.mMessageWaiting[phoneId]);
            } catch (RemoteException e7) {
                this.mRemoveList.add(r.binder);
            }
        }
        if (events.contains(4)) {
            try {
                r.callback.onCallForwardingIndicatorChanged(this.mCallForwarding[phoneId]);
            } catch (RemoteException e8) {
                this.mRemoveList.add(r.binder);
            }
        }
        if (validateEventAndUserLocked(r, 5)) {
            try {
                if (checkCoarseLocationAccess(r, 1) && checkFineLocationAccess(r, 29)) {
                    r.callback.onCellLocationChanged(this.mCellIdentity[phoneId]);
                }
            } catch (RemoteException e9) {
                this.mRemoveList.add(r.binder);
            }
        }
        if (events.contains(7)) {
            try {
                r.callback.onDataConnectionStateChanged(this.mDataConnectionState[phoneId], this.mDataConnectionNetworkType[phoneId]);
            } catch (RemoteException e10) {
                this.mRemoveList.add(r.binder);
            }
        }
    }

    private String getNetworkTypeName(int type) {
        switch (type) {
            case 1:
                return "GPRS";
            case 2:
                return "EDGE";
            case 3:
                return "UMTS";
            case 4:
                return "CDMA";
            case 5:
                return "CDMA - EvDo rev. 0";
            case 6:
                return "CDMA - EvDo rev. A";
            case 7:
                return "CDMA - 1xRTT";
            case 8:
                return "HSDPA";
            case 9:
                return "HSUPA";
            case 10:
                return "HSPA";
            case 11:
                return "iDEN";
            case 12:
                return "CDMA - EvDo rev. B";
            case 13:
                return "LTE";
            case 14:
                return "CDMA - eHRPD";
            case 15:
                return "HSPA+";
            case 16:
                return "GSM";
            case 17:
                return "TD_SCDMA";
            case 18:
                return "IWLAN";
            case 19:
            default:
                return "UNKNOWN";
            case 20:
                return "NR";
        }
    }

    private static PreciseCallState createPreciseCallState() {
        return new PreciseCallState(-1, -1, -1, -1, -1);
    }

    private static CallQuality createCallQuality() {
        return new CallQuality(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int getPhoneIdFromSubId(int subId) {
        SubscriptionManager subManager = (SubscriptionManager) this.mContext.getSystemService("telephony_subscription_service");
        if (subManager == null) {
            return -1;
        }
        if (subId == Integer.MAX_VALUE) {
            subId = SubscriptionManager.getDefaultSubscriptionId();
        }
        SubscriptionInfo info = subManager.getActiveSubscriptionInfo(subId);
        if (info == null) {
            return -1;
        }
        return info.getSimSlotIndex();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static String pii(String packageName) {
        return Build.IS_DEBUGGABLE ? packageName : "***";
    }

    public SignalStrength makeSignalStrength(int phoneId) {
        Constructor constructor = sMtkSignalStrength;
        if (constructor == null) {
            SignalStrength instance = new SignalStrength();
            return instance;
        }
        try {
            SignalStrength instance2 = (SignalStrength) constructor.newInstance(new Integer(phoneId));
            return instance2;
        } catch (Exception e) {
            e.printStackTrace();
            SignalStrength instance3 = new SignalStrength();
            return instance3;
        }
    }

    public SignalStrength makeSignalStrength(int phoneId, SignalStrength signalStrength) {
        Constructor constructor = sMtkSignalStrength2;
        if (constructor == null) {
            SignalStrength instance = new SignalStrength(signalStrength);
            return instance;
        }
        try {
            SignalStrength instance2 = (SignalStrength) constructor.newInstance(new Integer(phoneId), signalStrength);
            return instance2;
        } catch (Exception e) {
            e.printStackTrace();
            SignalStrength instance3 = new SignalStrength(signalStrength);
            return instance3;
        }
    }

    private static String pii(List<String> packageNames) {
        if (packageNames.isEmpty() || Build.IS_DEBUGGABLE) {
            return packageNames.toString();
        }
        return "[***, size=" + packageNames.size() + "]";
    }
}
