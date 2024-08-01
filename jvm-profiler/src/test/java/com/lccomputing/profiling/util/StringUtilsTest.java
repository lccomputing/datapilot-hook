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
