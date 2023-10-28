package android.app;

import android.app.AlarmManager;
import android.app.IAlarmListener;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.WorkSource;
/* loaded from: classes.dex */
public interface IAlarmManager extends IInterface {
    boolean canScheduleExactAlarms(String str) throws RemoteException;

    void cancelPoweroffAlarm(String str) throws RemoteException;

    long currentNetworkTimeMillis() throws RemoteException;

    int getConfigVersion() throws RemoteException;

    AlarmManager.AlarmClockInfo getNextAlarmClock(int i) throws RemoteException;

    long getNextWakeFromIdleTime() throws RemoteException;

    boolean hasScheduleExactAlarm(String str, int i) throws RemoteException;

    void remove(PendingIntent pendingIntent, IAlarmListener iAlarmListener) throws RemoteException;

    void set(String str, int i, long j, long j2, long j3, int i2, PendingIntent pendingIntent, IAlarmListener iAlarmListener, String str2, WorkSource workSource, AlarmManager.AlarmClockInfo alarmClockInfo) throws RemoteException;

    boolean setTime(long j) throws RemoteException;

    void setTimeZone(String str) throws RemoteException;

    /* loaded from: classes.dex */
    public static class Default implements IAlarmManager {
        @Override // android.app.IAlarmManager
        public void set(String callingPackage, int type, long triggerAtTime, long windowLength, long interval, int flags, PendingIntent operation, IAlarmListener listener, String listenerTag, WorkSource workSource, AlarmManager.AlarmClockInfo alarmClock) throws RemoteException {
        }

        @Override // android.app.IAlarmManager
        public boolean setTime(long millis) throws RemoteException {
            return false;
        }

        @Override // android.app.IAlarmManager
        public void setTimeZone(String zone) throws RemoteException {
        }

        @Override // android.app.IAlarmManager
        public void remove(PendingIntent operation, IAlarmListener listener) throws RemoteException {
        }

        @Override // android.app.IAlarmManager
        public long getNextWakeFromIdleTime() throws RemoteException {
            return 0L;
        }

        @Override // android.app.IAlarmManager
        public AlarmManager.AlarmClockInfo getNextAlarmClock(int userId) throws RemoteException {
            return null;
        }

        @Override // android.app.IAlarmManager
        public long currentNetworkTimeMillis() throws RemoteException {
            return 0L;
        }

        @Override // android.app.IAlarmManager
        public boolean canScheduleExactAlarms(String packageName) throws RemoteException {
            return false;
        }

        @Override // android.app.IAlarmManager
        public boolean hasScheduleExactAlarm(String packageName, int userId) throws RemoteException {
            return false;
        }

        @Override // android.app.IAlarmManager
        public int getConfigVersion() throws RemoteException {
            return 0;
        }

        @Override // android.app.IAlarmManager
        public void cancelPoweroffAlarm(String name) throws RemoteException {
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes.dex */
    public static abstract class Stub extends Binder implements IAlarmManager {
        public static final String DESCRIPTOR = "android.app.IAlarmManager";
        static final int TRANSACTION_canScheduleExactAlarms = 8;
        static final int TRANSACTION_cancelPoweroffAlarm = 11;
        static final int TRANSACTION_currentNetworkTimeMillis = 7;
        static final int TRANSACTION_getConfigVersion = 10;
        static final int TRANSACTION_getNextAlarmClock = 6;
        static final int TRANSACTION_getNextWakeFromIdleTime = 5;
        static final int TRANSACTION_hasScheduleExactAlarm = 9;
        static final int TRANSACTION_remove = 4;
        static final int TRANSACTION_set = 1;
        static final int TRANSACTION_setTime = 2;
        static final int TRANSACTION_setTimeZone = 3;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IAlarmManager asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IAlarmManager)) {
                return (IAlarmManager) iin;
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
                    return "set";
                case 2:
                    return "setTime";
                case 3:
                    return "setTimeZone";
                case 4:
                    return "remove";
                case 5:
                    return "getNextWakeFromIdleTime";
                case 6:
                    return "getNextAlarmClock";
                case 7:
                    return "currentNetworkTimeMillis";
                case 8:
                    return "canScheduleExactAlarms";
                case 9:
                    return "hasScheduleExactAlarm";
                case 10:
                    return "getConfigVersion";
                case 11:
                    return "cancelPoweroffAlarm";
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
                            int _arg1 = data.readInt();
                            long _arg2 = data.readLong();
                            long _arg3 = data.readLong();
                            long _arg4 = data.readLong();
                            int _arg5 = data.readInt();
                            PendingIntent _arg6 = (PendingIntent) data.readTypedObject(PendingIntent.CREATOR);
                            IAlarmListener _arg7 = IAlarmListener.Stub.asInterface(data.readStrongBinder());
                            String _arg8 = data.readString();
                            WorkSource _arg9 = (WorkSource) data.readTypedObject(WorkSource.CREATOR);
                            AlarmManager.AlarmClockInfo _arg10 = (AlarmManager.AlarmClockInfo) data.readTypedObject(AlarmManager.AlarmClockInfo.CREATOR);
                            data.enforceNoDataAvail();
                            set(_arg0, _arg1, _arg2, _arg3, _arg4, _arg5, _arg6, _arg7, _arg8, _arg9, _arg10);
                            reply.writeNoException();
                            return true;
                        case 2:
                            long _arg02 = data.readLong();
                            data.enforceNoDataAvail();
                            boolean _result = setTime(_arg02);
                            reply.writeNoException();
                            reply.writeBoolean(_result);
                            return true;
                        case 3:
                            String _arg03 = data.readString();
                            data.enforceNoDataAvail();
                            setTimeZone(_arg03);
                            reply.writeNoException();
                            return true;
                        case 4:
                            PendingIntent _arg04 = (PendingIntent) data.readTypedObject(PendingIntent.CREATOR);
                            IAlarmListener _arg12 = IAlarmListener.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            remove(_arg04, _arg12);
                            reply.writeNoException();
                            return true;
                        case 5:
                            long _result2 = getNextWakeFromIdleTime();
                            reply.writeNoException();
                            reply.writeLong(_result2);
                            return true;
                        case 6:
                            int _arg05 = data.readInt();
                            data.enforceNoDataAvail();
                            AlarmManager.AlarmClockInfo _result3 = getNextAlarmClock(_arg05);
                            reply.writeNoException();
                            reply.writeTypedObject(_result3, 1);
                            return true;
                        case 7:
                            long _result4 = currentNetworkTimeMillis();
                            reply.writeNoException();
                            reply.writeLong(_result4);
                            return true;
                        case 8:
                            String _arg06 = data.readString();
                            data.enforceNoDataAvail();
                            boolean _result5 = canScheduleExactAlarms(_arg06);
                            reply.writeNoException();
                            reply.writeBoolean(_result5);
                            return true;
                        case 9:
                            String _arg07 = data.readString();
                            int _arg13 = data.readInt();
                            data.enforceNoDataAvail();
                            boolean _result6 = hasScheduleExactAlarm(_arg07, _arg13);
                            reply.writeNoException();
                            reply.writeBoolean(_result6);
                            return true;
                        case 10:
                            int _result7 = getConfigVersion();
                            reply.writeNoException();
                            reply.writeInt(_result7);
                            return true;
                        case 11:
                            String _arg08 = data.readString();
                            data.enforceNoDataAvail();
                            cancelPoweroffAlarm(_arg08);
                            reply.writeNoException();
                            return true;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        /* loaded from: classes.dex */
        public static class Proxy implements IAlarmManager {
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

            @Override // android.app.IAlarmManager
            public void set(String callingPackage, int type, long triggerAtTime, long windowLength, long interval, int flags, PendingIntent operation, IAlarmListener listener, String listenerTag, WorkSource workSource, AlarmManager.AlarmClockInfo alarmClock) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    _data.writeInt(type);
                    try {
                        _data.writeLong(triggerAtTime);
                        try {
                            _data.writeLong(windowLength);
                            try {
                                _data.writeLong(interval);
                                try {
                                    _data.writeInt(flags);
                                    try {
                                        _data.writeTypedObject(operation, 0);
                                    } catch (Throwable th) {
                                        th = th;
                                        _reply.recycle();
                                        _data.recycle();
                                        throw th;
                                    }
                                } catch (Throwable th2) {
                                    th = th2;
                                    _reply.recycle();
                                    _data.recycle();
                                    throw th;
                                }
                            } catch (Throwable th3) {
                                th = th3;
                                _reply.recycle();
                                _data.recycle();
                                throw th;
                            }
                        } catch (Throwable th4) {
                            th = th4;
                            _reply.recycle();
                            _data.recycle();
                            throw th;
                        }
                    } catch (Throwable th5) {
                        th = th5;
                        _reply.recycle();
                        _data.recycle();
                        throw th;
                    }
                } catch (Throwable th6) {
                    th = th6;
                }
                try {
                    _data.writeStrongInterface(listener);
                    try {
                        _data.writeString(listenerTag);
                        try {
                            _data.writeTypedObject(workSource, 0);
                            _data.writeTypedObject(alarmClock, 0);
                            this.mRemote.transact(1, _data, _reply, 0);
                            _reply.readException();
                            _reply.recycle();
                            _data.recycle();
                        } catch (Throwable th7) {
                            th = th7;
                            _reply.recycle();
                            _data.recycle();
                            throw th;
                        }
                    } catch (Throwable th8) {
                        th = th8;
                        _reply.recycle();
                        _data.recycle();
                        throw th;
                    }
                } catch (Throwable th9) {
                    th = th9;
                    _reply.recycle();
                    _data.recycle();
                    throw th;
                }
            }

            @Override // android.app.IAlarmManager
            public boolean setTime(long millis) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeLong(millis);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IAlarmManager
            public void setTimeZone(String zone) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(zone);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IAlarmManager
            public void remove(PendingIntent operation, IAlarmListener listener) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(operation, 0);
                    _data.writeStrongInterface(listener);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IAlarmManager
            public long getNextWakeFromIdleTime() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                    long _result = _reply.readLong();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IAlarmManager
            public AlarmManager.AlarmClockInfo getNextAlarmClock(int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                    AlarmManager.AlarmClockInfo _result = (AlarmManager.AlarmClockInfo) _reply.readTypedObject(AlarmManager.AlarmClockInfo.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IAlarmManager
            public long currentNetworkTimeMillis() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                    long _result = _reply.readLong();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IAlarmManager
            public boolean canScheduleExactAlarms(String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IAlarmManager
            public boolean hasScheduleExactAlarm(String packageName, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeInt(userId);
                    this.mRemote.transact(9, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IAlarmManager
            public int getConfigVersion() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(10, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IAlarmManager
            public void cancelPoweroffAlarm(String name) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(name);
                    this.mRemote.transact(11, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }

        @Override // android.os.Binder
        public int getMaxTransactionId() {
            return 10;
        }
    }
}
