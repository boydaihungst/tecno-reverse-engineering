package com.mediatek.server.pm.hbtpackage;

import android.provider.DeviceConfig;
import com.android.server.pm.InstructionSets;
import com.mediatek.internal.os.ZygoteConfigExt;
/* loaded from: classes2.dex */
public class HBTPackage {
    public static void HBTcheckUpdate(String name, String[] HBT_ISA, String[] ORIGISA) {
        if (ZygoteConfigExt.DISPATCH_POLICY == null || !ZygoteConfigExt.DISPATCH_POLICY.checkPackageName(name)) {
            return;
        }
        String[] HBT_ABI = InstructionSets.getDexCodeInstructionSets(HBT_ISA);
        String[] ORIGABI = InstructionSets.getDexCodeInstructionSets(ORIGISA);
        if (ORIGABI[0].equals("arm") && HBT_ABI[0].equals("arm64")) {
            Integer HBT_count = new Integer(DeviceConfig.getInt("vendor_system_native", "hbt_target_installed", 1));
            if (HBT_count.intValue() == 1) {
                if (ZygoteConfigExt.isApp32BoostEnabled()) {
                    DeviceConfig.setProperty("vendor_system_native", "zygote_HBT", "0", false);
                } else {
                    DeviceConfig.setProperty("vendor_system_native", "hbt_binfmt_misc", "0", false);
                }
            }
            DeviceConfig.setProperty("vendor_system_native", "hbt_target_installed", Integer.valueOf(HBT_count.intValue() - 1).toString(), false);
        } else if (ORIGABI[0].equals("arm64") && HBT_ABI[0].equals("arm")) {
            Integer HBT_count2 = new Integer(DeviceConfig.getInt("vendor_system_native", "hbt_target_installed", 0));
            if (HBT_count2.intValue() == 0) {
                if (ZygoteConfigExt.isApp32BoostEnabled()) {
                    DeviceConfig.setProperty("vendor_system_native", "zygote_HBT", "1", false);
                } else {
                    DeviceConfig.setProperty("vendor_system_native", "hbt_binfmt_misc", "1", false);
                }
            }
            DeviceConfig.setProperty("vendor_system_native", "hbt_target_installed", Integer.valueOf(HBT_count2.intValue() + 1).toString(), false);
        }
    }

    public static void HBTcheckInstall(String name, String[] HBT_ISA) {
        if (ZygoteConfigExt.DISPATCH_POLICY == null || !ZygoteConfigExt.DISPATCH_POLICY.checkPackageName(name)) {
            return;
        }
        String[] HBT_ABI = InstructionSets.getDexCodeInstructionSets(HBT_ISA);
        for (String dexCodeIsa : HBT_ABI) {
            if (dexCodeIsa.equals("arm")) {
                Integer HBT_count = new Integer(DeviceConfig.getInt("vendor_system_native", "hbt_target_installed", 0));
                if (HBT_count.intValue() == 0) {
                    if (ZygoteConfigExt.isApp32BoostEnabled()) {
                        DeviceConfig.setProperty("vendor_system_native", "zygote_HBT", "1", false);
                    } else {
                        DeviceConfig.setProperty("vendor_system_native", "hbt_binfmt_misc", "1", false);
                    }
                }
                DeviceConfig.setProperty("vendor_system_native", "hbt_target_installed", Integer.valueOf(HBT_count.intValue() + 1).toString(), false);
                return;
            }
        }
    }

    public static void HBTcheckUninstall(String name, String[] HBT_ISA) {
        if (ZygoteConfigExt.DISPATCH_POLICY == null || !ZygoteConfigExt.DISPATCH_POLICY.checkPackageName(name)) {
            return;
        }
        String[] HBT_ABI = InstructionSets.getDexCodeInstructionSets(HBT_ISA);
        for (String dexCodeIsa : HBT_ABI) {
            if (dexCodeIsa.equals("arm")) {
                Integer HBT_count = new Integer(DeviceConfig.getInt("vendor_system_native", "hbt_target_installed", 1));
                if (HBT_count.intValue() == 1) {
                    if (ZygoteConfigExt.isApp32BoostEnabled()) {
                        DeviceConfig.setProperty("vendor_system_native", "zygote_HBT", "0", false);
                    } else {
                        DeviceConfig.setProperty("vendor_system_native", "hbt_binfmt_misc", "0", false);
                    }
                }
                DeviceConfig.setProperty("vendor_system_native", "hbt_target_installed", Integer.valueOf(HBT_count.intValue() - 1).toString(), false);
                return;
            }
        }
    }
}
