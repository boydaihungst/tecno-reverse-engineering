package com.android.server.location;

import android.content.Context;
import android.location.Location;
import android.location.provider.ProviderProperties;
import android.os.SystemClock;
import android.os.UserHandle;
import com.android.modules.utils.BasicShellCommandHandler;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
/* loaded from: classes.dex */
class LocationShellCommand extends BasicShellCommandHandler {
    private static final float DEFAULT_TEST_LOCATION_ACCURACY = 100.0f;
    private final Context mContext;
    private final LocationManagerService mService;

    /* JADX INFO: Access modifiers changed from: package-private */
    public LocationShellCommand(Context context, LocationManagerService service) {
        this.mContext = context;
        this.mService = (LocationManagerService) Objects.requireNonNull(service);
    }

    public int onCommand(String cmd) {
        if (cmd == null) {
            return handleDefaultCommands(null);
        }
        char c = 65535;
        switch (cmd.hashCode()) {
            case -1064420500:
                if (cmd.equals("is-location-enabled")) {
                    c = 0;
                    break;
                }
                break;
            case -547571550:
                if (cmd.equals("providers")) {
                    c = 6;
                    break;
                }
                break;
            case -444268534:
                if (cmd.equals("is-automotive-gnss-suspended")) {
                    c = 5;
                    break;
                }
                break;
            case -361391806:
                if (cmd.equals("set-automotive-gnss-suspended")) {
                    c = 4;
                    break;
                }
                break;
            case -84945726:
                if (cmd.equals("set-adas-gnss-location-enabled")) {
                    c = 3;
                    break;
                }
                break;
            case 1546249012:
                if (cmd.equals("set-location-enabled")) {
                    c = 1;
                    break;
                }
                break;
            case 1640843002:
                if (cmd.equals("is-adas-gnss-location-enabled")) {
                    c = 2;
                    break;
                }
                break;
        }
        switch (c) {
            case 0:
                handleIsLocationEnabled();
                return 0;
            case 1:
                handleSetLocationEnabled();
                return 0;
            case 2:
                handleIsAdasGnssLocationEnabled();
                return 0;
            case 3:
                handleSetAdasGnssLocationEnabled();
                return 0;
            case 4:
                handleSetAutomotiveGnssSuspended();
                return 0;
            case 5:
                handleIsAutomotiveGnssSuspended();
                return 0;
            case 6:
                String command = getNextArgRequired();
                return parseProvidersCommand(command);
            default:
                return handleDefaultCommands(cmd);
        }
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    private int parseProvidersCommand(String cmd) {
        char c;
        switch (cmd.hashCode()) {
            case -1669563581:
                if (cmd.equals("remove-test-provider")) {
                    c = 1;
                    break;
                }
                c = 65535;
                break;
            case -1650104991:
                if (cmd.equals("set-test-provider-location")) {
                    c = 3;
                    break;
                }
                c = 65535;
                break;
            case -61579243:
                if (cmd.equals("set-test-provider-enabled")) {
                    c = 2;
                    break;
                }
                c = 65535;
                break;
            case 11404448:
                if (cmd.equals("add-test-provider")) {
                    c = 0;
                    break;
                }
                c = 65535;
                break;
            case 2036447497:
                if (cmd.equals("send-extra-command")) {
                    c = 4;
                    break;
                }
                c = 65535;
                break;
            default:
                c = 65535;
                break;
        }
        switch (c) {
            case 0:
                handleAddTestProvider();
                return 0;
            case 1:
                handleRemoveTestProvider();
                return 0;
            case 2:
                handleSetTestProviderEnabled();
                return 0;
            case 3:
                handleSetTestProviderLocation();
                return 0;
            case 4:
                handleSendExtraCommand();
                return 0;
            default:
                return handleDefaultCommands(cmd);
        }
    }

    private void handleIsLocationEnabled() {
        int userId = -3;
        while (true) {
            String option = getNextOption();
            if (option != null) {
                if ("--user".equals(option)) {
                    userId = UserHandle.parseUserArg(getNextArgRequired());
                } else {
                    throw new IllegalArgumentException("Unknown option: " + option);
                }
            } else {
                getOutPrintWriter().println(this.mService.isLocationEnabledForUser(userId));
                return;
            }
        }
    }

    private void handleSetLocationEnabled() {
        boolean enabled = Boolean.parseBoolean(getNextArgRequired());
        int userId = -3;
        while (true) {
            String option = getNextOption();
            if (option != null) {
                if ("--user".equals(option)) {
                    userId = UserHandle.parseUserArg(getNextArgRequired());
                } else {
                    throw new IllegalArgumentException("Unknown option: " + option);
                }
            } else {
                this.mService.setLocationEnabledForUser(enabled, userId);
                return;
            }
        }
    }

    private void handleIsAdasGnssLocationEnabled() {
        if (!this.mContext.getPackageManager().hasSystemFeature("android.hardware.type.automotive")) {
            throw new IllegalStateException("command only recognized on automotive devices");
        }
        int userId = -3;
        while (true) {
            String option = getNextOption();
            if (option != null) {
                if ("--user".equals(option)) {
                    userId = UserHandle.parseUserArg(getNextArgRequired());
                } else {
                    throw new IllegalArgumentException("Unknown option: " + option);
                }
            } else {
                getOutPrintWriter().println(this.mService.isAdasGnssLocationEnabledForUser(userId));
                return;
            }
        }
    }

    private void handleSetAdasGnssLocationEnabled() {
        if (!this.mContext.getPackageManager().hasSystemFeature("android.hardware.type.automotive")) {
            throw new IllegalStateException("command only recognized on automotive devices");
        }
        boolean enabled = Boolean.parseBoolean(getNextArgRequired());
        int userId = -3;
        while (true) {
            String option = getNextOption();
            if (option != null) {
                if ("--user".equals(option)) {
                    userId = UserHandle.parseUserArg(getNextArgRequired());
                } else {
                    throw new IllegalArgumentException("Unknown option: " + option);
                }
            } else {
                this.mService.setAdasGnssLocationEnabledForUser(enabled, userId);
                return;
            }
        }
    }

    private void handleSetAutomotiveGnssSuspended() {
        if (!this.mContext.getPackageManager().hasSystemFeature("android.hardware.type.automotive")) {
            throw new IllegalStateException("command only recognized on automotive devices");
        }
        boolean suspended = Boolean.parseBoolean(getNextArgRequired());
        this.mService.setAutomotiveGnssSuspended(suspended);
    }

    private void handleIsAutomotiveGnssSuspended() {
        if (!this.mContext.getPackageManager().hasSystemFeature("android.hardware.type.automotive")) {
            throw new IllegalStateException("command only recognized on automotive devices");
        }
        getOutPrintWriter().println(this.mService.isAutomotiveGnssSuspended());
    }

    private void handleAddTestProvider() {
        String provider = getNextArgRequired();
        boolean supportsAltitude = false;
        boolean supportsSpeed = false;
        boolean supportsBearing = false;
        int powerRequirement = 1;
        int accuracy = 1;
        List<String> extraAttributionTags = Collections.emptyList();
        boolean supportsBearing2 = false;
        boolean requiresSatellite = false;
        boolean requiresCell = false;
        boolean hasMonetaryCost = false;
        while (true) {
            String option = getNextOption();
            if (option != null) {
                int accuracy2 = accuracy;
                char c = 65535;
                switch (option.hashCode()) {
                    case -2115952999:
                        if (option.equals("--accuracy")) {
                            c = '\b';
                            break;
                        }
                        break;
                    case -1786843904:
                        if (option.equals("--requiresNetwork")) {
                            c = 0;
                            break;
                        }
                        break;
                    case -1474799448:
                        if (option.equals("--extraAttributionTags")) {
                            c = '\t';
                            break;
                        }
                        break;
                    case -1446936854:
                        if (option.equals("--supportsBearing")) {
                            c = 6;
                            break;
                        }
                        break;
                    case -1194644762:
                        if (option.equals("--supportsAltitude")) {
                            c = 4;
                            break;
                        }
                        break;
                    case 1086076880:
                        if (option.equals("--requiresCell")) {
                            c = 2;
                            break;
                        }
                        break;
                    case 1279633236:
                        if (option.equals("--hasMonetaryCost")) {
                            c = 3;
                            break;
                        }
                        break;
                    case 1483009933:
                        if (option.equals("--requiresSatellite")) {
                            c = 1;
                            break;
                        }
                        break;
                    case 1601002398:
                        if (option.equals("--powerRequirement")) {
                            c = 7;
                            break;
                        }
                        break;
                    case 2048042627:
                        if (option.equals("--supportsSpeed")) {
                            c = 5;
                            break;
                        }
                        break;
                }
                switch (c) {
                    case 0:
                        supportsBearing2 = true;
                        accuracy = accuracy2;
                        break;
                    case 1:
                        requiresSatellite = true;
                        accuracy = accuracy2;
                        break;
                    case 2:
                        requiresCell = true;
                        accuracy = accuracy2;
                        break;
                    case 3:
                        hasMonetaryCost = true;
                        accuracy = accuracy2;
                        break;
                    case 4:
                        supportsAltitude = true;
                        accuracy = accuracy2;
                        break;
                    case 5:
                        supportsSpeed = true;
                        accuracy = accuracy2;
                        break;
                    case 6:
                        supportsBearing = true;
                        accuracy = accuracy2;
                        break;
                    case 7:
                        int powerRequirement2 = Integer.parseInt(getNextArgRequired());
                        powerRequirement = powerRequirement2;
                        accuracy = accuracy2;
                        break;
                    case '\b':
                        int accuracy3 = Integer.parseInt(getNextArgRequired());
                        accuracy = accuracy3;
                        break;
                    case '\t':
                        List<String> extraAttributionTags2 = Arrays.asList(getNextArgRequired().split(","));
                        extraAttributionTags = extraAttributionTags2;
                        accuracy = accuracy2;
                        break;
                    default:
                        throw new IllegalArgumentException("Received unexpected option: " + option);
                }
            } else {
                ProviderProperties properties = new ProviderProperties.Builder().setHasNetworkRequirement(supportsBearing2).setHasSatelliteRequirement(requiresSatellite).setHasCellRequirement(requiresCell).setHasMonetaryCost(hasMonetaryCost).setHasAltitudeSupport(supportsAltitude).setHasSpeedSupport(supportsSpeed).setHasBearingSupport(supportsBearing).setPowerUsage(powerRequirement).setAccuracy(accuracy).build();
                this.mService.addTestProvider(provider, properties, extraAttributionTags, this.mContext.getOpPackageName(), this.mContext.getAttributionTag());
                return;
            }
        }
    }

    private void handleRemoveTestProvider() {
        String provider = getNextArgRequired();
        this.mService.removeTestProvider(provider, this.mContext.getOpPackageName(), this.mContext.getAttributionTag());
    }

    private void handleSetTestProviderEnabled() {
        String provider = getNextArgRequired();
        boolean enabled = Boolean.parseBoolean(getNextArgRequired());
        this.mService.setTestProviderEnabled(provider, enabled, this.mContext.getOpPackageName(), this.mContext.getAttributionTag());
    }

    private void handleSetTestProviderLocation() {
        String provider = getNextArgRequired();
        boolean hasLatLng = false;
        Location location = new Location(provider);
        location.setAccuracy(100.0f);
        location.setTime(System.currentTimeMillis());
        location.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
        while (true) {
            String option = getNextOption();
            if (option != null) {
                char c = 65535;
                switch (option.hashCode()) {
                    case -2115952999:
                        if (option.equals("--accuracy")) {
                            c = 1;
                            break;
                        }
                        break;
                    case 1333430381:
                        if (option.equals("--time")) {
                            c = 2;
                            break;
                        }
                        break;
                    case 1916798293:
                        if (option.equals("--location")) {
                            c = 0;
                            break;
                        }
                        break;
                }
                switch (c) {
                    case 0:
                        String[] locationInput = getNextArgRequired().split(",");
                        if (locationInput.length != 2) {
                            throw new IllegalArgumentException("Location argument must be in the form of \"<LATITUDE>,<LONGITUDE>\", not " + Arrays.toString(locationInput));
                        }
                        location.setLatitude(Double.parseDouble(locationInput[0]));
                        location.setLongitude(Double.parseDouble(locationInput[1]));
                        hasLatLng = true;
                        break;
                    case 1:
                        location.setAccuracy(Float.parseFloat(getNextArgRequired()));
                        break;
                    case 2:
                        location.setTime(Long.parseLong(getNextArgRequired()));
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown option: " + option);
                }
            } else if (!hasLatLng) {
                throw new IllegalArgumentException("Option \"--location\" is required");
            } else {
                this.mService.setTestProviderLocation(provider, location, this.mContext.getOpPackageName(), this.mContext.getAttributionTag());
                return;
            }
        }
    }

    private void handleSendExtraCommand() {
        String provider = getNextArgRequired();
        String command = getNextArgRequired();
        this.mService.sendExtraCommand(provider, command, null);
    }

    public void onHelp() {
        PrintWriter pw = getOutPrintWriter();
        pw.println("Location service commands:");
        pw.println("  help or -h");
        pw.println("    Print this help text.");
        pw.println("  is-location-enabled [--user <USER_ID>]");
        pw.println("    Gets the master location switch enabled state. If no user is specified,");
        pw.println("    the current user is assumed.");
        pw.println("  set-location-enabled true|false [--user <USER_ID>]");
        pw.println("    Sets the master location switch enabled state. If no user is specified,");
        pw.println("    the current user is assumed.");
        if (this.mContext.getPackageManager().hasSystemFeature("android.hardware.type.automotive")) {
            pw.println("  is-adas-gnss-location-enabled [--user <USER_ID>]");
            pw.println("    Gets the ADAS GNSS location enabled state. If no user is specified,");
            pw.println("    the current user is assumed.");
            pw.println("  set-adas-gnss-location-enabled true|false [--user <USER_ID>]");
            pw.println("    Sets the ADAS GNSS location enabled state. If no user is specified,");
            pw.println("    the current user is assumed.");
            pw.println("  is-automotive-gnss-suspended");
            pw.println("    Gets the automotive GNSS suspended state.");
            pw.println("  set-automotive-gnss-suspended true|false");
            pw.println("    Sets the automotive GNSS suspended state.");
        }
        pw.println("  providers");
        pw.println("    The providers command is followed by a subcommand, as listed below:");
        pw.println();
        pw.println("    add-test-provider <PROVIDER> [--requiresNetwork] [--requiresSatellite]");
        pw.println("      [--requiresCell] [--hasMonetaryCost] [--supportsAltitude]");
        pw.println("      [--supportsSpeed] [--supportsBearing]");
        pw.println("      [--powerRequirement <POWER_REQUIREMENT>]");
        pw.println("      [--extraAttributionTags <TAG>,<TAG>,...]");
        pw.println("      Add the given test provider. Requires MOCK_LOCATION permissions which");
        pw.println("      can be enabled by running \"adb shell appops set <uid>");
        pw.println("      android:mock_location allow\". There are optional flags that can be");
        pw.println("      used to configure the provider properties and additional arguments. If");
        pw.println("      no flags are included, then default values will be used.");
        pw.println("    remove-test-provider <PROVIDER>");
        pw.println("      Remove the given test provider.");
        pw.println("    set-test-provider-enabled <PROVIDER> true|false");
        pw.println("      Sets the given test provider enabled state.");
        pw.println("    set-test-provider-location <PROVIDER> --location <LATITUDE>,<LONGITUDE>");
        pw.println("      [--accuracy <ACCURACY>] [--time <TIME>]");
        pw.println("      Set location for given test provider. Accuracy and time are optional.");
        pw.println("    send-extra-command <PROVIDER> <COMMAND>");
        pw.println("      Sends the given extra command to the given provider.");
        pw.println();
        pw.println("      Common commands that may be supported by the gps provider, depending on");
        pw.println("      hardware and software configurations:");
        pw.println("        delete_aiding_data - requests deletion of any predictive aiding data");
        pw.println("        force_time_injection - requests NTP time injection");
        pw.println("        force_psds_injection - requests predictive aiding data injection");
        pw.println("        request_power_stats - requests GNSS power stats update");
    }
}
