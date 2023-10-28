package com.transsion.hubcore.server.mol;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.provider.Settings;
import android.util.Slog;
/* loaded from: classes2.dex */
public class TranMolObserver extends ContentObserver {
    public static final String MOL_SERVICE_ENABLE = "mol_service_enable";
    public static final String TAG = "TranMolObserver";
    private Context mContext;
    private ServiceChangeListener mListener;
    private Object mLock;
    private boolean mRegistered;

    /* loaded from: classes2.dex */
    public interface ServiceChangeListener {
        void onServiceSwitched(boolean z);
    }

    public TranMolObserver(Context context, Handler handler, ServiceChangeListener listener) {
        super(handler);
        this.mLock = new Object();
        this.mContext = context;
        this.mListener = listener;
        this.mRegistered = false;
    }

    public void registerObserver() {
        synchronized (this.mLock) {
            Slog.d(TAG, "registerObserver(), mRegistered:" + this.mRegistered);
            if (!this.mRegistered) {
                this.mRegistered = true;
                this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor(MOL_SERVICE_ENABLE), true, this);
            }
        }
    }

    public void unregisterObserver() {
        synchronized (this.mLock) {
            this.mContext.getContentResolver().unregisterContentObserver(this);
            this.mRegistered = false;
        }
    }

    @Override // android.database.ContentObserver
    public void onChange(boolean selfChange) {
        super.onChange(selfChange);
        ServiceChangeListener serviceChangeListener = this.mListener;
        if (serviceChangeListener != null) {
            serviceChangeListener.onServiceSwitched(isMolEnable());
        }
    }

    public boolean isMolEnable() {
        Context context = this.mContext;
        if (context == null) {
            Slog.d(TAG, "isMolEnable(), mContext is null");
            return false;
        }
        ContentResolver resolver = context.getContentResolver();
        if (resolver == null) {
            Slog.d(TAG, "isMolEnable(), resolver is null");
            return false;
        }
        String enable = Settings.Global.getString(resolver, MOL_SERVICE_ENABLE);
        return "true".equals(enable);
    }
}
