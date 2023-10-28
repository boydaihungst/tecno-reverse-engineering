package com.android.internal.app;

import android.app.usage.UsageStatsManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.BadParcelableException;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import com.android.internal.app.ResolverActivity;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
/* loaded from: classes4.dex */
public abstract class AbstractResolverComparator implements Comparator<ResolverActivity.ResolvedComponentInfo> {
    private static final boolean DEBUG = true;
    private static final int NUM_OF_TOP_ANNOTATIONS_TO_USE = 3;
    static final int RANKER_RESULT_TIMEOUT = 1;
    static final int RANKER_SERVICE_RESULT = 0;
    private static final String TAG = "AbstractResolverComp";
    private static final int WATCHDOG_TIMEOUT_MILLIS = 500;
    protected AfterCompute mAfterCompute;
    protected String[] mAnnotations;
    private final Comparator<ResolveInfo> mAzComparator;
    private ChooserActivityLogger mChooserActivityLogger;
    protected String mContentType;
    protected final Handler mHandler = new Handler(Looper.getMainLooper()) { // from class: com.android.internal.app.AbstractResolverComparator.1
        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    Log.d(AbstractResolverComparator.TAG, "RANKER_SERVICE_RESULT");
                    if (AbstractResolverComparator.this.mHandler.hasMessages(1)) {
                        AbstractResolverComparator.this.handleResultMessage(msg);
                        AbstractResolverComparator.this.mHandler.removeMessages(1);
                        AbstractResolverComparator.this.afterCompute();
                        return;
                    }
                    return;
                case 1:
                    Log.d(AbstractResolverComparator.TAG, "RANKER_RESULT_TIMEOUT; unbinding services");
                    AbstractResolverComparator.this.mHandler.removeMessages(0);
                    AbstractResolverComparator.this.afterCompute();
                    if (AbstractResolverComparator.this.mChooserActivityLogger != null) {
                        AbstractResolverComparator.this.mChooserActivityLogger.logSharesheetAppShareRankingTimeout();
                        return;
                    }
                    return;
                default:
                    super.handleMessage(msg);
                    return;
            }
        }
    };
    private final boolean mHttp;
    protected final PackageManager mPm;
    protected final UsageStatsManager mUsm;

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes4.dex */
    public interface AfterCompute {
        void afterCompute();
    }

    abstract int compare(ResolveInfo resolveInfo, ResolveInfo resolveInfo2);

    abstract void doCompute(List<ResolverActivity.ResolvedComponentInfo> list);

    /* JADX INFO: Access modifiers changed from: package-private */
    public abstract float getScore(ComponentName componentName);

    abstract void handleResultMessage(Message message);

    public AbstractResolverComparator(Context context, Intent intent) {
        String scheme = intent.getScheme();
        this.mHttp = IntentFilter.SCHEME_HTTP.equals(scheme) || IntentFilter.SCHEME_HTTPS.equals(scheme);
        this.mContentType = intent.getType();
        getContentAnnotations(intent);
        this.mPm = context.getPackageManager();
        this.mUsm = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
        this.mAzComparator = new AzInfoComparator(context);
    }

    private void getContentAnnotations(Intent intent) {
        try {
            ArrayList<String> annotations = intent.getStringArrayListExtra(Intent.EXTRA_CONTENT_ANNOTATIONS);
            if (annotations != null) {
                int size = annotations.size();
                if (size > 3) {
                    size = 3;
                }
                this.mAnnotations = new String[size];
                for (int i = 0; i < size; i++) {
                    this.mAnnotations[i] = annotations.get(i);
                }
            }
        } catch (BadParcelableException e) {
            Log.i(TAG, "Couldn't unparcel intent annotations. Ignoring.");
            this.mAnnotations = new String[0];
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setCallBack(AfterCompute afterCompute) {
        this.mAfterCompute = afterCompute;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setChooserActivityLogger(ChooserActivityLogger chooserActivityLogger) {
        this.mChooserActivityLogger = chooserActivityLogger;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ChooserActivityLogger getChooserActivityLogger() {
        return this.mChooserActivityLogger;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public final void afterCompute() {
        AfterCompute afterCompute = this.mAfterCompute;
        if (afterCompute != null) {
            afterCompute.afterCompute();
        }
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // java.util.Comparator
    public final int compare(ResolverActivity.ResolvedComponentInfo lhsp, ResolverActivity.ResolvedComponentInfo rhsp) {
        ResolveInfo lhs = lhsp.getResolveInfoAt(0);
        ResolveInfo rhs = rhsp.getResolveInfoAt(0);
        boolean lFixedAtTop = lhsp.isFixedAtTop();
        boolean rFixedAtTop = rhsp.isFixedAtTop();
        if (!lFixedAtTop || rFixedAtTop) {
            if (!lFixedAtTop && rFixedAtTop) {
                return 1;
            }
            if (lhs.targetUserId != -2) {
                if (rhs.targetUserId != -2) {
                    return 0;
                }
                return 1;
            } else if (rhs.targetUserId != -2) {
                return -1;
            } else {
                if (this.mHttp) {
                    boolean lhsSpecific = ResolverActivity.isSpecificUriMatch(lhs.match);
                    boolean rhsSpecific = ResolverActivity.isSpecificUriMatch(rhs.match);
                    if (lhsSpecific != rhsSpecific) {
                        if (lhsSpecific) {
                            return -1;
                        }
                        return 1;
                    }
                }
                boolean lPinned = lhsp.isPinned();
                boolean rPinned = rhsp.isPinned();
                if (lPinned && !rPinned) {
                    return -1;
                }
                if (!lPinned && rPinned) {
                    return 1;
                }
                if (lPinned && rPinned) {
                    return this.mAzComparator.compare(lhsp.getResolveInfoAt(0), rhsp.getResolveInfoAt(0));
                }
                return compare(lhs, rhs);
            }
        }
        return -1;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public final void compute(List<ResolverActivity.ResolvedComponentInfo> targets) {
        beforeCompute();
        doCompute(targets);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public final void updateChooserCounts(String packageName, int userId, String action) {
        UsageStatsManager usageStatsManager = this.mUsm;
        if (usageStatsManager != null) {
            usageStatsManager.reportChooserSelection(packageName, userId, this.mContentType, this.mAnnotations, action);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateModel(ComponentName componentName) {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void beforeCompute() {
        Log.d(TAG, "Setting watchdog timer for 500ms");
        Handler handler = this.mHandler;
        if (handler == null) {
            Log.d(TAG, "Error: Handler is Null; Needs to be initialized.");
        } else {
            handler.sendEmptyMessageDelayed(1, 500L);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void destroy() {
        this.mHandler.removeMessages(0);
        this.mHandler.removeMessages(1);
        afterCompute();
        this.mAfterCompute = null;
    }

    /* loaded from: classes4.dex */
    class AzInfoComparator implements Comparator<ResolveInfo> {
        Collator mCollator;

        AzInfoComparator(Context context) {
            this.mCollator = Collator.getInstance(context.getResources().getConfiguration().locale);
        }

        /* JADX DEBUG: Method merged with bridge method */
        @Override // java.util.Comparator
        public int compare(ResolveInfo lhsp, ResolveInfo rhsp) {
            if (lhsp == null) {
                return -1;
            }
            if (rhsp == null) {
                return 1;
            }
            return this.mCollator.compare(lhsp.activityInfo.packageName, rhsp.activityInfo.packageName);
        }
    }
}
