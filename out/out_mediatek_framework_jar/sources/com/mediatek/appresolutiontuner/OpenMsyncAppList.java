package com.mediatek.appresolutiontuner;

import android.util.Slog;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
/* loaded from: classes.dex */
public class OpenMsyncAppList {
    private static final String APP_LIST_PATH = "system/etc/open_msync_app_list.xml";
    private static final String NODE_PACKAGE_NAME = "packagename";
    private static final String TAG = "OpenMsyncAppList";
    private static final String TAG_APP = "app";
    private static OpenMsyncAppList sInstance;
    private boolean isRead = false;
    private ArrayList<Applic> mMsyncAppCache;

    public static OpenMsyncAppList getInstance() {
        if (sInstance == null) {
            sInstance = new OpenMsyncAppList();
        }
        return sInstance;
    }

    public boolean isRead() {
        return this.isRead;
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [86=4] */
    public void loadOpenMsyncAppList() {
        File target;
        Slog.d(TAG, "loadTunerAppList + ");
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
            this.mMsyncAppCache = parseAppListFile(inputStream2);
            this.isRead = true;
            inputStream2.close();
            Slog.d(TAG, "loadTunerAppList - ");
            return;
        }
        Slog.e(TAG, "Target file doesn't exist: system/etc/open_msync_app_list.xml");
        if (0 != 0) {
            try {
                inputStream.close();
            } catch (IOException e4) {
                Slog.w(TAG, "close failed..", e4);
            }
        }
    }

    public boolean isScaledBySurfaceView(String packageName) {
        ArrayList<Applic> arrayList = this.mMsyncAppCache;
        if (arrayList != null) {
            Iterator<Applic> it = arrayList.iterator();
            while (it.hasNext()) {
                Applic app = it.next();
                if (app.getPackageName().equals(packageName)) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class Applic {
        private String packageName;

        Applic() {
        }

        public String getPackageName() {
            return this.packageName;
        }

        public void setPackageName(String packageName) {
            this.packageName = packageName;
        }
    }

    private ArrayList<Applic> parseAppListFile(InputStream is) {
        ArrayList<Applic> list = new ArrayList<>();
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(is);
            NodeList appList = document.getElementsByTagName(TAG_APP);
            for (int i = 0; i < appList.getLength(); i++) {
                Node node_applic = appList.item(i);
                NodeList childNodes = node_applic.getChildNodes();
                Applic applic = new Applic();
                for (int j = 0; j < childNodes.getLength(); j++) {
                    Node childNode = childNodes.item(j);
                    if (childNode.getNodeName().equals(NODE_PACKAGE_NAME)) {
                        String packageName = childNode.getTextContent();
                        applic.setPackageName(packageName);
                    }
                }
                list.add(applic);
                Slog.d(TAG, "dom2xml: " + applic);
            }
            return list;
        } catch (IOException e) {
            Slog.w(TAG, "IOException", e);
            return list;
        } catch (ParserConfigurationException e2) {
            Slog.w(TAG, "dom2xml ParserConfigurationException", e2);
            return list;
        } catch (SAXException e3) {
            Slog.w(TAG, "dom2xml SAXException", e3);
            return list;
        }
    }
}
