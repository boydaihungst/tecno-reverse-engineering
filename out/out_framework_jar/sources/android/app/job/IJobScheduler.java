package android.app.job;

import android.content.pm.ParceledListSlice;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.service.notification.ZenModeConfig;
import java.util.List;
/* loaded from: classes.dex */
public interface IJobScheduler extends IInterface {
    void cancel(int i) throws RemoteException;

    void cancelAll() throws RemoteException;

    int enqueue(JobInfo jobInfo, JobWorkItem jobWorkItem) throws RemoteException;

    ParceledListSlice getAllJobSnapshots() throws RemoteException;

    ParceledListSlice getAllPendingJobs() throws RemoteException;

    JobInfo getPendingJob(int i) throws RemoteException;

    List<JobInfo> getStartedJobs() throws RemoteException;

    int schedule(JobInfo jobInfo) throws RemoteException;

    int scheduleAsPackage(JobInfo jobInfo, String str, int i, String str2) throws RemoteException;

    /* loaded from: classes.dex */
    public static class Default implements IJobScheduler {
        @Override // android.app.job.IJobScheduler
        public int schedule(JobInfo job) throws RemoteException {
            return 0;
        }

        @Override // android.app.job.IJobScheduler
        public int enqueue(JobInfo job, JobWorkItem work) throws RemoteException {
            return 0;
        }

        @Override // android.app.job.IJobScheduler
        public int scheduleAsPackage(JobInfo job, String packageName, int userId, String tag) throws RemoteException {
            return 0;
        }

        @Override // android.app.job.IJobScheduler
        public void cancel(int jobId) throws RemoteException {
        }

        @Override // android.app.job.IJobScheduler
        public void cancelAll() throws RemoteException {
        }

        @Override // android.app.job.IJobScheduler
        public ParceledListSlice getAllPendingJobs() throws RemoteException {
            return null;
        }

        @Override // android.app.job.IJobScheduler
        public JobInfo getPendingJob(int jobId) throws RemoteException {
            return null;
        }

        @Override // android.app.job.IJobScheduler
        public List<JobInfo> getStartedJobs() throws RemoteException {
            return null;
        }

        @Override // android.app.job.IJobScheduler
        public ParceledListSlice getAllJobSnapshots() throws RemoteException {
            return null;
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes.dex */
    public static abstract class Stub extends Binder implements IJobScheduler {
        public static final String DESCRIPTOR = "android.app.job.IJobScheduler";
        static final int TRANSACTION_cancel = 4;
        static final int TRANSACTION_cancelAll = 5;
        static final int TRANSACTION_enqueue = 2;
        static final int TRANSACTION_getAllJobSnapshots = 9;
        static final int TRANSACTION_getAllPendingJobs = 6;
        static final int TRANSACTION_getPendingJob = 7;
        static final int TRANSACTION_getStartedJobs = 8;
        static final int TRANSACTION_schedule = 1;
        static final int TRANSACTION_scheduleAsPackage = 3;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IJobScheduler asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IJobScheduler)) {
                return (IJobScheduler) iin;
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
                    return ZenModeConfig.SCHEDULE_PATH;
                case 2:
                    return "enqueue";
                case 3:
                    return "scheduleAsPackage";
                case 4:
                    return "cancel";
                case 5:
                    return "cancelAll";
                case 6:
                    return "getAllPendingJobs";
                case 7:
                    return "getPendingJob";
                case 8:
                    return "getStartedJobs";
                case 9:
                    return "getAllJobSnapshots";
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
                            JobInfo _arg0 = (JobInfo) data.readTypedObject(JobInfo.CREATOR);
                            data.enforceNoDataAvail();
                            int _result = schedule(_arg0);
                            reply.writeNoException();
                            reply.writeInt(_result);
                            break;
                        case 2:
                            JobInfo _arg02 = (JobInfo) data.readTypedObject(JobInfo.CREATOR);
                            JobWorkItem _arg1 = (JobWorkItem) data.readTypedObject(JobWorkItem.CREATOR);
                            data.enforceNoDataAvail();
                            int _result2 = enqueue(_arg02, _arg1);
                            reply.writeNoException();
                            reply.writeInt(_result2);
                            break;
                        case 3:
                            JobInfo _arg03 = (JobInfo) data.readTypedObject(JobInfo.CREATOR);
                            String _arg12 = data.readString();
                            int _arg2 = data.readInt();
                            String _arg3 = data.readString();
                            data.enforceNoDataAvail();
                            int _result3 = scheduleAsPackage(_arg03, _arg12, _arg2, _arg3);
                            reply.writeNoException();
                            reply.writeInt(_result3);
                            break;
                        case 4:
                            int _arg04 = data.readInt();
                            data.enforceNoDataAvail();
                            cancel(_arg04);
                            reply.writeNoException();
                            break;
                        case 5:
                            cancelAll();
                            reply.writeNoException();
                            break;
                        case 6:
                            ParceledListSlice _result4 = getAllPendingJobs();
                            reply.writeNoException();
                            reply.writeTypedObject(_result4, 1);
                            break;
                        case 7:
                            int _arg05 = data.readInt();
                            data.enforceNoDataAvail();
                            JobInfo _result5 = getPendingJob(_arg05);
                            reply.writeNoException();
                            reply.writeTypedObject(_result5, 1);
                            break;
                        case 8:
                            List<JobInfo> _result6 = getStartedJobs();
                            reply.writeNoException();
                            reply.writeTypedList(_result6);
                            break;
                        case 9:
                            ParceledListSlice _result7 = getAllJobSnapshots();
                            reply.writeNoException();
                            reply.writeTypedObject(_result7, 1);
                            break;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
                    return true;
            }
        }

        /* loaded from: classes.dex */
        private static class Proxy implements IJobScheduler {
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

            @Override // android.app.job.IJobScheduler
            public int schedule(JobInfo job) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(job, 0);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.job.IJobScheduler
            public int enqueue(JobInfo job, JobWorkItem work) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(job, 0);
                    _data.writeTypedObject(work, 0);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.job.IJobScheduler
            public int scheduleAsPackage(JobInfo job, String packageName, int userId, String tag) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(job, 0);
                    _data.writeString(packageName);
                    _data.writeInt(userId);
                    _data.writeString(tag);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.job.IJobScheduler
            public void cancel(int jobId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(jobId);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.job.IJobScheduler
            public void cancelAll() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.job.IJobScheduler
            public ParceledListSlice getAllPendingJobs() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                    ParceledListSlice _result = (ParceledListSlice) _reply.readTypedObject(ParceledListSlice.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.job.IJobScheduler
            public JobInfo getPendingJob(int jobId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(jobId);
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                    JobInfo _result = (JobInfo) _reply.readTypedObject(JobInfo.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.job.IJobScheduler
            public List<JobInfo> getStartedJobs() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                    List<JobInfo> _result = _reply.createTypedArrayList(JobInfo.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.job.IJobScheduler
            public ParceledListSlice getAllJobSnapshots() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(9, _data, _reply, 0);
                    _reply.readException();
                    ParceledListSlice _result = (ParceledListSlice) _reply.readTypedObject(ParceledListSlice.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }

        @Override // android.os.Binder
        public int getMaxTransactionId() {
            return 8;
        }
    }
}
