package org.apache.http.params;
@Deprecated
/* loaded from: classes4.dex */
public final class HttpConnectionParams implements CoreConnectionPNames {
    private HttpConnectionParams() {
    }

    public static int getSoTimeout(HttpParams params) {
        if (params == null) {
            throw new IllegalArgumentException("HTTP parameters may not be null");
        }
        return params.getIntParameter(CoreConnectionPNames.SO_TIMEOUT, 0);
    }

    public static void setSoTimeout(HttpParams params, int timeout) {
        if (params == null) {
            throw new IllegalArgumentException("HTTP parameters may not be null");
        }
        params.setIntParameter(CoreConnectionPNames.SO_TIMEOUT, timeout);
    }

    public static boolean getTcpNoDelay(HttpParams params) {
        if (params == null) {
            throw new IllegalArgumentException("HTTP parameters may not be null");
        }
        return params.getBooleanParameter(CoreConnectionPNames.TCP_NODELAY, true);
    }

    public static void setTcpNoDelay(HttpParams params, boolean value) {
        if (params == null) {
            throw new IllegalArgumentException("HTTP parameters may not be null");
        }
        params.setBooleanParameter(CoreConnectionPNames.TCP_NODELAY, value);
    }

    public static int getSocketBufferSize(HttpParams params) {
        if (params == null) {
            throw new IllegalArgumentException("HTTP parameters may not be null");
        }
        return params.getIntParameter(CoreConnectionPNames.SOCKET_BUFFER_SIZE, -1);
    }

    public static void setSocketBufferSize(HttpParams params, int size) {
        if (params == null) {
            throw new IllegalArgumentException("HTTP parameters may not be null");
        }
        params.setIntParameter(CoreConnectionPNames.SOCKET_BUFFER_SIZE, size);
    }

    public static int getLinger(HttpParams params) {
        if (params == null) {
            throw new IllegalArgumentException("HTTP parameters may not be null");
        }
        return params.getIntParameter(CoreConnectionPNames.SO_LINGER, -1);
    }

    public static void setLinger(HttpParams params, int value) {
        if (params == null) {
            throw new IllegalArgumentException("HTTP parameters may not be null");
        }
        params.setIntParameter(CoreConnectionPNames.SO_LINGER, value);
    }

    public static int getConnectionTimeout(HttpParams params) {
        if (params == null) {
            throw new IllegalArgumentException("HTTP parameters may not be null");
        }
        return params.getIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 0);
    }

    public static void setConnectionTimeout(HttpParams params, int timeout) {
        if (params == null) {
            throw new IllegalArgumentException("HTTP parameters may not be null");
        }
        params.setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, timeout);
    }

    public static boolean isStaleCheckingEnabled(HttpParams params) {
        if (params == null) {
            throw new IllegalArgumentException("HTTP parameters may not be null");
        }
        return params.getBooleanParameter(CoreConnectionPNames.STALE_CONNECTION_CHECK, true);
    }

    public static void setStaleCheckingEnabled(HttpParams params, boolean value) {
        if (params == null) {
            throw new IllegalArgumentException("HTTP parameters may not be null");
        }
        params.setBooleanParameter(CoreConnectionPNames.STALE_CONNECTION_CHECK, value);
    }
}
