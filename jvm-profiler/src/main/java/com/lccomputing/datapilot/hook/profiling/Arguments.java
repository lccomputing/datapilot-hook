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

import com.lccomputing.datapilot.hook.profiling.util.StringUtils;

public class Arguments {
    public final static String ARG_CLUSTER = "cluster";
    public final static String ARG_REPORTER = "reporter";
    public final static String ARG_SAMPLE_INTERVAL = "sampleinterval";
    public final static String ARG_TIMEOUT = "timeout";
    public final static String ARG_ENABLELOG = "enablelog";
    public final static String ARG_ENABLE_SUBTREE = "enablesubtree";

    private String cluster = "default";    // cluster name
    private String reporter = "console";   // console or http://server:port
    private int sampleInterval = 3000;    // unit: milliseconds
    private int timeout = 2000;           // connection or request timeout to http server, unit: milliseconds
    private boolean enablelog = true;         // logging or not, default logging
    private boolean enableSubtree = false;        // monitor subprocess tree, default false

    public Arguments(String args) {
        if (StringUtils.isNotEmpty(args)) {
            for (String argPair : args.split(";")) {
                String[] ss = argPair.split("=");
                if (ss.length != 2) {
                    throw new IllegalArgumentException("Arguments for the agent should be like: key1=value1;key2=value2");
                }

                String key = ss[0].trim().toLowerCase();
                if (key.isEmpty()) {
                    throw new IllegalArgumentException("Argument key should not be empty");
                }

                String value = ss[1].trim();
                switch (key) {
                    case ARG_CLUSTER:
                        this.cluster = value;
                        break;
                    case ARG_REPORTER:
                        this.reporter = value;
                        break;
                    case ARG_SAMPLE_INTERVAL:
                        this.sampleInterval = Integer.parseInt(value);
                        break;
                    case ARG_TIMEOUT:
                        this.timeout = Integer.parseInt(value);
                        break;
                    case ARG_ENABLELOG:
                        this.enablelog = value.equalsIgnoreCase("true");
                        break;
                    case ARG_ENABLE_SUBTREE:
                        this.enableSubtree = value.equalsIgnoreCase("true");
                        break;
                    default:
                        break;
                }
            }
        }
        log(this.toString());
    }

    public String getCluster() {
        return cluster;
    }

    public String getReporter() {
        return reporter;
    }

    public int getSampleInterval() {
        return sampleInterval;
    }

    public int getTimeout() {
        return timeout;
    }

    public boolean isEnableLog() {
        return enablelog;
    }

    public boolean isEnableSubtree() {
        return enableSubtree;
    }

    public void log(String msg) {
        if (enablelog) {
            String logMsg = StringUtils.formatLogMsg(msg);
            System.err.println(logMsg);
        }
    }

    @Override
    public String toString() {
        StringBuffer s = new StringBuffer("Arguments (");
        s.append("cluster=" + cluster);
        s.append(",reporter=").append(reporter);
        s.append(",sampleInterval=").append(sampleInterval);
        s.append(",enableSubtree=").append(enableSubtree);
        s.append(",timeout=").append(timeout).append(')');
        return s.toString();
    }
}
