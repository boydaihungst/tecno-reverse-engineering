package com.android.server;

import android.content.Context;
import android.location.Country;
import android.location.CountryListener;
import android.location.ICountryDetector;
import android.location.ICountryListener;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Slog;
import com.android.internal.os.BackgroundThread;
import com.android.internal.util.DumpUtils;
import com.android.server.location.countrydetector.ComprehensiveCountryDetector;
import com.android.server.location.countrydetector.CountryDetectorBase;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
/* loaded from: classes.dex */
public class CountryDetectorService extends ICountryDetector.Stub {
    private static final boolean DEBUG = false;
    private static final String TAG = "CountryDetector";
    private final Context mContext;
    private CountryDetectorBase mCountryDetector;
    private Handler mHandler;
    private CountryListener mLocationBasedDetectorListener;
    private final HashMap<IBinder, Receiver> mReceivers;
    private boolean mSystemReady;

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class Receiver implements IBinder.DeathRecipient {
        private final IBinder mKey;
        private final ICountryListener mListener;

        public Receiver(ICountryListener listener) {
            this.mListener = listener;
            this.mKey = listener.asBinder();
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            CountryDetectorService.this.removeListener(this.mKey);
        }

        public boolean equals(Object otherObj) {
            if (otherObj instanceof Receiver) {
                return this.mKey.equals(((Receiver) otherObj).mKey);
            }
            return false;
        }

        public int hashCode() {
            return this.mKey.hashCode();
        }

        public ICountryListener getListener() {
            return this.mListener;
        }
    }

    public CountryDetectorService(Context context) {
        this(context, BackgroundThread.getHandler());
    }

    CountryDetectorService(Context context, Handler handler) {
        this.mReceivers = new HashMap<>();
        this.mContext = context;
        this.mHandler = handler;
    }

    public Country detectCountry() {
        if (!this.mSystemReady) {
            return null;
        }
        return this.mCountryDetector.detectCountry();
    }

    public void addCountryListener(ICountryListener listener) throws RemoteException {
        if (!this.mSystemReady) {
            throw new RemoteException();
        }
        addListener(listener);
    }

    public void removeCountryListener(ICountryListener listener) throws RemoteException {
        if (!this.mSystemReady) {
            throw new RemoteException();
        }
        removeListener(listener.asBinder());
    }

    private void addListener(ICountryListener listener) {
        synchronized (this.mReceivers) {
            Receiver r = new Receiver(listener);
            try {
                listener.asBinder().linkToDeath(r, 0);
                this.mReceivers.put(listener.asBinder(), r);
                if (this.mReceivers.size() == 1) {
                    Slog.d(TAG, "The first listener is added");
                    setCountryListener(this.mLocationBasedDetectorListener);
                }
            } catch (RemoteException e) {
                Slog.e(TAG, "linkToDeath failed:", e);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void removeListener(IBinder key) {
        synchronized (this.mReceivers) {
            this.mReceivers.remove(key);
            if (this.mReceivers.isEmpty()) {
                setCountryListener(null);
                Slog.d(TAG, "No listener is left");
            }
        }
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: protected */
    /* renamed from: notifyReceivers */
    public void m109lambda$initialize$1$comandroidserverCountryDetectorService(Country country) {
        synchronized (this.mReceivers) {
            for (Receiver receiver : this.mReceivers.values()) {
                try {
                    receiver.getListener().onCountryDetected(country);
                } catch (RemoteException e) {
                    Slog.e(TAG, "notifyReceivers failed:", e);
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void systemRunning() {
        this.mHandler.post(new Runnable() { // from class: com.android.server.CountryDetectorService$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                CountryDetectorService.this.m112lambda$systemRunning$0$comandroidserverCountryDetectorService();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$systemRunning$0$com-android-server-CountryDetectorService  reason: not valid java name */
    public /* synthetic */ void m112lambda$systemRunning$0$comandroidserverCountryDetectorService() {
        initialize();
        this.mSystemReady = true;
    }

    void initialize() {
        String customCountryClass = this.mContext.getString(17039909);
        if (!TextUtils.isEmpty(customCountryClass)) {
            this.mCountryDetector = loadCustomCountryDetectorIfAvailable(customCountryClass);
        }
        if (this.mCountryDetector == null) {
            Slog.d(TAG, "Using default country detector");
            this.mCountryDetector = new ComprehensiveCountryDetector(this.mContext);
        }
        this.mLocationBasedDetectorListener = new CountryListener() { // from class: com.android.server.CountryDetectorService$$ExternalSyntheticLambda1
            public final void onCountryDetected(Country country) {
                CountryDetectorService.this.m110lambda$initialize$2$comandroidserverCountryDetectorService(country);
            }
        };
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$initialize$2$com-android-server-CountryDetectorService  reason: not valid java name */
    public /* synthetic */ void m110lambda$initialize$2$comandroidserverCountryDetectorService(final Country country) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.CountryDetectorService$$ExternalSyntheticLambda2
            @Override // java.lang.Runnable
            public final void run() {
                CountryDetectorService.this.m109lambda$initialize$1$comandroidserverCountryDetectorService(country);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$setCountryListener$3$com-android-server-CountryDetectorService  reason: not valid java name */
    public /* synthetic */ void m111xa7d81a9(CountryListener listener) {
        this.mCountryDetector.setCountryListener(listener);
    }

    protected void setCountryListener(final CountryListener listener) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.CountryDetectorService$$ExternalSyntheticLambda3
            @Override // java.lang.Runnable
            public final void run() {
                CountryDetectorService.this.m111xa7d81a9(listener);
            }
        });
    }

    CountryDetectorBase getCountryDetector() {
        return this.mCountryDetector;
    }

    boolean isSystemReady() {
        return this.mSystemReady;
    }

    private CountryDetectorBase loadCustomCountryDetectorIfAvailable(String customCountryClass) {
        Slog.d(TAG, "Using custom country detector class: " + customCountryClass);
        try {
            CountryDetectorBase customCountryDetector = (CountryDetectorBase) Class.forName(customCountryClass).asSubclass(CountryDetectorBase.class).getConstructor(Context.class).newInstance(this.mContext);
            return customCountryDetector;
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException e) {
            Slog.e(TAG, "Could not instantiate the custom country detector class");
            return null;
        }
    }

    protected void dump(FileDescriptor fd, PrintWriter fout, String[] args) {
        DumpUtils.checkDumpPermission(this.mContext, TAG, fout);
    }
}
