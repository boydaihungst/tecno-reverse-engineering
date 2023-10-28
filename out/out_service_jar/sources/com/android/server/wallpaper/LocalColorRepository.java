package com.android.server.wallpaper;

import android.app.ILocalWallpaperColorConsumer;
import android.graphics.RectF;
import android.os.IBinder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.SparseArray;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
/* loaded from: classes2.dex */
public class LocalColorRepository {
    ArrayMap<IBinder, SparseArray<ArraySet<RectF>>> mLocalColorAreas = new ArrayMap<>();
    RemoteCallbackList<ILocalWallpaperColorConsumer> mCallbacks = new RemoteCallbackList<>();

    public void addAreas(final ILocalWallpaperColorConsumer consumer, List<RectF> areas, int displayId) {
        IBinder binder = consumer.asBinder();
        SparseArray<ArraySet<RectF>> displays = this.mLocalColorAreas.get(binder);
        ArraySet<RectF> displayAreas = null;
        if (displays == null) {
            try {
                consumer.asBinder().linkToDeath(new IBinder.DeathRecipient() { // from class: com.android.server.wallpaper.LocalColorRepository$$ExternalSyntheticLambda0
                    @Override // android.os.IBinder.DeathRecipient
                    public final void binderDied() {
                        LocalColorRepository.this.m7661xe3c8e157(consumer);
                    }
                }, 0);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            displays = new SparseArray<>();
            this.mLocalColorAreas.put(binder, displays);
        } else {
            ArraySet<RectF> displayAreas2 = displays.get(displayId);
            displayAreas = displayAreas2;
        }
        if (displayAreas == null) {
            displayAreas = new ArraySet<>(areas);
            displays.put(displayId, displayAreas);
        }
        for (int i = 0; i < areas.size(); i++) {
            displayAreas.add(areas.get(i));
        }
        this.mCallbacks.register(consumer);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$addAreas$0$com-android-server-wallpaper-LocalColorRepository  reason: not valid java name */
    public /* synthetic */ void m7661xe3c8e157(ILocalWallpaperColorConsumer consumer) {
        this.mLocalColorAreas.remove(consumer.asBinder());
    }

    public List<RectF> removeAreas(ILocalWallpaperColorConsumer consumer, List<RectF> areas, int displayId) {
        IBinder binder = consumer.asBinder();
        SparseArray<ArraySet<RectF>> displays = this.mLocalColorAreas.get(binder);
        if (displays != null) {
            ArraySet<RectF> registeredAreas = displays.get(displayId);
            if (registeredAreas == null) {
                this.mCallbacks.unregister(consumer);
            } else {
                for (int i = 0; i < areas.size(); i++) {
                    registeredAreas.remove(areas.get(i));
                }
                int i2 = registeredAreas.size();
                if (i2 == 0) {
                    displays.remove(displayId);
                }
            }
            if (displays.size() == 0) {
                this.mLocalColorAreas.remove(binder);
                this.mCallbacks.unregister(consumer);
            }
        } else {
            this.mCallbacks.unregister(consumer);
        }
        ArraySet<RectF> purged = new ArraySet<>(areas);
        for (int i3 = 0; i3 < this.mLocalColorAreas.size(); i3++) {
            for (int j = 0; j < this.mLocalColorAreas.valueAt(i3).size(); j++) {
                for (int k = 0; k < this.mLocalColorAreas.valueAt(i3).valueAt(j).size(); k++) {
                    purged.remove(this.mLocalColorAreas.valueAt(i3).valueAt(j).valueAt(k));
                }
            }
        }
        return new ArrayList(purged);
    }

    public List<RectF> getAreasByDisplayId(int displayId) {
        ArraySet<RectF> displayAreas;
        ArrayList<RectF> areas = new ArrayList<>();
        for (int i = 0; i < this.mLocalColorAreas.size(); i++) {
            SparseArray<ArraySet<RectF>> displays = this.mLocalColorAreas.valueAt(i);
            if (displays != null && (displayAreas = displays.get(displayId)) != null) {
                for (int j = 0; j < displayAreas.size(); j++) {
                    areas.add(displayAreas.valueAt(j));
                }
            }
        }
        return areas;
    }

    public void forEachCallback(final Consumer<ILocalWallpaperColorConsumer> callback, final RectF area, final int displayId) {
        this.mCallbacks.broadcast(new Consumer() { // from class: com.android.server.wallpaper.LocalColorRepository$$ExternalSyntheticLambda1
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                LocalColorRepository.this.m7662x65493368(displayId, area, callback, (ILocalWallpaperColorConsumer) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$forEachCallback$1$com-android-server-wallpaper-LocalColorRepository  reason: not valid java name */
    public /* synthetic */ void m7662x65493368(int displayId, RectF area, Consumer callback, ILocalWallpaperColorConsumer cb) {
        ArraySet<RectF> displayAreas;
        IBinder binder = cb.asBinder();
        SparseArray<ArraySet<RectF>> displays = this.mLocalColorAreas.get(binder);
        if (displays != null && (displayAreas = displays.get(displayId)) != null && displayAreas.contains(area)) {
            callback.accept(cb);
        }
    }

    protected boolean isCallbackAvailable(ILocalWallpaperColorConsumer callback) {
        return this.mLocalColorAreas.get(callback.asBinder()) != null;
    }
}
