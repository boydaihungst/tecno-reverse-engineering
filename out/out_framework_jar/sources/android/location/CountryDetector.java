package android.location;

import android.location.ICountryListener;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.util.Log;
import java.util.HashMap;
/* loaded from: classes2.dex */
public class CountryDetector {
    private static final String TAG = "CountryDetector";
    private final HashMap<CountryListener, ListenerTransport> mListeners = new HashMap<>();
    private final ICountryDetector mService;

    /* loaded from: classes2.dex */
    private static final class ListenerTransport extends ICountryListener.Stub {
        private final Handler mHandler;
        private final CountryListener mListener;

        public ListenerTransport(CountryListener listener, Looper looper) {
            this.mListener = listener;
            if (looper != null) {
                this.mHandler = new Handler(looper);
            } else {
                this.mHandler = new Handler();
            }
        }

        @Override // android.location.ICountryListener
        public void onCountryDetected(final Country country) {
            this.mHandler.post(new Runnable() { // from class: android.location.CountryDetector.ListenerTransport.1
                @Override // java.lang.Runnable
                public void run() {
                    ListenerTransport.this.mListener.onCountryDetected(country);
                }
            });
        }
    }

    public CountryDetector(ICountryDetector service) {
        this.mService = service;
    }

    public Country detectCountry() {
        try {
            return this.mService.detectCountry();
        } catch (RemoteException e) {
            Log.e(TAG, "detectCountry: RemoteException", e);
            return null;
        }
    }

    public void addCountryListener(CountryListener listener, Looper looper) {
        synchronized (this.mListeners) {
            if (!this.mListeners.containsKey(listener)) {
                ListenerTransport transport = new ListenerTransport(listener, looper);
                try {
                    this.mService.addCountryListener(transport);
                    this.mListeners.put(listener, transport);
                } catch (RemoteException e) {
                    Log.e(TAG, "addCountryListener: RemoteException", e);
                }
            }
        }
    }

    public void removeCountryListener(CountryListener listener) {
        synchronized (this.mListeners) {
            ListenerTransport transport = this.mListeners.get(listener);
            if (transport != null) {
                try {
                    this.mListeners.remove(listener);
                    this.mService.removeCountryListener(transport);
                } catch (RemoteException e) {
                    Log.e(TAG, "removeCountryListener: RemoteException", e);
                }
            }
        }
    }
}
