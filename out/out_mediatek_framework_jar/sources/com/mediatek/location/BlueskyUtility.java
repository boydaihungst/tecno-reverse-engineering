package com.mediatek.location;

import android.content.Context;
import android.content.Intent;
import android.os.UserHandle;
/* loaded from: classes.dex */
public class BlueskyUtility {
    public static void sendBlueskyBroadcast(Context context) {
        Intent intent = new Intent("com.google.android.gms.phenotype.FLAG_OVERRIDE").putExtra("package", "com.google.android.location").putExtra("user", "*").putExtra("flags", "Bluesky__bluesky_enable_for_driving").putExtra("values", "true").putExtra("types", "boolean");
        intent.setPackage("com.google.android.gms");
        context.sendBroadcastAsUser(intent, new UserHandle(-2));
        Intent intent2 = new Intent("com.google.android.gms.phenotype.FLAG_OVERRIDE").putExtra("package", "com.google.android.location").putExtra("user", "*").putExtra("flags", "Bluesky__driving_max_speed_for_corrections_mps").putExtra("values", "60").putExtra("types", "long");
        intent2.setPackage("com.google.android.gms");
        context.sendBroadcastAsUser(intent2, new UserHandle(-2));
        Intent intent3 = new Intent("com.google.android.gms.phenotype.FLAG_OVERRIDE").putExtra("package", "com.google.android.location").putExtra("user", "*").putExtra("flags", "Bluesky__driving_ignore_chipset_driving_capability_bit").putExtra("values", "true").putExtra("types", "boolean");
        intent3.setPackage("com.google.android.gms");
        context.sendBroadcastAsUser(intent3, new UserHandle(-2));
    }
}
