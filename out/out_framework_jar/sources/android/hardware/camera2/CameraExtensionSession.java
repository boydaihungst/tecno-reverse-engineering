package android.hardware.camera2;

import java.util.concurrent.Executor;
/* loaded from: classes.dex */
public abstract class CameraExtensionSession implements AutoCloseable {

    /* loaded from: classes.dex */
    public static abstract class ExtensionCaptureCallback {
        public void onCaptureStarted(CameraExtensionSession session, CaptureRequest request, long timestamp) {
        }

        public void onCaptureProcessStarted(CameraExtensionSession session, CaptureRequest request) {
        }

        public void onCaptureFailed(CameraExtensionSession session, CaptureRequest request) {
        }

        public void onCaptureSequenceCompleted(CameraExtensionSession session, int sequenceId) {
        }

        public void onCaptureSequenceAborted(CameraExtensionSession session, int sequenceId) {
        }

        public void onCaptureResultAvailable(CameraExtensionSession session, CaptureRequest request, TotalCaptureResult result) {
        }
    }

    /* loaded from: classes.dex */
    public static abstract class StateCallback {
        public abstract void onConfigureFailed(CameraExtensionSession cameraExtensionSession);

        public abstract void onConfigured(CameraExtensionSession cameraExtensionSession);

        public void onClosed(CameraExtensionSession session) {
        }
    }

    public CameraDevice getDevice() {
        throw new UnsupportedOperationException("Subclasses must override this method");
    }

    public int capture(CaptureRequest request, Executor executor, ExtensionCaptureCallback listener) throws CameraAccessException {
        throw new UnsupportedOperationException("Subclasses must override this method");
    }

    public int setRepeatingRequest(CaptureRequest request, Executor executor, ExtensionCaptureCallback listener) throws CameraAccessException {
        throw new UnsupportedOperationException("Subclasses must override this method");
    }

    public void stopRepeating() throws CameraAccessException {
        throw new UnsupportedOperationException("Subclasses must override this method");
    }

    @Override // java.lang.AutoCloseable
    public void close() throws CameraAccessException {
        throw new UnsupportedOperationException("Subclasses must override this method");
    }
}
