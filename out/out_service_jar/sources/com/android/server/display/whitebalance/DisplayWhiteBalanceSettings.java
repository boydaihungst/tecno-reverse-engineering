package com.android.server.display.whitebalance;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Slog;
import com.android.server.LocalServices;
import com.android.server.display.color.ColorDisplayService;
import com.android.server.display.whitebalance.DisplayWhiteBalanceController;
import java.io.PrintWriter;
import java.util.Objects;
/* loaded from: classes.dex */
public class DisplayWhiteBalanceSettings implements ColorDisplayService.DisplayWhiteBalanceListener {
    private static final int MSG_SET_ACTIVE = 1;
    protected static final String TAG = "DisplayWhiteBalanceSettings";
    private boolean mActive;
    private DisplayWhiteBalanceController.Callbacks mCallbacks;
    private final ColorDisplayService.ColorDisplayServiceInternal mCdsi;
    private final Context mContext;
    private boolean mEnabled;
    private final Handler mHandler;
    protected boolean mLoggingEnabled;

    public DisplayWhiteBalanceSettings(Context context, Handler handler) {
        validateArguments(context, handler);
        this.mLoggingEnabled = false;
        this.mContext = context;
        this.mHandler = new DisplayWhiteBalanceSettingsHandler(handler.getLooper());
        this.mCallbacks = null;
        ColorDisplayService.ColorDisplayServiceInternal colorDisplayServiceInternal = (ColorDisplayService.ColorDisplayServiceInternal) LocalServices.getService(ColorDisplayService.ColorDisplayServiceInternal.class);
        this.mCdsi = colorDisplayServiceInternal;
        setEnabled(colorDisplayServiceInternal.isDisplayWhiteBalanceEnabled());
        boolean isActive = colorDisplayServiceInternal.setDisplayWhiteBalanceListener(this);
        setActive(isActive);
    }

    public boolean setCallbacks(DisplayWhiteBalanceController.Callbacks callbacks) {
        if (this.mCallbacks == callbacks) {
            return false;
        }
        this.mCallbacks = callbacks;
        return true;
    }

    public boolean setLoggingEnabled(boolean loggingEnabled) {
        if (this.mLoggingEnabled == loggingEnabled) {
            return false;
        }
        this.mLoggingEnabled = loggingEnabled;
        return true;
    }

    public boolean isEnabled() {
        return this.mEnabled && this.mActive;
    }

    public void dump(PrintWriter writer) {
        writer.println(TAG);
        writer.println("  mLoggingEnabled=" + this.mLoggingEnabled);
        writer.println("  mContext=" + this.mContext);
        writer.println("  mHandler=" + this.mHandler);
        writer.println("  mEnabled=" + this.mEnabled);
        writer.println("  mActive=" + this.mActive);
        writer.println("  mCallbacks=" + this.mCallbacks);
    }

    @Override // com.android.server.display.color.ColorDisplayService.DisplayWhiteBalanceListener
    public void onDisplayWhiteBalanceStatusChanged(boolean activated) {
        Message msg = this.mHandler.obtainMessage(1, activated ? 1 : 0, 0);
        msg.sendToTarget();
    }

    private void validateArguments(Context context, Handler handler) {
        Objects.requireNonNull(context, "context must not be null");
        Objects.requireNonNull(handler, "handler must not be null");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setEnabled(boolean enabled) {
        if (this.mEnabled == enabled) {
            return;
        }
        if (this.mLoggingEnabled) {
            Slog.d(TAG, "Setting: " + enabled);
        }
        this.mEnabled = enabled;
        DisplayWhiteBalanceController.Callbacks callbacks = this.mCallbacks;
        if (callbacks != null) {
            callbacks.updateWhiteBalance();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setActive(boolean active) {
        if (this.mActive == active) {
            return;
        }
        if (this.mLoggingEnabled) {
            Slog.d(TAG, "Active: " + active);
        }
        this.mActive = active;
        DisplayWhiteBalanceController.Callbacks callbacks = this.mCallbacks;
        if (callbacks != null) {
            callbacks.updateWhiteBalance();
        }
    }

    /* loaded from: classes.dex */
    private final class DisplayWhiteBalanceSettingsHandler extends Handler {
        DisplayWhiteBalanceSettingsHandler(Looper looper) {
            super(looper, null, true);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    DisplayWhiteBalanceSettings.this.setActive(msg.arg1 != 0);
                    DisplayWhiteBalanceSettings displayWhiteBalanceSettings = DisplayWhiteBalanceSettings.this;
                    displayWhiteBalanceSettings.setEnabled(displayWhiteBalanceSettings.mCdsi.isDisplayWhiteBalanceEnabled());
                    return;
                default:
                    return;
            }
        }
    }
}
