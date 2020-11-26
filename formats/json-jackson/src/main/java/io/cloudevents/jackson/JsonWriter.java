package io.cloudevents.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import io.cloudevents.CloudEventData;
import io.cloudevents.SpecVersion;
import io.cloudevents.core.v1.CloudEventV1;
import io.cloudevents.rw.CloudEventContextWriter;
import io.cloudevents.rw.CloudEventRWException;
import io.cloudevents.rw.CloudEventWriter;
import io.cloudevents.rw.CloudEventWriterFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

class JsonWriter implements CloudEventWriterFactory<JsonWriter, Void>, CloudEventWriter<Void> {

    private final JsonGenerator gen;
    private final boolean forceDataBase64Serialization;
    private final boolean forceStringSerialization;

    private SpecVersion specVersion;
    private String dataContentType;

    public JsonWriter(JsonGenerator gen, boolean forceDataBase64Serialization, boolean forceStringSerialization) {
        this.gen = gen;
        this.forceDataBase64Serialization = forceDataBase64Serialization;
        this.forceStringSerialization = forceStringSerialization;
    }

    @Override
    public JsonWriter create(SpecVersion version) throws CloudEventRWException {
        this.specVersion = version;
        try {
            gen.writeStartObject();
            gen.writeStringField(CloudEventV1.SPECVERSION, version.toString());
        } catch (IOException e) {
            throw CloudEventRWException.newOther(e);
        }
        return this;
    }

    @Override
    public CloudEventContextWriter withContextAttribute(String name, String value) throws CloudEventRWException {
        if (name.equals(CloudEventV1.DATACONTENTTYPE)) {
            this.dataContentType = value;
        }
        try {
            gen.writeStringField(name, value);
            return this;
        } catch (IOException e) {
            throw CloudEventRWException.newOther(e);
        }
    }

    @Override
    public CloudEventContextWriter withContextAttribute(String name, Number value) throws CloudEventRWException {
        try {
            gen.writeFieldName(name);
            if (value instanceof Integer) {
                gen.writeNumber((Integer) value);
            } else {
                gen.writeNumber(value.longValue());
            }
            return this;
        } catch (IOException e) {
            throw CloudEventRWException.newOther(e);
        }
    }

    @Override
    public CloudEventContextWriter withContextAttribute(String name, Boolean value) throws CloudEventRWException {
        try {
            gen.writeBooleanField(name, value);
            return this;
        } catch (IOException e) {
            throw CloudEventRWException.newOther(e);
        }
    }

    @Override
    public Void end(CloudEventData data) throws CloudEventRWException {
        try {
            if (data instanceof JsonCloudEventData) {
                gen.writeObjectField("data", ((JsonCloudEventData) data).getNode());
            } else {
                byte[] dataBytes = data.toBytes();
                if (shouldSerializeBase64(dataContentType)) {
                    switch (specVersion) {
                        case V03:
                            gen.writeStringField("datacontentencoding", "base64");
                            gen.writeFieldName("data");
                            gen.writeBinary(dataBytes);
                            break;
                        case V1:
                            gen.writeFieldName("data_base64");
                            gen.writeBinary(dataBytes);
                            break;
                    }
                } else if (JsonFormat.dataIsJsonContentType(dataContentType)) {
                    // TODO really bad b/c it allocates stuff, is there another solution out there?
                    char[] dataAsString = new String(dataBytes, StandardCharsets.UTF_8).toCharArray();
                    gen.writeFieldName("data");
                    gen.writeRawValue(dataAsString, 0, dataAsString.length);
                } else {
                    gen.writeFieldName("data");
                    gen.writeUTF8String(dataBytes, 0, dataBytes.length);
                }
            }
        } catch (IOException e) {
            throw CloudEventRWException.newOther(e);
        }
        return end();
    }

    @Override
    public Void end() throws CloudEventRWException {
        try {
            gen.writeEndObject();
        } catch (IOException e) {
            throw CloudEventRWException.newOther(e);
        }
        return null;
    }

    private boolean shouldSerializeBase64(String contentType) {
        if (JsonFormat.dataIsJsonContentType(contentType)) {
            return this.forceDataBase64Serialization;
        } else {
            return !this.forceStringSerialization;
        }
    }
}
