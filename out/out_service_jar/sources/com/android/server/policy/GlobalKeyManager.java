package com.android.server.policy;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.os.UserHandle;
import android.util.Log;
import android.util.SparseArray;
import android.view.KeyEvent;
import com.android.internal.util.XmlUtils;
import java.io.IOException;
import java.io.PrintWriter;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes2.dex */
final class GlobalKeyManager {
    private static final String ATTR_COMPONENT = "component";
    private static final String ATTR_DISPATCH_WHEN_NON_INTERACTIVE = "dispatchWhenNonInteractive";
    private static final String ATTR_KEY_CODE = "keyCode";
    private static final String ATTR_VERSION = "version";
    private static final int GLOBAL_KEY_FILE_VERSION = 1;
    private static final String TAG = "GlobalKeyManager";
    private static final String TAG_GLOBAL_KEYS = "global_keys";
    private static final String TAG_KEY = "key";
    private final SparseArray<GlobalKeyAction> mKeyMapping = new SparseArray<>();
    private boolean mBeganFromNonInteractive = false;

    public GlobalKeyManager(Context context) {
        loadGlobalKeys(context);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean handleGlobalKey(Context context, int keyCode, KeyEvent event) {
        GlobalKeyAction action;
        if (this.mKeyMapping.size() <= 0 || (action = this.mKeyMapping.get(keyCode)) == null) {
            return false;
        }
        Intent intent = new GlobalKeyIntent(action.mComponentName, event, this.mBeganFromNonInteractive).getIntent();
        context.sendBroadcastAsUser(intent, UserHandle.CURRENT, null);
        if (event.getAction() == 1) {
            this.mBeganFromNonInteractive = false;
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean shouldHandleGlobalKey(int keyCode) {
        return this.mKeyMapping.get(keyCode) != null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean shouldDispatchFromNonInteractive(int keyCode) {
        GlobalKeyAction action = this.mKeyMapping.get(keyCode);
        if (action == null) {
            return false;
        }
        return action.mDispatchWhenNonInteractive;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setBeganFromNonInteractive() {
        this.mBeganFromNonInteractive = true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public class GlobalKeyAction {
        private final ComponentName mComponentName;
        private final boolean mDispatchWhenNonInteractive;

        GlobalKeyAction(String componentName, String dispatchWhenNonInteractive) {
            this.mComponentName = ComponentName.unflattenFromString(componentName);
            this.mDispatchWhenNonInteractive = Boolean.parseBoolean(dispatchWhenNonInteractive);
        }
    }

    private void loadGlobalKeys(Context context) {
        try {
            XmlResourceParser parser = context.getResources().getXml(18284553);
            try {
                XmlUtils.beginDocument(parser, TAG_GLOBAL_KEYS);
                int version = parser.getAttributeIntValue(null, ATTR_VERSION, 0);
                if (1 == version) {
                    while (true) {
                        XmlUtils.nextElement(parser);
                        String element = parser.getName();
                        if (element == null) {
                            break;
                        } else if (TAG_KEY.equals(element)) {
                            String keyCodeName = parser.getAttributeValue(null, ATTR_KEY_CODE);
                            String componentName = parser.getAttributeValue(null, ATTR_COMPONENT);
                            String dispatchWhenNonInteractive = parser.getAttributeValue(null, ATTR_DISPATCH_WHEN_NON_INTERACTIVE);
                            int keyCode = KeyEvent.keyCodeFromString(keyCodeName);
                            if (keyCode != 0) {
                                this.mKeyMapping.put(keyCode, new GlobalKeyAction(componentName, dispatchWhenNonInteractive));
                            }
                        }
                    }
                }
                if (parser != null) {
                    parser.close();
                }
            } catch (Throwable th) {
                if (parser != null) {
                    try {
                        parser.close();
                    } catch (Throwable th2) {
                        th.addSuppressed(th2);
                    }
                }
                throw th;
            }
        } catch (Resources.NotFoundException e) {
            Log.w(TAG, "global keys file not found", e);
        } catch (IOException e2) {
            Log.w(TAG, "I/O exception reading global keys file", e2);
        } catch (XmlPullParserException e3) {
            Log.w(TAG, "XML parser exception reading global keys file", e3);
        }
    }

    public void dump(String prefix, PrintWriter pw) {
        int numKeys = this.mKeyMapping.size();
        if (numKeys == 0) {
            pw.print(prefix);
            pw.println("mKeyMapping.size=0");
            return;
        }
        pw.print(prefix);
        pw.println("mKeyMapping={");
        for (int i = 0; i < numKeys; i++) {
            pw.print("  ");
            pw.print(prefix);
            pw.print(KeyEvent.keyCodeToString(this.mKeyMapping.keyAt(i)));
            pw.print("=");
            pw.print(this.mKeyMapping.valueAt(i).mComponentName.flattenToString());
            pw.print(",dispatchWhenNonInteractive=");
            pw.println(this.mKeyMapping.valueAt(i).mDispatchWhenNonInteractive);
        }
        pw.print(prefix);
        pw.println("}");
    }
}
