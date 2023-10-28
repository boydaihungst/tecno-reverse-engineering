package com.android.internal.app;

import android.app.prediction.AppPredictor;
import android.app.prediction.AppTarget;
import android.app.prediction.AppTargetEvent;
import android.app.prediction.AppTargetId;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.Message;
import android.os.UserHandle;
import android.util.Log;
import com.android.internal.app.AbstractResolverComparator;
import com.android.internal.app.AppPredictionServiceResolverComparator;
import com.android.internal.app.ResolverActivity;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes4.dex */
public class AppPredictionServiceResolverComparator extends AbstractResolverComparator {
    private static final String TAG = "APSResolverComparator";
    private final AppPredictor mAppPredictor;
    private AppPredictionServiceComparatorModel mComparatorModel;
    private final Context mContext;
    private final Intent mIntent;
    private final String mReferrerPackage;
    private ResolverRankerServiceResolverComparator mResolverRankerService;
    private final Map<ComponentName, Integer> mTargetRanks;
    private final Map<ComponentName, Integer> mTargetScores;
    private final UserHandle mUser;

    /* JADX INFO: Access modifiers changed from: package-private */
    public AppPredictionServiceResolverComparator(Context context, Intent intent, String referrerPackage, AppPredictor appPredictor, UserHandle user, ChooserActivityLogger chooserActivityLogger) {
        super(context, intent);
        this.mTargetRanks = new HashMap();
        this.mTargetScores = new HashMap();
        this.mContext = context;
        this.mIntent = intent;
        this.mAppPredictor = appPredictor;
        this.mUser = user;
        this.mReferrerPackage = referrerPackage;
        setChooserActivityLogger(chooserActivityLogger);
        this.mComparatorModel = buildUpdatedModel();
    }

    @Override // com.android.internal.app.AbstractResolverComparator
    int compare(ResolveInfo lhs, ResolveInfo rhs) {
        return this.mComparatorModel.getComparator().compare(lhs, rhs);
    }

    @Override // com.android.internal.app.AbstractResolverComparator
    void doCompute(final List<ResolverActivity.ResolvedComponentInfo> targets) {
        if (targets.isEmpty()) {
            this.mHandler.sendEmptyMessage(0);
            return;
        }
        List<AppTarget> appTargets = new ArrayList<>();
        for (ResolverActivity.ResolvedComponentInfo target : targets) {
            appTargets.add(new AppTarget.Builder(new AppTargetId(target.name.flattenToString()), target.name.getPackageName(), this.mUser).setClassName(target.name.getClassName()).build());
        }
        this.mAppPredictor.sortTargets(appTargets, Executors.newSingleThreadExecutor(), new Consumer() { // from class: com.android.internal.app.AppPredictionServiceResolverComparator$$ExternalSyntheticLambda1
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                AppPredictionServiceResolverComparator.this.m6370xe9599cb(targets, (List) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$doCompute$1$com-android-internal-app-AppPredictionServiceResolverComparator  reason: not valid java name */
    public /* synthetic */ void m6370xe9599cb(List targets, List sortedAppTargets) {
        if (sortedAppTargets.isEmpty()) {
            Log.i(TAG, "AppPredictionService disabled. Using resolver.");
            this.mResolverRankerService = new ResolverRankerServiceResolverComparator(this.mContext, this.mIntent, this.mReferrerPackage, new AbstractResolverComparator.AfterCompute() { // from class: com.android.internal.app.AppPredictionServiceResolverComparator$$ExternalSyntheticLambda0
                @Override // com.android.internal.app.AbstractResolverComparator.AfterCompute
                public final void afterCompute() {
                    AppPredictionServiceResolverComparator.this.m6369xc0d621ca();
                }
            }, getChooserActivityLogger());
            this.mComparatorModel = buildUpdatedModel();
            this.mResolverRankerService.compute(targets);
            return;
        }
        Log.i(TAG, "AppPredictionService response received");
        handleResult(sortedAppTargets);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$doCompute$0$com-android-internal-app-AppPredictionServiceResolverComparator  reason: not valid java name */
    public /* synthetic */ void m6369xc0d621ca() {
        this.mHandler.sendEmptyMessage(0);
    }

    @Override // com.android.internal.app.AbstractResolverComparator
    void handleResultMessage(Message msg) {
        if (msg.what == 0 && msg.obj != null) {
            List<AppTarget> sortedAppTargets = (List) msg.obj;
            handleSortedAppTargets(sortedAppTargets);
        } else if (msg.obj == null && this.mResolverRankerService == null) {
            Log.e(TAG, "Unexpected null result");
        }
    }

    private void handleResult(List<AppTarget> sortedAppTargets) {
        if (this.mHandler.hasMessages(1)) {
            handleSortedAppTargets(sortedAppTargets);
            this.mHandler.removeMessages(1);
            afterCompute();
        }
    }

    private void handleSortedAppTargets(List<AppTarget> sortedAppTargets) {
        if (checkAppTargetRankValid(sortedAppTargets)) {
            sortedAppTargets.forEach(new Consumer() { // from class: com.android.internal.app.AppPredictionServiceResolverComparator$$ExternalSyntheticLambda2
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    AppPredictionServiceResolverComparator.this.m6371x19a99e40((AppTarget) obj);
                }
            });
        }
        for (int i = 0; i < sortedAppTargets.size(); i++) {
            ComponentName componentName = new ComponentName(sortedAppTargets.get(i).getPackageName(), sortedAppTargets.get(i).getClassName());
            this.mTargetRanks.put(componentName, Integer.valueOf(i));
            Log.i(TAG, "handleSortedAppTargets, sortedAppTargets #" + i + ": " + componentName);
        }
        this.mComparatorModel = buildUpdatedModel();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$handleSortedAppTargets$2$com-android-internal-app-AppPredictionServiceResolverComparator  reason: not valid java name */
    public /* synthetic */ void m6371x19a99e40(AppTarget target) {
        this.mTargetScores.put(new ComponentName(target.getPackageName(), target.getClassName()), Integer.valueOf(target.getRank()));
    }

    private boolean checkAppTargetRankValid(List<AppTarget> sortedAppTargets) {
        for (AppTarget target : sortedAppTargets) {
            if (target.getRank() != 0) {
                return true;
            }
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.internal.app.AbstractResolverComparator
    public float getScore(ComponentName name) {
        return this.mComparatorModel.getScore(name);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.internal.app.AbstractResolverComparator
    public void updateModel(ComponentName componentName) {
        this.mComparatorModel.notifyOnTargetSelected(componentName);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.internal.app.AbstractResolverComparator
    public void destroy() {
        ResolverRankerServiceResolverComparator resolverRankerServiceResolverComparator = this.mResolverRankerService;
        if (resolverRankerServiceResolverComparator != null) {
            resolverRankerServiceResolverComparator.destroy();
            this.mResolverRankerService = null;
            this.mComparatorModel = buildUpdatedModel();
        }
    }

    private AppPredictionServiceComparatorModel buildUpdatedModel() {
        return new AppPredictionServiceComparatorModel(this.mAppPredictor, this.mResolverRankerService, this.mUser, this.mTargetRanks);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes4.dex */
    public static class AppPredictionServiceComparatorModel implements ResolverComparatorModel {
        private final AppPredictor mAppPredictor;
        private final ResolverRankerServiceResolverComparator mResolverRankerService;
        private final Map<ComponentName, Integer> mTargetRanks;
        private final UserHandle mUser;

        AppPredictionServiceComparatorModel(AppPredictor appPredictor, ResolverRankerServiceResolverComparator resolverRankerService, UserHandle user, Map<ComponentName, Integer> targetRanks) {
            this.mAppPredictor = appPredictor;
            this.mResolverRankerService = resolverRankerService;
            this.mUser = user;
            this.mTargetRanks = targetRanks;
        }

        @Override // com.android.internal.app.ResolverComparatorModel
        public Comparator<ResolveInfo> getComparator() {
            return new Comparator() { // from class: com.android.internal.app.AppPredictionServiceResolverComparator$AppPredictionServiceComparatorModel$$ExternalSyntheticLambda0
                @Override // java.util.Comparator
                public final int compare(Object obj, Object obj2) {
                    return AppPredictionServiceResolverComparator.AppPredictionServiceComparatorModel.this.m6372x83fffa54((ResolveInfo) obj, (ResolveInfo) obj2);
                }
            };
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$getComparator$0$com-android-internal-app-AppPredictionServiceResolverComparator$AppPredictionServiceComparatorModel  reason: not valid java name */
        public /* synthetic */ int m6372x83fffa54(ResolveInfo lhs, ResolveInfo rhs) {
            ResolverRankerServiceResolverComparator resolverRankerServiceResolverComparator = this.mResolverRankerService;
            if (resolverRankerServiceResolverComparator != null) {
                return resolverRankerServiceResolverComparator.compare(lhs, rhs);
            }
            Integer lhsRank = this.mTargetRanks.get(new ComponentName(lhs.activityInfo.packageName, lhs.activityInfo.name));
            Integer rhsRank = this.mTargetRanks.get(new ComponentName(rhs.activityInfo.packageName, rhs.activityInfo.name));
            if (lhsRank == null && rhsRank == null) {
                return 0;
            }
            if (lhsRank == null) {
                return -1;
            }
            if (rhsRank == null) {
                return 1;
            }
            return lhsRank.intValue() - rhsRank.intValue();
        }

        @Override // com.android.internal.app.ResolverComparatorModel
        public float getScore(ComponentName name) {
            ResolverRankerServiceResolverComparator resolverRankerServiceResolverComparator = this.mResolverRankerService;
            if (resolverRankerServiceResolverComparator != null) {
                return resolverRankerServiceResolverComparator.getScore(name);
            }
            Integer rank = this.mTargetRanks.get(name);
            if (rank == null) {
                Log.w(AppPredictionServiceResolverComparator.TAG, "Score requested for unknown component. Did you call compute yet?");
                return 0.0f;
            }
            int consecutiveSumOfRanks = ((this.mTargetRanks.size() - 1) * this.mTargetRanks.size()) / 2;
            return 1.0f - (rank.intValue() / consecutiveSumOfRanks);
        }

        @Override // com.android.internal.app.ResolverComparatorModel
        public void notifyOnTargetSelected(ComponentName componentName) {
            ResolverRankerServiceResolverComparator resolverRankerServiceResolverComparator = this.mResolverRankerService;
            if (resolverRankerServiceResolverComparator != null) {
                resolverRankerServiceResolverComparator.updateModel(componentName);
            } else {
                this.mAppPredictor.notifyAppTargetEvent(new AppTargetEvent.Builder(new AppTarget.Builder(new AppTargetId(componentName.toString()), componentName.getPackageName(), this.mUser).setClassName(componentName.getClassName()).build(), 1).build());
            }
        }
    }
}
