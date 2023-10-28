package com.android.server.inputmethod;

import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Slog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodSubtype;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.Switch;
import android.widget.TextView;
import com.android.internal.R;
import com.android.server.LocalServices;
import com.android.server.inputmethod.InputMethodSubtypeSwitchingController;
import com.android.server.inputmethod.InputMethodUtils;
import com.android.server.wm.WindowManagerInternal;
import java.util.List;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public final class InputMethodMenuController {
    private static final String TAG = InputMethodMenuController.class.getSimpleName();
    private static final boolean TRAN_SECURITY_INPUT_SUPPORT;
    private AlertDialog.Builder mDialogBuilder;
    private InputMethodDialogWindowContext mDialogWindowContext;
    private InputMethodInfo[] mIms;
    private final KeyguardManager mKeyguardManager;
    private final ArrayMap<String, InputMethodInfo> mMethodMap;
    private final InputMethodManagerService mService;
    private final InputMethodUtils.InputMethodSettings mSettings;
    private boolean mShowImeWithHardKeyboard;
    private int[] mSubtypeIds;
    private final InputMethodSubtypeSwitchingController mSwitchingController;
    private AlertDialog mSwitchingDialog;
    private View mSwitchingDialogTitleView;
    private final WindowManagerInternal mWindowManagerInternal = (WindowManagerInternal) LocalServices.getService(WindowManagerInternal.class);

    static {
        TRAN_SECURITY_INPUT_SUPPORT = 1 == SystemProperties.getInt("ro.tran_security_input_support", 0);
    }

    public InputMethodMenuController(InputMethodManagerService service) {
        this.mService = service;
        this.mSettings = service.mSettings;
        this.mSwitchingController = service.mSwitchingController;
        this.mMethodMap = service.mMethodMap;
        this.mKeyguardManager = service.mKeyguardManager;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void showInputMethodMenu(boolean showAuxSubtypes, int displayId) {
        int subtypeId;
        InputMethodSubtype currentSubtype;
        if (InputMethodManagerService.DEBUG) {
            Slog.v(TAG, "Show switching menu. showAuxSubtypes=" + showAuxSubtypes);
        }
        boolean isScreenLocked = isScreenLocked();
        String lastInputMethodId = this.mSettings.getSelectedInputMethod();
        int lastInputMethodSubtypeId = this.mSettings.getSelectedInputMethodSubtypeId(lastInputMethodId);
        if (InputMethodManagerService.DEBUG) {
            Slog.v(TAG, "Current IME: " + lastInputMethodId);
        }
        synchronized (ImfLock.class) {
            try {
                try {
                    List<InputMethodSubtypeSwitchingController.ImeSubtypeListItem> imList = this.mSwitchingController.getSortedInputMethodAndSubtypeListForImeMenuLocked(showAuxSubtypes, isScreenLocked);
                    if (imList.isEmpty()) {
                        return;
                    }
                    if (TRAN_SECURITY_INPUT_SUPPORT) {
                        int i = 0;
                        while (true) {
                            if (i >= imList.size()) {
                                break;
                            }
                            InputMethodSubtypeSwitchingController.ImeSubtypeListItem tmpItem = imList.get(i);
                            if (tmpItem.mImi == null || !"com.transsion.sk/.inputservice.TInputMethodService".equals(tmpItem.mImi.getId())) {
                                i++;
                            } else {
                                imList.remove(tmpItem);
                                break;
                            }
                        }
                    }
                    hideInputMethodMenuLocked();
                    if (lastInputMethodSubtypeId == -1 && (currentSubtype = this.mService.getCurrentInputMethodSubtypeLocked()) != null) {
                        String curMethodId = this.mService.getSelectedMethodIdLocked();
                        InputMethodInfo currentImi = this.mMethodMap.get(curMethodId);
                        lastInputMethodSubtypeId = InputMethodUtils.getSubtypeIdFromHashCode(currentImi, currentSubtype.hashCode());
                    }
                    int size = imList.size();
                    this.mIms = new InputMethodInfo[size];
                    this.mSubtypeIds = new int[size];
                    int checkedItem = 0;
                    for (int i2 = 0; i2 < size; i2++) {
                        InputMethodSubtypeSwitchingController.ImeSubtypeListItem item = imList.get(i2);
                        this.mIms[i2] = item.mImi;
                        this.mSubtypeIds[i2] = item.mSubtypeId;
                        if (this.mIms[i2].getId().equals(lastInputMethodId) && ((subtypeId = this.mSubtypeIds[i2]) == -1 || ((lastInputMethodSubtypeId == -1 && subtypeId == 0) || subtypeId == lastInputMethodSubtypeId))) {
                            checkedItem = i2;
                        }
                    }
                    if (this.mDialogWindowContext == null) {
                        this.mDialogWindowContext = new InputMethodDialogWindowContext();
                    }
                    Context dialogWindowContext = this.mDialogWindowContext.get(displayId);
                    AlertDialog.Builder builder = new AlertDialog.Builder(dialogWindowContext);
                    this.mDialogBuilder = builder;
                    builder.setOnCancelListener(new DialogInterface.OnCancelListener() { // from class: com.android.server.inputmethod.InputMethodMenuController$$ExternalSyntheticLambda0
                        @Override // android.content.DialogInterface.OnCancelListener
                        public final void onCancel(DialogInterface dialogInterface) {
                            InputMethodMenuController.this.m4029x849a58c(dialogInterface);
                        }
                    });
                    Context dialogContext = this.mDialogBuilder.getContext();
                    TypedArray a = dialogContext.obtainStyledAttributes(null, R.styleable.DialogPreference, 16842845, 0);
                    Drawable dialogIcon = a.getDrawable(2);
                    a.recycle();
                    this.mDialogBuilder.setIcon(dialogIcon);
                    LayoutInflater inflater = (LayoutInflater) dialogContext.getSystemService(LayoutInflater.class);
                    View tv = inflater.inflate(17367187, (ViewGroup) null);
                    this.mDialogBuilder.setCustomTitle(tv);
                    this.mSwitchingDialogTitleView = tv;
                    tv.findViewById(16909068).setVisibility(this.mWindowManagerInternal.isHardKeyboardAvailable() ? 0 : 8);
                    Switch hardKeySwitch = (Switch) this.mSwitchingDialogTitleView.findViewById(16909069);
                    hardKeySwitch.setChecked(this.mShowImeWithHardKeyboard);
                    hardKeySwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() { // from class: com.android.server.inputmethod.InputMethodMenuController$$ExternalSyntheticLambda1
                        @Override // android.widget.CompoundButton.OnCheckedChangeListener
                        public final void onCheckedChanged(CompoundButton compoundButton, boolean z) {
                            InputMethodMenuController.this.m4030x9536bcab(compoundButton, z);
                        }
                    });
                    final ImeSubtypeListAdapter adapter = new ImeSubtypeListAdapter(dialogContext, 17367188, imList, checkedItem);
                    DialogInterface.OnClickListener choiceListener = new DialogInterface.OnClickListener() { // from class: com.android.server.inputmethod.InputMethodMenuController$$ExternalSyntheticLambda2
                        @Override // android.content.DialogInterface.OnClickListener
                        public final void onClick(DialogInterface dialogInterface, int i3) {
                            InputMethodMenuController.this.m4031x2223d3ca(adapter, dialogInterface, i3);
                        }
                    };
                    this.mDialogBuilder.setSingleChoiceItems(adapter, checkedItem, choiceListener);
                    AlertDialog create = this.mDialogBuilder.create();
                    this.mSwitchingDialog = create;
                    create.setCanceledOnTouchOutside(true);
                    Window w = this.mSwitchingDialog.getWindow();
                    WindowManager.LayoutParams attrs = w.getAttributes();
                    w.setType(2012);
                    w.setHideOverlayWindows(true);
                    attrs.token = dialogWindowContext.getWindowContextToken();
                    attrs.privateFlags |= 16;
                    attrs.setTitle("Select input method");
                    w.setAttributes(attrs);
                    this.mService.updateSystemUiLocked();
                    this.mService.sendOnNavButtonFlagsChangedLocked();
                    this.mSwitchingDialog.show();
                } catch (Throwable th) {
                    th = th;
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$showInputMethodMenu$0$com-android-server-inputmethod-InputMethodMenuController  reason: not valid java name */
    public /* synthetic */ void m4029x849a58c(DialogInterface dialog) {
        hideInputMethodMenu();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$showInputMethodMenu$1$com-android-server-inputmethod-InputMethodMenuController  reason: not valid java name */
    public /* synthetic */ void m4030x9536bcab(CompoundButton buttonView, boolean isChecked) {
        this.mSettings.setShowImeWithHardKeyboard(isChecked);
        hideInputMethodMenu();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$showInputMethodMenu$2$com-android-server-inputmethod-InputMethodMenuController  reason: not valid java name */
    public /* synthetic */ void m4031x2223d3ca(ImeSubtypeListAdapter adapter, DialogInterface dialog, int which) {
        int[] iArr;
        synchronized (ImfLock.class) {
            InputMethodInfo[] inputMethodInfoArr = this.mIms;
            if (inputMethodInfoArr != null && inputMethodInfoArr.length > which && (iArr = this.mSubtypeIds) != null && iArr.length > which) {
                InputMethodInfo im = inputMethodInfoArr[which];
                int subtypeId = iArr[which];
                adapter.mCheckedItem = which;
                adapter.notifyDataSetChanged();
                hideInputMethodMenu();
                if (im != null) {
                    subtypeId = (subtypeId < 0 || subtypeId >= im.getSubtypeCount()) ? -1 : -1;
                    this.mService.setInputMethodLocked(im.getId(), subtypeId);
                }
            }
        }
    }

    private boolean isScreenLocked() {
        KeyguardManager keyguardManager = this.mKeyguardManager;
        return keyguardManager != null && keyguardManager.isKeyguardLocked() && this.mKeyguardManager.isKeyguardSecure();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateKeyboardFromSettingsLocked() {
        this.mShowImeWithHardKeyboard = this.mSettings.isShowImeWithHardKeyboardEnabled();
        AlertDialog alertDialog = this.mSwitchingDialog;
        if (alertDialog != null && this.mSwitchingDialogTitleView != null && alertDialog.isShowing()) {
            Switch hardKeySwitch = (Switch) this.mSwitchingDialogTitleView.findViewById(16909069);
            hardKeySwitch.setChecked(this.mShowImeWithHardKeyboard);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void hideInputMethodMenu() {
        synchronized (ImfLock.class) {
            hideInputMethodMenuLocked();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void hideInputMethodMenuLocked() {
        if (InputMethodManagerService.DEBUG) {
            Slog.v(TAG, "Hide switching menu");
        }
        AlertDialog alertDialog = this.mSwitchingDialog;
        if (alertDialog != null) {
            alertDialog.dismiss();
            this.mSwitchingDialog = null;
            this.mSwitchingDialogTitleView = null;
            this.mService.updateSystemUiLocked();
            this.mService.sendOnNavButtonFlagsChangedLocked();
            this.mDialogBuilder = null;
            this.mIms = null;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public AlertDialog getSwitchingDialogLocked() {
        return this.mSwitchingDialog;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean getShowImeWithHardKeyboard() {
        return this.mShowImeWithHardKeyboard;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isisInputMethodPickerShownForTestLocked() {
        AlertDialog alertDialog = this.mSwitchingDialog;
        if (alertDialog == null) {
            return false;
        }
        return alertDialog.isShowing();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void handleHardKeyboardStatusChange(boolean available) {
        if (InputMethodManagerService.DEBUG) {
            Slog.w(TAG, "HardKeyboardStatusChanged: available=" + available);
        }
        synchronized (ImfLock.class) {
            AlertDialog alertDialog = this.mSwitchingDialog;
            if (alertDialog != null && this.mSwitchingDialogTitleView != null && alertDialog.isShowing()) {
                this.mSwitchingDialogTitleView.findViewById(16909068).setVisibility(available ? 0 : 8);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class ImeSubtypeListAdapter extends ArrayAdapter<InputMethodSubtypeSwitchingController.ImeSubtypeListItem> {
        public int mCheckedItem;
        private final LayoutInflater mInflater;
        private final List<InputMethodSubtypeSwitchingController.ImeSubtypeListItem> mItemsList;
        private final int mTextViewResourceId;

        private ImeSubtypeListAdapter(Context context, int textViewResourceId, List<InputMethodSubtypeSwitchingController.ImeSubtypeListItem> itemsList, int checkedItem) {
            super(context, textViewResourceId, itemsList);
            this.mTextViewResourceId = textViewResourceId;
            this.mItemsList = itemsList;
            this.mCheckedItem = checkedItem;
            this.mInflater = LayoutInflater.from(context);
        }

        @Override // android.widget.ArrayAdapter, android.widget.Adapter
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView != null ? convertView : this.mInflater.inflate(this.mTextViewResourceId, (ViewGroup) null);
            if (position < 0 || position >= this.mItemsList.size()) {
                return view;
            }
            InputMethodSubtypeSwitchingController.ImeSubtypeListItem item = this.mItemsList.get(position);
            CharSequence imeName = item.mImeName;
            CharSequence subtypeName = item.mSubtypeName;
            TextView firstTextView = (TextView) view.findViewById(16908308);
            TextView secondTextView = (TextView) view.findViewById(16908309);
            if (TextUtils.isEmpty(subtypeName)) {
                firstTextView.setText(imeName);
                secondTextView.setVisibility(8);
            } else {
                firstTextView.setText(subtypeName);
                secondTextView.setText(imeName);
                secondTextView.setVisibility(0);
            }
            RadioButton radioButton = (RadioButton) view.findViewById(16909390);
            radioButton.setChecked(position == this.mCheckedItem);
            return view;
        }
    }
}
