package com.android.server.wm;

import android.content.res.Resources;
import android.os.Bundle;
import android.text.TextUtils;
import com.android.server.wm.DisplayArea;
import com.android.server.wm.DisplayAreaPolicyBuilder;
import java.util.ArrayList;
import java.util.List;
/* loaded from: classes2.dex */
public abstract class DisplayAreaPolicy {
    protected final RootDisplayArea mRoot;
    protected final WindowManagerService mWmService;

    public abstract void addWindow(WindowToken windowToken);

    public abstract DisplayArea.Tokens findAreaForWindowType(int i, Bundle bundle, boolean z, boolean z2);

    public abstract TaskDisplayArea getDefaultTaskDisplayArea();

    public abstract List<DisplayArea<? extends WindowContainer>> getDisplayAreas(int i);

    public abstract TaskDisplayArea getTaskDisplayArea(Bundle bundle);

    /* JADX INFO: Access modifiers changed from: protected */
    public DisplayAreaPolicy(WindowManagerService wmService, RootDisplayArea root) {
        this.mWmService = wmService;
        this.mRoot = root;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public static final class DefaultProvider implements Provider {
        DefaultProvider() {
        }

        @Override // com.android.server.wm.DisplayAreaPolicy.Provider
        public DisplayAreaPolicy instantiate(WindowManagerService wmService, DisplayContent content, RootDisplayArea root, DisplayArea.Tokens imeContainer) {
            TaskDisplayArea defaultTaskDisplayArea = new TaskDisplayArea(content, wmService, "DefaultTaskDisplayArea", 1);
            DisplayAreaPolicy.hookSetMultiWindowingMode(defaultTaskDisplayArea);
            DisplayAreaPolicy.hookSetMultiWindowId(defaultTaskDisplayArea);
            List<TaskDisplayArea> tdaList = new ArrayList<>();
            tdaList.add(defaultTaskDisplayArea);
            DisplayAreaPolicyBuilder.HierarchyBuilder rootHierarchy = new DisplayAreaPolicyBuilder.HierarchyBuilder(root);
            rootHierarchy.setImeContainer(imeContainer).setTaskDisplayAreas(tdaList);
            if (content.isTrusted()) {
                configureTrustedHierarchyBuilder(rootHierarchy, wmService, content);
            }
            return new DisplayAreaPolicyBuilder().setRootHierarchy(rootHierarchy).build(wmService);
        }

        private void configureTrustedHierarchyBuilder(DisplayAreaPolicyBuilder.HierarchyBuilder rootHierarchy, WindowManagerService wmService, DisplayContent content) {
            rootHierarchy.addFeature(new DisplayAreaPolicyBuilder.Feature.Builder(wmService.mPolicy, "WindowedMagnification", 4).upTo(2039).except(2039).setNewDisplayAreaSupplier(new DisplayAreaPolicyBuilder.NewDisplayAreaSupplier() { // from class: com.android.server.wm.DisplayAreaPolicy$DefaultProvider$$ExternalSyntheticLambda0
                @Override // com.android.server.wm.DisplayAreaPolicyBuilder.NewDisplayAreaSupplier
                public final DisplayArea create(WindowManagerService windowManagerService, DisplayArea.Type type, String str, int i) {
                    return new DisplayArea.Dimmable(windowManagerService, type, str, i);
                }
            }).build());
            if (content.isDefaultDisplay) {
                rootHierarchy.addFeature(new DisplayAreaPolicyBuilder.Feature.Builder(wmService.mPolicy, "HideDisplayCutout", 6).all().except(2019, 2024, 2000, 2040).build()).addFeature(new DisplayAreaPolicyBuilder.Feature.Builder(wmService.mPolicy, "OneHanded", 3).all().except(2019, 2024, 2015).build());
            }
            rootHierarchy.addFeature(new DisplayAreaPolicyBuilder.Feature.Builder(wmService.mPolicy, "FullscreenMagnification", 5).all().except(2039, 2011, 2012, 2027, 2019, 2024).build()).addFeature(new DisplayAreaPolicyBuilder.Feature.Builder(wmService.mPolicy, "ImePlaceholder", 7).and(2011, 2012).build());
        }
    }

    /* loaded from: classes2.dex */
    public interface Provider {
        DisplayAreaPolicy instantiate(WindowManagerService windowManagerService, DisplayContent displayContent, RootDisplayArea rootDisplayArea, DisplayArea.Tokens tokens);

        static Provider fromResources(Resources res) {
            String name = res.getString(17039958);
            if (TextUtils.isEmpty(name)) {
                return new DefaultProvider();
            }
            try {
                return (Provider) Class.forName(name).newInstance();
            } catch (ClassCastException | ReflectiveOperationException e) {
                throw new IllegalStateException("Couldn't instantiate class " + name + " for config_deviceSpecificDisplayAreaPolicyProvider: make sure it has a public zero-argument constructor and implements DisplayAreaPolicy.Provider", e);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void hookSetMultiWindowingMode(TaskDisplayArea defaultTaskDisplayArea) {
        defaultTaskDisplayArea.setMultiWindowMode(1);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void hookSetMultiWindowId(TaskDisplayArea defaultTaskDisplayArea) {
        defaultTaskDisplayArea.setMultiWindowId(0);
    }
}
