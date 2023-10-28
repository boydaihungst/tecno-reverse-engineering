package com.android.internal.app;

import android.app.usage.UsageStats;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.metrics.LogMaker;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.os.UserHandle;
import android.service.resolver.IResolverRankerResult;
import android.service.resolver.IResolverRankerService;
import android.service.resolver.ResolverRankerService;
import android.service.resolver.ResolverTarget;
import android.util.Log;
import com.android.internal.app.AbstractResolverComparator;
import com.android.internal.app.ResolverActivity;
import com.android.internal.app.ResolverRankerServiceResolverComparator;
import com.android.internal.logging.MetricsLogger;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes4.dex */
public class ResolverRankerServiceResolverComparator extends AbstractResolverComparator {
    private static final int CONNECTION_COST_TIMEOUT_MILLIS = 200;
    private static final boolean DEBUG = false;
    private static final float RECENCY_MULTIPLIER = 2.0f;
    private static final long RECENCY_TIME_PERIOD = 43200000;
    private static final String TAG = "RRSResolverComparator";
    private static final long USAGE_STATS_PERIOD = 604800000;
    private String mAction;
    private final Collator mCollator;
    private ResolverRankerServiceComparatorModel mComparatorModel;
    private CountDownLatch mConnectSignal;
    private ResolverRankerServiceConnection mConnection;
    private Context mContext;
    private final long mCurrentTime;
    private final Object mLock;
    private IResolverRankerService mRanker;
    private ComponentName mRankerServiceName;
    private final String mReferrerPackage;
    private ComponentName mResolvedRankerName;
    private final long mSinceTime;
    private final Map<String, UsageStats> mStats;
    private ArrayList<ResolverTarget> mTargets;
    private final LinkedHashMap<ComponentName, ResolverTarget> mTargetsDict;

    public ResolverRankerServiceResolverComparator(Context context, Intent intent, String referrerPackage, AbstractResolverComparator.AfterCompute afterCompute, ChooserActivityLogger chooserActivityLogger) {
        super(context, intent);
        this.mTargetsDict = new LinkedHashMap<>();
        this.mLock = new Object();
        this.mCollator = Collator.getInstance(context.getResources().getConfiguration().locale);
        this.mReferrerPackage = referrerPackage;
        this.mContext = context;
        long currentTimeMillis = System.currentTimeMillis();
        this.mCurrentTime = currentTimeMillis;
        long j = currentTimeMillis - 604800000;
        this.mSinceTime = j;
        this.mStats = this.mUsm.queryAndAggregateUsageStats(j, currentTimeMillis);
        this.mAction = intent.getAction();
        this.mRankerServiceName = new ComponentName(this.mContext, getClass());
        setCallBack(afterCompute);
        setChooserActivityLogger(chooserActivityLogger);
        this.mComparatorModel = buildUpdatedModel();
    }

    @Override // com.android.internal.app.AbstractResolverComparator
    public void handleResultMessage(Message msg) {
        if (msg.what != 0) {
            return;
        }
        if (msg.obj == null) {
            Log.e(TAG, "Receiving null prediction results.");
            return;
        }
        List<ResolverTarget> receivedTargets = (List) msg.obj;
        if (receivedTargets != null && this.mTargets != null && receivedTargets.size() == this.mTargets.size()) {
            int size = this.mTargets.size();
            boolean isUpdated = false;
            for (int i = 0; i < size; i++) {
                float predictedProb = receivedTargets.get(i).getSelectProbability();
                if (predictedProb != this.mTargets.get(i).getSelectProbability()) {
                    this.mTargets.get(i).setSelectProbability(predictedProb);
                    isUpdated = true;
                }
            }
            if (isUpdated) {
                this.mRankerServiceName = this.mResolvedRankerName;
                this.mComparatorModel = buildUpdatedModel();
                return;
            }
            return;
        }
        Log.e(TAG, "Sizes of sent and received ResolverTargets diff.");
    }

    @Override // com.android.internal.app.AbstractResolverComparator
    public void doCompute(List<ResolverActivity.ResolvedComponentInfo> targets) {
        Iterator<ResolverActivity.ResolvedComponentInfo> it;
        long recentSinceTime = this.mCurrentTime - 43200000;
        Iterator<ResolverActivity.ResolvedComponentInfo> it2 = targets.iterator();
        float mostRecencyScore = 1.0f;
        float mostTimeSpentScore = 1.0f;
        float mostLaunchScore = 1.0f;
        float mostChooserScore = 1.0f;
        while (it2.hasNext()) {
            ResolverActivity.ResolvedComponentInfo target = it2.next();
            ResolverTarget resolverTarget = new ResolverTarget();
            this.mTargetsDict.put(target.name, resolverTarget);
            UsageStats pkStats = this.mStats.get(target.name.getPackageName());
            if (pkStats != null) {
                if (target.name.getPackageName().equals(this.mReferrerPackage)) {
                    it = it2;
                } else if (isPersistentProcess(target)) {
                    it = it2;
                } else {
                    it = it2;
                    float recencyScore = (float) Math.max(pkStats.getLastTimeUsed() - recentSinceTime, 0L);
                    resolverTarget.setRecencyScore(recencyScore);
                    if (recencyScore > mostRecencyScore) {
                        mostRecencyScore = recencyScore;
                    }
                }
                float timeSpentScore = (float) pkStats.getTotalTimeInForeground();
                resolverTarget.setTimeSpentScore(timeSpentScore);
                if (timeSpentScore > mostTimeSpentScore) {
                    mostTimeSpentScore = timeSpentScore;
                }
                float launchScore = pkStats.mLaunchCount;
                resolverTarget.setLaunchScore(launchScore);
                if (launchScore > mostLaunchScore) {
                    mostLaunchScore = launchScore;
                }
                float chooserScore = 0.0f;
                if (pkStats.mChooserCounts != null && this.mAction != null) {
                    if (pkStats.mChooserCounts.get(this.mAction) != null) {
                        chooserScore = pkStats.mChooserCounts.get(this.mAction).getOrDefault(this.mContentType, 0).intValue();
                        if (this.mAnnotations != null) {
                            int size = this.mAnnotations.length;
                            int i = 0;
                            while (i < size) {
                                chooserScore += pkStats.mChooserCounts.get(this.mAction).getOrDefault(this.mAnnotations[i], 0).intValue();
                                i++;
                                size = size;
                                pkStats = pkStats;
                            }
                        }
                    }
                }
                resolverTarget.setChooserScore(chooserScore);
                if (chooserScore > mostChooserScore) {
                    mostChooserScore = chooserScore;
                }
            } else {
                it = it2;
            }
            it2 = it;
        }
        ArrayList<ResolverTarget> arrayList = new ArrayList<>(this.mTargetsDict.values());
        this.mTargets = arrayList;
        Iterator<ResolverTarget> it3 = arrayList.iterator();
        while (it3.hasNext()) {
            ResolverTarget target2 = it3.next();
            float recency = target2.getRecencyScore() / mostRecencyScore;
            setFeatures(target2, recency * recency * RECENCY_MULTIPLIER, target2.getLaunchScore() / mostLaunchScore, target2.getTimeSpentScore() / mostTimeSpentScore, target2.getChooserScore() / mostChooserScore);
            addDefaultSelectProbability(target2);
        }
        predictSelectProbabilities(this.mTargets);
        this.mComparatorModel = buildUpdatedModel();
    }

    @Override // com.android.internal.app.AbstractResolverComparator
    public int compare(ResolveInfo lhs, ResolveInfo rhs) {
        return this.mComparatorModel.getComparator().compare(lhs, rhs);
    }

    @Override // com.android.internal.app.AbstractResolverComparator
    public float getScore(ComponentName name) {
        return this.mComparatorModel.getScore(name);
    }

    @Override // com.android.internal.app.AbstractResolverComparator
    public void updateModel(ComponentName componentName) {
        synchronized (this.mLock) {
            this.mComparatorModel.notifyOnTargetSelected(componentName);
        }
    }

    @Override // com.android.internal.app.AbstractResolverComparator
    public void destroy() {
        this.mHandler.removeMessages(0);
        this.mHandler.removeMessages(1);
        ResolverRankerServiceConnection resolverRankerServiceConnection = this.mConnection;
        if (resolverRankerServiceConnection != null) {
            this.mContext.unbindService(resolverRankerServiceConnection);
            this.mConnection.destroy();
        }
        afterCompute();
    }

    private void initRanker(Context context) {
        synchronized (this.mLock) {
            if (this.mConnection == null || this.mRanker == null) {
                Intent intent = resolveRankerService();
                if (intent == null) {
                    return;
                }
                CountDownLatch countDownLatch = new CountDownLatch(1);
                this.mConnectSignal = countDownLatch;
                ResolverRankerServiceConnection resolverRankerServiceConnection = new ResolverRankerServiceConnection(countDownLatch);
                this.mConnection = resolverRankerServiceConnection;
                context.bindServiceAsUser(intent, resolverRankerServiceConnection, 1, UserHandle.SYSTEM);
            }
        }
    }

    private Intent resolveRankerService() {
        Intent intent = new Intent(ResolverRankerService.SERVICE_INTERFACE);
        List<ResolveInfo> resolveInfos = this.mPm.queryIntentServices(intent, 0);
        for (ResolveInfo resolveInfo : resolveInfos) {
            if (resolveInfo != null && resolveInfo.serviceInfo != null && resolveInfo.serviceInfo.applicationInfo != null) {
                ComponentName componentName = new ComponentName(resolveInfo.serviceInfo.applicationInfo.packageName, resolveInfo.serviceInfo.name);
                try {
                    String perm = this.mPm.getServiceInfo(componentName, 0).permission;
                    if ("android.permission.BIND_RESOLVER_RANKER_SERVICE".equals(perm)) {
                        if (this.mPm.checkPermission("android.permission.PROVIDE_RESOLVER_RANKER_SERVICE", resolveInfo.serviceInfo.packageName) != 0) {
                            Log.w(TAG, "ResolverRankerService " + componentName + " does not hold permission android.permission.PROVIDE_RESOLVER_RANKER_SERVICE - this service will not be queried for ResolverRankerServiceResolverComparator.");
                        } else {
                            this.mResolvedRankerName = componentName;
                            intent.setComponent(componentName);
                            return intent;
                        }
                    } else {
                        Log.w(TAG, "ResolverRankerService " + componentName + " does not require permission android.permission.BIND_RESOLVER_RANKER_SERVICE - this service will not be queried for ResolverRankerServiceResolverComparator. add android:permission=\"android.permission.BIND_RESOLVER_RANKER_SERVICE\" to the <service> tag for " + componentName + " in the manifest.");
                    }
                } catch (PackageManager.NameNotFoundException e) {
                    Log.e(TAG, "Could not look up service " + componentName + "; component name not found");
                }
            }
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes4.dex */
    public class ResolverRankerServiceConnection implements ServiceConnection {
        private final CountDownLatch mConnectSignal;
        public final IResolverRankerResult resolverRankerResult = new IResolverRankerResult.Stub() { // from class: com.android.internal.app.ResolverRankerServiceResolverComparator.ResolverRankerServiceConnection.1
            @Override // android.service.resolver.IResolverRankerResult
            public void sendResult(List<ResolverTarget> targets) throws RemoteException {
                synchronized (ResolverRankerServiceResolverComparator.this.mLock) {
                    Message msg = Message.obtain();
                    msg.what = 0;
                    msg.obj = targets;
                    ResolverRankerServiceResolverComparator.this.mHandler.sendMessage(msg);
                }
            }
        };

        public ResolverRankerServiceConnection(CountDownLatch connectSignal) {
            this.mConnectSignal = connectSignal;
        }

        @Override // android.content.ServiceConnection
        public void onServiceConnected(ComponentName name, IBinder service) {
            synchronized (ResolverRankerServiceResolverComparator.this.mLock) {
                ResolverRankerServiceResolverComparator.this.mRanker = IResolverRankerService.Stub.asInterface(service);
                ResolverRankerServiceResolverComparator resolverRankerServiceResolverComparator = ResolverRankerServiceResolverComparator.this;
                resolverRankerServiceResolverComparator.mComparatorModel = resolverRankerServiceResolverComparator.buildUpdatedModel();
                this.mConnectSignal.countDown();
            }
        }

        @Override // android.content.ServiceConnection
        public void onServiceDisconnected(ComponentName name) {
            synchronized (ResolverRankerServiceResolverComparator.this.mLock) {
                destroy();
            }
        }

        public void destroy() {
            synchronized (ResolverRankerServiceResolverComparator.this.mLock) {
                ResolverRankerServiceResolverComparator.this.mRanker = null;
                ResolverRankerServiceResolverComparator resolverRankerServiceResolverComparator = ResolverRankerServiceResolverComparator.this;
                resolverRankerServiceResolverComparator.mComparatorModel = resolverRankerServiceResolverComparator.buildUpdatedModel();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.internal.app.AbstractResolverComparator
    public void beforeCompute() {
        super.beforeCompute();
        this.mTargetsDict.clear();
        this.mTargets = null;
        this.mRankerServiceName = new ComponentName(this.mContext, getClass());
        this.mComparatorModel = buildUpdatedModel();
        this.mResolvedRankerName = null;
        initRanker(this.mContext);
    }

    private void predictSelectProbabilities(List<ResolverTarget> targets) {
        if (this.mConnection != null) {
            try {
                this.mConnectSignal.await(200L, TimeUnit.MILLISECONDS);
                synchronized (this.mLock) {
                    IResolverRankerService iResolverRankerService = this.mRanker;
                    if (iResolverRankerService != null) {
                        iResolverRankerService.predict(targets, this.mConnection.resolverRankerResult);
                        return;
                    }
                }
            } catch (RemoteException e) {
                Log.e(TAG, "Error in Predict: " + e);
            } catch (InterruptedException e2) {
                Log.e(TAG, "Error in Wait for Service Connection.");
            }
        }
        afterCompute();
    }

    private void addDefaultSelectProbability(ResolverTarget target) {
        float sum = (target.getLaunchScore() * 2.5543f) + (target.getTimeSpentScore() * 2.8412f) + (target.getRecencyScore() * 0.269f) + (target.getChooserScore() * 4.2222f);
        target.setSelectProbability((float) (1.0d / (Math.exp(1.6568f - sum) + 1.0d)));
    }

    private void setFeatures(ResolverTarget target, float recencyScore, float launchScore, float timeSpentScore, float chooserScore) {
        target.setRecencyScore(recencyScore);
        target.setLaunchScore(launchScore);
        target.setTimeSpentScore(timeSpentScore);
        target.setChooserScore(chooserScore);
    }

    static boolean isPersistentProcess(ResolverActivity.ResolvedComponentInfo rci) {
        return (rci == null || rci.getCount() <= 0 || (rci.getResolveInfoAt(0).activityInfo.applicationInfo.flags & 8) == 0) ? false : true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public ResolverRankerServiceComparatorModel buildUpdatedModel() {
        return new ResolverRankerServiceComparatorModel(this.mStats, this.mTargetsDict, this.mTargets, this.mCollator, this.mRanker, this.mRankerServiceName, this.mAnnotations != null, this.mPm);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes4.dex */
    public static class ResolverRankerServiceComparatorModel implements ResolverComparatorModel {
        private final boolean mAnnotationsUsed;
        private final Collator mCollator;
        private final PackageManager mPm;
        private final IResolverRankerService mRanker;
        private final ComponentName mRankerServiceName;
        private final Map<String, UsageStats> mStats;
        private final List<ResolverTarget> mTargets;
        private final Map<ComponentName, ResolverTarget> mTargetsDict;

        ResolverRankerServiceComparatorModel(Map<String, UsageStats> stats, Map<ComponentName, ResolverTarget> targetsDict, List<ResolverTarget> targets, Collator collator, IResolverRankerService ranker, ComponentName rankerServiceName, boolean annotationsUsed, PackageManager pm) {
            this.mStats = stats;
            this.mTargetsDict = targetsDict;
            this.mTargets = targets;
            this.mCollator = collator;
            this.mRanker = ranker;
            this.mRankerServiceName = rankerServiceName;
            this.mAnnotationsUsed = annotationsUsed;
            this.mPm = pm;
        }

        @Override // com.android.internal.app.ResolverComparatorModel
        public Comparator<ResolveInfo> getComparator() {
            return new Comparator() { // from class: com.android.internal.app.ResolverRankerServiceResolverComparator$ResolverRankerServiceComparatorModel$$ExternalSyntheticLambda0
                @Override // java.util.Comparator
                public final int compare(Object obj, Object obj2) {
                    return ResolverRankerServiceResolverComparator.ResolverRankerServiceComparatorModel.this.m6491x533e5dd8((ResolveInfo) obj, (ResolveInfo) obj2);
                }
            };
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$getComparator$0$com-android-internal-app-ResolverRankerServiceResolverComparator$ResolverRankerServiceComparatorModel  reason: not valid java name */
        public /* synthetic */ int m6491x533e5dd8(ResolveInfo lhs, ResolveInfo rhs) {
            int selectProbabilityDiff;
            if (this.mStats != null) {
                ResolverTarget lhsTarget = this.mTargetsDict.get(new ComponentName(lhs.activityInfo.packageName, lhs.activityInfo.name));
                ResolverTarget rhsTarget = this.mTargetsDict.get(new ComponentName(rhs.activityInfo.packageName, rhs.activityInfo.name));
                if (lhsTarget != null && rhsTarget != null && (selectProbabilityDiff = Float.compare(rhsTarget.getSelectProbability(), lhsTarget.getSelectProbability())) != 0) {
                    return selectProbabilityDiff > 0 ? 1 : -1;
                }
            }
            CharSequence sa = lhs.loadLabel(this.mPm);
            if (sa == null) {
                sa = lhs.activityInfo.name;
            }
            CharSequence sb = rhs.loadLabel(this.mPm);
            if (sb == null) {
                sb = rhs.activityInfo.name;
            }
            return this.mCollator.compare(sa.toString().trim(), sb.toString().trim());
        }

        @Override // com.android.internal.app.ResolverComparatorModel
        public float getScore(ComponentName name) {
            ResolverTarget target = this.mTargetsDict.get(name);
            if (target != null) {
                return target.getSelectProbability();
            }
            return 0.0f;
        }

        @Override // com.android.internal.app.ResolverComparatorModel
        public void notifyOnTargetSelected(ComponentName componentName) {
            if (this.mRanker != null) {
                try {
                    int selectedPos = new ArrayList(this.mTargetsDict.keySet()).indexOf(componentName);
                    if (selectedPos >= 0 && this.mTargets != null) {
                        float selectedProbability = getScore(componentName);
                        int order = 0;
                        for (ResolverTarget target : this.mTargets) {
                            if (target.getSelectProbability() > selectedProbability) {
                                order++;
                            }
                        }
                        logMetrics(order);
                        this.mRanker.train(this.mTargets, selectedPos);
                    }
                } catch (RemoteException e) {
                    Log.e(ResolverRankerServiceResolverComparator.TAG, "Error in Train: " + e);
                }
            }
        }

        private void logMetrics(int selectedPos) {
            if (this.mRankerServiceName != null) {
                MetricsLogger metricsLogger = new MetricsLogger();
                LogMaker log = new LogMaker(1085);
                log.setComponentName(this.mRankerServiceName);
                log.addTaggedData(1086, Integer.valueOf(this.mAnnotationsUsed ? 1 : 0));
                log.addTaggedData(1087, Integer.valueOf(selectedPos));
                metricsLogger.write(log);
            }
        }
    }
}
