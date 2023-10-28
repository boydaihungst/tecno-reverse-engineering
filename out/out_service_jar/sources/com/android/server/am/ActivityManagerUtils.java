package com.android.server.am;

import android.app.ActivityThread;
import android.provider.Settings;
import android.util.ArrayMap;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
/* loaded from: classes.dex */
public class ActivityManagerUtils {
    private static Integer sAndroidIdHash;
    private static final ArrayMap<String, Integer> sHashCache = new ArrayMap<>();
    private static String sInjectedAndroidId;

    private ActivityManagerUtils() {
    }

    static void injectAndroidIdForTest(String androidId) {
        sInjectedAndroidId = androidId;
        sAndroidIdHash = null;
    }

    static int getAndroidIdHash() {
        if (sAndroidIdHash == null) {
            String androidId = Settings.Secure.getString(ActivityThread.currentApplication().getContentResolver(), "android_id");
            String str = sInjectedAndroidId;
            if (str == null) {
                str = androidId;
            }
            sAndroidIdHash = Integer.valueOf(getUnsignedHashUnCached(str));
        }
        return sAndroidIdHash.intValue();
    }

    static int getUnsignedHashCached(String s) {
        ArrayMap<String, Integer> arrayMap = sHashCache;
        synchronized (arrayMap) {
            Integer cached = arrayMap.get(s);
            if (cached != null) {
                return cached.intValue();
            }
            int hash = getUnsignedHashUnCached(s);
            arrayMap.put(s.intern(), Integer.valueOf(hash));
            return hash;
        }
    }

    private static int getUnsignedHashUnCached(String s) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            digest.update(s.getBytes());
            return unsignedIntFromBytes(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    static int unsignedIntFromBytes(byte[] longEnoughBytes) {
        return (extractByte(longEnoughBytes, 0) | extractByte(longEnoughBytes, 1) | extractByte(longEnoughBytes, 2) | extractByte(longEnoughBytes, 3)) & Integer.MAX_VALUE;
    }

    private static int extractByte(byte[] bytes, int index) {
        return (bytes[index] & 255) << (index * 8);
    }

    public static boolean shouldSamplePackageForAtom(String packageName, float rate) {
        if (rate <= 0.0f) {
            return false;
        }
        if (rate >= 1.0f) {
            return true;
        }
        int hash = getUnsignedHashCached(packageName) ^ getAndroidIdHash();
        return ((double) hash) / 2.147483647E9d <= ((double) rate);
    }

    public static int hashComponentNameForAtom(String shortInstanceName) {
        return getUnsignedHashUnCached(shortInstanceName) ^ getAndroidIdHash();
    }
}
