package com.android.server.pm.pkg;

import android.content.pm.SuspendDialogInfo;
import android.os.BaseBundle;
import android.os.PersistableBundle;
import android.util.Slog;
import android.util.TypedXmlPullParser;
import android.util.TypedXmlSerializer;
import java.io.IOException;
import java.util.Objects;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes2.dex */
public final class SuspendParams {
    private static final String LOG_TAG = "FrameworkPackageUserState";
    private static final String TAG_APP_EXTRAS = "app-extras";
    private static final String TAG_DIALOG_INFO = "dialog-info";
    private static final String TAG_LAUNCHER_EXTRAS = "launcher-extras";
    private final PersistableBundle appExtras;
    private final SuspendDialogInfo dialogInfo;
    private final PersistableBundle launcherExtras;

    public SuspendParams(SuspendDialogInfo dialogInfo, PersistableBundle appExtras, PersistableBundle launcherExtras) {
        this.dialogInfo = dialogInfo;
        this.appExtras = appExtras;
        this.launcherExtras = launcherExtras;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof SuspendParams) {
            SuspendParams other = (SuspendParams) obj;
            return Objects.equals(this.dialogInfo, other.dialogInfo) && BaseBundle.kindofEquals(this.appExtras, other.appExtras) && BaseBundle.kindofEquals(this.launcherExtras, other.launcherExtras);
        }
        return false;
    }

    public int hashCode() {
        int hashCode = Objects.hashCode(this.dialogInfo);
        int i = hashCode * 31;
        PersistableBundle persistableBundle = this.appExtras;
        int hashCode2 = i + (persistableBundle != null ? persistableBundle.size() : 0);
        int hashCode3 = hashCode2 * 31;
        PersistableBundle persistableBundle2 = this.launcherExtras;
        return hashCode3 + (persistableBundle2 != null ? persistableBundle2.size() : 0);
    }

    public void saveToXml(TypedXmlSerializer out) throws IOException {
        if (this.dialogInfo != null) {
            out.startTag((String) null, TAG_DIALOG_INFO);
            this.dialogInfo.saveToXml(out);
            out.endTag((String) null, TAG_DIALOG_INFO);
        }
        if (this.appExtras != null) {
            out.startTag((String) null, TAG_APP_EXTRAS);
            try {
                this.appExtras.saveToXml(out);
            } catch (XmlPullParserException e) {
                Slog.e(LOG_TAG, "Exception while trying to write appExtras. Will be lost on reboot", e);
            }
            out.endTag((String) null, TAG_APP_EXTRAS);
        }
        if (this.launcherExtras != null) {
            out.startTag((String) null, TAG_LAUNCHER_EXTRAS);
            try {
                this.launcherExtras.saveToXml(out);
            } catch (XmlPullParserException e2) {
                Slog.e(LOG_TAG, "Exception while trying to write launcherExtras. Will be lost on reboot", e2);
            }
            out.endTag((String) null, TAG_LAUNCHER_EXTRAS);
        }
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    public static SuspendParams restoreFromXml(TypedXmlPullParser in) throws IOException {
        SuspendDialogInfo readDialogInfo = null;
        PersistableBundle readAppExtras = null;
        PersistableBundle readLauncherExtras = null;
        int currentDepth = in.getDepth();
        while (true) {
            try {
                int type = in.next();
                char c = 1;
                if (type != 1 && (type != 3 || in.getDepth() > currentDepth)) {
                    if (type != 3 && type != 4) {
                        String name = in.getName();
                        switch (name.hashCode()) {
                            case -538220657:
                                if (name.equals(TAG_APP_EXTRAS)) {
                                    break;
                                }
                                c = 65535;
                                break;
                            case -22768109:
                                if (name.equals(TAG_DIALOG_INFO)) {
                                    c = 0;
                                    break;
                                }
                                c = 65535;
                                break;
                            case 1627485488:
                                if (name.equals(TAG_LAUNCHER_EXTRAS)) {
                                    c = 2;
                                    break;
                                }
                                c = 65535;
                                break;
                            default:
                                c = 65535;
                                break;
                        }
                        switch (c) {
                            case 0:
                                readDialogInfo = SuspendDialogInfo.restoreFromXml(in);
                                continue;
                            case 1:
                                readAppExtras = PersistableBundle.restoreFromXml(in);
                                continue;
                            case 2:
                                readLauncherExtras = PersistableBundle.restoreFromXml(in);
                                continue;
                            default:
                                Slog.w(LOG_TAG, "Unknown tag " + in.getName() + " in SuspendParams. Ignoring");
                                continue;
                        }
                    }
                }
            } catch (XmlPullParserException e) {
                Slog.e(LOG_TAG, "Exception while trying to parse SuspendParams, some fields may default", e);
            }
        }
        return new SuspendParams(readDialogInfo, readAppExtras, readLauncherExtras);
    }

    public SuspendDialogInfo getDialogInfo() {
        return this.dialogInfo;
    }

    public PersistableBundle getAppExtras() {
        return this.appExtras;
    }

    public PersistableBundle getLauncherExtras() {
        return this.launcherExtras;
    }
}
