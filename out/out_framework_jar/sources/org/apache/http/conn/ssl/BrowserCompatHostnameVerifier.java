package org.apache.http.conn.ssl;

import javax.net.ssl.SSLException;
@Deprecated
/* loaded from: classes4.dex */
public class BrowserCompatHostnameVerifier extends AbstractVerifier {
    @Override // org.apache.http.conn.ssl.X509HostnameVerifier
    public final void verify(String host, String[] cns, String[] subjectAlts) throws SSLException {
        verify(host, cns, subjectAlts, false);
    }

    public final String toString() {
        return "BROWSER_COMPATIBLE";
    }
}
