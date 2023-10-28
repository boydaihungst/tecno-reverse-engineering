package android.media.audiofx;

import java.util.UUID;
/* loaded from: classes2.dex */
public class DtsAudio extends AudioEffect {
    private static final String TAG = DtsAudio.class.getSimpleName();

    public DtsAudio(UUID type, UUID uuid, int priority, int audioSession) throws IllegalStateException, IllegalArgumentException, UnsupportedOperationException, RuntimeException {
        super(type, uuid, priority, audioSession);
    }

    @Override // android.media.audiofx.AudioEffect
    public int setEnabled(boolean enabled) throws IllegalStateException {
        return super.setEnabled(enabled);
    }

    @Override // android.media.audiofx.AudioEffect
    public boolean getEnabled() throws IllegalStateException {
        return super.getEnabled();
    }

    @Override // android.media.audiofx.AudioEffect
    public int setParameter(byte[] param, byte[] value) throws IllegalStateException {
        return super.setParameter(param, value);
    }

    @Override // android.media.audiofx.AudioEffect
    public int getParameter(byte[] param, byte[] value) throws IllegalStateException {
        return super.getParameter(param, value);
    }
}
