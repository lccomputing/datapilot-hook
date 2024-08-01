package com.lccomputing.datapilot.hook.agent;

import com.google.gson.JsonObject;
import org.apache.hadoop.yarn.api.records.ContainerId;
import org.apache.tez.common.counters.TaskCounter;
import org.apache.tez.dag.api.oldrecords.TaskReport;
import org.apache.tez.dag.app.dag.Task;
import org.apache.tez.dag.app.dag.Vertex;
import org.apache.tez.dag.app.dag.impl.DAGImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * container stats的计算。
 * 0.10.3版本的Tez已经有container相关的counter，ContainerStatsUtil和官方的统计点有些出入，但是整体差不多。
 * 相关MR: https://github.com/apache/tez/pull/301
 */
public class ContainerStatsUtil {


    private static final Logger LOG = LoggerFactory.getLogger(ContainerStatsUtil.class);

    private static int containerSize = 0;
    private static long containerTimes = 0L;
    private static final Map<ContainerId, Long> heldContainersStartTime = new ConcurrentHashMap<>();
    private static int maxConcurrency = 0;

    private static AtomicLong timeCost = new AtomicLong(0L);

    private static final Object lock = new Object();

    public static void reset() {
        try {
            synchronized (lock) {
                containerSize = 0;
                containerTimes = 0L;
                maxConcurrency = heldContainersStartTime.size();
                long resetTime = System.currentTimeMillis();
                for (Map.Entry<ContainerId, Long> entry : heldContainersStartTime.entrySet()) {
                    entry.setValue(resetTime);
                }
                LOG.info("LCC HBO container watch reset! in-running container size:{}",
                        heldContainersStartTime.size());
            }
            timeCost.set(0L);
        } catch (Exception e) {
            LOG.warn("LCC HBO ContainerStatsUtil hook reset failed, ignore", e);
        }


    }
    public static JsonObject calcCpuUtil(DAGImpl dag) {
        JsonObject cpuStats = new JsonObject();
        try {
            long startTime = System.currentTimeMillis();
            long taskCpuTimes = 0L;
            long taskTimes = 0L;
            long taskCounts = 0L;
            int vertexConcurrency = 0;
            int vertexCounts = dag.getVertices().size();
            int memory = 0; // unit: MB
            int core = 1;
            for (Vertex vertex : dag.getVertices().values()) {
                memory = vertex.getTaskResource().getMemory();
                core = vertex.getTaskResource().getVirtualCores();
                vertexConcurrency = vertex.getMaxTaskConcurrency();
                for (Task task : vertex.getTasks().values()) {
                    long cpuMills = task.getCounters().findCounter(TaskCounter.CPU_MILLISECONDS).getValue();
                    taskCpuTimes += cpuMills;
                    TaskReport report = task.getReport();
                    long time = report.getFinishTime() - report.getStartTime();
                    if (report.getStartTime() > 0 && time > 0) {
                        taskTimes += time;
                    } else {
                        LOG.debug("LCC HBO invalid task time {} {}", time, task.getTaskId());
                    }
                }
                taskCounts += vertex.getTasks().size();
            }

            synchronized (lock) {
                long endTime = System.currentTimeMillis();
                long runningTimes = 0L;
                for (Map.Entry<ContainerId, Long> entry : heldContainersStartTime.entrySet()) {
                    if (endTime - entry.getValue() < 0) {
                        LOG.warn("LCC HBO container duration invalid! containerId: {}", entry.getKey());
                        continue;
                    }
                    runningTimes += endTime - entry.getValue();
                }
                long cumulativeContainerTime = containerTimes + 1 + runningTimes; // unit:ms
                double cpuUtil = Math.round(taskCpuTimes * 1.0 / cumulativeContainerTime * 10000) * 1.0 /100;
                double cpuUtilV2 = Math.round(taskTimes * 1.0 / cumulativeContainerTime * 10000) * 1.0 /100;
                LOG.info("LCC HBO fin container size:{}, in-running container size:{}, maxConcurrency container size:{}, " +
                                "cumulativeContainerTime(ms):{} memory:{} MB core:{}",
                        containerSize, heldContainersStartTime.size(), maxConcurrency, cumulativeContainerTime, memory, core);
                LOG.info("LCC HBO collectContainerInfo tasksCpuTimes(ms):{} vertexConcurrency:{} vertexCounts:{} " +
                                "taskCounts:{}  cpuUsage:{}% cpuUtilV2:{}%",
                        taskCpuTimes, vertexConcurrency, vertexCounts, taskCounts, cpuUtil, cpuUtilV2);

                cpuStats.addProperty("CPU_UTIL", cpuUtil);
                cpuStats.addProperty("CPU_UTIL_V2", cpuUtilV2);
                cpuStats.addProperty("FIN_CONTAINER_SIZE", containerSize);
                cpuStats.addProperty("RUNNING_CONTAINER_SIZE", heldContainersStartTime.size());
                cpuStats.addProperty("REAL_MAX_CONCURRENCY", maxConcurrency);
                cpuStats.addProperty("MAX_CONCURRENCY", vertexConcurrency);
                cpuStats.addProperty("CPU_TIME", cumulativeContainerTime/1000 * core);
                cpuStats.addProperty("MEM_TIME", cumulativeContainerTime/1000 * memory);
            }
            timeCost.addAndGet(System.currentTimeMillis() - startTime);
            LOG.info("LCC HBO collectContainerInfo timeCost:{} ms", timeCost.get());
            return cpuStats;
        } catch (Exception e) {
            LOG.warn("LCC HBO ContainerStatsUtil hook calcCpuUtil failed, ignore, ", e);
        }
        return cpuStats;
    }

    public static void markStartNoException(ContainerId containerId) {
        try {
            long startTime = System.currentTimeMillis();
            synchronized (lock) {
                if (heldContainersStartTime.containsKey(containerId)) {
                    return;
                }
                LOG.debug("LCC HBO container start {}", containerId);
                heldContainersStartTime.put(containerId, System.currentTimeMillis());
                if (heldContainersStartTime.size() > maxConcurrency) {
                    maxConcurrency = heldContainersStartTime.size();
                }
            }
            timeCost.addAndGet(System.currentTimeMillis() - startTime);
        } catch (Exception e) {
            LOG.warn("LCC HBO ContainerStatsUtil hook markStartNoException failed, ignore", e);
        }

    }

    public static void markEndNoException(ContainerId containerId) {
        try {
            long start = System.currentTimeMillis();
            synchronized (lock) {
                Long startTime = heldContainersStartTime.remove(containerId);
                if (startTime == null) {
                    return;
                }
                long duration = System.currentTimeMillis() - startTime;
                containerSize++;
                containerTimes += duration;
                LOG.debug("LCC HBO container end duration:{}ms {}", duration, containerId);
            }
            timeCost.addAndGet(System.currentTimeMillis() - start);
        } catch (Exception e) {
            LOG.warn("LCC HBO ContainerStatsUtil hook markEndNoException failed, ignore", e);
        }


    }

}
