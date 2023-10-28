package com.android.server.wm;

import android.app.ActivityOptions;
import android.os.Bundle;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.window.WindowContainerToken;
import com.android.internal.protolog.ProtoLogGroup;
import com.android.internal.protolog.ProtoLogImpl;
import com.android.server.policy.WindowManagerPolicy;
import com.android.server.wm.DisplayArea;
import com.android.server.wm.DisplayAreaPolicyBuilder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.ToIntFunction;
/* loaded from: classes2.dex */
class DisplayAreaPolicyBuilder {
    private final ArrayList<HierarchyBuilder> mDisplayAreaGroupHierarchyBuilders = new ArrayList<>();
    private HierarchyBuilder mRootHierarchyBuilder;
    private BiFunction<Integer, Bundle, RootDisplayArea> mSelectRootForWindowFunc;
    private Function<Bundle, TaskDisplayArea> mSelectTaskDisplayAreaFunc;

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public interface NewDisplayAreaSupplier {
        DisplayArea create(WindowManagerService windowManagerService, DisplayArea.Type type, String str, int i);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public DisplayAreaPolicyBuilder setRootHierarchy(HierarchyBuilder rootHierarchyBuilder) {
        this.mRootHierarchyBuilder = rootHierarchyBuilder;
        return this;
    }

    DisplayAreaPolicyBuilder addDisplayAreaGroupHierarchy(HierarchyBuilder displayAreaGroupHierarchy) {
        this.mDisplayAreaGroupHierarchyBuilders.add(displayAreaGroupHierarchy);
        return this;
    }

    DisplayAreaPolicyBuilder setSelectRootForWindowFunc(BiFunction<Integer, Bundle, RootDisplayArea> selectRootForWindowFunc) {
        this.mSelectRootForWindowFunc = selectRootForWindowFunc;
        return this;
    }

    DisplayAreaPolicyBuilder setSelectTaskDisplayAreaFunc(Function<Bundle, TaskDisplayArea> selectTaskDisplayAreaFunc) {
        this.mSelectTaskDisplayAreaFunc = selectTaskDisplayAreaFunc;
        return this;
    }

    private void validate() {
        if (this.mRootHierarchyBuilder == null) {
            throw new IllegalStateException("Root must be set for the display area policy.");
        }
        Set<Integer> uniqueIdSet = new ArraySet<>();
        Set<Integer> allIdSet = new ArraySet<>();
        validateIds(this.mRootHierarchyBuilder, uniqueIdSet, allIdSet);
        boolean containsImeContainer = this.mRootHierarchyBuilder.mImeContainer != null;
        boolean containsDefaultTda = containsDefaultTaskDisplayArea(this.mRootHierarchyBuilder);
        for (int i = 0; i < this.mDisplayAreaGroupHierarchyBuilders.size(); i++) {
            HierarchyBuilder hierarchyBuilder = this.mDisplayAreaGroupHierarchyBuilders.get(i);
            validateIds(hierarchyBuilder, uniqueIdSet, allIdSet);
            if (hierarchyBuilder.mTaskDisplayAreas.isEmpty()) {
                throw new IllegalStateException("DisplayAreaGroup must contain at least one TaskDisplayArea.");
            }
            if (containsImeContainer) {
                if (hierarchyBuilder.mImeContainer != null) {
                    throw new IllegalStateException("Only one DisplayArea hierarchy can contain the IME container");
                }
            } else {
                containsImeContainer = hierarchyBuilder.mImeContainer != null;
            }
            if (containsDefaultTda) {
                if (containsDefaultTaskDisplayArea(hierarchyBuilder)) {
                    throw new IllegalStateException("Only one TaskDisplayArea can have the feature id of FEATURE_DEFAULT_TASK_CONTAINER");
                }
            } else {
                containsDefaultTda = containsDefaultTaskDisplayArea(hierarchyBuilder);
            }
        }
        if (!containsImeContainer) {
            throw new IllegalStateException("IME container must be set.");
        }
        if (!containsDefaultTda) {
            throw new IllegalStateException("There must be a default TaskDisplayArea with id of FEATURE_DEFAULT_TASK_CONTAINER.");
        }
    }

    private static boolean containsDefaultTaskDisplayArea(HierarchyBuilder displayAreaHierarchy) {
        for (int i = 0; i < displayAreaHierarchy.mTaskDisplayAreas.size(); i++) {
            if (((TaskDisplayArea) displayAreaHierarchy.mTaskDisplayAreas.get(i)).mFeatureId == 1) {
                return true;
            }
        }
        return false;
    }

    private static void validateIds(HierarchyBuilder displayAreaHierarchy, Set<Integer> uniqueIdSet, Set<Integer> allIdSet) {
        int rootId = displayAreaHierarchy.mRoot.mFeatureId;
        if (!allIdSet.add(Integer.valueOf(rootId)) || !uniqueIdSet.add(Integer.valueOf(rootId))) {
            throw new IllegalStateException("RootDisplayArea must have unique id, but id=" + rootId + " is not unique.");
        }
        if (rootId > 20001) {
            throw new IllegalStateException("RootDisplayArea should not have an id greater than FEATURE_VENDOR_LAST.");
        }
        for (int i = 0; i < displayAreaHierarchy.mTaskDisplayAreas.size(); i++) {
            int taskDisplayAreaId = ((TaskDisplayArea) displayAreaHierarchy.mTaskDisplayAreas.get(i)).mFeatureId;
            if (!allIdSet.add(Integer.valueOf(taskDisplayAreaId)) || !uniqueIdSet.add(Integer.valueOf(taskDisplayAreaId))) {
                throw new IllegalStateException("TaskDisplayArea must have unique id, but id=" + taskDisplayAreaId + " is not unique.");
            }
            if (taskDisplayAreaId > 20001) {
                throw new IllegalStateException("TaskDisplayArea declared in the policy should nothave an id greater than FEATURE_VENDOR_LAST.");
            }
        }
        Set<Integer> featureIdSet = new ArraySet<>();
        for (int i2 = 0; i2 < displayAreaHierarchy.mFeatures.size(); i2++) {
            int featureId = ((Feature) displayAreaHierarchy.mFeatures.get(i2)).getId();
            if (uniqueIdSet.contains(Integer.valueOf(featureId))) {
                throw new IllegalStateException("Feature must not have same id with any RootDisplayArea or TaskDisplayArea, but id=" + featureId + " is used");
            }
            if (!featureIdSet.add(Integer.valueOf(featureId))) {
                throw new IllegalStateException("Feature below the same root must have unique id, but id=" + featureId + " is not unique.");
            }
            if (featureId > 20001) {
                throw new IllegalStateException("Feature should not have an id greater than FEATURE_VENDOR_LAST.");
            }
        }
        allIdSet.addAll(featureIdSet);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Result build(WindowManagerService wmService) {
        validate();
        this.mRootHierarchyBuilder.build(this.mDisplayAreaGroupHierarchyBuilders);
        List<RootDisplayArea> displayAreaGroupRoots = new ArrayList<>(this.mDisplayAreaGroupHierarchyBuilders.size());
        for (int i = 0; i < this.mDisplayAreaGroupHierarchyBuilders.size(); i++) {
            HierarchyBuilder hierarchyBuilder = this.mDisplayAreaGroupHierarchyBuilders.get(i);
            hierarchyBuilder.build();
            displayAreaGroupRoots.add(hierarchyBuilder.mRoot);
        }
        if (this.mSelectRootForWindowFunc == null) {
            this.mSelectRootForWindowFunc = new DefaultSelectRootForWindowFunction(this.mRootHierarchyBuilder.mRoot, displayAreaGroupRoots);
        }
        return new Result(wmService, this.mRootHierarchyBuilder.mRoot, displayAreaGroupRoots, this.mSelectRootForWindowFunc, this.mSelectTaskDisplayAreaFunc);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static class DefaultSelectRootForWindowFunction implements BiFunction<Integer, Bundle, RootDisplayArea> {
        final List<RootDisplayArea> mDisplayAreaGroupRoots;
        final RootDisplayArea mDisplayRoot;

        DefaultSelectRootForWindowFunction(RootDisplayArea displayRoot, List<RootDisplayArea> displayAreaGroupRoots) {
            this.mDisplayRoot = displayRoot;
            this.mDisplayAreaGroupRoots = Collections.unmodifiableList(displayAreaGroupRoots);
        }

        /* JADX DEBUG: Method merged with bridge method */
        @Override // java.util.function.BiFunction
        public RootDisplayArea apply(Integer windowType, Bundle options) {
            if (this.mDisplayAreaGroupRoots.isEmpty()) {
                return this.mDisplayRoot;
            }
            if (options != null && options.containsKey("root_display_area_id")) {
                int rootId = options.getInt("root_display_area_id");
                if (this.mDisplayRoot.mFeatureId == rootId) {
                    return this.mDisplayRoot;
                }
                for (int i = this.mDisplayAreaGroupRoots.size() - 1; i >= 0; i--) {
                    if (this.mDisplayAreaGroupRoots.get(i).mFeatureId == rootId) {
                        return this.mDisplayAreaGroupRoots.get(i);
                    }
                }
            }
            return this.mDisplayRoot;
        }
    }

    /* loaded from: classes2.dex */
    private static class DefaultSelectTaskDisplayAreaFunction implements Function<Bundle, TaskDisplayArea> {
        private final TaskDisplayArea mDefaultTaskDisplayArea;
        private final int mDisplayId;

        DefaultSelectTaskDisplayAreaFunction(TaskDisplayArea defaultTaskDisplayArea) {
            this.mDefaultTaskDisplayArea = defaultTaskDisplayArea;
            this.mDisplayId = defaultTaskDisplayArea.getDisplayId();
        }

        /* JADX DEBUG: Method merged with bridge method */
        @Override // java.util.function.Function
        public TaskDisplayArea apply(Bundle options) {
            if (options == null) {
                return this.mDefaultTaskDisplayArea;
            }
            ActivityOptions activityOptions = new ActivityOptions(options);
            WindowContainerToken tdaToken = activityOptions.getLaunchTaskDisplayArea();
            if (tdaToken == null) {
                return this.mDefaultTaskDisplayArea;
            }
            TaskDisplayArea tda = WindowContainer.fromBinder(tdaToken.asBinder()).asTaskDisplayArea();
            if (tda == null) {
                if (ProtoLogCache.WM_DEBUG_WINDOW_ORGANIZER_enabled) {
                    String protoLogParam0 = String.valueOf(tdaToken);
                    ProtoLogImpl.w(ProtoLogGroup.WM_DEBUG_WINDOW_ORGANIZER, 1874559932, 0, (String) null, new Object[]{protoLogParam0});
                }
                return this.mDefaultTaskDisplayArea;
            } else if (tda.getDisplayId() != this.mDisplayId) {
                throw new IllegalArgumentException("The specified TaskDisplayArea must attach to Display#" + this.mDisplayId + ", but it is in Display#" + tda.getDisplayId());
            } else {
                return tda;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public static class HierarchyBuilder {
        private static final int LEAF_TYPE_IME_CONTAINERS = 2;
        private static final int LEAF_TYPE_TASK_CONTAINERS = 1;
        private static final int LEAF_TYPE_TOKENS = 0;
        private DisplayArea.Tokens mImeContainer;
        private final RootDisplayArea mRoot;
        private final ArrayList<Feature> mFeatures = new ArrayList<>();
        private final ArrayList<TaskDisplayArea> mTaskDisplayAreas = new ArrayList<>();

        /* JADX INFO: Access modifiers changed from: package-private */
        public HierarchyBuilder(RootDisplayArea root) {
            this.mRoot = root;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public HierarchyBuilder addFeature(Feature feature) {
            this.mFeatures.add(feature);
            return this;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public HierarchyBuilder setTaskDisplayAreas(List<TaskDisplayArea> taskDisplayAreas) {
            this.mTaskDisplayAreas.clear();
            this.mTaskDisplayAreas.addAll(taskDisplayAreas);
            return this;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public HierarchyBuilder setImeContainer(DisplayArea.Tokens imeContainer) {
            this.mImeContainer = imeContainer;
            return this;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void build() {
            build(null);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void build(List<HierarchyBuilder> displayAreaGroupHierarchyBuilders) {
            WindowManagerPolicy policy = this.mRoot.mWmService.mPolicy;
            int maxWindowLayerCount = policy.getMaxWindowLayer() + 1;
            DisplayArea.Tokens[] displayAreaForLayer = new DisplayArea.Tokens[maxWindowLayerCount];
            Map<Feature, List<DisplayArea<WindowContainer>>> featureAreas = new ArrayMap<>(this.mFeatures.size());
            for (int i = 0; i < this.mFeatures.size(); i++) {
                featureAreas.put(this.mFeatures.get(i), new ArrayList<>());
            }
            PendingArea[] areaForLayer = new PendingArea[maxWindowLayerCount];
            PendingArea root = new PendingArea(null, 0, null);
            Arrays.fill(areaForLayer, root);
            int size = this.mFeatures.size();
            for (int i2 = 0; i2 < size; i2++) {
                Feature feature = this.mFeatures.get(i2);
                PendingArea featureArea = null;
                for (int layer = 0; layer < maxWindowLayerCount; layer++) {
                    if (feature.mWindowLayers[layer]) {
                        if (featureArea == null || featureArea.mParent != areaForLayer[layer]) {
                            featureArea = new PendingArea(feature, layer, areaForLayer[layer]);
                            areaForLayer[layer].mChildren.add(featureArea);
                        }
                        areaForLayer[layer] = featureArea;
                    } else {
                        featureArea = null;
                    }
                }
            }
            PendingArea leafArea = null;
            int leafType = 0;
            for (int layer2 = 0; layer2 < maxWindowLayerCount; layer2++) {
                int type = typeOfLayer(policy, layer2);
                if (leafArea == null || leafArea.mParent != areaForLayer[layer2] || type != leafType) {
                    leafArea = new PendingArea(null, layer2, areaForLayer[layer2]);
                    areaForLayer[layer2].mChildren.add(leafArea);
                    leafType = type;
                    if (leafType == 1) {
                        addTaskDisplayAreasToApplicationLayer(areaForLayer[layer2]);
                        addDisplayAreaGroupsToApplicationLayer(areaForLayer[layer2], displayAreaGroupHierarchyBuilders);
                        leafArea.mSkipTokens = true;
                    } else if (leafType == 2) {
                        leafArea.mExisting = this.mImeContainer;
                        leafArea.mSkipTokens = true;
                    }
                }
                leafArea.mMaxLayer = layer2;
            }
            root.computeMaxLayer();
            root.instantiateChildren(this.mRoot, displayAreaForLayer, 0, featureAreas);
            this.mRoot.onHierarchyBuilt(this.mFeatures, displayAreaForLayer, featureAreas);
        }

        private void addTaskDisplayAreasToApplicationLayer(PendingArea parentPendingArea) {
            int count = this.mTaskDisplayAreas.size();
            for (int i = 0; i < count; i++) {
                PendingArea leafArea = new PendingArea(null, 2, parentPendingArea);
                leafArea.mExisting = this.mTaskDisplayAreas.get(i);
                leafArea.mMaxLayer = 2;
                parentPendingArea.mChildren.add(leafArea);
            }
        }

        private void addDisplayAreaGroupsToApplicationLayer(PendingArea parentPendingArea, List<HierarchyBuilder> displayAreaGroupHierarchyBuilders) {
            if (displayAreaGroupHierarchyBuilders == null) {
                return;
            }
            int count = displayAreaGroupHierarchyBuilders.size();
            for (int i = 0; i < count; i++) {
                PendingArea leafArea = new PendingArea(null, 2, parentPendingArea);
                leafArea.mExisting = displayAreaGroupHierarchyBuilders.get(i).mRoot;
                leafArea.mMaxLayer = 2;
                parentPendingArea.mChildren.add(leafArea);
            }
        }

        private static int typeOfLayer(WindowManagerPolicy policy, int layer) {
            if (layer == 2) {
                return 1;
            }
            if (layer == policy.getWindowLayerFromTypeLw(2011) || layer == policy.getWindowLayerFromTypeLw(2012)) {
                return 2;
            }
            return 0;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public static class Feature {
        private final int mId;
        private final String mName;
        private final NewDisplayAreaSupplier mNewDisplayAreaSupplier;
        private final boolean[] mWindowLayers;

        private Feature(String name, int id, boolean[] windowLayers, NewDisplayAreaSupplier newDisplayAreaSupplier) {
            this.mName = name;
            this.mId = id;
            this.mWindowLayers = windowLayers;
            this.mNewDisplayAreaSupplier = newDisplayAreaSupplier;
        }

        public int getId() {
            return this.mId;
        }

        public String toString() {
            return "Feature(\"" + this.mName + "\", " + this.mId + '}';
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* loaded from: classes2.dex */
        public static class Builder {
            private final int mId;
            private final boolean[] mLayers;
            private final String mName;
            private final WindowManagerPolicy mPolicy;
            private NewDisplayAreaSupplier mNewDisplayAreaSupplier = new NewDisplayAreaSupplier() { // from class: com.android.server.wm.DisplayAreaPolicyBuilder$Feature$Builder$$ExternalSyntheticLambda0
                @Override // com.android.server.wm.DisplayAreaPolicyBuilder.NewDisplayAreaSupplier
                public final DisplayArea create(WindowManagerService windowManagerService, DisplayArea.Type type, String str, int i) {
                    return new DisplayArea(windowManagerService, type, str, i);
                }
            };
            private boolean mExcludeRoundedCorner = true;

            /* JADX INFO: Access modifiers changed from: package-private */
            public Builder(WindowManagerPolicy policy, String name, int id) {
                this.mPolicy = policy;
                this.mName = name;
                this.mId = id;
                this.mLayers = new boolean[policy.getMaxWindowLayer() + 1];
            }

            /* JADX INFO: Access modifiers changed from: package-private */
            public Builder all() {
                Arrays.fill(this.mLayers, true);
                return this;
            }

            /* JADX INFO: Access modifiers changed from: package-private */
            public Builder and(int... types) {
                for (int type : types) {
                    set(type, true);
                }
                return this;
            }

            /* JADX INFO: Access modifiers changed from: package-private */
            public Builder except(int... types) {
                for (int type : types) {
                    set(type, false);
                }
                return this;
            }

            /* JADX INFO: Access modifiers changed from: package-private */
            public Builder upTo(int typeInclusive) {
                int max = layerFromType(typeInclusive, false);
                for (int i = 0; i < max; i++) {
                    this.mLayers[i] = true;
                }
                set(typeInclusive, true);
                return this;
            }

            /* JADX INFO: Access modifiers changed from: package-private */
            public Builder setNewDisplayAreaSupplier(NewDisplayAreaSupplier newDisplayAreaSupplier) {
                this.mNewDisplayAreaSupplier = newDisplayAreaSupplier;
                return this;
            }

            Builder setExcludeRoundedCornerOverlay(boolean excludeRoundedCorner) {
                this.mExcludeRoundedCorner = excludeRoundedCorner;
                return this;
            }

            /* JADX INFO: Access modifiers changed from: package-private */
            public Feature build() {
                if (this.mExcludeRoundedCorner) {
                    this.mLayers[this.mPolicy.getMaxWindowLayer()] = false;
                }
                return new Feature(this.mName, this.mId, (boolean[]) this.mLayers.clone(), this.mNewDisplayAreaSupplier);
            }

            private void set(int type, boolean value) {
                this.mLayers[layerFromType(type, true)] = value;
                if (type == 2038) {
                    this.mLayers[layerFromType(type, true)] = value;
                    this.mLayers[layerFromType(2003, false)] = value;
                    this.mLayers[layerFromType(2006, false)] = value;
                    this.mLayers[layerFromType(2010, false)] = value;
                }
            }

            private int layerFromType(int type, boolean internalWindows) {
                return this.mPolicy.getWindowLayerFromTypeLw(type, internalWindows);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public static class Result extends DisplayAreaPolicy {
        private final TaskDisplayArea mDefaultTaskDisplayArea;
        final List<RootDisplayArea> mDisplayAreaGroupRoots;
        final BiFunction<Integer, Bundle, RootDisplayArea> mSelectRootForWindowFunc;
        private final Function<Bundle, TaskDisplayArea> mSelectTaskDisplayAreaFunc;

        Result(WindowManagerService wmService, RootDisplayArea root, List<RootDisplayArea> displayAreaGroupRoots, BiFunction<Integer, Bundle, RootDisplayArea> selectRootForWindowFunc, Function<Bundle, TaskDisplayArea> selectTaskDisplayAreaFunc) {
            super(wmService, root);
            Function<Bundle, TaskDisplayArea> defaultSelectTaskDisplayAreaFunction;
            this.mDisplayAreaGroupRoots = Collections.unmodifiableList(displayAreaGroupRoots);
            this.mSelectRootForWindowFunc = selectRootForWindowFunc;
            TaskDisplayArea taskDisplayArea = (TaskDisplayArea) this.mRoot.getItemFromTaskDisplayAreas(new Function() { // from class: com.android.server.wm.DisplayAreaPolicyBuilder$Result$$ExternalSyntheticLambda0
                @Override // java.util.function.Function
                public final Object apply(Object obj) {
                    return DisplayAreaPolicyBuilder.Result.lambda$new$0((TaskDisplayArea) obj);
                }
            });
            this.mDefaultTaskDisplayArea = taskDisplayArea;
            if (taskDisplayArea == null) {
                throw new IllegalStateException("No display area with FEATURE_DEFAULT_TASK_CONTAINER");
            }
            if (selectTaskDisplayAreaFunc != null) {
                defaultSelectTaskDisplayAreaFunction = selectTaskDisplayAreaFunc;
            } else {
                defaultSelectTaskDisplayAreaFunction = new DefaultSelectTaskDisplayAreaFunction(taskDisplayArea);
            }
            this.mSelectTaskDisplayAreaFunc = defaultSelectTaskDisplayAreaFunction;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public static /* synthetic */ TaskDisplayArea lambda$new$0(TaskDisplayArea taskDisplayArea) {
            if (taskDisplayArea.mFeatureId == 1) {
                return taskDisplayArea;
            }
            return null;
        }

        @Override // com.android.server.wm.DisplayAreaPolicy
        public void addWindow(WindowToken token) {
            DisplayArea.Tokens area = findAreaForToken(token);
            area.addChild(token);
        }

        DisplayArea.Tokens findAreaForToken(WindowToken token) {
            return this.mSelectRootForWindowFunc.apply(Integer.valueOf(token.windowType), token.mOptions).findAreaForTokenInLayer(token);
        }

        @Override // com.android.server.wm.DisplayAreaPolicy
        public DisplayArea.Tokens findAreaForWindowType(int type, Bundle options, boolean ownerCanManageAppTokens, boolean roundedCornerOverlay) {
            return this.mSelectRootForWindowFunc.apply(Integer.valueOf(type), options).findAreaForWindowTypeInLayer(type, ownerCanManageAppTokens, roundedCornerOverlay);
        }

        List<Feature> getFeatures() {
            Set<Feature> features = new ArraySet<>();
            features.addAll(this.mRoot.mFeatures);
            for (int i = 0; i < this.mDisplayAreaGroupRoots.size(); i++) {
                features.addAll(this.mDisplayAreaGroupRoots.get(i).mFeatures);
            }
            return new ArrayList(features);
        }

        @Override // com.android.server.wm.DisplayAreaPolicy
        public List<DisplayArea<? extends WindowContainer>> getDisplayAreas(int featureId) {
            List<DisplayArea<? extends WindowContainer>> displayAreas = new ArrayList<>();
            getDisplayAreas(this.mRoot, featureId, displayAreas);
            for (int i = 0; i < this.mDisplayAreaGroupRoots.size(); i++) {
                getDisplayAreas(this.mDisplayAreaGroupRoots.get(i), featureId, displayAreas);
            }
            return displayAreas;
        }

        private static void getDisplayAreas(RootDisplayArea root, int featureId, List<DisplayArea<? extends WindowContainer>> displayAreas) {
            List<Feature> features = root.mFeatures;
            for (int i = 0; i < features.size(); i++) {
                Feature feature = features.get(i);
                if (feature.mId == featureId) {
                    displayAreas.addAll(root.mFeatureToDisplayAreas.get(feature));
                }
            }
        }

        @Override // com.android.server.wm.DisplayAreaPolicy
        public TaskDisplayArea getDefaultTaskDisplayArea() {
            return this.mDefaultTaskDisplayArea;
        }

        @Override // com.android.server.wm.DisplayAreaPolicy
        public TaskDisplayArea getTaskDisplayArea(Bundle options) {
            return this.mSelectTaskDisplayAreaFunc.apply(options);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public static class PendingArea {
        DisplayArea mExisting;
        final Feature mFeature;
        int mMaxLayer;
        final int mMinLayer;
        final PendingArea mParent;
        final ArrayList<PendingArea> mChildren = new ArrayList<>();
        boolean mSkipTokens = false;

        PendingArea(Feature feature, int minLayer, PendingArea parent) {
            this.mMinLayer = minLayer;
            this.mFeature = feature;
            this.mParent = parent;
        }

        int computeMaxLayer() {
            for (int i = 0; i < this.mChildren.size(); i++) {
                this.mMaxLayer = Math.max(this.mMaxLayer, this.mChildren.get(i).computeMaxLayer());
            }
            int i2 = this.mMaxLayer;
            return i2;
        }

        void instantiateChildren(DisplayArea<DisplayArea> parent, DisplayArea.Tokens[] areaForLayer, int level, Map<Feature, List<DisplayArea<WindowContainer>>> areas) {
            this.mChildren.sort(Comparator.comparingInt(new ToIntFunction() { // from class: com.android.server.wm.DisplayAreaPolicyBuilder$PendingArea$$ExternalSyntheticLambda0
                @Override // java.util.function.ToIntFunction
                public final int applyAsInt(Object obj) {
                    int i;
                    i = ((DisplayAreaPolicyBuilder.PendingArea) obj).mMinLayer;
                    return i;
                }
            }));
            for (int i = 0; i < this.mChildren.size(); i++) {
                PendingArea child = this.mChildren.get(i);
                DisplayArea area = child.createArea(parent, areaForLayer);
                if (area != null) {
                    parent.addChild(area, Integer.MAX_VALUE);
                    Feature feature = child.mFeature;
                    if (feature != null) {
                        areas.get(feature).add(area);
                    }
                    child.instantiateChildren(area, areaForLayer, level + 1, areas);
                }
            }
        }

        private DisplayArea createArea(DisplayArea<DisplayArea> parent, DisplayArea.Tokens[] areaForLayer) {
            DisplayArea.Type type;
            DisplayArea displayArea = this.mExisting;
            if (displayArea != null) {
                if (displayArea.asTokens() != null) {
                    fillAreaForLayers(this.mExisting.asTokens(), areaForLayer);
                }
                return this.mExisting;
            } else if (this.mSkipTokens) {
                return null;
            } else {
                if (this.mMinLayer > 2) {
                    type = DisplayArea.Type.ABOVE_TASKS;
                } else if (this.mMaxLayer < 2) {
                    type = DisplayArea.Type.BELOW_TASKS;
                } else {
                    type = DisplayArea.Type.ANY;
                }
                Feature feature = this.mFeature;
                if (feature == null) {
                    DisplayArea.Tokens leaf = new DisplayArea.Tokens(parent.mWmService, type, "Leaf:" + this.mMinLayer + ":" + this.mMaxLayer);
                    fillAreaForLayers(leaf, areaForLayer);
                    return leaf;
                }
                return feature.mNewDisplayAreaSupplier.create(parent.mWmService, type, this.mFeature.mName + ":" + this.mMinLayer + ":" + this.mMaxLayer, this.mFeature.mId);
            }
        }

        private void fillAreaForLayers(DisplayArea.Tokens leaf, DisplayArea.Tokens[] areaForLayer) {
            for (int i = this.mMinLayer; i <= this.mMaxLayer; i++) {
                areaForLayer[i] = leaf;
            }
        }
    }
}
