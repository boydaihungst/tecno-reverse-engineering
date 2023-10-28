package com.android.internal.telephony;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.ParcelUuid;
import android.os.RemoteException;
import android.telephony.SubscriptionInfo;
import com.android.internal.telephony.ISetOpportunisticDataCallback;
import java.util.List;
/* loaded from: classes4.dex */
public interface ISub extends IInterface {
    int addSubInfo(String str, String str2, int i, int i2) throws RemoteException;

    int addSubInfoRecord(String str, int i) throws RemoteException;

    void addSubscriptionsIntoGroup(int[] iArr, ParcelUuid parcelUuid, String str) throws RemoteException;

    boolean canDisablePhysicalSubscription() throws RemoteException;

    int clearSubInfo() throws RemoteException;

    ParcelUuid createSubscriptionGroup(int[] iArr, String str) throws RemoteException;

    List<SubscriptionInfo> getAccessibleSubscriptionInfoList(String str) throws RemoteException;

    int getActiveDataSubscriptionId() throws RemoteException;

    int[] getActiveSubIdList(boolean z) throws RemoteException;

    int getActiveSubInfoCount(String str, String str2) throws RemoteException;

    int getActiveSubInfoCountMax() throws RemoteException;

    SubscriptionInfo getActiveSubscriptionInfo(int i, String str, String str2) throws RemoteException;

    SubscriptionInfo getActiveSubscriptionInfoForIccId(String str, String str2, String str3) throws RemoteException;

    SubscriptionInfo getActiveSubscriptionInfoForSimSlotIndex(int i, String str, String str2) throws RemoteException;

    List<SubscriptionInfo> getActiveSubscriptionInfoList(String str, String str2) throws RemoteException;

    int getAllSubInfoCount(String str, String str2) throws RemoteException;

    List<SubscriptionInfo> getAllSubInfoList(String str, String str2) throws RemoteException;

    List<SubscriptionInfo> getAvailableSubscriptionInfoList(String str, String str2) throws RemoteException;

    int getDefaultDataSubId() throws RemoteException;

    int getDefaultSmsSubId() throws RemoteException;

    int getDefaultSubId() throws RemoteException;

    int getDefaultVoiceSubId() throws RemoteException;

    int getEnabledSubscriptionId(int i) throws RemoteException;

    List<SubscriptionInfo> getOpportunisticSubscriptions(String str, String str2) throws RemoteException;

    int getPhoneId(int i) throws RemoteException;

    String getPhoneNumber(int i, int i2, String str, String str2) throws RemoteException;

    String getPhoneNumberFromFirstAvailableSource(int i, String str, String str2) throws RemoteException;

    int getPreferredDataSubscriptionId() throws RemoteException;

    int getSimStateForSlotIndex(int i) throws RemoteException;

    int getSlotIndex(int i) throws RemoteException;

    int[] getSubId(int i) throws RemoteException;

    String getSubscriptionProperty(int i, String str, String str2, String str3) throws RemoteException;

    List<SubscriptionInfo> getSubscriptionsInGroup(ParcelUuid parcelUuid, String str, String str2) throws RemoteException;

    boolean isActiveSubId(int i, String str, String str2) throws RemoteException;

    boolean isSubscriptionEnabled(int i) throws RemoteException;

    int removeSubInfo(String str, int i) throws RemoteException;

    void removeSubscriptionsFromGroup(int[] iArr, ParcelUuid parcelUuid, String str) throws RemoteException;

    void requestEmbeddedSubscriptionInfoListRefresh(int i) throws RemoteException;

    int setDataRoaming(int i, int i2) throws RemoteException;

    void setDefaultDataSubId(int i) throws RemoteException;

    void setDefaultDataSubIdWithReason(int i, int i2) throws RemoteException;

    void setDefaultSmsSubId(int i) throws RemoteException;

    void setDefaultVoiceSubId(int i) throws RemoteException;

    int setDeviceToDeviceStatusSharing(int i, int i2) throws RemoteException;

    int setDeviceToDeviceStatusSharingContacts(String str, int i) throws RemoteException;

    int setDisplayNameUsingSrc(String str, int i, int i2) throws RemoteException;

    int setDisplayNumber(String str, int i) throws RemoteException;

    int setIconTint(int i, int i2) throws RemoteException;

    int setOpportunistic(boolean z, int i, String str) throws RemoteException;

    void setPhoneNumber(int i, int i2, String str, String str2, String str3) throws RemoteException;

    void setPreferredDataSubscriptionId(int i, boolean z, ISetOpportunisticDataCallback iSetOpportunisticDataCallback) throws RemoteException;

    boolean setSubscriptionEnabled(boolean z, int i) throws RemoteException;

    int setSubscriptionProperty(int i, String str, String str2) throws RemoteException;

    int setUiccApplicationsEnabled(boolean z, int i) throws RemoteException;

    int setUsageSetting(int i, int i2, String str) throws RemoteException;

    /* loaded from: classes4.dex */
    public static class Default implements ISub {
        @Override // com.android.internal.telephony.ISub
        public List<SubscriptionInfo> getAllSubInfoList(String callingPackage, String callingFeatureId) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.telephony.ISub
        public int getAllSubInfoCount(String callingPackage, String callingFeatureId) throws RemoteException {
            return 0;
        }

        @Override // com.android.internal.telephony.ISub
        public SubscriptionInfo getActiveSubscriptionInfo(int subId, String callingPackage, String callingFeatureId) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.telephony.ISub
        public SubscriptionInfo getActiveSubscriptionInfoForIccId(String iccId, String callingPackage, String callingFeatureId) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.telephony.ISub
        public SubscriptionInfo getActiveSubscriptionInfoForSimSlotIndex(int slotIndex, String callingPackage, String callingFeatureId) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.telephony.ISub
        public List<SubscriptionInfo> getActiveSubscriptionInfoList(String callingPackage, String callingFeatureId) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.telephony.ISub
        public int getActiveSubInfoCount(String callingPackage, String callingFeatureId) throws RemoteException {
            return 0;
        }

        @Override // com.android.internal.telephony.ISub
        public int getActiveSubInfoCountMax() throws RemoteException {
            return 0;
        }

        @Override // com.android.internal.telephony.ISub
        public List<SubscriptionInfo> getAvailableSubscriptionInfoList(String callingPackage, String callingFeatureId) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.telephony.ISub
        public List<SubscriptionInfo> getAccessibleSubscriptionInfoList(String callingPackage) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.telephony.ISub
        public void requestEmbeddedSubscriptionInfoListRefresh(int cardId) throws RemoteException {
        }

        @Override // com.android.internal.telephony.ISub
        public int addSubInfoRecord(String iccId, int slotIndex) throws RemoteException {
            return 0;
        }

        @Override // com.android.internal.telephony.ISub
        public int addSubInfo(String uniqueId, String displayName, int slotIndex, int subscriptionType) throws RemoteException {
            return 0;
        }

        @Override // com.android.internal.telephony.ISub
        public int removeSubInfo(String uniqueId, int subscriptionType) throws RemoteException {
            return 0;
        }

        @Override // com.android.internal.telephony.ISub
        public int setIconTint(int tint, int subId) throws RemoteException {
            return 0;
        }

        @Override // com.android.internal.telephony.ISub
        public int setDisplayNameUsingSrc(String displayName, int subId, int nameSource) throws RemoteException {
            return 0;
        }

        @Override // com.android.internal.telephony.ISub
        public int setDisplayNumber(String number, int subId) throws RemoteException {
            return 0;
        }

        @Override // com.android.internal.telephony.ISub
        public int setDataRoaming(int roaming, int subId) throws RemoteException {
            return 0;
        }

        @Override // com.android.internal.telephony.ISub
        public int setOpportunistic(boolean opportunistic, int subId, String callingPackage) throws RemoteException {
            return 0;
        }

        @Override // com.android.internal.telephony.ISub
        public ParcelUuid createSubscriptionGroup(int[] subIdList, String callingPackage) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.telephony.ISub
        public void setPreferredDataSubscriptionId(int subId, boolean needValidation, ISetOpportunisticDataCallback callback) throws RemoteException {
        }

        @Override // com.android.internal.telephony.ISub
        public int getPreferredDataSubscriptionId() throws RemoteException {
            return 0;
        }

        @Override // com.android.internal.telephony.ISub
        public List<SubscriptionInfo> getOpportunisticSubscriptions(String callingPackage, String callingFeatureId) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.telephony.ISub
        public void removeSubscriptionsFromGroup(int[] subIdList, ParcelUuid groupUuid, String callingPackage) throws RemoteException {
        }

        @Override // com.android.internal.telephony.ISub
        public void addSubscriptionsIntoGroup(int[] subIdList, ParcelUuid groupUuid, String callingPackage) throws RemoteException {
        }

        @Override // com.android.internal.telephony.ISub
        public List<SubscriptionInfo> getSubscriptionsInGroup(ParcelUuid groupUuid, String callingPackage, String callingFeatureId) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.telephony.ISub
        public int getSlotIndex(int subId) throws RemoteException {
            return 0;
        }

        @Override // com.android.internal.telephony.ISub
        public int[] getSubId(int slotIndex) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.telephony.ISub
        public int getDefaultSubId() throws RemoteException {
            return 0;
        }

        @Override // com.android.internal.telephony.ISub
        public int clearSubInfo() throws RemoteException {
            return 0;
        }

        @Override // com.android.internal.telephony.ISub
        public int getPhoneId(int subId) throws RemoteException {
            return 0;
        }

        @Override // com.android.internal.telephony.ISub
        public int getDefaultDataSubId() throws RemoteException {
            return 0;
        }

        @Override // com.android.internal.telephony.ISub
        public void setDefaultDataSubId(int subId) throws RemoteException {
        }

        @Override // com.android.internal.telephony.ISub
        public void setDefaultDataSubIdWithReason(int subId, int cause) throws RemoteException {
        }

        @Override // com.android.internal.telephony.ISub
        public int getDefaultVoiceSubId() throws RemoteException {
            return 0;
        }

        @Override // com.android.internal.telephony.ISub
        public void setDefaultVoiceSubId(int subId) throws RemoteException {
        }

        @Override // com.android.internal.telephony.ISub
        public int getDefaultSmsSubId() throws RemoteException {
            return 0;
        }

        @Override // com.android.internal.telephony.ISub
        public void setDefaultSmsSubId(int subId) throws RemoteException {
        }

        @Override // com.android.internal.telephony.ISub
        public int[] getActiveSubIdList(boolean visibleOnly) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.telephony.ISub
        public int setSubscriptionProperty(int subId, String propKey, String propValue) throws RemoteException {
            return 0;
        }

        @Override // com.android.internal.telephony.ISub
        public String getSubscriptionProperty(int subId, String propKey, String callingPackage, String callingFeatureId) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.telephony.ISub
        public boolean setSubscriptionEnabled(boolean enable, int subId) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.telephony.ISub
        public boolean isSubscriptionEnabled(int subId) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.telephony.ISub
        public int getEnabledSubscriptionId(int slotIndex) throws RemoteException {
            return 0;
        }

        @Override // com.android.internal.telephony.ISub
        public int getSimStateForSlotIndex(int slotIndex) throws RemoteException {
            return 0;
        }

        @Override // com.android.internal.telephony.ISub
        public boolean isActiveSubId(int subId, String callingPackage, String callingFeatureId) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.telephony.ISub
        public int getActiveDataSubscriptionId() throws RemoteException {
            return 0;
        }

        @Override // com.android.internal.telephony.ISub
        public boolean canDisablePhysicalSubscription() throws RemoteException {
            return false;
        }

        @Override // com.android.internal.telephony.ISub
        public int setUiccApplicationsEnabled(boolean enabled, int subscriptionId) throws RemoteException {
            return 0;
        }

        @Override // com.android.internal.telephony.ISub
        public int setDeviceToDeviceStatusSharing(int sharing, int subId) throws RemoteException {
            return 0;
        }

        @Override // com.android.internal.telephony.ISub
        public int setDeviceToDeviceStatusSharingContacts(String contacts, int subscriptionId) throws RemoteException {
            return 0;
        }

        @Override // com.android.internal.telephony.ISub
        public String getPhoneNumber(int subId, int source, String callingPackage, String callingFeatureId) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.telephony.ISub
        public String getPhoneNumberFromFirstAvailableSource(int subId, String callingPackage, String callingFeatureId) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.telephony.ISub
        public void setPhoneNumber(int subId, int source, String number, String callingPackage, String callingFeatureId) throws RemoteException {
        }

        @Override // com.android.internal.telephony.ISub
        public int setUsageSetting(int usageSetting, int subId, String callingPackage) throws RemoteException {
            return 0;
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes4.dex */
    public static abstract class Stub extends Binder implements ISub {
        public static final String DESCRIPTOR = "com.android.internal.telephony.ISub";
        static final int TRANSACTION_addSubInfo = 13;
        static final int TRANSACTION_addSubInfoRecord = 12;
        static final int TRANSACTION_addSubscriptionsIntoGroup = 25;
        static final int TRANSACTION_canDisablePhysicalSubscription = 48;
        static final int TRANSACTION_clearSubInfo = 30;
        static final int TRANSACTION_createSubscriptionGroup = 20;
        static final int TRANSACTION_getAccessibleSubscriptionInfoList = 10;
        static final int TRANSACTION_getActiveDataSubscriptionId = 47;
        static final int TRANSACTION_getActiveSubIdList = 39;
        static final int TRANSACTION_getActiveSubInfoCount = 7;
        static final int TRANSACTION_getActiveSubInfoCountMax = 8;
        static final int TRANSACTION_getActiveSubscriptionInfo = 3;
        static final int TRANSACTION_getActiveSubscriptionInfoForIccId = 4;
        static final int TRANSACTION_getActiveSubscriptionInfoForSimSlotIndex = 5;
        static final int TRANSACTION_getActiveSubscriptionInfoList = 6;
        static final int TRANSACTION_getAllSubInfoCount = 2;
        static final int TRANSACTION_getAllSubInfoList = 1;
        static final int TRANSACTION_getAvailableSubscriptionInfoList = 9;
        static final int TRANSACTION_getDefaultDataSubId = 32;
        static final int TRANSACTION_getDefaultSmsSubId = 37;
        static final int TRANSACTION_getDefaultSubId = 29;
        static final int TRANSACTION_getDefaultVoiceSubId = 35;
        static final int TRANSACTION_getEnabledSubscriptionId = 44;
        static final int TRANSACTION_getOpportunisticSubscriptions = 23;
        static final int TRANSACTION_getPhoneId = 31;
        static final int TRANSACTION_getPhoneNumber = 52;
        static final int TRANSACTION_getPhoneNumberFromFirstAvailableSource = 53;
        static final int TRANSACTION_getPreferredDataSubscriptionId = 22;
        static final int TRANSACTION_getSimStateForSlotIndex = 45;
        static final int TRANSACTION_getSlotIndex = 27;
        static final int TRANSACTION_getSubId = 28;
        static final int TRANSACTION_getSubscriptionProperty = 41;
        static final int TRANSACTION_getSubscriptionsInGroup = 26;
        static final int TRANSACTION_isActiveSubId = 46;
        static final int TRANSACTION_isSubscriptionEnabled = 43;
        static final int TRANSACTION_removeSubInfo = 14;
        static final int TRANSACTION_removeSubscriptionsFromGroup = 24;
        static final int TRANSACTION_requestEmbeddedSubscriptionInfoListRefresh = 11;
        static final int TRANSACTION_setDataRoaming = 18;
        static final int TRANSACTION_setDefaultDataSubId = 33;
        static final int TRANSACTION_setDefaultDataSubIdWithReason = 34;
        static final int TRANSACTION_setDefaultSmsSubId = 38;
        static final int TRANSACTION_setDefaultVoiceSubId = 36;
        static final int TRANSACTION_setDeviceToDeviceStatusSharing = 50;
        static final int TRANSACTION_setDeviceToDeviceStatusSharingContacts = 51;
        static final int TRANSACTION_setDisplayNameUsingSrc = 16;
        static final int TRANSACTION_setDisplayNumber = 17;
        static final int TRANSACTION_setIconTint = 15;
        static final int TRANSACTION_setOpportunistic = 19;
        static final int TRANSACTION_setPhoneNumber = 54;
        static final int TRANSACTION_setPreferredDataSubscriptionId = 21;
        static final int TRANSACTION_setSubscriptionEnabled = 42;
        static final int TRANSACTION_setSubscriptionProperty = 40;
        static final int TRANSACTION_setUiccApplicationsEnabled = 49;
        static final int TRANSACTION_setUsageSetting = 55;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static ISub asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof ISub)) {
                return (ISub) iin;
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
                    return "getAllSubInfoList";
                case 2:
                    return "getAllSubInfoCount";
                case 3:
                    return "getActiveSubscriptionInfo";
                case 4:
                    return "getActiveSubscriptionInfoForIccId";
                case 5:
                    return "getActiveSubscriptionInfoForSimSlotIndex";
                case 6:
                    return "getActiveSubscriptionInfoList";
                case 7:
                    return "getActiveSubInfoCount";
                case 8:
                    return "getActiveSubInfoCountMax";
                case 9:
                    return "getAvailableSubscriptionInfoList";
                case 10:
                    return "getAccessibleSubscriptionInfoList";
                case 11:
                    return "requestEmbeddedSubscriptionInfoListRefresh";
                case 12:
                    return "addSubInfoRecord";
                case 13:
                    return "addSubInfo";
                case 14:
                    return "removeSubInfo";
                case 15:
                    return "setIconTint";
                case 16:
                    return "setDisplayNameUsingSrc";
                case 17:
                    return "setDisplayNumber";
                case 18:
                    return "setDataRoaming";
                case 19:
                    return "setOpportunistic";
                case 20:
                    return "createSubscriptionGroup";
                case 21:
                    return "setPreferredDataSubscriptionId";
                case 22:
                    return "getPreferredDataSubscriptionId";
                case 23:
                    return "getOpportunisticSubscriptions";
                case 24:
                    return "removeSubscriptionsFromGroup";
                case 25:
                    return "addSubscriptionsIntoGroup";
                case 26:
                    return "getSubscriptionsInGroup";
                case 27:
                    return "getSlotIndex";
                case 28:
                    return "getSubId";
                case 29:
                    return "getDefaultSubId";
                case 30:
                    return "clearSubInfo";
                case 31:
                    return "getPhoneId";
                case 32:
                    return "getDefaultDataSubId";
                case 33:
                    return "setDefaultDataSubId";
                case 34:
                    return "setDefaultDataSubIdWithReason";
                case 35:
                    return "getDefaultVoiceSubId";
                case 36:
                    return "setDefaultVoiceSubId";
                case 37:
                    return "getDefaultSmsSubId";
                case 38:
                    return "setDefaultSmsSubId";
                case 39:
                    return "getActiveSubIdList";
                case 40:
                    return "setSubscriptionProperty";
                case 41:
                    return "getSubscriptionProperty";
                case 42:
                    return "setSubscriptionEnabled";
                case 43:
                    return "isSubscriptionEnabled";
                case 44:
                    return "getEnabledSubscriptionId";
                case 45:
                    return "getSimStateForSlotIndex";
                case 46:
                    return "isActiveSubId";
                case 47:
                    return "getActiveDataSubscriptionId";
                case 48:
                    return "canDisablePhysicalSubscription";
                case 49:
                    return "setUiccApplicationsEnabled";
                case 50:
                    return "setDeviceToDeviceStatusSharing";
                case 51:
                    return "setDeviceToDeviceStatusSharingContacts";
                case 52:
                    return "getPhoneNumber";
                case 53:
                    return "getPhoneNumberFromFirstAvailableSource";
                case 54:
                    return "setPhoneNumber";
                case 55:
                    return "setUsageSetting";
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
                            String _arg1 = data.readString();
                            data.enforceNoDataAvail();
                            List<SubscriptionInfo> _result = getAllSubInfoList(_arg0, _arg1);
                            reply.writeNoException();
                            reply.writeTypedList(_result);
                            break;
                        case 2:
                            String _arg02 = data.readString();
                            String _arg12 = data.readString();
                            data.enforceNoDataAvail();
                            int _result2 = getAllSubInfoCount(_arg02, _arg12);
                            reply.writeNoException();
                            reply.writeInt(_result2);
                            break;
                        case 3:
                            int _arg03 = data.readInt();
                            String _arg13 = data.readString();
                            String _arg2 = data.readString();
                            data.enforceNoDataAvail();
                            SubscriptionInfo _result3 = getActiveSubscriptionInfo(_arg03, _arg13, _arg2);
                            reply.writeNoException();
                            reply.writeTypedObject(_result3, 1);
                            break;
                        case 4:
                            String _arg04 = data.readString();
                            String _arg14 = data.readString();
                            String _arg22 = data.readString();
                            data.enforceNoDataAvail();
                            SubscriptionInfo _result4 = getActiveSubscriptionInfoForIccId(_arg04, _arg14, _arg22);
                            reply.writeNoException();
                            reply.writeTypedObject(_result4, 1);
                            break;
                        case 5:
                            int _arg05 = data.readInt();
                            String _arg15 = data.readString();
                            String _arg23 = data.readString();
                            data.enforceNoDataAvail();
                            SubscriptionInfo _result5 = getActiveSubscriptionInfoForSimSlotIndex(_arg05, _arg15, _arg23);
                            reply.writeNoException();
                            reply.writeTypedObject(_result5, 1);
                            break;
                        case 6:
                            String _arg06 = data.readString();
                            String _arg16 = data.readString();
                            data.enforceNoDataAvail();
                            List<SubscriptionInfo> _result6 = getActiveSubscriptionInfoList(_arg06, _arg16);
                            reply.writeNoException();
                            reply.writeTypedList(_result6);
                            break;
                        case 7:
                            String _arg07 = data.readString();
                            String _arg17 = data.readString();
                            data.enforceNoDataAvail();
                            int _result7 = getActiveSubInfoCount(_arg07, _arg17);
                            reply.writeNoException();
                            reply.writeInt(_result7);
                            break;
                        case 8:
                            int _result8 = getActiveSubInfoCountMax();
                            reply.writeNoException();
                            reply.writeInt(_result8);
                            break;
                        case 9:
                            String _arg08 = data.readString();
                            String _arg18 = data.readString();
                            data.enforceNoDataAvail();
                            List<SubscriptionInfo> _result9 = getAvailableSubscriptionInfoList(_arg08, _arg18);
                            reply.writeNoException();
                            reply.writeTypedList(_result9);
                            break;
                        case 10:
                            String _arg09 = data.readString();
                            data.enforceNoDataAvail();
                            List<SubscriptionInfo> _result10 = getAccessibleSubscriptionInfoList(_arg09);
                            reply.writeNoException();
                            reply.writeTypedList(_result10);
                            break;
                        case 11:
                            int _arg010 = data.readInt();
                            data.enforceNoDataAvail();
                            requestEmbeddedSubscriptionInfoListRefresh(_arg010);
                            break;
                        case 12:
                            String _arg011 = data.readString();
                            int _arg19 = data.readInt();
                            data.enforceNoDataAvail();
                            int _result11 = addSubInfoRecord(_arg011, _arg19);
                            reply.writeNoException();
                            reply.writeInt(_result11);
                            break;
                        case 13:
                            String _arg012 = data.readString();
                            String _arg110 = data.readString();
                            int _arg24 = data.readInt();
                            int _arg3 = data.readInt();
                            data.enforceNoDataAvail();
                            int _result12 = addSubInfo(_arg012, _arg110, _arg24, _arg3);
                            reply.writeNoException();
                            reply.writeInt(_result12);
                            break;
                        case 14:
                            String _arg013 = data.readString();
                            int _arg111 = data.readInt();
                            data.enforceNoDataAvail();
                            int _result13 = removeSubInfo(_arg013, _arg111);
                            reply.writeNoException();
                            reply.writeInt(_result13);
                            break;
                        case 15:
                            int _arg014 = data.readInt();
                            int _arg112 = data.readInt();
                            data.enforceNoDataAvail();
                            int _result14 = setIconTint(_arg014, _arg112);
                            reply.writeNoException();
                            reply.writeInt(_result14);
                            break;
                        case 16:
                            String _arg015 = data.readString();
                            int _arg113 = data.readInt();
                            int _arg25 = data.readInt();
                            data.enforceNoDataAvail();
                            int _result15 = setDisplayNameUsingSrc(_arg015, _arg113, _arg25);
                            reply.writeNoException();
                            reply.writeInt(_result15);
                            break;
                        case 17:
                            String _arg016 = data.readString();
                            int _arg114 = data.readInt();
                            data.enforceNoDataAvail();
                            int _result16 = setDisplayNumber(_arg016, _arg114);
                            reply.writeNoException();
                            reply.writeInt(_result16);
                            break;
                        case 18:
                            int _arg017 = data.readInt();
                            int _arg115 = data.readInt();
                            data.enforceNoDataAvail();
                            int _result17 = setDataRoaming(_arg017, _arg115);
                            reply.writeNoException();
                            reply.writeInt(_result17);
                            break;
                        case 19:
                            boolean _arg018 = data.readBoolean();
                            int _arg116 = data.readInt();
                            String _arg26 = data.readString();
                            data.enforceNoDataAvail();
                            int _result18 = setOpportunistic(_arg018, _arg116, _arg26);
                            reply.writeNoException();
                            reply.writeInt(_result18);
                            break;
                        case 20:
                            int[] _arg019 = data.createIntArray();
                            String _arg117 = data.readString();
                            data.enforceNoDataAvail();
                            ParcelUuid _result19 = createSubscriptionGroup(_arg019, _arg117);
                            reply.writeNoException();
                            reply.writeTypedObject(_result19, 1);
                            break;
                        case 21:
                            int _arg020 = data.readInt();
                            boolean _arg118 = data.readBoolean();
                            ISetOpportunisticDataCallback _arg27 = ISetOpportunisticDataCallback.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            setPreferredDataSubscriptionId(_arg020, _arg118, _arg27);
                            reply.writeNoException();
                            break;
                        case 22:
                            int _result20 = getPreferredDataSubscriptionId();
                            reply.writeNoException();
                            reply.writeInt(_result20);
                            break;
                        case 23:
                            String _arg021 = data.readString();
                            String _arg119 = data.readString();
                            data.enforceNoDataAvail();
                            List<SubscriptionInfo> _result21 = getOpportunisticSubscriptions(_arg021, _arg119);
                            reply.writeNoException();
                            reply.writeTypedList(_result21);
                            break;
                        case 24:
                            int[] _arg022 = data.createIntArray();
                            ParcelUuid _arg120 = (ParcelUuid) data.readTypedObject(ParcelUuid.CREATOR);
                            String _arg28 = data.readString();
                            data.enforceNoDataAvail();
                            removeSubscriptionsFromGroup(_arg022, _arg120, _arg28);
                            reply.writeNoException();
                            break;
                        case 25:
                            int[] _arg023 = data.createIntArray();
                            ParcelUuid _arg121 = (ParcelUuid) data.readTypedObject(ParcelUuid.CREATOR);
                            String _arg29 = data.readString();
                            data.enforceNoDataAvail();
                            addSubscriptionsIntoGroup(_arg023, _arg121, _arg29);
                            reply.writeNoException();
                            break;
                        case 26:
                            ParcelUuid _arg024 = (ParcelUuid) data.readTypedObject(ParcelUuid.CREATOR);
                            String _arg122 = data.readString();
                            String _arg210 = data.readString();
                            data.enforceNoDataAvail();
                            List<SubscriptionInfo> _result22 = getSubscriptionsInGroup(_arg024, _arg122, _arg210);
                            reply.writeNoException();
                            reply.writeTypedList(_result22);
                            break;
                        case 27:
                            int _arg025 = data.readInt();
                            data.enforceNoDataAvail();
                            int _result23 = getSlotIndex(_arg025);
                            reply.writeNoException();
                            reply.writeInt(_result23);
                            break;
                        case 28:
                            int _arg026 = data.readInt();
                            data.enforceNoDataAvail();
                            int[] _result24 = getSubId(_arg026);
                            reply.writeNoException();
                            reply.writeIntArray(_result24);
                            break;
                        case 29:
                            int _result25 = getDefaultSubId();
                            reply.writeNoException();
                            reply.writeInt(_result25);
                            break;
                        case 30:
                            int _result26 = clearSubInfo();
                            reply.writeNoException();
                            reply.writeInt(_result26);
                            break;
                        case 31:
                            int _arg027 = data.readInt();
                            data.enforceNoDataAvail();
                            int _result27 = getPhoneId(_arg027);
                            reply.writeNoException();
                            reply.writeInt(_result27);
                            break;
                        case 32:
                            int _result28 = getDefaultDataSubId();
                            reply.writeNoException();
                            reply.writeInt(_result28);
                            break;
                        case 33:
                            int _arg028 = data.readInt();
                            data.enforceNoDataAvail();
                            setDefaultDataSubId(_arg028);
                            reply.writeNoException();
                            break;
                        case 34:
                            int _arg029 = data.readInt();
                            int _arg123 = data.readInt();
                            data.enforceNoDataAvail();
                            setDefaultDataSubIdWithReason(_arg029, _arg123);
                            reply.writeNoException();
                            break;
                        case 35:
                            int _result29 = getDefaultVoiceSubId();
                            reply.writeNoException();
                            reply.writeInt(_result29);
                            break;
                        case 36:
                            int _arg030 = data.readInt();
                            data.enforceNoDataAvail();
                            setDefaultVoiceSubId(_arg030);
                            reply.writeNoException();
                            break;
                        case 37:
                            int _result30 = getDefaultSmsSubId();
                            reply.writeNoException();
                            reply.writeInt(_result30);
                            break;
                        case 38:
                            int _arg031 = data.readInt();
                            data.enforceNoDataAvail();
                            setDefaultSmsSubId(_arg031);
                            reply.writeNoException();
                            break;
                        case 39:
                            boolean _arg032 = data.readBoolean();
                            data.enforceNoDataAvail();
                            int[] _result31 = getActiveSubIdList(_arg032);
                            reply.writeNoException();
                            reply.writeIntArray(_result31);
                            break;
                        case 40:
                            int _arg033 = data.readInt();
                            String _arg124 = data.readString();
                            String _arg211 = data.readString();
                            data.enforceNoDataAvail();
                            int _result32 = setSubscriptionProperty(_arg033, _arg124, _arg211);
                            reply.writeNoException();
                            reply.writeInt(_result32);
                            break;
                        case 41:
                            int _arg034 = data.readInt();
                            String _arg125 = data.readString();
                            String _arg212 = data.readString();
                            String _arg32 = data.readString();
                            data.enforceNoDataAvail();
                            String _result33 = getSubscriptionProperty(_arg034, _arg125, _arg212, _arg32);
                            reply.writeNoException();
                            reply.writeString(_result33);
                            break;
                        case 42:
                            boolean _arg035 = data.readBoolean();
                            int _arg126 = data.readInt();
                            data.enforceNoDataAvail();
                            boolean _result34 = setSubscriptionEnabled(_arg035, _arg126);
                            reply.writeNoException();
                            reply.writeBoolean(_result34);
                            break;
                        case 43:
                            int _arg036 = data.readInt();
                            data.enforceNoDataAvail();
                            boolean _result35 = isSubscriptionEnabled(_arg036);
                            reply.writeNoException();
                            reply.writeBoolean(_result35);
                            break;
                        case 44:
                            int _arg037 = data.readInt();
                            data.enforceNoDataAvail();
                            int _result36 = getEnabledSubscriptionId(_arg037);
                            reply.writeNoException();
                            reply.writeInt(_result36);
                            break;
                        case 45:
                            int _arg038 = data.readInt();
                            data.enforceNoDataAvail();
                            int _result37 = getSimStateForSlotIndex(_arg038);
                            reply.writeNoException();
                            reply.writeInt(_result37);
                            break;
                        case 46:
                            int _arg039 = data.readInt();
                            String _arg127 = data.readString();
                            String _arg213 = data.readString();
                            data.enforceNoDataAvail();
                            boolean _result38 = isActiveSubId(_arg039, _arg127, _arg213);
                            reply.writeNoException();
                            reply.writeBoolean(_result38);
                            break;
                        case 47:
                            int _result39 = getActiveDataSubscriptionId();
                            reply.writeNoException();
                            reply.writeInt(_result39);
                            break;
                        case 48:
                            boolean _result40 = canDisablePhysicalSubscription();
                            reply.writeNoException();
                            reply.writeBoolean(_result40);
                            break;
                        case 49:
                            boolean _arg040 = data.readBoolean();
                            int _arg128 = data.readInt();
                            data.enforceNoDataAvail();
                            int _result41 = setUiccApplicationsEnabled(_arg040, _arg128);
                            reply.writeNoException();
                            reply.writeInt(_result41);
                            break;
                        case 50:
                            int _arg041 = data.readInt();
                            int _arg129 = data.readInt();
                            data.enforceNoDataAvail();
                            int _result42 = setDeviceToDeviceStatusSharing(_arg041, _arg129);
                            reply.writeNoException();
                            reply.writeInt(_result42);
                            break;
                        case 51:
                            String _arg042 = data.readString();
                            int _arg130 = data.readInt();
                            data.enforceNoDataAvail();
                            int _result43 = setDeviceToDeviceStatusSharingContacts(_arg042, _arg130);
                            reply.writeNoException();
                            reply.writeInt(_result43);
                            break;
                        case 52:
                            int _arg043 = data.readInt();
                            int _arg131 = data.readInt();
                            String _arg214 = data.readString();
                            String _arg33 = data.readString();
                            data.enforceNoDataAvail();
                            String _result44 = getPhoneNumber(_arg043, _arg131, _arg214, _arg33);
                            reply.writeNoException();
                            reply.writeString(_result44);
                            break;
                        case 53:
                            int _arg044 = data.readInt();
                            String _arg132 = data.readString();
                            String _arg215 = data.readString();
                            data.enforceNoDataAvail();
                            String _result45 = getPhoneNumberFromFirstAvailableSource(_arg044, _arg132, _arg215);
                            reply.writeNoException();
                            reply.writeString(_result45);
                            break;
                        case 54:
                            int _arg045 = data.readInt();
                            int _arg133 = data.readInt();
                            String _arg216 = data.readString();
                            String _arg34 = data.readString();
                            String _arg4 = data.readString();
                            data.enforceNoDataAvail();
                            setPhoneNumber(_arg045, _arg133, _arg216, _arg34, _arg4);
                            reply.writeNoException();
                            break;
                        case 55:
                            int _arg046 = data.readInt();
                            int _arg134 = data.readInt();
                            String _arg217 = data.readString();
                            data.enforceNoDataAvail();
                            int _result46 = setUsageSetting(_arg046, _arg134, _arg217);
                            reply.writeNoException();
                            reply.writeInt(_result46);
                            break;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
                    return true;
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        /* loaded from: classes4.dex */
        public static class Proxy implements ISub {
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

            @Override // com.android.internal.telephony.ISub
            public List<SubscriptionInfo> getAllSubInfoList(String callingPackage, String callingFeatureId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    _data.writeString(callingFeatureId);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                    List<SubscriptionInfo> _result = _reply.createTypedArrayList(SubscriptionInfo.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ISub
            public int getAllSubInfoCount(String callingPackage, String callingFeatureId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    _data.writeString(callingFeatureId);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ISub
            public SubscriptionInfo getActiveSubscriptionInfo(int subId, String callingPackage, String callingFeatureId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(callingPackage);
                    _data.writeString(callingFeatureId);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                    SubscriptionInfo _result = (SubscriptionInfo) _reply.readTypedObject(SubscriptionInfo.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ISub
            public SubscriptionInfo getActiveSubscriptionInfoForIccId(String iccId, String callingPackage, String callingFeatureId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(iccId);
                    _data.writeString(callingPackage);
                    _data.writeString(callingFeatureId);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                    SubscriptionInfo _result = (SubscriptionInfo) _reply.readTypedObject(SubscriptionInfo.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ISub
            public SubscriptionInfo getActiveSubscriptionInfoForSimSlotIndex(int slotIndex, String callingPackage, String callingFeatureId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(slotIndex);
                    _data.writeString(callingPackage);
                    _data.writeString(callingFeatureId);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                    SubscriptionInfo _result = (SubscriptionInfo) _reply.readTypedObject(SubscriptionInfo.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ISub
            public List<SubscriptionInfo> getActiveSubscriptionInfoList(String callingPackage, String callingFeatureId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    _data.writeString(callingFeatureId);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                    List<SubscriptionInfo> _result = _reply.createTypedArrayList(SubscriptionInfo.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ISub
            public int getActiveSubInfoCount(String callingPackage, String callingFeatureId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    _data.writeString(callingFeatureId);
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ISub
            public int getActiveSubInfoCountMax() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ISub
            public List<SubscriptionInfo> getAvailableSubscriptionInfoList(String callingPackage, String callingFeatureId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    _data.writeString(callingFeatureId);
                    this.mRemote.transact(9, _data, _reply, 0);
                    _reply.readException();
                    List<SubscriptionInfo> _result = _reply.createTypedArrayList(SubscriptionInfo.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ISub
            public List<SubscriptionInfo> getAccessibleSubscriptionInfoList(String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(10, _data, _reply, 0);
                    _reply.readException();
                    List<SubscriptionInfo> _result = _reply.createTypedArrayList(SubscriptionInfo.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ISub
            public void requestEmbeddedSubscriptionInfoListRefresh(int cardId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(cardId);
                    this.mRemote.transact(11, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ISub
            public int addSubInfoRecord(String iccId, int slotIndex) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(iccId);
                    _data.writeInt(slotIndex);
                    this.mRemote.transact(12, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ISub
            public int addSubInfo(String uniqueId, String displayName, int slotIndex, int subscriptionType) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(uniqueId);
                    _data.writeString(displayName);
                    _data.writeInt(slotIndex);
                    _data.writeInt(subscriptionType);
                    this.mRemote.transact(13, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ISub
            public int removeSubInfo(String uniqueId, int subscriptionType) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(uniqueId);
                    _data.writeInt(subscriptionType);
                    this.mRemote.transact(14, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ISub
            public int setIconTint(int tint, int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(tint);
                    _data.writeInt(subId);
                    this.mRemote.transact(15, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ISub
            public int setDisplayNameUsingSrc(String displayName, int subId, int nameSource) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(displayName);
                    _data.writeInt(subId);
                    _data.writeInt(nameSource);
                    this.mRemote.transact(16, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ISub
            public int setDisplayNumber(String number, int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(number);
                    _data.writeInt(subId);
                    this.mRemote.transact(17, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ISub
            public int setDataRoaming(int roaming, int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(roaming);
                    _data.writeInt(subId);
                    this.mRemote.transact(18, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ISub
            public int setOpportunistic(boolean opportunistic, int subId, String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeBoolean(opportunistic);
                    _data.writeInt(subId);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(19, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ISub
            public ParcelUuid createSubscriptionGroup(int[] subIdList, String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeIntArray(subIdList);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(20, _data, _reply, 0);
                    _reply.readException();
                    ParcelUuid _result = (ParcelUuid) _reply.readTypedObject(ParcelUuid.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ISub
            public void setPreferredDataSubscriptionId(int subId, boolean needValidation, ISetOpportunisticDataCallback callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeBoolean(needValidation);
                    _data.writeStrongInterface(callback);
                    this.mRemote.transact(21, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ISub
            public int getPreferredDataSubscriptionId() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(22, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ISub
            public List<SubscriptionInfo> getOpportunisticSubscriptions(String callingPackage, String callingFeatureId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    _data.writeString(callingFeatureId);
                    this.mRemote.transact(23, _data, _reply, 0);
                    _reply.readException();
                    List<SubscriptionInfo> _result = _reply.createTypedArrayList(SubscriptionInfo.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ISub
            public void removeSubscriptionsFromGroup(int[] subIdList, ParcelUuid groupUuid, String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeIntArray(subIdList);
                    _data.writeTypedObject(groupUuid, 0);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(24, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ISub
            public void addSubscriptionsIntoGroup(int[] subIdList, ParcelUuid groupUuid, String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeIntArray(subIdList);
                    _data.writeTypedObject(groupUuid, 0);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(25, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ISub
            public List<SubscriptionInfo> getSubscriptionsInGroup(ParcelUuid groupUuid, String callingPackage, String callingFeatureId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(groupUuid, 0);
                    _data.writeString(callingPackage);
                    _data.writeString(callingFeatureId);
                    this.mRemote.transact(26, _data, _reply, 0);
                    _reply.readException();
                    List<SubscriptionInfo> _result = _reply.createTypedArrayList(SubscriptionInfo.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ISub
            public int getSlotIndex(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(27, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ISub
            public int[] getSubId(int slotIndex) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(slotIndex);
                    this.mRemote.transact(28, _data, _reply, 0);
                    _reply.readException();
                    int[] _result = _reply.createIntArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ISub
            public int getDefaultSubId() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(29, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ISub
            public int clearSubInfo() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(30, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ISub
            public int getPhoneId(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(31, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ISub
            public int getDefaultDataSubId() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(32, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ISub
            public void setDefaultDataSubId(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(33, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ISub
            public void setDefaultDataSubIdWithReason(int subId, int cause) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeInt(cause);
                    this.mRemote.transact(34, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ISub
            public int getDefaultVoiceSubId() throws RemoteException {
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

            @Override // com.android.internal.telephony.ISub
            public void setDefaultVoiceSubId(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(36, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ISub
            public int getDefaultSmsSubId() throws RemoteException {
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

            @Override // com.android.internal.telephony.ISub
            public void setDefaultSmsSubId(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(38, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ISub
            public int[] getActiveSubIdList(boolean visibleOnly) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeBoolean(visibleOnly);
                    this.mRemote.transact(39, _data, _reply, 0);
                    _reply.readException();
                    int[] _result = _reply.createIntArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ISub
            public int setSubscriptionProperty(int subId, String propKey, String propValue) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(propKey);
                    _data.writeString(propValue);
                    this.mRemote.transact(40, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ISub
            public String getSubscriptionProperty(int subId, String propKey, String callingPackage, String callingFeatureId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(propKey);
                    _data.writeString(callingPackage);
                    _data.writeString(callingFeatureId);
                    this.mRemote.transact(41, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ISub
            public boolean setSubscriptionEnabled(boolean enable, int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeBoolean(enable);
                    _data.writeInt(subId);
                    this.mRemote.transact(42, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ISub
            public boolean isSubscriptionEnabled(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(43, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ISub
            public int getEnabledSubscriptionId(int slotIndex) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(slotIndex);
                    this.mRemote.transact(44, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ISub
            public int getSimStateForSlotIndex(int slotIndex) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(slotIndex);
                    this.mRemote.transact(45, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ISub
            public boolean isActiveSubId(int subId, String callingPackage, String callingFeatureId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(callingPackage);
                    _data.writeString(callingFeatureId);
                    this.mRemote.transact(46, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ISub
            public int getActiveDataSubscriptionId() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(47, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ISub
            public boolean canDisablePhysicalSubscription() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(48, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ISub
            public int setUiccApplicationsEnabled(boolean enabled, int subscriptionId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeBoolean(enabled);
                    _data.writeInt(subscriptionId);
                    this.mRemote.transact(49, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ISub
            public int setDeviceToDeviceStatusSharing(int sharing, int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(sharing);
                    _data.writeInt(subId);
                    this.mRemote.transact(50, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ISub
            public int setDeviceToDeviceStatusSharingContacts(String contacts, int subscriptionId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(contacts);
                    _data.writeInt(subscriptionId);
                    this.mRemote.transact(51, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ISub
            public String getPhoneNumber(int subId, int source, String callingPackage, String callingFeatureId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeInt(source);
                    _data.writeString(callingPackage);
                    _data.writeString(callingFeatureId);
                    this.mRemote.transact(52, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ISub
            public String getPhoneNumberFromFirstAvailableSource(int subId, String callingPackage, String callingFeatureId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(callingPackage);
                    _data.writeString(callingFeatureId);
                    this.mRemote.transact(53, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ISub
            public void setPhoneNumber(int subId, int source, String number, String callingPackage, String callingFeatureId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeInt(source);
                    _data.writeString(number);
                    _data.writeString(callingPackage);
                    _data.writeString(callingFeatureId);
                    this.mRemote.transact(54, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.ISub
            public int setUsageSetting(int usageSetting, int subId, String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(usageSetting);
                    _data.writeInt(subId);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(55, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }

        @Override // android.os.Binder
        public int getMaxTransactionId() {
            return 54;
        }
    }
}
