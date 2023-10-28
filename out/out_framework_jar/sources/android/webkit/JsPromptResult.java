package android.webkit;

import android.annotation.SystemApi;
import android.webkit.JsResult;
/* loaded from: classes3.dex */
public class JsPromptResult extends JsResult {
    private String mStringResult;

    public void confirm(String result) {
        this.mStringResult = result;
        confirm();
    }

    @SystemApi
    public JsPromptResult(JsResult.ResultReceiver receiver) {
        super(receiver);
    }

    @SystemApi
    public String getStringResult() {
        return this.mStringResult;
    }
}
