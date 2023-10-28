package com.android.server.people.prediction;

import android.app.prediction.AppPredictionContext;
import android.app.prediction.AppTarget;
import android.app.prediction.AppTargetEvent;
import android.app.prediction.AppTargetId;
import android.content.Context;
import com.android.server.people.data.DataManager;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
/* loaded from: classes2.dex */
public class AppTargetPredictor {
    private static final String UI_SURFACE_SHARE = "share";
    private final ExecutorService mCallbackExecutor = Executors.newSingleThreadExecutor();
    final int mCallingUserId;
    private final DataManager mDataManager;
    private final AppPredictionContext mPredictionContext;
    private final Consumer<List<AppTarget>> mUpdatePredictionsMethod;

    public static AppTargetPredictor create(AppPredictionContext predictionContext, Consumer<List<AppTarget>> updatePredictionsMethod, DataManager dataManager, int callingUserId, Context context) {
        if (UI_SURFACE_SHARE.equals(predictionContext.getUiSurface())) {
            return new ShareTargetPredictor(predictionContext, updatePredictionsMethod, dataManager, callingUserId, context);
        }
        return new AppTargetPredictor(predictionContext, updatePredictionsMethod, dataManager, callingUserId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public AppTargetPredictor(AppPredictionContext predictionContext, Consumer<List<AppTarget>> updatePredictionsMethod, DataManager dataManager, int callingUserId) {
        this.mPredictionContext = predictionContext;
        this.mUpdatePredictionsMethod = updatePredictionsMethod;
        this.mDataManager = dataManager;
        this.mCallingUserId = callingUserId;
    }

    public void onAppTargetEvent(final AppTargetEvent event) {
        this.mCallbackExecutor.execute(new Runnable() { // from class: com.android.server.people.prediction.AppTargetPredictor$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                AppTargetPredictor.this.m5362xf515a116(event);
            }
        });
    }

    public void onLaunchLocationShown(String launchLocation, List<AppTargetId> targetIds) {
    }

    public void onSortAppTargets(final List<AppTarget> targets, final Consumer<List<AppTarget>> callback) {
        this.mCallbackExecutor.execute(new Runnable() { // from class: com.android.server.people.prediction.AppTargetPredictor$$ExternalSyntheticLambda2
            @Override // java.lang.Runnable
            public final void run() {
                AppTargetPredictor.this.m5363x1c23108e(targets, callback);
            }
        });
    }

    public void onRequestPredictionUpdate() {
        this.mCallbackExecutor.execute(new Runnable() { // from class: com.android.server.people.prediction.AppTargetPredictor$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                AppTargetPredictor.this.predictTargets();
            }
        });
    }

    public Consumer<List<AppTarget>> getUpdatePredictionsMethod() {
        return this.mUpdatePredictionsMethod;
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: reportAppTargetEvent */
    public void m5362xf515a116(AppTargetEvent event) {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void predictTargets() {
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: sortTargets */
    public void m5363x1c23108e(List<AppTarget> targets, Consumer<List<AppTarget>> callback) {
        callback.accept(targets);
    }

    void destroy() {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public AppPredictionContext getPredictionContext() {
        return this.mPredictionContext;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public DataManager getDataManager() {
        return this.mDataManager;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updatePredictions(List<AppTarget> targets) {
        this.mUpdatePredictionsMethod.accept(targets);
    }
}
