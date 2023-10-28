package android.inputmethodservice;

import android.Manifest;
import android.content.Context;
import android.os.Binder;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.util.Log;
import android.view.InputChannel;
import android.view.MotionEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputBinding;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethod;
import android.view.inputmethod.InputMethodSession;
import android.view.inputmethod.InputMethodSubtype;
import android.window.ImeOnBackInvokedDispatcher;
import com.android.internal.inputmethod.CancellationGroup;
import com.android.internal.inputmethod.IInputMethodPrivilegedOperations;
import com.android.internal.os.HandlerCaller;
import com.android.internal.os.SomeArgs;
import com.android.internal.view.IInlineSuggestionsRequestCallback;
import com.android.internal.view.IInputContext;
import com.android.internal.view.IInputMethod;
import com.android.internal.view.IInputMethodSession;
import com.android.internal.view.IInputSessionCallback;
import com.android.internal.view.InlineSuggestionsRequestInfo;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
/* loaded from: classes2.dex */
class IInputMethodWrapper extends IInputMethod.Stub implements HandlerCaller.Callback {
    private static final int DO_CAN_START_STYLUS_HANDWRITING = 100;
    private static final int DO_CHANGE_INPUTMETHOD_SUBTYPE = 80;
    private static final int DO_CREATE_INLINE_SUGGESTIONS_REQUEST = 90;
    private static final int DO_CREATE_SESSION = 40;
    private static final int DO_DUMP = 1;
    private static final int DO_FINISH_STYLUS_HANDWRITING = 130;
    private static final int DO_HIDE_SOFT_INPUT = 70;
    private static final int DO_INITIALIZE_INTERNAL = 10;
    private static final int DO_INIT_INK_WINDOW = 120;
    private static final int DO_ON_NAV_BUTTON_FLAGS_CHANGED = 35;
    private static final int DO_SET_INPUT_CONTEXT = 20;
    private static final int DO_SET_SESSION_ENABLED = 45;
    private static final int DO_SHOW_SOFT_INPUT = 60;
    private static final int DO_START_INPUT = 32;
    private static final int DO_START_STYLUS_HANDWRITING = 110;
    private static final int DO_UNSET_INPUT_CONTEXT = 30;
    private static final String TAG = "InputMethodWrapper";
    final HandlerCaller mCaller;
    CancellationGroup mCancellationGroup = null;
    final Context mContext;
    final WeakReference<InputMethod> mInputMethod;
    final WeakReference<InputMethodServiceInternal> mTarget;
    final int mTargetSdkVersion;

    /* loaded from: classes2.dex */
    static final class InputMethodSessionCallbackWrapper implements InputMethod.SessionCallback {
        final IInputSessionCallback mCb;
        final InputChannel mChannel;
        final Context mContext;

        InputMethodSessionCallbackWrapper(Context context, InputChannel channel, IInputSessionCallback cb) {
            this.mContext = context;
            this.mChannel = channel;
            this.mCb = cb;
        }

        @Override // android.view.inputmethod.InputMethod.SessionCallback
        public void sessionCreated(InputMethodSession session) {
            try {
                if (session != null) {
                    IInputMethodSessionWrapper wrap = new IInputMethodSessionWrapper(this.mContext, session, this.mChannel);
                    this.mCb.sessionCreated(wrap);
                    return;
                }
                InputChannel inputChannel = this.mChannel;
                if (inputChannel != null) {
                    inputChannel.dispose();
                }
                this.mCb.sessionCreated(null);
            } catch (RemoteException e) {
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public IInputMethodWrapper(InputMethodServiceInternal imsInternal, InputMethod inputMethod) {
        this.mTarget = new WeakReference<>(imsInternal);
        Context applicationContext = imsInternal.getContext().getApplicationContext();
        this.mContext = applicationContext;
        this.mCaller = new HandlerCaller(applicationContext, null, this, true);
        this.mInputMethod = new WeakReference<>(inputMethod);
        this.mTargetSdkVersion = imsInternal.getContext().getApplicationInfo().targetSdkVersion;
    }

    @Override // com.android.internal.os.HandlerCaller.Callback
    public void executeMessage(Message msg) {
        SomeArgs args;
        InputConnection ic;
        InputMethod inputMethod = this.mInputMethod.get();
        if (inputMethod == null && msg.what != 1) {
            Log.w(TAG, "Input method reference was null, ignoring message: " + msg.what);
            return;
        }
        switch (msg.what) {
            case 1:
                InputMethodServiceInternal target = this.mTarget.get();
                if (target == null) {
                    return;
                }
                args = (SomeArgs) msg.obj;
                try {
                    target.dump((FileDescriptor) args.arg1, (PrintWriter) args.arg2, (String[]) args.arg3);
                } catch (RuntimeException e) {
                    ((PrintWriter) args.arg2).println("Exception: " + e);
                }
                synchronized (args.arg4) {
                    ((CountDownLatch) args.arg4).countDown();
                }
                return;
            case 10:
                args = (SomeArgs) msg.obj;
                try {
                    inputMethod.initializeInternal((IBinder) args.arg1, (IInputMethodPrivilegedOperations) args.arg2, msg.arg1, ((Boolean) args.arg3).booleanValue(), msg.arg2);
                    return;
                } finally {
                    args.recycle();
                }
            case 20:
                inputMethod.bindInput((InputBinding) msg.obj);
                return;
            case 30:
                inputMethod.unbindInput();
                return;
            case 32:
                args = (SomeArgs) msg.obj;
                IBinder startInputToken = (IBinder) args.arg1;
                IInputContext inputContext = (IInputContext) ((SomeArgs) args.arg2).arg1;
                ImeOnBackInvokedDispatcher imeDispatcher = (ImeOnBackInvokedDispatcher) ((SomeArgs) args.arg2).arg2;
                EditorInfo info = (EditorInfo) args.arg3;
                CancellationGroup cancellationGroup = (CancellationGroup) args.arg4;
                boolean restarting = args.argi5 == 1;
                int navButtonFlags = args.argi6;
                if (inputContext != null) {
                    ic = new RemoteInputConnection(this.mTarget, inputContext, cancellationGroup);
                } else {
                    ic = null;
                }
                info.makeCompatible(this.mTargetSdkVersion);
                inputMethod.dispatchStartInputWithToken(ic, info, restarting, startInputToken, navButtonFlags, imeDispatcher);
                return;
            case 35:
                inputMethod.onNavButtonFlagsChanged(msg.arg1);
                return;
            case 40:
                args = (SomeArgs) msg.obj;
                inputMethod.createSession(new InputMethodSessionCallbackWrapper(this.mContext, (InputChannel) args.arg1, (IInputSessionCallback) args.arg2));
                return;
            case 45:
                inputMethod.setSessionEnabled((InputMethodSession) msg.obj, msg.arg1 != 0);
                return;
            case 60:
                args = (SomeArgs) msg.obj;
                inputMethod.showSoftInputWithToken(msg.arg1, (ResultReceiver) args.arg2, (IBinder) args.arg1);
                return;
            case 70:
                args = (SomeArgs) msg.obj;
                inputMethod.hideSoftInputWithToken(msg.arg1, (ResultReceiver) args.arg2, (IBinder) args.arg1);
                return;
            case 80:
                inputMethod.changeInputMethodSubtype((InputMethodSubtype) msg.obj);
                return;
            case 90:
                args = (SomeArgs) msg.obj;
                inputMethod.onCreateInlineSuggestionsRequest((InlineSuggestionsRequestInfo) args.arg1, (IInlineSuggestionsRequestCallback) args.arg2);
                return;
            case 100:
                inputMethod.canStartStylusHandwriting(msg.arg1);
                return;
            case 110:
                args = (SomeArgs) msg.obj;
                inputMethod.startStylusHandwriting(msg.arg1, (InputChannel) args.arg1, (List) args.arg2);
                return;
            case 120:
                inputMethod.initInkWindow();
                return;
            case 130:
                inputMethod.finishStylusHandwriting();
                return;
            default:
                Log.w(TAG, "Unhandled message code: " + msg.what);
                return;
        }
    }

    @Override // android.os.Binder
    protected void dump(FileDescriptor fd, PrintWriter fout, String[] args) {
        InputMethodServiceInternal target = this.mTarget.get();
        if (target == null) {
            return;
        }
        if (target.getContext().checkCallingOrSelfPermission(Manifest.permission.DUMP) != 0) {
            fout.println("Permission Denial: can't dump InputMethodManager from from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid());
            return;
        }
        CountDownLatch latch = new CountDownLatch(1);
        this.mCaller.getHandler().sendMessageAtFrontOfQueue(this.mCaller.obtainMessageOOOO(1, fd, fout, args, latch));
        try {
            if (!latch.await(5L, TimeUnit.SECONDS)) {
                fout.println("Timeout waiting for dump");
            }
        } catch (InterruptedException e) {
            fout.println("Interrupted waiting for dump");
        }
    }

    @Override // com.android.internal.view.IInputMethod
    public void initializeInternal(IBinder token, IInputMethodPrivilegedOperations privOps, int configChanges, boolean stylusHwSupported, int navButtonFlags) {
        HandlerCaller handlerCaller = this.mCaller;
        handlerCaller.executeOrSendMessage(handlerCaller.obtainMessageIIOOO(10, configChanges, navButtonFlags, token, privOps, Boolean.valueOf(stylusHwSupported)));
    }

    @Override // com.android.internal.view.IInputMethod
    public void onCreateInlineSuggestionsRequest(InlineSuggestionsRequestInfo requestInfo, IInlineSuggestionsRequestCallback cb) {
        HandlerCaller handlerCaller = this.mCaller;
        handlerCaller.executeOrSendMessage(handlerCaller.obtainMessageOO(90, requestInfo, cb));
    }

    @Override // com.android.internal.view.IInputMethod
    public void bindInput(InputBinding binding) {
        if (this.mCancellationGroup != null) {
            Log.e(TAG, "bindInput must be paired with unbindInput.");
        }
        this.mCancellationGroup = new CancellationGroup();
        InputConnection ic = new RemoteInputConnection(this.mTarget, IInputContext.Stub.asInterface(binding.getConnectionToken()), this.mCancellationGroup);
        InputBinding nu = new InputBinding(ic, binding);
        HandlerCaller handlerCaller = this.mCaller;
        handlerCaller.executeOrSendMessage(handlerCaller.obtainMessageO(20, nu));
    }

    @Override // com.android.internal.view.IInputMethod
    public void unbindInput() {
        CancellationGroup cancellationGroup = this.mCancellationGroup;
        if (cancellationGroup != null) {
            cancellationGroup.cancelAll();
            this.mCancellationGroup = null;
        } else {
            Log.e(TAG, "unbindInput must be paired with bindInput.");
        }
        HandlerCaller handlerCaller = this.mCaller;
        handlerCaller.executeOrSendMessage(handlerCaller.obtainMessage(30));
    }

    @Override // com.android.internal.view.IInputMethod
    public void startInput(IBinder startInputToken, IInputContext inputContext, EditorInfo attribute, boolean restarting, int navButtonFlags, ImeOnBackInvokedDispatcher imeDispatcher) {
        if (this.mCancellationGroup == null) {
            Log.e(TAG, "startInput must be called after bindInput.");
            this.mCancellationGroup = new CancellationGroup();
        }
        SomeArgs args = SomeArgs.obtain();
        args.arg1 = inputContext;
        args.arg2 = imeDispatcher;
        HandlerCaller handlerCaller = this.mCaller;
        handlerCaller.executeOrSendMessage(handlerCaller.obtainMessageOOOOII(32, startInputToken, args, attribute, this.mCancellationGroup, restarting ? 1 : 0, navButtonFlags));
    }

    @Override // com.android.internal.view.IInputMethod
    public void onNavButtonFlagsChanged(int navButtonFlags) {
        HandlerCaller handlerCaller = this.mCaller;
        handlerCaller.executeOrSendMessage(handlerCaller.obtainMessageI(35, navButtonFlags));
    }

    @Override // com.android.internal.view.IInputMethod
    public void createSession(InputChannel channel, IInputSessionCallback callback) {
        HandlerCaller handlerCaller = this.mCaller;
        handlerCaller.executeOrSendMessage(handlerCaller.obtainMessageOO(40, channel, callback));
    }

    @Override // com.android.internal.view.IInputMethod
    public void setSessionEnabled(IInputMethodSession session, boolean enabled) {
        try {
            InputMethodSession ls = ((IInputMethodSessionWrapper) session).getInternalInputMethodSession();
            if (ls == null) {
                Log.w(TAG, "Session is already finished: " + session);
                return;
            }
            HandlerCaller handlerCaller = this.mCaller;
            handlerCaller.executeOrSendMessage(handlerCaller.obtainMessageIO(45, enabled ? 1 : 0, ls));
        } catch (ClassCastException e) {
            Log.w(TAG, "Incoming session not of correct type: " + session, e);
        }
    }

    @Override // com.android.internal.view.IInputMethod
    public void showSoftInput(IBinder showInputToken, int flags, ResultReceiver resultReceiver) {
        HandlerCaller handlerCaller = this.mCaller;
        handlerCaller.executeOrSendMessage(handlerCaller.obtainMessageIOO(60, flags, showInputToken, resultReceiver));
    }

    @Override // com.android.internal.view.IInputMethod
    public void hideSoftInput(IBinder hideInputToken, int flags, ResultReceiver resultReceiver) {
        HandlerCaller handlerCaller = this.mCaller;
        handlerCaller.executeOrSendMessage(handlerCaller.obtainMessageIOO(70, flags, hideInputToken, resultReceiver));
    }

    @Override // com.android.internal.view.IInputMethod
    public void changeInputMethodSubtype(InputMethodSubtype subtype) {
        HandlerCaller handlerCaller = this.mCaller;
        handlerCaller.executeOrSendMessage(handlerCaller.obtainMessageO(80, subtype));
    }

    @Override // com.android.internal.view.IInputMethod
    public void canStartStylusHandwriting(int requestId) throws RemoteException {
        HandlerCaller handlerCaller = this.mCaller;
        handlerCaller.executeOrSendMessage(handlerCaller.obtainMessageI(100, requestId));
    }

    @Override // com.android.internal.view.IInputMethod
    public void startStylusHandwriting(int requestId, InputChannel channel, List<MotionEvent> stylusEvents) throws RemoteException {
        HandlerCaller handlerCaller = this.mCaller;
        handlerCaller.executeOrSendMessage(handlerCaller.obtainMessageIOO(110, requestId, channel, stylusEvents));
    }

    @Override // com.android.internal.view.IInputMethod
    public void initInkWindow() {
        HandlerCaller handlerCaller = this.mCaller;
        handlerCaller.executeOrSendMessage(handlerCaller.obtainMessage(120));
    }

    @Override // com.android.internal.view.IInputMethod
    public void finishStylusHandwriting() {
        HandlerCaller handlerCaller = this.mCaller;
        handlerCaller.executeOrSendMessage(handlerCaller.obtainMessage(130));
    }
}
