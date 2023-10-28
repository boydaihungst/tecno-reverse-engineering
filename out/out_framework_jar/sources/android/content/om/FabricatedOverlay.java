package android.content.om;

import android.os.FabricatedOverlayInternal;
import android.os.FabricatedOverlayInternalEntry;
import android.text.TextUtils;
import com.android.internal.util.Preconditions;
import java.util.ArrayList;
/* loaded from: classes.dex */
public class FabricatedOverlay {
    final FabricatedOverlayInternal mOverlay;

    public OverlayIdentifier getIdentifier() {
        return new OverlayIdentifier(this.mOverlay.packageName, TextUtils.nullIfEmpty(this.mOverlay.overlayName));
    }

    /* loaded from: classes.dex */
    public static class Builder {
        private final String mName;
        private final String mOwningPackage;
        private final String mTargetPackage;
        private String mTargetOverlayable = "";
        private final ArrayList<FabricatedOverlayInternalEntry> mEntries = new ArrayList<>();

        public Builder(String owningPackage, String name, String targetPackage) {
            Preconditions.checkStringNotEmpty(owningPackage, "'owningPackage' must not be empty nor null");
            Preconditions.checkStringNotEmpty(name, "'name'' must not be empty nor null");
            Preconditions.checkStringNotEmpty(targetPackage, "'targetPackage' must not be empty nor null");
            this.mOwningPackage = owningPackage;
            this.mName = name;
            this.mTargetPackage = targetPackage;
        }

        public Builder setTargetOverlayable(String targetOverlayable) {
            this.mTargetOverlayable = TextUtils.emptyIfNull(targetOverlayable);
            return this;
        }

        public Builder setResourceValue(String resourceName, int dataType, int value) {
            FabricatedOverlayInternalEntry entry = new FabricatedOverlayInternalEntry();
            entry.resourceName = resourceName;
            entry.dataType = dataType;
            entry.data = value;
            this.mEntries.add(entry);
            return this;
        }

        public FabricatedOverlay build() {
            FabricatedOverlayInternal overlay = new FabricatedOverlayInternal();
            overlay.packageName = this.mOwningPackage;
            overlay.overlayName = this.mName;
            overlay.targetPackageName = this.mTargetPackage;
            overlay.targetOverlayable = this.mTargetOverlayable;
            overlay.entries = new ArrayList();
            overlay.entries.addAll(this.mEntries);
            return new FabricatedOverlay(overlay);
        }
    }

    private FabricatedOverlay(FabricatedOverlayInternal overlay) {
        this.mOverlay = overlay;
    }
}
