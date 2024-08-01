/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.lccomputing.datapilot.hook.profiling;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.lccomputing.datapilot.hook.profiling.util.ExecutorInfo;

public abstract class Reporter {

    private final Map<String, Object> metrics = new ConcurrentHashMap<>();
    protected Arguments arguments;
    protected ExecutorInfo executor;

    public Reporter(Arguments arguments, ExecutorInfo executor) {
        this.arguments = arguments;
        this.executor = executor;
    }

    public void updateMetrics(Map<String, Object> metrics) {
        this.metrics.putAll(metrics);
    }

    public abstract void report() throws IOException;

    protected String getMetrics() {
        StringBuilder builder = new StringBuilder("{");
        builder.append("\"cluster\":\"" + arguments.getCluster() + "\"");
        builder.append(",\"appId\":\"" + executor.getAppId() + "\"");
        builder.append(",\"executorId\":\"" + executor.getExecutorId() + "\"");
        for (int i = 0; i < Constants.REPORT_METRICS.length; i++) {
            String metric = Constants.REPORT_METRICS[i];
            Long value = (Long) metrics.get(Constants.REPORT_METRICS[i]);
            if (value == null || value < 0) {
                value = 0L;
            }
            builder.append(",\"" + metric + "\":" + value);
        }
        builder.append(",\"gc\":").append("{");
        Map<String, Object> gcMetrics = (Map<String, Object>)metrics.get("gc");
        if (gcMetrics != null && !gcMetrics.isEmpty()) {
            for (Map.Entry<String, Object> gcMetric : gcMetrics.entrySet()) {
                builder.append("\"" + gcMetric.getKey() + "\":" + gcMetric.getValue() + ",");
            }
            builder.deleteCharAt(builder.length()-1);
        }
        //builder.append(",\"gc\":\"" + gcMetrics.toString() + "\"");
        builder.append('}');
        builder.append('}');
        return builder.toString();
    }
}
