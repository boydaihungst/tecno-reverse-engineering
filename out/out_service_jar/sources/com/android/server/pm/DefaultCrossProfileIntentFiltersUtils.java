package com.android.server.pm;

import com.android.server.pm.DefaultCrossProfileIntentFilter;
import java.util.Arrays;
import java.util.List;
/* loaded from: classes2.dex */
public class DefaultCrossProfileIntentFiltersUtils {
    private static final DefaultCrossProfileIntentFilter EMERGENCY_CALL_MIME = new DefaultCrossProfileIntentFilter.Builder(0, 2, false).addAction("android.intent.action.CALL_EMERGENCY").addAction("android.intent.action.CALL_PRIVILEGED").addCategory("android.intent.category.DEFAULT").addCategory("android.intent.category.BROWSABLE").addDataType("vnd.android.cursor.item/phone").addDataType("vnd.android.cursor.item/phone_v2").addDataType("vnd.android.cursor.item/person").addDataType("vnd.android.cursor.dir/calls").addDataType("vnd.android.cursor.item/calls").build();
    private static final DefaultCrossProfileIntentFilter EMERGENCY_CALL_DATA = new DefaultCrossProfileIntentFilter.Builder(0, 2, false).addAction("android.intent.action.CALL_EMERGENCY").addAction("android.intent.action.CALL_PRIVILEGED").addCategory("android.intent.category.DEFAULT").addCategory("android.intent.category.BROWSABLE").addDataScheme("tel").addDataScheme("sip").addDataScheme("voicemail").build();
    private static final DefaultCrossProfileIntentFilter DIAL_MIME = new DefaultCrossProfileIntentFilter.Builder(0, 4, false).addAction("android.intent.action.DIAL").addAction("android.intent.action.VIEW").addCategory("android.intent.category.DEFAULT").addCategory("android.intent.category.BROWSABLE").addDataType("vnd.android.cursor.item/phone").addDataType("vnd.android.cursor.item/phone_v2").addDataType("vnd.android.cursor.item/person").addDataType("vnd.android.cursor.dir/calls").addDataType("vnd.android.cursor.item/calls").build();
    private static final DefaultCrossProfileIntentFilter DIAL_DATA = new DefaultCrossProfileIntentFilter.Builder(0, 4, false).addAction("android.intent.action.DIAL").addAction("android.intent.action.VIEW").addCategory("android.intent.category.DEFAULT").addCategory("android.intent.category.BROWSABLE").addDataScheme("tel").addDataScheme("sip").addDataScheme("voicemail").build();
    private static final DefaultCrossProfileIntentFilter DIAL_RAW = new DefaultCrossProfileIntentFilter.Builder(0, 4, false).addAction("android.intent.action.DIAL").addCategory("android.intent.category.DEFAULT").addCategory("android.intent.category.BROWSABLE").build();
    private static final DefaultCrossProfileIntentFilter CALL_BUTTON = new DefaultCrossProfileIntentFilter.Builder(0, 4, false).addAction("android.intent.action.CALL_BUTTON").addCategory("android.intent.category.DEFAULT").build();
    private static final DefaultCrossProfileIntentFilter SMS_MMS = new DefaultCrossProfileIntentFilter.Builder(0, 2, false).addAction("android.intent.action.VIEW").addAction("android.intent.action.SENDTO").addCategory("android.intent.category.DEFAULT").addCategory("android.intent.category.BROWSABLE").addDataScheme("sms").addDataScheme("smsto").addDataScheme("mms").addDataScheme("mmsto").build();
    private static final DefaultCrossProfileIntentFilter MOBILE_NETWORK_SETTINGS = new DefaultCrossProfileIntentFilter.Builder(0, 2, false).addAction("android.settings.DATA_ROAMING_SETTINGS").addAction("android.settings.NETWORK_OPERATOR_SETTINGS").addCategory("android.intent.category.DEFAULT").build();
    static final DefaultCrossProfileIntentFilter HOME = new DefaultCrossProfileIntentFilter.Builder(0, 2, false).addAction("android.intent.action.MAIN").addCategory("android.intent.category.DEFAULT").addCategory("android.intent.category.HOME").build();
    private static final DefaultCrossProfileIntentFilter GET_CONTENT = new DefaultCrossProfileIntentFilter.Builder(0, 0, true).addAction("android.intent.action.GET_CONTENT").addCategory("android.intent.category.DEFAULT").addCategory("android.intent.category.OPENABLE").addDataType("*/*").build();
    private static final DefaultCrossProfileIntentFilter ACTION_PICK_IMAGES = new DefaultCrossProfileIntentFilter.Builder(0, 0, true).addAction("android.provider.action.PICK_IMAGES").addCategory("android.intent.category.DEFAULT").build();
    private static final DefaultCrossProfileIntentFilter ACTION_PICK_IMAGES_WITH_DATA_TYPES = new DefaultCrossProfileIntentFilter.Builder(0, 0, true).addAction("android.provider.action.PICK_IMAGES").addCategory("android.intent.category.DEFAULT").addDataType("image/*").addDataType("video/*").build();
    private static final DefaultCrossProfileIntentFilter OPEN_DOCUMENT = new DefaultCrossProfileIntentFilter.Builder(0, 0, true).addAction("android.intent.action.OPEN_DOCUMENT").addCategory("android.intent.category.DEFAULT").addCategory("android.intent.category.OPENABLE").addDataType("*/*").build();
    private static final DefaultCrossProfileIntentFilter ACTION_PICK_DATA = new DefaultCrossProfileIntentFilter.Builder(0, 0, true).addAction("android.intent.action.PICK").addCategory("android.intent.category.DEFAULT").addDataType("*/*").build();
    private static final DefaultCrossProfileIntentFilter ACTION_PICK_RAW = new DefaultCrossProfileIntentFilter.Builder(0, 0, true).addAction("android.intent.action.PICK").addCategory("android.intent.category.DEFAULT").build();
    private static final DefaultCrossProfileIntentFilter RECOGNIZE_SPEECH = new DefaultCrossProfileIntentFilter.Builder(0, 4, false).addAction("android.speech.action.RECOGNIZE_SPEECH").addCategory("android.intent.category.DEFAULT").build();
    private static final DefaultCrossProfileIntentFilter MEDIA_CAPTURE = new DefaultCrossProfileIntentFilter.Builder(0, 4, true).addAction("android.media.action.IMAGE_CAPTURE").addAction("android.media.action.IMAGE_CAPTURE_SECURE").addAction("android.media.action.VIDEO_CAPTURE").addAction("android.provider.MediaStore.RECORD_SOUND").addAction("android.media.action.STILL_IMAGE_CAMERA").addAction("android.media.action.STILL_IMAGE_CAMERA_SECURE").addAction("android.media.action.VIDEO_CAMERA").addCategory("android.intent.category.DEFAULT").build();
    private static final DefaultCrossProfileIntentFilter SET_ALARM = new DefaultCrossProfileIntentFilter.Builder(0, 0, false).addAction("android.intent.action.SET_ALARM").addAction("android.intent.action.SHOW_ALARMS").addAction("android.intent.action.SET_TIMER").addCategory("android.intent.category.DEFAULT").build();
    private static final DefaultCrossProfileIntentFilter ACTION_SEND = new DefaultCrossProfileIntentFilter.Builder(1, 0, true).addAction("android.intent.action.SEND").addAction("android.intent.action.SEND_MULTIPLE").addCategory("android.intent.category.DEFAULT").addDataType("*/*").build();
    private static final DefaultCrossProfileIntentFilter USB_DEVICE_ATTACHED = new DefaultCrossProfileIntentFilter.Builder(1, 0, false).addAction("android.hardware.usb.action.USB_DEVICE_ATTACHED").addAction("android.hardware.usb.action.USB_ACCESSORY_ATTACHED").addCategory("android.intent.category.DEFAULT").build();

    private DefaultCrossProfileIntentFiltersUtils() {
    }

    public static List<DefaultCrossProfileIntentFilter> getDefaultManagedProfileFilters() {
        return Arrays.asList(EMERGENCY_CALL_MIME, EMERGENCY_CALL_DATA, DIAL_MIME, DIAL_DATA, DIAL_RAW, CALL_BUTTON, SMS_MMS, SET_ALARM, MEDIA_CAPTURE, RECOGNIZE_SPEECH, ACTION_PICK_RAW, ACTION_PICK_DATA, ACTION_PICK_IMAGES, ACTION_PICK_IMAGES_WITH_DATA_TYPES, OPEN_DOCUMENT, GET_CONTENT, USB_DEVICE_ATTACHED, ACTION_SEND, HOME, MOBILE_NETWORK_SETTINGS);
    }
}
