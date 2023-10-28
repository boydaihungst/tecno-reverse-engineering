package android.service.dreams;

import android.content.ComponentName;
/* loaded from: classes3.dex */
public abstract class DreamManagerInternal {
    public abstract ComponentName getActiveDreamComponent(boolean z);

    public abstract boolean isDreaming();

    public abstract void notifyAodAction(int i);

    public abstract void startDream(boolean z);

    public abstract void stopDream(boolean z);
}
