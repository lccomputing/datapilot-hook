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

package com.lccomputing.datapilot.hook.profiling.reporters;

import com.lccomputing.datapilot.hook.profiling.Arguments;
import com.lccomputing.datapilot.hook.profiling.Reporter;
import com.lccomputing.datapilot.hook.profiling.util.ExecutorInfo;
import com.lccomputing.datapilot.hook.profiling.util.HttpUtils;

import java.io.IOException;

public class HTTPOutputReporter extends Reporter {

    public final static String CREATE_EXECUTOR_STATS = "/v1/create_executor_stats";
    private final String httpServer;
    private int timeout = 2000;    // connection or request timeout to http server, unit: milliseconds

    public HTTPOutputReporter(Arguments arguments, ExecutorInfo executor) {
        super(arguments, executor);
        this.httpServer = arguments.getReporter().split(",")[0] + CREATE_EXECUTOR_STATS;
        this.timeout = arguments.getTimeout();
    }

    @Override
    public void report() throws IOException {
        try {
            String data = getMetrics();
            String mimeType = "application/json";
            arguments.log("start report to " + httpServer);
            HttpUtils.post(httpServer, data, mimeType,
                    timeout, timeout, 0, 0);
            arguments.log("report success: " + data);
        } catch (Exception e) {
            arguments.log("report failed: " + e.getMessage());
        }
    }
}
