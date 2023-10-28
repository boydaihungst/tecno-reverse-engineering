package com.android.server.audio;

import android.app.AppOpsManager;
import android.content.ContentResolver;
import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFocusInfo;
import android.media.AudioManager;
import android.media.IAudioFocusDispatcher;
import android.media.MediaMetrics;
import android.media.audiopolicy.IAudioPolicyCallback;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.Log;
import com.android.server.audio.AudioEventLogger;
import com.android.server.slice.SliceClientPermissions;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
/* loaded from: classes.dex */
public class MediaFocusControl implements PlayerFocusEnforcer {
    static final boolean DEBUG;
    static final int DUCKING_IN_APP_SDK_LEVEL = 25;
    static final boolean ENFORCE_DUCKING = true;
    static final boolean ENFORCE_DUCKING_FOR_NEW = true;
    static final boolean ENFORCE_FADEOUT_FOR_FOCUS_LOSS = true;
    static final boolean ENFORCE_MUTING_FOR_RING_OR_CALL = true;
    private static final int MAX_STACK_SIZE = 100;
    private static final int MSG_L_FOCUS_LOSS_AFTER_FADE = 1;
    private static final int MSL_L_FORGET_UID = 2;
    private static final int RING_CALL_MUTING_ENFORCEMENT_DELAY_MS = 100;
    private static final int RING_CALL_UNMUTING_ENFORCEMENT_DELAY_MS = 750;
    private static final String TAG = "MediaFocusControl";
    private static final int[] USAGES_TO_MUTE_IN_RING_OR_CALL;
    private static final Object mAudioFocusLock;
    private static final AudioEventLogger mEventLogger;
    private static final String mMetricsId = "audio.focus";
    private final AppOpsManager mAppOps;
    private final Context mContext;
    private long mExtFocusChangeCounter;
    private PlayerFocusEnforcer mFocusEnforcer;
    private Handler mFocusHandler;
    private HandlerThread mFocusThread;
    private boolean mMultiAudioFocusEnabled;
    private boolean mRingOrCallActive = false;
    private final Object mExtFocusChangeLock = new Object();
    private final Stack<FocusRequester> mFocusStack = new Stack<>();
    ArrayList<FocusRequester> mMultiAudioFocusList = new ArrayList<>();
    private boolean mNotifyFocusOwnerOnDuck = true;
    private ArrayList<IAudioPolicyCallback> mFocusFollowers = new ArrayList<>();
    private IAudioPolicyCallback mFocusPolicy = null;
    private IAudioPolicyCallback mPreviousFocusPolicy = null;
    private HashMap<String, FocusRequester> mFocusOwnersForFocusPolicy = new HashMap<>();

    static {
        DEBUG = "1".equals(SystemProperties.get("persist.user.root.support", "0")) || "1".equals(SystemProperties.get("persist.sys.fans.support", "0"));
        mAudioFocusLock = new Object();
        mEventLogger = new AudioEventLogger(50, "focus commands as seen by MediaFocusControl");
        USAGES_TO_MUTE_IN_RING_OR_CALL = new int[]{1, 14};
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public MediaFocusControl(Context cntxt, PlayerFocusEnforcer pfe) {
        this.mMultiAudioFocusEnabled = false;
        this.mContext = cntxt;
        this.mAppOps = (AppOpsManager) cntxt.getSystemService("appops");
        this.mFocusEnforcer = pfe;
        ContentResolver cr = cntxt.getContentResolver();
        this.mMultiAudioFocusEnabled = Settings.System.getIntForUser(cr, "multi_audio_focus_enabled", 0, cr.getUserId()) != 0;
        initFocusThreading();
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void dump(PrintWriter pw) {
        pw.println("\nMediaFocusControl dump time: " + DateFormat.getTimeInstance().format(new Date()));
        dumpFocusStack(pw);
        pw.println("\n");
        mEventLogger.dump(pw);
        dumpMultiAudioFocus(pw);
    }

    @Override // com.android.server.audio.PlayerFocusEnforcer
    public boolean duckPlayers(FocusRequester winner, FocusRequester loser, boolean forceDuck) {
        return this.mFocusEnforcer.duckPlayers(winner, loser, forceDuck);
    }

    @Override // com.android.server.audio.PlayerFocusEnforcer
    public void restoreVShapedPlayers(FocusRequester winner) {
        this.mFocusEnforcer.restoreVShapedPlayers(winner);
        this.mFocusHandler.removeEqualMessages(2, new ForgetFadeUidInfo(winner.getClientUid()));
    }

    @Override // com.android.server.audio.PlayerFocusEnforcer
    public void mutePlayersForCall(int[] usagesToMute) {
        this.mFocusEnforcer.mutePlayersForCall(usagesToMute);
    }

    @Override // com.android.server.audio.PlayerFocusEnforcer
    public void unmutePlayersForCall() {
        this.mFocusEnforcer.unmutePlayersForCall();
    }

    @Override // com.android.server.audio.PlayerFocusEnforcer
    public boolean fadeOutPlayers(FocusRequester winner, FocusRequester loser) {
        return this.mFocusEnforcer.fadeOutPlayers(winner, loser);
    }

    @Override // com.android.server.audio.PlayerFocusEnforcer
    public void forgetUid(int uid) {
        this.mFocusEnforcer.forgetUid(uid);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void noFocusForSuspendedApp(String packageName, int uid) {
        synchronized (mAudioFocusLock) {
            Iterator<FocusRequester> stackIterator = this.mFocusStack.iterator();
            List<String> clientsToRemove = new ArrayList<>();
            while (stackIterator.hasNext()) {
                FocusRequester focusOwner = stackIterator.next();
                if (focusOwner.hasSameUid(uid) && focusOwner.hasSamePackage(packageName)) {
                    clientsToRemove.add(focusOwner.getClientId());
                    mEventLogger.log(new AudioEventLogger.StringEvent("focus owner:" + focusOwner.getClientId() + " in uid:" + uid + " pack: " + packageName + " getting AUDIOFOCUS_LOSS due to app suspension").printLog(TAG));
                    focusOwner.dispatchFocusChange(-1);
                }
            }
            for (String clientToRemove : clientsToRemove) {
                removeFocusStackEntry(clientToRemove, false, true);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasAudioFocusUsers() {
        boolean z;
        synchronized (mAudioFocusLock) {
            z = !this.mFocusStack.empty();
        }
        return z;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void discardAudioFocusOwner() {
        synchronized (mAudioFocusLock) {
            if (!this.mFocusStack.empty()) {
                FocusRequester exFocusOwner = this.mFocusStack.pop();
                exFocusOwner.handleFocusLoss(-1, null, false);
                exFocusOwner.release();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public List<AudioFocusInfo> getFocusStack() {
        ArrayList<AudioFocusInfo> stack;
        synchronized (mAudioFocusLock) {
            stack = new ArrayList<>(this.mFocusStack.size());
            Iterator<FocusRequester> it = this.mFocusStack.iterator();
            while (it.hasNext()) {
                FocusRequester fr = it.next();
                stack.add(fr.toAudioFocusInfo());
            }
        }
        return stack;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean sendFocusLoss(AudioFocusInfo focusLoser) {
        synchronized (mAudioFocusLock) {
            FocusRequester loserToRemove = null;
            Iterator<FocusRequester> it = this.mFocusStack.iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                FocusRequester fr = it.next();
                if (fr.getClientId().equals(focusLoser.getClientId())) {
                    fr.handleFocusLoss(-1, null, false);
                    loserToRemove = fr;
                    break;
                }
            }
            if (loserToRemove != null) {
                this.mFocusStack.remove(loserToRemove);
                loserToRemove.release();
                return true;
            }
            return false;
        }
    }

    private void notifyTopOfAudioFocusStack() {
        if (!this.mFocusStack.empty() && canReassignAudioFocus()) {
            this.mFocusStack.peek().handleFocusGain(1);
        }
        if (this.mMultiAudioFocusEnabled && !this.mMultiAudioFocusList.isEmpty()) {
            Iterator<FocusRequester> it = this.mMultiAudioFocusList.iterator();
            while (it.hasNext()) {
                FocusRequester multifr = it.next();
                if (isLockedFocusOwner(multifr)) {
                    multifr.handleFocusGain(1);
                }
            }
        }
    }

    private void propagateFocusLossFromGain_syncAf(int focusGain, FocusRequester fr, boolean forceDuck) {
        List<String> clientsToRemove = new LinkedList<>();
        if (!this.mFocusStack.empty()) {
            Iterator<FocusRequester> it = this.mFocusStack.iterator();
            while (it.hasNext()) {
                FocusRequester focusLoser = it.next();
                boolean isDefinitiveLoss = focusLoser.handleFocusLossFromGain(focusGain, fr, forceDuck);
                if (isDefinitiveLoss) {
                    clientsToRemove.add(focusLoser.getClientId());
                }
            }
        }
        if (this.mMultiAudioFocusEnabled && !this.mMultiAudioFocusList.isEmpty()) {
            Iterator<FocusRequester> it2 = this.mMultiAudioFocusList.iterator();
            while (it2.hasNext()) {
                FocusRequester multifocusLoser = it2.next();
                boolean isDefinitiveLoss2 = multifocusLoser.handleFocusLossFromGain(focusGain, fr, forceDuck);
                if (isDefinitiveLoss2) {
                    clientsToRemove.add(multifocusLoser.getClientId());
                }
            }
        }
        for (String clientToRemove : clientsToRemove) {
            removeFocusStackEntry(clientToRemove, false, true);
        }
    }

    private void dumpFocusStack(PrintWriter pw) {
        pw.println("\nAudio Focus stack entries (last is top of stack):");
        synchronized (mAudioFocusLock) {
            Iterator<FocusRequester> stackIterator = this.mFocusStack.iterator();
            while (stackIterator.hasNext()) {
                stackIterator.next().dump(pw);
            }
            pw.println("\n");
            if (this.mFocusPolicy == null) {
                pw.println("No external focus policy\n");
            } else {
                pw.println("External focus policy: " + this.mFocusPolicy + ", focus owners:\n");
                dumpExtFocusPolicyFocusOwners(pw);
            }
        }
        pw.println("\n");
        pw.println(" Notify on duck:  " + this.mNotifyFocusOwnerOnDuck + "\n");
        pw.println(" In ring or call: " + this.mRingOrCallActive + "\n");
    }

    private void removeFocusStackEntry(String clientToRemove, boolean signal, boolean notifyFocusFollowers) {
        AudioFocusInfo abandonSource = null;
        if (!this.mFocusStack.empty() && this.mFocusStack.peek().hasSameClient(clientToRemove)) {
            FocusRequester fr = this.mFocusStack.pop();
            fr.maybeRelease();
            if (notifyFocusFollowers) {
                abandonSource = fr.toAudioFocusInfo();
            }
            if (signal) {
                notifyTopOfAudioFocusStack();
            }
        } else {
            Iterator<FocusRequester> stackIterator = this.mFocusStack.iterator();
            while (stackIterator.hasNext()) {
                FocusRequester fr2 = stackIterator.next();
                if (fr2.hasSameClient(clientToRemove)) {
                    Log.i(TAG, "AudioFocus  removeFocusStackEntry(): removing entry for " + clientToRemove);
                    stackIterator.remove();
                    if (notifyFocusFollowers) {
                        abandonSource = fr2.toAudioFocusInfo();
                    }
                    fr2.maybeRelease();
                }
            }
        }
        if (abandonSource != null) {
            abandonSource.clearLossReceived();
            notifyExtPolicyFocusLoss_syncAf(abandonSource, false);
        }
        if (this.mMultiAudioFocusEnabled && !this.mMultiAudioFocusList.isEmpty()) {
            Iterator<FocusRequester> listIterator = this.mMultiAudioFocusList.iterator();
            while (listIterator.hasNext()) {
                FocusRequester fr3 = listIterator.next();
                if (fr3.hasSameClient(clientToRemove)) {
                    listIterator.remove();
                    fr3.release();
                }
            }
            if (signal) {
                notifyTopOfAudioFocusStack();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void removeFocusStackEntryOnDeath(IBinder cb) {
        boolean isTopOfStackForClientToRemove = !this.mFocusStack.isEmpty() && this.mFocusStack.peek().hasSameBinder(cb);
        Iterator<FocusRequester> stackIterator = this.mFocusStack.iterator();
        while (stackIterator.hasNext()) {
            FocusRequester fr = stackIterator.next();
            if (fr.hasSameBinder(cb)) {
                Log.i(TAG, "AudioFocus  removeFocusStackEntryOnDeath(): removing entry for " + cb);
                mEventLogger.log(new AudioEventLogger.StringEvent("focus requester:" + fr.getClientId() + " in uid:" + fr.getClientUid() + " pack:" + fr.getPackageName() + " died"));
                notifyExtPolicyFocusLoss_syncAf(fr.toAudioFocusInfo(), false);
                stackIterator.remove();
                fr.release();
            }
        }
        if (isTopOfStackForClientToRemove) {
            notifyTopOfAudioFocusStack();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void removeFocusEntryForExtPolicyOnDeath(IBinder cb) {
        if (this.mFocusOwnersForFocusPolicy.isEmpty()) {
            return;
        }
        Set<Map.Entry<String, FocusRequester>> owners = this.mFocusOwnersForFocusPolicy.entrySet();
        Iterator<Map.Entry<String, FocusRequester>> ownerIterator = owners.iterator();
        while (ownerIterator.hasNext()) {
            Map.Entry<String, FocusRequester> owner = ownerIterator.next();
            FocusRequester fr = owner.getValue();
            if (fr.hasSameBinder(cb)) {
                ownerIterator.remove();
                mEventLogger.log(new AudioEventLogger.StringEvent("focus requester:" + fr.getClientId() + " in uid:" + fr.getClientUid() + " pack:" + fr.getPackageName() + " died"));
                fr.release();
                notifyExtFocusPolicyFocusAbandon_syncAf(fr.toAudioFocusInfo());
                return;
            }
        }
    }

    private boolean canReassignAudioFocus() {
        if (!this.mFocusStack.isEmpty() && isLockedFocusOwner(this.mFocusStack.peek())) {
            return false;
        }
        return true;
    }

    private boolean isLockedFocusOwner(FocusRequester fr) {
        return fr.hasSameClient("AudioFocus_For_Phone_Ring_And_Calls") || fr.isLockedFocusOwner();
    }

    private int pushBelowLockedFocusOwnersAndPropagate(FocusRequester nfr) {
        if (DEBUG) {
            Log.v(TAG, "pushBelowLockedFocusOwnersAndPropagate client=" + nfr.getClientId());
        }
        int lastLockedFocusOwnerIndex = this.mFocusStack.size();
        for (int index = this.mFocusStack.size() - 1; index >= 0; index--) {
            if (isLockedFocusOwner(this.mFocusStack.elementAt(index))) {
                lastLockedFocusOwnerIndex = index;
            }
        }
        if (lastLockedFocusOwnerIndex == this.mFocusStack.size()) {
            Log.e(TAG, "No exclusive focus owner found in propagateFocusLossFromGain_syncAf()", new Exception());
            propagateFocusLossFromGain_syncAf(nfr.getGainRequest(), nfr, false);
            this.mFocusStack.push(nfr);
            return 1;
        }
        if (DEBUG) {
            Log.v(TAG, "> lastLockedFocusOwnerIndex=" + lastLockedFocusOwnerIndex);
        }
        this.mFocusStack.insertElementAt(nfr, lastLockedFocusOwnerIndex);
        List<String> clientsToRemove = new LinkedList<>();
        for (int index2 = lastLockedFocusOwnerIndex - 1; index2 >= 0; index2--) {
            boolean isDefinitiveLoss = this.mFocusStack.elementAt(index2).handleFocusLossFromGain(nfr.getGainRequest(), nfr, false);
            if (isDefinitiveLoss) {
                clientsToRemove.add(this.mFocusStack.elementAt(index2).getClientId());
            }
        }
        for (String clientToRemove : clientsToRemove) {
            if (DEBUG) {
                Log.v(TAG, "> removing focus client " + clientToRemove);
            }
            removeFocusStackEntry(clientToRemove, false, true);
        }
        return 2;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    /* loaded from: classes.dex */
    public class AudioFocusDeathHandler implements IBinder.DeathRecipient {
        private IBinder mCb;

        AudioFocusDeathHandler(IBinder cb) {
            this.mCb = cb;
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            synchronized (MediaFocusControl.mAudioFocusLock) {
                if (MediaFocusControl.this.mFocusPolicy != null) {
                    MediaFocusControl.this.removeFocusEntryForExtPolicyOnDeath(this.mCb);
                } else {
                    MediaFocusControl.this.removeFocusStackEntryOnDeath(this.mCb);
                    if (MediaFocusControl.this.mMultiAudioFocusEnabled && !MediaFocusControl.this.mMultiAudioFocusList.isEmpty()) {
                        Iterator<FocusRequester> listIterator = MediaFocusControl.this.mMultiAudioFocusList.iterator();
                        while (listIterator.hasNext()) {
                            FocusRequester fr = listIterator.next();
                            if (fr.hasSameBinder(this.mCb)) {
                                listIterator.remove();
                                fr.release();
                            }
                        }
                    }
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void setDuckingInExtPolicyAvailable(boolean available) {
        this.mNotifyFocusOwnerOnDuck = !available;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean mustNotifyFocusOwnerOnDuck() {
        return this.mNotifyFocusOwnerOnDuck;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void addFocusFollower(IAudioPolicyCallback ff) {
        if (ff == null) {
            return;
        }
        synchronized (mAudioFocusLock) {
            boolean found = false;
            Iterator<IAudioPolicyCallback> it = this.mFocusFollowers.iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                IAudioPolicyCallback pcb = it.next();
                if (pcb.asBinder().equals(ff.asBinder())) {
                    found = true;
                    break;
                }
            }
            if (found) {
                return;
            }
            this.mFocusFollowers.add(ff);
            notifyExtPolicyCurrentFocusAsync(ff);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void removeFocusFollower(IAudioPolicyCallback ff) {
        if (ff == null) {
            return;
        }
        synchronized (mAudioFocusLock) {
            Iterator<IAudioPolicyCallback> it = this.mFocusFollowers.iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                IAudioPolicyCallback pcb = it.next();
                if (pcb.asBinder().equals(ff.asBinder())) {
                    this.mFocusFollowers.remove(pcb);
                    break;
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setFocusPolicy(IAudioPolicyCallback policy, boolean isTestFocusPolicy) {
        if (policy == null) {
            return;
        }
        synchronized (mAudioFocusLock) {
            if (isTestFocusPolicy) {
                this.mPreviousFocusPolicy = this.mFocusPolicy;
            }
            this.mFocusPolicy = policy;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void unsetFocusPolicy(IAudioPolicyCallback policy, boolean isTestFocusPolicy) {
        if (policy == null) {
            return;
        }
        synchronized (mAudioFocusLock) {
            if (this.mFocusPolicy == policy) {
                if (isTestFocusPolicy) {
                    this.mFocusPolicy = this.mPreviousFocusPolicy;
                } else {
                    this.mFocusPolicy = null;
                }
            }
        }
    }

    void notifyExtPolicyCurrentFocusAsync(final IAudioPolicyCallback pcb) {
        Thread thread = new Thread() { // from class: com.android.server.audio.MediaFocusControl.1
            @Override // java.lang.Thread, java.lang.Runnable
            public void run() {
                synchronized (MediaFocusControl.mAudioFocusLock) {
                    if (MediaFocusControl.this.mFocusStack.isEmpty()) {
                        return;
                    }
                    try {
                        pcb.notifyAudioFocusGrant(((FocusRequester) MediaFocusControl.this.mFocusStack.peek()).toAudioFocusInfo(), 1);
                    } catch (RemoteException e) {
                        Log.e(MediaFocusControl.TAG, "Can't call notifyAudioFocusGrant() on IAudioPolicyCallback " + pcb.asBinder(), e);
                    }
                }
            }
        };
        thread.start();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void notifyExtPolicyFocusGrant_syncAf(AudioFocusInfo afi, int requestResult) {
        Iterator<IAudioPolicyCallback> it = this.mFocusFollowers.iterator();
        while (it.hasNext()) {
            IAudioPolicyCallback pcb = it.next();
            try {
                pcb.notifyAudioFocusGrant(afi, requestResult);
            } catch (RemoteException e) {
                Log.e(TAG, "Can't call notifyAudioFocusGrant() on IAudioPolicyCallback " + pcb.asBinder(), e);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void notifyExtPolicyFocusLoss_syncAf(AudioFocusInfo afi, boolean wasDispatched) {
        Iterator<IAudioPolicyCallback> it = this.mFocusFollowers.iterator();
        while (it.hasNext()) {
            IAudioPolicyCallback pcb = it.next();
            try {
                pcb.notifyAudioFocusLoss(afi, wasDispatched);
            } catch (RemoteException e) {
                Log.e(TAG, "Can't call notifyAudioFocusLoss() on IAudioPolicyCallback " + pcb.asBinder(), e);
            }
        }
    }

    boolean notifyExtFocusPolicyFocusRequest_syncAf(AudioFocusInfo afi, IAudioFocusDispatcher fd, IBinder cb) {
        boolean keepTrack;
        if (DEBUG) {
            Log.v(TAG, "notifyExtFocusPolicyFocusRequest client=" + afi.getClientId() + " dispatcher=" + fd);
        }
        synchronized (this.mExtFocusChangeLock) {
            long j = this.mExtFocusChangeCounter;
            this.mExtFocusChangeCounter = 1 + j;
            afi.setGen(j);
        }
        FocusRequester existingFr = this.mFocusOwnersForFocusPolicy.get(afi.getClientId());
        if (existingFr != null) {
            if (existingFr.hasSameDispatcher(fd)) {
                keepTrack = false;
            } else {
                existingFr.release();
                keepTrack = true;
            }
        } else {
            keepTrack = true;
        }
        if (keepTrack) {
            AudioFocusDeathHandler hdlr = new AudioFocusDeathHandler(cb);
            try {
                cb.linkToDeath(hdlr, 0);
                this.mFocusOwnersForFocusPolicy.put(afi.getClientId(), new FocusRequester(afi, fd, cb, hdlr, this));
            } catch (RemoteException e) {
                return false;
            }
        }
        try {
            this.mFocusPolicy.notifyAudioFocusRequest(afi, 1);
            return true;
        } catch (RemoteException e2) {
            Log.e(TAG, "Can't call notifyAudioFocusRequest() on IAudioPolicyCallback " + this.mFocusPolicy.asBinder(), e2);
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setFocusRequestResultFromExtPolicy(AudioFocusInfo afi, int requestResult) {
        FocusRequester fr;
        synchronized (this.mExtFocusChangeLock) {
            if (afi.getGen() > this.mExtFocusChangeCounter) {
                return;
            }
            if (requestResult == 0) {
                fr = this.mFocusOwnersForFocusPolicy.remove(afi.getClientId());
            } else {
                fr = this.mFocusOwnersForFocusPolicy.get(afi.getClientId());
            }
            if (fr != null) {
                fr.dispatchFocusResultFromExtPolicy(requestResult);
            }
        }
    }

    boolean notifyExtFocusPolicyFocusAbandon_syncAf(AudioFocusInfo afi) {
        if (this.mFocusPolicy == null) {
            return false;
        }
        FocusRequester fr = this.mFocusOwnersForFocusPolicy.remove(afi.getClientId());
        if (fr != null) {
            fr.release();
        }
        try {
            this.mFocusPolicy.notifyAudioFocusAbandon(afi);
            return true;
        } catch (RemoteException e) {
            Log.e(TAG, "Can't call notifyAudioFocusAbandon() on IAudioPolicyCallback " + this.mFocusPolicy.asBinder(), e);
            return true;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int dispatchFocusChange(AudioFocusInfo afi, int focusChange) {
        FocusRequester fr;
        boolean z = DEBUG;
        if (z) {
            Log.v(TAG, "dispatchFocusChange " + focusChange + " to afi client=" + afi.getClientId());
        }
        synchronized (mAudioFocusLock) {
            if (this.mFocusPolicy == null) {
                if (z) {
                    Log.v(TAG, "> failed: no focus policy");
                }
                return 0;
            }
            if (focusChange == -1) {
                fr = this.mFocusOwnersForFocusPolicy.remove(afi.getClientId());
            } else {
                fr = this.mFocusOwnersForFocusPolicy.get(afi.getClientId());
            }
            if (fr == null) {
                if (z) {
                    Log.v(TAG, "> failed: no such focus requester known");
                }
                return 0;
            }
            return fr.dispatchFocusChange(focusChange);
        }
    }

    private void dumpExtFocusPolicyFocusOwners(PrintWriter pw) {
        Set<Map.Entry<String, FocusRequester>> owners = this.mFocusOwnersForFocusPolicy.entrySet();
        for (Map.Entry<String, FocusRequester> owner : owners) {
            FocusRequester fr = owner.getValue();
            fr.dump(pw);
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public int getCurrentAudioFocus() {
        synchronized (mAudioFocusLock) {
            if (this.mFocusStack.empty()) {
                return 0;
            }
            return this.mFocusStack.peek().getGainRequest();
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public String getCurrentAudioPackageName() {
        synchronized (mAudioFocusLock) {
            if (this.mFocusStack.empty()) {
                return null;
            }
            return this.mFocusStack.peek().getPackageName();
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public static int getFocusRampTimeMs(int focusGain, AudioAttributes attr) {
        switch (attr.getUsage()) {
            case 1:
            case 14:
                return 1000;
            case 2:
            case 3:
            case 5:
            case 7:
            case 8:
            case 9:
            case 10:
            case 13:
            case 1002:
                return 500;
            case 4:
            case 6:
            case 11:
            case 12:
            case 16:
            case 1003:
                return 700;
            default:
                return 0;
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [1151=5] */
    /* JADX DEBUG: Multi-variable search result rejected for r22v0, resolved type: com.android.server.audio.MediaFocusControl */
    /* JADX DEBUG: Multi-variable search result rejected for r25v0, resolved type: android.os.IBinder */
    /* JADX INFO: Access modifiers changed from: protected */
    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r11v1 */
    /* JADX WARN: Type inference failed for: r11v2, types: [int, boolean] */
    /* JADX WARN: Type inference failed for: r11v5 */
    public int requestAudioFocus(AudioAttributes aa, int focusChangeHint, IBinder cb, IAudioFocusDispatcher fd, String clientId, String callingPackageName, String attributionTag, int flags, int sdk, boolean forceDuck, int testUid) {
        int i;
        int i2;
        ?? r11;
        int uid;
        AudioFocusInfo afiForExtPolicy;
        boolean focusGrantDelayed;
        boolean z;
        boolean z2;
        new MediaMetrics.Item(mMetricsId).setUid(Binder.getCallingUid()).set(MediaMetrics.Property.CALLING_PACKAGE, callingPackageName).set(MediaMetrics.Property.CLIENT_NAME, clientId).set(MediaMetrics.Property.EVENT, "requestAudioFocus").set(MediaMetrics.Property.FLAGS, Integer.valueOf(flags)).set(MediaMetrics.Property.FOCUS_CHANGE_HINT, AudioManager.audioFocusToString(focusChangeHint)).record();
        int uid2 = flags == 8 ? testUid : Binder.getCallingUid();
        mEventLogger.log(new AudioEventLogger.StringEvent("requestAudioFocus() from uid/pid " + uid2 + SliceClientPermissions.SliceAuthority.DELIMITER + Binder.getCallingPid() + " AA=" + aa.usageToString() + SliceClientPermissions.SliceAuthority.DELIMITER + aa.contentTypeToString() + " clientId=" + clientId + " callingPack=" + callingPackageName + " req=" + focusChangeHint + " flags=0x" + Integer.toHexString(flags) + " sdk=" + sdk).printLog(TAG));
        if (!cb.pingBinder()) {
            Log.e(TAG, " AudioFocus DOA client for requestAudioFocus(), aborting.");
            return 0;
        }
        if (flags != 8) {
            i = 0;
            if (this.mAppOps.noteOp(32, Binder.getCallingUid(), callingPackageName, attributionTag, (String) null) != 0) {
                return 0;
            }
        } else {
            i = 0;
        }
        synchronized (mAudioFocusLock) {
            try {
                try {
                    if (this.mFocusStack.size() > 100) {
                        Log.e(TAG, "Max AudioFocus stack size reached, failing requestAudioFocus()");
                        return i;
                    }
                    int i3 = (!this.mRingOrCallActive ? 1 : i) & ("AudioFocus_For_Phone_Ring_And_Calls".compareTo(clientId) == 0 ? 1 : i);
                    if (i3 != 0) {
                        this.mRingOrCallActive = true;
                    }
                    if (this.mFocusPolicy != null) {
                        try {
                            i2 = 100;
                            r11 = i;
                            uid = uid2;
                        } catch (Throwable th) {
                            e = th;
                            throw e;
                        }
                        try {
                            afiForExtPolicy = new AudioFocusInfo(aa, uid2, clientId, callingPackageName, focusChangeHint, 0, flags, sdk);
                        } catch (Throwable th2) {
                            e = th2;
                            throw e;
                        }
                    } else {
                        i2 = 100;
                        r11 = i;
                        uid = uid2;
                        afiForExtPolicy = null;
                    }
                    if (canReassignAudioFocus()) {
                        focusGrantDelayed = false;
                    } else if ((flags & 1) == 0) {
                        return r11;
                    } else {
                        focusGrantDelayed = true;
                    }
                    if (this.mFocusPolicy != null) {
                        return notifyExtFocusPolicyFocusRequest_syncAf(afiForExtPolicy, fd, cb) ? i2 : r11;
                    }
                    AudioFocusDeathHandler afdh = new AudioFocusDeathHandler(cb);
                    try {
                        try {
                            try {
                                try {
                                    cb.linkToDeath(afdh, r11);
                                    if (this.mFocusStack.empty() || !this.mFocusStack.peek().hasSameClient(clientId)) {
                                        z = true;
                                    } else {
                                        FocusRequester fr = this.mFocusStack.peek();
                                        if (fr.getGainRequest() == focusChangeHint && fr.getGrantFlags() == flags) {
                                            cb.unlinkToDeath(afdh, r11);
                                            notifyExtPolicyFocusGrant_syncAf(fr.toAudioFocusInfo(), 1);
                                            return 1;
                                        }
                                        z = true;
                                        if (!focusGrantDelayed) {
                                            this.mFocusStack.pop();
                                            fr.release();
                                        }
                                    }
                                    removeFocusStackEntry(clientId, r11, r11);
                                    boolean z3 = z;
                                    FocusRequester nfr = new FocusRequester(aa, focusChangeHint, flags, fd, cb, clientId, afdh, callingPackageName, uid, this, sdk);
                                    if (!this.mMultiAudioFocusEnabled || focusChangeHint != z3) {
                                        z2 = forceDuck;
                                    } else if (i3 == 0) {
                                        boolean needAdd = true;
                                        if (!this.mMultiAudioFocusList.isEmpty()) {
                                            Iterator<FocusRequester> it = this.mMultiAudioFocusList.iterator();
                                            while (true) {
                                                if (!it.hasNext()) {
                                                    break;
                                                }
                                                FocusRequester multifr = it.next();
                                                if (multifr.getClientUid() == Binder.getCallingUid()) {
                                                    needAdd = false;
                                                    break;
                                                }
                                            }
                                        }
                                        if (needAdd) {
                                            this.mMultiAudioFocusList.add(nfr);
                                        }
                                        nfr.handleFocusGainFromRequest(z3 ? 1 : 0);
                                        notifyExtPolicyFocusGrant_syncAf(nfr.toAudioFocusInfo(), z3 ? 1 : 0);
                                        return z3 ? 1 : 0;
                                    } else if (this.mMultiAudioFocusList.isEmpty()) {
                                        z2 = forceDuck;
                                    } else {
                                        Iterator<FocusRequester> it2 = this.mMultiAudioFocusList.iterator();
                                        while (it2.hasNext()) {
                                            FocusRequester multifr2 = it2.next();
                                            multifr2.handleFocusLossFromGain(focusChangeHint, nfr, forceDuck);
                                        }
                                        z2 = forceDuck;
                                    }
                                    if (focusGrantDelayed) {
                                        int requestResult = pushBelowLockedFocusOwnersAndPropagate(nfr);
                                        if (requestResult != 0) {
                                            notifyExtPolicyFocusGrant_syncAf(nfr.toAudioFocusInfo(), requestResult);
                                        }
                                        return requestResult;
                                    }
                                    propagateFocusLossFromGain_syncAf(focusChangeHint, nfr, z2);
                                    this.mFocusStack.push(nfr);
                                    nfr.handleFocusGainFromRequest(z3 ? 1 : 0);
                                    notifyExtPolicyFocusGrant_syncAf(nfr.toAudioFocusInfo(), z3 ? 1 : 0);
                                    if ((i3 & 1) != 0) {
                                        runAudioCheckerForRingOrCallAsync(z3);
                                    }
                                    return z3 ? 1 : 0;
                                } catch (RemoteException e) {
                                    Log.w(TAG, "AudioFocus  requestAudioFocus() could not link to " + cb + " binder death");
                                    return r11;
                                }
                            } catch (Throwable th3) {
                                e = th3;
                                throw e;
                            }
                        } catch (Throwable th4) {
                            e = th4;
                            throw e;
                        }
                    } catch (Throwable th5) {
                        e = th5;
                    }
                } catch (Throwable th6) {
                    e = th6;
                }
            } catch (Throwable th7) {
                e = th7;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public int abandonAudioFocus(IAudioFocusDispatcher fl, String clientId, AudioAttributes aa, String callingPackageName) {
        new MediaMetrics.Item(mMetricsId).setUid(Binder.getCallingUid()).set(MediaMetrics.Property.CALLING_PACKAGE, callingPackageName).set(MediaMetrics.Property.CLIENT_NAME, clientId).set(MediaMetrics.Property.EVENT, "abandonAudioFocus").record();
        mEventLogger.log(new AudioEventLogger.StringEvent("abandonAudioFocus() from uid/pid " + Binder.getCallingUid() + SliceClientPermissions.SliceAuthority.DELIMITER + Binder.getCallingPid() + " clientId=" + clientId).printLog(TAG));
        try {
        } catch (ConcurrentModificationException cme) {
            Log.e(TAG, "FATAL EXCEPTION AudioFocus  abandonAudioFocus() caused " + cme);
            cme.printStackTrace();
        }
        synchronized (mAudioFocusLock) {
            if (this.mFocusPolicy != null) {
                AudioFocusInfo afi = new AudioFocusInfo(aa, Binder.getCallingUid(), clientId, callingPackageName, 0, 0, 0, 0);
                if (notifyExtFocusPolicyFocusAbandon_syncAf(afi)) {
                    return 1;
                }
            }
            boolean exitingRingOrCall = this.mRingOrCallActive & ("AudioFocus_For_Phone_Ring_And_Calls".compareTo(clientId) == 0);
            if (exitingRingOrCall) {
                this.mRingOrCallActive = false;
            }
            removeFocusStackEntry(clientId, true, true);
            if (exitingRingOrCall & true) {
                runAudioCheckerForRingOrCallAsync(false);
            }
            return 1;
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void unregisterAudioFocusClient(String clientId) {
        synchronized (mAudioFocusLock) {
            removeFocusStackEntry(clientId, false, true);
        }
    }

    /* JADX WARN: Type inference failed for: r0v0, types: [com.android.server.audio.MediaFocusControl$2] */
    private void runAudioCheckerForRingOrCallAsync(final boolean enteringRingOrCall) {
        new Thread() { // from class: com.android.server.audio.MediaFocusControl.2
            @Override // java.lang.Thread, java.lang.Runnable
            public void run() {
                int sleepTime;
                if (enteringRingOrCall) {
                    sleepTime = 100;
                } else {
                    sleepTime = MediaFocusControl.RING_CALL_UNMUTING_ENFORCEMENT_DELAY_MS;
                }
                if (MediaFocusControl.DEBUG) {
                    Log.d(MediaFocusControl.TAG, "runAudioCheckerForRingOrCallAsync sleepTime=" + sleepTime);
                }
                if (sleepTime != 0) {
                    try {
                        Thread.sleep(sleepTime);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                synchronized (MediaFocusControl.mAudioFocusLock) {
                    if (MediaFocusControl.this.mRingOrCallActive) {
                        MediaFocusControl.this.mFocusEnforcer.mutePlayersForCall(MediaFocusControl.USAGES_TO_MUTE_IN_RING_OR_CALL);
                    } else {
                        MediaFocusControl.this.mFocusEnforcer.unmutePlayersForCall();
                    }
                }
            }
        }.start();
    }

    public void updateMultiAudioFocus(boolean enabled) {
        Log.d(TAG, "updateMultiAudioFocus( " + enabled + " )");
        this.mMultiAudioFocusEnabled = enabled;
        ContentResolver cr = this.mContext.getContentResolver();
        Settings.System.putIntForUser(cr, "multi_audio_focus_enabled", enabled ? 1 : 0, cr.getUserId());
        if (!this.mFocusStack.isEmpty()) {
            FocusRequester fr = this.mFocusStack.peek();
            fr.handleFocusLoss(-1, null, false);
        }
        if (!enabled && !this.mMultiAudioFocusList.isEmpty()) {
            Iterator<FocusRequester> it = this.mMultiAudioFocusList.iterator();
            while (it.hasNext()) {
                FocusRequester multifr = it.next();
                multifr.handleFocusLoss(-1, null, false);
            }
            this.mMultiAudioFocusList.clear();
        }
    }

    public boolean getMultiAudioFocusEnabled() {
        return this.mMultiAudioFocusEnabled;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public long getFadeOutDurationOnFocusLossMillis(AudioAttributes aa) {
        return FadeOutManager.getFadeOutDurationOnFocusLossMillis(aa);
    }

    private void dumpMultiAudioFocus(PrintWriter pw) {
        pw.println("Multi Audio Focus enabled :" + this.mMultiAudioFocusEnabled);
        if (!this.mMultiAudioFocusList.isEmpty()) {
            pw.println("Multi Audio Focus List:");
            pw.println("------------------------------");
            Iterator<FocusRequester> it = this.mMultiAudioFocusList.iterator();
            while (it.hasNext()) {
                FocusRequester multifr = it.next();
                multifr.dump(pw);
            }
            pw.println("------------------------------");
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void postDelayedLossAfterFade(FocusRequester focusLoser, long delayMs) {
        if (DEBUG) {
            Log.v(TAG, "postDelayedLossAfterFade loser=" + focusLoser.getPackageName() + ", isInFocusLossLimbo=" + focusLoser.isInFocusLossLimbo() + "delayMs=" + delayMs);
        }
        Handler handler = this.mFocusHandler;
        handler.sendMessageDelayed(handler.obtainMessage(1, focusLoser), 1000L);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void postForgetUidLater(int uid) {
        Handler handler = this.mFocusHandler;
        handler.sendMessageDelayed(handler.obtainMessage(2, new ForgetFadeUidInfo(uid)), 2000L);
    }

    private void initFocusThreading() {
        HandlerThread handlerThread = new HandlerThread(TAG);
        this.mFocusThread = handlerThread;
        handlerThread.start();
        this.mFocusHandler = new Handler(this.mFocusThread.getLooper()) { // from class: com.android.server.audio.MediaFocusControl.3
            @Override // android.os.Handler
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 1:
                        if (MediaFocusControl.DEBUG) {
                            Log.d(MediaFocusControl.TAG, "MSG_L_FOCUS_LOSS_AFTER_FADE loser=" + ((FocusRequester) msg.obj).getPackageName());
                        }
                        synchronized (MediaFocusControl.mAudioFocusLock) {
                            FocusRequester loser = (FocusRequester) msg.obj;
                            if (loser.isInFocusLossLimbo()) {
                                loser.dispatchFocusChange(-1);
                                MediaFocusControl.this.mFocusEnforcer.restoreVShapedPlayers((FocusRequester) msg.obj);
                                loser.release();
                                MediaFocusControl.this.postForgetUidLater(loser.getClientUid());
                            }
                        }
                        return;
                    case 2:
                        int uid = ((ForgetFadeUidInfo) msg.obj).mUid;
                        if (MediaFocusControl.DEBUG) {
                            Log.d(MediaFocusControl.TAG, "MSL_L_FORGET_UID uid=" + uid);
                        }
                        MediaFocusControl.this.mFocusEnforcer.forgetUid(uid);
                        return;
                    default:
                        return;
                }
            }
        };
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static final class ForgetFadeUidInfo {
        private final int mUid;

        ForgetFadeUidInfo(int uid) {
            this.mUid = uid;
        }

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            ForgetFadeUidInfo f = (ForgetFadeUidInfo) o;
            if (f.mUid == this.mUid) {
                return true;
            }
            return false;
        }

        public int hashCode() {
            return this.mUid;
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public String getCurrentAudioFocusPackageName() {
        synchronized (mAudioFocusLock) {
            if (this.mFocusStack.empty()) {
                return null;
            }
            return this.mFocusStack.peek().getGainPackageName();
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public int getCurrentAudioFocusUid() {
        synchronized (mAudioFocusLock) {
            if (this.mFocusStack.empty()) {
                return 0;
            }
            return this.mFocusStack.peek().getCallingUid();
        }
    }
}
