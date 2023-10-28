package com.android.server.broadcastradio.hal2;

import android.hardware.radio.Announcement;
import android.hardware.radio.IAnnouncementListener;
import android.hardware.radio.ICloseHandle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Slog;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
/* loaded from: classes.dex */
public class AnnouncementAggregator extends ICloseHandle.Stub {
    private static final String TAG = "BcRadio2Srv.AnnAggr";
    private final IBinder.DeathRecipient mDeathRecipient;
    private boolean mIsClosed;
    private final IAnnouncementListener mListener;
    private final Object mLock;
    private final Collection<ModuleWatcher> mModuleWatchers;

    public AnnouncementAggregator(IAnnouncementListener listener, Object lock) {
        DeathRecipient deathRecipient = new DeathRecipient();
        this.mDeathRecipient = deathRecipient;
        this.mModuleWatchers = new ArrayList();
        this.mIsClosed = false;
        this.mListener = (IAnnouncementListener) Objects.requireNonNull(listener);
        this.mLock = Objects.requireNonNull(lock);
        try {
            listener.asBinder().linkToDeath(deathRecipient, 0);
        } catch (RemoteException ex) {
            ex.rethrowFromSystemServer();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class ModuleWatcher extends IAnnouncementListener.Stub {
        public List<Announcement> currentList;
        private ICloseHandle mCloseHandle;

        private ModuleWatcher() {
            this.currentList = new ArrayList();
        }

        public void onListUpdated(List<Announcement> active) {
            this.currentList = (List) Objects.requireNonNull(active);
            AnnouncementAggregator.this.onListUpdated();
        }

        public void setCloseHandle(ICloseHandle closeHandle) {
            this.mCloseHandle = (ICloseHandle) Objects.requireNonNull(closeHandle);
        }

        public void close() throws RemoteException {
            ICloseHandle iCloseHandle = this.mCloseHandle;
            if (iCloseHandle != null) {
                iCloseHandle.close();
            }
        }
    }

    /* loaded from: classes.dex */
    private class DeathRecipient implements IBinder.DeathRecipient {
        private DeathRecipient() {
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            try {
                AnnouncementAggregator.this.close();
            } catch (RemoteException e) {
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onListUpdated() {
        synchronized (this.mLock) {
            if (this.mIsClosed) {
                Slog.e(TAG, "Announcement aggregator is closed, it shouldn't receive callbacks");
                return;
            }
            List<Announcement> combined = new ArrayList<>();
            for (ModuleWatcher watcher : this.mModuleWatchers) {
                combined.addAll(watcher.currentList);
            }
            try {
                this.mListener.onListUpdated(combined);
            } catch (RemoteException ex) {
                Slog.e(TAG, "mListener.onListUpdated() failed: ", ex);
            }
        }
    }

    public void watchModule(RadioModule module, int[] enabledTypes) {
        synchronized (this.mLock) {
            if (this.mIsClosed) {
                throw new IllegalStateException();
            }
            ModuleWatcher watcher = new ModuleWatcher();
            try {
                ICloseHandle closeHandle = module.addAnnouncementListener(enabledTypes, watcher);
                watcher.setCloseHandle(closeHandle);
                this.mModuleWatchers.add(watcher);
            } catch (RemoteException ex) {
                Slog.e(TAG, "Failed to add announcement listener", ex);
            }
        }
    }

    public void close() throws RemoteException {
        synchronized (this.mLock) {
            if (this.mIsClosed) {
                return;
            }
            this.mIsClosed = true;
            this.mListener.asBinder().unlinkToDeath(this.mDeathRecipient, 0);
            for (ModuleWatcher watcher : this.mModuleWatchers) {
                watcher.close();
            }
            this.mModuleWatchers.clear();
        }
    }
}
