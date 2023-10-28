package com.transsion.hubcore.window;

import android.window.DisplayAreaAppearedInfo;
import android.window.IDisplayAreaOrganizer;
import android.window.IDisplayAreaOrganizerController;
import com.transsion.hubcore.utils.TranClassInfo;
import java.util.List;
import java.util.function.Supplier;
/* loaded from: classes4.dex */
public interface ITranDisplayAreaOrganizer {
    public static final TranClassInfo<ITranDisplayAreaOrganizer> classInfo = new TranClassInfo<>("com.transsion.hubcore.window.TranDisplayAreaOrganizerImpl", ITranDisplayAreaOrganizer.class, new Supplier() { // from class: com.transsion.hubcore.window.ITranDisplayAreaOrganizer$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return ITranDisplayAreaOrganizer.lambda$static$0();
        }
    });

    static /* synthetic */ ITranDisplayAreaOrganizer lambda$static$0() {
        return new ITranDisplayAreaOrganizer() { // from class: com.transsion.hubcore.window.ITranDisplayAreaOrganizer.1
        };
    }

    static ITranDisplayAreaOrganizer Instance() {
        return classInfo.getImpl();
    }

    default DisplayAreaAppearedInfo createMultiWindowTaskDisplayAreaV3(IDisplayAreaOrganizerController displayAreaOrganizerController, IDisplayAreaOrganizer mInterface, int displayId, int parentFeatureId, String name, int multiWindowState) {
        return null;
    }

    default DisplayAreaAppearedInfo createMultiWindowTaskDisplayAreaV4(IDisplayAreaOrganizerController displayAreaOrganizerController, IDisplayAreaOrganizer mInterface, int displayId, int parentFeatureId, String name) {
        return null;
    }

    default List<DisplayAreaAppearedInfo> registerImeOrganizer(IDisplayAreaOrganizerController displayAreaOrganizerController, IDisplayAreaOrganizer mInterface, int displayAreaFeature, boolean dettachImeWithActivity) {
        return null;
    }
}
