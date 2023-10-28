package com.android.server.media;

import android.media.Session2Token;
import android.media.session.MediaSession;
import android.util.Log;
import android.util.SparseArray;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class MediaSessionStack {
    private static final boolean DEBUG = MediaSessionService.DEBUG;
    private static final String TAG = "MediaSessionStack";
    private final AudioPlayerStateMonitor mAudioPlayerStateMonitor;
    private MediaSessionRecordImpl mCachedVolumeDefault;
    private MediaSessionRecordImpl mMediaButtonSession;
    private final OnMediaButtonSessionChangedListener mOnMediaButtonSessionChangedListener;
    private final List<MediaSessionRecordImpl> mSessions = new ArrayList();
    private final SparseArray<List<MediaSessionRecord>> mCachedActiveLists = new SparseArray<>();

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public interface OnMediaButtonSessionChangedListener {
        void onMediaButtonSessionChanged(MediaSessionRecordImpl mediaSessionRecordImpl, MediaSessionRecordImpl mediaSessionRecordImpl2);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public MediaSessionStack(AudioPlayerStateMonitor monitor, OnMediaButtonSessionChangedListener listener) {
        this.mAudioPlayerStateMonitor = monitor;
        this.mOnMediaButtonSessionChangedListener = listener;
    }

    public void addSession(MediaSessionRecordImpl record) {
        this.mSessions.add(record);
        clearCache(record.getUserId());
        updateMediaButtonSessionIfNeeded();
    }

    public void removeSession(MediaSessionRecordImpl record) {
        this.mSessions.remove(record);
        if (this.mMediaButtonSession == record) {
            updateMediaButtonSession(null);
        }
        clearCache(record.getUserId());
    }

    public boolean contains(MediaSessionRecordImpl record) {
        return this.mSessions.contains(record);
    }

    public MediaSessionRecord getMediaSessionRecord(MediaSession.Token sessionToken) {
        for (MediaSessionRecordImpl record : this.mSessions) {
            if (record instanceof MediaSessionRecord) {
                MediaSessionRecord session1 = (MediaSessionRecord) record;
                if (Objects.equals(session1.getSessionToken(), sessionToken)) {
                    return session1;
                }
            }
        }
        return null;
    }

    public void onPlaybackStateChanged(MediaSessionRecordImpl record, boolean shouldUpdatePriority) {
        MediaSessionRecordImpl newMediaButtonSession;
        if (shouldUpdatePriority) {
            this.mSessions.remove(record);
            this.mSessions.add(0, record);
            clearCache(record.getUserId());
        } else if (record.checkPlaybackActiveState(false)) {
            this.mCachedVolumeDefault = null;
        }
        MediaSessionRecordImpl mediaSessionRecordImpl = this.mMediaButtonSession;
        if (mediaSessionRecordImpl != null && mediaSessionRecordImpl.getUid() == record.getUid() && (newMediaButtonSession = findMediaButtonSession(this.mMediaButtonSession.getUid())) != this.mMediaButtonSession && (newMediaButtonSession.getSessionPolicies() & 2) == 0) {
            updateMediaButtonSession(newMediaButtonSession);
        }
    }

    public void onSessionActiveStateChanged(MediaSessionRecordImpl record) {
        clearCache(record.getUserId());
    }

    public void updateMediaButtonSessionIfNeeded() {
        if (DEBUG) {
            Log.d(TAG, "updateMediaButtonSessionIfNeeded, callers=" + getCallers(2));
        }
        List<Integer> audioPlaybackUids = this.mAudioPlayerStateMonitor.getSortedAudioPlaybackClientUids();
        for (int i = 0; i < audioPlaybackUids.size(); i++) {
            int audioPlaybackUid = audioPlaybackUids.get(i).intValue();
            MediaSessionRecordImpl mediaButtonSession = findMediaButtonSession(audioPlaybackUid);
            if (mediaButtonSession == null) {
                if (DEBUG) {
                    Log.d(TAG, "updateMediaButtonSessionIfNeeded, skipping uid=" + audioPlaybackUid);
                }
            } else {
                boolean ignoreButtonSession = (mediaButtonSession.getSessionPolicies() & 2) != 0;
                if (DEBUG) {
                    Log.d(TAG, "updateMediaButtonSessionIfNeeded, checking uid=" + audioPlaybackUid + ", mediaButtonSession=" + mediaButtonSession + ", ignoreButtonSession=" + ignoreButtonSession);
                }
                if (!ignoreButtonSession) {
                    this.mAudioPlayerStateMonitor.cleanUpAudioPlaybackUids(mediaButtonSession.getUid());
                    if (mediaButtonSession != this.mMediaButtonSession) {
                        updateMediaButtonSession(mediaButtonSession);
                        return;
                    }
                    return;
                }
            }
        }
    }

    public void updateMediaButtonSessionBySessionPolicyChange(MediaSessionRecord record) {
        if ((record.getSessionPolicies() & 2) != 0) {
            if (record == this.mMediaButtonSession) {
                updateMediaButtonSession(null);
                return;
            }
            return;
        }
        updateMediaButtonSessionIfNeeded();
    }

    private MediaSessionRecordImpl findMediaButtonSession(int uid) {
        MediaSessionRecordImpl mediaButtonSession = null;
        for (MediaSessionRecordImpl session : this.mSessions) {
            if (!(session instanceof MediaSession2Record) && uid == session.getUid()) {
                if (session.checkPlaybackActiveState(this.mAudioPlayerStateMonitor.isPlaybackActive(session.getUid()))) {
                    return session;
                }
                if (mediaButtonSession == null) {
                    mediaButtonSession = session;
                }
            }
        }
        return mediaButtonSession;
    }

    public List<MediaSessionRecord> getActiveSessions(int userId) {
        List<MediaSessionRecord> cachedActiveList = this.mCachedActiveLists.get(userId);
        if (cachedActiveList == null) {
            List<MediaSessionRecord> cachedActiveList2 = getPriorityList(true, userId);
            this.mCachedActiveLists.put(userId, cachedActiveList2);
            return cachedActiveList2;
        }
        return cachedActiveList;
    }

    public List<Session2Token> getSession2Tokens(int userId) {
        ArrayList<Session2Token> session2Records = new ArrayList<>();
        for (MediaSessionRecordImpl record : this.mSessions) {
            if (userId == -1 || record.getUserId() == userId) {
                if (record.isActive() && (record instanceof MediaSession2Record)) {
                    MediaSession2Record session2 = (MediaSession2Record) record;
                    session2Records.add(session2.getSession2Token());
                }
            }
        }
        return session2Records;
    }

    public MediaSessionRecordImpl getMediaButtonSession() {
        return this.mMediaButtonSession;
    }

    public void updateMediaButtonSession(MediaSessionRecordImpl newMediaButtonSession) {
        MediaSessionRecordImpl oldMediaButtonSession = this.mMediaButtonSession;
        this.mMediaButtonSession = newMediaButtonSession;
        this.mOnMediaButtonSessionChangedListener.onMediaButtonSessionChanged(oldMediaButtonSession, newMediaButtonSession);
    }

    public MediaSessionRecordImpl getDefaultVolumeSession() {
        MediaSessionRecordImpl mediaSessionRecordImpl = this.mCachedVolumeDefault;
        if (mediaSessionRecordImpl != null) {
            return mediaSessionRecordImpl;
        }
        List<MediaSessionRecord> records = getPriorityList(true, -1);
        int size = records.size();
        for (int i = 0; i < size; i++) {
            MediaSessionRecord record = records.get(i);
            if (record.checkPlaybackActiveState(true) && record.canHandleVolumeKey()) {
                this.mCachedVolumeDefault = record;
                return record;
            }
        }
        return null;
    }

    public MediaSessionRecordImpl getDefaultRemoteSession(int userId) {
        List<MediaSessionRecord> records = getPriorityList(true, userId);
        int size = records.size();
        for (int i = 0; i < size; i++) {
            MediaSessionRecord record = records.get(i);
            if (!record.isPlaybackTypeLocal()) {
                return record;
            }
        }
        return null;
    }

    public void dump(PrintWriter pw, String prefix) {
        pw.println(prefix + "Media button session is " + this.mMediaButtonSession);
        pw.println(prefix + "Sessions Stack - have " + this.mSessions.size() + " sessions:");
        String indent = prefix + "  ";
        for (MediaSessionRecordImpl record : this.mSessions) {
            record.dump(pw, indent);
        }
    }

    public List<MediaSessionRecord> getPriorityList(boolean activeOnly, int userId) {
        List<MediaSessionRecord> result = new ArrayList<>();
        int lastPlaybackActiveIndex = 0;
        int lastActiveIndex = 0;
        for (MediaSessionRecordImpl record : this.mSessions) {
            if (record instanceof MediaSessionRecord) {
                MediaSessionRecord session = (MediaSessionRecord) record;
                if (userId == -1 || userId == session.getUserId()) {
                    if (!session.isActive()) {
                        if (!activeOnly) {
                            result.add(session);
                        }
                    } else if (session.checkPlaybackActiveState(true)) {
                        result.add(lastPlaybackActiveIndex, session);
                        lastActiveIndex++;
                        lastPlaybackActiveIndex++;
                    } else {
                        int lastPlaybackActiveIndex2 = lastActiveIndex + 1;
                        result.add(lastActiveIndex, session);
                        lastActiveIndex = lastPlaybackActiveIndex2;
                    }
                }
            }
        }
        return result;
    }

    private void clearCache(int userId) {
        this.mCachedVolumeDefault = null;
        this.mCachedActiveLists.remove(userId);
        this.mCachedActiveLists.remove(-1);
    }

    private static String getCallers(int depth) {
        StackTraceElement[] callStack = Thread.currentThread().getStackTrace();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < depth; i++) {
            sb.append(getCaller(callStack, i)).append(" ");
        }
        return sb.toString();
    }

    private static String getCaller(StackTraceElement[] callStack, int depth) {
        if (depth + 4 >= callStack.length) {
            return "<bottom of call stack>";
        }
        StackTraceElement caller = callStack[depth + 4];
        return caller.getClassName() + "." + caller.getMethodName() + ":" + caller.getLineNumber();
    }
}
