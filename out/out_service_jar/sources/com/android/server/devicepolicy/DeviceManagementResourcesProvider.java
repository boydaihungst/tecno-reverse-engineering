package com.android.server.devicepolicy;

import android.app.admin.DevicePolicyDrawableResource;
import android.app.admin.DevicePolicyStringResource;
import android.app.admin.ParcelableResource;
import android.os.Environment;
import android.util.AtomicFile;
import android.util.Log;
import android.util.TypedXmlPullParser;
import android.util.TypedXmlSerializer;
import android.util.Xml;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import libcore.io.IoUtils;
import org.xmlpull.v1.XmlPullParserException;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class DeviceManagementResourcesProvider {
    private static final String ATTR_DRAWABLE_ID = "drawable-id";
    private static final String ATTR_DRAWABLE_SOURCE = "drawable-source";
    private static final String ATTR_DRAWABLE_STYLE = "drawable-style";
    private static final String ATTR_SOURCE_ID = "source-id";
    private static final String TAG = "DevicePolicyManagerService";
    private static final String TAG_DRAWABLE_SOURCE_ENTRY = "drawable-source-entry";
    private static final String TAG_DRAWABLE_STYLE_ENTRY = "drawable-style-entry";
    private static final String TAG_ROOT = "root";
    private static final String TAG_STRING_ENTRY = "string-entry";
    private static final String UPDATED_RESOURCES_XML = "updated_resources.xml";
    private final Injector mInjector;
    private final Object mLock;
    private final Map<String, Map<String, Map<String, ParcelableResource>>> mUpdatedDrawablesForSource;
    private final Map<String, Map<String, ParcelableResource>> mUpdatedDrawablesForStyle;
    private final Map<String, ParcelableResource> mUpdatedStrings;

    /* JADX INFO: Access modifiers changed from: package-private */
    public DeviceManagementResourcesProvider() {
        this(new Injector());
    }

    DeviceManagementResourcesProvider(Injector injector) {
        this.mUpdatedDrawablesForStyle = new HashMap();
        this.mUpdatedDrawablesForSource = new HashMap();
        this.mUpdatedStrings = new HashMap();
        this.mLock = new Object();
        this.mInjector = (Injector) Objects.requireNonNull(injector);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean updateDrawables(List<DevicePolicyDrawableResource> drawables) {
        boolean updateDrawableForSource;
        boolean updated = false;
        for (int i = 0; i < drawables.size(); i++) {
            String drawableId = drawables.get(i).getDrawableId();
            String drawableStyle = drawables.get(i).getDrawableStyle();
            String drawableSource = drawables.get(i).getDrawableSource();
            ParcelableResource resource = drawables.get(i).getResource();
            Objects.requireNonNull(drawableId, "drawableId must be provided.");
            Objects.requireNonNull(drawableStyle, "drawableStyle must be provided.");
            Objects.requireNonNull(drawableSource, "drawableSource must be provided.");
            Objects.requireNonNull(resource, "ParcelableResource must be provided.");
            if ("UNDEFINED".equals(drawableSource)) {
                updateDrawableForSource = updateDrawable(drawableId, drawableStyle, resource);
            } else {
                updateDrawableForSource = updateDrawableForSource(drawableId, drawableSource, drawableStyle, resource);
            }
            updated |= updateDrawableForSource;
        }
        if (!updated) {
            return false;
        }
        synchronized (this.mLock) {
            write();
        }
        return true;
    }

    private boolean updateDrawable(String drawableId, String drawableStyle, ParcelableResource updatableResource) {
        synchronized (this.mLock) {
            if (!this.mUpdatedDrawablesForStyle.containsKey(drawableId)) {
                this.mUpdatedDrawablesForStyle.put(drawableId, new HashMap());
            }
            ParcelableResource current = this.mUpdatedDrawablesForStyle.get(drawableId).get(drawableStyle);
            if (updatableResource.equals(current)) {
                return false;
            }
            this.mUpdatedDrawablesForStyle.get(drawableId).put(drawableStyle, updatableResource);
            return true;
        }
    }

    private boolean updateDrawableForSource(String drawableId, String drawableSource, String drawableStyle, ParcelableResource updatableResource) {
        synchronized (this.mLock) {
            if (!this.mUpdatedDrawablesForSource.containsKey(drawableId)) {
                this.mUpdatedDrawablesForSource.put(drawableId, new HashMap());
            }
            Map<String, Map<String, ParcelableResource>> drawablesForId = this.mUpdatedDrawablesForSource.get(drawableId);
            if (!drawablesForId.containsKey(drawableSource)) {
                this.mUpdatedDrawablesForSource.get(drawableId).put(drawableSource, new HashMap());
            }
            ParcelableResource current = drawablesForId.get(drawableSource).get(drawableStyle);
            if (updatableResource.equals(current)) {
                return false;
            }
            drawablesForId.get(drawableSource).put(drawableStyle, updatableResource);
            return true;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean removeDrawables(List<String> drawableIds) {
        synchronized (this.mLock) {
            boolean removed = false;
            int i = 0;
            while (true) {
                boolean z = false;
                if (i >= drawableIds.size()) {
                    break;
                }
                String drawableId = drawableIds.get(i);
                if (this.mUpdatedDrawablesForStyle.remove(drawableId) != null || this.mUpdatedDrawablesForSource.remove(drawableId) != null) {
                    z = true;
                }
                removed |= z;
                i++;
            }
            if (removed) {
                write();
                return true;
            }
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ParcelableResource getDrawable(String drawableId, String drawableStyle, String drawableSource) {
        synchronized (this.mLock) {
            ParcelableResource resource = getDrawableForSourceLocked(drawableId, drawableStyle, drawableSource);
            if (resource != null) {
                return resource;
            }
            if (!this.mUpdatedDrawablesForStyle.containsKey(drawableId)) {
                return null;
            }
            return this.mUpdatedDrawablesForStyle.get(drawableId).get(drawableStyle);
        }
    }

    ParcelableResource getDrawableForSourceLocked(String drawableId, String drawableStyle, String drawableSource) {
        if (this.mUpdatedDrawablesForSource.containsKey(drawableId) && this.mUpdatedDrawablesForSource.get(drawableId).containsKey(drawableSource)) {
            return this.mUpdatedDrawablesForSource.get(drawableId).get(drawableSource).get(drawableStyle);
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean updateStrings(List<DevicePolicyStringResource> strings) {
        boolean updated = false;
        for (int i = 0; i < strings.size(); i++) {
            String stringId = strings.get(i).getStringId();
            ParcelableResource resource = strings.get(i).getResource();
            Objects.requireNonNull(stringId, "stringId must be provided.");
            Objects.requireNonNull(resource, "ParcelableResource must be provided.");
            updated |= updateString(stringId, resource);
        }
        if (!updated) {
            return false;
        }
        synchronized (this.mLock) {
            write();
        }
        return true;
    }

    private boolean updateString(String stringId, ParcelableResource updatableResource) {
        synchronized (this.mLock) {
            ParcelableResource current = this.mUpdatedStrings.get(stringId);
            if (updatableResource.equals(current)) {
                return false;
            }
            this.mUpdatedStrings.put(stringId, updatableResource);
            return true;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean removeStrings(List<String> stringIds) {
        synchronized (this.mLock) {
            boolean removed = false;
            int i = 0;
            while (true) {
                boolean z = false;
                if (i >= stringIds.size()) {
                    break;
                }
                String stringId = stringIds.get(i);
                if (this.mUpdatedStrings.remove(stringId) != null) {
                    z = true;
                }
                removed |= z;
                i++;
            }
            if (removed) {
                write();
                return true;
            }
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ParcelableResource getString(String stringId) {
        ParcelableResource parcelableResource;
        synchronized (this.mLock) {
            parcelableResource = this.mUpdatedStrings.get(stringId);
        }
        return parcelableResource;
    }

    private void write() {
        Log.d(TAG, "Writing updated resources to file.");
        new ResourcesReaderWriter().writeToFileLocked();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void load() {
        synchronized (this.mLock) {
            new ResourcesReaderWriter().readFromFileLocked();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public File getResourcesFile() {
        return new File(this.mInjector.environmentGetDataSystemDirectory(), UPDATED_RESOURCES_XML);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class ResourcesReaderWriter {
        private final File mFile;

        private ResourcesReaderWriter() {
            this.mFile = DeviceManagementResourcesProvider.this.getResourcesFile();
        }

        void writeToFileLocked() {
            Log.d(DeviceManagementResourcesProvider.TAG, "Writing to " + this.mFile);
            AtomicFile f = new AtomicFile(this.mFile);
            FileOutputStream outputStream = null;
            try {
                outputStream = f.startWrite();
                TypedXmlSerializer out = Xml.resolveSerializer(outputStream);
                out.startDocument((String) null, true);
                out.startTag((String) null, DeviceManagementResourcesProvider.TAG_ROOT);
                writeInner(out);
                out.endTag((String) null, DeviceManagementResourcesProvider.TAG_ROOT);
                out.endDocument();
                out.flush();
                f.finishWrite(outputStream);
            } catch (IOException e) {
                Log.e(DeviceManagementResourcesProvider.TAG, "Exception when writing", e);
                if (outputStream != null) {
                    f.failWrite(outputStream);
                }
            }
        }

        /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [363=4] */
        void readFromFileLocked() {
            if (!this.mFile.exists()) {
                Log.d(DeviceManagementResourcesProvider.TAG, "" + this.mFile + " doesn't exist");
                return;
            }
            Log.d(DeviceManagementResourcesProvider.TAG, "Reading from " + this.mFile);
            AtomicFile f = new AtomicFile(this.mFile);
            InputStream input = null;
            try {
                try {
                    input = f.openRead();
                    TypedXmlPullParser parser = Xml.resolvePullParser(input);
                    int depth = 0;
                    while (true) {
                        int type = parser.next();
                        if (type != 1) {
                            switch (type) {
                                case 2:
                                    depth++;
                                    String tag = parser.getName();
                                    if (depth != 1) {
                                        if (readInner(parser, depth, tag)) {
                                            break;
                                        } else {
                                            return;
                                        }
                                    } else if (!DeviceManagementResourcesProvider.TAG_ROOT.equals(tag)) {
                                        Log.e(DeviceManagementResourcesProvider.TAG, "Invalid root tag: " + tag);
                                        return;
                                    } else {
                                        break;
                                    }
                                case 3:
                                    depth--;
                                    break;
                            }
                        }
                    }
                } catch (IOException | XmlPullParserException e) {
                    Log.e(DeviceManagementResourcesProvider.TAG, "Error parsing resources file", e);
                }
            } finally {
                IoUtils.closeQuietly(input);
            }
        }

        void writeInner(TypedXmlSerializer out) throws IOException {
            writeDrawablesForStylesInner(out);
            writeDrawablesForSourcesInner(out);
            writeStringsInner(out);
        }

        private void writeDrawablesForStylesInner(TypedXmlSerializer out) throws IOException {
            if (DeviceManagementResourcesProvider.this.mUpdatedDrawablesForStyle != null && !DeviceManagementResourcesProvider.this.mUpdatedDrawablesForStyle.isEmpty()) {
                for (Map.Entry<String, Map<String, ParcelableResource>> drawableEntry : DeviceManagementResourcesProvider.this.mUpdatedDrawablesForStyle.entrySet()) {
                    for (Map.Entry<String, ParcelableResource> styleEntry : drawableEntry.getValue().entrySet()) {
                        out.startTag((String) null, DeviceManagementResourcesProvider.TAG_DRAWABLE_STYLE_ENTRY);
                        out.attribute((String) null, DeviceManagementResourcesProvider.ATTR_DRAWABLE_ID, drawableEntry.getKey());
                        out.attribute((String) null, DeviceManagementResourcesProvider.ATTR_DRAWABLE_STYLE, styleEntry.getKey());
                        styleEntry.getValue().writeToXmlFile(out);
                        out.endTag((String) null, DeviceManagementResourcesProvider.TAG_DRAWABLE_STYLE_ENTRY);
                    }
                }
            }
        }

        private void writeDrawablesForSourcesInner(TypedXmlSerializer out) throws IOException {
            if (DeviceManagementResourcesProvider.this.mUpdatedDrawablesForSource != null && !DeviceManagementResourcesProvider.this.mUpdatedDrawablesForSource.isEmpty()) {
                for (Map.Entry<String, Map<String, Map<String, ParcelableResource>>> drawableEntry : DeviceManagementResourcesProvider.this.mUpdatedDrawablesForSource.entrySet()) {
                    for (Map.Entry<String, Map<String, ParcelableResource>> sourceEntry : drawableEntry.getValue().entrySet()) {
                        for (Map.Entry<String, ParcelableResource> styleEntry : sourceEntry.getValue().entrySet()) {
                            out.startTag((String) null, DeviceManagementResourcesProvider.TAG_DRAWABLE_SOURCE_ENTRY);
                            out.attribute((String) null, DeviceManagementResourcesProvider.ATTR_DRAWABLE_ID, drawableEntry.getKey());
                            out.attribute((String) null, DeviceManagementResourcesProvider.ATTR_DRAWABLE_SOURCE, sourceEntry.getKey());
                            out.attribute((String) null, DeviceManagementResourcesProvider.ATTR_DRAWABLE_STYLE, styleEntry.getKey());
                            styleEntry.getValue().writeToXmlFile(out);
                            out.endTag((String) null, DeviceManagementResourcesProvider.TAG_DRAWABLE_SOURCE_ENTRY);
                        }
                    }
                }
            }
        }

        private void writeStringsInner(TypedXmlSerializer out) throws IOException {
            if (DeviceManagementResourcesProvider.this.mUpdatedStrings != null && !DeviceManagementResourcesProvider.this.mUpdatedStrings.isEmpty()) {
                for (Map.Entry<String, ParcelableResource> entry : DeviceManagementResourcesProvider.this.mUpdatedStrings.entrySet()) {
                    out.startTag((String) null, DeviceManagementResourcesProvider.TAG_STRING_ENTRY);
                    out.attribute((String) null, DeviceManagementResourcesProvider.ATTR_SOURCE_ID, entry.getKey());
                    entry.getValue().writeToXmlFile(out);
                    out.endTag((String) null, DeviceManagementResourcesProvider.TAG_STRING_ENTRY);
                }
            }
        }

        /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
        /* JADX WARN: Code restructure failed: missing block: B:15:0x002a, code lost:
            if (r10.equals(com.android.server.devicepolicy.DeviceManagementResourcesProvider.TAG_STRING_ENTRY) != false) goto L10;
         */
        /*
            Code decompiled incorrectly, please refer to instructions dump.
        */
        private boolean readInner(TypedXmlPullParser parser, int depth, String tag) throws XmlPullParserException, IOException {
            char c = 2;
            if (depth > 2) {
                return true;
            }
            switch (tag.hashCode()) {
                case -1021023306:
                    break;
                case 1224071439:
                    if (tag.equals(DeviceManagementResourcesProvider.TAG_DRAWABLE_SOURCE_ENTRY)) {
                        c = 1;
                        break;
                    }
                    c = 65535;
                    break;
                case 1406273191:
                    if (tag.equals(DeviceManagementResourcesProvider.TAG_DRAWABLE_STYLE_ENTRY)) {
                        c = 0;
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
                    String id = parser.getAttributeValue((String) null, DeviceManagementResourcesProvider.ATTR_DRAWABLE_ID);
                    String style = parser.getAttributeValue((String) null, DeviceManagementResourcesProvider.ATTR_DRAWABLE_STYLE);
                    ParcelableResource resource = ParcelableResource.createFromXml(parser);
                    if (!DeviceManagementResourcesProvider.this.mUpdatedDrawablesForStyle.containsKey(id)) {
                        DeviceManagementResourcesProvider.this.mUpdatedDrawablesForStyle.put(id, new HashMap());
                    }
                    ((Map) DeviceManagementResourcesProvider.this.mUpdatedDrawablesForStyle.get(id)).put(style, resource);
                    break;
                case 1:
                    String id2 = parser.getAttributeValue((String) null, DeviceManagementResourcesProvider.ATTR_DRAWABLE_ID);
                    String source = parser.getAttributeValue((String) null, DeviceManagementResourcesProvider.ATTR_DRAWABLE_SOURCE);
                    String style2 = parser.getAttributeValue((String) null, DeviceManagementResourcesProvider.ATTR_DRAWABLE_STYLE);
                    ParcelableResource resource2 = ParcelableResource.createFromXml(parser);
                    if (!DeviceManagementResourcesProvider.this.mUpdatedDrawablesForSource.containsKey(id2)) {
                        DeviceManagementResourcesProvider.this.mUpdatedDrawablesForSource.put(id2, new HashMap());
                    }
                    if (!((Map) DeviceManagementResourcesProvider.this.mUpdatedDrawablesForSource.get(id2)).containsKey(source)) {
                        ((Map) DeviceManagementResourcesProvider.this.mUpdatedDrawablesForSource.get(id2)).put(source, new HashMap());
                    }
                    ((Map) ((Map) DeviceManagementResourcesProvider.this.mUpdatedDrawablesForSource.get(id2)).get(source)).put(style2, resource2);
                    break;
                case 2:
                    DeviceManagementResourcesProvider.this.mUpdatedStrings.put(parser.getAttributeValue((String) null, DeviceManagementResourcesProvider.ATTR_SOURCE_ID), ParcelableResource.createFromXml(parser));
                    break;
                default:
                    Log.e(DeviceManagementResourcesProvider.TAG, "Unexpected tag: " + tag);
                    return false;
            }
            return true;
        }
    }

    /* loaded from: classes.dex */
    public static class Injector {
        File environmentGetDataSystemDirectory() {
            return Environment.getDataSystemDirectory();
        }
    }
}
