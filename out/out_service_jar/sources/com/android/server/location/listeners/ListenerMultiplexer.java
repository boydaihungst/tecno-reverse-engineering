package com.android.server.location.listeners;

import android.os.Build;
import android.util.ArrayMap;
import android.util.ArraySet;
import com.android.internal.listeners.ListenerExecutor;
import com.android.internal.util.Preconditions;
import com.android.server.location.listeners.ListenerRegistration;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
/* loaded from: classes.dex */
public abstract class ListenerMultiplexer<TKey, TListener, TRegistration extends ListenerRegistration<TListener>, TMergedRegistration> {
    private TMergedRegistration mMerged;
    private final ArrayMap<TKey, TRegistration> mRegistrations = new ArrayMap<>();
    private final ListenerMultiplexer<TKey, TListener, TRegistration, TMergedRegistration>.UpdateServiceBuffer mUpdateServiceBuffer = new UpdateServiceBuffer();
    private final ListenerMultiplexer<TKey, TListener, TRegistration, TMergedRegistration>.ReentrancyGuard mReentrancyGuard = new ReentrancyGuard();
    private int mActiveRegistrationsCount = 0;
    private boolean mServiceRegistered = false;

    public abstract String getTag();

    protected abstract boolean isActive(TRegistration tregistration);

    protected abstract TMergedRegistration mergeRegistrations(Collection<TRegistration> collection);

    protected abstract boolean registerWithService(TMergedRegistration tmergedregistration, Collection<TRegistration> collection);

    protected abstract void unregisterWithService();

    protected boolean reregisterWithService(TMergedRegistration oldMerged, TMergedRegistration newMerged, Collection<TRegistration> registrations) {
        return registerWithService(newMerged, registrations);
    }

    protected void onRegister() {
    }

    protected void onUnregister() {
    }

    protected void onRegistrationAdded(TKey key, TRegistration registration) {
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void onRegistrationReplaced(TKey key, TRegistration oldRegistration, TRegistration newRegistration) {
        onRegistrationAdded(key, newRegistration);
    }

    protected void onRegistrationRemoved(TKey key, TRegistration registration) {
    }

    protected void onActive() {
    }

    protected void onInactive() {
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public final void putRegistration(TKey key, TRegistration registration) {
        replaceRegistration(key, key, registration);
    }

    /* JADX WARN: Removed duplicated region for block: B:19:0x0048  */
    /* JADX WARN: Removed duplicated region for block: B:28:0x0062 A[Catch: all -> 0x0080, TryCatch #5 {all -> 0x0080, blocks: (B:17:0x0039, B:22:0x004c, B:25:0x0055, B:28:0x0062, B:29:0x0065, B:31:0x006a, B:33:0x0071, B:32:0x006e, B:26:0x005b), top: B:58:0x0039 }] */
    /* JADX WARN: Removed duplicated region for block: B:31:0x006a A[Catch: all -> 0x0080, TryCatch #5 {all -> 0x0080, blocks: (B:17:0x0039, B:22:0x004c, B:25:0x0055, B:28:0x0062, B:29:0x0065, B:31:0x006a, B:33:0x0071, B:32:0x006e, B:26:0x005b), top: B:58:0x0039 }] */
    /* JADX WARN: Removed duplicated region for block: B:32:0x006e A[Catch: all -> 0x0080, TryCatch #5 {all -> 0x0080, blocks: (B:17:0x0039, B:22:0x004c, B:25:0x0055, B:28:0x0062, B:29:0x0065, B:31:0x006a, B:33:0x0071, B:32:0x006e, B:26:0x005b), top: B:58:0x0039 }] */
    /* JADX WARN: Removed duplicated region for block: B:35:0x0076 A[Catch: all -> 0x008c, TRY_ENTER, TRY_LEAVE, TryCatch #3 {, blocks: (B:4:0x000c, B:8:0x0019, B:10:0x001e, B:15:0x002a, B:37:0x007b, B:38:0x007e, B:16:0x0033, B:35:0x0076, B:46:0x008b, B:45:0x0088), top: B:57:0x000c }] */
    /* JADX WARN: Removed duplicated region for block: B:37:0x007b A[Catch: all -> 0x0098, TRY_ENTER, TryCatch #3 {, blocks: (B:4:0x000c, B:8:0x0019, B:10:0x001e, B:15:0x002a, B:37:0x007b, B:38:0x007e, B:16:0x0033, B:35:0x0076, B:46:0x008b, B:45:0x0088), top: B:57:0x000c }] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    protected final void replaceRegistration(TKey oldKey, TKey key, TRegistration registration) {
        boolean z;
        ListenerMultiplexer<TKey, TListener, TRegistration, TMergedRegistration>.UpdateServiceBuffer ignored1;
        ListenerMultiplexer<TKey, TListener, TRegistration, TMergedRegistration>.ReentrancyGuard ignored2;
        boolean wasEmpty;
        TRegistration oldRegistration;
        int index;
        Objects.requireNonNull(oldKey);
        Objects.requireNonNull(key);
        Objects.requireNonNull(registration);
        synchronized (this.mRegistrations) {
            boolean z2 = true;
            Preconditions.checkState(!this.mReentrancyGuard.isReentrant());
            try {
                if (oldKey != key && this.mRegistrations.containsKey(key)) {
                    z = false;
                    Preconditions.checkArgument(z);
                    ignored1 = this.mUpdateServiceBuffer.acquire();
                    ignored2 = this.mReentrancyGuard.acquire();
                    wasEmpty = this.mRegistrations.isEmpty();
                    oldRegistration = null;
                    index = this.mRegistrations.indexOfKey(oldKey);
                    if (index >= 0) {
                        if (oldKey == key) {
                            z2 = false;
                        }
                        oldRegistration = removeRegistration(index, z2);
                    }
                    if (oldKey != key && index >= 0) {
                        this.mRegistrations.setValueAt(index, registration);
                    } else {
                        this.mRegistrations.put(key, registration);
                    }
                    if (wasEmpty) {
                        onRegister();
                    }
                    registration.onRegister(key);
                    if (oldRegistration != null) {
                        onRegistrationAdded(key, registration);
                    } else {
                        onRegistrationReplaced(key, oldRegistration, registration);
                    }
                    onRegistrationActiveChanged(registration);
                    if (ignored2 != null) {
                        ignored2.close();
                    }
                    if (ignored1 != null) {
                        ignored1.close();
                    }
                }
                wasEmpty = this.mRegistrations.isEmpty();
                oldRegistration = null;
                index = this.mRegistrations.indexOfKey(oldKey);
                if (index >= 0) {
                }
                if (oldKey != key) {
                }
                this.mRegistrations.put(key, registration);
                if (wasEmpty) {
                }
                registration.onRegister(key);
                if (oldRegistration != null) {
                }
                onRegistrationActiveChanged(registration);
                if (ignored2 != null) {
                }
                if (ignored1 != null) {
                }
            } catch (Throwable th) {
                if (ignored2 != null) {
                    try {
                        ignored2.close();
                    } catch (Throwable th2) {
                        th.addSuppressed(th2);
                    }
                }
                throw th;
            }
            z = true;
            Preconditions.checkArgument(z);
            ignored1 = this.mUpdateServiceBuffer.acquire();
            ignored2 = this.mReentrancyGuard.acquire();
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public final void removeRegistration(Object key) {
        synchronized (this.mRegistrations) {
            Preconditions.checkState(!this.mReentrancyGuard.isReentrant());
            int index = this.mRegistrations.indexOfKey(key);
            if (index < 0) {
                return;
            }
            removeRegistration(index, true);
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public final void removeRegistrationIf(Predicate<TKey> predicate) {
        synchronized (this.mRegistrations) {
            Preconditions.checkState(!this.mReentrancyGuard.isReentrant());
            ListenerMultiplexer<TKey, TListener, TRegistration, TMergedRegistration>.UpdateServiceBuffer ignored1 = this.mUpdateServiceBuffer.acquire();
            ListenerMultiplexer<TKey, TListener, TRegistration, TMergedRegistration>.ReentrancyGuard ignored2 = this.mReentrancyGuard.acquire();
            try {
                int size = this.mRegistrations.size();
                for (int i = 0; i < size; i++) {
                    TKey key = this.mRegistrations.keyAt(i);
                    if (predicate.test(key)) {
                        removeRegistration(key, (ListenerRegistration<?>) this.mRegistrations.valueAt(i));
                    }
                }
                if (ignored2 != null) {
                    ignored2.close();
                }
                if (ignored1 != null) {
                    ignored1.close();
                }
            } catch (Throwable th) {
                if (ignored2 != null) {
                    try {
                        ignored2.close();
                    } catch (Throwable th2) {
                        th.addSuppressed(th2);
                    }
                }
                throw th;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public final void removeRegistration(Object key, ListenerRegistration<?> registration) {
        synchronized (this.mRegistrations) {
            int index = this.mRegistrations.indexOfKey(key);
            if (index < 0) {
                return;
            }
            TRegistration typedRegistration = this.mRegistrations.valueAt(index);
            if (typedRegistration != registration) {
                return;
            }
            if (this.mReentrancyGuard.isReentrant()) {
                unregister(typedRegistration);
                this.mReentrancyGuard.markForRemoval(key, typedRegistration);
            } else {
                removeRegistration(index, true);
            }
        }
    }

    private TRegistration removeRegistration(int index, boolean removeEntry) {
        if (Build.IS_DEBUGGABLE) {
            Preconditions.checkState(Thread.holdsLock(this.mRegistrations));
        }
        TKey key = this.mRegistrations.keyAt(index);
        TRegistration registration = this.mRegistrations.valueAt(index);
        ListenerMultiplexer<TKey, TListener, TRegistration, TMergedRegistration>.UpdateServiceBuffer ignored1 = this.mUpdateServiceBuffer.acquire();
        try {
            ListenerMultiplexer<TKey, TListener, TRegistration, TMergedRegistration>.ReentrancyGuard ignored2 = this.mReentrancyGuard.acquire();
            unregister(registration);
            onRegistrationRemoved(key, registration);
            registration.onUnregister();
            if (removeEntry) {
                this.mRegistrations.removeAt(index);
                if (this.mRegistrations.isEmpty()) {
                    onUnregister();
                }
            }
            if (ignored2 != null) {
                ignored2.close();
            }
            if (ignored1 != null) {
                ignored1.close();
            }
            return registration;
        } catch (Throwable th) {
            if (ignored1 != null) {
                try {
                    ignored1.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
            }
            throw th;
        }
    }

    /* JADX DEBUG: Multi-variable search result rejected for r3v3, resolved type: java.lang.Object */
    /* JADX INFO: Access modifiers changed from: protected */
    /* JADX WARN: Multi-variable type inference failed */
    public final void updateService() {
        synchronized (this.mRegistrations) {
            if (this.mUpdateServiceBuffer.isBuffered()) {
                this.mUpdateServiceBuffer.markUpdateServiceRequired();
                return;
            }
            ArrayList<TRegistration> actives = new ArrayList<>(this.mRegistrations.size());
            int size = this.mRegistrations.size();
            for (int i = 0; i < size; i++) {
                TRegistration registration = this.mRegistrations.valueAt(i);
                if (registration.isActive()) {
                    actives.add(registration);
                }
            }
            if (actives.isEmpty()) {
                if (this.mServiceRegistered) {
                    this.mMerged = null;
                    this.mServiceRegistered = false;
                    unregisterWithService();
                }
                return;
            }
            TMergedRegistration merged = mergeRegistrations(actives);
            if (!this.mServiceRegistered || !Objects.equals(merged, this.mMerged)) {
                if (this.mServiceRegistered) {
                    this.mServiceRegistered = reregisterWithService(this.mMerged, merged, actives);
                } else {
                    this.mServiceRegistered = registerWithService(merged, actives);
                }
                this.mMerged = this.mServiceRegistered ? merged : null;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public final void resetService() {
        synchronized (this.mRegistrations) {
            if (this.mServiceRegistered) {
                this.mMerged = null;
                this.mServiceRegistered = false;
                unregisterWithService();
                updateService();
            }
        }
    }

    public ListenerMultiplexer<TKey, TListener, TRegistration, TMergedRegistration>.UpdateServiceLock newUpdateServiceLock() {
        return new UpdateServiceLock(this.mUpdateServiceBuffer.acquire());
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public final void updateRegistrations(Predicate<TRegistration> predicate) {
        synchronized (this.mRegistrations) {
            ListenerMultiplexer<TKey, TListener, TRegistration, TMergedRegistration>.UpdateServiceBuffer ignored1 = this.mUpdateServiceBuffer.acquire();
            ListenerMultiplexer<TKey, TListener, TRegistration, TMergedRegistration>.ReentrancyGuard ignored2 = this.mReentrancyGuard.acquire();
            try {
                int size = this.mRegistrations.size();
                for (int i = 0; i < size; i++) {
                    TRegistration registration = this.mRegistrations.valueAt(i);
                    if (predicate.test(registration)) {
                        onRegistrationActiveChanged(registration);
                    }
                }
                if (ignored2 != null) {
                    ignored2.close();
                }
                if (ignored1 != null) {
                    ignored1.close();
                }
            } catch (Throwable th) {
                if (ignored2 != null) {
                    try {
                        ignored2.close();
                    } catch (Throwable th2) {
                        th.addSuppressed(th2);
                    }
                }
                throw th;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public final boolean updateRegistration(Object key, Predicate<TRegistration> predicate) {
        synchronized (this.mRegistrations) {
            ListenerMultiplexer<TKey, TListener, TRegistration, TMergedRegistration>.UpdateServiceBuffer ignored1 = this.mUpdateServiceBuffer.acquire();
            ListenerMultiplexer<TKey, TListener, TRegistration, TMergedRegistration>.ReentrancyGuard ignored2 = this.mReentrancyGuard.acquire();
            try {
                int index = this.mRegistrations.indexOfKey(key);
                if (index >= 0) {
                    TRegistration registration = this.mRegistrations.valueAt(index);
                    if (predicate.test(registration)) {
                        onRegistrationActiveChanged(registration);
                    }
                    if (ignored2 != null) {
                        ignored2.close();
                    }
                    if (ignored1 != null) {
                        ignored1.close();
                    }
                    return true;
                }
                if (ignored2 != null) {
                    ignored2.close();
                }
                if (ignored1 != null) {
                    ignored1.close();
                }
                return false;
            } catch (Throwable th) {
                if (ignored2 != null) {
                    try {
                        ignored2.close();
                    } catch (Throwable th2) {
                        th.addSuppressed(th2);
                    }
                }
                throw th;
            }
        }
    }

    private void onRegistrationActiveChanged(TRegistration registration) {
        if (Build.IS_DEBUGGABLE) {
            Preconditions.checkState(Thread.holdsLock(this.mRegistrations));
        }
        boolean active = registration.isRegistered() && isActive(registration);
        boolean changed = registration.setActive(active);
        if (changed) {
            if (active) {
                int i = this.mActiveRegistrationsCount + 1;
                this.mActiveRegistrationsCount = i;
                if (i == 1) {
                    onActive();
                }
                registration.onActive();
            } else {
                registration.onInactive();
                int i2 = this.mActiveRegistrationsCount - 1;
                this.mActiveRegistrationsCount = i2;
                if (i2 == 0) {
                    onInactive();
                }
            }
            updateService();
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public final void deliverToListeners(Function<TRegistration, ListenerExecutor.ListenerOperation<TListener>> function) {
        ListenerExecutor.ListenerOperation<TListener> operation;
        synchronized (this.mRegistrations) {
            ListenerMultiplexer<TKey, TListener, TRegistration, TMergedRegistration>.ReentrancyGuard ignored = this.mReentrancyGuard.acquire();
            int size = this.mRegistrations.size();
            for (int i = 0; i < size; i++) {
                TRegistration registration = this.mRegistrations.valueAt(i);
                if (registration.isActive() && (operation = function.apply(registration)) != null) {
                    registration.executeOperation(operation);
                }
            }
            if (ignored != null) {
                ignored.close();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public final void deliverToListeners(ListenerExecutor.ListenerOperation<TListener> operation) {
        synchronized (this.mRegistrations) {
            ListenerMultiplexer<TKey, TListener, TRegistration, TMergedRegistration>.ReentrancyGuard ignored = this.mReentrancyGuard.acquire();
            int size = this.mRegistrations.size();
            for (int i = 0; i < size; i++) {
                TRegistration registration = this.mRegistrations.valueAt(i);
                if (registration.isActive()) {
                    registration.executeOperation(operation);
                }
            }
            if (ignored != null) {
                ignored.close();
            }
        }
    }

    private void unregister(TRegistration registration) {
        registration.unregisterInternal();
        onRegistrationActiveChanged(registration);
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        synchronized (this.mRegistrations) {
            pw.print("service: ");
            pw.print(getServiceState());
            pw.println();
            if (!this.mRegistrations.isEmpty()) {
                pw.println("listeners:");
                int size = this.mRegistrations.size();
                for (int i = 0; i < size; i++) {
                    TRegistration registration = this.mRegistrations.valueAt(i);
                    pw.print("  ");
                    pw.print(registration);
                    if (!registration.isActive()) {
                        pw.println(" (inactive)");
                    } else {
                        pw.println();
                    }
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public String getServiceState() {
        if (this.mServiceRegistered) {
            TMergedRegistration tmergedregistration = this.mMerged;
            if (tmergedregistration != null) {
                return tmergedregistration.toString();
            }
            return "registered";
        }
        return "unregistered";
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class ReentrancyGuard implements AutoCloseable {
        private int mGuardCount = 0;
        private ArraySet<Map.Entry<Object, ListenerRegistration<?>>> mScheduledRemovals = null;

        ReentrancyGuard() {
        }

        boolean isReentrant() {
            if (Build.IS_DEBUGGABLE) {
                Preconditions.checkState(Thread.holdsLock(ListenerMultiplexer.this.mRegistrations));
            }
            return this.mGuardCount != 0;
        }

        void markForRemoval(Object key, ListenerRegistration<?> registration) {
            if (Build.IS_DEBUGGABLE) {
                Preconditions.checkState(Thread.holdsLock(ListenerMultiplexer.this.mRegistrations));
            }
            Preconditions.checkState(isReentrant());
            if (this.mScheduledRemovals == null) {
                this.mScheduledRemovals = new ArraySet<>(ListenerMultiplexer.this.mRegistrations.size());
            }
            this.mScheduledRemovals.add(new AbstractMap.SimpleImmutableEntry(key, registration));
        }

        ListenerMultiplexer<TKey, TListener, TRegistration, TMergedRegistration>.ReentrancyGuard acquire() {
            this.mGuardCount++;
            return this;
        }

        @Override // java.lang.AutoCloseable
        public void close() {
            ArraySet<Map.Entry<Object, ListenerRegistration<?>>> scheduledRemovals = null;
            Preconditions.checkState(this.mGuardCount > 0);
            int i = this.mGuardCount - 1;
            this.mGuardCount = i;
            if (i == 0) {
                scheduledRemovals = this.mScheduledRemovals;
                this.mScheduledRemovals = null;
            }
            if (scheduledRemovals == null) {
                return;
            }
            ListenerMultiplexer<TKey, TListener, TRegistration, TMergedRegistration>.UpdateServiceBuffer ignored = ListenerMultiplexer.this.mUpdateServiceBuffer.acquire();
            try {
                int size = scheduledRemovals.size();
                for (int i2 = 0; i2 < size; i2++) {
                    Map.Entry<Object, ListenerRegistration<?>> entry = scheduledRemovals.valueAt(i2);
                    ListenerMultiplexer.this.removeRegistration(entry.getKey(), entry.getValue());
                }
                if (ignored != null) {
                    ignored.close();
                }
            } catch (Throwable th) {
                if (ignored != null) {
                    try {
                        ignored.close();
                    } catch (Throwable th2) {
                        th.addSuppressed(th2);
                    }
                }
                throw th;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class UpdateServiceBuffer implements AutoCloseable {
        private int mBufferCount = 0;
        private boolean mUpdateServiceRequired = false;

        UpdateServiceBuffer() {
        }

        synchronized boolean isBuffered() {
            return this.mBufferCount != 0;
        }

        synchronized void markUpdateServiceRequired() {
            Preconditions.checkState(isBuffered());
            this.mUpdateServiceRequired = true;
        }

        synchronized ListenerMultiplexer<TKey, TListener, TRegistration, TMergedRegistration>.UpdateServiceBuffer acquire() {
            this.mBufferCount++;
            return this;
        }

        @Override // java.lang.AutoCloseable
        public void close() {
            boolean updateServiceRequired = false;
            synchronized (this) {
                Preconditions.checkState(this.mBufferCount > 0);
                int i = this.mBufferCount - 1;
                this.mBufferCount = i;
                if (i == 0) {
                    updateServiceRequired = this.mUpdateServiceRequired;
                    this.mUpdateServiceRequired = false;
                }
            }
            if (updateServiceRequired) {
                ListenerMultiplexer.this.updateService();
            }
        }
    }

    /* loaded from: classes.dex */
    public final class UpdateServiceLock implements AutoCloseable {
        private ListenerMultiplexer<TKey, TListener, TRegistration, TMergedRegistration>.UpdateServiceBuffer mUpdateServiceBuffer;

        UpdateServiceLock(ListenerMultiplexer<TKey, TListener, TRegistration, TMergedRegistration>.UpdateServiceBuffer updateServiceBuffer) {
            this.mUpdateServiceBuffer = updateServiceBuffer;
        }

        @Override // java.lang.AutoCloseable
        public void close() {
            if (this.mUpdateServiceBuffer != null) {
                ListenerMultiplexer<TKey, TListener, TRegistration, TMergedRegistration>.UpdateServiceBuffer buffer = this.mUpdateServiceBuffer;
                this.mUpdateServiceBuffer = null;
                buffer.close();
            }
        }
    }
}
