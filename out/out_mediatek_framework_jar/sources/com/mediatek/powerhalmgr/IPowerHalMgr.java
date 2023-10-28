package com.mediatek.powerhalmgr;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.IRemoteCallback;
import android.os.Parcel;
import android.os.RemoteException;
/* loaded from: classes.dex */
public interface IPowerHalMgr extends IInterface {
    public static final String DESCRIPTOR = "com.mediatek.powerhalmgr.IPowerHalMgr";

    void UpdateManagementPkt(int i, String str) throws RemoteException;

    boolean configBoosterInfo(BoosterInfo boosterInfo) throws RemoteException;

    boolean flushPriorityRules(int i) throws RemoteException;

    void getCpuCap() throws RemoteException;

    void getCpuRTInfo() throws RemoteException;

    void getGpuCap() throws RemoteException;

    void getGpuRTInfo() throws RemoteException;

    boolean isDupPacketPredictionStarted() throws RemoteException;

    void mtkCusPowerHint(int i, int i2) throws RemoteException;

    void mtkPowerHint(int i, int i2) throws RemoteException;

    int perfCusLockHint(int i, int i2) throws RemoteException;

    int perfLockAcquire(int i, int i2, int[] iArr) throws RemoteException;

    void perfLockRelease(int i) throws RemoteException;

    int querySysInfo(int i, int i2) throws RemoteException;

    boolean registerDuplicatePacketPredictionEvent(IRemoteCallback iRemoteCallback) throws RemoteException;

    void scnConfig(int i, int i2, int i3, int i4, int i5, int i6) throws RemoteException;

    void scnDisable(int i) throws RemoteException;

    void scnEnable(int i, int i2) throws RemoteException;

    int scnReg() throws RemoteException;

    void scnUltraCfg(int i, int i2, int i3, int i4, int i5, int i6) throws RemoteException;

    void scnUnreg(int i) throws RemoteException;

    void setForegroundSports() throws RemoteException;

    void setPredictInfo(String str, int i) throws RemoteException;

    boolean setPriorityByLinkinfo(int i, DupLinkInfo dupLinkInfo) throws RemoteException;

    boolean setPriorityByUid(int i, int i2) throws RemoteException;

    void setSysInfo(int i, String str) throws RemoteException;

    int setSysInfoSync(int i, String str) throws RemoteException;

    boolean startDuplicatePacketPrediction() throws RemoteException;

    boolean stopDuplicatePacketPrediction() throws RemoteException;

    boolean unregisterDuplicatePacketPredictionEvent(IRemoteCallback iRemoteCallback) throws RemoteException;

    boolean updateMultiDuplicatePacketLink(DupLinkInfo[] dupLinkInfoArr) throws RemoteException;

    /* loaded from: classes.dex */
    public static class Default implements IPowerHalMgr {
        @Override // com.mediatek.powerhalmgr.IPowerHalMgr
        public int scnReg() throws RemoteException {
            return 0;
        }

        @Override // com.mediatek.powerhalmgr.IPowerHalMgr
        public void scnConfig(int handle, int cmd, int param_1, int param_2, int param_3, int param_4) throws RemoteException {
        }

        @Override // com.mediatek.powerhalmgr.IPowerHalMgr
        public void scnUnreg(int handle) throws RemoteException {
        }

        @Override // com.mediatek.powerhalmgr.IPowerHalMgr
        public void scnEnable(int handle, int timeout) throws RemoteException {
        }

        @Override // com.mediatek.powerhalmgr.IPowerHalMgr
        public void scnDisable(int handle) throws RemoteException {
        }

        @Override // com.mediatek.powerhalmgr.IPowerHalMgr
        public void scnUltraCfg(int handle, int ultracmd, int param_1, int param_2, int param_3, int param_4) throws RemoteException {
        }

        @Override // com.mediatek.powerhalmgr.IPowerHalMgr
        public void mtkCusPowerHint(int hint, int data) throws RemoteException {
        }

        @Override // com.mediatek.powerhalmgr.IPowerHalMgr
        public void getCpuCap() throws RemoteException {
        }

        @Override // com.mediatek.powerhalmgr.IPowerHalMgr
        public void getGpuCap() throws RemoteException {
        }

        @Override // com.mediatek.powerhalmgr.IPowerHalMgr
        public void getGpuRTInfo() throws RemoteException {
        }

        @Override // com.mediatek.powerhalmgr.IPowerHalMgr
        public void getCpuRTInfo() throws RemoteException {
        }

        @Override // com.mediatek.powerhalmgr.IPowerHalMgr
        public void UpdateManagementPkt(int type, String packet) throws RemoteException {
        }

        @Override // com.mediatek.powerhalmgr.IPowerHalMgr
        public void setForegroundSports() throws RemoteException {
        }

        @Override // com.mediatek.powerhalmgr.IPowerHalMgr
        public void setSysInfo(int type, String data) throws RemoteException {
        }

        @Override // com.mediatek.powerhalmgr.IPowerHalMgr
        public boolean startDuplicatePacketPrediction() throws RemoteException {
            return false;
        }

        @Override // com.mediatek.powerhalmgr.IPowerHalMgr
        public boolean stopDuplicatePacketPrediction() throws RemoteException {
            return false;
        }

        @Override // com.mediatek.powerhalmgr.IPowerHalMgr
        public boolean isDupPacketPredictionStarted() throws RemoteException {
            return false;
        }

        @Override // com.mediatek.powerhalmgr.IPowerHalMgr
        public boolean registerDuplicatePacketPredictionEvent(IRemoteCallback listener) throws RemoteException {
            return false;
        }

        @Override // com.mediatek.powerhalmgr.IPowerHalMgr
        public boolean unregisterDuplicatePacketPredictionEvent(IRemoteCallback listener) throws RemoteException {
            return false;
        }

        @Override // com.mediatek.powerhalmgr.IPowerHalMgr
        public boolean updateMultiDuplicatePacketLink(DupLinkInfo[] linkList) throws RemoteException {
            return false;
        }

        @Override // com.mediatek.powerhalmgr.IPowerHalMgr
        public void setPredictInfo(String pack_name, int uid) throws RemoteException {
        }

        @Override // com.mediatek.powerhalmgr.IPowerHalMgr
        public int perfLockAcquire(int handle, int duration, int[] list) throws RemoteException {
            return 0;
        }

        @Override // com.mediatek.powerhalmgr.IPowerHalMgr
        public void perfLockRelease(int handle) throws RemoteException {
        }

        @Override // com.mediatek.powerhalmgr.IPowerHalMgr
        public int querySysInfo(int cmd, int param) throws RemoteException {
            return 0;
        }

        @Override // com.mediatek.powerhalmgr.IPowerHalMgr
        public void mtkPowerHint(int hint, int data) throws RemoteException {
        }

        @Override // com.mediatek.powerhalmgr.IPowerHalMgr
        public int perfCusLockHint(int hint, int duration) throws RemoteException {
            return 0;
        }

        @Override // com.mediatek.powerhalmgr.IPowerHalMgr
        public int setSysInfoSync(int type, String data) throws RemoteException {
            return 0;
        }

        @Override // com.mediatek.powerhalmgr.IPowerHalMgr
        public boolean setPriorityByUid(int action, int uid) throws RemoteException {
            return false;
        }

        @Override // com.mediatek.powerhalmgr.IPowerHalMgr
        public boolean setPriorityByLinkinfo(int action, DupLinkInfo linkinfo) throws RemoteException {
            return false;
        }

        @Override // com.mediatek.powerhalmgr.IPowerHalMgr
        public boolean flushPriorityRules(int type) throws RemoteException {
            return false;
        }

        @Override // com.mediatek.powerhalmgr.IPowerHalMgr
        public boolean configBoosterInfo(BoosterInfo info) throws RemoteException {
            return false;
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes.dex */
    public static abstract class Stub extends Binder implements IPowerHalMgr {
        static final int TRANSACTION_UpdateManagementPkt = 12;
        static final int TRANSACTION_configBoosterInfo = 31;
        static final int TRANSACTION_flushPriorityRules = 30;
        static final int TRANSACTION_getCpuCap = 8;
        static final int TRANSACTION_getCpuRTInfo = 11;
        static final int TRANSACTION_getGpuCap = 9;
        static final int TRANSACTION_getGpuRTInfo = 10;
        static final int TRANSACTION_isDupPacketPredictionStarted = 17;
        static final int TRANSACTION_mtkCusPowerHint = 7;
        static final int TRANSACTION_mtkPowerHint = 25;
        static final int TRANSACTION_perfCusLockHint = 26;
        static final int TRANSACTION_perfLockAcquire = 22;
        static final int TRANSACTION_perfLockRelease = 23;
        static final int TRANSACTION_querySysInfo = 24;
        static final int TRANSACTION_registerDuplicatePacketPredictionEvent = 18;
        static final int TRANSACTION_scnConfig = 2;
        static final int TRANSACTION_scnDisable = 5;
        static final int TRANSACTION_scnEnable = 4;
        static final int TRANSACTION_scnReg = 1;
        static final int TRANSACTION_scnUltraCfg = 6;
        static final int TRANSACTION_scnUnreg = 3;
        static final int TRANSACTION_setForegroundSports = 13;
        static final int TRANSACTION_setPredictInfo = 21;
        static final int TRANSACTION_setPriorityByLinkinfo = 29;
        static final int TRANSACTION_setPriorityByUid = 28;
        static final int TRANSACTION_setSysInfo = 14;
        static final int TRANSACTION_setSysInfoSync = 27;
        static final int TRANSACTION_startDuplicatePacketPrediction = 15;
        static final int TRANSACTION_stopDuplicatePacketPrediction = 16;
        static final int TRANSACTION_unregisterDuplicatePacketPredictionEvent = 19;
        static final int TRANSACTION_updateMultiDuplicatePacketLink = 20;

        public Stub() {
            attachInterface(this, IPowerHalMgr.DESCRIPTOR);
        }

        public static IPowerHalMgr asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(IPowerHalMgr.DESCRIPTOR);
            if (iin != null && (iin instanceof IPowerHalMgr)) {
                return (IPowerHalMgr) iin;
            }
            return new Proxy(obj);
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return this;
        }

        @Override // android.os.Binder
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            if (code >= 1 && code <= 16777215) {
                data.enforceInterface(IPowerHalMgr.DESCRIPTOR);
            }
            switch (code) {
                case 1598968902:
                    reply.writeString(IPowerHalMgr.DESCRIPTOR);
                    return true;
                default:
                    switch (code) {
                        case 1:
                            int _result = scnReg();
                            reply.writeNoException();
                            reply.writeInt(_result);
                            break;
                        case 2:
                            int _arg0 = data.readInt();
                            int _arg1 = data.readInt();
                            int _arg2 = data.readInt();
                            int _arg3 = data.readInt();
                            int _arg4 = data.readInt();
                            int _arg5 = data.readInt();
                            data.enforceNoDataAvail();
                            scnConfig(_arg0, _arg1, _arg2, _arg3, _arg4, _arg5);
                            break;
                        case 3:
                            int _arg02 = data.readInt();
                            data.enforceNoDataAvail();
                            scnUnreg(_arg02);
                            break;
                        case 4:
                            int _arg03 = data.readInt();
                            int _arg12 = data.readInt();
                            data.enforceNoDataAvail();
                            scnEnable(_arg03, _arg12);
                            break;
                        case 5:
                            int _arg04 = data.readInt();
                            data.enforceNoDataAvail();
                            scnDisable(_arg04);
                            break;
                        case 6:
                            int _arg05 = data.readInt();
                            int _arg13 = data.readInt();
                            int _arg22 = data.readInt();
                            int _arg32 = data.readInt();
                            int _arg42 = data.readInt();
                            int _arg52 = data.readInt();
                            data.enforceNoDataAvail();
                            scnUltraCfg(_arg05, _arg13, _arg22, _arg32, _arg42, _arg52);
                            break;
                        case 7:
                            int _arg06 = data.readInt();
                            int _arg14 = data.readInt();
                            data.enforceNoDataAvail();
                            mtkCusPowerHint(_arg06, _arg14);
                            break;
                        case 8:
                            getCpuCap();
                            break;
                        case 9:
                            getGpuCap();
                            break;
                        case 10:
                            getGpuRTInfo();
                            break;
                        case 11:
                            getCpuRTInfo();
                            break;
                        case 12:
                            int _arg07 = data.readInt();
                            String _arg15 = data.readString();
                            data.enforceNoDataAvail();
                            UpdateManagementPkt(_arg07, _arg15);
                            break;
                        case 13:
                            setForegroundSports();
                            break;
                        case 14:
                            int _arg08 = data.readInt();
                            String _arg16 = data.readString();
                            data.enforceNoDataAvail();
                            setSysInfo(_arg08, _arg16);
                            break;
                        case 15:
                            boolean _result2 = startDuplicatePacketPrediction();
                            reply.writeNoException();
                            reply.writeBoolean(_result2);
                            break;
                        case 16:
                            boolean _result3 = stopDuplicatePacketPrediction();
                            reply.writeNoException();
                            reply.writeBoolean(_result3);
                            break;
                        case 17:
                            boolean _result4 = isDupPacketPredictionStarted();
                            reply.writeNoException();
                            reply.writeBoolean(_result4);
                            break;
                        case 18:
                            IRemoteCallback _arg09 = IRemoteCallback.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            boolean _result5 = registerDuplicatePacketPredictionEvent(_arg09);
                            reply.writeNoException();
                            reply.writeBoolean(_result5);
                            break;
                        case TRANSACTION_unregisterDuplicatePacketPredictionEvent /* 19 */:
                            IRemoteCallback _arg010 = IRemoteCallback.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            boolean _result6 = unregisterDuplicatePacketPredictionEvent(_arg010);
                            reply.writeNoException();
                            reply.writeBoolean(_result6);
                            break;
                        case 20:
                            DupLinkInfo[] _arg011 = (DupLinkInfo[]) data.createTypedArray(DupLinkInfo.CREATOR);
                            data.enforceNoDataAvail();
                            boolean _result7 = updateMultiDuplicatePacketLink(_arg011);
                            reply.writeNoException();
                            reply.writeBoolean(_result7);
                            break;
                        case TRANSACTION_setPredictInfo /* 21 */:
                            String _arg012 = data.readString();
                            int _arg17 = data.readInt();
                            data.enforceNoDataAvail();
                            setPredictInfo(_arg012, _arg17);
                            break;
                        case TRANSACTION_perfLockAcquire /* 22 */:
                            int _arg013 = data.readInt();
                            int _arg18 = data.readInt();
                            int[] _arg23 = data.createIntArray();
                            data.enforceNoDataAvail();
                            int _result8 = perfLockAcquire(_arg013, _arg18, _arg23);
                            reply.writeNoException();
                            reply.writeInt(_result8);
                            break;
                        case TRANSACTION_perfLockRelease /* 23 */:
                            int _arg014 = data.readInt();
                            data.enforceNoDataAvail();
                            perfLockRelease(_arg014);
                            break;
                        case TRANSACTION_querySysInfo /* 24 */:
                            int _arg015 = data.readInt();
                            int _arg19 = data.readInt();
                            data.enforceNoDataAvail();
                            int _result9 = querySysInfo(_arg015, _arg19);
                            reply.writeNoException();
                            reply.writeInt(_result9);
                            break;
                        case TRANSACTION_mtkPowerHint /* 25 */:
                            int _arg016 = data.readInt();
                            int _arg110 = data.readInt();
                            data.enforceNoDataAvail();
                            mtkPowerHint(_arg016, _arg110);
                            break;
                        case TRANSACTION_perfCusLockHint /* 26 */:
                            int _arg017 = data.readInt();
                            int _arg111 = data.readInt();
                            data.enforceNoDataAvail();
                            int _result10 = perfCusLockHint(_arg017, _arg111);
                            reply.writeNoException();
                            reply.writeInt(_result10);
                            break;
                        case TRANSACTION_setSysInfoSync /* 27 */:
                            int _arg018 = data.readInt();
                            String _arg112 = data.readString();
                            data.enforceNoDataAvail();
                            int _result11 = setSysInfoSync(_arg018, _arg112);
                            reply.writeNoException();
                            reply.writeInt(_result11);
                            break;
                        case TRANSACTION_setPriorityByUid /* 28 */:
                            int _arg019 = data.readInt();
                            int _arg113 = data.readInt();
                            data.enforceNoDataAvail();
                            boolean _result12 = setPriorityByUid(_arg019, _arg113);
                            reply.writeNoException();
                            reply.writeBoolean(_result12);
                            break;
                        case TRANSACTION_setPriorityByLinkinfo /* 29 */:
                            int _arg020 = data.readInt();
                            DupLinkInfo _arg114 = (DupLinkInfo) data.readTypedObject(DupLinkInfo.CREATOR);
                            data.enforceNoDataAvail();
                            boolean _result13 = setPriorityByLinkinfo(_arg020, _arg114);
                            reply.writeNoException();
                            reply.writeBoolean(_result13);
                            break;
                        case TRANSACTION_flushPriorityRules /* 30 */:
                            int _arg021 = data.readInt();
                            data.enforceNoDataAvail();
                            boolean _result14 = flushPriorityRules(_arg021);
                            reply.writeNoException();
                            reply.writeBoolean(_result14);
                            break;
                        case TRANSACTION_configBoosterInfo /* 31 */:
                            BoosterInfo _arg022 = (BoosterInfo) data.readTypedObject(BoosterInfo.CREATOR);
                            data.enforceNoDataAvail();
                            boolean _result15 = configBoosterInfo(_arg022);
                            reply.writeNoException();
                            reply.writeBoolean(_result15);
                            break;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
                    return true;
            }
        }

        /* loaded from: classes.dex */
        private static class Proxy implements IPowerHalMgr {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return IPowerHalMgr.DESCRIPTOR;
            }

            @Override // com.mediatek.powerhalmgr.IPowerHalMgr
            public int scnReg() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IPowerHalMgr.DESCRIPTOR);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.mediatek.powerhalmgr.IPowerHalMgr
            public void scnConfig(int handle, int cmd, int param_1, int param_2, int param_3, int param_4) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IPowerHalMgr.DESCRIPTOR);
                    _data.writeInt(handle);
                    _data.writeInt(cmd);
                    _data.writeInt(param_1);
                    _data.writeInt(param_2);
                    _data.writeInt(param_3);
                    _data.writeInt(param_4);
                    this.mRemote.transact(2, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.mediatek.powerhalmgr.IPowerHalMgr
            public void scnUnreg(int handle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IPowerHalMgr.DESCRIPTOR);
                    _data.writeInt(handle);
                    this.mRemote.transact(3, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.mediatek.powerhalmgr.IPowerHalMgr
            public void scnEnable(int handle, int timeout) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IPowerHalMgr.DESCRIPTOR);
                    _data.writeInt(handle);
                    _data.writeInt(timeout);
                    this.mRemote.transact(4, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.mediatek.powerhalmgr.IPowerHalMgr
            public void scnDisable(int handle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IPowerHalMgr.DESCRIPTOR);
                    _data.writeInt(handle);
                    this.mRemote.transact(5, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.mediatek.powerhalmgr.IPowerHalMgr
            public void scnUltraCfg(int handle, int ultracmd, int param_1, int param_2, int param_3, int param_4) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IPowerHalMgr.DESCRIPTOR);
                    _data.writeInt(handle);
                    _data.writeInt(ultracmd);
                    _data.writeInt(param_1);
                    _data.writeInt(param_2);
                    _data.writeInt(param_3);
                    _data.writeInt(param_4);
                    this.mRemote.transact(6, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.mediatek.powerhalmgr.IPowerHalMgr
            public void mtkCusPowerHint(int hint, int data) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IPowerHalMgr.DESCRIPTOR);
                    _data.writeInt(hint);
                    _data.writeInt(data);
                    this.mRemote.transact(7, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.mediatek.powerhalmgr.IPowerHalMgr
            public void getCpuCap() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IPowerHalMgr.DESCRIPTOR);
                    this.mRemote.transact(8, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.mediatek.powerhalmgr.IPowerHalMgr
            public void getGpuCap() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IPowerHalMgr.DESCRIPTOR);
                    this.mRemote.transact(9, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.mediatek.powerhalmgr.IPowerHalMgr
            public void getGpuRTInfo() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IPowerHalMgr.DESCRIPTOR);
                    this.mRemote.transact(10, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.mediatek.powerhalmgr.IPowerHalMgr
            public void getCpuRTInfo() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IPowerHalMgr.DESCRIPTOR);
                    this.mRemote.transact(11, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.mediatek.powerhalmgr.IPowerHalMgr
            public void UpdateManagementPkt(int type, String packet) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IPowerHalMgr.DESCRIPTOR);
                    _data.writeInt(type);
                    _data.writeString(packet);
                    this.mRemote.transact(12, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.mediatek.powerhalmgr.IPowerHalMgr
            public void setForegroundSports() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IPowerHalMgr.DESCRIPTOR);
                    this.mRemote.transact(13, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.mediatek.powerhalmgr.IPowerHalMgr
            public void setSysInfo(int type, String data) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IPowerHalMgr.DESCRIPTOR);
                    _data.writeInt(type);
                    _data.writeString(data);
                    this.mRemote.transact(14, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.mediatek.powerhalmgr.IPowerHalMgr
            public boolean startDuplicatePacketPrediction() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IPowerHalMgr.DESCRIPTOR);
                    this.mRemote.transact(15, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.mediatek.powerhalmgr.IPowerHalMgr
            public boolean stopDuplicatePacketPrediction() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IPowerHalMgr.DESCRIPTOR);
                    this.mRemote.transact(16, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.mediatek.powerhalmgr.IPowerHalMgr
            public boolean isDupPacketPredictionStarted() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IPowerHalMgr.DESCRIPTOR);
                    this.mRemote.transact(17, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.mediatek.powerhalmgr.IPowerHalMgr
            public boolean registerDuplicatePacketPredictionEvent(IRemoteCallback listener) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IPowerHalMgr.DESCRIPTOR);
                    _data.writeStrongInterface(listener);
                    this.mRemote.transact(18, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.mediatek.powerhalmgr.IPowerHalMgr
            public boolean unregisterDuplicatePacketPredictionEvent(IRemoteCallback listener) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IPowerHalMgr.DESCRIPTOR);
                    _data.writeStrongInterface(listener);
                    this.mRemote.transact(Stub.TRANSACTION_unregisterDuplicatePacketPredictionEvent, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.mediatek.powerhalmgr.IPowerHalMgr
            public boolean updateMultiDuplicatePacketLink(DupLinkInfo[] linkList) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IPowerHalMgr.DESCRIPTOR);
                    _data.writeTypedArray(linkList, 0);
                    this.mRemote.transact(20, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.mediatek.powerhalmgr.IPowerHalMgr
            public void setPredictInfo(String pack_name, int uid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IPowerHalMgr.DESCRIPTOR);
                    _data.writeString(pack_name);
                    _data.writeInt(uid);
                    this.mRemote.transact(Stub.TRANSACTION_setPredictInfo, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.mediatek.powerhalmgr.IPowerHalMgr
            public int perfLockAcquire(int handle, int duration, int[] list) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IPowerHalMgr.DESCRIPTOR);
                    _data.writeInt(handle);
                    _data.writeInt(duration);
                    _data.writeIntArray(list);
                    this.mRemote.transact(Stub.TRANSACTION_perfLockAcquire, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.mediatek.powerhalmgr.IPowerHalMgr
            public void perfLockRelease(int handle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IPowerHalMgr.DESCRIPTOR);
                    _data.writeInt(handle);
                    this.mRemote.transact(Stub.TRANSACTION_perfLockRelease, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.mediatek.powerhalmgr.IPowerHalMgr
            public int querySysInfo(int cmd, int param) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IPowerHalMgr.DESCRIPTOR);
                    _data.writeInt(cmd);
                    _data.writeInt(param);
                    this.mRemote.transact(Stub.TRANSACTION_querySysInfo, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.mediatek.powerhalmgr.IPowerHalMgr
            public void mtkPowerHint(int hint, int data) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IPowerHalMgr.DESCRIPTOR);
                    _data.writeInt(hint);
                    _data.writeInt(data);
                    this.mRemote.transact(Stub.TRANSACTION_mtkPowerHint, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.mediatek.powerhalmgr.IPowerHalMgr
            public int perfCusLockHint(int hint, int duration) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IPowerHalMgr.DESCRIPTOR);
                    _data.writeInt(hint);
                    _data.writeInt(duration);
                    this.mRemote.transact(Stub.TRANSACTION_perfCusLockHint, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.mediatek.powerhalmgr.IPowerHalMgr
            public int setSysInfoSync(int type, String data) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IPowerHalMgr.DESCRIPTOR);
                    _data.writeInt(type);
                    _data.writeString(data);
                    this.mRemote.transact(Stub.TRANSACTION_setSysInfoSync, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.mediatek.powerhalmgr.IPowerHalMgr
            public boolean setPriorityByUid(int action, int uid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IPowerHalMgr.DESCRIPTOR);
                    _data.writeInt(action);
                    _data.writeInt(uid);
                    this.mRemote.transact(Stub.TRANSACTION_setPriorityByUid, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.mediatek.powerhalmgr.IPowerHalMgr
            public boolean setPriorityByLinkinfo(int action, DupLinkInfo linkinfo) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IPowerHalMgr.DESCRIPTOR);
                    _data.writeInt(action);
                    _data.writeTypedObject(linkinfo, 0);
                    this.mRemote.transact(Stub.TRANSACTION_setPriorityByLinkinfo, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.mediatek.powerhalmgr.IPowerHalMgr
            public boolean flushPriorityRules(int type) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IPowerHalMgr.DESCRIPTOR);
                    _data.writeInt(type);
                    this.mRemote.transact(Stub.TRANSACTION_flushPriorityRules, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.mediatek.powerhalmgr.IPowerHalMgr
            public boolean configBoosterInfo(BoosterInfo info) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IPowerHalMgr.DESCRIPTOR);
                    _data.writeTypedObject(info, 0);
                    this.mRemote.transact(Stub.TRANSACTION_configBoosterInfo, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }
    }
}
