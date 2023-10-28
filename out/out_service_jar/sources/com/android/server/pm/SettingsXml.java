package com.android.server.pm;

import android.util.Slog;
import android.util.TypedXmlPullParser;
import android.util.TypedXmlSerializer;
import android.util.Xml;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Stack;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes2.dex */
public class SettingsXml {
    private static final boolean DEBUG_THROW_EXCEPTIONS = false;
    private static final int DEFAULT_NUMBER = -1;
    private static final String FEATURE_INDENT = "http://xmlpull.org/v1/doc/features.html#indent-output";
    private static final String TAG = "SettingsXml";

    /* loaded from: classes2.dex */
    public interface ChildSection extends ReadSection {
        boolean moveToNext();

        boolean moveToNext(String str);
    }

    /* loaded from: classes2.dex */
    public interface ReadSection extends AutoCloseable {
        ChildSection children();

        boolean getBoolean(String str);

        boolean getBoolean(String str, boolean z);

        String getDescription();

        int getInt(String str);

        int getInt(String str, int i);

        long getLong(String str);

        long getLong(String str, int i);

        String getName();

        String getString(String str);

        String getString(String str, String str2);

        boolean has(String str);
    }

    /* loaded from: classes2.dex */
    public interface WriteSection extends AutoCloseable {
        WriteSection attribute(String str, int i) throws IOException;

        WriteSection attribute(String str, long j) throws IOException;

        WriteSection attribute(String str, String str2) throws IOException;

        WriteSection attribute(String str, boolean z) throws IOException;

        @Override // java.lang.AutoCloseable
        void close() throws IOException;

        void finish() throws IOException;

        WriteSection startSection(String str) throws IOException;
    }

    public static Serializer serializer(TypedXmlSerializer serializer) {
        return new Serializer(serializer);
    }

    public static ReadSection parser(TypedXmlPullParser parser) throws IOException, XmlPullParserException {
        return new ReadSectionImpl(parser);
    }

    /* loaded from: classes2.dex */
    public static class Serializer implements AutoCloseable {
        private final WriteSectionImpl mWriteSection;
        private final TypedXmlSerializer mXmlSerializer;

        private Serializer(TypedXmlSerializer serializer) {
            this.mXmlSerializer = serializer;
            this.mWriteSection = new WriteSectionImpl(serializer);
        }

        public WriteSection startSection(String sectionName) throws IOException {
            return this.mWriteSection.startSection(sectionName);
        }

        @Override // java.lang.AutoCloseable
        public void close() throws IOException {
            this.mWriteSection.closeCompletely();
            this.mXmlSerializer.flush();
        }
    }

    /* loaded from: classes2.dex */
    public static class ReadSectionImpl implements ChildSection {
        private final Stack<Integer> mDepthStack;
        private final InputStream mInput;
        private final TypedXmlPullParser mParser;

        public ReadSectionImpl(InputStream input) throws IOException, XmlPullParserException {
            this.mDepthStack = new Stack<>();
            this.mInput = input;
            TypedXmlPullParser newFastPullParser = Xml.newFastPullParser();
            this.mParser = newFastPullParser;
            newFastPullParser.setInput(input, StandardCharsets.UTF_8.name());
            moveToFirstTag();
        }

        public ReadSectionImpl(TypedXmlPullParser parser) throws IOException, XmlPullParserException {
            this.mDepthStack = new Stack<>();
            this.mInput = null;
            this.mParser = parser;
            moveToFirstTag();
        }

        private void moveToFirstTag() throws IOException, XmlPullParserException {
            int type;
            if (this.mParser.getEventType() == 2) {
                return;
            }
            do {
                type = this.mParser.next();
                if (type == 2) {
                    return;
                }
            } while (type != 1);
        }

        @Override // com.android.server.pm.SettingsXml.ReadSection
        public String getName() {
            return this.mParser.getName();
        }

        @Override // com.android.server.pm.SettingsXml.ReadSection
        public String getDescription() {
            return this.mParser.getPositionDescription();
        }

        @Override // com.android.server.pm.SettingsXml.ReadSection
        public boolean has(String attrName) {
            return this.mParser.getAttributeValue((String) null, attrName) != null;
        }

        @Override // com.android.server.pm.SettingsXml.ReadSection
        public String getString(String attrName) {
            return this.mParser.getAttributeValue((String) null, attrName);
        }

        @Override // com.android.server.pm.SettingsXml.ReadSection
        public String getString(String attrName, String defaultValue) {
            String value = this.mParser.getAttributeValue((String) null, attrName);
            if (value == null) {
                return defaultValue;
            }
            return value;
        }

        @Override // com.android.server.pm.SettingsXml.ReadSection
        public boolean getBoolean(String attrName) {
            return getBoolean(attrName, false);
        }

        @Override // com.android.server.pm.SettingsXml.ReadSection
        public boolean getBoolean(String attrName, boolean defaultValue) {
            return this.mParser.getAttributeBoolean((String) null, attrName, defaultValue);
        }

        @Override // com.android.server.pm.SettingsXml.ReadSection
        public int getInt(String attrName) {
            return getInt(attrName, -1);
        }

        @Override // com.android.server.pm.SettingsXml.ReadSection
        public int getInt(String attrName, int defaultValue) {
            return this.mParser.getAttributeInt((String) null, attrName, defaultValue);
        }

        @Override // com.android.server.pm.SettingsXml.ReadSection
        public long getLong(String attrName) {
            return getLong(attrName, -1);
        }

        @Override // com.android.server.pm.SettingsXml.ReadSection
        public long getLong(String attrName, int defaultValue) {
            return this.mParser.getAttributeLong((String) null, attrName, defaultValue);
        }

        @Override // com.android.server.pm.SettingsXml.ReadSection
        public ChildSection children() {
            this.mDepthStack.push(Integer.valueOf(this.mParser.getDepth()));
            return this;
        }

        @Override // com.android.server.pm.SettingsXml.ChildSection
        public boolean moveToNext() {
            return moveToNextInternal(null);
        }

        @Override // com.android.server.pm.SettingsXml.ChildSection
        public boolean moveToNext(String expectedChildTagName) {
            return moveToNextInternal(expectedChildTagName);
        }

        private boolean moveToNextInternal(String expectedChildTagName) {
            try {
                int depth = this.mDepthStack.peek().intValue();
                boolean hasTag = false;
                while (!hasTag) {
                    int type = this.mParser.next();
                    if (type == 1 || (type == 3 && this.mParser.getDepth() <= depth)) {
                        break;
                    } else if (type == 2 && (expectedChildTagName == null || expectedChildTagName.equals(this.mParser.getName()))) {
                        hasTag = true;
                    }
                }
                if (!hasTag) {
                    this.mDepthStack.pop();
                }
                return hasTag;
            } catch (Exception e) {
                return false;
            }
        }

        @Override // java.lang.AutoCloseable
        public void close() throws Exception {
            if (this.mDepthStack.isEmpty()) {
                Slog.wtf(SettingsXml.TAG, "Children depth stack was not empty, data may have been lost", new Exception());
            }
            InputStream inputStream = this.mInput;
            if (inputStream != null) {
                inputStream.close();
            }
        }
    }

    /* loaded from: classes2.dex */
    private static class WriteSectionImpl implements WriteSection {
        private final Stack<String> mTagStack;
        private final TypedXmlSerializer mXmlSerializer;

        private WriteSectionImpl(TypedXmlSerializer xmlSerializer) {
            this.mTagStack = new Stack<>();
            this.mXmlSerializer = xmlSerializer;
        }

        @Override // com.android.server.pm.SettingsXml.WriteSection
        public WriteSection startSection(String sectionName) throws IOException {
            this.mXmlSerializer.startTag((String) null, sectionName);
            this.mTagStack.push(sectionName);
            return this;
        }

        @Override // com.android.server.pm.SettingsXml.WriteSection
        public WriteSection attribute(String attrName, String value) throws IOException {
            if (value != null) {
                this.mXmlSerializer.attribute((String) null, attrName, value);
            }
            return this;
        }

        @Override // com.android.server.pm.SettingsXml.WriteSection
        public WriteSection attribute(String attrName, int value) throws IOException {
            if (value != -1) {
                this.mXmlSerializer.attributeInt((String) null, attrName, value);
            }
            return this;
        }

        @Override // com.android.server.pm.SettingsXml.WriteSection
        public WriteSection attribute(String attrName, long value) throws IOException {
            if (value != -1) {
                this.mXmlSerializer.attributeLong((String) null, attrName, value);
            }
            return this;
        }

        @Override // com.android.server.pm.SettingsXml.WriteSection
        public WriteSection attribute(String attrName, boolean value) throws IOException {
            if (value) {
                this.mXmlSerializer.attributeBoolean((String) null, attrName, value);
            }
            return this;
        }

        @Override // com.android.server.pm.SettingsXml.WriteSection
        public void finish() throws IOException {
            close();
        }

        @Override // com.android.server.pm.SettingsXml.WriteSection, java.lang.AutoCloseable
        public void close() throws IOException {
            this.mXmlSerializer.endTag((String) null, this.mTagStack.pop());
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void closeCompletely() throws IOException {
            if (this.mTagStack != null) {
                while (!this.mTagStack.isEmpty()) {
                    close();
                }
            }
        }
    }
}
