package com.transsion.lice;

import android.util.Log;
import com.transsion.annotation.OSBridge;
import java.lang.reflect.Method;
import java.util.function.Supplier;
@OSBridge(client = OSBridge.Client.LICE_CORE_FRAMEWORK)
/* loaded from: classes4.dex */
public class LiceInfo<I> {
    private static boolean DEBUG = true;
    private static final String TAG = "os.LiceInfo";
    private final Supplier<I> mFuncDefault;
    private volatile I mImpl = null;
    private final String mImplClassName;
    private final Class<I> mInterfaceClass;

    public LiceInfo(String implClassName, Class<I> interfaceClass, Supplier<I> funcDefault) {
        this.mInterfaceClass = interfaceClass;
        this.mImplClassName = implClassName;
        this.mFuncDefault = funcDefault;
    }

    public I getImpl() {
        if (this.mImpl != null) {
            return this.mImpl;
        }
        synchronized (this.mInterfaceClass) {
            if (this.mImpl != null) {
                return this.mImpl;
            }
            Class<I> cls = this.mInterfaceClass;
            Class<?> classFactory = Class.forName(this.mImplClassName, true, cls.getClassLoader());
            Method methodInstance = classFactory.getDeclaredMethod("instance", new Class[0]);
            I i = (I) methodInstance.invoke(classFactory, new Object[0]);
            if (this.mInterfaceClass.isInstance(i)) {
                this.mImpl = i;
                if (DEBUG) {
                    Log.d(TAG, "instance successfully. " + i + " from " + cls.getName());
                }
            } else {
                this.mImpl = this.mFuncDefault.get();
                Log.d(TAG, "using " + this.mImpl + " instead of " + (i != null ? i : this.mImplClassName) + " from " + cls.getName());
            }
            return this.mImpl;
        }
    }
}
