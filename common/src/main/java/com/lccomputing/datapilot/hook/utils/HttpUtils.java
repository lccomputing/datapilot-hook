package com.lccomputing.datapilot.hook.utils;

import com.lccomputing.datapilot.hook.common.RollingString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

public class HttpUtils {
    private static final Logger log = LoggerFactory.getLogger(HttpUtils.class);
    private static final String UTF8 = "UTF-8";

    public static String post(RollingString hosts, String url, String data, Map<String, String> header, String mimeType,
                              int connectTimeoutMs, int readTimeoutMs,
                              int retryAttempts, int retryIntervalMs) throws IOException {
        HttpURLConnection conn = null;

        int retryCount = 0;
        while (true) {
            try {
                String fullUrl = hosts.get() + url;
                log.debug("LCC request url: {}", fullUrl);
                URL urlObj = new URL(null, fullUrl, new sun.net.www.protocol.http.Handler());
                conn = (HttpURLConnection) urlObj.openConnection();
                conn.setRequestMethod("POST");
                conn.setUseCaches(false);
                conn.setConnectTimeout(connectTimeoutMs);
                conn.setReadTimeout(readTimeoutMs);
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", mimeType + "; charset=" + UTF8);
                if (header != null) {
                    for (Map.Entry<String, String> en : header.entrySet()) {
                		conn.setRequestProperty(en.getKey(), en.getValue());
                    }
                }

                conn.connect();

                try (OutputStream out = conn.getOutputStream()) {
                    out.write(data.getBytes(UTF8));
                }

                try (InputStream in = conn.getInputStream()) {
                    String resp = read(in);
                    int status = conn.getResponseCode();
                    if (status != 200) {
                        throw new RuntimeException("request failed, status is: " + status + ". resp is: " + resp);
                    }
                    return resp;
                }
            } catch (IOException e) {
                if (retryCount++ < retryAttempts) {
                    log.warn("request failed, retry({}/{}): {} {}", retryCount, retryAttempts, e.getClass().getName(), e.getMessage());
                    try {
                        Thread.sleep(retryIntervalMs);
                        hosts.next();
                    } catch (InterruptedException ex) {
                        log.warn("request failed, thread interrupted");
                        throw new RuntimeException(ex);
                    }
                } else {
                    log.warn("request failed, retry max times reached: {} {}", e.getClass().getName(), e.getMessage());
                    throw e;
                }
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        }
    }

    private static String read(InputStream in) throws IOException {
        try (InputStreamReader reader = new InputStreamReader(in, UTF8)) {
            char[] charBuf = new char[1024];

            StringBuilder sb = new StringBuilder();
            int len;
            while ((len = reader.read(charBuf)) != -1) {
                sb.append(charBuf, 0, len);
            }
            return sb.toString();
        }
    }
}
