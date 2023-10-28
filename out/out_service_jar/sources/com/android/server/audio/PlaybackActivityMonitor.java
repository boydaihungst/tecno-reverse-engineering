package com.android.server.audio;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioDeviceAttributes;
import android.media.AudioPlaybackConfiguration;
import android.media.AudioSystem;
import android.media.IPlaybackConfigDispatcher;
import android.media.PlayerBase;
import android.media.VolumeShaper;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import com.android.internal.util.ArrayUtils;
import com.android.server.audio.AudioEventLogger;
import com.android.server.slice.SliceClientPermissions;
import com.transsion.hubcore.server.utils.ITranMediaUtils;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
/* loaded from: classes.dex */
public final class PlaybackActivityMonitor implements AudioPlaybackConfiguration.PlayerDeathMonitor, PlayerFocusEnforcer {
    static final boolean DEBUG = false;
    private static final int FLAGS_FOR_SILENCE_OVERRIDE = 192;
    private static final int MSG_L_TIMEOUT_MUTE_AWAIT_CONNECTION = 1;
    private static final VolumeShaper.Configuration MUTE_AWAIT_CONNECTION_VSHAPE;
    private static final VolumeShaper.Operation PLAY_CREATE_IF_NEEDED;
    private static final VolumeShaper.Operation PLAY_SKIP_RAMP;
    public static final String TAG = "AudioService.PlaybackActivityMonitor";
    private static final int[] UNDUCKABLE_PLAYER_TYPES;
    private static final long UNMUTE_DURATION_MS = 100;
    static final int VOLUME_SHAPER_SYSTEM_DUCK_ID = 1;
    static final int VOLUME_SHAPER_SYSTEM_FADEOUT_ID = 2;
    static final int VOLUME_SHAPER_SYSTEM_MUTE_AWAIT_CONNECTION_ID = 3;
    static final AudioEventLogger sEventLogger;
    private final Context mContext;
    private Handler mEventHandler;
    private HandlerThread mEventThread;
    private final int mMaxAlarmVolume;
    private final Consumer<AudioDeviceAttributes> mMuteAwaitConnectionTimeoutCb;
    private static final VolumeShaper.Configuration DUCK_VSHAPE = new VolumeShaper.Configuration.Builder().setId(1).setCurve(new float[]{0.0f, 1.0f}, new float[]{1.0f, 0.2f}).setOptionFlags(2).setDuration(MediaFocusControl.getFocusRampTimeMs(3, new AudioAttributes.Builder().setUsage(5).build())).build();
    private static final VolumeShaper.Configuration DUCK_ID = new VolumeShaper.Configuration(1);
    private final ArrayList<PlayMonitorClient> mClients = new ArrayList<>();
    private boolean mHasPublicClients = false;
    private final Object mPlayerLock = new Object();
    private final HashMap<Integer, AudioPlaybackConfiguration> mPlayers = new HashMap<>();
    private int mSavedAlarmVolume = -1;
    private int mPrivilegedAlarmActiveCount = 0;
    private final ArrayList<Integer> mBannedUids = new ArrayList<>();
    private final HashMap<Integer, Integer> mAllowedCapturePolicies = new HashMap<>();
    private final ArrayList<Integer> mMutedPlayers = new ArrayList<>();
    private final DuckingManager mDuckingManager = new DuckingManager();
    private final FadeOutManager mFadingManager = new FadeOutManager();
    private final ArrayList<Integer> mMutedPlayersAwaitingConnection = new ArrayList<>();
    private int[] mMutedUsagesAwaitingConnection = null;

    static {
        VolumeShaper.Operation build = new VolumeShaper.Operation.Builder(VolumeShaper.Operation.PLAY).createIfNeeded().build();
        PLAY_CREATE_IF_NEEDED = build;
        MUTE_AWAIT_CONNECTION_VSHAPE = new VolumeShaper.Configuration.Builder().setId(3).setCurve(new float[]{0.0f, 1.0f}, new float[]{1.0f, 0.0f}).setOptionFlags(2).setDuration(UNMUTE_DURATION_MS).build();
        UNDUCKABLE_PLAYER_TYPES = new int[]{13, 3};
        PLAY_SKIP_RAMP = new VolumeShaper.Operation.Builder(build).setXOffset(1.0f).build();
        sEventLogger = new AudioEventLogger(100, "playback activity as reported through PlayerBase");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public PlaybackActivityMonitor(Context context, int maxAlarmVolume, Consumer<AudioDeviceAttributes> muteTimeoutCallback) {
        this.mContext = context;
        this.mMaxAlarmVolume = maxAlarmVolume;
        PlayMonitorClient.sListenerDeathMonitor = this;
        AudioPlaybackConfiguration.sPlayerDeathMonitor = this;
        this.mMuteAwaitConnectionTimeoutCb = muteTimeoutCallback;
        initEventHandler();
    }

    public void disableAudioForUid(boolean disable, int uid) {
        synchronized (this.mPlayerLock) {
            int index = this.mBannedUids.indexOf(new Integer(uid));
            if (index >= 0) {
                if (!disable) {
                    this.mBannedUids.remove(index);
                }
            } else if (disable) {
                for (AudioPlaybackConfiguration apc : this.mPlayers.values()) {
                    checkBanPlayer(apc, uid);
                }
                this.mBannedUids.add(new Integer(uid));
            }
        }
    }

    private boolean checkBanPlayer(AudioPlaybackConfiguration apc, int uid) {
        boolean toBan = apc.getClientUid() == uid;
        if (toBan) {
            int piid = apc.getPlayerInterfaceId();
            try {
                Log.v(TAG, "banning player " + piid + " uid:" + uid);
                apc.getPlayerProxy().pause();
            } catch (Exception e) {
                Log.e(TAG, "error banning player " + piid + " uid:" + uid, e);
            }
        }
        return toBan;
    }

    public int trackPlayer(PlayerBase.PlayerIdCard pic) {
        int newPiid = AudioSystem.newAudioPlayerId();
        AudioPlaybackConfiguration apc = new AudioPlaybackConfiguration(pic, newPiid, Binder.getCallingUid(), Binder.getCallingPid());
        apc.init();
        synchronized (this.mAllowedCapturePolicies) {
            int uid = apc.getClientUid();
            if (this.mAllowedCapturePolicies.containsKey(Integer.valueOf(uid))) {
                updateAllowedCapturePolicy(apc, this.mAllowedCapturePolicies.get(Integer.valueOf(uid)).intValue());
            }
        }
        sEventLogger.log(new NewPlayerEvent(apc));
        synchronized (this.mPlayerLock) {
            this.mPlayers.put(Integer.valueOf(newPiid), apc);
            maybeMutePlayerAwaitingConnection(apc);
        }
        return newPiid;
    }

    public void playerAttributes(int piid, AudioAttributes attr, int binderUid) {
        boolean change;
        synchronized (this.mAllowedCapturePolicies) {
            if (this.mAllowedCapturePolicies.containsKey(Integer.valueOf(binderUid)) && attr.getAllowedCapturePolicy() < this.mAllowedCapturePolicies.get(Integer.valueOf(binderUid)).intValue()) {
                attr = new AudioAttributes.Builder(attr).setAllowedCapturePolicy(this.mAllowedCapturePolicies.get(Integer.valueOf(binderUid)).intValue()).build();
            }
        }
        synchronized (this.mPlayerLock) {
            AudioPlaybackConfiguration apc = this.mPlayers.get(new Integer(piid));
            if (checkConfigurationCaller(piid, apc, binderUid)) {
                sEventLogger.log(new AudioAttrEvent(piid, attr));
                change = apc.handleAudioAttributesEvent(attr);
            } else {
                Log.e(TAG, "Error updating audio attributes");
                change = false;
            }
        }
        if (change) {
            dispatchPlaybackChange(false);
        }
    }

    public void playerSessionId(int piid, int sessionId, int binderUid) {
        boolean change;
        synchronized (this.mPlayerLock) {
            AudioPlaybackConfiguration apc = this.mPlayers.get(new Integer(piid));
            if (checkConfigurationCaller(piid, apc, binderUid)) {
                change = apc.handleSessionIdEvent(sessionId);
            } else {
                Log.e(TAG, "Error updating audio session");
                change = false;
            }
        }
        if (change) {
            dispatchPlaybackChange(false);
        }
    }

    private void checkVolumeForPrivilegedAlarm(AudioPlaybackConfiguration apc, int event) {
        if (event == 5) {
            return;
        }
        if ((event == 2 || apc.getPlayerState() == 2) && (apc.getAudioAttributes().getAllFlags() & 192) == 192 && apc.getAudioAttributes().getUsage() == 4 && this.mContext.checkPermission("android.permission.MODIFY_PHONE_STATE", apc.getClientPid(), apc.getClientUid()) == 0) {
            if (event == 2 && apc.getPlayerState() != 2) {
                int i = this.mPrivilegedAlarmActiveCount;
                this.mPrivilegedAlarmActiveCount = i + 1;
                if (i == 0) {
                    this.mSavedAlarmVolume = AudioSystem.getStreamVolumeIndex(4, 2);
                    AudioSystem.setStreamVolumeIndexAS(4, this.mMaxAlarmVolume, 2);
                }
            } else if (event != 2 && apc.getPlayerState() == 2) {
                int i2 = this.mPrivilegedAlarmActiveCount - 1;
                this.mPrivilegedAlarmActiveCount = i2;
                if (i2 == 0 && AudioSystem.getStreamVolumeIndex(4, 2) == this.mMaxAlarmVolume) {
                    AudioSystem.setStreamVolumeIndexAS(4, this.mSavedAlarmVolume, 2);
                }
            }
        }
    }

    public void playerEvent(int piid, int event, int deviceId, int binderUid) {
        boolean change;
        synchronized (this.mPlayerLock) {
            AudioPlaybackConfiguration apc = this.mPlayers.get(new Integer(piid));
            if (apc == null) {
                return;
            }
            sEventLogger.log(new PlayerEvent(piid, event, deviceId));
            if (event == 2) {
                Iterator<Integer> it = this.mBannedUids.iterator();
                while (it.hasNext()) {
                    Integer uidInteger = it.next();
                    if (checkBanPlayer(apc, uidInteger.intValue())) {
                        sEventLogger.log(new AudioEventLogger.StringEvent("not starting piid:" + piid + " ,is banned"));
                        return;
                    }
                }
            }
            if (apc.getPlayerType() == 3) {
                return;
            }
            if (checkConfigurationCaller(piid, apc, binderUid)) {
                checkVolumeForPrivilegedAlarm(apc, event);
                change = apc.handleStateEvent(event, deviceId);
                ITranMediaUtils.Instance().hookAudioPlayerChanged(apc.getClientUid(), apc.getClientPid(), apc.getPlayerInterfaceId(), apc.getPlayerState(), apc.toString());
            } else {
                Log.e(TAG, "Error handling event " + event);
                change = false;
            }
            if (change && event == 2) {
                this.mDuckingManager.checkDuck(apc);
                this.mFadingManager.checkFade(apc);
            }
            if (change) {
                dispatchPlaybackChange(event == 0);
            }
        }
    }

    public void playerHasOpPlayAudio(int piid, boolean hasOpPlayAudio, int binderUid) {
        sEventLogger.log(new PlayerOpPlayAudioEvent(piid, hasOpPlayAudio, binderUid));
    }

    public void releasePlayer(int piid, int binderUid) {
        boolean change = false;
        synchronized (this.mPlayerLock) {
            AudioPlaybackConfiguration apc = this.mPlayers.get(new Integer(piid));
            if (checkConfigurationCaller(piid, apc, binderUid)) {
                sEventLogger.log(new AudioEventLogger.StringEvent("releasing player piid:" + piid));
                this.mPlayers.remove(new Integer(piid));
                this.mDuckingManager.removeReleased(apc);
                this.mFadingManager.removeReleased(apc);
                this.mMutedPlayersAwaitingConnection.remove(Integer.valueOf(piid));
                checkVolumeForPrivilegedAlarm(apc, 0);
                change = apc.handleStateEvent(0, 0);
                ITranMediaUtils.Instance().hookAudioPlayerChanged(apc.getClientUid(), apc.getClientPid(), apc.getPlayerInterfaceId(), apc.getPlayerState(), apc.toString());
            }
        }
        if (change) {
            dispatchPlaybackChange(true);
        }
    }

    public void setAllowedCapturePolicy(int uid, int capturePolicy) {
        synchronized (this.mAllowedCapturePolicies) {
            if (capturePolicy == 1) {
                this.mAllowedCapturePolicies.remove(Integer.valueOf(uid));
                return;
            }
            this.mAllowedCapturePolicies.put(Integer.valueOf(uid), Integer.valueOf(capturePolicy));
            synchronized (this.mPlayerLock) {
                for (AudioPlaybackConfiguration apc : this.mPlayers.values()) {
                    if (apc.getClientUid() == uid) {
                        updateAllowedCapturePolicy(apc, capturePolicy);
                    }
                }
            }
        }
    }

    public int getAllowedCapturePolicy(int uid) {
        return this.mAllowedCapturePolicies.getOrDefault(Integer.valueOf(uid), 1).intValue();
    }

    public HashMap<Integer, Integer> getAllAllowedCapturePolicies() {
        HashMap<Integer, Integer> hashMap;
        synchronized (this.mAllowedCapturePolicies) {
            hashMap = (HashMap) this.mAllowedCapturePolicies.clone();
        }
        return hashMap;
    }

    private void updateAllowedCapturePolicy(AudioPlaybackConfiguration apc, int capturePolicy) {
        AudioAttributes attr = apc.getAudioAttributes();
        if (attr.getAllowedCapturePolicy() >= capturePolicy) {
            return;
        }
        apc.handleAudioAttributesEvent(new AudioAttributes.Builder(apc.getAudioAttributes()).setAllowedCapturePolicy(capturePolicy).build());
    }

    public void playerDeath(int piid) {
        releasePlayer(piid, 0);
    }

    public boolean isPlaybackActiveForUid(int uid) {
        synchronized (this.mPlayerLock) {
            for (AudioPlaybackConfiguration apc : this.mPlayers.values()) {
                if (apc.isActive() && apc.getClientUid() == uid) {
                    return true;
                }
            }
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void dump(PrintWriter pw) {
        pw.println("\nPlaybackActivityMonitor dump time: " + DateFormat.getTimeInstance().format(new Date()));
        synchronized (this.mPlayerLock) {
            pw.println("\n  playback listeners:");
            synchronized (this.mClients) {
                Iterator<PlayMonitorClient> it = this.mClients.iterator();
                while (it.hasNext()) {
                    PlayMonitorClient pmc = it.next();
                    pw.print(" " + (pmc.mIsPrivileged ? "(S)" : "(P)") + pmc.toString());
                }
            }
            pw.println("\n");
            pw.println("\n  players:");
            List<Integer> piidIntList = new ArrayList<>(this.mPlayers.keySet());
            Collections.sort(piidIntList);
            for (Integer piidInt : piidIntList) {
                AudioPlaybackConfiguration apc = this.mPlayers.get(piidInt);
                if (apc != null) {
                    apc.dump(pw);
                }
            }
            pw.println("\n  ducked players piids:");
            this.mDuckingManager.dump(pw);
            pw.println("\n  faded out players piids:");
            this.mFadingManager.dump(pw);
            pw.print("\n  muted player piids due to call/ring:");
            Iterator<Integer> it2 = this.mMutedPlayers.iterator();
            while (it2.hasNext()) {
                int piid = it2.next().intValue();
                pw.print(" " + piid);
            }
            pw.println();
            pw.print("\n  banned uids:");
            Iterator<Integer> it3 = this.mBannedUids.iterator();
            while (it3.hasNext()) {
                int uid = it3.next().intValue();
                pw.print(" " + uid);
            }
            pw.println("\n");
            pw.print("\n  muted players (piids) awaiting device connection:");
            Iterator<Integer> it4 = this.mMutedPlayersAwaitingConnection.iterator();
            while (it4.hasNext()) {
                int piid2 = it4.next().intValue();
                pw.print(" " + piid2);
            }
            pw.println("\n");
            sEventLogger.dump(pw);
        }
        synchronized (this.mAllowedCapturePolicies) {
            pw.println("\n  allowed capture policies:");
            for (Map.Entry<Integer, Integer> entry : this.mAllowedCapturePolicies.entrySet()) {
                pw.println("  uid: " + entry.getKey() + " policy: " + entry.getValue());
            }
        }
    }

    private static boolean checkConfigurationCaller(int piid, AudioPlaybackConfiguration apc, int binderUid) {
        if (apc == null) {
            return false;
        }
        if (binderUid != 0 && apc.getClientUid() != binderUid) {
            Log.e(TAG, "Forbidden operation from uid " + binderUid + " for player " + piid);
            return false;
        }
        return true;
    }

    private void dispatchPlaybackChange(boolean iplayerReleased) {
        synchronized (this.mClients) {
            if (this.mClients.isEmpty()) {
                return;
            }
            synchronized (this.mPlayerLock) {
                if (this.mPlayers.isEmpty()) {
                    return;
                }
                List<AudioPlaybackConfiguration> configsSystem = new ArrayList<>(this.mPlayers.values());
                synchronized (this.mClients) {
                    if (this.mClients.isEmpty()) {
                        return;
                    }
                    List<AudioPlaybackConfiguration> configsPublic = this.mHasPublicClients ? anonymizeForPublicConsumption(configsSystem) : null;
                    Iterator<PlayMonitorClient> clientIterator = this.mClients.iterator();
                    while (clientIterator.hasNext()) {
                        PlayMonitorClient pmc = clientIterator.next();
                        try {
                            if (pmc.mErrorCount < 5) {
                                if (pmc.mIsPrivileged) {
                                    pmc.mDispatcherCb.dispatchPlaybackConfigChange(configsSystem, iplayerReleased);
                                } else {
                                    pmc.mDispatcherCb.dispatchPlaybackConfigChange(configsPublic, false);
                                }
                            }
                        } catch (RemoteException e) {
                            pmc.mErrorCount++;
                            Log.e(TAG, "Error (" + pmc.mErrorCount + ") trying to dispatch playback config change to " + pmc, e);
                        }
                    }
                }
            }
        }
    }

    private ArrayList<AudioPlaybackConfiguration> anonymizeForPublicConsumption(List<AudioPlaybackConfiguration> sysConfigs) {
        ArrayList<AudioPlaybackConfiguration> publicConfigs = new ArrayList<>();
        for (AudioPlaybackConfiguration config : sysConfigs) {
            if (config.isActive()) {
                publicConfigs.add(AudioPlaybackConfiguration.anonymizedCopy(config));
            }
        }
        return publicConfigs;
    }

    @Override // com.android.server.audio.PlayerFocusEnforcer
    public boolean duckPlayers(FocusRequester winner, FocusRequester loser, boolean forceDuck) {
        synchronized (this.mPlayerLock) {
            if (this.mPlayers.isEmpty()) {
                return true;
            }
            ArrayList<AudioPlaybackConfiguration> apcsToDuck = new ArrayList<>();
            for (AudioPlaybackConfiguration apc : this.mPlayers.values()) {
                if (!winner.hasSameUid(apc.getClientUid()) && loser.hasSameUid(apc.getClientUid()) && apc.getPlayerState() == 2) {
                    if (!forceDuck && apc.getAudioAttributes().getContentType() == 1) {
                        Log.v(TAG, "not ducking player " + apc.getPlayerInterfaceId() + " uid:" + apc.getClientUid() + " pid:" + apc.getClientPid() + " - SPEECH");
                        return false;
                    } else if (ArrayUtils.contains(UNDUCKABLE_PLAYER_TYPES, apc.getPlayerType())) {
                        Log.v(TAG, "not ducking player " + apc.getPlayerInterfaceId() + " uid:" + apc.getClientUid() + " pid:" + apc.getClientPid() + " due to type:" + AudioPlaybackConfiguration.toLogFriendlyPlayerType(apc.getPlayerType()));
                        return false;
                    } else {
                        apcsToDuck.add(apc);
                    }
                }
            }
            this.mDuckingManager.duckUid(loser.getClientUid(), apcsToDuck);
            return true;
        }
    }

    @Override // com.android.server.audio.PlayerFocusEnforcer
    public void restoreVShapedPlayers(FocusRequester winner) {
        synchronized (this.mPlayerLock) {
            this.mDuckingManager.unduckUid(winner.getClientUid(), this.mPlayers);
            this.mFadingManager.unfadeOutUid(winner.getClientUid(), this.mPlayers);
        }
    }

    @Override // com.android.server.audio.PlayerFocusEnforcer
    public void mutePlayersForCall(int[] usagesToMute) {
        synchronized (this.mPlayerLock) {
            Set<Integer> piidSet = this.mPlayers.keySet();
            for (Integer piid : piidSet) {
                AudioPlaybackConfiguration apc = this.mPlayers.get(piid);
                if (apc != null) {
                    int playerUsage = apc.getAudioAttributes().getUsage();
                    boolean mute = false;
                    int length = usagesToMute.length;
                    int i = 0;
                    while (true) {
                        if (i >= length) {
                            break;
                        }
                        int usageToMute = usagesToMute[i];
                        if (playerUsage != usageToMute) {
                            i++;
                        } else {
                            mute = true;
                            break;
                        }
                    }
                    if (mute) {
                        try {
                            sEventLogger.log(new AudioEventLogger.StringEvent("call: muting piid:" + piid + " uid:" + apc.getClientUid()).printLog(TAG));
                            apc.getPlayerProxy().setVolume(0.0f);
                            this.mMutedPlayers.add(new Integer(piid.intValue()));
                        } catch (Exception e) {
                            Log.e(TAG, "call: error muting player " + piid, e);
                        }
                    }
                }
            }
        }
    }

    @Override // com.android.server.audio.PlayerFocusEnforcer
    public void unmutePlayersForCall() {
        synchronized (this.mPlayerLock) {
            if (this.mMutedPlayers.isEmpty()) {
                return;
            }
            Iterator<Integer> it = this.mMutedPlayers.iterator();
            while (it.hasNext()) {
                int piid = it.next().intValue();
                AudioPlaybackConfiguration apc = this.mPlayers.get(Integer.valueOf(piid));
                if (apc != null) {
                    try {
                        sEventLogger.log(new AudioEventLogger.StringEvent("call: unmuting piid:" + piid).printLog(TAG));
                        apc.getPlayerProxy().setVolume(1.0f);
                    } catch (Exception e) {
                        Log.e(TAG, "call: error unmuting player " + piid + " uid:" + apc.getClientUid(), e);
                    }
                }
            }
            this.mMutedPlayers.clear();
        }
    }

    @Override // com.android.server.audio.PlayerFocusEnforcer
    public boolean fadeOutPlayers(FocusRequester winner, FocusRequester loser) {
        boolean loserHasActivePlayers = false;
        synchronized (this.mPlayerLock) {
            if (this.mPlayers.isEmpty()) {
                return false;
            }
            if (FadeOutManager.canCauseFadeOut(winner, loser)) {
                ArrayList<AudioPlaybackConfiguration> apcsToFadeOut = new ArrayList<>();
                for (AudioPlaybackConfiguration apc : this.mPlayers.values()) {
                    if (!winner.hasSameUid(apc.getClientUid()) && loser.hasSameUid(apc.getClientUid()) && apc.getPlayerState() == 2) {
                        if (!FadeOutManager.canBeFadedOut(apc)) {
                            Log.v(TAG, "not fading out player " + apc.getPlayerInterfaceId() + " uid:" + apc.getClientUid() + " pid:" + apc.getClientPid() + " type:" + AudioPlaybackConfiguration.toLogFriendlyPlayerType(apc.getPlayerType()) + " attr:" + apc.getAudioAttributes());
                            return false;
                        }
                        loserHasActivePlayers = true;
                        apcsToFadeOut.add(apc);
                    }
                }
                if (loserHasActivePlayers) {
                    this.mFadingManager.fadeOutUid(loser.getClientUid(), apcsToFadeOut);
                }
                return loserHasActivePlayers;
            }
            return false;
        }
    }

    @Override // com.android.server.audio.PlayerFocusEnforcer
    public void forgetUid(int uid) {
        HashMap<Integer, AudioPlaybackConfiguration> players;
        synchronized (this.mPlayerLock) {
            players = (HashMap) this.mPlayers.clone();
        }
        this.mFadingManager.unfadeOutUid(uid, players);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void registerPlaybackCallback(IPlaybackConfigDispatcher pcdb, boolean isPrivileged) {
        if (pcdb == null) {
            return;
        }
        synchronized (this.mClients) {
            PlayMonitorClient pmc = new PlayMonitorClient(pcdb, isPrivileged);
            if (pmc.init()) {
                if (!isPrivileged) {
                    this.mHasPublicClients = true;
                }
                this.mClients.add(pmc);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void unregisterPlaybackCallback(IPlaybackConfigDispatcher pcdb) {
        if (pcdb == null) {
            return;
        }
        synchronized (this.mClients) {
            Iterator<PlayMonitorClient> clientIterator = this.mClients.iterator();
            boolean hasPublicClients = false;
            while (clientIterator.hasNext()) {
                PlayMonitorClient pmc = clientIterator.next();
                if (pcdb.asBinder().equals(pmc.mDispatcherCb.asBinder())) {
                    pmc.release();
                    clientIterator.remove();
                } else if (!pmc.mIsPrivileged) {
                    hasPublicClients = true;
                }
            }
            this.mHasPublicClients = hasPublicClients;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public List<AudioPlaybackConfiguration> getActivePlaybackConfigurations(boolean isPrivileged) {
        List<AudioPlaybackConfiguration> configsPublic;
        synchronized (this.mPlayers) {
            if (isPrivileged) {
                return new ArrayList(this.mPlayers.values());
            }
            synchronized (this.mPlayerLock) {
                configsPublic = anonymizeForPublicConsumption(new ArrayList(this.mPlayers.values()));
            }
            return configsPublic;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static final class PlayMonitorClient implements IBinder.DeathRecipient {
        static final int MAX_ERRORS = 5;
        static PlaybackActivityMonitor sListenerDeathMonitor;
        final IPlaybackConfigDispatcher mDispatcherCb;
        int mErrorCount = 0;
        final boolean mIsPrivileged;

        PlayMonitorClient(IPlaybackConfigDispatcher pcdb, boolean isPrivileged) {
            this.mDispatcherCb = pcdb;
            this.mIsPrivileged = isPrivileged;
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            Log.w(PlaybackActivityMonitor.TAG, "client died");
            sListenerDeathMonitor.unregisterPlaybackCallback(this.mDispatcherCb);
        }

        boolean init() {
            try {
                this.mDispatcherCb.asBinder().linkToDeath(this, 0);
                return true;
            } catch (RemoteException e) {
                Log.w(PlaybackActivityMonitor.TAG, "Could not link to client death", e);
                return false;
            }
        }

        void release() {
            this.mDispatcherCb.asBinder().unlinkToDeath(this, 0);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static final class DuckingManager {
        private final HashMap<Integer, DuckedApp> mDuckers;

        private DuckingManager() {
            this.mDuckers = new HashMap<>();
        }

        synchronized void duckUid(int uid, ArrayList<AudioPlaybackConfiguration> apcsToDuck) {
            if (!this.mDuckers.containsKey(Integer.valueOf(uid))) {
                this.mDuckers.put(Integer.valueOf(uid), new DuckedApp(uid));
            }
            DuckedApp da = this.mDuckers.get(Integer.valueOf(uid));
            Iterator<AudioPlaybackConfiguration> it = apcsToDuck.iterator();
            while (it.hasNext()) {
                AudioPlaybackConfiguration apc = it.next();
                da.addDuck(apc, false);
            }
        }

        synchronized void unduckUid(int uid, HashMap<Integer, AudioPlaybackConfiguration> players) {
            DuckedApp da = this.mDuckers.remove(Integer.valueOf(uid));
            if (da == null) {
                return;
            }
            da.removeUnduckAll(players);
        }

        synchronized void checkDuck(AudioPlaybackConfiguration apc) {
            DuckedApp da = this.mDuckers.get(Integer.valueOf(apc.getClientUid()));
            if (da == null) {
                return;
            }
            da.addDuck(apc, true);
        }

        synchronized void dump(PrintWriter pw) {
            for (DuckedApp da : this.mDuckers.values()) {
                da.dump(pw);
            }
        }

        synchronized void removeReleased(AudioPlaybackConfiguration apc) {
            int uid = apc.getClientUid();
            DuckedApp da = this.mDuckers.get(Integer.valueOf(uid));
            if (da == null) {
                return;
            }
            da.removeReleased(apc);
        }

        /* JADX INFO: Access modifiers changed from: private */
        /* loaded from: classes.dex */
        public static final class DuckedApp {
            private final ArrayList<Integer> mDuckedPlayers = new ArrayList<>();
            private final int mUid;

            DuckedApp(int uid) {
                this.mUid = uid;
            }

            void dump(PrintWriter pw) {
                pw.print("\t uid:" + this.mUid + " piids:");
                Iterator<Integer> it = this.mDuckedPlayers.iterator();
                while (it.hasNext()) {
                    int piid = it.next().intValue();
                    pw.print(" " + piid);
                }
                pw.println("");
            }

            void addDuck(AudioPlaybackConfiguration apc, boolean skipRamp) {
                int piid = new Integer(apc.getPlayerInterfaceId()).intValue();
                if (!this.mDuckedPlayers.contains(Integer.valueOf(piid))) {
                    try {
                        PlaybackActivityMonitor.sEventLogger.log(new DuckEvent(apc, skipRamp).printLog(PlaybackActivityMonitor.TAG));
                        apc.getPlayerProxy().applyVolumeShaper(PlaybackActivityMonitor.DUCK_VSHAPE, skipRamp ? PlaybackActivityMonitor.PLAY_SKIP_RAMP : PlaybackActivityMonitor.PLAY_CREATE_IF_NEEDED);
                        this.mDuckedPlayers.add(Integer.valueOf(piid));
                    } catch (Exception e) {
                        Log.e(PlaybackActivityMonitor.TAG, "Error ducking player piid:" + piid + " uid:" + this.mUid, e);
                    }
                }
            }

            void removeUnduckAll(HashMap<Integer, AudioPlaybackConfiguration> players) {
                Iterator<Integer> it = this.mDuckedPlayers.iterator();
                while (it.hasNext()) {
                    int piid = it.next().intValue();
                    AudioPlaybackConfiguration apc = players.get(Integer.valueOf(piid));
                    if (apc != null) {
                        try {
                            PlaybackActivityMonitor.sEventLogger.log(new AudioEventLogger.StringEvent("unducking piid:" + piid).printLog(PlaybackActivityMonitor.TAG));
                            apc.getPlayerProxy().applyVolumeShaper(PlaybackActivityMonitor.DUCK_ID, VolumeShaper.Operation.REVERSE);
                        } catch (Exception e) {
                            Log.e(PlaybackActivityMonitor.TAG, "Error unducking player piid:" + piid + " uid:" + this.mUid, e);
                        }
                    }
                }
                this.mDuckedPlayers.clear();
            }

            void removeReleased(AudioPlaybackConfiguration apc) {
                this.mDuckedPlayers.remove(new Integer(apc.getPlayerInterfaceId()));
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static final class PlayerEvent extends AudioEventLogger.Event {
        final int mDeviceId;
        final int mPlayerIId;
        final int mState;

        PlayerEvent(int piid, int state, int deviceId) {
            this.mPlayerIId = piid;
            this.mState = state;
            this.mDeviceId = deviceId;
        }

        @Override // com.android.server.audio.AudioEventLogger.Event
        public String eventToString() {
            return "player piid:" + this.mPlayerIId + " state:" + AudioPlaybackConfiguration.toLogFriendlyPlayerState(this.mState) + " DeviceId:" + this.mDeviceId;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static final class PlayerOpPlayAudioEvent extends AudioEventLogger.Event {
        final boolean mHasOp;
        final int mPlayerIId;
        final int mUid;

        PlayerOpPlayAudioEvent(int piid, boolean hasOp, int uid) {
            this.mPlayerIId = piid;
            this.mHasOp = hasOp;
            this.mUid = uid;
        }

        @Override // com.android.server.audio.AudioEventLogger.Event
        public String eventToString() {
            return "player piid:" + this.mPlayerIId + " has OP_PLAY_AUDIO:" + this.mHasOp + " in uid:" + this.mUid;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static final class NewPlayerEvent extends AudioEventLogger.Event {
        private final int mClientPid;
        private final int mClientUid;
        private final AudioAttributes mPlayerAttr;
        private final int mPlayerIId;
        private final int mPlayerType;
        private final int mSessionId;

        NewPlayerEvent(AudioPlaybackConfiguration apc) {
            this.mPlayerIId = apc.getPlayerInterfaceId();
            this.mPlayerType = apc.getPlayerType();
            this.mClientUid = apc.getClientUid();
            this.mClientPid = apc.getClientPid();
            this.mPlayerAttr = apc.getAudioAttributes();
            this.mSessionId = apc.getSessionId();
        }

        @Override // com.android.server.audio.AudioEventLogger.Event
        public String eventToString() {
            return new String("new player piid:" + this.mPlayerIId + " uid/pid:" + this.mClientUid + SliceClientPermissions.SliceAuthority.DELIMITER + this.mClientPid + " type:" + AudioPlaybackConfiguration.toLogFriendlyPlayerType(this.mPlayerType) + " attr:" + this.mPlayerAttr + " session:" + this.mSessionId);
        }
    }

    /* loaded from: classes.dex */
    private static abstract class VolumeShaperEvent extends AudioEventLogger.Event {
        private final int mClientPid;
        private final int mClientUid;
        private final int mPlayerIId;
        private final boolean mSkipRamp;

        abstract String getVSAction();

        VolumeShaperEvent(AudioPlaybackConfiguration apc, boolean skipRamp) {
            this.mPlayerIId = apc.getPlayerInterfaceId();
            this.mSkipRamp = skipRamp;
            this.mClientUid = apc.getClientUid();
            this.mClientPid = apc.getClientPid();
        }

        @Override // com.android.server.audio.AudioEventLogger.Event
        public String eventToString() {
            return getVSAction() + " player piid:" + this.mPlayerIId + " uid/pid:" + this.mClientUid + SliceClientPermissions.SliceAuthority.DELIMITER + this.mClientPid + " skip ramp:" + this.mSkipRamp;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static final class DuckEvent extends VolumeShaperEvent {
        @Override // com.android.server.audio.PlaybackActivityMonitor.VolumeShaperEvent
        String getVSAction() {
            return "ducking";
        }

        DuckEvent(AudioPlaybackConfiguration apc, boolean skipRamp) {
            super(apc, skipRamp);
        }
    }

    /* loaded from: classes.dex */
    static final class FadeOutEvent extends VolumeShaperEvent {
        @Override // com.android.server.audio.PlaybackActivityMonitor.VolumeShaperEvent
        String getVSAction() {
            return "fading out";
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public FadeOutEvent(AudioPlaybackConfiguration apc, boolean skipRamp) {
            super(apc, skipRamp);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static final class AudioAttrEvent extends AudioEventLogger.Event {
        private final AudioAttributes mPlayerAttr;
        private final int mPlayerIId;

        AudioAttrEvent(int piid, AudioAttributes attr) {
            this.mPlayerIId = piid;
            this.mPlayerAttr = attr;
        }

        @Override // com.android.server.audio.AudioEventLogger.Event
        public String eventToString() {
            return new String("player piid:" + this.mPlayerIId + " new AudioAttributes:" + this.mPlayerAttr);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static final class MuteAwaitConnectionEvent extends AudioEventLogger.Event {
        private final int[] mUsagesToMute;

        MuteAwaitConnectionEvent(int[] usagesToMute) {
            this.mUsagesToMute = usagesToMute;
        }

        @Override // com.android.server.audio.AudioEventLogger.Event
        public String eventToString() {
            return "muteAwaitConnection muting usages " + Arrays.toString(this.mUsagesToMute);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void muteAwaitConnection(int[] usagesToMute, AudioDeviceAttributes dev, long timeOutMs) {
        sEventLogger.loglogi("muteAwaitConnection() dev:" + dev + " timeOutMs:" + timeOutMs, TAG);
        synchronized (this.mPlayerLock) {
            mutePlayersExpectingDevice(usagesToMute);
            this.mEventHandler.removeMessages(1);
            Handler handler = this.mEventHandler;
            handler.sendMessageDelayed(handler.obtainMessage(1, dev), timeOutMs);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void cancelMuteAwaitConnection(String source) {
        sEventLogger.loglogi("cancelMuteAwaitConnection() from:" + source, TAG);
        synchronized (this.mPlayerLock) {
            this.mEventHandler.removeMessages(1);
            unmutePlayersExpectingDevice();
        }
    }

    private void mutePlayersExpectingDevice(int[] usagesToMute) {
        sEventLogger.log(new MuteAwaitConnectionEvent(usagesToMute));
        this.mMutedUsagesAwaitingConnection = usagesToMute;
        Set<Integer> piidSet = this.mPlayers.keySet();
        for (Integer piid : piidSet) {
            AudioPlaybackConfiguration apc = this.mPlayers.get(piid);
            if (apc != null) {
                maybeMutePlayerAwaitingConnection(apc);
            }
        }
    }

    private void maybeMutePlayerAwaitingConnection(AudioPlaybackConfiguration apc) {
        int[] iArr = this.mMutedUsagesAwaitingConnection;
        if (iArr == null) {
            return;
        }
        for (int usage : iArr) {
            if (usage == apc.getAudioAttributes().getUsage()) {
                try {
                    sEventLogger.log(new AudioEventLogger.StringEvent("awaiting connection: muting piid:" + apc.getPlayerInterfaceId() + " uid:" + apc.getClientUid()).printLog(TAG));
                    apc.getPlayerProxy().applyVolumeShaper(MUTE_AWAIT_CONNECTION_VSHAPE, PLAY_SKIP_RAMP);
                    this.mMutedPlayersAwaitingConnection.add(Integer.valueOf(apc.getPlayerInterfaceId()));
                } catch (Exception e) {
                    Log.e(TAG, "awaiting connection: error muting player " + apc.getPlayerInterfaceId(), e);
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void unmutePlayersExpectingDevice() {
        this.mMutedUsagesAwaitingConnection = null;
        Iterator<Integer> it = this.mMutedPlayersAwaitingConnection.iterator();
        while (it.hasNext()) {
            int piid = it.next().intValue();
            AudioPlaybackConfiguration apc = this.mPlayers.get(Integer.valueOf(piid));
            if (apc != null) {
                try {
                    sEventLogger.log(new AudioEventLogger.StringEvent("unmuting piid:" + piid).printLog(TAG));
                    apc.getPlayerProxy().applyVolumeShaper(MUTE_AWAIT_CONNECTION_VSHAPE, VolumeShaper.Operation.REVERSE);
                } catch (Exception e) {
                    Log.e(TAG, "Error unmuting player " + piid + " uid:" + apc.getClientUid(), e);
                }
            }
        }
        this.mMutedPlayersAwaitingConnection.clear();
    }

    private void initEventHandler() {
        HandlerThread handlerThread = new HandlerThread(TAG);
        this.mEventThread = handlerThread;
        handlerThread.start();
        this.mEventHandler = new Handler(this.mEventThread.getLooper()) { // from class: com.android.server.audio.PlaybackActivityMonitor.1
            @Override // android.os.Handler
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 1:
                        PlaybackActivityMonitor.sEventLogger.loglogi("Timeout for muting waiting for " + ((AudioDeviceAttributes) msg.obj) + ", unmuting", PlaybackActivityMonitor.TAG);
                        synchronized (PlaybackActivityMonitor.this.mPlayerLock) {
                            PlaybackActivityMonitor.this.unmutePlayersExpectingDevice();
                        }
                        PlaybackActivityMonitor.this.mMuteAwaitConnectionTimeoutCb.accept((AudioDeviceAttributes) msg.obj);
                        return;
                    default:
                        return;
                }
            }
        };
    }
}
