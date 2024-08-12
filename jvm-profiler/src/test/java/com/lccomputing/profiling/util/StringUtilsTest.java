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
package com.lccomputing.profiling.util;

import com.lccomputing.datapilot.hook.profiling.util.StringUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

public class StringUtilsTest {

    @Test
    public void getValueAsBytes() {
        Assert.assertEquals(null, StringUtils.getBytesValueOrNull(null));
        Assert.assertEquals(null, StringUtils.getBytesValueOrNull(""));
        Assert.assertEquals(null, StringUtils.getBytesValueOrNull("xxx"));

        Assert.assertEquals(0L, StringUtils.getBytesValueOrNull("0").longValue());
        Assert.assertEquals(123L, StringUtils.getBytesValueOrNull("123").longValue());
        Assert.assertNull(StringUtils.getBytesValueOrNull("123."));
        Assert.assertNull(StringUtils.getBytesValueOrNull("123.123"));

        Assert.assertEquals(123 * 1024L, StringUtils.getBytesValueOrNull("123k").longValue());
        Assert.assertEquals(123 * 1024L, StringUtils.getBytesValueOrNull("123kb").longValue());
        Assert.assertEquals(123 * 1024L, StringUtils.getBytesValueOrNull("123 kb").longValue());
        Assert.assertEquals(123 * 1024L, StringUtils.getBytesValueOrNull("123K").longValue());
        Assert.assertEquals(123 * 1024L, StringUtils.getBytesValueOrNull("123KB").longValue());
        Assert.assertEquals(123 * 1024L, StringUtils.getBytesValueOrNull("123 KB").longValue());

        Assert.assertEquals(123 * 1024L * 1024L, StringUtils.getBytesValueOrNull("123m").longValue());
        Assert.assertEquals(123 * 1024L * 1024L, StringUtils.getBytesValueOrNull("123mb").longValue());
        Assert.assertEquals(123 * 1024L * 1024L, StringUtils.getBytesValueOrNull("123 mb").longValue());
        Assert.assertEquals(123 * 1024L * 1024L, StringUtils.getBytesValueOrNull("123M").longValue());

        Assert.assertEquals(123 * 1024L * 1024L * 1024L, StringUtils.getBytesValueOrNull("123g").longValue());
        Assert.assertEquals(123 * 1024L * 1024L * 1024L, StringUtils.getBytesValueOrNull("123gb").longValue());
        Assert.assertEquals(123 * 1024L * 1024L * 1024L, StringUtils.getBytesValueOrNull("123 gb").longValue());
        Assert.assertEquals(123 * 1024L * 1024L * 1024L, StringUtils.getBytesValueOrNull("123G").longValue());

        Assert.assertEquals(123L, StringUtils.getBytesValueOrNull("123bytes").longValue());
        Assert.assertEquals(123L, StringUtils.getBytesValueOrNull("123 Bytes").longValue());
    }

}
