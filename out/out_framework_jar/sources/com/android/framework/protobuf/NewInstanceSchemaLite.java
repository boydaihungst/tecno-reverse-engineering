package com.android.framework.protobuf;

import com.android.framework.protobuf.GeneratedMessageLite;
/* loaded from: classes4.dex */
final class NewInstanceSchemaLite implements NewInstanceSchema {
    @Override // com.android.framework.protobuf.NewInstanceSchema
    public Object newInstance(Object defaultInstance) {
        return ((GeneratedMessageLite) defaultInstance).dynamicMethod(GeneratedMessageLite.MethodToInvoke.NEW_MUTABLE_INSTANCE);
    }
}
