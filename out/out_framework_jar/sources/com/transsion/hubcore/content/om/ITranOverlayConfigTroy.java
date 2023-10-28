package com.transsion.hubcore.content.om;

import com.android.internal.content.om.OverlayConfig;
import java.util.ArrayList;
/* loaded from: classes4.dex */
public interface ITranOverlayConfigTroy {
    String[] createIdmap(String str, String[] strArr, String[] strArr2, boolean z);

    OverlayConfig.IdmapInvocation getIdmapInvocation(boolean z, String str);

    ArrayList<OverlayConfig.Configuration> getSortedOverlays();
}
