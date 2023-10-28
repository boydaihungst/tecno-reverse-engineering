package com.android.server.devicestate;

import android.content.Context;
import android.content.res.Resources;
import android.text.TextUtils;
import com.android.server.policy.DeviceStatePolicyImpl;
/* loaded from: classes.dex */
public abstract class DeviceStatePolicy {
    protected final Context mContext;

    public abstract void configureDeviceForState(int i, Runnable runnable);

    public abstract DeviceStateProvider getDeviceStateProvider();

    /* JADX INFO: Access modifiers changed from: protected */
    public DeviceStatePolicy(Context context) {
        this.mContext = context;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static final class DefaultProvider implements Provider {
        DefaultProvider() {
        }

        @Override // com.android.server.devicestate.DeviceStatePolicy.Provider
        public DeviceStatePolicy instantiate(Context context) {
            return new DeviceStatePolicyImpl(context);
        }
    }

    /* loaded from: classes.dex */
    public interface Provider {
        DeviceStatePolicy instantiate(Context context);

        static Provider fromResources(Resources res) {
            String name = res.getString(17039957);
            if (TextUtils.isEmpty(name)) {
                return new DefaultProvider();
            }
            try {
                return (Provider) Class.forName(name).newInstance();
            } catch (ClassCastException | ReflectiveOperationException e) {
                throw new IllegalStateException("Couldn't instantiate class " + name + " for config_deviceSpecificDeviceStatePolicyProvider: make sure it has a public zero-argument constructor and implements DeviceStatePolicy.Provider", e);
            }
        }
    }
}
