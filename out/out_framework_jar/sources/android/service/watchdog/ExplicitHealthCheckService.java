package android.service.watchdog;

import android.annotation.SystemApi;
import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteCallback;
import android.os.RemoteException;
import android.service.watchdog.ExplicitHealthCheckService;
import android.service.watchdog.IExplicitHealthCheckService;
import android.util.Log;
import com.android.internal.util.Preconditions;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
@SystemApi
/* loaded from: classes3.dex */
public abstract class ExplicitHealthCheckService extends Service {
    public static final String BIND_PERMISSION = "android.permission.BIND_EXPLICIT_HEALTH_CHECK_SERVICE";
    public static final String EXTRA_HEALTH_CHECK_PASSED_PACKAGE = "android.service.watchdog.extra.health_check_passed_package";
    public static final String EXTRA_REQUESTED_PACKAGES = "android.service.watchdog.extra.requested_packages";
    public static final String EXTRA_SUPPORTED_PACKAGES = "android.service.watchdog.extra.supported_packages";
    public static final String SERVICE_INTERFACE = "android.service.watchdog.ExplicitHealthCheckService";
    private static final String TAG = "ExplicitHealthCheckService";
    private RemoteCallback mCallback;
    private final ExplicitHealthCheckServiceWrapper mWrapper = new ExplicitHealthCheckServiceWrapper();
    private final Handler mHandler = new Handler(Looper.getMainLooper(), null, true);

    public abstract void onCancelHealthCheck(String str);

    public abstract List<String> onGetRequestedPackages();

    public abstract List<PackageConfig> onGetSupportedPackages();

    public abstract void onRequestHealthCheck(String str);

    @Override // android.app.Service
    public final IBinder onBind(Intent intent) {
        return this.mWrapper;
    }

    public void setCallback(RemoteCallback callback) {
        this.mCallback = callback;
    }

    public final void notifyHealthCheckPassed(final String packageName) {
        this.mHandler.post(new Runnable() { // from class: android.service.watchdog.ExplicitHealthCheckService$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                ExplicitHealthCheckService.this.m3745x409119a5(packageName);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$notifyHealthCheckPassed$0$android-service-watchdog-ExplicitHealthCheckService  reason: not valid java name */
    public /* synthetic */ void m3745x409119a5(String packageName) {
        if (this.mCallback != null) {
            Objects.requireNonNull(packageName, "Package passing explicit health check must be non-null");
            Bundle bundle = new Bundle();
            bundle.putString(EXTRA_HEALTH_CHECK_PASSED_PACKAGE, packageName);
            this.mCallback.sendResult(bundle);
            return;
        }
        Log.wtf(TAG, "System missed explicit health check result for " + packageName);
    }

    @SystemApi
    /* loaded from: classes3.dex */
    public static final class PackageConfig implements Parcelable {
        private final long mHealthCheckTimeoutMillis;
        private final String mPackageName;
        private static final long DEFAULT_HEALTH_CHECK_TIMEOUT_MILLIS = TimeUnit.HOURS.toMillis(1);
        public static final Parcelable.Creator<PackageConfig> CREATOR = new Parcelable.Creator<PackageConfig>() { // from class: android.service.watchdog.ExplicitHealthCheckService.PackageConfig.1
            /* JADX DEBUG: Method merged with bridge method */
            /* JADX WARN: Can't rename method to resolve collision */
            @Override // android.os.Parcelable.Creator
            public PackageConfig createFromParcel(Parcel source) {
                return new PackageConfig(source);
            }

            /* JADX DEBUG: Method merged with bridge method */
            /* JADX WARN: Can't rename method to resolve collision */
            @Override // android.os.Parcelable.Creator
            public PackageConfig[] newArray(int size) {
                return new PackageConfig[size];
            }
        };

        public PackageConfig(String packageName, long healthCheckTimeoutMillis) {
            this.mPackageName = (String) Preconditions.checkNotNull(packageName);
            if (healthCheckTimeoutMillis == 0) {
                this.mHealthCheckTimeoutMillis = DEFAULT_HEALTH_CHECK_TIMEOUT_MILLIS;
            } else {
                this.mHealthCheckTimeoutMillis = Preconditions.checkArgumentNonnegative(healthCheckTimeoutMillis);
            }
        }

        private PackageConfig(Parcel parcel) {
            this.mPackageName = parcel.readString();
            this.mHealthCheckTimeoutMillis = parcel.readLong();
        }

        public String getPackageName() {
            return this.mPackageName;
        }

        public long getHealthCheckTimeoutMillis() {
            return this.mHealthCheckTimeoutMillis;
        }

        public String toString() {
            return "PackageConfig{" + this.mPackageName + ", " + this.mHealthCheckTimeoutMillis + "}";
        }

        public boolean equals(Object other) {
            if (other == this) {
                return true;
            }
            if (other instanceof PackageConfig) {
                PackageConfig otherInfo = (PackageConfig) other;
                return Objects.equals(Long.valueOf(otherInfo.getHealthCheckTimeoutMillis()), Long.valueOf(this.mHealthCheckTimeoutMillis)) && Objects.equals(otherInfo.getPackageName(), this.mPackageName);
            }
            return false;
        }

        public int hashCode() {
            return Objects.hash(this.mPackageName, Long.valueOf(this.mHealthCheckTimeoutMillis));
        }

        @Override // android.os.Parcelable
        public int describeContents() {
            return 0;
        }

        @Override // android.os.Parcelable
        public void writeToParcel(Parcel parcel, int flags) {
            parcel.writeString(this.mPackageName);
            parcel.writeLong(this.mHealthCheckTimeoutMillis);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes3.dex */
    public class ExplicitHealthCheckServiceWrapper extends IExplicitHealthCheckService.Stub {
        private ExplicitHealthCheckServiceWrapper() {
        }

        @Override // android.service.watchdog.IExplicitHealthCheckService
        public void setCallback(final RemoteCallback callback) throws RemoteException {
            ExplicitHealthCheckService.this.mHandler.post(new Runnable() { // from class: android.service.watchdog.ExplicitHealthCheckService$ExplicitHealthCheckServiceWrapper$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    ExplicitHealthCheckService.ExplicitHealthCheckServiceWrapper.this.m3750xb34d300b(callback);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$setCallback$0$android-service-watchdog-ExplicitHealthCheckService$ExplicitHealthCheckServiceWrapper  reason: not valid java name */
        public /* synthetic */ void m3750xb34d300b(RemoteCallback callback) {
            ExplicitHealthCheckService.this.mCallback = callback;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$request$1$android-service-watchdog-ExplicitHealthCheckService$ExplicitHealthCheckServiceWrapper  reason: not valid java name */
        public /* synthetic */ void m3749xbe1da694(String packageName) {
            ExplicitHealthCheckService.this.onRequestHealthCheck(packageName);
        }

        @Override // android.service.watchdog.IExplicitHealthCheckService
        public void request(final String packageName) throws RemoteException {
            ExplicitHealthCheckService.this.mHandler.post(new Runnable() { // from class: android.service.watchdog.ExplicitHealthCheckService$ExplicitHealthCheckServiceWrapper$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    ExplicitHealthCheckService.ExplicitHealthCheckServiceWrapper.this.m3749xbe1da694(packageName);
                }
            });
        }

        @Override // android.service.watchdog.IExplicitHealthCheckService
        public void cancel(final String packageName) throws RemoteException {
            ExplicitHealthCheckService.this.mHandler.post(new Runnable() { // from class: android.service.watchdog.ExplicitHealthCheckService$ExplicitHealthCheckServiceWrapper$$ExternalSyntheticLambda2
                @Override // java.lang.Runnable
                public final void run() {
                    ExplicitHealthCheckService.ExplicitHealthCheckServiceWrapper.this.m3746x8619a41a(packageName);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$cancel$2$android-service-watchdog-ExplicitHealthCheckService$ExplicitHealthCheckServiceWrapper  reason: not valid java name */
        public /* synthetic */ void m3746x8619a41a(String packageName) {
            ExplicitHealthCheckService.this.onCancelHealthCheck(packageName);
        }

        @Override // android.service.watchdog.IExplicitHealthCheckService
        public void getSupportedPackages(final RemoteCallback callback) throws RemoteException {
            ExplicitHealthCheckService.this.mHandler.post(new Runnable() { // from class: android.service.watchdog.ExplicitHealthCheckService$ExplicitHealthCheckServiceWrapper$$ExternalSyntheticLambda3
                @Override // java.lang.Runnable
                public final void run() {
                    ExplicitHealthCheckService.ExplicitHealthCheckServiceWrapper.this.m3748x1bbfe626(callback);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$getSupportedPackages$3$android-service-watchdog-ExplicitHealthCheckService$ExplicitHealthCheckServiceWrapper  reason: not valid java name */
        public /* synthetic */ void m3748x1bbfe626(RemoteCallback callback) {
            List<PackageConfig> packages = ExplicitHealthCheckService.this.onGetSupportedPackages();
            Objects.requireNonNull(packages, "Supported package list must be non-null");
            Bundle bundle = new Bundle();
            bundle.putParcelableArrayList(ExplicitHealthCheckService.EXTRA_SUPPORTED_PACKAGES, new ArrayList<>(packages));
            callback.sendResult(bundle);
        }

        @Override // android.service.watchdog.IExplicitHealthCheckService
        public void getRequestedPackages(final RemoteCallback callback) throws RemoteException {
            ExplicitHealthCheckService.this.mHandler.post(new Runnable() { // from class: android.service.watchdog.ExplicitHealthCheckService$ExplicitHealthCheckServiceWrapper$$ExternalSyntheticLambda4
                @Override // java.lang.Runnable
                public final void run() {
                    ExplicitHealthCheckService.ExplicitHealthCheckServiceWrapper.this.m3747x5ba0ba27(callback);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$getRequestedPackages$4$android-service-watchdog-ExplicitHealthCheckService$ExplicitHealthCheckServiceWrapper  reason: not valid java name */
        public /* synthetic */ void m3747x5ba0ba27(RemoteCallback callback) {
            List<String> packages = ExplicitHealthCheckService.this.onGetRequestedPackages();
            Objects.requireNonNull(packages, "Requested  package list must be non-null");
            Bundle bundle = new Bundle();
            bundle.putStringArrayList(ExplicitHealthCheckService.EXTRA_REQUESTED_PACKAGES, new ArrayList<>(packages));
            callback.sendResult(bundle);
        }
    }
}
