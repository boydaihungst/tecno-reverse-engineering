package com.android.framework.protobuf;

import com.android.framework.protobuf.MapEntryLite;
import java.util.Map;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes4.dex */
public interface MapFieldSchema {
    Map<?, ?> forMapData(Object obj);

    MapEntryLite.Metadata<?, ?> forMapMetadata(Object obj);

    Map<?, ?> forMutableMapData(Object obj);

    int getSerializedSize(int i, Object obj, Object obj2);

    boolean isImmutable(Object obj);

    Object mergeFrom(Object obj, Object obj2);

    Object newMapField(Object obj);

    Object toImmutable(Object obj);
}
