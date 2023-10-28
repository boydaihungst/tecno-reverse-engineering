package android.transparency;

import android.content.Context;
import android.os.RemoteException;
import android.util.Slog;
import com.android.internal.os.IBinaryTransparencyService;
import java.util.Map;
/* loaded from: classes3.dex */
public class BinaryTransparencyManager {
    private static final String TAG = "TransparencyManager";
    private final Context mContext;
    private final IBinaryTransparencyService mService;

    public BinaryTransparencyManager(Context context, IBinaryTransparencyService service) {
        this.mContext = context;
        this.mService = service;
    }

    public String getSignedImageInfo() {
        try {
            return this.mService.getSignedImageInfo();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public Map getApexInfo() {
        try {
            Slog.d(TAG, "Calling backend's getApexInfo()");
            return this.mService.getApexInfo();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }
}
