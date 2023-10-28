package com.android.server.pm;

import android.content.pm.SharedLibraryInfo;
import android.util.proto.ProtoOutputStream;
import com.android.server.utils.WatchedArrayMap;
import com.android.server.utils.WatchedLongSparseArray;
import java.io.PrintWriter;
/* loaded from: classes2.dex */
public interface SharedLibrariesRead {
    void dump(PrintWriter printWriter, DumpState dumpState);

    void dumpProto(ProtoOutputStream protoOutputStream);

    WatchedArrayMap<String, WatchedLongSparseArray<SharedLibraryInfo>> getAll();

    SharedLibraryInfo getSharedLibraryInfo(String str, long j);

    WatchedLongSparseArray<SharedLibraryInfo> getSharedLibraryInfos(String str);

    WatchedLongSparseArray<SharedLibraryInfo> getStaticLibraryInfos(String str);
}
