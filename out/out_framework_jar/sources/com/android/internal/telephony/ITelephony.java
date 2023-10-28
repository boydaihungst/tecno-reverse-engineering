package com.android.internal.telephony;

import android.app.PendingIntent;
import android.content.Intent;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Messenger;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.WorkSource;
import android.telecom.PhoneAccountHandle;
import android.telephony.CallForwardingInfo;
import android.telephony.CarrierRestrictionRules;
import android.telephony.CellIdentity;
import android.telephony.CellInfo;
import android.telephony.ClientRequestStats;
import android.telephony.IBootstrapAuthenticationCallback;
import android.telephony.ICellInfoCallback;
import android.telephony.IccOpenLogicalChannelResponse;
import android.telephony.NeighboringCellInfo;
import android.telephony.NetworkScanRequest;
import android.telephony.PhoneCapability;
import android.telephony.PhoneNumberRange;
import android.telephony.RadioAccessSpecifier;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.SignalStrengthUpdateRequest;
import android.telephony.TelephonyHistogram;
import android.telephony.ThermalMitigationRequest;
import android.telephony.UiccCardInfo;
import android.telephony.UiccSlotInfo;
import android.telephony.UiccSlotMapping;
import android.telephony.VisualVoicemailSmsFilterSettings;
import android.telephony.emergency.EmergencyNumber;
import android.telephony.gba.UaSecurityProtocolIdentifier;
import android.telephony.ims.RcsClientConfiguration;
import android.telephony.ims.RcsContactUceCapability;
import android.telephony.ims.aidl.IFeatureProvisioningCallback;
import android.telephony.ims.aidl.IImsCapabilityCallback;
import android.telephony.ims.aidl.IImsConfig;
import android.telephony.ims.aidl.IImsConfigCallback;
import android.telephony.ims.aidl.IImsRegistration;
import android.telephony.ims.aidl.IImsRegistrationCallback;
import android.telephony.ims.aidl.IRcsConfigCallback;
import com.android.ims.internal.IImsServiceFeatureCallback;
import com.android.internal.telephony.IBooleanConsumer;
import com.android.internal.telephony.ICallForwardingInfoCallback;
import com.android.internal.telephony.IImsStateCallback;
import com.android.internal.telephony.IIntegerConsumer;
import com.android.internal.telephony.INumberVerificationCallback;
import java.util.List;
import java.util.Map;
/* loaded from: classes4.dex */
public interface ITelephony extends IInterface {
    RcsContactUceCapability addUceRegistrationOverrideShell(int i, List<String> list) throws RemoteException;

    void bootstrapAuthenticationRequest(int i, int i2, Uri uri, UaSecurityProtocolIdentifier uaSecurityProtocolIdentifier, boolean z, IBootstrapAuthenticationCallback iBootstrapAuthenticationCallback) throws RemoteException;

    void call(String str, String str2) throws RemoteException;

    boolean canChangeDtmfToneLength(int i, String str, String str2) throws RemoteException;

    boolean canConnectTo5GInDsdsMode() throws RemoteException;

    void carrierActionReportDefaultNetworkStatus(int i, boolean z) throws RemoteException;

    void carrierActionResetAll(int i) throws RemoteException;

    void carrierActionSetRadioEnabled(int i, boolean z) throws RemoteException;

    int changeIccLockPassword(int i, String str, String str2) throws RemoteException;

    int checkCarrierPrivilegesForPackage(int i, String str) throws RemoteException;

    int checkCarrierPrivilegesForPackageAnyPhone(String str) throws RemoteException;

    boolean clearCarrierImsServiceOverride(int i) throws RemoteException;

    void clearSignalStrengthUpdateRequest(int i, SignalStrengthUpdateRequest signalStrengthUpdateRequest, String str) throws RemoteException;

    RcsContactUceCapability clearUceRegistrationOverrideShell(int i) throws RemoteException;

    void dial(String str) throws RemoteException;

    boolean disableDataConnectivity(String str) throws RemoteException;

    void disableIms(int i) throws RemoteException;

    void disableLocationUpdates() throws RemoteException;

    void disableVisualVoicemailSmsFilter(String str, int i) throws RemoteException;

    boolean doesSwitchMultiSimConfigTriggerReboot(int i, String str, String str2) throws RemoteException;

    boolean enableDataConnectivity(String str) throws RemoteException;

    void enableIms(int i) throws RemoteException;

    void enableLocationUpdates() throws RemoteException;

    boolean enableModemForSlot(int i, boolean z) throws RemoteException;

    void enableVideoCalling(boolean z) throws RemoteException;

    void enableVisualVoicemailSmsFilter(String str, int i, VisualVoicemailSmsFilterSettings visualVoicemailSmsFilterSettings) throws RemoteException;

    void enqueueSmsPickResult(String str, String str2, IIntegerConsumer iIntegerConsumer) throws RemoteException;

    void factoryReset(int i, String str) throws RemoteException;

    int getActivePhoneType() throws RemoteException;

    int getActivePhoneTypeForSlot(int i) throws RemoteException;

    VisualVoicemailSmsFilterSettings getActiveVisualVoicemailSmsFilterSettings(int i) throws RemoteException;

    String getAidForAppType(int i, int i2) throws RemoteException;

    List<CellInfo> getAllCellInfo(String str, String str2) throws RemoteException;

    CarrierRestrictionRules getAllowedCarriers() throws RemoteException;

    int getAllowedNetworkTypesBitmask(int i) throws RemoteException;

    long getAllowedNetworkTypesForReason(int i, int i2) throws RemoteException;

    String getBoundGbaService(int i) throws RemoteException;

    String getBoundImsServicePackage(int i, boolean z, int i2) throws RemoteException;

    int getCallComposerStatus(int i) throws RemoteException;

    void getCallForwarding(int i, int i2, ICallForwardingInfoCallback iCallForwardingInfoCallback) throws RemoteException;

    int getCallState() throws RemoteException;

    int getCallStateForSubscription(int i, String str, String str2) throws RemoteException;

    void getCallWaitingStatus(int i, IIntegerConsumer iIntegerConsumer) throws RemoteException;

    String getCapabilityFromEab(String str) throws RemoteException;

    int getCardIdForDefaultEuicc(int i, String str) throws RemoteException;

    int getCarrierIdFromMccMnc(int i, String str, boolean z) throws RemoteException;

    int getCarrierIdListVersion(int i) throws RemoteException;

    List<String> getCarrierPackageNamesForIntentAndPhone(Intent intent, int i) throws RemoteException;

    int getCarrierPrivilegeStatus(int i) throws RemoteException;

    int getCarrierPrivilegeStatusForUid(int i, int i2) throws RemoteException;

    String getCarrierServicePackageNameForLogicalSlot(int i) throws RemoteException;

    boolean getCarrierSingleRegistrationEnabled(int i) throws RemoteException;

    int getCdmaEriIconIndex(String str, String str2) throws RemoteException;

    int getCdmaEriIconIndexForSubscriber(int i, String str, String str2) throws RemoteException;

    int getCdmaEriIconMode(String str, String str2) throws RemoteException;

    int getCdmaEriIconModeForSubscriber(int i, String str, String str2) throws RemoteException;

    String getCdmaEriText(String str, String str2) throws RemoteException;

    String getCdmaEriTextForSubscriber(int i, String str, String str2) throws RemoteException;

    String getCdmaMdn(int i) throws RemoteException;

    String getCdmaMin(int i) throws RemoteException;

    String getCdmaPrlVersion(int i) throws RemoteException;

    int getCdmaRoamingMode(int i) throws RemoteException;

    int getCdmaSubscriptionMode(int i) throws RemoteException;

    CellIdentity getCellLocation(String str, String str2) throws RemoteException;

    CellNetworkScanResult getCellNetworkScanResults(int i, String str, String str2) throws RemoteException;

    List<String> getCertsFromCarrierPrivilegeAccessRules(int i) throws RemoteException;

    List<ClientRequestStats> getClientRequestStats(String str, String str2, int i) throws RemoteException;

    String getContactFromEab(String str) throws RemoteException;

    String getCurrentPackageName() throws RemoteException;

    int getDataActivationState(int i, String str) throws RemoteException;

    int getDataActivity() throws RemoteException;

    int getDataActivityForSubId(int i) throws RemoteException;

    boolean getDataEnabled(int i) throws RemoteException;

    int getDataNetworkType(String str, String str2) throws RemoteException;

    int getDataNetworkTypeForSubscriber(int i, String str, String str2) throws RemoteException;

    int getDataState() throws RemoteException;

    int getDataStateForSubId(int i) throws RemoteException;

    @Deprecated
    String getDeviceId(String str) throws RemoteException;

    String getDeviceIdWithFeature(String str, String str2) throws RemoteException;

    boolean getDeviceSingleRegistrationEnabled() throws RemoteException;

    String getDeviceSoftwareVersionForSlot(int i, String str, String str2) throws RemoteException;

    boolean getDeviceUceEnabled() throws RemoteException;

    boolean getEmergencyCallbackMode(int i) throws RemoteException;

    int getEmergencyNumberDbVersion(int i) throws RemoteException;

    Map getEmergencyNumberList(String str, String str2) throws RemoteException;

    List<String> getEmergencyNumberListTestMode() throws RemoteException;

    List<String> getEquivalentHomePlmns(int i, String str, String str2) throws RemoteException;

    String getEsn(int i) throws RemoteException;

    String[] getForbiddenPlmns(int i, int i2, String str, String str2) throws RemoteException;

    int getGbaReleaseTime(int i) throws RemoteException;

    String getImeiForSlot(int i, String str, String str2) throws RemoteException;

    IImsConfig getImsConfig(int i, int i2) throws RemoteException;

    boolean getImsFeatureValidationOverride(int i) throws RemoteException;

    void getImsMmTelFeatureState(int i, IIntegerConsumer iIntegerConsumer) throws RemoteException;

    void getImsMmTelRegistrationState(int i, IIntegerConsumer iIntegerConsumer) throws RemoteException;

    void getImsMmTelRegistrationTransportType(int i, IIntegerConsumer iIntegerConsumer) throws RemoteException;

    int getImsProvisioningInt(int i, int i2) throws RemoteException;

    boolean getImsProvisioningStatusForCapability(int i, int i2, int i3) throws RemoteException;

    String getImsProvisioningString(int i, int i2) throws RemoteException;

    int getImsRegTechnologyForMmTel(int i) throws RemoteException;

    IImsRegistration getImsRegistration(int i, int i2) throws RemoteException;

    CellIdentity getLastKnownCellIdentity(int i, String str, String str2) throws RemoteException;

    String getLastUcePidfXmlShell(int i) throws RemoteException;

    RcsContactUceCapability getLatestRcsContactUceCapabilityShell(int i) throws RemoteException;

    String getLine1AlphaTagForDisplay(int i, String str, String str2) throws RemoteException;

    String getLine1NumberForDisplay(int i, String str, String str2) throws RemoteException;

    int getLteOnCdmaMode(String str, String str2) throws RemoteException;

    int getLteOnCdmaModeForSubscriber(int i, String str, String str2) throws RemoteException;

    String getManualNetworkSelectionPlmn(int i) throws RemoteException;

    String getManufacturerCodeForSlot(int i) throws RemoteException;

    String getMeidForSlot(int i, String str, String str2) throws RemoteException;

    String[] getMergedImsisFromGroup(int i, String str) throws RemoteException;

    String[] getMergedSubscriberIds(int i, String str, String str2) throws RemoteException;

    String getMmsUAProfUrl(int i) throws RemoteException;

    String getMmsUserAgent(int i) throws RemoteException;

    String getMobileProvisioningUrl() throws RemoteException;

    String getModemService() throws RemoteException;

    List<NeighboringCellInfo> getNeighboringCellInfo(String str, String str2) throws RemoteException;

    String getNetworkCountryIsoForPhone(int i) throws RemoteException;

    int getNetworkSelectionMode(int i) throws RemoteException;

    int getNetworkTypeForSubscriber(int i, String str, String str2) throws RemoteException;

    int getNumberOfModemsWithSimultaneousDataConnections(int i, String str, String str2) throws RemoteException;

    List<String> getPackagesWithCarrierPrivileges(int i) throws RemoteException;

    List<String> getPackagesWithCarrierPrivilegesForAllPhones() throws RemoteException;

    PhoneAccountHandle getPhoneAccountHandleForSubscriptionId(int i) throws RemoteException;

    PhoneCapability getPhoneCapability() throws RemoteException;

    int getRadioAccessFamily(int i, String str) throws RemoteException;

    int getRadioHalVersion() throws RemoteException;

    int getRadioPowerState(int i, String str, String str2) throws RemoteException;

    boolean getRcsProvisioningStatusForCapability(int i, int i2, int i3) throws RemoteException;

    boolean getRcsSingleRegistrationTestModeEnabled() throws RemoteException;

    ServiceState getServiceStateForSubscriber(int i, boolean z, boolean z2, String str, String str2) throws RemoteException;

    SignalStrength getSignalStrength(int i) throws RemoteException;

    String getSimLocaleForSubscriber(int i) throws RemoteException;

    void getSlicingConfig(ResultReceiver resultReceiver) throws RemoteException;

    List<UiccSlotMapping> getSlotsMapping(String str) throws RemoteException;

    int getSubIdForPhoneAccountHandle(PhoneAccountHandle phoneAccountHandle, String str, String str2) throws RemoteException;

    int getSubscriptionCarrierId(int i) throws RemoteException;

    String getSubscriptionCarrierName(int i) throws RemoteException;

    int getSubscriptionSpecificCarrierId(int i) throws RemoteException;

    String getSubscriptionSpecificCarrierName(int i) throws RemoteException;

    List<RadioAccessSpecifier> getSystemSelectionChannels(int i) throws RemoteException;

    List<TelephonyHistogram> getTelephonyHistograms() throws RemoteException;

    String getTypeAllocationCodeForSlot(int i) throws RemoteException;

    List<UiccCardInfo> getUiccCardsInfo(String str) throws RemoteException;

    UiccSlotInfo[] getUiccSlotsInfo(String str) throws RemoteException;

    String getVisualVoicemailPackageName(String str, String str2, int i) throws RemoteException;

    Bundle getVisualVoicemailSettings(String str, int i) throws RemoteException;

    VisualVoicemailSmsFilterSettings getVisualVoicemailSmsFilterSettings(String str, int i) throws RemoteException;

    int getVoWiFiModeSetting(int i) throws RemoteException;

    int getVoWiFiRoamingModeSetting(int i) throws RemoteException;

    int getVoiceActivationState(int i, String str) throws RemoteException;

    int getVoiceMessageCountForSubscriber(int i, String str, String str2) throws RemoteException;

    int getVoiceNetworkTypeForSubscriber(int i, String str, String str2) throws RemoteException;

    Uri getVoicemailRingtoneUri(PhoneAccountHandle phoneAccountHandle) throws RemoteException;

    boolean handlePinMmi(String str) throws RemoteException;

    boolean handlePinMmiForSubscriber(int i, String str) throws RemoteException;

    void handleUssdRequest(int i, String str, ResultReceiver resultReceiver) throws RemoteException;

    boolean hasIccCard() throws RemoteException;

    boolean hasIccCardUsingSlotIndex(int i) throws RemoteException;

    boolean iccCloseLogicalChannel(IccLogicalChannelRequest iccLogicalChannelRequest) throws RemoteException;

    byte[] iccExchangeSimIO(int i, int i2, int i3, int i4, int i5, int i6, String str) throws RemoteException;

    IccOpenLogicalChannelResponse iccOpenLogicalChannel(IccLogicalChannelRequest iccLogicalChannelRequest) throws RemoteException;

    String iccTransmitApduBasicChannel(int i, String str, int i2, int i3, int i4, int i5, int i6, String str2) throws RemoteException;

    String iccTransmitApduBasicChannelByPort(int i, int i2, String str, int i3, int i4, int i5, int i6, int i7, String str2) throws RemoteException;

    String iccTransmitApduLogicalChannel(int i, int i2, int i3, int i4, int i5, int i6, int i7, String str) throws RemoteException;

    String iccTransmitApduLogicalChannelByPort(int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8, String str) throws RemoteException;

    int invokeOemRilRequestRaw(byte[] bArr, byte[] bArr2) throws RemoteException;

    boolean isAdvancedCallingSettingEnabled(int i) throws RemoteException;

    boolean isApnMetered(int i, int i2) throws RemoteException;

    boolean isApplicationOnUicc(int i, int i2) throws RemoteException;

    boolean isAvailable(int i, int i2, int i3) throws RemoteException;

    boolean isCapable(int i, int i2, int i3) throws RemoteException;

    boolean isConcurrentVoiceAndDataAllowed(int i) throws RemoteException;

    boolean isCrossSimCallingEnabledByUser(int i) throws RemoteException;

    boolean isDataConnectivityPossible(int i) throws RemoteException;

    boolean isDataEnabled(int i) throws RemoteException;

    boolean isDataEnabledForApn(int i, int i2, String str) throws RemoteException;

    boolean isDataEnabledForReason(int i, int i2) throws RemoteException;

    boolean isDataRoamingEnabled(int i) throws RemoteException;

    boolean isEmergencyNumber(String str, boolean z) throws RemoteException;

    boolean isHearingAidCompatibilitySupported() throws RemoteException;

    boolean isIccLockEnabled(int i) throws RemoteException;

    boolean isImsRegistered(int i) throws RemoteException;

    boolean isInEmergencySmsMode() throws RemoteException;

    boolean isManualNetworkSelectionAllowed(int i) throws RemoteException;

    void isMmTelCapabilitySupported(int i, IIntegerConsumer iIntegerConsumer, int i2, int i3) throws RemoteException;

    boolean isMobileDataPolicyEnabled(int i, int i2) throws RemoteException;

    boolean isModemEnabledForSlot(int i, String str, String str2) throws RemoteException;

    int isMultiSimSupported(String str, String str2) throws RemoteException;

    boolean isMvnoMatched(int i, int i2, String str) throws RemoteException;

    boolean isNrDualConnectivityEnabled(int i) throws RemoteException;

    boolean isProvisioningRequiredForCapability(int i, int i2, int i3) throws RemoteException;

    boolean isRadioInterfaceCapabilitySupported(String str) throws RemoteException;

    @Deprecated
    boolean isRadioOn(String str) throws RemoteException;

    @Deprecated
    boolean isRadioOnForSubscriber(int i, String str) throws RemoteException;

    boolean isRadioOnForSubscriberWithFeature(int i, String str, String str2) throws RemoteException;

    boolean isRadioOnWithFeature(String str, String str2) throws RemoteException;

    boolean isRcsProvisioningRequiredForCapability(int i, int i2, int i3) throws RemoteException;

    boolean isRcsVolteSingleRegistrationCapable(int i) throws RemoteException;

    boolean isRemovableEsimDefaultEuicc(String str) throws RemoteException;

    boolean isRttSupported(int i) throws RemoteException;

    boolean isTetheringApnRequiredForSubscriber(int i) throws RemoteException;

    boolean isTtyModeSupported() throws RemoteException;

    boolean isTtyOverVolteEnabled(int i) throws RemoteException;

    boolean isUserDataEnabled(int i) throws RemoteException;

    boolean isUsingNewDataStack() throws RemoteException;

    boolean isVideoCallingEnabled(String str, String str2) throws RemoteException;

    boolean isVideoTelephonyAvailable(int i) throws RemoteException;

    boolean isVoNrEnabled(int i) throws RemoteException;

    boolean isVoWiFiRoamingSettingEnabled(int i) throws RemoteException;

    boolean isVoWiFiSettingEnabled(int i) throws RemoteException;

    boolean isVoicemailVibrationEnabled(PhoneAccountHandle phoneAccountHandle) throws RemoteException;

    boolean isVtSettingEnabled(int i) throws RemoteException;

    boolean isWifiCallingAvailable(int i) throws RemoteException;

    boolean isWorldPhone(int i, String str, String str2) throws RemoteException;

    boolean needMobileRadioShutdown() throws RemoteException;

    boolean needsOtaServiceProvisioning() throws RemoteException;

    void notifyOtaEmergencyNumberDbInstalled() throws RemoteException;

    void notifyRcsAutoConfigurationReceived(int i, byte[] bArr, boolean z) throws RemoteException;

    String nvReadItem(int i) throws RemoteException;

    boolean nvWriteCdmaPrl(byte[] bArr) throws RemoteException;

    boolean nvWriteItem(int i, String str) throws RemoteException;

    int prepareForUnattendedReboot() throws RemoteException;

    boolean rebootModem(int i) throws RemoteException;

    void refreshUiccProfile(int i) throws RemoteException;

    void registerFeatureProvisioningChangedCallback(int i, IFeatureProvisioningCallback iFeatureProvisioningCallback) throws RemoteException;

    void registerImsProvisioningChangedCallback(int i, IImsConfigCallback iImsConfigCallback) throws RemoteException;

    void registerImsRegistrationCallback(int i, IImsRegistrationCallback iImsRegistrationCallback) throws RemoteException;

    void registerImsStateCallback(int i, int i2, IImsStateCallback iImsStateCallback, String str) throws RemoteException;

    void registerMmTelCapabilityCallback(int i, IImsCapabilityCallback iImsCapabilityCallback) throws RemoteException;

    void registerMmTelFeatureCallback(int i, IImsServiceFeatureCallback iImsServiceFeatureCallback) throws RemoteException;

    void registerRcsProvisioningCallback(int i, IRcsConfigCallback iRcsConfigCallback) throws RemoteException;

    int removeContactFromEab(int i, String str) throws RemoteException;

    RcsContactUceCapability removeUceRegistrationOverrideShell(int i, List<String> list) throws RemoteException;

    boolean removeUceRequestDisallowedStatus(int i) throws RemoteException;

    void requestCellInfoUpdate(int i, ICellInfoCallback iCellInfoCallback, String str, String str2) throws RemoteException;

    void requestCellInfoUpdateWithWorkSource(int i, ICellInfoCallback iCellInfoCallback, String str, String str2, WorkSource workSource) throws RemoteException;

    void requestModemActivityInfo(ResultReceiver resultReceiver) throws RemoteException;

    int requestNetworkScan(int i, boolean z, NetworkScanRequest networkScanRequest, Messenger messenger, IBinder iBinder, String str, String str2) throws RemoteException;

    void requestNumberVerification(PhoneNumberRange phoneNumberRange, long j, INumberVerificationCallback iNumberVerificationCallback, String str) throws RemoteException;

    void requestUserActivityNotification() throws RemoteException;

    void resetIms(int i) throws RemoteException;

    boolean resetModemConfig(int i) throws RemoteException;

    void resetOtaEmergencyNumberDbFilePath() throws RemoteException;

    void sendDeviceToDeviceMessage(int i, int i2) throws RemoteException;

    void sendDialerSpecialCode(String str, String str2) throws RemoteException;

    String sendEnvelopeWithStatus(int i, String str) throws RemoteException;

    int sendThermalMitigationRequest(int i, ThermalMitigationRequest thermalMitigationRequest, String str) throws RemoteException;

    void sendVisualVoicemailSmsForSubscriber(String str, String str2, int i, String str3, int i2, String str4, PendingIntent pendingIntent) throws RemoteException;

    void setActiveDeviceToDeviceTransport(String str) throws RemoteException;

    void setAdvancedCallingSettingEnabled(int i, boolean z) throws RemoteException;

    int setAllowedCarriers(CarrierRestrictionRules carrierRestrictionRules) throws RemoteException;

    boolean setAllowedNetworkTypesForReason(int i, int i2, long j) throws RemoteException;

    boolean setBoundGbaServiceOverride(int i, String str) throws RemoteException;

    boolean setBoundImsServiceOverride(int i, boolean z, int[] iArr, String str) throws RemoteException;

    void setCallComposerStatus(int i, int i2) throws RemoteException;

    void setCallForwarding(int i, CallForwardingInfo callForwardingInfo, IIntegerConsumer iIntegerConsumer) throws RemoteException;

    void setCallWaitingStatus(int i, boolean z, IIntegerConsumer iIntegerConsumer) throws RemoteException;

    boolean setCapabilitiesRequestTimeout(int i, long j) throws RemoteException;

    boolean setCarrierSingleRegistrationEnabledOverride(int i, String str) throws RemoteException;

    void setCarrierTestOverride(int i, String str, String str2, String str3, String str4, String str5, String str6, String str7, String str8, String str9) throws RemoteException;

    boolean setCdmaRoamingMode(int i, int i2) throws RemoteException;

    boolean setCdmaSubscriptionMode(int i, int i2) throws RemoteException;

    void setCellInfoListRate(int i) throws RemoteException;

    void setCepEnabled(boolean z) throws RemoteException;

    void setCrossSimCallingEnabled(int i, boolean z) throws RemoteException;

    void setDataActivationState(int i, int i2) throws RemoteException;

    void setDataEnabledForReason(int i, int i2, boolean z, String str) throws RemoteException;

    void setDataRoamingEnabled(int i, boolean z) throws RemoteException;

    void setDeviceSingleRegistrationEnabledOverride(String str) throws RemoteException;

    void setDeviceToDeviceForceEnabled(boolean z) throws RemoteException;

    void setDeviceUceEnabled(boolean z) throws RemoteException;

    int setForbiddenPlmns(int i, int i2, List<String> list, String str, String str2) throws RemoteException;

    boolean setGbaReleaseTimeOverride(int i, int i2) throws RemoteException;

    int setIccLockEnabled(int i, boolean z, String str) throws RemoteException;

    boolean setImsFeatureValidationOverride(int i, String str) throws RemoteException;

    int setImsProvisioningInt(int i, int i2, int i3) throws RemoteException;

    void setImsProvisioningStatusForCapability(int i, int i2, int i3, boolean z) throws RemoteException;

    int setImsProvisioningString(int i, int i2, String str) throws RemoteException;

    void setImsRegistrationState(boolean z) throws RemoteException;

    boolean setLine1NumberForDisplayForSubscriber(int i, String str, String str2) throws RemoteException;

    void setMobileDataPolicyEnabled(int i, int i2, boolean z) throws RemoteException;

    boolean setModemService(String str) throws RemoteException;

    void setMultiSimCarrierRestriction(boolean z) throws RemoteException;

    void setNetworkSelectionModeAutomatic(int i) throws RemoteException;

    boolean setNetworkSelectionModeManual(int i, OperatorInfo operatorInfo, boolean z) throws RemoteException;

    int setNrDualConnectivityState(int i, int i2) throws RemoteException;

    boolean setOperatorBrandOverride(int i, String str) throws RemoteException;

    boolean setRadio(boolean z) throws RemoteException;

    boolean setRadioForSubscriber(int i, boolean z) throws RemoteException;

    boolean setRadioPower(boolean z) throws RemoteException;

    void setRcsClientConfiguration(int i, RcsClientConfiguration rcsClientConfiguration) throws RemoteException;

    void setRcsProvisioningStatusForCapability(int i, int i2, int i3, boolean z) throws RemoteException;

    void setRcsSingleRegistrationTestModeEnabled(boolean z) throws RemoteException;

    void setRemovableEsimAsDefaultEuicc(boolean z, String str) throws RemoteException;

    boolean setRoamingOverride(int i, List<String> list, List<String> list2, List<String> list3, List<String> list4) throws RemoteException;

    void setRttCapabilitySetting(int i, boolean z) throws RemoteException;

    void setSignalStrengthUpdateRequest(int i, SignalStrengthUpdateRequest signalStrengthUpdateRequest, String str) throws RemoteException;

    void setSimPowerStateForSlot(int i, int i2) throws RemoteException;

    void setSimPowerStateForSlotWithCallback(int i, int i2, IIntegerConsumer iIntegerConsumer) throws RemoteException;

    boolean setSimSlotMapping(List<UiccSlotMapping> list) throws RemoteException;

    void setSystemSelectionChannels(List<RadioAccessSpecifier> list, int i, IBooleanConsumer iBooleanConsumer) throws RemoteException;

    int setVoNrEnabled(int i, boolean z) throws RemoteException;

    void setVoWiFiModeSetting(int i, int i2) throws RemoteException;

    void setVoWiFiNonPersistent(int i, boolean z, int i2) throws RemoteException;

    void setVoWiFiRoamingModeSetting(int i, int i2) throws RemoteException;

    void setVoWiFiRoamingSettingEnabled(int i, boolean z) throws RemoteException;

    void setVoWiFiSettingEnabled(int i, boolean z) throws RemoteException;

    void setVoiceActivationState(int i, int i2) throws RemoteException;

    boolean setVoiceMailNumber(int i, String str, String str2) throws RemoteException;

    void setVoiceServiceStateOverride(int i, boolean z, String str) throws RemoteException;

    void setVoicemailRingtoneUri(String str, PhoneAccountHandle phoneAccountHandle, Uri uri) throws RemoteException;

    void setVoicemailVibrationEnabled(String str, PhoneAccountHandle phoneAccountHandle, boolean z) throws RemoteException;

    void setVtSettingEnabled(int i, boolean z) throws RemoteException;

    void shutdownMobileRadios() throws RemoteException;

    void startEmergencyCallbackMode() throws RemoteException;

    void stopNetworkScan(int i, int i2) throws RemoteException;

    boolean supplyPinForSubscriber(int i, String str) throws RemoteException;

    int[] supplyPinReportResultForSubscriber(int i, String str) throws RemoteException;

    boolean supplyPukForSubscriber(int i, String str, String str2) throws RemoteException;

    int[] supplyPukReportResultForSubscriber(int i, String str, String str2) throws RemoteException;

    void switchMultiSimConfig(int i) throws RemoteException;

    @Deprecated
    boolean switchSlots(int[] iArr) throws RemoteException;

    void toggleRadioOnOff() throws RemoteException;

    void toggleRadioOnOffForSubscriber(int i) throws RemoteException;

    void triggerRcsReconfiguration(int i) throws RemoteException;

    void unregisterFeatureProvisioningChangedCallback(int i, IFeatureProvisioningCallback iFeatureProvisioningCallback) throws RemoteException;

    void unregisterImsFeatureCallback(IImsServiceFeatureCallback iImsServiceFeatureCallback) throws RemoteException;

    void unregisterImsProvisioningChangedCallback(int i, IImsConfigCallback iImsConfigCallback) throws RemoteException;

    void unregisterImsRegistrationCallback(int i, IImsRegistrationCallback iImsRegistrationCallback) throws RemoteException;

    void unregisterImsStateCallback(IImsStateCallback iImsStateCallback) throws RemoteException;

    void unregisterMmTelCapabilityCallback(int i, IImsCapabilityCallback iImsCapabilityCallback) throws RemoteException;

    void unregisterRcsProvisioningCallback(int i, IRcsConfigCallback iRcsConfigCallback) throws RemoteException;

    void updateEmergencyNumberListTestMode(int i, EmergencyNumber emergencyNumber) throws RemoteException;

    void updateOtaEmergencyNumberDbFilePath(ParcelFileDescriptor parcelFileDescriptor) throws RemoteException;

    void updateServiceLocation() throws RemoteException;

    void updateServiceLocationWithPackageName(String str) throws RemoteException;

    void uploadCallComposerPicture(int i, String str, String str2, ParcelFileDescriptor parcelFileDescriptor, ResultReceiver resultReceiver) throws RemoteException;

    void userActivity() throws RemoteException;

    /* loaded from: classes4.dex */
    public static class Default implements ITelephony {
        @Override // com.android.internal.telephony.ITelephony
        public void dial(String number) throws RemoteException {
        }

        @Override // com.android.internal.telephony.ITelephony
        public void call(String callingPackage, String number) throws RemoteException {
        }

        @Override // com.android.internal.telephony.ITelephony
        public boolean isRadioOn(String callingPackage) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.telephony.ITelephony
        public boolean isRadioOnWithFeature(String callingPackage, String callingFeatureId) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.telephony.ITelephony
        public boolean isRadioOnForSubscriber(int subId, String callingPackage) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.telephony.ITelephony
        public boolean isRadioOnForSubscriberWithFeature(int subId, String callingPackage, String callingFeatureId) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.telephony.ITelephony
        public void setCallComposerStatus(int subId, int status) throws RemoteException {
        }

        @Override // com.android.internal.telephony.ITelephony
        public int getCallComposerStatus(int subId) throws RemoteException {
            return 0;
        }

        @Override // com.android.internal.telephony.ITelephony
        public boolean supplyPinForSubscriber(int subId, String pin) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.telephony.ITelephony
        public boolean supplyPukForSubscriber(int subId, String puk, String pin) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.telephony.ITelephony
        public int[] supplyPinReportResultForSubscriber(int subId, String pin) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.telephony.ITelephony
        public int[] supplyPukReportResultForSubscriber(int subId, String puk, String pin) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.telephony.ITelephony
        public boolean handlePinMmi(String dialString) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.telephony.ITelephony
        public void handleUssdRequest(int subId, String ussdRequest, ResultReceiver wrappedCallback) throws RemoteException {
        }

        @Override // com.android.internal.telephony.ITelephony
        public boolean handlePinMmiForSubscriber(int subId, String dialString) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.telephony.ITelephony
        public void toggleRadioOnOff() throws RemoteException {
        }

        @Override // com.android.internal.telephony.ITelephony
        public void toggleRadioOnOffForSubscriber(int subId) throws RemoteException {
        }

        @Override // com.android.internal.telephony.ITelephony
        public boolean setRadio(boolean turnOn) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.telephony.ITelephony
        public boolean setRadioForSubscriber(int subId, boolean turnOn) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.telephony.ITelephony
        public boolean setRadioPower(boolean turnOn) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.telephony.ITelephony
        public void updateServiceLocation() throws RemoteException {
        }

        @Override // com.android.internal.telephony.ITelephony
        public void updateServiceLocationWithPackageName(String callingPkg) throws RemoteException {
        }

        @Override // com.android.internal.telephony.ITelephony
        public void enableLocationUpdates() throws RemoteException {
        }

        @Override // com.android.internal.telephony.ITelephony
        public void disableLocationUpdates() throws RemoteException {
        }

        @Override // com.android.internal.telephony.ITelephony
        public boolean enableDataConnectivity(String callingPackage) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.telephony.ITelephony
        public boolean disableDataConnectivity(String callingPackage) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.telephony.ITelephony
        public boolean isDataConnectivityPossible(int subId) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.telephony.ITelephony
        public CellIdentity getCellLocation(String callingPkg, String callingFeatureId) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.telephony.ITelephony
        public String getNetworkCountryIsoForPhone(int phoneId) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.telephony.ITelephony
        public List<NeighboringCellInfo> getNeighboringCellInfo(String callingPkg, String callingFeatureId) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.telephony.ITelephony
        public int getCallState() throws RemoteException {
            return 0;
        }

        @Override // com.android.internal.telephony.ITelephony
        public int getCallStateForSubscription(int subId, String callingPackage, String featureId) throws RemoteException {
            return 0;
        }

        @Override // com.android.internal.telephony.ITelephony
        public int getDataActivity() throws RemoteException {
            return 0;
        }

        @Override // com.android.internal.telephony.ITelephony
        public int getDataActivityForSubId(int subId) throws RemoteException {
            return 0;
        }

        @Override // com.android.internal.telephony.ITelephony
        public int getDataState() throws RemoteException {
            return 0;
        }

        @Override // com.android.internal.telephony.ITelephony
        public int getDataStateForSubId(int subId) throws RemoteException {
            return 0;
        }

        @Override // com.android.internal.telephony.ITelephony
        public int getActivePhoneType() throws RemoteException {
            return 0;
        }

        @Override // com.android.internal.telephony.ITelephony
        public int getActivePhoneTypeForSlot(int slotIndex) throws RemoteException {
            return 0;
        }

        @Override // com.android.internal.telephony.ITelephony
        public int getCdmaEriIconIndex(String callingPackage, String callingFeatureId) throws RemoteException {
            return 0;
        }

        @Override // com.android.internal.telephony.ITelephony
        public int getCdmaEriIconIndexForSubscriber(int subId, String callingPackage, String callingFeatureId) throws RemoteException {
            return 0;
        }

        @Override // com.android.internal.telephony.ITelephony
        public int getCdmaEriIconMode(String callingPackage, String callingFeatureId) throws RemoteException {
            return 0;
        }

        @Override // com.android.internal.telephony.ITelephony
        public int getCdmaEriIconModeForSubscriber(int subId, String callingPackage, String callingFeatureId) throws RemoteException {
            return 0;
        }

        @Override // com.android.internal.telephony.ITelephony
        public String getCdmaEriText(String callingPackage, String callingFeatureId) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.telephony.ITelephony
        public String getCdmaEriTextForSubscriber(int subId, String callingPackage, String callingFeatureId) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.telephony.ITelephony
        public boolean needsOtaServiceProvisioning() throws RemoteException {
            return false;
        }

        @Override // com.android.internal.telephony.ITelephony
        public boolean setVoiceMailNumber(int subId, String alphaTag, String number) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.telephony.ITelephony
        public void setVoiceActivationState(int subId, int activationState) throws RemoteException {
        }

        @Override // com.android.internal.telephony.ITelephony
        public void setDataActivationState(int subId, int activationState) throws RemoteException {
        }

        @Override // com.android.internal.telephony.ITelephony
        public int getVoiceActivationState(int subId, String callingPackage) throws RemoteException {
            return 0;
        }

        @Override // com.android.internal.telephony.ITelephony
        public int getDataActivationState(int subId, String callingPackage) throws RemoteException {
            return 0;
        }

        @Override // com.android.internal.telephony.ITelephony
        public int getVoiceMessageCountForSubscriber(int subId, String callingPackage, String callingFeatureId) throws RemoteException {
            return 0;
        }

        @Override // com.android.internal.telephony.ITelephony
        public boolean isConcurrentVoiceAndDataAllowed(int subId) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.telephony.ITelephony
        public Bundle getVisualVoicemailSettings(String callingPackage, int subId) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.telephony.ITelephony
        public String getVisualVoicemailPackageName(String callingPackage, String callingFeatureId, int subId) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.telephony.ITelephony
        public void enableVisualVoicemailSmsFilter(String callingPackage, int subId, VisualVoicemailSmsFilterSettings settings) throws RemoteException {
        }

        @Override // com.android.internal.telephony.ITelephony
        public void disableVisualVoicemailSmsFilter(String callingPackage, int subId) throws RemoteException {
        }

        @Override // com.android.internal.telephony.ITelephony
        public VisualVoicemailSmsFilterSettings getVisualVoicemailSmsFilterSettings(String callingPackage, int subId) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.telephony.ITelephony
        public VisualVoicemailSmsFilterSettings getActiveVisualVoicemailSmsFilterSettings(int subId) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.telephony.ITelephony
        public void sendVisualVoicemailSmsForSubscriber(String callingPackage, String callingAttributeTag, int subId, String number, int port, String text, PendingIntent sentIntent) throws RemoteException {
        }

        @Override // com.android.internal.telephony.ITelephony
        public void sendDialerSpecialCode(String callingPackageName, String inputCode) throws RemoteException {
        }

        @Override // com.android.internal.telephony.ITelephony
        public int getNetworkTypeForSubscriber(int subId, String callingPackage, String callingFeatureId) throws RemoteException {
            return 0;
        }

        @Override // com.android.internal.telephony.ITelephony
        public int getDataNetworkType(String callingPackage, String callingFeatureId) throws RemoteException {
            return 0;
        }

        @Override // com.android.internal.telephony.ITelephony
        public int getDataNetworkTypeForSubscriber(int subId, String callingPackage, String callingFeatureId) throws RemoteException {
            return 0;
        }

        @Override // com.android.internal.telephony.ITelephony
        public int getVoiceNetworkTypeForSubscriber(int subId, String callingPackage, String callingFeatureId) throws RemoteException {
            return 0;
        }

        @Override // com.android.internal.telephony.ITelephony
        public boolean hasIccCard() throws RemoteException {
            return false;
        }

        @Override // com.android.internal.telephony.ITelephony
        public boolean hasIccCardUsingSlotIndex(int slotIndex) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.telephony.ITelephony
        public int getLteOnCdmaMode(String callingPackage, String callingFeatureId) throws RemoteException {
            return 0;
        }

        @Override // com.android.internal.telephony.ITelephony
        public int getLteOnCdmaModeForSubscriber(int subId, String callingPackage, String callingFeatureId) throws RemoteException {
            return 0;
        }

        @Override // com.android.internal.telephony.ITelephony
        public List<CellInfo> getAllCellInfo(String callingPkg, String callingFeatureId) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.telephony.ITelephony
        public void requestCellInfoUpdate(int subId, ICellInfoCallback cb, String callingPkg, String callingFeatureId) throws RemoteException {
        }

        @Override // com.android.internal.telephony.ITelephony
        public void requestCellInfoUpdateWithWorkSource(int subId, ICellInfoCallback cb, String callingPkg, String callingFeatureId, WorkSource ws) throws RemoteException {
        }

        @Override // com.android.internal.telephony.ITelephony
        public void setCellInfoListRate(int rateInMillis) throws RemoteException {
        }

        @Override // com.android.internal.telephony.ITelephony
        public IccOpenLogicalChannelResponse iccOpenLogicalChannel(IccLogicalChannelRequest request) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.telephony.ITelephony
        public boolean iccCloseLogicalChannel(IccLogicalChannelRequest request) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.telephony.ITelephony
        public String iccTransmitApduLogicalChannelByPort(int slotIndex, int portIndex, int channel, int cla, int instruction, int p1, int p2, int p3, String data) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.telephony.ITelephony
        public String iccTransmitApduLogicalChannel(int subId, int channel, int cla, int instruction, int p1, int p2, int p3, String data) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.telephony.ITelephony
        public String iccTransmitApduBasicChannelByPort(int slotIndex, int portIndex, String callingPackage, int cla, int instruction, int p1, int p2, int p3, String data) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.telephony.ITelephony
        public String iccTransmitApduBasicChannel(int subId, String callingPackage, int cla, int instruction, int p1, int p2, int p3, String data) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.telephony.ITelephony
        public byte[] iccExchangeSimIO(int subId, int fileID, int command, int p1, int p2, int p3, String filePath) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.telephony.ITelephony
        public String sendEnvelopeWithStatus(int subId, String content) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.telephony.ITelephony
        public String nvReadItem(int itemID) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.telephony.ITelephony
        public boolean nvWriteItem(int itemID, String itemValue) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.telephony.ITelephony
        public boolean nvWriteCdmaPrl(byte[] preferredRoamingList) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.telephony.ITelephony
        public boolean resetModemConfig(int slotIndex) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.telephony.ITelephony
        public boolean rebootModem(int slotIndex) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.telephony.ITelephony
        public int getAllowedNetworkTypesBitmask(int subId) throws RemoteException {
            return 0;
        }

        @Override // com.android.internal.telephony.ITelephony
        public boolean isTetheringApnRequiredForSubscriber(int subId) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.telephony.ITelephony
        public void enableIms(int slotId) throws RemoteException {
        }

        @Override // com.android.internal.telephony.ITelephony
        public void disableIms(int slotId) throws RemoteException {
        }

        @Override // com.android.internal.telephony.ITelephony
        public void resetIms(int slotIndex) throws RemoteException {
        }

        @Override // com.android.internal.telephony.ITelephony
        public void registerMmTelFeatureCallback(int slotId, IImsServiceFeatureCallback callback) throws RemoteException {
        }

        @Override // com.android.internal.telephony.ITelephony
        public void unregisterImsFeatureCallback(IImsServiceFeatureCallback callback) throws RemoteException {
        }

        @Override // com.android.internal.telephony.ITelephony
        public IImsRegistration getImsRegistration(int slotId, int feature) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.telephony.ITelephony
        public IImsConfig getImsConfig(int slotId, int feature) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.telephony.ITelephony
        public boolean setBoundImsServiceOverride(int slotIndex, boolean isCarrierService, int[] featureTypes, String packageName) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.telephony.ITelephony
        public boolean clearCarrierImsServiceOverride(int slotIndex) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.telephony.ITelephony
        public String getBoundImsServicePackage(int slotIndex, boolean isCarrierImsService, int featureType) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.telephony.ITelephony
        public void getImsMmTelFeatureState(int subId, IIntegerConsumer callback) throws RemoteException {
        }

        @Override // com.android.internal.telephony.ITelephony
        public void setNetworkSelectionModeAutomatic(int subId) throws RemoteException {
        }

        @Override // com.android.internal.telephony.ITelephony
        public CellNetworkScanResult getCellNetworkScanResults(int subId, String callingPackage, String callingFeatureId) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.telephony.ITelephony
        public int requestNetworkScan(int subId, boolean renounceFineLocationAccess, NetworkScanRequest request, Messenger messenger, IBinder binder, String callingPackage, String callingFeatureId) throws RemoteException {
            return 0;
        }

        @Override // com.android.internal.telephony.ITelephony
        public void stopNetworkScan(int subId, int scanId) throws RemoteException {
        }

        @Override // com.android.internal.telephony.ITelephony
        public boolean setNetworkSelectionModeManual(int subId, OperatorInfo operatorInfo, boolean persisSelection) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.telephony.ITelephony
        public long getAllowedNetworkTypesForReason(int subId, int reason) throws RemoteException {
            return 0L;
        }

        @Override // com.android.internal.telephony.ITelephony
        public boolean setAllowedNetworkTypesForReason(int subId, int reason, long allowedNetworkTypes) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.telephony.ITelephony
        public boolean getDataEnabled(int subId) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.telephony.ITelephony
        public boolean isUserDataEnabled(int subId) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.telephony.ITelephony
        public boolean isDataEnabled(int subId) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.telephony.ITelephony
        public void setDataEnabledForReason(int subId, int reason, boolean enable, String callingPackage) throws RemoteException {
        }

        @Override // com.android.internal.telephony.ITelephony
        public boolean isDataEnabledForReason(int subId, int reason) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.telephony.ITelephony
        public boolean isManualNetworkSelectionAllowed(int subId) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.telephony.ITelephony
        public void setImsRegistrationState(boolean registered) throws RemoteException {
        }

        @Override // com.android.internal.telephony.ITelephony
        public String getCdmaMdn(int subId) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.telephony.ITelephony
        public String getCdmaMin(int subId) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.telephony.ITelephony
        public void requestNumberVerification(PhoneNumberRange range, long timeoutMillis, INumberVerificationCallback callback, String callingPackage) throws RemoteException {
        }

        @Override // com.android.internal.telephony.ITelephony
        public int getCarrierPrivilegeStatus(int subId) throws RemoteException {
            return 0;
        }

        @Override // com.android.internal.telephony.ITelephony
        public int getCarrierPrivilegeStatusForUid(int subId, int uid) throws RemoteException {
            return 0;
        }

        @Override // com.android.internal.telephony.ITelephony
        public int checkCarrierPrivilegesForPackage(int subId, String pkgName) throws RemoteException {
            return 0;
        }

        @Override // com.android.internal.telephony.ITelephony
        public int checkCarrierPrivilegesForPackageAnyPhone(String pkgName) throws RemoteException {
            return 0;
        }

        @Override // com.android.internal.telephony.ITelephony
        public List<String> getCarrierPackageNamesForIntentAndPhone(Intent intent, int phoneId) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.telephony.ITelephony
        public boolean setLine1NumberForDisplayForSubscriber(int subId, String alphaTag, String number) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.telephony.ITelephony
        public String getLine1NumberForDisplay(int subId, String callingPackage, String callingFeatureId) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.telephony.ITelephony
        public String getLine1AlphaTagForDisplay(int subId, String callingPackage, String callingFeatureId) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.telephony.ITelephony
        public String[] getMergedSubscriberIds(int subId, String callingPackage, String callingFeatureId) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.telephony.ITelephony
        public String[] getMergedImsisFromGroup(int subId, String callingPackage) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.telephony.ITelephony
        public boolean setOperatorBrandOverride(int subId, String brand) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.telephony.ITelephony
        public boolean setRoamingOverride(int subId, List<String> gsmRoamingList, List<String> gsmNonRoamingList, List<String> cdmaRoamingList, List<String> cdmaNonRoamingList) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.telephony.ITelephony
        public int invokeOemRilRequestRaw(byte[] oemReq, byte[] oemResp) throws RemoteException {
            return 0;
        }

        @Override // com.android.internal.telephony.ITelephony
        public boolean needMobileRadioShutdown() throws RemoteException {
            return false;
        }

        @Override // com.android.internal.telephony.ITelephony
        public void shutdownMobileRadios() throws RemoteException {
        }

        @Override // com.android.internal.telephony.ITelephony
        public int getRadioAccessFamily(int phoneId, String callingPackage) throws RemoteException {
            return 0;
        }

        @Override // com.android.internal.telephony.ITelephony
        public void uploadCallComposerPicture(int subscriptionId, String callingPackage, String contentType, ParcelFileDescriptor fd, ResultReceiver callback) throws RemoteException {
        }

        @Override // com.android.internal.telephony.ITelephony
        public void enableVideoCalling(boolean enable) throws RemoteException {
        }

        @Override // com.android.internal.telephony.ITelephony
        public boolean isVideoCallingEnabled(String callingPackage, String callingFeatureId) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.telephony.ITelephony
        public boolean canChangeDtmfToneLength(int subId, String callingPackage, String callingFeatureId) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.telephony.ITelephony
        public boolean isWorldPhone(int subId, String callingPackage, String callingFeatureId) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.telephony.ITelephony
        public boolean isTtyModeSupported() throws RemoteException {
            return false;
        }

        @Override // com.android.internal.telephony.ITelephony
        public boolean isRttSupported(int subscriptionId) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.telephony.ITelephony
        public boolean isHearingAidCompatibilitySupported() throws RemoteException {
            return false;
        }

        @Override // com.android.internal.telephony.ITelephony
        public boolean isImsRegistered(int subId) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.telephony.ITelephony
        public boolean isWifiCallingAvailable(int subId) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.telephony.ITelephony
        public boolean isVideoTelephonyAvailable(int subId) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.telephony.ITelephony
        public int getImsRegTechnologyForMmTel(int subId) throws RemoteException {
            return 0;
        }

        @Override // com.android.internal.telephony.ITelephony
        public String getDeviceId(String callingPackage) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.telephony.ITelephony
        public String getDeviceIdWithFeature(String callingPackage, String callingFeatureId) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.telephony.ITelephony
        public String getImeiForSlot(int slotIndex, String callingPackage, String callingFeatureId) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.telephony.ITelephony
        public String getTypeAllocationCodeForSlot(int slotIndex) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.telephony.ITelephony
        public String getMeidForSlot(int slotIndex, String callingPackage, String callingFeatureId) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.telephony.ITelephony
        public String getManufacturerCodeForSlot(int slotIndex) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.telephony.ITelephony
        public String getDeviceSoftwareVersionForSlot(int slotIndex, String callingPackage, String callingFeatureId) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.telephony.ITelephony
        public int getSubIdForPhoneAccountHandle(PhoneAccountHandle phoneAccountHandle, String callingPackage, String callingFeatureId) throws RemoteException {
            return 0;
        }

        @Override // com.android.internal.telephony.ITelephony
        public PhoneAccountHandle getPhoneAccountHandleForSubscriptionId(int subscriptionId) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.telephony.ITelephony
        public void factoryReset(int subId, String callingPackage) throws RemoteException {
        }

        @Override // com.android.internal.telephony.ITelephony
        public String getSimLocaleForSubscriber(int subId) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.telephony.ITelephony
        public void requestModemActivityInfo(ResultReceiver result) throws RemoteException {
        }

        @Override // com.android.internal.telephony.ITelephony
        public ServiceState getServiceStateForSubscriber(int subId, boolean renounceFineLocationAccess, boolean renounceCoarseLocationAccess, String callingPackage, String callingFeatureId) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.telephony.ITelephony
        public Uri getVoicemailRingtoneUri(PhoneAccountHandle accountHandle) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.telephony.ITelephony
        public void setVoicemailRingtoneUri(String callingPackage, PhoneAccountHandle phoneAccountHandle, Uri uri) throws RemoteException {
        }

        @Override // com.android.internal.telephony.ITelephony
        public boolean isVoicemailVibrationEnabled(PhoneAccountHandle accountHandle) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.telephony.ITelephony
        public void setVoicemailVibrationEnabled(String callingPackage, PhoneAccountHandle phoneAccountHandle, boolean enabled) throws RemoteException {
        }

        @Override // com.android.internal.telephony.ITelephony
        public List<String> getPackagesWithCarrierPrivileges(int phoneId) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.telephony.ITelephony
        public List<String> getPackagesWithCarrierPrivilegesForAllPhones() throws RemoteException {
            return null;
        }

        @Override // com.android.internal.telephony.ITelephony
        public String getAidForAppType(int subId, int appType) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.telephony.ITelephony
        public String getEsn(int subId) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.telephony.ITelephony
        public String getCdmaPrlVersion(int subId) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.telephony.ITelephony
        public List<TelephonyHistogram> getTelephonyHistograms() throws RemoteException {
            return null;
        }

        @Override // com.android.internal.telephony.ITelephony
        public int setAllowedCarriers(CarrierRestrictionRules carrierRestrictionRules) throws RemoteException {
            return 0;
        }

        @Override // com.android.internal.telephony.ITelephony
        public CarrierRestrictionRules getAllowedCarriers() throws RemoteException {
            return null;
        }

        @Override // com.android.internal.telephony.ITelephony
        public int getSubscriptionCarrierId(int subId) throws RemoteException {
            return 0;
        }

        @Override // com.android.internal.telephony.ITelephony
        public String getSubscriptionCarrierName(int subId) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.telephony.ITelephony
        public int getSubscriptionSpecificCarrierId(int subId) throws RemoteException {
            return 0;
        }

        @Override // com.android.internal.telephony.ITelephony
        public String getSubscriptionSpecificCarrierName(int subId) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.telephony.ITelephony
        public int getCarrierIdFromMccMnc(int slotIndex, String mccmnc, boolean isSubscriptionMccMnc) throws RemoteException {
            return 0;
        }

        @Override // com.android.internal.telephony.ITelephony
        public void carrierActionSetRadioEnabled(int subId, boolean enabled) throws RemoteException {
        }

        @Override // com.android.internal.telephony.ITelephony
        public void carrierActionReportDefaultNetworkStatus(int subId, boolean report) throws RemoteException {
        }

        @Override // com.android.internal.telephony.ITelephony
        public void carrierActionResetAll(int subId) throws RemoteException {
        }

        @Override // com.android.internal.telephony.ITelephony
        public void getCallForwarding(int subId, int callForwardingReason, ICallForwardingInfoCallback callback) throws RemoteException {
        }

        @Override // com.android.internal.telephony.ITelephony
        public void setCallForwarding(int subId, CallForwardingInfo callForwardingInfo, IIntegerConsumer callback) throws RemoteException {
        }

        @Override // com.android.internal.telephony.ITelephony
        public void getCallWaitingStatus(int subId, IIntegerConsumer callback) throws RemoteException {
        }

        @Override // com.android.internal.telephony.ITelephony
        public void setCallWaitingStatus(int subId, boolean enabled, IIntegerConsumer callback) throws RemoteException {
        }

        @Override // com.android.internal.telephony.ITelephony
        public List<ClientRequestStats> getClientRequestStats(String callingPackage, String callingFeatureId, int subid) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.telephony.ITelephony
        public void setSimPowerStateForSlot(int slotIndex, int state) throws RemoteException {
        }

        @Override // com.android.internal.telephony.ITelephony
        public void setSimPowerStateForSlotWithCallback(int slotIndex, int state, IIntegerConsumer callback) throws RemoteException {
        }

        @Override // com.android.internal.telephony.ITelephony
        public String[] getForbiddenPlmns(int subId, int appType, String callingPackage, String callingFeatureId) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.telephony.ITelephony
        public int setForbiddenPlmns(int subId, int appType, List<String> fplmns, String callingPackage, String callingFeatureId) throws RemoteException {
            return 0;
        }

        @Override // com.android.internal.telephony.ITelephony
        public boolean getEmergencyCallbackMode(int subId) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.telephony.ITelephony
        public SignalStrength getSignalStrength(int subId) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.telephony.ITelephony
        public int getCardIdForDefaultEuicc(int subId, String callingPackage) throws RemoteException {
            return 0;
        }

        @Override // com.android.internal.telephony.ITelephony
        public List<UiccCardInfo> getUiccCardsInfo(String callingPackage) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.telephony.ITelephony
        public UiccSlotInfo[] getUiccSlotsInfo(String callingPackage) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.telephony.ITelephony
        public boolean switchSlots(int[] physicalSlots) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.telephony.ITelephony
        public boolean setSimSlotMapping(List<UiccSlotMapping> slotMapping) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.telephony.ITelephony
        public boolean isDataRoamingEnabled(int subId) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.telephony.ITelephony
        public void setDataRoamingEnabled(int subId, boolean isEnabled) throws RemoteException {
        }

        @Override // com.android.internal.telephony.ITelephony
        public int getCdmaRoamingMode(int subId) throws RemoteException {
            return 0;
        }

        @Override // com.android.internal.telephony.ITelephony
        public boolean setCdmaRoamingMode(int subId, int mode) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.telephony.ITelephony
        public int getCdmaSubscriptionMode(int subId) throws RemoteException {
            return 0;
        }

        @Override // com.android.internal.telephony.ITelephony
        public boolean setCdmaSubscriptionMode(int subId, int mode) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.telephony.ITelephony
        public void setCarrierTestOverride(int subId, String mccmnc, String imsi, String iccid, String gid1, String gid2, String plmn, String spn, String carrierPrivilegeRules, String apn) throws RemoteException {
        }

        @Override // com.android.internal.telephony.ITelephony
        public int getCarrierIdListVersion(int subId) throws RemoteException {
            return 0;
        }

        @Override // com.android.internal.telephony.ITelephony
        public void refreshUiccProfile(int subId) throws RemoteException {
        }

        @Override // com.android.internal.telephony.ITelephony
        public int getNumberOfModemsWithSimultaneousDataConnections(int subId, String callingPackage, String callingFeatureId) throws RemoteException {
            return 0;
        }

        @Override // com.android.internal.telephony.ITelephony
        public int getNetworkSelectionMode(int subId) throws RemoteException {
            return 0;
        }

        @Override // com.android.internal.telephony.ITelephony
        public boolean isInEmergencySmsMode() throws RemoteException {
            return false;
        }

        @Override // com.android.internal.telephony.ITelephony
        public int getRadioPowerState(int slotIndex, String callingPackage, String callingFeatureId) throws RemoteException {
            return 0;
        }

        @Override // com.android.internal.telephony.ITelephony
        public void registerImsRegistrationCallback(int subId, IImsRegistrationCallback c) throws RemoteException {
        }

        @Override // com.android.internal.telephony.ITelephony
        public void unregisterImsRegistrationCallback(int subId, IImsRegistrationCallback c) throws RemoteException {
        }

        @Override // com.android.internal.telephony.ITelephony
        public void getImsMmTelRegistrationState(int subId, IIntegerConsumer consumer) throws RemoteException {
        }

        @Override // com.android.internal.telephony.ITelephony
        public void getImsMmTelRegistrationTransportType(int subId, IIntegerConsumer consumer) throws RemoteException {
        }

        @Override // com.android.internal.telephony.ITelephony
        public void registerMmTelCapabilityCallback(int subId, IImsCapabilityCallback c) throws RemoteException {
        }

        @Override // com.android.internal.telephony.ITelephony
        public void unregisterMmTelCapabilityCallback(int subId, IImsCapabilityCallback c) throws RemoteException {
        }

        @Override // com.android.internal.telephony.ITelephony
        public boolean isCapable(int subId, int capability, int regTech) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.telephony.ITelephony
        public boolean isAvailable(int subId, int capability, int regTech) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.telephony.ITelephony
        public void isMmTelCapabilitySupported(int subId, IIntegerConsumer callback, int capability, int transportType) throws RemoteException {
        }

        @Override // com.android.internal.telephony.ITelephony
        public boolean isAdvancedCallingSettingEnabled(int subId) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.telephony.ITelephony
        public void setAdvancedCallingSettingEnabled(int subId, boolean isEnabled) throws RemoteException {
        }

        @Override // com.android.internal.telephony.ITelephony
        public boolean isVtSettingEnabled(int subId) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.telephony.ITelephony
        public void setVtSettingEnabled(int subId, boolean isEnabled) throws RemoteException {
        }

        @Override // com.android.internal.telephony.ITelephony
        public boolean isVoWiFiSettingEnabled(int subId) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.telephony.ITelephony
        public void setVoWiFiSettingEnabled(int subId, boolean isEnabled) throws RemoteException {
        }

        @Override // com.android.internal.telephony.ITelephony
        public boolean isCrossSimCallingEnabledByUser(int subId) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.telephony.ITelephony
        public void setCrossSimCallingEnabled(int subId, boolean isEnabled) throws RemoteException {
        }

        @Override // com.android.internal.telephony.ITelephony
        public boolean isVoWiFiRoamingSettingEnabled(int subId) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.telephony.ITelephony
        public void setVoWiFiRoamingSettingEnabled(int subId, boolean isEnabled) throws RemoteException {
        }

        @Override // com.android.internal.telephony.ITelephony
        public void setVoWiFiNonPersistent(int subId, boolean isCapable, int mode) throws RemoteException {
        }

        @Override // com.android.internal.telephony.ITelephony
        public int getVoWiFiModeSetting(int subId) throws RemoteException {
            return 0;
        }

        @Override // com.android.internal.telephony.ITelephony
        public void setVoWiFiModeSetting(int subId, int mode) throws RemoteException {
        }

        @Override // com.android.internal.telephony.ITelephony
        public int getVoWiFiRoamingModeSetting(int subId) throws RemoteException {
            return 0;
        }

        @Override // com.android.internal.telephony.ITelephony
        public void setVoWiFiRoamingModeSetting(int subId, int mode) throws RemoteException {
        }

        @Override // com.android.internal.telephony.ITelephony
        public void setRttCapabilitySetting(int subId, boolean isEnabled) throws RemoteException {
        }

        @Override // com.android.internal.telephony.ITelephony
        public boolean isTtyOverVolteEnabled(int subId) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.telephony.ITelephony
        public Map getEmergencyNumberList(String callingPackage, String callingFeatureId) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.telephony.ITelephony
        public boolean isEmergencyNumber(String number, boolean exactMatch) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.telephony.ITelephony
        public List<String> getCertsFromCarrierPrivilegeAccessRules(int subId) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.telephony.ITelephony
        public void registerImsProvisioningChangedCallback(int subId, IImsConfigCallback callback) throws RemoteException {
        }

        @Override // com.android.internal.telephony.ITelephony
        public void unregisterImsProvisioningChangedCallback(int subId, IImsConfigCallback callback) throws RemoteException {
        }

        @Override // com.android.internal.telephony.ITelephony
        public void registerFeatureProvisioningChangedCallback(int subId, IFeatureProvisioningCallback callback) throws RemoteException {
        }

        @Override // com.android.internal.telephony.ITelephony
        public void unregisterFeatureProvisioningChangedCallback(int subId, IFeatureProvisioningCallback callback) throws RemoteException {
        }

        @Override // com.android.internal.telephony.ITelephony
        public void setImsProvisioningStatusForCapability(int subId, int capability, int tech, boolean isProvisioned) throws RemoteException {
        }

        @Override // com.android.internal.telephony.ITelephony
        public boolean getImsProvisioningStatusForCapability(int subId, int capability, int tech) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.telephony.ITelephony
        public boolean getRcsProvisioningStatusForCapability(int subId, int capability, int tech) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.telephony.ITelephony
        public void setRcsProvisioningStatusForCapability(int subId, int capability, int tech, boolean isProvisioned) throws RemoteException {
        }

        @Override // com.android.internal.telephony.ITelephony
        public int getImsProvisioningInt(int subId, int key) throws RemoteException {
            return 0;
        }

        @Override // com.android.internal.telephony.ITelephony
        public String getImsProvisioningString(int subId, int key) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.telephony.ITelephony
        public int setImsProvisioningInt(int subId, int key, int value) throws RemoteException {
            return 0;
        }

        @Override // com.android.internal.telephony.ITelephony
        public int setImsProvisioningString(int subId, int key, String value) throws RemoteException {
            return 0;
        }

        @Override // com.android.internal.telephony.ITelephony
        public void startEmergencyCallbackMode() throws RemoteException {
        }

        @Override // com.android.internal.telephony.ITelephony
        public void updateEmergencyNumberListTestMode(int action, EmergencyNumber num) throws RemoteException {
        }

        @Override // com.android.internal.telephony.ITelephony
        public List<String> getEmergencyNumberListTestMode() throws RemoteException {
            return null;
        }

        @Override // com.android.internal.telephony.ITelephony
        public int getEmergencyNumberDbVersion(int subId) throws RemoteException {
            return 0;
        }

        @Override // com.android.internal.telephony.ITelephony
        public void notifyOtaEmergencyNumberDbInstalled() throws RemoteException {
        }

        @Override // com.android.internal.telephony.ITelephony
        public void updateOtaEmergencyNumberDbFilePath(ParcelFileDescriptor otaParcelFileDescriptor) throws RemoteException {
        }

        @Override // com.android.internal.telephony.ITelephony
        public void resetOtaEmergencyNumberDbFilePath() throws RemoteException {
        }

        @Override // com.android.internal.telephony.ITelephony
        public boolean enableModemForSlot(int slotIndex, boolean enable) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.telephony.ITelephony
        public void setMultiSimCarrierRestriction(boolean isMultiSimCarrierRestricted) throws RemoteException {
        }

        @Override // com.android.internal.telephony.ITelephony
        public int isMultiSimSupported(String callingPackage, String callingFeatureId) throws RemoteException {
            return 0;
        }

        @Override // com.android.internal.telephony.ITelephony
        public void switchMultiSimConfig(int numOfSims) throws RemoteException {
        }

        @Override // com.android.internal.telephony.ITelephony
        public boolean doesSwitchMultiSimConfigTriggerReboot(int subId, String callingPackage, String callingFeatureId) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.telephony.ITelephony
        public List<UiccSlotMapping> getSlotsMapping(String callingPackage) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.telephony.ITelephony
        public int getRadioHalVersion() throws RemoteException {
            return 0;
        }

        @Override // com.android.internal.telephony.ITelephony
        public String getCurrentPackageName() throws RemoteException {
            return null;
        }

        @Override // com.android.internal.telephony.ITelephony
        public boolean isApplicationOnUicc(int subId, int appType) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.telephony.ITelephony
        public boolean isModemEnabledForSlot(int slotIndex, String callingPackage, String callingFeatureId) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.telephony.ITelephony
        public boolean isDataEnabledForApn(int apnType, int subId, String callingPackage) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.telephony.ITelephony
        public boolean isApnMetered(int apnType, int subId) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.telephony.ITelephony
        public void setSystemSelectionChannels(List<RadioAccessSpecifier> specifiers, int subId, IBooleanConsumer resultCallback) throws RemoteException {
        }

        @Override // com.android.internal.telephony.ITelephony
        public List<RadioAccessSpecifier> getSystemSelectionChannels(int subId) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.telephony.ITelephony
        public boolean isMvnoMatched(int slotIndex, int mvnoType, String mvnoMatchData) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.telephony.ITelephony
        public void enqueueSmsPickResult(String callingPackage, String callingAttributeTag, IIntegerConsumer subIdResult) throws RemoteException {
        }

        @Override // com.android.internal.telephony.ITelephony
        public String getMmsUserAgent(int subId) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.telephony.ITelephony
        public String getMmsUAProfUrl(int subId) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.telephony.ITelephony
        public void setMobileDataPolicyEnabled(int subscriptionId, int policy, boolean enabled) throws RemoteException {
        }

        @Override // com.android.internal.telephony.ITelephony
        public boolean isMobileDataPolicyEnabled(int subscriptionId, int policy) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.telephony.ITelephony
        public void setCepEnabled(boolean isCepEnabled) throws RemoteException {
        }

        @Override // com.android.internal.telephony.ITelephony
        public void notifyRcsAutoConfigurationReceived(int subId, byte[] config, boolean isCompressed) throws RemoteException {
        }

        @Override // com.android.internal.telephony.ITelephony
        public boolean isIccLockEnabled(int subId) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.telephony.ITelephony
        public int setIccLockEnabled(int subId, boolean enabled, String password) throws RemoteException {
            return 0;
        }

        @Override // com.android.internal.telephony.ITelephony
        public int changeIccLockPassword(int subId, String oldPassword, String newPassword) throws RemoteException {
            return 0;
        }

        @Override // com.android.internal.telephony.ITelephony
        public void requestUserActivityNotification() throws RemoteException {
        }

        @Override // com.android.internal.telephony.ITelephony
        public void userActivity() throws RemoteException {
        }

        @Override // com.android.internal.telephony.ITelephony
        public String getManualNetworkSelectionPlmn(int subId) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.telephony.ITelephony
        public boolean canConnectTo5GInDsdsMode() throws RemoteException {
            return false;
        }

        @Override // com.android.internal.telephony.ITelephony
        public List<String> getEquivalentHomePlmns(int subId, String callingPackage, String callingFeatureId) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.telephony.ITelephony
        public int setVoNrEnabled(int subId, boolean enabled) throws RemoteException {
            return 0;
        }

        @Override // com.android.internal.telephony.ITelephony
        public boolean isVoNrEnabled(int subId) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.telephony.ITelephony
        public int setNrDualConnectivityState(int subId, int nrDualConnectivityState) throws RemoteException {
            return 0;
        }

        @Override // com.android.internal.telephony.ITelephony
        public boolean isNrDualConnectivityEnabled(int subId) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.telephony.ITelephony
        public boolean isRadioInterfaceCapabilitySupported(String capability) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.telephony.ITelephony
        public int sendThermalMitigationRequest(int subId, ThermalMitigationRequest thermalMitigationRequest, String callingPackage) throws RemoteException {
            return 0;
        }

        @Override // com.android.internal.telephony.ITelephony
        public void bootstrapAuthenticationRequest(int subId, int appType, Uri nafUrl, UaSecurityProtocolIdentifier securityProtocol, boolean forceBootStrapping, IBootstrapAuthenticationCallback callback) throws RemoteException {
        }

        @Override // com.android.internal.telephony.ITelephony
        public boolean setBoundGbaServiceOverride(int subId, String packageName) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.telephony.ITelephony
        public String getBoundGbaService(int subId) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.telephony.ITelephony
        public boolean setGbaReleaseTimeOverride(int subId, int interval) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.telephony.ITelephony
        public int getGbaReleaseTime(int subId) throws RemoteException {
            return 0;
        }

        @Override // com.android.internal.telephony.ITelephony
        public void setRcsClientConfiguration(int subId, RcsClientConfiguration rcc) throws RemoteException {
        }

        @Override // com.android.internal.telephony.ITelephony
        public boolean isRcsVolteSingleRegistrationCapable(int subId) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.telephony.ITelephony
        public void registerRcsProvisioningCallback(int subId, IRcsConfigCallback callback) throws RemoteException {
        }

        @Override // com.android.internal.telephony.ITelephony
        public void unregisterRcsProvisioningCallback(int subId, IRcsConfigCallback callback) throws RemoteException {
        }

        @Override // com.android.internal.telephony.ITelephony
        public void triggerRcsReconfiguration(int subId) throws RemoteException {
        }

        @Override // com.android.internal.telephony.ITelephony
        public void setRcsSingleRegistrationTestModeEnabled(boolean enabled) throws RemoteException {
        }

        @Override // com.android.internal.telephony.ITelephony
        public boolean getRcsSingleRegistrationTestModeEnabled() throws RemoteException {
            return false;
        }

        @Override // com.android.internal.telephony.ITelephony
        public void setDeviceSingleRegistrationEnabledOverride(String enabled) throws RemoteException {
        }

        @Override // com.android.internal.telephony.ITelephony
        public boolean getDeviceSingleRegistrationEnabled() throws RemoteException {
            return false;
        }

        @Override // com.android.internal.telephony.ITelephony
        public boolean setCarrierSingleRegistrationEnabledOverride(int subId, String enabled) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.telephony.ITelephony
        public void sendDeviceToDeviceMessage(int message, int value) throws RemoteException {
        }

        @Override // com.android.internal.telephony.ITelephony
        public void setActiveDeviceToDeviceTransport(String transport) throws RemoteException {
        }

        @Override // com.android.internal.telephony.ITelephony
        public void setDeviceToDeviceForceEnabled(boolean isForceEnabled) throws RemoteException {
        }

        @Override // com.android.internal.telephony.ITelephony
        public boolean getCarrierSingleRegistrationEnabled(int subId) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.telephony.ITelephony
        public boolean setImsFeatureValidationOverride(int subId, String enabled) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.telephony.ITelephony
        public boolean getImsFeatureValidationOverride(int subId) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.telephony.ITelephony
        public String getMobileProvisioningUrl() throws RemoteException {
            return null;
        }

        @Override // com.android.internal.telephony.ITelephony
        public int removeContactFromEab(int subId, String contacts) throws RemoteException {
            return 0;
        }

        @Override // com.android.internal.telephony.ITelephony
        public String getContactFromEab(String contact) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.telephony.ITelephony
        public String getCapabilityFromEab(String contact) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.telephony.ITelephony
        public boolean getDeviceUceEnabled() throws RemoteException {
            return false;
        }

        @Override // com.android.internal.telephony.ITelephony
        public void setDeviceUceEnabled(boolean isEnabled) throws RemoteException {
        }

        @Override // com.android.internal.telephony.ITelephony
        public RcsContactUceCapability addUceRegistrationOverrideShell(int subId, List<String> featureTags) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.telephony.ITelephony
        public RcsContactUceCapability removeUceRegistrationOverrideShell(int subId, List<String> featureTags) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.telephony.ITelephony
        public RcsContactUceCapability clearUceRegistrationOverrideShell(int subId) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.telephony.ITelephony
        public RcsContactUceCapability getLatestRcsContactUceCapabilityShell(int subId) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.telephony.ITelephony
        public String getLastUcePidfXmlShell(int subId) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.telephony.ITelephony
        public boolean removeUceRequestDisallowedStatus(int subId) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.telephony.ITelephony
        public boolean setCapabilitiesRequestTimeout(int subId, long timeoutAfterMs) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.telephony.ITelephony
        public void setSignalStrengthUpdateRequest(int subId, SignalStrengthUpdateRequest request, String callingPackage) throws RemoteException {
        }

        @Override // com.android.internal.telephony.ITelephony
        public void clearSignalStrengthUpdateRequest(int subId, SignalStrengthUpdateRequest request, String callingPackage) throws RemoteException {
        }

        @Override // com.android.internal.telephony.ITelephony
        public PhoneCapability getPhoneCapability() throws RemoteException {
            return null;
        }

        @Override // com.android.internal.telephony.ITelephony
        public int prepareForUnattendedReboot() throws RemoteException {
            return 0;
        }

        @Override // com.android.internal.telephony.ITelephony
        public void getSlicingConfig(ResultReceiver callback) throws RemoteException {
        }

        @Override // com.android.internal.telephony.ITelephony
        public void registerImsStateCallback(int subId, int feature, IImsStateCallback cb, String callingPackage) throws RemoteException {
        }

        @Override // com.android.internal.telephony.ITelephony
        public void unregisterImsStateCallback(IImsStateCallback cb) throws RemoteException {
        }

        @Override // com.android.internal.telephony.ITelephony
        public CellIdentity getLastKnownCellIdentity(int subId, String callingPackage, String callingFeatureId) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.telephony.ITelephony
        public boolean isUsingNewDataStack() throws RemoteException {
            return false;
        }

        @Override // com.android.internal.telephony.ITelephony
        public boolean setModemService(String serviceName) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.telephony.ITelephony
        public String getModemService() throws RemoteException {
            return null;
        }

        @Override // com.android.internal.telephony.ITelephony
        public boolean isProvisioningRequiredForCapability(int subId, int capability, int tech) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.telephony.ITelephony
        public boolean isRcsProvisioningRequiredForCapability(int subId, int capability, int tech) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.telephony.ITelephony
        public void setVoiceServiceStateOverride(int subId, boolean hasService, String callingPackage) throws RemoteException {
        }

        @Override // com.android.internal.telephony.ITelephony
        public String getCarrierServicePackageNameForLogicalSlot(int logicalSlotIndex) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.telephony.ITelephony
        public void setRemovableEsimAsDefaultEuicc(boolean isDefault, String callingPackage) throws RemoteException {
        }

        @Override // com.android.internal.telephony.ITelephony
        public boolean isRemovableEsimDefaultEuicc(String callingPackage) throws RemoteException {
            return false;
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes4.dex */
    public static abstract class Stub extends Binder implements ITelephony {
        public static final String DESCRIPTOR = "com.android.internal.telephony.ITelephony";
        static final int TRANSACTION_addUceRegistrationOverrideShell = 317;
        static final int TRANSACTION_bootstrapAuthenticationRequest = 290;
        static final int TRANSACTION_call = 2;
        static final int TRANSACTION_canChangeDtmfToneLength = 135;
        static final int TRANSACTION_canConnectTo5GInDsdsMode = 282;
        static final int TRANSACTION_carrierActionReportDefaultNetworkStatus = 175;
        static final int TRANSACTION_carrierActionResetAll = 176;
        static final int TRANSACTION_carrierActionSetRadioEnabled = 174;
        static final int TRANSACTION_changeIccLockPassword = 278;
        static final int TRANSACTION_checkCarrierPrivilegesForPackage = 118;
        static final int TRANSACTION_checkCarrierPrivilegesForPackageAnyPhone = 119;
        static final int TRANSACTION_clearCarrierImsServiceOverride = 96;
        static final int TRANSACTION_clearSignalStrengthUpdateRequest = 325;
        static final int TRANSACTION_clearUceRegistrationOverrideShell = 319;
        static final int TRANSACTION_dial = 1;
        static final int TRANSACTION_disableDataConnectivity = 26;
        static final int TRANSACTION_disableIms = 89;
        static final int TRANSACTION_disableLocationUpdates = 24;
        static final int TRANSACTION_disableVisualVoicemailSmsFilter = 56;
        static final int TRANSACTION_doesSwitchMultiSimConfigTriggerReboot = 258;
        static final int TRANSACTION_enableDataConnectivity = 25;
        static final int TRANSACTION_enableIms = 88;
        static final int TRANSACTION_enableLocationUpdates = 23;
        static final int TRANSACTION_enableModemForSlot = 254;
        static final int TRANSACTION_enableVideoCalling = 133;
        static final int TRANSACTION_enableVisualVoicemailSmsFilter = 55;
        static final int TRANSACTION_enqueueSmsPickResult = 269;
        static final int TRANSACTION_factoryReset = 153;
        static final int TRANSACTION_getActivePhoneType = 37;
        static final int TRANSACTION_getActivePhoneTypeForSlot = 38;
        static final int TRANSACTION_getActiveVisualVoicemailSmsFilterSettings = 58;
        static final int TRANSACTION_getAidForAppType = 163;
        static final int TRANSACTION_getAllCellInfo = 69;
        static final int TRANSACTION_getAllowedCarriers = 168;
        static final int TRANSACTION_getAllowedNetworkTypesBitmask = 86;
        static final int TRANSACTION_getAllowedNetworkTypesForReason = 104;
        static final int TRANSACTION_getBoundGbaService = 292;
        static final int TRANSACTION_getBoundImsServicePackage = 97;
        static final int TRANSACTION_getCallComposerStatus = 8;
        static final int TRANSACTION_getCallForwarding = 177;
        static final int TRANSACTION_getCallState = 31;
        static final int TRANSACTION_getCallStateForSubscription = 32;
        static final int TRANSACTION_getCallWaitingStatus = 179;
        static final int TRANSACTION_getCapabilityFromEab = 314;
        static final int TRANSACTION_getCardIdForDefaultEuicc = 188;
        static final int TRANSACTION_getCarrierIdFromMccMnc = 173;
        static final int TRANSACTION_getCarrierIdListVersion = 200;
        static final int TRANSACTION_getCarrierPackageNamesForIntentAndPhone = 120;
        static final int TRANSACTION_getCarrierPrivilegeStatus = 116;
        static final int TRANSACTION_getCarrierPrivilegeStatusForUid = 117;
        static final int TRANSACTION_getCarrierServicePackageNameForLogicalSlot = 338;
        static final int TRANSACTION_getCarrierSingleRegistrationEnabled = 308;
        static final int TRANSACTION_getCdmaEriIconIndex = 39;
        static final int TRANSACTION_getCdmaEriIconIndexForSubscriber = 40;
        static final int TRANSACTION_getCdmaEriIconMode = 41;
        static final int TRANSACTION_getCdmaEriIconModeForSubscriber = 42;
        static final int TRANSACTION_getCdmaEriText = 43;
        static final int TRANSACTION_getCdmaEriTextForSubscriber = 44;
        static final int TRANSACTION_getCdmaMdn = 113;
        static final int TRANSACTION_getCdmaMin = 114;
        static final int TRANSACTION_getCdmaPrlVersion = 165;
        static final int TRANSACTION_getCdmaRoamingMode = 195;
        static final int TRANSACTION_getCdmaSubscriptionMode = 197;
        static final int TRANSACTION_getCellLocation = 28;
        static final int TRANSACTION_getCellNetworkScanResults = 100;
        static final int TRANSACTION_getCertsFromCarrierPrivilegeAccessRules = 234;
        static final int TRANSACTION_getClientRequestStats = 181;
        static final int TRANSACTION_getContactFromEab = 313;
        static final int TRANSACTION_getCurrentPackageName = 261;
        static final int TRANSACTION_getDataActivationState = 50;
        static final int TRANSACTION_getDataActivity = 33;
        static final int TRANSACTION_getDataActivityForSubId = 34;
        static final int TRANSACTION_getDataEnabled = 106;
        static final int TRANSACTION_getDataNetworkType = 62;
        static final int TRANSACTION_getDataNetworkTypeForSubscriber = 63;
        static final int TRANSACTION_getDataState = 35;
        static final int TRANSACTION_getDataStateForSubId = 36;
        static final int TRANSACTION_getDeviceId = 144;
        static final int TRANSACTION_getDeviceIdWithFeature = 145;
        static final int TRANSACTION_getDeviceSingleRegistrationEnabled = 303;
        static final int TRANSACTION_getDeviceSoftwareVersionForSlot = 150;
        static final int TRANSACTION_getDeviceUceEnabled = 315;
        static final int TRANSACTION_getEmergencyCallbackMode = 186;
        static final int TRANSACTION_getEmergencyNumberDbVersion = 250;
        static final int TRANSACTION_getEmergencyNumberList = 232;
        static final int TRANSACTION_getEmergencyNumberListTestMode = 249;
        static final int TRANSACTION_getEquivalentHomePlmns = 283;
        static final int TRANSACTION_getEsn = 164;
        static final int TRANSACTION_getForbiddenPlmns = 184;
        static final int TRANSACTION_getGbaReleaseTime = 294;
        static final int TRANSACTION_getImeiForSlot = 146;
        static final int TRANSACTION_getImsConfig = 94;
        static final int TRANSACTION_getImsFeatureValidationOverride = 310;
        static final int TRANSACTION_getImsMmTelFeatureState = 98;
        static final int TRANSACTION_getImsMmTelRegistrationState = 208;
        static final int TRANSACTION_getImsMmTelRegistrationTransportType = 209;
        static final int TRANSACTION_getImsProvisioningInt = 243;
        static final int TRANSACTION_getImsProvisioningStatusForCapability = 240;
        static final int TRANSACTION_getImsProvisioningString = 244;
        static final int TRANSACTION_getImsRegTechnologyForMmTel = 143;
        static final int TRANSACTION_getImsRegistration = 93;
        static final int TRANSACTION_getLastKnownCellIdentity = 331;
        static final int TRANSACTION_getLastUcePidfXmlShell = 321;
        static final int TRANSACTION_getLatestRcsContactUceCapabilityShell = 320;
        static final int TRANSACTION_getLine1AlphaTagForDisplay = 123;
        static final int TRANSACTION_getLine1NumberForDisplay = 122;
        static final int TRANSACTION_getLteOnCdmaMode = 67;
        static final int TRANSACTION_getLteOnCdmaModeForSubscriber = 68;
        static final int TRANSACTION_getManualNetworkSelectionPlmn = 281;
        static final int TRANSACTION_getManufacturerCodeForSlot = 149;
        static final int TRANSACTION_getMeidForSlot = 148;
        static final int TRANSACTION_getMergedImsisFromGroup = 125;
        static final int TRANSACTION_getMergedSubscriberIds = 124;
        static final int TRANSACTION_getMmsUAProfUrl = 271;
        static final int TRANSACTION_getMmsUserAgent = 270;
        static final int TRANSACTION_getMobileProvisioningUrl = 311;
        static final int TRANSACTION_getModemService = 334;
        static final int TRANSACTION_getNeighboringCellInfo = 30;
        static final int TRANSACTION_getNetworkCountryIsoForPhone = 29;
        static final int TRANSACTION_getNetworkSelectionMode = 203;
        static final int TRANSACTION_getNetworkTypeForSubscriber = 61;
        static final int TRANSACTION_getNumberOfModemsWithSimultaneousDataConnections = 202;
        static final int TRANSACTION_getPackagesWithCarrierPrivileges = 161;
        static final int TRANSACTION_getPackagesWithCarrierPrivilegesForAllPhones = 162;
        static final int TRANSACTION_getPhoneAccountHandleForSubscriptionId = 152;
        static final int TRANSACTION_getPhoneCapability = 326;
        static final int TRANSACTION_getRadioAccessFamily = 131;
        static final int TRANSACTION_getRadioHalVersion = 260;
        static final int TRANSACTION_getRadioPowerState = 205;
        static final int TRANSACTION_getRcsProvisioningStatusForCapability = 241;
        static final int TRANSACTION_getRcsSingleRegistrationTestModeEnabled = 301;
        static final int TRANSACTION_getServiceStateForSubscriber = 156;
        static final int TRANSACTION_getSignalStrength = 187;
        static final int TRANSACTION_getSimLocaleForSubscriber = 154;
        static final int TRANSACTION_getSlicingConfig = 328;
        static final int TRANSACTION_getSlotsMapping = 259;
        static final int TRANSACTION_getSubIdForPhoneAccountHandle = 151;
        static final int TRANSACTION_getSubscriptionCarrierId = 169;
        static final int TRANSACTION_getSubscriptionCarrierName = 170;
        static final int TRANSACTION_getSubscriptionSpecificCarrierId = 171;
        static final int TRANSACTION_getSubscriptionSpecificCarrierName = 172;
        static final int TRANSACTION_getSystemSelectionChannels = 267;
        static final int TRANSACTION_getTelephonyHistograms = 166;
        static final int TRANSACTION_getTypeAllocationCodeForSlot = 147;
        static final int TRANSACTION_getUiccCardsInfo = 189;
        static final int TRANSACTION_getUiccSlotsInfo = 190;
        static final int TRANSACTION_getVisualVoicemailPackageName = 54;
        static final int TRANSACTION_getVisualVoicemailSettings = 53;
        static final int TRANSACTION_getVisualVoicemailSmsFilterSettings = 57;
        static final int TRANSACTION_getVoWiFiModeSetting = 226;
        static final int TRANSACTION_getVoWiFiRoamingModeSetting = 228;
        static final int TRANSACTION_getVoiceActivationState = 49;
        static final int TRANSACTION_getVoiceMessageCountForSubscriber = 51;
        static final int TRANSACTION_getVoiceNetworkTypeForSubscriber = 64;
        static final int TRANSACTION_getVoicemailRingtoneUri = 157;
        static final int TRANSACTION_handlePinMmi = 13;
        static final int TRANSACTION_handlePinMmiForSubscriber = 15;
        static final int TRANSACTION_handleUssdRequest = 14;
        static final int TRANSACTION_hasIccCard = 65;
        static final int TRANSACTION_hasIccCardUsingSlotIndex = 66;
        static final int TRANSACTION_iccCloseLogicalChannel = 74;
        static final int TRANSACTION_iccExchangeSimIO = 79;
        static final int TRANSACTION_iccOpenLogicalChannel = 73;
        static final int TRANSACTION_iccTransmitApduBasicChannel = 78;
        static final int TRANSACTION_iccTransmitApduBasicChannelByPort = 77;
        static final int TRANSACTION_iccTransmitApduLogicalChannel = 76;
        static final int TRANSACTION_iccTransmitApduLogicalChannelByPort = 75;
        static final int TRANSACTION_invokeOemRilRequestRaw = 128;
        static final int TRANSACTION_isAdvancedCallingSettingEnabled = 215;
        static final int TRANSACTION_isApnMetered = 265;
        static final int TRANSACTION_isApplicationOnUicc = 262;
        static final int TRANSACTION_isAvailable = 213;
        static final int TRANSACTION_isCapable = 212;
        static final int TRANSACTION_isConcurrentVoiceAndDataAllowed = 52;
        static final int TRANSACTION_isCrossSimCallingEnabledByUser = 221;
        static final int TRANSACTION_isDataConnectivityPossible = 27;
        static final int TRANSACTION_isDataEnabled = 108;
        static final int TRANSACTION_isDataEnabledForApn = 264;
        static final int TRANSACTION_isDataEnabledForReason = 110;
        static final int TRANSACTION_isDataRoamingEnabled = 193;
        static final int TRANSACTION_isEmergencyNumber = 233;
        static final int TRANSACTION_isHearingAidCompatibilitySupported = 139;
        static final int TRANSACTION_isIccLockEnabled = 276;
        static final int TRANSACTION_isImsRegistered = 140;
        static final int TRANSACTION_isInEmergencySmsMode = 204;
        static final int TRANSACTION_isManualNetworkSelectionAllowed = 111;
        static final int TRANSACTION_isMmTelCapabilitySupported = 214;
        static final int TRANSACTION_isMobileDataPolicyEnabled = 273;
        static final int TRANSACTION_isModemEnabledForSlot = 263;
        static final int TRANSACTION_isMultiSimSupported = 256;
        static final int TRANSACTION_isMvnoMatched = 268;
        static final int TRANSACTION_isNrDualConnectivityEnabled = 287;
        static final int TRANSACTION_isProvisioningRequiredForCapability = 335;
        static final int TRANSACTION_isRadioInterfaceCapabilitySupported = 288;
        static final int TRANSACTION_isRadioOn = 3;
        static final int TRANSACTION_isRadioOnForSubscriber = 5;
        static final int TRANSACTION_isRadioOnForSubscriberWithFeature = 6;
        static final int TRANSACTION_isRadioOnWithFeature = 4;
        static final int TRANSACTION_isRcsProvisioningRequiredForCapability = 336;
        static final int TRANSACTION_isRcsVolteSingleRegistrationCapable = 296;
        static final int TRANSACTION_isRemovableEsimDefaultEuicc = 340;
        static final int TRANSACTION_isRttSupported = 138;
        static final int TRANSACTION_isTetheringApnRequiredForSubscriber = 87;
        static final int TRANSACTION_isTtyModeSupported = 137;
        static final int TRANSACTION_isTtyOverVolteEnabled = 231;
        static final int TRANSACTION_isUserDataEnabled = 107;
        static final int TRANSACTION_isUsingNewDataStack = 332;
        static final int TRANSACTION_isVideoCallingEnabled = 134;
        static final int TRANSACTION_isVideoTelephonyAvailable = 142;
        static final int TRANSACTION_isVoNrEnabled = 285;
        static final int TRANSACTION_isVoWiFiRoamingSettingEnabled = 223;
        static final int TRANSACTION_isVoWiFiSettingEnabled = 219;
        static final int TRANSACTION_isVoicemailVibrationEnabled = 159;
        static final int TRANSACTION_isVtSettingEnabled = 217;
        static final int TRANSACTION_isWifiCallingAvailable = 141;
        static final int TRANSACTION_isWorldPhone = 136;
        static final int TRANSACTION_needMobileRadioShutdown = 129;
        static final int TRANSACTION_needsOtaServiceProvisioning = 45;
        static final int TRANSACTION_notifyOtaEmergencyNumberDbInstalled = 251;
        static final int TRANSACTION_notifyRcsAutoConfigurationReceived = 275;
        static final int TRANSACTION_nvReadItem = 81;
        static final int TRANSACTION_nvWriteCdmaPrl = 83;
        static final int TRANSACTION_nvWriteItem = 82;
        static final int TRANSACTION_prepareForUnattendedReboot = 327;
        static final int TRANSACTION_rebootModem = 85;
        static final int TRANSACTION_refreshUiccProfile = 201;
        static final int TRANSACTION_registerFeatureProvisioningChangedCallback = 237;
        static final int TRANSACTION_registerImsProvisioningChangedCallback = 235;
        static final int TRANSACTION_registerImsRegistrationCallback = 206;
        static final int TRANSACTION_registerImsStateCallback = 329;
        static final int TRANSACTION_registerMmTelCapabilityCallback = 210;
        static final int TRANSACTION_registerMmTelFeatureCallback = 91;
        static final int TRANSACTION_registerRcsProvisioningCallback = 297;
        static final int TRANSACTION_removeContactFromEab = 312;
        static final int TRANSACTION_removeUceRegistrationOverrideShell = 318;
        static final int TRANSACTION_removeUceRequestDisallowedStatus = 322;
        static final int TRANSACTION_requestCellInfoUpdate = 70;
        static final int TRANSACTION_requestCellInfoUpdateWithWorkSource = 71;
        static final int TRANSACTION_requestModemActivityInfo = 155;
        static final int TRANSACTION_requestNetworkScan = 101;
        static final int TRANSACTION_requestNumberVerification = 115;
        static final int TRANSACTION_requestUserActivityNotification = 279;
        static final int TRANSACTION_resetIms = 90;
        static final int TRANSACTION_resetModemConfig = 84;
        static final int TRANSACTION_resetOtaEmergencyNumberDbFilePath = 253;
        static final int TRANSACTION_sendDeviceToDeviceMessage = 305;
        static final int TRANSACTION_sendDialerSpecialCode = 60;
        static final int TRANSACTION_sendEnvelopeWithStatus = 80;
        static final int TRANSACTION_sendThermalMitigationRequest = 289;
        static final int TRANSACTION_sendVisualVoicemailSmsForSubscriber = 59;
        static final int TRANSACTION_setActiveDeviceToDeviceTransport = 306;
        static final int TRANSACTION_setAdvancedCallingSettingEnabled = 216;
        static final int TRANSACTION_setAllowedCarriers = 167;
        static final int TRANSACTION_setAllowedNetworkTypesForReason = 105;
        static final int TRANSACTION_setBoundGbaServiceOverride = 291;
        static final int TRANSACTION_setBoundImsServiceOverride = 95;
        static final int TRANSACTION_setCallComposerStatus = 7;
        static final int TRANSACTION_setCallForwarding = 178;
        static final int TRANSACTION_setCallWaitingStatus = 180;
        static final int TRANSACTION_setCapabilitiesRequestTimeout = 323;
        static final int TRANSACTION_setCarrierSingleRegistrationEnabledOverride = 304;
        static final int TRANSACTION_setCarrierTestOverride = 199;
        static final int TRANSACTION_setCdmaRoamingMode = 196;
        static final int TRANSACTION_setCdmaSubscriptionMode = 198;
        static final int TRANSACTION_setCellInfoListRate = 72;
        static final int TRANSACTION_setCepEnabled = 274;
        static final int TRANSACTION_setCrossSimCallingEnabled = 222;
        static final int TRANSACTION_setDataActivationState = 48;
        static final int TRANSACTION_setDataEnabledForReason = 109;
        static final int TRANSACTION_setDataRoamingEnabled = 194;
        static final int TRANSACTION_setDeviceSingleRegistrationEnabledOverride = 302;
        static final int TRANSACTION_setDeviceToDeviceForceEnabled = 307;
        static final int TRANSACTION_setDeviceUceEnabled = 316;
        static final int TRANSACTION_setForbiddenPlmns = 185;
        static final int TRANSACTION_setGbaReleaseTimeOverride = 293;
        static final int TRANSACTION_setIccLockEnabled = 277;
        static final int TRANSACTION_setImsFeatureValidationOverride = 309;
        static final int TRANSACTION_setImsProvisioningInt = 245;
        static final int TRANSACTION_setImsProvisioningStatusForCapability = 239;
        static final int TRANSACTION_setImsProvisioningString = 246;
        static final int TRANSACTION_setImsRegistrationState = 112;
        static final int TRANSACTION_setLine1NumberForDisplayForSubscriber = 121;
        static final int TRANSACTION_setMobileDataPolicyEnabled = 272;
        static final int TRANSACTION_setModemService = 333;
        static final int TRANSACTION_setMultiSimCarrierRestriction = 255;
        static final int TRANSACTION_setNetworkSelectionModeAutomatic = 99;
        static final int TRANSACTION_setNetworkSelectionModeManual = 103;
        static final int TRANSACTION_setNrDualConnectivityState = 286;
        static final int TRANSACTION_setOperatorBrandOverride = 126;
        static final int TRANSACTION_setRadio = 18;
        static final int TRANSACTION_setRadioForSubscriber = 19;
        static final int TRANSACTION_setRadioPower = 20;
        static final int TRANSACTION_setRcsClientConfiguration = 295;
        static final int TRANSACTION_setRcsProvisioningStatusForCapability = 242;
        static final int TRANSACTION_setRcsSingleRegistrationTestModeEnabled = 300;
        static final int TRANSACTION_setRemovableEsimAsDefaultEuicc = 339;
        static final int TRANSACTION_setRoamingOverride = 127;
        static final int TRANSACTION_setRttCapabilitySetting = 230;
        static final int TRANSACTION_setSignalStrengthUpdateRequest = 324;
        static final int TRANSACTION_setSimPowerStateForSlot = 182;
        static final int TRANSACTION_setSimPowerStateForSlotWithCallback = 183;
        static final int TRANSACTION_setSimSlotMapping = 192;
        static final int TRANSACTION_setSystemSelectionChannels = 266;
        static final int TRANSACTION_setVoNrEnabled = 284;
        static final int TRANSACTION_setVoWiFiModeSetting = 227;
        static final int TRANSACTION_setVoWiFiNonPersistent = 225;
        static final int TRANSACTION_setVoWiFiRoamingModeSetting = 229;
        static final int TRANSACTION_setVoWiFiRoamingSettingEnabled = 224;
        static final int TRANSACTION_setVoWiFiSettingEnabled = 220;
        static final int TRANSACTION_setVoiceActivationState = 47;
        static final int TRANSACTION_setVoiceMailNumber = 46;
        static final int TRANSACTION_setVoiceServiceStateOverride = 337;
        static final int TRANSACTION_setVoicemailRingtoneUri = 158;
        static final int TRANSACTION_setVoicemailVibrationEnabled = 160;
        static final int TRANSACTION_setVtSettingEnabled = 218;
        static final int TRANSACTION_shutdownMobileRadios = 130;
        static final int TRANSACTION_startEmergencyCallbackMode = 247;
        static final int TRANSACTION_stopNetworkScan = 102;
        static final int TRANSACTION_supplyPinForSubscriber = 9;
        static final int TRANSACTION_supplyPinReportResultForSubscriber = 11;
        static final int TRANSACTION_supplyPukForSubscriber = 10;
        static final int TRANSACTION_supplyPukReportResultForSubscriber = 12;
        static final int TRANSACTION_switchMultiSimConfig = 257;
        static final int TRANSACTION_switchSlots = 191;
        static final int TRANSACTION_toggleRadioOnOff = 16;
        static final int TRANSACTION_toggleRadioOnOffForSubscriber = 17;
        static final int TRANSACTION_triggerRcsReconfiguration = 299;
        static final int TRANSACTION_unregisterFeatureProvisioningChangedCallback = 238;
        static final int TRANSACTION_unregisterImsFeatureCallback = 92;
        static final int TRANSACTION_unregisterImsProvisioningChangedCallback = 236;
        static final int TRANSACTION_unregisterImsRegistrationCallback = 207;
        static final int TRANSACTION_unregisterImsStateCallback = 330;
        static final int TRANSACTION_unregisterMmTelCapabilityCallback = 211;
        static final int TRANSACTION_unregisterRcsProvisioningCallback = 298;
        static final int TRANSACTION_updateEmergencyNumberListTestMode = 248;
        static final int TRANSACTION_updateOtaEmergencyNumberDbFilePath = 252;
        static final int TRANSACTION_updateServiceLocation = 21;
        static final int TRANSACTION_updateServiceLocationWithPackageName = 22;
        static final int TRANSACTION_uploadCallComposerPicture = 132;
        static final int TRANSACTION_userActivity = 280;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static ITelephony asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof ITelephony)) {
                return (ITelephony) iin;
            }
            return new Proxy(obj);
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return this;
        }

        public static String getDefaultTransactionName(int transactionCode) {
            switch (transactionCode) {
                case 1:
                    return "dial";
                case 2:
                    return "call";
                case 3:
                    return "isRadioOn";
                case 4:
                    return "isRadioOnWithFeature";
                case 5:
                    return "isRadioOnForSubscriber";
                case 6:
                    return "isRadioOnForSubscriberWithFeature";
                case 7:
                    return "setCallComposerStatus";
                case 8:
                    return "getCallComposerStatus";
                case 9:
                    return "supplyPinForSubscriber";
                case 10:
                    return "supplyPukForSubscriber";
                case 11:
                    return "supplyPinReportResultForSubscriber";
                case 12:
                    return "supplyPukReportResultForSubscriber";
                case 13:
                    return "handlePinMmi";
                case 14:
                    return "handleUssdRequest";
                case 15:
                    return "handlePinMmiForSubscriber";
                case 16:
                    return "toggleRadioOnOff";
                case 17:
                    return "toggleRadioOnOffForSubscriber";
                case 18:
                    return "setRadio";
                case 19:
                    return "setRadioForSubscriber";
                case 20:
                    return "setRadioPower";
                case 21:
                    return "updateServiceLocation";
                case 22:
                    return "updateServiceLocationWithPackageName";
                case 23:
                    return "enableLocationUpdates";
                case 24:
                    return "disableLocationUpdates";
                case 25:
                    return "enableDataConnectivity";
                case 26:
                    return "disableDataConnectivity";
                case 27:
                    return "isDataConnectivityPossible";
                case 28:
                    return "getCellLocation";
                case 29:
                    return "getNetworkCountryIsoForPhone";
                case 30:
                    return "getNeighboringCellInfo";
                case 31:
                    return "getCallState";
                case 32:
                    return "getCallStateForSubscription";
                case 33:
                    return "getDataActivity";
                case 34:
                    return "getDataActivityForSubId";
                case 35:
                    return "getDataState";
                case 36:
                    return "getDataStateForSubId";
                case 37:
                    return "getActivePhoneType";
                case 38:
                    return "getActivePhoneTypeForSlot";
                case 39:
                    return "getCdmaEriIconIndex";
                case 40:
                    return "getCdmaEriIconIndexForSubscriber";
                case 41:
                    return "getCdmaEriIconMode";
                case 42:
                    return "getCdmaEriIconModeForSubscriber";
                case 43:
                    return "getCdmaEriText";
                case 44:
                    return "getCdmaEriTextForSubscriber";
                case 45:
                    return "needsOtaServiceProvisioning";
                case 46:
                    return "setVoiceMailNumber";
                case 47:
                    return "setVoiceActivationState";
                case 48:
                    return "setDataActivationState";
                case 49:
                    return "getVoiceActivationState";
                case 50:
                    return "getDataActivationState";
                case 51:
                    return "getVoiceMessageCountForSubscriber";
                case 52:
                    return "isConcurrentVoiceAndDataAllowed";
                case 53:
                    return "getVisualVoicemailSettings";
                case 54:
                    return "getVisualVoicemailPackageName";
                case 55:
                    return "enableVisualVoicemailSmsFilter";
                case 56:
                    return "disableVisualVoicemailSmsFilter";
                case 57:
                    return "getVisualVoicemailSmsFilterSettings";
                case 58:
                    return "getActiveVisualVoicemailSmsFilterSettings";
                case 59:
                    return "sendVisualVoicemailSmsForSubscriber";
                case 60:
                    return "sendDialerSpecialCode";
                case 61:
                    return "getNetworkTypeForSubscriber";
                case 62:
                    return "getDataNetworkType";
                case 63:
                    return "getDataNetworkTypeForSubscriber";
                case 64:
                    return "getVoiceNetworkTypeForSubscriber";
                case 65:
                    return "hasIccCard";
                case 66:
                    return "hasIccCardUsingSlotIndex";
                case 67:
                    return "getLteOnCdmaMode";
                case 68:
                    return "getLteOnCdmaModeForSubscriber";
                case 69:
                    return "getAllCellInfo";
                case 70:
                    return "requestCellInfoUpdate";
                case 71:
                    return "requestCellInfoUpdateWithWorkSource";
                case 72:
                    return "setCellInfoListRate";
                case 73:
                    return "iccOpenLogicalChannel";
                case 74:
                    return "iccCloseLogicalChannel";
                case 75:
                    return "iccTransmitApduLogicalChannelByPort";
                case 76:
                    return "iccTransmitApduLogicalChannel";
                case 77:
                    return "iccTransmitApduBasicChannelByPort";
                case 78:
                    return "iccTransmitApduBasicChannel";
                case 79:
                    return "iccExchangeSimIO";
                case 80:
                    return "sendEnvelopeWithStatus";
                case 81:
                    return "nvReadItem";
                case 82:
                    return "nvWriteItem";
                case 83:
                    return "nvWriteCdmaPrl";
                case 84:
                    return "resetModemConfig";
                case 85:
                    return "rebootModem";
                case 86:
                    return "getAllowedNetworkTypesBitmask";
                case 87:
                    return "isTetheringApnRequiredForSubscriber";
                case 88:
                    return "enableIms";
                case 89:
                    return "disableIms";
                case 90:
                    return "resetIms";
                case 91:
                    return "registerMmTelFeatureCallback";
                case 92:
                    return "unregisterImsFeatureCallback";
                case 93:
                    return "getImsRegistration";
                case 94:
                    return "getImsConfig";
                case 95:
                    return "setBoundImsServiceOverride";
                case 96:
                    return "clearCarrierImsServiceOverride";
                case 97:
                    return "getBoundImsServicePackage";
                case 98:
                    return "getImsMmTelFeatureState";
                case 99:
                    return "setNetworkSelectionModeAutomatic";
                case 100:
                    return "getCellNetworkScanResults";
                case 101:
                    return "requestNetworkScan";
                case 102:
                    return "stopNetworkScan";
                case 103:
                    return "setNetworkSelectionModeManual";
                case 104:
                    return "getAllowedNetworkTypesForReason";
                case 105:
                    return "setAllowedNetworkTypesForReason";
                case 106:
                    return "getDataEnabled";
                case 107:
                    return "isUserDataEnabled";
                case 108:
                    return "isDataEnabled";
                case 109:
                    return "setDataEnabledForReason";
                case 110:
                    return "isDataEnabledForReason";
                case 111:
                    return "isManualNetworkSelectionAllowed";
                case 112:
                    return "setImsRegistrationState";
                case 113:
                    return "getCdmaMdn";
                case 114:
                    return "getCdmaMin";
                case 115:
                    return "requestNumberVerification";
                case 116:
                    return "getCarrierPrivilegeStatus";
                case 117:
                    return "getCarrierPrivilegeStatusForUid";
                case 118:
                    return "checkCarrierPrivilegesForPackage";
                case 119:
                    return "checkCarrierPrivilegesForPackageAnyPhone";
                case 120:
                    return "getCarrierPackageNamesForIntentAndPhone";
                case 121:
                    return "setLine1NumberForDisplayForSubscriber";
                case 122:
                    return "getLine1NumberForDisplay";
                case 123:
                    return "getLine1AlphaTagForDisplay";
                case 124:
                    return "getMergedSubscriberIds";
                case 125:
                    return "getMergedImsisFromGroup";
                case 126:
                    return "setOperatorBrandOverride";
                case 127:
                    return "setRoamingOverride";
                case 128:
                    return "invokeOemRilRequestRaw";
                case 129:
                    return "needMobileRadioShutdown";
                case 130:
                    return "shutdownMobileRadios";
                case 131:
                    return "getRadioAccessFamily";
                case 132:
                    return "uploadCallComposerPicture";
                case 133:
                    return "enableVideoCalling";
                case 134:
                    return "isVideoCallingEnabled";
                case 135:
                    return "canChangeDtmfToneLength";
                case 136:
                    return "isWorldPhone";
                case 137:
                    return "isTtyModeSupported";
                case 138:
                    return "isRttSupported";
                case 139:
                    return "isHearingAidCompatibilitySupported";
                case 140:
                    return "isImsRegistered";
                case 141:
                    return "isWifiCallingAvailable";
                case 142:
                    return "isVideoTelephonyAvailable";
                case 143:
                    return "getImsRegTechnologyForMmTel";
                case 144:
                    return "getDeviceId";
                case 145:
                    return "getDeviceIdWithFeature";
                case 146:
                    return "getImeiForSlot";
                case 147:
                    return "getTypeAllocationCodeForSlot";
                case 148:
                    return "getMeidForSlot";
                case 149:
                    return "getManufacturerCodeForSlot";
                case 150:
                    return "getDeviceSoftwareVersionForSlot";
                case 151:
                    return "getSubIdForPhoneAccountHandle";
                case 152:
                    return "getPhoneAccountHandleForSubscriptionId";
                case 153:
                    return "factoryReset";
                case 154:
                    return "getSimLocaleForSubscriber";
                case 155:
                    return "requestModemActivityInfo";
                case 156:
                    return "getServiceStateForSubscriber";
                case 157:
                    return "getVoicemailRingtoneUri";
                case 158:
                    return "setVoicemailRingtoneUri";
                case 159:
                    return "isVoicemailVibrationEnabled";
                case 160:
                    return "setVoicemailVibrationEnabled";
                case 161:
                    return "getPackagesWithCarrierPrivileges";
                case 162:
                    return "getPackagesWithCarrierPrivilegesForAllPhones";
                case 163:
                    return "getAidForAppType";
                case 164:
                    return "getEsn";
                case 165:
                    return "getCdmaPrlVersion";
                case 166:
                    return "getTelephonyHistograms";
                case 167:
                    return "setAllowedCarriers";
                case 168:
                    return "getAllowedCarriers";
                case 169:
                    return "getSubscriptionCarrierId";
                case 170:
                    return "getSubscriptionCarrierName";
                case 171:
                    return "getSubscriptionSpecificCarrierId";
                case 172:
                    return "getSubscriptionSpecificCarrierName";
                case 173:
                    return "getCarrierIdFromMccMnc";
                case 174:
                    return "carrierActionSetRadioEnabled";
                case 175:
                    return "carrierActionReportDefaultNetworkStatus";
                case 176:
                    return "carrierActionResetAll";
                case 177:
                    return "getCallForwarding";
                case 178:
                    return "setCallForwarding";
                case 179:
                    return "getCallWaitingStatus";
                case 180:
                    return "setCallWaitingStatus";
                case 181:
                    return "getClientRequestStats";
                case 182:
                    return "setSimPowerStateForSlot";
                case 183:
                    return "setSimPowerStateForSlotWithCallback";
                case 184:
                    return "getForbiddenPlmns";
                case 185:
                    return "setForbiddenPlmns";
                case 186:
                    return "getEmergencyCallbackMode";
                case 187:
                    return "getSignalStrength";
                case 188:
                    return "getCardIdForDefaultEuicc";
                case 189:
                    return "getUiccCardsInfo";
                case 190:
                    return "getUiccSlotsInfo";
                case 191:
                    return "switchSlots";
                case 192:
                    return "setSimSlotMapping";
                case 193:
                    return "isDataRoamingEnabled";
                case 194:
                    return "setDataRoamingEnabled";
                case 195:
                    return "getCdmaRoamingMode";
                case 196:
                    return "setCdmaRoamingMode";
                case 197:
                    return "getCdmaSubscriptionMode";
                case 198:
                    return "setCdmaSubscriptionMode";
                case 199:
                    return "setCarrierTestOverride";
                case 200:
                    return "getCarrierIdListVersion";
                case 201:
                    return "refreshUiccProfile";
                case 202:
                    return "getNumberOfModemsWithSimultaneousDataConnections";
                case 203:
                    return "getNetworkSelectionMode";
                case 204:
                    return "isInEmergencySmsMode";
                case 205:
                    return "getRadioPowerState";
                case 206:
                    return "registerImsRegistrationCallback";
                case 207:
                    return "unregisterImsRegistrationCallback";
                case 208:
                    return "getImsMmTelRegistrationState";
                case 209:
                    return "getImsMmTelRegistrationTransportType";
                case 210:
                    return "registerMmTelCapabilityCallback";
                case 211:
                    return "unregisterMmTelCapabilityCallback";
                case 212:
                    return "isCapable";
                case 213:
                    return "isAvailable";
                case 214:
                    return "isMmTelCapabilitySupported";
                case 215:
                    return "isAdvancedCallingSettingEnabled";
                case 216:
                    return "setAdvancedCallingSettingEnabled";
                case 217:
                    return "isVtSettingEnabled";
                case 218:
                    return "setVtSettingEnabled";
                case 219:
                    return "isVoWiFiSettingEnabled";
                case 220:
                    return "setVoWiFiSettingEnabled";
                case 221:
                    return "isCrossSimCallingEnabledByUser";
                case 222:
                    return "setCrossSimCallingEnabled";
                case 223:
                    return "isVoWiFiRoamingSettingEnabled";
                case 224:
                    return "setVoWiFiRoamingSettingEnabled";
                case 225:
                    return "setVoWiFiNonPersistent";
                case 226:
                    return "getVoWiFiModeSetting";
                case 227:
                    return "setVoWiFiModeSetting";
                case 228:
                    return "getVoWiFiRoamingModeSetting";
                case 229:
                    return "setVoWiFiRoamingModeSetting";
                case 230:
                    return "setRttCapabilitySetting";
                case 231:
                    return "isTtyOverVolteEnabled";
                case 232:
                    return "getEmergencyNumberList";
                case 233:
                    return "isEmergencyNumber";
                case 234:
                    return "getCertsFromCarrierPrivilegeAccessRules";
                case 235:
                    return "registerImsProvisioningChangedCallback";
                case 236:
                    return "unregisterImsProvisioningChangedCallback";
                case 237:
                    return "registerFeatureProvisioningChangedCallback";
                case 238:
                    return "unregisterFeatureProvisioningChangedCallback";
                case 239:
                    return "setImsProvisioningStatusForCapability";
                case 240:
                    return "getImsProvisioningStatusForCapability";
                case 241:
                    return "getRcsProvisioningStatusForCapability";
                case 242:
                    return "setRcsProvisioningStatusForCapability";
                case 243:
                    return "getImsProvisioningInt";
                case 244:
                    return "getImsProvisioningString";
                case 245:
                    return "setImsProvisioningInt";
                case 246:
                    return "setImsProvisioningString";
                case 247:
                    return "startEmergencyCallbackMode";
                case 248:
                    return "updateEmergencyNumberListTestMode";
                case 249:
                    return "getEmergencyNumberListTestMode";
                case 250:
                    return "getEmergencyNumberDbVersion";
                case 251:
                    return "notifyOtaEmergencyNumberDbInstalled";
                case 252:
                    return "updateOtaEmergencyNumberDbFilePath";
                case 253:
                    return "resetOtaEmergencyNumberDbFilePath";
                case 254:
                    return "enableModemForSlot";
                case 255:
                    return "setMultiSimCarrierRestriction";
                case 256:
                    return "isMultiSimSupported";
                case 257:
                    return "switchMultiSimConfig";
                case 258:
                    return "doesSwitchMultiSimConfigTriggerReboot";
                case 259:
                    return "getSlotsMapping";
                case 260:
                    return "getRadioHalVersion";
                case 261:
                    return "getCurrentPackageName";
                case 262:
                    return "isApplicationOnUicc";
                case 263:
                    return "isModemEnabledForSlot";
                case 264:
                    return "isDataEnabledForApn";
                case 265:
                    return "isApnMetered";
                case 266:
                    return "setSystemSelectionChannels";
                case 267:
                    return "getSystemSelectionChannels";
                case 268:
                    return "isMvnoMatched";
                case 269:
                    return "enqueueSmsPickResult";
                case 270:
                    return "getMmsUserAgent";
                case 271:
                    return "getMmsUAProfUrl";
                case 272:
                    return "setMobileDataPolicyEnabled";
                case 273:
                    return "isMobileDataPolicyEnabled";
                case 274:
                    return "setCepEnabled";
                case 275:
                    return "notifyRcsAutoConfigurationReceived";
                case 276:
                    return "isIccLockEnabled";
                case 277:
                    return "setIccLockEnabled";
                case 278:
                    return "changeIccLockPassword";
                case 279:
                    return "requestUserActivityNotification";
                case 280:
                    return "userActivity";
                case 281:
                    return "getManualNetworkSelectionPlmn";
                case 282:
                    return "canConnectTo5GInDsdsMode";
                case 283:
                    return "getEquivalentHomePlmns";
                case 284:
                    return "setVoNrEnabled";
                case 285:
                    return "isVoNrEnabled";
                case 286:
                    return "setNrDualConnectivityState";
                case 287:
                    return "isNrDualConnectivityEnabled";
                case 288:
                    return "isRadioInterfaceCapabilitySupported";
                case 289:
                    return "sendThermalMitigationRequest";
                case 290:
                    return "bootstrapAuthenticationRequest";
                case 291:
                    return "setBoundGbaServiceOverride";
                case 292:
                    return "getBoundGbaService";
                case 293:
                    return "setGbaReleaseTimeOverride";
                case 294:
                    return "getGbaReleaseTime";
                case 295:
                    return "setRcsClientConfiguration";
                case 296:
                    return "isRcsVolteSingleRegistrationCapable";
                case 297:
                    return "registerRcsProvisioningCallback";
                case 298:
                    return "unregisterRcsProvisioningCallback";
                case 299:
                    return "triggerRcsReconfiguration";
                case 300:
                    return "setRcsSingleRegistrationTestModeEnabled";
                case 301:
                    return "getRcsSingleRegistrationTestModeEnabled";
                case 302:
                    return "setDeviceSingleRegistrationEnabledOverride";
                case 303:
                    return "getDeviceSingleRegistrationEnabled";
                case 304:
                    return "setCarrierSingleRegistrationEnabledOverride";
                case 305:
                    return "sendDeviceToDeviceMessage";
                case 306:
                    return "setActiveDeviceToDeviceTransport";
                case 307:
                    return "setDeviceToDeviceForceEnabled";
                case 308:
                    return "getCarrierSingleRegistrationEnabled";
                case 309:
                    return "setImsFeatureValidationOverride";
                case 310:
                    return "getImsFeatureValidationOverride";
                case 311:
                    return "getMobileProvisioningUrl";
                case 312:
                    return "removeContactFromEab";
                case 313:
                    return "getContactFromEab";
                case 314:
                    return "getCapabilityFromEab";
                case 315:
                    return "getDeviceUceEnabled";
                case 316:
                    return "setDeviceUceEnabled";
                case 317:
                    return "addUceRegistrationOverrideShell";
                case 318:
                    return "removeUceRegistrationOverrideShell";
                case 319:
                    return "clearUceRegistrationOverrideShell";
                case 320:
                    return "getLatestRcsContactUceCapabilityShell";
                case 321:
                    return "getLastUcePidfXmlShell";
                case 322:
                    return "removeUceRequestDisallowedStatus";
                case 323:
                    return "setCapabilitiesRequestTimeout";
                case 324:
                    return "setSignalStrengthUpdateRequest";
                case 325:
                    return "clearSignalStrengthUpdateRequest";
                case 326:
                    return "getPhoneCapability";
                case 327:
                    return "prepareForUnattendedReboot";
                case 328:
                    return "getSlicingConfig";
                case 329:
                    return "registerImsStateCallback";
                case 330:
                    return "unregisterImsStateCallback";
                case 331:
                    return "getLastKnownCellIdentity";
                case 332:
                    return "isUsingNewDataStack";
                case 333:
                    return "setModemService";
                case 334:
                    return "getModemService";
                case 335:
                    return "isProvisioningRequiredForCapability";
                case 336:
                    return "isRcsProvisioningRequiredForCapability";
                case 337:
                    return "setVoiceServiceStateOverride";
                case 338:
                    return "getCarrierServicePackageNameForLogicalSlot";
                case 339:
                    return "setRemovableEsimAsDefaultEuicc";
                case 340:
                    return "isRemovableEsimDefaultEuicc";
                default:
                    return null;
            }
        }

        @Override // android.os.Binder
        public String getTransactionName(int transactionCode) {
            return getDefaultTransactionName(transactionCode);
        }

        @Override // android.os.Binder
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            byte[] _arg1;
            if (code >= 1 && code <= 16777215) {
                data.enforceInterface(DESCRIPTOR);
            }
            switch (code) {
                case IBinder.INTERFACE_TRANSACTION /* 1598968902 */:
                    reply.writeString(DESCRIPTOR);
                    return true;
                default:
                    switch (code) {
                        case 1:
                            String _arg0 = data.readString();
                            data.enforceNoDataAvail();
                            dial(_arg0);
                            reply.writeNoException();
                            break;
                        case 2:
                            String _arg02 = data.readString();
                            String _arg12 = data.readString();
                            data.enforceNoDataAvail();
                            call(_arg02, _arg12);
                            reply.writeNoException();
                            break;
                        case 3:
                            String _arg03 = data.readString();
                            data.enforceNoDataAvail();
                            boolean _result = isRadioOn(_arg03);
                            reply.writeNoException();
                            reply.writeBoolean(_result);
                            break;
                        case 4:
                            String _arg04 = data.readString();
                            String _arg13 = data.readString();
                            data.enforceNoDataAvail();
                            boolean _result2 = isRadioOnWithFeature(_arg04, _arg13);
                            reply.writeNoException();
                            reply.writeBoolean(_result2);
                            break;
                        case 5:
                            int _arg05 = data.readInt();
                            String _arg14 = data.readString();
                            data.enforceNoDataAvail();
                            boolean _result3 = isRadioOnForSubscriber(_arg05, _arg14);
                            reply.writeNoException();
                            reply.writeBoolean(_result3);
                            break;
                        case 6:
                            int _arg06 = data.readInt();
                            String _arg15 = data.readString();
                            String _arg2 = data.readString();
                            data.enforceNoDataAvail();
                            boolean _result4 = isRadioOnForSubscriberWithFeature(_arg06, _arg15, _arg2);
                            reply.writeNoException();
                            reply.writeBoolean(_result4);
                            break;
                        case 7:
                            int _arg07 = data.readInt();
                            int _arg16 = data.readInt();
                            data.enforceNoDataAvail();
                            setCallComposerStatus(_arg07, _arg16);
                            reply.writeNoException();
                            break;
                        case 8:
                            int _arg08 = data.readInt();
                            data.enforceNoDataAvail();
                            int _result5 = getCallComposerStatus(_arg08);
                            reply.writeNoException();
                            reply.writeInt(_result5);
                            break;
                        case 9:
                            int _arg09 = data.readInt();
                            String _arg17 = data.readString();
                            data.enforceNoDataAvail();
                            boolean _result6 = supplyPinForSubscriber(_arg09, _arg17);
                            reply.writeNoException();
                            reply.writeBoolean(_result6);
                            break;
                        case 10:
                            int _arg010 = data.readInt();
                            String _arg18 = data.readString();
                            String _arg22 = data.readString();
                            data.enforceNoDataAvail();
                            boolean _result7 = supplyPukForSubscriber(_arg010, _arg18, _arg22);
                            reply.writeNoException();
                            reply.writeBoolean(_result7);
                            break;
                        case 11:
                            int _arg011 = data.readInt();
                            String _arg19 = data.readString();
                            data.enforceNoDataAvail();
                            int[] _result8 = supplyPinReportResultForSubscriber(_arg011, _arg19);
                            reply.writeNoException();
                            reply.writeIntArray(_result8);
                            break;
                        case 12:
                            int _arg012 = data.readInt();
                            String _arg110 = data.readString();
                            String _arg23 = data.readString();
                            data.enforceNoDataAvail();
                            int[] _result9 = supplyPukReportResultForSubscriber(_arg012, _arg110, _arg23);
                            reply.writeNoException();
                            reply.writeIntArray(_result9);
                            break;
                        case 13:
                            String _arg013 = data.readString();
                            data.enforceNoDataAvail();
                            boolean _result10 = handlePinMmi(_arg013);
                            reply.writeNoException();
                            reply.writeBoolean(_result10);
                            break;
                        case 14:
                            int _arg014 = data.readInt();
                            String _arg111 = data.readString();
                            ResultReceiver _arg24 = (ResultReceiver) data.readTypedObject(ResultReceiver.CREATOR);
                            data.enforceNoDataAvail();
                            handleUssdRequest(_arg014, _arg111, _arg24);
                            reply.writeNoException();
                            break;
                        case 15:
                            int _arg015 = data.readInt();
                            String _arg112 = data.readString();
                            data.enforceNoDataAvail();
                            boolean _result11 = handlePinMmiForSubscriber(_arg015, _arg112);
                            reply.writeNoException();
                            reply.writeBoolean(_result11);
                            break;
                        case 16:
                            toggleRadioOnOff();
                            reply.writeNoException();
                            break;
                        case 17:
                            int _arg016 = data.readInt();
                            data.enforceNoDataAvail();
                            toggleRadioOnOffForSubscriber(_arg016);
                            reply.writeNoException();
                            break;
                        case 18:
                            boolean _arg017 = data.readBoolean();
                            data.enforceNoDataAvail();
                            boolean _result12 = setRadio(_arg017);
                            reply.writeNoException();
                            reply.writeBoolean(_result12);
                            break;
                        case 19:
                            int _arg018 = data.readInt();
                            boolean _arg113 = data.readBoolean();
                            data.enforceNoDataAvail();
                            boolean _result13 = setRadioForSubscriber(_arg018, _arg113);
                            reply.writeNoException();
                            reply.writeBoolean(_result13);
                            break;
                        case 20:
                            boolean _arg019 = data.readBoolean();
                            data.enforceNoDataAvail();
                            boolean _result14 = setRadioPower(_arg019);
                            reply.writeNoException();
                            reply.writeBoolean(_result14);
                            break;
                        case 21:
                            updateServiceLocation();
                            reply.writeNoException();
                            break;
                        case 22:
                            String _arg020 = data.readString();
                            data.enforceNoDataAvail();
                            updateServiceLocationWithPackageName(_arg020);
                            reply.writeNoException();
                            break;
                        case 23:
                            enableLocationUpdates();
                            reply.writeNoException();
                            break;
                        case 24:
                            disableLocationUpdates();
                            reply.writeNoException();
                            break;
                        case 25:
                            String _arg021 = data.readString();
                            data.enforceNoDataAvail();
                            boolean _result15 = enableDataConnectivity(_arg021);
                            reply.writeNoException();
                            reply.writeBoolean(_result15);
                            break;
                        case 26:
                            String _arg022 = data.readString();
                            data.enforceNoDataAvail();
                            boolean _result16 = disableDataConnectivity(_arg022);
                            reply.writeNoException();
                            reply.writeBoolean(_result16);
                            break;
                        case 27:
                            int _arg023 = data.readInt();
                            data.enforceNoDataAvail();
                            boolean _result17 = isDataConnectivityPossible(_arg023);
                            reply.writeNoException();
                            reply.writeBoolean(_result17);
                            break;
                        case 28:
                            String _arg024 = data.readString();
                            String _arg114 = data.readString();
                            data.enforceNoDataAvail();
                            CellIdentity _result18 = getCellLocation(_arg024, _arg114);
                            reply.writeNoException();
                            reply.writeTypedObject(_result18, 1);
                            break;
                        case 29:
                            int _arg025 = data.readInt();
                            data.enforceNoDataAvail();
                            String _result19 = getNetworkCountryIsoForPhone(_arg025);
                            reply.writeNoException();
                            reply.writeString(_result19);
                            break;
                        case 30:
                            String _arg026 = data.readString();
                            String _arg115 = data.readString();
                            data.enforceNoDataAvail();
                            List<NeighboringCellInfo> _result20 = getNeighboringCellInfo(_arg026, _arg115);
                            reply.writeNoException();
                            reply.writeTypedList(_result20);
                            break;
                        case 31:
                            int _result21 = getCallState();
                            reply.writeNoException();
                            reply.writeInt(_result21);
                            break;
                        case 32:
                            int _arg027 = data.readInt();
                            String _arg116 = data.readString();
                            String _arg25 = data.readString();
                            data.enforceNoDataAvail();
                            int _result22 = getCallStateForSubscription(_arg027, _arg116, _arg25);
                            reply.writeNoException();
                            reply.writeInt(_result22);
                            break;
                        case 33:
                            int _result23 = getDataActivity();
                            reply.writeNoException();
                            reply.writeInt(_result23);
                            break;
                        case 34:
                            int _arg028 = data.readInt();
                            data.enforceNoDataAvail();
                            int _result24 = getDataActivityForSubId(_arg028);
                            reply.writeNoException();
                            reply.writeInt(_result24);
                            break;
                        case 35:
                            int _result25 = getDataState();
                            reply.writeNoException();
                            reply.writeInt(_result25);
                            break;
                        case 36:
                            int _arg029 = data.readInt();
                            data.enforceNoDataAvail();
                            int _result26 = getDataStateForSubId(_arg029);
                            reply.writeNoException();
                            reply.writeInt(_result26);
                            break;
                        case 37:
                            int _result27 = getActivePhoneType();
                            reply.writeNoException();
                            reply.writeInt(_result27);
                            break;
                        case 38:
                            int _arg030 = data.readInt();
                            data.enforceNoDataAvail();
                            int _result28 = getActivePhoneTypeForSlot(_arg030);
                            reply.writeNoException();
                            reply.writeInt(_result28);
                            break;
                        case 39:
                            String _arg031 = data.readString();
                            String _arg117 = data.readString();
                            data.enforceNoDataAvail();
                            int _result29 = getCdmaEriIconIndex(_arg031, _arg117);
                            reply.writeNoException();
                            reply.writeInt(_result29);
                            break;
                        case 40:
                            int _arg032 = data.readInt();
                            String _arg118 = data.readString();
                            String _arg26 = data.readString();
                            data.enforceNoDataAvail();
                            int _result30 = getCdmaEriIconIndexForSubscriber(_arg032, _arg118, _arg26);
                            reply.writeNoException();
                            reply.writeInt(_result30);
                            break;
                        case 41:
                            String _arg033 = data.readString();
                            String _arg119 = data.readString();
                            data.enforceNoDataAvail();
                            int _result31 = getCdmaEriIconMode(_arg033, _arg119);
                            reply.writeNoException();
                            reply.writeInt(_result31);
                            break;
                        case 42:
                            int _arg034 = data.readInt();
                            String _arg120 = data.readString();
                            String _arg27 = data.readString();
                            data.enforceNoDataAvail();
                            int _result32 = getCdmaEriIconModeForSubscriber(_arg034, _arg120, _arg27);
                            reply.writeNoException();
                            reply.writeInt(_result32);
                            break;
                        case 43:
                            String _arg035 = data.readString();
                            String _arg121 = data.readString();
                            data.enforceNoDataAvail();
                            String _result33 = getCdmaEriText(_arg035, _arg121);
                            reply.writeNoException();
                            reply.writeString(_result33);
                            break;
                        case 44:
                            int _arg036 = data.readInt();
                            String _arg122 = data.readString();
                            String _arg28 = data.readString();
                            data.enforceNoDataAvail();
                            String _result34 = getCdmaEriTextForSubscriber(_arg036, _arg122, _arg28);
                            reply.writeNoException();
                            reply.writeString(_result34);
                            break;
                        case 45:
                            boolean _result35 = needsOtaServiceProvisioning();
                            reply.writeNoException();
                            reply.writeBoolean(_result35);
                            break;
                        case 46:
                            int _arg037 = data.readInt();
                            String _arg123 = data.readString();
                            String _arg29 = data.readString();
                            data.enforceNoDataAvail();
                            boolean _result36 = setVoiceMailNumber(_arg037, _arg123, _arg29);
                            reply.writeNoException();
                            reply.writeBoolean(_result36);
                            break;
                        case 47:
                            int _arg038 = data.readInt();
                            int _arg124 = data.readInt();
                            data.enforceNoDataAvail();
                            setVoiceActivationState(_arg038, _arg124);
                            reply.writeNoException();
                            break;
                        case 48:
                            int _arg039 = data.readInt();
                            int _arg125 = data.readInt();
                            data.enforceNoDataAvail();
                            setDataActivationState(_arg039, _arg125);
                            reply.writeNoException();
                            break;
                        case 49:
                            int _arg040 = data.readInt();
                            String _arg126 = data.readString();
                            data.enforceNoDataAvail();
                            int _result37 = getVoiceActivationState(_arg040, _arg126);
                            reply.writeNoException();
                            reply.writeInt(_result37);
                            break;
                        case 50:
                            int _arg041 = data.readInt();
                            String _arg127 = data.readString();
                            data.enforceNoDataAvail();
                            int _result38 = getDataActivationState(_arg041, _arg127);
                            reply.writeNoException();
                            reply.writeInt(_result38);
                            break;
                        case 51:
                            int _arg042 = data.readInt();
                            String _arg128 = data.readString();
                            String _arg210 = data.readString();
                            data.enforceNoDataAvail();
                            int _result39 = getVoiceMessageCountForSubscriber(_arg042, _arg128, _arg210);
                            reply.writeNoException();
                            reply.writeInt(_result39);
                            break;
                        case 52:
                            int _arg043 = data.readInt();
                            data.enforceNoDataAvail();
                            boolean _result40 = isConcurrentVoiceAndDataAllowed(_arg043);
                            reply.writeNoException();
                            reply.writeBoolean(_result40);
                            break;
                        case 53:
                            String _arg044 = data.readString();
                            int _arg129 = data.readInt();
                            data.enforceNoDataAvail();
                            Bundle _result41 = getVisualVoicemailSettings(_arg044, _arg129);
                            reply.writeNoException();
                            reply.writeTypedObject(_result41, 1);
                            break;
                        case 54:
                            String _arg045 = data.readString();
                            String _arg130 = data.readString();
                            int _arg211 = data.readInt();
                            data.enforceNoDataAvail();
                            String _result42 = getVisualVoicemailPackageName(_arg045, _arg130, _arg211);
                            reply.writeNoException();
                            reply.writeString(_result42);
                            break;
                        case 55:
                            String _arg046 = data.readString();
                            int _arg131 = data.readInt();
                            VisualVoicemailSmsFilterSettings _arg212 = (VisualVoicemailSmsFilterSettings) data.readTypedObject(VisualVoicemailSmsFilterSettings.CREATOR);
                            data.enforceNoDataAvail();
                            enableVisualVoicemailSmsFilter(_arg046, _arg131, _arg212);
                            reply.writeNoException();
                            break;
                        case 56:
                            String _arg047 = data.readString();
                            int _arg132 = data.readInt();
                            data.enforceNoDataAvail();
                            disableVisualVoicemailSmsFilter(_arg047, _arg132);
                            break;
                        case 57:
                            String _arg048 = data.readString();
                            int _arg133 = data.readInt();
                            data.enforceNoDataAvail();
                            VisualVoicemailSmsFilterSettings _result43 = getVisualVoicemailSmsFilterSettings(_arg048, _arg133);
                            reply.writeNoException();
                            reply.writeTypedObject(_result43, 1);
                            break;
                        case 58:
                            int _arg049 = data.readInt();
                            data.enforceNoDataAvail();
                            VisualVoicemailSmsFilterSettings _result44 = getActiveVisualVoicemailSmsFilterSettings(_arg049);
                            reply.writeNoException();
                            reply.writeTypedObject(_result44, 1);
                            break;
                        case 59:
                            return onTransact$sendVisualVoicemailSmsForSubscriber$(data, reply);
                        case 60:
                            String _arg050 = data.readString();
                            String _arg134 = data.readString();
                            data.enforceNoDataAvail();
                            sendDialerSpecialCode(_arg050, _arg134);
                            reply.writeNoException();
                            break;
                        case 61:
                            int _arg051 = data.readInt();
                            String _arg135 = data.readString();
                            String _arg213 = data.readString();
                            data.enforceNoDataAvail();
                            int _result45 = getNetworkTypeForSubscriber(_arg051, _arg135, _arg213);
                            reply.writeNoException();
                            reply.writeInt(_result45);
                            break;
                        case 62:
                            String _arg052 = data.readString();
                            String _arg136 = data.readString();
                            data.enforceNoDataAvail();
                            int _result46 = getDataNetworkType(_arg052, _arg136);
                            reply.writeNoException();
                            reply.writeInt(_result46);
                            break;
                        case 63:
                            int _arg053 = data.readInt();
                            String _arg137 = data.readString();
                            String _arg214 = data.readString();
                            data.enforceNoDataAvail();
                            int _result47 = getDataNetworkTypeForSubscriber(_arg053, _arg137, _arg214);
                            reply.writeNoException();
                            reply.writeInt(_result47);
                            break;
                        case 64:
                            int _arg054 = data.readInt();
                            String _arg138 = data.readString();
                            String _arg215 = data.readString();
                            data.enforceNoDataAvail();
                            int _result48 = getVoiceNetworkTypeForSubscriber(_arg054, _arg138, _arg215);
                            reply.writeNoException();
                            reply.writeInt(_result48);
                            break;
                        case 65:
                            boolean _result49 = hasIccCard();
                            reply.writeNoException();
                            reply.writeBoolean(_result49);
                            break;
                        case 66:
                            int _arg055 = data.readInt();
                            data.enforceNoDataAvail();
                            boolean _result50 = hasIccCardUsingSlotIndex(_arg055);
                            reply.writeNoException();
                            reply.writeBoolean(_result50);
                            break;
                        case 67:
                            String _arg056 = data.readString();
                            String _arg139 = data.readString();
                            data.enforceNoDataAvail();
                            int _result51 = getLteOnCdmaMode(_arg056, _arg139);
                            reply.writeNoException();
                            reply.writeInt(_result51);
                            break;
                        case 68:
                            int _arg057 = data.readInt();
                            String _arg140 = data.readString();
                            String _arg216 = data.readString();
                            data.enforceNoDataAvail();
                            int _result52 = getLteOnCdmaModeForSubscriber(_arg057, _arg140, _arg216);
                            reply.writeNoException();
                            reply.writeInt(_result52);
                            break;
                        case 69:
                            String _arg058 = data.readString();
                            String _arg141 = data.readString();
                            data.enforceNoDataAvail();
                            List<CellInfo> _result53 = getAllCellInfo(_arg058, _arg141);
                            reply.writeNoException();
                            reply.writeTypedList(_result53);
                            break;
                        case 70:
                            return onTransact$requestCellInfoUpdate$(data, reply);
                        case 71:
                            return onTransact$requestCellInfoUpdateWithWorkSource$(data, reply);
                        case 72:
                            int _arg059 = data.readInt();
                            data.enforceNoDataAvail();
                            setCellInfoListRate(_arg059);
                            reply.writeNoException();
                            break;
                        case 73:
                            IccLogicalChannelRequest _arg060 = (IccLogicalChannelRequest) data.readTypedObject(IccLogicalChannelRequest.CREATOR);
                            data.enforceNoDataAvail();
                            IccOpenLogicalChannelResponse _result54 = iccOpenLogicalChannel(_arg060);
                            reply.writeNoException();
                            reply.writeTypedObject(_result54, 1);
                            break;
                        case 74:
                            IccLogicalChannelRequest _arg061 = (IccLogicalChannelRequest) data.readTypedObject(IccLogicalChannelRequest.CREATOR);
                            data.enforceNoDataAvail();
                            boolean _result55 = iccCloseLogicalChannel(_arg061);
                            reply.writeNoException();
                            reply.writeBoolean(_result55);
                            break;
                        case 75:
                            return onTransact$iccTransmitApduLogicalChannelByPort$(data, reply);
                        case 76:
                            return onTransact$iccTransmitApduLogicalChannel$(data, reply);
                        case 77:
                            return onTransact$iccTransmitApduBasicChannelByPort$(data, reply);
                        case 78:
                            return onTransact$iccTransmitApduBasicChannel$(data, reply);
                        case 79:
                            return onTransact$iccExchangeSimIO$(data, reply);
                        case 80:
                            int _arg062 = data.readInt();
                            String _arg142 = data.readString();
                            data.enforceNoDataAvail();
                            String _result56 = sendEnvelopeWithStatus(_arg062, _arg142);
                            reply.writeNoException();
                            reply.writeString(_result56);
                            break;
                        case 81:
                            int _arg063 = data.readInt();
                            data.enforceNoDataAvail();
                            String _result57 = nvReadItem(_arg063);
                            reply.writeNoException();
                            reply.writeString(_result57);
                            break;
                        case 82:
                            int _arg064 = data.readInt();
                            String _arg143 = data.readString();
                            data.enforceNoDataAvail();
                            boolean _result58 = nvWriteItem(_arg064, _arg143);
                            reply.writeNoException();
                            reply.writeBoolean(_result58);
                            break;
                        case 83:
                            byte[] _arg065 = data.createByteArray();
                            data.enforceNoDataAvail();
                            boolean _result59 = nvWriteCdmaPrl(_arg065);
                            reply.writeNoException();
                            reply.writeBoolean(_result59);
                            break;
                        case 84:
                            int _arg066 = data.readInt();
                            data.enforceNoDataAvail();
                            boolean _result60 = resetModemConfig(_arg066);
                            reply.writeNoException();
                            reply.writeBoolean(_result60);
                            break;
                        case 85:
                            int _arg067 = data.readInt();
                            data.enforceNoDataAvail();
                            boolean _result61 = rebootModem(_arg067);
                            reply.writeNoException();
                            reply.writeBoolean(_result61);
                            break;
                        case 86:
                            int _arg068 = data.readInt();
                            data.enforceNoDataAvail();
                            int _result62 = getAllowedNetworkTypesBitmask(_arg068);
                            reply.writeNoException();
                            reply.writeInt(_result62);
                            break;
                        case 87:
                            int _arg069 = data.readInt();
                            data.enforceNoDataAvail();
                            boolean _result63 = isTetheringApnRequiredForSubscriber(_arg069);
                            reply.writeNoException();
                            reply.writeBoolean(_result63);
                            break;
                        case 88:
                            int _arg070 = data.readInt();
                            data.enforceNoDataAvail();
                            enableIms(_arg070);
                            reply.writeNoException();
                            break;
                        case 89:
                            int _arg071 = data.readInt();
                            data.enforceNoDataAvail();
                            disableIms(_arg071);
                            reply.writeNoException();
                            break;
                        case 90:
                            int _arg072 = data.readInt();
                            data.enforceNoDataAvail();
                            resetIms(_arg072);
                            reply.writeNoException();
                            break;
                        case 91:
                            int _arg073 = data.readInt();
                            IImsServiceFeatureCallback _arg144 = IImsServiceFeatureCallback.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            registerMmTelFeatureCallback(_arg073, _arg144);
                            reply.writeNoException();
                            break;
                        case 92:
                            IImsServiceFeatureCallback _arg074 = IImsServiceFeatureCallback.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            unregisterImsFeatureCallback(_arg074);
                            reply.writeNoException();
                            break;
                        case 93:
                            int _arg075 = data.readInt();
                            int _arg145 = data.readInt();
                            data.enforceNoDataAvail();
                            IImsRegistration _result64 = getImsRegistration(_arg075, _arg145);
                            reply.writeNoException();
                            reply.writeStrongInterface(_result64);
                            break;
                        case 94:
                            int _arg076 = data.readInt();
                            int _arg146 = data.readInt();
                            data.enforceNoDataAvail();
                            IImsConfig _result65 = getImsConfig(_arg076, _arg146);
                            reply.writeNoException();
                            reply.writeStrongInterface(_result65);
                            break;
                        case 95:
                            return onTransact$setBoundImsServiceOverride$(data, reply);
                        case 96:
                            int _arg077 = data.readInt();
                            data.enforceNoDataAvail();
                            boolean _result66 = clearCarrierImsServiceOverride(_arg077);
                            reply.writeNoException();
                            reply.writeBoolean(_result66);
                            break;
                        case 97:
                            int _arg078 = data.readInt();
                            boolean _arg147 = data.readBoolean();
                            int _arg217 = data.readInt();
                            data.enforceNoDataAvail();
                            String _result67 = getBoundImsServicePackage(_arg078, _arg147, _arg217);
                            reply.writeNoException();
                            reply.writeString(_result67);
                            break;
                        case 98:
                            int _arg079 = data.readInt();
                            IIntegerConsumer _arg148 = IIntegerConsumer.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            getImsMmTelFeatureState(_arg079, _arg148);
                            reply.writeNoException();
                            break;
                        case 99:
                            int _arg080 = data.readInt();
                            data.enforceNoDataAvail();
                            setNetworkSelectionModeAutomatic(_arg080);
                            reply.writeNoException();
                            break;
                        case 100:
                            int _arg081 = data.readInt();
                            String _arg149 = data.readString();
                            String _arg218 = data.readString();
                            data.enforceNoDataAvail();
                            CellNetworkScanResult _result68 = getCellNetworkScanResults(_arg081, _arg149, _arg218);
                            reply.writeNoException();
                            reply.writeTypedObject(_result68, 1);
                            break;
                        case 101:
                            return onTransact$requestNetworkScan$(data, reply);
                        case 102:
                            int _arg082 = data.readInt();
                            int _arg150 = data.readInt();
                            data.enforceNoDataAvail();
                            stopNetworkScan(_arg082, _arg150);
                            reply.writeNoException();
                            break;
                        case 103:
                            int _arg083 = data.readInt();
                            OperatorInfo _arg151 = (OperatorInfo) data.readTypedObject(OperatorInfo.CREATOR);
                            boolean _arg219 = data.readBoolean();
                            data.enforceNoDataAvail();
                            boolean _result69 = setNetworkSelectionModeManual(_arg083, _arg151, _arg219);
                            reply.writeNoException();
                            reply.writeBoolean(_result69);
                            break;
                        case 104:
                            int _arg084 = data.readInt();
                            int _arg152 = data.readInt();
                            data.enforceNoDataAvail();
                            long _result70 = getAllowedNetworkTypesForReason(_arg084, _arg152);
                            reply.writeNoException();
                            reply.writeLong(_result70);
                            break;
                        case 105:
                            int _arg085 = data.readInt();
                            int _arg153 = data.readInt();
                            long _arg220 = data.readLong();
                            data.enforceNoDataAvail();
                            boolean _result71 = setAllowedNetworkTypesForReason(_arg085, _arg153, _arg220);
                            reply.writeNoException();
                            reply.writeBoolean(_result71);
                            break;
                        case 106:
                            int _arg086 = data.readInt();
                            data.enforceNoDataAvail();
                            boolean _result72 = getDataEnabled(_arg086);
                            reply.writeNoException();
                            reply.writeBoolean(_result72);
                            break;
                        case 107:
                            int _arg087 = data.readInt();
                            data.enforceNoDataAvail();
                            boolean _result73 = isUserDataEnabled(_arg087);
                            reply.writeNoException();
                            reply.writeBoolean(_result73);
                            break;
                        case 108:
                            int _arg088 = data.readInt();
                            data.enforceNoDataAvail();
                            boolean _result74 = isDataEnabled(_arg088);
                            reply.writeNoException();
                            reply.writeBoolean(_result74);
                            break;
                        case 109:
                            return onTransact$setDataEnabledForReason$(data, reply);
                        case 110:
                            int _arg089 = data.readInt();
                            int _arg154 = data.readInt();
                            data.enforceNoDataAvail();
                            boolean _result75 = isDataEnabledForReason(_arg089, _arg154);
                            reply.writeNoException();
                            reply.writeBoolean(_result75);
                            break;
                        case 111:
                            int _arg090 = data.readInt();
                            data.enforceNoDataAvail();
                            boolean _result76 = isManualNetworkSelectionAllowed(_arg090);
                            reply.writeNoException();
                            reply.writeBoolean(_result76);
                            break;
                        case 112:
                            boolean _arg091 = data.readBoolean();
                            data.enforceNoDataAvail();
                            setImsRegistrationState(_arg091);
                            reply.writeNoException();
                            break;
                        case 113:
                            int _arg092 = data.readInt();
                            data.enforceNoDataAvail();
                            String _result77 = getCdmaMdn(_arg092);
                            reply.writeNoException();
                            reply.writeString(_result77);
                            break;
                        case 114:
                            int _arg093 = data.readInt();
                            data.enforceNoDataAvail();
                            String _result78 = getCdmaMin(_arg093);
                            reply.writeNoException();
                            reply.writeString(_result78);
                            break;
                        case 115:
                            return onTransact$requestNumberVerification$(data, reply);
                        case 116:
                            int _arg094 = data.readInt();
                            data.enforceNoDataAvail();
                            int _result79 = getCarrierPrivilegeStatus(_arg094);
                            reply.writeNoException();
                            reply.writeInt(_result79);
                            break;
                        case 117:
                            int _arg095 = data.readInt();
                            int _arg155 = data.readInt();
                            data.enforceNoDataAvail();
                            int _result80 = getCarrierPrivilegeStatusForUid(_arg095, _arg155);
                            reply.writeNoException();
                            reply.writeInt(_result80);
                            break;
                        case 118:
                            int _arg096 = data.readInt();
                            String _arg156 = data.readString();
                            data.enforceNoDataAvail();
                            int _result81 = checkCarrierPrivilegesForPackage(_arg096, _arg156);
                            reply.writeNoException();
                            reply.writeInt(_result81);
                            break;
                        case 119:
                            String _arg097 = data.readString();
                            data.enforceNoDataAvail();
                            int _result82 = checkCarrierPrivilegesForPackageAnyPhone(_arg097);
                            reply.writeNoException();
                            reply.writeInt(_result82);
                            break;
                        case 120:
                            Intent _arg098 = (Intent) data.readTypedObject(Intent.CREATOR);
                            int _arg157 = data.readInt();
                            data.enforceNoDataAvail();
                            List<String> _result83 = getCarrierPackageNamesForIntentAndPhone(_arg098, _arg157);
                            reply.writeNoException();
                            reply.writeStringList(_result83);
                            break;
                        case 121:
                            int _arg099 = data.readInt();
                            String _arg158 = data.readString();
                            String _arg221 = data.readString();
                            data.enforceNoDataAvail();
                            boolean _result84 = setLine1NumberForDisplayForSubscriber(_arg099, _arg158, _arg221);
                            reply.writeNoException();
                            reply.writeBoolean(_result84);
                            break;
                        case 122:
                            int _arg0100 = data.readInt();
                            String _arg159 = data.readString();
                            String _arg222 = data.readString();
                            data.enforceNoDataAvail();
                            String _result85 = getLine1NumberForDisplay(_arg0100, _arg159, _arg222);
                            reply.writeNoException();
                            reply.writeString(_result85);
                            break;
                        case 123:
                            int _arg0101 = data.readInt();
                            String _arg160 = data.readString();
                            String _arg223 = data.readString();
                            data.enforceNoDataAvail();
                            String _result86 = getLine1AlphaTagForDisplay(_arg0101, _arg160, _arg223);
                            reply.writeNoException();
                            reply.writeString(_result86);
                            break;
                        case 124:
                            return onTransact$getMergedSubscriberIds$(data, reply);
                        case 125:
                            int _arg0102 = data.readInt();
                            String _arg161 = data.readString();
                            data.enforceNoDataAvail();
                            String[] _result87 = getMergedImsisFromGroup(_arg0102, _arg161);
                            reply.writeNoException();
                            reply.writeStringArray(_result87);
                            break;
                        case 126:
                            int _arg0103 = data.readInt();
                            String _arg162 = data.readString();
                            data.enforceNoDataAvail();
                            boolean _result88 = setOperatorBrandOverride(_arg0103, _arg162);
                            reply.writeNoException();
                            reply.writeBoolean(_result88);
                            break;
                        case 127:
                            return onTransact$setRoamingOverride$(data, reply);
                        case 128:
                            byte[] _arg0104 = data.createByteArray();
                            int _arg1_length = data.readInt();
                            if (_arg1_length < 0) {
                                _arg1 = null;
                            } else {
                                _arg1 = new byte[_arg1_length];
                            }
                            data.enforceNoDataAvail();
                            int _result89 = invokeOemRilRequestRaw(_arg0104, _arg1);
                            reply.writeNoException();
                            reply.writeInt(_result89);
                            reply.writeByteArray(_arg1);
                            break;
                        case 129:
                            boolean _result90 = needMobileRadioShutdown();
                            reply.writeNoException();
                            reply.writeBoolean(_result90);
                            break;
                        case 130:
                            shutdownMobileRadios();
                            reply.writeNoException();
                            break;
                        case 131:
                            int _arg0105 = data.readInt();
                            String _arg163 = data.readString();
                            data.enforceNoDataAvail();
                            int _result91 = getRadioAccessFamily(_arg0105, _arg163);
                            reply.writeNoException();
                            reply.writeInt(_result91);
                            break;
                        case 132:
                            return onTransact$uploadCallComposerPicture$(data, reply);
                        case 133:
                            boolean _arg0106 = data.readBoolean();
                            data.enforceNoDataAvail();
                            enableVideoCalling(_arg0106);
                            reply.writeNoException();
                            break;
                        case 134:
                            String _arg0107 = data.readString();
                            String _arg164 = data.readString();
                            data.enforceNoDataAvail();
                            boolean _result92 = isVideoCallingEnabled(_arg0107, _arg164);
                            reply.writeNoException();
                            reply.writeBoolean(_result92);
                            break;
                        case 135:
                            return onTransact$canChangeDtmfToneLength$(data, reply);
                        case 136:
                            return onTransact$isWorldPhone$(data, reply);
                        case 137:
                            boolean _result93 = isTtyModeSupported();
                            reply.writeNoException();
                            reply.writeBoolean(_result93);
                            break;
                        case 138:
                            int _arg0108 = data.readInt();
                            data.enforceNoDataAvail();
                            boolean _result94 = isRttSupported(_arg0108);
                            reply.writeNoException();
                            reply.writeBoolean(_result94);
                            break;
                        case 139:
                            boolean _result95 = isHearingAidCompatibilitySupported();
                            reply.writeNoException();
                            reply.writeBoolean(_result95);
                            break;
                        case 140:
                            int _arg0109 = data.readInt();
                            data.enforceNoDataAvail();
                            boolean _result96 = isImsRegistered(_arg0109);
                            reply.writeNoException();
                            reply.writeBoolean(_result96);
                            break;
                        case 141:
                            int _arg0110 = data.readInt();
                            data.enforceNoDataAvail();
                            boolean _result97 = isWifiCallingAvailable(_arg0110);
                            reply.writeNoException();
                            reply.writeBoolean(_result97);
                            break;
                        case 142:
                            int _arg0111 = data.readInt();
                            data.enforceNoDataAvail();
                            boolean _result98 = isVideoTelephonyAvailable(_arg0111);
                            reply.writeNoException();
                            reply.writeBoolean(_result98);
                            break;
                        case 143:
                            int _arg0112 = data.readInt();
                            data.enforceNoDataAvail();
                            int _result99 = getImsRegTechnologyForMmTel(_arg0112);
                            reply.writeNoException();
                            reply.writeInt(_result99);
                            break;
                        case 144:
                            String _arg0113 = data.readString();
                            data.enforceNoDataAvail();
                            String _result100 = getDeviceId(_arg0113);
                            reply.writeNoException();
                            reply.writeString(_result100);
                            break;
                        case 145:
                            String _arg0114 = data.readString();
                            String _arg165 = data.readString();
                            data.enforceNoDataAvail();
                            String _result101 = getDeviceIdWithFeature(_arg0114, _arg165);
                            reply.writeNoException();
                            reply.writeString(_result101);
                            break;
                        case 146:
                            return onTransact$getImeiForSlot$(data, reply);
                        case 147:
                            int _arg0115 = data.readInt();
                            data.enforceNoDataAvail();
                            String _result102 = getTypeAllocationCodeForSlot(_arg0115);
                            reply.writeNoException();
                            reply.writeString(_result102);
                            break;
                        case 148:
                            return onTransact$getMeidForSlot$(data, reply);
                        case 149:
                            int _arg0116 = data.readInt();
                            data.enforceNoDataAvail();
                            String _result103 = getManufacturerCodeForSlot(_arg0116);
                            reply.writeNoException();
                            reply.writeString(_result103);
                            break;
                        case 150:
                            return onTransact$getDeviceSoftwareVersionForSlot$(data, reply);
                        case 151:
                            return onTransact$getSubIdForPhoneAccountHandle$(data, reply);
                        case 152:
                            int _arg0117 = data.readInt();
                            data.enforceNoDataAvail();
                            PhoneAccountHandle _result104 = getPhoneAccountHandleForSubscriptionId(_arg0117);
                            reply.writeNoException();
                            reply.writeTypedObject(_result104, 1);
                            break;
                        case 153:
                            int _arg0118 = data.readInt();
                            String _arg166 = data.readString();
                            data.enforceNoDataAvail();
                            factoryReset(_arg0118, _arg166);
                            reply.writeNoException();
                            break;
                        case 154:
                            int _arg0119 = data.readInt();
                            data.enforceNoDataAvail();
                            String _result105 = getSimLocaleForSubscriber(_arg0119);
                            reply.writeNoException();
                            reply.writeString(_result105);
                            break;
                        case 155:
                            ResultReceiver _arg0120 = (ResultReceiver) data.readTypedObject(ResultReceiver.CREATOR);
                            data.enforceNoDataAvail();
                            requestModemActivityInfo(_arg0120);
                            break;
                        case 156:
                            return onTransact$getServiceStateForSubscriber$(data, reply);
                        case 157:
                            PhoneAccountHandle _arg0121 = (PhoneAccountHandle) data.readTypedObject(PhoneAccountHandle.CREATOR);
                            data.enforceNoDataAvail();
                            Uri _result106 = getVoicemailRingtoneUri(_arg0121);
                            reply.writeNoException();
                            reply.writeTypedObject(_result106, 1);
                            break;
                        case 158:
                            return onTransact$setVoicemailRingtoneUri$(data, reply);
                        case 159:
                            PhoneAccountHandle _arg0122 = (PhoneAccountHandle) data.readTypedObject(PhoneAccountHandle.CREATOR);
                            data.enforceNoDataAvail();
                            boolean _result107 = isVoicemailVibrationEnabled(_arg0122);
                            reply.writeNoException();
                            reply.writeBoolean(_result107);
                            break;
                        case 160:
                            return onTransact$setVoicemailVibrationEnabled$(data, reply);
                        case 161:
                            int _arg0123 = data.readInt();
                            data.enforceNoDataAvail();
                            List<String> _result108 = getPackagesWithCarrierPrivileges(_arg0123);
                            reply.writeNoException();
                            reply.writeStringList(_result108);
                            break;
                        case 162:
                            List<String> _result109 = getPackagesWithCarrierPrivilegesForAllPhones();
                            reply.writeNoException();
                            reply.writeStringList(_result109);
                            break;
                        case 163:
                            int _arg0124 = data.readInt();
                            int _arg167 = data.readInt();
                            data.enforceNoDataAvail();
                            String _result110 = getAidForAppType(_arg0124, _arg167);
                            reply.writeNoException();
                            reply.writeString(_result110);
                            break;
                        case 164:
                            int _arg0125 = data.readInt();
                            data.enforceNoDataAvail();
                            String _result111 = getEsn(_arg0125);
                            reply.writeNoException();
                            reply.writeString(_result111);
                            break;
                        case 165:
                            int _arg0126 = data.readInt();
                            data.enforceNoDataAvail();
                            String _result112 = getCdmaPrlVersion(_arg0126);
                            reply.writeNoException();
                            reply.writeString(_result112);
                            break;
                        case 166:
                            List<TelephonyHistogram> _result113 = getTelephonyHistograms();
                            reply.writeNoException();
                            reply.writeTypedList(_result113);
                            break;
                        case 167:
                            CarrierRestrictionRules _arg0127 = (CarrierRestrictionRules) data.readTypedObject(CarrierRestrictionRules.CREATOR);
                            data.enforceNoDataAvail();
                            int _result114 = setAllowedCarriers(_arg0127);
                            reply.writeNoException();
                            reply.writeInt(_result114);
                            break;
                        case 168:
                            CarrierRestrictionRules _result115 = getAllowedCarriers();
                            reply.writeNoException();
                            reply.writeTypedObject(_result115, 1);
                            break;
                        case 169:
                            int _arg0128 = data.readInt();
                            data.enforceNoDataAvail();
                            int _result116 = getSubscriptionCarrierId(_arg0128);
                            reply.writeNoException();
                            reply.writeInt(_result116);
                            break;
                        case 170:
                            int _arg0129 = data.readInt();
                            data.enforceNoDataAvail();
                            String _result117 = getSubscriptionCarrierName(_arg0129);
                            reply.writeNoException();
                            reply.writeString(_result117);
                            break;
                        case 171:
                            int _arg0130 = data.readInt();
                            data.enforceNoDataAvail();
                            int _result118 = getSubscriptionSpecificCarrierId(_arg0130);
                            reply.writeNoException();
                            reply.writeInt(_result118);
                            break;
                        case 172:
                            int _arg0131 = data.readInt();
                            data.enforceNoDataAvail();
                            String _result119 = getSubscriptionSpecificCarrierName(_arg0131);
                            reply.writeNoException();
                            reply.writeString(_result119);
                            break;
                        case 173:
                            return onTransact$getCarrierIdFromMccMnc$(data, reply);
                        case 174:
                            int _arg0132 = data.readInt();
                            boolean _arg168 = data.readBoolean();
                            data.enforceNoDataAvail();
                            carrierActionSetRadioEnabled(_arg0132, _arg168);
                            reply.writeNoException();
                            break;
                        case 175:
                            int _arg0133 = data.readInt();
                            boolean _arg169 = data.readBoolean();
                            data.enforceNoDataAvail();
                            carrierActionReportDefaultNetworkStatus(_arg0133, _arg169);
                            reply.writeNoException();
                            break;
                        case 176:
                            int _arg0134 = data.readInt();
                            data.enforceNoDataAvail();
                            carrierActionResetAll(_arg0134);
                            reply.writeNoException();
                            break;
                        case 177:
                            return onTransact$getCallForwarding$(data, reply);
                        case 178:
                            return onTransact$setCallForwarding$(data, reply);
                        case 179:
                            int _arg0135 = data.readInt();
                            IIntegerConsumer _arg170 = IIntegerConsumer.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            getCallWaitingStatus(_arg0135, _arg170);
                            reply.writeNoException();
                            break;
                        case 180:
                            return onTransact$setCallWaitingStatus$(data, reply);
                        case 181:
                            return onTransact$getClientRequestStats$(data, reply);
                        case 182:
                            int _arg0136 = data.readInt();
                            int _arg171 = data.readInt();
                            data.enforceNoDataAvail();
                            setSimPowerStateForSlot(_arg0136, _arg171);
                            reply.writeNoException();
                            break;
                        case 183:
                            return onTransact$setSimPowerStateForSlotWithCallback$(data, reply);
                        case 184:
                            return onTransact$getForbiddenPlmns$(data, reply);
                        case 185:
                            return onTransact$setForbiddenPlmns$(data, reply);
                        case 186:
                            int _arg0137 = data.readInt();
                            data.enforceNoDataAvail();
                            boolean _result120 = getEmergencyCallbackMode(_arg0137);
                            reply.writeNoException();
                            reply.writeBoolean(_result120);
                            break;
                        case 187:
                            int _arg0138 = data.readInt();
                            data.enforceNoDataAvail();
                            SignalStrength _result121 = getSignalStrength(_arg0138);
                            reply.writeNoException();
                            reply.writeTypedObject(_result121, 1);
                            break;
                        case 188:
                            int _arg0139 = data.readInt();
                            String _arg172 = data.readString();
                            data.enforceNoDataAvail();
                            int _result122 = getCardIdForDefaultEuicc(_arg0139, _arg172);
                            reply.writeNoException();
                            reply.writeInt(_result122);
                            break;
                        case 189:
                            String _arg0140 = data.readString();
                            data.enforceNoDataAvail();
                            List<UiccCardInfo> _result123 = getUiccCardsInfo(_arg0140);
                            reply.writeNoException();
                            reply.writeTypedList(_result123);
                            break;
                        case 190:
                            String _arg0141 = data.readString();
                            data.enforceNoDataAvail();
                            UiccSlotInfo[] _result124 = getUiccSlotsInfo(_arg0141);
                            reply.writeNoException();
                            reply.writeTypedArray(_result124, 1);
                            break;
                        case 191:
                            int[] _arg0142 = data.createIntArray();
                            data.enforceNoDataAvail();
                            boolean _result125 = switchSlots(_arg0142);
                            reply.writeNoException();
                            reply.writeBoolean(_result125);
                            break;
                        case 192:
                            List<UiccSlotMapping> _arg0143 = data.createTypedArrayList(UiccSlotMapping.CREATOR);
                            data.enforceNoDataAvail();
                            boolean _result126 = setSimSlotMapping(_arg0143);
                            reply.writeNoException();
                            reply.writeBoolean(_result126);
                            break;
                        case 193:
                            int _arg0144 = data.readInt();
                            data.enforceNoDataAvail();
                            boolean _result127 = isDataRoamingEnabled(_arg0144);
                            reply.writeNoException();
                            reply.writeBoolean(_result127);
                            break;
                        case 194:
                            int _arg0145 = data.readInt();
                            boolean _arg173 = data.readBoolean();
                            data.enforceNoDataAvail();
                            setDataRoamingEnabled(_arg0145, _arg173);
                            reply.writeNoException();
                            break;
                        case 195:
                            int _arg0146 = data.readInt();
                            data.enforceNoDataAvail();
                            int _result128 = getCdmaRoamingMode(_arg0146);
                            reply.writeNoException();
                            reply.writeInt(_result128);
                            break;
                        case 196:
                            int _arg0147 = data.readInt();
                            int _arg174 = data.readInt();
                            data.enforceNoDataAvail();
                            boolean _result129 = setCdmaRoamingMode(_arg0147, _arg174);
                            reply.writeNoException();
                            reply.writeBoolean(_result129);
                            break;
                        case 197:
                            int _arg0148 = data.readInt();
                            data.enforceNoDataAvail();
                            int _result130 = getCdmaSubscriptionMode(_arg0148);
                            reply.writeNoException();
                            reply.writeInt(_result130);
                            break;
                        case 198:
                            int _arg0149 = data.readInt();
                            int _arg175 = data.readInt();
                            data.enforceNoDataAvail();
                            boolean _result131 = setCdmaSubscriptionMode(_arg0149, _arg175);
                            reply.writeNoException();
                            reply.writeBoolean(_result131);
                            break;
                        case 199:
                            return onTransact$setCarrierTestOverride$(data, reply);
                        case 200:
                            int _arg0150 = data.readInt();
                            data.enforceNoDataAvail();
                            int _result132 = getCarrierIdListVersion(_arg0150);
                            reply.writeNoException();
                            reply.writeInt(_result132);
                            break;
                        case 201:
                            int _arg0151 = data.readInt();
                            data.enforceNoDataAvail();
                            refreshUiccProfile(_arg0151);
                            reply.writeNoException();
                            break;
                        case 202:
                            return onTransact$getNumberOfModemsWithSimultaneousDataConnections$(data, reply);
                        case 203:
                            int _arg0152 = data.readInt();
                            data.enforceNoDataAvail();
                            int _result133 = getNetworkSelectionMode(_arg0152);
                            reply.writeNoException();
                            reply.writeInt(_result133);
                            break;
                        case 204:
                            boolean _result134 = isInEmergencySmsMode();
                            reply.writeNoException();
                            reply.writeBoolean(_result134);
                            break;
                        case 205:
                            return onTransact$getRadioPowerState$(data, reply);
                        case 206:
                            int _arg0153 = data.readInt();
                            IImsRegistrationCallback _arg176 = IImsRegistrationCallback.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            registerImsRegistrationCallback(_arg0153, _arg176);
                            reply.writeNoException();
                            break;
                        case 207:
                            int _arg0154 = data.readInt();
                            IImsRegistrationCallback _arg177 = IImsRegistrationCallback.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            unregisterImsRegistrationCallback(_arg0154, _arg177);
                            reply.writeNoException();
                            break;
                        case 208:
                            int _arg0155 = data.readInt();
                            IIntegerConsumer _arg178 = IIntegerConsumer.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            getImsMmTelRegistrationState(_arg0155, _arg178);
                            reply.writeNoException();
                            break;
                        case 209:
                            int _arg0156 = data.readInt();
                            IIntegerConsumer _arg179 = IIntegerConsumer.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            getImsMmTelRegistrationTransportType(_arg0156, _arg179);
                            reply.writeNoException();
                            break;
                        case 210:
                            int _arg0157 = data.readInt();
                            IImsCapabilityCallback _arg180 = IImsCapabilityCallback.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            registerMmTelCapabilityCallback(_arg0157, _arg180);
                            reply.writeNoException();
                            break;
                        case 211:
                            int _arg0158 = data.readInt();
                            IImsCapabilityCallback _arg181 = IImsCapabilityCallback.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            unregisterMmTelCapabilityCallback(_arg0158, _arg181);
                            reply.writeNoException();
                            break;
                        case 212:
                            return onTransact$isCapable$(data, reply);
                        case 213:
                            return onTransact$isAvailable$(data, reply);
                        case 214:
                            return onTransact$isMmTelCapabilitySupported$(data, reply);
                        case 215:
                            int _arg0159 = data.readInt();
                            data.enforceNoDataAvail();
                            boolean _result135 = isAdvancedCallingSettingEnabled(_arg0159);
                            reply.writeNoException();
                            reply.writeBoolean(_result135);
                            break;
                        case 216:
                            int _arg0160 = data.readInt();
                            boolean _arg182 = data.readBoolean();
                            data.enforceNoDataAvail();
                            setAdvancedCallingSettingEnabled(_arg0160, _arg182);
                            reply.writeNoException();
                            break;
                        case 217:
                            int _arg0161 = data.readInt();
                            data.enforceNoDataAvail();
                            boolean _result136 = isVtSettingEnabled(_arg0161);
                            reply.writeNoException();
                            reply.writeBoolean(_result136);
                            break;
                        case 218:
                            int _arg0162 = data.readInt();
                            boolean _arg183 = data.readBoolean();
                            data.enforceNoDataAvail();
                            setVtSettingEnabled(_arg0162, _arg183);
                            reply.writeNoException();
                            break;
                        case 219:
                            int _arg0163 = data.readInt();
                            data.enforceNoDataAvail();
                            boolean _result137 = isVoWiFiSettingEnabled(_arg0163);
                            reply.writeNoException();
                            reply.writeBoolean(_result137);
                            break;
                        case 220:
                            int _arg0164 = data.readInt();
                            boolean _arg184 = data.readBoolean();
                            data.enforceNoDataAvail();
                            setVoWiFiSettingEnabled(_arg0164, _arg184);
                            reply.writeNoException();
                            break;
                        case 221:
                            int _arg0165 = data.readInt();
                            data.enforceNoDataAvail();
                            boolean _result138 = isCrossSimCallingEnabledByUser(_arg0165);
                            reply.writeNoException();
                            reply.writeBoolean(_result138);
                            break;
                        case 222:
                            int _arg0166 = data.readInt();
                            boolean _arg185 = data.readBoolean();
                            data.enforceNoDataAvail();
                            setCrossSimCallingEnabled(_arg0166, _arg185);
                            reply.writeNoException();
                            break;
                        case 223:
                            int _arg0167 = data.readInt();
                            data.enforceNoDataAvail();
                            boolean _result139 = isVoWiFiRoamingSettingEnabled(_arg0167);
                            reply.writeNoException();
                            reply.writeBoolean(_result139);
                            break;
                        case 224:
                            int _arg0168 = data.readInt();
                            boolean _arg186 = data.readBoolean();
                            data.enforceNoDataAvail();
                            setVoWiFiRoamingSettingEnabled(_arg0168, _arg186);
                            reply.writeNoException();
                            break;
                        case 225:
                            return onTransact$setVoWiFiNonPersistent$(data, reply);
                        case 226:
                            int _arg0169 = data.readInt();
                            data.enforceNoDataAvail();
                            int _result140 = getVoWiFiModeSetting(_arg0169);
                            reply.writeNoException();
                            reply.writeInt(_result140);
                            break;
                        case 227:
                            int _arg0170 = data.readInt();
                            int _arg187 = data.readInt();
                            data.enforceNoDataAvail();
                            setVoWiFiModeSetting(_arg0170, _arg187);
                            reply.writeNoException();
                            break;
                        case 228:
                            int _arg0171 = data.readInt();
                            data.enforceNoDataAvail();
                            int _result141 = getVoWiFiRoamingModeSetting(_arg0171);
                            reply.writeNoException();
                            reply.writeInt(_result141);
                            break;
                        case 229:
                            int _arg0172 = data.readInt();
                            int _arg188 = data.readInt();
                            data.enforceNoDataAvail();
                            setVoWiFiRoamingModeSetting(_arg0172, _arg188);
                            reply.writeNoException();
                            break;
                        case 230:
                            int _arg0173 = data.readInt();
                            boolean _arg189 = data.readBoolean();
                            data.enforceNoDataAvail();
                            setRttCapabilitySetting(_arg0173, _arg189);
                            reply.writeNoException();
                            break;
                        case 231:
                            int _arg0174 = data.readInt();
                            data.enforceNoDataAvail();
                            boolean _result142 = isTtyOverVolteEnabled(_arg0174);
                            reply.writeNoException();
                            reply.writeBoolean(_result142);
                            break;
                        case 232:
                            String _arg0175 = data.readString();
                            String _arg190 = data.readString();
                            data.enforceNoDataAvail();
                            Map _result143 = getEmergencyNumberList(_arg0175, _arg190);
                            reply.writeNoException();
                            reply.writeMap(_result143);
                            break;
                        case 233:
                            String _arg0176 = data.readString();
                            boolean _arg191 = data.readBoolean();
                            data.enforceNoDataAvail();
                            boolean _result144 = isEmergencyNumber(_arg0176, _arg191);
                            reply.writeNoException();
                            reply.writeBoolean(_result144);
                            break;
                        case 234:
                            int _arg0177 = data.readInt();
                            data.enforceNoDataAvail();
                            List<String> _result145 = getCertsFromCarrierPrivilegeAccessRules(_arg0177);
                            reply.writeNoException();
                            reply.writeStringList(_result145);
                            break;
                        case 235:
                            int _arg0178 = data.readInt();
                            IImsConfigCallback _arg192 = IImsConfigCallback.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            registerImsProvisioningChangedCallback(_arg0178, _arg192);
                            reply.writeNoException();
                            break;
                        case 236:
                            int _arg0179 = data.readInt();
                            IImsConfigCallback _arg193 = IImsConfigCallback.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            unregisterImsProvisioningChangedCallback(_arg0179, _arg193);
                            reply.writeNoException();
                            break;
                        case 237:
                            int _arg0180 = data.readInt();
                            IFeatureProvisioningCallback _arg194 = IFeatureProvisioningCallback.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            registerFeatureProvisioningChangedCallback(_arg0180, _arg194);
                            reply.writeNoException();
                            break;
                        case 238:
                            int _arg0181 = data.readInt();
                            IFeatureProvisioningCallback _arg195 = IFeatureProvisioningCallback.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            unregisterFeatureProvisioningChangedCallback(_arg0181, _arg195);
                            reply.writeNoException();
                            break;
                        case 239:
                            return onTransact$setImsProvisioningStatusForCapability$(data, reply);
                        case 240:
                            return onTransact$getImsProvisioningStatusForCapability$(data, reply);
                        case 241:
                            return onTransact$getRcsProvisioningStatusForCapability$(data, reply);
                        case 242:
                            return onTransact$setRcsProvisioningStatusForCapability$(data, reply);
                        case 243:
                            int _arg0182 = data.readInt();
                            int _arg196 = data.readInt();
                            data.enforceNoDataAvail();
                            int _result146 = getImsProvisioningInt(_arg0182, _arg196);
                            reply.writeNoException();
                            reply.writeInt(_result146);
                            break;
                        case 244:
                            int _arg0183 = data.readInt();
                            int _arg197 = data.readInt();
                            data.enforceNoDataAvail();
                            String _result147 = getImsProvisioningString(_arg0183, _arg197);
                            reply.writeNoException();
                            reply.writeString(_result147);
                            break;
                        case 245:
                            return onTransact$setImsProvisioningInt$(data, reply);
                        case 246:
                            return onTransact$setImsProvisioningString$(data, reply);
                        case 247:
                            startEmergencyCallbackMode();
                            reply.writeNoException();
                            break;
                        case 248:
                            int _arg0184 = data.readInt();
                            EmergencyNumber _arg198 = (EmergencyNumber) data.readTypedObject(EmergencyNumber.CREATOR);
                            data.enforceNoDataAvail();
                            updateEmergencyNumberListTestMode(_arg0184, _arg198);
                            reply.writeNoException();
                            break;
                        case 249:
                            List<String> _result148 = getEmergencyNumberListTestMode();
                            reply.writeNoException();
                            reply.writeStringList(_result148);
                            break;
                        case 250:
                            int _arg0185 = data.readInt();
                            data.enforceNoDataAvail();
                            int _result149 = getEmergencyNumberDbVersion(_arg0185);
                            reply.writeNoException();
                            reply.writeInt(_result149);
                            break;
                        case 251:
                            notifyOtaEmergencyNumberDbInstalled();
                            reply.writeNoException();
                            break;
                        case 252:
                            ParcelFileDescriptor _arg0186 = (ParcelFileDescriptor) data.readTypedObject(ParcelFileDescriptor.CREATOR);
                            data.enforceNoDataAvail();
                            updateOtaEmergencyNumberDbFilePath(_arg0186);
                            reply.writeNoException();
                            break;
                        case 253:
                            resetOtaEmergencyNumberDbFilePath();
                            reply.writeNoException();
                            break;
                        case 254:
                            int _arg0187 = data.readInt();
                            boolean _arg199 = data.readBoolean();
                            data.enforceNoDataAvail();
                            boolean _result150 = enableModemForSlot(_arg0187, _arg199);
                            reply.writeNoException();
                            reply.writeBoolean(_result150);
                            break;
                        case 255:
                            boolean _arg0188 = data.readBoolean();
                            data.enforceNoDataAvail();
                            setMultiSimCarrierRestriction(_arg0188);
                            reply.writeNoException();
                            break;
                        case 256:
                            String _arg0189 = data.readString();
                            String _arg1100 = data.readString();
                            data.enforceNoDataAvail();
                            int _result151 = isMultiSimSupported(_arg0189, _arg1100);
                            reply.writeNoException();
                            reply.writeInt(_result151);
                            break;
                        case 257:
                            int _arg0190 = data.readInt();
                            data.enforceNoDataAvail();
                            switchMultiSimConfig(_arg0190);
                            reply.writeNoException();
                            break;
                        case 258:
                            return onTransact$doesSwitchMultiSimConfigTriggerReboot$(data, reply);
                        case 259:
                            String _arg0191 = data.readString();
                            data.enforceNoDataAvail();
                            List<UiccSlotMapping> _result152 = getSlotsMapping(_arg0191);
                            reply.writeNoException();
                            reply.writeTypedList(_result152);
                            break;
                        case 260:
                            int _result153 = getRadioHalVersion();
                            reply.writeNoException();
                            reply.writeInt(_result153);
                            break;
                        case 261:
                            String _result154 = getCurrentPackageName();
                            reply.writeNoException();
                            reply.writeString(_result154);
                            break;
                        case 262:
                            int _arg0192 = data.readInt();
                            int _arg1101 = data.readInt();
                            data.enforceNoDataAvail();
                            boolean _result155 = isApplicationOnUicc(_arg0192, _arg1101);
                            reply.writeNoException();
                            reply.writeBoolean(_result155);
                            break;
                        case 263:
                            return onTransact$isModemEnabledForSlot$(data, reply);
                        case 264:
                            return onTransact$isDataEnabledForApn$(data, reply);
                        case 265:
                            int _arg0193 = data.readInt();
                            int _arg1102 = data.readInt();
                            data.enforceNoDataAvail();
                            boolean _result156 = isApnMetered(_arg0193, _arg1102);
                            reply.writeNoException();
                            reply.writeBoolean(_result156);
                            break;
                        case 266:
                            return onTransact$setSystemSelectionChannels$(data, reply);
                        case 267:
                            int _arg0194 = data.readInt();
                            data.enforceNoDataAvail();
                            List<RadioAccessSpecifier> _result157 = getSystemSelectionChannels(_arg0194);
                            reply.writeNoException();
                            reply.writeTypedList(_result157);
                            break;
                        case 268:
                            return onTransact$isMvnoMatched$(data, reply);
                        case 269:
                            return onTransact$enqueueSmsPickResult$(data, reply);
                        case 270:
                            int _arg0195 = data.readInt();
                            data.enforceNoDataAvail();
                            String _result158 = getMmsUserAgent(_arg0195);
                            reply.writeNoException();
                            reply.writeString(_result158);
                            break;
                        case 271:
                            int _arg0196 = data.readInt();
                            data.enforceNoDataAvail();
                            String _result159 = getMmsUAProfUrl(_arg0196);
                            reply.writeNoException();
                            reply.writeString(_result159);
                            break;
                        case 272:
                            return onTransact$setMobileDataPolicyEnabled$(data, reply);
                        case 273:
                            int _arg0197 = data.readInt();
                            int _arg1103 = data.readInt();
                            data.enforceNoDataAvail();
                            boolean _result160 = isMobileDataPolicyEnabled(_arg0197, _arg1103);
                            reply.writeNoException();
                            reply.writeBoolean(_result160);
                            break;
                        case 274:
                            boolean _arg0198 = data.readBoolean();
                            data.enforceNoDataAvail();
                            setCepEnabled(_arg0198);
                            break;
                        case 275:
                            return onTransact$notifyRcsAutoConfigurationReceived$(data, reply);
                        case 276:
                            int _arg0199 = data.readInt();
                            data.enforceNoDataAvail();
                            boolean _result161 = isIccLockEnabled(_arg0199);
                            reply.writeNoException();
                            reply.writeBoolean(_result161);
                            break;
                        case 277:
                            return onTransact$setIccLockEnabled$(data, reply);
                        case 278:
                            return onTransact$changeIccLockPassword$(data, reply);
                        case 279:
                            requestUserActivityNotification();
                            break;
                        case 280:
                            userActivity();
                            break;
                        case 281:
                            int _arg0200 = data.readInt();
                            data.enforceNoDataAvail();
                            String _result162 = getManualNetworkSelectionPlmn(_arg0200);
                            reply.writeNoException();
                            reply.writeString(_result162);
                            break;
                        case 282:
                            boolean _result163 = canConnectTo5GInDsdsMode();
                            reply.writeNoException();
                            reply.writeBoolean(_result163);
                            break;
                        case 283:
                            return onTransact$getEquivalentHomePlmns$(data, reply);
                        case 284:
                            int _arg0201 = data.readInt();
                            boolean _arg1104 = data.readBoolean();
                            data.enforceNoDataAvail();
                            int _result164 = setVoNrEnabled(_arg0201, _arg1104);
                            reply.writeNoException();
                            reply.writeInt(_result164);
                            break;
                        case 285:
                            int _arg0202 = data.readInt();
                            data.enforceNoDataAvail();
                            boolean _result165 = isVoNrEnabled(_arg0202);
                            reply.writeNoException();
                            reply.writeBoolean(_result165);
                            break;
                        case 286:
                            int _arg0203 = data.readInt();
                            int _arg1105 = data.readInt();
                            data.enforceNoDataAvail();
                            int _result166 = setNrDualConnectivityState(_arg0203, _arg1105);
                            reply.writeNoException();
                            reply.writeInt(_result166);
                            break;
                        case 287:
                            int _arg0204 = data.readInt();
                            data.enforceNoDataAvail();
                            boolean _result167 = isNrDualConnectivityEnabled(_arg0204);
                            reply.writeNoException();
                            reply.writeBoolean(_result167);
                            break;
                        case 288:
                            String _arg0205 = data.readString();
                            data.enforceNoDataAvail();
                            boolean _result168 = isRadioInterfaceCapabilitySupported(_arg0205);
                            reply.writeNoException();
                            reply.writeBoolean(_result168);
                            break;
                        case 289:
                            return onTransact$sendThermalMitigationRequest$(data, reply);
                        case 290:
                            return onTransact$bootstrapAuthenticationRequest$(data, reply);
                        case 291:
                            int _arg0206 = data.readInt();
                            String _arg1106 = data.readString();
                            data.enforceNoDataAvail();
                            boolean _result169 = setBoundGbaServiceOverride(_arg0206, _arg1106);
                            reply.writeNoException();
                            reply.writeBoolean(_result169);
                            break;
                        case 292:
                            int _arg0207 = data.readInt();
                            data.enforceNoDataAvail();
                            String _result170 = getBoundGbaService(_arg0207);
                            reply.writeNoException();
                            reply.writeString(_result170);
                            break;
                        case 293:
                            int _arg0208 = data.readInt();
                            int _arg1107 = data.readInt();
                            data.enforceNoDataAvail();
                            boolean _result171 = setGbaReleaseTimeOverride(_arg0208, _arg1107);
                            reply.writeNoException();
                            reply.writeBoolean(_result171);
                            break;
                        case 294:
                            int _arg0209 = data.readInt();
                            data.enforceNoDataAvail();
                            int _result172 = getGbaReleaseTime(_arg0209);
                            reply.writeNoException();
                            reply.writeInt(_result172);
                            break;
                        case 295:
                            int _arg0210 = data.readInt();
                            RcsClientConfiguration _arg1108 = (RcsClientConfiguration) data.readTypedObject(RcsClientConfiguration.CREATOR);
                            data.enforceNoDataAvail();
                            setRcsClientConfiguration(_arg0210, _arg1108);
                            reply.writeNoException();
                            break;
                        case 296:
                            int _arg0211 = data.readInt();
                            data.enforceNoDataAvail();
                            boolean _result173 = isRcsVolteSingleRegistrationCapable(_arg0211);
                            reply.writeNoException();
                            reply.writeBoolean(_result173);
                            break;
                        case 297:
                            int _arg0212 = data.readInt();
                            IRcsConfigCallback _arg1109 = IRcsConfigCallback.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            registerRcsProvisioningCallback(_arg0212, _arg1109);
                            reply.writeNoException();
                            break;
                        case 298:
                            int _arg0213 = data.readInt();
                            IRcsConfigCallback _arg1110 = IRcsConfigCallback.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            unregisterRcsProvisioningCallback(_arg0213, _arg1110);
                            reply.writeNoException();
                            break;
                        case 299:
                            int _arg0214 = data.readInt();
                            data.enforceNoDataAvail();
                            triggerRcsReconfiguration(_arg0214);
                            reply.writeNoException();
                            break;
                        case 300:
                            boolean _arg0215 = data.readBoolean();
                            data.enforceNoDataAvail();
                            setRcsSingleRegistrationTestModeEnabled(_arg0215);
                            reply.writeNoException();
                            break;
                        case 301:
                            boolean _result174 = getRcsSingleRegistrationTestModeEnabled();
                            reply.writeNoException();
                            reply.writeBoolean(_result174);
                            break;
                        case 302:
                            String _arg0216 = data.readString();
                            data.enforceNoDataAvail();
                            setDeviceSingleRegistrationEnabledOverride(_arg0216);
                            reply.writeNoException();
                            break;
                        case 303:
                            boolean _result175 = getDeviceSingleRegistrationEnabled();
                            reply.writeNoException();
                            reply.writeBoolean(_result175);
                            break;
                        case 304:
                            int _arg0217 = data.readInt();
                            String _arg1111 = data.readString();
                            data.enforceNoDataAvail();
                            boolean _result176 = setCarrierSingleRegistrationEnabledOverride(_arg0217, _arg1111);
                            reply.writeNoException();
                            reply.writeBoolean(_result176);
                            break;
                        case 305:
                            int _arg0218 = data.readInt();
                            int _arg1112 = data.readInt();
                            data.enforceNoDataAvail();
                            sendDeviceToDeviceMessage(_arg0218, _arg1112);
                            reply.writeNoException();
                            break;
                        case 306:
                            String _arg0219 = data.readString();
                            data.enforceNoDataAvail();
                            setActiveDeviceToDeviceTransport(_arg0219);
                            reply.writeNoException();
                            break;
                        case 307:
                            boolean _arg0220 = data.readBoolean();
                            data.enforceNoDataAvail();
                            setDeviceToDeviceForceEnabled(_arg0220);
                            reply.writeNoException();
                            break;
                        case 308:
                            int _arg0221 = data.readInt();
                            data.enforceNoDataAvail();
                            boolean _result177 = getCarrierSingleRegistrationEnabled(_arg0221);
                            reply.writeNoException();
                            reply.writeBoolean(_result177);
                            break;
                        case 309:
                            int _arg0222 = data.readInt();
                            String _arg1113 = data.readString();
                            data.enforceNoDataAvail();
                            boolean _result178 = setImsFeatureValidationOverride(_arg0222, _arg1113);
                            reply.writeNoException();
                            reply.writeBoolean(_result178);
                            break;
                        case 310:
                            int _arg0223 = data.readInt();
                            data.enforceNoDataAvail();
                            boolean _result179 = getImsFeatureValidationOverride(_arg0223);
                            reply.writeNoException();
                            reply.writeBoolean(_result179);
                            break;
                        case 311:
                            String _result180 = getMobileProvisioningUrl();
                            reply.writeNoException();
                            reply.writeString(_result180);
                            break;
                        case 312:
                            int _arg0224 = data.readInt();
                            String _arg1114 = data.readString();
                            data.enforceNoDataAvail();
                            int _result181 = removeContactFromEab(_arg0224, _arg1114);
                            reply.writeNoException();
                            reply.writeInt(_result181);
                            break;
                        case 313:
                            String _arg0225 = data.readString();
                            data.enforceNoDataAvail();
                            String _result182 = getContactFromEab(_arg0225);
                            reply.writeNoException();
                            reply.writeString(_result182);
                            break;
                        case 314:
                            String _arg0226 = data.readString();
                            data.enforceNoDataAvail();
                            String _result183 = getCapabilityFromEab(_arg0226);
                            reply.writeNoException();
                            reply.writeString(_result183);
                            break;
                        case 315:
                            boolean _result184 = getDeviceUceEnabled();
                            reply.writeNoException();
                            reply.writeBoolean(_result184);
                            break;
                        case 316:
                            boolean _arg0227 = data.readBoolean();
                            data.enforceNoDataAvail();
                            setDeviceUceEnabled(_arg0227);
                            reply.writeNoException();
                            break;
                        case 317:
                            int _arg0228 = data.readInt();
                            List<String> _arg1115 = data.createStringArrayList();
                            data.enforceNoDataAvail();
                            RcsContactUceCapability _result185 = addUceRegistrationOverrideShell(_arg0228, _arg1115);
                            reply.writeNoException();
                            reply.writeTypedObject(_result185, 1);
                            break;
                        case 318:
                            int _arg0229 = data.readInt();
                            List<String> _arg1116 = data.createStringArrayList();
                            data.enforceNoDataAvail();
                            RcsContactUceCapability _result186 = removeUceRegistrationOverrideShell(_arg0229, _arg1116);
                            reply.writeNoException();
                            reply.writeTypedObject(_result186, 1);
                            break;
                        case 319:
                            int _arg0230 = data.readInt();
                            data.enforceNoDataAvail();
                            RcsContactUceCapability _result187 = clearUceRegistrationOverrideShell(_arg0230);
                            reply.writeNoException();
                            reply.writeTypedObject(_result187, 1);
                            break;
                        case 320:
                            int _arg0231 = data.readInt();
                            data.enforceNoDataAvail();
                            RcsContactUceCapability _result188 = getLatestRcsContactUceCapabilityShell(_arg0231);
                            reply.writeNoException();
                            reply.writeTypedObject(_result188, 1);
                            break;
                        case 321:
                            int _arg0232 = data.readInt();
                            data.enforceNoDataAvail();
                            String _result189 = getLastUcePidfXmlShell(_arg0232);
                            reply.writeNoException();
                            reply.writeString(_result189);
                            break;
                        case 322:
                            int _arg0233 = data.readInt();
                            data.enforceNoDataAvail();
                            boolean _result190 = removeUceRequestDisallowedStatus(_arg0233);
                            reply.writeNoException();
                            reply.writeBoolean(_result190);
                            break;
                        case 323:
                            int _arg0234 = data.readInt();
                            long _arg1117 = data.readLong();
                            data.enforceNoDataAvail();
                            boolean _result191 = setCapabilitiesRequestTimeout(_arg0234, _arg1117);
                            reply.writeNoException();
                            reply.writeBoolean(_result191);
                            break;
                        case 324:
                            return onTransact$setSignalStrengthUpdateRequest$(data, reply);
                        case 325:
                            return onTransact$clearSignalStrengthUpdateRequest$(data, reply);
                        case 326:
                            PhoneCapability _result192 = getPhoneCapability();
                            reply.writeNoException();
                            reply.writeTypedObject(_result192, 1);
                            break;
                        case 327:
                            int _result193 = prepareForUnattendedReboot();
                            reply.writeNoException();
                            reply.writeInt(_result193);
                            break;
                        case 328:
                            ResultReceiver _arg0235 = (ResultReceiver) data.readTypedObject(ResultReceiver.CREATOR);
                            data.enforceNoDataAvail();
                            getSlicingConfig(_arg0235);
                            reply.writeNoException();
                            break;
                        case 329:
                            return onTransact$registerImsStateCallback$(data, reply);
                        case 330:
                            IImsStateCallback _arg0236 = IImsStateCallback.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            unregisterImsStateCallback(_arg0236);
                            reply.writeNoException();
                            break;
                        case 331:
                            return onTransact$getLastKnownCellIdentity$(data, reply);
                        case 332:
                            boolean _result194 = isUsingNewDataStack();
                            reply.writeNoException();
                            reply.writeBoolean(_result194);
                            break;
                        case 333:
                            String _arg0237 = data.readString();
                            data.enforceNoDataAvail();
                            boolean _result195 = setModemService(_arg0237);
                            reply.writeNoException();
                            reply.writeBoolean(_result195);
                            break;
                        case 334:
                            String _result196 = getModemService();
                            reply.writeNoException();
                            reply.writeString(_result196);
                            break;
                        case 335:
                            return onTransact$isProvisioningRequiredForCapability$(data, reply);
                        case 336:
                            return onTransact$isRcsProvisioningRequiredForCapability$(data, reply);
                        case 337:
                            return onTransact$setVoiceServiceStateOverride$(data, reply);
                        case 338:
                            int _arg0238 = data.readInt();
                            data.enforceNoDataAvail();
                            String _result197 = getCarrierServicePackageNameForLogicalSlot(_arg0238);
                            reply.writeNoException();
                            reply.writeString(_result197);
                            break;
                        case 339:
                            boolean _arg0239 = data.readBoolean();
                            String _arg1118 = data.readString();
                            data.enforceNoDataAvail();
                            setRemovableEsimAsDefaultEuicc(_arg0239, _arg1118);
                            reply.writeNoException();
                            break;
                        case 340:
                            String _arg0240 = data.readString();
                            data.enforceNoDataAvail();
                            boolean _result198 = isRemovableEsimDefaultEuicc(_arg0240);
                            reply.writeNoException();
                            reply.writeBoolean(_result198);
                            break;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
                    return true;
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        /* loaded from: classes4.dex */
        public static class Proxy implements ITelephony {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return Stub.DESCRIPTOR;
            }

            @Override // com.android.internal.telephony.ITelephony
            public void dial(String number) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(number);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public void call(String callingPackage, String number) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    _data.writeString(number);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public boolean isRadioOn(String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public boolean isRadioOnWithFeature(String callingPackage, String callingFeatureId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    _data.writeString(callingFeatureId);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public boolean isRadioOnForSubscriber(int subId, String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public boolean isRadioOnForSubscriberWithFeature(int subId, String callingPackage, String callingFeatureId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(callingPackage);
                    _data.writeString(callingFeatureId);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public void setCallComposerStatus(int subId, int status) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeInt(status);
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public int getCallComposerStatus(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public boolean supplyPinForSubscriber(int subId, String pin) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(pin);
                    this.mRemote.transact(9, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public boolean supplyPukForSubscriber(int subId, String puk, String pin) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(puk);
                    _data.writeString(pin);
                    this.mRemote.transact(10, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public int[] supplyPinReportResultForSubscriber(int subId, String pin) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(pin);
                    this.mRemote.transact(11, _data, _reply, 0);
                    _reply.readException();
                    int[] _result = _reply.createIntArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public int[] supplyPukReportResultForSubscriber(int subId, String puk, String pin) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(puk);
                    _data.writeString(pin);
                    this.mRemote.transact(12, _data, _reply, 0);
                    _reply.readException();
                    int[] _result = _reply.createIntArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public boolean handlePinMmi(String dialString) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(dialString);
                    this.mRemote.transact(13, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public void handleUssdRequest(int subId, String ussdRequest, ResultReceiver wrappedCallback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(ussdRequest);
                    _data.writeTypedObject(wrappedCallback, 0);
                    this.mRemote.transact(14, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public boolean handlePinMmiForSubscriber(int subId, String dialString) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(dialString);
                    this.mRemote.transact(15, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public void toggleRadioOnOff() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(16, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public void toggleRadioOnOffForSubscriber(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(17, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public boolean setRadio(boolean turnOn) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeBoolean(turnOn);
                    this.mRemote.transact(18, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public boolean setRadioForSubscriber(int subId, boolean turnOn) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeBoolean(turnOn);
                    this.mRemote.transact(19, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public boolean setRadioPower(boolean turnOn) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeBoolean(turnOn);
                    this.mRemote.transact(20, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public void updateServiceLocation() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(21, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public void updateServiceLocationWithPackageName(String callingPkg) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPkg);
                    this.mRemote.transact(22, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public void enableLocationUpdates() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(23, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public void disableLocationUpdates() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(24, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public boolean enableDataConnectivity(String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(25, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public boolean disableDataConnectivity(String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(26, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public boolean isDataConnectivityPossible(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(27, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public CellIdentity getCellLocation(String callingPkg, String callingFeatureId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPkg);
                    _data.writeString(callingFeatureId);
                    this.mRemote.transact(28, _data, _reply, 0);
                    _reply.readException();
                    CellIdentity _result = (CellIdentity) _reply.readTypedObject(CellIdentity.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public String getNetworkCountryIsoForPhone(int phoneId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(phoneId);
                    this.mRemote.transact(29, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public List<NeighboringCellInfo> getNeighboringCellInfo(String callingPkg, String callingFeatureId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPkg);
                    _data.writeString(callingFeatureId);
                    this.mRemote.transact(30, _data, _reply, 0);
                    _reply.readException();
                    List<NeighboringCellInfo> _result = _reply.createTypedArrayList(NeighboringCellInfo.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public int getCallState() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(31, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public int getCallStateForSubscription(int subId, String callingPackage, String featureId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(callingPackage);
                    _data.writeString(featureId);
                    this.mRemote.transact(32, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public int getDataActivity() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(33, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public int getDataActivityForSubId(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(34, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public int getDataState() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(35, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public int getDataStateForSubId(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(36, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public int getActivePhoneType() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(37, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public int getActivePhoneTypeForSlot(int slotIndex) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(slotIndex);
                    this.mRemote.transact(38, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public int getCdmaEriIconIndex(String callingPackage, String callingFeatureId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    _data.writeString(callingFeatureId);
                    this.mRemote.transact(39, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public int getCdmaEriIconIndexForSubscriber(int subId, String callingPackage, String callingFeatureId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(callingPackage);
                    _data.writeString(callingFeatureId);
                    this.mRemote.transact(40, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public int getCdmaEriIconMode(String callingPackage, String callingFeatureId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    _data.writeString(callingFeatureId);
                    this.mRemote.transact(41, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public int getCdmaEriIconModeForSubscriber(int subId, String callingPackage, String callingFeatureId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(callingPackage);
                    _data.writeString(callingFeatureId);
                    this.mRemote.transact(42, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public String getCdmaEriText(String callingPackage, String callingFeatureId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    _data.writeString(callingFeatureId);
                    this.mRemote.transact(43, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public String getCdmaEriTextForSubscriber(int subId, String callingPackage, String callingFeatureId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(callingPackage);
                    _data.writeString(callingFeatureId);
                    this.mRemote.transact(44, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public boolean needsOtaServiceProvisioning() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(45, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public boolean setVoiceMailNumber(int subId, String alphaTag, String number) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(alphaTag);
                    _data.writeString(number);
                    this.mRemote.transact(46, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public void setVoiceActivationState(int subId, int activationState) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeInt(activationState);
                    this.mRemote.transact(47, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public void setDataActivationState(int subId, int activationState) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeInt(activationState);
                    this.mRemote.transact(48, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public int getVoiceActivationState(int subId, String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(49, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public int getDataActivationState(int subId, String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(50, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public int getVoiceMessageCountForSubscriber(int subId, String callingPackage, String callingFeatureId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(callingPackage);
                    _data.writeString(callingFeatureId);
                    this.mRemote.transact(51, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public boolean isConcurrentVoiceAndDataAllowed(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(52, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public Bundle getVisualVoicemailSettings(String callingPackage, int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    _data.writeInt(subId);
                    this.mRemote.transact(53, _data, _reply, 0);
                    _reply.readException();
                    Bundle _result = (Bundle) _reply.readTypedObject(Bundle.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public String getVisualVoicemailPackageName(String callingPackage, String callingFeatureId, int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    _data.writeString(callingFeatureId);
                    _data.writeInt(subId);
                    this.mRemote.transact(54, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public void enableVisualVoicemailSmsFilter(String callingPackage, int subId, VisualVoicemailSmsFilterSettings settings) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    _data.writeInt(subId);
                    _data.writeTypedObject(settings, 0);
                    this.mRemote.transact(55, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public void disableVisualVoicemailSmsFilter(String callingPackage, int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    _data.writeInt(subId);
                    this.mRemote.transact(56, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public VisualVoicemailSmsFilterSettings getVisualVoicemailSmsFilterSettings(String callingPackage, int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    _data.writeInt(subId);
                    this.mRemote.transact(57, _data, _reply, 0);
                    _reply.readException();
                    VisualVoicemailSmsFilterSettings _result = (VisualVoicemailSmsFilterSettings) _reply.readTypedObject(VisualVoicemailSmsFilterSettings.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public VisualVoicemailSmsFilterSettings getActiveVisualVoicemailSmsFilterSettings(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(58, _data, _reply, 0);
                    _reply.readException();
                    VisualVoicemailSmsFilterSettings _result = (VisualVoicemailSmsFilterSettings) _reply.readTypedObject(VisualVoicemailSmsFilterSettings.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public void sendVisualVoicemailSmsForSubscriber(String callingPackage, String callingAttributeTag, int subId, String number, int port, String text, PendingIntent sentIntent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    _data.writeString(callingAttributeTag);
                    _data.writeInt(subId);
                    _data.writeString(number);
                    _data.writeInt(port);
                    _data.writeString(text);
                    _data.writeTypedObject(sentIntent, 0);
                    this.mRemote.transact(59, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public void sendDialerSpecialCode(String callingPackageName, String inputCode) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackageName);
                    _data.writeString(inputCode);
                    this.mRemote.transact(60, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public int getNetworkTypeForSubscriber(int subId, String callingPackage, String callingFeatureId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(callingPackage);
                    _data.writeString(callingFeatureId);
                    this.mRemote.transact(61, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public int getDataNetworkType(String callingPackage, String callingFeatureId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    _data.writeString(callingFeatureId);
                    this.mRemote.transact(62, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public int getDataNetworkTypeForSubscriber(int subId, String callingPackage, String callingFeatureId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(callingPackage);
                    _data.writeString(callingFeatureId);
                    this.mRemote.transact(63, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public int getVoiceNetworkTypeForSubscriber(int subId, String callingPackage, String callingFeatureId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(callingPackage);
                    _data.writeString(callingFeatureId);
                    this.mRemote.transact(64, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public boolean hasIccCard() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(65, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public boolean hasIccCardUsingSlotIndex(int slotIndex) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(slotIndex);
                    this.mRemote.transact(66, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public int getLteOnCdmaMode(String callingPackage, String callingFeatureId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    _data.writeString(callingFeatureId);
                    this.mRemote.transact(67, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public int getLteOnCdmaModeForSubscriber(int subId, String callingPackage, String callingFeatureId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(callingPackage);
                    _data.writeString(callingFeatureId);
                    this.mRemote.transact(68, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public List<CellInfo> getAllCellInfo(String callingPkg, String callingFeatureId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPkg);
                    _data.writeString(callingFeatureId);
                    this.mRemote.transact(69, _data, _reply, 0);
                    _reply.readException();
                    List<CellInfo> _result = _reply.createTypedArrayList(CellInfo.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public void requestCellInfoUpdate(int subId, ICellInfoCallback cb, String callingPkg, String callingFeatureId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeStrongInterface(cb);
                    _data.writeString(callingPkg);
                    _data.writeString(callingFeatureId);
                    this.mRemote.transact(70, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public void requestCellInfoUpdateWithWorkSource(int subId, ICellInfoCallback cb, String callingPkg, String callingFeatureId, WorkSource ws) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeStrongInterface(cb);
                    _data.writeString(callingPkg);
                    _data.writeString(callingFeatureId);
                    _data.writeTypedObject(ws, 0);
                    this.mRemote.transact(71, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public void setCellInfoListRate(int rateInMillis) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(rateInMillis);
                    this.mRemote.transact(72, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public IccOpenLogicalChannelResponse iccOpenLogicalChannel(IccLogicalChannelRequest request) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(request, 0);
                    this.mRemote.transact(73, _data, _reply, 0);
                    _reply.readException();
                    IccOpenLogicalChannelResponse _result = (IccOpenLogicalChannelResponse) _reply.readTypedObject(IccOpenLogicalChannelResponse.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public boolean iccCloseLogicalChannel(IccLogicalChannelRequest request) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(request, 0);
                    this.mRemote.transact(74, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public String iccTransmitApduLogicalChannelByPort(int slotIndex, int portIndex, int channel, int cla, int instruction, int p1, int p2, int p3, String data) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(slotIndex);
                    _data.writeInt(portIndex);
                    _data.writeInt(channel);
                    _data.writeInt(cla);
                    _data.writeInt(instruction);
                    _data.writeInt(p1);
                    _data.writeInt(p2);
                    _data.writeInt(p3);
                    _data.writeString(data);
                    this.mRemote.transact(75, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public String iccTransmitApduLogicalChannel(int subId, int channel, int cla, int instruction, int p1, int p2, int p3, String data) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeInt(channel);
                    _data.writeInt(cla);
                    _data.writeInt(instruction);
                    _data.writeInt(p1);
                    _data.writeInt(p2);
                    _data.writeInt(p3);
                    _data.writeString(data);
                    this.mRemote.transact(76, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public String iccTransmitApduBasicChannelByPort(int slotIndex, int portIndex, String callingPackage, int cla, int instruction, int p1, int p2, int p3, String data) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(slotIndex);
                    _data.writeInt(portIndex);
                    _data.writeString(callingPackage);
                    _data.writeInt(cla);
                    _data.writeInt(instruction);
                    _data.writeInt(p1);
                    _data.writeInt(p2);
                    _data.writeInt(p3);
                    _data.writeString(data);
                    this.mRemote.transact(77, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public String iccTransmitApduBasicChannel(int subId, String callingPackage, int cla, int instruction, int p1, int p2, int p3, String data) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(callingPackage);
                    _data.writeInt(cla);
                    _data.writeInt(instruction);
                    _data.writeInt(p1);
                    _data.writeInt(p2);
                    _data.writeInt(p3);
                    _data.writeString(data);
                    this.mRemote.transact(78, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public byte[] iccExchangeSimIO(int subId, int fileID, int command, int p1, int p2, int p3, String filePath) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeInt(fileID);
                    _data.writeInt(command);
                    _data.writeInt(p1);
                    _data.writeInt(p2);
                    _data.writeInt(p3);
                    _data.writeString(filePath);
                    this.mRemote.transact(79, _data, _reply, 0);
                    _reply.readException();
                    byte[] _result = _reply.createByteArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public String sendEnvelopeWithStatus(int subId, String content) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(content);
                    this.mRemote.transact(80, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public String nvReadItem(int itemID) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(itemID);
                    this.mRemote.transact(81, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public boolean nvWriteItem(int itemID, String itemValue) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(itemID);
                    _data.writeString(itemValue);
                    this.mRemote.transact(82, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public boolean nvWriteCdmaPrl(byte[] preferredRoamingList) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeByteArray(preferredRoamingList);
                    this.mRemote.transact(83, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public boolean resetModemConfig(int slotIndex) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(slotIndex);
                    this.mRemote.transact(84, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public boolean rebootModem(int slotIndex) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(slotIndex);
                    this.mRemote.transact(85, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public int getAllowedNetworkTypesBitmask(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(86, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public boolean isTetheringApnRequiredForSubscriber(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(87, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public void enableIms(int slotId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(slotId);
                    this.mRemote.transact(88, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public void disableIms(int slotId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(slotId);
                    this.mRemote.transact(89, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public void resetIms(int slotIndex) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(slotIndex);
                    this.mRemote.transact(90, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public void registerMmTelFeatureCallback(int slotId, IImsServiceFeatureCallback callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(slotId);
                    _data.writeStrongInterface(callback);
                    this.mRemote.transact(91, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public void unregisterImsFeatureCallback(IImsServiceFeatureCallback callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongInterface(callback);
                    this.mRemote.transact(92, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public IImsRegistration getImsRegistration(int slotId, int feature) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(slotId);
                    _data.writeInt(feature);
                    this.mRemote.transact(93, _data, _reply, 0);
                    _reply.readException();
                    IImsRegistration _result = IImsRegistration.Stub.asInterface(_reply.readStrongBinder());
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public IImsConfig getImsConfig(int slotId, int feature) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(slotId);
                    _data.writeInt(feature);
                    this.mRemote.transact(94, _data, _reply, 0);
                    _reply.readException();
                    IImsConfig _result = IImsConfig.Stub.asInterface(_reply.readStrongBinder());
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public boolean setBoundImsServiceOverride(int slotIndex, boolean isCarrierService, int[] featureTypes, String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(slotIndex);
                    _data.writeBoolean(isCarrierService);
                    _data.writeIntArray(featureTypes);
                    _data.writeString(packageName);
                    this.mRemote.transact(95, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public boolean clearCarrierImsServiceOverride(int slotIndex) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(slotIndex);
                    this.mRemote.transact(96, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public String getBoundImsServicePackage(int slotIndex, boolean isCarrierImsService, int featureType) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(slotIndex);
                    _data.writeBoolean(isCarrierImsService);
                    _data.writeInt(featureType);
                    this.mRemote.transact(97, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public void getImsMmTelFeatureState(int subId, IIntegerConsumer callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeStrongInterface(callback);
                    this.mRemote.transact(98, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public void setNetworkSelectionModeAutomatic(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(99, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public CellNetworkScanResult getCellNetworkScanResults(int subId, String callingPackage, String callingFeatureId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(callingPackage);
                    _data.writeString(callingFeatureId);
                    this.mRemote.transact(100, _data, _reply, 0);
                    _reply.readException();
                    CellNetworkScanResult _result = (CellNetworkScanResult) _reply.readTypedObject(CellNetworkScanResult.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public int requestNetworkScan(int subId, boolean renounceFineLocationAccess, NetworkScanRequest request, Messenger messenger, IBinder binder, String callingPackage, String callingFeatureId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeBoolean(renounceFineLocationAccess);
                    _data.writeTypedObject(request, 0);
                    _data.writeTypedObject(messenger, 0);
                    _data.writeStrongBinder(binder);
                    _data.writeString(callingPackage);
                    _data.writeString(callingFeatureId);
                    this.mRemote.transact(101, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public void stopNetworkScan(int subId, int scanId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeInt(scanId);
                    this.mRemote.transact(102, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public boolean setNetworkSelectionModeManual(int subId, OperatorInfo operatorInfo, boolean persisSelection) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeTypedObject(operatorInfo, 0);
                    _data.writeBoolean(persisSelection);
                    this.mRemote.transact(103, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public long getAllowedNetworkTypesForReason(int subId, int reason) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeInt(reason);
                    this.mRemote.transact(104, _data, _reply, 0);
                    _reply.readException();
                    long _result = _reply.readLong();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public boolean setAllowedNetworkTypesForReason(int subId, int reason, long allowedNetworkTypes) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeInt(reason);
                    _data.writeLong(allowedNetworkTypes);
                    this.mRemote.transact(105, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public boolean getDataEnabled(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(106, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public boolean isUserDataEnabled(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(107, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public boolean isDataEnabled(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(108, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public void setDataEnabledForReason(int subId, int reason, boolean enable, String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeInt(reason);
                    _data.writeBoolean(enable);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(109, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public boolean isDataEnabledForReason(int subId, int reason) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeInt(reason);
                    this.mRemote.transact(110, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public boolean isManualNetworkSelectionAllowed(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(111, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public void setImsRegistrationState(boolean registered) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeBoolean(registered);
                    this.mRemote.transact(112, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public String getCdmaMdn(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(113, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public String getCdmaMin(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(114, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public void requestNumberVerification(PhoneNumberRange range, long timeoutMillis, INumberVerificationCallback callback, String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(range, 0);
                    _data.writeLong(timeoutMillis);
                    _data.writeStrongInterface(callback);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(115, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public int getCarrierPrivilegeStatus(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(116, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public int getCarrierPrivilegeStatusForUid(int subId, int uid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeInt(uid);
                    this.mRemote.transact(117, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public int checkCarrierPrivilegesForPackage(int subId, String pkgName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(pkgName);
                    this.mRemote.transact(118, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public int checkCarrierPrivilegesForPackageAnyPhone(String pkgName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(pkgName);
                    this.mRemote.transact(119, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public List<String> getCarrierPackageNamesForIntentAndPhone(Intent intent, int phoneId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(intent, 0);
                    _data.writeInt(phoneId);
                    this.mRemote.transact(120, _data, _reply, 0);
                    _reply.readException();
                    List<String> _result = _reply.createStringArrayList();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public boolean setLine1NumberForDisplayForSubscriber(int subId, String alphaTag, String number) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(alphaTag);
                    _data.writeString(number);
                    this.mRemote.transact(121, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public String getLine1NumberForDisplay(int subId, String callingPackage, String callingFeatureId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(callingPackage);
                    _data.writeString(callingFeatureId);
                    this.mRemote.transact(122, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public String getLine1AlphaTagForDisplay(int subId, String callingPackage, String callingFeatureId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(callingPackage);
                    _data.writeString(callingFeatureId);
                    this.mRemote.transact(123, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public String[] getMergedSubscriberIds(int subId, String callingPackage, String callingFeatureId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(callingPackage);
                    _data.writeString(callingFeatureId);
                    this.mRemote.transact(124, _data, _reply, 0);
                    _reply.readException();
                    String[] _result = _reply.createStringArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public String[] getMergedImsisFromGroup(int subId, String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(125, _data, _reply, 0);
                    _reply.readException();
                    String[] _result = _reply.createStringArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public boolean setOperatorBrandOverride(int subId, String brand) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(brand);
                    this.mRemote.transact(126, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public boolean setRoamingOverride(int subId, List<String> gsmRoamingList, List<String> gsmNonRoamingList, List<String> cdmaRoamingList, List<String> cdmaNonRoamingList) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeStringList(gsmRoamingList);
                    _data.writeStringList(gsmNonRoamingList);
                    _data.writeStringList(cdmaRoamingList);
                    _data.writeStringList(cdmaNonRoamingList);
                    this.mRemote.transact(127, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public int invokeOemRilRequestRaw(byte[] oemReq, byte[] oemResp) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeByteArray(oemReq);
                    if (oemResp == null) {
                        _data.writeInt(-1);
                    } else {
                        _data.writeInt(oemResp.length);
                    }
                    this.mRemote.transact(128, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    _reply.readByteArray(oemResp);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public boolean needMobileRadioShutdown() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(129, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public void shutdownMobileRadios() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(130, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public int getRadioAccessFamily(int phoneId, String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(phoneId);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(131, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public void uploadCallComposerPicture(int subscriptionId, String callingPackage, String contentType, ParcelFileDescriptor fd, ResultReceiver callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subscriptionId);
                    _data.writeString(callingPackage);
                    _data.writeString(contentType);
                    _data.writeTypedObject(fd, 0);
                    _data.writeTypedObject(callback, 0);
                    this.mRemote.transact(132, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public void enableVideoCalling(boolean enable) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeBoolean(enable);
                    this.mRemote.transact(133, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public boolean isVideoCallingEnabled(String callingPackage, String callingFeatureId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    _data.writeString(callingFeatureId);
                    this.mRemote.transact(134, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public boolean canChangeDtmfToneLength(int subId, String callingPackage, String callingFeatureId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(callingPackage);
                    _data.writeString(callingFeatureId);
                    this.mRemote.transact(135, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public boolean isWorldPhone(int subId, String callingPackage, String callingFeatureId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(callingPackage);
                    _data.writeString(callingFeatureId);
                    this.mRemote.transact(136, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public boolean isTtyModeSupported() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(137, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public boolean isRttSupported(int subscriptionId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subscriptionId);
                    this.mRemote.transact(138, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public boolean isHearingAidCompatibilitySupported() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(139, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public boolean isImsRegistered(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(140, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public boolean isWifiCallingAvailable(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(141, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public boolean isVideoTelephonyAvailable(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(142, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public int getImsRegTechnologyForMmTel(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(143, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public String getDeviceId(String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(144, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public String getDeviceIdWithFeature(String callingPackage, String callingFeatureId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    _data.writeString(callingFeatureId);
                    this.mRemote.transact(145, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public String getImeiForSlot(int slotIndex, String callingPackage, String callingFeatureId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(slotIndex);
                    _data.writeString(callingPackage);
                    _data.writeString(callingFeatureId);
                    this.mRemote.transact(146, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public String getTypeAllocationCodeForSlot(int slotIndex) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(slotIndex);
                    this.mRemote.transact(147, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public String getMeidForSlot(int slotIndex, String callingPackage, String callingFeatureId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(slotIndex);
                    _data.writeString(callingPackage);
                    _data.writeString(callingFeatureId);
                    this.mRemote.transact(148, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public String getManufacturerCodeForSlot(int slotIndex) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(slotIndex);
                    this.mRemote.transact(149, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public String getDeviceSoftwareVersionForSlot(int slotIndex, String callingPackage, String callingFeatureId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(slotIndex);
                    _data.writeString(callingPackage);
                    _data.writeString(callingFeatureId);
                    this.mRemote.transact(150, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public int getSubIdForPhoneAccountHandle(PhoneAccountHandle phoneAccountHandle, String callingPackage, String callingFeatureId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(phoneAccountHandle, 0);
                    _data.writeString(callingPackage);
                    _data.writeString(callingFeatureId);
                    this.mRemote.transact(151, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public PhoneAccountHandle getPhoneAccountHandleForSubscriptionId(int subscriptionId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subscriptionId);
                    this.mRemote.transact(152, _data, _reply, 0);
                    _reply.readException();
                    PhoneAccountHandle _result = (PhoneAccountHandle) _reply.readTypedObject(PhoneAccountHandle.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public void factoryReset(int subId, String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(153, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public String getSimLocaleForSubscriber(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(154, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public void requestModemActivityInfo(ResultReceiver result) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(result, 0);
                    this.mRemote.transact(155, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public ServiceState getServiceStateForSubscriber(int subId, boolean renounceFineLocationAccess, boolean renounceCoarseLocationAccess, String callingPackage, String callingFeatureId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeBoolean(renounceFineLocationAccess);
                    _data.writeBoolean(renounceCoarseLocationAccess);
                    _data.writeString(callingPackage);
                    _data.writeString(callingFeatureId);
                    this.mRemote.transact(156, _data, _reply, 0);
                    _reply.readException();
                    ServiceState _result = (ServiceState) _reply.readTypedObject(ServiceState.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public Uri getVoicemailRingtoneUri(PhoneAccountHandle accountHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(accountHandle, 0);
                    this.mRemote.transact(157, _data, _reply, 0);
                    _reply.readException();
                    Uri _result = (Uri) _reply.readTypedObject(Uri.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public void setVoicemailRingtoneUri(String callingPackage, PhoneAccountHandle phoneAccountHandle, Uri uri) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    _data.writeTypedObject(phoneAccountHandle, 0);
                    _data.writeTypedObject(uri, 0);
                    this.mRemote.transact(158, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public boolean isVoicemailVibrationEnabled(PhoneAccountHandle accountHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(accountHandle, 0);
                    this.mRemote.transact(159, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public void setVoicemailVibrationEnabled(String callingPackage, PhoneAccountHandle phoneAccountHandle, boolean enabled) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    _data.writeTypedObject(phoneAccountHandle, 0);
                    _data.writeBoolean(enabled);
                    this.mRemote.transact(160, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public List<String> getPackagesWithCarrierPrivileges(int phoneId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(phoneId);
                    this.mRemote.transact(161, _data, _reply, 0);
                    _reply.readException();
                    List<String> _result = _reply.createStringArrayList();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public List<String> getPackagesWithCarrierPrivilegesForAllPhones() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(162, _data, _reply, 0);
                    _reply.readException();
                    List<String> _result = _reply.createStringArrayList();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public String getAidForAppType(int subId, int appType) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeInt(appType);
                    this.mRemote.transact(163, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public String getEsn(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(164, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public String getCdmaPrlVersion(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(165, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public List<TelephonyHistogram> getTelephonyHistograms() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(166, _data, _reply, 0);
                    _reply.readException();
                    List<TelephonyHistogram> _result = _reply.createTypedArrayList(TelephonyHistogram.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public int setAllowedCarriers(CarrierRestrictionRules carrierRestrictionRules) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(carrierRestrictionRules, 0);
                    this.mRemote.transact(167, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public CarrierRestrictionRules getAllowedCarriers() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(168, _data, _reply, 0);
                    _reply.readException();
                    CarrierRestrictionRules _result = (CarrierRestrictionRules) _reply.readTypedObject(CarrierRestrictionRules.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public int getSubscriptionCarrierId(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(169, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public String getSubscriptionCarrierName(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(170, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public int getSubscriptionSpecificCarrierId(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(171, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public String getSubscriptionSpecificCarrierName(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(172, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public int getCarrierIdFromMccMnc(int slotIndex, String mccmnc, boolean isSubscriptionMccMnc) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(slotIndex);
                    _data.writeString(mccmnc);
                    _data.writeBoolean(isSubscriptionMccMnc);
                    this.mRemote.transact(173, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public void carrierActionSetRadioEnabled(int subId, boolean enabled) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeBoolean(enabled);
                    this.mRemote.transact(174, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public void carrierActionReportDefaultNetworkStatus(int subId, boolean report) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeBoolean(report);
                    this.mRemote.transact(175, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public void carrierActionResetAll(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(176, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public void getCallForwarding(int subId, int callForwardingReason, ICallForwardingInfoCallback callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeInt(callForwardingReason);
                    _data.writeStrongInterface(callback);
                    this.mRemote.transact(177, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public void setCallForwarding(int subId, CallForwardingInfo callForwardingInfo, IIntegerConsumer callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeTypedObject(callForwardingInfo, 0);
                    _data.writeStrongInterface(callback);
                    this.mRemote.transact(178, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public void getCallWaitingStatus(int subId, IIntegerConsumer callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeStrongInterface(callback);
                    this.mRemote.transact(179, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public void setCallWaitingStatus(int subId, boolean enabled, IIntegerConsumer callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeBoolean(enabled);
                    _data.writeStrongInterface(callback);
                    this.mRemote.transact(180, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public List<ClientRequestStats> getClientRequestStats(String callingPackage, String callingFeatureId, int subid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    _data.writeString(callingFeatureId);
                    _data.writeInt(subid);
                    this.mRemote.transact(181, _data, _reply, 0);
                    _reply.readException();
                    List<ClientRequestStats> _result = _reply.createTypedArrayList(ClientRequestStats.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public void setSimPowerStateForSlot(int slotIndex, int state) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(slotIndex);
                    _data.writeInt(state);
                    this.mRemote.transact(182, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public void setSimPowerStateForSlotWithCallback(int slotIndex, int state, IIntegerConsumer callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(slotIndex);
                    _data.writeInt(state);
                    _data.writeStrongInterface(callback);
                    this.mRemote.transact(183, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public String[] getForbiddenPlmns(int subId, int appType, String callingPackage, String callingFeatureId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeInt(appType);
                    _data.writeString(callingPackage);
                    _data.writeString(callingFeatureId);
                    this.mRemote.transact(184, _data, _reply, 0);
                    _reply.readException();
                    String[] _result = _reply.createStringArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public int setForbiddenPlmns(int subId, int appType, List<String> fplmns, String callingPackage, String callingFeatureId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeInt(appType);
                    _data.writeStringList(fplmns);
                    _data.writeString(callingPackage);
                    _data.writeString(callingFeatureId);
                    this.mRemote.transact(185, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public boolean getEmergencyCallbackMode(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(186, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public SignalStrength getSignalStrength(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(187, _data, _reply, 0);
                    _reply.readException();
                    SignalStrength _result = (SignalStrength) _reply.readTypedObject(SignalStrength.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public int getCardIdForDefaultEuicc(int subId, String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(188, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public List<UiccCardInfo> getUiccCardsInfo(String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(189, _data, _reply, 0);
                    _reply.readException();
                    List<UiccCardInfo> _result = _reply.createTypedArrayList(UiccCardInfo.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public UiccSlotInfo[] getUiccSlotsInfo(String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(190, _data, _reply, 0);
                    _reply.readException();
                    UiccSlotInfo[] _result = (UiccSlotInfo[]) _reply.createTypedArray(UiccSlotInfo.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public boolean switchSlots(int[] physicalSlots) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeIntArray(physicalSlots);
                    this.mRemote.transact(191, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public boolean setSimSlotMapping(List<UiccSlotMapping> slotMapping) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedList(slotMapping);
                    this.mRemote.transact(192, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public boolean isDataRoamingEnabled(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(193, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public void setDataRoamingEnabled(int subId, boolean isEnabled) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeBoolean(isEnabled);
                    this.mRemote.transact(194, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public int getCdmaRoamingMode(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(195, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public boolean setCdmaRoamingMode(int subId, int mode) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeInt(mode);
                    this.mRemote.transact(196, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public int getCdmaSubscriptionMode(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(197, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public boolean setCdmaSubscriptionMode(int subId, int mode) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeInt(mode);
                    this.mRemote.transact(198, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public void setCarrierTestOverride(int subId, String mccmnc, String imsi, String iccid, String gid1, String gid2, String plmn, String spn, String carrierPrivilegeRules, String apn) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(mccmnc);
                    _data.writeString(imsi);
                    _data.writeString(iccid);
                    _data.writeString(gid1);
                    _data.writeString(gid2);
                    _data.writeString(plmn);
                    _data.writeString(spn);
                    _data.writeString(carrierPrivilegeRules);
                    _data.writeString(apn);
                    this.mRemote.transact(199, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public int getCarrierIdListVersion(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(200, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public void refreshUiccProfile(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(201, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public int getNumberOfModemsWithSimultaneousDataConnections(int subId, String callingPackage, String callingFeatureId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(callingPackage);
                    _data.writeString(callingFeatureId);
                    this.mRemote.transact(202, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public int getNetworkSelectionMode(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(203, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public boolean isInEmergencySmsMode() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(204, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public int getRadioPowerState(int slotIndex, String callingPackage, String callingFeatureId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(slotIndex);
                    _data.writeString(callingPackage);
                    _data.writeString(callingFeatureId);
                    this.mRemote.transact(205, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public void registerImsRegistrationCallback(int subId, IImsRegistrationCallback c) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeStrongInterface(c);
                    this.mRemote.transact(206, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public void unregisterImsRegistrationCallback(int subId, IImsRegistrationCallback c) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeStrongInterface(c);
                    this.mRemote.transact(207, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public void getImsMmTelRegistrationState(int subId, IIntegerConsumer consumer) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeStrongInterface(consumer);
                    this.mRemote.transact(208, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public void getImsMmTelRegistrationTransportType(int subId, IIntegerConsumer consumer) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeStrongInterface(consumer);
                    this.mRemote.transact(209, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public void registerMmTelCapabilityCallback(int subId, IImsCapabilityCallback c) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeStrongInterface(c);
                    this.mRemote.transact(210, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public void unregisterMmTelCapabilityCallback(int subId, IImsCapabilityCallback c) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeStrongInterface(c);
                    this.mRemote.transact(211, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public boolean isCapable(int subId, int capability, int regTech) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeInt(capability);
                    _data.writeInt(regTech);
                    this.mRemote.transact(212, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public boolean isAvailable(int subId, int capability, int regTech) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeInt(capability);
                    _data.writeInt(regTech);
                    this.mRemote.transact(213, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public void isMmTelCapabilitySupported(int subId, IIntegerConsumer callback, int capability, int transportType) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeStrongInterface(callback);
                    _data.writeInt(capability);
                    _data.writeInt(transportType);
                    this.mRemote.transact(214, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public boolean isAdvancedCallingSettingEnabled(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(215, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public void setAdvancedCallingSettingEnabled(int subId, boolean isEnabled) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeBoolean(isEnabled);
                    this.mRemote.transact(216, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public boolean isVtSettingEnabled(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(217, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public void setVtSettingEnabled(int subId, boolean isEnabled) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeBoolean(isEnabled);
                    this.mRemote.transact(218, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public boolean isVoWiFiSettingEnabled(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(219, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public void setVoWiFiSettingEnabled(int subId, boolean isEnabled) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeBoolean(isEnabled);
                    this.mRemote.transact(220, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public boolean isCrossSimCallingEnabledByUser(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(221, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public void setCrossSimCallingEnabled(int subId, boolean isEnabled) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeBoolean(isEnabled);
                    this.mRemote.transact(222, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public boolean isVoWiFiRoamingSettingEnabled(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(223, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public void setVoWiFiRoamingSettingEnabled(int subId, boolean isEnabled) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeBoolean(isEnabled);
                    this.mRemote.transact(224, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public void setVoWiFiNonPersistent(int subId, boolean isCapable, int mode) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeBoolean(isCapable);
                    _data.writeInt(mode);
                    this.mRemote.transact(225, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public int getVoWiFiModeSetting(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(226, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public void setVoWiFiModeSetting(int subId, int mode) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeInt(mode);
                    this.mRemote.transact(227, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public int getVoWiFiRoamingModeSetting(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(228, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public void setVoWiFiRoamingModeSetting(int subId, int mode) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeInt(mode);
                    this.mRemote.transact(229, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public void setRttCapabilitySetting(int subId, boolean isEnabled) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeBoolean(isEnabled);
                    this.mRemote.transact(230, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public boolean isTtyOverVolteEnabled(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(231, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public Map getEmergencyNumberList(String callingPackage, String callingFeatureId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    _data.writeString(callingFeatureId);
                    this.mRemote.transact(232, _data, _reply, 0);
                    _reply.readException();
                    ClassLoader cl = getClass().getClassLoader();
                    Map _result = _reply.readHashMap(cl);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public boolean isEmergencyNumber(String number, boolean exactMatch) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(number);
                    _data.writeBoolean(exactMatch);
                    this.mRemote.transact(233, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public List<String> getCertsFromCarrierPrivilegeAccessRules(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(234, _data, _reply, 0);
                    _reply.readException();
                    List<String> _result = _reply.createStringArrayList();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public void registerImsProvisioningChangedCallback(int subId, IImsConfigCallback callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeStrongInterface(callback);
                    this.mRemote.transact(235, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public void unregisterImsProvisioningChangedCallback(int subId, IImsConfigCallback callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeStrongInterface(callback);
                    this.mRemote.transact(236, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public void registerFeatureProvisioningChangedCallback(int subId, IFeatureProvisioningCallback callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeStrongInterface(callback);
                    this.mRemote.transact(237, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public void unregisterFeatureProvisioningChangedCallback(int subId, IFeatureProvisioningCallback callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeStrongInterface(callback);
                    this.mRemote.transact(238, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public void setImsProvisioningStatusForCapability(int subId, int capability, int tech, boolean isProvisioned) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeInt(capability);
                    _data.writeInt(tech);
                    _data.writeBoolean(isProvisioned);
                    this.mRemote.transact(239, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public boolean getImsProvisioningStatusForCapability(int subId, int capability, int tech) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeInt(capability);
                    _data.writeInt(tech);
                    this.mRemote.transact(240, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public boolean getRcsProvisioningStatusForCapability(int subId, int capability, int tech) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeInt(capability);
                    _data.writeInt(tech);
                    this.mRemote.transact(241, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public void setRcsProvisioningStatusForCapability(int subId, int capability, int tech, boolean isProvisioned) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeInt(capability);
                    _data.writeInt(tech);
                    _data.writeBoolean(isProvisioned);
                    this.mRemote.transact(242, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public int getImsProvisioningInt(int subId, int key) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeInt(key);
                    this.mRemote.transact(243, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public String getImsProvisioningString(int subId, int key) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeInt(key);
                    this.mRemote.transact(244, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public int setImsProvisioningInt(int subId, int key, int value) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeInt(key);
                    _data.writeInt(value);
                    this.mRemote.transact(245, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public int setImsProvisioningString(int subId, int key, String value) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeInt(key);
                    _data.writeString(value);
                    this.mRemote.transact(246, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public void startEmergencyCallbackMode() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(247, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public void updateEmergencyNumberListTestMode(int action, EmergencyNumber num) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(action);
                    _data.writeTypedObject(num, 0);
                    this.mRemote.transact(248, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public List<String> getEmergencyNumberListTestMode() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(249, _data, _reply, 0);
                    _reply.readException();
                    List<String> _result = _reply.createStringArrayList();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public int getEmergencyNumberDbVersion(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(250, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public void notifyOtaEmergencyNumberDbInstalled() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(251, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public void updateOtaEmergencyNumberDbFilePath(ParcelFileDescriptor otaParcelFileDescriptor) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(otaParcelFileDescriptor, 0);
                    this.mRemote.transact(252, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public void resetOtaEmergencyNumberDbFilePath() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(253, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public boolean enableModemForSlot(int slotIndex, boolean enable) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(slotIndex);
                    _data.writeBoolean(enable);
                    this.mRemote.transact(254, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public void setMultiSimCarrierRestriction(boolean isMultiSimCarrierRestricted) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeBoolean(isMultiSimCarrierRestricted);
                    this.mRemote.transact(255, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public int isMultiSimSupported(String callingPackage, String callingFeatureId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    _data.writeString(callingFeatureId);
                    this.mRemote.transact(256, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public void switchMultiSimConfig(int numOfSims) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(numOfSims);
                    this.mRemote.transact(257, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public boolean doesSwitchMultiSimConfigTriggerReboot(int subId, String callingPackage, String callingFeatureId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(callingPackage);
                    _data.writeString(callingFeatureId);
                    this.mRemote.transact(258, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public List<UiccSlotMapping> getSlotsMapping(String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(259, _data, _reply, 0);
                    _reply.readException();
                    List<UiccSlotMapping> _result = _reply.createTypedArrayList(UiccSlotMapping.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public int getRadioHalVersion() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(260, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public String getCurrentPackageName() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(261, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public boolean isApplicationOnUicc(int subId, int appType) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeInt(appType);
                    this.mRemote.transact(262, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public boolean isModemEnabledForSlot(int slotIndex, String callingPackage, String callingFeatureId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(slotIndex);
                    _data.writeString(callingPackage);
                    _data.writeString(callingFeatureId);
                    this.mRemote.transact(263, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public boolean isDataEnabledForApn(int apnType, int subId, String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(apnType);
                    _data.writeInt(subId);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(264, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public boolean isApnMetered(int apnType, int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(apnType);
                    _data.writeInt(subId);
                    this.mRemote.transact(265, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public void setSystemSelectionChannels(List<RadioAccessSpecifier> specifiers, int subId, IBooleanConsumer resultCallback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedList(specifiers);
                    _data.writeInt(subId);
                    _data.writeStrongInterface(resultCallback);
                    this.mRemote.transact(266, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public List<RadioAccessSpecifier> getSystemSelectionChannels(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(267, _data, _reply, 0);
                    _reply.readException();
                    List<RadioAccessSpecifier> _result = _reply.createTypedArrayList(RadioAccessSpecifier.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public boolean isMvnoMatched(int slotIndex, int mvnoType, String mvnoMatchData) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(slotIndex);
                    _data.writeInt(mvnoType);
                    _data.writeString(mvnoMatchData);
                    this.mRemote.transact(268, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public void enqueueSmsPickResult(String callingPackage, String callingAttributeTag, IIntegerConsumer subIdResult) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    _data.writeString(callingAttributeTag);
                    _data.writeStrongInterface(subIdResult);
                    this.mRemote.transact(269, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public String getMmsUserAgent(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(270, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public String getMmsUAProfUrl(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(271, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public void setMobileDataPolicyEnabled(int subscriptionId, int policy, boolean enabled) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subscriptionId);
                    _data.writeInt(policy);
                    _data.writeBoolean(enabled);
                    this.mRemote.transact(272, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public boolean isMobileDataPolicyEnabled(int subscriptionId, int policy) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subscriptionId);
                    _data.writeInt(policy);
                    this.mRemote.transact(273, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public void setCepEnabled(boolean isCepEnabled) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeBoolean(isCepEnabled);
                    this.mRemote.transact(274, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public void notifyRcsAutoConfigurationReceived(int subId, byte[] config, boolean isCompressed) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeByteArray(config);
                    _data.writeBoolean(isCompressed);
                    this.mRemote.transact(275, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public boolean isIccLockEnabled(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(276, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public int setIccLockEnabled(int subId, boolean enabled, String password) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeBoolean(enabled);
                    _data.writeString(password);
                    this.mRemote.transact(277, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public int changeIccLockPassword(int subId, String oldPassword, String newPassword) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(oldPassword);
                    _data.writeString(newPassword);
                    this.mRemote.transact(278, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public void requestUserActivityNotification() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(279, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public void userActivity() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(280, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public String getManualNetworkSelectionPlmn(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(281, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public boolean canConnectTo5GInDsdsMode() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(282, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public List<String> getEquivalentHomePlmns(int subId, String callingPackage, String callingFeatureId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(callingPackage);
                    _data.writeString(callingFeatureId);
                    this.mRemote.transact(283, _data, _reply, 0);
                    _reply.readException();
                    List<String> _result = _reply.createStringArrayList();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public int setVoNrEnabled(int subId, boolean enabled) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeBoolean(enabled);
                    this.mRemote.transact(284, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public boolean isVoNrEnabled(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(285, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public int setNrDualConnectivityState(int subId, int nrDualConnectivityState) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeInt(nrDualConnectivityState);
                    this.mRemote.transact(286, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public boolean isNrDualConnectivityEnabled(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(287, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public boolean isRadioInterfaceCapabilitySupported(String capability) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(capability);
                    this.mRemote.transact(288, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public int sendThermalMitigationRequest(int subId, ThermalMitigationRequest thermalMitigationRequest, String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeTypedObject(thermalMitigationRequest, 0);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(289, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public void bootstrapAuthenticationRequest(int subId, int appType, Uri nafUrl, UaSecurityProtocolIdentifier securityProtocol, boolean forceBootStrapping, IBootstrapAuthenticationCallback callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeInt(appType);
                    _data.writeTypedObject(nafUrl, 0);
                    _data.writeTypedObject(securityProtocol, 0);
                    _data.writeBoolean(forceBootStrapping);
                    _data.writeStrongInterface(callback);
                    this.mRemote.transact(290, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public boolean setBoundGbaServiceOverride(int subId, String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(packageName);
                    this.mRemote.transact(291, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public String getBoundGbaService(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(292, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public boolean setGbaReleaseTimeOverride(int subId, int interval) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeInt(interval);
                    this.mRemote.transact(293, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public int getGbaReleaseTime(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(294, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public void setRcsClientConfiguration(int subId, RcsClientConfiguration rcc) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeTypedObject(rcc, 0);
                    this.mRemote.transact(295, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public boolean isRcsVolteSingleRegistrationCapable(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(296, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public void registerRcsProvisioningCallback(int subId, IRcsConfigCallback callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeStrongInterface(callback);
                    this.mRemote.transact(297, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public void unregisterRcsProvisioningCallback(int subId, IRcsConfigCallback callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeStrongInterface(callback);
                    this.mRemote.transact(298, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public void triggerRcsReconfiguration(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(299, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public void setRcsSingleRegistrationTestModeEnabled(boolean enabled) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeBoolean(enabled);
                    this.mRemote.transact(300, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public boolean getRcsSingleRegistrationTestModeEnabled() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(301, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public void setDeviceSingleRegistrationEnabledOverride(String enabled) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(enabled);
                    this.mRemote.transact(302, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public boolean getDeviceSingleRegistrationEnabled() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(303, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public boolean setCarrierSingleRegistrationEnabledOverride(int subId, String enabled) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(enabled);
                    this.mRemote.transact(304, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public void sendDeviceToDeviceMessage(int message, int value) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(message);
                    _data.writeInt(value);
                    this.mRemote.transact(305, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public void setActiveDeviceToDeviceTransport(String transport) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(transport);
                    this.mRemote.transact(306, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public void setDeviceToDeviceForceEnabled(boolean isForceEnabled) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeBoolean(isForceEnabled);
                    this.mRemote.transact(307, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public boolean getCarrierSingleRegistrationEnabled(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(308, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public boolean setImsFeatureValidationOverride(int subId, String enabled) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(enabled);
                    this.mRemote.transact(309, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public boolean getImsFeatureValidationOverride(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(310, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public String getMobileProvisioningUrl() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(311, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public int removeContactFromEab(int subId, String contacts) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(contacts);
                    this.mRemote.transact(312, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public String getContactFromEab(String contact) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(contact);
                    this.mRemote.transact(313, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public String getCapabilityFromEab(String contact) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(contact);
                    this.mRemote.transact(314, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public boolean getDeviceUceEnabled() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(315, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public void setDeviceUceEnabled(boolean isEnabled) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeBoolean(isEnabled);
                    this.mRemote.transact(316, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public RcsContactUceCapability addUceRegistrationOverrideShell(int subId, List<String> featureTags) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeStringList(featureTags);
                    this.mRemote.transact(317, _data, _reply, 0);
                    _reply.readException();
                    RcsContactUceCapability _result = (RcsContactUceCapability) _reply.readTypedObject(RcsContactUceCapability.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public RcsContactUceCapability removeUceRegistrationOverrideShell(int subId, List<String> featureTags) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeStringList(featureTags);
                    this.mRemote.transact(318, _data, _reply, 0);
                    _reply.readException();
                    RcsContactUceCapability _result = (RcsContactUceCapability) _reply.readTypedObject(RcsContactUceCapability.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public RcsContactUceCapability clearUceRegistrationOverrideShell(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(319, _data, _reply, 0);
                    _reply.readException();
                    RcsContactUceCapability _result = (RcsContactUceCapability) _reply.readTypedObject(RcsContactUceCapability.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public RcsContactUceCapability getLatestRcsContactUceCapabilityShell(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(320, _data, _reply, 0);
                    _reply.readException();
                    RcsContactUceCapability _result = (RcsContactUceCapability) _reply.readTypedObject(RcsContactUceCapability.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public String getLastUcePidfXmlShell(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(321, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public boolean removeUceRequestDisallowedStatus(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(322, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public boolean setCapabilitiesRequestTimeout(int subId, long timeoutAfterMs) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeLong(timeoutAfterMs);
                    this.mRemote.transact(323, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public void setSignalStrengthUpdateRequest(int subId, SignalStrengthUpdateRequest request, String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeTypedObject(request, 0);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(324, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public void clearSignalStrengthUpdateRequest(int subId, SignalStrengthUpdateRequest request, String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeTypedObject(request, 0);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(325, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public PhoneCapability getPhoneCapability() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(326, _data, _reply, 0);
                    _reply.readException();
                    PhoneCapability _result = (PhoneCapability) _reply.readTypedObject(PhoneCapability.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public int prepareForUnattendedReboot() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(327, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public void getSlicingConfig(ResultReceiver callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(callback, 0);
                    this.mRemote.transact(328, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public void registerImsStateCallback(int subId, int feature, IImsStateCallback cb, String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeInt(feature);
                    _data.writeStrongInterface(cb);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(329, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public void unregisterImsStateCallback(IImsStateCallback cb) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongInterface(cb);
                    this.mRemote.transact(330, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public CellIdentity getLastKnownCellIdentity(int subId, String callingPackage, String callingFeatureId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(callingPackage);
                    _data.writeString(callingFeatureId);
                    this.mRemote.transact(331, _data, _reply, 0);
                    _reply.readException();
                    CellIdentity _result = (CellIdentity) _reply.readTypedObject(CellIdentity.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public boolean isUsingNewDataStack() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(332, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public boolean setModemService(String serviceName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(serviceName);
                    this.mRemote.transact(333, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public String getModemService() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(334, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public boolean isProvisioningRequiredForCapability(int subId, int capability, int tech) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeInt(capability);
                    _data.writeInt(tech);
                    this.mRemote.transact(335, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public boolean isRcsProvisioningRequiredForCapability(int subId, int capability, int tech) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeInt(capability);
                    _data.writeInt(tech);
                    this.mRemote.transact(336, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public void setVoiceServiceStateOverride(int subId, boolean hasService, String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeBoolean(hasService);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(337, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public String getCarrierServicePackageNameForLogicalSlot(int logicalSlotIndex) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(logicalSlotIndex);
                    this.mRemote.transact(338, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public void setRemovableEsimAsDefaultEuicc(boolean isDefault, String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeBoolean(isDefault);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(339, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ITelephony
            public boolean isRemovableEsimDefaultEuicc(String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(340, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }

        private boolean onTransact$sendVisualVoicemailSmsForSubscriber$(Parcel data, Parcel reply) throws RemoteException {
            String _arg0 = data.readString();
            String _arg1 = data.readString();
            int _arg2 = data.readInt();
            String _arg3 = data.readString();
            int _arg4 = data.readInt();
            String _arg5 = data.readString();
            PendingIntent _arg6 = (PendingIntent) data.readTypedObject(PendingIntent.CREATOR);
            data.enforceNoDataAvail();
            sendVisualVoicemailSmsForSubscriber(_arg0, _arg1, _arg2, _arg3, _arg4, _arg5, _arg6);
            reply.writeNoException();
            return true;
        }

        private boolean onTransact$requestCellInfoUpdate$(Parcel data, Parcel reply) throws RemoteException {
            int _arg0 = data.readInt();
            ICellInfoCallback _arg1 = ICellInfoCallback.Stub.asInterface(data.readStrongBinder());
            String _arg2 = data.readString();
            String _arg3 = data.readString();
            data.enforceNoDataAvail();
            requestCellInfoUpdate(_arg0, _arg1, _arg2, _arg3);
            reply.writeNoException();
            return true;
        }

        private boolean onTransact$requestCellInfoUpdateWithWorkSource$(Parcel data, Parcel reply) throws RemoteException {
            int _arg0 = data.readInt();
            ICellInfoCallback _arg1 = ICellInfoCallback.Stub.asInterface(data.readStrongBinder());
            String _arg2 = data.readString();
            String _arg3 = data.readString();
            WorkSource _arg4 = (WorkSource) data.readTypedObject(WorkSource.CREATOR);
            data.enforceNoDataAvail();
            requestCellInfoUpdateWithWorkSource(_arg0, _arg1, _arg2, _arg3, _arg4);
            reply.writeNoException();
            return true;
        }

        private boolean onTransact$iccTransmitApduLogicalChannelByPort$(Parcel data, Parcel reply) throws RemoteException {
            int _arg0 = data.readInt();
            int _arg1 = data.readInt();
            int _arg2 = data.readInt();
            int _arg3 = data.readInt();
            int _arg4 = data.readInt();
            int _arg5 = data.readInt();
            int _arg6 = data.readInt();
            int _arg7 = data.readInt();
            String _arg8 = data.readString();
            data.enforceNoDataAvail();
            String _result = iccTransmitApduLogicalChannelByPort(_arg0, _arg1, _arg2, _arg3, _arg4, _arg5, _arg6, _arg7, _arg8);
            reply.writeNoException();
            reply.writeString(_result);
            return true;
        }

        private boolean onTransact$iccTransmitApduLogicalChannel$(Parcel data, Parcel reply) throws RemoteException {
            int _arg0 = data.readInt();
            int _arg1 = data.readInt();
            int _arg2 = data.readInt();
            int _arg3 = data.readInt();
            int _arg4 = data.readInt();
            int _arg5 = data.readInt();
            int _arg6 = data.readInt();
            String _arg7 = data.readString();
            data.enforceNoDataAvail();
            String _result = iccTransmitApduLogicalChannel(_arg0, _arg1, _arg2, _arg3, _arg4, _arg5, _arg6, _arg7);
            reply.writeNoException();
            reply.writeString(_result);
            return true;
        }

        private boolean onTransact$iccTransmitApduBasicChannelByPort$(Parcel data, Parcel reply) throws RemoteException {
            int _arg0 = data.readInt();
            int _arg1 = data.readInt();
            String _arg2 = data.readString();
            int _arg3 = data.readInt();
            int _arg4 = data.readInt();
            int _arg5 = data.readInt();
            int _arg6 = data.readInt();
            int _arg7 = data.readInt();
            String _arg8 = data.readString();
            data.enforceNoDataAvail();
            String _result = iccTransmitApduBasicChannelByPort(_arg0, _arg1, _arg2, _arg3, _arg4, _arg5, _arg6, _arg7, _arg8);
            reply.writeNoException();
            reply.writeString(_result);
            return true;
        }

        private boolean onTransact$iccTransmitApduBasicChannel$(Parcel data, Parcel reply) throws RemoteException {
            int _arg0 = data.readInt();
            String _arg1 = data.readString();
            int _arg2 = data.readInt();
            int _arg3 = data.readInt();
            int _arg4 = data.readInt();
            int _arg5 = data.readInt();
            int _arg6 = data.readInt();
            String _arg7 = data.readString();
            data.enforceNoDataAvail();
            String _result = iccTransmitApduBasicChannel(_arg0, _arg1, _arg2, _arg3, _arg4, _arg5, _arg6, _arg7);
            reply.writeNoException();
            reply.writeString(_result);
            return true;
        }

        private boolean onTransact$iccExchangeSimIO$(Parcel data, Parcel reply) throws RemoteException {
            int _arg0 = data.readInt();
            int _arg1 = data.readInt();
            int _arg2 = data.readInt();
            int _arg3 = data.readInt();
            int _arg4 = data.readInt();
            int _arg5 = data.readInt();
            String _arg6 = data.readString();
            data.enforceNoDataAvail();
            byte[] _result = iccExchangeSimIO(_arg0, _arg1, _arg2, _arg3, _arg4, _arg5, _arg6);
            reply.writeNoException();
            reply.writeByteArray(_result);
            return true;
        }

        private boolean onTransact$setBoundImsServiceOverride$(Parcel data, Parcel reply) throws RemoteException {
            int _arg0 = data.readInt();
            boolean _arg1 = data.readBoolean();
            int[] _arg2 = data.createIntArray();
            String _arg3 = data.readString();
            data.enforceNoDataAvail();
            boolean _result = setBoundImsServiceOverride(_arg0, _arg1, _arg2, _arg3);
            reply.writeNoException();
            reply.writeBoolean(_result);
            return true;
        }

        private boolean onTransact$requestNetworkScan$(Parcel data, Parcel reply) throws RemoteException {
            int _arg0 = data.readInt();
            boolean _arg1 = data.readBoolean();
            NetworkScanRequest _arg2 = (NetworkScanRequest) data.readTypedObject(NetworkScanRequest.CREATOR);
            Messenger _arg3 = (Messenger) data.readTypedObject(Messenger.CREATOR);
            IBinder _arg4 = data.readStrongBinder();
            String _arg5 = data.readString();
            String _arg6 = data.readString();
            data.enforceNoDataAvail();
            int _result = requestNetworkScan(_arg0, _arg1, _arg2, _arg3, _arg4, _arg5, _arg6);
            reply.writeNoException();
            reply.writeInt(_result);
            return true;
        }

        private boolean onTransact$setDataEnabledForReason$(Parcel data, Parcel reply) throws RemoteException {
            int _arg0 = data.readInt();
            int _arg1 = data.readInt();
            boolean _arg2 = data.readBoolean();
            String _arg3 = data.readString();
            data.enforceNoDataAvail();
            setDataEnabledForReason(_arg0, _arg1, _arg2, _arg3);
            reply.writeNoException();
            return true;
        }

        private boolean onTransact$requestNumberVerification$(Parcel data, Parcel reply) throws RemoteException {
            PhoneNumberRange _arg0 = (PhoneNumberRange) data.readTypedObject(PhoneNumberRange.CREATOR);
            long _arg1 = data.readLong();
            INumberVerificationCallback _arg2 = INumberVerificationCallback.Stub.asInterface(data.readStrongBinder());
            String _arg3 = data.readString();
            data.enforceNoDataAvail();
            requestNumberVerification(_arg0, _arg1, _arg2, _arg3);
            reply.writeNoException();
            return true;
        }

        private boolean onTransact$getMergedSubscriberIds$(Parcel data, Parcel reply) throws RemoteException {
            int _arg0 = data.readInt();
            String _arg1 = data.readString();
            String _arg2 = data.readString();
            data.enforceNoDataAvail();
            String[] _result = getMergedSubscriberIds(_arg0, _arg1, _arg2);
            reply.writeNoException();
            reply.writeStringArray(_result);
            return true;
        }

        private boolean onTransact$setRoamingOverride$(Parcel data, Parcel reply) throws RemoteException {
            int _arg0 = data.readInt();
            List<String> _arg1 = data.createStringArrayList();
            List<String> _arg2 = data.createStringArrayList();
            List<String> _arg3 = data.createStringArrayList();
            List<String> _arg4 = data.createStringArrayList();
            data.enforceNoDataAvail();
            boolean _result = setRoamingOverride(_arg0, _arg1, _arg2, _arg3, _arg4);
            reply.writeNoException();
            reply.writeBoolean(_result);
            return true;
        }

        private boolean onTransact$uploadCallComposerPicture$(Parcel data, Parcel reply) throws RemoteException {
            int _arg0 = data.readInt();
            String _arg1 = data.readString();
            String _arg2 = data.readString();
            ParcelFileDescriptor _arg3 = (ParcelFileDescriptor) data.readTypedObject(ParcelFileDescriptor.CREATOR);
            ResultReceiver _arg4 = (ResultReceiver) data.readTypedObject(ResultReceiver.CREATOR);
            data.enforceNoDataAvail();
            uploadCallComposerPicture(_arg0, _arg1, _arg2, _arg3, _arg4);
            reply.writeNoException();
            return true;
        }

        private boolean onTransact$canChangeDtmfToneLength$(Parcel data, Parcel reply) throws RemoteException {
            int _arg0 = data.readInt();
            String _arg1 = data.readString();
            String _arg2 = data.readString();
            data.enforceNoDataAvail();
            boolean _result = canChangeDtmfToneLength(_arg0, _arg1, _arg2);
            reply.writeNoException();
            reply.writeBoolean(_result);
            return true;
        }

        private boolean onTransact$isWorldPhone$(Parcel data, Parcel reply) throws RemoteException {
            int _arg0 = data.readInt();
            String _arg1 = data.readString();
            String _arg2 = data.readString();
            data.enforceNoDataAvail();
            boolean _result = isWorldPhone(_arg0, _arg1, _arg2);
            reply.writeNoException();
            reply.writeBoolean(_result);
            return true;
        }

        private boolean onTransact$getImeiForSlot$(Parcel data, Parcel reply) throws RemoteException {
            int _arg0 = data.readInt();
            String _arg1 = data.readString();
            String _arg2 = data.readString();
            data.enforceNoDataAvail();
            String _result = getImeiForSlot(_arg0, _arg1, _arg2);
            reply.writeNoException();
            reply.writeString(_result);
            return true;
        }

        private boolean onTransact$getMeidForSlot$(Parcel data, Parcel reply) throws RemoteException {
            int _arg0 = data.readInt();
            String _arg1 = data.readString();
            String _arg2 = data.readString();
            data.enforceNoDataAvail();
            String _result = getMeidForSlot(_arg0, _arg1, _arg2);
            reply.writeNoException();
            reply.writeString(_result);
            return true;
        }

        private boolean onTransact$getDeviceSoftwareVersionForSlot$(Parcel data, Parcel reply) throws RemoteException {
            int _arg0 = data.readInt();
            String _arg1 = data.readString();
            String _arg2 = data.readString();
            data.enforceNoDataAvail();
            String _result = getDeviceSoftwareVersionForSlot(_arg0, _arg1, _arg2);
            reply.writeNoException();
            reply.writeString(_result);
            return true;
        }

        private boolean onTransact$getSubIdForPhoneAccountHandle$(Parcel data, Parcel reply) throws RemoteException {
            PhoneAccountHandle _arg0 = (PhoneAccountHandle) data.readTypedObject(PhoneAccountHandle.CREATOR);
            String _arg1 = data.readString();
            String _arg2 = data.readString();
            data.enforceNoDataAvail();
            int _result = getSubIdForPhoneAccountHandle(_arg0, _arg1, _arg2);
            reply.writeNoException();
            reply.writeInt(_result);
            return true;
        }

        private boolean onTransact$getServiceStateForSubscriber$(Parcel data, Parcel reply) throws RemoteException {
            int _arg0 = data.readInt();
            boolean _arg1 = data.readBoolean();
            boolean _arg2 = data.readBoolean();
            String _arg3 = data.readString();
            String _arg4 = data.readString();
            data.enforceNoDataAvail();
            ServiceState _result = getServiceStateForSubscriber(_arg0, _arg1, _arg2, _arg3, _arg4);
            reply.writeNoException();
            reply.writeTypedObject(_result, 1);
            return true;
        }

        private boolean onTransact$setVoicemailRingtoneUri$(Parcel data, Parcel reply) throws RemoteException {
            String _arg0 = data.readString();
            PhoneAccountHandle _arg1 = (PhoneAccountHandle) data.readTypedObject(PhoneAccountHandle.CREATOR);
            Uri _arg2 = (Uri) data.readTypedObject(Uri.CREATOR);
            data.enforceNoDataAvail();
            setVoicemailRingtoneUri(_arg0, _arg1, _arg2);
            reply.writeNoException();
            return true;
        }

        private boolean onTransact$setVoicemailVibrationEnabled$(Parcel data, Parcel reply) throws RemoteException {
            String _arg0 = data.readString();
            PhoneAccountHandle _arg1 = (PhoneAccountHandle) data.readTypedObject(PhoneAccountHandle.CREATOR);
            boolean _arg2 = data.readBoolean();
            data.enforceNoDataAvail();
            setVoicemailVibrationEnabled(_arg0, _arg1, _arg2);
            reply.writeNoException();
            return true;
        }

        private boolean onTransact$getCarrierIdFromMccMnc$(Parcel data, Parcel reply) throws RemoteException {
            int _arg0 = data.readInt();
            String _arg1 = data.readString();
            boolean _arg2 = data.readBoolean();
            data.enforceNoDataAvail();
            int _result = getCarrierIdFromMccMnc(_arg0, _arg1, _arg2);
            reply.writeNoException();
            reply.writeInt(_result);
            return true;
        }

        private boolean onTransact$getCallForwarding$(Parcel data, Parcel reply) throws RemoteException {
            int _arg0 = data.readInt();
            int _arg1 = data.readInt();
            ICallForwardingInfoCallback _arg2 = ICallForwardingInfoCallback.Stub.asInterface(data.readStrongBinder());
            data.enforceNoDataAvail();
            getCallForwarding(_arg0, _arg1, _arg2);
            reply.writeNoException();
            return true;
        }

        private boolean onTransact$setCallForwarding$(Parcel data, Parcel reply) throws RemoteException {
            int _arg0 = data.readInt();
            CallForwardingInfo _arg1 = (CallForwardingInfo) data.readTypedObject(CallForwardingInfo.CREATOR);
            IIntegerConsumer _arg2 = IIntegerConsumer.Stub.asInterface(data.readStrongBinder());
            data.enforceNoDataAvail();
            setCallForwarding(_arg0, _arg1, _arg2);
            reply.writeNoException();
            return true;
        }

        private boolean onTransact$setCallWaitingStatus$(Parcel data, Parcel reply) throws RemoteException {
            int _arg0 = data.readInt();
            boolean _arg1 = data.readBoolean();
            IIntegerConsumer _arg2 = IIntegerConsumer.Stub.asInterface(data.readStrongBinder());
            data.enforceNoDataAvail();
            setCallWaitingStatus(_arg0, _arg1, _arg2);
            reply.writeNoException();
            return true;
        }

        private boolean onTransact$getClientRequestStats$(Parcel data, Parcel reply) throws RemoteException {
            String _arg0 = data.readString();
            String _arg1 = data.readString();
            int _arg2 = data.readInt();
            data.enforceNoDataAvail();
            List<ClientRequestStats> _result = getClientRequestStats(_arg0, _arg1, _arg2);
            reply.writeNoException();
            reply.writeTypedList(_result);
            return true;
        }

        private boolean onTransact$setSimPowerStateForSlotWithCallback$(Parcel data, Parcel reply) throws RemoteException {
            int _arg0 = data.readInt();
            int _arg1 = data.readInt();
            IIntegerConsumer _arg2 = IIntegerConsumer.Stub.asInterface(data.readStrongBinder());
            data.enforceNoDataAvail();
            setSimPowerStateForSlotWithCallback(_arg0, _arg1, _arg2);
            reply.writeNoException();
            return true;
        }

        private boolean onTransact$getForbiddenPlmns$(Parcel data, Parcel reply) throws RemoteException {
            int _arg0 = data.readInt();
            int _arg1 = data.readInt();
            String _arg2 = data.readString();
            String _arg3 = data.readString();
            data.enforceNoDataAvail();
            String[] _result = getForbiddenPlmns(_arg0, _arg1, _arg2, _arg3);
            reply.writeNoException();
            reply.writeStringArray(_result);
            return true;
        }

        private boolean onTransact$setForbiddenPlmns$(Parcel data, Parcel reply) throws RemoteException {
            int _arg0 = data.readInt();
            int _arg1 = data.readInt();
            List<String> _arg2 = data.createStringArrayList();
            String _arg3 = data.readString();
            String _arg4 = data.readString();
            data.enforceNoDataAvail();
            int _result = setForbiddenPlmns(_arg0, _arg1, _arg2, _arg3, _arg4);
            reply.writeNoException();
            reply.writeInt(_result);
            return true;
        }

        private boolean onTransact$setCarrierTestOverride$(Parcel data, Parcel reply) throws RemoteException {
            int _arg0 = data.readInt();
            String _arg1 = data.readString();
            String _arg2 = data.readString();
            String _arg3 = data.readString();
            String _arg4 = data.readString();
            String _arg5 = data.readString();
            String _arg6 = data.readString();
            String _arg7 = data.readString();
            String _arg8 = data.readString();
            String _arg9 = data.readString();
            data.enforceNoDataAvail();
            setCarrierTestOverride(_arg0, _arg1, _arg2, _arg3, _arg4, _arg5, _arg6, _arg7, _arg8, _arg9);
            reply.writeNoException();
            return true;
        }

        private boolean onTransact$getNumberOfModemsWithSimultaneousDataConnections$(Parcel data, Parcel reply) throws RemoteException {
            int _arg0 = data.readInt();
            String _arg1 = data.readString();
            String _arg2 = data.readString();
            data.enforceNoDataAvail();
            int _result = getNumberOfModemsWithSimultaneousDataConnections(_arg0, _arg1, _arg2);
            reply.writeNoException();
            reply.writeInt(_result);
            return true;
        }

        private boolean onTransact$getRadioPowerState$(Parcel data, Parcel reply) throws RemoteException {
            int _arg0 = data.readInt();
            String _arg1 = data.readString();
            String _arg2 = data.readString();
            data.enforceNoDataAvail();
            int _result = getRadioPowerState(_arg0, _arg1, _arg2);
            reply.writeNoException();
            reply.writeInt(_result);
            return true;
        }

        private boolean onTransact$isCapable$(Parcel data, Parcel reply) throws RemoteException {
            int _arg0 = data.readInt();
            int _arg1 = data.readInt();
            int _arg2 = data.readInt();
            data.enforceNoDataAvail();
            boolean _result = isCapable(_arg0, _arg1, _arg2);
            reply.writeNoException();
            reply.writeBoolean(_result);
            return true;
        }

        private boolean onTransact$isAvailable$(Parcel data, Parcel reply) throws RemoteException {
            int _arg0 = data.readInt();
            int _arg1 = data.readInt();
            int _arg2 = data.readInt();
            data.enforceNoDataAvail();
            boolean _result = isAvailable(_arg0, _arg1, _arg2);
            reply.writeNoException();
            reply.writeBoolean(_result);
            return true;
        }

        private boolean onTransact$isMmTelCapabilitySupported$(Parcel data, Parcel reply) throws RemoteException {
            int _arg0 = data.readInt();
            IIntegerConsumer _arg1 = IIntegerConsumer.Stub.asInterface(data.readStrongBinder());
            int _arg2 = data.readInt();
            int _arg3 = data.readInt();
            data.enforceNoDataAvail();
            isMmTelCapabilitySupported(_arg0, _arg1, _arg2, _arg3);
            reply.writeNoException();
            return true;
        }

        private boolean onTransact$setVoWiFiNonPersistent$(Parcel data, Parcel reply) throws RemoteException {
            int _arg0 = data.readInt();
            boolean _arg1 = data.readBoolean();
            int _arg2 = data.readInt();
            data.enforceNoDataAvail();
            setVoWiFiNonPersistent(_arg0, _arg1, _arg2);
            reply.writeNoException();
            return true;
        }

        private boolean onTransact$setImsProvisioningStatusForCapability$(Parcel data, Parcel reply) throws RemoteException {
            int _arg0 = data.readInt();
            int _arg1 = data.readInt();
            int _arg2 = data.readInt();
            boolean _arg3 = data.readBoolean();
            data.enforceNoDataAvail();
            setImsProvisioningStatusForCapability(_arg0, _arg1, _arg2, _arg3);
            reply.writeNoException();
            return true;
        }

        private boolean onTransact$getImsProvisioningStatusForCapability$(Parcel data, Parcel reply) throws RemoteException {
            int _arg0 = data.readInt();
            int _arg1 = data.readInt();
            int _arg2 = data.readInt();
            data.enforceNoDataAvail();
            boolean _result = getImsProvisioningStatusForCapability(_arg0, _arg1, _arg2);
            reply.writeNoException();
            reply.writeBoolean(_result);
            return true;
        }

        private boolean onTransact$getRcsProvisioningStatusForCapability$(Parcel data, Parcel reply) throws RemoteException {
            int _arg0 = data.readInt();
            int _arg1 = data.readInt();
            int _arg2 = data.readInt();
            data.enforceNoDataAvail();
            boolean _result = getRcsProvisioningStatusForCapability(_arg0, _arg1, _arg2);
            reply.writeNoException();
            reply.writeBoolean(_result);
            return true;
        }

        private boolean onTransact$setRcsProvisioningStatusForCapability$(Parcel data, Parcel reply) throws RemoteException {
            int _arg0 = data.readInt();
            int _arg1 = data.readInt();
            int _arg2 = data.readInt();
            boolean _arg3 = data.readBoolean();
            data.enforceNoDataAvail();
            setRcsProvisioningStatusForCapability(_arg0, _arg1, _arg2, _arg3);
            reply.writeNoException();
            return true;
        }

        private boolean onTransact$setImsProvisioningInt$(Parcel data, Parcel reply) throws RemoteException {
            int _arg0 = data.readInt();
            int _arg1 = data.readInt();
            int _arg2 = data.readInt();
            data.enforceNoDataAvail();
            int _result = setImsProvisioningInt(_arg0, _arg1, _arg2);
            reply.writeNoException();
            reply.writeInt(_result);
            return true;
        }

        private boolean onTransact$setImsProvisioningString$(Parcel data, Parcel reply) throws RemoteException {
            int _arg0 = data.readInt();
            int _arg1 = data.readInt();
            String _arg2 = data.readString();
            data.enforceNoDataAvail();
            int _result = setImsProvisioningString(_arg0, _arg1, _arg2);
            reply.writeNoException();
            reply.writeInt(_result);
            return true;
        }

        private boolean onTransact$doesSwitchMultiSimConfigTriggerReboot$(Parcel data, Parcel reply) throws RemoteException {
            int _arg0 = data.readInt();
            String _arg1 = data.readString();
            String _arg2 = data.readString();
            data.enforceNoDataAvail();
            boolean _result = doesSwitchMultiSimConfigTriggerReboot(_arg0, _arg1, _arg2);
            reply.writeNoException();
            reply.writeBoolean(_result);
            return true;
        }

        private boolean onTransact$isModemEnabledForSlot$(Parcel data, Parcel reply) throws RemoteException {
            int _arg0 = data.readInt();
            String _arg1 = data.readString();
            String _arg2 = data.readString();
            data.enforceNoDataAvail();
            boolean _result = isModemEnabledForSlot(_arg0, _arg1, _arg2);
            reply.writeNoException();
            reply.writeBoolean(_result);
            return true;
        }

        private boolean onTransact$isDataEnabledForApn$(Parcel data, Parcel reply) throws RemoteException {
            int _arg0 = data.readInt();
            int _arg1 = data.readInt();
            String _arg2 = data.readString();
            data.enforceNoDataAvail();
            boolean _result = isDataEnabledForApn(_arg0, _arg1, _arg2);
            reply.writeNoException();
            reply.writeBoolean(_result);
            return true;
        }

        private boolean onTransact$setSystemSelectionChannels$(Parcel data, Parcel reply) throws RemoteException {
            List<RadioAccessSpecifier> _arg0 = data.createTypedArrayList(RadioAccessSpecifier.CREATOR);
            int _arg1 = data.readInt();
            IBooleanConsumer _arg2 = IBooleanConsumer.Stub.asInterface(data.readStrongBinder());
            data.enforceNoDataAvail();
            setSystemSelectionChannels(_arg0, _arg1, _arg2);
            return true;
        }

        private boolean onTransact$isMvnoMatched$(Parcel data, Parcel reply) throws RemoteException {
            int _arg0 = data.readInt();
            int _arg1 = data.readInt();
            String _arg2 = data.readString();
            data.enforceNoDataAvail();
            boolean _result = isMvnoMatched(_arg0, _arg1, _arg2);
            reply.writeNoException();
            reply.writeBoolean(_result);
            return true;
        }

        private boolean onTransact$enqueueSmsPickResult$(Parcel data, Parcel reply) throws RemoteException {
            String _arg0 = data.readString();
            String _arg1 = data.readString();
            IIntegerConsumer _arg2 = IIntegerConsumer.Stub.asInterface(data.readStrongBinder());
            data.enforceNoDataAvail();
            enqueueSmsPickResult(_arg0, _arg1, _arg2);
            return true;
        }

        private boolean onTransact$setMobileDataPolicyEnabled$(Parcel data, Parcel reply) throws RemoteException {
            int _arg0 = data.readInt();
            int _arg1 = data.readInt();
            boolean _arg2 = data.readBoolean();
            data.enforceNoDataAvail();
            setMobileDataPolicyEnabled(_arg0, _arg1, _arg2);
            reply.writeNoException();
            return true;
        }

        private boolean onTransact$notifyRcsAutoConfigurationReceived$(Parcel data, Parcel reply) throws RemoteException {
            int _arg0 = data.readInt();
            byte[] _arg1 = data.createByteArray();
            boolean _arg2 = data.readBoolean();
            data.enforceNoDataAvail();
            notifyRcsAutoConfigurationReceived(_arg0, _arg1, _arg2);
            reply.writeNoException();
            return true;
        }

        private boolean onTransact$setIccLockEnabled$(Parcel data, Parcel reply) throws RemoteException {
            int _arg0 = data.readInt();
            boolean _arg1 = data.readBoolean();
            String _arg2 = data.readString();
            data.enforceNoDataAvail();
            int _result = setIccLockEnabled(_arg0, _arg1, _arg2);
            reply.writeNoException();
            reply.writeInt(_result);
            return true;
        }

        private boolean onTransact$changeIccLockPassword$(Parcel data, Parcel reply) throws RemoteException {
            int _arg0 = data.readInt();
            String _arg1 = data.readString();
            String _arg2 = data.readString();
            data.enforceNoDataAvail();
            int _result = changeIccLockPassword(_arg0, _arg1, _arg2);
            reply.writeNoException();
            reply.writeInt(_result);
            return true;
        }

        private boolean onTransact$getEquivalentHomePlmns$(Parcel data, Parcel reply) throws RemoteException {
            int _arg0 = data.readInt();
            String _arg1 = data.readString();
            String _arg2 = data.readString();
            data.enforceNoDataAvail();
            List<String> _result = getEquivalentHomePlmns(_arg0, _arg1, _arg2);
            reply.writeNoException();
            reply.writeStringList(_result);
            return true;
        }

        private boolean onTransact$sendThermalMitigationRequest$(Parcel data, Parcel reply) throws RemoteException {
            int _arg0 = data.readInt();
            ThermalMitigationRequest _arg1 = (ThermalMitigationRequest) data.readTypedObject(ThermalMitigationRequest.CREATOR);
            String _arg2 = data.readString();
            data.enforceNoDataAvail();
            int _result = sendThermalMitigationRequest(_arg0, _arg1, _arg2);
            reply.writeNoException();
            reply.writeInt(_result);
            return true;
        }

        private boolean onTransact$bootstrapAuthenticationRequest$(Parcel data, Parcel reply) throws RemoteException {
            int _arg0 = data.readInt();
            int _arg1 = data.readInt();
            Uri _arg2 = (Uri) data.readTypedObject(Uri.CREATOR);
            UaSecurityProtocolIdentifier _arg3 = (UaSecurityProtocolIdentifier) data.readTypedObject(UaSecurityProtocolIdentifier.CREATOR);
            boolean _arg4 = data.readBoolean();
            IBootstrapAuthenticationCallback _arg5 = IBootstrapAuthenticationCallback.Stub.asInterface(data.readStrongBinder());
            data.enforceNoDataAvail();
            bootstrapAuthenticationRequest(_arg0, _arg1, _arg2, _arg3, _arg4, _arg5);
            reply.writeNoException();
            return true;
        }

        private boolean onTransact$setSignalStrengthUpdateRequest$(Parcel data, Parcel reply) throws RemoteException {
            int _arg0 = data.readInt();
            SignalStrengthUpdateRequest _arg1 = (SignalStrengthUpdateRequest) data.readTypedObject(SignalStrengthUpdateRequest.CREATOR);
            String _arg2 = data.readString();
            data.enforceNoDataAvail();
            setSignalStrengthUpdateRequest(_arg0, _arg1, _arg2);
            reply.writeNoException();
            return true;
        }

        private boolean onTransact$clearSignalStrengthUpdateRequest$(Parcel data, Parcel reply) throws RemoteException {
            int _arg0 = data.readInt();
            SignalStrengthUpdateRequest _arg1 = (SignalStrengthUpdateRequest) data.readTypedObject(SignalStrengthUpdateRequest.CREATOR);
            String _arg2 = data.readString();
            data.enforceNoDataAvail();
            clearSignalStrengthUpdateRequest(_arg0, _arg1, _arg2);
            reply.writeNoException();
            return true;
        }

        private boolean onTransact$registerImsStateCallback$(Parcel data, Parcel reply) throws RemoteException {
            int _arg0 = data.readInt();
            int _arg1 = data.readInt();
            IImsStateCallback _arg2 = IImsStateCallback.Stub.asInterface(data.readStrongBinder());
            String _arg3 = data.readString();
            data.enforceNoDataAvail();
            registerImsStateCallback(_arg0, _arg1, _arg2, _arg3);
            reply.writeNoException();
            return true;
        }

        private boolean onTransact$getLastKnownCellIdentity$(Parcel data, Parcel reply) throws RemoteException {
            int _arg0 = data.readInt();
            String _arg1 = data.readString();
            String _arg2 = data.readString();
            data.enforceNoDataAvail();
            CellIdentity _result = getLastKnownCellIdentity(_arg0, _arg1, _arg2);
            reply.writeNoException();
            reply.writeTypedObject(_result, 1);
            return true;
        }

        private boolean onTransact$isProvisioningRequiredForCapability$(Parcel data, Parcel reply) throws RemoteException {
            int _arg0 = data.readInt();
            int _arg1 = data.readInt();
            int _arg2 = data.readInt();
            data.enforceNoDataAvail();
            boolean _result = isProvisioningRequiredForCapability(_arg0, _arg1, _arg2);
            reply.writeNoException();
            reply.writeBoolean(_result);
            return true;
        }

        private boolean onTransact$isRcsProvisioningRequiredForCapability$(Parcel data, Parcel reply) throws RemoteException {
            int _arg0 = data.readInt();
            int _arg1 = data.readInt();
            int _arg2 = data.readInt();
            data.enforceNoDataAvail();
            boolean _result = isRcsProvisioningRequiredForCapability(_arg0, _arg1, _arg2);
            reply.writeNoException();
            reply.writeBoolean(_result);
            return true;
        }

        private boolean onTransact$setVoiceServiceStateOverride$(Parcel data, Parcel reply) throws RemoteException {
            int _arg0 = data.readInt();
            boolean _arg1 = data.readBoolean();
            String _arg2 = data.readString();
            data.enforceNoDataAvail();
            setVoiceServiceStateOverride(_arg0, _arg1, _arg2);
            reply.writeNoException();
            return true;
        }

        @Override // android.os.Binder
        public int getMaxTransactionId() {
            return 339;
        }
    }
}
