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
package com.lccomputing.datapilot.hook.profiling;

import com.lccomputing.datapilot.hook.profiling.util.ExecutorInfo;
import com.lccomputing.datapilot.hook.profiling.util.ProcFileUtils;

import java.util.List;
import java.util.regex.Pattern;

import static com.lccomputing.datapilot.hook.profiling.util.ProcessUtils.getJvmInputArguments;
import static com.lccomputing.datapilot.hook.profiling.util.StringUtils.getLastMatcher;
import static com.lccomputing.datapilot.hook.profiling.util.StringUtils.isEmpty;

public class JvmProfiler {
    private static final Pattern PATTERN_APP_ID = Pattern.compile("\\s--app-id\\s+(\\S+?)(?:\\s|$)");
    private static final Pattern PATTERN_EXECUTOR_ID = Pattern.compile("\\s--executor-id\\s+(\\S+?)(?:\\s|$)");
    // container_e201_1708312386883_11815_01_000001 or container_1711420612171_0207_01_000001
    private static final Pattern PATTERN_CONTAINER_ID = Pattern.compile("_(\\d{8,}_\\d+)");

    private static final String SPARK_EXECUTOR_CLASS_NAME = "spark.executor.CoarseGrainedExecutorBackend";
    private static final String SPARK_EXECUTOR_KEYWORD = "spark.driver.port";
    private static final String SPARK_CLUSTER_AM_CLASS_NAME = "org.apache.spark.deploy.yarn.ApplicationMaster";
    private static final String SPARK_CLIENT_AM_CLASS_NAME = "org.apache.spark.deploy.yarn.ExecutorLauncher";
    private static final String TEZ_AM_CLASS_NAME = "org.apache.tez.dag.app.DAGAppMaster";

    public static void run(String args) {
        Arguments arguments = new Arguments(args);
        AgentImpl agentImpl = new AgentImpl(arguments);
        try {
            ExecutorInfo executorInfo = probeExecutor();
            agentImpl.run(executorInfo);
        } catch (Exception e) {
            arguments.log(e.getMessage());
        }
    }

    public static ExecutorInfo probeExecutor() throws Exception {
        String cmdline = ProcFileUtils.getCmdline();
        if (isSparkExecutor(cmdline)) {
            String appId = getLastMatcher(cmdline, PATTERN_APP_ID, 1);
            String executorId = getLastMatcher(cmdline, PATTERN_EXECUTOR_ID, 1);

            if (isEmpty(appId)) {
                throw new Exception("can't find the appId");
            }
            if (isEmpty(executorId)) {
                throw new Exception("can't find the executorId");
            }

            return new ExecutorInfo(appId, executorId);
        } else if (isSparkAM(cmdline) || isTezAM(cmdline)) {
            String containerId = System.getenv("CONTAINER_ID");
            if (isEmpty(containerId)) {
                throw new Exception("can't find the containerId");
            }

            String appIdNum = getLastMatcher(containerId, PATTERN_CONTAINER_ID, 1);
            if (isEmpty(appIdNum)) {
                throw new Exception("this containerId can't be parsed: " + containerId);
            }

            String appId = "application_" + appIdNum;
            return new ExecutorInfo(appId, "driver");
        }
        throw new Exception("this is not spark executor or spark driver");
    }

    private static boolean isSparkAM(String cmdline) {
        if (cmdline != null && !cmdline.isEmpty()) {
            if (cmdline.contains(SPARK_CLUSTER_AM_CLASS_NAME) || cmdline.contains(SPARK_CLIENT_AM_CLASS_NAME)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isSparkExecutor(String cmdline) {
        if (cmdline != null && !cmdline.isEmpty()) {
            if (cmdline.contains(SPARK_EXECUTOR_CLASS_NAME)) {
                return true;
            }
        }

        List<String> strList = getJvmInputArguments();
        for (String str : strList) {
            if (str.toLowerCase().contains(SPARK_EXECUTOR_KEYWORD.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isTezAM(String cmdline) {
        if (cmdline != null && !cmdline.isEmpty()) {
            if (cmdline.contains(TEZ_AM_CLASS_NAME)) {
                return true;
            }
        }
        return false;
    }

}
