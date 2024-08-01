package com.lccomputing.datapilot.hook.profiling.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class HttpUtils {
    private static final String UTF8 = "UTF-8";

    public static String post(String url, String data, String mimeType, int connectTimeoutMs, int readTimeoutMs,
                              int retryAttempts, int retryIntervalMs) throws IOException {
        URL urlObj = new URL(null, url, new sun.net.www.protocol.http.Handler());
        HttpURLConnection conn = (HttpURLConnection) urlObj.openConnection();
        conn.setRequestMethod("POST");
        conn.setUseCaches(false);
        conn.setConnectTimeout(connectTimeoutMs);
        conn.setReadTimeout(readTimeoutMs);
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", mimeType + "; charset=" + UTF8);

        int retryCount = 0;
        while (true) {
            try {
                conn.connect();

                try (OutputStream out = conn.getOutputStream()) {
                    out.write(data.getBytes(StandardCharsets.UTF_8));
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
                    try {
                        Thread.sleep(retryIntervalMs);
                    } catch (InterruptedException ex) {
                        throw new RuntimeException(ex);
                    }
                } else {
                    throw e;
                }
            } finally {
                conn.disconnect();
            }
        }
    }

    private static String read(InputStream in) throws IOException {
        try (InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
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
