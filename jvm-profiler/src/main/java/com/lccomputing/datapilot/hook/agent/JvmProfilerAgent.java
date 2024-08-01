package com.lccomputing.datapilot.hook.agent;

import com.lccomputing.datapilot.hook.profiling.JvmProfiler;

import java.lang.instrument.Instrumentation;

public class JvmProfilerAgent {
    public static void premain(final String args, final Instrumentation instrumentation) {
        try {
            JvmProfiler.run(args);
        } catch (Exception ex) {
            System.err.println("Agent start failed: " + ex.getMessage() + ", premain args: " + args);
        }
    }
}
