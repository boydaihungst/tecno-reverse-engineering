package com.android.server.soundtrigger_middleware;

import android.content.Context;
import android.media.permission.ClearCallingIdentityContext;
import android.media.permission.Identity;
import android.media.permission.PermissionUtil;
import android.media.permission.SafeCloseable;
import android.media.soundtrigger.ModelParameterRange;
import android.media.soundtrigger.PhraseSoundModel;
import android.media.soundtrigger.RecognitionConfig;
import android.media.soundtrigger.SoundModel;
import android.media.soundtrigger_middleware.ISoundTriggerCallback;
import android.media.soundtrigger_middleware.ISoundTriggerMiddlewareService;
import android.media.soundtrigger_middleware.ISoundTriggerModule;
import android.media.soundtrigger_middleware.SoundTriggerModuleDescriptor;
import android.os.RemoteException;
import com.android.server.SystemService;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Objects;
/* loaded from: classes2.dex */
public class SoundTriggerMiddlewareService extends ISoundTriggerMiddlewareService.Stub {
    private static final String TAG = "SoundTriggerMiddlewareService";
    private final Context mContext;
    private final ISoundTriggerMiddlewareInternal mDelegate;

    private SoundTriggerMiddlewareService(ISoundTriggerMiddlewareInternal delegate, Context context) {
        this.mDelegate = (ISoundTriggerMiddlewareInternal) Objects.requireNonNull(delegate);
        this.mContext = context;
    }

    public SoundTriggerModuleDescriptor[] listModulesAsOriginator(Identity identity) {
        SafeCloseable ignored = establishIdentityDirect(identity);
        try {
            SoundTriggerModuleDescriptor[] listModules = this.mDelegate.listModules();
            if (ignored != null) {
                ignored.close();
            }
            return listModules;
        } catch (Throwable th) {
            if (ignored != null) {
                try {
                    ignored.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
            }
            throw th;
        }
    }

    public SoundTriggerModuleDescriptor[] listModulesAsMiddleman(Identity middlemanIdentity, Identity originatorIdentity) {
        SafeCloseable ignored = establishIdentityIndirect(middlemanIdentity, originatorIdentity);
        try {
            SoundTriggerModuleDescriptor[] listModules = this.mDelegate.listModules();
            if (ignored != null) {
                ignored.close();
            }
            return listModules;
        } catch (Throwable th) {
            if (ignored != null) {
                try {
                    ignored.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
            }
            throw th;
        }
    }

    public ISoundTriggerModule attachAsOriginator(int handle, Identity identity, ISoundTriggerCallback callback) {
        SafeCloseable ignored = establishIdentityDirect((Identity) Objects.requireNonNull(identity));
        try {
            ModuleService moduleService = new ModuleService(this.mDelegate.attach(handle, callback));
            if (ignored != null) {
                ignored.close();
            }
            return moduleService;
        } catch (Throwable th) {
            if (ignored != null) {
                try {
                    ignored.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
            }
            throw th;
        }
    }

    public ISoundTriggerModule attachAsMiddleman(int handle, Identity middlemanIdentity, Identity originatorIdentity, ISoundTriggerCallback callback) {
        SafeCloseable ignored = establishIdentityIndirect((Identity) Objects.requireNonNull(middlemanIdentity), (Identity) Objects.requireNonNull(originatorIdentity));
        try {
            ModuleService moduleService = new ModuleService(this.mDelegate.attach(handle, callback));
            if (ignored != null) {
                ignored.close();
            }
            return moduleService;
        } catch (Throwable th) {
            if (ignored != null) {
                try {
                    ignored.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
            }
            throw th;
        }
    }

    protected void dump(FileDescriptor fd, PrintWriter fout, String[] args) {
        ISoundTriggerMiddlewareInternal iSoundTriggerMiddlewareInternal = this.mDelegate;
        if (iSoundTriggerMiddlewareInternal instanceof Dumpable) {
            ((Dumpable) iSoundTriggerMiddlewareInternal).dump(fout);
        }
    }

    private SafeCloseable establishIdentityIndirect(Identity middlemanIdentity, Identity originatorIdentity) {
        return PermissionUtil.establishIdentityIndirect(this.mContext, "android.permission.SOUNDTRIGGER_DELEGATE_IDENTITY", middlemanIdentity, originatorIdentity);
    }

    private SafeCloseable establishIdentityDirect(Identity originatorIdentity) {
        return PermissionUtil.establishIdentityDirect(originatorIdentity);
    }

    /* loaded from: classes2.dex */
    private static final class ModuleService extends ISoundTriggerModule.Stub {
        private final ISoundTriggerModule mDelegate;

        private ModuleService(ISoundTriggerModule delegate) {
            this.mDelegate = delegate;
        }

        public int loadModel(SoundModel model) throws RemoteException {
            SafeCloseable ignored = ClearCallingIdentityContext.create();
            try {
                int loadModel = this.mDelegate.loadModel(model);
                if (ignored != null) {
                    ignored.close();
                }
                return loadModel;
            } catch (Throwable th) {
                if (ignored != null) {
                    try {
                        ignored.close();
                    } catch (Throwable th2) {
                        th.addSuppressed(th2);
                    }
                }
                throw th;
            }
        }

        public int loadPhraseModel(PhraseSoundModel model) throws RemoteException {
            SafeCloseable ignored = ClearCallingIdentityContext.create();
            try {
                int loadPhraseModel = this.mDelegate.loadPhraseModel(model);
                if (ignored != null) {
                    ignored.close();
                }
                return loadPhraseModel;
            } catch (Throwable th) {
                if (ignored != null) {
                    try {
                        ignored.close();
                    } catch (Throwable th2) {
                        th.addSuppressed(th2);
                    }
                }
                throw th;
            }
        }

        public void unloadModel(int modelHandle) throws RemoteException {
            SafeCloseable ignored = ClearCallingIdentityContext.create();
            try {
                this.mDelegate.unloadModel(modelHandle);
                if (ignored != null) {
                    ignored.close();
                }
            } catch (Throwable th) {
                if (ignored != null) {
                    try {
                        ignored.close();
                    } catch (Throwable th2) {
                        th.addSuppressed(th2);
                    }
                }
                throw th;
            }
        }

        public void startRecognition(int modelHandle, RecognitionConfig config) throws RemoteException {
            SafeCloseable ignored = ClearCallingIdentityContext.create();
            try {
                this.mDelegate.startRecognition(modelHandle, config);
                if (ignored != null) {
                    ignored.close();
                }
            } catch (Throwable th) {
                if (ignored != null) {
                    try {
                        ignored.close();
                    } catch (Throwable th2) {
                        th.addSuppressed(th2);
                    }
                }
                throw th;
            }
        }

        public void stopRecognition(int modelHandle) throws RemoteException {
            SafeCloseable ignored = ClearCallingIdentityContext.create();
            try {
                this.mDelegate.stopRecognition(modelHandle);
                if (ignored != null) {
                    ignored.close();
                }
            } catch (Throwable th) {
                if (ignored != null) {
                    try {
                        ignored.close();
                    } catch (Throwable th2) {
                        th.addSuppressed(th2);
                    }
                }
                throw th;
            }
        }

        public void forceRecognitionEvent(int modelHandle) throws RemoteException {
            SafeCloseable ignored = ClearCallingIdentityContext.create();
            try {
                this.mDelegate.forceRecognitionEvent(modelHandle);
                if (ignored != null) {
                    ignored.close();
                }
            } catch (Throwable th) {
                if (ignored != null) {
                    try {
                        ignored.close();
                    } catch (Throwable th2) {
                        th.addSuppressed(th2);
                    }
                }
                throw th;
            }
        }

        public void setModelParameter(int modelHandle, int modelParam, int value) throws RemoteException {
            SafeCloseable ignored = ClearCallingIdentityContext.create();
            try {
                this.mDelegate.setModelParameter(modelHandle, modelParam, value);
                if (ignored != null) {
                    ignored.close();
                }
            } catch (Throwable th) {
                if (ignored != null) {
                    try {
                        ignored.close();
                    } catch (Throwable th2) {
                        th.addSuppressed(th2);
                    }
                }
                throw th;
            }
        }

        public int getModelParameter(int modelHandle, int modelParam) throws RemoteException {
            SafeCloseable ignored = ClearCallingIdentityContext.create();
            try {
                int modelParameter = this.mDelegate.getModelParameter(modelHandle, modelParam);
                if (ignored != null) {
                    ignored.close();
                }
                return modelParameter;
            } catch (Throwable th) {
                if (ignored != null) {
                    try {
                        ignored.close();
                    } catch (Throwable th2) {
                        th.addSuppressed(th2);
                    }
                }
                throw th;
            }
        }

        public ModelParameterRange queryModelParameterSupport(int modelHandle, int modelParam) throws RemoteException {
            SafeCloseable ignored = ClearCallingIdentityContext.create();
            try {
                ModelParameterRange queryModelParameterSupport = this.mDelegate.queryModelParameterSupport(modelHandle, modelParam);
                if (ignored != null) {
                    ignored.close();
                }
                return queryModelParameterSupport;
            } catch (Throwable th) {
                if (ignored != null) {
                    try {
                        ignored.close();
                    } catch (Throwable th2) {
                        th.addSuppressed(th2);
                    }
                }
                throw th;
            }
        }

        public void detach() throws RemoteException {
            SafeCloseable ignored = ClearCallingIdentityContext.create();
            try {
                this.mDelegate.detach();
                if (ignored != null) {
                    ignored.close();
                }
            } catch (Throwable th) {
                if (ignored != null) {
                    try {
                        ignored.close();
                    } catch (Throwable th2) {
                        th.addSuppressed(th2);
                    }
                }
                throw th;
            }
        }
    }

    /* loaded from: classes2.dex */
    public static final class Lifecycle extends SystemService {
        public Lifecycle(Context context) {
            super(context);
        }

        @Override // com.android.server.SystemService
        public void onStart() {
            HalFactory[] factories = {new DefaultHalFactory()};
            publishBinderService("soundtrigger_middleware", new SoundTriggerMiddlewareService(new SoundTriggerMiddlewareLogging(new SoundTriggerMiddlewarePermission(new SoundTriggerMiddlewareValidation(new SoundTriggerMiddlewareImpl(factories, new AudioSessionProviderImpl())), getContext())), getContext()));
        }
    }
}
