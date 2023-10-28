package android.media;

import android.annotation.SystemApi;
import android.hardware.usb.UsbManager;
import android.media.AudioAttributes;
import android.media.IPlayer;
import android.media.PlayerBase;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;
import android.util.Log;
import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;
/* loaded from: classes2.dex */
public final class AudioPlaybackConfiguration implements Parcelable {
    public static final Parcelable.Creator<AudioPlaybackConfiguration> CREATOR;
    private static final boolean DEBUG;
    public static final int PLAYER_DEVICEID_INVALID = 0;
    public static final int PLAYER_PIID_INVALID = -1;
    @SystemApi
    public static final int PLAYER_STATE_IDLE = 1;
    @SystemApi
    public static final int PLAYER_STATE_PAUSED = 3;
    @SystemApi
    public static final int PLAYER_STATE_RELEASED = 0;
    @SystemApi
    public static final int PLAYER_STATE_STARTED = 2;
    @SystemApi
    public static final int PLAYER_STATE_STOPPED = 4;
    @SystemApi
    public static final int PLAYER_STATE_UNKNOWN = -1;
    @SystemApi
    public static final int PLAYER_TYPE_AAUDIO = 13;
    public static final int PLAYER_TYPE_EXTERNAL_PROXY = 15;
    public static final int PLAYER_TYPE_HW_SOURCE = 14;
    @SystemApi
    public static final int PLAYER_TYPE_JAM_AUDIOTRACK = 1;
    @SystemApi
    public static final int PLAYER_TYPE_JAM_MEDIAPLAYER = 2;
    @SystemApi
    public static final int PLAYER_TYPE_JAM_SOUNDPOOL = 3;
    @SystemApi
    public static final int PLAYER_TYPE_SLES_AUDIOPLAYER_BUFFERQUEUE = 11;
    @SystemApi
    public static final int PLAYER_TYPE_SLES_AUDIOPLAYER_URI_FD = 12;
    @SystemApi
    public static final int PLAYER_TYPE_UNKNOWN = -1;
    public static final int PLAYER_UPDATE_DEVICE_ID = 5;
    public static final int PLAYER_UPID_INVALID = -1;
    private static final String TAG = new String("AudioPlaybackConfiguration");
    public static PlayerDeathMonitor sPlayerDeathMonitor;
    private int mClientPid;
    private int mClientUid;
    private int mDeviceId;
    private IPlayerShell mIPlayerShell;
    private AudioAttributes mPlayerAttr;
    private final int mPlayerIId;
    private int mPlayerState;
    private int mPlayerType;
    private int mSessionId;

    /* loaded from: classes2.dex */
    public interface PlayerDeathMonitor {
        void playerDeath(int i);
    }

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes2.dex */
    public @interface PlayerState {
    }

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes2.dex */
    public @interface PlayerType {
    }

    static {
        DEBUG = "eng".equals(Build.TYPE) || "userdebug".equals(Build.TYPE);
        CREATOR = new Parcelable.Creator<AudioPlaybackConfiguration>() { // from class: android.media.AudioPlaybackConfiguration.1
            /* JADX DEBUG: Method merged with bridge method */
            /* JADX WARN: Can't rename method to resolve collision */
            @Override // android.os.Parcelable.Creator
            public AudioPlaybackConfiguration createFromParcel(Parcel p) {
                return new AudioPlaybackConfiguration(p);
            }

            /* JADX DEBUG: Method merged with bridge method */
            /* JADX WARN: Can't rename method to resolve collision */
            @Override // android.os.Parcelable.Creator
            public AudioPlaybackConfiguration[] newArray(int size) {
                return new AudioPlaybackConfiguration[size];
            }
        };
    }

    public static String playerStateToString(int state) {
        switch (state) {
            case -1:
                return "PLAYER_STATE_UNKNOWN";
            case 0:
                return "PLAYER_STATE_RELEASED";
            case 1:
                return "PLAYER_STATE_IDLE";
            case 2:
                return "PLAYER_STATE_STARTED";
            case 3:
                return "PLAYER_STATE_PAUSED";
            case 4:
                return "PLAYER_STATE_STOPPED";
            case 5:
                return "PLAYER_UPDATE_DEVICE_ID";
            default:
                return "invalid state " + state;
        }
    }

    private AudioPlaybackConfiguration(int piid) {
        this.mPlayerIId = piid;
        this.mIPlayerShell = null;
    }

    public AudioPlaybackConfiguration(PlayerBase.PlayerIdCard pic, int piid, int uid, int pid) {
        if (DEBUG) {
            Log.d(TAG, "new: piid=" + piid + " iplayer=" + pic.mIPlayer + " sessionId=" + pic.mSessionId);
        }
        this.mPlayerIId = piid;
        this.mPlayerType = pic.mPlayerType;
        this.mClientUid = uid;
        this.mClientPid = pid;
        this.mDeviceId = 0;
        this.mPlayerState = 1;
        this.mPlayerAttr = pic.mAttributes;
        if (sPlayerDeathMonitor != null && pic.mIPlayer != null) {
            this.mIPlayerShell = new IPlayerShell(this, pic.mIPlayer);
        } else {
            this.mIPlayerShell = null;
        }
        this.mSessionId = pic.mSessionId;
    }

    public void init() {
        synchronized (this) {
            IPlayerShell iPlayerShell = this.mIPlayerShell;
            if (iPlayerShell != null) {
                iPlayerShell.monitorDeath();
            }
        }
    }

    public static AudioPlaybackConfiguration anonymizedCopy(AudioPlaybackConfiguration in) {
        AudioPlaybackConfiguration anonymCopy = new AudioPlaybackConfiguration(in.mPlayerIId);
        anonymCopy.mPlayerState = in.mPlayerState;
        AudioAttributes.Builder builder = new AudioAttributes.Builder().setContentType(in.mPlayerAttr.getContentType()).setFlags(in.mPlayerAttr.getFlags()).setAllowedCapturePolicy(in.mPlayerAttr.getAllowedCapturePolicy() != 1 ? 3 : 1);
        if (AudioAttributes.isSystemUsage(in.mPlayerAttr.getSystemUsage())) {
            builder.setSystemUsage(in.mPlayerAttr.getSystemUsage());
        } else {
            builder.setUsage(in.mPlayerAttr.getUsage());
        }
        anonymCopy.mPlayerAttr = builder.build();
        anonymCopy.mDeviceId = in.mDeviceId;
        anonymCopy.mPlayerType = -1;
        anonymCopy.mClientUid = -1;
        anonymCopy.mClientPid = -1;
        anonymCopy.mIPlayerShell = null;
        anonymCopy.mSessionId = 0;
        return anonymCopy;
    }

    public AudioAttributes getAudioAttributes() {
        return this.mPlayerAttr;
    }

    @SystemApi
    public int getClientUid() {
        return this.mClientUid;
    }

    @SystemApi
    public int getClientPid() {
        return this.mClientPid;
    }

    public AudioDeviceInfo getAudioDeviceInfo() {
        int i = this.mDeviceId;
        if (i == 0) {
            return null;
        }
        return AudioManager.getDeviceForPortId(i, 2);
    }

    @SystemApi
    public int getSessionId() {
        return this.mSessionId;
    }

    @SystemApi
    public int getPlayerType() {
        int i = this.mPlayerType;
        switch (i) {
            case 14:
            case 15:
                return -1;
            default:
                return i;
        }
    }

    @SystemApi
    public int getPlayerState() {
        return this.mPlayerState;
    }

    @SystemApi
    public int getPlayerInterfaceId() {
        return this.mPlayerIId;
    }

    @SystemApi
    public PlayerProxy getPlayerProxy() {
        IPlayerShell ips;
        synchronized (this) {
            ips = this.mIPlayerShell;
        }
        if (ips == null) {
            return null;
        }
        return new PlayerProxy(this);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public IPlayer getIPlayer() {
        IPlayerShell ips;
        synchronized (this) {
            ips = this.mIPlayerShell;
        }
        if (ips == null) {
            return null;
        }
        return ips.getIPlayer();
    }

    public boolean handleAudioAttributesEvent(AudioAttributes attr) {
        boolean changed = !attr.equals(this.mPlayerAttr);
        this.mPlayerAttr = attr;
        return changed;
    }

    public boolean handleSessionIdEvent(int sessionId) {
        boolean changed = sessionId != this.mSessionId;
        this.mSessionId = sessionId;
        return changed;
    }

    public boolean handleStateEvent(int event, int deviceId) {
        IPlayerShell iPlayerShell;
        boolean changed = false;
        synchronized (this) {
            boolean z = false;
            if (event != 5) {
                try {
                    changed = this.mPlayerState != event;
                    this.mPlayerState = event;
                } catch (Throwable th) {
                    throw th;
                }
            }
            if (event == 2 || event == 5) {
                if (changed || this.mDeviceId != deviceId) {
                    z = true;
                }
                changed = z;
                this.mDeviceId = deviceId;
            }
            if (changed && event == 0 && (iPlayerShell = this.mIPlayerShell) != null) {
                iPlayerShell.release();
                this.mIPlayerShell = null;
            }
        }
        return changed;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void playerDied() {
        PlayerDeathMonitor playerDeathMonitor = sPlayerDeathMonitor;
        if (playerDeathMonitor != null) {
            playerDeathMonitor.playerDeath(this.mPlayerIId);
        }
    }

    @SystemApi
    public boolean isActive() {
        switch (this.mPlayerState) {
            case 2:
                return true;
            default:
                return false;
        }
    }

    public void dump(PrintWriter pw) {
        pw.println("  " + this);
    }

    public int hashCode() {
        return Objects.hash(Integer.valueOf(this.mPlayerIId), Integer.valueOf(this.mDeviceId), Integer.valueOf(this.mPlayerType), Integer.valueOf(this.mClientUid), Integer.valueOf(this.mClientPid), Integer.valueOf(this.mSessionId));
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        IPlayerShell ips;
        dest.writeInt(this.mPlayerIId);
        dest.writeInt(this.mDeviceId);
        dest.writeInt(this.mPlayerType);
        dest.writeInt(this.mClientUid);
        dest.writeInt(this.mClientPid);
        dest.writeInt(this.mPlayerState);
        this.mPlayerAttr.writeToParcel(dest, 0);
        synchronized (this) {
            ips = this.mIPlayerShell;
        }
        dest.writeStrongInterface(ips == null ? null : ips.getIPlayer());
        dest.writeInt(this.mSessionId);
    }

    private AudioPlaybackConfiguration(Parcel in) {
        this.mPlayerIId = in.readInt();
        this.mDeviceId = in.readInt();
        this.mPlayerType = in.readInt();
        this.mClientUid = in.readInt();
        this.mClientPid = in.readInt();
        this.mPlayerState = in.readInt();
        this.mPlayerAttr = AudioAttributes.CREATOR.createFromParcel(in);
        IPlayer p = IPlayer.Stub.asInterface(in.readStrongBinder());
        this.mIPlayerShell = p != null ? new IPlayerShell(null, p) : null;
        this.mSessionId = in.readInt();
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !(o instanceof AudioPlaybackConfiguration)) {
            return false;
        }
        AudioPlaybackConfiguration that = (AudioPlaybackConfiguration) o;
        if (this.mPlayerIId == that.mPlayerIId && this.mDeviceId == that.mDeviceId && this.mPlayerType == that.mPlayerType && this.mClientUid == that.mClientUid && this.mClientPid == that.mClientPid && this.mSessionId == that.mSessionId) {
            return true;
        }
        return false;
    }

    public String toString() {
        return "AudioPlaybackConfiguration piid:" + this.mPlayerIId + " deviceId:" + this.mDeviceId + " type:" + toLogFriendlyPlayerType(this.mPlayerType) + " u/pid:" + this.mClientUid + "/" + this.mClientPid + " state:" + toLogFriendlyPlayerState(this.mPlayerState) + " attr:" + this.mPlayerAttr + " sessionId:" + this.mSessionId;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public static final class IPlayerShell implements IBinder.DeathRecipient {
        private volatile IPlayer mIPlayer;
        final AudioPlaybackConfiguration mMonitor;

        IPlayerShell(AudioPlaybackConfiguration monitor, IPlayer iplayer) {
            this.mMonitor = monitor;
            this.mIPlayer = iplayer;
        }

        synchronized void monitorDeath() {
            if (this.mIPlayer == null) {
                return;
            }
            try {
                this.mIPlayer.asBinder().linkToDeath(this, 0);
            } catch (RemoteException e) {
                if (this.mMonitor != null) {
                    Log.w(AudioPlaybackConfiguration.TAG, "Could not link to client death for piid=" + this.mMonitor.mPlayerIId, e);
                } else {
                    Log.w(AudioPlaybackConfiguration.TAG, "Could not link to client death", e);
                }
            }
        }

        IPlayer getIPlayer() {
            return this.mIPlayer;
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            if (this.mMonitor != null) {
                if (AudioPlaybackConfiguration.DEBUG) {
                    Log.i(AudioPlaybackConfiguration.TAG, "IPlayerShell binderDied for piid=" + this.mMonitor.mPlayerIId);
                }
                this.mMonitor.playerDied();
            } else if (AudioPlaybackConfiguration.DEBUG) {
                Log.i(AudioPlaybackConfiguration.TAG, "IPlayerShell binderDied");
            }
        }

        synchronized void release() {
            if (this.mIPlayer == null) {
                return;
            }
            this.mIPlayer.asBinder().unlinkToDeath(this, 0);
            this.mIPlayer = null;
            Binder.flushPendingCommands();
        }
    }

    public static String toLogFriendlyPlayerType(int type) {
        switch (type) {
            case -1:
                return "unknown";
            case 0:
            case 4:
            case 5:
            case 6:
            case 7:
            case 8:
            case 9:
            case 10:
            default:
                return "unknown player type " + type + " - FIXME";
            case 1:
                return "android.media.AudioTrack";
            case 2:
                return "android.media.MediaPlayer";
            case 3:
                return "android.media.SoundPool";
            case 11:
                return "OpenSL ES AudioPlayer (Buffer Queue)";
            case 12:
                return "OpenSL ES AudioPlayer (URI/FD)";
            case 13:
                return "AAudio";
            case 14:
                return "hardware source";
            case 15:
                return "external proxy";
        }
    }

    public static String toLogFriendlyPlayerState(int state) {
        switch (state) {
            case -1:
                return "unknown";
            case 0:
                return "released";
            case 1:
                return "idle";
            case 2:
                return "started";
            case 3:
                return "paused";
            case 4:
                return "stopped";
            case 5:
                return UsbManager.EXTRA_DEVICE;
            default:
                return "unknown player state - FIXME";
        }
    }
}
