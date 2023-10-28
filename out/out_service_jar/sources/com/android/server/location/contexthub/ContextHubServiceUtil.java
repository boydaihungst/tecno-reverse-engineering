package com.android.server.location.contexthub;

import android.content.Context;
import android.hardware.contexthub.ContextHubMessage;
import android.hardware.contexthub.NanoappBinary;
import android.hardware.contexthub.NanoappInfo;
import android.hardware.contexthub.NanoappRpcService;
import android.hardware.contexthub.V1_0.ContextHubMsg;
import android.hardware.contexthub.V1_0.NanoAppBinary;
import android.hardware.contexthub.V1_2.HubAppInfo;
import android.hardware.location.ContextHubInfo;
import android.hardware.location.NanoAppMessage;
import android.hardware.location.NanoAppRpcService;
import android.hardware.location.NanoAppState;
import android.util.Log;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
/* loaded from: classes.dex */
class ContextHubServiceUtil {
    private static final String CONTEXT_HUB_PERMISSION = "android.permission.ACCESS_CONTEXT_HUB";
    private static final char HOST_ENDPOINT_BROADCAST = 65535;
    private static final String TAG = "ContextHubServiceUtil";

    ContextHubServiceUtil() {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static HashMap<Integer, ContextHubInfo> createContextHubInfoMap(List<ContextHubInfo> hubList) {
        HashMap<Integer, ContextHubInfo> contextHubIdToInfoMap = new HashMap<>();
        for (ContextHubInfo contextHubInfo : hubList) {
            contextHubIdToInfoMap.put(Integer.valueOf(contextHubInfo.getId()), contextHubInfo);
        }
        return contextHubIdToInfoMap;
    }

    static void copyToByteArrayList(byte[] inputArray, ArrayList<Byte> outputArray) {
        outputArray.clear();
        outputArray.ensureCapacity(inputArray.length);
        for (byte element : inputArray) {
            outputArray.add(Byte.valueOf(element));
        }
    }

    static byte[] createPrimitiveByteArray(ArrayList<Byte> array) {
        byte[] primitiveArray = new byte[array.size()];
        for (int i = 0; i < array.size(); i++) {
            primitiveArray[i] = array.get(i).byteValue();
        }
        return primitiveArray;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static int[] createPrimitiveIntArray(Collection<Integer> collection) {
        int[] primitiveArray = new int[collection.size()];
        int i = 0;
        for (Integer num : collection) {
            int contextHubId = num.intValue();
            primitiveArray[i] = contextHubId;
            i++;
        }
        return primitiveArray;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static NanoAppBinary createHidlNanoAppBinary(android.hardware.location.NanoAppBinary nanoAppBinary) {
        NanoAppBinary hidlNanoAppBinary = new NanoAppBinary();
        hidlNanoAppBinary.appId = nanoAppBinary.getNanoAppId();
        hidlNanoAppBinary.appVersion = nanoAppBinary.getNanoAppVersion();
        hidlNanoAppBinary.flags = nanoAppBinary.getFlags();
        hidlNanoAppBinary.targetChreApiMajorVersion = nanoAppBinary.getTargetChreApiMajorVersion();
        hidlNanoAppBinary.targetChreApiMinorVersion = nanoAppBinary.getTargetChreApiMinorVersion();
        try {
            copyToByteArrayList(nanoAppBinary.getBinaryNoHeader(), hidlNanoAppBinary.customBinary);
        } catch (IndexOutOfBoundsException e) {
            Log.w(TAG, e.getMessage());
        } catch (NullPointerException e2) {
            Log.w(TAG, "NanoApp binary was null");
        }
        return hidlNanoAppBinary;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static NanoappBinary createAidlNanoAppBinary(android.hardware.location.NanoAppBinary nanoAppBinary) {
        NanoappBinary aidlNanoAppBinary = new NanoappBinary();
        aidlNanoAppBinary.nanoappId = nanoAppBinary.getNanoAppId();
        aidlNanoAppBinary.nanoappVersion = nanoAppBinary.getNanoAppVersion();
        aidlNanoAppBinary.flags = nanoAppBinary.getFlags();
        aidlNanoAppBinary.targetChreApiMajorVersion = nanoAppBinary.getTargetChreApiMajorVersion();
        aidlNanoAppBinary.targetChreApiMinorVersion = nanoAppBinary.getTargetChreApiMinorVersion();
        aidlNanoAppBinary.customBinary = new byte[0];
        try {
            aidlNanoAppBinary.customBinary = nanoAppBinary.getBinaryNoHeader();
        } catch (IndexOutOfBoundsException e) {
            Log.w(TAG, e.getMessage());
        } catch (NullPointerException e2) {
            Log.w(TAG, "NanoApp binary was null");
        }
        return aidlNanoAppBinary;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static List<NanoAppState> createNanoAppStateList(List<HubAppInfo> nanoAppInfoList) {
        ArrayList<NanoAppState> nanoAppStateList = new ArrayList<>();
        for (HubAppInfo appInfo : nanoAppInfoList) {
            nanoAppStateList.add(new NanoAppState(appInfo.info_1_0.appId, appInfo.info_1_0.version, appInfo.info_1_0.enabled, appInfo.permissions));
        }
        return nanoAppStateList;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static List<NanoAppState> createNanoAppStateList(NanoappInfo[] nanoAppInfoList) {
        NanoappRpcService[] nanoappRpcServiceArr;
        ArrayList<NanoAppState> nanoAppStateList = new ArrayList<>();
        for (NanoappInfo appInfo : nanoAppInfoList) {
            ArrayList<NanoAppRpcService> rpcServiceList = new ArrayList<>();
            for (NanoappRpcService service : appInfo.rpcServices) {
                rpcServiceList.add(new NanoAppRpcService(service.id, service.version));
            }
            nanoAppStateList.add(new NanoAppState(appInfo.nanoappId, appInfo.nanoappVersion, appInfo.enabled, new ArrayList(Arrays.asList(appInfo.permissions)), rpcServiceList));
        }
        return nanoAppStateList;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static ContextHubMsg createHidlContextHubMessage(short hostEndPoint, NanoAppMessage message) {
        ContextHubMsg hidlMessage = new ContextHubMsg();
        hidlMessage.appName = message.getNanoAppId();
        hidlMessage.hostEndPoint = hostEndPoint;
        hidlMessage.msgType = message.getMessageType();
        copyToByteArrayList(message.getMessageBody(), hidlMessage.msg);
        return hidlMessage;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static ContextHubMessage createAidlContextHubMessage(short hostEndPoint, NanoAppMessage message) {
        ContextHubMessage aidlMessage = new ContextHubMessage();
        aidlMessage.nanoappId = message.getNanoAppId();
        aidlMessage.hostEndPoint = (char) hostEndPoint;
        aidlMessage.messageType = message.getMessageType();
        aidlMessage.messageBody = message.getMessageBody();
        aidlMessage.permissions = new String[0];
        return aidlMessage;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static NanoAppMessage createNanoAppMessage(ContextHubMsg message) {
        byte[] messageArray = createPrimitiveByteArray(message.msg);
        return NanoAppMessage.createMessageFromNanoApp(message.appName, message.msgType, messageArray, message.hostEndPoint == -1);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static NanoAppMessage createNanoAppMessage(ContextHubMessage message) {
        return NanoAppMessage.createMessageFromNanoApp(message.nanoappId, message.messageType, message.messageBody, message.hostEndPoint == 65535);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void checkPermissions(Context context) {
        context.enforceCallingOrSelfPermission(CONTEXT_HUB_PERMISSION, "ACCESS_CONTEXT_HUB permission required to use Context Hub");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static int toTransactionResult(int halResult) {
        switch (halResult) {
            case 0:
                return 0;
            case 1:
            case 4:
            default:
                return 1;
            case 2:
                return 2;
            case 3:
                return 3;
            case 5:
                return 4;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static ArrayList<HubAppInfo> toHubAppInfo_1_2(ArrayList<android.hardware.contexthub.V1_0.HubAppInfo> oldInfoList) {
        ArrayList newAppInfo = new ArrayList();
        Iterator<android.hardware.contexthub.V1_0.HubAppInfo> it = oldInfoList.iterator();
        while (it.hasNext()) {
            android.hardware.contexthub.V1_0.HubAppInfo oldInfo = it.next();
            HubAppInfo newInfo = new HubAppInfo();
            newInfo.info_1_0.appId = oldInfo.appId;
            newInfo.info_1_0.version = oldInfo.version;
            newInfo.info_1_0.memUsage = oldInfo.memUsage;
            newInfo.info_1_0.enabled = oldInfo.enabled;
            newInfo.permissions = new ArrayList();
            newAppInfo.add(newInfo);
        }
        return newAppInfo;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static int toContextHubEvent(int hidlEventType) {
        switch (hidlEventType) {
            case 1:
                return 1;
            default:
                Log.e(TAG, "toContextHubEvent: Unknown event type: " + hidlEventType);
                return 0;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static int toContextHubEventFromAidl(int aidlEventType) {
        switch (aidlEventType) {
            case 1:
                return 1;
            default:
                Log.e(TAG, "toContextHubEventFromAidl: Unknown event type: " + aidlEventType);
                return 0;
        }
    }
}
