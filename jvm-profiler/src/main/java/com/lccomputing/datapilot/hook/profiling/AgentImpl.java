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

import com.lccomputing.datapilot.hook.profiling.reporters.ConsoleOutputReporter;
import com.lccomputing.datapilot.hook.profiling.profilers.CpuAndMemoryProfiler;
import com.lccomputing.datapilot.hook.profiling.reporters.HTTPOutputReporter;
import com.lccomputing.datapilot.hook.profiling.util.ExecutorInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AgentImpl {

    private boolean started = false;
    private final Arguments arguments;

    public AgentImpl(Arguments arguments) {
        this.arguments = arguments;
    }

    public void run(ExecutorInfo executorInfo) {
        if (executorInfo == null) {
            arguments.log("this is not a valid executor process, exit agent");
            return;
        }

        Reporter reporter = createReporter(executorInfo);

        List<Profiler> profilers = createProfilers();
        startProfilers(profilers);

        Thread shutdownHook = new Thread(new ShutdownHookRunner(arguments,
                profilers, reporter));
        Runtime.getRuntime().addShutdownHook(shutdownHook);
    }

    private Reporter createReporter(ExecutorInfo executor) {
        if (arguments.getReporter().startsWith("http")) {
            return new HTTPOutputReporter(arguments, executor);
        }
        return new ConsoleOutputReporter(arguments, executor);
    }

    private List<Profiler> createProfilers() {
        List<Profiler> profilers = new ArrayList<>();

        CpuAndMemoryProfiler cpuAndMemoryProfiler = new CpuAndMemoryProfiler(arguments);
        profilers.add(cpuAndMemoryProfiler);

        return profilers;
    }

    public void startProfilers(Collection<Profiler> profilers) {
        if (started) {
            arguments.log("Profilers already started, do not start it again");
            return;
        }
        for (Profiler profiler : profilers) {
            try {
                profiler.profile();
            } catch (Throwable ex) {
                arguments.log("Failed to run one time profiler: " + profiler + ", err msg: " + ex.getMessage());
            }
        }
        scheduleProfilers(profilers);
        started = true;
    }

    private void scheduleProfilers(Collection<Profiler> profilers) {
        ScheduledExecutorService scheduledExecutorService =
                Executors.newScheduledThreadPool(1, new AgentThreadFactory());

        for (Profiler profiler : profilers) {
            ProfilerRunner worker = new ProfilerRunner(arguments, profiler);
            scheduledExecutorService.scheduleAtFixedRate(worker, 0,
                    arguments.getSampleInterval(), TimeUnit.MILLISECONDS);

            arguments.log(String.format("Scheduled profiler %s with interval %s millis",
                    profiler.getName(), arguments.getSampleInterval()));
        }
    }
}
