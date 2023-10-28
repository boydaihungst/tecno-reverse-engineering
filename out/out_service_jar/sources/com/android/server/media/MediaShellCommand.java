package com.android.server.media;

import android.app.ActivityThread;
import android.media.MediaMetadata;
import android.media.session.ISessionManager;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.ShellCommand;
import android.os.SystemClock;
import android.text.TextUtils;
import android.view.KeyEvent;
import com.android.internal.util.FrameworkStatsLog;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;
/* loaded from: classes2.dex */
public class MediaShellCommand extends ShellCommand {
    private static MediaSessionManager sMediaSessionManager;
    private static ActivityThread sThread;
    private PrintWriter mErrorWriter;
    private InputStream mInput;
    private final String mPackageName;
    private ISessionManager mSessionService;
    private PrintWriter mWriter;

    public MediaShellCommand(String packageName) {
        this.mPackageName = packageName;
    }

    public int onCommand(String cmd) {
        this.mWriter = getOutPrintWriter();
        this.mErrorWriter = getErrPrintWriter();
        this.mInput = getRawInputStream();
        if (TextUtils.isEmpty(cmd)) {
            return handleDefaultCommands(cmd);
        }
        if (sThread == null) {
            Looper.prepare();
            ActivityThread currentActivityThread = ActivityThread.currentActivityThread();
            sThread = currentActivityThread;
            sMediaSessionManager = (MediaSessionManager) currentActivityThread.getSystemContext().getSystemService("media_session");
        }
        ISessionManager asInterface = ISessionManager.Stub.asInterface(ServiceManager.checkService("media_session"));
        this.mSessionService = asInterface;
        if (asInterface == null) {
            throw new IllegalStateException("Can't connect to media session service; is the system running?");
        }
        try {
            if (cmd.equals("dispatch")) {
                runDispatch();
                return 0;
            } else if (cmd.equals("list-sessions")) {
                runListSessions();
                return 0;
            } else if (cmd.equals("monitor")) {
                runMonitor();
                return 0;
            } else if (cmd.equals("volume")) {
                runVolume();
                return 0;
            } else {
                showError("Error: unknown command '" + cmd + "'");
                return -1;
            }
        } catch (Exception e) {
            showError(e.toString());
            return -1;
        }
    }

    public void onHelp() {
        this.mWriter.println("usage: media_session [subcommand] [options]");
        this.mWriter.println("       media_session dispatch KEY");
        this.mWriter.println("       media_session list-sessions");
        this.mWriter.println("       media_session monitor <tag>");
        this.mWriter.println("       media_session volume [options]");
        this.mWriter.println();
        this.mWriter.println("media_session dispatch: dispatch a media key to the system.");
        this.mWriter.println("                KEY may be: play, pause, play-pause, mute, headsethook,");
        this.mWriter.println("                stop, next, previous, rewind, record, fast-forward.");
        this.mWriter.println("media_session list-sessions: print a list of the current sessions.");
        this.mWriter.println("media_session monitor: monitor updates to the specified session.");
        this.mWriter.println("                       Use the tag from list-sessions.");
        this.mWriter.println("media_session volume:  " + VolumeCtrl.USAGE);
        this.mWriter.println();
    }

    private void sendMediaKey(KeyEvent event) {
        try {
            this.mSessionService.dispatchMediaKeyEvent(this.mPackageName, false, event, false);
        } catch (RemoteException e) {
        }
    }

    private void runMonitor() throws Exception {
        String id = getNextArgRequired();
        if (id == null) {
            showError("Error: must include a session id");
            return;
        }
        boolean success = false;
        try {
            List<MediaController> controllers = sMediaSessionManager.getActiveSessions(null);
            Iterator<MediaController> it = controllers.iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                MediaController controller = it.next();
                if (controller != null) {
                    try {
                        if (id.equals(controller.getTag())) {
                            ControllerMonitor monitor = new ControllerMonitor(controller);
                            monitor.run();
                            success = true;
                            break;
                        }
                        continue;
                    } catch (RemoteException e) {
                    }
                }
            }
        } catch (Exception e2) {
            this.mErrorWriter.println("***Error monitoring session*** " + e2.getMessage());
        }
        if (!success) {
            this.mErrorWriter.println("No session found with id " + id);
        }
    }

    private void runDispatch() throws Exception {
        int keycode;
        String cmd = getNextArgRequired();
        if ("play".equals(cmd)) {
            keycode = 126;
        } else if ("pause".equals(cmd)) {
            keycode = FrameworkStatsLog.DEVICE_POLICY_EVENT__EVENT_ID__SET_AUTO_TIME;
        } else if ("play-pause".equals(cmd)) {
            keycode = 85;
        } else if ("mute".equals(cmd)) {
            keycode = 91;
        } else if ("headsethook".equals(cmd)) {
            keycode = 79;
        } else if ("stop".equals(cmd)) {
            keycode = 86;
        } else if ("next".equals(cmd)) {
            keycode = 87;
        } else if ("previous".equals(cmd)) {
            keycode = 88;
        } else if ("rewind".equals(cmd)) {
            keycode = 89;
        } else if ("record".equals(cmd)) {
            keycode = 130;
        } else if (!"fast-forward".equals(cmd)) {
            showError("Error: unknown dispatch code '" + cmd + "'");
            return;
        } else {
            keycode = 90;
        }
        long now = SystemClock.uptimeMillis();
        int i = keycode;
        sendMediaKey(new KeyEvent(now, now, 0, i, 0, 0, -1, 0, 0, 257));
        sendMediaKey(new KeyEvent(now, now, 1, i, 0, 0, -1, 0, 0, 257));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void log(String code, String msg) {
        this.mWriter.println(code + " " + msg);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void showError(String errMsg) {
        onHelp();
        this.mErrorWriter.println(errMsg);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public class ControllerCallback extends MediaController.Callback {
        ControllerCallback() {
        }

        @Override // android.media.session.MediaController.Callback
        public void onSessionDestroyed() {
            MediaShellCommand.this.mWriter.println("onSessionDestroyed. Enter q to quit.");
        }

        @Override // android.media.session.MediaController.Callback
        public void onSessionEvent(String event, Bundle extras) {
            MediaShellCommand.this.mWriter.println("onSessionEvent event=" + event + ", extras=" + extras);
        }

        @Override // android.media.session.MediaController.Callback
        public void onPlaybackStateChanged(PlaybackState state) {
            MediaShellCommand.this.mWriter.println("onPlaybackStateChanged " + state);
        }

        @Override // android.media.session.MediaController.Callback
        public void onMetadataChanged(MediaMetadata metadata) {
            String mmString = metadata == null ? null : "title=" + metadata.getDescription();
            MediaShellCommand.this.mWriter.println("onMetadataChanged " + mmString);
        }

        @Override // android.media.session.MediaController.Callback
        public void onQueueChanged(List<MediaSession.QueueItem> queue) {
            MediaShellCommand.this.mWriter.println("onQueueChanged, " + (queue == null ? "null queue" : " size=" + queue.size()));
        }

        @Override // android.media.session.MediaController.Callback
        public void onQueueTitleChanged(CharSequence title) {
            MediaShellCommand.this.mWriter.println("onQueueTitleChange " + ((Object) title));
        }

        @Override // android.media.session.MediaController.Callback
        public void onExtrasChanged(Bundle extras) {
            MediaShellCommand.this.mWriter.println("onExtrasChanged " + extras);
        }

        @Override // android.media.session.MediaController.Callback
        public void onAudioInfoChanged(MediaController.PlaybackInfo info) {
            MediaShellCommand.this.mWriter.println("onAudioInfoChanged " + info);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class ControllerMonitor {
        private final MediaController mController;
        private final ControllerCallback mControllerCallback;

        ControllerMonitor(MediaController controller) {
            this.mController = controller;
            this.mControllerCallback = new ControllerCallback();
        }

        void printUsageMessage() {
            try {
                MediaShellCommand.this.mWriter.println("V2Monitoring session " + this.mController.getTag() + "...  available commands: play, pause, next, previous");
            } catch (RuntimeException e) {
                MediaShellCommand.this.mWriter.println("Error trying to monitor session!");
            }
            MediaShellCommand.this.mWriter.println("(q)uit: finish monitoring");
        }

        /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [322=4] */
        void run() throws RemoteException {
            printUsageMessage();
            HandlerThread cbThread = new HandlerThread("MediaCb") { // from class: com.android.server.media.MediaShellCommand.ControllerMonitor.1
                @Override // android.os.HandlerThread
                protected void onLooperPrepared() {
                    try {
                        ControllerMonitor.this.mController.registerCallback(ControllerMonitor.this.mControllerCallback);
                    } catch (RuntimeException e) {
                        MediaShellCommand.this.mErrorWriter.println("Error registering monitor callback");
                    }
                }
            };
            cbThread.start();
            try {
                try {
                    try {
                        InputStreamReader converter = new InputStreamReader(MediaShellCommand.this.mInput);
                        BufferedReader in = new BufferedReader(converter);
                        while (true) {
                            MediaShellCommand.this.mWriter.flush();
                            MediaShellCommand.this.mErrorWriter.flush();
                            String line = in.readLine();
                            if (line == null) {
                                break;
                            }
                            boolean addNewline = true;
                            if (line.length() > 0) {
                                if ("q".equals(line) || "quit".equals(line)) {
                                    break;
                                } else if ("play".equals(line)) {
                                    dispatchKeyCode(126);
                                } else if ("pause".equals(line)) {
                                    dispatchKeyCode(FrameworkStatsLog.DEVICE_POLICY_EVENT__EVENT_ID__SET_AUTO_TIME);
                                } else if ("next".equals(line)) {
                                    dispatchKeyCode(87);
                                } else if ("previous".equals(line)) {
                                    dispatchKeyCode(88);
                                } else {
                                    MediaShellCommand.this.mErrorWriter.println("Invalid command: " + line);
                                }
                            } else {
                                addNewline = false;
                            }
                            synchronized (this) {
                                if (addNewline) {
                                    MediaShellCommand.this.mWriter.println("");
                                }
                                printUsageMessage();
                            }
                        }
                        cbThread.getLooper().quit();
                        this.mController.unregisterCallback(this.mControllerCallback);
                    } catch (Throwable th) {
                        cbThread.getLooper().quit();
                        try {
                            this.mController.unregisterCallback(this.mControllerCallback);
                        } catch (Exception e) {
                        }
                        throw th;
                    }
                } catch (IOException e2) {
                    e2.printStackTrace();
                    cbThread.getLooper().quit();
                    this.mController.unregisterCallback(this.mControllerCallback);
                }
            } catch (Exception e3) {
            }
        }

        private void dispatchKeyCode(int keyCode) {
            long now = SystemClock.uptimeMillis();
            KeyEvent down = new KeyEvent(now, now, 0, keyCode, 0, 0, -1, 0, 0, 257);
            KeyEvent up = new KeyEvent(now, now, 1, keyCode, 0, 0, -1, 0, 0, 257);
            try {
                this.mController.dispatchMediaButtonEvent(down);
                this.mController.dispatchMediaButtonEvent(up);
            } catch (RuntimeException e) {
                MediaShellCommand.this.mErrorWriter.println("Failed to dispatch " + keyCode);
            }
        }
    }

    private void runListSessions() {
        this.mWriter.println("Sessions:");
        try {
            List<MediaController> controllers = sMediaSessionManager.getActiveSessions(null);
            for (MediaController controller : controllers) {
                if (controller != null) {
                    try {
                        this.mWriter.println("  tag=" + controller.getTag() + ", package=" + controller.getPackageName());
                    } catch (RuntimeException e) {
                    }
                }
            }
        } catch (Exception e2) {
            this.mErrorWriter.println("***Error listing sessions***");
        }
    }

    private void runVolume() throws Exception {
        VolumeCtrl.run(this);
    }
}
