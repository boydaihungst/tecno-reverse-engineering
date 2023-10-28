package com.android.server.broadcastradio.hal1;

import android.hardware.radio.ITunerCallback;
import android.hardware.radio.ProgramList;
import android.hardware.radio.ProgramSelector;
import android.hardware.radio.RadioManager;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Slog;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class TunerCallback implements ITunerCallback {
    private static final String TAG = "BroadcastRadioService.TunerCallback";
    private final ITunerCallback mClientCallback;
    private final long mNativeContext;
    private final Tuner mTuner;
    private final AtomicReference<ProgramList.Filter> mProgramListFilter = new AtomicReference<>();
    private boolean mInitialConfigurationDone = false;

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public interface RunnableThrowingRemoteException {
        void run() throws RemoteException;
    }

    private native void nativeDetach(long j);

    private native void nativeFinalize(long j);

    private native long nativeInit(Tuner tuner, int i);

    /* JADX INFO: Access modifiers changed from: package-private */
    public TunerCallback(Tuner tuner, ITunerCallback clientCallback, int halRev) {
        this.mTuner = tuner;
        this.mClientCallback = clientCallback;
        this.mNativeContext = nativeInit(tuner, halRev);
    }

    protected void finalize() throws Throwable {
        nativeFinalize(this.mNativeContext);
        super.finalize();
    }

    public void detach() {
        nativeDetach(this.mNativeContext);
    }

    private void dispatch(RunnableThrowingRemoteException func) {
        try {
            func.run();
        } catch (RemoteException e) {
            Slog.e(TAG, "client died", e);
        }
    }

    private void handleHwFailure() {
        onError(0);
        this.mTuner.close();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void startProgramListUpdates(ProgramList.Filter filter) {
        if (filter == null) {
            filter = new ProgramList.Filter();
        }
        this.mProgramListFilter.set(filter);
        sendProgramListUpdate();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void stopProgramListUpdates() {
        this.mProgramListFilter.set(null);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isInitialConfigurationDone() {
        return this.mInitialConfigurationDone;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onError$0$com-android-server-broadcastradio-hal1-TunerCallback  reason: not valid java name */
    public /* synthetic */ void m2605x7041876f(int status) throws RemoteException {
        this.mClientCallback.onError(status);
    }

    public void onError(final int status) {
        dispatch(new RunnableThrowingRemoteException() { // from class: com.android.server.broadcastradio.hal1.TunerCallback$$ExternalSyntheticLambda3
            @Override // com.android.server.broadcastradio.hal1.TunerCallback.RunnableThrowingRemoteException
            public final void run() {
                TunerCallback.this.m2605x7041876f(status);
            }
        });
    }

    public void onTuneFailed(int result, ProgramSelector selector) {
        Slog.e(TAG, "Not applicable for HAL 1.x");
    }

    public void onConfigurationChanged(final RadioManager.BandConfig config) {
        this.mInitialConfigurationDone = true;
        dispatch(new RunnableThrowingRemoteException() { // from class: com.android.server.broadcastradio.hal1.TunerCallback$$ExternalSyntheticLambda1
            @Override // com.android.server.broadcastradio.hal1.TunerCallback.RunnableThrowingRemoteException
            public final void run() {
                TunerCallback.this.m2602x2f074f00(config);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onConfigurationChanged$1$com-android-server-broadcastradio-hal1-TunerCallback  reason: not valid java name */
    public /* synthetic */ void m2602x2f074f00(RadioManager.BandConfig config) throws RemoteException {
        this.mClientCallback.onConfigurationChanged(config);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onCurrentProgramInfoChanged$2$com-android-server-broadcastradio-hal1-TunerCallback  reason: not valid java name */
    public /* synthetic */ void m2603x243f4cfa(RadioManager.ProgramInfo info) throws RemoteException {
        this.mClientCallback.onCurrentProgramInfoChanged(info);
    }

    public void onCurrentProgramInfoChanged(final RadioManager.ProgramInfo info) {
        dispatch(new RunnableThrowingRemoteException() { // from class: com.android.server.broadcastradio.hal1.TunerCallback$$ExternalSyntheticLambda2
            @Override // com.android.server.broadcastradio.hal1.TunerCallback.RunnableThrowingRemoteException
            public final void run() {
                TunerCallback.this.m2603x243f4cfa(info);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onTrafficAnnouncement$3$com-android-server-broadcastradio-hal1-TunerCallback  reason: not valid java name */
    public /* synthetic */ void m2608xe54694f0(boolean active) throws RemoteException {
        this.mClientCallback.onTrafficAnnouncement(active);
    }

    public void onTrafficAnnouncement(final boolean active) {
        dispatch(new RunnableThrowingRemoteException() { // from class: com.android.server.broadcastradio.hal1.TunerCallback$$ExternalSyntheticLambda9
            @Override // com.android.server.broadcastradio.hal1.TunerCallback.RunnableThrowingRemoteException
            public final void run() {
                TunerCallback.this.m2608xe54694f0(active);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onEmergencyAnnouncement$4$com-android-server-broadcastradio-hal1-TunerCallback  reason: not valid java name */
    public /* synthetic */ void m2604x338a287b(boolean active) throws RemoteException {
        this.mClientCallback.onEmergencyAnnouncement(active);
    }

    public void onEmergencyAnnouncement(final boolean active) {
        dispatch(new RunnableThrowingRemoteException() { // from class: com.android.server.broadcastradio.hal1.TunerCallback$$ExternalSyntheticLambda10
            @Override // com.android.server.broadcastradio.hal1.TunerCallback.RunnableThrowingRemoteException
            public final void run() {
                TunerCallback.this.m2604x338a287b(active);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onAntennaState$5$com-android-server-broadcastradio-hal1-TunerCallback  reason: not valid java name */
    public /* synthetic */ void m2599xc7cbf88c(boolean connected) throws RemoteException {
        this.mClientCallback.onAntennaState(connected);
    }

    public void onAntennaState(final boolean connected) {
        dispatch(new RunnableThrowingRemoteException() { // from class: com.android.server.broadcastradio.hal1.TunerCallback$$ExternalSyntheticLambda6
            @Override // com.android.server.broadcastradio.hal1.TunerCallback.RunnableThrowingRemoteException
            public final void run() {
                TunerCallback.this.m2599xc7cbf88c(connected);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onBackgroundScanAvailabilityChange$6$com-android-server-broadcastradio-hal1-TunerCallback  reason: not valid java name */
    public /* synthetic */ void m2600xad817ae3(boolean isAvailable) throws RemoteException {
        this.mClientCallback.onBackgroundScanAvailabilityChange(isAvailable);
    }

    public void onBackgroundScanAvailabilityChange(final boolean isAvailable) {
        dispatch(new RunnableThrowingRemoteException() { // from class: com.android.server.broadcastradio.hal1.TunerCallback$$ExternalSyntheticLambda8
            @Override // com.android.server.broadcastradio.hal1.TunerCallback.RunnableThrowingRemoteException
            public final void run() {
                TunerCallback.this.m2600xad817ae3(isAvailable);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onBackgroundScanComplete$7$com-android-server-broadcastradio-hal1-TunerCallback  reason: not valid java name */
    public /* synthetic */ void m2601x733de0b4() throws RemoteException {
        this.mClientCallback.onBackgroundScanComplete();
    }

    public void onBackgroundScanComplete() {
        dispatch(new RunnableThrowingRemoteException() { // from class: com.android.server.broadcastradio.hal1.TunerCallback$$ExternalSyntheticLambda4
            @Override // com.android.server.broadcastradio.hal1.TunerCallback.RunnableThrowingRemoteException
            public final void run() {
                TunerCallback.this.m2601x733de0b4();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onProgramListChanged$8$com-android-server-broadcastradio-hal1-TunerCallback  reason: not valid java name */
    public /* synthetic */ void m2606xffe4c5a5() throws RemoteException {
        this.mClientCallback.onProgramListChanged();
    }

    public void onProgramListChanged() {
        dispatch(new RunnableThrowingRemoteException() { // from class: com.android.server.broadcastradio.hal1.TunerCallback$$ExternalSyntheticLambda5
            @Override // com.android.server.broadcastradio.hal1.TunerCallback.RunnableThrowingRemoteException
            public final void run() {
                TunerCallback.this.m2606xffe4c5a5();
            }
        });
        sendProgramListUpdate();
    }

    private void sendProgramListUpdate() {
        ProgramList.Filter filter = this.mProgramListFilter.get();
        if (filter == null) {
            return;
        }
        try {
            List<RadioManager.ProgramInfo> modified = this.mTuner.getProgramList(filter.getVendorFilter());
            Set<RadioManager.ProgramInfo> modifiedSet = (Set) modified.stream().collect(Collectors.toSet());
            final ProgramList.Chunk chunk = new ProgramList.Chunk(true, true, modifiedSet, (Set) null);
            dispatch(new RunnableThrowingRemoteException() { // from class: com.android.server.broadcastradio.hal1.TunerCallback$$ExternalSyntheticLambda0
                @Override // com.android.server.broadcastradio.hal1.TunerCallback.RunnableThrowingRemoteException
                public final void run() {
                    TunerCallback.this.m2609x3853206c(chunk);
                }
            });
        } catch (IllegalStateException e) {
            Slog.d(TAG, "Program list not ready yet");
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$sendProgramListUpdate$9$com-android-server-broadcastradio-hal1-TunerCallback  reason: not valid java name */
    public /* synthetic */ void m2609x3853206c(ProgramList.Chunk chunk) throws RemoteException {
        this.mClientCallback.onProgramListUpdated(chunk);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onProgramListUpdated$10$com-android-server-broadcastradio-hal1-TunerCallback  reason: not valid java name */
    public /* synthetic */ void m2607xd62c3f39(ProgramList.Chunk chunk) throws RemoteException {
        this.mClientCallback.onProgramListUpdated(chunk);
    }

    public void onProgramListUpdated(final ProgramList.Chunk chunk) {
        dispatch(new RunnableThrowingRemoteException() { // from class: com.android.server.broadcastradio.hal1.TunerCallback$$ExternalSyntheticLambda7
            @Override // com.android.server.broadcastradio.hal1.TunerCallback.RunnableThrowingRemoteException
            public final void run() {
                TunerCallback.this.m2607xd62c3f39(chunk);
            }
        });
    }

    public void onParametersUpdated(Map<String, String> parameters) {
        Slog.e(TAG, "Not applicable for HAL 1.x");
    }

    public IBinder asBinder() {
        throw new RuntimeException("Not a binder");
    }
}
