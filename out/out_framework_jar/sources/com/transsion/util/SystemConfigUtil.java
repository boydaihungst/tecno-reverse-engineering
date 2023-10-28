package com.transsion.util;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.PatternSyntaxException;
/* loaded from: classes4.dex */
public class SystemConfigUtil {
    private static RAM CURRENT = RAM.RAM_NULL;

    /* loaded from: classes4.dex */
    public enum RAM {
        RAM_1Gb(1),
        RAM_2Gb(2),
        RAM_3Gb(3),
        RAM_4Gb(4),
        RAM_6Gb(6),
        RAM_8Gb(8),
        RAM_12Gb(12),
        RAM_14Gb(14),
        RAM_16Gb(16),
        RAM_NULL(-1);
        
        public int size;

        RAM(int size) {
            this.size = -1;
            this.size = size;
        }
    }

    public static int getRamLevel() {
        checkRAM();
        return (CURRENT.size == RAM.RAM_NULL.size ? RAM.RAM_4Gb : CURRENT).size;
    }

    public static boolean matchRam(int level) {
        checkRAM();
        return CURRENT.size == level;
    }

    private static void checkRAM() {
        if (RAM.RAM_NULL == CURRENT) {
            String[] arrayOfString = null;
            long memTotal = 0;
            try {
                FileReader localFileReader = new FileReader("/proc/meminfo");
                BufferedReader localBufferedReader = new BufferedReader(localFileReader, 1024);
                String firstLine = localBufferedReader.readLine();
                if (firstLine != null) {
                    arrayOfString = firstLine.split("\\s+");
                }
                if (arrayOfString != null && arrayOfString.length > 1) {
                    memTotal = Integer.valueOf(arrayOfString[1]).intValue();
                }
                localFileReader.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e2) {
                e2.printStackTrace();
            } catch (PatternSyntaxException e3) {
                e3.printStackTrace();
            }
            long total_GB = memTotal / 1048576;
            long total_MB = memTotal / 1024;
            if (total_GB != 0 && total_MB % (1024 * total_GB) >= 512) {
                total_GB++;
            }
            if (total_GB <= 1) {
                CURRENT = RAM.RAM_1Gb;
            } else if (total_GB <= 2) {
                CURRENT = RAM.RAM_2Gb;
            } else if (total_GB <= 3) {
                CURRENT = RAM.RAM_3Gb;
            } else if (total_GB <= 4) {
                CURRENT = RAM.RAM_4Gb;
            } else if (total_GB <= 6) {
                CURRENT = RAM.RAM_6Gb;
            } else if (total_GB <= 8) {
                CURRENT = RAM.RAM_8Gb;
            } else if (total_GB <= 12) {
                CURRENT = RAM.RAM_12Gb;
            } else if (total_GB <= 14) {
                CURRENT = RAM.RAM_14Gb;
            } else if (total_GB <= 16) {
                CURRENT = RAM.RAM_16Gb;
            } else {
                CURRENT = RAM.RAM_16Gb;
            }
        }
    }
}
