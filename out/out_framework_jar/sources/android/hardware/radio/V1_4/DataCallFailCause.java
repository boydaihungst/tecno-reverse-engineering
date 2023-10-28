package android.hardware.radio.V1_4;

import android.security.keystore.KeyProperties;
/* loaded from: classes2.dex */
public final class DataCallFailCause {
    public static final int ACCESS_ATTEMPT_ALREADY_IN_PROGRESS = 2219;
    public static final int ACCESS_BLOCK = 2087;
    public static final int ACCESS_BLOCK_ALL = 2088;
    public static final int ACCESS_CLASS_DSAC_REJECTION = 2108;
    public static final int ACCESS_CONTROL_LIST_CHECK_FAILURE = 2128;
    public static final int ACTIVATION_REJECTED_BCM_VIOLATION = 48;
    public static final int ACTIVATION_REJECT_GGSN = 30;
    public static final int ACTIVATION_REJECT_UNSPECIFIED = 31;
    public static final int APN_DISABLED = 2045;
    public static final int APN_DISALLOWED_ON_ROAMING = 2059;
    public static final int APN_MISMATCH = 2054;
    public static final int APN_PARAMETERS_CHANGED = 2060;
    public static final int APN_PENDING_HANDOVER = 2041;
    public static final int APN_TYPE_CONFLICT = 112;
    public static final int AUTH_FAILURE_ON_EMERGENCY_CALL = 122;
    public static final int BEARER_HANDLING_NOT_SUPPORTED = 60;
    public static final int CALL_DISALLOWED_IN_ROAMING = 2068;
    public static final int CALL_PREEMPT_BY_EMERGENCY_APN = 127;
    public static final int CANNOT_ENCODE_OTA_MESSAGE = 2159;
    public static final int CDMA_ALERT_STOP = 2077;
    public static final int CDMA_INCOMING_CALL = 2076;
    public static final int CDMA_INTERCEPT = 2073;
    public static final int CDMA_LOCK = 2072;
    public static final int CDMA_RELEASE_DUE_TO_SO_REJECTION = 2075;
    public static final int CDMA_REORDER = 2074;
    public static final int CDMA_RETRY_ORDER = 2086;
    public static final int CHANNEL_ACQUISITION_FAILURE = 2078;
    public static final int CLOSE_IN_PROGRESS = 2030;
    public static final int COLLISION_WITH_NETWORK_INITIATED_REQUEST = 56;
    public static final int COMPANION_IFACE_IN_USE = 118;
    public static final int CONCURRENT_SERVICES_INCOMPATIBLE = 2083;
    public static final int CONCURRENT_SERVICES_NOT_ALLOWED = 2091;
    public static final int CONCURRENT_SERVICE_NOT_SUPPORTED_BY_BASE_STATION = 2080;
    public static final int CONDITIONAL_IE_ERROR = 100;
    public static final int CONGESTION = 2106;
    public static final int CONNECTION_RELEASED = 2113;
    public static final int CS_DOMAIN_NOT_AVAILABLE = 2181;
    public static final int CS_FALLBACK_CALL_ESTABLISHMENT_NOT_ALLOWED = 2188;
    public static final int DATA_PLAN_EXPIRED = 2198;
    public static final int DATA_REGISTRATION_FAIL = -2;
    public static final int DATA_ROAMING_SETTINGS_DISABLED = 2064;
    public static final int DATA_SETTINGS_DISABLED = 2063;
    public static final int DBM_OR_SMS_IN_PROGRESS = 2211;
    public static final int DDS_SWITCHED = 2065;
    public static final int DDS_SWITCH_IN_PROGRESS = 2067;
    public static final int DRB_RELEASED_BY_RRC = 2112;
    public static final int DS_EXPLICIT_DEACTIVATION = 2125;
    public static final int DUAL_SWITCH = 2227;
    public static final int DUN_CALL_DISALLOWED = 2056;
    public static final int DUPLICATE_BEARER_ID = 2118;
    public static final int EHRPD_TO_HRPD_FALLBACK = 2049;
    public static final int EMBMS_NOT_ENABLED = 2193;
    public static final int EMBMS_REGULAR_DEACTIVATION = 2195;
    public static final int EMERGENCY_IFACE_ONLY = 116;
    public static final int EMERGENCY_MODE = 2221;
    public static final int EMM_ACCESS_BARRED = 115;
    public static final int EMM_ACCESS_BARRED_INFINITE_RETRY = 121;
    public static final int EMM_ATTACH_FAILED = 2115;
    public static final int EMM_ATTACH_STARTED = 2116;
    public static final int EMM_DETACHED = 2114;
    public static final int EMM_T3417_EXPIRED = 2130;
    public static final int EMM_T3417_EXT_EXPIRED = 2131;
    public static final int EPS_SERVICES_AND_NON_EPS_SERVICES_NOT_ALLOWED = 2178;
    public static final int EPS_SERVICES_NOT_ALLOWED_IN_PLMN = 2179;
    public static final int ERROR_UNSPECIFIED = 65535;
    public static final int ESM_BAD_OTA_MESSAGE = 2122;
    public static final int ESM_BEARER_DEACTIVATED_TO_SYNC_WITH_NETWORK = 2120;
    public static final int ESM_COLLISION_SCENARIOS = 2119;
    public static final int ESM_CONTEXT_TRANSFERRED_DUE_TO_IRAT = 2124;
    public static final int ESM_DOWNLOAD_SERVER_REJECTED_THE_CALL = 2123;
    public static final int ESM_FAILURE = 2182;
    public static final int ESM_INFO_NOT_RECEIVED = 53;
    public static final int ESM_LOCAL_CAUSE_NONE = 2126;
    public static final int ESM_NW_ACTIVATED_DED_BEARER_WITH_ID_OF_DEF_BEARER = 2121;
    public static final int ESM_PROCEDURE_TIME_OUT = 2155;
    public static final int ESM_UNKNOWN_EPS_BEARER_CONTEXT = 2111;
    public static final int EVDO_CONNECTION_DENY_BY_BILLING_OR_AUTHENTICATION_FAILURE = 2201;
    public static final int EVDO_CONNECTION_DENY_BY_GENERAL_OR_NETWORK_BUSY = 2200;
    public static final int EVDO_HDR_CHANGED = 2202;
    public static final int EVDO_HDR_CONNECTION_SETUP_TIMEOUT = 2206;
    public static final int EVDO_HDR_EXITED = 2203;
    public static final int EVDO_HDR_NO_SESSION = 2204;
    public static final int EVDO_USING_GPS_FIX_INSTEAD_OF_HDR_CALL = 2205;
    public static final int FADE = 2217;
    public static final int FAILED_TO_ACQUIRE_COLOCATED_HDR = 2207;
    public static final int FEATURE_NOT_SUPP = 40;
    public static final int FILTER_SEMANTIC_ERROR = 44;
    public static final int FILTER_SYTAX_ERROR = 45;
    public static final int FORBIDDEN_APN_NAME = 2066;
    public static final int GPRS_SERVICES_AND_NON_GPRS_SERVICES_NOT_ALLOWED = 2097;
    public static final int GPRS_SERVICES_NOT_ALLOWED = 2098;
    public static final int GPRS_SERVICES_NOT_ALLOWED_IN_THIS_PLMN = 2103;
    public static final int HANDOFF_PREFERENCE_CHANGED = 2251;
    public static final int HDR_ACCESS_FAILURE = 2213;
    public static final int HDR_FADE = 2212;
    public static final int HDR_NO_LOCK_GRANTED = 2210;
    public static final int IFACE_AND_POL_FAMILY_MISMATCH = 120;
    public static final int IFACE_MISMATCH = 117;
    public static final int ILLEGAL_ME = 2096;
    public static final int ILLEGAL_MS = 2095;
    public static final int IMEI_NOT_ACCEPTED = 2177;
    public static final int IMPLICITLY_DETACHED = 2100;
    public static final int IMSI_UNKNOWN_IN_HOME_SUBSCRIBER_SERVER = 2176;
    public static final int INCOMING_CALL_REJECTED = 2092;
    public static final int INSUFFICIENT_RESOURCES = 26;
    public static final int INTERFACE_IN_USE = 2058;
    public static final int INTERNAL_CALL_PREEMPT_BY_HIGH_PRIO_APN = 114;
    public static final int INTERNAL_EPC_NONEPC_TRANSITION = 2057;
    public static final int INVALID_CONNECTION_ID = 2156;
    public static final int INVALID_DNS_ADDR = 123;
    public static final int INVALID_EMM_STATE = 2190;
    public static final int INVALID_MANDATORY_INFO = 96;
    public static final int INVALID_MODE = 2223;
    public static final int INVALID_PCSCF_ADDR = 113;
    public static final int INVALID_PCSCF_OR_DNS_ADDRESS = 124;
    public static final int INVALID_PRIMARY_NSAPI = 2158;
    public static final int INVALID_SIM_STATE = 2224;
    public static final int INVALID_TRANSACTION_ID = 81;
    public static final int IPV6_ADDRESS_TRANSFER_FAILED = 2047;
    public static final int IPV6_PREFIX_UNAVAILABLE = 2250;
    public static final int IP_ADDRESS_MISMATCH = 119;
    public static final int IP_VERSION_MISMATCH = 2055;
    public static final int IRAT_HANDOVER_FAILED = 2194;
    public static final int IS707B_MAX_ACCESS_PROBES = 2089;
    public static final int LIMITED_TO_IPV4 = 2234;
    public static final int LIMITED_TO_IPV6 = 2235;
    public static final int LLC_SNDCP = 25;
    public static final int LOCAL_END = 2215;
    public static final int LOCATION_AREA_NOT_ALLOWED = 2102;
    public static final int LOWER_LAYER_REGISTRATION_FAILURE = 2197;
    public static final int LOW_POWER_MODE_OR_POWERING_DOWN = 2044;
    public static final int LTE_NAS_SERVICE_REQUEST_FAILED = 2117;
    public static final int LTE_THROTTLING_NOT_REQUIRED = 2127;
    public static final int MAC_FAILURE = 2183;
    public static final int MAXIMIUM_NSAPIS_EXCEEDED = 2157;
    public static final int MAXINUM_SIZE_OF_L2_MESSAGE_EXCEEDED = 2166;
    public static final int MAX_ACCESS_PROBE = 2079;
    public static final int MAX_ACTIVE_PDP_CONTEXT_REACHED = 65;
    public static final int MAX_IPV4_CONNECTIONS = 2052;
    public static final int MAX_IPV6_CONNECTIONS = 2053;
    public static final int MAX_PPP_INACTIVITY_TIMER_EXPIRED = 2046;
    public static final int MESSAGE_INCORRECT_SEMANTIC = 95;
    public static final int MESSAGE_TYPE_UNSUPPORTED = 97;
    public static final int MIP_CONFIG_FAILURE = 2050;
    public static final int MIP_FA_ADMIN_PROHIBITED = 2001;
    public static final int MIP_FA_DELIVERY_STYLE_NOT_SUPPORTED = 2012;
    public static final int MIP_FA_ENCAPSULATION_UNAVAILABLE = 2008;
    public static final int MIP_FA_HOME_AGENT_AUTHENTICATION_FAILURE = 2004;
    public static final int MIP_FA_INSUFFICIENT_RESOURCES = 2002;
    public static final int MIP_FA_MALFORMED_REPLY = 2007;
    public static final int MIP_FA_MALFORMED_REQUEST = 2006;
    public static final int MIP_FA_MISSING_CHALLENGE = 2017;
    public static final int MIP_FA_MISSING_HOME_ADDRESS = 2015;
    public static final int MIP_FA_MISSING_HOME_AGENT = 2014;
    public static final int MIP_FA_MISSING_NAI = 2013;
    public static final int MIP_FA_MOBILE_NODE_AUTHENTICATION_FAILURE = 2003;
    public static final int MIP_FA_REASON_UNSPECIFIED = 2000;
    public static final int MIP_FA_REQUESTED_LIFETIME_TOO_LONG = 2005;
    public static final int MIP_FA_REVERSE_TUNNEL_IS_MANDATORY = 2011;
    public static final int MIP_FA_REVERSE_TUNNEL_UNAVAILABLE = 2010;
    public static final int MIP_FA_STALE_CHALLENGE = 2018;
    public static final int MIP_FA_UNKNOWN_CHALLENGE = 2016;
    public static final int MIP_FA_VJ_HEADER_COMPRESSION_UNAVAILABLE = 2009;
    public static final int MIP_HA_ADMIN_PROHIBITED = 2020;
    public static final int MIP_HA_ENCAPSULATION_UNAVAILABLE = 2029;
    public static final int MIP_HA_FOREIGN_AGENT_AUTHENTICATION_FAILURE = 2023;
    public static final int MIP_HA_INSUFFICIENT_RESOURCES = 2021;
    public static final int MIP_HA_MALFORMED_REQUEST = 2025;
    public static final int MIP_HA_MOBILE_NODE_AUTHENTICATION_FAILURE = 2022;
    public static final int MIP_HA_REASON_UNSPECIFIED = 2019;
    public static final int MIP_HA_REGISTRATION_ID_MISMATCH = 2024;
    public static final int MIP_HA_REVERSE_TUNNEL_IS_MANDATORY = 2028;
    public static final int MIP_HA_REVERSE_TUNNEL_UNAVAILABLE = 2027;
    public static final int MIP_HA_UNKNOWN_HOME_AGENT_ADDRESS = 2026;
    public static final int MISSING_UKNOWN_APN = 27;
    public static final int MODEM_APP_PREEMPTED = 2032;
    public static final int MODEM_RESTART = 2037;
    public static final int MSC_TEMPORARILY_NOT_REACHABLE = 2180;
    public static final int MSG_AND_PROTOCOL_STATE_UNCOMPATIBLE = 101;
    public static final int MSG_TYPE_NONCOMPATIBLE_STATE = 98;
    public static final int MS_IDENTITY_CANNOT_BE_DERIVED_BY_THE_NETWORK = 2099;
    public static final int MULTIPLE_PDP_CALL_NOT_ALLOWED = 2192;
    public static final int MULTI_CONN_TO_SAME_PDN_NOT_ALLOWED = 55;
    public static final int NAS_LAYER_FAILURE = 2191;
    public static final int NAS_REQUEST_REJECTED_BY_NETWORK = 2167;
    public static final int NAS_SIGNALLING = 14;
    public static final int NETWORK_FAILURE = 38;
    public static final int NETWORK_INITIATED_DETACH_NO_AUTO_REATTACH = 2154;
    public static final int NETWORK_INITIATED_DETACH_WITH_AUTO_REATTACH = 2153;
    public static final int NETWORK_INITIATED_TERMINATION = 2031;
    public static final int NONE = 0;
    public static final int NON_IP_NOT_SUPPORTED = 2069;
    public static final int NORMAL_RELEASE = 2218;
    public static final int NO_CDMA_SERVICE = 2084;
    public static final int NO_COLLOCATED_HDR = 2225;
    public static final int NO_EPS_BEARER_CONTEXT_ACTIVATED = 2189;
    public static final int NO_GPRS_CONTEXT = 2094;
    public static final int NO_HYBRID_HDR_SERVICE = 2209;
    public static final int NO_PDP_CONTEXT_ACTIVATED = 2107;
    public static final int NO_RESPONSE_FROM_BASE_STATION = 2081;
    public static final int NO_SERVICE = 2216;
    public static final int NO_SERVICE_ON_GATEWAY = 2093;
    public static final int NSAPI_IN_USE = 35;
    public static final int NULL_APN_DISALLOWED = 2061;
    public static final int OEM_DCFAILCAUSE_1 = 4097;
    public static final int OEM_DCFAILCAUSE_10 = 4106;
    public static final int OEM_DCFAILCAUSE_11 = 4107;
    public static final int OEM_DCFAILCAUSE_12 = 4108;
    public static final int OEM_DCFAILCAUSE_13 = 4109;
    public static final int OEM_DCFAILCAUSE_14 = 4110;
    public static final int OEM_DCFAILCAUSE_15 = 4111;
    public static final int OEM_DCFAILCAUSE_2 = 4098;
    public static final int OEM_DCFAILCAUSE_3 = 4099;
    public static final int OEM_DCFAILCAUSE_4 = 4100;
    public static final int OEM_DCFAILCAUSE_5 = 4101;
    public static final int OEM_DCFAILCAUSE_6 = 4102;
    public static final int OEM_DCFAILCAUSE_7 = 4103;
    public static final int OEM_DCFAILCAUSE_8 = 4104;
    public static final int OEM_DCFAILCAUSE_9 = 4105;
    public static final int ONLY_IPV4V6_ALLOWED = 57;
    public static final int ONLY_IPV4_ALLOWED = 50;
    public static final int ONLY_IPV6_ALLOWED = 51;
    public static final int ONLY_NON_IP_ALLOWED = 58;
    public static final int ONLY_SINGLE_BEARER_ALLOWED = 52;
    public static final int OPERATOR_BARRED = 8;
    public static final int OTASP_COMMIT_IN_PROGRESS = 2208;
    public static final int PDN_CONN_DOES_NOT_EXIST = 54;
    public static final int PDN_INACTIVITY_TIMER_EXPIRED = 2051;
    public static final int PDN_IPV4_CALL_DISALLOWED = 2033;
    public static final int PDN_IPV4_CALL_THROTTLED = 2034;
    public static final int PDN_IPV6_CALL_DISALLOWED = 2035;
    public static final int PDN_IPV6_CALL_THROTTLED = 2036;
    public static final int PDN_NON_IP_CALL_DISALLOWED = 2071;
    public static final int PDN_NON_IP_CALL_THROTTLED = 2070;
    public static final int PDP_ACTIVATE_MAX_RETRY_FAILED = 2109;
    public static final int PDP_DUPLICATE = 2104;
    public static final int PDP_ESTABLISH_TIMEOUT_EXPIRED = 2161;
    public static final int PDP_INACTIVE_TIMEOUT_EXPIRED = 2163;
    public static final int PDP_LOWERLAYER_ERROR = 2164;
    public static final int PDP_MODIFY_COLLISION = 2165;
    public static final int PDP_MODIFY_TIMEOUT_EXPIRED = 2162;
    public static final int PDP_PPP_NOT_SUPPORTED = 2038;
    public static final int PDP_WITHOUT_ACTIVE_TFT = 46;
    public static final int PHONE_IN_USE = 2222;
    public static final int PHYSICAL_LINK_CLOSE_IN_PROGRESS = 2040;
    public static final int PLMN_NOT_ALLOWED = 2101;
    public static final int PPP_AUTH_FAILURE = 2229;
    public static final int PPP_CHAP_FAILURE = 2232;
    public static final int PPP_CLOSE_IN_PROGRESS = 2233;
    public static final int PPP_OPTION_MISMATCH = 2230;
    public static final int PPP_PAP_FAILURE = 2231;
    public static final int PPP_TIMEOUT = 2228;
    public static final int PREF_RADIO_TECH_CHANGED = -4;
    public static final int PROFILE_BEARER_INCOMPATIBLE = 2042;
    public static final int PROTOCOL_ERRORS = 111;
    public static final int QOS_NOT_ACCEPTED = 37;
    public static final int RADIO_ACCESS_BEARER_FAILURE = 2110;
    public static final int RADIO_ACCESS_BEARER_SETUP_FAILURE = 2160;
    public static final int RADIO_POWER_OFF = -5;
    public static final int REDIRECTION_OR_HANDOFF_IN_PROGRESS = 2220;
    public static final int REGULAR_DEACTIVATION = 36;
    public static final int REJECTED_BY_BASE_STATION = 2082;
    public static final int RRC_CONNECTION_ABORTED_AFTER_HANDOVER = 2173;
    public static final int RRC_CONNECTION_ABORTED_AFTER_IRAT_CELL_CHANGE = 2174;
    public static final int RRC_CONNECTION_ABORTED_DUE_TO_IRAT_CHANGE = 2171;
    public static final int RRC_CONNECTION_ABORTED_DURING_IRAT_CELL_CHANGE = 2175;
    public static final int RRC_CONNECTION_ABORT_REQUEST = 2151;
    public static final int RRC_CONNECTION_ACCESS_BARRED = 2139;
    public static final int RRC_CONNECTION_ACCESS_STRATUM_FAILURE = 2137;
    public static final int RRC_CONNECTION_ANOTHER_PROCEDURE_IN_PROGRESS = 2138;
    public static final int RRC_CONNECTION_CELL_NOT_CAMPED = 2144;
    public static final int RRC_CONNECTION_CELL_RESELECTION = 2140;
    public static final int RRC_CONNECTION_CONFIG_FAILURE = 2141;
    public static final int RRC_CONNECTION_INVALID_REQUEST = 2168;
    public static final int RRC_CONNECTION_LINK_FAILURE = 2143;
    public static final int RRC_CONNECTION_NORMAL_RELEASE = 2147;
    public static final int RRC_CONNECTION_OUT_OF_SERVICE_DURING_CELL_REGISTER = 2150;
    public static final int RRC_CONNECTION_RADIO_LINK_FAILURE = 2148;
    public static final int RRC_CONNECTION_REESTABLISHMENT_FAILURE = 2149;
    public static final int RRC_CONNECTION_REJECT_BY_NETWORK = 2146;
    public static final int RRC_CONNECTION_RELEASED_SECURITY_NOT_ACTIVE = 2172;
    public static final int RRC_CONNECTION_RF_UNAVAILABLE = 2170;
    public static final int RRC_CONNECTION_SYSTEM_INFORMATION_BLOCK_READ_ERROR = 2152;
    public static final int RRC_CONNECTION_SYSTEM_INTERVAL_FAILURE = 2145;
    public static final int RRC_CONNECTION_TIMER_EXPIRED = 2142;
    public static final int RRC_CONNECTION_TRACKING_AREA_ID_CHANGED = 2169;
    public static final int RRC_UPLINK_CONNECTION_RELEASE = 2134;
    public static final int RRC_UPLINK_DATA_TRANSMISSION_FAILURE = 2132;
    public static final int RRC_UPLINK_DELIVERY_FAILED_DUE_TO_HANDOVER = 2133;
    public static final int RRC_UPLINK_ERROR_REQUEST_FROM_NAS = 2136;
    public static final int RRC_UPLINK_RADIO_LINK_FAILURE = 2135;
    public static final int RUIM_NOT_PRESENT = 2085;
    public static final int SECURITY_MODE_REJECTED = 2186;
    public static final int SERVICE_NOT_ALLOWED_ON_PLMN = 2129;
    public static final int SERVICE_OPTION_NOT_SUBSCRIBED = 33;
    public static final int SERVICE_OPTION_NOT_SUPPORTED = 32;
    public static final int SERVICE_OPTION_OUT_OF_ORDER = 34;
    public static final int SIGNAL_LOST = -3;
    public static final int SIM_CARD_CHANGED = 2043;
    public static final int SYNCHRONIZATION_FAILURE = 2184;
    public static final int TEST_LOOPBACK_REGULAR_DEACTIVATION = 2196;
    public static final int TETHERED_CALL_ACTIVE = -6;
    public static final int TFT_SEMANTIC_ERROR = 41;
    public static final int TFT_SYTAX_ERROR = 42;
    public static final int THERMAL_EMERGENCY = 2090;
    public static final int THERMAL_MITIGATION = 2062;
    public static final int TRAT_SWAP_FAILED = 2048;
    public static final int UE_INITIATED_DETACH_OR_DISCONNECT = 128;
    public static final int UE_IS_ENTERING_POWERSAVE_MODE = 2226;
    public static final int UE_RAT_CHANGE = 2105;
    public static final int UE_SECURITY_CAPABILITIES_MISMATCH = 2185;
    public static final int UMTS_HANDOVER_TO_IWLAN = 2199;
    public static final int UMTS_REACTIVATION_REQ = 39;
    public static final int UNACCEPTABLE_NON_EPS_AUTHENTICATION = 2187;
    public static final int UNKNOWN_INFO_ELEMENT = 99;
    public static final int UNKNOWN_PDP_ADDRESS_TYPE = 28;
    public static final int UNKNOWN_PDP_CONTEXT = 43;
    public static final int UNPREFERRED_RAT = 2039;
    public static final int UNSUPPORTED_1X_PREV = 2214;
    public static final int UNSUPPORTED_APN_IN_CURRENT_PLMN = 66;
    public static final int UNSUPPORTED_QCI_VALUE = 59;
    public static final int USER_AUTHENTICATION = 29;
    public static final int VOICE_REGISTRATION_FAIL = -1;
    public static final int VSNCP_ADMINISTRATIVELY_PROHIBITED = 2245;
    public static final int VSNCP_APN_UNATHORIZED = 2238;
    public static final int VSNCP_GEN_ERROR = 2237;
    public static final int VSNCP_INSUFFICIENT_PARAMETERS = 2243;
    public static final int VSNCP_NO_PDN_GATEWAY_ADDRESS = 2240;
    public static final int VSNCP_PDN_EXISTS_FOR_THIS_APN = 2248;
    public static final int VSNCP_PDN_GATEWAY_REJECT = 2242;
    public static final int VSNCP_PDN_GATEWAY_UNREACHABLE = 2241;
    public static final int VSNCP_PDN_ID_IN_USE = 2246;
    public static final int VSNCP_PDN_LIMIT_EXCEEDED = 2239;
    public static final int VSNCP_RECONNECT_NOT_ALLOWED = 2249;
    public static final int VSNCP_RESOURCE_UNAVAILABLE = 2244;
    public static final int VSNCP_SUBSCRIBER_LIMITATION = 2247;
    public static final int VSNCP_TIMEOUT = 2236;

    public static final String toString(int o) {
        if (o == 0) {
            return KeyProperties.DIGEST_NONE;
        }
        if (o == 8) {
            return "OPERATOR_BARRED";
        }
        if (o == 14) {
            return "NAS_SIGNALLING";
        }
        if (o == 26) {
            return "INSUFFICIENT_RESOURCES";
        }
        if (o == 27) {
            return "MISSING_UKNOWN_APN";
        }
        if (o == 28) {
            return "UNKNOWN_PDP_ADDRESS_TYPE";
        }
        if (o == 29) {
            return "USER_AUTHENTICATION";
        }
        if (o == 30) {
            return "ACTIVATION_REJECT_GGSN";
        }
        if (o == 31) {
            return "ACTIVATION_REJECT_UNSPECIFIED";
        }
        if (o == 32) {
            return "SERVICE_OPTION_NOT_SUPPORTED";
        }
        if (o == 33) {
            return "SERVICE_OPTION_NOT_SUBSCRIBED";
        }
        if (o == 34) {
            return "SERVICE_OPTION_OUT_OF_ORDER";
        }
        if (o == 35) {
            return "NSAPI_IN_USE";
        }
        if (o == 36) {
            return "REGULAR_DEACTIVATION";
        }
        if (o == 37) {
            return "QOS_NOT_ACCEPTED";
        }
        if (o == 38) {
            return "NETWORK_FAILURE";
        }
        if (o == 39) {
            return "UMTS_REACTIVATION_REQ";
        }
        if (o == 40) {
            return "FEATURE_NOT_SUPP";
        }
        if (o == 41) {
            return "TFT_SEMANTIC_ERROR";
        }
        if (o == 42) {
            return "TFT_SYTAX_ERROR";
        }
        if (o == 43) {
            return "UNKNOWN_PDP_CONTEXT";
        }
        if (o == 44) {
            return "FILTER_SEMANTIC_ERROR";
        }
        if (o == 45) {
            return "FILTER_SYTAX_ERROR";
        }
        if (o == 46) {
            return "PDP_WITHOUT_ACTIVE_TFT";
        }
        if (o == 50) {
            return "ONLY_IPV4_ALLOWED";
        }
        if (o == 51) {
            return "ONLY_IPV6_ALLOWED";
        }
        if (o == 52) {
            return "ONLY_SINGLE_BEARER_ALLOWED";
        }
        if (o == 53) {
            return "ESM_INFO_NOT_RECEIVED";
        }
        if (o == 54) {
            return "PDN_CONN_DOES_NOT_EXIST";
        }
        if (o == 55) {
            return "MULTI_CONN_TO_SAME_PDN_NOT_ALLOWED";
        }
        if (o == 65) {
            return "MAX_ACTIVE_PDP_CONTEXT_REACHED";
        }
        if (o == 66) {
            return "UNSUPPORTED_APN_IN_CURRENT_PLMN";
        }
        if (o == 81) {
            return "INVALID_TRANSACTION_ID";
        }
        if (o == 95) {
            return "MESSAGE_INCORRECT_SEMANTIC";
        }
        if (o == 96) {
            return "INVALID_MANDATORY_INFO";
        }
        if (o == 97) {
            return "MESSAGE_TYPE_UNSUPPORTED";
        }
        if (o == 98) {
            return "MSG_TYPE_NONCOMPATIBLE_STATE";
        }
        if (o == 99) {
            return "UNKNOWN_INFO_ELEMENT";
        }
        if (o == 100) {
            return "CONDITIONAL_IE_ERROR";
        }
        if (o == 101) {
            return "MSG_AND_PROTOCOL_STATE_UNCOMPATIBLE";
        }
        if (o == 111) {
            return "PROTOCOL_ERRORS";
        }
        if (o == 112) {
            return "APN_TYPE_CONFLICT";
        }
        if (o == 113) {
            return "INVALID_PCSCF_ADDR";
        }
        if (o == 114) {
            return "INTERNAL_CALL_PREEMPT_BY_HIGH_PRIO_APN";
        }
        if (o == 115) {
            return "EMM_ACCESS_BARRED";
        }
        if (o == 116) {
            return "EMERGENCY_IFACE_ONLY";
        }
        if (o == 117) {
            return "IFACE_MISMATCH";
        }
        if (o == 118) {
            return "COMPANION_IFACE_IN_USE";
        }
        if (o == 119) {
            return "IP_ADDRESS_MISMATCH";
        }
        if (o == 120) {
            return "IFACE_AND_POL_FAMILY_MISMATCH";
        }
        if (o == 121) {
            return "EMM_ACCESS_BARRED_INFINITE_RETRY";
        }
        if (o == 122) {
            return "AUTH_FAILURE_ON_EMERGENCY_CALL";
        }
        if (o == 4097) {
            return "OEM_DCFAILCAUSE_1";
        }
        if (o == 4098) {
            return "OEM_DCFAILCAUSE_2";
        }
        if (o == 4099) {
            return "OEM_DCFAILCAUSE_3";
        }
        if (o == 4100) {
            return "OEM_DCFAILCAUSE_4";
        }
        if (o == 4101) {
            return "OEM_DCFAILCAUSE_5";
        }
        if (o == 4102) {
            return "OEM_DCFAILCAUSE_6";
        }
        if (o == 4103) {
            return "OEM_DCFAILCAUSE_7";
        }
        if (o == 4104) {
            return "OEM_DCFAILCAUSE_8";
        }
        if (o == 4105) {
            return "OEM_DCFAILCAUSE_9";
        }
        if (o == 4106) {
            return "OEM_DCFAILCAUSE_10";
        }
        if (o == 4107) {
            return "OEM_DCFAILCAUSE_11";
        }
        if (o == 4108) {
            return "OEM_DCFAILCAUSE_12";
        }
        if (o == 4109) {
            return "OEM_DCFAILCAUSE_13";
        }
        if (o == 4110) {
            return "OEM_DCFAILCAUSE_14";
        }
        if (o == 4111) {
            return "OEM_DCFAILCAUSE_15";
        }
        if (o == -1) {
            return "VOICE_REGISTRATION_FAIL";
        }
        if (o == -2) {
            return "DATA_REGISTRATION_FAIL";
        }
        if (o == -3) {
            return "SIGNAL_LOST";
        }
        if (o == -4) {
            return "PREF_RADIO_TECH_CHANGED";
        }
        if (o == -5) {
            return "RADIO_POWER_OFF";
        }
        if (o == -6) {
            return "TETHERED_CALL_ACTIVE";
        }
        if (o == 65535) {
            return "ERROR_UNSPECIFIED";
        }
        if (o == 25) {
            return "LLC_SNDCP";
        }
        if (o == 48) {
            return "ACTIVATION_REJECTED_BCM_VIOLATION";
        }
        if (o == 56) {
            return "COLLISION_WITH_NETWORK_INITIATED_REQUEST";
        }
        if (o == 57) {
            return "ONLY_IPV4V6_ALLOWED";
        }
        if (o == 58) {
            return "ONLY_NON_IP_ALLOWED";
        }
        if (o == 59) {
            return "UNSUPPORTED_QCI_VALUE";
        }
        if (o == 60) {
            return "BEARER_HANDLING_NOT_SUPPORTED";
        }
        if (o == 123) {
            return "INVALID_DNS_ADDR";
        }
        if (o == 124) {
            return "INVALID_PCSCF_OR_DNS_ADDRESS";
        }
        if (o == 127) {
            return "CALL_PREEMPT_BY_EMERGENCY_APN";
        }
        if (o == 128) {
            return "UE_INITIATED_DETACH_OR_DISCONNECT";
        }
        if (o == 2000) {
            return "MIP_FA_REASON_UNSPECIFIED";
        }
        if (o == 2001) {
            return "MIP_FA_ADMIN_PROHIBITED";
        }
        if (o == 2002) {
            return "MIP_FA_INSUFFICIENT_RESOURCES";
        }
        if (o == 2003) {
            return "MIP_FA_MOBILE_NODE_AUTHENTICATION_FAILURE";
        }
        if (o == 2004) {
            return "MIP_FA_HOME_AGENT_AUTHENTICATION_FAILURE";
        }
        if (o == 2005) {
            return "MIP_FA_REQUESTED_LIFETIME_TOO_LONG";
        }
        if (o == 2006) {
            return "MIP_FA_MALFORMED_REQUEST";
        }
        if (o == 2007) {
            return "MIP_FA_MALFORMED_REPLY";
        }
        if (o == 2008) {
            return "MIP_FA_ENCAPSULATION_UNAVAILABLE";
        }
        if (o == 2009) {
            return "MIP_FA_VJ_HEADER_COMPRESSION_UNAVAILABLE";
        }
        if (o == 2010) {
            return "MIP_FA_REVERSE_TUNNEL_UNAVAILABLE";
        }
        if (o == 2011) {
            return "MIP_FA_REVERSE_TUNNEL_IS_MANDATORY";
        }
        if (o == 2012) {
            return "MIP_FA_DELIVERY_STYLE_NOT_SUPPORTED";
        }
        if (o == 2013) {
            return "MIP_FA_MISSING_NAI";
        }
        if (o == 2014) {
            return "MIP_FA_MISSING_HOME_AGENT";
        }
        if (o == 2015) {
            return "MIP_FA_MISSING_HOME_ADDRESS";
        }
        if (o == 2016) {
            return "MIP_FA_UNKNOWN_CHALLENGE";
        }
        if (o == 2017) {
            return "MIP_FA_MISSING_CHALLENGE";
        }
        if (o == 2018) {
            return "MIP_FA_STALE_CHALLENGE";
        }
        if (o == 2019) {
            return "MIP_HA_REASON_UNSPECIFIED";
        }
        if (o == 2020) {
            return "MIP_HA_ADMIN_PROHIBITED";
        }
        if (o == 2021) {
            return "MIP_HA_INSUFFICIENT_RESOURCES";
        }
        if (o == 2022) {
            return "MIP_HA_MOBILE_NODE_AUTHENTICATION_FAILURE";
        }
        if (o == 2023) {
            return "MIP_HA_FOREIGN_AGENT_AUTHENTICATION_FAILURE";
        }
        if (o == 2024) {
            return "MIP_HA_REGISTRATION_ID_MISMATCH";
        }
        if (o == 2025) {
            return "MIP_HA_MALFORMED_REQUEST";
        }
        if (o == 2026) {
            return "MIP_HA_UNKNOWN_HOME_AGENT_ADDRESS";
        }
        if (o == 2027) {
            return "MIP_HA_REVERSE_TUNNEL_UNAVAILABLE";
        }
        if (o == 2028) {
            return "MIP_HA_REVERSE_TUNNEL_IS_MANDATORY";
        }
        if (o == 2029) {
            return "MIP_HA_ENCAPSULATION_UNAVAILABLE";
        }
        if (o == 2030) {
            return "CLOSE_IN_PROGRESS";
        }
        if (o == 2031) {
            return "NETWORK_INITIATED_TERMINATION";
        }
        if (o == 2032) {
            return "MODEM_APP_PREEMPTED";
        }
        if (o == 2033) {
            return "PDN_IPV4_CALL_DISALLOWED";
        }
        if (o == 2034) {
            return "PDN_IPV4_CALL_THROTTLED";
        }
        if (o == 2035) {
            return "PDN_IPV6_CALL_DISALLOWED";
        }
        if (o == 2036) {
            return "PDN_IPV6_CALL_THROTTLED";
        }
        if (o == 2037) {
            return "MODEM_RESTART";
        }
        if (o == 2038) {
            return "PDP_PPP_NOT_SUPPORTED";
        }
        if (o == 2039) {
            return "UNPREFERRED_RAT";
        }
        if (o == 2040) {
            return "PHYSICAL_LINK_CLOSE_IN_PROGRESS";
        }
        if (o == 2041) {
            return "APN_PENDING_HANDOVER";
        }
        if (o == 2042) {
            return "PROFILE_BEARER_INCOMPATIBLE";
        }
        if (o == 2043) {
            return "SIM_CARD_CHANGED";
        }
        if (o == 2044) {
            return "LOW_POWER_MODE_OR_POWERING_DOWN";
        }
        if (o == 2045) {
            return "APN_DISABLED";
        }
        if (o == 2046) {
            return "MAX_PPP_INACTIVITY_TIMER_EXPIRED";
        }
        if (o == 2047) {
            return "IPV6_ADDRESS_TRANSFER_FAILED";
        }
        if (o == 2048) {
            return "TRAT_SWAP_FAILED";
        }
        if (o == 2049) {
            return "EHRPD_TO_HRPD_FALLBACK";
        }
        if (o == 2050) {
            return "MIP_CONFIG_FAILURE";
        }
        if (o == 2051) {
            return "PDN_INACTIVITY_TIMER_EXPIRED";
        }
        if (o == 2052) {
            return "MAX_IPV4_CONNECTIONS";
        }
        if (o == 2053) {
            return "MAX_IPV6_CONNECTIONS";
        }
        if (o == 2054) {
            return "APN_MISMATCH";
        }
        if (o == 2055) {
            return "IP_VERSION_MISMATCH";
        }
        if (o == 2056) {
            return "DUN_CALL_DISALLOWED";
        }
        if (o == 2057) {
            return "INTERNAL_EPC_NONEPC_TRANSITION";
        }
        if (o == 2058) {
            return "INTERFACE_IN_USE";
        }
        if (o == 2059) {
            return "APN_DISALLOWED_ON_ROAMING";
        }
        if (o == 2060) {
            return "APN_PARAMETERS_CHANGED";
        }
        if (o == 2061) {
            return "NULL_APN_DISALLOWED";
        }
        if (o == 2062) {
            return "THERMAL_MITIGATION";
        }
        if (o == 2063) {
            return "DATA_SETTINGS_DISABLED";
        }
        if (o == 2064) {
            return "DATA_ROAMING_SETTINGS_DISABLED";
        }
        if (o == 2065) {
            return "DDS_SWITCHED";
        }
        if (o == 2066) {
            return "FORBIDDEN_APN_NAME";
        }
        if (o == 2067) {
            return "DDS_SWITCH_IN_PROGRESS";
        }
        if (o == 2068) {
            return "CALL_DISALLOWED_IN_ROAMING";
        }
        if (o == 2069) {
            return "NON_IP_NOT_SUPPORTED";
        }
        if (o == 2070) {
            return "PDN_NON_IP_CALL_THROTTLED";
        }
        if (o == 2071) {
            return "PDN_NON_IP_CALL_DISALLOWED";
        }
        if (o == 2072) {
            return "CDMA_LOCK";
        }
        if (o == 2073) {
            return "CDMA_INTERCEPT";
        }
        if (o == 2074) {
            return "CDMA_REORDER";
        }
        if (o == 2075) {
            return "CDMA_RELEASE_DUE_TO_SO_REJECTION";
        }
        if (o == 2076) {
            return "CDMA_INCOMING_CALL";
        }
        if (o == 2077) {
            return "CDMA_ALERT_STOP";
        }
        if (o == 2078) {
            return "CHANNEL_ACQUISITION_FAILURE";
        }
        if (o == 2079) {
            return "MAX_ACCESS_PROBE";
        }
        if (o == 2080) {
            return "CONCURRENT_SERVICE_NOT_SUPPORTED_BY_BASE_STATION";
        }
        if (o == 2081) {
            return "NO_RESPONSE_FROM_BASE_STATION";
        }
        if (o == 2082) {
            return "REJECTED_BY_BASE_STATION";
        }
        if (o == 2083) {
            return "CONCURRENT_SERVICES_INCOMPATIBLE";
        }
        if (o == 2084) {
            return "NO_CDMA_SERVICE";
        }
        if (o == 2085) {
            return "RUIM_NOT_PRESENT";
        }
        if (o == 2086) {
            return "CDMA_RETRY_ORDER";
        }
        if (o == 2087) {
            return "ACCESS_BLOCK";
        }
        if (o == 2088) {
            return "ACCESS_BLOCK_ALL";
        }
        if (o == 2089) {
            return "IS707B_MAX_ACCESS_PROBES";
        }
        if (o == 2090) {
            return "THERMAL_EMERGENCY";
        }
        if (o == 2091) {
            return "CONCURRENT_SERVICES_NOT_ALLOWED";
        }
        if (o == 2092) {
            return "INCOMING_CALL_REJECTED";
        }
        if (o == 2093) {
            return "NO_SERVICE_ON_GATEWAY";
        }
        if (o == 2094) {
            return "NO_GPRS_CONTEXT";
        }
        if (o == 2095) {
            return "ILLEGAL_MS";
        }
        if (o == 2096) {
            return "ILLEGAL_ME";
        }
        if (o == 2097) {
            return "GPRS_SERVICES_AND_NON_GPRS_SERVICES_NOT_ALLOWED";
        }
        if (o == 2098) {
            return "GPRS_SERVICES_NOT_ALLOWED";
        }
        if (o == 2099) {
            return "MS_IDENTITY_CANNOT_BE_DERIVED_BY_THE_NETWORK";
        }
        if (o == 2100) {
            return "IMPLICITLY_DETACHED";
        }
        if (o == 2101) {
            return "PLMN_NOT_ALLOWED";
        }
        if (o == 2102) {
            return "LOCATION_AREA_NOT_ALLOWED";
        }
        if (o == 2103) {
            return "GPRS_SERVICES_NOT_ALLOWED_IN_THIS_PLMN";
        }
        if (o == 2104) {
            return "PDP_DUPLICATE";
        }
        if (o == 2105) {
            return "UE_RAT_CHANGE";
        }
        if (o == 2106) {
            return "CONGESTION";
        }
        if (o == 2107) {
            return "NO_PDP_CONTEXT_ACTIVATED";
        }
        if (o == 2108) {
            return "ACCESS_CLASS_DSAC_REJECTION";
        }
        if (o == 2109) {
            return "PDP_ACTIVATE_MAX_RETRY_FAILED";
        }
        if (o == 2110) {
            return "RADIO_ACCESS_BEARER_FAILURE";
        }
        if (o == 2111) {
            return "ESM_UNKNOWN_EPS_BEARER_CONTEXT";
        }
        if (o == 2112) {
            return "DRB_RELEASED_BY_RRC";
        }
        if (o == 2113) {
            return "CONNECTION_RELEASED";
        }
        if (o == 2114) {
            return "EMM_DETACHED";
        }
        if (o == 2115) {
            return "EMM_ATTACH_FAILED";
        }
        if (o == 2116) {
            return "EMM_ATTACH_STARTED";
        }
        if (o == 2117) {
            return "LTE_NAS_SERVICE_REQUEST_FAILED";
        }
        if (o == 2118) {
            return "DUPLICATE_BEARER_ID";
        }
        if (o == 2119) {
            return "ESM_COLLISION_SCENARIOS";
        }
        if (o == 2120) {
            return "ESM_BEARER_DEACTIVATED_TO_SYNC_WITH_NETWORK";
        }
        if (o == 2121) {
            return "ESM_NW_ACTIVATED_DED_BEARER_WITH_ID_OF_DEF_BEARER";
        }
        if (o == 2122) {
            return "ESM_BAD_OTA_MESSAGE";
        }
        if (o == 2123) {
            return "ESM_DOWNLOAD_SERVER_REJECTED_THE_CALL";
        }
        if (o == 2124) {
            return "ESM_CONTEXT_TRANSFERRED_DUE_TO_IRAT";
        }
        if (o == 2125) {
            return "DS_EXPLICIT_DEACTIVATION";
        }
        if (o == 2126) {
            return "ESM_LOCAL_CAUSE_NONE";
        }
        if (o == 2127) {
            return "LTE_THROTTLING_NOT_REQUIRED";
        }
        if (o == 2128) {
            return "ACCESS_CONTROL_LIST_CHECK_FAILURE";
        }
        if (o == 2129) {
            return "SERVICE_NOT_ALLOWED_ON_PLMN";
        }
        if (o == 2130) {
            return "EMM_T3417_EXPIRED";
        }
        if (o == 2131) {
            return "EMM_T3417_EXT_EXPIRED";
        }
        if (o == 2132) {
            return "RRC_UPLINK_DATA_TRANSMISSION_FAILURE";
        }
        if (o == 2133) {
            return "RRC_UPLINK_DELIVERY_FAILED_DUE_TO_HANDOVER";
        }
        if (o == 2134) {
            return "RRC_UPLINK_CONNECTION_RELEASE";
        }
        if (o == 2135) {
            return "RRC_UPLINK_RADIO_LINK_FAILURE";
        }
        if (o == 2136) {
            return "RRC_UPLINK_ERROR_REQUEST_FROM_NAS";
        }
        if (o == 2137) {
            return "RRC_CONNECTION_ACCESS_STRATUM_FAILURE";
        }
        if (o == 2138) {
            return "RRC_CONNECTION_ANOTHER_PROCEDURE_IN_PROGRESS";
        }
        if (o == 2139) {
            return "RRC_CONNECTION_ACCESS_BARRED";
        }
        if (o == 2140) {
            return "RRC_CONNECTION_CELL_RESELECTION";
        }
        if (o == 2141) {
            return "RRC_CONNECTION_CONFIG_FAILURE";
        }
        if (o == 2142) {
            return "RRC_CONNECTION_TIMER_EXPIRED";
        }
        if (o == 2143) {
            return "RRC_CONNECTION_LINK_FAILURE";
        }
        if (o == 2144) {
            return "RRC_CONNECTION_CELL_NOT_CAMPED";
        }
        if (o == 2145) {
            return "RRC_CONNECTION_SYSTEM_INTERVAL_FAILURE";
        }
        if (o == 2146) {
            return "RRC_CONNECTION_REJECT_BY_NETWORK";
        }
        if (o == 2147) {
            return "RRC_CONNECTION_NORMAL_RELEASE";
        }
        if (o == 2148) {
            return "RRC_CONNECTION_RADIO_LINK_FAILURE";
        }
        if (o == 2149) {
            return "RRC_CONNECTION_REESTABLISHMENT_FAILURE";
        }
        if (o == 2150) {
            return "RRC_CONNECTION_OUT_OF_SERVICE_DURING_CELL_REGISTER";
        }
        if (o == 2151) {
            return "RRC_CONNECTION_ABORT_REQUEST";
        }
        if (o == 2152) {
            return "RRC_CONNECTION_SYSTEM_INFORMATION_BLOCK_READ_ERROR";
        }
        if (o == 2153) {
            return "NETWORK_INITIATED_DETACH_WITH_AUTO_REATTACH";
        }
        if (o == 2154) {
            return "NETWORK_INITIATED_DETACH_NO_AUTO_REATTACH";
        }
        if (o == 2155) {
            return "ESM_PROCEDURE_TIME_OUT";
        }
        if (o == 2156) {
            return "INVALID_CONNECTION_ID";
        }
        if (o == 2157) {
            return "MAXIMIUM_NSAPIS_EXCEEDED";
        }
        if (o == 2158) {
            return "INVALID_PRIMARY_NSAPI";
        }
        if (o == 2159) {
            return "CANNOT_ENCODE_OTA_MESSAGE";
        }
        if (o == 2160) {
            return "RADIO_ACCESS_BEARER_SETUP_FAILURE";
        }
        if (o == 2161) {
            return "PDP_ESTABLISH_TIMEOUT_EXPIRED";
        }
        if (o == 2162) {
            return "PDP_MODIFY_TIMEOUT_EXPIRED";
        }
        if (o == 2163) {
            return "PDP_INACTIVE_TIMEOUT_EXPIRED";
        }
        if (o == 2164) {
            return "PDP_LOWERLAYER_ERROR";
        }
        if (o == 2165) {
            return "PDP_MODIFY_COLLISION";
        }
        if (o == 2166) {
            return "MAXINUM_SIZE_OF_L2_MESSAGE_EXCEEDED";
        }
        if (o == 2167) {
            return "NAS_REQUEST_REJECTED_BY_NETWORK";
        }
        if (o == 2168) {
            return "RRC_CONNECTION_INVALID_REQUEST";
        }
        if (o == 2169) {
            return "RRC_CONNECTION_TRACKING_AREA_ID_CHANGED";
        }
        if (o == 2170) {
            return "RRC_CONNECTION_RF_UNAVAILABLE";
        }
        if (o == 2171) {
            return "RRC_CONNECTION_ABORTED_DUE_TO_IRAT_CHANGE";
        }
        if (o == 2172) {
            return "RRC_CONNECTION_RELEASED_SECURITY_NOT_ACTIVE";
        }
        if (o == 2173) {
            return "RRC_CONNECTION_ABORTED_AFTER_HANDOVER";
        }
        if (o == 2174) {
            return "RRC_CONNECTION_ABORTED_AFTER_IRAT_CELL_CHANGE";
        }
        if (o == 2175) {
            return "RRC_CONNECTION_ABORTED_DURING_IRAT_CELL_CHANGE";
        }
        if (o == 2176) {
            return "IMSI_UNKNOWN_IN_HOME_SUBSCRIBER_SERVER";
        }
        if (o == 2177) {
            return "IMEI_NOT_ACCEPTED";
        }
        if (o == 2178) {
            return "EPS_SERVICES_AND_NON_EPS_SERVICES_NOT_ALLOWED";
        }
        if (o == 2179) {
            return "EPS_SERVICES_NOT_ALLOWED_IN_PLMN";
        }
        if (o == 2180) {
            return "MSC_TEMPORARILY_NOT_REACHABLE";
        }
        if (o == 2181) {
            return "CS_DOMAIN_NOT_AVAILABLE";
        }
        if (o == 2182) {
            return "ESM_FAILURE";
        }
        if (o == 2183) {
            return "MAC_FAILURE";
        }
        if (o == 2184) {
            return "SYNCHRONIZATION_FAILURE";
        }
        if (o == 2185) {
            return "UE_SECURITY_CAPABILITIES_MISMATCH";
        }
        if (o == 2186) {
            return "SECURITY_MODE_REJECTED";
        }
        if (o == 2187) {
            return "UNACCEPTABLE_NON_EPS_AUTHENTICATION";
        }
        if (o == 2188) {
            return "CS_FALLBACK_CALL_ESTABLISHMENT_NOT_ALLOWED";
        }
        if (o == 2189) {
            return "NO_EPS_BEARER_CONTEXT_ACTIVATED";
        }
        if (o == 2190) {
            return "INVALID_EMM_STATE";
        }
        if (o == 2191) {
            return "NAS_LAYER_FAILURE";
        }
        if (o == 2192) {
            return "MULTIPLE_PDP_CALL_NOT_ALLOWED";
        }
        if (o == 2193) {
            return "EMBMS_NOT_ENABLED";
        }
        if (o == 2194) {
            return "IRAT_HANDOVER_FAILED";
        }
        if (o == 2195) {
            return "EMBMS_REGULAR_DEACTIVATION";
        }
        if (o == 2196) {
            return "TEST_LOOPBACK_REGULAR_DEACTIVATION";
        }
        if (o == 2197) {
            return "LOWER_LAYER_REGISTRATION_FAILURE";
        }
        if (o == 2198) {
            return "DATA_PLAN_EXPIRED";
        }
        if (o == 2199) {
            return "UMTS_HANDOVER_TO_IWLAN";
        }
        if (o == 2200) {
            return "EVDO_CONNECTION_DENY_BY_GENERAL_OR_NETWORK_BUSY";
        }
        if (o == 2201) {
            return "EVDO_CONNECTION_DENY_BY_BILLING_OR_AUTHENTICATION_FAILURE";
        }
        if (o == 2202) {
            return "EVDO_HDR_CHANGED";
        }
        if (o == 2203) {
            return "EVDO_HDR_EXITED";
        }
        if (o == 2204) {
            return "EVDO_HDR_NO_SESSION";
        }
        if (o == 2205) {
            return "EVDO_USING_GPS_FIX_INSTEAD_OF_HDR_CALL";
        }
        if (o == 2206) {
            return "EVDO_HDR_CONNECTION_SETUP_TIMEOUT";
        }
        if (o == 2207) {
            return "FAILED_TO_ACQUIRE_COLOCATED_HDR";
        }
        if (o == 2208) {
            return "OTASP_COMMIT_IN_PROGRESS";
        }
        if (o == 2209) {
            return "NO_HYBRID_HDR_SERVICE";
        }
        if (o == 2210) {
            return "HDR_NO_LOCK_GRANTED";
        }
        if (o == 2211) {
            return "DBM_OR_SMS_IN_PROGRESS";
        }
        if (o == 2212) {
            return "HDR_FADE";
        }
        if (o == 2213) {
            return "HDR_ACCESS_FAILURE";
        }
        if (o == 2214) {
            return "UNSUPPORTED_1X_PREV";
        }
        if (o == 2215) {
            return "LOCAL_END";
        }
        if (o == 2216) {
            return "NO_SERVICE";
        }
        if (o == 2217) {
            return "FADE";
        }
        if (o == 2218) {
            return "NORMAL_RELEASE";
        }
        if (o == 2219) {
            return "ACCESS_ATTEMPT_ALREADY_IN_PROGRESS";
        }
        if (o == 2220) {
            return "REDIRECTION_OR_HANDOFF_IN_PROGRESS";
        }
        if (o == 2221) {
            return "EMERGENCY_MODE";
        }
        if (o == 2222) {
            return "PHONE_IN_USE";
        }
        if (o == 2223) {
            return "INVALID_MODE";
        }
        if (o == 2224) {
            return "INVALID_SIM_STATE";
        }
        if (o == 2225) {
            return "NO_COLLOCATED_HDR";
        }
        if (o == 2226) {
            return "UE_IS_ENTERING_POWERSAVE_MODE";
        }
        if (o == 2227) {
            return "DUAL_SWITCH";
        }
        if (o == 2228) {
            return "PPP_TIMEOUT";
        }
        if (o == 2229) {
            return "PPP_AUTH_FAILURE";
        }
        if (o == 2230) {
            return "PPP_OPTION_MISMATCH";
        }
        if (o == 2231) {
            return "PPP_PAP_FAILURE";
        }
        if (o == 2232) {
            return "PPP_CHAP_FAILURE";
        }
        if (o == 2233) {
            return "PPP_CLOSE_IN_PROGRESS";
        }
        if (o == 2234) {
            return "LIMITED_TO_IPV4";
        }
        if (o == 2235) {
            return "LIMITED_TO_IPV6";
        }
        if (o == 2236) {
            return "VSNCP_TIMEOUT";
        }
        if (o == 2237) {
            return "VSNCP_GEN_ERROR";
        }
        if (o == 2238) {
            return "VSNCP_APN_UNATHORIZED";
        }
        if (o == 2239) {
            return "VSNCP_PDN_LIMIT_EXCEEDED";
        }
        if (o == 2240) {
            return "VSNCP_NO_PDN_GATEWAY_ADDRESS";
        }
        if (o == 2241) {
            return "VSNCP_PDN_GATEWAY_UNREACHABLE";
        }
        if (o == 2242) {
            return "VSNCP_PDN_GATEWAY_REJECT";
        }
        if (o == 2243) {
            return "VSNCP_INSUFFICIENT_PARAMETERS";
        }
        if (o == 2244) {
            return "VSNCP_RESOURCE_UNAVAILABLE";
        }
        if (o == 2245) {
            return "VSNCP_ADMINISTRATIVELY_PROHIBITED";
        }
        if (o == 2246) {
            return "VSNCP_PDN_ID_IN_USE";
        }
        if (o == 2247) {
            return "VSNCP_SUBSCRIBER_LIMITATION";
        }
        if (o == 2248) {
            return "VSNCP_PDN_EXISTS_FOR_THIS_APN";
        }
        if (o == 2249) {
            return "VSNCP_RECONNECT_NOT_ALLOWED";
        }
        if (o == 2250) {
            return "IPV6_PREFIX_UNAVAILABLE";
        }
        if (o == 2251) {
            return "HANDOFF_PREFERENCE_CHANGED";
        }
        return "0x" + Integer.toHexString(o);
    }

    /*  JADX ERROR: Type inference failed with exception
        jadx.core.utils.exceptions.JadxOverflowException: Type update terminated with stack overflow, arg: (r1v135 ??)
        	at jadx.core.utils.ErrorsCounter.addError(ErrorsCounter.java:56)
        	at jadx.core.utils.ErrorsCounter.error(ErrorsCounter.java:30)
        	at jadx.core.dex.attributes.nodes.NotificationAttrNode.addError(NotificationAttrNode.java:18)
        	at jadx.core.dex.visitors.typeinference.TypeInferenceVisitor.visit(TypeInferenceVisitor.java:114)
        */
    public static final java.lang.String dumpBitfield(int r4) {
        /*
            java.util.ArrayList r0 = new java.util.ArrayList
            r0.<init>()
            r1 = 0
            java.lang.String r2 = "NONE"
            r0.add(r2)
            r2 = r4 & 8
            r3 = 8
            if (r2 != r3) goto L18
            java.lang.String r2 = "OPERATOR_BARRED"
            r0.add(r2)
            r1 = r1 | 8
        L18:
            r2 = r4 & 14
            r3 = 14
            if (r2 != r3) goto L25
            java.lang.String r2 = "NAS_SIGNALLING"
            r0.add(r2)
            r1 = r1 | 14
        L25:
            r2 = r4 & 26
            r3 = 26
            if (r2 != r3) goto L32
            java.lang.String r2 = "INSUFFICIENT_RESOURCES"
            r0.add(r2)
            r1 = r1 | 26
        L32:
            r2 = r4 & 27
            r3 = 27
            if (r2 != r3) goto L3f
            java.lang.String r2 = "MISSING_UKNOWN_APN"
            r0.add(r2)
            r1 = r1 | 27
        L3f:
            r2 = r4 & 28
            r3 = 28
            if (r2 != r3) goto L4c
            java.lang.String r2 = "UNKNOWN_PDP_ADDRESS_TYPE"
            r0.add(r2)
            r1 = r1 | 28
        L4c:
            r2 = r4 & 29
            r3 = 29
            if (r2 != r3) goto L59
            java.lang.String r2 = "USER_AUTHENTICATION"
            r0.add(r2)
            r1 = r1 | 29
        L59:
            r2 = r4 & 30
            r3 = 30
            if (r2 != r3) goto L66
            java.lang.String r2 = "ACTIVATION_REJECT_GGSN"
            r0.add(r2)
            r1 = r1 | 30
        L66:
            r2 = r4 & 31
            r3 = 31
            if (r2 != r3) goto L73
            java.lang.String r2 = "ACTIVATION_REJECT_UNSPECIFIED"
            r0.add(r2)
            r1 = r1 | 31
        L73:
            r2 = r4 & 32
            r3 = 32
            if (r2 != r3) goto L80
            java.lang.String r2 = "SERVICE_OPTION_NOT_SUPPORTED"
            r0.add(r2)
            r1 = r1 | 32
        L80:
            r2 = r4 & 33
            r3 = 33
            if (r2 != r3) goto L8d
            java.lang.String r2 = "SERVICE_OPTION_NOT_SUBSCRIBED"
            r0.add(r2)
            r1 = r1 | 33
        L8d:
            r2 = r4 & 34
            r3 = 34
            if (r2 != r3) goto L9a
            java.lang.String r2 = "SERVICE_OPTION_OUT_OF_ORDER"
            r0.add(r2)
            r1 = r1 | 34
        L9a:
            r2 = r4 & 35
            r3 = 35
            if (r2 != r3) goto La7
            java.lang.String r2 = "NSAPI_IN_USE"
            r0.add(r2)
            r1 = r1 | 35
        La7:
            r2 = r4 & 36
            r3 = 36
            if (r2 != r3) goto Lb4
            java.lang.String r2 = "REGULAR_DEACTIVATION"
            r0.add(r2)
            r1 = r1 | 36
        Lb4:
            r2 = r4 & 37
            r3 = 37
            if (r2 != r3) goto Lc1
            java.lang.String r2 = "QOS_NOT_ACCEPTED"
            r0.add(r2)
            r1 = r1 | 37
        Lc1:
            r2 = r4 & 38
            r3 = 38
            if (r2 != r3) goto Lce
            java.lang.String r2 = "NETWORK_FAILURE"
            r0.add(r2)
            r1 = r1 | 38
        Lce:
            r2 = r4 & 39
            r3 = 39
            if (r2 != r3) goto Ldb
            java.lang.String r2 = "UMTS_REACTIVATION_REQ"
            r0.add(r2)
            r1 = r1 | 39
        Ldb:
            r2 = r4 & 40
            r3 = 40
            if (r2 != r3) goto Le8
            java.lang.String r2 = "FEATURE_NOT_SUPP"
            r0.add(r2)
            r1 = r1 | 40
        Le8:
            r2 = r4 & 41
            r3 = 41
            if (r2 != r3) goto Lf5
            java.lang.String r2 = "TFT_SEMANTIC_ERROR"
            r0.add(r2)
            r1 = r1 | 41
        Lf5:
            r2 = r4 & 42
            r3 = 42
            if (r2 != r3) goto L102
            java.lang.String r2 = "TFT_SYTAX_ERROR"
            r0.add(r2)
            r1 = r1 | 42
        L102:
            r2 = r4 & 43
            r3 = 43
            if (r2 != r3) goto L10f
            java.lang.String r2 = "UNKNOWN_PDP_CONTEXT"
            r0.add(r2)
            r1 = r1 | 43
        L10f:
            r2 = r4 & 44
            r3 = 44
            if (r2 != r3) goto L11c
            java.lang.String r2 = "FILTER_SEMANTIC_ERROR"
            r0.add(r2)
            r1 = r1 | 44
        L11c:
            r2 = r4 & 45
            r3 = 45
            if (r2 != r3) goto L129
            java.lang.String r2 = "FILTER_SYTAX_ERROR"
            r0.add(r2)
            r1 = r1 | 45
        L129:
            r2 = r4 & 46
            r3 = 46
            if (r2 != r3) goto L136
            java.lang.String r2 = "PDP_WITHOUT_ACTIVE_TFT"
            r0.add(r2)
            r1 = r1 | 46
        L136:
            r2 = r4 & 50
            r3 = 50
            if (r2 != r3) goto L143
            java.lang.String r2 = "ONLY_IPV4_ALLOWED"
            r0.add(r2)
            r1 = r1 | 50
        L143:
            r2 = r4 & 51
            r3 = 51
            if (r2 != r3) goto L150
            java.lang.String r2 = "ONLY_IPV6_ALLOWED"
            r0.add(r2)
            r1 = r1 | 51
        L150:
            r2 = r4 & 52
            r3 = 52
            if (r2 != r3) goto L15d
            java.lang.String r2 = "ONLY_SINGLE_BEARER_ALLOWED"
            r0.add(r2)
            r1 = r1 | 52
        L15d:
            r2 = r4 & 53
            r3 = 53
            if (r2 != r3) goto L16a
            java.lang.String r2 = "ESM_INFO_NOT_RECEIVED"
            r0.add(r2)
            r1 = r1 | 53
        L16a:
            r2 = r4 & 54
            r3 = 54
            if (r2 != r3) goto L177
            java.lang.String r2 = "PDN_CONN_DOES_NOT_EXIST"
            r0.add(r2)
            r1 = r1 | 54
        L177:
            r2 = r4 & 55
            r3 = 55
            if (r2 != r3) goto L184
            java.lang.String r2 = "MULTI_CONN_TO_SAME_PDN_NOT_ALLOWED"
            r0.add(r2)
            r1 = r1 | 55
        L184:
            r2 = r4 & 65
            r3 = 65
            if (r2 != r3) goto L191
            java.lang.String r2 = "MAX_ACTIVE_PDP_CONTEXT_REACHED"
            r0.add(r2)
            r1 = r1 | 65
        L191:
            r2 = r4 & 66
            r3 = 66
            if (r2 != r3) goto L19e
            java.lang.String r2 = "UNSUPPORTED_APN_IN_CURRENT_PLMN"
            r0.add(r2)
            r1 = r1 | 66
        L19e:
            r2 = r4 & 81
            r3 = 81
            if (r2 != r3) goto L1ab
            java.lang.String r2 = "INVALID_TRANSACTION_ID"
            r0.add(r2)
            r1 = r1 | 81
        L1ab:
            r2 = r4 & 95
            r3 = 95
            if (r2 != r3) goto L1b8
            java.lang.String r2 = "MESSAGE_INCORRECT_SEMANTIC"
            r0.add(r2)
            r1 = r1 | 95
        L1b8:
            r2 = r4 & 96
            r3 = 96
            if (r2 != r3) goto L1c5
            java.lang.String r2 = "INVALID_MANDATORY_INFO"
            r0.add(r2)
            r1 = r1 | 96
        L1c5:
            r2 = r4 & 97
            r3 = 97
            if (r2 != r3) goto L1d2
            java.lang.String r2 = "MESSAGE_TYPE_UNSUPPORTED"
            r0.add(r2)
            r1 = r1 | 97
        L1d2:
            r2 = r4 & 98
            r3 = 98
            if (r2 != r3) goto L1df
            java.lang.String r2 = "MSG_TYPE_NONCOMPATIBLE_STATE"
            r0.add(r2)
            r1 = r1 | 98
        L1df:
            r2 = r4 & 99
            r3 = 99
            if (r2 != r3) goto L1ec
            java.lang.String r2 = "UNKNOWN_INFO_ELEMENT"
            r0.add(r2)
            r1 = r1 | 99
        L1ec:
            r2 = r4 & 100
            r3 = 100
            if (r2 != r3) goto L1f9
            java.lang.String r2 = "CONDITIONAL_IE_ERROR"
            r0.add(r2)
            r1 = r1 | 100
        L1f9:
            r2 = r4 & 101(0x65, float:1.42E-43)
            r3 = 101(0x65, float:1.42E-43)
            if (r2 != r3) goto L206
            java.lang.String r2 = "MSG_AND_PROTOCOL_STATE_UNCOMPATIBLE"
            r0.add(r2)
            r1 = r1 | 101(0x65, float:1.42E-43)
        L206:
            r2 = r4 & 111(0x6f, float:1.56E-43)
            r3 = 111(0x6f, float:1.56E-43)
            if (r2 != r3) goto L213
            java.lang.String r2 = "PROTOCOL_ERRORS"
            r0.add(r2)
            r1 = r1 | 111(0x6f, float:1.56E-43)
        L213:
            r2 = r4 & 112(0x70, float:1.57E-43)
            r3 = 112(0x70, float:1.57E-43)
            if (r2 != r3) goto L220
            java.lang.String r2 = "APN_TYPE_CONFLICT"
            r0.add(r2)
            r1 = r1 | 112(0x70, float:1.57E-43)
        L220:
            r2 = r4 & 113(0x71, float:1.58E-43)
            r3 = 113(0x71, float:1.58E-43)
            if (r2 != r3) goto L22d
            java.lang.String r2 = "INVALID_PCSCF_ADDR"
            r0.add(r2)
            r1 = r1 | 113(0x71, float:1.58E-43)
        L22d:
            r2 = r4 & 114(0x72, float:1.6E-43)
            r3 = 114(0x72, float:1.6E-43)
            if (r2 != r3) goto L23a
            java.lang.String r2 = "INTERNAL_CALL_PREEMPT_BY_HIGH_PRIO_APN"
            r0.add(r2)
            r1 = r1 | 114(0x72, float:1.6E-43)
        L23a:
            r2 = r4 & 115(0x73, float:1.61E-43)
            r3 = 115(0x73, float:1.61E-43)
            if (r2 != r3) goto L247
            java.lang.String r2 = "EMM_ACCESS_BARRED"
            r0.add(r2)
            r1 = r1 | 115(0x73, float:1.61E-43)
        L247:
            r2 = r4 & 116(0x74, float:1.63E-43)
            r3 = 116(0x74, float:1.63E-43)
            if (r2 != r3) goto L254
            java.lang.String r2 = "EMERGENCY_IFACE_ONLY"
            r0.add(r2)
            r1 = r1 | 116(0x74, float:1.63E-43)
        L254:
            r2 = r4 & 117(0x75, float:1.64E-43)
            r3 = 117(0x75, float:1.64E-43)
            if (r2 != r3) goto L261
            java.lang.String r2 = "IFACE_MISMATCH"
            r0.add(r2)
            r1 = r1 | 117(0x75, float:1.64E-43)
        L261:
            r2 = r4 & 118(0x76, float:1.65E-43)
            r3 = 118(0x76, float:1.65E-43)
            if (r2 != r3) goto L26e
            java.lang.String r2 = "COMPANION_IFACE_IN_USE"
            r0.add(r2)
            r1 = r1 | 118(0x76, float:1.65E-43)
        L26e:
            r2 = r4 & 119(0x77, float:1.67E-43)
            r3 = 119(0x77, float:1.67E-43)
            if (r2 != r3) goto L27b
            java.lang.String r2 = "IP_ADDRESS_MISMATCH"
            r0.add(r2)
            r1 = r1 | 119(0x77, float:1.67E-43)
        L27b:
            r2 = r4 & 120(0x78, float:1.68E-43)
            r3 = 120(0x78, float:1.68E-43)
            if (r2 != r3) goto L288
            java.lang.String r2 = "IFACE_AND_POL_FAMILY_MISMATCH"
            r0.add(r2)
            r1 = r1 | 120(0x78, float:1.68E-43)
        L288:
            r2 = r4 & 121(0x79, float:1.7E-43)
            r3 = 121(0x79, float:1.7E-43)
            if (r2 != r3) goto L295
            java.lang.String r2 = "EMM_ACCESS_BARRED_INFINITE_RETRY"
            r0.add(r2)
            r1 = r1 | 121(0x79, float:1.7E-43)
        L295:
            r2 = r4 & 122(0x7a, float:1.71E-43)
            r3 = 122(0x7a, float:1.71E-43)
            if (r2 != r3) goto L2a2
            java.lang.String r2 = "AUTH_FAILURE_ON_EMERGENCY_CALL"
            r0.add(r2)
            r1 = r1 | 122(0x7a, float:1.71E-43)
        L2a2:
            r2 = r4 & 4097(0x1001, float:5.741E-42)
            r3 = 4097(0x1001, float:5.741E-42)
            if (r2 != r3) goto L2af
            java.lang.String r2 = "OEM_DCFAILCAUSE_1"
            r0.add(r2)
            r1 = r1 | 4097(0x1001, float:5.741E-42)
        L2af:
            r2 = r4 & 4098(0x1002, float:5.743E-42)
            r3 = 4098(0x1002, float:5.743E-42)
            if (r2 != r3) goto L2bc
            java.lang.String r2 = "OEM_DCFAILCAUSE_2"
            r0.add(r2)
            r1 = r1 | 4098(0x1002, float:5.743E-42)
        L2bc:
            r2 = r4 & 4099(0x1003, float:5.744E-42)
            r3 = 4099(0x1003, float:5.744E-42)
            if (r2 != r3) goto L2c9
            java.lang.String r2 = "OEM_DCFAILCAUSE_3"
            r0.add(r2)
            r1 = r1 | 4099(0x1003, float:5.744E-42)
        L2c9:
            r2 = r4 & 4100(0x1004, float:5.745E-42)
            r3 = 4100(0x1004, float:5.745E-42)
            if (r2 != r3) goto L2d6
            java.lang.String r2 = "OEM_DCFAILCAUSE_4"
            r0.add(r2)
            r1 = r1 | 4100(0x1004, float:5.745E-42)
        L2d6:
            r2 = r4 & 4101(0x1005, float:5.747E-42)
            r3 = 4101(0x1005, float:5.747E-42)
            if (r2 != r3) goto L2e3
            java.lang.String r2 = "OEM_DCFAILCAUSE_5"
            r0.add(r2)
            r1 = r1 | 4101(0x1005, float:5.747E-42)
        L2e3:
            r2 = r4 & 4102(0x1006, float:5.748E-42)
            r3 = 4102(0x1006, float:5.748E-42)
            if (r2 != r3) goto L2f0
            java.lang.String r2 = "OEM_DCFAILCAUSE_6"
            r0.add(r2)
            r1 = r1 | 4102(0x1006, float:5.748E-42)
        L2f0:
            r2 = r4 & 4103(0x1007, float:5.75E-42)
            r3 = 4103(0x1007, float:5.75E-42)
            if (r2 != r3) goto L2fd
            java.lang.String r2 = "OEM_DCFAILCAUSE_7"
            r0.add(r2)
            r1 = r1 | 4103(0x1007, float:5.75E-42)
        L2fd:
            r2 = r4 & 4104(0x1008, float:5.751E-42)
            r3 = 4104(0x1008, float:5.751E-42)
            if (r2 != r3) goto L30a
            java.lang.String r2 = "OEM_DCFAILCAUSE_8"
            r0.add(r2)
            r1 = r1 | 4104(0x1008, float:5.751E-42)
        L30a:
            r2 = r4 & 4105(0x1009, float:5.752E-42)
            r3 = 4105(0x1009, float:5.752E-42)
            if (r2 != r3) goto L317
            java.lang.String r2 = "OEM_DCFAILCAUSE_9"
            r0.add(r2)
            r1 = r1 | 4105(0x1009, float:5.752E-42)
        L317:
            r2 = r4 & 4106(0x100a, float:5.754E-42)
            r3 = 4106(0x100a, float:5.754E-42)
            if (r2 != r3) goto L324
            java.lang.String r2 = "OEM_DCFAILCAUSE_10"
            r0.add(r2)
            r1 = r1 | 4106(0x100a, float:5.754E-42)
        L324:
            r2 = r4 & 4107(0x100b, float:5.755E-42)
            r3 = 4107(0x100b, float:5.755E-42)
            if (r2 != r3) goto L331
            java.lang.String r2 = "OEM_DCFAILCAUSE_11"
            r0.add(r2)
            r1 = r1 | 4107(0x100b, float:5.755E-42)
        L331:
            r2 = r4 & 4108(0x100c, float:5.757E-42)
            r3 = 4108(0x100c, float:5.757E-42)
            if (r2 != r3) goto L33e
            java.lang.String r2 = "OEM_DCFAILCAUSE_12"
            r0.add(r2)
            r1 = r1 | 4108(0x100c, float:5.757E-42)
        L33e:
            r2 = r4 & 4109(0x100d, float:5.758E-42)
            r3 = 4109(0x100d, float:5.758E-42)
            if (r2 != r3) goto L34b
            java.lang.String r2 = "OEM_DCFAILCAUSE_13"
            r0.add(r2)
            r1 = r1 | 4109(0x100d, float:5.758E-42)
        L34b:
            r2 = r4 & 4110(0x100e, float:5.76E-42)
            r3 = 4110(0x100e, float:5.76E-42)
            if (r2 != r3) goto L358
            java.lang.String r2 = "OEM_DCFAILCAUSE_14"
            r0.add(r2)
            r1 = r1 | 4110(0x100e, float:5.76E-42)
        L358:
            r2 = r4 & 4111(0x100f, float:5.761E-42)
            r3 = 4111(0x100f, float:5.761E-42)
            if (r2 != r3) goto L365
            java.lang.String r2 = "OEM_DCFAILCAUSE_15"
            r0.add(r2)
            r1 = r1 | 4111(0x100f, float:5.761E-42)
        L365:
            r2 = r4 & (-1)
            r3 = -1
            if (r2 != r3) goto L371
            java.lang.String r2 = "VOICE_REGISTRATION_FAIL"
            r0.add(r2)
            r1 = r1 | (-1)
        L371:
            r2 = r4 & (-2)
            r3 = -2
            if (r2 != r3) goto L37d
            java.lang.String r2 = "DATA_REGISTRATION_FAIL"
            r0.add(r2)
            r1 = r1 | (-2)
        L37d:
            r2 = r4 & (-3)
            r3 = -3
            if (r2 != r3) goto L389
            java.lang.String r2 = "SIGNAL_LOST"
            r0.add(r2)
            r1 = r1 | (-3)
        L389:
            r2 = r4 & (-4)
            r3 = -4
            if (r2 != r3) goto L395
            java.lang.String r2 = "PREF_RADIO_TECH_CHANGED"
            r0.add(r2)
            r1 = r1 | (-4)
        L395:
            r2 = r4 & (-5)
            r3 = -5
            if (r2 != r3) goto L3a1
            java.lang.String r2 = "RADIO_POWER_OFF"
            r0.add(r2)
            r1 = r1 | (-5)
        L3a1:
            r2 = r4 & (-6)
            r3 = -6
            if (r2 != r3) goto L3ad
            java.lang.String r2 = "TETHERED_CALL_ACTIVE"
            r0.add(r2)
            r1 = r1 | (-6)
        L3ad:
            r2 = 65535(0xffff, float:9.1834E-41)
            r2 = r2 & r4
            r3 = 65535(0xffff, float:9.1834E-41)
            if (r2 != r3) goto L3bf
            java.lang.String r2 = "ERROR_UNSPECIFIED"
            r0.add(r2)
            r2 = 65535(0xffff, float:9.1834E-41)
            r1 = r1 | r2
        L3bf:
            r2 = r4 & 25
            r3 = 25
            if (r2 != r3) goto L3cc
            java.lang.String r2 = "LLC_SNDCP"
            r0.add(r2)
            r1 = r1 | 25
        L3cc:
            r2 = r4 & 48
            r3 = 48
            if (r2 != r3) goto L3d9
            java.lang.String r2 = "ACTIVATION_REJECTED_BCM_VIOLATION"
            r0.add(r2)
            r1 = r1 | 48
        L3d9:
            r2 = r4 & 56
            r3 = 56
            if (r2 != r3) goto L3e6
            java.lang.String r2 = "COLLISION_WITH_NETWORK_INITIATED_REQUEST"
            r0.add(r2)
            r1 = r1 | 56
        L3e6:
            r2 = r4 & 57
            r3 = 57
            if (r2 != r3) goto L3f3
            java.lang.String r2 = "ONLY_IPV4V6_ALLOWED"
            r0.add(r2)
            r1 = r1 | 57
        L3f3:
            r2 = r4 & 58
            r3 = 58
            if (r2 != r3) goto L400
            java.lang.String r2 = "ONLY_NON_IP_ALLOWED"
            r0.add(r2)
            r1 = r1 | 58
        L400:
            r2 = r4 & 59
            r3 = 59
            if (r2 != r3) goto L40d
            java.lang.String r2 = "UNSUPPORTED_QCI_VALUE"
            r0.add(r2)
            r1 = r1 | 59
        L40d:
            r2 = r4 & 60
            r3 = 60
            if (r2 != r3) goto L41a
            java.lang.String r2 = "BEARER_HANDLING_NOT_SUPPORTED"
            r0.add(r2)
            r1 = r1 | 60
        L41a:
            r2 = r4 & 123(0x7b, float:1.72E-43)
            r3 = 123(0x7b, float:1.72E-43)
            if (r2 != r3) goto L427
            java.lang.String r2 = "INVALID_DNS_ADDR"
            r0.add(r2)
            r1 = r1 | 123(0x7b, float:1.72E-43)
        L427:
            r2 = r4 & 124(0x7c, float:1.74E-43)
            r3 = 124(0x7c, float:1.74E-43)
            if (r2 != r3) goto L434
            java.lang.String r2 = "INVALID_PCSCF_OR_DNS_ADDRESS"
            r0.add(r2)
            r1 = r1 | 124(0x7c, float:1.74E-43)
        L434:
            r2 = r4 & 127(0x7f, float:1.78E-43)
            r3 = 127(0x7f, float:1.78E-43)
            if (r2 != r3) goto L441
            java.lang.String r2 = "CALL_PREEMPT_BY_EMERGENCY_APN"
            r0.add(r2)
            r1 = r1 | 127(0x7f, float:1.78E-43)
        L441:
            r2 = r4 & 128(0x80, float:1.794E-43)
            r3 = 128(0x80, float:1.794E-43)
            if (r2 != r3) goto L44e
            java.lang.String r2 = "UE_INITIATED_DETACH_OR_DISCONNECT"
            r0.add(r2)
            r1 = r1 | 128(0x80, float:1.794E-43)
        L44e:
            r2 = r4 & 2000(0x7d0, float:2.803E-42)
            r3 = 2000(0x7d0, float:2.803E-42)
            if (r2 != r3) goto L45b
            java.lang.String r2 = "MIP_FA_REASON_UNSPECIFIED"
            r0.add(r2)
            r1 = r1 | 2000(0x7d0, float:2.803E-42)
        L45b:
            r2 = r4 & 2001(0x7d1, float:2.804E-42)
            r3 = 2001(0x7d1, float:2.804E-42)
            if (r2 != r3) goto L468
            java.lang.String r2 = "MIP_FA_ADMIN_PROHIBITED"
            r0.add(r2)
            r1 = r1 | 2001(0x7d1, float:2.804E-42)
        L468:
            r2 = r4 & 2002(0x7d2, float:2.805E-42)
            r3 = 2002(0x7d2, float:2.805E-42)
            if (r2 != r3) goto L475
            java.lang.String r2 = "MIP_FA_INSUFFICIENT_RESOURCES"
            r0.add(r2)
            r1 = r1 | 2002(0x7d2, float:2.805E-42)
        L475:
            r2 = r4 & 2003(0x7d3, float:2.807E-42)
            r3 = 2003(0x7d3, float:2.807E-42)
            if (r2 != r3) goto L482
            java.lang.String r2 = "MIP_FA_MOBILE_NODE_AUTHENTICATION_FAILURE"
            r0.add(r2)
            r1 = r1 | 2003(0x7d3, float:2.807E-42)
        L482:
            r2 = r4 & 2004(0x7d4, float:2.808E-42)
            r3 = 2004(0x7d4, float:2.808E-42)
            if (r2 != r3) goto L48f
            java.lang.String r2 = "MIP_FA_HOME_AGENT_AUTHENTICATION_FAILURE"
            r0.add(r2)
            r1 = r1 | 2004(0x7d4, float:2.808E-42)
        L48f:
            r2 = r4 & 2005(0x7d5, float:2.81E-42)
            r3 = 2005(0x7d5, float:2.81E-42)
            if (r2 != r3) goto L49c
            java.lang.String r2 = "MIP_FA_REQUESTED_LIFETIME_TOO_LONG"
            r0.add(r2)
            r1 = r1 | 2005(0x7d5, float:2.81E-42)
        L49c:
            r2 = r4 & 2006(0x7d6, float:2.811E-42)
            r3 = 2006(0x7d6, float:2.811E-42)
            if (r2 != r3) goto L4a9
            java.lang.String r2 = "MIP_FA_MALFORMED_REQUEST"
            r0.add(r2)
            r1 = r1 | 2006(0x7d6, float:2.811E-42)
        L4a9:
            r2 = r4 & 2007(0x7d7, float:2.812E-42)
            r3 = 2007(0x7d7, float:2.812E-42)
            if (r2 != r3) goto L4b6
            java.lang.String r2 = "MIP_FA_MALFORMED_REPLY"
            r0.add(r2)
            r1 = r1 | 2007(0x7d7, float:2.812E-42)
        L4b6:
            r2 = r4 & 2008(0x7d8, float:2.814E-42)
            r3 = 2008(0x7d8, float:2.814E-42)
            if (r2 != r3) goto L4c3
            java.lang.String r2 = "MIP_FA_ENCAPSULATION_UNAVAILABLE"
            r0.add(r2)
            r1 = r1 | 2008(0x7d8, float:2.814E-42)
        L4c3:
            r2 = r4 & 2009(0x7d9, float:2.815E-42)
            r3 = 2009(0x7d9, float:2.815E-42)
            if (r2 != r3) goto L4d0
            java.lang.String r2 = "MIP_FA_VJ_HEADER_COMPRESSION_UNAVAILABLE"
            r0.add(r2)
            r1 = r1 | 2009(0x7d9, float:2.815E-42)
        L4d0:
            r2 = r4 & 2010(0x7da, float:2.817E-42)
            r3 = 2010(0x7da, float:2.817E-42)
            if (r2 != r3) goto L4dd
            java.lang.String r2 = "MIP_FA_REVERSE_TUNNEL_UNAVAILABLE"
            r0.add(r2)
            r1 = r1 | 2010(0x7da, float:2.817E-42)
        L4dd:
            r2 = r4 & 2011(0x7db, float:2.818E-42)
            r3 = 2011(0x7db, float:2.818E-42)
            if (r2 != r3) goto L4ea
            java.lang.String r2 = "MIP_FA_REVERSE_TUNNEL_IS_MANDATORY"
            r0.add(r2)
            r1 = r1 | 2011(0x7db, float:2.818E-42)
        L4ea:
            r2 = r4 & 2012(0x7dc, float:2.82E-42)
            r3 = 2012(0x7dc, float:2.82E-42)
            if (r2 != r3) goto L4f7
            java.lang.String r2 = "MIP_FA_DELIVERY_STYLE_NOT_SUPPORTED"
            r0.add(r2)
            r1 = r1 | 2012(0x7dc, float:2.82E-42)
        L4f7:
            r2 = r4 & 2013(0x7dd, float:2.821E-42)
            r3 = 2013(0x7dd, float:2.821E-42)
            if (r2 != r3) goto L504
            java.lang.String r2 = "MIP_FA_MISSING_NAI"
            r0.add(r2)
            r1 = r1 | 2013(0x7dd, float:2.821E-42)
        L504:
            r2 = r4 & 2014(0x7de, float:2.822E-42)
            r3 = 2014(0x7de, float:2.822E-42)
            if (r2 != r3) goto L511
            java.lang.String r2 = "MIP_FA_MISSING_HOME_AGENT"
            r0.add(r2)
            r1 = r1 | 2014(0x7de, float:2.822E-42)
        L511:
            r2 = r4 & 2015(0x7df, float:2.824E-42)
            r3 = 2015(0x7df, float:2.824E-42)
            if (r2 != r3) goto L51e
            java.lang.String r2 = "MIP_FA_MISSING_HOME_ADDRESS"
            r0.add(r2)
            r1 = r1 | 2015(0x7df, float:2.824E-42)
        L51e:
            r2 = r4 & 2016(0x7e0, float:2.825E-42)
            r3 = 2016(0x7e0, float:2.825E-42)
            if (r2 != r3) goto L52b
            java.lang.String r2 = "MIP_FA_UNKNOWN_CHALLENGE"
            r0.add(r2)
            r1 = r1 | 2016(0x7e0, float:2.825E-42)
        L52b:
            r2 = r4 & 2017(0x7e1, float:2.826E-42)
            r3 = 2017(0x7e1, float:2.826E-42)
            if (r2 != r3) goto L538
            java.lang.String r2 = "MIP_FA_MISSING_CHALLENGE"
            r0.add(r2)
            r1 = r1 | 2017(0x7e1, float:2.826E-42)
        L538:
            r2 = r4 & 2018(0x7e2, float:2.828E-42)
            r3 = 2018(0x7e2, float:2.828E-42)
            if (r2 != r3) goto L545
            java.lang.String r2 = "MIP_FA_STALE_CHALLENGE"
            r0.add(r2)
            r1 = r1 | 2018(0x7e2, float:2.828E-42)
        L545:
            r2 = r4 & 2019(0x7e3, float:2.829E-42)
            r3 = 2019(0x7e3, float:2.829E-42)
            if (r2 != r3) goto L552
            java.lang.String r2 = "MIP_HA_REASON_UNSPECIFIED"
            r0.add(r2)
            r1 = r1 | 2019(0x7e3, float:2.829E-42)
        L552:
            r2 = r4 & 2020(0x7e4, float:2.83E-42)
            r3 = 2020(0x7e4, float:2.83E-42)
            if (r2 != r3) goto L55f
            java.lang.String r2 = "MIP_HA_ADMIN_PROHIBITED"
            r0.add(r2)
            r1 = r1 | 2020(0x7e4, float:2.83E-42)
        L55f:
            r2 = r4 & 2021(0x7e5, float:2.832E-42)
            r3 = 2021(0x7e5, float:2.832E-42)
            if (r2 != r3) goto L56c
            java.lang.String r2 = "MIP_HA_INSUFFICIENT_RESOURCES"
            r0.add(r2)
            r1 = r1 | 2021(0x7e5, float:2.832E-42)
        L56c:
            r2 = r4 & 2022(0x7e6, float:2.833E-42)
            r3 = 2022(0x7e6, float:2.833E-42)
            if (r2 != r3) goto L579
            java.lang.String r2 = "MIP_HA_MOBILE_NODE_AUTHENTICATION_FAILURE"
            r0.add(r2)
            r1 = r1 | 2022(0x7e6, float:2.833E-42)
        L579:
            r2 = r4 & 2023(0x7e7, float:2.835E-42)
            r3 = 2023(0x7e7, float:2.835E-42)
            if (r2 != r3) goto L586
            java.lang.String r2 = "MIP_HA_FOREIGN_AGENT_AUTHENTICATION_FAILURE"
            r0.add(r2)
            r1 = r1 | 2023(0x7e7, float:2.835E-42)
        L586:
            r2 = r4 & 2024(0x7e8, float:2.836E-42)
            r3 = 2024(0x7e8, float:2.836E-42)
            if (r2 != r3) goto L593
            java.lang.String r2 = "MIP_HA_REGISTRATION_ID_MISMATCH"
            r0.add(r2)
            r1 = r1 | 2024(0x7e8, float:2.836E-42)
        L593:
            r2 = r4 & 2025(0x7e9, float:2.838E-42)
            r3 = 2025(0x7e9, float:2.838E-42)
            if (r2 != r3) goto L5a0
            java.lang.String r2 = "MIP_HA_MALFORMED_REQUEST"
            r0.add(r2)
            r1 = r1 | 2025(0x7e9, float:2.838E-42)
        L5a0:
            r2 = r4 & 2026(0x7ea, float:2.839E-42)
            r3 = 2026(0x7ea, float:2.839E-42)
            if (r2 != r3) goto L5ad
            java.lang.String r2 = "MIP_HA_UNKNOWN_HOME_AGENT_ADDRESS"
            r0.add(r2)
            r1 = r1 | 2026(0x7ea, float:2.839E-42)
        L5ad:
            r2 = r4 & 2027(0x7eb, float:2.84E-42)
            r3 = 2027(0x7eb, float:2.84E-42)
            if (r2 != r3) goto L5ba
            java.lang.String r2 = "MIP_HA_REVERSE_TUNNEL_UNAVAILABLE"
            r0.add(r2)
            r1 = r1 | 2027(0x7eb, float:2.84E-42)
        L5ba:
            r2 = r4 & 2028(0x7ec, float:2.842E-42)
            r3 = 2028(0x7ec, float:2.842E-42)
            if (r2 != r3) goto L5c7
            java.lang.String r2 = "MIP_HA_REVERSE_TUNNEL_IS_MANDATORY"
            r0.add(r2)
            r1 = r1 | 2028(0x7ec, float:2.842E-42)
        L5c7:
            r2 = r4 & 2029(0x7ed, float:2.843E-42)
            r3 = 2029(0x7ed, float:2.843E-42)
            if (r2 != r3) goto L5d4
            java.lang.String r2 = "MIP_HA_ENCAPSULATION_UNAVAILABLE"
            r0.add(r2)
            r1 = r1 | 2029(0x7ed, float:2.843E-42)
        L5d4:
            r2 = r4 & 2030(0x7ee, float:2.845E-42)
            r3 = 2030(0x7ee, float:2.845E-42)
            if (r2 != r3) goto L5e1
            java.lang.String r2 = "CLOSE_IN_PROGRESS"
            r0.add(r2)
            r1 = r1 | 2030(0x7ee, float:2.845E-42)
        L5e1:
            r2 = r4 & 2031(0x7ef, float:2.846E-42)
            r3 = 2031(0x7ef, float:2.846E-42)
            if (r2 != r3) goto L5ee
            java.lang.String r2 = "NETWORK_INITIATED_TERMINATION"
            r0.add(r2)
            r1 = r1 | 2031(0x7ef, float:2.846E-42)
        L5ee:
            r2 = r4 & 2032(0x7f0, float:2.847E-42)
            r3 = 2032(0x7f0, float:2.847E-42)
            if (r2 != r3) goto L5fb
            java.lang.String r2 = "MODEM_APP_PREEMPTED"
            r0.add(r2)
            r1 = r1 | 2032(0x7f0, float:2.847E-42)
        L5fb:
            r2 = r4 & 2033(0x7f1, float:2.849E-42)
            r3 = 2033(0x7f1, float:2.849E-42)
            if (r2 != r3) goto L608
            java.lang.String r2 = "PDN_IPV4_CALL_DISALLOWED"
            r0.add(r2)
            r1 = r1 | 2033(0x7f1, float:2.849E-42)
        L608:
            r2 = r4 & 2034(0x7f2, float:2.85E-42)
            r3 = 2034(0x7f2, float:2.85E-42)
            if (r2 != r3) goto L615
            java.lang.String r2 = "PDN_IPV4_CALL_THROTTLED"
            r0.add(r2)
            r1 = r1 | 2034(0x7f2, float:2.85E-42)
        L615:
            r2 = r4 & 2035(0x7f3, float:2.852E-42)
            r3 = 2035(0x7f3, float:2.852E-42)
            if (r2 != r3) goto L622
            java.lang.String r2 = "PDN_IPV6_CALL_DISALLOWED"
            r0.add(r2)
            r1 = r1 | 2035(0x7f3, float:2.852E-42)
        L622:
            r2 = r4 & 2036(0x7f4, float:2.853E-42)
            r3 = 2036(0x7f4, float:2.853E-42)
            if (r2 != r3) goto L62f
            java.lang.String r2 = "PDN_IPV6_CALL_THROTTLED"
            r0.add(r2)
            r1 = r1 | 2036(0x7f4, float:2.853E-42)
        L62f:
            r2 = r4 & 2037(0x7f5, float:2.854E-42)
            r3 = 2037(0x7f5, float:2.854E-42)
            if (r2 != r3) goto L63c
            java.lang.String r2 = "MODEM_RESTART"
            r0.add(r2)
            r1 = r1 | 2037(0x7f5, float:2.854E-42)
        L63c:
            r2 = r4 & 2038(0x7f6, float:2.856E-42)
            r3 = 2038(0x7f6, float:2.856E-42)
            if (r2 != r3) goto L649
            java.lang.String r2 = "PDP_PPP_NOT_SUPPORTED"
            r0.add(r2)
            r1 = r1 | 2038(0x7f6, float:2.856E-42)
        L649:
            r2 = r4 & 2039(0x7f7, float:2.857E-42)
            r3 = 2039(0x7f7, float:2.857E-42)
            if (r2 != r3) goto L656
            java.lang.String r2 = "UNPREFERRED_RAT"
            r0.add(r2)
            r1 = r1 | 2039(0x7f7, float:2.857E-42)
        L656:
            r2 = r4 & 2040(0x7f8, float:2.859E-42)
            r3 = 2040(0x7f8, float:2.859E-42)
            if (r2 != r3) goto L663
            java.lang.String r2 = "PHYSICAL_LINK_CLOSE_IN_PROGRESS"
            r0.add(r2)
            r1 = r1 | 2040(0x7f8, float:2.859E-42)
        L663:
            r2 = r4 & 2041(0x7f9, float:2.86E-42)
            r3 = 2041(0x7f9, float:2.86E-42)
            if (r2 != r3) goto L670
            java.lang.String r2 = "APN_PENDING_HANDOVER"
            r0.add(r2)
            r1 = r1 | 2041(0x7f9, float:2.86E-42)
        L670:
            r2 = r4 & 2042(0x7fa, float:2.861E-42)
            r3 = 2042(0x7fa, float:2.861E-42)
            if (r2 != r3) goto L67d
            java.lang.String r2 = "PROFILE_BEARER_INCOMPATIBLE"
            r0.add(r2)
            r1 = r1 | 2042(0x7fa, float:2.861E-42)
        L67d:
            r2 = r4 & 2043(0x7fb, float:2.863E-42)
            r3 = 2043(0x7fb, float:2.863E-42)
            if (r2 != r3) goto L68a
            java.lang.String r2 = "SIM_CARD_CHANGED"
            r0.add(r2)
            r1 = r1 | 2043(0x7fb, float:2.863E-42)
        L68a:
            r2 = r4 & 2044(0x7fc, float:2.864E-42)
            r3 = 2044(0x7fc, float:2.864E-42)
            if (r2 != r3) goto L697
            java.lang.String r2 = "LOW_POWER_MODE_OR_POWERING_DOWN"
            r0.add(r2)
            r1 = r1 | 2044(0x7fc, float:2.864E-42)
        L697:
            r2 = r4 & 2045(0x7fd, float:2.866E-42)
            r3 = 2045(0x7fd, float:2.866E-42)
            if (r2 != r3) goto L6a4
            java.lang.String r2 = "APN_DISABLED"
            r0.add(r2)
            r1 = r1 | 2045(0x7fd, float:2.866E-42)
        L6a4:
            r2 = r4 & 2046(0x7fe, float:2.867E-42)
            r3 = 2046(0x7fe, float:2.867E-42)
            if (r2 != r3) goto L6b1
            java.lang.String r2 = "MAX_PPP_INACTIVITY_TIMER_EXPIRED"
            r0.add(r2)
            r1 = r1 | 2046(0x7fe, float:2.867E-42)
        L6b1:
            r2 = r4 & 2047(0x7ff, float:2.868E-42)
            r3 = 2047(0x7ff, float:2.868E-42)
            if (r2 != r3) goto L6be
            java.lang.String r2 = "IPV6_ADDRESS_TRANSFER_FAILED"
            r0.add(r2)
            r1 = r1 | 2047(0x7ff, float:2.868E-42)
        L6be:
            r2 = r4 & 2048(0x800, float:2.87E-42)
            r3 = 2048(0x800, float:2.87E-42)
            if (r2 != r3) goto L6cb
            java.lang.String r2 = "TRAT_SWAP_FAILED"
            r0.add(r2)
            r1 = r1 | 2048(0x800, float:2.87E-42)
        L6cb:
            r2 = r4 & 2049(0x801, float:2.871E-42)
            r3 = 2049(0x801, float:2.871E-42)
            if (r2 != r3) goto L6d8
            java.lang.String r2 = "EHRPD_TO_HRPD_FALLBACK"
            r0.add(r2)
            r1 = r1 | 2049(0x801, float:2.871E-42)
        L6d8:
            r2 = r4 & 2050(0x802, float:2.873E-42)
            r3 = 2050(0x802, float:2.873E-42)
            if (r2 != r3) goto L6e5
            java.lang.String r2 = "MIP_CONFIG_FAILURE"
            r0.add(r2)
            r1 = r1 | 2050(0x802, float:2.873E-42)
        L6e5:
            r2 = r4 & 2051(0x803, float:2.874E-42)
            r3 = 2051(0x803, float:2.874E-42)
            if (r2 != r3) goto L6f2
            java.lang.String r2 = "PDN_INACTIVITY_TIMER_EXPIRED"
            r0.add(r2)
            r1 = r1 | 2051(0x803, float:2.874E-42)
        L6f2:
            r2 = r4 & 2052(0x804, float:2.875E-42)
            r3 = 2052(0x804, float:2.875E-42)
            if (r2 != r3) goto L6ff
            java.lang.String r2 = "MAX_IPV4_CONNECTIONS"
            r0.add(r2)
            r1 = r1 | 2052(0x804, float:2.875E-42)
        L6ff:
            r2 = r4 & 2053(0x805, float:2.877E-42)
            r3 = 2053(0x805, float:2.877E-42)
            if (r2 != r3) goto L70c
            java.lang.String r2 = "MAX_IPV6_CONNECTIONS"
            r0.add(r2)
            r1 = r1 | 2053(0x805, float:2.877E-42)
        L70c:
            r2 = r4 & 2054(0x806, float:2.878E-42)
            r3 = 2054(0x806, float:2.878E-42)
            if (r2 != r3) goto L719
            java.lang.String r2 = "APN_MISMATCH"
            r0.add(r2)
            r1 = r1 | 2054(0x806, float:2.878E-42)
        L719:
            r2 = r4 & 2055(0x807, float:2.88E-42)
            r3 = 2055(0x807, float:2.88E-42)
            if (r2 != r3) goto L726
            java.lang.String r2 = "IP_VERSION_MISMATCH"
            r0.add(r2)
            r1 = r1 | 2055(0x807, float:2.88E-42)
        L726:
            r2 = r4 & 2056(0x808, float:2.881E-42)
            r3 = 2056(0x808, float:2.881E-42)
            if (r2 != r3) goto L733
            java.lang.String r2 = "DUN_CALL_DISALLOWED"
            r0.add(r2)
            r1 = r1 | 2056(0x808, float:2.881E-42)
        L733:
            r2 = r4 & 2057(0x809, float:2.882E-42)
            r3 = 2057(0x809, float:2.882E-42)
            if (r2 != r3) goto L740
            java.lang.String r2 = "INTERNAL_EPC_NONEPC_TRANSITION"
            r0.add(r2)
            r1 = r1 | 2057(0x809, float:2.882E-42)
        L740:
            r2 = r4 & 2058(0x80a, float:2.884E-42)
            r3 = 2058(0x80a, float:2.884E-42)
            if (r2 != r3) goto L74d
            java.lang.String r2 = "INTERFACE_IN_USE"
            r0.add(r2)
            r1 = r1 | 2058(0x80a, float:2.884E-42)
        L74d:
            r2 = r4 & 2059(0x80b, float:2.885E-42)
            r3 = 2059(0x80b, float:2.885E-42)
            if (r2 != r3) goto L75a
            java.lang.String r2 = "APN_DISALLOWED_ON_ROAMING"
            r0.add(r2)
            r1 = r1 | 2059(0x80b, float:2.885E-42)
        L75a:
            r2 = r4 & 2060(0x80c, float:2.887E-42)
            r3 = 2060(0x80c, float:2.887E-42)
            if (r2 != r3) goto L767
            java.lang.String r2 = "APN_PARAMETERS_CHANGED"
            r0.add(r2)
            r1 = r1 | 2060(0x80c, float:2.887E-42)
        L767:
            r2 = r4 & 2061(0x80d, float:2.888E-42)
            r3 = 2061(0x80d, float:2.888E-42)
            if (r2 != r3) goto L774
            java.lang.String r2 = "NULL_APN_DISALLOWED"
            r0.add(r2)
            r1 = r1 | 2061(0x80d, float:2.888E-42)
        L774:
            r2 = r4 & 2062(0x80e, float:2.89E-42)
            r3 = 2062(0x80e, float:2.89E-42)
            if (r2 != r3) goto L781
            java.lang.String r2 = "THERMAL_MITIGATION"
            r0.add(r2)
            r1 = r1 | 2062(0x80e, float:2.89E-42)
        L781:
            r2 = r4 & 2063(0x80f, float:2.891E-42)
            r3 = 2063(0x80f, float:2.891E-42)
            if (r2 != r3) goto L78e
            java.lang.String r2 = "DATA_SETTINGS_DISABLED"
            r0.add(r2)
            r1 = r1 | 2063(0x80f, float:2.891E-42)
        L78e:
            r2 = r4 & 2064(0x810, float:2.892E-42)
            r3 = 2064(0x810, float:2.892E-42)
            if (r2 != r3) goto L79b
            java.lang.String r2 = "DATA_ROAMING_SETTINGS_DISABLED"
            r0.add(r2)
            r1 = r1 | 2064(0x810, float:2.892E-42)
        L79b:
            r2 = r4 & 2065(0x811, float:2.894E-42)
            r3 = 2065(0x811, float:2.894E-42)
            if (r2 != r3) goto L7a8
            java.lang.String r2 = "DDS_SWITCHED"
            r0.add(r2)
            r1 = r1 | 2065(0x811, float:2.894E-42)
        L7a8:
            r2 = r4 & 2066(0x812, float:2.895E-42)
            r3 = 2066(0x812, float:2.895E-42)
            if (r2 != r3) goto L7b5
            java.lang.String r2 = "FORBIDDEN_APN_NAME"
            r0.add(r2)
            r1 = r1 | 2066(0x812, float:2.895E-42)
        L7b5:
            r2 = r4 & 2067(0x813, float:2.896E-42)
            r3 = 2067(0x813, float:2.896E-42)
            if (r2 != r3) goto L7c2
            java.lang.String r2 = "DDS_SWITCH_IN_PROGRESS"
            r0.add(r2)
            r1 = r1 | 2067(0x813, float:2.896E-42)
        L7c2:
            r2 = r4 & 2068(0x814, float:2.898E-42)
            r3 = 2068(0x814, float:2.898E-42)
            if (r2 != r3) goto L7cf
            java.lang.String r2 = "CALL_DISALLOWED_IN_ROAMING"
            r0.add(r2)
            r1 = r1 | 2068(0x814, float:2.898E-42)
        L7cf:
            r2 = r4 & 2069(0x815, float:2.899E-42)
            r3 = 2069(0x815, float:2.899E-42)
            if (r2 != r3) goto L7dc
            java.lang.String r2 = "NON_IP_NOT_SUPPORTED"
            r0.add(r2)
            r1 = r1 | 2069(0x815, float:2.899E-42)
        L7dc:
            r2 = r4 & 2070(0x816, float:2.9E-42)
            r3 = 2070(0x816, float:2.9E-42)
            if (r2 != r3) goto L7e9
            java.lang.String r2 = "PDN_NON_IP_CALL_THROTTLED"
            r0.add(r2)
            r1 = r1 | 2070(0x816, float:2.9E-42)
        L7e9:
            r2 = r4 & 2071(0x817, float:2.902E-42)
            r3 = 2071(0x817, float:2.902E-42)
            if (r2 != r3) goto L7f6
            java.lang.String r2 = "PDN_NON_IP_CALL_DISALLOWED"
            r0.add(r2)
            r1 = r1 | 2071(0x817, float:2.902E-42)
        L7f6:
            r2 = r4 & 2072(0x818, float:2.903E-42)
            r3 = 2072(0x818, float:2.903E-42)
            if (r2 != r3) goto L803
            java.lang.String r2 = "CDMA_LOCK"
            r0.add(r2)
            r1 = r1 | 2072(0x818, float:2.903E-42)
        L803:
            r2 = r4 & 2073(0x819, float:2.905E-42)
            r3 = 2073(0x819, float:2.905E-42)
            if (r2 != r3) goto L810
            java.lang.String r2 = "CDMA_INTERCEPT"
            r0.add(r2)
            r1 = r1 | 2073(0x819, float:2.905E-42)
        L810:
            r2 = r4 & 2074(0x81a, float:2.906E-42)
            r3 = 2074(0x81a, float:2.906E-42)
            if (r2 != r3) goto L81d
            java.lang.String r2 = "CDMA_REORDER"
            r0.add(r2)
            r1 = r1 | 2074(0x81a, float:2.906E-42)
        L81d:
            r2 = r4 & 2075(0x81b, float:2.908E-42)
            r3 = 2075(0x81b, float:2.908E-42)
            if (r2 != r3) goto L82a
            java.lang.String r2 = "CDMA_RELEASE_DUE_TO_SO_REJECTION"
            r0.add(r2)
            r1 = r1 | 2075(0x81b, float:2.908E-42)
        L82a:
            r2 = r4 & 2076(0x81c, float:2.909E-42)
            r3 = 2076(0x81c, float:2.909E-42)
            if (r2 != r3) goto L837
            java.lang.String r2 = "CDMA_INCOMING_CALL"
            r0.add(r2)
            r1 = r1 | 2076(0x81c, float:2.909E-42)
        L837:
            r2 = r4 & 2077(0x81d, float:2.91E-42)
            r3 = 2077(0x81d, float:2.91E-42)
            if (r2 != r3) goto L844
            java.lang.String r2 = "CDMA_ALERT_STOP"
            r0.add(r2)
            r1 = r1 | 2077(0x81d, float:2.91E-42)
        L844:
            r2 = r4 & 2078(0x81e, float:2.912E-42)
            r3 = 2078(0x81e, float:2.912E-42)
            if (r2 != r3) goto L851
            java.lang.String r2 = "CHANNEL_ACQUISITION_FAILURE"
            r0.add(r2)
            r1 = r1 | 2078(0x81e, float:2.912E-42)
        L851:
            r2 = r4 & 2079(0x81f, float:2.913E-42)
            r3 = 2079(0x81f, float:2.913E-42)
            if (r2 != r3) goto L85e
            java.lang.String r2 = "MAX_ACCESS_PROBE"
            r0.add(r2)
            r1 = r1 | 2079(0x81f, float:2.913E-42)
        L85e:
            r2 = r4 & 2080(0x820, float:2.915E-42)
            r3 = 2080(0x820, float:2.915E-42)
            if (r2 != r3) goto L86b
            java.lang.String r2 = "CONCURRENT_SERVICE_NOT_SUPPORTED_BY_BASE_STATION"
            r0.add(r2)
            r1 = r1 | 2080(0x820, float:2.915E-42)
        L86b:
            r2 = r4 & 2081(0x821, float:2.916E-42)
            r3 = 2081(0x821, float:2.916E-42)
            if (r2 != r3) goto L878
            java.lang.String r2 = "NO_RESPONSE_FROM_BASE_STATION"
            r0.add(r2)
            r1 = r1 | 2081(0x821, float:2.916E-42)
        L878:
            r2 = r4 & 2082(0x822, float:2.918E-42)
            r3 = 2082(0x822, float:2.918E-42)
            if (r2 != r3) goto L885
            java.lang.String r2 = "REJECTED_BY_BASE_STATION"
            r0.add(r2)
            r1 = r1 | 2082(0x822, float:2.918E-42)
        L885:
            r2 = r4 & 2083(0x823, float:2.919E-42)
            r3 = 2083(0x823, float:2.919E-42)
            if (r2 != r3) goto L892
            java.lang.String r2 = "CONCURRENT_SERVICES_INCOMPATIBLE"
            r0.add(r2)
            r1 = r1 | 2083(0x823, float:2.919E-42)
        L892:
            r2 = r4 & 2084(0x824, float:2.92E-42)
            r3 = 2084(0x824, float:2.92E-42)
            if (r2 != r3) goto L89f
            java.lang.String r2 = "NO_CDMA_SERVICE"
            r0.add(r2)
            r1 = r1 | 2084(0x824, float:2.92E-42)
        L89f:
            r2 = r4 & 2085(0x825, float:2.922E-42)
            r3 = 2085(0x825, float:2.922E-42)
            if (r2 != r3) goto L8ac
            java.lang.String r2 = "RUIM_NOT_PRESENT"
            r0.add(r2)
            r1 = r1 | 2085(0x825, float:2.922E-42)
        L8ac:
            r2 = r4 & 2086(0x826, float:2.923E-42)
            r3 = 2086(0x826, float:2.923E-42)
            if (r2 != r3) goto L8b9
            java.lang.String r2 = "CDMA_RETRY_ORDER"
            r0.add(r2)
            r1 = r1 | 2086(0x826, float:2.923E-42)
        L8b9:
            r2 = r4 & 2087(0x827, float:2.925E-42)
            r3 = 2087(0x827, float:2.925E-42)
            if (r2 != r3) goto L8c6
            java.lang.String r2 = "ACCESS_BLOCK"
            r0.add(r2)
            r1 = r1 | 2087(0x827, float:2.925E-42)
        L8c6:
            r2 = r4 & 2088(0x828, float:2.926E-42)
            r3 = 2088(0x828, float:2.926E-42)
            if (r2 != r3) goto L8d3
            java.lang.String r2 = "ACCESS_BLOCK_ALL"
            r0.add(r2)
            r1 = r1 | 2088(0x828, float:2.926E-42)
        L8d3:
            r2 = r4 & 2089(0x829, float:2.927E-42)
            r3 = 2089(0x829, float:2.927E-42)
            if (r2 != r3) goto L8e0
            java.lang.String r2 = "IS707B_MAX_ACCESS_PROBES"
            r0.add(r2)
            r1 = r1 | 2089(0x829, float:2.927E-42)
        L8e0:
            r2 = r4 & 2090(0x82a, float:2.929E-42)
            r3 = 2090(0x82a, float:2.929E-42)
            if (r2 != r3) goto L8ed
            java.lang.String r2 = "THERMAL_EMERGENCY"
            r0.add(r2)
            r1 = r1 | 2090(0x82a, float:2.929E-42)
        L8ed:
            r2 = r4 & 2091(0x82b, float:2.93E-42)
            r3 = 2091(0x82b, float:2.93E-42)
            if (r2 != r3) goto L8fa
            java.lang.String r2 = "CONCURRENT_SERVICES_NOT_ALLOWED"
            r0.add(r2)
            r1 = r1 | 2091(0x82b, float:2.93E-42)
        L8fa:
            r2 = r4 & 2092(0x82c, float:2.932E-42)
            r3 = 2092(0x82c, float:2.932E-42)
            if (r2 != r3) goto L907
            java.lang.String r2 = "INCOMING_CALL_REJECTED"
            r0.add(r2)
            r1 = r1 | 2092(0x82c, float:2.932E-42)
        L907:
            r2 = r4 & 2093(0x82d, float:2.933E-42)
            r3 = 2093(0x82d, float:2.933E-42)
            if (r2 != r3) goto L914
            java.lang.String r2 = "NO_SERVICE_ON_GATEWAY"
            r0.add(r2)
            r1 = r1 | 2093(0x82d, float:2.933E-42)
        L914:
            r2 = r4 & 2094(0x82e, float:2.934E-42)
            r3 = 2094(0x82e, float:2.934E-42)
            if (r2 != r3) goto L921
            java.lang.String r2 = "NO_GPRS_CONTEXT"
            r0.add(r2)
            r1 = r1 | 2094(0x82e, float:2.934E-42)
        L921:
            r2 = r4 & 2095(0x82f, float:2.936E-42)
            r3 = 2095(0x82f, float:2.936E-42)
            if (r2 != r3) goto L92e
            java.lang.String r2 = "ILLEGAL_MS"
            r0.add(r2)
            r1 = r1 | 2095(0x82f, float:2.936E-42)
        L92e:
            r2 = r4 & 2096(0x830, float:2.937E-42)
            r3 = 2096(0x830, float:2.937E-42)
            if (r2 != r3) goto L93b
            java.lang.String r2 = "ILLEGAL_ME"
            r0.add(r2)
            r1 = r1 | 2096(0x830, float:2.937E-42)
        L93b:
            r2 = r4 & 2097(0x831, float:2.939E-42)
            r3 = 2097(0x831, float:2.939E-42)
            if (r2 != r3) goto L948
            java.lang.String r2 = "GPRS_SERVICES_AND_NON_GPRS_SERVICES_NOT_ALLOWED"
            r0.add(r2)
            r1 = r1 | 2097(0x831, float:2.939E-42)
        L948:
            r2 = r4 & 2098(0x832, float:2.94E-42)
            r3 = 2098(0x832, float:2.94E-42)
            if (r2 != r3) goto L955
            java.lang.String r2 = "GPRS_SERVICES_NOT_ALLOWED"
            r0.add(r2)
            r1 = r1 | 2098(0x832, float:2.94E-42)
        L955:
            r2 = r4 & 2099(0x833, float:2.941E-42)
            r3 = 2099(0x833, float:2.941E-42)
            if (r2 != r3) goto L962
            java.lang.String r2 = "MS_IDENTITY_CANNOT_BE_DERIVED_BY_THE_NETWORK"
            r0.add(r2)
            r1 = r1 | 2099(0x833, float:2.941E-42)
        L962:
            r2 = r4 & 2100(0x834, float:2.943E-42)
            r3 = 2100(0x834, float:2.943E-42)
            if (r2 != r3) goto L96f
            java.lang.String r2 = "IMPLICITLY_DETACHED"
            r0.add(r2)
            r1 = r1 | 2100(0x834, float:2.943E-42)
        L96f:
            r2 = r4 & 2101(0x835, float:2.944E-42)
            r3 = 2101(0x835, float:2.944E-42)
            if (r2 != r3) goto L97c
            java.lang.String r2 = "PLMN_NOT_ALLOWED"
            r0.add(r2)
            r1 = r1 | 2101(0x835, float:2.944E-42)
        L97c:
            r2 = r4 & 2102(0x836, float:2.946E-42)
            r3 = 2102(0x836, float:2.946E-42)
            if (r2 != r3) goto L989
            java.lang.String r2 = "LOCATION_AREA_NOT_ALLOWED"
            r0.add(r2)
            r1 = r1 | 2102(0x836, float:2.946E-42)
        L989:
            r2 = r4 & 2103(0x837, float:2.947E-42)
            r3 = 2103(0x837, float:2.947E-42)
            if (r2 != r3) goto L996
            java.lang.String r2 = "GPRS_SERVICES_NOT_ALLOWED_IN_THIS_PLMN"
            r0.add(r2)
            r1 = r1 | 2103(0x837, float:2.947E-42)
        L996:
            r2 = r4 & 2104(0x838, float:2.948E-42)
            r3 = 2104(0x838, float:2.948E-42)
            if (r2 != r3) goto L9a3
            java.lang.String r2 = "PDP_DUPLICATE"
            r0.add(r2)
            r1 = r1 | 2104(0x838, float:2.948E-42)
        L9a3:
            r2 = r4 & 2105(0x839, float:2.95E-42)
            r3 = 2105(0x839, float:2.95E-42)
            if (r2 != r3) goto L9b0
            java.lang.String r2 = "UE_RAT_CHANGE"
            r0.add(r2)
            r1 = r1 | 2105(0x839, float:2.95E-42)
        L9b0:
            r2 = r4 & 2106(0x83a, float:2.951E-42)
            r3 = 2106(0x83a, float:2.951E-42)
            if (r2 != r3) goto L9bd
            java.lang.String r2 = "CONGESTION"
            r0.add(r2)
            r1 = r1 | 2106(0x83a, float:2.951E-42)
        L9bd:
            r2 = r4 & 2107(0x83b, float:2.953E-42)
            r3 = 2107(0x83b, float:2.953E-42)
            if (r2 != r3) goto L9ca
            java.lang.String r2 = "NO_PDP_CONTEXT_ACTIVATED"
            r0.add(r2)
            r1 = r1 | 2107(0x83b, float:2.953E-42)
        L9ca:
            r2 = r4 & 2108(0x83c, float:2.954E-42)
            r3 = 2108(0x83c, float:2.954E-42)
            if (r2 != r3) goto L9d7
            java.lang.String r2 = "ACCESS_CLASS_DSAC_REJECTION"
            r0.add(r2)
            r1 = r1 | 2108(0x83c, float:2.954E-42)
        L9d7:
            r2 = r4 & 2109(0x83d, float:2.955E-42)
            r3 = 2109(0x83d, float:2.955E-42)
            if (r2 != r3) goto L9e4
            java.lang.String r2 = "PDP_ACTIVATE_MAX_RETRY_FAILED"
            r0.add(r2)
            r1 = r1 | 2109(0x83d, float:2.955E-42)
        L9e4:
            r2 = r4 & 2110(0x83e, float:2.957E-42)
            r3 = 2110(0x83e, float:2.957E-42)
            if (r2 != r3) goto L9f1
            java.lang.String r2 = "RADIO_ACCESS_BEARER_FAILURE"
            r0.add(r2)
            r1 = r1 | 2110(0x83e, float:2.957E-42)
        L9f1:
            r2 = r4 & 2111(0x83f, float:2.958E-42)
            r3 = 2111(0x83f, float:2.958E-42)
            if (r2 != r3) goto L9fe
            java.lang.String r2 = "ESM_UNKNOWN_EPS_BEARER_CONTEXT"
            r0.add(r2)
            r1 = r1 | 2111(0x83f, float:2.958E-42)
        L9fe:
            r2 = r4 & 2112(0x840, float:2.96E-42)
            r3 = 2112(0x840, float:2.96E-42)
            if (r2 != r3) goto La0b
            java.lang.String r2 = "DRB_RELEASED_BY_RRC"
            r0.add(r2)
            r1 = r1 | 2112(0x840, float:2.96E-42)
        La0b:
            r2 = r4 & 2113(0x841, float:2.961E-42)
            r3 = 2113(0x841, float:2.961E-42)
            if (r2 != r3) goto La18
            java.lang.String r2 = "CONNECTION_RELEASED"
            r0.add(r2)
            r1 = r1 | 2113(0x841, float:2.961E-42)
        La18:
            r2 = r4 & 2114(0x842, float:2.962E-42)
            r3 = 2114(0x842, float:2.962E-42)
            if (r2 != r3) goto La25
            java.lang.String r2 = "EMM_DETACHED"
            r0.add(r2)
            r1 = r1 | 2114(0x842, float:2.962E-42)
        La25:
            r2 = r4 & 2115(0x843, float:2.964E-42)
            r3 = 2115(0x843, float:2.964E-42)
            if (r2 != r3) goto La32
            java.lang.String r2 = "EMM_ATTACH_FAILED"
            r0.add(r2)
            r1 = r1 | 2115(0x843, float:2.964E-42)
        La32:
            r2 = r4 & 2116(0x844, float:2.965E-42)
            r3 = 2116(0x844, float:2.965E-42)
            if (r2 != r3) goto La3f
            java.lang.String r2 = "EMM_ATTACH_STARTED"
            r0.add(r2)
            r1 = r1 | 2116(0x844, float:2.965E-42)
        La3f:
            r2 = r4 & 2117(0x845, float:2.967E-42)
            r3 = 2117(0x845, float:2.967E-42)
            if (r2 != r3) goto La4c
            java.lang.String r2 = "LTE_NAS_SERVICE_REQUEST_FAILED"
            r0.add(r2)
            r1 = r1 | 2117(0x845, float:2.967E-42)
        La4c:
            r2 = r4 & 2118(0x846, float:2.968E-42)
            r3 = 2118(0x846, float:2.968E-42)
            if (r2 != r3) goto La59
            java.lang.String r2 = "DUPLICATE_BEARER_ID"
            r0.add(r2)
            r1 = r1 | 2118(0x846, float:2.968E-42)
        La59:
            r2 = r4 & 2119(0x847, float:2.97E-42)
            r3 = 2119(0x847, float:2.97E-42)
            if (r2 != r3) goto La66
            java.lang.String r2 = "ESM_COLLISION_SCENARIOS"
            r0.add(r2)
            r1 = r1 | 2119(0x847, float:2.97E-42)
        La66:
            r2 = r4 & 2120(0x848, float:2.971E-42)
            r3 = 2120(0x848, float:2.971E-42)
            if (r2 != r3) goto La73
            java.lang.String r2 = "ESM_BEARER_DEACTIVATED_TO_SYNC_WITH_NETWORK"
            r0.add(r2)
            r1 = r1 | 2120(0x848, float:2.971E-42)
        La73:
            r2 = r4 & 2121(0x849, float:2.972E-42)
            r3 = 2121(0x849, float:2.972E-42)
            if (r2 != r3) goto La80
            java.lang.String r2 = "ESM_NW_ACTIVATED_DED_BEARER_WITH_ID_OF_DEF_BEARER"
            r0.add(r2)
            r1 = r1 | 2121(0x849, float:2.972E-42)
        La80:
            r2 = r4 & 2122(0x84a, float:2.974E-42)
            r3 = 2122(0x84a, float:2.974E-42)
            if (r2 != r3) goto La8d
            java.lang.String r2 = "ESM_BAD_OTA_MESSAGE"
            r0.add(r2)
            r1 = r1 | 2122(0x84a, float:2.974E-42)
        La8d:
            r2 = r4 & 2123(0x84b, float:2.975E-42)
            r3 = 2123(0x84b, float:2.975E-42)
            if (r2 != r3) goto La9a
            java.lang.String r2 = "ESM_DOWNLOAD_SERVER_REJECTED_THE_CALL"
            r0.add(r2)
            r1 = r1 | 2123(0x84b, float:2.975E-42)
        La9a:
            r2 = r4 & 2124(0x84c, float:2.976E-42)
            r3 = 2124(0x84c, float:2.976E-42)
            if (r2 != r3) goto Laa7
            java.lang.String r2 = "ESM_CONTEXT_TRANSFERRED_DUE_TO_IRAT"
            r0.add(r2)
            r1 = r1 | 2124(0x84c, float:2.976E-42)
        Laa7:
            r2 = r4 & 2125(0x84d, float:2.978E-42)
            r3 = 2125(0x84d, float:2.978E-42)
            if (r2 != r3) goto Lab4
            java.lang.String r2 = "DS_EXPLICIT_DEACTIVATION"
            r0.add(r2)
            r1 = r1 | 2125(0x84d, float:2.978E-42)
        Lab4:
            r2 = r4 & 2126(0x84e, float:2.979E-42)
            r3 = 2126(0x84e, float:2.979E-42)
            if (r2 != r3) goto Lac1
            java.lang.String r2 = "ESM_LOCAL_CAUSE_NONE"
            r0.add(r2)
            r1 = r1 | 2126(0x84e, float:2.979E-42)
        Lac1:
            r2 = r4 & 2127(0x84f, float:2.98E-42)
            r3 = 2127(0x84f, float:2.98E-42)
            if (r2 != r3) goto Lace
            java.lang.String r2 = "LTE_THROTTLING_NOT_REQUIRED"
            r0.add(r2)
            r1 = r1 | 2127(0x84f, float:2.98E-42)
        Lace:
            r2 = r4 & 2128(0x850, float:2.982E-42)
            r3 = 2128(0x850, float:2.982E-42)
            if (r2 != r3) goto Ladb
            java.lang.String r2 = "ACCESS_CONTROL_LIST_CHECK_FAILURE"
            r0.add(r2)
            r1 = r1 | 2128(0x850, float:2.982E-42)
        Ladb:
            r2 = r4 & 2129(0x851, float:2.983E-42)
            r3 = 2129(0x851, float:2.983E-42)
            if (r2 != r3) goto Lae8
            java.lang.String r2 = "SERVICE_NOT_ALLOWED_ON_PLMN"
            r0.add(r2)
            r1 = r1 | 2129(0x851, float:2.983E-42)
        Lae8:
            r2 = r4 & 2130(0x852, float:2.985E-42)
            r3 = 2130(0x852, float:2.985E-42)
            if (r2 != r3) goto Laf5
            java.lang.String r2 = "EMM_T3417_EXPIRED"
            r0.add(r2)
            r1 = r1 | 2130(0x852, float:2.985E-42)
        Laf5:
            r2 = r4 & 2131(0x853, float:2.986E-42)
            r3 = 2131(0x853, float:2.986E-42)
            if (r2 != r3) goto Lb02
            java.lang.String r2 = "EMM_T3417_EXT_EXPIRED"
            r0.add(r2)
            r1 = r1 | 2131(0x853, float:2.986E-42)
        Lb02:
            r2 = r4 & 2132(0x854, float:2.988E-42)
            r3 = 2132(0x854, float:2.988E-42)
            if (r2 != r3) goto Lb0f
            java.lang.String r2 = "RRC_UPLINK_DATA_TRANSMISSION_FAILURE"
            r0.add(r2)
            r1 = r1 | 2132(0x854, float:2.988E-42)
        Lb0f:
            r2 = r4 & 2133(0x855, float:2.989E-42)
            r3 = 2133(0x855, float:2.989E-42)
            if (r2 != r3) goto Lb1c
            java.lang.String r2 = "RRC_UPLINK_DELIVERY_FAILED_DUE_TO_HANDOVER"
            r0.add(r2)
            r1 = r1 | 2133(0x855, float:2.989E-42)
        Lb1c:
            r2 = r4 & 2134(0x856, float:2.99E-42)
            r3 = 2134(0x856, float:2.99E-42)
            if (r2 != r3) goto Lb29
            java.lang.String r2 = "RRC_UPLINK_CONNECTION_RELEASE"
            r0.add(r2)
            r1 = r1 | 2134(0x856, float:2.99E-42)
        Lb29:
            r2 = r4 & 2135(0x857, float:2.992E-42)
            r3 = 2135(0x857, float:2.992E-42)
            if (r2 != r3) goto Lb36
            java.lang.String r2 = "RRC_UPLINK_RADIO_LINK_FAILURE"
            r0.add(r2)
            r1 = r1 | 2135(0x857, float:2.992E-42)
        Lb36:
            r2 = r4 & 2136(0x858, float:2.993E-42)
            r3 = 2136(0x858, float:2.993E-42)
            if (r2 != r3) goto Lb43
            java.lang.String r2 = "RRC_UPLINK_ERROR_REQUEST_FROM_NAS"
            r0.add(r2)
            r1 = r1 | 2136(0x858, float:2.993E-42)
        Lb43:
            r2 = r4 & 2137(0x859, float:2.995E-42)
            r3 = 2137(0x859, float:2.995E-42)
            if (r2 != r3) goto Lb50
            java.lang.String r2 = "RRC_CONNECTION_ACCESS_STRATUM_FAILURE"
            r0.add(r2)
            r1 = r1 | 2137(0x859, float:2.995E-42)
        Lb50:
            r2 = r4 & 2138(0x85a, float:2.996E-42)
            r3 = 2138(0x85a, float:2.996E-42)
            if (r2 != r3) goto Lb5d
            java.lang.String r2 = "RRC_CONNECTION_ANOTHER_PROCEDURE_IN_PROGRESS"
            r0.add(r2)
            r1 = r1 | 2138(0x85a, float:2.996E-42)
        Lb5d:
            r2 = r4 & 2139(0x85b, float:2.997E-42)
            r3 = 2139(0x85b, float:2.997E-42)
            if (r2 != r3) goto Lb6a
            java.lang.String r2 = "RRC_CONNECTION_ACCESS_BARRED"
            r0.add(r2)
            r1 = r1 | 2139(0x85b, float:2.997E-42)
        Lb6a:
            r2 = r4 & 2140(0x85c, float:2.999E-42)
            r3 = 2140(0x85c, float:2.999E-42)
            if (r2 != r3) goto Lb77
            java.lang.String r2 = "RRC_CONNECTION_CELL_RESELECTION"
            r0.add(r2)
            r1 = r1 | 2140(0x85c, float:2.999E-42)
        Lb77:
            r2 = r4 & 2141(0x85d, float:3.0E-42)
            r3 = 2141(0x85d, float:3.0E-42)
            if (r2 != r3) goto Lb84
            java.lang.String r2 = "RRC_CONNECTION_CONFIG_FAILURE"
            r0.add(r2)
            r1 = r1 | 2141(0x85d, float:3.0E-42)
        Lb84:
            r2 = r4 & 2142(0x85e, float:3.002E-42)
            r3 = 2142(0x85e, float:3.002E-42)
            if (r2 != r3) goto Lb91
            java.lang.String r2 = "RRC_CONNECTION_TIMER_EXPIRED"
            r0.add(r2)
            r1 = r1 | 2142(0x85e, float:3.002E-42)
        Lb91:
            r2 = r4 & 2143(0x85f, float:3.003E-42)
            r3 = 2143(0x85f, float:3.003E-42)
            if (r2 != r3) goto Lb9e
            java.lang.String r2 = "RRC_CONNECTION_LINK_FAILURE"
            r0.add(r2)
            r1 = r1 | 2143(0x85f, float:3.003E-42)
        Lb9e:
            r2 = r4 & 2144(0x860, float:3.004E-42)
            r3 = 2144(0x860, float:3.004E-42)
            if (r2 != r3) goto Lbab
            java.lang.String r2 = "RRC_CONNECTION_CELL_NOT_CAMPED"
            r0.add(r2)
            r1 = r1 | 2144(0x860, float:3.004E-42)
        Lbab:
            r2 = r4 & 2145(0x861, float:3.006E-42)
            r3 = 2145(0x861, float:3.006E-42)
            if (r2 != r3) goto Lbb8
            java.lang.String r2 = "RRC_CONNECTION_SYSTEM_INTERVAL_FAILURE"
            r0.add(r2)
            r1 = r1 | 2145(0x861, float:3.006E-42)
        Lbb8:
            r2 = r4 & 2146(0x862, float:3.007E-42)
            r3 = 2146(0x862, float:3.007E-42)
            if (r2 != r3) goto Lbc5
            java.lang.String r2 = "RRC_CONNECTION_REJECT_BY_NETWORK"
            r0.add(r2)
            r1 = r1 | 2146(0x862, float:3.007E-42)
        Lbc5:
            r2 = r4 & 2147(0x863, float:3.009E-42)
            r3 = 2147(0x863, float:3.009E-42)
            if (r2 != r3) goto Lbd2
            java.lang.String r2 = "RRC_CONNECTION_NORMAL_RELEASE"
            r0.add(r2)
            r1 = r1 | 2147(0x863, float:3.009E-42)
        Lbd2:
            r2 = r4 & 2148(0x864, float:3.01E-42)
            r3 = 2148(0x864, float:3.01E-42)
            if (r2 != r3) goto Lbdf
            java.lang.String r2 = "RRC_CONNECTION_RADIO_LINK_FAILURE"
            r0.add(r2)
            r1 = r1 | 2148(0x864, float:3.01E-42)
        Lbdf:
            r2 = r4 & 2149(0x865, float:3.011E-42)
            r3 = 2149(0x865, float:3.011E-42)
            if (r2 != r3) goto Lbec
            java.lang.String r2 = "RRC_CONNECTION_REESTABLISHMENT_FAILURE"
            r0.add(r2)
            r1 = r1 | 2149(0x865, float:3.011E-42)
        Lbec:
            r2 = r4 & 2150(0x866, float:3.013E-42)
            r3 = 2150(0x866, float:3.013E-42)
            if (r2 != r3) goto Lbf9
            java.lang.String r2 = "RRC_CONNECTION_OUT_OF_SERVICE_DURING_CELL_REGISTER"
            r0.add(r2)
            r1 = r1 | 2150(0x866, float:3.013E-42)
        Lbf9:
            r2 = r4 & 2151(0x867, float:3.014E-42)
            r3 = 2151(0x867, float:3.014E-42)
            if (r2 != r3) goto Lc06
            java.lang.String r2 = "RRC_CONNECTION_ABORT_REQUEST"
            r0.add(r2)
            r1 = r1 | 2151(0x867, float:3.014E-42)
        Lc06:
            r2 = r4 & 2152(0x868, float:3.016E-42)
            r3 = 2152(0x868, float:3.016E-42)
            if (r2 != r3) goto Lc13
            java.lang.String r2 = "RRC_CONNECTION_SYSTEM_INFORMATION_BLOCK_READ_ERROR"
            r0.add(r2)
            r1 = r1 | 2152(0x868, float:3.016E-42)
        Lc13:
            r2 = r4 & 2153(0x869, float:3.017E-42)
            r3 = 2153(0x869, float:3.017E-42)
            if (r2 != r3) goto Lc20
            java.lang.String r2 = "NETWORK_INITIATED_DETACH_WITH_AUTO_REATTACH"
            r0.add(r2)
            r1 = r1 | 2153(0x869, float:3.017E-42)
        Lc20:
            r2 = r4 & 2154(0x86a, float:3.018E-42)
            r3 = 2154(0x86a, float:3.018E-42)
            if (r2 != r3) goto Lc2d
            java.lang.String r2 = "NETWORK_INITIATED_DETACH_NO_AUTO_REATTACH"
            r0.add(r2)
            r1 = r1 | 2154(0x86a, float:3.018E-42)
        Lc2d:
            r2 = r4 & 2155(0x86b, float:3.02E-42)
            r3 = 2155(0x86b, float:3.02E-42)
            if (r2 != r3) goto Lc3a
            java.lang.String r2 = "ESM_PROCEDURE_TIME_OUT"
            r0.add(r2)
            r1 = r1 | 2155(0x86b, float:3.02E-42)
        Lc3a:
            r2 = r4 & 2156(0x86c, float:3.021E-42)
            r3 = 2156(0x86c, float:3.021E-42)
            if (r2 != r3) goto Lc47
            java.lang.String r2 = "INVALID_CONNECTION_ID"
            r0.add(r2)
            r1 = r1 | 2156(0x86c, float:3.021E-42)
        Lc47:
            r2 = r4 & 2157(0x86d, float:3.023E-42)
            r3 = 2157(0x86d, float:3.023E-42)
            if (r2 != r3) goto Lc54
            java.lang.String r2 = "MAXIMIUM_NSAPIS_EXCEEDED"
            r0.add(r2)
            r1 = r1 | 2157(0x86d, float:3.023E-42)
        Lc54:
            r2 = r4 & 2158(0x86e, float:3.024E-42)
            r3 = 2158(0x86e, float:3.024E-42)
            if (r2 != r3) goto Lc61
            java.lang.String r2 = "INVALID_PRIMARY_NSAPI"
            r0.add(r2)
            r1 = r1 | 2158(0x86e, float:3.024E-42)
        Lc61:
            r2 = r4 & 2159(0x86f, float:3.025E-42)
            r3 = 2159(0x86f, float:3.025E-42)
            if (r2 != r3) goto Lc6e
            java.lang.String r2 = "CANNOT_ENCODE_OTA_MESSAGE"
            r0.add(r2)
            r1 = r1 | 2159(0x86f, float:3.025E-42)
        Lc6e:
            r2 = r4 & 2160(0x870, float:3.027E-42)
            r3 = 2160(0x870, float:3.027E-42)
            if (r2 != r3) goto Lc7b
            java.lang.String r2 = "RADIO_ACCESS_BEARER_SETUP_FAILURE"
            r0.add(r2)
            r1 = r1 | 2160(0x870, float:3.027E-42)
        Lc7b:
            r2 = r4 & 2161(0x871, float:3.028E-42)
            r3 = 2161(0x871, float:3.028E-42)
            if (r2 != r3) goto Lc88
            java.lang.String r2 = "PDP_ESTABLISH_TIMEOUT_EXPIRED"
            r0.add(r2)
            r1 = r1 | 2161(0x871, float:3.028E-42)
        Lc88:
            r2 = r4 & 2162(0x872, float:3.03E-42)
            r3 = 2162(0x872, float:3.03E-42)
            if (r2 != r3) goto Lc95
            java.lang.String r2 = "PDP_MODIFY_TIMEOUT_EXPIRED"
            r0.add(r2)
            r1 = r1 | 2162(0x872, float:3.03E-42)
        Lc95:
            r2 = r4 & 2163(0x873, float:3.031E-42)
            r3 = 2163(0x873, float:3.031E-42)
            if (r2 != r3) goto Lca2
            java.lang.String r2 = "PDP_INACTIVE_TIMEOUT_EXPIRED"
            r0.add(r2)
            r1 = r1 | 2163(0x873, float:3.031E-42)
        Lca2:
            r2 = r4 & 2164(0x874, float:3.032E-42)
            r3 = 2164(0x874, float:3.032E-42)
            if (r2 != r3) goto Lcaf
            java.lang.String r2 = "PDP_LOWERLAYER_ERROR"
            r0.add(r2)
            r1 = r1 | 2164(0x874, float:3.032E-42)
        Lcaf:
            r2 = r4 & 2165(0x875, float:3.034E-42)
            r3 = 2165(0x875, float:3.034E-42)
            if (r2 != r3) goto Lcbc
            java.lang.String r2 = "PDP_MODIFY_COLLISION"
            r0.add(r2)
            r1 = r1 | 2165(0x875, float:3.034E-42)
        Lcbc:
            r2 = r4 & 2166(0x876, float:3.035E-42)
            r3 = 2166(0x876, float:3.035E-42)
            if (r2 != r3) goto Lcc9
            java.lang.String r2 = "MAXINUM_SIZE_OF_L2_MESSAGE_EXCEEDED"
            r0.add(r2)
            r1 = r1 | 2166(0x876, float:3.035E-42)
        Lcc9:
            r2 = r4 & 2167(0x877, float:3.037E-42)
            r3 = 2167(0x877, float:3.037E-42)
            if (r2 != r3) goto Lcd6
            java.lang.String r2 = "NAS_REQUEST_REJECTED_BY_NETWORK"
            r0.add(r2)
            r1 = r1 | 2167(0x877, float:3.037E-42)
        Lcd6:
            r2 = r4 & 2168(0x878, float:3.038E-42)
            r3 = 2168(0x878, float:3.038E-42)
            if (r2 != r3) goto Lce3
            java.lang.String r2 = "RRC_CONNECTION_INVALID_REQUEST"
            r0.add(r2)
            r1 = r1 | 2168(0x878, float:3.038E-42)
        Lce3:
            r2 = r4 & 2169(0x879, float:3.04E-42)
            r3 = 2169(0x879, float:3.04E-42)
            if (r2 != r3) goto Lcf0
            java.lang.String r2 = "RRC_CONNECTION_TRACKING_AREA_ID_CHANGED"
            r0.add(r2)
            r1 = r1 | 2169(0x879, float:3.04E-42)
        Lcf0:
            r2 = r4 & 2170(0x87a, float:3.041E-42)
            r3 = 2170(0x87a, float:3.041E-42)
            if (r2 != r3) goto Lcfd
            java.lang.String r2 = "RRC_CONNECTION_RF_UNAVAILABLE"
            r0.add(r2)
            r1 = r1 | 2170(0x87a, float:3.041E-42)
        Lcfd:
            r2 = r4 & 2171(0x87b, float:3.042E-42)
            r3 = 2171(0x87b, float:3.042E-42)
            if (r2 != r3) goto Ld0a
            java.lang.String r2 = "RRC_CONNECTION_ABORTED_DUE_TO_IRAT_CHANGE"
            r0.add(r2)
            r1 = r1 | 2171(0x87b, float:3.042E-42)
        Ld0a:
            r2 = r4 & 2172(0x87c, float:3.044E-42)
            r3 = 2172(0x87c, float:3.044E-42)
            if (r2 != r3) goto Ld17
            java.lang.String r2 = "RRC_CONNECTION_RELEASED_SECURITY_NOT_ACTIVE"
            r0.add(r2)
            r1 = r1 | 2172(0x87c, float:3.044E-42)
        Ld17:
            r2 = r4 & 2173(0x87d, float:3.045E-42)
            r3 = 2173(0x87d, float:3.045E-42)
            if (r2 != r3) goto Ld24
            java.lang.String r2 = "RRC_CONNECTION_ABORTED_AFTER_HANDOVER"
            r0.add(r2)
            r1 = r1 | 2173(0x87d, float:3.045E-42)
        Ld24:
            r2 = r4 & 2174(0x87e, float:3.046E-42)
            r3 = 2174(0x87e, float:3.046E-42)
            if (r2 != r3) goto Ld31
            java.lang.String r2 = "RRC_CONNECTION_ABORTED_AFTER_IRAT_CELL_CHANGE"
            r0.add(r2)
            r1 = r1 | 2174(0x87e, float:3.046E-42)
        Ld31:
            r2 = r4 & 2175(0x87f, float:3.048E-42)
            r3 = 2175(0x87f, float:3.048E-42)
            if (r2 != r3) goto Ld3e
            java.lang.String r2 = "RRC_CONNECTION_ABORTED_DURING_IRAT_CELL_CHANGE"
            r0.add(r2)
            r1 = r1 | 2175(0x87f, float:3.048E-42)
        Ld3e:
            r2 = r4 & 2176(0x880, float:3.049E-42)
            r3 = 2176(0x880, float:3.049E-42)
            if (r2 != r3) goto Ld4b
            java.lang.String r2 = "IMSI_UNKNOWN_IN_HOME_SUBSCRIBER_SERVER"
            r0.add(r2)
            r1 = r1 | 2176(0x880, float:3.049E-42)
        Ld4b:
            r2 = r4 & 2177(0x881, float:3.05E-42)
            r3 = 2177(0x881, float:3.05E-42)
            if (r2 != r3) goto Ld58
            java.lang.String r2 = "IMEI_NOT_ACCEPTED"
            r0.add(r2)
            r1 = r1 | 2177(0x881, float:3.05E-42)
        Ld58:
            r2 = r4 & 2178(0x882, float:3.052E-42)
            r3 = 2178(0x882, float:3.052E-42)
            if (r2 != r3) goto Ld65
            java.lang.String r2 = "EPS_SERVICES_AND_NON_EPS_SERVICES_NOT_ALLOWED"
            r0.add(r2)
            r1 = r1 | 2178(0x882, float:3.052E-42)
        Ld65:
            r2 = r4 & 2179(0x883, float:3.053E-42)
            r3 = 2179(0x883, float:3.053E-42)
            if (r2 != r3) goto Ld72
            java.lang.String r2 = "EPS_SERVICES_NOT_ALLOWED_IN_PLMN"
            r0.add(r2)
            r1 = r1 | 2179(0x883, float:3.053E-42)
        Ld72:
            r2 = r4 & 2180(0x884, float:3.055E-42)
            r3 = 2180(0x884, float:3.055E-42)
            if (r2 != r3) goto Ld7f
            java.lang.String r2 = "MSC_TEMPORARILY_NOT_REACHABLE"
            r0.add(r2)
            r1 = r1 | 2180(0x884, float:3.055E-42)
        Ld7f:
            r2 = r4 & 2181(0x885, float:3.056E-42)
            r3 = 2181(0x885, float:3.056E-42)
            if (r2 != r3) goto Ld8c
            java.lang.String r2 = "CS_DOMAIN_NOT_AVAILABLE"
            r0.add(r2)
            r1 = r1 | 2181(0x885, float:3.056E-42)
        Ld8c:
            r2 = r4 & 2182(0x886, float:3.058E-42)
            r3 = 2182(0x886, float:3.058E-42)
            if (r2 != r3) goto Ld99
            java.lang.String r2 = "ESM_FAILURE"
            r0.add(r2)
            r1 = r1 | 2182(0x886, float:3.058E-42)
        Ld99:
            r2 = r4 & 2183(0x887, float:3.059E-42)
            r3 = 2183(0x887, float:3.059E-42)
            if (r2 != r3) goto Lda6
            java.lang.String r2 = "MAC_FAILURE"
            r0.add(r2)
            r1 = r1 | 2183(0x887, float:3.059E-42)
        Lda6:
            r2 = r4 & 2184(0x888, float:3.06E-42)
            r3 = 2184(0x888, float:3.06E-42)
            if (r2 != r3) goto Ldb3
            java.lang.String r2 = "SYNCHRONIZATION_FAILURE"
            r0.add(r2)
            r1 = r1 | 2184(0x888, float:3.06E-42)
        Ldb3:
            r2 = r4 & 2185(0x889, float:3.062E-42)
            r3 = 2185(0x889, float:3.062E-42)
            if (r2 != r3) goto Ldc0
            java.lang.String r2 = "UE_SECURITY_CAPABILITIES_MISMATCH"
            r0.add(r2)
            r1 = r1 | 2185(0x889, float:3.062E-42)
        Ldc0:
            r2 = r4 & 2186(0x88a, float:3.063E-42)
            r3 = 2186(0x88a, float:3.063E-42)
            if (r2 != r3) goto Ldcd
            java.lang.String r2 = "SECURITY_MODE_REJECTED"
            r0.add(r2)
            r1 = r1 | 2186(0x88a, float:3.063E-42)
        Ldcd:
            r2 = r4 & 2187(0x88b, float:3.065E-42)
            r3 = 2187(0x88b, float:3.065E-42)
            if (r2 != r3) goto Ldda
            java.lang.String r2 = "UNACCEPTABLE_NON_EPS_AUTHENTICATION"
            r0.add(r2)
            r1 = r1 | 2187(0x88b, float:3.065E-42)
        Ldda:
            r2 = r4 & 2188(0x88c, float:3.066E-42)
            r3 = 2188(0x88c, float:3.066E-42)
            if (r2 != r3) goto Lde7
            java.lang.String r2 = "CS_FALLBACK_CALL_ESTABLISHMENT_NOT_ALLOWED"
            r0.add(r2)
            r1 = r1 | 2188(0x88c, float:3.066E-42)
        Lde7:
            r2 = r4 & 2189(0x88d, float:3.067E-42)
            r3 = 2189(0x88d, float:3.067E-42)
            if (r2 != r3) goto Ldf4
            java.lang.String r2 = "NO_EPS_BEARER_CONTEXT_ACTIVATED"
            r0.add(r2)
            r1 = r1 | 2189(0x88d, float:3.067E-42)
        Ldf4:
            r2 = r4 & 2190(0x88e, float:3.069E-42)
            r3 = 2190(0x88e, float:3.069E-42)
            if (r2 != r3) goto Le01
            java.lang.String r2 = "INVALID_EMM_STATE"
            r0.add(r2)
            r1 = r1 | 2190(0x88e, float:3.069E-42)
        Le01:
            r2 = r4 & 2191(0x88f, float:3.07E-42)
            r3 = 2191(0x88f, float:3.07E-42)
            if (r2 != r3) goto Le0e
            java.lang.String r2 = "NAS_LAYER_FAILURE"
            r0.add(r2)
            r1 = r1 | 2191(0x88f, float:3.07E-42)
        Le0e:
            r2 = r4 & 2192(0x890, float:3.072E-42)
            r3 = 2192(0x890, float:3.072E-42)
            if (r2 != r3) goto Le1b
            java.lang.String r2 = "MULTIPLE_PDP_CALL_NOT_ALLOWED"
            r0.add(r2)
            r1 = r1 | 2192(0x890, float:3.072E-42)
        Le1b:
            r2 = r4 & 2193(0x891, float:3.073E-42)
            r3 = 2193(0x891, float:3.073E-42)
            if (r2 != r3) goto Le28
            java.lang.String r2 = "EMBMS_NOT_ENABLED"
            r0.add(r2)
            r1 = r1 | 2193(0x891, float:3.073E-42)
        Le28:
            r2 = r4 & 2194(0x892, float:3.074E-42)
            r3 = 2194(0x892, float:3.074E-42)
            if (r2 != r3) goto Le35
            java.lang.String r2 = "IRAT_HANDOVER_FAILED"
            r0.add(r2)
            r1 = r1 | 2194(0x892, float:3.074E-42)
        Le35:
            r2 = r4 & 2195(0x893, float:3.076E-42)
            r3 = 2195(0x893, float:3.076E-42)
            if (r2 != r3) goto Le42
            java.lang.String r2 = "EMBMS_REGULAR_DEACTIVATION"
            r0.add(r2)
            r1 = r1 | 2195(0x893, float:3.076E-42)
        Le42:
            r2 = r4 & 2196(0x894, float:3.077E-42)
            r3 = 2196(0x894, float:3.077E-42)
            if (r2 != r3) goto Le4f
            java.lang.String r2 = "TEST_LOOPBACK_REGULAR_DEACTIVATION"
            r0.add(r2)
            r1 = r1 | 2196(0x894, float:3.077E-42)
        Le4f:
            r2 = r4 & 2197(0x895, float:3.079E-42)
            r3 = 2197(0x895, float:3.079E-42)
            if (r2 != r3) goto Le5c
            java.lang.String r2 = "LOWER_LAYER_REGISTRATION_FAILURE"
            r0.add(r2)
            r1 = r1 | 2197(0x895, float:3.079E-42)
        Le5c:
            r2 = r4 & 2198(0x896, float:3.08E-42)
            r3 = 2198(0x896, float:3.08E-42)
            if (r2 != r3) goto Le69
            java.lang.String r2 = "DATA_PLAN_EXPIRED"
            r0.add(r2)
            r1 = r1 | 2198(0x896, float:3.08E-42)
        Le69:
            r2 = r4 & 2199(0x897, float:3.081E-42)
            r3 = 2199(0x897, float:3.081E-42)
            if (r2 != r3) goto Le76
            java.lang.String r2 = "UMTS_HANDOVER_TO_IWLAN"
            r0.add(r2)
            r1 = r1 | 2199(0x897, float:3.081E-42)
        Le76:
            r2 = r4 & 2200(0x898, float:3.083E-42)
            r3 = 2200(0x898, float:3.083E-42)
            if (r2 != r3) goto Le83
            java.lang.String r2 = "EVDO_CONNECTION_DENY_BY_GENERAL_OR_NETWORK_BUSY"
            r0.add(r2)
            r1 = r1 | 2200(0x898, float:3.083E-42)
        Le83:
            r2 = r4 & 2201(0x899, float:3.084E-42)
            r3 = 2201(0x899, float:3.084E-42)
            if (r2 != r3) goto Le90
            java.lang.String r2 = "EVDO_CONNECTION_DENY_BY_BILLING_OR_AUTHENTICATION_FAILURE"
            r0.add(r2)
            r1 = r1 | 2201(0x899, float:3.084E-42)
        Le90:
            r2 = r4 & 2202(0x89a, float:3.086E-42)
            r3 = 2202(0x89a, float:3.086E-42)
            if (r2 != r3) goto Le9d
            java.lang.String r2 = "EVDO_HDR_CHANGED"
            r0.add(r2)
            r1 = r1 | 2202(0x89a, float:3.086E-42)
        Le9d:
            r2 = r4 & 2203(0x89b, float:3.087E-42)
            r3 = 2203(0x89b, float:3.087E-42)
            if (r2 != r3) goto Leaa
            java.lang.String r2 = "EVDO_HDR_EXITED"
            r0.add(r2)
            r1 = r1 | 2203(0x89b, float:3.087E-42)
        Leaa:
            r2 = r4 & 2204(0x89c, float:3.088E-42)
            r3 = 2204(0x89c, float:3.088E-42)
            if (r2 != r3) goto Leb7
            java.lang.String r2 = "EVDO_HDR_NO_SESSION"
            r0.add(r2)
            r1 = r1 | 2204(0x89c, float:3.088E-42)
        Leb7:
            r2 = r4 & 2205(0x89d, float:3.09E-42)
            r3 = 2205(0x89d, float:3.09E-42)
            if (r2 != r3) goto Lec4
            java.lang.String r2 = "EVDO_USING_GPS_FIX_INSTEAD_OF_HDR_CALL"
            r0.add(r2)
            r1 = r1 | 2205(0x89d, float:3.09E-42)
        Lec4:
            r2 = r4 & 2206(0x89e, float:3.091E-42)
            r3 = 2206(0x89e, float:3.091E-42)
            if (r2 != r3) goto Led1
            java.lang.String r2 = "EVDO_HDR_CONNECTION_SETUP_TIMEOUT"
            r0.add(r2)
            r1 = r1 | 2206(0x89e, float:3.091E-42)
        Led1:
            r2 = r4 & 2207(0x89f, float:3.093E-42)
            r3 = 2207(0x89f, float:3.093E-42)
            if (r2 != r3) goto Lede
            java.lang.String r2 = "FAILED_TO_ACQUIRE_COLOCATED_HDR"
            r0.add(r2)
            r1 = r1 | 2207(0x89f, float:3.093E-42)
        Lede:
            r2 = r4 & 2208(0x8a0, float:3.094E-42)
            r3 = 2208(0x8a0, float:3.094E-42)
            if (r2 != r3) goto Leeb
            java.lang.String r2 = "OTASP_COMMIT_IN_PROGRESS"
            r0.add(r2)
            r1 = r1 | 2208(0x8a0, float:3.094E-42)
        Leeb:
            r2 = r4 & 2209(0x8a1, float:3.095E-42)
            r3 = 2209(0x8a1, float:3.095E-42)
            if (r2 != r3) goto Lef8
            java.lang.String r2 = "NO_HYBRID_HDR_SERVICE"
            r0.add(r2)
            r1 = r1 | 2209(0x8a1, float:3.095E-42)
        Lef8:
            r2 = r4 & 2210(0x8a2, float:3.097E-42)
            r3 = 2210(0x8a2, float:3.097E-42)
            if (r2 != r3) goto Lf05
            java.lang.String r2 = "HDR_NO_LOCK_GRANTED"
            r0.add(r2)
            r1 = r1 | 2210(0x8a2, float:3.097E-42)
        Lf05:
            r2 = r4 & 2211(0x8a3, float:3.098E-42)
            r3 = 2211(0x8a3, float:3.098E-42)
            if (r2 != r3) goto Lf12
            java.lang.String r2 = "DBM_OR_SMS_IN_PROGRESS"
            r0.add(r2)
            r1 = r1 | 2211(0x8a3, float:3.098E-42)
        Lf12:
            r2 = r4 & 2212(0x8a4, float:3.1E-42)
            r3 = 2212(0x8a4, float:3.1E-42)
            if (r2 != r3) goto Lf1f
            java.lang.String r2 = "HDR_FADE"
            r0.add(r2)
            r1 = r1 | 2212(0x8a4, float:3.1E-42)
        Lf1f:
            r2 = r4 & 2213(0x8a5, float:3.101E-42)
            r3 = 2213(0x8a5, float:3.101E-42)
            if (r2 != r3) goto Lf2c
            java.lang.String r2 = "HDR_ACCESS_FAILURE"
            r0.add(r2)
            r1 = r1 | 2213(0x8a5, float:3.101E-42)
        Lf2c:
            r2 = r4 & 2214(0x8a6, float:3.102E-42)
            r3 = 2214(0x8a6, float:3.102E-42)
            if (r2 != r3) goto Lf39
            java.lang.String r2 = "UNSUPPORTED_1X_PREV"
            r0.add(r2)
            r1 = r1 | 2214(0x8a6, float:3.102E-42)
        Lf39:
            r2 = r4 & 2215(0x8a7, float:3.104E-42)
            r3 = 2215(0x8a7, float:3.104E-42)
            if (r2 != r3) goto Lf46
            java.lang.String r2 = "LOCAL_END"
            r0.add(r2)
            r1 = r1 | 2215(0x8a7, float:3.104E-42)
        Lf46:
            r2 = r4 & 2216(0x8a8, float:3.105E-42)
            r3 = 2216(0x8a8, float:3.105E-42)
            if (r2 != r3) goto Lf53
            java.lang.String r2 = "NO_SERVICE"
            r0.add(r2)
            r1 = r1 | 2216(0x8a8, float:3.105E-42)
        Lf53:
            r2 = r4 & 2217(0x8a9, float:3.107E-42)
            r3 = 2217(0x8a9, float:3.107E-42)
            if (r2 != r3) goto Lf60
            java.lang.String r2 = "FADE"
            r0.add(r2)
            r1 = r1 | 2217(0x8a9, float:3.107E-42)
        Lf60:
            r2 = r4 & 2218(0x8aa, float:3.108E-42)
            r3 = 2218(0x8aa, float:3.108E-42)
            if (r2 != r3) goto Lf6d
            java.lang.String r2 = "NORMAL_RELEASE"
            r0.add(r2)
            r1 = r1 | 2218(0x8aa, float:3.108E-42)
        Lf6d:
            r2 = r4 & 2219(0x8ab, float:3.11E-42)
            r3 = 2219(0x8ab, float:3.11E-42)
            if (r2 != r3) goto Lf7a
            java.lang.String r2 = "ACCESS_ATTEMPT_ALREADY_IN_PROGRESS"
            r0.add(r2)
            r1 = r1 | 2219(0x8ab, float:3.11E-42)
        Lf7a:
            r2 = r4 & 2220(0x8ac, float:3.111E-42)
            r3 = 2220(0x8ac, float:3.111E-42)
            if (r2 != r3) goto Lf87
            java.lang.String r2 = "REDIRECTION_OR_HANDOFF_IN_PROGRESS"
            r0.add(r2)
            r1 = r1 | 2220(0x8ac, float:3.111E-42)
        Lf87:
            r2 = r4 & 2221(0x8ad, float:3.112E-42)
            r3 = 2221(0x8ad, float:3.112E-42)
            if (r2 != r3) goto Lf94
            java.lang.String r2 = "EMERGENCY_MODE"
            r0.add(r2)
            r1 = r1 | 2221(0x8ad, float:3.112E-42)
        Lf94:
            r2 = r4 & 2222(0x8ae, float:3.114E-42)
            r3 = 2222(0x8ae, float:3.114E-42)
            if (r2 != r3) goto Lfa1
            java.lang.String r2 = "PHONE_IN_USE"
            r0.add(r2)
            r1 = r1 | 2222(0x8ae, float:3.114E-42)
        Lfa1:
            r2 = r4 & 2223(0x8af, float:3.115E-42)
            r3 = 2223(0x8af, float:3.115E-42)
            if (r2 != r3) goto Lfae
            java.lang.String r2 = "INVALID_MODE"
            r0.add(r2)
            r1 = r1 | 2223(0x8af, float:3.115E-42)
        Lfae:
            r2 = r4 & 2224(0x8b0, float:3.116E-42)
            r3 = 2224(0x8b0, float:3.116E-42)
            if (r2 != r3) goto Lfbb
            java.lang.String r2 = "INVALID_SIM_STATE"
            r0.add(r2)
            r1 = r1 | 2224(0x8b0, float:3.116E-42)
        Lfbb:
            r2 = r4 & 2225(0x8b1, float:3.118E-42)
            r3 = 2225(0x8b1, float:3.118E-42)
            if (r2 != r3) goto Lfc8
            java.lang.String r2 = "NO_COLLOCATED_HDR"
            r0.add(r2)
            r1 = r1 | 2225(0x8b1, float:3.118E-42)
        Lfc8:
            r2 = r4 & 2226(0x8b2, float:3.119E-42)
            r3 = 2226(0x8b2, float:3.119E-42)
            if (r2 != r3) goto Lfd5
            java.lang.String r2 = "UE_IS_ENTERING_POWERSAVE_MODE"
            r0.add(r2)
            r1 = r1 | 2226(0x8b2, float:3.119E-42)
        Lfd5:
            r2 = r4 & 2227(0x8b3, float:3.12E-42)
            r3 = 2227(0x8b3, float:3.12E-42)
            if (r2 != r3) goto Lfe2
            java.lang.String r2 = "DUAL_SWITCH"
            r0.add(r2)
            r1 = r1 | 2227(0x8b3, float:3.12E-42)
        Lfe2:
            r2 = r4 & 2228(0x8b4, float:3.122E-42)
            r3 = 2228(0x8b4, float:3.122E-42)
            if (r2 != r3) goto Lfef
            java.lang.String r2 = "PPP_TIMEOUT"
            r0.add(r2)
            r1 = r1 | 2228(0x8b4, float:3.122E-42)
        Lfef:
            r2 = r4 & 2229(0x8b5, float:3.123E-42)
            r3 = 2229(0x8b5, float:3.123E-42)
            if (r2 != r3) goto Lffc
            java.lang.String r2 = "PPP_AUTH_FAILURE"
            r0.add(r2)
            r1 = r1 | 2229(0x8b5, float:3.123E-42)
        Lffc:
            r2 = r4 & 2230(0x8b6, float:3.125E-42)
            r3 = 2230(0x8b6, float:3.125E-42)
            if (r2 != r3) goto L1009
            java.lang.String r2 = "PPP_OPTION_MISMATCH"
            r0.add(r2)
            r1 = r1 | 2230(0x8b6, float:3.125E-42)
        L1009:
            r2 = r4 & 2231(0x8b7, float:3.126E-42)
            r3 = 2231(0x8b7, float:3.126E-42)
            if (r2 != r3) goto L1016
            java.lang.String r2 = "PPP_PAP_FAILURE"
            r0.add(r2)
            r1 = r1 | 2231(0x8b7, float:3.126E-42)
        L1016:
            r2 = r4 & 2232(0x8b8, float:3.128E-42)
            r3 = 2232(0x8b8, float:3.128E-42)
            if (r2 != r3) goto L1023
            java.lang.String r2 = "PPP_CHAP_FAILURE"
            r0.add(r2)
            r1 = r1 | 2232(0x8b8, float:3.128E-42)
        L1023:
            r2 = r4 & 2233(0x8b9, float:3.129E-42)
            r3 = 2233(0x8b9, float:3.129E-42)
            if (r2 != r3) goto L1030
            java.lang.String r2 = "PPP_CLOSE_IN_PROGRESS"
            r0.add(r2)
            r1 = r1 | 2233(0x8b9, float:3.129E-42)
        L1030:
            r2 = r4 & 2234(0x8ba, float:3.13E-42)
            r3 = 2234(0x8ba, float:3.13E-42)
            if (r2 != r3) goto L103d
            java.lang.String r2 = "LIMITED_TO_IPV4"
            r0.add(r2)
            r1 = r1 | 2234(0x8ba, float:3.13E-42)
        L103d:
            r2 = r4 & 2235(0x8bb, float:3.132E-42)
            r3 = 2235(0x8bb, float:3.132E-42)
            if (r2 != r3) goto L104a
            java.lang.String r2 = "LIMITED_TO_IPV6"
            r0.add(r2)
            r1 = r1 | 2235(0x8bb, float:3.132E-42)
        L104a:
            r2 = r4 & 2236(0x8bc, float:3.133E-42)
            r3 = 2236(0x8bc, float:3.133E-42)
            if (r2 != r3) goto L1057
            java.lang.String r2 = "VSNCP_TIMEOUT"
            r0.add(r2)
            r1 = r1 | 2236(0x8bc, float:3.133E-42)
        L1057:
            r2 = r4 & 2237(0x8bd, float:3.135E-42)
            r3 = 2237(0x8bd, float:3.135E-42)
            if (r2 != r3) goto L1064
            java.lang.String r2 = "VSNCP_GEN_ERROR"
            r0.add(r2)
            r1 = r1 | 2237(0x8bd, float:3.135E-42)
        L1064:
            r2 = r4 & 2238(0x8be, float:3.136E-42)
            r3 = 2238(0x8be, float:3.136E-42)
            if (r2 != r3) goto L1071
            java.lang.String r2 = "VSNCP_APN_UNATHORIZED"
            r0.add(r2)
            r1 = r1 | 2238(0x8be, float:3.136E-42)
        L1071:
            r2 = r4 & 2239(0x8bf, float:3.138E-42)
            r3 = 2239(0x8bf, float:3.138E-42)
            if (r2 != r3) goto L107e
            java.lang.String r2 = "VSNCP_PDN_LIMIT_EXCEEDED"
            r0.add(r2)
            r1 = r1 | 2239(0x8bf, float:3.138E-42)
        L107e:
            r2 = r4 & 2240(0x8c0, float:3.139E-42)
            r3 = 2240(0x8c0, float:3.139E-42)
            if (r2 != r3) goto L108b
            java.lang.String r2 = "VSNCP_NO_PDN_GATEWAY_ADDRESS"
            r0.add(r2)
            r1 = r1 | 2240(0x8c0, float:3.139E-42)
        L108b:
            r2 = r4 & 2241(0x8c1, float:3.14E-42)
            r3 = 2241(0x8c1, float:3.14E-42)
            if (r2 != r3) goto L1098
            java.lang.String r2 = "VSNCP_PDN_GATEWAY_UNREACHABLE"
            r0.add(r2)
            r1 = r1 | 2241(0x8c1, float:3.14E-42)
        L1098:
            r2 = r4 & 2242(0x8c2, float:3.142E-42)
            r3 = 2242(0x8c2, float:3.142E-42)
            if (r2 != r3) goto L10a5
            java.lang.String r2 = "VSNCP_PDN_GATEWAY_REJECT"
            r0.add(r2)
            r1 = r1 | 2242(0x8c2, float:3.142E-42)
        L10a5:
            r2 = r4 & 2243(0x8c3, float:3.143E-42)
            r3 = 2243(0x8c3, float:3.143E-42)
            if (r2 != r3) goto L10b2
            java.lang.String r2 = "VSNCP_INSUFFICIENT_PARAMETERS"
            r0.add(r2)
            r1 = r1 | 2243(0x8c3, float:3.143E-42)
        L10b2:
            r2 = r4 & 2244(0x8c4, float:3.145E-42)
            r3 = 2244(0x8c4, float:3.145E-42)
            if (r2 != r3) goto L10bf
            java.lang.String r2 = "VSNCP_RESOURCE_UNAVAILABLE"
            r0.add(r2)
            r1 = r1 | 2244(0x8c4, float:3.145E-42)
        L10bf:
            r2 = r4 & 2245(0x8c5, float:3.146E-42)
            r3 = 2245(0x8c5, float:3.146E-42)
            if (r2 != r3) goto L10cc
            java.lang.String r2 = "VSNCP_ADMINISTRATIVELY_PROHIBITED"
            r0.add(r2)
            r1 = r1 | 2245(0x8c5, float:3.146E-42)
        L10cc:
            r2 = r4 & 2246(0x8c6, float:3.147E-42)
            r3 = 2246(0x8c6, float:3.147E-42)
            if (r2 != r3) goto L10d9
            java.lang.String r2 = "VSNCP_PDN_ID_IN_USE"
            r0.add(r2)
            r1 = r1 | 2246(0x8c6, float:3.147E-42)
        L10d9:
            r2 = r4 & 2247(0x8c7, float:3.149E-42)
            r3 = 2247(0x8c7, float:3.149E-42)
            if (r2 != r3) goto L10e6
            java.lang.String r2 = "VSNCP_SUBSCRIBER_LIMITATION"
            r0.add(r2)
            r1 = r1 | 2247(0x8c7, float:3.149E-42)
        L10e6:
            r2 = r4 & 2248(0x8c8, float:3.15E-42)
            r3 = 2248(0x8c8, float:3.15E-42)
            if (r2 != r3) goto L10f3
            java.lang.String r2 = "VSNCP_PDN_EXISTS_FOR_THIS_APN"
            r0.add(r2)
            r1 = r1 | 2248(0x8c8, float:3.15E-42)
        L10f3:
            r2 = r4 & 2249(0x8c9, float:3.152E-42)
            r3 = 2249(0x8c9, float:3.152E-42)
            if (r2 != r3) goto L1100
            java.lang.String r2 = "VSNCP_RECONNECT_NOT_ALLOWED"
            r0.add(r2)
            r1 = r1 | 2249(0x8c9, float:3.152E-42)
        L1100:
            r2 = r4 & 2250(0x8ca, float:3.153E-42)
            r3 = 2250(0x8ca, float:3.153E-42)
            if (r2 != r3) goto L110d
            java.lang.String r2 = "IPV6_PREFIX_UNAVAILABLE"
            r0.add(r2)
            r1 = r1 | 2250(0x8ca, float:3.153E-42)
        L110d:
            r2 = r4 & 2251(0x8cb, float:3.154E-42)
            r3 = 2251(0x8cb, float:3.154E-42)
            if (r2 != r3) goto L111a
            java.lang.String r2 = "HANDOFF_PREFERENCE_CHANGED"
            r0.add(r2)
            r1 = r1 | 2251(0x8cb, float:3.154E-42)
        L111a:
            if (r4 == r1) goto L1138
            java.lang.StringBuilder r2 = new java.lang.StringBuilder
            r2.<init>()
            java.lang.String r3 = "0x"
            java.lang.StringBuilder r2 = r2.append(r3)
            int r3 = ~r1
            r3 = r3 & r4
            java.lang.String r3 = java.lang.Integer.toHexString(r3)
            java.lang.StringBuilder r2 = r2.append(r3)
            java.lang.String r2 = r2.toString()
            r0.add(r2)
        L1138:
            java.lang.String r2 = " | "
            java.lang.String r2 = java.lang.String.join(r2, r0)
            return r2
        */
        throw new UnsupportedOperationException("Method not decompiled: android.hardware.radio.V1_4.DataCallFailCause.dumpBitfield(int):java.lang.String");
    }
}
