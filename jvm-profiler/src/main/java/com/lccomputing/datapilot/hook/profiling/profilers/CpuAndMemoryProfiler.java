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

package com.lccomputing.datapilot.hook.profiling.profilers;

import com.lccomputing.datapilot.hook.profiling.Arguments;
import com.lccomputing.datapilot.hook.profiling.Constants;
import com.lccomputing.datapilot.hook.profiling.Profiler;
import com.lccomputing.datapilot.hook.profiling.util.ProcFileUtils;
import com.lccomputing.datapilot.hook.profiling.util.ProcessUtils;
import com.lccomputing.datapilot.hook.profiling.util.StringUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.management.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class CpuAndMemoryProfiler implements Profiler {

    private Arguments args;

    private MemoryMXBean memoryMXBean;

    private long maxHeapMemory = Long.MIN_VALUE;
    private long maxHeapMemoryUsed = Long.MIN_VALUE;
    private long maxHeapMemoryUsedCommitted = Long.MIN_VALUE;
    private long maxNonHeapMemoryUsed = Long.MIN_VALUE;
    private long maxNonHeapMemoryCommitted = Long.MIN_VALUE;
    private long maxHeapMemoryUsedVmRSS = Long.MIN_VALUE;
    private long maxPhysicalMemory = Long.MIN_VALUE;
    private long maxNettyDirectMemory = Long.MIN_VALUE;
    private long maxMetaspaceMemoryUsed = Long.MIN_VALUE;
    private long maxMetaspaceMemoryCommitted = Long.MIN_VALUE;
    private long maxProcessTreeRSS = Long.MIN_VALUE;
    private long totalGcTime = 0L;

    private AtomicLong nettyDirectMemoryCounter = null;
    // gc
    private Map<String, Object> gcMetrics = new HashMap<>();
    private long startTime;
    private long duration;
    private String user;
    private int pid;

    public CpuAndMemoryProfiler(Arguments args) {
        this.args = args;
        init();
    }

    public static void main(String[] args) {
        new CpuAndMemoryProfiler(new Arguments("")).profile();
    }

    @Override
    public String getName() {
        return Constants.PROFILER_CpuAndMemory;
    }

    @Override
    public Map<String, Object> getMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put(Constants.METRIC_MaxHeapMemory, maxHeapMemory / Constants.MBYTE);
        metrics.put(Constants.METRIC_MaxHeapMemoryUsed, maxHeapMemoryUsed / Constants.MBYTE);
        metrics.put(Constants.METRIC_MaxHeapMemoryUsedCommitted, maxHeapMemoryUsedCommitted / Constants.MBYTE);
        metrics.put(Constants.METRIC_MaxHeapMemoryUsedVmRSS, maxHeapMemoryUsedVmRSS / Constants.MBYTE);
        metrics.put(Constants.METRIC_MaxNonHeapMemoryUsed, maxNonHeapMemoryUsed / Constants.MBYTE);
        metrics.put(Constants.METRIC_MaxNonHeapMemoryCommitted, maxNonHeapMemoryCommitted / Constants.MBYTE);
        metrics.put(Constants.METRIC_MaxPhysicalMemory, maxPhysicalMemory / Constants.MBYTE);
        metrics.put(Constants.METRIC_MaxNettyDirectMemory, maxNettyDirectMemory / Constants.MBYTE);
        metrics.put(Constants.METRIC_MaxMetaspaceMemoryUsed, maxMetaspaceMemoryUsed / Constants.MBYTE);
        metrics.put(Constants.METRIC_MaxMetaspaceMemoryCommitted, maxMetaspaceMemoryCommitted / Constants.MBYTE);
        metrics.put(Constants.METRIC_MaxProcessTreeRSS, maxProcessTreeRSS / Constants.KBYTE);
        metrics.put(Constants.METRIC_Duration, duration);
        metrics.put(Constants.METRIC_Gc, gcMetrics);
        return metrics;
    }

    @Override
    public synchronized void profile() {
        if (memoryMXBean != null) {
            MemoryUsage memoryUsage = memoryMXBean.getHeapMemoryUsage();
            long heapMemoryUsed = memoryUsage.getUsed();
            long heapMemoryCommitted = memoryUsage.getCommitted();

            if (heapMemoryUsed > maxHeapMemoryUsed) {
                maxHeapMemoryUsed = heapMemoryUsed;
                maxHeapMemoryUsedCommitted = heapMemoryCommitted;
                Map<String, String> procStatus = ProcFileUtils.getProcStatus();
                Long rss = ProcFileUtils.getBytesValue(procStatus, "VmRSS");
                if (rss != null) {
                    maxHeapMemoryUsedVmRSS = rss;
                }
                memoryUsage = memoryMXBean.getNonHeapMemoryUsage();
                maxNonHeapMemoryUsed = memoryUsage.getUsed();
                maxNonHeapMemoryCommitted = memoryUsage.getCommitted();
            }
        }

        if (nettyDirectMemoryCounter != null) {
            long memUsed = nettyDirectMemoryCounter.get();
            if (memUsed > maxNettyDirectMemory) {
                maxNettyDirectMemory = memUsed;
            }
        }

        if (args.isEnableSubtree()) {
            long ptrss = computeProcessTreeRSS();
            if (ptrss > 0 && ptrss > maxProcessTreeRSS) {
                maxProcessTreeRSS = ptrss;
            }
        }
    }

    protected Long computeProcessTreeRSS() {
        if (user == null || pid <= 0) {
            // user or pid error, do not collect subprocess rss
            return 0L;
        }

        try {
            ProcessBuilder builder = new ProcessBuilder("ps", "-U", user,
                    "-o", "pid,ppid,rss").redirectErrorStream(true);
            Process process = builder.start();

            Field pidField = process.getClass().getDeclaredField("pid");
            pidField.setAccessible(true);
            int psCmdPid = pidField.getInt(process);

            BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
            List<int[]> items = new ArrayList();
            String line;
            while ((line = br.readLine()) != null) {
                if (line.contains("PID"))
                    continue;
                // \W+ means non-word characters
                String[] ss = line.trim().split("\\W+");
                if (ss.length == 3) {
                    int[] item = new int[3];
                    item[0] = Integer.parseInt(ss[0]);
                    item[1] = Integer.parseInt(ss[1]);
                    item[2] = Integer.parseInt(ss[2]);
                    items.add(item);
                }
            }
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                return 0L;
            }

            Set pPIDs = new HashSet<Integer>();
            pPIDs.add(pid);

            short depth = 5;
            long totalRss = 0;
            boolean mainPidProcessed = false;
            while (depth > 0) {
                boolean newChildFound = false;
                for (int[] item : items) {
                    // 0:pid, 1:ppid, 2:rss
                    if (psCmdPid == item[0]) {
                        // ignore ps sub-process
                        continue;
                    }
                    if (pid == item[0] && !mainPidProcessed) {
                        // main process, only add once
                        totalRss += item[2];
                        mainPidProcessed = true;
                    }
                    if (pPIDs.contains(item[1]) && !pPIDs.contains(item[0])) {
                        newChildFound = true;
                        // add this pid for next iteration
                        pPIDs.add(item[0]);
                        totalRss += item[2];
                    }
                }
                if (newChildFound == false) {
                    // break when no new child found
                    break;
                }
                depth--;
            }
            return totalRss;

        } catch (Exception ex) {
        }

        return 0L;
    }

    @Override
    public void terminate() {
        // See http://man7.org/linux/man-pages/man5/proc.5.html for details about proc status
        Map<String, String> procStatus = ProcFileUtils.getProcStatus();
        Long procStatusVmHWM = ProcFileUtils.getBytesValue(procStatus, "VmHWM");
        if (procStatusVmHWM != null) {
            maxPhysicalMemory = procStatusVmHWM;
        }
        duration = (System.currentTimeMillis() - startTime) / 1000;
        for (MemoryPoolMXBean pool : ManagementFactory.getMemoryPoolMXBeans()) {
            if ("Metaspace".equalsIgnoreCase(pool.getName())) {
                maxMetaspaceMemoryUsed = pool.getPeakUsage().getUsed();
                maxMetaspaceMemoryCommitted = pool.getPeakUsage().getCommitted();
            }
        }
        List<GarbageCollectorMXBean> gcMXBeans = ManagementFactory.getGarbageCollectorMXBeans();
        if (gcMXBeans != null) {
            for (GarbageCollectorMXBean gcMXBean : gcMXBeans) {
                totalGcTime += new Long(gcMXBean.getCollectionTime());
                if (Constants.YOUNG_GENERATION_BUILTIN_GARBAGE_COLLECTORS.contains(gcMXBean.getName())) {
                    gcMetrics.put("MinorGCCount", new Long(gcMXBean.getCollectionCount()));
                    gcMetrics.put("MinorGCTime", new Long(gcMXBean.getCollectionTime()));
                } else if (Constants.OLD_GENERATION_BUILTIN_GARBAGE_COLLECTORS.contains(gcMXBean.getName())) {
                    gcMetrics.put("MajorGCCount", new Long(gcMXBean.getCollectionCount()));
                    gcMetrics.put("MajorGCTime", new Long(gcMXBean.getCollectionTime()));
                } else if (!Constants.nonBuiltInCollectors.contains(gcMXBean.getName())) {
                    Constants.nonBuiltInCollectors.add(gcMXBean.getName());
                    // print it when first seen
//                    System.err.println("WARN:  To enable non-built-in garbage collector(s): " + Constants.nonBuiltInCollectors);
                }
            }
            gcMetrics.put("TotalGCTime", totalGcTime);
        }

        for (String inputArgument : ManagementFactory.getRuntimeMXBean().getInputArguments()) {
            if (inputArgument.startsWith("-Xmx")) {
                maxHeapMemory = StringUtils.getBytesValueOrNull(inputArgument.substring("-Xmx".length()));
                break;
            }
        }
    }

    private void init() {
        try {
            startTime = System.currentTimeMillis();
            user = System.getProperty("user.name");
            pid = ProcessUtils.getProcessID();
            memoryMXBean = ManagementFactory.getMemoryMXBean();

            try {
                Class PlatformDependentClass = Class.forName("io.netty.util.internal.PlatformDependent");
                Field usedMemory = PlatformDependentClass.getDeclaredField("DIRECT_MEMORY_COUNTER");
                usedMemory.setAccessible(true);
                nettyDirectMemoryCounter = (AtomicLong) usedMemory.get(PlatformDependentClass);
            } catch (Exception ex) {
//                System.err.println("WARN:  init netty memory counter failed: " + ex.getMessage());
            }

        } catch (Throwable ex) {
//            System.err.println("init memoryMXBean failed: " + ex.getMessage());
        }
    }
}
