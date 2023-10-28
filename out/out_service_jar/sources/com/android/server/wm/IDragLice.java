package com.android.server.wm;

import android.content.ClipData;
import android.os.IBinder;
import com.android.internal.view.IDragAndDropPermissions;
import com.android.server.wm.IDragLice;
import com.transsion.annotation.OSBridge;
import com.transsion.lice.LiceInfo;
import java.util.function.Supplier;
@OSBridge(client = OSBridge.Client.LICE_INTERFACE_SERVICES)
/* loaded from: classes2.dex */
public interface IDragLice {
    public static final LiceInfo<IDragLice> sLiceInfo = new LiceInfo<>("com.transsion.server.wm.DragLice", IDragLice.class, new Supplier() { // from class: com.android.server.wm.IDragLice$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new IDragLice.DefaultImpl();
        }
    });

    /* loaded from: classes2.dex */
    public static class DefaultImpl implements IDragLice {
    }

    /* loaded from: classes2.dex */
    public interface ITrDragAndDropPermissions {
        IDragAndDropPermissions getIDragAndDropPermissions(String str);
    }

    /* loaded from: classes2.dex */
    public interface IUpdateClipDataCallback {
        void updateClipData(ClipData clipData);
    }

    static IDragLice Instance() {
        return (IDragLice) sLiceInfo.getImpl();
    }

    default boolean isSupportedUpdateClipData(int flags) {
        return false;
    }

    default IBinder updateClipData(int flags, ClipData data, IUpdateClipDataCallback callback) {
        return null;
    }

    default void handleDragAndDropPermissions(IDragAndDropPermissions iDragAndDropPermissions, WindowState windowState, ITrDragAndDropPermissions iTrDragAndDropPermissions) {
    }

    default void reportDropWindowLockException(WindowState state) {
    }

    default void reportDropStatus(int dragStatus) {
    }
}
