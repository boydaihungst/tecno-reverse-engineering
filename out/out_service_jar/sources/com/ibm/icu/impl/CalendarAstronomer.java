package com.ibm.icu.impl;

import java.util.Date;
import java.util.TimeZone;
/* loaded from: classes2.dex */
public class CalendarAstronomer {
    public static final long DAY_MS = 86400000;
    private static final double DEG_RAD = 0.017453292519943295d;
    static final long EPOCH_2000_MS = 946598400000L;
    public static final int HOUR_MS = 3600000;
    private static final double INVALID = Double.MIN_VALUE;
    static final double JD_EPOCH = 2447891.5d;
    public static final long JULIAN_EPOCH_MS = -210866760000000L;
    public static final int MINUTE_MS = 60000;
    private static final double PI2 = 6.283185307179586d;
    private static final double RAD_DEG = 57.29577951308232d;
    private static final double RAD_HOUR = 3.819718634205488d;
    public static final int SECOND_MS = 1000;
    public static final double SIDEREAL_DAY = 23.93446960027d;
    public static final double SIDEREAL_MONTH = 27.32166d;
    public static final double SIDEREAL_YEAR = 365.25636d;
    public static final double SOLAR_DAY = 24.065709816d;
    static final double SUN_E = 0.016713d;
    static final double SUN_ETA_G = 4.87650757829735d;
    static final double SUN_OMEGA_G = 4.935239984568769d;
    public static final double SYNODIC_MONTH = 29.530588853d;
    public static final double TROPICAL_YEAR = 365.242191d;
    static final double moonA = 384401.0d;
    static final double moonE = 0.0549d;
    static final double moonI = 0.08980357792017056d;
    static final double moonL0 = 5.556284436750021d;
    static final double moonN0 = 5.559050068029439d;
    static final double moonP0 = 0.6342598060246725d;
    static final double moonPi = 0.016592845198710092d;
    static final double moonT0 = 0.009042550854582622d;
    private transient double eclipObliquity;
    private long fGmtOffset;
    private double fLatitude;
    private double fLongitude;
    private transient double julianCentury;
    private transient double julianDay;
    private transient double meanAnomalySun;
    private transient double moonEclipLong;
    private transient double moonLongitude;
    private transient Equatorial moonPosition;
    private transient double siderealT0;
    private transient double siderealTime;
    private transient double sunLongitude;
    private long time;
    public static final SolarLongitude VERNAL_EQUINOX = new SolarLongitude(0.0d);
    public static final SolarLongitude SUMMER_SOLSTICE = new SolarLongitude(1.5707963267948966d);
    private static final double PI = 3.141592653589793d;
    public static final SolarLongitude AUTUMN_EQUINOX = new SolarLongitude(PI);
    public static final SolarLongitude WINTER_SOLSTICE = new SolarLongitude(4.71238898038469d);
    public static final MoonAge NEW_MOON = new MoonAge(0.0d);
    public static final MoonAge FIRST_QUARTER = new MoonAge(1.5707963267948966d);
    public static final MoonAge FULL_MOON = new MoonAge(PI);
    public static final MoonAge LAST_QUARTER = new MoonAge(4.71238898038469d);

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public interface AngleFunc {
        double eval();
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public interface CoordFunc {
        Equatorial eval();
    }

    public CalendarAstronomer() {
        this(System.currentTimeMillis());
    }

    public CalendarAstronomer(Date d) {
        this(d.getTime());
    }

    public CalendarAstronomer(long aTime) {
        this.fLongitude = 0.0d;
        this.fLatitude = 0.0d;
        this.fGmtOffset = 0L;
        this.julianDay = INVALID;
        this.julianCentury = INVALID;
        this.sunLongitude = INVALID;
        this.meanAnomalySun = INVALID;
        this.moonLongitude = INVALID;
        this.moonEclipLong = INVALID;
        this.eclipObliquity = INVALID;
        this.siderealT0 = INVALID;
        this.siderealTime = INVALID;
        this.moonPosition = null;
        this.time = aTime;
    }

    public CalendarAstronomer(double longitude, double latitude) {
        this();
        this.fLongitude = normPI(longitude * DEG_RAD);
        this.fLatitude = normPI(DEG_RAD * latitude);
        this.fGmtOffset = (long) (((this.fLongitude * 24.0d) * 3600000.0d) / PI2);
    }

    public void setTime(long aTime) {
        this.time = aTime;
        clearCache();
    }

    public void setDate(Date date) {
        setTime(date.getTime());
    }

    public void setJulianDay(double jdn) {
        this.time = ((long) (8.64E7d * jdn)) + JULIAN_EPOCH_MS;
        clearCache();
        this.julianDay = jdn;
    }

    public long getTime() {
        return this.time;
    }

    public Date getDate() {
        return new Date(this.time);
    }

    public double getJulianDay() {
        if (this.julianDay == INVALID) {
            this.julianDay = (this.time - JULIAN_EPOCH_MS) / 8.64E7d;
        }
        return this.julianDay;
    }

    public double getJulianCentury() {
        if (this.julianCentury == INVALID) {
            this.julianCentury = (getJulianDay() - 2415020.0d) / 36525.0d;
        }
        return this.julianCentury;
    }

    public double getGreenwichSidereal() {
        if (this.siderealTime == INVALID) {
            double UT = normalize(this.time / 3600000.0d, 24.0d);
            this.siderealTime = normalize(getSiderealOffset() + (1.002737909d * UT), 24.0d);
        }
        double UT2 = this.siderealTime;
        return UT2;
    }

    private double getSiderealOffset() {
        if (this.siderealT0 == INVALID) {
            double JD = Math.floor(getJulianDay() - 0.5d) + 0.5d;
            double S = JD - 2451545.0d;
            double T = S / 36525.0d;
            this.siderealT0 = normalize((2400.051336d * T) + 6.697374558d + (2.5862E-5d * T * T), 24.0d);
        }
        double JD2 = this.siderealT0;
        return JD2;
    }

    public double getLocalSidereal() {
        return normalize(getGreenwichSidereal() + (this.fGmtOffset / 3600000.0d), 24.0d);
    }

    private long lstToUT(double lst) {
        double lt = normalize((lst - getSiderealOffset()) * 0.9972695663d, 24.0d);
        long j = this.time;
        long j2 = this.fGmtOffset;
        long base = (((j + j2) / 86400000) * 86400000) - j2;
        return ((long) (3600000.0d * lt)) + base;
    }

    public final Equatorial eclipticToEquatorial(Ecliptic ecliptic) {
        return eclipticToEquatorial(ecliptic.longitude, ecliptic.latitude);
    }

    public final Equatorial eclipticToEquatorial(double eclipLong, double eclipLat) {
        double obliq = eclipticObliquity();
        double sinE = Math.sin(obliq);
        double cosE = Math.cos(obliq);
        double sinL = Math.sin(eclipLong);
        double cosL = Math.cos(eclipLong);
        double sinB = Math.sin(eclipLat);
        double cosB = Math.cos(eclipLat);
        double tanB = Math.tan(eclipLat);
        return new Equatorial(Math.atan2((sinL * cosE) - (tanB * sinE), cosL), Math.asin((sinB * cosE) + (cosB * sinE * sinL)));
    }

    public final Equatorial eclipticToEquatorial(double eclipLong) {
        return eclipticToEquatorial(eclipLong, 0.0d);
    }

    public Horizon eclipticToHorizon(double eclipLong) {
        Equatorial equatorial = eclipticToEquatorial(eclipLong);
        double H = ((getLocalSidereal() * PI) / 12.0d) - equatorial.ascension;
        double sinH = Math.sin(H);
        double cosH = Math.cos(H);
        double sinD = Math.sin(equatorial.declination);
        double cosD = Math.cos(equatorial.declination);
        double sinL = Math.sin(this.fLatitude);
        double cosL = Math.cos(this.fLatitude);
        double altitude = Math.asin((sinD * sinL) + (cosD * cosL * cosH));
        double d = (-cosD) * cosL * sinH;
        double sinH2 = sinD - (Math.sin(altitude) * sinL);
        double azimuth = Math.atan2(d, sinH2);
        return new Horizon(azimuth, altitude);
    }

    public double getSunLongitude() {
        if (this.sunLongitude == INVALID) {
            double[] result = getSunLongitude(getJulianDay());
            this.sunLongitude = result[0];
            this.meanAnomalySun = result[1];
        }
        return this.sunLongitude;
    }

    double[] getSunLongitude(double julian) {
        double day = julian - JD_EPOCH;
        double epochAngle = norm2PI(0.017202791632524146d * day);
        double meanAnomaly = norm2PI((SUN_ETA_G + epochAngle) - SUN_OMEGA_G);
        return new double[]{norm2PI(trueAnomaly(meanAnomaly, SUN_E) + SUN_OMEGA_G), meanAnomaly};
    }

    public Equatorial getSunPosition() {
        return eclipticToEquatorial(getSunLongitude(), 0.0d);
    }

    /* loaded from: classes2.dex */
    private static class SolarLongitude {
        double value;

        SolarLongitude(double val) {
            this.value = val;
        }
    }

    public long getSunTime(double desired, boolean next) {
        return timeOfAngle(new AngleFunc() { // from class: com.ibm.icu.impl.CalendarAstronomer.1
            @Override // com.ibm.icu.impl.CalendarAstronomer.AngleFunc
            public double eval() {
                return CalendarAstronomer.this.getSunLongitude();
            }
        }, desired, 365.242191d, 60000L, next);
    }

    public long getSunTime(SolarLongitude desired, boolean next) {
        return getSunTime(desired.value, next);
    }

    public long getSunRiseSet(boolean rise) {
        long t0 = this.time;
        long j = this.time;
        long j2 = this.fGmtOffset;
        long noon = ((((j + j2) / 86400000) * 86400000) - j2) + 43200000;
        setTime(((rise ? -6L : 6L) * 3600000) + noon);
        long t = riseOrSet(new CoordFunc() { // from class: com.ibm.icu.impl.CalendarAstronomer.2
            @Override // com.ibm.icu.impl.CalendarAstronomer.CoordFunc
            public Equatorial eval() {
                return CalendarAstronomer.this.getSunPosition();
            }
        }, rise, 0.009302604913129777d, 0.009890199094634533d, 5000L);
        setTime(t0);
        return t;
    }

    public Equatorial getMoonPosition() {
        if (this.moonPosition == null) {
            double sunLong = getSunLongitude();
            double day = getJulianDay() - JD_EPOCH;
            double meanLongitude = norm2PI((0.22997150421858628d * day) + moonL0);
            double meanAnomalyMoon = norm2PI((meanLongitude - (0.001944368345221015d * day)) - moonP0);
            double evection = Math.sin(((meanLongitude - sunLong) * 2.0d) - meanAnomalyMoon) * 0.022233749341155764d;
            double annual = Math.sin(this.meanAnomalySun) * 0.003242821750205464d;
            double a3 = Math.sin(this.meanAnomalySun) * 0.00645771823237902d;
            double meanAnomalyMoon2 = meanAnomalyMoon + ((evection - annual) - a3);
            double center = Math.sin(meanAnomalyMoon2) * 0.10975677534091541d;
            double a4 = Math.sin(meanAnomalyMoon2 * 2.0d) * 0.0037350045992678655d;
            double d = (((meanLongitude + evection) + center) - annual) + a4;
            this.moonLongitude = d;
            double variation = Math.sin((d - sunLong) * 2.0d) * 0.011489502465878671d;
            this.moonLongitude += variation;
            double nodeLongitude = norm2PI(moonN0 - (9.242199067718253E-4d * day));
            double a32 = this.meanAnomalySun;
            double nodeLongitude2 = nodeLongitude - (Math.sin(a32) * 0.0027925268031909274d);
            double y = Math.sin(this.moonLongitude - nodeLongitude2);
            double x = Math.cos(this.moonLongitude - nodeLongitude2);
            this.moonEclipLong = Math.atan2(y * Math.cos(moonI), x) + nodeLongitude2;
            double moonEclipLat = Math.asin(Math.sin(moonI) * y);
            double nodeLongitude3 = this.moonEclipLong;
            this.moonPosition = eclipticToEquatorial(nodeLongitude3, moonEclipLat);
        }
        return this.moonPosition;
    }

    public double getMoonAge() {
        getMoonPosition();
        return norm2PI(this.moonEclipLong - this.sunLongitude);
    }

    public double getMoonPhase() {
        return (1.0d - Math.cos(getMoonAge())) * 0.5d;
    }

    /* loaded from: classes2.dex */
    private static class MoonAge {
        double value;

        MoonAge(double val) {
            this.value = val;
        }
    }

    public long getMoonTime(double desired, boolean next) {
        return timeOfAngle(new AngleFunc() { // from class: com.ibm.icu.impl.CalendarAstronomer.3
            @Override // com.ibm.icu.impl.CalendarAstronomer.AngleFunc
            public double eval() {
                return CalendarAstronomer.this.getMoonAge();
            }
        }, desired, 29.530588853d, 60000L, next);
    }

    public long getMoonTime(MoonAge desired, boolean next) {
        return getMoonTime(desired.value, next);
    }

    public long getMoonRiseSet(boolean rise) {
        return riseOrSet(new CoordFunc() { // from class: com.ibm.icu.impl.CalendarAstronomer.4
            @Override // com.ibm.icu.impl.CalendarAstronomer.CoordFunc
            public Equatorial eval() {
                return CalendarAstronomer.this.getMoonPosition();
            }
        }, rise, 0.009302604913129777d, 0.009890199094634533d, 60000L);
    }

    private long timeOfAngle(AngleFunc func, double desired, double periodDays, long epsilon, boolean next) {
        double lastAngle = func.eval();
        double deltaAngle = norm2PI(desired - lastAngle);
        double deltaT = (((next ? 0.0d : -6.283185307179586d) + deltaAngle) * (periodDays * 8.64E7d)) / PI2;
        double lastDeltaT = deltaT;
        long startTime = this.time;
        setTime(this.time + ((long) deltaT));
        while (true) {
            double angle = func.eval();
            double factor = Math.abs(deltaT / normPI(angle - lastAngle));
            deltaT = normPI(desired - angle) * factor;
            if (Math.abs(deltaT) <= Math.abs(lastDeltaT)) {
                double deltaAngle2 = deltaAngle;
                lastDeltaT = deltaT;
                setTime(this.time + ((long) deltaT));
                if (Math.abs(deltaT) > epsilon) {
                    deltaAngle = deltaAngle2;
                    lastAngle = angle;
                } else {
                    return this.time;
                }
            } else {
                long delta = (long) ((8.64E7d * periodDays) / 8.0d);
                setTime((next ? delta : -delta) + startTime);
                return timeOfAngle(func, desired, periodDays, epsilon, next);
            }
        }
    }

    private long riseOrSet(CoordFunc func, boolean rise, double diameter, double refraction, long epsilon) {
        Equatorial pos;
        long deltaT;
        double tanL = Math.tan(this.fLatitude);
        int count = 0;
        do {
            pos = func.eval();
            double angle = Math.acos((-tanL) * Math.tan(pos.declination));
            double lst = (((rise ? PI2 - angle : angle) + pos.ascension) * 24.0d) / PI2;
            long newTime = lstToUT(lst);
            deltaT = newTime - this.time;
            setTime(newTime);
            count++;
            if (count >= 5) {
                break;
            }
        } while (Math.abs(deltaT) > epsilon);
        double cosD = Math.cos(pos.declination);
        double psi = Math.acos(Math.sin(this.fLatitude) / cosD);
        double x = (diameter / 2.0d) + refraction;
        double y = Math.asin(Math.sin(x) / Math.sin(psi));
        long delta = (long) ((((240.0d * y) * RAD_DEG) / cosD) * 1000.0d);
        return this.time + (rise ? -delta : delta);
    }

    private static final double normalize(double value, double range) {
        return value - (Math.floor(value / range) * range);
    }

    private static final double norm2PI(double angle) {
        return normalize(angle, PI2);
    }

    private static final double normPI(double angle) {
        return normalize(angle + PI, PI2) - PI;
    }

    private double trueAnomaly(double meanAnomaly, double eccentricity) {
        double delta;
        double E = meanAnomaly;
        do {
            delta = (E - (Math.sin(E) * eccentricity)) - meanAnomaly;
            E -= delta / (1.0d - (Math.cos(E) * eccentricity));
        } while (Math.abs(delta) > 1.0E-5d);
        return Math.atan(Math.tan(E / 2.0d) * Math.sqrt((eccentricity + 1.0d) / (1.0d - eccentricity))) * 2.0d;
    }

    private double eclipticObliquity() {
        if (this.eclipObliquity == INVALID) {
            double T = (getJulianDay() - 2451545.0d) / 36525.0d;
            double d = ((23.439292d - (0.013004166666666666d * T)) - ((1.6666666666666665E-7d * T) * T)) + (5.027777777777778E-7d * T * T * T);
            this.eclipObliquity = d;
            this.eclipObliquity = d * DEG_RAD;
        }
        double epoch = this.eclipObliquity;
        return epoch;
    }

    private void clearCache() {
        this.julianDay = INVALID;
        this.julianCentury = INVALID;
        this.sunLongitude = INVALID;
        this.meanAnomalySun = INVALID;
        this.moonLongitude = INVALID;
        this.moonEclipLong = INVALID;
        this.eclipObliquity = INVALID;
        this.siderealTime = INVALID;
        this.siderealT0 = INVALID;
        this.moonPosition = null;
    }

    public String local(long localMillis) {
        return new Date(localMillis - TimeZone.getDefault().getRawOffset()).toString();
    }

    /* loaded from: classes2.dex */
    public static final class Ecliptic {
        public final double latitude;
        public final double longitude;

        public Ecliptic(double lat, double lon) {
            this.latitude = lat;
            this.longitude = lon;
        }

        public String toString() {
            return Double.toString(this.longitude * CalendarAstronomer.RAD_DEG) + "," + (this.latitude * CalendarAstronomer.RAD_DEG);
        }
    }

    /* loaded from: classes2.dex */
    public static final class Equatorial {
        public final double ascension;
        public final double declination;

        public Equatorial(double asc, double dec) {
            this.ascension = asc;
            this.declination = dec;
        }

        public String toString() {
            return Double.toString(this.ascension * CalendarAstronomer.RAD_DEG) + "," + (this.declination * CalendarAstronomer.RAD_DEG);
        }

        public String toHmsString() {
            return CalendarAstronomer.radToHms(this.ascension) + "," + CalendarAstronomer.radToDms(this.declination);
        }
    }

    /* loaded from: classes2.dex */
    public static final class Horizon {
        public final double altitude;
        public final double azimuth;

        public Horizon(double alt, double azim) {
            this.altitude = alt;
            this.azimuth = azim;
        }

        public String toString() {
            return Double.toString(this.altitude * CalendarAstronomer.RAD_DEG) + "," + (this.azimuth * CalendarAstronomer.RAD_DEG);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static String radToHms(double angle) {
        int hrs = (int) (angle * RAD_HOUR);
        int min = (int) (((angle * RAD_HOUR) - hrs) * 60.0d);
        int sec = (int) ((((RAD_HOUR * angle) - hrs) - (min / 60.0d)) * 3600.0d);
        return Integer.toString(hrs) + "h" + min + "m" + sec + "s";
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static String radToDms(double angle) {
        int deg = (int) (angle * RAD_DEG);
        int min = (int) (((angle * RAD_DEG) - deg) * 60.0d);
        int sec = (int) ((((RAD_DEG * angle) - deg) - (min / 60.0d)) * 3600.0d);
        return Integer.toString(deg) + "°" + min + "'" + sec + "\"";
    }
}
