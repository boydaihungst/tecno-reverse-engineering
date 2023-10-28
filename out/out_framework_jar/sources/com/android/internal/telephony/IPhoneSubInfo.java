package com.android.internal.telephony;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.telephony.ImsiEncryptionInfo;
/* loaded from: classes4.dex */
public interface IPhoneSubInfo extends IInterface {
    ImsiEncryptionInfo getCarrierInfoForImsiEncryption(int i, int i2, String str) throws RemoteException;

    @Deprecated
    String getDeviceId(String str) throws RemoteException;

    String getDeviceIdForPhone(int i, String str, String str2) throws RemoteException;

    String getDeviceIdWithFeature(String str, String str2) throws RemoteException;

    String getDeviceSvn(String str, String str2) throws RemoteException;

    String getDeviceSvnUsingSubId(int i, String str, String str2) throws RemoteException;

    String getGroupIdLevel1ForSubscriber(int i, String str, String str2) throws RemoteException;

    String getIccSerialNumber(String str) throws RemoteException;

    String getIccSerialNumberForSubscriber(int i, String str, String str2) throws RemoteException;

    String getIccSerialNumberWithFeature(String str, String str2) throws RemoteException;

    String getIccSimChallengeResponse(int i, int i2, int i3, String str, String str2, String str3) throws RemoteException;

    String getImeiForSubscriber(int i, String str, String str2) throws RemoteException;

    String getIsimDomain(int i) throws RemoteException;

    String getIsimImpi(int i) throws RemoteException;

    String[] getIsimImpu(int i) throws RemoteException;

    String getIsimIst(int i) throws RemoteException;

    String[] getIsimPcscf(int i) throws RemoteException;

    String getLine1AlphaTag(String str, String str2) throws RemoteException;

    String getLine1AlphaTagForSubscriber(int i, String str, String str2) throws RemoteException;

    String getLine1Number(String str, String str2) throws RemoteException;

    String getLine1NumberForSubscriber(int i, String str, String str2) throws RemoteException;

    String getMsisdn(String str, String str2) throws RemoteException;

    String getMsisdnForSubscriber(int i, String str, String str2) throws RemoteException;

    String getNaiForSubscriber(int i, String str, String str2) throws RemoteException;

    @Deprecated
    String getSubscriberId(String str) throws RemoteException;

    String getSubscriberIdForSubscriber(int i, String str, String str2) throws RemoteException;

    String getSubscriberIdWithFeature(String str, String str2) throws RemoteException;

    String getVoiceMailAlphaTag(String str, String str2) throws RemoteException;

    String getVoiceMailAlphaTagForSubscriber(int i, String str, String str2) throws RemoteException;

    String getVoiceMailNumber(String str, String str2) throws RemoteException;

    String getVoiceMailNumberForSubscriber(int i, String str, String str2) throws RemoteException;

    void resetCarrierKeysForImsiEncryption(int i, String str) throws RemoteException;

    void setCarrierInfoForImsiEncryption(int i, String str, ImsiEncryptionInfo imsiEncryptionInfo) throws RemoteException;

    /* loaded from: classes4.dex */
    public static class Default implements IPhoneSubInfo {
        @Override // com.android.internal.telephony.IPhoneSubInfo
        public String getDeviceId(String callingPackage) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.telephony.IPhoneSubInfo
        public String getDeviceIdWithFeature(String callingPackage, String callingFeatureId) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.telephony.IPhoneSubInfo
        public String getNaiForSubscriber(int subId, String callingPackage, String callingFeatureId) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.telephony.IPhoneSubInfo
        public String getDeviceIdForPhone(int phoneId, String callingPackage, String callingFeatureId) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.telephony.IPhoneSubInfo
        public String getImeiForSubscriber(int subId, String callingPackage, String callingFeatureId) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.telephony.IPhoneSubInfo
        public String getDeviceSvn(String callingPackage, String callingFeatureId) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.telephony.IPhoneSubInfo
        public String getDeviceSvnUsingSubId(int subId, String callingPackage, String callingFeatureId) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.telephony.IPhoneSubInfo
        public String getSubscriberId(String callingPackage) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.telephony.IPhoneSubInfo
        public String getSubscriberIdWithFeature(String callingPackage, String callingComponenId) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.telephony.IPhoneSubInfo
        public String getSubscriberIdForSubscriber(int subId, String callingPackage, String callingFeatureId) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.telephony.IPhoneSubInfo
        public String getGroupIdLevel1ForSubscriber(int subId, String callingPackage, String callingFeatureId) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.telephony.IPhoneSubInfo
        public String getIccSerialNumber(String callingPackage) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.telephony.IPhoneSubInfo
        public String getIccSerialNumberWithFeature(String callingPackage, String callingFeatureId) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.telephony.IPhoneSubInfo
        public String getIccSerialNumberForSubscriber(int subId, String callingPackage, String callingFeatureId) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.telephony.IPhoneSubInfo
        public String getLine1Number(String callingPackage, String callingFeatureId) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.telephony.IPhoneSubInfo
        public String getLine1NumberForSubscriber(int subId, String callingPackage, String callingFeatureId) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.telephony.IPhoneSubInfo
        public String getLine1AlphaTag(String callingPackage, String callingFeatureId) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.telephony.IPhoneSubInfo
        public String getLine1AlphaTagForSubscriber(int subId, String callingPackage, String callingFeatureId) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.telephony.IPhoneSubInfo
        public String getMsisdn(String callingPackage, String callingFeatureId) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.telephony.IPhoneSubInfo
        public String getMsisdnForSubscriber(int subId, String callingPackage, String callingFeatureId) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.telephony.IPhoneSubInfo
        public String getVoiceMailNumber(String callingPackage, String callingFeatureId) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.telephony.IPhoneSubInfo
        public String getVoiceMailNumberForSubscriber(int subId, String callingPackage, String callingFeatureId) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.telephony.IPhoneSubInfo
        public ImsiEncryptionInfo getCarrierInfoForImsiEncryption(int subId, int keyType, String callingPackage) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.telephony.IPhoneSubInfo
        public void setCarrierInfoForImsiEncryption(int subId, String callingPackage, ImsiEncryptionInfo imsiEncryptionInfo) throws RemoteException {
        }

        @Override // com.android.internal.telephony.IPhoneSubInfo
        public void resetCarrierKeysForImsiEncryption(int subId, String callingPackage) throws RemoteException {
        }

        @Override // com.android.internal.telephony.IPhoneSubInfo
        public String getVoiceMailAlphaTag(String callingPackage, String callingFeatureId) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.telephony.IPhoneSubInfo
        public String getVoiceMailAlphaTagForSubscriber(int subId, String callingPackage, String callingFeatureId) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.telephony.IPhoneSubInfo
        public String getIsimImpi(int subId) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.telephony.IPhoneSubInfo
        public String getIsimDomain(int subId) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.telephony.IPhoneSubInfo
        public String[] getIsimImpu(int subId) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.telephony.IPhoneSubInfo
        public String getIsimIst(int subId) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.telephony.IPhoneSubInfo
        public String[] getIsimPcscf(int subId) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.telephony.IPhoneSubInfo
        public String getIccSimChallengeResponse(int subId, int appType, int authType, String data, String callingPackage, String callingFeatureId) throws RemoteException {
            return null;
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes4.dex */
    public static abstract class Stub extends Binder implements IPhoneSubInfo {
        public static final String DESCRIPTOR = "com.android.internal.telephony.IPhoneSubInfo";
        static final int TRANSACTION_getCarrierInfoForImsiEncryption = 23;
        static final int TRANSACTION_getDeviceId = 1;
        static final int TRANSACTION_getDeviceIdForPhone = 4;
        static final int TRANSACTION_getDeviceIdWithFeature = 2;
        static final int TRANSACTION_getDeviceSvn = 6;
        static final int TRANSACTION_getDeviceSvnUsingSubId = 7;
        static final int TRANSACTION_getGroupIdLevel1ForSubscriber = 11;
        static final int TRANSACTION_getIccSerialNumber = 12;
        static final int TRANSACTION_getIccSerialNumberForSubscriber = 14;
        static final int TRANSACTION_getIccSerialNumberWithFeature = 13;
        static final int TRANSACTION_getIccSimChallengeResponse = 33;
        static final int TRANSACTION_getImeiForSubscriber = 5;
        static final int TRANSACTION_getIsimDomain = 29;
        static final int TRANSACTION_getIsimImpi = 28;
        static final int TRANSACTION_getIsimImpu = 30;
        static final int TRANSACTION_getIsimIst = 31;
        static final int TRANSACTION_getIsimPcscf = 32;
        static final int TRANSACTION_getLine1AlphaTag = 17;
        static final int TRANSACTION_getLine1AlphaTagForSubscriber = 18;
        static final int TRANSACTION_getLine1Number = 15;
        static final int TRANSACTION_getLine1NumberForSubscriber = 16;
        static final int TRANSACTION_getMsisdn = 19;
        static final int TRANSACTION_getMsisdnForSubscriber = 20;
        static final int TRANSACTION_getNaiForSubscriber = 3;
        static final int TRANSACTION_getSubscriberId = 8;
        static final int TRANSACTION_getSubscriberIdForSubscriber = 10;
        static final int TRANSACTION_getSubscriberIdWithFeature = 9;
        static final int TRANSACTION_getVoiceMailAlphaTag = 26;
        static final int TRANSACTION_getVoiceMailAlphaTagForSubscriber = 27;
        static final int TRANSACTION_getVoiceMailNumber = 21;
        static final int TRANSACTION_getVoiceMailNumberForSubscriber = 22;
        static final int TRANSACTION_resetCarrierKeysForImsiEncryption = 25;
        static final int TRANSACTION_setCarrierInfoForImsiEncryption = 24;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IPhoneSubInfo asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IPhoneSubInfo)) {
                return (IPhoneSubInfo) iin;
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
                    return "getDeviceId";
                case 2:
                    return "getDeviceIdWithFeature";
                case 3:
                    return "getNaiForSubscriber";
                case 4:
                    return "getDeviceIdForPhone";
                case 5:
                    return "getImeiForSubscriber";
                case 6:
                    return "getDeviceSvn";
                case 7:
                    return "getDeviceSvnUsingSubId";
                case 8:
                    return "getSubscriberId";
                case 9:
                    return "getSubscriberIdWithFeature";
                case 10:
                    return "getSubscriberIdForSubscriber";
                case 11:
                    return "getGroupIdLevel1ForSubscriber";
                case 12:
                    return "getIccSerialNumber";
                case 13:
                    return "getIccSerialNumberWithFeature";
                case 14:
                    return "getIccSerialNumberForSubscriber";
                case 15:
                    return "getLine1Number";
                case 16:
                    return "getLine1NumberForSubscriber";
                case 17:
                    return "getLine1AlphaTag";
                case 18:
                    return "getLine1AlphaTagForSubscriber";
                case 19:
                    return "getMsisdn";
                case 20:
                    return "getMsisdnForSubscriber";
                case 21:
                    return "getVoiceMailNumber";
                case 22:
                    return "getVoiceMailNumberForSubscriber";
                case 23:
                    return "getCarrierInfoForImsiEncryption";
                case 24:
                    return "setCarrierInfoForImsiEncryption";
                case 25:
                    return "resetCarrierKeysForImsiEncryption";
                case 26:
                    return "getVoiceMailAlphaTag";
                case 27:
                    return "getVoiceMailAlphaTagForSubscriber";
                case 28:
                    return "getIsimImpi";
                case 29:
                    return "getIsimDomain";
                case 30:
                    return "getIsimImpu";
                case 31:
                    return "getIsimIst";
                case 32:
                    return "getIsimPcscf";
                case 33:
                    return "getIccSimChallengeResponse";
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
                            data.enforceNoDataAvail();
                            String _result = getDeviceId(_arg0);
                            reply.writeNoException();
                            reply.writeString(_result);
                            break;
                        case 2:
                            String _arg02 = data.readString();
                            String _arg1 = data.readString();
                            data.enforceNoDataAvail();
                            String _result2 = getDeviceIdWithFeature(_arg02, _arg1);
                            reply.writeNoException();
                            reply.writeString(_result2);
                            break;
                        case 3:
                            int _arg03 = data.readInt();
                            String _arg12 = data.readString();
                            String _arg2 = data.readString();
                            data.enforceNoDataAvail();
                            String _result3 = getNaiForSubscriber(_arg03, _arg12, _arg2);
                            reply.writeNoException();
                            reply.writeString(_result3);
                            break;
                        case 4:
                            int _arg04 = data.readInt();
                            String _arg13 = data.readString();
                            String _arg22 = data.readString();
                            data.enforceNoDataAvail();
                            String _result4 = getDeviceIdForPhone(_arg04, _arg13, _arg22);
                            reply.writeNoException();
                            reply.writeString(_result4);
                            break;
                        case 5:
                            int _arg05 = data.readInt();
                            String _arg14 = data.readString();
                            String _arg23 = data.readString();
                            data.enforceNoDataAvail();
                            String _result5 = getImeiForSubscriber(_arg05, _arg14, _arg23);
                            reply.writeNoException();
                            reply.writeString(_result5);
                            break;
                        case 6:
                            String _arg06 = data.readString();
                            String _arg15 = data.readString();
                            data.enforceNoDataAvail();
                            String _result6 = getDeviceSvn(_arg06, _arg15);
                            reply.writeNoException();
                            reply.writeString(_result6);
                            break;
                        case 7:
                            int _arg07 = data.readInt();
                            String _arg16 = data.readString();
                            String _arg24 = data.readString();
                            data.enforceNoDataAvail();
                            String _result7 = getDeviceSvnUsingSubId(_arg07, _arg16, _arg24);
                            reply.writeNoException();
                            reply.writeString(_result7);
                            break;
                        case 8:
                            String _arg08 = data.readString();
                            data.enforceNoDataAvail();
                            String _result8 = getSubscriberId(_arg08);
                            reply.writeNoException();
                            reply.writeString(_result8);
                            break;
                        case 9:
                            String _arg09 = data.readString();
                            String _arg17 = data.readString();
                            data.enforceNoDataAvail();
                            String _result9 = getSubscriberIdWithFeature(_arg09, _arg17);
                            reply.writeNoException();
                            reply.writeString(_result9);
                            break;
                        case 10:
                            int _arg010 = data.readInt();
                            String _arg18 = data.readString();
                            String _arg25 = data.readString();
                            data.enforceNoDataAvail();
                            String _result10 = getSubscriberIdForSubscriber(_arg010, _arg18, _arg25);
                            reply.writeNoException();
                            reply.writeString(_result10);
                            break;
                        case 11:
                            int _arg011 = data.readInt();
                            String _arg19 = data.readString();
                            String _arg26 = data.readString();
                            data.enforceNoDataAvail();
                            String _result11 = getGroupIdLevel1ForSubscriber(_arg011, _arg19, _arg26);
                            reply.writeNoException();
                            reply.writeString(_result11);
                            break;
                        case 12:
                            String _arg012 = data.readString();
                            data.enforceNoDataAvail();
                            String _result12 = getIccSerialNumber(_arg012);
                            reply.writeNoException();
                            reply.writeString(_result12);
                            break;
                        case 13:
                            String _arg013 = data.readString();
                            String _arg110 = data.readString();
                            data.enforceNoDataAvail();
                            String _result13 = getIccSerialNumberWithFeature(_arg013, _arg110);
                            reply.writeNoException();
                            reply.writeString(_result13);
                            break;
                        case 14:
                            int _arg014 = data.readInt();
                            String _arg111 = data.readString();
                            String _arg27 = data.readString();
                            data.enforceNoDataAvail();
                            String _result14 = getIccSerialNumberForSubscriber(_arg014, _arg111, _arg27);
                            reply.writeNoException();
                            reply.writeString(_result14);
                            break;
                        case 15:
                            String _arg015 = data.readString();
                            String _arg112 = data.readString();
                            data.enforceNoDataAvail();
                            String _result15 = getLine1Number(_arg015, _arg112);
                            reply.writeNoException();
                            reply.writeString(_result15);
                            break;
                        case 16:
                            int _arg016 = data.readInt();
                            String _arg113 = data.readString();
                            String _arg28 = data.readString();
                            data.enforceNoDataAvail();
                            String _result16 = getLine1NumberForSubscriber(_arg016, _arg113, _arg28);
                            reply.writeNoException();
                            reply.writeString(_result16);
                            break;
                        case 17:
                            String _arg017 = data.readString();
                            String _arg114 = data.readString();
                            data.enforceNoDataAvail();
                            String _result17 = getLine1AlphaTag(_arg017, _arg114);
                            reply.writeNoException();
                            reply.writeString(_result17);
                            break;
                        case 18:
                            int _arg018 = data.readInt();
                            String _arg115 = data.readString();
                            String _arg29 = data.readString();
                            data.enforceNoDataAvail();
                            String _result18 = getLine1AlphaTagForSubscriber(_arg018, _arg115, _arg29);
                            reply.writeNoException();
                            reply.writeString(_result18);
                            break;
                        case 19:
                            String _arg019 = data.readString();
                            String _arg116 = data.readString();
                            data.enforceNoDataAvail();
                            String _result19 = getMsisdn(_arg019, _arg116);
                            reply.writeNoException();
                            reply.writeString(_result19);
                            break;
                        case 20:
                            int _arg020 = data.readInt();
                            String _arg117 = data.readString();
                            String _arg210 = data.readString();
                            data.enforceNoDataAvail();
                            String _result20 = getMsisdnForSubscriber(_arg020, _arg117, _arg210);
                            reply.writeNoException();
                            reply.writeString(_result20);
                            break;
                        case 21:
                            String _arg021 = data.readString();
                            String _arg118 = data.readString();
                            data.enforceNoDataAvail();
                            String _result21 = getVoiceMailNumber(_arg021, _arg118);
                            reply.writeNoException();
                            reply.writeString(_result21);
                            break;
                        case 22:
                            int _arg022 = data.readInt();
                            String _arg119 = data.readString();
                            String _arg211 = data.readString();
                            data.enforceNoDataAvail();
                            String _result22 = getVoiceMailNumberForSubscriber(_arg022, _arg119, _arg211);
                            reply.writeNoException();
                            reply.writeString(_result22);
                            break;
                        case 23:
                            int _arg023 = data.readInt();
                            int _arg120 = data.readInt();
                            String _arg212 = data.readString();
                            data.enforceNoDataAvail();
                            ImsiEncryptionInfo _result23 = getCarrierInfoForImsiEncryption(_arg023, _arg120, _arg212);
                            reply.writeNoException();
                            reply.writeTypedObject(_result23, 1);
                            break;
                        case 24:
                            int _arg024 = data.readInt();
                            String _arg121 = data.readString();
                            ImsiEncryptionInfo _arg213 = (ImsiEncryptionInfo) data.readTypedObject(ImsiEncryptionInfo.CREATOR);
                            data.enforceNoDataAvail();
                            setCarrierInfoForImsiEncryption(_arg024, _arg121, _arg213);
                            reply.writeNoException();
                            break;
                        case 25:
                            int _arg025 = data.readInt();
                            String _arg122 = data.readString();
                            data.enforceNoDataAvail();
                            resetCarrierKeysForImsiEncryption(_arg025, _arg122);
                            reply.writeNoException();
                            break;
                        case 26:
                            String _arg026 = data.readString();
                            String _arg123 = data.readString();
                            data.enforceNoDataAvail();
                            String _result24 = getVoiceMailAlphaTag(_arg026, _arg123);
                            reply.writeNoException();
                            reply.writeString(_result24);
                            break;
                        case 27:
                            int _arg027 = data.readInt();
                            String _arg124 = data.readString();
                            String _arg214 = data.readString();
                            data.enforceNoDataAvail();
                            String _result25 = getVoiceMailAlphaTagForSubscriber(_arg027, _arg124, _arg214);
                            reply.writeNoException();
                            reply.writeString(_result25);
                            break;
                        case 28:
                            int _arg028 = data.readInt();
                            data.enforceNoDataAvail();
                            String _result26 = getIsimImpi(_arg028);
                            reply.writeNoException();
                            reply.writeString(_result26);
                            break;
                        case 29:
                            int _arg029 = data.readInt();
                            data.enforceNoDataAvail();
                            String _result27 = getIsimDomain(_arg029);
                            reply.writeNoException();
                            reply.writeString(_result27);
                            break;
                        case 30:
                            int _arg030 = data.readInt();
                            data.enforceNoDataAvail();
                            String[] _result28 = getIsimImpu(_arg030);
                            reply.writeNoException();
                            reply.writeStringArray(_result28);
                            break;
                        case 31:
                            int _arg031 = data.readInt();
                            data.enforceNoDataAvail();
                            String _result29 = getIsimIst(_arg031);
                            reply.writeNoException();
                            reply.writeString(_result29);
                            break;
                        case 32:
                            int _arg032 = data.readInt();
                            data.enforceNoDataAvail();
                            String[] _result30 = getIsimPcscf(_arg032);
                            reply.writeNoException();
                            reply.writeStringArray(_result30);
                            break;
                        case 33:
                            int _arg033 = data.readInt();
                            int _arg125 = data.readInt();
                            int _arg215 = data.readInt();
                            String _arg3 = data.readString();
                            String _arg4 = data.readString();
                            String _arg5 = data.readString();
                            data.enforceNoDataAvail();
                            String _result31 = getIccSimChallengeResponse(_arg033, _arg125, _arg215, _arg3, _arg4, _arg5);
                            reply.writeNoException();
                            reply.writeString(_result31);
                            break;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
                    return true;
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        /* loaded from: classes4.dex */
        public static class Proxy implements IPhoneSubInfo {
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

            @Override // com.android.internal.telephony.IPhoneSubInfo
            public String getDeviceId(String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.IPhoneSubInfo
            public String getDeviceIdWithFeature(String callingPackage, String callingFeatureId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    _data.writeString(callingFeatureId);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.IPhoneSubInfo
            public String getNaiForSubscriber(int subId, String callingPackage, String callingFeatureId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(callingPackage);
                    _data.writeString(callingFeatureId);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.IPhoneSubInfo
            public String getDeviceIdForPhone(int phoneId, String callingPackage, String callingFeatureId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(phoneId);
                    _data.writeString(callingPackage);
                    _data.writeString(callingFeatureId);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.IPhoneSubInfo
            public String getImeiForSubscriber(int subId, String callingPackage, String callingFeatureId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(callingPackage);
                    _data.writeString(callingFeatureId);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.IPhoneSubInfo
            public String getDeviceSvn(String callingPackage, String callingFeatureId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    _data.writeString(callingFeatureId);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.IPhoneSubInfo
            public String getDeviceSvnUsingSubId(int subId, String callingPackage, String callingFeatureId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(callingPackage);
                    _data.writeString(callingFeatureId);
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.IPhoneSubInfo
            public String getSubscriberId(String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.IPhoneSubInfo
            public String getSubscriberIdWithFeature(String callingPackage, String callingComponenId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    _data.writeString(callingComponenId);
                    this.mRemote.transact(9, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.IPhoneSubInfo
            public String getSubscriberIdForSubscriber(int subId, String callingPackage, String callingFeatureId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(callingPackage);
                    _data.writeString(callingFeatureId);
                    this.mRemote.transact(10, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.IPhoneSubInfo
            public String getGroupIdLevel1ForSubscriber(int subId, String callingPackage, String callingFeatureId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(callingPackage);
                    _data.writeString(callingFeatureId);
                    this.mRemote.transact(11, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.IPhoneSubInfo
            public String getIccSerialNumber(String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(12, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.IPhoneSubInfo
            public String getIccSerialNumberWithFeature(String callingPackage, String callingFeatureId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    _data.writeString(callingFeatureId);
                    this.mRemote.transact(13, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.IPhoneSubInfo
            public String getIccSerialNumberForSubscriber(int subId, String callingPackage, String callingFeatureId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(callingPackage);
                    _data.writeString(callingFeatureId);
                    this.mRemote.transact(14, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.IPhoneSubInfo
            public String getLine1Number(String callingPackage, String callingFeatureId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    _data.writeString(callingFeatureId);
                    this.mRemote.transact(15, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.IPhoneSubInfo
            public String getLine1NumberForSubscriber(int subId, String callingPackage, String callingFeatureId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(callingPackage);
                    _data.writeString(callingFeatureId);
                    this.mRemote.transact(16, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.IPhoneSubInfo
            public String getLine1AlphaTag(String callingPackage, String callingFeatureId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    _data.writeString(callingFeatureId);
                    this.mRemote.transact(17, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.IPhoneSubInfo
            public String getLine1AlphaTagForSubscriber(int subId, String callingPackage, String callingFeatureId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(callingPackage);
                    _data.writeString(callingFeatureId);
                    this.mRemote.transact(18, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.IPhoneSubInfo
            public String getMsisdn(String callingPackage, String callingFeatureId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    _data.writeString(callingFeatureId);
                    this.mRemote.transact(19, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.IPhoneSubInfo
            public String getMsisdnForSubscriber(int subId, String callingPackage, String callingFeatureId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(callingPackage);
                    _data.writeString(callingFeatureId);
                    this.mRemote.transact(20, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.IPhoneSubInfo
            public String getVoiceMailNumber(String callingPackage, String callingFeatureId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    _data.writeString(callingFeatureId);
                    this.mRemote.transact(21, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.IPhoneSubInfo
            public String getVoiceMailNumberForSubscriber(int subId, String callingPackage, String callingFeatureId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(callingPackage);
                    _data.writeString(callingFeatureId);
                    this.mRemote.transact(22, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.IPhoneSubInfo
            public ImsiEncryptionInfo getCarrierInfoForImsiEncryption(int subId, int keyType, String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeInt(keyType);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(23, _data, _reply, 0);
                    _reply.readException();
                    ImsiEncryptionInfo _result = (ImsiEncryptionInfo) _reply.readTypedObject(ImsiEncryptionInfo.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.IPhoneSubInfo
            public void setCarrierInfoForImsiEncryption(int subId, String callingPackage, ImsiEncryptionInfo imsiEncryptionInfo) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(callingPackage);
                    _data.writeTypedObject(imsiEncryptionInfo, 0);
                    this.mRemote.transact(24, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.IPhoneSubInfo
            public void resetCarrierKeysForImsiEncryption(int subId, String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(25, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.IPhoneSubInfo
            public String getVoiceMailAlphaTag(String callingPackage, String callingFeatureId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    _data.writeString(callingFeatureId);
                    this.mRemote.transact(26, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.IPhoneSubInfo
            public String getVoiceMailAlphaTagForSubscriber(int subId, String callingPackage, String callingFeatureId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(callingPackage);
                    _data.writeString(callingFeatureId);
                    this.mRemote.transact(27, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.IPhoneSubInfo
            public String getIsimImpi(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(28, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.IPhoneSubInfo
            public String getIsimDomain(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(29, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.IPhoneSubInfo
            public String[] getIsimImpu(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(30, _data, _reply, 0);
                    _reply.readException();
                    String[] _result = _reply.createStringArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.IPhoneSubInfo
            public String getIsimIst(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(31, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.IPhoneSubInfo
            public String[] getIsimPcscf(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(32, _data, _reply, 0);
                    _reply.readException();
                    String[] _result = _reply.createStringArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telephony.IPhoneSubInfo
            public String getIccSimChallengeResponse(int subId, int appType, int authType, String data, String callingPackage, String callingFeatureId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeInt(appType);
                    _data.writeInt(authType);
                    _data.writeString(data);
                    _data.writeString(callingPackage);
                    _data.writeString(callingFeatureId);
                    this.mRemote.transact(33, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }

        @Override // android.os.Binder
        public int getMaxTransactionId() {
            return 32;
        }
    }
}
