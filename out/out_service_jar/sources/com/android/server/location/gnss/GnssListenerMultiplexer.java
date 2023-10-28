package com.android.server.location.gnss;

import android.location.LocationManagerInternal;
import android.location.util.identity.CallerIdentity;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.IInterface;
import android.util.ArraySet;
import com.android.internal.util.Preconditions;
import com.android.server.LocalServices;
import com.android.server.location.gnss.GnssListenerMultiplexer;
import com.android.server.location.injector.AppForegroundHelper;
import com.android.server.location.injector.Injector;
import com.android.server.location.injector.LocationPermissionsHelper;
import com.android.server.location.injector.SettingsHelper;
import com.android.server.location.injector.UserInfoHelper;
import com.android.server.location.listeners.BinderListenerRegistration;
import com.android.server.location.listeners.ListenerMultiplexer;
import com.android.server.location.listeners.ListenerRegistration;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Predicate;
/* loaded from: classes.dex */
public abstract class GnssListenerMultiplexer<TRequest, TListener extends IInterface, TMergedRegistration> extends ListenerMultiplexer<IBinder, TListener, GnssListenerMultiplexer<TRequest, TListener, TMergedRegistration>.GnssListenerRegistration, TMergedRegistration> {
    protected final AppForegroundHelper mAppForegroundHelper;
    protected final LocationPermissionsHelper mLocationPermissionsHelper;
    protected final SettingsHelper mSettingsHelper;
    protected final UserInfoHelper mUserInfoHelper;
    private final UserInfoHelper.UserListener mUserChangedListener = new UserInfoHelper.UserListener() { // from class: com.android.server.location.gnss.GnssListenerMultiplexer$$ExternalSyntheticLambda5
        @Override // com.android.server.location.injector.UserInfoHelper.UserListener
        public final void onUserChanged(int i, int i2) {
            GnssListenerMultiplexer.this.onUserChanged(i, i2);
        }
    };
    private final LocationManagerInternal.ProviderEnabledListener mProviderEnabledChangedListener = new LocationManagerInternal.ProviderEnabledListener() { // from class: com.android.server.location.gnss.GnssListenerMultiplexer$$ExternalSyntheticLambda6
        public final void onProviderEnabledChanged(String str, int i, boolean z) {
            GnssListenerMultiplexer.this.onProviderEnabledChanged(str, i, z);
        }
    };
    private final SettingsHelper.GlobalSettingChangedListener mBackgroundThrottlePackageWhitelistChangedListener = new SettingsHelper.GlobalSettingChangedListener() { // from class: com.android.server.location.gnss.GnssListenerMultiplexer$$ExternalSyntheticLambda7
        @Override // com.android.server.location.injector.SettingsHelper.GlobalSettingChangedListener
        public final void onSettingChanged() {
            GnssListenerMultiplexer.this.onBackgroundThrottlePackageWhitelistChanged();
        }
    };
    private final SettingsHelper.UserSettingChangedListener mLocationPackageBlacklistChangedListener = new SettingsHelper.UserSettingChangedListener() { // from class: com.android.server.location.gnss.GnssListenerMultiplexer$$ExternalSyntheticLambda8
        @Override // com.android.server.location.injector.SettingsHelper.UserSettingChangedListener
        public final void onSettingChanged(int i) {
            GnssListenerMultiplexer.this.onLocationPackageBlacklistChanged(i);
        }
    };
    private final LocationPermissionsHelper.LocationPermissionsListener mLocationPermissionsListener = new LocationPermissionsHelper.LocationPermissionsListener() { // from class: com.android.server.location.gnss.GnssListenerMultiplexer.1
        @Override // com.android.server.location.injector.LocationPermissionsHelper.LocationPermissionsListener
        public void onLocationPermissionsChanged(String packageName) {
            GnssListenerMultiplexer.this.onLocationPermissionsChanged(packageName);
        }

        @Override // com.android.server.location.injector.LocationPermissionsHelper.LocationPermissionsListener
        public void onLocationPermissionsChanged(int uid) {
            GnssListenerMultiplexer.this.onLocationPermissionsChanged(uid);
        }
    };
    private final AppForegroundHelper.AppForegroundListener mAppForegroundChangedListener = new AppForegroundHelper.AppForegroundListener() { // from class: com.android.server.location.gnss.GnssListenerMultiplexer$$ExternalSyntheticLambda9
        @Override // com.android.server.location.injector.AppForegroundHelper.AppForegroundListener
        public final void onAppForegroundChanged(int i, boolean z) {
            GnssListenerMultiplexer.this.onAppForegroundChanged(i, z);
        }
    };
    protected final LocationManagerInternal mLocationManagerInternal = (LocationManagerInternal) Objects.requireNonNull((LocationManagerInternal) LocalServices.getService(LocationManagerInternal.class));

    @Override // com.android.server.location.listeners.ListenerMultiplexer
    protected /* bridge */ /* synthetic */ boolean isActive(ListenerRegistration listenerRegistration) {
        return isActive((GnssListenerRegistration) ((GnssListenerRegistration) listenerRegistration));
    }

    /* JADX INFO: Access modifiers changed from: protected */
    /* loaded from: classes.dex */
    public class GnssListenerRegistration extends BinderListenerRegistration<TRequest, TListener> {
        private boolean mForeground;
        private boolean mPermitted;

        /* JADX INFO: Access modifiers changed from: protected */
        public GnssListenerRegistration(TRequest request, CallerIdentity callerIdentity, TListener listener) {
            super(request, callerIdentity, listener);
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX INFO: Access modifiers changed from: protected */
        @Override // com.android.server.location.listeners.RemovableListenerRegistration
        public GnssListenerMultiplexer<TRequest, TListener, TMergedRegistration> getOwner() {
            return GnssListenerMultiplexer.this;
        }

        public boolean isForeground() {
            return this.mForeground;
        }

        boolean isPermitted() {
            return this.mPermitted;
        }

        @Override // com.android.server.location.listeners.BinderListenerRegistration
        protected final void onBinderListenerRegister() {
            this.mPermitted = GnssListenerMultiplexer.this.mLocationPermissionsHelper.hasLocationPermissions(2, getIdentity());
            this.mForeground = GnssListenerMultiplexer.this.mAppForegroundHelper.isAppForeground(getIdentity().getUid());
            onGnssListenerRegister();
        }

        @Override // com.android.server.location.listeners.BinderListenerRegistration
        protected final void onBinderListenerUnregister() {
            onGnssListenerUnregister();
        }

        protected void onGnssListenerRegister() {
        }

        protected void onGnssListenerUnregister() {
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public boolean onLocationPermissionsChanged(String packageName) {
            if (packageName == null || getIdentity().getPackageName().equals(packageName)) {
                return onLocationPermissionsChanged();
            }
            return false;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public boolean onLocationPermissionsChanged(int uid) {
            if (getIdentity().getUid() == uid) {
                return onLocationPermissionsChanged();
            }
            return false;
        }

        private boolean onLocationPermissionsChanged() {
            boolean permitted = GnssListenerMultiplexer.this.mLocationPermissionsHelper.hasLocationPermissions(2, getIdentity());
            if (permitted != this.mPermitted) {
                this.mPermitted = permitted;
                return true;
            }
            return false;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public boolean onForegroundChanged(int uid, boolean foreground) {
            if (getIdentity().getUid() == uid && foreground != this.mForeground) {
                this.mForeground = foreground;
                return true;
            }
            return false;
        }

        @Override // com.android.server.location.listeners.RequestListenerRegistration, com.android.server.location.listeners.ListenerRegistration
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append(getIdentity());
            ArraySet<String> flags = new ArraySet<>(2);
            if (!this.mForeground) {
                flags.add("bg");
            }
            if (!this.mPermitted) {
                flags.add("na");
            }
            if (!flags.isEmpty()) {
                builder.append(" ").append(flags);
            }
            if (getRequest() != null) {
                builder.append(" ").append(getRequest());
            }
            return builder.toString();
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public GnssListenerMultiplexer(Injector injector) {
        this.mUserInfoHelper = injector.getUserInfoHelper();
        this.mSettingsHelper = injector.getSettingsHelper();
        this.mLocationPermissionsHelper = injector.getLocationPermissionsHelper();
        this.mAppForegroundHelper = injector.getAppForegroundHelper();
    }

    @Override // com.android.server.location.listeners.ListenerMultiplexer
    public String getTag() {
        return GnssManagerService.TAG;
    }

    public boolean isSupported() {
        return true;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void addListener(CallerIdentity identity, TListener listener) {
        addListener(null, identity, listener);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void addListener(TRequest request, CallerIdentity callerIdentity, TListener listener) {
        long identity = Binder.clearCallingIdentity();
        try {
            putRegistration(listener.asBinder(), createRegistration(request, callerIdentity, listener));
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    protected GnssListenerMultiplexer<TRequest, TListener, TMergedRegistration>.GnssListenerRegistration createRegistration(TRequest request, CallerIdentity callerIdentity, TListener listener) {
        return new GnssListenerRegistration(request, callerIdentity, listener);
    }

    public void removeListener(TListener listener) {
        long identity = Binder.clearCallingIdentity();
        try {
            removeRegistration(listener.asBinder());
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    protected boolean isActive(GnssListenerMultiplexer<TRequest, TListener, TMergedRegistration>.GnssListenerRegistration registration) {
        if (isSupported()) {
            CallerIdentity identity = registration.getIdentity();
            if (registration.isPermitted()) {
                return (registration.isForeground() || isBackgroundRestrictionExempt(identity)) && isActive(identity);
            }
            return false;
        }
        return false;
    }

    private boolean isActive(CallerIdentity identity) {
        return identity.isSystemServer() ? this.mLocationManagerInternal.isProviderEnabledForUser("gps", this.mUserInfoHelper.getCurrentUserId()) : this.mLocationManagerInternal.isProviderEnabledForUser("gps", identity.getUserId()) && this.mUserInfoHelper.isCurrentUserId(identity.getUserId()) && !this.mSettingsHelper.isLocationPackageBlacklisted(identity.getUserId(), identity.getPackageName());
    }

    private boolean isBackgroundRestrictionExempt(CallerIdentity identity) {
        if (identity.getUid() == 1000 || this.mSettingsHelper.getBackgroundThrottlePackageWhitelist().contains(identity.getPackageName())) {
            return true;
        }
        return this.mLocationManagerInternal.isProvider((String) null, identity);
    }

    @Override // com.android.server.location.listeners.ListenerMultiplexer
    protected TMergedRegistration mergeRegistrations(Collection<GnssListenerMultiplexer<TRequest, TListener, TMergedRegistration>.GnssListenerRegistration> gnssListenerRegistrations) {
        if (Build.IS_DEBUGGABLE) {
            for (GnssListenerMultiplexer<TRequest, TListener, TMergedRegistration>.GnssListenerRegistration registration : gnssListenerRegistrations) {
                Preconditions.checkState(registration.getRequest() == null);
            }
            return null;
        }
        return null;
    }

    @Override // com.android.server.location.listeners.ListenerMultiplexer
    protected void onRegister() {
        if (!isSupported()) {
            return;
        }
        this.mUserInfoHelper.addListener(this.mUserChangedListener);
        this.mLocationManagerInternal.addProviderEnabledListener("gps", this.mProviderEnabledChangedListener);
        this.mSettingsHelper.addOnBackgroundThrottlePackageWhitelistChangedListener(this.mBackgroundThrottlePackageWhitelistChangedListener);
        this.mSettingsHelper.addOnLocationPackageBlacklistChangedListener(this.mLocationPackageBlacklistChangedListener);
        this.mLocationPermissionsHelper.addListener(this.mLocationPermissionsListener);
        this.mAppForegroundHelper.addListener(this.mAppForegroundChangedListener);
    }

    @Override // com.android.server.location.listeners.ListenerMultiplexer
    protected void onUnregister() {
        if (!isSupported()) {
            return;
        }
        this.mUserInfoHelper.removeListener(this.mUserChangedListener);
        this.mLocationManagerInternal.removeProviderEnabledListener("gps", this.mProviderEnabledChangedListener);
        this.mSettingsHelper.removeOnBackgroundThrottlePackageWhitelistChangedListener(this.mBackgroundThrottlePackageWhitelistChangedListener);
        this.mSettingsHelper.removeOnLocationPackageBlacklistChangedListener(this.mLocationPackageBlacklistChangedListener);
        this.mLocationPermissionsHelper.removeListener(this.mLocationPermissionsListener);
        this.mAppForegroundHelper.removeListener(this.mAppForegroundChangedListener);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onUserChanged(final int userId, int change) {
        if (change == 1) {
            updateRegistrations(new Predicate() { // from class: com.android.server.location.gnss.GnssListenerMultiplexer$$ExternalSyntheticLambda0
                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    return GnssListenerMultiplexer.lambda$onUserChanged$0(userId, (GnssListenerMultiplexer.GnssListenerRegistration) obj);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$onUserChanged$0(int userId, GnssListenerRegistration registration) {
        return registration.getIdentity().getUserId() == userId;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onProviderEnabledChanged(String provider, final int userId, boolean enabled) {
        Preconditions.checkState("gps".equals(provider));
        updateRegistrations(new Predicate() { // from class: com.android.server.location.gnss.GnssListenerMultiplexer$$ExternalSyntheticLambda2
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return GnssListenerMultiplexer.lambda$onProviderEnabledChanged$1(userId, (GnssListenerMultiplexer.GnssListenerRegistration) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$onProviderEnabledChanged$1(int userId, GnssListenerRegistration registration) {
        return registration.getIdentity().getUserId() == userId;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$onBackgroundThrottlePackageWhitelistChanged$2(GnssListenerRegistration registration) {
        return true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onBackgroundThrottlePackageWhitelistChanged() {
        updateRegistrations(new Predicate() { // from class: com.android.server.location.gnss.GnssListenerMultiplexer$$ExternalSyntheticLambda4
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return GnssListenerMultiplexer.lambda$onBackgroundThrottlePackageWhitelistChanged$2((GnssListenerMultiplexer.GnssListenerRegistration) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$onLocationPackageBlacklistChanged$3(int userId, GnssListenerRegistration registration) {
        return registration.getIdentity().getUserId() == userId;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onLocationPackageBlacklistChanged(final int userId) {
        updateRegistrations(new Predicate() { // from class: com.android.server.location.gnss.GnssListenerMultiplexer$$ExternalSyntheticLambda11
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return GnssListenerMultiplexer.lambda$onLocationPackageBlacklistChanged$3(userId, (GnssListenerMultiplexer.GnssListenerRegistration) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onLocationPermissionsChanged(final String packageName) {
        updateRegistrations(new Predicate() { // from class: com.android.server.location.gnss.GnssListenerMultiplexer$$ExternalSyntheticLambda10
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                boolean onLocationPermissionsChanged;
                onLocationPermissionsChanged = ((GnssListenerMultiplexer.GnssListenerRegistration) obj).onLocationPermissionsChanged(packageName);
                return onLocationPermissionsChanged;
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onLocationPermissionsChanged(final int uid) {
        updateRegistrations(new Predicate() { // from class: com.android.server.location.gnss.GnssListenerMultiplexer$$ExternalSyntheticLambda1
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                boolean onLocationPermissionsChanged;
                onLocationPermissionsChanged = ((GnssListenerMultiplexer.GnssListenerRegistration) obj).onLocationPermissionsChanged(uid);
                return onLocationPermissionsChanged;
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onAppForegroundChanged(final int uid, final boolean foreground) {
        updateRegistrations(new Predicate() { // from class: com.android.server.location.gnss.GnssListenerMultiplexer$$ExternalSyntheticLambda3
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                boolean onForegroundChanged;
                onForegroundChanged = ((GnssListenerMultiplexer.GnssListenerRegistration) obj).onForegroundChanged(uid, foreground);
                return onForegroundChanged;
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.location.listeners.ListenerMultiplexer
    public String getServiceState() {
        if (!isSupported()) {
            return "unsupported";
        }
        return super.getServiceState();
    }
}
