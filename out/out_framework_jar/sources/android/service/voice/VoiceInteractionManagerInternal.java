package android.service.voice;

import android.os.Bundle;
import android.os.IBinder;
/* loaded from: classes3.dex */
public abstract class VoiceInteractionManagerInternal {
    public abstract HotwordDetectionServiceIdentity getHotwordDetectionServiceIdentity();

    public abstract String getVoiceInteractorPackageName(IBinder iBinder);

    public abstract boolean hasActiveSession(String str);

    public abstract void startLocalVoiceInteraction(IBinder iBinder, Bundle bundle);

    public abstract void stopLocalVoiceInteraction(IBinder iBinder);

    public abstract boolean supportsLocalVoiceInteraction();

    /* loaded from: classes3.dex */
    public static class HotwordDetectionServiceIdentity {
        private final int mIsolatedUid;
        private final int mOwnerUid;

        public HotwordDetectionServiceIdentity(int isolatedUid, int ownerUid) {
            this.mIsolatedUid = isolatedUid;
            this.mOwnerUid = ownerUid;
        }

        public int getIsolatedUid() {
            return this.mIsolatedUid;
        }

        public int getOwnerUid() {
            return this.mOwnerUid;
        }
    }
}
