package android.service.dreams;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.service.dreams.IDreamOverlay;
import android.util.Log;
import android.view.WindowManager;
/* loaded from: classes3.dex */
public abstract class DreamOverlayService extends Service {
    private static final boolean DEBUG = false;
    private static final String TAG = "DreamOverlayService";
    private IDreamOverlay mDreamOverlay = new IDreamOverlay.Stub() { // from class: android.service.dreams.DreamOverlayService.1
        @Override // android.service.dreams.IDreamOverlay
        public void startDream(WindowManager.LayoutParams layoutParams, IDreamOverlayCallback callback) {
            DreamOverlayService.this.mDreamOverlayCallback = callback;
            DreamOverlayService.this.onStartDream(layoutParams);
        }
    };
    IDreamOverlayCallback mDreamOverlayCallback;
    private boolean mShowComplications;

    public abstract void onStartDream(WindowManager.LayoutParams layoutParams);

    @Override // android.app.Service
    public final IBinder onBind(Intent intent) {
        this.mShowComplications = intent.getBooleanExtra(DreamService.EXTRA_SHOW_COMPLICATIONS, false);
        return this.mDreamOverlay.asBinder();
    }

    public final void requestExit() {
        try {
            this.mDreamOverlayCallback.onExitRequested();
        } catch (RemoteException e) {
            Log.e(TAG, "Could not request exit:" + e);
        }
    }

    public final boolean shouldShowComplications() {
        return this.mShowComplications;
    }
}
