package com.android.server.pm.resolution;

import android.util.ArrayMap;
import com.android.server.pm.UserManagerService;
import com.android.server.pm.UserNeedsBadgingCache;
import com.android.server.pm.resolution.ComponentResolver;
/* loaded from: classes2.dex */
public class ComponentResolverSnapshot extends ComponentResolverBase {
    public ComponentResolverSnapshot(ComponentResolver orig, UserNeedsBadgingCache userNeedsBadgingCache) {
        super(UserManagerService.getInstance());
        this.mActivities = new ComponentResolver.ActivityIntentResolver(orig.mActivities, this.mUserManager, userNeedsBadgingCache);
        this.mProviders = new ComponentResolver.ProviderIntentResolver(orig.mProviders, this.mUserManager);
        this.mReceivers = new ComponentResolver.ReceiverIntentResolver(orig.mReceivers, this.mUserManager, userNeedsBadgingCache);
        this.mServices = new ComponentResolver.ServiceIntentResolver(orig.mServices, this.mUserManager);
        this.mProvidersByAuthority = new ArrayMap<>(orig.mProvidersByAuthority);
    }
}
