package com.android.server.autofill.ui;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.graphics.drawable.Drawable;
import android.metrics.LogMaker;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.service.autofill.Dataset;
import android.service.autofill.FillResponse;
import android.service.autofill.SaveInfo;
import android.service.autofill.ValueFinder;
import android.text.TextUtils;
import android.util.Slog;
import android.view.KeyEvent;
import android.view.autofill.AutofillId;
import android.view.autofill.IAutofillWindowPresenter;
import android.widget.Toast;
import com.android.internal.logging.MetricsLogger;
import com.android.server.LocalServices;
import com.android.server.UiModeManagerInternal;
import com.android.server.UiThread;
import com.android.server.autofill.Helper;
import com.android.server.autofill.ui.DialogFillUi;
import com.android.server.autofill.ui.FillUi;
import com.android.server.autofill.ui.SaveUi;
import com.android.server.location.gnss.hal.GnssNative;
import java.io.PrintWriter;
/* loaded from: classes.dex */
public final class AutoFillUI {
    private static final String TAG = "AutofillUI";
    private AutoFillUiCallback mCallback;
    private final Context mContext;
    private Runnable mCreateFillUiRunnable;
    private DialogFillUi mFillDialog;
    private FillUi mFillUi;
    private final OverlayControl mOverlayControl;
    private SaveUi mSaveUi;
    private AutoFillUiCallback mSaveUiCallback;
    private final Handler mHandler = UiThread.getHandler();
    private final MetricsLogger mMetricsLogger = new MetricsLogger();
    private final UiModeManagerInternal mUiModeMgr = (UiModeManagerInternal) LocalServices.getService(UiModeManagerInternal.class);

    /* loaded from: classes.dex */
    public interface AutoFillUiCallback {
        void authenticate(int i, int i2, IntentSender intentSender, Bundle bundle, boolean z);

        void cancelSave();

        void cancelSession();

        void dispatchUnhandledKey(AutofillId autofillId, KeyEvent keyEvent);

        void fill(int i, int i2, Dataset dataset, int i3);

        void requestFallbackFromFillDialog();

        void requestHideFillUi(AutofillId autofillId);

        void requestShowFillUi(AutofillId autofillId, int i, int i2, IAutofillWindowPresenter iAutofillWindowPresenter);

        void requestShowSoftInput(AutofillId autofillId);

        void save();

        void startIntentSender(IntentSender intentSender, Intent intent);

        void startIntentSenderAndFinishSession(IntentSender intentSender);
    }

    public AutoFillUI(Context context) {
        this.mContext = context;
        this.mOverlayControl = new OverlayControl(context);
    }

    public void setCallback(final AutoFillUiCallback callback) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.autofill.ui.AutoFillUI$$ExternalSyntheticLambda3
            @Override // java.lang.Runnable
            public final void run() {
                AutoFillUI.this.m2088lambda$setCallback$0$comandroidserverautofilluiAutoFillUI(callback);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$setCallback$0$com-android-server-autofill-ui-AutoFillUI  reason: not valid java name */
    public /* synthetic */ void m2088lambda$setCallback$0$comandroidserverautofilluiAutoFillUI(AutoFillUiCallback callback) {
        AutoFillUiCallback autoFillUiCallback = this.mCallback;
        if (autoFillUiCallback != callback) {
            if (autoFillUiCallback != null) {
                if (isSaveUiShowing()) {
                    hideFillUiUiThread(callback, true);
                } else {
                    m2084lambda$hideAll$10$comandroidserverautofilluiAutoFillUI(this.mCallback);
                }
            }
            this.mCallback = callback;
        }
    }

    public void clearCallback(final AutoFillUiCallback callback) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.autofill.ui.AutoFillUI$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                AutoFillUI.this.m2081lambda$clearCallback$1$comandroidserverautofilluiAutoFillUI(callback);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$clearCallback$1$com-android-server-autofill-ui-AutoFillUI  reason: not valid java name */
    public /* synthetic */ void m2081lambda$clearCallback$1$comandroidserverautofilluiAutoFillUI(AutoFillUiCallback callback) {
        if (this.mCallback == callback) {
            m2084lambda$hideAll$10$comandroidserverautofilluiAutoFillUI(callback);
            this.mCallback = null;
        }
    }

    public void showError(int resId, AutoFillUiCallback callback) {
        showError(this.mContext.getString(resId), callback);
    }

    public void showError(final CharSequence message, final AutoFillUiCallback callback) {
        Slog.w(TAG, "showError(): " + ((Object) message));
        this.mHandler.post(new Runnable() { // from class: com.android.server.autofill.ui.AutoFillUI$$ExternalSyntheticLambda6
            @Override // java.lang.Runnable
            public final void run() {
                AutoFillUI.this.m2089lambda$showError$2$comandroidserverautofilluiAutoFillUI(callback, message);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$showError$2$com-android-server-autofill-ui-AutoFillUI  reason: not valid java name */
    public /* synthetic */ void m2089lambda$showError$2$comandroidserverautofilluiAutoFillUI(AutoFillUiCallback callback, CharSequence message) {
        if (this.mCallback != callback) {
            return;
        }
        m2084lambda$hideAll$10$comandroidserverautofilluiAutoFillUI(callback);
        if (!TextUtils.isEmpty(message)) {
            Toast.makeText(this.mContext, message, 1).show();
        }
    }

    public void hideFillUi(final AutoFillUiCallback callback) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.autofill.ui.AutoFillUI$$ExternalSyntheticLambda2
            @Override // java.lang.Runnable
            public final void run() {
                AutoFillUI.this.m2086lambda$hideFillUi$3$comandroidserverautofilluiAutoFillUI(callback);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$hideFillUi$3$com-android-server-autofill-ui-AutoFillUI  reason: not valid java name */
    public /* synthetic */ void m2086lambda$hideFillUi$3$comandroidserverautofilluiAutoFillUI(AutoFillUiCallback callback) {
        hideFillUiUiThread(callback, true);
    }

    public void hideFillDialog(final AutoFillUiCallback callback) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.autofill.ui.AutoFillUI$$ExternalSyntheticLambda8
            @Override // java.lang.Runnable
            public final void run() {
                AutoFillUI.this.m2085x6dbfb39a(callback);
            }
        });
    }

    public void filterFillUi(final String filterText, final AutoFillUiCallback callback) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.autofill.ui.AutoFillUI$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                AutoFillUI.this.m2083lambda$filterFillUi$5$comandroidserverautofilluiAutoFillUI(callback, filterText);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$filterFillUi$5$com-android-server-autofill-ui-AutoFillUI  reason: not valid java name */
    public /* synthetic */ void m2083lambda$filterFillUi$5$comandroidserverautofilluiAutoFillUI(AutoFillUiCallback callback, String filterText) {
        FillUi fillUi;
        if (callback == this.mCallback && (fillUi = this.mFillUi) != null) {
            fillUi.setFilterText(filterText);
        }
    }

    public void showFillUi(final AutofillId focusedId, final FillResponse response, final String filterText, String servicePackageName, ComponentName componentName, final CharSequence serviceLabel, final Drawable serviceIcon, final AutoFillUiCallback callback, int sessionId, boolean compatMode) {
        if (Helper.sDebug) {
            int size = filterText == null ? 0 : filterText.length();
            Slog.d(TAG, "showFillUi(): id=" + focusedId + ", filter=" + size + " chars");
        }
        final LogMaker log = Helper.newLogMaker(910, componentName, servicePackageName, sessionId, compatMode).addTaggedData(911, Integer.valueOf(filterText == null ? 0 : filterText.length())).addTaggedData(909, Integer.valueOf(response.getDatasets() != null ? response.getDatasets().size() : 0));
        Runnable createFillUiRunnable = new Runnable() { // from class: com.android.server.autofill.ui.AutoFillUI$$ExternalSyntheticLambda7
            @Override // java.lang.Runnable
            public final void run() {
                AutoFillUI.this.m2091lambda$showFillUi$6$comandroidserverautofilluiAutoFillUI(callback, response, focusedId, filterText, serviceLabel, serviceIcon, log);
            }
        };
        if (isSaveUiShowing()) {
            if (Helper.sDebug) {
                Slog.d(TAG, "postpone fill UI request..");
            }
            this.mCreateFillUiRunnable = createFillUiRunnable;
            return;
        }
        this.mHandler.post(createFillUiRunnable);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$showFillUi$6$com-android-server-autofill-ui-AutoFillUI  reason: not valid java name */
    public /* synthetic */ void m2091lambda$showFillUi$6$comandroidserverautofilluiAutoFillUI(final AutoFillUiCallback callback, final FillResponse response, final AutofillId focusedId, String filterText, CharSequence serviceLabel, Drawable serviceIcon, final LogMaker log) {
        if (callback != this.mCallback) {
            return;
        }
        m2084lambda$hideAll$10$comandroidserverautofilluiAutoFillUI(callback);
        this.mFillUi = new FillUi(this.mContext, response, focusedId, filterText, this.mOverlayControl, serviceLabel, serviceIcon, this.mUiModeMgr.isNightMode(), new FillUi.Callback() { // from class: com.android.server.autofill.ui.AutoFillUI.1
            @Override // com.android.server.autofill.ui.FillUi.Callback
            public void onResponsePicked(FillResponse response2) {
                log.setType(3);
                AutoFillUI.this.hideFillUiUiThread(callback, true);
                if (AutoFillUI.this.mCallback != null) {
                    AutoFillUI.this.mCallback.authenticate(response2.getRequestId(), GnssNative.GNSS_AIDING_TYPE_ALL, response2.getAuthentication(), response2.getClientState(), false);
                }
            }

            @Override // com.android.server.autofill.ui.FillUi.Callback
            public void onDatasetPicked(Dataset dataset) {
                log.setType(4);
                AutoFillUI.this.hideFillUiUiThread(callback, true);
                if (AutoFillUI.this.mCallback != null) {
                    int datasetIndex = response.getDatasets().indexOf(dataset);
                    AutoFillUI.this.mCallback.fill(response.getRequestId(), datasetIndex, dataset, 1);
                }
            }

            @Override // com.android.server.autofill.ui.FillUi.Callback
            public void onCanceled() {
                log.setType(5);
                AutoFillUI.this.hideFillUiUiThread(callback, true);
            }

            @Override // com.android.server.autofill.ui.FillUi.Callback
            public void onDestroy() {
                if (log.getType() == 0) {
                    log.setType(2);
                }
                AutoFillUI.this.mMetricsLogger.write(log);
            }

            @Override // com.android.server.autofill.ui.FillUi.Callback
            public void requestShowFillUi(int width, int height, IAutofillWindowPresenter windowPresenter) {
                if (AutoFillUI.this.mCallback != null) {
                    AutoFillUI.this.mCallback.requestShowFillUi(focusedId, width, height, windowPresenter);
                }
            }

            @Override // com.android.server.autofill.ui.FillUi.Callback
            public void requestHideFillUi() {
                if (AutoFillUI.this.mCallback != null) {
                    AutoFillUI.this.mCallback.requestHideFillUi(focusedId);
                }
            }

            @Override // com.android.server.autofill.ui.FillUi.Callback
            public void startIntentSender(IntentSender intentSender) {
                if (AutoFillUI.this.mCallback != null) {
                    AutoFillUI.this.mCallback.startIntentSenderAndFinishSession(intentSender);
                }
            }

            @Override // com.android.server.autofill.ui.FillUi.Callback
            public void dispatchUnhandledKey(KeyEvent keyEvent) {
                if (AutoFillUI.this.mCallback != null) {
                    AutoFillUI.this.mCallback.dispatchUnhandledKey(focusedId, keyEvent);
                }
            }

            @Override // com.android.server.autofill.ui.FillUi.Callback
            public void cancelSession() {
                if (AutoFillUI.this.mCallback != null) {
                    AutoFillUI.this.mCallback.cancelSession();
                }
            }
        });
    }

    public void showSaveUi(final CharSequence serviceLabel, final Drawable serviceIcon, final String servicePackageName, final SaveInfo info, final ValueFinder valueFinder, final ComponentName componentName, final AutoFillUiCallback callback, final PendingUi pendingSaveUi, final boolean isUpdate, final boolean compatMode) {
        if (Helper.sVerbose) {
            Slog.v(TAG, "showSaveUi(update=" + isUpdate + ") for " + componentName.toShortString() + ": " + info);
        }
        int numIds = 0 + (info.getRequiredIds() == null ? 0 : info.getRequiredIds().length);
        final LogMaker log = Helper.newLogMaker(916, componentName, servicePackageName, pendingSaveUi.sessionId, compatMode).addTaggedData(917, Integer.valueOf(numIds + (info.getOptionalIds() != null ? info.getOptionalIds().length : 0)));
        if (isUpdate) {
            log.addTaggedData(1555, 1);
        }
        this.mHandler.post(new Runnable() { // from class: com.android.server.autofill.ui.AutoFillUI$$ExternalSyntheticLambda4
            @Override // java.lang.Runnable
            public final void run() {
                AutoFillUI.this.m2092lambda$showSaveUi$7$comandroidserverautofilluiAutoFillUI(callback, pendingSaveUi, serviceLabel, serviceIcon, servicePackageName, componentName, info, valueFinder, log, isUpdate, compatMode);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$showSaveUi$7$com-android-server-autofill-ui-AutoFillUI  reason: not valid java name */
    public /* synthetic */ void m2092lambda$showSaveUi$7$comandroidserverautofilluiAutoFillUI(final AutoFillUiCallback callback, final PendingUi pendingSaveUi, CharSequence serviceLabel, Drawable serviceIcon, String servicePackageName, ComponentName componentName, SaveInfo info, ValueFinder valueFinder, final LogMaker log, boolean isUpdate, boolean compatMode) {
        if (callback != this.mCallback) {
            return;
        }
        m2084lambda$hideAll$10$comandroidserverautofilluiAutoFillUI(callback);
        this.mSaveUiCallback = callback;
        this.mSaveUi = new SaveUi(this.mContext, pendingSaveUi, serviceLabel, serviceIcon, servicePackageName, componentName, info, valueFinder, this.mOverlayControl, new SaveUi.OnSaveListener() { // from class: com.android.server.autofill.ui.AutoFillUI.2
            @Override // com.android.server.autofill.ui.SaveUi.OnSaveListener
            public void onSave() {
                log.setType(4);
                AutoFillUI.this.hideSaveUiUiThread(callback);
                callback.save();
                AutoFillUI.this.destroySaveUiUiThread(pendingSaveUi, true);
            }

            @Override // com.android.server.autofill.ui.SaveUi.OnSaveListener
            public void onCancel(IntentSender listener) {
                log.setType(5);
                AutoFillUI.this.hideSaveUiUiThread(callback);
                if (listener != null) {
                    try {
                        listener.sendIntent(AutoFillUI.this.mContext, 0, null, null, null);
                    } catch (IntentSender.SendIntentException e) {
                        Slog.e(AutoFillUI.TAG, "Error starting negative action listener: " + listener, e);
                    }
                }
                callback.cancelSave();
                AutoFillUI.this.destroySaveUiUiThread(pendingSaveUi, true);
            }

            @Override // com.android.server.autofill.ui.SaveUi.OnSaveListener
            public void onDestroy() {
                if (log.getType() == 0) {
                    log.setType(2);
                    callback.cancelSave();
                }
                AutoFillUI.this.mMetricsLogger.write(log);
            }

            @Override // com.android.server.autofill.ui.SaveUi.OnSaveListener
            public void startIntentSender(IntentSender intentSender, Intent intent) {
                callback.startIntentSender(intentSender, intent);
            }
        }, this.mUiModeMgr.isNightMode(), isUpdate, compatMode);
    }

    public void showFillDialog(final AutofillId focusedId, final FillResponse response, final String filterText, final String servicePackageName, final ComponentName componentName, final Drawable serviceIcon, final AutoFillUiCallback callback, int sessionId, boolean compatMode) {
        if (Helper.sVerbose) {
            Slog.v(TAG, "showFillDialog for " + componentName.toShortString() + ": " + response);
        }
        final LogMaker log = Helper.newLogMaker(910, componentName, servicePackageName, sessionId, compatMode).addTaggedData(911, Integer.valueOf(filterText == null ? 0 : filterText.length())).addTaggedData(909, Integer.valueOf(response.getDatasets() != null ? response.getDatasets().size() : 0));
        this.mHandler.post(new Runnable() { // from class: com.android.server.autofill.ui.AutoFillUI$$ExternalSyntheticLambda10
            @Override // java.lang.Runnable
            public final void run() {
                AutoFillUI.this.m2090xe0a4f219(callback, response, focusedId, filterText, serviceIcon, servicePackageName, componentName, log);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$showFillDialog$8$com-android-server-autofill-ui-AutoFillUI  reason: not valid java name */
    public /* synthetic */ void m2090xe0a4f219(final AutoFillUiCallback callback, final FillResponse response, final AutofillId focusedId, String filterText, Drawable serviceIcon, String servicePackageName, ComponentName componentName, final LogMaker log) {
        if (callback != this.mCallback) {
            return;
        }
        m2084lambda$hideAll$10$comandroidserverautofilluiAutoFillUI(callback);
        this.mFillDialog = new DialogFillUi(this.mContext, response, focusedId, filterText, serviceIcon, servicePackageName, componentName, this.mOverlayControl, this.mUiModeMgr.isNightMode(), new DialogFillUi.UiCallback() { // from class: com.android.server.autofill.ui.AutoFillUI.3
            @Override // com.android.server.autofill.ui.DialogFillUi.UiCallback
            public void onResponsePicked(FillResponse response2) {
                log(3);
                AutoFillUI.this.m2085x6dbfb39a(callback);
                if (AutoFillUI.this.mCallback != null) {
                    AutoFillUI.this.mCallback.authenticate(response2.getRequestId(), GnssNative.GNSS_AIDING_TYPE_ALL, response2.getAuthentication(), response2.getClientState(), false);
                }
            }

            @Override // com.android.server.autofill.ui.DialogFillUi.UiCallback
            public void onDatasetPicked(Dataset dataset) {
                log(4);
                AutoFillUI.this.m2085x6dbfb39a(callback);
                if (AutoFillUI.this.mCallback != null) {
                    int datasetIndex = response.getDatasets().indexOf(dataset);
                    AutoFillUI.this.mCallback.fill(response.getRequestId(), datasetIndex, dataset, 3);
                }
            }

            @Override // com.android.server.autofill.ui.DialogFillUi.UiCallback
            public void onDismissed() {
                log(5);
                AutoFillUI.this.m2085x6dbfb39a(callback);
                callback.requestShowSoftInput(focusedId);
            }

            @Override // com.android.server.autofill.ui.DialogFillUi.UiCallback
            public void onCanceled() {
                log(2);
                AutoFillUI.this.m2085x6dbfb39a(callback);
                callback.requestShowSoftInput(focusedId);
                callback.requestFallbackFromFillDialog();
            }

            @Override // com.android.server.autofill.ui.DialogFillUi.UiCallback
            public void startIntentSender(IntentSender intentSender) {
                AutoFillUI.this.mCallback.startIntentSenderAndFinishSession(intentSender);
            }

            private void log(int type) {
                log.setType(type);
                AutoFillUI.this.mMetricsLogger.write(log);
            }
        });
    }

    public void onPendingSaveUi(final int operation, final IBinder token) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.autofill.ui.AutoFillUI$$ExternalSyntheticLambda11
            @Override // java.lang.Runnable
            public final void run() {
                AutoFillUI.this.m2087x8535e341(operation, token);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onPendingSaveUi$9$com-android-server-autofill-ui-AutoFillUI  reason: not valid java name */
    public /* synthetic */ void m2087x8535e341(int operation, IBinder token) {
        SaveUi saveUi = this.mSaveUi;
        if (saveUi != null) {
            saveUi.onPendingUi(operation, token);
        } else {
            Slog.w(TAG, "onPendingSaveUi(" + operation + "): no save ui");
        }
    }

    public void hideAll(final AutoFillUiCallback callback) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.autofill.ui.AutoFillUI$$ExternalSyntheticLambda5
            @Override // java.lang.Runnable
            public final void run() {
                AutoFillUI.this.m2084lambda$hideAll$10$comandroidserverautofilluiAutoFillUI(callback);
            }
        });
    }

    public void destroyAll(final PendingUi pendingSaveUi, final AutoFillUiCallback callback, final boolean notifyClient) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.autofill.ui.AutoFillUI$$ExternalSyntheticLambda9
            @Override // java.lang.Runnable
            public final void run() {
                AutoFillUI.this.m2082lambda$destroyAll$11$comandroidserverautofilluiAutoFillUI(pendingSaveUi, callback, notifyClient);
            }
        });
    }

    public boolean isSaveUiShowing() {
        SaveUi saveUi = this.mSaveUi;
        if (saveUi == null) {
            return false;
        }
        return saveUi.isShowing();
    }

    public boolean isFillDialogShowing() {
        DialogFillUi dialogFillUi = this.mFillDialog;
        if (dialogFillUi == null) {
            return false;
        }
        return dialogFillUi.isShowing();
    }

    public void dump(PrintWriter pw) {
        pw.println("Autofill UI");
        pw.print("  ");
        pw.print("Night mode: ");
        pw.println(this.mUiModeMgr.isNightMode());
        if (this.mFillUi != null) {
            pw.print("  ");
            pw.println("showsFillUi: true");
            this.mFillUi.dump(pw, "    ");
        } else {
            pw.print("  ");
            pw.println("showsFillUi: false");
        }
        if (this.mSaveUi != null) {
            pw.print("  ");
            pw.println("showsSaveUi: true");
            this.mSaveUi.dump(pw, "    ");
        } else {
            pw.print("  ");
            pw.println("showsSaveUi: false");
        }
        if (this.mFillDialog != null) {
            pw.print("  ");
            pw.println("showsFillDialog: true");
            this.mFillDialog.dump(pw, "    ");
            return;
        }
        pw.print("  ");
        pw.println("showsFillDialog: false");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void hideFillUiUiThread(AutoFillUiCallback callback, boolean notifyClient) {
        FillUi fillUi = this.mFillUi;
        if (fillUi != null) {
            if (callback == null || callback == this.mCallback) {
                fillUi.destroy(notifyClient);
                this.mFillUi = null;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public PendingUi hideSaveUiUiThread(AutoFillUiCallback callback) {
        if (Helper.sVerbose) {
            Slog.v(TAG, "hideSaveUiUiThread(): mSaveUi=" + this.mSaveUi + ", callback=" + callback + ", mCallback=" + this.mCallback);
        }
        SaveUi saveUi = this.mSaveUi;
        if (saveUi != null && this.mSaveUiCallback == callback) {
            return saveUi.hide();
        }
        return null;
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: hideFillDialogUiThread */
    public void m2085x6dbfb39a(AutoFillUiCallback callback) {
        DialogFillUi dialogFillUi = this.mFillDialog;
        if (dialogFillUi != null) {
            if (callback == null || callback == this.mCallback) {
                dialogFillUi.destroy();
                this.mFillDialog = null;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void destroySaveUiUiThread(PendingUi pendingSaveUi, boolean notifyClient) {
        if (this.mSaveUi == null) {
            if (Helper.sDebug) {
                Slog.d(TAG, "destroySaveUiUiThread(): already destroyed");
                return;
            }
            return;
        }
        if (Helper.sDebug) {
            Slog.d(TAG, "destroySaveUiUiThread(): " + pendingSaveUi);
        }
        this.mSaveUi.destroy();
        this.mSaveUi = null;
        this.mSaveUiCallback = null;
        if (pendingSaveUi != null && notifyClient) {
            try {
                if (Helper.sDebug) {
                    Slog.d(TAG, "destroySaveUiUiThread(): notifying client");
                }
                pendingSaveUi.client.setSaveUiState(pendingSaveUi.sessionId, false);
            } catch (RemoteException e) {
                Slog.e(TAG, "Error notifying client to set save UI state to hidden: " + e);
            }
        }
        if (this.mCreateFillUiRunnable != null) {
            if (Helper.sDebug) {
                Slog.d(TAG, "start the pending fill UI request..");
            }
            this.mHandler.post(this.mCreateFillUiRunnable);
            this.mCreateFillUiRunnable = null;
        }
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: destroyAllUiThread */
    public void m2082lambda$destroyAll$11$comandroidserverautofilluiAutoFillUI(PendingUi pendingSaveUi, AutoFillUiCallback callback, boolean notifyClient) {
        hideFillUiUiThread(callback, notifyClient);
        m2085x6dbfb39a(callback);
        destroySaveUiUiThread(pendingSaveUi, notifyClient);
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: hideAllUiThread */
    public void m2084lambda$hideAll$10$comandroidserverautofilluiAutoFillUI(AutoFillUiCallback callback) {
        hideFillUiUiThread(callback, true);
        m2085x6dbfb39a(callback);
        PendingUi pendingSaveUi = hideSaveUiUiThread(callback);
        if (pendingSaveUi != null && pendingSaveUi.getState() == 4) {
            if (Helper.sDebug) {
                Slog.d(TAG, "hideAllUiThread(): destroying Save UI because pending restoration is finished");
            }
            destroySaveUiUiThread(pendingSaveUi, true);
        }
    }
}
