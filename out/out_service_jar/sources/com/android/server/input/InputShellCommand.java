package com.android.server.input;

import android.hardware.input.InputManager;
import android.os.ShellCommand;
import android.os.SystemClock;
import android.util.ArrayMap;
import android.util.IntArray;
import android.view.InputDevice;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import com.android.server.usb.descriptors.UsbACInterface;
import com.android.server.usb.descriptors.UsbTerminalTypes;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.Map;
/* loaded from: classes.dex */
public class InputShellCommand extends ShellCommand {
    private static final int DEFAULT_BUTTON_STATE = 0;
    private static final int DEFAULT_DEVICE_ID = 0;
    private static final int DEFAULT_EDGE_FLAGS = 0;
    private static final int DEFAULT_FLAGS = 0;
    private static final int DEFAULT_META_STATE = 0;
    private static final float DEFAULT_PRECISION_X = 1.0f;
    private static final float DEFAULT_PRECISION_Y = 1.0f;
    private static final float DEFAULT_PRESSURE = 1.0f;
    private static final float DEFAULT_SIZE = 1.0f;
    private static final String INVALID_ARGUMENTS = "Error: Invalid arguments for command: ";
    private static final String INVALID_DISPLAY_ARGUMENTS = "Error: Invalid arguments for display ID.";
    private static final Map<Integer, Integer> MODIFIER;
    private static final float NO_PRESSURE = 0.0f;
    private static final Map<String, Integer> SOURCES;

    static {
        Map<Integer, Integer> map = new ArrayMap<>();
        map.put(113, 12288);
        map.put(114, 20480);
        map.put(57, 18);
        map.put(58, 34);
        map.put(59, 65);
        map.put(60, 129);
        map.put(117, 196608);
        map.put(118, 327680);
        MODIFIER = Collections.unmodifiableMap(map);
        Map<String, Integer> map2 = new ArrayMap<>();
        map2.put("keyboard", 257);
        map2.put("dpad", Integer.valueOf((int) UsbTerminalTypes.TERMINAL_IN_MIC));
        map2.put("gamepad", Integer.valueOf((int) UsbTerminalTypes.TERMINAL_BIDIR_HANDSET));
        map2.put("touchscreen", Integer.valueOf((int) UsbACInterface.FORMAT_II_AC3));
        map2.put("mouse", Integer.valueOf((int) UsbACInterface.FORMAT_III_IEC1937_MPEG1_Layer1));
        map2.put("stylus", 16386);
        map2.put("trackball", 65540);
        map2.put("touchpad", 1048584);
        map2.put("touchnavigation", 2097152);
        map2.put("joystick", 16777232);
        SOURCES = Collections.unmodifiableMap(map2);
    }

    private void injectKeyEvent(KeyEvent event) {
        InputManager.getInstance().injectInputEvent(event, 2);
    }

    private int getInputDeviceId(int inputSource) {
        int[] devIds = InputDevice.getDeviceIds();
        for (int devId : devIds) {
            InputDevice inputDev = InputDevice.getDevice(devId);
            if (inputDev.supportsSource(inputSource)) {
                return devId;
            }
        }
        return 0;
    }

    private int getDisplayId() {
        String displayArg = getNextArgRequired();
        if ("INVALID_DISPLAY".equalsIgnoreCase(displayArg)) {
            return -1;
        }
        if ("DEFAULT_DISPLAY".equalsIgnoreCase(displayArg)) {
            return 0;
        }
        try {
            int displayId = Integer.parseInt(displayArg);
            if (displayId == -1) {
                return -1;
            }
            return Math.max(displayId, 0);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(INVALID_DISPLAY_ARGUMENTS);
        }
    }

    private void injectMotionEvent(int inputSource, int action, long downTime, long when, float x, float y, float pressure, int displayId) {
        int displayId2;
        MotionEvent.PointerProperties[] pointerProperties = new MotionEvent.PointerProperties[1];
        MotionEvent.PointerCoords[] pointerCoords = new MotionEvent.PointerCoords[1];
        for (int i = 0; i < 1; i++) {
            pointerProperties[i] = new MotionEvent.PointerProperties();
            pointerProperties[i].id = i;
            pointerProperties[i].toolType = getToolType(inputSource);
            pointerCoords[i] = new MotionEvent.PointerCoords();
            pointerCoords[i].x = x;
            pointerCoords[i].y = y;
            pointerCoords[i].pressure = pressure;
            pointerCoords[i].size = 1.0f;
        }
        if (displayId == -1 && (inputSource & 2) != 0) {
            displayId2 = 0;
        } else {
            displayId2 = displayId;
        }
        MotionEvent event = MotionEvent.obtain(downTime, when, action, 1, pointerProperties, pointerCoords, 0, 0, 1.0f, 1.0f, getInputDeviceId(inputSource), 0, inputSource, displayId2, 0);
        InputManager.getInstance().injectInputEvent(event, 2);
    }

    private float lerp(float a, float b, float alpha) {
        return ((b - a) * alpha) + a;
    }

    private int getSource(int inputSource, int defaultSource) {
        return inputSource == 0 ? defaultSource : inputSource;
    }

    private int getToolType(int inputSource) {
        switch (inputSource) {
            case UsbACInterface.FORMAT_II_AC3 /* 4098 */:
            case 1048584:
            case 2097152:
                return 1;
            case UsbACInterface.FORMAT_III_IEC1937_MPEG1_Layer1 /* 8194 */:
            case 65540:
            case 131076:
                return 3;
            case 16386:
            case 49154:
                return 2;
            default:
                return 0;
        }
    }

    public final int onCommand(String cmd) {
        String arg = cmd;
        int inputSource = 0;
        Map<String, Integer> map = SOURCES;
        if (map.containsKey(arg)) {
            inputSource = map.get(arg).intValue();
            arg = getNextArgRequired();
        }
        int displayId = -1;
        if ("-d".equals(arg)) {
            displayId = getDisplayId();
            arg = getNextArgRequired();
        }
        try {
            if ("text".equals(arg)) {
                runText(inputSource, displayId);
                return 0;
            } else if ("keyevent".equals(arg)) {
                runKeyEvent(inputSource, displayId);
                return 0;
            } else if ("tap".equals(arg)) {
                runTap(inputSource, displayId);
                return 0;
            } else if ("swipe".equals(arg)) {
                runSwipe(inputSource, displayId);
                return 0;
            } else if ("draganddrop".equals(arg)) {
                runDragAndDrop(inputSource, displayId);
                return 0;
            } else if ("press".equals(arg)) {
                runPress(inputSource, displayId);
                return 0;
            } else if ("roll".equals(arg)) {
                runRoll(inputSource, displayId);
                return 0;
            } else if ("motionevent".equals(arg)) {
                runMotionEvent(inputSource, displayId);
                return 0;
            } else if ("keycombination".equals(arg)) {
                runKeyCombination(inputSource, displayId);
                return 0;
            } else {
                handleDefaultCommands(arg);
                return 0;
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(INVALID_ARGUMENTS + arg);
        }
    }

    public final void onHelp() {
        PrintWriter out = getOutPrintWriter();
        try {
            out.println("Usage: input [<source>] [-d DISPLAY_ID] <command> [<arg>...]");
            out.println();
            out.println("The sources are: ");
            for (String src : SOURCES.keySet()) {
                out.println("      " + src);
            }
            out.println();
            out.printf("-d: specify the display ID.\n      (Default: %d for key event, %d for motion event if not specified.)", -1, 0);
            out.println();
            out.println("The commands and default sources are:");
            out.println("      text <string> (Default: touchscreen)");
            out.println("      keyevent [--longpress|--doubletap] <key code number or name> ... (Default: keyboard)");
            out.println("      tap <x> <y> (Default: touchscreen)");
            out.println("      swipe <x1> <y1> <x2> <y2> [duration(ms)] (Default: touchscreen)");
            out.println("      draganddrop <x1> <y1> <x2> <y2> [duration(ms)] (Default: touchscreen)");
            out.println("      press (Default: trackball)");
            out.println("      roll <dx> <dy> (Default: trackball)");
            out.println("      motionevent <DOWN|UP|MOVE|CANCEL> <x> <y> (Default: touchscreen)");
            out.println("      keycombination [-t duration(ms)] <key code 1> <key code 2> ... (Default: keyboard, the key order is important here.)");
            if (out != null) {
                out.close();
            }
        } catch (Throwable th) {
            if (out != null) {
                try {
                    out.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
            }
            throw th;
        }
    }

    private void runText(int inputSource, int displayId) {
        sendText(getSource(inputSource, 257), getNextArgRequired(), displayId);
    }

    private void sendText(int source, String text, int displayId) {
        StringBuilder buff = new StringBuilder(text);
        boolean escapeFlag = false;
        int i = 0;
        while (i < buff.length()) {
            if (escapeFlag) {
                escapeFlag = false;
                if (buff.charAt(i) == 's') {
                    buff.setCharAt(i, ' ');
                    i--;
                    buff.deleteCharAt(i);
                }
            }
            if (buff.charAt(i) == '%') {
                escapeFlag = true;
            }
            i++;
        }
        char[] chars = buff.toString().toCharArray();
        KeyCharacterMap kcm = KeyCharacterMap.load(-1);
        KeyEvent[] events = kcm.getEvents(chars);
        for (KeyEvent e : events) {
            if (source != e.getSource()) {
                e.setSource(source);
            }
            e.setDisplayId(displayId);
            injectKeyEvent(e);
        }
    }

    private void runKeyEvent(int inputSource, int displayId) {
        String nextArg;
        String arg = getNextArgRequired();
        boolean longpress = "--longpress".equals(arg);
        if (longpress) {
            arg = getNextArgRequired();
        } else {
            boolean doubleTap = "--doubletap".equals(arg);
            if (doubleTap) {
                int keycode = KeyEvent.keyCodeFromString(getNextArgRequired());
                sendKeyDoubleTap(inputSource, keycode, displayId);
                return;
            }
        }
        do {
            int keycode2 = KeyEvent.keyCodeFromString(arg);
            sendKeyEvent(inputSource, keycode2, longpress, displayId);
            nextArg = getNextArg();
            arg = nextArg;
        } while (nextArg != null);
    }

    private void sendKeyEvent(int inputSource, int keyCode, boolean longpress, int displayId) {
        long now = SystemClock.uptimeMillis();
        KeyEvent event = new KeyEvent(now, now, 0, keyCode, 0, 0, -1, 0, 0, inputSource);
        event.setDisplayId(displayId);
        injectKeyEvent(event);
        if (longpress) {
            sleep(ViewConfiguration.getLongPressTimeout());
            long nextEventTime = ViewConfiguration.getLongPressTimeout() + now;
            injectKeyEvent(KeyEvent.changeTimeRepeat(event, nextEventTime, 1, 128));
        }
        injectKeyEvent(KeyEvent.changeAction(event, 1));
    }

    private void sendKeyDoubleTap(int inputSource, int keyCode, int displayId) {
        sendKeyEvent(inputSource, keyCode, false, displayId);
        sleep(ViewConfiguration.getDoubleTapMinTime());
        sendKeyEvent(inputSource, keyCode, false, displayId);
    }

    private void runTap(int inputSource, int displayId) {
        sendTap(getSource(inputSource, UsbACInterface.FORMAT_II_AC3), Float.parseFloat(getNextArgRequired()), Float.parseFloat(getNextArgRequired()), displayId);
    }

    private void sendTap(int inputSource, float x, float y, int displayId) {
        long now = SystemClock.uptimeMillis();
        injectMotionEvent(inputSource, 0, now, now, x, y, 1.0f, displayId);
        injectMotionEvent(inputSource, 1, now, now, x, y, 0.0f, displayId);
    }

    private void runPress(int inputSource, int displayId) {
        sendTap(getSource(inputSource, 65540), 0.0f, 0.0f, displayId);
    }

    private void runSwipe(int inputSource, int displayId) {
        sendSwipe(getSource(inputSource, UsbACInterface.FORMAT_II_AC3), displayId, false);
    }

    private void sendSwipe(int inputSource, int displayId, boolean isDragDrop) {
        int duration;
        float x1 = Float.parseFloat(getNextArgRequired());
        float y1 = Float.parseFloat(getNextArgRequired());
        float x2 = Float.parseFloat(getNextArgRequired());
        float y2 = Float.parseFloat(getNextArgRequired());
        String durationArg = getNextArg();
        int duration2 = durationArg != null ? Integer.parseInt(durationArg) : -1;
        if (duration2 >= 0) {
            duration = duration2;
        } else {
            duration = 300;
        }
        long down = SystemClock.uptimeMillis();
        float y12 = y1;
        int duration3 = duration;
        injectMotionEvent(inputSource, 0, down, down, x1, y1, 1.0f, displayId);
        if (isDragDrop) {
            sleep(ViewConfiguration.getLongPressTimeout());
        }
        long now = SystemClock.uptimeMillis();
        long endTime = down + duration3;
        long now2 = now;
        while (now2 < endTime) {
            long elapsedTime = now2 - down;
            float alpha = ((float) elapsedTime) / duration3;
            float y13 = y12;
            injectMotionEvent(inputSource, 2, down, now2, lerp(x1, x2, alpha), lerp(y13, y2, alpha), 1.0f, displayId);
            now2 = SystemClock.uptimeMillis();
            y12 = y13;
        }
        injectMotionEvent(inputSource, 1, down, now2, x2, y2, 0.0f, displayId);
    }

    private void runDragAndDrop(int inputSource, int displayId) {
        sendSwipe(getSource(inputSource, UsbACInterface.FORMAT_II_AC3), displayId, true);
    }

    private void runRoll(int inputSource, int displayId) {
        sendMove(getSource(inputSource, 65540), Float.parseFloat(getNextArgRequired()), Float.parseFloat(getNextArgRequired()), displayId);
    }

    private void sendMove(int inputSource, float dx, float dy, int displayId) {
        long now = SystemClock.uptimeMillis();
        injectMotionEvent(inputSource, 2, now, now, dx, dy, 0.0f, displayId);
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    private int getAction() {
        char c;
        String actionString = getNextArgRequired();
        String upperCase = actionString.toUpperCase();
        switch (upperCase.hashCode()) {
            case 2715:
                if (upperCase.equals("UP")) {
                    c = 1;
                    break;
                }
                c = 65535;
                break;
            case 2104482:
                if (upperCase.equals("DOWN")) {
                    c = 0;
                    break;
                }
                c = 65535;
                break;
            case 2372561:
                if (upperCase.equals("MOVE")) {
                    c = 2;
                    break;
                }
                c = 65535;
                break;
            case 1980572282:
                if (upperCase.equals("CANCEL")) {
                    c = 3;
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
                return 0;
            case 1:
                return 1;
            case 2:
                return 2;
            case 3:
                return 3;
            default:
                throw new IllegalArgumentException("Unknown action: " + actionString);
        }
    }

    private void runMotionEvent(int inputSource, int displayId) {
        float x;
        float y;
        int inputSource2 = getSource(inputSource, UsbACInterface.FORMAT_II_AC3);
        int action = getAction();
        if (action == 0 || action == 2 || action == 1) {
            float x2 = Float.parseFloat(getNextArgRequired());
            float y2 = Float.parseFloat(getNextArgRequired());
            x = x2;
            y = y2;
        } else {
            String xString = getNextArg();
            String yString = getNextArg();
            if (xString != null && yString != null) {
                float x3 = Float.parseFloat(xString);
                float y3 = Float.parseFloat(yString);
                x = x3;
                y = y3;
            } else {
                x = 0.0f;
                y = 0.0f;
            }
        }
        sendMotionEvent(inputSource2, action, x, y, displayId);
    }

    private void sendMotionEvent(int inputSource, int action, float x, float y, int displayId) {
        float pressure = (action == 0 || action == 2) ? 1.0f : 0.0f;
        long now = SystemClock.uptimeMillis();
        injectMotionEvent(inputSource, action, now, now, x, y, pressure, displayId);
    }

    private void runKeyCombination(int inputSource, int displayId) {
        long duration;
        String arg = getNextArgRequired();
        if (!"-t".equals(arg)) {
            duration = 0;
        } else {
            long duration2 = Integer.parseInt(getNextArgRequired());
            arg = getNextArgRequired();
            duration = duration2;
        }
        IntArray keyCodes = new IntArray();
        while (arg != null) {
            int keyCode = KeyEvent.keyCodeFromString(arg);
            if (keyCode == 0) {
                throw new IllegalArgumentException("Unknown keycode: " + arg);
            }
            keyCodes.add(keyCode);
            arg = getNextArg();
        }
        if (keyCodes.size() < 2) {
            throw new IllegalArgumentException("keycombination requires at least 2 keycodes");
        }
        sendKeyCombination(inputSource, keyCodes, displayId, duration);
    }

    private void injectKeyEventAsync(KeyEvent event) {
        InputManager.getInstance().injectInputEvent(event, 0);
    }

    private void sendKeyCombination(int inputSource, IntArray keyCodes, int displayId, long duration) {
        long now = SystemClock.uptimeMillis();
        int count = keyCodes.size();
        KeyEvent[] events = new KeyEvent[count];
        int metaState = 0;
        int i = 0;
        while (i < count) {
            int keyCode = keyCodes.get(i);
            int i2 = i;
            KeyEvent event = new KeyEvent(now, now, 0, keyCode, 0, metaState, -1, 0, 0, inputSource);
            event.setDisplayId(displayId);
            events = events;
            events[i2] = event;
            metaState |= MODIFIER.getOrDefault(Integer.valueOf(keyCode), 0).intValue();
            i = i2 + 1;
            count = count;
        }
        for (KeyEvent event2 : events) {
            injectKeyEventAsync(event2);
        }
        sleep(duration);
        int length = events.length;
        int i3 = 0;
        while (i3 < length) {
            KeyEvent event3 = events[i3];
            int keyCode2 = event3.getKeyCode();
            KeyEvent upEvent = new KeyEvent(now, now, 1, keyCode2, 0, metaState, -1, 0, 0, inputSource);
            injectKeyEventAsync(upEvent);
            metaState &= ~MODIFIER.getOrDefault(Integer.valueOf(keyCode2), 0).intValue();
            i3++;
            length = length;
            events = events;
        }
    }

    private void sleep(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
