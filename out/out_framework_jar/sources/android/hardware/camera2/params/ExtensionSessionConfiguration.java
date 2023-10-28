package android.hardware.camera2.params;

import android.hardware.camera2.CameraExtensionSession;
import java.util.List;
import java.util.concurrent.Executor;
/* loaded from: classes.dex */
public final class ExtensionSessionConfiguration {
    private static final String TAG = "ExtensionSessionConfiguration";
    private CameraExtensionSession.StateCallback mCallback;
    private Executor mExecutor;
    private int mExtensionType;
    private List<OutputConfiguration> mOutputs;

    public ExtensionSessionConfiguration(int extension, List<OutputConfiguration> outputs, Executor executor, CameraExtensionSession.StateCallback listener) {
        this.mExecutor = null;
        this.mCallback = null;
        this.mExtensionType = extension;
        this.mOutputs = outputs;
        this.mExecutor = executor;
        this.mCallback = listener;
    }

    public int getExtension() {
        return this.mExtensionType;
    }

    public List<OutputConfiguration> getOutputConfigurations() {
        return this.mOutputs;
    }

    public CameraExtensionSession.StateCallback getStateCallback() {
        return this.mCallback;
    }

    public Executor getExecutor() {
        return this.mExecutor;
    }
}
