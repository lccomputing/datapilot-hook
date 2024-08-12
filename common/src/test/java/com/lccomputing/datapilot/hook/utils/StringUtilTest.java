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

import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.*;

@Test
public class StringUtilTest {

    public void testIsEmpty() {
        assertTrue(StringUtil.isEmpty(""));
        assertTrue(StringUtil.isEmpty(null));
        assertFalse(StringUtil.isEmpty("  "));
        assertFalse(StringUtil.isEmpty(" \t "));
        assertFalse(StringUtil.isEmpty(" \n "));
        assertFalse(StringUtil.isEmpty(" \r "));
    }

    public void testSplitKv() {
        String content = "k1,k2,k3:v1;k4,k5:v2;:v0;k6:v3;k7;k8::;,k9,,:v4,v5;k10";

        Map<String, String> expect = new HashMap<>();
        expect.put("k1,k2,k3", "v1");
        expect.put("k4,k5", "v2");
        expect.put("k6", "v3");
        expect.put("k7", null);
        expect.put("k8", null);
        expect.put(",k9,,", "v4,v5");
        expect.put("k10", null);
        assertEquals(StringUtil.splitKv(content, ':', ';'), expect);
    }

}
