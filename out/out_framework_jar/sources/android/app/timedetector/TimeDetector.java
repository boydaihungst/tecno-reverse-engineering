package android.app.timedetector;

import android.os.SystemClock;
import android.os.TimestampedValue;
/* loaded from: classes.dex */
public interface TimeDetector {
    public static final String SHELL_COMMAND_IS_AUTO_DETECTION_ENABLED = "is_auto_detection_enabled";
    public static final String SHELL_COMMAND_SERVICE_NAME = "time_detector";

    void suggestGnssTime(GnssTimeSuggestion gnssTimeSuggestion);

    boolean suggestManualTime(ManualTimeSuggestion manualTimeSuggestion);

    void suggestNetworkTime(NetworkTimeSuggestion networkTimeSuggestion);

    void suggestTelephonyTime(TelephonyTimeSuggestion telephonyTimeSuggestion);

    static ManualTimeSuggestion createManualTimeSuggestion(long when, String why) {
        TimestampedValue<Long> utcTime = new TimestampedValue<>(SystemClock.elapsedRealtime(), Long.valueOf(when));
        ManualTimeSuggestion manualTimeSuggestion = new ManualTimeSuggestion(utcTime);
        manualTimeSuggestion.addDebugInfo(why);
        return manualTimeSuggestion;
    }
}
