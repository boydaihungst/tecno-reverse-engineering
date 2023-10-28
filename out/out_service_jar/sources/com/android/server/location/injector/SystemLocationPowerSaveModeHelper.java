package com.android.server.location.injector;

import android.content.Context;
import android.os.PowerManager;
import android.os.PowerManagerInternal;
import android.os.PowerSaveState;
import com.android.internal.util.Preconditions;
import com.android.server.FgThread;
import com.android.server.LocalServices;
import java.util.Objects;
import java.util.function.Consumer;
/* loaded from: classes.dex */
public class SystemLocationPowerSaveModeHelper extends LocationPowerSaveModeHelper implements Consumer<PowerSaveState> {
    private final Context mContext;
    private volatile int mLocationPowerSaveMode;
    private boolean mReady;

    public SystemLocationPowerSaveModeHelper(Context context) {
        this.mContext = context;
    }

    public void onSystemReady() {
        if (this.mReady) {
            return;
        }
        ((PowerManagerInternal) LocalServices.getService(PowerManagerInternal.class)).registerLowPowerModeObserver(1, this);
        this.mLocationPowerSaveMode = ((PowerManager) Objects.requireNonNull((PowerManager) this.mContext.getSystemService(PowerManager.class))).getLocationPowerSaveMode();
        this.mReady = true;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // java.util.function.Consumer
    public void accept(PowerSaveState powerSaveState) {
        final int locationPowerSaveMode;
        if (!powerSaveState.batterySaverEnabled) {
            locationPowerSaveMode = 0;
        } else {
            locationPowerSaveMode = powerSaveState.locationMode;
        }
        if (locationPowerSaveMode == this.mLocationPowerSaveMode) {
            return;
        }
        this.mLocationPowerSaveMode = locationPowerSaveMode;
        FgThread.getHandler().post(new Runnable() { // from class: com.android.server.location.injector.SystemLocationPowerSaveModeHelper$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                SystemLocationPowerSaveModeHelper.this.m4510xaac33c81(locationPowerSaveMode);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$accept$0$com-android-server-location-injector-SystemLocationPowerSaveModeHelper  reason: not valid java name */
    public /* synthetic */ void m4510xaac33c81(int locationPowerSaveMode) {
        notifyLocationPowerSaveModeChanged(locationPowerSaveMode);
    }

    @Override // com.android.server.location.injector.LocationPowerSaveModeHelper
    public int getLocationPowerSaveMode() {
        Preconditions.checkState(this.mReady);
        return this.mLocationPowerSaveMode;
    }
}
