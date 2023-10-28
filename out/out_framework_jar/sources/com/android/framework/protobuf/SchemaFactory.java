package com.android.framework.protobuf;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes4.dex */
public interface SchemaFactory {
    <T> Schema<T> createSchema(Class<T> cls);
}
