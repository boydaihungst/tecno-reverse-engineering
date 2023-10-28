package com.android.server.integrity.serializer;

import android.util.TypedXmlSerializer;
import android.util.Xml;
import com.android.server.integrity.model.RuleMetadata;
import com.android.server.integrity.parser.RuleMetadataParser;
import java.io.IOException;
import java.io.OutputStream;
/* loaded from: classes.dex */
public class RuleMetadataSerializer {
    public static void serialize(RuleMetadata ruleMetadata, OutputStream outputStream) throws IOException {
        TypedXmlSerializer xmlSerializer = Xml.resolveSerializer(outputStream);
        serializeTaggedValue(xmlSerializer, RuleMetadataParser.RULE_PROVIDER_TAG, ruleMetadata.getRuleProvider());
        serializeTaggedValue(xmlSerializer, RuleMetadataParser.VERSION_TAG, ruleMetadata.getVersion());
        xmlSerializer.endDocument();
    }

    private static void serializeTaggedValue(TypedXmlSerializer xmlSerializer, String tag, String value) throws IOException {
        xmlSerializer.startTag((String) null, tag);
        xmlSerializer.text(value);
        xmlSerializer.endTag((String) null, tag);
    }
}
