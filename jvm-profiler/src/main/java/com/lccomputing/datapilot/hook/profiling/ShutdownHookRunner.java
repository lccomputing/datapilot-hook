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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ShutdownHookRunner implements Runnable {
    private final Arguments arguments;
    private final List<Profiler> profilers;
    private final Reporter reporter;

    public ShutdownHookRunner(Arguments arguments, Collection<Profiler> profilers, Reporter reporter) {
        this.arguments = arguments;
        this.profilers = new ArrayList<>(profilers);
        this.reporter = reporter;
    }

    @Override
    public void run() {
        arguments.log("Running shutdown hook");
        for (Profiler profiler : profilers) {
            try {
                profiler.terminate();
                reporter.updateMetrics(profiler.getMetrics());
            } catch (Throwable ex) {
                arguments.log("Failed to run periodic profiler (last run): " + ex.getMessage());
            }
        }

        try {
            // output to console or send metrics to collect_server
            reporter.report();
        } catch (Throwable ex) {
            arguments.log("Failed to report: " + ex.getMessage());
        }
    }
}
