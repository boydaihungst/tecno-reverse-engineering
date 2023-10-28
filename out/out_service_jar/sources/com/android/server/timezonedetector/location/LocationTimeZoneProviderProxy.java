package com.android.server.timezonedetector.location;

import android.content.Context;
import android.service.timezone.TimeZoneProviderEvent;
import com.android.server.timezonedetector.Dumpable;
import java.util.Objects;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public abstract class LocationTimeZoneProviderProxy implements Dumpable {
    protected final Context mContext;
    protected Listener mListener;
    protected final Object mSharedLock;
    protected final ThreadingDomain mThreadingDomain;

    /* loaded from: classes2.dex */
    interface Listener {
        void onProviderBound();

        void onProviderUnbound();

        void onReportTimeZoneProviderEvent(TimeZoneProviderEvent timeZoneProviderEvent);
    }

    abstract void onDestroy();

    abstract void onInitialize();

    /* JADX INFO: Access modifiers changed from: package-private */
    public abstract void setRequest(TimeZoneProviderRequest timeZoneProviderRequest);

    /* JADX INFO: Access modifiers changed from: package-private */
    public LocationTimeZoneProviderProxy(Context context, ThreadingDomain threadingDomain) {
        this.mContext = (Context) Objects.requireNonNull(context);
        this.mThreadingDomain = (ThreadingDomain) Objects.requireNonNull(threadingDomain);
        this.mSharedLock = threadingDomain.getLockObject();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void initialize(Listener listener) {
        Objects.requireNonNull(listener);
        synchronized (this.mSharedLock) {
            if (this.mListener != null) {
                throw new IllegalStateException("listener already set");
            }
            this.mListener = listener;
            onInitialize();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void destroy() {
        synchronized (this.mSharedLock) {
            onDestroy();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public final void handleTimeZoneProviderEvent(final TimeZoneProviderEvent timeZoneProviderEvent) {
        this.mThreadingDomain.post(new Runnable() { // from class: com.android.server.timezonedetector.location.LocationTimeZoneProviderProxy$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                LocationTimeZoneProviderProxy.this.m6927xecf3cfb2(timeZoneProviderEvent);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$handleTimeZoneProviderEvent$0$com-android-server-timezonedetector-location-LocationTimeZoneProviderProxy  reason: not valid java name */
    public /* synthetic */ void m6927xecf3cfb2(TimeZoneProviderEvent timeZoneProviderEvent) {
        this.mListener.onReportTimeZoneProviderEvent(timeZoneProviderEvent);
    }
}
