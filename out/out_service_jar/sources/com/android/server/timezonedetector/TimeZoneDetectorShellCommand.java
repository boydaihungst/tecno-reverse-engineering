package com.android.server.timezonedetector;

import android.app.time.TimeZoneConfiguration;
import android.app.timezonedetector.ManualTimeZoneSuggestion;
import android.app.timezonedetector.TelephonyTimeZoneSuggestion;
import android.os.ShellCommand;
import com.android.server.timedetector.ServerFlags;
import java.io.PrintWriter;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class TimeZoneDetectorShellCommand extends ShellCommand {
    private final TimeZoneDetectorService mInterface;

    /* JADX INFO: Access modifiers changed from: package-private */
    public TimeZoneDetectorShellCommand(TimeZoneDetectorService timeZoneDetectorService) {
        this.mInterface = timeZoneDetectorService;
    }

    public int onCommand(String cmd) {
        if (cmd == null) {
            return handleDefaultCommands(cmd);
        }
        char c = 65535;
        switch (cmd.hashCode()) {
            case -1908861832:
                if (cmd.equals("is_telephony_detection_supported")) {
                    c = 2;
                    break;
                }
                break;
            case -1595273216:
                if (cmd.equals("suggest_manual_time_zone")) {
                    c = 7;
                    break;
                }
                break;
            case -1316904020:
                if (cmd.equals("is_auto_detection_enabled")) {
                    c = 0;
                    break;
                }
                break;
            case -1264030344:
                if (cmd.equals("dump_metrics")) {
                    c = '\n';
                    break;
                }
                break;
            case -646187524:
                if (cmd.equals("set_geo_detection_enabled")) {
                    c = 5;
                    break;
                }
                break;
            case 496894148:
                if (cmd.equals("is_geo_detection_enabled")) {
                    c = 4;
                    break;
                }
                break;
            case 596690236:
                if (cmd.equals("suggest_telephony_time_zone")) {
                    c = '\b';
                    break;
                }
                break;
            case 648078493:
                if (cmd.equals("suggest_geo_location_time_zone")) {
                    c = 6;
                    break;
                }
                break;
            case 1133835109:
                if (cmd.equals("enable_telephony_fallback")) {
                    c = '\t';
                    break;
                }
                break;
            case 1385756017:
                if (cmd.equals("is_geo_detection_supported")) {
                    c = 3;
                    break;
                }
                break;
            case 1902269812:
                if (cmd.equals("set_auto_detection_enabled")) {
                    c = 1;
                    break;
                }
                break;
        }
        switch (c) {
            case 0:
                return runIsAutoDetectionEnabled();
            case 1:
                return runSetAutoDetectionEnabled();
            case 2:
                return runIsTelephonyDetectionSupported();
            case 3:
                return runIsGeoDetectionSupported();
            case 4:
                return runIsGeoDetectionEnabled();
            case 5:
                return runSetGeoDetectionEnabled();
            case 6:
                return runSuggestGeolocationTimeZone();
            case 7:
                return runSuggestManualTimeZone();
            case '\b':
                return runSuggestTelephonyTimeZone();
            case '\t':
                return runEnableTelephonyFallback();
            case '\n':
                return runDumpMetrics();
            default:
                return handleDefaultCommands(cmd);
        }
    }

    private int runIsAutoDetectionEnabled() {
        PrintWriter pw = getOutPrintWriter();
        boolean enabled = this.mInterface.getCapabilitiesAndConfig(-2).getConfiguration().isAutoDetectionEnabled();
        pw.println(enabled);
        return 0;
    }

    private int runIsTelephonyDetectionSupported() {
        PrintWriter pw = getOutPrintWriter();
        boolean enabled = this.mInterface.isTelephonyTimeZoneDetectionSupported();
        pw.println(enabled);
        return 0;
    }

    private int runIsGeoDetectionSupported() {
        PrintWriter pw = getOutPrintWriter();
        boolean enabled = this.mInterface.isGeoTimeZoneDetectionSupported();
        pw.println(enabled);
        return 0;
    }

    private int runIsGeoDetectionEnabled() {
        PrintWriter pw = getOutPrintWriter();
        boolean enabled = this.mInterface.getCapabilitiesAndConfig(-2).getConfiguration().isGeoDetectionEnabled();
        pw.println(enabled);
        return 0;
    }

    private int runSetAutoDetectionEnabled() {
        boolean enabled = Boolean.parseBoolean(getNextArgRequired());
        TimeZoneConfiguration configuration = new TimeZoneConfiguration.Builder().setAutoDetectionEnabled(enabled).build();
        return !this.mInterface.updateConfiguration(-2, configuration);
    }

    private int runSetGeoDetectionEnabled() {
        boolean enabled = Boolean.parseBoolean(getNextArgRequired());
        TimeZoneConfiguration configuration = new TimeZoneConfiguration.Builder().setGeoDetectionEnabled(enabled).build();
        return !this.mInterface.updateConfiguration(-2, configuration);
    }

    private int runSuggestGeolocationTimeZone() {
        Supplier supplier = new Supplier() { // from class: com.android.server.timezonedetector.TimeZoneDetectorShellCommand$$ExternalSyntheticLambda0
            @Override // java.util.function.Supplier
            public final Object get() {
                return TimeZoneDetectorShellCommand.this.m6907xe8f88bf7();
            }
        };
        final TimeZoneDetectorService timeZoneDetectorService = this.mInterface;
        Objects.requireNonNull(timeZoneDetectorService);
        return runSuggestTimeZone(supplier, new Consumer() { // from class: com.android.server.timezonedetector.TimeZoneDetectorShellCommand$$ExternalSyntheticLambda1
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                TimeZoneDetectorService.this.suggestGeolocationTimeZone((GeolocationTimeZoneSuggestion) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$runSuggestGeolocationTimeZone$0$com-android-server-timezonedetector-TimeZoneDetectorShellCommand  reason: not valid java name */
    public /* synthetic */ GeolocationTimeZoneSuggestion m6907xe8f88bf7() {
        return GeolocationTimeZoneSuggestion.parseCommandLineArg(this);
    }

    private int runSuggestManualTimeZone() {
        Supplier supplier = new Supplier() { // from class: com.android.server.timezonedetector.TimeZoneDetectorShellCommand$$ExternalSyntheticLambda2
            @Override // java.util.function.Supplier
            public final Object get() {
                return TimeZoneDetectorShellCommand.this.m6908x8cefbe6a();
            }
        };
        final TimeZoneDetectorService timeZoneDetectorService = this.mInterface;
        Objects.requireNonNull(timeZoneDetectorService);
        return runSuggestTimeZone(supplier, new Consumer() { // from class: com.android.server.timezonedetector.TimeZoneDetectorShellCommand$$ExternalSyntheticLambda3
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                TimeZoneDetectorService.this.suggestManualTimeZone((ManualTimeZoneSuggestion) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$runSuggestManualTimeZone$1$com-android-server-timezonedetector-TimeZoneDetectorShellCommand  reason: not valid java name */
    public /* synthetic */ ManualTimeZoneSuggestion m6908x8cefbe6a() {
        return ManualTimeZoneSuggestion.parseCommandLineArg(this);
    }

    private int runSuggestTelephonyTimeZone() {
        Supplier supplier = new Supplier() { // from class: com.android.server.timezonedetector.TimeZoneDetectorShellCommand$$ExternalSyntheticLambda4
            @Override // java.util.function.Supplier
            public final Object get() {
                return TimeZoneDetectorShellCommand.this.m6909x67aa7463();
            }
        };
        final TimeZoneDetectorService timeZoneDetectorService = this.mInterface;
        Objects.requireNonNull(timeZoneDetectorService);
        return runSuggestTimeZone(supplier, new Consumer() { // from class: com.android.server.timezonedetector.TimeZoneDetectorShellCommand$$ExternalSyntheticLambda5
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                TimeZoneDetectorService.this.suggestTelephonyTimeZone((TelephonyTimeZoneSuggestion) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$runSuggestTelephonyTimeZone$2$com-android-server-timezonedetector-TimeZoneDetectorShellCommand  reason: not valid java name */
    public /* synthetic */ TelephonyTimeZoneSuggestion m6909x67aa7463() {
        return TelephonyTimeZoneSuggestion.parseCommandLineArg(this);
    }

    private <T> int runSuggestTimeZone(Supplier<T> suggestionParser, Consumer<T> invoker) {
        PrintWriter pw = getOutPrintWriter();
        try {
            T suggestion = suggestionParser.get();
            if (suggestion == null) {
                pw.println("Error: suggestion not specified");
                return 1;
            }
            invoker.accept(suggestion);
            pw.println("Suggestion " + suggestion + " injected.");
            return 0;
        } catch (RuntimeException e) {
            pw.println(e);
            return 1;
        }
    }

    private int runEnableTelephonyFallback() {
        this.mInterface.enableTelephonyFallback();
        return 0;
    }

    private int runDumpMetrics() {
        PrintWriter pw = getOutPrintWriter();
        MetricsTimeZoneDetectorState metricsState = this.mInterface.generateMetricsState();
        pw.println("MetricsTimeZoneDetectorState:");
        pw.println(metricsState.toString());
        return 0;
    }

    public void onHelp() {
        PrintWriter pw = getOutPrintWriter();
        pw.printf("Time Zone Detector (%s) commands:\n", "time_zone_detector");
        pw.printf("  help\n", new Object[0]);
        pw.printf("    Print this help text.\n", new Object[0]);
        pw.printf("  %s\n", "is_auto_detection_enabled");
        pw.printf("    Prints true/false according to the automatic time zone detection setting\n", new Object[0]);
        pw.printf("  %s true|false\n", "set_auto_detection_enabled");
        pw.printf("    Sets the automatic time zone detection setting.\n", new Object[0]);
        pw.printf("  %s\n", "is_telephony_detection_supported");
        pw.printf("    Prints true/false according to whether telephony time zone detection is supported on this device.\n", new Object[0]);
        pw.printf("  %s\n", "is_geo_detection_supported");
        pw.printf("    Prints true/false according to whether geolocation time zone detection is supported on this device.\n", new Object[0]);
        pw.printf("  %s\n", "is_geo_detection_enabled");
        pw.printf("    Prints true/false according to the geolocation time zone detection setting.\n", new Object[0]);
        pw.printf("  %s true|false\n", "set_geo_detection_enabled");
        pw.printf("    Sets the geolocation time zone detection enabled setting.\n", new Object[0]);
        pw.printf("  %s\n", "enable_telephony_fallback");
        pw.printf("    Signals that telephony time zone detection fall back can be used if geolocation detection is supported and enabled. This is a temporary state until geolocation detection becomes \"certain\". To have an effect this requires that the telephony fallback feature is supported on the device, see below for for device_config flags.\n", new Object[0]);
        pw.println();
        pw.printf("  %s <geolocation suggestion opts>\n", "suggest_geo_location_time_zone");
        pw.printf("  %s <manual suggestion opts>\n", "suggest_manual_time_zone");
        pw.printf("  %s <telephony suggestion opts>\n", "suggest_telephony_time_zone");
        pw.printf("  %s\n", "dump_metrics");
        pw.printf("    Dumps the service metrics to stdout for inspection.\n", new Object[0]);
        pw.println();
        GeolocationTimeZoneSuggestion.printCommandLineOpts(pw);
        pw.println();
        ManualTimeZoneSuggestion.printCommandLineOpts(pw);
        pw.println();
        TelephonyTimeZoneSuggestion.printCommandLineOpts(pw);
        pw.println();
        pw.printf("This service is also affected by the following device_config flags in the %s namespace:\n", "system_time");
        pw.printf("  %s\n", ServerFlags.KEY_LOCATION_TIME_ZONE_DETECTION_FEATURE_SUPPORTED);
        pw.printf("    Only observed if the geolocation time zone detection feature is enabled in config.\n", new Object[0]);
        pw.printf("    Set this to false to disable the feature.\n", new Object[0]);
        pw.printf("  %s\n", ServerFlags.KEY_LOCATION_TIME_ZONE_DETECTION_RUN_IN_BACKGROUND_ENABLED);
        pw.printf("    Runs geolocation time zone detection even when it not enabled by the user. The result is not used to set the device's time zone [*]\n", new Object[0]);
        pw.printf("  %s\n", ServerFlags.KEY_LOCATION_TIME_ZONE_DETECTION_SETTING_ENABLED_DEFAULT);
        pw.printf("    Only used if the device does not have an explicit 'geolocation time zone detection enabled' setting stored [*].\n", new Object[0]);
        pw.printf("    The default is when unset is false.\n", new Object[0]);
        pw.printf("  %s\n", ServerFlags.KEY_LOCATION_TIME_ZONE_DETECTION_SETTING_ENABLED_OVERRIDE);
        pw.printf("    Used to override the device's 'geolocation time zone detection enabled' setting [*].\n", new Object[0]);
        pw.printf("  %s\n", ServerFlags.KEY_TIME_ZONE_DETECTOR_TELEPHONY_FALLBACK_SUPPORTED);
        pw.printf("    Used to enable / disable support for telephony detection fallback. Also see the %s command.\n", "enable_telephony_fallback");
        pw.printf("  %s\n", ServerFlags.KEY_ENHANCED_METRICS_COLLECTION_ENABLED);
        pw.printf("    Used to increase the detail of metrics collected / reported.\n", new Object[0]);
        pw.println();
        pw.printf("[*] To be enabled, the user must still have location = on / auto time zone detection = on.\n", new Object[0]);
        pw.println();
        pw.printf("See \"adb shell cmd device_config\" for more information on setting flags.\n", new Object[0]);
        pw.println();
        pw.printf("Also see \"adb shell cmd %s help\" for lower-level location time zone commands / settings.\n", "location_time_zone_manager");
        pw.println();
    }
}
