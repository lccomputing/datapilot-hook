/*
 * Lccomputing Sky DataPilot Hook
 * Copyright 2021 Lccomputing
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.lccomputing.datapilot.hook.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.lccomputing.datapilot.hook.common.RollingString;
import org.testng.annotations.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Arrays;

import static org.testng.Assert.*;

@Test
public class HttpUtilsTest {

    public void testPost() throws IOException {
        final JsonObject data = new JsonObject();
        data.addProperty("k1", "v1");
        data.addProperty("k2", "v2");
        data.addProperty("&k3", " =好");

        final RollingString hosts = new RollingString(Arrays.asList("http://httpbin.org"), 0);

        String resp = HttpUtils.post(hosts, "/post", data.toString(), null, "application/json", 3000, 30000, 1, 500);

        JsonObject respObj = new JsonParser().parse(resp).getAsJsonObject();
        assertEquals(respObj.get("url").getAsString(), "http://httpbin.org/post");
        assertEquals(respObj.get("json").getAsJsonObject().get("k1").getAsString(), "v1");
        assertEquals(respObj.get("json").getAsJsonObject().get("k2").getAsString(), "v2");
        assertEquals(respObj.get("json").getAsJsonObject().get("&k3").getAsString(), " =好");

        assertEquals(respObj.get("data").getAsString(), data.toString());

        assertThrows(FileNotFoundException.class, new ThrowingRunnable() {
            @Override
            public void run() throws Throwable {
                HttpUtils.post(hosts, "/post2", data.toString(), null, "application/json", 3000, 30000, 1, 500);
            }
        });

        assertThrows(SocketTimeoutException.class, new ThrowingRunnable() {
            @Override
            public void run() throws Throwable {
                HttpUtils.post(hosts, "/post2", data.toString(), null, "application/json", 3000, 100, 1, 500);
            }
        });
    }
}
