package com.android.server.broadcastradio.hal2;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.broadcastradio.V2_0.AmFmRegionConfig;
import android.hardware.broadcastradio.V2_0.Announcement;
import android.hardware.broadcastradio.V2_0.DabTableEntry;
import android.hardware.broadcastradio.V2_0.IAnnouncementListener;
import android.hardware.broadcastradio.V2_0.IBroadcastRadio;
import android.hardware.broadcastradio.V2_0.ITunerCallback;
import android.hardware.broadcastradio.V2_0.ITunerSession;
import android.hardware.broadcastradio.V2_0.ProgramInfo;
import android.hardware.broadcastradio.V2_0.ProgramListChunk;
import android.hardware.broadcastradio.V2_0.ProgramSelector;
import android.hardware.broadcastradio.V2_0.VendorKeyValue;
import android.hardware.radio.IAnnouncementListener;
import android.hardware.radio.ICloseHandle;
import android.hardware.radio.ProgramList;
import android.hardware.radio.ProgramSelector;
import android.hardware.radio.RadioManager;
import android.os.DeadObjectException;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.util.MutableInt;
import android.util.Slog;
import com.android.server.broadcastradio.hal2.RadioModule;
import com.android.server.broadcastradio.hal2.Utils;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class RadioModule {
    private static final String TAG = "BcRadio2Srv.module";
    private ITunerSession mHalTunerSession;
    private final Object mLock;
    public final RadioManager.ModuleProperties mProperties;
    private final IBroadcastRadio mService;
    private Boolean mAntennaConnected = null;
    private RadioManager.ProgramInfo mCurrentProgramInfo = null;
    private final ProgramInfoCache mProgramInfoCache = new ProgramInfoCache(null);
    private ProgramList.Filter mUnionOfAidlProgramFilters = null;
    private final ITunerCallback mHalTunerCallback = new AnonymousClass1();
    private final Set<TunerSession> mAidlTunerSessions = new HashSet();
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public interface AidlCallbackRunnable {
        void run(android.hardware.radio.ITunerCallback iTunerCallback) throws RemoteException;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: com.android.server.broadcastradio.hal2.RadioModule$1  reason: invalid class name */
    /* loaded from: classes.dex */
    public class AnonymousClass1 extends ITunerCallback.Stub {
        AnonymousClass1() {
        }

        @Override // android.hardware.broadcastradio.V2_0.ITunerCallback
        public void onTuneFailed(final int result, final ProgramSelector programSelector) {
            RadioModule.this.lockAndFireLater(new Runnable() { // from class: com.android.server.broadcastradio.hal2.RadioModule$1$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    RadioModule.AnonymousClass1.this.m2634x995ad61f(programSelector, result);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onTuneFailed$1$com-android-server-broadcastradio-hal2-RadioModule$1  reason: not valid java name */
        public /* synthetic */ void m2634x995ad61f(ProgramSelector programSelector, final int result) {
            final android.hardware.radio.ProgramSelector csel = Convert.programSelectorFromHal(programSelector);
            RadioModule.this.m2626x3f8d93f3(new AidlCallbackRunnable() { // from class: com.android.server.broadcastradio.hal2.RadioModule$1$$ExternalSyntheticLambda3
                @Override // com.android.server.broadcastradio.hal2.RadioModule.AidlCallbackRunnable
                public final void run(android.hardware.radio.ITunerCallback iTunerCallback) {
                    iTunerCallback.onTuneFailed(result, csel);
                }
            });
        }

        @Override // android.hardware.broadcastradio.V2_0.ITunerCallback
        public void onCurrentProgramInfoChanged(final ProgramInfo halProgramInfo) {
            RadioModule.this.lockAndFireLater(new Runnable() { // from class: com.android.server.broadcastradio.hal2.RadioModule$1$$ExternalSyntheticLambda8
                @Override // java.lang.Runnable
                public final void run() {
                    RadioModule.AnonymousClass1.this.m2631x4af8590f(halProgramInfo);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onCurrentProgramInfoChanged$3$com-android-server-broadcastradio-hal2-RadioModule$1  reason: not valid java name */
        public /* synthetic */ void m2631x4af8590f(ProgramInfo halProgramInfo) {
            RadioModule.this.mCurrentProgramInfo = Convert.programInfoFromHal(halProgramInfo);
            RadioModule.this.m2626x3f8d93f3(new AidlCallbackRunnable() { // from class: com.android.server.broadcastradio.hal2.RadioModule$1$$ExternalSyntheticLambda7
                @Override // com.android.server.broadcastradio.hal2.RadioModule.AidlCallbackRunnable
                public final void run(android.hardware.radio.ITunerCallback iTunerCallback) {
                    RadioModule.AnonymousClass1.this.m2630x30dcda70(iTunerCallback);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onCurrentProgramInfoChanged$2$com-android-server-broadcastradio-hal2-RadioModule$1  reason: not valid java name */
        public /* synthetic */ void m2630x30dcda70(android.hardware.radio.ITunerCallback cb) throws RemoteException {
            cb.onCurrentProgramInfoChanged(RadioModule.this.mCurrentProgramInfo);
        }

        @Override // android.hardware.broadcastradio.V2_0.ITunerCallback
        public void onProgramListUpdated(final ProgramListChunk programListChunk) {
            RadioModule.this.lockAndFireLater(new Runnable() { // from class: com.android.server.broadcastradio.hal2.RadioModule$1$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    RadioModule.AnonymousClass1.this.m2633x44ff5eb8(programListChunk);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onProgramListUpdated$4$com-android-server-broadcastradio-hal2-RadioModule$1  reason: not valid java name */
        public /* synthetic */ void m2633x44ff5eb8(ProgramListChunk programListChunk) {
            ProgramList.Chunk chunk = Convert.programListChunkFromHal(programListChunk);
            RadioModule.this.mProgramInfoCache.filterAndApplyChunk(chunk);
            for (TunerSession tunerSession : RadioModule.this.mAidlTunerSessions) {
                tunerSession.onMergedProgramListUpdateFromHal(chunk);
            }
        }

        @Override // android.hardware.broadcastradio.V2_0.ITunerCallback
        public void onAntennaStateChange(final boolean connected) {
            RadioModule.this.lockAndFireLater(new Runnable() { // from class: com.android.server.broadcastradio.hal2.RadioModule$1$$ExternalSyntheticLambda4
                @Override // java.lang.Runnable
                public final void run() {
                    RadioModule.AnonymousClass1.this.m2629x937dea51(connected);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onAntennaStateChange$6$com-android-server-broadcastradio-hal2-RadioModule$1  reason: not valid java name */
        public /* synthetic */ void m2629x937dea51(final boolean connected) {
            RadioModule.this.mAntennaConnected = Boolean.valueOf(connected);
            RadioModule.this.m2626x3f8d93f3(new AidlCallbackRunnable() { // from class: com.android.server.broadcastradio.hal2.RadioModule$1$$ExternalSyntheticLambda5
                @Override // com.android.server.broadcastradio.hal2.RadioModule.AidlCallbackRunnable
                public final void run(android.hardware.radio.ITunerCallback iTunerCallback) {
                    iTunerCallback.onAntennaState(connected);
                }
            });
        }

        @Override // android.hardware.broadcastradio.V2_0.ITunerCallback
        public void onParametersUpdated(final ArrayList<VendorKeyValue> parameters) {
            RadioModule.this.lockAndFireLater(new Runnable() { // from class: com.android.server.broadcastradio.hal2.RadioModule$1$$ExternalSyntheticLambda2
                @Override // java.lang.Runnable
                public final void run() {
                    RadioModule.AnonymousClass1.this.m2632x77670454(parameters);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onParametersUpdated$8$com-android-server-broadcastradio-hal2-RadioModule$1  reason: not valid java name */
        public /* synthetic */ void m2632x77670454(ArrayList parameters) {
            final Map<String, String> cparam = Convert.vendorInfoFromHal(parameters);
            RadioModule.this.m2626x3f8d93f3(new AidlCallbackRunnable() { // from class: com.android.server.broadcastradio.hal2.RadioModule$1$$ExternalSyntheticLambda6
                @Override // com.android.server.broadcastradio.hal2.RadioModule.AidlCallbackRunnable
                public final void run(android.hardware.radio.ITunerCallback iTunerCallback) {
                    iTunerCallback.onParametersUpdated(cparam);
                }
            });
        }
    }

    RadioModule(IBroadcastRadio service, RadioManager.ModuleProperties properties, Object lock) {
        this.mProperties = (RadioManager.ModuleProperties) Objects.requireNonNull(properties);
        this.mService = (IBroadcastRadio) Objects.requireNonNull(service);
        this.mLock = Objects.requireNonNull(lock);
    }

    public static RadioModule tryLoadingModule(int idx, String fqName, Object lock) {
        try {
            IBroadcastRadio service = IBroadcastRadio.getService(fqName);
            if (service == null) {
                return null;
            }
            final Mutable<AmFmRegionConfig> amfmConfig = new Mutable<>();
            service.getAmFmRegionConfig(false, new IBroadcastRadio.getAmFmRegionConfigCallback() { // from class: com.android.server.broadcastradio.hal2.RadioModule$$ExternalSyntheticLambda2
                @Override // android.hardware.broadcastradio.V2_0.IBroadcastRadio.getAmFmRegionConfigCallback
                public final void onValues(int i, AmFmRegionConfig amFmRegionConfig) {
                    RadioModule.lambda$tryLoadingModule$0(Mutable.this, i, amFmRegionConfig);
                }
            });
            final Mutable<List<DabTableEntry>> dabConfig = new Mutable<>();
            service.getDabRegionConfig(new IBroadcastRadio.getDabRegionConfigCallback() { // from class: com.android.server.broadcastradio.hal2.RadioModule$$ExternalSyntheticLambda3
                @Override // android.hardware.broadcastradio.V2_0.IBroadcastRadio.getDabRegionConfigCallback
                public final void onValues(int i, ArrayList arrayList) {
                    RadioModule.lambda$tryLoadingModule$1(Mutable.this, i, arrayList);
                }
            });
            RadioManager.ModuleProperties prop = Convert.propertiesFromHal(idx, fqName, service.getProperties(), (AmFmRegionConfig) amfmConfig.value, (List) dabConfig.value);
            return new RadioModule(service, prop, lock);
        } catch (RemoteException ex) {
            Slog.e(TAG, "failed to load module " + fqName, ex);
            return null;
        }
    }

    /* JADX DEBUG: Multi-variable search result rejected for r2v0, resolved type: android.hardware.broadcastradio.V2_0.AmFmRegionConfig */
    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: Multi-variable type inference failed */
    public static /* synthetic */ void lambda$tryLoadingModule$0(Mutable amfmConfig, int result, AmFmRegionConfig config) {
        if (result == 0) {
            amfmConfig.value = config;
        }
    }

    /* JADX DEBUG: Multi-variable search result rejected for r2v0, resolved type: java.util.ArrayList */
    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: Multi-variable type inference failed */
    public static /* synthetic */ void lambda$tryLoadingModule$1(Mutable dabConfig, int result, ArrayList config) {
        if (result == 0) {
            dabConfig.value = config;
        }
    }

    public IBroadcastRadio getService() {
        return this.mService;
    }

    public TunerSession openSession(android.hardware.radio.ITunerCallback userCb) throws RemoteException {
        TunerSession tunerSession;
        synchronized (this.mLock) {
            if (this.mHalTunerSession == null) {
                final Mutable<ITunerSession> hwSession = new Mutable<>();
                this.mService.openSession(this.mHalTunerCallback, new IBroadcastRadio.openSessionCallback() { // from class: com.android.server.broadcastradio.hal2.RadioModule$$ExternalSyntheticLambda6
                    @Override // android.hardware.broadcastradio.V2_0.IBroadcastRadio.openSessionCallback
                    public final void onValues(int i, ITunerSession iTunerSession) {
                        RadioModule.lambda$openSession$2(Mutable.this, i, iTunerSession);
                    }
                });
                this.mHalTunerSession = (ITunerSession) Objects.requireNonNull((ITunerSession) hwSession.value);
            }
            tunerSession = new TunerSession(this, this.mHalTunerSession, userCb, this.mLock);
            this.mAidlTunerSessions.add(tunerSession);
            Boolean bool = this.mAntennaConnected;
            if (bool != null) {
                userCb.onAntennaState(bool.booleanValue());
            }
            RadioManager.ProgramInfo programInfo = this.mCurrentProgramInfo;
            if (programInfo != null) {
                userCb.onCurrentProgramInfoChanged(programInfo);
            }
        }
        return tunerSession;
    }

    /* JADX DEBUG: Multi-variable search result rejected for r3v0, resolved type: android.hardware.broadcastradio.V2_0.ITunerSession */
    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: Multi-variable type inference failed */
    public static /* synthetic */ void lambda$openSession$2(Mutable hwSession, int result, ITunerSession session) {
        Convert.throwOnError("openSession", result);
        hwSession.value = session;
    }

    public void closeSessions(Integer error) {
        TunerSession[] tunerSessions;
        synchronized (this.mLock) {
            tunerSessions = new TunerSession[this.mAidlTunerSessions.size()];
            this.mAidlTunerSessions.toArray(tunerSessions);
            this.mAidlTunerSessions.clear();
        }
        for (TunerSession tunerSession : tunerSessions) {
            tunerSession.close(error);
        }
    }

    private ProgramList.Filter buildUnionOfTunerSessionFiltersLocked() {
        Set<Integer> idTypes = null;
        Set<ProgramSelector.Identifier> ids = null;
        boolean includeCategories = false;
        boolean excludeModifications = true;
        for (TunerSession tunerSession : this.mAidlTunerSessions) {
            ProgramList.Filter filter = tunerSession.getProgramListFilter();
            if (filter != null) {
                if (idTypes == null) {
                    idTypes = new HashSet<>(filter.getIdentifierTypes());
                    ids = new HashSet<>(filter.getIdentifiers());
                    includeCategories = filter.areCategoriesIncluded();
                    excludeModifications = filter.areModificationsExcluded();
                } else {
                    if (!idTypes.isEmpty()) {
                        if (filter.getIdentifierTypes().isEmpty()) {
                            idTypes.clear();
                        } else {
                            idTypes.addAll(filter.getIdentifierTypes());
                        }
                    }
                    if (!ids.isEmpty()) {
                        if (filter.getIdentifiers().isEmpty()) {
                            ids.clear();
                        } else {
                            ids.addAll(filter.getIdentifiers());
                        }
                    }
                    includeCategories |= filter.areCategoriesIncluded();
                    excludeModifications &= filter.areModificationsExcluded();
                }
            }
        }
        if (idTypes == null) {
            return null;
        }
        return new ProgramList.Filter(idTypes, ids, includeCategories, excludeModifications);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onTunerSessionProgramListFilterChanged(TunerSession session) {
        synchronized (this.mLock) {
            onTunerSessionProgramListFilterChangedLocked(session);
        }
    }

    private void onTunerSessionProgramListFilterChangedLocked(TunerSession session) {
        ProgramList.Filter newFilter = buildUnionOfTunerSessionFiltersLocked();
        if (newFilter == null) {
            if (this.mUnionOfAidlProgramFilters == null) {
                return;
            }
            this.mUnionOfAidlProgramFilters = null;
            try {
                this.mHalTunerSession.stopProgramListUpdates();
            } catch (RemoteException ex) {
                Slog.e(TAG, "mHalTunerSession.stopProgramListUpdates() failed: ", ex);
            }
        } else if (newFilter.equals(this.mUnionOfAidlProgramFilters)) {
            if (session != null) {
                session.updateProgramInfoFromHalCache(this.mProgramInfoCache);
            }
        } else {
            this.mUnionOfAidlProgramFilters = newFilter;
            try {
                int halResult = this.mHalTunerSession.startProgramListUpdates(Convert.programFilterToHal(newFilter));
                Convert.throwOnError("startProgramListUpdates", halResult);
            } catch (RemoteException ex2) {
                Slog.e(TAG, "mHalTunerSession.startProgramListUpdates() failed: ", ex2);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onTunerSessionClosed(TunerSession tunerSession) {
        synchronized (this.mLock) {
            onTunerSessionsClosedLocked(tunerSession);
        }
    }

    private void onTunerSessionsClosedLocked(TunerSession... tunerSessions) {
        for (TunerSession tunerSession : tunerSessions) {
            this.mAidlTunerSessions.remove(tunerSession);
        }
        onTunerSessionProgramListFilterChanged(null);
        if (this.mAidlTunerSessions.isEmpty() && this.mHalTunerSession != null) {
            Slog.v(TAG, "closing HAL tuner session");
            try {
                this.mHalTunerSession.close();
            } catch (RemoteException ex) {
                Slog.e(TAG, "mHalTunerSession.close() failed: ", ex);
            }
            this.mHalTunerSession = null;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void lockAndFireLater(final Runnable r) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.broadcastradio.hal2.RadioModule$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                RadioModule.this.m2628x6c3299fa(r);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$lockAndFireLater$3$com-android-server-broadcastradio-hal2-RadioModule  reason: not valid java name */
    public /* synthetic */ void m2628x6c3299fa(Runnable r) {
        synchronized (this.mLock) {
            r.run();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void fanoutAidlCallback(final AidlCallbackRunnable runnable) {
        lockAndFireLater(new Runnable() { // from class: com.android.server.broadcastradio.hal2.RadioModule$$ExternalSyntheticLambda4
            @Override // java.lang.Runnable
            public final void run() {
                RadioModule.this.m2626x3f8d93f3(runnable);
            }
        });
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: fanoutAidlCallbackLocked */
    public void m2626x3f8d93f3(AidlCallbackRunnable runnable) {
        List<TunerSession> deadSessions = null;
        for (TunerSession tunerSession : this.mAidlTunerSessions) {
            try {
                runnable.run(tunerSession.mCallback);
            } catch (DeadObjectException e) {
                Slog.e(TAG, "Removing dead TunerSession");
                if (deadSessions == null) {
                    deadSessions = new ArrayList<>();
                }
                deadSessions.add(tunerSession);
            } catch (RemoteException ex) {
                Slog.e(TAG, "Failed to invoke ITunerCallback: ", ex);
            }
        }
        if (deadSessions != null) {
            onTunerSessionsClosedLocked((TunerSession[]) deadSessions.toArray(new TunerSession[deadSessions.size()]));
        }
    }

    public ICloseHandle addAnnouncementListener(int[] enabledTypes, IAnnouncementListener listener) throws RemoteException {
        ArrayList<Byte> enabledList = new ArrayList<>();
        for (int type : enabledTypes) {
            enabledList.add(Byte.valueOf((byte) type));
        }
        final MutableInt halResult = new MutableInt(1);
        final Mutable<android.hardware.broadcastradio.V2_0.ICloseHandle> hwCloseHandle = new Mutable<>();
        android.hardware.broadcastradio.V2_0.IAnnouncementListener hwListener = new AnonymousClass2(listener);
        synchronized (this.mLock) {
            this.mService.registerAnnouncementListener(enabledList, hwListener, new IBroadcastRadio.registerAnnouncementListenerCallback() { // from class: com.android.server.broadcastradio.hal2.RadioModule$$ExternalSyntheticLambda0
                @Override // android.hardware.broadcastradio.V2_0.IBroadcastRadio.registerAnnouncementListenerCallback
                public final void onValues(int i, android.hardware.broadcastradio.V2_0.ICloseHandle iCloseHandle) {
                    RadioModule.lambda$addAnnouncementListener$5(halResult, hwCloseHandle, i, iCloseHandle);
                }
            });
        }
        Convert.throwOnError("addAnnouncementListener", halResult.value);
        return new ICloseHandle.Stub() { // from class: com.android.server.broadcastradio.hal2.RadioModule.3
            public void close() {
                try {
                    ((android.hardware.broadcastradio.V2_0.ICloseHandle) hwCloseHandle.value).close();
                } catch (RemoteException ex) {
                    Slog.e(RadioModule.TAG, "Failed closing announcement listener", ex);
                }
                hwCloseHandle.value = null;
            }
        };
    }

    /* renamed from: com.android.server.broadcastradio.hal2.RadioModule$2  reason: invalid class name */
    /* loaded from: classes.dex */
    class AnonymousClass2 extends IAnnouncementListener.Stub {
        final /* synthetic */ android.hardware.radio.IAnnouncementListener val$listener;

        AnonymousClass2(android.hardware.radio.IAnnouncementListener iAnnouncementListener) {
            this.val$listener = iAnnouncementListener;
        }

        @Override // android.hardware.broadcastradio.V2_0.IAnnouncementListener
        public void onListUpdated(ArrayList<Announcement> hwAnnouncements) throws RemoteException {
            this.val$listener.onListUpdated((List) hwAnnouncements.stream().map(new Function() { // from class: com.android.server.broadcastradio.hal2.RadioModule$2$$ExternalSyntheticLambda0
                @Override // java.util.function.Function
                public final Object apply(Object obj) {
                    android.hardware.radio.Announcement announcementFromHal;
                    announcementFromHal = Convert.announcementFromHal((Announcement) obj);
                    return announcementFromHal;
                }
            }).collect(Collectors.toList()));
        }
    }

    /* JADX DEBUG: Multi-variable search result rejected for r3v0, resolved type: android.hardware.broadcastradio.V2_0.ICloseHandle */
    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: Multi-variable type inference failed */
    public static /* synthetic */ void lambda$addAnnouncementListener$5(MutableInt halResult, Mutable hwCloseHandle, int result, android.hardware.broadcastradio.V2_0.ICloseHandle closeHnd) {
        halResult.value = result;
        hwCloseHandle.value = closeHnd;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Bitmap getImage(final int id) {
        byte[] rawImage;
        if (id == 0) {
            throw new IllegalArgumentException("Image ID is missing");
        }
        synchronized (this.mLock) {
            List<Byte> rawList = (List) Utils.maybeRethrow(new Utils.FuncThrowingRemoteException() { // from class: com.android.server.broadcastradio.hal2.RadioModule$$ExternalSyntheticLambda5
                @Override // com.android.server.broadcastradio.hal2.Utils.FuncThrowingRemoteException
                public final Object exec() {
                    return RadioModule.this.m2627x520699fc(id);
                }
            });
            rawImage = new byte[rawList.size()];
            for (int i = 0; i < rawList.size(); i++) {
                rawImage[i] = rawList.get(i).byteValue();
            }
        }
        if (rawImage.length == 0) {
            return null;
        }
        return BitmapFactory.decodeByteArray(rawImage, 0, rawImage.length);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getImage$6$com-android-server-broadcastradio-hal2-RadioModule  reason: not valid java name */
    public /* synthetic */ ArrayList m2627x520699fc(int id) throws RemoteException {
        return this.mService.getImage(id);
    }
}
