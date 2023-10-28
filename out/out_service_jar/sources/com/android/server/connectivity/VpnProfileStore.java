package com.android.server.connectivity;

import android.security.LegacyVpnProfileStore;
/* loaded from: classes.dex */
public class VpnProfileStore {
    public boolean put(String alias, byte[] profile) {
        return LegacyVpnProfileStore.put(alias, profile);
    }

    public byte[] get(String alias) {
        return LegacyVpnProfileStore.get(alias);
    }

    public boolean remove(String alias) {
        return LegacyVpnProfileStore.remove(alias);
    }

    public String[] list(String prefix) {
        return LegacyVpnProfileStore.list(prefix);
    }
}
