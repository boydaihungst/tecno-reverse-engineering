package com.android.server.autofill.ui;

import android.app.Dialog;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.metrics.LogMaker;
import android.os.Handler;
import android.os.IBinder;
import android.os.UserHandle;
import android.service.autofill.BatchUpdates;
import android.service.autofill.CustomDescription;
import android.service.autofill.InternalOnClickAction;
import android.service.autofill.InternalTransformation;
import android.service.autofill.InternalValidator;
import android.service.autofill.SaveInfo;
import android.service.autofill.ValueFinder;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.ArraySet;
import android.util.Pair;
import android.util.Slog;
import android.util.SparseArray;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RemoteViews;
import android.widget.TextView;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.util.ArrayUtils;
import com.android.server.UiThread;
import com.android.server.autofill.Helper;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public final class SaveUi {
    private static final int SCROLL_BAR_DEFAULT_DELAY_BEFORE_FADE_MS = 500;
    private static final String TAG = "SaveUi";
    private static final int THEME_ID_DARK = 16974827;
    private static final int THEME_ID_LIGHT = 16974838;
    private final boolean mCompatMode;
    private final ComponentName mComponentName;
    private boolean mDestroyed;
    private final Dialog mDialog;
    private final OneActionThenDestroyListener mListener;
    private final OverlayControl mOverlayControl;
    private final PendingUi mPendingUi;
    private final String mServicePackageName;
    private final CharSequence mSubTitle;
    private final int mThemeId;
    private final CharSequence mTitle;
    private final int mType;
    private final Handler mHandler = UiThread.getHandler();
    private final MetricsLogger mMetricsLogger = new MetricsLogger();

    /* loaded from: classes.dex */
    public interface OnSaveListener {
        void onCancel(IntentSender intentSender);

        void onDestroy();

        void onSave();

        void startIntentSender(IntentSender intentSender, Intent intent);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class OneActionThenDestroyListener implements OnSaveListener {
        private boolean mDone;
        private final OnSaveListener mRealListener;

        OneActionThenDestroyListener(OnSaveListener realListener) {
            this.mRealListener = realListener;
        }

        @Override // com.android.server.autofill.ui.SaveUi.OnSaveListener
        public void onSave() {
            if (Helper.sDebug) {
                Slog.d(SaveUi.TAG, "OneTimeListener.onSave(): " + this.mDone);
            }
            if (this.mDone) {
                return;
            }
            this.mRealListener.onSave();
        }

        @Override // com.android.server.autofill.ui.SaveUi.OnSaveListener
        public void onCancel(IntentSender listener) {
            if (Helper.sDebug) {
                Slog.d(SaveUi.TAG, "OneTimeListener.onCancel(): " + this.mDone);
            }
            if (this.mDone) {
                return;
            }
            this.mRealListener.onCancel(listener);
        }

        @Override // com.android.server.autofill.ui.SaveUi.OnSaveListener
        public void onDestroy() {
            if (Helper.sDebug) {
                Slog.d(SaveUi.TAG, "OneTimeListener.onDestroy(): " + this.mDone);
            }
            if (this.mDone) {
                return;
            }
            this.mDone = true;
            this.mRealListener.onDestroy();
        }

        @Override // com.android.server.autofill.ui.SaveUi.OnSaveListener
        public void startIntentSender(IntentSender intentSender, Intent intent) {
            if (Helper.sDebug) {
                Slog.d(SaveUi.TAG, "OneTimeListener.startIntentSender(): " + this.mDone);
            }
            if (this.mDone) {
                return;
            }
            this.mRealListener.startIntentSender(intentSender, intent);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public SaveUi(Context context, PendingUi pendingUi, CharSequence serviceLabel, Drawable serviceIcon, String servicePackageName, ComponentName componentName, final SaveInfo info, ValueFinder valueFinder, OverlayControl overlayControl, OnSaveListener listener, boolean nightMode, boolean isUpdate, boolean compatMode) {
        if (Helper.sVerbose) {
            Slog.v(TAG, "nightMode: " + nightMode);
        }
        int i = nightMode ? THEME_ID_DARK : THEME_ID_LIGHT;
        this.mThemeId = i;
        this.mPendingUi = pendingUi;
        this.mListener = new OneActionThenDestroyListener(listener);
        this.mOverlayControl = overlayControl;
        this.mServicePackageName = servicePackageName;
        this.mComponentName = componentName;
        this.mCompatMode = compatMode;
        Context context2 = new ContextThemeWrapper(context, i) { // from class: com.android.server.autofill.ui.SaveUi.1
            @Override // android.content.ContextWrapper, android.content.Context
            public void startActivity(Intent intent) {
                if (resolveActivity(intent) == null) {
                    if (Helper.sDebug) {
                        Slog.d(SaveUi.TAG, "Can not startActivity for save UI with intent=" + intent);
                        return;
                    }
                    return;
                }
                intent.putExtra("android.view.autofill.extra.RESTORE_CROSS_ACTIVITY", true);
                PendingIntent p = PendingIntent.getActivityAsUser(this, 0, intent, 33554432, null, UserHandle.CURRENT);
                if (Helper.sDebug) {
                    Slog.d(SaveUi.TAG, "startActivity add save UI restored with intent=" + intent);
                }
                SaveUi.this.startIntentSenderWithRestore(p, intent);
            }

            private ComponentName resolveActivity(Intent intent) {
                PackageManager packageManager = getPackageManager();
                ComponentName componentName2 = intent.resolveActivity(packageManager);
                if (componentName2 != null) {
                    return componentName2;
                }
                intent.addFlags(2048);
                ActivityInfo ai = intent.resolveActivityInfo(packageManager, 8388608);
                if (ai != null) {
                    return new ComponentName(ai.applicationInfo.packageName, ai.name);
                }
                return null;
            }
        };
        LayoutInflater inflater = LayoutInflater.from(context2);
        View view = inflater.inflate(17367110, (ViewGroup) null);
        TextView titleView = (TextView) view.findViewById(16908812);
        ArraySet<String> types = new ArraySet<>(3);
        int type = info.getType();
        this.mType = type;
        if ((type & 1) != 0) {
            types.add(context2.getString(17039775));
        }
        if ((type & 2) != 0) {
            types.add(context2.getString(17039770));
        }
        int count = Integer.bitCount(type & 100);
        if (count > 1 || (type & 128) != 0) {
            types.add(context2.getString(17039774));
        } else if ((type & 64) != 0) {
            types.add(context2.getString(17039776));
        } else if ((type & 4) != 0) {
            types.add(context2.getString(17039771));
        } else if ((type & 32) != 0) {
            types.add(context2.getString(17039772));
        }
        if ((type & 8) != 0) {
            types.add(context2.getString(17039777));
        }
        if ((type & 16) != 0) {
            types.add(context2.getString(17039773));
        }
        switch (types.size()) {
            case 1:
                this.mTitle = Html.fromHtml(context2.getString(isUpdate ? 17039786 : 17039769, types.valueAt(0), serviceLabel), 0);
                break;
            case 2:
                this.mTitle = Html.fromHtml(context2.getString(isUpdate ? 17039784 : 17039767, types.valueAt(0), types.valueAt(1), serviceLabel), 0);
                break;
            case 3:
                this.mTitle = Html.fromHtml(context2.getString(isUpdate ? 17039785 : 17039768, types.valueAt(0), types.valueAt(1), types.valueAt(2), serviceLabel), 0);
                break;
            default:
                this.mTitle = Html.fromHtml(context2.getString(isUpdate ? 17039783 : 17039766, serviceLabel), 0);
                break;
        }
        titleView.setText(this.mTitle);
        setServiceIcon(context2, view, serviceIcon);
        boolean hasCustomDescription = applyCustomDescription(context2, view, valueFinder, info);
        if (hasCustomDescription) {
            this.mSubTitle = null;
            if (Helper.sDebug) {
                Slog.d(TAG, "on constructor: applied custom description");
            }
        } else {
            CharSequence description = info.getDescription();
            this.mSubTitle = description;
            if (description != null) {
                writeLog(1131);
                ViewGroup subtitleContainer = (ViewGroup) view.findViewById(16908809);
                TextView subtitleView = new TextView(context2);
                subtitleView.setText(description);
                applyMovementMethodIfNeed(subtitleView);
                subtitleContainer.addView(subtitleView, new ViewGroup.LayoutParams(-1, -2));
                subtitleContainer.setVisibility(0);
                subtitleContainer.setScrollBarDefaultDelayBeforeFade(500);
            }
            if (Helper.sDebug) {
                Slog.d(TAG, "on constructor: title=" + ((Object) this.mTitle) + ", subTitle=" + ((Object) description));
            }
        }
        TextView noButton = (TextView) view.findViewById(16908811);
        int negativeActionStyle = info.getNegativeActionStyle();
        switch (negativeActionStyle) {
            case 1:
                noButton.setText(17039765);
                break;
            case 2:
                noButton.setText(17039763);
                break;
            default:
                noButton.setText(17039764);
                break;
        }
        noButton.setOnClickListener(new View.OnClickListener() { // from class: com.android.server.autofill.ui.SaveUi$$ExternalSyntheticLambda0
            @Override // android.view.View.OnClickListener
            public final void onClick(View view2) {
                SaveUi.this.m2146lambda$new$0$comandroidserverautofilluiSaveUi(info, view2);
            }
        });
        TextView yesButton = (TextView) view.findViewById(16908813);
        if (info.getPositiveActionStyle() == 1) {
            yesButton.setText(17039727);
        } else if (isUpdate) {
            yesButton.setText(17039787);
        }
        yesButton.setOnClickListener(new View.OnClickListener() { // from class: com.android.server.autofill.ui.SaveUi$$ExternalSyntheticLambda1
            @Override // android.view.View.OnClickListener
            public final void onClick(View view2) {
                SaveUi.this.m2147lambda$new$1$comandroidserverautofilluiSaveUi(view2);
            }
        });
        Dialog dialog = new Dialog(context2, i);
        this.mDialog = dialog;
        dialog.setContentView(view);
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() { // from class: com.android.server.autofill.ui.SaveUi$$ExternalSyntheticLambda2
            @Override // android.content.DialogInterface.OnDismissListener
            public final void onDismiss(DialogInterface dialogInterface) {
                SaveUi.this.m2148lambda$new$2$comandroidserverautofilluiSaveUi(dialogInterface);
            }
        });
        Window window = dialog.getWindow();
        window.setType(2038);
        window.addFlags(131074);
        window.setDimAmount(0.6f);
        window.addPrivateFlags(16);
        window.setSoftInputMode(32);
        window.setGravity(81);
        window.setCloseOnTouchOutside(true);
        WindowManager.LayoutParams params = window.getAttributes();
        params.width = -1;
        params.accessibilityTitle = context2.getString(17039762);
        params.windowAnimations = 16974615;
        show();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$new$0$com-android-server-autofill-ui-SaveUi  reason: not valid java name */
    public /* synthetic */ void m2146lambda$new$0$comandroidserverautofilluiSaveUi(SaveInfo info, View v) {
        this.mListener.onCancel(info.getNegativeActionListener());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$new$1$com-android-server-autofill-ui-SaveUi  reason: not valid java name */
    public /* synthetic */ void m2147lambda$new$1$comandroidserverautofilluiSaveUi(View v) {
        this.mListener.onSave();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$new$2$com-android-server-autofill-ui-SaveUi  reason: not valid java name */
    public /* synthetic */ void m2148lambda$new$2$comandroidserverautofilluiSaveUi(DialogInterface d) {
        this.mListener.onCancel(null);
    }

    private boolean applyCustomDescription(Context context, View saveUiView, ValueFinder valueFinder, SaveInfo info) {
        View customSubtitleView;
        ArrayList<Pair<Integer, InternalTransformation>> transformations;
        RemoteViews.InteractionHandler handler;
        ArrayList<Pair<InternalValidator, BatchUpdates>> updates;
        CustomDescription customDescription = info.getCustomDescription();
        if (customDescription != null) {
            writeLog(1129);
            RemoteViews template = customDescription.getPresentation();
            if (template == null) {
                Slog.w(TAG, "No remote view on custom description");
                return false;
            }
            ArrayList<Pair<Integer, InternalTransformation>> transformations2 = customDescription.getTransformations();
            if (Helper.sVerbose) {
                Slog.v(TAG, "applyCustomDescription(): transformations = " + transformations2);
            }
            if (transformations2 == null || InternalTransformation.batchApply(valueFinder, template, transformations2)) {
                RemoteViews.InteractionHandler handler2 = new RemoteViews.InteractionHandler() { // from class: com.android.server.autofill.ui.SaveUi$$ExternalSyntheticLambda4
                    public final boolean onInteraction(View view, PendingIntent pendingIntent, RemoteViews.RemoteResponse remoteResponse) {
                        return SaveUi.this.m2145x200aead4(view, pendingIntent, remoteResponse);
                    }
                };
                try {
                    customSubtitleView = template.applyWithTheme(context, null, handler2, this.mThemeId);
                    ArrayList<Pair<InternalValidator, BatchUpdates>> updates2 = customDescription.getUpdates();
                    if (Helper.sVerbose) {
                        try {
                            Slog.v(TAG, "applyCustomDescription(): view = " + customSubtitleView + " updates=" + updates2);
                        } catch (Exception e) {
                            e = e;
                            Slog.e(TAG, "Error applying custom description. ", e);
                            return false;
                        }
                    }
                    if (updates2 != null) {
                        int size = updates2.size();
                        if (Helper.sDebug) {
                            Slog.d(TAG, "custom description has " + size + " batch updates");
                        }
                        int i = 0;
                        while (i < size) {
                            Pair<InternalValidator, BatchUpdates> pair = updates2.get(i);
                            InternalValidator condition = (InternalValidator) pair.first;
                            try {
                                if (condition == null) {
                                    transformations = transformations2;
                                    handler = handler2;
                                    updates = updates2;
                                } else if (condition.isValid(valueFinder)) {
                                    BatchUpdates batchUpdates = (BatchUpdates) pair.second;
                                    RemoteViews templateUpdates = batchUpdates.getUpdates();
                                    transformations = transformations2;
                                    if (templateUpdates == null) {
                                        handler = handler2;
                                        updates = updates2;
                                    } else {
                                        try {
                                            if (Helper.sDebug) {
                                                handler = handler2;
                                                updates = updates2;
                                                Slog.d(TAG, "Applying template updates for batch update #" + i);
                                            } else {
                                                handler = handler2;
                                                updates = updates2;
                                            }
                                            templateUpdates.reapply(context, customSubtitleView);
                                        } catch (Exception e2) {
                                            e = e2;
                                            Slog.e(TAG, "Error applying custom description. ", e);
                                            return false;
                                        }
                                    }
                                    ArrayList<Pair<Integer, InternalTransformation>> batchTransformations = batchUpdates.getTransformations();
                                    if (batchTransformations != null) {
                                        if (Helper.sDebug) {
                                            Slog.d(TAG, "Applying child transformation for batch update #" + i + ": " + batchTransformations);
                                        }
                                        if (!InternalTransformation.batchApply(valueFinder, template, batchTransformations)) {
                                            Slog.w(TAG, "Could not apply child transformation for batch update #" + i + ": " + batchTransformations);
                                            return false;
                                        }
                                        template.reapply(context, customSubtitleView);
                                    }
                                    i++;
                                    transformations2 = transformations;
                                    handler2 = handler;
                                    updates2 = updates;
                                } else {
                                    transformations = transformations2;
                                    handler = handler2;
                                    updates = updates2;
                                }
                                if (Helper.sDebug) {
                                    Slog.d(TAG, "Skipping batch update #" + i);
                                }
                                i++;
                                transformations2 = transformations;
                                handler2 = handler;
                                updates2 = updates;
                            } catch (Exception e3) {
                                e = e3;
                                Slog.e(TAG, "Error applying custom description. ", e);
                                return false;
                            }
                        }
                    }
                    SparseArray<InternalOnClickAction> actions = customDescription.getActions();
                    if (actions != null) {
                        int size2 = actions.size();
                        if (Helper.sDebug) {
                            Slog.d(TAG, "custom description has " + size2 + " actions");
                        }
                        if (!(customSubtitleView instanceof ViewGroup)) {
                            Slog.w(TAG, "cannot apply actions because custom description root is not a ViewGroup: " + customSubtitleView);
                        } else {
                            final ViewGroup rootView = (ViewGroup) customSubtitleView;
                            for (int i2 = 0; i2 < size2; i2++) {
                                int id = actions.keyAt(i2);
                                final InternalOnClickAction action = actions.valueAt(i2);
                                View child = rootView.findViewById(id);
                                if (child == null) {
                                    Slog.w(TAG, "Ignoring action " + action + " for view " + id + " because it's not on " + rootView);
                                } else {
                                    child.setOnClickListener(new View.OnClickListener() { // from class: com.android.server.autofill.ui.SaveUi$$ExternalSyntheticLambda5
                                        @Override // android.view.View.OnClickListener
                                        public final void onClick(View view) {
                                            SaveUi.lambda$applyCustomDescription$4(action, rootView, view);
                                        }
                                    });
                                }
                            }
                        }
                    }
                    applyTextViewStyle(customSubtitleView);
                } catch (Exception e4) {
                    e = e4;
                }
                try {
                    ViewGroup subtitleContainer = (ViewGroup) saveUiView.findViewById(16908809);
                    subtitleContainer.addView(customSubtitleView);
                    subtitleContainer.setVisibility(0);
                    subtitleContainer.setScrollBarDefaultDelayBeforeFade(500);
                    return true;
                } catch (Exception e5) {
                    e = e5;
                    Slog.e(TAG, "Error applying custom description. ", e);
                    return false;
                }
            }
            Slog.w(TAG, "could not apply main transformations on custom description");
            return false;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$applyCustomDescription$3$com-android-server-autofill-ui-SaveUi  reason: not valid java name */
    public /* synthetic */ boolean m2145x200aead4(View view, PendingIntent pendingIntent, RemoteViews.RemoteResponse response) {
        Intent intent = (Intent) response.getLaunchOptions(view).first;
        boolean isValid = isValidLink(pendingIntent, intent);
        if (!isValid) {
            LogMaker log = newLogMaker(1132, this.mType);
            log.setType(0);
            this.mMetricsLogger.write(log);
            return false;
        }
        startIntentSenderWithRestore(pendingIntent, intent);
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$applyCustomDescription$4(InternalOnClickAction action, ViewGroup rootView, View v) {
        if (Helper.sVerbose) {
            Slog.v(TAG, "Applying " + action + " after " + v + " was clicked");
        }
        action.onClick(rootView);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void startIntentSenderWithRestore(PendingIntent pendingIntent, Intent intent) {
        if (Helper.sVerbose) {
            Slog.v(TAG, "Intercepting custom description intent");
        }
        IBinder token = this.mPendingUi.getToken();
        intent.putExtra("android.view.autofill.extra.RESTORE_SESSION_TOKEN", token);
        this.mListener.startIntentSender(pendingIntent.getIntentSender(), intent);
        this.mPendingUi.setState(2);
        if (Helper.sDebug) {
            Slog.d(TAG, "hiding UI until restored with token " + token);
        }
        hide();
        LogMaker log = newLogMaker(1132, this.mType);
        log.setType(1);
        this.mMetricsLogger.write(log);
    }

    private void applyTextViewStyle(View rootView) {
        final List<TextView> textViews = new ArrayList<>();
        Predicate<View> predicate = new Predicate() { // from class: com.android.server.autofill.ui.SaveUi$$ExternalSyntheticLambda3
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return SaveUi.lambda$applyTextViewStyle$5(textViews, (View) obj);
            }
        };
        rootView.findViewByPredicate(predicate);
        int size = textViews.size();
        for (int i = 0; i < size; i++) {
            applyMovementMethodIfNeed(textViews.get(i));
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$applyTextViewStyle$5(List textViews, View view) {
        if (view instanceof TextView) {
            textViews.add((TextView) view);
            return false;
        }
        return false;
    }

    private void applyMovementMethodIfNeed(TextView textView) {
        CharSequence message = textView.getText();
        if (TextUtils.isEmpty(message)) {
            return;
        }
        SpannableStringBuilder ssb = new SpannableStringBuilder(message);
        ClickableSpan[] spans = (ClickableSpan[]) ssb.getSpans(0, ssb.length(), ClickableSpan.class);
        if (ArrayUtils.isEmpty(spans)) {
            return;
        }
        textView.setMovementMethod(LinkMovementMethod.getInstance());
    }

    private void setServiceIcon(Context context, View view, Drawable serviceIcon) {
        ImageView iconView = (ImageView) view.findViewById(16908810);
        Resources res = context.getResources();
        int maxWidth = res.getDimensionPixelSize(17104958);
        int actualWidth = serviceIcon.getMinimumWidth();
        int actualHeight = serviceIcon.getMinimumHeight();
        if (actualWidth > maxWidth || actualHeight > maxWidth) {
            Slog.w(TAG, "Not adding service icon of size (" + actualWidth + "x" + actualHeight + ") because maximum is (" + maxWidth + "x" + maxWidth + ").");
            ((ViewGroup) iconView.getParent()).removeView(iconView);
            return;
        }
        if (Helper.sDebug) {
            Slog.d(TAG, "Adding service icon (" + actualWidth + "x" + actualHeight + ") as it's less than maximum (" + maxWidth + "x" + maxWidth + ").");
        }
        iconView.setImageDrawable(serviceIcon);
    }

    private static boolean isValidLink(PendingIntent pendingIntent, Intent intent) {
        if (pendingIntent == null) {
            Slog.w(TAG, "isValidLink(): custom description without pending intent");
            return false;
        } else if (!pendingIntent.isActivity()) {
            Slog.w(TAG, "isValidLink(): pending intent not for activity");
            return false;
        } else if (intent == null) {
            Slog.w(TAG, "isValidLink(): no intent");
            return false;
        } else {
            return true;
        }
    }

    private LogMaker newLogMaker(int category, int saveType) {
        return newLogMaker(category).addTaggedData(1130, Integer.valueOf(saveType));
    }

    private LogMaker newLogMaker(int category) {
        return Helper.newLogMaker(category, this.mComponentName, this.mServicePackageName, this.mPendingUi.sessionId, this.mCompatMode);
    }

    private void writeLog(int category) {
        this.mMetricsLogger.write(newLogMaker(category, this.mType));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onPendingUi(int operation, IBinder token) {
        if (!this.mPendingUi.matches(token)) {
            Slog.w(TAG, "restore(" + operation + "): got token " + token + " instead of " + this.mPendingUi.getToken());
            return;
        }
        LogMaker log = newLogMaker(1134);
        try {
            switch (operation) {
                case 1:
                    log.setType(5);
                    if (Helper.sDebug) {
                        Slog.d(TAG, "Cancelling pending save dialog for " + token);
                    }
                    hide();
                    break;
                case 2:
                    if (Helper.sDebug) {
                        Slog.d(TAG, "Restoring save dialog for " + token);
                    }
                    log.setType(1);
                    show();
                    break;
                default:
                    log.setType(11);
                    Slog.w(TAG, "restore(): invalid operation " + operation);
                    break;
            }
            this.mMetricsLogger.write(log);
            this.mPendingUi.setState(4);
        } catch (Throwable th) {
            this.mMetricsLogger.write(log);
            throw th;
        }
    }

    private void show() {
        Slog.i(TAG, "Showing save dialog: " + ((Object) this.mTitle));
        this.mDialog.show();
        this.mOverlayControl.hideOverlays();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public PendingUi hide() {
        if (Helper.sVerbose) {
            Slog.v(TAG, "Hiding save dialog.");
        }
        try {
            this.mDialog.hide();
            this.mOverlayControl.showOverlays();
            return this.mPendingUi;
        } catch (Throwable th) {
            this.mOverlayControl.showOverlays();
            throw th;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isShowing() {
        return this.mDialog.isShowing();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void destroy() {
        try {
            if (Helper.sDebug) {
                Slog.d(TAG, "destroy()");
            }
            throwIfDestroyed();
            this.mListener.onDestroy();
            this.mHandler.removeCallbacksAndMessages(this.mListener);
            this.mDialog.dismiss();
            this.mDestroyed = true;
        } finally {
            this.mOverlayControl.showOverlays();
        }
    }

    private void throwIfDestroyed() {
        if (this.mDestroyed) {
            throw new IllegalStateException("cannot interact with a destroyed instance");
        }
    }

    public String toString() {
        CharSequence charSequence = this.mTitle;
        return charSequence == null ? "NO TITLE" : charSequence.toString();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dump(PrintWriter pw, String prefix) {
        pw.print(prefix);
        pw.print("title: ");
        pw.println(this.mTitle);
        pw.print(prefix);
        pw.print("subtitle: ");
        pw.println(this.mSubTitle);
        pw.print(prefix);
        pw.print("pendingUi: ");
        pw.println(this.mPendingUi);
        pw.print(prefix);
        pw.print("service: ");
        pw.println(this.mServicePackageName);
        pw.print(prefix);
        pw.print("app: ");
        pw.println(this.mComponentName.toShortString());
        pw.print(prefix);
        pw.print("compat mode: ");
        pw.println(this.mCompatMode);
        pw.print(prefix);
        pw.print("theme id: ");
        pw.print(this.mThemeId);
        switch (this.mThemeId) {
            case THEME_ID_DARK /* 16974827 */:
                pw.println(" (dark)");
                break;
            case THEME_ID_LIGHT /* 16974838 */:
                pw.println(" (light)");
                break;
            default:
                pw.println("(UNKNOWN_MODE)");
                break;
        }
        View view = this.mDialog.getWindow().getDecorView();
        int[] loc = view.getLocationOnScreen();
        pw.print(prefix);
        pw.print("coordinates: ");
        pw.print('(');
        pw.print(loc[0]);
        pw.print(',');
        pw.print(loc[1]);
        pw.print(')');
        pw.print('(');
        pw.print(loc[0] + view.getWidth());
        pw.print(',');
        pw.print(loc[1] + view.getHeight());
        pw.println(')');
        pw.print(prefix);
        pw.print("destroyed: ");
        pw.println(this.mDestroyed);
    }
}
