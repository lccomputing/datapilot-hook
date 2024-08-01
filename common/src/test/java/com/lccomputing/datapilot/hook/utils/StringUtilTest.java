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
