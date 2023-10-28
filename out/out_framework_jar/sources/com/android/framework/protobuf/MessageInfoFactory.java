package com.android.framework.protobuf;
/* loaded from: classes4.dex */
interface MessageInfoFactory {
    boolean isSupported(Class<?> cls);

    MessageInfo messageInfoFor(Class<?> cls);
}
