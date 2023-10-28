package com.android.server.soundtrigger_middleware;

import android.media.permission.Identity;
import android.media.permission.IdentityContext;
import android.media.soundtrigger.ModelParameterRange;
import android.media.soundtrigger.PhraseRecognitionEvent;
import android.media.soundtrigger.PhraseSoundModel;
import android.media.soundtrigger.RecognitionConfig;
import android.media.soundtrigger.RecognitionEvent;
import android.media.soundtrigger.SoundModel;
import android.media.soundtrigger_middleware.ISoundTriggerCallback;
import android.media.soundtrigger_middleware.ISoundTriggerModule;
import android.media.soundtrigger_middleware.SoundTriggerModuleDescriptor;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Objects;
/* loaded from: classes2.dex */
public class SoundTriggerMiddlewareLogging implements ISoundTriggerMiddlewareInternal, Dumpable {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MM-dd HH:mm:ss:SSS");
    private static final int NUM_EVENTS_TO_DUMP = 64;
    private static final String TAG = "SoundTriggerMiddlewareLogging";
    private final ISoundTriggerMiddlewareInternal mDelegate;
    private final LinkedList<Event> mLastEvents = new LinkedList<>();

    public SoundTriggerMiddlewareLogging(ISoundTriggerMiddlewareInternal delegate) {
        this.mDelegate = delegate;
    }

    @Override // com.android.server.soundtrigger_middleware.ISoundTriggerMiddlewareInternal
    public SoundTriggerModuleDescriptor[] listModules() {
        try {
            SoundTriggerModuleDescriptor[] result = this.mDelegate.listModules();
            logReturn("listModules", result, new Object[0]);
            return result;
        } catch (Exception e) {
            logException("listModules", e, new Object[0]);
            throw e;
        }
    }

    @Override // com.android.server.soundtrigger_middleware.ISoundTriggerMiddlewareInternal
    public ISoundTriggerModule attach(int handle, ISoundTriggerCallback callback) {
        try {
            ModuleLogging result = new ModuleLogging(callback);
            result.attach(this.mDelegate.attach(handle, result.getCallbackWrapper()));
            logReturn("attach", result, Integer.valueOf(handle), callback);
            return result;
        } catch (Exception e) {
            logException("attach", e, Integer.valueOf(handle), callback);
            throw e;
        }
    }

    public String toString() {
        return this.mDelegate.toString();
    }

    private void logException(String methodName, Exception ex, Object... args) {
        logExceptionWithObject(this, IdentityContext.get(), methodName, ex, args);
    }

    private void logReturn(String methodName, Object retVal, Object... args) {
        logReturnWithObject(this, IdentityContext.get(), methodName, retVal, args);
    }

    private void logVoidReturn(String methodName, Object... args) {
        logVoidReturnWithObject(this, IdentityContext.get(), methodName, args);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class ModuleLogging implements ISoundTriggerModule {
        private final CallbackLogging mCallbackWrapper;
        private ISoundTriggerModule mDelegate;
        private final Identity mOriginatorIdentity = IdentityContext.getNonNull();

        ModuleLogging(ISoundTriggerCallback callback) {
            this.mCallbackWrapper = new CallbackLogging(callback);
        }

        void attach(ISoundTriggerModule delegate) {
            this.mDelegate = delegate;
        }

        ISoundTriggerCallback getCallbackWrapper() {
            return this.mCallbackWrapper;
        }

        public int loadModel(SoundModel model) throws RemoteException {
            try {
                int result = this.mDelegate.loadModel(model);
                logReturn("loadModel", Integer.valueOf(result), model);
                return result;
            } catch (Exception e) {
                logException("loadModel", e, model);
                throw e;
            }
        }

        public int loadPhraseModel(PhraseSoundModel model) throws RemoteException {
            try {
                int result = this.mDelegate.loadPhraseModel(model);
                logReturn("loadPhraseModel", Integer.valueOf(result), model);
                return result;
            } catch (Exception e) {
                logException("loadPhraseModel", e, model);
                throw e;
            }
        }

        public void unloadModel(int modelHandle) throws RemoteException {
            try {
                this.mDelegate.unloadModel(modelHandle);
                logVoidReturn("unloadModel", Integer.valueOf(modelHandle));
            } catch (Exception e) {
                logException("unloadModel", e, Integer.valueOf(modelHandle));
                throw e;
            }
        }

        public void startRecognition(int modelHandle, RecognitionConfig config) throws RemoteException {
            try {
                this.mDelegate.startRecognition(modelHandle, config);
                logVoidReturn("startRecognition", Integer.valueOf(modelHandle), config);
            } catch (Exception e) {
                logException("startRecognition", e, Integer.valueOf(modelHandle), config);
                throw e;
            }
        }

        public void stopRecognition(int modelHandle) throws RemoteException {
            try {
                this.mDelegate.stopRecognition(modelHandle);
                logVoidReturn("stopRecognition", Integer.valueOf(modelHandle));
            } catch (Exception e) {
                logException("stopRecognition", e, Integer.valueOf(modelHandle));
                throw e;
            }
        }

        public void forceRecognitionEvent(int modelHandle) throws RemoteException {
            try {
                this.mDelegate.forceRecognitionEvent(modelHandle);
                logVoidReturn("forceRecognitionEvent", Integer.valueOf(modelHandle));
            } catch (Exception e) {
                logException("forceRecognitionEvent", e, Integer.valueOf(modelHandle));
                throw e;
            }
        }

        public void setModelParameter(int modelHandle, int modelParam, int value) throws RemoteException {
            try {
                this.mDelegate.setModelParameter(modelHandle, modelParam, value);
                logVoidReturn("setModelParameter", Integer.valueOf(modelHandle), Integer.valueOf(modelParam), Integer.valueOf(value));
            } catch (Exception e) {
                logException("setModelParameter", e, Integer.valueOf(modelHandle), Integer.valueOf(modelParam), Integer.valueOf(value));
                throw e;
            }
        }

        public int getModelParameter(int modelHandle, int modelParam) throws RemoteException {
            try {
                int result = this.mDelegate.getModelParameter(modelHandle, modelParam);
                logReturn("getModelParameter", Integer.valueOf(result), Integer.valueOf(modelHandle), Integer.valueOf(modelParam));
                return result;
            } catch (Exception e) {
                logException("getModelParameter", e, Integer.valueOf(modelHandle), Integer.valueOf(modelParam));
                throw e;
            }
        }

        public ModelParameterRange queryModelParameterSupport(int modelHandle, int modelParam) throws RemoteException {
            try {
                ModelParameterRange result = this.mDelegate.queryModelParameterSupport(modelHandle, modelParam);
                logReturn("queryModelParameterSupport", result, Integer.valueOf(modelHandle), Integer.valueOf(modelParam));
                return result;
            } catch (Exception e) {
                logException("queryModelParameterSupport", e, Integer.valueOf(modelHandle), Integer.valueOf(modelParam));
                throw e;
            }
        }

        public void detach() throws RemoteException {
            try {
                this.mDelegate.detach();
                logVoidReturn("detach", new Object[0]);
            } catch (Exception e) {
                logException("detach", e, new Object[0]);
                throw e;
            }
        }

        public IBinder asBinder() {
            return this.mDelegate.asBinder();
        }

        public String toString() {
            return Objects.toString(this.mDelegate);
        }

        private void logException(String methodName, Exception ex, Object... args) {
            SoundTriggerMiddlewareLogging.this.logExceptionWithObject(this, this.mOriginatorIdentity, methodName, ex, args);
        }

        private void logReturn(String methodName, Object retVal, Object... args) {
            SoundTriggerMiddlewareLogging.this.logReturnWithObject(this, this.mOriginatorIdentity, methodName, retVal, args);
        }

        private void logVoidReturn(String methodName, Object... args) {
            SoundTriggerMiddlewareLogging.this.logVoidReturnWithObject(this, this.mOriginatorIdentity, methodName, args);
        }

        /* JADX INFO: Access modifiers changed from: private */
        /* loaded from: classes2.dex */
        public class CallbackLogging implements ISoundTriggerCallback {
            private final ISoundTriggerCallback mCallbackDelegate;

            private CallbackLogging(ISoundTriggerCallback delegate) {
                this.mCallbackDelegate = delegate;
            }

            public void onRecognition(int modelHandle, RecognitionEvent event, int captureSession) throws RemoteException {
                try {
                    this.mCallbackDelegate.onRecognition(modelHandle, event, captureSession);
                    logVoidReturn("onRecognition", Integer.valueOf(modelHandle), event);
                } catch (Exception e) {
                    logException("onRecognition", e, Integer.valueOf(modelHandle), event);
                    throw e;
                }
            }

            public void onPhraseRecognition(int modelHandle, PhraseRecognitionEvent event, int captureSession) throws RemoteException {
                try {
                    this.mCallbackDelegate.onPhraseRecognition(modelHandle, event, captureSession);
                    logVoidReturn("onPhraseRecognition", Integer.valueOf(modelHandle), event);
                } catch (Exception e) {
                    logException("onPhraseRecognition", e, Integer.valueOf(modelHandle), event);
                    throw e;
                }
            }

            public void onModelUnloaded(int modelHandle) throws RemoteException {
                try {
                    this.mCallbackDelegate.onModelUnloaded(modelHandle);
                    logVoidReturn("onModelUnloaded", Integer.valueOf(modelHandle));
                } catch (Exception e) {
                    logException("onModelUnloaded", e, Integer.valueOf(modelHandle));
                    throw e;
                }
            }

            public void onResourcesAvailable() throws RemoteException {
                try {
                    this.mCallbackDelegate.onResourcesAvailable();
                    logVoidReturn("onResourcesAvailable", new Object[0]);
                } catch (Exception e) {
                    logException("onResourcesAvailable", e, new Object[0]);
                    throw e;
                }
            }

            public void onModuleDied() throws RemoteException {
                try {
                    this.mCallbackDelegate.onModuleDied();
                    logVoidReturn("onModuleDied", new Object[0]);
                } catch (Exception e) {
                    logException("onModuleDied", e, new Object[0]);
                    throw e;
                }
            }

            private void logException(String methodName, Exception ex, Object... args) {
                SoundTriggerMiddlewareLogging.this.logExceptionWithObject(this, ModuleLogging.this.mOriginatorIdentity, methodName, ex, args);
            }

            private void logVoidReturn(String methodName, Object... args) {
                SoundTriggerMiddlewareLogging.this.logVoidReturnWithObject(this, ModuleLogging.this.mOriginatorIdentity, methodName, args);
            }

            public IBinder asBinder() {
                return this.mCallbackDelegate.asBinder();
            }

            public String toString() {
                return Objects.toString(this.mCallbackDelegate);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static class Event {
        public final String message;
        public final long timestamp;

        private Event(String message) {
            this.timestamp = System.currentTimeMillis();
            this.message = message;
        }
    }

    private static String printArgs(Object[] args) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            if (i > 0) {
                result.append(", ");
            }
            printObject(result, args[i]);
        }
        return result.toString();
    }

    private static void printObject(StringBuilder builder, Object obj) {
        ObjectPrinter.print(builder, obj, 16);
    }

    private static String printObject(Object obj) {
        StringBuilder builder = new StringBuilder();
        printObject(builder, obj);
        return builder.toString();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void logReturnWithObject(Object object, Identity originatorIdentity, String methodName, Object retVal, Object[] args) {
        String message = String.format("%s[this=%s, client=%s](%s) -> %s", methodName, object, printObject(originatorIdentity), printArgs(args), printObject(retVal));
        Log.i(TAG, message);
        appendMessage(message);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void logVoidReturnWithObject(Object object, Identity originatorIdentity, String methodName, Object[] args) {
        String message = String.format("%s[this=%s, client=%s](%s)", methodName, object, printObject(originatorIdentity), printArgs(args));
        Log.i(TAG, message);
        appendMessage(message);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void logExceptionWithObject(Object object, Identity originatorIdentity, String methodName, Exception ex, Object[] args) {
        String message = String.format("%s[this=%s, client=%s](%s) threw", methodName, object, printObject(originatorIdentity), printArgs(args));
        Log.e(TAG, message, ex);
        appendMessage(message + " " + ex.toString());
    }

    private void appendMessage(String message) {
        Event event = new Event(message);
        synchronized (this.mLastEvents) {
            if (this.mLastEvents.size() > 64) {
                this.mLastEvents.remove();
            }
            this.mLastEvents.add(event);
        }
    }

    @Override // com.android.server.soundtrigger_middleware.Dumpable
    public void dump(PrintWriter pw) {
        pw.println();
        pw.println("=========================================");
        pw.println("Last events");
        pw.println("=========================================");
        synchronized (this.mLastEvents) {
            Iterator<Event> it = this.mLastEvents.iterator();
            while (it.hasNext()) {
                Event event = it.next();
                pw.print(DATE_FORMAT.format(new Date(event.timestamp)));
                pw.print('\t');
                pw.println(event.message);
            }
        }
        pw.println();
        ISoundTriggerMiddlewareInternal iSoundTriggerMiddlewareInternal = this.mDelegate;
        if (iSoundTriggerMiddlewareInternal instanceof Dumpable) {
            ((Dumpable) iSoundTriggerMiddlewareInternal).dump(pw);
        }
    }
}
