package com.lucid.propertiesapi;

import android.util.Log;
import java.io.File;
import java.io.IOException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
/* loaded from: classes2.dex */
public class Utils {
    public static String getPXVersion() {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document document = null;
            try {
                try {
                    document = db.parse(new File("/product/etc/lucid/PowerXtendCommonConfig.xml"));
                } catch (IOException e) {
                    Log.i("LUCID", "getPXVersion: failed to read from /product/etc/lucid/PowerXtendCommonConfig.xml");
                }
            } catch (SAXException e2) {
                Log.i("LUCID", "getPXVersion: failed to read from /product/etc/lucid/PowerXtendCommonConfig.xml");
            }
            if (document == null) {
                Log.i("LUCID", "getPXVersion: retry to read from /system/etc/lucid/PowerXtendCommonConfig.xml");
                try {
                    try {
                        document = db.parse(new File("/system/etc/lucid/PowerXtendCommonConfig.xml"));
                    } catch (IOException e3) {
                        Log.e("LUCID", "getPXVersion: failed to read from PowerXtendCommonConfig.xml");
                    }
                } catch (SAXException e4) {
                    Log.e("LUCID", "getPXVersion: failed to read from PowerXtendCommonConfig.xml");
                }
            }
            if (document == null) {
                return null;
            }
            NodeList nodeList = document.getElementsByTagName("ps_version");
            if (nodeList != null && nodeList.getLength() > 0) {
                if (nodeList.item(0).getNodeType() != 1) {
                    return null;
                }
                String name = nodeList.item(0).getNodeName();
                if (!name.equals("ps_version")) {
                    return null;
                }
                String version = nodeList.item(0).getTextContent();
                return version;
            }
            NodeList nodeList2 = document.getElementsByTagName("soc");
            if (nodeList2 != null && nodeList2.getLength() > 0) {
                if (nodeList2.item(0).getNodeType() != 1) {
                    return null;
                }
                String name2 = nodeList2.item(0).getNodeName();
                if (!name2.equals("soc")) {
                    return null;
                }
                return "4.1";
            }
            return "4.0";
        } catch (ParserConfigurationException e5) {
            Log.e("LUCID", "getPXVersion: failed to read from PowerXtendCommonConfig.xml");
            return null;
        }
    }
}
