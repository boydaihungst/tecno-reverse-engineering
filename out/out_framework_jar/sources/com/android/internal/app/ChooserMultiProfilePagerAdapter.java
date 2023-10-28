package com.android.internal.app;

import android.app.admin.DevicePolicyManager;
import android.app.admin.DevicePolicyResources;
import android.content.Context;
import android.os.UserHandle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.android.internal.R;
import com.android.internal.app.AbstractMultiProfilePagerAdapter;
import com.android.internal.app.ChooserActivity;
import com.android.internal.widget.GridLayoutManager;
import com.android.internal.widget.RecyclerView;
import java.util.function.Supplier;
/* loaded from: classes4.dex */
public class ChooserMultiProfilePagerAdapter extends AbstractMultiProfilePagerAdapter {
    private static final int SINGLE_CELL_SPAN_SIZE = 1;
    private int mBottomOffset;
    private final boolean mIsSendAction;
    private final ChooserProfileDescriptor[] mItems;
    private int mMaxTargetsPerRow;

    /* JADX INFO: Access modifiers changed from: package-private */
    public ChooserMultiProfilePagerAdapter(Context context, ChooserActivity.ChooserGridAdapter adapter, UserHandle personalProfileUserHandle, UserHandle workProfileUserHandle, boolean isSendAction, int maxTargetsPerRow) {
        super(context, 0, personalProfileUserHandle, workProfileUserHandle);
        this.mItems = new ChooserProfileDescriptor[]{createProfileDescriptor(adapter)};
        this.mIsSendAction = isSendAction;
        this.mMaxTargetsPerRow = maxTargetsPerRow;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ChooserMultiProfilePagerAdapter(Context context, ChooserActivity.ChooserGridAdapter personalAdapter, ChooserActivity.ChooserGridAdapter workAdapter, int defaultProfile, UserHandle personalProfileUserHandle, UserHandle workProfileUserHandle, boolean isSendAction, int maxTargetsPerRow) {
        super(context, defaultProfile, personalProfileUserHandle, workProfileUserHandle);
        this.mItems = new ChooserProfileDescriptor[]{createProfileDescriptor(personalAdapter), createProfileDescriptor(workAdapter)};
        this.mIsSendAction = isSendAction;
        this.mMaxTargetsPerRow = maxTargetsPerRow;
    }

    private ChooserProfileDescriptor createProfileDescriptor(ChooserActivity.ChooserGridAdapter adapter) {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.chooser_list_per_profile, (ViewGroup) null, false);
        ChooserProfileDescriptor profileDescriptor = new ChooserProfileDescriptor(rootView, adapter);
        profileDescriptor.recyclerView.setAccessibilityDelegateCompat(new ChooserRecyclerViewAccessibilityDelegate(profileDescriptor.recyclerView));
        return profileDescriptor;
    }

    RecyclerView getListViewForIndex(int index) {
        return getItem(index).recyclerView;
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.internal.app.AbstractMultiProfilePagerAdapter
    public ChooserProfileDescriptor getItem(int pageIndex) {
        return this.mItems[pageIndex];
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.internal.app.AbstractMultiProfilePagerAdapter
    public int getItemCount() {
        return this.mItems.length;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.internal.app.AbstractMultiProfilePagerAdapter
    public ChooserActivity.ChooserGridAdapter getAdapterForIndex(int pageIndex) {
        return this.mItems[pageIndex].chooserGridAdapter;
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.internal.app.AbstractMultiProfilePagerAdapter
    public ChooserListAdapter getListAdapterForUserHandle(UserHandle userHandle) {
        if (getActiveListAdapter().getUserHandle().equals(userHandle)) {
            return getActiveListAdapter();
        }
        if (getInactiveListAdapter() != null && getInactiveListAdapter().getUserHandle().equals(userHandle)) {
            return getInactiveListAdapter();
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.internal.app.AbstractMultiProfilePagerAdapter
    public void setupListAdapter(int pageIndex) {
        RecyclerView recyclerView = getItem(pageIndex).recyclerView;
        final ChooserActivity.ChooserGridAdapter chooserGridAdapter = getItem(pageIndex).chooserGridAdapter;
        final GridLayoutManager glm = (GridLayoutManager) recyclerView.getLayoutManager();
        glm.setSpanCount(this.mMaxTargetsPerRow);
        glm.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() { // from class: com.android.internal.app.ChooserMultiProfilePagerAdapter.1
            @Override // com.android.internal.widget.GridLayoutManager.SpanSizeLookup
            public int getSpanSize(int position) {
                if (chooserGridAdapter.shouldCellSpan(position)) {
                    return 1;
                }
                return glm.getSpanCount();
            }
        });
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.internal.app.AbstractMultiProfilePagerAdapter
    public ChooserListAdapter getActiveListAdapter() {
        return getAdapterForIndex(getCurrentPage()).getListAdapter();
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.internal.app.AbstractMultiProfilePagerAdapter
    public ChooserListAdapter getInactiveListAdapter() {
        if (getCount() == 1) {
            return null;
        }
        return getAdapterForIndex(1 - getCurrentPage()).getListAdapter();
    }

    @Override // com.android.internal.app.AbstractMultiProfilePagerAdapter
    public ResolverListAdapter getPersonalListAdapter() {
        return getAdapterForIndex(0).getListAdapter();
    }

    @Override // com.android.internal.app.AbstractMultiProfilePagerAdapter
    public ResolverListAdapter getWorkListAdapter() {
        return getAdapterForIndex(1).getListAdapter();
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.internal.app.AbstractMultiProfilePagerAdapter
    public ChooserActivity.ChooserGridAdapter getCurrentRootAdapter() {
        return getAdapterForIndex(getCurrentPage());
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.internal.app.AbstractMultiProfilePagerAdapter
    public RecyclerView getActiveAdapterView() {
        return getListViewForIndex(getCurrentPage());
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.internal.app.AbstractMultiProfilePagerAdapter
    public RecyclerView getInactiveAdapterView() {
        if (getCount() == 1) {
            return null;
        }
        return getListViewForIndex(1 - getCurrentPage());
    }

    @Override // com.android.internal.app.AbstractMultiProfilePagerAdapter
    String getMetricsCategory() {
        return "intent_chooser";
    }

    @Override // com.android.internal.app.AbstractMultiProfilePagerAdapter
    protected void showWorkProfileOffEmptyState(ResolverListAdapter activeListAdapter, View.OnClickListener listener) {
        showEmptyState(activeListAdapter, getWorkAppPausedTitle(), null, listener);
    }

    @Override // com.android.internal.app.AbstractMultiProfilePagerAdapter
    protected void showNoPersonalToWorkIntentsEmptyState(ResolverListAdapter activeListAdapter) {
        if (this.mIsSendAction) {
            showEmptyState(activeListAdapter, getCrossProfileBlockedTitle(), getCantShareWithWorkMessage());
        } else {
            showEmptyState(activeListAdapter, getCrossProfileBlockedTitle(), getCantAccessWorkMessage());
        }
    }

    @Override // com.android.internal.app.AbstractMultiProfilePagerAdapter
    protected void showNoWorkToPersonalIntentsEmptyState(ResolverListAdapter activeListAdapter) {
        if (this.mIsSendAction) {
            showEmptyState(activeListAdapter, getCrossProfileBlockedTitle(), getCantShareWithPersonalMessage());
        } else {
            showEmptyState(activeListAdapter, getCrossProfileBlockedTitle(), getCantAccessPersonalMessage());
        }
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
        return ((DevicePolicyManager) getContext().getSystemService(DevicePolicyManager.class)).getResources().getString(DevicePolicyResources.Strings.Core.RESOLVER_WORK_PAUSED_TITLE, new Supplier() { // from class: com.android.internal.app.ChooserMultiProfilePagerAdapter$$ExternalSyntheticLambda2
            @Override // java.util.function.Supplier
            public final Object get() {
                return ChooserMultiProfilePagerAdapter.this.m6420x4a616f4a();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getWorkAppPausedTitle$0$com-android-internal-app-ChooserMultiProfilePagerAdapter  reason: not valid java name */
    public /* synthetic */ String m6420x4a616f4a() {
        return getContext().getString(R.string.resolver_turn_on_work_apps);
    }

    private String getCrossProfileBlockedTitle() {
        return ((DevicePolicyManager) getContext().getSystemService(DevicePolicyManager.class)).getResources().getString(DevicePolicyResources.Strings.Core.RESOLVER_CROSS_PROFILE_BLOCKED_TITLE, new Supplier() { // from class: com.android.internal.app.ChooserMultiProfilePagerAdapter$$ExternalSyntheticLambda0
            @Override // java.util.function.Supplier
            public final Object get() {
                return ChooserMultiProfilePagerAdapter.this.m6417xef2562ee();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getCrossProfileBlockedTitle$1$com-android-internal-app-ChooserMultiProfilePagerAdapter  reason: not valid java name */
    public /* synthetic */ String m6417xef2562ee() {
        return getContext().getString(R.string.resolver_cross_profile_blocked);
    }

    private String getCantShareWithWorkMessage() {
        return ((DevicePolicyManager) getContext().getSystemService(DevicePolicyManager.class)).getResources().getString(DevicePolicyResources.Strings.Core.RESOLVER_CANT_SHARE_WITH_WORK, new Supplier() { // from class: com.android.internal.app.ChooserMultiProfilePagerAdapter$$ExternalSyntheticLambda4
            @Override // java.util.function.Supplier
            public final Object get() {
                return ChooserMultiProfilePagerAdapter.this.m6416x873197cd();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getCantShareWithWorkMessage$2$com-android-internal-app-ChooserMultiProfilePagerAdapter  reason: not valid java name */
    public /* synthetic */ String m6416x873197cd() {
        return getContext().getString(R.string.resolver_cant_share_with_work_apps_explanation);
    }

    private String getCantShareWithPersonalMessage() {
        return ((DevicePolicyManager) getContext().getSystemService(DevicePolicyManager.class)).getResources().getString(DevicePolicyResources.Strings.Core.RESOLVER_CANT_SHARE_WITH_PERSONAL, new Supplier() { // from class: com.android.internal.app.ChooserMultiProfilePagerAdapter$$ExternalSyntheticLambda6
            @Override // java.util.function.Supplier
            public final Object get() {
                return ChooserMultiProfilePagerAdapter.this.m6415x2926725b();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getCantShareWithPersonalMessage$3$com-android-internal-app-ChooserMultiProfilePagerAdapter  reason: not valid java name */
    public /* synthetic */ String m6415x2926725b() {
        return getContext().getString(R.string.resolver_cant_share_with_personal_apps_explanation);
    }

    private String getCantAccessWorkMessage() {
        return ((DevicePolicyManager) getContext().getSystemService(DevicePolicyManager.class)).getResources().getString(DevicePolicyResources.Strings.Core.RESOLVER_CANT_ACCESS_WORK, new Supplier() { // from class: com.android.internal.app.ChooserMultiProfilePagerAdapter$$ExternalSyntheticLambda3
            @Override // java.util.function.Supplier
            public final Object get() {
                return ChooserMultiProfilePagerAdapter.this.m6414xc0e13bc4();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getCantAccessWorkMessage$4$com-android-internal-app-ChooserMultiProfilePagerAdapter  reason: not valid java name */
    public /* synthetic */ String m6414xc0e13bc4() {
        return getContext().getString(R.string.resolver_cant_access_work_apps_explanation);
    }

    private String getCantAccessPersonalMessage() {
        return ((DevicePolicyManager) getContext().getSystemService(DevicePolicyManager.class)).getResources().getString(DevicePolicyResources.Strings.Core.RESOLVER_CANT_ACCESS_PERSONAL, new Supplier() { // from class: com.android.internal.app.ChooserMultiProfilePagerAdapter$$ExternalSyntheticLambda1
            @Override // java.util.function.Supplier
            public final Object get() {
                return ChooserMultiProfilePagerAdapter.this.m6413xeb4291d2();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getCantAccessPersonalMessage$5$com-android-internal-app-ChooserMultiProfilePagerAdapter  reason: not valid java name */
    public /* synthetic */ String m6413xeb4291d2() {
        return getContext().getString(R.string.resolver_cant_access_personal_apps_explanation);
    }

    private String getNoWorkAppsAvailableMessage() {
        return ((DevicePolicyManager) getContext().getSystemService(DevicePolicyManager.class)).getResources().getString(DevicePolicyResources.Strings.Core.RESOLVER_NO_WORK_APPS, new Supplier() { // from class: com.android.internal.app.ChooserMultiProfilePagerAdapter$$ExternalSyntheticLambda5
            @Override // java.util.function.Supplier
            public final Object get() {
                return ChooserMultiProfilePagerAdapter.this.m6419x9b10b13c();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getNoWorkAppsAvailableMessage$6$com-android-internal-app-ChooserMultiProfilePagerAdapter  reason: not valid java name */
    public /* synthetic */ String m6419x9b10b13c() {
        return getContext().getString(R.string.resolver_no_work_apps_available);
    }

    private String getNoPersonalAppsAvailableMessage() {
        return ((DevicePolicyManager) getContext().getSystemService(DevicePolicyManager.class)).getResources().getString(DevicePolicyResources.Strings.Core.RESOLVER_NO_PERSONAL_APPS, new Supplier() { // from class: com.android.internal.app.ChooserMultiProfilePagerAdapter$$ExternalSyntheticLambda7
            @Override // java.util.function.Supplier
            public final Object get() {
                return ChooserMultiProfilePagerAdapter.this.m6418xa3894a4c();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getNoPersonalAppsAvailableMessage$7$com-android-internal-app-ChooserMultiProfilePagerAdapter  reason: not valid java name */
    public /* synthetic */ String m6418xa3894a4c() {
        return getContext().getString(R.string.resolver_no_personal_apps_available);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setEmptyStateBottomOffset(int bottomOffset) {
        this.mBottomOffset = bottomOffset;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.internal.app.AbstractMultiProfilePagerAdapter
    public void setupContainerPadding(View container) {
        int initialBottomPadding = getContext().getResources().getDimensionPixelSize(R.dimen.resolver_empty_state_container_padding_bottom);
        container.setPadding(container.getPaddingLeft(), container.getPaddingTop(), container.getPaddingRight(), this.mBottomOffset + initialBottomPadding);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes4.dex */
    public class ChooserProfileDescriptor extends AbstractMultiProfilePagerAdapter.ProfileDescriptor {
        private ChooserActivity.ChooserGridAdapter chooserGridAdapter;
        private RecyclerView recyclerView;

        ChooserProfileDescriptor(ViewGroup rootView, ChooserActivity.ChooserGridAdapter adapter) {
            super(rootView);
            this.chooserGridAdapter = adapter;
            this.recyclerView = (RecyclerView) rootView.findViewById(R.id.resolver_list);
        }
    }
}
