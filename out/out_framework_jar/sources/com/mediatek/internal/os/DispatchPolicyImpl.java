package com.mediatek.internal.os;

import android.os.SystemProperties;
import android.util.JsonReader;
import android.util.Log;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.HashSet;
/* loaded from: classes4.dex */
public class DispatchPolicyImpl {
    private static final String APP32_BOOST_CONFIG = getConfig();
    private static final String DEFAULT_BOOST_CONFIG = "empty";
    private static final String LOG_TAG = "app32_boost";
    private static final String PROP_APP32_BOOST_CONFIG = "vendor.mtk.app32_boost_config";

    private static String getConfig() {
        if (BoardApiLevelChecker.check(33)) {
            String config = SystemProperties.get(PROP_APP32_BOOST_CONFIG, DEFAULT_BOOST_CONFIG);
            return config;
        }
        return DEFAULT_BOOST_CONFIG;
    }

    /* loaded from: classes4.dex */
    private static class ConfigLoader {
        private JsonReader mReader;

        public ConfigLoader(InputStreamReader in) {
            JsonReader jsonReader = new JsonReader(in);
            this.mReader = jsonReader;
            jsonReader.setLenient(true);
        }

        public void load(HashSet<String> results) {
            try {
                this.mReader.beginObject();
                while (this.mReader.hasNext()) {
                    String name = this.mReader.nextName();
                    if (name.equals(DispatchPolicyImpl.APP32_BOOST_CONFIG)) {
                        readPackageList(results);
                    } else {
                        this.mReader.skipValue();
                    }
                }
                this.mReader.endObject();
            } catch (IOException e) {
                results.clear();
                Log.w(DispatchPolicyImpl.LOG_TAG, "invalid config");
            }
        }

        private void readPackageList(HashSet<String> results) throws IOException {
            this.mReader.beginArray();
            while (this.mReader.hasNext()) {
                String name = this.mReader.nextString();
                results.add(name);
            }
            this.mReader.endArray();
        }
    }

    /* loaded from: classes4.dex */
    protected static class PackageList {
        private static final String CONFIG_FILE = "/vendor/etc/app32_boost.json";
        private static final boolean DEBUG = false;
        private static final HashSet<String> PACKAGE_LIST;

        protected PackageList() {
        }

        static {
            HashSet<String> hashSet = new HashSet<>();
            PACKAGE_LIST = hashSet;
            Log.i(DispatchPolicyImpl.LOG_TAG, "load " + DispatchPolicyImpl.APP32_BOOST_CONFIG + " in " + CONFIG_FILE);
            try {
                FileInputStream fis = new FileInputStream(CONFIG_FILE);
                ConfigLoader loader = new ConfigLoader(new InputStreamReader(fis, "UTF-8"));
                loader.load(hashSet);
            } catch (FileNotFoundException e) {
                Log.w(DispatchPolicyImpl.LOG_TAG, "config not found");
            } catch (UnsupportedEncodingException | SecurityException e2) {
                Log.w(DispatchPolicyImpl.LOG_TAG, "cannot read config");
            }
        }

        public static boolean find(String packageName) {
            return PACKAGE_LIST.contains(packageName);
        }
    }
}
