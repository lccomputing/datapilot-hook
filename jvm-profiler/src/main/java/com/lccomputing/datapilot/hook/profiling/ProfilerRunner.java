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

import java.util.concurrent.atomic.AtomicLong;

public class ProfilerRunner implements Runnable {

    private static final int ERROR_COUNT_TO_LOG = 100;
    private final Profiler profiler;
    private final AtomicLong errorCounter = new AtomicLong(0);
    private final Arguments arguments;

    public ProfilerRunner(Arguments arguments, Profiler profiler) {
        this.arguments = arguments;
        this.profiler = profiler;
    }

    @Override
    public void run() {
        try {
            profiler.profile();
        } catch (Throwable e) {
            long count = errorCounter.incrementAndGet();
            if (count % ERROR_COUNT_TO_LOG == 0) {
                arguments.log("Failed to run profile: " + profiler + ", err: " + e.getMessage());
            }
        }
    }
}
