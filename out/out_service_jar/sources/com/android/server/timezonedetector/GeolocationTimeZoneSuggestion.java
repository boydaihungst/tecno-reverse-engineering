package com.android.server.timezonedetector;

import android.os.ShellCommand;
import android.os.SystemClock;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.StringTokenizer;
/* loaded from: classes2.dex */
public final class GeolocationTimeZoneSuggestion {
    private ArrayList<String> mDebugInfo;
    private final long mEffectiveFromElapsedMillis;
    private final List<String> mZoneIds;

    private GeolocationTimeZoneSuggestion(long effectiveFromElapsedMillis, List<String> zoneIds) {
        this.mEffectiveFromElapsedMillis = effectiveFromElapsedMillis;
        if (zoneIds == null) {
            this.mZoneIds = null;
        } else {
            this.mZoneIds = Collections.unmodifiableList(new ArrayList(zoneIds));
        }
    }

    public static GeolocationTimeZoneSuggestion createUncertainSuggestion(long effectiveFromElapsedMillis) {
        return new GeolocationTimeZoneSuggestion(effectiveFromElapsedMillis, null);
    }

    public static GeolocationTimeZoneSuggestion createCertainSuggestion(long effectiveFromElapsedMillis, List<String> zoneIds) {
        return new GeolocationTimeZoneSuggestion(effectiveFromElapsedMillis, zoneIds);
    }

    public long getEffectiveFromElapsedMillis() {
        return this.mEffectiveFromElapsedMillis;
    }

    public List<String> getZoneIds() {
        return this.mZoneIds;
    }

    public List<String> getDebugInfo() {
        ArrayList<String> arrayList = this.mDebugInfo;
        return arrayList == null ? Collections.emptyList() : Collections.unmodifiableList(arrayList);
    }

    public void addDebugInfo(String... debugInfos) {
        if (this.mDebugInfo == null) {
            this.mDebugInfo = new ArrayList<>();
        }
        this.mDebugInfo.addAll(Arrays.asList(debugInfos));
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        GeolocationTimeZoneSuggestion that = (GeolocationTimeZoneSuggestion) o;
        if (this.mEffectiveFromElapsedMillis == that.mEffectiveFromElapsedMillis && Objects.equals(this.mZoneIds, that.mZoneIds)) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        return Objects.hash(Long.valueOf(this.mEffectiveFromElapsedMillis), this.mZoneIds);
    }

    public String toString() {
        return "GeolocationTimeZoneSuggestion{mEffectiveFromElapsedMillis=" + this.mEffectiveFromElapsedMillis + ", mZoneIds=" + this.mZoneIds + ", mDebugInfo=" + this.mDebugInfo + '}';
    }

    public static GeolocationTimeZoneSuggestion parseCommandLineArg(ShellCommand cmd) {
        String zoneIdsString = null;
        while (true) {
            String opt = cmd.getNextArg();
            if (opt != null) {
                char c = 65535;
                switch (opt.hashCode()) {
                    case 864328005:
                        if (opt.equals("--zone_ids")) {
                            c = 0;
                            break;
                        }
                        break;
                }
                switch (c) {
                    case 0:
                        zoneIdsString = cmd.getNextArgRequired();
                    default:
                        throw new IllegalArgumentException("Unknown option: " + opt);
                }
            } else {
                long elapsedRealtimeMillis = SystemClock.elapsedRealtime();
                List<String> zoneIds = parseZoneIdsArg(zoneIdsString);
                GeolocationTimeZoneSuggestion suggestion = new GeolocationTimeZoneSuggestion(elapsedRealtimeMillis, zoneIds);
                suggestion.addDebugInfo("Command line injection");
                return suggestion;
            }
        }
    }

    private static List<String> parseZoneIdsArg(String zoneIdsString) {
        if ("UNCERTAIN".equals(zoneIdsString)) {
            return null;
        }
        if ("EMPTY".equals(zoneIdsString)) {
            return Collections.emptyList();
        }
        ArrayList<String> zoneIds = new ArrayList<>();
        StringTokenizer tokenizer = new StringTokenizer(zoneIdsString, ",");
        while (tokenizer.hasMoreTokens()) {
            zoneIds.add(tokenizer.nextToken());
        }
        return zoneIds;
    }

    public static void printCommandLineOpts(PrintWriter pw) {
        pw.println("Geolocation suggestion options:");
        pw.println("  --zone_ids {UNCERTAIN|EMPTY|<Olson ID>+}");
        pw.println();
        pw.println("See " + GeolocationTimeZoneSuggestion.class.getName() + " for more information");
    }
}
