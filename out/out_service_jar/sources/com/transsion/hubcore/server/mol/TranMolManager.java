package com.transsion.hubcore.server.mol;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.UserHandle;
import android.util.Slog;
import android.view.InputMonitor;
import com.android.server.SystemService;
import com.transsion.hubcore.server.mol.TranMolObserver;
import com.transsion.mol.IMolService;
import java.util.List;
/* loaded from: classes2.dex */
public class TranMolManager implements TranMolObserver.ServiceChangeListener {
    private static final String MOL_PKG = "com.transsion.mol";
    private static final String MOL_SERVICE = "com.transsion.mol.service.MolService";
    private static final String TAG = "TranMolManager";
    private static volatile Object mLock = new Object();
    private static TranMolManager sTranMolManager;
    private boolean mConnected;
    private Context mContext;
    private int mCurUserId;
    private Handler mHandler;
    private InputMonitor mInputMonitor;
    private boolean mIsBinding;
    private TranMolObserver mMolOberser;
    private IMolService mMolService;
    private int mReconnectCount;
    private boolean mServiceExist;
    private ServiceConnection mMolConnection = new ServiceConnection() { // from class: com.transsion.hubcore.server.mol.TranMolManager.2
        @Override // android.content.ServiceConnection
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            synchronized (TranMolManager.mLock) {
                Slog.d(TranMolManager.TAG, "onServiceConnected()");
                TranMolManager.this.mMolService = IMolService.Stub.asInterface(iBinder);
                TranMolManager.this.mConnected = true;
                TranMolManager.this.mIsBinding = false;
                TranMolManager.this.mReconnectCount = 0;
                try {
                    if (TranMolManager.this.mMolService != null) {
                        TranMolManager.this.mMolService.asBinder().linkToDeath(TranMolManager.this.mDeathRecipient, 0);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        @Override // android.content.ServiceConnection
        public void onServiceDisconnected(ComponentName componentName) {
            synchronized (TranMolManager.mLock) {
                Slog.d(TranMolManager.TAG, "onServiceDisconnected()");
                TranMolManager.this.mConnected = false;
                TranMolManager.this.mIsBinding = false;
                TranMolManager.this.releaseInputMonitor();
                TranMolManager.this.releaseDeathRecipient();
                Slog.d(TranMolManager.TAG, "onServiceDisconnected, release input monitor and death recipient");
                TranMolManager.this.delayReconnect(2000);
            }
        }
    };
    private IBinder.DeathRecipient mDeathRecipient = new IBinder.DeathRecipient() { // from class: com.transsion.hubcore.server.mol.TranMolManager.3
        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            TranMolManager.this.releaseInputMonitor();
            TranMolManager.this.releaseDeathRecipient();
            Slog.d(TranMolManager.TAG, "Binder is died, release input monitor and death recipient");
        }
    };

    public static TranMolManager getInstance() {
        TranMolManager tranMolManager;
        synchronized (mLock) {
            if (sTranMolManager == null) {
                sTranMolManager = new TranMolManager();
            }
            tranMolManager = sTranMolManager;
        }
        return tranMolManager;
    }

    public void init(Context context) {
        this.mContext = context;
        this.mCurUserId = context.getUserId();
        this.mServiceExist = isServiceExist();
        synchronized (mLock) {
            this.mConnected = false;
            this.mIsBinding = false;
            this.mMolService = null;
        }
        this.mHandler = new Handler(Looper.getMainLooper());
        this.mMolOberser = new TranMolObserver(this.mContext, this.mHandler, this);
        Slog.d(TAG, "init()");
    }

    public void initInputMonitor(InputMonitor inputMonitor) {
        this.mInputMonitor = inputMonitor;
    }

    public void initService() {
    }

    public void onSystemUiStarted() {
        synchronized (mLock) {
            this.mReconnectCount = 5;
        }
        this.mMolOberser.registerObserver();
        delayReconnect(3000);
    }

    private boolean isServiceExist() {
        List<ResolveInfo> resolveInfo;
        if (this.mContext == null) {
            return false;
        }
        Intent queryIntent = getMolBindIntent();
        PackageManager pm = this.mContext.getPackageManager();
        if (pm == null || (resolveInfo = pm.queryIntentServicesAsUser(queryIntent, 0, UserHandle.of(this.mCurUserId))) == null || resolveInfo.size() <= 0) {
            return false;
        }
        return true;
    }

    private Intent getMolBindIntent() {
        Intent bindIntent = new Intent();
        bindIntent.setClassName(MOL_PKG, MOL_SERVICE);
        bindIntent.setPackage(MOL_PKG);
        return bindIntent;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void bindMolService() {
        synchronized (mLock) {
            Slog.d(TAG, "bindMolService(), mContext:" + this.mContext + ", mServiceExist:" + isServiceExist());
            try {
                if (this.mContext != null) {
                    this.mIsBinding = true;
                    Intent bindIntent = getMolBindIntent();
                    Slog.d(TAG, "do bindMolService()");
                    boolean success = this.mContext.bindServiceAsUser(bindIntent, this.mMolConnection, 1, UserHandle.of(this.mCurUserId));
                    Slog.d(TAG, "do bindMolService(), success:" + success);
                    if (!success) {
                        this.mConnected = false;
                        this.mIsBinding = false;
                        int i = this.mReconnectCount;
                        if (i > 0) {
                            this.mReconnectCount = i - 1;
                            delayReconnect(5000);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                Slog.d(TAG, "bindMolService error:" + e.toString());
                this.mConnected = false;
                this.mIsBinding = false;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void unbindMolService() {
        Context context;
        synchronized (mLock) {
            Slog.d(TAG, "unbindMolService(), mConnected:" + this.mConnected + ", mServiceExist:" + isServiceExist());
            try {
                if (this.mConnected && (context = this.mContext) != null) {
                    context.unbindService(this.mMolConnection);
                    this.mConnected = false;
                }
            } catch (Exception e) {
                e.printStackTrace();
                Slog.d(TAG, "unbindMolService error:" + e.toString());
                this.mConnected = false;
            }
            this.mIsBinding = false;
        }
    }

    public void onUserSwitching(SystemService.TargetUser from, final SystemService.TargetUser to) {
        Slog.d(TAG, "onUserSwitching, from:" + from + ",to:" + to);
        this.mHandler.post(new Runnable() { // from class: com.transsion.hubcore.server.mol.TranMolManager.1
            @Override // java.lang.Runnable
            public void run() {
                SystemService.TargetUser targetUser = to;
                if (targetUser != null) {
                    TranMolManager.this.mCurUserId = targetUser.getUserIdentifier();
                    TranMolManager.this.unbindMolService();
                    TranMolManager.this.bindMolService();
                }
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void releaseInputMonitor() {
        Slog.d(TAG, "releaseInputMonitor:" + this.mInputMonitor);
        InputMonitor inputMonitor = this.mInputMonitor;
        if (inputMonitor != null) {
            inputMonitor.dispose();
            this.mInputMonitor = null;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void releaseDeathRecipient() {
        try {
            IMolService iMolService = this.mMolService;
            if (iMolService != null) {
                iMolService.asBinder().unlinkToDeath(this.mDeathRecipient, 0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override // com.transsion.hubcore.server.mol.TranMolObserver.ServiceChangeListener
    public void onServiceSwitched(boolean enable) {
        synchronized (mLock) {
            Slog.d(TAG, "onServiceSwitched(), enable:" + enable + ", mConnected:" + this.mConnected + ", mIsBinding:" + this.mIsBinding + ", mServiceExist:" + isServiceExist());
            this.mReconnectCount = 0;
            if (enable) {
                if (!this.mConnected) {
                    bindMolService();
                }
            } else {
                unbindMolService();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void delayReconnect(int delayTime) {
        this.mHandler.postDelayed(new Runnable() { // from class: com.transsion.hubcore.server.mol.TranMolManager.4
            @Override // java.lang.Runnable
            public void run() {
                TranMolManager.this.tryReconnect();
            }
        }, delayTime);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void tryReconnect() {
        synchronized (mLock) {
            boolean enable = this.mMolOberser.isMolEnable();
            Slog.d(TAG, "tryReconnect(), enable:" + enable + ", mConnected:" + this.mConnected + ", mIsBinding:" + this.mIsBinding + ", mServiceExist:" + isServiceExist());
            if (enable && !this.mConnected && !this.mIsBinding) {
                bindMolService();
            }
        }
    }
}
