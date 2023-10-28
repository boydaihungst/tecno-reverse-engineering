package com.android.server.media;

import android.media.AudioSystem;
import android.media.IAudioService;
import android.os.ServiceManager;
import android.util.AndroidException;
/* loaded from: classes2.dex */
public class VolumeCtrl {
    private static final String ADJUST_LOWER = "lower";
    private static final String ADJUST_RAISE = "raise";
    private static final String ADJUST_SAME = "same";
    private static final String LOG_E = "[E]";
    private static final String LOG_V = "[V]";
    private static final String TAG = "VolumeCtrl";
    public static final String USAGE = new String("the options are as follows: \n\t\t--stream STREAM selects the stream to control, see AudioManager.STREAM_*\n\t\t                controls AudioManager.STREAM_MUSIC if no stream is specified\n\t\t--set INDEX     sets the volume index value\n\t\t--adj DIRECTION adjusts the volume, use raise|same|lower for the direction\n\t\t--get           outputs the current volume\n\t\t--show          shows the UI during the volume change\n\texamples:\n\t\tadb shell media volume --show --stream 3 --set 11\n\t\tadb shell media volume --stream 0 --adj lower\n\t\tadb shell media volume --stream 3 --get\n");
    private static final int VOLUME_CONTROL_MODE_ADJUST = 2;
    private static final int VOLUME_CONTROL_MODE_SET = 1;

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    public static void run(MediaShellCommand cmd) throws Exception {
        char c;
        char c2;
        int stream = 3;
        int volIndex = 5;
        int mode = 0;
        int adjDir = 1;
        int i = 0;
        boolean doGet = false;
        String adjustment = null;
        while (true) {
            String option = cmd.getNextOption();
            if (option != null) {
                switch (option.hashCode()) {
                    case 42995463:
                        if (option.equals("--adj")) {
                            c2 = 4;
                            break;
                        }
                        c2 = 65535;
                        break;
                    case 43001270:
                        if (option.equals("--get")) {
                            c2 = 1;
                            break;
                        }
                        c2 = 65535;
                        break;
                    case 43012802:
                        if (option.equals("--set")) {
                            c2 = 3;
                            break;
                        }
                        c2 = 65535;
                        break;
                    case 1333399709:
                        if (option.equals("--show")) {
                            c2 = 0;
                            break;
                        }
                        c2 = 65535;
                        break;
                    case 1508023584:
                        if (option.equals("--stream")) {
                            c2 = 2;
                            break;
                        }
                        c2 = 65535;
                        break;
                    default:
                        c2 = 65535;
                        break;
                }
                switch (c2) {
                    case 0:
                        i = 1;
                        break;
                    case 1:
                        doGet = true;
                        cmd.log(LOG_V, "will get volume");
                        break;
                    case 2:
                        stream = Integer.decode(cmd.getNextArgRequired()).intValue();
                        cmd.log(LOG_V, "will control stream=" + stream + " (" + streamName(stream) + ")");
                        break;
                    case 3:
                        volIndex = Integer.decode(cmd.getNextArgRequired()).intValue();
                        mode = 1;
                        cmd.log(LOG_V, "will set volume to index=" + volIndex);
                        break;
                    case 4:
                        mode = 2;
                        adjustment = cmd.getNextArgRequired();
                        cmd.log(LOG_V, "will adjust volume");
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown argument " + option);
                }
            } else {
                if (mode == 2) {
                    if (adjustment == null) {
                        cmd.showError("Error: no valid volume adjustment (null)");
                        return;
                    }
                    switch (adjustment.hashCode()) {
                        case 3522662:
                            if (adjustment.equals(ADJUST_SAME)) {
                                c = 1;
                                break;
                            }
                            c = 65535;
                            break;
                        case 103164673:
                            if (adjustment.equals(ADJUST_LOWER)) {
                                c = 2;
                                break;
                            }
                            c = 65535;
                            break;
                        case 108275692:
                            if (adjustment.equals(ADJUST_RAISE)) {
                                c = 0;
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
                            adjDir = 1;
                            break;
                        case 1:
                            adjDir = 0;
                            break;
                        case 2:
                            adjDir = -1;
                            break;
                        default:
                            cmd.showError("Error: no valid volume adjustment, was " + adjustment + ", expected " + ADJUST_LOWER + "|" + ADJUST_SAME + "|" + ADJUST_RAISE);
                            return;
                    }
                }
                cmd.log(LOG_V, "Connecting to AudioService");
                IAudioService audioService = IAudioService.Stub.asInterface(ServiceManager.checkService("audio"));
                if (audioService == null) {
                    cmd.log(LOG_E, "Error type 2");
                    throw new AndroidException("Can't connect to audio service; is the system running?");
                } else if (mode == 1 && (volIndex > audioService.getStreamMaxVolume(stream) || volIndex < audioService.getStreamMinVolume(stream))) {
                    cmd.showError(String.format("Error: invalid volume index %d for stream %d (should be in [%d..%d])", Integer.valueOf(volIndex), Integer.valueOf(stream), Integer.valueOf(audioService.getStreamMinVolume(stream)), Integer.valueOf(audioService.getStreamMaxVolume(stream))));
                    return;
                } else {
                    int flag = i;
                    String pack = cmd.getClass().getPackage().getName();
                    if (mode == 1) {
                        audioService.setStreamVolume(stream, volIndex, flag, pack);
                    } else if (mode == 2) {
                        audioService.adjustStreamVolume(stream, adjDir, flag, pack);
                    }
                    if (doGet) {
                        cmd.log(LOG_V, "volume is " + audioService.getStreamVolume(stream) + " in range [" + audioService.getStreamMinVolume(stream) + ".." + audioService.getStreamMaxVolume(stream) + "]");
                        return;
                    }
                    return;
                }
            }
        }
    }

    static String streamName(int stream) {
        try {
            return AudioSystem.STREAM_NAMES[stream];
        } catch (ArrayIndexOutOfBoundsException e) {
            return "invalid stream";
        }
    }
}
