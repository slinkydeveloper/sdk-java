/*
 * Copyright 2018-Present The CloudEvents Authors
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.cloudevents.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;

import java.io.IOException;

/**
 * Jackson {@link com.fasterxml.jackson.databind.JsonDeserializer} for {@link CloudEvent}
 */
public class CloudEventDeserializer extends StdDeserializer<CloudEvent> {

    protected CloudEventDeserializer() {
        super(CloudEvent.class);
    }

    @Override
    public CloudEvent deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        // In future we could eventually find a better solution avoiding this buffering step, but now this is the best option
        // Other sdk does the same in order to support all versions
        ObjectNode node = ctxt.readValue(p, ObjectNode.class);

        try {
            return new JsonReader(p, node).read(CloudEventBuilder::fromSpecVersion);
        } catch (RuntimeException e) {
            // Yeah this is bad but it's needed to support checked exceptions...
            if (e.getCause() instanceof IOException) {
                throw (IOException) e.getCause();
            }
            throw MismatchedInputException.wrapWithPath(e, null);
        }
    }
}
