package com.android.server.media.metrics;

import android.content.Context;
import android.content.pm.PackageManager;
import android.media.MediaMetrics;
import android.media.metrics.IMediaMetricsManager;
import android.media.metrics.NetworkEvent;
import android.media.metrics.PlaybackErrorEvent;
import android.media.metrics.PlaybackMetrics;
import android.media.metrics.PlaybackStateEvent;
import android.media.metrics.TrackChangeEvent;
import android.os.Binder;
import android.os.PersistableBundle;
import android.provider.DeviceConfig;
import android.util.Base64;
import android.util.Slog;
import android.util.StatsEvent;
import android.util.StatsLog;
import com.android.internal.util.FrameworkStatsLog;
import com.android.server.SystemService;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;
/* loaded from: classes2.dex */
public final class MediaMetricsManagerService extends SystemService {
    private static final String FAILED_TO_GET = "failed_to_get";
    private static final int LOGGING_LEVEL_BLOCKED = 99999;
    private static final int LOGGING_LEVEL_EVERYTHING = 0;
    private static final int LOGGING_LEVEL_NO_UID = 1000;
    private static final String MEDIA_METRICS_MODE = "media_metrics_mode";
    private static final int MEDIA_METRICS_MODE_ALLOWLIST = 3;
    private static final int MEDIA_METRICS_MODE_BLOCKLIST = 2;
    private static final int MEDIA_METRICS_MODE_OFF = 0;
    private static final int MEDIA_METRICS_MODE_ON = 1;
    private static final String PLAYER_METRICS_APP_ALLOWLIST = "player_metrics_app_allowlist";
    private static final String PLAYER_METRICS_APP_BLOCKLIST = "player_metrics_app_blocklist";
    private static final String PLAYER_METRICS_PER_APP_ATTRIBUTION_ALLOWLIST = "player_metrics_per_app_attribution_allowlist";
    private static final String PLAYER_METRICS_PER_APP_ATTRIBUTION_BLOCKLIST = "player_metrics_per_app_attribution_blocklist";
    private static final String TAG = "MediaMetricsManagerService";
    private static final String mMetricsId = "metrics.manager";
    private List<String> mAllowlist;
    private List<String> mBlockList;
    private final Context mContext;
    private final Object mLock;
    private Integer mMode;
    private List<String> mNoUidAllowlist;
    private List<String> mNoUidBlocklist;
    private final SecureRandom mSecureRandom;

    public MediaMetricsManagerService(Context context) {
        super(context);
        this.mMode = null;
        this.mAllowlist = null;
        this.mNoUidAllowlist = null;
        this.mBlockList = null;
        this.mNoUidBlocklist = null;
        this.mLock = new Object();
        this.mContext = context;
        this.mSecureRandom = new SecureRandom();
    }

    @Override // com.android.server.SystemService
    public void onStart() {
        publishBinderService("media_metrics", new BinderService());
        DeviceConfig.addOnPropertiesChangedListener("media", this.mContext.getMainExecutor(), new DeviceConfig.OnPropertiesChangedListener() { // from class: com.android.server.media.metrics.MediaMetricsManagerService$$ExternalSyntheticLambda0
            public final void onPropertiesChanged(DeviceConfig.Properties properties) {
                MediaMetricsManagerService.this.updateConfigs(properties);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateConfigs(DeviceConfig.Properties properties) {
        synchronized (this.mLock) {
            this.mMode = Integer.valueOf(properties.getInt(MEDIA_METRICS_MODE, 2));
            List<String> newList = getListLocked(PLAYER_METRICS_APP_ALLOWLIST);
            if (newList != null || this.mMode.intValue() != 3) {
                this.mAllowlist = newList;
            }
            List<String> newList2 = getListLocked(PLAYER_METRICS_PER_APP_ATTRIBUTION_ALLOWLIST);
            if (newList2 != null || this.mMode.intValue() != 3) {
                this.mNoUidAllowlist = newList2;
            }
            List<String> newList3 = getListLocked(PLAYER_METRICS_APP_BLOCKLIST);
            if (newList3 != null || this.mMode.intValue() != 2) {
                this.mBlockList = newList3;
            }
            List<String> newList4 = getListLocked(PLAYER_METRICS_PER_APP_ATTRIBUTION_BLOCKLIST);
            if (newList4 != null || this.mMode.intValue() != 2) {
                this.mNoUidBlocklist = newList4;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public List<String> getListLocked(String listName) {
        long identity = Binder.clearCallingIdentity();
        try {
            String listString = DeviceConfig.getString("media", listName, FAILED_TO_GET);
            Binder.restoreCallingIdentity(identity);
            if (listString.equals(FAILED_TO_GET)) {
                Slog.d(TAG, "failed to get " + listName + " from DeviceConfig");
                return null;
            }
            String[] pkgArr = listString.split(",");
            return Arrays.asList(pkgArr);
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(identity);
            throw th;
        }
    }

    /* loaded from: classes2.dex */
    private final class BinderService extends IMediaMetricsManager.Stub {
        private BinderService() {
        }

        public void reportPlaybackMetrics(String sessionId, PlaybackMetrics metrics, int userId) {
            int level = loggingLevel();
            if (level == MediaMetricsManagerService.LOGGING_LEVEL_BLOCKED) {
                return;
            }
            StatsEvent statsEvent = StatsEvent.newBuilder().setAtomId(320).writeInt(level == 0 ? Binder.getCallingUid() : 0).writeString(sessionId).writeLong(metrics.getMediaDurationMillis()).writeInt(metrics.getStreamSource()).writeInt(metrics.getStreamType()).writeInt(metrics.getPlaybackType()).writeInt(metrics.getDrmType()).writeInt(metrics.getContentType()).writeString(metrics.getPlayerName()).writeString(metrics.getPlayerVersion()).writeByteArray(new byte[0]).writeInt(metrics.getVideoFramesPlayed()).writeInt(metrics.getVideoFramesDropped()).writeInt(metrics.getAudioUnderrunCount()).writeLong(metrics.getNetworkBytesRead()).writeLong(metrics.getLocalBytesRead()).writeLong(metrics.getNetworkTransferDurationMillis()).writeString(Base64.encodeToString(metrics.getDrmSessionId(), 0)).usePooledBuffer().build();
            StatsLog.write(statsEvent);
        }

        public void reportBundleMetrics(String sessionId, PersistableBundle metrics, int userId) {
            int level = loggingLevel();
            if (level == MediaMetricsManagerService.LOGGING_LEVEL_BLOCKED) {
                return;
            }
            int atomid = metrics.getInt("bundlesession-statsd-atom");
            switch (atomid) {
                case 322:
                    String _sessionId = metrics.getString("playbackstateevent-sessionid");
                    int _state = metrics.getInt("playbackstateevent-state", -1);
                    long _lifetime = metrics.getLong("playbackstateevent-lifetime", -1L);
                    if (_sessionId == null || _state < 0 || _lifetime < 0) {
                        Slog.d(MediaMetricsManagerService.TAG, "dropping incomplete data for atom 322: _sessionId: " + _sessionId + " _state: " + _state + " _lifetime: " + _lifetime);
                        return;
                    }
                    StatsEvent statsEvent = StatsEvent.newBuilder().setAtomId(322).writeString(_sessionId).writeInt(_state).writeLong(_lifetime).usePooledBuffer().build();
                    StatsLog.write(statsEvent);
                    return;
                default:
                    return;
            }
        }

        public void reportPlaybackStateEvent(String sessionId, PlaybackStateEvent event, int userId) {
            int level = loggingLevel();
            if (level == MediaMetricsManagerService.LOGGING_LEVEL_BLOCKED) {
                return;
            }
            StatsEvent statsEvent = StatsEvent.newBuilder().setAtomId(322).writeString(sessionId).writeInt(event.getState()).writeLong(event.getTimeSinceCreatedMillis()).usePooledBuffer().build();
            StatsLog.write(statsEvent);
        }

        private String getSessionIdInternal(int userId) {
            byte[] byteId = new byte[12];
            MediaMetricsManagerService.this.mSecureRandom.nextBytes(byteId);
            String id = Base64.encodeToString(byteId, 11);
            new MediaMetrics.Item(MediaMetricsManagerService.mMetricsId).set(MediaMetrics.Property.EVENT, "create").set(MediaMetrics.Property.LOG_SESSION_ID, id).record();
            return id;
        }

        public void releaseSessionId(String sessionId, int userId) {
            Slog.v(MediaMetricsManagerService.TAG, "Releasing sessionId " + sessionId + " for userId " + userId + " [NOP]");
        }

        public String getPlaybackSessionId(int userId) {
            return getSessionIdInternal(userId);
        }

        public String getRecordingSessionId(int userId) {
            return getSessionIdInternal(userId);
        }

        public String getTranscodingSessionId(int userId) {
            return getSessionIdInternal(userId);
        }

        public String getEditingSessionId(int userId) {
            return getSessionIdInternal(userId);
        }

        public String getBundleSessionId(int userId) {
            return getSessionIdInternal(userId);
        }

        public void reportPlaybackErrorEvent(String sessionId, PlaybackErrorEvent event, int userId) {
            int level = loggingLevel();
            if (level == MediaMetricsManagerService.LOGGING_LEVEL_BLOCKED) {
                return;
            }
            StatsEvent statsEvent = StatsEvent.newBuilder().setAtomId(323).writeString(sessionId).writeString(event.getExceptionStack()).writeInt(event.getErrorCode()).writeInt(event.getSubErrorCode()).writeLong(event.getTimeSinceCreatedMillis()).usePooledBuffer().build();
            StatsLog.write(statsEvent);
        }

        public void reportNetworkEvent(String sessionId, NetworkEvent event, int userId) {
            int level = loggingLevel();
            if (level == MediaMetricsManagerService.LOGGING_LEVEL_BLOCKED) {
                return;
            }
            StatsEvent statsEvent = StatsEvent.newBuilder().setAtomId(321).writeString(sessionId).writeInt(event.getNetworkType()).writeLong(event.getTimeSinceCreatedMillis()).usePooledBuffer().build();
            StatsLog.write(statsEvent);
        }

        public void reportTrackChangeEvent(String sessionId, TrackChangeEvent event, int userId) {
            int level = loggingLevel();
            if (level == MediaMetricsManagerService.LOGGING_LEVEL_BLOCKED) {
                return;
            }
            StatsEvent statsEvent = StatsEvent.newBuilder().setAtomId((int) FrameworkStatsLog.APP_BACKGROUND_RESTRICTIONS_INFO__EXEMPTION_REASON__REASON_ACTIVE_DEVICE_ADMIN).writeString(sessionId).writeInt(event.getTrackState()).writeInt(event.getTrackChangeReason()).writeString(event.getContainerMimeType()).writeString(event.getSampleMimeType()).writeString(event.getCodecName()).writeInt(event.getBitrate()).writeLong(event.getTimeSinceCreatedMillis()).writeInt(event.getTrackType()).writeString(event.getLanguage()).writeString(event.getLanguageRegion()).writeInt(event.getChannelCount()).writeInt(event.getAudioSampleRate()).writeInt(event.getWidth()).writeInt(event.getHeight()).writeFloat(event.getVideoFrameRate()).usePooledBuffer().build();
            StatsLog.write(statsEvent);
        }

        private int loggingLevel() {
            synchronized (MediaMetricsManagerService.this.mLock) {
                int uid = Binder.getCallingUid();
                if (MediaMetricsManagerService.this.mMode == null) {
                    long identity = Binder.clearCallingIdentity();
                    MediaMetricsManagerService.this.mMode = Integer.valueOf(DeviceConfig.getInt("media", MediaMetricsManagerService.MEDIA_METRICS_MODE, 2));
                    Binder.restoreCallingIdentity(identity);
                }
                if (MediaMetricsManagerService.this.mMode.intValue() == 1) {
                    return 0;
                }
                int intValue = MediaMetricsManagerService.this.mMode.intValue();
                int i = MediaMetricsManagerService.LOGGING_LEVEL_BLOCKED;
                if (intValue == 0) {
                    Slog.v(MediaMetricsManagerService.TAG, "Logging level blocked: MEDIA_METRICS_MODE_OFF");
                    return MediaMetricsManagerService.LOGGING_LEVEL_BLOCKED;
                }
                PackageManager pm = MediaMetricsManagerService.this.getContext().getPackageManager();
                String[] packages = pm.getPackagesForUid(uid);
                if (packages != null && packages.length != 0) {
                    if (MediaMetricsManagerService.this.mMode.intValue() == 2) {
                        if (MediaMetricsManagerService.this.mBlockList == null) {
                            MediaMetricsManagerService mediaMetricsManagerService = MediaMetricsManagerService.this;
                            mediaMetricsManagerService.mBlockList = mediaMetricsManagerService.getListLocked(MediaMetricsManagerService.PLAYER_METRICS_APP_BLOCKLIST);
                            if (MediaMetricsManagerService.this.mBlockList == null) {
                                Slog.v(MediaMetricsManagerService.TAG, "Logging level blocked: Failed to get PLAYER_METRICS_APP_BLOCKLIST.");
                                return MediaMetricsManagerService.LOGGING_LEVEL_BLOCKED;
                            }
                        }
                        Integer level = loggingLevelInternal(packages, MediaMetricsManagerService.this.mBlockList, MediaMetricsManagerService.PLAYER_METRICS_APP_BLOCKLIST);
                        if (level != null) {
                            return level.intValue();
                        }
                        if (MediaMetricsManagerService.this.mNoUidBlocklist == null) {
                            MediaMetricsManagerService mediaMetricsManagerService2 = MediaMetricsManagerService.this;
                            mediaMetricsManagerService2.mNoUidBlocklist = mediaMetricsManagerService2.getListLocked(MediaMetricsManagerService.PLAYER_METRICS_PER_APP_ATTRIBUTION_BLOCKLIST);
                            if (MediaMetricsManagerService.this.mNoUidBlocklist == null) {
                                Slog.v(MediaMetricsManagerService.TAG, "Logging level blocked: Failed to get PLAYER_METRICS_PER_APP_ATTRIBUTION_BLOCKLIST.");
                                return MediaMetricsManagerService.LOGGING_LEVEL_BLOCKED;
                            }
                        }
                        Integer level2 = loggingLevelInternal(packages, MediaMetricsManagerService.this.mNoUidBlocklist, MediaMetricsManagerService.PLAYER_METRICS_PER_APP_ATTRIBUTION_BLOCKLIST);
                        if (level2 != null) {
                            return level2.intValue();
                        }
                        return 0;
                    } else if (MediaMetricsManagerService.this.mMode.intValue() == 3) {
                        if (MediaMetricsManagerService.this.mNoUidAllowlist == null) {
                            MediaMetricsManagerService mediaMetricsManagerService3 = MediaMetricsManagerService.this;
                            mediaMetricsManagerService3.mNoUidAllowlist = mediaMetricsManagerService3.getListLocked(MediaMetricsManagerService.PLAYER_METRICS_PER_APP_ATTRIBUTION_ALLOWLIST);
                            if (MediaMetricsManagerService.this.mNoUidAllowlist == null) {
                                Slog.v(MediaMetricsManagerService.TAG, "Logging level blocked: Failed to get PLAYER_METRICS_PER_APP_ATTRIBUTION_ALLOWLIST.");
                                return MediaMetricsManagerService.LOGGING_LEVEL_BLOCKED;
                            }
                        }
                        Integer level3 = loggingLevelInternal(packages, MediaMetricsManagerService.this.mNoUidAllowlist, MediaMetricsManagerService.PLAYER_METRICS_PER_APP_ATTRIBUTION_ALLOWLIST);
                        if (level3 != null) {
                            return level3.intValue();
                        }
                        if (MediaMetricsManagerService.this.mAllowlist == null) {
                            MediaMetricsManagerService mediaMetricsManagerService4 = MediaMetricsManagerService.this;
                            mediaMetricsManagerService4.mAllowlist = mediaMetricsManagerService4.getListLocked(MediaMetricsManagerService.PLAYER_METRICS_APP_ALLOWLIST);
                            if (MediaMetricsManagerService.this.mAllowlist == null) {
                                Slog.v(MediaMetricsManagerService.TAG, "Logging level blocked: Failed to get PLAYER_METRICS_APP_ALLOWLIST.");
                                return MediaMetricsManagerService.LOGGING_LEVEL_BLOCKED;
                            }
                        }
                        Integer level4 = loggingLevelInternal(packages, MediaMetricsManagerService.this.mAllowlist, MediaMetricsManagerService.PLAYER_METRICS_APP_ALLOWLIST);
                        if (level4 != null) {
                            return level4.intValue();
                        }
                        Slog.v(MediaMetricsManagerService.TAG, "Logging level blocked: Not detected in any allowlist.");
                        return MediaMetricsManagerService.LOGGING_LEVEL_BLOCKED;
                    } else {
                        Slog.v(MediaMetricsManagerService.TAG, "Logging level blocked: Blocked by default.");
                        return MediaMetricsManagerService.LOGGING_LEVEL_BLOCKED;
                    }
                }
                Slog.d(MediaMetricsManagerService.TAG, "empty package from uid " + uid);
                if (MediaMetricsManagerService.this.mMode.intValue() == 2) {
                    i = 1000;
                }
                return i;
            }
        }

        private Integer loggingLevelInternal(String[] packages, List<String> cached, String listName) {
            if (inList(packages, cached)) {
                return Integer.valueOf(listNameToLoggingLevel(listName));
            }
            return null;
        }

        private boolean inList(String[] packages, List<String> arr) {
            for (String p : packages) {
                for (String element : arr) {
                    if (p.equals(element)) {
                        return true;
                    }
                }
            }
            return false;
        }

        /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
        private int listNameToLoggingLevel(String listName) {
            char c;
            switch (listName.hashCode()) {
                case -1894232751:
                    if (listName.equals(MediaMetricsManagerService.PLAYER_METRICS_PER_APP_ATTRIBUTION_BLOCKLIST)) {
                        c = 3;
                        break;
                    }
                    c = 65535;
                    break;
                case -1289480849:
                    if (listName.equals(MediaMetricsManagerService.PLAYER_METRICS_APP_ALLOWLIST)) {
                        c = 1;
                        break;
                    }
                    c = 65535;
                    break;
                case -789056333:
                    if (listName.equals(MediaMetricsManagerService.PLAYER_METRICS_APP_BLOCKLIST)) {
                        c = 0;
                        break;
                    }
                    c = 65535;
                    break;
                case 1900310029:
                    if (listName.equals(MediaMetricsManagerService.PLAYER_METRICS_PER_APP_ATTRIBUTION_ALLOWLIST)) {
                        c = 2;
                        break;
                    }
                    c = 65535;
                    break;
                default:
                    c = 65535;
                    break;
            }
            switch (c) {
                case 0:
                    return MediaMetricsManagerService.LOGGING_LEVEL_BLOCKED;
                case 1:
                    return 0;
                case 2:
                case 3:
                    return 1000;
                default:
                    return MediaMetricsManagerService.LOGGING_LEVEL_BLOCKED;
            }
        }
    }
}
