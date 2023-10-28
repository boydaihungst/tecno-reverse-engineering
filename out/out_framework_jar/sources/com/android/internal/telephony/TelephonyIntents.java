package com.android.internal.telephony;
/* loaded from: classes4.dex */
public class TelephonyIntents {
    public static final String ACTION_ANY_DATA_CONNECTION_STATE_CHANGED = "android.intent.action.ANY_DATA_STATE";
    public static final String ACTION_APC_INFO_NOTIFY = "com.mediatek.phone.ACTION_APC_INFO_NOTIFY";
    public static final String ACTION_CARD_DETECTED = "com.mediatek.phone.ACTION_CARD_DETECTED";
    public static final String ACTION_CARRIER_CERTIFICATE_DOWNLOAD = "com.android.internal.telephony.ACTION_CARRIER_CERTIFICATE_DOWNLOAD";
    @Deprecated
    public static final String ACTION_CARRIER_SIGNAL_DEFAULT_NETWORK_AVAILABLE = "com.android.internal.telephony.CARRIER_SIGNAL_DEFAULT_NETWORK_AVAILABLE";
    @Deprecated
    public static final String ACTION_CARRIER_SIGNAL_PCO_VALUE = "com.android.internal.telephony.CARRIER_SIGNAL_PCO_VALUE";
    @Deprecated
    public static final String ACTION_CARRIER_SIGNAL_REDIRECTED = "com.android.internal.telephony.CARRIER_SIGNAL_REDIRECTED";
    @Deprecated
    public static final String ACTION_CARRIER_SIGNAL_REQUEST_NETWORK_FAILED = "com.android.internal.telephony.CARRIER_SIGNAL_REQUEST_NETWORK_FAILED";
    @Deprecated
    public static final String ACTION_CARRIER_SIGNAL_RESET = "com.android.internal.telephony.CARRIER_SIGNAL_RESET";
    public static final String ACTION_COMMON_SLOT_NO_CHANGED = "com.mediatek.phone.ACTION_COMMON_SLOT_NO_CHANGED";
    public static final String ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED = "android.intent.action.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED";
    @Deprecated
    public static final String ACTION_DEFAULT_SMS_SUBSCRIPTION_CHANGED = "android.telephony.action.DEFAULT_SMS_SUBSCRIPTION_CHANGED";
    @Deprecated
    public static final String ACTION_DEFAULT_SUBSCRIPTION_CHANGED = "android.telephony.action.DEFAULT_SUBSCRIPTION_CHANGED";
    public static final String ACTION_DEFAULT_VOICE_SUBSCRIPTION_CHANGED = "android.intent.action.ACTION_DEFAULT_VOICE_SUBSCRIPTION_CHANGED";
    public static final String ACTION_EF_CSP_CONTENT_NOTIFY = "com.mediatek.phone.ACTION_EF_CSP_CONTENT_NOTIFY";
    public static final String ACTION_EF_RAT_CONTENT_NOTIFY = "com.mediatek.phone.ACTION_EF_RAT_CONTENT_NOTIFY";
    public static final String ACTION_EMERGENCY_CALLBACK_MODE_CHANGED = "android.intent.action.EMERGENCY_CALLBACK_MODE_CHANGED";
    public static final String ACTION_EMERGENCY_CALL_STATE_CHANGED = "android.intent.action.EMERGENCY_CALL_STATE_CHANGED";
    public static final String ACTION_EXIT_SCBM = "com.mediatek.intent.action.ACTION_EXIT_SCBM";
    @Deprecated
    public static final String ACTION_FORBIDDEN_NO_SERVICE_AUTHORIZATION = "com.android.internal.intent.action.ACTION_FORBIDDEN_NO_SERVICE_AUTHORIZATION";
    public static final String ACTION_IVSR_NOTIFY = "com.mediatek.intent.action.IVSR_NOTIFY";
    public static final String ACTION_LINE1_NUMBER_ERROR_DETECTED = "com.android.internal.telephony.ACTION_LINE1_NUMBER_ERROR_DETECTED";
    public static final String ACTION_LOCATED_PLMN_CHANGED = "com.mediatek.intent.action.LOCATED_PLMN_CHANGED";
    public static final String ACTION_MSIM_MODE_CHANGED = "com.mediatek.intent.action.MSIM_MODE";
    public static final String ACTION_NETWORK_EVENT = "com.mediatek.intent.action.ACTION_NETWORK_EVENT";
    public static final String ACTION_NOTIFY_MODULATION_INFO = "com.mediatek.intent.action.ACTION_NOTIFY_MODULATION_INFO";
    public static final String ACTION_PHB_STATE_CHANGED = "mediatek.intent.action.PHB_STATE_CHANGED";
    public static final String ACTION_PT_LOCAL_SIM_CONFIG_CHANGED = "com.transsion.intent.action.ACTION_PT_LOCAL_SIM_CONFIG_CHANGED";
    public static final String ACTION_RADIO_STATE_CHANGED = "com.mediatek.intent.action.RADIO_STATE_CHANGED";
    public static final String ACTION_RADIO_TECHNOLOGY_CHANGED = "android.intent.action.RADIO_TECHNOLOGY";
    public static final String ACTION_REQUEST_OMADM_CONFIGURATION_UPDATE = "com.android.omadm.service.CONFIGURATION_UPDATE";
    public static final String ACTION_SCBM_CHANGED = "com.mediatek.intent.action.ACTION_SCBM_CHANGED";
    @Deprecated
    public static final String ACTION_SERVICE_STATE_CHANGED = "android.intent.action.SERVICE_STATE";
    public static final String ACTION_SET_RADIO_CAPABILITY_DONE = "android.intent.action.ACTION_SET_RADIO_CAPABILITY_DONE";
    public static final String ACTION_SET_RADIO_CAPABILITY_FAILED = "android.intent.action.ACTION_SET_RADIO_CAPABILITY_FAILED";
    public static final String ACTION_SHOW_NOTICE_ECM_BLOCK_OTHERS = "android.telephony.action.SHOW_NOTICE_ECM_BLOCK_OTHERS";
    public static final String ACTION_SIM_RECOVERY_DONE = "com.mediatek.phone.ACTION_SIM_RECOVERY_DONE";
    public static final String ACTION_SIM_SLOT_LOCK_POLICY_INFORMATION = "com.mediatek.phone.ACTION_SIM_SLOT_LOCK_POLICY_INFORMATION";
    public static final String ACTION_SIM_SLOT_SIM_MOUNT_CHANGE = "com.mediatek.phone.ACTION_SIM_SLOT_SIM_MOUNT_CHANGE";
    public static final String ACTION_SIM_STATE_CHANGED = "android.intent.action.SIM_STATE_CHANGED";
    public static final String ACTION_SUBINFO_CONTENT_CHANGE = "android.intent.action.ACTION_SUBINFO_CONTENT_CHANGE";
    public static final String ACTION_SUBINFO_RECORD_UPDATED = "android.intent.action.ACTION_SUBINFO_RECORD_UPDATED";
    public static final String ACTION_UNLOCK_SIM_LOCK = "com.mediatek.phone.ACTION_UNLOCK_SIM_LOCK";
    public static final String ACTION_USER_ACTIVITY_NOTIFICATION = "android.intent.action.USER_ACTIVITY_NOTIFICATION";
    public static final String ACTION_VOWIFI_ENABLED = "com.android.internal.telephony.ACTION_VOWIFI_ENABLED";
    public static final String EXTRA_APC_INFO = "info";
    public static final String EXTRA_APC_PHONE = "phoneId";
    @Deprecated
    public static final String EXTRA_APN_PROTOCOL = "apnProto";
    @Deprecated
    public static final String EXTRA_APN_PROTOCOL_INT = "apnProtoInt";
    @Deprecated
    public static final String EXTRA_APN_TYPE = "apnType";
    @Deprecated
    public static final String EXTRA_APN_TYPE_INT = "apnTypeInt";
    public static final String EXTRA_CSG_ID = "csgId";
    @Deprecated
    public static final String EXTRA_DEFAULT_NETWORK_AVAILABLE = "defaultNetworkAvailable";
    public static final String EXTRA_DOMAIN = "domain";
    @Deprecated
    public static final String EXTRA_ERROR_CODE = "errorCode";
    public static final String EXTRA_EVENT_TYPE = "eventType";
    public static final String EXTRA_FEMTO = "femtocell";
    public static final String EXTRA_HNB_NAME = "hnbName";
    public static final String EXTRA_ISO = "iso";
    public static final String EXTRA_MODULATION_INFO = "modulation_info";
    public static final String EXTRA_MSIM_MODE = "mode";
    @Deprecated
    public static final String EXTRA_PCO_ID = "pcoId";
    @Deprecated
    public static final String EXTRA_PCO_VALUE = "pcoValue";
    public static final String EXTRA_PLMN_MODE_BIT = "plmn_mode_bit";
    public static final String EXTRA_RADIO_ACCESS_FAMILY = "rafs";
    @Deprecated
    public static final String EXTRA_REDIRECTION_URL = "redirectionUrl";
    public static final String INTENT_KEY_IVSR_ACTION = "action";
    public static final String SECRET_CODE_ACTION = "android.provider.Telephony.SECRET_CODE";
}
