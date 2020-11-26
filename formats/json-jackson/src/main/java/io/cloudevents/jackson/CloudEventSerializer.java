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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.CloudEventUtils;
import io.cloudevents.rw.CloudEventRWException;
import io.cloudevents.rw.CloudEventReader;

import java.io.IOException;

/**
 * Jackson {@link com.fasterxml.jackson.databind.JsonSerializer} for {@link CloudEvent}
 */
public class CloudEventSerializer extends StdSerializer<CloudEvent> {

    private final boolean forceDataBase64Serialization;
    private final boolean forceStringSerialization;

    protected CloudEventSerializer(boolean forceDataBase64Serialization, boolean forceStringSerialization) {
        super(CloudEvent.class);
        this.forceDataBase64Serialization = forceDataBase64Serialization;
        this.forceStringSerialization = forceStringSerialization;
    }

    @Override
    public void serialize(CloudEvent value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        CloudEventReader reader = CloudEventUtils.toReader(value);
        try {
            reader.read(new JsonWriter(gen, this.forceDataBase64Serialization, this.forceStringSerialization));
        } catch (CloudEventRWException e) {
            // We need to propagate back io exception
            if (e.getKind() == CloudEventRWException.CloudEventRWExceptionKind.OTHER && e.getCause() instanceof IOException) {
                throw (IOException) e.getCause();
            }
        }
    }

}
