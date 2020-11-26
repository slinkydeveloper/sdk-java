package io.cloudevents.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.cloudevents.CloudEvent;
import io.cloudevents.CloudEventData;
import io.cloudevents.SpecVersion;
import io.cloudevents.core.data.BytesCloudEventData;
import io.cloudevents.rw.*;

import java.io.IOException;

class JsonReader implements CloudEventReader {

    private final JsonParser p;
    private final ObjectNode node;

    public JsonReader(JsonParser p, ObjectNode node) {
        this.p = p;
        this.node = node;
    }

    @Override
    public <T extends CloudEventWriter<V>, V> V read(CloudEventWriterFactory<T, V> writerFactory, CloudEventDataMapper<? extends CloudEventData> mapper) throws CloudEventRWException, IllegalStateException {
        try {
            SpecVersion specVersion = SpecVersion.parse(getStringNode(this.node, this.p, "specversion"));
            CloudEventWriter<V> writer = writerFactory.create(specVersion);

            // TODO remove all the unnecessary code specversion aware

            // Read mandatory attributes
            for (String attr : specVersion.getMandatoryAttributes()) {
                if (!"specversion".equals(attr)) {
                    writer.withContextAttribute(attr, getStringNode(this.node, this.p, attr));
                }
            }

            // Parse datacontenttype if any
            String contentType = getOptionalStringNode(this.node, this.p, "datacontenttype");
            if (contentType != null) {
                writer.withContextAttribute("datacontenttype", contentType);
            }

            // Read optional attributes
            for (String attr : specVersion.getOptionalAttributes()) {
                if (!"datacontentencoding".equals(attr)) { // Skip datacontentencoding, we need it later
                    String val = getOptionalStringNode(this.node, this.p, attr);
                    if (val != null) {
                        writer.withContextAttribute(attr, val);
                    }
                }
            }

            CloudEventData data = null;

            // Now let's handle the data
            switch (specVersion) {
                case V03:
                    boolean isBase64 = "base64".equals(getOptionalStringNode(this.node, this.p, "datacontentencoding"));
                    if (node.has("data")) {
                        if (isBase64) {
                            data = BytesCloudEventData.wrap(node.remove("data").binaryValue());
                        } else {
                            if (JsonFormat.dataIsJsonContentType(contentType)) {
                                // This solution is quite bad, but i see no alternatives now.
                                // Hopefully in future we can improve it
                                data = new JsonCloudEventData(node.remove("data"));
                            } else {
                                JsonNode dataNode = node.remove("data");
                                assertNodeType(dataNode, JsonNodeType.STRING, "data", "Because content type is not a json, only a string is accepted as data");
                                data = BytesCloudEventData.wrap(dataNode.asText().getBytes());
                            }
                        }
                    }
                case V1:
                    if (node.has("data_base64") && node.has("data")) {
                        throw MismatchedInputException.from(p, CloudEvent.class, "CloudEvent cannot have both 'data' and 'data_base64' fields");
                    }
                    if (node.has("data_base64")) {
                        data = BytesCloudEventData.wrap(node.remove("data_base64").binaryValue());
                    } else if (node.has("data")) {
                        if (JsonFormat.dataIsJsonContentType(contentType)) {
                            // This solution is quite bad, but i see no alternatives now.
                            // Hopefully in future we can improve it
                            data = new JsonCloudEventData(node.remove("data"));
                        } else {
                            JsonNode dataNode = node.remove("data");
                            assertNodeType(dataNode, JsonNodeType.STRING, "data", "Because content type is not a json, only a string is accepted as data");
                            data = BytesCloudEventData.wrap(dataNode.asText().getBytes());
                        }
                    }
            }

            // Now let's process the extensions
            node.fields().forEachRemaining(entry -> {
                String extensionName = entry.getKey();
                JsonNode extensionValue = entry.getValue();

                switch (extensionValue.getNodeType()) {
                    case BOOLEAN:
                        writer.withContextAttribute(extensionName, extensionValue.booleanValue());
                        break;
                    case NUMBER:
                        writer.withContextAttribute(extensionName, extensionValue.numberValue());
                        break;
                    case STRING:
                        writer.withContextAttribute(extensionName, extensionValue.textValue());
                        break;
                    default:
                        writer.withContextAttribute(extensionName, extensionValue.toString());
                }

            });

            if (data != null) {
                return writer.end(mapper.map(data));
            }
            return writer.end();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(MismatchedInputException.from(this.p, CloudEvent.class, e.getMessage()));
        }
    }

    private String getStringNode(ObjectNode objNode, JsonParser p, String attributeName) throws JsonProcessingException {
        String val = getOptionalStringNode(objNode, p, attributeName);
        if (val == null) {
            throw MismatchedInputException.from(p, CloudEvent.class, "Missing mandatory " + attributeName + " attribute");
        }
        return val;
    }

    private String getOptionalStringNode(ObjectNode objNode, JsonParser p, String attributeName) throws JsonProcessingException {
        JsonNode unparsedSpecVersion = objNode.remove(attributeName);
        if (unparsedSpecVersion == null) {
            return null;
        }
        assertNodeType(unparsedSpecVersion, JsonNodeType.STRING, attributeName, null);
        return unparsedSpecVersion.asText();
    }

    private void assertNodeType(JsonNode node, JsonNodeType type, String attributeName, String desc) throws JsonProcessingException {
        if (node.getNodeType() != type) {
            throw MismatchedInputException.from(
                p,
                CloudEvent.class,
                "Wrong type " + node.getNodeType() + " for attribute " + attributeName + ", expecting " + type + (desc != null ? ". " + desc : "")
            );
        }
    }
}
