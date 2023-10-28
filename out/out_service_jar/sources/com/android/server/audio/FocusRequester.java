package com.android.server.audio;

import android.media.AudioAttributes;
import android.media.AudioFocusInfo;
import android.media.IAudioFocusDispatcher;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import com.android.server.audio.MediaFocusControl;
import java.io.PrintWriter;
import java.util.NoSuchElementException;
/* loaded from: classes.dex */
public class FocusRequester {
    private static final boolean DEBUG;
    private static final String TAG = "MediaFocusControl";
    private final AudioAttributes mAttributes;
    private final int mCallingUid;
    private final String mClientId;
    private MediaFocusControl.AudioFocusDeathHandler mDeathHandler;
    private final MediaFocusControl mFocusController;
    private IAudioFocusDispatcher mFocusDispatcher;
    private final int mFocusGainRequest;
    private final int mGrantFlags;
    private final String mPackageName;
    private final int mSdkTarget;
    private final IBinder mSourceRef;
    private int mFocusLossReceived = 0;
    private boolean mFocusLossWasNotified = true;
    boolean mFocusLossFadeLimbo = false;

    static {
        DEBUG = "eng".equals(Build.TYPE) || "userdebug".equals(Build.TYPE);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public FocusRequester(AudioAttributes aa, int focusRequest, int grantFlags, IAudioFocusDispatcher afl, IBinder source, String id, MediaFocusControl.AudioFocusDeathHandler hdlr, String pn, int uid, MediaFocusControl ctlr, int sdk) {
        this.mAttributes = aa;
        this.mFocusDispatcher = afl;
        this.mSourceRef = source;
        this.mClientId = id;
        this.mDeathHandler = hdlr;
        this.mPackageName = pn;
        this.mCallingUid = uid;
        this.mFocusGainRequest = focusRequest;
        this.mGrantFlags = grantFlags;
        this.mFocusController = ctlr;
        this.mSdkTarget = sdk;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public FocusRequester(AudioFocusInfo afi, IAudioFocusDispatcher afl, IBinder source, MediaFocusControl.AudioFocusDeathHandler hdlr, MediaFocusControl ctlr) {
        this.mAttributes = afi.getAttributes();
        this.mClientId = afi.getClientId();
        this.mPackageName = afi.getPackageName();
        this.mCallingUid = afi.getClientUid();
        this.mFocusGainRequest = afi.getGainRequest();
        this.mGrantFlags = afi.getFlags();
        this.mSdkTarget = afi.getSdkTarget();
        this.mFocusDispatcher = afl;
        this.mSourceRef = source;
        this.mDeathHandler = hdlr;
        this.mFocusController = ctlr;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasSameClient(String otherClient) {
        return this.mClientId.compareTo(otherClient) == 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isLockedFocusOwner() {
        return (this.mGrantFlags & 4) != 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isInFocusLossLimbo() {
        return this.mFocusLossFadeLimbo;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasSameBinder(IBinder ib) {
        IBinder iBinder = this.mSourceRef;
        return iBinder != null && iBinder.equals(ib);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasSameDispatcher(IAudioFocusDispatcher fd) {
        IAudioFocusDispatcher iAudioFocusDispatcher = this.mFocusDispatcher;
        return iAudioFocusDispatcher != null && iAudioFocusDispatcher.equals(fd);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public String getPackageName() {
        return this.mPackageName;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasSamePackage(String pack) {
        return this.mPackageName.compareTo(pack) == 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasSameUid(int uid) {
        return this.mCallingUid == uid;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getClientUid() {
        return this.mCallingUid;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public String getClientId() {
        return this.mClientId;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getGainRequest() {
        return this.mFocusGainRequest;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getGrantFlags() {
        return this.mGrantFlags;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public AudioAttributes getAudioAttributes() {
        return this.mAttributes;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public String getGainPackageName() {
        return this.mPackageName;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getCallingUid() {
        return this.mCallingUid;
    }

    int getSdkTarget() {
        return this.mSdkTarget;
    }

    private static String focusChangeToString(int focus) {
        switch (focus) {
            case -3:
                return "LOSS_TRANSIENT_CAN_DUCK";
            case -2:
                return "LOSS_TRANSIENT";
            case -1:
                return "LOSS";
            case 0:
                return "none";
            case 1:
                return "GAIN";
            case 2:
                return "GAIN_TRANSIENT";
            case 3:
                return "GAIN_TRANSIENT_MAY_DUCK";
            case 4:
                return "GAIN_TRANSIENT_EXCLUSIVE";
            default:
                return "[invalid focus change" + focus + "]";
        }
    }

    private String focusGainToString() {
        return focusChangeToString(this.mFocusGainRequest);
    }

    private String focusLossToString() {
        return focusChangeToString(this.mFocusLossReceived);
    }

    private static String flagsToString(int flags) {
        String msg = new String();
        if ((flags & 1) != 0) {
            msg = msg + "DELAY_OK";
        }
        if ((flags & 4) != 0) {
            if (!msg.isEmpty()) {
                msg = msg + "|";
            }
            msg = msg + "LOCK";
        }
        if ((flags & 2) != 0) {
            if (!msg.isEmpty()) {
                msg = msg + "|";
            }
            return msg + "PAUSES_ON_DUCKABLE_LOSS";
        }
        return msg;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dump(PrintWriter pw) {
        pw.println("  source:" + this.mSourceRef + " -- pack: " + this.mPackageName + " -- client: " + this.mClientId + " -- gain: " + focusGainToString() + " -- flags: " + flagsToString(this.mGrantFlags) + " -- loss: " + focusLossToString() + " -- notified: " + this.mFocusLossWasNotified + " -- limbo" + this.mFocusLossFadeLimbo + " -- uid: " + this.mCallingUid + " -- attr: " + this.mAttributes + " -- sdk:" + this.mSdkTarget);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void maybeRelease() {
        if (!this.mFocusLossFadeLimbo) {
            release();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void release() {
        IBinder srcRef = this.mSourceRef;
        MediaFocusControl.AudioFocusDeathHandler deathHdlr = this.mDeathHandler;
        if (srcRef != null && deathHdlr != null) {
            try {
                srcRef.unlinkToDeath(deathHdlr, 0);
            } catch (NoSuchElementException e) {
            }
        }
        this.mDeathHandler = null;
        this.mFocusDispatcher = null;
    }

    protected void finalize() throws Throwable {
        release();
        super.finalize();
    }

    /* JADX WARN: Removed duplicated region for block: B:12:0x0013 A[RETURN] */
    /* JADX WARN: Removed duplicated region for block: B:13:0x0014 A[RETURN] */
    /* JADX WARN: Removed duplicated region for block: B:17:0x001b A[RETURN] */
    /* JADX WARN: Removed duplicated region for block: B:18:0x001c A[RETURN] */
    /* JADX WARN: Removed duplicated region for block: B:19:0x001d A[RETURN] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private int focusLossForGainRequest(int gainRequest) {
        switch (gainRequest) {
            case 1:
                switch (this.mFocusLossReceived) {
                    case -3:
                    case -2:
                    case -1:
                    case 0:
                        return -1;
                }
                switch (this.mFocusLossReceived) {
                    case -3:
                    case -2:
                    case 0:
                        return -2;
                    case -1:
                        return -1;
                }
                switch (this.mFocusLossReceived) {
                    case -3:
                    case 0:
                        return -3;
                    case -2:
                        return -2;
                    case -1:
                        return -1;
                }
                Log.e(TAG, "focusLossForGainRequest() for invalid focus request " + gainRequest);
                return 0;
            case 2:
            case 4:
                switch (this.mFocusLossReceived) {
                }
                switch (this.mFocusLossReceived) {
                }
                Log.e(TAG, "focusLossForGainRequest() for invalid focus request " + gainRequest);
                return 0;
            case 3:
                switch (this.mFocusLossReceived) {
                }
                Log.e(TAG, "focusLossForGainRequest() for invalid focus request " + gainRequest);
                return 0;
            default:
                Log.e(TAG, "focusLossForGainRequest() for invalid focus request " + gainRequest);
                return 0;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean handleFocusLossFromGain(int focusGain, FocusRequester frWinner, boolean forceDuck) {
        int focusLoss = focusLossForGainRequest(focusGain);
        handleFocusLoss(focusLoss, frWinner, forceDuck);
        return focusLoss == -1;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void handleFocusGain(int focusGain) {
        try {
            this.mFocusLossReceived = 0;
            this.mFocusLossFadeLimbo = false;
            this.mFocusController.notifyExtPolicyFocusGrant_syncAf(toAudioFocusInfo(), 1);
            IAudioFocusDispatcher fd = this.mFocusDispatcher;
            if (fd != null) {
                if (DEBUG) {
                    Log.v(TAG, "dispatching " + focusChangeToString(focusGain) + " to " + this.mClientId);
                }
                if (this.mFocusLossWasNotified) {
                    fd.dispatchAudioFocusChange(focusGain, this.mClientId);
                }
            }
            this.mFocusController.restoreVShapedPlayers(this);
        } catch (RemoteException e) {
            Log.e(TAG, "Failure to signal gain of audio focus due to: ", e);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void handleFocusGainFromRequest(int focusRequestResult) {
        if (focusRequestResult == 1) {
            this.mFocusController.restoreVShapedPlayers(this);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void handleFocusLoss(int focusLoss, FocusRequester frWinner, boolean forceDuck) {
        try {
            if (focusLoss != this.mFocusLossReceived) {
                this.mFocusLossReceived = focusLoss;
                this.mFocusLossWasNotified = false;
                if (!this.mFocusController.mustNotifyFocusOwnerOnDuck() && this.mFocusLossReceived == -3 && (this.mGrantFlags & 2) == 0) {
                    if (DEBUG) {
                        Log.v(TAG, "NOT dispatching " + focusChangeToString(this.mFocusLossReceived) + " to " + this.mClientId + ", to be handled externally");
                    }
                    this.mFocusController.notifyExtPolicyFocusLoss_syncAf(toAudioFocusInfo(), false);
                    return;
                }
                boolean handled = false;
                if (frWinner != null) {
                    handled = frameworkHandleFocusLoss(focusLoss, frWinner, forceDuck);
                }
                if (handled) {
                    if (DEBUG) {
                        Log.v(TAG, "NOT dispatching " + focusChangeToString(this.mFocusLossReceived) + " to " + this.mClientId + ", response handled by framework");
                    }
                    this.mFocusController.notifyExtPolicyFocusLoss_syncAf(toAudioFocusInfo(), false);
                    return;
                }
                IAudioFocusDispatcher fd = this.mFocusDispatcher;
                if (fd != null) {
                    if (DEBUG) {
                        Log.v(TAG, "dispatching " + focusChangeToString(this.mFocusLossReceived) + " to " + this.mClientId);
                    }
                    this.mFocusController.notifyExtPolicyFocusLoss_syncAf(toAudioFocusInfo(), true);
                    this.mFocusLossWasNotified = true;
                    fd.dispatchAudioFocusChange(this.mFocusLossReceived, this.mClientId);
                }
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Failure to signal loss of audio focus due to:", e);
        }
    }

    private boolean frameworkHandleFocusLoss(int focusLoss, FocusRequester frWinner, boolean forceDuck) {
        if (frWinner.mCallingUid == this.mCallingUid) {
            return false;
        }
        if (focusLoss != -3) {
            if (focusLoss == -1) {
                boolean playersAreFaded = this.mFocusController.fadeOutPlayers(frWinner, this);
                if (playersAreFaded) {
                    this.mFocusLossFadeLimbo = true;
                    this.mFocusController.postDelayedLossAfterFade(this, 1000L);
                    return true;
                }
            }
            return false;
        } else if (!forceDuck && (this.mGrantFlags & 2) != 0) {
            Log.v(TAG, "not ducking uid " + this.mCallingUid + " - flags");
            return false;
        } else if (!forceDuck && getSdkTarget() <= 25) {
            Log.v(TAG, "not ducking uid " + this.mCallingUid + " - old SDK");
            return false;
        } else {
            return this.mFocusController.duckPlayers(frWinner, this, forceDuck);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int dispatchFocusChange(int focusChange) {
        IAudioFocusDispatcher fd = this.mFocusDispatcher;
        if (fd == null) {
            if (MediaFocusControl.DEBUG) {
                Log.e(TAG, "dispatchFocusChange: no focus dispatcher");
            }
            return 0;
        } else if (focusChange == 0) {
            if (MediaFocusControl.DEBUG) {
                Log.v(TAG, "dispatchFocusChange: AUDIOFOCUS_NONE");
            }
            return 0;
        } else {
            if ((focusChange == 3 || focusChange == 4 || focusChange == 2 || focusChange == 1) && this.mFocusGainRequest != focusChange) {
                Log.w(TAG, "focus gain was requested with " + this.mFocusGainRequest + ", dispatching " + focusChange);
            } else if (focusChange == -3 || focusChange == -2 || focusChange == -1) {
                this.mFocusLossReceived = focusChange;
            }
            try {
                fd.dispatchAudioFocusChange(focusChange, this.mClientId);
                return 1;
            } catch (RemoteException e) {
                Log.e(TAG, "dispatchFocusChange: error talking to focus listener " + this.mClientId, e);
                return 0;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dispatchFocusResultFromExtPolicy(int requestResult) {
        IAudioFocusDispatcher fd = this.mFocusDispatcher;
        if (fd == null) {
            if (MediaFocusControl.DEBUG) {
                Log.e(TAG, "dispatchFocusResultFromExtPolicy: no focus dispatcher");
                return;
            }
            return;
        }
        if (DEBUG) {
            Log.v(TAG, "dispatching result" + requestResult + " to " + this.mClientId);
        }
        try {
            fd.dispatchFocusResultFromExtPolicy(requestResult, this.mClientId);
        } catch (RemoteException e) {
            Log.e(TAG, "dispatchFocusResultFromExtPolicy: error talking to focus listener" + this.mClientId, e);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public AudioFocusInfo toAudioFocusInfo() {
        return new AudioFocusInfo(this.mAttributes, this.mCallingUid, this.mClientId, this.mPackageName, this.mFocusGainRequest, this.mFocusLossReceived, this.mGrantFlags, this.mSdkTarget);
    }
}
