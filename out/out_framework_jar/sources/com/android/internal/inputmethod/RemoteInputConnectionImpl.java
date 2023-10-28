package com.android.internal.inputmethod;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Trace;
import android.util.Log;
import android.util.proto.ProtoOutputStream;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewRootImpl;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.CorrectionInfo;
import android.view.inputmethod.DumpableInputConnection;
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.ExtractedTextRequest;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputContentInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.SurroundingText;
import android.view.inputmethod.TextAttribute;
import android.view.inputmethod.TextSnapshot;
import com.android.internal.infra.AndroidFuture;
import com.android.internal.inputmethod.IRemoteAccessibilityInputConnection;
import com.android.internal.inputmethod.RemoteInputConnectionImpl;
import com.android.internal.view.IInputContext;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;
/* loaded from: classes4.dex */
public final class RemoteInputConnectionImpl extends IInputContext.Stub {
    private static final boolean DEBUG = false;
    private static final int MAX_END_BATCH_EDIT_RETRY = 16;
    private static final String TAG = "RemoteInputConnectionImpl";
    private final Handler mH;
    private InputConnection mInputConnection;
    private final Looper mLooper;
    private final InputMethodManager mParentInputMethodManager;
    private final WeakReference<View> mServedView;
    private final Object mLock = new Object();
    private boolean mFinished = false;
    private final AtomicInteger mCurrentSessionId = new AtomicInteger(0);
    private final AtomicBoolean mHasPendingInvalidation = new AtomicBoolean();
    private final IRemoteAccessibilityInputConnection mAccessibilityInputConnection = new AnonymousClass1();

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes4.dex */
    private @interface Dispatching {
        boolean cancellable();
    }

    /* renamed from: -$$Nest$smuseImeTracing  reason: not valid java name */
    static /* bridge */ /* synthetic */ boolean m6622$$Nest$smuseImeTracing() {
        return useImeTracing();
    }

    /* loaded from: classes4.dex */
    private static final class KnownAlwaysTrueEndBatchEditCache {
        private static volatile Class<?>[] sArray;
        private static volatile Class<?> sElement;

        private KnownAlwaysTrueEndBatchEditCache() {
        }

        static boolean contains(Class<? extends InputConnection> klass) {
            if (klass == sElement) {
                return true;
            }
            Class<?>[] array = sArray;
            if (array == null) {
                return false;
            }
            for (Class<?> item : array) {
                if (item == klass) {
                    return true;
                }
            }
            return false;
        }

        static void add(Class<? extends InputConnection> klass) {
            if (sElement == null) {
                sElement = klass;
                return;
            }
            Class<?>[] array = sArray;
            int arraySize = array != null ? array.length : 0;
            Class<?>[] newArray = new Class[arraySize + 1];
            for (int i = 0; i < arraySize; i++) {
                newArray[i] = array[i];
            }
            newArray[arraySize] = klass;
            sArray = newArray;
        }
    }

    public RemoteInputConnectionImpl(Looper looper, InputConnection inputConnection, InputMethodManager inputMethodManager, View servedView) {
        this.mInputConnection = inputConnection;
        this.mLooper = looper;
        this.mH = new Handler(looper);
        this.mParentInputMethodManager = inputMethodManager;
        this.mServedView = new WeakReference<>(servedView);
    }

    public InputConnection getInputConnection() {
        InputConnection inputConnection;
        synchronized (this.mLock) {
            inputConnection = this.mInputConnection;
        }
        return inputConnection;
    }

    public boolean hasPendingInvalidation() {
        return this.mHasPendingInvalidation.get();
    }

    public boolean isFinished() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mFinished;
        }
        return z;
    }

    public boolean isActive() {
        return this.mParentInputMethodManager.isActive() && !isFinished();
    }

    public View getServedView() {
        return this.mServedView.get();
    }

    public void scheduleInvalidateInput() {
        if (this.mHasPendingInvalidation.compareAndSet(false, true)) {
            final int nextSessionId = this.mCurrentSessionId.incrementAndGet();
            this.mH.post(new Runnable() { // from class: com.android.internal.inputmethod.RemoteInputConnectionImpl$$ExternalSyntheticLambda7
                @Override // java.lang.Runnable
                public final void run() {
                    RemoteInputConnectionImpl.this.m6651x1f8acd17(nextSessionId);
                }
            });
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [300=6] */
    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$scheduleInvalidateInput$0$com-android-internal-inputmethod-RemoteInputConnectionImpl  reason: not valid java name */
    public /* synthetic */ void m6651x1f8acd17(int nextSessionId) {
        TextSnapshot textSnapshot;
        try {
            if (isFinished()) {
                return;
            }
            InputConnection ic = getInputConnection();
            if (ic == null) {
                return;
            }
            View view = getServedView();
            if (view == null) {
                return;
            }
            Class<?> cls = ic.getClass();
            boolean alwaysTrueEndBatchEditDetected = KnownAlwaysTrueEndBatchEditCache.contains(cls);
            if (!alwaysTrueEndBatchEditDetected) {
                boolean supportsBatchEdit = ic.beginBatchEdit();
                ic.finishComposingText();
                if (supportsBatchEdit) {
                    int retryCount = 0;
                    while (true) {
                        if (!ic.endBatchEdit()) {
                            break;
                        }
                        retryCount++;
                        if (retryCount > 16) {
                            Log.e(TAG, cls.getTypeName() + "#endBatchEdit() still returns true even after retrying 16 times.  Falling back to InputMethodManager#restartInput(View)");
                            alwaysTrueEndBatchEditDetected = true;
                            KnownAlwaysTrueEndBatchEditCache.add(cls);
                            break;
                        }
                    }
                }
            }
            if (alwaysTrueEndBatchEditDetected || (textSnapshot = ic.takeSnapshot()) == null || !this.mParentInputMethodManager.doInvalidateInput(this, textSnapshot, nextSessionId)) {
                this.mParentInputMethodManager.restartInput(view);
            }
        } finally {
            this.mHasPendingInvalidation.set(false);
        }
    }

    public void deactivate() {
        if (isFinished()) {
            return;
        }
        dispatch(new Runnable() { // from class: com.android.internal.inputmethod.RemoteInputConnectionImpl$$ExternalSyntheticLambda25
            @Override // java.lang.Runnable
            public final void run() {
                RemoteInputConnectionImpl.this.m6630xc0bfabff();
            }
        });
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [344=6] */
    /* JADX DEBUG: Finally have unexpected throw blocks count: 2, expect 1 */
    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$deactivate$2$com-android-internal-inputmethod-RemoteInputConnectionImpl  reason: not valid java name */
    public /* synthetic */ void m6630xc0bfabff() {
        Handler handler;
        if (isFinished()) {
            return;
        }
        Trace.traceBegin(4L, "InputConnection#closeConnection");
        try {
            InputConnection ic = getInputConnection();
            if (ic == null) {
                synchronized (this.mLock) {
                    this.mInputConnection = null;
                    this.mFinished = true;
                }
                Trace.traceEnd(4L);
                return;
            }
            try {
                ic.closeConnection();
            } catch (AbstractMethodError e) {
            }
            synchronized (this.mLock) {
                this.mInputConnection = null;
                this.mFinished = true;
            }
            Trace.traceEnd(4L);
            final View servedView = this.mServedView.get();
            if (servedView == null || (handler = servedView.getHandler()) == null) {
                return;
            }
            if (!handler.getLooper().isCurrentThread()) {
                Objects.requireNonNull(servedView);
                handler.post(new Runnable() { // from class: com.android.internal.inputmethod.RemoteInputConnectionImpl$$ExternalSyntheticLambda39
                    @Override // java.lang.Runnable
                    public final void run() {
                        View.this.onInputConnectionClosedInternal();
                    }
                });
                handler.post(new Runnable() { // from class: com.android.internal.inputmethod.RemoteInputConnectionImpl$$ExternalSyntheticLambda40
                    @Override // java.lang.Runnable
                    public final void run() {
                        RemoteInputConnectionImpl.lambda$deactivate$1(View.this);
                    }
                });
                return;
            }
            servedView.onInputConnectionClosedInternal();
            ViewRootImpl viewRoot = servedView.getViewRootImpl();
            if (viewRoot != null) {
                viewRoot.getHandwritingInitiator().onInputConnectionClosed(servedView);
            }
        } catch (Throwable th) {
            synchronized (this.mLock) {
                this.mInputConnection = null;
                this.mFinished = true;
                Trace.traceEnd(4L);
                throw th;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$deactivate$1(View servedView) {
        ViewRootImpl viewRoot = servedView.getViewRootImpl();
        if (viewRoot != null) {
            viewRoot.getHandwritingInitiator().onInputConnectionClosed(servedView);
        }
    }

    public String toString() {
        return "RemoteInputConnectionImpl{connection=" + getInputConnection() + " finished=" + isFinished() + " mParentInputMethodManager.isActive()=" + this.mParentInputMethodManager.isActive() + " mServedView=" + this.mServedView.get() + "}";
    }

    public void dumpDebug(ProtoOutputStream proto, long fieldId) {
        synchronized (this.mLock) {
            if ((this.mInputConnection instanceof DumpableInputConnection) && this.mLooper.isCurrentThread()) {
                ((DumpableInputConnection) this.mInputConnection).dumpDebug(proto, fieldId);
            }
        }
    }

    public void dispatchReportFullscreenMode(final boolean enabled) {
        dispatch(new Runnable() { // from class: com.android.internal.inputmethod.RemoteInputConnectionImpl$$ExternalSyntheticLambda41
            @Override // java.lang.Runnable
            public final void run() {
                RemoteInputConnectionImpl.this.m6633xe2f85626(enabled);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$dispatchReportFullscreenMode$3$com-android-internal-inputmethod-RemoteInputConnectionImpl  reason: not valid java name */
    public /* synthetic */ void m6633xe2f85626(boolean enabled) {
        InputConnection ic = getInputConnection();
        if (ic == null || !isActive()) {
            return;
        }
        ic.reportFullscreenMode(enabled);
    }

    @Override // com.android.internal.view.IInputContext
    public void getTextAfterCursor(final InputConnectionCommandHeader header, final int length, final int flags, AndroidFuture future) {
        dispatchWithTracing("getTextAfterCursor", future, new Supplier() { // from class: com.android.internal.inputmethod.RemoteInputConnectionImpl$$ExternalSyntheticLambda37
            @Override // java.util.function.Supplier
            public final Object get() {
                return RemoteInputConnectionImpl.this.m6642x8c546362(header, length, flags);
            }
        }, useImeTracing() ? new Function() { // from class: com.android.internal.inputmethod.RemoteInputConnectionImpl$$ExternalSyntheticLambda38
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                byte[] buildGetTextAfterCursorProto;
                buildGetTextAfterCursorProto = InputConnectionProtoDumper.buildGetTextAfterCursorProto(length, flags, (CharSequence) obj);
                return buildGetTextAfterCursorProto;
            }
        } : null);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getTextAfterCursor$4$com-android-internal-inputmethod-RemoteInputConnectionImpl  reason: not valid java name */
    public /* synthetic */ CharSequence m6642x8c546362(InputConnectionCommandHeader header, int length, int flags) {
        if (header.mSessionId != this.mCurrentSessionId.get()) {
            return null;
        }
        InputConnection ic = getInputConnection();
        if (ic == null || !isActive()) {
            Log.w(TAG, "getTextAfterCursor on inactive InputConnection");
            return null;
        } else if (length < 0) {
            Log.i(TAG, "Returning null to getTextAfterCursor due to an invalid length=" + length);
            return null;
        } else {
            return ic.getTextAfterCursor(length, flags);
        }
    }

    @Override // com.android.internal.view.IInputContext
    public void getTextBeforeCursor(final InputConnectionCommandHeader header, final int length, final int flags, AndroidFuture future) {
        dispatchWithTracing("getTextBeforeCursor", future, new Supplier() { // from class: com.android.internal.inputmethod.RemoteInputConnectionImpl$$ExternalSyntheticLambda26
            @Override // java.util.function.Supplier
            public final Object get() {
                return RemoteInputConnectionImpl.this.m6643xe3337e91(header, length, flags);
            }
        }, useImeTracing() ? new Function() { // from class: com.android.internal.inputmethod.RemoteInputConnectionImpl$$ExternalSyntheticLambda27
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                byte[] buildGetTextBeforeCursorProto;
                buildGetTextBeforeCursorProto = InputConnectionProtoDumper.buildGetTextBeforeCursorProto(length, flags, (CharSequence) obj);
                return buildGetTextBeforeCursorProto;
            }
        } : null);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getTextBeforeCursor$6$com-android-internal-inputmethod-RemoteInputConnectionImpl  reason: not valid java name */
    public /* synthetic */ CharSequence m6643xe3337e91(InputConnectionCommandHeader header, int length, int flags) {
        if (header.mSessionId != this.mCurrentSessionId.get()) {
            return null;
        }
        InputConnection ic = getInputConnection();
        if (ic == null || !isActive()) {
            Log.w(TAG, "getTextBeforeCursor on inactive InputConnection");
            return null;
        } else if (length < 0) {
            Log.i(TAG, "Returning null to getTextBeforeCursor due to an invalid length=" + length);
            return null;
        } else {
            return ic.getTextBeforeCursor(length, flags);
        }
    }

    @Override // com.android.internal.view.IInputContext
    public void getSelectedText(final InputConnectionCommandHeader header, final int flags, AndroidFuture future) {
        dispatchWithTracing("getSelectedText", future, new Supplier() { // from class: com.android.internal.inputmethod.RemoteInputConnectionImpl$$ExternalSyntheticLambda12
            @Override // java.util.function.Supplier
            public final Object get() {
                return RemoteInputConnectionImpl.this.m6640xc8c375a9(header, flags);
            }
        }, useImeTracing() ? new Function() { // from class: com.android.internal.inputmethod.RemoteInputConnectionImpl$$ExternalSyntheticLambda13
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                byte[] buildGetSelectedTextProto;
                buildGetSelectedTextProto = InputConnectionProtoDumper.buildGetSelectedTextProto(flags, (CharSequence) obj);
                return buildGetSelectedTextProto;
            }
        } : null);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getSelectedText$8$com-android-internal-inputmethod-RemoteInputConnectionImpl  reason: not valid java name */
    public /* synthetic */ CharSequence m6640xc8c375a9(InputConnectionCommandHeader header, int flags) {
        if (header.mSessionId != this.mCurrentSessionId.get()) {
            return null;
        }
        InputConnection ic = getInputConnection();
        if (ic == null || !isActive()) {
            Log.w(TAG, "getSelectedText on inactive InputConnection");
            return null;
        }
        try {
            return ic.getSelectedText(flags);
        } catch (AbstractMethodError e) {
            return null;
        }
    }

    @Override // com.android.internal.view.IInputContext
    public void getSurroundingText(final InputConnectionCommandHeader header, final int beforeLength, final int afterLength, final int flags, AndroidFuture future) {
        dispatchWithTracing("getSurroundingText", future, new Supplier() { // from class: com.android.internal.inputmethod.RemoteInputConnectionImpl$$ExternalSyntheticLambda42
            @Override // java.util.function.Supplier
            public final Object get() {
                return RemoteInputConnectionImpl.this.m6641xd47def3(header, beforeLength, afterLength, flags);
            }
        }, useImeTracing() ? new Function() { // from class: com.android.internal.inputmethod.RemoteInputConnectionImpl$$ExternalSyntheticLambda43
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                byte[] buildGetSurroundingTextProto;
                buildGetSurroundingTextProto = InputConnectionProtoDumper.buildGetSurroundingTextProto(beforeLength, afterLength, flags, (SurroundingText) obj);
                return buildGetSurroundingTextProto;
            }
        } : null);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getSurroundingText$10$com-android-internal-inputmethod-RemoteInputConnectionImpl  reason: not valid java name */
    public /* synthetic */ SurroundingText m6641xd47def3(InputConnectionCommandHeader header, int beforeLength, int afterLength, int flags) {
        if (header.mSessionId != this.mCurrentSessionId.get()) {
            return null;
        }
        InputConnection ic = getInputConnection();
        if (ic == null || !isActive()) {
            Log.w(TAG, "getSurroundingText on inactive InputConnection");
            return null;
        } else if (beforeLength < 0) {
            Log.i(TAG, "Returning null to getSurroundingText due to an invalid beforeLength=" + beforeLength);
            return null;
        } else if (afterLength < 0) {
            Log.i(TAG, "Returning null to getSurroundingText due to an invalid afterLength=" + afterLength);
            return null;
        } else {
            return ic.getSurroundingText(beforeLength, afterLength, flags);
        }
    }

    @Override // com.android.internal.view.IInputContext
    public void getCursorCapsMode(final InputConnectionCommandHeader header, final int reqModes, AndroidFuture future) {
        dispatchWithTracing("getCursorCapsMode", future, new Supplier() { // from class: com.android.internal.inputmethod.RemoteInputConnectionImpl$$ExternalSyntheticLambda4
            @Override // java.util.function.Supplier
            public final Object get() {
                return RemoteInputConnectionImpl.this.m6638x359c306c(header, reqModes);
            }
        }, useImeTracing() ? new Function() { // from class: com.android.internal.inputmethod.RemoteInputConnectionImpl$$ExternalSyntheticLambda5
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                byte[] buildGetCursorCapsModeProto;
                buildGetCursorCapsModeProto = InputConnectionProtoDumper.buildGetCursorCapsModeProto(reqModes, ((Integer) obj).intValue());
                return buildGetCursorCapsModeProto;
            }
        } : null);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getCursorCapsMode$12$com-android-internal-inputmethod-RemoteInputConnectionImpl  reason: not valid java name */
    public /* synthetic */ Integer m6638x359c306c(InputConnectionCommandHeader header, int reqModes) {
        if (header.mSessionId != this.mCurrentSessionId.get()) {
            return 0;
        }
        InputConnection ic = getInputConnection();
        if (ic == null || !isActive()) {
            Log.w(TAG, "getCursorCapsMode on inactive InputConnection");
            return 0;
        }
        return Integer.valueOf(ic.getCursorCapsMode(reqModes));
    }

    @Override // com.android.internal.view.IInputContext
    public void getExtractedText(final InputConnectionCommandHeader header, final ExtractedTextRequest request, final int flags, AndroidFuture future) {
        dispatchWithTracing("getExtractedText", future, new Supplier() { // from class: com.android.internal.inputmethod.RemoteInputConnectionImpl$$ExternalSyntheticLambda29
            @Override // java.util.function.Supplier
            public final Object get() {
                return RemoteInputConnectionImpl.this.m6639x43a81e2b(header, request, flags);
            }
        }, useImeTracing() ? new Function() { // from class: com.android.internal.inputmethod.RemoteInputConnectionImpl$$ExternalSyntheticLambda30
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                byte[] buildGetExtractedTextProto;
                buildGetExtractedTextProto = InputConnectionProtoDumper.buildGetExtractedTextProto(ExtractedTextRequest.this, flags, (ExtractedText) obj);
                return buildGetExtractedTextProto;
            }
        } : null);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getExtractedText$14$com-android-internal-inputmethod-RemoteInputConnectionImpl  reason: not valid java name */
    public /* synthetic */ ExtractedText m6639x43a81e2b(InputConnectionCommandHeader header, ExtractedTextRequest request, int flags) {
        if (header.mSessionId != this.mCurrentSessionId.get()) {
            return null;
        }
        InputConnection ic = getInputConnection();
        if (ic == null || !isActive()) {
            Log.w(TAG, "getExtractedText on inactive InputConnection");
            return null;
        }
        return ic.getExtractedText(request, flags);
    }

    @Override // com.android.internal.view.IInputContext
    public void commitText(final InputConnectionCommandHeader header, final CharSequence text, final int newCursorPosition) {
        dispatchWithTracing("commitText", new Runnable() { // from class: com.android.internal.inputmethod.RemoteInputConnectionImpl$$ExternalSyntheticLambda34
            @Override // java.lang.Runnable
            public final void run() {
                RemoteInputConnectionImpl.this.m6628x8bc80a76(header, text, newCursorPosition);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$commitText$16$com-android-internal-inputmethod-RemoteInputConnectionImpl  reason: not valid java name */
    public /* synthetic */ void m6628x8bc80a76(InputConnectionCommandHeader header, CharSequence text, int newCursorPosition) {
        if (header.mSessionId != this.mCurrentSessionId.get()) {
            return;
        }
        InputConnection ic = getInputConnection();
        if (ic == null || !isActive()) {
            Log.w(TAG, "commitText on inactive InputConnection");
        } else {
            ic.commitText(text, newCursorPosition);
        }
    }

    @Override // com.android.internal.view.IInputContext
    public void commitTextWithTextAttribute(final InputConnectionCommandHeader header, final CharSequence text, final int newCursorPosition, final TextAttribute textAttribute) {
        dispatchWithTracing("commitTextWithTextAttribute", new Runnable() { // from class: com.android.internal.inputmethod.RemoteInputConnectionImpl$$ExternalSyntheticLambda16
            @Override // java.lang.Runnable
            public final void run() {
                RemoteInputConnectionImpl.this.m6629xf823665c(header, text, newCursorPosition, textAttribute);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$commitTextWithTextAttribute$17$com-android-internal-inputmethod-RemoteInputConnectionImpl  reason: not valid java name */
    public /* synthetic */ void m6629xf823665c(InputConnectionCommandHeader header, CharSequence text, int newCursorPosition, TextAttribute textAttribute) {
        if (header.mSessionId != this.mCurrentSessionId.get()) {
            return;
        }
        InputConnection ic = getInputConnection();
        if (ic == null || !isActive()) {
            Log.w(TAG, "commitText on inactive InputConnection");
        } else {
            ic.commitText(text, newCursorPosition, textAttribute);
        }
    }

    @Override // com.android.internal.view.IInputContext
    public void commitCompletion(final InputConnectionCommandHeader header, final CompletionInfo text) {
        dispatchWithTracing("commitCompletion", new Runnable() { // from class: com.android.internal.inputmethod.RemoteInputConnectionImpl$$ExternalSyntheticLambda14
            @Override // java.lang.Runnable
            public final void run() {
                RemoteInputConnectionImpl.this.m6625x24cef8a3(header, text);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$commitCompletion$18$com-android-internal-inputmethod-RemoteInputConnectionImpl  reason: not valid java name */
    public /* synthetic */ void m6625x24cef8a3(InputConnectionCommandHeader header, CompletionInfo text) {
        if (header.mSessionId != this.mCurrentSessionId.get()) {
            return;
        }
        InputConnection ic = getInputConnection();
        if (ic == null || !isActive()) {
            Log.w(TAG, "commitCompletion on inactive InputConnection");
        } else {
            ic.commitCompletion(text);
        }
    }

    @Override // com.android.internal.view.IInputContext
    public void commitCorrection(final InputConnectionCommandHeader header, final CorrectionInfo info) {
        dispatchWithTracing("commitCorrection", new Runnable() { // from class: com.android.internal.inputmethod.RemoteInputConnectionImpl$$ExternalSyntheticLambda35
            @Override // java.lang.Runnable
            public final void run() {
                RemoteInputConnectionImpl.this.m6627x2b52e804(header, info);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$commitCorrection$19$com-android-internal-inputmethod-RemoteInputConnectionImpl  reason: not valid java name */
    public /* synthetic */ void m6627x2b52e804(InputConnectionCommandHeader header, CorrectionInfo info) {
        if (header.mSessionId != this.mCurrentSessionId.get()) {
            return;
        }
        InputConnection ic = getInputConnection();
        if (ic == null || !isActive()) {
            Log.w(TAG, "commitCorrection on inactive InputConnection");
            return;
        }
        try {
            ic.commitCorrection(info);
        } catch (AbstractMethodError e) {
        }
    }

    @Override // com.android.internal.view.IInputContext
    public void setSelection(final InputConnectionCommandHeader header, final int start, final int end) {
        dispatchWithTracing("setSelection", new Runnable() { // from class: com.android.internal.inputmethod.RemoteInputConnectionImpl$$ExternalSyntheticLambda15
            @Override // java.lang.Runnable
            public final void run() {
                RemoteInputConnectionImpl.this.m6658xa23f2943(header, start, end);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$setSelection$20$com-android-internal-inputmethod-RemoteInputConnectionImpl  reason: not valid java name */
    public /* synthetic */ void m6658xa23f2943(InputConnectionCommandHeader header, int start, int end) {
        if (header.mSessionId != this.mCurrentSessionId.get()) {
            return;
        }
        InputConnection ic = getInputConnection();
        if (ic == null || !isActive()) {
            Log.w(TAG, "setSelection on inactive InputConnection");
        } else {
            ic.setSelection(start, end);
        }
    }

    @Override // com.android.internal.view.IInputContext
    public void performEditorAction(final InputConnectionCommandHeader header, final int id) {
        dispatchWithTracing("performEditorAction", new Runnable() { // from class: com.android.internal.inputmethod.RemoteInputConnectionImpl$$ExternalSyntheticLambda33
            @Override // java.lang.Runnable
            public final void run() {
                RemoteInputConnectionImpl.this.m6645xf08c4f42(header, id);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$performEditorAction$21$com-android-internal-inputmethod-RemoteInputConnectionImpl  reason: not valid java name */
    public /* synthetic */ void m6645xf08c4f42(InputConnectionCommandHeader header, int id) {
        if (header.mSessionId != this.mCurrentSessionId.get()) {
            return;
        }
        InputConnection ic = getInputConnection();
        if (ic == null || !isActive()) {
            Log.w(TAG, "performEditorAction on inactive InputConnection");
        } else {
            ic.performEditorAction(id);
        }
    }

    @Override // com.android.internal.view.IInputContext
    public void performContextMenuAction(final InputConnectionCommandHeader header, final int id) {
        dispatchWithTracing("performContextMenuAction", new Runnable() { // from class: com.android.internal.inputmethod.RemoteInputConnectionImpl$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                RemoteInputConnectionImpl.this.m6644x8d5cc99a(header, id);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$performContextMenuAction$22$com-android-internal-inputmethod-RemoteInputConnectionImpl  reason: not valid java name */
    public /* synthetic */ void m6644x8d5cc99a(InputConnectionCommandHeader header, int id) {
        if (header.mSessionId != this.mCurrentSessionId.get()) {
            return;
        }
        InputConnection ic = getInputConnection();
        if (ic == null || !isActive()) {
            Log.w(TAG, "performContextMenuAction on inactive InputConnection");
        } else {
            ic.performContextMenuAction(id);
        }
    }

    @Override // com.android.internal.view.IInputContext
    public void setComposingRegion(final InputConnectionCommandHeader header, final int start, final int end) {
        dispatchWithTracing("setComposingRegion", new Runnable() { // from class: com.android.internal.inputmethod.RemoteInputConnectionImpl$$ExternalSyntheticLambda22
            @Override // java.lang.Runnable
            public final void run() {
                RemoteInputConnectionImpl.this.m6653x705ad0f7(header, start, end);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$setComposingRegion$23$com-android-internal-inputmethod-RemoteInputConnectionImpl  reason: not valid java name */
    public /* synthetic */ void m6653x705ad0f7(InputConnectionCommandHeader header, int start, int end) {
        if (header.mSessionId != this.mCurrentSessionId.get()) {
            return;
        }
        InputConnection ic = getInputConnection();
        if (ic == null || !isActive()) {
            Log.w(TAG, "setComposingRegion on inactive InputConnection");
            return;
        }
        try {
            ic.setComposingRegion(start, end);
        } catch (AbstractMethodError e) {
        }
    }

    @Override // com.android.internal.view.IInputContext
    public void setComposingRegionWithTextAttribute(final InputConnectionCommandHeader header, final int start, final int end, final TextAttribute textAttribute) {
        dispatchWithTracing("setComposingRegionWithTextAttribute", new Runnable() { // from class: com.android.internal.inputmethod.RemoteInputConnectionImpl$$ExternalSyntheticLambda24
            @Override // java.lang.Runnable
            public final void run() {
                RemoteInputConnectionImpl.this.m6654xfe0cba43(header, start, end, textAttribute);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$setComposingRegionWithTextAttribute$24$com-android-internal-inputmethod-RemoteInputConnectionImpl  reason: not valid java name */
    public /* synthetic */ void m6654xfe0cba43(InputConnectionCommandHeader header, int start, int end, TextAttribute textAttribute) {
        if (header.mSessionId != this.mCurrentSessionId.get()) {
            return;
        }
        InputConnection ic = getInputConnection();
        if (ic == null || !isActive()) {
            Log.w(TAG, "setComposingRegion on inactive InputConnection");
        } else {
            ic.setComposingRegion(start, end, textAttribute);
        }
    }

    @Override // com.android.internal.view.IInputContext
    public void setComposingText(final InputConnectionCommandHeader header, final CharSequence text, final int newCursorPosition) {
        dispatchWithTracing("setComposingText", new Runnable() { // from class: com.android.internal.inputmethod.RemoteInputConnectionImpl$$ExternalSyntheticLambda19
            @Override // java.lang.Runnable
            public final void run() {
                RemoteInputConnectionImpl.this.m6655x13fe8aae(header, text, newCursorPosition);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$setComposingText$25$com-android-internal-inputmethod-RemoteInputConnectionImpl  reason: not valid java name */
    public /* synthetic */ void m6655x13fe8aae(InputConnectionCommandHeader header, CharSequence text, int newCursorPosition) {
        if (header.mSessionId != this.mCurrentSessionId.get()) {
            return;
        }
        InputConnection ic = getInputConnection();
        if (ic == null || !isActive()) {
            Log.w(TAG, "setComposingText on inactive InputConnection");
        } else {
            ic.setComposingText(text, newCursorPosition);
        }
    }

    @Override // com.android.internal.view.IInputContext
    public void setComposingTextWithTextAttribute(final InputConnectionCommandHeader header, final CharSequence text, final int newCursorPosition, final TextAttribute textAttribute) {
        dispatchWithTracing("setComposingTextWithTextAttribute", new Runnable() { // from class: com.android.internal.inputmethod.RemoteInputConnectionImpl$$ExternalSyntheticLambda17
            @Override // java.lang.Runnable
            public final void run() {
                RemoteInputConnectionImpl.this.m6656xafe55f28(header, text, newCursorPosition, textAttribute);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$setComposingTextWithTextAttribute$26$com-android-internal-inputmethod-RemoteInputConnectionImpl  reason: not valid java name */
    public /* synthetic */ void m6656xafe55f28(InputConnectionCommandHeader header, CharSequence text, int newCursorPosition, TextAttribute textAttribute) {
        if (header.mSessionId != this.mCurrentSessionId.get()) {
            return;
        }
        InputConnection ic = getInputConnection();
        if (ic == null || !isActive()) {
            Log.w(TAG, "setComposingText on inactive InputConnection");
        } else {
            ic.setComposingText(text, newCursorPosition, textAttribute);
        }
    }

    public void finishComposingTextFromImm() {
        final int currentSessionId = this.mCurrentSessionId.get();
        dispatchWithTracing("finishComposingTextFromImm", new Runnable() { // from class: com.android.internal.inputmethod.RemoteInputConnectionImpl$$ExternalSyntheticLambda3
            @Override // java.lang.Runnable
            public final void run() {
                RemoteInputConnectionImpl.this.m6637x3477ef88(currentSessionId);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$finishComposingTextFromImm$27$com-android-internal-inputmethod-RemoteInputConnectionImpl  reason: not valid java name */
    public /* synthetic */ void m6637x3477ef88(int currentSessionId) {
        if (isFinished() || currentSessionId != this.mCurrentSessionId.get()) {
            return;
        }
        InputConnection ic = getInputConnection();
        if (ic == null) {
            Log.w(TAG, "finishComposingTextFromImm on inactive InputConnection");
        } else {
            ic.finishComposingText();
        }
    }

    @Override // com.android.internal.view.IInputContext
    public void finishComposingText(final InputConnectionCommandHeader header) {
        dispatchWithTracing("finishComposingText", new Runnable() { // from class: com.android.internal.inputmethod.RemoteInputConnectionImpl$$ExternalSyntheticLambda44
            @Override // java.lang.Runnable
            public final void run() {
                RemoteInputConnectionImpl.this.m6636x65d141e0(header);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$finishComposingText$28$com-android-internal-inputmethod-RemoteInputConnectionImpl  reason: not valid java name */
    public /* synthetic */ void m6636x65d141e0(InputConnectionCommandHeader header) {
        if (isFinished() || header.mSessionId != this.mCurrentSessionId.get()) {
            return;
        }
        InputConnection ic = getInputConnection();
        if (ic == null) {
            Log.w(TAG, "finishComposingText on inactive InputConnection");
        } else {
            ic.finishComposingText();
        }
    }

    @Override // com.android.internal.view.IInputContext
    public void sendKeyEvent(final InputConnectionCommandHeader header, final KeyEvent event) {
        dispatchWithTracing("sendKeyEvent", new Runnable() { // from class: com.android.internal.inputmethod.RemoteInputConnectionImpl$$ExternalSyntheticLambda10
            @Override // java.lang.Runnable
            public final void run() {
                RemoteInputConnectionImpl.this.m6652xcccbb9b3(header, event);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$sendKeyEvent$29$com-android-internal-inputmethod-RemoteInputConnectionImpl  reason: not valid java name */
    public /* synthetic */ void m6652xcccbb9b3(InputConnectionCommandHeader header, KeyEvent event) {
        if (header.mSessionId != this.mCurrentSessionId.get()) {
            return;
        }
        InputConnection ic = getInputConnection();
        if (ic == null || !isActive()) {
            Log.w(TAG, "sendKeyEvent on inactive InputConnection");
        } else {
            ic.sendKeyEvent(event);
        }
    }

    @Override // com.android.internal.view.IInputContext
    public void clearMetaKeyStates(final InputConnectionCommandHeader header, final int states) {
        dispatchWithTracing("clearMetaKeyStates", new Runnable() { // from class: com.android.internal.inputmethod.RemoteInputConnectionImpl$$ExternalSyntheticLambda21
            @Override // java.lang.Runnable
            public final void run() {
                RemoteInputConnectionImpl.this.m6624x760a8569(header, states);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$clearMetaKeyStates$30$com-android-internal-inputmethod-RemoteInputConnectionImpl  reason: not valid java name */
    public /* synthetic */ void m6624x760a8569(InputConnectionCommandHeader header, int states) {
        if (header.mSessionId != this.mCurrentSessionId.get()) {
            return;
        }
        InputConnection ic = getInputConnection();
        if (ic == null || !isActive()) {
            Log.w(TAG, "clearMetaKeyStates on inactive InputConnection");
        } else {
            ic.clearMetaKeyStates(states);
        }
    }

    @Override // com.android.internal.view.IInputContext
    public void deleteSurroundingText(final InputConnectionCommandHeader header, final int beforeLength, final int afterLength) {
        dispatchWithTracing("deleteSurroundingText", new Runnable() { // from class: com.android.internal.inputmethod.RemoteInputConnectionImpl$$ExternalSyntheticLambda23
            @Override // java.lang.Runnable
            public final void run() {
                RemoteInputConnectionImpl.this.m6631xd48d6385(header, beforeLength, afterLength);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$deleteSurroundingText$31$com-android-internal-inputmethod-RemoteInputConnectionImpl  reason: not valid java name */
    public /* synthetic */ void m6631xd48d6385(InputConnectionCommandHeader header, int beforeLength, int afterLength) {
        if (header.mSessionId != this.mCurrentSessionId.get()) {
            return;
        }
        InputConnection ic = getInputConnection();
        if (ic == null || !isActive()) {
            Log.w(TAG, "deleteSurroundingText on inactive InputConnection");
        } else {
            ic.deleteSurroundingText(beforeLength, afterLength);
        }
    }

    @Override // com.android.internal.view.IInputContext
    public void deleteSurroundingTextInCodePoints(final InputConnectionCommandHeader header, final int beforeLength, final int afterLength) {
        dispatchWithTracing("deleteSurroundingTextInCodePoints", new Runnable() { // from class: com.android.internal.inputmethod.RemoteInputConnectionImpl$$ExternalSyntheticLambda32
            @Override // java.lang.Runnable
            public final void run() {
                RemoteInputConnectionImpl.this.m6632xc6f186f9(header, beforeLength, afterLength);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$deleteSurroundingTextInCodePoints$32$com-android-internal-inputmethod-RemoteInputConnectionImpl  reason: not valid java name */
    public /* synthetic */ void m6632xc6f186f9(InputConnectionCommandHeader header, int beforeLength, int afterLength) {
        if (header.mSessionId != this.mCurrentSessionId.get()) {
            return;
        }
        InputConnection ic = getInputConnection();
        if (ic == null || !isActive()) {
            Log.w(TAG, "deleteSurroundingTextInCodePoints on inactive InputConnection");
            return;
        }
        try {
            ic.deleteSurroundingTextInCodePoints(beforeLength, afterLength);
        } catch (AbstractMethodError e) {
        }
    }

    @Override // com.android.internal.view.IInputContext
    public void beginBatchEdit(final InputConnectionCommandHeader header) {
        dispatchWithTracing("beginBatchEdit", new Runnable() { // from class: com.android.internal.inputmethod.RemoteInputConnectionImpl$$ExternalSyntheticLambda18
            @Override // java.lang.Runnable
            public final void run() {
                RemoteInputConnectionImpl.this.m6623x8ab7ff52(header);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$beginBatchEdit$33$com-android-internal-inputmethod-RemoteInputConnectionImpl  reason: not valid java name */
    public /* synthetic */ void m6623x8ab7ff52(InputConnectionCommandHeader header) {
        if (header.mSessionId != this.mCurrentSessionId.get()) {
            return;
        }
        InputConnection ic = getInputConnection();
        if (ic == null || !isActive()) {
            Log.w(TAG, "beginBatchEdit on inactive InputConnection");
        } else {
            ic.beginBatchEdit();
        }
    }

    @Override // com.android.internal.view.IInputContext
    public void endBatchEdit(final InputConnectionCommandHeader header) {
        dispatchWithTracing("endBatchEdit", new Runnable() { // from class: com.android.internal.inputmethod.RemoteInputConnectionImpl$$ExternalSyntheticLambda31
            @Override // java.lang.Runnable
            public final void run() {
                RemoteInputConnectionImpl.this.m6635x3c05597f(header);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$endBatchEdit$34$com-android-internal-inputmethod-RemoteInputConnectionImpl  reason: not valid java name */
    public /* synthetic */ void m6635x3c05597f(InputConnectionCommandHeader header) {
        if (header.mSessionId != this.mCurrentSessionId.get()) {
            return;
        }
        InputConnection ic = getInputConnection();
        if (ic == null || !isActive()) {
            Log.w(TAG, "endBatchEdit on inactive InputConnection");
        } else {
            ic.endBatchEdit();
        }
    }

    @Override // com.android.internal.view.IInputContext
    public void performSpellCheck(final InputConnectionCommandHeader header) {
        dispatchWithTracing("performSpellCheck", new Runnable() { // from class: com.android.internal.inputmethod.RemoteInputConnectionImpl$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                RemoteInputConnectionImpl.this.m6647xc485003c(header);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$performSpellCheck$35$com-android-internal-inputmethod-RemoteInputConnectionImpl  reason: not valid java name */
    public /* synthetic */ void m6647xc485003c(InputConnectionCommandHeader header) {
        if (header.mSessionId != this.mCurrentSessionId.get()) {
            return;
        }
        InputConnection ic = getInputConnection();
        if (ic == null || !isActive()) {
            Log.w(TAG, "performSpellCheck on inactive InputConnection");
        } else {
            ic.performSpellCheck();
        }
    }

    @Override // com.android.internal.view.IInputContext
    public void performPrivateCommand(final InputConnectionCommandHeader header, final String action, final Bundle data) {
        dispatchWithTracing("performPrivateCommand", new Runnable() { // from class: com.android.internal.inputmethod.RemoteInputConnectionImpl$$ExternalSyntheticLambda6
            @Override // java.lang.Runnable
            public final void run() {
                RemoteInputConnectionImpl.this.m6646x67489723(header, action, data);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$performPrivateCommand$36$com-android-internal-inputmethod-RemoteInputConnectionImpl  reason: not valid java name */
    public /* synthetic */ void m6646x67489723(InputConnectionCommandHeader header, String action, Bundle data) {
        if (header.mSessionId != this.mCurrentSessionId.get()) {
            return;
        }
        InputConnection ic = getInputConnection();
        if (ic == null || !isActive()) {
            Log.w(TAG, "performPrivateCommand on inactive InputConnection");
        } else {
            ic.performPrivateCommand(action, data);
        }
    }

    public void requestCursorUpdatesFromImm(final int cursorUpdateMode, final int cursorUpdateFilter, final int imeDisplayId) {
        final int currentSessionId = this.mCurrentSessionId.get();
        dispatchWithTracing("requestCursorUpdatesFromImm", new Runnable() { // from class: com.android.internal.inputmethod.RemoteInputConnectionImpl$$ExternalSyntheticLambda8
            @Override // java.lang.Runnable
            public final void run() {
                RemoteInputConnectionImpl.this.m6649x58d45833(currentSessionId, cursorUpdateMode, cursorUpdateFilter, imeDisplayId);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$requestCursorUpdatesFromImm$37$com-android-internal-inputmethod-RemoteInputConnectionImpl  reason: not valid java name */
    public /* synthetic */ void m6649x58d45833(int currentSessionId, int cursorUpdateMode, int cursorUpdateFilter, int imeDisplayId) {
        if (currentSessionId != this.mCurrentSessionId.get()) {
            return;
        }
        requestCursorUpdatesInternal(cursorUpdateMode, cursorUpdateFilter, imeDisplayId);
    }

    @Override // com.android.internal.view.IInputContext
    public void requestCursorUpdates(final InputConnectionCommandHeader header, final int cursorUpdateMode, final int imeDisplayId, AndroidFuture future) {
        dispatchWithTracing("requestCursorUpdates", future, new Supplier() { // from class: com.android.internal.inputmethod.RemoteInputConnectionImpl$$ExternalSyntheticLambda11
            @Override // java.util.function.Supplier
            public final Object get() {
                return RemoteInputConnectionImpl.this.m6648x7eba9577(header, cursorUpdateMode, imeDisplayId);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$requestCursorUpdates$38$com-android-internal-inputmethod-RemoteInputConnectionImpl  reason: not valid java name */
    public /* synthetic */ Boolean m6648x7eba9577(InputConnectionCommandHeader header, int cursorUpdateMode, int imeDisplayId) {
        if (header.mSessionId != this.mCurrentSessionId.get()) {
            return false;
        }
        return Boolean.valueOf(requestCursorUpdatesInternal(cursorUpdateMode, 0, imeDisplayId));
    }

    @Override // com.android.internal.view.IInputContext
    public void requestCursorUpdatesWithFilter(final InputConnectionCommandHeader header, final int cursorUpdateMode, final int cursorUpdateFilter, final int imeDisplayId, AndroidFuture future) {
        dispatchWithTracing("requestCursorUpdates", future, new Supplier() { // from class: com.android.internal.inputmethod.RemoteInputConnectionImpl$$ExternalSyntheticLambda20
            @Override // java.util.function.Supplier
            public final Object get() {
                return RemoteInputConnectionImpl.this.m6650xb39d75d4(header, cursorUpdateMode, cursorUpdateFilter, imeDisplayId);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$requestCursorUpdatesWithFilter$39$com-android-internal-inputmethod-RemoteInputConnectionImpl  reason: not valid java name */
    public /* synthetic */ Boolean m6650xb39d75d4(InputConnectionCommandHeader header, int cursorUpdateMode, int cursorUpdateFilter, int imeDisplayId) {
        if (header.mSessionId != this.mCurrentSessionId.get()) {
            return false;
        }
        return Boolean.valueOf(requestCursorUpdatesInternal(cursorUpdateMode, cursorUpdateFilter, imeDisplayId));
    }

    private boolean requestCursorUpdatesInternal(int cursorUpdateMode, int cursorUpdateFilter, int imeDisplayId) {
        InputConnection ic = getInputConnection();
        if (ic == null || !isActive()) {
            Log.w(TAG, "requestCursorAnchorInfo on inactive InputConnection");
            return false;
        } else if (this.mParentInputMethodManager.getDisplayId() != imeDisplayId && !this.mParentInputMethodManager.hasVirtualDisplayToScreenMatrix()) {
            return false;
        } else {
            try {
                return ic.requestCursorUpdates(cursorUpdateMode, cursorUpdateFilter);
            } catch (AbstractMethodError e) {
                return false;
            }
        }
    }

    @Override // com.android.internal.view.IInputContext
    public void commitContent(final InputConnectionCommandHeader header, final InputContentInfo inputContentInfo, final int flags, final Bundle opts, AndroidFuture future) {
        dispatchWithTracing("commitContent", future, new Supplier() { // from class: com.android.internal.inputmethod.RemoteInputConnectionImpl$$ExternalSyntheticLambda9
            @Override // java.util.function.Supplier
            public final Object get() {
                return RemoteInputConnectionImpl.this.m6626x666341e3(header, inputContentInfo, flags, opts);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$commitContent$40$com-android-internal-inputmethod-RemoteInputConnectionImpl  reason: not valid java name */
    public /* synthetic */ Boolean m6626x666341e3(InputConnectionCommandHeader header, InputContentInfo inputContentInfo, int flags, Bundle opts) {
        if (header.mSessionId != this.mCurrentSessionId.get()) {
            return false;
        }
        InputConnection ic = getInputConnection();
        if (ic == null || !isActive()) {
            Log.w(TAG, "commitContent on inactive InputConnection");
            return false;
        } else if (inputContentInfo == null || !inputContentInfo.validate()) {
            Log.w(TAG, "commitContent with invalid inputContentInfo=" + inputContentInfo);
            return false;
        } else {
            try {
                return Boolean.valueOf(ic.commitContent(inputContentInfo, flags, opts));
            } catch (AbstractMethodError e) {
                return false;
            }
        }
    }

    @Override // com.android.internal.view.IInputContext
    public void setImeConsumesInput(final InputConnectionCommandHeader header, final boolean imeConsumesInput) {
        dispatchWithTracing("setImeConsumesInput", new Runnable() { // from class: com.android.internal.inputmethod.RemoteInputConnectionImpl$$ExternalSyntheticLambda36
            @Override // java.lang.Runnable
            public final void run() {
                RemoteInputConnectionImpl.this.m6657x577a6894(header, imeConsumesInput);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$setImeConsumesInput$41$com-android-internal-inputmethod-RemoteInputConnectionImpl  reason: not valid java name */
    public /* synthetic */ void m6657x577a6894(InputConnectionCommandHeader header, boolean imeConsumesInput) {
        if (header.mSessionId != this.mCurrentSessionId.get()) {
            return;
        }
        InputConnection ic = getInputConnection();
        if (ic == null || !isActive()) {
            Log.w(TAG, "setImeConsumesInput on inactive InputConnection");
        } else {
            ic.setImeConsumesInput(imeConsumesInput);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: com.android.internal.inputmethod.RemoteInputConnectionImpl$1  reason: invalid class name */
    /* loaded from: classes4.dex */
    public class AnonymousClass1 extends IRemoteAccessibilityInputConnection.Stub {
        AnonymousClass1() {
        }

        @Override // com.android.internal.inputmethod.IRemoteAccessibilityInputConnection
        public void commitText(final InputConnectionCommandHeader header, final CharSequence text, final int newCursorPosition, final TextAttribute textAttribute) {
            RemoteInputConnectionImpl.this.dispatchWithTracing("commitTextFromA11yIme", new Runnable() { // from class: com.android.internal.inputmethod.RemoteInputConnectionImpl$1$$ExternalSyntheticLambda8
                @Override // java.lang.Runnable
                public final void run() {
                    RemoteInputConnectionImpl.AnonymousClass1.this.m6660x7a89cfe(header, text, newCursorPosition, textAttribute);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$commitText$0$com-android-internal-inputmethod-RemoteInputConnectionImpl$1  reason: not valid java name */
        public /* synthetic */ void m6660x7a89cfe(InputConnectionCommandHeader header, CharSequence text, int newCursorPosition, TextAttribute textAttribute) {
            if (header.mSessionId != RemoteInputConnectionImpl.this.mCurrentSessionId.get()) {
                return;
            }
            InputConnection ic = RemoteInputConnectionImpl.this.getInputConnection();
            if (ic == null || !RemoteInputConnectionImpl.this.isActive()) {
                Log.w(RemoteInputConnectionImpl.TAG, "commitText on inactive InputConnection");
                return;
            }
            ic.beginBatchEdit();
            ic.finishComposingText();
            ic.commitText(text, newCursorPosition, textAttribute);
            ic.endBatchEdit();
        }

        @Override // com.android.internal.inputmethod.IRemoteAccessibilityInputConnection
        public void setSelection(final InputConnectionCommandHeader header, final int start, final int end) {
            RemoteInputConnectionImpl.this.dispatchWithTracing("setSelectionFromA11yIme", new Runnable() { // from class: com.android.internal.inputmethod.RemoteInputConnectionImpl$1$$ExternalSyntheticLambda7
                @Override // java.lang.Runnable
                public final void run() {
                    RemoteInputConnectionImpl.AnonymousClass1.this.m6667x1ed71ad7(header, start, end);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$setSelection$1$com-android-internal-inputmethod-RemoteInputConnectionImpl$1  reason: not valid java name */
        public /* synthetic */ void m6667x1ed71ad7(InputConnectionCommandHeader header, int start, int end) {
            if (header.mSessionId != RemoteInputConnectionImpl.this.mCurrentSessionId.get()) {
                return;
            }
            InputConnection ic = RemoteInputConnectionImpl.this.getInputConnection();
            if (ic == null || !RemoteInputConnectionImpl.this.isActive()) {
                Log.w(RemoteInputConnectionImpl.TAG, "setSelection on inactive InputConnection");
            } else {
                ic.setSelection(start, end);
            }
        }

        @Override // com.android.internal.inputmethod.IRemoteAccessibilityInputConnection
        public void getSurroundingText(final InputConnectionCommandHeader header, final int beforeLength, final int afterLength, final int flags, AndroidFuture future) {
            RemoteInputConnectionImpl.this.dispatchWithTracing("getSurroundingTextFromA11yIme", future, new Supplier() { // from class: com.android.internal.inputmethod.RemoteInputConnectionImpl$1$$ExternalSyntheticLambda3
                @Override // java.util.function.Supplier
                public final Object get() {
                    return RemoteInputConnectionImpl.AnonymousClass1.this.m6663x3d61fa65(header, beforeLength, afterLength, flags);
                }
            }, RemoteInputConnectionImpl.m6622$$Nest$smuseImeTracing() ? new Function() { // from class: com.android.internal.inputmethod.RemoteInputConnectionImpl$1$$ExternalSyntheticLambda4
                @Override // java.util.function.Function
                public final Object apply(Object obj) {
                    byte[] buildGetSurroundingTextProto;
                    buildGetSurroundingTextProto = InputConnectionProtoDumper.buildGetSurroundingTextProto(beforeLength, afterLength, flags, (SurroundingText) obj);
                    return buildGetSurroundingTextProto;
                }
            } : null);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$getSurroundingText$2$com-android-internal-inputmethod-RemoteInputConnectionImpl$1  reason: not valid java name */
        public /* synthetic */ SurroundingText m6663x3d61fa65(InputConnectionCommandHeader header, int beforeLength, int afterLength, int flags) {
            if (header.mSessionId != RemoteInputConnectionImpl.this.mCurrentSessionId.get()) {
                return null;
            }
            InputConnection ic = RemoteInputConnectionImpl.this.getInputConnection();
            if (ic == null || !RemoteInputConnectionImpl.this.isActive()) {
                Log.w(RemoteInputConnectionImpl.TAG, "getSurroundingText on inactive InputConnection");
                return null;
            } else if (beforeLength < 0) {
                Log.i(RemoteInputConnectionImpl.TAG, "Returning null to getSurroundingText due to an invalid beforeLength=" + beforeLength);
                return null;
            } else if (afterLength < 0) {
                Log.i(RemoteInputConnectionImpl.TAG, "Returning null to getSurroundingText due to an invalid afterLength=" + afterLength);
                return null;
            } else {
                return ic.getSurroundingText(beforeLength, afterLength, flags);
            }
        }

        @Override // com.android.internal.inputmethod.IRemoteAccessibilityInputConnection
        public void deleteSurroundingText(final InputConnectionCommandHeader header, final int beforeLength, final int afterLength) {
            RemoteInputConnectionImpl.this.dispatchWithTracing("deleteSurroundingTextFromA11yIme", new Runnable() { // from class: com.android.internal.inputmethod.RemoteInputConnectionImpl$1$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    RemoteInputConnectionImpl.AnonymousClass1.this.m6661xa4576792(header, beforeLength, afterLength);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$deleteSurroundingText$4$com-android-internal-inputmethod-RemoteInputConnectionImpl$1  reason: not valid java name */
        public /* synthetic */ void m6661xa4576792(InputConnectionCommandHeader header, int beforeLength, int afterLength) {
            if (header.mSessionId != RemoteInputConnectionImpl.this.mCurrentSessionId.get()) {
                return;
            }
            InputConnection ic = RemoteInputConnectionImpl.this.getInputConnection();
            if (ic == null || !RemoteInputConnectionImpl.this.isActive()) {
                Log.w(RemoteInputConnectionImpl.TAG, "deleteSurroundingText on inactive InputConnection");
            } else {
                ic.deleteSurroundingText(beforeLength, afterLength);
            }
        }

        @Override // com.android.internal.inputmethod.IRemoteAccessibilityInputConnection
        public void sendKeyEvent(final InputConnectionCommandHeader header, final KeyEvent event) {
            RemoteInputConnectionImpl.this.dispatchWithTracing("sendKeyEventFromA11yIme", new Runnable() { // from class: com.android.internal.inputmethod.RemoteInputConnectionImpl$1$$ExternalSyntheticLambda10
                @Override // java.lang.Runnable
                public final void run() {
                    RemoteInputConnectionImpl.AnonymousClass1.this.m6666x8b3b25a(header, event);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$sendKeyEvent$5$com-android-internal-inputmethod-RemoteInputConnectionImpl$1  reason: not valid java name */
        public /* synthetic */ void m6666x8b3b25a(InputConnectionCommandHeader header, KeyEvent event) {
            if (header.mSessionId != RemoteInputConnectionImpl.this.mCurrentSessionId.get()) {
                return;
            }
            InputConnection ic = RemoteInputConnectionImpl.this.getInputConnection();
            if (ic == null || !RemoteInputConnectionImpl.this.isActive()) {
                Log.w(RemoteInputConnectionImpl.TAG, "sendKeyEvent on inactive InputConnection");
            } else {
                ic.sendKeyEvent(event);
            }
        }

        @Override // com.android.internal.inputmethod.IRemoteAccessibilityInputConnection
        public void performEditorAction(final InputConnectionCommandHeader header, final int id) {
            RemoteInputConnectionImpl.this.dispatchWithTracing("performEditorActionFromA11yIme", new Runnable() { // from class: com.android.internal.inputmethod.RemoteInputConnectionImpl$1$$ExternalSyntheticLambda9
                @Override // java.lang.Runnable
                public final void run() {
                    RemoteInputConnectionImpl.AnonymousClass1.this.m6665xc4f14252(header, id);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$performEditorAction$6$com-android-internal-inputmethod-RemoteInputConnectionImpl$1  reason: not valid java name */
        public /* synthetic */ void m6665xc4f14252(InputConnectionCommandHeader header, int id) {
            if (header.mSessionId != RemoteInputConnectionImpl.this.mCurrentSessionId.get()) {
                return;
            }
            InputConnection ic = RemoteInputConnectionImpl.this.getInputConnection();
            if (ic == null || !RemoteInputConnectionImpl.this.isActive()) {
                Log.w(RemoteInputConnectionImpl.TAG, "performEditorAction on inactive InputConnection");
            } else {
                ic.performEditorAction(id);
            }
        }

        @Override // com.android.internal.inputmethod.IRemoteAccessibilityInputConnection
        public void performContextMenuAction(final InputConnectionCommandHeader header, final int id) {
            RemoteInputConnectionImpl.this.dispatchWithTracing("performContextMenuActionFromA11yIme", new Runnable() { // from class: com.android.internal.inputmethod.RemoteInputConnectionImpl$1$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    RemoteInputConnectionImpl.AnonymousClass1.this.m6664x9bf8e218(header, id);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$performContextMenuAction$7$com-android-internal-inputmethod-RemoteInputConnectionImpl$1  reason: not valid java name */
        public /* synthetic */ void m6664x9bf8e218(InputConnectionCommandHeader header, int id) {
            if (header.mSessionId != RemoteInputConnectionImpl.this.mCurrentSessionId.get()) {
                return;
            }
            InputConnection ic = RemoteInputConnectionImpl.this.getInputConnection();
            if (ic == null || !RemoteInputConnectionImpl.this.isActive()) {
                Log.w(RemoteInputConnectionImpl.TAG, "performContextMenuAction on inactive InputConnection");
            } else {
                ic.performContextMenuAction(id);
            }
        }

        @Override // com.android.internal.inputmethod.IRemoteAccessibilityInputConnection
        public void getCursorCapsMode(final InputConnectionCommandHeader header, final int reqModes, AndroidFuture future) {
            RemoteInputConnectionImpl.this.dispatchWithTracing("getCursorCapsModeFromA11yIme", future, new Supplier() { // from class: com.android.internal.inputmethod.RemoteInputConnectionImpl$1$$ExternalSyntheticLambda5
                @Override // java.util.function.Supplier
                public final Object get() {
                    return RemoteInputConnectionImpl.AnonymousClass1.this.m6662x242334c4(header, reqModes);
                }
            }, RemoteInputConnectionImpl.m6622$$Nest$smuseImeTracing() ? new Function() { // from class: com.android.internal.inputmethod.RemoteInputConnectionImpl$1$$ExternalSyntheticLambda6
                @Override // java.util.function.Function
                public final Object apply(Object obj) {
                    byte[] buildGetCursorCapsModeProto;
                    buildGetCursorCapsModeProto = InputConnectionProtoDumper.buildGetCursorCapsModeProto(reqModes, ((Integer) obj).intValue());
                    return buildGetCursorCapsModeProto;
                }
            } : null);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$getCursorCapsMode$8$com-android-internal-inputmethod-RemoteInputConnectionImpl$1  reason: not valid java name */
        public /* synthetic */ Integer m6662x242334c4(InputConnectionCommandHeader header, int reqModes) {
            if (header.mSessionId != RemoteInputConnectionImpl.this.mCurrentSessionId.get()) {
                return 0;
            }
            InputConnection ic = RemoteInputConnectionImpl.this.getInputConnection();
            if (ic == null || !RemoteInputConnectionImpl.this.isActive()) {
                Log.w(RemoteInputConnectionImpl.TAG, "getCursorCapsMode on inactive InputConnection");
                return 0;
            }
            return Integer.valueOf(ic.getCursorCapsMode(reqModes));
        }

        @Override // com.android.internal.inputmethod.IRemoteAccessibilityInputConnection
        public void clearMetaKeyStates(final InputConnectionCommandHeader header, final int states) {
            RemoteInputConnectionImpl.this.dispatchWithTracing("clearMetaKeyStatesFromA11yIme", new Runnable() { // from class: com.android.internal.inputmethod.RemoteInputConnectionImpl$1$$ExternalSyntheticLambda2
                @Override // java.lang.Runnable
                public final void run() {
                    RemoteInputConnectionImpl.AnonymousClass1.this.m6659x365fcb34(header, states);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$clearMetaKeyStates$10$com-android-internal-inputmethod-RemoteInputConnectionImpl$1  reason: not valid java name */
        public /* synthetic */ void m6659x365fcb34(InputConnectionCommandHeader header, int states) {
            if (header.mSessionId != RemoteInputConnectionImpl.this.mCurrentSessionId.get()) {
                return;
            }
            InputConnection ic = RemoteInputConnectionImpl.this.getInputConnection();
            if (ic == null || !RemoteInputConnectionImpl.this.isActive()) {
                Log.w(RemoteInputConnectionImpl.TAG, "clearMetaKeyStates on inactive InputConnection");
            } else {
                ic.clearMetaKeyStates(states);
            }
        }
    }

    public IRemoteAccessibilityInputConnection asIRemoteAccessibilityInputConnection() {
        return this.mAccessibilityInputConnection;
    }

    private void dispatch(Runnable runnable) {
        if (this.mLooper.isCurrentThread()) {
            runnable.run();
        } else {
            this.mH.post(runnable);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void dispatchWithTracing(final String methodName, final Runnable runnable) {
        Runnable actualRunnable;
        if (Trace.isTagEnabled(4L)) {
            actualRunnable = new Runnable() { // from class: com.android.internal.inputmethod.RemoteInputConnectionImpl$$ExternalSyntheticLambda28
                @Override // java.lang.Runnable
                public final void run() {
                    RemoteInputConnectionImpl.lambda$dispatchWithTracing$42(methodName, runnable);
                }
            };
        } else {
            actualRunnable = runnable;
        }
        dispatch(actualRunnable);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$dispatchWithTracing$42(String methodName, Runnable runnable) {
        Trace.traceBegin(4L, "InputConnection#" + methodName);
        try {
            runnable.run();
        } finally {
            Trace.traceEnd(4L);
        }
    }

    private <T> void dispatchWithTracing(String methodName, AndroidFuture untypedFuture, Supplier<T> supplier) {
        dispatchWithTracing(methodName, untypedFuture, supplier, null);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public <T> void dispatchWithTracing(final String methodName, final AndroidFuture untypedFuture, final Supplier<T> supplier, final Function<T, byte[]> dumpProtoProvider) {
        dispatchWithTracing(methodName, new Runnable() { // from class: com.android.internal.inputmethod.RemoteInputConnectionImpl$$ExternalSyntheticLambda2
            @Override // java.lang.Runnable
            public final void run() {
                RemoteInputConnectionImpl.this.m6634x8a2316c0(supplier, untypedFuture, dumpProtoProvider, methodName);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$dispatchWithTracing$43$com-android-internal-inputmethod-RemoteInputConnectionImpl  reason: not valid java name */
    public /* synthetic */ void m6634x8a2316c0(Supplier supplier, AndroidFuture future, Function dumpProtoProvider, String methodName) {
        try {
            Object obj = supplier.get();
            future.complete(obj);
            if (dumpProtoProvider != null) {
                byte[] icProto = (byte[]) dumpProtoProvider.apply(obj);
                ImeTracing.getInstance().triggerClientDump("RemoteInputConnectionImpl#" + methodName, this.mParentInputMethodManager, icProto);
            }
        } catch (Throwable throwable) {
            future.completeExceptionally(throwable);
            throw throwable;
        }
    }

    private static boolean useImeTracing() {
        return ImeTracing.getInstance().isEnabled();
    }

    public AtomicInteger getSessionId() {
        return this.mCurrentSessionId;
    }
}
