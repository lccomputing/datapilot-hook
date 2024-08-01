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

import java.util.HashSet;
import java.util.Set;

public class Constants {
    public final static int KBYTE = 1024;
    public final static int MBYTE = 1024 * 1024;

    public final static String PROFILER_CpuAndMemory = "CpuAndMemory";
    public final static String METRIC_MaxHeapMemory = "MaxHeapMem";
    public final static String METRIC_MaxHeapMemoryUsed = "PeakHeapMem";
    public final static String METRIC_MaxHeapMemoryUsedCommitted = "PeakHeapMemCommitted";
    public final static String METRIC_MaxHeapMemoryUsedVmRSS = "PeakHeapMemVmRSS";
    public final static String METRIC_MaxNonHeapMemoryUsed = "PeakNonHeapMem";
    public final static String METRIC_MaxNonHeapMemoryCommitted = "PeakNonHeapMemCommitted";
    public final static String METRIC_MaxPhysicalMemory = "PeakMem";
    public final static String METRIC_MaxNettyDirectMemory = "PeakNettyDirectMemory";
    public final static String METRIC_MaxMetaspaceMemoryUsed = "PeakMetaspaceMemoryUsed";
    public final static String METRIC_MaxMetaspaceMemoryCommitted = "PeakMetaspaceMemoryCommitted";
    public final static String METRIC_MaxProcessTreeRSS = "PeakProcessTreeRSS";

    public final static String METRIC_Duration = "Duration";
    public final static String METRIC_Gc = "gc";

    public final static String[] REPORT_METRICS = new String[] {
        METRIC_MaxHeapMemoryUsed, METRIC_MaxHeapMemoryUsedCommitted, METRIC_MaxHeapMemoryUsedVmRSS,
        METRIC_MaxNonHeapMemoryUsed, METRIC_MaxNonHeapMemoryCommitted, METRIC_MaxPhysicalMemory,
        METRIC_MaxNettyDirectMemory, METRIC_MaxMetaspaceMemoryUsed, METRIC_MaxMetaspaceMemoryCommitted,
        METRIC_MaxProcessTreeRSS, METRIC_MaxHeapMemory,
        METRIC_Duration
    };

    /* We builtin some common GC collectors which categorized as young generation and old */
    public static Set<String> YOUNG_GENERATION_BUILTIN_GARBAGE_COLLECTORS = new HashSet<String>() {
        {
            add("Copy");
            add("PS Scavenge");
            add("ParNew");
            add("G1 Young Generation");
        }
    };
    public static Set<String> OLD_GENERATION_BUILTIN_GARBAGE_COLLECTORS = new HashSet<String>() {{
        add("MarkSweepCompact");
        add("PS MarkSweep");
        add("ConcurrentMarkSweep");
        add("G1 Old Generation");
    }};
    public static Set<String> nonBuiltInCollectors = new HashSet<>();

}
