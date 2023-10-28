package android.nfc;

import android.annotation.SystemApi;
import android.app.Activity;
import android.app.ActivityThread;
import android.app.OnActivityPausedListener;
import android.app.PendingIntent;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.nfc.INfcAdapter;
import android.nfc.INfcUnlockHandler;
import android.nfc.ITagRemovedCallback;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executor;
/* loaded from: classes2.dex */
public final class NfcAdapter {
    public static final String ACTION_ADAPTER_STATE_CHANGED = "android.nfc.action.ADAPTER_STATE_CHANGED";
    public static final String ACTION_HANDOVER_TRANSFER_DONE = "android.nfc.action.HANDOVER_TRANSFER_DONE";
    public static final String ACTION_HANDOVER_TRANSFER_STARTED = "android.nfc.action.HANDOVER_TRANSFER_STARTED";
    public static final String ACTION_NDEF_DISCOVERED = "android.nfc.action.NDEF_DISCOVERED";
    public static final String ACTION_PREFERRED_PAYMENT_CHANGED = "android.nfc.action.PREFERRED_PAYMENT_CHANGED";
    public static final String ACTION_REQUIRE_UNLOCK_FOR_NFC = "android.nfc.action.REQUIRE_UNLOCK_FOR_NFC";
    public static final String ACTION_TAG_DISCOVERED = "android.nfc.action.TAG_DISCOVERED";
    public static final String ACTION_TAG_LEFT_FIELD = "android.nfc.action.TAG_LOST";
    public static final String ACTION_TECH_DISCOVERED = "android.nfc.action.TECH_DISCOVERED";
    public static final String ACTION_TRANSACTION_DETECTED = "android.nfc.action.TRANSACTION_DETECTED";
    public static final String EXTRA_ADAPTER_STATE = "android.nfc.extra.ADAPTER_STATE";
    public static final String EXTRA_AID = "android.nfc.extra.AID";
    public static final String EXTRA_DATA = "android.nfc.extra.DATA";
    public static final String EXTRA_HANDOVER_TRANSFER_STATUS = "android.nfc.extra.HANDOVER_TRANSFER_STATUS";
    public static final String EXTRA_HANDOVER_TRANSFER_URI = "android.nfc.extra.HANDOVER_TRANSFER_URI";
    public static final String EXTRA_ID = "android.nfc.extra.ID";
    public static final String EXTRA_NDEF_MESSAGES = "android.nfc.extra.NDEF_MESSAGES";
    public static final String EXTRA_PREFERRED_PAYMENT_CHANGED_REASON = "android.nfc.extra.PREFERRED_PAYMENT_CHANGED_REASON";
    public static final String EXTRA_READER_PRESENCE_CHECK_DELAY = "presence";
    public static final String EXTRA_SECURE_ELEMENT_NAME = "android.nfc.extra.SECURE_ELEMENT_NAME";
    public static final String EXTRA_TAG = "android.nfc.extra.TAG";
    @SystemApi
    public static final int FLAG_NDEF_PUSH_NO_CONFIRM = 1;
    public static final int FLAG_READER_NFC_A = 1;
    public static final int FLAG_READER_NFC_B = 2;
    public static final int FLAG_READER_NFC_BARCODE = 16;
    public static final int FLAG_READER_NFC_F = 4;
    public static final int FLAG_READER_NFC_V = 8;
    public static final int FLAG_READER_NO_PLATFORM_SOUNDS = 256;
    public static final int FLAG_READER_SKIP_NDEF_CHECK = 128;
    public static final int HANDOVER_TRANSFER_STATUS_FAILURE = 1;
    public static final int HANDOVER_TRANSFER_STATUS_SUCCESS = 0;
    public static final int PREFERRED_PAYMENT_CHANGED = 2;
    public static final int PREFERRED_PAYMENT_LOADED = 1;
    public static final int PREFERRED_PAYMENT_UPDATED = 3;
    public static final int STATE_OFF = 1;
    public static final int STATE_ON = 3;
    public static final int STATE_TURNING_OFF = 4;
    public static final int STATE_TURNING_ON = 2;
    static final String TAG = "NFC";
    static INfcCardEmulation sCardEmulationService;
    static boolean sHasBeamFeature;
    static boolean sHasNfcFeature;
    static boolean sIsInitialized = false;
    static HashMap<Context, NfcAdapter> sNfcAdapters = new HashMap<>();
    static INfcFCardEmulation sNfcFCardEmulationService;
    static NfcAdapter sNullContextNfcAdapter;
    static INfcAdapter sService;
    static INfcTag sTagService;
    final Context mContext;
    OnActivityPausedListener mForegroundDispatchListener = new OnActivityPausedListener() { // from class: android.nfc.NfcAdapter.1
        @Override // android.app.OnActivityPausedListener
        public void onPaused(Activity activity) {
            NfcAdapter.this.disableForegroundDispatchInternal(activity, true);
        }
    };
    final NfcActivityManager mNfcActivityManager = new NfcActivityManager(this);
    final HashMap<NfcUnlockHandler, INfcUnlockHandler> mNfcUnlockHandlers = new HashMap<>();
    ITagRemovedCallback mTagRemovedListener = null;
    final Object mLock = new Object();
    private final NfcControllerAlwaysOnListener mControllerAlwaysOnListener = new NfcControllerAlwaysOnListener(getService());

    @SystemApi
    /* loaded from: classes2.dex */
    public interface ControllerAlwaysOnListener {
        void onControllerAlwaysOnChanged(boolean z);
    }

    @Deprecated
    /* loaded from: classes2.dex */
    public interface CreateBeamUrisCallback {
        Uri[] createBeamUris(NfcEvent nfcEvent);
    }

    @Deprecated
    /* loaded from: classes2.dex */
    public interface CreateNdefMessageCallback {
        NdefMessage createNdefMessage(NfcEvent nfcEvent);
    }

    @SystemApi
    /* loaded from: classes2.dex */
    public interface NfcUnlockHandler {
        boolean onUnlockAttempted(Tag tag);
    }

    @Deprecated
    /* loaded from: classes2.dex */
    public interface OnNdefPushCompleteCallback {
        void onNdefPushComplete(NfcEvent nfcEvent);
    }

    /* loaded from: classes2.dex */
    public interface OnTagRemovedListener {
        void onTagRemoved();
    }

    /* loaded from: classes2.dex */
    public interface ReaderCallback {
        void onTagDiscovered(Tag tag);
    }

    private static boolean hasBeamFeature() {
        IPackageManager pm = ActivityThread.getPackageManager();
        if (pm == null) {
            Log.e(TAG, "Cannot get package manager, assuming no Android Beam feature");
            return false;
        }
        try {
            return pm.hasSystemFeature(PackageManager.FEATURE_NFC_BEAM, 0);
        } catch (RemoteException e) {
            Log.e(TAG, "Package manager query failed, assuming no Android Beam feature", e);
            return false;
        }
    }

    private static boolean hasNfcFeature() {
        IPackageManager pm = ActivityThread.getPackageManager();
        if (pm == null) {
            Log.e(TAG, "Cannot get package manager, assuming no NFC feature");
            return false;
        }
        try {
            return pm.hasSystemFeature(PackageManager.FEATURE_NFC, 0);
        } catch (RemoteException e) {
            Log.e(TAG, "Package manager query failed, assuming no NFC feature", e);
            return false;
        }
    }

    private static boolean hasNfcHceFeature() {
        IPackageManager pm = ActivityThread.getPackageManager();
        if (pm == null) {
            Log.e(TAG, "Cannot get package manager, assuming no NFC feature");
            return false;
        }
        try {
            if (!pm.hasSystemFeature("android.hardware.nfc.hce", 0)) {
                if (!pm.hasSystemFeature(PackageManager.FEATURE_NFC_HOST_CARD_EMULATION_NFCF, 0)) {
                    return false;
                }
            }
            return true;
        } catch (RemoteException e) {
            Log.e(TAG, "Package manager query failed, assuming no NFC feature", e);
            return false;
        }
    }

    public List<String> getSupportedOffHostSecureElements() {
        List<String> offHostSE = new ArrayList<>();
        IPackageManager pm = ActivityThread.getPackageManager();
        if (pm == null) {
            Log.e(TAG, "Cannot get package manager, assuming no off-host CE feature");
            return offHostSE;
        }
        try {
            if (pm.hasSystemFeature(PackageManager.FEATURE_NFC_OFF_HOST_CARD_EMULATION_UICC, 0)) {
                offHostSE.add("SIM");
            }
            if (pm.hasSystemFeature(PackageManager.FEATURE_NFC_OFF_HOST_CARD_EMULATION_ESE, 0)) {
                offHostSE.add("eSE");
            }
            return offHostSE;
        } catch (RemoteException e) {
            Log.e(TAG, "Package manager query failed, assuming no off-host CE feature", e);
            offHostSE.clear();
            return offHostSE;
        }
    }

    public static synchronized NfcAdapter getNfcAdapter(Context context) {
        synchronized (NfcAdapter.class) {
            if (!sIsInitialized) {
                sHasNfcFeature = hasNfcFeature();
                sHasBeamFeature = hasBeamFeature();
                boolean hasHceFeature = hasNfcHceFeature();
                if (!sHasNfcFeature && !hasHceFeature) {
                    Log.v(TAG, "this device does not have NFC support");
                    throw new UnsupportedOperationException();
                }
                INfcAdapter serviceInterface = getServiceInterface();
                sService = serviceInterface;
                if (serviceInterface == null) {
                    Log.e(TAG, "could not retrieve NFC service");
                    throw new UnsupportedOperationException();
                }
                if (sHasNfcFeature) {
                    try {
                        sTagService = serviceInterface.getNfcTagInterface();
                    } catch (RemoteException e) {
                        Log.e(TAG, "could not retrieve NFC Tag service");
                        throw new UnsupportedOperationException();
                    }
                }
                if (hasHceFeature) {
                    try {
                        sNfcFCardEmulationService = sService.getNfcFCardEmulationInterface();
                        try {
                            sCardEmulationService = sService.getNfcCardEmulationInterface();
                        } catch (RemoteException e2) {
                            Log.e(TAG, "could not retrieve card emulation service");
                            throw new UnsupportedOperationException();
                        }
                    } catch (RemoteException e3) {
                        Log.e(TAG, "could not retrieve NFC-F card emulation service");
                        throw new UnsupportedOperationException();
                    }
                }
                sIsInitialized = true;
            }
            if (context == null) {
                if (sNullContextNfcAdapter == null) {
                    sNullContextNfcAdapter = new NfcAdapter(null);
                }
                return sNullContextNfcAdapter;
            }
            NfcAdapter adapter = sNfcAdapters.get(context);
            if (adapter == null) {
                adapter = new NfcAdapter(context);
                sNfcAdapters.put(context, adapter);
            }
            return adapter;
        }
    }

    private static INfcAdapter getServiceInterface() {
        IBinder b = ServiceManager.getService("nfc");
        if (b == null) {
            return null;
        }
        return INfcAdapter.Stub.asInterface(b);
    }

    public static NfcAdapter getDefaultAdapter(Context context) {
        NfcManager manager;
        if (context == null) {
            throw new IllegalArgumentException("context cannot be null");
        }
        Context context2 = context.getApplicationContext();
        if (context2 == null) {
            throw new IllegalArgumentException("context not associated with any application (using a mock context?)");
        }
        if (getServiceInterface() == null || (manager = (NfcManager) context2.getSystemService("nfc")) == null) {
            return null;
        }
        return manager.getDefaultAdapter();
    }

    @Deprecated
    public static NfcAdapter getDefaultAdapter() {
        Log.w(TAG, "WARNING: NfcAdapter.getDefaultAdapter() is deprecated, use NfcAdapter.getDefaultAdapter(Context) instead", new Exception());
        return getNfcAdapter(null);
    }

    NfcAdapter(Context context) {
        this.mContext = context;
    }

    public Context getContext() {
        return this.mContext;
    }

    public INfcAdapter getService() {
        isEnabled();
        return sService;
    }

    public INfcTag getTagService() {
        isEnabled();
        return sTagService;
    }

    public INfcCardEmulation getCardEmulationService() {
        isEnabled();
        return sCardEmulationService;
    }

    public INfcFCardEmulation getNfcFCardEmulationService() {
        isEnabled();
        return sNfcFCardEmulationService;
    }

    public INfcDta getNfcDtaInterface() {
        Context context = this.mContext;
        if (context == null) {
            throw new UnsupportedOperationException("You need a context on NfcAdapter to use the  NFC extras APIs");
        }
        try {
            return sService.getNfcDtaInterface(context.getPackageName());
        } catch (RemoteException e) {
            attemptDeadServiceRecovery(e);
            INfcAdapter iNfcAdapter = sService;
            if (iNfcAdapter == null) {
                Log.e(TAG, "Failed to recover NFC Service.");
                return null;
            }
            try {
                return iNfcAdapter.getNfcDtaInterface(this.mContext.getPackageName());
            } catch (RemoteException e2) {
                Log.e(TAG, "Failed to recover NFC Service.");
                return null;
            }
        }
    }

    public void attemptDeadServiceRecovery(Exception e) {
        Log.e(TAG, "NFC service dead - attempting to recover", e);
        INfcAdapter service = getServiceInterface();
        if (service == null) {
            Log.e(TAG, "could not retrieve NFC service during service recovery");
            return;
        }
        sService = service;
        try {
            sTagService = service.getNfcTagInterface();
            try {
                sCardEmulationService = service.getNfcCardEmulationInterface();
            } catch (RemoteException e2) {
                Log.e(TAG, "could not retrieve NFC card emulation service during service recovery");
            }
            try {
                sNfcFCardEmulationService = service.getNfcFCardEmulationInterface();
            } catch (RemoteException e3) {
                Log.e(TAG, "could not retrieve NFC-F card emulation service during service recovery");
            }
        } catch (RemoteException e4) {
            Log.e(TAG, "could not retrieve NFC tag service during service recovery");
        }
    }

    public boolean isEnabled() {
        try {
            return sService.getState() == 3;
        } catch (RemoteException e) {
            attemptDeadServiceRecovery(e);
            INfcAdapter iNfcAdapter = sService;
            if (iNfcAdapter == null) {
                Log.e(TAG, "Failed to recover NFC Service.");
                return false;
            }
            try {
                return iNfcAdapter.getState() == 3;
            } catch (RemoteException e2) {
                Log.e(TAG, "Failed to recover NFC Service.");
                return false;
            }
        }
    }

    public int getAdapterState() {
        try {
            return sService.getState();
        } catch (RemoteException e) {
            attemptDeadServiceRecovery(e);
            INfcAdapter iNfcAdapter = sService;
            if (iNfcAdapter == null) {
                Log.e(TAG, "Failed to recover NFC Service.");
                return 1;
            }
            try {
                return iNfcAdapter.getState();
            } catch (RemoteException e2) {
                Log.e(TAG, "Failed to recover NFC Service.");
                return 1;
            }
        }
    }

    @SystemApi
    public boolean enable() {
        try {
            return sService.enable();
        } catch (RemoteException e) {
            attemptDeadServiceRecovery(e);
            INfcAdapter iNfcAdapter = sService;
            if (iNfcAdapter == null) {
                Log.e(TAG, "Failed to recover NFC Service.");
                return false;
            }
            try {
                return iNfcAdapter.enable();
            } catch (RemoteException e2) {
                Log.e(TAG, "Failed to recover NFC Service.");
                return false;
            }
        }
    }

    @SystemApi
    public boolean disable() {
        try {
            return sService.disable(true);
        } catch (RemoteException e) {
            attemptDeadServiceRecovery(e);
            INfcAdapter iNfcAdapter = sService;
            if (iNfcAdapter == null) {
                Log.e(TAG, "Failed to recover NFC Service.");
                return false;
            }
            try {
                return iNfcAdapter.disable(true);
            } catch (RemoteException e2) {
                Log.e(TAG, "Failed to recover NFC Service.");
                return false;
            }
        }
    }

    @SystemApi
    public boolean disable(boolean persist) {
        try {
            return sService.disable(persist);
        } catch (RemoteException e) {
            attemptDeadServiceRecovery(e);
            INfcAdapter iNfcAdapter = sService;
            if (iNfcAdapter == null) {
                Log.e(TAG, "Failed to recover NFC Service.");
                return false;
            }
            try {
                return iNfcAdapter.disable(persist);
            } catch (RemoteException e2) {
                Log.e(TAG, "Failed to recover NFC Service.");
                return false;
            }
        }
    }

    public void pausePolling(int timeoutInMs) {
        try {
            sService.pausePolling(timeoutInMs);
        } catch (RemoteException e) {
            attemptDeadServiceRecovery(e);
        }
    }

    public void resumePolling() {
        try {
            sService.resumePolling();
        } catch (RemoteException e) {
            attemptDeadServiceRecovery(e);
        }
    }

    @Deprecated
    public void setBeamPushUris(Uri[] uris, Activity activity) {
        synchronized (NfcAdapter.class) {
            if (!sHasNfcFeature) {
                throw new UnsupportedOperationException();
            }
            if (sHasBeamFeature) {
                if (activity == null) {
                    throw new NullPointerException("activity cannot be null");
                }
                if (uris != null) {
                    for (Uri uri : uris) {
                        if (uri == null) {
                            throw new NullPointerException("Uri not allowed to be null");
                        }
                        String scheme = uri.getScheme();
                        if (scheme == null || (!scheme.equalsIgnoreCase("file") && !scheme.equalsIgnoreCase("content"))) {
                            throw new IllegalArgumentException("URI needs to have either scheme file or scheme content");
                        }
                    }
                }
                this.mNfcActivityManager.setNdefPushContentUri(activity, uris);
            }
        }
    }

    @Deprecated
    public void setBeamPushUrisCallback(CreateBeamUrisCallback callback, Activity activity) {
        synchronized (NfcAdapter.class) {
            if (!sHasNfcFeature) {
                throw new UnsupportedOperationException();
            }
            if (sHasBeamFeature) {
                if (activity == null) {
                    throw new NullPointerException("activity cannot be null");
                }
                this.mNfcActivityManager.setNdefPushContentUriCallback(activity, callback);
            }
        }
    }

    @Deprecated
    public void setNdefPushMessage(NdefMessage message, Activity activity, Activity... activities) {
        synchronized (NfcAdapter.class) {
            if (!sHasNfcFeature) {
                throw new UnsupportedOperationException();
            }
            if (sHasBeamFeature) {
                int targetSdkVersion = getSdkVersion();
                try {
                    if (activity == null) {
                        throw new NullPointerException("activity cannot be null");
                    }
                    this.mNfcActivityManager.setNdefPushMessage(activity, message, 0);
                    for (Activity a : activities) {
                        if (a == null) {
                            throw new NullPointerException("activities cannot contain null");
                        }
                        this.mNfcActivityManager.setNdefPushMessage(a, message, 0);
                    }
                } catch (IllegalStateException e) {
                    if (targetSdkVersion < 16) {
                        Log.e(TAG, "Cannot call API with Activity that has already been destroyed", e);
                        return;
                    }
                    throw e;
                }
            }
        }
    }

    @SystemApi
    public void setNdefPushMessage(NdefMessage message, Activity activity, int flags) {
        synchronized (NfcAdapter.class) {
            if (!sHasNfcFeature) {
                throw new UnsupportedOperationException();
            }
        }
        if (activity == null) {
            throw new NullPointerException("activity cannot be null");
        }
        this.mNfcActivityManager.setNdefPushMessage(activity, message, flags);
    }

    @Deprecated
    public void setNdefPushMessageCallback(CreateNdefMessageCallback callback, Activity activity, Activity... activities) {
        synchronized (NfcAdapter.class) {
            if (!sHasNfcFeature) {
                throw new UnsupportedOperationException();
            }
            if (sHasBeamFeature) {
                int targetSdkVersion = getSdkVersion();
                try {
                    if (activity == null) {
                        throw new NullPointerException("activity cannot be null");
                    }
                    this.mNfcActivityManager.setNdefPushMessageCallback(activity, callback, 0);
                    for (Activity a : activities) {
                        if (a == null) {
                            throw new NullPointerException("activities cannot contain null");
                        }
                        this.mNfcActivityManager.setNdefPushMessageCallback(a, callback, 0);
                    }
                } catch (IllegalStateException e) {
                    if (targetSdkVersion < 16) {
                        Log.e(TAG, "Cannot call API with Activity that has already been destroyed", e);
                        return;
                    }
                    throw e;
                }
            }
        }
    }

    public void setNdefPushMessageCallback(CreateNdefMessageCallback callback, Activity activity, int flags) {
        if (activity == null) {
            throw new NullPointerException("activity cannot be null");
        }
        this.mNfcActivityManager.setNdefPushMessageCallback(activity, callback, flags);
    }

    @Deprecated
    public void setOnNdefPushCompleteCallback(OnNdefPushCompleteCallback callback, Activity activity, Activity... activities) {
        synchronized (NfcAdapter.class) {
            if (!sHasNfcFeature) {
                throw new UnsupportedOperationException();
            }
            if (sHasBeamFeature) {
                int targetSdkVersion = getSdkVersion();
                try {
                    if (activity == null) {
                        throw new NullPointerException("activity cannot be null");
                    }
                    this.mNfcActivityManager.setOnNdefPushCompleteCallback(activity, callback);
                    for (Activity a : activities) {
                        if (a == null) {
                            throw new NullPointerException("activities cannot contain null");
                        }
                        this.mNfcActivityManager.setOnNdefPushCompleteCallback(a, callback);
                    }
                } catch (IllegalStateException e) {
                    if (targetSdkVersion < 16) {
                        Log.e(TAG, "Cannot call API with Activity that has already been destroyed", e);
                        return;
                    }
                    throw e;
                }
            }
        }
    }

    public void enableForegroundDispatch(Activity activity, PendingIntent intent, IntentFilter[] filters, String[][] techLists) {
        synchronized (NfcAdapter.class) {
            if (!sHasNfcFeature) {
                throw new UnsupportedOperationException();
            }
        }
        if (activity == null || intent == null) {
            throw new NullPointerException();
        }
        if (!activity.isResumed()) {
            throw new IllegalStateException("Foreground dispatch can only be enabled when your activity is resumed");
        }
        TechListParcel parcel = null;
        if (techLists != null) {
            try {
                if (techLists.length > 0) {
                    parcel = new TechListParcel(techLists);
                }
            } catch (RemoteException e) {
                attemptDeadServiceRecovery(e);
                return;
            }
        }
        ActivityThread.currentActivityThread().registerOnActivityPausedListener(activity, this.mForegroundDispatchListener);
        sService.setForegroundDispatch(intent, filters, parcel);
    }

    public void disableForegroundDispatch(Activity activity) {
        synchronized (NfcAdapter.class) {
            if (!sHasNfcFeature) {
                throw new UnsupportedOperationException();
            }
        }
        ActivityThread.currentActivityThread().unregisterOnActivityPausedListener(activity, this.mForegroundDispatchListener);
        disableForegroundDispatchInternal(activity, false);
    }

    void disableForegroundDispatchInternal(Activity activity, boolean force) {
        try {
            sService.setForegroundDispatch(null, null, null);
            if (!force && !activity.isResumed()) {
                throw new IllegalStateException("You must disable foreground dispatching while your activity is still resumed");
            }
        } catch (RemoteException e) {
            attemptDeadServiceRecovery(e);
        }
    }

    public void enableReaderMode(Activity activity, ReaderCallback callback, int flags, Bundle extras) {
        synchronized (NfcAdapter.class) {
            if (!sHasNfcFeature) {
                throw new UnsupportedOperationException();
            }
        }
        this.mNfcActivityManager.enableReaderMode(activity, callback, flags, extras);
    }

    public void disableReaderMode(Activity activity) {
        synchronized (NfcAdapter.class) {
            if (!sHasNfcFeature) {
                throw new UnsupportedOperationException();
            }
        }
        this.mNfcActivityManager.disableReaderMode(activity);
    }

    @Deprecated
    public boolean invokeBeam(Activity activity) {
        synchronized (NfcAdapter.class) {
            if (!sHasNfcFeature) {
                throw new UnsupportedOperationException();
            }
            if (sHasBeamFeature) {
                if (activity == null) {
                    throw new NullPointerException("activity may not be null.");
                }
                enforceResumed(activity);
                try {
                    sService.invokeBeam();
                    return true;
                } catch (RemoteException e) {
                    Log.e(TAG, "invokeBeam: NFC process has died.");
                    attemptDeadServiceRecovery(e);
                    return false;
                }
            }
            return false;
        }
    }

    public boolean invokeBeam(BeamShareData shareData) {
        try {
            Log.e(TAG, "invokeBeamInternal()");
            sService.invokeBeamInternal(shareData);
            return true;
        } catch (RemoteException e) {
            Log.e(TAG, "invokeBeam: NFC process has died.");
            attemptDeadServiceRecovery(e);
            return false;
        }
    }

    @Deprecated
    public void enableForegroundNdefPush(Activity activity, NdefMessage message) {
        synchronized (NfcAdapter.class) {
            if (!sHasNfcFeature) {
                throw new UnsupportedOperationException();
            }
            if (sHasBeamFeature) {
                if (activity == null || message == null) {
                    throw new NullPointerException();
                }
                enforceResumed(activity);
                this.mNfcActivityManager.setNdefPushMessage(activity, message, 0);
            }
        }
    }

    @Deprecated
    public void disableForegroundNdefPush(Activity activity) {
        synchronized (NfcAdapter.class) {
            if (!sHasNfcFeature) {
                throw new UnsupportedOperationException();
            }
            if (sHasBeamFeature) {
                if (activity == null) {
                    throw new NullPointerException();
                }
                enforceResumed(activity);
                this.mNfcActivityManager.setNdefPushMessage(activity, null, 0);
                this.mNfcActivityManager.setNdefPushMessageCallback(activity, null, 0);
                this.mNfcActivityManager.setOnNdefPushCompleteCallback(activity, null);
            }
        }
    }

    @SystemApi
    public boolean enableSecureNfc(boolean enable) {
        if (!sHasNfcFeature) {
            throw new UnsupportedOperationException();
        }
        try {
            return sService.setNfcSecure(enable);
        } catch (RemoteException e) {
            attemptDeadServiceRecovery(e);
            INfcAdapter iNfcAdapter = sService;
            if (iNfcAdapter == null) {
                Log.e(TAG, "Failed to recover NFC Service.");
                return false;
            }
            try {
                return iNfcAdapter.setNfcSecure(enable);
            } catch (RemoteException e2) {
                Log.e(TAG, "Failed to recover NFC Service.");
                return false;
            }
        }
    }

    public boolean isSecureNfcSupported() {
        if (!sHasNfcFeature) {
            throw new UnsupportedOperationException();
        }
        try {
            return sService.deviceSupportsNfcSecure();
        } catch (RemoteException e) {
            attemptDeadServiceRecovery(e);
            INfcAdapter iNfcAdapter = sService;
            if (iNfcAdapter == null) {
                Log.e(TAG, "Failed to recover NFC Service.");
                return false;
            }
            try {
                return iNfcAdapter.deviceSupportsNfcSecure();
            } catch (RemoteException e2) {
                Log.e(TAG, "Failed to recover NFC Service.");
                return false;
            }
        }
    }

    public boolean isSecureNfcEnabled() {
        if (!sHasNfcFeature) {
            throw new UnsupportedOperationException();
        }
        try {
            return sService.isNfcSecureEnabled();
        } catch (RemoteException e) {
            attemptDeadServiceRecovery(e);
            INfcAdapter iNfcAdapter = sService;
            if (iNfcAdapter == null) {
                Log.e(TAG, "Failed to recover NFC Service.");
                return false;
            }
            try {
                return iNfcAdapter.isNfcSecureEnabled();
            } catch (RemoteException e2) {
                Log.e(TAG, "Failed to recover NFC Service.");
                return false;
            }
        }
    }

    @SystemApi
    public boolean enableNdefPush() {
        if (!sHasNfcFeature) {
            throw new UnsupportedOperationException();
        }
        try {
            return sService.enableNdefPush();
        } catch (RemoteException e) {
            attemptDeadServiceRecovery(e);
            return false;
        }
    }

    @SystemApi
    public boolean disableNdefPush() {
        synchronized (NfcAdapter.class) {
            if (!sHasNfcFeature) {
                throw new UnsupportedOperationException();
            }
        }
        try {
            return sService.disableNdefPush();
        } catch (RemoteException e) {
            attemptDeadServiceRecovery(e);
            return false;
        }
    }

    @Deprecated
    public boolean isNdefPushEnabled() {
        synchronized (NfcAdapter.class) {
            if (!sHasNfcFeature) {
                throw new UnsupportedOperationException();
            }
            if (sHasBeamFeature) {
                try {
                    return sService.isNdefPushEnabled();
                } catch (RemoteException e) {
                    attemptDeadServiceRecovery(e);
                    return false;
                }
            }
            return false;
        }
    }

    public boolean ignore(Tag tag, int debounceMs, final OnTagRemovedListener tagRemovedListener, final Handler handler) {
        ITagRemovedCallback.Stub iListener = null;
        if (tagRemovedListener != null) {
            iListener = new ITagRemovedCallback.Stub() { // from class: android.nfc.NfcAdapter.2
                @Override // android.nfc.ITagRemovedCallback
                public void onTagRemoved() throws RemoteException {
                    Handler handler2 = handler;
                    if (handler2 != null) {
                        handler2.post(new Runnable() { // from class: android.nfc.NfcAdapter.2.1
                            @Override // java.lang.Runnable
                            public void run() {
                                tagRemovedListener.onTagRemoved();
                            }
                        });
                    } else {
                        tagRemovedListener.onTagRemoved();
                    }
                    synchronized (NfcAdapter.this.mLock) {
                        NfcAdapter.this.mTagRemovedListener = null;
                    }
                }
            };
        }
        synchronized (this.mLock) {
            this.mTagRemovedListener = iListener;
        }
        try {
            return sService.ignore(tag.getServiceHandle(), debounceMs, iListener);
        } catch (RemoteException e) {
            return false;
        }
    }

    public void dispatch(Tag tag) {
        if (tag == null) {
            throw new NullPointerException("tag cannot be null");
        }
        try {
            sService.dispatch(tag);
        } catch (RemoteException e) {
            attemptDeadServiceRecovery(e);
        }
    }

    public void setP2pModes(int initiatorModes, int targetModes) {
        try {
            sService.setP2pModes(initiatorModes, targetModes);
        } catch (RemoteException e) {
            attemptDeadServiceRecovery(e);
        }
    }

    @SystemApi
    public boolean addNfcUnlockHandler(final NfcUnlockHandler unlockHandler, String[] tagTechnologies) {
        synchronized (NfcAdapter.class) {
            if (!sHasNfcFeature) {
                throw new UnsupportedOperationException();
            }
        }
        if (tagTechnologies.length == 0) {
            return false;
        }
        try {
            synchronized (this.mLock) {
                if (this.mNfcUnlockHandlers.containsKey(unlockHandler)) {
                    sService.removeNfcUnlockHandler(this.mNfcUnlockHandlers.get(unlockHandler));
                    this.mNfcUnlockHandlers.remove(unlockHandler);
                }
                INfcUnlockHandler.Stub iHandler = new INfcUnlockHandler.Stub() { // from class: android.nfc.NfcAdapter.3
                    @Override // android.nfc.INfcUnlockHandler
                    public boolean onUnlockAttempted(Tag tag) throws RemoteException {
                        return unlockHandler.onUnlockAttempted(tag);
                    }
                };
                sService.addNfcUnlockHandler(iHandler, Tag.getTechCodesFromStrings(tagTechnologies));
                this.mNfcUnlockHandlers.put(unlockHandler, iHandler);
            }
            return true;
        } catch (RemoteException e) {
            attemptDeadServiceRecovery(e);
            return false;
        } catch (IllegalArgumentException e2) {
            Log.e(TAG, "Unable to register LockscreenDispatch", e2);
            return false;
        }
    }

    @SystemApi
    public boolean removeNfcUnlockHandler(NfcUnlockHandler unlockHandler) {
        synchronized (NfcAdapter.class) {
            if (!sHasNfcFeature) {
                throw new UnsupportedOperationException();
            }
        }
        try {
            synchronized (this.mLock) {
                if (this.mNfcUnlockHandlers.containsKey(unlockHandler)) {
                    sService.removeNfcUnlockHandler(this.mNfcUnlockHandlers.remove(unlockHandler));
                }
            }
            return true;
        } catch (RemoteException e) {
            attemptDeadServiceRecovery(e);
            return false;
        }
    }

    public INfcAdapterExtras getNfcAdapterExtrasInterface() {
        Context context = this.mContext;
        if (context == null) {
            throw new UnsupportedOperationException("You need a context on NfcAdapter to use the  NFC extras APIs");
        }
        try {
            return sService.getNfcAdapterExtrasInterface(context.getPackageName());
        } catch (RemoteException e) {
            attemptDeadServiceRecovery(e);
            INfcAdapter iNfcAdapter = sService;
            if (iNfcAdapter == null) {
                Log.e(TAG, "Failed to recover NFC Service.");
                return null;
            }
            try {
                return iNfcAdapter.getNfcAdapterExtrasInterface(this.mContext.getPackageName());
            } catch (RemoteException e2) {
                Log.e(TAG, "Failed to recover NFC Service.");
                return null;
            }
        }
    }

    void enforceResumed(Activity activity) {
        if (!activity.isResumed()) {
            throw new IllegalStateException("API cannot be called while activity is paused");
        }
    }

    int getSdkVersion() {
        Context context = this.mContext;
        if (context == null) {
            return 9;
        }
        return context.getApplicationInfo().targetSdkVersion;
    }

    @SystemApi
    public boolean setControllerAlwaysOn(boolean value) {
        if (!sHasNfcFeature) {
            throw new UnsupportedOperationException();
        }
        try {
            return sService.setControllerAlwaysOn(value);
        } catch (RemoteException e) {
            attemptDeadServiceRecovery(e);
            INfcAdapter iNfcAdapter = sService;
            if (iNfcAdapter == null) {
                Log.e(TAG, "Failed to recover NFC Service.");
                return false;
            }
            try {
                return iNfcAdapter.setControllerAlwaysOn(value);
            } catch (RemoteException e2) {
                Log.e(TAG, "Failed to recover NFC Service.");
                return false;
            }
        }
    }

    @SystemApi
    public boolean isControllerAlwaysOn() {
        try {
            return sService.isControllerAlwaysOn();
        } catch (RemoteException e) {
            attemptDeadServiceRecovery(e);
            INfcAdapter iNfcAdapter = sService;
            if (iNfcAdapter == null) {
                Log.e(TAG, "Failed to recover NFC Service.");
                return false;
            }
            try {
                return iNfcAdapter.isControllerAlwaysOn();
            } catch (RemoteException e2) {
                Log.e(TAG, "Failed to recover NFC Service.");
                return false;
            }
        }
    }

    @SystemApi
    public boolean isControllerAlwaysOnSupported() {
        if (!sHasNfcFeature) {
            throw new UnsupportedOperationException();
        }
        try {
            return sService.isControllerAlwaysOnSupported();
        } catch (RemoteException e) {
            attemptDeadServiceRecovery(e);
            INfcAdapter iNfcAdapter = sService;
            if (iNfcAdapter == null) {
                Log.e(TAG, "Failed to recover NFC Service.");
                return false;
            }
            try {
                return iNfcAdapter.isControllerAlwaysOnSupported();
            } catch (RemoteException e2) {
                Log.e(TAG, "Failed to recover NFC Service.");
                return false;
            }
        }
    }

    @SystemApi
    public void registerControllerAlwaysOnListener(Executor executor, ControllerAlwaysOnListener listener) {
        this.mControllerAlwaysOnListener.register(executor, listener);
    }

    @SystemApi
    public void unregisterControllerAlwaysOnListener(ControllerAlwaysOnListener listener) {
        this.mControllerAlwaysOnListener.unregister(listener);
    }
}
