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

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public class StringUtil {
    public static boolean isEmpty(String str) {
        return str == null || str.length() == 0;
    }

    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }

    public static Map<String, String> splitKv(String content, char kvDelimiter, char listDelimiter) {
        return splitKkv(content, null, kvDelimiter, listDelimiter);
    }

    private static String[] splitWorker(String str, char separatorChar, boolean preserveAllTokens) {
        // Performance tuned for 2.0 (JDK1.4)
        if (str == null) {
            return null;
        }
        int len = str.length();
        if (len == 0) {
            return new String[0];
        }

        if (!preserveAllTokens) {
            char[] filter = new char[len];
            int j = 0;
            for (int i = 0; i < len; i++) {
                char c = str.charAt(i);
                if (c != separatorChar) {
                    filter[j++] = c;
                } else {
                    if (i < 1 || str.charAt(i - 1) != separatorChar) {
                        filter[j++] = c;
                    }
                }
            }
            str = new String(filter, 0, j);
        }

        List<Character> specialChar = Arrays.asList('$', '(', ')', '*', '.', '+', '[', ']', '?', '\\', '^', '{', '}', '|');
        StringBuilder regx = new StringBuilder(2);
        if (specialChar.contains(separatorChar)) {
            regx.append('\\').append(separatorChar);
        } else {
            regx.append(separatorChar);
        }
        return str.split(regx.toString());
    }

    private static Map<String, String> splitKkv(String content, Character keyDelimiter, char kvDelimiter,
                                               char listDelimiter) {
        if (isEmpty(content)) {
            return null;
        }

        Map<String, String> result = new LinkedHashMap<>();
        String[] kkvList = splitWorker(content, listDelimiter, false);
        for (String kkvStr : kkvList) {
            String[] kkv = splitWorker(kkvStr, kvDelimiter, false);

            String kkStr = kkv[0];
            String value;
            if (kkv.length == 1) {
                value = null;
            } else {
                if (isEmpty(kkv[1])) {
                    value = null;
                } else {
                    value = kkv[1];
                }
            }

            if (keyDelimiter != null) {
                String[] kk = splitWorker(kkStr, keyDelimiter.charValue(), false);
                for (String k : kk) {
                    if (isEmpty(k)) {
                        continue;
                    }
                    result.put(k, value);
                }
            } else {
                if (isNotEmpty(kkStr)) {
                    result.put(kkStr, value);
                }
            }
        }
        return result;
    }
}
