package com.android.server.timedetector;

import android.app.time.ExternalTimeSuggestion;
import android.app.timedetector.GnssTimeSuggestion;
import android.app.timedetector.ManualTimeSuggestion;
import android.app.timedetector.NetworkTimeSuggestion;
import android.app.timedetector.TelephonyTimeSuggestion;
import android.os.TimestampedValue;
import com.android.internal.util.Preconditions;
import com.android.server.timezonedetector.Dumpable;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
/* loaded from: classes2.dex */
public interface TimeDetectorStrategy extends Dumpable {
    public static final int ORIGIN_EXTERNAL = 5;
    public static final int ORIGIN_GNSS = 4;
    public static final int ORIGIN_MANUAL = 2;
    public static final int ORIGIN_NETWORK = 3;
    public static final int ORIGIN_TELEPHONY = 1;

    @Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes2.dex */
    public @interface Origin {
    }

    ConfigurationInternal getConfigurationInternal(int i);

    void suggestExternalTime(ExternalTimeSuggestion externalTimeSuggestion);

    void suggestGnssTime(GnssTimeSuggestion gnssTimeSuggestion);

    boolean suggestManualTime(ManualTimeSuggestion manualTimeSuggestion);

    void suggestNetworkTime(NetworkTimeSuggestion networkTimeSuggestion);

    void suggestTelephonyTime(TelephonyTimeSuggestion telephonyTimeSuggestion);

    static long getTimeAt(TimestampedValue<Long> timeValue, long referenceClockMillisNow) {
        return (referenceClockMillisNow - timeValue.getReferenceTimeMillis()) + ((Long) timeValue.getValue()).longValue();
    }

    static String originToString(int origin) {
        switch (origin) {
            case 1:
                return "telephony";
            case 2:
                return "manual";
            case 3:
                return "network";
            case 4:
                return "gnss";
            case 5:
                return "external";
            default:
                throw new IllegalArgumentException("origin=" + origin);
        }
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Code restructure failed: missing block: B:19:0x003d, code lost:
        if (r7.equals("manual") != false) goto L9;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    static int stringToOrigin(String originString) {
        boolean z = false;
        Preconditions.checkArgument(originString != null);
        switch (originString.hashCode()) {
            case -1820761141:
                if (originString.equals("external")) {
                    z = true;
                    break;
                }
                z = true;
                break;
            case -1081415738:
                break;
            case 3177863:
                if (originString.equals("gnss")) {
                    z = true;
                    break;
                }
                z = true;
                break;
            case 783201304:
                if (originString.equals("telephony")) {
                    z = true;
                    break;
                }
                z = true;
                break;
            case 1843485230:
                if (originString.equals("network")) {
                    z = true;
                    break;
                }
                z = true;
                break;
            default:
                z = true;
                break;
        }
        switch (z) {
            case false:
                return 2;
            case true:
                return 3;
            case true:
                return 1;
            case true:
                return 4;
            case true:
                return 5;
            default:
                throw new IllegalArgumentException("originString=" + originString);
        }
    }
}
