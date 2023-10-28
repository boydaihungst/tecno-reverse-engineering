package com.transsion.hubcore.utils;

import android.util.Slog;
import dalvik.system.PathClassLoader;
import java.lang.reflect.Constructor;
import java.util.function.Supplier;
/* loaded from: classes4.dex */
public class TranClassInfo<I> {
    private static boolean DEBUG = true;
    private static final String TAG = "TranClassInfo";
    private volatile I mDefault;
    private final Supplier<I> mFuncDefault;
    private volatile I mImpl = null;
    private final String mImplClassName;
    private final Class<I> mInterfaceClass;

    public TranClassInfo(String implClassName, Class<I> interfaceClass, Supplier<I> funcDefault) {
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
            if (TranDynamicFeatureSwitch.isFeatureOpen(this.mInterfaceClass.getName())) {
                Class<I> cls = this.mInterfaceClass;
                Class<?> classFactory = Class.forName(this.mImplClassName, true, cls.getClassLoader());
                Constructor constructor = classFactory.getDeclaredConstructor(new Class[0]);
                constructor.setAccessible(true);
                I i = (I) constructor.newInstance(new Object[0]);
                if (this.mInterfaceClass.isInstance(i)) {
                    this.mImpl = i;
                    if (DEBUG) {
                        Slog.d(TAG, "instance successfully. " + i + " from " + cls.getName());
                    }
                } else {
                    this.mImpl = this.mFuncDefault.get();
                    Slog.d(TAG, "using " + this.mImpl + " instead of " + (i != null ? i : this.mImplClassName) + " from " + cls.getName());
                }
            } else {
                this.mImpl = this.mFuncDefault.get();
                Slog.d(TAG, "[THUB] Feature: " + TranDynamicFeatureSwitch.getFeatureName(this.mInterfaceClass.getName()) + " is disabled!");
            }
            return this.mImpl;
        }
    }

    public I getImpl(String classPackage) {
        if (this.mImpl != null) {
            return this.mImpl;
        }
        synchronized (this.mInterfaceClass) {
            if (this.mImpl != null) {
                return this.mImpl;
            }
            if (TranDynamicFeatureSwitch.isFeatureOpen(this.mInterfaceClass.getName())) {
                Class<I> cls = this.mInterfaceClass;
                PathClassLoader classLoader = new PathClassLoader(classPackage, cls.getClassLoader());
                Class<?> classFactory = Class.forName(this.mImplClassName, false, classLoader);
                Constructor constructor = classFactory.getDeclaredConstructor(new Class[0]);
                constructor.setAccessible(true);
                I i = (I) constructor.newInstance(new Object[0]);
                if (this.mInterfaceClass.isInstance(i)) {
                    this.mImpl = i;
                    if (DEBUG) {
                        Slog.d(TAG, "instance successfully. " + i + " from " + cls.getName());
                    }
                } else {
                    this.mImpl = this.mFuncDefault.get();
                    Slog.d(TAG, "using " + this.mImpl + " instead of " + (i != null ? i : this.mImplClassName) + " from " + cls.getName());
                }
            } else {
                this.mImpl = this.mFuncDefault.get();
                Slog.d(TAG, "[THUB] Feature: " + TranDynamicFeatureSwitch.getFeatureName(this.mInterfaceClass.getName()) + " is disabled!");
            }
            return this.mImpl;
        }
    }

    public I getDefault() {
        if (this.mDefault == null) {
            this.mDefault = this.mFuncDefault.get();
        }
        return this.mDefault;
    }
}
