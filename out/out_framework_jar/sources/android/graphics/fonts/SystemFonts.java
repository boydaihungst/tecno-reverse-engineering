package android.graphics.fonts;

import android.graphics.FontListParser;
import android.graphics.Typeface;
import android.graphics.fonts.Font;
import android.graphics.fonts.FontFamily;
import android.text.FontConfig;
import android.util.ArrayMap;
import android.util.Log;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes.dex */
public final class SystemFonts {
    private static final String FONTS_XML = "/system/etc/fonts_zg.xml";
    private static final Object LOCK = new Object();
    public static final String OEM_FONT_DIR = "/product/fonts/";
    private static final String OEM_XML = "/product/etc/fonts_customization.xml";
    public static final String SYSTEM_FONT_DIR = "/system/fonts/";
    private static final String TAG = "SystemFonts";
    private static Set<Font> sAvailableFonts;

    private SystemFonts() {
    }

    public static Set<Font> getAvailableFonts() {
        Set<Font> set;
        synchronized (LOCK) {
            if (sAvailableFonts == null) {
                sAvailableFonts = Font.getAvailableFonts();
            }
            set = sAvailableFonts;
        }
        return set;
    }

    public static void resetAvailableFonts() {
        synchronized (LOCK) {
            sAvailableFonts = null;
        }
    }

    private static ByteBuffer mmap(String fullPath) {
        try {
            FileInputStream file = new FileInputStream(fullPath);
            FileChannel fileChannel = file.getChannel();
            long fontSize = fileChannel.size();
            MappedByteBuffer map = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0L, fontSize);
            file.close();
            return map;
        } catch (IOException e) {
            return null;
        }
    }

    private static void pushFamilyToFallback(FontConfig.FontFamily xmlFamily, ArrayMap<String, ArrayList<FontFamily>> fallbackMap, Map<String, ByteBuffer> cache) {
        FontConfig.Font[] fonts;
        String languageTags = xmlFamily.getLocaleList().toLanguageTags();
        int variant = xmlFamily.getVariant();
        ArrayList<FontConfig.Font> defaultFonts = new ArrayList<>();
        ArrayMap<String, ArrayList<FontConfig.Font>> specificFallbackFonts = new ArrayMap<>();
        for (FontConfig.Font font : xmlFamily.getFonts()) {
            String fallbackName = font.getFontFamilyName();
            if (fallbackName == null) {
                defaultFonts.add(font);
            } else {
                ArrayList<FontConfig.Font> fallback = specificFallbackFonts.get(fallbackName);
                if (fallback == null) {
                    fallback = new ArrayList<>();
                    specificFallbackFonts.put(fallbackName, fallback);
                }
                fallback.add(font);
            }
        }
        FontFamily defaultFamily = defaultFonts.isEmpty() ? null : createFontFamily(xmlFamily.getName(), defaultFonts, languageTags, variant, cache);
        for (int i = 0; i < fallbackMap.size(); i++) {
            String name = fallbackMap.keyAt(i);
            ArrayList<FontConfig.Font> fallback2 = specificFallbackFonts.get(name);
            if (fallback2 == null) {
                String familyName = xmlFamily.getName();
                if (defaultFamily != null && (familyName == null || !familyName.equals(name))) {
                    fallbackMap.valueAt(i).add(defaultFamily);
                }
            } else {
                FontFamily family = createFontFamily(xmlFamily.getName(), fallback2, languageTags, variant, cache);
                if (family != null) {
                    fallbackMap.valueAt(i).add(family);
                } else if (defaultFamily != null) {
                    fallbackMap.valueAt(i).add(defaultFamily);
                }
            }
        }
    }

    private static FontFamily createFontFamily(String familyName, List<FontConfig.Font> fonts, String languageTags, int variant, Map<String, ByteBuffer> cache) {
        if (fonts.size() == 0) {
            return null;
        }
        FontFamily.Builder b = null;
        for (int i = 0; i < fonts.size(); i++) {
            FontConfig.Font fontConfig = fonts.get(i);
            String fullPath = fontConfig.getFile().getAbsolutePath();
            ByteBuffer buffer = cache.get(fullPath);
            try {
                if (buffer == null) {
                    if (cache.containsKey(fullPath)) {
                        continue;
                    } else {
                        buffer = mmap(fullPath);
                        cache.put(fullPath, buffer);
                        if (buffer == null) {
                            continue;
                        }
                    }
                }
                Font font = new Font.Builder(buffer, new File(fullPath), languageTags).setWeight(fontConfig.getStyle().getWeight()).setSlant(fontConfig.getStyle().getSlant()).setTtcIndex(fontConfig.getTtcIndex()).setFontVariationSettings(fontConfig.getFontVariationSettings()).build();
                if (b == null) {
                    b = new FontFamily.Builder(font);
                } else {
                    b.addFont(font);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        if (b == null) {
            return null;
        }
        return b.build(languageTags, variant, false);
    }

    private static void appendNamedFamily(FontConfig.FontFamily xmlFamily, ArrayMap<String, ByteBuffer> bufferCache, ArrayMap<String, ArrayList<FontFamily>> fallbackListMap) {
        String familyName = xmlFamily.getName();
        FontFamily family = createFontFamily(familyName, xmlFamily.getFontList(), xmlFamily.getLocaleList().toLanguageTags(), xmlFamily.getVariant(), bufferCache);
        if (family == null) {
            return;
        }
        ArrayList<FontFamily> fallback = new ArrayList<>();
        fallback.add(family);
        fallbackListMap.put(familyName, fallback);
    }

    public static FontConfig getSystemFontConfig(Map<String, File> updatableFontMap, long lastModifiedDate, int configVersion) {
        return getSystemFontConfigInternal(FONTS_XML, SYSTEM_FONT_DIR, OEM_XML, OEM_FONT_DIR, updatableFontMap, lastModifiedDate, configVersion);
    }

    public static FontConfig getSystemPreinstalledFontConfig() {
        return getSystemFontConfigInternal(FONTS_XML, SYSTEM_FONT_DIR, OEM_XML, OEM_FONT_DIR, null, 0L, 0);
    }

    static FontConfig getSystemFontConfigInternal(String fontsXml, String systemFontDir, String oemXml, String productFontDir, Map<String, File> updatableFontMap, long lastModifiedDate, int configVersion) {
        try {
            return FontListParser.parse(fontsXml, systemFontDir, oemXml, productFontDir, updatableFontMap, lastModifiedDate, configVersion);
        } catch (IOException e) {
            Log.e(TAG, "Failed to open/read system font configurations.", e);
            return new FontConfig(Collections.emptyList(), Collections.emptyList(), 0L, 0);
        } catch (XmlPullParserException e2) {
            Log.e(TAG, "Failed to parse the system font configuration.", e2);
            return new FontConfig(Collections.emptyList(), Collections.emptyList(), 0L, 0);
        }
    }

    public static Map<String, FontFamily[]> buildSystemFallback(FontConfig fontConfig) {
        return buildSystemFallback(fontConfig, new ArrayMap());
    }

    public static Map<String, FontFamily[]> buildSystemFallback(FontConfig fontConfig, ArrayMap<String, ByteBuffer> outBufferCache) {
        Map<String, FontFamily[]> fallbackMap = new ArrayMap<>();
        List<FontConfig.FontFamily> xmlFamilies = fontConfig.getFontFamilies();
        ArrayMap<String, ArrayList<FontFamily>> fallbackListMap = new ArrayMap<>();
        for (FontConfig.FontFamily xmlFamily : xmlFamilies) {
            String familyName = xmlFamily.getName();
            if (familyName != null) {
                appendNamedFamily(xmlFamily, outBufferCache, fallbackListMap);
            }
        }
        for (int i = 0; i < xmlFamilies.size(); i++) {
            FontConfig.FontFamily xmlFamily2 = xmlFamilies.get(i);
            if (i == 0 || xmlFamily2.getName() == null) {
                pushFamilyToFallback(xmlFamily2, fallbackListMap, outBufferCache);
            }
        }
        for (int i2 = 0; i2 < fallbackListMap.size(); i2++) {
            String fallbackName = fallbackListMap.keyAt(i2);
            List<FontFamily> familyList = fallbackListMap.valueAt(i2);
            fallbackMap.put(fallbackName, (FontFamily[]) familyList.toArray(new FontFamily[0]));
        }
        return fallbackMap;
    }

    public static Map<String, Typeface> buildSystemTypefaces(FontConfig fontConfig, Map<String, FontFamily[]> fallbackMap) {
        ArrayMap<String, Typeface> result = new ArrayMap<>();
        Typeface.initSystemDefaultTypefaces(fallbackMap, fontConfig.getAliases(), result);
        return result;
    }
}
