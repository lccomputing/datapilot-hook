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
package com.lccomputing.datapilot.hook.profiling.util;

public class ExecutorInfo {
    private final String appId;
    private final String executorId;

    public ExecutorInfo(String appId, String executorId) {
        this.appId = appId;
        this.executorId = executorId;
    }

    public String getAppId() {
        return appId;
    }

    public String getExecutorId() {
        return executorId;
    }


    @Override
    public String toString() {
        return "ExecutorInfo{" +
                "appId='" + appId + '\'' +
                ", executorId='" + executorId + '\'' +
                '}';
    }
}
