package com.mediatek.wmsmsync;

import android.util.Log;
import android.util.Slog;
import com.mediatek.wmsmsync.MSyncCtrlBean;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
/* loaded from: classes.dex */
public class MSyncCtrlTable {
    private static final String APP_LIST_PATH = "system/etc/msync_ctrl_table.xml";
    private static final String ARRAY_ACTIVITY = "activities";
    private static final Object LOCK = new Object();
    private static final String NODE_ACTIVITY = "activity";
    private static final String NODE_ACTIVITY_FPS = "fps";
    private static final String NODE_ACTIVITY_NAME = "name";
    private static final String NODE_DEFAULT_FPS = "defaultfps";
    private static final String NODE_GLOBAL_FPS = "globalfps";
    private static final String NODE_IME_DEFAULT_FPS = "defaultimefps";
    private static final String NODE_IME_FPS = "imefps";
    private static final String NODE_IME_GLOBAL_CONTROL = "enableimeglobalcontrol";
    private static final String NODE_PACKAGE_NAME = "packagename";
    private static final String NODE_SLIDE_RESPONSE = "slideresponse";
    private static final String NODE_VOICE_DEFAULT_FPS = "defaultvoicefps";
    private static final String NODE_VOICE_FPS = "voicefps";
    private static final String NODE_VOICE_GLOBAL_CONTROL = "enablevoiceglobalcontrol";
    private static final String TAG = "MSyncCtrlTable";
    private static final String TAG_APP = "app";
    private static volatile MSyncCtrlTable sInstance;
    private float mDefaultImeFps;
    private float mDefaultVoiceFps;
    private float mGlobalFPS;
    private ArrayList<MSyncCtrlBean> mMSyncAppCache;
    private boolean mEnableImeGlobalFpsControl = false;
    private boolean mEnableVoiceGlobalFpsControl = false;
    private boolean mIsRead = false;

    public ArrayList<MSyncCtrlBean> getMSyncAppCache() {
        return this.mMSyncAppCache;
    }

    public static MSyncCtrlTable getInstance() {
        if (sInstance == null) {
            synchronized (LOCK) {
                if (sInstance == null) {
                    sInstance = new MSyncCtrlTable();
                }
            }
        }
        return sInstance;
    }

    private MSyncCtrlTable() {
    }

    public boolean isRead() {
        return this.mIsRead;
    }

    public boolean isEnableImeGlobalFpsControl() {
        return this.mEnableImeGlobalFpsControl;
    }

    public boolean isEnableVoiceGlobalFpsControl() {
        return this.mEnableVoiceGlobalFpsControl;
    }

    public float getDefaultImeFps() {
        return this.mDefaultImeFps;
    }

    public float getDefaultVoiceFps() {
        return this.mDefaultVoiceFps;
    }

    public float getGlobalFPS() {
        return this.mGlobalFPS;
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [159=4] */
    public void loadMSyncCtrlTable() {
        File target;
        Slog.d(TAG, "loadMSyncCtrlTable + ");
        InputStream inputStream = null;
        try {
            try {
                try {
                    target = new File(APP_LIST_PATH);
                } catch (IOException e) {
                    Slog.w(TAG, "IOException", e);
                    if (0 != 0) {
                        inputStream.close();
                    }
                }
            } catch (Throwable th) {
                if (0 != 0) {
                    try {
                        inputStream.close();
                    } catch (IOException e2) {
                        Slog.w(TAG, "close failed..", e2);
                    }
                }
                throw th;
            }
        } catch (IOException e3) {
            Slog.w(TAG, "close failed..", e3);
        }
        if (target.exists()) {
            InputStream inputStream2 = new FileInputStream(target);
            this.mMSyncAppCache = parseAppListFile(inputStream2);
            this.mIsRead = true;
            inputStream2.close();
            Slog.d(TAG, "loadMSyncCtrlTable - ");
            return;
        }
        Slog.e(TAG, "Target file doesn't exist: system/etc/msync_ctrl_table.xml");
        if (0 != 0) {
            try {
                inputStream.close();
            } catch (IOException e4) {
                Slog.w(TAG, "close failed..", e4);
            }
        }
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    private ArrayList<MSyncCtrlBean> parseAppListFile(InputStream is) {
        char c;
        NodeList appList;
        Document document;
        Node nodeApps;
        NodeList childNodes;
        Node nodeApps2;
        NodeList childNodes2;
        NodeList childNodes3;
        char c2;
        ArrayList<MSyncCtrlBean> list = new ArrayList<>();
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document2 = builder.parse(is);
            try {
                this.mEnableImeGlobalFpsControl = Boolean.parseBoolean(document2.getElementsByTagName(NODE_IME_GLOBAL_CONTROL).item(0).getTextContent());
                this.mEnableVoiceGlobalFpsControl = Boolean.parseBoolean(document2.getElementsByTagName(NODE_VOICE_GLOBAL_CONTROL).item(0).getTextContent());
                if (this.mEnableImeGlobalFpsControl) {
                    try {
                        this.mDefaultImeFps = Float.parseFloat(document2.getElementsByTagName(NODE_IME_DEFAULT_FPS).item(0).getTextContent());
                    } catch (Exception e) {
                        e = e;
                        e.printStackTrace();
                        return new ArrayList<>();
                    }
                }
                if (this.mEnableVoiceGlobalFpsControl) {
                    this.mDefaultVoiceFps = Float.parseFloat(document2.getElementsByTagName(NODE_VOICE_DEFAULT_FPS).item(0).getTextContent());
                }
                this.mGlobalFPS = Float.parseFloat(document2.getElementsByTagName(NODE_GLOBAL_FPS).item(0).getTextContent());
                Slog.d(TAG, "mEnableIMEGlobalFPSControl = " + this.mEnableImeGlobalFpsControl + " | mDefaultIMEFPS = " + this.mDefaultImeFps + "\nmEnableVoiceGlobalFPSControl = " + this.mEnableVoiceGlobalFpsControl + " | mDefaultVoiceFPS = " + this.mDefaultVoiceFps + "\nmGlobalFPS = " + this.mGlobalFPS);
                NodeList appList2 = document2.getElementsByTagName(TAG_APP);
                int i = 0;
                while (i < appList2.getLength()) {
                    Node nodeApps3 = appList2.item(i);
                    NodeList childNodes4 = nodeApps3.getChildNodes();
                    MSyncCtrlBean MSyncCtrlBean = new MSyncCtrlBean();
                    int j = 0;
                    while (j < childNodes4.getLength()) {
                        Node childNode = childNodes4.item(j);
                        String nodeName = childNode.getNodeName();
                        switch (nodeName.hashCode()) {
                            case -1972517257:
                                if (nodeName.equals(NODE_VOICE_FPS)) {
                                    c = 4;
                                    break;
                                }
                                c = 65535;
                                break;
                            case -1185132152:
                                if (nodeName.equals(NODE_IME_FPS)) {
                                    c = 3;
                                    break;
                                }
                                c = 65535;
                                break;
                            case 678658824:
                                if (nodeName.equals(NODE_DEFAULT_FPS)) {
                                    c = 2;
                                    break;
                                }
                                c = 65535;
                                break;
                            case 909712337:
                                if (nodeName.equals(NODE_PACKAGE_NAME)) {
                                    c = 0;
                                    break;
                                }
                                c = 65535;
                                break;
                            case 1291981042:
                                if (nodeName.equals(NODE_SLIDE_RESPONSE)) {
                                    c = 1;
                                    break;
                                }
                                c = 65535;
                                break;
                            case 2048605165:
                                if (nodeName.equals(ARRAY_ACTIVITY)) {
                                    c = 5;
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
                                appList = appList2;
                                document = document2;
                                nodeApps = nodeApps3;
                                childNodes = childNodes4;
                                String packageName = childNode.getTextContent();
                                MSyncCtrlBean.setPackageName(packageName);
                                continue;
                            case 1:
                                appList = appList2;
                                document = document2;
                                nodeApps = nodeApps3;
                                childNodes = childNodes4;
                                String slideResponse = childNode.getTextContent();
                                MSyncCtrlBean.setSlideResponse(Boolean.parseBoolean(slideResponse));
                                continue;
                            case 2:
                                appList = appList2;
                                document = document2;
                                nodeApps = nodeApps3;
                                childNodes = childNodes4;
                                String defaultFps = childNode.getTextContent();
                                MSyncCtrlBean.setFps(Float.parseFloat(defaultFps));
                                continue;
                            case 3:
                                appList = appList2;
                                document = document2;
                                nodeApps = nodeApps3;
                                childNodes = childNodes4;
                                String imeFps = childNode.getTextContent();
                                MSyncCtrlBean.setImeFps(Float.parseFloat(imeFps));
                                continue;
                            case 4:
                                appList = appList2;
                                document = document2;
                                nodeApps = nodeApps3;
                                childNodes = childNodes4;
                                String voiceFps = childNode.getTextContent();
                                MSyncCtrlBean.setVoiceFps(Float.parseFloat(voiceFps));
                                continue;
                            case 5:
                                NodeList grandSunNodes = childNode.getChildNodes();
                                List<MSyncCtrlBean.ActivityBean> activityBeanList = new ArrayList<>();
                                appList = appList2;
                                int k = 0;
                                while (k < grandSunNodes.getLength()) {
                                    Node grandSunNode = grandSunNodes.item(k);
                                    Document document3 = document2;
                                    try {
                                        if ("activity".equals(grandSunNode.getNodeName())) {
                                            NodeList grandGrandSunNodes = grandSunNode.getChildNodes();
                                            MSyncCtrlBean.ActivityBean activityBean = new MSyncCtrlBean.ActivityBean();
                                            int l = 0;
                                            while (true) {
                                                nodeApps2 = nodeApps3;
                                                if (l < grandGrandSunNodes.getLength()) {
                                                    Node grandGrandSunNode = grandGrandSunNodes.item(l);
                                                    NodeList grandGrandSunNodes2 = grandGrandSunNodes;
                                                    String nodeName2 = grandGrandSunNode.getNodeName();
                                                    switch (nodeName2.hashCode()) {
                                                        case -1972517257:
                                                            childNodes3 = childNodes4;
                                                            if (nodeName2.equals(NODE_VOICE_FPS)) {
                                                                c2 = 3;
                                                                break;
                                                            }
                                                            c2 = 65535;
                                                            break;
                                                        case -1185132152:
                                                            childNodes3 = childNodes4;
                                                            if (nodeName2.equals(NODE_IME_FPS)) {
                                                                c2 = 2;
                                                                break;
                                                            }
                                                            c2 = 65535;
                                                            break;
                                                        case 101609:
                                                            childNodes3 = childNodes4;
                                                            if (nodeName2.equals(NODE_ACTIVITY_FPS)) {
                                                                c2 = 1;
                                                                break;
                                                            }
                                                            c2 = 65535;
                                                            break;
                                                        case 3373707:
                                                            childNodes3 = childNodes4;
                                                            if (nodeName2.equals(NODE_ACTIVITY_NAME)) {
                                                                c2 = 0;
                                                                break;
                                                            }
                                                            c2 = 65535;
                                                            break;
                                                        default:
                                                            childNodes3 = childNodes4;
                                                            c2 = 65535;
                                                            break;
                                                    }
                                                    switch (c2) {
                                                        case 0:
                                                            String activityName = grandGrandSunNode.getTextContent();
                                                            activityBean.setName(activityName);
                                                            break;
                                                        case 1:
                                                            String activityFps = grandGrandSunNode.getTextContent();
                                                            activityBean.setFps(Float.parseFloat(activityFps));
                                                            break;
                                                        case 2:
                                                            String atyImeFps = grandGrandSunNode.getTextContent();
                                                            activityBean.setImeFps(Float.parseFloat(atyImeFps));
                                                            break;
                                                        case 3:
                                                            String atyVoiceFps = grandGrandSunNode.getTextContent();
                                                            activityBean.setVoiceFps(Float.parseFloat(atyVoiceFps));
                                                            break;
                                                    }
                                                    l++;
                                                    nodeApps3 = nodeApps2;
                                                    grandGrandSunNodes = grandGrandSunNodes2;
                                                    childNodes4 = childNodes3;
                                                } else {
                                                    childNodes2 = childNodes4;
                                                    activityBeanList.add(activityBean);
                                                }
                                            }
                                        } else {
                                            nodeApps2 = nodeApps3;
                                            childNodes2 = childNodes4;
                                        }
                                        k++;
                                        document2 = document3;
                                        nodeApps3 = nodeApps2;
                                        childNodes4 = childNodes2;
                                    } catch (Exception e2) {
                                        e = e2;
                                        e.printStackTrace();
                                        return new ArrayList<>();
                                    }
                                }
                                document = document2;
                                nodeApps = nodeApps3;
                                childNodes = childNodes4;
                                MSyncCtrlBean.setActivityBeans(activityBeanList);
                                continue;
                            default:
                                appList = appList2;
                                document = document2;
                                nodeApps = nodeApps3;
                                childNodes = childNodes4;
                                continue;
                        }
                        j++;
                        document2 = document;
                        appList2 = appList;
                        nodeApps3 = nodeApps;
                        childNodes4 = childNodes;
                    }
                    NodeList appList3 = appList2;
                    Document document4 = document2;
                    list.add(MSyncCtrlBean);
                    Log.d(TAG, "MSyncCtrlTableBean dom2xml: " + MSyncCtrlBean);
                    i++;
                    document2 = document4;
                    appList2 = appList3;
                }
                return list;
            } catch (Exception e3) {
                e = e3;
            }
        } catch (IOException e4) {
            Log.w(TAG, "IOException", e4);
            return list;
        } catch (ParserConfigurationException e5) {
            Log.w(TAG, "dom2xml ParserConfigurationException", e5);
            return list;
        } catch (SAXException e6) {
            Log.w(TAG, "dom2xml SAXException", e6);
            return list;
        }
    }
}
