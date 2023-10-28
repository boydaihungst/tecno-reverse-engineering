package android.net;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.INetconManager;
import android.net.NetconManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import com.android.internal.util.Preconditions;
import java.net.MpHttp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
/* loaded from: classes.dex */
public class NetconManagerImpl extends NetconManager {
    private static final int ALREADY_UNREGISTERED = -1;
    private static final String TAG = "NetconManagerImpl";
    private static CallbackHandler sCallbackHandler;
    private volatile boolean isMpHttpRegistered;
    private ServiceConnection mConn;
    private final Context mContext;
    private MpHttp.MpHttpCallback mMpHttpCallback;
    private volatile INetconManager mService;
    private static final HashMap<Integer, NetconManager.NetconCallback> sCallbacks = new HashMap<>();
    private static final HashMap<Integer, NetconManager.MpHttpCallback> mMpHttpCallbacks = new HashMap<>();

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class CallbackHandler extends Handler {
        private static final boolean DBG = false;
        private static final String TAG = "NetconManager.CallbackHandler";

        CallbackHandler(Looper looper) {
            super(looper);
        }

        CallbackHandler(Handler handler) {
            this(((Handler) Preconditions.checkNotNull(handler, "Handler cannot be null.")).getLooper());
        }

        @Override // android.os.Handler
        public void handleMessage(Message message) {
            Log.i(TAG, "handleMessage " + NetconManager.getCallbackName(message.what));
            int requestId = message.getData().getInt("requestId");
            Log.i(TAG, "handleMessage:the requestId from Netconservice is :" + requestId);
            NetconManager.NetconCallback callback = null;
            NetconManager.MpHttpCallback mpHttpCallback = null;
            int msgWhat = message.what;
            if (msgWhat == 1 || msgWhat == 3 || msgWhat == 2) {
                synchronized (NetconManagerImpl.sCallbacks) {
                    callback = (NetconManager.NetconCallback) NetconManagerImpl.sCallbacks.get(Integer.valueOf(requestId));
                }
                if (callback == null) {
                    Log.w(TAG, "callback not found for " + NetconManager.getCallbackName(message.what) + " message");
                    return;
                }
            } else if (msgWhat == 4 || msgWhat == 5) {
                synchronized (NetconManagerImpl.mMpHttpCallbacks) {
                    mpHttpCallback = (NetconManager.MpHttpCallback) NetconManagerImpl.mMpHttpCallbacks.get(Integer.valueOf(requestId));
                }
                if (mpHttpCallback == null) {
                    Log.w(TAG, "mpHttpCallback not found for " + NetconManager.getCallbackName(message.what) + " message");
                    return;
                }
            }
            switch (msgWhat) {
                case 1:
                    if (callback != null) {
                        callback.onBindProcessToNetwork((Network) message.obj);
                        return;
                    }
                    return;
                case 2:
                    if (callback != null) {
                        callback.onClearBindingRequest();
                        return;
                    }
                    return;
                case 3:
                    if (callback != null) {
                        callback.onBindProcessToNetwork((Network) message.obj);
                        return;
                    }
                    return;
                case 4:
                    if (mpHttpCallback != null) {
                        mpHttpCallback.notifyNetworkAvailable(message.getData().getString("InterfaceName"));
                        return;
                    }
                    return;
                case 5:
                    if (mpHttpCallback != null) {
                        mpHttpCallback.notifyNetworkLost(message.getData().getString("InterfaceName"));
                        return;
                    }
                    return;
                default:
                    return;
            }
        }
    }

    public NetconManagerImpl(Context context) {
        this.mConn = null;
        this.isMpHttpRegistered = false;
        this.mMpHttpCallback = null;
        Log.i(TAG, "NetconManagerImpl one args");
        this.mContext = context;
        this.mConn = new ServiceConnection() { // from class: android.net.NetconManagerImpl.1
            @Override // android.content.ServiceConnection
            public void onServiceDisconnected(ComponentName name) {
                Log.i(NetconManagerImpl.TAG, "Netcon service is disconnected already");
                NetconManagerImpl.this.mService = null;
            }

            @Override // android.content.ServiceConnection
            public void onServiceConnected(ComponentName name, IBinder service) {
                Log.d(NetconManagerImpl.TAG, "Netcon service is connected already, bind service name: " + name.getClassName());
                NetconManagerImpl.this.mService = INetconManager.Stub.asInterface(service);
                if (NetconManagerImpl.this.mMpHttpCallback != null && !NetconManagerImpl.this.isMpHttpRegistered) {
                    Log.i(NetconManagerImpl.TAG, "start register to MPHTTP now");
                    MpHttp.registerMpHttpCallback(NetconManagerImpl.this.mMpHttpCallback);
                    NetconManagerImpl.this.isMpHttpRegistered = true;
                }
            }
        };
        Intent serviceIntent = new Intent();
        serviceIntent.setAction("com.tran.netcon.ACTION_AIDL_SERVICE");
        serviceIntent.setPackage("com.tran.netcon");
        Log.i(TAG, "bindService netcon service --com.tran.netcon");
        context.bindService(serviceIntent, this.mConn, 1);
    }

    public NetconManagerImpl(Context context, MpHttp.MpHttpCallback mpHttpCallback) {
        this.mConn = null;
        this.isMpHttpRegistered = false;
        this.mMpHttpCallback = null;
        Log.i(TAG, "NetconManagerImpl two args");
        this.mContext = context;
        this.mConn = new ServiceConnection() { // from class: android.net.NetconManagerImpl.2
            @Override // android.content.ServiceConnection
            public void onServiceDisconnected(ComponentName name) {
                Log.i(NetconManagerImpl.TAG, "Netcon service is disconnected already");
                NetconManagerImpl.this.mService = null;
            }

            @Override // android.content.ServiceConnection
            public void onServiceConnected(ComponentName name, IBinder service) {
                Log.d(NetconManagerImpl.TAG, "Netcon service is connected already, bind service name: " + name.getClassName());
                NetconManagerImpl.this.mService = INetconManager.Stub.asInterface(service);
                if (NetconManagerImpl.this.mMpHttpCallback != null && !NetconManagerImpl.this.isMpHttpRegistered) {
                    Log.i(NetconManagerImpl.TAG, "start register to MPHTTP now");
                    MpHttp.registerMpHttpCallback(NetconManagerImpl.this.mMpHttpCallback);
                    NetconManagerImpl.this.isMpHttpRegistered = true;
                }
            }
        };
        this.mMpHttpCallback = mpHttpCallback;
        Intent serviceIntent = new Intent();
        serviceIntent.setAction("com.tran.netcon.ACTION_AIDL_SERVICE");
        serviceIntent.setPackage("com.tran.netcon");
        Log.i(TAG, "bindService Netcon service --com.tran.netcon");
        context.bindService(serviceIntent, this.mConn, 1);
    }

    public void destroy() {
        if (this.mConn != null && this.mContext != null) {
            Log.i(TAG, "unbind to SmartLinkService now");
            this.mContext.unbindService(this.mConn);
        }
    }

    private CallbackHandler getDefaultHandler() {
        CallbackHandler callbackHandler;
        synchronized (sCallbacks) {
            if (sCallbackHandler == null) {
                sCallbackHandler = new CallbackHandler(NetconThread.getInstanceLooper());
            }
            callbackHandler = sCallbackHandler;
        }
        return callbackHandler;
    }

    private int sendRequestForNetwork(NetconManager.NetconCallback callback, CallbackHandler handler, String packageName) {
        Log.i(TAG, "sendRequestForNetwork ");
        checkCallbackNotNull(callback);
        int requestId = 0;
        try {
            HashMap<Integer, NetconManager.NetconCallback> hashMap = sCallbacks;
            synchronized (hashMap) {
                if (callback != null) {
                    Log.i(TAG, "sendRequestForNetwork callback.networkRequestId  = " + callback.networkRequestId);
                    if (callback.networkRequestId > 0) {
                        Log.e(TAG, "NetconCallback was already registered");
                    }
                }
                Messenger messenger = new Messenger(handler);
                Binder binder = new Binder();
                if (this.mService != null) {
                    requestId = this.mService.registerNetconCallback(messenger, binder, packageName);
                    Log.i(TAG, "mService.registerNetconCallback : requestId  = " + requestId);
                    if (requestId > 0) {
                        hashMap.put(Integer.valueOf(requestId), callback);
                    }
                    callback.networkRequestId = requestId;
                }
            }
            return requestId;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    private static void checkCallbackNotNull(NetconManager.NetconCallback callback) {
        Preconditions.checkNotNull(callback, "null NetworkCallback");
    }

    public void registerMpHttpNetworkCallback(NetconManager.MpHttpCallback callback) {
        Log.i(TAG, "registerMpHttpNetworkCallback ");
        if (this.mService == null) {
            for (int tryNum = 0; tryNum < 3; tryNum++) {
                try {
                    if (this.mService != null) {
                        Log.i(TAG, "tryNum:" + tryNum + ", mService is not null!!!");
                        registerMpHttpNetworkCallback(callback, getDefaultHandler());
                        return;
                    }
                    Thread.sleep(1000L);
                    Log.i(TAG, "after sleep 1 seconds for tryNum:" + tryNum + ", mService is still null!!!");
                } catch (InterruptedException e) {
                    Log.e(TAG, "error occurred:" + e);
                    return;
                }
            }
            return;
        }
        registerMpHttpNetworkCallback(callback, getDefaultHandler());
    }

    public void registerMpHttpNetworkCallback(NetconManager.MpHttpCallback callback, Handler handler) {
        new CallbackHandler(handler);
        try {
            HashMap<Integer, NetconManager.MpHttpCallback> hashMap = mMpHttpCallbacks;
            synchronized (hashMap) {
                if (callback != null) {
                    Log.i(TAG, "registerMpHttpNetworkCallback callback.networkRequestId  = " + callback.networkRequestId);
                    if (callback.networkRequestId > 0) {
                        Log.e(TAG, "MpHttpCallback was already registered");
                    }
                }
                Messenger messenger = new Messenger(handler);
                Binder binder = new Binder();
                if (this.mService != null) {
                    int requestId = this.mService.registerMpHttpNetworkCallback(messenger, binder);
                    Log.i(TAG, "mService.registerMpHttpCallback : requestId  = " + requestId);
                    if (requestId > 0) {
                        hashMap.put(Integer.valueOf(requestId), callback);
                    }
                    callback.networkRequestId = requestId;
                }
            }
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void unregisterMpHttpNetworkCallback(NetconManager.MpHttpCallback callback) {
        if (callback == null) {
            Log.e(TAG, "unregisterMpHttpNetworkCallback MpHttpCallback is null");
            return;
        }
        List<Integer> reqIds = new ArrayList<>();
        HashMap<Integer, NetconManager.MpHttpCallback> hashMap = mMpHttpCallbacks;
        synchronized (hashMap) {
            if (callback.networkRequestId != 0 && callback.networkRequestId != ALREADY_UNREGISTERED) {
                for (Map.Entry<Integer, NetconManager.MpHttpCallback> e : hashMap.entrySet()) {
                    if (e.getValue() == callback) {
                        reqIds.add(e.getKey());
                    }
                }
                for (Integer r : reqIds) {
                    try {
                        this.mService.unregisterMpHttpNetworkCallback(r.intValue());
                        mMpHttpCallbacks.remove(r);
                    } catch (RemoteException e2) {
                        throw e2.rethrowFromSystemServer();
                    }
                }
                callback.networkRequestId = ALREADY_UNREGISTERED;
                return;
            }
            Log.i(TAG, "MpHttpCallback was not registered");
        }
    }

    public Network getNetwork(String interfaceName) {
        try {
            if (this.mService == null) {
                if (this.mService != null) {
                    Log.i(TAG, "getNetwork mService is not null!!!");
                    Network network = this.mService.getNetwork(interfaceName);
                    return network;
                }
                Thread.sleep(1000L);
                Log.i(TAG, "getNetwork after sleep 1 seconds, mService is still null!!!");
                return null;
            }
            Network network2 = this.mService.getNetwork(interfaceName);
            return network2;
        } catch (Exception e) {
            Log.e(TAG, "some error occurred: " + e);
            return null;
        }
    }

    public void registerNetworkCallback(NetconManager.NetconCallback netconCallback, String packageName) {
        Log.i(TAG, "registerNetworkCallback ");
        if (this.mService == null) {
            try {
                Thread.sleep(2000L);
                Log.i(TAG, "after sleep 2 seconds ");
                if (this.mService != null) {
                    Log.i(TAG, "after sleep 2 seconds, mService is not null!!!");
                    registerNetworkCallback(netconCallback, getDefaultHandler(), packageName);
                    return;
                }
                Log.i(TAG, "after sleep 2 seconds, mService is still null!!!");
                return;
            } catch (InterruptedException e) {
                return;
            }
        }
        registerNetworkCallback(netconCallback, getDefaultHandler(), packageName);
    }

    public void registerNetworkCallback(NetconManager.NetconCallback netconCallback, Handler handler, String packageName) {
        CallbackHandler cbHandler = new CallbackHandler(handler);
        sendRequestForNetwork(netconCallback, cbHandler, packageName);
    }

    public void unregisterNetworkCallback(NetconManager.NetconCallback netconCallback) {
        checkCallbackNotNull(netconCallback);
        List<Integer> reqIds = new ArrayList<>();
        HashMap<Integer, NetconManager.NetconCallback> hashMap = sCallbacks;
        synchronized (hashMap) {
            if (netconCallback.networkRequestId != 0 && netconCallback.networkRequestId != ALREADY_UNREGISTERED) {
                for (Map.Entry<Integer, NetconManager.NetconCallback> e : hashMap.entrySet()) {
                    if (e.getValue() == netconCallback) {
                        reqIds.add(e.getKey());
                    }
                }
                for (Integer r : reqIds) {
                    try {
                        if (this.mService != null) {
                            this.mService.unregisterNetconCallback(r.intValue());
                        }
                        sCallbacks.remove(r);
                    } catch (RemoteException e2) {
                        throw e2.rethrowFromSystemServer();
                    }
                }
                netconCallback.networkRequestId = ALREADY_UNREGISTERED;
                return;
            }
            Log.i(TAG, "netconCallback was not registered");
        }
    }

    public void onBindedNetworkFinished(int netId, int pid) {
        if (this.mService != null) {
            try {
                this.mService.onBindedNetworkFinished(netId, pid);
                Log.i(TAG, "mService.onBindedNetworkChanged : netId  = " + netId + " pid = " + pid);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }
}
