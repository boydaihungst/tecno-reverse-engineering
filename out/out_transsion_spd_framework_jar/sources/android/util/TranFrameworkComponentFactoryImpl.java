package android.util;

import android.content.Context;
import android.net.NetconManager;
import android.net.NetconManagerImpl;
import java.net.MpHttp;
/* loaded from: classes.dex */
public class TranFrameworkComponentFactoryImpl extends TranFrameworkComponentFactory {
    public NetconManager makeNetconManager(Context context, MpHttp.MpHttpCallback mpHttpCallback) {
        return new NetconManagerImpl(context, mpHttpCallback);
    }

    public NetconManager makeNetconManager(Context context) {
        return new NetconManagerImpl(context);
    }
}
