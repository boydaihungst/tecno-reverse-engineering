package com.android.server.wm;

import com.android.server.policy.WindowManagerPolicy;
import com.android.server.wm.DisplayArea;
import com.android.server.wm.DisplayAreaPolicyBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class RootDisplayArea extends DisplayArea.Dimmable {
    private DisplayArea.Tokens[] mAreaForLayer;
    Map<DisplayAreaPolicyBuilder.Feature, List<DisplayArea<WindowContainer>>> mFeatureToDisplayAreas;
    List<DisplayAreaPolicyBuilder.Feature> mFeatures;
    private boolean mHasBuiltHierarchy;

    /* JADX INFO: Access modifiers changed from: package-private */
    public RootDisplayArea(WindowManagerService wms, String name, int featureId) {
        super(wms, DisplayArea.Type.ANY, name, featureId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.wm.WindowContainer
    public RootDisplayArea getRootDisplayArea() {
        return this;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.wm.WindowContainer
    public RootDisplayArea asRootDisplayArea() {
        return this;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isOrientationDifferentFromDisplay() {
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void placeImeContainer(DisplayArea.Tokens imeContainer) {
        RootDisplayArea previousRoot = imeContainer.getRootDisplayArea();
        List<DisplayAreaPolicyBuilder.Feature> features = this.mFeatures;
        for (int i = 0; i < features.size(); i++) {
            DisplayAreaPolicyBuilder.Feature feature = features.get(i);
            if (feature.getId() == 7) {
                List<DisplayArea<WindowContainer>> imeDisplayAreas = this.mFeatureToDisplayAreas.get(feature);
                if (imeDisplayAreas.size() != 1) {
                    throw new IllegalStateException("There must be exactly one DisplayArea for the FEATURE_IME_PLACEHOLDER");
                }
                previousRoot.updateImeContainerForLayers(null);
                imeContainer.reparent(imeDisplayAreas.get(0), Integer.MAX_VALUE);
                updateImeContainerForLayers(imeContainer);
                return;
            }
        }
        throw new IllegalStateException("There is no FEATURE_IME_PLACEHOLDER in this root to place the IME container");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public DisplayArea.Tokens findAreaForTokenInLayer(WindowToken token) {
        return findAreaForWindowTypeInLayer(token.windowType, token.mOwnerCanManageAppTokens, token.mRoundedCornerOverlay);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public DisplayArea.Tokens findAreaForWindowTypeInLayer(int windowType, boolean ownerCanManageAppTokens, boolean roundedCornerOverlay) {
        int windowLayerFromType = this.mWmService.mPolicy.getWindowLayerFromTypeLw(windowType, ownerCanManageAppTokens, roundedCornerOverlay);
        if (windowLayerFromType == 2) {
            throw new IllegalArgumentException("There shouldn't be WindowToken on APPLICATION_LAYER");
        }
        return this.mAreaForLayer[windowLayerFromType];
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onHierarchyBuilt(ArrayList<DisplayAreaPolicyBuilder.Feature> features, DisplayArea.Tokens[] areaForLayer, Map<DisplayAreaPolicyBuilder.Feature, List<DisplayArea<WindowContainer>>> featureToDisplayAreas) {
        if (this.mHasBuiltHierarchy) {
            throw new IllegalStateException("Root should only build the hierarchy once");
        }
        this.mHasBuiltHierarchy = true;
        this.mFeatures = Collections.unmodifiableList(features);
        this.mAreaForLayer = areaForLayer;
        this.mFeatureToDisplayAreas = featureToDisplayAreas;
    }

    private void updateImeContainerForLayers(DisplayArea.Tokens imeContainer) {
        WindowManagerPolicy policy = this.mWmService.mPolicy;
        this.mAreaForLayer[policy.getWindowLayerFromTypeLw(2011)] = imeContainer;
        this.mAreaForLayer[policy.getWindowLayerFromTypeLw(2012)] = imeContainer;
    }
}
