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

package io.cloudevents.http.restful.ws.impl;

import io.cloudevents.core.message.impl.MessageUtils;

import javax.ws.rs.core.HttpHeaders;
import java.util.Map;

class CloudEventsHeaders {

    static final String CE_PREFIX = "ce-";

    static final Map<String, String> ATTRIBUTES_TO_HEADERS = MessageUtils.generateAttributesToHeadersMapping(v -> {
        if (v.equals("datacontenttype")) {
            return HttpHeaders.CONTENT_TYPE;
        }
        return CE_PREFIX + v;
    });

    static final String SPEC_VERSION = ATTRIBUTES_TO_HEADERS.get("specversion");

}
