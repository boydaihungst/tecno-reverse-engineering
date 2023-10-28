package com.android.internal.app;

import android.app.admin.DevicePolicyManager;
import android.app.admin.DevicePolicyResources;
import android.content.Context;
import android.os.UserHandle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.ListView;
import com.android.internal.R;
import com.android.internal.app.AbstractMultiProfilePagerAdapter;
import java.util.function.Supplier;
/* loaded from: classes4.dex */
public class ResolverMultiProfilePagerAdapter extends AbstractMultiProfilePagerAdapter {
    private final ResolverProfileDescriptor[] mItems;
    private final boolean mShouldShowNoCrossProfileIntentsEmptyState;
    private boolean mUseLayoutWithDefault;

    /* JADX INFO: Access modifiers changed from: package-private */
    public ResolverMultiProfilePagerAdapter(Context context, ResolverListAdapter adapter, UserHandle personalProfileUserHandle, UserHandle workProfileUserHandle) {
        super(context, 0, personalProfileUserHandle, workProfileUserHandle);
        this.mItems = new ResolverProfileDescriptor[]{createProfileDescriptor(adapter)};
        this.mShouldShowNoCrossProfileIntentsEmptyState = true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ResolverMultiProfilePagerAdapter(Context context, ResolverListAdapter personalAdapter, ResolverListAdapter workAdapter, int defaultProfile, UserHandle personalProfileUserHandle, UserHandle workProfileUserHandle, boolean shouldShowNoCrossProfileIntentsEmptyState) {
        super(context, defaultProfile, personalProfileUserHandle, workProfileUserHandle);
        this.mItems = new ResolverProfileDescriptor[]{createProfileDescriptor(personalAdapter), createProfileDescriptor(workAdapter)};
        this.mShouldShowNoCrossProfileIntentsEmptyState = shouldShowNoCrossProfileIntentsEmptyState;
    }

    private ResolverProfileDescriptor createProfileDescriptor(ResolverListAdapter adapter) {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.resolver_list_per_profile, (ViewGroup) null, false);
        return new ResolverProfileDescriptor(rootView, adapter);
    }

    ListView getListViewForIndex(int index) {
        return getItem(index).listView;
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.internal.app.AbstractMultiProfilePagerAdapter
    public ResolverProfileDescriptor getItem(int pageIndex) {
        return this.mItems[pageIndex];
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.internal.app.AbstractMultiProfilePagerAdapter
    public int getItemCount() {
        return this.mItems.length;
    }

    @Override // com.android.internal.app.AbstractMultiProfilePagerAdapter
    void setupListAdapter(int pageIndex) {
        ListView listView = getItem(pageIndex).listView;
        if (getContext() instanceof ResolverActivity) {
            ResolverActivity activity = (ResolverActivity) getContext();
            if (!activity.isCheckAndResolve()) {
                listView.setAdapter((ListAdapter) getItem(pageIndex).resolverListAdapter);
                return;
            }
            return;
        }
        listView.setAdapter((ListAdapter) getItem(pageIndex).resolverListAdapter);
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.internal.app.AbstractMultiProfilePagerAdapter
    public ResolverListAdapter getAdapterForIndex(int pageIndex) {
        return this.mItems[pageIndex].resolverListAdapter;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.internal.app.AbstractMultiProfilePagerAdapter, com.android.internal.widget.PagerAdapter
    public ViewGroup instantiateItem(ViewGroup container, int position) {
        setupListAdapter(position);
        return super.instantiateItem(container, position);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.internal.app.AbstractMultiProfilePagerAdapter
    public ResolverListAdapter getListAdapterForUserHandle(UserHandle userHandle) {
        if (getActiveListAdapter().getUserHandle().equals(userHandle)) {
            return getActiveListAdapter();
        }
        if (getInactiveListAdapter() != null && getInactiveListAdapter().getUserHandle().equals(userHandle)) {
            return getInactiveListAdapter();
        }
        return null;
    }

    @Override // com.android.internal.app.AbstractMultiProfilePagerAdapter
    public ResolverListAdapter getActiveListAdapter() {
        return getAdapterForIndex(getCurrentPage());
    }

    @Override // com.android.internal.app.AbstractMultiProfilePagerAdapter
    public ResolverListAdapter getInactiveListAdapter() {
        if (getCount() == 1) {
            return null;
        }
        return getAdapterForIndex(1 - getCurrentPage());
    }

    @Override // com.android.internal.app.AbstractMultiProfilePagerAdapter
    public ResolverListAdapter getPersonalListAdapter() {
        return getAdapterForIndex(0);
    }

    @Override // com.android.internal.app.AbstractMultiProfilePagerAdapter
    public ResolverListAdapter getWorkListAdapter() {
        return getAdapterForIndex(1);
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.internal.app.AbstractMultiProfilePagerAdapter
    public ResolverListAdapter getCurrentRootAdapter() {
        return getActiveListAdapter();
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.internal.app.AbstractMultiProfilePagerAdapter
    public ListView getActiveAdapterView() {
        return getListViewForIndex(getCurrentPage());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.internal.app.AbstractMultiProfilePagerAdapter
    public ViewGroup getInactiveAdapterView() {
        if (getCount() == 1) {
            return null;
        }
        return getListViewForIndex(1 - getCurrentPage());
    }

    @Override // com.android.internal.app.AbstractMultiProfilePagerAdapter
    String getMetricsCategory() {
        return "intent_resolver";
    }

    @Override // com.android.internal.app.AbstractMultiProfilePagerAdapter
    boolean allowShowNoCrossProfileIntentsEmptyState() {
        return this.mShouldShowNoCrossProfileIntentsEmptyState;
    }

    @Override // com.android.internal.app.AbstractMultiProfilePagerAdapter
    protected void showWorkProfileOffEmptyState(ResolverListAdapter activeListAdapter, View.OnClickListener listener) {
        showEmptyState(activeListAdapter, getWorkAppPausedTitle(), null, listener);
    }

    @Override // com.android.internal.app.AbstractMultiProfilePagerAdapter
    protected void showNoPersonalToWorkIntentsEmptyState(ResolverListAdapter activeListAdapter) {
        showEmptyState(activeListAdapter, getCrossProfileBlockedTitle(), getCantAccessWorkMessage());
    }

    @Override // com.android.internal.app.AbstractMultiProfilePagerAdapter
    protected void showNoWorkToPersonalIntentsEmptyState(ResolverListAdapter activeListAdapter) {
        showEmptyState(activeListAdapter, getCrossProfileBlockedTitle(), getCantAccessPersonalMessage());
    }

    @Override // com.android.internal.app.AbstractMultiProfilePagerAdapter
    protected void showNoPersonalAppsAvailableEmptyState(ResolverListAdapter listAdapter) {
        showEmptyState(listAdapter, getNoPersonalAppsAvailableMessage(), null);
    }

    @Override // com.android.internal.app.AbstractMultiProfilePagerAdapter
    protected void showNoWorkAppsAvailableEmptyState(ResolverListAdapter listAdapter) {
        showEmptyState(listAdapter, getNoWorkAppsAvailableMessage(), null);
    }

    private String getWorkAppPausedTitle() {
        return ((DevicePolicyManager) getContext().getSystemService(DevicePolicyManager.class)).getResources().getString(DevicePolicyResources.Strings.Core.RESOLVER_WORK_PAUSED_TITLE, new Supplier() { // from class: com.android.internal.app.ResolverMultiProfilePagerAdapter$$ExternalSyntheticLambda5
            @Override // java.util.function.Supplier
            public final Object get() {
                return ResolverMultiProfilePagerAdapter.this.m6485x1757e24f();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getWorkAppPausedTitle$0$com-android-internal-app-ResolverMultiProfilePagerAdapter  reason: not valid java name */
    public /* synthetic */ String m6485x1757e24f() {
        return getContext().getString(R.string.resolver_turn_on_work_apps);
    }

    private String getCrossProfileBlockedTitle() {
        return ((DevicePolicyManager) getContext().getSystemService(DevicePolicyManager.class)).getResources().getString(DevicePolicyResources.Strings.Core.RESOLVER_CROSS_PROFILE_BLOCKED_TITLE, new Supplier() { // from class: com.android.internal.app.ResolverMultiProfilePagerAdapter$$ExternalSyntheticLambda4
            @Override // java.util.function.Supplier
            public final Object get() {
                return ResolverMultiProfilePagerAdapter.this.m6482xb12632b();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getCrossProfileBlockedTitle$1$com-android-internal-app-ResolverMultiProfilePagerAdapter  reason: not valid java name */
    public /* synthetic */ String m6482xb12632b() {
        return getContext().getString(R.string.resolver_cross_profile_blocked);
    }

    private String getCantAccessWorkMessage() {
        return ((DevicePolicyManager) getContext().getSystemService(DevicePolicyManager.class)).getResources().getString(DevicePolicyResources.Strings.Core.RESOLVER_CANT_ACCESS_WORK, new Supplier() { // from class: com.android.internal.app.ResolverMultiProfilePagerAdapter$$ExternalSyntheticLambda0
            @Override // java.util.function.Supplier
            public final Object get() {
                return ResolverMultiProfilePagerAdapter.this.m6481x4f660b93();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getCantAccessWorkMessage$2$com-android-internal-app-ResolverMultiProfilePagerAdapter  reason: not valid java name */
    public /* synthetic */ String m6481x4f660b93() {
        return getContext().getString(R.string.resolver_cant_access_work_apps_explanation);
    }

    private String getCantAccessPersonalMessage() {
        return ((DevicePolicyManager) getContext().getSystemService(DevicePolicyManager.class)).getResources().getString(DevicePolicyResources.Strings.Core.RESOLVER_CANT_ACCESS_PERSONAL, new Supplier() { // from class: com.android.internal.app.ResolverMultiProfilePagerAdapter$$ExternalSyntheticLambda2
            @Override // java.util.function.Supplier
            public final Object get() {
                return ResolverMultiProfilePagerAdapter.this.m6480x712f7745();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getCantAccessPersonalMessage$3$com-android-internal-app-ResolverMultiProfilePagerAdapter  reason: not valid java name */
    public /* synthetic */ String m6480x712f7745() {
        return getContext().getString(R.string.resolver_cant_access_personal_apps_explanation);
    }

    private String getNoWorkAppsAvailableMessage() {
        return ((DevicePolicyManager) getContext().getSystemService(DevicePolicyManager.class)).getResources().getString(DevicePolicyResources.Strings.Core.RESOLVER_NO_WORK_APPS, new Supplier() { // from class: com.android.internal.app.ResolverMultiProfilePagerAdapter$$ExternalSyntheticLambda1
            @Override // java.util.function.Supplier
            public final Object get() {
                return ResolverMultiProfilePagerAdapter.this.m6484xbb25451b();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getNoWorkAppsAvailableMessage$4$com-android-internal-app-ResolverMultiProfilePagerAdapter  reason: not valid java name */
    public /* synthetic */ String m6484xbb25451b() {
        return getContext().getString(R.string.resolver_no_work_apps_available);
    }

    private String getNoPersonalAppsAvailableMessage() {
        return ((DevicePolicyManager) getContext().getSystemService(DevicePolicyManager.class)).getResources().getString(DevicePolicyResources.Strings.Core.RESOLVER_NO_PERSONAL_APPS, new Supplier() { // from class: com.android.internal.app.ResolverMultiProfilePagerAdapter$$ExternalSyntheticLambda3
            @Override // java.util.function.Supplier
            public final Object get() {
                return ResolverMultiProfilePagerAdapter.this.m6483xc1bfce0b();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getNoPersonalAppsAvailableMessage$5$com-android-internal-app-ResolverMultiProfilePagerAdapter  reason: not valid java name */
    public /* synthetic */ String m6483xc1bfce0b() {
        return getContext().getString(R.string.resolver_no_personal_apps_available);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setUseLayoutWithDefault(boolean useLayoutWithDefault) {
        this.mUseLayoutWithDefault = useLayoutWithDefault;
    }

    @Override // com.android.internal.app.AbstractMultiProfilePagerAdapter
    protected void setupContainerPadding(View container) {
        int bottom = this.mUseLayoutWithDefault ? container.getPaddingBottom() : 0;
        container.setPadding(container.getPaddingLeft(), container.getPaddingTop(), container.getPaddingRight(), bottom);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes4.dex */
    public class ResolverProfileDescriptor extends AbstractMultiProfilePagerAdapter.ProfileDescriptor {
        final ListView listView;
        private ResolverListAdapter resolverListAdapter;

        ResolverProfileDescriptor(ViewGroup rootView, ResolverListAdapter adapter) {
            super(rootView);
            this.resolverListAdapter = adapter;
            this.listView = (ListView) rootView.findViewById(R.id.resolver_list);
        }
    }
}
