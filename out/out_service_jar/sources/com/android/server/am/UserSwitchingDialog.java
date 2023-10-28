package com.android.server.am;

import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.UserInfo;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.os.UserManager;
import android.util.Slog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.TextView;
import com.android.server.backup.BackupAgentTimeoutParameters;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class UserSwitchingDialog extends AlertDialog implements ViewTreeObserver.OnWindowShownListener {
    protected static final boolean DEBUG = true;
    private static final int MSG_START_USER = 1;
    private static final String TAG = "ActivityManagerUserSwitchingDialog";
    private static final int WINDOW_SHOWN_TIMEOUT_MS = 3000;
    protected final Context mContext;
    private final Handler mHandler;
    protected final UserInfo mNewUser;
    protected final UserInfo mOldUser;
    private final ActivityManagerService mService;
    private boolean mStartedUser;
    private final String mSwitchingFromSystemUserMessage;
    private final String mSwitchingToSystemUserMessage;
    private final int mUserId;

    public UserSwitchingDialog(ActivityManagerService service, Context context, UserInfo oldUser, UserInfo newUser, boolean aboveSystem, String switchingFromSystemUserMessage, String switchingToSystemUserMessage) {
        super(context);
        this.mHandler = new Handler() { // from class: com.android.server.am.UserSwitchingDialog.1
            @Override // android.os.Handler
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 1:
                        Slog.w(UserSwitchingDialog.TAG, "user switch window not shown in 3000 ms");
                        UserSwitchingDialog.this.startUser();
                        return;
                    default:
                        return;
                }
            }
        };
        this.mContext = context;
        this.mService = service;
        this.mUserId = newUser.id;
        this.mOldUser = oldUser;
        this.mNewUser = newUser;
        this.mSwitchingFromSystemUserMessage = switchingFromSystemUserMessage;
        this.mSwitchingToSystemUserMessage = switchingToSystemUserMessage;
        inflateContent();
        if (aboveSystem) {
            getWindow().setType(2010);
        }
        WindowManager.LayoutParams attrs = getWindow().getAttributes();
        attrs.privateFlags = 272;
        getWindow().setAttributes(attrs);
    }

    void inflateContent() {
        String viewMessage;
        setCancelable(false);
        Resources res = getContext().getResources();
        TextView view = (TextView) LayoutInflater.from(getContext()).inflate(17367371, (ViewGroup) null);
        String viewMessage2 = null;
        if (UserManager.isSplitSystemUser() && this.mNewUser.id == 0) {
            viewMessage = res.getString(17041686, this.mOldUser.name);
        } else if (UserManager.isDeviceInDemoMode(this.mContext)) {
            if (this.mOldUser.isDemo()) {
                viewMessage = res.getString(17040150);
            } else {
                viewMessage = res.getString(17040151);
            }
        } else {
            if (this.mOldUser.id == 0) {
                viewMessage2 = this.mSwitchingFromSystemUserMessage;
            } else if (this.mNewUser.id == 0) {
                viewMessage2 = this.mSwitchingToSystemUserMessage;
            }
            if (viewMessage2 != null) {
                viewMessage = viewMessage2;
            } else {
                viewMessage = res.getString(17041689, this.mNewUser.name);
            }
            view.setCompoundDrawablesWithIntrinsicBounds((Drawable) null, getContext().getDrawable(17302904), (Drawable) null, (Drawable) null);
        }
        view.setAccessibilityPaneTitle(viewMessage);
        view.setText(viewMessage);
        setView(view);
    }

    @Override // android.app.Dialog
    public void show() {
        Slog.d(TAG, "show called");
        super.show();
        View decorView = getWindow().getDecorView();
        if (decorView != null) {
            decorView.getViewTreeObserver().addOnWindowShownListener(this);
        }
        Handler handler = this.mHandler;
        handler.sendMessageDelayed(handler.obtainMessage(1), BackupAgentTimeoutParameters.DEFAULT_QUOTA_EXCEEDED_TIMEOUT_MILLIS);
    }

    public void onWindowShown() {
        Slog.d(TAG, "onWindowShown called");
        startUser();
    }

    void startUser() {
        synchronized (this) {
            if (!this.mStartedUser) {
                Slog.i(TAG, "starting user " + this.mUserId);
                this.mService.mUserController.startUserInForeground(this.mUserId);
                dismiss();
                this.mStartedUser = true;
                View decorView = getWindow().getDecorView();
                if (decorView != null) {
                    decorView.getViewTreeObserver().removeOnWindowShownListener(this);
                }
                this.mHandler.removeMessages(1);
            } else {
                Slog.i(TAG, "user " + this.mUserId + " already started");
            }
        }
    }
}
