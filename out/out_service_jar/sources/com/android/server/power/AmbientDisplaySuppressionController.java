package com.android.server.power;

import android.content.Context;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.ArraySet;
import android.util.Pair;
import android.util.Slog;
import com.android.internal.statusbar.IStatusBarService;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
/* loaded from: classes2.dex */
public class AmbientDisplaySuppressionController {
    private static final String TAG = "AmbientDisplaySuppressionController";
    private final Context mContext;
    private IStatusBarService mStatusBarService;
    private final Set<Pair<String, Integer>> mSuppressionTokens = Collections.synchronizedSet(new ArraySet());

    /* JADX INFO: Access modifiers changed from: package-private */
    public AmbientDisplaySuppressionController(Context context) {
        this.mContext = (Context) Objects.requireNonNull(context);
    }

    public void suppress(String token, int callingUid, boolean suppress) {
        Pair<String, Integer> suppressionToken = Pair.create((String) Objects.requireNonNull(token), Integer.valueOf(callingUid));
        if (suppress) {
            this.mSuppressionTokens.add(suppressionToken);
        } else {
            this.mSuppressionTokens.remove(suppressionToken);
        }
        try {
            synchronized (this.mSuppressionTokens) {
                getStatusBar().suppressAmbientDisplay(isSuppressed());
            }
        } catch (RemoteException e) {
            Slog.e(TAG, "Failed to suppress ambient display", e);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public List<String> getSuppressionTokens(int callingUid) {
        List<String> result = new ArrayList<>();
        synchronized (this.mSuppressionTokens) {
            for (Pair<String, Integer> token : this.mSuppressionTokens) {
                if (((Integer) token.second).intValue() == callingUid) {
                    result.add((String) token.first);
                }
            }
        }
        return result;
    }

    public boolean isSuppressed(String token, int callingUid) {
        return this.mSuppressionTokens.contains(Pair.create((String) Objects.requireNonNull(token), Integer.valueOf(callingUid)));
    }

    public boolean isSuppressed() {
        return !this.mSuppressionTokens.isEmpty();
    }

    public void dump(PrintWriter pw) {
        pw.println("AmbientDisplaySuppressionController:");
        pw.println(" ambientDisplaySuppressed=" + isSuppressed());
        pw.println(" mSuppressionTokens=" + this.mSuppressionTokens);
    }

    private synchronized IStatusBarService getStatusBar() {
        if (this.mStatusBarService == null) {
            this.mStatusBarService = IStatusBarService.Stub.asInterface(ServiceManager.getService("statusbar"));
        }
        return this.mStatusBarService;
    }
}
