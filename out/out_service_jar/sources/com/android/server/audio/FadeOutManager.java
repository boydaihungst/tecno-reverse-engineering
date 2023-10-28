package com.android.server.audio;

import android.media.AudioAttributes;
import android.media.AudioPlaybackConfiguration;
import android.media.VolumeShaper;
import android.util.Log;
import com.android.internal.util.ArrayUtils;
import com.android.server.audio.AudioEventLogger;
import com.android.server.audio.PlaybackActivityMonitor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
/* loaded from: classes.dex */
public final class FadeOutManager {
    private static final boolean DEBUG = false;
    static final long DELAY_FADE_IN_OFFENDERS_MS = 2000;
    private static final int[] FADEABLE_USAGES;
    private static final VolumeShaper.Configuration FADEOUT_VSHAPE = new VolumeShaper.Configuration.Builder().setId(2).setCurve(new float[]{0.0f, 0.25f, 1.0f}, new float[]{1.0f, 0.65f, 0.0f}).setOptionFlags(2).setDuration(1000).build();
    static final long FADE_OUT_DURATION_MS = 1000;
    private static final VolumeShaper.Operation PLAY_CREATE_IF_NEEDED;
    private static final VolumeShaper.Operation PLAY_SKIP_RAMP;
    public static final String TAG = "AudioService.FadeOutManager";
    private static final int[] UNFADEABLE_CONTENT_TYPES;
    private static final int[] UNFADEABLE_PLAYER_TYPES;
    private final HashMap<Integer, FadedOutApp> mFadedApps = new HashMap<>();

    static {
        VolumeShaper.Operation build = new VolumeShaper.Operation.Builder(VolumeShaper.Operation.PLAY).createIfNeeded().build();
        PLAY_CREATE_IF_NEEDED = build;
        UNFADEABLE_PLAYER_TYPES = new int[]{13, 3};
        UNFADEABLE_CONTENT_TYPES = new int[]{1};
        FADEABLE_USAGES = new int[]{14, 1};
        PLAY_SKIP_RAMP = new VolumeShaper.Operation.Builder(build).setXOffset(1.0f).build();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static boolean canCauseFadeOut(FocusRequester requester, FocusRequester loser) {
        return requester.getAudioAttributes().getContentType() != 1 && (loser.getGrantFlags() & 2) == 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static boolean canBeFadedOut(AudioPlaybackConfiguration apc) {
        return (ArrayUtils.contains(UNFADEABLE_PLAYER_TYPES, apc.getPlayerType()) || ArrayUtils.contains(UNFADEABLE_CONTENT_TYPES, apc.getAudioAttributes().getContentType()) || !ArrayUtils.contains(FADEABLE_USAGES, apc.getAudioAttributes().getUsage())) ? false : true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static long getFadeOutDurationOnFocusLossMillis(AudioAttributes aa) {
        return (!ArrayUtils.contains(UNFADEABLE_CONTENT_TYPES, aa.getContentType()) && ArrayUtils.contains(FADEABLE_USAGES, aa.getUsage())) ? 1000L : 0L;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public synchronized void fadeOutUid(int uid, ArrayList<AudioPlaybackConfiguration> players) {
        Log.i(TAG, "fadeOutUid() uid:" + uid);
        if (!this.mFadedApps.containsKey(Integer.valueOf(uid))) {
            this.mFadedApps.put(Integer.valueOf(uid), new FadedOutApp(uid));
        }
        FadedOutApp fa = this.mFadedApps.get(Integer.valueOf(uid));
        Iterator<AudioPlaybackConfiguration> it = players.iterator();
        while (it.hasNext()) {
            AudioPlaybackConfiguration apc = it.next();
            fa.addFade(apc, false);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public synchronized void unfadeOutUid(int uid, HashMap<Integer, AudioPlaybackConfiguration> players) {
        Log.i(TAG, "unfadeOutUid() uid:" + uid);
        FadedOutApp fa = this.mFadedApps.remove(Integer.valueOf(uid));
        if (fa == null) {
            return;
        }
        fa.removeUnfadeAll(players);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public synchronized void checkFade(AudioPlaybackConfiguration apc) {
        FadedOutApp fa = this.mFadedApps.get(Integer.valueOf(apc.getClientUid()));
        if (fa == null) {
            return;
        }
        fa.addFade(apc, true);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public synchronized void removeReleased(AudioPlaybackConfiguration apc) {
        int uid = apc.getClientUid();
        FadedOutApp fa = this.mFadedApps.get(Integer.valueOf(uid));
        if (fa == null) {
            return;
        }
        fa.removeReleased(apc);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public synchronized void dump(PrintWriter pw) {
        for (FadedOutApp da : this.mFadedApps.values()) {
            da.dump(pw);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static final class FadedOutApp {
        private final ArrayList<Integer> mFadedPlayers = new ArrayList<>();
        private final int mUid;

        FadedOutApp(int uid) {
            this.mUid = uid;
        }

        void dump(PrintWriter pw) {
            pw.print("\t uid:" + this.mUid + " piids:");
            Iterator<Integer> it = this.mFadedPlayers.iterator();
            while (it.hasNext()) {
                int piid = it.next().intValue();
                pw.print(" " + piid);
            }
            pw.println("");
        }

        void addFade(AudioPlaybackConfiguration apc, boolean skipRamp) {
            int piid = new Integer(apc.getPlayerInterfaceId()).intValue();
            if (this.mFadedPlayers.contains(Integer.valueOf(piid))) {
                return;
            }
            try {
                PlaybackActivityMonitor.sEventLogger.log(new PlaybackActivityMonitor.FadeOutEvent(apc, skipRamp).printLog(FadeOutManager.TAG));
                apc.getPlayerProxy().applyVolumeShaper(FadeOutManager.FADEOUT_VSHAPE, skipRamp ? FadeOutManager.PLAY_SKIP_RAMP : FadeOutManager.PLAY_CREATE_IF_NEEDED);
                this.mFadedPlayers.add(Integer.valueOf(piid));
            } catch (Exception e) {
                Log.e(FadeOutManager.TAG, "Error fading out player piid:" + piid + " uid:" + apc.getClientUid(), e);
            }
        }

        void removeUnfadeAll(HashMap<Integer, AudioPlaybackConfiguration> players) {
            Iterator<Integer> it = this.mFadedPlayers.iterator();
            while (it.hasNext()) {
                int piid = it.next().intValue();
                AudioPlaybackConfiguration apc = players.get(Integer.valueOf(piid));
                if (apc != null) {
                    try {
                        PlaybackActivityMonitor.sEventLogger.log(new AudioEventLogger.StringEvent("unfading out piid:" + piid).printLog(FadeOutManager.TAG));
                        apc.getPlayerProxy().applyVolumeShaper(FadeOutManager.FADEOUT_VSHAPE, VolumeShaper.Operation.REVERSE);
                    } catch (Exception e) {
                        Log.e(FadeOutManager.TAG, "Error unfading out player piid:" + piid + " uid:" + this.mUid, e);
                    }
                }
            }
            this.mFadedPlayers.clear();
        }

        void removeReleased(AudioPlaybackConfiguration apc) {
            this.mFadedPlayers.remove(new Integer(apc.getPlayerInterfaceId()));
        }
    }
}
