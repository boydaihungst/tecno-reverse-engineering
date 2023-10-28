package android.nfc;

import android.app.Activity;
import android.app.Application;
import android.content.ContentProvider;
import android.inputmethodservice.navigationbar.NavigationBarInflaterView;
import android.net.Uri;
import android.nfc.IAppCallback;
import android.nfc.NfcAdapter;
import android.os.Binder;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
/* loaded from: classes2.dex */
public final class NfcActivityManager extends IAppCallback.Stub implements Application.ActivityLifecycleCallbacks {
    static final Boolean DBG = false;
    static final String TAG = "NFC";
    final NfcAdapter mAdapter;
    final List<NfcActivityState> mActivities = new LinkedList();
    final List<NfcApplicationState> mApps = new ArrayList(1);

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public class NfcApplicationState {
        final Application app;
        int refCount = 0;

        public NfcApplicationState(Application app) {
            this.app = app;
        }

        public void register() {
            int i = this.refCount + 1;
            this.refCount = i;
            if (i == 1) {
                this.app.registerActivityLifecycleCallbacks(NfcActivityManager.this);
            }
        }

        public void unregister() {
            int i = this.refCount - 1;
            this.refCount = i;
            if (i == 0) {
                this.app.unregisterActivityLifecycleCallbacks(NfcActivityManager.this);
            } else if (i < 0) {
                Log.e(NfcActivityManager.TAG, "-ve refcount for " + this.app);
            }
        }
    }

    NfcApplicationState findAppState(Application app) {
        for (NfcApplicationState appState : this.mApps) {
            if (appState.app == app) {
                return appState;
            }
        }
        return null;
    }

    void registerApplication(Application app) {
        NfcApplicationState appState = findAppState(app);
        if (appState == null) {
            appState = new NfcApplicationState(app);
            this.mApps.add(appState);
        }
        appState.register();
    }

    void unregisterApplication(Application app) {
        NfcApplicationState appState = findAppState(app);
        if (appState == null) {
            Log.e(TAG, "app was not registered " + app);
        } else {
            appState.unregister();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public class NfcActivityState {
        Activity activity;
        boolean resumed;
        Binder token;
        NdefMessage ndefMessage = null;
        NfcAdapter.CreateNdefMessageCallback ndefMessageCallback = null;
        NfcAdapter.OnNdefPushCompleteCallback onNdefPushCompleteCallback = null;
        NfcAdapter.CreateBeamUrisCallback uriCallback = null;
        Uri[] uris = null;
        int flags = 0;
        int readerModeFlags = 0;
        NfcAdapter.ReaderCallback readerCallback = null;
        Bundle readerModeExtras = null;

        public NfcActivityState(Activity activity) {
            this.resumed = false;
            if (activity.getWindow().isDestroyed()) {
                throw new IllegalStateException("activity is already destroyed");
            }
            this.resumed = activity.isResumed();
            this.activity = activity;
            this.token = new Binder();
            NfcActivityManager.this.registerApplication(activity.getApplication());
        }

        public void destroy() {
            NfcActivityManager.this.unregisterApplication(this.activity.getApplication());
            this.resumed = false;
            this.activity = null;
            this.ndefMessage = null;
            this.ndefMessageCallback = null;
            this.onNdefPushCompleteCallback = null;
            this.uriCallback = null;
            this.uris = null;
            this.readerModeFlags = 0;
            this.token = null;
        }

        public String toString() {
            StringBuilder s = new StringBuilder(NavigationBarInflaterView.SIZE_MOD_START).append(" ");
            s.append(this.ndefMessage).append(" ").append(this.ndefMessageCallback).append(" ");
            s.append(this.uriCallback).append(" ");
            Uri[] uriArr = this.uris;
            if (uriArr != null) {
                for (Uri uri : uriArr) {
                    s.append(this.onNdefPushCompleteCallback).append(" ").append(uri).append(NavigationBarInflaterView.SIZE_MOD_END);
                }
            }
            return s.toString();
        }
    }

    synchronized NfcActivityState findActivityState(Activity activity) {
        for (NfcActivityState state : this.mActivities) {
            if (state.activity == activity) {
                return state;
            }
        }
        return null;
    }

    synchronized NfcActivityState getActivityState(Activity activity) {
        NfcActivityState state;
        state = findActivityState(activity);
        if (state == null) {
            state = new NfcActivityState(activity);
            this.mActivities.add(state);
        }
        return state;
    }

    synchronized NfcActivityState findResumedActivityState() {
        for (NfcActivityState state : this.mActivities) {
            if (state.resumed) {
                return state;
            }
        }
        return null;
    }

    synchronized void destroyActivityState(Activity activity) {
        NfcActivityState activityState = findActivityState(activity);
        if (activityState != null) {
            activityState.destroy();
            this.mActivities.remove(activityState);
        }
    }

    public NfcActivityManager(NfcAdapter adapter) {
        this.mAdapter = adapter;
    }

    public void enableReaderMode(Activity activity, NfcAdapter.ReaderCallback callback, int flags, Bundle extras) {
        Binder token;
        boolean isResumed;
        synchronized (this) {
            NfcActivityState state = getActivityState(activity);
            state.readerCallback = callback;
            state.readerModeFlags = flags;
            state.readerModeExtras = extras;
            token = state.token;
            isResumed = state.resumed;
        }
        if (isResumed) {
            setReaderMode(token, flags, extras);
        }
    }

    public void disableReaderMode(Activity activity) {
        Binder token;
        boolean isResumed;
        synchronized (this) {
            NfcActivityState state = getActivityState(activity);
            state.readerCallback = null;
            state.readerModeFlags = 0;
            state.readerModeExtras = null;
            token = state.token;
            isResumed = state.resumed;
        }
        if (isResumed) {
            setReaderMode(token, 0, null);
        }
    }

    public void setReaderMode(Binder token, int flags, Bundle extras) {
        if (DBG.booleanValue()) {
            Log.d(TAG, "Setting reader mode");
        }
        try {
            NfcAdapter.sService.setReaderMode(token, this, flags, extras);
        } catch (RemoteException e) {
            this.mAdapter.attemptDeadServiceRecovery(e);
        }
    }

    public void setNdefPushContentUri(Activity activity, Uri[] uris) {
        boolean isResumed;
        synchronized (this) {
            NfcActivityState state = getActivityState(activity);
            state.uris = uris;
            isResumed = state.resumed;
        }
        if (isResumed) {
            requestNfcServiceCallback();
        } else {
            verifyNfcPermission();
        }
    }

    public void setNdefPushContentUriCallback(Activity activity, NfcAdapter.CreateBeamUrisCallback callback) {
        boolean isResumed;
        synchronized (this) {
            NfcActivityState state = getActivityState(activity);
            state.uriCallback = callback;
            isResumed = state.resumed;
        }
        if (isResumed) {
            requestNfcServiceCallback();
        } else {
            verifyNfcPermission();
        }
    }

    public void setNdefPushMessage(Activity activity, NdefMessage message, int flags) {
        boolean isResumed;
        synchronized (this) {
            NfcActivityState state = getActivityState(activity);
            state.ndefMessage = message;
            state.flags = flags;
            isResumed = state.resumed;
        }
        if (isResumed) {
            requestNfcServiceCallback();
        } else {
            verifyNfcPermission();
        }
    }

    public void setNdefPushMessageCallback(Activity activity, NfcAdapter.CreateNdefMessageCallback callback, int flags) {
        boolean isResumed;
        synchronized (this) {
            NfcActivityState state = getActivityState(activity);
            state.ndefMessageCallback = callback;
            state.flags = flags;
            isResumed = state.resumed;
        }
        if (isResumed) {
            requestNfcServiceCallback();
        } else {
            verifyNfcPermission();
        }
    }

    public void setOnNdefPushCompleteCallback(Activity activity, NfcAdapter.OnNdefPushCompleteCallback callback) {
        boolean isResumed;
        synchronized (this) {
            NfcActivityState state = getActivityState(activity);
            state.onNdefPushCompleteCallback = callback;
            isResumed = state.resumed;
        }
        if (isResumed) {
            requestNfcServiceCallback();
        } else {
            verifyNfcPermission();
        }
    }

    void requestNfcServiceCallback() {
        try {
            NfcAdapter.sService.setAppCallback(this);
        } catch (RemoteException e) {
            this.mAdapter.attemptDeadServiceRecovery(e);
        }
    }

    void verifyNfcPermission() {
        try {
            NfcAdapter.sService.verifyNfcPermission();
        } catch (RemoteException e) {
            this.mAdapter.attemptDeadServiceRecovery(e);
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [412=4] */
    @Override // android.nfc.IAppCallback
    public BeamShareData createBeamShareData(byte peerLlcpVersion) {
        NfcEvent event;
        NfcEvent event2 = new NfcEvent(this.mAdapter, peerLlcpVersion);
        synchronized (this) {
            try {
                NfcActivityState state = findResumedActivityState();
                if (state == null) {
                    try {
                        return null;
                    } catch (Throwable th) {
                        th = th;
                        while (true) {
                            try {
                                break;
                            } catch (Throwable th2) {
                                th = th2;
                            }
                        }
                        throw th;
                    }
                }
                NfcAdapter.CreateNdefMessageCallback ndefCallback = state.ndefMessageCallback;
                NfcAdapter.CreateBeamUrisCallback urisCallback = state.uriCallback;
                NdefMessage message = state.ndefMessage;
                Uri[] uris = state.uris;
                int flags = state.flags;
                Activity activity = state.activity;
                long ident = Binder.clearCallingIdentity();
                if (ndefCallback != null) {
                    try {
                        message = ndefCallback.createNdefMessage(event2);
                    } catch (Throwable th3) {
                        th = th3;
                        Binder.restoreCallingIdentity(ident);
                        throw th;
                    }
                }
                if (urisCallback != null) {
                    try {
                        uris = urisCallback.createBeamUris(event2);
                        if (uris != null) {
                            ArrayList<Uri> validUris = new ArrayList<>();
                            int length = uris.length;
                            int i = 0;
                            while (i < length) {
                                Uri uri = uris[i];
                                if (uri == null) {
                                    event = event2;
                                    try {
                                        Log.e(TAG, "Uri not allowed to be null.");
                                    } catch (Throwable th4) {
                                        th = th4;
                                        Binder.restoreCallingIdentity(ident);
                                        throw th;
                                    }
                                } else {
                                    event = event2;
                                    String scheme = uri.getScheme();
                                    if (scheme != null && (scheme.equalsIgnoreCase("file") || scheme.equalsIgnoreCase("content"))) {
                                        validUris.add(ContentProvider.maybeAddUserId(uri, activity.getUserId()));
                                    }
                                    Log.e(TAG, "Uri needs to have either scheme file or scheme content");
                                }
                                i++;
                                event2 = event;
                            }
                            uris = (Uri[]) validUris.toArray(new Uri[validUris.size()]);
                        }
                    } catch (Throwable th5) {
                        th = th5;
                        Binder.restoreCallingIdentity(ident);
                        throw th;
                    }
                }
                if (uris != null && uris.length > 0) {
                    for (Uri uri2 : uris) {
                        activity.grantUriPermission("com.android.nfc", uri2, 1);
                    }
                }
                Binder.restoreCallingIdentity(ident);
                return new BeamShareData(message, uris, activity.getUser(), flags);
            } catch (Throwable th6) {
                th = th6;
            }
        }
    }

    @Override // android.nfc.IAppCallback
    public void onNdefPushComplete(byte peerLlcpVersion) {
        synchronized (this) {
            NfcActivityState state = findResumedActivityState();
            if (state == null) {
                return;
            }
            NfcAdapter.OnNdefPushCompleteCallback callback = state.onNdefPushCompleteCallback;
            NfcEvent event = new NfcEvent(this.mAdapter, peerLlcpVersion);
            if (callback != null) {
                callback.onNdefPushComplete(event);
            }
        }
    }

    @Override // android.nfc.IAppCallback
    public void onTagDiscovered(Tag tag) throws RemoteException {
        synchronized (this) {
            NfcActivityState state = findResumedActivityState();
            if (state == null) {
                return;
            }
            NfcAdapter.ReaderCallback callback = state.readerCallback;
            if (callback != null) {
                callback.onTagDiscovered(tag);
            }
        }
    }

    @Override // android.app.Application.ActivityLifecycleCallbacks
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
    }

    @Override // android.app.Application.ActivityLifecycleCallbacks
    public void onActivityStarted(Activity activity) {
    }

    @Override // android.app.Application.ActivityLifecycleCallbacks
    public void onActivityResumed(Activity activity) {
        synchronized (this) {
            NfcActivityState state = findActivityState(activity);
            if (DBG.booleanValue()) {
                Log.d(TAG, "onResume() for " + activity + " " + state);
            }
            if (state == null) {
                return;
            }
            state.resumed = true;
            Binder token = state.token;
            int readerModeFlags = state.readerModeFlags;
            Bundle readerModeExtras = state.readerModeExtras;
            if (readerModeFlags != 0) {
                setReaderMode(token, readerModeFlags, readerModeExtras);
            }
            requestNfcServiceCallback();
        }
    }

    @Override // android.app.Application.ActivityLifecycleCallbacks
    public void onActivityPaused(Activity activity) {
        synchronized (this) {
            NfcActivityState state = findActivityState(activity);
            if (DBG.booleanValue()) {
                Log.d(TAG, "onPause() for " + activity + " " + state);
            }
            if (state == null) {
                return;
            }
            state.resumed = false;
            Binder token = state.token;
            boolean readerModeFlagsSet = state.readerModeFlags != 0;
            if (readerModeFlagsSet) {
                setReaderMode(token, 0, null);
            }
        }
    }

    @Override // android.app.Application.ActivityLifecycleCallbacks
    public void onActivityStopped(Activity activity) {
    }

    @Override // android.app.Application.ActivityLifecycleCallbacks
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
    }

    @Override // android.app.Application.ActivityLifecycleCallbacks
    public void onActivityDestroyed(Activity activity) {
        synchronized (this) {
            NfcActivityState state = findActivityState(activity);
            if (DBG.booleanValue()) {
                Log.d(TAG, "onDestroy() for " + activity + " " + state);
            }
            if (state != null) {
                destroyActivityState(activity);
            }
        }
    }
}
