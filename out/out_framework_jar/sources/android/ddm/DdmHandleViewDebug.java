package android.ddm;

import android.util.Log;
import android.view.View;
import android.view.ViewDebug;
import android.view.WindowManagerGlobal;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.Method;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import org.apache.harmony.dalvik.ddmc.Chunk;
import org.apache.harmony.dalvik.ddmc.ChunkHandler;
import org.apache.harmony.dalvik.ddmc.DdmServer;
/* loaded from: classes.dex */
public class DdmHandleViewDebug extends DdmHandle {
    private static final int ERR_EXCEPTION = -3;
    private static final int ERR_INVALID_OP = -1;
    private static final int ERR_INVALID_PARAM = -2;
    private static final String TAG = "DdmViewDebug";
    private static final int VUOP_CAPTURE_VIEW = 1;
    private static final int VUOP_DUMP_DISPLAYLIST = 2;
    private static final int VUOP_INVOKE_VIEW_METHOD = 4;
    private static final int VUOP_PROFILE_VIEW = 3;
    private static final int VUOP_SET_LAYOUT_PARAMETER = 5;
    private static final int VURT_CAPTURE_LAYERS = 2;
    private static final int VURT_DUMP_HIERARCHY = 1;
    private static final int VURT_DUMP_THEME = 3;
    private static final int CHUNK_VULW = ChunkHandler.type("VULW");
    private static final int CHUNK_VURT = ChunkHandler.type("VURT");
    private static final int CHUNK_VUOP = ChunkHandler.type("VUOP");
    private static final DdmHandleViewDebug sInstance = new DdmHandleViewDebug();

    private DdmHandleViewDebug() {
    }

    public static void register() {
        int i = CHUNK_VULW;
        DdmHandleViewDebug ddmHandleViewDebug = sInstance;
        DdmServer.registerHandler(i, ddmHandleViewDebug);
        DdmServer.registerHandler(CHUNK_VURT, ddmHandleViewDebug);
        DdmServer.registerHandler(CHUNK_VUOP, ddmHandleViewDebug);
    }

    public void onConnected() {
    }

    public void onDisconnected() {
    }

    public Chunk handleChunk(Chunk request) {
        int type = request.type;
        if (type == CHUNK_VULW) {
            return listWindows();
        }
        ByteBuffer in = wrapChunk(request);
        int op = in.getInt();
        View rootView = getRootView(in);
        if (rootView == null) {
            return createFailChunk(-2, "Invalid View Root");
        }
        if (type == CHUNK_VURT) {
            if (op == 1) {
                return dumpHierarchy(rootView, in);
            }
            if (op == 2) {
                return captureLayers(rootView);
            }
            if (op == 3) {
                return dumpTheme(rootView);
            }
            return createFailChunk(-1, "Unknown view root operation: " + op);
        }
        View targetView = getTargetView(rootView, in);
        if (targetView == null) {
            return createFailChunk(-2, "Invalid target view");
        }
        if (type == CHUNK_VUOP) {
            switch (op) {
                case 1:
                    return captureView(rootView, targetView);
                case 2:
                    return dumpDisplayLists(rootView, targetView);
                case 3:
                    return profileView(rootView, targetView);
                case 4:
                    return invokeViewMethod(rootView, targetView, in);
                case 5:
                    return setLayoutParameter(rootView, targetView, in);
                default:
                    return createFailChunk(-1, "Unknown view operation: " + op);
            }
        }
        throw new RuntimeException("Unknown packet " + name(type));
    }

    private Chunk listWindows() {
        String[] windowNames = WindowManagerGlobal.getInstance().getViewRootNames();
        int responseLength = 4;
        for (String name : windowNames) {
            responseLength = responseLength + 4 + (name.length() * 2);
        }
        ByteBuffer out = ByteBuffer.allocate(responseLength);
        out.order(ChunkHandler.CHUNK_ORDER);
        out.putInt(windowNames.length);
        for (String name2 : windowNames) {
            out.putInt(name2.length());
            putString(out, name2);
        }
        return new Chunk(CHUNK_VULW, out);
    }

    private View getRootView(ByteBuffer in) {
        try {
            int viewRootNameLength = in.getInt();
            String viewRootName = getString(in, viewRootNameLength);
            return WindowManagerGlobal.getInstance().getRootView(viewRootName);
        } catch (BufferUnderflowException e) {
            return null;
        }
    }

    private View getTargetView(View root, ByteBuffer in) {
        try {
            int viewLength = in.getInt();
            String viewName = getString(in, viewLength);
            return ViewDebug.findView(root, viewName);
        } catch (BufferUnderflowException e) {
            return null;
        }
    }

    /* JADX DEBUG: Failed to insert an additional move for type inference into block B:25:0x002e */
    /* JADX DEBUG: Multi-variable search result rejected for r1v1, resolved type: int */
    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r1v0 */
    /* JADX WARN: Type inference failed for: r1v7, types: [byte[]] */
    private Chunk dumpHierarchy(View rootView, ByteBuffer in) {
        int i = 1;
        boolean skipChildren = in.getInt() > 0;
        boolean includeProperties = in.getInt() > 0;
        boolean v2 = in.hasRemaining() && in.getInt() > 0;
        long start = System.currentTimeMillis();
        ByteArrayOutputStream b = new ByteArrayOutputStream(2097152);
        try {
            if (v2) {
                ViewDebug.dumpv2(rootView, b);
            } else {
                ViewDebug.dump(rootView, skipChildren, includeProperties, b);
            }
            long end = System.currentTimeMillis();
            Log.d(TAG, "Time to obtain view hierarchy (ms): " + (end - start));
            i = b.toByteArray();
            return new Chunk(CHUNK_VURT, (byte[]) i, 0, i.length);
        } catch (IOException | InterruptedException e) {
            return createFailChunk(i, "Unexpected error while obtaining view hierarchy: " + e.getMessage());
        }
    }

    private Chunk captureLayers(View rootView) {
        ByteArrayOutputStream b = new ByteArrayOutputStream(1024);
        DataOutputStream dos = new DataOutputStream(b);
        try {
            try {
                ViewDebug.captureLayers(rootView, dos);
                try {
                    dos.close();
                } catch (IOException e) {
                }
                byte[] data = b.toByteArray();
                return new Chunk(CHUNK_VURT, data, 0, data.length);
            } catch (IOException e2) {
                Chunk createFailChunk = createFailChunk(1, "Unexpected error while obtaining view hierarchy: " + e2.getMessage());
                try {
                    dos.close();
                } catch (IOException e3) {
                }
                return createFailChunk;
            }
        } catch (Throwable th) {
            try {
                dos.close();
            } catch (IOException e4) {
            }
            throw th;
        }
    }

    private Chunk dumpTheme(View rootView) {
        ByteArrayOutputStream b = new ByteArrayOutputStream(1024);
        try {
            ViewDebug.dumpTheme(rootView, b);
            byte[] data = b.toByteArray();
            return new Chunk(CHUNK_VURT, data, 0, data.length);
        } catch (IOException e) {
            return createFailChunk(1, "Unexpected error while dumping the theme: " + e.getMessage());
        }
    }

    private Chunk captureView(View rootView, View targetView) {
        ByteArrayOutputStream b = new ByteArrayOutputStream(1024);
        try {
            ViewDebug.capture(rootView, b, targetView);
            byte[] data = b.toByteArray();
            return new Chunk(CHUNK_VUOP, data, 0, data.length);
        } catch (IOException e) {
            return createFailChunk(1, "Unexpected error while capturing view: " + e.getMessage());
        }
    }

    private Chunk dumpDisplayLists(final View rootView, final View targetView) {
        rootView.post(new Runnable() { // from class: android.ddm.DdmHandleViewDebug.1
            @Override // java.lang.Runnable
            public void run() {
                ViewDebug.outputDisplayList(rootView, targetView);
            }
        });
        return null;
    }

    private Chunk invokeViewMethod(View rootView, View targetView, ByteBuffer in) {
        Class<?>[] argTypes;
        Object[] args;
        int l = in.getInt();
        String methodName = getString(in, l);
        if (!in.hasRemaining()) {
            argTypes = new Class[0];
            args = new Object[0];
        } else {
            int nArgs = in.getInt();
            Class<?>[] argTypes2 = new Class[nArgs];
            Object[] args2 = new Object[nArgs];
            for (int i = 0; i < nArgs; i++) {
                char c = in.getChar();
                switch (c) {
                    case 'B':
                        argTypes2[i] = Byte.TYPE;
                        args2[i] = Byte.valueOf(in.get());
                        break;
                    case 'C':
                        argTypes2[i] = Character.TYPE;
                        args2[i] = Character.valueOf(in.getChar());
                        break;
                    case 'D':
                        argTypes2[i] = Double.TYPE;
                        args2[i] = Double.valueOf(in.getDouble());
                        break;
                    case 'F':
                        argTypes2[i] = Float.TYPE;
                        args2[i] = Float.valueOf(in.getFloat());
                        break;
                    case 'I':
                        argTypes2[i] = Integer.TYPE;
                        args2[i] = Integer.valueOf(in.getInt());
                        break;
                    case 'J':
                        argTypes2[i] = Long.TYPE;
                        args2[i] = Long.valueOf(in.getLong());
                        break;
                    case 'S':
                        argTypes2[i] = Short.TYPE;
                        args2[i] = Short.valueOf(in.getShort());
                        break;
                    case 'Z':
                        argTypes2[i] = Boolean.TYPE;
                        args2[i] = Boolean.valueOf(in.get() != 0);
                        break;
                    default:
                        Log.e(TAG, "arg " + i + ", unrecognized type: " + c);
                        return createFailChunk(-2, "Unsupported parameter type (" + c + ") to invoke view method.");
                }
            }
            argTypes = argTypes2;
            args = args2;
        }
        try {
            Method method = targetView.getClass().getMethod(methodName, argTypes);
            try {
                ViewDebug.invokeViewMethod(targetView, method, args);
                return null;
            } catch (Exception e) {
                Log.e(TAG, "Exception while invoking method: " + e.getCause().getMessage());
                String msg = e.getCause().getMessage();
                if (msg == null) {
                    msg = e.getCause().toString();
                }
                return createFailChunk(-3, msg);
            }
        } catch (NoSuchMethodException e2) {
            Log.e(TAG, "No such method: " + e2.getMessage());
            return createFailChunk(-2, "No such method: " + e2.getMessage());
        }
    }

    private Chunk setLayoutParameter(View rootView, View targetView, ByteBuffer in) {
        int l = in.getInt();
        String param = getString(in, l);
        int value = in.getInt();
        try {
            ViewDebug.setLayoutParameter(targetView, param, value);
            return null;
        } catch (Exception e) {
            Log.e(TAG, "Exception setting layout parameter: " + e);
            return createFailChunk(-3, "Error accessing field " + param + ":" + e.getMessage());
        }
    }

    private Chunk profileView(View rootView, View targetView) {
        ByteArrayOutputStream b = new ByteArrayOutputStream(32768);
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(b), 32768);
        try {
            try {
                ViewDebug.profileViewAndChildren(targetView, bw);
                try {
                    bw.close();
                } catch (IOException e) {
                }
                byte[] data = b.toByteArray();
                return new Chunk(CHUNK_VUOP, data, 0, data.length);
            } catch (Throwable th) {
                try {
                    bw.close();
                } catch (IOException e2) {
                }
                throw th;
            }
        } catch (IOException e3) {
            Chunk createFailChunk = createFailChunk(1, "Unexpected error while profiling view: " + e3.getMessage());
            try {
                bw.close();
            } catch (IOException e4) {
            }
            return createFailChunk;
        }
    }
}
