package com.android.server.blob;

import android.content.Context;
import android.content.pm.PackageManager;
import android.util.ArraySet;
import android.util.Base64;
import android.util.DebugUtils;
import android.util.IndentingPrintWriter;
import com.android.internal.util.XmlUtils;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.Objects;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;
/* loaded from: classes.dex */
class BlobAccessMode {
    public static final int ACCESS_TYPE_ALLOWLIST = 8;
    public static final int ACCESS_TYPE_PRIVATE = 1;
    public static final int ACCESS_TYPE_PUBLIC = 2;
    public static final int ACCESS_TYPE_SAME_SIGNATURE = 4;
    private int mAccessType = 1;
    private final ArraySet<PackageIdentifier> mAllowedPackages = new ArraySet<>();

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes.dex */
    @interface AccessType {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void allow(BlobAccessMode other) {
        if ((other.mAccessType & 8) != 0) {
            this.mAllowedPackages.addAll((ArraySet<? extends PackageIdentifier>) other.mAllowedPackages);
        }
        this.mAccessType |= other.mAccessType;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void allowPublicAccess() {
        this.mAccessType |= 2;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void allowSameSignatureAccess() {
        this.mAccessType |= 4;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void allowPackageAccess(String packageName, byte[] certificate) {
        this.mAccessType |= 8;
        this.mAllowedPackages.add(PackageIdentifier.create(packageName, certificate));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isPublicAccessAllowed() {
        return (this.mAccessType & 2) != 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isSameSignatureAccessAllowed() {
        return (this.mAccessType & 4) != 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isPackageAccessAllowed(String packageName, byte[] certificate) {
        if ((this.mAccessType & 8) == 0) {
            return false;
        }
        return this.mAllowedPackages.contains(PackageIdentifier.create(packageName, certificate));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isAccessAllowedForCaller(Context context, String callingPackage, String committerPackage) {
        if ((this.mAccessType & 2) != 0) {
            return true;
        }
        PackageManager pm = context.getPackageManager();
        if ((this.mAccessType & 4) == 0 || pm.checkSignatures(committerPackage, callingPackage) != 0) {
            if ((this.mAccessType & 8) != 0) {
                for (int i = 0; i < this.mAllowedPackages.size(); i++) {
                    PackageIdentifier packageIdentifier = this.mAllowedPackages.valueAt(i);
                    if (packageIdentifier.packageName.equals(callingPackage) && pm.hasSigningCertificate(callingPackage, packageIdentifier.certificate, 1)) {
                        return true;
                    }
                }
                return false;
            }
            return false;
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getAccessType() {
        return this.mAccessType;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getAllowedPackagesCount() {
        return this.mAllowedPackages.size();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dump(IndentingPrintWriter fout) {
        fout.println("accessType: " + DebugUtils.flagsToString(BlobAccessMode.class, "ACCESS_TYPE_", this.mAccessType));
        fout.print("Explicitly allowed pkgs:");
        if (this.mAllowedPackages.isEmpty()) {
            fout.println(" (Empty)");
            return;
        }
        fout.increaseIndent();
        int count = this.mAllowedPackages.size();
        for (int i = 0; i < count; i++) {
            fout.println(this.mAllowedPackages.valueAt(i).toString());
        }
        fout.decreaseIndent();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void writeToXml(XmlSerializer out) throws IOException {
        XmlUtils.writeIntAttribute(out, "t", this.mAccessType);
        int count = this.mAllowedPackages.size();
        for (int i = 0; i < count; i++) {
            out.startTag(null, "wl");
            PackageIdentifier packageIdentifier = this.mAllowedPackages.valueAt(i);
            XmlUtils.writeStringAttribute(out, "p", packageIdentifier.packageName);
            XmlUtils.writeByteArrayAttribute(out, "ct", packageIdentifier.certificate);
            out.endTag(null, "wl");
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static BlobAccessMode createFromXml(XmlPullParser in) throws IOException, XmlPullParserException {
        BlobAccessMode blobAccessMode = new BlobAccessMode();
        int accessType = XmlUtils.readIntAttribute(in, "t");
        blobAccessMode.mAccessType = accessType;
        int depth = in.getDepth();
        while (XmlUtils.nextElementWithin(in, depth)) {
            if ("wl".equals(in.getName())) {
                String packageName = XmlUtils.readStringAttribute(in, "p");
                byte[] certificate = XmlUtils.readByteArrayAttribute(in, "ct");
                blobAccessMode.allowPackageAccess(packageName, certificate);
            }
        }
        return blobAccessMode;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static final class PackageIdentifier {
        public final byte[] certificate;
        public final String packageName;

        private PackageIdentifier(String packageName, byte[] certificate) {
            this.packageName = packageName;
            this.certificate = certificate;
        }

        public static PackageIdentifier create(String packageName, byte[] certificate) {
            return new PackageIdentifier(packageName, certificate);
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || !(obj instanceof PackageIdentifier)) {
                return false;
            }
            PackageIdentifier other = (PackageIdentifier) obj;
            if (this.packageName.equals(other.packageName) && Arrays.equals(this.certificate, other.certificate)) {
                return true;
            }
            return false;
        }

        public int hashCode() {
            return Objects.hash(this.packageName, Integer.valueOf(Arrays.hashCode(this.certificate)));
        }

        public String toString() {
            return "[" + this.packageName + ", " + Base64.encodeToString(this.certificate, 2) + "]";
        }
    }
}
