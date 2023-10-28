package android.telephony;

import android.compat.Compatibility;
import android.content.Context;
import android.media.AudioPort$$ExternalSyntheticLambda0;
import android.os.Binder;
import android.os.Build;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyCallback;
import android.telephony.TelephonyManager;
import android.telephony.TelephonyRegistryManager;
import android.telephony.emergency.EmergencyNumber;
import android.telephony.ims.ImsReasonInfo;
import android.util.ArraySet;
import android.util.Log;
import com.android.internal.listeners.ListenerExecutor;
import com.android.internal.telephony.ICarrierPrivilegesCallback;
import com.android.internal.telephony.IOnSubscriptionsChangedListener;
import com.android.internal.telephony.ITelephonyRegistry;
import com.android.internal.util.FunctionalUtils;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.Executor;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;
/* loaded from: classes3.dex */
public class TelephonyRegistryManager {
    private static final boolean IS_DEBUG_BUILD;
    private static final long LISTEN_CODE_CHANGE = 147600208;
    public static final int SIM_ACTIVATION_TYPE_DATA = 1;
    public static final int SIM_ACTIVATION_TYPE_VOICE = 0;
    private static final String TAG = "TelephonyRegistryManager";
    private static final WeakHashMap<TelephonyManager.CarrierPrivilegesCallback, WeakReference<CarrierPrivilegesCallbackWrapper>> sCarrierPrivilegeCallbacks;
    private static ITelephonyRegistry sRegistry;
    private final Context mContext;
    private final Map<SubscriptionManager.OnSubscriptionsChangedListener, IOnSubscriptionsChangedListener> mSubscriptionChangedListenerMap = new HashMap();
    private final Map<SubscriptionManager.OnOpportunisticSubscriptionsChangedListener, IOnSubscriptionsChangedListener> mOpportunisticSubscriptionChangedListenerMap = new HashMap();

    static {
        IS_DEBUG_BUILD = Build.TYPE.equals("eng") || Build.TYPE.equals("userdebug");
        sCarrierPrivilegeCallbacks = new WeakHashMap<>();
    }

    public TelephonyRegistryManager(Context context) {
        this.mContext = context;
        if (sRegistry == null) {
            sRegistry = ITelephonyRegistry.Stub.asInterface(ServiceManager.getService("telephony.registry"));
        }
    }

    public void addOnSubscriptionsChangedListener(SubscriptionManager.OnSubscriptionsChangedListener listener, Executor executor) {
        if (this.mSubscriptionChangedListenerMap.get(listener) != null) {
            Log.d(TAG, "addOnSubscriptionsChangedListener listener already present");
            return;
        }
        IOnSubscriptionsChangedListener callback = new AnonymousClass1(executor, listener);
        this.mSubscriptionChangedListenerMap.put(listener, callback);
        try {
            sRegistry.addOnSubscriptionsChangedListener(this.mContext.getOpPackageName(), this.mContext.getAttributionTag(), callback);
        } catch (RemoteException ex) {
            throw ex.rethrowFromSystemServer();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: android.telephony.TelephonyRegistryManager$1  reason: invalid class name */
    /* loaded from: classes3.dex */
    public class AnonymousClass1 extends IOnSubscriptionsChangedListener.Stub {
        final /* synthetic */ Executor val$executor;
        final /* synthetic */ SubscriptionManager.OnSubscriptionsChangedListener val$listener;

        AnonymousClass1(Executor executor, SubscriptionManager.OnSubscriptionsChangedListener onSubscriptionsChangedListener) {
            this.val$executor = executor;
            this.val$listener = onSubscriptionsChangedListener;
        }

        @Override // com.android.internal.telephony.IOnSubscriptionsChangedListener
        public void onSubscriptionsChanged() {
            Executor executor = this.val$executor;
            final SubscriptionManager.OnSubscriptionsChangedListener onSubscriptionsChangedListener = this.val$listener;
            executor.execute(new Runnable() { // from class: android.telephony.TelephonyRegistryManager$1$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    TelephonyRegistryManager.AnonymousClass1.lambda$onSubscriptionsChanged$0(SubscriptionManager.OnSubscriptionsChangedListener.this);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public static /* synthetic */ void lambda$onSubscriptionsChanged$0(SubscriptionManager.OnSubscriptionsChangedListener listener) {
            long durationUptime = 0;
            if (TelephonyRegistryManager.IS_DEBUG_BUILD) {
                durationUptime = SystemClock.uptimeMillis();
            }
            listener.onSubscriptionsChanged();
            if (TelephonyRegistryManager.IS_DEBUG_BUILD) {
                long durationUptime2 = SystemClock.uptimeMillis() - durationUptime;
                if (durationUptime2 > 200) {
                    Log.d(TelephonyRegistryManager.TAG, "Cost " + durationUptime2 + "ms in listener = " + listener);
                }
            }
        }
    }

    public void removeOnSubscriptionsChangedListener(SubscriptionManager.OnSubscriptionsChangedListener listener) {
        if (this.mSubscriptionChangedListenerMap.get(listener) == null) {
            return;
        }
        try {
            sRegistry.removeOnSubscriptionsChangedListener(this.mContext.getOpPackageName(), this.mSubscriptionChangedListenerMap.get(listener));
            this.mSubscriptionChangedListenerMap.remove(listener);
        } catch (RemoteException ex) {
            throw ex.rethrowFromSystemServer();
        }
    }

    public void addOnOpportunisticSubscriptionsChangedListener(SubscriptionManager.OnOpportunisticSubscriptionsChangedListener listener, Executor executor) {
        if (this.mOpportunisticSubscriptionChangedListenerMap.get(listener) != null) {
            Log.d(TAG, "addOnOpportunisticSubscriptionsChangedListener listener already present");
            return;
        }
        IOnSubscriptionsChangedListener callback = new AnonymousClass2(executor, listener);
        this.mOpportunisticSubscriptionChangedListenerMap.put(listener, callback);
        try {
            sRegistry.addOnOpportunisticSubscriptionsChangedListener(this.mContext.getOpPackageName(), this.mContext.getAttributionTag(), callback);
        } catch (RemoteException ex) {
            throw ex.rethrowFromSystemServer();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: android.telephony.TelephonyRegistryManager$2  reason: invalid class name */
    /* loaded from: classes3.dex */
    public class AnonymousClass2 extends IOnSubscriptionsChangedListener.Stub {
        final /* synthetic */ Executor val$executor;
        final /* synthetic */ SubscriptionManager.OnOpportunisticSubscriptionsChangedListener val$listener;

        AnonymousClass2(Executor executor, SubscriptionManager.OnOpportunisticSubscriptionsChangedListener onOpportunisticSubscriptionsChangedListener) {
            this.val$executor = executor;
            this.val$listener = onOpportunisticSubscriptionsChangedListener;
        }

        @Override // com.android.internal.telephony.IOnSubscriptionsChangedListener
        public void onSubscriptionsChanged() {
            long identity = Binder.clearCallingIdentity();
            try {
                Log.d(TelephonyRegistryManager.TAG, "onOpportunisticSubscriptionsChanged callback received.");
                Executor executor = this.val$executor;
                final SubscriptionManager.OnOpportunisticSubscriptionsChangedListener onOpportunisticSubscriptionsChangedListener = this.val$listener;
                executor.execute(new Runnable() { // from class: android.telephony.TelephonyRegistryManager$2$$ExternalSyntheticLambda0
                    @Override // java.lang.Runnable
                    public final void run() {
                        SubscriptionManager.OnOpportunisticSubscriptionsChangedListener.this.onOpportunisticSubscriptionsChanged();
                    }
                });
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }
    }

    public void removeOnOpportunisticSubscriptionsChangedListener(SubscriptionManager.OnOpportunisticSubscriptionsChangedListener listener) {
        if (this.mOpportunisticSubscriptionChangedListenerMap.get(listener) == null) {
            return;
        }
        try {
            sRegistry.removeOnSubscriptionsChangedListener(this.mContext.getOpPackageName(), this.mOpportunisticSubscriptionChangedListenerMap.get(listener));
            this.mOpportunisticSubscriptionChangedListenerMap.remove(listener);
        } catch (RemoteException ex) {
            throw ex.rethrowFromSystemServer();
        }
    }

    public void listenFromListener(int subId, boolean renounceFineLocationAccess, boolean renounceCoarseLocationAccess, String pkg, String featureId, PhoneStateListener listener, int events, boolean notifyNow) {
        int subId2;
        if (listener == null) {
            throw new IllegalStateException("telephony service is null.");
        }
        try {
            int[] eventsList = getEventsFromBitmask(events).stream().mapToInt(new ToIntFunction() { // from class: android.telephony.TelephonyRegistryManager$$ExternalSyntheticLambda1
                @Override // java.util.function.ToIntFunction
                public final int applyAsInt(Object obj) {
                    int intValue;
                    intValue = ((Integer) obj).intValue();
                    return intValue;
                }
            }).toArray();
            try {
                if (Compatibility.isChangeEnabled((long) LISTEN_CODE_CHANGE)) {
                    listener.mSubId = Integer.valueOf(eventsList.length == 0 ? -1 : subId);
                } else if (listener.mSubId != null) {
                    subId2 = listener.mSubId.intValue();
                    sRegistry.listenWithEventList(renounceFineLocationAccess, renounceCoarseLocationAccess, subId2, pkg, featureId, listener.callback, eventsList, notifyNow);
                    return;
                }
                sRegistry.listenWithEventList(renounceFineLocationAccess, renounceCoarseLocationAccess, subId2, pkg, featureId, listener.callback, eventsList, notifyNow);
                return;
            } catch (RemoteException e) {
                e = e;
                throw e.rethrowFromSystemServer();
            }
            subId2 = subId;
        } catch (RemoteException e2) {
            e = e2;
        }
    }

    private void listenFromCallback(boolean renounceFineLocationAccess, boolean renounceCoarseLocationAccess, int subId, String pkg, String featureId, TelephonyCallback telephonyCallback, int[] events, boolean notifyNow) {
        try {
            try {
                sRegistry.listenWithEventList(renounceFineLocationAccess, renounceCoarseLocationAccess, subId, pkg, featureId, telephonyCallback.callback, events, notifyNow);
            } catch (RemoteException e) {
                e = e;
                throw e.rethrowFromSystemServer();
            }
        } catch (RemoteException e2) {
            e = e2;
        }
    }

    public void notifyCarrierNetworkChange(boolean active) {
        try {
            sRegistry.notifyCarrierNetworkChange(active);
        } catch (RemoteException ex) {
            throw ex.rethrowFromSystemServer();
        }
    }

    public void notifyCarrierNetworkChange(int subscriptionId, boolean active) {
        try {
            sRegistry.notifyCarrierNetworkChangeWithSubId(subscriptionId, active);
        } catch (RemoteException ex) {
            throw ex.rethrowFromSystemServer();
        }
    }

    public void notifyCallStateChanged(int slotIndex, int subId, int state, String incomingNumber) {
        try {
            sRegistry.notifyCallState(slotIndex, subId, state, incomingNumber);
        } catch (RemoteException ex) {
            throw ex.rethrowFromSystemServer();
        }
    }

    public void notifyCallStateChangedForAllSubscriptions(int state, String incomingNumber) {
        try {
            sRegistry.notifyCallStateForAllSubs(state, incomingNumber);
        } catch (RemoteException ex) {
            throw ex.rethrowFromSystemServer();
        }
    }

    public void notifySubscriptionInfoChanged() {
        try {
            sRegistry.notifySubscriptionInfoChanged();
        } catch (RemoteException ex) {
            throw ex.rethrowFromSystemServer();
        }
    }

    public void notifyOpportunisticSubscriptionInfoChanged() {
        try {
            sRegistry.notifyOpportunisticSubscriptionInfoChanged();
        } catch (RemoteException ex) {
            throw ex.rethrowFromSystemServer();
        }
    }

    public void notifyServiceStateChanged(int slotIndex, int subId, ServiceState state) {
        try {
            sRegistry.notifyServiceStateForPhoneId(slotIndex, subId, state);
        } catch (RemoteException ex) {
            throw ex.rethrowFromSystemServer();
        }
    }

    public void notifySignalStrengthChanged(int slotIndex, int subId, SignalStrength signalStrength) {
        try {
            sRegistry.notifySignalStrengthForPhoneId(slotIndex, subId, signalStrength);
        } catch (RemoteException ex) {
            throw ex.rethrowFromSystemServer();
        }
    }

    public void notifyMessageWaitingChanged(int slotIndex, int subId, boolean msgWaitingInd) {
        try {
            sRegistry.notifyMessageWaitingChangedForPhoneId(slotIndex, subId, msgWaitingInd);
        } catch (RemoteException ex) {
            throw ex.rethrowFromSystemServer();
        }
    }

    public void notifyCallForwardingChanged(int subId, boolean callForwardInd) {
        try {
            sRegistry.notifyCallForwardingChangedForSubscriber(subId, callForwardInd);
        } catch (RemoteException ex) {
            throw ex.rethrowFromSystemServer();
        }
    }

    public void notifyDataActivityChanged(int subId, int dataActivityType) {
        try {
            sRegistry.notifyDataActivityForSubscriber(subId, dataActivityType);
        } catch (RemoteException ex) {
            throw ex.rethrowFromSystemServer();
        }
    }

    public void notifyDataConnectionForSubscriber(int slotIndex, int subId, PreciseDataConnectionState preciseState) {
        try {
            sRegistry.notifyDataConnectionForSubscriber(slotIndex, subId, preciseState);
        } catch (RemoteException ex) {
            throw ex.rethrowFromSystemServer();
        }
    }

    public void notifyCallQualityChanged(int slotIndex, int subId, CallQuality callQuality, int networkType) {
        try {
            sRegistry.notifyCallQualityChanged(callQuality, slotIndex, subId, networkType);
        } catch (RemoteException ex) {
            throw ex.rethrowFromSystemServer();
        }
    }

    public void notifyEmergencyNumberList(int slotIndex, int subId) {
        try {
            sRegistry.notifyEmergencyNumberList(slotIndex, subId);
        } catch (RemoteException ex) {
            throw ex.rethrowFromSystemServer();
        }
    }

    public void notifyOutgoingEmergencyCall(int phoneId, int subId, EmergencyNumber emergencyNumber) {
        try {
            sRegistry.notifyOutgoingEmergencyCall(phoneId, subId, emergencyNumber);
        } catch (RemoteException ex) {
            throw ex.rethrowFromSystemServer();
        }
    }

    public void notifyOutgoingEmergencySms(int phoneId, int subId, EmergencyNumber emergencyNumber) {
        try {
            sRegistry.notifyOutgoingEmergencySms(phoneId, subId, emergencyNumber);
        } catch (RemoteException ex) {
            throw ex.rethrowFromSystemServer();
        }
    }

    public void notifyRadioPowerStateChanged(int slotIndex, int subId, int radioPowerState) {
        try {
            sRegistry.notifyRadioPowerStateChanged(slotIndex, subId, radioPowerState);
        } catch (RemoteException ex) {
            throw ex.rethrowFromSystemServer();
        }
    }

    public void notifyPhoneCapabilityChanged(PhoneCapability phoneCapability) {
        try {
            sRegistry.notifyPhoneCapabilityChanged(phoneCapability);
        } catch (RemoteException ex) {
            throw ex.rethrowFromSystemServer();
        }
    }

    public void notifyDataActivationStateChanged(int slotIndex, int subId, int activationState) {
        try {
            sRegistry.notifySimActivationStateChangedForPhoneId(slotIndex, subId, 1, activationState);
        } catch (RemoteException ex) {
            throw ex.rethrowFromSystemServer();
        }
    }

    public void notifyVoiceActivationStateChanged(int slotIndex, int subId, int activationState) {
        try {
            sRegistry.notifySimActivationStateChangedForPhoneId(slotIndex, subId, 0, activationState);
        } catch (RemoteException ex) {
            throw ex.rethrowFromSystemServer();
        }
    }

    public void notifyUserMobileDataStateChanged(int slotIndex, int subId, boolean state) {
        try {
            sRegistry.notifyUserMobileDataStateChangedForPhoneId(slotIndex, subId, state);
        } catch (RemoteException ex) {
            throw ex.rethrowFromSystemServer();
        }
    }

    public void notifyDisplayInfoChanged(int slotIndex, int subscriptionId, TelephonyDisplayInfo telephonyDisplayInfo) {
        try {
            sRegistry.notifyDisplayInfoChanged(slotIndex, subscriptionId, telephonyDisplayInfo);
        } catch (RemoteException ex) {
            throw ex.rethrowFromSystemServer();
        }
    }

    public void notifyImsDisconnectCause(int subId, ImsReasonInfo imsReasonInfo) {
        try {
            sRegistry.notifyImsDisconnectCause(subId, imsReasonInfo);
        } catch (RemoteException ex) {
            throw ex.rethrowFromSystemServer();
        }
    }

    public void notifySrvccStateChanged(int subId, int state) {
        try {
            sRegistry.notifySrvccStateChanged(subId, state);
        } catch (RemoteException ex) {
            throw ex.rethrowFromSystemServer();
        }
    }

    public void notifyPreciseCallState(int slotIndex, int subId, int ringCallPreciseState, int foregroundCallPreciseState, int backgroundCallPreciseState) {
        try {
            sRegistry.notifyPreciseCallState(slotIndex, subId, ringCallPreciseState, foregroundCallPreciseState, backgroundCallPreciseState);
        } catch (RemoteException ex) {
            throw ex.rethrowFromSystemServer();
        }
    }

    public void notifyDisconnectCause(int slotIndex, int subId, int cause, int preciseCause) {
        try {
            sRegistry.notifyDisconnectCause(slotIndex, subId, cause, preciseCause);
        } catch (RemoteException ex) {
            throw ex.rethrowFromSystemServer();
        }
    }

    public void notifyCellLocation(int subId, CellIdentity cellLocation) {
        try {
            sRegistry.notifyCellLocationForSubscriber(subId, cellLocation);
        } catch (RemoteException ex) {
            throw ex.rethrowFromSystemServer();
        }
    }

    public void notifyCellInfoChanged(int subId, List<CellInfo> cellInfo) {
        try {
            sRegistry.notifyCellInfoForSubscriber(subId, cellInfo);
        } catch (RemoteException ex) {
            throw ex.rethrowFromSystemServer();
        }
    }

    public void notifyActiveDataSubIdChanged(int activeDataSubId) {
        try {
            sRegistry.notifyActiveDataSubIdChanged(activeDataSubId);
        } catch (RemoteException ex) {
            throw ex.rethrowFromSystemServer();
        }
    }

    public void notifyRegistrationFailed(int slotIndex, int subId, CellIdentity cellIdentity, String chosenPlmn, int domain, int causeCode, int additionalCauseCode) {
        try {
            sRegistry.notifyRegistrationFailed(slotIndex, subId, cellIdentity, chosenPlmn, domain, causeCode, additionalCauseCode);
        } catch (RemoteException ex) {
            throw ex.rethrowFromSystemServer();
        }
    }

    public void notifyBarringInfoChanged(int slotIndex, int subId, BarringInfo barringInfo) {
        try {
            sRegistry.notifyBarringInfoChanged(slotIndex, subId, barringInfo);
        } catch (RemoteException ex) {
            throw ex.rethrowFromSystemServer();
        }
    }

    public void notifyPhysicalChannelConfigForSubscriber(int slotIndex, int subId, List<PhysicalChannelConfig> configs) {
        try {
            sRegistry.notifyPhysicalChannelConfigForSubscriber(slotIndex, subId, configs);
        } catch (RemoteException ex) {
            throw ex.rethrowFromSystemServer();
        }
    }

    public void notifyDataEnabled(int slotIndex, int subId, boolean enabled, int reason) {
        try {
            sRegistry.notifyDataEnabled(slotIndex, subId, enabled, reason);
        } catch (RemoteException ex) {
            throw ex.rethrowFromSystemServer();
        }
    }

    public void notifyAllowedNetworkTypesChanged(int slotIndex, int subId, int reason, long allowedNetworkType) {
        try {
            sRegistry.notifyAllowedNetworkTypesChanged(slotIndex, subId, reason, allowedNetworkType);
        } catch (RemoteException ex) {
            throw ex.rethrowFromSystemServer();
        }
    }

    public void notifyLinkCapacityEstimateChanged(int slotIndex, int subId, List<LinkCapacityEstimate> linkCapacityEstimateList) {
        try {
            sRegistry.notifyLinkCapacityEstimateChanged(slotIndex, subId, linkCapacityEstimateList);
        } catch (RemoteException ex) {
            throw ex.rethrowFromSystemServer();
        }
    }

    public Set<Integer> getEventsFromCallback(TelephonyCallback telephonyCallback) {
        Set<Integer> eventList = new ArraySet<>();
        if (telephonyCallback instanceof TelephonyCallback.ServiceStateListener) {
            eventList.add(1);
        }
        if (telephonyCallback instanceof TelephonyCallback.MessageWaitingIndicatorListener) {
            eventList.add(3);
        }
        if (telephonyCallback instanceof TelephonyCallback.CallForwardingIndicatorListener) {
            eventList.add(4);
        }
        if (telephonyCallback instanceof TelephonyCallback.CellLocationListener) {
            eventList.add(5);
        }
        if (telephonyCallback instanceof TelephonyCallback.CallStateListener) {
            eventList.add(6);
        }
        if (telephonyCallback instanceof TelephonyCallback.DataConnectionStateListener) {
            eventList.add(7);
        }
        if (telephonyCallback instanceof TelephonyCallback.DataActivityListener) {
            eventList.add(8);
        }
        if (telephonyCallback instanceof TelephonyCallback.SignalStrengthsListener) {
            eventList.add(9);
        }
        if (telephonyCallback instanceof TelephonyCallback.CellInfoListener) {
            eventList.add(11);
        }
        if (telephonyCallback instanceof TelephonyCallback.PreciseCallStateListener) {
            eventList.add(12);
        }
        if (telephonyCallback instanceof TelephonyCallback.CallDisconnectCauseListener) {
            eventList.add(26);
        }
        if (telephonyCallback instanceof TelephonyCallback.ImsCallDisconnectCauseListener) {
            eventList.add(28);
        }
        if (telephonyCallback instanceof TelephonyCallback.PreciseDataConnectionStateListener) {
            eventList.add(13);
        }
        if (telephonyCallback instanceof TelephonyCallback.SrvccStateListener) {
            eventList.add(16);
        }
        if (telephonyCallback instanceof TelephonyCallback.VoiceActivationStateListener) {
            eventList.add(18);
        }
        if (telephonyCallback instanceof TelephonyCallback.DataActivationStateListener) {
            eventList.add(19);
        }
        if (telephonyCallback instanceof TelephonyCallback.UserMobileDataStateListener) {
            eventList.add(20);
        }
        if (telephonyCallback instanceof TelephonyCallback.DisplayInfoListener) {
            eventList.add(21);
        }
        if (telephonyCallback instanceof TelephonyCallback.EmergencyNumberListListener) {
            eventList.add(25);
        }
        if (telephonyCallback instanceof TelephonyCallback.OutgoingEmergencyCallListener) {
            eventList.add(29);
        }
        if (telephonyCallback instanceof TelephonyCallback.OutgoingEmergencySmsListener) {
            eventList.add(30);
        }
        if (telephonyCallback instanceof TelephonyCallback.PhoneCapabilityListener) {
            eventList.add(22);
        }
        if (telephonyCallback instanceof TelephonyCallback.ActiveDataSubscriptionIdListener) {
            eventList.add(23);
        }
        if (telephonyCallback instanceof TelephonyCallback.RadioPowerStateListener) {
            eventList.add(24);
        }
        if (telephonyCallback instanceof TelephonyCallback.CarrierNetworkListener) {
            eventList.add(17);
        }
        if (telephonyCallback instanceof TelephonyCallback.RegistrationFailedListener) {
            eventList.add(31);
        }
        if (telephonyCallback instanceof TelephonyCallback.CallAttributesListener) {
            eventList.add(27);
        }
        if (telephonyCallback instanceof TelephonyCallback.BarringInfoListener) {
            eventList.add(32);
        }
        if (telephonyCallback instanceof TelephonyCallback.PhysicalChannelConfigListener) {
            eventList.add(33);
        }
        if (telephonyCallback instanceof TelephonyCallback.DataEnabledListener) {
            eventList.add(34);
        }
        if (telephonyCallback instanceof TelephonyCallback.AllowedNetworkTypesListener) {
            eventList.add(35);
        }
        if (telephonyCallback instanceof TelephonyCallback.LinkCapacityEstimateChangedListener) {
            eventList.add(37);
        }
        return eventList;
    }

    private Set<Integer> getEventsFromBitmask(int eventMask) {
        Set<Integer> eventList = new ArraySet<>();
        if ((eventMask & 1) != 0) {
            eventList.add(1);
        }
        if ((eventMask & 2) != 0) {
            eventList.add(2);
        }
        if ((eventMask & 4) != 0) {
            eventList.add(3);
        }
        if ((eventMask & 8) != 0) {
            eventList.add(4);
        }
        if ((eventMask & 16) != 0) {
            eventList.add(5);
        }
        if ((eventMask & 32) != 0) {
            eventList.add(36);
        }
        if ((eventMask & 64) != 0) {
            eventList.add(7);
        }
        if ((eventMask & 128) != 0) {
            eventList.add(8);
        }
        if ((eventMask & 256) != 0) {
            eventList.add(9);
        }
        if ((eventMask & 1024) != 0) {
            eventList.add(11);
        }
        if ((eventMask & 2048) != 0) {
            eventList.add(12);
        }
        if ((eventMask & 4096) != 0) {
            eventList.add(13);
        }
        if ((eventMask & 8192) != 0) {
            eventList.add(14);
        }
        if ((32768 & eventMask) != 0) {
            eventList.add(15);
        }
        if ((eventMask & 16384) != 0) {
            eventList.add(16);
        }
        if ((65536 & eventMask) != 0) {
            eventList.add(17);
        }
        if ((131072 & eventMask) != 0) {
            eventList.add(18);
        }
        if ((262144 & eventMask) != 0) {
            eventList.add(19);
        }
        if ((524288 & eventMask) != 0) {
            eventList.add(20);
        }
        if ((1048576 & eventMask) != 0) {
            eventList.add(21);
        }
        if ((2097152 & eventMask) != 0) {
            eventList.add(22);
        }
        if ((4194304 & eventMask) != 0) {
            eventList.add(23);
        }
        if ((8388608 & eventMask) != 0) {
            eventList.add(24);
        }
        if ((16777216 & eventMask) != 0) {
            eventList.add(25);
        }
        if ((33554432 & eventMask) != 0) {
            eventList.add(26);
        }
        if ((67108864 & eventMask) != 0) {
            eventList.add(27);
        }
        if ((134217728 & eventMask) != 0) {
            eventList.add(28);
        }
        if ((268435456 & eventMask) != 0) {
            eventList.add(29);
        }
        if ((536870912 & eventMask) != 0) {
            eventList.add(30);
        }
        if ((1073741824 & eventMask) != 0) {
            eventList.add(31);
        }
        if ((Integer.MIN_VALUE & eventMask) != 0) {
            eventList.add(32);
        }
        return eventList;
    }

    public void registerTelephonyCallback(boolean renounceFineLocationAccess, boolean renounceCoarseLocationAccess, Executor executor, int subId, String pkgName, String attributionTag, TelephonyCallback callback, boolean notifyNow) {
        if (callback == null) {
            throw new IllegalStateException("telephony service is null.");
        }
        callback.init(executor);
        listenFromCallback(renounceFineLocationAccess, renounceCoarseLocationAccess, subId, pkgName, attributionTag, callback, getEventsFromCallback(callback).stream().mapToInt(new ToIntFunction() { // from class: android.telephony.TelephonyRegistryManager$$ExternalSyntheticLambda0
            @Override // java.util.function.ToIntFunction
            public final int applyAsInt(Object obj) {
                int intValue;
                intValue = ((Integer) obj).intValue();
                return intValue;
            }
        }).toArray(), notifyNow);
    }

    public void unregisterTelephonyCallback(int subId, String pkgName, String attributionTag, TelephonyCallback callback, boolean notifyNow) {
        listenFromCallback(false, false, subId, pkgName, attributionTag, callback, new int[0], notifyNow);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes3.dex */
    public static class CarrierPrivilegesCallbackWrapper extends ICarrierPrivilegesCallback.Stub implements ListenerExecutor {
        private final WeakReference<TelephonyManager.CarrierPrivilegesCallback> mCallback;
        private final Executor mExecutor;

        CarrierPrivilegesCallbackWrapper(TelephonyManager.CarrierPrivilegesCallback callback, Executor executor) {
            this.mCallback = new WeakReference<>(callback);
            this.mExecutor = executor;
        }

        @Override // com.android.internal.telephony.ICarrierPrivilegesCallback
        public void onCarrierPrivilegesChanged(List<String> privilegedPackageNames, int[] privilegedUids) {
            final Set<String> privilegedPkgNamesSet = Set.copyOf(privilegedPackageNames);
            final Set<Integer> privilegedUidsSet = (Set) Arrays.stream(privilegedUids).boxed().collect(Collectors.toSet());
            Binder.withCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: android.telephony.TelephonyRegistryManager$CarrierPrivilegesCallbackWrapper$$ExternalSyntheticLambda0
                @Override // com.android.internal.util.FunctionalUtils.ThrowingRunnable
                public final void runOrThrow() {
                    TelephonyRegistryManager.CarrierPrivilegesCallbackWrapper.this.m4154x30c1a44c(privilegedPkgNamesSet, privilegedUidsSet);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onCarrierPrivilegesChanged$1$android-telephony-TelephonyRegistryManager$CarrierPrivilegesCallbackWrapper  reason: not valid java name */
        public /* synthetic */ void m4154x30c1a44c(final Set privilegedPkgNamesSet, final Set privilegedUidsSet) throws Exception {
            Executor executor = this.mExecutor;
            WeakReference<TelephonyManager.CarrierPrivilegesCallback> weakReference = this.mCallback;
            Objects.requireNonNull(weakReference);
            executeSafely(executor, new TelephonyRegistryManager$CarrierPrivilegesCallbackWrapper$$ExternalSyntheticLambda1(weakReference), new ListenerExecutor.ListenerOperation() { // from class: android.telephony.TelephonyRegistryManager$CarrierPrivilegesCallbackWrapper$$ExternalSyntheticLambda3
                @Override // com.android.internal.listeners.ListenerExecutor.ListenerOperation
                public final void operate(Object obj) {
                    ((TelephonyManager.CarrierPrivilegesCallback) obj).onCarrierPrivilegesChanged(privilegedPkgNamesSet, privilegedUidsSet);
                }
            });
        }

        @Override // com.android.internal.telephony.ICarrierPrivilegesCallback
        public void onCarrierServiceChanged(final String packageName, final int uid) {
            Binder.withCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: android.telephony.TelephonyRegistryManager$CarrierPrivilegesCallbackWrapper$$ExternalSyntheticLambda4
                @Override // com.android.internal.util.FunctionalUtils.ThrowingRunnable
                public final void runOrThrow() {
                    TelephonyRegistryManager.CarrierPrivilegesCallbackWrapper.this.m4155x7667d13(packageName, uid);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onCarrierServiceChanged$3$android-telephony-TelephonyRegistryManager$CarrierPrivilegesCallbackWrapper  reason: not valid java name */
        public /* synthetic */ void m4155x7667d13(final String packageName, final int uid) throws Exception {
            Executor executor = this.mExecutor;
            WeakReference<TelephonyManager.CarrierPrivilegesCallback> weakReference = this.mCallback;
            Objects.requireNonNull(weakReference);
            executeSafely(executor, new TelephonyRegistryManager$CarrierPrivilegesCallbackWrapper$$ExternalSyntheticLambda1(weakReference), new ListenerExecutor.ListenerOperation() { // from class: android.telephony.TelephonyRegistryManager$CarrierPrivilegesCallbackWrapper$$ExternalSyntheticLambda2
                @Override // com.android.internal.listeners.ListenerExecutor.ListenerOperation
                public final void operate(Object obj) {
                    ((TelephonyManager.CarrierPrivilegesCallback) obj).onCarrierServiceChanged(packageName, uid);
                }
            });
        }
    }

    public void addCarrierPrivilegesCallback(int logicalSlotIndex, Executor executor, TelephonyManager.CarrierPrivilegesCallback callback) {
        if (callback == null || executor == null) {
            throw new IllegalArgumentException("callback and executor must be non-null");
        }
        WeakHashMap<TelephonyManager.CarrierPrivilegesCallback, WeakReference<CarrierPrivilegesCallbackWrapper>> weakHashMap = sCarrierPrivilegeCallbacks;
        synchronized (weakHashMap) {
            WeakReference<CarrierPrivilegesCallbackWrapper> existing = weakHashMap.get(callback);
            if (existing != null && existing.get() != null) {
                Log.d(TAG, "addCarrierPrivilegesCallback: callback already registered");
                return;
            }
            CarrierPrivilegesCallbackWrapper wrapper = new CarrierPrivilegesCallbackWrapper(callback, executor);
            weakHashMap.put(callback, new WeakReference<>(wrapper));
            try {
                sRegistry.addCarrierPrivilegesCallback(logicalSlotIndex, wrapper, this.mContext.getOpPackageName(), this.mContext.getAttributionTag());
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    public void removeCarrierPrivilegesCallback(TelephonyManager.CarrierPrivilegesCallback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("listener must be non-null");
        }
        WeakHashMap<TelephonyManager.CarrierPrivilegesCallback, WeakReference<CarrierPrivilegesCallbackWrapper>> weakHashMap = sCarrierPrivilegeCallbacks;
        synchronized (weakHashMap) {
            WeakReference<CarrierPrivilegesCallbackWrapper> ref = weakHashMap.remove(callback);
            if (ref == null) {
                return;
            }
            CarrierPrivilegesCallbackWrapper wrapper = ref.get();
            if (wrapper == null) {
                return;
            }
            try {
                sRegistry.removeCarrierPrivilegesCallback(wrapper, this.mContext.getOpPackageName());
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    public void notifyCarrierPrivilegesChanged(int logicalSlotIndex, Set<String> privilegedPackageNames, Set<Integer> privilegedUids) {
        if (privilegedPackageNames == null || privilegedUids == null) {
            throw new IllegalArgumentException("privilegedPackageNames and privilegedUids must be non-null");
        }
        try {
            List<String> pkgList = List.copyOf(privilegedPackageNames);
            int[] uids = privilegedUids.stream().mapToInt(new AudioPort$$ExternalSyntheticLambda0()).toArray();
            sRegistry.notifyCarrierPrivilegesChanged(logicalSlotIndex, pkgList, uids);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void notifyCarrierServiceChanged(int logicalSlotIndex, String packageName, int uid) {
        try {
            sRegistry.notifyCarrierServiceChanged(logicalSlotIndex, packageName, uid);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }
}
