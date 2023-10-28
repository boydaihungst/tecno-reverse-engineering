package com.android.internal.app;

import android.app.Activity;
import android.app.ActivityThread;
import android.app.AppGlobals;
import android.app.admin.DevicePolicyManager;
import android.app.admin.DevicePolicyResources;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.UserInfo;
import android.metrics.LogMaker;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.util.Slog;
import android.widget.Toast;
import com.android.internal.R;
import com.android.internal.app.IntentForwarderActivity;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.nano.MetricsProto;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
/* loaded from: classes4.dex */
public class IntentForwarderActivity extends Activity {
    private static final String TEL_SCHEME = "tel";
    protected ExecutorService mExecutorService;
    private Injector mInjector;
    private MetricsLogger mMetricsLogger;
    public static String TAG = "IntentForwarderActivity";
    public static String FORWARD_INTENT_TO_PARENT = "com.android.internal.app.ForwardIntentToParent";
    public static String FORWARD_INTENT_TO_MANAGED_PROFILE = "com.android.internal.app.ForwardIntentToManagedProfile";
    private static final Set<String> ALLOWED_TEXT_MESSAGE_SCHEMES = new HashSet(Arrays.asList(Context.SMS_SERVICE, "smsto", "mms", "mmsto"));
    private static final ComponentName RESOLVER_COMPONENT_NAME = new ComponentName("android", ResolverActivity.class.getName());
    private static final ComponentName TRANSSION_RESOLVER_COMPONENT_NAME = new ComponentName("com.transsion.resolver", "com.transsion.resolver.ResolverActivity");

    /* loaded from: classes4.dex */
    public interface Injector {
        IPackageManager getIPackageManager();

        PackageManager getPackageManager();

        UserManager getUserManager();

        CompletableFuture<ResolveInfo> resolveActivityAsUser(Intent intent, int i, int i2);

        void showToast(String str, int i);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // android.app.Activity
    public void onDestroy() {
        super.onDestroy();
        this.mExecutorService.shutdown();
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // android.app.Activity
    public void onCreate(Bundle savedInstanceState) {
        final String userMessage;
        int targetUserId;
        super.onCreate(savedInstanceState);
        this.mInjector = createInjector();
        this.mExecutorService = Executors.newSingleThreadExecutor();
        final Intent intentReceived = getIntent();
        final String className = intentReceived.getComponent().getClassName();
        if (className.equals(FORWARD_INTENT_TO_PARENT)) {
            String userMessage2 = getForwardToPersonalMessage();
            int targetUserId2 = getProfileParent();
            getMetricsLogger().write(new LogMaker((int) MetricsProto.MetricsEvent.ACTION_SWITCH_SHARE_PROFILE).setSubtype(1));
            userMessage = userMessage2;
            targetUserId = targetUserId2;
        } else {
            String userMessage3 = FORWARD_INTENT_TO_MANAGED_PROFILE;
            if (className.equals(userMessage3)) {
                String userMessage4 = getForwardToWorkMessage();
                int targetUserId3 = getManagedProfile();
                getMetricsLogger().write(new LogMaker((int) MetricsProto.MetricsEvent.ACTION_SWITCH_SHARE_PROFILE).setSubtype(2));
                userMessage = userMessage4;
                targetUserId = targetUserId3;
            } else {
                String userMessage5 = TAG;
                Slog.wtf(userMessage5, IntentForwarderActivity.class.getName() + " cannot be called directly");
                userMessage = null;
                targetUserId = -10000;
            }
        }
        if (targetUserId == -10000) {
            finish();
        } else if (Intent.ACTION_CHOOSER.equals(intentReceived.getAction())) {
            launchChooserActivityWithCorrectTab(intentReceived, className);
        } else {
            final int callingUserId = getUserId();
            final Intent newIntent = canForward(intentReceived, getUserId(), targetUserId, this.mInjector.getIPackageManager(), getContentResolver());
            if (newIntent == null) {
                Slog.wtf(TAG, "the intent: " + intentReceived + " cannot be forwarded from user " + callingUserId + " to user " + targetUserId);
                finish();
                return;
            }
            newIntent.prepareToLeaveUser(callingUserId);
            CompletableFuture<ResolveInfo> targetResolveInfoFuture = this.mInjector.resolveActivityAsUser(newIntent, 65536, targetUserId);
            final int i = targetUserId;
            targetResolveInfoFuture.thenApplyAsync(new Function() { // from class: com.android.internal.app.IntentForwarderActivity$$ExternalSyntheticLambda0
                @Override // java.util.function.Function
                public final Object apply(Object obj) {
                    return IntentForwarderActivity.this.m6427xca0e24d3(intentReceived, className, newIntent, callingUserId, i, (ResolveInfo) obj);
                }
            }, (Executor) this.mExecutorService).thenAcceptAsync((Consumer<? super U>) new Consumer() { // from class: com.android.internal.app.IntentForwarderActivity$$ExternalSyntheticLambda1
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    IntentForwarderActivity.this.m6428xbbb7caf2(intentReceived, userMessage, (ResolveInfo) obj);
                }
            }, getApplicationContext().getMainExecutor());
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onCreate$0$com-android-internal-app-IntentForwarderActivity  reason: not valid java name */
    public /* synthetic */ ResolveInfo m6427xca0e24d3(Intent intentReceived, String className, Intent newIntent, int callingUserId, int targetUserId, ResolveInfo targetResolveInfo) {
        if (isResolverActivityResolveInfo(targetResolveInfo)) {
            launchResolverActivityWithCorrectTab(intentReceived, className, newIntent, callingUserId, targetUserId);
            return targetResolveInfo;
        }
        startActivityAsCaller(newIntent, targetUserId);
        return targetResolveInfo;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onCreate$1$com-android-internal-app-IntentForwarderActivity  reason: not valid java name */
    public /* synthetic */ void m6428xbbb7caf2(Intent intentReceived, String userMessage, ResolveInfo result) {
        maybeShowDisclosure(intentReceived, result, userMessage);
        finish();
    }

    private String getForwardToPersonalMessage() {
        return ((DevicePolicyManager) getSystemService(DevicePolicyManager.class)).getResources().getString(DevicePolicyResources.Strings.Core.FORWARD_INTENT_TO_PERSONAL, new Supplier() { // from class: com.android.internal.app.IntentForwarderActivity$$ExternalSyntheticLambda3
            @Override // java.util.function.Supplier
            public final Object get() {
                return IntentForwarderActivity.this.m6425x2bf33889();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getForwardToPersonalMessage$2$com-android-internal-app-IntentForwarderActivity  reason: not valid java name */
    public /* synthetic */ String m6425x2bf33889() {
        return getString(R.string.forward_intent_to_owner);
    }

    private String getForwardToWorkMessage() {
        return ((DevicePolicyManager) getSystemService(DevicePolicyManager.class)).getResources().getString(DevicePolicyResources.Strings.Core.FORWARD_INTENT_TO_WORK, new Supplier() { // from class: com.android.internal.app.IntentForwarderActivity$$ExternalSyntheticLambda2
            @Override // java.util.function.Supplier
            public final Object get() {
                return IntentForwarderActivity.this.m6426xadfe83b9();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getForwardToWorkMessage$3$com-android-internal-app-IntentForwarderActivity  reason: not valid java name */
    public /* synthetic */ String m6426xadfe83b9() {
        return getString(R.string.forward_intent_to_work);
    }

    private boolean isIntentForwarderResolveInfo(ResolveInfo resolveInfo) {
        ActivityInfo activityInfo;
        if (resolveInfo == null || (activityInfo = resolveInfo.activityInfo) == null || !"android".equals(activityInfo.packageName)) {
            return false;
        }
        return activityInfo.name.equals(FORWARD_INTENT_TO_PARENT) || activityInfo.name.equals(FORWARD_INTENT_TO_MANAGED_PROFILE);
    }

    private boolean isResolverActivityResolveInfo(ResolveInfo resolveInfo) {
        return (resolveInfo == null || resolveInfo.activityInfo == null || (!RESOLVER_COMPONENT_NAME.equals(resolveInfo.activityInfo.getComponentName()) && !TRANSSION_RESOLVER_COMPONENT_NAME.equals(resolveInfo.activityInfo.getComponentName()))) ? false : true;
    }

    private void maybeShowDisclosure(Intent intentReceived, ResolveInfo resolveInfo, String message) {
        if (shouldShowDisclosure(resolveInfo, intentReceived) && message != null) {
            this.mInjector.showToast(message, 1);
        }
    }

    private void startActivityAsCaller(Intent newIntent, int userId) {
        try {
            startActivityAsCaller(newIntent, null, false, userId);
        } catch (RuntimeException e) {
            Slog.wtf(TAG, "Unable to launch as UID " + getLaunchedFromUid() + " package " + getLaunchedFromPackage() + ", while running in " + ActivityThread.currentProcessName(), e);
        }
    }

    private void launchChooserActivityWithCorrectTab(Intent intentReceived, String className) {
        int selectedProfile = findSelectedProfile(className);
        sanitizeIntent(intentReceived);
        intentReceived.putExtra("com.android.internal.app.ResolverActivity.EXTRA_SELECTED_PROFILE", selectedProfile);
        Intent innerIntent = (Intent) intentReceived.getParcelableExtra(Intent.EXTRA_INTENT);
        if (innerIntent == null) {
            Slog.wtf(TAG, "Cannot start a chooser intent with no extra android.intent.extra.INTENT");
            return;
        }
        sanitizeIntent(innerIntent);
        startActivityAsCaller(intentReceived, null, false, getUserId());
        finish();
    }

    private void launchResolverActivityWithCorrectTab(Intent intentReceived, String className, Intent newIntent, int callingUserId, int targetUserId) {
        ResolveInfo callingResolveInfo = this.mInjector.resolveActivityAsUser(newIntent, 65536, callingUserId).join();
        int userId = isIntentForwarderResolveInfo(callingResolveInfo) ? targetUserId : callingUserId;
        int selectedProfile = findSelectedProfile(className);
        sanitizeIntent(intentReceived);
        intentReceived.putExtra("com.android.internal.app.ResolverActivity.EXTRA_SELECTED_PROFILE", selectedProfile);
        intentReceived.putExtra("com.android.internal.app.ResolverActivity.EXTRA_CALLING_USER", UserHandle.of(callingUserId));
        startActivityAsCaller(intentReceived, null, false, userId);
        finish();
    }

    private int findSelectedProfile(String className) {
        if (className.equals(FORWARD_INTENT_TO_PARENT)) {
            return 0;
        }
        if (className.equals(FORWARD_INTENT_TO_MANAGED_PROFILE)) {
            return 1;
        }
        return -1;
    }

    private boolean shouldShowDisclosure(ResolveInfo ri, Intent intent) {
        if (isDeviceProvisioned()) {
            if (ri == null || ri.activityInfo == null) {
                return true;
            }
            if (ri.activityInfo.applicationInfo.isSystemApp() && (isDialerIntent(intent) || isTextMessageIntent(intent))) {
                return false;
            }
            return true ^ isTargetResolverOrChooserActivity(ri.activityInfo);
        }
        return false;
    }

    private boolean isDeviceProvisioned() {
        return Settings.Global.getInt(getContentResolver(), "device_provisioned", 0) != 0;
    }

    private boolean isTextMessageIntent(Intent intent) {
        return (Intent.ACTION_SENDTO.equals(intent.getAction()) || isViewActionIntent(intent)) && ALLOWED_TEXT_MESSAGE_SCHEMES.contains(intent.getScheme());
    }

    private boolean isDialerIntent(Intent intent) {
        return Intent.ACTION_DIAL.equals(intent.getAction()) || Intent.ACTION_CALL.equals(intent.getAction()) || Intent.ACTION_CALL_PRIVILEGED.equals(intent.getAction()) || Intent.ACTION_CALL_EMERGENCY.equals(intent.getAction()) || (isViewActionIntent(intent) && "tel".equals(intent.getScheme()));
    }

    private boolean isViewActionIntent(Intent intent) {
        return "android.intent.action.VIEW".equals(intent.getAction()) && intent.hasCategory(Intent.CATEGORY_BROWSABLE);
    }

    private boolean isTargetResolverOrChooserActivity(ActivityInfo activityInfo) {
        if ("android".equals(activityInfo.packageName)) {
            return ResolverActivity.class.getName().equals(activityInfo.name) || ChooserActivity.class.getName().equals(activityInfo.name);
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static Intent canForward(Intent incomingIntent, int sourceUserId, int targetUserId, IPackageManager packageManager, ContentResolver contentResolver) {
        Intent forwardIntent = new Intent(incomingIntent);
        forwardIntent.addFlags(50331648);
        sanitizeIntent(forwardIntent);
        Intent intentToCheck = forwardIntent;
        if (Intent.ACTION_CHOOSER.equals(forwardIntent.getAction())) {
            return null;
        }
        if (forwardIntent.getSelector() != null) {
            intentToCheck = forwardIntent.getSelector();
        }
        String resolvedType = intentToCheck.resolveTypeIfNeeded(contentResolver);
        sanitizeIntent(intentToCheck);
        try {
        } catch (RemoteException e) {
            Slog.e(TAG, "PackageManagerService is dead?");
        }
        if (packageManager.canForwardTo(intentToCheck, resolvedType, sourceUserId, targetUserId)) {
            return forwardIntent;
        }
        return null;
    }

    private int getManagedProfile() {
        List<UserInfo> relatedUsers = this.mInjector.getUserManager().getProfiles(UserHandle.myUserId());
        for (UserInfo userInfo : relatedUsers) {
            if (userInfo.isManagedProfile()) {
                return userInfo.id;
            }
        }
        Slog.wtf(TAG, FORWARD_INTENT_TO_MANAGED_PROFILE + " has been called, but there is no managed profile");
        return -10000;
    }

    private int getProfileParent() {
        UserInfo parent = this.mInjector.getUserManager().getProfileParent(UserHandle.myUserId());
        if (parent == null) {
            Slog.wtf(TAG, FORWARD_INTENT_TO_PARENT + " has been called, but there is no parent");
            return -10000;
        }
        return parent.id;
    }

    private static void sanitizeIntent(Intent intent) {
        intent.setPackage(null);
        intent.setComponent(null);
    }

    protected MetricsLogger getMetricsLogger() {
        if (this.mMetricsLogger == null) {
            this.mMetricsLogger = new MetricsLogger();
        }
        return this.mMetricsLogger;
    }

    protected Injector createInjector() {
        return new InjectorImpl();
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes4.dex */
    public class InjectorImpl implements Injector {
        private InjectorImpl() {
        }

        @Override // com.android.internal.app.IntentForwarderActivity.Injector
        public IPackageManager getIPackageManager() {
            return AppGlobals.getPackageManager();
        }

        @Override // com.android.internal.app.IntentForwarderActivity.Injector
        public UserManager getUserManager() {
            return (UserManager) IntentForwarderActivity.this.getSystemService(UserManager.class);
        }

        @Override // com.android.internal.app.IntentForwarderActivity.Injector
        public PackageManager getPackageManager() {
            return IntentForwarderActivity.this.getPackageManager();
        }

        @Override // com.android.internal.app.IntentForwarderActivity.Injector
        public CompletableFuture<ResolveInfo> resolveActivityAsUser(final Intent intent, final int flags, final int userId) {
            return CompletableFuture.supplyAsync(new Supplier() { // from class: com.android.internal.app.IntentForwarderActivity$InjectorImpl$$ExternalSyntheticLambda0
                @Override // java.util.function.Supplier
                public final Object get() {
                    return IntentForwarderActivity.InjectorImpl.this.m6429x12e3e2c8(intent, flags, userId);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$resolveActivityAsUser$0$com-android-internal-app-IntentForwarderActivity$InjectorImpl  reason: not valid java name */
        public /* synthetic */ ResolveInfo m6429x12e3e2c8(Intent intent, int flags, int userId) {
            return getPackageManager().resolveActivityAsUser(intent, flags, userId);
        }

        @Override // com.android.internal.app.IntentForwarderActivity.Injector
        public void showToast(String message, int duration) {
            Toast.makeText(IntentForwarderActivity.this, message, duration).show();
        }
    }
}
