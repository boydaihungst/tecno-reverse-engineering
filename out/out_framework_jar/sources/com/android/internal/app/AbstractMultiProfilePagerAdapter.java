package com.android.internal.app;

import android.app.AppGlobals;
import android.app.admin.DevicePolicyEventLogger;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.IPackageManager;
import android.content.pm.ResolveInfo;
import android.os.AsyncTask;
import android.os.Trace;
import android.os.UserHandle;
import android.os.UserManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import com.android.internal.R;
import com.android.internal.app.ResolverActivity;
import com.android.internal.widget.PagerAdapter;
import com.android.internal.widget.ViewPager;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executor;
/* loaded from: classes4.dex */
public abstract class AbstractMultiProfilePagerAdapter extends PagerAdapter {
    static final int PROFILE_PERSONAL = 0;
    static final int PROFILE_WORK = 1;
    private static final String TAG = "AbstractMultiProfilePagerAdapter";
    private final Context mContext;
    private int mCurrentPage;
    private Injector mInjector;
    private boolean mIsWaitingToEnableWorkProfile;
    private Set<Integer> mLoadedPages = new HashSet();
    private OnProfileSelectedListener mOnProfileSelectedListener;
    private OnSwitchOnWorkSelectedListener mOnSwitchOnWorkSelectedListener;
    private final UserHandle mPersonalProfileUserHandle;
    private final UserHandle mWorkProfileUserHandle;

    /* loaded from: classes4.dex */
    public interface Injector {
        boolean hasCrossProfileIntents(List<Intent> list, int i, int i2);

        boolean isQuietModeEnabled(UserHandle userHandle);

        void requestQuietModeEnabled(boolean z, UserHandle userHandle);
    }

    /* loaded from: classes4.dex */
    public interface OnProfileSelectedListener {
        void onProfilePageStateChanged(int i);

        void onProfileSelected(int i);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes4.dex */
    public interface OnSwitchOnWorkSelectedListener {
        void onSwitchOnWorkSelected();
    }

    /* loaded from: classes4.dex */
    @interface Profile {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public abstract ViewGroup getActiveAdapterView();

    public abstract ResolverListAdapter getActiveListAdapter();

    public abstract Object getAdapterForIndex(int i);

    abstract Object getCurrentRootAdapter();

    /* JADX INFO: Access modifiers changed from: package-private */
    public abstract ViewGroup getInactiveAdapterView();

    public abstract ResolverListAdapter getInactiveListAdapter();

    abstract ProfileDescriptor getItem(int i);

    /* JADX INFO: Access modifiers changed from: package-private */
    public abstract int getItemCount();

    /* JADX INFO: Access modifiers changed from: package-private */
    public abstract ResolverListAdapter getListAdapterForUserHandle(UserHandle userHandle);

    abstract String getMetricsCategory();

    public abstract ResolverListAdapter getPersonalListAdapter();

    public abstract ResolverListAdapter getWorkListAdapter();

    abstract void setupListAdapter(int i);

    protected abstract void showNoPersonalAppsAvailableEmptyState(ResolverListAdapter resolverListAdapter);

    protected abstract void showNoPersonalToWorkIntentsEmptyState(ResolverListAdapter resolverListAdapter);

    protected abstract void showNoWorkAppsAvailableEmptyState(ResolverListAdapter resolverListAdapter);

    protected abstract void showNoWorkToPersonalIntentsEmptyState(ResolverListAdapter resolverListAdapter);

    protected abstract void showWorkProfileOffEmptyState(ResolverListAdapter resolverListAdapter, View.OnClickListener onClickListener);

    /* JADX INFO: Access modifiers changed from: package-private */
    public AbstractMultiProfilePagerAdapter(Context context, int currentPage, UserHandle personalProfileUserHandle, UserHandle workProfileUserHandle) {
        this.mContext = (Context) Objects.requireNonNull(context);
        this.mCurrentPage = currentPage;
        this.mPersonalProfileUserHandle = personalProfileUserHandle;
        this.mWorkProfileUserHandle = workProfileUserHandle;
        UserManager userManager = (UserManager) context.getSystemService(UserManager.class);
        this.mInjector = new AnonymousClass1(userManager);
    }

    /* renamed from: com.android.internal.app.AbstractMultiProfilePagerAdapter$1  reason: invalid class name */
    /* loaded from: classes4.dex */
    class AnonymousClass1 implements Injector {
        final /* synthetic */ UserManager val$userManager;

        AnonymousClass1(UserManager userManager) {
            this.val$userManager = userManager;
        }

        @Override // com.android.internal.app.AbstractMultiProfilePagerAdapter.Injector
        public boolean hasCrossProfileIntents(List<Intent> intents, int sourceUserId, int targetUserId) {
            return AbstractMultiProfilePagerAdapter.this.hasCrossProfileIntents(intents, sourceUserId, targetUserId);
        }

        @Override // com.android.internal.app.AbstractMultiProfilePagerAdapter.Injector
        public boolean isQuietModeEnabled(UserHandle workProfileUserHandle) {
            return this.val$userManager.isQuietModeEnabled(workProfileUserHandle);
        }

        @Override // com.android.internal.app.AbstractMultiProfilePagerAdapter.Injector
        public void requestQuietModeEnabled(final boolean enabled, final UserHandle workProfileUserHandle) {
            Executor executor = AsyncTask.THREAD_POOL_EXECUTOR;
            final UserManager userManager = this.val$userManager;
            executor.execute(new Runnable() { // from class: com.android.internal.app.AbstractMultiProfilePagerAdapter$1$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    UserManager.this.requestQuietModeEnabled(enabled, workProfileUserHandle);
                }
            });
            AbstractMultiProfilePagerAdapter.this.mIsWaitingToEnableWorkProfile = true;
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void markWorkProfileEnabledBroadcastReceived() {
        this.mIsWaitingToEnableWorkProfile = false;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public boolean isWaitingToEnableWorkProfile() {
        return this.mIsWaitingToEnableWorkProfile;
    }

    public void setInjector(Injector injector) {
        this.mInjector = injector;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public boolean isQuietModeEnabled(UserHandle workProfileUserHandle) {
        return this.mInjector.isQuietModeEnabled(workProfileUserHandle);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setOnProfileSelectedListener(OnProfileSelectedListener listener) {
        this.mOnProfileSelectedListener = listener;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setOnSwitchOnWorkSelectedListener(OnSwitchOnWorkSelectedListener listener) {
        this.mOnSwitchOnWorkSelectedListener = listener;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Context getContext() {
        return this.mContext;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setupViewPager(ViewPager viewPager) {
        viewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() { // from class: com.android.internal.app.AbstractMultiProfilePagerAdapter.2
            @Override // com.android.internal.widget.ViewPager.SimpleOnPageChangeListener, com.android.internal.widget.ViewPager.OnPageChangeListener
            public void onPageSelected(int position) {
                AbstractMultiProfilePagerAdapter.this.mCurrentPage = position;
                if (!AbstractMultiProfilePagerAdapter.this.mLoadedPages.contains(Integer.valueOf(position))) {
                    AbstractMultiProfilePagerAdapter.this.rebuildActiveTab(true);
                    AbstractMultiProfilePagerAdapter.this.mLoadedPages.add(Integer.valueOf(position));
                }
                if (AbstractMultiProfilePagerAdapter.this.mOnProfileSelectedListener != null) {
                    AbstractMultiProfilePagerAdapter.this.mOnProfileSelectedListener.onProfileSelected(position);
                }
            }

            @Override // com.android.internal.widget.ViewPager.SimpleOnPageChangeListener, com.android.internal.widget.ViewPager.OnPageChangeListener
            public void onPageScrollStateChanged(int state) {
                if (AbstractMultiProfilePagerAdapter.this.mOnProfileSelectedListener != null) {
                    AbstractMultiProfilePagerAdapter.this.mOnProfileSelectedListener.onProfilePageStateChanged(state);
                }
            }
        });
        viewPager.setAdapter(this);
        viewPager.setCurrentItem(this.mCurrentPage);
        this.mLoadedPages.add(Integer.valueOf(this.mCurrentPage));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void clearInactiveProfileCache() {
        if (this.mLoadedPages.size() == 1) {
            return;
        }
        this.mLoadedPages.remove(Integer.valueOf(1 - this.mCurrentPage));
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.internal.widget.PagerAdapter
    public ViewGroup instantiateItem(ViewGroup container, int position) {
        ProfileDescriptor profileDescriptor = getItem(position);
        container.addView(profileDescriptor.rootView);
        return profileDescriptor.rootView;
    }

    @Override // com.android.internal.widget.PagerAdapter
    public void destroyItem(ViewGroup container, int position, Object view) {
        container.removeView((View) view);
    }

    @Override // com.android.internal.widget.PagerAdapter
    public int getCount() {
        return getItemCount();
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public int getCurrentPage() {
        return this.mCurrentPage;
    }

    public UserHandle getCurrentUserHandle() {
        return getActiveListAdapter().mResolverListController.getUserHandle();
    }

    @Override // com.android.internal.widget.PagerAdapter
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    @Override // com.android.internal.widget.PagerAdapter
    public CharSequence getPageTitle(int position) {
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean rebuildActiveTab(boolean doPostProcessing) {
        Trace.beginSection("MultiProfilePagerAdapter#rebuildActiveTab");
        boolean result = rebuildTab(getActiveListAdapter(), doPostProcessing);
        Trace.endSection();
        return result;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean rebuildInactiveTab(boolean doPostProcessing) {
        Trace.beginSection("MultiProfilePagerAdapter#rebuildInactiveTab");
        if (getItemCount() == 1) {
            Trace.endSection();
            return false;
        }
        boolean result = rebuildTab(getInactiveListAdapter(), doPostProcessing);
        Trace.endSection();
        return result;
    }

    private int userHandleToPageIndex(UserHandle userHandle) {
        if (userHandle.equals(getPersonalListAdapter().mResolverListController.getUserHandle())) {
            return 0;
        }
        return 1;
    }

    private boolean rebuildTab(ResolverListAdapter activeListAdapter, boolean doPostProcessing) {
        if (shouldShowNoCrossProfileIntentsEmptyState(activeListAdapter)) {
            activeListAdapter.postListReadyRunnable(doPostProcessing, true);
            return false;
        }
        return activeListAdapter.rebuildList(doPostProcessing);
    }

    private boolean shouldShowNoCrossProfileIntentsEmptyState(ResolverListAdapter activeListAdapter) {
        UserHandle listUserHandle = activeListAdapter.getUserHandle();
        return (UserHandle.myUserId() == listUserHandle.getIdentifier() || !allowShowNoCrossProfileIntentsEmptyState() || this.mInjector.hasCrossProfileIntents(activeListAdapter.getIntents(), UserHandle.myUserId(), listUserHandle.getIdentifier())) ? false : true;
    }

    boolean allowShowNoCrossProfileIntentsEmptyState() {
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void showEmptyResolverListEmptyState(ResolverListAdapter listAdapter) {
        if (maybeShowNoCrossProfileIntentsEmptyState(listAdapter) || maybeShowWorkProfileOffEmptyState(listAdapter)) {
            return;
        }
        maybeShowNoAppsAvailableEmptyState(listAdapter);
    }

    private boolean maybeShowNoCrossProfileIntentsEmptyState(ResolverListAdapter listAdapter) {
        if (shouldShowNoCrossProfileIntentsEmptyState(listAdapter)) {
            if (listAdapter.getUserHandle().equals(this.mPersonalProfileUserHandle)) {
                DevicePolicyEventLogger.createEvent(158).setStrings(getMetricsCategory()).write();
                showNoWorkToPersonalIntentsEmptyState(listAdapter);
            } else {
                DevicePolicyEventLogger.createEvent(159).setStrings(getMetricsCategory()).write();
                showNoPersonalToWorkIntentsEmptyState(listAdapter);
            }
            return true;
        }
        return false;
    }

    private boolean maybeShowWorkProfileOffEmptyState(final ResolverListAdapter listAdapter) {
        UserHandle listUserHandle = listAdapter.getUserHandle();
        if (listUserHandle.equals(this.mWorkProfileUserHandle) && this.mInjector.isQuietModeEnabled(this.mWorkProfileUserHandle) && listAdapter.getCount() != 0) {
            DevicePolicyEventLogger.createEvent(157).setStrings(getMetricsCategory()).write();
            showWorkProfileOffEmptyState(listAdapter, new View.OnClickListener() { // from class: com.android.internal.app.AbstractMultiProfilePagerAdapter$$ExternalSyntheticLambda0
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    AbstractMultiProfilePagerAdapter.this.m6353x2fd14ddb(listAdapter, view);
                }
            });
            return true;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$maybeShowWorkProfileOffEmptyState$0$com-android-internal-app-AbstractMultiProfilePagerAdapter  reason: not valid java name */
    public /* synthetic */ void m6353x2fd14ddb(ResolverListAdapter listAdapter, View v) {
        ProfileDescriptor descriptor = getItem(userHandleToPageIndex(listAdapter.getUserHandle()));
        showSpinner(descriptor.getEmptyStateView());
        OnSwitchOnWorkSelectedListener onSwitchOnWorkSelectedListener = this.mOnSwitchOnWorkSelectedListener;
        if (onSwitchOnWorkSelectedListener != null) {
            onSwitchOnWorkSelectedListener.onSwitchOnWorkSelected();
        }
        this.mInjector.requestQuietModeEnabled(false, this.mWorkProfileUserHandle);
    }

    private void maybeShowNoAppsAvailableEmptyState(ResolverListAdapter listAdapter) {
        UserHandle listUserHandle = listAdapter.getUserHandle();
        if (this.mWorkProfileUserHandle != null && (UserHandle.myUserId() == listUserHandle.getIdentifier() || !hasAppsInOtherProfile(listAdapter))) {
            DevicePolicyEventLogger.createEvent(160).setStrings(getMetricsCategory()).setBoolean(listUserHandle == this.mPersonalProfileUserHandle).write();
            if (listUserHandle == this.mPersonalProfileUserHandle) {
                showNoPersonalAppsAvailableEmptyState(listAdapter);
            } else {
                showNoWorkAppsAvailableEmptyState(listAdapter);
            }
        } else if (this.mWorkProfileUserHandle == null) {
            showConsumerUserNoAppsAvailableEmptyState(listAdapter);
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void showEmptyState(ResolverListAdapter activeListAdapter, String title, String subtitle) {
        showEmptyState(activeListAdapter, title, subtitle, null);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void showEmptyState(ResolverListAdapter activeListAdapter, String title, String subtitle, View.OnClickListener buttonOnClick) {
        ProfileDescriptor descriptor = getItem(userHandleToPageIndex(activeListAdapter.getUserHandle()));
        descriptor.rootView.findViewById(R.id.resolver_list).setVisibility(8);
        ViewGroup emptyStateView = descriptor.getEmptyStateView();
        resetViewVisibilitiesForWorkProfileEmptyState(emptyStateView);
        emptyStateView.setVisibility(0);
        View container = emptyStateView.findViewById(R.id.resolver_empty_state_container);
        setupContainerPadding(container);
        TextView titleView = (TextView) emptyStateView.findViewById(R.id.resolver_empty_state_title);
        titleView.setText(title);
        TextView subtitleView = (TextView) emptyStateView.findViewById(R.id.resolver_empty_state_subtitle);
        if (subtitle != null) {
            subtitleView.setVisibility(0);
            subtitleView.setText(subtitle);
        } else {
            subtitleView.setVisibility(8);
        }
        Button button = (Button) emptyStateView.findViewById(R.id.resolver_empty_state_button);
        button.setVisibility(buttonOnClick != null ? 0 : 8);
        button.setOnClickListener(buttonOnClick);
        activeListAdapter.markTabLoaded();
    }

    protected void setupContainerPadding(View container) {
    }

    private void showConsumerUserNoAppsAvailableEmptyState(ResolverListAdapter activeListAdapter) {
        ProfileDescriptor descriptor = getItem(userHandleToPageIndex(activeListAdapter.getUserHandle()));
        descriptor.rootView.findViewById(R.id.resolver_list).setVisibility(8);
        View emptyStateView = descriptor.getEmptyStateView();
        resetViewVisibilitiesForConsumerUserEmptyState(emptyStateView);
        emptyStateView.setVisibility(0);
        activeListAdapter.markTabLoaded();
    }

    private boolean isSpinnerShowing(View emptyStateView) {
        return emptyStateView.findViewById(R.id.resolver_empty_state_progress).getVisibility() == 0;
    }

    private void showSpinner(View emptyStateView) {
        emptyStateView.findViewById(R.id.resolver_empty_state_title).setVisibility(4);
        emptyStateView.findViewById(R.id.resolver_empty_state_button).setVisibility(4);
        emptyStateView.findViewById(R.id.resolver_empty_state_progress).setVisibility(0);
        emptyStateView.findViewById(16908292).setVisibility(8);
    }

    private void resetViewVisibilitiesForWorkProfileEmptyState(View emptyStateView) {
        emptyStateView.findViewById(R.id.resolver_empty_state_title).setVisibility(0);
        emptyStateView.findViewById(R.id.resolver_empty_state_subtitle).setVisibility(0);
        emptyStateView.findViewById(R.id.resolver_empty_state_button).setVisibility(4);
        emptyStateView.findViewById(R.id.resolver_empty_state_progress).setVisibility(8);
        emptyStateView.findViewById(16908292).setVisibility(8);
    }

    private void resetViewVisibilitiesForConsumerUserEmptyState(View emptyStateView) {
        emptyStateView.findViewById(R.id.resolver_empty_state_title).setVisibility(8);
        emptyStateView.findViewById(R.id.resolver_empty_state_subtitle).setVisibility(8);
        emptyStateView.findViewById(R.id.resolver_empty_state_button).setVisibility(8);
        emptyStateView.findViewById(R.id.resolver_empty_state_progress).setVisibility(8);
        emptyStateView.findViewById(16908292).setVisibility(0);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void showListView(ResolverListAdapter activeListAdapter) {
        ProfileDescriptor descriptor = getItem(userHandleToPageIndex(activeListAdapter.getUserHandle()));
        descriptor.rootView.findViewById(R.id.resolver_list).setVisibility(0);
        View emptyStateView = descriptor.rootView.findViewById(R.id.resolver_empty_state);
        emptyStateView.setVisibility(8);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean hasCrossProfileIntents(List<Intent> intents, int source, int target) {
        IPackageManager packageManager = AppGlobals.getPackageManager();
        ContentResolver contentResolver = this.mContext.getContentResolver();
        for (Intent intent : intents) {
            if (IntentForwarderActivity.canForward(intent, source, target, packageManager, contentResolver) != null) {
                return true;
            }
        }
        return false;
    }

    private boolean hasAppsInOtherProfile(ResolverListAdapter adapter) {
        if (this.mWorkProfileUserHandle == null) {
            return false;
        }
        List<ResolverActivity.ResolvedComponentInfo> resolversForIntent = adapter.getResolversForUser(UserHandle.of(UserHandle.myUserId()));
        for (ResolverActivity.ResolvedComponentInfo info : resolversForIntent) {
            ResolveInfo resolveInfo = info.getResolveInfoAt(0);
            if (resolveInfo.targetUserId != -2) {
                return true;
            }
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean shouldShowEmptyStateScreen(ResolverListAdapter listAdapter) {
        int count = listAdapter.getUnfilteredCount();
        return (count == 0 && listAdapter.getPlaceholderCount() == 0) || (listAdapter.getUserHandle().equals(this.mWorkProfileUserHandle) && isQuietModeEnabled(this.mWorkProfileUserHandle));
    }

    /* JADX INFO: Access modifiers changed from: protected */
    /* loaded from: classes4.dex */
    public class ProfileDescriptor {
        private final ViewGroup mEmptyStateView;
        final ViewGroup rootView;

        /* JADX INFO: Access modifiers changed from: package-private */
        public ProfileDescriptor(ViewGroup rootView) {
            this.rootView = rootView;
            this.mEmptyStateView = (ViewGroup) rootView.findViewById(R.id.resolver_empty_state);
        }

        /* JADX INFO: Access modifiers changed from: protected */
        public ViewGroup getEmptyStateView() {
            return this.mEmptyStateView;
        }
    }
}
