package com.android.internal.app;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import com.android.internal.R;
/* loaded from: classes4.dex */
public class SystemUserHomeActivity extends Activity {
    private static final String TAG = "SystemUserHome";

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // android.app.Activity
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate");
        setContentView(R.layout.system_user_home);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // android.app.Activity
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy");
    }
}
