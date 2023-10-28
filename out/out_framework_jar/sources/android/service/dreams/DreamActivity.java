package android.service.dreams;

import android.app.Activity;
import android.os.Bundle;
import android.service.dreams.DreamService;
import android.text.TextUtils;
import com.android.internal.R;
/* loaded from: classes3.dex */
public class DreamActivity extends Activity {
    static final String EXTRA_CALLBACK = "binder";
    static final String EXTRA_DREAM_TITLE = "title";

    @Override // android.app.Activity
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        String title = getIntent().getStringExtra("title");
        if (!TextUtils.isEmpty(title)) {
            setTitle(title);
        }
        Bundle extras = getIntent().getExtras();
        DreamService.DreamActivityCallback callback = (DreamService.DreamActivityCallback) extras.getBinder("binder");
        if (callback != null) {
            callback.onActivityCreated(this);
        }
    }

    @Override // android.app.Activity
    public void onResume() {
        super.onResume();
        overridePendingTransition(R.anim.dream_activity_open_enter, R.anim.dream_activity_open_exit);
    }

    @Override // android.app.Activity
    public void finishAndRemoveTask() {
        super.finishAndRemoveTask();
        overridePendingTransition(0, R.anim.dream_activity_close_exit);
    }
}
