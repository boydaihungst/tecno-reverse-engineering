package com.mediatek.appresolutiontuner;

import android.os.FileUtils;
import android.os.SystemProperties;
import android.util.Slog;
import com.mediatek.boostfwk.utils.Config;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
/* loaded from: classes.dex */
public class ResolutionTunerAppList {
    private static final String APP_LIST_PATH = "vendor/etc/resolution_tuner_app_list.xml";
    private static final String APP_LIST_PATH_FOR_AIVRS = "/vendor/etc/aivrs.ini";
    private static final String APP_LIST_PATH_FOR_GAISR = "/vendor/etc/gaisr.ini";
    private static final String APP_LIST_PATH_NEW = "/data/retuner/sf_resolution_tuner_app_list.xml";
    private static final int DYNAMIC_RESOLUTION_TUNING = SystemProperties.getInt("debug.enable.sv_tuner", 1);
    private static final String NODE_FILTERED_WINDOW = "filteredwindow";
    private static final String NODE_PACKAGE_NAME = "packagename";
    private static final String NODE_SCALE = "scale";
    private static final String NODE_SCALING_FLOW = "flow";
    private static final String TAG = "AppResolutionTuner";
    private static final String TAG_APP = "app";
    private static final String VALUE_SCALING_FLOW_GAME = "game";
    private static final String VALUE_SCALING_FLOW_SURFACEVIEW = "surfaceview";
    private static final String VALUE_SCALING_FLOW_WMS = "wms";
    private static ResolutionTunerAppList sInstance;
    private ArrayList<Applic> mTunerAppCache;

    public static ResolutionTunerAppList getInstance() {
        if (sInstance == null) {
            sInstance = new ResolutionTunerAppList();
        }
        return sInstance;
    }

    /* JADX DEBUG: Another duplicated slice has different insns count: {[IF]}, finally: {[IF, INVOKE, INVOKE, INVOKE, INVOKE, INVOKE, INVOKE, INVOKE, MOVE_EXCEPTION, INVOKE, INVOKE, INVOKE, INVOKE, MOVE_EXCEPTION] complete} */
    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [144=7, 145=7, 146=6, 148=6] */
    public ArrayList<Applic> loadTunerAppList() {
        Slog.d(TAG, "loadTunerAppList + ");
        InputStream inputStream = null;
        try {
            try {
            } catch (IOException e) {
                Slog.w(TAG, "IOException", e);
                if (0 != 0) {
                    try {
                        inputStream.close();
                    } catch (IOException e2) {
                        e = e2;
                        Slog.w(TAG, "close failed..", e);
                        Slog.d(TAG, "loadTunerAppList - ");
                        return this.mTunerAppCache;
                    }
                }
            }
            if (!Config.USER_CONFIG_DEFAULT_TYPE.equals(SystemProperties.get("ro.vendor.game_aisr_enable"))) {
                File target = new File(APP_LIST_PATH_NEW);
                if (!target.exists()) {
                    Slog.w(TAG, "Target file doesn't exist: /data/retuner/sf_resolution_tuner_app_list.xml");
                    target = new File(APP_LIST_PATH);
                    if (!target.exists()) {
                        Slog.e(TAG, "Target file doesn't exist: vendor/etc/resolution_tuner_app_list.xml");
                        ArrayList<Applic> arrayList = this.mTunerAppCache;
                        if (0 != 0) {
                            try {
                                inputStream.close();
                            } catch (IOException e3) {
                                Slog.w(TAG, "close failed..", e3);
                            }
                        }
                        Slog.d(TAG, "loadTunerAppList - ");
                        return arrayList;
                    }
                }
                InputStream inputStream2 = new FileInputStream(target);
                this.mTunerAppCache = parseAppListFile(inputStream2);
                try {
                    inputStream2.close();
                } catch (IOException e4) {
                    e = e4;
                    Slog.w(TAG, "close failed..", e);
                    Slog.d(TAG, "loadTunerAppList - ");
                    return this.mTunerAppCache;
                }
                Slog.d(TAG, "loadTunerAppList - ");
                return this.mTunerAppCache;
            }
            File target2 = new File(APP_LIST_PATH_FOR_AIVRS);
            if (target2.exists()) {
                ArrayList<Applic> parseAppListFileForAIVRS = parseAppListFileForAIVRS(target2);
                this.mTunerAppCache = parseAppListFileForAIVRS;
                if (0 != 0) {
                    try {
                        inputStream.close();
                    } catch (IOException e5) {
                        Slog.w(TAG, "close failed..", e5);
                    }
                }
                Slog.d(TAG, "loadTunerAppList - ");
                return parseAppListFileForAIVRS;
            }
            Slog.d(TAG, "Target file doesn't exist: /vendor/etc/aivrs.ini");
            File target3 = new File(APP_LIST_PATH_FOR_GAISR);
            if (target3.exists()) {
                ArrayList<Applic> parseAppListFileForGAISR = parseAppListFileForGAISR(target3);
                this.mTunerAppCache = parseAppListFileForGAISR;
                if (0 != 0) {
                    try {
                        inputStream.close();
                    } catch (IOException e6) {
                        Slog.w(TAG, "close failed..", e6);
                    }
                }
                Slog.d(TAG, "loadTunerAppList - ");
                return parseAppListFileForGAISR;
            }
            Slog.d(TAG, "Target file doesn't exist: /vendor/etc/gaisr.ini");
            ArrayList<Applic> arrayList2 = this.mTunerAppCache;
            if (0 != 0) {
                try {
                    inputStream.close();
                } catch (IOException e7) {
                    Slog.w(TAG, "close failed..", e7);
                }
            }
            Slog.d(TAG, "loadTunerAppList - ");
            return arrayList2;
        } catch (Throwable th) {
            if (0 != 0) {
                try {
                    inputStream.close();
                } catch (IOException e8) {
                    Slog.w(TAG, "close failed..", e8);
                }
            }
            Slog.d(TAG, "loadTunerAppList - ");
            throw th;
        }
    }

    public boolean isScaledByWMS(String packageName, String windowName) {
        ArrayList<Applic> arrayList = this.mTunerAppCache;
        if (arrayList != null) {
            Iterator<Applic> it = arrayList.iterator();
            while (it.hasNext()) {
                Applic app = it.next();
                if (app.getPackageName().equals(packageName) && app.getScalingFlow().equals(VALUE_SCALING_FLOW_WMS)) {
                    return !app.isFiltered(windowName);
                }
            }
            return false;
        }
        return false;
    }

    public boolean isScaledByGameMode(String packageName) {
        ArrayList<Applic> arrayList = this.mTunerAppCache;
        if (arrayList != null) {
            Iterator<Applic> it = arrayList.iterator();
            while (it.hasNext()) {
                Applic app = it.next();
                if (app.getPackageName().equals(packageName) && app.getScalingFlow().equals(VALUE_SCALING_FLOW_GAME)) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    public boolean isScaledBySurfaceView(String packageName) {
        ArrayList<Applic> arrayList = this.mTunerAppCache;
        if (arrayList != null) {
            Iterator<Applic> it = arrayList.iterator();
            while (it.hasNext()) {
                Applic app = it.next();
                if (app.getPackageName().equals(packageName) && !app.getScalingFlow().equals(VALUE_SCALING_FLOW_WMS) && !app.getScalingFlow().equals(VALUE_SCALING_FLOW_GAME)) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    public float getScaleValue(String packageName) {
        ArrayList<Applic> arrayList = this.mTunerAppCache;
        if (arrayList != null && DYNAMIC_RESOLUTION_TUNING == 1) {
            Iterator<Applic> it = arrayList.iterator();
            while (it.hasNext()) {
                Applic app = it.next();
                if (app.getPackageName().equals(packageName)) {
                    return app.getScale();
                }
            }
            return 1.0f;
        }
        return 1.0f;
    }

    public float getScaleWidth(String packageName) {
        ArrayList<Applic> arrayList = this.mTunerAppCache;
        if (arrayList != null) {
            Iterator<Applic> it = arrayList.iterator();
            while (it.hasNext()) {
                Applic app = it.next();
                if (app.getPackageName().equals(packageName)) {
                    return app.getScaleWidth();
                }
            }
            return 1.0f;
        }
        return 1.0f;
    }

    public float getScaleHeight(String packageName) {
        ArrayList<Applic> arrayList = this.mTunerAppCache;
        if (arrayList != null) {
            Iterator<Applic> it = arrayList.iterator();
            while (it.hasNext()) {
                Applic app = it.next();
                if (app.getPackageName().equals(packageName)) {
                    return app.getScaleHeight();
                }
            }
            return 1.0f;
        }
        return 1.0f;
    }

    /* loaded from: classes.dex */
    public static class Applic {
        private String packageName;
        private float scale;
        private float scaleHeight;
        private float scaleWidth;
        private ArrayList<String> filteredWindows = new ArrayList<>();
        private String scalingFlow = "";

        public String getPackageName() {
            return this.packageName;
        }

        public void setPackageName(String packageName) {
            this.packageName = packageName;
        }

        public float getScale() {
            return this.scale;
        }

        public void setScale(float scale) {
            this.scale = scale;
        }

        public void setScale(float scaleWidth, float scaleHeight) {
            this.scaleWidth = scaleWidth;
            this.scaleHeight = scaleHeight;
        }

        public float getScaleWidth() {
            return this.scaleWidth;
        }

        public void setScaleWidth(float scaleWidth) {
            this.scaleWidth = scaleWidth;
        }

        public float getScaleHeight() {
            return this.scaleHeight;
        }

        public void setScaleHeight(float scaleHeight) {
            this.scaleHeight = scaleHeight;
        }

        public void addFilteredWindow(String windowName) {
            this.filteredWindows.add(windowName);
        }

        public boolean isFiltered(String windowName) {
            return this.filteredWindows.contains(windowName);
        }

        public String getScalingFlow() {
            return this.scalingFlow;
        }

        public void setScalingFlow(String scalingFlow) {
            this.scalingFlow = scalingFlow;
        }

        public boolean isGameFlow() {
            return ResolutionTunerAppList.VALUE_SCALING_FLOW_GAME.equals(this.scalingFlow);
        }

        public String toString() {
            return "App{packageName='" + this.packageName + "', scale='" + this.scale + "', scaleWidth='" + this.scaleWidth + "', scaleHeight='" + this.scaleHeight + "', filteredWindows= " + this.filteredWindows + "', scalingFlow= " + this.scalingFlow + "'}";
        }
    }

    private ArrayList<Applic> parseAppListFile(InputStream is) {
        Document document;
        ArrayList<Applic> list = new ArrayList<>();
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            try {
                Document document2 = builder.parse(is);
                NodeList appList = document2.getElementsByTagName(TAG_APP);
                int i = 0;
                while (i < appList.getLength()) {
                    Node node_applic = appList.item(i);
                    NodeList childNodes = node_applic.getChildNodes();
                    Applic applic = new Applic();
                    int j = 0;
                    while (j < childNodes.getLength()) {
                        Node childNode = childNodes.item(j);
                        if (childNode.getNodeName().equals(NODE_PACKAGE_NAME)) {
                            String packageName = childNode.getTextContent();
                            applic.setPackageName(packageName);
                            document = document2;
                        } else if (childNode.getNodeName().equals(NODE_SCALE)) {
                            String scale = childNode.getTextContent();
                            if (scale != null && scale.matches("1.[1-9]*[0-9]")) {
                                applic.setScale(Float.parseFloat(scale));
                                document = document2;
                            } else {
                                document = document2;
                                Slog.w(TAG, "there are wrong value of the scale " + scale);
                                deleteFile(APP_LIST_PATH_NEW);
                                applic.setScale(Float.parseFloat("1.f"));
                            }
                        } else {
                            document = document2;
                            if (childNode.getNodeName().startsWith(NODE_FILTERED_WINDOW)) {
                                String filteredWindow = childNode.getTextContent();
                                applic.addFilteredWindow(filteredWindow);
                            } else if (childNode.getNodeName().startsWith(NODE_SCALING_FLOW)) {
                                String scalingFlow = childNode.getTextContent();
                                applic.setScalingFlow(scalingFlow);
                            }
                        }
                        j++;
                        document2 = document;
                    }
                    list.add(applic);
                    Slog.d(TAG, "dom2xml: " + applic);
                    i++;
                    document2 = document2;
                }
                return list;
            } catch (IOException e) {
                e = e;
                Slog.w(TAG, "IOException", e);
                return list;
            } catch (ParserConfigurationException e2) {
                e = e2;
                Slog.w(TAG, "dom2xml ParserConfigurationException", e);
                return list;
            } catch (SAXException e3) {
                e = e3;
                deleteFile(APP_LIST_PATH_NEW);
                Slog.w(TAG, "dom2xml SAXException", e);
                return list;
            }
        } catch (IOException e4) {
            e = e4;
        } catch (ParserConfigurationException e5) {
            e = e5;
        } catch (SAXException e6) {
            e = e6;
        }
    }

    private ArrayList<Applic> parseAppListFileForGAISR(File listFile) {
        ArrayList<Applic> list = new ArrayList<>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(listFile));
            while (true) {
                String line = br.readLine();
                if (line == null) {
                    break;
                } else if (!line.isEmpty() && line.indexOf("=") >= 1) {
                    String line2 = line.substring(0, line.lastIndexOf("\"") + 1);
                    boolean isGameMode = line2.endsWith("game\"");
                    if (isGameMode) {
                        line2 = line2.replace(",game", "");
                    }
                    String packageName = line2.substring(0, line2.indexOf("="));
                    float width = Float.parseFloat(line2.substring(line2.indexOf("\"") + 1, line2.indexOf(" ")));
                    float height = Float.parseFloat(line2.substring(line2.indexOf(" ") + 1, line2.length() - 1));
                    Applic applic = new Applic();
                    if (isGameMode) {
                        applic.setScalingFlow(VALUE_SCALING_FLOW_GAME);
                    }
                    applic.setPackageName(packageName.trim());
                    applic.setScale(width, height);
                    list.add(applic);
                    Slog.w(TAG, "parseAppListFileForGAISR  packageName: " + packageName);
                }
            }
            br.close();
            br.close();
        } catch (Exception e) {
            Slog.w(TAG, "Failed to read app list for resolution tuner app list " + listFile, e);
        }
        return list;
    }

    private ArrayList<Applic> parseAppListFileForAIVRS(File listFile) {
        ArrayList<Applic> list = new ArrayList<>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(listFile));
            while (true) {
                String line = br.readLine();
                if (line == null) {
                    break;
                } else if (!line.isEmpty() && line.indexOf("=") >= 1 && line.indexOf("\"") >= 1) {
                    String line2 = line.substring(0, line.lastIndexOf("\"") + 1);
                    boolean isGameMode = line2.endsWith("game\"");
                    if (isGameMode) {
                        line2 = line2.replace(",game", "");
                    }
                    String value = line2.substring(line2.indexOf("\"") + 1, line2.length() - 1);
                    if (Integer.parseInt(value) < 70 && Integer.parseInt(value) > 0) {
                        String packageName = line2.substring(0, line2.indexOf("="));
                        Applic applic = new Applic();
                        if (isGameMode) {
                            applic.setScalingFlow(VALUE_SCALING_FLOW_GAME);
                        }
                        applic.setPackageName(packageName.trim());
                        applic.setScale(1.5f);
                        list.add(applic);
                        Slog.w(TAG, "parseAppListFileForAIVRS  packageName: " + packageName + " value:" + value);
                    }
                }
            }
            br.close();
            br.close();
        } catch (Exception e) {
            Slog.w(TAG, "Failed to read app list for resolution tuner app list " + listFile, e);
        }
        return list;
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [463=4] */
    public void reLoadTunerAppList(String mPackName, String mScale) {
        Slog.d(TAG, "reLoadTunerAppList+ ");
        InputStream inputStream = null;
        try {
            try {
                try {
                    File target = new File(APP_LIST_PATH_NEW);
                    if (!target.exists()) {
                        Slog.w(TAG, "Target file doesn't exist: /data/retuner/sf_resolution_tuner_app_list.xml");
                        target = new File(APP_LIST_PATH);
                        if (!target.exists()) {
                            Slog.e(TAG, "Target file doesn't exist: vendor/etc/resolution_tuner_app_list.xml");
                            if (0 != 0) {
                                try {
                                    inputStream.close();
                                    return;
                                } catch (IOException e) {
                                    Slog.w(TAG, "close failed..", e);
                                    return;
                                }
                            }
                            return;
                        }
                    }
                    InputStream inputStream2 = new FileInputStream(target);
                    reParseAppListFile(inputStream2, mPackName, mScale);
                    inputStream2.close();
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
                Slog.w(TAG, "IOException", e3);
                if (0 == 0) {
                    return;
                }
                inputStream.close();
            }
        } catch (IOException e4) {
            Slog.w(TAG, "close failed..", e4);
        }
    }

    /* JADX WARN: Removed duplicated region for block: B:26:0x004d  */
    /* JADX WARN: Removed duplicated region for block: B:81:0x013b A[SYNTHETIC] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public void reParseAppListFile(InputStream is, String mPackName, String mScale) {
        int i;
        Document document;
        NodeList appList;
        ResolutionTunerAppList resolutionTunerAppList = this;
        Document document2 = null;
        NodeList appList2 = null;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            try {
                document2 = builder.parse(is);
                appList2 = document2.getElementsByTagName(TAG_APP);
            } catch (IOException e) {
                e = e;
                Slog.w(TAG, "IOException", e);
                i = 0;
                boolean flag = false;
                while (true) {
                    if (i < appList2.getLength()) {
                    }
                    i++;
                    resolutionTunerAppList = this;
                    document2 = document;
                    appList2 = appList;
                }
                loadTunerAppList();
            } catch (ParserConfigurationException e2) {
                e = e2;
                resolutionTunerAppList.deleteFile(APP_LIST_PATH_NEW);
                Slog.w(TAG, "dom2xml ParserConfigurationException", e);
                i = 0;
                boolean flag2 = false;
                while (true) {
                    if (i < appList2.getLength()) {
                    }
                    i++;
                    resolutionTunerAppList = this;
                    document2 = document;
                    appList2 = appList;
                }
                loadTunerAppList();
            } catch (SAXException e3) {
                e = e3;
                Slog.w(TAG, "dom2xml SAXException", e);
                i = 0;
                boolean flag22 = false;
                while (true) {
                    if (i < appList2.getLength()) {
                    }
                    i++;
                    resolutionTunerAppList = this;
                    document2 = document;
                    appList2 = appList;
                }
                loadTunerAppList();
            }
        } catch (IOException e4) {
            e = e4;
        } catch (ParserConfigurationException e5) {
            e = e5;
        } catch (SAXException e6) {
            e = e6;
        }
        i = 0;
        boolean flag222 = false;
        while (true) {
            if (i < appList2.getLength()) {
                break;
            }
            Node node_applic = appList2.item(i);
            NodeList childNodes = node_applic.getChildNodes();
            int j = 0;
            while (true) {
                if (j >= childNodes.getLength() - 2) {
                    document = document2;
                    appList = appList2;
                    break;
                }
                Node childNode = childNodes.item(j);
                if (childNode.getNodeName().equals(NODE_PACKAGE_NAME)) {
                    String packName1 = childNode.getTextContent();
                    if (!packName1.equals(mPackName)) {
                        document = document2;
                        appList = appList2;
                        Slog.d(TAG, "not the same package");
                        break;
                    } else if (childNodes.item(j + 2).getNodeName().equals(NODE_SCALE)) {
                        childNodes.item(j + 2).setTextContent(mScale);
                        TransformerFactory factory2 = TransformerFactory.newInstance();
                        try {
                            if (resolutionTunerAppList.createXmlFile(APP_LIST_PATH_NEW) != null) {
                                Transformer former = factory2.newTransformer();
                                document = document2;
                                try {
                                    appList = appList2;
                                    try {
                                        former.transform(new DOMSource(document2), new StreamResult(new File(APP_LIST_PATH_NEW)));
                                        flag222 = true;
                                        break;
                                    } catch (TransformerConfigurationException e7) {
                                        e = e7;
                                        Slog.w(TAG, "TransformerConfigurationException", e);
                                        j++;
                                        resolutionTunerAppList = this;
                                        document2 = document;
                                        appList2 = appList;
                                    } catch (TransformerException e8) {
                                        e = e8;
                                        Slog.w(TAG, "TransformerException", e);
                                        j++;
                                        resolutionTunerAppList = this;
                                        document2 = document;
                                        appList2 = appList;
                                    }
                                } catch (TransformerConfigurationException e9) {
                                    e = e9;
                                    appList = appList2;
                                } catch (TransformerException e10) {
                                    e = e10;
                                    appList = appList2;
                                }
                            } else {
                                document = document2;
                                appList = appList2;
                                Slog.i(TAG, "Unable to create xmlFile file: createNewFile failed/data/retuner/sf_resolution_tuner_app_list.xml");
                                break;
                            }
                        } catch (TransformerConfigurationException e11) {
                            e = e11;
                            document = document2;
                            appList = appList2;
                        } catch (TransformerException e12) {
                            e = e12;
                            document = document2;
                            appList = appList2;
                        }
                    } else {
                        document = document2;
                        appList = appList2;
                    }
                } else {
                    document = document2;
                    appList = appList2;
                }
                j++;
                resolutionTunerAppList = this;
                document2 = document;
                appList2 = appList;
            }
            if (!flag222) {
                i++;
                resolutionTunerAppList = this;
                document2 = document;
                appList2 = appList;
            } else {
                Slog.d(TAG, "already save successfully i=" + i);
                break;
            }
        }
        loadTunerAppList();
    }

    private File createXmlFile(String filePath) {
        Slog.i(TAG, "enter createXmlFile");
        if (filePath == null || filePath.length() == 0) {
            return null;
        }
        File xmlFile = new File(filePath);
        try {
            if (!xmlFile.createNewFile()) {
                Slog.i(TAG, "create xmlFile failed or the file already exists");
            } else {
                FileUtils.setPermissions(xmlFile.getAbsolutePath(), 438, -1, -1);
            }
        } catch (IOException ioe) {
            Slog.w(TAG, "Exception creating xmlFile file:" + filePath, ioe);
        }
        return xmlFile;
    }

    private void deleteFile(String fileName) {
        File file = new File(fileName);
        if (file.exists() && file.isFile()) {
            if (file.delete()) {
                Slog.d(TAG, " delete/data/retuner/sf_resolution_tuner_app_list.xml successful");
            } else {
                Slog.d(TAG, "delete/data/retuner/sf_resolution_tuner_app_list.xml failed");
            }
        }
    }
}
