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
package com.lccomputing.datapilot.hook.agent;

import com.google.gson.JsonObject;
import com.lccomputing.datapilot.hook.common.RollingString;
import com.lccomputing.datapilot.hook.utils.HttpUtils;
import com.lccomputing.datapilot.hook.utils.ReflectUtil;
import org.apache.hadoop.conf.Configuration;
import org.apache.tez.common.TezUtils;
import org.apache.tez.common.counters.CounterGroup;
import org.apache.tez.common.counters.TaskCounter;
import org.apache.tez.common.counters.TezCounter;
import org.apache.tez.dag.app.dag.DAGState;
import org.apache.tez.dag.app.dag.Task;
import org.apache.tez.dag.app.dag.Vertex;
import org.apache.tez.dag.app.dag.impl.DAGImpl;
import org.apache.tez.dag.records.TezVertexID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("unused")
public class DagReporter {
    private static final Logger LOG = LoggerFactory.getLogger(DagReporter.class);
    private static final Set<DAGState> FINISHED_STATES = EnumSet.of(DAGState.SUCCEEDED, DAGState.FAILED, DAGState.KILLED, DAGState.ERROR);

    public static void report(DAGImpl dag, DAGState state, long finishTime, long initTime) throws Exception {
        if (!FINISHED_STATES.contains(state)) {
            return;
        }

        if (isMergeFilesTask(dag)) {
            // 过滤合并小文件任务，不汇报
            LOG.info("LCC Hook skip report, merge files task");
            return;
        }

        JsonObject report = new JsonObject();

        LinkedHashMap<String, LinkedHashSet<String>> dagConf = new LinkedHashMap<>();
        for (Map.Entry<TezVertexID, Vertex> vertexEntry : dag.getVertices().entrySet()) {
            Vertex vertex = vertexEntry.getValue();
            Configuration payload = TezUtils.createConfFromUserPayload(vertex.getProcessorDescriptor().getUserPayload());
            if (payload == null) {
                continue;
            }

            for (Map.Entry<String, String> en : payload) {
                String key = en.getKey();
                String value = en.getValue();
                if (value == null) {
                    continue;
                }

                LinkedHashSet<String> values = dagConf.get(key);
                if (values == null) {
                    values = new LinkedHashSet<>();
                    dagConf.put(key, values);
                }
                values.add(value);
            }

            // 仅获取合并前的vertexConf，原因是合并后的vertex.conf会包括amConf中的值，而这个amConf中的值有可能是第一个dag传入的
            Configuration conf = ReflectUtil.extract(vertex, "org.apache.tez.dag.app.dag.impl.VertexImpl", "vertexOnlyConf");
            for (Map.Entry<String, String> en : conf) {
                String key = en.getKey();
                String value = en.getValue();
                if (value == null) {
                    continue;
                }

                // vertex.conf中有的会覆盖掉已有的值（vertex.conf中的值是我们放入的，hive on tez不会放入到DAGImpl里面）
                LinkedHashSet<String> values = new LinkedHashSet<>();
                dagConf.put(key, values);
                values.add(value);
            }
        }
        JsonObject dagConfObj = new JsonObject();
        for (Map.Entry<String, LinkedHashSet<String>> en : dagConf.entrySet()) {
            StringBuilder sb = new StringBuilder();
            for (String v : en.getValue()) {
                sb.append(v).append(",");
            }
            if (sb.length() > 0) {
                dagConfObj.addProperty(en.getKey(), sb.substring(0, sb.length() - 1));
            }
        }
        report.add("conf", dagConfObj);

        JsonObject counterObj = new JsonObject();
        for (CounterGroup counterGroup : dag.getAllCounters()) {
            JsonObject c = new JsonObject();
            for (TezCounter counter : counterGroup) {
                c.addProperty(counter.getName(), counter.getValue());
            }
            counterObj.add(counterGroup.getName(), c);
        }

        long maxMem = -1L;
        TaskCounter[] counterNames = new TaskCounter[]{
                TaskCounter.PHYSICAL_MEMORY_BYTES,
                TaskCounter.SHUFFLE_BYTES,
                TaskCounter.COMMITTED_HEAP_BYTES,
                TaskCounter.ADDITIONAL_SPILLS_BYTES_WRITTEN,
                TaskCounter.ADDITIONAL_SPILLS_BYTES_READ,
                TaskCounter.CPU_MILLISECONDS,
                TaskCounter.SHUFFLE_PHASE_TIME,
                TaskCounter.MERGE_PHASE_TIME
        };
        int counterLen = counterNames.length;

        long[] maxVals = new long[counterLen];
        JsonObject vertexCounters = new JsonObject();
        for (Vertex vertex : dag.getVertices().values()) {
            Arrays.fill(maxVals, -1L);
            JsonObject counters = new JsonObject();
            long gcRatioMilliMax = 0;
            long accumulatedGcMills = 0;
            long accumulatedCpuMills = 0;
            long accumulatedShuffleBytes = 0;
            long accumulatedSpillWrittenBytes = 0;
            long accumulatedSpillReadBytes = 0;
            long accumulatedInputSplitLengthBytes = 0;
            long accumulatedShufflePhaseTime = 0;
            long accumulatedMergePhaseTime = 0;
            for (Task task : vertex.getTasks().values()) {
                for (int i = 0; i < counterLen; i++) {
                    maxVals[i] = FindMaxValue(task, counterNames[i], maxVals[i]);
                }
                long cpuMills = task.getCounters().findCounter(TaskCounter.CPU_MILLISECONDS).getValue();
                long gcMills = task.getCounters().findCounter(TaskCounter.GC_TIME_MILLIS).getValue();
                if (cpuMills > 0 && (gcMills * 1000 / cpuMills) > gcRatioMilliMax) {
                    gcRatioMilliMax = gcMills * 1000 / cpuMills;
                }
                accumulatedCpuMills += cpuMills;
                accumulatedGcMills += gcMills;
                accumulatedShuffleBytes += task.getCounters().findCounter(TaskCounter.SHUFFLE_BYTES).getValue();
                accumulatedSpillWrittenBytes += task.getCounters().findCounter(TaskCounter.ADDITIONAL_SPILLS_BYTES_WRITTEN).getValue();
                accumulatedSpillReadBytes += task.getCounters().findCounter(TaskCounter.ADDITIONAL_SPILLS_BYTES_READ).getValue();
                accumulatedInputSplitLengthBytes += task.getCounters().findCounter(TaskCounter.INPUT_SPLIT_LENGTH_BYTES).getValue();
                accumulatedShufflePhaseTime += task.getCounters().findCounter(TaskCounter.SHUFFLE_PHASE_TIME).getValue();
                accumulatedMergePhaseTime += task.getCounters().findCounter(TaskCounter.MERGE_PHASE_TIME).getValue();
            }
            for (int i = 0; i < counterLen; i++) {
                counters.addProperty(counterNames[i].toString(), maxVals[i]);
            }
            long gcRatioMilli = 0;
            if (accumulatedCpuMills > 0) {
                gcRatioMilli = accumulatedGcMills * 1000 / accumulatedCpuMills;
            }
            counters.addProperty("TASK_COUNT", vertex.getTasks().size());
            counters.addProperty("TASK_MEM", vertex.getTaskResource().getMemory());
            counters.addProperty("ACCUMULATED_CPU_MILLS", accumulatedCpuMills);
            counters.addProperty("GC_RATIO_MILLI", gcRatioMilli);
            counters.addProperty("GC_RATIO_MILLI_MAX", gcRatioMilliMax);
            counters.addProperty("ACCUMULATED_SHUFFLE_BYTES", accumulatedShuffleBytes);
            counters.addProperty("ACCUMULATED_SPILLS_BYTES_WRITTEN", accumulatedSpillWrittenBytes);
            counters.addProperty("ACCUMULATED_SPILLS_BYTES_READ", accumulatedSpillReadBytes);
            counters.addProperty("INPUT_SPLIT_LENGTH_BYTES", accumulatedInputSplitLengthBytes);
            counters.addProperty("ACCUMULATED_SHUFFLE_PHASE_TIME", accumulatedShufflePhaseTime);
            counters.addProperty("ACCUMULATED_MERGE_PHASE_TIME", accumulatedMergePhaseTime);
            vertexCounters.add(vertex.getName(), counters);
            if (maxVals[0] > maxMem) {
                maxMem = maxVals[0];
            }
        }

        JsonObject addons = new JsonObject();
        addons.add("vertex_counters", vertexCounters);
        addons.addProperty("max_mem", maxMem);
        counterObj.add("addons", addons);


        // 添加dag cpuUtil统计信息
        counterObj.add("cpu_util_stats", ContainerStatsUtil.calcCpuUtil(dag));


        report.add("counter", counterObj);
        report.addProperty("max_mem", maxMem);

        report.addProperty("status", state.name());
        report.addProperty("dag_id", dag.getID().toString());
        report.addProperty("dag_name", dag.getName());
        report.addProperty("start_time", dag.getStartTime());
        report.addProperty("end_time", finishTime);
        report.addProperty("init_time", initTime);


        RollingString hosts = new RollingString(Arrays.asList(System.getProperty("lcc.reporter")), 0);
        String url = "/v1/create_tez_stats";
        try {
            String body = report.toString();
            LOG.debug("LCC DataPilot reportStats request body: {}", body);
            String result = HttpUtils.post(hosts, url, body, null, "application/json",
                    3000, 3000,
                    2, 1000);
            LOG.info("LCC DataPilot reportStats response from {} is: {}", hosts.get() + url, result);
        } catch (Exception e) {
            LOG.warn("LCC DataPilot reportStats on {} failed: {}", url, e.toString());
            LOG.debug("LCC DataPilot reportStats on {} failed", url, e);
        }
    }

    private static boolean isMergeFilesTask(DAGImpl dag) {
        // 只有1个Vertex且名字包含"File Merge"的dag为合并小文件任务
        if (dag.getVertices().size() == 1) {
            for (org.apache.tez.dag.app.dag.Vertex v : dag.getVertices().values()) {
                return v.getName().contains("File Merge");
            }
        }
        return false;
    }

    private static long FindMaxValue(Task task, Enum counterName, long curMaxValue) {
        TezCounter counter = task.getCounters().findCounter(counterName);
        if (counter != null && counter.getValue() > curMaxValue) {
            return counter.getValue();
        }
        return curMaxValue;
    }

}
