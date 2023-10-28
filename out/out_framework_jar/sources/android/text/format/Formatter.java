package android.text.format;

import android.content.Context;
import android.content.res.Resources;
import android.icu.text.MeasureFormat;
import android.icu.util.Measure;
import android.icu.util.MeasureUnit;
import android.text.BidiFormatter;
import android.text.TextUtils;
import com.android.internal.R;
import com.android.net.module.util.Inet4AddressUtils;
import java.util.Locale;
/* loaded from: classes3.dex */
public final class Formatter {
    public static final int FLAG_CALCULATE_ROUNDED = 2;
    public static final int FLAG_IEC_UNITS = 8;
    public static final int FLAG_SHORTER = 1;
    public static final int FLAG_SI_UNITS = 4;
    private static final int MILLIS_PER_MINUTE = 60000;
    private static final int SECONDS_PER_DAY = 86400;
    private static final int SECONDS_PER_HOUR = 3600;
    private static final int SECONDS_PER_MINUTE = 60;

    /* loaded from: classes3.dex */
    public static class BytesResult {
        public final long roundedBytes;
        public final String units;
        public final String value;

        public BytesResult(String value, String units, long roundedBytes) {
            this.value = value;
            this.units = units;
            this.roundedBytes = roundedBytes;
        }
    }

    private static Locale localeFromContext(Context context) {
        return context.getResources().getConfiguration().getLocales().get(0);
    }

    private static String bidiWrap(Context context, String source) {
        Locale locale = localeFromContext(context);
        if (TextUtils.getLayoutDirectionFromLocale(locale) == 1) {
            return BidiFormatter.getInstance(true).unicodeWrap(source);
        }
        return source;
    }

    public static String formatFileSize(Context context, long sizeBytes) {
        return formatFileSize(context, sizeBytes, 4);
    }

    public static String formatFileSize(Context context, long sizeBytes, int flags) {
        if (context == null) {
            return "";
        }
        BytesResult res = formatBytes(context.getResources(), sizeBytes, flags);
        return bidiWrap(context, context.getString(R.string.fileSizeSuffix, res.value, res.units));
    }

    public static String formatShortFileSize(Context context, long sizeBytes) {
        if (context == null) {
            return "";
        }
        BytesResult res = formatBytes(context.getResources(), sizeBytes, 5);
        return bidiWrap(context, context.getString(R.string.fileSizeSuffix, res.value, res.units));
    }

    public static BytesResult formatBytes(Resources res, long sizeBytes, int flags) {
        int roundFactor;
        String roundFormat;
        int unit = (flags & 8) != 0 ? 1024 : 1000;
        long roundedBytes = 0;
        boolean isNegative = sizeBytes < 0;
        float result = isNegative ? (float) (-sizeBytes) : (float) sizeBytes;
        int suffix = R.string.byteShort;
        long mult = 1;
        if (result > 900.0f) {
            suffix = R.string.kilobyteShort;
            mult = unit;
            result /= unit;
        }
        if (result > 900.0f) {
            suffix = R.string.megabyteShort;
            mult *= unit;
            result /= unit;
        }
        if (result > 900.0f) {
            suffix = R.string.gigabyteShort;
            mult *= unit;
            result /= unit;
        }
        if (result > 900.0f) {
            suffix = R.string.terabyteShort;
            mult *= unit;
            result /= unit;
        }
        if (result > 900.0f) {
            suffix = R.string.petabyteShort;
            mult *= unit;
            result /= unit;
        }
        if (mult == 1 || result >= 100.0f) {
            roundFactor = 1;
            roundFormat = "%.0f";
        } else if (result < 1.0f) {
            roundFactor = 100;
            roundFormat = "%.2f";
        } else if (result < 10.0f) {
            if ((flags & 1) != 0) {
                roundFactor = 10;
                roundFormat = "%.1f";
            } else {
                roundFactor = 100;
                roundFormat = "%.2f";
            }
        } else {
            int roundFactor2 = flags & 1;
            if (roundFactor2 != 0) {
                roundFactor = 1;
                roundFormat = "%.0f";
            } else {
                roundFactor = 100;
                roundFormat = "%.2f";
            }
        }
        if (isNegative) {
            result = -result;
        }
        String roundedString = String.format(roundFormat, Float.valueOf(result));
        if ((flags & 2) != 0) {
            roundedBytes = (Math.round(roundFactor * result) * mult) / roundFactor;
        }
        String units = res.getString(suffix);
        return new BytesResult(roundedString, units, roundedBytes);
    }

    @Deprecated
    public static String formatIpAddress(int ipv4Address) {
        return Inet4AddressUtils.intToInet4AddressHTL(ipv4Address).getHostAddress();
    }

    public static String formatShortElapsedTime(Context context, long millis) {
        long secondsLong = millis / 1000;
        int days = 0;
        int hours = 0;
        int minutes = 0;
        if (secondsLong >= 86400) {
            days = (int) (secondsLong / 86400);
            secondsLong -= SECONDS_PER_DAY * days;
        }
        if (secondsLong >= 3600) {
            hours = (int) (secondsLong / 3600);
            secondsLong -= hours * 3600;
        }
        if (secondsLong >= 60) {
            minutes = (int) (secondsLong / 60);
            secondsLong -= minutes * 60;
        }
        int seconds = (int) secondsLong;
        Locale locale = localeFromContext(context);
        MeasureFormat measureFormat = MeasureFormat.getInstance(locale, MeasureFormat.FormatWidth.SHORT);
        if (days >= 2 || (days > 0 && hours == 0)) {
            return measureFormat.format(new Measure(Integer.valueOf(days + ((hours + 12) / 24)), MeasureUnit.DAY));
        }
        if (days > 0) {
            return measureFormat.formatMeasures(new Measure(Integer.valueOf(days), MeasureUnit.DAY), new Measure(Integer.valueOf(hours), MeasureUnit.HOUR));
        }
        if (hours >= 2 || (hours > 0 && minutes == 0)) {
            return measureFormat.format(new Measure(Integer.valueOf(hours + ((minutes + 30) / 60)), MeasureUnit.HOUR));
        }
        if (hours > 0) {
            return measureFormat.formatMeasures(new Measure(Integer.valueOf(hours), MeasureUnit.HOUR), new Measure(Integer.valueOf(minutes), MeasureUnit.MINUTE));
        }
        if (minutes >= 2 || (minutes > 0 && seconds == 0)) {
            return measureFormat.format(new Measure(Integer.valueOf(minutes + ((seconds + 30) / 60)), MeasureUnit.MINUTE));
        }
        if (minutes > 0) {
            return measureFormat.formatMeasures(new Measure(Integer.valueOf(minutes), MeasureUnit.MINUTE), new Measure(Integer.valueOf(seconds), MeasureUnit.SECOND));
        }
        return measureFormat.format(new Measure(Integer.valueOf(seconds), MeasureUnit.SECOND));
    }

    public static String formatShortElapsedTimeRoundingUpToMinutes(Context context, long millis) {
        long minutesRoundedUp = ((millis + 60000) - 1) / 60000;
        if (minutesRoundedUp == 0 || minutesRoundedUp == 1) {
            Locale locale = localeFromContext(context);
            MeasureFormat measureFormat = MeasureFormat.getInstance(locale, MeasureFormat.FormatWidth.SHORT);
            return measureFormat.format(new Measure(Long.valueOf(minutesRoundedUp), MeasureUnit.MINUTE));
        }
        return formatShortElapsedTime(context, 60000 * minutesRoundedUp);
    }
}
