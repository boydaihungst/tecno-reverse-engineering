package com.android.server.wm;

import android.util.Slog;
import com.android.server.wm.WindowManagerService;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class ViewServer implements Runnable {
    private static final String COMMAND_PROTOCOL_VERSION = "PROTOCOL";
    private static final String COMMAND_SERVER_VERSION = "SERVER";
    private static final String COMMAND_WINDOW_MANAGER_AUTOLIST = "AUTOLIST";
    private static final String COMMAND_WINDOW_MANAGER_GET_FOCUS = "GET_FOCUS";
    private static final String COMMAND_WINDOW_MANAGER_LIST = "LIST";
    private static final String LOG_TAG = "WindowManager";
    private static final String VALUE_PROTOCOL_VERSION = "4";
    private static final String VALUE_SERVER_VERSION = "4";
    public static final int VIEW_SERVER_DEFAULT_PORT = 4939;
    private static final int VIEW_SERVER_MAX_CONNECTIONS = 10;
    private final int mPort;
    private ServerSocket mServer;
    private Thread mThread;
    private ExecutorService mThreadPool;
    private final WindowManagerService mWindowManager;

    /* JADX INFO: Access modifiers changed from: package-private */
    public ViewServer(WindowManagerService windowManager, int port) {
        this.mWindowManager = windowManager;
        this.mPort = port;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean start() throws IOException {
        if (this.mThread != null) {
            return false;
        }
        this.mServer = new ServerSocket(this.mPort, 10, InetAddress.getLocalHost());
        this.mThread = new Thread(this, "Remote View Server [port=" + this.mPort + "]");
        this.mThreadPool = Executors.newFixedThreadPool(10);
        this.mThread.start();
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean stop() {
        Thread thread = this.mThread;
        if (thread != null) {
            thread.interrupt();
            ExecutorService executorService = this.mThreadPool;
            if (executorService != null) {
                try {
                    executorService.shutdownNow();
                } catch (SecurityException e) {
                    Slog.w("WindowManager", "Could not stop all view server threads");
                }
            }
            this.mThreadPool = null;
            this.mThread = null;
            try {
                this.mServer.close();
                this.mServer = null;
                return true;
            } catch (IOException e2) {
                Slog.w("WindowManager", "Could not close the view server");
                return false;
            }
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isRunning() {
        Thread thread = this.mThread;
        return thread != null && thread.isAlive();
    }

    @Override // java.lang.Runnable
    public void run() {
        while (Thread.currentThread() == this.mThread) {
            try {
                Socket client = this.mServer.accept();
                ExecutorService executorService = this.mThreadPool;
                if (executorService != null) {
                    executorService.submit(new ViewServerWorker(client));
                } else {
                    try {
                        client.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } catch (Exception e2) {
                Slog.w("WindowManager", "Connection error: ", e2);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX WARN: Unsupported multi-entry loop pattern (BACK_EDGE: B:19:0x0035 -> B:23:0x0036). Please submit an issue!!! */
    public static boolean writeValue(Socket client, String value) {
        boolean result;
        BufferedWriter out = null;
        try {
            try {
                OutputStream clientStream = client.getOutputStream();
                out = new BufferedWriter(new OutputStreamWriter(clientStream), 8192);
                out.write(value);
                out.write("\n");
                out.flush();
                result = true;
                out.close();
            } catch (Exception e) {
                result = false;
                if (out != null) {
                    out.close();
                }
            } catch (Throwable th) {
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException e2) {
                    }
                }
                throw th;
            }
        } catch (IOException e3) {
            result = false;
        }
        return result;
    }

    /* loaded from: classes2.dex */
    class ViewServerWorker implements Runnable, WindowManagerService.WindowChangeListener {
        private Socket mClient;
        private boolean mNeedWindowListUpdate = false;
        private boolean mNeedFocusedWindowUpdate = false;

        public ViewServerWorker(Socket client) {
            this.mClient = client;
        }

        @Override // java.lang.Runnable
        public void run() {
            String command;
            String parameters;
            boolean result;
            Socket socket;
            BufferedReader in = null;
            try {
                try {
                    try {
                        in = new BufferedReader(new InputStreamReader(this.mClient.getInputStream()), 1024);
                        String request = in.readLine();
                        int index = request.indexOf(32);
                        if (index == -1) {
                            command = request;
                            parameters = "";
                        } else {
                            command = request.substring(0, index);
                            parameters = request.substring(index + 1);
                        }
                        if (ViewServer.COMMAND_PROTOCOL_VERSION.equalsIgnoreCase(command)) {
                            result = ViewServer.writeValue(this.mClient, "4");
                        } else if (ViewServer.COMMAND_SERVER_VERSION.equalsIgnoreCase(command)) {
                            result = ViewServer.writeValue(this.mClient, "4");
                        } else if (ViewServer.COMMAND_WINDOW_MANAGER_LIST.equalsIgnoreCase(command)) {
                            result = ViewServer.this.mWindowManager.viewServerListWindows(this.mClient);
                        } else if (ViewServer.COMMAND_WINDOW_MANAGER_GET_FOCUS.equalsIgnoreCase(command)) {
                            result = ViewServer.this.mWindowManager.viewServerGetFocusedWindow(this.mClient);
                        } else if (ViewServer.COMMAND_WINDOW_MANAGER_AUTOLIST.equalsIgnoreCase(command)) {
                            result = windowManagerAutolistLoop();
                        } else {
                            result = ViewServer.this.mWindowManager.viewServerWindowCommand(this.mClient, command, parameters);
                        }
                        if (!result) {
                            Slog.w("WindowManager", "An error occurred with the command: " + command);
                        }
                        try {
                            in.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        socket = this.mClient;
                    } catch (Throwable th) {
                        if (in != null) {
                            try {
                                in.close();
                            } catch (IOException e2) {
                                e2.printStackTrace();
                            }
                        }
                        Socket socket2 = this.mClient;
                        if (socket2 != null) {
                            try {
                                socket2.close();
                            } catch (IOException e3) {
                                e3.printStackTrace();
                            }
                        }
                        throw th;
                    }
                } catch (IOException e4) {
                    Slog.w("WindowManager", "Connection error: ", e4);
                    if (in != null) {
                        try {
                            in.close();
                        } catch (IOException e5) {
                            e5.printStackTrace();
                        }
                    }
                    Socket socket3 = this.mClient;
                    if (socket3 != null) {
                        socket3.close();
                    } else {
                        return;
                    }
                }
                if (socket != null) {
                    socket.close();
                }
            } catch (IOException e6) {
                e6.printStackTrace();
            }
        }

        @Override // com.android.server.wm.WindowManagerService.WindowChangeListener
        public void windowsChanged() {
            synchronized (this) {
                this.mNeedWindowListUpdate = true;
                notifyAll();
            }
        }

        @Override // com.android.server.wm.WindowManagerService.WindowChangeListener
        public void focusChanged() {
            synchronized (this) {
                this.mNeedFocusedWindowUpdate = true;
                notifyAll();
            }
        }

        private boolean windowManagerAutolistLoop() {
            boolean z;
            ViewServer.this.mWindowManager.addWindowChangeListener(this);
            BufferedWriter out = null;
            try {
                out = new BufferedWriter(new OutputStreamWriter(this.mClient.getOutputStream()));
                while (!Thread.interrupted()) {
                    boolean needWindowListUpdate = false;
                    boolean needFocusedWindowUpdate = false;
                    synchronized (this) {
                        while (true) {
                            z = this.mNeedWindowListUpdate;
                            if (z || this.mNeedFocusedWindowUpdate) {
                                break;
                            }
                            wait();
                        }
                        if (z) {
                            this.mNeedWindowListUpdate = false;
                            needWindowListUpdate = true;
                        }
                        if (this.mNeedFocusedWindowUpdate) {
                            this.mNeedFocusedWindowUpdate = false;
                            needFocusedWindowUpdate = true;
                        }
                    }
                    if (needWindowListUpdate) {
                        out.write("LIST UPDATE\n");
                        out.flush();
                    }
                    if (needFocusedWindowUpdate) {
                        out.write("ACTION_FOCUS UPDATE\n");
                        out.flush();
                    }
                }
                try {
                    out.close();
                } catch (IOException e) {
                }
            } catch (Exception e2) {
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException e3) {
                    }
                }
            } catch (Throwable th) {
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException e4) {
                    }
                }
                ViewServer.this.mWindowManager.removeWindowChangeListener(this);
                throw th;
            }
            ViewServer.this.mWindowManager.removeWindowChangeListener(this);
            return true;
        }
    }
}
