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
